package level2.display;

import display.SizedLabel;
import level2.stripDFH.L2DFHZone;
import level2.common.L2ParamGroup;
import level2.common.L2ZoneParam;
import level2.common.Tag;
import mvUtils.display.FramedPanel;
import mvUtils.display.MultiColDataPanel;
import mvUtils.display.MultiPairColPanel;

import javax.swing.*;
import java.awt.*;
import java.util.GregorianCalendar;
import java.util.Vector;

/**
 * User: M Viswanathan
 * Date: 29-Jan-16
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2DFHDisplay implements L2Display{
    L2DFHZone l2DFHZone;
    JPanel processDisplayPanel;
    JPanel level2DisplayPanel;
    static Dimension colHeadSize = new JTextField(12).getPreferredSize();
    Dimension dataFieldSize = new JTextField(8).getPreferredSize();
    public L2DFHDisplay(L2DFHZone l2DFHZone) {
        this.l2DFHZone = l2DFHZone;
        processTags = new Vector<Tag>();
        createProcessDisplayPanel();
        l2Tags = new Vector<Tag>();
        createL2DisplayPanel();
    }

    static FramedPanel rowHead;

    Vector<Tag> processTags;
    Vector<Tag> l2Tags;

    public static FramedPanel getRowHeader() {
        if (rowHead == null) {
            rowHead = new FramedPanel(new GridBagLayout());
            GridBagConstraints gbcH = new GridBagConstraints();
            SizedLabel sL;
            Insets ins = new Insets(0, 0, 0, 0);
            gbcH.gridx = 0;
            gbcH.gridy = 0;
            gbcH.insets = ins;
            gbcH.weightx = 0.1;
            FramedPanel grpPan;
            GridBagConstraints gbcL = new GridBagConstraints();
            gbcL.gridx = 0;
            grpPan = new FramedPanel(new GridBagLayout());
            gbcL.gridy = 0;
            sL = new SizedLabel("", colHeadSize);
            grpPan.add(sL, gbcL);
            rowHead.add(grpPan, gbcH);
            gbcH.gridy++;

            grpPan = new FramedPanel(new GridBagLayout());
            gbcL.gridy = 0;
            sL = new SizedLabel("Temperature", colHeadSize, JLabel.LEADING, true, true);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Auto/Manual", colHeadSize, JLabel.TRAILING, false, false);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Set Point", colHeadSize, JLabel.TRAILING, false, false);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Process Value (degC)", colHeadSize, JLabel.TRAILING, false, false);
            grpPan.add(sL, gbcL);
            gbcH.gridy++;
            rowHead.add(grpPan, gbcH);

            grpPan = new FramedPanel(new GridBagLayout());
            gbcL.gridy = 0;
            sL = new SizedLabel("Fuel", colHeadSize, JLabel.LEADING, true, true);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Remote/Local", colHeadSize, JLabel.TRAILING, false, false);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Auto/Manual", colHeadSize, JLabel.TRAILING, false, false);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Set Point", colHeadSize, JLabel.TRAILING, false, false);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Process Value", colHeadSize, JLabel.TRAILING, false, false);
            grpPan.add(sL, gbcL);
            gbcH.gridy++;
            rowHead.add(grpPan, gbcH);

            grpPan = new FramedPanel(new GridBagLayout());
            gbcL.gridy = 0;
            sL = new SizedLabel("Combustion Air", colHeadSize, JLabel.LEADING, true, true);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Air/Fuel Ratio SP", colHeadSize, JLabel.TRAILING, false, false);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Remote/Local", colHeadSize, JLabel.TRAILING, false, false);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Auto/Manual", colHeadSize, JLabel.TRAILING, false, false);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Process Value", colHeadSize, JLabel.TRAILING, false, false);
            grpPan.add(sL, gbcL);
            gbcH.gridy++;
            rowHead.add(grpPan, gbcH);
        }
        return rowHead;
    }

    void createProcessDisplayPanel() {
        processDisplayPanel = new FramedPanel(new GridBagLayout());
        L2ZoneParam param;
        Component c;
        Tag tag;
        GridBagConstraints gbcH = new GridBagConstraints();
        SizedLabel sL;
        Insets ins = new Insets(0, 0, 0, 0);
        gbcH.gridx = 0;
        gbcH.gridy = 0;
        gbcH.insets = ins;
        gbcH.weightx = 0.1;
        FramedPanel grpPan;
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridx = 0;
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = new SizedLabel(l2DFHZone.descriptiveName, dataFieldSize, true);
        grpPan.add(sL, gbcL);
        processDisplayPanel.add(grpPan, gbcH);
        gbcH.gridy++;

        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = new SizedLabel(" ", dataFieldSize);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        param = l2DFHZone.getL2Param(L2ParamGroup.Parameter.Temperature);
        tag = param.getProcessTag(Tag.TagName.Auto);
        processTags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        tag = param.getProcessTag(Tag.TagName.SP);
        processTags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        tag = param.getProcessTag(Tag.TagName.PV);
        processTags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcH.gridy++;
        processDisplayPanel.add(grpPan, gbcH);

        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = new SizedLabel(" ", dataFieldSize, true);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        param = l2DFHZone.getL2Param(L2ParamGroup.Parameter.FuelFlow);
        tag = param.getProcessTag(Tag.TagName.Remote);
        processTags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        tag = param.getProcessTag(Tag.TagName.Auto);
        processTags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        tag = param.getProcessTag(Tag.TagName.SP);
        processTags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        tag = param.getProcessTag(Tag.TagName.PV);
        processTags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcH.gridy++;
        gbcH.gridy++;
        processDisplayPanel.add(grpPan, gbcH);

        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = new SizedLabel(" ", dataFieldSize, true);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        param = l2DFHZone.getL2Param(L2ParamGroup.Parameter.AFRatio);
        tag = param.getProcessTag(Tag.TagName.SP);
        processTags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        param = l2DFHZone.getL2Param(L2ParamGroup.Parameter.AirFlow);
        tag = param.getProcessTag(Tag.TagName.Remote);
        processTags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        tag = param.getProcessTag(Tag.TagName.Auto);
        processTags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        tag = param.getProcessTag(Tag.TagName.PV);
        processTags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcH.gridy++;
        processDisplayPanel.add(grpPan, gbcH);
    }

    JPanel createL2DisplayPanel() {
        level2DisplayPanel = new FramedPanel(new GridBagLayout());
        L2ZoneParam param;
        Component c;
        Tag tag;
        GridBagConstraints gbcH = new GridBagConstraints();
        SizedLabel sL;
        Insets ins = new Insets(0, 0, 0, 0);
        gbcH.gridx = 0;
        gbcH.gridy = 0;
        gbcH.insets = ins;
        gbcH.weightx = 0.1;
        FramedPanel grpPan;
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridx = 0;
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = new SizedLabel(l2DFHZone.descriptiveName, dataFieldSize, true);
        grpPan.add(sL, gbcL);
        level2DisplayPanel.add(grpPan, gbcH);
        gbcH.gridy++;

        MultiPairColPanel spPanel = new MultiPairColPanel("");
        param = l2DFHZone.getL2Param(L2ParamGroup.Parameter.Temperature);
        tag = param.getLevel2Tag(Tag.TagName.SP);
        l2Tags.add(tag);
        c = tag.displayComponent();
        spPanel.addItemPair("Temperature SP" , tag.displayComponent());
        param = l2DFHZone.getL2Param(L2ParamGroup.Parameter.FuelFlow);
        tag = param.getLevel2Tag(Tag.TagName.SP);
        l2Tags.add(tag);
        c = tag.displayComponent();
        spPanel.addItemPair("Fuel Flow SP", tag.displayComponent());
        level2DisplayPanel.add(spPanel, gbcH);
        gbcH.gridy++;

//        MultiPairColPanel fuelChrPanel = new MultiPairColPanel("Fuel Control Characteristic");
//        fuelChrPanel.addItemPair( "Total", "Zone", true, GridBagConstraints.CENTER);
//        for (int s = 0; s < l2DFHZone.tagsFuelChrTotal.length; s++) {
//            Tag tagL = l2DFHZone.tagsFuelChrTotal[s];
//            l2Tags.add(tagL);
//            Tag tagR = l2DFHZone.tagsFuelChrZone[s];
//            l2Tags.add(tagR);
//            fuelChrPanel.addItemPair(tagL.displayComponent(), tagR.displayComponent());
//        }

        MultiColDataPanel fuelChrPanel = new MultiColDataPanel("Fuel Control Characteristic", 3);
        fuelChrPanel.addItemSet(new String[]{"Total", "Zone", "Speed"}, true, GridBagConstraints.CENTER);
        for (int s = 0; s < l2DFHZone.tagsFuelChrTotal.length; s++) {
            Tag tag1 = l2DFHZone.tagsFuelChrTotal[s];
            l2Tags.add(tag1);
            Tag tag2 = l2DFHZone.tagsFuelChrZone[s];
            l2Tags.add(tag2);
            Tag tag3 = l2DFHZone.tagsFuelChrSpeed[s];
            l2Tags.add(tag3);
            fuelChrPanel.addItemSet(new Component[]{tag1.displayComponent(), tag2.displayComponent(), tag3.displayComponent()});
        }
        level2DisplayPanel.add(fuelChrPanel, gbcH);
        return level2DisplayPanel;
    }

    public void updateProcessDisplay() {
        for (Tag tag: processTags) {
            try {
                tag.updateUI();
            } catch (Exception e) {
                System.out.println("Tag : " + tag + " had some display problem");
//                e.printStackTrace();
            }
        }
    }

    public void updateLevel2Display() {
        for (Tag tag: l2Tags) {
            try {
                tag.updateUI();
            } catch (Exception e) {
                System.out.println("Tag : " + tag + " had some display problem");
//                e.printStackTrace();
            }
        }

    }

    public Container getProcessDisplay() {
        return processDisplayPanel;
    }

    public Container getLevel2Display() {
        return level2DisplayPanel;
    }
}
