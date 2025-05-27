package deploy;

import java.util.TimerTask;

import network.Network;

public class UpdateNodeStatus extends TimerTask {
    private final boolean status;

    public UpdateNodeStatus(boolean status) {
        this.status = status;
    }

    @Override
    public void run() {
        try {
            Deployer.updateNodeStatus(status);
        }
        catch (Exception e) {
            Network.log("Update node status timer task exception", true);
            e.printStackTrace();
        }
    }
}
