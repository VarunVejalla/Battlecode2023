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

    // TODO: Add attacking anchors to this
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

        if(haveUncommedIsland()){
            returnToClosestHQ();
        }
        else if(isAttacking){
            runAttackMovement();
        }
        else{
            runDefensiveMovement();
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
//        nav.goToFuzzy(targetLoc, myType.actionRadiusSquared);
//        nav.circle(targetLoc, circleRadius);

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
            targetLoc = getNearestWell();
            if(targetLoc == null){
                targetLoc = getRandomScoutingLocation();
            }
        }

        // TODO: Maybe circle the well to defend it better instead of just standing in one spot?
        //        nav.goToFuzzy(targetLoc, myType.actionRadiusSquared);
        //        nav.circle(targetLoc, circleRadius);

        if(myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){   // we have arrived
            // rc.senseNearby
            if(rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam).length  > DEFENDING_THRESHOLD) { // don't want to crowd any mining areas
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
