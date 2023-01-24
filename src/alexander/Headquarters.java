package alexander;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

class CarrierInfo {
    int roundNum;
    HashSet<Integer> carriers;
    public CarrierInfo(int roundNum){
        this.roundNum = roundNum;
        this.carriers = new HashSet<>();
    }
}

public class Headquarters extends Robot {

    int TIME_TO_FORGET_CARRIER = 35; // forget we've seen a carrier after this many rounds

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

//    int[] prevCommsArray = new int[63];
//    Queue<Integer> commsChanges = new LinkedList<>();

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);
        myLoc = rc.getLocation();
        computeIndex();
        if(myIndex == 0){
            comms.resetSymmetry();
        }
        spawner = new Spawner(rc, this);
        comms.writeOurHQLocation(myIndex, myLoc);
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
//        System.out.println("Bytecode before forgetting: " + Clock.getBytecodesLeft());
        forgetOldCarriers();
//        System.out.println("Bytecode after forgetting: " + Clock.getBytecodesLeft());
        addNewCarriers();
//        System.out.println("Bytecode after adding: " + Clock.getBytecodesLeft());
        indicatorString += ";C: " + carrierToRoundMap.size() + ";";
    }

    // criteria on whether hq should start saving up for an anchor
    //TODO: improve this criteria?
    public void shouldISaveUp() {
        savingUp = false;
//        double ratioOfUncontrolled = (double) getNumIslandsControlledByTeam(Team.NEUTRAL) / (double) rc.getIslandCount();
//        double howOftenToSpawnAnchors = 30.0 / ratioOfUncontrolled;
//        savingUp = adamantiumDeltaEMA > 6 && manaDeltaEMA > 6;
//        savingUp |= numCarriersSpawned > 2 && numLaunchersSpawned > 2 && adamantiumDeltaEMA > 2 && manaDeltaEMA > 2 && turnCount - lastAnchorBuiltTurn > howOftenToSpawnAnchors;
//        savingUp &= rc.getNumAnchors(Anchor.STANDARD) == 0; // Only save up for an anchor if you don't currently have one built
//        savingUp &= getNearestUncontrolledIsland() != null; // Only save up for an anchor if there's an unoccupied island somewhere
    }


    public void setResourceRatios() throws GameActionException{
        if(savingUp){   // if we're trying to make an anchor but don't have enough of a specific resource, get that resource
            boolean needAdamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM) < Anchor.STANDARD.getBuildCost(ResourceType.ADAMANTIUM);
            boolean needMana = rc.getResourceAmount(ResourceType.MANA) < Anchor.STANDARD.getBuildCost(ResourceType.MANA);

            if(needAdamantium && needMana) {
                comms.writeRatio(myIndex, 1, 1, 0);
                return;
            }
            else if(needAdamantium){
                comms.writeRatio(myIndex, 12, 3, 0);
                return;
            }
            else if(needMana) {
                comms.writeRatio(myIndex, 3, 12, 0);
                return;
            }
        }

//        if(seenCarriers.size() > initialCarrierThreshold){          // if we've made enough carriers, prioritize mana so we can make launchers
//            comms.writeRatio(myIndex, 4, 8, 0);
//            return;
//        }
//        else {
//            comms.writeRatio(myIndex, 8, 4, 0);        // we need to make more carriers, so prioritize adamantium
//            return;
//        }

        int numCarriers = carrierToRoundMap.size();
        if(numCarriers > 7) {
            comms.writeRatio(myIndex, 0, 15, 0);
        }
        else if(numCarriers < 3){
            comms.writeRatio(myIndex, 15, 0, 0);
        }
        else{
            comms.writeRatio(myIndex, 15 - numCarriers - 1, numCarriers + 1, 0);
        }

    }


    public void run() throws GameActionException {
        super.run();
        if(myIndex == 0){
//            updateCommsChangesQueue();
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

        shouldISaveUp();
        setResourceRatios();
        int[] ratio = comms.readRatio(myIndex);

        indicatorString += "Ratio Request - A:" + ratio[0] + ", M:" + ratio[1] + ", E:" + ratio[2] + "; ";
        if(savingUp){
            indicatorString += "saving up for anchor";

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
            indicatorString += "trying normal build order";
            buildLaunchers();
            buildCarriers();
        }

        // We only want to consider the rate at which we're gaining resources we don't wanna consider spending, so we wanna set
        // the prevMana and prevAdamantium variables AFTER we do all our spending.
        prevAdamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        prevMana = rc.getResourceAmount(ResourceType.MANA);
    }

//    public void updateCommsChangesQueue() throws GameActionException {
//        int newVal;
//        for(int i = 0; i < prevCommsArray.length; i++){
//            newVal = rc.readSharedArray(i);
//            if(newVal != prevCommsArray[i]){
//                commsChanges.add(i);
//            }
//            prevCommsArray[i] = newVal;
//        }
//        if(!commsChanges.isEmpty()){
//            int changedIdx = commsChanges.poll();
//            rc.writeSharedArray(63, changedIdx);
//        }
//
//    }

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
        readNewWellLocations(ResourceType.ADAMANTIUM, adamantiumWells, sortedClosestAdamantiumWells);
        readNewWellLocations(ResourceType.MANA, manaWells, sortedClosestManaWells);
        readNewWellLocations(ResourceType.ELIXIR, elixirWells, sortedClosestElixirWells);
    }

    public void readNewWellLocations(ResourceType type, MapLocation[] regionWells, ArrayList<MapLocation> sortedClosestWells) throws GameActionException {
        MapLocation newWellLoc = comms.getNewWellDetected(type);
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
        // TODO: Make the 10 a constant and also make it dynamic based on map size?
        if(sortedClosestWells.size() > 10){
            sortedClosestWells.remove(sortedClosestWells.size() - 1);
        }
    }

    public void buildCarriers() throws GameActionException {
        MapLocation closestWell = sortedClosestElixirWells.isEmpty() ? null : sortedClosestElixirWells.get(0);
        if(closestWell == null){
            closestWell = sortedClosestManaWells.isEmpty() ? null : sortedClosestManaWells.get(0);
        }
        if(closestWell == null){
            closestWell = sortedClosestManaWells.isEmpty() ? null : sortedClosestManaWells.get(0);
        }
        Direction spawnDir = movementDirections[rng.nextInt(movementDirections.length)];
        if (closestWell != null) {
            spawnDir = myLoc.directionTo(closestWell);
        }

        if(spawner.trySpawnGeneralDirection(RobotType.CARRIER, spawnDir)) {
            numCarriersSpawned++;
        }
    }

    public void buildLaunchers() throws GameActionException {
        Direction spawnDir = movementDirections[rng.nextInt(movementDirections.length)];

        if(spawner.trySpawnGeneralDirection(RobotType.LAUNCHER, spawnDir)) {
            numLaunchersSpawned++;
        }
    }
}
