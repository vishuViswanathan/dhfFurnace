package performance.stripFce;

import basic.ChMaterial;
import basic.Fuel;
import basic.ProductionData;
import directFiredHeating.DFHFurnace;
import directFiredHeating.DFHTuningParams;
import directFiredHeating.DFHeating;
import display.TimedMessage;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 1/27/14
 * Time: 10:28 AM
 * To change this template use File | Settings | File Templates.
 */
// The furnace is considered with 3 Firing zones and one Recu zone
public class PerformanceGroup implements ActionListener{
    public static final int NODATA = 0;
    public static final int SAMEWIDTH = 1;
    public static final int DIFFWIDTH = 2;

    Vector<Performance> refPerformance;
    public boolean canInterpolate = false;
    public boolean chTempProfAvailable = false;
    DFHFurnace furnace;
    DFHeating controller;
    DFHTuningParams tuningParams;
    boolean tobeSaved = false;

    public PerformanceGroup(DFHFurnace furnace, DFHTuningParams tuningParams) {
        this.furnace = furnace;
        this.controller = furnace.controller;
        this.tuningParams = tuningParams;
        refPerformance = new Vector<Performance>();
    }

    public boolean noteBasePerformance(Performance performance) {
        return noteBasePerformance(performance, false);
    }

    double[] outputFactors;
    double[] widthFactors;

    public void setTableFactors(double minOutputFactor, double outputStep, double minWidthFactor, double widthSteps) {
        int nOutput = (int)((1.0 - minOutputFactor) / outputStep) + 1;
        outputFactors = new double[nOutput];
        double val = 1.0;
        for (int n = 0; n < (nOutput - 1); n++) {
            outputFactors[n] = val;
            val -= outputStep;
        }
        outputFactors[nOutput - 1] = minOutputFactor;

        int nWidth = (int)((1.0 - minWidthFactor) / widthSteps) + 1;
        widthFactors = new double[nWidth];
        val = 1.0;
        for (int n = 0; n < (nWidth - 1); n++) {
            widthFactors[n] = val;
            val -= widthSteps;
        }
        widthFactors[nWidth - 1] = minWidthFactor;
    }

    public boolean noteBasePerformance(Performance performance, boolean fromXML) {
        boolean bNoted = false;
        boolean requiresInterpolCheck = false;
        int foundAt = getIndexOfSimilarPerformace(performance);
        if (foundAt >= 0) {
            if (controller.decide("Existing Base", "Near Performance base for " +
                    "\n" + refPerformance.get(foundAt) +
                    "\nALREADY EXISTS!" +
                            "\n\nDo you want to OVERWRITE it?")) {
                // replace data
                refPerformance.remove(foundAt);
                refPerformance.add(foundAt, performance);
                bNoted = true;
                if (foundAt < 2)
                    requiresInterpolCheck = true;
                showMessage("Replaced earlier data" + ((requiresInterpolCheck) ? " with interpolation check" : ""));
            }
        }
        else {
            refPerformance.add(performance);
            bNoted = true;
        }
        if (bNoted) {
            if (checkChTempProfAvailable() && !fromXML)
                tobeSaved = true;
            int nPerf = refPerformance.size();
            if (nPerf == 2 || requiresInterpolCheck)
                checkIfCanInterpolate();
        }
        return bNoted;
    }

    public boolean isValid() {
        return (refPerformance.size() > 0);
    }

    int checkIfDuplicate(Performance performance)   {
        int foundAt = -1;
        for (Performance per: refPerformance) {
            if (per.isProductionComparable(performance, tuningParams.exitTempTolerance)) {
                foundAt = refPerformance.indexOf(per);
                break;
            }
        }
        return foundAt;
    }

    public Performance similarPerformance(Performance performance) {
        return getRefPerformance(performance.chMaterial, performance.exitTemp());
    }

    int getIndexOfSimilarPerformace(Performance performance) {
        ChMaterial chMaterial = controller.getSelChMaterial(performance.chMaterial);
        Performance similarPerformance = getRefPerformance(chMaterial, performance.exitTemp());
        int index = -1;
        if (similarPerformance != null)
            index = refPerformance.indexOf(similarPerformance);
        return index;
    }

    public boolean replaceExistingPerformance(Performance performance) {
        boolean retVal = false;
        int foundAt = getIndexOfSimilarPerformace(performance);
        if (foundAt >= 0) {
            refPerformance.remove(foundAt);
            refPerformance.add(foundAt, performance);
            retVal = true;
        }
        return retVal;
    }


//    public double zoneTempGonPerformance(ProductionData production, int sec) {
//        double outputReqd = production.production;
//        Performance pRef = getNearerPerformance(production);
//        OneZone zoneRef = pRef.topZones.get(sec);
//        double tempGref = zoneRef.gasTemp;
//        double tempChMean = (zoneRef.stripTempIn + zoneRef.stripTempOut) / 2;
//        double outputRef = pRef.getOutput();
//        double tempGKreqd4 = (Math.pow(tempGref + 273, 4) - Math.pow(tempChMean + 273, 4)) * outputReqd / outputRef +
//                            Math.pow(tempChMean + 274, 4);
//        return Math.pow(tempGKreqd4, 0.25) - 273;
//    }

    boolean checkChTempProfAvailable() {
        chTempProfAvailable = false;
        if (refPerformance.size() > 0) {
            chTempProfAvailable = true;
        }
        return chTempProfAvailable;
     }

    void checkIfCanInterpolate() {
        canInterpolate = false;
        if (refPerformance.size() >= 2) {
            Performance p1 = refPerformance.get(0);
            Performance p2 = refPerformance.get(1);
            if ((p1.chLength == p2.chLength) &&
                    (Math.abs(p1.topZones.get(3).stripTempOut - p2.topZones.get(3).stripTempOut) < 1) &&
                    (p1.chMaterial.equals(p2.chMaterial)) &&
                    (p1.fuelName.equals(p2.fuelName)))
                canInterpolate = true;
            }
    }

    public Performance getRefPerformance(ProductionData forProduction, Fuel withFuel) {
        Performance refP = null;
        double requiredUP = forProduction.production / forProduction.charge.getLength();
        int compareOn = Performance.EXITTEMP + Performance.MATERIAL + Performance.FUEL;
        for (Performance p: refPerformance) {
            if (p.isProductionComparable(forProduction, withFuel, compareOn, tuningParams.exitTempTolerance)) {
                // check unit production limits
                if (requiredUP <= (p.unitOutput * tuningParams.overUP) &&
                            requiredUP >= (p.unitOutput * tuningParams.underUP) )   {
                    refP = p;
                    break;
                }
            }
        }
        return refP;
    }

    public Performance getRefPerformance(ChMaterial chMaterial, double exitTemp) {
        return getRefPerformance(chMaterial.name, exitTemp);
    }

    public Performance getRefPerformance(String chMaterial, double exitTemp) {
        Performance refP = null;
        for (Performance p : refPerformance) {
            if (p.isProductionComparable(chMaterial, exitTemp, tuningParams.exitTempTolerance)) {
                refP = p;
                break;
            }
        }
        return refP;
    }

//    private Performance getNearerPerformance(ProductionData forProduction) {
//        Performance p1 = refPerformance.get(0);
//        Performance p2 = refPerformance.get(1);
//        double outputReqd = forProduction.production;
//        double wrt1 = Math.abs(p1.getOutput() - outputReqd);
//        double wrt2 = Math.abs(p2.getOutput() - outputReqd);
//        if (wrt1 < wrt2)
//            return p1;
//        else
//            return p2;
//    }

    // @TODO -  suggestZoneFuels() is not used
    public int suggestZoneFuels(ProductionData forProduction, Fuel withFuel, double[] zoneFuelSuggestion) {
        int retVal = NODATA;
        Performance pSel = null;
        for (Performance p:refPerformance) {
            if (p.isProductionComparable(forProduction, withFuel, Performance.MATERIAL + Performance.FUEL + Performance.EXITTEMP,
                    1.0)) {
                pSel = p;
                break;
            }
        }
        if (pSel != null)
            if (pSel.getSuggestedFuels(forProduction.production, zoneFuelSuggestion))
                retVal = DIFFWIDTH;
        return retVal;
    }

    public int getChInTempProfile(ProductionData forProduction, Fuel withFuel, double[] chInTempProfile) {
        Performance refP = getRefPerformance(forProduction, withFuel);
        int retVal = 0;
        if (refP != null)
            retVal = refP.getChInTempProfile(chInTempProfile, forProduction.exitTemp);
        return retVal;
    }

    // @TODO - getChInTempProfile with keeping exit temp of first fired zone - NOT USED
    public int getChInTempProfile(ProductionData forProduction, Fuel withFuel, double[] chInTempProfile,
                                  int firstFiredSec) {
        Performance refP = getRefPerformance(forProduction, withFuel);
        int retVal = 0;
        if (refP != null)
            retVal = refP.getChInTempProfile(chInTempProfile, forProduction.exitTemp, firstFiredSec);
        return retVal;
    }

    public boolean isItToBeSaved() { // new data added
        return tobeSaved;
    }

    public void itIsSaved() {
        tobeSaved = false;
    }

    public StringBuilder dataInXML() {
//        String xmlStr = XMLmv.putTag("performance1", performance1.dataInXML());
//        xmlStr += XMLmv.putTag("performance2", performance2.dataInXML());
        int nRefP = refPerformance.size();
        StringBuilder xmlStr = new StringBuilder(XMLmv.putTagNew("nRefP", nRefP));
        for (int refP = 0; refP < refPerformance.size(); refP++) {
            xmlStr.append(XMLmv.putTagNew("RefP" + ("" + refP).trim(), refPerformance.get(refP).dataInXML()));
        }
        return xmlStr;
    }

    void clearData() {
        refPerformance = new Vector<Performance>();
        canInterpolate = false;
        tobeSaved = false;
    }

    public boolean takeDataFromXML(String xmlStr) {
        clearData();
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "nRefP", 0);
        if (vp.val.length() > 0) {
            try {
                int nRefP = Integer.valueOf(vp.val);
                if (nRefP > 0) {
                    Performance p;
                    if (nRefP < 100) {
                        for (int refP = 0; refP <nRefP; refP ++)  {
                            vp = XMLmv.getTag(xmlStr, "RefP" + ("" + refP).trim(), vp.endPos);
                            p = new Performance(furnace);
                            if (p.takeDataFromXML(vp.val)) {
                                noteBasePerformance(p, true);
                            }
                            else
                                break; // take no more since seems to have some error
                        }
                    }
                    else
                        showError("Too many reference data <" + nRefP + ">");
                }
            } catch (NumberFormatException e) {
                showError("some problem in reading the number of Reference Performances");
            }
        }
        return chTempProfAvailable; // was canInterpolate
    }

    JButton deleteB = new JButton("Delete the displayed Performance Data");
    JPanel buttonP = new JPanel();
    JTable listTable;
    JPanel listPanel;
    int perfDataPLoc = 0;
    JPanel innerP = null;
    public JPanel getListPanel() {
        if (listPanel == null) {
            listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            deleteB.addActionListener(this);
            buttonP.add(deleteB);
        }
        else
            listPanel.removeAll();

        listTable = new JTable(new MyTableModel());
        listTable.getSelectionModel().addListSelectionListener(new RowListener());
//        listTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listTable.setPreferredScrollableViewportSize(new Dimension(700, 50));
        TableColumnModel colModel = listTable.getColumnModel();
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment( JLabel.RIGHT );

//        colModel.getSelectionModel().addListSelectionListener(new ColumnListener());
        colModel.getColumn(0).setPreferredWidth(30);
        colModel.getColumn(1).setPreferredWidth(200);
        colModel.getColumn(2).setPreferredWidth(80);
        colModel.getColumn(3).setPreferredWidth(50);
        colModel.getColumn(3).setCellRenderer(rightRenderer);
        colModel.getColumn(4).setPreferredWidth(50);
        colModel.getColumn(4).setCellRenderer(rightRenderer);
        colModel.getColumn(5).setPreferredWidth(150);
//        colModel.getColumn(6).setPreferredWidth(50);
//        table.setFillsViewportHeight(true);

        listPanel.add(new JLabel("Performance Base List"));
        JScrollPane sP = new JScrollPane(listTable);
        sP.setBackground(SystemColor.lightGray);
//        sP.setPreferredSize(new Dimension(listTable.getPreferredSize().width, 100));
//        sP.setSize(sP.getPreferredSize());
        innerP = new JPanel();
        innerP.setLayout(new BoxLayout(innerP, BoxLayout.Y_AXIS));
        innerP.add(sP);
        perfDataPLoc = innerP.getComponentCount();
        JPanel dummyP = new JPanel();
        dummyP.setPreferredSize(new Dimension(700, 600));
        innerP.add(dummyP); // a dummy
//        innerP.add(deleteB);
        listPanel.add(innerP);
//        outerP.setPreferredSize(new Dimension(800, 500));
        return listPanel;
    }

    void showSelectedPerf() {
        showThisPerformance(refPerformance.get(listTable.getSelectedRow()));
/*
        innerP.remove(perfDataPLoc);
        JPanel detPerfP = new JPanel(new BorderLayout());
        detPerfP.add(refPerformance.get(listTable.getSelectedRow()).performanceP(), BorderLayout.CENTER);
//        detPerfP.add(refPerformance.get(listTable.getSelectedRow()).perfTableP(), BorderLayout.CENTER);
        detPerfP.add(buttonP, BorderLayout.SOUTH);
        innerP.add(detPerfP, perfDataPLoc) ;
        innerP.updateUI();
*/
    }

    void showThisPerformance(Performance p) {
        innerP.remove(perfDataPLoc);
        JPanel detPerfP = new JPanel(new BorderLayout());
        detPerfP.add(p.performanceP(), BorderLayout.CENTER);
        detPerfP.add(buttonP, BorderLayout.SOUTH);
        innerP.add(detPerfP, perfDataPLoc) ;
        innerP.updateUI();
    }

//    void showSelectedPerfTable() {
//        innerP.remove(perfDataPLoc);
//        JPanel detPerfP = new JPanel(new BorderLayout());
//
//        detPerfP.add(refPerformance.get(listTable.getSelectedRow()).perfTableP(), BorderLayout.CENTER);
//        detPerfP.add(buttonP, BorderLayout.SOUTH);
//        innerP.add(detPerfP, perfDataPLoc) ;
//        innerP.updateUI();
//    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == deleteB)  {
            int selPerf = listTable.getSelectedRow();
             if (selPerf >= 0) {
                if (decide("Deletion of Performance Data", "The Selected Performance Data will be DELETED permanently")) {
                    refPerformance.remove(selPerf);
                    getListPanel();
                    listPanel.updateUI();
                    if (!isValid())
                        controller.clearPerformBase();
                }
            }
        }
    }


    class MyTableModel extends AbstractTableModel {
        private String[] columnNames = {"SlNo",  "Charge Material", "Strip Size", "Exit Temp", "Output t/h", "Fuel", "Data Date"};
//        private String[] columnNames = {"SlNo",  "Charge Material", "Strip Size", "Exit Temp", "Output t/h", "Fuel",
//                                                                                "Delete"};

        int _rows = refPerformance.size();
        int _cols = columnNames.length;
        Object[][] data = new Object[_rows][_cols];

        MyTableModel() {
            Performance p;
            DecimalFormat outputFmt = new DecimalFormat("#,##0.00");
            DecimalFormat tempFmt = new DecimalFormat("#,##0");
            for (int r = 0; r < _rows; r++) {
                p = refPerformance.get(r);
                data[r][0] = (r + 1);
                data[r][1] = p.chMaterial;
                data[r][2] = p.stripSize();
                data[r][3] = tempFmt.format(p.exitTemp());
                data[r][4] = outputFmt.format(p.output / 1000);
                data[r][5] = p.fuelName;
                data[r][6] = p.dateStr();
            }

        }
        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }


    }

    private class RowListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
//            debug("ROW SELECTION EVENT. " + listTable.getValueAt(listTable.getSelectedRow(), 0));
            showSelectedPerf();
         }
    }

    boolean decide(String title, String msg) {
        int resp = JOptionPane.showConfirmDialog(controller.parent(), msg, title, JOptionPane.YES_NO_OPTION);
        if (resp == JOptionPane.YES_OPTION)
            return true;
        else
            return false;
    }

    void debug(String msg) {
         System.out.println("DFHFurnace: " + msg);
     }

    void showError(String msg){
        (new TimedMessage("In Performance Data", msg, TimedMessage.ERROR, controller.parent(), 5000)).show();
     }

     void showMessage(String msg) {
         (new TimedMessage("In Performance Data", msg, TimedMessage.INFO, controller.parent(), 3000)).show();
     }
}
