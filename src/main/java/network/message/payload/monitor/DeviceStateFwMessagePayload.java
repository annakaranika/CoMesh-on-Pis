package network.message.payload.monitor;

import java.io.Serial;

import deploy.Deployer;
import deploy.DeviceState;
import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class DeviceStateFwMessagePayload extends MessagePayload {
    @Serial
    private static final long serialVersionUID = 8883248266329086745L;

    private DeviceState state;

    public DeviceStateFwMessagePayload(String rtnID, String devID, DeviceState state) {
        super(KGroupType.ROUTINE, rtnID);
        this.devID = devID;
        this.state = state;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        Deployer.updateDevState(devID, state);

        String rtnID = entitiesIDs.get(0);
        mngr.log("Rtn " + rtnID + ": device " + devID + " state changed to " + state
                + "\n\tnew relevant states: " + mngr.getDevStates(rtnID));

        if (mngr.isRtnTriggered(rtnID)) {
            mngr.log("Rtn " + rtnID + " check trigger condition");
            int rtnSeqNo = mngr.setRtnTriggered(rtnID);
            if (rtnSeqNo > -1) {
                mngr.log("Rtn " + rtnID + " triggered", true);
                mngr.gatherQuorumForRtnTrigger(rtnID, rtnSeqNo);
            }
        }

        MessageType replyType = MessageType.DEVICE_STATE_ACK;
        ReplyMessagePayload replyPayload = new DeviceStateAckMessagePayload(devID, srcSeqNo);
        return new Pair<MessageType, ReplyMessagePayload>(replyType, replyPayload);
    }
}
