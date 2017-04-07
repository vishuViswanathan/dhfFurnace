package level2.stripDFH;

import TMopcUa.ProcessValue;
import TMopcUa.TMSubscription;
import TMopcUa.TMuaClient;
import basic.*;
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
public class L2DFHFurnace extends StripFurnace implements L2Interface {
    public TMuaClient source;
    LinkedHashMap<FceSection, L2DFHZone> topL2Zones;
    LinkedHashMap<FceSection, L2DFHZone> botL2Zones;
    L2OneParameterZone rthExit;
    L2OneParameterZone soakZone;
    L2OneParameterZone hbExit;
    ReadyNotedParam l2InfoMessages;
    ReadyNotedParam l2ErrorMessages;
    ReadyNotedParam fieldDataParams;
    ReadyNotedParam processListParams;
    ReadyNotedParam fuelCharParams;
//    ReadyNotedBothL2 performanceStat;
    ReadyNotedBothL2 processDataStat;
    ReadyNotedParam l2YesNoQuery;
    ReadyNotedParam l2DataQuery;
    Vector<ReadyNotedParam> readyNotedParamList = new Vector<>();
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
    Tag tagProcessListReady;
    Tag tagProcessCount;
    TMSubscription basicSub;
    TMSubscription messageSub;
    TMSubscription fieldDataSub;
    TMSubscription processListSub;
    TMSubscription fuelCharSub;
    TMSubscription updaterChangeSub;
    boolean messagesON = false;
    Hashtable<MonitoredDataItem, Tag> monitoredTags;
    boolean monitoredTagsReady = false;
    public L2DFHeating l2DFHeating;
    boolean itIsRuntime = false;
    boolean processListSetOnLevel1 = false;

    long displayUpdateInterval = 1000; // 1 sec

    public L2DFHFurnace(L2DFHeating l2DFHEating, boolean bTopBot, boolean bAddTopSoak, ActionListener listener) {
        super(l2DFHEating, bTopBot, bAddTopSoak, listener);
        this.l2DFHeating = l2DFHEating;
    }

    public boolean basicConnectionToLevel1(TMuaClient level1Source) {
        source = level1Source;
        source.removeAllSubscriptions();
        this.equipment = l2DFHeating.equipment;
        monitoredTags = new Hashtable<>();
        topL2Zones = new LinkedHashMap<>();
        messagesON = createL2Messages();
        itIsRuntime = (l2DFHeating.accessLevel == L2AccessControl.AccessLevel.RUNTIME);
        return createInternalZone();
    }

    public boolean makeAllConnections() {
        boolean retVal = false;
        if (createBasicZone())
            if (createStripZone())
                if (createRecuParam())
                    if (createCommonDFHZ()) {
                        if (createL2Zones())
                            retVal = true;
                    }
        return retVal;
    }

    Thread displayThread;

    public void startL2DisplayUpdater() {
        l2DisplayON = true;
        L2DisplayUpdater displayUpdater = new L2DisplayUpdater();
        displayThread = new Thread(displayUpdater);
        displayThread.start();
        logTrace("L2DfhFurnace.126: l2 Display updater started ...");
    }

    void stopDisplayUpdater() {
        if (l2DisplayON) {
            l2DisplayON = false;
            try {
                if (displayThread != null) displayThread.join(displayUpdateInterval * 5);
            } catch (InterruptedException e) {
                logError("L2DfhFurnace..135: Problem in stopping Display Updater");
                e.printStackTrace();
            }
        }
    }

    public void startSpeedUpdater() {
        stripZone.startSpeedUpdater();
    }

    boolean canResetAccess = false; // resetting the application status in 'Level2.Internal'

    public boolean considerFieldZoneTempForLossCorrection() {
        return controller.dfhProcessList.considerFieldZoneTempForLossCorrection;
    }

    public double getMinLossCorrectionFactor() {
        return l2DFHeating.dfhProcessList.getMinLossCorrectionFactor();
    }

    public double getMaxLossCorrectionFactor() {
        return l2DFHeating.dfhProcessList.getMaxLossCorrectionFactor();
    }

    /**
     * Checks in the the module access level is valid or not
     * Baically checks if another module exists with this level
     * @return
     */
    public StatusWithMessage checkAccessLevel() {
        StatusWithMessage retVal = new StatusWithMessage();
        if (l2DFHeating.isOnProductionLine()) {
            switch (l2DFHeating.accessLevel) {
                case EXPERT:
                    if (tagExpertReady.getValue().booleanValue)
                        retVal.setErrorMessage("Level2 with Expert Access is already ON");
                    else if (tagUpdaterReady.getValue().booleanValue)
                        retVal.setErrorMessage("Exit from Level2 Updater and try again as Expert");
                    break;
                case UPDATER:
                    if (tagUpdaterReady.getValue().booleanValue)
                        retVal.setErrorMessage("Level2 with Updater Access is already ON");
                    else if (tagExpertReady.getValue().booleanValue)
                        retVal.setErrorMessage("Exit from Level2 Expert and try again as Updater");
                    break;
                default:
                    if (tagRuntimeReady.getValue().booleanValue)
                        retVal.setErrorMessage("Level2-Runtime is already ON");
                    break;
            }
        }
        return retVal;
    }

    public StatusWithMessage noteAccessLevel() {
        StatusWithMessage retVal = new StatusWithMessage();
        Tag accessMarkTag;
        if (l2DFHeating.isOnProductionLine()) {
            switch (l2DFHeating.accessLevel) {
                case EXPERT:
                    accessMarkTag = tagExpertReady;
                    break;
                case UPDATER:
                    accessMarkTag = tagUpdaterReady;
                    break;
                default:
                    accessMarkTag = tagRuntimeReady;
                    break;
            }
            accessMarkTag.setValue(true);
            if (accessMarkTag.getValue().booleanValue)
                canResetAccess = (retVal.getDataStatus() == DataStat.Status.OK);
            else
                retVal.addErrorMessage("Unable to set Runtime ON on Level1");
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

//    Tag performanceChanged, performanceNoted;
    Tag processDataChanged, processDataNoted;

    public boolean createInternalZone() {
        boolean retVal = false;
        String processElement = "Internal.Performance";
        internalZone = new L2ParamGroup(this, "Internal");
        updaterChangeSub = source.createTMSubscription("Updater Status",new SubAliveListener(), new UpdaterChangeListener());
//        Tag[] performanceTags = {
//                performanceChanged = new Tag(L2ParamGroup.Parameter.Performance, Tag.TagName.Ready, true, itIsRuntime),
//                performanceNoted = new Tag(L2ParamGroup.Parameter.Performance, Tag.TagName.Noted, true, !itIsRuntime)};
//        try {
//            performanceStat = new ReadyNotedBothL2(source, equipment, processElement, performanceTags, updaterChangeSub);
//            performanceStat.setReadWriteStat(!itIsRuntime);
//        } catch (TagCreationException e) {
//            showError("Some Tags could not be created: " + e.getMessage());
//            return false;
//        }
//        readyNotedParamList.add(performanceStat);
//        internalZone.addOneParameter(L2ParamGroup.Parameter.Performance, performanceStat);
//        noteMonitoredTags(performanceTags);

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
//            logTrace("L2DfhFurnace..274: Internal zone created");
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
        processListSub = source.createTMSubscription("Process List", new SubAliveListener(), new ProcessListListener());
        commonDFHZ = new L2ParamGroup(this, "DFHCommon"); // , fieldDataSub);
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

            Tag[] commonDFHTags4 = {
                    (tagProcessListReady = new Tag(L2ParamGroup.Parameter.Processes, Tag.TagName.Ready, true, false)),
                    (tagProcessCount = new Tag(L2ParamGroup.Parameter.Processes, Tag.TagName.Count, true, false)),
                    (new Tag(L2ParamGroup.Parameter.Processes, Tag.TagName.Noted, false, true))};
            processListParams = new ReadyNotedParam(source, equipment, "DFHCommon.Processes", commonDFHTags4, processListSub);
            readyNotedParamList.add(processListParams);
            commonDFHZ.addOneParameter(L2ParamGroup.Parameter.Processes, processListParams);
            noteMonitoredTags(commonDFHTags4);

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
                    new Tag(L2ParamGroup.Parameter.Flue, Tag.TagName.EntryTemp, false, false),
                    new Tag(L2ParamGroup.Parameter.Flue, Tag.TagName.ExitTemp, false, false)};
            recuperatorZ.addOneParameter(L2ParamGroup.Parameter.Flue, recuFlueTags);
            noteMonitoredTags(recuFlueTags);
            Tag[] recuAirTags = {
                    new Tag(L2ParamGroup.Parameter.AirFlow, Tag.TagName.ExitTemp, false, false)};
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
//        logDebug("L2DFHFurnace.379: creating L2 Messages");
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

    public void resetForLevel2Operation(boolean starting) {
        if (itIsRuntime) {
            for (ReadyNotedParam p: readyNotedParamList)
                p.initStatus();
            stripZone.resetForLevel2Operation();
            if (starting) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                level2Enabled = informLevel2Ready();
                logInfo("Level2 is disabled from Level1");
            }
            else {
                informLevel2IsOff();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        else
            level2Enabled = tagLevel2Enabled.getValue().booleanValue;

    }

    public void resetForLevel2OperationOLD() {  // TODO-remove
        for (ReadyNotedParam p: readyNotedParamList)
            p.initStatus();
        stripZone.resetForLevel2Operation();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        furnaceDisplayZones = new Vector<>();
        int zNum = 1;
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
        } catch (TagCreationException e) {
            showError("Error in " + descriptiveName + " - " + e.getMessage());
            retVal = false;
        }
        return retVal;
    }

    public void processListToLevel1Updated(int count) {
        tagProcessCount.setValue(count);
        tagProcessListReady.setValue(true);
        processListSetOnLevel1 = true;
    }

    public void clearProcessList() {
        if (processListSetOnLevel1) {
            logInfo("L2DfhFurnace.491: Process List cleared");
            processListToLevel1Updated(0);
            processListSetOnLevel1 = false;
        }
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
        stripZone.updateLevel2Display();
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
        JPanel jp = new JPanel();
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
        JPanel jp = new JPanel();
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

    double totalFuelFlowNow() {
        double totFlow = 0;
        for (L2DFHZone oneZone: topL2Zones.values())
            totFlow += oneZone.fuelFlowNow();
        return totFlow;
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

    boolean l2DisplayON = false;

    public boolean prepareForDisconnection() {
        resetForLevel2Operation(false);
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

    public FceSection getOneSection(boolean bBot, int nSec) {
        return ((bBot) ? botSections : topSections).get(nSec);
    }

    public FuelFiring getFuelFiring(boolean bRegen, double excessAir, double airTemp, double fuelTemp)  {
        Fuel f = l2DFHeating.getSelFuel(); // TODO considers only one fuel
        if (f != null)
            return new FuelFiring(f, bRegen, excessAir, airTemp, fuelTemp);
        else
            return null;
    }

    /**
     * Evaluating charge exit conditions for the existing furnace temperautures
     * @return
     */
    public ChargeStatus processInFurnace(ChargeStatus chargeStatus) {
        if (controller.heatingMode == DFHeating.HeatingMode.TOPBOTSTRIP) {
            setOnlyWallRadiation(true);
            setDisplayResults(false);
            chargeStatus.setProductionData(productionData);
            getReadyToCalcul(true); // do not create new slots if already created
            setEntrySlotChargeTemperature(productionData.entryTemp);
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

    public DataWithStatus getSpeedWithFurnaceFuelStatus(L2ZonalFuelProfile zFP) {
        double totFuel = totalFuelFlowNow();
//        logTrace("L2DfhFurnace..743: total fuel = " + totFuel);
        return zFP.recommendedSpeed(totFuel, false);
    }

    public double getOutputWithFurnaceTemperatureStatus(Performance refP, double stripWidth, double stripThick) {
        double exitTempRequired = refP.exitTemp();
        Charge ch = new Charge(controller.getSelChMaterial(refP.chMaterial), stripWidth, 1, stripThick);
        ChargeStatus chStatus = new ChargeStatus(ch, 0, exitTempRequired);
        double outputAssumed = 0;
        tuningParams.setSelectedProc(controller.furnaceFor);
        FieldResults fieldData = new FieldResults(this, false);
        if (fieldData.inError)   {
            logError("L2DfhFurnace..755: Facing some problem in reading furnace field data: " + fieldData.errMsg);
        }
        else {
            outputAssumed = getOutputWithFurnaceTemperatureStatus(fieldData, chStatus, refP, exitTempRequired);
        }
        return outputAssumed;
    }

    private double getOutputWithFurnaceTemperatureStatus(FieldResults fieldData, ChargeStatus chStatus, Performance refP, double exitTempRequired) {
        long stTimeNano = System.nanoTime();
        double outputAssumed = 0;
        this.calculStep = controller.calculStep;
        setProductionData(chStatus.getProductionData());
        if (prepareSlots()) {
            prepareSlotsWithTempO(fieldData);
            double tempAllowance = 2;
            outputAssumed = refP.unitOutput * chStatus.getProductionData().charge.getLength();
            boolean done = false;
            double nowExitTemp, diff;
            int trials = 0;
            setChEmmissCorrectionFactor(refP.chEmmCorrectionFactor);
//            logTrace("L2DFHFurnace.824: chEmmCorrectionFactor = " + refP.chEmmCorrectionFactor);
            while (!done && trials < 1000) {
//                logTrace("L2DfhFurnace.778: in the loop " + trials + ", " + outputAssumed);
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
            if (trials < 1000) {
                resetChEmmissCorrectionFactor();
//            logTrace("L2DFHFurnace.797: Trials in getOutputWithFurnaceStatus = " + trials);
            }
            else
                outputAssumed = -1;
        } else
            logInfo("L2DFHFurnace.847: Facing problem in creating calculation steps");
//        logTrace("L2DFHFurnace.785: Nano seconds for calculation = " + (System.nanoTime() - stTimeNano));
        return outputAssumed;
    }

    public DataWithStatus<Double> setEmmissFactorBasedOnFieldResults(FieldResults fieldData) {
        long stTimeNano = System.nanoTime();
        tuningParams.setSelectedProc(controller.furnaceFor);
        this.calculStep = controller.calculStep;
        double exitTempRequired = fieldData.production.exitTemp;
        ProductionData productionReqd = fieldData.production;
        ChargeStatus chStatus = new ChargeStatus(productionReqd.charge, productionReqd.production, productionReqd.exitTemp);
        setProductionData(fieldData.production);
        double chEmmissCorrectionFactor = 1.0;
        double chInTemp = productionReqd.entryTemp;
        boolean done = false;
        if (prepareSlots()) {
            prepareSlotsWithTempO(fieldData);
            double tempAllowance = 2;
            double nowExitTemp, diff;
            int trials = 0;
            while (!done) {
                trials++;
                setChEmmissCorrectionFactor(chEmmissCorrectionFactor);
                processInFurnace(chStatus);
                if (chStatus.isValid()) {
                    nowExitTemp = chStatus.tempWM;
                    diff = exitTempRequired - nowExitTemp;
                    logTrace(String.format("L2DfhFurnace.827: nowExitTemp %3.2f, diff %3.4f, chEmmissCorrectionFactor %3.3f", nowExitTemp, diff, chEmmissCorrectionFactor));
                    if (Math.abs(diff) < tempAllowance)
                        done = true;
                    else {
                        logTrace(String.format("L2DfhFurnace.831: exitTempRequired %3.3f, chInTemp %3.3f, nowExitTemp %3.3f",
                                exitTempRequired, chInTemp, nowExitTemp));
                        chEmmissCorrectionFactor = chEmmissCorrectionFactor * (exitTempRequired - chInTemp) / (nowExitTemp - chInTemp);
                        if (chEmmissCorrectionFactor > 2 || chEmmissCorrectionFactor < 0.1)
                            break;
                    }
                }
                else {
                    logError("setEmmissFactorBasedOnFieldResults:" +
                            "processInFurnace returned with error!");
                    break;
                }
            }
//            logTrace("L2DfhFurnace.844: setEmmissFactorBasedOnFieldResults:" +
//                    "Trials in setEmmissFactorBasedOnFieldResults = " + trials);
        } else
            logError("L2DfhFurnace.847: Facing problem in creating calculation steps");
//        logTrace("L2DfhFurnace.848; Emm Factor = " + chEmmissCorrectionFactor + ",  " +
//                "L2DFHFurnace.849: Nano seconds for calculation = " + (System.nanoTime() - stTimeNano));
        DataWithStatus<Double> retVal =  new DataWithStatus<>(chEmmissCorrectionFactor);
        if (!done)
            retVal.addErrorMessage("Unable to evaluate Charge emissivity Correction based on Field Data");
        return retVal;
    }

    void prepareSlotsWithTempO(FieldResults results) {
        results.copyTempAtTCtoSection();
        setTempOForAllSlots();
    }

    public void handleNextStrip() {
        logTrace("L2DFHFurnace.862: Entering handleNewStrip");
        if (processBeingUpdated.get()) {
            logInfo("L2DfhFurnace.863: process Data is being modified, cannot handle new Strip now!");
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

    boolean sendRTHandOtherSPs(OneStripDFHProcess theProcess) {
        ProcessValue p;
        p = rthExit.setValue(L2ParamGroup.Parameter.Temperature, Tag.TagName.SP, (float)theProcess.rthExitTemp);
        if (p.valid) {
            p = soakZone.setValue(L2ParamGroup.Parameter.Temperature, Tag.TagName.SP, (float) theProcess.soakTemp);
            if (p.valid) {
                p = hbExit.setValue(L2ParamGroup.Parameter.Temperature, Tag.TagName.SP, (float) theProcess.hbrStripTemp);
            }
        }
        return p.valid;
    }

    void handleFieldPerformance() {
        FieldPerformanceHandler thePerfHandler= new FieldPerformanceHandler();
        Thread t = new Thread(thePerfHandler);
        t.start();
    }

    public void informProcessDataModified() {
        if (tagRuntimeReady.getValue().booleanValue)
            processDataStat.markReady(true);
    }

    void handleFieldData()  {
        logTrace("L2DFHFurnace.917: entering handleFieldData");
//        fieldDataParams.setAsNoted(true);
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
        fieldDataParams.setAsNoted(true);
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
        logTrace("L2DFHFurnace.1002: Performance = " + perform);   // TODO tobe removed on RELEASE
        boolean retVal = false;
        boolean goAhead = true;
        boolean replace = false;
        if (perform != null && performBase != null) {
//            logTrace("L2DFHFurnace.1007: performance looks ok");
            Performance similarOne = performBase.similarPerformance(perform);
            if (similarOne != null) {
//                logTrace("L2DFHFurnace.1009: Similar performance already available");
                replace = true;
                goAhead = (l2DFHeating.loadTesting) || l2DFHeating.decide("Similar performance already available",
                        "Existing performance:\n" + similarOne.toString() +
                                "\n    Do you want to over-write Performance Data?");
            }
        }
        if (goAhead) {
            retVal = calculateForPerformanceTable(perform, replace);
        }
        return retVal;
    }

    public boolean calculateForPerformanceTable(Performance perform, boolean replace) {
        boolean retVal = false;
        l2DFHeating.enablePerfMenu(false);
        FceEvaluator eval = controller.calculateForPerformanceTable(perform);
        if (eval != null) {
//            logTrace("L2DfhFurnace.975: eval for Performance table is ok");
            try {
                eval.awaitThreadToExit();
//                logTrace("L2DfhFurnace.978: eval for Performance table is completed");
                if (eval.healthyExit()) {
//                    logTrace("L2DfhFurnace.980: eval for Performance table had healthy exit");
                    if (replace) {
                        retVal = performBase.replaceExistingPerformance(perform);
                    }
                    else
                        retVal = performBase.noteBasePerformance(perform);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else
            logInfo("L2DfhFurnace.991: eval for Performance table is null");
        return retVal;
    }

    public DataWithStatus<OneStripDFHProcess> createNewNewProcess(double stripExitT, double thick, double width,
                                                                  double speed, String baseProcessName) {
        return l2DFHeating.dfhProcessList.addFieldProcess(baseProcessName, stripExitT, thick, width, speed);
    }

    synchronized void resetFieldDataBeingHandled() {
//        logTrace("L2DfhFurnace.1001: Trying to make fieldDataIsBeingHandled OFF");
        fieldDataIsBeingHandled.set(false);
        logTrace("L2DfhFurnace.1003: fieldDataIsBeingHandled is made OFF");
        l2DFHeating.enablePerfMenu(true);
    }

    synchronized void setFieldDataBeingHandled() {
//        logTrace("L2DfhFurnace.1008: Trying to make fieldDataIsBeingHandled ON");
        fieldDataIsBeingHandled.set(true);
        logTrace("L2DfhFurnace.1010: fieldDataIsBeingHandled is made ON");
        l2DFHeating.enablePerfMenu(false);

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
        l2DFHeating.l2Error("L2DFHFurnace: " + msg);
    }

    public void showError(String msg) {
        super.showError(l2DFHeating.accessLevel.toString() + ":" +  msg);
        logError(msg);
    }

    void showMessage(String title, String msg) {
        (new TimedMessage(l2DFHeating.accessLevel.toString() + ":" + title, msg, TimedMessage.INFO, controller.parent(), 3000)).show();
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
                DataStat.Status newStripStatus = newStripStatusWithMsg.getDataStatus();
                if (newStripStatus != DataStat.Status.WithErrorMsg) {
                    logTrace("L2DfhFurnace.1103: New " + newStrip);
                    L2ZonalFuelProfile zFP = new L2ZonalFuelProfile(newStrip.refP.getPerformanceTable(),
                            furnaceSettings.fuelCharSteps, l2DFHeating);
                    if (zFP.prepareFuelTable(newStrip.width, newStrip.thickness)) {
//                        DataWithStatus speedBasedOnFuel = getSpeedWithFurnaceFuelStatus(zFP) ;
//                        DataStat.Status speedOnFuelStat = speedBasedOnFuel.getStatus();
//                        if (speedOnFuelStat == DataStat.Status.OK)
//                            logTrace("L2DfhFurnace.1110: Speed Based On Fuel = " + speedBasedOnFuel.getValue() + "m/min");
//                        else
//                            logInfo("L2DfhFurnace..1112: Error in Speed based on Fuel :" + speedBasedOnFuel.getErrorMessage());
                        if (sendFuelCharacteristics(zFP)) {
                            if (newStripStatus == DataStat.Status.WithInfoMsg)
                                showInfoInLevel1(newStripStatusWithMsg.getInfoMessage());
                            if (sendRTHandOtherSPs(newStrip.getProcess()))
                                commonDFHZ.setValue(L2ParamGroup.Parameter.L2Data, Tag.TagName.Ready, true);
                            else
                                showErrorInLevel1("Unable to set RTH, Soak snd Exit section Temperatures");
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
        boolean bErrMSg = false;
        boolean bInfoMsg = false;
        StringBuilder errMsg = new StringBuilder("Handling Field Results:");
        StringBuilder infoMsg = new StringBuilder("Handling Field Results:");
        public void run() {
            logTrace("L2DFHFurnace.1138: Run in FieldPerformanceHandler");
            setFieldDataBeingHandled();
            boolean response = getYesNoResponseFromLevel1("Confirm that Strip Size Data is updated", 20);
            FieldResults theFieldResults = null;
            aBlock:
            {
                if ((response) && (l2YesNoQuery.getValue(Tag.TagName.Response).booleanValue)) {
                    boolean canContinue = true;
                    theFieldResults = new FieldResults(L2DFHFurnace.this, true,
                            (l2DFHeating.accessLevel == L2AccessControl.AccessLevel.EXPERT));
                    if (theFieldResults.inError) {
                        canContinue = false;
                        addErrorMsg("01 " + theFieldResults.errMsg);
                    } else {
                        ErrorStatAndMsg dataOkForFieldResults = theFieldResults.processOkForFieldResults();  // TODO modify this for Fresh Process from field
                        if (dataOkForFieldResults.inError) {
                            canContinue = false;
                            addErrorMsg("02 " + dataOkForFieldResults.msg);
                        }
                    }
                    if (canContinue) {
                        if (l2DFHeating.setFieldProductionData(theFieldResults)) {
                            boolean considerFieldZoneTempForStripTempProfile =
                                    l2DFHeating.dfhProcessList.considerFieldZoneTempForStripTempProfile;
                            setCurveSmoothening(false);
                            DataWithStatus<Double> emmStat = null;
                            logTrace("l2DFHFurnace.1163: theFieldResults.production.chEmmissCorrectionFactor = " +
                                    theFieldResults.production.chEmmissCorrectionFactor);   // TOD to be removed on RELEASE
                            if (considerFieldZoneTempForStripTempProfile) {
                                emmStat = setEmmissFactorBasedOnFieldResults(theFieldResults);
                            }

//                            logInfo("l2DFHFurnace.1218: productionData = " + productionData);   // TOD to be removed on RELEASE
                            if (productionData != null)
                                logTrace("l2DFHFurnace.1171: productionData.chEmmissCorrectionFactor = " + productionData.chEmmissCorrectionFactor);   // TOD to be removed on RELEASE

                            if (!considerFieldZoneTempForStripTempProfile || (emmStat.getDataStatus() == DataStat.Status.OK)) {
                                if (!considerFieldZoneTempForStripTempProfile || fillChInTempProfile()) {

                                    bConsiderPresetChInTempProfile = considerFieldZoneTempForStripTempProfile;
//                                    l2DFHeating.showMessage("Field Performance", "Evaluating from Model" );
//                                    logTrace("L2DFHFurnace.1177: Evaluating From Model");
//                                    if (productionData != null)
//                                        logInfo("l2DFHFurnace.1180: productionData.chEmmissCorrectionFactor = " + productionData.chEmmissCorrectionFactor);
                                    boolean resetLossFactors = true;
                                    if (theFieldResults.stripDFHProc.isPerformanceAvailable()) {
                                        controller.takeValuesFromUI();
                                        setLossFactor(theFieldResults.stripDFHProc.getPerformance());
                                        resetLossFactors = false;
                                    }
                                    FceEvaluator eval1 = l2DFHeating.calculateFce(resetLossFactors, null, "From Model");
                                    if (eval1 != null) {
                                        showMessage("Field Data is being Processed");
                                        try {
                                            eval1.awaitThreadToExit();
                                            if (eval1.healthyExit()) {
                                                logTrace("L2DFHFurnace.11197: eval1 had healthy exit" );
                                                boolean proceed = true;
                                                if (proceed && theFieldResults.adjustForFieldResults()) { // was (adjustForFieldResults()) {
//                                                logTrace("L2DFHFurnace.1195: Recalculating after fuel flow adjustments");
                                                    FceEvaluator eval2 = l2DFHeating.calculateFce(false, null, "With Field Fuel Flow"); // without reset the loss Factors
                                                    if (eval2 != null) {
                                                        eval2.awaitThreadToExit();
//                                                        logTrace("L2DFHFurnace.1199: eval2 completed" );
                                                        if (eval2.healthyExit()) {
//                                                            logTrace("L2DFHFurnace.1205: eval2 had healthy exit" );
                                                            if (addFieldBasedPerformanceToPerfBase()) {
//                                                                logTrace("l2DFHFurnace.1207: productionData.chEmmissCorrectionFactor = " + productionData.chEmmissCorrectionFactor);
                                                                if (l2DFHeating.loadTesting)
                                                                    informProcessDataModified();
                                                                else
                                                                    l2DFHeating.showMessage("Field Performance", "Save updated Performance to file from Performance Menu" );

                                                            }
                                                            else {
                                                                addErrorMsg("Performance Data is rejected Level2 user");
                                                                showError("Performance Data is rejected Level2 user");
                                                            }
                                                        } else {
//                                                            if (eval2.isAborted())
//                                                                showError("Aborted from Calculation with Fuel Corrections" );
                                                            addErrorMsg("calculation was aborted by Level2 user");
                                                        }
                                                    } else
                                                        showError("Unable ot proceed with Fuel Corrections" );
                                                    resetLossFactor();
                                                    logTrace("L2DfhFurnace.1218: lossFactors reset" );
                                                }
                                            } else {
//                                                if (eval1.isAborted())
//                                                    showError("Aborted from Model Calculation" );
                                                addErrorMsg("calculation was aborted by Level2 user");
                                            }
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        showError("Unable to do Calculation from Model");
                                        addErrorMsg("Unable to do Calculation from Model");
                                    }
                                    resetLossFactor();
                                }
                            } else {
                                showError("Facing some problem in evaluating strip emissivity factor");
                                addErrorMsg("Facing some problem in evaluating strip emissivity factor");
                            }
                            resetChEmmissCorrectionFactor();
                        }
                        setCurveSmoothening(false);
                    } else
                        break aBlock;
                } else
                    addErrorMsg("Running Strip size data is not confirmed. Performance data is NOT recorded" );
            }
            if (theFieldResults != null)  {
                OneStripDFHProcess theProcess= theFieldResults.stripDFHProc;
                if (theProcess != null) {
                    if (!theProcess.isPerformanceAvailable()) {
                        l2DFHeating.removeTheProcess(theProcess);
                        logInfo("L2DfhFurnace.1247: The process " + theProcess + " was removed since Performance calculation failed");
                    }
                }
            }
            if (bErrMSg) {
                showErrorInLevel1(errMsg.toString());
            }
            else if (bInfoMsg) {
                showInfoInLevel1(infoMsg.toString());
            }
            bConsiderPresetChInTempProfile = false;
            resetFieldDataBeingHandled();

            l2DFHeating.switchPage(L2DFHeating.L2DisplayPageType.PROCESS);
        }

        void addErrorMsg(String msg) {
            errMsg.append("\n").append(msg);
            bErrMSg = true;
        }

        void addInfoMsg(String msg) {
            infoMsg.append("\n").append(msg);
            bInfoMsg = true;
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
                logTrace("L2DFHFurnace.1324: FieldDataListener heard something");
                if (fieldDataParams.isItAMember(theTag)) {
                    if (fieldDataParams.isNewData(theTag))   // the data will be already read if new data
                        if (theTag == tagFieldDataReady) {
                            if (!theTag.getValue().booleanValue)  // look again after getAllValues in isNewData()
                                logError("L2DFHFurnace.1328: value is false");
                            handleFieldData();
                        }
                }
                else
                    logError("L2DSFHFurnace.1334: theTag is not a member of fieldDataParams");
            }
        }
    }

    /**
     *  This only for handling ProcessList change noted signal for Level1
      */
    class ProcessListListener extends L2SubscriptionListener {
        @Override
        public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
            L2AccessControl.AccessLevel accessLevel = l2DFHeating.accessLevel;
            if (l2DFHeating.isL2SystemReady() && itIsRuntime ) {
                Tag theTag = monitoredTags.get(monitoredDataItem);
                processListParams.isNewData(theTag);
//                logTrace("L2DfhFurnace.1336: ProcessListListener heard something");
            }
        }
    }

    class FuelCharListener extends L2SubscriptionListener {
         @Override
         public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
             if (l2DFHeating.isL2SystemReady()) {
                 Tag theTag = monitoredTags.get(monitoredDataItem);
                 fuelCharParams.isNewData(theTag);
//                 logTrace("L2DfhFurnace..1347: FuelCharListener heard something");
             }
         }
    }

    class UpdaterChangeListener extends L2SubscriptionListener {
        @Override
        public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
            if (l2DFHeating.isL2SystemReady()) {
                Tag theTag = monitoredTags.get(monitoredDataItem);
                if (processDataStat.isNewData(theTag)) {
                    if (itIsRuntime && (theTag == processDataChanged) && theTag.getValue().booleanValue) {
                        logTrace("L2DfhFurnace.1384: Process data Changed");
                        processDataStat.setAsNoted(true);
                        l2DFHeating.handleModifiedProcessData();
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
                    logInfo("L2DfhFurnace.1417: Level2 is enabled from Level1");
                else
                    logInfo("L2DfhFurnace.1419: Level2 is disabled from Level1");
            }
        }
    }

    void informLevel2IsOff() {
        basicZone.setValue(L2ParamGroup.Parameter.L2Stat, Tag.TagName.Ready, false);
    }

    public boolean informLevel2Ready() {
        return (basicZone.setValue(L2ParamGroup.Parameter.L2Stat, Tag.TagName.Ready, tagLevel2Enabled.getValue().booleanValue)).booleanValue;
    }
}
