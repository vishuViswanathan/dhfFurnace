package PropertyViewer;

import mvUtils.display.TraceHeader;
import mvUtils.math.DoublePoint;
import mvUtils.math.DoubleRange;
import mvUtils.math.OnePropertyDet;
import mvUtils.math.XYArray;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 3/18/12
 * Time: 12:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class OnePropertyTrace {
    TraceHeader header;
    DoubleRange xRange;
    DoubleRange yRange;
    OnePropertyDet propX, propY;
    public int length;
    XYArray dataArr = null;

    public OnePropertyTrace() {
    }

    public OnePropertyTrace(TraceHeader tH, String dataPairStrList) {
        this();
        setHeader(tH);
        setData(dataPairStrList);
    }

    public OnePropertyTrace(TraceHeader tH, DoublePoint[] dataPairList) {
        this();
        setHeader(tH);
        setData(dataPairList);
    }

    public void setHeader(TraceHeader tH) {
        header = tH;
    }

    public int setData(String dataPairStrList)    {
        dataArr = new XYArray(dataPairStrList);
        length = dataArr.arrLen;
        return length;
    }

    public int setData(DoublePoint[] dataPairList)    {
        dataArr = new XYArray(dataPairList);
        length = dataArr.arrLen;
        return length;
    }

    public void setRanges(DoubleRange xR, DoubleRange yR) {
        xRange = xR;
        yRange = yR;
    }

    public void setAutoRanges() {
        setAutoXorYrange(true);
        setAutoXorYrange(false);
    }

    public void setAutoXorYrange( boolean x) {  // if true it is x else y
        double vMin, vMax;
        boolean isSmall = false;
        double roundedI  = 0;
        double leftMost;
        double unit;
        double pow = 0;
        if (x) { // for x
            vMin = dataArr.getXmin();
            vMax = dataArr.getXmax();
            xRange = getAutoRange(vMin, vMax);
        }
        else {
            vMin = dataArr.getYmin();
            vMax = dataArr.getYmax();
            yRange = getAutoRange(vMin, vMax);
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

    public double getXmin() {
        return dataArr.getXmin();
    }

    public double getXmax() {
        return dataArr.getXmax();
    }

    public double getYmin() {
         return dataArr.getYmin();
     }

     public double getYmax() {
         return dataArr.getYmax();
     }


    public double getYat(double x) {
        // NOT ready Yet
        return dataArr.getYat(x);
//       return Double.NaN;
    }

    public DoublePoint[] getGraph() {
        return dataArr.getGraph();
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

}
