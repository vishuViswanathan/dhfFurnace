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
    Vector<OneRTslot> allSlots;
    public int nSlots = 0;
    OneRTslot startSlot;

    public RTFurnace(RTHeating rth, int nChargeAlongFceWidth) {
        this.rth = rth;
        this.nChargeAlongFceWidth = nChargeAlongFceWidth;
        sections = new Vector<>();
        sections.add(new RTFSection(this));
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
//        row = styles.xlMultiPairColPanel(rt.getDataPanel(), sheet, topRow, col);
        rRow = styles.xlMultiPairColPanel(rth.productionPanel, sheet, topRow, col + 3);
        row = Math.max(row, rRow);
        topRow = row + 1;
        row = styles.xlMultiPairColPanel(radiantTubeInFceP, sheet, topRow, col);
        rRow = styles.xlMultiPairColPanel(rth.calculModeP, sheet, topRow, col + 3);
        return true;
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
//            updateData();
        }
        return retVal;
    }

    public void enableDataEdit(boolean ena) {
        ntInnerWidth.setEditable(ena);
        ntHeightAboveCharge.setEditable(ena);
        ntUsefullLength.setEditable(ena);
        ntLossPerMeter.setEditable(ena);
    }

    public void setProduction() {
        production = rth.getProduction();
        this.unitLoss = lossPerM * unitLen;
        uAreaChTot = charge.projectedTopArea * 2 *  nChargeAlongFceWidth;
        uAreaChHe = uAreaChTot;
        uAreaWalls = (width + 2 * heightAbove + charge.getHeight()) * 2 *unitLen;
        chargeUwt = charge.unitWt * nChargeAlongFceWidth;
        unitTime = unitLen / (production / 60 / chargeUwt);
    }

    MultiPairColPanel radiantTubeInFceP;


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

    OnePropertyTrace getTrace(int tr) {
        if (tr < nTraces)
            return traces.get(tr);
        else
            return null;
    }

    public void switchToSelectedPage(JPanel page) {
        rth.switchToSelectedPage(page);
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
}
