package PropertySetter;

import mvmath.SPECIAL;
import mvmath.XYArray;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 10/11/12
 * Time: 4:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class FuelComponent {
    String idInMaterialCode;
    String elemName;
    String units;
    String elementType;
    double molWeight, calValue, density;
    double hAtoms, cAtoms, oAtoms, sAtoms, nAtoms;
    double o2Required, airRequired;
    double CO2, H2O, N2, SO2, flue;
    XYArray heatContent;

    public FuelComponent(String idInMaterialCode, String elemName, String units, String elementType, double molWeight,
                         double calValue, double density, double hAtoms, double cAtoms, double oAtoms,
                         double sAtoms, double nAtoms, String heatContStr) {
        this.idInMaterialCode = idInMaterialCode;
        this.elemName = elemName;
        this.units = units;
        this.elementType = elementType;
        this.molWeight = molWeight;
        this.calValue = calValue;
        this.density = density;
        this.hAtoms = hAtoms;
        this.cAtoms = cAtoms;
        this.oAtoms = oAtoms;
        this.sAtoms = sAtoms;
        this.nAtoms = nAtoms;
        heatContent = new XYArray(heatContStr);
        findFlue();
    }

    double getO2Required() {
        return o2Required;
    }

    void findO2Required() {
        o2Required = (hAtoms / 2 + cAtoms * 2 - oAtoms + sAtoms * 2) / 2;
        airRequired = o2Required / SPECIAL.o2InAir;
    }

    void findFlue() {
        findO2Required();
        CO2 = cAtoms;
        H2O = hAtoms / 2;
        N2 = nAtoms / 2 + o2Required / SPECIAL.o2InAir * (1 - SPECIAL.o2InAir);
        SO2 = sAtoms;
        flue = CO2 + H2O + N2 + SO2;
    }
}
