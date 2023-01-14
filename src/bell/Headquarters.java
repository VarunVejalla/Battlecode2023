package bell;

import battlecode.common.*;

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

    Spawner spawner;

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);
        myLoc = rc.getLocation();
        computeIndex();
        spawner = new Spawner(rc, this);

        comms.writeOurHQLocation(myIndex, myLoc);
        Util.log("index " + myIndex);
        Util.log("loc from shared array: " + comms.readOurHQLocation(myIndex));
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


    public void run() throws GameActionException {
        super.run();
        if(rc.getRoundNum() > 2){
            computeAdamantiumDeltaEMA();
            computeManaDeltaEMA();
        }

        rc.setIndicatorString(adamantiumDeltaEMA + " " + manaDeltaEMA);

        comms.writeAdamantium(myIndex, rc.getResourceAmount(ResourceType.ADAMANTIUM));
        comms.writeMana(myIndex, rc.getResourceAmount(ResourceType.MANA));
//        comms.writeElixir(myIndex, rc.getResourceAmount(ResourceType.ELIXIR));

        Util.log("Num carriers spawned: " + numCarriersSpawned);
        Util.log("Num launchers spawned: " + numLaunchersSpawned);

        // If you're past some threshold, then make sure you always have an anchor available (or save up for one) for a carrier to grab.

        // TODO: Come up with better criteria. One possibility: Keep track of your "adamantium (or mana) increase / turn" over the
        //       last 50 turns and if its above a certain rate, then you have enough carriers and you can save up for an anchor.
        // I implemented exponential moving average, which i think could solve this effeciently. see the computeEMA method above
        // might need to mess around with smoothing (EMASmoothing) and windowSize (EMAWindowSize) to get values on the proper scale

        // Criteria for saving up
        boolean savingUp = adamantiumDeltaEMA > 4 && manaDeltaEMA > 4;
        savingUp |= numCarriersSpawned > 2 && numLaunchersSpawned > 2 && adamantiumDeltaEMA > 2 && manaDeltaEMA > 2 && turnCount - lastAnchorBuiltTurn > 40;
        savingUp &= rc.getNumAnchors(Anchor.STANDARD) == 0; // Only save up for an anchor if you don't currently have one built
        savingUp &= getNearestUncontrolledIsland() != null; // Only save up for an anchor if there's an unoccupied island somewhere
        if(savingUp){
            Util.log("Saving up for anchor!");
            if(rc.canBuildAnchor(Anchor.STANDARD)){
                rc.buildAnchor(Anchor.STANDARD);
                lastAnchorBuiltTurn = turnCount;
            }
            if(rc.getResourceAmount(ResourceType.ADAMANTIUM) > Anchor.STANDARD.getBuildCost(ResourceType.ADAMANTIUM) + RobotType.CARRIER.buildCostAdamantium){
                buildCarriers();
            }
            if(rc.getResourceAmount(ResourceType.MANA) > Anchor.STANDARD.getBuildCost(ResourceType.MANA) + RobotType.LAUNCHER.buildCostMana){
                buildLaunchers();
            }
        }
        else {
            buildCarriers();
            buildLaunchers();
        }

        // We only want to consider the rate at which we're gaining resources we don't wanna consider spending, so we wanna set
        // the prevMana and prevAdamantium variables AFTER we do all our spending.
        prevAdamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        prevMana = rc.getResourceAmount(ResourceType.MANA);
    }

    public void buildCarriers() throws GameActionException {
        MapLocation closestWell = getNearestWell();
        Direction spawnDir = movementDirections[rng.nextInt(movementDirections.length)];
        if (closestWell != null) {
            spawnDir = myLoc.directionTo(closestWell);
        }

//        if(Util.trySpawnGeneralDirection(RobotType.CARRIER, spawnDir)){
        if(spawner.trySpawnGeneralDirection(RobotType.CARRIER, spawnDir)) {
            numCarriersSpawned++;
        }
    }

    public void buildLaunchers() throws GameActionException {
        Direction spawnDir = movementDirections[rng.nextInt(movementDirections.length)];
//        if(Util.trySpawnGeneralDirection(RobotType.LAUNCHER, spawnDir)){
//            numLaunchersSpawned++;
//        }
        if(spawner.trySpawnGeneralDirection(RobotType.LAUNCHER, spawnDir)) {
            numLaunchersSpawned++;
        }
    }
}
