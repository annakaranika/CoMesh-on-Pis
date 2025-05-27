package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.payload.MessagePayload;

import java.io.Serial;
import java.util.List;

public class PLockCancelMessagePayload extends MessagePayload {
    @Serial
    private static final long serialVersionUID = 5128713308909528515L;
    public int rtnSeqNo;

    public PLockCancelMessagePayload(
            int epochNo, List<String> devID, String rtnID, int rtnSeqNo) {
        super(epochNo, KGroupType.DEVICE, devID);
        this.rtnID = rtnID;
        this.rtnSeqNo = rtnSeqNo;
    }

    public PLockCancelMessagePayload(
            int epochNo, String devID, String rtnID, int rtnSeqNo) {
        super(epochNo, KGroupType.DEVICE, devID);
        this.rtnID = rtnID;
        this.rtnSeqNo = rtnSeqNo;
    }
}
