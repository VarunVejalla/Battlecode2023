package columbus;

import battlecode.common.*;

import java.util.ArrayList;

public class Headquarters extends Robot {

    ArrayList<MapLocation> wells;
    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);
        wells = new ArrayList<MapLocation>();
        checkForNearbyWells();
    }

    public void run() throws GameActionException{
        super.run();
        readComms();
        if(wells.size() > 0){
            buildCarriers();
        }
    }

    public void checkForNearbyWells() {
        WellInfo[] nearbyWells = rc.senseNearbyWells();
        for(WellInfo well : nearbyWells){
            this.wells.add(well.getMapLocation());
        }
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
        Direction spawnDir = directions[rng.nextInt(directions.length)];
        if (closestWell != null) {
            spawnDir = myLoc.directionTo(closestWell);
        }
        Util.trySpawnGeneralDirection(RobotType.CARRIER, spawnDir);
    }
}
