package network.message.payload.lock;

import java.io.Serial;

import deploy.KGroupManager;
import deploy.UnicastMsgInfo;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class LockReleasedAckMessagePayload extends ReplyMessagePayload {
    @Serial
    private static final long serialVersionUID = -6501728481760721880L;

    public LockReleasedAckMessagePayload(
        int epochNo, String devID, int srcSeqNo
    ) {
        super(epochNo, KGroupType.DEVICE, devID, srcSeqNo);
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        if (!mngr.isMemberOfKGroup()) {
            mngr.removeMsgInfo(this.srcSeqNo);
            return null;
        }

        UnicastMsgInfo msgInfo = mngr.getUnicastMsgInfo(this.srcSeqNo);
        if (msgInfo == null) {
            mngr.removeMsgInfo(this.srcSeqNo);
            return null;
        }
        
        String devID = entitiesIDs.get(0);
        if (mngr.isLeader()) {
            mngr.log(
                "Received LOCK_RELEASED_ACK for routine " + rtnID + " device " + devID
            );
        }

        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}
