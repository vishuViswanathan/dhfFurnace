package basic;

import java.util.*;
import java.io.*;

/**
 * interpolatable table with on column of reference
 * and multible data columns. the ref column data must be
 * not repeat.  The data rows are inserted based on
 * the ref value. The columns are referred by 0 based column
 * indexes.
 */
public class InterpolTable {
    int nDataColumns;
    String colNames[];
    TreeSet table; // of array of columns
    TwoDTable finalTable; // finally this is created
    // from the TreeSet
    boolean ready; // finalTable is ready

    /**
     * @param colNames length must be nDataCumns + 1
     *                 the index 0 is the ref column nane
     */
    public InterpolTable(int nDataColumns, String[] colNames) {
        this(nDataColumns);
        setColumnNames(colNames);
    }

    public InterpolTable(int nDataColumns) {
        this.nDataColumns = nDataColumns;
        table = new TreeSet(new RefComparator());
        setDefaultNames();
    }

    void setColumnNames(String[] names) {
        for (int n = 0; n < names.length &&
                n < (nDataColumns + 1); n++)
            colNames[n] = names[n];
    }

    void setDefaultNames() {
        colNames = new String[nDataColumns + 1];
        colNames[0] = "Ref";
        for (int n = 0; n < nDataColumns; n++)
            colNames[n + 1] = "Data#" + n;
    }

    /**
     * data inserted based on the value of ref
     * data.length must be =  nDataColumns
     */
    public boolean add(double ref, double[] data) {
        if (data.length != nDataColumns) {
            throw new IllegalArgumentException("data Size Error");
        }
        double[] combined = new double[nDataColumns + 1];
        combined[0] = ref;
        for (int n = 0; n < nDataColumns; n++)
            combined[n + 1] = data[n];
        return add(combined);
    }

    /**
     * refAndData[0] is assumed to be the ref
     * and refAndData.length must be = nDataColumns +1
     * data inserted based on the value of ref
     */
    public boolean add(double[] refAndData) {
        if (refAndData.length != nDataColumns + 1) {
            throw new IllegalArgumentException("refAndData Size Error");
        }
        ready = false;
        return table.add(refAndData);
    }

    public double[] getFirstData() {
        return (double[]) table.first();
    }

    public double[] getNextData(double[] ref) {
        Iterator iter = table.iterator();
        boolean found = false;
        double[] element = null;
        RefComparator comparator = new RefComparator();
        while (iter.hasNext()) {
            element = (double[]) iter.next();
            if (comparator.compare(element, ref) > 0) {
                found = true;
                break;
            }
        }
        if (found)
            return element;
        else
            return null;
    }

    public boolean remove(double ref) {
        Iterator iter = table.iterator();
        boolean found = false;
        double[] element = null;
        while (iter.hasNext()) {
            element = (double[]) iter.next();
            if (element[0] == ref) {
                found = true;
                break;
            }
        }
        if (found) {
            ready = false;
            return table.remove(element);
        } else
            return false;
    }

    public Object clone() {
        String[] names = new String[nDataColumns + 1];
        for (int n = 0; n < nDataColumns + 1; n++)
            names[n] = colNames[n];
        InterpolTable newIP = new InterpolTable(nDataColumns, names);
        Iterator iter = table.iterator();
        while (iter.hasNext()) {
            newIP.add((double[]) iter.next());
        }
        return newIP;
    }

    /**
     * all details have been entered and the table
     * to be finalized for use
     */
    boolean thatsIt() {
        ready = false;
        finalTable =
                new TwoDTable(nDataColumns, table.size(), false, false);
        while (finalTable != null) { // just for breaking out
            boolean allOK = true;
            // set column heads
            double[] colHead = new double[nDataColumns];
            for (int n = 0; n < nDataColumns; n++)
                colHead[n] = n; // set the column numbers as column heads
            if (!finalTable.setColHeader(colHead))
                break;
            // set row heads
            double[] rowHead = new double[table.size()];
            Iterator iter = table.iterator();
            double[] data;
            int row = 0;
            while (iter.hasNext()) {
                data = (double[]) iter.next();
                rowHead[row++] = data[0];
            }
            if (!finalTable.setRowHeader(rowHead))
                break;
            // set Data
            iter = table.iterator();
            double[] rowData = new double[nDataColumns];
            row = 0;
            while (iter.hasNext()) {
                data = (double[]) iter.next();
                for (int n = 0; n < nDataColumns; n++)
                    rowData[n] = data[n + 1];
                if (!finalTable.setOneRow(rowHead[row++], rowData)) {
                    allOK = false;
                    break;
                }
            }
            if (allOK)
                ready = true;
            break;
        }
        return ready;
    }

    /**
     * @param dataNum is 0 based index of data columns
     */
    double getDataAtRef(double ref, int dataNum) {
        double value = Double.NaN;
        try {
            if (ready)
                value = finalTable.getData(dataNum, ref, true);
        } catch (Exception e) {
            value = Double.NaN;
        }
        return value;
    }

    public void printData() {
        PrintStream ps = System.out;
        double[] data = null;
        ps.println("from table");
        Iterator iter = table.iterator();
        while (iter.hasNext()) {
            data = (double[]) iter.next();
            ps.println("Data: " + data[0] + ",   " + data[1]);
        }
        ps.println();
        finalTable.printData();
    }

    class RefComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if (!(o1 instanceof double[]) ||
                    !(o2 instanceof double[])) {
                throw new ClassCastException("Not double[]");
            }
            double v1 = ((double[]) o1)[0];
            double v2 = ((double[]) o2)[0];
            if (v2 > v1)
                return -1;
            if (v2 == v1)
                return 0;
            else
                return 1;
        }

        public boolean equals(Object o) {
            if (o instanceof RefComparator)
                return true;
            else
                return false;
        }
    }

}

