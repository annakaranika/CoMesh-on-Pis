package network.message.payload.failure;

import java.util.List;

import deploy.KGroupManager;
import deploy.UnicastMsgInfo;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class NodeRecruitedMessagePayload extends ReplyMessagePayload {
    private static final long serialVersionUID = -5798654073094211382L;

    public NodeRecruitedMessagePayload(
        int epochNo, KGroupType kGroupType, List<String> entitiesIDs, int srcSeqNo
    ) {
        super(epochNo, kGroupType, entitiesIDs, srcSeqNo);
    }

    public NodeRecruitedMessagePayload(KGroupManager mngr, int srcSeqNo) {
        super(mngr, srcSeqNo);
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
        
        mngr.nodeRecruited(src);
        
        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}
