package musashi2;

import battlecode.common.*;

import java.util.HashSet;

class LauncherHeuristic {
    double friendlyHP;
    double friendlyDamage;
    double enemyHP;
    double enemyDamage;
    // TODO: Factor this in. Retreat if you're about to get one shotted.
    double totalEnemyDamage;

    public LauncherHeuristic(double FH, double FD, double EH, double ED){
        friendlyHP = FH;
        friendlyDamage = FD;
        enemyHP = EH;
        enemyDamage = ED;
    }

    public boolean getSafe(){
        double myTurnsNeeded = enemyHP / friendlyDamage;
        double enemyTurnsNeeded = friendlyHP / enemyDamage;
//        Util.addToIndicatorString("FH:" + (int)friendlyHP + ",FD:" + (int)friendlyDamage);
//        Util.addToIndicatorString("EH:" + (int)enemyHP + ",ED:" + (int)enemyDamage);
//        Util.addToIndicatorString("MT:" + (int)myTurnsNeeded + ",ET:" + (int)enemyTurnsNeeded);
        // 1.5 simply because im ballsy and wanna go for it
        return myTurnsNeeded <= enemyTurnsNeeded * 1.0; // If you can kill them faster than they can kill you, return true
    }
}


// enum class to customize behaviours when we arrive at different types of targets
enum DestinationType {ENEMY_HQ, FRIENDLY_HQ, ENEMY_ISLAND, FRIENDLY_ISLAND, SYMMETRY, COMM_INFO};


public class Launcher extends Robot {

    private MapLocation targetLoc;

    int OFFENSIVE_THRESHOLD = 40;
    MapLocation[] enemyHQLocs = null;
    int enemyHQIdx = 0;
    RobotInfo[] nearbyFriendlies;
    RobotInfo[] nearbyActionEnemies;
    RobotInfo[] nearbyVisionEnemies;
    RobotInfo nearestEnemyInfo;
    LauncherHeuristic heuristic;
    MapLocation enemyCOM;
    boolean enemyInActionRadius;
    boolean enemyInVisionRadius;

    MapLocation spawningHQ;
    HashSet<MapLocation> locationsToIgnore = new HashSet<>();
    DestinationType destinationType = null;




    RobotInfo bestAttackVictim = null;
    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        spawningHQ = getNearestFriendlyHQ();    // location of HQ that spawned me
    }

    public void updateAllNearbyInfo() throws GameActionException{
        updateNearbyActionInfo();
        nearbyFriendlies = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam);
        nearbyVisionEnemies = rc.senseNearbyRobots(myType.visionRadiusSquared, opponent);
        nearestEnemyInfo = getNearestEnemy(nearbyVisionEnemies);
        heuristic = getHeuristic(nearbyFriendlies, nearbyVisionEnemies, nearestEnemyInfo);
        enemyCOM = getCenterOfMass(nearbyVisionEnemies);


        // don't consider HQs as an enemyHQ as an enemy in vision radius
        enemyInVisionRadius = nearbyVisionEnemies.length > 0;
        if(nearbyVisionEnemies.length == 1 && nearbyVisionEnemies[0].type == RobotType.HEADQUARTERS) enemyInVisionRadius = false;
    }

    public void updateNearbyActionInfo() throws GameActionException{
        nearbyActionEnemies = rc.senseNearbyRobots(myType.actionRadiusSquared, opponent);

        // don't consider HQs as an enemyHQ as an enemy in vision radius
        enemyInActionRadius = nearbyActionEnemies.length > 0;
        if(nearbyActionEnemies.length == 1 && nearbyActionEnemies[0].type == RobotType.HEADQUARTERS) enemyInActionRadius = false;

        bestAttackVictim = getBestAttackVictim();
    }

    //TODO: factor in island healing and headquarters damage
    //TODO: maybe have leaders?
    public void run() throws GameActionException{
        super.run();
        rc.setIndicatorDot(myLoc, 0, 255, 0);

        runAttackLoop();
        updateAllNearbyInfo();

        boolean isSafe = heuristic.getSafe();
        if(isSafe){
            Util.addToIndicatorString("SF");
        }
        else{
            Util.addToIndicatorString("USF");
        }
        // TODO: Consider HP and go back for healing
        if(isSafe){
            runSafeStrategy();
        }else{
            runUnsafeStrategy();
        }
        runAttackLoop();
    }


    public void runAttackLoop() throws GameActionException {
        updateNearbyActionInfo();
        boolean successfullyAttacked = runAttack();
        while(rc.isActionReady() && successfullyAttacked){
            updateNearbyActionInfo();
            successfullyAttacked = runAttack();
        }
    }


    public void runSafeStrategy() throws GameActionException{
        if(enemyInActionRadius) {
            if(rc.isActionReady()){
//                Util.log("Error: why didn't you attack?");
            }
            if(rc.isMovementReady()){
                moveBackIfAvoidsEnemy();
                moveBackIfAvoidsEnemy();
                Util.addToIndicatorString("AVD");
            }
        } else if (enemyInVisionRadius) {
            if(rc.isActionReady() && rc.isMovementReady()){
                // TODO: only move if it would get you in the range of attack?
                // TODO: instead of moving towards the COM, maybe instead use heuristic to determine where to move so that you move to the spot with the least enemies attacking?
                moveTowardsEnemyCOM();
            }
            else if (rc.isMovementReady()) {
//                moveTowardsEnemyCOM();
                moveBackIfAvoidsEnemy();
            }
        } else { // no enemy in sight
            //TODO: factor in when you saw enemies and started retreating.
//            Util.log("found symmetry");
//            if (haveUncommedIsland() || haveUncommedSymmetry()) {
//                returnToClosestHQ();
//            } else {
                runNormalOffensiveStrategy();
//            }
        }
    }


    public void runUnsafeStrategy() throws GameActionException{
        if(enemyInActionRadius){
            if(rc.isActionReady()){
//                Util.log("Error: why didn't you attack?");
            }
            if(rc.isMovementReady()){
                moveBackFromEnemy();
            }
        } else if (enemyInVisionRadius){
            moveBackFromEnemy();
        } else {
//            Util.log("There's no enemies nearby... Why the hell is this an unsafe environment?");
        }
    }



//    public boolean locationIsCrowded(MapLocation attackLoc) throws GameActionException{
//        // only run this method if we're within a certain radius of attackLoc
//        // check to see if you're within a certain radius of the target
//        // check to see if there are x soliders closer to the target than you
//        //       if so, this spot is crowded and you should be moving on
//        double OCCUPIED_FRACTION_THRESHOLD = 0.8;   // fixed reaction, over which we say a spot is crowded
//        int radius = myType.visionRadiusSquared;
//        int friendlies = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam).length;
//        int enemies = rc.senseNearbyRobots(myType.visionRadiusSquared, opponent).length;
//        int troopsWithinRadius  = friendlies - enemies;
//
//        MapInfo[] nearbyMapInfos = rc.senseNearbyMapInfos(attackLoc, radius);
//        int validMapSquares = 0;
//        for(int i = 0; i < nearbyMapInfos.length; i++){
//            MapInfo info = nearbyMapInfos[i];
//            if(info.isPassable()){       // possible squares we can sit at around attackLoc
//                validMapSquares++;      // possible squares you can go to
//            }
//        }
//        double occupiedFraction = (double)troopsWithinRadius / (double)validMapSquares;
//        if(occupiedFraction >= 0.8){
//            return true;
//        }
//        return false;
//    }






    // this method generates a sorted list of enemy HQ locations, sorted by distance from our HQ
    // returns null if there are no enemyHQs to visit that are not in locationsToIgnore
    public MapLocation getNextEnemyHQToVisit() throws GameActionException {
        MapLocation nextEnemyHQToVisit = null;
        int bestDistanceSquared = Integer.MAX_VALUE;
        MapLocation[] potentialLocs = getPotentialEnemyHQLocs();
        if(potentialLocs.length == 0){
            return null;
        }
        for(MapLocation loc : potentialLocs){
            if(locationsToIgnore.contains(loc)){
                continue;
            }
            int currDistanceSquared = spawningHQ.distanceSquaredTo(loc);
            if(nextEnemyHQToVisit == null || currDistanceSquared < bestDistanceSquared){
                nextEnemyHQToVisit = loc;
                bestDistanceSquared = currDistanceSquared;
            }
        }
        if(nextEnemyHQToVisit == null){
            locationsToIgnore.clear();
            return getNextEnemyHQToVisit();
        }


//        for (int i = 0; i < numHQs; i++) {
//            if (locationsToIgnore.contains(enemyHQLocs[i]))
//                continue; // locationsToIgnore will contain enemyHQLocs if we've already visited
//            int currDistanceSquared = spawningHQ.distanceSquaredTo(enemyHQLocs[i]);   // distance between spawningHQ and enemyHQ
//            int currDistanceSquared = myLoc.distanceSquaredTo(enemyHQLocs[i]);          // distance between current location and enemyHQ
//            if (nextEnemyHQToVisit == null || currDistanceSquared < bestDistanceSquared) {
//                bestDistanceSquared = currDistanceSquared;
//                nextEnemyHQToVisit = enemyHQLocs[i];
//            return enemyHQLocs[i];

//            }
//            return null;
//        }
//        return null;
//    }
        return nextEnemyHQToVisit;
    }


    // checks to see if any of our HQs need help and return that location
    // if multiple HQs need our help, go to the closest one
//    public MapLocation getNearestFriendlyHQToHelp() throws GameActionException {
//        MapLocation friendlyHQToHelp = null;
//        int bestDistanceSquared = Integer.MAX_VALUE;
//        for(int i=0; i<numHQs; i++){
//            if(comms.readCallForHelpFlag(constants.HQ_LOC_IDX_MAP[i])){
//                int currDistanceSquared = myLoc.distanceSquaredTo(HQlocs[i]);
//                if(currDistanceSquared < bestDistanceSquared){
//                    friendlyHQToHelp = HQlocs[i];
//                    bestDistanceSquared = currDistanceSquared;
//                }
//            }
//        }
//        return friendlyHQToHelp;
//    }



    public MapLocation getNearestFriendlyHQToHelp() throws GameActionException {
        double HEURISTIC_THRESHOLD = 3.0;
        for(int i=0; i<numHQs; i++){
            int diff = comms.readCallForHelpFlag(constants.HQ_LOC_IDX_MAP[i]);

            if(diff <= 0) continue;     // we don't need to help this island
            if(diff > 0 && targetLoc == null){      // if we currently don't have a target and this HQ is in trouble, go to that
                return HQlocs[i];
            }
            int distanceToHQ = (int)Math.sqrt(myLoc.distanceSquaredTo(HQlocs[i]));
            int distanceToTarget = (int)Math.sqrt(myLoc.distanceSquaredTo(HQlocs[i]));
            double heuristic = (double)(distanceToHQ - distanceToTarget) / (double) diff;

            Util.log("pot friendlyHQ: " + HQlocs[i]);
            Util.log("heuristic: " + heuristic);

            if(heuristic < HEURISTIC_THRESHOLD){
                Util.addToIndicatorString("H: " + heuristic);
                return HQlocs[i];
            }
        }

        return null;

//        MapLocation friendlyHQToHelp = null;
//        int bestDistanceSquared = Integer.MAX_VALUE;
//        for(int i=0; i<numHQs; i++){
//            if(comms.readCallForHelpFlag(constants.HQ_LOC_IDX_MAP[i])){
//                int currDistanceSquared = myLoc.distanceSquaredTo(HQlocs[i]);
//                if(currDistanceSquared < bestDistanceSquared){
//                    friendlyHQToHelp = HQlocs[i];
//                    bestDistanceSquared = currDistanceSquared;
//                }
//            }
//        }
//        return friendlyHQToHelp;
    }


    public MapLocation getNextTargetLoc() throws GameActionException{
        // run movement to determine symmetry

        enemyHQLocs = getPotentialEnemyHQLocs();

        // determine symmetry
//        if(enemyHQLocs.length != numHQs * Util.checkNumSymmetriesPossible()){
//            enemyHQLocs = getPotentialEnemyHQLocs();
//            enemyHQIdx = 0;
//            targetLoc = enemyHQLocs[enemyHQIdx];
//            destinationType = DestinationType.SYMMETRY;
//            // TODO: make launchers go to closestPotentialEnemyHQLocation
//            //  targetLoc = getClosestPotentialEnemyHQLocation();
//        }

        // go to friendlyHQ to comm info
        if (haveUncommedIsland() || haveUncommedSymmetry()){
            targetLoc = getNearestFriendlyHQ();
            destinationType = DestinationType.COMM_INFO;
        }


//        if(targetLoc == null){
//            // go to the nearest opposing island and destroy the enemy (hopefully)
//            targetLoc = getNearestOpposingIsland();        // if there's an enemy controlled island, go to that and kill the enemy
//            destinationType = DestinationType.ENEMY_ISLAND;
//        }
//
//
////      need to come up with something better
//        if(targetLoc == null) {
//            targetLoc = getNearestFriendlyHQToHelp();   // find a boi that needs some backup
//            destinationType = DestinationType.FRIENDLY_HQ;
//        }


        // go to the next enemyHQ to visit (and destroy ;))
        if(targetLoc == null){
            // get the next closest HQ that we haven't already visited
            // get the nearest hq that hasn't been visited
            targetLoc = getNextEnemyHQToVisit();
            destinationType = DestinationType.ENEMY_HQ;
        }

         if(targetLoc == null) destinationType = null;
        return targetLoc;
    }


    // this method takes care of going to different destination types
    //    - the code to go to a enemyHQ may be different than going to a well... (i think)
    // rn, we only use goTo differently if we're tryna move to an enmyHQ (so we don't get hurt by enemyHQ)
    // but we could expand this if we wanna have different behaviour around islands, hqs, or something else?
    //TODO: need to change what we do to navigate to different destinationTypes
    public boolean goToHandler() throws GameActionException {
        if(destinationType == null) return false;
        switch (destinationType){
            case ENEMY_HQ:
//                return nav.goToBug(targetLoc, myType.visionRadiusSquared+4);   // don't go too close to the enemyHQ
                return nav.goToFuzzy(targetLoc, myType.visionRadiusSquared+4);   // don't go too close to the enemyHQ
            default:
//                return nav.goToBug(targetLoc, myType.visionRadiusSquared+4);   // don't go too close to the enemyHQ
                return nav.goToFuzzy(targetLoc, 0);
        }
    }


    // this method checks if the launcher has arrived at its destination
    // note that "arrived" may be different depending on where we're trying to go
    public boolean checkIfArrived() throws GameActionException{
        if(targetLoc == null) return false;
        if(destinationType == null) return false;

        switch (destinationType){
            case ENEMY_HQ:
                return myLoc.distanceSquaredTo(targetLoc) < myType.visionRadiusSquared;   // don't go too close to the enemyHQ
            default:
                return myLoc.distanceSquaredTo(targetLoc) < 4;
        }
    }


    public void arrivedHandler() throws GameActionException{
        switch(destinationType){
            case ENEMY_HQ:
                locationsToIgnore.add(targetLoc); break;
            default:
                break;
        }
    }


    // this method checks to see if we should update our targetLoc
    // if we should update, it sets targetLoc to null
    public void rerouteHandler() throws GameActionException {
        // if we are going to an enemy island but it is no longer the closest enemy island --> reroute
        if(destinationType == DestinationType.ENEMY_ISLAND && targetLoc != getNearestOpposingIsland()){
            targetLoc = null;
            destinationType = null;
        }

        // if we are going to a friendly HQ but it no longer needs help --> reroute
        MapLocation nearestHQToHelp = getNearestFriendlyHQToHelp();
        if(destinationType == DestinationType.FRIENDLY_HQ && nearestHQToHelp != targetLoc){

            targetLoc = null;
            destinationType = null;
        }


        // if we are going to anywhere, but a friendlyHQ needs help, switch
        if(nearestHQToHelp != null && destinationType != DestinationType.FRIENDLY_HQ){
            if(destinationType == DestinationType.ENEMY_HQ){
                locationsToIgnore.add(targetLoc);
            }
            targetLoc = nearestHQToHelp;
            destinationType = DestinationType.FRIENDLY_HQ;
        }
    }



    // Go attack an enemy HQ
    //TODO: use previously calculated info from updateAllNearbyInfo to reduce the bytecode of this
    public void runNormalOffensiveStrategy() throws GameActionException {
//        rerouteHandler();   // check to see if we should change our targetLoc

        if(targetLoc != null && myLoc.distanceSquaredTo(targetLoc) < myType.visionRadiusSquared){
            locationsToIgnore.add(targetLoc);
            targetLoc = null;
        }

//        if(checkIfArrived()){
//            arrivedHandler();
//            targetLoc = null;
//        }

        if(targetLoc == null) {
            targetLoc = getNextTargetLoc();
        }

        if(targetLoc == null){          // if getNextTargetLoc didn't return anything, recylce all the locations in locationsToIgnore
            locationsToIgnore.clear();
            targetLoc = getNextTargetLoc(); // run getNextTargetLoc again to get the next HQ to go to (if nothing else comes up)
        }

        goToHandler();
        Util.addToIndicatorString("DEST:" + targetLoc); // Potential Enemy HQ
    }



    public void moveBackFromEnemy() throws GameActionException {
        // this works assuming that we've calculated enemyCOM already
//        Direction oppositeDir = myLoc.directionTo(enemyCOM).opposite();
        int xDisplacement = enemyCOM.x - myLoc.x;
        int yDisplacement = enemyCOM.y - myLoc.y;
        MapLocation target = new MapLocation(myLoc.x - xDisplacement*3, myLoc.y-yDisplacement*3);
        boolean moved = nav.goToFuzzy(target, 0);
        //TODO: if you can't move, what should you do?
    }


    public void moveBackIfAvoidsEnemy() throws GameActionException{
        bestAttackVictim = getBestAttackVictim();
        if(bestAttackVictim == null){
            return;
        }
        int xDisplacement = bestAttackVictim.location.x - myLoc.x;
        int yDisplacement = bestAttackVictim.location.y - myLoc.y;
        MapLocation target = new MapLocation(myLoc.x - xDisplacement*3, myLoc.y-yDisplacement*3);
        Direction bestDir = nav.fuzzyNav(target);
        MapLocation newLoc;
        for(Direction dir : Util.closeDirections(bestDir)){
            newLoc = myLoc.add(dir);
            if(newLoc.distanceSquaredTo(bestAttackVictim.location) > bestAttackVictim.type.actionRadiusSquared){
                Util.tryMove(bestDir);
            }
        }
    }


    public void moveTowardsEnemyCOM() throws GameActionException{
        nav.goToFuzzy(enemyCOM, 0);
    }


    public RobotInfo getBestAttackVictim(){
        // this method assumes nearbyActionEnemies was updated
        int toAttackIndex = -1;
        for(int i = 0; i < nearbyActionEnemies.length; i++) {
            // if it'd be better to attack enemies[i], change attackIndex to i
            if(rc.canAttack(nearbyActionEnemies[i].location)) {
                if(toAttackIndex == -1 || Util.attackCompare(nearbyActionEnemies[i], nearbyActionEnemies[toAttackIndex]) < 0) {
                    toAttackIndex = i;
                }
            }
        }

        if(toAttackIndex == -1 || nearbyActionEnemies[toAttackIndex].type == RobotType.HEADQUARTERS) return null;
        return nearbyActionEnemies[toAttackIndex];
    }


    // summary of this method:
    // check to see is attack is ready. if not, you can't do anything so return
    // check to see nearby enemies (note we sense nearby enemies several times in a round, so can optimize that)
    // find the enemy to attack by selecting the one with highest priority / lowest health
    // if you found an enemy, attack. otherwise, don't do anything.
    public boolean runAttack() throws GameActionException {
        // returns if we successfully attacked someone
        if(!rc.isActionReady()){
            return false;
        }
        // Try to attack someone
        // want to attack the one that's closest to dying
        // maybe want to attack
        // MapLocation toAttack = enemies[0].location;

        if(bestAttackVictim != null) {
            MapLocation toAttack = bestAttackVictim.location;
            rc.attack(toAttack);
            return true;
        }
        return false;
    }



    // summary of this method: trying to achieve the strat of staying on the border of the enemies' action radius so you don't get hit

    // if you're in a fight (if there's danger nearby), run fighting micro
    //      if you're within range, try attacking
    //      if you're not within range, move in, then try attacking
    //      if you couldn't attack, step away so the enemy can't hit you

    // otherwise (not in danger)
    //      - if we have uncommed islands, go to nearest hq so we can relay info
    //      - if we're in attacking mode (isAttacking == True), run AttackMovement(), which takes us to the nearest uncontrolled island or scouting location
    //      - otherwise, we're defending, so run defensive movement


    //TODO: make this smarter instead of just average
    public MapLocation getCenterOfMass(RobotInfo[] nearbyEnemies) {

        if(nearbyEnemies.length == 0) {
            return null;
        }
        int xSum = 0;
        int ySum = 0;
        for(RobotInfo info : nearbyEnemies) {
            xSum += info.location.x;
            ySum += info.location.y;
        }
        return new MapLocation(xSum/nearbyEnemies.length, ySum/ nearbyEnemies.length);
    }

    public boolean haveUncommedIsland() {
        for(int i = 0; i < numIslands; i++){
            IslandInfo info = islands[i];
            if(info != null && !info.commed){
                return true;
            }
        }
        return false;
    }


    public boolean haveUncommedSymmetry() {
        return !impossibleSymmetries.isEmpty();
    }


    public void returnToClosestHQ() throws GameActionException {
        targetLoc = getNearestFriendlyHQ();
        if(myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
            nav.goToFuzzy(targetLoc, 0);
            Util.addToIndicatorString("UNC,FUZ:" + targetLoc); // Uncommed info, fuzzying to targetLoc
        }
        else{
            nav.goToBug(targetLoc, 0);
            Util.addToIndicatorString("UNC,BUG:" + targetLoc); // Uncommed info, bugging to targetLoc
        }
    }


    public RobotInfo getNearestEnemy(RobotInfo[] nearbyEnemies) throws GameActionException {
        // Find nearest enemy
        RobotInfo nearestEnemyInfo = null;
        int minDist = Integer.MAX_VALUE;
        for(int i = 0; i < nearbyEnemies.length; i++){
            int dist = myLoc.distanceSquaredTo(nearbyEnemies[i].location);
            if(dist < minDist){
                minDist = dist;
                nearestEnemyInfo = nearbyEnemies[i];
            }
        }
        return nearestEnemyInfo;
    }


    // TODO: Consider carriers as one shot (decrease from friendly HP).
    // TODO: Consider total enemy damage, and if ur aboutta get yeeted then dip so you can get healed.
    public LauncherHeuristic getHeuristic(RobotInfo[] nearbyFriendlies, RobotInfo[] dangerousEnemies, RobotInfo nearestEnemyInfo) throws GameActionException { // TODO: Maybe only check # of attackers on the robot closest to you?
        //TODO: Fix this method?

        // your attack isn't ready, then don't engage

        if(nearestEnemyInfo == null){ // No enemies nearby, we safe
            return new LauncherHeuristic(100, 100, 0, 0.01);
        }


        double friendlyDamage = 0.0;
        double enemyDamage = 0.0;
        double friendlyHP = 0.0;
        double enemyHP = 0.0;
        double totalEnemyDamage = 0.0;

        // Calculate enemies attacking you

        // TODO: Sense the square and use actual cooldown instead of just using info.type.actionCooldown.
        for(int i = 0; i < dangerousEnemies.length; i++){
            RobotInfo info = dangerousEnemies[i];
            if(info.type == RobotType.LAUNCHER){
                enemyDamage += (double)info.type.damage / (double)info.type.actionCooldown;
                enemyHP += info.getHealth();
            }
        }

        // Calculate friendlies attacking the enemy
        for(int i = 0; i < nearbyFriendlies.length; i++){
            RobotInfo info = nearbyFriendlies[i];
            if(info.type != RobotType.LAUNCHER){
                continue;
            }
            if(info.getLocation().distanceSquaredTo(nearestEnemyInfo.getLocation()) > info.type.actionRadiusSquared){
                continue; // Only count friendlies that can attack said enemy
            }
            // TODO: Consider this.
//            if(info.getHealth() < health_to_retreat){
//                continue;
//            }
            friendlyDamage += (double)info.type.damage / (double)info.type.actionCooldown;
            friendlyHP += info.getHealth();
        }

        friendlyDamage += (double)myType.damage / (double)myType.actionCooldown;
        friendlyHP += rc.getHealth();

        return new LauncherHeuristic(friendlyHP, friendlyDamage, enemyHP, enemyDamage);
    }
}
