package linkTFM;

import display.*;
import mvUtils.display.*;
import mvUtils.mvXML.DoubleWithErrStat;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLgroupStat;
import mvUtils.mvXML.XMLmv;
import mvUtils.display.FramedPanel;
import mvUtils.math.SPECIAL;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/4/13
 * Time: 10:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class BeamParams {
    static enum BeamType {FIXED, MOVING;}
    InputControl control;
    BeamType type;
    int secNum;
    double secLength;
    int beamsCount;
    int postsCount;
    double beamsTransmittance;
    double postsTransmittance;
    double beamsSurface;
    double postsSurface;
    double beamsExtTopWidth, beamsExtTopHeight;
    double beamsExtTotalWidth, beamsExtTotalHeight;
    // the following only for MOVING
    double slotsCoverFraction;
    double allSlotsSurface;
    double slotsEmissivity;
    double holesSurface;
    Vector<Double> beamPos;

    public BeamParams(String xmlStr, int secNum, BeamType type, XMLgroupStat grpStat, InputControl control) {
        this.type = type;
        this.secNum = secNum;
        this.control = control;
        DoubleWithErrStat dblWithStat;
        ValAndPos vp;
        String secname = "Sec" + ("" + secNum).trim() + ".";
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, "secLength", grpStat)).allOK)
            secLength = dblWithStat.val;
        if (type == BeamType.MOVING) {
            if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, "slotsCoverFraction", grpStat)).allOK)
                slotsCoverFraction = dblWithStat.val;
            if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, "allSlotsSurface", grpStat)).allOK)
                allSlotsSurface = dblWithStat.val;
            if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, "slotsEmissivity", grpStat)).allOK)
                slotsEmissivity = dblWithStat.val;
            // for walking hearth the following is applicable
            if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, "HolesSurface", grpStat)).allOK)
                holesSurface = dblWithStat.val;
        }
        String typeStr = (type == BeamType.MOVING) ? "Movable" : "Fixed";
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, typeStr + "BeamsCount", grpStat)).allOK)
            beamsCount = (int) dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, typeStr + "PostsCount", grpStat)).allOK)
            postsCount = (int) dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, typeStr + "BeamsTransmittance", grpStat)).allOK)
            beamsTransmittance = dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, typeStr + "PostsTransmittance", grpStat)).allOK)
            postsTransmittance = dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, typeStr + "BeamsSurface", grpStat)).allOK)
            beamsSurface = dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, typeStr + "PostsSurface", grpStat)).allOK)
            postsSurface = dblWithStat.val;

        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, typeStr + "BeamsExtTopWidth", grpStat)).allOK)
            beamsExtTopWidth = dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, typeStr + "BeamsExtTopHeight", grpStat)).allOK)
            beamsExtTopHeight = dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, typeStr + "BeamsExtTotalWidth", grpStat)).allOK)
            beamsExtTotalWidth = dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, secname, typeStr + "BeamsExtTotalHeight", grpStat)).allOK)
            beamsExtTotalHeight = dblWithStat.val;

        if (beamsCount > 0) {
            beamPos = new Vector<Double>();
            vp = XMLmv.getTag(xmlStr, typeStr + "BeamsPos", 0);
            String beamPosXML = vp.val;
            getBeamPos(secname + typeStr + "Beam", beamPosXML, beamPos, grpStat);
        }
    }

    double totalSkidLoss() {
        return beamsSurface * beamsTransmittance; // was * 1000;
    }

    double totalPostsLoss() {
        return postsSurface * postsTransmittance; // was * 1000;
    }

    double totalSlotsLossPerT4() {
        if (allSlotsSurface > 0)
            return allSlotsSurface * (1 - slotsCoverFraction) * SPECIAL.stefenBoltz * 0.3 * slotsEmissivity;
        else
            return 0;
    }

    double totalWHSlotsLossPerT4() {
        if (holesSurface > 0)
            return holesSurface * SPECIAL.stefenBoltz * 0.2;
        else
            return 0;
    }

    void getBeamPos(String beamType, String xmlStr, Vector<Double> beamPos, XMLgroupStat grpStat) {
        String posID;
        ValAndPos vp;
        DoubleWithErrStat dblWithStat;
        for (int i = 1; i <= beamsCount; i++) {
            posID = "pos" + ("" + i).trim();
            vp = XMLmv.getTag(xmlStr, posID, 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, beamType + posID, grpStat)).allOK)
                beamPos.add(dblWithStat.val);
        }
    }

    NumberLabel nlSecNum;
    NumberTextField ntSecLength;
    NumberTextField ntSlotsCoverFraction;
    NumberTextField ntAllSlotsSurface;
    NumberTextField ntSlotsEmissivity;
    NumberTextField ntHolesSurface;
    NumberTextField ntBeamsCount;
    NumberTextField ntPostsCount;
    NumberTextField ntBeamsTransmittance;
    NumberTextField ntPostsTransmittance;
    NumberTextField ntBeamsSurface;
    NumberTextField ntPostsSurface;
//    NumberTextField movableBeamsExtTopWidth, movableBeamsExtTopHeight;
//    NumberTextField movableBeamsExtTotalWidth, movableBeamsExtTotalHeight;

    static JPanel rowHeadP;
    static Vector<XLcellData> chCommonData, chFixedBeamData, chFixedPostData,chMovBeamData, chMovPostData;
    static Dimension colHeadSize = new JTextField("Flow of flue passing (m3N/h)", 20).getPreferredSize();
    public static JPanel colHeader() {
        if (rowHeadP == null) {
            rowHeadP = new FramedPanel(new GridBagLayout());
            GridBagConstraints gbcH = new GridBagConstraints();
            SizedLabel sL;
            Insets ins = new Insets(0, 0, 0, 0);
            gbcH.gridx = 0;
            gbcH.gridy = 0;
            gbcH.insets = ins;
            gbcH.weightx = 0.1;

            FramedPanel grpPan = new FramedPanel(new GridBagLayout());
            GridBagConstraints gbcL = new GridBagConstraints();
            gbcL.gridx = 0;
            gbcL.gridy = 0;
            chCommonData = new Vector<XLcellData>();
            sL = new SizedLabel("Section Length (m)", colHeadSize, false);
            chCommonData.add(sL);
            grpPan.add(sL, gbcL);
            rowHeadP.add(grpPan, gbcH);
            gbcH.gridy++;

            grpPan = new FramedPanel(new GridBagLayout());
            gbcL.gridy = 0;
            chFixedBeamData = new Vector<XLcellData>();
            sL = new SizedLabel("Fixed Beam Count", colHeadSize, false);
            chFixedBeamData.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Fixed Positions along Fce Width (m)", colHeadSize, false);
            chFixedBeamData.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Fixed Beam Transmittance", colHeadSize, false);
            chFixedBeamData.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Fixed Beam Surface Area (m2)", colHeadSize, false);
            chFixedBeamData.add(sL);
            grpPan.add(sL, gbcL);
            rowHeadP.add(grpPan, gbcH);
            gbcH.gridy++;

            grpPan = new FramedPanel(new GridBagLayout());
            gbcL.gridy = 0;
            chFixedPostData = new Vector<XLcellData>();
            sL = new SizedLabel("Fixed Post Count/Beam", colHeadSize, false);
            chFixedPostData.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Fixed Post Transmittance", colHeadSize, false);
            chFixedPostData.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Fixed Post Total Surface Area (m2)", colHeadSize, false);
            chFixedPostData.add(sL);
            grpPan.add(sL, gbcL);
            rowHeadP.add(grpPan, gbcH);
            gbcH.gridy++;

            grpPan = new FramedPanel(new GridBagLayout());
            gbcL.gridy = 0;
            chMovBeamData = new Vector<XLcellData>();
            sL = new SizedLabel("Movable Beam Count", colHeadSize, false);
            chMovBeamData.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Movable Positions along Fce Width (m)", colHeadSize, false);
            chFixedBeamData.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Movable Beam Transmittance", colHeadSize, false);
            chMovBeamData.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Movable Beam Surface Area (m2)", colHeadSize, false);
            chMovBeamData.add(sL);
            grpPan.add(sL, gbcL);
            rowHeadP.add(grpPan, gbcH);
            gbcH.gridy++;

            grpPan = new FramedPanel(new GridBagLayout());
            gbcL.gridy = 0;
            chMovPostData = new Vector<XLcellData>();
            sL = new SizedLabel("Movable Post Count/Beam", colHeadSize, false);
            chMovPostData.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Movable Post Transmittance", colHeadSize, false);
            chMovPostData.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Movable Post Total Surface Area (m2)", colHeadSize, false);
            chMovPostData.add(sL);
            grpPan.add(sL, gbcL);

            gbcL.gridy++;
            sL = new SizedLabel("Slot Cover Percenatge (%)", colHeadSize, false);
            chMovPostData.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("All Slots Surface Area (m2)", colHeadSize, false);
            chMovPostData.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Slot Emissivity", colHeadSize, false);
            chMovPostData.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = new SizedLabel("Holes Surface Area (m2)", colHeadSize, false);
            chMovPostData.add(sL);
            grpPan.add(sL, gbcL);


            rowHeadP.add(grpPan, gbcH);
            gbcH.gridy++;
        }
        return rowHeadP;
    }

    Vector<XLcellData> cmmonData, beamData, postData;
    int datW = 70;

    public JPanel commDataP() {
        String typeStr = (type == BeamType.MOVING) ? "Moving " : "Fixed ";
        FramedPanel grpPan = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridx = 0;
        gbcL.gridy = 0;
        cmmonData = new Vector<XLcellData>();
        ntSecLength = new NumberTextField(control, secLength, 6, false, 0, 100, "#,###.00", typeStr + "Section Length (m)");
        ntSecLength.setEditable(false);
        cmmonData.add(ntSecLength);
        grpPan.add(ntSecLength, gbcL);
        return grpPan;
    }

    public JPanel getBeamsDataP() {
        String typeStr = (type == BeamType.MOVING) ? "Moving " : "Fixed ";
        FramedPanel grpPan = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridx = 0;
        gbcL.gridy = 0;
        beamData = new Vector<XLcellData>();
        ntBeamsCount = new NumberTextField(control, beamsCount, 6, false, 0, 20, "#,###", typeStr + "Beams Count");
        ntBeamsCount.setEditable(false);
        beamData.add(ntBeamsCount);
        grpPan.add(ntBeamsCount, gbcL);
        gbcL.gridy++;

        XLComboBox list = new XLComboBox(beamPos, false);
        list.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ((JComboBox)(e.getSource())).setSelectedIndex(0);
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        list.setEditable(false);
//        JList list = new JList(beamPos);
        list.setPreferredSize(new Dimension(datW, 20));
        beamData.add(list);
        grpPan.add(list, gbcL);
        gbcL.gridy++;

        ntBeamsTransmittance = new NumberTextField(control, beamsTransmittance, 6, false, 0, 1000, "#,###.00", typeStr + "Beams Transmittance");
        ntBeamsTransmittance.setEditable(false);
        beamData.add(ntBeamsTransmittance);
        grpPan.add(ntBeamsTransmittance, gbcL);
        gbcL.gridy++;

        ntBeamsSurface = new NumberTextField(control, beamsSurface, 6, false, 0, 1000, "#,###.00", typeStr + "Beams Surface Area (m2)");
        ntBeamsSurface.setEditable(false);
        beamData.add(ntBeamsSurface);
        grpPan.add(ntBeamsSurface, gbcL);
        return grpPan;
    }

    public JPanel getPostDataP() {
        String typeStr = (type == BeamType.MOVING) ? "Moving " : "Fixed ";
        FramedPanel grpPan = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridx = 0;
        gbcL.gridy = 0;
        postData = new Vector<XLcellData>();
        ntPostsCount = new NumberTextField(control, postsCount, 6, false, 0, 100, "#,###", typeStr + "Posts Count");
        ntPostsCount.setEditable(false);
        beamData.add(ntPostsCount);
        grpPan.add(ntPostsCount, gbcL);
        gbcL.gridy++;

        ntPostsTransmittance = new NumberTextField(control,  postsTransmittance, 6, false, 0, 1000, "#,###.00", typeStr + "Posts Transmittance");
        ntPostsTransmittance.setEditable(false);
        postData.add(ntPostsTransmittance);
        grpPan.add(ntPostsTransmittance, gbcL);
        gbcL.gridy++;

        ntPostsSurface = new NumberTextField(control, postsSurface, 6, false, 0, 1000, "#,###.00", typeStr + "Posts Surface Area (m2)");
        ntPostsSurface.setEditable(false);
        postData.add(ntPostsSurface);
        grpPan.add(ntPostsSurface, gbcL);

        if (type == BeamType.MOVING) {
            gbcL.gridy++;
            ntSlotsCoverFraction = new NumberTextField(control, slotsCoverFraction * 100, 6, false, 0, 100, "#,###.00", typeStr + "Slots Cover Pecentage (%)");
            ntSlotsCoverFraction.setEditable(false);
            postData.add(ntSlotsCoverFraction);
            grpPan.add(ntSlotsCoverFraction, gbcL);
            gbcL.gridy++;

            ntAllSlotsSurface = new NumberTextField(control, allSlotsSurface, 6, false, 0, 1000, "#,###.00", typeStr + "All Slots Surface Area (m2)");
            ntAllSlotsSurface.setEditable(false);
            postData.add(ntAllSlotsSurface);
            grpPan.add(ntAllSlotsSurface, gbcL);

            gbcL.gridy++;
            ntSlotsEmissivity = new NumberTextField(control, slotsEmissivity, 6, false, 0, 1, "#,###.00", typeStr + "Slot Emissivity");
            ntSlotsEmissivity.setEditable(false);
            postData.add(ntSlotsEmissivity);
            grpPan.add(ntSlotsEmissivity, gbcL);
            gbcL.gridy++;
            ntHolesSurface = new NumberTextField(control, holesSurface, 6, false, 0, 1000, "#,###.00", typeStr + "Surface area of holes (m2)");
            ntHolesSurface.setEditable(false);
            postData.add(ntHolesSurface);
            grpPan.add(ntHolesSurface, gbcL);
        }
        return grpPan;
    }

}
