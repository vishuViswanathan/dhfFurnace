package mvUtils.display;

import mvUtils.math.DoublePoint;
import mvUtils.math.DoubleRange;
import mvUtils.math.OnePropertyDet;
import mvUtils.math.XYArray;

import java.awt.*;
import java.text.DecimalFormat;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 8/3/12
 * Time: 10:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class VariableDataTrace {
    TraceHeader header;
    DoubleRange xRange;
    DoubleRange yRange;
    OnePropertyDet propX, propY;
    public int length;
    DoublePoint[] dp;
    boolean bFixedYRange = false, bFixedXRange = false;
    public String xFormat = "", yFormat = "";
    public Color color;
    GraphDisplay.LineStyle lineStyle;
    boolean traceable = false;

    public VariableDataTrace(TraceHeader tH, DoublePoint[] dataPairList, Color color, GraphDisplay.LineStyle lineStyle) {
        setHeader(tH);
        setData(dataPairList);
        this.color = color;
        this.lineStyle = lineStyle;
        this.traceable = true;
    }

    public VariableDataTrace(TraceHeader tH, DoublePoint[] dataPairList, Color color) {
        this(tH, dataPairList, color, GraphDisplay.LineStyle.NORMAL);
    }
//    public VariableDataTrace(TraceHeader tH, DoublePoint[] dataPairList) {
//        this(th, dataPairList, Color.WHITE);
//    }

    public VariableDataTrace(String xName, String yName, double[] xVals, String xFormat, String yFormat, Color color,
                             GraphDisplay.LineStyle lineStyle) {
        int rows = xVals.length;
        dp = new DoublePoint[rows];
        xRange = new DoubleRange();
        for (int r = 0; r < rows; r++) {
            dp[r] = new DoublePoint(xVals[r], 0);
            xRange.takeVal(xVals[r]);
        }
        header = new TraceHeader(xName, "", yName, "");
        yRange = new DoubleRange();
        this.xFormat = xFormat;
        this.yFormat = yFormat;
        this.color = color;
        this.lineStyle = lineStyle;
        traceable = true;
    }

    public VariableDataTrace(String xName, String yName, double[] xVals, String xFormat, String yFormat, Color color) {
        this(xName, yName, xVals, xFormat, yFormat, color, GraphDisplay.LineStyle.NORMAL);
    }

    public VariableDataTrace(String xName, String yName, double[] xVals, String xFormat, String yFormat) {
        this(xName, yName, xVals, xFormat, yFormat, Color.BLACK);
        traceable = false;
    }

    public VariableDataTrace(VariableDataTrace copyFrom, GraphDisplay.LineStyle newStyle) {
        this(copyFrom.getHeader(), copyFrom.dp, copyFrom.color, newStyle);
        this.traceable = true;
    }

    public boolean isTraceable() {
        return traceable;
    }

    public boolean setYval(int x, double val) {
        if (x >= 0 && x < dp.length) {
            dp[x].setYval(val);
            if (!bFixedYRange)
                yRange.takeVal(val);
            return true;
        }
        else
            return false;
    }

    public void setHeader(TraceHeader tH) {
        header = tH;
    }

    public void fixXrange(DoubleRange range) {
        bFixedXRange = true;
        xRange = range;
    }

    public void fixYrange(DoubleRange range) {
         bFixedYRange = true;
         yRange = range;
     }

    public int setData(String dataPairStrList)    {
        XYArray xyArr =   new XYArray(dataPairStrList);
        dp = xyArr.getGraph();
        length = dp.length;
        return length;
    }

    public int setData(DoublePoint[] dataPairList)    {
        dp = dataPairList;
        length = dp.length;
        return length;
    }

    public void setRanges(DoubleRange xR, DoubleRange yR) {
        bFixedXRange = true;
        xRange = xR;
        bFixedYRange = true;
        yRange = yR;
    }

    public void setAutoRanges() {
        setAutoXorYrange(true);
        setAutoXorYrange(false);
    }

    public void setAutoXorYrange( boolean x) {  // if true it is x else y
        if (x) { // for x
            xRange = new DoubleRange();
            for (int i = 0; i < dp.length; i++)
                xRange.takeVal(dp[i].x);
            xRange = getAutoRange(xRange.min, xRange.max);
        }
        else {
            yRange = new DoubleRange();
            for (int i = 0; i < dp.length; i++)
                yRange.takeVal(dp[i].y);
            yRange = getAutoRange(yRange.min, yRange.max);
        }
    }

    static public DoubleRange getAutoRange(double vMin, double vMax) {
        return getAutoRange(vMin, vMax, false, false);
    }

    static public DoubleRange getAutoRange(double vMin, double vMax, boolean limitMin, boolean limitMax) {
        double diff = (vMax - vMin) / 20;
        double roundedI  = 0;
        double leftMost;
        double unit;
        double pow = 0;
        if (diff < 1) {
           String diffDStr = ("" + diff).trim();
           int dotLoc = diffDStr.indexOf(".");
//System.out.print("dotLoc = " + dotLoc + "\n");
            char[] arr = diffDStr.toCharArray();
            for (int c = dotLoc + 1; c < arr.length; c++) {
                if (arr[c] != '0') {
                    String valStr = String.copyValueOf(arr, c, 1);
                    leftMost = Integer.valueOf(valStr) + 1;
                    leftMost = Integer.valueOf("" + arr[c]) + 1;
                    roundedI =  leftMost < 2 ? 2 : (leftMost < 5 ? 5: 10);
                    pow = dotLoc - c ;
//System.out.print("leftMost = " + leftMost + ", pow = " + pow + "\n");
                    break;
                }
            }
        }
        else {
            int diffInt = (int)diff;
            String diffIStr = ("" + diffInt).trim();
            leftMost = Double.valueOf(diffIStr.substring(0, 1));
            roundedI =  leftMost < 2 ? 2 : (leftMost < 5 ? 5: 10);
            pow = ("" + (int)diff).trim().length() - 1;
        }
        unit = roundedI * Math.pow(10, pow);
        double rvMax = ((int)(vMax/ unit) + 1)* unit;
        double rvMin = rvMax - ((int)((rvMax - vMin)/ unit) + ((limitMin) ? 0 :1) ) * unit;
        DoubleRange range = new DoubleRange(rvMin, rvMax);
        range.setMajDiv(unit);
        return range;
    }

    public void setProperties(OnePropertyDet pX, OnePropertyDet pY)   {
        propX = pX;
        propY = pY;
    }


    public TraceHeader getTraceHeader() {
        return header;
    }

    public DoubleRange getXrange() {
       return xRange;
    }

    public  DoubleRange getYrange() {
        return yRange;
    }

    public DoublePoint[] getGraph() {
        return dp;
    }
    public TraceHeader getHeader() {
        return header;
    }

    public String getPropertyX() {
        return "" + propX;
    }

    public String getPropertyXname() {
        return propX.name;
    }

    public String getPropertyXunits() {
        return propX.units;
    }

    public String getPropertyY() {
         return "" + propY;
     }
    public String getPropertyYname() {
         return propY.name;
     }

    public String getPropertyYunits() {
        return propY.units;
    }

    public double getYat(double x) {
        double val;
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
        }
        else {
            if (iBase == 0)
                val = dp[0].y;
            else
                val = dp[arrLen - 1].y;
        }
         return val;
    }

    public String getFormatedYat(double x) {
        return formatNumber(getYat(x), yFormat);
    }

    String formatNumber(double value, String format) {
         String fmt = format;
        if (fmt.length() <= 0) {
             double absVal = Math.abs(value);
             if (absVal == 0)
                 fmt = "#";
             else if (absVal < 0.001 || absVal > 1e5)
                 fmt = "#.###E00";
             else if (absVal > 100)
                 fmt = "###,###";
             else
                 fmt = "###.###";
        }
         return new DecimalFormat(fmt).format(value);

     }

}
