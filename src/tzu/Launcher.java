package tzu;

import battlecode.common.*;

import java.util.HashSet;

class LauncherHeuristic {
    double friendlyDamage;
    double enemyDamage;

    public LauncherHeuristic(double FD, double ED){
        friendlyDamage = FD;
        enemyDamage = ED;
    }

    public boolean getSafe(){
        Util.addToIndicatorString("FD:" + (int)friendlyDamage + ",ED:" + (int)enemyDamage);
        return friendlyDamage >= enemyDamage;
    }
}


// enum class to customize behaviours when we arrive at different types of targets
enum DestinationType {ENEMY_HQ, FRIENDLY_HQ, ENEMY_ISLAND, FRIENDLY_ISLAND, SYMMETRY, COMM_INFO};


public class Launcher extends Robot {

    private MapLocation targetLoc;

    int OFFENSIVE_THRESHOLD = 10;
    MapLocation[] enemyHQLocs = null;
    int enemyHQIdx = 0;
    RobotInfo[] nearbyFriendlies;
    RobotInfo[] nearbyActionEnemies;
    RobotInfo[] nearbyVisionEnemies;
    LauncherHeuristic heuristic;
    MapLocation enemyCOM;
    boolean enemyInActionRadius;
    boolean enemyInVisionRadius;

    MapLocation baseHQ;
    HashSet<MapLocation> locationsToIgnore = new HashSet<>();
    DestinationType destinationType = null;
    int islandDestinationIdx = -1;
    int numIslandsControlledByOpponent = 0;



    RobotInfo bestAttackVictim = null;
    boolean trynaHeal = false;
    MapLocation enemyChaseLoc = null;
    int turnsSinceChaseLocSet = 0;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        baseHQ = getNearestFriendlyHQ();    // location of HQ that spawned me
    }

    public void updateAllNearbyInfo() throws GameActionException{
        updateNearbyActionInfo();
        nearbyFriendlies = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam);
        nearbyVisionEnemies = rc.senseNearbyRobots(myType.visionRadiusSquared, opponent);
        Util.addToIndicatorString(String.valueOf(nearbyVisionEnemies.length)+";");
        heuristic = getHeuristic(nearbyFriendlies, nearbyVisionEnemies);
        enemyCOM = getCenterOfMass(nearbyVisionEnemies);
        enemyInVisionRadius = false;
        for(RobotInfo rob : nearbyVisionEnemies){
            if(rob.type != RobotType.HEADQUARTERS){
                enemyInVisionRadius = true;
            }
        }
    }

    public void updateNearbyActionInfo() throws GameActionException{
        nearbyActionEnemies = rc.senseNearbyRobots(myType.actionRadiusSquared, opponent);
        enemyInActionRadius = nearbyActionEnemies.length > 0;
        enemyInActionRadius = false;
        for(RobotInfo rob : nearbyActionEnemies){
            if(rob.type != RobotType.HEADQUARTERS){
                enemyInActionRadius = true;
            }
        }
        bestAttackVictim = getBestAttackVictim();
    }

    public void run() throws GameActionException{
        super.run();

        rc.setIndicatorDot(myLoc, 0, 255, 0);

        runAttackLoop();
        updateAllNearbyInfo();

        MapLocation nearestFriendlyIsland = getNearestFriendlyIsland();
        int returnToHealThreshold = Math.max(enemyDamageOneTurn(), constants.THRESHOLD_TO_GO_TO_ISLAND_TO_HEAL);
        returnToHealThreshold = Math.min(returnToHealThreshold, myType.getMaxHealth() / 2);
        if(rc.getHealth() < returnToHealThreshold && nearestFriendlyIsland != null){
            trynaHeal = true;
        }
        else if(rc.getHealth() == myType.getMaxHealth() || nearestFriendlyIsland == null){
            trynaHeal = false;
        }

        boolean isSafe = heuristic.getSafe();
        if(trynaHeal){
            Util.addToIndicatorString("TH");
            targetLoc = null;
            runHealingStrategy(nearestFriendlyIsland);
        }
        else if(isSafe){
            Util.addToIndicatorString("SF");
            runSafeStrategy();
        } else{
            Util.addToIndicatorString("USF");
            runUnsafeStrategy();
        }
        runAttackLoop();
        turnsSinceChaseLocSet++;
    }

    public void runAttackLoop() throws GameActionException {
        updateNearbyActionInfo();
        boolean successfullyAttacked = runAttack();
        while(rc.isActionReady() && successfullyAttacked){
            updateNearbyActionInfo();
            successfullyAttacked = runAttack();
        }
    }

    public void runSafeStrategy() throws GameActionException {
        if(enemyInActionRadius) {
            if(rc.isActionReady()){
                Util.log("Error: why didn't you attack?");
            }
            if(rc.isMovementReady()){
                moveToSafestSpot();
                Util.addToIndicatorString("Avd");
            }
            enemyChaseLoc = enemyCOM.add(myLoc.directionTo(enemyCOM));
            if(!rc.onTheMap(enemyChaseLoc)){
                enemyChaseLoc = enemyCOM;
            }
            turnsSinceChaseLocSet = 0;
        } else if (enemyInVisionRadius) {
            if(rc.isActionReady() && rc.isMovementReady()){
                moveToBestPushLocation();
            }
            else if (rc.isMovementReady()) {
                moveToSafestSpot();
            }
            enemyChaseLoc = enemyCOM.add(myLoc.directionTo(enemyCOM));
            if(!rc.onTheMap(enemyChaseLoc)){
                enemyChaseLoc = enemyCOM;
            }
            turnsSinceChaseLocSet = 0;
        } else { // no enemy in sight
            if (haveUncommedIsland() || haveUncommedSymmetry()) {
                returnToClosestHQ();
            } else if(enemyChaseLoc != null && !rc.canSenseLocation(enemyChaseLoc)){
                Util.addToIndicatorString("ECL:" + enemyChaseLoc);
                nav.goToFuzzy(enemyChaseLoc, 0);
            } else{
                enemyChaseLoc = null;
                runNormalOffensiveStrategy();
            }
        }
    }

    public void runUnsafeStrategy() throws GameActionException{
        enemyChaseLoc = null;
        if(enemyInActionRadius){
            if(rc.isActionReady()){
                Util.log("Error: why didn't you attack?");
            }
            if(rc.isMovementReady()){
                moveBackFromEnemy();
            }
        } else if (enemyInVisionRadius){
            moveBackFromEnemy();
        } else {
            Util.log("There's no enemies nearby... Why the hell is this an unsafe environment?");
        }
    }

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
            int currDistanceSquared = baseHQ.distanceSquaredTo(loc);
            if(nextEnemyHQToVisit == null || currDistanceSquared < bestDistanceSquared){
                nextEnemyHQToVisit = loc;
                bestDistanceSquared = currDistanceSquared;
            }
        }
        if(nextEnemyHQToVisit == null){
            locationsToIgnore.clear();
            return getNextEnemyHQToVisit();
        }
        return nextEnemyHQToVisit;
    }



    // TODO: if multiple HQs need help at the same time, should all troops go to the same HQ or split up?
    // TODO: tune this on bigger maps???
    public MapLocation getNearestFriendlyHQToHelp() throws GameActionException {
        double HEURISTIC_THRESHOLD = 3.0;
        for (int i = 0; i < numHQs; i++) {
            int diff = comms.readCallForHelpFlag(constants.HQ_LOC_IDX_MAP[i]);

            if (diff <= 0) continue;     // we don't need to help this island

            if (diff > 0 && targetLoc == null) {      // if we currently don't have a target and this HQ is in trouble, go to that
                return HQlocs[i];
            }

            int distanceToHQ = (int) Math.sqrt(myLoc.distanceSquaredTo(HQlocs[i]));
            int distanceToTarget = (int) Math.sqrt(myLoc.distanceSquaredTo(HQlocs[i]));
            double heuristic = (double) (distanceToHQ - distanceToTarget) / (double) diff;

            Util.log("pot friendlyHQ: " + HQlocs[i]);
            Util.log("heuristic: " + heuristic);

            if (heuristic < HEURISTIC_THRESHOLD) {
                Util.addToIndicatorString("H: " + heuristic);
                return HQlocs[i];
            }
        }
        return null;
    }


    public MapLocation getNextTargetLoc() throws GameActionException{
        // we don't need to run this because we run if before
//        numIslandsControlledByOpponent = getNumIslandsControlledByTeam(opponent);
        // go to friendlyHQ to comm info (first priority)
        if (haveUncommedIsland() || haveUncommedSymmetry()){
            targetLoc = getNearestFriendlyHQToHelp();
            destinationType = DestinationType.FRIENDLY_HQ;
            if(targetLoc == null){
                targetLoc = getNearestFriendlyHQ();
                destinationType = DestinationType.COMM_INFO;
            }
        }

        // if enemy has more than half the islands, neutralizing islands is the highest priority
        if(targetLoc == null && numIslandsControlledByOpponent >= numIslands*0.5){
            // go to the nearest opposing island and destroy the enemy (hopefully)
            islandDestinationIdx = getNearestOpposingIslandIdx();
            if(islandDestinationIdx != -1){
                targetLoc = islands[islandDestinationIdx].loc;
                destinationType = DestinationType.ENEMY_ISLAND;
            }
        }

        // if there's an HQ that needs help, go help
        if(targetLoc == null){
            targetLoc = getNearestFriendlyHQToHelp();   // find a boi that needs some backup
            destinationType = DestinationType.FRIENDLY_HQ;
        }

        // go to the next enemyHQ to visit (and destroy ;))
        if(targetLoc == null){
            // get the next closest HQ that we haven't already visited
            // get the nearest hq that hasn't been visited
            targetLoc = getNextEnemyHQToVisit();
            destinationType = DestinationType.ENEMY_HQ;
        }

        if(targetLoc == null && numIslandsControlledByOpponent > 0){
            islandDestinationIdx = getNearestOpposingIslandIdx();
            if(islandDestinationIdx != -1){
                targetLoc = islands[islandDestinationIdx].loc;
                destinationType = DestinationType.ENEMY_ISLAND;
            }
        }

        if(targetLoc == null) destinationType = null;
        return targetLoc;
    }


    // this method takes care of going to different destination types
    //    - the code to go to a enemyHQ may be different than going to a well... (i think)
    // rn, we only use goTo differently if we're tryna move to an enmyHQ (so we don't get hurt by enemyHQ)
    // but we could expand this if we wanna have different behaviour around islands, hqs, or something else?
    public boolean goToHandler() throws GameActionException {
        if(destinationType == null) return false;
        switch (destinationType){
            case ENEMY_HQ:
                return nav.goToBug(targetLoc, myType.visionRadiusSquared);   // don't go too close to the enemyHQ
            default:
                return nav.goToBug(targetLoc, 0);
        }
    }



    // this method checks to see if we should update our targetLoc
    // if we should update, it sets targetLoc to null
    public void rerouteHandler() throws GameActionException {
        int nearestOppIslandDestinationIdx = getNearestOpposingIslandIdx();
        MapLocation nearestHQToHelp = getNearestFriendlyHQToHelp();

        // if opponent controls more than 50% of islands
        if(numIslandsControlledByOpponent > 0.5*numIslands && nearestOppIslandDestinationIdx != islandDestinationIdx){
            islandDestinationIdx = nearestOppIslandDestinationIdx;
            targetLoc = islands[nearestOppIslandDestinationIdx].loc;
            destinationType = DestinationType.ENEMY_ISLAND;
        }

        // if one of the HQs needs help, go help it
        else if(nearestHQToHelp != null && nearestHQToHelp != targetLoc){
            targetLoc = nearestHQToHelp;
            destinationType = DestinationType.FRIENDLY_HQ;
        }
    }


    // returns an empty spot on an island that we can go to
    // TODO: sort these spots according to distance from myLoc?
    public MapLocation getEmptySpotOnIsland() throws GameActionException {
        if(islandDestinationIdx == -1) return null;
        MapLocation[] potentialSpots = rc.senseNearbyIslandLocations(islandDestinationIdx);
        for(MapLocation loc: potentialSpots){
            if(!rc.canSenseRobotAtLocation(loc)) return loc;    // if there is no robot at this location, return it
        }
        return null;
    }

    // Go attack an enemy HQ
    public void runNormalOffensiveStrategy() throws GameActionException {
        //this only gets called when there are no enemies in sight and you are safe
        numIslands = getNumIslandsControlledByTeam(opponent);
        rerouteHandler();   // check to see if we should change our targetLoc
        // check to see if we have arrived at our location
        // ------------------------------------------------------------------------------------------------------------
        // if we have arrived at an enemyHQ destination
        if(targetLoc != null && destinationType == DestinationType.ENEMY_HQ && myLoc.distanceSquaredTo(targetLoc) <= myType.visionRadiusSquared){
            locationsToIgnore.add(targetLoc);
            targetLoc = null;
        }

        // if we have arrived at an enemy island, stay on it until the island is neutralized
        // also, if there's someone already sitting on the spot you were going to, get another spot on the island
        if(targetLoc != null && destinationType == DestinationType.ENEMY_ISLAND && myLoc.distanceSquaredTo(targetLoc) <= myType.visionRadiusSquared){
            if(rc.canSenseRobotAtLocation(targetLoc)){  // if there is someone sitting at the spot we were trying to go to, get a new spot on the island
                targetLoc = getEmptySpotOnIsland();
            }
            Team controllingTeam = rc.senseTeamOccupyingIsland(islandDestinationIdx);
            if(controllingTeam != opponent){       // if the opponent is no longer occupying the island, we need to get another targetLoc
                islandDestinationIdx = -1;
                targetLoc = null;
            }
        }

        // if we have arrived at a friendlyHQ destination
        else if(targetLoc != null &&
                (destinationType == DestinationType.FRIENDLY_HQ || destinationType == DestinationType.COMM_INFO) &&
                myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
            locationsToIgnore.clear();  // reset our route to move to enemyHQ closer to this HQ
            baseHQ = targetLoc;
            targetLoc = null;
        }
        // ------------------------------------------------------------------------------------------------------------

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
        nav.goToFuzzy(target, 0);
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

    //TODO: make sure this method doesn't use too much bytecode
    public void moveToBestPushLocation() throws GameActionException{
        MapLocation[] possibleSpots = new MapLocation[9];   // list of the possible spots we can go to on our next move
        boolean[] newSpotIsValid = new boolean[9];  // whether or not we can move to each new spot
        double[] enemyDamage = new double[9];   // contains the enemy damage you will receive at each new spot
        int[] sumOfDistanceSquaredToEnemies = new int[9]; //contains the sum of distances to enemies from each new spot
        boolean[] enemyPresentToAttack = new boolean[9];    // contains whether or not there is a

        possibleSpots[0] = myLoc.add(Direction.NORTH);
        possibleSpots[1] = myLoc.add(Direction.NORTHEAST);
        possibleSpots[2] = myLoc.add(Direction.EAST);
        possibleSpots[3] = myLoc.add(Direction.SOUTHEAST);
        possibleSpots[4] = myLoc.add(Direction.SOUTH);
        possibleSpots[5] = myLoc.add(Direction.SOUTHWEST);
        possibleSpots[6] = myLoc.add(Direction.WEST);
        possibleSpots[7] = myLoc.add(Direction.NORTHWEST);
        possibleSpots[8] = myLoc;
        newSpotIsValid[8] = true;   // we know this spot is valid, because we're on it!

        // check if we can sense each new possible location, and that the new location is passable
        for(int i=0; i<8; i++){
            newSpotIsValid[i] = false;
            if(rc.canMove(myLoc.directionTo(possibleSpots[i]))){
                newSpotIsValid[i] = true;
            }
        }

        for(int i = 0; i < 9; i++) {
            if(!newSpotIsValid[i]){
                continue;
            }

            for (RobotInfo enemy : nearbyVisionEnemies) {         //loop over each enemy in vision radius
                if(possibleSpots[i].distanceSquaredTo(enemy.location) <= myType.actionRadiusSquared
                        && enemy.type != RobotType.HEADQUARTERS){
                    enemyPresentToAttack[i] = true;
                }
                if(possibleSpots[i].distanceSquaredTo(enemy.location) <= enemy.type.actionRadiusSquared){
                    enemyDamage[i] += Util.getEnemyDamage(enemy);
                }
                sumOfDistanceSquaredToEnemies[i] += possibleSpots[i].distanceSquaredTo(enemy.location);
            }
        }

        MapLocation bestSpot = null;
        double leastEnemyDamage = nearbyActionEnemies.length;
        int greatestSumDistanceSquared = Integer.MIN_VALUE;


        for(int i=0; i<9; i++){
            // don't consider this new position if there is no enemy at the new location
            // TODO: Hmm but what if you can't move to a square to attack the enemy but you're currently getting attacked, should you really stay there?
            // also don't consider this new position if this spot is not valid
            if(!enemyPresentToAttack[i] || !newSpotIsValid[i]){
                continue;
            }

            // make this spot the new bestSpot if we currently don't have a best spot
            if(bestSpot == null){
                bestSpot = possibleSpots[i];
                leastEnemyDamage = enemyDamage[i];
                greatestSumDistanceSquared = sumOfDistanceSquaredToEnemies[i];
            }

            // make this spot the new bestSpot if
            // 1) we receive less damage at this spot or
            // 2) we receive the same damage as the current best spot but we are further away from the enemies at the new spot
            else if(enemyDamage[i] < leastEnemyDamage || (enemyDamage[i] == leastEnemyDamage &&
                    sumOfDistanceSquaredToEnemies[i] > greatestSumDistanceSquared)){
                bestSpot = possibleSpots[i];
                leastEnemyDamage = enemyDamage[i];
                greatestSumDistanceSquared = sumOfDistanceSquaredToEnemies[i];
            }
        }

        if(bestSpot != null && !myLoc.equals(bestSpot)){
            rc.move(myLoc.directionTo(bestSpot));
        }

        Util.log("safest spot: " + bestSpot + ", with " + leastEnemyDamage + " damage with sumDistanceSquared " + greatestSumDistanceSquared);
    }

    // calculates the safest direction to move in that will
    //TODO: need to make sure this doesn't each up too much bytecode if we have a vision radius filled up with enemies
    public void moveToSafestSpot() throws GameActionException{
        MapLocation[] possibleSpots = new MapLocation[9];   // list of the possible spots we can go to on our next move
        boolean[] newSpotIsValid = new boolean[9];  // whether we can move to each new spot

        int[] sumOfDistanceSquaredToEnemies = new int[9]; //contains the sum of distances to enemies from each new spot

        possibleSpots[0] = myLoc.add(Direction.NORTH);
        possibleSpots[1] = myLoc.add(Direction.NORTHEAST);
        possibleSpots[2] = myLoc.add(Direction.EAST);
        possibleSpots[3] = myLoc.add(Direction.SOUTHEAST);
        possibleSpots[4] = myLoc.add(Direction.SOUTH);
        possibleSpots[5] = myLoc.add(Direction.SOUTHWEST);
        possibleSpots[6] = myLoc.add(Direction.WEST);
        possibleSpots[7] = myLoc.add(Direction.NORTHWEST);
        possibleSpots[8] = myLoc;
        newSpotIsValid[8] = true;   // we know this spot is valid, because we're on it!

        // check if we can sense each new possible location, and that the new location is passable
        for(int i=0; i<8; i++){
            newSpotIsValid[i] = false;
            if(rc.canMove(myLoc.directionTo(possibleSpots[i]))){
                newSpotIsValid[i] = true;
            }
        }

        double[] enemyDamage = DamageFinder.getDamages(myLoc, possibleSpots, newSpotIsValid, nearbyVisionEnemies);

        MapLocation bestSpot = myLoc;
        double leastEnemyDamage = Double.MAX_VALUE;
        int smallestSumDistanceSquared = Integer.MAX_VALUE;

        for(int i=0; i<9; i++){
            if(!newSpotIsValid[i]){
                continue;
            }

            // if the new spot will give us less enemy damage than the current best spot, make the new spot our best spot
            // if the new spot will give us the same enemy damage, but will move us closer to the enemies, make the new spot our best spot
            if(enemyDamage[i] < leastEnemyDamage){
//                    || (enemyDamage[i] == leastEnemyDamage &&
//                    sumOfDistanceSquaredToEnemies[i] < smallestSumDistanceSquared)){
                bestSpot = possibleSpots[i];
                leastEnemyDamage = enemyDamage[i];
//                smallestSumDistanceSquared = sumOfDistanceSquaredToEnemies[i];
            } else if(enemyDamage[i] == leastEnemyDamage && possibleSpots[i].distanceSquaredTo(enemyCOM) < bestSpot.distanceSquaredTo(enemyCOM)){
                bestSpot = possibleSpots[i];
                leastEnemyDamage = enemyDamage[i];
            }
        }
        Util.log("safest spot: " + bestSpot + ", with " + leastEnemyDamage + " damage ");//with sumDistanceSquared " + smallestSumDistanceSquared);

        if(bestSpot == null){
            rc.resign();
        }

        if(!bestSpot.equals(myLoc)){
            rc.move(myLoc.directionTo(bestSpot));
        }
//        System.out.println(Clock.getBytecodesLeft());
//        nav.goToFuzzy(bestSpot, 0);
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

        if(toAttackIndex == -1) return null;
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

    public LauncherHeuristic getHeuristic(RobotInfo[] nearbyFriendlies, RobotInfo[] nearbyEnemies) throws GameActionException {
        // your attack isn't ready, then don't engage

        if(nearbyEnemies.length == 0){ // No enemies nearby, we safe
            return new LauncherHeuristic(100, 0);
        }

        MapInfo[] nearbyFriendlyMapInfo = new MapInfo[nearbyFriendlies.length];
        MapInfo[] nearbyEnemyMapInfo = new MapInfo[nearbyEnemies.length];
        for(int i = 0; i < nearbyFriendlies.length; i++){
            nearbyFriendlyMapInfo[i] = rc.senseMapInfo(nearbyFriendlies[i].location);
        }
        for(int i = 0; i < nearbyEnemies.length; i++){
            nearbyEnemyMapInfo[i] = rc.senseMapInfo(nearbyEnemies[i].location);
        }

        double friendlyDamage = 0.0;
        double enemyDamage = 0.0;

        for(int i = 0; i < nearbyEnemies.length; i++){
            RobotInfo enemyInfo = nearbyEnemies[i];

//            if(enemyInfo.type != RobotType.HEADQUARTERS && enemyInfo.type != RobotType.LAUNCHER){
//                continue;
//            }
            if(enemyInfo.type != RobotType.LAUNCHER){
                continue;
            }

//            if(enemyInfo.type == RobotType.HEADQUARTERS){
//                for(int j = 0; j < nearbyFriendlies.length; j++){
//                    RobotInfo friendlyInfo = nearbyFriendlies[j];
//                    if(friendlyInfo.type != RobotType.LAUNCHER){
//                        continue;
//                    }
//                    if(friendlyInfo.location.isWithinDistanceSquared(enemyInfo.location, enemyInfo.type.visionRadiusSquared)){
//                        enemyDamage += (double)enemyInfo.type.damage / 10.0;
//                    }
//                }
//                // accounts for yourself
//                enemyDamage += (double) enemyInfo.type.damage / 10.0;
//                // Only consider the enemy launcher / carrier if it's in range of a friendly launcher.
//            } else
            if(enemyInfo.type == RobotType.LAUNCHER) {
                double cooldown = enemyInfo.type.actionCooldown * nearbyEnemyMapInfo[i].getCooldownMultiplier(opponent);
                enemyDamage += (double) enemyInfo.type.damage / cooldown;
            }
        }

        for(int j = 0; j < nearbyFriendlies.length; j++){
            RobotInfo friendlyInfo = nearbyFriendlies[j];
            if(friendlyInfo.type != RobotType.LAUNCHER && friendlyInfo.type != RobotType.HEADQUARTERS){
                continue;
            }
            if(friendlyInfo.type == RobotType.HEADQUARTERS) {
                for (int i = 0; i < nearbyEnemies.length; i++) {
                    RobotInfo enemyInfo = nearbyEnemies[i];
                    if (enemyInfo.type != RobotType.LAUNCHER) {
                        continue;
                    }
                    if (enemyInfo.location.isWithinDistanceSquared(friendlyInfo.location, friendlyInfo.type.actionRadiusSquared)) {
                        friendlyDamage += (double) friendlyInfo.type.damage / 10.0;
                    }
                }
            } else if(friendlyInfo.type == RobotType.LAUNCHER){
                for (int i = 0; i < nearbyEnemies.length; i++) {
                    RobotInfo enemyInfo = nearbyEnemies[i];
                    if (enemyInfo.type != RobotType.LAUNCHER) {
                        continue;
                    }
                    if (enemyInfo.location.isWithinDistanceSquared(friendlyInfo.location, friendlyInfo.type.actionRadiusSquared)) {
                        double cooldown = (double) friendlyInfo.type.actionCooldown * nearbyFriendlyMapInfo[j].getCooldownMultiplier(myTeam);
                        friendlyDamage += (double) friendlyInfo.type.damage / cooldown;
                        break;
                    }
                }
            }
        }

        //accounts for yourself
        Util.log("num enemies nearby: " + String.valueOf(nearbyEnemies.length));
        double cooldown = (double)myType.actionCooldown * rc.senseMapInfo(myLoc).getCooldownMultiplier(myTeam);
        friendlyDamage += (double)myType.damage / cooldown;

        return new LauncherHeuristic(friendlyDamage, enemyDamage);
    }
}
// TODO: Merge ali changes
