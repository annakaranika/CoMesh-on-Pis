package network.message.payload.stateTransfer;

import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import kgroup.LeaderElectionCause;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class KGroupStateRequestMessagePayload extends MessagePayload {
    private static final long serialVersionUID = -4554161628775213371L;
    
    public LeaderElectionCause cause;

    public KGroupStateRequestMessagePayload(int epochNo, KGroupType kGroupType, List<String> entitiesIDs, LeaderElectionCause cause) {
        super(epochNo, kGroupType, entitiesIDs);
        this.cause = cause;
    }

    public KGroupStateRequestMessagePayload(KGroupManager mngr, LeaderElectionCause cause) {
        super(mngr);
        this.cause = cause;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        if (mngr.getEpochNo() != epochNo) {
            return null;
        }

        ReplyMessagePayload replyPayload;
        MessageType replyMsgType;
        mngr.updateLeader(src);
        if (mngr.isOldLeaderNull()) {
            replyPayload = new NotOldLeaderMessagePayload(mngr, srcSeqNo);
            replyMsgType = MessageType.NOT_OLD_LEADER;
        }
        else if (!mngr.isOldLeader()) {
            replyPayload = new NotOldLeaderMessagePayload(mngr, srcSeqNo);
            replyMsgType = MessageType.NOT_OLD_LEADER;
        }
        else {
            replyPayload = new KGroupStateMessagePayload(mngr, srcSeqNo);
            replyMsgType = MessageType.KGROUP_STATE;
        }
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
