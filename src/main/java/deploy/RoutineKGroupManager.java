package deploy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import configs.KGroupSelectionPolicy;
import configs.LeaderElectionPolicy;
import configs.LockStrategy;
import kgroup.DeviceKGroup;
import kgroup.KGroupType;
import kgroup.RoutineKGroup;
import kgroup.state.RoutineStage;
import network.Network;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.execution.DeviceCommandMessagePayload;
import network.message.payload.lock.LockReleaseRequestMessagePayload;
import network.message.payload.lock.LockRequestMessagePayload;
import network.message.payload.routineStage.ExecutedMessagePayload;
import network.message.payload.routineStage.ReleasedLocksMessagePayload;
import network.message.payload.routineStage.TriggerQuorumMessagePayload;
import routine.Routine;

public class RoutineKGroupManager extends KGroupManager {
    private final Map<String, ReleaseLocks> lockReleaseTasks = new HashMap<>();

    public RoutineKGroupManager(
        Map<String, Routine> rtns, RoutineKGroup rtnKGrp, List<DeviceKGroup> devKGrps,
        int F, int K, long epochLen, long mntrPeriod, long initDelay,
        KGroupSelectionPolicy kGrpSlctPlc, LeaderElectionPolicy ldrElctnPlc,
        LockStrategy lockStrategy,
        TestType testType, float waitCoef
    ) {
        super(F, K, epochLen, mntrPeriod, initDelay,
              kGrpSlctPlc, ldrElctnPlc, lockStrategy, rtnKGrp, rtns,
              testType, waitCoef);
        this.devKGrps = devKGrps;
    }

    @Override
    public RoutineKGroup getRtnKGrp(String rtnID) {
        if (isInChargeOf(rtnID))
            return getRtnKGrp();
        else return null;
    }

    @Override
    public RoutineKGroup getRtnKGrp(List<String> rtnIDs) {
        if (isInChargeOf(rtnIDs))
            return getRtnKGrp();
        else return null;
    }

    @Override
    public DeviceKGroup getDevKGrp(String devID) {
        for (DeviceKGroup devKGrp: devKGrps) {
            if (devKGrp.isInChargeOf(devID)) {
                return devKGrp;
            }
        }
        return null;
    }

    @Override
    public DeviceKGroup getDevKGrp(List<String> devIDs) {
        for (DeviceKGroup devKGrp: devKGrps) {
            if (devKGrp.isInChargeOf(devIDs)) {
                return devKGrp;
            }
        }
        return null;
    }

    @Override
    public Map<Integer, RoutineStage> getRtnState(String rtnID) {
        return getRtnKGrp().getRtnState(rtnID);
    }

    @Override
    public List<String> getTouchedDevIDs(String rtnID) {
        return getRtnKGrp().getTouchedDevIDs(rtnID);
    }

    @Override
    public List<String> getAllUnreleasedDevIDs(String rtnID, int rtnSeqNo) {
        return getRtnKGrp().getAllUnreleasedDevIDs(rtnID, rtnSeqNo);
    }

    @Override
    public Map<String, Integer> getSeqNos() {
        return kgroup.getSeqNos();
    }

    @Override
    public void setSeqNos(Map<String, Integer> seqNos) {
        kgroup.setSeqNos(seqNos);
    }

    @Override
    public void addLocalSeqNos(Map<String, Integer> localSeqNos) {
        kgroup.addLocalSeqNos(localSeqNos);
    }

    @Override
    public Set<Integer> getRtnSeqNos(String rtnID) {
        return getRtnKGrp().getRtnSeqNos(rtnID);
    }

    @Override
    public boolean isRtnTriggered(String rtnID) {
        return rtns.get(rtnID).isTriggered(Deployer.getDevStates(getRtn(rtnID)));
    }

    @Override
    public void gatherQuorumForRtnTrigger(String rtnID, int rtnSeqNo) {
        Deployer.recordTriggerSysTime(rtnID, rtnSeqNo);
        if (rtnID == null) log("routine id is null!!!!!");
        MessagePayload payload = new TriggerQuorumMessagePayload(epochNo, rtnID, rtnSeqNo);
        quorum(MessageType.TRIGGER_QUORUM, payload, false, false);
    }
    
    @Override
    public boolean isRtnAcquiringLocks(String rtnID, int rtnSeqNo) {
        return getRtnKGrp().isRtnAcquiringLocks(rtnID, rtnSeqNo);
    }
    
    @Override
    public boolean isLockAcquireFailed(String rtnID, int rtnSeqNo) {
        return getRtnKGrp().isLockAcquireFailed(rtnID, rtnSeqNo);
    }

    @Override
    public boolean areRtnLocksAcquired(String rtnID, int rtnSeqNo) {
        return getRtnKGrp().areRtnLocksAcquired(rtnID, rtnSeqNo);
    }

    @Override
    public boolean isRtnExecuting(String rtnID, int rtnSeqNo) {
        return getRtnKGrp().isRtnExecuting(rtnID, rtnSeqNo);
    }

    @Override
    public boolean isRtnReleasingLocks(String rtnID, int rtnSeqNo) {
        return getRtnKGrp().isRtnReleasingLocks(rtnID, rtnSeqNo);
    }
    
    @Override
    public long getRtnEndTS(String rtnID, int rtnSeqNo) {
        return getRtnKGrp().getRtnEndTS(rtnID, rtnSeqNo);
    }

    @Override
    public int setRtnTriggered(String rtnID) {
        return getRtnKGrp().setRtnTriggered(rtnID);
    }

    @Override
    public void setRtnTriggered(String rtnID, int rtnSeqNo) {
        getRtnKGrp().setRtnTriggered(rtnID, rtnSeqNo);
    }

    @Override
    public void requestDevLockForRtn(String rtnID, int rtnSeqNo) {
        String devID = getRtnKGrp().nextDevToLock(rtnID, rtnSeqNo);
        
        if (devID == null) {
            return;
        }
        log("Next device for routine " + rtnID + "-" + rtnSeqNo + " to lock: " + devID, true);
        
        LockRequestMessagePayload payload;
        payload = new LockRequestMessagePayload(epochNo, List.of(devID), rtnID, rtnSeqNo);
        remoteCall(KGroupType.DEVICE, devID, MessageType.LOCK_REQUEST, payload, false);
    }

    @Override
    public void requestDevLockForRtnInParallel(String rtnID, int reqSeqNo) {
        List<String> devToTouch = getRtnKGrp().getTouchedDevIDs(rtnID);
        for (String devID: devToTouch) {
            LockRequestMessagePayload payload =
                new LockRequestMessagePayload(epochNo, List.of(devID), rtnID, reqSeqNo);
            remoteCall(KGroupType.DEVICE, devID,
                    MessageType.PLOCK_REQUEST, payload, false);
        }
    }

    public void requestDevLockCancel(String rtnID, int reqSeqNo) {
        if (getRtnKGrp().arePreLocksEmpty(rtnID, reqSeqNo)) {
            getRtnKGrp().startAcquiringLocksForRtn(rtnID, reqSeqNo);
            log("Routine " + rtnID + "-" + reqSeqNo + " all dev cancelled.");
            Map<Integer, RoutineStage> stages = getRtnState(rtnID);
            log("    local state: " + stages.get(reqSeqNo).toString());
            requestDevLockForRtnInParallel(rtnID, reqSeqNo);
        }
    }

    @Override
    public boolean acquiredDevLockForRtn(
        String rtnID, int rtnSeqNo, String devID, List<String> touchedDevicesIDs
    ) {
        return getRtnKGrp().acquiredDevLockForRtn(rtnID, rtnSeqNo, devID, touchedDevicesIDs);
    }

    @Override
    public void cancelLockReleases() {
        for (ReleaseLocks task: lockReleaseTasks.values()) {
            task.cancel();
        }
    }

    public void actuateCmd(String devID, DeviceState newState, String rtnID) {
        MessagePayload payload = new DeviceCommandMessagePayload(newState, rtnID);
        remoteCall(devID, MessageType.DEVICE_COMMAND, payload, false);
    }

    @Override
    public void schedLockRelease(String rtnID, int rtnSeqNo, long delay) {
        lockReleaseTasks.put(rtnID, new ReleaseLocks(this, rtnID, rtnSeqNo));
        try {
            timer.schedule(lockReleaseTasks.get(rtnID), delay);
        } catch (IllegalStateException e) {
            Network.log("RoutineKGroupManager: lock release scheduling failed", true);
            // System.exit(-1);
        }
    }

    @Override
    public void startRtnExec(String rtnID, int rtnSeqNo) {
        Deployer.recordStartExecTime(rtnID, rtnSeqNo);
        long rtnLength = getRtn(rtnID).getLength();
        long rtnEnd = Network.getCurTS() + rtnLength;
        getRtnKGrp().startRtnExecution(rtnID, rtnSeqNo, rtnEnd);

        for (String devID: getRtnKGrp().getTouchedDevIDs(rtnID)) {
            if (getRtn(rtnID).getLength() > 0)
                getRtnKGrp().storeOldDevState(devID, Deployer.getDevState(devID));
            actuateCmd(devID, getRtn(rtnID).getNewState(devID), rtnID);
        }

        schedLockRelease(rtnID, rtnSeqNo, rtnLength);
        List<String> touchedDevIDs = getTouchedDevIDs(rtnID);
        Deployer.updateRtnPreRtnLockRelease(rtnID, rtnSeqNo, touchedDevIDs);
        Deployer.printDevLockReleaseMap(touchedDevIDs);
    }

    @Override
    public void replicateRtnExec(String rtnID, int rtnSeqNo) {
        MessagePayload payload = new ExecutedMessagePayload(epochNo, rtnID, rtnSeqNo);
        quorum(MessageType.EXECUTED, payload, false, false);
    }

    @Override
    public void finishRtnExec(String rtnID, int rtnSeqNo) {
        if (getRtn(rtnID).getLength() > 0) {
            for (String devID: getRtnKGrp().getTouchedDevIDs(rtnID)) {
                actuateCmd(devID, getRtnKGrp(rtnID).getOldDevState(devID), rtnID);
            }
        }

        setRtnExecuted(rtnID, rtnSeqNo);

        Deployer.recordRtnLockReleaseTime(rtnID, rtnSeqNo);
        List<String> touchedDevIDs = getTouchedDevIDs(rtnID);
        Deployer.recordMultiDevLockReleaseTime(touchedDevIDs);
        log("Routine " + rtnID + "-" + rtnSeqNo + " finished execution, releasing devices "
            + getRtnKGrp().getTouchedDevIDs(rtnID));
        getRtnKGrp().startReleasingRtnLocks(rtnID, rtnSeqNo);
        for (String devID: getRtnKGrp().getTouchedDevIDs(rtnID)) {
            releaseDevLockForRtn(rtnID, rtnSeqNo, devID);
        }
    }

    @Override
    public void setRtnExecuted(String rtnID, int rtnSeqNo) {
        getRtnKGrp().setRtnExecuted(rtnID, rtnSeqNo);
    }

    @Override
    public void releaseDevLockForRtn(String rtnID, int rtnSeqNo, String devID) {
        LockReleaseRequestMessagePayload payload = new LockReleaseRequestMessagePayload(
            epochNo, List.of(devID), rtnID, rtnSeqNo);
        remoteCall(KGroupType.DEVICE, devID, MessageType.LOCK_RELEASE_REQUEST,
                   payload, false);
    }
    
    @Override
    public void lockReleased(String rtnID, int rtnSeqNo, String devID) {
        getRtnKGrp().releasedDevLockForRtn(rtnID, rtnSeqNo, devID);
    }

    @Override
    public boolean areRtnLocksReleased(String rtnID, int rtnSeqNo) {
        return getRtnKGrp().areRtnLocksReleased(rtnID, rtnSeqNo);
    }

    @Override
    public void replicateRtnLocksReleased(String rtnID, int rtnSeqNo) {
        MessagePayload payload = new ReleasedLocksMessagePayload(epochNo, rtnID, rtnSeqNo);
        quorum(MessageType.RELEASED_LOCKS, payload, false, false);
    }

    @Override
    public void removeRtnStage(String rtnID, int rtnSeqNo) {
        getRtnKGrp().removeRtnStage(rtnID, rtnSeqNo);
    }
}
