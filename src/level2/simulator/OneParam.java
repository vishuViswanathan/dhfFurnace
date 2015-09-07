package level2.simulator;

import TMopcUa.OneDataGroup;
import TMopcUa.ProcessData;
import TMopcUa.ProcessValue;
import TMopcUa.TMuaClient;
import com.prosysopc.ua.client.Subscription;
import level2.Tag;
import level2.TagCreationException;
import mvUtils.display.ErrorStatAndMsg;
import mvUtils.display.MultiPairColPanel;

import javax.swing.*;


/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 21-Aug-15
 * Time: 12:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class OneParam extends OneDataGroup {
    TagWithDisplay[] tagList;
    String processElement;

    public OneParam(TMuaClient source, String equipment, String processElement, TagWithDisplay[] tagList,
                       Subscription subscription) throws TagCreationException {
        super(source, equipment, processElement, subscription);
        this.processElement = processElement;
        this.tagList = tagList;
        for (TagWithDisplay tag : tagList) {
            try {

                tag.setProcessData(getProcessData(tag.dataSource, "" + tag, tag.dataType, tag.access, tag.bSubscribe));
                if (tag.bSubscribe && (subscription == null))
                    throw new TagCreationException("" + tag, "Subscription is null");

            } catch (Exception e) {
                throw new TagCreationException("" + tag, "Data Point could not be created!\n" + e.getMessage());
            }
        }
     }

    JPanel getDisplayPanel() {
        MultiPairColPanel mp = new MultiPairColPanel("");
        for (TagWithDisplay tag : tagList) {
            tag.addToMultiPairColPanel(mp);
        }
        return mp;
    }

    public void updateUI() {
        for (TagWithDisplay tag : tagList)
            tag.updateUI();
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

}
