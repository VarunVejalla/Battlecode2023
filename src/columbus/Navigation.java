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


    public boolean goToFuzzy(MapLocation target, int minDistToSatisfy) throws GameActionException{
        // thy journey hath been completed
        if (robot.myLoc.distanceSquaredTo(target) <= minDistToSatisfy){
            return true;
        }

        rc.setIndicatorString(String.format("travelling to (%d, %d)", target.x, target.y));
        if(!rc.isMovementReady()){
            return false;
        }

        while(rc.isMovementReady()){
            Direction toGo;
            toGo = fuzzyNav(target);
            if(toGo == null) return false;
            Util.tryMove(toGo); // Should always return true since fuzzyNav checks if rc.canMove(dir)
        }
        return true;
    }


    public Direction fuzzyNav(MapLocation target) throws GameActionException{
        Direction toTarget = robot.myLoc.directionTo(target);
        Direction[] moveOptions = {
                toTarget,
                toTarget.rotateLeft(),
                toTarget.rotateRight(),
                toTarget.rotateLeft().rotateLeft(),
                toTarget.rotateRight().rotateRight(),
                toTarget.rotateLeft().rotateLeft().rotateLeft(),
                toTarget.rotateRight().rotateRight().rotateRight(),
                toTarget.opposite()
        };

        Direction bestDir = null;
        double bestCost = Double.MAX_VALUE;

        for(int i = moveOptions.length; i-- > 0;){
            Direction dir = moveOptions[i];
            MapLocation newLoc = robot.myLoc.add(dir);

            if(!rc.canSenseLocation(newLoc) || !rc.canMove(dir)){   // skip checking this cell if we can't see / move to it
                continue;
            }

            // TODO: need to integrate whether or not a square is passable here, but when i tried the method returned every method as unpassable

            MapInfo newLocInfo = rc.senseMapInfo(newLoc); // (10 bytecode) get MapInfo for the location of interest, which gives us a lot of juicy details about the spot

            // If there's a current, add that to the end of the new location
            if(newLocInfo.getCurrentDirection() != Direction.CENTER){
                newLoc.add(newLocInfo.getCurrentDirection());
                if(!rc.canSenseLocation(newLoc) || !rc.canMove(dir)){
                    continue;
                }
                newLocInfo = rc.senseMapInfo(newLoc);
            }

            double cost = newLocInfo.getCooldownMuliplier(robot.myTeam) * 10;
            cost += Util.minMovesToReach(newLoc, target) * 10;

            if(cost < bestCost){
                bestCost = cost;
                bestDir = dir;

            }
        }

        return bestDir;
    }
}
