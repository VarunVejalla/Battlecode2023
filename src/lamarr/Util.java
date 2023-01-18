package lamarr;

import battlecode.common.*;

public class Util {
    static RobotController rc;
    static Robot robot;

    public static int minMovesToReach(MapLocation a, MapLocation b){
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.max(Math.abs(dx), Math.abs(dy));
    }


    public static Direction[] closeDirections(Direction dir){
        Direction[] close = {
                dir,
                dir.rotateLeft(),
                dir.rotateRight(),
                dir.rotateLeft().rotateLeft(),
                dir.rotateRight().rotateRight(),
                dir.rotateLeft().rotateLeft().rotateLeft(),
                dir.rotateRight().rotateRight().rotateRight(),
                dir.opposite()
        };
        return close;
    }


    public static boolean tryMine(ResourceType type, int numResources) throws GameActionException {
        MapLocation me = rc.getLocation();
        for (Direction dir : Robot.allDirections) {
            MapLocation wellLocation = me.add(dir);
            if (rc.canSenseLocation(wellLocation)) {
                WellInfo info = rc.senseWell(wellLocation);
                if (info == null) {
                    continue;
                }
                if (info.getResourceType() != type) {
                    continue;
                }
                int rate = numResources;
                if(rate > info.getRate()){
                    rate = info.getRate();
                }
                // See if you can collect the requested amount.
                if (rc.canCollectResource(wellLocation, rate)) {
                    rc.collectResource(wellLocation, rate);
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean tryMove(Direction dir) throws GameActionException{
        if(rc.canMove(dir)) {
            rc.move(dir);
            robot.myLoc = rc.getLocation();
            robot.myLocInfo = rc.senseMapInfo(robot.myLoc);
            robot.nav.lastDirectionMoved = dir;
            return true;
        }
        return false;
    }

    public static void log(String str){
//        if(true){
//            return;
//        }

        if(rc.getType() != RobotType.LAUNCHER){
            return;
        }

//        if(rc.getID() != 12586){
//            return;
//        }
//        if(rc.getType() == RobotType.HEADQUARTERS)  System.out.println(str);


        System.out.println(str);
    }
}
