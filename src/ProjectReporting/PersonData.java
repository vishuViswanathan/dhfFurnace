package ProjectReporting;

import display.ExcelStyles;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;

//import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.ss.usermodel.Row;
//import org.apache.poi.ss.usermodel.Sheet;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 7/26/13
 * Time: 11:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class PersonData {
    int personID;
    String fullName;
    String depttName;
    double totalTime;
    double totalCost;

    public PersonData(int personID, String fullName, String depttName) {
        this.personID = personID;
        this.fullName = fullName;
        this.depttName = depttName;
    }

    public PersonData(int personID, String fullName, String depttName, double totalTime, double totalCost) {
        this.personID = personID;
        this.fullName = fullName;
        this.depttName = depttName;
        this.totalTime = totalTime;
        this.totalCost = totalCost;
    }

    public void setTimeAndCost(double totalTime, double totalCost) {
        this.totalTime = totalTime;
        this.totalCost = totalCost;
    }

    IntPoint toXL(Sheet sheet, int row, int col, ExcelStyles styles) {
        int topRow = row;
        int leftCol = col;
        styles.setCellValue(sheet, row, col,fullName);
        col++;
        styles.setCellValue(sheet, row, col,depttName);
        col++;
        styles.setCellValue(sheet, row, col,totalTime);
        col++;
        styles.setCellValue(sheet, row, col,totalCost, "#,###");
        return new IntPoint(row, col);
    }

}
