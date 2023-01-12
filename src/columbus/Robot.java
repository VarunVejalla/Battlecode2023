package columbus;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.HashSet;
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
    }

    public void run() throws GameActionException{
        // fill in with code common to all robots
        myLoc = rc.getLocation();
        myLocInfo = rc.senseMapInfo(myLoc);
        if(myType != RobotType.HEADQUARTERS){
            checkForNearbyWells();
        }
    }

    public void checkForNearbyWells() {
        WellInfo[] nearbyWells = rc.senseNearbyWells();
        for(WellInfo well : nearbyWells){
            this.wells.add(well.getMapLocation());
        }
    }

}


