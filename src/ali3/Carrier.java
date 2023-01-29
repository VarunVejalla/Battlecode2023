package ali3;

import battlecode.common.*;

import java.util.HashSet;

// TODO: Don't just run away from launchers, but also don't walk into squares where you could be attacked

public class Carrier extends Robot {

    private MapLocation targetLoc;
    private ResourceType targetResource;
    private int targetRegion = -1;
    boolean mining = true;
    int HQImHelpingIdx = -1;
    HashSet<Integer> regionsToIgnore = new HashSet<>();


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
            HQImHelpingIdx = -1;
            regionsToIgnore.clear();
        }

        tryTakingAnchor();
//        Util.log("Anchor: " + rc.getAnchor());
        // If you're holding an anchor, make your top priority to deposit it.
        boolean dangerNearby = runAway();
        if(!dangerNearby) {


            if (rc.getAnchor() != null) {
                moveTowardsNearestUncontrolledIsland();
                tryPlacing();
            }
            // Otherwise, go mine
            else if (mining) {
//            Util.log("Mining");
                moveTowardsNearbyWell();
                tryMining();
            }
            // If you're at full capacity, go deposit
            else {
//            Util.log("Moving towards HQ");
                moveTowardsHQ();
                tryTransferring();
            }
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
        if(targetLoc != null){
            // If you're scouting and reach a dead end, reset.
            if(rc.canSenseLocation(targetLoc) && rc.senseIsland(targetLoc) == -1){
                targetLoc = null;
            }

            // If the target island is no longer an uncontrolled island, reset.
            Team controllingTeam = getControllingTeam(targetLoc);
            if(controllingTeam != null && controllingTeam != myTeam){
                targetLoc = null;
            }
        }

        if(targetLoc == null){
            MapLocation closestUncontrolledIsland = getNearestUncontrolledIsland();
            if(closestUncontrolledIsland != null){
                targetLoc = closestUncontrolledIsland;
                Util.log("Moving towards nearest uncontrolled island");
            }
            else{
                // TODO: Highkey idt they shld go to a random scouting location, I think they shld j chill.
                targetLoc = getRandomScoutingLocation();
                Util.log("Scouting random location");
            }
        }
        if(myLoc.distanceSquaredTo(targetLoc) > myType.actionRadiusSquared){
            nav.goToBug(targetLoc, myType.actionRadiusSquared);
            Util.addToIndicatorString("UI.BG:" + targetLoc);
        }
        else{
            nav.goToFuzzy(targetLoc, 0);
            Util.addToIndicatorString("UI.FZ:" + targetLoc);
        }
    }

    public int determineWhichWellToGoTo(int HQImHelpingIdx, ResourceType type){
        MapLocation HQloc = HQlocs[HQImHelpingIdx];
        int bestRegion = -1;
        int bestDist = Integer.MAX_VALUE;
        MapLocation[] wellList = getWellList(type);
        for(int i = 0; i < wellList.length; i++){
            if(regionsToIgnore.contains(i)){
                continue;
            }
            MapLocation wellLoc = wellList[i];
            if(wellLoc == null){
                continue;
            }
            int dist = HQloc.distanceSquaredTo(wellLoc);
            if(dist < bestDist){
                bestRegion = i;
                bestDist = dist;
            }
        }
        return bestRegion;
    }
    public void resetWellLocation() throws GameActionException {
        MapLocation HQImHelping = getNearestFriendlyHQ();
        HQImHelpingIdx = getFriendlyHQIndex(HQImHelping);
//            MapLocation closestWell = getNearestWell(targetType);
        int bestRegion = determineWhichWellToGoTo(HQImHelpingIdx, targetResource);
        if(bestRegion != -1){
            MapLocation[] wellList = getWellList(targetResource);
            targetLoc = wellList[bestRegion];
            targetRegion = bestRegion;
        }
        if(targetLoc == null){
            targetLoc = getRandomScoutingLocation();
        }
    }

    public void moveTowardsNearbyWell() throws GameActionException {
        // If you're scouting and reach a dead end, reset.
        if(targetLoc != null && rc.canSenseLocation(targetLoc) && rc.senseWell(targetLoc) == null){
            Util.log("Resetting because there wasn't acc a well there");
            targetResource = null;
        }

        // Check if I should still go to the well
        if(targetResource != null && targetLoc != null && targetRegion != -1 && myLoc.distanceSquaredTo(targetLoc) > myType.actionRadiusSquared){
            // Check crowding
            if(checkWellCrowded(targetLoc)){
                // If there's crowding, go to the next nearest location
                regionsToIgnore.add(targetRegion);
                Util.log("Resetting because well is crowded");
                targetResource = null;
            }

            // If you're near the well, but you can't move towards the well, then you're prolly stuck
            // or there's crowding so go find a new well.
            else if(rc.canSenseLocation(targetLoc)){
                Direction fuzzyNavDir = nav.fuzzyNav(targetLoc);
                if(fuzzyNavDir == null){
                    regionsToIgnore.add(targetRegion);
                    Util.log("Resetting because I can't fuzzy nav towards it");
                    targetResource = null;
                }
            }
        }
        Util.addToIndicatorString("R:" + targetResource);
        if(targetResource == null || targetResource==ResourceType.ELIXIR){
            targetResource = Util.determineWhichResourceToGet(HQImHelpingIdx);
        }


        // Reset new target well location
//        if(targetLoc == null) {
            resetWellLocation();
//        }

        // Go to target well location
        if(myLoc.distanceSquaredTo(targetLoc) > myType.actionRadiusSquared){
            nav.goToBug(targetLoc, myType.actionRadiusSquared);
            Util.addToIndicatorString("NW.BG:" + targetLoc);
        }
        else{
            nav.goToFuzzy(targetLoc, 0);
            Util.addToIndicatorString("NW.FZ:" + targetLoc);
        }
    }

    public boolean checkWellCrowded(MapLocation wellLoc) throws GameActionException {
        if(wellLoc != null && rc.canSenseLocation(wellLoc)){
            int radius = 2;
            int troopsWithinRadius = rc.senseNearbyRobots(wellLoc, radius, myTeam).length;
            MapInfo[] nearbyMapInfos = rc.senseNearbyMapInfos(wellLoc, radius);
            int validMapSquares = 0;
            for(int i = 0; i < nearbyMapInfos.length; i++){
                MapInfo info = nearbyMapInfos[i];
                if(info.isPassable()){
                    validMapSquares++;
                }
            }
            if(troopsWithinRadius >= validMapSquares - 2){
                return true;
            }
        }
        return false;
    }

    public MapLocation getHQToReturnTo() {
        if(HQImHelpingIdx > -1) return HQlocs[HQImHelpingIdx];
        else return HQlocs[rng.nextInt(numHQs)];
    }

    public void moveTowardsHQ() throws GameActionException {
        if(targetLoc == null){
            targetLoc = getHQToReturnTo();
        }
        if(myLoc.distanceSquaredTo(targetLoc) > myType.actionRadiusSquared){
            nav.goToBug(targetLoc, myType.actionRadiusSquared);
            Util.addToIndicatorString("HQ.BG:" + targetLoc);
        }
        else{
            nav.goToFuzzy(targetLoc, 0);
            Util.addToIndicatorString("HQ.FZ:" + targetLoc);
        }
    }

    // Returns true if there's danger nearby, false if everything's safe
    public boolean runAway() throws GameActionException {
        // If you're in a fight w/ an enemy, run launcher micro
        RobotInfo[] enemies = rc.senseNearbyRobots(myType.visionRadiusSquared, opponent);
        RobotInfo closestDanger = null;
        for (RobotInfo info : enemies) {
//            if (info.type == RobotType.LAUNCHER || info.type == RobotType.CARRIER) {
            if (info.type == RobotType.LAUNCHER) {
                if (closestDanger == null || myLoc.distanceSquaredTo(info.location) < myLoc.distanceSquaredTo(closestDanger.location)) {
                    closestDanger = info;
                }
            }
        }
        if(closestDanger != null) {
            Util.log("Danger nearby! Gonna try running away");
            // Try to attack the enemy
            if(rc.isActionReady()){
                if(rc.canAttack(closestDanger.location)){
                    rc.attack(closestDanger.location);
                }
            }
            // Get the hell outta there.
            if (rc.isMovementReady()) {
                Direction enemyDir = myLoc.directionTo(closestDanger.location);
                MapLocation farAway = myLoc.subtract(enemyDir).subtract(enemyDir).subtract(enemyDir).subtract(enemyDir);
                nav.goToFuzzy(farAway, 0);
                Util.addToIndicatorString("DNG:" + closestDanger.location); // Danger!
            }
        }
        return closestDanger != null;
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
        if(targetResource != null){
            tryMine(targetResource);
        }

        // Default priority order of resource mining
        tryMine(ResourceType.ELIXIR);
        tryMine(ResourceType.MANA);
        tryMine(ResourceType.ADAMANTIUM);
    }

    public void tryMine(ResourceType type) throws GameActionException {
        int maxMineable = GameConstants.CARRIER_CAPACITY - totalResourceWeight();
        Util.tryMine(type, maxMineable);
    }
}