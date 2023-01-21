package navbot;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Destabilizer extends Robot {

    public Destabilizer(RobotController rc) throws GameActionException {
        super(rc);
    }

    MapLocation destination;

    public void run() throws GameActionException{
        super.run();
        // code to run booster
        destination = new MapLocation(29,29);   // top right
        System.out.println("movement cooldown turns A: " + rc.getMovementCooldownTurns());

        while(rc.isMovementReady()) {
            nav.goToFuzzy(destination, 0);
        }
    }
    }
