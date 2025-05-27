package metrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import network.Network;

public class LoadMetric {
    private static final LoadMetric loadMetricSingleton = new LoadMetric();
    private static final Runtime run = Runtime.getRuntime();
    private static final DecimalFormat df = new DecimalFormat("#.##");
    private class NodeLoad {
        private double _memLoad;
        private double _cpuLoad;

        private NodeLoad(double memLoad, double cpuLoad) {
            _memLoad = memLoad;
            _cpuLoad = cpuLoad;
        }
        
        private double getMemLoad() { return _memLoad; }
        
        private double getCpuLoad() { return _cpuLoad; }
    }

    private static final Map<String, SortedMap<Long, NodeLoad>> _nodeLoads = new ConcurrentHashMap<>();

    public LoadMetric() {}

    public static synchronized void recordLoad(String nodeID, long ts, double memLoad, double cpuLoad) {
        if (!_nodeLoads.containsKey(nodeID)) {
            _nodeLoads.put(nodeID, new TreeMap<Long, NodeLoad>());
        }
        _nodeLoads.get(nodeID).put(ts, loadMetricSingleton.new NodeLoad(memLoad, cpuLoad));
    }

    public static void recordLoad(double memLoad, double cpuLoad) {
        String nodeID = Network.getMyID();
        long ts = Network.getCurTS();
        recordLoad(nodeID, ts, memLoad, cpuLoad);
    }

    private static String getCmdOutput(Process pr) {
        String output = "Standard output:\n", s;
        try {
            BufferedReader stdOutput = new BufferedReader(new 
                    InputStreamReader(pr.getInputStream()));
            while ((s = stdOutput.readLine()) != null) {
                output += s + "\n";
            }

            output += "\nStandard error:\n";
            BufferedReader stdError = new BufferedReader(new 
                    InputStreamReader(pr.getErrorStream()));
            while ((s = stdError.readLine()) != null) {
                output += s + "\n";
            }
        }
        catch (IOException e) {
            Network.log("Command output retrieval failed!", true);
            e.printStackTrace();
        }
        catch (Exception e) {
            Network.log("Exception happened", true);
        }
        return output;
    }

    private static String runCmd(String cmd, String errorMsg) {
        Network.log("Executing '" + cmd + "'");
        Process pr = null;
        while (true) {
            try {
                pr = run.exec(cmd);
                if (pr.waitFor(500, TimeUnit.MILLISECONDS)) break;
            }
            catch (IOException e) {
                Network.log(errorMsg, true);
            }
            catch (InterruptedException e) {
                Network.log("InterruptedException", true);
            }
            catch (Exception e) {
                Network.log("Exception", true);
            }
            if (pr != null) pr.destroy();
        }
        return getCmdOutput(pr);
    }

    public static long getTotalMem() {
        String memCmdOutput;
        
        while (true) {
            memCmdOutput = runCmd("free -b", "Memory cmd failed!");
            memCmdOutput = memCmdOutput.split("\n")[2];
            if (memCmdOutput.split("\\s+")[0].equals("Mem:")) {
                return Long.valueOf(memCmdOutput.split("\\s+")[1]);
            }
        }
    }

    public static long getUsedMem() {
        String memCmdOutput;
        while (true) {
            memCmdOutput = runCmd("free -b", "Memory cmd failed!");
            memCmdOutput = memCmdOutput.split("\n")[2];
            if (memCmdOutput.split("\\s+")[0].equals("Mem:")) {
                return Long.valueOf(memCmdOutput.split("\\s+")[2]);
            }
        }
    }

    public static double getMemLoad(long usedMem, long totalMem) {
        return Double.valueOf((100d * usedMem) / totalMem);
    }

    public static double getMemLoad() {
        return Double.valueOf((100d * getUsedMem()) / getTotalMem());
    }

    public static double getCpuLoad() {
        double cpuLoad;
        String cpuCmdOutput;
        while (true) {
            cpuCmdOutput = runCmd("top -bn 1", "CPU cmd failed!");
            cpuCmdOutput = cpuCmdOutput.split("\n")[3];
            if (
                cpuCmdOutput.split("\\s+")[0].equals("%Cpu(s):") &&
                !cpuCmdOutput.split("\\s+")[7].equals("id,")
            ) {
                cpuLoad = 100 - Double.valueOf(cpuCmdOutput.split("\\s+")[7]);
                return Double.valueOf(df.format(cpuLoad));
            }
        }
    }

    public static double[] getResourceLoad() {
        double memLoad = -1.0, cpuLoad = -1.0;
        long pid = ProcessHandle.current().pid();

        String cmdOutput, resourceStr;
        while (true) {
            cmdOutput = runCmd("top -bn 1 -p " + pid, "top failed!");
            cmdOutput = cmdOutput.split("\n")[8];

            resourceStr = cmdOutput.split("\\s+")[8];
            if (resourceStr.contains(".")) {
                cpuLoad = Double.valueOf(resourceStr);
            }

            resourceStr = cmdOutput.split("\\s+")[9];
            if (resourceStr.contains(".")) {
                memLoad = Double.valueOf(resourceStr);
            }

            if (memLoad != -1.0 && cpuLoad != -1.0) {
                double[] pcs = {memLoad, cpuLoad};
                return pcs;
            }
        }
    }

    public static void recordLoad(String nodeID, long ts) {
        double[] pcs = getResourceLoad();
        double memLoad = pcs[0];
        double cpuLoad = pcs[1];
        
        recordLoad(nodeID, ts, memLoad, cpuLoad);
    }

    public static void recordLoad() {
        String nodeID = Network.getMyID();
        long ts = Network.getCurTS();
        recordLoad(nodeID, ts);
    }

    public static boolean existsLoadEntry(long ts) {
        String nodeID = Network.getMyID();
        if (!_nodeLoads.containsKey(nodeID)) return false;
        return _nodeLoads.get(nodeID).containsKey(ts);
    }
    
    public static NodeLoad getLoadEntry(long ts) {
        String nodeID = Network.getMyID();
        if (!_nodeLoads.containsKey(nodeID)) return null;
        return _nodeLoads.get(nodeID).getOrDefault(ts, null);
    }
    
    public static Map<String, SortedMap<Long, NodeLoad>> getLoadHistory() { return _nodeLoads; }

    public static synchronized String stringifyLoadHistories() {
        String text = "nodeID,ts,mem_load,cpu_load\n";
        
        String nodeID;
        long ts;
        double memLoad, cpuLoad;
        
        for (Entry<String, SortedMap<Long, NodeLoad>> nodeLoad: _nodeLoads.entrySet()) {
            nodeID = nodeLoad.getKey();
            for (Entry<Long, NodeLoad> loadEntry: nodeLoad.getValue().entrySet()) {
                ts = loadEntry.getKey();
                memLoad = loadEntry.getValue().getMemLoad();
                cpuLoad = loadEntry.getValue().getCpuLoad();
                text += nodeID + "," + ts + "," + memLoad + "," + cpuLoad + "\n";
            }
        }
        return text;
    }
}
