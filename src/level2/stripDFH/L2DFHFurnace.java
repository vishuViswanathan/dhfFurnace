package level2.stripDFH;

import FceElements.heatExchanger.HeatExchProps;
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
import directFiredHeating.accessControl.L2AccessControl;
import directFiredHeating.stripDFH.StripFurnace;
import level2.applications.L2DFHeating;
import level2.common.*;
import level2.display.L2DFHDisplay;
import level2.fieldResults.FieldResults;
import directFiredHeating.process.FurnaceSettings;
import directFiredHeating.process.OneStripDFHProcess;
import mvUtils.display.*;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import org.opcfoundation.ua.builtintypes.DataValue;
import performance.stripFce.Performance;
import performance.stripFce.StripProcessAndSize;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 05-Jan-15
 * Time: 10:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class L2DFHFurnace extends DFHFurnace implements L2Interface {
    public TMuaClient source;
//    public FurnaceSettings furnaceSettings;
    LinkedHashMap<FceSection, L2DFHZone> topL2Zones;
    LinkedHashMap<FceSection, L2DFHZone> botL2Zones;
    L2OneParameterZone rthExit;
    L2OneParameterZone soakZone;
    L2OneParameterZone hbExit;
    ReadyNotedParam l2InfoMessages;
    ReadyNotedParam l2ErrorMessages;
    ReadyNotedParam fieldDataParams;
    ReadyNotedParam fuelCharParams;
    ReadyNotedBothL2 performanceStat;
    ReadyNotedBothL2 processDataStat;
    ReadyNotedParam l2YesNoQuery;
    ReadyNotedParam l2DataQuery;
    Vector<ReadyNotedParam> readyNotedParamList = new Vector<ReadyNotedParam>();
    GetLevelResponse yesNoResponse;
    GetLevelResponse dataResponse;
    L2ParamGroup internalZone;
    L2ParamGroup basicZone;
    L2StripZone stripZone;  // all strip data size, speed temperature etc.
    L2ParamGroup recuperatorZ;
    L2ParamGroup commonDFHZ;
    Vector<L2DFHDisplay> furnaceDisplayZones;
    public String equipment;
    public boolean level2Enabled = false;
    Tag tagLevel2Enabled;
    Tag tagRuntimeReady;
    Tag tagUpdaterReady;
    Tag tagExpertReady;
    Tag tagFieldDataReady;
    TMSubscription basicSub;
    TMSubscription messageSub;
    TMSubscription fieldDataSub;
    TMSubscription fuelCharSub;
    TMSubscription updaterChangeSub;
    boolean messagesON = false;
    Hashtable<MonitoredDataItem, Tag> monitoredTags;
    boolean monitoredTagsReady = false;
    public L2DFHeating l2DFHeating;
    public boolean basicsSet = false;
    boolean itIsRuntime = false;

    long displayUpdateInterval = 1000; // 1 sec

    public L2DFHFurnace(L2DFHeating l2DFHEating, boolean bTopBot, boolean bAddTopSoak, ActionListener listener) {
        super(l2DFHEating, bTopBot, bAddTopSoak, listener);
        this.l2DFHeating = l2DFHEating;
    }

    public boolean basicConnectionToLevel1(TMuaClient level1Source) {
        source = level1Source;
        source.removeAllSubscriptions();
        this.equipment = l2DFHeating.equipment;
        monitoredTags = new Hashtable<MonitoredDataItem, Tag>();
        topL2Zones = new LinkedHashMap<FceSection, L2DFHZone>();
        messagesON = createL2Messages();
        itIsRuntime = (l2DFHeating.accessLevel == L2AccessControl.AccessLevel.RUNTIME);
        return createInternalZone();
    }

    public boolean makeAllConnections() {
        if (createBasicZone())
            if (createStripZone())
                if (createRecuParam())
                    if (createCommonDFHZ()) {
                        createL2Zones();
                        basicsSet = true;
                    }
//        enableSubscriptions(false);
        return basicsSet;
    }

    Thread displayThread;

    public void startL2DisplayUpdater() {
        l2DisplayON = true;
        L2DisplayUpdater displayUpdater = new L2DisplayUpdater();
        displayThread = new Thread(displayUpdater);
        displayThread.start();
        logTrace("l2 Display updater started ...");
    }

    void stopDisplayUpdater() {
        if (l2DisplayON) {
            l2DisplayON = false;
            try {
                if (displayThread != null) displayThread.join(displayUpdateInterval * 5);
            } catch (InterruptedException e) {
                logError("Problem in stopping Display Updater");
                e.printStackTrace();
            }
        }
    }

    Thread speedThread; // for updating recommended at regular intervals based on furnace

    public void startSpeedUpdater() {
        stripZone.startSpeedUpdater();
    }

    boolean canResetAccess = true; // resetting the application status in 'Level2.Internal'

    /**
     * Checks in the the module access level is valid or not
     * Baically checks if another module exists with this level
     * @return
     */
    public StatusWithMessage checkAndNoteAccessLevel() {
        StatusWithMessage retVal = new StatusWithMessage();
        if (l2DFHeating.isOnProductionLine()) {
            switch (l2DFHeating.accessLevel) {
                case EXPERT:
                    if (tagExpertReady.getValue().booleanValue)
                        retVal.setErrorMessage("Level2 with Expert Access is already ON");
                    else if (tagUpdaterReady.getValue().booleanValue)
                        retVal.setErrorMessage("Exit from Level2 Updater and try again as Expert");
                    else
                        tagExpertReady.setValue(true);
                    break;
                case UPDATER:
                    if (tagUpdaterReady.getValue().booleanValue)
                        retVal.setErrorMessage("Level2 with Updater Access is already ON");
                    else if (tagExpertReady.getValue().booleanValue)
                        retVal.setErrorMessage("Exit from Level2 Expert and try again as Updater");
                    else
                        tagUpdaterReady.setValue(true);
                    break;
                default:
                    if (tagRuntimeReady.getValue().booleanValue)
                        retVal.setErrorMessage("Level2-Runtime is already ON");
                    else
                        tagRuntimeReady.setValue(true);
                    break;
            }
            canResetAccess = (retVal.getDataStatus() == StatusWithMessage.DataStat.OK);
        }
        return retVal;
    }

    public void exitFromAccess() {
        if (canResetAccess) {
            switch (l2DFHeating.accessLevel) {
                case EXPERT:
                    tagExpertReady.setValue(false);
                    break;
                case UPDATER:
                    tagUpdaterReady.setValue(false);
                    break;
                default:
                    tagRuntimeReady.setValue(false);
                    break;
            }
        }
    }

    Tag performanceChanged, performanceNoted;
    Tag processDataChanged, processDataNoted;

    public boolean createInternalZone() {
        boolean retVal = false;
        internalZone = new L2ParamGroup(this, "Internal");
        updaterChangeSub = source.createTMSubscription("Updater Status",new SubAliveListener(), new UpdaterChangeListener());
        logDebug("updaterChangeSub created");
        Tag[] performanceTags = {
                performanceChanged = new Tag(L2ParamGroup.Parameter.Performance, Tag.TagName.Ready, true, itIsRuntime),
                performanceNoted = new Tag(L2ParamGroup.Parameter.Performance, Tag.TagName.Noted, true, !itIsRuntime)};
        try {
            performanceStat = new ReadyNotedBothL2(source, equipment, "Internal.Performance", performanceTags, updaterChangeSub);
            performanceStat.setReadWriteStat(!itIsRuntime);
        } catch (TagCreationException e) {
            e.printStackTrace();
        }
        readyNotedParamList.add(performanceStat);
        internalZone.addOneParameter(L2ParamGroup.Parameter.Performance, performanceStat);
        noteMonitoredTags(performanceTags);

        Tag[] processDataTags = {
                processDataChanged = new Tag(L2ParamGroup.Parameter.ProcessData, Tag.TagName.Ready, true, itIsRuntime),
                processDataNoted = new Tag(L2ParamGroup.Parameter.ProcessData, Tag.TagName.Noted, true, !itIsRuntime)};
        try {
            processDataStat = new ReadyNotedBothL2(source, equipment, "Internal.ProcessData", processDataTags, updaterChangeSub);
            processDataStat.setReadWriteStat(!itIsRuntime);
        } catch (TagCreationException e) {
            e.printStackTrace();
        }
        readyNotedParamList.add(processDataStat);
        internalZone.addOneParameter(L2ParamGroup.Parameter.ProcessData, processDataStat);
        noteMonitoredTags(processDataTags);

        tagRuntimeReady = new Tag(L2ParamGroup.Parameter.Runtime, Tag.TagName.Running, true, false);
        tagUpdaterReady = new Tag(L2ParamGroup.Parameter.Updater, Tag.TagName.Running, true, false);
        tagExpertReady = new Tag(L2ParamGroup.Parameter.Expert, Tag.TagName.Running, true, false);

        try {
            internalZone.addOneParameter(L2ParamGroup.Parameter.Runtime, tagRuntimeReady);
            internalZone.addOneParameter(L2ParamGroup.Parameter.Updater, tagUpdaterReady);
            internalZone.addOneParameter(L2ParamGroup.Parameter.Expert, tagExpertReady);
            logTrace("Internal zone created");
            retVal = true;
        } catch (TagCreationException e) {
            showError("Some problem in accessing Level2.Internal :" + e.getMessage());
        }
        return retVal;
    }

    boolean createBasicZone() {
        boolean retVal = false;
        basicSub = source.createTMSubscription("Base data",new SubAliveListener(), new BasicListener());
        try {
            basicSub.setMaxKeepAliveCount((long)5);
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        basicZone = new L2ParamGroup(this, "Basic", basicSub);
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

    boolean createStripZone() {
        boolean retVal = false;
        try {
            stripZone = new L2StripZone(l2DFHeating, "Strip", "Strip Data");
            retVal = true;
        } catch (TagCreationException e) {
            e.printStackTrace();
        }
        return retVal;
    }

    boolean createCommonDFHZ() {
        boolean retVal = false;
        fuelCharSub = source.createTMSubscription("DFH Common Data", new SubAliveListener(), new FuelCharListener());
        fieldDataSub = source.createTMSubscription("Field Data", new SubAliveListener(), new FieldDataListener());
        commonDFHZ = new L2ParamGroup(this, "DFHCommon", fieldDataSub);
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
                    (tagFieldDataReady = new Tag(L2ParamGroup.Parameter.FieldData, Tag.TagName.Ready, false, true))};  // is monitored
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
        recuperatorZ = new L2ParamGroup(this, "Recuperator");
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
        logDebug("creating L2 Massages");
        messageSub = source.createTMSubscription("Messages Data", new SubAliveListener(), new MessageListener());
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
            yesNoResponse = new GetLevelResponse(l2YesNoQuery, this);
            l2DataQuery = new ReadyNotedParam(source, equipment, "Messages.DataQuery", dataQueryTags, messageSub);
            readyNotedParamList.add(l2DataQuery);
            dataResponse = new GetLevelResponse(l2DataQuery, this);
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
        stripZone.initForLevel2Operation();
        level2Enabled = informLevel2Ready();
    }

    void noteMonitoredTags(Tag[] tags) {
        for (Tag tag : tags)
            if (tag.isMonitored())
                monitoredTags.put(tag.getMonitoredDataItem(), tag);
    }

    boolean createL2Zones() {
        boolean retVal = true;
        topL2Zones.clear();
        furnaceDisplayZones = new Vector<L2DFHDisplay>();
        int zNum = 1;
//        String zoneName = "";
        String descriptiveName = "";
        try {
            for (FceSection sec : topSections) {
                if (sec.isActive()) {
                    String zoneName = "DFHZone" + ("" + zNum).trim();
                    descriptiveName = "DFH Zone-" + ("" + zNum).trim();
                    L2DFHZone oneZone = new L2DFHZone(this, zoneName, descriptiveName, sec, true);
                    topL2Zones.put(sec, oneZone);
                    furnaceDisplayZones.add(new L2DFHDisplay(oneZone));
                    zNum++;
                } else
                    break;
            }
            rthExit = new L2OneParameterZone(this, "RTHExit", "RTH Strip Exit", L2ParamGroup.Parameter.Temperature, "#,###", true);
            soakZone = new L2OneParameterZone(this, "SoakZone", "Soak Zone", L2ParamGroup.Parameter.Temperature, "#,###", true);
            hbExit = new L2OneParameterZone(this, "HBExit", " HB Strip Exit", L2ParamGroup.Parameter.Temperature, "#,###", true);
//            updateUIDisplay();
        } catch (TagCreationException e) {
            e.setEquipment(equipment, "Creating L2 Zones");
            showError("Error in " + descriptiveName + " - " + e.getMessage());
            retVal = false;
        }
        return retVal;
    }

    void updateUIDisplay() {
        switch(l2DFHeating.l2DisplayNow) {
            case PROCESS:
                updateProcessDisplay();
                break;
            case LEVEL2:
                updateLevel2Display();
                break;
        }
    }

    void updateProcessDisplay()  {
        for (L2DFHDisplay z: furnaceDisplayZones)
            z.updateProcessDisplay();
        stripZone.updateProcessDisplay();
        rthExit.updateProcessDisplay();
        soakZone.updateProcessDisplay();
        hbExit.updateProcessDisplay();
    }

    void updateLevel2Display() {
        for (L2DFHDisplay z: furnaceDisplayZones)
            z.updateLevel2Display();
        stripZone.updateL2Display();
        rthExit.updateLevel2Display();
        soakZone.updateLevel2Display();
        hbExit.updateLevel2Display();

    }

    public JPanel getFurnaceProcessPanel() {
        JPanel outerP = new JPanel(new BorderLayout());
        outerP.add(dfhProcessDisplayPanel(), BorderLayout.NORTH);
        outerP.add(othersProcessDisplayPanel(), BorderLayout.CENTER);
        outerP.add(stripZone.stripProcessPanel(), BorderLayout.SOUTH);
        return outerP;
    }

    public JPanel getFurnaceLevel2Panel() {
        JPanel outerP = new JPanel(new BorderLayout());
        outerP.add(dfhLevel2DisplayPanel(), BorderLayout.NORTH);
        outerP.add(othersLevel2DisplayPanel(), BorderLayout.CENTER);
        outerP.add(stripZone.stripLevel2Panel(), BorderLayout.SOUTH);
        return outerP;
    }

    JPanel dfhProcessDisplayPanel() {
        FramedPanel innerP = new FramedPanel(new BorderLayout());
        FramedPanel jp = new FramedPanel();
        jp.add(L2DFHDisplay.getRowHeader());
        for (L2DFHDisplay z:furnaceDisplayZones) {
            jp.add(z.getProcessDisplay());
        }
        JPanel titleP = new JPanel();
        titleP.add(new JLabel("DFH Zones"));
        innerP.add(titleP, BorderLayout.NORTH);
        innerP.add(jp, BorderLayout.CENTER);
        return innerP;
    }

    JPanel dfhLevel2DisplayPanel() {
        FramedPanel innerP = new FramedPanel(new BorderLayout());
        FramedPanel jp = new FramedPanel();
        for (L2DFHDisplay z:furnaceDisplayZones) {
            jp.add(z.getLevel2Display());
        }
        innerP.add(jp);
        return innerP;
    }

    JPanel othersProcessDisplayPanel() {
        FramedPanel innerP = new FramedPanel(new BorderLayout());
        FramedPanel jp = new FramedPanel();
        jp.add(stripZone.nowStripProcessTempPanel);
        jp.add(rthExit.getProcessDisplay());
        jp.add(soakZone.getProcessDisplay());
        jp.add(hbExit.getProcessDisplay());
        JPanel titleP = new JPanel();
        titleP.add(new JLabel("Sections After DFH"));
        innerP.add(titleP, BorderLayout.NORTH);
        innerP.add(jp, BorderLayout.CENTER);
        return innerP;
    }

    JPanel othersLevel2DisplayPanel() {
        FramedPanel innerP = new FramedPanel(new BorderLayout());
        FramedPanel jp = new FramedPanel();
        jp.add(stripZone.nowStripLevel2TempPanel);
        jp.add(rthExit.getLevel2Display());
        jp.add(soakZone.getLevel2Display());
        jp.add(hbExit.getLevel2Display());
        JPanel titleP = new JPanel();
        titleP.add(new JLabel("Sections After DFH"));
        innerP.add(titleP, BorderLayout.NORTH);
        innerP.add(jp, BorderLayout.CENTER);
        return innerP;
    }
    public ErrorStatAndMsg checkConnection() {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg(false, "Error, connecting to OPC :");
        noteConnectionsCheckStat(basicZone, retVal);
        noteConnectionsCheckStat(commonDFHZ, retVal);
        noteConnectionsCheckStat(l2ErrorMessages, retVal);
        noteConnectionsCheckStat(l2InfoMessages, retVal);
        noteConnectionsCheckStat(stripZone, retVal);
        noteConnectionsCheckStat(recuperatorZ, retVal);
        for (L2DFHZone z: topL2Zones.values())
            noteConnectionsCheckStat(z, retVal);
        return retVal;
    }

    boolean noteConnectionsCheckStat(L2ParamGroup oneZ, ErrorStatAndMsg stat) {
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

    boolean l2DisplayON = false;
    boolean speedUpdaterOn = false;

    public boolean prepareForDisconnection() {
        stopDisplayUpdater();
        stripZone.stopSpeedUpdater();
        try {
            if (updaterChangeSub != null)
                updaterChangeSub.removeItems();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        exitFromAccess();
        if (l2DFHeating.isOnProductionLine() && l2DFHeating.isL2SystemReady()) {
//            exitFromAccess();
            for (FceSection sec : topL2Zones.keySet())
                topL2Zones.get(sec).closeSubscriptions();
            try {
                basicSub.removeItems();
                messageSub.removeItems();
                fuelCharSub.removeItems();
                fieldDataSub.removeItems();
                stripZone.prepareForDisconnection();
            } catch (ServiceException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public Performance getBasePerformance(OneStripDFHProcess stripDFHProc, double stripThick) {
        return performBase.getRefPerformance(stripDFHProc.processName, stripDFHProc.getChMaterial(stripDFHProc.processName, stripThick),
                l2DFHeating.getSelectedFuel().name, stripDFHProc.tempDFHExit);
    }

    public Performance getBasePerformance(String forProcess, double stripThick) {
        Performance prf = null;
        OneStripDFHProcess stripDFHProc = l2DFHeating.getStripDFHProcess(forProcess);
        if (stripDFHProc != null)
//            prf = performBase.getRefPerformance(stripDFHProc.getChMaterial(forProcess, stripThick),
//                    stripDFHProc.tempDFHExit, exitTempAllowance);
            prf = performBase.getRefPerformance(stripDFHProc.processName, stripDFHProc.getChMaterial(forProcess, stripThick),
                l2DFHeating.getSelectedFuel().name, stripDFHProc.tempDFHExit);
        else
            logInfo("strip process " + forProcess + " is not available in the database");
        return prf;
    }

    public boolean isRefPerformanceAvailable(OneStripDFHProcess dfhProc, double stripThick) {
        return (performBase != null) &&
                performBase.isRefPerformanceAvailable(dfhProc.processName,
                        dfhProc.getChMaterial(stripThick).name, dfhProc.tempDFHExit);
    }

    public L2ParamGroup getStripZone() {
        return stripZone;
    }

    public L2ParamGroup getRecuperatorZ() {
        return recuperatorZ;
    }

    public L2ParamGroup getCommonDFHZ() {
        return commonDFHZ;
    }

    public L2DFHZone getOneL2Zone(int zNum, boolean bBot) {
        L2DFHZone theZone = null;
        if (bBot)
            showError("Not Ready for Bottom Zones in Level2");
        else {
            if (zNum < topL2Zones.size())
                theZone = topL2Zones.get(topSections.get(zNum));
        }
        return theZone;
    }

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

    public boolean getFieldDataFromUser() {
        return oneFieldResults.getDataFromUser();
    }

    public FceSection getOneSection(boolean bBot, int nSec) {
        return ((bBot) ? botSections : topSections).get(nSec);
    }

    FieldResults oneFieldResults;

    synchronized public boolean setFieldProductionData() {
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

    /**
     *
     * @param withStripData  this is required only for taking results from Level1
     * @return
     */
    public ErrorStatAndMsg takeFieldResultsFromLevel1(boolean withStripData) {
        oneFieldResults = new FieldResults(this, withStripData);
        if (oneFieldResults.inError)
            return new ErrorStatAndMsg(true, oneFieldResults.errMsg);
        else {
            if (withStripData) {
                return oneFieldResults.processOkForFieldResults();
            }
            else
                return new ErrorStatAndMsg(); // all ok
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

    public DataWithMsg getSpeedWithFurnaceFuelStatus(L2ZonalFuelProfile zFP) {
        double totFuel = oneFieldResults.totFuel(); // oneFieldResults already read
        return zFP.recommendedSpeed(totFuel, false);
    }

    public double getOutputWithFurnaceTemperatureStatus() {
        ErrorStatAndMsg response = takeFieldResultsFromLevel1(false);
        if (response.inError) {
            logInfo("stripSpeedUpdate: Facing some problem in reading furnace field data: " + response.msg);
        }
        else {
        }
        return 0;
    }

    public double getOutputWithFurnaceTemperatureStatus(Performance refP, double stripWidth, double stripThick) {
//        long stTimeNano = System.nanoTime();
        double exitTempRequired = refP.exitTemp();
        Charge ch = new Charge(controller.getSelChMaterial(refP.chMaterial), stripWidth, 1, stripThick);
        ChargeStatus chStatus = new ChargeStatus(ch, 0, exitTempRequired);
        double outputAssumed = 0;
        tuningParams.setSelectedProc(controller.proc);
        ErrorStatAndMsg response = takeFieldResultsFromLevel1(false);
        if (response.inError) {
            logInfo("Facing some problem in reading furnace field data: " + response.msg);
        }
        else {
            outputAssumed = getOutputWithFurnaceTemperatureStatus(chStatus, refP, exitTempRequired);
        }
        return outputAssumed;
    }

    public double getOutputWithFurnaceTemperatureStatus(ChargeStatus chStatus, Performance refP, double exitTempRequired) {
        long stTimeNano = System.nanoTime();
        double outputAssumed = 0;
        this.calculStep = controller.calculStep;
        setProduction(chStatus.getProductionData());
        if (prepareSlots()) {
            prepareSlotsWithTempO();
            double tempAllowance = 2;
            outputAssumed = refP.unitOutput * chStatus.getProductionData().charge.getLength();
            boolean done = false;
            double nowExitTemp, diff;
            int trials = 0;
            while (!done) {
                logTrace("in the loop " + trials + ", " + outputAssumed);
                trials++;
                chStatus.output = outputAssumed;
                processInFurnace(chStatus);
                if (chStatus.isValid()) {
                    nowExitTemp = chStatus.tempWM;
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
            logTrace("Trials in getOutputWithFurnaceStatus = " + trials);
        } else
            logInfo("Facing problem in creating calculation steps");
        logTrace("Nano seconds for calculation = " + (System.nanoTime() - stTimeNano));
        return outputAssumed;
    }

    void prepareSlotsWithTempO() {
        oneFieldResults.copyTempAtTCtoSection();
        setTempOForAllSlots();
    }

    public void handleNextStrip() {
        if (processBeingUpdated.get()) {
            logInfo("process Data is being modified, cannot handle new Strip now!");
        }
        else {
            if (fieldDataIsBeingHandled.get()) {
                showErrorInLevel1("Level2 is busy handling field Performance");
            } else {
                if (newStripIsBeingHandled.get()) {
                    showErrorInLevel1("Level2 is still processing the last New Strip Data");
                }
                else {
                    NextStripHandler theNextStripHandler = new NextStripHandler();
                    Thread t = new Thread(theNextStripHandler);
                    t.start();
                }
            }
        }
    }

    boolean sendFuelCharacteristics(L2ZonalFuelProfile zFP) {
        boolean retVal = true;
        for (L2DFHZone oneZone: topL2Zones.values())
            if (!oneZone.setFuelCharacteristic(zFP)) {
                showError("Some problem in setting fuel Characteristic for " + oneZone.groupName);
                retVal = false;
                break;
            }
        return retVal;
    }

    void handleFieldPerformance() {
        FieldPerformanceHandler thePerfHandler= new FieldPerformanceHandler(oneFieldResults);
        Thread t = new Thread(thePerfHandler);
        t.start();
    }

    public void informPerformanceDataModified() {
        if (tagRuntimeReady.getValue().booleanValue)
            performanceStat.markReady(true);
    }

    public void informProcessDataModified() {
        if (tagRuntimeReady.getValue().booleanValue)
            processDataStat.markReady(true);
    }

    void handleFieldData()  {
        fieldDataParams.setAsNoted(true);
        if (l2DFHeating.bAllowUpdateWithFieldData) {
            if (newStripIsBeingHandled.get()) {
                showErrorInLevel1("Level2 is busy handling new Strip data");
            } else {
                if (level2Enabled) {
                    if (fieldDataIsBeingHandled.get()) {
                        showErrorInLevel1("Last field data is still being handled. Retry later");
                    } else {
                        handleFieldPerformance();
                    }
                } else
                    l2DFHeating.showMessage("Level2 is not enabled from Level1");
            }
        } else
            showErrorInLevel1("Not enabled for handling field data");
    }

    AtomicBoolean fieldDataIsBeingHandled = new AtomicBoolean(false);
    AtomicBoolean newStripIsBeingHandled = new AtomicBoolean(false);
    AtomicBoolean processBeingUpdated = new AtomicBoolean(false);
    AtomicBoolean stripSpeedRoutineON = new AtomicBoolean(false);

    public boolean isProcessDataBeingUsed() {
        return (fieldDataIsBeingHandled.get()  ||  newStripIsBeingHandled.get() || stripSpeedRoutineON.get());
    }

    public void setProcessBeingUpdated(boolean updateON) {
        processBeingUpdated.set(updateON);
    }

    boolean addFieldBasedPerformanceToPerfBase() {
        Performance perform = getPerformance();
        boolean retVal = false;
        boolean goAhead = true;
        boolean replace = false;
        if (perform != null && performBase != null) {
            logTrace("performance looks ok");
            if (performBase.similarPerformance(perform) != null) {
                logTrace("Similar performance already available");
                replace = true;
                goAhead = l2DFHeating.decide("Similar performance already available", "Do you want to over-write Performance Data?");
            }
        }
        if (goAhead) {
            l2DFHeating.enablePerfMenu(false);
            StatusWithMessage resp = l2DFHeating.setPerformanceTableLimits(perform);
            if (resp.getDataStatus() == StatusWithMessage.DataStat.OK) {
                FceEvaluator eval = controller.calculateForPerformanceTable(perform);
                if (eval != null) {
                    logTrace("eval for Performance table is ok");
                    try {
                        eval.awaitThreadToExit();
                        logTrace("eval for Performance table is completed");
                        if (eval.healthyExit()) {
                            logTrace("eval for Performance table had healthy exit");
                            if (replace)
                                performBase.replaceExistingPerformance(perform);
                            else
                                performBase.noteBasePerformance(perform);
                            retVal = true;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else
                    logInfo("eval for Performance table is null");
            }
            else
                showError(resp.getErrorMessage() + "\nField production/strips size data outside range\nPerformance not saved");
        }
        return retVal;
    }

    synchronized void resetFieldDataBeingHandled() {
        logTrace("fieldDataIsBeingHandled is made OFF");
        fieldDataIsBeingHandled.set(false);
        l2DFHeating.enablePerfMenu(true);
    }

    synchronized void setFieldDataBeingHandled() {
        fieldDataIsBeingHandled.set(true);
        l2DFHeating.enablePerfMenu(false);

    }

    void handleModifiedPerformanceData() {
        PerformanceModificationHandler thePerfModHandler= new PerformanceModificationHandler();
        Thread t = new Thread(thePerfModHandler);
        t.start();
    }

    public boolean showErrorInLevel1(String msg) {
        return showErrorInLevel1("", msg);
    }

    public boolean showErrorInLevel1(String header, String msg) {
        String assembledMsg = ((header.length() > 0) ? (header + headerBreak) : "") + msg;
        logInfo(msg);
        l2ErrorMessages.setValue(Tag.TagName.Msg, assembledMsg);
        return l2ErrorMessages.markReady(true);
    }

    public boolean showInfoInLevel1(String msg) {
        return showInfoInLevel1("", msg);
    }

    public boolean showInfoInLevel1(String header, String msg) {
        String assembledMsg = ((header.length() > 0) ? (header + headerBreak) : "") + msg;
        l2InfoMessages.setValue(Tag.TagName.Msg, assembledMsg);
        return l2InfoMessages.markReady(true);
    }

    public boolean getYesNoResponseFromLevel1(String msg, int waitSeconds) {
        return getYesNoResponseFromLevel1("", msg, waitSeconds);
    }

    public boolean getYesNoResponseFromLevel1(String header, String msg, int waitSeconds) {
        String assembledMsg = ((header.length() > 0) ? (header + headerBreak) : "") + msg;
        return yesNoResponse.getResponse(assembledMsg, waitSeconds);
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

    public void logInfo(String msg) {
        l2DFHeating.l2Info("l2DFHFurnace: " + msg);
    }

    public void logDebug(String msg) {
        l2DFHeating.l2debug("l2DFHFurnace: " + msg);
    }

    public void logTrace(String msg) {
        l2DFHeating.l2Trace("l2DFHFurnace: " + msg);
    }

    public void logError(String msg) {
        showError("L2DFHFurnace: " + msg);
    }

    void showMessage(String title, String msg) {
        (new TimedMessage(title, msg, TimedMessage.INFO, controller.parent(), 3000)).show();
    }

    class L2DisplayUpdater implements Runnable {
        public void run() {
            while (l2DisplayON) {
                try {
                    Thread.sleep(displayUpdateInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                updateUIDisplay();
            }
        }
    }

    class NextStripHandler implements Runnable {
        public void run() {
            newStripIsBeingHandled.set(true);
            if (level2Enabled) {
                StatusWithMessage newStripStatusWithMsg = new StatusWithMessage();
                StripProcessAndSize newStrip = stripZone.setNextStripAction(newStripStatusWithMsg);
                StatusWithMessage.DataStat newStripStatus = newStripStatusWithMsg.getDataStatus();
                if (newStripStatus != StatusWithMessage.DataStat.WithErrorMsg) {
                    logTrace("New " + newStrip);
                    L2ZonalFuelProfile zFP = new L2ZonalFuelProfile(newStrip.refP.getPerformanceTable(),
                            furnaceSettings.fuelCharSteps, l2DFHeating);
                    if (zFP.prepareFuelTable(newStrip.width, newStrip.thickness)) {
                        DataWithMsg speedBasedOnFuel = getSpeedWithFurnaceFuelStatus(zFP);
                        DataWithMsg.DataStat speedOnFuelStat = speedBasedOnFuel.getStatus();
                        if (speedOnFuelStat == DataWithMsg.DataStat.OK)
                            logTrace("Speed Based On Fuel = " + speedBasedOnFuel.doubleValue + "m/min");
                        else
                            logInfo("Error in Speed based on Fuel :" + speedBasedOnFuel.errorMessage);
                        if (sendFuelCharacteristics(zFP)) {
                            if (newStripStatus == StatusWithMessage.DataStat.WithInfoMsg)
                                showInfoInLevel1(newStripStatusWithMsg.getInfoMessage());
                            commonDFHZ.setValue(L2ParamGroup.Parameter.L2Data, Tag.TagName.Ready, true);
                        } else
                            showErrorInLevel1("Unable to send Fuel Characteristics");
                    } else
                        showErrorInLevel1("Unable to prepare Fuel Characteristics");
                } else
                    showErrorInLevel1(newStripStatusWithMsg.getErrorMessage());
            } else
                l2DFHeating.showMessage("Level2 is not enabled from Level1");
            newStripIsBeingHandled.set(false);
        }
    }

    class FieldPerformanceHandler implements Runnable {
        FieldResults theFieldData;
        FieldPerformanceHandler(FieldResults theFieldData) {
            this.theFieldData = theFieldData;
        }
        public void run() {
            setFieldDataBeingHandled();
            boolean response = getYesNoResponseFromLevel1("Confirm that Strip Size Data is updated", 20);
            logTrace("L2 " + ((response) ? "Responded " : "did not Respond ") + "to Confirm-Strip-Data query");
            if ((response) && (l2YesNoQuery.getValue(Tag.TagName.Response).booleanValue)) {
                ErrorStatAndMsg stat = takeFieldResultsFromLevel1(true); // l2DFHeating.takeResultsFromLevel1();
                if (stat.inError) {
                    showErrorInLevel1("Taking Field Performance", stat.msg);
                }
                else {
                    if (setFieldProductionData()) {
                        setCurveSmoothening(false);
                        l2DFHeating.showMessage("Field Performance", "Evaluating from Model");
                        FceEvaluator eval1 = l2DFHeating.calculateFce(true, null);
                        if (eval1 != null) {
                            try {
                                eval1.awaitThreadToExit();
                                if (eval1.healthyExit()) {
                                    if (adjustForFieldResults()) {
                                        FceEvaluator eval2 = l2DFHeating.calculateFce(false, null); // without reset the loss Factors
                                        if (eval2 != null) {
                                            eval2.awaitThreadToExit();
                                            logTrace("eval2 completed");
                                            if (eval2.healthyExit()) {
                                                logTrace("eval2 had healthy exit");
                                                if (addFieldBasedPerformanceToPerfBase())
                                                    l2DFHeating.showMessage("Field Performance", "Save updated Performance to file from Performance Menu");
                                            }
                                        }
                                    }
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            else
                showErrorInLevel1("Running Strip size data is not confirmed. Performance data is NOT recorded");
            resetFieldDataBeingHandled();
        }
    }

    class PerformanceModificationHandler implements Runnable {
        public void run() {
            int count = 5;
            boolean gotIt = false;
            while(--count > 0) {
                if (!isProcessDataBeingUsed()) { // newStripIsBeingHandled.get() && !fieldDataIsBeingHandled.get()) {
                    fieldDataIsBeingHandled.set(true);
                    gotIt = l2DFHeating.handleModifiedPerformanceData();
                    fieldDataIsBeingHandled.set(false);
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (!gotIt)
                logInfo("Unable to read Modified Performance Data");
        }
    }

    class SubAliveListener implements SubscriptionAliveListener {     // TODO This is common dummy class used everywhere to be made proper
        public void onAlive(Subscription s) {
//            l2DFHeating.showMessage("" + s + String.format(
//                    "%tc Subscription alive: ID=%d lastAlive=%tc",
//                    Calendar.getInstance(), s.getSubscriptionId().getValue(),
//                    s.getLastAlive()));
        }

        public void onTimeout(Subscription s) {
            l2DFHeating.showMessage(String.format("In L2Furnace (%s) %s timeout at %tc, last alive at %tc ",
                    l2DFHeating.releaseDate(), s, Calendar.getInstance(), s.getLastAlive()));
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

    class FieldDataListener extends L2SubscriptionListener {
        @Override
        public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
            L2AccessControl.AccessLevel accessLevel = l2DFHeating.accessLevel;
            if (l2DFHeating.isL2SystemReady() &&
                    ((accessLevel == L2AccessControl.AccessLevel.UPDATER) || (accessLevel == L2AccessControl.AccessLevel.EXPERT))) {
                Tag theTag = monitoredTags.get(monitoredDataItem);
                if (fieldDataParams.isNewData(theTag))   // the data will be already read if new data
                    if (theTag == tagFieldDataReady)
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

    class UpdaterChangeListener extends L2SubscriptionListener {
        @Override
        public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
            L2AccessControl.AccessLevel accessLevel = l2DFHeating.accessLevel;
            if (l2DFHeating.isL2SystemReady()) {
                Tag theTag = monitoredTags.get(monitoredDataItem);
                if (processDataStat.isNewData(theTag)) {
                    if (itIsRuntime && (theTag == processDataChanged) && theTag.getValue().booleanValue) {
                        logTrace("Process data Changed");
                        processDataStat.setAsNoted(true);
                        l2DFHeating.handleModifiedProcessData();
                    }
                }
                if (performanceStat.isNewData(theTag)) {
                    if (itIsRuntime && (theTag == performanceChanged) && theTag.getValue().booleanValue) {
                        logTrace("Performance data Changed");
                        performanceStat.setAsNoted(true);
                        handleModifiedPerformanceData();
                    }
                }
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
                if (level2Enabled)
                    logInfo("Level2 has been Enabled!");
                else
                    l2DFHeating.showMessage("Level2 has been disabled!");
            }
        }
    }

    public boolean informLevel2Ready() {
        return (basicZone.setValue(L2ParamGroup.Parameter.L2Stat, Tag.TagName.Ready, tagLevel2Enabled.getValue().booleanValue)).booleanValue;
    }
}
