package display;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
//import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 2/8/13
 * Time: 11:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class NumberTextFieldTable extends JTable implements ActionListener {
    NumberTextField[][] data;
    String[] header;
    int nCols, nRows;
    String rowstring, value;
//    Clipboard clipboard;
    StringSelection stsel;
    boolean freezeLeftCol = false;
    boolean bCanEdit;

    public NumberTextFieldTable(NumberTextField[][] data, String[] header, boolean freezeLeftCol, ActionListener li) {
        super(data, header);

        this.data = data;
        this.header = header;
        nRows = data.length;
        nCols = data[0].length;
        setModel(new MyAdvTableModel());
        setupForCopyPaste();
        actLi = new Vector<ActionListener>();
        actLi.add(li);
        this.freezeLeftCol = freezeLeftCol;
        getSelectionModel().addListSelectionListener(new mySelection());

    }

    public void setEditable(boolean bCanEdit) {
        this.bCanEdit = bCanEdit;
        setEnabled(bCanEdit);
    }

    class mySelection implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            int cols[] = getSelectedColumns();
            int len = cols.length;
            switch (len) {
                case 0:
                    break;
                case 1:
                    if (cols[0] == 0)
                        clearSelection();
                    break;
                default:
                    if (cols[0] == 0)
                        setColumnSelectionInterval(cols[1], cols[len - 1]);
                    break;
            }
        }
    }

    void setupForCopyPaste() {
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK, false);
        // Identifying the copy KeyStroke user can modify this
        // to copy on some other Key combination.
        KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK, false);
        // Identifying the Paste KeyStroke user can modify this
        //to copy on some other Key combination.

        registerKeyboardAction(this, "Copy", copy, JComponent.WHEN_FOCUSED);
        registerKeyboardAction(this, "Paste", paste, JComponent.WHEN_FOCUSED);
//        clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    class MyAdvTableModel extends AbstractTableModel {
        MyAdvTableModel() {
        }

        public int getColumnCount() {
            return nCols;
        }

        public String getColumnName(int col) {
            if (checkCol(col))
                return header[col];
            else
                return "";
        }

        boolean checkCol(int col) {
            return ((col >= 0) && (col < nCols));
        }

        boolean checkRow(int row) {
            return (row >= 0 && row < nRows);

        }

        public int getRowCount() {
            return nRows;
        }

        public Object getValueAt(int row, int col) {
            String retVal = "";
            if (checkCol(col) && checkRow(row))
                retVal = data[row][col].getText();
            return retVal;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (checkCol(columnIndex) && checkRow(rowIndex)) {
                double val;
                try {
                    val = Double.valueOf(("" + aValue));
                    NumberTextField oneDat = data[rowIndex][columnIndex];
                    oneDat.setData(val);
                    informChange(oneDat);
                } catch (NumberFormatException e) {
                    // e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }

        public boolean isCellEditable(int row, int col) {
            if (col > 0 || !freezeLeftCol)
                return true;
            else
                return false;
        }
    }

    Vector<ActionListener> actLi;

    void informChange(NumberTextField fi) {
        for (ActionListener li : actLi)
            li.actionPerformed(new ActionEvent(fi, 0, ""));
    }

    /**
     * This method is activated on the Keystrokes we are listening to
     * in this implementation. Here it listens for Copy and Paste ActionCommands.
     * Selections comprising non-adjacent cells result in invalid selection and
     * then copy action cannot be performed.
     * Paste is done by aligning the upper left corner of the selection with the
     * 1st element in the current selection of the JTable.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().compareTo("Copy") == 0) {
            StringBuilder sbf = new StringBuilder();
            // Check to ensure we have selected only a contiguous block of
            // cells
            int numcols = getSelectedColumnCount();
            int numrows = getSelectedRowCount();
            if (numcols > 0 && numrows > 0) {
                int[] rowsselected = getSelectedRows();
                int[] colsselected = getSelectedColumns();
                if (!((numrows - 1 == rowsselected[rowsselected.length - 1] - rowsselected[0] &&
                        numrows == rowsselected.length) &&
                        (numcols - 1 == colsselected[colsselected.length - 1] - colsselected[0] &&
                                numcols == colsselected.length))) {
                    JOptionPane.showMessageDialog(null, "Invalid Copy Selection",
                            "Invalid Copy Selection",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                for (int i = 0; i < numrows; i++) {
                    for (int j = 0; j < numcols; j++) {
                        sbf.append(getValueAt(rowsselected[i], colsselected[j]));
                        if (j < numcols - 1) sbf.append("\t");
                    }
                    sbf.append("\n");
                }
                stsel = new StringSelection(sbf.toString());
//                system = Toolkit.getDefaultToolkit().getSystemClipboard();
//                system.setContents(stsel, stsel);
            }
        }
        if (e.getActionCommand().compareTo("Paste") == 0) {
            int numcols = getSelectedColumnCount();
            int numrows = getSelectedRowCount();
            if (numcols > 0 && numrows > 0) {
                System.out.println("Trying to Paste");
                int startRow = (getSelectedRows())[0];
                int startCol = (getSelectedColumns())[0];
                try {
                    String trstring = "";
//                    trstring = (String) (system.getContents(this).getTransferData(DataFlavor.stringFlavor));
                    System.out.println("String is:" + trstring);
                    StringTokenizer st1 = new StringTokenizer(trstring, "\n");
                    for (int i = 0; st1.hasMoreTokens(); i++) {
                        rowstring = st1.nextToken();
                        StringTokenizer st2 = new StringTokenizer(rowstring, "\t");
                        for (int j = 0; st2.hasMoreTokens(); j++) {
                            value = (String) st2.nextToken();
                            if (startRow + i < getRowCount() &&
                                    startCol + j < getColumnCount())
                                setValueAt(value, startRow + i, startCol + j);
                            System.out.println("Putting " + value + "at row = " + startRow + i + " column = " + startCol + j);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                updateUI();
            }
        }
    }
}
