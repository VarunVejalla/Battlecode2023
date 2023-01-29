package ali8;

import battlecode.common.*;

import javax.swing.*;
import java.util.HashSet;
import java.util.Map;

enum NavigationMode{
    FUZZYNAV, BUGNAV;
}
public class Navigation {

    final int ROUNDS_TO_LIVE = 5;
    final int NUM_TEMP_OBSTACLES = 20;
    MapLocation[] tempObstacles = new MapLocation[NUM_TEMP_OBSTACLES];
    int[] timesToLive = new int[NUM_TEMP_OBSTACLES];
    int BAD_CURRENT_SIZE = 50;
    MapLocation[] badCurrents = new MapLocation[BAD_CURRENT_SIZE];
    int badCurrentCounter = 0;
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
//        Util.log("rc.getType().movementCooldown: " + rc.getType().movementCooldown);
//        Util.log("rc.getMovementCooldownTurns(): " + rc.getMovementCooldownTurns());
//        Util.log("rc.isMovementReady(): " + rc.isMovementReady());
//        Util.log("isMyLastTurn(): " + isThisMyLastMovementTurn());

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

        Util.log("movement cooldown turns B: " + rc.getMovementCooldownTurns());
        Util.log("best direction to move in: " + " from " + robot.myLoc + ": " + bestDir);
        Util.log("newLoc from " + robot.myLoc + ": " + bestNewLoc);
        Util.log("leastNumMoves from " + robot.myLoc + ": " + leastNumMoves);
        Util.log("leastDistSq from " + robot.myLoc + ": " + leastDistanceSquared);
        Util.log("-------------------------");


        return bestDir;
    }

    public void resetBugNav() {
        closestDistToTarget = Integer.MAX_VALUE;
        lastWallFollowed = null;
        lastDirectionMoved = null;
        roundsSinceClosestDistReset = 0;
        basic = false;
        counter = 0;
        tempObstacles = new MapLocation[NUM_TEMP_OBSTACLES];
        timesToLive = new int[NUM_TEMP_OBSTACLES];
        badCurrents = new MapLocation[BAD_CURRENT_SIZE];
        badCurrentCounter = 0;
        seen = new MapLocation[ROUNDS_TO_RESET_BUG_CLOSEST*4];
    }
    public boolean sensePassabilityReal(MapLocation loc) throws GameActionException{
        if (! (0 <= loc.x && loc.x < rc.getMapWidth() && 0 <= loc.y & loc.y < rc.getMapHeight()))
            return false;
        for (MapLocation obstacleLoc: tempObstacles){
            if (loc.equals(obstacleLoc)) return false;
        }


        boolean noWall = rc.sensePassability(loc);
        RobotInfo bot = rc.senseRobotAtLocation(loc);
        if (noWall && bot != null) {
            int obstacleInd = 0;
            while (tempObstacles[obstacleInd] != null) obstacleInd++;
            if (bot.type != RobotType.HEADQUARTERS) {
                tempObstacles[obstacleInd] = loc;
                timesToLive[obstacleInd] = ROUNDS_TO_LIVE;
            }
            return false;
        }

        Direction myDir = robot.myLoc.directionTo(loc);
        Direction current = rc.senseMapInfo(loc).getCurrentDirection();
        if (current != Direction.CENTER) {
            for (int i = 0; i<badCurrentCounter; i++) {
                if (loc.equals(badCurrents[i]))
                    return false;
            }
        }
        if (current != Direction.CENTER && current != myDir &&
                current != myDir.rotateLeft() && current != myDir.rotateRight()){
            badCurrentCounter %= badCurrents.length;
            badCurrents[++badCurrentCounter] = loc;
        }
        return noWall;
    }
    public Direction bugBasic(MapLocation target) throws GameActionException {
        if (rc.getRoundNum() > 400) { return Direction.CENTER ;}
        int initial = Clock.getBytecodesLeft();
        if (followingWall) {
            for (int i = 0; i < tempObstacles.length; i++) {
                if (tempObstacles[i] == null) continue;
                timesToLive[i] -= 1;
                if (timesToLive[i] == 0) {
                    tempObstacles[i] = null;
                }
            }
        }
        //Util.log("Old heading " + heading );
        Direction bee = robot.myLoc.directionTo(target);
        if (beeOverwrite != null) {
            //Util.log("Direction overwritten to " + beeOverwrite);
            bee = beeOverwrite;
        }
        beeOverwrite = null;

        Direction diag = null; //Used for optimizing
        if (!followingWall) {
            if (sensePassabilityReal(rc.adjacentLocation(bee))) {
                heading = bee;
              //  Util.log("No walls, going " + heading);
                return bee;
            }
            else {
            //    Util.log("Can't go " + bee);
                heading = bee.rotateRight().rotateRight();
                wallStart = robot.myLoc;
                followingWall = true;

              //  Util.log("Bumped into wall, turning "+heading+"and starting wall tracking at " + robot.myLoc);
                closestDistToTarget = Integer.MAX_VALUE;

                if (heading == Direction.NORTHEAST || heading == Direction.NORTHWEST ||
                        heading == Direction.SOUTHEAST || heading == Direction.SOUTHWEST){
                    heading = heading.rotateLeft();
                  //  Util.log("Rare edge case, sending it " + heading);
                    beeOverwrite = heading;
                    followingWall = false;
                    return bugBasic(target);
                }
            }
        }
        Util.log("Segment 1 "+(initial-Clock.getBytecodesLeft()));
        initial = Clock.getBytecodesLeft();

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
          //  Util.log("Hopped off, heading " + heading);
            tempObstacles = new MapLocation[NUM_TEMP_OBSTACLES];
            timesToLive = new int[NUM_TEMP_OBSTACLES];
            return heading;
        }
        if (closestDistToTarget != Integer.MAX_VALUE && robot.myLoc.equals(wallStart)) {
         //   Util.log("Reached wall start, going to hop at " + hopPoint);
            if (hopping) {
                followingWall = false;
                return bugBasic(target);
                //Util.log("SOMETHING IS WRONG!!!");
                // ABORT!!!!
            }
            hopping = true;
        }
        Util.log("Segment 2 "+(initial-Clock.getBytecodesLeft()));
        initial = Clock.getBytecodesLeft();
        MapLocation toLeft = rc.adjacentLocation(heading.rotateLeft().rotateLeft());
        Direction headingTemp = heading;
        if (sensePassabilityReal(toLeft)) {
            // if wall to the left ends, turn left to keep wall on left-hand side
            heading = heading.rotateLeft().rotateLeft();

          //  Util.log("Can't find wall at " + rc.adjacentLocation(heading) + ", im facing " + heading.rotateRight().rotateRight() + " at " + robot.myLoc);
          //  Util.log("Wall disappeared, turning left to " + heading);

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
                  //  Util.log("Boxed in, flipping to " + heading);
                }
                else {
                    heading = headingTemp.rotateRight().rotateRight();
                }
            } else if (sensePassabilityReal(rc.adjacentLocation(heading.rotateLeft()))) {
                diag = headingTemp.rotateLeft();
                heading = headingTemp.rotateLeft().rotateLeft();
            }
            //else Util.log("Wall following, going " + heading);


        }
        Util.log("Segment 3 "+(initial-Clock.getBytecodesLeft()));
        initial = Clock.getBytecodesLeft();
        int dist = robot.myLoc.distanceSquaredTo(target);
        if (dist < closestDistToTarget) {
            closestDistToTarget = dist;
            hopPoint = robot.myLoc;
        }
        if (diag != null) {
            // Util.log("Taking diagonal shortcut " + diag);
            return rc.canMove(diag) ? diag : Direction.CENTER;
        }
        if (!rc.canMove(heading)) {
            // Another bot in the way. Just wait for now
            return Direction.CENTER;
        }

        // Util.log("Going " + heading);
        return heading;
    }
    public Direction bugNav(MapLocation target) throws GameActionException {
     //   Util.log("Running bugnav");
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
//            Util.log("ID: " + rc.getID());
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
