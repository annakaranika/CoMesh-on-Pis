package network.message;

import network.message.payload.MessagePayload;
import network.message.payload.election.*;
import network.message.payload.execution.*;
import network.message.payload.failure.*;
import network.message.payload.lock.*;
import network.message.payload.monitor.*;
import network.message.payload.routineStage.*;
import network.message.payload.stateTransfer.*;

public enum MessageType {
    // k-Group Leader Election
    ELECTION(ElectionMessagePayload.class), // sent by non-bully node to start √
    ELECTION_ACK(ElectionAckMessagePayload.class), // sent by other k-group nodes √
    ELECTED(ElectedMessagePayload.class), // sent by k-group's elected leader √
    ELECTED_ACK(ElectedAckMessagePayload.class), // sent by all nodes in a k-group √
    // k-Group node replacement
    NODE_FAILURE(NodeFailureMessagePayload.class), // sent by k-group node to leader √
    NODE_FAILURE_ACK(NodeFailureAckMessagePayload.class), //sent by k-group leader to k-group node √
    NODE_RECRUITEMENT_REQUEST(NodeRecruitedMessagePayload.class), // sent by k-group leader to new k-group recruit √
    NODE_RECRUITED(NodeRecruitedMessagePayload.class), // sent by new k-group recruit to k-group leader √
    // k-Group State Transfer upon Epoch Change or node failure and replacement
    KGROUP_STATE_REQUEST(KGroupStateRequestMessagePayload.class), // sent by new k-group leader to old k-group leader √
    KGROUP_STATE(KGroupStateMessagePayload.class), // sent by old k-group leader to new k-group leader √
    NOT_OLD_LEADER(NotOldLeaderMessagePayload.class), // sent by old k-group member to new k-group leader (in response to KGROUP_STATE_REQUEST) √
    KGROUP_STATE_DISTRIBUTION(KGroupStateDistributionMessagePayload.class), // sent by new k-group leader to new k-group nodes √
    KGROUP_STATE_ACK(KGroupStateAckMessagePayload.class), // sent by new k-group nodes to new k-group leader √
    LOCAL_KGROUP_STATE_REQUEST(LocalKGroupStateRequestMessagePayload.class), // sent by new k-group leader to current/old k-group nodes √
    LOCAL_KGROUP_STATE(LocalKGroupStateMessagePayload.class), // sent by current/old k-group nodes to new k-group leader √
    // Routine Condition-Monitoring
    DEVICE_STATE_CHECK(DeviceStateCheckMessagePayload.class), // sent by routine k-group's leader to device √
    DEVICE_STATE(DeviceStateMessagePayload.class), // sent by device to routine k-group's leader √
    DEVICE_STATE_FW(DeviceStateFwMessagePayload.class), // sent by device to routine leader √
    DEVICE_STATE_ACK(DeviceStateAckMessagePayload.class), // sent by routine to device leader √
    // Routine Triggering
    TRIGGER_QUORUM(TriggerQuorumMessagePayload.class), // sent by routine k-group's leader to routine k-group's members √
    TRIGGER_QUORUM_ACK(TriggerQuorumAckMessagePayload.class), // sent by all nodes in a routine k-group to its leader √
    // K-group leader discovery
    DEVICE_KGROUP_LEADER_INFO(DeviceKGroupLeaderInfoMessagePayload.class), // sent by a device k-group's member to a routine k-group's leader √
    ROUTINE_KGROUP_LEADER_INFO(RoutineKGroupLeaderInfoMessagePayload.class), // sent by a routine k-group's member to a device k-group's leader √
    // Device Lock Request Queueing
    LOCK_REQUEST(LockRequestMessagePayload.class), // sent by routine k-group's leader to device k-group's leader √
    LOCK_REQUEST_ACK(LockRequestAckMessagePayload.class), // sent as an acknowledgement to the routine k-group's leader by device k-group's leader (in response to LOCK_REQUEST) √
    LOCK_REQUEST_QUORUM(LockRequestQuorumMessagePayload.class), // sent by device k-group's leader to all k-group nodes √
    LOCK_REQUEST_QUORUM_ACK(LockRequestQuorumAckMessagePayload.class), // sent by all nodes in a device k-group to leader √
    LOCK_REQUESTED(LockRequestedMessagePayload.class), // sent by device k-group's leader to routine k-group's leader √
    LOCK_REQUESTED_ACK(LockRequestedAckMessagePayload.class), // sent as an acknowledgement to the device k-group's leader by routine k-group's leader (in response to LOCK_REQUEST_REPLY) √
    // Device Lock for Parallel Locking
    PLOCK_REQUEST(LockRequestMessagePayload.class), // sent by routine k-group's leader to device k-group's leader
    PLOCK_REQUEST_ACK(PLockRequestAckMessagePayload.class), // sent as an acknowledgement to the routine k-group's leader by device k-group's leader √
    PLOCK_REQUEST_QUORUM(PLockRequestQuorumMessagePayload.class), // sent by device k-group's leader to all k-group nodes
    PLOCK_REQUEST_QUORUM_ACK(PLockRequestQuorumAckMessagePayload.class), // sent by all nodes in a device k-group to leader
    PLOCK_REQUESTED(PLockRequestedMessagePayload.class),  // sent by device k-group's leader to routine k-group's leader
                        // In response to PLOCK_REQUEST. Sent if the lock is able to be granted.
    PLOCK_REQUESTED_ACK(PLockRequestedAckMessagePayload.class),  // sent as an acknowledgement to the device k-group's leader by routine k-group's leader √
    PLOCK_FAILED(PLockFailMessagePayload.class), // sent by device k-group's leader to routine k-group's leader
                        // In response to PLOCK_REQUEST. Sent if the lock fails to be granted.
                        // Used to decrease lock time.
    PLOCK_FAILED_ACK(PLockFailAckMessagePayload.class),  // sent as an acknowledgement to the device k-group's leader by routine k-group's leader √
    PLOCK_LOCK(PLockLockMessagePayload.class),    // sent by routine k-group's leader to device k-group's leader to actually lock a device
    PLOCK_LOCK_ACK(PLockLockAckMessagePayload.class), // sent by device k-group's leader to routine k-group's leader that a lock is held by the routine √
    PLOCK_CANCEL(PLockCancelMessagePayload.class),  // sent by routine k-group's leader to device k-group's leader
    PLOCK_CANCEL_ACK(PLockCancelAckMessagePayload.class),  // sent by device k-group's leader to routine k-group's leader √
    PLOCK_CANCEL_QUORUM(PLockCancelQuorumMessagePayload.class),  // sent by device k-group's leader to all k-group nodes
    PLOCK_CANCEL_QUORUM_ACK(PLockCancelQuorumAckMessagePayload.class), // sent by all nodes in a device k-group to leader
    PLOCK_CANCELLED(PLockCancelledMessagePayload.class),  // sent by device k-group's leader to routine k-group's leader
    PLOCK_CANCELLED_ACK(PLockCancelledAckMessagePayload.class),  // sent as an acknowledgement to the device k-group's leader by routine k-group's leader √
    // Device Lock Acquisition
    LOCKED_QUORUM(LockedQuorumMessagePayload.class), // sent by device k-group's leader to all k-group nodes √
    LOCKED_QUORUM_ACK(LockedQuorumAckMessagePayload.class), // sent by all nodes in a device k-group to leader √
    LOCKED(LockedMessagePayload.class), // sent by device k-group's leader to routine k-group's leader √
    LOCKED_ACK(LockedAckMessagePayload.class), // sent by routine k-group's leader to device k-group's leader √
    // Routine Execution
    DEVICE_COMMAND(DeviceCommandMessagePayload.class), // sent by device k-group's leader to device
    DEVICE_COMMAND_ACK(DeviceCommandAckMessagePayload.class), // sent by device to device k-group's leader
    EXECUTED(ExecutedMessagePayload.class), // sent by routine k-group's leader to routine k-group's members √
    EXECUTED_ACK(ExecutedAckMessagePayload.class), // sent by all nodes in a routine k-group to its leader √
    // START_EXEC(StartExecutionMessagePayload.class),
    // START_EXEC_ACK(StartExecutionAckMessagePayload.class),
    // Device Lock Release
    LOCK_RELEASE_REQUEST(LockReleaseRequestMessagePayload.class), // sent by routine k-group's leader to device k-group's leader √
    LOCK_RELEASE_REQUEST_ACK(LockReleaseRequestAckMessagePayload.class), // sent as an acknowledgement to the routine k-group's leader by device k-group's leader (in response to LOCK_RELEASE_REQUEST) √
    LOCK_RELEASE_QUORUM(LockReleaseQuorumMessagePayload.class), // sent by device k-group's leader to all k-group nodes √
    LOCK_RELEASE_QUORUM_ACK(LockReleaseQuorumAckMessagePayload.class), // sent by all nodes in a device k-group to leader √
    LOCK_RELEASED(LockReleasedMessagePayload.class), // sent by device k-group's leader to routine k-group's leader (in response to LOCK_RELEASE_REQUEST) √
    LOCK_RELEASED_ACK(LockReleasedAckMessagePayload.class), // sent as an acknowledgement to the routine k-group's leader by device k-group's leader (in response to LOCK_RELEASED) √
    RELEASED_LOCKS(ReleasedLocksMessagePayload.class), // sent by routine k-group's leader to routine k-group's members √
    RELEASED_LOCKS_ACK(ReleasedLocksAckMessagePayload.class) // sent by all nodes in a routine k-group to its leader √
    ;

    private Class<? extends MessagePayload> payloadClass;
    private MessageType(Class<? extends MessagePayload> payloadClass) {
        this.payloadClass = payloadClass;
    }

    public Class<? extends MessagePayload> getPayloadClass() {
        return payloadClass;
    }
}
