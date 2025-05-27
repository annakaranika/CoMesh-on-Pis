package routine.command;

import java.util.List;

import deploy.DeviceState;

public class RoutineCommand {
    private String devID;
    private DeviceState newState;

    public RoutineCommand(String devID, String newState) {
        this.devID = devID;
        this.newState = new DeviceState(newState);
    }

    public String getDevID() { return devID; }

    public DeviceState getNewState() { return newState; }
    
    public List<String> getTouchedDevicesIDs() { return List.of(devID); }

    public long getLength() { return 1; }
}
