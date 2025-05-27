package network.message.payload.lock;

import kgroup.KGroupType;
import network.message.payload.ReplyMessagePayload;

import java.io.Serial;

public class PLockFailMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = 1646035768600306694L;
    public int rtnSeqNo;
    public String devID;

    public PLockFailMessagePayload(int epochNo, String rtnID, int rtnSeqNo, int srcSeqNo, String devID) {
        super(epochNo, KGroupType.ROUTINE, rtnID, srcSeqNo);
        this.rtnSeqNo = rtnSeqNo;
        this.devID = devID;
    }
}
