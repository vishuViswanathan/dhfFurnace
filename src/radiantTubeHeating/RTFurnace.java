package radiantTubeHeating;

import display.OnePropertyTrace;
import basic.Charge;
import basic.RadiantTube;
import mvUtils.display.*;
import mvUtils.math.DoublePoint;
import mvUtils.math.DoubleRange;
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
 * Date: 4/19/12
 * Time: 10:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class RTFurnace extends GraphInfoAdapter {
//    public enum LimitMode {RTTEMP, RTHEAT}

    RTHeating rth;
    RTHeating.LimitMode iLimitMode;
    public double width = 1.55;
    public double heightAbove = 0.8, heightBelow = 0.8;
    public double rtCenterAbove = 0.3, rtCenterBelow = 0.3;
    public RadiantTube rt;
    double rtPerM = 3;
    double topRTperM, botRTperM;
    double rollPitch = 0;
    double rollDia = 0;

    public Charge charge;
    public int nChargeAlongFceWidth = 1;
    double production = 20, unitTime;
    double maxFceLen = 50, unitLen = 1;
    double unitLoss;
    double lossPerM = 4000;
    double uAreaChTot, uAreaHeTot;
    public double uAreaChHe, uAreaHeCh;
    double uAreaWalls;
    double gapHeCh ;
    Vector<OneRTslot> allSlots;
    int maxSlots;
    public int nSlots = 0;
    MyTableModel tableModel;
    OneRTslot startSlot;

    public RTFurnace(RTHeating rth) {
        this.rth = rth;
        rt = new RadiantTube();
    }

    public RTFurnace(double width, double heightAbove, double rtCenterAbove, RadiantTube radiantTube, double rtPerM) {
        this.width = width;
        this.heightAbove = heightAbove;
        this.rtCenterAbove = rtCenterAbove;
        this.rt = radiantTube;
        this.rtPerM = rtPerM;
        updateData();
    }

    void updateData() {
        this.heightBelow = this.heightAbove;
        this.rtCenterBelow = this.rtCenterAbove;
        this.topRTperM = rtPerM / 2;
        this.botRTperM = this.topRTperM;
    }

    NumberTextField ntInnerWidth;
    NumberTextField ntHeightAboveCharge;
    NumberTextField ntUsefullLength;
    NumberTextField ntLossPerMeter;
    boolean fceDataFieldsSet = false;
    MultiPairColPanel fceDetailsPanel;
    Vector<NumberTextField> fceDetailsFields;

    public JPanel fceDetailsP(InputControl ipc) {
        if (!fceDataFieldsSet) {
            MultiPairColPanel pan = new MultiPairColPanel("Furnace Data");
            fceDetailsFields = new Vector<>();
            fceDetailsFields.add(ntInnerWidth = new NumberTextField(ipc, width * 1000, 6, false,
                    200, 200000, "#,###", "Inner Width (mm)"));
            fceDetailsFields.add(ntHeightAboveCharge = new NumberTextField(ipc, heightAbove * 1000, 6,
                    false, 200, 200000, "#,###", "Height above Charge (mm)"));
            fceDetailsFields.add(ntUsefullLength = new NumberTextField(ipc, maxFceLen, 6,
                    false, 0.20, 2000, "#,###.000", "Useful Length (m)"));
            fceDetailsFields.add(ntLossPerMeter = new NumberTextField(ipc, lossPerM, 6, false,
                    -10000, 200000, "#,###", "Loss per Furnace length (kcal/h.m)"));
            pan.addItemPair(ntInnerWidth);
            pan.addItemPair(ntHeightAboveCharge);
            pan.addItemPair(ntUsefullLength);
            pan.addBlank();
            pan.addItemPair(ntLossPerMeter);
            fceDetailsPanel = pan;
            fceDataFieldsSet = true;
        }
        return fceDetailsPanel;
    }

    boolean xlFurnaceData(Sheet sheet, ExcelStyles styles) {
        Cell cell;
        Row r;
        r = sheet.createRow(0);
        cell = r.createCell(0);
        cell.setCellStyle(styles.csHeader1);
        cell.setCellValue("Furnace Details");

        sheet.setColumnWidth(1, 10000);
        sheet.setColumnWidth(2, 3000);
        sheet.setColumnWidth(3, 500);
        sheet.setColumnWidth(4, 9000);
        sheet.setColumnWidth(5, 3000);
        int topRow = 4, row, rRow;
        int col = 1;
        row = styles.xlMultiPairColPanel(fceDetailsPanel, sheet, topRow, col);
        rRow = styles.xlMultiPairColPanel(rth.chargeP, sheet, topRow, col + 3);
        row = Math.max(row, rRow);
        row = Math.max(row, rRow);
        topRow = row + 1;
        row = styles.xlMultiPairColPanel(rt.getDataPanel(), sheet, topRow, col);
        rRow = styles.xlMultiPairColPanel(rth.productionPanel, sheet, topRow, col + 3);
        row = Math.max(row, rRow);
        topRow = row + 1;
        row = styles.xlMultiPairColPanel(radiantTubeInFceP, sheet, topRow, col);
        rRow = styles.xlMultiPairColPanel(rth.calculModeP, sheet, topRow, col + 3);
        return true;
    }

    boolean xlTempProfile(Sheet sheet, ExcelStyles styles) {
        Cell cell;
        Row r;
        r = sheet.createRow(0);
        cell = r.createCell(0);
        cell.setCellStyle(styles.csHeader1);
        cell.setCellValue("TEMPERATURE PROFILE OF SECTIONS");
        int topRow = 4;
        int leftCol = 1;
        JTable table = getResultTable();
        int row = styles.xlAddXLCellData(sheet, topRow, leftCol, table);
        return true;
    }

    public boolean takeFceDetailsFromUI() {
        boolean retVal = true;
        for (NumberTextField f : fceDetailsFields)
            retVal &= !f.isInError();
        if (retVal) {
            width = ntInnerWidth.getData() / 1000;
            heightAbove = ntHeightAboveCharge.getData() / 1000;
            maxFceLen = ntUsefullLength.getData();
            lossPerM = ntLossPerMeter.getData();
            retVal = takeRTinFceFromUI();
            updateData();
        }
        return retVal;
    }

    public void enableDataEdit(boolean ena) {
        ntInnerWidth.setEditable(ena);
        ntHeightAboveCharge.setEditable(ena);
        ntUsefullLength.setEditable(ena);
        ntLossPerMeter.setEditable(ena);
        ntTubesPerFceLength.setEditable(ena);
        ntRadiantTubeChargeDistance.setEditable(ena);
        rt.enableDataEdit(ena);

    }

    public void setProduction(Charge charge, int nItemsInFceWidth, double production,
                              double unitLen, RTHeating.LimitMode iCalculMode) {
        this.charge = charge;
        this.nChargeAlongFceWidth = nItemsInFceWidth;
        this.production = production;
        this.unitLen = unitLen;
//        this.maxFceLen = maxFceLen;
        this.unitLoss = lossPerM * unitLen;
        this.iLimitMode = iCalculMode;
//        uAreaChTot = charge.getLength() * 2 * unitLen * nItemsInFceWidth;
        uAreaChTot = charge.getProjectedTopArea() * 2 *  nItemsInFceWidth;
        uAreaChHe = uAreaChTot;
        uAreaHeTot = rt.getTotHeatingSurface() * (topRTperM + botRTperM) * unitLen;
        uAreaHeCh = uAreaHeTot / 2; // ?? * 2 / (Math.PI / 2); // 20201014
        uAreaWalls = (width + 2 * heightAbove + charge.getHeight()) * 2 *unitLen;
        gapHeCh = rtCenterAbove; // was before 20201014 rtCenterAbove - rt.dia / 2;
        maxSlots = (int) (maxFceLen / unitLen);
        allSlots = new Vector<OneRTslot>();
        double lPos = unitLen;
        OneRTslot slot;
        double endTime = 0;
        double uWt = charge.unitWt * nItemsInFceWidth;
        unitTime = unitLen / (production / 60 / uWt);
        OneRTslot prevSlot = null;
        for (int i = 0; i < maxSlots; i++) {
            endTime += unitTime;
//            slot = new OneRTslot(this, lPos, prevSlot);
            slot = new OneRTslot(this, lPos, prevSlot, unitLen, rtPerM, 0, 0);
            slot.setCharge(charge, uWt, unitTime, endTime);
            allSlots.add(slot);
            lPos += unitLen;
            prevSlot = slot;
        }
    }

    NumberTextField ntTubesPerFceLength;
    NumberTextField ntRadiantTubeChargeDistance;
    boolean radiantTubeFieldsSet = false;
    MultiPairColPanel radiantTubeInFceP;
    Vector<NumberTextField> tubesInFceFields;

    public JPanel radiantTubesP(InputControl ipc) {
        if (!radiantTubeFieldsSet) {
            MultiPairColPanel pan = new MultiPairColPanel("Radiant Tubes in Furnace");
            pan.addItem(rt.radiantTubesP(ipc));
            tubesInFceFields = new Vector<>();
            tubesInFceFields.add(ntTubesPerFceLength = new NumberTextField(ipc, rtPerM, 6, false,
                    0, 100, "#.000", "Number of Tube per m of Furnace Length"));
            tubesInFceFields.add(ntRadiantTubeChargeDistance = new NumberTextField(ipc, rtCenterAbove * 1000, 6, false,
                    0, 10000, "#,###", "RT Centerline above Charge Surface (mm)"));
            pan.addItem("<html><B>Tubes to have a minimum gap of One Dia between the legs</B></html>");
            pan.addItemPair(ntTubesPerFceLength);
            pan.addItemPair(ntRadiantTubeChargeDistance);
            radiantTubeInFceP = pan;
            radiantTubeFieldsSet = true;
        }
        return radiantTubeInFceP;
    }

    boolean takeRTinFceFromUI() {
        boolean retVal = true;
        for (NumberTextField f : tubesInFceFields)
            retVal &= !f.isInError();
        if (retVal) {
            rtPerM = ntTubesPerFceLength.getData();
            rtCenterAbove = ntRadiantTubeChargeDistance.getData() / 1000;
            retVal = rt.takeFromUI();
            if (retVal) {
                if (heightAbove <= (rtCenterAbove + rt.dia)) {
                    rth.showError("Error in Furnace Height/ Radiant tube location");
                    retVal = false;
                }
            }
        }
        return retVal;
    }

    double chStTemp, chEndTemp, srcTemp, rtHeat;

    // Temperatures in K
    public boolean calculate(double chStTemp, double chEndTemp, double srcTemp, double rtHeat) {
        this.chStTemp = chStTemp;
        this.chEndTemp = chEndTemp;
        this.srcTemp = srcTemp;
        this.rtHeat = rtHeat;
        OneRTslot slot = allSlots.get(0);
        double srcHeat = rtHeat * 860 * (topRTperM + botRTperM) * unitLen;
        double exitTemp = slot.calculate(chStTemp, srcTemp, 0, srcHeat, iLimitMode);
        nSlots = 1;
        while ((exitTemp < chEndTemp) && (nSlots < allSlots.size())) {
            slot = allSlots.get(nSlots);
            exitTemp = slot.calculate();
            nSlots++;
        }
        startSlot = new OneRTslot(allSlots.get(0), 0);
        setDataForTraces();
        return true;
    }

    public JTable getResultTable() {
        tableModel = new MyTableModel();
        JTable table = new JTable(tableModel);
        table.setDefaultRenderer(table.getColumnClass(1), new CellRenderer());
        return table;
    }

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
    DoublePoint[] tempCh;
    Vector<OnePropertyTrace> traces;
    DoubleRange commonX, commonY;

    void setDataForTraces() {
        OneRTslot slot;
        tempHe = new DoublePoint[nSlots + 1];
        tempFce = new DoublePoint[nSlots + 1];
        tempCh = new DoublePoint[nSlots + 1];
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
            tempCh[s] = new DoublePoint(pos, slot.tempChEnd - 273);
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

    public String resultsInCVS() {
        return "\n\n\n" + tableModel.getResultsCVS();
    }


    public String fceDataToSave() {
        String retVal;
        retVal = "<furnace>" + XMLmv.putTag("width", "" + width * 1000);
        retVal += XMLmv.putTag("heightAbove", "" + heightAbove * 1000);
        retVal += XMLmv.putTag("maxFceLen", "" + maxFceLen * 1000);
        retVal += XMLmv.putTag("perMLoss", "" + unitLoss / unitTime);  // TODO check
        retVal += "</furnace>" + "\n";
        return retVal;
    }

    //chStTemp, double chEndTemp, double srcTemp, double rtHeat
    public String prodDataToSave() {
        String retVal;
        retVal = "<production>" + XMLmv.putTag("output", "" + production);
        retVal += XMLmv.putTag("chStTemp", "" + (chStTemp - 273));
        retVal += XMLmv.putTag("chEndTemp", "" + (chEndTemp - 273));
        retVal += XMLmv.putTag("rtLimitTemp", "" + (srcTemp - 273));
        retVal += XMLmv.putTag("rtLimitHeat", "" + rtHeat);
        retVal += "</production>" + "\n";
        return retVal;
    }
}
