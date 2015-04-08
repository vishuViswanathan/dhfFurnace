package performance.stripFce;

import appReporting.Reporter;
import directFiredHeating.DFHeating;
import display.ExcelStyles;
import mvUtils.display.FramedPanel;
import mvUtils.math.XYArray;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 08-Oct-14
 * Time: 3:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ZonalFuelProfile {
    protected final int USpeedCol = 0;
    protected final int UOutputCol = 1;
    protected final int TotFuelCol = 2;
    protected final int FirstZoneCol = 3;
    protected final int TotHeatCol = TotFuelCol;
    PerformanceTable performanceTable;
    DFHeating controller;

    double stripWidth;
    double stripThickness;
    protected int outputSteps;
    protected int nTopZones;
    protected int nBotZones;
    protected Double[][] topZoneFuels;
    protected Double[][] topZoneFuelHeat; // total heat comprising combustion + fuel sensible + air sensible
    protected Double[][] botZoneFuels;
    protected Double[][] botZoneFuelHeat; // total heat comprising combustion + fuel sensible + air sensible
    XYArray speedTotalFuelTop, speedTotalFuelBot;
    XYArray speedTotalFHTop, speedTotalFHBot; // based on FuelHeat (Combustion + air and fuel sensible
    Reporter fuelCharacteristicReport;
    Reporter fuelHeatCharacteristicReport;
    // columns 1 - output, 2- Total Fuel, 3, 4 ... individual zone fuels

    public ZonalFuelProfile(PerformanceTable performanceTable, double stripWidth, double stripThickness,
                            int outputSteps, DFHeating controller) throws Exception {
        this(performanceTable, outputSteps, controller);
        if (!performanceTable.isWidthInRange(stripWidth))
            throw new Exception("Strip Width is not in Range of Performance Table");
        this.stripWidth = stripWidth;
        this.stripThickness = stripThickness;
         if (!prepareFuelTable(stripWidth, stripThickness))
            throw new Exception("Facing some problem in creating Fuel Table");
    }

    protected ZonalFuelProfile(PerformanceTable performanceTable, int outputSteps, DFHeating controller) {
        this.performanceTable = performanceTable;
        this.outputSteps = outputSteps;
        this.controller = controller;
        nTopZones = performanceTable.nZones(false);
        nBotZones = performanceTable.nZones(true);
        topZoneFuels = new Double[outputSteps][nTopZones + 3];
        topZoneFuelHeat = new Double[outputSteps][nTopZones + 3];
        if (nBotZones > 0) {
            botZoneFuels = new Double[outputSteps][nBotZones + 3];
            botZoneFuelHeat = new Double[outputSteps][nBotZones + 3];
        }
    }

    protected boolean prepareFuelTable(double stripWidth, double stripThickness) {
        boolean allOk = true;
        this.stripWidth = stripWidth;
        this.stripThickness = stripThickness;
        InterpolatedParams iParam;
        double outputFactor;
        double maxOutputFactor = performanceTable.getMaxOutputFactor();
        double minOutputFactor = performanceTable.getMinOutputFactor();
        double step = (maxOutputFactor - minOutputFactor) / (outputSteps - 1);
        outputFactor = minOutputFactor;
        double thicknessFactor = stripThickness / performanceTable.baseP.chThick;
        double topTotalFuel;
        double topTotalFuelHeat;  // comprising combustion + fuel sensible + air sensible
        double botTotalFuel;
        double botTotalFuelHeat;  // comprising combustion + fuel sensible + air sensible
        double fuelFlow;
        double fuelHeat; // comprising combustion + fuel sensible + air sensible
        OneZone oneZone;
        for (int o = 0; o < outputSteps; o++) {
            iParam = new InterpolatedParams();
            if (performanceTable.getOperationData(iParam, stripWidth, outputFactor)) {
                topTotalFuel = 0;
                topTotalFuelHeat = 0;
                for (int z = 0; z < nTopZones; z++) {
                    oneZone = iParam.getOneZone(z, false);
                    fuelFlow = oneZone.fuelFlow;
                    fuelHeat = oneZone.fuelCombustionHeat + oneZone.fuelSensibleHeat + oneZone.airHeat - oneZone.heatToZoneFlue;
                    topZoneFuels[o][z + 3] = fuelFlow;
                    topZoneFuelHeat[o][z + 3] = fuelHeat;
                    topTotalFuel += fuelFlow;
                    topTotalFuelHeat += fuelHeat;
                }
                topZoneFuels[o][USpeedCol] = iParam.speed / thicknessFactor / 60;
                topZoneFuels[o][UOutputCol] = iParam.output / 1000;
                topZoneFuels[o][TotFuelCol] = topTotalFuel;
                topZoneFuelHeat[o][USpeedCol] = iParam.speed / thicknessFactor / 60;
                topZoneFuelHeat[o][UOutputCol] = iParam.output / 1000;
                topZoneFuelHeat[o][TotHeatCol] = topTotalFuelHeat;
                if (nBotZones > 0) {
                    botTotalFuel = 0;
                    botTotalFuelHeat = 0;
                    for (int z = 0; z < nBotZones; z++) {
                        oneZone = iParam.getOneZone(z, true);
                        fuelFlow = oneZone.fuelFlow;
                        fuelHeat = oneZone.fuelCombustionHeat + oneZone.fuelSensibleHeat + oneZone.airHeat - oneZone.heatToZoneFlue;
                        botZoneFuels[o][z + FirstZoneCol] = fuelFlow;
                        topZoneFuelHeat[o][z + FirstZoneCol] = fuelHeat;
                        botTotalFuel += fuelFlow;
                        botTotalFuelHeat += fuelHeat;
                    }
                    botZoneFuels[o][USpeedCol] = iParam.speed / thicknessFactor / 60;
                    botZoneFuels[o][UOutputCol] = iParam.output / 1000;
                    botZoneFuels[o][TotFuelCol] =botTotalFuel;
                    botZoneFuelHeat[o][USpeedCol] = iParam.speed / thicknessFactor / 60;
                    botZoneFuelHeat[o][UOutputCol] = iParam.output / 1000;
                    botZoneFuelHeat[o][TotHeatCol] = botTotalFuelHeat;
                }
                outputFactor += step;
            }
            else
                allOk = false;
        }
        if (allOk) {
            speedFuelArray(false);
            speedFHArray(false);
//            speedZonelFuelTop = new XYArray[nTopZones];
//            for (int z = 0; z < nTopZones; z++)
//                speedZonelFuelTop[z] = speedFuelArray(z, false);
            if (nBotZones > 0) {
                speedFuelArray(true);
                speedFHArray(false);
//                speedZonelFuelBot = new XYArray[nBotZones];
//                for (int z = 0; z < nBotZones; z++)
//                    speedZonelFuelBot[z] = speedFuelArray(z, true);
            }
        }
        return allOk;
    }

    public double[][] oneZoneFuelArray(int zNum, boolean bBot) {
        double[][] zoneFuel = new double[outputSteps][(bBot) ? nBotZones : nTopZones];
        if (zNum >=0 && zNum < ((bBot) ? nBotZones : nTopZones)) {
            Double[][] allZoneFuels = (bBot) ? botZoneFuels : topZoneFuels;
            for (int r = 0; r < outputSteps; r++) {
                zoneFuel[r][0] = allZoneFuels[r][TotFuelCol];
                zoneFuel[r][1] = allZoneFuels[r][zNum + FirstZoneCol];
            }
        }
        return zoneFuel;
    }

    public double[][] oneZoneFuelHeatArray(int zNum, boolean bBot) {
        double[][] zoneFuelHeat = new double[outputSteps][(bBot) ? nBotZones : nTopZones];
        if (zNum >=0 && zNum < ((bBot) ? nBotZones : nTopZones)) {
            Double[][] allZoneFuelHeats = (bBot) ? botZoneFuelHeat : topZoneFuelHeat;
            for (int r = 0; r < outputSteps; r++) {
                zoneFuelHeat[r][0] = allZoneFuelHeats[r][TotHeatCol];
                zoneFuelHeat[r][1] = allZoneFuelHeats[r][zNum + FirstZoneCol];
            }
        }
        return zoneFuelHeat;
    }

    /*
    XY array with speed and fuel flow for the particular zone
    */
    XYArray speedFuelArray(int zNum, boolean bBot) {
        XYArray arr = null;
        int nZones;
        Double[][] table;
        if (bBot) {
            nZones = nBotZones;
            table = botZoneFuels;
        }
        else {
            nZones = nTopZones;
            table = topZoneFuels;
        }
        if (zNum >= 0 && zNum < nZones) {
            arr = new XYArray(table, 0, zNum + 3);
        }
        return arr;
    }

    /*
     based on total fuel
    */
    XYArray speedFuelArray(boolean bBot) {
        XYArray arr;
        Double[][] table;
        if (bBot) {
            arr = speedTotalFuelBot;
            table = botZoneFuels;
        }
        else {
            arr = speedTotalFuelTop;
            table = topZoneFuels;
        }
        if (arr == null)
            arr = new XYArray(table, 0, 2);
        else
            arr.setValues(table, 0, 2);
        return arr;
    }

    XYArray speedFuelArrayOLD(boolean bBot) {
        XYArray arr = null;
        Double[][] table;
        if (bBot) {
            table = botZoneFuels;
        }
        else {
            table = topZoneFuels;
        }
        arr = new XYArray(table, 0, 2);
        return arr;
    }

    /*
     based on total FuelHeat
    */
    XYArray speedFHArray(boolean bBot) {
        XYArray arr;
        Double[][] table;
        if (bBot) {
            arr = speedTotalFHBot;
            table = botZoneFuelHeat;
        }
        else {
            arr = speedTotalFHTop;
            table = topZoneFuelHeat;
        }
        arr = new XYArray(table, 0, 2);
        return arr;
    }

    XYArray speedFHArrayOLD(boolean bBot) {
         XYArray arr = null;
         Double[][] table;
         if (bBot) {
             table = botZoneFuelHeat;
         }
         else {
             table = topZoneFuelHeat;
         }
         arr = new XYArray(table, 0, 2);
         return arr;
     }

    public double recommendedSpeed(double totFuel, boolean bBot) {
        if (bBot)
            return speedTotalFuelBot.getXat(totFuel);
        else
            return speedTotalFuelTop.getXat(totFuel);
    }

    public double recommendedSpeedOnFH(double totalFH, boolean bBot) {
        if (bBot)
            return speedTotalFHBot.getXat(totalFH);
        else
            return speedTotalFHTop.getXat(totalFH);
    }
/*

    public double recommendedSpeed(double fuelFlow, int zNum, boolean bBot) {
        int nZones;
        XYArray[] arr;
        if (bBot) {
            nZones = nBotZones;
            arr = speedZonelFuelBot;
        }
        else {
            nZones = nTopZones;
            arr = speedZonelFuelTop;
        }
        if (zNum >= 0 && zNum < nZones)
            return arr[zNum].getXat(fuelFlow);
        else
            return -1;
    }
*/

    /**
     *
     * @param fuelFlows  is an array with zonal flows
     * @return  DoubleRange with max and min values
     */

 /*
    public DoubleRange recommendedSpeedRange(double[] fuelFlows, boolean bBot) {
        DoubleRange range = new DoubleRange();
        int nZones;
         if (bBot) {
             nZones = nBotZones;
         }
         else {
             nZones = nTopZones;
         }
        double totFuel;
        int len = fuelFlows.length;
        if (len == nZones) {
            totFuel = 0;
//            for (int z = 0; z < len; z++) {
            controller.showMessage("Neglecting zones 1 ");
            for (int z = 0; z < len; z++) {
                range.takeVal(recommendedSpeed(fuelFlows[z], z, bBot));
                totFuel += fuelFlows[z];
            }
            range.takeVal(recommendedSpeed(totFuel, bBot));  // check for total flow
        }
        return range;
    }
*/
    JPanel fuelTrendP;

    public JPanel fuelFlowCharacteristic(boolean bBot) {
        JPanel outerP = new FramedPanel(new BorderLayout());
        outerP.add(new JLabel(headerName(bBot)), BorderLayout.NORTH);

        Reporter report = fuelFlowCharacteristicReport(bBot);
        Reporter fhReport = fuelHeatCharacteristicReport(bBot);
        JTabbedPane tP = new JTabbedPane();

//        outerP.add(new JLabel(report.getHeader()), BorderLayout.NORTH);
        JTable table = report.getJTable();
        JScrollPane sP = new JScrollPane(table);
        sP.setBackground(SystemColor.lightGray);
        sP.setPreferredSize(new Dimension(table.getPreferredSize().width, 100));
//        JPanel innerP = new JPanel();
//        innerP.add(sP);
        tP.addTab("Fuel Flow", sP);
        table = fhReport.getJTable();
        sP = new JScrollPane(table);
        sP.setBackground(SystemColor.lightGray);
        sP.setPreferredSize(new Dimension(table.getPreferredSize().width, 100));
//        innerP.add(sP);
        tP.addTab("Fuel Heat", sP);

        outerP.add(tP, BorderLayout.CENTER);
        outerP.add(buttonPanel(), BorderLayout.SOUTH);
        fuelCharacteristicReport = report;
        fuelHeatCharacteristicReport = fhReport;
        fuelTrendP = outerP;
        return outerP;
    }

    JPanel buttonPanel() {
        JPanel jp = new JPanel();
        JButton xlButton = new JButton("Save to XL file");
        xlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveFuelTrendToXL();
            }
        });
        jp.add(xlButton);
        xlButton = new JButton("Append Fuel Trend File XL file");
        xlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                appendFuelTrendToXL();
            }
        });
        jp.add(xlButton);
        return jp;
    }

    void addReportColumns(Reporter report, boolean bBot) {
        report.addColumn(Reporter.ColType.NUMBER, 50, 1500, "#,##0.0", "Speed m/m");
        report.addColumn(Reporter.ColType.NUMBER, 50, 1500, "#,##0.0", "Output t/h");
        report.addColumn(Reporter.ColType.NUMBER, 50, 1500, "#,##0.0", "TotFuel");
//        report.addColumn(Reporter.ColType.NUMBER, 50, 1500, "#.00.0e#", "Fuel+APH heat");
        int nZones = (bBot) ? nBotZones : nTopZones;
        for (int z =0; z < nZones; z++)
            report.addColumn(Reporter.ColType.NUMBER, 50, 1500, "#,##0.0", "Zone#" + ("" + (z + 1)).trim());
    }

    void addFHReportColumns(Reporter report, boolean bBot) {
        report.addColumn(Reporter.ColType.NUMBER, 50, 1500, "#,##0.0", "Speed m/m");
        report.addColumn(Reporter.ColType.NUMBER, 50, 1500, "#,##0.0", "Output t/h");
        report.addColumn(Reporter.ColType.NUMBER, 50, 1500, "0.00E0", "TotHeat");
//        report.addColumn(Reporter.ColType.NUMBER, 50, 1500, "#.00.0e#", "Fuel+APH heat");
        int nZones = (bBot) ? nBotZones : nTopZones;
        for (int z =0; z < nZones; z++)
            report.addColumn(Reporter.ColType.NUMBER, 50, 1500, "0.00E0", "Zone#" + ("" + (z + 1)).trim());
    }

    void addReportData(Reporter report, boolean bBot) {
//        Double[][] dataSrc = (bBot) ? botZoneFuels : topZoneFuels;
        Double[][] dataSrc = (bBot) ? botZoneFuels : topZoneFuels;
        for (int o = 0; o < outputSteps; o++)
            report.addResultLine(dataSrc[o]);
    }

    void addFHReportData(Reporter report, boolean bBot) {
        Double[][] dataSrc = (bBot) ? botZoneFuelHeat : topZoneFuelHeat;
        for (int o = 0; o < outputSteps; o++)
            report.addResultLine(dataSrc[o]);
    }

    String headerName(boolean bBot) {
        return "For " + performanceTable.furnace.topBotName(bBot) + "zones - " +
                "Strip size " + (stripWidth * 1000) + " x " + stripThickness * 1000;
    }

    public Reporter fuelFlowCharacteristicReport(boolean bBot) {
        Reporter report = new Reporter("Fuel Flow Characteristic for " +
                performanceTable.furnace.topBotName(bBot) + "zones - " +
               "Strip size " + (stripWidth * 1000) + " x " + stripThickness * 1000 );
        addReportColumns(report, bBot);
        addReportData(report, bBot);
        return report;
    }

    public Reporter fuelHeatCharacteristicReport(boolean bBot) {
        Reporter report = new Reporter("Fuel Heat Characteristic for " +
                performanceTable.furnace.topBotName(bBot) + "zones - " +
               "Strip size " + (stripWidth * 1000) + " x " + stripThickness * 1000 );
        addFHReportColumns(report, bBot);
        addFHReportData(report, bBot);
        return report;
    }

    boolean xlFuelCharacteristicReport(Sheet sheet, ExcelStyles styles) {
        Cell c, rowNumCell;
        Row r;
        int slNo;
        // row 0 is for ID and used row slNo.
        r = sheet.getRow(1);
        rowNumCell = r.getCell(2);
        int topRow = (new Double(rowNumCell.getNumericCellValue())).intValue();
        int leftCol = 0;
        if (topRow <= 0)
            topRow = 4;
        topRow = oneXlReport(fuelCharacteristicReport, sheet, styles, leftCol, topRow);
        leftCol = 0;
        topRow++;
        topRow = oneXlReport(fuelHeatCharacteristicReport, sheet, styles, leftCol, topRow);
        topRow++;
        rowNumCell.setCellValue(topRow);
        return true;
    }

    int oneXlReport(Reporter report, Sheet sheet, ExcelStyles styles, int leftCol, int topRow) {
        int col;
        styles.setCellValue(sheet, topRow, 0, report.getHeader());
        topRow++;
        col = leftCol + 2; // leave a column for the SlNo
        topRow = report.xlReportColHead(sheet, styles,  topRow, col);
        col = leftCol + 1;
        int slNo = 1;
        topRow = report.xlReportLines(sheet, styles, topRow, col, slNo);
        topRow++; // leave a gap
        return topRow;
    }

    void saveFuelTrendToXL() {
        FileOutputStream out = null;
        FileDialog fileDlg =
                new FileDialog(DFHeating.mainF, "Saving Fuel Trend Table to Excel",
                        FileDialog.SAVE);
        fileDlg.setFile("FuelTrend.xls");
        fileDlg.setVisible(true);
        String bareFile = fileDlg.getFile();
        if (bareFile != null) {
            int len = bareFile.length();
            if ((len < 4) || !(bareFile.substring(len - 4).equalsIgnoreCase(".xls"))) {
                controller.showMessage("Adding '.xls' to file name");
                bareFile = bareFile + ".xls";
            }
            String fileName = fileDlg.getDirectory() + bareFile;
            try {
                out = new FileOutputStream(fileName);
            } catch (FileNotFoundException e) {
                controller.showError("Some problem in file.\n" + e.getMessage());
                return;
            }
//  create a new workbook
            Workbook wb = new HSSFWorkbook();
            int nSheet = 0;
//  create a new sheet
            ExcelStyles styles = new ExcelStyles(wb);
            Sheet sh = prepareReportWB(wb, styles);
            xlFuelCharacteristicReport(sh, styles);
            try {
                wb.write(out);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                controller.showError("Some problem with file.\n" + e.getMessage());
            }
        }
        controller.parent().toFront();
    }

    Sheet prepareReportWB(Workbook wb, ExcelStyles styles) {
         Sheet sheet = wb.createSheet("Fuel Trend");
         Row r = sheet.createRow(0);
         Cell c = r.createCell(0);
         c.setCellStyle(styles.csHeader1);
         c.setCellValue("Fuel Trend table from DFHFurnace");
         r = sheet.createRow(1);
         c = r.createCell(0);
         c.setCellValue("Last Entry Row");
         c = r.createCell(2);
         c.setCellValue(0);
         return sheet;
     }

    void appendFuelTrendToXL() {
        FileOutputStream out = null;
        FileDialog fileDlg =
                new FileDialog(controller.mainF, "Appending Fuel Trend Table to Excel",
                        FileDialog.LOAD);
        fileDlg.setFile("*.xls");
        fileDlg.setVisible(true);
        String bareFile = fileDlg.getFile();
        if (bareFile != null) {
            boolean bXL = (bareFile.length() > 4) && (bareFile.substring(bareFile.length() - 4).equalsIgnoreCase(".xls"));
            if (bXL) {
                String filePath = fileDlg.getDirectory() + bareFile;
                if (!filePath.equals("nullnull")) {
                    FileInputStream xlInput = null;
                    HSSFWorkbook wB = null;
                    try {
                        xlInput = new FileInputStream(filePath);
                        /** Create a workbook using the File System**/
                        wB = new HSSFWorkbook(xlInput);
                    } catch (Exception e) {
                        controller.showError("Some problem in Reading/saving Fuel Trend file\n" + e.getMessage());
                    }

                    /** Get the first sheet from workbook**/
                    HSSFSheet sh = wB.getSheet("Fuel Trend");
                    if (sh != null) {
                        ExcelStyles styles = new ExcelStyles(wB);
                        try {
                            xlFuelCharacteristicReport(sh, styles);
                            xlInput.close();
                            FileOutputStream outFile = new FileOutputStream(filePath);
                            wB.write(outFile);
                            outFile.close();
                        } catch (FileNotFoundException e) {
                            controller.showError("Some problem in Reading/saving Fuel Trend file\n" + e.getMessage());
                        } catch (IOException e) {
                            controller.showError("Some problem in Reading/saving Fuel Trend file\n" + e.getMessage());
                        }
                    }
                }
            } else
                controller.showMessage("Choose *.xls file");
        }
    }

}


