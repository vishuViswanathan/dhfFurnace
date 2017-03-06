package level2.stripDFH;

import FceElements.heatExchanger.HeatExchProps;
import TMopcUa.ProcessValue;
import TMopcUa.TMSubscription;
import TMopcUa.TMuaClient;
import basic.*;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionAliveListener;
import com.sun.org.apache.bcel.internal.generic.L2D;
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
//    public FurnaceSettings furnaceSettings;
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
//    public boolean basicsSet = false;
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
        monitoredTags = new Hashtable<MonitoredDataItem, Tag>();
        topL2Zones = new LinkedHashMap<FceSection, L2DFHZone>();
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
//        enableSubscriptions(false);
        return retVal;
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

    public void startSpeedUpdater() {
        stripZone.startSpeedUpdater();
    }

    boolean canResetAccess = false; // resetting the application status in 'Level2.Internal'

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
            canResetAccess = (retVal.getDataStatus() == DataStat.Status.OK);
        }
        return retVal;
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

    Tag performanceChanged, performanceNoted;
    Tag processDataChanged, processDataNoted;

    public boolean createInternalZone() {
        boolean retVal = false;
        String processElement = "Internal.Performance";
        internalZone = new L2ParamGroup(this, "Internal");
        updaterChangeSub = source.createTMSubscription("Updater Status",new SubAliveListener(), new UpdaterChangeListener());
        logDebug("updaterChangeSub created");
        Tag[] performanceTags = {
                performanceChanged = new Tag(L2ParamGroup.Parameter.Performance, Tag.TagName.Ready, true, itIsRuntime),
                performanceNoted = new Tag(L2ParamGroup.Parameter.Performance, Tag.TagName.Noted, true, !itIsRuntime)};
        try {
            performanceStat = new ReadyNotedBothL2(source, equipment, processElement, performanceTags, updaterChangeSub);
            performanceStat.setReadWriteStat(!itIsRuntime);
        } catch (TagCreationException e) {
            showError("Some Tags could not be created: " + e.getMessage());
            return false;
//            e.printStackTrace();
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
        processListSub = source.createTMSubscription("Process List", new SubAliveListener(), new ProcessListListener());
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
            retVal = false;
        }
        return retVal;
    }

    boolean createL2Messages() {
        boolean retVal = false;
        logDebug("creating L2 Messages");
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
        furnaceDisplayZones = new Vector<L2DFHDisplay>();
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
//            updateUIDisplay();
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
            logInfo("Process List cleared");
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
//        FramedPanel jp = new FramedPanel();
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

    public boolean isRefPerformanceAvailable(OneStripDFHProcess dfhProc, double stripThick) {
        DataWithStatus<ChMaterial> chMat = dfhProc.getChMaterial(stripThick);
        return (performBase != null) && (chMat.valid) &&
                performBase.isRefPerformanceAvailable(dfhProc.getFullProcessID(),
                        chMat.getValue().name, dfhProc.tempDFHExit);
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

    public HeatExchProps getAirHeatExchProps() {
        return airRecu.getHeatExchProps(recuCounterFlow.isSelected());
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
        logTrace("total fuel = " + totFuel);
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
            logInfo("Facing some problem in reading furnace field data: " + fieldData.errMsg);
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
            logTrace("L2DFHFurnace.824: chEmmCorrectionFactor = " + refP.chEmmCorrectionFactor);
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
            resetChEmmissCorrectionFactor();
            logTrace("Trials in getOutputWithFurnaceStatus = " + trials);
        } else
            logInfo("L2DFHFurnace.847: Facing problem in creating calculation steps");
        logTrace("L2DFHFurnace.785: Nano seconds for calculation = " + (System.nanoTime() - stTimeNano));
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
//                    logTrace(String.format("nowExitTemp %3.2f, diff %3.4f, chEmmissCorrectionFactor %3.3f", nowExitTemp, diff, chEmmissCorrectionFactor));
                    if (Math.abs(diff) < tempAllowance)
                        done = true;
                    else {
//                        logTrace(String.format("exitTempRequired %3.3f, chInTemp %3.3f, nowExitTemp %3.3f",
//                                exitTempRequired, chInTemp, nowExitTemp));
                        chEmmissCorrectionFactor = chEmmissCorrectionFactor * (exitTempRequired - chInTemp) / (nowExitTemp - chInTemp);
                        if (chEmmissCorrectionFactor > 2 || chEmmissCorrectionFactor < 0.1)
                            break;
                    }
                }
                else {
                    showError("setEmmissFactorBasedOnFieldResults:" +
                            "processInFurnace returned with error!");
                    break;
                }
            }
            logTrace("setEmmissFactorBasedOnFieldResults:" +
                    "Trials in setEmmissFactorBasedOnFieldResults = " + trials);
        } else
            logInfo("Facing problem in creating calculation steps");
        logTrace("Emm Factor = " + chEmmissCorrectionFactor + ",  " +
                "L2DFHFurnace.915: Nano seconds for calculation = " + (System.nanoTime() - stTimeNano));
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
        logInfo("L2DFHFurnace.1002: Performance = " + perform);   // TODO tobe removed on RELEASE
        boolean retVal = false;
        boolean goAhead = true;
        boolean replace = false;
        if (perform != null && performBase != null) {
            logTrace("L2DFHFurnace.1007: performance looks ok");
            Performance similarOne = performBase.similarPerformance(perform);
            if (similarOne != null) {
                logTrace("L2DFHFurnace.1009: Similar performance already available");
                replace = true;
                goAhead = l2DFHeating.decide("Similar performance already available",
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
            logTrace("eval for Performance table is ok");
            try {
                eval.awaitThreadToExit();
                logTrace("eval for Performance table is completed");
                if (eval.healthyExit()) {
                    logTrace("eval for Performance table had healthy exit");
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
            logInfo("eval for Performance table is null");
        return retVal;
    }

    public DataWithStatus<OneStripDFHProcess> createNewNewProcess(double stripExitT, double thick, double width,
                                                                  double speed, String baseProcessName) {
        return l2DFHeating.dfhProcessList.addFieldProcess(baseProcessName, stripExitT, thick, width, speed);
    }

    synchronized void resetFieldDataBeingHandled() {
        logTrace("Trying to make fieldDataIsBeingHandled OFF");
        fieldDataIsBeingHandled.set(false);
        logTrace("fieldDataIsBeingHandled is made OFF");
        l2DFHeating.enablePerfMenu(true);
    }

    synchronized void setFieldDataBeingHandled() {
        logTrace("Trying to make fieldDataIsBeingHandled ON");
        fieldDataIsBeingHandled.set(true);
        logTrace("fieldDataIsBeingHandled is made ON");
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
                    logTrace("New " + newStrip);
                    L2ZonalFuelProfile zFP = new L2ZonalFuelProfile(newStrip.refP.getPerformanceTable(),
                            furnaceSettings.fuelCharSteps, l2DFHeating);
                    if (zFP.prepareFuelTable(newStrip.width, newStrip.thickness)) {
                        DataWithStatus speedBasedOnFuel = getSpeedWithFurnaceFuelStatus(zFP) ;
                        DataStat.Status speedOnFuelStat = speedBasedOnFuel.getStatus();
                        if (speedOnFuelStat == DataStat.Status.OK)
                            logTrace("Speed Based On Fuel = " + speedBasedOnFuel.getValue() + "m/min");
                        else
                            logInfo("Error in Speed based on Fuel :" + speedBasedOnFuel.getErrorMessage());
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
                            boolean considerFieldZonalTemperatures = true;
                            setCurveSmoothening(false);
                            DataWithStatus<Double> emmStat = null;
                            logTrace("l2DFHFurnace.1211: theFieldResults.production.chEmmissCorrectionFactor = " +
                                    theFieldResults.production.chEmmissCorrectionFactor);   // TOD to be removed on RELEASE
                            if (considerFieldZonalTemperatures) {
                                emmStat = setEmmissFactorBasedOnFieldResults(theFieldResults);
                            }

//                            logInfo("l2DFHFurnace.1218: productionData = " + productionData);   // TOD to be removed on RELEASE
                            if (productionData != null)
                                logTrace("l2DFHFurnace.1219: productionData.chEmmissCorrectionFactor = " + productionData.chEmmissCorrectionFactor);   // TOD to be removed on RELEASE

                            if (!considerFieldZonalTemperatures || (emmStat.getDataStatus() == DataStat.Status.OK)) {
                                if (!considerFieldZonalTemperatures || fillChInTempProfile()) {

                                    bConsiderPresetChInTempProfile = considerFieldZonalTemperatures;
//                                    l2DFHeating.showMessage("Field Performance", "Evaluating from Model" );
                                    logTrace("L2DFHFurnace.1226: Evaluating From Model");
                                    if (productionData != null)
                                        logInfo("l2DFHFurnace.1228: productionData.chEmmissCorrectionFactor = " + productionData.chEmmissCorrectionFactor);
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
                                                boolean proceed = true;
                                                if (proceed && theFieldResults.adjustForFieldResults()) { // was (adjustForFieldResults()) {
                                                logTrace("L2DFHFurnace.1237: Recalculating after fuel flow adjustments");
                                                    FceEvaluator eval2 = l2DFHeating.calculateFce(false, null, "With Field Fuel Flow"); // without reset the loss Factors
                                                    if (eval2 != null) {
                                                        eval2.awaitThreadToExit();
                                                        logTrace("L2DFHFurnace.1241: eval2 completed" );
                                                        if (eval2.healthyExit()) {
                                                            logTrace("L2DFHFurnace.1243: eval2 had healthy exit" );
                                                            if (addFieldBasedPerformanceToPerfBase()) {
                                                                logInfo("l2DFHFurnace.1280: productionData.chEmmissCorrectionFactor = " + productionData.chEmmissCorrectionFactor);
                                                                l2DFHeating.showMessage("Field Performance", "Save updated Performance to file from Performance Menu" );
                                                            }
                                                            else {
                                                                addErrorMsg("Performance Data is rejected Level2 user");
                                                                showError("Performance Data is rejected Level2 user");
                                                            }
                                                        } else {
                                                            if (eval2.isAborted())
                                                                showError("Aborted from Calculation with Fuel Corrections" );
                                                            addErrorMsg("calculation was aborted by Level2 user");
                                                        }
                                                    } else
                                                        showError("Unable ot proceed with Fuel Corrections" );
                                                    resetLossFactor();
                                                    logTrace("lossFactors reset" );
                                                }
                                            } else {
                                                if (eval1.isAborted())
                                                    showError("Aborted from Model Calculation" );
                                                addErrorMsg("calculation was aborted by Level2 user");
                                            }
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    } else
                                        showError("Unable to do Calculation from Model" );
                                    resetLossFactor();
                                }
                            } else
                                showError("Facing some problem in evaluating strip emissivity factor" );
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
                        logInfo("The process " + theProcess + " was removed since Performance calculation failed");
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
            errMsg.append("\n" + msg);
            bErrMSg = true;
        }

        void addInfoMsg(String msg) {
            infoMsg.append("\n" + msg);
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
                if (fieldDataParams.isNewData(theTag))   // the data will be already read if new data
                    if (theTag == tagFieldDataReady)
                        handleFieldData();
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
//                logTrace("ProcessListListener heard something");
            }
        }
    }

    class FuelCharListener extends L2SubscriptionListener {
         @Override
         public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
             if (l2DFHeating.isL2SystemReady()) {
                 Tag theTag = monitoredTags.get(monitoredDataItem);
                 fuelCharParams.isNewData(theTag);
//                 logTrace("FuelCharListener heard something");
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
                        logTrace("Process data Changed");
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
