package radiantTubeHeating;

import basic.Charge;
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
 * Date: 4/19/12
 * Time: 10:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class RTFurnace extends GraphInfoAdapter{
    RTHeating rth;
    RTHeating.LimitMode iLimitMode;
    public double width = 1.55;
    public double heightAbove = 0.8, heightBelow = 0.8;
    public double rollPitch = 0;
    public double rollDia = 0;

    public Charge charge;
    public int nChargeAlongFceWidth = 1;
    double production = 20, unitTime;
    double maxFceLen = 50, unitLen = 1;
    public double unitLoss;
    double lossPerM = 4000;
    public double uAreaChTot;
    public double uAreaChHe;
    public double uAreaWalls;
    public double chargeUwt;
    Vector<RTFSection> sections;
    JTabbedPane tabbedSectionsPane;
    JButton jbAddZone;
    InputControl ipc;

    public RTFurnace(RTHeating rth) {
        this.rth = rth;
        this.ipc = rth;
        sections = new Vector<>();
        prepareUIs();
        RTFSection oneSec = new RTFSection(ipc,this, null);
        sections.add(oneSec);
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

    public JPanel inputPage(Component chargePanel) {
        JPanel inputFrame = new JPanel(new GridBagLayout());
        GridBagConstraints gbcMf = new GridBagConstraints();

        gbcMf.anchor = GridBagConstraints.CENTER;
        gbcMf.gridx = 0;
        gbcMf.gridy = 0;
        gbcMf.insets = new Insets(0, 0, 0, 0);
        gbcMf.gridwidth = 1;
        gbcMf.gridheight = 1;
        gbcMf.gridy++;
        inputFrame.add(fceDetailsP(), gbcMf);
        gbcMf.gridheight = 1;
        gbcMf.gridx = 1;
        gbcMf.gridy = 1;
        inputFrame.add(chargePanel, gbcMf);
        gbcMf.gridy++;
        gbcMf.gridx = 0;
        gbcMf.gridwidth = 2;
        inputFrame.add(sectionDetailsPanel(), gbcMf);
        return inputFrame;
    }

    void prepareUIs() {
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
                100, 20000, "#,###", "Furnace Calculation Step Length (mm)"));


        fceDetailsFields.add(ntLossPerMeter = new NumberTextField(ipc, lossPerM, 6, false,
                -10000, 200000, "#,###", "Loss per Furnace length (kcal/h.m)"));
    }

    public JPanel fceDetailsP() {
        if (!fceDataFieldsSet) {
            MultiPairColPanel pan = new MultiPairColPanel("Furnace Data");
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

    JPanel sectionDetailsPanel() {
        this.ipc = ipc;
        FramedPanel outerP = new FramedPanel(new BorderLayout());
        tabbedSectionsPane = new JTabbedPane();
        for (RTFSection oneSec: sections) {
            addToTabbedSectionsPane(oneSec);
        }
        outerP.add(tabbedSectionsPane);
        jbAddZone = new JButton("Add a Zone");
        jbAddZone.addActionListener(e-> {
            addSection();
        });
        jbAddZone.setEnabled(true);
        JPanel addButtP = new JPanel(new GridLayout());
        addButtP.add(jbAddZone);
        for (int l = 0; l < 4; l++)
            addButtP.add(new JLabel());
        outerP.add(addButtP, BorderLayout.SOUTH);
        return outerP;
    }

    boolean addSection() {
        boolean retVal = false;
        int lastZone = sections.size();
        if (lastZone < rth.maxNzones) {
            RTFSection lastSec = sections.get(lastZone - 1);
            if (lastSec.calculationDone) {
                RTFSection newSec = new RTFSection(ipc, lastSec);
                sections.add(newSec);
                addToTabbedSectionsPane(newSec);
                if (sections.size() >= rth.maxNzones)
                    jbAddZone.setEnabled(false);
                tabbedSectionsPane.setSelectedIndex(tabbedSectionsPane.getTabCount() - 1);
                retVal = true;
            }
            else
                showError("Adding as Zone", "The last zone is not yet calculated");
        }
        else
            showError("Adding as Zone", "Reached the limit of " + rth.maxNzones);
        return retVal;
    }

    void addToTabbedSectionsPane(RTFSection sec) {
        tabbedSectionsPane.addTab("Zone#" + sec.zoneNum, null,
                sec.zoneDataP());
    }

// ##########################
//    ###################
    OneRTslot startSlot;
    Vector<OneRTslot> allZoneSlots;
    MultiPairColPanel allResultsP;
    JPanel resultsPage;
    JPanel trendsPage;
    ScrollPane resultScroll;
    JTable resultTable;
    GraphPanel trendPanel;
    FramedPanel gP;
    int nAllZoneSlots = 0;
    MyTableModel tableModel;

    JPanel getAllResultsPanel() {
        setDataForTraces();
        trendsPage = trendsPage();
        resultsPage = resultsPage();
        allResultsP = new MultiPairColPanel("Combined Results for Furnace");
        JTabbedPane jtp= new JTabbedPane();
        jtp.addTab("Trends", trendPanel);
        jtp.addTab("Table", resultsPage);
        allResultsP.addItem(jtp);
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
        Insets lttp = new Insets(vse, hse, vs, hs);
        gbcTp.anchor = GridBagConstraints.CENTER;
        gbcTp.gridx = 0;
        gbcTp.gridy = 0;
        gbcTp.insets = lttp;
        titlePanel.add(new Label(OneRTslot.header1), gbcTp);
        gbcTp.gridy++;
        gbcTp.insets = lt;
        JLabel jLtitle = new JLabel(OneRTslot.unitsHeader);
        titlePanel.add(jLtitle, gbcTp);
        gbcTp.gridy++;
        return titlePanel;
    }

    public JTable getResultTable() {
        tableModel = new MyTableModel();
        JTable table = new JTable(tableModel);
        table.setDefaultRenderer(table.getColumnClass(1), new CellRenderer());
        return table;
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
        allZoneSlots = new Vector<>();
        nAllZoneSlots = 0;
        for (RTFSection sec: sections) {
            if (sec.calculationDone)
                allZoneSlots.addAll(sec.allSlots);
            else
                break;
        }
        nAllZoneSlots = allZoneSlots.size();
        tempHe = new DoublePoint[nAllZoneSlots + 1];
        tempFce = new DoublePoint[nAllZoneSlots + 1];
        tempChSurf = new DoublePoint[nAllZoneSlots + 1];
        tempCh = new DoublePoint[nAllZoneSlots + 1];
        tempChCore = new DoublePoint[nAllZoneSlots + 1];
        startSlot = sections.get(0).startSlot;
        double pos;
        pos = startSlot.lPos;
        tempHe[0] = new DoublePoint(pos, startSlot.tempHe - 273);
        tempFce[0] = new DoublePoint(pos, startSlot.tempFce - 273);
        tempCh[0] = new DoublePoint(pos, startSlot.tempChEnd - 273);
        int i = 1;
        for (OneRTslot slot:allZoneSlots) {
            pos = slot.lPos;
            tempHe[i] = new DoublePoint(pos, slot.tempHe - 273);
            tempFce[i] = new DoublePoint(pos, slot.tempFce - 273);
            tempChSurf[i] = new DoublePoint(pos, slot.tempChSurf - 273);
            tempCh[i] = new DoublePoint(pos, slot.tempChEnd - 273);
            tempChCore[i] = new DoublePoint(pos, slot.tempChCore - 273);
            i++;
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

    OnePropertyTrace getTrace(int tr) {
        if (tr < nTraces)
            return traces.get(tr);
        else
            return null;
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
        return OnePropertyTrace.getAutoRange(mn, mx, true, false);  // formces minimum to 0
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
            return nAllZoneSlots + 1;
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

            if (row >= 0 && row < nAllZoneSlots + 1) {
                if (row == 0)
                    slot = startSlot;
                else
                    slot = allZoneSlots.get(row - 1);
                data = slot.getColData(col);
            } else
                data = "NO DATA";
            return data;
        }

        public double getdoubleValueAt(int row, int col) { // was(int xPos, int yPos) {
            double data;
            OneRTslot slot;

            if (row >= 0 && row < allZoneSlots.size() + 1) {
                if (row == 0)
                    slot = startSlot;
                else
                    slot = allZoneSlots.get(row - 1);
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

// ####################


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
//        topRow = row + 1;
//        rRow = styles.xlMultiPairColPanel(rth.productionPanel, sheet, topRow, col + 3);
//        row = Math.max(row, rRow);
        topRow = row + 1;
        return xlSectionDetails(sheet, styles, topRow);
    }

    boolean xlSectionDetails(Sheet sheet, ExcelStyles styles, int topRow) {
        Cell cell;
        Row r;
        int row = topRow + 2;
        for (RTFSection sec: sections) {
            r = sheet.createRow(row);
            cell = r.createCell(0);
            cell.setCellStyle(styles.csHeader1);
            cell.setCellValue("Zone #" + sec.zoneNum);
            row = sec.xlSectionData(sheet, styles, row + 1);
            row += 1;
        }
        return true;
    }

    boolean areAllResultsReady() {
        boolean ready = true;
        for (RTFSection oneSec:sections)
            ready &= oneSec.calculationDone;
        return ready;
    }

    boolean xlTempProfile(Sheet sheet, ExcelStyles styles) {
        int row = 0;
        for (RTFSection oneSec:sections) {
            if (oneSec.calculationDone)
                row = oneSec.xlTempProfile(sheet, styles, row) + 1;
            else
                break;
        }
        return row > 0;
   }

    public boolean prepareForCalculation() {
        boolean proceed = takeFceDetailsFromUI();
        if (proceed) {
            charge = rth.getChargeDetails(unitLen);
            if (charge != null)
                proceed = setProduction();
            else
                proceed = false;
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
        }
        return retVal;
    }

    void upDateUIfromData() {
        ntInnerWidth.setData(width * 1000);
        ntHeightAboveCharge.setData(heightAbove * 1000);
        ntUsefullLength.setData(maxFceLen * 1000);
        ntUfceLen.setData(unitLen * 1000);
        ntRollDia.setData(rollDia * 1000);
        ntRollPitch.setData(rollPitch * 1000);
        ntLossPerMeter.setData(lossPerM);
    }

    public void enableDataEdit(boolean ena) {
        rth.enableDataEdit(ena);
        for (Component c:fceDetailsFields)
            c.setEnabled(ena);
        for (RTFSection sec:sections)
            sec.enableDataEdit(ena);
    }

    public void enableDataEdit(int zoneNum, boolean ena) {
        rth.enableDataEdit(ena);
        for (Component c:fceDetailsFields)
            c.setEnabled(ena);
        if (ena)
            for (RTFSection sec:sections)
                sec.enableDataEdit(ena);
        else
            sections.get(zoneNum - 1).enableDataEdit(ena);
    }

    public void resetZoneCalculation(int zoneNum) {
        for (int z = zoneNum - 1; z < sections.size(); z++) {
            sections.get(z).enableDataEdit(true);
        }

        tabbedSectionsPane.setSelectedIndex(zoneNum - 1);
        rth.switchToInputPage();
    }

    void inputPageForSelectedZone(int zoneNum) {
        if (zoneNum <= sections.size())
            tabbedSectionsPane.setSelectedIndex(zoneNum - 1);
        rth.switchToInputPage();
    }

    public boolean setProduction() {
        production = rth.getProduction();
        nChargeAlongFceWidth = rth.getNChargeAlongFceWidth();
        this.unitLoss = lossPerM * unitLen;
        uAreaChTot = charge.projectedTopArea * 2 *  nChargeAlongFceWidth;
        uAreaChHe = uAreaChTot;
        uAreaWalls = (width + 2 * heightAbove + charge.getHeight()) * 2 *unitLen;
        chargeUwt = charge.unitWt * nChargeAlongFceWidth;
        unitTime = unitLen / (production / 60 / chargeUwt);
        return true;
    }

//    public void switchToSelectedPage(JPanel page) {
//        rth.switchToSelectedPage(page);
//    }

    public void addResult(RTHeating.ResultsType type, JPanel p) {
        rth.addResult(type, p);
    }

    void addItTemp(RTHeating.ResultsType type, JPanel p) {
        addResult(type, p);
    }

    public void undoResults(int zoneNum) {
        rth.undoResults(RTHeating.ResultsType.getZoneEnum(zoneNum));
        undoSummedResults();
    }

    void undoSummedResults() {
        rth.undoResults(RTHeating.ResultsType.ALLZONERESULTS);
    }

    public void resultsReady(RTHeating.ResultsType type) {
        // get all ResultsPanel Ready
        getAllResultsPanel();
        RTHeating.ResultsType rType = RTHeating.ResultsType.ALLZONERESULTS;
        addResult(rType, allResultsP);
        rth.resultsReady(type);
    }

    public String fceDataInXML() {
        StringBuilder fceStr =
                new StringBuilder(fceDataToSave());
        StringBuilder secStr = new StringBuilder();
        for (RTFSection sec:sections) {
            secStr.append(sec.zoneDataToSave());
        }
        fceStr.append(XMLmv.putTag("AllZones", "\n" + secStr));
        return XMLmv.putTag("theFurnace",  "\n" + fceStr);
    }

    public String fceDataToSave() {
        StringBuilder theStr = new StringBuilder(XMLmv.putTag("width", "" + width));
        theStr.append(XMLmv.putTag("heightAbove", "" + heightAbove)).
                append(XMLmv.putTag("maxFceLen", "" + maxFceLen)).
                append(XMLmv.putTag("unitLen", "" + unitLen)).
                append(XMLmv.putTag("rollDia", "" + rollDia)).
                append(XMLmv.putTag("rollPitch", "" + rollPitch)).
                append(XMLmv.putTag("lossPerM", "" + lossPerM));
        String retVal = XMLmv.putTag("CommonData", "\n" + theStr);
        return retVal;
    }

    StatusWithMessage takeFceDataFromXML(String xmlStr) {
        StatusWithMessage retVal = new StatusWithMessage();
        enableDataEdit(true);
        ValAndPos vp;
        if (xmlStr.length() > 50) {
            try {
                vp = XMLmv.getTag(xmlStr, "CommonData", 0);
                retVal = takeFceCommanDataFromXML(vp.val);
                if (retVal.getDataStatus() == DataStat.Status.OK) {
                    vp = XMLmv.getTag(xmlStr, "AllZones", vp.endPos);
                    String allZones = vp.val;
                    if (allZones.length() > 50) {
                        vp = XMLmv.getTag(allZones, "ZoneData");
                        RTFSection prevSec = null;
                        sections.clear();
                        while (vp.val.length() > 10) {
                            RTFSection sec = new RTFSection(ipc, this, prevSec);
                            retVal = sec.takeFceDataFromXML(vp.val);
                            if (retVal.getDataStatus() == DataStat.Status.OK) {
                                sections.add(sec);
                                prevSec = sec;
                                vp = XMLmv.getTag(allZones, "ZoneData", vp.endPos);
                            }
                            else
                                break;
                        }
                    }
                }
            }
            catch(Exception e) {
                retVal.setErrorMessage("takeFceDataFroXML - Some problem taking data at #824");
            }
        }
        else {
            retVal.addErrorMessage("reading Fce data from xml - data too short at #546");
        }
        return retVal;
    }

    StatusWithMessage takeFceCommanDataFromXML(String xmlStr) {
        StatusWithMessage retVal = new StatusWithMessage();
        enableDataEdit(true);
        String errMsg = "";
        boolean allOK = true;
        ValAndPos vp;
        if (xmlStr.length() > 50) {
            try {
                vp = XMLmv.getTag(xmlStr, "width", 0);
                width = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "heightAbove", 0);
                heightAbove = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "maxFceLen", 0);
                maxFceLen = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "unitLen", 0);
                unitLen = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "rollDia", 0);
                rollDia = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "rollPitch", 0);
                rollPitch = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "lossPerM", 0);
                lossPerM = Double.valueOf(vp.val);
                upDateUIfromData();
            }
            catch (Exception e) {
                allOK = false;
                errMsg = "Reading Fce Common Data from xml - Some error in data";
            }
            if (!allOK)
                retVal.setErrorMessage(errMsg);
        }
        else {
            retVal.addErrorMessage("reading Fce Common data from xml - data too short at #583");
        }
        return retVal;
    }

    public String prodDataToSave() {
        String retVal;
        retVal = "<production>" + XMLmv.putTag("output", "" + production);
        retVal += "</production>" + "\n";
        return retVal;
    }

    public void debug(String msg) {
        rth.debug(msg);
    }

    public void showError(String title, String msg) {
        rth.showError(title, msg);
    }

}
