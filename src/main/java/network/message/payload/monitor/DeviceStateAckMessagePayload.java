package network.message.payload.monitor;

import java.io.Serial;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class DeviceStateAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = 8883248266329096748L;

    public DeviceStateAckMessagePayload(String devID, int srcSeqNo) {
        super(KGroupType.DEVICE, devID, srcSeqNo);
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }

}
