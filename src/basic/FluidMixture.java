package basic;

import mvmath.DoublePoint;
import mvmath.XYArray;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 2/26/13
 * Time: 10:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class FluidMixture extends Fluid {
    XYArray heatCont;

    public FluidMixture() {
        clearIt();
    }

    public FluidMixture(Fluid baseFluid) {
        addFluid(baseFluid, 1);
    }

    private void clearIt() {
        heatCont = null;
    }

    public void addFluid(Fluid baseFluid) {
        clearIt();
        heatCont = new XYArray();
        double temp;
        for (int i = 0; i < 20; i++)  {
            temp = i * 100;
            heatCont.add(new DoublePoint(temp, baseFluid.sensHeatFromTemp(temp)));
        }
    }

    /**
     *
     * @param addedFluid   the fluid to be added
     * @param fract Fraction of the added fluid wrt the existing mixture
     */

    public void addFluid(Fluid addedFluid, double fract) {
        if (fract == 1 && heatCont == null)  {
            heatCont = new XYArray();
            double temp;
            for (int i = 0; i < 20; i++)  {
                temp = i * 100;
                heatCont.add(new DoublePoint(temp, addedFluid.sensHeatFromTemp(temp)));
            }
        }
        else {
            double fractInNewMix = fract / (1 + fract);
            updateHeatCont(addedFluid, fractInNewMix);
        }
    }

    /**
     *
     * @param addedFluid
     * @param fractnInMixture fraction in mixture
     *                        newMixture = (1-fractnInMixture)oldMixture + fractnInMixture * addedFluid
     */
    void updateHeatCont(Fluid addedFluid, double fractnInMixture) {
        XYArray newHeat = new XYArray();
        double temp, heat;
        double oldFract = 1 - fractnInMixture;
        for (int i = 0; i < heatCont.arrLen; i++) {
            temp = heatCont.getXat(i);
            heat = oldFract * heatCont.getYat(i) + fractnInMixture * addedFluid.sensHeatFromTemp(temp);
            newHeat.add(new DoublePoint(temp, heat));
        }
        heatCont = newHeat;
    }

    public double sensHeatFromTemp(double temperature) {
        if (heatCont != null)
            return heatCont.getYat(temperature);
        else
            return Double.NaN;
    }

    public double tempFromSensHeat(double heat) {
        if (heatCont != null)
            return heatCont.getXat(heat);
        else
            return Double.NaN;
    }

}