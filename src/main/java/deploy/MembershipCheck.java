package deploy;

import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.Map.Entry;

import kgroup.KGroupType;
import network.Membership;
import network.Network;

public class MembershipCheck extends TimerTask {
    public void run() {
        try {
            Map<String, Membership> membershipList = Network.getMembershipList();

            for (Entry<String, Membership> entry: membershipList.entrySet()) {
                String nodeID = entry.getKey();
                Membership status = entry.getValue();

                if (status == Membership.FAILED) {
                    Network.log("Node " + nodeID + "'s failure detected");
                    List<KGroupManager> mngrs = Deployer.getAffectedManagers(nodeID);
                    boolean rtnKGrp = false, devKGrp = false;
                    for (KGroupManager mngr: mngrs) {
                        if (!rtnKGrp && mngr.isType(KGroupType.ROUTINE)) {
                            mngr.nodeFailureDetected(nodeID);
                            rtnKGrp = true;
                        }
                        else if (!devKGrp && mngr.isType(KGroupType.DEVICE)) {
                            mngr.nodeFailureDetected(nodeID);
                            devKGrp = true;
                        }
                        else {
                            mngr.handleNodeFailure(nodeID);
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            Network.log("Membership check timer task exception", true);
            e.printStackTrace();
        }
    }
}