package network.message.payload.routineStage;

import deploy.KGroupManager;
import deploy.QuorumMsgInfo;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.Network;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class ReleasedLocksAckMessagePayload extends ReplyMessagePayload {
    private static final long serialVersionUID = 4548420034890318714L;

    public ReleasedLocksAckMessagePayload(int epochNo, String rtnID, int srcSeqNo) {
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
            mngr.log("Received new locks released ack (" + msgInfo.getPositiveRepliesCount()
                    + "/" + neededAckCount + " now)");
            if (msgInfo.getPositiveRepliesCount() >= neededAckCount) {
                msgInfo.setApproved(Network.getCurTS());
                String rtnID = entitiesIDs.get(0);
                ReleasedLocksMessagePayload ogPayload =
                    (ReleasedLocksMessagePayload) msgInfo.getMessagePayload();
                int rtnSeqNo = ogPayload.rtnSeqNo;
                mngr.log("Routine " + rtnID + "-" + rtnSeqNo + " locks released replicated");
                mngr.removeRtnStage(rtnID, rtnSeqNo);
                mngr.removeMsgInfo(this.srcSeqNo);
            }
        }
        
        return null;
    }

    @Override
    public String toString() {
        return "RELEASED_LOCKS_ACK " + super.toString();
    }
}
