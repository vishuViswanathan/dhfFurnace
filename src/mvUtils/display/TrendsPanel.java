package mvUtils.display;

import mvUtils.display.GraphDisplay;
import mvUtils.display.GraphInfo;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 19-Nov-14
 * Time: 2:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrendsPanel extends JPanel {
    final Insets borders = new Insets(2, 2, 2, 2);
    GraphDisplay gDisplay;
    Dimension size;
    Point origin; // in % of graph area

    public TrendsPanel(Dimension size, int curValPanelLoc) {
        this.size = size;
        setSize(size);
        origin = new Point(0, 0);
        gDisplay = new GraphDisplay(this, origin, null, curValPanelLoc);
    }


    public int addTrace(GraphInfo gInfo, int trace, Color color, GraphDisplay.LineStyle lStyle) {
        gDisplay.addTrace(gInfo, trace, color, lStyle);
        return gDisplay.traceCount();
    }

    public int addTrace(GraphInfo gInfo, int trace, Color color) {
        return addTrace(gInfo, trace, color, GraphDisplay.LineStyle.NORMAL);
    }

    public void setbShowVal(int trace, boolean bShowVal) {
        gDisplay.setbShowVal(trace, bShowVal);
    }

    public void prepareDisplay() {
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

