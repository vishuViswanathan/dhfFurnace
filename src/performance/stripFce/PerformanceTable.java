package performance.stripFce;

import basic.CheckInRange;
import basic.TwoDTable;
import directFiredHeating.DFHFurnace;
import directFiredHeating.DFHeating;
import display.MultiPairColPanel;
import display.NumberTextField;
import mvXML.ValAndPos;
import mvXML.XMLmv;
import mvmath.FramedPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Hashtable;
import java.util.TimerTask;
import java.util.Vector;

/**
 * Created by IntelliJDEA.
 * User: M Viswanathan
 * Date: 4/9/14
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class PerformanceTable {
    Hashtable<Performance.Params, TwoDTable> commTables;
    Vector<ZonalTable> zonalTablesT;
    Vector<ZonalTable> zonalTablesB;
    Performance baseP;
    DFHeating control;
    String colHeadName = "Output";
    String rowHeadName = "Width";
    DFHFurnace furnace;

    public PerformanceTable(Performance baseP, String xmlStr) throws Exception {
        init(baseP);
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "outputPT", 0);
        if (vp.val.length() > 20) {
            TwoDTable outputT = splTwoDTable(vp.val, "Performance Table, output");
//            outputT.setColAndRowHeadNames(colHeadName, rowHeadName);
            outputT.setFormats("0.00", "0.000", "0.00");
            commTables.put(Performance.Params.OUTPUT, outputT);
            vp = XMLmv.getTag(xmlStr, "airTempPT", vp.endPos);
            if (vp.val.length() > 20) {
                TwoDTable airTempT = splTwoDTable(vp.val, "Performance Table, airTemp");
//                airTempT.setColAndRowHeadNames(colHeadName, rowHeadName);
                commTables.put(Performance.Params.AIRTEMP, airTempT);
                setFormats();
                vp = XMLmv.getTag(xmlStr, "nTopZonesPT", vp.endPos);
                int nTopZones = Integer.valueOf(vp.val);
                for (int zN = 0; zN < nTopZones; zN++) {
                    vp = XMLmv.getTag(xmlStr, "zTPT" + ("" + zN).trim(), vp.endPos);
                    zonalTablesT.add(new ZonalTable(false, (zN + 1), vp.val));
                }
                vp = XMLmv.getTag(xmlStr, "nTopZonesPT", vp.endPos);
                if (vp.val.length() > 20) {
                    int nBotZones = Integer.valueOf(vp.val);
                    for (int zN = 0; zN < nBotZones; zN++) {
                        vp = XMLmv.getTag(xmlStr, "zBPT" + ("" + zN).trim(), vp.endPos);
                        zonalTablesB.add(new ZonalTable(false, (zN + 1), vp.val));
                    }
                }
            }
        }
    }

    public PerformanceTable(Performance baseP, double[] capFactors, double[] width) {
        init(baseP);
        TwoDTable outputT = new TwoDTable(capFactors, width);
        outputT.setColAndRowHeadNames(colHeadName, rowHeadName);
        commTables.put(Performance.Params.OUTPUT, outputT);
        TwoDTable airTempT = new TwoDTable(capFactors, width);
        airTempT.setColAndRowHeadNames(colHeadName, rowHeadName);
        commTables.put(Performance.Params.AIRTEMP, airTempT);
        setFormats();
        for (int z = 0; z < baseP.topZones.size(); z++){
            zonalTablesT.add(new ZonalTable(false, (z + 1), capFactors, width));
        }
        if (baseP.botZones != null) {
            for (int z = 0; z < baseP.botZones.size(); z++){
                zonalTablesB.add(new ZonalTable(true, (z + 1), capFactors, width));
            }
        }
        addToTable(1, 1, baseP, true);
    }

    private void init(Performance baseP) {
        this.baseP = baseP;
        furnace = baseP.furnace;
        control = baseP.controller;
        commTables = new Hashtable<Performance.Params, TwoDTable>();
        zonalTablesT = new Vector<ZonalTable>();
        if (baseP.botZones != null)
            zonalTablesB = new Vector<ZonalTable>();
    }

    public int nZones(boolean bBot) {
        return baseP.nZones(bBot);
    }

    public ZonalTable getZonalTable(int zoneNum, boolean bBot) {
        return ((bBot) ? zonalTablesB : zonalTablesT).get(zoneNum);
    }

    TwoDTable splTwoDTable(String inStr, String cMsg) throws Exception{
        TwoDTable table = new TwoDTable(inStr, cMsg);
        table.setColAndRowHeadNames(colHeadName, rowHeadName);
        return table;
    }

    private void setFormats() {
        commTables.get(Performance.Params.OUTPUT).setFormats("0.00", "0.000", "0.00");
        commTables.get(Performance.Params.AIRTEMP).setFormats("0.00", "0.000", "0.00");
    }

    public boolean addToTable(double width, double outputFactor, Performance p) {
        return addToTable(width, outputFactor, p, false);
    }

    public boolean addToTable(double width, double outputFactor, Performance p, boolean bBase) {
        boolean itsOk = false;
        if (bBase || checkIfInRange(p)) {
            if (commTables.get(Performance.Params.OUTPUT).setOneValue(width, outputFactor, (p.output)))
                if (commTables.get(Performance.Params.AIRTEMP).setOneValue( width, outputFactor, p.airTemp))
                    itsOk = true;
        }
        int zNum = 0;
        if (itsOk) {
            for (ZonalTable zT:zonalTablesT) {
                itsOk = zT.addToTable(width, outputFactor, p.topZones.get(zNum++));
                if (!itsOk)
                    break;
            }
            if (zonalTablesB != null) {
                if (itsOk)  {
                    zNum = 0;
                    for (ZonalTable zT:zonalTablesB) {
                        itsOk = zT.addToTable(width, outputFactor, p.botZones.get(zNum++));
                        if (!itsOk)
                            break;
                    }
                }
            }
        }
        return itsOk;
    }

    boolean stripWidthOK(double width) {
        TwoDTable oneTable = commTables.get(Performance.Params.OUTPUT) ;
        return (oneTable.IsRowHeadInRange(width));
    }

    boolean outputFactorOK( double outputFactor) {
        TwoDTable oneTable = commTables.get(Performance.Params.OUTPUT) ;
        return (oneTable.IsColHeadInRange(outputFactor));
    }

    public double getLimitedWidth(double stripWidth) {
        TwoDTable oneTable = commTables.get(Performance.Params.OUTPUT) ;
        CheckInRange checkResult = oneTable.checkRowHeadInRange(stripWidth);
        if (checkResult.inRange)
            return stripWidth;
        else
            return checkResult.limitVal;
    }

    public double getLimitedOutputFactor(double outputFactor) {
        TwoDTable oneTable = commTables.get(Performance.Params.OUTPUT) ;
        CheckInRange checkResult = oneTable.checkColHeadInRange(outputFactor);
        if (checkResult.inRange)
            return outputFactor;
        else
            return checkResult.limitVal;
    }

    public boolean isWidthInRange(double stripWidth) {
        return stripWidthOK(stripWidth);
    }

    public double getMaxOutputFactor() {
        TwoDTable oneTable = commTables.get(Performance.Params.OUTPUT) ;
        return (oneTable.getMaxColHead());
    }

    public double getMinOutputFactor() {
        TwoDTable oneTable = commTables.get(Performance.Params.OUTPUT) ;
        return (oneTable.getMinColHead());
    }

    public boolean fillInterpolatedData(InterpolatedParams iParams, double width, double outputFactor, double thickness)  {
        boolean allOk = true;
        if (stripWidthOK(width) && outputFactorOK(outputFactor)) {
            iParams.takeFromPerfTable(this, width, outputFactor, thickness);
            try {
                iParams.airTemp = commTables.get(Performance.Params.AIRTEMP).getData(outputFactor, width);
//            iParams.speed = commTables.get(Performance.Params.STRIPSPEED).getData(outputFactor, widthFactor);
            } catch (Exception e) {
                allOk = false;
                showError("Problem in filling interpolated data of air temperature or speed, " + e.getMessage());
            }
            if (allOk) {
                int zNum = 0;
                for (ZonalTable tz : zonalTablesT) {
                    OneZone oneZone = new OneZone(false, baseP.topZones.get(zNum).bRecuType);
                    allOk = tz.fillInterpolatedZoneData(oneZone, width, outputFactor);
                    if (!allOk)
                        break;
                    iParams.addToZones(false, oneZone);
                    zNum++;
                }
                if (allOk) {
                    zNum = 0;
                    if (zonalTablesB != null)
                        for (ZonalTable bz : zonalTablesB) {
                            OneZone oneZone = new OneZone(true, baseP.botZones.get(zNum).bRecuType);
                            allOk = bz.fillInterpolatedZoneData(oneZone, width, outputFactor);
                            if (!allOk)
                                break;
                            iParams.addToZones(true, oneZone);
                            zNum++;
                        }
                }
            }
        }
        else
            allOk = false;
        return allOk;
    }

    public boolean getOperationData(InterpolatedParams iParams, double width, double output, double thickness)  {
        double widthFactor = width / baseP.chLength;
        double outputFactor = output / baseP.output / widthFactor;
        return fillInterpolatedData(iParams, width, outputFactor, thickness);
    }

    public boolean getOperationData(InterpolatedParams iParams, double width, double outputFactor)  {
        outputFactor = getLimitedOutputFactor(outputFactor);
        return fillInterpolatedData(iParams, width, outputFactor, baseP.chThick);
    }

    StringBuilder dataInXML() {
        TwoDTable outputT = commTables.get(Performance.Params.OUTPUT);
        StringBuilder xmlStr = new StringBuilder(XMLmv.putTagNew("outputPT", outputT.dataForTextFile()));
        TwoDTable airTempT = commTables.get(Performance.Params.AIRTEMP);
        xmlStr.append(XMLmv.putTagNew("airTempPT",airTempT.dataForTextFile()));
        xmlStr.append(XMLmv.putTagNew("nTopZonesPT", zonalTablesT.size()));
        int zN = 0;
        for (ZonalTable zt: zonalTablesT) {
            xmlStr.append(XMLmv.putTagNew("zTPT" + ("" + zN).trim(), zt.dataInXML()));
            zN++;
        }
        if (zonalTablesB != null) {
            xmlStr.append(XMLmv.putTagNew("nBotZonesPT", zonalTablesB.size()));
            zN = 0;
            for (ZonalTable zb: zonalTablesB) {
                xmlStr.append(XMLmv.putTagNew("zBPT" + ("" + zN).trim(), zb.dataInXML()));
                zN++;
            }
        }
        return xmlStr;
    }

    boolean checkIfInRange(Performance p) {
        return ((p.chMaterial.equalsIgnoreCase(baseP.chMaterial)) &&
                (p.fuelName.equalsIgnoreCase(baseP.fuelName)) &&
                (p.chWidth <= baseP.chWidth) &&
                (p.unitOutput <= baseP.unitOutput) &&
                (Math.abs(p.exitTemp() - baseP.exitTemp()) <= 1)
        );
    }

    public JPanel perfTableP() {
        FramedPanel outerP = new FramedPanel(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Output", null, commDataP(Performance.Params.OUTPUT),
                        "Output in kg/h");
        tabbedPane.addTab("Air Temp", null, commDataP(Performance.Params.AIRTEMP),
                "Preheat Air Temperature");
        tabbedPane.addTab(baseP.furnace.topBotName(false) + "Zones", null, zonalPanel(false));
        if (zonalTablesB != null)
            tabbedPane.addTab("Bottom Zones", null, zonalPanel(true));
        outerP.add(tabbedPane, BorderLayout.NORTH);
//        outerP.add(tableSelPanel(), BorderLayout.SOUTH);
        return outerP;
    }

    NumberTextField ntWidth;
    NumberTextField ntOutput;
    NumberTextField ntThick;
    JButton buttGetData;
    JPanel tableSelP;
    int outputSteps = 16;

    void showSelDataSet() {
        if (ntWidth.isInError() || ntOutput.isInError())
            showError("Select parameters within Range");
        else {
            double width = ntWidth.getData();
            double outputFactor = ntOutput.getData();
            double thickness = ntThick.getData() / 1000;
            InterpolatedParams interpolatedParams = new InterpolatedParams(baseP.furnace);
            if (fillInterpolatedData(interpolatedParams, width, outputFactor, thickness)) {
                baseP.fillPerformanceP(interpolatedParams);
                ZonalFuelProfile fuelProfile = null;
                try {
                    fuelProfile = new ZonalFuelProfile(this, width, thickness, outputSteps, control);
                    baseP.fillFuelProfile(fuelProfile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else
                showError("ShowDataSet:Facing some problem in filling interpolated data\nEither Width and/or output is out of Range");
        }
    }

    void greyPerformanceP() {
        baseP.greyPerformanceP();
    }

    JPanel tableSelPanel() {
        TwoDTable tempT =  commTables.get(Performance.Params.AIRTEMP);
        double maxW =  tempT.getMaxRowHead();
        double maxS = tempT.getMaxColHead();
        if (tableSelP == null) {
//            double baseW = baseP.chLength;
            double minW = tempT.getMinRowHead();
//            double baseS = baseP.speed;
            double minS = tempT.getMinColHead();
            FocusListener li = new FocusListener() {
                public void focusGained(FocusEvent e) {
                    greyPerformanceP();
                }

                public void focusLost(FocusEvent e) {

                }
            };
            ntWidth = new NumberTextField(control, maxW, 6, false, minW, maxW, tempT.rowHeadFmtStr, "Selected Width (m)");
            ntWidth.addFocusListener(li);
            ntOutput = new NumberTextField(control, maxS, 6, false, minS, maxS, tempT.colHeadFmtStr, "Selected output Factor");
            ntOutput.addFocusListener(li);
            ntThick = new NumberTextField(control, baseP.chThick * 1000, 6, false, 0.01, 10.0, "#0.00", "Select thickness (mm)");
            ntThick.addFocusListener(li);
            buttGetData = new JButton("Get Data");
            buttGetData.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showSelDataSet();
                }
            });
            tableSelP = new JPanel(new BorderLayout());
            MultiPairColPanel innerP = new MultiPairColPanel("");
            innerP.addItemPair(ntWidth);
            innerP.addItemPair(ntOutput);
            innerP.addItemPair(ntThick);
            innerP.addItemPair("", buttGetData);
            tableSelP.add(new JLabel("Performance Data For "), BorderLayout.NORTH);
            tableSelP.add(innerP, BorderLayout.CENTER);
//            tableSelP.add(buttGetData, BorderLayout.EAST);
        }
        else {
            ntWidth.setData(maxW);
            ntOutput.setData(maxS);
            ntThick.setData(baseP.chThick * 1000);
        }
        return tableSelP;
    }

    JPanel commDataP(Performance.Params forParam) {
        JPanel jp = new JPanel();
        JScrollPane sP = new JScrollPane(commTables.get(forParam).getTable());
        sP.setPreferredSize(new Dimension(400, 150));
        jp.add(sP);
        return jp;
    }

    JPanel zonalPanel(boolean bBot) {
        JPanel jp = new JPanel();
        JTabbedPane tabbedPane = new JTabbedPane();
        Vector<ZonalTable> vZonal = (bBot) ? zonalTablesB : zonalTablesT;
        int z = 0;
        for (ZonalTable zT: vZonal) {
            z++;
            tabbedPane.addTab("Zone#" + ("" + z).trim(), null, zT.perfTableP(),
                    ((bBot) ? "Bottom" : "Top") + " Zone#" + ("" + z).trim() + " Data");
        }

        jp.add(tabbedPane);
        return jp;
    }

    void showError(String msg) {
        Frame frame = baseP.controller.parent();
        JOptionPane.showMessageDialog(frame, "PerformanceTable:" + msg, "ERROR", JOptionPane.ERROR_MESSAGE);
        frame.toFront();
     }

    void showError(String msg, int forTime) {
        Frame frame = baseP.controller.parent();
        JOptionPane pane = new JOptionPane("PerformaceTable:" + msg, JOptionPane.ERROR_MESSAGE);
        JDialog dialog = pane.createDialog(frame, "ERROR");
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new CloseDialogTask(dialog), forTime);
        dialog.setVisible(true);
    }

    class CloseDialogTask extends TimerTask {
        JDialog dlg;
        CloseDialogTask(JDialog dlg) {
            this.dlg = dlg;
        }

        public void run() {
            dlg.setVisible(false);
        }
    }

    void showMessage(String msg) {
        Frame frame = baseP.controller.parent();
        JOptionPane.showMessageDialog(frame , msg, "FOR INFORMATION", JOptionPane.INFORMATION_MESSAGE);
        frame.toFront();
    }

}
