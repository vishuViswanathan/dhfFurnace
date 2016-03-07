package level2;

import com.sun.org.apache.bcel.internal.generic.L2I;
import level2.common.L2Interface;
import level2.common.Tag;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 14-Sep-15
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetLevelResponse {
    ReadyNotedParam readyNotedParam;
    L2Interface l2Interface;

    public GetLevelResponse(ReadyNotedParam param, L2Interface l2Interface) {
        this.l2Interface = l2Interface;
        this.readyNotedParam = param;
    }

    boolean getResponse(String msg, int waitSeconds) {
        boolean retVal = false;
        if (!readyNotedParam.isNoted()) {
            readyNotedParam.setValue(Tag.TagName.Msg, msg);
            readyNotedParam.markReady(true);
            int nowTime = 0;
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
                if (readyNotedParam.isNoted()) {
                    retVal = true;
                    break;
                }
            } while (++nowTime < waitSeconds);
            readyNotedParam.markReady(false);
        }
        else
            l2Interface.logInfo("GetLevel1Response: Last message is not yet noted by Level1");
        return retVal;
    }
}
