package network.message.payload.routineStage;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.*;

public class ReleasedLocksMessagePayload extends MessagePayload {
    private static final long serialVersionUID = -710072838803293143L;
    public int rtnSeqNo;

    public ReleasedLocksMessagePayload(int epochNo, String rtnID, int rtnSeqNo) {
        super(epochNo, KGroupType.ROUTINE, rtnID);
        this.rtnSeqNo = rtnSeqNo;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        if (epochNo != mngr.getEpochNo() || !mngr.isMemberOfKGroup())
            return null;

        String rtnID = entitiesIDs.get(0);
        if (!mngr.isLeader()) {
            mngr.removeRtnStage(rtnID, rtnSeqNo);
        }

        ReplyMessagePayload replyPayload =
            new ReleasedLocksAckMessagePayload(epochNo, rtnID, srcSeqNo);
        MessageType replyMsgType = MessageType.RELEASED_LOCKS_ACK;
        
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
