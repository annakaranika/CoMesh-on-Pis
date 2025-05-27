package kgroup.state;

import configs.LockStrategy;
import network.Network;

import java.util.*;

// Have a different RoutineStage instance for each routine triggering
public class RoutineStage extends KGroupState {
    private static final long serialVersionUID = 3882050344443438437L;
    
    private String latestLockedDevID;
    private RoutineStageType stage;
    private long endTS;
    private LockStrategy lockStrategy = LockStrategy.SERIAL;
    private final List<String> unreleasedDevIDs = new ArrayList<>();

    // Devices that has not gotten the pre-lock.
    // Used for parallel lock strategy only.
    private final List<String> touchedDevIDs = new ArrayList<>();
    private final List<String> pendingDevIDs = new ArrayList<>();
    private final Map<String, Integer> preLockDevReqNo = new HashMap<>();
    private final List<String> lockedDevIDs = new ArrayList<>();

    private List<String> latestLockedDevIDRecreation = new ArrayList<>();
    private List<RoutineStageType> stageRecreation = new ArrayList<>();
    private List<String> unreleasedDevIDsRecreation = new ArrayList<>();
    private List<String> pendingDevIDsRecreation = new ArrayList<>();
    private List<Map.Entry<String, Integer>> preLockDevReqNoRecreation = new ArrayList<>();
    private List<String> lockedDevIDsRecreation = new ArrayList<>();

    public RoutineStage() {
        latestLockedDevID = null;
        stage = RoutineStageType.ACQUIRING_LOCKS;
        endTS = -1;
    }

    public RoutineStage(RoutineStage routineStage) {
        latestLockedDevID = routineStage.latestLockedDevID;
        stage = routineStage.stage;
        endTS = routineStage.endTS;
        touchedDevIDs.addAll(routineStage.touchedDevIDs);
        unreleasedDevIDs.addAll(routineStage.unreleasedDevIDs);
        pendingDevIDs.addAll(routineStage.pendingDevIDs);
        preLockDevReqNo.putAll(routineStage.preLockDevReqNo);
        lockedDevIDs.addAll(routineStage.lockedDevIDs);
    }

    public RoutineStage(LockStrategy ls, List<String> devs) {
        this();
        lockStrategy = ls;
        touchedDevIDs.addAll(devs);
        pendingDevIDs.addAll(devs);
    }

    public RoutineStage(RoutineStage routineStage, LockStrategy ls) {
        this(routineStage);
        lockStrategy = ls;
    }

    public String nextDeviceToLock(List<String> touchedDevicesIDs) {
        if (touchedDevicesIDs.isEmpty()) return null;
        Network.log(touchedDevicesIDs + ", last locked: " + latestLockedDevID);
        if (latestLockedDevID == null) {
            return touchedDevicesIDs.get(0);
        }
        else if (latestLockedDevID == Collections.max(touchedDevicesIDs)) {
            stage = RoutineStageType.ACQUIRED_LOCKS;
            return null;
        }
        else {
            for (String devID: touchedDevicesIDs) {
                if (devID.compareTo(latestLockedDevID) > 0) {
                    return devID;
                }
            }
        }
        return null;
    }

    public void acquireDeviceLockForParallel(String devID, int reqSeqNo) {
        preLockDevReqNo.put(devID, reqSeqNo);
        pendingDevIDs.remove(devID);
    }

    public void resetAllDevPreLocks(List<String> ids) {
        pendingDevIDs.clear();
        pendingDevIDs.addAll(ids);
        preLockDevReqNo.clear();
        lockedDevIDs.clear();
    }

    public void resetSingleDevPreLock(String dev) {
        if (!pendingDevIDs.contains(dev)) {
            pendingDevIDs.add(dev);
        }
        preLockDevReqNo.remove(dev);
    }

    public boolean arePreLocksEmpty() {
        return preLockDevReqNo.isEmpty();
    }

    public boolean acquiredAllLocksForParallel() {
        return pendingDevIDs.size() == 0;
    }

    public boolean acquiredDevLock(String devID) {
        // For PARALLEL locking
        if (lockStrategy.equals(LockStrategy.PARALLEL)) {
            if (!lockedDevIDs.contains(devID)) {
                lockedDevIDs.add(devID);
            }
            if (pendingDevIDs.isEmpty() && lockedDevIDs.size() == touchedDevIDs.size()) {
                acquiredAllDevLocks();
            }
            return true;
        }
        // For SERIAL locking
        if (latestLockedDevID == null || devID.compareTo(latestLockedDevID) > 0) {
            latestLockedDevID = devID;
            return true;
        }
        return false;
    }

    public void acquiredAllDevLocks() {
        stage = RoutineStageType.ACQUIRED_LOCKS;
        latestLockedDevID = null;
    }

    public void lockAcquireFailed() { stage = RoutineStageType.AC_LOCK_FAILED; }

    public void startAcquiringLocks() { stage = RoutineStageType.ACQUIRING_LOCKS; }

    public void startExecution(long endTS) {
        stage = RoutineStageType.EXECUTING;
        this.endTS = endTS;
    }

    public void setExecuted() {
        stage = RoutineStageType.RELEASING_LOCKS;
    }

    public void startReleasingLocks() {
        stage = RoutineStageType.RELEASING_LOCKS;
        unreleasedDevIDs.clear();
        unreleasedDevIDs.addAll(touchedDevIDs);
    }

    public void releasedLock(String devID) {
        unreleasedDevIDs.remove(devID);
    }

    public List<String> getUnreleasedDevIDs() {
        return unreleasedDevIDs;
    }

    public boolean isAcquiringLocks() {
        return stage == RoutineStageType.ACQUIRING_LOCKS;
    }

    public String getLatestLockedDevID() {
        return latestLockedDevID;
    }

    public boolean areLocksAcquired() {
        return stage == RoutineStageType.ACQUIRED_LOCKS;
    }

    public boolean isExecuting() {
        return stage == RoutineStageType.EXECUTING;
    }

    public long getEndTS() {
        return endTS;
    }

    public boolean isReleasingLocks() {
        return stage == RoutineStageType.RELEASING_LOCKS;
    }

    public boolean areLocksReleased() {
        return stage == RoutineStageType.RELEASING_LOCKS && unreleasedDevIDs.isEmpty();
    }

    public void addLocalState(RoutineStage localState, int f) {
        if (latestLockedDevID != localState.latestLockedDevID) {
            latestLockedDevIDRecreation.add(localState.latestLockedDevID);
            if (Collections.frequency(latestLockedDevIDRecreation, localState.latestLockedDevID) >= 1) {
                latestLockedDevID = localState.latestLockedDevID;
            }
        }
        if (stage != localState.stage) {
            stageRecreation.add(localState.stage);
            if (Collections.frequency(stageRecreation, localState.stage) >= 1) {
                stage = localState.stage;
            }
        }
        if (touchedDevIDs.size() == 0 && localState.touchedDevIDs.size() > 0) {
            touchedDevIDs.addAll(localState.touchedDevIDs);
        }
        if (pendingDevIDs != localState.pendingDevIDs) {
            pendingDevIDsRecreation.addAll(localState.pendingDevIDs);
            for (String devID: localState.pendingDevIDs) {
                if (pendingDevIDs.contains(devID)) { continue; }
                if (Collections.frequency(pendingDevIDsRecreation, devID) >= 1) {
                    pendingDevIDs.add(devID);
                }
            }
        }
        if (preLockDevReqNo != localState.preLockDevReqNo) {
            preLockDevReqNoRecreation.addAll(localState.preLockDevReqNoRecreation);
            for (Map.Entry<String, Integer> entry: localState.preLockDevReqNo.entrySet()) {
                if (preLockDevReqNo.containsKey(entry.getKey())) { continue; }
                if (Collections.frequency(preLockDevReqNoRecreation, entry) >= 1) {
                    preLockDevReqNo.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (lockedDevIDs != localState.lockedDevIDs) {
            lockedDevIDsRecreation.addAll(localState.lockedDevIDs);
            for (String devID: localState.lockedDevIDs) {
                if (lockedDevIDs.contains(devID)) { continue; }
                if (Collections.frequency(lockedDevIDsRecreation, devID) >= 1) {
                    lockedDevIDs.add(devID);
                }
            }
        }
        if (unreleasedDevIDs != localState.unreleasedDevIDs) {
            unreleasedDevIDsRecreation.addAll(localState.unreleasedDevIDs);
            for (String devID: localState.unreleasedDevIDs) {
                if (unreleasedDevIDs.contains(devID)) { continue; }
                if (Collections.frequency(lockedDevIDsRecreation, devID) >= 1) {
                    unreleasedDevIDs.add(devID);
                }
            }
        }
    }

    public String toString() {
        String lock_info;
        if (lockStrategy.equals(LockStrategy.SERIAL)) {
            lock_info = "latest locked device ID: " + latestLockedDevID
                      + ", unreleased devices: " + unreleasedDevIDs;
        } else {
            lock_info = "pre-locked: " + pendingDevIDs +
                    " locked-queue: " + preLockDevReqNo.keySet() +
                    " lock-confirmed: " + lockedDevIDs;
        }
        return "Stage: " + stage + ", " + lock_info;
    }

    public List<String> getPendingDevIDs() {
        return pendingDevIDs;
    }

    public Map<String, Integer> getAllPreLockDevSeqNo() {
        return preLockDevReqNo;
    }

    public List<String> getDevIDsLockAcquiredNotGranted() {
        List<String> res = new ArrayList<>(touchedDevIDs);
        res.removeAll(lockedDevIDs);
        return res;
    }

    public boolean isLockAcquireFailed() {
        return stage.equals(RoutineStageType.AC_LOCK_FAILED);
    }
}
