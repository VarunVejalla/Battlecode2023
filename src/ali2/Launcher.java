package ali2;

import battlecode.common.*;

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
        Util.addToIndicatorString("FH:" + (int)friendlyHP + ",FD:" + (int)friendlyDamage);
        Util.addToIndicatorString("EH:" + (int)enemyHP + ",ED:" + (int)enemyDamage);
        Util.addToIndicatorString("MT:" + (int)myTurnsNeeded + ",ET:" + (int)enemyTurnsNeeded);
        // 1.5 simply because im ballsy and wanna go for it
        return myTurnsNeeded <= enemyTurnsNeeded * 1.0; // If you can kill them faster than they can kill you, return true
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

    //TODO: factor in island healing
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
                Util.log("Error: why didn't you attack?");
            }
            if(rc.isMovementReady()){
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
            if (haveUncommedIsland() || haveUncommedSymmetry()) {
                returnToClosestHQ();
            } else {
                runNormalOffensiveStrategy();
            }
        }
    }

    public void runUnsafeStrategy() throws GameActionException{
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

    // TODO: Consider total enemy damage, and if ur aboutta get yeeted then dip so you can get healed.
    // TODO: Maybe only check # of attackers on the robot closest to you?
    public LauncherHeuristic getHeuristic(RobotInfo[] nearbyFriendlies, RobotInfo[] nearbyEnemies) throws GameActionException {
        // your attack isn't ready, then don't engage

        if(nearbyEnemies.length == 0){ // No enemies nearby, we safe
            Util.addToIndicatorString("NE1");
            return new LauncherHeuristic(100, 100, 0, 0.01);
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
        double friendlyHP = 0.0;
        double enemyHP = 0.0;
        double totalEnemyDamage = 0.0;

        boolean added;
        for(int i = 0; i < nearbyEnemies.length; i++){
            RobotInfo enemyInfo = nearbyEnemies[i];

            if(enemyInfo.type != RobotType.HEADQUARTERS && enemyInfo.type != RobotType.LAUNCHER && enemyInfo.type != RobotType.CARRIER){
                continue;
            }

            if(enemyInfo.type == RobotType.HEADQUARTERS){
                for(int j = 0; j < nearbyFriendlies.length; j++){
                    RobotInfo friendlyInfo = nearbyFriendlies[j];
                    if(friendlyInfo.type != RobotType.LAUNCHER){
                        continue;
                    }
                    if(friendlyInfo.location.isWithinDistanceSquared(enemyInfo.location, enemyInfo.type.actionRadiusSquared)){
                        enemyDamage += (double)enemyInfo.type.damage / 10;
                    }
                }
                // accounts for yourself
                if (myLoc.isWithinDistanceSquared(enemyInfo.location, enemyInfo.type.actionRadiusSquared)) {
                    enemyDamage += (double) enemyInfo.type.damage / 10;
                }
                // Only consider the enemy launcher / carrier if it's in range of a friendly launcher.
            } else if(enemyInfo.type == RobotType.LAUNCHER || enemyInfo.type == RobotType.CARRIER) {
                added = false;

                for (int j = 0; j < nearbyFriendlies.length; j++) {
                    RobotInfo friendlyInfo = nearbyFriendlies[j];
                    if (friendlyInfo.type != RobotType.LAUNCHER) {
                        continue;
                    }
                    if (friendlyInfo.location.isWithinDistanceSquared(enemyInfo.location, enemyInfo.type.actionRadiusSquared)) {
                        if(enemyInfo.type == RobotType.LAUNCHER) {
                            double cooldown = enemyInfo.type.actionCooldown * nearbyEnemyMapInfo[i].getCooldownMultiplier(opponent);
                            enemyDamage += (double) enemyInfo.type.damage / cooldown;
                            enemyHP += enemyInfo.getHealth();
                        }
                        else if(enemyInfo.type == RobotType.CARRIER) {
                            int carrierWeight = enemyInfo.getResourceAmount(ResourceType.ADAMANTIUM) + enemyInfo.getResourceAmount(ResourceType.MANA) + enemyInfo.getResourceAmount(ResourceType.ELIXIR);
                            int carrierDamage = (int)(carrierWeight * GameConstants.CARRIER_DAMAGE_FACTOR);
                            friendlyHP -= carrierDamage;
                        }
                        added = true;
                        break;
                    }
                }
                // accounts for yourself
                if (!added && myLoc.isWithinDistanceSquared(enemyInfo.location, enemyInfo.type.actionRadiusSquared)) {
                    if(enemyInfo.type == RobotType.LAUNCHER) {
                        double cooldown = enemyInfo.type.actionCooldown * nearbyEnemyMapInfo[i].getCooldownMultiplier(opponent);
                        enemyDamage += (double) enemyInfo.type.damage / cooldown;
                        enemyHP += enemyInfo.getHealth();
                    }
                    else if(enemyInfo.type == RobotType.CARRIER) {
                        int carrierWeight = enemyInfo.getResourceAmount(ResourceType.ADAMANTIUM) + enemyInfo.getResourceAmount(ResourceType.MANA) + enemyInfo.getResourceAmount(ResourceType.ELIXIR);
                        int carrierDamage = (int)(carrierWeight * GameConstants.CARRIER_DAMAGE_FACTOR);
                        friendlyHP -= carrierDamage;
                    }
                }
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
                        friendlyHP += friendlyInfo.getHealth();
                        break;
                    }
                }
            }
        }

        //accounts for yourself
        Util.log("num enemies nearby: " + String.valueOf(nearbyEnemies.length));
        for(int i = 0; i < nearbyEnemies.length; i++){
            RobotInfo enemyInfo = nearbyEnemies[i];
            if(enemyInfo.type != RobotType.LAUNCHER){
                continue;
            }
            Util.log("distance: " + String.valueOf(enemyInfo.location.distanceSquaredTo(myLoc)));
            if(enemyInfo.location.isWithinDistanceSquared(myLoc, myType.actionRadiusSquared)){
                double cooldown = (double)myType.actionCooldown * rc.senseMapInfo(myLoc).getCooldownMultiplier(myTeam);
                if(rc.isActionReady()) {
                    friendlyDamage += (double) myType.damage / cooldown;
                }
                friendlyHP += rc.getHealth();
                break;
            }
        }

        friendlyHP = Math.max(friendlyHP, 1.0); // Just don't let it go to 0.

        return new LauncherHeuristic(friendlyHP, friendlyDamage, enemyHP, enemyDamage);
    }
}
