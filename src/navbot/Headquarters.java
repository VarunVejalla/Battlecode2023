package navbot;

import battlecode.common.*;

public class Headquarters extends Robot {


    int desiredCount = 1;
    int count = 0;
    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);
        myLoc = rc.getLocation();
    }


    public void run() throws GameActionException {
        super.run();

        Direction spawnDir = Direction.EAST;
        MapLocation spawnLocation = myLoc.add(spawnDir);

//        // spawn a carrier
        if(rc.getID() < 5  &&  count < desiredCount) {
            if (rc.canBuildRobot(RobotType.CARRIER, spawnLocation)) {
                rc.buildRobot(RobotType.CARRIER, spawnLocation);
                count += 1;
            }
        }



////// spawn a launcher
//        if(rc.getID() == 3 &&  count < desiredCount) {
//            if (rc.canBuildRobot(RobotType.LAUNCHER, spawnLocation)) {
//                rc.buildRobot(RobotType.LAUNCHER, spawnLocation);
//                count += 1;
//            }
//        }

//        if(rc.getID() == 3 &&  count < desiredCount) {
//            if (rc.canBuildRobot(RobotType.DESTABILIZER, spawnLocation)) {
//                rc.buildRobot(RobotType.DESTABILIZER, spawnLocation);
//                count += 1;
//            }
//        }




    }

}
