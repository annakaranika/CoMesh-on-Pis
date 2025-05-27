package routine.statement;

import java.util.List;
import java.util.Map;

import deploy.DeviceState;
import routine.statement.condition.RoutineCondition;

public class Statement extends RoutineStatement {
    RoutineCondition condition;

    public Statement(RoutineCondition condition) {
        this.condition = condition;
    }

    @Override
    public RoutineStatement negate() {
        return new Statement(condition.invertRelation());
    }

    public RoutineCondition getCondition() {
        return condition;
    }

    public boolean isSatisfied(Map<String, DeviceState> devStates) {
        return condition.isSatisfied(devStates);
    }

    @Override
    public List<String> getTriggerDevIDs() {
        String devID = condition.getTriggerDevID();
        if (devID == null) return List.of();
        return List.of(condition.getTriggerDevID());
    }

    @Override
    public String toString() {
        return condition.toString();
    }
}
