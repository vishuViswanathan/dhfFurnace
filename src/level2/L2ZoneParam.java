package level2;

import TMopcUa.OneDataGroup;
import TMopcUa.ProcessData;
import TMopcUa.ProcessValue;
import TMopcUa.TMuaClient;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.client.Subscription;
import directFiredHeating.FceSection;

import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 22-Sep-14
 * Time: 3:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2ZoneParam extends OneDataGroup {
    String zoneAndParameter;    // eg.DFHZone3.temperature
//    Hashtable<Tag, ProcessData> tagList;
    Hashtable<Tag.TagName, Tag> processTagList;
    Hashtable<Tag.TagName, Tag> level2TagList;

    /**
     * @param source         is TUaClient connecte to Level1
     * @param equipment      eg. DFHFurnae, PotFurnace ...
     * @param processElement Temperature, FuelFlow ..
     * @param tags           a tag list for PV, SP etc.
     * @param subscription   a common subscription already created to be used for subscribed data
     */
    public L2ZoneParam(TMuaClient source, String equipment, String processElement, Tag[] tags,
                       Subscription subscription) throws TagCreationException {
        super(source, equipment, processElement, subscription);
        processTagList = new Hashtable<Tag.TagName, Tag>();
        level2TagList = new Hashtable<Tag.TagName, Tag>();
        for (Tag tag : tags) {
            try {
                tag.setProcessData(getProcessData(tag.dataSource, "" + tag, tag.dataType, tag.access, tag.subscribe));
                if (tag.subscribe && (subscription == null))
                    throw new TagCreationException("" + tag, "Subscription is null");
                if (tag.dataSource == ProcessData.Source.LEVEL2)
                    level2TagList.put(tag.tagName, tag);
                else
                    processTagList.put(tag.tagName, tag);
            } catch (Exception e) {
                throw new TagCreationException("" + tag, "Data Point could not be created!\n" + e.getMessage());
            }
        }
    }

    public L2ZoneParam(TMuaClient source, String equipment, String processElement, Tag tag,
                       Subscription subscription) throws TagCreationException {
        super(source, equipment, processElement, subscription);
        processTagList = new Hashtable<Tag.TagName, Tag>();
        level2TagList = new Hashtable<Tag.TagName, Tag>();
        try {
            tag.setProcessData(getProcessData(tag.dataSource, "" + tag, tag.dataType, tag.access, tag.subscribe));
            if (tag.dataSource == ProcessData.Source.LEVEL2)
                level2TagList.put(tag.tagName, tag);
            else
                processTagList.put(tag.tagName, tag);
        } catch (Exception e) {
            throw new TagCreationException("" + tag, "Data Point could not be created!\n" + e.getMessage());
        }
    }

    Tag getProcessTag(Tag.TagName tagName) {
        return processTagList.get(tagName);
    }

    Tag getLevel2Tag(Tag.TagName tagName) {
        return level2TagList.get(tagName);
    }

    ProcessValue getValue(Tag.TagName tagName) {
        Tag theTag = getProcessTag(tagName);
        return theTag.getValue();
    }

    public ProcessValue processValue(Tag.TagName tagName) {
        return getProcessTag(tagName).processValue();
    }

    ProcessValue setValue(Tag.TagName tagName, float newValue) {
        Tag theTag = getLevel2Tag(tagName);
        return theTag.setValue(newValue);
    }

    ProcessValue setValue(Tag.TagName tagName, boolean newValue) {
        Tag theTag = getLevel2Tag(tagName);
        return theTag.setValue(newValue);
    }

    ProcessValue setValue(Tag.TagName tagName, double newValue) {
        Tag theTag = getLevel2Tag(tagName);
        return theTag.setValue(newValue);
     }

    ProcessValue setValue(Tag.TagName tagName, String newValue) {
        Tag theTag = getLevel2Tag(tagName);
        return theTag.setValue(newValue);
    }

//    public boolean getFloatValue(Tag.TagName tagName) {
//        float retVal;
//        Tag theTag;
//        if (tagList.containsKey(tagName))
//            theTag = tagList.get(tagName);
//        else
//            return false;
//    }


    public String toString() {
        return zoneAndParameter;
    }
}
