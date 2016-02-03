package level2.display;

import display.SizedLabel;
import level2.L2Zone;
import level2.common.L2ParamGroup;
import level2.common.L2ZoneParam;
import level2.common.Tag;
import mvUtils.display.FramedPanel;
import mvUtils.display.XLcellData;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

/**
 * User: M Viswanathan
 * Date: 29-Jan-16
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2DisplayZone implements L2Display{
    L2Zone l2Zone;
    JPanel displayPanel;
    static Dimension colHeadSize = new JTextField(12).getPreferredSize();
    Dimension dataFieldSize = new JTextField(8).getPreferredSize();
    public L2DisplayZone(L2Zone l2Zone) {
        this.l2Zone = l2Zone;
        tags = new Vector<Tag>();
        createDisplayPanel();
    }

    static FramedPanel rowHead;

    Vector<Tag> tags;

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

    void createDisplayPanel() {
        displayPanel = new FramedPanel(new GridBagLayout());
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
        sL = new SizedLabel(l2Zone.groupName, dataFieldSize, true);
        grpPan.add(sL, gbcL);
        displayPanel.add(grpPan, gbcH);
        gbcH.gridy++;

        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = new SizedLabel(" ", dataFieldSize);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        param = l2Zone.getL2Param(L2ParamGroup.Parameter.Temperature);
        tag = param.getProcessTag(Tag.TagName.Auto);
        tags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        tag = param.getProcessTag(Tag.TagName.SP);
        tags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        tag = param.getProcessTag(Tag.TagName.PV);
        tags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcH.gridy++;
        displayPanel.add(grpPan, gbcH);

        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = new SizedLabel(" ", dataFieldSize, true);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        param = l2Zone.getL2Param(L2ParamGroup.Parameter.FuelFlow);
        tag = param.getProcessTag(Tag.TagName.Remote);
        tags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        tag = param.getProcessTag(Tag.TagName.Auto);
        tags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        tag = param.getProcessTag(Tag.TagName.SP);
        tags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        tag = param.getProcessTag(Tag.TagName.PV);
        tags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcH.gridy++;
        gbcH.gridy++;
        displayPanel.add(grpPan, gbcH);

        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = new SizedLabel(" ", dataFieldSize, true);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        param = l2Zone.getL2Param(L2ParamGroup.Parameter.AFRatio);
        tag = param.getProcessTag(Tag.TagName.SP);
        tags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        param = l2Zone.getL2Param(L2ParamGroup.Parameter.AirFlow);
        tag = param.getProcessTag(Tag.TagName.Remote);
        tags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        tag = param.getProcessTag(Tag.TagName.Auto);
        tags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcL.gridy++;
        tag = param.getProcessTag(Tag.TagName.PV);
        tags.add(tag);
        c = tag.displayComponent();
        grpPan.add(c, gbcL);
        gbcH.gridy++;
        displayPanel.add(grpPan, gbcH);
    }

    public void updateDisplay() {
        for (Tag tag:tags) {
            try {
                tag.updateUI();
            } catch (Exception e) {
                System.out.println("Tag : " + tag + " had some display problem");
//                e.printStackTrace();
            }
        }
    }

    public Container getDisplay() {
        return displayPanel;
    }
}
