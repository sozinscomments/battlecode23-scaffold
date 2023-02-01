package examplefuncsplayer;

import battlecode.common.*;

import java.util.Random;

import static examplefuncsplayer.OptimalResource.getOptimalResourceCount;

public class CarrierStrategy {

    static MapLocation hqLoc;
    static MapLocation wellLoc;
    static WellInfo well;
    static MapLocation islandLoc;
    static int islandLocIndex;
    static MapLocation oldIslandLoc;

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
        scanWells(rc); /**making this not conditional lets them go to other wells, but now they wont add reality anchors*/
        scanIslands(rc);

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
            System.out.println("Broadcasting Carrier ID: " + rc.getID());
            System.out.println("Starting Island Index: "+ Communication.STARTING_ISLAND_IDX);
            System.out.println("Carrier with anchor index: " + Communication.CARRIER_WITH_ANCHOR_IDX);
            System.out.println("INDEX 4: " + rc.readSharedArray(Communication.STARTING_ISLAND_IDX));
            System.out.println("INDEX 14: " + rc.readSharedArray(Communication.CARRIER_WITH_ANCHOR_IDX));
            rc.takeAnchor(hqLoc, Anchor.STANDARD);
            anchorMode = true;
            rc.setIndicatorString("I MUST BE PROTECTED AT ALL COSTS");
            Communication.addCarrierWithAnchor(rc);/**Needs to put this in communication array so that it becomes the leader for a bunch of launchers, that way it can head toward the well and be protected*/
        }

        //no resources -> look for well
        if(anchorMode) {
            if (rc.getHealth() < 100 && rc.readSharedArray(Communication.CARRIER_WITH_ANCHOR_IDX) == rc.getID()){
                rc.writeSharedArray(Communication.CARRIER_WITH_ANCHOR_IDX,0);
                rc.setIndicatorString("I'm dying so I let them know that");
            }
            System.out.println("MY CURRENT LOCATION IS " + rc.getLocation() + " AND MY TARGET ISLAND IS AT " + islandLoc);
            if(islandLoc == null) {
                for (int i = Communication.STARTING_ISLAND_IDX; i < Communication.STARTING_ISLAND_IDX + 10; i++) {
                    MapLocation islandNearestLoc = Communication.readIslandLocation(rc, i);
                    if (islandNearestLoc != null && Communication.readTeamHoldingIsland(rc,i)==Team.NEUTRAL) {
                        islandLoc = islandNearestLoc;
                        islandLocIndex = i;
                    }
                }
            }
            else if (rc.getLocation().x == islandLoc.x && rc.getLocation().y == islandLoc.y) { /**IDEALLY: IF YOU GOT THERE AND STILL CANT PLACE IT, TRY TO FIND ANOTHER ISLAND*/
                /**Should also figure out how to write messages so you can put the team in the communication array*/
                //                System.out.println("AM I GETTING TO SET ISLAND LOC TO NULL FR");
                if (rc.senseTeamOccupyingIsland(islandLocIndex)!=Team.NEUTRAL){
                    oldIslandLoc = new MapLocation(islandLoc.x, islandLoc.y);
                    for (int i = Communication.STARTING_ISLAND_IDX; i < Communication.STARTING_ISLAND_IDX + 10; i++) {
                        MapLocation islandNearestLoc = Communication.readIslandLocation(rc, i);
                        rc.setIndicatorString("From the ELIF, Maybe I should go to ISLAND " + (i - Communication.STARTING_ISLAND_IDX +1));
                        if (islandNearestLoc != null && Communication.readTeamHoldingIsland(rc, i) == Team.NEUTRAL) {
                            islandLoc = islandNearestLoc;
                            islandLocIndex = i - Communication.STARTING_ISLAND_IDX +1;
                            if (!islandLoc.equals(oldIslandLoc)) {
                                break;
                            }
                        }
                    } /**Make the search through the shared array for islands a method*/
                }
                //return; /**POTENTIALLY FIXES SOMETHING? THE PROBLEM IS THE CARRIER WITH ANCHOR VALUE ISN'T SET TO 0*/
            }
            else {
                if (Communication.readTeamHoldingIsland(rc, islandLocIndex)!=Team.NEUTRAL) {
                    oldIslandLoc = new MapLocation(islandLoc.x,islandLoc.y);
                    for (int i = Communication.STARTING_ISLAND_IDX; i < Communication.STARTING_ISLAND_IDX + 10; i++) {
                        MapLocation islandNearestLoc = Communication.readIslandLocation(rc, i);
                        rc.setIndicatorString("Maybe I should go to ISLAND " + (i-Communication.STARTING_ISLAND_IDX));
                        if (islandNearestLoc != null && Communication.readTeamHoldingIsland(rc,i)==Team.NEUTRAL) {
                            islandLoc = islandNearestLoc;
                            islandLocIndex = i-Communication.STARTING_ISLAND_IDX;
                            if (!islandLoc.equals(oldIslandLoc)) {
                                break;
                            }
                        }
                    } /**Make the search through the shared array for islands a method*/
                }
                Pathing.moveTowards(rc, islandLoc);
                rc.setIndicatorString("I'M MOVING TOWARD " + islandLoc + "WHICH IS CURRENTLY HELD BY TEAM " + Communication.readTeamHoldingIsland(rc, islandLocIndex));
            }

            if(rc.canPlaceAnchor() && rc.senseTeamOccupyingIsland(rc.senseIsland(rc.getLocation())) == Team.NEUTRAL) {
                System.out.println("AM I GETTING TO A PLACE WHERE IT THINKGS ITS PLACING ANCHORS?");
                rc.placeAnchor();
                anchorMode = false;
                System.out.println("I'm placing the anchor and the anchormode is now false");
                if(rc.readSharedArray(Communication.CARRIER_WITH_ANCHOR_IDX) == rc.getID()){
                    rc.writeSharedArray(Communication.CARRIER_WITH_ANCHOR_IDX,0);
                    rc.setIndicatorString("Just placed an anchor and told HQ they can build more");
                }
            }
//            else if (rc.getLocation().x == islandLoc.x && rc.getLocation().y == islandLoc.y){ /**IDEALLY: IF YOU GOT THERE AND STILL CANT PLACE IT, TRY TO FIND ANOTHER ISLAND*/
//                System.out.println("AM I GETTING TO SET ISLAND LOC TO NULL FR");
//                oldIslandLoc = new MapLocation(islandLoc.x,islandLoc.y);
//                for (int i = Communication.STARTING_ISLAND_IDX; i < Communication.STARTING_ISLAND_IDX + 10; i++) {
//                    MapLocation islandNearestLoc = Communication.readIslandLocation(rc, i);
//                    if (islandNearestLoc != null && Communication.readTeamHoldingIsland(rc,i)==Team.NEUTRAL) {
//                        islandLoc = islandNearestLoc;
//                        if (!islandLoc.equals(oldIslandLoc)) {
//                            break;
//                        }
//                    }
//                } /**Make the search through the shared array for islands a method*/
//                //return; /**POTENTIALLY FIXES SOMETHING? THE PROBLEM IS THE CARRIER WITH ANCHOR VALUE ISN'T SET TO 0*/
//            }
//            else{
//                islandLoc=null;
//                System.out.println("I couldn't place the anchor and the island location is set to null to try to find another one");
//                rc.setIndicatorString("I DONT KNOW WHERE THE ISLAND IS RN");
//                return;
//            }
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
            int wellChoice = RobotPlayer.rng.nextInt(wells.length);
            wellLoc = wells[wellChoice].getMapLocation();
            well = wells[wellChoice];
            /**for(int i = 1; i < wells.length; i++) {
             if(rc.getLocation().distanceSquaredTo(wellLoc) > rc.getLocation().distanceSquaredTo(wells[i].getMapLocation())) {
             wellLoc = wells[i].getMapLocation();
             well = wells[i];
             }
             }*/ //*Note! getMapLocation is for wells, getLocation is for robots*/
        }/**Also, updates well so we actually get a well info object*/
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
//        int[] ids = rc.senseNearbyIslands();
//        int j=0;
//        for(int i = 0; i<ids.length; i++) {
//            Communication.updateIslandInfo(rc, ids[i]);
//            if (rc.senseTeamOccupyingIsland(ids[i]) == Team.NEUTRAL) {
//                islandLoc = rc.senseNearbyIslandLocations(ids[i])[0];
//                j = i;
//                break; /**Should hopefully find a island. If there's more than one, the next part will start from the index it left off at and see if it can find a closer one**/
//            }
//        }
//        for(int k = j; k<ids.length; k++) {
//            Communication.updateIslandInfo(rc, ids[k]);
//            if (rc.senseTeamOccupyingIsland(ids[k]) == Team.NEUTRAL && rc.getLocation().distanceSquaredTo(islandLoc) > rc.getLocation().distanceSquaredTo(rc.senseNearbyIslandLocations(ids[k])[0])) {
//                islandLoc = rc.senseNearbyIslandLocations(ids[k])[0];
//            } /**This keeps going to see if any of the other islands are closer**/
//        }/**IMPROVEMENT SEAN MADE: WILL CHOSE THE NEAREST ISLAND**/
        int[] ids = rc.senseNearbyIslands();
        for(int id : ids) {
            System.out.println("THE AVAILABLE ISLANDS ARE " + id);
            if(rc.senseTeamOccupyingIsland(id) == Team.NEUTRAL) {
                MapLocation[] locs = rc.senseNearbyIslandLocations(id);
                if(locs.length > 0) {
                    islandLoc = locs[0];
                    islandLocIndex = id; /**TRYING TO GET IT TO SAVE THE ID OF THE ISLAND, IDK IF THIS IS HOW TO DO IT. WILL ALSO NEED TO DO THIS IN ANYTHING YOU COMMENTED OUT THAT YOU PLAN TO USE*/
                    break;
                }
            }
            Communication.updateIslandInfo(rc, id);
        }
    }
}