package network.message.payload;

import java.util.List;

import deploy.KGroupManager;
import kgroup.KGroupType;

public class ReplyMessagePayload extends MessagePayload {
    private static final long serialVersionUID = -7456568437786695638L;
    public int srcSeqNo; // this message replies to src's message with seqNo = srcSeqNo

    public ReplyMessagePayload(int epochNo, KGroupType kGroupType, List<String> entitiesIDs, int srcSeqNo) {
        super(epochNo, kGroupType, entitiesIDs);
        this.srcSeqNo = srcSeqNo;
    }

    public ReplyMessagePayload(KGroupManager mngr, int srcSeqNo) {
        super(mngr);
        this.srcSeqNo = srcSeqNo;
    }

    public ReplyMessagePayload(int epochNo, KGroupType kGroupType, String entityID, int srcSeqNo) {
        super(epochNo, kGroupType, entityID);
        this.srcSeqNo = srcSeqNo;
    }

    public ReplyMessagePayload(KGroupType kGroupType, String entityID, int srcSeqNo) {
        super(kGroupType, entityID);
        this.srcSeqNo = srcSeqNo;
    }

    public ReplyMessagePayload(int srcSeqNo) {
        this.srcSeqNo = srcSeqNo;
    }

    @Override
    public int getSrcSeqNo() {
        return srcSeqNo;
    }

    @Override
    public String toString() {
        return super.toString() + " answering to srcSeqNo " + srcSeqNo;
    }
}
