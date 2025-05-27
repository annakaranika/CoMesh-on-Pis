package routine.command;

import java.util.List;

import deploy.DeviceState;

public class Command extends Subroutine {
    // this class contains a single command
    private RoutineCommand cmd;

    public Command(RoutineCommand cmd) {
        this.cmd = cmd;
    }

    public RoutineCommand getCmd() {
        return cmd;
    }

    @Override
    public long getLength() {
        return cmd.getLength();
    }

    @Override
    public List<String> getTouchedDevicesIDs() {
        return cmd.getTouchedDevicesIDs();
    }

    public DeviceState getNewState(String devID) {
        if (cmd.getDevID().equals(devID))
            return cmd.getNewState();
        else return null;
    }
}
