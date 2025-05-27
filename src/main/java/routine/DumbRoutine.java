package routine;

import java.util.List;
import java.util.Map;

import deploy.DeviceState;

public class DumbRoutine extends Routine {
    private boolean triggered;
    
    public DumbRoutine(List<String> triggerDevIDs, List<String> touchedDevIDs, int length) {
        super(triggerDevIDs, touchedDevIDs, length);
        triggered = false;
    }

    public DumbRoutine(List<String> triggerDevIDs, List<String> touchedDevIDs) {
        this.triggerDevIDs = triggerDevIDs;
        this.touchedDevIDs = touchedDevIDs;
        triggered = false;
    }

    @Override
    public boolean isTriggered(Map<String, DeviceState> devStates) {
        return triggered;
    }

    public void setTriggered() {
        triggered = true;
    }

    public void resetTriggered() {
        triggered = false;
    }
}
