package bell;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

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

    static final Random rng = new Random(6147);

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
        scanNearbySquares();
        updateComms();
    }

    public void run() throws GameActionException{
        // fill in with code common to all robots
        myLoc = rc.getLocation();
        Util.log("Currently at: " + myLoc.toString());
        myLocInfo = rc.senseMapInfo(myLoc);
        if(numHQs == 0 && rc.getRoundNum() >= 2){
            readComms();
        }
        if(myType != RobotType.HEADQUARTERS){
            scanNearbySquares();
            updateComms();
        }
        for(IslandInfo info : islands.values()){
            Util.log("Island idx: " + info.idx + ", Island location: " + info.loc + ", Island control " + info.controllingTeam);
        }
    }

    public void readComms() throws GameActionException {
        readHQLocs();
        readIslandLocs();
        readWellLocations();
    }

    public void readWellLocations() throws GameActionException {
        for(int regionNum = 0; regionNum < comms.NUM_REGIONS_TOTAL; regionNum++){
            RegionData data = comms.getRegionData(regionNum);
            // TODO: Process this and somehow add it to the Wells list
        }
    }

    public void readIslandLocs() throws GameActionException {
        for(int idx = 1; idx <= 35; idx++){
            MapLocation islandLoc = comms.getIslandLocation(idx);
            if(islandLoc != null){
                Team controllingTeam = comms.getIslandControl(idx);
                // TODO: Only update this if it changed AFTER we last saw the island.
                IslandInfo info = new IslandInfo(islandLoc, idx, controllingTeam, true);
                updateIslands(info);
            }
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

    public MapLocation getNearestUncontrolledIsland(){
        return getNearestIsland(Team.NEUTRAL);
    }

    public MapLocation getNearestFriendlyIsland(){
        return getNearestIsland(myTeam);
    }

    public MapLocation getNearestOpposingIsland(){
        return getNearestIsland(opponent);
    }


    public MapLocation getRandomScoutingLocation() {
        int x = rng.nextInt(rc.getMapWidth());
        int y = rng.nextInt(rc.getMapHeight());
        return new MapLocation(x, y);
    }




}
