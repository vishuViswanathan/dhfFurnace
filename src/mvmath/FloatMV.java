package mvmath;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 05-Mar-15
 * Time: 12:43 PM
 * To change this template use File | Settings | File Templates.
 */

public class FloatMV {
    float val;

    public FloatMV(float val) {
        this.val = val;
    }

    public FloatMV() {
        this(0);
    }

    public void addVal(float addVal) {
        val += addVal;
    }

    public float val() {
        return val;
    }

    static public float round(float val, int decimals) {
        float factor = (float)Math.pow(10.0, decimals);
        return Math.round(val * factor) / factor;
    }
}
