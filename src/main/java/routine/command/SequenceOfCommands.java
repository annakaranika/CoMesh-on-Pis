package routine.command;

import java.util.ArrayList;
import java.util.List;

import deploy.DeviceState;

public class SequenceOfCommands extends Subroutine {
    // the commands in this class must be performed sequentially
    private List<Subroutine> cmds;

    public SequenceOfCommands(List<Subroutine> cmds) {
        this.cmds = cmds;
    }
    
    public List<String> getTouchedDevicesIDs() {
        List<String> touchedDevicesIDs = new ArrayList<>();

        for (Subroutine sub: cmds) {
            touchedDevicesIDs.addAll(sub.getTouchedDevicesIDs());
        }
        
        return touchedDevicesIDs;
    }

    @Override
    public long getLength() {
        long length = 0;
        for (Subroutine sub: cmds) {
            length += sub.getLength();
        }
        return length;
    }

    public DeviceState getNewState(String devID) {
        DeviceState newState = null;
        for (Subroutine cmd: cmds) {
            newState = cmd.getNewState(devID);
            if (newState != null) {
                break;
            }
        }
        return newState;
    }
}
