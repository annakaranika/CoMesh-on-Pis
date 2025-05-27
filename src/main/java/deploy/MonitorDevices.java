package deploy;

import java.util.TimerTask;

import network.Network;
import network.message.MessageType;
import network.message.payload.monitor.DeviceStateCheckMessagePayload;

public class MonitorDevices extends TimerTask {
    private final KGroupManager mngr;

    public MonitorDevices(KGroupManager mngr) {
        this.mngr = mngr;
    }

    public void run() {
        try {
            DeviceStateCheckMessagePayload payload = new DeviceStateCheckMessagePayload();
            for (String devID: mngr.getMonitored()) {
                mngr.remoteCall(devID, MessageType.DEVICE_STATE_CHECK, payload, false);
            }
        }
        catch (Exception e) {
            Network.log("Monitor devices timer task exception", true);
            e.printStackTrace();
        }
    }
}
