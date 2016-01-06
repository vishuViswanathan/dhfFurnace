package level2;

import FceElements.heatExchanger.HeatExchProps;
import TMopcUa.ProcessValue;
import TMopcUa.TMSubscription;
import TMopcUa.TMuaClient;
import basic.Charge;
import basic.Fuel;
import basic.FuelFiring;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionAliveListener;
import directFiredHeating.*;
import level2.common.*;
import level2.fieldResults.FieldResults;
import mvUtils.display.DataWithMsg;
import mvUtils.display.ErrorStatAndMsg;
import mvUtils.display.InputControl;
import mvUtils.display.StatusWithMessage;
import mvUtils.math.BooleanWithStatus;
import mvUtils.math.DoubleWithStatus;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.math.DoubleMV;
import org.opcfoundation.ua.builtintypes.DataValue;
import performance.stripFce.Performance;

import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 05-Jan-15
 * Time: 10:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class L2DFHFurnace extends DFHFurnace implements ResultsReadyListener, L2Interface {
    public TMuaClient source;
    public FurnaceSettings furnaceSettings;
    LinkedHashMap<FceSection, L2Zone> topL2Zones;
    LinkedHashMap<FceSection, L2Zone> botL2Zones;
    ReadyNotedParam l2InfoMessages;
    ReadyNotedParam l2ErrorMessages;
    ReadyNotedParam l2StripSizeNow;
    ReadyNotedParam l2StripSizeNext;
    ReadyNotedParam fieldDataParams;
    ReadyNotedParam fuelCharParams;
    ReadyNotedParam l2YesNoQuery;
    ReadyNotedParam l2DataQuery;
    Vector<ReadyNotedParam> readyNotedParamList = new Vector<ReadyNotedParam>();
    GetLevelResponse yesNoResponse;
    GetLevelResponse dataResponse;
    L2Zone basicZone;
    L2Zone stripZone;  // all strip data size, speed temperature etc.
    L2Zone recuperatorZ;
    L2Zone commonDFHZ;
    public String equipment;
    public boolean level2Enabled = false;
    Tag tagLevel2Enabled;
    TMSubscription basicSub;
    TMSubscription messageSub;
    TMSubscription stripSub;
    TMSubscription fieldDataSub;
    TMSubscription fuelCharSub;
    boolean messagesON = false;
    Hashtable<MonitoredDataItem, Tag> monitoredTags;
    boolean monitoredTagsReady = false;
    public L2DFHeating l2DFHeating;
    double exitTempAllowance = 5;
    boolean calculatedForFieldResults = false;
    boolean reCalculateWithFieldCorrections = false;
    public boolean basicsSet = false;

    public L2DFHFurnace(L2DFHeating l2DFHEating, boolean bTopBot, boolean bAddTopSoak, ActionListener listener) {
        super(l2DFHEating, bTopBot, bAddTopSoak, listener);
        this.l2DFHeating = l2DFHEating;
        source = l2DFHEating.uaClient;
        source.removeAllSubscriptions();
        this.equipment = l2DFHEating.equipment;
        monitoredTags = new Hashtable<MonitoredDataItem, Tag>();
        topL2Zones = new LinkedHashMap<FceSection, L2Zone>();
        messagesON = createL2Messages();
        if (createBasicZone())
            if (createStripParam())
                if (createRecuParam())
                    if (createCommonDFHZ())
                        basicsSet = true;
    }

    public boolean showEditFceSettings(boolean bEdit) {
        return furnaceSettings.showEditData(bEdit);
    }

    boolean createBasicZone() {
        boolean retVal = false;
        basicSub = source.createTMSubscription("Base data",new SubAliveListener(), new BasicListener());
        try {
            basicSub.setMaxKeepAliveCount((long)5);
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        basicZone = new L2Zone(this, "Basic", basicSub);
        tagLevel2Enabled = new Tag(L2ParamGroup.Parameter.L2Stat, Tag.TagName.Enabled, false, true);
        Tag[] basicTags = {tagLevel2Enabled,
                new Tag(L2ParamGroup.Parameter.L2Stat, Tag.TagName.Ready, true, false)};
        try {
            basicZone.addOneParameter(L2ParamGroup.Parameter.L2Stat, basicTags);
            noteMonitoredTags(basicTags);
            retVal = true;
        } catch (TagCreationException e) {
            showError("Some problem in accessing Level2Stat :" + e.getMessage());
        }
        return retVal;
    }

    boolean createStripParam() {
        boolean retVal = false;
        String location = "";
        blk:
        {
            stripSub = source.createTMSubscription("Strip Data", new SubAliveListener(), new StripListener());
            stripZone = new L2Zone(this, "Strip", stripSub);
            try {
                Tag[] stripDataNowTags = {
                        new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Process, false, false),
                        new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Ready, false, true),
                        new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Thick, false, false),
                        new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Width, false, false),
                        new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Noted, false, true),

                        new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Ready, true, false),
                        new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.SpeedMax, true, false),
                        new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.SpeedNow, true, false),
                        new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Temperature, true, false),
                        new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Noted, true, false)};
                location = "Strip size Now tags";   // for error messages
                l2StripSizeNow = new ReadyNotedParam(source, equipment, "Strip.Now", stripDataNowTags, stripSub);
                readyNotedParamList.add(l2StripSizeNow);
                stripZone.addOneParameter(L2ParamGroup.Parameter.Now, l2StripSizeNow);
//                stripZone.addOneParameter(L2ParamGroup.Parameter.Next, l2StripSizeNow);
                noteMonitoredTags(stripDataNowTags);

                Tag[] stripDataNextTags = {
                        new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Process, false, false),
                        new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Ready, false, true),
                        new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Thick, false, false),
                        new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Width, false, false),
                        new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Noted, false, true),

                        new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Ready, true, false),
                        new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.SpeedMax, true, false),
                        new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.SpeedNow, true, false),
                        new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Temperature, true, false),
                        new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Noted, true, false)};
                location = "Strip size Next tags";
                l2StripSizeNext = new ReadyNotedParam(source, equipment, "Strip.Next", stripDataNextTags, stripSub);
                readyNotedParamList.add(l2StripSizeNext);
                stripZone.addOneParameter(L2ParamGroup.Parameter.Next, l2StripSizeNext);
//                stripZone.addOneParameter(L2ParamGroup.Parameter.Now, l2StripSizeNext);
                noteMonitoredTags(stripDataNextTags);

                Tag[] stripTemperatureTags = {
                        new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.SP, false, false),
                        new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.PV, false, false),
                        new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.CV, false, false),
                        new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.Mode, false, false) };
                location = "Strip Temperature tags";
                stripZone.addOneParameter(L2ParamGroup.Parameter.Temperature, stripTemperatureTags);
                noteMonitoredTags(stripTemperatureTags);
                Tag[] stripSpeedTags = {
                        new Tag(L2ParamGroup.Parameter.Speed, Tag.TagName.SP, false, false),
                        new Tag(L2ParamGroup.Parameter.Speed, Tag.TagName.PV, false, false)};
                location = "Strip Speed tags";
                stripZone.addOneParameter(L2ParamGroup.Parameter.Speed, stripSpeedTags);
                noteMonitoredTags(stripSpeedTags);
                Tag[] stripStatusTags = {
                        new Tag(L2ParamGroup.Parameter.Status, Tag.TagName.Length, false, false)};
                location = "Strip Status tags";
                stripZone.addOneParameter(L2ParamGroup.Parameter.Status, stripStatusTags);
                noteMonitoredTags(stripStatusTags);
                retVal = true;
            } catch (TagCreationException e) {
                showError("Problem in connecting to Strip Data at " + location + ": " + e.getMessage());
                break blk;
            }
        }
        return retVal;
    }

    boolean createCommonDFHZ() {
        boolean retVal = false;
        fuelCharSub = source.createTMSubscription("DFH Common data", new SubAliveListener(), new FuelCharListener());
        fieldDataSub = source.createTMSubscription("Field Data", new SubAliveListener(), new FieldDataListener());
        commonDFHZ = new L2Zone(this, "DFHCommon", fieldDataSub);
        try {
            Tag[] commonDFHTags1 = {
                    new Tag(L2ParamGroup.Parameter.L2Data, Tag.TagName.Noted, false, true),
                    new Tag(L2ParamGroup.Parameter.L2Data, Tag.TagName.Ready, true, false)};
            fuelCharParams = new ReadyNotedParam(source, equipment, "DFHCommon.L2Data", commonDFHTags1, fuelCharSub);
            readyNotedParamList.add(fuelCharParams);
            commonDFHZ.addOneParameter(L2ParamGroup.Parameter.L2Data, fuelCharParams);
            noteMonitoredTags(commonDFHTags1);
            Tag[] commonDFHTags2 = {
                    new Tag(L2ParamGroup.Parameter.Flue, Tag.TagName.Temperature, false, false)};
            commonDFHZ.addOneParameter(L2ParamGroup.Parameter.Flue, commonDFHTags2);
            noteMonitoredTags(commonDFHTags2);
            Tag[] commonDFHTags3 = {
                    new Tag(L2ParamGroup.Parameter.FieldData, Tag.TagName.Noted, true, false),  // noted by Level2
                    new Tag(L2ParamGroup.Parameter.FieldData, Tag.TagName.Ready, false, true)};  // is monitored
            fieldDataParams = new ReadyNotedParam(source, equipment, "DFHCommon.FieldData", commonDFHTags3, fieldDataSub);
            readyNotedParamList.add(fieldDataParams);
            commonDFHZ.addOneParameter(L2ParamGroup.Parameter.FieldData, fieldDataParams);
            noteMonitoredTags(commonDFHTags3);
            retVal = true;
        } catch (TagCreationException e) {
            showError("CommonDFH connection to Level1 :" + e.getMessage());
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
            retVal = true;
        } catch (TagCreationException e) {
            showError("Recuperator connection to Level1 :" + e.getMessage());
        }
        return retVal;
    }

    boolean createL2Messages() {
        boolean retVal = false;
        messageSub = source.createTMSubscription("Messages", new SubAliveListener(), new MessageListener());
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
        Tag[] yesNoQueryTags = {new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Msg, false, false),
                new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Ready, false, true),
                new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Msg, true, false),
                new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Ready, true, false),
                new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Noted, true, false),  // sending 'Noted' to Process
                new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Noted, false, true),  // reading 'Noted' from Process
                new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Response, true, false),
                new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Response, false, true)
        };
        Tag[] dataQueryTags = {new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Msg, false, false),
                new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Ready, false, true),
                new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Msg, true, false),
                new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Ready, true, false),
                new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Noted, true, false),  // sending 'Noted' to Process
                new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Noted, false, true),  // reading 'Noted' from Process
                new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Data, true, false),
                new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Data, false, true)
        };
        try {
            l2InfoMessages = new ReadyNotedParam(source, equipment, "Messages.InfoMsg", infoMessageTags, messageSub);
            readyNotedParamList.add(l2InfoMessages);
            l2ErrorMessages = new ReadyNotedParam(source, equipment, "Messages.ErrorMsg", errMessageTags, messageSub);
            readyNotedParamList.add(l2ErrorMessages);
            l2YesNoQuery = new ReadyNotedParam(source, equipment, "Messages.YesNoQuery", yesNoQueryTags, messageSub);
            readyNotedParamList.add(l2YesNoQuery);
            yesNoResponse = new GetLevelResponse(l2YesNoQuery);
            l2DataQuery = new ReadyNotedParam(source, equipment, "Messages.DataQuery", dataQueryTags, messageSub);
            readyNotedParamList.add(l2DataQuery);
            dataResponse = new GetLevelResponse(l2DataQuery);
            noteMonitoredTags(errMessageTags);
            noteMonitoredTags(infoMessageTags);
            noteMonitoredTags(yesNoQueryTags);
            noteMonitoredTags(dataQueryTags);
            monitoredTagsReady = true;
            messagesON = true;
            retVal = true;
        } catch (TagCreationException e) {
            showError("Message connection to Level1 :" + e.getMessage());
        }
        return retVal;
    }

    public void initForLevel2Operation() {
        for (ReadyNotedParam p: readyNotedParamList)
            p.initStatus();
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
        } catch (TagCreationException e) {
            e.setEquipment(equipment, "Creating L2 Zones");
            showError("Error in " + zoneName + " - " + e.getMessage());
            retVal = false;
        }
        return retVal;
    }

    ErrorStatAndMsg checkConnection() {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg(false, "Error, connecting to OPC :");
        noteConnectionsCheckStat(basicZone, retVal);
        noteConnectionsCheckStat(commonDFHZ, retVal);
        noteConnectionsCheckStat(l2ErrorMessages, retVal);
        noteConnectionsCheckStat(l2InfoMessages, retVal);
        noteConnectionsCheckStat(stripZone, retVal);
        noteConnectionsCheckStat(recuperatorZ, retVal);
        for (L2Zone z: topL2Zones.values())
            noteConnectionsCheckStat(z, retVal);
        return retVal;
    }

    boolean noteConnectionsCheckStat(L2Zone oneZ, ErrorStatAndMsg stat) {
        ErrorStatAndMsg oneSecStat;
        oneSecStat = oneZ.checkConnections();
        if (oneSecStat.inError) {
            stat.inError = true;
            stat.msg += "\n" + oneSecStat.msg;
        }
        return stat.inError;
    }

    boolean noteConnectionsCheckStat(L2ZoneParam p, ErrorStatAndMsg stat) {
        ErrorStatAndMsg oneSecStat;
        oneSecStat = p.checkConnections();
        if (oneSecStat.inError) {
            stat.inError = true;
            stat.msg += "\n" + oneSecStat.msg;
        }
        return stat.inError;
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

    public ErrorStatAndMsg takeFieldResultsFromLevel1() {
        oneFieldResults = new FieldResults(this, true);
        if (oneFieldResults.inError)
            return new ErrorStatAndMsg(true, oneFieldResults.errMsg);
        else {
            info("Strip Size = "  + oneFieldResults.production.charge.length);
            return oneFieldResults.processOkForFieldResults();
        }
    }

    public HeatExchProps getAirHeatExchProps() {
        return airRecu.getHeatExchProps(recuCounterFlow.isSelected());
    }

    /**
     * Evaluating charge exit conditions for the existing furnace temperautures
     * @return
     */
    public ChargeStatus processInFurnace(ChargeStatus chargeStatus) {
        if (controller.heatingMode == DFHeating.HeatingMode.TOPBOTSTRIP) {
            setOnlyWallRadiation();
            setDisplayResults(false);
            chargeStatus.setProductionData(production);
            getReadyToCalcul(true); // do not create new slots if already created
            setEntrySlotChargeTemperature(production.entryTemp);
            setProductionBasedSlotParams();
            if (doTheCalculationWithOnlyWallRadiation()) {
                chargeAtExit(chargeStatus, false);
                chargeStatus.setStatus(true);
            }
            else
                chargeStatus.setStatus(false);
        }
        else
            showError("Not ready for processInFurnace for other than Top Bottom Strip Heating");
        return chargeStatus;
    }

    void setEntrySlotChargeTemperature(double temp) {
        vTopUnitFces.get(0).setChargeTemperature(temp);
        if (bTopBot)
            vBotUnitFces.get(0).setChargeTemperature(temp);
    }

    double getSpeedWithFurnaceFuelStatus(L2ZonalFuelProfile zFP) {
        double totFuel = oneFieldResults.totFuel(); // TODO oneFieldResults already read
        return zFP.recommendedSpeed(totFuel, false);
    }

    double getOutputWithFurnaceTemperatureStatus(ChargeStatus chStatus, double exitTempRequired)  {
        double outputAssumed = 0;
        tuningParams.setSelectedProc(controller.proc);
        ErrorStatAndMsg response = takeFieldResultsFromLevel1();
        if (response.inError) {
            info("Facing some problem in reading furnace field data");
        }
        else {
            this.calculStep = controller.calculStep;
            setProduction(oneFieldResults.production);
//            getReadyToCalcul(controller.calculStep, true);
//            if ((vTopUnitFces != null) || prepareSlots()) {
            if (prepareSlots()) {
                prepareSlotsWithTempO();
                double tempAllowance = 2;
                outputAssumed = production.production;    // chargeAtExit(false).output;
                boolean done = false;
                double nowExitTemp, diff;
//                boolean firstTime = true;
                int trials = 0;
                while (!done) {
                    trials++;
                    chStatus.output = outputAssumed;
                    processInFurnace(chStatus);
                    if (chStatus.isValid()) {
                        nowExitTemp = chStatus.tempWM;
//                        if (firstTime) {
//                            info("First pass in getOutputWithFurnaceStatus for " +
//                                    production.chargeAndSize() + " at " + outputAssumed + "kg/h, exit Temp = " + nowExitTemp);
//                            String chTemp = "";
//                            firstTime = false;
//                        }
                        diff = exitTempRequired - nowExitTemp;
                        if (Math.abs(diff) < tempAllowance)
                            done = true;
                        else
                            outputAssumed = outputAssumed * (nowExitTemp - chTempIN) / (exitTempRequired - chTempIN);
                    }
                    else {
                        showError("processInfurnace returned with error!");
                        outputAssumed = 0;
                        break;
                    }
                }
                info("Trials in getOutputWithFurnaceStatus = " + trials);
            } else
                info("Facing problem in creating calculation steps");
        }
        return outputAssumed;
    }

    void prepareSlotsWithTempO() {
        oneFieldResults.copyTempAtTCtoSection();
        setTempOForAllSlots();
    }
    ProcessValue setRecommendedStripTemperature(double temp) {
        return stripZone.setValue(L2ParamGroup.Parameter.Next, Tag.TagName.Temperature, (float)temp);
    }

    ProcessValue setRecommendedStripSpeed(double speed) {
        return stripZone.setValue(L2ParamGroup.Parameter.Next, Tag.TagName.SpeedNow, (float)speed);
    }

    void handleNewStrip() {
        if (level2Enabled) {
//            ProcessValue pwW = stripZone.getValue(L2ParamGroup.Parameter.Next, Tag.TagName.Width);
//            if (!pwW.valid)
//                l2DFHeating.showError("Problem reading strip width: " + pwW.errorMessage) ;
            double stripWidth = DoubleMV.round(stripZone.getValue(L2ParamGroup.Parameter.Next, Tag.TagName.Width).floatValue, 3) / 1000;
            double stripThick = DoubleMV.round(stripZone.getValue(L2ParamGroup.Parameter.Next, Tag.TagName.Thick).floatValue, 3) / 1000;
            String process = stripZone.getValue(L2ParamGroup.Parameter.Next, Tag.TagName.Process).stringValue;
            String msg = "New Strip " + stripWidth + " x " + stripThick + " for process " + process;
            info(msg);
            if (level2Enabled) {
                stripZone.setValue(L2ParamGroup.Parameter.Next, Tag.TagName.Noted, true);
                OneStripDFHProcess oneProcess = l2DFHeating.getStripDFHProcess(process);
                if (oneProcess != null) {
                    BooleanWithStatus sizeCheck = oneProcess.checkStripSize(stripWidth, stripThick);
                    if (!(sizeCheck.getDataStatus() == StatusWithMessage.DataStat.WithErrorMsg)) {
                        Performance refP = getBasePerformance(process, stripThick);
                        if (refP != null) {
                            Charge ch = new Charge(controller.getSelChMaterial(refP.chMaterial), stripWidth, 1, stripThick);
                            ChargeStatus chStatus = new ChargeStatus(ch, 0, refP.exitTemp());
                            double output = getOutputWithFurnaceTemperatureStatus(chStatus, refP.exitTemp());
                            info("capacity Based On  temperature= " + (output / 1000) + "t/h");
                            DoubleWithStatus speedData = oneProcess.getRecommendedSpeed(output, stripWidth, stripThick);
                            StatusWithMessage.DataStat speedStatus = speedData.getDataStatus();
                            if (speedStatus != StatusWithMessage.DataStat.WithErrorMsg) {
                                ProcessValue responseSpeed = setRecommendedStripSpeed(speedData.getValue() / 60);
                                if (responseSpeed.valid) {
                                    ProcessValue responseTempSp = setRecommendedStripTemperature(oneProcess.tempDFHExit);
                                    if (responseTempSp.valid) {
                                        stripZone.setValue(L2ParamGroup.Parameter.Next, Tag.TagName.Ready, true);
                                        L2ZonalFuelProfile zFP = new L2ZonalFuelProfile(refP.getPerformanceTable(),
                                                furnaceSettings.fuelCharSteps, l2DFHeating);
                                        if (zFP.prepareFuelTable(stripWidth, stripThick)) {
                                            double speedBasedOnFuel = getSpeedWithFurnaceFuelStatus(zFP);
                                            info("capacityBasedOnFuel = " + speedBasedOnFuel + "m/min");
                                            if (sendFuelCharacteristics(zFP)) {
                                                if (speedStatus == StatusWithMessage.DataStat.WithInfoMsg)
                                                    showInfoInLevel1(speedData.getInfoMessage());
                                                commonDFHZ.setValue(L2ParamGroup.Parameter.L2Data, Tag.TagName.Ready, true);
                                            }
                                            else
                                                showErrorInLevel1("Unable to send Fuel Characteristics");
                                        }
                                        else
                                            showErrorInLevel1("Unable to prepare Fuel Characteristics");
                                    }
                                    else
                                        showErrorInLevel1("Unable to set Strip Exit Temperature");
                                }
                                else
                                    showErrorInLevel1("Unable to Set recommended speed: " + responseSpeed.errorMessage);
                            }
                            else
                                showErrorInLevel1("Unable to get recommended speed: " + speedData.getErrorMessage());
                        } else
                            showErrorInLevel1("Unable to get reference performance data");
                    }
                    else
                        showErrorInLevel1("Strip size is not in range : " + sizeCheck.getErrorMessage());
                }
                else
                    showErrorInLevel1("Process " + process + " is not in record");
            }
            else
                l2DFHeating.showMessage("Level2 has been disabled before the data could be sent back!");
        }
        else
            l2DFHeating.showMessage("Level2 is not enabled from Level1");
    }

    boolean sendFuelCharacteristics(L2ZonalFuelProfile zFP) {
        boolean retVal = true;
        int z = 0;

        for (L2Zone oneZone: topL2Zones.values())
            if (!oneZone.setFuelCharacteristic(zFP)) {
                showError("Some problem in setting fuel Characteristic for " + oneZone.groupName);
                retVal = false;
                break;
            }
//        if (retVal)
//            commonDFHZ.setValue(L2ParamGroup.Parameter.L2Data, Tag.TagName.Ready, true);
        return retVal;
    }


    void handleFieldData()  {
        if (level2Enabled) {
            if (fieldDataIsBeingHandled)
                showErrorInLevel1("Last field data is still being handled. Retry later");
            else {
                fieldDataIsBeingHandled = true;
                ErrorStatAndMsg stat = l2DFHeating.takeResultsFromLevel1();
                if (stat.inError) {
                    showErrorInLevel1(stat.msg);
                } else {
                    showInfoInLevel1("Evaluating from Model");
                    commonDFHZ.setValue(L2ParamGroup.Parameter.FieldData, Tag.TagName.Noted, true);
                    calculatedForFieldResults = l2DFHeating.evalForFieldProduction(this);
                }
            }
        }
        else
            l2DFHeating.showMessage("Level2 is not enabled from Level1");
    }

    boolean fieldDataIsBeingHandled = false;

    public void noteResultsReady() {
//        controller.showMessage("results ready Noted by L2DFHFurnace");
        if (calculatedForFieldResults) {
            info("calculated for Field process ..............");
            calculatedForFieldResults = false;
            reCalculateWithFieldCorrections = true;
            l2DFHeating.recalculateWithFieldCorrections(this);
        } else if (reCalculateWithFieldCorrections) {
            info("recalculated with Field Corrections .......... ");
            reCalculateWithFieldCorrections = false;
            boolean response = getYesNoResponseFromLevel1(
                    String.format("%s, %tc",
                            "Do you want to save as reference ", Calendar.getInstance()), 50);
            boolean saveIt =  (response) && (l2YesNoQuery.getValue(Tag.TagName.Response).booleanValue);
//            l2DFHeating.showMessage("The response was " +
//                    ((response) ? ("" + l2YesNoQuery.getValue(Tag.TagName.Response).booleanValue) : " No Response"));
            if (saveIt)
                addToPerfBase();
            fieldDataIsBeingHandled = false;
        }
    }

    public boolean showErrorInLevel1(String msg) {
        info(msg);
        l2ErrorMessages.setValue(Tag.TagName.Msg, msg);
        return l2ErrorMessages.markReady(true);
    }

    public boolean showInfoInLevel1(String msg) {
        l2InfoMessages.setValue(Tag.TagName.Msg, msg);
        return l2InfoMessages.markReady(true);
    }

    public boolean getYesNoResponseFromLevel1(String msg, int waitSeconds) {
        return yesNoResponse.getResponse(msg, waitSeconds);
    }

    public TMuaClient source() {
        return source;
    }

    public InputControl controller() {
        return controller;
    }

    public String equipment() {
        return equipment;
    }

    public void info(String msg) {
        controller.info(msg);
    }

    public void error(String msg) {
        showError(msg);
    }

    class SubAliveListener implements SubscriptionAliveListener {     // TODO This is common dummy class used everywhere to be made proper
        public void onAlive(Subscription s) {
//            l2DFHeating.showMessage("" + s + String.format(
//                    "%tc Subscription alive: ID=%d lastAlive=%tc",
//                    Calendar.getInstance(), s.getSubscriptionId().getValue(),
//                    s.getLastAlive()));
        }

        public void onTimeout(Subscription s) {
            l2DFHeating.showMessage(String.format("%s timeout at %tc, last alive at %tc ",
                    s, Calendar.getInstance(), s.getLastAlive()));
        }
    }

    class MessageListener extends L2SubscriptionListener {
         @Override
        public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
            if (monitoredTagsReady) {
                String fromElement = monitoredDataItem.toString();
//                controller.info("From L2Zones " + "Messages" + ":fromElement-" + fromElement + ", VALUE: " + dataValue.getValue().toStringWithType());
                Tag theTag = monitoredTags.get(monitoredDataItem);
                if (theTag.element == L2ParamGroup.Parameter.InfoMsg) {
                    if (l2InfoMessages.isNewData(theTag)) {  // the data will be already read if new data
                        String msg = l2InfoMessages.processValue(Tag.TagName.Msg).stringValue;
                        controller.showMessage(msg);
                        l2InfoMessages.setAsNoted(true);
                    }
                }
                if (theTag.element == L2ParamGroup.Parameter.ErrMsg) {
                    if (l2ErrorMessages.isNewData(theTag)) {  // the data will be already read if new data
                        String msg = l2ErrorMessages.processValue(Tag.TagName.Msg).stringValue;
                        controller.showError(msg);
                        l2ErrorMessages.setAsNoted(true);
                    }
                }
            }
        }
    }

    class StripListener extends L2SubscriptionListener {
        @Override
        public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
            if (l2DFHeating.isL2SystemReady()) {
                Tag theTag = monitoredTags.get(monitoredDataItem);
                if (l2StripSizeNext.isNewData(theTag))   // the data will be already read if new data
                    handleNewStrip();
            }
        }
    }

    class FieldDataListener extends L2SubscriptionListener {
         @Override
         public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
             if (l2DFHeating.isL2SystemReady()) {
                 Tag theTag = monitoredTags.get(monitoredDataItem);
                 if (fieldDataParams.isNewData(theTag))   // the data will be already read if new data
                     handleFieldData();
             }
         }
    }

    class FuelCharListener extends L2SubscriptionListener {
         @Override
         public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
             if (l2DFHeating.isL2SystemReady()) {
                 Tag theTag = monitoredTags.get(monitoredDataItem);
                 fuelCharParams.isNewData(theTag);
             }
         }
    }

    class BasicListener extends L2SubscriptionListener {
           @Override
           public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
               if (l2DFHeating.isL2SystemReady()) {
                   Tag theTag = monitoredTags.get(monitoredDataItem);
                   level2Enabled = theTag.getValue().booleanValue;
                   informLevel2Ready();
                   if (!level2Enabled)
                       l2DFHeating.showMessage("Level2 has been disabled!");

               }
           }
    }

    public void informLevel2Ready() {
        basicZone.setValue(L2ParamGroup.Parameter.L2Stat, Tag.TagName.Ready, tagLevel2Enabled.getValue().booleanValue);
    }
}
