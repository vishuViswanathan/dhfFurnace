package basic;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 04-Mar-15
 * Time: 1:01 PM
 * To change this template use File | Settings | File Templates.
 * Checks that the value is range and gives above Max or below Min and recommended value as the crossed limit
 */
public class CheckInRange {
    public boolean inRange = false;
    public boolean aboveMax; // if not it is below Min
    public double limitVal;  // the upper or the lower limit which was violated

    public CheckInRange() {
        inRange = true;
    }

    public void maxViolated(double limitVal) {
        this.limitVal = limitVal;
        inRange = false;
        aboveMax = true;
    }

    public void minViolated(double limitVal) {
        this.limitVal = limitVal;
        inRange = false;
        aboveMax = false;
    }
}
