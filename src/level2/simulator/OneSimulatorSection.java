package level2.simulator;

import TMopcUa.ProcessData;
import TMopcUa.TMuaClient;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionAliveListener;
import level2.common.L2ParamGroup;
import level2.common.Tag;
import level2.common.TagCreationException;
import level2.common.L2SubscriptionListener;
import mvUtils.display.ErrorStatAndMsg;
import mvUtils.display.FramedPanel;
import mvUtils.display.MultiPairColPanel;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import org.opcfoundation.ua.builtintypes.DataValue;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 21-Aug-15
 * Time: 12:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class OneSimulatorSection {
    OpcSimulator opcSimulator;
    TMuaClient source;
    boolean readWrite = false;
    String name;
    int nParams;
    Subscription subscription = null;
    boolean dataCollectionOn = false;
    Calendar lastAlive;
    Calendar switchTime;
    boolean subsTimeout = false;
    LinkedHashMap<L2ParamGroup.Parameter, OneSimulatorParam> paramList;
    Hashtable<MonitoredDataItem, Tag> monitoredTags;
    boolean monitoredTagsReady = false;

    public OneSimulatorSection(OpcSimulator opcSimulator, OpcTagGroup tagGrp, boolean rw) throws Exception {
        this.opcSimulator = opcSimulator;
        this.source = opcSimulator.source;
        this.readWrite = rw;
        paramList = new LinkedHashMap<L2ParamGroup.Parameter, OneSimulatorParam>();
        if (!readWrite)
            subscription = source.createSubscription(new SubAliveListener(), new ZoneSubscriptionListener());
        createFromTagGroup(tagGrp);
    }

    boolean createFromTagGroup(OpcTagGroup tagGrp) throws Exception {
        name = tagGrp.name;
//        if (!name.equalsIgnoreCase("Messages"))
            return addParameters(tagGrp);
//        else
//            return false;
    }

    boolean addOneTag(L2ParamGroup.Parameter element, String xmlStr, Vector<TagWithDisplay> vTags) {
        boolean retVal = false;
        if (xmlStr.length() > 10) {
            ValAndPos vp;
            vp = XMLmv.getTag(xmlStr, "servermain:Name", 0);
            Tag.TagName tagName = Tag.TagName.getEnum(vp.val);
            vp = XMLmv.getTag(xmlStr, "servermain:DataType", 0);
            ProcessData.DataType dataType = ProcessData.DataType.getEnum(vp.val);
            vp = XMLmv.getTag(xmlStr, "servermain:ReadWriteAccess", 0);
            boolean rw = vp.val.equalsIgnoreCase("Read/Write");
            vTags.add(new TagWithDisplay(element, tagName, !rw, !rw, "#,##0.000", opcSimulator));
            retVal = true;
        }
        return retVal;
    }

    boolean addParameters(OpcTagGroup tagGrp) throws Exception {
        for (OpcTagGroup oneGrp : tagGrp.subGroups) {
            addOneParameter(L2ParamGroup.Parameter.getEnum(oneGrp.name), oneGrp.getVTags(readWrite, opcSimulator));
        }
        return true;
    }

    boolean addOneParameter(L2ParamGroup.Parameter element, Vector<TagWithDisplay> vTags) throws TagCreationException {
        boolean retVal = false;
        String basePath = "";
        try {
            OneSimulatorParam param = new OneSimulatorParam(opcSimulator.source, opcSimulator.equipment,
                    name, element.toString(),
                    vTags, subscription);
            basePath = name + "." + element;
            paramList.put(element, param);
            retVal = true;
        } catch (TagCreationException e) {
            e.setElement(basePath, "");
            throw (e);
        }
        return retVal;
    }

    JPanel getDisplayPanel(int width) {
        MultiPairColPanel outerP = new MultiPairColPanel(name);
        FramedPanel innerP = new FramedPanel(new FlowLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridy = 0;
        for (OneSimulatorParam param : paramList.values()) {
//            innerP.add(param.getDisplayPanel(), gbc);
            innerP.add(param.getDisplayPanel());
            gbc.gridx++;
        }
        outerP.addItem(innerP);
        return outerP;
    }

    void updateUI() {
        for (OneSimulatorParam param : paramList.values())
            param.updateUI();
    }

    ErrorStatAndMsg checkConnections() {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg(false, "" + name + " params:");
        boolean additional = false;
        ErrorStatAndMsg oneParamRetVal;
        for (OneSimulatorParam p : paramList.values()) {
            oneParamRetVal = p.checkConnections();
            if (oneParamRetVal.inError) {
                retVal.inError = true;
                retVal.msg += ((additional) ? ", " : "") + oneParamRetVal.msg;
                additional = true;
            }
        }
        return retVal;
    }

    void closeSubscription() throws ServiceException {
        if (subscription != null)
            subscription.removeItems();
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
            if (opcSimulator.uiReady)
                updateUI();
//             String fromElement =  monitoredDataItem.toString();
        }
    }

    void showMessage(String msg) {
        opcSimulator.showMessage("Section." + name + ": " + msg);
    }

    void showError(String msg) {
        opcSimulator.showError("Section." + name + ": " + msg);
    }

}
