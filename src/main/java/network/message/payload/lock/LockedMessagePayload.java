package network.message.payload.lock;

import java.io.Serial;
import java.util.List;

import configs.LockStrategy;
import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.Network;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class LockedMessagePayload extends MessagePayload{
    @Serial
    private static final long serialVersionUID = 8889721375113625608L;
    
    public int rtnSeqNo;

    public LockedMessagePayload(int epochNo, List<String> rtnID, String devID, int rtnSeqNo) {
        super(epochNo, KGroupType.ROUTINE, rtnID);
        this.devID = devID;
        this.rtnSeqNo = rtnSeqNo;
    }

    public LockedMessagePayload(int epochNo, String rtnID, String devID, int rtnSeqNo) {
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
            replyPayload = new LockedAckMessagePayload(epochNo, devID, srcSeqNo);
            replyMsgType = MessageType.LOCKED_ACK;

            if (!mngr.isRtnAcquiringLocks(rtnID, rtnSeqNo)) {
                return null;
            }
            mngr.acquiredDevLockForRtn(rtnID, rtnSeqNo, devID, mngr.getTouchedDevIDs(rtnID));
            mngr.log("Routine " + rtnID + "-" + rtnSeqNo + " k-group: device "
                     + devID + " lock acquired");
            mngr.log("    Leader " + Network.getMyID() + " receives LOCKED info");
            mngr.log("State: " + mngr.getRtnState(rtnID));
            
            if (mngr.isLockStrategy(LockStrategy.SERIAL) &&
                mngr.isRtnAcquiringLocks(rtnID, rtnSeqNo)) {
                mngr.requestDevLockForRtn(rtnID, rtnSeqNo);
            } else if (mngr.areRtnLocksAcquired(rtnID, rtnSeqNo)) {
                mngr.startRtnExec(rtnID, rtnSeqNo);
                
                mngr.log("Routine " + rtnID + "-" + rtnSeqNo
                        + " is starting execution!!!!");
            }
            
            if (mngr.isRtnReleasingLocks(rtnID, rtnSeqNo)) {
                mngr.releaseDevLockForRtn(rtnID, rtnSeqNo, devID);
            }
        }
        
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
