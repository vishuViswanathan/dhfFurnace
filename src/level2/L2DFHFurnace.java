package level2;

import FceElements.heatExchanger.HeatExchProps;
import TMopcUa.TMuaClient;
import basic.Fuel;
import basic.FuelFiring;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionAliveListener;
import directFiredHeating.DFHFurnace;
import directFiredHeating.FceSection;
import level2.fieldResults.FieldResults;
import level2.listeners.L2SubscriptionListener;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.math.DoubleMV;
import org.opcfoundation.ua.builtintypes.DataValue;
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
    LinkedHashMap<FceSection, L2Zone> topL2Zones;
    LinkedHashMap<FceSection, L2Zone> botL2Zones;
    ReadyNotedParam l2InfoMessages;
    ReadyNotedParam l2ErrorMessages;
    ReadyNotedParam l2StripSizeParams;
    L2Zone stripZone;  // all strip data size, speed temperature etc.
    L2Zone recuperatorZ;
    L2Zone commonDFHZ;
    String equipment;
    Subscription messageSub;
    Subscription stripSub;
    boolean messagesON = false;
    Hashtable<MonitoredDataItem, Tag> monitoredTags;
    boolean monitoredTagsReady = false;
    public L2DFHeating l2DFHeating;
    double exitTempAllowance = 5;

    public L2DFHFurnace(L2DFHeating l2DFHEating, boolean bTopBot, boolean bAddTopSoak, ActionListener listener) {
        super(l2DFHEating, bTopBot, bAddTopSoak, listener);
        this.l2DFHeating = l2DFHEating;
        source = l2DFHEating.uaClient;
        source.removeAllSubscriptions();
        this.equipment = l2DFHEating.equipment;
        monitoredTags = new Hashtable<MonitoredDataItem, Tag>();
        topL2Zones = new LinkedHashMap<FceSection, L2Zone>();
        messagesON = createL2Messages();
        createStripParam();
        createRecuParam();
        createCommonDFHZ();
    }

    boolean createStripParamOLD() { // TODO to be removed subsequently
        boolean retVal = false;
        stripSub = source.createSubscription(new SubAliveListener(), new StripListener());
        Tag[] stripTags = {new Tag(L2ParamGroup.Parameter.Data, Tag.TagName.Process, false, false),
                new Tag(L2ParamGroup.Parameter.Data, Tag.TagName.Ready, false, true),
                new Tag(L2ParamGroup.Parameter.Data, Tag.TagName.Thick, false, false),
                new Tag(L2ParamGroup.Parameter.Data, Tag.TagName.Width, false, false),
                new Tag(L2ParamGroup.Parameter.Data, Tag.TagName.Noted, true, false)};
        try {
            l2StripSizeParams = new ReadyNotedParam(source, equipment, "Strip.Data", stripTags, stripSub);
            noteMonitoredTags(stripTags);
        } catch (TagCreationException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean showEditFceSettings(boolean bEdit) {
        return furnaceSettings.showEditData(bEdit);
    }

    boolean createStripParam() {
        boolean retVal = false;
        String location = "";
        blk:
        {
            stripSub = source.createSubscription(new SubAliveListener(), new StripListener());
            stripZone = new L2Zone(this, "Strip", stripSub);
            try {
                Tag[] stripDataTags = {
                        new Tag(L2ParamGroup.Parameter.Data, Tag.TagName.Process, false, false),
                        new Tag(L2ParamGroup.Parameter.Data, Tag.TagName.Ready, false, true),
                        new Tag(L2ParamGroup.Parameter.Data, Tag.TagName.Thick, false, false),
                        new Tag(L2ParamGroup.Parameter.Data, Tag.TagName.Width, false, false),
                        new Tag(L2ParamGroup.Parameter.Data, Tag.TagName.Noted, true, false)};
                location = "Strip size tags";
                l2StripSizeParams = new ReadyNotedParam(source, equipment, "Strip.Data", stripDataTags, stripSub);
                stripZone.addOneParameter(L2ParamGroup.Parameter.Data, l2StripSizeParams);
                noteMonitoredTags(stripDataTags);
                Tag[] stripTemperatureTags = {
                        new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.SP, false, false),
                        new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.SP, true, false),   // SP from Level2
                        new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.PV, false, true),   // TODO strip Temperature is monitored
                        new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.CV, false, false),
                        new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.Mode, false, false) };
                location = "Strip Temperature tags";
                stripZone.addOneParameter(L2ParamGroup.Parameter.Temperature, stripTemperatureTags);
                noteMonitoredTags(stripTemperatureTags);
                Tag[] stripSpeedTags = {
                        new Tag(L2ParamGroup.Parameter.Speed, Tag.TagName.SP, false, false),
                        new Tag(L2ParamGroup.Parameter.Speed, Tag.TagName.SP, true, false),
                        new Tag(L2ParamGroup.Parameter.Speed, Tag.TagName.PV, false, true)}; // TODO strip speed is monitored
                location = "Strip Speed tags";
                stripZone.addOneParameter(L2ParamGroup.Parameter.Speed, stripSpeedTags);
                noteMonitoredTags(stripSpeedTags);
                Tag[] stripStatusTags = {
                        new Tag(L2ParamGroup.Parameter.Status, Tag.TagName.Length, false, true)};  // TODO balance length monitored
                location = "Strip Status tags";
                stripZone.addOneParameter(L2ParamGroup.Parameter.Status, stripStatusTags);
                noteMonitoredTags(stripStatusTags);
            } catch (TagCreationException e) {
                showError("Problem in connecting to Strip Data at " + location + ": " + e.getMessage());
                break blk;
            }

        }


        return retVal;
     }

    boolean createCommonDFHZ() {
        boolean retVal = false;
//        stripSub = source.createSubscription(new SubAliveListener(), new StripListener());
        commonDFHZ = new L2Zone(this, "DFHCommon", null);
        try {
            Tag[] commonDFHTags1 = {
                    new Tag(L2ParamGroup.Parameter.FuelCharacteristic, Tag.TagName.Noted, false, false)};
            commonDFHZ.addOneParameter(L2ParamGroup.Parameter.FuelCharacteristic, commonDFHTags1);
            noteMonitoredTags(commonDFHTags1);
            Tag[] commonDFHTags2 = {
                    new Tag(L2ParamGroup.Parameter.Flue, Tag.TagName.Temperature, false, false)};
            commonDFHZ.addOneParameter(L2ParamGroup.Parameter.Flue, commonDFHTags2);
            noteMonitoredTags(commonDFHTags2);
        } catch (TagCreationException e) {
            showError("CommonDFH connection to Level1 :" + e.getMessage());
            retVal = false;
        }
        return retVal;
    }

    boolean createRecuParam() {
        boolean retVal = false;
//        stripSub = source.createSubscription(new SubAliveListener(), new StripListener());
        recuperatorZ = new L2Zone(this, "Recuperator", null);
        try {
            Tag[] recuFlueTags = {
                    new Tag(L2ParamGroup.Parameter.Flue, Tag.TagName.Temperature, false, false)};
            recuperatorZ.addOneParameter(L2ParamGroup.Parameter.Flue, recuFlueTags);
            noteMonitoredTags(recuFlueTags);
            Tag[] recuAirTags = {
                    new Tag(L2ParamGroup.Parameter.AirFlow, Tag.TagName.Temperature, false, false)};
            recuperatorZ.addOneParameter(L2ParamGroup.Parameter.AirFlow, recuAirTags);
            noteMonitoredTags(recuAirTags);
        } catch (TagCreationException e) {
            showError("Recuperator connection to Level1 :" + e.getMessage());
            retVal = false;
        }
        return retVal;
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
            retVal = true;
        } catch (TagCreationException e) {
            showError("Message connection to Level1 :" + e.getMessage());
            retVal = false;
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
        topL2Zones.clear();
        int zNum = 1;
        String zoneName = "";
        try {
            for (FceSection sec : topSections) {
                if (sec.isActive()) {
                    zoneName = "DFHZone" + ("" + zNum).trim();
                    L2Zone oneZone = new L2Zone(this, zoneName, sec, true);
                    topL2Zones.put(sec, oneZone);
                    zNum++;
                } else
                    break;
            }
            furnaceSettings = new FurnaceSettings(l2DFHeating);
            furnaceSettings.setMaxSpeed(1000);
            double[] fuelRange = new double[topL2Zones.size()];
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
        for (FceSection sec : topL2Zones.keySet())
            topL2Zones.get(sec).closeSubscriptions();
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

    public L2Zone getStripZone() {
        return stripZone;
    }

    public L2Zone getRecuperatorZ() {
        return recuperatorZ;
    }

    public L2Zone getCommonDFHZ() {
        return commonDFHZ;
    }

    public L2Zone getOneL2Zone(int zNum, boolean bBot) {
        L2Zone theZone = null;
        if (bBot)
            showError("Not Ready for Bottom Zones in Level2");
        else {
            if (zNum < topL2Zones.size())
                theZone = topL2Zones.get(topSections.get(zNum));
        }
        return theZone;
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

    boolean getFieldDataFromUser() {
        return oneFieldResults.getDataFromUser();
    }

    public FceSection getOneSection(boolean bBot, int nSec) {
        return ((bBot) ? botSections : topSections).get(nSec);
    }

    FieldResults oneFieldResults;

    public boolean setFieldProductionData() {
        if (oneFieldResults != null) {
//            oneFieldResults.compareResults();
            l2DFHeating.setFieldProductionData(oneFieldResults.production, oneFieldResults.commonAirTemp,
                    oneFieldResults.commonFuelTemp);
            return true;
        }
        else
            return false;

    }

    public FuelFiring getFuelFiring(boolean bRegen, double excessAir, double airTemp, double fuelTemp)  {
        Fuel f = l2DFHeating.getSelFuel(); // TODO considers only one fuel
        if (f != null)
            return new FuelFiring(f, bRegen, excessAir, airTemp, fuelTemp);
        else
            return null;
    }

    public boolean adjustForFieldResults() {
        if (oneFieldResults != null) {
            return oneFieldResults.adjustForFieldResults();
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

    public boolean takeFieldResultsFromLevel1() {
        oneFieldResults = new FieldResults(this, true);
         return !oneFieldResults.inError;
    }

    public HeatExchProps getAirHeatExchProps() {
        return airRecu.getHeatExchProps(recuCounterFlow.isSelected());
    }

    void handleNewStrip() {
//        double stripWidth = DoubleMV.round(l2StripSizeParams.processValue(Tag.TagName.Width).floatValue, 3) / 1000;
//        double  stripThick = DoubleMV.round(l2StripSizeParams.processValue(Tag.TagName.Thick).floatValue, 3) / 1000;
//        String process = l2StripSizeParams.processValue(Tag.TagName.Process).stringValue;
//        String msg = "New Strip " + stripWidth + " x " + stripThick + " for process " + process;
//        controller.showMessage(msg);
//        l2StripSizeParams.setAsNoted();

        double stripWidth = DoubleMV.round(stripZone.getValue(L2ParamGroup.Parameter.Data, Tag.TagName.Width).floatValue, 3) / 1000;
        double  stripThick = DoubleMV.round(stripZone.getValue(L2ParamGroup.Parameter.Data, Tag.TagName.Thick).floatValue, 3) / 1000;
        String process = stripZone.getValue(L2ParamGroup.Parameter.Data, Tag.TagName.Process).stringValue;
        String msg = "New Strip " + stripWidth + " x " + stripThick + " for process " + process;
        controller.showMessage(msg);
        stripZone.setValue(L2ParamGroup.Parameter.Data, Tag.TagName.Noted, true);

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
            if (l2StripSizeParams.isNewData(theTag))   // the data will be already read if new data
                handleNewStrip();
        }
    }

}
