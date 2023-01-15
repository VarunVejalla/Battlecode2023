package bell;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import static bell.Constants.*;

class WellSquareInfo {
    MapLocation loc;
    ResourceType type;
    boolean locationKnown;
    boolean commed;

    public WellSquareInfo(MapLocation loc, ResourceType type, boolean locationKnown, boolean commed){
        this.loc = loc;
        this.type = type;
        this.locationKnown = locationKnown;
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
    HashMap<Integer, IslandInfo> islands;
    Comms comms;
    int numHQs;
    MapLocation[] HQlocs;
    int turnCount = 0;
    int[] prevCommsArray = new int[64];
    int numIslands;

    MapLocation[] regionCenters = new MapLocation[NUM_REGIONS_TOTAL];


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
        islands = new HashMap();
        comms = new Comms(rc, this);
        numHQs = 0;
        numIslands = rc.getIslandCount();
        scanNearbySquares();
        readComms();
        updateComms();
    }

    public void run() throws GameActionException{
        // fill in with code common to all robots
        turnCount++;
        myLoc = rc.getLocation();
        Util.log("Currently at: " + myLoc.toString());
        myLocInfo = rc.senseMapInfo(myLoc);
        if(rc.getRoundNum() >= 2){
            readComms();
        }
        if(myType != RobotType.HEADQUARTERS){
            scanNearbySquares();
            updateComms();
        }
        updatedPrevCommsArray();
        computeRegionCenters();
//        for(IslandInfo info : islands.values()){
//            Util.log("Island idx: " + info.idx + ", Island location: " + info.loc + ", Island control " + info.controllingTeam + ", Commed: " + info.commed);
//        }
    }

    // compute all the region centers
    public void computeRegionCenters() throws GameActionException{
        for(int i=0; i< NUM_REGIONS_TOTAL; i++){
            regionCenters[i] = comms.getRegionCenter(i);
        }
    }

    public MapLocation getRegionCenter(int regionIndex) throws GameActionException{
        return regionCenters[regionIndex];
    }

    public void updatedPrevCommsArray() throws GameActionException {
        for(int i = 0; i < prevCommsArray.length; i++){
            prevCommsArray[i] = rc.readSharedArray(i);
        }
    }

    public void readComms() throws GameActionException {
        readHQLocs(); // TODO: We only need to call this once
        readIslandLocs();
        readWellLocations();
    }


    // NOTE: This takes a SHIT TON of bytecode and causes robots to go over their bytecode limit
    public void readWellLocations() throws GameActionException {

//        for(int regionNum = 0; regionNum < comms.NUM_REGIONS_TOTAL; regionNum++){
//            RegionData data = comms.getRegionData(regionNum);
//            // TODO: Process this and somehow add it to the Wells list
//        }
    }

    public void readIslandLocs() throws GameActionException {
        for(int idx = 1; idx <= numIslands; idx++){
            int commsIdx = ISLAND_START_IDX + idx - 1;
            if(rc.readSharedArray(commsIdx) == prevCommsArray[commsIdx]){
                continue;
            }
            MapLocation islandLoc = comms.getIslandLocation(idx);
            if(islandLoc == null){
                // Message is empty
                continue;
            }
            Team controllingTeam = comms.getIslandControl(idx);
            IslandInfo info = new IslandInfo(islandLoc, idx, controllingTeam, true);
            updateIslands(info);
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
                return;
            }
        }
        wells.put(info.loc, info);
    }

    public void updateIslands(IslandInfo info) {
        if(islands.containsKey(info.idx)){
            IslandInfo existingInfo = islands.get(info.idx);
            if(existingInfo.controllingTeam == info.controllingTeam){
                existingInfo.commed |= info.commed;
                return;
            }
        }
        islands.put(info.idx, info);
    }

    public void scanNearbySquares() throws GameActionException {
        // Scan nearby wells
        WellInfo[] nearbyWells = rc.senseNearbyWells();
        for(WellInfo well : nearbyWells){
            MapLocation wellLocation = well.getMapLocation();
            WellSquareInfo info = new WellSquareInfo(wellLocation, well.getResourceType(), true,false);
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

    public void updateComms() throws GameActionException {
        // If you're not in range to write to shared array, skip
        if(!rc.canWriteSharedArray(0, 0)){
            return;
        }

        // Comm any new wells updates to shared array
        for(WellSquareInfo info : wells.values()){
            if(info.commed){
                continue;
            }
            int regionNum = comms.getRegionNum(info.loc);
            RegionData data = comms.getRegionData(regionNum);
            if(info.type == ResourceType.ADAMANTIUM){
                data.adamantiumWell = true;
            }
            else if(info.type == ResourceType.MANA){
                data.manaWell = true;
            }
            else{
                data.elixirWell = true;
            }
            comms.saveRegionData(regionNum, data);
            info.commed = true;
        }

        // Comm any new island updates to shared array
        for(IslandInfo info : islands.values()){
            if(info.commed){
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
        for(IslandInfo info : islands.values()){
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




}
