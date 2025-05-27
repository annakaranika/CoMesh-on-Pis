package deploy;

import kgroup.DeviceKGroup;
import kgroup.KGroup;
import kgroup.RoutineKGroup;
import network.message.MessageType;
import network.message.payload.MessagePayload;

public class UnicastMsgInfo extends MsgInfo {
    private String dst;
    private KGroup otherKGroup;
    private boolean acknowledged;

    public UnicastMsgInfo(
        MessageType type, MessagePayload payload, KGroup otherKGroup, String dst,
        KGroupManager mngr, boolean boundedWait, long initialTS, int initialEpoch,
        ResendTask resendTask
    ) {
        super(type, payload, mngr, boundedWait, initialTS, initialEpoch, resendTask);
        this.otherKGroup = otherKGroup;
        this.dst = dst;
        acknowledged = false;
    }

    public String getDestination() {
        return dst;
    }

    public void updateDestination(String dst) {
        this.dst = dst;
    }

    public KGroup getOtherKGroup() {
        return otherKGroup;
    }

    public RoutineKGroup getRtnKGroup() {
        return (RoutineKGroup) otherKGroup;
    }

    public DeviceKGroup getDevKGroup() {
        return (DeviceKGroup) otherKGroup;
    }

    public void setAcknowledged() {
        acknowledged = true;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }
}
