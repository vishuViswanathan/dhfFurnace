package level2.common;

import level2.common.L2ParamGroup;
import level2.common.Tag;
import level2.common.TagCreationException;
import level2.display.L2Display;
import level2.stripDFH.L2DFHFurnace;
import mvUtils.display.FramedPanel;
import mvUtils.display.MultiPairColPanel;

import javax.swing.*;
import java.awt.*;

/**
 * User: M Viswanathan
 * Date: 04-Apr-16
 * Time: 5:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2OneParameterZone extends L2ParamGroup implements L2Display {
    boolean withDisplay = false;
    L2ParamGroup.Parameter parameter;
    Tag[] processTags;
    Tag[] level2Tags;
    JPanel processDisplayPanel;
    JPanel level2DisplayPanel;
    public L2OneParameterZone(L2DFHFurnace l2Furnace, String zoneName, String descriptiveName, L2ParamGroup.Parameter parameter,
                              String fmtStr, boolean withDisplay) throws TagCreationException {
        super(l2Furnace, zoneName, descriptiveName);
        this.parameter = parameter;
        String basePath = "";
        Tag[] pTags = {new Tag(parameter, Tag.TagName.SP, false, false, fmtStr),
                new Tag(parameter, Tag.TagName.PV, false, false, fmtStr),
                new Tag(parameter, Tag.TagName.Auto, false, false)};
        processTags = pTags;
        addOneParameter(parameter, pTags);
        Tag[] l2Tags = {new Tag(parameter, Tag.TagName.SP, true, false, fmtStr)};
        level2Tags = l2Tags;
        addOneParameter(parameter, l2Tags);
        if (withDisplay) {
            createProcessDisplay();
            createLevel2Display();
        }
    }

    void createProcessDisplay() {
//        processDisplayPanel = new FramedPanel();
        processDisplayPanel = new JPanel();
        MultiPairColPanel mp = new MultiPairColPanel(descriptiveName + ": " + parameter);
        for (Tag t: processTags)
            mp.addItemPair(t.toString(), t.displayComponent());
        processDisplayPanel.add(mp);
        withDisplay = true;
    }

    void createLevel2Display() {
//        level2DisplayPanel = new FramedPanel();
        level2DisplayPanel = new JPanel();
        MultiPairColPanel mp = new MultiPairColPanel(descriptiveName + ": " + parameter);
        for (Tag t: level2Tags)
            mp.addItemPair(t.toString(), t.displayComponent());
        level2DisplayPanel.add(mp);
        withDisplay = true;
    }

    public void updateProcessDisplay() {
        String errorTags = "";
        boolean errorsExist = false;
        for (Tag tag: processTags) {
            try {
                tag.updateUI();

            } catch (Exception e) {
                errorTags += tag.elementAndTag() + "; ";
                errorsExist = true;
            }
        }
        if (errorsExist)
            logError("ProcessDisplay-Tag/s : " + groupName + ": " + errorTags + " had some display problem");
    }

    public void updateLevel2Display() {
        String errorTags = "";
        boolean errorsExist = false;
        for (Tag tag: level2Tags) {
            try {
                tag.updateUI();
            } catch (Exception e) {
                errorTags += tag.elementAndTag() + "; ";
                errorsExist = true;
            }
        }
        if (errorsExist)
            logError("L2Display-Tag/s : " + groupName + ": " + errorTags + " had some display problem");
    }


//    public void updateProcessDisplay() {
//        if (withDisplay) {
//            for (Tag t : processTags) {
//                try {
//                    t.updateUI();
//                } catch (Exception e) {
//                    logError("ProcessDisplay-Tag " + t.totalPath() + " had some display problem");
//                }
//            }
//        }
//    }
//
//    public void updateLevel2Display() {
//        for (Tag t : level2Tags){
//            try {
//                t.updateUI();
//            } catch (Exception e) {
//                logError("L2Display-Tag " + t.totalPath() + " had some display problem");
//            }
//        }
//    }

    public Container getProcessDisplay() {
        return processDisplayPanel;
    }

    public Container getLevel2Display() {
        return level2DisplayPanel;
    }
}