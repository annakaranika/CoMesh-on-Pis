package network.message.payload.election;

import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import kgroup.LeaderElectionCause;
import kgroup.LeaderElectionStage;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class ElectionMessagePayload extends MessagePayload {
    private static final long serialVersionUID = -5070340407181689772L;
    
    public LeaderElectionCause cause;

    public ElectionMessagePayload(int epochNo, KGroupType kGroupType, List<String> entitiesIDs, LeaderElectionCause cause) {
        super(epochNo, kGroupType, entitiesIDs);
        this.cause = cause;
    }

    public ElectionMessagePayload(KGroupManager mngr, LeaderElectionCause cause) {
        super(mngr);
        this.cause = cause;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        String leaderNodeID = null;
        if (mngr.getEpochNo() > epochNo || !mngr.isMemberOfKGroup()) {
            return null;
        }
        else if (mngr.getEpochNo() == epochNo && mngr.isLdrElctnStg(LeaderElectionStage.COMPLETE)) {
            leaderNodeID = mngr.getLeader();
        }
        else if (mngr.getEpochNo() < epochNo) {
            mngr.runIncEpochTask();
        }

        MessageType replyMsgType = MessageType.ELECTION_ACK;
        ReplyMessagePayload replyPayload = new ElectionAckMessagePayload(
            epochNo, mngr.getType(), mngr.getMonitored(), srcSeqNo, leaderNodeID);

        if (mngr.getLdrElctnStg() == LeaderElectionStage.NOT_STARTED) {
            mngr.leaderElection(cause);
        }
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
