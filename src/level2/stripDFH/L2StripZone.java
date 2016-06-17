package level2.stripDFH;

import TMopcUa.ProcessValue;
import TMopcUa.TMSubscription;
import TMopcUa.TMuaClient;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionAliveListener;
import directFiredHeating.accessControl.L2AccessControl;
import level2.applications.L2DFHeating;
import directFiredHeating.process.OneStripDFHProcess;
import level2.common.*;
import mvUtils.display.FramedPanel;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.StatusWithMessage;
import mvUtils.math.DoubleMV;
import mvUtils.math.DoubleWithStatus;
import org.opcfoundation.ua.builtintypes.DataValue;
import performance.stripFce.Performance;
import performance.stripFce.StripProcessAndSize;

import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Vector;

/**
 * User: M Viswanathan
 * Date: 06-Apr-16
 * Time: 5:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2StripZone extends L2ParamGroup {
    L2DFHeating l2DFHeating;
    L2DFHFurnace l2Furnace;
    Hashtable<MonitoredDataItem, Tag> monitoredTags;
    TMSubscription stripSub;
    Tag[] tagsSpeedCheck;
    Tag[] tagsStripDataNow;
    Tag[] tagsStripDataNext;
    Tag[] tagsStripTemperature;
    Tag[] tagsStripSpeed;
    Tag[] tagsStripStatus;
    Tag tagNextStripDataReady;
    Tag tagNowStripDataReady;
    ReadyNotedParam paramStripNowSize;
    ReadyNotedParam paramStripNextSize;
    L2ZoneParam paramStripNowTemperature;
    L2ZoneParam paramStripNowSpeed;
    L2ZoneParam paramStripSpeedCheck;
    L2ZoneParam paramStripNowStatus;
    Vector<ReadyNotedParam> readyNotedParamList = new Vector<ReadyNotedParam>();
    String errorLocation;
    boolean speedUpdaterThresdOn = false;
    boolean updateSpeed = false;

    public L2StripZone(L2DFHeating l2DFHeating, String zoneName, String descriptiveName) throws TagCreationException {
        super(l2DFHeating.l2Furnace, zoneName, descriptiveName);
        this.l2DFHeating = l2DFHeating;
        this.l2Furnace = l2DFHeating.l2Furnace;
        monitoredTags = new Hashtable<MonitoredDataItem, Tag>();
        createStripParam();
        createNowStripTempProcessDisplay();
        createNowStripTempLevel2Display();
        createNowStripSpeedProcessDisplay();
        createStripSpeedCheckProcessDisplay();
        createNowStripDataProcessDisplay();
        createNowStripDataLevel2Display();
        createNextStripDataProcessDisplay();
        createNextStripDataLevel2Display();
    }

    public boolean IsSpeedUpdaterOn() {
        return speedUpdaterThresdOn;
    }

    boolean createStripParam() throws TagCreationException {
        boolean retVal = false;
        errorLocation = "";
        TMuaClient source = l2Interface.source();
        String widthFmt = "#,##0";
        String thicknessFmt = " 0.000";
        String temperatureFmt = "#,###";
        String lengthFmt = "#,###";
        String speedFmt = "#,###";
        String cvFmt = "###";
        blk:
        {
            stripSub = source.createTMSubscription("Strip Data", new SubAliveListener(), new StripListener());
            setSubscription(stripSub);
            Tag[] tags1 = {
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Process, false, false),
                    (tagNowStripDataReady = new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Ready, false, true)),
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Thick, false, false, thicknessFmt),
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Width, false, false, widthFmt),
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Noted, false, true),

                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Ready, true, false),
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.SpeedMax, true, false, speedFmt),
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.SpeedNow, true, false, speedFmt),
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Temperature, true, false, temperatureFmt),
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Noted, true, false)};
            tagsStripDataNow = tags1;
            errorLocation = "Strip size Now tags";   // for error messages
            paramStripNowSize = new ReadyNotedParam(source, l2Interface.equipment(), "Strip.Now", tagsStripDataNow, stripSub);
            readyNotedParamList.add(paramStripNowSize);
            addOneParameter(L2ParamGroup.Parameter.Now, paramStripNowSize);
            noteMonitoredTags(tagsStripDataNow);

            Tag[] tags2 = {
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Process, false, false),
                    (tagNextStripDataReady = new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Ready, false, true)),
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Thick, false, false, thicknessFmt),
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Width, false, false, widthFmt),
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Noted, false, true),

                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Ready, true, false),
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.SpeedMax, true, false, speedFmt),
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.SpeedNow, true, false, speedFmt),
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Temperature, true, false, temperatureFmt),
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Noted, true, false)};
            tagsStripDataNext = tags2;
            errorLocation = "Strip size Next tags";
            paramStripNextSize = new ReadyNotedParam(source, l2Interface.equipment(), "Strip.Next", tagsStripDataNext, stripSub);
            readyNotedParamList.add(paramStripNextSize);
            addOneParameter(L2ParamGroup.Parameter.Next, paramStripNextSize);
            noteMonitoredTags(tagsStripDataNext);

            String[] modeStr = {"Strip", "Zonal"};
            Tag[] tags3 = {
                    new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.SP, false, false, temperatureFmt),
                    new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.PV, false, false, temperatureFmt),
                    new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.CV, false, false, cvFmt),
                    new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.Mode, false, false, modeStr)};
            tagsStripTemperature = tags3;
            errorLocation = "Strip Temperature tags";
            paramStripNowTemperature = addOneParameter(L2ParamGroup.Parameter.Temperature, tagsStripTemperature);
            noteMonitoredTags(tagsStripTemperature);

            Tag[] tags4 = {
                    new Tag(L2ParamGroup.Parameter.Speed, Tag.TagName.SP, false, false, speedFmt),
                    new Tag(L2ParamGroup.Parameter.Speed, Tag.TagName.PV, false, false, speedFmt)};
            tagsStripSpeed = tags4;
            errorLocation = "Strip Speed tags";
            paramStripNowSpeed =  addOneParameter(L2ParamGroup.Parameter.Speed, tagsStripSpeed);
            noteMonitoredTags(tagsStripSpeed);

            Tag[] tags5 = {
                    new Tag(Parameter.Status, Tag.TagName.Length, false, false, lengthFmt)};
            tagsStripStatus = tags5;
            errorLocation = "Strip Status tags";
            paramStripNowStatus =  addOneParameter(L2ParamGroup.Parameter.Status, tagsStripStatus);
            noteMonitoredTags(tagsStripStatus);

            Tag[] tags6 = {
                    new Tag(Parameter.SpeedCheck, Tag.TagName.Enabled, false, true),
                    new Tag(Parameter.SpeedCheck, Tag.TagName.Running, true, false),
                    new Tag(Parameter.SpeedCheck, Tag.TagName.Msg, true, false)};
            tagsSpeedCheck = tags6;
            errorLocation = "Strip SpeedCheck tags";
            paramStripSpeedCheck =  addOneParameter(Parameter.SpeedCheck, tagsSpeedCheck);
            noteMonitoredTags(tagsSpeedCheck);

            retVal = true;
        }
        return retVal;
    }

    void noteMonitoredTags(Tag[] tags) {
        for (Tag tag : tags)
            if (tag.isMonitored())
                monitoredTags.put(tag.getMonitoredDataItem(), tag);
    }

    public void initForLevel2Operation() {
        for (ReadyNotedParam p : readyNotedParamList)
            p.initStatus();
        paramStripSpeedCheck.setValue(Tag.TagName.Running, false);
        paramStripSpeedCheck.setValue(Tag.TagName.Msg, "");
    }

    public StripProcessAndSize getNowStripData() {
        double stripWidth = DoubleMV.round(getValue(L2ParamGroup.Parameter.Now, Tag.TagName.Width).floatValue, 3) / 1000;
        double stripThick = DoubleMV.round(getValue(L2ParamGroup.Parameter.Now, Tag.TagName.Thick).floatValue, 3) / 1000;
        String process = getValue(L2ParamGroup.Parameter.Now, Tag.TagName.Process).stringValue;
//        if (l2Furnace.level2Enabled)
//            setValue(L2ParamGroup.Parameter.Now, Tag.TagName.Noted, true);
        return new StripProcessAndSize(process, stripWidth, stripThick);
    }

    public StripProcessAndSize setNowStripAction(StatusWithMessage status) {
        StripProcessAndSize theStrip = null;
        if (l2Furnace.level2Enabled) {
            theStrip = getNowStripData();
            l2Furnace.logTrace("Running strip with Process:" + theStrip.process);
            OneStripDFHProcess oneProcess = l2DFHeating.getStripDFHProcess(theStrip.process);
            if (oneProcess != null) {
                Performance refP = l2Furnace.getBasePerformance(oneProcess, theStrip.thickness);
                if (refP != null) {
                    theStrip.refP = refP;
                    double output = l2Furnace.getOutputWithFurnaceTemperatureStatus(refP, theStrip.width, theStrip.thickness);
                    l2Furnace.logTrace("Running Strip capacity Based On temperature= " + (output / 1000) + "t/h");
                    if (output > 0) {
                        DoubleWithStatus speedData = oneProcess.getRecommendedSpeed(output, theStrip.width, theStrip.thickness);
                        StatusWithMessage.DataStat speedStatus = speedData.getDataStatus();
                        if (speedStatus != StatusWithMessage.DataStat.WithErrorMsg) {
                            ProcessValue responseSpeed = setValue(L2ParamGroup.Parameter.Now, Tag.TagName.SpeedNow,
                                    (float)(speedData.getValue() / 60));
                            if (responseSpeed.valid) {
                                ProcessValue responseSpeedLimit = setValue(L2ParamGroup.Parameter.Now, Tag.TagName.SpeedMax,
                                        (float) ((oneProcess.getLimitSpeed(theStrip.thickness) / 60)));
                            }else
                                status.setErrorMessage("Running Strip: Unable to set recommended speed" + responseSpeed.errorMessage);
                        } else
                            status.setErrorMessage("Running Strip: Unable to get recommended speed: " + speedData.getErrorMessage());
                    } else
                        status.setErrorMessage("Running Strip: Unable to calculate recommended capacity from reference");
                }
                else
                    status.setErrorMessage("Running Strip: Unable to get Reference performance in the database");
            } else
                status.setErrorMessage("Running Strip: Process " + theStrip.process + " not available");
        }
        else
            status.setErrorMessage("Running Strip: Level2 is not enabled");
        return theStrip;
    }

    public StripProcessAndSize getNextStripData() {
        double stripWidth = DoubleMV.round(getValue(L2ParamGroup.Parameter.Next, Tag.TagName.Width).floatValue, 3) / 1000;
        double stripThick = DoubleMV.round(getValue(L2ParamGroup.Parameter.Next, Tag.TagName.Thick).floatValue, 3) / 1000;
        String process = getValue(L2ParamGroup.Parameter.Next, Tag.TagName.Process).stringValue;
        if (l2Furnace.level2Enabled)
            setValue(L2ParamGroup.Parameter.Next, Tag.TagName.Noted, true);
        return new StripProcessAndSize(process, stripWidth, stripThick);
    }

    public StripProcessAndSize setNextStripAction(StatusWithMessage status) {
        StripProcessAndSize theStrip = null;
        if (l2Furnace.level2Enabled) {
            theStrip = getNextStripData();
            l2Furnace.logTrace("new strip with Process:" + theStrip.process);
            OneStripDFHProcess oneProcess = l2DFHeating.getStripDFHProcess(theStrip.process);
            if (oneProcess != null) {
                Performance refP = l2Furnace.getBasePerformance(oneProcess, theStrip.thickness);
                if (refP != null) {
                    theStrip.refP = refP;
                    double output = l2Furnace.getOutputWithFurnaceTemperatureStatus(refP, theStrip.width, theStrip.thickness);
                    l2Furnace.logTrace("capacity Based On temperature= " + (output / 1000) + "t/h");
                    if (output > 0) {
                        DoubleWithStatus speedData = oneProcess.getRecommendedSpeed(output, theStrip.width, theStrip.thickness);
                        StatusWithMessage.DataStat speedStatus = speedData.getDataStatus();
                        if (speedStatus != StatusWithMessage.DataStat.WithErrorMsg) {
                            ProcessValue responseSpeed = setValue(L2ParamGroup.Parameter.Next, Tag.TagName.SpeedNow,
                                    (float)(speedData.getValue() / 60));
                            if (responseSpeed.valid) {
                                ProcessValue responseSpeedLimit = setValue(L2ParamGroup.Parameter.Next, Tag.TagName.SpeedMax,
                                        (float) ((oneProcess.getLimitSpeed(theStrip.thickness) / 60)));
                                if (responseSpeedLimit.valid) {
                                    ProcessValue responseTempSp = setValue(Parameter.Next, Tag.TagName.Temperature,
                                            (float) (oneProcess.tempDFHExit));
                                    if (responseTempSp.valid) {
                                        setValue(L2ParamGroup.Parameter.Next, Tag.TagName.Ready, true);
                                        if (speedStatus == StatusWithMessage.DataStat.WithInfoMsg)
                                            status.setInfoMessage(speedData.getInfoMessage());
                                    } else
                                        status.setErrorMessage("Unable to set Strip Exit Temperature : " + responseTempSp.errorMessage);
                                } else
                                    status.setErrorMessage("Unable to set Strip Speed Limit" + responseSpeedLimit.errorMessage);
                            }else
                                status.setErrorMessage("Unable to set recommended speed" + responseSpeed.errorMessage);
                        } else
                            status.setErrorMessage("Unable to get recommended speed: " + speedData.getErrorMessage());
                    } else
                        status.setErrorMessage("Unable to calculate recommended capacity from reference");
                }
                else
                    status.setErrorMessage("Unable to get Reference performance in the database");
            } else
                status.setErrorMessage("Process " + theStrip.process + " not available");
        }
        else
            status.setErrorMessage("Level2 is not enabled");
        return theStrip;
    }

    public JComponent nowStripProcessTempPanel;
    public JComponent nowStripLevel2TempPanel;
    JComponent nowStripProcessSpeedPanel;
    JComponent nowStripProcessDataPanel;
    JComponent nowStripLevel2DataPanel;
    JComponent nextStripProcessDataPanel;
    JComponent nextStripLevel2DataPanel;
    JComponent stripSpeedCheckPanel;
    JComponent stripSpeedCheckLevel2Panel;

    public JComponent stripProcessPanel() {
        JPanel outerP = new JPanel(new BorderLayout()) ;
        outerP.add(stripNowProcessPanel(), BorderLayout.WEST);
        outerP.add(stripNextProcessPanel(), BorderLayout.EAST);
        return outerP;
    }

    public JComponent stripLevel2Panel() {
        JPanel outerP = new JPanel(new BorderLayout()) ;
        outerP.add(stripNowLevel2Panel(), BorderLayout.WEST);
        outerP.add(stripNextLevel2Panel(), BorderLayout.EAST);
        return outerP;
    }

    public JComponent stripNowProcessPanel() {
        FramedPanel innerP = new FramedPanel(new BorderLayout());
        FramedPanel jp = new FramedPanel();
//        jp.add(nowStripProcessTempPanel);
        jp.add(nowStripProcessSpeedPanel);
        jp.add(nowStripProcessDataPanel);
        jp.add(stripSpeedCheckPanel);
        JPanel titleP = new JPanel();
        titleP.add(new JLabel("Strip in Process"));
        innerP.add(titleP, BorderLayout.NORTH);
        innerP.add(jp, BorderLayout.CENTER);
        return innerP;
    }

    public JComponent stripNowLevel2Panel() {
        FramedPanel innerP = new FramedPanel(new BorderLayout());
        FramedPanel jp = new FramedPanel();
        jp.add(nowStripLevel2DataPanel);
//        jp.add(stripSpeedCheckPanel);
        JPanel titleP = new JPanel();
        titleP.add(new JLabel("Strip in Process"));
        innerP.add(titleP, BorderLayout.NORTH);
        innerP.add(jp, BorderLayout.CENTER);
        return innerP;
    }

    public JComponent stripNextProcessPanel() {
        FramedPanel innerP = new FramedPanel(new BorderLayout());
        FramedPanel jp = new FramedPanel();
        jp.add(nextStripProcessDataPanel);
        JPanel titleP = new JPanel();
        titleP.add(new JLabel("Strip in Waiting"));
        innerP.add(titleP, BorderLayout.NORTH);
        innerP.add(jp, BorderLayout.CENTER);
        return innerP;
    }

    public JComponent stripNextLevel2Panel() {
        FramedPanel innerP = new FramedPanel(new BorderLayout());
        FramedPanel jp = new FramedPanel();
        jp.add(nextStripLevel2DataPanel);
        JPanel titleP = new JPanel();
        titleP.add(new JLabel("Strip in Waiting"));
        innerP.add(titleP, BorderLayout.NORTH);
        innerP.add(jp, BorderLayout.CENTER);
        return innerP;
    }

    public JComponent createNowStripTempProcessDisplay() {
        nowStripProcessTempPanel = new FramedPanel();
        MultiPairColPanel mp = new MultiPairColPanel("DFH Exit: Temperature");
        Tag t =  paramStripNowTemperature.getProcessTag(Tag.TagName.SP);
        mp.addItemPair(t.toString(), t.displayComponent());
        t =  paramStripNowTemperature.getProcessTag(Tag.TagName.PV);
        mp.addItemPair(t.toString(), t.displayComponent());
        t =  paramStripNowTemperature.getProcessTag(Tag.TagName.Mode);
        mp.addItemPair(t.toString(), t.displayComponent());
        t =  paramStripNowTemperature.getProcessTag(Tag.TagName.CV);
        mp.addItemPair(t.toString(), t.displayComponent());
        nowStripProcessTempPanel.add(mp);
        return nowStripProcessTempPanel;
    }

    public JComponent createNowStripTempLevel2Display() {
        nowStripLevel2TempPanel = new FramedPanel();
        MultiPairColPanel mp = new MultiPairColPanel("DFH Exit: Strip");
        Tag t =  paramStripNowSize.getLevel2Tag(Tag.TagName.Temperature);
        mp.addItemPair(t.toString(), t.displayComponent());
        nowStripLevel2TempPanel.add(mp);
        return nowStripLevel2TempPanel;
    }

    JComponent createNowStripSpeedProcessDisplay() {
        nowStripProcessSpeedPanel = new FramedPanel();
        MultiPairColPanel mp = new MultiPairColPanel("Speed");
        Tag t = paramStripNowSpeed.getProcessTag(Tag.TagName.SP);
        mp.addItemPair(t.toString(), t.displayComponent());
        t = paramStripNowSpeed.getProcessTag(Tag.TagName.PV);
        mp.addItemPair(t.toString(), t.displayComponent());
        t = paramStripNowStatus.getProcessTag(Tag.TagName.Length);
        mp.addItemPair(t.toString(), t.displayComponent());
        nowStripProcessSpeedPanel.add(mp);
        return nowStripProcessSpeedPanel;
    }

    JComponent createStripSpeedCheckProcessDisplay() {
        stripSpeedCheckPanel = new FramedPanel();
        MultiPairColPanel mp = new MultiPairColPanel("Strip Speed Check");
        Tag t = paramStripSpeedCheck.getProcessTag(Tag.TagName.Enabled);
        mp.addItemPair(t.toString(), t.displayComponent());
        stripSpeedCheckPanel.add(mp);
        return stripSpeedCheckPanel;
    }

    JComponent createNowStripDataProcessDisplay() {
        nowStripProcessDataPanel = new FramedPanel();
        MultiPairColPanel mp = new MultiPairColPanel("Process and Size");
        Tag t = paramStripNowSize.getProcessTag(Tag.TagName.Process);
        mp.addItemPair(t.toString(), t.displayComponent());
        t = paramStripNowSize.getProcessTag(Tag.TagName.Thick);
        mp.addItemPair(t.toString(), t.displayComponent());
        t = paramStripNowSize.getProcessTag(Tag.TagName.Width);
        mp.addItemPair(t.toString(), t.displayComponent());
        nowStripProcessDataPanel.add(mp);
        return nowStripProcessDataPanel;
    }

    JComponent createNowStripDataLevel2Display() {
        nowStripLevel2DataPanel = new FramedPanel();
        MultiPairColPanel mp = new MultiPairColPanel("Recommendations");
        Tag t = paramStripNowSize.getLevel2Tag(Tag.TagName.SpeedNow);
        mp.addItemPair(t.toString(), t.displayComponent());
        t = paramStripNowSize.getLevel2Tag(Tag.TagName.SpeedMax);
        mp.addItemPair(t.toString(), t.displayComponent());
        nowStripLevel2DataPanel.add(mp);
        return nowStripLevel2DataPanel;
    }

    JComponent createNextStripDataProcessDisplay() {
        nextStripProcessDataPanel = new FramedPanel();
        MultiPairColPanel mp = new MultiPairColPanel("Process and Size");
        Tag t = paramStripNextSize.getProcessTag(Tag.TagName.Process);
        mp.addItemPair(t.toString(), t.displayComponent());
        t = paramStripNextSize.getProcessTag(Tag.TagName.Thick);
        mp.addItemPair(t.toString(), t.displayComponent());
        t = paramStripNextSize.getProcessTag(Tag.TagName.Width);
        mp.addItemPair(t.toString(), t.displayComponent());
        nextStripProcessDataPanel.add(mp);
        return nextStripProcessDataPanel;
    }

    JComponent createNextStripDataLevel2Display() {
        nextStripLevel2DataPanel = new FramedPanel();
        MultiPairColPanel mp = new MultiPairColPanel("Recommendations");
        Tag t = paramStripNextSize.getLevel2Tag(Tag.TagName.Temperature);
        mp.addItemPair(t.toString(), t.displayComponent());
        t = paramStripNextSize.getLevel2Tag(Tag.TagName.SpeedNow);
        mp.addItemPair(t.toString(), t.displayComponent());
        t = paramStripNextSize.getLevel2Tag(Tag.TagName.SpeedMax);
        mp.addItemPair(t.toString(), t.displayComponent());
        nextStripLevel2DataPanel.add(mp);
        return nextStripLevel2DataPanel;
    }


    public void updateProcessDisplay() {
        updateDisplay(tagsStripTemperature);
        updateDisplay(tagsStripSpeed);
        updateDisplay(tagsStripDataNow);
        updateDisplay(tagsStripDataNext);
        updateDisplay(tagsStripStatus);
        updateDisplay(tagsSpeedCheck);
    }

    public void updateL2Display() {

    }

    void updateDisplay(Tag[] tags) {
        for (Tag t: tags)
            t.updateUI();
    }

    public void prepareForDisconnection()throws ServiceException {
        stripSub.removeItems();
    }

    void setSpeedCheckMessge(String msg) {
        paramStripSpeedCheck.setValue(Tag.TagName.Msg, msg);
    }

    Thread speedThread; // for updating recommended at regular intervals based on furnace

    public void startSpeedUpdater() {
        SpeedUpdater speedUpdater = new SpeedUpdater();
        speedThread = new Thread(speedUpdater);
        speedThread.start();
        l2Furnace.logTrace("Strip speed updater started ...");
    }

    public void stopSpeedUpdater() {
        if (speedUpdaterThresdOn) {
            speedUpdaterThresdOn = false;
            try {
                if (speedThread != null) speedThread.join(speedNapInterval * 5);
            } catch (InterruptedException e) {
                l2Furnace.logError("Problem in stopping Strip Speed Updater");
                e.printStackTrace();
            }
        }
    }

    long speedNapInterval = 2000; // 2 sec
    int nNaps = 5;  // TODO the valeu tobe set as 15 or 30

    class SpeedUpdater implements Runnable {
        public void run() {
            speedUpdaterThresdOn = true;
            while (speedUpdaterThresdOn) {
                if (updateSpeed) {
                    int count = 5;
                    boolean gotIt = false;
                    while (--count > 0) {
                        if (!l2Furnace.isProcessDataBeingUsed()) {
                            l2Furnace.stripSpeedRoutineON.set(true);
                            gotIt = true;
                            StatusWithMessage nowStripStatusWithMsg = new StatusWithMessage();
                            setNowStripAction(nowStripStatusWithMsg);
                            if (nowStripStatusWithMsg.getDataStatus() == StatusWithMessage.DataStat.OK) {
                                setSpeedCheckMessge("OK");
                                paramStripSpeedCheck.setValue(Tag.TagName.Running, true);
                            }
                            else {
                                String msg = nowStripStatusWithMsg.getErrorMessage();
                                setSpeedCheckMessge(msg);
                                l2Furnace.logInfo(msg);
                                paramStripSpeedCheck.setValue(Tag.TagName.Running, false);
                            }

                            break;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!gotIt)
                        l2Furnace.logInfo("Unable to process Strip speed routine");
                    l2Furnace.stripSpeedRoutineON.set(false);
                }
                try {  // multiple intervals to enable faster exit
                    for (int n = 0; n < nNaps; n++) {
                        if (speedUpdaterThresdOn)
                            Thread.sleep(speedNapInterval);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    class StripListener extends L2SubscriptionListener {
        @Override
        public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
            L2AccessControl.AccessLevel accessLevel = l2DFHeating.accessLevel;
            if (l2DFHeating.isL2SystemReady() && (accessLevel == L2AccessControl.AccessLevel.RUNTIME)) {
                Tag theTag = monitoredTags.get(monitoredDataItem);
                if (paramStripNextSize.isItAMember(theTag)) {
                    if (paramStripNextSize.isNewData(theTag))   // the data will be already read if new data
                        if (theTag == tagNextStripDataReady)
                            l2Furnace.handleNextStrip();
                }
                else if (paramStripSpeedCheck.isItAMember(theTag)) {
                    updateSpeed = paramStripSpeedCheck.getValue(Tag.TagName.Enabled).booleanValue;
                    paramStripSpeedCheck.setValue(Tag.TagName.Running, updateSpeed);
                    setSpeedCheckMessge((updateSpeed) ? "Started" : "Disabled from Level1");
                }
            }
        }
    }

    void trace(String msg) {
        System.out.println("L2StripZone: " + msg);
    }

    class SubAliveListener implements SubscriptionAliveListener {     // TODO This is common dummy class used everywhere to be made proper
        public void onAlive(Subscription s) {
//            l2DFHeating.showMessage("" + s + String.format(
//                    "%tc Subscription alive: ID=%d lastAlive=%tc",
//                    Calendar.getInstance(), s.getSubscriptionId().getValue(),
//                    s.getLastAlive()));
        }

        public void onTimeout(Subscription s) {
            l2DFHeating.showMessage(String.format("In Strip Zone %s timeout at %tc, last alive at %tc ",
                    s, Calendar.getInstance(), s.getLastAlive()));
        }
    }
}
