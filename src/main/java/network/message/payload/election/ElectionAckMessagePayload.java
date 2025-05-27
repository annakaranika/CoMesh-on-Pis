package network.message.payload.election;

import java.util.List;

import deploy.KGroupManager;
import deploy.UnicastMsgInfo;
import javafx.util.Pair;
import kgroup.KGroupType;
import kgroup.LeaderElectionStage;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class ElectionAckMessagePayload extends ReplyMessagePayload {
    private static final long serialVersionUID = 3917732281659350393L;

    private String leaderNodeID;

    public ElectionAckMessagePayload(
        int epochNo, KGroupType kGroupType, List<String> entitiesIDs, int srcSeqNo, String leaderNodeID
    ) {
        super(epochNo, kGroupType, entitiesIDs, srcSeqNo);
        this.leaderNodeID = leaderNodeID;
    }

    public ElectionAckMessagePayload(KGroupManager mngr, int srcSeqNo, String leaderNodeID) {
        super(mngr, srcSeqNo);
        this.leaderNodeID = leaderNodeID;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        if (!mngr.isMemberOfKGroup()) {
            mngr.removeMsgInfo(this.srcSeqNo);
            return null;
        }

        UnicastMsgInfo msgInfo = mngr.getUnicastMsgInfo(this.srcSeqNo);
        if (msgInfo == null) {
            mngr.removeMsgInfo(this.srcSeqNo);
            return null;
        }

        if (leaderNodeID != null) {
            mngr.updateLeader(leaderNodeID);
            mngr.removeMsgInfo(srcSeqNo);
            mngr.advLdrElctnStg(LeaderElectionStage.COMPLETE);
            mngr.log(
                "Leader election already completed earlier! Leader is " + leaderNodeID
                + ", not me"
            );
        }
        else if (mngr.isLdrElctnStg(LeaderElectionStage.WAITING_ON_ELECTION_ACK)) {
            mngr.advLdrElctnStg(LeaderElectionStage.WAITING_ON_ELECTED);
        }

        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}
