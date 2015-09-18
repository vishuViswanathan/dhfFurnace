package level2;

import TMopcUa.ProcessValue;
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
public class L2Zone extends L2ParamGroup {
    FceSection theSection = null;
    Subscription subscription = null;
    boolean dataCollectionOn = false;
    Calendar lastAlive;
    Calendar switchTime;
    boolean subsTimeout = false;

    Hashtable<MonitoredDataItem, Tag> monitoredTags;
    boolean monitoredTagsReady = false;
    int fuelCharSteps;

    public L2Zone(L2DFHFurnace l2Furnace, String zoneName, FceSection theSection,
                  boolean bSubscribe) throws TagCreationException {
        super(l2Furnace, zoneName);
        this.theSection = theSection;
        if (bSubscribe) {
            setSubscription(l2Furnace.source.createSubscription(new SubAliveListener(), new ZoneSubscriptionListener()));
        }
        String basePath = "";
        Tag[] temperatureTags = {new Tag(Parameter.Temperature, Tag.TagName.SP, false, false),
                new Tag(Parameter.Temperature, Tag.TagName.PV, false, true),
                new Tag(Parameter.Temperature, Tag.TagName.Auto, false, false),
                new Tag(Parameter.Temperature, Tag.TagName.SP, true, false)};
        Tag[] fuelFlowTags = {new Tag(Parameter.FuelFlow, Tag.TagName.SP, false, false),
                new Tag(Parameter.FuelFlow, Tag.TagName.PV, false, true),
                new Tag(Parameter.FuelFlow, Tag.TagName.Auto, false, false),
                new Tag(Parameter.FuelFlow, Tag.TagName.Remote, false, false),
                new Tag(Parameter.FuelFlow, Tag.TagName.Span, false, false),
                new Tag(Parameter.FuelFlow, Tag.TagName.SP, false, false),
                new Tag(Parameter.FuelFlow, Tag.TagName.SP, true, false)};
        Tag[] airFlowTags = {new Tag(Parameter.AirFlow, Tag.TagName.SP, false, false),
                new Tag(Parameter.AirFlow, Tag.TagName.PV, false, true),
                new Tag(Parameter.AirFlow, Tag.TagName.Auto, false, false),
                new Tag(Parameter.AirFlow, Tag.TagName.Remote, false, false),
                new Tag(Parameter.AirFlow, Tag.TagName.Temperature, false, true)};
        fuelCharSteps = l2Furnace.furnaceSettings.fuelCharSteps;
        Tag[] fuelCharTags = new Tag[fuelCharSteps * 2];
        int pos = 0;
        for (int s = 1; s <= fuelCharSteps; s++) {
            fuelCharTags[pos++] = new Tag(Parameter.FuelCharacteristic,
                    Tag.TagName.getEnum("X" + ("" + s).trim()), true, false);
            fuelCharTags[pos++] = new Tag(Parameter.FuelCharacteristic,
                    Tag.TagName.getEnum("Y" + ("" + s).trim()), true, false);
        }
//        Tag[] fuelCharTags = {new Tag(Parameter.FuelCharacteristic, Tag.TagName.X1, true, false),
//                new Tag(Parameter.FuelCharacteristic, Tag.TagName.X2, true, false),
//                new Tag(Parameter.FuelCharacteristic, Tag.TagName.X3, true, false),
//                new Tag(Parameter.FuelCharacteristic, Tag.TagName.X4, true, false),
//                new Tag(Parameter.FuelCharacteristic, Tag.TagName.X5, true, false),
//                new Tag(Parameter.FuelCharacteristic, Tag.TagName.Y1, true, false),
//                new Tag(Parameter.FuelCharacteristic, Tag.TagName.Y2, true, false),
//                new Tag(Parameter.FuelCharacteristic, Tag.TagName.Y3, true, false),
//                new Tag(Parameter.FuelCharacteristic, Tag.TagName.Y4, true, false),
//                new Tag(Parameter.FuelCharacteristic, Tag.TagName.Y5, true, false)
//        };
        monitoredTags = new Hashtable<MonitoredDataItem, Tag>();
        addOneParameter(Parameter.Temperature, temperatureTags);
        addOneParameter(Parameter.FuelFlow, fuelFlowTags);
        addOneParameter(Parameter.AirFlow, airFlowTags);
        addOneParameter(Parameter.FuelCharacteristic, fuelCharTags);
        noteMonitoredTags(temperatureTags);
        noteMonitoredTags(fuelFlowTags);
        noteMonitoredTags(airFlowTags);
        monitoredTagsReady = true;
    }

    /**
     * For common sections with external subscription
     * @param l2Furnace
     * @param zoneName
     * @param subscription
     */
    public L2Zone(L2DFHFurnace l2Furnace, String zoneName, Subscription subscription) {
        super(l2Furnace, zoneName, subscription);
    }

    void noteMonitoredTags(Tag[] tags) {
        for (Tag tag : tags)
            if (tag.isMonitored())
                monitoredTags.put(tag.getMonitoredDataItem(), tag);
    }

    public boolean setFuelCharacteristic(L2ZonalFuelProfile zFP) {
        boolean retVal = true;
        double[][] fuelTable = zFP.oneZoneFuelArray(theSection.secNum - 1, theSection.botSection);
        if (theSection != null) {
            int steps = fuelTable.length;
            for (int s = 0; s < steps; s++) {
                setValue(Parameter.FuelCharacteristic, Tag.TagName.getEnum("X" + ("" + (s + 1)).trim()), (float)fuelTable[s][0]);
                setValue(Parameter.FuelCharacteristic, Tag.TagName.getEnum("Y" + ("" + (s + 1)).trim()), (float)fuelTable[s][1]);
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
            info("From L2Zones " + groupName + ":fromElement-" + fromElement + ", VALUE: " + dataValue.getValue().toStringWithType());
            Tag theTag = monitoredTags.get(monitoredDataItem);
            if ((monitoredTagsReady) && theTag.element == Parameter.Temperature) {
                ProcessValue v = setValue(Parameter.Temperature, Tag.TagName.SP, theTag.getValue().floatValue + 100);
                if (!v.valid)
                    showError(v.errorMessage);
            }
         }

    }

    void info(String msg) {
        l2Interface.info("L2Zone: " + msg);
    }

    void showError(String msg) {
        l2Interface.error("L2Zone: " + msg);
    }
}
