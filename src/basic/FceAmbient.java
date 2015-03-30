package basic;

import java.text.DecimalFormat;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 1/25/13
 * Time: 2:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class FceAmbient {
    public double gasTemp;
    public double startTime;
    public double alpha;
    DecimalFormat formatTemp = new DecimalFormat("##0.0");
    DecimalFormat formatTime = new DecimalFormat("##0.000000");
    DecimalFormat formatAlpha = new DecimalFormat("##0.000");

    public FceAmbient(double startTime, double gasTemp, double alpha) {
        this.gasTemp = gasTemp;
        this.startTime = startTime;
        this.alpha = alpha;
    }
    public String ambString(double factor) {
        return "" + formatTime.format(startTime) + "," + formatTemp.format(gasTemp) + "," + formatAlpha.format(alpha * factor);
    }

    public String avgAmbString(FceAmbient addAmb, double factor) {
        double avgAlpha = (alpha + addAmb.alpha) / 2;
        double avgGasTemp = (gasTemp + addAmb.gasTemp) / 2;
        return "" + formatTime.format(startTime) + "," + formatTemp.format(avgGasTemp) + ","  + formatAlpha.format(avgAlpha * factor);
    }

    public String ambString() {
        return ambString(1) ;
    }
}
