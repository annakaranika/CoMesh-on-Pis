package network.message.payload.lock;

import java.io.Serial;
import java.util.List;

import deploy.KGroupManager;
import deploy.UnicastMsgInfo;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class RoutineKGroupLeaderInfoMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = -1694369111580051543L;
    
    public String leaderNodeID;

    public RoutineKGroupLeaderInfoMessagePayload(int epochNo, List<String> devID, int srcSeqNo, String leaderNodeID) {
        super(epochNo, KGroupType.DEVICE, devID, srcSeqNo);
        this.leaderNodeID = leaderNodeID;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int seqNo) {
        if (epochNo != mngr.getEpochNo()) {
            mngr.removeMsgInfo(this.srcSeqNo);
            return null;
        }
        
        UnicastMsgInfo msgInfo = mngr.getUnicastMsgInfo(this.srcSeqNo);
        if (msgInfo != null) {
            mngr.updateLeader(leaderNodeID);
            msgInfo.updateDestination(leaderNodeID);
        }
        
        return null;
    }

    @Override
    public String toString() {
        return "ROUTINE_KGROUP_LEADER_INFO " + super.toString();
    }
}
