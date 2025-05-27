package deploy;

import java.util.ArrayList;
import java.util.List;

import kgroup.KGroup;
import metrics.KGroupMetric;
import network.message.MessageType;
import network.message.payload.MessagePayload;

public class QuorumMsgInfo extends MsgInfo {
    private int positiveReplies, seqNo;
    private List<String> replied;
    private boolean approved, oldKGroup;

    public QuorumMsgInfo(
        MessageType type, MessagePayload payload, KGroupManager mngr, boolean boundedWait,
        boolean oldKGroup, long initialTS, int initialEpoch, int seqNo, ResendTask resendTask
    ) {
        super(type, payload, mngr, boundedWait, initialTS, initialEpoch, resendTask);
        positiveReplies = 0;
        replied = new ArrayList<String>();
        approved = false;
        this.oldKGroup = oldKGroup;
        this.seqNo = seqNo;
        KGroupMetric.startQuorum(mngr, seqNo, initialTS);
    }

    public synchronized void newReply(String nodeID, boolean positive) {
        if (!replied.contains(nodeID)) {
            replied.add(nodeID);
            if (positive)
                positiveReplies ++;
        }
    }

    public synchronized int getPositiveRepliesCount() {
        return positiveReplies;
    }

    public KGroup getKGroup() {
        return mngr.kgroup;
    }

    public List<String> getRepliedNodesIDs() {
        return replied;
    }

    public List<String> getNoReplyNodesIDs() {
        List<String> kgroupRecipients;
        kgroupRecipients = new ArrayList<>(mngr.getCurMemberIDs());
        kgroupRecipients.removeAll(replied);
        return kgroupRecipients;
    }

    public void setApproved(long ts) {
        approved = true;
        KGroupMetric.completeQuorum(mngr, seqNo, ts);
    }

    public boolean isApproved() {
        return approved;
    }

    public boolean toOldKGroup() {
        return oldKGroup;
    }
}
