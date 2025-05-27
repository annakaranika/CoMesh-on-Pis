package network.message.payload.stateTransfer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import deploy.DeviceState;
import deploy.KGroupManager;
import deploy.QuorumMsgInfo;
import javafx.util.Pair;
import kgroup.KGroupType;
import kgroup.state.StateTransferStage;
import metrics.KGroupMetric;
import network.Network;
import network.message.MessageType;
import network.message.payload.ReplyMessagePayload;

public class LocalKGroupStateMessagePayload extends ReplyMessagePayload {
    private static final long serialVersionUID = 1008075774431667084L;
    
    public Map<String, Object> states;
    public Map<String, Integer> seqNos;
    public Map<String, DeviceState> oldDevStates;
    
    public LocalKGroupStateMessagePayload(
        int epochNo, KGroupType kGroupType, List<String> entitiesIDs, int srcSeqNo,
        Map<String, Object> states, Map<String, Integer> seqNos,
        Map<String, DeviceState> oldDevStates
    ) {
        super(epochNo, kGroupType, entitiesIDs, srcSeqNo);
        this.states = new HashMap<String, Object>(states);
        if (seqNos != null) {
            this.seqNos = new HashMap<String, Integer>(seqNos);
        }
        if (oldDevStates != null) {
            this.oldDevStates = new HashMap<String, DeviceState>(oldDevStates);
        }
    }
    
    public LocalKGroupStateMessagePayload(KGroupManager mngr, int srcSeqNo) {
        super(mngr, srcSeqNo);
        this.states = new HashMap<String, Object>(mngr.getState());
        if (mngr.getSeqNos() != null) {
            this.seqNos = new HashMap<String, Integer>(mngr.getSeqNos());
        }
        if (mngr.getOldDevStates() != null) {
            this.oldDevStates = new HashMap<String, DeviceState>(mngr.getOldDevStates());
        }
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(
        KGroupManager mngr, String src, int srcSeqNo
    ) {
        if (mngr.getEpochNo() != epochNo || !mngr.isMemberOfKGroup()
        || mngr.isStateTrnsfrStg(StateTransferStage.COMPLETE)) {
            return null;
        }

        mngr.addLocalState(states);
        mngr.addLocalSeqNos(seqNos);
        mngr.addLocalOldDevStates(oldDevStates);

        QuorumMsgInfo msgInfo = mngr.getQuorumMsgInfo(this.srcSeqNo);
        if (msgInfo != null && !msgInfo.isApproved()) {
            msgInfo.setApproved(Network.getCurTS());
            msgInfo.newReply(src, true);
            final int neededAckCount = mngr.getF() + 1;
            if (msgInfo.getPositiveRepliesCount() >= neededAckCount) {
                int seqNo = mngr.quorum(MessageType.KGROUP_STATE_DISTRIBUTION,
                        new KGroupStateDistributionMessagePayload(
                            epochNo, kGroupType, mngr.getState(), mngr.getSeqNos()),
                        false, false);

                KGroupMetric.addStateTrnsfrSeqNo(mngr, this.srcSeqNo, seqNo);
                mngr.removeMsgInfo(this.srcSeqNo);
            }
        }
        return null;
    }
}
