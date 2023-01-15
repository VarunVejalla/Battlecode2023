package bell;

import battlecode.common.*;

public class Util {
    static RobotController rc;
    static Robot robot;

    public static int minMovesToReach(MapLocation a, MapLocation b){
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.max(Math.abs(dx), Math.abs(dy));
    }


    public static boolean tryMine(ResourceType type, int numResources) throws GameActionException {
        Util.log("Trying to mine " + numResources + " of type " + type.toString());
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
                Util.log("Collecting at a rate of: " + rate);
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
        if(rc.getType() != RobotType.CARRIER){
            return;
        }
//        if(rc.getID() != 12586){
//            return;
//        }
        System.out.println(str);
    }
}
