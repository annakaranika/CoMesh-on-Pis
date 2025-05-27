package network.message;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import javafx.util.Pair;

import deploy.KGroupManager;
import kgroup.KGroupType;
import network.Network;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;

public class Message<P extends MessagePayload> implements Cloneable, Serializable {
    private static final long serialVersionUID = 6939491387675430673L;
    private String src, dst;
    private int srcSeqNo; // needed for receiver to know which original message a reply it received corresponds to
    private MessageType type; // deprecating it in favor of MessagePayload specific extensions
    private P payload;
    
    public Message(String src, int srcSeqNo, String dst, MessageType type, P payload) {
        this.src = src;
        this.srcSeqNo = srcSeqNo;
        this.dst = dst;
        this.type = type;
        this.payload = payload;
    }

    public String getSrcID() {
        return src;
    }

    public String getDstID() {
        return dst;
    }

    public int getSrcSeqNo() {
        return srcSeqNo;
    }

    public MessageType getType() {
        return type;
    }

    public boolean isType(MessageType type) {
        return this.type == type;
    }

    public P getPayload() {
        return payload;
    }

    public KGroupType getKGroupType() {
        return payload.getKGroupType();
    }

    public List<String> getMonitored() {
        return payload.getMonitored();
    }

    public void process(KGroupManager mngr) {
        Pair<MessageType, ReplyMessagePayload> replyInfo = getPayload().process(mngr, src, srcSeqNo);
        if (replyInfo == null) {
            return;
        }

        MessageType replyMsgType = replyInfo.getKey();
        ReplyMessagePayload replyPayload = replyInfo.getValue();
        int seqNo;
        if (mngr != null) {
            seqNo = mngr.getNextSeqNo();
            mngr.unicast(src, replyMsgType, replyPayload, seqNo);
        }
        else{
            seqNo = Network.getNextSeqNo();
            KGroupManager.unicastS(src, replyMsgType, replyPayload, seqNo);
        }
    }

    public int getByteSize() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this.clone());
            oos.flush();
            return bos.toByteArray().length;
        }
        catch (Exception ex) {
            System.out.println("Exception during msg byte size calculation");
            ex.printStackTrace(System.out);
            return -1;
        }
    }

    public String toString() {
        return type + " msg from " + src + " to " + dst + " (srcSeqNo: " + srcSeqNo + ")";
    }
}
