package appReporting;

import mvUtils.display.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 12/16/13
 * Time: 10:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class Reporter implements InputControl {
    public enum ColType {
        NUMBER, TEXT
    }

    InputControl controller;
    String header;
    Vector <ColSpec> columns;
    String filePath;
    boolean colsFrozen = false;
    Vector <Vector<Object>> results;
    public Reporter(String header) {
        this.header = header;
        columns = new Vector<ColSpec>();
        controller = this;
    }

    public String getHeader() {
        return header;
    }

    public void clearReport() {
        results.clear();
    }

    public int xlReportColHead(Sheet sheet, ExcelStyles styles, int topRow, int leftCol) {
        Row r = sheet.createRow(topRow);
        r.setHeightInPoints((3*sheet.getDefaultRowHeightInPoints()));
        int col = leftCol;
        Cell cell;
        for (ColSpec cSp: columns) {
            sheet.setColumnWidth(col, cSp.xlWidth);
            cell = r.createCell(col++);
            cell.setCellStyle(styles.csTextWrapBold);
            cell.setCellValue(cSp.nameForXL);
        }
        return topRow + 1;
    }

    public int xlReportLines(Sheet sheet, ExcelStyles styles, int topRow, int leftCol, int slNo) {
        int nowRow = topRow;
        int col = leftCol;
        int pos;
        Row row;
        JTable table = getJTable();
        int tRow = table.getRowCount();
        int tCol = table.getColumnCount();
        XLcellData data;
        for (int r = 0; r < tRow; r++) {
            row = sheet.createRow(nowRow++);
            col = leftCol;
            (row.createCell(col++)).setCellValue(Double.valueOf("" + slNo++));
            for (int c = 0; c < tCol; c++)  {
                data = (XLcellData)(table.getValueAt(r, c));
                styles.setCellValue(row.createCell(col++), data.getValueForExcel() );
            }
        }
        return nowRow;
    }

    public int xlReportLinesOLD(Sheet sheet, ExcelStyles styles, int topRow, int leftCol) {
        int nowRow = topRow;
        int col = leftCol;
        int pos;
        Row r;
        for (Vector <Object> oneRow:results) {
            r = sheet.createRow(nowRow++);
            col = leftCol;
            pos = 0;
            for (Object obj:oneRow)
                columns.get(pos++).setCellValue(r.createCell(col++), obj);
        }
        return nowRow;
    }


    public void addColumn(ColType type, int width, int xlColWidth, String fmt, String... name) {
        columns.add(new ColSpec(type, width, xlColWidth, fmt, name));
    }

    public int addResultLine(Object... data) {
        freezeColumns();
        boolean ok = true;
        Vector <Object> theRow = new Vector<Object>();
        if (data.length == columns.size()) {
            int pos = 0;
            ColSpec oneCol;
            for (Object oneParam: data) {
                oneCol  = columns.get(pos);
                if (!oneCol.addToResults(oneParam, theRow)) {
                    ok = false;
                    break;
                }
                pos++;
            }
            if (ok)
                results.add(theRow);
        }
        else
            ok = false;
        if (ok)
            return results.size();
        else
            return 0;
    }

    public int addResultLine(Vector<Object> data) {
        freezeColumns();
        boolean ok = true;
        Vector <Object> theRow = new Vector<Object>();
        if (data.size() == columns.size()) {
            int pos = 0;
            ColSpec oneCol;
            for (Object oneParam: data) {
                oneCol  = columns.get(pos);
                if (!oneCol.addToResults(oneParam, theRow)) {
                    ok = false;
                    break;
                }
                pos++;
            }
            if (ok)
                results.add(theRow);
        }
        else
            ok = false;
        if (ok)
            return results.size();
        else
            return 0;
    }

    public int addResultLine(Double[] data) {
        freezeColumns();
        boolean ok = true;
        Vector <Object> theRow = new Vector<Object>();
        if (data.length == columns.size()) {
            int pos = 0;
            ColSpec oneCol;
            for (Object oneParam: data) {
                oneCol  = columns.get(pos);
                if (!oneCol.addToResults(oneParam, theRow)) {
                    ok = false;
                    break;
                }
                pos++;
            }
            if (ok)
                results.add(theRow);
        }
        else
            ok = false;
        if (ok)
            return results.size();
        else
            return 0;
    }

    void freezeColumns() {
        if (!colsFrozen) {
            results = new Vector<Vector<Object>>();
            colsFrozen = true;
        }
    }

    public JTable getJTable() {
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment( JLabel.RIGHT );

        int _rows = results.size();
        int _cols = columns.size();
        Object[][] allData = new Object[_rows][_cols];
        Object[] header = new Object[_cols];
        int nC = 0;
        for (ColSpec oneSpec: columns)
            header[nC++] = oneSpec.nameForJTable;
        Vector <Object> oneRow;
        for (int r = 0; r < _rows; r++) {
            oneRow = results.get(r);
            for (int c = 0; c < _cols; c++)
                allData[r][c] = columns.get(c).formattedData(oneRow.get(c));
        }
        JTable table = new JTable(allData, header);

        nC = 0;
        TableColumnModel colModel = table.getColumnModel();
        TableColumn col;
        for (ColSpec oneSpec: columns) {
            col = colModel.getColumn(nC++);
            col.setPreferredWidth(oneSpec.width);
            if (oneSpec.type == ColType.NUMBER)
                col.setCellRenderer( rightRenderer );
        }
        return table;
    }

    public int getColumnCount() {
        return columns.size();
    }

    public boolean canNotify() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void enableNotify(boolean ena) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Window parent() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    class ColSpec {
        Object[] colHead;
        String nameForJTable;
        String nameForXL;
        ColType type;
        int width;
        int xlWidth;
        String fmt;
        DecimalFormat dFmt;
//        JLabel nameLabel;

        ColSpec(ColType type, int width, int xlColWidth, String fmt, String... name) {
            colHead = name;
            nameForJTable = "<html>" + colHead[0];
            nameForXL = "" + colHead[0];
            for (int i = 1; i < colHead.length; i++) {
                nameForJTable += "<br>" + colHead[i];
                nameForXL += "\n" + colHead[i];
            }
//            nameLabel = new JLabel(name);
//            nameLabel.setPreferredSize(new Dimension(100, 20));
            this.type = type;
            this.width = width;
            xlWidth = xlColWidth;
            if (type == ColType.NUMBER)  {
                this.fmt = fmt;
                dFmt = new DecimalFormat(fmt);
            }
        }

        boolean addToResults(Object obj, Vector<Object> theRow) {
            boolean ok = true;
            switch (type)  {
                case NUMBER:
                    if (obj instanceof Double) {
                        theRow.add(obj);
                        break;
                    }
                    if (obj instanceof Integer) {
                        theRow.add(new Double((Integer)obj).intValue());
                    }
                    else
                        ok = false;
                    break;
                case TEXT:
                    if (obj instanceof String)
                        theRow.add(obj);
                    else
                        ok = false;
                    break;
            }
            return ok;
        }

        Object formattedData(Object data) {
            Object retVal = null;
            switch (type)  {
                case NUMBER:
                    retVal = new NumberLabel(controller, ((Double)data).doubleValue(), width, fmt);
                    break;
                case TEXT:
                    retVal = new TextLabel((String)data);
                    break;
            }
            return retVal;
        }

        void setCellValue(Cell cell, Object obj) {
            switch (type) {
                case NUMBER:
                    double val = ((Double)obj).doubleValue();
                    cell.setCellValue(dFmt.format(val));
                    break;
                case TEXT:
                    cell.setCellValue((String)obj);
            }
        }
    }
}
