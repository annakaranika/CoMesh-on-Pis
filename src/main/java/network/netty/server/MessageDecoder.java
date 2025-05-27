package network.netty.server;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import network.Network;
import network.message.*;
import network.message.payload.MessagePayload;
import network.message.payload.election.*;
import network.message.payload.execution.*;
import network.message.payload.failure.*;
import network.message.payload.lock.*;
import network.message.payload.monitor.*;
import network.message.payload.routineStage.*;
import network.message.payload.stateTransfer.*;

public class MessageDecoder extends MessageToMessageDecoder<DatagramPacket> {

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {
        ByteBuf in = packet.content();
        int readableBytes = in.readableBytes();
        if (readableBytes <= 0) {
            return;
        }

        String jsonMsg = in.toString(CharsetUtil.UTF_8);
        in.readerIndex(in.readerIndex() + in.readableBytes());

        JSONObject msgObj = null;
        try {
            msgObj = new JSONObject(jsonMsg);
        } catch (Exception e) {
            Network.log("Message decoding failed.", true);
            return;
        }

        MessageType msgType = msgObj.getEnum(MessageType.class, "type");
        GsonBuilder gson = new GsonBuilder();
        Message<? extends MessagePayload> message = null;
        switch (msgType) {
        case DEVICE_COMMAND:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<DeviceCommandMessagePayload>>(){}.getType());
            break;
        case DEVICE_COMMAND_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<DeviceCommandAckMessagePayload>>(){}.getType());
            break;
        case DEVICE_KGROUP_LEADER_INFO:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<DeviceKGroupLeaderInfoMessagePayload>>(){}.getType());
            break;
        case DEVICE_STATE:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<DeviceStateMessagePayload>>(){}.getType());
            break;
        case DEVICE_STATE_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<DeviceStateAckMessagePayload>>(){}.getType());
            break;
        case DEVICE_STATE_CHECK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<DeviceStateCheckMessagePayload>>(){}.getType());
            break;
        case DEVICE_STATE_FW:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<DeviceStateFwMessagePayload>>(){}.getType());
            break;
        case ELECTED:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<ElectedMessagePayload>>(){}.getType());
            break;
        case ELECTED_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<ElectedAckMessagePayload>>(){}.getType());
            break;
        case ELECTION:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<ElectionMessagePayload>>(){}.getType());
            break;
        case ELECTION_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<ElectionAckMessagePayload>>(){}.getType());
            break;
        case EXECUTED:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<ExecutedMessagePayload>>(){}.getType());
            break;
        case EXECUTED_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<ExecutedAckMessagePayload>>(){}.getType());
            break;
        case KGROUP_STATE:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<KGroupStateMessagePayload>>(){}.getType());
            break;
        case KGROUP_STATE_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<KGroupStateAckMessagePayload>>(){}.getType());
            break;
        case KGROUP_STATE_DISTRIBUTION:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<KGroupStateDistributionMessagePayload>>(){}.getType());
            break;
        case KGROUP_STATE_REQUEST:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<KGroupStateRequestMessagePayload>>(){}.getType());
            break;
        case LOCAL_KGROUP_STATE:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LocalKGroupStateMessagePayload>>(){}.getType());
            break;
        case LOCAL_KGROUP_STATE_REQUEST:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LocalKGroupStateRequestMessagePayload>>(){}.getType());
            break;
        case LOCKED:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockedMessagePayload>>(){}.getType());
            break;
        case LOCKED_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockedAckMessagePayload>>(){}.getType());
            break;
        case LOCKED_QUORUM:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockedQuorumMessagePayload>>(){}.getType());
            break;
        case LOCKED_QUORUM_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockedQuorumAckMessagePayload>>(){}.getType());
            break;
        case LOCK_RELEASED:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockReleasedMessagePayload>>(){}.getType());
            break;
        case LOCK_RELEASED_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockReleasedAckMessagePayload>>(){}.getType());
            break;
        case LOCK_RELEASE_QUORUM:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockReleaseQuorumMessagePayload>>(){}.getType());
            break;
        case LOCK_RELEASE_QUORUM_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockReleaseQuorumAckMessagePayload>>(){}.getType());
            break;
        case LOCK_RELEASE_REQUEST:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockReleaseRequestMessagePayload>>(){}.getType());
            break;
        case LOCK_RELEASE_REQUEST_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockReleaseRequestAckMessagePayload>>(){}.getType());
            break;
        case LOCK_REQUEST:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockRequestMessagePayload>>(){}.getType());
            break;
        case LOCK_REQUESTED:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockRequestedMessagePayload>>(){}.getType());
            break;
        case LOCK_REQUESTED_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockRequestedAckMessagePayload>>(){}.getType());
            break;
        case LOCK_REQUEST_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockRequestAckMessagePayload>>(){}.getType());
            break;
        case LOCK_REQUEST_QUORUM:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockRequestQuorumMessagePayload>>(){}.getType());
            break;
        case LOCK_REQUEST_QUORUM_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockRequestQuorumAckMessagePayload>>(){}.getType());
            break;
        case NODE_FAILURE:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<NodeFailureMessagePayload>>(){}.getType());
            break;
        case NODE_FAILURE_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<NodeFailureAckMessagePayload>>(){}.getType());
            break;
        case NODE_RECRUITED:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<NodeRecruitedMessagePayload>>(){}.getType());
            break;
        case NODE_RECRUITEMENT_REQUEST:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<NodeRecruitmentRequestMessagePayload>>(){}.getType());
            break;
        case NOT_OLD_LEADER:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<NotOldLeaderMessagePayload>>(){}.getType());
            break;
        case PLOCK_CANCEL:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockCancelMessagePayload>>(){}.getType());
            break;
        case PLOCK_CANCELLED:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockCancelledMessagePayload>>(){}.getType());
            break;
        case PLOCK_CANCELLED_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockCancelledAckMessagePayload>>(){}.getType());
            break;
        case PLOCK_CANCEL_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockCancelAckMessagePayload>>(){}.getType());
            break;
        case PLOCK_CANCEL_QUORUM:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockCancelQuorumMessagePayload>>(){}.getType());
            break;
        case PLOCK_CANCEL_QUORUM_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockCancelQuorumAckMessagePayload>>(){}.getType());
            break;
        case PLOCK_FAILED:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockFailMessagePayload>>(){}.getType());
            break;
        case PLOCK_FAILED_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockFailAckMessagePayload>>(){}.getType());
            break;
        case PLOCK_LOCK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockLockMessagePayload>>(){}.getType());
            break;
        case PLOCK_LOCK_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockLockAckMessagePayload>>(){}.getType());
            break;
        case PLOCK_REQUEST:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<LockRequestMessagePayload>>(){}.getType());
            break;
        case PLOCK_REQUESTED:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockRequestedMessagePayload>>(){}.getType());
            break;
        case PLOCK_REQUESTED_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockRequestedAckMessagePayload>>(){}.getType());
            break;
        case PLOCK_REQUEST_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockRequestAckMessagePayload>>(){}.getType());
            break;
        case PLOCK_REQUEST_QUORUM:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockRequestQuorumMessagePayload>>(){}.getType());
            break;
        case PLOCK_REQUEST_QUORUM_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<PLockRequestQuorumAckMessagePayload>>(){}.getType());
            break;
        case RELEASED_LOCKS:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<ReleasedLocksMessagePayload>>(){}.getType());
            break;
        case RELEASED_LOCKS_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<ReleasedLocksAckMessagePayload>>(){}.getType());
            break;
        case ROUTINE_KGROUP_LEADER_INFO:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<RoutineKGroupLeaderInfoMessagePayload>>(){}.getType());
            break;
        case TRIGGER_QUORUM:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<TriggerQuorumMessagePayload>>(){}.getType());
            break;
        case TRIGGER_QUORUM_ACK:
            message = gson.create().fromJson(jsonMsg,
                new TypeToken<Message<TriggerQuorumAckMessagePayload>>(){}.getType());
            break;
        default:
            break;
        }

        out.add(message);

    }

}
