package ProjectReporting;

import mvUtils.display.ExcelStyles;
import org.apache.poi.ss.usermodel.*;
//
//import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.ss.usermodel.Row;
//import org.apache.poi.ss.usermodel.Sheet;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 7/25/13
 * Time: 4:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class TSElement {
    int personID;
    double addTime, unitCost, totalCost;

    public TSElement(int personID, double addTime, double unitCost, double totalCost) {
        this.personID = personID;
        this.addTime = addTime;
        this.unitCost = unitCost;
        this.totalCost = totalCost;
    }

    IntPoint toXL(Sheet sheet, int row, int col, ExcelStyles styles) {
        styles.setCellValue(sheet, row, col, addTime, "#,##0");
        col++;
        styles.setCellValue(sheet, row, col, unitCost, "#,##0");
        col++;
        styles.setCellValue(sheet, row, col, totalCost, "#,##0");
        return new IntPoint(row, col);
    }
}
