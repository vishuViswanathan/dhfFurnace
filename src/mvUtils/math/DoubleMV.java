package mvUtils.math;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 8/28/12
 * Time: 1:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class DoubleMV {
    double val;

    public DoubleMV(double val) {
        this.val = val;
    }

    public DoubleMV() {
        this(0);
    }

    public void addVal(double addVal) {
        val += addVal;
    }

    public double val() {
        return val;
    }

    static public double round(double val, int decimals) {
        double factor = Math.pow(10.0, decimals);
        return Math.round(val * factor) / factor;
    }
}
