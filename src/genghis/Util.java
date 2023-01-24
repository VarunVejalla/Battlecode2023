package genghis;

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
        WellInfo[] nearbyWells = rc.senseNearbyWells(RobotType.CARRIER.actionRadiusSquared);
        for(WellInfo well : nearbyWells){
            if(well.getResourceType() != type){
                continue;
            }
            int rate = numResources;
            if(rate > well.getRate()){
                rate = well.getRate();
            }
            // See if you can collect the requested amount.
            if (rc.canCollectResource(well.getMapLocation(), rate)) {
                rc.collectResource(well.getMapLocation(), rate);
                return true;
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

    public static int checkNumSymmetriesPossible() throws GameActionException {
        return getPotentialSymmetries().length;
    }

    public static SymmetryType[] getPotentialSymmetries() throws GameActionException {
        SymmetryType[] potential = new SymmetryType[3];
        int num = 0;
        if(robot.comms.checkSymmetryPossible(SymmetryType.HORIZONTAL)){
            potential[num] = SymmetryType.HORIZONTAL;
            num++;
        }
        if(robot.comms.checkSymmetryPossible(SymmetryType.VERTICAL)){
            potential[num] = SymmetryType.VERTICAL;
            num++;
        }
        if(robot.comms.checkSymmetryPossible(SymmetryType.ROTATIONAL)){
            potential[num] = SymmetryType.ROTATIONAL;
            num++;
        }
        SymmetryType[] output = new SymmetryType[num];
        for(int i = 0; i < num; i++){
            output[i] = potential[i];
        }
        return output;
    }

    public static MapLocation applySymmetry(MapLocation loc, SymmetryType type){
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        switch(type){
            case HORIZONTAL:
                return new MapLocation(width - loc.x - 1, loc.y);
            case VERTICAL:
                return new MapLocation(loc.x, height - loc.y - 1);
            case ROTATIONAL:
                return new MapLocation(width - loc.x - 1, height - loc.y - 1);
        }
        return null;
    }

    public MapLocation getClosestMapLocation(MapLocation[] locs){
        MapLocation closest = null;
        int closestDist = Integer.MAX_VALUE;
        for(MapLocation loc : locs){
            int dist = robot.myLoc.distanceSquaredTo(loc);
            if(dist < closestDist){
                closest = loc;
                closestDist = dist;
            }
        }
        return closest;
    }

    public static int getNumTroopsInRange(int radius, Team team, RobotType type) throws GameActionException {
        RobotInfo[] infos = team == null ? rc.senseNearbyRobots(radius) : rc.senseNearbyRobots(radius, team);
        if(type == null){
            return infos.length;
        }
        int count = 0;
        for(RobotInfo info : infos){
            if(info.type == type){
                count++;
            }
        }
        return count;
    }


    public static void log(String str){
        if(true){
            return;
        }

//        if(rc.getType() != RobotType.LAUNCHER){
//            return;
//        }

//        if(rc.getID() != 12586){
//            return;
//        }
//        if(rc.getType() == RobotType.HEADQUARTERS)  System.out.println(str);


        System.out.println(str);
    }
}
