package lamarr;

import battlecode.common.*;

public class Launcher extends Robot {

    private MapLocation targetLoc;

    // TODO: Replace this with an actually good strategy
    boolean isAttacking = false;
    int DEFENDING_THRESHOLD = 15;
    int ATTACKING_THRESHOLD = 15;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        decideIfAttacking();
    }


    // this method is used in the constructor, runAttackMovement(), and runDefensiveMovement()
    public void decideIfAttacking(){
        // look at enemyIslands vs homeIslands
        // if we're clearly winning, push it

        double defenseProbability = 0.7;
        if(getNumIslandsControlledByTeam(myTeam) > getNumIslandsControlledByTeam(opponent) * 1.5){
            defenseProbability = 0.2;   // make it more likely that we attack if we're clearly winning
        }
        double randomChoice = rng.nextDouble();
        if (randomChoice < defenseProbability) isAttacking = false;    //defend with 70% probability if we're not clearly winning
        else isAttacking=true;

    }


    public void run() throws GameActionException{
        super.run();

        if(isAttacking){
            rc.setIndicatorDot(myLoc, 255, 0, 0);
            Util.log("Yam attacking");
        }
        else{
            rc.setIndicatorDot(myLoc, 0, 255, 0);
            Util.log(rc.getID() + ": Yam defending");
        }
        runAttack();
        runMovement();
        runAttack();
    }


    public int value(RobotInfo enemy) {
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

    public int compare(RobotInfo enemy1, RobotInfo enemy2) {
        // attack launchers, then carriers, then amplifiers, then other things
        int value1 = value(enemy1);
        int value2 = value(enemy2);
        if(value1 != value2) {
            return value1-value2;
        }
        else {
            // same type, attack whichever one is closer to dying
            return enemy1.health-enemy2.health;
        }
    }



    // summary of this method:
    // check to see is attack is ready. if not, you can't do anything so return
    // check to see nearby enemies (note we sense nearby enemies several times in a round, so can optimize that)
    // find the enemy to attack by selecting the one with highest priority / lowest health
    // if you found an enemy, attack. otherwise, don't do anything.
    public void runAttack() throws GameActionException {
        if(!rc.isActionReady()){
            return;
        }
        // Try to attack someone
        int radius = myType.actionRadiusSquared;
        Team opponent = myTeam.opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);

        // want to attack the one that's closest to dying
        // maybe want to attack
        // MapLocation toAttack = enemies[0].location;

        int toAttackIndex = -1;
        for(int i = 0; i < enemies.length; i++) {
            // if it'd be better to attack enemies[i], change attackIndex to i
            if(rc.canAttack(enemies[i].location)) {
                if(toAttackIndex == -1 || compare(enemies[i], enemies[toAttackIndex]) < 0) {
                    toAttackIndex = i;
                }
            }
        }

        if(toAttackIndex != -1){
            MapLocation toAttack = enemies[toAttackIndex].location;
            rc.setIndicatorString("Attacking");
            rc.attack(toAttack);
        }
    }



    // summary of this method: trying to achieve the strat of staying on the border of the enemies' action radius so you don't get hit

    // if you're in a fight (if there's danger nearby), run fighting micro
    //      if you're within range, try attacking
    //      if you're not within range, move in, then try attacking
    //      if you couldn't attack, step away so the enemy can't hit you

    // otherwise (not in danger)
    //      - if we have uncommed islands, go to nearest hq so we can relay info
    //      - if we're in attacking mode (isAttacking == True), run AttackMovement(), which takes us to the nearest uncontrolled island or scouting location
    //      - otherwise, we're defending, so run defensive movement


    public void runMovement() throws GameActionException {
        if(!rc.isMovementReady()){
            return;
        }

        // If you're in a fight w/ an enemy, run launcher micro
        RobotInfo[] enemies = rc.senseNearbyRobots(myType.visionRadiusSquared, opponent);
        boolean dangerNearby = false;
        for(RobotInfo info : enemies){
            if(info.type == RobotType.LAUNCHER || info.type == RobotType.CARRIER){
                dangerNearby = true;
            }
        }
        if(dangerNearby) {
            RobotInfo closestDanger = null;
            for (RobotInfo info : enemies) {
                if (info.type == RobotType.LAUNCHER || info.type == RobotType.CARRIER) {
                    if (closestDanger == null || myLoc.distanceSquaredTo(info.location) < myLoc.distanceSquaredTo(closestDanger.location)) {
                        closestDanger = info;
                    }
                }
            }

            // If you're within range, try attacking then move out.
            runAttack();

            // If you're outside of range but your attack is ready, move in then attack, but only move in if attack is ready
            if (rc.isActionReady()) {
                // get bestDirection to enemy from (should also incorporate info about cooldowns and currents)
                // TODO: Reimplement fuzzynav kinda
                Direction bestDirection = nav.fuzzyNav(closestDanger.location);

                //get all directions sorted by closeness to best direction
                Direction[] potentialMoveDirs = Util.closeDirections(bestDirection);

                // If I can move towards enemy and attack him, then do it.
                for (Direction potentialMoveDir : potentialMoveDirs) {
                    if (!rc.canMove(potentialMoveDir)) {
                        continue;
                    }
                    // TODO: Do the whole "do we have more allied troops than enemy troops" thing to figure out if it's a fight worth taking
                    // Can prolly copy some of that stuff from last year.
                    // If I can move in and attack, then do that.
                    if (myLoc.add(potentialMoveDir).distanceSquaredTo(closestDanger.location) <= myType.actionRadiusSquared) {
                        rc.move(potentialMoveDir);
                        runAttack();
                    }
                }

                // If you couldn't move in then attack, then simply move away until your attack comes back.
                if (rc.isMovementReady()) {
                    Direction enemyDir = myLoc.directionTo(closestDanger.location);
                    MapLocation farAway = myLoc.subtract(enemyDir).subtract(enemyDir).subtract(enemyDir).subtract(enemyDir);
                    nav.goToFuzzy(farAway, 0);
                }
            }
        }else {
                // Otherwise, run normal launcher movement
                if (haveUncommedIsland()) {
                    returnToClosestHQ();
                } else if (isAttacking) {
                    runAttackMovement();
                } else {
                    runDefensiveMovement();
                }
            }
        }


    public boolean haveUncommedIsland() {
        for(int i = 0; i < numIslands; i++){
            IslandInfo info = islands[i];
            if(info == null){
                continue;
            }
            if(!info.commed){
                return true;
            }
        }
        return false;
    }

    public void returnToClosestHQ() throws GameActionException {
        targetLoc = getNearestFriendlyHQ();
        if(myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
            nav.goToFuzzy(targetLoc, 0);
            rc.setIndicatorString("have uncommed info, fuzzying to HQ: " + targetLoc);
        }
        else{
            nav.goToBug(targetLoc, 0);
            rc.setIndicatorString("have uncommed info, bugging to HQ: " + targetLoc);
        }
    }


    // Go attack an island
    //TODO: determine symmetry of map and try to surround HQs?
    public void runAttackMovement() throws GameActionException {

        if(targetLoc != null && myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
            targetLoc = null;
        }

        targetLoc = getNearestUncontrolledIsland();
        if(targetLoc == null){
            targetLoc = getNearestOpposingIsland();
        }
        if(targetLoc == null)
            targetLoc = getRandomScoutingLocation();

        rc.setIndicatorString("going to " + targetLoc + " to attack");
        if(myLoc.distanceSquaredTo(targetLoc) <= myType.actionRadiusSquared){
//            nav.goToFuzzy(targetLoc, 0);
            nav.circle(targetLoc, 0, myType.actionRadiusSquared);

            if(rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam).length > ATTACKING_THRESHOLD && myLoc.distanceSquaredTo(targetLoc) > 8) { // don't want to crowd any areas so leave if you're not super close
                {
                    decideIfAttacking();
                    targetLoc = null;
                }
            }
        }
        else{
            nav.goToBug(targetLoc, myType.actionRadiusSquared);
        }


    }

    // Go defend a well
    //  We should not crowd wells
    // maybe scale the guarding radius upwards as we see more friendly troops?
    //TODO: also defend islands (with the highest priority)
    // TODO: if enough luanchers are already at you're place, go into attacking mode
    public void runDefensiveMovement() throws GameActionException {
        if(targetLoc == null){
            // choose between defending an island, or well (and if well, what type of well?)
            double defensiveChoice = rng.nextDouble();
            if(defensiveChoice < 0.5){
                targetLoc = getNearestFriendlyIsland();     // defend a friendly island with probability 50%
            }
            else{       // defend a well with probability 25%
                MapLocation closestHQ = getNearestFriendlyHQ();
                int HQIdx = getFriendlyHQIndex(closestHQ);
                if(rng.nextBoolean()){      // randomly select between Adamantium and Mana
                    targetLoc = comms.getClosestWell(HQIdx, ResourceType.ADAMANTIUM);
                }
                else{
                    targetLoc = comms.getClosestWell(HQIdx, ResourceType.MANA);
                }
            }
            if(targetLoc == null){
                targetLoc = getRandomScoutingLocation();
            }
        }

        rc.setIndicatorString("going to  " + targetLoc + " to defend");
        int distanceSquaredToTarget = myLoc.distanceSquaredTo(targetLoc);
        if(distanceSquaredToTarget <= myType.actionRadiusSquared){   // we have arrived
            if(rc.senseNearbyRobots(myType.visionRadiusSquared, myTeam).length > DEFENDING_THRESHOLD && distanceSquaredToTarget > 8) { // don't want to crowd any areas so leave if you're not super close
                targetLoc = null;
                decideIfAttacking();    // see if we should switch to attacking mode
            }

            else {
                nav.circle(targetLoc, 2, myType.actionRadiusSquared);
            }
        }

        else{
            nav.goToBug(targetLoc, myType.actionRadiusSquared);
        }
    }

}
