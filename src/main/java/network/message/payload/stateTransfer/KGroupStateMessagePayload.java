package network.message.payload.stateTransfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import deploy.DeviceState;
import deploy.KGroupManager;
import deploy.UnicastMsgInfo;
import javafx.util.Pair;
import kgroup.KGroupType;
import metrics.KGroupMetric;
import network.message.Message;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class KGroupStateMessagePayload extends ReplyMessagePayload {
    private static final long serialVersionUID = -6346149567749302839L;
    
    public Map<String, Object> state;
    public Map<String, Integer> seqNos;
    public Map<String, DeviceState> oldDevStates;
    public List<Message<? extends MessagePayload>> unprocessedMessages;
    
    public KGroupStateMessagePayload(
        int epochNo, KGroupType kGroupType, List<String> entitiesIDs, int srcSeqNo,
        Map<String, Object> state, Map<String, Integer> seqNos,
        Map<String, DeviceState> oldDevStates,
        List<Message<? extends MessagePayload>> messages
    ) {
        super(epochNo, kGroupType, entitiesIDs, srcSeqNo);
        this.state = new HashMap<String, Object>(state);
        if (seqNos != null) {
            this.seqNos = new HashMap<String, Integer>(seqNos);
        }
        if (oldDevStates != null) {
            this.oldDevStates = new HashMap<String, DeviceState>(oldDevStates);
        }
        this.unprocessedMessages = new ArrayList<Message<? extends MessagePayload>>(messages);
    }
    
    public KGroupStateMessagePayload(KGroupManager mngr, int srcSeqNo) {
        super(mngr, srcSeqNo);
        this.state = new HashMap<String, Object>(mngr.getState());
        if (mngr.getSeqNos() != null) {
            this.seqNos = new HashMap<String, Integer>(mngr.getSeqNos());
        }
        if (mngr.getOldDevStates() != null) {
            this.oldDevStates = new HashMap<String, DeviceState>(mngr.getOldDevStates());
        }
        this.unprocessedMessages = new ArrayList<Message<? extends MessagePayload>>(
            mngr.getWaitingMessageQueue()
        );
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
        
        mngr.setState(state);
        mngr.setSeqNos(seqNos);
        mngr.setOldDevStates(oldDevStates);
        mngr.setWaitingMessageQueue(unprocessedMessages);

        int seqNo = mngr.quorum(MessageType.KGROUP_STATE_DISTRIBUTION,
            new KGroupStateDistributionMessagePayload(
                epochNo, kGroupType, state, seqNos
            ), false, false
        );
        KGroupMetric.addStateTrnsfrSeqNo(mngr, this.srcSeqNo, seqNo);
        
        mngr.removeMsgInfo(this.srcSeqNo);
        return null;
    }
}
