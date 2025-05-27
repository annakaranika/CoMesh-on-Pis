package deploy;

import java.util.TimerTask;

import network.Network;

public class UpdateDevState extends TimerTask {
    private final DeviceState state;

    public UpdateDevState(DeviceState state) {
        this.state = state;
    }

    @Override
    public void run() {
        try {
            Deployer.updateDevState(state);
        }
        catch (Exception e) {
            Network.log("Update dev state timer task exception", true);
            e.printStackTrace();
        }
    }
    
}
