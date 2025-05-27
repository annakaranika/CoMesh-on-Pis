package kgroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import configs.KGroupSelectionPolicy;
import configs.LeaderElectionPolicy;
import configs.LockStrategy;
import kgroup.state.DeviceLock;
import kgroup.state.LockRequest;

public class DeviceKGroup extends KGroup {
    public DeviceKGroup(
        List<String> devicesIDs, int F, int K,
        KGroupSelectionPolicy kGroupPolicy, LeaderElectionPolicy lep,
        LockStrategy ls, boolean debug) {
        super(KGroupType.DEVICE, F, K, lep, ls, kGroupPolicy, debug);

        state = new HashMap<String, Object>();
        for (String devID: devicesIDs) {
            state.put(devID, new DeviceLock());
        }
    }

    private DeviceLock getDevState(Map<String, Object> state, String devID) {
        if (state.get(devID) instanceof LinkedTreeMap) {
            Gson gson = new Gson();
            state.put(devID, gson.fromJson(gson.toJson(state.get(devID)), DeviceLock.class));
        }
        try {
            return (DeviceLock) state.get(devID);
        }
        catch (ClassCastException e) {
            System.out.println("DeviceLock ClassCastException: " + state.get(devID));
        }
        return null;
    }

    public DeviceLock getDevState(String devID) {
        return getDevState(this.state, devID);
    }

    // called by leader, or also by non-leader in parallel locking
    public boolean isLockRequestReceived(String devID, String rtnID, int rtnSeqNo) {
        return getDevState(devID).isLockRequestReceived(rtnID, rtnSeqNo);
    }

    // called by leader
    public boolean isLockAvailable(String devID) {
        return getDevState(devID).isAvailable();
    }

    // called by leader
    public boolean isLocked(String devID) {
        return getDevState(devID).isLocked();
    }

    // called by leader
    public int getQueueHeadReqSeqNo(String devID) {
        return getDevState(devID).getQueueHeadReqSeqNo();
    }

    // called by leader
    public LockRequest getQueueHead(String devID) {
        return getDevState(devID).getQueueHead();
    }

    public int getLockQueueSize(String devID) {
        return getDevState(devID).getQueueSize();
    }

    // called by leader
    public int getLockerReqSeqNo(String devID) {
        return getDevState(devID).getLockerReqSeqNo();
    }

    // call by leader
    public int getReqSeqNo(String devID, String rtnID, int rtnSeqNo) {
        return getDevState(devID).getLockReqSeqNo(rtnID, rtnSeqNo);
    }

    // called by leader
    public Map<Integer, LockRequest> getNewLockRequests(String devID) {
        return getDevState(devID).getNewLockRequests();
    }

    // call by leader; for PARALLEL Locking
    public Map<Integer, LockRequest> getNewPLockRequest(String devID) {
        return getDevState(devID).getNewPLockRequest();
    }

    // Util function for both leader/non-leader
    public Map<String, Map<Integer, LockRequest>> getAllNewLockRequests() {
        Map<String, Map<Integer, LockRequest>> res = new HashMap<>();
        for (String devID: state.keySet()) {
            res.put(devID, getNewLockRequests(devID));
        }
        return res;
    }

    // called by leader
    public LockRequest getLocker(String devID) {
        return getDevState(devID).getLocker();
    }

    public boolean isLocker(String devID, String rtnID, int rtnSeqNo) {
        return getDevState(devID).getLocker().contains(rtnID, rtnSeqNo);
    }

    // called by leader
    public boolean isLockAcquired(String devID, String rtnID, int rtnSeqNo) {
        return getDevState(devID).isAcquired(rtnID, rtnSeqNo);
    }

    // called by leader
    public boolean isLockQueueEmpty(String devID) {
        return getDevState(devID).isQueueEmpty();
    }

    // called by leader
    public boolean isNewLockRequestsEmpty(String devID) {
        return getDevState(devID).isNewLockRequestsEmpty();
    }

    // called by leader
    public boolean isLockRequestGranted(String devID, String rtnID, int rtnSeqNo) {
        return getDevState(devID).isLockRequestGranted(rtnID, rtnSeqNo);
    }

    // called by leader or non-leader upon receiving plock request
    public int addNewLockRequest(String devID, String rtnID, int rtnSeqNo) {
        return getDevState(devID).addNewRequest(rtnID, rtnSeqNo);
    }

    // called by non-leader
    public int replicateNewLockRequest(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        return getDevState(devID).replicateNewRequest(rtnID, rtnSeqNo, reqSeqNo);
    }

    // called by leader or non-leader upon receiving plock cancel
    public void removeLockRequest(
            String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        getDevState(devID).removeRequest(reqSeqNo, rtnID, rtnSeqNo);
    }

    // called by leader
    public int getLockReqSeqNo(String devID, String rtnID, int rtnSeqNo) {
        return getDevState(devID).getLockReqSeqNo(rtnID, rtnSeqNo);
    }

    // called by leader
    public int getLastLockedReqSeqNo(String devID) {
        return getDevState(devID).getLastLockedReqSeqNo();
    }

    // called by a non-leader upon request reception from the leader
    public void replicateLockRequest(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        getDevState(devID).replicateRequest(new LockRequest(rtnID, rtnSeqNo), reqSeqNo);
    }

    // called by leader
    public void lockRequestReplicated(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        getDevState(devID).requestReplicated(new LockRequest(rtnID, rtnSeqNo), reqSeqNo);
    }

    // called by non-leader
    public void replicateLockAcquisition(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        getDevState(devID).replicateLock(new LockRequest(rtnID, rtnSeqNo), reqSeqNo);
    }

    // called by leader
    public void lock(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        if (lockStrategy.equals(LockStrategy.SERIAL)) {
            getDevState(devID).lock(new LockRequest(rtnID, rtnSeqNo), reqSeqNo);
        } else {
            getDevState(devID).plock(rtnID, rtnSeqNo, reqSeqNo);
        }
    }

    // called by non-leader
    public void replicateLockRelease(String devID, String rtnID, int rtnSeqNo) {
        getDevState(devID).replicateRelease(new LockRequest(rtnID, rtnSeqNo));
    }

    // called by leader
    public LockRequest releaseLock(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        return getDevState(devID).release(new LockRequest(rtnID, rtnSeqNo), reqSeqNo);
    }

    @Override
    public void setState(Map<String, Object> state) {
        this.state = new HashMap<String, Object>();
        for (String devID: state.keySet()) {
            this.state.put(devID, new DeviceLock(getDevState(state, devID)));
        }
    }

    @Override
    public void addLocalState(Map<String, Object> localState) {
        countReceivedLocalStates ++;
        for (String devID: getMonitored()) {
            getDevState(devID).addLocalState(getDevState(localState, devID), F);
        }
    }
}
