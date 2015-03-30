package directFiredHeating;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 7/25/12
 * Time: 12:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class CreateUFceReturn {
    public int iLloc, iSlot;
    public double balLCombined, totLength, endTime;

    public CreateUFceReturn(int iLloc, int iSlot, double balLCombined, double totLength, double endTime) {
        this.iLloc = iLloc;
        this.iSlot = iSlot;
        this.balLCombined = balLCombined;
        this.totLength = totLength;
        this.endTime = endTime;
    }
}
