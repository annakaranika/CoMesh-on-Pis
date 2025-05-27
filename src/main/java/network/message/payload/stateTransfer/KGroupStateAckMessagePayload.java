package network.message.payload.stateTransfer;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import configs.LockStrategy;
import deploy.KGroupManager;
import deploy.QuorumMsgInfo;
import javafx.util.Pair;
import kgroup.KGroupType;
import kgroup.state.DeviceLock;
import kgroup.state.LockRequest;
import kgroup.state.StateTransferStage;
import metrics.KGroupMetric;
import network.Network;
import network.message.MessageType;
import network.message.payload.MessagePayload;
import network.message.payload.ReplyMessagePayload;
import network.message.payload.lock.LockedMessagePayload;
import network.message.payload.lock.PLockCancelMessagePayload;
import network.message.payload.lock.PLockRequestedMessagePayload;

public class KGroupStateAckMessagePayload extends ReplyMessagePayload {
    private static final long serialVersionUID = -5768899016281379791L;

    public KGroupStateAckMessagePayload(int epochNo, KGroupType kGroupType,
                                        List<String> entitiesIDs, int srcSeqNo) {
        super(epochNo, kGroupType, entitiesIDs, srcSeqNo);
    }

    public KGroupStateAckMessagePayload(KGroupManager mngr, int srcSeqNo) {
        super(mngr, srcSeqNo);
    }

    public Pair<MessageType, ReplyMessagePayload> process(KGroupManager mngr, String src, int srcSeqNo) {
        if (mngr.getEpochNo() != epochNo || !mngr.isMemberOfKGroup()) {
            mngr.removeMsgInfo(this.srcSeqNo);
            return null;
        }

        QuorumMsgInfo msgInfo = mngr.getQuorumMsgInfo(this.srcSeqNo);
        if (msgInfo == null) {
            mngr.removeMsgInfo(this.srcSeqNo);
            return null;
        }
        
        if (msgInfo != null && !msgInfo.isApproved()) {
            msgInfo.newReply(src, true);
            final int neededAckCount = mngr.getF() + 1;
            mngr.log("Received new ack for state distribution (" + msgInfo.getPositiveRepliesCount()
                   + "/" + neededAckCount + " now)");
            if (msgInfo.getPositiveRepliesCount() >= neededAckCount) {
                msgInfo.setApproved(Network.getCurTS());
                long stateTransferDelay = KGroupMetric.completeStateTrnsfr(
                    mngr, this.srcSeqNo, Network.getCurTS()
                );
                mngr.log("state transfered to members for epoch " + epochNo
                       + "\n\tit lasted: " + stateTransferDelay
                       + "ms\n\tgot replies from: " + msgInfo.getRepliedNodesIDs()
                       + "\n\tlocal state: " + mngr.getState());
                mngr.advStateTrnsfrStg(StateTransferStage.COMPLETE);
                mngr.newMntrTask();
                mngr.openReceivingEnd();
                switch (kGroupType) {
                case DEVICE:
                    for (String devID: entitiesIDs) {
                        // if a device's lock is acquired,
                        // let the locker routine's k-group know in case
                        // the device k-group did not succeed before the epoch change
                        if (mngr.isLocked(devID)) {
                            String rtnID = mngr.getDevState(devID).getLocker().getRtnID();
                            int rtnSeqNo = mngr.getDevState(devID).getLocker().getRtnSeqNo();
                            mngr.log("Device " + devID + " k-group: Reminding routine "
                                    + rtnID + "-" + rtnSeqNo
                                    + " that it has the device's lock");
                            mngr.log(mngr.getRtnKGrp(rtnID).getMostProbableLeader(epochNo));
                            mngr.remoteCall(KGroupType.ROUTINE, rtnID, MessageType.LOCKED,
                                new LockedMessagePayload(
                                    epochNo, rtnID, devID, rtnSeqNo),
                                false
                            );
                        }
                        // if a device is not locked but the queue is not empty either
                        else if (!mngr.isLocked(devID) &&
                                 !mngr.getDevKGrp(devID).isLockQueueEmpty(devID)) {
                            if (mngr.isLockStrategy(LockStrategy.SERIAL)) {
                                String rtnID = mngr.getDevState(devID).getQueueHead().getRtnID();

                                mngr.gatherQuorumForDevLockAcquisition(rtnID,
                                    mngr.getDevState(devID).getQueueHead().getRtnSeqNo(), devID);
                            } else {
                                DeviceLock lock = mngr.getDevState(devID);
                                LockRequest request = lock.getQueueHead();
                                String rtnID = request.getRtnID();
                                int rtnSeqNo = request.getRtnSeqNo();
                                int reqSeqNo = lock.getQueueHeadReqSeqNo();
                                MessagePayload pLockRequestedPayload =
                                    new PLockRequestedMessagePayload(
                                        epochNo, rtnID, rtnSeqNo, reqSeqNo, devID);

                                mngr.remoteCall(KGroupType.ROUTINE, rtnID,
                                        MessageType.PLOCK_REQUESTED,
                                        pLockRequestedPayload, false);
                            }
                        }

                        // if there are requests in the newLockRequests queue
                        if (!mngr.isNewLockRequestsEmpty(devID)) {
                            mngr.log("---- Dev " + devID + " has new requests in " +
                                         Network.getMyID() + ". Asking for quorum. ---" );
                            mngr.log("    kgroup state: " + mngr.getDevState(devID).toString());
                            // }
                            // resend LOCK_REQUEST_QUORUM to move lock requests to the queue
                            Map<Integer, LockRequest> newLockRequests
                                = mngr.getNewLockRequests(devID);
                            for (Entry<Integer, LockRequest> req: newLockRequests.entrySet()) {
                                LockRequest request = req.getValue();
                                String rtnID = request.getRtnID();
                                int rtnSeqNo = request.getRtnSeqNo();
                                int reqSeqNo = req.getKey();
                                if (mngr.isLockStrategy(LockStrategy.SERIAL)) {
                                    mngr.gatherQuorumForDevLockRequest(
                                        rtnID, rtnSeqNo, devID, reqSeqNo);
                                } else {
                                    mngr.gatherQuorumForDevPLockRequest(
                                        rtnID, rtnSeqNo, devID, reqSeqNo);
                                }
                            }
                        }
                    }
                    break;
                
                case ROUTINE:
                    // 1. resend lock request for next device to lock
                    //    in case a lock request was lost before
                    // 2. if routine had previously started execution
                    //    check to see if it has stopped or not
                    // 3. if routine had finished execution but not
                    //    released all device locks, release them now
                    for (String rtnID: entitiesIDs) {
                        for (Integer rtnSeqNo: mngr.getRtnSeqNos(rtnID)) {
                            if (mngr.isRtnAcquiringLocks(rtnID, rtnSeqNo)) {
                                if (mngr.isLockStrategy(LockStrategy.SERIAL)) {
                                    mngr.requestDevLockForRtn(rtnID, rtnSeqNo);
                                } else {
                                    mngr.requestDevLockForRtnInParallel(rtnID, rtnSeqNo);
                                }
                            }
                            else if (mngr.isRtnExecuting(rtnID, rtnSeqNo)) {
                                long rtnEndTS = mngr.getRtnEndTS(rtnID, rtnSeqNo);
                                long curTS = Network.getCurTS();
                                if (rtnEndTS < curTS) {
                                    mngr.finishRtnExec(rtnID, rtnSeqNo);
                                }
                                else if (rtnEndTS > curTS) {
                                    mngr.schedLockRelease(rtnID, rtnSeqNo, rtnEndTS - curTS);
                                }
                            }
                            else if (mngr.isRtnReleasingLocks(rtnID, rtnSeqNo)) {
                                for (String devID: mngr.getAllUnreleasedDevIDs(rtnID, rtnSeqNo)) {
                                    mngr.releaseDevLockForRtn(rtnID, rtnSeqNo, devID);
                                }
                            }
                            else if (mngr.isLockAcquireFailed(rtnID, rtnSeqNo)) {
                                for (String devID: mngr.getTouchedDevIDs(rtnID)) {
                                    PLockCancelMessagePayload cancelPayload =
                                            new PLockCancelMessagePayload(epochNo, devID, rtnID, rtnSeqNo);
                                    mngr.remoteCall(KGroupType.DEVICE, devID,
                                            MessageType.PLOCK_CANCEL, cancelPayload, false);
                                }
                            }
                        }
                    }
                    break;
                default:
                    break;
                }
                mngr.removeMsgInfo(this.srcSeqNo);
            }
        }
        return null;
    }
}
