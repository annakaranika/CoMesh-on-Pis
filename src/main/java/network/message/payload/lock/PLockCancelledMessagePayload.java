package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.payload.MessagePayload;

import java.io.Serial;
import java.util.List;

public class PLockCancelledMessagePayload extends MessagePayload {
    @Serial
    private static final long serialVersionUID = 9105750019058971231L;
    public int rtnSeqNo;

    public PLockCancelledMessagePayload(int epochNo, List<String> rtnID, int rtnSeqNo, String devID) {
        super(epochNo, KGroupType.ROUTINE, rtnID);
        this.rtnID = rtnID.get(0);
        this.rtnSeqNo = rtnSeqNo;
        this.devID = devID;
    }
}
