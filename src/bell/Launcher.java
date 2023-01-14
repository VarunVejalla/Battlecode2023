package bell;

import battlecode.common.*;

public class Launcher extends Robot {

    private MapLocation targetLoc;

    // TODO: Replace this with an actually good strategy
    boolean isAttacking;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
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

        // TODO: Implement this
//        if(spottedUncommedIsland()){
//            returnToHQ();
//        }

        if(isAttacking){
            runAttackMovement();
        }
        else{
            runDefensiveMovement();
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
            }
        }

        // TODO: Maybe circle the island to defend it better instead of just standing in one spot?
//        nav.goToFuzzy(targetLoc, myType.actionRadiusSquared);
//        nav.circle(targetLoc, circleRadius);

        if(myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
            nav.moveRandom();
        }
        else{
            nav.goToFuzzy(targetLoc, myType.actionRadiusSquared);
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

        if(myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
            nav.moveRandom();
        }
        else{
            nav.goToBug(targetLoc, myType.actionRadiusSquared);
        }


    }

}
