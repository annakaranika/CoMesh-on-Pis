package deploy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

import org.json.JSONObject;
import org.json.JSONArray;

import configs.*;
import kgroup.DeviceKGroup;
import kgroup.KMemberMix;
import kgroup.RoutineKGroup;
import metrics.*;
import network.*;
import network.message.Message;
import network.message.payload.MessagePayload;
import routine.*;
import routine.command.*;
import routine.statement.*;
import routine.statement.condition.*;

public class Deployer {
    public static String configsFn = "config.json";

    private static boolean debug;

    private static int seed;

    // topology info
    private static String devTopoDims, devLocsFn, devTopoFn, devClstrsFn, devSchedFn;
    private static DevClusterPolicy devClstrPlc;
    private static long devMntrPeriod;
    private static int devKGrpRng;
    private static String medleyOut;

    // smart node info
    private static String nodesListFn, nodesSchedFn;

    // routine info
    private static int rtnNo, rtnKGrpRng;
    private static long rtnMntrPeriod;
    private static String rtnsFn;

    // CoMesh info
    private static int K, F;
    private static long epochLen;
    private static KGroupSelectionPolicy kGrpSlctPlc;
    private static LSHParams lshParams;
    private static LeaderElectionPolicy ldrElctnPlc;
    private static LockStrategy lockStrategy;

    private static List<DeviceKGroup> devKGrps = new ArrayList<>();
    private static List<RoutineKGroup> rtnKGrps = new ArrayList<>();
    private static List<KGroupManager> mngrs = new ArrayList<>();

    // measurement info
    private static TestType testType;
    private static String outDir;
    private static NodeMetrics nodeMetric = null;
    private static RoutineMetricSingleton rtnMetric = RoutineMetricSingleton.getInstance();

    // experiment variables
    private static float waitCoef;
    private static long initTS, initDelay, expLength, membershipMntrPeriod, loadMntrPeriod;
    private static Timer timer;
    private static int minEpoch = 0, atMinEpoch = 0, minTriggerOffset = 0;
    private static String devVirtualID;

    private static Map<String, DeviceState> devStates = new ConcurrentHashMap<>();
    // the previous state for itself before last time it receives a DEVICE_STATE message from itself.
    // this will only be necessary if the current node is device leader
    private static DeviceState prevState = null;
    private static boolean nodeStatus;

    private static void parseConfigs() {
        String jsonText = null;
        try {
            InputStream is = new FileInputStream(configsFn);
            jsonText = IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject configs = new JSONObject(jsonText);

        debug = configs.getBoolean("debug");
        seed = configs.getInt("seed");

        devTopoDims = configs.getString("devTopoDims");
        devLocsFn = configs.getString("devLocsFn");
        devTopoFn = configs.getString("devTopoFn");
        devClstrPlc = configs.getEnum(DevClusterPolicy.class, "devClstrPlc");
        devClstrsFn = configs.optString("devClstrsFn");
        devSchedFn = configs.getString("devSchedFn");
        devKGrpRng = configs.optInt("devKGrpRng");
        devMntrPeriod = configs.getInt("devMntrPeriod");
        medleyOut = configs.getString("medleyOut");
        waitCoef = configs.getFloat("waitCoef");
        minTriggerOffset = configs.getInt("minTriggerOffset");

        nodesListFn = configs.getString("nodesListFn");
        nodesSchedFn = configs.getString("nodeSchedFn");

        rtnNo = configs.getInt("rtnNo");
        rtnsFn = configs.getString("rtnsFn");
        rtnMntrPeriod = configs.getInt("rtnMntrPeriod");
        rtnKGrpRng = configs.getInt("rtnKGrpRng");

        K = configs.getInt("K");
        F = configs.getInt("F");
        kGrpSlctPlc = configs.getEnum(KGroupSelectionPolicy.class, "kGrpSlctPlc");
        if (kGrpSlctPlc == KGroupSelectionPolicy.LSHMIX)
            lshParams = new LSHParams(seed, configs.optJSONObject("lshParams"));
        ldrElctnPlc = configs.getEnum(LeaderElectionPolicy.class, "ldrElctnPlc");
        lockStrategy = configs.getEnum(LockStrategy.class, "lockStrategy");
        epochLen = configs.getLong("epochLen");
        membershipMntrPeriod = configs.getLong("membershipMntrPeriod");

        testType = configs.getEnum(TestType.class, "testType");
        outDir = configs.getString("outDir") + devVirtualID + "/";;
        initDelay = configs.getLong("initDelay");
        expLength = configs.getLong("expLength");
        loadMntrPeriod = configs.getLong("loadMntrPeriod");
    }

    public static void initialize() {
        Network.log("-------------------------------------------> starting initialize");
        File theDir = new File(outDir);
        Network.log("Deleting output folder " + outDir);
        try {
            FileUtils.deleteDirectory(theDir);
            Network.log("Deleted output folder " + outDir);
        }
        catch (IOException e) {
            Network.log("Exception deleting output folder" + outDir, true);
        }
        if (!theDir.exists()) theDir.mkdirs();

        String logFn = outDir + "log.txt";
        File logFile = new File(logFn);
        FileWriter logger = null;
        try {
            if (logFile.exists()) logFile.delete();
            logFile.createNewFile();
            Network.log("Log file " + logFn + " created: " + logFile.exists());
            logger = new FileWriter(logFile);
        }
        catch (IOException e) {
            Network.log("Error creating logger at " + outDir, true);
        }

        initTS += initDelay;
        new Network(devVirtualID, devTopoFn, devLocsFn, medleyOut, debug, logger, initTS);
        List<String> devIDs = new ArrayList<>();
        devIDs.addAll(Network.getDevIDs());

        // Node initialization
        String[] line = {};
        List<String> nodesIDs = new ArrayList<>();
        Map<String, Integer> nodesLimits = new HashMap<>();
        try (Scanner sc = new Scanner(new File(nodesListFn))) {
            while (sc.hasNextLine()) {
                line = sc.nextLine().split(" ");
                nodesIDs.add(line[0]);
                nodesLimits.put(line[0], Integer.parseInt(line[1])); // should get rid of this
                // challenge: what to do if a node cannot handle more k-groups at some point
            }
        } catch (FileNotFoundException ex) {
            Network.log("Node list file " + nodesListFn + " does not exist", true);
            System.exit(-1);
        }

        timer = new Timer();
        long eventTS;
        String devID;
        DeviceState state;
        UpdateDevState updateDevState;
        try (Scanner sc = new Scanner(new File(devSchedFn))) {
            while (sc.hasNextLine()) {
                line = sc.nextLine().split(" ");
                devID = line[1];
                if (!Network.getMyID().equals(devID)) {
                    continue;
                }
                eventTS = (long) Double.parseDouble(line[0]);
                state = new DeviceState(line[2]);
                updateDevState = new UpdateDevState(state);
                timer.schedule(updateDevState, initDelay + eventTS);
            }
        }
        catch (FileNotFoundException e) {
            Network.log("Device schedule file " + devSchedFn + " does not exist", true);
            System.exit(-1);
        }
        
        nodeStatus = true;
        // Network.log("Nodes in the topology: " + nodesIDs);
        // if (!nodesIDs.contains(Network.getMyID())) {
        //     isNode = false;
        //     return;
        // }

        String myID = Network.getMyID();
        if (nodesIDs.contains(myID)) {
            List<List<String>> devClstrs = new ArrayList<>();
            if (devClstrPlc == DevClusterPolicy.LOCALITY) {
                try (Scanner sc = new Scanner(new File(devClstrsFn))) {
                    List<String> devClstr;
                    while (sc.hasNextLine()) {
                        devClstr = Arrays.asList(sc.nextLine().split(" "));
                        devClstrs.add(devClstr);
                        devKGrps.add(new DeviceKGroup(
                            devClstr, F, K, kGrpSlctPlc, ldrElctnPlc, lockStrategy, debug));
                    }
                } catch (FileNotFoundException ex) {
                    Network.log("Device clusters file " + devClstrsFn + " does not exist", true);
                    System.exit(-1);
                }
            }
            else { // random clusters
                List<String> devClstr;
                for (int counter = 0; counter < devIDs.size(); counter += devKGrpRng) {
                    int endIdx = Math.min(counter + devKGrpRng, devIDs.size());
                    devClstr = devIDs.subList(counter, endIdx);
                    devClstrs.add(devClstr);
                    devKGrps.add(new DeviceKGroup(
                        devClstr, F, K, kGrpSlctPlc, ldrElctnPlc, lockStrategy, debug));
                }
            }

            if (nodesIDs.size() < K) {
                Network.log("number of nodes and K : " + nodesIDs.size() + " " + K, true);
                Network.log("mission impossible!", true);
                System.exit(-1);
            }
            Network.log("Nodes: " + nodesIDs);

            String nodeID;
            UpdateNodeStatus updateNodeStatus;
            try (Scanner sc = new Scanner(new File(nodesSchedFn))) {
                while (sc.hasNextLine()) {
                    line = sc.nextLine().split(" ");
                    nodeID = line[2];
                    if (!nodeID.equals(Network.getMyID())) continue;
                    eventTS = Long.valueOf(line[0]);
                    boolean currNodeStatus = line[1].equals("f") ? false: true;
                    updateNodeStatus = new UpdateNodeStatus(currNodeStatus);
                    timer.schedule(updateNodeStatus, initDelay + eventTS);
                }
            } catch (FileNotFoundException ex) {
                Network.log("Node schedule file " + nodesSchedFn + " does not exist", true);
                System.exit(-1);
            }

            // Routine initialization
            Map<String, Routine> rtns = new HashMap<>();
            List<List<String>> rtnClstrs = new ArrayList<>();
            Map<String, List<String>> touchedDevIDsMap = new HashMap<>();

            List<List<String>> rtnTriggerDevs = new ArrayList<>();
            List<String> triggerDevs = new ArrayList<>();
            if (rtnNo != 0) {
                Routine rtn = null;
                try (Scanner sc = new Scanner(new File(rtnsFn))) {
                    String rtnID;
                    String rtnsDirName = rtnsFn.substring(0, rtnsFn.lastIndexOf("/") + 1);
                    while (sc.hasNext()) {
                        rtnID = sc.next();

                        String jsonText = null;
                        try {
                            InputStream is = new FileInputStream(rtnsDirName + rtnID + ".json");
                            jsonText = IOUtils.toString(is, "UTF-8");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        JSONObject rtnObj = new JSONObject(jsonText);
                        rtn = new DetailedRoutine(parseStatement(rtnObj.getJSONObject("conds")),
                                                parseCommand(rtnObj.getJSONArray("cmds")));
                        rtns.put(rtnID, rtn);
                    }
                } catch (FileNotFoundException ex) {
                    Network.log("Routine list file " + rtnsFn + " does not exist", true);
                    System.exit(-1);
                }
                
                Network.log("routines: " + rtns);

                List<String> rtnClstr;
                List<String> rtnIDs = new ArrayList<>(rtns.keySet());
                for (int counter = 0; counter < rtns.size(); counter += rtnKGrpRng) {
                    int endIdx = Math.min(counter + rtnKGrpRng, rtns.size());
                    rtnClstr = rtnIDs.subList(counter, endIdx);
                    rtnClstrs.add(rtnClstr);
                    touchedDevIDsMap.clear();
                    triggerDevs.clear();
                    for (String rtnID: rtnClstr) {
                        touchedDevIDsMap.put(rtnID, rtns.get(rtnID).getTouchedDevIDs());
                        triggerDevs.addAll(rtns.get(rtnID).getTriggerDevIDs());
                    }
                    rtnKGrps.add(new RoutineKGroup(touchedDevIDsMap, F, K, minTriggerOffset,
                                kGrpSlctPlc, ldrElctnPlc, lockStrategy, debug));
                    rtnTriggerDevs.add(triggerDevs);
                }
            }
            
            Network.log("has " + devKGrps.size() + " device k-groups: " + devKGrps);
            for (DeviceKGroup kgroup: devKGrps) {
                mngrs.add(new DeviceKGroupManager(
                    kgroup.getMonitored(), kgroup, rtnKGrps,
                    F, K, epochLen, devMntrPeriod, initDelay,
                    kGrpSlctPlc, ldrElctnPlc, lockStrategy, rtns,
                    testType, waitCoef));
            }

            for (RoutineKGroup rtnKGrp: rtnKGrps) {
                mngrs.add(new RoutineKGroupManager(rtns,
                    rtnKGrp, devKGrps, F, K, epochLen, rtnMntrPeriod, initDelay,
                    kGrpSlctPlc, ldrElctnPlc, lockStrategy,
                    testType, waitCoef));
            }

            if (kGrpSlctPlc.equals(KGroupSelectionPolicy.LSHMIX)) {
                KMemberMix kMemberMix = new KMemberMix(devTopoDims, F, K, lshParams,
                                    devLocsFn, devTopoFn, devClstrs, rtnTriggerDevs,
                                    nodesIDs, nodesLimits, rtns, myID);
                Network.setkMemberMix(kMemberMix);
                Network.getkMemberMix().getKMembersNewEpoch(0, Network.getMembershipList());
            }

            TimerTask membershipCheck = new MembershipCheck();
            TimerTask measureLoad = new MeasureLoad();
            timer.scheduleAtFixedRate(membershipCheck, initDelay, membershipMntrPeriod);
            timer.scheduleAtFixedRate(measureLoad, initDelay, loadMntrPeriod);
            nodeMetric = new NodeMetrics(myID, initTS);
        }
        Network.log("-------------------------------------------> " + myID + ": exiting initialize");
    }

    public static int getKGrpNo() { return rtnKGrps.size() + devKGrps.size(); }

    public static RoutineStatement parseStatement(JSONObject rj) {
        String k = rj.getString("kind");
        switch (k) {
        case "and":
            List<RoutineStatement> rStatements = new ArrayList<>();
            rStatements.add(parseStatement((JSONObject) rj.get("op1")));
            rStatements.add(parseStatement((JSONObject) rj.get("op2")));
            return new AndStatement(rStatements);
        case "or":
            rStatements = new ArrayList<>();
            rStatements.add(parseStatement((JSONObject) rj.get("op1")));
            rStatements.add(parseStatement((JSONObject) rj.get("op2")));
            return new OrStatement(rStatements);
        case "not":
            return new NotStatement(parseStatement((JSONObject) rj.get("op1")));
        default:
            return new Statement(parseCondition(rj));
        }
    }

    public static RoutineCondition parseCondition(JSONObject rj) {
        ConditionRelation relation = ConditionRelation.getCondFromVal(rj.getString("kind"));
        String op1 = rj.getString("op1");
        String value = rj.getString("op2");
        if (op1.equals("time")) {
            return new TimeCondition(relation, Float.valueOf(value));
        }
        else {
            return new DeviceStateCondition(op1, relation, value);
        }
    }

    public static SetOfCommands parseCommand(JSONArray rj) {
        List<Subroutine> rCommands = new ArrayList<>();
        for (int i = 0 ; i < rj.length(); i++) {
            JSONObject obj = rj.getJSONObject(i);
            if (obj.has("len")) {
                LengthyCommand rc = new LengthyCommand(obj.getString("actDevID"), obj.getString("newState"),
                obj.getLong("len"));
                rCommands.add(new Command(rc));
            }
            else {
                RoutineCommand rr = new RoutineCommand(obj.getString("actDevID"), obj.getString("newState"));
                rCommands.add(new Command(rr));
            }
        }
        return new SetOfCommands(rCommands);
    }

    public static List<KGroupManager> getAffectedManagers(String failedNodeID) {
        List<KGroupManager> affected = new ArrayList<>();
        for (KGroupManager mngr: mngrs) {
            if (mngr.isCurMember(failedNodeID)) {
                affected.add(mngr);
            }
        }
        return affected;
    }

    private static KGroupManager getResponsibleMngr(Message<? extends MessagePayload> msg) {
        return getResponsibleMngr(msg.getPayload());
    }

    private static KGroupManager getResponsibleMngr(MessagePayload payload) {
        for (KGroupManager mngr: mngrs) {
            if (mngr.isType(payload.getKGroupType())) {
                if (mngr.isInChargeOf(payload)) {
                    return mngr;
                }
            }
        }
        return null;
    }

    public static synchronized void updateMinEpoch(int newEpoch) {
        int allKGrps = getKGrpNo();
        // assumption: all k-groups' epoch # n should overlap with each other
        if (newEpoch == minEpoch) {
            atMinEpoch ++;
        }
        else if (newEpoch > minEpoch) {
            Network.log(" - epoch " + newEpoch + ": selecting new k-group members");
            Network.getkMemberMix().getKMembersNewEpoch(newEpoch, Network.getMembershipList());
        }
        if (atMinEpoch == allKGrps) {
            minEpoch ++;
            atMinEpoch = 0;
        }
    }

    public static DeviceState getDevState() {
        return getDevState(Network.getMyID());
    }

    public static boolean updatePrevDevState(DeviceState newState) {
        boolean diff;
        if (prevState == null) {
            diff = newState != null;
        } else {
            diff = !prevState.equals(newState);
        }
        devStates.put(Network.getMyID(), newState);
        prevState = newState;
        return diff;
    }

    public static DeviceState getDevState(String devID) {
        return devStates.getOrDefault(devID, new DeviceState("val0"));
    }

    public static Map<String, DeviceState> getDevStates(Routine rtn) {
        return rtn.getTriggerDevIDs().stream().distinct()
                  .collect(Collectors.toMap(x -> x, Deployer::getDevState));
    }

    public static boolean updateDevState(DeviceState newDevState) {
        return updateDevState(Network.getMyID(), newDevState);
    }

    public static boolean updateDevState(String devID, DeviceState newDevState) {
        DeviceState oldDevState = devStates.put(devID, newDevState);
        Network.log("Updated dev " + devID + "'s state from " + oldDevState + " to " + newDevState);
        if (oldDevState == null) {
            return newDevState != null;
        }
        return !oldDevState.equals(newDevState);
    }

    public static void updateNodeStatus(boolean newStatus) {
        Network.log("node status set to: " + newStatus);
        nodeStatus = newStatus;
    }

    private static void recvAndProcessMsgs() {
        Message<? extends MessagePayload> msg;
        KGroupManager mngr;
        
        while (Network.getCurTS() < expLength) {
            msg = null;
            Network.log("Checking for new messages");
            if (!Network.existUnreadMsgs()) {
                try {
                    // Thread.yield();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                continue;
            }
            // Network.log("New messages exist");

            for (KGroupManager _mngr: mngrs) {
                if (!_mngr.getWaitingMessageQueue().isEmpty() && _mngr.isReceivingEndOpen()) {
                    msg = _mngr.getWaitingMessageQueue().remove(0);
                    break;
                }
            }

            if (msg == null) {
                try {
                    msg = Network.recvMsg();
                } catch (NoSuchElementException e) {
                    Network.log(Network.existUnreadMsgs() + "\n\t" + Network.msgBuffer);
                }
            }

            if (!nodeStatus) {
                Network.log("node status: " + nodeStatus);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                continue;
            }
            
            mngr = getResponsibleMngr(msg);
            if (mngr != null) mngr.log("Received " + msg);
            else Network.log("Received " + msg);
            
            switch (msg.getType()) {
            case ELECTION:
            case ELECTION_ACK:
            case ELECTED:
            case ELECTED_ACK:
            case KGROUP_STATE_REQUEST:
            case KGROUP_STATE:
            case NOT_OLD_LEADER:
            case LOCAL_KGROUP_STATE_REQUEST:
            case LOCAL_KGROUP_STATE:
            case KGROUP_STATE_DISTRIBUTION:
            case KGROUP_STATE_ACK:
            case NODE_RECRUITEMENT_REQUEST:
            case NODE_RECRUITED:
                if (mngr == null) {
                    Network.log("TOP Msg received: referenced k-group in charge of "
                        + msg.getKGroupType() + " " + msg.getMonitored()
                        + " which does not exist!");
                    continue;
                }
                // mngr.log("Msg received from Node" + msg.src + " about " + kgroup.type + " kgroup " + kgroup.entitiesIDs + "=?=" + msg.payload.entitiesIDs + " with members " + kgroup.curNodesIDs);
                // }
            case NODE_FAILURE:
            case NODE_FAILURE_ACK:
            case DEVICE_STATE_CHECK:
            case DEVICE_STATE:
            case DEVICE_COMMAND:
            case DEVICE_COMMAND_ACK:
                msg.process(mngr);
                break;
            default:
                if (mngr == null) {
                    Network.log("DEFAULT Msg " + msg.getType() +
                        " received: referenced k-group in charge of " + msg.getKGroupType()
                        + " " + msg.getMonitored() + " which does not exist!");
                    continue;
                }
                if (mngr.isReceivingEndOpen()) {
                    msg.process(mngr);
                }
                else {
                    mngr.addToWaitingMessageQueue(msg);
                }
            }
        }
    }

    public static void startRtnRoles(NodeRole nodeRole, List<String> rtnIDs) {
        if (nodeMetric == null) return;
        nodeMetric.startRtnRoles(nodeRole, rtnIDs, Network.getCurTS());
    }

    public static void startDevRoles(NodeRole nodeRole, List<String> devIDs) {
        if (nodeMetric == null) return;
        nodeMetric.startDevRoles(nodeRole, devIDs, Network.getCurTS());
    }

    public static void finalizeRole() {
        finalizeRole(Network.getCurTS());
    }

    public static void finalizeRole(long expLength) {
        if (nodeMetric == null) return;
        nodeMetric.finishRtnRoles(expLength);
        nodeMetric.finishDevRoles(expLength);
    }

    public static String getRoleTimeInString() {
        if (nodeMetric == null) return "";
        return nodeMetric.getRoleTimeInString();
    }

    public static Map<Integer, Long> getRoleCountOverTime(NodeRole role) {
        if (nodeMetric == null) return new HashMap<>();
        return nodeMetric.getRoleCountOverTime(role);
    }

    public static void recordTriggerSysTime(String rtnID, int rtnSeqNo) {
        rtnMetric.recordTriggerSysTime(rtnID, rtnSeqNo, Network.getCurTS());
    }

    public static void recordTriggerAckTime(String rtnID, int rtnSeqNo) {
        rtnMetric.recordTriggerAckTime(rtnID, rtnSeqNo, Network.getCurTS());
    }

    public static void recordStartExecTime(String rtnID, int rtnSeqNo) {
        rtnMetric.recordStartExecutionTime(rtnID, rtnSeqNo, Network.getCurTS());
    }

    public static void updateRtnPreRtnLockRelease(
            String rtnID, int rtnSeqNo, List<String> touchedDevIDs) {
        rtnMetric.updateRtnPreRtnLockRelease(rtnID, rtnSeqNo, touchedDevIDs);
    }

    public static void printDevLockReleaseMap(List<String> touchedDevIDs) {
        rtnMetric.printDevLockReleaseMap(touchedDevIDs);
    }

    public static void recordRtnLockReleaseTime(String rtnID, int rtnSeqNo) {
        rtnMetric.recordRtnLockReleaseTime(rtnID, rtnSeqNo, Network.getCurTS());
    }

    public static void recordMultiDevLockReleaseTime(List<String> touchedDevIDs) {
        rtnMetric.recordMultiDevLockReleaseTime(touchedDevIDs, Network.getCurTS());
    }

    private static void cancelTimers() {
        // Network.log("Cancelling timers");
        timer.cancel();
        for (KGroupManager mngr: mngrs) {
            mngr.cancelTimer();
        }
        Network.log("Cancelled timers", nodeMetric != null);
    }

    public static void writeString(String outFn, String text) {
        File fout = new File(outFn);
        boolean written = false;
        while (!written)
            try {
                // Network.log("Checking whether " + outFn + " exists: " + fout.exists());
                if (!fout.exists()) {
                    // Network.log("Creating " + outFn);
                    fout.createNewFile();
                    // Network.log("Created " + outFn);
                }
                FileWriter writer = new FileWriter(fout);
                writer.write(text);
                writer.close();
                Network.log("Wrote file " + outFn, true);
                written = true;
            } catch (IOException e) {
                Network.log("IOException while writing results to " + outFn, true);
            } catch (ConcurrentModificationException e) {
                Network.log("ConcurrentModificationException while writing results to " + outFn, true);
            } catch (Exception e) {
                Network.log("Exception while writing results to " + outFn, true);
            }
    }

    private static void writeResults() {
        Network.log("Writing results", true);

        String outFn;
        File fout;
        FileWriter writer;

        Network.flushLog();
        Network.log("Wrote file " + outDir + "log.txt", nodeMetric != null);

        if (nodeMetric != null) {
            String delays = KGroupMetric.stringifyQuorumDelays();
            outFn = outDir + "quorum_delays.csv";
            writeString(outFn, delays);
            
            delays = KGroupMetric.stringifyLdrElctnDelays();
            outFn = outDir + "elctn_delays.csv";
            writeString(outFn, delays);
            
            delays = KGroupMetric.stringifyStateTrnsfrDelays();
            outFn = outDir + "state_delays.csv";
            writeString(outFn, delays);
            
            List<String> delayStrings = rtnMetric.getClientDelaysInString();
            Network.log("client delays: " + delays);
            delays = "RoutineID,SeqNo,ClientDelayUsr,ClientDelaySys,ClientDelayAck\n";
            delays += String.join("\n", delayStrings);
            outFn = outDir + "client_delays.csv";
            writeString(outFn, delays);
            
            delayStrings = rtnMetric.getSyncDelayInString();
            delays = "RoutineID,SeqNo,SyncDelay\n";
            delays += String.join("\n", delayStrings);
            outFn = outDir + "sync_delays.csv";
            writeString(outFn, delays);
            
            outFn = outDir + "balance.csv";
            try {
                fout = new File(outFn);
                if (!fout.exists()) { fout.createNewFile(); }
                writer = new FileWriter(fout);
                writer.write("id,leader,member,ld_and_mem,sing_leader,sing_member,sing_idle\n");
                writer.close();
                writer = new FileWriter(fout, true);
                // Align ending time across nodes for role time collection.
                finalizeRole();
                finalizeRole(expLength);
                writer.write(getRoleTimeInString() + "\n");
                writer.close();
                Network.log("Wrote file " + outFn, true);
            } catch (IOException e) {
                Network.log("An error occurred while creating file " + outFn + ".", true);
                e.printStackTrace();
            }

            // Record Role count over time.
            for (NodeRole role: List.of(NodeRole.LEADER, NodeRole.MEMBER, NodeRole.NON_IDLE)) {
                String fname = outFn.substring(0, outFn.length() - 4) + "_" +
                        role.name().toLowerCase() + ".csv";
                try {
                    fout = new File(fname);
                    if (!fout.exists()) { fout.createNewFile(); }
                    writer = new FileWriter(fout);
                    writer.write("num_role,time\n");
                    writer.close();
                    writer = new FileWriter(fout, true);

                    HashMap<Integer, Long> sum_count = new HashMap<>();
                    Map<Integer, Long> role_count = getRoleCountOverTime(role);
                    role_count.forEach((time, count) -> sum_count.merge(time, count, Long::sum));
                    for (Map.Entry<Integer, Long> entry: sum_count.entrySet()) {
                        if (entry.getValue() > 0) {
                            writer.write(entry.getKey() + "," + entry.getValue() + "\n");
                        }
                    }
                    writer.close();
                    Network.log("Wrote file " + fname, true);
                } catch (IOException e) {
                    Network.log("An error occurred while creating file " + fname + ".", true);
                    e.printStackTrace();
                }
            }
        
            String loadHistories = LoadMetric.stringifyLoadHistories();
            outFn = outDir + "load_histories.csv";
            writeString(outFn, loadHistories);
        }

        outFn = outDir + "bandwidth.csv";
        Network.metric.recordRawMsgData(outFn);
        // Network.metric.printAllMessageSummary();
        Network.log("Wrote file " + outFn, true);

        outFn = outDir + "bg_bandwidth.csv";
        Network.metric.recordRawMsgData(outFn);
        Network.log("Wrote file " + outFn, true);
    }

    public static void main(String[] args) {
        configsFn = args[0];
        devVirtualID = args[1];
        String myIP = Network.findMyIP();
        Network.setMyID(myIP, devVirtualID);
        Network.log("Using configs file " + configsFn, devVirtualID.equals("0000"));

        initTS = Long.parseLong(args[2]);
        try {
            while (System.currentTimeMillis() < initTS)
                Thread.sleep(100);
        } catch (Exception e) {
            Network.log("Sleep failed! Exiting...");
            e.printStackTrace();
        }

        parseConfigs();

        initialize();

        recvAndProcessMsgs();

        cancelTimers();

        writeResults();

        Network.log("Exiting", true);
    }
}
