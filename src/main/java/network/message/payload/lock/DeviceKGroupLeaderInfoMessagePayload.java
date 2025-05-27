package network.message.payload.lock;

import java.io.Serial;
import java.util.List;

import deploy.KGroupManager;
import deploy.UnicastMsgInfo;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class DeviceKGroupLeaderInfoMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = -1694369111580051543L;
    
    public String leaderNodeID;

    public DeviceKGroupLeaderInfoMessagePayload(int epochNo, List<String> rtnID, int srcSeqNo, String leaderNodeID) {
        super(epochNo, KGroupType.ROUTINE, rtnID, srcSeqNo);
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
        return "DEVICE_KGROUP_LEADER_INFO " + super.toString();
    }
}
