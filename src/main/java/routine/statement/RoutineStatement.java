package routine.statement;

import java.util.List;
import java.util.Map;

import deploy.DeviceState;

public abstract class RoutineStatement {
    public abstract RoutineStatement negate();
    public abstract boolean isSatisfied(Map<String, DeviceState> devStates);
    public List<String> getTriggerDevIDs() { return List.of(); }
    public abstract String toString();
}