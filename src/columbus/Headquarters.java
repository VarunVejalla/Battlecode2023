package columbus;

import battlecode.common.*;

public class Headquarters extends Robot {

    MapLocation myLoc;
    int myIndex;

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);
        myLoc = rc.getLocation();
        computeIndex();
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

    public void run() throws GameActionException {
        super.run();
        readComms();
        Util.log("index " + myIndex);
        Util.log("loc from shared array: " + comms.readOurHQLocation(myIndex));

        comms.writeAdamantium(myIndex, rc.getResourceAmount(ResourceType.ADAMANTIUM));
        comms.writeMana(myIndex, rc.getResourceAmount(ResourceType.MANA));
//        comms.writeElixir(myIndex, rc.getResourceAmount(ResourceType.ELIXIR));
        Util.log("adamantium amount read from comms: " + comms.readAdamantium(myIndex));
        Util.log("mana amount read from comms: " + comms.readMana(myIndex));
//        Util.log("elixir amount read from comms: " + comms.readElixir(myIndex));

        buildCarriers();
    }

    public void readComms() {
        // TODO
    }

    public MapLocation getNearestWell(){
        int closestDist = Integer.MAX_VALUE;
        MapLocation closestWell = null;
        for(MapLocation well : wells){
            if(myLoc.distanceSquaredTo(well) < closestDist){
                closestWell = well;
            }
        }
        return closestWell;
    }

    public void buildCarriers() throws GameActionException {
        MapLocation closestWell = getNearestWell();
        Direction spawnDir = movementDirections[rng.nextInt(movementDirections.length)];
        if (closestWell != null) {
            spawnDir = myLoc.directionTo(closestWell);
        }
        Util.trySpawnGeneralDirection(RobotType.CARRIER, spawnDir);
    }
}