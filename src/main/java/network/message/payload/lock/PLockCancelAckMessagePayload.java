package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

import java.io.Serial;
import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;

public class PLockCancelAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = 2054952907416124030L;
    public int rtnSeqNo;

    public PLockCancelAckMessagePayload(
        int epochNo, List<String> devID, String rtnID, int rtnSeqNo, int srcSeqNo
    ) {
        super(epochNo, KGroupType.DEVICE, devID, srcSeqNo);
        this.rtnID = rtnID;
        this.rtnSeqNo = rtnSeqNo;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}
