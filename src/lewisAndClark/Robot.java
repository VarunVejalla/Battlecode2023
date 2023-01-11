package lewisAndClark;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.Team;

import java.util.Random;


public class Robot {

    RobotController rc;
    Team myTeam;
    Team opponent;

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
        myTeam = rc.getTeam();
        opponent = myTeam.opponent();
    }

    public void run() throws GameActionException{
        // fill in with code common to all robots
    }
}


