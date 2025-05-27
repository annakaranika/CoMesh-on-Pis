package network.message.payload.monitor;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

import deploy.Deployer;
import deploy.DeviceState;
import deploy.KGroupManager;
import deploy.UnicastMsgInfo;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.Network;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class DeviceStateMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = 8883248266329096745L;
    
    private DeviceState state;
    
    public DeviceStateMessagePayload(String devID, DeviceState state, int srcSeqNo) {
        super(KGroupType.DEVICE, devID, srcSeqNo);
        this.state = state;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(
        KGroupManager mngr, String src, int srcSeqNo
    ) {
        if (!mngr.isMemberOfKGroup()) {
            mngr.log("not part of k group!!!, " + srcSeqNo + " from " + src);
            mngr.removeMsgInfo(this.srcSeqNo);
            return null;
        }

        UnicastMsgInfo msgInfo = mngr.getUnicastMsgInfo(this.srcSeqNo);
        if (msgInfo == null) {
            mngr.log(
                "UnicastMsgInfo not found for DeviceStateRequest to " + src + " and srcSeqNo " +
                this.srcSeqNo
            );
            mngr.removeMsgInfo(this.srcSeqNo);
            return null;
        }

        List<Integer> prevSeqNos = mngr.getUnicastMsgInfoForDevStateReq(src, this.srcSeqNo);
        if (!prevSeqNos.isEmpty()) {
            for (Integer prevSeqNo: prevSeqNos) {
                mngr.removeMsgInfo(prevSeqNo);

            }
        }

        boolean diff;
        if (Objects.equals(src, Network.getMyID())) {
            diff = Deployer.updatePrevDevState(state);
        } else {
            diff = Deployer.updateDevState(src, state);
        }
        mngr.log("State updated for device " + src + " to " + state + ", diff: " + diff);
        if (diff) {
            MessageType msgType = MessageType.DEVICE_STATE_FW;
            MessagePayload payload;
            List<String> rtnIDs = mngr.getRtnsToNotify(src);
            mngr.log(
                "Informing the k-group leaders of rtns " + rtnIDs +
                " for dev " + src + "'s state update"
            );
            for (String rtnID: rtnIDs) {
                payload = new DeviceStateFwMessagePayload(rtnID, src, state);
                mngr.remoteCall(KGroupType.ROUTINE, rtnID, msgType, payload, false);
            }
        }

        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}