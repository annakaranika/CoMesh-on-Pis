package network.message.payload.stateTransfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import deploy.DeviceState;
import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import kgroup.LeaderElectionStage;
import kgroup.state.StateTransferStage;
import network.Network;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class KGroupStateDistributionMessagePayload extends MessagePayload {
    private static final long serialVersionUID = 5530604947123381069L;
    
    public Map<String, Object> state;
    public Map<String, Integer> seqNos;
    public Map<String, DeviceState> oldDevStates;
    
    public KGroupStateDistributionMessagePayload(
        int epochNo, KGroupType kGroupType, Map<String, Object> state, Map<String, Integer> seqNos
    ) {
        super(epochNo, kGroupType, new ArrayList<String>(state.keySet()));
        Network.log("Sent KGroupStateDistributionMessagePayload's state: " + state);
        this.state = new HashMap<String, Object>(state);
        if (seqNos != null) {
            this.seqNos = new HashMap<String, Integer>(seqNos);
        }
        if (oldDevStates != null) {
            this.oldDevStates = new HashMap<String, DeviceState>(oldDevStates);
        }
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        if (mngr.getEpochNo() != epochNo || !mngr.isMemberOfKGroup()) {
            return null;
        }

        if (mngr.isLeaderNull() || !mngr.isLdrElctnStg(LeaderElectionStage.COMPLETE)) {
            mngr.log("Leader election stage: " + mngr.getLdrElctnStg());
            mngr.updateLeader(src);
            mngr.advLdrElctnStg(LeaderElectionStage.COMPLETE);
        }

        Network.log("Received KGroupStateDistributionMessagePayload's state: " + state);
        mngr.setState(state);
        mngr.setSeqNos(seqNos);
        mngr.setOldDevStates(oldDevStates);

        mngr.advStateTrnsfrStg(StateTransferStage.COMPLETE);
        mngr.openReceivingEnd();
        ReplyMessagePayload replyPayload = new KGroupStateAckMessagePayload(mngr, srcSeqNo);
        MessageType replyMsgType = MessageType.KGROUP_STATE_ACK;

        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
