package deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import configs.KGroupSelectionPolicy;
import configs.LeaderElectionPolicy;
import configs.LockStrategy;
import kgroup.DeviceKGroup;
import kgroup.RoutineKGroup;
import kgroup.state.DeviceLock;
import kgroup.state.LockRequest;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.lock.LockReleaseQuorumMessagePayload;
import network.message.payload.lock.LockRequestQuorumMessagePayload;
import network.message.payload.lock.LockedQuorumMessagePayload;
import network.message.payload.lock.PLockRequestQuorumMessagePayload;
import routine.Routine;

public class DeviceKGroupManager extends KGroupManager {
    private final Map<String, List<String>> rtnsToNotify = new HashMap<>();

    public DeviceKGroupManager(
        List<String> devClstr, DeviceKGroup devKGrp,
        List<RoutineKGroup> rtnKGrps, int F, int K,
        long epochLen, long mntrPeriod, long initDelay,
        KGroupSelectionPolicy kGrpSlctPlc, LeaderElectionPolicy ldrElctnPlc,
        LockStrategy lockStrategy, Map<String, Routine> rtns,
        TestType testType, float waitCoef
    ) {
        super(F, K, epochLen, mntrPeriod, initDelay,
              kGrpSlctPlc, ldrElctnPlc, lockStrategy, devKGrp, rtns,
              testType, waitCoef);
        this.rtnKGrps = rtnKGrps;
        for (Entry<String, Routine> rtn: rtns.entrySet()) {
            String rtnID = rtn.getKey();
            List<String> trigDevIDs = rtn.getValue().getTriggerDevIDs();
            for (String devID: trigDevIDs) {
                if (!devClstr.contains(devID)) continue;
                if (!rtnsToNotify.containsKey(devID)) {
                    rtnsToNotify.put(devID, new ArrayList<>());
                }
                rtnsToNotify.get(devID).add(rtnID);
            }
        }
        this.mntrTask = new MonitorDevices(this);
        timer.scheduleAtFixedRate(mntrTask, initDelay, mntrPeriod);
    }

    @Override
    public RoutineKGroup getRtnKGrp(String rtnID) {
        for (RoutineKGroup rtnKGrp: rtnKGrps) {
            if (rtnKGrp.isInChargeOf(rtnID)) {
                return rtnKGrp;
            }
        }
        return null;
    }

    @Override
    public RoutineKGroup getRtnKGrp(List<String> rtnIDs) {
        for (RoutineKGroup rtnKGrp: rtnKGrps) {
            if (rtnKGrp.isInChargeOf(rtnIDs)) {
                return rtnKGrp;
            }
        }
        return null;
    }

    @Override
    public DeviceKGroup getDevKGrp(String devID) {
        if (isInChargeOf(devID))
            return getDevKGrp();
        else return null;
    }

    @Override
    public DeviceKGroup getDevKGrp(List<String> devIDs) {
        if (isInChargeOf(devIDs))
            return getDevKGrp();
        else return null;
    }

    @Override
    public DeviceLock getDevState(String devID) {
        return getDevKGrp().getDevState(devID);
    }

    @Override
    public List<String> getRtnsToNotify(String devID) {
        return rtnsToNotify.getOrDefault(devID, new ArrayList<>());
    }

    @Override
    public boolean isNewLockRequestsEmpty(String devID) {
        return getDevKGrp().isNewLockRequestsEmpty(devID);
    }

    @Override
    public boolean isLockRequestReceived(String devID, String rtnID, int rtnSeqNo) {
        return getDevKGrp().isLockRequestReceived(devID, rtnID, rtnSeqNo);
    }

    @Override
    public boolean isLockRequestGranted(String devID, String rtnID, int rtnSeqNo) {
        return getDevKGrp().isLockRequestGranted(devID, rtnID, rtnSeqNo);
    }

    public boolean isLockAcquired(String devID, String rtnID, int rtnSeqNo) {
        return getDevKGrp().isLockAcquired(devID, rtnID, rtnSeqNo);
    }

    @Override
    public boolean isLocked(String devID) {
        return getDevKGrp().isLocked(devID);
    }

    @Override
    public boolean isLocker(String devID, String rtnID, int rtnSeqNo) {
        return getDevKGrp().isLocker(devID, rtnID, rtnSeqNo);
    }

    @Override
    public int getLockerReqSeqNo(String devID) {
        return getDevKGrp().getLockerReqSeqNo(devID);
    }

    @Override
    public void newMntrTask() {
        this.mntrTask = new MonitorDevices(this);
        try {
            timer.scheduleAtFixedRate(mntrTask, 0, mntrPeriod);
        } catch (IllegalStateException e) {
            log("Monitor task Illegal state exception", true);
        }
    }

    @Override
    public int addNewLockRequest(String devID, String rtnID, int rtnSeqNo) {
        return getDevKGrp().addNewLockRequest(devID, rtnID, rtnSeqNo);
    }

    @Override
    public int replicateNewLockRequest(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        return getDevKGrp().replicateNewLockRequest(devID, rtnID, rtnSeqNo, reqSeqNo);
    }

    @Override
    public int getLockReqSeqNo(String devID, String rtnID, int rtnSeqNo) {
        return getDevKGrp().getLockReqSeqNo(devID, rtnID, rtnSeqNo);
    }

    @Override
    public Map<Integer, LockRequest> getNewLockRequests(String devID) {
        return getDevKGrp().getNewLockRequests(devID);
    }

    @Override
    public void gatherQuorumForDevLockRequest(
        String rtnID, int rtnSeqNo, String devID, int reqSeqNo
    ) {
        MessagePayload payload = new LockRequestQuorumMessagePayload(
            epochNo, List.of(devID), rtnID, rtnSeqNo, reqSeqNo);
        quorum(MessageType.LOCK_REQUEST_QUORUM, payload, false, false);
    }

    @Override
    public void gatherQuorumForDevPLockRequest(
        String rtnID, int rtnSeqNo, String devID, int reqSeqNo
    ) {
        MessagePayload payload = new PLockRequestQuorumMessagePayload(
                epochNo, List.of(devID), rtnID, rtnSeqNo, reqSeqNo);
        quorum(MessageType.PLOCK_REQUEST_QUORUM, payload,
                false  /* oldKGroup */, false /* boundedWait */);
    }
    
    @Override
    public void lockRequestReplicated(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        getDevKGrp().lockRequestReplicated(devID, rtnID, rtnSeqNo, reqSeqNo);
    }

    @Override
    public int getLastLockedReqSeqNo(String devID) {
        return getDevKGrp().getLastLockedReqSeqNo(devID);
    }

    @Override
    public LockRequest getLockQueueHead(String devID) {
        return getDevKGrp().getQueueHead(devID);
    }

    @Override
    public void gatherQuorumForDevLockAcquisition(
        String rtnID, int rtnSeqNo, String devID, int reqSeqNo
    ) {
        MessagePayload payload = new LockedQuorumMessagePayload(
            epochNo, List.of(devID), rtnID, reqSeqNo, rtnSeqNo);
        quorum(MessageType.LOCKED_QUORUM, payload, false, false);
    }

    @Override
    public void gatherQuorumForDevLockAcquisition(String rtnID, int rtnSeqNo, String devID) {
        int reqSeqNo = getDevKGrp().getQueueHeadReqSeqNo(devID);
        gatherQuorumForDevLockAcquisition(rtnID, rtnSeqNo, devID, reqSeqNo);
    }

    @Override
    public void replicateLockAcquisition(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        getDevKGrp().replicateLockAcquisition(devID, rtnID, rtnSeqNo, reqSeqNo);
    }

    @Override
    public void replicateLockRelease(String devID, String rtnID, int rtnSeqNo) {
        getDevKGrp().replicateLockRelease(devID, rtnID, rtnSeqNo);
    }

    @Override
    public void lock(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        getDevKGrp().lock(devID, rtnID, rtnSeqNo, reqSeqNo);
    }

    @Override
    public void gatherQuorumForDevLockRelease(
        String rtnID, int rtnSeqNo, String devID, int reqSeqNo
    ) {
        MessagePayload payload = new LockReleaseQuorumMessagePayload(
            epochNo, List.of(devID), rtnID, rtnSeqNo, reqSeqNo
        );
        quorum(MessageType.LOCK_RELEASE_QUORUM, payload, false, false);
    }

    @Override
    public LockRequest releaseLock(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        return getDevKGrp().releaseLock(devID, rtnID, rtnSeqNo, reqSeqNo);
    }
}
