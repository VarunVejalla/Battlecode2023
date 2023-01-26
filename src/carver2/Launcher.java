package carver2;

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

    // TODO: Replace this with an actually good strategy
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

        runAttackLoop();
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

    public void runSafeOffensiveStrategy() throws GameActionException{
        if(enemyInActionRadius) {
            if(rc.isActionReady()){
                Util.log("Error: why didn't you attack?");
            }
            if(rc.isMovementReady()){
                moveBackIfAvoidsEnemy();
                indicatorString += "Avoiding;";
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
            //TODO: factor in when you saw enemies and started retreating?
            if (haveUncommedIsland() || haveUncommedSymmetry()) {
                returnToClosestHQ();
            } else {
                runNormalOffensiveStrategy();
            }
        }
    }

    //TODO: implement this
    public void runUnsafeOffensiveStrategy() throws GameActionException{
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

    //TODO: implement this
    public void runSafeDefensiveStrategy() throws GameActionException{
        runSafeOffensiveStrategy();
    }

    //TODO: implement this
    public void runUnsafeDefensiveStrategy() throws GameActionException{
        runUnsafeOffensiveStrategy();
    }

    // Go attack an enemy HQ
    //TODO: use previously calculated info from updateAllNearbyInfo to reduce the bytecode of this
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

        indicatorString += "going to " + targetLoc + " to attack";
//        targetLoc = getNearestUncontrolledIsland();
//        if(targetLoc == null){
//            targetLoc = getNearestOpposingIsland();
//        }
//        if(targetLoc == null)
//            targetLoc = getRandomScoutingLocation();

        rc.setIndicatorString("going to " + targetLoc + " to attack potential enemy HQ");
        if(myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
//            nav.goToFuzzy(targetLoc, 0);
            nav.circle(targetLoc, 0, myType.actionRadiusSquared);

            int numFriendlyLaunchers = Util.getNumTroopsInRange(myType.visionRadiusSquared, myTeam, RobotType.LAUNCHER);
            int numEnemyLaunchers = Util.getNumTroopsInRange(myType.visionRadiusSquared, opponent, RobotType.LAUNCHER);
            int numEnemyCarriers = Util.getNumTroopsInRange(myType.visionRadiusSquared, opponent, RobotType.CARRIER);

            if(numFriendlyLaunchers - (numEnemyLaunchers + numEnemyCarriers) > OFFENSIVE_THRESHOLD && myLoc.distanceSquaredTo(targetLoc) > 8) { // don't want to crowd any areas so leave if you're not super close
                {
//                    decideIfAttacking();
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
            indicatorString += "Attacking";
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
    //TODO: determine symmetry of map and try to surround HQs?
    public void runAttackMovement() throws GameActionException {
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

        indicatorString += "going to " + targetLoc + " to attack";
//        targetLoc = getNearestUncontrolledIsland();
//        if(targetLoc == null){
//            targetLoc = getNearestOpposingIsland();
//        }
//        if(targetLoc == null)
//            targetLoc = getRandomScoutingLocation();

        rc.setIndicatorString("going to " + targetLoc + " to attack potential enemy HQ");
        if(myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
//            nav.goToFuzzy(targetLoc, 0);
            nav.circle(targetLoc, 0, myType.actionRadiusSquared);

            int numFriendlyLaunchers = Util.getNumTroopsInRange(myType.visionRadiusSquared, myTeam, RobotType.LAUNCHER);
            int numEnemyLaunchers = Util.getNumTroopsInRange(myType.visionRadiusSquared, opponent, RobotType.LAUNCHER);
            int numEnemyCarriers = Util.getNumTroopsInRange(myType.visionRadiusSquared, opponent, RobotType.CARRIER);

            if(numFriendlyLaunchers - (numEnemyLaunchers + numEnemyCarriers) > OFFENSIVE_THRESHOLD && myLoc.distanceSquaredTo(targetLoc) > 8) { // don't want to crowd any areas so leave if you're not super close
                {
//                    decideIfAttacking();
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

    // Go defend a well
    //  We should not crowd wells
    // maybe scale the guarding radius upwards as we see more friendly troops?
    // TODO: also defend islands (with the highest priority)
    // TODO: if enough launchers are already at you're place, go into attacking mode
    public void runDefensiveMovement() throws GameActionException {
        if(targetLoc != null && rc.canSenseLocation(targetLoc) && rc.senseWell(targetLoc) == null){
            targetLoc = null;
        }

        if(targetLoc == null){
            // choose between defending an island, or well (and if well, what type of well?)
            double defensiveChoice = rng.nextDouble();
            if(defensiveChoice < 0.5){
                targetLoc = getNearestFriendlyIsland();     // defend a friendly island with probability 50%
            }
            else{       // defend a well with probability 25%
                MapLocation closestHQ = getNearestFriendlyHQ();
                int HQIdx = getFriendlyHQIndex(closestHQ);
                // TODO: Be smart about choosing which well to go to.
                if(rng.nextBoolean()){      // randomly select between Adamantium and Mana
                    targetLoc = comms.getClosestWell(HQIdx, ResourceType.ADAMANTIUM);
                }
                else{
                    targetLoc = comms.getClosestWell(HQIdx, ResourceType.MANA);
                }
            }
            if(targetLoc == null){
                targetLoc = getRandomScoutingLocation();
            }
        }

        indicatorString += "going to  " + targetLoc + " to defend";
        int distanceSquaredToTarget = myLoc.distanceSquaredTo(targetLoc);
        if(distanceSquaredToTarget <= myType.actionRadiusSquared){   // we have arrived
            if(rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam).length > DEFENSIVE_THRESHOLD && distanceSquaredToTarget > 8) { // don't want to crowd any areas so leave if you're not super close
                targetLoc = null;
                determineMode();    // see if we should switch to attacking mode
            }

            else {
                nav.circle(targetLoc, 2, myType.actionRadiusSquared);
            }
        }

        else{
            nav.goToBug(targetLoc, myType.actionRadiusSquared);
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

    // TODO Count the # of soldiers on their front lines? I'm alr kinda doing that, but maybe comm that info so that everyone's aware of how fucked you are?
    public LauncherHeuristic getHeuristic(RobotInfo[] nearbyFriendlies, RobotInfo[] dangerousEnemies, RobotInfo nearestEnemyInfo) throws GameActionException { // TODO: Maybe only check # of attackers on the robot closest to you?
        //TODO: Fix this method?

        // your attack isn't ready, then don't engage

        if(nearestEnemyInfo == null){ // No enemies nearby, we safe
            indicatorString += "NE1; ";
            return new LauncherHeuristic(100, 100, 0, 0.01);
        }
        Util.log("Nearest enemy Info: " + nearestEnemyInfo.location.toString());

        double friendlyDamage = 0.0;
        double enemyDamage = 0.0;
        double friendlyHP = 0.0;
        double enemyHP = 0.0;
        double totalEnemyDamage = 0.0;

        // Calculate enemies attacking you


        for(int i = 0; i < dangerousEnemies.length; i++){
            RobotInfo info = dangerousEnemies[i];
            if(info.type == RobotType.LAUNCHER){
                double attackCooldown = info.type.actionCooldown;
                enemyDamage += info.type.damage / attackCooldown;
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
//            if(info.getHealth() < health_to_retreat){
//                continue;
//            }
//            if(info.type == RobotType.SAGE){
//                continue;
//            }
            double attackCooldown = info.type.actionCooldown;
            friendlyDamage += info.type.damage / attackCooldown;
            friendlyHP += info.getHealth();
//            if(info.type == RobotType.ARCHON){ // NOTE: Archons can't attack, but this just makes you more likely to wanna protect your own archon
////                friendlyHP += info.getHealth();
//                double damageDiff = 2.0 / (rc.senseRubble(info.location) + 10.0);
//                if(enemyDamage <= damageDiff){
//                    enemyDamage = 0;
//                }
//                else{
//                    enemyDamage -= damageDiff;
//                }
//            }
        }

        double myAttackCooldown = 0;
        friendlyDamage += myType.damage / myType.actionCooldown;
        friendlyHP += rc.getHealth();

        return new LauncherHeuristic(friendlyHP, friendlyDamage, enemyHP, enemyDamage);
    }



}
