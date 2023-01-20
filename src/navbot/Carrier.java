package navbot;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

// TODO: Be aware of how many carriers are at your mine and go to a farther away mine if ur mine is too crowded.

public class Carrier extends Robot {



    MapLocation destination;
    public Carrier(RobotController rc) throws GameActionException {
        super(rc);
    }


    public void run() throws GameActionException {
//        myLoc = rc.getLocation();
        destination = new MapLocation(29,29);   // top right
        nav.goToFuzzy(destination,0);
        super.run();


    }



}