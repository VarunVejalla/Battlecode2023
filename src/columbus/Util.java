package columbus;

import battlecode.common.*;

public class Util {
    static RobotController rc;
    static Robot robot;

    public static int minMovesToReach(MapLocation a, MapLocation b){
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.max(Math.abs(dx), Math.abs(dy));
    }

    // TODO: Fix this to take into consideration that HQ can spawn up to 9 (radius squared) distance away from it, not necessarily just adjacent.
    public static boolean trySpawnGeneralDirection(RobotType type, Direction spawnDir) throws GameActionException  {
        Direction[] spawnDirs = {
                spawnDir, spawnDir.rotateLeft(), spawnDir.rotateRight(), spawnDir.rotateLeft().rotateLeft(), spawnDir.rotateRight().rotateRight(), spawnDir.rotateLeft().rotateLeft().rotateLeft(), spawnDir.rotateRight().rotateRight().rotateRight(), spawnDir.opposite()
        };
        for(Direction dir : spawnDirs){
            MapLocation newLoc = robot.myLoc.add(dir);
            if(rc.canBuildRobot(type, newLoc)){
                rc.buildRobot(type, newLoc);
                return true;
            }
        }
        return false;
    }

    public static boolean tryMine(ResourceType type, int numResources) throws GameActionException{
        MapLocation me = rc.getLocation();
        for(Direction dir : robot.directions){
            MapLocation wellLocation = me.add(dir);
            if(rc.canSenseLocation(wellLocation)){
                WellInfo info = rc.senseWell(wellLocation);
                if(info == null){
                    continue;
                }
                if(info.getResourceType() != type){
                    continue;
                }
                // See if you can collect the requested amount.
                if(rc.canCollectResource(wellLocation, numResources)){
                    rc.collectResource(wellLocation, numResources);
                    return true;
                }
                // Otherwise just try collecting as much as you can.
                else if(rc.canCollectResource(wellLocation, -1)){
                    rc.collectResource(wellLocation, -1);
                    return true;
                }
            }
        }

        return false;
    }
}
