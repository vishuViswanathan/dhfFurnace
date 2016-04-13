package performance.stripFce;

/**
 * User: M Viswanathan
 * Date: 11-Apr-16
 * Time: 11:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class StripAction {
    public double speed;
    public double maxSpeed;
    public double temperature;

    public StripAction(double speed, double maxSpeed, double temperature) {
        set(speed, maxSpeed, temperature);
    }

    public StripAction set(double speed, double maxSpeed, double temperature) {
        this.speed = speed;
        this.maxSpeed = maxSpeed;
        this.temperature = temperature;
        return this;
    }
}
