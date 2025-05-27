package network.message.payload.routineStage;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.*;

public class ExecutedMessagePayload extends MessagePayload {
    private static final long serialVersionUID = -710072838803253143L;
    public int rtnSeqNo;

    public ExecutedMessagePayload(int epochNo, String rtnID, int rtnSeqNo) {
        super(epochNo, KGroupType.ROUTINE, rtnID);
        this.rtnSeqNo = rtnSeqNo;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        if (epochNo != mngr.getEpochNo() || !mngr.isMemberOfKGroup())
            return null;

        String rtnID = entitiesIDs.get(0);
        if (!mngr.isLeader()) {
            mngr.setRtnExecuted(rtnID, rtnSeqNo);
        }

        ReplyMessagePayload replyPayload =
            new ExecutedAckMessagePayload(epochNo, rtnID, srcSeqNo);
        MessageType replyMsgType = MessageType.EXECUTED_ACK;
        
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
