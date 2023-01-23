package examplefuncsplayer;

import battlecode.common.*;

import static examplefuncsplayer.OptimalResource.getOptimalResourceCount;

public class CarrierStrategy {

    static MapLocation hqLoc;
    static MapLocation wellLoc;
    static WellInfo well;
    static MapLocation islandLoc;

    static boolean anchorMode = false;
    static int numHeadquarters = 0;

    static int optimalAmount;

    /**
     * Run a single turn for a Carrier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runCarrier(RobotController rc) throws GameActionException {
        if (RobotPlayer.turnCount == 2) {
            Communication.updateHeadquarterInfo(rc);
        }
        if(hqLoc == null) scanHQ(rc);
        if(wellLoc == null) scanWells(rc);
        scanIslands(rc);
        System.out.println(wellLoc);

        //Collect from well if close and inventory not full
        if(wellLoc != null) {
            int distance = (int) Math.sqrt(rc.getLocation().distanceSquaredTo(wellLoc));
            optimalAmount = getOptimalResourceCount(distance, well.isUpgraded());
            if(rc.canCollectResource(wellLoc, -1) && getTotalResources(rc) < optimalAmount) {
                rc.collectResource(wellLoc, -1); /**MAYBE ITS TRYING TO COLLECT OPTIMAL AMOUNT EACH TIME AND GETTING CONFUSED?*/
            }
        }

        //Transfer resource to headquarters
        depositResource(rc, ResourceType.ADAMANTIUM);
        depositResource(rc, ResourceType.MANA);

        if(rc.canTakeAnchor(hqLoc, Anchor.STANDARD)) {
            rc.takeAnchor(hqLoc, Anchor.STANDARD);
            anchorMode = true;
            Communication.addCarierWithAnchor(rc);/**Needs to put this in communication array so that it becomes the leader for a bunch of launchers, that way it can head toward the well and be protected*/
        }

        //no resources -> look for well
        if(anchorMode) {
            if(islandLoc == null) {
                for (int i = Communication.STARTING_ISLAND_IDX; i < Communication.STARTING_ISLAND_IDX + GameConstants.MAX_NUMBER_ISLANDS; i++) {
                    MapLocation islandNearestLoc = Communication.readIslandLocation(rc, i);
                    if (islandNearestLoc != null) {
                        islandLoc = islandNearestLoc;
                        break;
                    }
                }
            }
            else Pathing.moveTowards(rc, islandLoc);

            if(rc.canPlaceAnchor() && rc.senseTeamOccupyingIsland(rc.senseIsland(rc.getLocation())) == Team.NEUTRAL) {
                rc.placeAnchor();
                anchorMode = false;
            }
        }
        else {
            int total = getTotalResources(rc);
            if(total == 0) {
                //move towards well or search for well
                if(wellLoc == null) RobotPlayer.moveRandom(rc); //COULD BE COOL TO KEEP A LOG OF THE PREVIOUS STEPS TO MAKE REPEATING STEPS STOP HAPPENING
                else if(!rc.getLocation().isAdjacentTo(wellLoc)) Pathing.moveTowards(rc, wellLoc);
            }
            if (total==optimalAmount){ /**just changed this from else if to else*/
                //move towards HQ
                Pathing.moveTowards(rc, hqLoc);
            }
        }
        Communication.tryWriteMessages(rc);
    }

    static void scanHQ(RobotController rc) throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        int j = 0;
        for(int i = 0; i<robots.length; i++) {
            if (robots[i].getTeam() == rc.getTeam() && robots[i].getType() == RobotType.HEADQUARTERS) {
                hqLoc = robots[i].getLocation();
                j = i;
                break; /**Should hopefully find the first HQ robot. If there's more than one, the next part will start from the index it left off at and see if it can find a closer one**/
            }
        }
        for(int k = j; k<robots.length; k++) {
            if (robots[k].getTeam() == rc.getTeam() && robots[k].getType() == RobotType.HEADQUARTERS && rc.getLocation().distanceSquaredTo(hqLoc) > rc.getLocation().distanceSquaredTo(robots[k].getLocation())) {
                hqLoc = robots[k].getLocation();
            } /**This keeps going to see if any of the other HQs are closer**/
        }
        /**RobotInfo[] robots = rc.senseNearbyRobots();
        for(RobotInfo robot : robots) {
            if(robot.getTeam() == rc.getTeam() && robot.getType() == RobotType.HEADQUARTERS) {
                hqLoc = robot.getLocation();
                break;
            }
        }**/ /**IMPROVEMENT SEAN MADE: WILL CHOSE THE NEAREST HQ. THIS COULD BE COMPUTATIONALLY EXPENSIVE BECAUSE THERE ARE A LOT OF ROBOTS, SO MAYBE GO BACK TO ORIGINAL AT SOME POINT**/
    }

    static void scanWells(RobotController rc) throws GameActionException {
        WellInfo[] wells = rc.senseNearbyWells();
        if(wells.length > 0) {
            wellLoc = wells[0].getMapLocation();
            well = wells[0];
            for(int i = 1; i < wells.length; i++) {
                if(rc.getLocation().distanceSquaredTo(wellLoc) > rc.getLocation().distanceSquaredTo(wells[i].getMapLocation())) { //*Note! getMapLocation is for wells, getLocation is for robots*//
                    wellLoc = wells[i].getMapLocation();
                    well = wells[i];
                    /**IMPROVEMENT SEAN MADE: WILL CHOSE THE NEAREST WELL**/ /**FIX THIS SHIT*/
                    /**Also, updates well so we actually get a well info object*/
                }
            }
        }
    }

    static void depositResource(RobotController rc, ResourceType type) throws GameActionException {
        int amount = rc.getResourceAmount(type);
        if(amount > 0) {
            if(rc.canTransferResource(hqLoc, type, amount)) rc.transferResource(hqLoc, type, amount);
        }
    }

    static int getTotalResources(RobotController rc) {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM)
                + rc.getResourceAmount(ResourceType.MANA)
                + rc.getResourceAmount(ResourceType.ELIXIR);
    }

    static void scanIslands(RobotController rc) throws GameActionException {
        int[] ids = rc.senseNearbyIslands();
        int j=0;
        for(int i = 0; i<ids.length; i++) {
            Communication.updateIslandInfo(rc, ids[i]);
            if (rc.senseTeamOccupyingIsland(ids[i]) == Team.NEUTRAL) {
                islandLoc = rc.senseNearbyIslandLocations(ids[i])[0];
                j = i;
                break; /**Should hopefully find a island. If there's more than one, the next part will start from the index it left off at and see if it can find a closer one**/
            }
        }
        for(int k = j; k<ids.length; k++) {
            Communication.updateIslandInfo(rc, ids[k]);
            if (rc.senseTeamOccupyingIsland(ids[k]) == Team.NEUTRAL && rc.getLocation().distanceSquaredTo(islandLoc) > rc.getLocation().distanceSquaredTo(rc.senseNearbyIslandLocations(ids[k])[0])) {
                islandLoc = rc.senseNearbyIslandLocations(ids[k])[0];
            } /**This keeps going to see if any of the other islands are closer**/
        }/**IMPROVEMENT SEAN MADE: WILL CHOSE THE NEAREST ISLAND**/
        /**int[] ids = rc.senseNearbyIslands();
        for(int id : ids) {
            if(rc.senseTeamOccupyingIsland(id) == Team.NEUTRAL) {
                MapLocation[] locs = rc.senseNearbyIslandLocations(id);
                if(locs.length > 0) {
                    islandLoc = locs[0];
                    break;
                }
            }
            Communication.updateIslandInfo(rc, id);
        }**/
    }
}