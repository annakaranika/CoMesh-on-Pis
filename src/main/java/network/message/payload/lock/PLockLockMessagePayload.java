package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.payload.MessagePayload;

import java.io.Serial;
import java.util.List;

public class PLockLockMessagePayload extends MessagePayload {
    @Serial
    private static final long serialVersionUID = 4527894244950821016L;
    public int rtnSeqNo;
    public int reqSeqNo;

    public PLockLockMessagePayload(int epochNo, List<String> devID,
                                   String rtnID, int rtnSeqNo, int reqSeqNo) {
        super(epochNo, KGroupType.DEVICE, devID);
        this.rtnID = rtnID;
        this.rtnSeqNo = rtnSeqNo;
        this.reqSeqNo = reqSeqNo;
    }
}
