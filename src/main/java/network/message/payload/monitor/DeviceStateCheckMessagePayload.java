package network.message.payload.monitor;

import java.io.Serial;

import deploy.Deployer;
import deploy.KGroupManager;
import javafx.util.Pair;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class DeviceStateCheckMessagePayload extends MessagePayload {
    @Serial
    private static final long serialVersionUID = 7447602879459978795L;

    public DeviceStateCheckMessagePayload() {}

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(
        KGroupManager mngr, String src, int srcSeqNo
    ) {
        MessageType replyMsgType = MessageType.DEVICE_STATE;
        ReplyMessagePayload replyPayload = new DeviceStateMessagePayload(
            KGroupManager.getMyID(), Deployer.getDevState(), srcSeqNo
        );
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
