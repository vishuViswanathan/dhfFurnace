package level2;

import level2.common.L2ParamGroup;
import level2.common.Tag;
import level2.common.TagCreationException;
import level2.display.L2Display;
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
        if (withDisplay)
            createProcessDisplay();
    }

    void createProcessDisplay() {
        processDisplayPanel = new FramedPanel();
        MultiPairColPanel mp = new MultiPairColPanel(descriptiveName + ": " + parameter);
        for (Tag t: processTags)
            mp.addItemPair(t.toString(), t.displayComponent());
        processDisplayPanel.add(mp);
        withDisplay = true;
    }

    void createLevel2Display() {
        level2DisplayPanel = new FramedPanel();
        MultiPairColPanel mp = new MultiPairColPanel(descriptiveName + ": " + parameter);
        for (Tag t: level2Tags)
            mp.addItemPair(t.toString(), t.displayComponent());
        level2DisplayPanel.add(mp);
        withDisplay = true;
    }

    public void updateDisplay() {
        if (withDisplay) {
            for (Tag t : processTags)
                t.updateUI();
            for (Tag t : level2Tags)
                t.updateUI();
        }
    }

    public Container getProcessDisplay() {
        return processDisplayPanel;
    }

    public Container getLevel2Display() {
        return level2DisplayPanel;
    }
}