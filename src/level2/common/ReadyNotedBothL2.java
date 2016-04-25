package level2.common;

import TMopcUa.TMuaClient;
import com.prosysopc.ua.client.Subscription;
import level2.common.ReadyNotedParam;
import level2.common.Tag;
import level2.common.TagCreationException;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 25-Jan-16
 * Time: 3:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReadyNotedBothL2 extends ReadyNotedParam {

    public ReadyNotedBothL2(TMuaClient source, String equipment, String processElement, Tag[] tags,
                           Subscription subscription) throws TagCreationException {
        super(source, equipment, processElement, tags, subscription);
    }

    public void setReadWriteStat(boolean bReadyNotedWrite) {
        isReadyNotedWrite = bReadyNotedWrite;
        isReadyNotedRead = !isReadyNotedWrite;
    }

    public boolean isNewData(Tag theTag) {
        boolean retVal = false;
        if ((theTag.tagName == Tag.TagName.Ready) && isReadyNotedRead)
            if (getLevel2Tag(Tag.TagName.Ready).getValue().booleanValue) {
                getAllValues();
                retVal = true;
            } else
                setValue(Tag.TagName.Noted, false);
        else if ((theTag.tagName == Tag.TagName.Noted) && isReadyNotedWrite) {
            if (getLevel2Tag(Tag.TagName.Noted).getValue().booleanValue)
                setValue(Tag.TagName.Ready, false);
        }
        return retVal;
    }
}
