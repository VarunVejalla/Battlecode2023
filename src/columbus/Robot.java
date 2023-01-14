package columbus;

import battlecode.common.*;

import java.util.*;
import java.util.Map.Entry;

enum SquareType {
    WELL, ISLAND
}

class SquareInfo {
    MapLocation loc;
    SquareType squareType;
    ResourceType resourceType; // Only applicable for wells
    Team occupyingTeam; // Only applicable for islands
    boolean commed;

    public SquareInfo(MapLocation loc, SquareType squareType, ResourceType resourceType, Team occupyingTeam, boolean commed){
        this.loc = loc;
        this.squareType = squareType;
        this.resourceType = resourceType;
        this.occupyingTeam = occupyingTeam;
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
    HashMap<MapLocation, SquareInfo> keySquares;
    Comms comms;
    int numHQs;
    MapLocation[] HQlocs;
    RegionData[] regionDatas;

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
        keySquares = new HashMap();
        comms = new Comms(rc, this);
        numHQs = 0;
        regionDatas = new RegionData[comms.NUM_REGIONS_TOTAL];
        scanNearbySquares();
        updateComms();
    }

    public void run() throws GameActionException{
        // fill in with code common to all robots
        myLoc = rc.getLocation();
        Util.log("Currently at: " + myLoc.toString());
        myLocInfo = rc.senseMapInfo(myLoc);
        if(numHQs == 0 && rc.getRoundNum() >= 2){
            readHQLocs();
        }
        if(myType != RobotType.HEADQUARTERS){
            scanNearbySquares();
            updateComms();
            readRegionData();
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

    public void readRegionData() throws GameActionException {
        for(int regionNum = 0; regionNum < comms.NUM_REGIONS_TOTAL; regionNum++){
            RegionData data = comms.getRegionData(regionNum);
            regionDatas[regionNum] = data;
        }
    }

    public void updateKeySquares(MapLocation loc, SquareInfo info){
        if(keySquares.containsKey(loc)){
            SquareInfo existingInfo = keySquares.get(loc);
            if(existingInfo.squareType == info.squareType && existingInfo.resourceType == info.resourceType){
                return;
            }
        }
        keySquares.put(loc, info);
    }

    public void scanNearbySquares() throws GameActionException {
        // Scan nearby wells
        WellInfo[] nearbyWells = rc.senseNearbyWells();
        for(WellInfo well : nearbyWells){
            MapLocation wellLocation = well.getMapLocation();
            SquareInfo info = new SquareInfo(wellLocation, SquareType.WELL, well.getResourceType(), null, false);
            updateKeySquares(wellLocation, info);
        }

        // Scan for nearby islands
        int[] islandIdxs = rc.senseNearbyIslands();
        for(int islandIdx : islandIdxs){
            MapLocation[] islandLocs = rc.senseNearbyIslandLocations(islandIdx);
            for(MapLocation islandLoc : islandLocs){
                Team occupyingTeam = rc.senseTeamOccupyingIsland(islandIdx);
                SquareInfo info = new SquareInfo(islandLoc, SquareType.ISLAND, null, occupyingTeam, false);
                updateKeySquares(islandLoc, info);
            }
        }
    }

    public void updateComms() throws GameActionException {
        // If you're not in range to write to shared array, skip
        if(!rc.canWriteSharedArray(0, 0)){
            return;
        }

        // Comm any new updates to shared array
        for(SquareInfo info : keySquares.values()){
            if(info.commed){
                continue;
            }
            int regionNum = comms.getRegionNum(info.loc);
            RegionData data = comms.getRegionData(regionNum);
            if(info.squareType == SquareType.ISLAND){
                if(info.occupyingTeam == myTeam){
                    data.islandStatus = 2;
                }
                else if(info.occupyingTeam == opponent){
                    data.islandStatus = 3;
                }
                else{
                    data.islandStatus = 1;
                }
            }
            else if(info.squareType == SquareType.WELL){
                if(info.resourceType == ResourceType.ADAMANTIUM){
                    data.adamantiumWell = true;
                }
                else if(info.resourceType == ResourceType.MANA){
                    data.manaWell = true;
                }
                else{
                    data.elixirWell = true;
                }
            }
            comms.saveRegionData(regionNum, data);
            info.commed = true;
        }
    }

    // Find the nearest well
    public MapLocation getNearestWell(){
        int closestDist = Integer.MAX_VALUE;
        MapLocation closestWell = null;
        for(MapLocation keyLoc : keySquares.keySet()){
            if(keySquares.get(keyLoc).squareType != SquareType.WELL){
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
    public MapLocation getNearestUncontrolledIsland(){

        int closestDist = Integer.MAX_VALUE;
        MapLocation closestIsland = null;
        for(MapLocation keyLoc : keySquares.keySet()){
            if(keySquares.get(keyLoc).squareType != SquareType.ISLAND){
                continue;
            }
            if(keySquares.get(keyLoc).occupyingTeam != Team.NEUTRAL){
                continue;
            }
            int dist = myLoc.distanceSquaredTo(keyLoc);
            if(dist < closestDist){
                closestDist = dist;
                closestIsland = keyLoc;
            }
        }

        return closestIsland;
    }

    public MapLocation getRandomScoutingLocation() {
        int x = rng.nextInt(rc.getMapWidth());
        int y = rng.nextInt(rc.getMapHeight());
        return new MapLocation(x, y);
    }

}


