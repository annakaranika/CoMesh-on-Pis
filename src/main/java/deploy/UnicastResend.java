package deploy;

import kgroup.KGroup;
import kgroup.LeaderElectionStage;
import metrics.KGroupMetric;
import network.Network;
import network.message.MessageType;
import network.message.payload.election.ElectedMessagePayload;
import network.message.payload.election.ElectionMessagePayload;
import network.message.payload.stateTransfer.LocalKGroupStateRequestMessagePayload;

public class UnicastResend extends ResendTask {
    
    public UnicastResend(KGroupManager mngr, int seqNo) {
       super(mngr, seqNo);
    }

    public void run() {
        try {
            UnicastMsgInfo msgInfo = getUnicastMsgInfo();
            if (msgInfo == null) {
                cancel();
                return;
            }
            if (msgInfo.isAcknowledged()) {
                mngr.removeMsgInfo(seqNo);
            }

            long timePassed = getIntervalMillis(msgInfo.initialTS, Network.getCurTS());
            if (timePassed < 1000) {}
            if (!msgInfo.isWaitBounded() || timePassed < 4. / 3. * mngr.getResendTO(msgInfo.getDestination())) {
                if (mngr.getEpochNo() == msgInfo.initialEpoch) {
                    mngr.log(KGroupManager.getMyID() + ": Resending "
                        + msgInfo.getMessageType() + " msg to " + msgInfo.getDestination()
                        + " (initialSendTS " + msgInfo.getInitialTS() + ")"
                        + ", initial epoch: " + msgInfo.initialEpoch + " about k-groups: "
                        + mngr.kgroup + ", " + msgInfo.getOtherKGroup());
                    mngr.unicast(seqNo, msgInfo);
                    ResendTask newResendTask = mngr.scheduleUnicastResend(seqNo, msgInfo.getDestination());
                    msgInfo.updateLastTS(Network.getCurTS());
                    msgInfo.updateResendTask(newResendTask);
                }
                else {
                    KGroup otherKgrp = msgInfo.getOtherKGroup();
                    if (otherKgrp != null) {
                        mngr.log("Resending " + msgInfo.getMessagePayload() + " to new destination");
                        String newDst = mngr.getMostProbableLeader();
                        mngr.unicast(newDst, msgInfo.getMessageType(), msgInfo.getMessagePayload(), seqNo);
                        ResendTask newResendTask = mngr.scheduleUnicastResend(seqNo, newDst);
                        msgInfo.updateLastTS(Network.getCurTS());
                        msgInfo.updateDestination(newDst);
                        msgInfo.updateResendTask(newResendTask);
                    }
                }
            }
            else {
                if (msgInfo.isMsgType(MessageType.ELECTION) &&
                    mngr.getEpochNo() == msgInfo.initialEpoch) {
                    if (mngr.isLdrElctnStg(LeaderElectionStage.WAITING_ON_ELECTION_ACK)) {
                        mngr.log("Becoming bully. Time passed since sending ELECTION msg: " + timePassed
                                + "\n\tinitial ts: " + msgInfo.initialTS
                                + ", current ts: " + Network.getCurTS());
                        int newSeqNo = mngr.quorum(MessageType.ELECTED, new ElectedMessagePayload(
                            mngr, ((ElectionMessagePayload) msgInfo.getMessagePayload()).cause),
                            false, false); // true);
                        KGroupMetric.addLdrElctnSeqNo(mngr, seqNo, newSeqNo);
                    }
                    else if (mngr.isLdrElctnStg(LeaderElectionStage.WAITING_ON_ELECTED)) {
                        if (timePassed > 8./3. * mngr.getResendTO(msgInfo.getDestination())) {
                            mngr.leaderElection(((ElectionMessagePayload) msgInfo
                                    .getMessagePayload()).cause
                            );
                            mngr.removeMsgInfo(seqNo);
                        }
                    }
                    else {
                        mngr.removeMsgInfo(seqNo);
                    }
                }
                else if (msgInfo.isMsgType(MessageType.KGROUP_STATE_REQUEST) &&
                        mngr.getEpochNo() == msgInfo.initialEpoch) {
                    int newSeqNo = mngr.quorum(MessageType.LOCAL_KGROUP_STATE_REQUEST,
                        new LocalKGroupStateRequestMessagePayload(mngr), true, false);
                    KGroupMetric.addStateTrnsfrSeqNo(mngr, seqNo, newSeqNo);
                }
                else {
                    mngr.removeMsgInfo(seqNo);
                }
            }
        }
        catch (Exception e) {
            Network.log("Unicast resend timer task exception", true);
            e.printStackTrace();
        }
    }
}
