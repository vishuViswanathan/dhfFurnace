package ProjectReporting;

import display.ExcelStyles;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.hssf.usermodel.HSSFWorkbook;
//import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.ss.usermodel.Row;
//import org.apache.poi.ss.usermodel.Sheet;
//import org.apache.poi.ss.usermodel.Workbook;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 7/25/13
 * Time: 12:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class SaveToFile extends JApplet {
    String version = "SaveToFile 2013/07/27 16:33";
    boolean onTest = false;
    JSObject win;
    String saveFor = "";
    static public JFrame mainF;

    Vector <PersonData> vPersonData;
    LinkedHashMap<String, ProjectTSData> vProjTSData;

    int year, month;
    double grandTotHrs, grandTotCost;
    public SaveToFile() {
        debug(version);
    }


    public void init() {
         String strTest = this.getParameter("OnTest");
         if (strTest != null)
             onTest = strTest.equalsIgnoreCase("YES");
         if (onTest) {
         } else {
             try {
                 debug("in init");
                 win = JSObject.getWindow(this);
                 saveFor = (String)win.eval("getHeader()");
              } catch (JSException e) {
                 e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                 win = null;
             }
         }
         displayIt();
    }

    boolean itsON = false;

    public void displayIt() {
        if (!itsON) {
            itsON = true;
            JButton butt = new JButton("Save to Excel file");
            butt.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    saveToFileLocal();
                }
            });
            add(butt);
        }
    }

    String setTestData() {
        setYM("2020", "13", "500", "5000");
        addPersonData("10001", "Person One", "Deptt One", "100", " 1000");
        addPersonData("10002", "Person Two", "Deptt Two", "200", " 2000");
        addPersonData("10003", "Person Three", "Deptt Two", "200", " 2000");
        addPersonData("10003", "Person Four ", "Deptt Two", "200", " 2000");
        addProjectData("TH-9001", "Test Project 1", " Customer 1");
        addProjectData("TH-9002", "Test Project 2", " Customer 2");
        addTSElement("TH-9001", "10001", "0", "10", "1000");
        addTSElement("TH-9001", "10002", "0", "20", "2000");
        addTSElement("TH-9001", "10003", "0", "20", "2000");
        addTSElement("TH-9002", "10001", "0", "30", "3000");
        return "OK";
    }

    public String setYM(String yearStr, String monthStr, String grandTotHrsStr, String grandTotCostStr) {
        String retVal = "OK";
        try {
            year = Integer.valueOf(yearStr);
            month = Integer.valueOf(monthStr);
            grandTotHrs = Double.valueOf(grandTotHrsStr);
            grandTotCost = Double.valueOf(grandTotCostStr);
        } catch (NumberFormatException e) {
            retVal = "ERROR:In Number Conversion Year = " + yearStr + " Month = " + monthStr +
                    ", grandTotHrs = " + grandTotHrsStr + ", grandTotCost = " + grandTotCostStr;
        }
        return retVal;
    }

    public String addPersonData(String personIDStr, String fullName, String depttName, String totalTimeStr,
                                    String totalCostStr) {
        String retVal = "OK";
        int personID;
        double totalTime, totalCost;
        try {
            personID = Integer.valueOf(personIDStr);
            totalTime = Double.valueOf(totalTimeStr);
            totalCost = Double.valueOf(totalCostStr);
            vPersonData.add(new PersonData(personID, fullName, depttName, totalTime, totalCost));
        } catch (NumberFormatException e) {
            retVal = "ERROR: In number conversion, personID = " + personIDStr + ", totalTime = " + totalTimeStr
                            + ", totalCostStr = " + totalCostStr;
        }
        return retVal;
    }

    public String addProjectData(String projectID, String description, String customer) {
        String retVal = "OK";
        projectID = projectID.trim();
        vProjTSData.put(projectID, new ProjectTSData(projectID, description, customer));
        return retVal;
    }

    public String addTSElement(String projectID, String personIDStr, String unitCostStr, String addTimeStr, String totalCostStr) {
        String retVal = "OK";
        int personID = 0 ;
        double unitCost= 0, addTime = 0, totalCost = 0;
        try {
            personID = Integer.valueOf(personIDStr);
            unitCost = Double.valueOf(unitCostStr);
            addTime = Double.valueOf(addTimeStr);
            totalCost = Double.valueOf(totalCostStr);
        } catch (Exception e) {
            retVal = "ERROR:" + e.getMessage();
            //number conversion, personID = " + personIDStr + ", unitCost = " + unitCostStr
            //                            + ", addTime = " + addTimeStr + ", totalCost = " + totalCostStr;
        }
        String prID = projectID.trim();
debug("prID = " + prID);
        ProjectTSData oneProject = vProjTSData.get(prID);
debug("oneProject = " + oneProject);
        if (oneProject != null)
            oneProject.addTSElement(personID, addTime, unitCost, totalCost);
        else
            retVal = "ERROR:Project " + projectID + " not inited";
        return retVal;
    }

    IntPoint personsToXL(Sheet sheet, int row, int col, ExcelStyles styles) {
        int topRow = row;
        int leftCol = col;
        int rightCol = col;
        styles.mergeCells(sheet, row, row, col, col + 4, styles.csBorderedHeader2, "Personnel List");
        row++;
        styles.drawBorder(sheet, row, row + 1, leftCol, leftCol + 4, styles.borderLine);
        row += 2;   // to allow for ProjID, Description and Customer for the ProjectTS layout
        styles.setCellValue(sheet, row, col, "No.", styles.csBorderedHeader2, 1000);
        col++;
        styles.setCellValue(sheet, row, col, "FullName", styles.csBorderedHeader2, 6000);
        col++;
        styles.setCellValue(sheet, row, col, "Department", styles.csBorderedHeader2, 5000);
        col++;
        styles.setCellValue(sheet, row, col, "Time(h)", styles.csBorderedHeader2, 2000);
        col++;
        styles.setCellValue(sheet, row, col, "Cost(INR)", styles.csBorderedHeader2, 3000);
        rightCol = col;
        row++;
        col = leftCol + 3;
        styles.setCellValue(sheet, row, col, grandTotHrs, "#,##0", styles.borderLine);
        col++;
        styles.setCellValue(sheet, row, col, grandTotCost, "#,##0", styles.borderLine);
        row++;
        int blockTop = row;
        for (int p = 0; p < vPersonData.size(); p++) {
            col = leftCol;
            styles.setCellValue(sheet, row, col, (double)(p + 1));
            col++;
            vPersonData.get(p).toXL(sheet, row, col, styles);
            row++;
        }
        styles.drawBorder(sheet, blockTop, row -1, leftCol, leftCol + 4, styles.borderLine);
        return new IntPoint(row, rightCol);

    }


    String saveToFileLocal() {
        String dataStatus;
        vPersonData = new Vector<PersonData>();
        vProjTSData = new LinkedHashMap<String, ProjectTSData>();
        if (onTest)
            dataStatus = setTestData();
        else
            dataStatus = (String) win.eval("getTSData()");
        if (dataStatus.equalsIgnoreCase("OK")) {
// create a new file
            FileOutputStream out = null;
            FileDialog fileDlg =
                    new FileDialog(mainF, saveFor + " to Excel",
                            FileDialog.SAVE);
            fileDlg.setFile("Time Sheet from applet.xls");
            fileDlg.setVisible(true);
            String bareFile = fileDlg.getFile();
            if (bareFile != null) {
                int len = bareFile.length();
                if ((len < 4) || !(bareFile.substring(len - 4).equalsIgnoreCase(".xls"))) {
                    showMessage("Adding '.xls' to file name");
                    bareFile = bareFile + ".xls";
                }
                String fileName = fileDlg.getDirectory() + bareFile;
                debug("got File name " + fileName);
                try {
                    debug("Opening file " + fileName);
                    out = new FileOutputStream(fileName);
                    debug("File opened " + fileName);
                } catch (Exception e) {
                    debug("Some problem in file.\n" + e.getMessage());
//                showError("Some problem in file.\n" + e.getMessage());
                    return "ERROR:" + e.getMessage();
                }
// create a new workbook
                Workbook wb = new HSSFWorkbook();
                int nSheet = 0;
// create a new sheet
                ExcelStyles styles = new ExcelStyles(wb);
                wb.createSheet("Time Sheet");
                debug("sheet created");
                int row = 1, col = 1;
                Sheet sh = wb.getSheetAt(nSheet);
                styles.setCellValue(sh, row, col, "Time Sheet For Projects", styles.csHeader1);
                row++;
                styles.setCellValue(sh, row, col, "For Month " + month + " of Year " + year, styles.csHeader1);
                row++;
                debug("Filled Header " + fileName);
                int topRow = row;
                IntPoint lastRP = personsToXL(sh, row, col, styles);
                debug("got personsToXL " + lastRP.row + ", " + lastRP.col);
                for (ProjectTSData p : vProjTSData.values())  {
                    row = topRow;
                    col = lastRP.col + 1;
                    lastRP = p.toXL(sh, row, col, styles);
                }
                try {
                    wb.write(out);
                    debug("file written to ");
                    out.close();
                } catch (IOException e) {
                    debug("in Error " + e);
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                showError("Some problem with file.\n" + e.getMessage());
                }

            }
//        parent().toFront();
            return "Ok";
        }
        else
            return "ERROR";
    }

    //region Message functions
    boolean decide(String title, String msg) {
        int resp = JOptionPane.showConfirmDialog(parent(), msg, title, JOptionPane.YES_NO_OPTION);
        if (resp == JOptionPane.YES_OPTION)
            return true;
        else
            return false;
    }

    void showError(String msg) {
        JOptionPane.showMessageDialog(parent(), msg, "ERROR", JOptionPane.ERROR_MESSAGE);
//        parent().toFront();
    }

    void showMessage(String msg) {
        JOptionPane.showMessageDialog(parent(), msg, "FOR INFORMATION", JOptionPane.INFORMATION_MESSAGE);
//        parent().toFront();
    }
    //endregion

    public Frame parent() {
        return mainF;
    }

    void debug(String msg) {
        System.out.println("SaveToFile " + msg);
    }
}
