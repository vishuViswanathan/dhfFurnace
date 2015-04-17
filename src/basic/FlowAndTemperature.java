package basic;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 15-Apr-15
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class FlowAndTemperature {
    public double flow;
    public double temperature;

    public FlowAndTemperature(double flow, double temperature) {
        this.flow = flow;
        this.temperature = temperature;
    }

    public FlowAndTemperature() {
        this(0, 0);
    }

    public void reset() {
        flow = 0;
        temperature = 0;
    }
}
