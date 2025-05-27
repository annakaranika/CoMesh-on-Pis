package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

import java.io.Serial;

import deploy.KGroupManager;
import javafx.util.Pair;

public class PLockRequestAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = -2108963766272582011L;
    public int rtnSeqNo;

    public PLockRequestAckMessagePayload(
            int epochNo, String rtnID, int rtnSeqNo, int srcSeqNo, String devID) {
        super(epochNo, KGroupType.ROUTINE, rtnID, srcSeqNo);
        this.rtnSeqNo = rtnSeqNo;
        this.devID = devID;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}
