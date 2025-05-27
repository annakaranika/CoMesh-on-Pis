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

public class LockRequestQuorumAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = 8234550092862487011L;
    
    public int reqSeqNo, rtnSeqNo;

    public LockRequestQuorumAckMessagePayload(
        int epochNo, List<String> devID, int srcSeqNo,
        String rtnID, int reqSeqNo, int rtnSeqNo
    ) {
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
            mngr.log("Received new vote for lock request (" + msgInfo.getPositiveRepliesCount()
                    + "/" + neededAckCount + " now)");
            if (msgInfo.getPositiveRepliesCount() >= neededAckCount) {
                msgInfo.setApproved(Network.getCurTS());
                String devID = entitiesIDs.get(0);
                // if the request has been previously acknowledged, do not do it again
                if (!mngr.isLockRequestGranted(devID, rtnID, rtnSeqNo)) {
                    mngr.lockRequestReplicated(devID, rtnID, rtnSeqNo, reqSeqNo);

                    mngr.log("Lock request granted by device " + devID
                            + " to routine " + rtnID + "-" + rtnSeqNo
                           );
                    mngr.log("Device " + devID + " state: " + mngr.getDevState(devID));

                    mngr.remoteCall(KGroupType.ROUTINE, rtnID, MessageType.LOCK_REQUESTED,
                        new LockRequestedMessagePayload(
                            epochNo, List.of(rtnID), rtnSeqNo, devID),
                        false
                    );

                    // if the device lock is free and this is the only request
                    // in the queue, give it to the new request
                    if (!mngr.isLocked(devID) &&
                        mngr.getLastLockedReqSeqNo(devID) + 1 == reqSeqNo) {
                        LockRequest nextRtn = mngr.getLockQueueHead(devID);
                        mngr.gatherQuorumForDevLockAcquisition(
                            nextRtn.getRtnID(), nextRtn.getRtnSeqNo(), devID);
                    }
                }
                mngr.removeMsgInfo(this.srcSeqNo);
            }
        }
        
        return null;
    }

    @Override
    public String toString() {
        return "LOCK_REQUEST_QUORUM_ACK " + super.toString();
    }
}
