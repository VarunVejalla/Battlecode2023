package ali7;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class DamageFinder {
    static RobotController rc;
    static Robot robot;

    public static double[] getDamages(MapLocation myLocation, MapLocation[] possibleSpots, boolean[] newSpotIsValid, RobotInfo[] nearbyVisionEnemies){
        int distance;
        double[] enemyDamage = new double[9];
        for(RobotInfo enemy : nearbyVisionEnemies){
            distance = enemy.location.distanceSquaredTo(myLocation);
            if (enemy.type == RobotType.LAUNCHER || enemy.type == RobotType.DESTABILIZER) {
                // don't need to check if distance is <= 5
                if (distance <= 5) {
                    continue;
                }
            } else if (enemy.type == RobotType.HEADQUARTERS || enemy.type == RobotType.CARRIER) {
                if (distance <= 2 || distance >= 20) {
                    continue;
                }
            }

            int damage = Util.getEnemyDamage(enemy);
            int xDiff = enemy.location.x-myLocation.x;
            int yDiff = enemy.location.y-myLocation.y;

            if(enemy.type == RobotType.LAUNCHER){

                if(xDiff == -4 && yDiff == -2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                }

                else if(xDiff == -4 && yDiff == -1){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                }

                else if(xDiff == -4 && yDiff == 0){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                }

                else if(xDiff == -4 && yDiff == 1){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[0]+=damage;
                }

                else if(xDiff == -4 && yDiff == 2){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                }

                else if(xDiff == -3 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                }

                else if(xDiff == -3 && yDiff == -2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                }

                else if(xDiff == -3 && yDiff == -1){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == -3 && yDiff == 0){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == -3 && yDiff == 1){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == -3 && yDiff == 2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                }

                else if(xDiff == -3 && yDiff == 3){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[0]+=damage;
                }

                else if(xDiff == -2 && yDiff == -4){
                    enemyDamage[5]+=damage;
                    enemyDamage[4]+=damage;
                }

                else if(xDiff == -2 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == -2 && yDiff == -2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == -2 && yDiff == 2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == -2 && yDiff == 3){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == -2 && yDiff == 4){
                    enemyDamage[7]+=damage;
                    enemyDamage[0]+=damage;
                }

                else if(xDiff == -1 && yDiff == -4){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == -1 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == -1 && yDiff == 3){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == -1 && yDiff == 4){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 0 && yDiff == -4){
                    enemyDamage[5]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == 0 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 0 && yDiff == 3){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 0 && yDiff == 4){
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 1 && yDiff == -4){
                    enemyDamage[5]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 1 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 1 && yDiff == 3){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 1 && yDiff == 4){
                    enemyDamage[7]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 2 && yDiff == -4){
                    enemyDamage[4]+=damage;
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == 2 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 2 && yDiff == -2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 2 && yDiff == 2){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 2 && yDiff == 3){
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 2 && yDiff == 4){
                    enemyDamage[0]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == -3){
                    enemyDamage[4]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 3 && yDiff == -2){
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == -1){
                    enemyDamage[5]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == 0){
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == 1){
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == 2){
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == 3){
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 4 && yDiff == -2){
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 4 && yDiff == -1){
                    enemyDamage[4]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 4 && yDiff == 0){
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 4 && yDiff == 1){
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 4 && yDiff == 2){
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else{
                    System.out.println("fucked up with updating damages");
                }


            } else if(enemy.type == RobotType.HEADQUARTERS || enemy.type == RobotType.CARRIER){
                //same action radius

                if(xDiff == -4 && yDiff == -1){
                    enemyDamage[5]+=damage;
                }

                else if(xDiff == -4 && yDiff == 0){
                    enemyDamage[6]+=damage;
                }

                else if(xDiff == -4 && yDiff == 1){
                    enemyDamage[7]+=damage;
                }

                else if(xDiff == -3 && yDiff == -3){
                    enemyDamage[5]+=damage;
                }

                else if(xDiff == -3 && yDiff == -2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                }

                else if(xDiff == -3 && yDiff == -1){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                }

                else if(xDiff == -3 && yDiff == 0){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                }

                else if(xDiff == -3 && yDiff == 1){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[0]+=damage;
                }

                else if(xDiff == -3 && yDiff == 2){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                }

                else if(xDiff == -3 && yDiff == 3){
                    enemyDamage[7]+=damage;
                }

                else if(xDiff == -2 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[4]+=damage;
                }

                else if(xDiff == -2 && yDiff == -2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                }

                else if(xDiff == -2 && yDiff == -1){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == -2 && yDiff == 0){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == -2 && yDiff == 1){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == -2 && yDiff == 2){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                }

                else if(xDiff == -2 && yDiff == 3){
                    enemyDamage[7]+=damage;
                    enemyDamage[0]+=damage;
                }

                else if(xDiff == -1 && yDiff == -4){
                    enemyDamage[5]+=damage;
                }

                else if(xDiff == -1 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == -1 && yDiff == -2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == -1 && yDiff == 2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == -1 && yDiff == 3){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == -1 && yDiff == 4){
                    enemyDamage[7]+=damage;
                }

                else if(xDiff == 0 && yDiff == -4){
                    enemyDamage[4]+=damage;
                }

                else if(xDiff == 0 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == 0 && yDiff == -2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 0 && yDiff == 2){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 0 && yDiff == 3){
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 0 && yDiff == 4){
                    enemyDamage[0]+=damage;
                }

                else if(xDiff == 1 && yDiff == -4){
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == 1 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 1 && yDiff == -2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 1 && yDiff == 2){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 1 && yDiff == 3){
                    enemyDamage[7]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 1 && yDiff == 4){
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 2 && yDiff == -3){
                    enemyDamage[4]+=damage;
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == 2 && yDiff == -2){
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 2 && yDiff == -1){
                    enemyDamage[5]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 2 && yDiff == 0){
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 2 && yDiff == 1){
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 2 && yDiff == 2){
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 2 && yDiff == 3){
                    enemyDamage[0]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == -3){
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == 3 && yDiff == -2){
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 3 && yDiff == -1){
                    enemyDamage[4]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == 0){
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == 1){
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == 2){
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == 3){
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 4 && yDiff == -1){
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == 4 && yDiff == 0){
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 4 && yDiff == 1){
                    enemyDamage[1]+=damage;
                }


                else{
                    System.out.println("fucked up with updating damages");
                }

            } else if(enemy.type == RobotType.DESTABILIZER){

                if(xDiff == -4 && yDiff == -2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                }

                else if(xDiff == -4 && yDiff == -1){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                }

                else if(xDiff == -4 && yDiff == 0){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                }

                else if(xDiff == -4 && yDiff == 1){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                }

                else if(xDiff == -4 && yDiff == 2){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                }

                else if(xDiff == -3 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                }

                else if(xDiff == -3 && yDiff == -2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                }

                else if(xDiff == -3 && yDiff == -1){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                }

                else if(xDiff == -3 && yDiff == 0){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                }

                else if(xDiff == -3 && yDiff == 1){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                }

                else if(xDiff == -3 && yDiff == 2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                }

                else if(xDiff == -3 && yDiff == 3){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[0]+=damage;
                }

                else if(xDiff == -2 && yDiff == -4){
                    enemyDamage[5]+=damage;
                    enemyDamage[4]+=damage;
                }

                else if(xDiff == -2 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == -2 && yDiff == -2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == -2 && yDiff == 2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == -2 && yDiff == 3){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == -2 && yDiff == 4){
                    enemyDamage[7]+=damage;
                    enemyDamage[0]+=damage;
                }

                else if(xDiff == -1 && yDiff == -4){
                    enemyDamage[5]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == -1 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == -1 && yDiff == 3){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == -1 && yDiff == 4){
                    enemyDamage[7]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 0 && yDiff == -4){
                    enemyDamage[5]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == 0 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 0 && yDiff == 3){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 0 && yDiff == 4){
                    enemyDamage[7]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 1 && yDiff == -4){
                    enemyDamage[5]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == 1 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 1 && yDiff == 3){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 1 && yDiff == 4){
                    enemyDamage[7]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 2 && yDiff == -4){
                    enemyDamage[4]+=damage;
                    enemyDamage[3]+=damage;
                }

                else if(xDiff == 2 && yDiff == -3){
                    enemyDamage[5]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 2 && yDiff == -2){
                    enemyDamage[5]+=damage;
                    enemyDamage[6]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 2 && yDiff == 2){
                    enemyDamage[6]+=damage;
                    enemyDamage[7]+=damage;
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 2 && yDiff == 3){
                    enemyDamage[7]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 2 && yDiff == 4){
                    enemyDamage[0]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == -3){
                    enemyDamage[4]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 3 && yDiff == -2){
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == -1){
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == 0){
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == 1){
                    enemyDamage[4]+=damage;
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == 2){
                    enemyDamage[8]+=damage;
                    enemyDamage[0]+=damage;
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 3 && yDiff == 3){
                    enemyDamage[0]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 4 && yDiff == -2){
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                }

                else if(xDiff == 4 && yDiff == -1){
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 4 && yDiff == 0){
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 4 && yDiff == 1){
                    enemyDamage[3]+=damage;
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else if(xDiff == 4 && yDiff == 2){
                    enemyDamage[2]+=damage;
                    enemyDamage[1]+=damage;
                }

                else{
                    System.out.println("fucked up with updating damages");
                }


            } else if(enemy.type == RobotType.BOOSTER){
                //TODO
            }
        }
        return enemyDamage;

    }

}
