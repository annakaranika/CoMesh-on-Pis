package deploy;

import java.util.TimerTask;

import java.util.Timer;
import metrics.LoadMetric;
import network.Network;

public class MeasureLoad extends TimerTask {
    private static long targetNo = -1;
    private static Timer timer;

    public void run() {
        try {
            if (targetNo == -1) {
                LoadMetric.recordLoad();
            }
            else {
                double memLoad, cpuLoad;
                memLoad = LoadMetric.getMemLoad();
                cpuLoad = LoadMetric.getCpuLoad();
                String myIP = Network.findMyIP();
                Network.setMyID(myIP);
                LoadMetric.recordLoad(myIP, System.currentTimeMillis(), memLoad, cpuLoad);
            }
        }
        catch (Exception e) {
            Network.log("Measure load timer task exception", true);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        TimerTask recorder = new MeasureLoad();
        timer = new Timer();
        timer.scheduleAtFixedRate(recorder, 1000, 1000);
        targetNo = 100;

        try {
            Thread.sleep(targetNo * 1000);
        } catch (InterruptedException e) {}

        recorder.cancel();
        timer.cancel();

        String outFn = "outputs/plain_loads/plain_load_hist.csv";
        String loadHistories = LoadMetric.stringifyLoadHistories();
        Deployer.writeString(outFn, loadHistories);
    }
}
