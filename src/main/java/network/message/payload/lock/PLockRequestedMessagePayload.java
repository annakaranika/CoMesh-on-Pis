package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.payload.MessagePayload;

import java.io.Serial;

public class PLockRequestedMessagePayload extends MessagePayload {
    @Serial
    private static final long serialVersionUID = 6580474586145450814L;
    public int rtnSeqNo;
    public int reqSeqNo;

    public PLockRequestedMessagePayload(
            int epochNo, String rtnID, int rtnSeqNo, int reqSeqNo, String devID) {
        super(epochNo, KGroupType.ROUTINE, rtnID);
        this.rtnSeqNo = rtnSeqNo;
        this.reqSeqNo = reqSeqNo;
        this.devID = devID;
    }
}
