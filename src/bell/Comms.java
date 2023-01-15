package bell;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

// some parts inspired by: https://github.com/mvpatel2000/Battlecode2022/blob/main/src/athena/CommsHandler.java

class RegionData {
    boolean adamantiumWell;
    boolean manaWell;
    boolean elixirWell;

    public RegionData(boolean adamantiumWell, boolean manaWell, boolean elixirWell){
        this.adamantiumWell = adamantiumWell;
        this.manaWell = manaWell;
        this.elixirWell = elixirWell;
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

    // Region consts

    final int NUM_REGIONS_HORIZONTAL = 7;
    final int NUM_REGIONS_VERTICAL = 7;
    final int NUM_REGIONS_TOTAL = NUM_REGIONS_HORIZONTAL * NUM_REGIONS_VERTICAL;

    final int REGION_START_IDX = 43;
    final int REGIONS_PER_COMM = 5;
    final int REGION_MASK_SIZE = 3;

    // Island consts
    final int ISLAND_START_IDX = 8;
    final int ISLAND_X_MASK = 4032;
    final int ISLAND_X_SHIFT = 6;
    final int ISLAND_Y_MASK = 63;
    final int ISLAND_Y_SHIFT = 0;
    final int ISLAND_CONTROL_MASK = 12288;
    final int ISLAND_CONTROL_SHIFT = 12;

    RobotController rc;
    Robot robot;

    public Comms(RobotController rc, Robot robot){
        this.rc = rc;
        this.robot = robot;
    }

    /// General Methods

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

    /// HQ Stuff

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

    /// Island stuff

    // Note: Island idxs go from 1 - # of islands

    public void writeIslandControl(int islandIdx, Team controllingTeam) throws GameActionException {
        int controlValue = 1;
        if(controllingTeam == robot.myTeam){
            controlValue = 2;
        }
        else if(controllingTeam == robot.opponent){
            controlValue = 3;
        }

        insertVal(ISLAND_START_IDX + islandIdx - 1, ISLAND_CONTROL_MASK, ISLAND_CONTROL_SHIFT, controlValue);
    }


    public Team getIslandControl(int islandIdx) throws GameActionException {
        int controlVal = extractVal(ISLAND_START_IDX + islandIdx - 1, ISLAND_CONTROL_MASK, ISLAND_CONTROL_SHIFT);
        if(controlVal == 1){
            return Team.NEUTRAL;
        }
        else if(controlVal == 2){
            return robot.myTeam;
        }
        else if(controlVal == 3){
            return robot.opponent;
        }
        return Team.NEUTRAL;
    }

    public void writeIslandLocation(int islandIdx, MapLocation loc) throws GameActionException {
        int xVal = loc.x;
        int yVal = loc.y + 1;
        insertVal(ISLAND_START_IDX + islandIdx - 1,ISLAND_X_MASK, ISLAND_X_SHIFT, xVal);
        insertVal(ISLAND_START_IDX + islandIdx - 1, ISLAND_Y_MASK, ISLAND_Y_SHIFT, yVal);
    }

    public MapLocation getIslandLocation(int islandIdx) throws GameActionException {
        if(rc.readSharedArray(ISLAND_START_IDX + islandIdx - 1) == 0){
            return null;
        }
        int xVal = extractVal(ISLAND_START_IDX + islandIdx - 1, ISLAND_X_MASK, ISLAND_X_SHIFT);
        int yVal = extractVal(ISLAND_START_IDX + islandIdx - 1, ISLAND_Y_MASK, ISLAND_Y_SHIFT) - 1;
        return new MapLocation(xVal, yVal);
    }

    /// Region stuff

    public int getRegionX(MapLocation loc){
        int regionWidth = rc.getMapWidth() / NUM_REGIONS_HORIZONTAL;
        int numUppers = rc.getMapWidth() % NUM_REGIONS_HORIZONTAL;

        // the first numUppers should have regionWidth+1 squares
        // the rest should have regionWith squares
        if(loc.x+1 <= numUppers * (regionWidth+1)) {
            return (int) Math.ceil((loc.x+1)/(regionWidth+1) - 1);
        }
        else {
            return (int) Math.ceil((loc.x+1-numUppers)/regionWidth - 1);
        }
    }

    public int getRegionY(MapLocation loc){
        int regionHeight = rc.getMapHeight() / NUM_REGIONS_VERTICAL;
        int numUppers = rc.getMapHeight() % NUM_REGIONS_VERTICAL;
        if(loc.y+1 <= numUppers * (regionHeight+1)) {
            return (int) Math.ceil((loc.y+1)/(regionHeight+1) - 1);
        }
        else {
            return (int) Math.ceil((loc.y+1-numUppers)/regionHeight - 1);
        }
    }
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
        int xCenter;
        if(xIdx < numUppers) {
            xCenter = (xIdx)*(regionWidth+1) + (regionWidth+1)/2;
        }
        else {
            xCenter = numUppers + (regionWidth * (2*xIdx+1))/2;
        }

        int regionHeight = rc.getMapHeight() / NUM_REGIONS_VERTICAL;
        numUppers = rc.getMapHeight() % NUM_REGIONS_VERTICAL;
        int yCenter;
        if(yIdx < numUppers) {
            yCenter = (yIdx)*(regionWidth+1) + (regionWidth+1)/2;
        }
        else {
            yCenter = numUppers + (regionHeight * (2*yIdx+1))/2;
        }

        return new MapLocation(xCenter, yCenter);
    }

    public RegionData getRegionData(int regionNum) throws GameActionException {
        int commsIdx = regionNum / REGIONS_PER_COMM + REGION_START_IDX;
        int regionIdxWithinComm = regionNum % REGIONS_PER_COMM;
        int mask = (1<<REGION_MASK_SIZE) - 1;
        int shift = regionIdxWithinComm * REGION_MASK_SIZE;
        mask = mask << shift;
        int regionDataInt = extractVal(commsIdx, mask, shift);
        return intToRegionData(regionDataInt);
    }

    public void saveRegionData(int regionNum, RegionData data) throws GameActionException {
        int regionDataInt = regionDataToInt(data);
        int commsIdx = regionNum / REGIONS_PER_COMM + REGION_START_IDX;
        int regionIdxWithinComm = regionNum % REGIONS_PER_COMM;
        int mask = (1<<REGION_MASK_SIZE) - 1;
        int shift = regionIdxWithinComm * REGION_MASK_SIZE;
        mask = mask << shift;
        insertVal(commsIdx, mask, shift, regionDataInt);
    }

    private RegionData intToRegionData(int regionDataInt) {
        boolean adamantiumWell = (regionDataInt & 4) != 0;
        boolean manaWell = (regionDataInt & 2) != 0;
        boolean elixirWell = (regionDataInt & 1) != 0;
        return new RegionData(adamantiumWell, manaWell, elixirWell);
    }

    private int regionDataToInt(RegionData data) {
        int value = 0;
        if(data.adamantiumWell){
            value |= 4;
        }
        if(data.manaWell){
            value |= 2;
        }
        if(data.elixirWell){
            value |= 1;
        }
        return value;
    }


}
