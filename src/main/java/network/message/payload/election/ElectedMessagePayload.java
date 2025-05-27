package network.message.payload.election;

import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import kgroup.LeaderElectionCause;
import kgroup.LeaderElectionStage;
import network.Network;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class ElectedMessagePayload extends MessagePayload {
    private static final long serialVersionUID = 4094035504829395065L;
    
    public LeaderElectionCause cause;

    public ElectedMessagePayload(int epochNo, KGroupType kGroupType, List<String> entitiesIDs, LeaderElectionCause cause) {
        super(epochNo, kGroupType, entitiesIDs);
        this.cause = cause;
    }

    public ElectedMessagePayload(KGroupManager mngr, LeaderElectionCause cause) {
        super(mngr);
        this.cause = cause;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        String leaderNodeID = null;
        if (mngr.getEpochNo() > epochNo || !mngr.isMemberOfKGroup()) {
            return null;
        }
        else if (mngr.getEpochNo() < epochNo) {
            mngr.runIncEpochTask();
        }
        else if (mngr.getEpochNo() == epochNo && mngr.isLdrElctnStg(LeaderElectionStage.COMPLETE)) {
            leaderNodeID = mngr.getLeader();
        }

        if (cause == LeaderElectionCause.LEADER_FAILURE) {
            if (mngr.getLeader() != null)
                mngr.nodeFailed(mngr.getLeader());
            mngr.log("\n\tDEBUG 3\n");
        }

        if (!Network.getMyID().equals(src)) {
            // update leader
            mngr.updateLeader(src);
            mngr.advLdrElctnStg(LeaderElectionStage.COMPLETE);
            // mngr.advStateTrnsfrStg(StateTransferStage.COMPLETE); // this does not make sense! is it for the receiving end to open?
            mngr.openReceivingEnd();
            mngr.log("Elected node");
        }

        ReplyMessagePayload replyPayload = new ElectedAckMessagePayload(mngr, srcSeqNo, leaderNodeID);
        MessageType replyMsgType = MessageType.ELECTED_ACK;
        
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
