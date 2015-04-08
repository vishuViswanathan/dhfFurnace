package mvUtils.display;

import mvUtils.math.DoublePoint;
import mvUtils.math.DoubleRange;

import javax.swing.*;

import java.io.Serializable;

public interface GraphInfo
        extends Serializable {
    public abstract TraceHeader[] getTraceHeader();

    public abstract TraceHeader getTraceHeader(int trace);

    public abstract DoubleRange getXrange(int trace);

    public abstract DoubleRange getYrange(int trace);

    public abstract DoubleRange getCommonXrange();

    public abstract DoubleRange getCommonYrange();

    public abstract DoubleRange getXrange(int trace, DoubleRange xRange);

    public abstract double getYat(int trace, double x);

    public abstract DoublePoint[] getGraph(int trace);

    public abstract DoubleRange[] getGraph(int trace, double step);

    public abstract DoubleRange[] getGraph(int trace, DoubleRange xRange);

    public abstract DoubleRange[] getGraph(int trace,
                                           DoubleRange xRange, double step);

    public abstract DoublePoint[] getReactions();

    public abstract String[] getTabularResults(int trace);

    public abstract int traceCount();

    public abstract JTable getResultTable();

    public String getYFormat(int trace);

    public String getXFormat();

    public String getXFormat(int trace);

}