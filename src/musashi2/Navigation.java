package musashi2;

import battlecode.common.*;

enum NavigationMode{
    FUZZYNAV, BUGNAV;
}

//TODO: Fix bugnav

public class Navigation {

    RobotController rc;
    Robot robot;

    NavigationMode mode = NavigationMode.FUZZYNAV;

    // Bugnav variables
    int closestDistToTarget = Integer.MAX_VALUE;
    MapLocation lastWallFollowed = null;
    Direction lastDirectionMoved = null;
    int roundsSinceClosestDistReset = 0;
    MapLocation prevTarget = null;

    final int ROUNDS_TO_RESET_BUG_CLOSEST = 15;

    public Navigation(RobotController rc, Robot robot){
        this.rc = rc;
        this.robot = robot;
    }

    public boolean goToFuzzy(MapLocation target, int minDistToSatisfy) throws GameActionException {
        mode = NavigationMode.FUZZYNAV;
        return goTo(target, minDistToSatisfy);
    }

    public boolean goToBug(MapLocation target, int minDistToSatisfy) throws GameActionException {
        if(mode != NavigationMode.BUGNAV){
            mode = NavigationMode.BUGNAV;
            resetBugNav();
        }
        if(!target.equals(prevTarget)){
            resetBugNav();
        }
        prevTarget = target;
        return goTo(target, minDistToSatisfy);
    }

    public boolean goTo(MapLocation target, int minDistToSatisfy) throws GameActionException{
//        rc.setIndicatorString(String.format("travelling to (%d, %d)", target.x, target.y));
//        rc.setIndicatorLine(robot.myLoc, target, 0, 0, 255);

        // thy journey hath been completed
        if (robot.myLoc.distanceSquaredTo(target) <= minDistToSatisfy){
            return true;
        }

        if(!rc.isMovementReady()){
            return false;
        }

        while(rc.isMovementReady()){
            Direction toGo = null;
            switch(mode){
                case FUZZYNAV:
                    toGo = fuzzyNav(target);
                    break;
                case BUGNAV:
                    toGo = bugNav(target);
                    break;
            }
            if(toGo == null) return false;
            Util.tryMove(toGo); // Should always return true since fuzzyNav checks if rc.canMove(dir)
            if (robot.myLoc.distanceSquaredTo(target) <= minDistToSatisfy){
                return true;
            }
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
        };

        Direction bestDir = null;
        double bestCost = Double.MAX_VALUE;

        for(int i = moveOptions.length; i-- > 0;){
            Direction dir = moveOptions[i];
            MapLocation newLoc = robot.myLoc.add(dir);

            if(!rc.canSenseLocation(newLoc) || !rc.canMove(dir)){   // skip checking this cell if we can't see / move to it
                continue;
            }

            if(!rc.sensePassability(newLoc)) continue;  // don't consider if the new location is not passable

            MapInfo newLocInfo = rc.senseMapInfo(newLoc); // (10 bytecode) get MapInfo for the location of interest, which gives us a lot of juicy details about the spot

            // TODO: Lowkey idk if this code works
            // If there's a current, add that to the end of the new location
            if(newLocInfo.getCurrentDirection() != Direction.CENTER){
                newLoc = newLoc.add(newLocInfo.getCurrentDirection());
                if(!rc.canSenseLocation(newLoc) || !rc.canMove(dir)){
                    continue;
                }
                newLocInfo = rc.senseMapInfo(newLoc);
            }

            double cost = newLocInfo.getCooldownMultiplier(robot.myTeam) * 10;
            cost += Util.minMovesToReach(newLoc, target) * 10;

            if(cost < bestCost){
                bestCost = cost;
                bestDir = dir;

            }
        }

        return bestDir;
    }

    public void resetBugNav() {
        closestDistToTarget = Integer.MAX_VALUE;
        lastWallFollowed = null;
        lastDirectionMoved = null;
        roundsSinceClosestDistReset = 0;
    }

    public Direction bugNav(MapLocation target) throws GameActionException {
        Util.log("Running bugnav");
        // Every 20 turns reset the closest distance to target
        if(roundsSinceClosestDistReset >= ROUNDS_TO_RESET_BUG_CLOSEST){
            closestDistToTarget = Integer.MAX_VALUE;
            roundsSinceClosestDistReset = 0;
        }
        roundsSinceClosestDistReset++;

        Direction closestDir = null;
        Direction wallDir = null;
        Direction dir = null;

        if(lastWallFollowed != null){
            // If the wall no longer exists there, so note that.
            Direction toLastWallFollowed = robot.myLoc.directionTo(lastWallFollowed);
            if(toLastWallFollowed == Direction.CENTER || (robot.myLoc.isAdjacentTo(lastWallFollowed) && rc.canMove(toLastWallFollowed))){
                lastWallFollowed = null;
            }
            else{
                dir = robot.myLoc.directionTo(lastWallFollowed);
            }
        }
        if(dir == null){
            dir = robot.myLoc.directionTo(target);
        }

        // This should never happen theoretically, but in case it does, just reset and continue.
        if(dir == Direction.CENTER){
//            System.out.println("ID: " + rc.getID());
//            rc.resign();
//            return null;
            resetBugNav();
            return Direction.CENTER;
        }

        for(int i = 0; i < 8; i++){
            MapLocation newLoc = rc.adjacentLocation(dir);
            if(rc.canSenseLocation(newLoc) && rc.canMove(dir)){
                // If we can get closer to the target than we've ever been before, do that.
                int dist = newLoc.distanceSquaredTo(target);
                if(dist < closestDistToTarget){
                    closestDistToTarget = dist;
                    closestDir = dir;
                }

                // Check if wall-following is viable
                if(wallDir == null){
                    wallDir = dir;
                }
            }
            else{
                if(wallDir == null){
                    lastWallFollowed = newLoc;
                }
            }
            dir = dir.rotateRight();
        }

        if(closestDir != null){
            return closestDir;
        }
        return wallDir;
    }

    public void moveRandom() throws GameActionException {
        int randomIdx = robot.rng.nextInt(8);
        for(int i = 0; i < Robot.movementDirections.length; i++){
            if(Util.tryMove(Robot.movementDirections[(randomIdx + i) % Robot.movementDirections.length])){
                return;
            }
        }
    }

    public boolean circle(MapLocation center, int minDist, int maxDist) throws GameActionException {
        if(circle(center, minDist, maxDist, true)){
            return true;
        }
        return circle(center, minDist, maxDist, false);
    }

    // from: https://github.com/srikarg89/Battlecode2022/blob/main/src/cracked4BuildOrder/Navigation.java
    public boolean circle(MapLocation center, int minDist, int maxDist, boolean ccw) throws GameActionException {
        if(!rc.isMovementReady()){
            return false;
        }
        MapLocation myLoc = robot.myLoc;
        if(myLoc.distanceSquaredTo(center) > maxDist){
            Util.log("Moving closer!");
            return goTo(center, minDist);
        }
        if(myLoc.distanceSquaredTo(center) < minDist){
            Util.log("Moving away!");
            Direction centerDir = myLoc.directionTo(center);
            MapLocation target = myLoc.subtract(centerDir).subtract(centerDir).subtract(centerDir).subtract(centerDir).subtract(centerDir);
            boolean moved = goToBug(target, minDist);
            if(moved){
                return true;
            }
            moved = goToFuzzy(target, minDist);
            if(moved) {
                return true;
            }
            return false;
        }

        int dx = myLoc.x - center.x;
        int dy = myLoc.y - center.y;
        double cs = Math.cos(ccw ? 0.5 : -0.5);
        double sn = Math.sin(ccw ? 0.5 : -0.5);
        int x = (int) (dx * cs - dy * sn);
        int y = (int) (dx * sn + dy * cs);
        MapLocation target = center.translate(x, y);
        Direction targetDir = myLoc.directionTo(target);
        Direction[] options = {targetDir, targetDir.rotateRight(), targetDir.rotateLeft(), targetDir.rotateRight().rotateRight(), targetDir.rotateLeft().rotateLeft()};
        Direction bestDirection = null;
        double lowestCooldown = Double.MAX_VALUE;
        for(int i = 0; i < options.length; i++){
            if(!rc.canMove(options[i])){
                continue;
            }
            MapLocation newLoc = myLoc.add(options[i]);
            if(center.distanceSquaredTo(newLoc) < minDist){
                continue;
            }
            if(center.distanceSquaredTo(newLoc) > maxDist){
                continue;
            }
            double cooldown = rc.senseMapInfo(newLoc).getCooldownMultiplier(robot.myTeam);
            if(cooldown < lowestCooldown){
                lowestCooldown = cooldown;
                bestDirection = options[i];
            }
        }
        if(bestDirection != null){
            rc.move(bestDirection);
            return true;
        }
        return false;
    }
}
