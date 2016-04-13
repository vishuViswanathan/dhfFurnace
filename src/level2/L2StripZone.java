package level2;

import TMopcUa.ProcessValue;
import TMopcUa.TMSubscription;
import TMopcUa.TMuaClient;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionAliveListener;
import level2.common.*;
import mvUtils.display.StatusWithMessage;
import mvUtils.math.DoubleMV;
import mvUtils.math.DoubleWithStatus;
import org.opcfoundation.ua.builtintypes.DataValue;
import performance.stripFce.Performance;
import performance.stripFce.StripAction;
import performance.stripFce.StripProcessAndSize;

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
    Tag tagNextStripDataReady;
    Tag tagNowStripDataReady;
    ReadyNotedParam l2StripSizeNow;
    ReadyNotedParam l2StripSizeNext;
    Vector<ReadyNotedParam> readyNotedParamList = new Vector<ReadyNotedParam>();
    String errorLocation;

    public L2StripZone(L2DFHeating l2DFHeating, String zoneName, String descriptiveName) throws TagCreationException {
        super(l2DFHeating.l2Furnace, zoneName, descriptiveName);
        this.l2DFHeating = l2DFHeating;
        this.l2Furnace = l2DFHeating.l2Furnace;
        monitoredTags = new Hashtable<MonitoredDataItem, Tag>();
        createStripParam();
    }

    boolean createStripParam() throws TagCreationException {
        boolean retVal = false;
        errorLocation = "";
        TMuaClient source = l2Interface.source();
        blk:
        {
            stripSub = source.createTMSubscription("Strip Data", new SubAliveListener(), new StripListener());
            Tag[] stripDataNowTags = {
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Process, false, false),
                    (tagNowStripDataReady = new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Ready, false, true)),
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Thick, false, false),
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Width, false, false),
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Noted, false, true),

                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Ready, true, false),
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.SpeedMax, true, false),
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.SpeedNow, true, false),
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Temperature, true, false),
                    new Tag(L2ParamGroup.Parameter.Now, Tag.TagName.Noted, true, false)};
            errorLocation = "Strip size Now tags";   // for error messages
            l2StripSizeNow = new ReadyNotedParam(source, l2Interface.equipment(), "Strip.Now", stripDataNowTags, stripSub);
            readyNotedParamList.add(l2StripSizeNow);
            addOneParameter(L2ParamGroup.Parameter.Now, l2StripSizeNow);
            noteMonitoredTags(stripDataNowTags);

            Tag[] stripDataNextTags = {
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Process, false, false),
                    (tagNextStripDataReady = new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Ready, false, true)),
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Thick, false, false),
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Width, false, false),
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Noted, false, true),

                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Ready, true, false),
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.SpeedMax, true, false),
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.SpeedNow, true, false),
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Temperature, true, false),
                    new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Noted, true, false)};
            errorLocation = "Strip size Next tags";
            l2StripSizeNext = new ReadyNotedParam(source, l2Interface.equipment(), "Strip.Next", stripDataNextTags, stripSub);
            readyNotedParamList.add(l2StripSizeNext);
            addOneParameter(L2ParamGroup.Parameter.Next, l2StripSizeNext);
            noteMonitoredTags(stripDataNextTags);

            Tag[] stripTemperatureTags = {
                    new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.SP, false, false),
                    new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.PV, false, false),
                    new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.CV, false, false),
                    new Tag(L2ParamGroup.Parameter.Temperature, Tag.TagName.Mode, false, false)};
            errorLocation = "Strip Temperature tags";
            addOneParameter(L2ParamGroup.Parameter.Temperature, stripTemperatureTags);
            noteMonitoredTags(stripTemperatureTags);
            Tag[] stripSpeedTags = {
                    new Tag(L2ParamGroup.Parameter.Speed, Tag.TagName.SP, false, false),
                    new Tag(L2ParamGroup.Parameter.Speed, Tag.TagName.PV, false, false)};
            errorLocation = "Strip Speed tags";
            addOneParameter(L2ParamGroup.Parameter.Speed, stripSpeedTags);
            noteMonitoredTags(stripSpeedTags);
            Tag[] stripStatusTags = {
                    new Tag(L2ParamGroup.Parameter.Status, Tag.TagName.Length, false, false)};
            errorLocation = "Strip Status tags";
            addOneParameter(L2ParamGroup.Parameter.Status, stripStatusTags);
            noteMonitoredTags(stripStatusTags);
            retVal = true;
        }
        return retVal;
    }

    void noteMonitoredTags(Tag[] tags) {
        for (Tag tag : tags)
            if (tag.isMonitored())
                monitoredTags.put(tag.getMonitoredDataItem(), tag);
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

    public void prepareForDisconnection()throws ServiceException {
        stripSub.removeItems();
    }

    class StripListener extends L2SubscriptionListener {
        @Override
        public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
            L2DFHeating.AccessLevel accessLevel = l2DFHeating.accessLevel;
            if (l2DFHeating.isL2SystemReady() && (accessLevel == L2DFHeating.AccessLevel.RUNTIME)) {
                Tag theTag = monitoredTags.get(monitoredDataItem);
                if (l2StripSizeNext.isNewData(theTag))   // the data will be already read if new data
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
