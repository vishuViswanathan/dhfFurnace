package mvmath;

import com.sun.org.apache.bcel.internal.generic.NEW;
import display.NumberLabel;
import display.NumberTextField;
import display.TrendsPanel;
import sun.management.counter.Variability;
import sun.swing.table.DefaultTableCellHeaderRenderer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 7/20/12
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 * Multi columns with same X values
 */
public class MultiColData extends GraphInfoAdapter {
    int MAXCOLS = 10;
    int cols = 0;
    int rows;
    double[] xVals;
    Vector<Double> vXval;
    String xFormat = "";
    String xName = "";
    String[] colNames = new String[MAXCOLS];
    Hashtable<Integer, VariableDataTrace> colData;
    DoubleRange xRange;
    DoubleRange yRange;
    int colWidth = 0;

    public MultiColData(String xName, double[] xVals, String xFormat, int colWidth) {
        this.xName = xName;
        colData = new Hashtable<Integer, VariableDataTrace>();
        takeXvalues(xVals);
        this.xFormat = xFormat;
        this.colWidth = colWidth;
    }

    void takeXvalues(double[] xVals) {
        this.xVals = xVals;
        rows = xVals.length;
        xRange = new DoubleRange();
        vXval = new Vector<Double>();
        for (int r = 0; r < xVals.length; r++) {
            xRange.takeVal(xVals[r]);
            vXval.add(new Double(xVals[r]));
        }
    }

    public int getColumns() {
        return cols;
    }

    public void setXLimits(double min, double max) {
        xRange = VariableDataTrace.getAutoRange(min, max, true, true);
    }

    public void setYLimits(double min, double max) {
        yRange = VariableDataTrace.getAutoRange(min, max, true, true);
    }

    public int addColumn(String colName, String yFormat, Color color) {
        if (cols < MAXCOLS) {
            VariableDataTrace dp = new VariableDataTrace(xName, colName, xVals, xFormat, yFormat, color);
            colNames[cols] = new String(colName.trim());
            colData.put(cols, dp);
            cols++;
            return cols;
        } else
            return -1;
    }

    public int addColumn(String colName, String yFormat) {
        if (cols < MAXCOLS) {
            VariableDataTrace dp = new VariableDataTrace(xName, colName, xVals, xFormat, yFormat);
            colNames[cols] = new String(colName.trim());
            colData.put(cols, dp);
            cols++;
            return cols;
        } else
            return -1;
    }
//    public int addColumn(String colName) {
//        return addColumn(colName, "");
//    }

    public int setData(int col, double x, double val) {
        int ix = -1;
        VariableDataTrace dp;
        if (col < cols) {
            if ((ix = vXval.indexOf(new Double(x))) >= 0) {
                dp = colData.get(new Integer(col));
                dp.setYval(ix, val);
            }
        }
        return ix;
    }

    public boolean updateValue(int col, int row, double val) {
        boolean bRetVal = false;
        VariableDataTrace dp;
        if (col < cols) {
            dp = colData.get(col);
            bRetVal = (dp.setYval(row, val));
        }
        return bRetVal;
    }

    public int setData(int col, DoublePoint valDp) {
        return setData(col, valDp.x, valDp.y);
    }

    public int setData(int col, int row, double val) {
        int retVal = -1;
        if (col < cols) {
            VariableDataTrace dp;
            if (row < vXval.size()) {
                dp = colData.get(new Integer(col));
                dp.setYval(row, val);
                retVal = row;
            }
        }
        return retVal;
    }

    public double[][] getDataArray(int[] colList) {
        double[][] dataArr = null;
        boolean allAvailable = true;
        for (int c:colList) {
            if (c >= cols) {
                allAvailable = false;
                break;
            }
        }
        if (allAvailable) {
            int nRows = xVals.length;
            double nowX;
            dataArr = new double[nRows][colList.length + 1];
            for (int r = 0; r < nRows; r++) {
                nowX = xVals[r];
                dataArr[r][0] = nowX;
                for (int c = 0; c < colList.length; c++) {
                    dataArr[r][c + 1] = getYat(colList[c], nowX);
                }
            }
        }
        return dataArr;
    }

    public int addTraces(TrendsPanel tp, int count) {
        int nowCount = 0;
        VariableDataTrace vdt;
        for (int t = 0;t < colData.size(); t++)   {
            vdt = colData.get(t);
            if (vdt.isTraceable())
                tp.addTrace(this, nowCount++, vdt.color);
        }
        return count + nowCount;
    }

    @Override
    public DoublePoint[] getGraph(int trace) {
        if (trace < cols)
            return colData.get(trace).getGraph();
        else
            return null;
    }

    @Override
    public DoubleRange getXrange(int trace) {
        return getCommonXrange();
    }


    @Override
    public DoubleRange getCommonXrange() {
        return xRange;
    }

    @Override
    public DoubleRange getCommonYrange() {
        if (xRange == null) {
            DoubleRange range;
            DoubleRange commRange = new DoubleRange();

            for (int c = 0; c < cols; c++) {
                range = colData.get(c).getYrange();
                commRange.takeVal(range.min);
                commRange.takeVal(range.max);
            }
            return VariableDataTrace.getAutoRange(commRange.min, commRange.max);
        } else
            return yRange;

    }

    @Override
    public DoubleRange getYrange(int trace) {
        if (trace < cols)
            return colData.get(trace).getYrange();
        else
            return null;
    }

    @Override
    public TraceHeader getTraceHeader(int trace) {
        if (trace < cols)
            return colData.get(trace).getHeader();
        else
            return null;
    }

    @Override
    public double getYat(int trace, double x) {
        VariableDataTrace tr = colData.get(new Integer(trace));
        return tr.getYat(x);
    }

    @Override

    public JTable getResultTable() {
        JTable table = new JTable(new MyTableModel());
        DefaultTableCellRenderer rightRenderer = new CellRenderer(JLabel.RIGHT);
        DefaultTableCellRenderer leftRenderer = new CellRenderer(JLabel.LEFT);
/*
        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setCellRenderer(leftRenderer);
        colModel.getColumn(1).setCellRenderer(rightRenderer);
        colModel.getColumn(2).setCellRenderer(rightRenderer);
*/

        table.setDefaultRenderer(table.getColumnClass(0), rightRenderer);
//        table.setDefaultRenderer(table.getColumnClass(1), leftRenderer);
        return table;
    }


    public JTable getJTable() {
        int _rows = xVals.length;
        int _cols = colData.size() + 1;
        VariableDataTrace tr;
        Object[][] allData = new Object[_rows][_cols];
        Object[] header = new Object[_cols];
        header[0] = "Pos(m)";
        DecimalFormat xFmt = new DecimalFormat(xFormat);
        for (int r = 0; r < _rows; r++) {
            allData[r][0] = xFmt.format(xVals[r]);
        }
        for (int c = 1; c < _cols; c++) {
            tr = colData.get(c - 1);
            for (int r = 0; r < _rows; r++)
                allData[r][c] = new NumberLabel(tr.getYat(xVals[r]), 6, tr.yFormat);
            header[c] = colNames[c - 1];
        }
        return new JTable(allData, header);
    }
    @Override
    public String getYFormat(int trace) {
        return colData.get(new Integer(trace)).yFormat;    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public String getXFormat() {
        return xFormat;    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public String getXFormat(int trace) {
        return xFormat;
    }

    class MyTableModel extends AbstractTableModel {
        public MyTableModel() {

        }

        public int getColumnCount() {
            return cols + 1;
        }

        public String getColumnName(int col) {
            if (checkCol(col))
                if (col == 0)
                    return "Length";
                else
                    return colNames[col - 1];
            else return "";
        }

        boolean checkCol(int col) {
            return (col >= 0) && (col <= (cols + 1));
        }

        boolean checkRow(int row) {
            return (row >= 0 && row < xVals.length);

        }

        public int getRowCount() {
            return xVals.length;
        }

        public Object getValueAt(int row, int col) {
            String retVal = "";
            double data = 0;
            if (checkCol(col) && checkRow(row)) {
                if (col == 0)
                    retVal = formatNumber(xVals[row], xFormat);
                else
                    retVal = colData.get(new Integer(col - 1)).getFormatedYat(xVals[row]);
            }
            return retVal;
        }

        String formatNumber(double value, String format) {
            String fmt = format;
            if (fmt.length() <= 0) {
                double absVal = Math.abs(value);
                if (absVal == 0)
                    fmt = "#";
                else if (absVal < 0.001 || absVal > 1e5)
                    fmt = "#.###E00";
                else if (absVal > 100)
                    fmt = "###,###";
                else
                    fmt = "###.###";
            }
//            return ((new DecimalFormat(fmt)).format(value)).applyLocalizedPattern();
            return new DecimalFormat(fmt).format(value);
        }
    }


    public class CellRenderer extends DefaultTableCellRenderer {
        int alignment = 0;
        JLabel comp = new JLabel("Something");

        CellRenderer(int alignment) {
            super();
            this.alignment = alignment;
            comp.setHorizontalAlignment(alignment);
//            setHorizontalAlignment(alignment);
        }
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
/*
            JLabel comp = new JLabel((String)value);
            comp.setHorizontalAlignment(alignment);
*/
            comp.setText((String) value + "    ");
            comp.setPreferredSize(new Dimension(colWidth, 40));
            return comp;
        }
    }
}