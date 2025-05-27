package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.payload.ReplyMessagePayload;

import java.io.Serial;
import java.util.List;

public class PLockCancelQuorumAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = 3985041353348347310L;
    public int reqSeqNo, rtnSeqNo;

    public PLockCancelQuorumAckMessagePayload(int epochNo, List<String> devID, int srcSeqNo, String rtnID, int reqSeqNo, int rtnSeqNo) {
        super(epochNo, KGroupType.DEVICE, devID, srcSeqNo);
        this.rtnID = rtnID;
        this.reqSeqNo = reqSeqNo;
        this.rtnSeqNo = rtnSeqNo;
    }
}
