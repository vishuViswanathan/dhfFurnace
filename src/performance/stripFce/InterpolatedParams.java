package performance.stripFce;

import directFiredHeating.DFHFurnace;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/21/14
 * Time: 5:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class InterpolatedParams extends Performance {
    double stripWidth;
    double outputFactor;
    Performance baseP;

    public InterpolatedParams() {

    }

    public InterpolatedParams(DFHFurnace furnace) {
        super(furnace);
    }

    public void takeFromPerfTable(PerformanceTable refTable, double stripWidth, double outputFactor, double thickness) {
        this.baseP = refTable.baseP;
        this.stripWidth = stripWidth;
        this.outputFactor = outputFactor;
        copyDataFromBaseP(thickness);
    }

    void copyDataFromBaseP(double thickness) {
        this.processName = baseP.processName;
        interpolated = true;
        chMaterial = baseP.chMaterial;
        fuelName = baseP.fuelName;
        fuelName = baseP.fuelName;
        chLength = stripWidth; // * baseP.chLength;
        chWidth = baseP.chWidth;
        chThick = thickness;   // requires attention
        chPitch = baseP.chPitch;
        chWt = baseP.chWt / (baseP.chLength * baseP.chThick) * (chLength * chThick);
        output = outputFactor * stripWidth / baseP.chLength * baseP.output;
        unitOutput = output / stripWidth;
        speed = unitOutput / baseP.unitOutput * baseP.speed;  // this is valid only if the thickness is the same
        piecesPerH = speed / baseP.speed * baseP.piecesPerH;
        chMaterial = baseP.chMaterial;
    }

}
