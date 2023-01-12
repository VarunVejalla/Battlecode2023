package columbus;

import battlecode.common.*;

import java.util.Random;


public class Robot {

    RobotController rc;
    Team myTeam;
    Team opponent;
    Navigation nav;
    MapLocation myLoc;

    static final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };


    public Robot(RobotController rc) throws GameActionException{
        this.rc = rc;
        myLoc = rc.getLocation();
        myTeam = rc.getTeam();
        opponent = myTeam.opponent();
        nav = new Navigation(rc, this);
        Util.rc = rc;
        Util.robot = this;
    }

    public void run() throws GameActionException{
        // fill in with code common to all robots
        myLoc = rc.getLocation();
        System.out.println("MY LOC: " + myLoc.toString());
    }
}


