package display;

import mvUtils.display.ComponentPair;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.ValueForExcel;
import mvUtils.display.XLcellData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;

import javax.swing.*;
import java.awt.*;
import java.util.Date;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 12/4/12
 * Time: 2:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExcelStyles {
    Workbook wb;
    DataFormat dataFormat;
    public CellStyle csHeader1, csHeader2, csNormal, csNormalBold, csTextWrap, csTextWrapBold;
    public CellStyle csBorderedHeader2;
    public CellStyle csDescript, csNumData1, csNumData2, csNumData;
    public short borderLine1, borderLine;
    org.apache.poi.ss.usermodel.Font fontNormal, fontBold10, fontBold11, fontBold12;

    public ExcelStyles(Workbook wb) {
        this.wb = wb;
        dataFormat = wb.createDataFormat();
        borderLine1 = csNormal.BORDER_THICK;
        borderLine = csNormal.BORDER_THIN;
        createFonts();
        createSplStyles();
    }

    public void setCellValue(Sheet sheet, int row, int col, String value, CellStyle style, int width, short border) {
        setCellValue(sheet, row, col, value, style, width);
        setAllBorder(sheet, row, col, border);
     }

    public void setCellValue(Sheet sheet, int row, int col, Double value, CellStyle style, int width, short border) {
        setCellValue(sheet, row, col, value, style, width);
        setAllBorder(sheet, row, col, border);    }

    public void setCellValue(Sheet sheet, int row, int col, Date value, CellStyle style, int width, short border) {
        setCellValue(sheet, row, col, value, style, width);
        setAllBorder(sheet, row, col, border);    }

    public void setCellValue(Sheet sheet, int row, int col, String value, CellStyle style, int width) {
        setCellValue(sheet, row, col, value, style);
        sheet.setColumnWidth(col, width);
    }

    public void setCellValue(Sheet sheet, int row, int col, Double value, CellStyle style, int width) {
        setCellValue(sheet, row, col, value, style);
        sheet.setColumnWidth(col, width);
    }

    public void setCellValue(Sheet sheet, int row, int col, Date value, CellStyle style, int width) {
        setCellValue(sheet, row, col, value, style);
        sheet.setColumnWidth(col, width);
    }

    public void setCellValue(Sheet sheet, int row, int col, String value, CellStyle style) {
        Row r = createRow(sheet, row);
        Cell c = r.createCell(col);
        c.setCellStyle(style);
        c.setCellValue(value);
    }

    public void setCellValue(Sheet sheet, int row, int col, Double value, CellStyle style) {
        Row r = createRow(sheet, row);
        Cell c = r.createCell(col);
        c.setCellStyle(style);
        c.setCellValue(value);
    }

    public void setCellValue(Sheet sheet, int row, int col, Double value, String fmtStr, CellStyle style) {
        Row r = createRow(sheet, row);
        Cell c = r.createCell(col);
        c.setCellStyle(style);
        setNumberWithFormat(c, value, fmtStr);
    }
    public void setCellValue(Sheet sheet, int row, int col, Date value, CellStyle style) {
        Row r = createRow(sheet, row);
        Cell c = r.createCell(col);
        c.setCellStyle(style);
        c.setCellValue(value);
    }

    public void setCellValue(Sheet sheet, int row, int col, String value, short border) {
        setCellValue(sheet, row, col, value);
        setAllBorder(sheet, row, col, border);
    }

    public void setCellValue(Sheet sheet, int row, int col, Double value, short border) {
        setCellValue(sheet, row, col, value);
        setAllBorder(sheet, row, col, border);
    }

    public void setCellValue(Sheet sheet, int row, int col, Double value, String fmtStr, short border) {
        setCellValue(sheet, row, col, value, fmtStr);
        setAllBorder(sheet, row, col, border);
    }

    public void setCellValue(Sheet sheet, int row, int col, Date value, short border) {
        setCellValue(sheet, row, col, value);
        setAllBorder(sheet, row, col, border);
    }

    void setDefaultCellStyle(Cell c) {
        CellStyle cs = wb.createCellStyle();
        cs.cloneStyleFrom(csNormal);
        c.setCellStyle(cs);
    }

    public void setCellValue(Sheet sheet, int row, int col, String value) {
        Row r = createRow(sheet, row);
        Cell c = r.createCell(col);
        setDefaultCellStyle(c);
        c.setCellValue(value);
    }

    public void setCellValue(Sheet sheet, int row, int col, Double value) {
        Row r = createRow(sheet, row);
        Cell c = r.createCell(col);
        setDefaultCellStyle(c);
        c.setCellValue(value);
    }

    public void setCellValue(Sheet sheet, int row, int col, Double value, String fmtStr) {
        Row r = createRow(sheet, row);
        Cell c = r.createCell(col);
        setDefaultCellStyle(c);
        setNumberWithFormat(c, value, fmtStr);
    }

    public void setCellValue(Sheet sheet, int row, int col, Date value) {
        Row r = createRow(sheet, row);
        Cell c = r.createCell(col);
        setDefaultCellStyle(c);
        c.setCellValue(value);
    }

    public Row createRow(Sheet sheet, int row) {
        Row r = sheet.getRow(row);
        if (r == null)
            r = sheet.createRow(row);
        return r;
    }
    Cell createCell(Row row, short column, short halign, CellStyle cs) {
        Cell cell = row.createCell(column);
        cs.setAlignment(halign);
        cell.setCellStyle(cs);
        return cell;
    }

    void createFonts() {
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setColor((short) Font.COLOR_NORMAL);
        f.setBoldweight(Font.BOLDWEIGHT_NORMAL);
        fontNormal = f;

        f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setColor((short) Font.COLOR_NORMAL);
        f.setBoldweight(Font.BOLDWEIGHT_BOLD);
        fontBold10 = f;

        f = wb.createFont();
        f.setFontHeightInPoints((short) 11);
        f.setColor((short) Font.COLOR_NORMAL);
        f.setBoldweight(Font.BOLDWEIGHT_BOLD);
        fontBold11 = f;

        f = wb.createFont();
        f.setFontHeightInPoints((short) 12);
        f.setColor((short) Font.COLOR_NORMAL);
        f.setBoldweight(Font.BOLDWEIGHT_BOLD);
        fontBold12 = f;
    }

    void createSplStyles() {
        CellStyle cs = wb.createCellStyle();
        cs.setFont(fontNormal);
        csNormal = cs;

        cs = wb.createCellStyle();
        cs.setFont(fontBold10);
        csNormalBold = cs;

        cs = wb.createCellStyle();
        cs.setFont(fontBold12);
        csHeader1 = cs;

        cs = wb.createCellStyle();
        cs.setFont(fontBold11);
        cs.setAlignment(CellStyle.ALIGN_CENTER);
        csHeader2 = cs;

        cs = wb.createCellStyle();
        cs.setFont(fontBold11);
        cs.setAlignment(CellStyle.ALIGN_CENTER);
        setAllBorder(cs, borderLine);
        csBorderedHeader2 = cs;

        cs = wb.createCellStyle();
        cs.setWrapText(true);
        csTextWrap = cs;

        cs = wb.createCellStyle();
        cs.setWrapText(true);
        cs.setFont(fontBold10);
        csTextWrapBold = cs;

    }

    void setAllBorder(CellStyle cs,  short border) {
        cs.setBorderTop(border);
        cs.setBorderRight(border);
        cs.setBorderBottom(border);
        cs.setBorderLeft(border);
    }

    void setAllBorder(Sheet sheet, int row, int col, short border) {
        CellStyle cs = wb.createCellStyle();
        Cell cell = sheet.getRow(row).getCell(col);
        cs.cloneStyleFrom(cell.getCellStyle());
        setAllBorder(cs, border);
        cell.setCellStyle(cs);
    }

    public void mergeCells(Sheet sh, int topRow, int bottomRow, int leftCol, int rightCol, CellStyle cs, String val) {
        Row row;
        Cell cell;
        for (int r = topRow; r <= bottomRow;  r++) {
            row = sh.getRow(r);
            if (row == null)
                row = sh.createRow(r);
            for (int c = leftCol; c <= rightCol; c++) {
                cell = row.getCell(c);
                if (cell == null)
                    cell = row.createCell(c);
                if (r == topRow && c == leftCol)
                    cell.setCellValue(val);
                cell.setCellStyle(cs);
            }
        }
        sh.addMergedRegion(new CellRangeAddress(topRow, bottomRow, leftCol, rightCol));
    }

    public void drawBorder(Sheet sh, int row, int col, short border) {
        drawBorder(sh, row,  row, col, col,  border);
    }


    public void drawBorder(Sheet sh, int topRow, int bottomRow, int leftCol, int rightCol, short border) {
        Row row;
        Cell cell;
        CellStyle cs, newcs;
        for (int r = topRow; r <= bottomRow;  r++) {
            row = sh.getRow(r);
            if (row == null)
                row = sh.createRow(r);
            if (r == topRow) {
                for (int c = leftCol; c <= rightCol; c++) {
                    cell = row.getCell(c);
                    if (cell == null)   {
                        cell = row.createCell(c);
                        setDefaultCellStyle(cell);
                    }

                    newcs = wb.createCellStyle();
                    newcs.cloneStyleFrom(cell.getCellStyle());
                    newcs.setBorderTop(border);

//                    cs = cell.getCellStyle();
//                    cs.setBorderTop(border);
                    if (c == leftCol)
                        newcs.setBorderLeft(border);
                    if (c == rightCol)
                        newcs.setBorderRight(border);
                    cell.setCellStyle(newcs);
                }
            }
            if (r == bottomRow) {
                for (int c = leftCol; c <= rightCol; c++) {
                    cell = row.getCell(c);
                    if (cell == null) {
                        cell = row.createCell(c);
                        setDefaultCellStyle(cell);
                    }
                    newcs = wb.createCellStyle();
                    newcs.cloneStyleFrom(cell.getCellStyle());
                    newcs.setBorderBottom(border);
                    if (c == leftCol)
                        newcs.setBorderLeft(border);
                    if (c == rightCol)
                        newcs.setBorderRight(border);
                    cell.setCellStyle(newcs);
//                    cell = row.getCell(c);
//                    if (cell == null)
//                        cell = row.createCell(c);
//                    cs = cell.getCellStyle();
//                    cs.setBorderBottom(border);
//                    if (c == leftCol)
//                        cs.setBorderLeft(border);
//                    if (c == rightCol)
//                        cs.setBorderRight(border);
                }
            }
            if (r != topRow && r != bottomRow) {

                cell = row.getCell(leftCol);
                if (cell == null)
                    cell = row.createCell(leftCol);
                newcs = wb.createCellStyle();
                newcs.cloneStyleFrom(cell.getCellStyle());
                newcs.setBorderLeft(border);
                cell.setCellStyle(newcs);
                cell = row.getCell(rightCol);
                if (cell == null)  {
                    cell = row.createCell(rightCol);
                    setDefaultCellStyle(cell);
                }
                newcs = wb.createCellStyle();
                newcs.cloneStyleFrom(cell.getCellStyle());
                newcs.setBorderRight(border);
                cell.setCellStyle(newcs);

//                cell = row.getCell(leftCol);
//                if (cell == null)
//                    cell = row.createCell(leftCol);
//                cell.getCellStyle().setBorderLeft(border);
//                cell = row.getCell(rightCol);
//                if (cell == null)
//                    cell = row.createCell(rightCol);
//                cell.getCellStyle().setBorderRight(border);


            }
        }
    }

    public void hideCell(Cell cell) {
        CellStyle cs = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        short bg = cs.getFillBackgroundColor();
        f.setColor((short)9);
        cs.setFont(f);
        cell.setCellStyle(cs);
    }

    public int xlMultiPairColPanel(MultiPairColPanel mP, Sheet sheet, int row, int col) {
        Row r;
        Cell c;
        if (mP.bHasTitle) {
            r = sheet.getRow(row);
            if (r == null)
                r = sheet.createRow(row);
            c = r.createCell(col);
            c.setCellStyle(csHeader2);
            c.setCellValue(mP.title);
            r.createCell(col + 1);
            CellRangeAddress range = new CellRangeAddress(row, row, col, col + 1);
            sheet.addMergedRegion(range);
        }
        for (int n = 0; n < mP.getRowCount(); n++) {
            row++;
            if (!xlAddItemPair(sheet, row, col, mP.getComponentPair(n)))
                row--;
        }
        row++;
        return (row);
    }

    void xlAddItemPair(Sheet sheet, int row, int col, String name, String value) {
        Row r = sheet.getRow(row);
        Cell c;
        r = (r == null) ? sheet.createRow(row) : r;
        r.createCell(col).setCellValue(name);
        c = r.createCell(col + 1);
        double d = isNumeric(value);
        if (Double.isNaN(d))
            c.setCellValue(value);
        else
            c.setCellValue(d);

//        createCell(r, (short)col, CellStyle.ALIGN_LEFT, csNormal).setCellValue(name);
//        createCell(r, (short)(col + 1), CellStyle.ALIGN_RIGHT, csNormal).setCellValue(value);
    }


    boolean xlAddItemPair(Sheet sheet, int row, int col, ComponentPair pair) {
        boolean retVal = false;
        Component compL, compR;
        compL = pair.getComponent(true);
        compR = pair.getComponent(false);
//        if (compR.isEnabled()) {
            Row r = sheet.getRow(row);
            Cell c;
            r = (r == null) ? sheet.createRow(row) : r;
            c = r.createCell(col);
            if (compL instanceof XLcellData)
                setCellValue(c, ((XLcellData)compL).getValueForExcel());
            else {
                c.setCellValue(pair.compString(true));
                if (pair.isBold(true))
                    c.setCellStyle(csNormalBold);
            }
            c = r.createCell(col + 1);
            if (compR instanceof XLcellData)
                setCellValue(c, ((XLcellData)compR).getValueForExcel());
            else if (compR instanceof AbstractButton) {
                c.setCellValue(((AbstractButton)compR).isSelected() ? "Yes" : "No");
            }
            else {
                String value = pair.compString(false);
                double d = isNumeric(value);
                if (Double.isNaN(d))
                    c.setCellValue(value);
                else
                    c.setCellValue(d);
                if (pair.isBold(false))
                    c.setCellStyle(csNormalBold);
            }
            retVal = true;
//        }
        return retVal;

//        createCell(r, (short)col, CellStyle.ALIGN_LEFT, csNormal).setCellValue(name);
//        createCell(r, (short)(col + 1), CellStyle.ALIGN_RIGHT, csNormal).setCellValue(value);
    }

    public int xlAddXLCellData(Sheet sheet, int row, int col, Vector<XLcellData> data) {
        Row r;
        Cell c;
        int rowNow = row;
        XLcellData oneDat;
        ValueForExcel xlVal;
        String val;
        for (int n = 0; n < data.size(); n++) {
            oneDat = data.get(n);
            xlVal = oneDat.getValueForExcel();
            r = sheet.getRow(rowNow);
            r = (r == null) ? sheet.createRow(rowNow) : r;
            c = r.createCell(col);
            setCellValue(c, xlVal);
            rowNow++;
        } sheet.getRow(row);
        return rowNow;
    }

    void setNumberWithFormat(Cell cell, double value, String fmtStr) {
        cell.setCellValue(value);
        DataFormat format = wb.createDataFormat();
        cell.getCellStyle().setDataFormat(format.getFormat(fmtStr));

    }

    public void setCellValue(Cell c, ValueForExcel xlVal) {
        CellStyle cs = wb.createCellStyle();
        cs.cloneStyleFrom((xlVal.bBold)? csNormalBold : csNormal);
        short oldFmt = cs.getDataFormat();
        if (xlVal.bNumeric)  {
            c.setCellValue(xlVal.numValue);
            DataFormat format = wb.createDataFormat();
            cs.setDataFormat(format.getFormat(xlVal.fmtStr));
        }
        else
            c.setCellValue(xlVal.strValue);
        c.setCellStyle(cs);
//        if (xlVal.bBold)
//            c.setCellStyle(csNormalBold);
//        else
//            c.setCellStyle(csNormal);
//        cs.setDataFormat(oldFmt);
    }

    public static double isNumeric(String str) {
        double d = 0;
        try {
            d = Double.parseDouble(str.replace(",", ""));
        } catch (NumberFormatException nfe) {
            return Double.NaN;
        }
        return d;
    }

    public int xlAddXLCellData(Sheet sheet, int topRow, int leftCol, JTable table, boolean bHeaderOnly) {
        int nowRow = topRow;
        int nCols = table.getColumnCount();
        Row row = sheet.createRow(nowRow);
        Cell cell;
        for (int c = 0; c < nCols; c++) {
            cell = row.createCell(c + leftCol);
            cell.setCellValue(table.getColumnName(c));
            cell.setCellStyle(csNormalBold);
        }
        nowRow++;
        if (!bHeaderOnly)
            nowRow = xlAddXLCellData(sheet, nowRow, leftCol, table, 0, table.getRowCount());
        return nowRow;
    }

    public int xlAddXLCellData(Sheet sheet, int topRow, int leftCol, JTable table) {
        return xlAddXLCellData(sheet, topRow, leftCol, table, false);
    }

    public int xlAddXLCellData(Sheet sheet, int topRow, int leftCol, JTable table, int stRow, int endRow) {
        int nowRow = topRow;
        Row row;
        Cell cell;
        String val;
        double dVal;
        int nCols = table.getColumnCount();
        endRow = Math.min(endRow, table.getRowCount());
        for (int r = stRow; r < endRow; r++)  {
            row = sheet.createRow(nowRow);
            for (int c = 0; c < nCols; c++) {
                cell = row.createCell(c + leftCol);
                val = ("" + table.getValueAt(r, c)).replace(",", "");
                dVal = isNumeric(val);
                if (Double.isNaN(dVal))
                    cell.setCellValue(val);
                else
                    cell.setCellValue(dVal);
                if (c == 0)
                    cell.setCellStyle(csNormalBold);
            }
            nowRow++;
        }
        return nowRow;
    }
}
