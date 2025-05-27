package metrics;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import network.Network;

public class RoutineMetricSingleton {
    private static RoutineMetricSingleton singleton;

    private RoutineMetricSingleton() {}

    public static synchronized RoutineMetricSingleton getInstance() {
        if (RoutineMetricSingleton.singleton == null) {
            RoutineMetricSingleton.singleton = new RoutineMetricSingleton();
        }
        else {
            singleton._dev_records.clear();
        }
        return RoutineMetricSingleton.singleton;
    }

    class RtnTimeRecord {
        private long _pre_rtn_lock_release = -1;
        // trigger related time
        private long _trigger_usr_time = -1;
        private long _trigger_sys_time = -1;
        private long _trigger_ack_time = -1;
        // execution start related time
        private long _exe_start_time = -1;
        // rtn finish related time
        private long _lock_release_time = -1;


        public RtnTimeRecord() {}

        public void setPreRtnLockRelease(long time) { _pre_rtn_lock_release = time; }
        public void setTriggerUsrTime(long time) { _trigger_usr_time = time; }
        public void setTriggerSysTime(long time) { _trigger_sys_time = time; }
        public void setTriggerAckTime(long time) { _trigger_ack_time = time; }
        public void setStartExecutionTime(long time) { _exe_start_time = time; }
        public void setLockReleaseTime(long time) { _lock_release_time = time; }

        public long getTriggerUsrTime() { return _trigger_usr_time; }
        public long getTriggerSysTime() { return _trigger_sys_time; }
        public long getTriggerAckTime() { return _trigger_ack_time; }
        public long getStartExecutionTime() { return _exe_start_time; }
        public long getLockReleaseTime() { return _lock_release_time; }

        public long getClientDelay(String type) {
            // Get client delay with different trigger time.
            long target_trigger_time;
            if (type.equals("usr")) {
                target_trigger_time = _trigger_usr_time;
            } else if (type.equals("ack")) {
                target_trigger_time = _trigger_ack_time;
            } else {
                target_trigger_time = _trigger_sys_time;
            }

            if (target_trigger_time < 0 || _exe_start_time < 0) {
                return -1;
            } else if (target_trigger_time > _exe_start_time) {
                System.out.println("[ERROR] #TRCD trigger time later than execution start time!");
                return -2;
            } else {
                return _exe_start_time - target_trigger_time;
            }
        }

        public long getSyncDelay() {
            if (_pre_rtn_lock_release == -1) {
                return -1;
            }
            return _exe_start_time - _pre_rtn_lock_release;
        }

        public String stringForClientDelays() {
            return String.format("%d,%d,%d",
                    getClientDelay("usr"),
                    getClientDelay("sys"),
                    getClientDelay("ack"));
        }

        public String stringForSyncDelay() {
            return Long.toString(getSyncDelay());
        }
    }

    class DevTimeRecord {
        private long _release_req_time = -1;

        public DevTimeRecord() {}
        public void setReleaseReqTime(long time) { _release_req_time = time; }

        public long getReleaseReqTime() { return _release_req_time; }
    }

    private final HashMap<String, HashMap<Integer, RtnTimeRecord>> _all_records = new HashMap<>();
    private final HashMap<String, DevTimeRecord> _dev_records = new HashMap<>();

    public void recordTriggerUsrTime(String routine_id, int seq_no, long time) {
        if (!_all_records.containsKey(routine_id)) {
            _all_records.put(routine_id, new HashMap<>());
        }
        if (!_all_records.get(routine_id).containsKey(seq_no)) {
            _all_records.get(routine_id).put(seq_no, new RtnTimeRecord());
        }
        _all_records.get(routine_id).get(seq_no).setTriggerUsrTime(time);
    }

    public void recordTriggerSysTime(String routine_id, int seq_no, long time) {
        if (!_all_records.containsKey(routine_id)) {
            _all_records.put(routine_id, new HashMap<>());
        }
        if (!_all_records.get(routine_id).containsKey(seq_no)) {
            _all_records.get(routine_id).put(seq_no, new RtnTimeRecord());
        }
        _all_records.get(routine_id).get(seq_no).setTriggerSysTime(time);
    }

    public void recordTriggerAckTime(String routine_id, int seq_no, long time) {
        if (!_all_records.containsKey(routine_id)) {
            _all_records.put(routine_id, new HashMap<>());
        }
        if (!_all_records.get(routine_id).containsKey(seq_no)) {
            _all_records.get(routine_id).put(seq_no, new RtnTimeRecord());
        }
        _all_records.get(routine_id).get(seq_no).setTriggerAckTime(time);
    }

    public void recordStartExecutionTime(String routine_id, int seq_no, long time) {
        if (!_all_records.containsKey(routine_id)) {
            _all_records.put(routine_id, new HashMap<>());
        }
        if (!_all_records.get(routine_id).containsKey(seq_no)) {
            _all_records.get(routine_id).put(seq_no, new RtnTimeRecord());
        }
        _all_records.get(routine_id).get(seq_no).setStartExecutionTime(time);
    }

    public void recordRtnLockReleaseTime(String routine_id, int seq_no, long time) {
        if (!_all_records.containsKey(routine_id)) {
            _all_records.put(routine_id, new HashMap<>());
        }
        if (!_all_records.get(routine_id).containsKey(seq_no)) {
            _all_records.get(routine_id).put(seq_no, new RtnTimeRecord());
        }
        _all_records.get(routine_id).get(seq_no).setLockReleaseTime(time);
    }

    public void updateRtnPreRtnLockRelease(String routine_id, int seq_no, List<String> dev_ids) {
        // Get the last lock release time of routine_id touched devices
        long max_lock_release_ts = -1;
        for (String dev : dev_ids) {
            long release_ts = _dev_records.getOrDefault(dev, new DevTimeRecord()).getReleaseReqTime();
            max_lock_release_ts = Math.max(max_lock_release_ts, release_ts);
        }
        Network.log(
            "Routine " + routine_id + "-" + seq_no +
            " recording prelock time as " + max_lock_release_ts
        );
        // Record the this lock release time to routine.
        if (!_all_records.containsKey(routine_id)) {
            _all_records.put(routine_id, new HashMap<>());
        }
        if (!_all_records.get(routine_id).containsKey(seq_no)) {
            _all_records.get(routine_id).put(seq_no, new RtnTimeRecord());
        }
        _all_records.get(routine_id).get(seq_no).setPreRtnLockRelease(max_lock_release_ts);
    }

    public void printDevLockReleaseMap(List<String> dev_ids) {
        Network.log("Dev lock release time after recording:   ", true);
        for (String dev: dev_ids) {
            Network.log("   " + dev + ":" +
                    _dev_records.getOrDefault(dev, new DevTimeRecord()).getReleaseReqTime(), true);
        }
    }

    public void recordMultiDevLockReleaseTime(List<String> dev_ids, long time) {
        for (String dev: dev_ids) {
            recordDevLockReleaseTime(dev, time);
        }
    }

    public void recordDevLockReleaseTime(String dev_id, long time) {
        if (!_dev_records.containsKey(dev_id)) {
            _dev_records.put(dev_id, new DevTimeRecord());
        }
        _dev_records.get(dev_id).setReleaseReqTime(time);
    }

    public List<Long> getClientDelay() {
        return getClientDelay("sys");
    }

    public List<Long> getClientDelay(String type) {
        List<Long> delays = new ArrayList<>();
        for (Map.Entry<String, HashMap<Integer, RtnTimeRecord>> entry: _all_records.entrySet()) {
            for (Integer seq: entry.getValue().keySet()) {
                long time = entry.getValue().get(seq).getClientDelay(type);
                if (time == -1) {
                    System.out.println("[ERROR] #RMCLND Routine " + entry.getKey() + " seqNo " + seq + " lack of ts");
                } else if (time == -2) {
                    System.out.println("[ERROR] #RMCLND Routine " + entry.getKey() +
                            " seqNo " + seq + " execution ts earlier than trigger ts");
                } else {
                    delays.add(time);
                }
            }
        }
        return delays;
    }

    public List<Long> getSyncDelay() {
        List<Long> delays = new ArrayList<>();
        for (Map.Entry<String, HashMap<Integer, RtnTimeRecord>> entry: _all_records.entrySet()) {
            for (Integer seq: entry.getValue().keySet()) {
                long time = entry.getValue().get(seq).getSyncDelay();
                if (time == -1) {
                    System.out.println("[ERROR] #RMSYND Routine " + entry.getKey() + " seqNo " + seq + " lack of ts");
                } else {
                    delays.add(time);
                }
            }
        }
        return delays;
    }

    private <T> List<List<T>> transposeList(List<List<T>> list) {
        final int N = list.stream().mapToInt(l -> l.size()).max().orElse(-1);
        List<Iterator<T>> iterList = list.stream().map(it->it.iterator()).collect(Collectors.toList());
        return IntStream.range(0, N)
                .mapToObj(n -> iterList.stream()
                        .filter(it -> it.hasNext())
                        .map(m -> m.next())
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    public List<List<Long>> getClientDelayAllTypes(Boolean transpose) {
        if (transpose) { return transposeList(getClientDelayAllTypes()); }
        return getClientDelayAllTypes();
    }

    public List<List<Long>> getClientDelayAllTypes() {
        List<List<Long>> all_delays = new ArrayList<>();
        all_delays.add(getClientDelay("usr"));
        all_delays.add(getClientDelay("sys"));
        all_delays.add(getClientDelay("ack"));
        return all_delays;
    }

    public List<String> getClientDelaysInString() {
        List<String> delays = new ArrayList<>();
        for (Map.Entry<String, HashMap<Integer, RtnTimeRecord>> entry: _all_records.entrySet()) {
            for (Integer seq: entry.getValue().keySet()) {
                String delay = entry.getValue().get(seq).stringForClientDelays();
                delay = entry.getKey() + "," + seq + "," +  delay;
                delays.add(delay);
            }
        }
        return delays;
    }

    public List<String> getSyncDelayInString() {
        List<String> delays = new ArrayList<>();
        for (Map.Entry<String, HashMap<Integer, RtnTimeRecord>> entry: _all_records.entrySet()) {
            for (Integer seq: entry.getValue().keySet()) {
                String delay = entry.getValue().get(seq).stringForSyncDelay();
                delay = entry.getKey() + "," + seq + "," +  delay;
                delays.add(delay);
            }
        }
        return delays;
    }
}
