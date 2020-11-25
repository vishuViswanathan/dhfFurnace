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
    RTHeating rth;
    RTHeating.LimitMode iLimitMode;
    public double width = 1.55;
    public double heightAbove = 0.8, heightBelow = 0.8;
    public double rtCenterAbove = 0.3, rtCenterBelow = 0.3;
    public RadiantTube rt;
    double rtPerM = 3;
    double topRTperM, botRTperM;
    public double rollPitch = 0;
    public double rollDia = 0;

    public Charge charge;
    public int nChargeAlongFceWidth = 1;
    double production = 20, unitTime;
    double maxFceLen = 50, unitLen = 1;
    public double unitLoss;
    double lossPerM = 4000;
    public double uAreaChTot, uAreaHeTot;
    public double uAreaChHe, uAreaHeCh;
    public double uAreaWalls;
    public double chargeUwt;
    double gapHeCh ;
    Vector<RTFSection> sections;
    JTabbedPane tabbedSectionsPane;
    Vector<OneRTslot> allSlots;
    int maxSlots;
    public int nSlots = 0;
    MyTableModel tableModel;
    OneRTslot startSlot;

    public RTFurnace(RTHeating rth, int nChargeAlongFceWidth) {
        this.rth = rth;
        this.nChargeAlongFceWidth = nChargeAlongFceWidth;
        sections = new Vector<>();
        sections.add(new RTFSection(this));
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
    NumberTextField ntRollDia;
    NumberTextField ntRollPitch;

    NumberTextField ntUsefullLength;
    NumberTextField ntUfceLen;
    NumberTextField ntLossPerMeter;
    boolean fceDataFieldsSet = false;
    MultiPairColPanel fceDetailsPanel;
    Vector<NumberTextField> fceDetailsFields;

    public JPanel inputPage(InputControl ipc, Component chargePanel) {
        JPanel inputFrame = new JPanel(new GridBagLayout());
        GridBagConstraints gbcMf = new GridBagConstraints();

        gbcMf.anchor = GridBagConstraints.CENTER;
        gbcMf.gridx = 0;
        gbcMf.gridy = 0;
        gbcMf.insets = new Insets(0, 0, 0, 0);
        gbcMf.gridwidth = 1;
        gbcMf.gridheight = 1;
        gbcMf.gridy++;
        inputFrame.add(fceDetailsP(ipc), gbcMf);
        gbcMf.gridheight = 1;
        gbcMf.gridx = 1;
        gbcMf.gridy = 1;
        inputFrame.add(chargePanel, gbcMf);
        gbcMf.gridy++;
        gbcMf.gridx = 0;
        gbcMf.gridwidth = 2;
        inputFrame.add(sectionDetailsPanel(ipc), gbcMf);
        return inputFrame;
    }

    public JPanel fceDetailsP(InputControl ipc) {
        if (!fceDataFieldsSet) {
            MultiPairColPanel pan = new MultiPairColPanel("Furnace Data");
            fceDetailsFields = new Vector<>();
            fceDetailsFields.add(ntInnerWidth = new NumberTextField(ipc, width * 1000, 6, false,
                    200, 200000, "#,###", "Inner Width (mm)"));
            fceDetailsFields.add(ntHeightAboveCharge = new NumberTextField(ipc, heightAbove * 1000, 6,
                    false, 200, 200000, "#,###", "Height above Charge (mm)"));
            fceDetailsFields.add(ntRollDia = new NumberTextField(ipc, rollDia * 1000, 6,
                    false, 0, 1000, "#,###", "Support Roll dia (mm)"));
            fceDetailsFields.add(ntRollPitch = new NumberTextField(ipc, rollPitch * 1000, 6,
                    false, 0, 10000, "#,###", "Roll pitch (mm)"));
            fceDetailsFields.add(ntUsefullLength = new NumberTextField(ipc, maxFceLen * 1000, 6,
                    false, 2000, 200000, "#,###", "Maximum Length (mm)"));
            fceDetailsFields.add(ntUfceLen = new NumberTextField(ipc, unitLen * 1000, 6, false,
                    200, 20000, "#,###", "Furnace Calculation Step Length (mm)"));


            fceDetailsFields.add(ntLossPerMeter = new NumberTextField(ipc, lossPerM, 6, false,
                    -10000, 200000, "#,###", "Loss per Furnace length (kcal/h.m)"));
            pan.addItemPair(ntInnerWidth);
            pan.addItemPair(ntHeightAboveCharge);
            pan.addItemPair(ntUsefullLength);
            pan.addItemPair(ntUfceLen);
            pan.addBlank();
            pan.addItemPair(ntRollDia);
            pan.addItemPair(ntRollPitch);
            pan.addBlank();
            pan.addItemPair(ntLossPerMeter);
            fceDetailsPanel = pan;
            fceDataFieldsSet = true;
        }
        return fceDetailsPanel;
    }

    JPanel sectionDetailsPanel(InputControl ipc) {
        FramedPanel outerP = new FramedPanel(new BorderLayout());
        tabbedSectionsPane = new JTabbedPane();
        tabbedSectionsPane.addTab("Sec#1", null,
                sections.get(0).zoneDataP(ipc));
        outerP.add(tabbedSectionsPane);
        return outerP;
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

    public boolean prepareForCalculation() {
        boolean proceed = takeFceDetailsFromUI();
        if (proceed) {
            charge = rth.getChargeDetails(unitLen);
            setProduction();
            proceed = (charge != null);
        }
        return proceed;
    }

    public boolean takeFceDetailsFromUI() {
        boolean retVal = true;
        for (NumberTextField f : fceDetailsFields)
            retVal &= !f.isInError();
        if (retVal) {
            width = ntInnerWidth.getData() / 1000;
            heightAbove = ntHeightAboveCharge.getData() / 1000;
            maxFceLen = ntUsefullLength.getData() / 1000;
            unitLen = ntUfceLen.getData() / 1000;
            rollDia = ntRollDia.getData() / 1000;
            rollPitch = ntRollPitch.getData() / 1000;
            lossPerM = ntLossPerMeter.getData();
//            retVal = takeRTinFceFromUI();
            updateData();
        }
        return retVal;
    }

    public void enableDataEdit(boolean ena) {
        ntInnerWidth.setEditable(ena);
        ntHeightAboveCharge.setEditable(ena);
        ntUsefullLength.setEditable(ena);
        ntLossPerMeter.setEditable(ena);
        rt.enableDataEdit(ena);

    }

    public void setProduction() {
        production = rth.getProduction();
        this.unitLoss = lossPerM * unitLen;
        uAreaChTot = charge.projectedTopArea * 2 *  nChargeAlongFceWidth;
        uAreaChHe = uAreaChTot;
//        uAreaHeTot = rt.getTotHeatingSurface() * (topRTperM + botRTperM) * unitLen;
//        uAreaHeCh = uAreaHeTot / 2; // ?? * 2 / (Math.PI / 2); // 20201014
        uAreaWalls = (width + 2 * heightAbove + charge.getHeight()) * 2 *unitLen;
//        gapHeCh = rtCenterAbove; // was before 20201014 rtCenterAbove - rt.dia / 2;
//        maxSlots = (int) (maxFceLen / unitLen);
//        allSlots = new Vector<OneRTslot>();
        double lPos = unitLen;
        OneRTslot slot;
        double endTime = 0;
        chargeUwt = charge.unitWt * nChargeAlongFceWidth;
        unitTime = unitLen / (production / 60 / chargeUwt);
        OneRTslot prevSlot = null;
        for (int i = 0; i < maxSlots; i++) {
            endTime += unitTime;
//            slot = new OneRTslot(this, lPos, prevSlot);
            slot = new OneRTslot(this, lPos, prevSlot, unitLen, rtPerM, rollDia, rollPitch);
            slot.setCharge(charge, chargeUwt, unitTime, endTime);
            allSlots.add(slot);
            lPos += unitLen;
            prevSlot = slot;
        }
    }

    MultiPairColPanel radiantTubeInFceP;
    Vector<NumberTextField> tubesInFceFields;

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

    public void switchToSelectedPage(JPanel page) {
        rth.switchToSelectedPage(page);
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
/*
        retVal += XMLmv.putTag("chStTemp", "" + (chStTemp - 273));
        retVal += XMLmv.putTag("chEndTemp", "" + (chEndTemp - 273));
        retVal += XMLmv.putTag("rtLimitTemp", "" + (srcTemp - 273));
        retVal += XMLmv.putTag("rtLimitHeat", "" + rtHeat);
*/
        retVal += "</production>" + "\n";
        return retVal;
    }

    public void debug(String msg) {
        rth.debug(msg);
    }
}
