package ali7;

import battlecode.common.*;

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

    RobotInfo bestAttackVictim = null;
    boolean trynaHeal = false;
    MapLocation enemyChaseLoc = null;
    int turnsSinceChaseLocSet = 0;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
    }

    public void updateAllNearbyInfo() throws GameActionException{
        updateNearbyActionInfo();
        nearbyFriendlies = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam);
        nearbyVisionEnemies = rc.senseNearbyRobots(myType.visionRadiusSquared, opponent);
        Util.addToIndicatorString(String.valueOf(nearbyVisionEnemies.length)+";");
        heuristic = getHeuristic(nearbyFriendlies, nearbyVisionEnemies);
        enemyCOM = getCenterOfMass(nearbyVisionEnemies);

        enemyInVisionRadius = nearbyVisionEnemies.length > 0;
    }

    public void updateNearbyActionInfo() throws GameActionException{
        nearbyActionEnemies = rc.senseNearbyRobots(myType.actionRadiusSquared, opponent);
        enemyInActionRadius = nearbyActionEnemies.length > 0;
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
                // ALEXANDER CHANGE
                moveBackIfAvoidsEnemy();
//                moveToSafestSpot();
                Util.addToIndicatorString("Avd");
            }
            enemyChaseLoc = enemyCOM.add(myLoc.directionTo(enemyCOM));
            if(!rc.onTheMap(enemyChaseLoc)){
                enemyChaseLoc = enemyCOM;
            }
            turnsSinceChaseLocSet = 0;
        } else if (enemyInVisionRadius) {
            if(rc.isActionReady() && rc.isMovementReady()){
                // ALEXANDER CHANGE
//                moveToBestPushLocation();
                moveTowardsEnemyCOM();
            }
            else if (rc.isMovementReady()) {
                // ALEXANDER CHANGE
//                moveToSafestSpot();
                moveBackIfAvoidsEnemy();
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

    // Go attack an enemy HQ
    public void runNormalOffensiveStrategy() throws GameActionException {
        //this only gets called when there are no enemies in sight and you are safe

        if(targetLoc != null && myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
            targetLoc = null;
        }

        // TODO: Also defend islands?
        if(enemyHQLocs == null || enemyHQLocs.length != numHQs * Util.checkNumSymmetriesPossible()){
            enemyHQLocs = getPotentialEnemyHQLocs();
            enemyHQIdx = 0;
        }
        if(enemyHQLocs == null){
            targetLoc = getRandomScoutingLocation();
        }
        else{
            targetLoc = enemyHQLocs[enemyHQIdx];
        }
        // NOTE: Theoretically this shouldn't ever happen. If it did then our symmetry got fucked somehow.

        Util.addToIndicatorString("PEHQ:" + targetLoc); // Potential Enemy HQ

        // TODO: This criteria should be a lil different for islands (you can go close to islands. You can't go close to enemy HQs).
        if(myLoc.distanceSquaredTo(targetLoc) <= myType.visionRadiusSquared){
//            nav.goToFuzzy(targetLoc, 0);
            nav.circle(targetLoc, RobotType.HEADQUARTERS.actionRadiusSquared + 1, myType.visionRadiusSquared);

            int numFriendlyLaunchers = 0;
            for(RobotInfo robot : nearbyFriendlies){
                if(robot.type == RobotType.LAUNCHER){
                    numFriendlyLaunchers += 1;
                }
            }

            if(numFriendlyLaunchers > OFFENSIVE_THRESHOLD && myLoc.distanceSquaredTo(targetLoc) > 8) { // don't want to crowd any areas so leave if you're not super close
                {
                    enemyHQIdx++;
                    if(enemyHQLocs == null){
                        enemyHQIdx = 0;
                    }
                    else{
                        enemyHQIdx %= enemyHQLocs.length;
                    }
                    targetLoc = null;
                }
            }
        }
        else{
            nav.goToBug(targetLoc, myType.visionRadiusSquared);
        }
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

//    //TODO: make sure this method doesn't use too much bytecode
//    public void moveToBestPushLocation() throws GameActionException{
//        MapLocation[] possibleSpots = new MapLocation[9];   // list of the possible spots we can go to on our next move
//        boolean[] newSpotIsValid = new boolean[9];  // whether or not we can move to each new spot
//        double[] enemyDamage = new double[9];   // contains the enemy damage you will receive at each new spot
//        int[] sumOfDistanceSquaredToEnemies = new int[9]; //contains the sum of distances to enemies from each new spot
//        boolean[] enemyPresentToAttack = new boolean[9];    // contains whether or not there is a
//
//        possibleSpots[0] = myLoc.add(Direction.NORTH);
//        possibleSpots[1] = myLoc.add(Direction.NORTHEAST);
//        possibleSpots[2] = myLoc.add(Direction.EAST);
//        possibleSpots[3] = myLoc.add(Direction.SOUTHEAST);
//        possibleSpots[4] = myLoc.add(Direction.SOUTH);
//        possibleSpots[5] = myLoc.add(Direction.SOUTHWEST);
//        possibleSpots[6] = myLoc.add(Direction.WEST);
//        possibleSpots[7] = myLoc.add(Direction.NORTHWEST);
//        possibleSpots[8] = myLoc;
//        newSpotIsValid[8] = true;   // we know this spot is valid, because we're on it!
//
//        // check if we can sense each new possible location, and that the new location is passable
//        for(int i=0; i<8; i++){
//            newSpotIsValid[i] = false;
//            if(rc.canMove(myLoc.directionTo(possibleSpots[i]))){
//                newSpotIsValid[i] = true;
//            }
//        }
//
//        for(int i = 0; i < 9; i++) {
//            if(!newSpotIsValid[i]){
//                continue;
//            }
//
//            for (RobotInfo enemy : nearbyVisionEnemies) {         //loop over each enemy in vision radius
//                if(possibleSpots[i].distanceSquaredTo(enemy.location) <= myType.actionRadiusSquared
//                        && enemy.type != RobotType.HEADQUARTERS){
//                    enemyPresentToAttack[i] = true;
//                }
//                if(possibleSpots[i].distanceSquaredTo(enemy.location) <= enemy.type.actionRadiusSquared){
//                    enemyDamage[i] += Util.getEnemyDamage(enemy);
//                }
//                sumOfDistanceSquaredToEnemies[i] += possibleSpots[i].distanceSquaredTo(enemy.location);
//            }
//        }
//
//        MapLocation bestSpot = null;
//        double leastEnemyDamage = nearbyActionEnemies.length;
//        int greatestSumDistanceSquared = Integer.MIN_VALUE;
//
//
//        for(int i=0; i<9; i++){
//            // don't consider this new position if there is no enemy at the new location
//            // TODO: Hmm but what if you can't move to a square to attack the enemy but you're currently getting attacked, should you really stay there?
//            // also don't consider this new position if this spot is not valid
//            if(!enemyPresentToAttack[i] || !newSpotIsValid[i]){
//                continue;
//            }
//
//            // make this spot the new bestSpot if we currently don't have a best spot
//            if(bestSpot == null){
//                bestSpot = possibleSpots[i];
//                leastEnemyDamage = enemyDamage[i];
//                greatestSumDistanceSquared = sumOfDistanceSquaredToEnemies[i];
//            }
//
//            // make this spot the new bestSpot if
//            // 1) we receive less damage at this spot or
//            // 2) we receive the same damage as the current best spot but we are further away from the enemies at the new spot
//            else if(enemyDamage[i] < leastEnemyDamage || (enemyDamage[i] == leastEnemyDamage &&
//                    sumOfDistanceSquaredToEnemies[i] > greatestSumDistanceSquared)){
//                bestSpot = possibleSpots[i];
//                leastEnemyDamage = enemyDamage[i];
//                greatestSumDistanceSquared = sumOfDistanceSquaredToEnemies[i];
//            }
//        }
//
//        if(bestSpot != null && !myLoc.equals(bestSpot)){
//            rc.move(myLoc.directionTo(bestSpot));
//        }
//
//        Util.log("safest spot: " + bestSpot + ", with " + leastEnemyDamage + " damage with sumDistanceSquared " + greatestSumDistanceSquared);
//    }

    // calculates the safest direction to move in that will
    //TODO: need to make sure this doesn't each up too much bytecode if we have a vision radius filled up with enemies
//    public void moveToSafestSpot() throws GameActionException{
//        MapLocation[] possibleSpots = new MapLocation[9];   // list of the possible spots we can go to on our next move
//        boolean[] newSpotIsValid = new boolean[9];  // whether we can move to each new spot
//
//        int[] sumOfDistanceSquaredToEnemies = new int[9]; //contains the sum of distances to enemies from each new spot
//
//        possibleSpots[0] = myLoc.add(Direction.NORTH);
//        possibleSpots[1] = myLoc.add(Direction.NORTHEAST);
//        possibleSpots[2] = myLoc.add(Direction.EAST);
//        possibleSpots[3] = myLoc.add(Direction.SOUTHEAST);
//        possibleSpots[4] = myLoc.add(Direction.SOUTH);
//        possibleSpots[5] = myLoc.add(Direction.SOUTHWEST);
//        possibleSpots[6] = myLoc.add(Direction.WEST);
//        possibleSpots[7] = myLoc.add(Direction.NORTHWEST);
//        possibleSpots[8] = myLoc;
//        newSpotIsValid[8] = true;   // we know this spot is valid, because we're on it!
//
//        // check if we can sense each new possible location, and that the new location is passable
//        for(int i=0; i<8; i++){
//            newSpotIsValid[i] = false;
//            if(rc.canMove(myLoc.directionTo(possibleSpots[i]))){
//                newSpotIsValid[i] = true;
//            }
//        }
//
//        double[] enemyDamage = DamageFinder.getDamages(myLoc, possibleSpots, newSpotIsValid, nearbyVisionEnemies);
//
//        MapLocation bestSpot = myLoc;
//        double leastEnemyDamage = Double.MAX_VALUE;
//        int smallestSumDistanceSquared = Integer.MAX_VALUE;
//
//        for(int i=0; i<9; i++){
//            if(!newSpotIsValid[i]){
//                continue;
//            }
//
//            // if the new spot will give us less enemy damage than the current best spot, make the new spot our best spot
//            // if the new spot will give us the same enemy damage, but will move us closer to the enemies, make the new spot our best spot
//            if(enemyDamage[i] < leastEnemyDamage){
////                    || (enemyDamage[i] == leastEnemyDamage &&
////                    sumOfDistanceSquaredToEnemies[i] < smallestSumDistanceSquared)){
//                bestSpot = possibleSpots[i];
//                leastEnemyDamage = enemyDamage[i];
////                smallestSumDistanceSquared = sumOfDistanceSquaredToEnemies[i];
//            } else if(enemyDamage[i] == leastEnemyDamage && possibleSpots[i].distanceSquaredTo(enemyCOM) < bestSpot.distanceSquaredTo(enemyCOM)){
//                bestSpot = possibleSpots[i];
//                leastEnemyDamage = enemyDamage[i];
//            }
//        }
//        Util.log("safest spot: " + bestSpot + ", with " + leastEnemyDamage + " damage ");//with sumDistanceSquared " + smallestSumDistanceSquared);
//
//        if(bestSpot == null){
//            rc.resign();
//        }
//
//        if(!bestSpot.equals(myLoc)){
//            rc.move(myLoc.directionTo(bestSpot));
//        }
////        System.out.println(Clock.getBytecodesLeft());
////        nav.goToFuzzy(bestSpot, 0);
//    }

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
