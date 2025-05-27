package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

import java.io.Serial;
import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;

public class PLockRequestedAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = -2849547850329886131L;

    public PLockRequestedAckMessagePayload(int epochNo, List<String> devID, int srcSeqNo,
                                           String rtnID, int rtnSeqNo) {
        super(epochNo, KGroupType.DEVICE, devID, srcSeqNo);
        this.rtnID = rtnID;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}
