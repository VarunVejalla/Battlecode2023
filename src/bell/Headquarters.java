package bell;

import battlecode.common.*;

public class Headquarters extends Robot {

    MapLocation myLoc;
    int myIndex;
    int numCarriersSpawned = 0;
    int numLaunchersSpawned = 0;

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);
        myLoc = rc.getLocation();
        computeIndex();
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
        if(numCarriersSpawned > 8 && numLaunchersSpawned > 4 && rc.getNumAnchors(Anchor.STANDARD) == 0){
            Util.log("Saving up for anchor!");
            if(rc.canBuildAnchor(Anchor.STANDARD)){
                rc.buildAnchor(Anchor.STANDARD);
            }
        }
        else{
            build();
        }
    }

    public void buildCarriers() throws GameActionException {
        MapLocation closestWell = getNearestWell();
        Direction spawnDir = movementDirections[rng.nextInt(movementDirections.length)];
        if (closestWell != null) {
            spawnDir = myLoc.directionTo(closestWell);
        }
        if(Util.trySpawnGeneralDirection(RobotType.CARRIER, spawnDir)){
            numCarriersSpawned++;
        }
    }

    public void buildLaunchers() throws GameActionException {
        Direction spawnDir = movementDirections[rng.nextInt(movementDirections.length)];
        if(Util.trySpawnGeneralDirection(RobotType.LAUNCHER, spawnDir)){
            numLaunchersSpawned++;
        }
    }

    public void build() throws GameActionException {
        buildLaunchers();
        buildCarriers();
    }
}
