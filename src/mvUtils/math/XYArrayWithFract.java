package mvUtils.math;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 11/5/12
 * Time: 1:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class XYArrayWithFract {
    double fract;
    XYArray array;
    public XYArrayWithFract(XYArray array, double fract) {
        this.array = array;
        this.fract = fract;
    }

    public XYArrayWithFract(XYArrayWithFract[] elements) {  // the first element must have the full x range
        array = null;
        double tot = 0;
        for (int e = 0; e < elements.length; e++)
            tot += elements[e].fract;
        if (Math.abs(tot - 1) < 0.02) {
            XYArrayWithFract firstE =  elements[0];
            array = new XYArray(firstE.array);
            double fr = firstE.fract;
            for (int r = 0; r < array.arrLen; r++)
                array.setYat(r, firstE.array.getYat(r) * fr);
            double xVal;
            for (int r = 0; r < array.arrLen; r++) {
                xVal = array.getXat(r);
                for (int e = 1; e < elements.length; e++)
                    array.addToYat(r, elements[e].fract * elements[e].array.getYat(xVal));
            }
        }
    }

    public String getDataPairStr() {
        return array.getDataPairStr();
    }

    public String getDataPairStr(String fmtX, String fmtY) {
        return array.getDataPairStr(fmtX, fmtY);
    }
}
