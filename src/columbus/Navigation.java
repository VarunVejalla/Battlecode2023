package columbus;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.MapInfo;

public class Navigation {


    RobotController rc;
    Robot robot;

    public Navigation(RobotController rc, Robot robot){
        this.rc = rc;
        this.robot = robot;
    }


    public boolean goToFuzzy(MapLocation target) throws GameActionException{
        // returns true if we've moved or
        return false;
    }



    public Direction fuzzyNav(MapLocation target) throws GameActionException{
        Direction toTarget = robot.myLoc.directionTo(target);
        Direction[] moveOptions = {toTarget, toTarget.rotateLeft(), toTarget.rotateRight(), toTarget.rotateLeft().rotateLeft(), toTarget.rotateRight().rotateRight()};
        Direction bestDir = null;
        int bestCost = Integer.MAX_VALUE;

        for(int i=moveOptions.length; i-- > 0;){
            Direction dir = moveOptions[i];
            MapLocation newLoc = robot.myLoc.add(dir);
            if(!rc.canSenseLocation(newLoc) || !rc.canMove(dir)){
                continue;
            }
            MapInfo newLocInfo = rc.senseMapInfo(newLoc);
            if(!newLocInfo.isPassable()){        // if it's not passible, we don't care about the cost
                continue;
            }
            double cooldownCost = newLocInfo.getCooldownMuliplier(robot.myTeam)*10;


        }


        return bestDir;
    }
}
