package cicero;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static bell.Constants.ISLAND_START_IDX;

class WellSquareInfo {
    MapLocation loc;
    ResourceType type;
    boolean commed;

    public WellSquareInfo(MapLocation loc, ResourceType type, boolean commed){
        this.loc = loc;
        this.type = type;
        this.commed = commed;
    }
}

class IslandInfo {
    MapLocation loc;
    int idx;
    Team controllingTeam;
    boolean commed;

    public IslandInfo(MapLocation loc, int idx, Team controllingTeam, boolean commed){
        this.loc = loc;
        this.idx = idx;
        this.controllingTeam = controllingTeam;
        this.commed = commed;
    }

    public String toString() {
        return "Island idx: " + idx + ", Island loc: " + loc + ", Island control: " + controllingTeam + ", Commed: " + commed;
    }
}

public class Robot {

    RobotController rc;
    Team myTeam;
    Team opponent;
    RobotType myType;
    Navigation nav;
    MapLocation myLoc;
    MapInfo myLocInfo;
    HashMap<MapLocation, WellSquareInfo> wells;
//    HashMap<Integer, IslandInfo> islands;
    int numIslands;
    IslandInfo[] islands;
    Comms comms;
    int numHQs;
    MapLocation[] HQlocs;
    int turnCount = 0;
    int[] prevCommsArray = new int[64];

    static Random rng;

    /** Array containing all the possible movement directions. */
    static final Direction[] movementDirections = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static final Direction[] allDirections = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
            Direction.CENTER,
    };

    public Robot(RobotController rc) throws GameActionException{
        rng = new Random(rc.getID());
        this.rc = rc;
        myLoc = rc.getLocation();
        myTeam = rc.getTeam();
        myType = rc.getType();
        opponent = myTeam.opponent();
        nav = new Navigation(rc, this);
        Util.rc = rc;
        Util.robot = this;
        wells = new HashMap();
//        islands = new HashMap();
        numIslands = rc.getIslandCount();
        islands = new IslandInfo[numIslands + 1];
        comms = new Comms(rc, this);
        numHQs = 0;
        if(myType == RobotType.HEADQUARTERS){
            scanNearbySquares();
            readComms();
            updateComms();
        }
        else{
            readHQLocs();
        }
    }

    public void run() throws GameActionException{
        // fill in with code common to all robots
        turnCount++;
        myLoc = rc.getLocation();
        Util.log("Currently at: " + myLoc.toString());
        myLocInfo = rc.senseMapInfo(myLoc);
        if(rc.getRoundNum() == 2 && myType == RobotType.HEADQUARTERS){
            readHQLocs();
        }
        if(rc.getRoundNum() >= 2){
            readComms();
        }
        if(myType != RobotType.HEADQUARTERS){
            scanNearbySquares();
            updateComms();
        }
        updatePrevCommsArray();
//        for(IslandInfo info : islands.values()){
//            Util.log("Island idx: " + info.idx + ", Island location: " + info.loc + ", Island control " + info.controllingTeam + ", Commed: " + info.commed);
//        }
    }

    public void readComms() throws GameActionException { // 2500 bytecode
        readIslandLocs();
        readWellLocations();
    }

    // TODO: We need to change this up a little bit once we start using elixir and the well's resource can change.
    public void readWellLocations() throws GameActionException { // 1500-2000 bytecode
        for(int HQIdx = 0; HQIdx < numHQs; HQIdx++){
            int wellIndex = comms.getClosestWellCommsIndex(HQIdx, ResourceType.ADAMANTIUM); // Ada well index
            if(rc.readSharedArray(wellIndex) != prevCommsArray[wellIndex]){
                MapLocation adamantiumLoc = comms.getClosestWell(HQIdx, ResourceType.ADAMANTIUM);
                if(adamantiumLoc != null){
                    updateWells(new WellSquareInfo(adamantiumLoc, ResourceType.ADAMANTIUM, true));
                }
            }
            wellIndex++; // Mana well index
            if(rc.readSharedArray(wellIndex) != prevCommsArray[wellIndex]) {
                MapLocation manaLoc = comms.getClosestWell(HQIdx, ResourceType.MANA);
                if (manaLoc != null) {
                    updateWells(new WellSquareInfo(manaLoc, ResourceType.MANA, true));
                }
            }
            wellIndex++; // Elixir well index
            if(rc.readSharedArray(wellIndex) != prevCommsArray[wellIndex]) {
                MapLocation elixirLoc = comms.getClosestWell(HQIdx, ResourceType.ELIXIR);
                if (elixirLoc != null) {
                    updateWells(new WellSquareInfo(elixirLoc, ResourceType.ELIXIR, true));
                }
            }
        }
    }

    public void readIslandLocs() throws GameActionException {
        for(int idx = 1; idx <= numIslands; idx++){
            int commsIdx = ISLAND_START_IDX + idx - 1;
            if(rc.readSharedArray(commsIdx) == prevCommsArray[commsIdx]){
                continue;
            }
            MapLocation islandLoc = comms.getIslandLocation(idx);
            if(islandLoc == null){
                continue;
            }
            Team controllingTeam = comms.getIslandControl(idx);
            IslandInfo info = new IslandInfo(islandLoc, idx, controllingTeam, true);
            updateIslands(info); // 150 bytecode
        }
    }

    public void readHQLocs() throws GameActionException {
        ArrayList<MapLocation> locs = new ArrayList<>();
        for(int i = 0; i < 4; i++){
            if(rc.readSharedArray(i) == 0){
                break;
            }
            else{
                locs.add(comms.readOurHQLocation(i));
            }
        }
        numHQs = locs.size();
        HQlocs = new MapLocation[0];
        HQlocs = locs.toArray(HQlocs);
    }

    public MapLocation getClosestWellToHQ(int HQIndex) throws GameActionException {
        MapLocation well1 = comms.getClosestWell(HQIndex, ResourceType.ADAMANTIUM);
        MapLocation well2 = comms.getClosestWell(HQIndex, ResourceType.MANA);
        MapLocation well3 = comms.getClosestWell(HQIndex, ResourceType.ELIXIR);
        MapLocation HQLoc = HQlocs[HQIndex];
        MapLocation closest = null;
        int closestDist = Integer.MAX_VALUE;
        if(well1 != null && HQLoc.distanceSquaredTo(well1) < closestDist){
            closest = well1;
            closestDist = HQLoc.distanceSquaredTo(well1);
        }
        if(well2 != null && HQLoc.distanceSquaredTo(well2) < closestDist){
            closest = well2;
            closestDist = HQLoc.distanceSquaredTo(well2);
        }
        if(well3 != null && HQLoc.distanceSquaredTo(well3) < closestDist){
            closest = well3;
            closestDist = HQLoc.distanceSquaredTo(well3);
        }
        return closest;
    }

    // tried to unroll this to save bytecode
    public MapLocation getNearestFriendlyHQ() throws GameActionException {
        int distanceToHQ_0, distanceToHQ_1, distanceToHQ_2,distanceToHQ_3;
        if(HQlocs == null) readHQLocs();

        if(numHQs == 1){
            return HQlocs[0];
        }

        else if(numHQs == 2){
            distanceToHQ_0 = HQlocs[0].distanceSquaredTo(myLoc);
            distanceToHQ_1 = HQlocs[1].distanceSquaredTo(myLoc);

            if(distanceToHQ_0 <= distanceToHQ_1) return HQlocs[0];
            return HQlocs[1];
        }

        else if(numHQs == 3){
            distanceToHQ_0 = HQlocs[0].distanceSquaredTo(myLoc);
            distanceToHQ_1 = HQlocs[1].distanceSquaredTo(myLoc);
            distanceToHQ_2 = HQlocs[2].distanceSquaredTo(myLoc);

            if(distanceToHQ_0 <= distanceToHQ_1 && distanceToHQ_0 <= distanceToHQ_2) return HQlocs[0];
            else if(distanceToHQ_1 <= distanceToHQ_0 && distanceToHQ_1 <= distanceToHQ_2) return HQlocs[1];
            return HQlocs[2];
        }

        else{
            distanceToHQ_0 = HQlocs[0].distanceSquaredTo(myLoc);
            distanceToHQ_1 = HQlocs[1].distanceSquaredTo(myLoc);
            distanceToHQ_2 = HQlocs[2].distanceSquaredTo(myLoc);
            distanceToHQ_3 = HQlocs[3].distanceSquaredTo(myLoc);


            if(distanceToHQ_0 <= distanceToHQ_1
                    && distanceToHQ_0 <= distanceToHQ_2
                    && distanceToHQ_0 <= distanceToHQ_3) return HQlocs[0];

            else if(distanceToHQ_1 <= distanceToHQ_0
                    && distanceToHQ_1 <= distanceToHQ_2
                    && distanceToHQ_1 <= distanceToHQ_3) return HQlocs[1];

            else if(distanceToHQ_2 <= distanceToHQ_0
                    && distanceToHQ_2 <= distanceToHQ_1
                    && distanceToHQ_2 <= distanceToHQ_3) return HQlocs[2];

            return HQlocs[3];
        }
    }

    public int getFriendlyHQIndex(MapLocation friendlyHQ) throws GameActionException {
        for(int i = 0; i < numHQs; i++){
            if(friendlyHQ.equals(HQlocs[i])){
                return i;
            }
        }
        return -1;
    }

    public void updateWells(WellSquareInfo info) {
        if(wells.containsKey(info.loc)){
            WellSquareInfo existingInfo = wells.get(info.loc);
            if(existingInfo.type == info.type){
                existingInfo.commed |= info.commed;
                return;
            }
        }
        wells.put(info.loc, info);
    }

    public void updateIslands(IslandInfo info) {
        if(islands[info.idx] != null){
            IslandInfo existingInfo = islands[info.idx];
            if(existingInfo.controllingTeam == info.controllingTeam){
                existingInfo.commed |= info.commed;
                return;
            }
        }
        islands[info.idx] = info;
    }

    public void scanNearbySquares() throws GameActionException { // 1000 bytecode
        // Scan nearby wells
        WellInfo[] nearbyWells = rc.senseNearbyWells();
        for(WellInfo well : nearbyWells){
            MapLocation wellLocation = well.getMapLocation();
            WellSquareInfo info = new WellSquareInfo(wellLocation, well.getResourceType(),false);
            updateWells(info);
        }

        // Scan for nearby islands
        int[] islandIdxs = rc.senseNearbyIslands();
        for(int islandIdx : islandIdxs){
            MapLocation[] islandLocs = rc.senseNearbyIslandLocations(islandIdx);
            for(MapLocation islandLoc : islandLocs){
                Team occupyingTeam = rc.senseTeamOccupyingIsland(islandIdx);
                IslandInfo info = new IslandInfo(islandLoc, islandIdx, occupyingTeam, false);
                updateIslands(info);
            }
        }
    }

    public void updateComms() throws GameActionException { // 1000 bytecode
        // If you're not in range to write to shared array, skip
        if(!rc.canWriteSharedArray(0, 0)){
            return;
        }

        // Comm any new wells updates to shared array
        for(WellSquareInfo info : wells.values()){
            if(info.commed){
                continue;
            }
            for(int HQIdx = 0; HQIdx < numHQs; HQIdx++){
                MapLocation HQLoc = HQlocs[HQIdx];
                MapLocation closestWellToThatHQ = comms.getClosestWell(HQIdx, info.type);
                if(closestWellToThatHQ == null || HQLoc.distanceSquaredTo(info.loc) < HQLoc.distanceSquaredTo(closestWellToThatHQ)){
                    comms.setClosestWell(HQIdx, info.type, info.loc);
                }
            }
            info.commed = true;
        }

        // Comm any new island updates to shared array
        for(int idx = 1; idx <= numIslands; idx++){
            IslandInfo info = islands[idx];
            if(info == null || info.commed){
                continue;
            }
            Util.log("Updating comms w/ new island info: " + info.toString());
            comms.writeIslandLocation(info.idx, info.loc);
            comms.writeIslandControl(info.idx, info.controllingTeam);
            info.commed = true;
        }

    }

    // Find the nearest well of a specific resource. Null to allow any resource
    public MapLocation getNearestWell(ResourceType type){
        int closestDist = Integer.MAX_VALUE;
        MapLocation closestWell = null;
        for(MapLocation keyLoc : wells.keySet()){
            Util.log("Well: " + keyLoc);
            if(type != null && wells.get(keyLoc).type != type){
                continue;
            }
            int dist = myLoc.distanceSquaredTo(keyLoc);
            if(dist < closestDist){
                closestDist = dist;
                closestWell = keyLoc;
             }
        }
        return closestWell;
    }

    // Find the nearest well
    public MapLocation getNearestWell(){
        return getNearestWell(null);
    }

    // Find the nearest island with specified controlling team
    private MapLocation getNearestIsland(Team controllingTeam){
        int closestDist = Integer.MAX_VALUE;
        MapLocation closestIsland = null;
        for(int idx = 1; idx <= numIslands; idx++){
            IslandInfo info = islands[idx];
            if(info == null){
                continue;
            }
            Util.log("Island: " + info.loc);
            if(info.controllingTeam != controllingTeam){
                continue;
            }
            int dist = myLoc.distanceSquaredTo(info.loc);
            if(dist < closestDist){
                closestDist = dist;
                closestIsland = info.loc;
            }
        }

        return closestIsland;
    }

    public MapLocation getNearestUncontrolledIsland(){ return getNearestIsland(Team.NEUTRAL); }
    public MapLocation getNearestFriendlyIsland(){ return getNearestIsland(myTeam); }
    public MapLocation getNearestOpposingIsland(){ return getNearestIsland(opponent); }

    public MapLocation getRandomScoutingLocation() {
        int x = rng.nextInt(rc.getMapWidth());
        int y = rng.nextInt(rc.getMapHeight());
        return new MapLocation(x, y);
    }


    public void updatePrevCommsArray() throws GameActionException { // 1000 bytecode
        prevCommsArray[0] = rc.readSharedArray(0);
        prevCommsArray[1] = rc.readSharedArray(1);
        prevCommsArray[2] = rc.readSharedArray(2);
        prevCommsArray[3] = rc.readSharedArray(3);
        prevCommsArray[4] = rc.readSharedArray(4);
        prevCommsArray[5] = rc.readSharedArray(5);
        prevCommsArray[6] = rc.readSharedArray(6);
        prevCommsArray[7] = rc.readSharedArray(7);
        prevCommsArray[8] = rc.readSharedArray(8);
        prevCommsArray[9] = rc.readSharedArray(9);
        prevCommsArray[10] = rc.readSharedArray(10);
        prevCommsArray[11] = rc.readSharedArray(11);
        prevCommsArray[12] = rc.readSharedArray(12);
        prevCommsArray[13] = rc.readSharedArray(13);
        prevCommsArray[14] = rc.readSharedArray(14);
        prevCommsArray[15] = rc.readSharedArray(15);
        prevCommsArray[16] = rc.readSharedArray(16);
        prevCommsArray[17] = rc.readSharedArray(17);
        prevCommsArray[18] = rc.readSharedArray(18);
        prevCommsArray[19] = rc.readSharedArray(19);
        prevCommsArray[20] = rc.readSharedArray(20);
        prevCommsArray[21] = rc.readSharedArray(21);
        prevCommsArray[22] = rc.readSharedArray(22);
        prevCommsArray[23] = rc.readSharedArray(23);
        prevCommsArray[24] = rc.readSharedArray(24);
        prevCommsArray[25] = rc.readSharedArray(25);
        prevCommsArray[26] = rc.readSharedArray(26);
        prevCommsArray[27] = rc.readSharedArray(27);
        prevCommsArray[28] = rc.readSharedArray(28);
        prevCommsArray[29] = rc.readSharedArray(29);
        prevCommsArray[30] = rc.readSharedArray(30);
        prevCommsArray[31] = rc.readSharedArray(31);
        prevCommsArray[32] = rc.readSharedArray(32);
        prevCommsArray[33] = rc.readSharedArray(33);
        prevCommsArray[34] = rc.readSharedArray(34);
        prevCommsArray[35] = rc.readSharedArray(35);
        prevCommsArray[36] = rc.readSharedArray(36);
        prevCommsArray[37] = rc.readSharedArray(37);
        prevCommsArray[38] = rc.readSharedArray(38);
        prevCommsArray[39] = rc.readSharedArray(39);
        prevCommsArray[40] = rc.readSharedArray(40);
        prevCommsArray[41] = rc.readSharedArray(41);
        prevCommsArray[42] = rc.readSharedArray(42);
        prevCommsArray[43] = rc.readSharedArray(43);
        prevCommsArray[44] = rc.readSharedArray(44);
        prevCommsArray[45] = rc.readSharedArray(45);
        prevCommsArray[46] = rc.readSharedArray(46);
        prevCommsArray[47] = rc.readSharedArray(47);
        prevCommsArray[48] = rc.readSharedArray(48);
        prevCommsArray[49] = rc.readSharedArray(49);
        prevCommsArray[50] = rc.readSharedArray(50);
        prevCommsArray[51] = rc.readSharedArray(51);
        prevCommsArray[52] = rc.readSharedArray(52);
        prevCommsArray[53] = rc.readSharedArray(53);
        prevCommsArray[54] = rc.readSharedArray(54);
        prevCommsArray[55] = rc.readSharedArray(55);
        prevCommsArray[56] = rc.readSharedArray(56);
        prevCommsArray[57] = rc.readSharedArray(57);
        prevCommsArray[58] = rc.readSharedArray(58);
        prevCommsArray[59] = rc.readSharedArray(59);
        prevCommsArray[60] = rc.readSharedArray(60);
        prevCommsArray[61] = rc.readSharedArray(61);
        prevCommsArray[62] = rc.readSharedArray(62);
        prevCommsArray[63] = rc.readSharedArray(63);
    }

}
