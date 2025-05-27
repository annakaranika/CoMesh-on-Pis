package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

import java.io.Serial;

import deploy.KGroupManager;
import javafx.util.Pair;

public class LockedAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = -9066647417477343542L;

    public LockedAckMessagePayload(int epochNo, String devID, int srcSeqNo) {
        super(epochNo, KGroupType.DEVICE, devID, srcSeqNo);
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}
