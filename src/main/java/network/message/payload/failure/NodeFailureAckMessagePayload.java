package network.message.payload.failure;

import deploy.KGroupManager;
import javafx.util.Pair;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class NodeFailureAckMessagePayload extends ReplyMessagePayload {
    private static final long serialVersionUID = 2467589647387006617L;

    public NodeFailureAckMessagePayload(int srcSeqNo) {
        super(srcSeqNo);
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(
        KGroupManager mngr, String src, int srcSeqNo
    ) {
        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}
