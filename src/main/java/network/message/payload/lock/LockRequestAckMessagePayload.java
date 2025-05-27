package network.message.payload.lock;

import java.io.Serial;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class LockRequestAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = -6501728481760721880L;

    public LockRequestAckMessagePayload(int epochNo, String rtnID, int srcSeqNo) {
        super(epochNo, KGroupType.ROUTINE, rtnID, srcSeqNo);
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}
