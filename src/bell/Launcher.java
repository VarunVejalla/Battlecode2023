package bell;

import battlecode.common.*;

public class Launcher extends Robot {

    private MapLocation targetLoc;

    // TODO: Replace this with an actually good strategy
    boolean isAttacking;
    int DEFENDING_THRESHOLD = 15;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        // TODO: Do something smart to figure this out instead of just random.
        // TODO: For some reason this is always set to false. Figure out why.
        isAttacking = rng.nextBoolean();
    }

    public void run() throws GameActionException{
        super.run();
        if(isAttacking){
            rc.setIndicatorDot(myLoc, 255, 0, 0);
            Util.log("Yam attacking");
        }
        else{
            rc.setIndicatorDot(myLoc, 0, 255, 0);
            Util.log("Yam defending");
        }
        runAttack();
        runMovement();
        runAttack();
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

    public void runAttack() throws GameActionException {
        if(!rc.isActionReady()){
            return;
        }
        // Try to attack someone
        int radius = myType.actionRadiusSquared;
        Team opponent = myTeam.opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);

        // want to attack the one that's closest to dying
        // maybe want to attack
        // MapLocation toAttack = enemies[0].location;

        int toAttackIndex = -1;
        for(int i = 0; i < enemies.length; i++) {
            // if it'd be better to attack enemies[i], change attackIndex to i
            if(rc.canAttack(enemies[i].location)) {
                if(toAttackIndex == -1 || compare(enemies[i], enemies[toAttackIndex]) < 0) {
                    toAttackIndex = i;
                }
            }
        }

        if(toAttackIndex != -1){
            MapLocation toAttack = enemies[toAttackIndex].location;
            rc.setIndicatorString("Attacking");
            rc.attack(toAttack);
        }
    }

    public void runMovement() throws GameActionException {
        if(!rc.isMovementReady()){
            return;
        }

        // If you're in a fight w/ an enemy, run launcher micro
        RobotInfo[] enemies = rc.senseNearbyRobots(myType.visionRadiusSquared, opponent);
        boolean dangerNearby = false;
        for(RobotInfo info : enemies){
            if(info.type == RobotType.LAUNCHER || info.type == RobotType.CARRIER){
                dangerNearby = true;
            }
        }
        if(dangerNearby){
            RobotInfo closestDanger = null;
            for(RobotInfo info : enemies){
                if(info.type == RobotType.LAUNCHER || info.type == RobotType.CARRIER){
                    if(closestDanger == null || myLoc.distanceSquaredTo(info.location) < myLoc.distanceSquaredTo(closestDanger.location)){
                        closestDanger = info;
                    }
                }
            }

            // If you're within range, try attacking then move out.
            runAttack();
            // If you're outside of range but your attack is ready, move in then attack, but only move in if attack is ready
            if(rc.isActionReady()){
                Direction enemyDir = myLoc.directionTo(closestDanger.location);
                // If I can move towards enemy and attack him, then do it.
                Direction[] potentialMoveDirs = {enemyDir, enemyDir.rotateLeft(), enemyDir.rotateRight()};
                for(Direction potentialMoveDir : potentialMoveDirs){
                    if(!rc.canMove(potentialMoveDir)){
                        continue;
                    }
                    // TODO: Check for currents and cooldowns and stuff
                    // TODO: Do the whole "do we have more allied troops than enemy troops" thing to figure out if it's a fight worth taking
                    // Can prolly copy some of that stuff from last year.
                    // If I can move in and attack, then do that.
                    if(myLoc.add(potentialMoveDir).distanceSquaredTo(closestDanger.location) <= myType.actionRadiusSquared){
                        rc.move(potentialMoveDir);
                        runAttack();
                    }
                }
            }
            // If you couldn't move in then attack, then simply move away until your attack comes back.
            if(rc.isMovementReady()){
                Direction enemyDir = myLoc.directionTo(closestDanger.location);
                MapLocation farAway = myLoc.subtract(enemyDir).subtract(enemyDir).subtract(enemyDir).subtract(enemyDir);
                nav.goToFuzzy(farAway, 0);
            }
        } else{
            // Otherwise, run normal launcher movement
            if(haveUncommedIsland()) {
                returnToClosestHQ();
            }
            else if(isAttacking){
                runAttackMovement();
            }
            else{
                runDefensiveMovement();
            }
        }
    }

    public boolean haveUncommedIsland() {
        for(IslandInfo info : islands.values()){
            if(!info.commed){
                return true;
            }
        }
        return false;
    }

    public void returnToClosestHQ() throws GameActionException {
        targetLoc = getNearestFriendlyHQ();
        if(myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
            nav.goToFuzzy(targetLoc, 0);
            rc.setIndicatorString("have uncommed info, fuzzying to HQ: " + targetLoc);
        }
        else{
            nav.goToBug(targetLoc, 0);
            rc.setIndicatorString("have uncommed info, bugging to HQ: " + targetLoc);
        }
    }


    // Go attack an island
    public void runAttackMovement() throws GameActionException {
        if(targetLoc != null && myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
            targetLoc = null;
        }
        if(targetLoc == null){
            targetLoc = getNearestUncontrolledIsland();
            if(targetLoc == null){
                targetLoc = getRandomScoutingLocation();
                Util.log("Going towards random scouting location: " + targetLoc);
            }
            else{
                Util.log("Going towards nearest uncontrolled island: " + targetLoc);
            }
        }

        // TODO: Maybe circle the island to defend it better instead of just standing in one spot?
        if(myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
            nav.moveRandom();
            rc.setIndicatorString("attackingly moving random" + targetLoc);
        }
        else{
            nav.goToFuzzy(targetLoc, myType.actionRadiusSquared);
            rc.setIndicatorString("attackingly going to " + targetLoc);
        }

    }

    // Go defend a well
    public void runDefensiveMovement() throws GameActionException {
        if(targetLoc != null && myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
            targetLoc = null;
        }
        if(targetLoc == null){
            int HQIdx = rng.nextInt(numHQs);
            if(rng.nextBoolean()){
                targetLoc = comms.getClosestWell(HQIdx, ResourceType.ADAMANTIUM);
            }
            else{
                targetLoc = comms.getClosestWell(HQIdx, ResourceType.MANA);
            }
            if(targetLoc == null){
                targetLoc = getRandomScoutingLocation();
            }
        }

        int distanceSquaredToTarget = myLoc.distanceSquaredTo(targetLoc);
        if(distanceSquaredToTarget <= myType.actionRadiusSquared){   // we have arrived
            // rc.senseNearby


            //TODO: make this strategy better. Determine whether or not we should try to defened depending on how much pressure we're under
            if(rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam).length  > DEFENDING_THRESHOLD && distanceSquaredToTarget > 8) { // don't want to crowd any mining areas so leave if you're not super close
                targetLoc = getRandomScoutingLocation();        // move on to a different location to scout
                nav.goToBug(targetLoc, myType.actionRadiusSquared);
            }
            else {
                nav.circle(targetLoc, 2, (int) (myType.actionRadiusSquared * 1.5));  // the constants here are kinda arbitrary
                rc.setIndicatorString("defensively circling " + targetLoc);
            }
        }
        else{
            nav.goToBug(targetLoc, myType.actionRadiusSquared);
            rc.setIndicatorString("defensively going to " + targetLoc);
        }


    }

}
