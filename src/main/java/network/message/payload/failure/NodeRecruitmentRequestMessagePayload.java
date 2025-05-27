package network.message.payload.failure;

import java.util.ArrayList;
import java.util.Map;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class NodeRecruitmentRequestMessagePayload extends MessagePayload {
    private static final long serialVersionUID = -2822918732737107307L;
    
    public Map<String, Object> state;

    public NodeRecruitmentRequestMessagePayload(int epochNo, KGroupType kGroupType, Map<String, Object> states) {
        super(epochNo, kGroupType, new ArrayList<String>(states.keySet()));
        this.state = states;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(
        KGroupManager mngr, String src, int srcSeqNo) {
        if (mngr.getEpochNo() > epochNo) return null;
        else if (mngr.getEpochNo() < epochNo)
            mngr.updateKGroup();

        mngr.nodeRecruited();
        mngr.updateLeader(src);
        mngr.setState(state);

        MessageType replyMsgType = MessageType.NODE_RECRUITED;
        ReplyMessagePayload replyPayload = new NodeRecruitedMessagePayload(mngr, srcSeqNo);

        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
