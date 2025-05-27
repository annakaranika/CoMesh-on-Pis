package network.message.payload.stateTransfer;

import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class LocalKGroupStateRequestMessagePayload extends MessagePayload {
    private static final long serialVersionUID = -437966124955578173L;

    public LocalKGroupStateRequestMessagePayload(int epochNo, KGroupType kGroupType, List<String> entitiesIDs) {
        super(epochNo, kGroupType, entitiesIDs);
    }

    public LocalKGroupStateRequestMessagePayload(KGroupManager mngr) {
        super(mngr);
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        if (mngr.getEpochNo() != epochNo) {
            return null;
        }

        ReplyMessagePayload replyPayload
                            = new LocalKGroupStateMessagePayload(mngr, srcSeqNo);
        MessageType replyMsgType = MessageType.LOCAL_KGROUP_STATE;
        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
