package network.message.payload.lock;

import java.io.Serial;
import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class LockRequestMessagePayload extends MessagePayload {
    @Serial
    private static final long serialVersionUID = 8452728320378949730L;
    
    public int rtnSeqNo;

    public LockRequestMessagePayload(int epochNo, List<String> devID, String rtnID, int rtnSeqNo) {
        super(epochNo, KGroupType.DEVICE, devID);
        this.rtnID = rtnID;
        this.rtnSeqNo = rtnSeqNo;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        if (epochNo != mngr.getEpochNo() ||  !mngr.isMemberOfKGroup()) {
            // forward message to new device k-group leader
            return null;
        }
    
        // update the routine k-group's leader so that I can safely reply afterwards
        mngr.getRtnKGrp(rtnID).updateLeader(src);

        ReplyMessagePayload replyPayload;
        MessageType replyMsgType;

        if (!mngr.isLeader()) {
            replyPayload = new DeviceKGroupLeaderInfoMessagePayload(
                                    epochNo, List.of(rtnID), srcSeqNo, src);
            replyMsgType = MessageType.DEVICE_KGROUP_LEADER_INFO;
        }
        else {
            String devID = entitiesIDs.get(0);
            mngr.log("Lock request for device " + devID + " by routine " + rtnID
                    + "-" + rtnSeqNo + " received");
            replyPayload = new LockRequestAckMessagePayload(epochNo, rtnID, srcSeqNo);
            replyMsgType = MessageType.LOCK_REQUEST_ACK;
            int reqSeqNo;

            // if this request has not been received before
            // meaning if it is not in newLockRequests/queue
            if (!mngr.isLockRequestReceived(devID, rtnID, rtnSeqNo)) {
                reqSeqNo = mngr.addNewLockRequest(devID, rtnID, rtnSeqNo);
                if (reqSeqNo == -1) {
                    return null;
                }
                mngr.gatherQuorumForDevLockRequest(rtnID, rtnSeqNo, devID, reqSeqNo);
            }
            // if this request has already been received before
            else {
                reqSeqNo = mngr.getLockReqSeqNo(devID, rtnID, rtnSeqNo);
                if (reqSeqNo == -1) {
                    return null;
                }
                // if this lock referred to by this request has already been granted before
                // meaning if it is the current locker
                if (mngr.isLockAcquired(devID, rtnID, rtnSeqNo)) {
                    mngr.remoteCall(KGroupType.ROUTINE, rtnID, MessageType.LOCKED,
                        new LockedMessagePayload(
                            epochNo, List.of(rtnID), devID, rtnSeqNo
                        ), false
                    );
                }
                // if lock request has been granted
                else if (mngr.isLockRequestGranted(devID, rtnID, rtnSeqNo)) {
                    mngr.remoteCall(KGroupType.ROUTINE, rtnID, MessageType.LOCK_REQUESTED,
                        new LockRequestedMessagePayload(
                            epochNo, List.of(rtnID), rtnSeqNo, devID),
                        false
                    );
                }
                // if lock request has been received but not granted
                else {
                    mngr.gatherQuorumForDevLockRequest(rtnID, rtnSeqNo, devID, reqSeqNo);
                }
            }
            mngr.log("Device " + devID + " state after: " + mngr.getState().get(devID));
        }
        
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
