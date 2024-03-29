package genghis;

import battlecode.common.*;

class LauncherHeuristic {
    double friendlyHP;
    double friendlyDamage;
    double enemyHP;
    double enemyDamage;
    double totalEnemyDamage;

    public LauncherHeuristic(double FH, double FD, double EH, double ED){
        friendlyHP = FH;
        friendlyDamage = FD;
        enemyHP = EH;
        enemyDamage = ED;
    }

    public boolean getSafe(Robot robot){
        double myTurnsNeeded = enemyHP / friendlyDamage;
        double enemyTurnsNeeded = friendlyHP / enemyDamage;
//        System.out.println("Friendly HP: " + friendlyHP + ", DMG: " + friendlyDamage + ", Enemy HP: " + enemyHP + ", DMG: " + enemyDamage);
        robot.indicatorString += "EH: " + enemyHP+", FD: "+ friendlyDamage+";";
        robot.indicatorString += "MT: " + (int)myTurnsNeeded + ", ET: " + (int)enemyTurnsNeeded + "; ";

        // 1.5 simply because im ballsy and wanna go for it
        return myTurnsNeeded <= enemyTurnsNeeded * 1.0; // If you can kill them faster than they can kill you, return true
    }

}

public class Launcher extends Robot {

    private MapLocation targetLoc;

    boolean isOffensive = false;
    int DEFENSIVE_THRESHOLD = 15;
    int OFFENSIVE_THRESHOLD = 10;
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

    MapLocation lastAttacked;

    RobotInfo bestAttackVictim = null;
    MapLocation placeImDefending = null;


    // variables used for circling behaviour in runNormalDefensiveStrategy
    int defensiveMinDistToCircle = 0;
    int defensiveMaxDistToCircle = 4;


    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        determineMode();
    }

    public void updateAllNearbyInfo() throws GameActionException{
        updateNearbyActionInfo();
        nearbyFriendlies = rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam);
        nearbyVisionEnemies = rc.senseNearbyRobots(myType.visionRadiusSquared, opponent);
        nearestEnemyInfo = getNearestEnemy(nearbyVisionEnemies);
        heuristic = getHeuristic(nearbyFriendlies, nearbyVisionEnemies, nearestEnemyInfo);
        enemyCOM = getCenterOfMass(nearbyVisionEnemies);

        enemyInVisionRadius = nearbyVisionEnemies.length > 0;
    }

    public void updateNearbyActionInfo() throws GameActionException{
        nearbyActionEnemies = rc.senseNearbyRobots(myType.actionRadiusSquared, opponent);
        enemyInActionRadius = nearbyActionEnemies.length > 0;
        bestAttackVictim = getBestAttackVictim();
    }

    // this method is used in the constructor, runAttackMovement(), and runDefensiveMovement()
    //TODO: make this method dyamic based on how we're doing
    public void determineMode(){
        // look at enemyIslands vs homeIslands
        // if we're clearly winning, push it

        double defensiveProbability = 0.5;
        if(getNumIslandsControlledByTeam(myTeam) > getNumIslandsControlledByTeam(opponent) * 1.5){
            defensiveProbability = 0.2;   // make it more likely that we attack if we're clearly winning
        }
        double randomChoice = rng.nextDouble();
        if (randomChoice < defensiveProbability) isOffensive = false;    //defend with 70% probability if we're not clearly winning
        else isOffensive =true;
    }

    //TODO: factor in island healing and headquarters damage
    //TODO: maybe have leaders?
    // TODO: implement retreating to islands to regenerate health
    public void run() throws GameActionException{
        super.run();

        if(isOffensive){
            rc.setIndicatorDot(myLoc, 255, 0, 0);
            Util.log("Yam attacking");
        }
        else{
            rc.setIndicatorDot(myLoc, 0, 255, 0);
            Util.log(rc.getID() + ": Yam defending");
        }

//        if(rc.getRoundNum() < 500){
//            nav.circle(getNearestFriendlyHQ(), 20, 70);
//            return;
//        }

        updateNearbyActionInfo();
        boolean successfullyAttacked = runAttack();
        while(rc.isActionReady() && successfullyAttacked){
            updateNearbyActionInfo();
            successfullyAttacked = runAttack();
        }
        updateAllNearbyInfo();

        boolean isSafe = heuristic.getSafe(this);
        if(isSafe){
            indicatorString += "Safe;";
        }
        else{
            indicatorString += "Unsafe;";
        }
        if(isOffensive){
            if(isSafe){
                runSafeOffensiveStrategy();
            }else{
                runUnsafeOffensiveStrategy();
            }
        } else{
            if(isSafe){
                runSafeDefensiveStrategy();
            } else{
                runUnsafeDefensiveStrategy();
            }
        }
        updateNearbyActionInfo();
        successfullyAttacked = runAttack();
        while(rc.isActionReady() && successfullyAttacked){
            updateNearbyActionInfo();
            successfullyAttacked = runAttack();
        }
        
    }

    public void runSafeOffensiveStrategy() throws GameActionException{
        if(enemyInActionRadius) {
            if(rc.isActionReady()){
                Util.log("Error: why didn't you attack?");
            }
            if(rc.isMovementReady()){
                moveToSafestSpot();
                indicatorString += "Avoiding;";
            }
        } else if (enemyInVisionRadius) {
            if(rc.isActionReady() && rc.isMovementReady()){
                //TODO: move somewhere where you can hit the enemy but also hit by the least number of enemies
//                moveTowardsEnemyCOM();
                moveToBestPushLocation();
            } else if (rc.isMovementReady()) {
                // TODO: what should you do in this case (i.e. you're safe and the enemy is in vision radius)
//                moveTowardsEnemyCOM();
                moveToBestPushLocation();
            }
        } else { // no enemy in sight
            //TODO: factor in when you saw enemies and started retreating? Maybe go towards friends?
            if (haveUncommedIsland() || haveUncommedSymmetry()) {
                returnToClosestHQ();
            } else {
                runNormalOffensiveStrategy();
            }
        }
    }


    public void runUnsafeOffensiveStrategy() throws GameActionException{
        if(enemyInActionRadius){
            if(rc.isActionReady()){
                Util.log("Error: why didn't you attack?");
            }
            if(rc.isMovementReady()){
                moveToSafestSpot();
            }
        } else if (enemyInVisionRadius){
            moveToSafestSpot();
        } else {
            Util.log("There's no enemies nearby... Why the hell is this an unsafe environment?");
        }
    }

    public void runSafeDefensiveStrategy() throws GameActionException{
        if(enemyInActionRadius){
//            moveBackIfAvoidsEnemy();
            moveToSafestSpot();
        }

        else{
            runNormalDefensiveStrategy();
        }
    }


    public boolean moveToPlaceDefending() throws GameActionException {
        if(placeImDefending == null){
            return false;
        }
        return nav.goToFuzzy(placeImDefending, 0);
    }


    public void runUnsafeDefensiveStrategy() throws GameActionException{
        if (placeImDefending != null && myLoc.distanceSquaredTo(placeImDefending) <= myType.visionRadiusSquared){
           boolean moved = moveToPlaceDefending();
           if(moved) return;
        }
        isOffensive = true;
        runUnsafeOffensiveStrategy();
    }


    // Go attack an island
    // TODO: go attack enemy islands / wells
    public void runNormalOffensiveStrategy() throws GameActionException {
        if(targetLoc != null && myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
            targetLoc = null;
        }

        if(enemyHQLocs == null || enemyHQLocs.length != numHQs * Util.checkNumSymmetriesPossible()){
            enemyHQLocs = getPotentialEnemyHQLocs();
            enemyHQIdx = 0;
        }
        targetLoc = enemyHQLocs[enemyHQIdx];
//        targetLoc = getClosestPotentialEnemyHQLocation();
        // NOTE: Theoretically this shouldn't ever happen. If it did then our symmetry got fucked somehow.
        if(targetLoc == null){
            targetLoc = getRandomScoutingLocation();
        }

        indicatorString += "Attack Dest:" + targetLoc;
//        rc.setIndicatorString("HQ Attack Dest: " + targetLoc);

        if(myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){

            nav.circle(targetLoc, RobotType.HEADQUARTERS.actionRadiusSquared+1, myType.actionRadiusSquared);
            int numFriendlyLaunchers = Util.getNumTroopsInRange(myType.visionRadiusSquared, myTeam, RobotType.LAUNCHER);
            int numEnemyLaunchers = Util.getNumTroopsInRange(myType.visionRadiusSquared, opponent, RobotType.LAUNCHER);
            int numEnemyCarriers = Util.getNumTroopsInRange(myType.visionRadiusSquared, opponent, RobotType.CARRIER);
            if(numFriendlyLaunchers - (numEnemyLaunchers + numEnemyCarriers) > OFFENSIVE_THRESHOLD && myLoc.distanceSquaredTo(targetLoc) > 8) { // don't want to crowd any areas so leave if you're not super close
                {
                    enemyHQIdx++;
                    enemyHQIdx %= enemyHQLocs.length;
                    targetLoc = null;
                }
            }
        }
        else{
            nav.goToBug(targetLoc, myType.actionRadiusSquared);
        }
    }

//    public void moveBackFromEnemy() throws GameActionException {
//        // this works assuming that we've calculated enemyCOM already
////        Direction oppositeDir = myLoc.directionTo(enemyCOM).opposite();
//        int xDisplacement = enemyCOM.x - myLoc.x;
//        int yDisplacement = enemyCOM.y - myLoc.y;
//        MapLocation target = new MapLocation(myLoc.x - xDisplacement*3, myLoc.y-yDisplacement*3);
//        boolean moved = nav.goToFuzzy(target, 0);
//    }
//
//    public boolean moveBackIfAvoidsEnemy() throws GameActionException{
//        bestAttackVictim = getBestAttackVictim();
//        if(bestAttackVictim == null){
//            return false;
//        }
//        int xDisplacement = bestAttackVictim.location.x - myLoc.x;
//        int yDisplacement = bestAttackVictim.location.y - myLoc.y;
//        MapLocation target = new MapLocation(myLoc.x - xDisplacement*3, myLoc.y-yDisplacement*3);
//        Direction bestDir = nav.fuzzyNav(target);
//        MapLocation newLoc;
//        for(Direction dir : Util.closeDirections(bestDir)){
//            newLoc = myLoc.add(dir);
//            if(newLoc.distanceSquaredTo(bestAttackVictim.location) > bestAttackVictim.type.actionRadiusSquared){
//                if(Util.tryMove(bestDir)) return true;
//            }
//        }
//        return false;
//    }

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
            if(rc.canSenseLocation(possibleSpots[i]) && rc.senseMapInfo(possibleSpots[i]).isPassable()){
                newSpotIsValid[i] = true;
            }
        }

        for(RobotInfo enemy: nearbyVisionEnemies){         //loop over each enemy in vision radius

            // launchers and destabilizers
            if (enemy.type == RobotType.LAUNCHER || enemy.type == RobotType.DESTABILIZER) {
                for (int i = 0; i < 9; i++) {
                    // if the new spot is valid and an enemy can attack
                    if (newSpotIsValid[i] && enemy.location.distanceSquaredTo(possibleSpots[i]) <= enemy.getType().actionRadiusSquared) {
                        enemyDamage[i] += enemy.type.damage;
                        enemyPresentToAttack[i] = true;
                    }
                    sumOfDistanceSquaredToEnemies[i] += possibleSpots[i].distanceSquaredTo(enemy.location);
                }
            }


            // carriers
            else if (enemy.type == RobotType.CARRIER) {
                for (int i = 0; i < 9; i++) {
                    // if the new spot is valid and an enemy can attack us from there
                    if (newSpotIsValid[i] && enemy.location.distanceSquaredTo(possibleSpots[i]) <= enemy.getType().actionRadiusSquared) {
                        int massCarrying = enemy.getResourceAmount(ResourceType.MANA) + enemy.getResourceAmount(ResourceType.ADAMANTIUM) + enemy.getResourceAmount(ResourceType.ELIXIR);
                        enemyDamage[i] += (int) (massCarrying * 5/4);   // assume enemy will use their carriers to attack us
                        enemyPresentToAttack[i] = true;
                    }
                    sumOfDistanceSquaredToEnemies[i] += possibleSpots[i].distanceSquaredTo(enemy.location);
                }
            }


            // boosters can't hurt us, but they still are an enemy we would want to attack
            else if(enemy.type == RobotType.BOOSTER){
                for(int i = 0; i<9; i++){
                    if(!enemyPresentToAttack[i] && newSpotIsValid[i] && enemy.location.distanceSquaredTo(possibleSpots[i]) <= enemy.getType().actionRadiusSquared){
                            enemyPresentToAttack[i] = true;
                    }
                }
            }


            // headquarters
            else if (enemy.type == RobotType.HEADQUARTERS) {
                for (int i = 0; i < 9; i++) {
                    // if the new spot is valid and an enemy can attack us from there
                    if (newSpotIsValid[i] && enemy.location.distanceSquaredTo(possibleSpots[i]) <= enemy.getType().actionRadiusSquared) {
                        enemyDamage[i] += 4;    // hq's deal 4 damage for all bots in their action radius
                    }
                    sumOfDistanceSquaredToEnemies[i] += possibleSpots[i].distanceSquaredTo(enemy.location);
                }
            }
        }

        MapLocation bestSpot = null;
        double leastEnemyDamage = Double.MAX_VALUE;
        int greatestSumDistanceSquared = Integer.MIN_VALUE;


        for(int i=0; i<9; i++){
            // don't consider this new position if there is no enemy at the new location
            // also don't consider this new position if this spot is not valid
            if(!enemyPresentToAttack[i] || !newSpotIsValid[i]){
                continue;
            }

            // make this spot the new bestSpot if we currently don't have a best spot
            if(bestSpot == null ){
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

        nav.goToFuzzy(bestSpot, 0);
        Util.log("safest spot: " + bestSpot + ", with " + leastEnemyDamage + " damage with sumDistanceSquared " + greatestSumDistanceSquared);
    }


    // calculates the safest direction to move in that will
    //TODO: need to make sure this doesn't each up too much bytecode if we have a vision radius filled up with enemies
    public void moveToSafestSpot() throws GameActionException{
        MapLocation[] possibleSpots = new MapLocation[9];   // list of the possible spots we can go to on our next move
        boolean[] newSpotIsValid = new boolean[9];  // whether we can move to each new spot
        double[] enemyDamage = new double[9];   // contains the enemy damage you will receive at each new spot
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
            if(rc.canSenseLocation(possibleSpots[i]) && rc.senseMapInfo(possibleSpots[i]).isPassable()){
                newSpotIsValid[i] = true;
            }
        }

        for(RobotInfo enemy: nearbyVisionEnemies) {         //loop over each enemy in vision radius
            // launchers and destabilizers
            if (enemy.type == RobotType.LAUNCHER || enemy.type == RobotType.DESTABILIZER) {
                for (int i = 0; i < 9; i++) {
                    // if the new spot is valid and an enemy can attack
                    if (newSpotIsValid[i] && enemy.location.distanceSquaredTo(possibleSpots[i]) <= enemy.getType().actionRadiusSquared) {
                        enemyDamage[i] += enemy.type.damage;
                    }
                    sumOfDistanceSquaredToEnemies[i] += possibleSpots[i].distanceSquaredTo(enemy.location);
                }
            }

            // carriers
            else if (enemy.type == RobotType.CARRIER) {
                for (int i = 0; i < 9; i++) {
                    // if the new spot is valid and an enemy can attack us from there
                    if (newSpotIsValid[i] && enemy.location.distanceSquaredTo(possibleSpots[i]) <= enemy.getType().actionRadiusSquared) {
                        int massCarrying = enemy.getResourceAmount(ResourceType.MANA) + enemy.getResourceAmount(ResourceType.ADAMANTIUM) + enemy.getResourceAmount(ResourceType.ELIXIR);
                        enemyDamage[i] += (int) (massCarrying * 5/4);   // assume enemy will use their carriers to attack us
                    }
                    sumOfDistanceSquaredToEnemies[i] += possibleSpots[i].distanceSquaredTo(enemy.location);
                }
            }

            // headquarters
            else if (enemy.type == RobotType.HEADQUARTERS) {
                for (int i = 0; i < 9; i++) {
                    // if the new spot is valid and an enemy can attack us from there
                    if (newSpotIsValid[i] && enemy.location.distanceSquaredTo(possibleSpots[i]) <= enemy.getType().actionRadiusSquared) {
                        enemyDamage[i] += 4;    // hq's deal 4 damage for all bots in their action radius
                    }
                    sumOfDistanceSquaredToEnemies[i] += possibleSpots[i].distanceSquaredTo(enemy.location);
                }
            }
        }

        MapLocation bestSpot = myLoc;
        double leastEnemyDamage = nearbyActionEnemies.length;
        int greatestSumDistanceSquared = Integer.MIN_VALUE;

        for(int i=0; i<9; i++){
            // if the new spot will give us less enemy damage than the current best spot, make the new spot our best spot
            if(newSpotIsValid[i] && enemyDamage[i] < leastEnemyDamage){
                bestSpot = possibleSpots[i];
                leastEnemyDamage = enemyDamage[i];
                greatestSumDistanceSquared = sumOfDistanceSquaredToEnemies[i];
            }

            // if the new spot will give us the same enemy damage, but will move us farther away from the enemies, make the new spot our best spot
            else if(newSpotIsValid[i] &&
                    enemyDamage[i] == leastEnemyDamage &&
                    sumOfDistanceSquaredToEnemies[i] > greatestSumDistanceSquared){
                    bestSpot = possibleSpots[i];
                    leastEnemyDamage = enemyDamage[i];
                    greatestSumDistanceSquared = sumOfDistanceSquaredToEnemies[i];
            }
        }
        Util.log("safest spot: " + bestSpot + ", with " + leastEnemyDamage + " damage with sumDistanceSquared " + greatestSumDistanceSquared);
        nav.goToFuzzy(bestSpot, 0);
    }


    public void moveTowardsEnemyCOM() throws GameActionException{
        nav.goToFuzzy(enemyCOM, 0);
    }

    public int value(RobotInfo enemy) {
        if(enemy.type == RobotType.LAUNCHER) {
            return 0;
        } else if (enemy.type == RobotType.CARRIER) {
            return 1;
        } else if (enemy.type == RobotType.AMPLIFIER) {
            return 2;
        } else {
            return 3;
        }
    }

    public int compare(RobotInfo enemy1, RobotInfo enemy2) {
        // attack launchers, then carriers, then amplifiers, then other things
        int value1 = value(enemy1);
        int value2 = value(enemy2);
        if(value1 != value2) {
            return value1-value2;
        }
        else {
            // same type, attack whichever one is closer to dying
            return enemy1.health-enemy2.health;
        }
    }



    public RobotInfo getBestAttackVictim(){
        // this method assumes nearbyActionEnemies was updated
        int toAttackIndex = -1;
        for(int i = 0; i < nearbyActionEnemies.length; i++) {
            // if it'd be better to attack enemies[i], change attackIndex to i
            if(rc.canAttack(nearbyActionEnemies[i].location)) {
                if(toAttackIndex == -1 || compare(nearbyActionEnemies[i], nearbyActionEnemies[toAttackIndex]) < 0) {
                    toAttackIndex = i;
                }
            }
        }
        if(toAttackIndex == -1) return null;
        // don't attack HQs
        if(nearbyActionEnemies[toAttackIndex].getType() == RobotType.HEADQUARTERS) return null;
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
        if(bestAttackVictim != null) {
            MapLocation toAttack = bestAttackVictim.location;
            indicatorString += "Attacking";
            rc.attack(toAttack);
            return true;
        }
        return false;
    }


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
            if(info == null){
                continue;
            }
            if(!info.commed){
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
            indicatorString += "have uncommed info, fuzzying to HQ: " + targetLoc;
        }
        else{
            nav.goToBug(targetLoc, 0);
            indicatorString += "have uncommed info, bugging to HQ: " + targetLoc;
        }
    }


    // Go attack an island
//    public void runAttackMovement() throws GameActionException {
//        if(targetLoc != null && myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
//            targetLoc = null;
//        }
//
//        if(enemyHQLocs == null || enemyHQLocs.length != numHQs * Util.checkNumSymmetriesPossible()){
//            enemyHQLocs = getPotentialEnemyHQLocs();
//            enemyHQIdx = 0;
//        }
//        targetLoc = enemyHQLocs[enemyHQIdx];
////        targetLoc = getClosestPotentialEnemyHQLocation();
//        // NOTE: Theoretically this shouldn't ever happen. If it did then our symmetry got fucked somehow.
//        if(targetLoc == null){
//            targetLoc = getRandomScoutingLocation();
//        }
//
//        indicatorString += "going to " + targetLoc + " to attack";
////        targetLoc = getNearestUncontrolledIsland();
////        if(targetLoc == null){
////            targetLoc = getNearestOpposingIsland();
////        }
////        if(targetLoc == null)
////            targetLoc = getRandomScoutingLocation();
//
//        rc.setIndicatorString("going to " + targetLoc + " to attack potential enemy HQ");
//        if(myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
////            nav.goToFuzzy(targetLoc, 0);
//            nav.circle(targetLoc, 0, myType.actionRadiusSquared);
//
//            int numFriendlyLaunchers = Util.getNumTroopsInRange(myType.visionRadiusSquared, myTeam, RobotType.LAUNCHER);
//            int numEnemyLaunchers = Util.getNumTroopsInRange(myType.visionRadiusSquared, opponent, RobotType.LAUNCHER);
//            int numEnemyCarriers = Util.getNumTroopsInRange(myType.visionRadiusSquared, opponent, RobotType.CARRIER);
//
//            if(numFriendlyLaunchers - (numEnemyLaunchers + numEnemyCarriers) > OFFENSIVE_THRESHOLD && myLoc.distanceSquaredTo(targetLoc) > 8) { // don't want to crowd any areas so leave if you're not super close
//                {
////                    decideIfAttacking();
//                    enemyHQIdx++;
//                    enemyHQIdx %= enemyHQLocs.length;
//                    targetLoc = null;
//                }
//            }
//        }
//        else{
//            nav.goToBug(targetLoc, myType.actionRadiusSquared);
//        }
//    }


    //TODO: improve this method to defend all islands, wells, and hqs
    public void pickPlaceToDefend() throws GameActionException {
            MapLocation closestHQ = getNearestFriendlyHQ();
            int HQIdx = getFriendlyHQIndex(closestHQ);
            targetLoc = closestHQ;

//        targetLoc = null;
//        double defensiveChoice = rng.nextDouble();
//            if(defensiveChoice < 0.5){
//                targetLoc = getNearestFriendlyIsland();     // defend a friendly island with probability 50%
//            }
//
//            if(targetLoc == null){       // defend a well with probability 25%
//                MapLocation closestHQ = getNearestFriendlyHQ();
//                int HQIdx = getFriendlyHQIndex(closestHQ);
//                if(rng.nextBoolean()){      // randomly select between Adamantium and Mana
//                    targetLoc = comms.getClosestWell(HQIdx, ResourceType.ADAMANTIUM);
//                }
//                else{
//                    targetLoc = comms.getClosestWell(HQIdx, ResourceType.MANA);
//                }
//            }
    }


    // Go defend a well
    //  We should not crowd wells
    // maybe scale the guarding radius upwards as we see more friendly troops?
    // TODO: also defend islands (with the highest priority)
    // TODO: if enough launchers are already at you're place, go into attacking mode
    //TODO: if you're guarding a well that hasn't been in use, choose another target or become an attacker
    // TODO: do the push / pull micro attacking code, but stay further back than a carrier if you're a defender
    public void runNormalDefensiveStrategy() throws GameActionException {
        if(targetLoc == null){
            pickPlaceToDefend();
        }
        final double launcherToCarrierThresholdToMoveAway = 1/3;
        final double launcherToCarrierThresholdToMoveBack = 1/6;

        int numFriendlies = nearbyFriendlies.length;
        int numNearbyLaunchers = 0;
        int numNearbyCarriers = 0;

        for(RobotInfo info : nearbyFriendlies){
            if(info.type == RobotType.LAUNCHER) numNearbyLaunchers++;
            else if(info.type == RobotType.CARRIER) numNearbyCarriers++;
        }
        double launcherToCarrierRatio;
        if(numNearbyCarriers != 0) {
            launcherToCarrierRatio = numNearbyLaunchers / numNearbyCarriers;
        }
        else{
            launcherToCarrierRatio = Double.MAX_VALUE;
        }

        if(targetLoc == null || myLoc.distanceSquaredTo(targetLoc) > myType.visionRadiusSquared){
            isOffensive = true;
            runNormalOffensiveStrategy();
        }

        Util.log("launcherToCarrierRatio: " + launcherToCarrierRatio);

        //TODO: factor in enemy soldiers for whether or not you should move out
        if(launcherToCarrierRatio > launcherToCarrierThresholdToMoveAway){
            Util.log("adding to radii");
                defensiveMinDistToCircle += 1;
                defensiveMaxDistToCircle += 1;
        }

        if(launcherToCarrierRatio < launcherToCarrierThresholdToMoveBack){
            Util.log("subtracting from radii");

            defensiveMinDistToCircle -= 1;
            defensiveMaxDistToCircle -= 1;
        }

        Util.log("minDistToCircle: " + defensiveMinDistToCircle);
        Util.log("maxDistToCircle: " + defensiveMaxDistToCircle);
        nav.circle(targetLoc, defensiveMinDistToCircle, defensiveMaxDistToCircle);


//        if(targetLoc != null && rc.canSenseLocation(targetLoc) && rc.senseWell(targetLoc) == null){
//            targetLoc = null;
//        }
//
//        if(targetLoc == null){
//            // choose between defending an island, or well (and if well, what type of well?)
//            double defensiveChoice = rng.nextDouble();
//            if(defensiveChoice < 0.5){
//                targetLoc = getNearestFriendlyIsland();     // defend a friendly island with probability 50%
//            }
//            else{       // defend a well with probability 25%
//                MapLocation closestHQ = getNearestFriendlyHQ();
//                int HQIdx = getFriendlyHQIndex(closestHQ);
//                if(rng.nextBoolean()){      // randomly select between Adamantium and Mana
//                    targetLoc = comms.getClosestWell(HQIdx, ResourceType.ADAMANTIUM);
//                }
//                else{
//                    targetLoc = comms.getClosestWell(HQIdx, ResourceType.MANA);
//                }
//            }
//            if(targetLoc == null){
//                targetLoc = getRandomScoutingLocation();
//            }
//        }
//
//        indicatorString += "going to  " + targetLoc + " to defend";
//        int distanceSquaredToTarget = myLoc.distanceSquaredTo(targetLoc);
//        if(distanceSquaredToTarget <= myType.actionRadiusSquared){   // we have arrived
//            if(rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam).length > DEFENSIVE_THRESHOLD && distanceSquaredToTarget > 8) { // don't want to crowd any areas so leave if you're not super close
//                targetLoc = null;
//                determineMode();    // see if we should switch to attacking mode
//            }
//
//            else {
//                nav.circle(targetLoc, 2, myType.actionRadiusSquared);
//            }
//        }
//
//        else{
//            nav.goToBug(targetLoc, myType.actionRadiusSquared);
//        }
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


    // TODO: make sure this doesn't eat up too much bytecode in fights with lots of people
    // TODO: consider both friendly and enemy cooldowns (due to boosters or destabilizers or clouds)
    // TODO: should we also factor in headquarters in these calculations? We could loop over all soldiers in nearbyFriendlies and add 4 to enemyDamage for every bot in actionRadius of HQ, but that's pretty inefficient?
    public LauncherHeuristic getHeuristic(RobotInfo[] nearbyFriendlies, RobotInfo[] dangerousEnemies, RobotInfo nearestEnemyInfo) throws GameActionException {
        if(nearestEnemyInfo == null){ // No enemies nearby, we safe
            indicatorString += "NE1; ";
            return new LauncherHeuristic(100, 100, 0, 0.01);
        }
        Util.log("Nearest enemy Info: " + nearestEnemyInfo.location.toString());
        double friendlyDamage = 0.0;
        double enemyDamage = 0.0;
        double friendlyHP = 0.0;
        double enemyHP = 0.0;

        for(int i = 0; i < dangerousEnemies.length; i++){
            RobotInfo info = dangerousEnemies[i];
            if(info.type == RobotType.LAUNCHER || info.type == RobotType.DESTABILIZER){
                double attackCooldown = info.type.actionCooldown;
                enemyDamage += info.type.damage / attackCooldown;
                enemyHP += info.getHealth();
            }
            else if(info.type == RobotType.CARRIER){    //factor in damage that carrier can do (in one round)
                int resourceMass = info.getResourceAmount(ResourceType.MANA) + info.getResourceAmount(ResourceType.ADAMANTIUM) + info.getResourceAmount(ResourceType.ELIXIR);
                friendlyHP -=  (int)(5*resourceMass/4);
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
            double attackCooldown = info.type.actionCooldown;
            friendlyDamage += info.type.damage / attackCooldown;
            friendlyHP += info.getHealth();

        }
        friendlyDamage += myType.damage / myType.actionCooldown;
        friendlyHP += rc.getHealth();
        return new LauncherHeuristic(friendlyHP, friendlyDamage, enemyHP, enemyDamage);
    }



}
