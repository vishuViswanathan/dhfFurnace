package mvUtils.math;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 7/26/12
 * Time: 4:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class SPECIAL {
    public static double lmtd(double deltaTa, double deltaTb) {
        double lmtd;
        double denominator = deltaTa / deltaTb;
        if (denominator <= 0) {
            lmtd = (deltaTa + deltaTb) / 2;
        }
        else {
            try {
                lmtd = (deltaTa - deltaTb) / Math.log(deltaTa / deltaTb);
            } catch (Exception e) {
                lmtd = (deltaTa + deltaTb) / 2;
            }
        }
        return lmtd;
    }

    public static final double stefenBoltz = 0.0000000496;
    public static final double o2InAir = 0.21;

    public static double roundToNDecimals(double val, int nDec) {
        double decNum = Math.pow(10, nDec + 1);
        return (double)(Math.round(val * decNum)) / decNum;

    }
}
