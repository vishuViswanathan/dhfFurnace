package performance.stripFce;

/**
 * User: M Viswanathan
 * Date: 11-Apr-16
 * Time: 11:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class StripProcessAndSize {
    public String process;
    public double width;
    public double thickness;
    public Performance refP;

    public StripProcessAndSize(String process, double width, double thickness) {
        set(process, width, thickness);
    }

    public StripProcessAndSize set(String process, double width, double thickness) {
        this.process = process;
        this.width = width;
        this.thickness = thickness;
        return this;
    }

    public String toString() {
        return "Strip " + width + " x " + thickness + " for process " + process;
    }
}
