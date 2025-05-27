package routine.statement.condition;

import java.util.Map;

import deploy.DeviceState;

public class RoutineCondition {
    protected ConditionRelation relation;
    public boolean satisfied;

    public RoutineCondition invertRelation() {
        switch (relation) {
            case EQUAL:
                relation = ConditionRelation.NOT_EQUAL;
                break;
            case GREATER:
                relation = ConditionRelation.LESS_EQUAL;
                break;
            case GREATER_EQUAL:
                relation = ConditionRelation.LESS;
                break;
            case LESS:
                relation = ConditionRelation.GREATER_EQUAL;
                break;
            case LESS_EQUAL:
                relation = ConditionRelation.GREATER;
                break;
            case NOT_EQUAL:
                relation = ConditionRelation.EQUAL;
                break;
        }
        return this;
    }

    public ConditionRelation getRelation() {
        return relation;
    }

    public boolean isSatisfied(Map<String, DeviceState> devStates) {
        return satisfied;
    }

    public void setSatisfied() {
        satisfied = true;
    }

    public String getTriggerDevID() { return null; }
}