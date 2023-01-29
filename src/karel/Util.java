package karel;

import battlecode.common.*;

public class Util {
    static RobotController rc;
    static Robot robot;

    public static int minMovesToReach(MapLocation a, MapLocation b){
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.max(Math.abs(dx), Math.abs(dy));
    }

    public static Direction[] closeDirections(Direction dir){
        Direction[] close = {
                dir,
                dir.rotateLeft(),
                dir.rotateRight(),
                dir.rotateLeft().rotateLeft(),
                dir.rotateRight().rotateRight(),
                dir.rotateLeft().rotateLeft().rotateLeft(),
                dir.rotateRight().rotateRight().rotateRight(),
                dir.opposite()
        };
        return close;
    }


    public static boolean tryMine(ResourceType type, int numResources) throws GameActionException {
        WellInfo[] nearbyWells = rc.senseNearbyWells(RobotType.CARRIER.actionRadiusSquared);
        for(WellInfo well : nearbyWells){
            if(well.getResourceType() != type){
                continue;
            }
            int rate = numResources;
            if(rate > well.getRate()){
                rate = well.getRate();
            }
            // See if you can collect the requested amount.
            if (rc.canCollectResource(well.getMapLocation(), rate)) {
                rc.collectResource(well.getMapLocation(), rate);
                return true;
            }
        }

        return false;
    }

    public static boolean tryMove(Direction dir) throws GameActionException{
        if(rc.canMove(dir)) {
            rc.move(dir);
            robot.myLoc = rc.getLocation();
            robot.myLocInfo = rc.senseMapInfo(robot.myLoc);
            robot.nav.lastDirectionMoved = dir;
            return true;
        }
        return false;
    }

    public static int checkNumSymmetriesPossible() throws GameActionException {
        return getPotentialSymmetries().length;
    }

    public static SymmetryType[] getPotentialSymmetries() throws GameActionException {
        SymmetryType[] potential = new SymmetryType[3];
        int num = 0;
        if(robot.comms.checkSymmetryPossible(SymmetryType.HORIZONTAL)){
            potential[num] = SymmetryType.HORIZONTAL;
            num++;
        }
        if(robot.comms.checkSymmetryPossible(SymmetryType.VERTICAL)){
            potential[num] = SymmetryType.VERTICAL;
            num++;
        }
        if(robot.comms.checkSymmetryPossible(SymmetryType.ROTATIONAL)){
            potential[num] = SymmetryType.ROTATIONAL;
            num++;
        }
        SymmetryType[] output = new SymmetryType[num];
        for(int i = 0; i < num; i++){
            output[i] = potential[i];
        }
        return output;
    }

    public static MapLocation applySymmetry(MapLocation loc, SymmetryType type){
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        switch(type){
            case HORIZONTAL:
                return new MapLocation(width - loc.x - 1, loc.y);
            case VERTICAL:
                return new MapLocation(loc.x, height - loc.y - 1);
            case ROTATIONAL:
                return new MapLocation(width - loc.x - 1, height - loc.y - 1);
        }
        return null;
    }

    public static MapLocation getClosestMapLocation(MapLocation[] locs){
        MapLocation closest = null;
        int closestDist = Integer.MAX_VALUE;
        for(MapLocation loc : locs){
            int dist = robot.myLoc.distanceSquaredTo(loc);
            if(dist < closestDist){
                closest = loc;
                closestDist = dist;
            }
        }
        return closest;
    }

    public static ResourceType determineWhichResourceToGet(int HQImHelpingIdx) throws GameActionException {
        int[] ratio = robot.comms.readRatio(HQImHelpingIdx);
        int num = robot.rng.nextInt(15);  // note that 16 is an exclusive bond

        // e.g let's say the resources are [10, 2, 3] (Adamantium, mana, elxir)
        // cumuulative sums become [10, 12, 15]

        // we first check if the random number is less than 10, if so we return adamantium
        // otherwise, we get the cumulative sum for the next index (2+10) = 12, and we see if the random variable is less than 12. if so, we return mana
        // otherwise, return elixir

        //Ratio data indices

        if(num < ratio[robot.constants.ADAMANTIUM_RATIO_INDEX]) {
            Util.log("Gonna go find Adamantium");
            return ResourceType.ADAMANTIUM;
        }

        // get cumulative sum so far by adding up adamantium ratio w/ mana ratio
        ratio[robot.constants.MANA_RATIO_INDEX] += ratio[robot.constants.ADAMANTIUM_RATIO_INDEX];   //get cumulative sum up till now
        if(num < ratio[1]) {
            Util.log("Gonna go find Mana");
            return ResourceType.MANA;
        }

        else{
            Util.log("Gonna go find Elixir");
            return ResourceType.ELIXIR;
        }
    }

    public static int getNumTroopsInRange(int radius, Team team, RobotType type) throws GameActionException {
        RobotInfo[] infos = team == null ? rc.senseNearbyRobots(radius) : rc.senseNearbyRobots(radius, team);
        return getNumTroopsInRange(infos, type);
    }

    public static int getNumTroopsInRange(RobotInfo[] infos, RobotType type){
        if(type == null){
            return infos.length;
        }
        int count = 0;
        for(RobotInfo info : infos){
            if(info.type == type){
                count++;
            }
        }
        return count;
    }

    /// Region stuff
    public static int getRegionX(MapLocation loc){
        int regionWidth = rc.getMapWidth() / Constants.NUM_REGIONS_HORIZONTAL;
        int numUppers = rc.getMapWidth() % Constants.NUM_REGIONS_HORIZONTAL;

        // the first numUppers should have regionWidth+1 squares
        // the rest should have regionWith squares
        if(loc.x+1 <= numUppers * (regionWidth+1)) {
//            return (int) Math.ceil((loc.x+1)/(regionWidth+1) - 1); //don't want integer division
            if((loc.x+1)%(regionWidth+1) == 0){
                return (loc.x+1)/(regionWidth+1)-1;
            }
            else{
                return (loc.x+1)/(regionWidth+1);
            }

        }
        else {
//            return (int) Math.ceil((loc.x+1-numUppers)/regionWidth - 1); // don't want integer division
            if((loc.x+1-numUppers)%(regionWidth) == 0){
                return (loc.x+1-numUppers)/(regionWidth)-1;
            }
            else{
                return (loc.x+1-numUppers)/(regionWidth);
            }
        }
    }

    public static int getRegionY(MapLocation loc){
        int regionHeight = rc.getMapHeight() / Constants.NUM_REGIONS_VERTICAL;
        int numUppers = rc.getMapHeight() % Constants.NUM_REGIONS_VERTICAL;
        if(loc.y+1 <= numUppers * (regionHeight+1)) {
//            return (int) Math.ceil((loc.y+1)/(regionHeight+1) - 1);
            if((loc.y+1)%(regionHeight+1) == 0){
                return (loc.y+1)/(regionHeight+1)-1;
            }
            else{
                return (loc.y+1)/(regionHeight+1);
            }
        }
        else {
//            return (int) Math.ceil((loc.y+1-numUppers)/regionHeight - 1);
            if((loc.y+1-numUppers)%(regionHeight) == 0){
                return (loc.y+1-numUppers)/(regionHeight)-1;
            }
            else{
                return (loc.y+1-numUppers)/(regionHeight);
            }
        }
    }

    public static int getRegionNum(MapLocation loc){
        return getRegionY(loc) * Constants.NUM_REGIONS_HORIZONTAL + getRegionX(loc);
    }

    public static int getEnemyDamage(RobotInfo info){
        switch(info.type){
            case LAUNCHER:
            case DESTABILIZER:
            case HEADQUARTERS:
                return info.type.damage;
            case CARRIER:
                int mass = info.getResourceAmount(ResourceType.ADAMANTIUM) +info.getResourceAmount(ResourceType.MANA) + info.getResourceAmount(ResourceType.ELIXIR);
                return (int)(mass * GameConstants.CARRIER_DAMAGE_FACTOR);
            default:
                return 0;
        }
    }

    public static int attackValue(RobotInfo enemy) {
        if(enemy.type == RobotType.LAUNCHER) {
            return 0;
        } else if (enemy.type == RobotType.CARRIER) {
            return 1;
        } else if (enemy.type == RobotType.AMPLIFIER) {
            return 2;
        } else {
            return 3;
        }
    }

    public static int attackCompare(RobotInfo enemy1, RobotInfo enemy2) {
        // attack launchers, then carriers, then amplifiers, then other things
        int value1 = attackValue(enemy1);
        int value2 = attackValue(enemy2);
        if(value1 != value2) {
            return value1-value2;
        }
        else {
            // same type, attack whichever one is closer to dying
            return enemy1.health - enemy2.health;
        }
    }

    public static int addDiffToSquaredNum(int x, int diff) {
        int sqrt = (int) Math.sqrt(x);
        int newSqrt = sqrt + diff;
        return newSqrt * newSqrt;
    }

    public static void addToIndicatorString(String str){
        robot.indicatorString += str + ";";
    }

    public static void log(String str){
        if(true){
            return;
        }

//        if(rc.getType() != RobotType.LAUNCHER){
//            return;
//        }

//        if(rc.getID() != 12586){
//            return;
//        }
//        if(rc.getType() == RobotType.HEADQUARTERS)  System.out.println(str);


        System.out.println(str);
    }
}
