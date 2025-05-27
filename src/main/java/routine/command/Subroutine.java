package routine.command;

import java.util.List;

import deploy.DeviceState;

public abstract class Subroutine {
    abstract public List<String> getTouchedDevicesIDs();

    abstract public long getLength();
    abstract public DeviceState getNewState(String devID);
}
