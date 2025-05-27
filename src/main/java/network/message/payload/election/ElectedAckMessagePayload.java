package network.message.payload.election;

import java.util.List;

import deploy.Deployer;
import deploy.KGroupManager;
import deploy.QuorumMsgInfo;
import javafx.util.Pair;
import kgroup.KGroupType;
import kgroup.LeaderElectionStage;
import metrics.KGroupMetric;
import metrics.NodeRole;
import network.Network;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class ElectedAckMessagePayload extends ReplyMessagePayload {
    private static final long serialVersionUID = 1845402298249270653L;

    private String leaderNodeID;

    public ElectedAckMessagePayload(
        int epochNo, KGroupType kGroupType, List<String> entitiesIDs, int srcSeqNo, String leaderNodeID
    ) {
        super(epochNo, kGroupType, entitiesIDs, srcSeqNo);
        this.leaderNodeID = leaderNodeID;
    }

    public ElectedAckMessagePayload(KGroupManager mngr, int srcSeqNo, String leaderNodeID) {
        super(mngr, srcSeqNo);
        this.leaderNodeID = leaderNodeID;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        if (mngr.getEpochNo() != epochNo || !mngr.isMemberOfKGroup()
            || mngr.isLdrElctnStg(LeaderElectionStage.COMPLETE)) {
            mngr.removeMsgInfo(this.srcSeqNo);
            return null;
        }
        
        QuorumMsgInfo msgInfo = mngr.getQuorumMsgInfo(this.srcSeqNo);
        if (msgInfo == null) {
            mngr.log("Msg seq no " + this.srcSeqNo + " does not exist!\n\t" + this);
        }
        if (msgInfo != null && !msgInfo.isApproved()) {
            if (leaderNodeID != null) {
                mngr.updateLeader(leaderNodeID);
                mngr.removeMsgInfo(srcSeqNo);
                mngr.advLdrElctnStg(LeaderElectionStage.COMPLETE);
                mngr.log(
                    "Leader election already completed earlier! Leader is " + leaderNodeID
                    + ", not me"
                );
            }
            else {
                msgInfo.newReply(src, true);
                final int neededAckCount = mngr.getF() + 1;
                mngr.log("Received new vote for election (" + msgInfo.getPositiveRepliesCount()
                        + "/" + neededAckCount + " now)");
                if (msgInfo.getPositiveRepliesCount() >= neededAckCount) {
                    mngr.updateLeader(Network.getMyID());
                    msgInfo.setApproved(Network.getCurTS());
                    long ldrElctnDelay = KGroupMetric.completeLdrElctn(
                        mngr, this.srcSeqNo, Network.getCurTS()
                    );
                    mngr.advLdrElctnStg(LeaderElectionStage.COMPLETE);
                    mngr.log("Finished election for epoch " + epochNo
                            + "\n\tit lasted " + ldrElctnDelay
                            + " ms,\n\tgot replies from " + msgInfo.getRepliedNodesIDs());
                    
                    if (mngr.getCountFailedNodes() >= mngr.getF()) {
                        mngr.replaceFailedNodes();
                    }
                    if (kGroupType.equals(KGroupType.ROUTINE)) {
                        Deployer.startRtnRoles(NodeRole.LEADER, entitiesIDs);
                    } else {
                        Deployer.startDevRoles(NodeRole.LEADER, entitiesIDs);
                    }
                    mngr.stateTransfer(
                        ((ElectedMessagePayload) msgInfo.getMessagePayload()).cause);
                    mngr.removeMsgInfo(this.srcSeqNo);
                }
            }
        }
        
        return null;
    }

    @Override
    public String toString() {
        return "ELECTED_ACK " + super.toString();
    }
}