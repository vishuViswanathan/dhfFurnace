package radiantTubeHeating;

import basic.RadiantTube;
import display.OnePropertyTrace;
import mvUtils.display.*;
import mvUtils.math.DoublePoint;
import mvUtils.math.DoubleRange;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 09 Nov 2020
 * Time: 11:02 AM
 * To change this template use File | Settings | File Templates.
 */

public class RTFSection extends GraphInfoAdapter{
    InputControl ipc;
    int zoneNum;
    public boolean edited = false;
    RTFurnace furnace;
    public double startPos;
    public double endPos;
    RTFSection prevSection = null;
    RTFSection nextSection = null;
    RTHeating.LimitMode calculationLimitMode = RTHeating.LimitMode.RTTEMP;

    public RadiantTube rt;
    public double rtCenterAbove = 0.3, rtCenterBelow = 0.3;
    double rtPerM = 3;
    double topRTperM, botRTperM;
    double uAreaHeTot;
    double uAreaHeCh;
    double unitLen = 1;
    Vector<OneRTslot> allSlots;
    int maxSlots = 100;
    public int nSlots = 0;
    RTFSection.MyTableModel tableModel;
    OneRTslot startSlot;

    JPanel resultsPage;
    JPanel trendsPage;
    public MultiPairColPanel allResultsP;
    GraphPanel trendPanel;
    protected JScrollPane slate = new JScrollPane();
    FramedPanel gP;
    ScrollPane resultScroll;
    JTable resultTable;
    public boolean calculationDone = false;

    public RTFSection(InputControl ipc, RTFurnace rtF, RTFSection prevSection) {
        this(ipc, prevSection);
        furnace = rtF;
    }

    public RTFSection(InputControl ipc, RTFSection prevSection) {
        this.ipc = ipc;
        prepareUIs();
        if (prevSection == null) {
            calculationLimitMode = RTHeating.LimitMode.RTTEMP;
            startPos = 0;
            zoneNum = 1;
            rt = new RadiantTube(ipc);
            rtPerM = 3;
        }
        else {
            furnace = prevSection.furnace;
//            startPos = prevSection.endPos;
            zoneNum = prevSection.zoneNum + 1;
            this.prevSection = prevSection;
            rt = prevSection.rt.getACopy();
            rtPerM = prevSection.rtPerM;
            rtCenterAbove = prevSection.rtCenterAbove;
            prevSection.nextSection = this;
            takeStartConditionsFromPrevZone();
        }
    }

    public boolean copyFrom(RTFSection from) {
        prevSection = from.prevSection;
        zoneNum = from.zoneNum;
        rt = from.rt.getACopy();
        rtPerM = from.rtPerM;
        rtCenterAbove = from.rtCenterAbove;
        if (prevSection != null) {
            prevSection.nextSection = this;
            takeStartConditionsFromPrevZone();
        }
        updateUI();
        updateData();
        return true;
    }

    void updateData() {
        this.topRTperM = rtPerM / 2;
        this.botRTperM = this.topRTperM;
    }

    void takeStartConditionsFromPrevZone() {
        chStTemp = prevSection.actualEndTempMean;
        chStTempSurf = prevSection.actualEndTempSurf;
        chStTempCore = prevSection.actualEndTempCore;
//        calculationLimitMode = prevSection.calculationLimitMode;
//        rtLimitTemp = prevSection.rtLimitTemp;
//        rtLimitHeat = prevSection.rtLimitHeat;
        updateUIwithprevZoneData();
        startPos = prevSection.endPos;
        enableDataEdit(true);
    }

    void prepareUIs() {
        ntChEntryTemp = new NumberTextField(ipc, chStTemp - 273, 6, false,
                -200, 2000, "#,###", "Charge Entry Temperatures (C)");
        ntChExitTemp = new NumberTextField(ipc, chEndTemp - 273, 6, false,
                -200, 2000, "#,###", "Charge Exit Temperatures (C)");
        cbLimitMode = new XLComboBox(RTHeating.LimitMode.values());
        cbLimitMode.setSelectedItem(calculationLimitMode);
        ntMaxRtTemp = new NumberTextField(ipc, rtLimitTemp -273, 6, false,
                200, 2000, "#,###", "Radiant Tube Temperature Limit (C)");
        ntMaxRtHeat = new NumberTextField(ipc, rtLimitHeat, 6, false,
                0, 2000, "#,###.00", "Radiant Tube Heat Limit (kW)");
        ntTubesPerFceLength = new NumberTextField(ipc, rtPerM, 6, false,
                0, 100, "#.000", "Number of Tube per m of Furnace Length");
        ntRadiantTubeChargeDistance = new NumberTextField(ipc, rtCenterAbove * 1000, 6, false,
                0, 10000, "#,###", "RT Centre line above Charge Surface (mm)");
        calculTemperatureFields = new Vector<>();
        calculTemperatureFields.add(ntChEntryTemp);
        calculTemperatureFields.add(ntChExitTemp);
        calculFields = new Vector<>();
        calculFields.add(ntMaxRtTemp);
        calculFields.add(ntMaxRtHeat);

    }

    void updateUI() {
        ntChEntryTemp.setData(chStTemp - 273);
        ntChExitTemp.setData(chEndTemp - 273);
        cbLimitMode.setSelectedItem(calculationLimitMode);
        ntMaxRtTemp.setData(rtLimitTemp - 273);
        ntMaxRtHeat.setData(rtLimitHeat);
        ntRadiantTubeChargeDistance.setData(rtCenterAbove * 1000);
        ntTubesPerFceLength.setData(rtPerM);
    }

    void updateUIwithprevZoneData() {
        ntChEntryTemp.setData(chStTemp - 273);

    }

    Vector<NumberTextField> fceDetailsFields;

    int xlSectionData(Sheet sheet, ExcelStyles styles, int topRow) {
        topRow += 1;
        int row, rRow;
        int col = 1;
        row = styles.xlMultiPairColPanel(rt.getDataPanel(), sheet, topRow, col);
        rRow = styles.xlMultiPairColPanel(calculModeP, sheet, topRow, col + 3);
        topRow = row + 1;
        row = styles.xlMultiPairColPanel(radiantTubeInFceP, sheet, topRow, col);
//        rRow = styles.xlMultiPairColPanel(calculModeP, sheet, topRow, col + 3);
        rRow = styles.xlMultiPairColPanel(calculatePanel, sheet, topRow, col + 3);
        return Math.max(row, rRow) + 1;
    }

    public int xlTempProfile(Sheet sheet, ExcelStyles styles, int headRow) {
        Cell cell;
        Row r;
        r = sheet.createRow(headRow);
        cell = r.createCell(0);
        cell.setCellStyle(styles.csHeader1);
        cell.setCellValue("TEMPERATURE PROFILE OF Zone#" + zoneNum);
        int topRow = headRow + 1;
        int leftCol = 1;
        JTable table = getResultTable();
        int row = styles.xlAddXLCellData(sheet, topRow, leftCol, table);
        return row + 2;
    }

    public void enableDataEdit(boolean ena) {
        cbLimitMode.setEnabled(ena);
        ntTubesPerFceLength.setEditable(ena);
        ntRadiantTubeChargeDistance.setEditable(ena);
        rt.enableDataEdit(ena);
        for (Component c:calculFields)
            c.setEnabled(ena);
        for (Component c:calculTemperatureFields)
            c.setEnabled(ena);
        jBcalculate.setEnabled(ena);
        jBmodifyData.setEnabled(!ena);
        if (ena)
            undoResults();
    }

    void undoResults() {
        calculationDone = false;
        furnace.undoResults(zoneNum);
    }

    NumberTextField ntTubesPerFceLength;
    NumberTextField ntRadiantTubeChargeDistance;
//    boolean radiantTubeFieldsSet = false;
    Vector<NumberTextField> calculFields;
    Vector<NumberTextField> calculTemperatureFields;
    JComboBox<RTHeating.LimitMode> cbLimitMode;
    boolean calculFieldsSet = false;
    MultiPairColPanel radiantTubeInFceP;
    Vector<NumberTextField> tubesInFceFields;
    Vector<NumberTextField> calculateFields;
    Vector<NumberTextField> productionFields;
    // temperaures in degK
    double rtLimitTemp = 1173, rtLimitHeat = 24, fceLimitLength;
    double chStTemp = 303, chEndTemp = 873; // all in degK
    double chStTempSurf = chStTemp;
    double chStTempCore = chStTemp;
    double actualEndTempMean;
    double actualEndTempCore;
    double actualEndTempSurf;

    boolean bHeatLimit, bRtTempLimit, bFceLengthLimit;
    MultiPairColPanel calculModeP;
    NumberTextField ntMaxRtTemp;
    NumberTextField ntMaxRtHeat;
    JButton jBcalculate = new JButton("Calculate Zone");
    JButton jBmodifyData = new JButton("Modify Data");
    JButton jBredo;
    NumberTextField ntChEntryTemp;
    NumberTextField ntChExitTemp;
    MultiPairColPanel calculatePanel;

    public JPanel zoneDataP() {
        JPanel detFrame = new JPanel(new GridBagLayout());
        GridBagConstraints gbcMf = new GridBagConstraints();

        gbcMf.anchor = GridBagConstraints.CENTER;
        gbcMf.gridx = 0;
        gbcMf.gridy = 0;
        gbcMf.insets = new Insets(0, 0, 0, 0);
        gbcMf.gridwidth = 1;

//        if (!radiantTubeFieldsSet) {
//            MultiPairColPanel pan = new MultiPairColPanel("Furnace Zone Section Data (Zone " + zoneNum);
        detFrame.add(radiantTubesP(), gbcMf);
        gbcMf.gridx = 1;
        detFrame.add(calculDataP(), gbcMf);
//            radiantTubeFieldsSet = true;
//        }
        return detFrame;
    }

    JPanel calculDataP() {
        if (!calculFieldsSet) {
            productionFields = new Vector<>();
            MultiPairColPanel pan = new MultiPairColPanel("Calculation Mode");
//            calculFields = new Vector<>();
            pan.addItemPair("Calculation Limiting Mode", cbLimitMode);
//            calculFields.add(ntMaxRtTemp);
//            calculFields.add(ntMaxRtHeat);
            pan.addItemPair(ntMaxRtTemp);
            pan.addItemPair(ntMaxRtHeat);
            pan.addItem(calculateP());
            calculModeP = pan;
            calculFieldsSet = true;
        }
        return calculModeP;
    }

    public JPanel radiantTubesP() {
//        if (!radiantTubeFieldsSet) {
            MultiPairColPanel pan = new MultiPairColPanel("Radiant Tubes in Furnace");
            pan.addItem(rt.radiantTubesP(ipc));
            tubesInFceFields = new Vector<>();
            tubesInFceFields.add(ntTubesPerFceLength);
            tubesInFceFields.add(ntRadiantTubeChargeDistance);
            pan.addItem("<html><B>Tubes to have a minimum gap of One Dia between the legs</B></html>");
            pan.addItemPair(ntTubesPerFceLength);
            pan.addItemPair(ntRadiantTubeChargeDistance);
            radiantTubeInFceP = pan;
//            radiantTubeFieldsSet = true;
//        }
        return radiantTubeInFceP;
    }

    public JPanel calculateP() {
        MultiPairColPanel pan = new MultiPairColPanel("Calculate");
//        calculTemperatureFields = new Vector<>();
//        calculTemperatureFields.add(ntChEntryTemp);
//        calculTemperatureFields.add(ntChExitTemp);
        pan.addItemPair(ntChEntryTemp);
        pan.addItemPair(ntChExitTemp);
        JPanel buttonPanel = new JPanel();
        jBcalculate.addActionListener(e -> {
            furnace.takeFceDetailsFromUI();
            if (takeRTinSectionFromUI() && takeCalculModeFromUI())
                if (prevSection == null || prevSection.calculationDone)
                    calculate();
                else
                    furnace.showError("Zone #" + zoneNum + " Calculation", "Zone #" + prevSection.zoneNum + " is still to be calculated");
            else
                furnace.showError("Zone Calculation", "Some data are not proper");
        });
        jBmodifyData.addActionListener(e -> {
            furnace.resetZoneCalculation(zoneNum);
        });
        jBmodifyData.setEnabled(false);
        buttonPanel.add(jBmodifyData);
        buttonPanel.add(jBcalculate);
        pan.addItem(buttonPanel);
        calculatePanel = pan;
        return calculatePanel;
    }

    boolean takeRTinSectionFromUI() {
        boolean retVal = true;
        for (NumberTextField f : tubesInFceFields)
            retVal &= !f.isInError();
        retVal = rt.takeFromUI();
        if (retVal) {
            rtPerM = ntTubesPerFceLength.getData();
            if (rtPerM < (unitLen / rt.rTdia)) {
                rtCenterAbove = ntRadiantTubeChargeDistance.getData() / 1000;
                if (rtCenterAbove < (furnace.heightAbove - rt.rTdia))
                    updateData();
                else {
                    furnace.showError("Zone #" + zoneNum, "Radiant tube location cannot be accommodated");
                    retVal = false;
                }
            }
            else  {
                furnace.showError("Zone #" + zoneNum, "Check RT numbers/m");
                retVal = false;

            }
        }
        return retVal;
    };

    boolean takeCalculModeFromUI() {
        boolean retVal = true;
        for (NumberTextField f:calculTemperatureFields)
            retVal &= !f.isInError();
        for (NumberTextField f: calculFields)
            retVal &= !f.isInError();
        if (retVal) {
            chStTemp = ntChEntryTemp.getData() + 273;
            chEndTemp = ntChExitTemp.getData() + 273;
            rtLimitTemp = ntMaxRtTemp.getData() + 273;
            if (chEndTemp > chStTemp) {
                rtLimitHeat = ntMaxRtHeat.getData();
                calculationLimitMode = (RTHeating.LimitMode) (cbLimitMode.getSelectedItem());
                switch (calculationLimitMode) {
                    case RTHEAT:
                        bHeatLimit = true;
                        bRtTempLimit = false;
                        break;
                    case RTTEMP:
                        bHeatLimit = false;
                        bRtTempLimit = true;
                        if (rtLimitTemp <= chEndTemp) {
                            furnace.showError("Zone #" + zoneNum, "Check Radiant Tube Temperature Limit");
                            retVal = false;
                        }
                        break;
                }
            }
            else {
                furnace.showError("Zone #" + zoneNum, "Check Charge Exit Temperature");
                retVal = false;
            }
        }
        return retVal;
    }

    boolean prepareForCalculation() {
        boolean proceed = false;
        if (furnace.prepareForCalculation() &&
                takeRTinSectionFromUI() &&
                takeCalculModeFromUI()) {
            proceed = prepareSlots();
        }
        return proceed;
    }

    boolean prepareSlots() {
        allSlots = new Vector();
        double lPos = startPos + unitLen;
        OneRTslot slot;
        double endTime = 0;
        double uWt =  furnace.charge.unitWt * furnace.nChargeAlongFceWidth;
        double unitTime = unitLen / (furnace.production / 60 / uWt);
        OneRTslot prevSlot = null;
        uAreaHeTot = rt.getTotHeatingSurface() * (topRTperM + botRTperM) * unitLen;
        uAreaHeCh = uAreaHeTot / 2;
//        furnace.debug("maxSlots limited to " + maxSlots);
        for (int i = 0; i < maxSlots; i++) {
            endTime += unitTime;
            slot = new OneRTslot(furnace, this, lPos, prevSlot);
            slot.setCharge(furnace.charge, uWt, unitTime, endTime);
            allSlots.add(slot);
            lPos += unitLen;
            prevSlot = slot;
        }
        return true;
    }

    // Temperatures in K
    public boolean calculate() {
        if (prepareForCalculation()) {
            furnace.enableDataEdit(zoneNum, false);
            OneRTslot slot = allSlots.get(0);
            double srcHeat = rtLimitHeat * 860 * (topRTperM + botRTperM) * unitLen;
            double exitTemp = slot.calculate(chStTemp, rtLimitTemp, 0, srcHeat, calculationLimitMode);
            nSlots = 1;
            while ((exitTemp < chEndTemp) && (nSlots < allSlots.size())) {
                slot = allSlots.get(nSlots);
                exitTemp = slot.calculate();
                nSlots++;
            }
            noteEndConditions(slot);
            getAllResultsPanel();
            RTHeating.ResultsType rType = RTHeating.ResultsType.getZoneEnum(zoneNum);
            furnace.addResult(rType, allResultsP);
            furnace.resultsReady(rType);
            if (nextSection != null) {
                nextSection.takeStartConditionsFromPrevZone();
            }
            calculationDone = true;
        }
        return true;
    }

    void noteEndConditions(OneRTslot slot) {
        actualEndTempMean = slot.tempChEnd;
        actualEndTempCore = slot.tempChCore;
        actualEndTempSurf = slot.tempChSurf;
        endPos = slot.lPos;
    }

    JPanel getAllResultsPanel() {
        startSlot = new OneRTslot(allSlots.get(0), startPos, chStTemp, chStTempSurf, chStTempCore);
        setDataForTraces();
        trendsPage = trendsPage();
        resultsPage = resultsPage();
        allResultsP = new MultiPairColPanel("Results for Zone #" + zoneNum);
        JTabbedPane jtp= new JTabbedPane();
        jtp.addTab("Trends", trendPanel);
        jtp.addTab("Table", resultsPage);
        allResultsP.addItem(jtp);
        JPanel buttonP = new JPanel(new GridLayout());
        jBredo = new JButton("Redo the Calculation");
        jBredo.addActionListener(e -> {
            furnace.resetZoneCalculation(zoneNum);
        });
        // space them
        for (int l = 0; l < 4; l++)
            buttonP.add(new JLabel());
        buttonP.add(jBredo);
        allResultsP.addItem(buttonP);
        return allResultsP;
    }

    JPanel trendsPage() {
        JPanel resultsFrame = new JPanel(new GridBagLayout());
        GridBagConstraints gbcMf = new GridBagConstraints();
        gbcMf.anchor = GridBagConstraints.CENTER;
        gbcMf.gridx = 0;
        gbcMf.gridy = 0;
        gbcMf.insets = new Insets(0, 0, 0, 0);
        gbcMf.gridwidth = 1;
        gbcMf.gridheight = 1;
        resultsFrame.add(getTitlePanel(), gbcMf);
        gbcMf.gridx = 0;
        gbcMf.gridy++;
        resultsFrame.add(getGraphPanel(), gbcMf);
        trendPanel.setTraceToShow(-1);
        return resultsFrame;
    }

    JPanel resultsPage() {
        JPanel resultsFrame = new JPanel(new GridBagLayout());
        GridBagConstraints gbcMf = new GridBagConstraints();
        gbcMf.anchor = GridBagConstraints.CENTER;
        gbcMf.gridx = 0;
        gbcMf.gridy = 0;
        gbcMf.insets = new Insets(0, 0, 0, 0);
        gbcMf.gridwidth = 1;
        gbcMf.gridheight = 1;
        resultsFrame.add(getTitlePanel(), gbcMf);
        gbcMf.gridx = 0;
        gbcMf.gridy++;
        gbcMf.gridwidth = 1;
        gbcMf.gridheight = 2;
        resultsFrame.add(getListPanel(), gbcMf);
        return resultsFrame;
    }

    FramedPanel getTitlePanel() {
        FramedPanel titlePanel = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbcTp = new GridBagConstraints();
        final int hse = 4; // ends
        final int vse = 4; // ends
        final int hs = 0;
        final int vs = 0;
        Insets lt = new Insets(vs, hse, vs, hs);
        Insets ltbt = new Insets(vs, hse, vse, hs);
        Insets bt = new Insets(vs, hs, vse, hs);
        Insets rtbt = new Insets(vs, hs, vse, hse);
        Insets rt = new Insets(vs, hs, vs, hse);
        Insets rttp = new Insets(vse, hs, vs, hse);
        Insets lttp = new Insets(vse, hse, vs, hs);
        Insets tp = new Insets(vse, hs, vs, hs);
        Insets ltrt = new Insets(vs, hse, vs, hse);
        Insets lttprt = new Insets(vse, hse, vs, hse);
        Insets ltbtrt = new Insets(vs, hse, vse, hse);
        Insets mid = new Insets(vs, hs, vs, hs);
        gbcTp.anchor = GridBagConstraints.CENTER;
        gbcTp.gridx = 0;
        gbcTp.gridy = 0;
        gbcTp.insets = lttp;
        titlePanel.add(new Label("Radiant Tube Furnace"), gbcTp);
        gbcTp.gridy++;
        gbcTp.insets = lt;
        JLabel jLtitle = new JLabel();
        titlePanel.add(jLtitle, gbcTp);
        gbcTp.gridy++;
        return titlePanel;
    }

    Panel getListPanel() {
        resultTable = getResultTable();
        JPanel headPanel = new JPanel(new GridLayout(1, 1));
        headPanel.add(resultTable.getTableHeader());
        Panel listPanel = new Panel(new BorderLayout());
        listPanel.add(headPanel, BorderLayout.NORTH);
        resultScroll = new ScrollPane();
        resultScroll.setSize(new Dimension(700, 250));
        resultScroll.add(resultTable);
        resultScroll.repaint();
        listPanel.add(resultScroll, BorderLayout.CENTER);
        return listPanel;
    }

    FramedPanel getGraphPanel() {
        gP = new FramedPanel(new GridLayout(1, 0));
        trendPanel =
                new GraphPanel(new Dimension(700, 350));
        for (int t = 0; t < nTraces; t++)
            trendPanel.addTrace(this, t, GraphDisplay.COLORS[t]);
        trendPanel.setTraceToShow(0);   // all
        trendPanel.prepareDisplay();
        gP.add(trendPanel);
        return gP;
    }

    public JTable getResultTable() {
        tableModel = new RTFSection.MyTableModel();
        JTable table = new JTable(tableModel);
        table.setDefaultRenderer(table.getColumnClass(1), new RTFSection.CellRenderer());
        return table;
    }

    class GraphPanel
            extends JPanel {
        final Insets borders = new Insets(2, 2, 2, 2);
        GraphDisplay gDisplay;
        Dimension size;
        Point origin; // in % of graph area

        GraphPanel(Dimension size) {
            this.size = size;
            setSize(size);
            origin = new Point(0, 00);
            gDisplay = new GraphDisplay(this, origin, null); //frameEventDespatcher);
            //       gDisplay.setBasicCalculData(traces);
        }

        int addTrace(GraphInfo gInfo, int trace, Color color) {
            gDisplay.addTrace(gInfo, trace, color);
            return gDisplay.traceCount();
        }

        void prepareDisplay() {
            gDisplay.prepareDisplay();
        }

        public Insets getInsets() {
            return borders;
        }

        public Dimension getMinimumSize() {
            return size;
        }

        public Dimension getPreferredSize() {
            return size;
        }

        public void setTraceToShow(int t) {
            gDisplay.setTraceToShow(t);
        }
    } // class GraphPanel

    class MyTableModel extends AbstractTableModel { //here
        public MyTableModel() {
        }

        public int getColumnCount() {
            return OneRTslot.nColumn;
        }

        public int getRowCount() {
            return nSlots + 1;
        }

        public String getColumnName(int col) {
            if (col < OneRTslot.nColumn)
                return OneRTslot.colName[col];
            else
                return "UNKNOWN";
        }

        public Object getValueAt(int row, int col) { // was(int xPos, int yPos) {
            String data;
            OneRTslot slot;

            if (row >= 0 && row < allSlots.size() + 1) {
                if (row == 0)
                    slot = startSlot;
                else
                    slot = allSlots.get(row - 1);
                data = slot.getColData(col);
            } else
                data = "NO DATA";
            return data;
        }

        public double getdoubleValueAt(int row, int col) { // was(int xPos, int yPos) {
            double data;
            OneRTslot slot;

            if (row >= 0 && row < allSlots.size() + 1) {
                if (row == 0)
                    slot = startSlot;
                else
                    slot = allSlots.get(row - 1);
                data = slot.getColDataVal(col);
            } else
                data = Double.NaN;
            return data;
        }

        String getResultsCVS() {
            String cvs = "";
            int c;
            int columns = OneRTslot.nColumn;
            for (c = 0; c < OneRTslot.nColumn; c++) {
                cvs += getColumnName(c) + ((c == (columns - 1) ? "\n" : ","));
            }
            int rows = getRowCount();
            for (int r = 0; r < rows; r++) {
                for (c = 0; c < columns; c++) {
                    cvs += ((String) getValueAt(r, c)).replace(",", "") + ((c == (columns - 1) ? "\n" : ","));
                }
            }
            return cvs;
        }

    }  // class MyTableModel


    public class CellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            JLabel comp = new JLabel(value.toString());
            return comp;
        }
    }    // class CellRenderer

    public int nTraces = 3;
    String[] traceName = {"tempHe", "tempFce", "tempCh"};
    TraceHeader[] traceHeader = {new TraceHeader(traceName[0], "m", "DegC"), new TraceHeader(traceName[1], "m", "DegC"),
            new TraceHeader(traceName[2], "m", "DegC")};
    DoublePoint[] tempHe;
    DoublePoint[] tempFce;
    DoublePoint[] tempChSurf;
    DoublePoint[] tempCh;
    DoublePoint[] tempChCore;
    Vector<OnePropertyTrace> traces;
    DoubleRange commonX, commonY;

    void setDataForTraces() {
        OneRTslot slot;
        tempHe = new DoublePoint[nSlots + 1];
        tempFce = new DoublePoint[nSlots + 1];
        tempChSurf = new DoublePoint[nSlots + 1];
        tempCh = new DoublePoint[nSlots + 1];
        tempChCore = new DoublePoint[nSlots + 1];
        slot = startSlot;
        double pos;
        pos = slot.lPos;
        tempHe[0] = new DoublePoint(pos, slot.tempHe - 273);
        tempFce[0] = new DoublePoint(pos, slot.tempFce - 273);
        tempCh[0] = new DoublePoint(pos, slot.tempChEnd - 273);
        for (int s = 1; s <= nSlots; s++) {
            slot = allSlots.get(s - 1);
            pos = slot.lPos;
            tempHe[s] = new DoublePoint(pos, slot.tempHe - 273);
            tempFce[s] = new DoublePoint(pos, slot.tempFce - 273);
            tempChSurf[s] = new DoublePoint(pos, slot.tempChSurf - 273);
            tempCh[s] = new DoublePoint(pos, slot.tempChEnd - 273);
            tempChCore[s] = new DoublePoint(pos, slot.tempChCore - 273);
        }
        traces = new Vector<OnePropertyTrace>();
        OnePropertyTrace oneTrace;
        oneTrace = new OnePropertyTrace(traceHeader[0], tempHe);
        oneTrace.setAutoRanges();
        traces.add(oneTrace);
        oneTrace = new OnePropertyTrace(traceHeader[1], tempFce);
        oneTrace.setAutoRanges();
        traces.add(oneTrace);
        oneTrace = new OnePropertyTrace(traceHeader[2], tempCh);
        oneTrace.setAutoRanges();
        traces.add(oneTrace);
        for (int t = 0; t < nTraces; t++)
            traces.get(t).setRanges(getCommonXrange(), getCommonYrange());
    }

    public TraceHeader[] getTraceHeader() {
        return traceHeader;
    }

    public TraceHeader getTraceHeader(int trace) {
        if (trace < nTraces)
            return traces.get(trace).getTraceHeader();
        else
            return null;
    }

    public DoubleRange getXrange(int trace) {
        if (trace < nTraces)
            return traces.get(trace).getXrange();
        else
            return null;
    }

    public DoubleRange getYrange(int trace) {
        if (trace < nTraces)
            return traces.get(trace).getYrange();
        else
            return null;
    }

    OnePropertyTrace getTrace(int tr) {
        if (tr < nTraces)
            return traces.get(tr);
        else
            return null;
    }

    public DoubleRange getCommonXrange() {
        OnePropertyTrace tr;
        double mn = Double.MAX_VALUE;
        double mx = Double.MIN_VALUE;
        double x;
        for (int i = 0; i < nTraces; i++) {
            tr = getTrace(i);
            if (tr.length > 1) {
                x = tr.getXmin();
                mn = (x < mn) ? x : mn;
                x = tr.getXmax();
                mx = (x > mx) ? x : mx;
            }
        }
        return OnePropertyTrace.getAutoRange(0, mx, true, false);  // formces minimum to 0
    }

    public DoubleRange getCommonYrange() {
        OnePropertyTrace tr;
        double mn = Double.MAX_VALUE;
        double mx = Double.MIN_VALUE;
        double y;
        for (int i = 0; i < nTraces; i++) {
            tr = getTrace(i);
            if (tr.length > 1) {
                y = tr.getYmin();
                mn = (y < mn) ? y : mn;
                y = tr.getYmax();
                mx = (y > mx) ? y : mx;
            }
        }
        return OnePropertyTrace.getAutoRange(0, mx, true, false);  // formces minimum to 0
    }

    public double getYat(int trace, double x) {
        if (trace < nTraces) {
            return traces.get(trace).getYat(x);
        } else {
            return Double.NaN;
        }
    }

    public DoublePoint[] getGraph(int trace) {
        if (trace < nTraces) {
            return traces.get(trace).getGraph();
        } else {
            return null;
        }
    }

    StatusWithMessage takeFceDataFromXML(String xmlStr) {
        StatusWithMessage retVal = new StatusWithMessage();
        enableDataEdit(true);
        ValAndPos vp;
        if (xmlStr.length() > 50) {
            vp = XMLmv.getTag(xmlStr, "ZoneBaseData", 0);
            retVal = takeBaseDataFromXML(vp.val);
            if (retVal.getDataStatus() == DataStat.Status.OK)  {
                vp = XMLmv.getTag(xmlStr, "radiantTube", vp.endPos);
                retVal = rt.takeRadiantTubeFroXML(vp.val);
            }
        }
        return retVal;
    }

    StatusWithMessage takeBaseDataFromXML(String xmlStr) {
        StatusWithMessage retVal = new StatusWithMessage();
        enableDataEdit(true);
        String errMsg = "";
        boolean allOK = true;
        ValAndPos vp;
        if (xmlStr.length() > 50) {
            try {
                vp = XMLmv.getTag(xmlStr, "calculationLimitMode", 0);
                calculationLimitMode = RTHeating.LimitMode.getEnum(vp.val);
                vp = XMLmv.getTag(xmlStr, "chStTemp", 0);
                chStTemp = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "chEndTemp", 0);
                chEndTemp = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "rtLimitTemp", 0);
                rtLimitTemp = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "rtLimitHeat", 0);
                rtLimitHeat = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "rtCenterAbove", 0);
                rtCenterAbove = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "rtPerM", 0);
                rtPerM = Double.valueOf(vp.val);
                updateUI();
            }
            catch(Exception e) {
                errMsg = "Taking Fce Data from xml - some problem in reading data";
                allOK = false;
            }
        }
        if (!allOK)
            retVal.setErrorMessage(errMsg);
        return retVal;
    }

    /*     public double rtCenterAbove = 0.3, rtCenterBelow = 0.3;
    double rtPerM = 3;
*/

    String baseDataToSave() {
        StringBuilder theStr =
                new StringBuilder(XMLmv.putTag("calculationLimitMode", "" + calculationLimitMode));
        theStr.append(XMLmv.putTag("chStTemp", "" + chStTemp)).
                append(XMLmv.putTag("chEndTemp", "" + chEndTemp)).
                append(XMLmv.putTag("rtLimitTemp", "" + rtLimitTemp)).
                append(XMLmv.putTag("rtLimitHeat", "" + rtLimitHeat)).
                append(XMLmv.putTag("rtCenterAbove", "" + rtCenterAbove)).
                append(XMLmv.putTag("rtPerM", "" + rtPerM));
        String retVal = XMLmv.putTag("ZoneBaseData", "\n" + theStr);
        return retVal;
    }

    public String zoneDataToSave() {
        return XMLmv.putTag("ZoneData", baseDataToSave() + rt.RTDataToSave());
    }

}
