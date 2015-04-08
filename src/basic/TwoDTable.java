package basic;

import mvUtils.math.DoublePoint;
import mvUtils.math.XYArray;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.io.*;
import java.text.*;

public class TwoDTable implements Serializable {

    int StructCode;
    public int nCols;
    public int nRows;
    boolean bColHeadSet;
    boolean bColAscending;
    boolean bCanInterpolateCol;
    boolean bLogCol;
    double[] colHeader;
    boolean bRowHeadSet;
    boolean bRowAscending;
    boolean bCanInterpolateRow;
    double[] rowHeader;
    boolean bLogRow;
    double[][] data;
    boolean allowRowAdd = false;
    int validRowCount = 0; // used only with allowRoadAdd
    boolean bOrderChecked = false;
    public String colHeadFmtStr, rowHeadFmtStr;
    DecimalFormat colHeadFmt, rowHeadFmt;
    DecimalFormat dataFmt;
    String rowHeadName, colHeadName; // definition of row and column heads
    boolean headNameSpecified = false;

    public TwoDTable() {
    }

    public TwoDTable(TwoDTable copyFrom) {
        this(copyFrom.colHeader, copyFrom.rowHeader, copyFrom.bLogCol,  copyFrom.bLogRow);
        setColAndRowHeadNames(copyFrom.colHeadName, copyFrom.rowHeadName);
        for (int row = 0; row < nRows; row++)
            for (int col = 0; col < nCols; col++)
                data[row][col] = copyFrom.data[row][col];
    }

    /**
      *
      * @param takeBaseFrom The size and the headers are taken from this table
      * @param defaultValue The default value sof all cells
      */
    public TwoDTable(TwoDTable takeBaseFrom, double defaultValue) {
        this(takeBaseFrom.colHeader, takeBaseFrom.rowHeader, takeBaseFrom.bLogCol,  takeBaseFrom.bLogRow);
        setColAndRowHeadNames(takeBaseFrom.colHeadName, takeBaseFrom.rowHeadName);
        for (int row = 0; row < nRows; row++)
            for (int col = 0; col < nCols; col++)
                data[row][col] = defaultValue;
    }


    public TwoDTable(int cols, int rows,
                     boolean logCol, boolean logRow) throws
            IllegalArgumentException {
        if (!setTwoDTable(cols, rows, logCol, logRow))
            throw new IllegalArgumentException("Argument error");
    }

    /**
     * @param colHead
     * @param rows
     * @param ascending this allows additional of rows with rowHeader
     *                  data assumed linear, caller to take care of ascending of descending of
     *                  row header
     */
    public TwoDTable(double[] colHead, int rows, boolean ascending,
                     boolean canInterpolateCol) {
        this(colHead,rows, ascending,  canInterpolateCol, false, false);
     }

    public TwoDTable(double[] colHead, int rows, boolean ascending,
                     boolean canInterpolateCol, boolean bLogCol, boolean bLogRow) {
        if (!setTwoDTable(colHead.length, rows, bLogCol, bLogRow))
            throw new IllegalArgumentException("Argument error");
        bRowAscending = ascending;
        allowRowAdd = true;
        bCanInterpolateCol = canInterpolateCol;
        setColHeader(colHead);
    }

    public TwoDTable(InputStream in, String cMsg) throws IllegalArgumentException {
        if (!loadTableAt(in, cMsg)) {
            throw new IllegalArgumentException("Argument or InputStream error");
        }
    }

    public TwoDTable(String inStr, String cMsg) throws Exception {
        InputStream in=  new ByteArrayInputStream(inStr.getBytes());
        if (!loadTableAt(in, cMsg)) {
            throw new Exception("Unable to create TwoDTable for " + cMsg);
        }
    }

     public void setColAndRowHeadNames(String colHeadName, String rowHeadName) {
        this.colHeadName = colHeadName;
        this.rowHeadName = rowHeadName;
        headNameSpecified = true;
    }

    public double getMinRowHead() {
        return (bRowAscending) ? rowHeader[0] : rowHeader[nRows - 1];
    }

    public double getMaxRowHead() {
        return (bRowAscending) ? rowHeader[nRows - 1] : rowHeader[0];
    }

    public double getMinColHead() {
        return (bColAscending) ? colHeader[0] : colHeader[nCols - 1];
    }

    public boolean IsColHeadInRange(double head) {
        return (head >= getMinColHead() && head <= getMaxColHead());
    }

    public CheckInRange checkRowHeadInRange(double head) {
        CheckInRange checkResult = new CheckInRange();
        double max = getMaxRowHead();
        double min = getMinRowHead();
        if (head > max)
            checkResult.maxViolated(max);
        else  if (head < min)
            checkResult.minViolated(min);
        return checkResult;
    }

    public boolean IsRowHeadInRange(double head) {
        return (head >= getMinRowHead() && head <= getMaxRowHead());
    }

    public CheckInRange checkColHeadInRange(double head) {
        CheckInRange checkResult = new CheckInRange();
        double max = getMaxColHead();
        double min = getMinColHead();
        if (head > max)
            checkResult.maxViolated(max);
        else  if (head < min)
            checkResult.minViolated(min);
        return checkResult;
    }

    public double getMaxColHead() {
        return (bColAscending) ? colHeader[nCols - 1] : colHeader[0];
    }

    private boolean setTwoDTable(int nCols, int nRows,
                                 boolean bLogCol, boolean bLogRow) {
        this.nRows = nRows;
        this.nCols = nCols;
        this.bLogCol = bLogCol;
        this.bLogRow = bLogRow;
        if (nCols > 0 && nRows > 0) {
            colHeader = new double[nCols];
            rowHeader = new double[nRows];
            data = new double[nRows][nCols];
            if (nRows < 2)
                bRowHeadSet = true; // No need for header
            else
                bRowHeadSet = false;
//      if (nCols < 2)
//        bColHeadSet = true; // no need for header
//      else
            bColHeadSet = false;

            bCanInterpolateCol = false;
            bCanInterpolateRow = false;
            return true;
        } else
            return false;
    }

    public TwoDTable(double[] colH, double[] rowH)  {
        this(colH, rowH, false, false);
    }

    public TwoDTable(double[] colH, double[] rowH, boolean bLogCol, boolean bLogRow)  {
        this(colH.length, rowH.length, bLogCol, bLogRow);
        setColHeader(colH);
        setRowHeader(rowH);
    }

    public void setFormats(String colHeadFmtStr, String rowHeadFmtStr, String dataFmtStr) {
        this.colHeadFmtStr = colHeadFmtStr;
        this.rowHeadFmtStr = rowHeadFmtStr;
        this.colHeadFmt = new DecimalFormat(colHeadFmtStr);
        this.rowHeadFmt = new DecimalFormat(rowHeadFmtStr);
        this.dataFmt = new DecimalFormat(dataFmtStr);
    }

    boolean setColHeader(double[] header) {
        boolean bRetval = false;
        boolean bInOrder;
        boolean bAscending = true;
        if (bColHeadSet) {
            errMessage("Column Header Already Set in 2D table");
            return bRetval;
        }
        if (nCols == header.length) {
            for (int n = 0; n < nCols; n++)
                colHeader[n] = header[n];
            bRetval = true;
            bColHeadSet = true;
            if (nCols > 1) {
                if (header[1] > header[0])
                    bAscending = true;
                else
                    bAscending = false;
                if (nCols > 2) {
                    bInOrder = true;
                    for (int n = 2; n < nCols; n++) {
                        if ((bAscending && (header[n - 1] >= header[n])) ||
                                (!bAscending && (header[n - 1] <= header[n]))) {
                            bInOrder = false;
                            break;
                        }
                    }
                } else {
                    bInOrder = true;
                }
            } else
                bInOrder = false;
            if (bInOrder) {
                bCanInterpolateCol = true;
                bColAscending = bAscending;
            }
        } else {
            errMessage("Columns mismatch");
        }
        return bRetval;
    }

    boolean setRowHeader(double[] header) {
        boolean bRetval = false;
        boolean bInOrder;
        boolean bAscending = true;
        if (bRowHeadSet) {
            errMessage("Row Header Already Set in 2D table");
            return bRetval;
        }
        if (nRows == header.length) {
            for (int n = 0; n < nRows; n++)
                rowHeader[n] = header[n];
            bRetval = true;
            bRowHeadSet = true;
            if (nRows > 1) {
                if (header[1] > header[0])
                    bAscending = true;
                else
                    bAscending = false;
                if (nRows > 2) {
                    bInOrder = true;
                    for (int n = 2; n < nRows; n++) {
                        if ((bAscending && (header[n - 1] >= header[n])) ||
                                (!bAscending && (header[n - 1] <= header[n]))) {
                            bInOrder = false;
                            break;
                        }
                    }
                } else {
                    bInOrder = true;
                }
            } else
                bInOrder = false;
            if (bInOrder) {
                bCanInterpolateRow = true;
                bRowAscending = bAscending;
            }
        } else {
            errMessage("Rows mismatch");
        }
        if (bRetval)
            validRowCount = nRows;
        bOrderChecked = true;
        return bRetval;
    }

    public boolean setOneValue(double rowHead, double colHead, double val) {
        int row = -1;
        int col = -1;
        boolean bRetVal = false;
        for (int r = 0; r < rowHeader.length; r++)
            if (rowHeader[r] == rowHead) {
                row = r;
                break;
            }
        if (row >= 0) {
            for (int c = 0; c < colHeader.length; c++)
                if (colHeader[c] == colHead) {
                    col = c;
                    break;
                }
            if (col >= 0) {
                data[row][col] = val;
                bRetVal = true;
            }
        }
        return bRetVal;
    }

    boolean setOneRow(double rowHead, double[] oneRow) {
        boolean bFound = false;
        int theRow = -1;
        if (!bRowHeadSet && !allowRowAdd) {
            errMessage("Row Header NOT set in 2D table");
            return false;
        }
        if (nCols == oneRow.length) {
            if (nRows > 1) {
                for (int n = 0; n < nRows; n++) {
                    if (rowHeader[n] == rowHead) {
                        theRow = n;
                        bFound = true;
                        break;
                    }
                }
            } else { // do not bother about RowHead
                theRow = 0;
                bFound = true;
            }
            if (bFound) {
                return setSelectedRow(theRow, oneRow);
            }
        }
        return false;
    }

    boolean setSelectedRow(int theRow, double[] oneRow) {
        for (int n = 0; n < nCols; n++)
            data[theRow][n] = oneRow[n];
        return true;
    }

    boolean addRow(double rowHead, double[] oneRow) {
        if (validRowCount < nRows && oneRow.length == nCols) {
            rowHeader[validRowCount] = rowHead;
            setOneRow(rowHead, oneRow);
            validRowCount++;
            return true;
        } else
            return false;
    }

    boolean setOneColumn(double colHead, double[] oneCol) {
        boolean bFound = false;
        int theCol = -1;
        if (!bColHeadSet) {
            errMessage("Column Header NOT set in 2D table");
            return false;
        }
        if (nRows == oneCol.length) {
            if (nCols > 1) {
                for (int n = 0; n < nCols; n++) {
                    if (colHeader[n] == colHead) {
                        theCol = n;
                        bFound = true;
                        break;
                    }
                }
            } else { // do not bother about colHead
                theCol = 0;
                bFound = true;
            }
            if (bFound) {
                return setSelectedColumn(theCol, oneCol);
            }
        }
        return false;
    }

    boolean setSelectedColumn(int theCol, double[] oneCol) {
        for (int n = 0; n < nRows; n++)
            data[n][theCol] = oneCol[n];
        return true;
    }

    public double getData(double colHead, double rowHead) throws Exception {
        return getData(colHead, rowHead, true);
    }

    double getData(double colHead, double rowHead, boolean bInterpolate) throws
            Exception {
        double theValue = 0;
        boolean allOK = false;
        int theRow = -1, theCol = -1;
        int iColBase = -1;
        int iRowBase = -1;
        if (!bOrderChecked)
            checkDataOrder();
        int rowsToCheck = (allowRowAdd) ? validRowCount : nRows;
        boolean bInterpolCol = bInterpolate && bCanInterpolateCol,
                bInterpolRow = bInterpolate && bCanInterpolateRow;

        if (!bRowHeadSet && !allowRowAdd) { // if allowRowAdd use allowRowAdd instead of nRows
            throw new Exception("Row Header NOT set in 2D table");
        }

        if (!bColHeadSet) {
            throw new Exception("Column Header NOT set in 2D table");
        }

        if (rowsToCheck < 2) // donot bother about RowHead
            theRow = 0;
        else {
            if (bInterpolRow) {
                if (bRowAscending) {
                    if (rowHeader[rowsToCheck - 1] < rowHead) {
                        theRow = rowsToCheck - 1;
                    } else if (rowHeader[0] > rowHead) {
                        theRow = 0;
                    }
                } else {
                    if (rowHeader[rowsToCheck - 1] > rowHead) {
                        theRow = rowsToCheck - 1;
                    } else if (rowHeader[0] < rowHead) {
                        theRow = 0;
                    }
                }
            }
            if (theRow < 0) {
                for (int n = 0; n < rowsToCheck; n++) {
                    if (rowHeader[n] == rowHead) {
                        theRow = n;
                        break;
                    }
                    if (bInterpolRow) {
                        if (bRowAscending) {
                            if (rowHeader[n] > rowHead) {
                                iRowBase = n;
                                break;
                            }
                        } else {
                            if (rowHeader[n] < rowHead) {
                                iRowBase = n;
                                break;
                            }
                        }
                    } else if (bCanInterpolateRow) {
                        if (bRowAscending) {
                            if (rowHeader[n] > rowHead) { // out of range
                                break;
                            }
                        } else {
                            if (rowHeader[n] < rowHead) { // out of range
                                break;
                            }
                        }
                    }
                }
            } // if theRow < 0
        } // if rowsToCheck > 1

        if (nCols < 2) // donot bother about ColHead
            theCol = 0;
        else {
            if (bInterpolCol) {
                if (bColAscending) {
                    if (colHeader[nCols - 1] < colHead) {
                        theCol = nCols - 1;
                    } else if (colHeader[0] > colHead) {
                        theCol = 0;
                    }
                } else {
                    if (colHeader[nCols - 1] > colHead) {
                        theCol = nCols - 1;
                    } else if (colHeader[0] < colHead) {
                        theCol = 0;
                    }
                }
            }
            if (theCol < 0) {
                for (int n = 0; n < nCols; n++) {
                    if (colHeader[n] == colHead) {
                        theCol = n;
                        break;
                    }
                    if (bInterpolCol) {
                        if (bColAscending) {
                            if (colHeader[n] > colHead) {
                                iColBase = n;
                                break;
                            }
                        } else {
                            if (colHeader[n] < colHead) {
                                iColBase = n;
                                break;
                            }
                        }
                    } else if (bCanInterpolateCol) {
                        if (bColAscending) {
                            if (colHeader[n] > colHead) { // out of range
                                break;
                            }
                        } else {
                            if (colHeader[n] < colHead) { // Out of range
                                break;
                            }
                        }
                    }
                }
            } // if theCol < 0
        } // if nCols > 1

        // get the value
        if (theCol >= 0 && theRow >= 0) {
            theValue = data[theRow][theCol];
            allOK = true;
        } else if (bInterpolate) {
            do {
                if ((theCol < 0) && ((iColBase < 0) || !bInterpolCol))
                    break;
                if ((theRow < 0) && ((iRowBase < 0) || !bInterpolRow))
                    break;

                int colBef = 0, colAft = 0;
                int rowBef = 0, rowAft = 0;
                // check if it requires extrapolation
                if (theCol < 0) {
                    if (bColAscending) {
                        if (iColBase == 0)
                            break;
                        colBef = iColBase - 1;
                        colAft = iColBase;
                    } else {
                        if (iColBase >= nCols)   // 20140425 was (nCols - 1))
                            break;
                        colBef = iColBase - 1;
                        colAft = iColBase;
                    }
                }
                if (theRow < 0) {
                    if (bRowAscending) {
                        if (iRowBase <= 0)
                            break;
                        rowBef = iRowBase - 1;
                        rowAft = iRowBase;
                    } else {
                        if (iRowBase >= rowsToCheck)
                            break;
                        rowBef = iRowBase - 1;
                        rowAft = iRowBase;
                    }
                }

                // Now Get interpolated data
                if (theRow >= 0) { // no interpolation for rows
                    double f1 = data[theRow][colBef];
                    double f2 = data[theRow][colAft];
                    if (bLogCol)
                        theValue = f1 + (f2 - f1) /
                                (log10(colHeader[colAft]) -
                                        log10(colHeader[colBef])) *
                                (log10(colHead) -
                                        log10(colHeader[colBef]));
                    else
                        theValue = f1 + (f2 - f1) /
                                (colHeader[colAft] - colHeader[colBef]) *
                                (colHead - colHeader[colBef]);
                    allOK = true;
                    break;
                } else if (theCol >= 0) { // no interpolation for columns
                    double f1 = data[rowBef][theCol];
                    double f2 = data[rowAft][theCol];
                    if (bLogRow)  {
                        double f = (log10(rowHead) -
                                        log10(rowHeader[rowBef])) /
                                    (log10(rowHeader[rowAft]) -
                                        log10(rowHeader[rowBef]));
                        theValue = f1 + (f2 - f1) /
                                (log10(rowHeader[rowAft]) -
                                        log10(rowHeader[rowBef])) *
                                (log10(rowHead) -
                                        log10(rowHeader[rowBef]));
                    }
                    else
                        theValue = f1 + (f2 - f1) /
                                (rowHeader[rowAft] - rowHeader[rowBef]) *
                                (rowHead - rowHeader[rowBef]);
                    allOK = true;
                    break;
                }

                // interpolation for columns and rows
                double fRowBef = 0, fRowAft = 0; // interpolated for the two rows
                // interpolate between columns for row rowBef
                { // just a block
                    double f1 = data[rowBef][colBef];
                    double f2 = data[rowBef][colAft];
                    if (bLogCol)
                        fRowBef = f1 + (f2 - f1) /
                                (log10(colHeader[colAft]) -
                                        log10(colHeader[colBef])) *
                                (log10(colHead) -
                                        log10(colHeader[colBef]));
                    else
                        fRowBef = f1 + (f2 - f1) /
                                (colHeader[colAft] - colHeader[colBef]) *
                                (colHead - colHeader[colBef]);
                }

                // interpolate between columns for row rowAft
                { // just a block
                    double f1 = data[rowAft][colBef];
                    double f2 = data[rowAft][colAft];
                    if (bLogCol)
                        fRowAft = f1 + (f2 - f1) /
                                (log10(colHeader[colAft]) -
                                        log10(colHeader[colBef])) *
                                (log10(colHead) -
                                        log10(colHeader[colBef]));
                    else
                        fRowAft = f1 + (f2 - f1) /
                                (colHeader[colAft] - colHeader[colBef]) *
                                (colHead - colHeader[colBef]);
                }

                // final interpolation between rows
                if (bLogRow)
                    theValue = fRowBef + (fRowAft - fRowBef) /
                            (log10(rowHeader[rowAft]) -
                                    log10(rowHeader[rowBef])) *
                            (log10(rowHead) -
                                    log10(rowHeader[rowBef]));
                else
                    theValue = fRowBef + (fRowAft - fRowBef) /
                            (rowHeader[rowAft] - rowHeader[rowBef]) *
                            (rowHead - rowHeader[rowBef]);
                allOK = true;
                break;
            }
            while (true);
        }

        if (!allOK)
            throw new Exception("Some Error in Getting table data");
        return theValue;
    }

    double[] getOneRowWithColData(double rowHead, double[] data) {
        double[] retVal = data;
        if (retVal == null)
            retVal = new double[1 + nCols];
        int n = 0;
        try {
            for (; n < nCols; n++)
                retVal[n] = getData(colHeader[n], rowHead, true);
        } catch (Exception e) {
            errMessage("Problem getting data at column " + n);
            retVal[0] = Double.NaN;
        }
        return retVal;
    }

    StringBuilder getOneRowString(int r) {
        StringBuilder text = new StringBuilder();
        if (r >= 0 && r <nRows) {
            text.append("" + rowHeadFmt.format(rowHeader[r]));
            double rDat[] = data[r];
            for (double v:rDat)
                text.append(", " + dataFmt.format(v));
            text.append("\n");
        }
        return text;
    }

    public double[][] getOneColWithRowHead(int col) {
        double[][] retVal = null;
        if (col >= 0 && col < nCols) {
            int rowsToCheck = (allowRowAdd) ? validRowCount : nRows;
            retVal = new double[2][rowsToCheck];
            for (int n = 0; n < rowsToCheck; n++) {
                retVal[0][n] = rowHeader[n];
                retVal[1][n] = data[n][col];
            }
        }
        return retVal;
    }

    public double[] getOneRow(int row, double[] dataWithHead) {
        double[] retVal = dataWithHead;
        if (retVal == null)
            retVal = new double[1 + nCols];
        int rowsToCheck = (allowRowAdd) ? validRowCount : nRows;
        if (row >= 0 || row < rowsToCheck) {
            int n = 0;
            try {
                for (; n < nCols; n++)
                    retVal[n + 1] = getData(colHeader[n], rowHeader[row], true);
                retVal[0] = rowHeader[row];
            } catch (Exception e) {
                errMessage("Problem getting data at column " + n);
                retVal[0] = Double.NaN;
            }
        } else {
            retVal[0] = Double.NaN;
        }
        return retVal;
    }

    void checkDataOrder() {
        // check rows
        int totRows = rowHeader.length;
        if (totRows > 1) {
            bCanInterpolateRow = true;
            double lastVal = rowHeader[0];
            if (rowHeader[1] > lastVal)

                bColAscending = true;
            else
                bColAscending = false;
            lastVal = rowHeader[1];
            if (totRows > 2) {
                for (int r = 2; r < totRows; r++) {
                    if ((bColAscending && !(rowHeader[r] > lastVal)) ||
                            (!bColAscending && !(rowHeader[r] < lastVal))) {
                        bCanInterpolateRow = false;
                        break;
                    }
                    lastVal = rowHeader[r];
                }
            }
        }
        bOrderChecked = true;
    }

    boolean loadTableAt(InputStream in, String cMsg) {
        boolean allOK = false;
        try {
            while (true) {
                int rows, cols;
                boolean logCol, logRow;
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(in));
                // Get Number of Columns
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                int startLoc, endLoc;
                startLoc = line.indexOf("COL");
                if (startLoc < 0) {
                    errMessage("Column Numbers NOT found in " + cMsg);
                    errMessage("the line: " + line);
                    break;
                }
                startLoc = line.indexOf(':', startLoc);
                if (startLoc < 0) {
                    errMessage("Column Number Location NOT found in " + cMsg);
                    break;
                }
                startLoc++;
                endLoc = line.indexOf(',', startLoc);
                if (endLoc < 0) {
                    errMessage("Column Mode NOT found in " + cMsg);
                    break;
                }
                try {
                    cols = new Integer(line.substring(startLoc, endLoc).trim()).intValue();
                } catch (NumberFormatException mfe) {
                    errMessage("nCols Error: " + mfe);
                    break;
                }
                if (cols <= 0) {
                    errMessage("Column Number (" + cols + ") ERROR in " + cMsg);
                    break;
                }
                endLoc++;
                line = (line.substring(endLoc)).trim();
                if (line.equals("LOG")) {
                    logCol = true;
                } else if (line.equals("LIN")) {
                    logCol = false;
                } else {
                    errMessage("Unknown Column Mode " + line + " in " + cMsg);
                    break;
                }

                // Get Number of Rows
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                startLoc = line.indexOf("ROWS");
                if (startLoc < 0) {
                    errMessage("Row Numbers NOT found in " + cMsg);
                    break;
                }
                startLoc = line.indexOf(':', startLoc);
                if (startLoc < 0) {
                    errMessage("Row Number Location NOT found in " + cMsg);
                    break;
                }
                startLoc++;
                endLoc = line.indexOf(',', startLoc);
                if (endLoc < 0) {
                    errMessage("Row Mode NOT found in " + cMsg);
                    break;
                }

                try {
                    rows = new Integer(line.substring(startLoc, endLoc).trim()).intValue();
                } catch (NumberFormatException mfe) {
                    errMessage("nRows Error: " + mfe);
                    break;
                }
                if (rows <= 0) {
                    errMessage("Row Number (" + rows + ") ERROR in " + cMsg);
                    break;
                }
                endLoc++;
                line = (line.substring(endLoc)).trim();
                if (line.equals("LOG")) {
                    logRow = true;
                } else if (line.equals("LIN")) {
                    logRow = false;
                } else {
                    errMessage("Unknown Row Mode " + line + " in " + cMsg);
                    break;
                }

                if (!setTwoDTable(cols, rows, logCol, logRow)) {
                    break;
                }
                boolean breakOut = false;
                int iData;

                // Get Column Header
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                startLoc = line.indexOf("COL HEADS");
                if (startLoc < 0) {
                    errMessage("Column Heads NOT found for " + cMsg);
                    break;
                }

                line = reader.readLine();
                if (line == null) {
                    break;
                }

                // parse col header data into tempColHeader[]
                double[] tempColHeader = new double[nCols];
                iData = 0;
                for (iData = 0; iData < nCols - 1; iData++) {
                    endLoc = line.indexOf(',', startLoc);
                    if (endLoc < 0) {
                        errMessage(
                                "Delimiter ',' NOT found in Column Header for " + cMsg);
                        breakOut = true;
                        break;
                    }
                    try {
                        tempColHeader[iData] = new Double(line.substring(startLoc, endLoc).
                                trim()).doubleValue();
                    } catch (NumberFormatException nfe) {
                        errMessage("col Header: " + nfe);
                        breakOut = true;
                        break;
                    }
                    startLoc = endLoc + 1;
                }
                if (breakOut) {
                    break;
                }
                // the last one does not have ',' delimiter
                try {
                    tempColHeader[iData] = new Double(line.substring(startLoc).trim()).
                            doubleValue();
                } catch (NumberFormatException nfe) {
                    errMessage("col Header: " + nfe);
                    break;
                }

                // set column header
                setColHeader(tempColHeader);

                // Get Row Header
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                startLoc = line.indexOf("ROW HEADS");
                if (startLoc < 0) {
                    errMessage("Row Heads NOT found for " + cMsg);
                    break;
                }

                line = reader.readLine();
                if (line == null) {
                    break;
                }

                // parse col header data into tempRowHeader[]
                double[] tempRowHeader = new double[nRows];
                iData = 0;
                for (iData = 0; iData < nRows - 1; iData++) {
                    endLoc = line.indexOf(',', startLoc);
                    if (endLoc < 0) {
                        errMessage(
                                "Delimiter ',' NOT found in Column Header for " + cMsg);
                        breakOut = true;
                        break;
                    }
                    try {
                        tempRowHeader[iData] =
                                new Double(line.substring(startLoc, endLoc).trim()).doubleValue();
                    } catch (NumberFormatException nfe) {
                        errMessage("row Header: " + nfe);
                        breakOut = true;
                        break;
                    }
                    startLoc = endLoc + 1;
                }
                if (breakOut) {
                    break;
                }
                // the last one does not have ',' delimiter
                try {
                    tempRowHeader[iData] =
                            new Double(line.substring(startLoc).trim()).doubleValue();
                } catch (NumberFormatException nfe) {
                    errMessage("row Header: " + nfe);
                    break;
                }

                // set Row header
                setRowHeader(tempRowHeader);

                // get the table data for each row , the first element is the row head
                //   value
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                startLoc = line.indexOf("DATA ROWS");
                if (startLoc < 0) {
                    errMessage("DATA Rows NOT found for " + cMsg);
                    break;
                }

                double rowRef;
                double[] oneRow = new double[nCols];
                for (int iRow = 0; iRow < nRows; iRow++) {
                    line = reader.readLine();
                    startLoc = 0;
                    if (line == null) {
                        break;
                    }

                    // the first element is the row header value
                    endLoc = line.indexOf(",", startLoc);
                    if (endLoc < 0) {
                        errMessage("Delimiter ',' NOT found for row head of Data for " +
                                cMsg + "(" +
                                iRow + ")");
                        errMessage("The line: (" + line + ")");
                        breakOut = true;
                        break;
                    }
                    try {
                        rowRef = new Double(line.substring(startLoc, endLoc).trim()).
                                doubleValue();
                    } catch (NumberFormatException nfe) {
                        errMessage("Data row head :(" + iRow + ") :" + nfe);
                        breakOut = true;
                        break;
                    }
                    startLoc = endLoc + 1;

                    // parse data
                    for (iData = 0; iData < nCols - 1; iData++) {
                        endLoc = line.indexOf(",", startLoc);
                        if (endLoc < 0) {
                            errMessage("Delimiter ',' NOT found in Data for " + cMsg +
                                    "(Row " +
                                    iRow + ")(Col " + iData + ")");
                            breakOut = true;
                            break;
                        }
                        try {
                            oneRow[iData] = new Double(line.substring(startLoc, endLoc).trim()).
                                    doubleValue();
                        } catch (NumberFormatException nfe) {
                            errMessage("Data :(" + iData + ") :" + nfe);
                            breakOut = true;
                            break;
                        }
                        startLoc = endLoc + 1;
                    }

                    if (breakOut)
                        break;
                    // the last one does not have ',' delimiter
                    try {
                        oneRow[iData] = new Double(line.substring(startLoc).trim()).
                                doubleValue();
                    } catch (NumberFormatException nfe) {
                        errMessage("Data : last " + nfe);
                        breakOut = true;
                        break;
                    }

                    // set one row of data
                    if (!setOneRow(rowRef, oneRow)) {
                        errMessage("Problem in setting data for row " + iRow +
                                " row Head " + rowRef);
                        breakOut = true;
                        break;
                    }
                }
                if (breakOut)
                    break;
                allOK = true;
                break;
            }
        } catch (IOException ie) {
            errMessage("loadTableAt: " + cMsg + ": " + ie);
            allOK = false;
        }
//        if (allOK)
//            System.out.println("allOK");
        return allOK;
    }

    double log10(double arg) {
        return Math.log(arg) / Math.log(10);
    }

    public String getOneRow(int row, String headfmt, String datafmt) {
        String retVal;
        int rowsToCheck = (allowRowAdd) ? validRowCount : nRows;
        if (row >= 0 && row < rowsToCheck) {
            DecimalFormat dfHead = new DecimalFormat(headfmt);
            DecimalFormat dfData = new DecimalFormat(datafmt);
            StringBuffer buff = new StringBuffer(dfHead.format(rowHeader[row]) + ": ");
            for (int n = 0; n < nCols; n++) {
                if (n == (nCols - 1))
                    buff.append(dfData.format(data[row][n]));
                else
                    buff.append(dfData.format(data[row][n]) + ", ");
            }
            retVal = new String(buff);
        } else
            retVal = new String("No Data");
        return retVal;
    }

    public String getColumnHeader(String fmt) {
        DecimalFormat df = new DecimalFormat(fmt);
        String retVal;
        StringBuffer buff = new StringBuffer();
        for (int n = 0; n < nCols; n++) {
            if (n == (nCols - 1))
                buff.append(df.format(colHeader[n]));
            else
                buff.append(df.format(colHeader[n]) + ", ");
        }
        retVal = new String(buff);
        return retVal;
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        out.writeInt(StructCode);
        out.writeInt(nCols);
        out.writeInt(nRows);
        out.writeBoolean(bColHeadSet);
        out.writeBoolean(bColAscending);
        out.writeBoolean(bCanInterpolateCol);
        out.writeBoolean(bLogCol);
        int i;
        // DEBUG
//        debug("Writing Column Headers");
        for (i = 0; i < colHeader.length; i++)
            out.writeDouble(colHeader[i]);
        out.writeBoolean(bRowHeadSet);
        out.writeBoolean(bRowAscending);
        out.writeBoolean(bCanInterpolateRow);
        // DEBUG
//        debug("Writing Row Headers");
        for (i = 0; i < rowHeader.length; i++)
            out.writeDouble(rowHeader[i]);
        out.writeBoolean(bLogRow);
        // DEBUG
//        debug("Writing Data");
        for (int r = 0; r < nRows; r++)
            for (int c = 0; c < nCols; c++)
                out.writeDouble(data[r][c]);
        out.writeBoolean(allowRowAdd);
        out.writeInt(validRowCount);
        // TEMP
        out.writeChars("Finished TwoDTable");
        // DEBUG
//        debug("Finished Writing");
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        StructCode = in.readInt();
        nCols = in.readInt();
        nRows = in.readInt();
        bColHeadSet = in.readBoolean();
        bColAscending = in.readBoolean();
        bCanInterpolateCol = in.readBoolean();
        bLogCol = in.readBoolean();
        colHeader = new double[nCols];
        for (int i = 0; i < colHeader.length; i++)
            colHeader[i] = in.readDouble();
        double[] colHeader;
        bRowHeadSet = in.readBoolean();
        bRowAscending = in.readBoolean();
        bCanInterpolateRow = in.readBoolean();
        rowHeader = new double[nRows];
        for (int i = 0; i < rowHeader.length; i++)
            rowHeader[i] = in.readDouble();
        bLogRow = in.readBoolean();
        data = new double[nRows][nCols];
        for (int r = 0; r < nRows; r++)
            for (int c = 0; c < nCols; c++)
                data[r][c] = in.readDouble();
        double[][] data;
        allowRowAdd = in.readBoolean();
        validRowCount = in.readInt();

    }

    public XYArray getRowArray(double rowHead) throws Exception  {
        XYArray retArr = new XYArray();
        double[] data =  new double[colHeader.length + 1];
        getOneRowWithColData(rowHead, data);
        for (int c = 0; c < colHeader.length; c++)
            retArr.add(new DoublePoint(colHeader[c], data[c]));
        return retArr;
     }

    void printData() {
        PrintStream ps = System.out;
        ps.println("TwoDTable");
        for (int n = 0; n < nRows; n++) {
            ps.println("Row #" + n + ": " + rowHeader[n] + ">> " + data[n][0] + ", " +
                    data[n][1]);
        }
    }

    /**
     *
     * @return if the format
     * A line with
     * 0, columnsHeader list with comma separator
     * For each row a line with
     * rowHead, data list of the row with comma separator
     */

    public String csvString() {
        StringBuilder csvStr = new StringBuilder("0");
        for (double cH:colHeader)
            csvStr.append(", " + colHeadFmt.format(cH));
        int r = 0;
        int nCol = colHeader.length;
        for (double rH:rowHeader) {
            csvStr.append(rowHeadFmt.format(rH));
            for (int c = 0; c < nCol; c++)
                csvStr.append(", " + dataFmt.format(data[r][c]));
            csvStr.append("\n");
            r++;
        }
        csvStr.append("\n");
        return csvStr.toString();
    }

    public StringBuilder dataForTextFile() {
        StringBuilder text = new StringBuilder();
        text.append("COL:" + nCols + "," + ((bLogCol) ? "LOG" : "LIN") + "\n");
        text.append("ROWS:" + nRows + "," + ((bLogRow) ? "LOG" : "LIN") + "\n");
        text.append("COL HEADS\n");
        text.append("" + colHeadFmt.format(colHeader[0]));
        for (int c = 1; c < colHeader.length; c++)
            text.append(", " + colHeadFmt.format(colHeader[c]));
        text.append("\n");
        text.append("ROW HEADS\n");
        text.append("" + rowHeadFmt.format(rowHeader[0]));
        for (int r = 1; r < rowHeader.length; r++)
            text.append(", " + rowHeadFmt.format(rowHeader[r]));
        text.append("\n");
        text.append("DATA ROWS\n");
        for (int r = 0; r < nRows; r++)
            text.append(getOneRowString(r));
        return text;
    }

    public JTable getTable() {
        JTable table = new JTable(new MyTableModel());
        TableColumnModel colModel = table.getColumnModel();
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment( JLabel.RIGHT );
        colModel.getColumn(0).setPreferredWidth((new JLabel(rowHeadFmt.format(rowHeader[0]))).getWidth());
        int colHeadWidth = (new JLabel(colHeadFmt.format(colHeader[0]))).getWidth();
        int dataWidth = (new JLabel(dataFmt.format(data[0][0]))).getWidth();
        int prefWidth = Math.max(colHeadWidth, dataWidth);
        TableColumn col;
        for (int c = 0; c < colHeader.length; c++) {
            col = colModel.getColumn(c + 1);
            col.setPreferredWidth(prefWidth);
            col.setCellRenderer(rightRenderer);
        }
        return table;
    }

    class MyTableModel extends AbstractTableModel {
        int _rows = rowHeader.length;
        int _cols = colHeader.length + 1;

        MyTableModel() {

        }
        public int getColumnCount() {
            return _cols;
        }

        public int getRowCount() {
            return _rows;
        }

        public String getColumnName(int col) {
            if (col > 0)
                return colHeadFmt.format(colHeader[col - 1]);
            else { // for the row header column
                if (headNameSpecified)
                    return rowHeadName + " \\ " + colHeadName;
                else
                    return "";
            }
        }


        public Object getValueAt(int row, int col) {
            if (col == 0)
                return rowHeadFmt.format(rowHeader[row]);
            else
                return dataFmt.format(data[row][col - 1]);
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        public Class getColumnClass(int c) {
            return String.class;
        }
    }



    void debug(String msg) {
        System.out.println("TwoDTable: " + msg);
    }

    void errMessage(String msg) {
        System.err.println("TwoDTable Error: " + msg);
    }
}