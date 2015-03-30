package level2;

import FceElements.heatExchanger.HeatExchProps;
import TMopcUa.TMuaClient;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionAliveListener;
import directFiredHeating.DFHFurnace;
import directFiredHeating.FceSection;
import level2.fieldResults.FieldResults;
import level2.listeners.L2SubscriptionListener;
import mvXML.ValAndPos;
import mvXML.XMLmv;
import mvmath.DoubleMV;
import org.opcfoundation.ua.builtintypes.DataValue;
import performance.stripFce.OneZone;
import performance.stripFce.Performance;

import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.LinkedHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 05-Jan-15
 * Time: 10:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class L2DFHFurnace extends DFHFurnace {
    TMuaClient source;
    FurnaceSettings furnaceSettings;
    LinkedHashMap<FceSection, L2Zone> zones;
    ReadyNotedParam l2InfoMessages;
    ReadyNotedParam l2ErrorMessages;
    ReadyNotedParam l2StripParams;
    String equipment;
    Subscription messageSub;
    Subscription stripSub;
    boolean messagesON = false;
    Hashtable<MonitoredDataItem, Tag> monitoredTags;
    boolean monitoredTagsReady = false;
    L2DFHeating l2DFHeating;
    double exitTempAllowance = 5;

    public L2DFHFurnace(L2DFHeating l2DFHEating, boolean bTopBot, boolean bAddTopSoak, ActionListener listener) {
        super(l2DFHEating, bTopBot, bAddTopSoak, listener);
        this.l2DFHeating = l2DFHEating;
        source = l2DFHEating.uaClient;
        source.removeAllSubscriptions();
        this.equipment = l2DFHEating.equipment;
        monitoredTags = new Hashtable<MonitoredDataItem, Tag>();
        zones = new LinkedHashMap<FceSection, L2Zone>();
        messagesON = createL2Messages();
        createStripParam();
    }

    boolean createStripParam() {
        boolean retVal = false;
        stripSub = source.createSubscription(new SubAliveListener(), new StripListener());
        Tag[] stripTags = {new Tag(L2ParamGroup.Parameter.Data, Tag.TagName.Process, false, false),
                new Tag(L2ParamGroup.Parameter.Data, Tag.TagName.Ready, false, true),
                new Tag(L2ParamGroup.Parameter.Data, Tag.TagName.Thick, false, false),
                new Tag(L2ParamGroup.Parameter.Data, Tag.TagName.Width, false, false),
                new Tag(L2ParamGroup.Parameter.ErrMsg, Tag.TagName.Noted, true, false)};
        try {
            l2StripParams = new ReadyNotedParam(source, equipment, "Strip.Data", stripTags, stripSub);
            noteMonitoredTags(stripTags);
        } catch (TagCreationException e) {
            e.printStackTrace();
        }
        return true;
    }

    boolean createL2Messages() {
        boolean retVal = false;
        messageSub = source.createSubscription(new SubAliveListener(), new MessageListener());
        Tag[] errMessageTags = {new Tag(L2ParamGroup.Parameter.ErrMsg, Tag.TagName.Msg, false, false),
                new Tag(L2ParamGroup.Parameter.ErrMsg, Tag.TagName.Ready, false, true),
                new Tag(L2ParamGroup.Parameter.ErrMsg, Tag.TagName.Msg, true, false),
                new Tag(L2ParamGroup.Parameter.ErrMsg, Tag.TagName.Ready, true, false),
                new Tag(L2ParamGroup.Parameter.ErrMsg, Tag.TagName.Noted, true, false),  // sending 'Noted' to Process
                new Tag(L2ParamGroup.Parameter.ErrMsg, Tag.TagName.Noted, false, true)  // reading 'Noted' from Process
        };
        Tag[] infoMessageTags = {new Tag(L2ParamGroup.Parameter.InfoMsg, Tag.TagName.Msg, false, false),
                new Tag(L2ParamGroup.Parameter.InfoMsg, Tag.TagName.Ready, false, true),
                new Tag(L2ParamGroup.Parameter.InfoMsg, Tag.TagName.Msg, true, false),
                new Tag(L2ParamGroup.Parameter.InfoMsg, Tag.TagName.Ready, true, false),
                new Tag(L2ParamGroup.Parameter.InfoMsg, Tag.TagName.Noted, true, false),  // sending 'Noted' to Process
                new Tag(L2ParamGroup.Parameter.InfoMsg, Tag.TagName.Noted, false, true)   // reading 'Noted' from Process
        };
        try {
            l2InfoMessages = new ReadyNotedParam(source, equipment, "Messages.InfoMsg", infoMessageTags, messageSub);
            l2ErrorMessages = new ReadyNotedParam(source, equipment, "Messages.ErrorMsg", errMessageTags, messageSub);
            noteMonitoredTags(errMessageTags);
            noteMonitoredTags(infoMessageTags);
            monitoredTagsReady = true;
            messagesON = true;
        } catch (TagCreationException e) {
            e.printStackTrace();
        }
        return retVal;
    }

    void noteMonitoredTags(Tag[] tags) {
        for (Tag tag : tags)
            if (tag.isMonitored())
                monitoredTags.put(tag.getMonitoredDataItem(), tag);
    }

    boolean createL2Zones() {
        boolean retVal = true;
        zones.clear();
        int zNum = 1;
        String zoneName = "";
        try {
            for (FceSection sec : topSections) {
                if (sec.isActive()) {
                    zoneName = "DFHZone" + ("" + zNum).trim();
                    L2Zone oneZone = new L2Zone(this, zoneName, sec, true);
                    zones.put(sec, oneZone);
                    zNum++;
                } else
                    break;
            }
            furnaceSettings = new FurnaceSettings(l2DFHeating);
            furnaceSettings.setMaxSpeed(1000);
            double[] fuelRange = new double[zones.size()];
            for (int z = 0; z < fuelRange.length; z++)
                fuelRange[z] = 100; // some Value
            furnaceSettings.setFuelRanges(fuelRange);
        } catch (TagCreationException e) {
            e.setEquipment(equipment, "Creating L2 Zones");
            showError("Error in " + zoneName + " - " + e.getMessage());
            retVal = false;
        }
        return retVal;
    }

    public FurnaceSettings getFurnaceSettings() {
        return furnaceSettings;
    }

    public boolean prepareForDisconnection() {
        for (FceSection sec : zones.keySet())
            zones.get(sec).closeSubscriptions();
        try {
            messageSub.removeItems();
            stripSub.removeItems();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return true;
    }

    Performance getBasePerformance(String forProcess, double stripThick) {
        Performance prf = null;
        OneStripDFHProcess stripDFHProc = l2DFHeating.getStripDFHProcess(forProcess);
        if (stripDFHProc != null)
            prf = performBase.getRefPerformance(stripDFHProc.getChMaterial(forProcess, stripThick),
                    stripDFHProc.tempDFHExit, exitTempAllowance);
        return prf;
    }

    public void setExitTempAllowance(double exitTempAllowance) {
        this.exitTempAllowance = exitTempAllowance;
    }

    public double getExitTempAllowance() {
        return exitTempAllowance;
    }

    //    L2ZonalFuelProfile getZonalFuelProfile(PerformanceTable perfTable, int steps) {
//        L2ZonalFuelProfile fuelProfile = new L2ZonalFuelProfile(perfTable, steps, controller);
//    }

    public String fceSettingsInXML() {
        return furnaceSettings.dataInXML().toString();
    }

    public boolean takeFceSettingsFromXML(String xmlStr) {
        boolean retVal = true;
        furnaceSettings = new FurnaceSettings(l2DFHeating, xmlStr);
        if (furnaceSettings.inError) {
            controller.showError("Reading Furnace Settings: " + furnaceSettings.errMsg);
            retVal = false;
        }
        return retVal;
    }

    public StringBuilder fieldResultsInXML() {
        return new StringBuilder(XMLmv.putTag("fieldResults", createOneFieldResults().dataInXML()));
    }

    /**
     * creates FieldResults type data from the calculated Results
     */
    FieldResults createOneFieldResults() {
        FieldResults fieldResults = new FieldResults(this);
        fieldResults.takeFromCalculations();
        return fieldResults;
    }

    public FceSection getOneSection(boolean bBot, int nSec) {
        return ((bBot) ? botSections : topSections).get(nSec);
    }

    FieldResults oneFieldResults;

    public boolean setFieldProductionData() {
        if (oneFieldResults != null) {
            oneFieldResults.compareResults();
            l2DFHeating.setFieldProductionData(oneFieldResults.production, oneFieldResults.commonAirTemp,
                    oneFieldResults.commonFuelTemp);
            return true;
        }
        else
            return false;

    }

    public boolean takeFieldResultsFromXML(String xmlStr) {
        boolean retVal = false;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "fieldResults", 0);
        if (vp.val.length() > 10) {
            oneFieldResults = new FieldResults(this, xmlStr);
            retVal = !oneFieldResults.inError;
            if (!retVal)
                controller.showError(oneFieldResults.errMsg);
        }
        else
            controller.showError("Field Results data NOT found!");
        return retVal;
    }

    public HeatExchProps getAirHeatExchProps() {
        return airRecu.getHeatExchProps(recuCounterFlow.isSelected());
    }

    void handleNewStrip() {
        double stripWidth = DoubleMV.round(l2StripParams.processValue(Tag.TagName.Width).floatValue, 3) / 1000;
        double  stripThick = DoubleMV.round(l2StripParams.processValue(Tag.TagName.Thick).floatValue, 3) / 1000;
        String process = l2StripParams.processValue(Tag.TagName.Process).stringValue;
        String msg = "New Strip " + stripWidth + " x " + stripThick + " for process " + process;
        controller.showMessage(msg);
        l2StripParams.setAsNoted();

        Performance refP = getBasePerformance(process, stripThick);
        if (refP != null) {
            L2ZonalFuelProfile zFP = new L2ZonalFuelProfile(refP.getPerformanceTable(),
                    furnaceSettings.fuelCharSteps, l2DFHeating);
            zFP.prepareFuelTable(stripWidth, stripThick);
            String fuelMsg = "";
            DecimalFormat fmt = new DecimalFormat("#,##0.0");
            for (int z = 0; z < nTopActiveSecs; z++) {
                double[][] z2fuelTable = zFP.oneZoneFuelArray(z, false);
                fuelMsg += "Fuel Table for Zone#" + z + "\n";
                for (int r = 0; r < z2fuelTable.length; r++)
                    fuelMsg += "Total " + fmt.format(z2fuelTable[r][0]) + ", Zonal " +
                            fmt.format(z2fuelTable[r][1]) + "\n";
                fuelMsg += "\n";
            }
            controller.showMessage(fuelMsg);
        }
    }

    class SubAliveListener implements SubscriptionAliveListener {
        public void onAlive(Subscription s) {
        }

        public void onTimeout(Subscription s) {
        }
    }

    class MessageListener extends L2SubscriptionListener {
         @Override
        public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
            if (monitoredTagsReady) {
                String fromElement = monitoredDataItem.toString();
                controller.info("From L2Zones " + "Messages" + ":fromElement-" + fromElement + ", VALUE: " + dataValue.getValue().toStringWithType());
                Tag theTag = monitoredTags.get(monitoredDataItem);
                if (theTag.element == L2ParamGroup.Parameter.InfoMsg) {
                    if (l2InfoMessages.isNewData(theTag)) {  // the data will be already read if new data
                        String msg = l2InfoMessages.processValue(Tag.TagName.Msg).stringValue;
                        controller.showMessage(msg);
                        l2InfoMessages.setAsNoted();
                    }
                }
                if (theTag.element == L2ParamGroup.Parameter.ErrMsg) {
                    if (l2ErrorMessages.isNewData(theTag)) {  // the data will be already read if new data
                        String msg = l2ErrorMessages.processValue(Tag.TagName.Msg).stringValue;
                        controller.showError(msg);
                        l2ErrorMessages.setAsNoted();
                    }
                }
            }
        }
    }

    class StripListener extends L2SubscriptionListener {
        @Override
        public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
            Tag theTag = monitoredTags.get(monitoredDataItem);
            if (l2StripParams.isNewData(theTag))   // the data will be already read if new data
                handleNewStrip();
        }
    }

}
