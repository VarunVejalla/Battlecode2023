package ali7;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

// TODO: Deterministic resource ratios.

class CarrierInfo {
    int roundNum;
    HashSet<Integer> carriers;
    public CarrierInfo(int roundNum){
        this.roundNum = roundNum;
        this.carriers = new HashSet<>();
    }
}

public class Headquarters extends Robot {

    int TIME_TO_FORGET_CARRIER = 50; // forget we've seen a carrier after this many rounds

    MapLocation myLoc;
    int myIndex;
    int numCarriersSpawned = 0;
    int numLaunchersSpawned = 0;

    double prevAdamantium = 0; // amount of adamantium we held in the previous round
    double prevMana = 0; // amount of mana we held in the previous round
    double adamantiumDeltaEMA = 0;  // EMA = Exponential Moving average: https://www.investopedia.com/terms/e/ema.asp
    double manaDeltaEMA = 0;
    double EMAWindowSize = 50; // previous rounds that we consider
    double EMASmoothing = 5;   // parameter used in EMA --> higher value means we give more priority to recent changes
    int lastAnchorBuiltTurn = 0;

    // Comms stuff
    ArrayList<MapLocation> sortedClosestAdamantiumWells = new ArrayList<>();
    ArrayList<MapLocation> sortedClosestManaWells = new ArrayList<>();
    ArrayList<MapLocation> sortedClosestElixirWells = new ArrayList<>();

    boolean savingUp = false;
    Spawner spawner;
    CarrierInfo[] carriersLastSeen = new CarrierInfo[TIME_TO_FORGET_CARRIER];
    HashMap<Integer, Integer> carrierToRoundMap = new HashMap<Integer, Integer>();

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);
        myLoc = rc.getLocation();
        computeIndex();
        if(myIndex == 0){
            comms.resetSymmetry();
        }
        spawner = new Spawner(rc, this);
        comms.writeOurHQLocation(myIndex, myLoc);
        for(WellSquareInfo info : wellsToComm){
            if(info.type == ResourceType.ADAMANTIUM){
                addWellToSortedList(info.loc, sortedClosestAdamantiumWells);
            }
            else if(info.type == ResourceType.MANA){
                addWellToSortedList(info.loc, sortedClosestManaWells);
            }
            else if(info.type == ResourceType.ELIXIR){
                addWellToSortedList(info.loc, sortedClosestElixirWells);
            }
        }
    }

    public void computeIndex() throws GameActionException {
        if(rc.readSharedArray(0) == 0){
            myIndex = 0;
        }
        else if(rc.readSharedArray(1) == 0){
            myIndex = 1;
        }
        else if(rc.readSharedArray(2) == 0){
            myIndex = 2;
        }
        else{
            myIndex = 3;
        }
    }

    // If you're past some threshold, then make sure you always have an anchor available (or save up for one) for a carrier to grab.
    // I implemented exponential moving average, which i think could solve this effeciently. see the computeEMA method above
    // might need to mess around with smoothing (EMASmoothing) and windowSize (EMAWindowSize) to get values on the proper scale
    public void computeAdamantiumDeltaEMA(){
        int currAdamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        if(rc.getRoundNum() > 0){
            // We only want to consider the rate at which we're gaining resources we don't wanna consider spending.
            double delta = currAdamantium - prevAdamantium;
            adamantiumDeltaEMA = delta * (EMASmoothing / (1+EMAWindowSize)) + adamantiumDeltaEMA * (1 - EMASmoothing / (1+EMAWindowSize));
        }
        prevAdamantium = currAdamantium;
    }


    public void computeManaDeltaEMA(){
        int currMana = rc.getResourceAmount(ResourceType.MANA);
        if(rc.getRoundNum() > 0){
            double delta = currMana - prevMana;
            manaDeltaEMA = delta * (EMASmoothing / (1.0 + EMAWindowSize)) + manaDeltaEMA * (1.0 - EMASmoothing / (1.0 + EMAWindowSize));
        }
    }


    public void forgetOldCarriers() {
        int roundNum = rc.getRoundNum();

        // forget carriers that we haven't seen in the past "TIME_TO_FORGET_CARRIER" rounds
        CarrierInfo info = carriersLastSeen[roundNum % TIME_TO_FORGET_CARRIER];
        if(info == null){
            return;
        }

        for(Integer id : info.carriers){
            carrierToRoundMap.remove(id);
        }
    }

    public void addNewCarriers() throws GameActionException {
        int roundNum = rc.getRoundNum();
        // check the robots that may have deposited resources to you and add them to seenCarriers
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(RobotType.CARRIER.actionRadiusSquared, myTeam);
        CarrierInfo newRoundInfo = new CarrierInfo(roundNum);
        int numCarriersParsed = 0;
        for(int j = 0; j < nearbyRobots.length; j++) {
            RobotInfo info = nearbyRobots[j];
            if(info.type != RobotType.CARRIER){
                continue;
            }
            numCarriersParsed++;
            if(numCarriersParsed > 15){
                break;
            }
            int id = info.ID;
            if(carrierToRoundMap.containsKey(id)){
                int round = carrierToRoundMap.get(id);
                carriersLastSeen[round % TIME_TO_FORGET_CARRIER].carriers.remove(id);
            }
            newRoundInfo.carriers.add(id);
            carrierToRoundMap.put(id, roundNum);
        }
        carriersLastSeen[roundNum % TIME_TO_FORGET_CARRIER] = newRoundInfo;
    }

    // count the number of miners that we've seen in the past 50 rounds
    // This method will run fully every other round cuz it takes so much goddamn bytecode
    public void updateSeenCarriers() throws GameActionException {
        forgetOldCarriers();
        addNewCarriers();
        Util.addToIndicatorString("C:" + carrierToRoundMap.size());
    }

    // criteria on whether hq should start saving up for an anchor
    public boolean shouldISaveUp() throws GameActionException {
        // Super simple saving up criteria to win the games where we're destroying opponent.
        MapLocation nearestUncontrolled = getNearestUncontrolledIsland();
        if(nearestUncontrolled == null){
            return false;
        }
        if(rc.getNumAnchors(Anchor.STANDARD) != 0){
            return false;
        }

        int numEnemyLaunchersNearby = Util.getNumTroopsInRange(myType.visionRadiusSquared, opponent, RobotType.LAUNCHER);
        boolean islandUnderControl = getNearestFriendlyIsland() != null;
        int roundsSinceLastAnchor = turnCount - lastAnchorBuiltTurn;

        int numCarriers = carrierToRoundMap.size();
        int distToIsland = myLoc.distanceSquaredTo(nearestUncontrolled);
        int roundNum = rc.getRoundNum();
        int numFriendlyIslands = getNumIslandsControlledByTeam(myTeam);
        int numEnemyIslands = getNumIslandsControlledByTeam(opponent);
        int numIslandsToWin = 4 * numIslands / 4 - numFriendlyIslands;

        if(numEnemyLaunchersNearby > 0){
            return false;
        }

        if(roundNum < 1200 && roundsSinceLastAnchor < 50){
            return false;
        }

        int heuristic = numCarriers;
        numIslandsToWin = Math.min(numIslandsToWin, 10);
        heuristic -= numIslandsToWin;

        Util.addToIndicatorString("H:" + heuristic);

        if(islandUnderControl){
            if(roundNum < 500){
                if(heuristic >= 12){
                    return true;
                }
            }
            else if(roundNum < 1400){
                if(heuristic >= 10){
                    return true;
                }
            }
            else{
                if(heuristic >= 8){
                    return true;
                }
            }
        }
        else{
            if(roundNum < 500){
                if(heuristic >= 11){
                    return true;
                }
            }
            else if(roundNum < 1400){
                if(heuristic >= 8){
                    return true;
                }
            }
            else{
                if(heuristic >= 5){
                    return true;
                }
            }
        }
        return false;
    }

    // TODO: Consider map size and numIslands in this

    public void setResourceRatios() throws GameActionException{
        if(savingUp){   // if we're trying to make an anchor but don't have enough of a specific resource, get that resource
            boolean needAdamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM) < Anchor.STANDARD.getBuildCost(ResourceType.ADAMANTIUM);
            boolean needMana = rc.getResourceAmount(ResourceType.MANA) < Anchor.STANDARD.getBuildCost(ResourceType.MANA);

            if(needAdamantium && needMana) {
                comms.writeRatio(myIndex, 1, 1, 0);
            }
            else if(needAdamantium){
                comms.writeRatio(myIndex, 12, 3, 0);
            }
            else if(needMana) {
                comms.writeRatio(myIndex, 3, 12, 0);
            }
            return;
        }

        int numCarriers = carrierToRoundMap.size();
        if(rc.getRoundNum() < 500){
            if(numCarriers > 3) {
                comms.writeRatio(myIndex, 0, 15, 0);
            }
            else if(numCarriers < 3){
                comms.writeRatio(myIndex, 8, 7, 0);
            }
        }
        else if(rc.getRoundNum() < 1200){
            if(numCarriers > 6) {
                comms.writeRatio(myIndex, 0, 15, 0);
            }
            else if(numCarriers < 6){
                comms.writeRatio(myIndex, 8, 7, 0);
            }
        }
        else{
            if(numCarriers > 9) {
                comms.writeRatio(myIndex, 0, 15, 0);
            }
            else if(numCarriers < 9){
                comms.writeRatio(myIndex, 8, 7, 0);
            }
        }
    }



    public void run() throws GameActionException {
        super.run();
        if(myIndex == 0){
            if(Util.checkNumSymmetriesPossible() == 0){
                System.out.println("Goddamnit we fucked up the symmetries");
//                rc.resign();
                comms.resetSymmetry();
            }
        }

        readNewWellLocations();
        updateClosestWells();

        if(rc.getRoundNum() > 2){
            computeAdamantiumDeltaEMA();
            computeManaDeltaEMA();

            // Once all HQs have processed the new well, reset it
            // so that a new miner can comm a new well if it has one.
            if(myIndex == numHQs - 1){
                comms.resetNewWellComms();
            }
        }

//        rc.setIndicatorString(adamantiumDeltaEMA + " " + manaDeltaEMA);
        comms.writeAdamantium(myIndex, rc.getResourceAmount(ResourceType.ADAMANTIUM));
        comms.writeMana(myIndex, rc.getResourceAmount(ResourceType.MANA));

        updateSeenCarriers();
//        Util.log("Seen carriers: " + carrierToRoundMap.size());

        savingUp = shouldISaveUp();
        setResourceRatios();
        int[] ratio = comms.readRatio(myIndex);

        Util.addToIndicatorString("RR - A:" + ratio[0] + ",M:" + ratio[1] + "E:" + ratio[2]);
        if(savingUp){
            Util.addToIndicatorString("SAVING"); // Saving up for anchor

            Util.log("Saving up for anchor!");
            if(rc.canBuildAnchor(Anchor.STANDARD)){
                rc.buildAnchor(Anchor.STANDARD);
                lastAnchorBuiltTurn = turnCount;
                savingUp = false;
            }
            // Always prioritize building launchers over carriers
            if(rc.getResourceAmount(ResourceType.MANA) > Anchor.STANDARD.getBuildCost(ResourceType.MANA) + RobotType.LAUNCHER.buildCostMana){
                buildLaunchers();
            }
            if(rc.getResourceAmount(ResourceType.ADAMANTIUM) > Anchor.STANDARD.getBuildCost(ResourceType.ADAMANTIUM) + RobotType.CARRIER.buildCostAdamantium){
                buildCarriers();
            }
        }

        else {
            // Always prioritize building launchers over carriers
            Util.addToIndicatorString("NBD"); // Normal build order
            buildLaunchers();
            buildCarriers();
        }

        // We only want to consider the rate at which we're gaining resources we don't wanna consider spending, so we wanna set
        // the prevMana and prevAdamantium variables AFTER we do all our spending.
        prevAdamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        prevMana = rc.getResourceAmount(ResourceType.MANA);
    }

    public void updateClosestWells() throws GameActionException {
        updateClosestWells(ResourceType.ADAMANTIUM, sortedClosestAdamantiumWells);
        updateClosestWells(ResourceType.MANA, sortedClosestManaWells);
        updateClosestWells(ResourceType.ELIXIR, sortedClosestElixirWells);
    }

    public void updateClosestWells(ResourceType type, ArrayList<MapLocation> sortedClosestWells) throws GameActionException {
        if(sortedClosestWells.size() == 0){
            return;
        }
        int wellIdx = rc.getRoundNum() % sortedClosestWells.size();
        MapLocation wellLoc = sortedClosestWells.get(wellIdx);
        comms.setClosestWell(myIndex, type, wellLoc);
    }

    public void readNewWellLocations() throws GameActionException {
        readNewWellLocations(comms.getNewWellDetected(ResourceType.ADAMANTIUM), adamantiumWells, sortedClosestAdamantiumWells);
        readNewWellLocations(comms.getNewWellDetected(ResourceType.MANA), manaWells, sortedClosestManaWells);
        readNewWellLocations(comms.getNewWellDetected(ResourceType.ELIXIR), elixirWells, sortedClosestElixirWells);
    }

    public void readNewWellLocations(MapLocation newWellLoc, MapLocation[] regionWells, ArrayList<MapLocation> sortedClosestWells) throws GameActionException {
        if(newWellLoc == null){
            return;
        }
        int newWellRegionNum = Util.getRegionNum(newWellLoc);
        if(regionWells[newWellRegionNum] != null) {
            return;
        }
        regionWells[newWellRegionNum] = newWellLoc;
        sortedClosestWells.add(newWellLoc);
        sortedClosestWells.sort(Comparator.comparingInt((MapLocation a) -> myLoc.distanceSquaredTo(a)));
        if(sortedClosestWells.size() > constants.NUM_WELLS_TO_CYCLE_WHEN_SATURATED){
            sortedClosestWells.remove(sortedClosestWells.size() - 1);
        }
    }

    public void addWellToSortedList(MapLocation newWellLoc, ArrayList<MapLocation> sortedClosestWells){
        sortedClosestWells.add(newWellLoc);
        sortedClosestWells.sort(Comparator.comparingInt((MapLocation a) -> myLoc.distanceSquaredTo(a)));
        if(sortedClosestWells.size() > constants.NUM_WELLS_TO_CYCLE_WHEN_SATURATED){
            sortedClosestWells.remove(sortedClosestWells.size() - 1);
        }
    }

    public MapLocation getWellToSpawnTowards() throws GameActionException {
        ResourceType resourceType = Util.determineWhichResourceToGet(myIndex);
        ArrayList<MapLocation> sortedClosestWells = getClosestWellList(resourceType);
        if(!sortedClosestWells.isEmpty()){
            return sortedClosestWells.get(0);
        }
        if(!sortedClosestElixirWells.isEmpty()){
            return sortedClosestElixirWells.get(0);
        }
        if(!sortedClosestManaWells.isEmpty()){
            return sortedClosestManaWells.get(0);
        }
        if(!sortedClosestAdamantiumWells.isEmpty()){
            return sortedClosestAdamantiumWells.get(0);
        }
        return null;
    }

    public ArrayList<MapLocation> getClosestWellList(ResourceType type){
        switch(type){
            case ADAMANTIUM:
                return sortedClosestAdamantiumWells;
            case MANA:
                return sortedClosestManaWells;
            case ELIXIR:
                return sortedClosestElixirWells;
        }
        throw new RuntimeException("Trying to get closest well list of unknown resource: " + type);
    }

    public void buildCarriers() throws GameActionException {
        MapLocation closestWell = getWellToSpawnTowards();
        Direction spawnDir = movementDirections[rng.nextInt(movementDirections.length)];
        if (closestWell != null) {
            spawnDir = myLoc.directionTo(closestWell);
        }

        while(spawner.trySpawnGeneralDirection(RobotType.CARRIER, spawnDir)) {
            numCarriersSpawned++;
        }
    }

    // Spawn in direction of potentialHQLoc
    // TODO: If you're surrounded, then wait until you can spawn a few at a time and spawn all at once.
    public void buildLaunchers() throws GameActionException {
        MapLocation spawnLoc = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        MapLocation[] enemyHQLocs = getPotentialEnemyHQLocs();
        if(enemyHQLocs != null){
            spawnLoc = Util.getClosestMapLocation(enemyHQLocs);
        }
        Direction spawnDir = myLoc.directionTo(spawnLoc);

        while(spawner.trySpawnGeneralDirection(RobotType.LAUNCHER, spawnDir)) {
            numLaunchersSpawned++;
        }
    }
}
