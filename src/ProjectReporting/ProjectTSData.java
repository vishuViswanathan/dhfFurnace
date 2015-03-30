package ProjectReporting;

import display.ExcelStyles;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
//
//import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.ss.usermodel.Row;
//import org.apache.poi.ss.usermodel.Sheet;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 7/25/13
 * Time: 5:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProjectTSData {
    String projectID;
    String description;
    String customer;
    Vector<TSElement> entriesInTS;
    int nElements;
    double allCost;
    double allHrs;

    public ProjectTSData(String projectID, String description, String customer) {
        this.projectID = projectID;
        this.description = description;
        this.customer = customer;
        entriesInTS = new Vector<TSElement>();
        nElements = 0;
    }

    void addTSElement(int personID, double addTime, double unitCost, double totalCost) {
        entriesInTS.add(new TSElement(personID, addTime, unitCost, totalCost));
        nElements++;
        allHrs += addTime;
        allCost += totalCost;
    }

    IntPoint toXL(Sheet sheet, int row, int col, ExcelStyles styles)  {
        int leftCol = col;
        int topRow = row;
        int blockTop;
        Row r;
        Cell c;
        styles.mergeCells(sheet, row, row, leftCol, leftCol + 2, styles.csBorderedHeader2,
                "Project: " + projectID);
        row++;
        blockTop = row;
        styles.mergeCells(sheet, row, row, leftCol, leftCol + 2, styles.csNormalBold, description);
        row++;
        styles.mergeCells(sheet, row, row, leftCol, leftCol + 2, styles.csNormalBold, "for " + customer);
        styles.drawBorder(sheet, blockTop, row, leftCol, leftCol + 2, styles.borderLine);
        row++;
        col = leftCol;
        styles.setCellValue(sheet, row, col, "Time(h)", styles.csBorderedHeader2, 2000);
        col++;
        styles.setCellValue(sheet, row, col, "UnitCost", styles.csBorderedHeader2, 3000);
        col++;
        styles.setCellValue(sheet, row, col, "Cost (INR)", styles.csBorderedHeader2, 3000);
        row++;
        col = leftCol;
        styles.setCellValue(sheet, row, col, allHrs, "#,##0", styles.borderLine);
        col += 2;
        styles.setCellValue(sheet, row, col, allCost , "#,##0", styles.borderLine);
        row++;
        col = leftCol;
        IntPoint iP;
        int rtCol = leftCol;
        blockTop = row;
        for (int e = 0; e < nElements; e++) {
            iP =  entriesInTS.get(e).toXL(sheet, row, col, styles);
            rtCol = iP.col;
            row = iP.row + 1;
            col = leftCol;
        }
        styles.drawBorder(sheet, blockTop, row - 1, leftCol, rtCol, styles.borderLine);
        return new IntPoint(row, rtCol);
    }

 }
