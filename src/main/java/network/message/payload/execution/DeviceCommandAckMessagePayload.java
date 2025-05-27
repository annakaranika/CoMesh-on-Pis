package network.message.payload.execution;

import java.io.Serial;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class DeviceCommandAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = 8883248266329096745L;

    
    public DeviceCommandAckMessagePayload(String rtnID, int srcSeqNo) {
        super(KGroupType.ROUTINE, rtnID, srcSeqNo);
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(
        KGroupManager mngr, String src, int srcSeqNo
    ) {
        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}
