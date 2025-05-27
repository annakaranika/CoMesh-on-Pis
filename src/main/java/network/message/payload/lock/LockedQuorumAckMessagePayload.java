package network.message.payload.lock;

import java.io.Serial;
import java.util.List;

import deploy.KGroupManager;
import deploy.QuorumMsgInfo;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.Network;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class LockedQuorumAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = 2704514377346524282L;
    public int reqSeqNo, rtnSeqNo;

    public LockedQuorumAckMessagePayload(
        int epochNo, List<String> devID, int srcSeqNo, String rtnID, int rtnSeqNo, int reqSeqNo
    ) {
        super(epochNo, KGroupType.DEVICE, devID, srcSeqNo);
        this.rtnID = rtnID;
        this.rtnSeqNo = rtnSeqNo;
        this.reqSeqNo = reqSeqNo;
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
            mngr.log("Received new vote for lock request (" + msgInfo.getPositiveRepliesCount()
                    + "/" + neededAckCount + " now)");
            if (msgInfo.getPositiveRepliesCount() >= neededAckCount) {
                msgInfo.setApproved(Network.getCurTS());
                String devID = entitiesIDs.get(0);
                if (!mngr.isLockAcquired(devID, rtnID, rtnSeqNo)) {
                    mngr.lock(devID, rtnID, rtnSeqNo, reqSeqNo);

                    mngr.log("Device " + devID + " granted lock to routine "
                             + rtnID + "-" + rtnSeqNo);
                    mngr.log("Device " + devID + " state: " + mngr.getDevState(devID));

                    MessagePayload payload = new LockedMessagePayload(
                        epochNo, List.of(rtnID), devID, rtnSeqNo);
                    mngr.remoteCall(KGroupType.ROUTINE, rtnID,
                        MessageType.LOCKED, payload, false);
                }
                mngr.removeMsgInfo(this.srcSeqNo);
            }
        }
        
        return null;
    }

    @Override
    public String toString() {
        return "LOCKED_QUORUM_ACK " + super.toString();
    }
}
