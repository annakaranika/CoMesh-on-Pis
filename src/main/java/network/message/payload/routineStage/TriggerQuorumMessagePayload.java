package network.message.payload.routineStage;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.*;

public class TriggerQuorumMessagePayload extends MessagePayload {
    private static final long serialVersionUID = -710012838803253143L;
    public int rtnSeqNo;

    public TriggerQuorumMessagePayload(int epochNo, String rtnID, int rtnSeqNo) {
        super(epochNo, KGroupType.ROUTINE, rtnID);
        this.rtnSeqNo = rtnSeqNo;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        if (epochNo != mngr.getEpochNo() || !mngr.isMemberOfKGroup())
            return null;

        String rtnID = entitiesIDs.get(0);
        if (!mngr.isLeader()) {
            mngr.setRtnTriggered(rtnID, rtnSeqNo);
        }

        ReplyMessagePayload replyPayload =
            new TriggerQuorumAckMessagePayload(epochNo, rtnID, srcSeqNo);
        MessageType replyMsgType = MessageType.TRIGGER_QUORUM_ACK;
        
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
