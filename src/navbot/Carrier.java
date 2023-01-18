package navbot;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

// TODO: Be aware of how many carriers are at your mine and go to a farther away mine if ur mine is too crowded.

public class Carrier extends Robot {

    public Carrier(RobotController rc) throws GameActionException {
        super(rc);
    }




    public void run() throws GameActionException {
        MapLocation A = new MapLocation(0,0);   // bottom left
        MapLocation B = new MapLocation(rc.getMapWidth(), rc.getMapHeight());   // top right
        MapLocation C = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);   // middle of the map


        nav.goToFuzzy(B,0);

        super.run();

    }



}