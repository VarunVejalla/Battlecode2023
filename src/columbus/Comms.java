package columbus;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.MapLocation;

import javax.swing.plaf.synth.Region;

// some parts inspired by: https://github.com/mvpatel2000/Battlecode2022/blob/main/src/athena/CommsHandler.java

class RegionData {
    boolean adamantiumWell;
    boolean manaWell;
    boolean elixirWell;

    // This is 0 if there's no islands in the region, 1 if there's an uncontrolled island, 2 if there's a friendly island, 3 if there's an enemy island.
    int islandStatus;

    public RegionData(boolean adamantiumWell, boolean manaWell, boolean elixirWell, int islandStatus){
        this.adamantiumWell = adamantiumWell;
        this.manaWell = manaWell;
        this.elixirWell = elixirWell;
        this.islandStatus = islandStatus;
    }
}

public class Comms {

    final int FULL_MASK = 65535; // 11111111111111

    final int HQ0_LOC_IDX = 0;
    final int HQ1_LOC_IDX = 1;
    final int HQ2_LOC_IDX = 2;
    final int HQ3_LOC_IDX = 3;
    final int[] HQ_LOC_IDX_MAP = {HQ0_LOC_IDX, HQ1_LOC_IDX, HQ2_LOC_IDX, HQ3_LOC_IDX};

    final int HQ_X_MASK = 4032;
    final int HQ_X_SHIFT = 6;
    final int HQ_Y_MASK = 63;
    final int HQ_Y_SHIFT = 0;

    final int HQ0_RESOURCES_IDX = 4;
    final int HQ1_RESOURCES_IDX = 5;
    final int HQ2_RESOURCES_IDX = 6;
    final int HQ3_RESOURCES_IDX = 7;
    final int[] HQ_RESOURCES_IDX_MAP = {HQ0_RESOURCES_IDX, HQ1_RESOURCES_IDX, HQ2_RESOURCES_IDX, HQ3_RESOURCES_IDX};

    final int HQ_MANA_MASK = 127; // 0000000001111111 (digits 0-6)
    final int HQ_MANA_SHIFT = 0;
    final int HQ_ADAMANTIUM_MASK = 16256; // 0011111110000000 (digits 7-13)
    final int HQ_ADAMANTIUM_SHIFT = 7;

    // TODO: We should really make a constants file

    final int NUM_REGIONS_HORIZONTAL = 7;
    final int NUM_REGIONS_VERTICAL = 7;
    final int NUM_REGIONS_TOTAL = NUM_REGIONS_HORIZONTAL * NUM_REGIONS_VERTICAL;

    final int REGION_START_IDX = 8;
    final int REGIONS_PER_COMM = 3;
    final int REGION_FIRST_MASK = 31;
    final int REGION_FIRST_SHIFT = 0;
    final int REGION_SECOND_MASK = 992;
    final int REGION_SECOND_SHIFT = 5;
    final int REGION_THIRD_MASK = 31744;
    final int REGION_THIRD_SHIFT = 10;

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
        return extractVal(HQ_LOC_IDX_MAP[idx], HQ_X_MASK, HQ_X_SHIFT);
    }

    public int readOurHQYCoord(int idx) throws GameActionException {
        // note that we subtract 1 from the y coordinate after reading the y location
        // this is done so that all zeros in an index in the shared array means it hasn't been claimed yet
        return extractVal(HQ_LOC_IDX_MAP[idx], HQ_Y_MASK, HQ_Y_SHIFT) - 1;
    }

    public void writeOurHQXCoord(int idx, int value) throws GameActionException {
        insertVal(HQ_LOC_IDX_MAP[idx], HQ_X_MASK, HQ_X_SHIFT, value);
    }

    public void writeOurHQYCoord(int idx, int value) throws GameActionException {
        // note that we add 1 to the y coordinate before writing to the shared array
        // this is done so that all zeros in an index in the shared array means it hasn't been claimed yet

        value += 1;
        insertVal(HQ_LOC_IDX_MAP[idx], HQ_Y_MASK, HQ_Y_SHIFT, value);
    }

    public int readMana(int idx) throws GameActionException {
        return extractVal(HQ_RESOURCES_IDX_MAP[idx], HQ_MANA_MASK, HQ_MANA_SHIFT) * 100;
    }

    public int readAdamantium(int idx) throws GameActionException {
        return extractVal(HQ_RESOURCES_IDX_MAP[idx], HQ_ADAMANTIUM_MASK, HQ_ADAMANTIUM_SHIFT) * 100;
    }

    public void writeMana(int idx, int value) throws GameActionException {
        value /= 100;
        if(value > 127) value = 127;

        insertVal(HQ_RESOURCES_IDX_MAP[idx], HQ_MANA_MASK, HQ_MANA_SHIFT, value);
    }

    public void writeAdamantium(int idx, int value) throws GameActionException {
        value /= 100;
        if(value > 127) value = 127;

        insertVal(HQ_RESOURCES_IDX_MAP[idx], HQ_ADAMANTIUM_MASK, HQ_ADAMANTIUM_SHIFT, value);
    }

    public int getRegionX(MapLocation loc){
        int regionWidth = rc.getMapWidth() / NUM_REGIONS_HORIZONTAL;
        int numUppers = rc.getMapWidth() % NUM_REGIONS_HORIZONTAL;
        int maxX = 0;
        for(int i = 0; i < NUM_REGIONS_HORIZONTAL; i++){
            if(i < numUppers){
                maxX += regionWidth + 1;
            }
            else{
                maxX += regionWidth;
            }
            if(loc.x <= maxX){
                return i;
            }
        }
        return NUM_REGIONS_HORIZONTAL - 1;
    }

    public int getRegionY(MapLocation loc){
        int regionHeight = rc.getMapHeight() / NUM_REGIONS_VERTICAL;
        int numUppers = rc.getMapHeight() % NUM_REGIONS_VERTICAL;
        int maxY = 0;
        for(int i = 0; i < NUM_REGIONS_VERTICAL; i++){
            maxY += regionHeight;
            if(i < numUppers){
                maxY += 1;
            }
            if(loc.y <= maxY){
                return i;
            }
        }
        return NUM_REGIONS_VERTICAL - 1;
    }

    // TODO: Make this not take 1 bajillion bytecode
    public int getRegionNum(MapLocation loc){
        return getRegionY(loc) * NUM_REGIONS_HORIZONTAL + getRegionX(loc);
    }

    public int regionNumToRegionX(int regionNum){
        return regionNum % NUM_REGIONS_HORIZONTAL;
    }

    public int regionNumToRegionY(int regionNum){
        return regionNum / NUM_REGIONS_HORIZONTAL;
    }


    public MapLocation getRegionCenter(int regionNum){
        int xIdx = regionNum % NUM_REGIONS_HORIZONTAL;
        int yIdx = regionNum / NUM_REGIONS_HORIZONTAL;

        int regionWidth = rc.getMapWidth() / NUM_REGIONS_HORIZONTAL;
        int numUppers = rc.getMapWidth() % NUM_REGIONS_HORIZONTAL;
        int xCenter = regionWidth * xIdx - regionWidth / 2;
        if(xIdx < numUppers){
            xCenter += xIdx;
        }
        else{
            xCenter += numUppers;
        }

        int regionHeight = rc.getMapHeight() / NUM_REGIONS_VERTICAL;
        numUppers = rc.getMapHeight() % NUM_REGIONS_VERTICAL;
        int yCenter = regionHeight * yIdx - regionHeight / 2;
        if(yIdx < numUppers){
            yCenter += yIdx;
        }
        else{
            yCenter += numUppers;
        }

        return new MapLocation(xCenter, yCenter);
    }

    public RegionData getRegionData(int regionNum) throws GameActionException {
        int commsIdx = regionNum / REGIONS_PER_COMM + REGION_START_IDX;
        int mask = 0;
        int shift = 0;
        switch(regionNum % REGIONS_PER_COMM){
            case 0:
                mask = REGION_FIRST_MASK;
                shift = REGION_FIRST_SHIFT;
                break;
            case 1:
                mask = REGION_SECOND_MASK;
                shift = REGION_SECOND_SHIFT;
                break;
            case 2:
                mask = REGION_THIRD_MASK;
                shift = REGION_THIRD_SHIFT;
                break;
        }
        int regionDataInt = extractVal(commsIdx, mask, shift);
        return intToRegionData(regionDataInt);
    }

    public void saveRegionData(int regionNum, RegionData data) throws GameActionException {
        int regionDataInt = regionDataToInt(data);
        int commsIdx = regionNum / REGIONS_PER_COMM + REGION_START_IDX;
        int mask = 0;
        int shift = 0;
        switch(regionNum % REGIONS_PER_COMM){
            case 0:
                mask = REGION_FIRST_MASK;
                shift = REGION_FIRST_SHIFT;
                break;
            case 1:
                mask = REGION_SECOND_MASK;
                shift = REGION_SECOND_SHIFT;
                break;
            case 2:
                mask = REGION_THIRD_MASK;
                shift = REGION_THIRD_SHIFT;
                break;
        }
        insertVal(commsIdx, mask, shift, regionDataInt);
    }

    private RegionData intToRegionData(int regionDataInt) throws GameActionException {
        boolean adamantiumWell = (regionDataInt & 16) != 0;
        boolean manaWell = (regionDataInt & 8) != 0;
        boolean elixirWell = (regionDataInt & 4) != 0;
        return new RegionData(adamantiumWell, manaWell, elixirWell, regionDataInt & 3);
    }

    private int regionDataToInt(RegionData data) throws GameActionException {
        int value = 0;
        if(data.adamantiumWell){
            value |= 16;
        }
        if(data.manaWell){
            value |= 8;
        }
        if(data.elixirWell){
            value |= 4;
        }
        value |= data.islandStatus;
        return value;
    }


}
