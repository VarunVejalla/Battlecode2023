package liskov;

import battlecode.common.*;

import static liskov.Constants.*;

// some parts inspired by: https://github.com/mvpatel2000/Battlecode2022/blob/main/src/athena/CommsHandler.java

public class Comms {

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


    public void writeRatio(int idx, int adamantium, int mana, int elixir) throws GameActionException {
        // scale all numbers to <= 15 so we can write to the shared array

        double totalSum = adamantium + mana + elixir;
        adamantium = (int)(adamantium / totalSum * 15.0);
        mana = (int)(mana / totalSum * 15.0);
        elixir = (int)(elixir / totalSum * 15.0);

        int adamantiumWellIndex = getClosestWellCommsIndex(idx, ResourceType.ADAMANTIUM);
        int manaWellIndex = getClosestWellCommsIndex(idx, ResourceType.MANA);
        int elixirWellIndex = getClosestWellCommsIndex(idx, ResourceType.ELIXIR);

        insertVal(adamantiumWellIndex, RESOURCE_RATIO_MASK, RESOURCE_RATIO_SHIFT, adamantium);
        insertVal(manaWellIndex, RESOURCE_RATIO_MASK, RESOURCE_RATIO_SHIFT, mana);
        insertVal(elixirWellIndex, RESOURCE_RATIO_MASK, RESOURCE_RATIO_SHIFT, elixir);
    }


    public int[] readRatio(int idx) throws GameActionException {
        int adamantiumWellIndex = getClosestWellCommsIndex(idx, ResourceType.ADAMANTIUM);
        int manaWellIndex = getClosestWellCommsIndex(idx, ResourceType.MANA);
        int elixirWellIndex = getClosestWellCommsIndex(idx, ResourceType.ELIXIR);

        int adamantium = extractVal(adamantiumWellIndex, RESOURCE_RATIO_MASK, RESOURCE_RATIO_SHIFT);
        int mana = extractVal(manaWellIndex, RESOURCE_RATIO_MASK, RESOURCE_RATIO_SHIFT);
        int elixir = extractVal(elixirWellIndex, RESOURCE_RATIO_MASK, RESOURCE_RATIO_SHIFT);


        int[] ratioData = new int[3];
        ratioData[ADAMANTIUM_RATIO_INDEX] = adamantium;
        ratioData[MANA_RATIO_INDEX] = mana;
        ratioData[ELIXIR_RATIO_INDEX] = elixir;

        return ratioData;
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

    /// Wells stuff

    public int getClosestWellCommsIndex(int HQIndex, ResourceType resource){
        switch(resource){
            case ADAMANTIUM:
                return WELLS_START_IDX + HQIndex * NUM_WELLS_PER_HQ + ADAMANTIUM_WELL_OFFSET;
            case MANA:
                return WELLS_START_IDX + HQIndex * NUM_WELLS_PER_HQ + MANA_WELL_OFFSET;
            case ELIXIR:
                return WELLS_START_IDX + HQIndex * NUM_WELLS_PER_HQ + ELIXIR_WELL_OFFSET;
        }
        throw new RuntimeException("INVALID RESOURCE SPECIFIED FOR getClosestWellCommsIndex! " + resource);
    }

    public void setClosestWell(int HQIndex, ResourceType resource, MapLocation wellLoc) throws GameActionException{
        int commsIdx = getClosestWellCommsIndex(HQIndex, resource);
        int xVal = wellLoc.x;
        int yVal = wellLoc.y + 1;
        insertVal(commsIdx, WELLS_X_MASK, WELLS_X_SHIFT, xVal);
        insertVal(commsIdx, WELLS_Y_MASK, WELLS_Y_SHIFT, yVal);
    }

    public MapLocation getClosestWell(int HQIndex, ResourceType resource) throws GameActionException {
        int commsIdx = getClosestWellCommsIndex(HQIndex, resource);
        if(rc.readSharedArray(commsIdx) == 0){
            return null;
        }
        int xVal = extractVal(commsIdx, WELLS_X_MASK, WELLS_X_SHIFT);
        int yVal = extractVal(commsIdx, WELLS_Y_MASK, WELLS_Y_SHIFT) - 1;
        if(yVal == -1){
            return null;
        }
        return new MapLocation(xVal, yVal);
    }

    // Symmetry methods

    public void resetSymmetry() throws GameActionException {

    }

    public boolean horizontalSymmetryPossible() throws GameActionException {
        return extractVal(SYMMETRY_COMMS_IDX, HORIZONTAL_SYMMETRY_MASK, HORIZONTAL_SYMMETRY_SHIFT) == 1;
    }

    public boolean verticalSymmetryPossible() throws GameActionException {
        return extractVal(SYMMETRY_COMMS_IDX, VERTICAL_SYMMETRY_MASK, VERTICAL_SYMMETRY_SHIFT) == 1;
    }

    public boolean rotationalSymmetryPossible() throws GameActionException {
        return extractVal(SYMMETRY_COMMS_IDX, ROTATIONAL_SYMMETRY_MASK, ROTATIONAL_SYMMETRY_SHIFT) == 1;
    }

    public void eliminateHorizontalSymmetry() throws GameActionException {
        insertVal(SYMMETRY_COMMS_IDX, HORIZONTAL_SYMMETRY_MASK, HORIZONTAL_SYMMETRY_SHIFT, 0);
    }

    public void eliminateVerticalSymmetry() throws GameActionException {
        insertVal(SYMMETRY_COMMS_IDX, VERTICAL_SYMMETRY_MASK, VERTICAL_SYMMETRY_SHIFT, 0);
    }

    public void eliminateRotationalSymmetry() throws GameActionException {
        insertVal(SYMMETRY_COMMS_IDX, ROTATIONAL_SYMMETRY_MASK, ROTATIONAL_SYMMETRY_SHIFT, 0);
    }


}
