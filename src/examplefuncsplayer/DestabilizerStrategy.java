package examplefuncsplayer;

import battlecode.common.*;
import java.util.*;

public class DestabilizerStrategy {
    static List<MapLocation> islandLocs = new ArrayList<MapLocation>();
    static List<Integer> numOfDes = new ArrayList<Integer>();
    static MapLocation islandLoc;



    //The idea I am trying to implement behind the Destabilizer Strategy is to essentially hover islands so when enemy
    //robots try to go near it, they will be weakened or damaged.

    static void runDestabilizer(RobotController rc) throws GameActionException {
        scanIslands(rc);
        // iterates through the list to check whether the numofDes is 0. If so, move towards it! Else, move on to the
        // next island!
        for (int i = 0; i < islandLocs.size(); i++) {
            islandLoc = islandLocs.get(i);
            checkDesAtIslands(rc);
            if (numOfDes.get(i) == 0)
                RobotPlayer.moveTowards(rc, islandLoc);
            if (numOfDes.get(i) == null) {
                RobotPlayer.moveRandom(rc);
            }
        }
    }

    static void checkDesAtIslands(RobotController rc) throws GameActionException {
        for (int i = 0; i < islandLocs.size(); i++) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            if ((rc.canSenseRobotAtLocation(islandLocs.get(i))) && (rc.senseRobotAtLocation(islandLocs.get(i)).getType() == RobotType.DESTABILIZER)) {
                if (numOfDes.get(i) == null) {
                    numOfDes.add(1);
                }
            }

            // not sure how to implement the idea of checking the number of destabilizers at a certain location.
        }
    }

    // scans nearby islands and adds the location at 0th index to a list for each island.
    static void scanIslands(RobotController rc) throws GameActionException {
        int[] ids = rc.senseNearbyIslands();
        for(int id: ids) {
            MapLocation[] locs = rc.senseNearbyIslandLocations(id);
            if (locs.length > 0) {
                islandLocs.add(locs[0]);
            }
        }
    }

}
