package level2;

import TMopcUa.TMuaClient;
import com.prosysopc.ua.client.Subscription;
import level2.common.L2ZoneParam;
import level2.common.Tag;
import level2.common.TagCreationException;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 02-Feb-15
 * Time: 11:19 AM
 * A group of data with ready/Noted flags.
 * I can be ready from Level1 and Noted from Level2 or the other way around
 */
public class ReadyNotedParam extends L2ZoneParam {
    boolean isReadyNotedRead = false;   // ready inLevel1 and noted form Level2
    boolean isReadyNotedWrite = false;  // ready in level2 and noted in Level1

    public ReadyNotedParam(TMuaClient source, String equipment, String processElement, Tag[] tags,
                           Subscription subscription) throws TagCreationException {
        super(source, equipment, processElement, tags, subscription);
        checkReadyNoted();
    }

    public boolean isNewData(Tag theTag) {
        boolean retVal = false;
        if ((theTag.tagName == Tag.TagName.Ready) && isReadyNotedRead)
            if (getValue(Tag.TagName.Ready).booleanValue) {
                getAllValues();
                retVal = true;
            } else
                setValue(Tag.TagName.Noted, false);
        else if ((theTag.tagName == Tag.TagName.Noted) && isReadyNotedWrite) {
            if (getValue(Tag.TagName.Noted).booleanValue)
                setValue(Tag.TagName.Ready, false);
        }
        return retVal;
    }

    void initStatus() {
        if (isReadyNotedRead)
            setAsNoted(false);
        if (isReadyNotedWrite)
            markReady(false);
    }

    /**
     * Read all values
     */
    void getAllValues() {
        for (Tag tag:processTagList.values())
            tag.getValue();
    }

    private void checkReadyNoted() {
        isReadyNotedRead = processTagList.containsKey(Tag.TagName.Ready) && level2TagList.containsKey(Tag.TagName.Noted);
        isReadyNotedWrite = level2TagList.containsKey(Tag.TagName.Ready) && processTagList.containsKey(Tag.TagName.Noted);
    }

    public void setAsNoted(boolean noted) {
        if (isReadyNotedRead)
            setValue(Tag.TagName.Noted, noted);
     }

    public boolean markReady(boolean ready) {
        boolean retVal = false;
        if (isReadyNotedWrite) {
            setValue(Tag.TagName.Ready, ready);
//            if (!getValue(Tag.TagName.Noted).booleanValue) {
//                setValue(Tag.TagName.Ready, true);
                retVal = true;
        }

        return retVal;
    }



    public boolean isNoted() {
        return (isReadyNotedWrite && getValue(Tag.TagName.Noted).booleanValue);
    }
}
