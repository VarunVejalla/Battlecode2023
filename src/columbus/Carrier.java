package columbus;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Carrier extends Robot {

    private MapLocation targetLoc;
    private MapLocation HQLoc;
    boolean mining = true;

    public Carrier(RobotController rc) throws GameActionException {
        super(rc);
        saveHQLoc();
    }

    public void run() throws GameActionException {
        super.run();
        int weight = totalResourceWeight();
        if(weight == GameConstants.CARRIER_CAPACITY){
            mining = false;
            targetLoc = null;
        }
        else if(weight == 0){
            mining = true;
            targetLoc = null;
        }
        if(mining){
            moveTowardsNearbyWell();
            tryMining();
        }
        else{
            moveTowardsHQ();
            tryTransferring();
        }
    }

    public void saveHQLoc(){
        for(RobotInfo info : rc.senseNearbyRobots()){
            if(info.getType() == RobotType.HEADQUARTERS && info.getTeam() == myTeam){
                HQLoc = info.getLocation();
            }
        }
    }

    public int totalResourceWeight() {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR);
    }

    public MapLocation getNearbyWell(){
        int closestDist = Integer.MAX_VALUE;
        MapLocation closestWell = null;
        for(MapLocation well : wells){
            int dist = myLoc.distanceSquaredTo(well);
            if(dist < closestDist){
                closestDist = dist;
                closestWell = well;
            }
        }
        return closestWell;
    }

    public MapLocation getRandomScoutingLocation() {
        int x = rng.nextInt(rc.getMapWidth());
        int y = rng.nextInt(rc.getMapHeight());
        return new MapLocation(x, y);
    }

    public void moveTowardsNearbyWell() throws GameActionException {
        if(targetLoc == null){
            MapLocation closestWell = getNearbyWell();
            if(closestWell != null){
                targetLoc = closestWell;
            }
            else{
                targetLoc = getRandomScoutingLocation();
            }
        }
        if(myLoc.distanceSquaredTo(targetLoc) > myType.actionRadiusSquared){
            nav.goToBug(targetLoc, myType.actionRadiusSquared);
        }
        else{
            nav.goToFuzzy(targetLoc, 0);
        }
    }

    public void moveTowardsHQ() throws GameActionException {
        if(targetLoc == null){
            targetLoc = HQLoc;
        }
        if(myLoc.distanceSquaredTo(targetLoc) > myType.actionRadiusSquared){
            nav.goToBug(targetLoc, myType.actionRadiusSquared);
        }
        else{
            nav.goToFuzzy(targetLoc, 0);
        }
    }

    public void tryTransferring() throws GameActionException {
        if(rc.canTransferResource(HQLoc, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM))){
            rc.transferResource(HQLoc, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM));
        }
        if(rc.canTransferResource(HQLoc, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA))){
            rc.transferResource(HQLoc, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA));
        }
        if(rc.canTransferResource(HQLoc, ResourceType.ELIXIR, rc.getResourceAmount(ResourceType.ELIXIR))){
            rc.transferResource(HQLoc, ResourceType.ELIXIR, rc.getResourceAmount(ResourceType.ELIXIR));
        }
    }

    public void tryMining() throws GameActionException {
        tryMine(ResourceType.ELIXIR);
        tryMine(ResourceType.MANA);
        tryMine(ResourceType.ADAMANTIUM);
    }

    public void tryMine(ResourceType type) throws GameActionException {
        int maxMineable = GameConstants.CARRIER_CAPACITY - totalResourceWeight();
        Util.tryMine(type, maxMineable);
    }
}