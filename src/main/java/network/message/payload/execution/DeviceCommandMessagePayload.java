package network.message.payload.execution;

import java.io.Serial;

import deploy.Deployer;
import deploy.DeviceState;
import deploy.KGroupManager;
import javafx.util.Pair;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class DeviceCommandMessagePayload extends MessagePayload {
    @Serial
    private static final long serialVersionUID = 7447662879459978795L;
    private final DeviceState newState;

    public DeviceCommandMessagePayload(DeviceState newState, String rtnID) {
        this.newState = newState;
        this.rtnID = rtnID;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(
        KGroupManager mngr, String src, int srcSeqNo
    ) {
        if (newState != null) Deployer.updateDevState(newState);

        MessageType replyMsgType = MessageType.DEVICE_COMMAND_ACK;
        ReplyMessagePayload replyPayload = new DeviceCommandAckMessagePayload(rtnID, srcSeqNo);
        
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
