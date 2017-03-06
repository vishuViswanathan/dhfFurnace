package performance.stripFce;

import basic.ChMaterial;
import basic.Fuel;
import basic.ProductionData;
import directFiredHeating.DFHFurnace;
import directFiredHeating.DFHTuningParams;
import directFiredHeating.DFHeating;
import directFiredHeating.process.OneStripDFHProcess;
import mvUtils.display.DataStat;
import mvUtils.display.DataWithStatus;
import mvUtils.display.StatusWithMessage;
import mvUtils.display.TimedMessage;
import mvUtils.math.BooleanWithStatus;
import mvUtils.math.MoreOrLess;
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

    void deletePerformance(int atLoc) {
        Performance existingP = refPerformance.get(atLoc);
        deletePerformance(existingP);
    }

    public void deletePerformance(Performance p) {
        p.deleteProcessLink();
        refPerformance.remove(p);
        getListPanel();
    }

    public void addPerformance(Performance p) {
        refPerformance.add(p);
    }

    public void addPerformance(Performance p, int atLoc) {
        if (atLoc >= 0)
            refPerformance.set(atLoc, p);
        else
            addPerformance(p);
    }

    public boolean noteBasePerformance(Performance performance, boolean fromXML) {
        boolean bNoted = false;
        boolean requiresInterpolCheck = false;
        int foundAt = getIndexOfSimilarPerformance(performance);
        if (foundAt >= 0) {
            MoreOrLess.CompareResult compareResult =
                    MoreOrLess.compare(refPerformance.get(foundAt).dateOfResult, performance.dateOfResult);
            boolean overWrite = true;
            if (compareResult == MoreOrLess.CompareResult.DATA1MORE) {
                overWrite = controller.decide("Existing Base", "Near Performance base for " +
                        "\n" + refPerformance.get(foundAt) +
                        "\nof Later Date ALREADY EXISTS!" +
                        "\n\nDo you want to OVERWRITE it?");
            }
            if (overWrite)  {
                deletePerformance(foundAt);
                StatusWithMessage addResponse = furnace.addPerformance(performance, -1);
                DataStat.Status status = addResponse.getDataStatus();
                if (status == DataStat.Status.OK) {
                    bNoted = true;
                    if (foundAt < 2)
                        requiresInterpolCheck = true;
                    if (!fromXML)
                        showMessage("Replaced earlier data" + ((requiresInterpolCheck) ? " with interpolation check" : ""));
                }
                else {
                    String msg =  (status == DataStat.Status.WithInfoMsg) ? addResponse.getInfoMessage() : addResponse.getErrorMessage();
                    showError("Performance NOT noted: " + msg);
                }
            }
        }
        else {
            StatusWithMessage stat = furnace.addPerformance(performance);
            if (stat.getDataStatus() == DataStat.Status.WithErrorMsg)  {
                controller.showError("Noting Performance data: " + stat.getErrorMessage());
            }
            else
                bNoted = true;
        }
        if (bNoted) {
            if (checkChTempProfAvailable() && !fromXML)
                tobeSaved = true;
            int nPerf = refPerformance.size();
            if (nPerf == 2 || requiresInterpolCheck)
                checkIfCanInterpolate();
        }
        getListPanel();
        return bNoted;
    }

    public void markToBeSaved(boolean tobeSaved) {
        this.tobeSaved = tobeSaved;
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
        return getRefPerformance(performance.processName, performance.chMaterial, performance.fuelName, performance.exitTemp());
    }

    int getIndexOfSimilarPerformance(Performance performance) {
        int index = -2;
        ChMaterial chMaterial = controller.getSelChMaterial(performance.chMaterial);
        if (chMaterial != null) {
            index = -1;
            Performance similarPerformance = getRefPerformance(performance.processName, chMaterial, performance.fuelName, performance.exitTemp());
            if (similarPerformance != null)
                index = refPerformance.indexOf(similarPerformance);
        }
        return index;
    }

    public boolean replaceExistingPerformance(Performance performance) {
        boolean retVal = false;
        int foundAt = getIndexOfSimilarPerformance(performance);
        if (foundAt >= 0) {
            deletePerformance(foundAt);
//            refPerformance.remove(foundAt);
            StatusWithMessage stat = furnace.addPerformance(performance, -1);
            if (stat.getDataStatus() == DataStat.Status.OK)   {
//            addPerformance(performance, foundAt);
//            refPerformance.add(foundAt, performance);
                tobeSaved = true;
                retVal = true;
            }
            else {
                showError("Unable to replace the existing Performance data");
                controller.logError("PerformanceGroup.replaceExistingPerformance: Unable to replace the existing Performance data: " + stat.getErrorMessage());
            }
        }
        else {
            showError("Could not locate existing Performance data");
            controller.logError("PerformanceGroup.replaceExistingPerformance: Could not locate existing Performance data");
        }
        return retVal;
    }

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
//        double requiredUP = forProduction.production / forProduction.charge.getLength();
        int compareOn = Performance.EXITTEMP + Performance.MATERIAL + Performance.FUEL;
        for (Performance p: refPerformance) {
            if (p.isProductionComparable(forProduction, withFuel, compareOn, tuningParams.exitTempTolerance)) {
                // check unit production limits
//                if (requiredUP <= (p.unitOutput * tuningParams.overUP) &&
//                            requiredUP >= (p.unitOutput * tuningParams.underUP) )   {
                    refP = p;
//                    break;
//                }
            }
        }
        return refP;
    }

    public Performance getRefPerformance(String processName, ChMaterial chMaterial, String withFuel, double exitTemp) {
        Performance p = null;
        if (chMaterial != null)
            p = getRefPerformance(processName, chMaterial.name, withFuel, exitTemp);
        return p;
    }

    public boolean isRefPerformanceAvailable(String processName, String chMaterialName, double exitTemp) {
        boolean retVal = false;
        for (Performance p : refPerformance) {
            if (p.isProductionComparable(processName, chMaterialName, exitTemp, tuningParams.exitTempTolerance)) {
                retVal = true;
                break;
            }
        }
        return retVal;
    }

    public Performance getRefPerformance(String processName, String chMaterialName, String withFuel, double exitTemp) {
        Performance refP = null;
        for (Performance p : refPerformance) {
            if (p.isProductionComparable(processName, chMaterialName, withFuel, exitTemp, tuningParams.exitTempTolerance)) {
                refP = p;
                break;
            }
        }
        return refP;
    }


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

//    double[] chInTempProfile;

//    public int getChInTempProfile(ProductionData forProduction, Fuel withFuel, double[] chInTempProfile) {
//        Performance refP = getRefPerformance(forProduction, withFuel);
//        int retVal = 0;
//        if (refP != null) {
//            retVal = refP.getChInTempProfile(chInTempProfile, forProduction.exitTemp);
//            if (retVal > 0)
//                forProduction.setChEmmissCorrectionFactor(refP.chEmmCorrectionFactor);
//        }
//        return retVal;
//    }

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

    public boolean takeDataFromXML(String xmlStr, boolean readPerfTable, boolean append) {
        if (!append)
            clearData();
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "nRefP", 0);
        if (vp.val.length() > 0) {
            try {
                int nRefP = Integer.valueOf(vp.val);
                if (nRefP > 0) {
                    Performance p;
                    if (nRefP < 100) {
                        for (int refP = 0; refP <nRefP; refP++)  {
                            vp = XMLmv.getTag(xmlStr, "RefP" + ("" + refP).trim(), vp.endPos);
                            p = new Performance(furnace);
                            if (p.takeDataFromXML(vp.val, readPerfTable)) {
                                if (!noteBasePerformance(p, true))
                                    showError("Unable to note performance data :" + p);
                            }
                            else
                                showError("Some mismatch in performance data :" + p +
                                    "\n    The Data is discarded !");
//                                break; // take no more since seems to have some error
                        }
                    }
                    else
                        showError("Too many reference Performance Data <" + nRefP + ">");
                }
            } catch (NumberFormatException e) {
                showError("some problem in reading the number of Reference Performances");
            }
        }
        return chTempProfAvailable; // was canInterpolate
    }

    public void enableDeleteAction(boolean ena) {
//        if (deleteB != null)
            deleteB.setEnabled(ena);
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
        listTable.setPreferredScrollableViewportSize(new Dimension(700, 50));
        TableColumnModel colModel = listTable.getColumnModel();
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment( JLabel.RIGHT );

        colModel.getColumn(0).setPreferredWidth(10);
        colModel.getColumn(1).setPreferredWidth(200);
        colModel.getColumn(2).setPreferredWidth(120);
        colModel.getColumn(3).setPreferredWidth(60);
        colModel.getColumn(4).setPreferredWidth(20);
        colModel.getColumn(4).setCellRenderer(rightRenderer);
        colModel.getColumn(5).setPreferredWidth(30);
        colModel.getColumn(5).setCellRenderer(rightRenderer);
        colModel.getColumn(6).setPreferredWidth(150);

        listPanel.add(new JLabel("Performance Base List"));
        JScrollPane sP = new JScrollPane(listTable);
        sP.setBackground(SystemColor.lightGray);
        innerP = new JPanel();
        innerP.setLayout(new BoxLayout(innerP, BoxLayout.Y_AXIS));
        innerP.add(sP);
        perfDataPLoc = innerP.getComponentCount();
        JPanel dummyP = new JPanel();
        dummyP.setPreferredSize(new Dimension(700, 600));
        innerP.add(dummyP); // a dummy
        listPanel.add(innerP);
        listPanel.updateUI();
        return listPanel;
    }

    void showSelectedPerf() {
        showThisPerformance(refPerformance.get(listTable.getSelectedRow()));
    }

    void showThisPerformance(Performance p) {
        innerP.remove(perfDataPLoc);
        JPanel detPerfP = new JPanel(new BorderLayout());
        detPerfP.add(p.performanceP(), BorderLayout.CENTER);
        detPerfP.add(buttonP, BorderLayout.SOUTH);
        innerP.add(detPerfP, perfDataPLoc) ;
        innerP.updateUI();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == deleteB)  {
            int selPerf = listTable.getSelectedRow();
             if (selPerf >= 0) {
                if (decide("Deletion of Performance Data", "The Selected Performance Data will be DELETED permanently")) {
                    deletePerformance(selPerf);
                    if (!isValid())
                        controller.clearPerformBase();
                }
            }
        }
    }


    class MyTableModel extends AbstractTableModel {
        private String[] columnNames = {"SlNo",  "Process", "Charge Material", "Strip Size", "Exit Temp", "Output t/h", "Fuel", "Data Date"};
        int _rows = refPerformance.size();
        int _cols = columnNames.length;
        Object[][] data = new Object[_rows][_cols];

        MyTableModel() {
            Performance p;
            DecimalFormat outputFmt = new DecimalFormat("#,##0.00");
            DecimalFormat tempFmt = new DecimalFormat("#,##0");
            for (int r = 0; r < _rows; r++) {
                p = refPerformance.get(r);
                int c = 0;
                data[r][c++] = (r + 1);
                data[r][c++] = p.processName;
                data[r][c++] = p.chMaterial;
                data[r][c++] = p.stripSize();
                data[r][c++] = tempFmt.format(p.exitTemp());
                data[r][c++] = outputFmt.format(p.output / 1000);
                data[r][c++] = p.fuelName;
                data[r][c] = p.dateStr();
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
        controller.logError("PerformanceGroup: " + msg);
    }

    void showMessage(String msg) {
        (new TimedMessage("In Performance Data", msg, TimedMessage.INFO, controller.parent(), 3000)).show();
        controller.logInfo("PerformanceGroup: " + msg);
    }
}
