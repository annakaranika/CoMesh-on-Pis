package routine.command;

import java.util.ArrayList;
import java.util.List;

import deploy.DeviceState;

public class SetOfCommands extends Subroutine {
    // the commands in this class can be performed in parallel
    private List<Subroutine> cmds;

    public SetOfCommands(List<Subroutine> cmds) {
        this.cmds = cmds;
    }

    public List<Subroutine> getCmds() {
        return cmds;
    }
    
    @Override
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
            length = Math.max(length, sub.getLength());
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
