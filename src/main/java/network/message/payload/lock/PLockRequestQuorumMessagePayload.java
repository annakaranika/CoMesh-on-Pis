package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.payload.MessagePayload;

import java.io.Serial;
import java.util.List;

public class PLockRequestQuorumMessagePayload extends MessagePayload {
    @Serial
    private static final long serialVersionUID = -5659243460030671211L;
    public int reqSeqNo, rtnSeqNo;

    public PLockRequestQuorumMessagePayload(int epochNo, List<String> devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        super(epochNo, KGroupType.DEVICE, devID);
        this.rtnID = rtnID;
        this.reqSeqNo = reqSeqNo;
        this.rtnSeqNo = rtnSeqNo;
    }
}
