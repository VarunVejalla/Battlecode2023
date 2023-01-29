package navbot;

import battlecode.common.*;

public class Launcher extends Robot {

    private MapLocation targetLoc;


    MapLocation destination;
    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
    }


    public void run() throws GameActionException{
        super.run();

        destination = new MapLocation(0,0);   // top right

        while(rc.isMovementReady()) {
            nav.goToBug(destination, 0);
        }
    }



}
