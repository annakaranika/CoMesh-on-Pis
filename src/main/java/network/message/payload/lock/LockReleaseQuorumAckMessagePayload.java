package network.message.payload.lock;

import java.io.Serial;
import java.util.List;

import deploy.KGroupManager;
import deploy.QuorumMsgInfo;
import javafx.util.Pair;
import kgroup.KGroupType;
import kgroup.state.LockRequest;
import network.Network;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class LockReleaseQuorumAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = -5205396067179785877L;

    public int reqSeqNo, rtnSeqNo;

    public LockReleaseQuorumAckMessagePayload(int epochNo, List<String> devID, int srcSeqNo, String rtnID, int reqSeqNo, int rtnSeqNo) {
        super(epochNo, KGroupType.DEVICE, devID, srcSeqNo);
        this.rtnID = rtnID;
        this.reqSeqNo = reqSeqNo;
        this.rtnSeqNo = rtnSeqNo;
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
            mngr.log("Received new vote for lock release (" + msgInfo.getPositiveRepliesCount()
                    + "/" + neededAckCount + " now)");
            if (msgInfo.getPositiveRepliesCount() >= neededAckCount) {
                msgInfo.setApproved(Network.getCurTS());
                String devID = entitiesIDs.get(0);
                LockRequest nextRtn = mngr.releaseLock(devID, rtnID, rtnSeqNo, reqSeqNo);
                mngr.log("Device " + devID + " k-group: lock released for routine "
                        + rtnID + "-" + rtnSeqNo);

                mngr.remoteCall(
                    KGroupType.ROUTINE, rtnID, MessageType.LOCK_RELEASED,
                    new LockReleasedMessagePayload(epochNo, List.of(rtnID), devID, rtnSeqNo),
                    true
                );
                if (nextRtn != null && !mngr.isLockAcquired(
                        devID, nextRtn.getRtnID(), nextRtn.getRtnSeqNo())) {
                    mngr.log("Device " + devID +
                            " k-group: gathering quorum for granting lock to routine "
                            + nextRtn);
                    mngr.gatherQuorumForDevLockAcquisition(
                        nextRtn.getRtnID(), nextRtn.getRtnSeqNo(), devID);
                }
                mngr.removeMsgInfo(this.srcSeqNo);
            }
        }
        
        return null;
    }

    @Override
    public String toString() {
        return "LOCK_RELEASE_QUORUM_ACK " + super.toString();
    }
}
