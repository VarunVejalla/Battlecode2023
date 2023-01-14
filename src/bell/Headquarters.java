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
    double EMASmoothing = 3;   // parameter used in EMA --> higher value means we give more priority to recent changes


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


    public double computeAdamantiumDeltaEMA(){
        int currAdamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);

        if(rc.getRoundNum() > 0){
            double delta = currAdamantium - prevAdamantium;
            Util.log("delta" + delta);
            adamantiumDeltaEMA = delta * (EMASmoothing / (1+EMAWindowSize)) + adamantiumDeltaEMA * (1 - EMASmoothing / (1+EMAWindowSize));

        }
        prevAdamantium = currAdamantium;
        return adamantiumDeltaEMA;
    }


    public double computeManaDeltaEMA(){
        int currMana = rc.getResourceAmount(ResourceType.MANA);
        if(rc.getRoundNum() > 0){
            double delta = currMana - prevMana;
            manaDeltaEMA = delta * (EMASmoothing / (1+EMAWindowSize)) + manaDeltaEMA * (1 - EMASmoothing / (1+EMAWindowSize));
        }
        prevMana = currMana;
        return manaDeltaEMA;
    }


    public void run() throws GameActionException {
        super.run();
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


        if(numCarriersSpawned > 8 && numLaunchersSpawned > 4 && rc.getNumAnchors(Anchor.STANDARD) == 0){
            Util.log("Saving up for anchor!");
            if(rc.canBuildAnchor(Anchor.STANDARD)){
                rc.buildAnchor(Anchor.STANDARD);
            }
        }
        else{
            build();
        }

        rc.setIndicatorString(Double.toString(computeAdamantiumDeltaEMA()) + " " + Double.toString(computeManaDeltaEMA()));




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

    public void build() throws GameActionException {
        buildLaunchers();
        buildCarriers();
    }
}
