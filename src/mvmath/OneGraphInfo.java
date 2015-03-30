package mvmath;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 3/18/12
 * Time: 12:32 PM
 * To change this template use File | Settings | File Templates.
 */
public interface OneGraphInfo {
  public abstract TraceHeader getTraceHeader();

  public abstract DoubleRange getXrange();

  public abstract DoubleRange getYrange();

  public abstract double getYat(double x);

  public abstract DoublePoint[] getGraph();

  public abstract DoubleRange[] getGraph(double step);

}