package network.message.payload.routineStage;

import configs.LockStrategy;
import deploy.Deployer;
import deploy.KGroupManager;
import deploy.QuorumMsgInfo;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.Network;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class TriggerQuorumAckMessagePayload extends ReplyMessagePayload {
    private static final long serialVersionUID = 4548420034890318714L;

    public TriggerQuorumAckMessagePayload(int epochNo, String rtnID, int srcSeqNo) {
        super(epochNo, KGroupType.ROUTINE, rtnID, srcSeqNo);
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int seqNo) {
        if (epochNo != mngr.getEpochNo() || !mngr.isMemberOfKGroup()) {
            mngr.removeMsgInfo(this.srcSeqNo);
            return null;
        }
        
        QuorumMsgInfo msgInfo = mngr.getQuorumMsgInfo(this.srcSeqNo);
        if (msgInfo == null) {
            mngr.log("Msg seq no " + this.srcSeqNo + " does not exist!\n\t" + this);
        }
        if (msgInfo != null && !msgInfo.isApproved()) {
            msgInfo.newReply(src, true);
            final int neededAckCount = mngr.getF() + 1;
            mngr.log("Received new vote for trigger (" + msgInfo.getPositiveRepliesCount()
                    + "/" + neededAckCount + " now)");
            if (msgInfo.getPositiveRepliesCount() >= neededAckCount) {
                msgInfo.setApproved(Network.getCurTS());
                String rtnID = entitiesIDs.get(0);
                int rtnSeqNo = ((TriggerQuorumMessagePayload) msgInfo.getMessagePayload()).rtnSeqNo;
                mngr.getRtnKGrp(rtnID).setRtnTriggered(rtnID, rtnSeqNo);
                if (mngr.isLockStrategy(LockStrategy.SERIAL)) {
                    mngr.requestDevLockForRtn(rtnID, rtnSeqNo);
                } else {
                    mngr.requestDevLockForRtnInParallel(rtnID, rtnSeqNo);
                }
                Deployer.recordTriggerAckTime(rtnID, rtnSeqNo);
                mngr.log("Routine " + rtnID + "-" + rtnSeqNo + " trigger replicated");
                mngr.removeMsgInfo(this.srcSeqNo);
            }
        }
        
        return null;
    }

    @Override
    public String toString() {
        return "TRIGGER_QUORUM_ACK " + super.toString();
    }
}
