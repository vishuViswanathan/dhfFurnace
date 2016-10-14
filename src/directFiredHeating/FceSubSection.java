package directFiredHeating;

import basic.*;
import display.*;
import mvUtils.display.*;
import mvUtils.math.DoublePoint;
import mvUtils.math.MultiColData;
import mvUtils.math.SPECIAL;
import mvUtils.math.XYArray;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import org.apache.poi.ss.usermodel.Sheet;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 5/9/12
 * Time: 10:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class FceSubSection {
    static boolean inited = false;
    static JPanel lossHeadP = new JPanel(new GridBagLayout());
    static FramedPanel rowHead;
    static int firstLossY;
    static GridBagConstraints gbcHeader = new GridBagConstraints();
    static Dimension colHeadSize = new Dimension(220, 20); //(new JTextField("abcd", 15)).getPreferredSize();
    static Dimension colDataSize = (new JTextField("100000", 6)).getPreferredSize();
    public static DFHFurnace furnace;
    static ButtonGroup topBotSel;
    static JRadioButton rbTop;
    static JRadioButton rbBot;
    static String nlSpace = ErrorStatAndMsg.nlSpace;
    static VScrollSync vScrollSync;
    static String strTopZones = "Top Zones";
    static String strZones = "Zones";

    static void initStaticData(Component tfTopLen, Component tfBotLen) {
//        if (!inited) {
            topBotSel = new ButtonGroup();
            rbTop = new JRadioButton(strTopZones);
            rbBot = new JRadioButton("Bottom Zones");
            rbTop.setSelected(true);
            topBotSel.add(rbTop);
            topBotSel.add(rbBot);

            rowHead = new FramedPanel(new GridBagLayout());
            Insets ins = new Insets(0, 0, 0, 0);
            gbcHeader.gridx = 0;
            gbcHeader.gridy = 0;
            JPanel jb = new JPanel(new BorderLayout());
            jb.setPreferredSize(new Dimension(200, 50));
            JPanel jpTop = new JPanel(new BorderLayout());
            jpTop.add(rbTop, BorderLayout.WEST);
            jpTop.add(tfTopLen, BorderLayout.CENTER);
            jpTop.add(new JLabel(" mm", 2), BorderLayout.EAST);

            jb.add(jpTop, BorderLayout.NORTH);

            JPanel jpBot = new JPanel(new BorderLayout());
            jpBot.add(rbBot, BorderLayout.WEST);
            jpBot.add(tfBotLen, BorderLayout.CENTER);
            jpBot.add(new JLabel(" mm", 2), BorderLayout.EAST);

            jb.add(jpBot, BorderLayout.SOUTH);
            rowHead.add(jb, gbcHeader);
            gbcHeader.gridy++;
            gbcHeader.insets = ins;
            gbcHeader.weightx = 0.1;
            rowHead.add(sizedLabel("Length(mm)", colHeadSize), gbcHeader);
            gbcHeader.gridy++;
            rowHead.add(sizedLabel("Height1(mm)", colHeadSize), gbcHeader);
            gbcHeader.gridy++;
            rowHead.add(sizedLabel("Height2(mm)", colHeadSize), gbcHeader);
            gbcHeader.gridy++;
//            rowHead.add(sizedLabel("Temperature(C)", colHeadSize), gbcHeader);
//            gbcHeader.gridy++;
            gbcHeader.insets = new Insets(1, 0, 1, 0);
            rowHead.add(sizedLabel("Losses", colHeadSize), gbcHeader);
            gbcHeader.gridy++;
            rowHead.add(sizedLabel("Fixed Losses(kcal/h)", colHeadSize), gbcHeader);

            LossTypeList lossTypeList = furnace.lossTypeList;
            firstLossY = gbcHeader.gridy;
            gbcHeader.gridy++;
            initColHeaderPane();
            colHeadPane.setViewportView(lossHeadPanel());
            rowHead.add(colHeadPane, gbcHeader);
            gbcHeader.gridy++;
            JLabel jl = new JLabel("");
            jl.setPreferredSize(new Dimension(180, 13));
            jl.setEnabled(false);
            rowHead.add(jl, gbcHeader);
            inited = true;
//        }
    }

    static boolean bColHeadInited = false;
    static JScrollPane colHeadPane;

    static void initColHeaderPane() {
        if (!bColHeadInited) {
            colHeadPane = new JScrollPane();
            colHeadPane.getVerticalScrollBar().setUnitIncrement(20);
            colHeadPane.setPreferredSize(new Dimension(250, 170));
            vScrollSync = new VScrollSync(colHeadPane);
            bColHeadInited = true;
        }
    }

    static JLabel sizedLabel(String name, Dimension d) {
        JLabel lab = new JLabel(name);
        lab.setPreferredSize(d);
        lab.setBorder(new LabelBorder());
        return lab;
    }

    static Insets headerIns = new Insets(1, 1, 1, 1);

    static class LabelBorder implements Border {
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(Color.LIGHT_GRAY);
            g.drawRect(x, y, width - 1, height - 1);
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Insets getBorderInsets(Component c) {
            return headerIns;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean isBorderOpaque() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    static void noteFiringModeChange(boolean bTopBot) {
        if (bTopBot) {
            rbTop.setText(strTopZones);
            rbBot.setEnabled(true);
            rbBot.setVisible(true);
        }
        else {
            rbTop.setText(strZones);
            rbBot.setEnabled(false);
            rbBot.setVisible(false);
            rbTop.setSelected(true);
        }
    }
    static JComponent sizeComponent(JComponent c, Dimension d, boolean shadeIt) {
        c.setPreferredSize(d);
        if (shadeIt)
            c.setBackground(Color.PINK);
        return c;
    }

    static void justShadeIt(JComponent c, boolean shadeIt) {
        if (shadeIt)
            c.setBackground(Color.PINK);
    }

    static JPanel lossHeadPanel() {
        JPanel pan = lossHeadP;
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(1, 0, 1, 0);
        LossTypeList lossTypeList = furnace.lossTypeList;
        Iterator<Integer> iter = lossTypeList.keysIter();
        Integer k;
        boolean shadeIt = false;
        JLabel lab;
        while (iter.hasNext()) {
            k = iter.next();
            gbcHeader.gridy++;
            lab = sizedLabel(lossTypeList.get(k).lossName, colHeadSize);
            if (shadeIt) {
                lab.setOpaque(true);
                lab.setBackground(Color.PINK);
            }
            pan.add(lab, gbcHeader);   //, colHeadSize)
            shadeIt = !shadeIt;
        }
        lossHeadP = pan;
        pan.validate();
        return pan;
    }

    static void showLossHeader() {
        lossHeadP.removeAll();
        lossHeadPanel();
    }

    static void addLossHeader() {
        showLossHeader();
    }

    public static FramedPanel getRowHeader(Component tfTopLen, Component tfBottLen) {
        initStaticData(tfTopLen, tfBottLen);
        ActionListener li = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object str = e.getSource();
                controller.changeTopBot(str == rbBot);
            }
        };
        rbBot.addActionListener(li);
        rbTop.addActionListener(li);
        return rowHead;
    }

    boolean enabled = false;
    boolean bActive = false;
    FceSection theSec;
    static DFHeating controller;
    public double length;
    public double width, stHeight, endHeight;
    double stLen, endLen;  // , slope;
    double temperature;
    double losses, fixedLoss;
    public double totLosses;
    double lossFactor = 1.0;
    public double wallArea, chEndWallArea, dischEndWallArea;  // all Walls
    public double roofArea, hearthArea;
    NumberTextField tfLength, tfStHeight, tfEndHeight, tfTemperature, tfFixedLosses;
    Hashtable<Integer, SecLossAssignment> secLossAssignment;

    JPanel subLossP = new JPanel(new GridBagLayout());
    FramedPanel detailsPanel;
    FceSection section;
    ProductionData productionData;
    boolean chEndSub = false, dischEndSub = false;     // location in Section
    int subNum;
    public FceSubSection(DFHeating controller, FceSection theSec, int subNum) {
        this(controller, theSec, 0, 0, 0, subNum);
    }

    public FceSubSection(DFHeating controller, FceSection theSec, double length, double stHeight, double endHeight, int subNum) {
        this.controller = controller;
        this.subNum = subNum;
        this.section = theSec;
        this.length = length;
        boolean allowZero = true;
        tfLength = new NumberTextField(controller, length * 1000, 6, false, 10, 50000, "#,###",
                "Subsection Length (mm)", allowZero);
        tfLength.addActionListener(controller.lengthChangeListener());
        tfLength.addFocusListener(controller.lengthFocusListener());
        this.stHeight = stHeight;
        tfStHeight = new NumberTextField(controller, stHeight * 1000, 6, false, 0, 10000, "#,###", "Subsection Start Height (mm)");
        this.endHeight = endHeight;
        tfEndHeight = new NumberTextField(controller, endHeight * 1000, 6, false, 0, 10000, "#,###", "Subsection End Height (mm)");
        temperature = 0;
        tfTemperature = new NumberTextField(controller, temperature, 6, false, 0, 1500, "#,###", "Subsection Temperature (C)");
        tfTemperature.setEditable(false);
        fixedLoss = 0;
        tfFixedLosses = new NumberTextField(controller, fixedLoss, 6, false, -1e6, 1e6, "#,###", "Subsection Fixed Losses (kcal/h)");
        secLossAssignment = new Hashtable<Integer, SecLossAssignment>();
        setValuesToUI();
        populateDetPanel();
    }

    // location in the Section
    void setLocInSec(boolean chEndSub, boolean dischEndSub) {
        this.chEndSub = chEndSub;
        this.dischEndSub = dischEndSub;
    }

    void resetLocInSec() {
        chEndSub = false;
        dischEndSub = false;
    }
    boolean enaEdit = true;

    public DFHFurnace getFurnace() {
        return furnace;
    }
    public void enableDataEntry(boolean ena) {
        enaEdit = ena;
        tfLength.setEditable(enaEdit);
        tfStHeight.setEditable(enaEdit);
        tfEndHeight.setEditable(enaEdit);
        tfTemperature.setEditable(enaEdit);
        tfFixedLosses.setEditable(enaEdit);
        tfTemperature.setEditable(bManualSecTemp && enaEdit);
        Enumeration ekey = secLossAssignment.keys();
        Object  k;
        SecLossAssignment jb;
        LossTypeList lossList = furnace.lossTypeList;
        while (ekey.hasMoreElements()) {
            k = ekey.nextElement();
            jb = secLossAssignment.get(k);
            jb.setEnabled(enaEdit && bActive && lossList.get((Integer)k).isValid());
        }
    }

    public void setDefaults() {
        changeData(0, 0, 0);
        temperature = 0;
        resetLocInSec();
        Enumeration ekey = secLossAssignment.keys();
        Object  k;
        JCheckBox jb;
        while (ekey.hasMoreElements()) {
            k = ekey.nextElement();
            jb = secLossAssignment.get(k);
            jb.setSelected(false);
         }
        setValuesToUI();
        isActive();
        enableIfOF();
        lossFactor = 1.0;
    }

    public void setLossFactor(double lossFactor) {
        this.lossFactor = lossFactor;
    }

    void changeData(double length, double stHeight, double endHeight) {
        this.length = length;
        this.stHeight = stHeight;
        this.endHeight = endHeight;
        resetLocInSec();
        setValuesToUI();
        isActive();
        enableIfOF();
        lossFactor = 1.0;
    }


    void informLchange() {
        tfLength.postActionEvent();
    }

    public void setProductionData(ProductionData productionData) {
        this.productionData = productionData;
    }

    public boolean takeDataFromXML(String xmlStr) {
        ValAndPos vp;
        try {
            vp = XMLmv.getTag(xmlStr, "length", 0);
            double l = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "stHeight", 0);
            double sH = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "endHeight", 0);
            double eH = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "temperature", 0);
            double t = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "fixedLoss", 0);
            double loss = Double.valueOf(vp.val);
            setTfFixedLosses(loss);
            vp = XMLmv.getTag(xmlStr, "lossFactor", 0);
            if (vp.val.length() > 1) {
                lossFactor = Double.valueOf(vp.val);
                if (lossFactor != 1.0)
                    controller.showError("Loss factor is " + lossFactor + " for " + section.sectionName() +
                        " sub section " + subNum);
            }
            else
                lossFactor = 1.0;
            changeData(l, sH, eH);
            setTemperature(t);
            Enumeration ekey = secLossAssignment.keys();
            Object k;
            SecLossAssignment jb;
            while (ekey.hasMoreElements()) {
                k = ekey.nextElement();
                jb = secLossAssignment.get(k);
                vp = XMLmv.getTag(xmlStr, "l" + ("" + k).trim(), 0);
                if (vp.val.length() == 1) {
                    jb.setSelected(vp.val.equals("1"));
                    jb.setFraction(1.0);
                }
                else
                    jb.takeDataFromXML(vp.val);
//                jb.setSelected((vp.val.equals("1") ? true : false));
            }
        } catch (NumberFormatException e) {
            errMsg("Number format in XML data");
            return false;
        }
        informLchange();
        return true;
    }

    boolean assignLoss(int lossID, double fraction) {
        SecLossAssignment secL = secLossAssignment.get(lossID);
        if (secL != null) {
            secL.setSelected(true);
            secL.setFraction(fraction);
            return true;
        }
        else
            return false;
    }

    public boolean assignLoss(double atPos, int lossID, double fraction) {
        boolean bRetVal = false;
        if (atPos >= stLen && atPos <= endLen)
            bRetVal = assignLoss(lossID, fraction);
        return bRetVal;
    }

    public boolean assignLoss(double stPos, double endPos, int lossID, double fraction) {
        boolean bRetVal = false;
        if (stPos < endLen && endPos > stLen) {
            if (stPos <= stLen && endPos >= endLen)
                bRetVal = assignLoss(lossID, fraction);
            else if (stPos <= stLen && endPos < endLen)   // a part
                bRetVal = assignLoss(lossID, (endPos - stLen) / (endLen - stLen) * fraction);
            else if (stPos > stLen && endPos >= endLen)
                bRetVal = assignLoss(lossID, (endLen - stPos) / (endLen - stLen) * fraction);
            else
                bRetVal = assignLoss(lossID, (endPos - stPos) / (endLen - stLen) * fraction);
        }
        return bRetVal;
    }

    void resetLossAssignment() {
        for (JCheckBox jb:secLossAssignment.values())
            jb.setSelected(false);
    }


    public void setTemperature(double t) {
        temperature = t;
        tfTemperature.setData(t);
        detailsPanel.updateUI();
    }

    public double getLenTemp() {  // length * temperature
        return (endLen -stLen) * temperature;
    }

    public void setTempForLosses(XYArray tProf) {
        setTemperature((tProf.getYat(stLen) + tProf.getYat(endLen)) / 2);
    }

    public void setTempForLosses(MultiColData tResults, int trace) {
        setTemperature((tResults.getYat(trace, stLen) + tResults.getYat(trace, endLen)) / 2);
    }

    boolean bManualSecTemp = false;
    void allowManuelTempForLosses(boolean bAllow) {
        bManualSecTemp = bAllow;
        tfTemperature.setEditable(bManualSecTemp && enaEdit);
    }


    void setTfFixedLosses(double l) {
        fixedLoss = l;
        tfFixedLosses.setData(l);
    }

    public void enableSubsection(boolean ena) {
        enabled = ena;
        if (!enabled)
            tfLength.setText("0");
        tfLength.setEnabled(enabled);
        isActive();
        enableIfOF();
    }

    public void enableIfOF() {
        tfStHeight.setEnabled(bActive);
        tfEndHeight.setEnabled(bActive);
        tfFixedLosses.setEnabled(bActive);
        tfTemperature.setEnabled(bActive);
        LossTypeList lossList = furnace.lossTypeList;
        Enumeration ekey = secLossAssignment.keys();
        Object  i;
        SecLossAssignment jb;
        while (ekey.hasMoreElements()) {
            i = ekey.nextElement();
            jb = secLossAssignment.get(i);
            jb.setEnabled(bActive && lossList.get((Integer)i).isValid());
        }
    }

    public boolean isActive() {
        takeValuesfromUI();
        bActive = enabled && (length > 0);
        return bActive;
    }

    void setValuesToUI() {
        tfLength.setData(length * 1000);
        tfStHeight.setData(stHeight * 1000);
        tfEndHeight.setData(endHeight * 1000);
        tfTemperature.setData(temperature);
        isActive();
    }

    public boolean getReadyToCalcul() {
        takeValuesfromUI();
        calculAreas();
        updateLossStatus();
        calculLosses(temperature);
//        slope = (endHeight - stHeight) / length;
        return true;
    }

    public double getRoofSlope() {
        return (endHeight - stHeight) / length;
    }

    String checkData() {
        String retVal = "";
        if ((stHeight + endHeight) / 2 <= 0)
            retVal = "Section Height is 0";
        return retVal;
    }

    void redoLosses() {
        updateLossStatus();
        calculLosses(temperature);
    }

    void populateDetPanel() {
        noteLossList();
        detailLossPanel();
        detailsPanel = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        detailsPanel.add(tfLength, gbc);
        gbc.gridy++;
        detailsPanel.add(tfStHeight, gbc);
        gbc.gridy++;
        detailsPanel.add(tfEndHeight, gbc);
        gbc.gridy++;
//        detailsPanel.add(tfTemperature, gbc);
//        gbc.gridy++;
        detailsPanel.add(subLossP, gbc);
        isActive();
        enableIfOF();
    }

    JPanel detailLossP;

    JScrollPane spDetLoss;

    JPanel detailLossPanel() {
        JPanel pan = subLossP;
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(1, 0, 1, 0);
        pan.add(sizeComponent(new JLabel(""), colDataSize, true), gbc);
        gbc.gridy++;
        pan.add(tfFixedLosses, gbc);
        gbc.gridy++;

        spDetLoss = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        spDetLoss.getVerticalScrollBar().setUnitIncrement(20);
        spDetLoss.setPreferredSize(new Dimension(70, 170));
        initColHeaderPane();
        vScrollSync.add(spDetLoss);
        JPanel lPan = new JPanel(new GridBagLayout());
        GridBagConstraints gbcLP = new GridBagConstraints();
        gbcLP.insets = new Insets(1, 0, 1, 0);
        gbcLP.gridx = 0;
        gbcLP.gridy = 0;
        LossTypeList lossList = furnace.lossTypeList;
        Iterator<Integer> iter = lossList.keysIter();
        Object k;
        while (iter.hasNext()) {
            k = iter.next();
//            lPan.add(lossCheckBox.get(k), gbcLP);
            lPan.add(secLossAssignment.get(k).getIUPanel(), gbcLP);
            gbcLP.gridy++;
        }
        spDetLoss.setViewportView(lPan);
        pan.add(spDetLoss, gbc);
        vScrollSync.syncPos();
        detailLossP = pan;
        pan.updateUI();
        return pan;
    }

    public FramedPanel getFceSubDetFrame() {
        return detailsPanel;
    }

    public JComponent getTfLength() {
        return tfLength;
    }

    public JComponent getTfStHeight() {
        return tfStHeight;
    }

    public JComponent getTfEndHeight() {
        return tfEndHeight;
    }

    public JComponent getTfTemperature() {
        return tfTemperature;
    }

    public JComponent getTfFixedLosses() {
        return tfFixedLosses;
    }

    public void takeValuesfromUI() {
        try {
            length = tfLength.getData()/ 1000;
            stHeight = tfStHeight.getData() / 1000;
            endHeight = tfEndHeight.getData() / 1000;
            temperature = tfTemperature.getData();
            fixedLoss = tfFixedLosses.getData();
            takeLossAssignFromUI();
        } catch (NumberFormatException e) {
            fixedLoss = 0;
        }
    }


    public ErrorStatAndMsg isSubSectionOK() {
        boolean ok = true;
        String msg = "";
        takeValuesfromUI();
        if (length > 0) {
            if (tfLength.isInError()) {
                ok = false;
                msg += nlSpace + tfLength.getName();
            }
            if  (tfStHeight.isInError()) {
                ok = false;
                msg += nlSpace + tfLength.getName();
            }
            if  (tfEndHeight.isInError()) {
                ok = false;
                msg += nlSpace + tfEndHeight.getName();
            }
            if  (tfFixedLosses.isInError()) {
                ok = false;
                msg += nlSpace + tfFixedLosses.getName();
            }
            if  (tfTemperature.isInError()) {
                ok = false;
                msg += nlSpace + tfTemperature.getName();
            }
        }
        return new ErrorStatAndMsg(!ok, msg);
    }

    String fmtDouble(double val, String fmt) {
        return new DecimalFormat(fmt).format(val);
    }

    String formatNumber(double value) {
        String fmt = "";
        double absVal = Math.abs(value);
        if (absVal == 0)
            fmt = "#";
        else if (absVal < 0.001 || absVal > 1e5)
            fmt = "#.###E00";
        else if (absVal > 100)
            fmt = "###,###";
        else
            fmt = "###.###";
        return fmtDouble(value, fmt);


    }

    public double getEndLen(double stLen) {
        this.stLen = stLen;
        endLen = SPECIAL.roundToNDecimals(stLen + length, 3);
        return endLen;
    }

    public double getEndLen() {
        return endLen;
    }


    public void noteLossList() {
         LossTypeList lossList = furnace.lossTypeList;
         Iterator<Integer> iter = lossList.keysIter();
         Integer lossID;
         SecLossAssignment oneLassign;
         boolean shadeIt = false;
         while (iter.hasNext()) {
             lossID = iter.next();
             if (!secLossAssignment.containsKey(lossID)) {
                 oneLassign = new SecLossAssignment(colDataSize);
                 justShadeIt(oneLassign, shadeIt);
                 if (!lossList.get(lossID.intValue()).isValid())
                     oneLassign.setEnabled(false);
                 secLossAssignment.put(lossID, oneLassign);
             }
             shadeIt = !shadeIt;
         }
         enableIfOF();
     }

    void takeLossAssignFromUI() {
        LossTypeList lossList = furnace.lossTypeList;
        Iterator<Integer> iter = lossList.keysIter();
        Integer lossID;
        SecLossAssignment oneLassign;
        while (iter.hasNext()) {
            lossID = iter.next();
            if (secLossAssignment.containsKey(lossID))  {
                oneLassign = secLossAssignment.get(lossID);
                oneLassign.takeFromUI();
            }
        }
    }

    public void noteLossListChange() {
        enableIfOF();
        LossTypeList lossList = furnace.lossTypeList;
        Iterator<Integer> iter = lossList.keysIter();
        Integer lossID;
        SecLossAssignment oneLassign;
        while (iter.hasNext()) {
            lossID = iter.next();
            if (secLossAssignment.containsKey(lossID))  {
                oneLassign = secLossAssignment.get(lossID);
                if (!lossList.get(lossID).isValid())
                    oneLassign.setEnabled(false);
            }
        }
        enableIfOF();
    }

    static Vector <XLcellData> lossDetRowHeadXL;


    static JPanel lossDetailTopRowHead() {
        lossDetRowHeadXL = new Vector<XLcellData>();
        SizedLabel lab;
        JPanel outerP = new JPanel(new GridBagLayout());
        GridBagConstraints outerGbc = new GridBagConstraints();
        outerGbc.gridx = 0;
        outerGbc.gridy = 0;
        outerGbc.insets = new Insets(1, 0, 1, 0);
        lab = new SizedLabel("Zone.subSection", colHeadSize);
        lossDetRowHeadXL.add(lab);
        outerP.add(lab, outerGbc);
        outerGbc.gridy++;
        lab = new SizedLabel("Zone Type", colHeadSize);
        lossDetRowHeadXL.add(lab);
        outerP.add(lab, outerGbc);
        outerGbc.gridy++;
        lab = new SizedLabel("Starting position (mm)", colHeadSize);
        lossDetRowHeadXL.add(lab);
        outerP.add(lab, outerGbc);
        outerGbc.gridy++;
        lab = new SizedLabel("Length (mm)", colHeadSize);
        lossDetRowHeadXL.add(lab);
        outerP.add(lab, outerGbc);
        outerGbc.gridy++;
        lab = new SizedLabel("Temperature (C))", colHeadSize);
        lossDetRowHeadXL.add(lab);
        outerP.add(lab, outerGbc);
        outerGbc.gridy++;
        lab = new SizedLabel("Lateral Wall Area (m2)", colHeadSize);
        lossDetRowHeadXL.add(lab);
        outerP.add(lab, outerGbc);
        outerGbc.gridy++;
        lab = new SizedLabel("Roof Area (m2)", colHeadSize);
        lossDetRowHeadXL.add(lab);
        outerP.add(lab, outerGbc);
        outerGbc.gridy++;
        lab = new SizedLabel("Hearth Area (m2)", colHeadSize);
        lossDetRowHeadXL.add(lab);
        outerP.add(lab, outerGbc);
        outerGbc.gridy++;
        lab = new SizedLabel("End Wall Area (m2)", colHeadSize);
        lossDetRowHeadXL.add(lab);
        outerP.add(lab, outerGbc);
        outerGbc.gridy++;
        lab = new SizedLabel("Total Losses (kcal/h)", colHeadSize, true);
        lossDetRowHeadXL.add(lab);
        outerP.add(lab, outerGbc);
        outerGbc.gridy++;
        outerP.add(furnace.lossTypeList.activeLossRowHead(lossDetRowHeadXL), outerGbc);
        outerGbc.gridy++;
        lab = new SizedLabel(" ", colHeadSize, true);
        outerP.add(lab, outerGbc);

        return outerP;
    }

    Vector<XLcellData> lossDetailsXL;
    JPanel lossValueDetPan(VScrollSync master) {
        lossDetailsXL = new Vector<XLcellData>();
        NumberLabel lab;
        JPanel outerP = new JPanel(new GridBagLayout());
        GridBagConstraints outerGbc = new GridBagConstraints();
        outerGbc.gridx = 0;
        outerGbc.gridy = 0;
        outerGbc.insets = new Insets(1, 0, 1, 0);
        Dimension dim =new Dimension(70, 20);
        SizedLabel sizedLab;
        sizedLab = new SizedLabel("Zone#" + section.secNum + "." + subNum, dim);
        lossDetailsXL.add(sizedLab);
        outerP.add(sizedLab,outerGbc);
        outerGbc.gridy++;
        sizedLab = new SizedLabel(((section.bRecuType) ? "RECU" : "BURNER"), dim);
        lossDetailsXL.add(sizedLab);
        outerP.add(sizedLab,outerGbc);
        outerGbc.gridy++;
        lab = new NumberLabel(stLen * 1000, 70, "#,##0");
        lossDetailsXL.add(lab);
        outerP.add(lab,outerGbc);
        outerGbc.gridy++;
        lab = new NumberLabel(length * 1000, 70, "#,##0");
        lossDetailsXL.add(lab);
        outerP.add(lab,outerGbc);
        outerGbc.gridy++;
        lab = new NumberLabel(temperature, 70, "#,##0");
        lossDetailsXL.add(lab);
        outerP.add(lab,outerGbc);
        outerGbc.gridy++;
        lab = new NumberLabel(wallArea, 70, "#,##0.00");
        lossDetailsXL.add(lab);
        outerP.add(lab,outerGbc);
        outerGbc.gridy++;
        lab = new NumberLabel(roofArea, 70, "#,##0.00");
        lossDetailsXL.add(lab);
        outerP.add(lab,outerGbc);
        outerGbc.gridy++;
        lab = new NumberLabel(hearthArea, 70, "#,##0.00");
        lossDetailsXL.add(lab);
        outerP.add(lab,outerGbc);
        outerGbc.gridy++;
        double endArea = 0;
        if (dischEndSub)
            endArea = dischEndWallArea;
        if (chEndSub)
            endArea = chEndWallArea;

        lab = new NumberLabel(endArea, 70, "#,##0.00");
        lossDetailsXL.add(lab);
        outerP.add(lab,outerGbc);
        outerGbc.gridy++;
        lab = new NumberLabel(totLosses, 70, "#,##0", true);
        lossDetailsXL.add(lab);
        outerP.add(lab,outerGbc);
        outerGbc.gridy++;

        outerP.add(getLossListWithVal().lossValuesPanel(master, lossDetailsXL), outerGbc);
        return outerP;
    }

    //getting non-section specific list (like summery of top zones, bottom zones, total etc
    static JPanel lossValueDetPan(LossListWithVal lossWithVal, Vector<XLcellData> detForXL, VScrollSync master) {
        JPanel outerP = new JPanel(new GridBagLayout());
        GridBagConstraints outerGbc = new GridBagConstraints();
        outerGbc.gridx = 0;
        outerGbc.gridy = 0;
        outerGbc.insets = new Insets(1, 0, 1, 0);
        Dimension dim =new Dimension(70, 20);
        SizedLabel sizedLab;
        for (int r = 0; r < 9; r++) {    // all blanks
            sizedLab = new SizedLabel("", dim);
            detForXL.add(sizedLab);
            outerP.add(sizedLab,outerGbc);
            outerGbc.gridy++;
        }
        NumberLabel lab = new NumberLabel(lossWithVal.getTotal(), 70, "#,##0", true);
        detForXL.add(lab);
        outerP.add(lab,outerGbc);
        outerGbc.gridy++;
        outerP.add(lossWithVal.lossValuesPanel(master, detForXL), outerGbc);
        return outerP;
    }

    public int xlSecLossDetails(Sheet sheet, ExcelStyles style, int topRow, int leftCol) {
        sheet.setColumnWidth(leftCol, 3000);
        style.xlAddXLCellData(sheet, topRow, leftCol, lossDetailsXL);
        return leftCol + 1;
    }

    static public int xlSecLossDetails(Sheet sheet, ExcelStyles style, int topRow, int leftCol, Vector<XLcellData> xlDat) {
        sheet.setColumnWidth(leftCol, 3000);
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, xlDat) + 1;
        return topRow;
    }

    static public int xlLossRowHead(Sheet sheet, ExcelStyles style, int topRow, int leftCol) {
        sheet.setColumnWidth(leftCol, 9000);
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, lossDetRowHeadXL) + 1;
        return topRow;

    }


    public boolean addToProfileTrace(Vector<DoublePoint> vProf, double scale, double baseY) {
        boolean bRetVal = true;
        vProf.add(new DoublePoint(stLen, baseY + stHeight * scale));
        vProf.add(new DoublePoint(endLen, baseY + endHeight * scale));
        return bRetVal;
    }

    public JComponent getLossCheckBox(Integer lossID) {
//        return lossCheckBox.get(lossID);
        return secLossAssignment.get(lossID);
    }

    public String dataInXML() {
        String xmlStr = XMLmv.putTag("length", "" + length) +
                        XMLmv.putTag("stHeight", "" + stHeight ) +
                XMLmv.putTag("endHeight", "" + endHeight) +
                XMLmv.putTag("temperature", "" + temperature) +
                XMLmv.putTag("fixedLoss", "" + fixedLoss) +
                XMLmv.putTag("lossFactor", "" + lossFactor);
        String lossStat = "";
        Enumeration ekey = secLossAssignment.keys();
        Object  k;
        SecLossAssignment jb;
        while (ekey.hasMoreElements()) {
            k = ekey.nextElement();
            jb = secLossAssignment.get(k);
            lossStat += XMLmv.putTag("l" + ("" + k).trim(), jb.dataInXML());
        }
        xmlStr += XMLmv.putTag("lossCB", lossStat);
        return xmlStr;
    }

    void updateLossStatus() {
        LossTypeList lossList = furnace.lossTypeList;
        Integer lossID;
        Iterator<Integer> iter = lossList.keysIter();
        SecLossAssignment oneLassign;
        while (iter.hasNext()) {
            lossID = iter.next();
            oneLassign = secLossAssignment.get(lossID);
            if (oneLassign != null && oneLassign.isSelected())
                oneLassign.setTypeAndVal(lossID, lossList.get(lossID), true);
        }
    }


    public void setFixedLoss(double fixedLoss) {
        this.fixedLoss = fixedLoss;
    }

    public void setSection(FceSection section) {
        this.section = section;
    }

    void calculAreas() {
        wallArea = 2 * length * (stHeight + endHeight) / 2;
        chEndWallArea = 0;
        dischEndWallArea = 0;
        if (dischEndSub)
            dischEndWallArea = furnace.width * endHeight;
        if (chEndSub)
            chEndWallArea = furnace.width * stHeight;
        if (section.botSection) {
            roofArea = length * furnace.width;
            hearthArea = Math.sqrt(Math.pow(length, 2) + Math.pow((stHeight - endHeight), 2)) * furnace.width;
        }
        else {
            hearthArea = length * furnace.width;
            roofArea = Math.sqrt(Math.pow(length, 2) + Math.pow((stHeight - endHeight), 2)) * furnace.width;
        }
    }


    public double calculLosses(double temp) {
        losses = 0;
        for (SecLossAssignment oneLassign: secLossAssignment.values())
            losses += oneLassign.calculateLoss(this, temp);
        totLosses = lossFactor * (losses + fixedLoss);
        return totLosses;
    }

    public LossListWithVal getLossListWithVal() {
        LossListWithVal list;
        list = new LossListWithVal(furnace.lossTypeList);
        for (SecLossAssignment onelAssign: secLossAssignment.values())
            onelAssign.addToLossList(list);
        list.add(LossTypeList.FIXEDLOSS, fixedLoss);
        list.add(LossTypeList.INERNALRADLOSS, 0);
        return list;
    }

    void errMsg(String msg) {
        System.out.println("FceSubSection: ERROR - " + msg);
    }

    class SecLossAssignment extends JCheckBox {
        LossTypeAndVal typeAndVal;
        JPanel jp;
        SecLossAssignment(Dimension dim) {
            super();
            Dimension nowDim = getPreferredSize();
            nowDim = new Dimension(nowDim.width, dim.height);
            setPreferredSize(nowDim);
            typeAndVal = new LossTypeAndVal(controller);
            prepareJP();
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    enableFraction();
                }
            });
        }

        void enableFraction() {
            typeAndVal.enableFraction(isEnabled() && isSelected());
        }

        boolean setFraction(double fraction)  {
            return typeAndVal.setFraction(fraction);
        }

        double getFraction() {
            return typeAndVal.getFraction();
        }

        void takeFromUI() {
            typeAndVal.takeFromUI();
        }

        String dataInXML() {
            String xmlStr = XMLmv.putTag("s", ((isSelected()) ? "1" : "0"));
            xmlStr += XMLmv.putTag("f", typeAndVal.getFraction());
            return xmlStr;
        }

        void takeDataFromXML(String xmlStr) {
            ValAndPos vp;
            vp = XMLmv.getTag(xmlStr, "s", 0);
            setSelected(vp.val.equals("1"));
            try {
                vp = XMLmv.getTag(xmlStr, "f", 0);
                double f = Double.valueOf(vp.val);
                typeAndVal.setFraction(f);
            } catch (NumberFormatException e) {
                typeAndVal.setFraction(1.0);
            }
        }

        void setTypeAndVal(int lossID, LossType lossType, boolean noteTemperature)  {
            typeAndVal.setParams(lossID, lossType, noteTemperature);
        }

        double calculateLoss(FceSubSection subSec, double temp) {
            double loss = 0;
            if (isSelected()) {
                loss = typeAndVal.calculateLoss(subSec, temp);
            }
            return loss;
        }

        void addToLossList(LossListWithVal list) {
            if (isSelected())
                list.add(typeAndVal);
        }

        public void setBackground(Color color) {
            super.setBackground(color);
            if (typeAndVal != null)
                typeAndVal.getFractionUI().setNormalBackground(color);
        }

        public void setEnabled(boolean ena) {
            super.setEnabled(ena);
            if (typeAndVal != null) {
                enableFraction();
//                NumberTextField nt = typeAndVal.getFractionUI();
//                nt.setEnabled(ena);
            }
        }

        void prepareJP() {
            jp = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            jp.add(this, gbc);
            gbc.gridx++;
            jp.add(typeAndVal.getFractionUI(), gbc);
        }

        JPanel getIUPanel() {
            return jp;
        }
     }
}
