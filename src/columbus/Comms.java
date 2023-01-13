package columbus;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.MapLocation;

// some parts inspired by: https://github.com/mvpatel2000/Battlecode2022/blob/main/src/athena/CommsHandler.java

public class Comms {

    final int HQ0_LOC_IDX = 0;
    final int HQ1_LOC_IDX = 1;
    final int HQ2_LOC_IDX = 2;
    final int HQ3_LOC_IDX = 3;

    final int HQ0_RESOURCES_IDX = 4;
    final int HQ1_RESOURCES_IDX = 5;
    final int HQ2_RESOURCES_IDX = 6;
    final int HQ3_RESOURCES_IDX = 7;

    final int FULL_MASK = 65535; // 11111111111111

    final int HQ_X_MASK = 4032;
    final int HQ_X_SHIFT = 6;
    final int HQ_Y_MASK = 63;
    final int HQ_Y_SHIFT = 0;

    final int HQ_MANA_MASK = 127; // 0000000001111111 (digits 0-6)
    final int HQ_MANA_SHIFT = 0;
    final int HQ_ADAMANTIUM_MASK = 16256; // 0011111110000000 (digits 7-13)
    final int HQ_ADAMANTIUM_SHIFT = 7;

    RobotController rc;
    Robot robot;

    public Comms(RobotController rc, Robot robot){
        this.rc = rc;
        this.robot = robot;
    }

    private int extractVal(int commsIdx, int mask, int shift) throws GameActionException {
        return (rc.readSharedArray(commsIdx) & mask) >> shift;
    }

    private void insertVal(int commsIdx, int mask, int shift, int value) throws GameActionException {
        // Clear out the existing value in that position
        int clearMask = FULL_MASK - mask;
        int newCommsVal = rc.readSharedArray(commsIdx) & clearMask;

        // Insert the new value
        newCommsVal = newCommsVal | (value << shift);
        rc.writeSharedArray(commsIdx, newCommsVal);
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
                return extractVal(HQ0_LOC_IDX, HQ_X_MASK, HQ_X_SHIFT);
            case 1:
                return extractVal(HQ1_LOC_IDX, HQ_X_MASK, HQ_X_SHIFT);
            case 2:
                return extractVal(HQ2_LOC_IDX, HQ_X_MASK, HQ_X_SHIFT);
            case 3:
                return extractVal(HQ3_LOC_IDX, HQ_X_MASK, HQ_X_SHIFT);
            default:
                return -1;
        }
    }

    public int readOurHQYCoord(int idx) throws GameActionException {
        // note that we subtract 1 from the y coordinate after reading the y location
        // this is done so that all zeros in an index in the shared array means it hasn't been claimed yet
        // ask saahith if this doesn't make sense

        switch (idx) {
            case 0:
                return extractVal(HQ0_LOC_IDX, HQ_Y_MASK, HQ_Y_SHIFT) - 1;
            case 1:
                return extractVal(HQ1_LOC_IDX, HQ_Y_MASK, HQ_Y_SHIFT) - 1;
            case 2:
                return extractVal(HQ2_LOC_IDX, HQ_Y_MASK, HQ_Y_SHIFT) - 1;
            case 3:
                return extractVal(HQ3_LOC_IDX, HQ_Y_MASK, HQ_Y_SHIFT) - 1;
            default:
                return -1;
        }
    }


    public void writeOurHQXCoord(int idx, int value) throws GameActionException {
        switch (idx) {
            case 0:
                insertVal(HQ0_LOC_IDX, HQ_X_MASK, HQ_X_SHIFT, value);
                break;
            case 1:
                insertVal(HQ1_LOC_IDX, HQ_X_MASK, HQ_X_SHIFT, value);
                break;
            case 2:
                insertVal(HQ2_LOC_IDX, HQ_X_MASK, HQ_X_SHIFT, value);
                break;
            case 3:
                insertVal(HQ3_LOC_IDX, HQ_X_MASK, HQ_X_SHIFT, value);
                break;
        }
    }


    public void writeOurHQYCoord(int idx, int value) throws GameActionException {
        // note that we add 1 to the y coordinate before writing to the shared array
        // this is done so that all zeros in an index in the shared array means it hasn't been claimed yet
        // ask saahith if this doesn't make sense
        value += 1;
        switch (idx) {
            case 0:
                insertVal(HQ0_LOC_IDX, HQ_Y_MASK, HQ_Y_SHIFT, value);
                break;
            case 1:
                insertVal(HQ1_LOC_IDX, HQ_Y_MASK, HQ_Y_SHIFT, value);
                break;
            case 2:
                insertVal(HQ2_LOC_IDX, HQ_Y_MASK, HQ_Y_SHIFT, value);
                break;
            case 3:
                insertVal(HQ3_LOC_IDX, HQ_Y_MASK, HQ_Y_SHIFT, value);
                break;
        }
    }

    public int readMana(int idx) throws GameActionException {
        switch (idx) {
            case 0:
                return extractVal(HQ0_RESOURCES_IDX, HQ_MANA_MASK, HQ_MANA_SHIFT) * 100;
            case 1:
                return extractVal(HQ1_RESOURCES_IDX, HQ_MANA_MASK, HQ_MANA_SHIFT) * 100;
            case 2:
                return extractVal(HQ2_RESOURCES_IDX, HQ_MANA_MASK, HQ_MANA_SHIFT) * 100;
            case 3:
                return extractVal(HQ3_RESOURCES_IDX, HQ_MANA_MASK, HQ_MANA_SHIFT) * 100;
            default:
                return -1;
        }
    }


    public int readAdamantium(int idx) throws GameActionException {
        switch (idx) {
            case 0:
                return extractVal(HQ0_RESOURCES_IDX, HQ_ADAMANTIUM_MASK, HQ_ADAMANTIUM_SHIFT) * 100;
            case 1:
                return extractVal(HQ1_RESOURCES_IDX, HQ_ADAMANTIUM_MASK, HQ_ADAMANTIUM_SHIFT) * 100;
            case 2:
                return extractVal(HQ2_RESOURCES_IDX, HQ_ADAMANTIUM_MASK, HQ_ADAMANTIUM_SHIFT) * 100;
            case 3:
                return extractVal(HQ3_RESOURCES_IDX, HQ_ADAMANTIUM_MASK, HQ_ADAMANTIUM_SHIFT) * 100;
            default:
                return -1;
        }
    }

    public void writeMana(int idx, int value) throws GameActionException {
        value /= 100;
        if(value > 127) value = 127;

        switch (idx) {
            case 0:
                insertVal(HQ0_RESOURCES_IDX, HQ_MANA_MASK, HQ_MANA_SHIFT, value);
                break;
            case 1:
                insertVal(HQ1_RESOURCES_IDX, HQ_MANA_MASK, HQ_MANA_SHIFT, value);
                break;
            case 2:
                insertVal(HQ2_RESOURCES_IDX, HQ_MANA_MASK, HQ_MANA_SHIFT, value);
                break;
            case 3:
                insertVal(HQ3_RESOURCES_IDX, HQ_MANA_MASK, HQ_MANA_SHIFT, value);
                break;
        }
    }

    public void writeAdamantium(int idx, int value) throws GameActionException {
        value /= 100;
        if(value > 127) value = 127;

        switch (idx) {
            case 0:
                insertVal(HQ0_RESOURCES_IDX, HQ_ADAMANTIUM_MASK, HQ_ADAMANTIUM_SHIFT, value);
                break;
            case 1:
                insertVal(HQ1_RESOURCES_IDX, HQ_ADAMANTIUM_MASK, HQ_ADAMANTIUM_SHIFT, value);
                break;
            case 2:
                insertVal(HQ2_RESOURCES_IDX, HQ_ADAMANTIUM_MASK, HQ_ADAMANTIUM_SHIFT, value);
                break;
            case 3:
                insertVal(HQ3_RESOURCES_IDX, HQ_ADAMANTIUM_MASK, HQ_ADAMANTIUM_SHIFT, value);
                break;
        }
    }

}
