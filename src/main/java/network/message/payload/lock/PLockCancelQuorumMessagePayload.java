package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.payload.MessagePayload;

import java.io.Serial;
import java.util.List;

public class PLockCancelQuorumMessagePayload extends MessagePayload {
    @Serial
    private static final long serialVersionUID = -274068365068723274L;
    public int reqSeqNo, rtnSeqNo;

    public PLockCancelQuorumMessagePayload(int epochNo, List<String> devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        super(epochNo, KGroupType.DEVICE, devID);
        this.rtnID = rtnID;
        this.reqSeqNo = reqSeqNo;
        this.rtnSeqNo = rtnSeqNo;
    }
}
