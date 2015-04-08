package mvUtils.math;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 3/24/12
 * Time: 2:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class PosAndValue {
    public int pos;
    public double val;
    SetNumberFormat formatter;
    public PosAndValue(int pos, double val) {
        this.pos = pos;
        this.val = val;
    }

    public void noteFormatter(SetNumberFormat nF) {
        formatter = nF;
    }

    public String toString() {
        return formatter.format(val);
    }
}
