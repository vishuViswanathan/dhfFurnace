package level2.simulator;

import TMopcUa.ProcessValue;
import TMopcUa.TMuaClient;
import com.prosysopc.ua.client.Subscription;
import level2.common.ReadyNotedParam;
import level2.common.Tag;
import level2.common.TagCreationException;

/**
 * User: M Viswanathan
 * Date: 02-Mar-16
 * Time: 2:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimulationReadyNoted extends ReadyNotedParam {
    public SimulationReadyNoted(TMuaClient source, String equipment, String processElement, Tag[] tags,
                           Subscription subscription) throws TagCreationException {
        super(source, equipment, processElement, tags, subscription);
        checkReadyNoted();
    }

    protected void checkReadyNoted() {
        // reverse of the original
        isReadyNotedWrite = processTagList.containsKey(Tag.TagName.Ready) && level2TagList.containsKey(Tag.TagName.Noted);
        isReadyNotedRead = level2TagList.containsKey(Tag.TagName.Ready) && processTagList.containsKey(Tag.TagName.Noted);
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

    /**
     * Read all values
     */
    protected void getAllValues() {
        for (Tag tag:level2TagList.values())
            tag.getValue();
    }

    public ProcessValue getValue(Tag.TagName tagName) {
        Tag theTag = getLevel2Tag(tagName);
        return theTag.getValue();
    }

    public ProcessValue setValue(Tag.TagName tagName, float newValue) {
        Tag theTag = getProcessTag(tagName);
        return theTag.setValue(newValue);
    }

    public ProcessValue setValue(Tag.TagName tagName, boolean newValue) {
        Tag theTag = getProcessTag(tagName);
        return theTag.setValue(newValue);
    }

    public ProcessValue setValue(Tag.TagName tagName, double newValue) {
        Tag theTag = getProcessTag(tagName);
        return theTag.setValue(newValue);
    }

    public ProcessValue setValue(Tag.TagName tagName, String newValue) {
        Tag theTag = getProcessTag(tagName);
        return theTag.setValue(newValue);
    }


}
