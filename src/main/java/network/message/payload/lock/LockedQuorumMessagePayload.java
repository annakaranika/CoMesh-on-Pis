package network.message.payload.lock;

import java.io.Serial;
import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class LockedQuorumMessagePayload extends MessagePayload {
    @Serial
    private static final long serialVersionUID = -6325764608392709083L;
    
    public int reqSeqNo, rtnSeqNo;

    public LockedQuorumMessagePayload(int epochNo, List<String> devID, String rtnID, int reqSeqNo, int rtnSeqNo) {
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
            mngr.replicateLockAcquisition(devID, rtnID, rtnSeqNo, reqSeqNo);
        }

        ReplyMessagePayload replyPayload = new LockedQuorumAckMessagePayload(
                        epochNo, entitiesIDs, srcSeqNo, rtnID, rtnSeqNo, reqSeqNo);
        MessageType replyMsgType = MessageType.LOCKED_QUORUM_ACK;
        
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
