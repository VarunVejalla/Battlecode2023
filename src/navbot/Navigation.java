package navbot;

import battlecode.common.*;

import javax.swing.*;
import java.util.HashSet;

enum NavigationMode{
    FUZZYNAV, BUGNAV;
}

public class Navigation {

    RobotController rc;
    Robot robot;

    NavigationMode mode = NavigationMode.BUGNAV;

    // Bugnav variables
    int closestDistToTarget = Integer.MAX_VALUE;
    MapLocation lastWallFollowed = null;
    Direction lastDirectionMoved = null;
    int roundsSinceClosestDistReset = 0;

    // BUGNAV VARIABLES
    int counter = 0;
    boolean basic = false;
    MapLocation prevTarget = null;
    MapLocation wallStart = null;
    MapLocation hopPoint = null;
    Direction heading = null;
    Direction beeOverwrite = null;
    boolean followingWall = false;
    boolean hopping = false;
    MapLocation oldLocation = null;

    int ROUNDS_TO_RESET_BUG_CLOSEST = 15;
    MapLocation[] seen = new MapLocation[ROUNDS_TO_RESET_BUG_CLOSEST*4];




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

    //TODO: should also factor in whether cooldown increases / decreases from clouds / destabilizers / boosters ???
    public boolean isThisMyLastMovementTurn(){
        int movementCooldownPerTurn = rc.getType().movementCooldown;
        if(rc.getType() == RobotType.CARRIER){  // for some reason, rc.getType().movementCooldown returns 0 for Carriers
            int massImCarrying = rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.ELIXIR);
            movementCooldownPerTurn = (int) Math.floor(5 + 3*massImCarrying);
        }
        int currCooldown = rc.getMovementCooldownTurns();   // how much cooldown do we already have

        if(movementCooldownPerTurn * 2 + currCooldown <= 10){
            return false;   // this is not your last movementTurn
        }
        return true;        // this is your last movementTurn
    }


    // TODO: should we also incorporate cooldown information
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
        int leastNumMoves = Integer.MAX_VALUE;
        int leastDistanceSquared = Integer.MAX_VALUE;

        MapLocation bestNewLoc = robot.myLoc;
//        System.out.println("rc.getType().movementCooldown: " + rc.getType().movementCooldown);
//        System.out.println("rc.getMovementCooldownTurns(): " + rc.getMovementCooldownTurns());
//        System.out.println("rc.isMovementReady(): " + rc.isMovementReady());
//        System.out.println("isMyLastTurn(): " + isThisMyLastMovementTurn());

        for(int i = moveOptions.length; i--> 0;){
            Direction dir = moveOptions[i];
            MapLocation newLoc = robot.myLoc.add(dir);

            if(!rc.canSenseLocation(newLoc) || !rc.canMove(dir)){   // skip checking this cell if we can't see / move to it
                continue;
            }

            if(!rc.sensePassability(newLoc)) continue;  // don't consider if the new location is not passable
            MapInfo newLocInfo = rc.senseMapInfo(newLoc); // (10 bytecode) get MapInfo for the location of interest, which gives us a lot of juicy details about the spot

            if(isThisMyLastMovementTurn()){            // only factor in currents if this is your last movement on this round
                if (newLocInfo.getCurrentDirection() != Direction.CENTER) {
                    newLoc = newLoc.add(newLocInfo.getCurrentDirection());
                    if (!rc.canSenseLocation(newLoc) || !rc.canMove(dir)) {
                        continue;
                    }
                }
            }

            int numMoves = Util.minMovesToReach(newLoc, target);
            int distanceSquared = newLoc.distanceSquaredTo(target);

            // first check if this new spot decreases the number of moves we need to take to get to our target
            // if so, make this new spot the bestNewLoc
            if(numMoves < leastNumMoves){
                leastNumMoves = numMoves;
                leastDistanceSquared = distanceSquared;
                bestDir = dir;
                bestNewLoc = newLoc;
            }

            // if numMoves == leastNumMoves but the newLoc is closer to the target than the current loc, make this newLoc the bestLoc
            else if(numMoves == leastNumMoves && distanceSquared < leastDistanceSquared){
                leastNumMoves = numMoves;
                leastDistanceSquared = distanceSquared;
                bestDir = dir;
                bestNewLoc = newLoc;
            }
        }

        System.out.println("movement cooldown turns B: " + rc.getMovementCooldownTurns());
        System.out.println("best direction to move in: " + " from " + robot.myLoc + ": " + bestDir);
        System.out.println("newLoc from " + robot.myLoc + ": " + bestNewLoc);
        System.out.println("leastNumMoves from " + robot.myLoc + ": " + leastNumMoves);
        System.out.println("leastDistSq from " + robot.myLoc + ": " + leastDistanceSquared);
        System.out.println("-------------------------");


        return bestDir;
    }

    public void resetBugNav() {
        closestDistToTarget = Integer.MAX_VALUE;
        lastWallFollowed = null;
        lastDirectionMoved = null;
        roundsSinceClosestDistReset = 0;
        basic = false;
        counter = 0;
        seen = new MapLocation[ROUNDS_TO_RESET_BUG_CLOSEST*4];
    }
    public boolean sensePassabilityReal(MapLocation loc) throws GameActionException{
        if (! (0 <= loc.x && loc.x < rc.getMapWidth() && 0 <= loc.y & loc.y < rc.getMapHeight()))
            return false;

        boolean noWall = rc.sensePassability(loc);
        RobotInfo bot = rc.senseRobotAtLocation(loc);
        if (noWall && bot != null)
            return bot.type != RobotType.HEADQUARTERS;
        Direction myDir = robot.myLoc.directionTo(loc);
        Direction current = rc.senseMapInfo(loc).getCurrentDirection();
        if (current != Direction.CENTER && current != myDir &&
                current != myDir.rotateLeft() && current != myDir.rotateRight()){
            return false;
        }
        return noWall;
    }
    public Direction bugBasic(MapLocation target) throws GameActionException {
        counter ++;
        if (counter > 400) return Direction.CENTER;

        System.out.println("Old heading " + heading );
        Direction bee = robot.myLoc.directionTo(target);
        if (beeOverwrite != null) {
            System.out.println("Direction overwritten to " + beeOverwrite);
            bee = beeOverwrite;
        }
        beeOverwrite = null;

        Direction diag = null; //Used for optimizing
        if (!followingWall) {
            if (sensePassabilityReal(rc.adjacentLocation(bee))) {
                heading = bee;
                System.out.println("No walls, going " + heading);
                return bee;
            }
            else {
                System.out.println("Can't go " + bee);
                heading = bee.rotateRight().rotateRight();
                wallStart = robot.myLoc;
                followingWall = true;

                System.out.println("Bumped into wall, turning "+heading+"and starting wall tracking at " + robot.myLoc);
                closestDistToTarget = Integer.MAX_VALUE;

                if (heading == Direction.NORTHEAST || heading == Direction.NORTHWEST ||
                        heading == Direction.SOUTHEAST || heading == Direction.SOUTHWEST){
                    heading = heading.rotateLeft();
                    System.out.println("Rare edge case, sending it " + heading);
                    beeOverwrite = heading;
                    followingWall = false;
                    return bugBasic(target);
                }
            }
        }
        if (hopping && robot.myLoc.equals(hopPoint)) {
            if (rc.canMove(bee)) {
                followingWall = false;
                heading = bee;
            } else {
                do {
                    bee = bee.rotateLeft();
                } while (!rc.canMove(bee));
                followingWall = true;
                heading = bee;
            }
            System.out.println("Hopped off, heading " + heading);
            return heading;
        }
        if (closestDistToTarget != Integer.MAX_VALUE && robot.myLoc.equals(wallStart)) {
            System.out.println("Reached wall start, going to hop at " + hopPoint);
            if (hopping) {
                System.out.println("SOMETHING IS WRONG!!!");
                // ABORT!!!!
            }
            hopping = true;
        }
        MapLocation toLeft = rc.adjacentLocation(heading.rotateLeft().rotateLeft());
        Direction headingTemp = heading;
        if (sensePassabilityReal(toLeft)) {
            // if wall to the left ends, turn left to keep wall on left-hand side
            heading = heading.rotateLeft().rotateLeft();

            System.out.println("Can't find wall at " + rc.adjacentLocation(heading) + ", im facing " + heading.rotateRight().rotateRight() + " at " + robot.myLoc);
            System.out.println("Wall disappeared, turning left to " + heading);

        } else {
            // wall directly in front
            if (!sensePassabilityReal(rc.adjacentLocation(heading))) {
                if (sensePassabilityReal(rc.adjacentLocation(heading.rotateLeft()))) {
                    // Skip past wall if possible
                    diag = headingTemp.rotateLeft();
                    heading = headingTemp.rotateLeft().rotateLeft();
                } else if (sensePassabilityReal(rc.adjacentLocation(heading.rotateRight()))) {
                    // Shortcut: go diagonal right and pretend you got there in two steps
                    diag = headingTemp.rotateRight();
                } else if (!sensePassabilityReal(rc.adjacentLocation(heading.rotateRight().rotateRight()))){
                    // Turn around if boxed in
                    heading = headingTemp.opposite();
                    System.out.println("Boxed in, flipping to " + heading);
                }
                else {
                    heading = headingTemp.rotateRight().rotateRight();
                }
            } else if (sensePassabilityReal(rc.adjacentLocation(heading.rotateLeft()))) {
                diag = headingTemp.rotateLeft();
                heading = headingTemp.rotateLeft().rotateLeft();
            }
            else System.out.println("Wall following, going " + heading);


        }
        int dist = robot.myLoc.distanceSquaredTo(target);
        if (dist < closestDistToTarget) {
            closestDistToTarget = dist;
            hopPoint = robot.myLoc;
        }
        if (diag != null) {
            System.out.println("Taking diagonal shortcut " + diag);
            return rc.canMove(diag) ? diag : Direction.CENTER;
        }
        if (!rc.canMove(heading)) {
            // Another bot in the way. Just wait for now
            System.out.println("Staying put, wanted to go " + heading);
            return Direction.CENTER;
        }

        System.out.println("Going " + heading);
        return heading;
    }
    public Direction bugNav(MapLocation target) throws GameActionException {
        Util.log("Running bugnav");
        basic = true;
        if (basic) return bugBasic(target);


            // Every 20 turns reset the closest distance to target
        ROUNDS_TO_RESET_BUG_CLOSEST = Integer.MAX_VALUE;
        if(roundsSinceClosestDistReset >= ROUNDS_TO_RESET_BUG_CLOSEST) {
            closestDistToTarget = Integer.MAX_VALUE;
            roundsSinceClosestDistReset = 0;
        }

        counter %= seen.length;
        for (MapLocation oldLoc : seen){
            if (robot.myLoc.equals(oldLoc)){
                basic = true;
                return bugBasic(target);
            }
        }
        seen[counter++] = robot.myLoc;

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
