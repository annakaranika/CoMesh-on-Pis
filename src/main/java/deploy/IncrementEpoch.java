package deploy;

import java.util.TimerTask;
import org.apache.commons.lang3.exception.ExceptionUtils;

import kgroup.LeaderElectionCause;
import metrics.NodeRole;
import network.Network;

public class IncrementEpoch extends TimerTask {
    KGroupManager mngr;
    static int cntE = 0, cntS = 0, cntI = 0;

    public IncrementEpoch(KGroupManager mngr) {
        this.mngr = mngr;
        // mngr.log(" created instance " + ++cntI + " of IncrementEpoch");
    }

    private synchronized static boolean checkCntE() {
        ++ cntE;
        if (cntE == Deployer.getKGrpNo()) {
            cntE = 0;
            return true;
        }
        return false;
    }

    private synchronized static boolean checkCntS() {
        cntS ++;
        if (cntS == Deployer.getKGrpNo()) {
            cntS = 0;
            return true;
        }
        return false;
    }

    public void run() {
        try {
            Network.log("Mngr is null!!!", mngr == null);
            mngr.cancelMntrTask();
            mngr.cancelLockReleases();
            int currentEpoch = mngr.incrementEpoch();
            mngr.log("increased the epoch to " + currentEpoch + "!");
            if (checkCntE()) {
                Network.log("All groups increased the epoch to " + currentEpoch + "!");
            }
            
            if (!mngr.isNodeOnline()) {
                mngr.log(" - epoch: " + currentEpoch + " 1 " + mngr.isNodeOnline());
                return;
            }

            // updating all k-group members and moving on to the leader election phase
            mngr.updateKGroup();
            // mngr.log(" - epoch: " + currentEpoch + " 2");
            if (checkCntS()) {
                Network.log("All groups' members updated for epoch " + currentEpoch + "!");
            }

            if (mngr.isMemberOfKGroup()) {
                // mngr.log(" - epoch: " + currentEpoch + " 3");
                mngr.leaderElection(LeaderElectionCause.NEW_EPOCH);
                switch (mngr.getType()) {
                case DEVICE:
                    Deployer.startDevRoles(NodeRole.MEMBER, mngr.getMonitored());
                    break;
                case ROUTINE:
                    Deployer.startRtnRoles(NodeRole.MEMBER, mngr.getMonitored());
                }
            }
            mngr.log("Finished change to epoch " + currentEpoch);
        }
        catch (Exception e) {
            Network.log("Increment epoch timer task exception", true);
            Network.log(ExceptionUtils.getStackTrace(e), true);
        }
    }
}
