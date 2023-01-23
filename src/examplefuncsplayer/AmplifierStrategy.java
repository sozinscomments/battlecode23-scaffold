package examplefuncsplayer;

import battlecode.common.*;

public class AmplifierStrategy {
    // not yet called in RobotPlayer
    static void runAmplifier(RobotController rc) throws GameActionException{
        /*
        have amplifiers follow the mob led by the carrier with the anchor/lowest id
         */
        int carrierWithAnchor = rc.readSharedArray(Communication.CARRIER_WITH_ANCHOR_IDX);
        RobotInfo[] allies = rc.senseNearbyRobots(9, rc.getTeam());

        if (carrierWithAnchor != 0) { /**SHOULD THIS BE NULL??????? IDK*/
            for (RobotInfo ally : allies){
                if(carrierWithAnchor == ally.getID()){
                    Pathing.moveTowards(rc,ally.getLocation());
                }
            }
        }
        else {
            int lowestID = rc.getID();
            MapLocation leaderPos = null;
            for (RobotInfo ally : allies) {
                if (ally.getType() != RobotType.LAUNCHER)
                    continue;
                if (ally.getID() < lowestID) {
                    lowestID = ally.getID();
                    leaderPos = ally.getLocation();
                }
            }
            if (leaderPos != null) {
                Pathing.moveTowards(rc, leaderPos);
                rc.setIndicatorString("Following " + lowestID);
            } else {
                Pathing.moveRandomNoBacktrack(rc);
                rc.setIndicatorString("I'm the leader!");
            }
        }
    }
}
