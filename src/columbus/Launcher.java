package columbus;

import battlecode.common.*;

public class Launcher extends Robot {

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
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

    public void run() throws GameActionException{
        super.run();

        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);



        if (enemies.length >= 0) {
            // want to attack the one that's closest to dying
            // maybe want to attack
            // MapLocation toAttack = enemies[0].location;

            int toAttackIndex = 0;
            for(int i = 1; i < enemies.length; i++) {
                // if it'd be better to attack enemies[i], change attackIndex to i
                if(rc.canAttack(enemies[i].location) && compare(enemies[i], enemies[toAttackIndex]) < 0) {
                    toAttackIndex = i;
                }
            }


            MapLocation toAttack = enemies[toAttackIndex].location;

            if (rc.canAttack(toAttack)) {
                rc.setIndicatorString("Attacking");
                rc.attack(toAttack);
            }
        }

        // Also try to move randomly.
        int height = rc.getMapHeight();
        int width = rc.getMapWidth();

        MapLocation center = new MapLocation(width/2, height/2);
        nav.goToFuzzy(center, 0);

    }
}
