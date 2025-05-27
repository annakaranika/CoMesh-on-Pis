package network.message.payload.stateTransfer;

import java.util.List;

import deploy.KGroupManager;
import deploy.UnicastMsgInfo;
import javafx.util.Pair;
import kgroup.KGroup;
import kgroup.KGroupType;
import kgroup.LeaderElectionCause;
import metrics.KGroupMetric;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class NotOldLeaderMessagePayload extends ReplyMessagePayload {
    private static final long serialVersionUID = -6346149567749302478L;

    public String oldLeader;

    public NotOldLeaderMessagePayload(
        int epochNo, KGroupType kGroupType, List<String> entitiesIDs,
        int srcSeqNo, String oldLeader
    ) {
        super(epochNo, kGroupType, entitiesIDs, srcSeqNo);
        this.oldLeader = oldLeader;
    }

    public NotOldLeaderMessagePayload(KGroupManager mngr, int srcSeqNo) {
        super(mngr, srcSeqNo);
        this.oldLeader = mngr.getMostProbableOldLeader();
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
        
        if (oldLeader != null) {
            mngr.updateOldLeader(oldLeader);
            mngr.log(mngr.getType() + " " + mngr.getMonitored() + " k-group: old leader updated to " + oldLeader + " by leader");
        }
        else {
            mngr.removeOldLdrCnddt(src);
            mngr.log(mngr.getType() + " " + mngr.getMonitored() + " k-group: old node " + src + " removed as old leader candidate by leader " + mngr.getLeader());
        }

        int seqNo;
        if (mngr.isOldLeaderNull() || !mngr.isOldLeader()) {
            if (mngr.isOldLeaderNull() && mngr.isOldMember()) {
                // in case I just joined
                // but the system believes I should have been the leader
                mngr.removeOldLdrCnddt();
            }
            if (mngr.noOldLdrCnddts()) {
                seqNo = mngr.quorum(MessageType.LOCAL_KGROUP_STATE_REQUEST,
                    new LocalKGroupStateRequestMessagePayload(epochNo,
                        kGroupType, entitiesIDs), true, false);
            }
            else {
                seqNo = mngr.remoteCall((KGroup) null, mngr.getMostProbableOldLeader(),
                        MessageType.KGROUP_STATE_REQUEST,
                        new KGroupStateRequestMessagePayload(
                                epochNo, kGroupType, entitiesIDs,
                                LeaderElectionCause.NEW_EPOCH), true);
            }
        }
        else {
            seqNo = mngr.quorum(
                MessageType.KGROUP_STATE_DISTRIBUTION, new KGroupStateDistributionMessagePayload(
                    epochNo, kGroupType, mngr.getState(), mngr.getSeqNos()
                ), false, false
            );
        }
        KGroupMetric.addStateTrnsfrSeqNo(mngr, this.srcSeqNo, seqNo);
        
        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}
