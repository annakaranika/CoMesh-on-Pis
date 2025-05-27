package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.payload.ReplyMessagePayload;

import java.io.Serial;
import java.util.List;

public class PLockRequestQuorumAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = 7941972998006287283L;
    public int reqSeqNo, rtnSeqNo;

    public PLockRequestQuorumAckMessagePayload(
            int epochNo, List<String> devID, int srcSeqNo,
            String rtnID, int rtnSeqNo, int reqSeqNo) {
        super(epochNo, KGroupType.DEVICE, devID, srcSeqNo);
        this.rtnID = rtnID;
        this.rtnSeqNo = rtnSeqNo;
        this.reqSeqNo = reqSeqNo;
        this.devID = devID.get(0);
    }
}
