package level2.simulator;

import TMopcUa.OneDataGroup;
import TMopcUa.TMuaClient;
import com.prosysopc.ua.client.Subscription;
import level2.common.TagCreationException;
import mvUtils.display.ErrorStatAndMsg;
import mvUtils.display.MultiPairColPanel;

import javax.swing.*;
import java.util.Collection;
import java.util.Vector;


/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 21-Aug-15
 * Time: 12:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class OneSimulatorParam extends OneDataGroup {
    Collection<TagWithDisplay> tagList;
    String processElement;
    String paramName;

//    public OneParam(TMuaClient source, String equipment, String processElement, Vector<TagWithDisplay> tagList,
//                       Subscription subscription) throws TagCreationException {
//        super(source, equipment, processElement, subscription);
//        this.processElement = processElement;
//        this.tagList = tagList;
//        for (TagWithDisplay tag : tagList) {
//            try {
//
//                tag.setProcessData(getProcessData(tag.dataSource, "" + tag, tag.dataType, tag.access, tag.bSubscribe));
//                if (tag.bSubscribe && (subscription == null))
//                    throw new TagCreationException("" + tag, "Subscription is null");
//
//            } catch (Exception e) {
//                throw new TagCreationException("" + tag, "Data Point could not be created!\n" + e.getMessage());
//            }
//        }
//    }

    public OneSimulatorParam(TMuaClient source, String equipment, String sectionName, String paramName, Vector<TagWithDisplay> tagList,
                             Subscription subscription) throws TagCreationException {
        super(source, equipment, sectionName + "." + paramName, subscription);
        this.paramName = paramName;
        this.processElement = sectionName + "." + paramName;
        this.tagList = tagList;
        for (TagWithDisplay tag : tagList) {
            try {

                tag.setProcessData(getProcessData(tag.dataSource, "" + tag, tag.dataType, tag.access, tag.bSubscribe));
                if (tag.bSubscribe && (subscription == null))
                    throw new TagCreationException(equipment, processElement, tag, "Subscription is null");

            } catch (Exception e) {
                throw new TagCreationException(equipment, processElement, tag, "Data Point could not be created!\n" + e.getMessage());
            }
        }
    }

    JPanel getDisplayPanel() {
        MultiPairColPanel mp = new MultiPairColPanel(paramName);
        for (TagWithDisplay tag : tagList) {
            tag.addToMultiPairColPanel(mp);
        }
        return mp;
    }

    public void updateUI() {
        String errorTags = "";
        boolean errorsExist = false;
        for (TagWithDisplay tag : tagList)
            try {
                tag.updateUI();
            }
            catch (Exception e) {
                errorTags += tag.elementAndTag() + "; ";
                errorsExist = true;
            }
        if (errorsExist)
            error("OneSimulatorParam-Tag/s : " + paramName + ": " + errorTags + " had some display problem");
    }

    ErrorStatAndMsg checkConnections() {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg(false, "" + processElement + " tags:");
        boolean additional = false;
        for (TagWithDisplay t: tagList) {
            if (!t.checkConnection()) {
                retVal.inError = true;
                retVal.msg += ((additional) ? ", " : "") +t.tagName;
                additional = true;
            }
        }
        return retVal;
    }

    public void info(String msg) {
        TMuaClient.info("OneDataGroup:" + msg);
    }

    public void error(String msg) {
        TMuaClient.error("OneDataGroup:" + msg);
    }}
