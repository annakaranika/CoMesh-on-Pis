package routine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import deploy.DeviceState;

public class Routine {
    protected List<String> triggerDevIDs, touchedDevIDs;
    protected long length;
    protected int seqNo;
    // A routine triggering should have a sequence number.
    // This way, we may support routine triggerings that happen
    // before a specific triggering's execution has finished
    // Specifically, this way, we may know if a lock request is new
    // or old and we may handle it appropriately.

    protected Routine() {}

    protected Routine(List<String> triggerDevIDs, List<String> touchedDevIDs, long length) {
        this.triggerDevIDs = new ArrayList<String>();
        if (triggerDevIDs != null) this.triggerDevIDs.addAll(triggerDevIDs);
        this.touchedDevIDs = new ArrayList<String>(touchedDevIDs);
        this.length = length;
        this.seqNo = -1;
    }

    public List<String> getTriggerDevIDs() {
        return triggerDevIDs;
    }

    public List<String> getTouchedDevIDs() {
        return touchedDevIDs;
    }

    public long getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getSeqNo() {
        return seqNo;
    }

    public boolean isTriggered(Map<String, DeviceState> devStates) { return false; }

    public int newTriggering() {
        return ++ seqNo;
    }

    public DeviceState getNewState(String devID) { return null; }

    @Override
    public String toString() {
        return "trig devs: " + triggerDevIDs + ", cmd devs: " + touchedDevIDs + ", length: "
            + length;
    }
}
