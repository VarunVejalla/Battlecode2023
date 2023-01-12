package columbus;

import battlecode.common.*;

public class Launcher extends Robot {

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
    }

    public void run() throws GameActionException{
        super.run();

        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length >= 0) {
            // MapLocation toAttack = enemies[0].location;
            MapLocation toAttack = rc.getLocation().add(Direction.EAST);

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
