package level2.stripDFH;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.*;
import directFiredHeating.FceSection;
import level2.common.L2ParamGroup;
import level2.common.L2SubscriptionListener;
import level2.common.Tag;
import level2.common.TagCreationException;
import org.opcfoundation.ua.builtintypes.*;

import java.util.Calendar;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 07-Jan-15
 * Time: 1:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2DFHZone extends L2ParamGroup {
    FceSection theSection = null;
    Subscription subscription = null;
    boolean dataCollectionOn = false;
    Calendar lastAlive;
    Calendar switchTime;
    boolean subsTimeout = false;

    Hashtable<MonitoredDataItem, Tag> monitoredTags;
    boolean monitoredTagsReady = false;
    int fuelCharSteps;
    public Tag[] tagsFuelChrTotal;
    public Tag[] tagsFuelChrZone;
    public Tag[] tagsFuelChrSpeed;

    public L2DFHZone(L2DFHFurnace l2Furnace, String zoneName, String descriptiveName, FceSection theSection,
                     boolean bSubscribe) throws TagCreationException {
        super(l2Furnace, zoneName, descriptiveName);
        this.theSection = theSection;
        if (bSubscribe) {
            setSubscription(l2Furnace.source.createSubscription(new SubAliveListener(), new ZoneSubscriptionListener()));
        }
        String basePath = "";
        String temperatureFmt = "#,##0";
        String fuelFlowFmt = "#,##0.00";
        String speedFmt = "#,##0";
        String ratioFmt = "#0.00";
        String airFlowFmt = "#,##0";
        Tag[] temperatureTags = {new Tag(Parameter.Temperature, Tag.TagName.SP, false, false, temperatureFmt),
                new Tag(Parameter.Temperature, Tag.TagName.PV, false, true, temperatureFmt),
                new Tag(Parameter.Temperature, Tag.TagName.Auto, false, false),
                new Tag(Parameter.Temperature, Tag.TagName.SP, true, false, temperatureFmt)};

        Tag[] fuelFlowTags = {   // new Tag(Parameter.FuelFlow, Tag.TagName.SP, false, false, fuelFlowFmt),
                new Tag(Parameter.FuelFlow, Tag.TagName.PV, false, true, fuelFlowFmt),
                new Tag(Parameter.FuelFlow, Tag.TagName.Auto, false, false),
                new Tag(Parameter.FuelFlow, Tag.TagName.Remote, false, false),
                new Tag(Parameter.FuelFlow, Tag.TagName.Span, false, false, fuelFlowFmt),
                new Tag(Parameter.FuelFlow, Tag.TagName.SP, false, false, fuelFlowFmt),
                new Tag(Parameter.FuelFlow, Tag.TagName.SP, true, false, fuelFlowFmt)};

        Tag airFuelRatioTags = new Tag(Parameter.AFRatio, Tag.TagName.SP, false, false, ratioFmt);

        Tag[] airFlowTags = {
                new Tag(Parameter.AirFlow, Tag.TagName.SP, false, false, airFlowFmt),
                new Tag(Parameter.AirFlow, Tag.TagName.PV, false, true, airFlowFmt),
                new Tag(Parameter.AirFlow, Tag.TagName.Auto, false, false),
                new Tag(Parameter.AirFlow, Tag.TagName.Remote, false, false),
                new Tag(Parameter.AirFlow, Tag.TagName.Temperature, false, true, temperatureFmt)};
        fuelCharSteps = l2Furnace.furnaceSettings.fuelCharSteps;
        Tag[] allfuelChrTags = new Tag[fuelCharSteps * 3]; // totFuel, zoneFuel and Speed
        tagsFuelChrTotal = new Tag[fuelCharSteps];
        tagsFuelChrZone = new Tag[fuelCharSteps];
        tagsFuelChrSpeed = new Tag[fuelCharSteps];
        int pos = 0;
        for (int s = 1; s <= fuelCharSteps; s++) {
            Tag tTotFuel = new Tag(Parameter.FuelCharacteristic,
                    Tag.TagName.TotFuel.buildTag(s), true, false, fuelFlowFmt );
            tagsFuelChrTotal[s - 1] = tTotFuel;
            allfuelChrTags[pos++] = tTotFuel;
            Tag tZoneFuel = new Tag(Parameter.FuelCharacteristic,
                    Tag.TagName.ZoneFuel.buildTag(s), true, false, fuelFlowFmt);
            tagsFuelChrZone[s - 1] = tZoneFuel;
            allfuelChrTags[pos++] = tZoneFuel;
            Tag tSpeed = new Tag(Parameter.FuelCharacteristic,
                    Tag.TagName.Speed.buildTag(s), true, false, speedFmt);
            tagsFuelChrSpeed[s - 1] = tSpeed;
            allfuelChrTags[pos++] = tSpeed;
        }
        monitoredTags = new Hashtable<MonitoredDataItem, Tag>();
        addOneParameter(Parameter.Temperature, temperatureTags);
        addOneParameter(Parameter.FuelFlow, fuelFlowTags);
        addOneParameter(Parameter.AFRatio, airFuelRatioTags);
        addOneParameter(Parameter.AirFlow, airFlowTags);
//        addOneParameter(Parameter.FuelCharacteristic, fuelCharTags);
        addOneParameter(Parameter.FuelCharacteristic, allfuelChrTags);
        noteMonitoredTags(temperatureTags);
        noteMonitoredTags(fuelFlowTags);
        noteMonitoredTags(airFlowTags);
        monitoredTagsReady = true;
    }

    void noteMonitoredTags(Tag[] tags) {
        for (Tag tag : tags)
            if (tag.isMonitored())
                monitoredTags.put(tag.getMonitoredDataItem(), tag);
    }

    public double fuelFlowNow() {
        return getValue(Parameter.FuelFlow, Tag.TagName.PV).floatValue;
    }

    public boolean setFuelCharacteristic(L2ZonalFuelProfile zFP) {
        boolean retVal = true;
        double[][] fuelTable = zFP.oneZoneFuelArray(theSection.secNum - 1, theSection.botSection);
        if (theSection != null) {
            int steps = fuelTable.length;
            for (int s = 0; s < steps; s++) {
                setValue(Parameter.FuelCharacteristic, Tag.TagName.TotFuel.buildTag(s + 1), (float)fuelTable[s][0]);
                setValue(Parameter.FuelCharacteristic, Tag.TagName.ZoneFuel.buildTag(s + 1), (float)fuelTable[s][1]);
                setValue(Parameter.FuelCharacteristic, Tag.TagName.Speed.buildTag(s + 1), (float)fuelTable[s][2]);
            }
        }
        return retVal;
    }

    public void closeSubscriptions() {
        if (subscription != null)
            try {
                subscription.removeItems();
            } catch (ServiceException e) {
                e.printStackTrace();
            }
    }

    class SubAliveListener implements SubscriptionAliveListener {
        public void onAlive(Subscription s) {
            switchTime = Calendar.getInstance();
            dataCollectionOn = true;
            subsTimeout = false;
        }

        public void onTimeout(Subscription s) {
            switchTime = Calendar.getInstance();
            lastAlive = s.getLastAlive();
            dataCollectionOn = false;
            subsTimeout = true;
        }
    }

    class ZoneSubscriptionListener extends L2SubscriptionListener {
        public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
            String fromElement =  monitoredDataItem.toString();
//            info("From L2Zones " + groupName + ":fromElement-" + fromElement + ", VALUE: " + dataValue.getValue().toStringWithType());
            Tag theTag = monitoredTags.get(monitoredDataItem);
//            if ((monitoredTagsReady) && theTag.element == Parameter.Temperature) {
//                info(groupName + ".Temperature changed to " + theTag.getValue().floatValue );
//                ProcessValue v = setValue(Parameter.Temperature, Tag.TagName.SP, theTag.getValue().floatValue + 100);
//                if (!v.valid)
//                    showError(v.errorMessage);
//            }
         }

    }

    void info(String msg) {
        l2Interface.logInfo("L2Zone: " + msg);
    }

    void showError(String msg) {
        l2Interface.logError("L2Zone: " + msg);
    }
}
