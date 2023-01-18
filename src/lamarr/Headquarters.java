package lamarr;

import battlecode.common.*;

import java.util.HashMap;
import java.util.Iterator;

public class Headquarters extends Robot {

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

    int timeToForgetCarrier = 50; // forget we've seen a carrier after this many rounds
    int initialCarrierThreshold = 10; //how many carriers an hq should see in its vision radius before transitioning over to ratio strategy

    boolean savingUp = false;
    Spawner spawner;
    HashMap<Integer, Integer> seenCarriers = new HashMap<Integer, Integer>();         // keys are the ids of carriers we've seen, values are the last round we say them

//    int[] prevCommsArray = new int[63];
//    Queue<Integer> commsChanges = new LinkedList<>();

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);
        myLoc = rc.getLocation();
        computeIndex();
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


    // TODO: Make this use less bytecode
    public void forgetOldCarriers() { // 7k bytecode!
        int roundNum = rc.getRoundNum();
        int forgetCarriersBeforeThisRound = roundNum - timeToForgetCarrier;

        // forget carriers that we haven't seen in the past "timeToForgetCarrier" rounds
        for(Iterator<HashMap.Entry<Integer, Integer>> it = seenCarriers.entrySet().iterator(); it.hasNext();){
            HashMap.Entry<Integer, Integer> entry = it.next();
            if(entry.getValue() < forgetCarriersBeforeThisRound){
                it.remove();
            }
        }
    }

    public void addNewCarriers() throws GameActionException { // 7k bytecode!
        int roundNum = rc.getRoundNum();
        // check the robots that may have deposited resources to you and add them to seenCarriers
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(RobotType.CARRIER.visionRadiusSquared, myTeam);
        for(RobotInfo info: nearbyRobots){
            if(info.type == RobotType.CARRIER){
                int id = info.getID();
                seenCarriers.put(id, roundNum);
            }
        }
    }

    // count the number of miners that we've seen in the past 50 rounds
    // This method will run fully every other round cuz it takes so much goddamn bytecode
    public void updateSeenCarriers() throws GameActionException {
        forgetOldCarriers();
        addNewCarriers();
//        System.out.println("# of carriers: " + seenCarriers.size());
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

        //TODO: maybe we can make this ratio dynamic based on adamantiumEMA and manaEMA i.e. manaEMA should be twice as large as adamantiumEMA or something like that
//        if(seenCarriers.size() > initialCarrierThreshold){          // if we've made enough carriers, prioritize mana so we can make launchers
//            comms.writeRatio(myIndex, 4, 8, 0);
//            return;
//        }
//        else {
//            comms.writeRatio(myIndex, 8, 4, 0);        // we need to make more carriers, so prioritize adamantium
//            return;
//        }
        int numCarriers = seenCarriers.size();
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
        }

        if(rc.getRoundNum() > 2){
            computeAdamantiumDeltaEMA();
            computeManaDeltaEMA();
        }

//        rc.setIndicatorString(adamantiumDeltaEMA + " " + manaDeltaEMA);
        comms.writeAdamantium(myIndex, rc.getResourceAmount(ResourceType.ADAMANTIUM));
        comms.writeMana(myIndex, rc.getResourceAmount(ResourceType.MANA));

        updateSeenCarriers();
        Util.log("Seen carriers: " + seenCarriers.size());

        shouldISaveUp();
        setResourceRatios();
        int[] ratio = comms.readRatio(myIndex);

        if(savingUp){
            rc.setIndicatorString("Ratio Request - A:" + ratio[0] + ", M:" + ratio[1] + ", E:" + ratio[2] + "; saving up for anchor");

            Util.log("Saving up for anchor!");
            if(rc.canBuildAnchor(Anchor.STANDARD)){
                rc.buildAnchor(Anchor.STANDARD);
                lastAnchorBuiltTurn = turnCount;
                savingUp = false;
            }
            if(rc.getResourceAmount(ResourceType.ADAMANTIUM) > Anchor.STANDARD.getBuildCost(ResourceType.ADAMANTIUM) + RobotType.CARRIER.buildCostAdamantium){
                buildCarriers();
            }
            if(rc.getResourceAmount(ResourceType.MANA) > Anchor.STANDARD.getBuildCost(ResourceType.MANA) + RobotType.LAUNCHER.buildCostMana){
                buildLaunchers();
            }
        }

        else {
            if(seenCarriers.size() < initialCarrierThreshold) {
                rc.setIndicatorString("Ratio Request - A:" + ratio[0] + ", M:" + ratio[1] + ", E:" + ratio[2] + "; trying to build carriers");
                buildCarriers();
                buildLaunchers();
            }
            else {
                rc.setIndicatorString("Ratio Request - A:" + ratio[0] + ", M:" + ratio[1] + ", E:" + ratio[2] + "; trying to build launchers");
                buildLaunchers();
                buildCarriers();
            }
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

    public void buildCarriers() throws GameActionException {
        MapLocation closestWell = getNearestWell();
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
