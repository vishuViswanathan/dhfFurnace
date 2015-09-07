package level2.simulator;

import TMopcUa.ProcessData;
import TMopcUa.TMuaClient;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionAliveListener;
import level2.L2ParamGroup;
import level2.OneStripDFHProcess;
import level2.Tag;
import level2.TagCreationException;
import level2.listeners.L2SubscriptionListener;
import mvUtils.display.ErrorStatAndMsg;
import mvUtils.display.FramedPanel;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import org.opcfoundation.ua.builtintypes.DataValue;
import performance.stripFce.Performance;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 21-Aug-15
 * Time: 12:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class OneSection {
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
    Hashtable<L2ParamGroup.Parameter, OneParam> paramList;
    Hashtable<MonitoredDataItem, Tag> monitoredTags;
    boolean monitoredTagsReady = false;

    public OneSection(OpcSimulator opcSimulator, String xmlStr, boolean readWrite) throws TagCreationException {
        this.opcSimulator = opcSimulator;
        this.source = opcSimulator.source;
        this.readWrite = readWrite;
        paramList = new Hashtable<L2ParamGroup.Parameter, OneParam>();
        if (!readWrite)
            subscription = source.createSubscription(new SubAliveListener(), new ZoneSubscriptionListener());
        takeFromXml(xmlStr);
    }

    boolean takeFromXml(String xmlStr) throws TagCreationException {
        boolean retVal = false;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "secName", 0);
        name = vp.val;
        vp = XMLmv.getTag(xmlStr, "nParams", 0);
        nParams = Integer.valueOf(vp.val);

        for (int p = 0; p < nParams; p++) {
            vp = XMLmv.getTag(xmlStr, "Param" + ("" + (p + 1)).trim(), vp.endPos);
            addOneParameter(vp.val);
        }
        return retVal;
    }

    boolean addOneParameter(String xmlStr) throws TagCreationException {
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "pName", 0);
        L2ParamGroup.Parameter element = L2ParamGroup.Parameter.getEnum(vp.val);

        vp = XMLmv.getTag(xmlStr, "nTags", 0);
        int nTags = Integer.valueOf(vp.val);
        TagWithDisplay[] tagList = new TagWithDisplay[nTags];
        ValAndPos oneTagVp;
        boolean tagReadWrite;
        String formatStr;
        for (int t = 0; t < nTags; t++) {
            vp = XMLmv.getTag(xmlStr, "Tag" + ("" + (t + 1)).trim(), vp.endPos);
            String tagXml = vp.val;
            oneTagVp = XMLmv.getTag(tagXml, "TagName", 0);
            Tag.TagName tagName = Tag.TagName.getEnum(oneTagVp.val);
            oneTagVp = XMLmv.getTag(tagXml, "access", 0);
            tagReadWrite = !(oneTagVp.val).equals("rw"); // TODO for simulation the rw field in opc server which is written by level2 is read only
                                                           // and the fields pof the process (which are ro for level2) are writable
            if (tagReadWrite != readWrite)
                opcSimulator.showError("ro/rw mismatch in " + name + "." + element + "." + tagName);
            oneTagVp = XMLmv.getTag(tagXml, "dataType", 0);
            ProcessData.DataType dataType = ProcessData.DataType.getEnum(oneTagVp.val);
            oneTagVp = XMLmv.getTag(tagXml, "format", 0);
            formatStr = oneTagVp.val;
            tagList[t] = new TagWithDisplay(element, tagName, !readWrite, !readWrite, formatStr,
                    opcSimulator);
        }
        return addOneParameter(element, tagList);
    }

    boolean addOneParameter(L2ParamGroup.Parameter element, TagWithDisplay[] tags) throws TagCreationException {
         boolean retVal = false;
         String basePath = "";
         try {
             OneParam param = new OneParam(opcSimulator.source, opcSimulator.equipment,
                     (basePath = name + "." + element),
                     tags, subscription);
             paramList.put(element, param);
             retVal = true;
         } catch (TagCreationException e) {
             e.setElement(basePath, "");
             throw (e);
         }
         return retVal;
    }

    JPanel getDisplayPanel(int width) {
        FramedPanel outerP = new FramedPanel();
        JTabbedPane tabbedPane = new JTabbedPane();
        for (OneParam param: paramList.values())
            tabbedPane.addTab(param.processElement, param.getDisplayPanel());
        Dimension dimension = tabbedPane.getPreferredSize();
        dimension.width = width;
        tabbedPane.setPreferredSize(dimension);
        outerP.add(tabbedPane);
        return outerP;
    }

    void updateUI() {
        for (OneParam param: paramList.values())
            param.updateUI();
    }

    ErrorStatAndMsg checkConnections() {
         ErrorStatAndMsg retVal = new ErrorStatAndMsg(false, "" + name + " params:");
         boolean additional = false;
        ErrorStatAndMsg oneParamRetVal;
         for (OneParam p: paramList.values()) {
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
