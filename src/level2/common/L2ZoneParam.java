package level2.common;

import TMopcUa.OneDataGroup;
import TMopcUa.ProcessData;
import TMopcUa.ProcessValue;
import TMopcUa.TMuaClient;
import com.prosysopc.ua.client.Subscription;
import level2.common.Tag;
import level2.common.TagCreationException;
import mvUtils.display.ErrorStatAndMsg;

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
    public Hashtable<Tag.TagName, Tag> processTagList;
    public Hashtable<Tag.TagName, Tag> level2TagList;

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
                tag.setProcessData(getProcessData(tag.dataSource, "" + tag, tag.dataType, tag.access, tag.bSubscribe));
                if (tag.bSubscribe && (subscription == null))
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
            tag.setProcessData(getProcessData(tag.dataSource, "" + tag, tag.dataType, tag.access, tag.bSubscribe));
            if (tag.dataSource == ProcessData.Source.LEVEL2)
                level2TagList.put(tag.tagName, tag);
            else
                processTagList.put(tag.tagName, tag);
        } catch (Exception e) {
            throw new TagCreationException("" + tag, "Data Point could not be created!\n" + e.getMessage());
        }
    }

    public Tag getProcessTag(Tag.TagName tagName) {
        return processTagList.get(tagName);
    }

    public Tag getLevel2Tag(Tag.TagName tagName) {
        return level2TagList.get(tagName);
    }

    public ProcessValue getValue(Tag.TagName tagName) {
        Tag theTag = getProcessTag(tagName);
        return theTag.getValue();
    }

    public ProcessValue processValue(Tag.TagName tagName) {
        return getProcessTag(tagName).processValue();
    }

    public ProcessValue setValue(Tag.TagName tagName, float newValue) {
        Tag theTag = getLevel2Tag(tagName);
        return theTag.setValue(newValue);
    }

    public ProcessValue setValue(Tag.TagName tagName, boolean newValue) {
        Tag theTag = getLevel2Tag(tagName);
        return theTag.setValue(newValue);
    }

    public ProcessValue setValue(Tag.TagName tagName, double newValue) {
        Tag theTag = getLevel2Tag(tagName);
        return theTag.setValue(newValue);
     }

    public ProcessValue setValue(Tag.TagName tagName, String newValue) {
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


    public ErrorStatAndMsg checkConnections() {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg(false, "");
        ErrorStatAndMsg processStat = new ErrorStatAndMsg(false, "process." + basePath + " tags:");
        boolean additional = false;
        for (Tag t: processTagList.values()) {
             if (!t.checkConnection()) {
                 processStat.inError = true;
                 processStat.msg += ((additional) ? ", " : "") + t.tagName;
                 additional = true;
             }
        }
        if (processStat.inError) {
            retVal.inError = true;
            retVal.msg += "\n" + processStat.msg;
        }
        additional = false;
        ErrorStatAndMsg level2Stat = new ErrorStatAndMsg(false, "level2." + basePath + " tags:");
        for (Tag t: level2TagList.values()) {
            if (!t.checkConnection()) {
                level2Stat.inError = true;
                level2Stat.msg += ((additional) ? ", " : "") +t.tagName;
                additional = true;
            }
        }
        if (level2Stat.inError) {
            retVal.inError = true;
            retVal.msg += "\n" + level2Stat.msg;
        }
        return retVal;
    }

    public String toString() {
        return zoneAndParameter;
    }
}
