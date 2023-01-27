package carver4;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * Array containing all the possible movement directions.
     */
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

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc The RobotController object. You use it to perform actions from this robot, and to get
     *           information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        Robot robot;
        switch (rc.getType()) {
            case HEADQUARTERS:
                robot = new Headquarters(rc); break;
            case LAUNCHER:
                robot = new Launcher(rc); break;
            case CARRIER:
                robot = new Carrier(rc); break;
            case BOOSTER:
                robot = new Booster(rc); break;
            case DESTABILIZER:
                robot = new Destabilizer(rc); break;
            case AMPLIFIER:
                robot = new Amplifier(rc); break;
            default:
                robot = new Launcher(rc); break;
        }


        while (true) {
            turnCount += 1;
//            if(turnCount > 500){
//                rc.resign();
//            }
            try {
                robot.indicatorString = "";
                int roundNum = rc.getRoundNum();
                robot.run();
                rc.setIndicatorString(robot.indicatorString);
                if(roundNum != rc.getRoundNum()){
                    System.out.println("BYTECODE EXCEEDED REEEE!");
//                    rc.resign();
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
}
