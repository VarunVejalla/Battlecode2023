package alexander;

import battlecode.common.*;


// some parts inspired by: https://github.com/mvpatel2000/Battlecode2022/blob/main/src/athena/CommsHandler.java

public class Comms {

    RobotController rc;
    Robot robot;
    Constants constants;

    public Comms(RobotController rc, Robot robot){
        this.rc = rc;
        this.robot = robot;
        this.constants = new Constants();
    }

    /// General Methods

    private int extractVal(int commsIdx, int mask, int shift) throws GameActionException {
        return (rc.readSharedArray(commsIdx) & mask) >> shift;
    }

    private void insertVal(int commsIdx, int mask, int shift, int value) throws GameActionException {
        // Clear out the existing value in that position
        int clearMask = constants.FULL_MASK - mask;
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

        insertVal(adamantiumWellIndex, constants.RESOURCE_RATIO_MASK, constants.RESOURCE_RATIO_SHIFT, adamantium);
        insertVal(manaWellIndex, constants.RESOURCE_RATIO_MASK, constants.RESOURCE_RATIO_SHIFT, mana);
        insertVal(elixirWellIndex, constants.RESOURCE_RATIO_MASK, constants.RESOURCE_RATIO_SHIFT, elixir);
    }


    public int[] readRatio(int idx) throws GameActionException {
        int adamantiumWellIndex = getClosestWellCommsIndex(idx, ResourceType.ADAMANTIUM);
        int manaWellIndex = getClosestWellCommsIndex(idx, ResourceType.MANA);
        int elixirWellIndex = getClosestWellCommsIndex(idx, ResourceType.ELIXIR);

        int adamantium = extractVal(adamantiumWellIndex, constants.RESOURCE_RATIO_MASK, constants.RESOURCE_RATIO_SHIFT);
        int mana = extractVal(manaWellIndex, constants.RESOURCE_RATIO_MASK, constants.RESOURCE_RATIO_SHIFT);
        int elixir = extractVal(elixirWellIndex, constants.RESOURCE_RATIO_MASK, constants.RESOURCE_RATIO_SHIFT);


        int[] ratioData = new int[3];
        ratioData[constants.ADAMANTIUM_RATIO_INDEX] = adamantium;
        ratioData[constants.MANA_RATIO_INDEX] = mana;
        ratioData[constants.ELIXIR_RATIO_INDEX] = elixir;

        return ratioData;
    }

    public int readOurHQXCoord(int idx) throws GameActionException {
        return extractVal(constants.HQ_LOC_IDX_MAP[idx], constants.HQ_X_MASK, constants.HQ_X_SHIFT);
    }

    public int readOurHQYCoord(int idx) throws GameActionException {
        // note that we subtract 1 from the y coordinate after reading the y location
        // this is done so that all zeros in an index in the shared array means it hasn't been claimed yet
        return extractVal(constants.HQ_LOC_IDX_MAP[idx], constants.HQ_Y_MASK, constants.HQ_Y_SHIFT) - 1;
    }

    public void writeOurHQXCoord(int idx, int value) throws GameActionException {
        insertVal(constants.HQ_LOC_IDX_MAP[idx], constants.HQ_X_MASK, constants.HQ_X_SHIFT, value);
    }

    public void writeOurHQYCoord(int idx, int value) throws GameActionException {
        // note that we add 1 to the y coordinate before writing to the shared array
        // this is done so that all zeros in an index in the shared array means it hasn't been claimed yet

        value += 1;
        insertVal(constants.HQ_LOC_IDX_MAP[idx], constants.HQ_Y_MASK, constants.HQ_Y_SHIFT, value);
    }

    public int readMana(int idx) throws GameActionException {
        return extractVal(constants.HQ_RESOURCES_IDX_MAP[idx], constants.HQ_MANA_MASK, constants.HQ_MANA_SHIFT) * 100;
    }

    public int readAdamantium(int idx) throws GameActionException {
        return extractVal(constants.HQ_RESOURCES_IDX_MAP[idx], constants.HQ_ADAMANTIUM_MASK, constants.HQ_ADAMANTIUM_SHIFT) * 100;
    }

    public void writeMana(int idx, int value) throws GameActionException {
        value /= 100;
        if(value > 127) value = 127;

        insertVal(constants.HQ_RESOURCES_IDX_MAP[idx], constants.HQ_MANA_MASK, constants.HQ_MANA_SHIFT, value);
    }

    public void writeAdamantium(int idx, int value) throws GameActionException {
        value /= 100;
        if(value > 127) value = 127;

        insertVal(constants.HQ_RESOURCES_IDX_MAP[idx], constants.HQ_ADAMANTIUM_MASK, constants.HQ_ADAMANTIUM_SHIFT, value);
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

        insertVal(constants.ISLAND_START_IDX + islandIdx - 1, constants.ISLAND_CONTROL_MASK, constants.ISLAND_CONTROL_SHIFT, controlValue);
    }


    public Team getIslandControl(int islandIdx) throws GameActionException {
        int controlVal = extractVal(constants.ISLAND_START_IDX + islandIdx - 1, constants.ISLAND_CONTROL_MASK, constants.ISLAND_CONTROL_SHIFT);
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
        insertVal(constants.ISLAND_START_IDX + islandIdx - 1,constants.ISLAND_X_MASK, constants.ISLAND_X_SHIFT, xVal);
        insertVal(constants.ISLAND_START_IDX + islandIdx - 1, constants.ISLAND_Y_MASK, constants.ISLAND_Y_SHIFT, yVal);
    }

    public MapLocation getIslandLocation(int islandIdx) throws GameActionException {
        if(rc.readSharedArray(constants.ISLAND_START_IDX + islandIdx - 1) == 0){
            return null;
        }
        int xVal = extractVal(constants.ISLAND_START_IDX + islandIdx - 1, constants.ISLAND_X_MASK, constants.ISLAND_X_SHIFT);
        int yVal = extractVal(constants.ISLAND_START_IDX + islandIdx - 1, constants.ISLAND_Y_MASK, constants.ISLAND_Y_SHIFT) - 1;
        return new MapLocation(xVal, yVal);
    }

    /// Wells stuff

    public int getClosestWellCommsIndex(int HQIndex, ResourceType resource){
        switch(resource){
            case ADAMANTIUM:
                return constants.WELLS_START_IDX + HQIndex * constants.NUM_WELLS_PER_HQ + constants.ADAMANTIUM_WELL_OFFSET;
            case MANA:
                return constants.WELLS_START_IDX + HQIndex * constants.NUM_WELLS_PER_HQ + constants.MANA_WELL_OFFSET;
            case ELIXIR:
                return constants.WELLS_START_IDX + HQIndex * constants.NUM_WELLS_PER_HQ + constants.ELIXIR_WELL_OFFSET;
        }
        throw new RuntimeException("INVALID RESOURCE SPECIFIED FOR getClosestWellCommsIndex! " + resource);
    }

    public void setClosestWell(int HQIndex, ResourceType resource, MapLocation wellLoc) throws GameActionException{
        int commsIdx = getClosestWellCommsIndex(HQIndex, resource);
        int xVal = wellLoc.x;
        int yVal = wellLoc.y + 1;
        insertVal(commsIdx, constants.WELLS_X_MASK, constants.WELLS_X_SHIFT, xVal);
        insertVal(commsIdx, constants.WELLS_Y_MASK, constants.WELLS_Y_SHIFT, yVal);
    }

    public MapLocation getClosestWell(int HQIndex, ResourceType resource) throws GameActionException {
        int commsIdx = getClosestWellCommsIndex(HQIndex, resource);
        if(rc.readSharedArray(commsIdx) == 0){
            return null;
        }
        int xVal = extractVal(commsIdx, constants.WELLS_X_MASK, constants.WELLS_X_SHIFT);
        int yVal = extractVal(commsIdx, constants.WELLS_Y_MASK, constants.WELLS_Y_SHIFT) - 1;
        if(yVal == -1){
            return null;
        }
        return new MapLocation(xVal, yVal);
    }

    // Symmetry methods

    public void resetSymmetry() throws GameActionException {
        rc.writeSharedArray(constants.SYMMETRY_COMMS_IDX, 7);
    }

    public boolean checkSymmetryPossible(SymmetryType type) throws GameActionException {
        switch (type) {
            case HORIZONTAL:
                return extractVal(constants.SYMMETRY_COMMS_IDX, constants.HORIZONTAL_SYMMETRY_MASK, constants.HORIZONTAL_SYMMETRY_SHIFT) == 1;
            case VERTICAL:
                return extractVal(constants.SYMMETRY_COMMS_IDX, constants.VERTICAL_SYMMETRY_MASK, constants.VERTICAL_SYMMETRY_SHIFT) == 1;
            case ROTATIONAL:
                return extractVal(constants.SYMMETRY_COMMS_IDX, constants.ROTATIONAL_SYMMETRY_MASK, constants.ROTATIONAL_SYMMETRY_SHIFT) == 1;
        }
        return true;
    }

    public void eliminateSymmetry(SymmetryType type) throws GameActionException {
        switch(type) {
            case HORIZONTAL:
                insertVal(constants.SYMMETRY_COMMS_IDX, constants.HORIZONTAL_SYMMETRY_MASK, constants.HORIZONTAL_SYMMETRY_SHIFT, 0);
                break;
            case VERTICAL:
                insertVal(constants.SYMMETRY_COMMS_IDX, constants.VERTICAL_SYMMETRY_MASK, constants.VERTICAL_SYMMETRY_SHIFT, 0);
                break;
            case ROTATIONAL:
                insertVal(constants.SYMMETRY_COMMS_IDX, constants.ROTATIONAL_SYMMETRY_MASK, constants.ROTATIONAL_SYMMETRY_SHIFT, 0);
                break;
        }
    }

    // New well detection

    public int getNewWellCommsIdx(ResourceType type){
        switch(type){
            case ADAMANTIUM:
                return Constants.NEW_ADAMANTIUM_WELL_COMMS_IDX;
            case MANA:
                return Constants.NEW_MANA_WELL_COMMS_IDX;
            case ELIXIR:
                return Constants.NEW_ELIXIR_WELL_COMMS_IDX;
        }
        throw new RuntimeException("Resource not found for getNewWellsCommsIdx: " + type);
    }

    public MapLocation getNewWellDetected(ResourceType type) throws GameActionException {
        int commsIdx = getNewWellCommsIdx(type);
        int xVal = extractVal(commsIdx, constants.NEW_WELL_X_MASK, constants.NEW_WELL_X_SHIFT);
        int yVal = extractVal(commsIdx, constants.NEW_WELL_Y_MASK, constants.NEW_WELL_Y_SHIFT) - 1;
        if(yVal == -1){
            return null;
        }
        return new MapLocation(xVal, yVal);
    }

    public void setNewWellDetected(MapLocation loc, ResourceType type) throws GameActionException {
        int commsIdx = getNewWellCommsIdx(type);
        int xVal = loc.x;
        int yVal = loc.y + 1;
        insertVal(commsIdx, constants.NEW_WELL_X_MASK, constants.NEW_WELL_X_SHIFT, xVal);
        insertVal(commsIdx, constants.NEW_WELL_Y_MASK, constants.NEW_WELL_Y_SHIFT, yVal);
    }

    public void resetNewWellComms() throws GameActionException {
        rc.writeSharedArray(Constants.NEW_ADAMANTIUM_WELL_COMMS_IDX, 0);
        rc.writeSharedArray(Constants.NEW_MANA_WELL_COMMS_IDX, 0);
        rc.writeSharedArray(Constants.NEW_ELIXIR_WELL_COMMS_IDX, 0);
    }

}
