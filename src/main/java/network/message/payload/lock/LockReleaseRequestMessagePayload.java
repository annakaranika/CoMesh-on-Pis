package network.message.payload.lock;

import java.io.Serial;
import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class LockReleaseRequestMessagePayload extends MessagePayload {
    @Serial
    private static final long serialVersionUID = 3631481029651092528L;
    
    public int rtnSeqNo;

    public LockReleaseRequestMessagePayload(int epochNo, List<String> devID, String rtnID, int rtnSeqNo) {
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
            mngr.log("Device " + devID + " k-group: lock release requested by routine "
                    + rtnID + "-" + rtnSeqNo);
            replyPayload = new LockReleaseRequestAckMessagePayload(epochNo, List.of(rtnID), srcSeqNo);
            replyMsgType = MessageType.LOCK_RELEASE_REQUEST_ACK;
            if (mngr.isLocked(devID) && mngr.isLocker(devID, rtnID, rtnSeqNo)) {
                int reqSeqNo = mngr.getLockerReqSeqNo(devID);
                mngr.gatherQuorumForDevLockRelease(rtnID, rtnSeqNo, devID, reqSeqNo);
            }
        }
        
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
