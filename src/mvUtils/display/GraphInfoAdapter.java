package mvUtils.display;

import mvUtils.math.DoublePoint;
import mvUtils.math.DoubleRange;

import javax.swing.*;

public class GraphInfoAdapter
        implements GraphInfo {
    public TraceHeader[] getTraceHeader() {
        return null;
    }

    /**
     * @param trace
     * @return
     */
    public TraceHeader getTraceHeader(int trace) {
        return null;
    }

    public DoubleRange getXrange(int trace) {
        return null;
    }

    public DoubleRange getYrange(int trace) {
        return null;
    }

    public DoubleRange getCommonXrange() {
        return null;
    }

    public DoubleRange getCommonYrange() {
        return null;
    }

    public DoubleRange getXrange(int trace, DoubleRange xRange) {
        return null;
    }

    public double getYat(int trace, double x) {

        return Double.NaN;
    }

    public DoublePoint[] getGraph(int trace) {
        return null;
    }

    public DoubleRange[] getGraph(int trace, double step) {
        return null;
    }

    /**
     * @param trace
     * @param xRange
     * @return
     */
    public DoubleRange[] getGraph(int trace, DoubleRange xRange) {
        return null;
    }

    public DoubleRange[] getGraph(int trace,
                                  DoubleRange xRange, double step) {
        return null;
    }

    /**
     * @param trace
     * @return
     */
    public String[] getTabularResults(int trace) {
        return null;
    }

    public JTable getResultTable() {
        return null;
    }

    public DoublePoint[] getReactions() {
        return null;
    }

    public int traceCount() {
        return 0;
    }

    public String getYFormat(int trace) {
        return "";  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getXFormat() {
        return "";  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getXFormat(int trace) {
        return "";  //To change body of implemented methods use File | Settings | File Templates.
    }
}