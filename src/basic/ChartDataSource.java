package basic;

import javax.vecmath.*;
import java.awt.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */

public interface ChartDataSource {
    public Point2d[] getOneChartData(int traceNum);

    public int traceCount();

    public Color traceColor(int traceNum);
}