package routine.statement.condition;

import java.util.Map;

import deploy.DeviceState;

public class DeviceStateCondition extends RoutineCondition {
    private String devID;
    private DeviceState value;

    public DeviceStateCondition(String devID, ConditionRelation relation, String value) {
        this.devID = devID;
        this.relation = relation;
        this.value = new DeviceState(value);
    }

    @Override
    public boolean isSatisfied(Map<String, DeviceState> devStates) {
        DeviceState state = devStates.get(devID);
        if (state != null) {
            switch (relation) {
                case EQUAL:
                    return state.equals(value);
                case NOT_EQUAL:
                    return !state.equals(value);
                case GREATER:
                    return state.getNum() > value.getNum();
                case GREATER_EQUAL:
                    return state.getNum() >= value.getNum();
                case LESS:
                    return state.getNum() < value.getNum();
                case LESS_EQUAL:
                    return state.getNum() <= value.getNum();
                default:
                    return false;
            }
        }
        return false;
    }

    public String getTriggerDevID() {
        return devID;
    }

    @Override
    public String toString() {
        return "dev" + devID + relation.toString() + value;
    }
}
