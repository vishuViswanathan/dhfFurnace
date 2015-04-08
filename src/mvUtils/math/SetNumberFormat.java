package mvUtils.math;

import java.text.DecimalFormat;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 3/24/12
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class SetNumberFormat {

    public SetNumberFormat(double max) {

    }
    public SetNumberFormat(double max, double min) {

    }

    public static String format(double value, String format) {
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

    public static String format(double value) {
        return format(value, "");
    }



    // The following is incomplete
    public static DecimalFormat getDecimalFormat(double val, int digits){
        String fmtStr = "";
        int aftDec;
        digits = (digits < 4) ? 4:digits;
        if (val > Math.pow(10, digits)) {
            if (digits > 6) {
                aftDec = digits - 4;
                fmtStr = "#.";
                for (int i = 0; i < aftDec; i++)
                    fmtStr += "#";
                fmtStr += "E##";
            }
            else {
                int befDec = (int)Math.log10(val);
                aftDec = digits - befDec;
                fmtStr = "#,###";
                if (aftDec > 0 ) {
                    fmtStr += ".";
                    for (int i = 0; i < aftDec; i++)
                        fmtStr += "#";
                }
            }
        }
        else {
            if (val < Math.pow(10, -digits)){
                if (digits > 6) {
                    aftDec = digits - 4;
                    fmtStr = "#.";
                    for (int i = 0; i < aftDec; i++)
                        fmtStr += "#";
                    fmtStr += "E##";
                }
            }
            else {
                fmtStr = "0.";
                for (int i = 1; i < digits; i++) {
                    fmtStr += "#";
                }
            }
        }
            return new DecimalFormat(fmtStr);
    }
}
