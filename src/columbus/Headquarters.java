package columbus;

import battlecode.common.*;

import java.util.ArrayList;

public class Headquarters extends Robot {

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);
    }

    public void run() throws GameActionException{
        super.run();
        readComms();
        buildCarriers();
    }

    public void readComms () {
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
