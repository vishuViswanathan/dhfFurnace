package radiantTubeHeating;

import basic.Charge;
import basic.RadiantTube;
import directFiredHeating.DFHeating;
import display.OnePropertyTrace;
import mvUtils.display.*;
import mvUtils.math.DoublePoint;
import mvUtils.math.DoubleRange;
import mvUtils.mvXML.XMLmv;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/19/12
 * Time: 10:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class RTFurnace {
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
        sections = new Vector<>();
        RTFSection oneSec = new RTFSection(rth, this, null);
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
                RTFSection newSec = new RTFSection(rth, lastSec);
                sections.add(newSec);
                addToTabbedSectionsPane(newSec);
                if (sections.size() >= rth.maxNzones)
                    jbAddZone.setEnabled(false);
                tabbedSectionsPane.setSelectedIndex(tabbedSectionsPane.getTabCount() - 1);
                retVal = true;
            }
        }
        return retVal;
    }

    void addToTabbedSectionsPane(RTFSection sec) {
        tabbedSectionsPane.addTab("Zone#" + sec.zoneNum, null,
                sec.zoneDataP());
    }


/*
    JPanel getAllResultsPanel() {
//        startSlot = new OneRTslot(allSlots.get(0), startPos);
        startSlot = new OneRTslot(allSlots.get(0), startPos, chStTemp, chStTempSurf, chStTempCore);
        setDataForTraces();
        JPanel trendsPage = trendsPage();
//        resultsPage = resultsPage();
        JPanel allResultsP = new MultiPairColPanel("Combined Results for Furnace");
        JTabbedPane jtp= new JTabbedPane();
        jtp.addTab("Trends", trendPanel);
//        jtp.addTab("Table", resultsPage);
//        allResultsP.addItem(jtp);
        JPanel buttonP = new JPanel(new GridLayout());
//        jBredo = new JButton("Redo the Calculation");
//        jBredo.addActionListener(e -> {
//            furnace.resetZoneCalculation(zoneNum);
//            furnace.showError("RTHSection", "Not ready for this!");
//        });
//        buttonP.add(new JLabel());
//        buttonP.add(new JLabel());
//        buttonP.add(new JLabel());
//        buttonP.add(jBredo);
//        allResultsP.addItem(buttonP);
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

    JPanel trendPanel;
    JPanel gP;

    FramedPanel getGraphPanel() {
        gP = new FramedPanel(new GridLayout(1, 0));
        trendPanel =
                new RTFSection.GraphPanel(new Dimension(700, 350));
        for (int t = 0; t < nTraces; t++)
            trendPanel.addTrace(this, t, GraphDisplay.COLORS[t]);
//        if (traces.nTraces > 1)
        trendPanel.setTraceToShow(0);   // all
        trendPanel.prepareDisplay();
        gP.add(trendPanel);
        //   gP.setSize(300,300);
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
        OneRTslot slot;
        int nSlots = 0;
        for (RTFSection sec: sections) {
            if (sec.calculationDone)
                nSlots += sec.nSlots;
        }
        tempHe = new DoublePoint[nSlots + 1];
        tempFce = new DoublePoint[nSlots + 1];
        tempChSurf = new DoublePoint[nSlots + 1];
        tempCh = new DoublePoint[nSlots + 1];
        tempChCore = new DoublePoint[nSlots + 1];
        slot = sections.get(0).startSlot;
        double pos;
        pos = slot.lPos;
        tempHe[0] = new DoublePoint(pos, slot.tempHe - 273);
        tempFce[0] = new DoublePoint(pos, slot.tempFce - 273);
        tempCh[0] = new DoublePoint(pos, slot.tempChEnd - 273);
        for (RTFSection sec: sections) {
            for (int s = 1; s <= sec.nSlots; s++) {
                slot = sec.allSlots.get(s - 1);
                pos = slot.lPos;
                tempHe[s] = new DoublePoint(pos, slot.tempHe - 273);
                tempFce[s] = new DoublePoint(pos, slot.tempFce - 273);
                tempChSurf[s] = new DoublePoint(pos, slot.tempChSurf - 273);
                tempCh[s] = new DoublePoint(pos, slot.tempChEnd - 273);
                tempChCore[s] = new DoublePoint(pos, slot.tempChCore - 273);
            }
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
*/

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
        rRow = styles.xlMultiPairColPanel(rth.productionPanel, sheet, topRow, col + 3);
        row = Math.max(row, rRow);
        topRow = row + 1;
        return xlSectionDetails(sheet, styles, topRow);
    }

    boolean xlSectionDetails(Sheet sheet, ExcelStyles styles, int topRow) {
        debug("RTHFurnace.162: Not ready for Section details");
//        row = styles.xlMultiPairColPanel(radiantTubeInFceP, sheet, topRow, col);
//        rRow = styles.xlMultiPairColPanel(rth.calculModeP, sheet, topRow, col + 3);
        return false;
    }

    boolean xlTempProfile(Sheet sheet, ExcelStyles styles) {
        int row = 0;
        for (RTFSection oneSec:sections) {
            row = oneSec.xlTempProfile(sheet, styles, row) + 1;
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
//            retVal = takeRTinFceFromUI();
//            updateData();
        }
        return retVal;
    }

    public void enableDataEdit(boolean ena) {
        rth.enableDataEdit(ena);
//        ntInnerWidth.setEditable(ena);
//        ntHeightAboveCharge.setEditable(ena);
//        ntUsefullLength.setEditable(ena);
//        ntLossPerMeter.setEditable(ena);
        for (Component c:fceDetailsFields)
            c.setEnabled(ena);
        for (RTFSection sec:sections)
            sec.enableDataEdit(ena);
    }

    public void enableDataEdit(int zoneNum, boolean ena) {
        rth.enableDataEdit(ena);
//        ntInnerWidth.setEditable(ena);
//        ntHeightAboveCharge.setEditable(ena);
//        ntUsefullLength.setEditable(ena);
//        ntLossPerMeter.setEditable(ena);
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

    public void undoResults(int zoneNum) {
        rth.undoResults(RTHeating.ResultsType.getZoneEnum(zoneNum));
    }

    public void resultsReady(RTHeating.ResultsType type) {
        rth.resultsReady(type);
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
        rth.showError(title, msg, null);
    }

}
