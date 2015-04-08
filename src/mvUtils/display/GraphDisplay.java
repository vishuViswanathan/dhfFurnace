package mvUtils.display;

import mvUtils.math.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * class GraphDisplay
 * Collects data from the GraphInfo interface and displays
 * the trace/traces on a specified Graphics of plot area
 * specified after scaling
 */

public class GraphDisplay {
    public static final int YPOSITVEISUP = 0;
    public static final int YPOSITVEISDOWN = 1;
    public static final int CURSVALPANALNONE = 0;
    public static final int CURSVALPANALATBOTTOM = 1;
    public static final int CURSVALPANALATLEFT = 2;
    public static final int CURSVALPANALATRIGHT = 3;

    public static enum LineStyle {NORMAL, DASHED, DOTTED; }

    GraphInfo allGInfo;
    Vector<OneTrace> traces;
    int numTraces;
    int traceScale = 0; // trace foe which the axes are shown
    int traceToShow = -2; // -1 is all else the trace number
    public static int MAXTRACES = 6;
    public static final Color[] COLORS = {
            Color.blue, Color.red,
            Color.black, Color.gray, Color.orange, Color.cyan};
    BasicCalculData basicData = null;
    JPanel plotPanel;
    GraphPanel graphPanel = new GraphPanel();
    JPanel titlePanel = new FramedPanel(new GridLayout(1, 0)); //new FramedPanel(new GridLayout(1, 0),false);
    Vector<JTextField> cursorVals = new Vector<JTextField>();
    JTextField tfXpos;
    JTextField tfMshiftX;
    JPanel cursorValPanel;
    Rectangle gPlotRect;
    Rectangle graphRect;
    Point origin; // in pecentage of graph area size
    int xMinPos = 70;
    int lastx = xMinPos; // line cursor position
    EventDespatcher frameEventDespatcher;
    Insets graphInset = new Insets(20, 5, 5, 5);
    IntRange xIntRange, yIntRange;
    IntDoubleScaler xCommScale;
    IntDoubleScaler yCommScale;
    Point intOrigin;
    BasicStroke tWidth = new BasicStroke(2F);
    BasicStroke sWidth = new BasicStroke(0F);
    int curValPanelLoc = CURSVALPANALATBOTTOM;
    JLabel lXName;
    Vector <JLabel> vlYName;

    /**
     * only plot size defined. Traces to be added
     * susequently using addTrace()
     *
     * @param origin is specified in % of graph area
     */
    public GraphDisplay(JPanel plotPanel,
                        Point origin,
                        EventDespatcher frameEventDespatcher, int curValPanelLoc) {
        this.plotPanel = plotPanel;
        this.curValPanelLoc = curValPanelLoc;
        vlYName = new Vector<JLabel>();
        if (curValPanelLoc == CURSVALPANALATBOTTOM){
            int rows = (int)((numTraces + 1) / 4);
//            rows = (rows < 1)? 1: rows;
            int cols = 4;
//            if (rows == 1)
//                cols = numTraces;
//            cursorValPanel = new JPanel(new GridLayout(rows, cols));
            cursorValPanel = new JPanel(new GridLayout());
        }
        else {
            cursorValPanel = new JPanel();
            cursorValPanel.setLayout(new GridLayout(0, 1));
        }
        plotPanel.setLayout(new BorderLayout());
        Dimension size = plotPanel.getSize();
        gPlotRect = new Rectangle(graphInset.left, graphInset.top,
                (size.width - graphInset.left - graphInset.right),
                (size.height - graphInset.top - graphInset.bottom));
        this.origin = new Point(origin);
        this.frameEventDespatcher = frameEventDespatcher;

        traces = new Vector<OneTrace>();
//        JPanel pXpos = new JPanel(new GridLayout(0, 2));
//        pXpos.add(new JLabel("At x :"));
        tfXpos = new JTextField(6);
        lXName = new JLabel("X  ");
        addCurtValUI(lXName, tfXpos, Color.black);
        tfMshiftX = new JTextField(6);
/*
        tfXpos.setForeground(Color.white);
        tfXpos.setBackground(Color.lightGray);
        tfXpos.setEditable(false);
        pXpos.add(tfXpos);
        cursorValPanel.add(pXpos);
*/
        numTraces = 0;
        CursorAtMousePos l = new CursorAtMousePos();
        graphPanel.addMouseMotionListener(l);
        graphPanel.addMouseListener(l);
//		frameEventDespatcher.
//					addComponentListener(new FrameComponentListener());
    }

    /**
     * This constructor is used for a single graph 'trace' in
     * graphInfo object.
     *
     * @param
     * @param origin in % of plot area
     */
    public GraphDisplay(GraphInfo graphInfo,
                        int trace,
                        JPanel plotPanel, Point origin,
                        EventDespatcher frameEventDespatcher) {
        this(plotPanel, origin, frameEventDespatcher);
        allGInfo = graphInfo;
        addTrace(graphInfo, trace, Color.black);
//        cursorValPanel.add(tfMshiftX);
    }

    /**
     * Constructor
     * This constructor is used for a set of all graphs available
     * in graphInfo object.
     */
    public GraphDisplay(GraphInfo graphInfo,
                        JPanel plotPanel,
                        Point origin,
                        EventDespatcher frameEventDespatcher) {
        this(plotPanel, origin, frameEventDespatcher);
        allGInfo = graphInfo;
        int nTraces = graphInfo.traceCount();
        for (int trace = 0; trace < nTraces; trace++) {
            addTrace(graphInfo, trace, COLORS[trace % COLORS.length]);
        }
    }
 int nCursorVals = 0;
    void addCurtValUI(JLabel nameL, JTextField textF, Color color) {
        textF.setEditable(false);
//        textF.setForeground(color);
        textF.setBackground(SystemColor.lightGray);
        FramedPanel pan = new FramedPanel(new GridBagLayout());
        nameL.setPreferredSize(new Dimension(120, 20));
        nameL.setForeground(color);
        nameL.setHorizontalAlignment(JLabel.RIGHT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        pan.add(nameL, gbc);
        gbc.gridx++;
        pan.add(textF, gbc);
        pan.setBackground(SystemColor.lightGray);
        nCursorVals++;
        int nRows = nCursorVals / 4;
        if (nRows > 0) {
            GridLayout lay = (GridLayout)cursorValPanel.getLayout();
            lay.setColumns(4);
            lay.setRows(nRows + 1);
        }
        cursorValPanel.add(pan);
    }

    void addCurtValUI(String name, JTextField textF, Color color) {
        addCurtValUI(new JLabel(name + "  "), textF, color);
    }


    public GraphDisplay(JPanel plotPanel,
                        Point origin,
                        EventDespatcher frameEventDespatcher) {
        this(plotPanel, origin, frameEventDespatcher, CURSVALPANALATBOTTOM);
    }

    public void setBasicCalculData(BasicCalculData basicData) {
        this.basicData = basicData;
    }

    public void prepareDisplay() {
        setupPanels();
    }

    void setupPanels() {
        plotPanel.add(titlePanel, BorderLayout.NORTH);
        graphPanel.setSize(gPlotRect.width, gPlotRect.height);
        plotPanel.add(graphPanel);
        JPanel outerP = new JPanel();
        outerP.add(cursorValPanel);
        switch(curValPanelLoc) {
            case CURSVALPANALATLEFT:
                plotPanel.add(outerP, BorderLayout.WEST);
                break;
            case CURSVALPANALATRIGHT:
                plotPanel.add(outerP, BorderLayout.EAST);
                break;
            case CURSVALPANALATBOTTOM:
                plotPanel.add(outerP, BorderLayout.SOUTH);
                break;
            default:
                break;
        }
     }

    /**
     * @param graphInfo
     * @param trace
     * @param color
     * @return
     */
    public int addTrace(GraphInfo graphInfo,
                        int trace, Color color) {
         return addTrace(graphInfo, trace, color, LineStyle.NORMAL,  YPOSITVEISUP);
    }

    public int addTrace(GraphInfo graphInfo,
                        int trace, Color color, LineStyle lStyle) {
//        allGInfo = graphInfo;
        return addTrace(graphInfo, trace, color, lStyle, YPOSITVEISUP);
    }

    /**
     * @param graphInfo
     * @param trace
     * @param color
     * @return
     */
    public int addTrace(GraphInfo graphInfo,
                        int trace, Color color, LineStyle lStyle, int mode) {
        allGInfo = graphInfo;
        JTextField tf = new JTextField(6);
        cursorVals.addElement(tf);
        OneTrace oneTrace = new OneTrace(graphInfo, trace, color, lStyle, mode, tf, tfXpos);
        traces.addElement(oneTrace);
        if (oneTrace.traceName.length() > 0) {
            JLabel lName = new JLabel(oneTrace.traceName);
            addCurtValUI(lName, tf, oneTrace.color);
            vlYName.add(lName);
        }
/*
        tf.setForeground(color);
        tf.setBackground(Color.lightGray);
        tf.setEditable(false);
*/
//        cursorValPanel.add(tf);
        numTraces++;
        return numTraces;
    }

    public int addTrace(GraphInfo graphInfo,
                        int trace, Color color, int mode) {
        return addTrace(graphInfo, trace, color, LineStyle.NORMAL,  mode);
    }

     public void setbShowVal(int trace, boolean bShowVal) {
        OneTrace oneT = traces.get(trace);
        if (oneT != null)
            oneT.setbShowVal(bShowVal);
    }

    public int traceCount() {
        return numTraces;
    }

    void setXname() {
        int traceForX = (traceToShow < 0) ? 0 : traceToShow;
        TraceHeader header = traces.get(traceForX).getHeader();
        if (header != null && header.xName.length() > 0)
            lXName.setText(header.xName + "  ");
    }

    void setYnames() {
        String tName;
        OneTrace tr;
        JLabel jL;
        for (int t = 0; t < vlYName.size(); t++) {
            jL = vlYName.get(t);
            tr = traces.get(t);
            tName = tr.getHeader().traceName;
            jL.setText(tName);
        }
    }

    public void headerChanged() {
        setXname();
        setYnames();
    }

    public void setTraceToShow(int toShow) {
        toShow = (toShow < 0) ? -1 : ((toShow < numTraces) ? toShow : traceToShow);
        if (toShow != traceToShow) {
            traceToShow = toShow;
            traceScale = (toShow < 0) ? 0 : toShow;
            setXname();
            graphPanel.repaint();
        }
    }


    /**
     * @param g
     * @param refresh
     */
    public void drawGraph(Graphics g, boolean refresh) {
        int left = xMinPos, right = 30, top = 10, bottom = 40;
        Dimension dim = graphPanel.getSize();
        OneTrace oneTrace;
        graphRect = new Rectangle(left, top,
                dim.width - (left + right),
                dim.height - (top + bottom));
        intOrigin = new Point(
                (int) ((double) graphRect.width * origin.x / 100 + left),
                (int) ((double) graphRect.height * (1 - (double) origin.y / 100)
                        + top));
//      ((Graphics2D) g).setStroke(tWidth);
        if (traceToShow < 0) {
            setCommonGraphSize();
            for (int trace = 0; trace < numTraces; trace++) {
                oneTrace = (OneTrace) traces.elementAt(trace);
//          oneTrace.setGraphSize(graphRect, intOrigin);
                oneTrace.setGraphSize(graphRect, intOrigin, xCommScale, yCommScale);
            }
            g.setColor(Color.black);
            Rectangle r = graphRect;
            if (basicData != null) {
                basicData.drawBasePic(g, graphRect, intOrigin);
            }
//        setCommonGraphSize();
            drawCommonAxes(g);
            for (int trace = 0; trace < numTraces; trace++) {
                oneTrace = (OneTrace) traces.elementAt(trace);
                oneTrace.drawTrace(g, true); //refresh);
            }
        } else {
            oneTrace = (OneTrace) traces.elementAt(traceToShow);
            oneTrace.setGraphSize(graphRect, intOrigin);
            g.setColor(Color.black);
            Rectangle r = graphRect;
            if (basicData != null) {
                basicData.drawBasePic(g, graphRect, intOrigin);
            }
            ((OneTrace) traces.elementAt(traceScale)).drawAxes(g);

            oneTrace.drawTrace(g, true); //refresh);
        }
//      ((Graphics2D) g).setStroke(sWidth);
    }

    void setCommonGraphSize() {
        if (intOrigin != null) {
            xIntRange = new IntRange(graphRect.x,
                    graphRect.x + graphRect.width);
            yIntRange = new IntRange(intOrigin.y, graphRect.y);

        }
        xCommScale = new IntDoubleScaler(xIntRange, allGInfo.getCommonXrange());
        yCommScale = new IntDoubleScaler(yIntRange, allGInfo.getCommonYrange());
//        debug("xCommScale = " + xCommScale.factor + ", yCommScale = " + yCommScale.factor);
    }

    void drawCommonAxes(Graphics g) {
        drawCommonXAxis(g, graphRect, intOrigin);
        drawCommonYAxis(g, graphRect, intOrigin);
    }

    void drawCommonXAxis(Graphics g, Rectangle pR, Point origin) {
        int xStart, yStart, xEnd, yEnd;
        xStart = pR.x;
        yStart = pR.y + pR.height;
        xEnd = pR.x + pR.width;
        yEnd = pR.y;
//     ((Graphics2D) g).setStroke(sWidth);
        g.setColor(Color.gray);

        g.drawLine(xStart, yStart + 1, xEnd, yStart + 1);
        PosAndValue[] scalePoints = xCommScale.getScalePoints();
        PosAndValue onePt;
        FontMetrics fM = g.getFontMetrics();
        int w, h;
        for (int i = 0; i < scalePoints.length; i++) {
            onePt = scalePoints[i];
            g.drawLine(onePt.pos, yStart, onePt.pos, yStart + 5);
            w = fM.stringWidth("" + onePt);
            h = fM.getHeight();
            g.drawString("" + onePt, onePt.pos - w / 2, yStart + 10 + h);
        }
    }

    void drawCommonYAxis(Graphics g, Rectangle pR, Point origin) {
        int xStart, yStart, xEnd, yEnd;
        xStart = pR.x;
        yStart = pR.y + pR.height;
        xEnd = pR.x + pR.width;
        yEnd = pR.y;
//     ((Graphics2D) g).setStroke(sWidth);
        g.setColor(Color.gray);
        g.drawLine(xStart, yStart, xStart, yEnd);

        PosAndValue[] scalePoints = yCommScale.getScalePoints();
        PosAndValue onePt;
        FontMetrics fM = g.getFontMetrics();
        int w, h;
        for (int i = 0; i < scalePoints.length; i++) {
            onePt = scalePoints[i];
            if (i == 0)
                g.drawLine(xStart - 5, onePt.pos + 1, xEnd, onePt.pos + 1);
            else
                g.drawLine(xStart - 5, onePt.pos, xEnd, onePt.pos);

            w = fM.stringWidth("" + onePt);
            h = fM.getHeight();
            g.drawString("" + onePt, xStart - w - 10, onePt.pos + h / 4);
        }

    }

    void drawLineCursor(Graphics g) {
        if (lastx != -1) {
            int fromY = graphRect.y - 2;
            int toY = graphRect.y + graphRect.height + 2;
            ((Graphics2D) g).setStroke(sWidth);
            g.setColor(Color.black);
            if (isAnyTrace())
                g.drawLine(lastx, fromY, lastx, toY);
            setCursorVals(lastx);
        }
    }

    boolean isAnyTrace() {
        boolean bRetVal = true;
        if (traceToShow >= 0)
            bRetVal = (traces.get(traceToShow).nPoints > 1);
        return bRetVal;
    }

    void setCursorVals(int x) {
        OneTrace ot;
        if (traceToShow >= 0)
            ot = (OneTrace) traces.elementAt(traceToShow);
        else
            ot = (OneTrace) traces.elementAt(0);  // multi trace
        ot.showCursorXVal(x);
//        tfXpos.setText(SetNumberFormat.format((double) ot.getXdouble(x),
//                 ot.getXFormat()));

        if (traceToShow >= 0) {     // one trace shown
            ot = (OneTrace) traces.elementAt(traceToShow);
            ot.showCursorYVal(x);
        } else {
            for (int n = 0; n < numTraces; n++) {
                ot = (OneTrace) traces.elementAt(n);
                ot.showCursorYVal(x);
            }
        }
    }

    public void showGraph() {
        graphPanel.repaint(graphRect.x, graphRect.y, graphRect.width + 100, graphRect.height + 2);
    }


    void repaintCursorArea(int x) {
        graphPanel.repaint(x - 2, graphRect.y,
                4, graphRect.height + 2);     // height increased by 2
    }

    class GraphPanel
            extends FramedPanel {

        GraphPanel() {
//			super(false); // lowered panel
//			setInsets(5, 5, 5, 5);
            setBackground(SystemColor.lightGray);
        }

        public void paint(Graphics g) {
            super.paint(g);
            drawGraph(g, false);
            drawLineCursor(g);
        }
    }

    class CursorAtMousePos
            extends MouseAdapter
            implements MouseMotionListener {
        public void mouseDragged(MouseEvent me) {
            setCursorAt(me.getX());
        }

        public void mouseMoved(MouseEvent me) {
        }

        void setCursorAt(int x) {
            if (lastx != -1) {
                repaintCursorArea(lastx);
            }
            if (x >= graphRect.x &&
                    x <= (graphRect.x + graphRect.width)) {
                repaintCursorArea(x);
                lastx = x;
            } else {
                lastx = -1;
            }
        }

        public void mousePressed(MouseEvent me) {
            setCursorAt(me.getX());
        }
    }


    /**
     * @param method
     * @param msg
     */
    void debug(String method, String msg) {
        debug(method + ":" + msg);
    }

    void debug(String msg) {
        System.out.println("\n" + new Date() +
                "\nGraphDisplay:" + msg);
    }


    class OneTrace {
        int graphMode;
        GraphInfo gInfo;
        int traceIngInfo;
        IntRange xPostvIntRange;
        IntRange yPostvIntRange;
        //  IntRange xNegtvIntRange;
//      IntRange yNegtvIntRange;
        DoublePoint[] graph;
        Point[] points;
        Color color;
        int[] xPos;
        int[] yPos;
        int nPoints;
        IntDoubleScaler xScale;
        IntDoubleScaler yScale;
        Rectangle plotRect;
        String traceName = "";
        Point origin;
        JTextField yTextF;
        JTextField xTextF;
        boolean bShowVal = true;
        LineStyle lStyle = LineStyle.NORMAL;
        Stroke lStroke;
        TraceHeader header;

        OneTrace(GraphInfo graphInfo, int trace, Color color, LineStyle lStyle, int mode, JTextField yTextField, JTextField xTextField) {
            gInfo = graphInfo;
            header= gInfo.getTraceHeader(trace);
            if (header != null)
                traceName = gInfo.getTraceHeader(trace).traceName;
            traceIngInfo = trace;
            this.color = color;
            this.lStyle = lStyle;
            setLineStyle();
            this.graphMode = checkMode(mode);
            yTextF = yTextField;
            xTextF = xTextField;
        }

        OneTrace(GraphInfo graphInfo, int trace, Color color, int mode, JTextField yTextField, JTextField xTextField) {
            this(graphInfo, trace, color, LineStyle.NORMAL, mode, yTextField, xTextField);
        }

        TraceHeader getHeader() {
            return header;
        }

        void setLineStyle() {
            switch (lStyle) {
                case DASHED:
                    lStroke = new BasicStroke(2, BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_ROUND, 1.0f, new float[]{4f, 2f},2f);
                    break;
                case DOTTED:
                    lStroke = new BasicStroke(2, BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_ROUND, 1.0f, new float[]{2f, 2f, 2f},2f);
                    break;
            }
        }

         void setbShowVal(boolean bShowVal) {
            this.bShowVal = bShowVal;
        }

        int checkMode(int mode) {
            if (mode == GraphDisplay.YPOSITVEISDOWN) {
                return mode;
            } else {
                return GraphDisplay.YPOSITVEISUP;
            }
        }

        void showCursorYVal(int x) {
            if (bShowVal) {
                if (nPoints < 1)
                    yTextF.setText("NO DATA");
                else
                    yTextF.setText(SetNumberFormat.format((double)getValueAtIntPos(x)));
            }
        }

        void showCursorXVal(int x) {
            if (bShowVal) {
                if (nPoints > 0)
                    xTextF.setText(SetNumberFormat.format((double)getXdouble(x)));
                else
                    xTextF.setText("NO DATA");
            }
        }

        String getXFormat() {
            return gInfo.getXFormat(traceIngInfo);
        }

        String getYFormat() {
            return gInfo.getYFormat(traceIngInfo);
        }

        void setGraphSize(Rectangle plotRect, Point origin) {
            this.plotRect = plotRect;
            if (origin != null) {
                this.origin = new Point(origin);
                xPostvIntRange = new IntRange(plotRect.x,
                        plotRect.x + plotRect.width);
                yPostvIntRange = new IntRange(origin.y, plotRect.y);

            }
            xScale = new IntDoubleScaler(xPostvIntRange, gInfo.getXrange(traceIngInfo));
            yScale = new IntDoubleScaler(yPostvIntRange, gInfo.getYrange(traceIngInfo));
        }

        void setGraphSize(Rectangle plotRect, Point origin, IntDoubleScaler commXscale, IntDoubleScaler commYScale) {
            this.plotRect = plotRect;
            if (origin != null) {
                this.origin = new Point(origin);
                xPostvIntRange = new IntRange(plotRect.x,
                        plotRect.x + plotRect.width);
                yPostvIntRange = new IntRange(origin.y, plotRect.y);

            }
            xScale = commXscale;
            yScale = commYScale;
        }

        void drawTrace(Graphics g, boolean refresh) {
            ((Graphics2D) g).setStroke(tWidth);
            if (refresh || graph == null) {
                getGraph();
            }
            //    drawAxes(g);
            g.clipRect(plotRect.x, plotRect.y, plotRect.width + 1,
                    plotRect.height + 1);
            g.setColor(color);
            if (lStyle != LineStyle.NORMAL)
                ((Graphics2D) g).setStroke(lStroke);
            if (nPoints > 1)
                g.drawPolyline(xPos, yPos, nPoints);
            else if (nPoints == 1)
                g.drawString("One Data", plotRect.width/ 2, plotRect.height / 2 );
            else
                g.drawString("NO DATA", plotRect.width/ 2, plotRect.height / 2 );
            ((Graphics2D) g).setStroke(tWidth);
        }

        public void drawAxes(Graphics g) {
            DoublePoint[] gr = gInfo.getGraph(traceIngInfo);
            if (gr != null && gr.length > 1) {
                drawXscale(g);
                drawYscale(g);
            }
        }

        void drawXscale(Graphics g) {
            int xStart, yStart, xEnd, yEnd;
            xStart = plotRect.x;
            yStart = plotRect.y + plotRect.height;
            xEnd = plotRect.x + plotRect.width;
            yEnd = plotRect.y;
            g.setColor(Color.gray);
            g.drawLine(xStart, yStart, xEnd, yStart);
            //      double factor = xScale.factor();
            PosAndValue[] scalePoints = xScale.getScalePoints();
            PosAndValue onePt;
            FontMetrics fM = g.getFontMetrics();
            int w, h;
            for (int i = 0; i < scalePoints.length; i++) {
                onePt = scalePoints[i];
                g.drawLine(onePt.pos, yStart, onePt.pos, yStart + 5);
                w = fM.stringWidth("" + onePt);
                h = fM.getHeight();
                g.drawString("" + onePt, onePt.pos - w / 2, yStart + 10 + h);
            }
        }

        void drawYscale(Graphics g) {
            int xStart, yStart, xEnd, yEnd;
            xStart = plotRect.x;
            yStart = plotRect.y + plotRect.height;
            xEnd = plotRect.x + plotRect.width;
            yEnd = plotRect.y;
            g.setColor(Color.gray);
            g.drawLine(xStart, yStart, xStart, yEnd);
            //        double factor = yScale.factor();
            PosAndValue[] scalePoints = yScale.getScalePoints();
            PosAndValue onePt;
            FontMetrics fM = g.getFontMetrics();
            int w, h;
            for (int i = 0; i < scalePoints.length; i++) {
                onePt = scalePoints[i];
                g.drawLine(xStart - 5, onePt.pos, xEnd, onePt.pos);
                w = fM.stringWidth("" + onePt);
                h = fM.getHeight();
                g.drawString("" + onePt, xStart - w - 10, onePt.pos + h / 4);
            }
        }

        void getGraph() {
            graph = gInfo.getGraph(traceIngInfo);
            if (graph != null) {
                nPoints = graph.length;
                xPos = new int[nPoints];
                yPos = new int[nPoints];
                for (int p = 0; p < nPoints; p++) {
                    xPos[p] = xScale.intVal(graph[p].x);
                    yPos[p] = yScale.intVal(graph[p].y);
                }
            }
            else
                nPoints = 0;
//debug("traceIngInfo, nPoints = " + traceIngInfo + ", " + nPoints);
        }

        double getValueAtIntPos(int x) {
            double dXval = xScale.doubleVal(x);
            return gInfo.getYat(traceIngInfo, dXval);
        }

        double getXdouble(int x) {
            return xScale.doubleVal(x);

        }
    } // calss OneTrace
}