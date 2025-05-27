package kgroup;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import configs.KGroupSelectionPolicy;
import configs.LeaderElectionPolicy;
import configs.LockStrategy;
import deploy.DeviceState;
import kgroup.state.RoutineStage;
import network.Network;

public class RoutineKGroup extends KGroup {
    private final Map<String, Integer> seqNos;
    private final Map<String, List<String>> touchedDevIDs;
    private final Map<String, DeviceState> oldDevStates = new ConcurrentHashMap<>();
    private final Map<String, Set<DeviceState>> oldDevStatesRecreation = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastTriggered;
    private final int minTriggerOffset;

    public RoutineKGroup(
        Map<String, List<String>> touchedDevIDsMap, int F, int K, int minTriggerOffset,
        KGroupSelectionPolicy kGrpSlctPlc, LeaderElectionPolicy ldrElctnPlc,
        LockStrategy lockStrategy, boolean debug
    ) {
        super(KGroupType.ROUTINE, F, K, ldrElctnPlc,
              lockStrategy, kGrpSlctPlc, debug);
        this.minTriggerOffset = minTriggerOffset;
        state = new ConcurrentHashMap<String, Object>();
        seqNos = new ConcurrentHashMap<String, Integer>();
        lastTriggered = new ConcurrentHashMap<>();
        for (String rtnID: touchedDevIDsMap.keySet()) {
            state.put(rtnID, new ConcurrentHashMap<Integer, RoutineStage>());
            seqNos.put(rtnID, 0);
            lastTriggered.put(rtnID, -minTriggerOffset);
        }
        this.touchedDevIDs = new HashMap<>();
        for (Entry<String, List<String>> touchedDevIDsEntry: touchedDevIDsMap.entrySet()) {
            Collections.sort(touchedDevIDsEntry.getValue());
            touchedDevIDs.put(
                touchedDevIDsEntry.getKey(), new ArrayList<>(touchedDevIDsEntry.getValue())
            );
        }
    }

    public List<String> getTouchedDevIDs(String rtnID) {
        return touchedDevIDs.get(rtnID);
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, RoutineStage> getRtnState(
                Map<String, Object> state, String rtnID) {
        if (state.get(rtnID) instanceof LinkedTreeMap) { // TODO: check
            Gson gson = new Gson();
            Map<Integer, RoutineStage> rtnState
                = gson.fromJson(gson.toJson(state.get(rtnID)),
                    new TypeToken<Map<Integer, RoutineStage>>(){}.getType());
            state.put(rtnID, rtnState);
        }
        return ((Map<Integer, RoutineStage>) state.get(rtnID));
    }

    public Map<Integer, RoutineStage> getRtnState(String rtnID) {
        return getRtnState(state, rtnID);
    }

    private RoutineStage getRtnStage(
                Map<String, Object> state, String rtnID, int rtnSeqNo) {
        return getRtnState(state, rtnID).get(rtnSeqNo);
    }

    public RoutineStage getRtnStage(String rtnID, int rtnSeqNo) {
        return getRtnStage(state, rtnID, rtnSeqNo);
    }

    // called by routine k-group leader
    public Set<Integer> getRtnSeqNos(String rtnID) {
        return getRtnState(rtnID).keySet();
    }

    // called by routine k-group leader
    @Override
    public Map<String, Integer> getSeqNos() {
        return seqNos;
    }

    // called by routine k-group leader
    @Override
    public void setSeqNos(Map<String, Integer> seqNos) {
        seqNos = new HashMap<String, Integer>();
        seqNos.putAll(seqNos);
    }

    // called by routine k-group leader
    @Override
    public void addLocalSeqNos(Map<String, Integer> localSeqNos) {
        for (String rtnID: getMonitored()) {
            if (localSeqNos.get(rtnID) > seqNos.get(rtnID)) {
                seqNos.put(rtnID, localSeqNos.get(rtnID));
            }
        }
    }

    // called by routine k-group leader
    @Override
    public Map<String, DeviceState> getOldDevStates() {
        return oldDevStates;
    }

    // called by routine k-group leader
    @Override
    public void setOldDevStates(Map<String, DeviceState> oldDevStates) {
        this.oldDevStates.clear();
        if (oldDevStates.size() > 0)
            this.oldDevStates.putAll(oldDevStates);
    }

    // called by routine k-group leader
    @Override
    public void addLocalOldDevStates(Map<String, DeviceState> localOldDevStates) {
        for (String devID: localOldDevStates.keySet()) {
            if (oldDevStatesRecreation.get(devID) == null) {
                oldDevStatesRecreation.put(devID, new HashSet<>());
            }
            oldDevStatesRecreation.get(devID).add(localOldDevStates.get(devID));
            if (Collections.frequency(
                    oldDevStatesRecreation.get(devID), localOldDevStates.get(devID)
                ) >= 1
            ) {
                oldDevStates.put(devID, localOldDevStates.get(devID));
            }
        }
    }

    // called by routine k-group leader
    public int setRtnTriggered(String rtnID) {
        if (lastTriggered.getOrDefault(rtnID, -minTriggerOffset) > Network.getCurTS() - minTriggerOffset) {
            return -1;
        }
        lastTriggered.put(rtnID, (int)Network.getCurTS());
        Network.log("Rtn " + rtnID + "-" + (seqNos.get(rtnID) + 1) + " triggered");
        // assign a sequence number to the new routine triggering, and return it later
        seqNos.put(rtnID, seqNos.get(rtnID) + 1);
        // create a new RoutineStage instance for the new triggering
        getRtnState(rtnID).put(seqNos.get(rtnID),
            new RoutineStage(lockStrategy, touchedDevIDs.get(rtnID)));
        return seqNos.get(rtnID);
    }

    // called by non-leader routine k-group member
    public void setRtnTriggered(String rtnID, int rtnSeqNo) {
        Network.log("Rtn " + rtnID + "-" + rtnSeqNo + " trigger replicated");
        // update the routine's sequence number
        seqNos.put(rtnID, rtnSeqNo);
        // create a new RoutineStage instance for the new triggering
        getRtnState(rtnID).put(rtnSeqNo,
            new RoutineStage(lockStrategy, touchedDevIDs.get(rtnID)));
        // System.out.println("Rtn " + rtnID + ": triggered, seqNo: " + seqNos.get(rtnID));
    }

    public String nextDevToLock(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return null;
        }
        return rtnStage.nextDeviceToLock(touchedDevIDs.get(rtnID));
    }

    /**** Starting utility functions for parallel locking strategy *****/
    public void acquiredDevLockForParallel(String rtnID, int rtnSeqNo, int reqSeqNo, String devID) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return;
        }
        rtnStage.acquireDeviceLockForParallel(devID, reqSeqNo);
    }

    public boolean acquiredAllLocksForParallel(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return false;
        }
       return rtnStage.acquiredAllLocksForParallel();
    }

    public int getPreLockDevSeqNo(String rtnID, int rtnSeqNo, String devID) {
        return getAllPreLockDevSeqNo(rtnID, rtnSeqNo).get(devID);
    }


    public Map<String, Integer> getAllPreLockDevSeqNo(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return null;
        }
       return rtnStage.getAllPreLockDevSeqNo();
    }

    public List<String> getAllDevIDsLockAcquiredNotGranted(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return null;
        }
       return rtnStage.getDevIDsLockAcquiredNotGranted();
    }

    public List<String> getAllUnreleasedDevIDs(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return null;
        }
       return rtnStage.getUnreleasedDevIDs();
    }

    public void removePreLockRecord(String rtnID, int rtnSeqNo, String devID) {
        getRtnStage(rtnID, rtnSeqNo).resetSingleDevPreLock(devID);
    }

    public boolean arePreLocksEmpty(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return false;
        }
       return rtnStage.arePreLocksEmpty();
    }

    public boolean acquiredDevLockForRtn(
        String rtnID, int rtnSeqNo, String devID, List<String> touchedDevicesIDs
    ) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return false;
        }
        boolean rv = rtnStage.acquiredDevLock(devID);
        if (lockStrategy == LockStrategy.SERIAL && rv
            && devID.equals(Collections.max(touchedDevicesIDs))
        ) {
            rtnStage.acquiredAllDevLocks();
        }

        return rv;
    }

    public void lockAcquireFailedForRtn(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return;
        }
       rtnStage.lockAcquireFailed();
    }

    public void releasedDevLockForRtn(String rtnID, int rtnSeqNo, String devID) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return;
        }
       rtnStage.releasedLock(devID);
    }

    public boolean isLockAcquireFailed(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return false;
        }
       return rtnStage.isLockAcquireFailed();
    }

    public void startAcquiringLocksForRtn(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return;
        }
       rtnStage.startAcquiringLocks();
    }

    public void startRtnExecution(String rtnID, int rtnSeqNo, long endTS) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return;
        }
       rtnStage.startExecution(endTS);
    }

    public void storeOldDevState(String devID, DeviceState oldState) {
        oldDevStates.put(devID, oldState);
    }

    public DeviceState getOldDevState(String devID) {
        return oldDevStates.get(devID);
    }

    public void startReleasingRtnLocks(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return;
        }
       rtnStage.startReleasingLocks();
    }

    public boolean isRtnAcquiringLocks(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return false;
        }
        return rtnStage.isAcquiringLocks();
    }

    public boolean areRtnLocksAcquired(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return false;
        }
       return rtnStage.areLocksAcquired();
    }

    public boolean isRtnExecuting(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return false;
        }
       return rtnStage.isExecuting();
    }

    public long getRtnEndTS(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return -1;
        }
       return rtnStage.getEndTS();
    }

    // called by non-leader routine k-group member
    public void setRtnExecuted(String rtnID, int rtnSeqNo) {
        Network.log("Rtn " + rtnID + "-" + rtnSeqNo + " execution replicated");
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return;
        }
       rtnStage.setExecuted();
    }

    public boolean isRtnReleasingLocks(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return false;
        }
       return rtnStage.isReleasingLocks();
    }

    public boolean areRtnLocksReleased(String rtnID, int rtnSeqNo) {
        RoutineStage rtnStage = getRtnStage(rtnID, rtnSeqNo);
        if (rtnStage == null) {
            return false;
        }
       return rtnStage.areLocksReleased();
    }

    public void removeRtnStage(String rtnID, int rtnSeqNo) {
        getRtnState(rtnID).remove(rtnSeqNo);
    }

    // called by non-leader routine k-group member
    public void setRtnLocksReleased(String rtnID, int rtnSeqNo) {
        Network.log("Rtn " + rtnID + "-" + rtnSeqNo + " locks released replicated");
        removeRtnStage(rtnID, rtnSeqNo);
    }

    @Override
    public void setState(Map<String, Object> state) {
        this.state = new HashMap<>();
        for (String rtnID: state.keySet()) {
            this.state.put(rtnID, new HashMap<Integer, RoutineStage>());
            Map<Integer, RoutineStage> curRtnStates = getRtnState(rtnID);
            Map<Integer, RoutineStage> tgtRtnStates = getRtnState(state, rtnID);
            for (Integer rtnSeqNo: tgtRtnStates.keySet()) {
                curRtnStates.put(rtnSeqNo,
                    new RoutineStage(tgtRtnStates.get(rtnSeqNo), lockStrategy));
            }
        }
    }

    @Override
    public void addLocalState(Map<String, Object> localState) {
        countReceivedLocalStates ++;
        for (String rtnID: getMonitored()) {
            for (Integer rtnSeqNo: getRtnState(localState, rtnID).keySet()) {
                if (getRtnStage(rtnID, rtnSeqNo) == null) {
                    getRtnState(rtnID).put(rtnSeqNo, new RoutineStage());
                }
                getRtnStage(rtnID, rtnSeqNo).addLocalState(
                    getRtnStage(localState, rtnID, rtnSeqNo), F);
            }
        }
    }
}
