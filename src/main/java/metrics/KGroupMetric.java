package metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import deploy.KGroupManager;
import network.Network;

public class KGroupMetric {
    private enum OperationKind {
        QUORUM,
        LEADER_ELECTION,
        STATE_TRANSFER;
    }

    private static KGroupMetric kGroupMetricSingleton = new KGroupMetric();

    private class OperationMetric {
        private long startTS, endTS = -1;
        private List<String> members = new ArrayList<>();
        private String leaderID;
        private final List<Integer> seqNos = new ArrayList<>();

        public OperationMetric(KGroupManager mngr, long startTS, int seqNo) {
            members.addAll(mngr.getCurMemberIDs());
            this.startTS = startTS;
            seqNos.add(seqNo);
        }

        public boolean containsSeqNo(int seqNo) { return seqNos.contains(seqNo); }

        public void addSeqNo(int seqNo) { seqNos.add(seqNo); }

        public long recordEndTS(long endTS, String leaderID) {
            this.endTS = endTS;
            this.leaderID = String.valueOf(leaderID);
            return getDelay();
        }

        public boolean existsEnd() { return endTS != -1; }

        public long getDelay() { return endTS - startTS; }

        public String toString() {
            return String.join(" ", members) + "," + leaderID + "," + getDelay();
        }
    }

    private class KGroupMetricNode {
        private final Map<Integer, List<OperationMetric>> _quorumDelays = new ConcurrentHashMap<>();
        private final Map<Integer, List<OperationMetric>> _ldrElctnDelays = new ConcurrentHashMap<>();
        private final Map<Integer, List<OperationMetric>> _stateTrnsfrDelays = new ConcurrentHashMap<>();

        public KGroupMetricNode() {}

        public Map<Integer, List<OperationMetric>> getDelays(OperationKind kind) {
            switch (kind) {
            case QUORUM:
                return _quorumDelays;
            case LEADER_ELECTION:
                return _ldrElctnDelays;
            case STATE_TRANSFER:
                return _stateTrnsfrDelays;
            default:
                return null;
            }
        }

        public OperationMetric getMetric(
            int epochNo, int seqNo, Map<Integer, List<OperationMetric>> metrics
        ) {
            for (OperationMetric curr: metrics.get(epochNo)) {
                if (curr.containsSeqNo(seqNo)) return curr;
            }
            Network.log("seqNo not found in Operation metrics: " + seqNo);
            return null;
        }

        public List<String> stringifyDelays(OperationKind kind) {
            int epochNo;
            List<OperationMetric> opList;
            List<String> lines = new ArrayList<>();
            Map<Integer, List<OperationMetric>> delays = getDelays(kind);
            for (Entry<Integer, List<OperationMetric>> entry: delays.entrySet()) {
                epochNo = entry.getKey();
                opList = entry.getValue();
                for (OperationMetric opMetric: opList) {
                    if (opMetric.existsEnd())
                        lines.add(epochNo + "," + opMetric + "\n");
                }
            }
            return lines;
        }
    }

    private static final Map<KGroupManager, KGroupMetricNode> _data = new ConcurrentHashMap<>();

    private static Map<Integer, List<OperationMetric>> opChecks(
        OperationKind kind, KGroupManager mngr) {
        if (mngr == null) {
            Network.log("kgroupmetric mngr is null!!!!!");
        }
        if (!_data.containsKey(mngr)) {
            _data.put(mngr, kGroupMetricSingleton.new KGroupMetricNode());
        }

        Map<Integer, List<OperationMetric>> ops = _data.get(mngr).getDelays(kind);
        
        if (!ops.containsKey(mngr.getEpochNo())) {
            ops.put(mngr.getEpochNo(), new ArrayList<>());
        }

        return ops;
    }

    private static OperationMetric getOpMetric(
        KGroupManager mngr, int seqNo, Map<Integer, List<OperationMetric>> ops
    ) {
        return _data.get(mngr).getMetric(mngr.getEpochNo(), seqNo, ops);
    }

    private static void startOp(OperationKind kind, KGroupManager mngr, int seqNo, long startTS) {
        try {
            Map<Integer, List<OperationMetric>> ops = opChecks(kind, mngr);

            OperationMetric opMetric = kGroupMetricSingleton.new OperationMetric(mngr, startTS, seqNo);
            ops.get(mngr.getEpochNo()).add(opMetric);
        }
        catch (Exception e) {
            mngr.log("Start op exception stack trace:");
            e.printStackTrace();
        }
    }

    private static void addOpSeqNo(OperationKind kind, KGroupManager mngr, int oldSeqNo, int newSeqNo) {
        try {
            Map<Integer, List<OperationMetric>> ops = opChecks(kind, mngr);

            OperationMetric opMetric = getOpMetric(mngr, oldSeqNo, ops);
            opMetric.addSeqNo(newSeqNo);
        }
        catch (Exception e) {
            mngr.log("Add op seq no exception stack trace:");
            e.printStackTrace();
        }
    }

    private static long completeOp(OperationKind kind, KGroupManager mngr, int seqNo, long endTS) {
        if (mngr == null) {
            return -1L;
        }
        try {
            Map<Integer, List<OperationMetric>> ops = opChecks(kind, mngr);

            OperationMetric opMetric = getOpMetric(mngr, seqNo, ops);
            long length = opMetric.recordEndTS(endTS, mngr.getLeader());
            return length;
        }
        catch (Exception e) {
            mngr.log("Start op exception stack trace:");
            e.printStackTrace();
        }
        return -1L;
    }

    private static String stringifyDelays(OperationKind kind) {
        String text = "monitored,epochNo,members,leader,delay\n";
        List<String> monitored;
        List<String> stringDelays;
        for (Entry<KGroupManager, KGroupMetricNode> kgrpEntry: _data.entrySet()) {
            monitored = kgrpEntry.getKey().getMonitored();
            stringDelays = kgrpEntry.getValue().stringifyDelays(kind);
            for (String delay: stringDelays) {
                text += String.join(" ", monitored) + "," + delay;
            }
        }

        return text;
    }

    public static void startQuorum(KGroupManager mngr, int seqNo, long startTS) {
        startOp(OperationKind.QUORUM, mngr, seqNo, startTS);
    }

    public static void completeQuorum(KGroupManager mngr, int seqNo, long endTS) {
        completeOp(OperationKind.QUORUM, mngr, seqNo, endTS);
    }

    public static String stringifyQuorumDelays() {
        return stringifyDelays(OperationKind.QUORUM);
    }

    public static void startLdrElctn(KGroupManager mngr, int seqNo, long startTS) {
        startOp(OperationKind.LEADER_ELECTION, mngr, seqNo, startTS);
    }

    public static void addLdrElctnSeqNo(KGroupManager mngr, int oldSeqNo, int newSeqNo) {
        addOpSeqNo(OperationKind.LEADER_ELECTION, mngr, oldSeqNo, newSeqNo);
    }

    public static long completeLdrElctn(KGroupManager mngr, int seqNo, long endTS) {
        return completeOp(OperationKind.LEADER_ELECTION, mngr, seqNo, endTS);
    }

    public static String stringifyLdrElctnDelays() {
        return stringifyDelays(OperationKind.LEADER_ELECTION);
    }

    public static void startStateTrnsfr(KGroupManager mngr, int seqNo, long startTS) {
        if (mngr.getEpochNo() == 0) return;
        startOp(OperationKind.STATE_TRANSFER, mngr, seqNo, startTS);
    }

    public static void addStateTrnsfrSeqNo(KGroupManager mngr, int oldSeqNo, int newSeqNo) {
        if (mngr.getEpochNo() == 0) return;
        addOpSeqNo(OperationKind.STATE_TRANSFER, mngr, oldSeqNo, newSeqNo);
    }

    public static long completeStateTrnsfr(KGroupManager mngr, int seqNo, long endTS) {
        if (mngr.getEpochNo() == 0) return -1;
        return completeOp(OperationKind.STATE_TRANSFER, mngr, seqNo, endTS);
    }

    public static String stringifyStateTrnsfrDelays() {
        return stringifyDelays(OperationKind.STATE_TRANSFER);
    }
}
