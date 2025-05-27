package deploy;

import java.util.List;

import network.Network;

public class QuorumResend extends ResendTask {
    
    public QuorumResend(KGroupManager mngr, int seqNo) {
       super(mngr, seqNo);
    }

    public void run() {
        try {
            QuorumMsgInfo msgInfo = getQuorumMsgInfo();
            if (msgInfo == null) {
                cancel();
                return;
            }
            if (msgInfo.isApproved()) {
                mngr.removeMsgInfo(seqNo);
            }

            List<String> targets = msgInfo.toOldKGroup()?
                                mngr.getOldMemberIDs(): mngr.getCurMemberIDs();
            long timePassed = getIntervalMillis(msgInfo.initialTS, Network.getCurTS());
            if (timePassed < 1000) {}
            else if (!msgInfo.isWaitBounded() || timePassed < 2 * mngr.getResendTO(targets)) {
                if (mngr.getEpochNo() == msgInfo.initialEpoch) {
                    Network.log("Resending " + msgInfo.getMessageType() + " msg (initialSendTS "
                            + msgInfo.getInitialTS() + ")"
                            + ", initial epoch: " + msgInfo.initialEpoch
                            + " about k-group " + msgInfo.getKGroup()
                            + ", already replied: " + msgInfo.getRepliedNodesIDs()
                    );
                    mngr.multicast(seqNo, msgInfo);
                    ResendTask resendTask = mngr.scheduleQuorumResend(seqNo, targets);
                    msgInfo.updateLastTS(Network.getCurTS());
                    msgInfo.updateResendTask(resendTask);
                }
            }
            else {
                mngr.log("Not resending " + msgInfo);
                mngr.removeMsgInfo(seqNo);
            }
        }
        catch (Exception e) {
            Network.log("Quorum resend timer task exception", true);
            e.printStackTrace();
        }
    }
}