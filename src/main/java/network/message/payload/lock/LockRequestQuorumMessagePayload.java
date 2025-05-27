package network.message.payload.lock;

import java.io.Serial;
import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class LockRequestQuorumMessagePayload extends MessagePayload {
    @Serial
    private static final long serialVersionUID = -9127445782736958486L;
    
    public int reqSeqNo, rtnSeqNo;

    public LockRequestQuorumMessagePayload(
        int epochNo, List<String> devID, String rtnID, int rtnSeqNo, int reqSeqNo
    ) {
        super(epochNo, KGroupType.DEVICE, devID);
        this.rtnID = rtnID;
        this.reqSeqNo = reqSeqNo;
        this.rtnSeqNo = rtnSeqNo;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        if (epochNo != mngr.getEpochNo() || !mngr.isMemberOfKGroup())
            return null;

        if (!mngr.isLeader()) {
            String devID = entitiesIDs.get(0);
            mngr.replicateNewLockRequest(devID, rtnID, rtnSeqNo, reqSeqNo);
        }

        ReplyMessagePayload replyPayload = new LockRequestQuorumAckMessagePayload(
                        epochNo, entitiesIDs, srcSeqNo, rtnID, reqSeqNo, rtnSeqNo);
        MessageType replyMsgType = MessageType.LOCK_REQUEST_QUORUM_ACK;
        
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
