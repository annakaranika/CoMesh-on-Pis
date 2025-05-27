package network.message.payload;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import deploy.KGroupManager;
import javafx.util.Pair;
import kgroup.KGroupType;
import network.message.MessageType;

public class MessagePayload implements Cloneable, Serializable {
    private static final long serialVersionUID = 8135931604175469785L;
    protected int epochNo;
    // the following two fields specify the k-group this message is addressed to
    protected KGroupType kGroupType;
    protected List<String> entitiesIDs;
    protected String rtnID, devID;

    public MessagePayload(int epochNo, KGroupType kGroupType, List<String> entitiesIDs) {
        this.epochNo = epochNo;
        this.kGroupType = kGroupType;
        this.entitiesIDs = new ArrayList<String>(entitiesIDs);
    }

    public MessagePayload(KGroupManager mngr) {
        this.epochNo = mngr.getEpochNo();
        this.kGroupType = mngr.getType();
        this.entitiesIDs = new ArrayList<String>(mngr.getMonitored());
    }

    public MessagePayload(int epochNo, KGroupType kGroupType, String entityID) {
        this.epochNo = epochNo;
        this.kGroupType = kGroupType;
        this.entitiesIDs = List.of(entityID);
    }

    public MessagePayload(KGroupType kGroupType, String entityID) {
        this.kGroupType = kGroupType;
        this.entitiesIDs = List.of(entityID);
    }

    public MessagePayload() {}

    public void changeEpochNo(int newEpoch) {
        this.epochNo = newEpoch;
    }

    public KGroupType getKGroupType() {
        return kGroupType;
    }

    public int getSrcSeqNo() {
        return -1;
    }

    public List<String> getMonitored() {
        return entitiesIDs;
    }

    public int getEpochNo() {
        return epochNo;
    }

    public String getDevID() {
        return devID;
    }

    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        return null;
    }
    
    public int getByteSize() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this.clone());
            oos.flush();
            return bos.toByteArray().length;
        }
        catch (Exception ex) {
            System.out.println("Exception during payload byte size calculation");
            ex.printStackTrace(System.out);
            return -1;
        }
    }

    @Override
    public String toString() {
        return "msg for " + kGroupType + " k-group in charge of " + entitiesIDs + " at epoch " + epochNo;
    }
}
