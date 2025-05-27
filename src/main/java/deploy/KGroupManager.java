package deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import configs.*;
import kgroup.*;
import kgroup.state.DeviceLock;
import kgroup.state.LockRequest;
import kgroup.state.RoutineStage;
import kgroup.state.StateTransferStage;
import metrics.KGroupMetric;
import metrics.NodeMetrics;
import metrics.NodeRole;
import network.*;
import network.message.*;
import network.message.payload.MessagePayload;
import network.message.payload.election.*;
import network.message.payload.execution.*;
import network.message.payload.failure.*;
import network.message.payload.lock.*;
import network.message.payload.monitor.*;
import network.message.payload.routineStage.*;
import network.message.payload.stateTransfer.*;
import routine.Routine;

public class KGroupManager {
    protected List<RoutineKGroup> rtnKGrps = null;
    protected List<DeviceKGroup> devKGrps = null;
    protected final KGroup kgroup;
    protected final int F, K;
    protected int epochNo = -1, _seqNo = -1;
    protected final long epochLen, mntrPeriod;
    protected final Map<String, Routine> rtns = new HashMap<>();
    protected final KGroupSelectionPolicy kGrpSlctPlc;
    protected final LeaderElectionPolicy ldrElctnPlc;
    protected final LockStrategy lockStrategy;
    protected TestType testType;
    public final Map<Integer, MsgInfo> msgsInfo = new ConcurrentHashMap<>();
    protected final Timer timer;
    protected TimerTask incrEpochTask;
    protected TimerTask mntrTask;
    protected final float waitCoef;
    protected final NodeMetrics nodeMetric;

    public KGroupManager(
        int F, int K, long epochLen, long mntrPeriod, long initDelay,
        KGroupSelectionPolicy kGrpSlctPlc, LeaderElectionPolicy ldrElctnPlc,
        LockStrategy lockStrategy, KGroup kgroup, Map<String, Routine> rtns,
        TestType testType, float waitCoef
    ) {
        this.F = F;
        this.K = K;
        this.kgroup = kgroup;
        this.epochLen = epochLen;
        this.mntrPeriod = mntrPeriod;

        this.kGrpSlctPlc = kGrpSlctPlc;
        this.ldrElctnPlc = ldrElctnPlc;
        this.lockStrategy = lockStrategy;
        this.rtns.putAll(rtns);

        timer = new Timer();
        incrEpochTask = new IncrementEpoch(this);
        timer.scheduleAtFixedRate(incrEpochTask, initDelay, epochLen);

        this.waitCoef = waitCoef;
        nodeMetric = new NodeMetrics(Network.getMyID(), Network.getCurTS());
    }

    public void log(String text) { Network.log(kgroup + " - " + text); }

    public void log(String text, boolean debug) { Network.log(kgroup + " - " + text, debug); }

    public void cancelTimer() {
        try {
            timer.cancel();
        } catch (IllegalStateException e) {
            Network.log("KGroupManager: timer cancel failed", true);
            // System.exit(-1);
        }
    }

    // k-group

    public KGroup getKGroup() {
        return kgroup;
    }

    public int getF() {
        return F;
    }

    public boolean isMemberOfKGroup() {
        return kgroup.isCurMember(getMyID());
    }

    public boolean isInChargeOf(String entityID) {
        return kgroup.isInChargeOf(entityID);
    }

    public boolean isInChargeOf(List<String> entityIDs) {
        return kgroup.isInChargeOf(entityIDs);
    }

    public boolean isInChargeOf(Message<? extends MessagePayload> msg) {
        return isInChargeOf(msg.getPayload());
    }

    public boolean isInChargeOf(MessagePayload payload) {
        return kgroup.isInChargeOf(payload.getMonitored());
    }

    public List<String> getMonitored() {
        return kgroup.getMonitored();
    }

    public List<String> getCurMemberIDs() {
        return kgroup.getCurMemberIDs();
    }

    public boolean isCurMember(String nodeID) {
        return getCurMemberIDs().contains(nodeID);
    }

    public boolean isCurMember() {
        return isCurMember(getMyID());
    }

    public List<String> getOldMemberIDs() {
        return kgroup.getOldMemberIDs();
    }

    public boolean isOldMember(String nodeID) {
        return getOldMemberIDs().contains(nodeID);
    }

    public boolean isOldMember() {
        return isOldMember(getMyID());
    }

    public int getCountFailedNodes() {
        return kgroup.getCountFailedNodes();
    }

    public KGroupType getType() {
        return kgroup.getType();
    }

    public boolean isType(KGroupType type) {
        return kgroup.isType(type);
    }

    public LeaderElectionStage getLdrElctnStg() {
        return kgroup.getLdrElctnStg();
    }

    public boolean isLdrElctnStg(LeaderElectionStage ldrElctnStg) {
        return kgroup.isLdrElctnStg(ldrElctnStg);
    }

    public void advLdrElctnStg(LeaderElectionStage ldrElctnStg) {
        kgroup.advLdrElctnStg(ldrElctnStg);
    }

    public long getLdrElctnTS() {
        return kgroup.getLdrElctnTS();
    }

    public void setLdrElctnTS(long ldrElctnTS) {
        kgroup.setLdrElctnTS(ldrElctnTS);
    }

    public boolean isStateTrnsfrStg(StateTransferStage stateTrnsfrStg) {
        return kgroup.isStateTrnsfrStg(stateTrnsfrStg);
    }

    public void advStateTrnsfrStg(StateTransferStage stateTrnsfrStg) {
        kgroup.advStateTrnsfrStg(stateTrnsfrStg);
    }

    public long getStateTrnsfrTS() {
        return kgroup.getStateTrnsfrTS();
    }

    public void setStateTrnsfrTS(long stateTrnsfrTS) {
        kgroup.setStateTrnsfrTS(stateTrnsfrTS);
    }

    public boolean isReceivingEndOpen() {
        return kgroup.isReceivingEndOpen();
    }

    public void closeReceivingEnd() {
        kgroup.closeReceivingEnd();
    }

    public void openReceivingEnd() {
        kgroup.openReceivingEnd();
    }

    public String getLeader() {
        return kgroup.getLeader();
    }

    public String getMostProbableLeader() {
        return kgroup.getMostProbableLeader(epochNo);
    }

    public boolean isLeader(String nodeID) {
        return getLeader().equals(nodeID);
    }

    public boolean isLeader() {
        return isLeader(getMyID());
    }

    public boolean isLeaderNull() {
        return getLeader() == null;
    }

    public void updateLeader(String leaderID) {
        kgroup.updateLeader(leaderID);
    }

    public String getOldLeader() {
        return kgroup.getOldLeader();
    }

    public boolean isOldLeader(String nodeID) {
        return getOldLeader().equals(nodeID);
    }

    public boolean isOldLeader() {
        return isOldLeader(getMyID());
    }

    public boolean isOldLeaderNull() {
        return getOldLeader() == null;
    }

    public String getMostProbableOldLeader() {
        return kgroup.getMostProbableOldLeader(epochNo);
    }

    public void updateOldLeader(String oldLdrID) {
        kgroup.updateOldLeader(oldLdrID);
    }

    public void removeOldLdrCnddt(String nodeID) {
        kgroup.removeOldLdrCnddt(nodeID);
    }

    public void removeOldLdrCnddt() {
        removeOldLdrCnddt(getMyID());
    }

    public boolean noOldLdrCnddts() {
        return kgroup.getOldLdrCnddts().isEmpty();
    }

    public void replaceFailedNodes() {
        List<String> replacementNodes = kgroup.replaceFailedNodes(epochNo);

        NodeRecruitmentRequestMessagePayload payload =
            new NodeRecruitmentRequestMessagePayload(epochNo, getType(), getState());
        
        for (String nodeID: replacementNodes) {
            remoteCall(nodeID, MessageType.NODE_RECRUITEMENT_REQUEST, payload, true);
        }
        kgroup.resetCountFailedNodes();
    }

    public Map<String, Object> getState() {
        return kgroup.getState();
    }

    public void setState(Map<String, Object> state) {
        kgroup.setState(state);
    }

    public void addLocalState(Map<String, Object> localState) {
        kgroup.addLocalState(localState);
    }

    public void addToWaitingMessageQueue(Message<? extends MessagePayload> msg) {
        kgroup.addToWaitingMessageQueue(msg);
    }

    public List<Message<? extends MessagePayload>> getWaitingMessageQueue() {
        return kgroup.getWaitingMessageQueue();
    }

    public void setWaitingMessageQueue(
        List<Message<? extends MessagePayload>> unprocessedMessages
    ) {
        kgroup.setWaitingMessageQueue(unprocessedMessages);
    }

    public Map<String, DeviceState> getDevStates(String rtnID) {
        return rtns.get(rtnID).getTriggerDevIDs()
                   .stream()
                   .distinct()
                   .collect(Collectors.toMap(x -> x, Deployer::getDevState));
    }

    // Device k-group

    protected DeviceKGroup getDevKGrp() { return (DeviceKGroup) kgroup; }

    public RoutineKGroup getRtnKGrp(String rtnID) { return null; }

    public RoutineKGroup getRtnKGrp(List<String> rtnID) { return null; }

    public boolean isNewLockRequestsEmpty(String devID) { return false; }

    public Map<Integer, LockRequest> getNewLockRequests(String devID) { return null; }

    public boolean isLocked(String devID) { return false; }

    public DeviceLock getDevState(String devID) { return null; }


    // Routine k-group

    protected RoutineKGroup getRtnKGrp() {
        return (RoutineKGroup) kgroup;
    }

    public DeviceKGroup getDevKGrp(String devID) { return null; }

    public DeviceKGroup getDevKGrp(List<String> devID) { return null; }

    public Map<String, Integer> getSeqNos() { return null; }

    public void setSeqNos(Map<String, Integer> seqNos) {}

    public void addLocalSeqNos(Map<String, Integer> localSeqNos) {}

    public DeviceState getOldDevState(String devID) { return null; }

    public Map<String, DeviceState> getOldDevStates() { return null; }

    public void setOldDevStates(Map<String, DeviceState> oldDevStates) {}

    public void addLocalOldDevStates(Map<String, DeviceState> localOldDevStates) {}

    public Set<Integer> getRtnSeqNos(String rtnID) { return null; }

    public List<String> getTouchedDevIDs(String rtnID) { return null; }

    public List<String> getAllUnreleasedDevIDs(String rtnID, int rtnSeqNo) { return null; }

    public boolean isRtnAcquiringLocks(String rtnID, int rtnSeqNo) { return false; }

    public boolean isLockAcquireFailed(String rtnID, int rtnSeqNo) { return false; }

    public boolean areRtnLocksAcquired(String rtnID, int rtnSeqNo) { return false; }

    public boolean isRtnExecuting(String rtnID, int rtnSeqNo) { return false; }

    public boolean isRtnReleasingLocks(String rtnID, int rtnSeqNo) { return false; }

    public long getRtnEndTS(String rtnID, int rtnSeqNo) { return -1; }

    public Map<Integer, RoutineStage> getRtnState(String rtnID) { return null; }

    // Network

    public static String getMyID() { return Network.getMyID(); }

    public Map<String, Membership> getMembershipList() { return Network.getMembershipList(); }

    public boolean isNodeOnline() { return isNodeOnline(getMyID()); }

    public boolean isNodeOnline(String nodeID) {
        return getMembershipList().get(nodeID) != Membership.FAILED;
    }

    public static int getDevNo() { return Network.getDevIDs().size(); }

    public int getRtnNo() { return rtns.size(); }

    // Messages

    public int getNextSeqNo() {
        // return ++ _seqNo;
        int nextSeqNo = Network.getNextSeqNo();
        // log("Got next seq no: " + nextSeqNo);
        return nextSeqNo;
    }

    public List<Integer> getUnicastMsgInfoForDevStateReq(String devID, int nextSeqNo) {
        List<Integer> relSeqNos = new ArrayList<>();
        int seqNo;
        MsgInfo msgInfo;
        for (Map.Entry<Integer, MsgInfo> msgInfoEntry: msgsInfo.entrySet()) {
            seqNo = msgInfoEntry.getKey();
            if (seqNo >= nextSeqNo) continue;
            msgInfo = msgInfoEntry.getValue();
            if (msgInfo instanceof QuorumMsgInfo) continue;
            if (!msgInfo.getMessageType().equals(MessageType.DEVICE_STATE)) continue;
            if (msgInfo.getMessagePayload().getMonitored().contains(devID)) {
                relSeqNos.add(seqNo);
            }
        }
        return relSeqNos;
    }

    public UnicastMsgInfo getUnicastMsgInfo(int seqNo) {
        return (UnicastMsgInfo) msgsInfo.get(seqNo);
    }

    public QuorumMsgInfo getQuorumMsgInfo(int seqNo) {
        return (QuorumMsgInfo) msgsInfo.get(seqNo);
    }

    public void removeMsgInfo(int seqNo) {
        // log("Removing msg seq no " + seqNo); // + " from " + msgsInfo.toString());
        MsgInfo msgInfo = msgsInfo.get(seqNo);
        if (msgInfo == null) return;
        msgInfo.cancelResendTask();
        msgsInfo.remove(seqNo);
    }

    public static Message<? extends MessagePayload> createMsg(
        String dst, MessageType type, MessagePayload payload, int msgSeqNo) {
        Message<? extends MessagePayload> msg = null;
        switch(type) {
        case DEVICE_COMMAND:
            msg = new Message<DeviceCommandMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (DeviceCommandMessagePayload) payload);
            break;
        case DEVICE_COMMAND_ACK:
            msg = new Message<DeviceCommandAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (DeviceCommandAckMessagePayload) payload);
            break;
        case DEVICE_KGROUP_LEADER_INFO:
            msg = new Message<DeviceKGroupLeaderInfoMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (DeviceKGroupLeaderInfoMessagePayload) payload);
            break;
        case DEVICE_STATE:
            msg = new Message<DeviceStateMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (DeviceStateMessagePayload) payload);
            break;
        case DEVICE_STATE_ACK:
            msg = new Message<DeviceStateAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (DeviceStateAckMessagePayload) payload);
            break;
        case DEVICE_STATE_CHECK:
            msg = new Message<DeviceStateCheckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (DeviceStateCheckMessagePayload) payload);
            break;
        case DEVICE_STATE_FW:
            msg = new Message<DeviceStateFwMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (DeviceStateFwMessagePayload) payload);
            break;
        case ELECTED:
            msg = new Message<ElectedMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (ElectedMessagePayload) payload);
            break;
        case ELECTED_ACK:
            msg = new Message<ElectedAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (ElectedAckMessagePayload) payload);
            break;
        case ELECTION:
            msg = new Message<ElectionMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (ElectionMessagePayload) payload);
            break;
        case ELECTION_ACK:
            msg = new Message<ElectionAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (ElectionAckMessagePayload) payload);
            break;
        case EXECUTED:
            msg = new Message<ExecutedMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (ExecutedMessagePayload) payload);
            break;
        case EXECUTED_ACK:
            msg = new Message<ExecutedAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (ExecutedAckMessagePayload) payload);
            break;
        case KGROUP_STATE:
            msg = new Message<KGroupStateMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (KGroupStateMessagePayload) payload);
            break;
        case KGROUP_STATE_ACK:
            msg = new Message<KGroupStateAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (KGroupStateAckMessagePayload) payload);
            break;
        case KGROUP_STATE_DISTRIBUTION:
            msg = new Message<KGroupStateDistributionMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (KGroupStateDistributionMessagePayload) payload);
            break;
        case KGROUP_STATE_REQUEST:
            msg = new Message<KGroupStateRequestMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (KGroupStateRequestMessagePayload) payload);
            break;
        case LOCAL_KGROUP_STATE:
            msg = new Message<LocalKGroupStateMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LocalKGroupStateMessagePayload) payload);
            break;
        case LOCAL_KGROUP_STATE_REQUEST:
            msg = new Message<LocalKGroupStateRequestMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LocalKGroupStateRequestMessagePayload) payload);
            break;
        case LOCKED:
            msg = new Message<LockedMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockedMessagePayload) payload);
            break;
        case LOCKED_ACK:
            msg = new Message<LockedAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockedAckMessagePayload) payload);
            break;
        case LOCKED_QUORUM:
            msg = new Message<LockedQuorumMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockedQuorumMessagePayload) payload);
            break;
        case LOCKED_QUORUM_ACK:
            msg = new Message<LockedQuorumAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockedQuorumAckMessagePayload) payload);
            break;
        case LOCK_RELEASED:
            msg = new Message<LockReleasedMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockReleasedMessagePayload) payload);
            break;
        case LOCK_RELEASED_ACK:
            msg = new Message<LockReleasedAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockReleasedAckMessagePayload) payload);
            break;
        case LOCK_RELEASE_QUORUM:
            msg = new Message<LockReleaseQuorumMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockReleaseQuorumMessagePayload) payload);
            break;
        case LOCK_RELEASE_QUORUM_ACK:
            msg = new Message<LockReleaseQuorumAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockReleaseQuorumAckMessagePayload) payload);
            break;
        case LOCK_RELEASE_REQUEST:
            msg = new Message<LockReleaseRequestMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockReleaseRequestMessagePayload) payload);
            break;
        case LOCK_RELEASE_REQUEST_ACK:
            msg = new Message<LockReleaseRequestAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockReleaseRequestAckMessagePayload) payload);
            break;
        case LOCK_REQUEST:
            msg = new Message<LockRequestMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockRequestMessagePayload) payload);
            break;
        case LOCK_REQUESTED:
            msg = new Message<LockRequestedMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockRequestedMessagePayload) payload);
            break;
        case LOCK_REQUESTED_ACK:
            msg = new Message<LockRequestedAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockRequestedAckMessagePayload) payload);
            break;
        case LOCK_REQUEST_ACK:
            msg = new Message<LockRequestAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockRequestAckMessagePayload) payload);
            break;
        case LOCK_REQUEST_QUORUM:
            msg = new Message<LockRequestQuorumMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockRequestQuorumMessagePayload) payload);
            break;
        case LOCK_REQUEST_QUORUM_ACK:
            msg = new Message<LockRequestQuorumAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockRequestQuorumAckMessagePayload) payload);
            break;
        case NODE_FAILURE:
            msg = new Message<NodeFailureMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (NodeFailureMessagePayload) payload);
            break;
        case NODE_FAILURE_ACK:
            msg = new Message<NodeFailureAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (NodeFailureAckMessagePayload) payload);
            break;
        case NODE_RECRUITED:
            msg = new Message<NodeRecruitedMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (NodeRecruitedMessagePayload) payload);
            break;
        case NODE_RECRUITEMENT_REQUEST:
            msg = new Message<NodeRecruitmentRequestMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (NodeRecruitmentRequestMessagePayload) payload);
            break;
        case NOT_OLD_LEADER:
            msg = new Message<NotOldLeaderMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (NotOldLeaderMessagePayload) payload);
            break;
        case PLOCK_CANCEL:
            msg = new Message<PLockCancelMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockCancelMessagePayload) payload);
            break;
        case PLOCK_CANCELLED:
            msg = new Message<PLockCancelledMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockCancelledMessagePayload) payload);
            break;
        case PLOCK_CANCELLED_ACK:
            msg = new Message<PLockCancelledAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockCancelledAckMessagePayload) payload);
            break;
        case PLOCK_CANCEL_ACK:
            msg = new Message<PLockCancelAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockCancelAckMessagePayload) payload);
            break;
        case PLOCK_CANCEL_QUORUM:
            msg = new Message<PLockCancelQuorumMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockCancelQuorumMessagePayload) payload);
            break;
        case PLOCK_CANCEL_QUORUM_ACK:
            msg = new Message<PLockCancelQuorumAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockCancelQuorumAckMessagePayload) payload);
            break;
        case PLOCK_FAILED:
            msg = new Message<PLockFailMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockFailMessagePayload) payload);
            break;
        case PLOCK_FAILED_ACK:
            msg = new Message<PLockFailAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockFailAckMessagePayload) payload);
            break;
        case PLOCK_LOCK:
            msg = new Message<PLockLockMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockLockMessagePayload) payload);
            break;
        case PLOCK_LOCK_ACK:
            msg = new Message<PLockLockAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockLockAckMessagePayload) payload);
            break;
        case PLOCK_REQUEST:
            msg = new Message<LockRequestMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (LockRequestMessagePayload) payload);
            break;
        case PLOCK_REQUESTED:
            msg = new Message<PLockRequestedMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockRequestedMessagePayload) payload);
            break;
        case PLOCK_REQUESTED_ACK:
            msg = new Message<PLockRequestedAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockRequestedAckMessagePayload) payload);
            break;
        case PLOCK_REQUEST_ACK:
            msg = new Message<PLockRequestAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockRequestAckMessagePayload) payload);
            break;
        case PLOCK_REQUEST_QUORUM:
            msg = new Message<PLockRequestQuorumMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockRequestQuorumMessagePayload) payload);
            break;
        case PLOCK_REQUEST_QUORUM_ACK:
            msg = new Message<PLockRequestQuorumAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (PLockRequestQuorumAckMessagePayload) payload);
            break;
        case RELEASED_LOCKS:
            msg = new Message<ReleasedLocksMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (ReleasedLocksMessagePayload) payload);
            break;
        case RELEASED_LOCKS_ACK:
            msg = new Message<ReleasedLocksAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (ReleasedLocksAckMessagePayload) payload);
            break;
        case ROUTINE_KGROUP_LEADER_INFO:
            msg = new Message<RoutineKGroupLeaderInfoMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (RoutineKGroupLeaderInfoMessagePayload) payload);
            break;
        case TRIGGER_QUORUM:
            msg = new Message<TriggerQuorumMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (TriggerQuorumMessagePayload) payload);
            break;
        case TRIGGER_QUORUM_ACK:
            msg = new Message<TriggerQuorumAckMessagePayload>(
                getMyID(), msgSeqNo, dst, type, (TriggerQuorumAckMessagePayload) payload);
            break;
        default:
            break;
        }
        return msg;
    }

    public static void unicastS(String dst, MessageType type,
                        MessagePayload payload, int msgSeqNo) {
        Message<? extends MessagePayload> msg = createMsg(dst, type, payload, msgSeqNo);
        if (Network.sendMsg(msg))
            Network.log("Sent " + msg);
    }

    public void unicast(
        String dst, MessageType type, MessagePayload payload, int msgSeqNo
    ) {
        Message<? extends MessagePayload> msg = createMsg(dst, type, payload, msgSeqNo);
        if (Network.sendMsg(msg))
            log("Sent " + msg);
    }

    public void unicast(int msgSeqNo, UnicastMsgInfo msgInfo) {
        unicast(msgInfo.getDestination(), msgInfo.getMessageType(),
                msgInfo.getMessagePayload(), msgSeqNo);
    }

    public void multicast(MessageType type, MessagePayload payload,
                          int msgSeqNo, boolean oldKGroup) {
        for (String dst: oldKGroup? kgroup.getOldMemberIDs(): kgroup.getCurMemberIDs()) {
            Message<? extends MessagePayload> msg = createMsg(dst, type, payload, msgSeqNo);
            if (Network.sendMsg(msg))
                log("Sent " + msg);
        }
    }

    public void multicast(List<String> recipients, MessageType type,
                          int msgSeqNo, MessagePayload payload) {
        for (String dst: recipients) {
            Message<? extends MessagePayload> msg = createMsg(dst, type, payload, msgSeqNo);
            if (Network.sendMsg(msg))
                log("Sent " + msg);
        }
    }

    public void multicast(int msgSeqNo, QuorumMsgInfo msgInfo) {
        multicast(msgInfo.getNoReplyNodesIDs(), msgInfo.getMessageType(),
                  msgSeqNo, msgInfo.getMessagePayload());
    }

    // return resend timeout in milliseconds
    public double getResendTO(String dst) {
        return Network.getRouteRTT(dst) * waitCoef * 2;
    }

    public ResendTask scheduleUnicastResend(int seqNo, String dstID) {
        double resendTO = getResendTO(dstID);
        if (resendTO == 0) return null;
        // log("resendTO: " + resendTO);
        ResendTask unicastResend = new UnicastResend(this, seqNo);
        try {
            timer.schedule(unicastResend, (long) resendTO);
        } catch (IllegalStateException e) {
            Network.log("KGroupManager: unicast resend scheduling failed", true);
            // System.exit(-1);
        }
        return unicastResend;
    }

    public int remoteCall(
        KGroup otherKGrp, String dst, MessageType type, MessagePayload payload, boolean boundedWait
    ) {
        int seqNo = getNextSeqNo();
        if (seqNo == -1) log("seqNo is -1!!!!!");
        if (dst == null) {
            dst = otherKGrp.getMostProbableLeader(epochNo);
        }
        ResendTask resendTask = null;
        if (!dst.equals(getMyID())) resendTask = scheduleUnicastResend(seqNo, dst);
        UnicastMsgInfo msgInfo = new UnicastMsgInfo(
            type, payload, otherKGrp, dst,
            this, boundedWait, Network.getCurTS(), epochNo, resendTask
        );
        msgsInfo.put(seqNo, msgInfo);
        // log("Increased msg seq no to " + seqNo + "\n\t" + msgsInfo);
        unicast(dst, type, payload, seqNo);
        return seqNo;
    }

    public int remoteCall(
        String dst, MessageType type, MessagePayload payload, boolean boundedWait
    ) {
        return remoteCall((KGroup) null, dst, type, payload, boundedWait);
    }

    public int remoteCall(
        KGroupType kGrpType, String entityID,
        MessageType msgType, MessagePayload payload, boolean boundedWait
    ) {
        KGroup dstKGrp;
        switch (kGrpType) {
        case ROUTINE:
            dstKGrp = getRtnKGrp(entityID);
            break;
        case DEVICE:
            dstKGrp = getDevKGrp(entityID);
            break;
        default:
            dstKGrp = null;
        }
        return remoteCall(dstKGrp, null, msgType, payload, boundedWait);
    }

    public int remoteCall(
        KGroupType kGrpType, List<String> entityIDs,
        MessageType msgType, MessagePayload payload, boolean boundedWait
    ) {
        KGroup dstKGrp;
        switch (kGrpType) {
        case ROUTINE:
            dstKGrp = getRtnKGrp(entityIDs);
            break;
        case DEVICE:
            dstKGrp = getDevKGrp(entityIDs);
            break;
        default:
            dstKGrp = null;
        }
        return remoteCall(dstKGrp, null, msgType, payload, boundedWait);
    }

    public double getResendTO(List<String> targets) {
        double routeRTT, maxRouteRTT = -1;
        for (String dst: targets) {
            routeRTT = Network.getRouteRTT(dst);
            if (maxRouteRTT == -1 || routeRTT > maxRouteRTT) {
                maxRouteRTT = routeRTT;
            }
        }
        return maxRouteRTT * waitCoef;
    }

    public ResendTask scheduleQuorumResend(int seqNo, List<String> targets) {
        double maxResendTO = getResendTO(targets);
        if (maxResendTO == 0) return null;
        ResendTask quorumResend = new QuorumResend(this, seqNo);
        try {
            timer.schedule(quorumResend, (long) maxResendTO);
        } catch (IllegalStateException e) {
            log("KGroupManager: quorum resend scheduling failed", true);
            // System.exit(-1);
        }
        return quorumResend;
    }

    public int quorum(
        MessageType type, MessagePayload payload, boolean oldKGroup, boolean boundedWait
    ) {
        int seqNo = getNextSeqNo();
        List<String> targets = oldKGroup? kgroup.getOldMemberIDs(): kgroup.getCurMemberIDs();
        ResendTask resendTask = scheduleQuorumResend(seqNo, targets);
        QuorumMsgInfo msgInfo = new QuorumMsgInfo(
            type, payload, this, boundedWait, oldKGroup, Network.getCurTS(), epochNo, seqNo, resendTask
        );
        msgsInfo.put(seqNo, msgInfo);
        log("Increased msg seq no to " + seqNo + "\n\t" + msgsInfo);
        multicast(type, payload, seqNo, oldKGroup);
        return seqNo;
    }

    // CoMesh Logic

    public int getEpochNo() { return epochNo; }

    public synchronized int incrementEpoch() {
        epochNo ++;
        Deployer.updateMinEpoch(epochNo);
        return epochNo;
    }

    private void cancelIncEpochTask() {
        if (incrEpochTask != null)
            incrEpochTask.cancel();
    }

    public void runIncEpochTask() {
        cancelIncEpochTask();

        incrEpochTask.run();
        
        newIncEpochTask();
    }

    private void newIncEpochTask() {
        incrEpochTask = new IncrementEpoch(this);
        try {
            timer.scheduleAtFixedRate(incrEpochTask, epochLen, epochLen);
        } catch (IllegalStateException e) {
            log("Inc epoch task Illegal state exception", true);
        }
    }

    public void updateKGroup() {
        kgroup.updateKGroup(epochNo);
    }

    public LockStrategy getLockStrategy() { return lockStrategy; }

    public boolean isLockStrategy(LockStrategy lockStrategy) {
        return this.lockStrategy == lockStrategy;
    }

    public Routine getRtn(String rtnID) {
        return rtns.get(rtnID);
    }

    public void cancelMntrTask() {
        if (mntrTask != null)
            mntrTask.cancel();
    }

    public void newMntrTask() {}

    // Bully Leader Election Algorithm
    public void leaderElection(LeaderElectionCause cause) {
        if ((cause == LeaderElectionCause.NEW_EPOCH && isLeaderNull()) ||
            (cause == LeaderElectionCause.LEADER_FAILURE
                && !isCurMember(getLeader())) ||
            isLdrElctnStg(LeaderElectionStage.NOT_STARTED)) {
            
            log("Starting election for epoch " + epochNo
              + ", most probable leader: " + getMostProbableLeader());
            setLdrElctnTS(Network.getCurTS());
            closeReceivingEnd();
            advLdrElctnStg(LeaderElectionStage.ONGOING);

            // current node is the bully
            if (getMyID().equals(getMostProbableLeader())) {
                int seqNo = quorum(MessageType.ELECTED, new ElectedMessagePayload(this, cause),
                       false, false); // true);
                KGroupMetric.startLdrElctn(this, seqNo, Network.getCurTS());
                if (getType().equals(KGroupType.ROUTINE)) {
                    nodeMetric.startRtnRoles(NodeRole.LEADER, kgroup.getCurMemberIDs(), Network.getCurTS());
                } else {
                    nodeMetric.startDevRoles(NodeRole.LEADER, kgroup.getCurMemberIDs(), Network.getCurTS());
                }
            }
            else {
                // initiate an election
                MessagePayload payload = new ElectionMessagePayload(this, cause);
                int lastSeqNo = -1, newSeqNo;
                for (String nodeID: getCurMemberIDs()) {
                    // send Election msg to every node in the k-group which is more appropriate for leader
                    if (kgroup.moreAppr(nodeID, getMyID(), epochNo)) {
                        newSeqNo = remoteCall(nodeID, MessageType.ELECTION, payload, true);
                        if (lastSeqNo == -1) {
                            KGroupMetric.startLdrElctn(this, newSeqNo, Network.getCurTS());
                        }
                        else {
                            KGroupMetric.addLdrElctnSeqNo(this, lastSeqNo, newSeqNo);
                        }
                    }
                }
                advLdrElctnStg(LeaderElectionStage.WAITING_ON_ELECTION_ACK);
            }
        }
    }

    public void stateTransfer(LeaderElectionCause cause) {
        // leader election is complete -- moving on to state transfer now
        if (isLeader() && epochNo != 0 && !isStateTrnsfrStg(StateTransferStage.COMPLETE)) {
            setStateTrnsfrTS(Network.getCurTS());
            advStateTrnsfrStg(StateTransferStage.ONGOING);
            int seqNo;
            if (cause == LeaderElectionCause.NEW_EPOCH) {
                // only request state transfer if current node was not the leader in the last epoch as well
                if (isOldLeaderNull() || !isOldLeader()) {
                    if (isOldLeaderNull() && isOldMember()) {
                        // in case I just joined
                        // but the system believes I should have been the leader
                        removeOldLdrCnddt();
                    }
                    seqNo = remoteCall(getMostProbableOldLeader(),
                               MessageType.KGROUP_STATE_REQUEST,
                               new KGroupStateRequestMessagePayload(this, cause), true);
                }
                else {
                    seqNo = quorum(MessageType.KGROUP_STATE_DISTRIBUTION, 
                        new KGroupStateDistributionMessagePayload(
                            epochNo, getType(), getState(), getSeqNos()),
                        false, false);
                    // if (kgroup.type == KGroupType.DEVICE) {
                    //     log(kgroup.getState());
                    // }
                }
            }
            else {
                seqNo = quorum(MessageType.LOCAL_KGROUP_STATE_REQUEST,
                    new LocalKGroupStateRequestMessagePayload(this), false, false);
            }
            KGroupMetric.startStateTrnsfr(this, seqNo, Network.getCurTS());
        }
        else if (epochNo == 0) {
            openReceivingEnd();
            newMntrTask();
        }
    }

    public void nodeRecruited(String nodeID) {
        kgroup.nodeRecruited(nodeID);
    }

    public void nodeRecruited() {
        kgroup.nodeRecruited(getMyID());
    }

    public void nodeFailed(String failedNodeID) {
        Network.nodeFailed(failedNodeID);
        kgroup.updateFailedNodes(failedNodeID);
    }

    // called by node when it finds out failure through membership service
    // informs other nodes
    public void nodeFailureDetected(String failedNodeID) {
        if (getMyID().equals(failedNodeID)) {
            log(failedNodeID + "'s failure detected");
        }
        
        // check which other reachable k-groups are affected
        MessagePayload payload;
        List<String> recipients = new ArrayList<>();
        String recipient;
        if (devKGrps != null)
            for (DeviceKGroup kgroup: devKGrps) {
                if (kgroup.isCurMember(failedNodeID)) {
                    payload = new NodeFailureMessagePayload(
                        kgroup.getType(), kgroup.getMonitored(), failedNodeID);
                    kgroup.updateFailedNodes(failedNodeID);
                    recipient = kgroup.getMostProbableLeader(epochNo);
                    if (!recipients.contains(recipient)) {
                        remoteCall(kgroup, recipient, MessageType.NODE_FAILURE, payload, false);
                        recipients.add(recipient);
                    }
                }
            }
        if (rtnKGrps != null)
            for (RoutineKGroup kgroup: rtnKGrps) {
                if (kgroup.isCurMember(failedNodeID)) {
                    payload = new NodeFailureMessagePayload(
                        kgroup.getType(), kgroup.getMonitored(), failedNodeID);
                    kgroup.updateFailedNodes(failedNodeID);
                    recipient = kgroup.getMostProbableLeader(epochNo);
                    if (!recipients.contains(recipient)) {
                        remoteCall(kgroup, recipient, MessageType.NODE_FAILURE, payload, false);
                        recipients.add(recipient);
                    }
                }
            }
        handleNodeFailure(failedNodeID);
    }

    // called by node when it is informed of the failure
    public void handleNodeFailure(String failedNodeID) {
        Network.nodeFailed(failedNodeID);
        if (isCurMember(failedNodeID)) {
            if (isCurMember()) {
                boolean failedLeader = kgroup.updateFailedNodes(failedNodeID);
                if (failedLeader || (isLeaderNull() && isLdrElctnStg(LeaderElectionStage.COMPLETE))) {
                    log(" - leader died -> new leader election");
                    leaderElection(LeaderElectionCause.LEADER_FAILURE);
                }
                else if (isLeader()) {
                    if (getCountFailedNodes() >= F) {
                        replaceFailedNodes();
                    }
                }
            }
        }
    }

    // Device k-group leader

    public List<String> getRtnsToNotify(String devID) { return null; }

    public void gatherQuorumForDevLockRequest(
        String rtnID, int rtnSeqNo, String devID, int reqSeqNo) {}

    public void gatherQuorumForDevPLockRequest(
        String rtnID, int rtnSeqNo, String devID, int reqSeqNo) {}
    
    public void lockRequestReplicated(
        String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {}
    
    public int getLastLockedReqSeqNo(String devID) { return -1; }

    public LockRequest getLockQueueHead(String devID) { return null; }

    public void gatherQuorumForDevLockAcquisition(
        String rtnID, int rtnSeqNo, String devID, int reqSeqNo) {}
    
    public void gatherQuorumForDevLockAcquisition(
                String rtnID, int rtnSeqNo, String devID) {}

    public boolean isLockRequestReceived(String devID, String rtnID, int rtnSeqNo) {
        return false;
    }

    public boolean isLockRequestGranted(String devID, String rtnID, int rtnSeqNo) { return false; }

    public int addNewLockRequest(String devID, String rtnID, int rtnSeqNo) { return -1; }

    public int getLockReqSeqNo(String devID, String rtnID, int rtnSeqNo) { return -1; }

    public boolean isLockAcquired(String devID, String rtnID, int rtnSeqNo) { return false; }

    public void lock(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {}

    public boolean isLocker(String devID, String rtnID, int rtnSeqNo) { return false; }

    public int getLockerReqSeqNo(String devID) { return -1; }

    public void gatherQuorumForDevLockRelease(
        String routineID, int routineSeqNo, String deviceID, int reqSeqNo) {}

    public LockRequest releaseLock(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        return null;
    }

    // Device k-group member

    public int replicateNewLockRequest(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {
        return -1;
    }

    public void replicateLockAcquisition(String devID, String rtnID, int rtnSeqNo, int reqSeqNo) {}

    public void replicateLockRelease(String devID, String rtnID, int rtnSeqNo) {}

    // Routine k-group leader

    public boolean isRtnTriggered(String rtnID) { return false; }

    public boolean areRtnLocksReleased(String rtnID, int rtnSeqNo) { return false; }

    public int setRtnTriggered(String rtnID) { return -1; }

    public void gatherQuorumForRtnTrigger(String rtnID, int rtnSeqNo) {}

    public void requestDevLockForRtn(String rtnID, int rtnSeqNo) {}

    public void requestDevLockForRtnInParallel(String rtnID, int rtnSeqNo) {}

    public void cancelLockReleases() {}

    public void schedLockRelease(String rtnID, int rtnSeqNo, long delay) {}

    public boolean acquiredDevLockForRtn(
        String rtnID, int rtnSeqNo, String devID, List<String> touchedDevicesIDs
    ) {
        return false;
    }

    public void startRtnExec(String rtnID, int rtnSeqNo) {}

    public void replicateRtnExec(String rtnID, int rtnSeqNo) {}

    public void finishRtnExec(String rtnID, int rtnSeqNo) {}

    public void releaseDevLockForRtn(String rtnID, int rtnSeqNo, String devID) {}

    public void lockReleased(String rtnID, int rtnSeqNo, String devID) {}

    public void replicateRtnLocksReleased(String rtnID, int rtnSeqNo) {}

    public void removeRtnStage(String rtnID, int rtnSeqNo) {}

    // Routine k-group member

    public void setRtnTriggered(String rtnID, int rtnSeqNo) {}

    public void setRtnExecuted(String rtnID, int rtnSeqNo) {}

    public void setRtnLocksReleased(String rtnID, int rtnSeqNo) {}
}
