package bell;

import battlecode.common.*;

public class Carrier extends Robot {

    private MapLocation targetLoc;
    boolean mining = true;

    public Carrier(RobotController rc) throws GameActionException {
        super(rc);
    }

    public void run() throws GameActionException {
        super.run();
        int weight = totalResourceWeight();
        if(weight == GameConstants.CARRIER_CAPACITY && mining){
            mining = false;
            targetLoc = null;
        }
        else if(weight == 0 && !mining){
            mining = true;
            targetLoc = null;
        }

        tryTakingAnchor();
//        Util.log("Anchor: " + rc.getAnchor());
        // If you're holding an anchor, make your top priority to deposit it.
        if(rc.getAnchor() != null){
//            Util.log("Moving towards nearest uncontrolled island");
            moveTowardsNearestUncontrolledIsland();
            tryPlacing();
        }
        // Otherwise, go mine
        else if(mining){
//            Util.log("Mining");
            moveTowardsNearbyWell();
            tryMining();
        }
        // If you're at full capacity, go deposit
        else{
//            Util.log("Moving towards HQ");
            moveTowardsHQ();
            tryTransferring();
        }
    }

    public int totalResourceWeight() {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ELIXIR);
    }

    public void tryTakingAnchor() throws GameActionException {
        if(rc.getAnchor() != null){
            return;
        }
        for(Direction dir : movementDirections){
            MapLocation adjLoc = myLoc.add(dir);
            if(rc.canTakeAnchor(adjLoc, Anchor.STANDARD)){
                rc.takeAnchor(adjLoc, Anchor.STANDARD);
                targetLoc = null;
                return;
            }
        }
    }

    public void moveTowardsNearestUncontrolledIsland() throws GameActionException {
        // If you're scouting and reach a dead end, reset.
        if(targetLoc != null && myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared && rc.canSenseLocation(targetLoc) && rc.senseIsland(targetLoc) == -1){
            targetLoc = null;
        }
        if(targetLoc == null){
            MapLocation closestUncontrolledIsland = getNearestUncontrolledIsland();
            if(closestUncontrolledIsland != null){
                targetLoc = closestUncontrolledIsland;
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

    public void moveTowardsNearbyWell() throws GameActionException {
        // If you're scouting and reach a dead end, reset.
        if(targetLoc != null && myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared && rc.canSenseLocation(targetLoc) && rc.senseWell(targetLoc) == null){
            targetLoc = null;
        }
        if(targetLoc == null){
            MapLocation closestWell = getNearestWell();
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

    // TODO: For now this is random, but we should have HQs set a flag or smth on comms for when they need a resource?
    public MapLocation getHQToReturnTo() {
        return HQlocs[rng.nextInt(numHQs)];
    }

    public void moveTowardsHQ() throws GameActionException {
        if(targetLoc == null){
            targetLoc = getHQToReturnTo();
        }
        if(myLoc.distanceSquaredTo(targetLoc) > myType.actionRadiusSquared){
            nav.goToBug(targetLoc, myType.actionRadiusSquared);
        }
        else{
            nav.goToFuzzy(targetLoc, 0);
        }
    }

    public void tryPlacing() throws GameActionException {
        if(rc.canPlaceAnchor()){
            rc.placeAnchor();
            targetLoc = null;
        }
    }

    public void tryTransferring() throws GameActionException {
        for(int i = 0; i < numHQs; i++){
            MapLocation HQLoc = HQlocs[i];
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