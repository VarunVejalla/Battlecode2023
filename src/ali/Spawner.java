package ali;
import battlecode.common.*;

public class Spawner{
    RobotController rc;
    Robot robot;


    public Spawner(RobotController rc, Robot robot){
        this.rc = rc;
        this.robot = robot;
    }

    // see this link for a visualization of the map locations with the encodings used her: https://docs.google.com/spreadsheets/d/1JZBAelcHcSx7x9B9q2wqjYOSKeEJ-bods9M-qlyBCnw/edit?usp=sharing

    public boolean trySpawnGeneralDirection(RobotType type, Direction spawnDir) throws GameActionException{
        switch(spawnDir){
            case NORTH: return trySpawnNorth(type);
            case SOUTH: return trySpawnSouth(type);
            case EAST: return trySpawnEast(type);
            case WEST: return trySpawnWest(type);
            case NORTHEAST: return trySpawnNorthEast(type);
            case NORTHWEST: return trySpawnNorthWest(type);
            case SOUTHEAST: return trySpawnSouthEast(type);
            case SOUTHWEST: return trySpawnSouthWest(type);
            default: return false;
        }
    }

    public boolean trySpawnNorth(RobotType type) throws GameActionException {
        int currX = robot.myLoc.x;
        int currY = robot.myLoc.y;

        MapLocation l11 = new MapLocation(currX +0, currY +3);  // (+0, +3) relative to center
        if(rc.canBuildRobot(type, l11)){
            rc.buildRobot(type, l11);
            return true;
        }
        MapLocation l18 = new MapLocation(currX +0, currY +2);  // (+0, +2) relative to center
        if(rc.canBuildRobot(type, l18)){
            rc.buildRobot(type, l18);
            return true;
        }
        MapLocation l17 = new MapLocation(currX -1, currY +2);  // (-1, +2) relative to center
        if(rc.canBuildRobot(type, l17)){
            rc.buildRobot(type, l17);
            return true;
        }
        MapLocation l19 = new MapLocation(currX +1, currY +2);  // (+1, +2) relative to center
        if(rc.canBuildRobot(type, l19)){
            rc.buildRobot(type, l19);
            return true;
        }
        MapLocation l25 = new MapLocation(currX +0, currY +1);  // (+0, +1) relative to center
        if(rc.canBuildRobot(type, l25)){
            rc.buildRobot(type, l25);
            return true;
        }
        MapLocation l24 = new MapLocation(currX -1, currY +1);  // (-1, +1) relative to center
        if(rc.canBuildRobot(type, l24)){
            rc.buildRobot(type, l24);
            return true;
        }
        MapLocation l26 = new MapLocation(currX +1, currY +1);  // (+1, +1) relative to center
        if(rc.canBuildRobot(type, l26)){
            rc.buildRobot(type, l26);
            return true;
        }
        MapLocation l16 = new MapLocation(currX -2, currY +2);  // (-2, +2) relative to center
        if(rc.canBuildRobot(type, l16)){
            rc.buildRobot(type, l16);
            return true;
        }
        MapLocation l20 = new MapLocation(currX +2, currY +2);  // (+2, +2) relative to center
        if(rc.canBuildRobot(type, l20)){
            rc.buildRobot(type, l20);
            return true;
        }
        MapLocation l23 = new MapLocation(currX -2, currY +1);  // (-2, +1) relative to center
        if(rc.canBuildRobot(type, l23)){
            rc.buildRobot(type, l23);
            return true;
        }
        MapLocation l27 = new MapLocation(currX +2, currY +1);  // (+2, +1) relative to center
        if(rc.canBuildRobot(type, l27)){
            rc.buildRobot(type, l27);
            return true;
        }
        MapLocation l32 = new MapLocation(currX +0, currY +0);  // (+0, +0) relative to center
        if(rc.canBuildRobot(type, l32)){
            rc.buildRobot(type, l32);
            return true;
        }
        MapLocation l31 = new MapLocation(currX -1, currY +0);  // (-1, +0) relative to center
        if(rc.canBuildRobot(type, l31)){
            rc.buildRobot(type, l31);
            return true;
        }
        MapLocation l33 = new MapLocation(currX +1, currY +0);  // (+1, +0) relative to center
        if(rc.canBuildRobot(type, l33)){
            rc.buildRobot(type, l33);
            return true;
        }
        MapLocation l30 = new MapLocation(currX -2, currY +0);  // (-2, +0) relative to center
        if(rc.canBuildRobot(type, l30)){
            rc.buildRobot(type, l30);
            return true;
        }
        MapLocation l34 = new MapLocation(currX +2, currY +0);  // (+2, +0) relative to center
        if(rc.canBuildRobot(type, l34)){
            rc.buildRobot(type, l34);
            return true;
        }
        MapLocation l39 = new MapLocation(currX +0, currY -1);  // (+0, -1) relative to center
        if(rc.canBuildRobot(type, l39)){
            rc.buildRobot(type, l39);
            return true;
        }
        MapLocation l38 = new MapLocation(currX -1, currY -1);  // (-1, -1) relative to center
        if(rc.canBuildRobot(type, l38)){
            rc.buildRobot(type, l38);
            return true;
        }
        MapLocation l40 = new MapLocation(currX +1, currY -1);  // (+1, -1) relative to center
        if(rc.canBuildRobot(type, l40)){
            rc.buildRobot(type, l40);
            return true;
        }
        MapLocation l29 = new MapLocation(currX -3, currY +0);  // (-3, +0) relative to center
        if(rc.canBuildRobot(type, l29)){
            rc.buildRobot(type, l29);
            return true;
        }
        MapLocation l35 = new MapLocation(currX +3, currY +0);  // (+3, +0) relative to center
        if(rc.canBuildRobot(type, l35)){
            rc.buildRobot(type, l35);
            return true;
        }
        MapLocation l37 = new MapLocation(currX -2, currY -1);  // (-2, -1) relative to center
        if(rc.canBuildRobot(type, l37)){
            rc.buildRobot(type, l37);
            return true;
        }
        MapLocation l41 = new MapLocation(currX +2, currY -1);  // (+2, -1) relative to center
        if(rc.canBuildRobot(type, l41)){
            rc.buildRobot(type, l41);
            return true;
        }
        MapLocation l46 = new MapLocation(currX +0, currY -2);  // (+0, -2) relative to center
        if(rc.canBuildRobot(type, l46)){
            rc.buildRobot(type, l46);
            return true;
        }
        MapLocation l45 = new MapLocation(currX -1, currY -2);  // (-1, -2) relative to center
        if(rc.canBuildRobot(type, l45)){
            rc.buildRobot(type, l45);
            return true;
        }
        MapLocation l47 = new MapLocation(currX +1, currY -2);  // (+1, -2) relative to center
        if(rc.canBuildRobot(type, l47)){
            rc.buildRobot(type, l47);
            return true;
        }
        MapLocation l44 = new MapLocation(currX -2, currY -2);  // (-2, -2) relative to center
        if(rc.canBuildRobot(type, l44)){
            rc.buildRobot(type, l44);
            return true;
        }
        MapLocation l48 = new MapLocation(currX +2, currY -2);  // (+2, -2) relative to center
        if(rc.canBuildRobot(type, l48)){
            rc.buildRobot(type, l48);
            return true;
        }
        MapLocation l53 = new MapLocation(currX +0, currY -3);  // (+0, -3) relative to center
        if(rc.canBuildRobot(type, l53)){
            rc.buildRobot(type, l53);
            return true;
        }
        return false;
    }

    public boolean trySpawnSouth(RobotType type) throws GameActionException {
        int currX = robot.myLoc.x;
        int currY = robot.myLoc.y;

        MapLocation l53 = new MapLocation(currX +0, currY -3);  // (+0, -3) relative to center
        if(rc.canBuildRobot(type, l53)){
            rc.buildRobot(type, l53);
            return true;
        }
        MapLocation l46 = new MapLocation(currX +0, currY -2);  // (+0, -2) relative to center
        if(rc.canBuildRobot(type, l46)){
            rc.buildRobot(type, l46);
            return true;
        }
        MapLocation l45 = new MapLocation(currX -1, currY -2);  // (-1, -2) relative to center
        if(rc.canBuildRobot(type, l45)){
            rc.buildRobot(type, l45);
            return true;
        }
        MapLocation l47 = new MapLocation(currX +1, currY -2);  // (+1, -2) relative to center
        if(rc.canBuildRobot(type, l47)){
            rc.buildRobot(type, l47);
            return true;
        }
        MapLocation l39 = new MapLocation(currX +0, currY -1);  // (+0, -1) relative to center
        if(rc.canBuildRobot(type, l39)){
            rc.buildRobot(type, l39);
            return true;
        }
        MapLocation l38 = new MapLocation(currX -1, currY -1);  // (-1, -1) relative to center
        if(rc.canBuildRobot(type, l38)){
            rc.buildRobot(type, l38);
            return true;
        }
        MapLocation l40 = new MapLocation(currX +1, currY -1);  // (+1, -1) relative to center
        if(rc.canBuildRobot(type, l40)){
            rc.buildRobot(type, l40);
            return true;
        }
        MapLocation l44 = new MapLocation(currX -2, currY -2);  // (-2, -2) relative to center
        if(rc.canBuildRobot(type, l44)){
            rc.buildRobot(type, l44);
            return true;
        }
        MapLocation l48 = new MapLocation(currX +2, currY -2);  // (+2, -2) relative to center
        if(rc.canBuildRobot(type, l48)){
            rc.buildRobot(type, l48);
            return true;
        }
        MapLocation l37 = new MapLocation(currX -2, currY -1);  // (-2, -1) relative to center
        if(rc.canBuildRobot(type, l37)){
            rc.buildRobot(type, l37);
            return true;
        }
        MapLocation l41 = new MapLocation(currX +2, currY -1);  // (+2, -1) relative to center
        if(rc.canBuildRobot(type, l41)){
            rc.buildRobot(type, l41);
            return true;
        }
        MapLocation l32 = new MapLocation(currX +0, currY +0);  // (+0, +0) relative to center
        if(rc.canBuildRobot(type, l32)){
            rc.buildRobot(type, l32);
            return true;
        }
        MapLocation l31 = new MapLocation(currX -1, currY +0);  // (-1, +0) relative to center
        if(rc.canBuildRobot(type, l31)){
            rc.buildRobot(type, l31);
            return true;
        }
        MapLocation l33 = new MapLocation(currX +1, currY +0);  // (+1, +0) relative to center
        if(rc.canBuildRobot(type, l33)){
            rc.buildRobot(type, l33);
            return true;
        }
        MapLocation l30 = new MapLocation(currX -2, currY +0);  // (-2, +0) relative to center
        if(rc.canBuildRobot(type, l30)){
            rc.buildRobot(type, l30);
            return true;
        }
        MapLocation l34 = new MapLocation(currX +2, currY +0);  // (+2, +0) relative to center
        if(rc.canBuildRobot(type, l34)){
            rc.buildRobot(type, l34);
            return true;
        }
        MapLocation l25 = new MapLocation(currX +0, currY +1);  // (+0, +1) relative to center
        if(rc.canBuildRobot(type, l25)){
            rc.buildRobot(type, l25);
            return true;
        }
        MapLocation l24 = new MapLocation(currX -1, currY +1);  // (-1, +1) relative to center
        if(rc.canBuildRobot(type, l24)){
            rc.buildRobot(type, l24);
            return true;
        }
        MapLocation l26 = new MapLocation(currX +1, currY +1);  // (+1, +1) relative to center
        if(rc.canBuildRobot(type, l26)){
            rc.buildRobot(type, l26);
            return true;
        }
        MapLocation l29 = new MapLocation(currX -3, currY +0);  // (-3, +0) relative to center
        if(rc.canBuildRobot(type, l29)){
            rc.buildRobot(type, l29);
            return true;
        }
        MapLocation l35 = new MapLocation(currX +3, currY +0);  // (+3, +0) relative to center
        if(rc.canBuildRobot(type, l35)){
            rc.buildRobot(type, l35);
            return true;
        }
        MapLocation l23 = new MapLocation(currX -2, currY +1);  // (-2, +1) relative to center
        if(rc.canBuildRobot(type, l23)){
            rc.buildRobot(type, l23);
            return true;
        }
        MapLocation l27 = new MapLocation(currX +2, currY +1);  // (+2, +1) relative to center
        if(rc.canBuildRobot(type, l27)){
            rc.buildRobot(type, l27);
            return true;
        }
        MapLocation l18 = new MapLocation(currX +0, currY +2);  // (+0, +2) relative to center
        if(rc.canBuildRobot(type, l18)){
            rc.buildRobot(type, l18);
            return true;
        }
        MapLocation l17 = new MapLocation(currX -1, currY +2);  // (-1, +2) relative to center
        if(rc.canBuildRobot(type, l17)){
            rc.buildRobot(type, l17);
            return true;
        }
        MapLocation l19 = new MapLocation(currX +1, currY +2);  // (+1, +2) relative to center
        if(rc.canBuildRobot(type, l19)){
            rc.buildRobot(type, l19);
            return true;
        }
        MapLocation l16 = new MapLocation(currX -2, currY +2);  // (-2, +2) relative to center
        if(rc.canBuildRobot(type, l16)){
            rc.buildRobot(type, l16);
            return true;
        }
        MapLocation l20 = new MapLocation(currX +2, currY +2);  // (+2, +2) relative to center
        if(rc.canBuildRobot(type, l20)){
            rc.buildRobot(type, l20);
            return true;
        }
        MapLocation l11 = new MapLocation(currX +0, currY +3);  // (+0, +3) relative to center
        if(rc.canBuildRobot(type, l11)){
            rc.buildRobot(type, l11);
            return true;
        }
        return false;
    }

    public boolean trySpawnEast(RobotType type) throws GameActionException {
        int currX = robot.myLoc.x;
        int currY = robot.myLoc.y;

        MapLocation l35 = new MapLocation(currX +3, currY +0);  // (+3, +0) relative to center
        if(rc.canBuildRobot(type, l35)){
            rc.buildRobot(type, l35);
            return true;
        }
        MapLocation l34 = new MapLocation(currX +2, currY +0);  // (+2, +0) relative to center
        if(rc.canBuildRobot(type, l34)){
            rc.buildRobot(type, l34);
            return true;
        }
        MapLocation l41 = new MapLocation(currX +2, currY -1);  // (+2, -1) relative to center
        if(rc.canBuildRobot(type, l41)){
            rc.buildRobot(type, l41);
            return true;
        }
        MapLocation l27 = new MapLocation(currX +2, currY +1);  // (+2, +1) relative to center
        if(rc.canBuildRobot(type, l27)){
            rc.buildRobot(type, l27);
            return true;
        }
        MapLocation l33 = new MapLocation(currX +1, currY +0);  // (+1, +0) relative to center
        if(rc.canBuildRobot(type, l33)){
            rc.buildRobot(type, l33);
            return true;
        }
        MapLocation l40 = new MapLocation(currX +1, currY -1);  // (+1, -1) relative to center
        if(rc.canBuildRobot(type, l40)){
            rc.buildRobot(type, l40);
            return true;
        }
        MapLocation l48 = new MapLocation(currX +2, currY -2);  // (+2, -2) relative to center
        if(rc.canBuildRobot(type, l48)){
            rc.buildRobot(type, l48);
            return true;
        }
        MapLocation l26 = new MapLocation(currX +1, currY +1);  // (+1, +1) relative to center
        if(rc.canBuildRobot(type, l26)){
            rc.buildRobot(type, l26);
            return true;
        }
        MapLocation l20 = new MapLocation(currX +2, currY +2);  // (+2, +2) relative to center
        if(rc.canBuildRobot(type, l20)){
            rc.buildRobot(type, l20);
            return true;
        }
        MapLocation l47 = new MapLocation(currX +1, currY -2);  // (+1, -2) relative to center
        if(rc.canBuildRobot(type, l47)){
            rc.buildRobot(type, l47);
            return true;
        }
        MapLocation l19 = new MapLocation(currX +1, currY +2);  // (+1, +2) relative to center
        if(rc.canBuildRobot(type, l19)){
            rc.buildRobot(type, l19);
            return true;
        }
        MapLocation l32 = new MapLocation(currX +0, currY +0);  // (+0, +0) relative to center
        if(rc.canBuildRobot(type, l32)){
            rc.buildRobot(type, l32);
            return true;
        }
        MapLocation l39 = new MapLocation(currX +0, currY -1);  // (+0, -1) relative to center
        if(rc.canBuildRobot(type, l39)){
            rc.buildRobot(type, l39);
            return true;
        }
        MapLocation l25 = new MapLocation(currX +0, currY +1);  // (+0, +1) relative to center
        if(rc.canBuildRobot(type, l25)){
            rc.buildRobot(type, l25);
            return true;
        }
        MapLocation l46 = new MapLocation(currX +0, currY -2);  // (+0, -2) relative to center
        if(rc.canBuildRobot(type, l46)){
            rc.buildRobot(type, l46);
            return true;
        }
        MapLocation l18 = new MapLocation(currX +0, currY +2);  // (+0, +2) relative to center
        if(rc.canBuildRobot(type, l18)){
            rc.buildRobot(type, l18);
            return true;
        }
        MapLocation l31 = new MapLocation(currX -1, currY +0);  // (-1, +0) relative to center
        if(rc.canBuildRobot(type, l31)){
            rc.buildRobot(type, l31);
            return true;
        }
        MapLocation l38 = new MapLocation(currX -1, currY -1);  // (-1, -1) relative to center
        if(rc.canBuildRobot(type, l38)){
            rc.buildRobot(type, l38);
            return true;
        }
        MapLocation l24 = new MapLocation(currX -1, currY +1);  // (-1, +1) relative to center
        if(rc.canBuildRobot(type, l24)){
            rc.buildRobot(type, l24);
            return true;
        }
        MapLocation l53 = new MapLocation(currX +0, currY -3);  // (+0, -3) relative to center
        if(rc.canBuildRobot(type, l53)){
            rc.buildRobot(type, l53);
            return true;
        }
        MapLocation l11 = new MapLocation(currX +0, currY +3);  // (+0, +3) relative to center
        if(rc.canBuildRobot(type, l11)){
            rc.buildRobot(type, l11);
            return true;
        }
        MapLocation l45 = new MapLocation(currX -1, currY -2);  // (-1, -2) relative to center
        if(rc.canBuildRobot(type, l45)){
            rc.buildRobot(type, l45);
            return true;
        }
        MapLocation l17 = new MapLocation(currX -1, currY +2);  // (-1, +2) relative to center
        if(rc.canBuildRobot(type, l17)){
            rc.buildRobot(type, l17);
            return true;
        }
        MapLocation l30 = new MapLocation(currX -2, currY +0);  // (-2, +0) relative to center
        if(rc.canBuildRobot(type, l30)){
            rc.buildRobot(type, l30);
            return true;
        }
        MapLocation l37 = new MapLocation(currX -2, currY -1);  // (-2, -1) relative to center
        if(rc.canBuildRobot(type, l37)){
            rc.buildRobot(type, l37);
            return true;
        }
        MapLocation l23 = new MapLocation(currX -2, currY +1);  // (-2, +1) relative to center
        if(rc.canBuildRobot(type, l23)){
            rc.buildRobot(type, l23);
            return true;
        }
        MapLocation l44 = new MapLocation(currX -2, currY -2);  // (-2, -2) relative to center
        if(rc.canBuildRobot(type, l44)){
            rc.buildRobot(type, l44);
            return true;
        }
        MapLocation l16 = new MapLocation(currX -2, currY +2);  // (-2, +2) relative to center
        if(rc.canBuildRobot(type, l16)){
            rc.buildRobot(type, l16);
            return true;
        }
        MapLocation l29 = new MapLocation(currX -3, currY +0);  // (-3, +0) relative to center
        if(rc.canBuildRobot(type, l29)){
            rc.buildRobot(type, l29);
            return true;
        }
        return false;
    }

    public boolean trySpawnWest(RobotType type) throws GameActionException {
        int currX = robot.myLoc.x;
        int currY = robot.myLoc.y;

        MapLocation l29 = new MapLocation(currX -3, currY +0);  // (-3, +0) relative to center
        if(rc.canBuildRobot(type, l29)){
            rc.buildRobot(type, l29);
            return true;
        }
        MapLocation l30 = new MapLocation(currX -2, currY +0);  // (-2, +0) relative to center
        if(rc.canBuildRobot(type, l30)){
            rc.buildRobot(type, l30);
            return true;
        }
        MapLocation l37 = new MapLocation(currX -2, currY -1);  // (-2, -1) relative to center
        if(rc.canBuildRobot(type, l37)){
            rc.buildRobot(type, l37);
            return true;
        }
        MapLocation l23 = new MapLocation(currX -2, currY +1);  // (-2, +1) relative to center
        if(rc.canBuildRobot(type, l23)){
            rc.buildRobot(type, l23);
            return true;
        }
        MapLocation l31 = new MapLocation(currX -1, currY +0);  // (-1, +0) relative to center
        if(rc.canBuildRobot(type, l31)){
            rc.buildRobot(type, l31);
            return true;
        }
        MapLocation l38 = new MapLocation(currX -1, currY -1);  // (-1, -1) relative to center
        if(rc.canBuildRobot(type, l38)){
            rc.buildRobot(type, l38);
            return true;
        }
        MapLocation l24 = new MapLocation(currX -1, currY +1);  // (-1, +1) relative to center
        if(rc.canBuildRobot(type, l24)){
            rc.buildRobot(type, l24);
            return true;
        }
        MapLocation l44 = new MapLocation(currX -2, currY -2);  // (-2, -2) relative to center
        if(rc.canBuildRobot(type, l44)){
            rc.buildRobot(type, l44);
            return true;
        }
        MapLocation l16 = new MapLocation(currX -2, currY +2);  // (-2, +2) relative to center
        if(rc.canBuildRobot(type, l16)){
            rc.buildRobot(type, l16);
            return true;
        }
        MapLocation l45 = new MapLocation(currX -1, currY -2);  // (-1, -2) relative to center
        if(rc.canBuildRobot(type, l45)){
            rc.buildRobot(type, l45);
            return true;
        }
        MapLocation l17 = new MapLocation(currX -1, currY +2);  // (-1, +2) relative to center
        if(rc.canBuildRobot(type, l17)){
            rc.buildRobot(type, l17);
            return true;
        }
        MapLocation l32 = new MapLocation(currX +0, currY +0);  // (+0, +0) relative to center
        if(rc.canBuildRobot(type, l32)){
            rc.buildRobot(type, l32);
            return true;
        }
        MapLocation l39 = new MapLocation(currX +0, currY -1);  // (+0, -1) relative to center
        if(rc.canBuildRobot(type, l39)){
            rc.buildRobot(type, l39);
            return true;
        }
        MapLocation l25 = new MapLocation(currX +0, currY +1);  // (+0, +1) relative to center
        if(rc.canBuildRobot(type, l25)){
            rc.buildRobot(type, l25);
            return true;
        }
        MapLocation l46 = new MapLocation(currX +0, currY -2);  // (+0, -2) relative to center
        if(rc.canBuildRobot(type, l46)){
            rc.buildRobot(type, l46);
            return true;
        }
        MapLocation l18 = new MapLocation(currX +0, currY +2);  // (+0, +2) relative to center
        if(rc.canBuildRobot(type, l18)){
            rc.buildRobot(type, l18);
            return true;
        }
        MapLocation l33 = new MapLocation(currX +1, currY +0);  // (+1, +0) relative to center
        if(rc.canBuildRobot(type, l33)){
            rc.buildRobot(type, l33);
            return true;
        }
        MapLocation l40 = new MapLocation(currX +1, currY -1);  // (+1, -1) relative to center
        if(rc.canBuildRobot(type, l40)){
            rc.buildRobot(type, l40);
            return true;
        }
        MapLocation l26 = new MapLocation(currX +1, currY +1);  // (+1, +1) relative to center
        if(rc.canBuildRobot(type, l26)){
            rc.buildRobot(type, l26);
            return true;
        }
        MapLocation l53 = new MapLocation(currX +0, currY -3);  // (+0, -3) relative to center
        if(rc.canBuildRobot(type, l53)){
            rc.buildRobot(type, l53);
            return true;
        }
        MapLocation l11 = new MapLocation(currX +0, currY +3);  // (+0, +3) relative to center
        if(rc.canBuildRobot(type, l11)){
            rc.buildRobot(type, l11);
            return true;
        }
        MapLocation l47 = new MapLocation(currX +1, currY -2);  // (+1, -2) relative to center
        if(rc.canBuildRobot(type, l47)){
            rc.buildRobot(type, l47);
            return true;
        }
        MapLocation l19 = new MapLocation(currX +1, currY +2);  // (+1, +2) relative to center
        if(rc.canBuildRobot(type, l19)){
            rc.buildRobot(type, l19);
            return true;
        }
        MapLocation l34 = new MapLocation(currX +2, currY +0);  // (+2, +0) relative to center
        if(rc.canBuildRobot(type, l34)){
            rc.buildRobot(type, l34);
            return true;
        }
        MapLocation l41 = new MapLocation(currX +2, currY -1);  // (+2, -1) relative to center
        if(rc.canBuildRobot(type, l41)){
            rc.buildRobot(type, l41);
            return true;
        }
        MapLocation l27 = new MapLocation(currX +2, currY +1);  // (+2, +1) relative to center
        if(rc.canBuildRobot(type, l27)){
            rc.buildRobot(type, l27);
            return true;
        }
        MapLocation l48 = new MapLocation(currX +2, currY -2);  // (+2, -2) relative to center
        if(rc.canBuildRobot(type, l48)){
            rc.buildRobot(type, l48);
            return true;
        }
        MapLocation l20 = new MapLocation(currX +2, currY +2);  // (+2, +2) relative to center
        if(rc.canBuildRobot(type, l20)){
            rc.buildRobot(type, l20);
            return true;
        }
        MapLocation l35 = new MapLocation(currX +3, currY +0);  // (+3, +0) relative to center
        if(rc.canBuildRobot(type, l35)){
            rc.buildRobot(type, l35);
            return true;
        }
        return false;
    }

    public boolean trySpawnNorthEast(RobotType type) throws GameActionException {
        int currX = robot.myLoc.x;
        int currY = robot.myLoc.y;

        MapLocation l20 = new MapLocation(currX +2, currY +2);  // (+2, +2) relative to center
        if(rc.canBuildRobot(type, l20)){
            rc.buildRobot(type, l20);
            return true;
        }
        MapLocation l19 = new MapLocation(currX +1, currY +2);  // (+1, +2) relative to center
        if(rc.canBuildRobot(type, l19)){
            rc.buildRobot(type, l19);
            return true;
        }
        MapLocation l27 = new MapLocation(currX +2, currY +1);  // (+2, +1) relative to center
        if(rc.canBuildRobot(type, l27)){
            rc.buildRobot(type, l27);
            return true;
        }
        MapLocation l26 = new MapLocation(currX +1, currY +1);  // (+1, +1) relative to center
        if(rc.canBuildRobot(type, l26)){
            rc.buildRobot(type, l26);
            return true;
        }
        MapLocation l11 = new MapLocation(currX +0, currY +3);  // (+0, +3) relative to center
        if(rc.canBuildRobot(type, l11)){
            rc.buildRobot(type, l11);
            return true;
        }
        MapLocation l35 = new MapLocation(currX +3, currY +0);  // (+3, +0) relative to center
        if(rc.canBuildRobot(type, l35)){
            rc.buildRobot(type, l35);
            return true;
        }
        MapLocation l18 = new MapLocation(currX +0, currY +2);  // (+0, +2) relative to center
        if(rc.canBuildRobot(type, l18)){
            rc.buildRobot(type, l18);
            return true;
        }
        MapLocation l34 = new MapLocation(currX +2, currY +0);  // (+2, +0) relative to center
        if(rc.canBuildRobot(type, l34)){
            rc.buildRobot(type, l34);
            return true;
        }
        MapLocation l25 = new MapLocation(currX +0, currY +1);  // (+0, +1) relative to center
        if(rc.canBuildRobot(type, l25)){
            rc.buildRobot(type, l25);
            return true;
        }
        MapLocation l33 = new MapLocation(currX +1, currY +0);  // (+1, +0) relative to center
        if(rc.canBuildRobot(type, l33)){
            rc.buildRobot(type, l33);
            return true;
        }
        MapLocation l17 = new MapLocation(currX -1, currY +2);  // (-1, +2) relative to center
        if(rc.canBuildRobot(type, l17)){
            rc.buildRobot(type, l17);
            return true;
        }
        MapLocation l41 = new MapLocation(currX +2, currY -1);  // (+2, -1) relative to center
        if(rc.canBuildRobot(type, l41)){
            rc.buildRobot(type, l41);
            return true;
        }
        MapLocation l32 = new MapLocation(currX +0, currY +0);  // (+0, +0) relative to center
        if(rc.canBuildRobot(type, l32)){
            rc.buildRobot(type, l32);
            return true;
        }
        MapLocation l24 = new MapLocation(currX -1, currY +1);  // (-1, +1) relative to center
        if(rc.canBuildRobot(type, l24)){
            rc.buildRobot(type, l24);
            return true;
        }
        MapLocation l40 = new MapLocation(currX +1, currY -1);  // (+1, -1) relative to center
        if(rc.canBuildRobot(type, l40)){
            rc.buildRobot(type, l40);
            return true;
        }
        MapLocation l31 = new MapLocation(currX -1, currY +0);  // (-1, +0) relative to center
        if(rc.canBuildRobot(type, l31)){
            rc.buildRobot(type, l31);
            return true;
        }
        MapLocation l39 = new MapLocation(currX +0, currY -1);  // (+0, -1) relative to center
        if(rc.canBuildRobot(type, l39)){
            rc.buildRobot(type, l39);
            return true;
        }
        MapLocation l16 = new MapLocation(currX -2, currY +2);  // (-2, +2) relative to center
        if(rc.canBuildRobot(type, l16)){
            rc.buildRobot(type, l16);
            return true;
        }
        MapLocation l48 = new MapLocation(currX +2, currY -2);  // (+2, -2) relative to center
        if(rc.canBuildRobot(type, l48)){
            rc.buildRobot(type, l48);
            return true;
        }
        MapLocation l23 = new MapLocation(currX -2, currY +1);  // (-2, +1) relative to center
        if(rc.canBuildRobot(type, l23)){
            rc.buildRobot(type, l23);
            return true;
        }
        MapLocation l47 = new MapLocation(currX +1, currY -2);  // (+1, -2) relative to center
        if(rc.canBuildRobot(type, l47)){
            rc.buildRobot(type, l47);
            return true;
        }
        MapLocation l38 = new MapLocation(currX -1, currY -1);  // (-1, -1) relative to center
        if(rc.canBuildRobot(type, l38)){
            rc.buildRobot(type, l38);
            return true;
        }
        MapLocation l30 = new MapLocation(currX -2, currY +0);  // (-2, +0) relative to center
        if(rc.canBuildRobot(type, l30)){
            rc.buildRobot(type, l30);
            return true;
        }
        MapLocation l46 = new MapLocation(currX +0, currY -2);  // (+0, -2) relative to center
        if(rc.canBuildRobot(type, l46)){
            rc.buildRobot(type, l46);
            return true;
        }
        MapLocation l37 = new MapLocation(currX -2, currY -1);  // (-2, -1) relative to center
        if(rc.canBuildRobot(type, l37)){
            rc.buildRobot(type, l37);
            return true;
        }
        MapLocation l45 = new MapLocation(currX -1, currY -2);  // (-1, -2) relative to center
        if(rc.canBuildRobot(type, l45)){
            rc.buildRobot(type, l45);
            return true;
        }
        MapLocation l29 = new MapLocation(currX -3, currY +0);  // (-3, +0) relative to center
        if(rc.canBuildRobot(type, l29)){
            rc.buildRobot(type, l29);
            return true;
        }
        MapLocation l53 = new MapLocation(currX +0, currY -3);  // (+0, -3) relative to center
        if(rc.canBuildRobot(type, l53)){
            rc.buildRobot(type, l53);
            return true;
        }
        MapLocation l44 = new MapLocation(currX -2, currY -2);  // (-2, -2) relative to center
        if(rc.canBuildRobot(type, l44)){
            rc.buildRobot(type, l44);
            return true;
        }
        return false;
    }

    public boolean trySpawnNorthWest(RobotType type) throws GameActionException {
        int currX = robot.myLoc.x;
        int currY = robot.myLoc.y;

        MapLocation l16 = new MapLocation(currX -2, currY +2);  // (-2, +2) relative to center
        if(rc.canBuildRobot(type, l16)){
            rc.buildRobot(type, l16);
            return true;
        }
        MapLocation l17 = new MapLocation(currX -1, currY +2);  // (-1, +2) relative to center
        if(rc.canBuildRobot(type, l17)){
            rc.buildRobot(type, l17);
            return true;
        }
        MapLocation l23 = new MapLocation(currX -2, currY +1);  // (-2, +1) relative to center
        if(rc.canBuildRobot(type, l23)){
            rc.buildRobot(type, l23);
            return true;
        }
        MapLocation l24 = new MapLocation(currX -1, currY +1);  // (-1, +1) relative to center
        if(rc.canBuildRobot(type, l24)){
            rc.buildRobot(type, l24);
            return true;
        }
        MapLocation l11 = new MapLocation(currX +0, currY +3);  // (+0, +3) relative to center
        if(rc.canBuildRobot(type, l11)){
            rc.buildRobot(type, l11);
            return true;
        }
        MapLocation l29 = new MapLocation(currX -3, currY +0);  // (-3, +0) relative to center
        if(rc.canBuildRobot(type, l29)){
            rc.buildRobot(type, l29);
            return true;
        }
        MapLocation l18 = new MapLocation(currX +0, currY +2);  // (+0, +2) relative to center
        if(rc.canBuildRobot(type, l18)){
            rc.buildRobot(type, l18);
            return true;
        }
        MapLocation l30 = new MapLocation(currX -2, currY +0);  // (-2, +0) relative to center
        if(rc.canBuildRobot(type, l30)){
            rc.buildRobot(type, l30);
            return true;
        }
        MapLocation l25 = new MapLocation(currX +0, currY +1);  // (+0, +1) relative to center
        if(rc.canBuildRobot(type, l25)){
            rc.buildRobot(type, l25);
            return true;
        }
        MapLocation l31 = new MapLocation(currX -1, currY +0);  // (-1, +0) relative to center
        if(rc.canBuildRobot(type, l31)){
            rc.buildRobot(type, l31);
            return true;
        }
        MapLocation l19 = new MapLocation(currX +1, currY +2);  // (+1, +2) relative to center
        if(rc.canBuildRobot(type, l19)){
            rc.buildRobot(type, l19);
            return true;
        }
        MapLocation l37 = new MapLocation(currX -2, currY -1);  // (-2, -1) relative to center
        if(rc.canBuildRobot(type, l37)){
            rc.buildRobot(type, l37);
            return true;
        }
        MapLocation l32 = new MapLocation(currX +0, currY +0);  // (+0, +0) relative to center
        if(rc.canBuildRobot(type, l32)){
            rc.buildRobot(type, l32);
            return true;
        }
        MapLocation l26 = new MapLocation(currX +1, currY +1);  // (+1, +1) relative to center
        if(rc.canBuildRobot(type, l26)){
            rc.buildRobot(type, l26);
            return true;
        }
        MapLocation l38 = new MapLocation(currX -1, currY -1);  // (-1, -1) relative to center
        if(rc.canBuildRobot(type, l38)){
            rc.buildRobot(type, l38);
            return true;
        }
        MapLocation l33 = new MapLocation(currX +1, currY +0);  // (+1, +0) relative to center
        if(rc.canBuildRobot(type, l33)){
            rc.buildRobot(type, l33);
            return true;
        }
        MapLocation l39 = new MapLocation(currX +0, currY -1);  // (+0, -1) relative to center
        if(rc.canBuildRobot(type, l39)){
            rc.buildRobot(type, l39);
            return true;
        }
        MapLocation l20 = new MapLocation(currX +2, currY +2);  // (+2, +2) relative to center
        if(rc.canBuildRobot(type, l20)){
            rc.buildRobot(type, l20);
            return true;
        }
        MapLocation l44 = new MapLocation(currX -2, currY -2);  // (-2, -2) relative to center
        if(rc.canBuildRobot(type, l44)){
            rc.buildRobot(type, l44);
            return true;
        }
        MapLocation l27 = new MapLocation(currX +2, currY +1);  // (+2, +1) relative to center
        if(rc.canBuildRobot(type, l27)){
            rc.buildRobot(type, l27);
            return true;
        }
        MapLocation l45 = new MapLocation(currX -1, currY -2);  // (-1, -2) relative to center
        if(rc.canBuildRobot(type, l45)){
            rc.buildRobot(type, l45);
            return true;
        }
        MapLocation l40 = new MapLocation(currX +1, currY -1);  // (+1, -1) relative to center
        if(rc.canBuildRobot(type, l40)){
            rc.buildRobot(type, l40);
            return true;
        }
        MapLocation l34 = new MapLocation(currX +2, currY +0);  // (+2, +0) relative to center
        if(rc.canBuildRobot(type, l34)){
            rc.buildRobot(type, l34);
            return true;
        }
        MapLocation l46 = new MapLocation(currX +0, currY -2);  // (+0, -2) relative to center
        if(rc.canBuildRobot(type, l46)){
            rc.buildRobot(type, l46);
            return true;
        }
        MapLocation l41 = new MapLocation(currX +2, currY -1);  // (+2, -1) relative to center
        if(rc.canBuildRobot(type, l41)){
            rc.buildRobot(type, l41);
            return true;
        }
        MapLocation l47 = new MapLocation(currX +1, currY -2);  // (+1, -2) relative to center
        if(rc.canBuildRobot(type, l47)){
            rc.buildRobot(type, l47);
            return true;
        }
        MapLocation l35 = new MapLocation(currX +3, currY +0);  // (+3, +0) relative to center
        if(rc.canBuildRobot(type, l35)){
            rc.buildRobot(type, l35);
            return true;
        }
        MapLocation l53 = new MapLocation(currX +0, currY -3);  // (+0, -3) relative to center
        if(rc.canBuildRobot(type, l53)){
            rc.buildRobot(type, l53);
            return true;
        }
        MapLocation l48 = new MapLocation(currX +2, currY -2);  // (+2, -2) relative to center
        if(rc.canBuildRobot(type, l48)){
            rc.buildRobot(type, l48);
            return true;
        }
        return false;
    }

    public boolean trySpawnSouthWest(RobotType type) throws GameActionException {
        int currX = robot.myLoc.x;
        int currY = robot.myLoc.y;

        MapLocation l44 = new MapLocation(currX -2, currY -2);  // (-2, -2) relative to center
        if(rc.canBuildRobot(type, l44)){
            rc.buildRobot(type, l44);
            return true;
        }
        MapLocation l37 = new MapLocation(currX -2, currY -1);  // (-2, -1) relative to center
        if(rc.canBuildRobot(type, l37)){
            rc.buildRobot(type, l37);
            return true;
        }
        MapLocation l45 = new MapLocation(currX -1, currY -2);  // (-1, -2) relative to center
        if(rc.canBuildRobot(type, l45)){
            rc.buildRobot(type, l45);
            return true;
        }
        MapLocation l38 = new MapLocation(currX -1, currY -1);  // (-1, -1) relative to center
        if(rc.canBuildRobot(type, l38)){
            rc.buildRobot(type, l38);
            return true;
        }
        MapLocation l29 = new MapLocation(currX -3, currY +0);  // (-3, +0) relative to center
        if(rc.canBuildRobot(type, l29)){
            rc.buildRobot(type, l29);
            return true;
        }
        MapLocation l53 = new MapLocation(currX +0, currY -3);  // (+0, -3) relative to center
        if(rc.canBuildRobot(type, l53)){
            rc.buildRobot(type, l53);
            return true;
        }
        MapLocation l30 = new MapLocation(currX -2, currY +0);  // (-2, +0) relative to center
        if(rc.canBuildRobot(type, l30)){
            rc.buildRobot(type, l30);
            return true;
        }
        MapLocation l46 = new MapLocation(currX +0, currY -2);  // (+0, -2) relative to center
        if(rc.canBuildRobot(type, l46)){
            rc.buildRobot(type, l46);
            return true;
        }
        MapLocation l31 = new MapLocation(currX -1, currY +0);  // (-1, +0) relative to center
        if(rc.canBuildRobot(type, l31)){
            rc.buildRobot(type, l31);
            return true;
        }
        MapLocation l39 = new MapLocation(currX +0, currY -1);  // (+0, -1) relative to center
        if(rc.canBuildRobot(type, l39)){
            rc.buildRobot(type, l39);
            return true;
        }
        MapLocation l23 = new MapLocation(currX -2, currY +1);  // (-2, +1) relative to center
        if(rc.canBuildRobot(type, l23)){
            rc.buildRobot(type, l23);
            return true;
        }
        MapLocation l47 = new MapLocation(currX +1, currY -2);  // (+1, -2) relative to center
        if(rc.canBuildRobot(type, l47)){
            rc.buildRobot(type, l47);
            return true;
        }
        MapLocation l32 = new MapLocation(currX +0, currY +0);  // (+0, +0) relative to center
        if(rc.canBuildRobot(type, l32)){
            rc.buildRobot(type, l32);
            return true;
        }
        MapLocation l24 = new MapLocation(currX -1, currY +1);  // (-1, +1) relative to center
        if(rc.canBuildRobot(type, l24)){
            rc.buildRobot(type, l24);
            return true;
        }
        MapLocation l40 = new MapLocation(currX +1, currY -1);  // (+1, -1) relative to center
        if(rc.canBuildRobot(type, l40)){
            rc.buildRobot(type, l40);
            return true;
        }
        MapLocation l25 = new MapLocation(currX +0, currY +1);  // (+0, +1) relative to center
        if(rc.canBuildRobot(type, l25)){
            rc.buildRobot(type, l25);
            return true;
        }
        MapLocation l33 = new MapLocation(currX +1, currY +0);  // (+1, +0) relative to center
        if(rc.canBuildRobot(type, l33)){
            rc.buildRobot(type, l33);
            return true;
        }
        MapLocation l16 = new MapLocation(currX -2, currY +2);  // (-2, +2) relative to center
        if(rc.canBuildRobot(type, l16)){
            rc.buildRobot(type, l16);
            return true;
        }
        MapLocation l48 = new MapLocation(currX +2, currY -2);  // (+2, -2) relative to center
        if(rc.canBuildRobot(type, l48)){
            rc.buildRobot(type, l48);
            return true;
        }
        MapLocation l17 = new MapLocation(currX -1, currY +2);  // (-1, +2) relative to center
        if(rc.canBuildRobot(type, l17)){
            rc.buildRobot(type, l17);
            return true;
        }
        MapLocation l41 = new MapLocation(currX +2, currY -1);  // (+2, -1) relative to center
        if(rc.canBuildRobot(type, l41)){
            rc.buildRobot(type, l41);
            return true;
        }
        MapLocation l26 = new MapLocation(currX +1, currY +1);  // (+1, +1) relative to center
        if(rc.canBuildRobot(type, l26)){
            rc.buildRobot(type, l26);
            return true;
        }
        MapLocation l18 = new MapLocation(currX +0, currY +2);  // (+0, +2) relative to center
        if(rc.canBuildRobot(type, l18)){
            rc.buildRobot(type, l18);
            return true;
        }
        MapLocation l34 = new MapLocation(currX +2, currY +0);  // (+2, +0) relative to center
        if(rc.canBuildRobot(type, l34)){
            rc.buildRobot(type, l34);
            return true;
        }
        MapLocation l19 = new MapLocation(currX +1, currY +2);  // (+1, +2) relative to center
        if(rc.canBuildRobot(type, l19)){
            rc.buildRobot(type, l19);
            return true;
        }
        MapLocation l27 = new MapLocation(currX +2, currY +1);  // (+2, +1) relative to center
        if(rc.canBuildRobot(type, l27)){
            rc.buildRobot(type, l27);
            return true;
        }
        MapLocation l11 = new MapLocation(currX +0, currY +3);  // (+0, +3) relative to center
        if(rc.canBuildRobot(type, l11)){
            rc.buildRobot(type, l11);
            return true;
        }
        MapLocation l35 = new MapLocation(currX +3, currY +0);  // (+3, +0) relative to center
        if(rc.canBuildRobot(type, l35)){
            rc.buildRobot(type, l35);
            return true;
        }
        MapLocation l20 = new MapLocation(currX +2, currY +2);  // (+2, +2) relative to center
        if(rc.canBuildRobot(type, l20)){
            rc.buildRobot(type, l20);
            return true;
        }
        return false;
    }

    public boolean trySpawnSouthEast(RobotType type) throws GameActionException {
        int currX = robot.myLoc.x;
        int currY = robot.myLoc.y;

        MapLocation l48 = new MapLocation(currX +2, currY -2);  // (+2, -2) relative to center
        if(rc.canBuildRobot(type, l48)){
            rc.buildRobot(type, l48);
            return true;
        }
        MapLocation l47 = new MapLocation(currX +1, currY -2);  // (+1, -2) relative to center
        if(rc.canBuildRobot(type, l47)){
            rc.buildRobot(type, l47);
            return true;
        }
        MapLocation l41 = new MapLocation(currX +2, currY -1);  // (+2, -1) relative to center
        if(rc.canBuildRobot(type, l41)){
            rc.buildRobot(type, l41);
            return true;
        }
        MapLocation l40 = new MapLocation(currX +1, currY -1);  // (+1, -1) relative to center
        if(rc.canBuildRobot(type, l40)){
            rc.buildRobot(type, l40);
            return true;
        }
        MapLocation l53 = new MapLocation(currX +0, currY -3);  // (+0, -3) relative to center
        if(rc.canBuildRobot(type, l53)){
            rc.buildRobot(type, l53);
            return true;
        }
        MapLocation l35 = new MapLocation(currX +3, currY +0);  // (+3, +0) relative to center
        if(rc.canBuildRobot(type, l35)){
            rc.buildRobot(type, l35);
            return true;
        }
        MapLocation l46 = new MapLocation(currX +0, currY -2);  // (+0, -2) relative to center
        if(rc.canBuildRobot(type, l46)){
            rc.buildRobot(type, l46);
            return true;
        }
        MapLocation l34 = new MapLocation(currX +2, currY +0);  // (+2, +0) relative to center
        if(rc.canBuildRobot(type, l34)){
            rc.buildRobot(type, l34);
            return true;
        }
        MapLocation l39 = new MapLocation(currX +0, currY -1);  // (+0, -1) relative to center
        if(rc.canBuildRobot(type, l39)){
            rc.buildRobot(type, l39);
            return true;
        }
        MapLocation l33 = new MapLocation(currX +1, currY +0);  // (+1, +0) relative to center
        if(rc.canBuildRobot(type, l33)){
            rc.buildRobot(type, l33);
            return true;
        }
        MapLocation l45 = new MapLocation(currX -1, currY -2);  // (-1, -2) relative to center
        if(rc.canBuildRobot(type, l45)){
            rc.buildRobot(type, l45);
            return true;
        }
        MapLocation l27 = new MapLocation(currX +2, currY +1);  // (+2, +1) relative to center
        if(rc.canBuildRobot(type, l27)){
            rc.buildRobot(type, l27);
            return true;
        }
        MapLocation l32 = new MapLocation(currX +0, currY +0);  // (+0, +0) relative to center
        if(rc.canBuildRobot(type, l32)){
            rc.buildRobot(type, l32);
            return true;
        }
        MapLocation l38 = new MapLocation(currX -1, currY -1);  // (-1, -1) relative to center
        if(rc.canBuildRobot(type, l38)){
            rc.buildRobot(type, l38);
            return true;
        }
        MapLocation l26 = new MapLocation(currX +1, currY +1);  // (+1, +1) relative to center
        if(rc.canBuildRobot(type, l26)){
            rc.buildRobot(type, l26);
            return true;
        }
        MapLocation l31 = new MapLocation(currX -1, currY +0);  // (-1, +0) relative to center
        if(rc.canBuildRobot(type, l31)){
            rc.buildRobot(type, l31);
            return true;
        }
        MapLocation l25 = new MapLocation(currX +0, currY +1);  // (+0, +1) relative to center
        if(rc.canBuildRobot(type, l25)){
            rc.buildRobot(type, l25);
            return true;
        }
        MapLocation l44 = new MapLocation(currX -2, currY -2);  // (-2, -2) relative to center
        if(rc.canBuildRobot(type, l44)){
            rc.buildRobot(type, l44);
            return true;
        }
        MapLocation l20 = new MapLocation(currX +2, currY +2);  // (+2, +2) relative to center
        if(rc.canBuildRobot(type, l20)){
            rc.buildRobot(type, l20);
            return true;
        }
        MapLocation l37 = new MapLocation(currX -2, currY -1);  // (-2, -1) relative to center
        if(rc.canBuildRobot(type, l37)){
            rc.buildRobot(type, l37);
            return true;
        }
        MapLocation l19 = new MapLocation(currX +1, currY +2);  // (+1, +2) relative to center
        if(rc.canBuildRobot(type, l19)){
            rc.buildRobot(type, l19);
            return true;
        }
        MapLocation l24 = new MapLocation(currX -1, currY +1);  // (-1, +1) relative to center
        if(rc.canBuildRobot(type, l24)){
            rc.buildRobot(type, l24);
            return true;
        }
        MapLocation l30 = new MapLocation(currX -2, currY +0);  // (-2, +0) relative to center
        if(rc.canBuildRobot(type, l30)){
            rc.buildRobot(type, l30);
            return true;
        }
        MapLocation l18 = new MapLocation(currX +0, currY +2);  // (+0, +2) relative to center
        if(rc.canBuildRobot(type, l18)){
            rc.buildRobot(type, l18);
            return true;
        }
        MapLocation l23 = new MapLocation(currX -2, currY +1);  // (-2, +1) relative to center
        if(rc.canBuildRobot(type, l23)){
            rc.buildRobot(type, l23);
            return true;
        }
        MapLocation l17 = new MapLocation(currX -1, currY +2);  // (-1, +2) relative to center
        if(rc.canBuildRobot(type, l17)){
            rc.buildRobot(type, l17);
            return true;
        }
        MapLocation l29 = new MapLocation(currX -3, currY +0);  // (-3, +0) relative to center
        if(rc.canBuildRobot(type, l29)){
            rc.buildRobot(type, l29);
            return true;
        }
        MapLocation l11 = new MapLocation(currX +0, currY +3);  // (+0, +3) relative to center
        if(rc.canBuildRobot(type, l11)){
            rc.buildRobot(type, l11);
            return true;
        }
        MapLocation l16 = new MapLocation(currX -2, currY +2);  // (-2, +2) relative to center
        if(rc.canBuildRobot(type, l16)){
            rc.buildRobot(type, l16);
            return true;
        }
        return false;
    }

}

