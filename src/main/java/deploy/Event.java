package deploy;

public class Event {
    private EventType type;
    private String affectedEntity;
    private int rtnSeqNo;

    public Event(EventType type, String affectedEntity) {
        this.type = type;
        this.affectedEntity = affectedEntity;
    }

    public Event(String rtnID, int rtnSeqNo) {
        this.type = EventType.ROUTINE_EXECUTED;
        this.affectedEntity = rtnID;
        this.rtnSeqNo = rtnSeqNo;
    }

    public Event(String type, String affectedEntity) {
        switch (type) {
        case "f":
            this.type = EventType.NODE_FAILED;
            break;
        case "j":
            this.type = EventType.NODE_JOINED;
            break;
        case "r":
            this.type = EventType.ROUTINE_TRIGGERED;
            break;
        case "e":
            this.type = EventType.ROUTINE_EXECUTED;
        }
        this.affectedEntity = affectedEntity;
    }

    public EventType getType() {
        return type;
    }

    public String getAffectedEntity() {
        return affectedEntity;
    }

    public int getrtnSeqNo() {
        return rtnSeqNo;
    }

    public String toString() {
        return type.toString() + " " + affectedEntity + (type == EventType.ROUTINE_EXECUTED? "-" + rtnSeqNo: "");
    }
}
