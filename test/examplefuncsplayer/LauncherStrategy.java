package examplefuncsplayer;

import battlecode.common.*;

public class LauncherStrategy {

    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runLauncher(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        int lowestHealth = 100;
        int smallestDistance = 100;
        RobotInfo target = null;
        if (RobotPlayer.turnCount == 2) {
            Communication.updateHeadquarterInfo(rc);
        }
        Communication.clearObsoleteEnemies(rc);
        if (enemies.length > 0) {
            for (RobotInfo enemy: enemies){
                Communication.reportEnemy(rc, enemy.location);
                int enemyHealth = enemy.getHealth();
                int enemyDistance = enemy.getLocation().distanceSquaredTo(rc.getLocation());
                if (enemyHealth < lowestHealth){
                    target = enemy;
                    lowestHealth = enemyHealth;
                    smallestDistance = enemyDistance;
                }
                else if (enemyHealth == lowestHealth){
                    if (enemyDistance < smallestDistance){
                        target = enemy;
                        smallestDistance = enemyDistance;
                    }
                }
            }
        }
        Communication.tryWriteMessages(rc);
        if (target != null){
            if (rc.canAttack(target.getLocation()))
                rc.attack(target.getLocation());
        }
        else {

            WellInfo[] wells = rc.senseNearbyWells();
            if (wells.length > 0){
                MapLocation wellLoc = wells[0].getMapLocation();
                Direction dir = rc.getLocation().directionTo(wellLoc);
                if (rc.canMove(dir))
                    rc.move(dir);
            }
        }

        RobotInfo[] visibleEnemies = rc.senseNearbyRobots(-1, opponent);
        for (RobotInfo enemy : visibleEnemies) {
            if (enemy.getType() != RobotType.HEADQUARTERS) {
                MapLocation enemyLocation = enemy.getLocation();
                MapLocation robotLocation = rc.getLocation();
                Direction moveDir = robotLocation.directionTo(enemyLocation);
                if (rc.canMove(moveDir)) {
                    rc.move(moveDir);
                }
            }
        }

        // Also try to move randomly.
        Direction dir = RobotPlayer.directions[RobotPlayer.rng.nextInt(RobotPlayer.directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}