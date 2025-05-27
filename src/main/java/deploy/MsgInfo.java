package deploy;

import network.message.MessageType;
import network.message.payload.MessagePayload;

public class MsgInfo {
    protected boolean boundedWait;
    protected int initialEpoch;
    protected long initialTS, lastTS;
    protected KGroupManager mngr;
    protected MessageType type;
    protected MessagePayload payload;
    protected ResendTask resendTask;

    public MsgInfo(
        MessageType type, MessagePayload payload, KGroupManager mngr,
        boolean boundedWait, long initialTS, int initialEpoch, ResendTask resendTask
    ) {
        this.type = type;
        this.payload = payload;
        this.mngr = mngr;
        this.boundedWait = boundedWait;
        this.initialTS = this.lastTS = initialTS;
        this.initialEpoch = initialEpoch;
        this.resendTask = resendTask;
    }

    public MessageType getMessageType() {
        return type;
    }

    public boolean isMsgType(MessageType type) {
        return this.type == type;
    }

    public MessagePayload getMessagePayload() {
        return payload;
    }

    public boolean isWaitBounded() {
        return boundedWait;
    }

    public long getInitialTS() {
        return initialTS;
    }

    public void updateLastTS(long ts) {
        lastTS = ts;
    }

    public void cancelResendTask() {
        if (resendTask != null) resendTask.cancel();
    }

    public void updateResendTask(ResendTask newResendTask) {
        resendTask = newResendTask;
    }

    public String toString() {
        return type + " msg for " + mngr.getKGroup();
    }
}