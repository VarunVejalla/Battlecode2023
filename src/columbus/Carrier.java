package columbus;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Carrier extends Robot {

    private MapLocation targetLoc;

    public Carrier(RobotController rc) throws GameActionException {
        super(rc);
    }

    public void run() throws GameActionException {
        super.run();
        moveTowardsNearbyWell();
        tryMining();
    }

    public MapLocation getNearbyWell(){
        WellInfo[] wells = rc.senseNearbyWells();
        int closestDist = Integer.MAX_VALUE;
        MapLocation closestWell = null;
        for(WellInfo well : wells){
            int dist = myLoc.distanceSquaredTo(well.getMapLocation());
            if(dist < closestDist){
                closestDist = dist;
                closestWell = well.getMapLocation();
            }
        }
        return closestWell;
    }

    public MapLocation getRandomScoutingLocation() {
        int x = rng.nextInt(rc.getMapWidth());
        int y = rng.nextInt(rc.getMapHeight());
        return new MapLocation(x, y);
    }

    public void setNewTargetLoc(){
        MapLocation closestWell = getNearbyWell();
        if(closestWell != null){
            targetLoc = closestWell;
            return;
        }
        targetLoc = getRandomScoutingLocation();
    }

    public void moveTowardsNearbyWell() throws GameActionException {
        if(targetLoc == null){
            setNewTargetLoc();
        }
        nav.goToFuzzy(targetLoc);
    }

    public void tryMining() throws GameActionException {
        Util.tryMine(ResourceType.ELIXIR, -1);
        Util.tryMine(ResourceType.MANA, -1);
        Util.tryMine(ResourceType.ADAMANTIUM, -1);
    }
}