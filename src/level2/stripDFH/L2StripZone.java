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
    L2ZoneParam paramStripNowStatus;
    Vector<ReadyNotedParam> readyNotedParamList = new Vector<ReadyNotedParam>();
    String errorLocation;

    public L2StripZone(L2DFHeating l2DFHeating, String zoneName, String descriptiveName) throws TagCreationException {
        super(l2DFHeating.l2Furnace, zoneName, descriptiveName);
        this.l2DFHeating = l2DFHeating;
        this.l2Furnace = l2DFHeating.l2Furnace;
        monitoredTags = new Hashtable<MonitoredDataItem, Tag>();
        createStripParam();
        createNowStripTempProcessDisplay();
        createNowStripSpeedProcessDisplay();
        createNowStripDataProcessDisplay();
        createNextStripDataProcessDisplay();
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
                    new Tag(L2ParamGroup.Parameter.Status, Tag.TagName.Length, false, false, lengthFmt)};
            tagsStripStatus = tags5;
            errorLocation = "Strip Status tags";
            paramStripNowStatus =  addOneParameter(L2ParamGroup.Parameter.Status, tagsStripStatus);
            noteMonitoredTags(tagsStripStatus);
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
    }


    public StripProcessAndSize getNewStripData() {
        double stripWidth = DoubleMV.round(getValue(L2ParamGroup.Parameter.Next, Tag.TagName.Width).floatValue, 3) / 1000;
        double stripThick = DoubleMV.round(getValue(L2ParamGroup.Parameter.Next, Tag.TagName.Thick).floatValue, 3) / 1000;
        String process = getValue(L2ParamGroup.Parameter.Next, Tag.TagName.Process).stringValue;
        if (l2Furnace.level2Enabled)
            setValue(L2ParamGroup.Parameter.Next, Tag.TagName.Noted, true);
        return new StripProcessAndSize(process, stripWidth, stripThick);
    }

    public StripProcessAndSize setNewStripAction(StatusWithMessage status) {
        StripProcessAndSize theStrip = null;
        if (l2Furnace.level2Enabled) {
            theStrip = getNewStripData();
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
    JComponent nowStripProcessSpeedPanel;
    JComponent nowStripProcessDataPanel;
    JComponent nextStripProcessDataPanel;

    public JComponent stripProcessPanel() {
        JPanel outerP = new JPanel(new BorderLayout()) ;
        outerP.add(stripNowProcessPanel(), BorderLayout.WEST);
        outerP.add(stripNextProcessPanel(), BorderLayout.EAST);
        return outerP;
    }

    public JComponent stripNowProcessPanel() {
        FramedPanel innerP = new FramedPanel(new BorderLayout());
        FramedPanel jp = new FramedPanel();
//        jp.add(nowStripProcessTempPanel);
        jp.add(nowStripProcessSpeedPanel);
        jp.add(nowStripProcessDataPanel);
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
//    public JComponent stripNextProcessPanel() {
//
//    }
//
//    public JComponent stripNowL2Panel() {
//
//    }
//
//    public JComponent stripNextL2Panel() {
//
//    }

    public void updateProcessDisplay() {
        updateDisplay(tagsStripTemperature);
        updateDisplay(tagsStripSpeed);
        updateDisplay(tagsStripDataNow);
        updateDisplay(tagsStripDataNext);
        updateDisplay(tagsStripStatus);
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

    class StripListener extends L2SubscriptionListener {
        @Override
        public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
            L2AccessControl.AccessLevel accessLevel = l2DFHeating.accessLevel;
            if (l2DFHeating.isL2SystemReady() && (accessLevel == L2AccessControl.AccessLevel.RUNTIME)) {
                Tag theTag = monitoredTags.get(monitoredDataItem);
                if (paramStripNextSize.isNewData(theTag))   // the data will be already read if new data
                    if (theTag == tagNextStripDataReady)
                        l2Furnace.handleNewStrip();
            }
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
            l2DFHeating.showMessage(String.format("In Strip Zone %s timeout at %tc, last alive at %tc ",
                    s, Calendar.getInstance(), s.getLastAlive()));
        }
    }
}
