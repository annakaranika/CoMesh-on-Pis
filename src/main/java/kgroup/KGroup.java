package kgroup;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import configs.KGroupSelectionPolicy;
import configs.LeaderElectionPolicy;
import configs.LockStrategy;
import deploy.DeviceState;
import kgroup.state.*;
import network.Membership;
import network.Network;
import network.message.Message;
import network.message.payload.MessagePayload;

public abstract class KGroup {
    // entitiesIDs refers to the ID(s) of the entities (device(s) or routine(s)) this k-group is responsible for
    // oldMemberIDs and curMemberIDs refer to the IDs of the nodes that are members of the old and current k-group respectively
    // failedNodesIDs consists of the IDs of nodes that have been detected as failed
    // type refers to whether the k-group is in charge of device(s) or routine(s)
    // K is the minimum number of nodes that are members of the k-group
    // N is the number of all nodes in the network

    protected List<String> oldMemberIDs, curMemberIDs, failedMemberIDs, oldLeaderCandidates;
    protected String curKGroupLeaderNodeID, oldKGroupLeaderNodeID;
    protected final KGroupType type;
    protected List<Message<? extends MessagePayload>> waitingMessageQueue;
    protected Map<String, Object> state;
    protected final int F, K, kGrpID;
    protected int countReceivedLocalStates, countFailedNodes;
    protected boolean receivingEndOpen;
    protected final LeaderElectionPolicy electionPolicy;
    protected final KGroupSelectionPolicy kGroupPolicy;
    protected final LockStrategy lockStrategy;
    protected LeaderElectionStage ldrElctnStg;
    protected StateTransferStage stateTrnsfrStg;
    protected long ldrElctnTS, stateTrnsfrTS;
    protected static int id = -1;
    protected final boolean debug;

    public KGroup(KGroupType type, int F, int K, LeaderElectionPolicy lep,
        LockStrategy ls, KGroupSelectionPolicy kGroupPolicy, boolean debug) {
        this.type = type;
        this.F = F;
        if (K != 2 * F + 1) {
            Network.log("K cannot be different from 2 * F + 1!", true);
            System.exit(-1);
        }
        this.K = K;
        
        oldMemberIDs = null;
        oldLeaderCandidates = null;
        oldKGroupLeaderNodeID = null;
        curMemberIDs = new CopyOnWriteArrayList<String>();
        curKGroupLeaderNodeID = null;
        failedMemberIDs = new CopyOnWriteArrayList<String>();
        receivingEndOpen = false;
        electionPolicy = lep;
        ldrElctnStg = LeaderElectionStage.NOT_STARTED;
        stateTrnsfrStg = StateTransferStage.NOT_STARTED;
        countReceivedLocalStates = 0;
        waitingMessageQueue = new ArrayList<Message<? extends MessagePayload>>();
        countFailedNodes = 0;
        kGrpID = ++ id;
        lockStrategy = ls;
        this.kGroupPolicy = kGroupPolicy;
        this.debug = debug;
    }

    public KGroupType getType() {
        return type;
    }

    public boolean isType(KGroupType type) {
        return this.type == type;
    }

    public List<String> getMonitored() {
        return new ArrayList<>(state.keySet());
    }

    public List<String> getCurMemberIDs() {
        return curMemberIDs;
    }

    public boolean isCurMember(String nodeID) {
        // System.out.println(nodeID + " in " + getCurMemberIDs() + " ?");
        return curMemberIDs.contains(nodeID);
    }

    public List<String> getOldMemberIDs() {
        return oldMemberIDs;
    }

    public List<String> getFailedMemberIDs() {
        return failedMemberIDs;
    }

    public boolean isLdrElctnStg(LeaderElectionStage ldrElctnStg) {
        return this.ldrElctnStg == ldrElctnStg;
    }

    public LeaderElectionStage getLdrElctnStg() {
        return ldrElctnStg;
    }

    public void advLdrElctnStg(LeaderElectionStage newLdrElctnStg) {
        ldrElctnStg = newLdrElctnStg;
    }

    public long getLdrElctnTS() {
        return ldrElctnTS;
    }

    public void setLdrElctnTS(long ldrElctnTS) {
        this.ldrElctnTS = ldrElctnTS;
    }

    public boolean isStateTrnsfrStg(StateTransferStage stateTrnsfrStg) {
        return this.stateTrnsfrStg == stateTrnsfrStg;
    }

    public void advStateTrnsfrStg(StateTransferStage newStateTrnsfrStg) {
        stateTrnsfrStg = newStateTrnsfrStg;
    }

    public long getStateTrnsfrTS() {
        return stateTrnsfrTS;
    }

    public void setStateTrnsfrTS(long stateTrnsfrTS) {
        this.stateTrnsfrTS = stateTrnsfrTS;
    }

    protected String hash(String nodeID, int epochNo) {
        String value = nodeID + kGrpID + type.toString() + getMonitored().toString()
                     + String.valueOf(epochNo);
        String sha1 = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(value.getBytes("utf8"));
            sha1 = String.format("%040x", new BigInteger(1, digest.digest()));
        } catch (Exception e){
            e.printStackTrace();
        }
        return sha1;
    }

    // returns the nodes that should be added to the k-group based on the recruitement strategy
    public List<String> newKGroupMembers(int desired, int epochNo, String type) {
        // if this is a replacement of failures, choose randomly
        if (kGroupPolicy.equals(KGroupSelectionPolicy.RANDOM) || desired < K) {
            return newKGroupMembersRandom(desired, epochNo);
        } else {
            // use LSH for new epoch
            if (type.equals("previous")) {
                return new ArrayList<>(
                    Network.getkMemberMix().getAllCenters().get(kGrpID).getPreviousKGroup()
                );
            }
            return newKGroupMembersLSHMix(K);
        }
    }

    // return the K group of CentersForDeviceKGroup.get(kGrpID) if kGrpID < getCentersForDeviceKGroup().size();
    // otherwise kGrpID = kGrpID - CentersForDeviceKGroup.size() and return accordingly
    public List<String> newKGroupMembersLSHMix(int desired) {
        KMemberMix kMemberMix = Network.getkMemberMix();
        // return a list of KGroup members: first F+1 are based on LSH and the last F from random selection
        // notice that first F+1 may also have random selected nodes if not enough from LSH
        List<String> temp = new ArrayList<>(kMemberMix.getAllCenters().get(kGrpID).getOrderedKGroup());
        return temp;
    }

    // returns the nodes that should be added to the k-group based on the recruitement strategy
    public List<String> newKGroupMembersRandom(int desired, int epochNo) {
        SortedMap<String, String> hashes = new TreeMap<>();
        String nodeID;
        Membership status;
        for (Entry<String, Membership> node: Network.getMembershipList().entrySet()) {
            nodeID = node.getKey();
            status = node.getValue();
            if (status != Membership.FAILED && (desired >= K ||
               (curMemberIDs != null && !curMemberIDs.contains(nodeID)))) {
                hashes.put(hash(nodeID, epochNo), nodeID);
            }
        }
        Network.log(Network.getMembershipList().toString(), desired > hashes.size());
        if (desired > hashes.size()) {
            Network.log("desired: " + desired + ", hashes size: " + hashes.size(), true);
            System.out.println("\nNode hashes collide!!!!!!!\n");
        }
        int c = 0;
        List<String> newMembers = new ArrayList<>();
        for (String _nodeID: hashes.values()) {
            if (c < desired) {
                newMembers.add(_nodeID);
                c ++;
            }
            else {
                break;
            }
        }
        return newMembers;
    }
    
    public void updateKGroup(int epochNo) {
        if (curMemberIDs.isEmpty()) {
            oldMemberIDs = new ArrayList<>(newKGroupMembers(K, epochNo-1, "previous"));
            oldLeaderCandidates = new ArrayList<>(newKGroupMembers(K, epochNo-1, "previous"));
        }
        else {
            oldMemberIDs = new ArrayList<>(curMemberIDs);
            oldLeaderCandidates = new ArrayList<>(curMemberIDs);
        }
        oldKGroupLeaderNodeID = curKGroupLeaderNodeID;
        curMemberIDs = new CopyOnWriteArrayList<>(newKGroupMembers(K, epochNo, "current"));
        curKGroupLeaderNodeID = null;
        ldrElctnStg = LeaderElectionStage.NOT_STARTED;
        stateTrnsfrStg = StateTransferStage.NOT_STARTED;
    }

    public List<String> getDifferentNodes() {
        List<String> result = new ArrayList<>();
        for (String nodeID: curMemberIDs) {
            if (!oldMemberIDs.contains(nodeID)) {
                result.add(nodeID);
            }
        }

        return result;
    }

    public boolean updateFailedNodes(String failedNodeID) {
        if (curMemberIDs.contains(failedNodeID)) {
            curMemberIDs.remove(failedNodeID);
            countFailedNodes ++;
            if (curKGroupLeaderNodeID == null) {
                return true;
            }
            if (curKGroupLeaderNodeID.equals(failedNodeID)) {
                oldKGroupLeaderNodeID = failedNodeID;
                curKGroupLeaderNodeID = null;
                ldrElctnStg = LeaderElectionStage.NOT_STARTED;
                return true;
            }
        }
        return false;
    }

    public int getCountFailedNodes() {
        return countFailedNodes;
    }

    public List<String> replaceFailedNodes(int epochNo) {
        failedMemberIDs.clear();
        return newKGroupMembers(F, epochNo, "current");
    }

    public void nodeRecruited(String newMemberNodeID) {
        curMemberIDs.add(newMemberNodeID);
    }

    public void resetCountFailedNodes() {
        countFailedNodes = 0;
    }

    public String chooseLeader(boolean oldKGroup, int epochNo) {
        List<String> nodesIDs = oldKGroup? oldLeaderCandidates: curMemberIDs;
        if (nodesIDs.isEmpty()) {
            Network.log("No nodes in " + this);
        }
        int epoch = epochNo - (oldKGroup? 1: 0);
        switch (electionPolicy) {
        case SMALLEST_HASH:
            SortedMap<String, String> hashes = new TreeMap<>();
            for (String nodeID: nodesIDs) {
                hashes.put(hash(nodeID, epoch), nodeID);
            }
            return hashes.get(hashes.firstKey());
        case LSH_SMALLEST_HASH:
            hashes = new TreeMap<>();
            for (String nodeID: nodesIDs.subList(0, Math.min(F+1, nodesIDs.size()))) {
                hashes.put(hash(nodeID, epoch), nodeID);
            }
            return hashes.get(hashes.firstKey());
        case SMALLEST_ID:
            return Collections.min(nodesIDs);
        case STATIC:
            if (nodesIDs.size() == 1) return nodesIDs.get(0);
        case CENTRAL_NODE:
        default:
            return null;
        }
    }

    // returns true if node1 is more appropriate for leader than node2
    public boolean moreAppr(String nodeID1, String nodeID2, int epochNo) {
        switch (electionPolicy) {
        case SMALLEST_HASH:
            return hash(nodeID1, epochNo).compareTo(hash(nodeID2, epochNo)) < 0;
        case SMALLEST_ID:
            return nodeID1.compareTo(nodeID2) < 0;
        case LSH_SMALLEST_HASH:
            int pos1 = curMemberIDs.indexOf(nodeID1);
            int pos2 = curMemberIDs.indexOf(nodeID2);
            if (pos1 <= F & pos2 > F) return true;
            else if (pos2 <= F & pos1 > F) return false;
            else return hash(nodeID1, epochNo)
                        .compareTo(hash(nodeID2, epochNo)) < 0;
        case CENTRAL_NODE:
        default:
            return false;
        }
    }

    public void updateLeader(String newLeaderNodeID) {
        curKGroupLeaderNodeID = newLeaderNodeID;
    }

    public void updateOldLeader(String oldLeaderNodeID) {
        oldKGroupLeaderNodeID = oldLeaderNodeID;
    }

    public String getLeader() {
        return curKGroupLeaderNodeID;
    }

    public String getOldLeader() {
        return oldKGroupLeaderNodeID;
    }

    public void removeOldLdrCnddt(String nodeID) {
        oldLeaderCandidates.remove(nodeID);
    }

    public List<String> getOldLdrCnddts() {
        return oldLeaderCandidates;
    }

    public String getMostProbableLeader(int epochNo) {
        if (curKGroupLeaderNodeID != null) {
            return curKGroupLeaderNodeID;
        }
        return chooseLeader(false, epochNo);
    }

    public String getMostProbableOldLeader(int epochNo) {
        if (oldKGroupLeaderNodeID != null) {
            return oldKGroupLeaderNodeID;
        }
        return chooseLeader(true, epochNo);
    }

    public int ascNodeOrder(String nodeID) {
        Collections.sort(curMemberIDs);
        int order = 0;
        for (String member: curMemberIDs) {
            if (member.equals(nodeID)) {
                return order;
            }
            order ++;
        }
        return -1;
    }

    public List<String> nodesBefore(String nodeID) {
        Collections.sort(curMemberIDs);
        List<String> lowerNodesIDs = new ArrayList<>();
        for (String curNodeID: curMemberIDs) {
            if (curNodeID.compareTo(nodeID) >= 0) {
                break;
            }
            lowerNodesIDs.add(curNodeID);
        }
        return lowerNodesIDs;
    }

    public Map<String, Object> getState() {
        return state;
    }

    public void setState(Map<String, Object> state) {
        this.state = state;
    }

    // called by new leader
    public abstract void addLocalState(Map<String, Object> localState);

    public void addLocalSeqNos(Map<String, Integer> localSeqNos) {}

    public void addLocalOldDevStates(Map<String, DeviceState> localOldDevStates) {}

    public int getCountReceivedLocalStates() {
        return countReceivedLocalStates;
    }

    public boolean isReceivingEndOpen() {
        return receivingEndOpen;
    }

    public void openReceivingEnd() {
        receivingEndOpen = true;
    }

    public void closeReceivingEnd() {
        receivingEndOpen = false;
    }

    public void addToWaitingMessageQueue(Message<? extends MessagePayload> msg) {
        waitingMessageQueue.add(msg);
    }

    public List<Message<? extends MessagePayload>> getWaitingMessageQueue() {
        return waitingMessageQueue;
    }

    public void setWaitingMessageQueue(List<Message<? extends MessagePayload>> unprocessedMessages) {
        waitingMessageQueue = unprocessedMessages;
    }

    public void setSeqNos(Map<String, Integer> seqNos) {}

    public Map<String, Integer> getSeqNos() { return null; }

    public void setOldDevStates(Map<String, DeviceState> oldDevStates) {}

    public Map<String, DeviceState> getOldDevStates() { return null; }

    public int size() {
        return K;
    }

    public boolean isMemberOfKGroup(String nodeID) {
        return curMemberIDs.contains(nodeID);
    }

    public boolean isInChargeOf(String entityID) {
        return getMonitored().contains(entityID);
    }

    public boolean isInChargeOf(List<String> entitiesIDs) {
        return getMonitored().containsAll(entitiesIDs);
    }

    public String toString() {
        return type + " k-group in charge of " + getMonitored() + " with members " + curMemberIDs + " and leader " + curKGroupLeaderNodeID;
    }
}
