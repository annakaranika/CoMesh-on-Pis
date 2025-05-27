package network.message.payload.lock;

import java.io.Serial;
import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class LockReleasedMessagePayload extends MessagePayload {
    @Serial
    private static final long serialVersionUID = -717256623598162112L;

    public int rtnSeqNo;

    public LockReleasedMessagePayload(int epochNo, List<String> rtnID, String devID, int rtnSeqNo) {
        super(epochNo, KGroupType.ROUTINE, rtnID);
        this.devID = devID;
        this.rtnSeqNo = rtnSeqNo;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        if (epochNo != mngr.getEpochNo() ||  !mngr.isMemberOfKGroup()) {
            // forward message to new device k-group leader
            return null;
        }

        ReplyMessagePayload replyPayload;
        MessageType replyMsgType;

        if (!mngr.isLeader()) {
            replyPayload = new RoutineKGroupLeaderInfoMessagePayload(
                                    epochNo, List.of(devID), srcSeqNo, src);
            replyMsgType = MessageType.ROUTINE_KGROUP_LEADER_INFO;
        }
        else {
            String rtnID = entitiesIDs.get(0);
            mngr.lockReleased(rtnID, rtnSeqNo, devID);
            mngr.log("Routine " + rtnID + "-" + rtnSeqNo + " k-group: device "
                    + devID + " lock released");

            replyPayload = new LockReleasedAckMessagePayload(epochNo, devID, srcSeqNo);
            replyMsgType = MessageType.LOCK_RELEASED_ACK;

            if (mngr.areRtnLocksReleased(rtnID, rtnSeqNo)) {
                mngr.log(
                    "Routine " + rtnID + ": released all locks - removing routine stage from state"
                );
                mngr.removeRtnStage(rtnID, rtnSeqNo);
            }
        }
        
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
