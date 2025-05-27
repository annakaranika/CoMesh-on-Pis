package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

import java.io.Serial;
import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;

public class PLockLockAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = -1692255007522253078L;
    public int rtnSeqNo;

    public PLockLockAckMessagePayload(int epochNo, List<String> rtnID, int srcSeqNo,
                                      String devID, int rtnSeqNo) {
        super(epochNo, KGroupType.ROUTINE, rtnID, srcSeqNo);
        this.devID = devID;
        this.rtnSeqNo = rtnSeqNo;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}
