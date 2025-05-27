package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

import java.io.Serial;
import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;

public class PLockCancelledAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = 1118965686247859190L;

    public PLockCancelledAckMessagePayload(int epochNo, List<String> devID, int srcSeqNo) {
        super(epochNo, KGroupType.DEVICE, devID, srcSeqNo);
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}
