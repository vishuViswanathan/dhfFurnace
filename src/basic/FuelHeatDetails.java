package basic;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 9/27/12
 * Time: 10:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class FuelHeatDetails {
    public double effctiveCalVal;
    public double airHeat;
    public double fuelSensHeat;
    public double lossToFlue;

    public FuelHeatDetails(double effctiveCalVal, double airHeat, double fuelSensHeat, double lossToFlue) {
        this.effctiveCalVal = effctiveCalVal;
        this.airHeat = airHeat;
        this.fuelSensHeat = fuelSensHeat;
        this.lossToFlue = lossToFlue;

    }
}
