package deploy;

import java.util.TimerTask;

public abstract class ResendTask extends TimerTask {
    protected KGroupManager mngr;
    protected int seqNo;
    // private MsgInfo msgInfo;

    public ResendTask(KGroupManager mngr, int seqNo) {
        this.mngr = mngr;
        this.seqNo = seqNo;
        // this.msgInfo = msgInfo;
    }

    protected UnicastMsgInfo getUnicastMsgInfo() {
        return (UnicastMsgInfo) mngr.getUnicastMsgInfo(seqNo);
    }

    protected QuorumMsgInfo getQuorumMsgInfo() {
        return (QuorumMsgInfo) mngr.getQuorumMsgInfo(seqNo);
    }

    protected static long getIntervalMillis(long first, long second) {
        return second - first;
    }
}
