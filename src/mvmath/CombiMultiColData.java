package mvmath;

import display.TrendsPanel;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 8/30/12
 * Time: 5:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class CombiMultiColData extends GraphInfoAdapter {
    MultiColData one, two;
    int cols;

    public CombiMultiColData(MultiColData one, MultiColData two) {
        this.one = one;
        this.two = two;
        cols = one.cols + two.cols;
    }

    public double[][] getDataArray(int[] colList) {
        double[][] dataArr = null;
        boolean allAvailable = true;
        for (int c:colList) {
            if (c >= cols) {
                allAvailable = false;
                break;
            }
        }
        if (allAvailable) {
            int nRows = one.xVals.length;
            double nowX;
            dataArr = new double[nRows][colList.length + 1];
            for (int r = 0; r < nRows; r++) {
                nowX = one.xVals[r];
                dataArr[r][0] = nowX;
                for (int c = 0; c < colList.length; c++) {
                    dataArr[r][c + 1] = getYat(colList[c], nowX);
                }
            }
        }
        return dataArr;
    }

    public int addTraces(TrendsPanel tp, int count) {
        int nowCount = 0;
        VariableDataTrace vdt;
        for (int t = 0;t < one.colData.size(); t++)   {
            vdt = one.colData.get(t);
            if (vdt.isTraceable())
                tp.addTrace(this, nowCount++, vdt.color);
        }
        for (int t = 0;t < two.colData.size(); t++)   {
            vdt = two.colData.get(t);
            if (vdt.isTraceable())
                tp.addTrace(this, nowCount++, vdt.color, GraphDisplay.LineStyle.DASHED);
        }
        return count + nowCount;
    }



    @Override
    public TraceHeader getTraceHeader(int trace) {
        SrcAndCol src = getSrc(trace);
        if (src != null) {
            return src.getTraceHeader();
        }
        return null;
     }

    @Override
    public DoublePoint[] getGraph(int trace) {
        SrcAndCol src = getSrc(trace);
        if (src != null) {
            return src.getGraph();
        }
        return null;
    }

    @Override
    public DoubleRange getXrange(int trace) {
        return getCommonXrange();
    }

    @Override
    public DoubleRange getYrange(int trace) {
        return getCommonYrange();
     }

    @Override
    public DoubleRange getCommonXrange() {
        return one.getCommonXrange();
    }

    @Override
    public DoubleRange getCommonYrange() {
        return one.getCommonYrange();
    }

    @Override
    public double getYat(int trace, double x) {
        SrcAndCol src = getSrc(trace);
        if (src != null) {
            return src.getYat(x);
        }
        return 0;
    }

    SrcAndCol getSrc(int trace) {
        if (trace >= 0 && trace < cols) {
            if (trace < one.cols - 1)
                return new SrcAndCol(one, trace);
            else {
                trace++;  // to skip the time column of 'one'
                return new SrcAndCol(two, trace - one.cols);
            }
        }
        else
            return null;
    }

    SrcAndCol getSrcOLD(int trace) {
        if (trace >= 0 && trace < cols) {
            if (trace < one.cols)
                return new SrcAndCol(one, trace);
            else {
                return new SrcAndCol(two, trace - one.cols);
            }
        }
        else
            return null;
    }

    class SrcAndCol {
        int col;
        MultiColData src;
        SrcAndCol(MultiColData src, int col) {
            this.src = src;
            this.col = col;
        }

        DoublePoint[] getGraph() {
            return src.getGraph(col);
        }

        TraceHeader getTraceHeader() {
            return src.getTraceHeader(col);
        }

        double getYat(double x) {
            return src.getYat(col, x);
        }
     }
}
