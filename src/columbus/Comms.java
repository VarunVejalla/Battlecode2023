package columbus;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.MapLocation;

// some parts inspired by: https://github.com/mvpatel2000/Battlecode2022/blob/main/src/athena/CommsHandler.java

public class Comms {

    RobotController rc;
    Robot robot;


    public Comms(RobotController rc, Robot robot){
        this.rc = rc;
        this.robot = robot;
    }


    public void writeOurHQLocation(int idx, MapLocation loc) throws GameActionException{
        writeOurHQXCoord(idx, loc.x);
        writeOurHQYCoord(idx, loc.y);
    }

    public MapLocation readOurHQLocation(int idx) throws GameActionException{
        return new MapLocation(readOurHQXCoord(idx), readOurHQYCoord(idx));
    }


    public int readOurHQXCoord(int idx) throws GameActionException {
        switch (idx) {
            case 0:
                return (rc.readSharedArray(0) & 4032) >>> 6;
            case 1:
                return (rc.readSharedArray(1) & 4032) >>> 6;
            case 2:
                return (rc.readSharedArray(2) & 4032) >>> 6;
            case 3:
                return (rc.readSharedArray(3) & 4032) >>> 6;
            default:
                return -1;
        }
    }

    public int readOurHQYCoord(int idx) throws GameActionException {
        // note that we subtract 1 from the y coordinate after reading the y location
        // this is done so that all zeros in an index in the shared array means it hasn't been claimed yet
        // ask saahith if this doesn't make sense (sry for bad explanation)

        switch (idx) {
            case 0:
                return (rc.readSharedArray(0) & 63) - 1;
            case 1:
                return (rc.readSharedArray(1) & 63) - 1;
            case 2:
                return (rc.readSharedArray(2) & 63) - 1;
            case 3:
                return (rc.readSharedArray(3) & 63) - 1;
            default:
                return -1;
        }
    }


    public void writeOurHQXCoord(int idx, int value) throws GameActionException {
        switch (idx) {
            case 0:
                rc.writeSharedArray(0, (rc.readSharedArray(0) & 61503) | (value << 6));
                break;
            case 1:
                rc.writeSharedArray(1, (rc.readSharedArray(1) & 61503) | (value << 6));
                break;
            case 2:
                rc.writeSharedArray(2, (rc.readSharedArray(2) & 61503) | (value << 6));
                break;
            case 3:
                rc.writeSharedArray(3, (rc.readSharedArray(3) & 61503) | (value << 6));
                break;
        }
    }


    public void writeOurHQYCoord(int idx, int value) throws GameActionException {
        // note that we add 1 to the y coordinate before writing to the shared array
        // this is done so that all zeros in an index in the shared array means it hasn't been claimed yet
        // ask saahith if this doesn't make sense (sry for bad explanation)
        value += 1;
        switch (idx) {
            case 0:
                rc.writeSharedArray(0, (rc.readSharedArray(0) & 65472) | (value));
                break;
            case 1:
                rc.writeSharedArray(1, (rc.readSharedArray(1) & 65472) | (value));
                break;
            case 2:
                rc.writeSharedArray(2, (rc.readSharedArray(2) & 65472) | (value));
                break;
            case 3:
                rc.writeSharedArray(3, (rc.readSharedArray(3) & 65472) | (value));
                break;
        }
    }


    public int readMana(int idx) throws GameActionException {
        switch (idx) {
            case 0:
                return (rc.readSharedArray(4) & 127) * 100;
            case 1:
                return (rc.readSharedArray(6) & 127) * 100;
            case 2:
                return (rc.readSharedArray(8) & 127) * 100;
            case 3:
                return (rc.readSharedArray(10) & 127) * 100;
            default:
                return -1;
        }
    }


    public int readAdamantium(int idx) throws GameActionException {
        switch (idx) {
            case 0:
                return ((rc.readSharedArray(4) & 16383) >> 7) * 100;
            case 1:
                return ((rc.readSharedArray(6) & 16383) >> 7) * 100;
            case 2:
                return ((rc.readSharedArray(8) & 16383) >> 7) * 100;
            case 3:
                return ((rc.readSharedArray(10) & 16383) >> 7) * 100;
            default:
                return -1;
        }
    }

    public int readElixir(int idx) throws GameActionException {
        switch (idx) {
            case 0:
                return (rc.readSharedArray(5) & 127) * 100;
            case 1:
                return (rc.readSharedArray(7) & 127) * 100;
            case 2:
                return (rc.readSharedArray(9) & 127) * 100;
            case 3:
                return (rc.readSharedArray(11) & 127) * 100;
            default:
                return -1;
        }
    }

    public void writeMana(int idx, int value) throws GameActionException {
        value /= 100;
        if(value > 127) value = 127;

        switch (idx) {
            case 0:
                rc.writeSharedArray(4, (rc.readSharedArray(4) & 65472) | (value));
                break;
            case 1:
                rc.writeSharedArray(6, (rc.readSharedArray(6) & 65472) | (value));
                break;
            case 2:
                rc.writeSharedArray(8, (rc.readSharedArray(8) & 65472) | (value));
                break;
            case 3:
                rc.writeSharedArray(10, (rc.readSharedArray(10) & 65472) | (value));
                break;
        }
    }

    public void writeAdamantium(int idx, int value) throws GameActionException {
        value /= 100;
        if(value > 127) value = 127;
        switch (idx) {
            case 0:
                rc.writeSharedArray(4, (rc.readSharedArray(4) & 49279) | (value) << 7);
                break;
            case 1:
                rc.writeSharedArray(6, (rc.readSharedArray(6) & 49279) | (value) << 7);
                break;
            case 2:
                rc.writeSharedArray(8, (rc.readSharedArray(8) & 49279) | (value) << 7);
                break;
            case 3:
                rc.writeSharedArray(10, (rc.readSharedArray(10) & 49279) | (value) << 7);
                break;
        }
    }

    public void writeElixir(int idx, int value) throws GameActionException {
        value /= 100;
        if(value > 127) value = 127;

        switch (idx) {
            case 0:
                rc.writeSharedArray(5, (rc.readSharedArray(4) & 65472) | (value));
                break;
            case 1:
                rc.writeSharedArray(7, (rc.readSharedArray(6) & 65472) | (value));
                break;
            case 2:
                rc.writeSharedArray(9, (rc.readSharedArray(8) & 65472) | (value));
                break;
            case 3:
                rc.writeSharedArray(11, (rc.readSharedArray(10) & 65472) | (value));
                break;
        }
    }







}
