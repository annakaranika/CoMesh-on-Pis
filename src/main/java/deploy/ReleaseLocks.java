package deploy;

import java.util.TimerTask;

import network.Network;

public class ReleaseLocks extends TimerTask {
    private final RoutineKGroupManager mngr;
    private final String rtnID;
    private final int rtnSeqNo;

    public ReleaseLocks(RoutineKGroupManager mngr, String rtnID, int rtnSeqNo) {
        this.mngr = mngr;
        this.rtnID = rtnID;
        this.rtnSeqNo = rtnSeqNo;
    }

    @Override
    public void run() {
        try {
            mngr.finishRtnExec(rtnID, rtnSeqNo);
        }
        catch (Exception e) {
            Network.log("Release locks timer task exception", true);
            e.printStackTrace();
        }
    }

}
