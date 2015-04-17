package basic;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 2/22/13
 * Time: 12:01 PM
 * For all Temperature Properties
 */
public class Fluid {
    double spHt;
    public Fluid() {
        spHt = 0.32;
    }

    public Fluid (Double spHt) {
        this.spHt = spHt;
    }

    public double sensHeatFromTemp(double temperature) {
        return temperature * spHt;
    }

    public double deltaHeat(double fromTemp, double toTemp) {
        return spHt * (fromTemp - toTemp);
    }

    public double tempFromSensHeat(double heat) {
        return heat / spHt;
    }

    /**
     *
     * @param addedFluid   the fluid to be added
     * @param fract Fraction of the added fluid wrt the main fluid
     * After adding the fluid mixture is created
     *
     */

    public void addFluid(Fluid addedFluid, double fract) {
    }
}
