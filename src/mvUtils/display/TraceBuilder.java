package mvUtils.display;

import mvUtils.math.DoublePoint;
import mvUtils.math.DoubleRange;
import mvUtils.math.XYArray;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 10/30/12
 * Time: 12:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class TraceBuilder extends GraphInfoAdapter {
    TraceHeader header;
    int maxLen;
    DoublePoint[] dp;
    NTFChain[][] colData;
    DoubleRange xMaxRange, yMaxRange;
    boolean bForceAscend = true;
    InputControl control;
    NoteDataChanges dataChangesListener;
    double xMin;
    DoubleRange xRange, yRange;

    public TraceBuilder(InputControl control, int maxLen, DoubleRange xMaxRange, DoubleRange yMaxRange, String xFormat, String yFormat) {
        this.maxLen = maxLen;
        this.xMaxRange = xMaxRange;
        this.yMaxRange = yMaxRange;
        this.control = control;
        colData = new NTFChain[maxLen][2];
        NTFChain prevOne = null;
        dataChangesListener = new NoteDataChanges();
        xMin = xMaxRange.min - 1;
        NTFChain ntf;
        for (int r = 0; r < maxLen; r++) {
            ntf = new NTFChain(control, xMaxRange.min - 1, 6, false, xMin, xMaxRange.max, "", "", true, prevOne);
            ntf.addActionListener(dataChangesListener);
            ntf.addFocusListener(dataChangesListener);
            colData[r][0] = ntf;
            prevOne = ntf;
            ntf = new NTFChain(control, yMaxRange.min, 6, false, yMaxRange.min, yMaxRange.max, "", "", true, null);
            ntf.addActionListener(dataChangesListener);
            ntf.addFocusListener(dataChangesListener);
            colData[r][1] = ntf;
        }
        xRange = new DoubleRange(0, 1000);
        yRange = new DoubleRange(0, 1000);
//        prepareGraphPanel();
    }

    public int populate(TraceHeader header, XYArray arr) {
        int retVal = 0;
        initData();
        if (this.header == null)
            this.header = new TraceHeader("", "", "");
        header.copyTo(this.header);
        String xName = "", yName = "";
        String xFormat = "", yFormat = "";
        NumberTextField xF, yF;
        if (header != null) {
            xName = header.xNameWithUnits;
            yName = header.yNameWithUnits;
            xFormat = header.xFormat;
            yFormat = header.yFormat;
        }
        if (arr != null) {
            int len = Math.min(maxLen, arr.arrLen);
            for (int r = 0; r < len; r++) {
                xF = colData[r][0];
                yF = colData[r][1];
                xF.setTitle(xName);
                xF.setFormat(xFormat);
                xF.setData(arr.getXat(r));
                yF.setTitle(yName);
                yF.setFormat(yFormat);
                yF.setData(arr.getYat(r));
            }
        }
        retVal = createDpoints();
        if (gp != null)
            gp.headerChanged();
        if (dataPanel != null)
            dataPanel.updateUI();
        return retVal;
    }

    public NumberTextField[][] getColData() {
        return colData;
    }

    public void enableColDataEdit(boolean bEna) {
        copyPan.setVisible(bEna);
        for (int r = 0; r < maxLen; r++) {
            colData[r][0].setEditable(bEna);
            colData[r][1].setEditable(bEna);
        }
        if (dataTable != null)
            dataTable.setEnabled(bEna);
    }

    JLabel lXname, lYname;
    JTable dataTable;
    TextArea copyText;
    JPanel copyPan = new JPanel(new BorderLayout());
    JButton pbTransfer = new JButton("Transfer to Table");

    public JPanel dataPanel() {
        JPanel colHeader = new JPanel(new GridBagLayout());
        GridBagConstraints gbcH = new GridBagConstraints();
        Insets ins = new Insets(1, 20, 1, 2);
        gbcH.insets = ins;
        gbcH.gridx = 0;
        gbcH.gridy = 0;
        colHeader.add(new JLabel(" "), gbcH);
        gbcH.gridx++;
        lXname = new JLabel("X Values");
        colHeader.add(lXname, gbcH);
        gbcH.gridx++;
        lYname = new JLabel("Y Values");
        colHeader.add(lYname, gbcH);

        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = ins;
        gbc.gridx = 0;
        gbc.gridy = 0;
        for (int r = 0; r < colData.length; r++) {
            gbc.gridx = 0;
            gbc.gridy++;
            jp.add(new JLabel("" + (r + 1)), gbc);
            gbc.gridx++;
            jp.add(colData[r][0], gbc);
            gbc.gridx++;
            jp.add(colData[r][1], gbc);
        }
        JScrollPane sp = new JScrollPane(jp);
        sp.setColumnHeaderView(colHeader);
        sp.setPreferredSize(new Dimension(jp.getPreferredSize().width + 50, 400));
        JScrollBar sb = sp.getVerticalScrollBar();
        sb.setUnitIncrement(lXname.getPreferredSize().height + ins.top + ins.bottom + 4);
        JPanel outerP = new JPanel(new BorderLayout());
        outerP.add(sp, BorderLayout.CENTER);
        copyText = new TextArea("");
        copyText.setPreferredSize(new Dimension(150, 200));
        copyPan.add(new JLabel("Past from Excel below"), BorderLayout.NORTH);
        copyPan.add(copyText, BorderLayout.CENTER);
        pbTransfer.addActionListener(new TransferToTable());
        copyPan.add(pbTransfer, BorderLayout.SOUTH);

        outerP.add(copyPan, BorderLayout.EAST);

        return outerP;
    }

    class TransferToTable implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                String trstring = copyText.getText();
                String rowstring, value;
                NTFChain oneCell;
                int maxRows = colData.length;
                int maxCols = colData[0].length;
                System.out.println("String is:" + trstring);
                StringTokenizer st1 = new StringTokenizer(trstring, "\n");
                for (int i = 0; st1.hasMoreTokens(); i++) {
                    rowstring = st1.nextToken().trim();
//                    if (rowstring.length() < 2)
//                        break;
                    StringTokenizer st2 = new StringTokenizer(rowstring, "\t");
                    for (int j = 0; st2.hasMoreTokens(); j++) {
                        value = (String) st2.nextToken();
                        if (i < maxRows && j < maxCols) {
                            oneCell = colData[i][j];
                            oneCell.setData(Double.valueOf(value));
                            System.out.println("Putting " + value + "at row = " + i + " column = " + j);
                        }
                    }
                }
                createDpoints();
                gp.updateUI();
            } catch (Exception ex) {
                showError("Some problem in converting data to number!");
            }
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    public JPanel dataPanelOLD() {
        JPanel colHeader = new JPanel(new GridBagLayout());
        GridBagConstraints gbcH = new GridBagConstraints();
        Insets ins = new Insets(1, 20, 1, 2);
        gbcH.insets = ins;
        gbcH.gridx = 0;
        gbcH.gridy = 0;
        colHeader.add(new JLabel(" "), gbcH);
        gbcH.gridx++;
        lXname = new JLabel("X Values");
        colHeader.add(lXname, gbcH);
        gbcH.gridx++;
        lYname = new JLabel("Y Values");
        colHeader.add(lYname, gbcH);

        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = ins;
//        gbc.gridx = 1;
//        gbc.gridy = 0;
//        lXname = new JLabel("X Values");
//        jp.add(lXname, gbc);
//        gbc.gridx++;
//        lYname = new JLabel("Y Values");
//        jp.add(lYname, gbc);
        gbc.gridx = 0;
        gbc.gridy = 0;
        for (int r = 0; r < colData.length; r++) {
            gbc.gridx = 0;
            gbc.gridy++;
            jp.add(new JLabel("" + (r + 1)), gbc);
            gbc.gridx++;
            jp.add(colData[r][0], gbc);
            gbc.gridx++;
            jp.add(colData[r][1], gbc);
        }
        JScrollPane sp = new JScrollPane(jp);
        sp.setColumnHeaderView(colHeader);
        sp.setPreferredSize(new Dimension(jp.getPreferredSize().width + 50, 400));
        JScrollBar sb = sp.getVerticalScrollBar();
        sb.setUnitIncrement(lXname.getPreferredSize().height + ins.top + ins.bottom + 4);
        JPanel outerP = new JPanel();
        outerP.add(sp);
        return outerP;
    }

    JScrollPane sp, hp;
    JPanel dataPanel;

    public JPanel dataPanelTable() {
        NumberTextField[][] data = new NumberTextField[maxLen][3];
        String[] header = new String[]{"X Val", "Y Val"};
        NumberTextField slNo;
        for (int r = 0; r < maxLen; r++) {
            slNo = new NumberTextField(control, r + 1, 6, false, 1, 100, "##0", "");
            data[r][0] = slNo;
            data[r][1] = colData[r][0];
            data[r][2] = colData[r][1];
        }
        boolean freezeLeftCol = true;
        dataTable = new NumberTextFieldTable(colData, header, false, dataChangesListener);
        dataTable.setEnabled(false);
        Object[][] rowHead = new Object[maxLen][1];
        String[] rowHeadHead = new String[]{"SlNo"};
        for (int r = 0; r < maxLen; r++)
            rowHead[r][0] = r + 1;

        JTable rowHeadT = new JTable(rowHead, rowHeadHead );
        rowHeadT.setEnabled(false);
        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        sp = new JScrollPane(dataTable);
        sp.setPreferredSize(dataTable.getPreferredSize());
        sp.setBackground(SystemColor.lightGray);
        hp = new JScrollPane(rowHeadT);
        hp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        hp.setPreferredSize(rowHeadT.getPreferredSize());
        sp.getVerticalScrollBar().addAdjustmentListener(
                new AdjustmentListener() {
                    public void adjustmentValueChanged(AdjustmentEvent e) {
                        hp.getVerticalScrollBar().setValue(((JScrollBar)(e.getSource())).getValue());
                        //To change body of implemented methods use File | Settings | File Templates.
                    }
                });
        jp.add(hp, gbc);
            gbc.gridx++;
        jp.add(sp, gbc);
        jp.updateUI();
        dataPanel = jp;
        return jp;
    }


    GraphPanel gp;

    public void prepareGraphPanel() {
        gp = new GraphPanel(new Dimension(400, 400), GraphDisplay.CURSVALPANALATBOTTOM);
        gp.addTrace(this, 0, Color.BLUE);
        gp.setTraceToShow(0);   // first trace
        gp.prepareDisplay();
        gp.updateUI();
    }

    public JPanel graphPanel() {
        prepareGraphPanel();
        return gp;
    }


    public boolean setValue(int loc, double xVal, double yVal) {
        if (loc >= 0 && loc <= maxLen) {
            colData[loc][0].setData(xVal);
            colData[loc][1].setData(yVal);
        }
        return true;
    }

    public void initData() {
        for (int r = 0; r < maxLen; r++) {
            colData[r][0].setData(xMaxRange.min - 1);
            colData[r][1].setData(yMaxRange.min);
        }
        dp = null;
        if (gp != null)
            gp.updateUI();
    }

    public String dataAsString() {
        String propStr = "";
        if (dataReady()) {
            for (int r = 0; r < dp.length; r++)
                propStr += ((r > 0) ? ", " : "") + dp[r].x + ", " + dp[r].y;
        }
        return propStr;
    }

    boolean dataReady() {
        boolean bRetVal = true;
        if (dp != null && dp.length > 0) {
            if (dp[0].x > xMin) {
                double val;
                for (int r = 0; r < dp.length; r++) {
                    val = dp[r].x;
                    if (val <= xMin)
                        break;
                    for (int x = 0; x < r; x++) {
                        if (dp[x].x == val) {
                            showError("Duplicate X value " + val);
                            bRetVal = false;
                            break;
                        }
                    }
                    if (!bRetVal)
                        break;
                }
            } else {
                showError("First Entry missing!");
                bRetVal = false;
            }
        } else {
            showError("Incomplete Data!");
            bRetVal = false;
        }
        return bRetVal;
    }

    boolean inErrorCheck = false;

    boolean checkDataSequence(NTFChain nF) {
        NTFChain prevOne = nF.prevOne;
        NTFChain nextOne = nF.nextOne;
        double nowVal = nF.getData();
        boolean bInError = false;
        if (bForceAscend && !inErrorCheck && nowVal > xMin) {
            inErrorCheck = true;
            if (prevOne != null) {
                if (prevOne.getData() > nowVal) {
                    bInError = true;
                    showError("Value here cannot be less than " + prevOne.getText());
                }
            }
            if (nextOne != null) {
                double nextVal = nextOne.getData();
                if (nextVal > xMin)
                    if (nextVal < nowVal) {
                        bInError = true;
                        showError("Value here cannot be greater than " + nextVal);
                    }
            }
            if (bInError)
                nF.resetData();
            else
                createDpoints();
        }
        inErrorCheck = false;
        return bInError;
    }

    int createDpoints() {
        int nDat = 0;
        xRange.reset();
        yRange.reset();
        for (int r = 0; r < maxLen; r++) {
            if (!colData[r][0].dataOK() || !colData[r][1].dataOK()) {
                nDat = r;
                break;
            }
        }
        if (nDat > 0) {
            dp = new DoublePoint[nDat];
            double xVal, yVal;
            DoubleRange tempXR = new DoubleRange();
            DoubleRange tempYR = new DoubleRange();
            for (int r = 0; r < nDat; r++) {
                xVal = colData[r][0].getData();
                yVal = colData[r][1].getData();
                dp[r] = new DoublePoint(xVal, yVal);
                tempXR.takeVal(xVal);
                tempYR.takeVal(yVal);
            }
            tempXR.copyTo(xRange);
            tempYR.copyTo(yRange);
            xRange.setOnlyEnds();
            yRange.setOnlyEnds();
        } else
            dp = null;
        if (gp != null)
            gp.updateUI();
        return nDat;
    }


    public boolean allOK() {
        return (createDpoints() > 0);
    }

    @Override
    public TraceHeader getTraceHeader(int trace) {
        return header;
    }

    @Override
    public DoubleRange getXrange(int trace) {
        return xRange;    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public DoubleRange getYrange(int trace) {
        return yRange;    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public DoubleRange getCommonXrange() {
        return xRange;    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public DoubleRange getCommonYrange() {
        return yRange;    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public double getYat(int trace, double x) {
        double val;
        if (dp == null)
            return Double.NaN;
        int arrLen = dp.length;
        int iBase = -1;
        for (int i = 0; i < arrLen; i++) {
            if (dp[i].x > x) {
                iBase = i;
                break;
            }
        }
        if (iBase < arrLen && iBase > 0) {
            val = dp[iBase].y - (dp[iBase].y - dp[iBase - 1].y) /
                    (dp[iBase].x - dp[iBase - 1].x) *
                    (dp[iBase].x - x);
        } else {
            if (iBase == 0)
                val = dp[0].y;
            else
                val = dp[arrLen - 1].y;
        }
        return val;
    }

    @Override
    public DoublePoint[] getGraph(int trace) {
        return dp;
    }

    @Override
    public String getXFormat() {
        return header.xFormat;
    }

    @Override
    public String getYFormat(int trace) {
        return header.yFormat;
    }

    void showError(String msg) {
        JOptionPane.showMessageDialog(control.parent(), msg, "ERROR", JOptionPane.ERROR_MESSAGE);
        control.parent().toFront();
    }

    class NoteDataChanges implements ActionListener, FocusListener {
        public void actionPerformed(ActionEvent e) {
            updateDisplay((NTFChain) e.getSource());
        }

        public void focusGained(FocusEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void focusLost(FocusEvent e) {
            updateDisplay((NTFChain) e.getSource());
        }

        void updateDisplay(NTFChain nF) {
            if (nF.isChanged())
                checkDataSequence(nF);
            createDpoints();
        }
    }

    class NTFChain extends NumberTextField {
        NTFChain prevOne, nextOne;

        NTFChain(InputControl controller, double val, int size, boolean onlyInteger, double min, double max, String fmtStr,
                 String title, boolean allowZero, NTFChain prevOne) {
            super(controller, val, size, onlyInteger, min, max, fmtStr, title, allowZero);
            this.prevOne = prevOne;
            if (prevOne != null)
                prevOne.nextOne = this;
        }

        void resetData() {
            super.resetValue();
        }

        public boolean dataOK() {
            double val = getData();
            return (val > min && val <= max);
        }
    }

    class GraphPanel extends FramedPanel {
        final Insets borders = new Insets(2, 2, 2, 2);
        GraphDisplay gDisplay;
        Dimension size;
        Point origin; // in % of graph area

        GraphPanel(Dimension size, int curValPanelLoc) {
            this.size = size;
            setSize(size);
            origin = new Point(0, 0);
            gDisplay = new GraphDisplay(this, origin, null, curValPanelLoc);
        }

        void headerChanged() {
            gDisplay.headerChanged();
        }


        int addTrace(GraphInfo gInfo, int trace, Color color, GraphDisplay.LineStyle lStyle) {
            gDisplay.addTrace(gInfo, trace, color, lStyle);
            return gDisplay.traceCount();
        }

        int addTrace(GraphInfo gInfo, int trace, Color color) {
            return addTrace(gInfo, trace, color, GraphDisplay.LineStyle.NORMAL);
        }

        void setbShowVal(int trace, boolean bShowVal) {
            gDisplay.setbShowVal(trace, bShowVal);
        }

        void prepareDisplay() {
            gDisplay.prepareDisplay();
        }

        public Insets getInsets() {
            return borders;
        }

        public Dimension getMinimumSize() {
            return size;
        }

        public Dimension getPreferredSize() {
            return size;
        }

        public void setTraceToShow(int t) {
            gDisplay.setTraceToShow(t);
//          mainF.repaint();
        }

        public void showGraph() {
            gDisplay.showGraph();
        }
    } // class GraphPanel

    class MyTableModel extends AbstractTableModel {
        public int getRowCount() {
            return maxLen;
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return colData[rowIndex][columnIndex];
        }
    }

}
