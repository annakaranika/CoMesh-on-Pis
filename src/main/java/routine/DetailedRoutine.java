package routine;

import java.util.Map;

import deploy.DeviceState;
import routine.command.Subroutine;
import routine.statement.RoutineStatement;

public class DetailedRoutine extends Routine {
    private RoutineStatement conditions;
    private Subroutine cmds;

    public DetailedRoutine(RoutineStatement conditions, Subroutine cmds) {
        super(conditions.getTriggerDevIDs(), cmds.getTouchedDevicesIDs(), cmds.getLength());
        this.conditions = conditions;
        this.cmds = cmds;
    }

    public RoutineStatement getConditions() {
        return conditions;
    }

    public Subroutine getCmds() {
        return cmds;
    }

    @Override
    public long getLength() {
        return cmds.getLength();
    }

    @Override
    public boolean isTriggered(Map<String, DeviceState> devStates) {
        return conditions.isSatisfied(devStates);
    }

    @Override
    public DeviceState getNewState(String devID) {
        return cmds.getNewState(devID);
    }
}
