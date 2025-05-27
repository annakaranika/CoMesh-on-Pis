package network.message.payload.failure;

import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class NodeFailureMessagePayload extends MessagePayload {
    private static final long serialVersionUID = -5307849874262842593L;
    
    public String failedNodeID;

    public NodeFailureMessagePayload(KGroupType kGroupType, List<String> entitiesIDs, String failedNodeID) {
        super(-1, kGroupType, entitiesIDs);
        this.failedNodeID = failedNodeID;
    }

    @Override
    public Pair<MessageType, ReplyMessagePayload> process(
        KGroupManager mngr, String src, int srcSeqNo) {
        
        mngr.handleNodeFailure(failedNodeID);

        MessageType replyMsgType = MessageType.NODE_FAILURE_ACK;
        ReplyMessagePayload replyPayload = new NodeFailureAckMessagePayload(srcSeqNo);

        return new Pair<MessageType, ReplyMessagePayload>(replyMsgType, replyPayload);
    }
}
