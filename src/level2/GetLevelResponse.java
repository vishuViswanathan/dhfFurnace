package level2;

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

    public GetLevelResponse(ReadyNotedParam param) {
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
        return retVal;
    }
}
