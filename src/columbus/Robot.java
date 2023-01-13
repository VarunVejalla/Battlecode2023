package columbus;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;


public class Robot {

    RobotController rc;
    Team myTeam;
    Team opponent;
    RobotType myType;
    Navigation nav;
    MapLocation myLoc;
    MapInfo myLocInfo;
    HashSet<MapLocation> wells;
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
        wells = new HashSet<MapLocation>();
        checkForNearbyWells();
        comms = new Comms(rc, this);
        numHQs = 0;
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
            checkForNearbyWells();
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

    public void checkForNearbyWells() {
        WellInfo[] nearbyWells = rc.senseNearbyWells();
        for(WellInfo well : nearbyWells){
            this.wells.add(well.getMapLocation());
        }
    }

}


