package routine.statement.condition;

import java.util.Map;

import deploy.DeviceState;
import network.Network;

public class TimeCondition extends RoutineCondition {
    public float value;

    public TimeCondition(ConditionRelation relation, float value) {
        this.relation = relation;
        this.value = value;
    }

    @Override
    public boolean isSatisfied(Map<String, DeviceState> devStates) {
        switch (relation) {
        case EQUAL:
            return Network.getCurTS() == value;
        case GREATER:
            return Network.getCurTS() > value;
        case GREATER_EQUAL:
            return Network.getCurTS() >= value;
        case LESS:
            return Network.getCurTS() < value;
        case LESS_EQUAL:
            return Network.getCurTS() <= value;
        case NOT_EQUAL:
            return Network.getCurTS() != value;
        default:
            // could throw exception here alternatively
            return false;
        }
    }

    @Override
    public String toString() {
        return "time" + relation.toString() + value;
    }
}
