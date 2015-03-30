package basic;

import directFiredHeating.DFHeating;
import mvXML.ValAndPos;
import mvXML.XMLmv;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 7/30/12
 * Time: 12:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class FuelFiring {
    public Fuel fuel;
    double excessAir;
    public double airTemp;
    public double airHeatPerUfuel;
    public double fuelTemp, fuelTempAdded;
    public FlueComposition flue;
    public double actAFRatio, actFFratio;
    public double fuelSensibleHeat = 0;
    double effCalValNoFlue;
    boolean bRegen = false;
    public String errMsg = "";
    public boolean inError = false;

    public FuelFiring(Fuel fuel, boolean bRegenBurner, double excessAir, double airTemp, double fuelTemp) {
        this.fuel = fuel;
        this.bRegen = bRegenBurner;
        evalFlue(excessAir);
        setTemperatures(airTemp, fuelTemp);
    }

    public FuelFiring(DFHeating dfHeating, String xmlStr) {
        if (!takeFromXML(dfHeating, xmlStr)) {
            inError = true;
            errMsg = "FuelFiring :" + errMsg;
        }
    }

    public FuelFiring(FuelFiring copyFrom, boolean bRegenBurner) {
        this(copyFrom.fuel, bRegenBurner, copyFrom.excessAir, copyFrom.airTemp, copyFrom.fuelTemp);
    }

    public double unitAirFlow() {
        return actAFRatio;
    }

    public double unitFlueFlow() {
        return actFFratio;
    }

    public void setTemperatures(double airTemp, double fuelTemp) {
        this.airTemp = airTemp;
        this.fuelTemp = fuelTemp;
        airHeatPerUfuel = FlueComposition.hContAir.getYat(airTemp) * actAFRatio;
        // fuelTemp is neglected if the fuel is a mix
        fuelSensibleHeat = fuel.sensHeatFromTemp(fuelTemp);
        effCalValNoFlue = fuel.calVal + airHeatPerUfuel + fuelSensibleHeat;
    }

    public double adiabaticFlameT(double airTemp, double fuelTemp) {
        double airSensHeat = flue.airSensHeatFromTemp(airTemp) * actAFRatio;
        double fuelSensHeat = fuel.sensHeatFromTemp(fuelTemp);
        return adiabaticFlameT(airSensHeat + fuelSensHeat);
    }

    /**
     *
     * @param airAndFuelHeat  for unit fuel
     * @return the adiabatic flem temperature
     */
    public double adiabaticFlameT(double airAndFuelHeat) {
        double totUnitHeat = (fuel.calVal + airAndFuelHeat) / actFFratio;
        double adiaTemp = flue.tempFromSensHeat(totUnitHeat);
        if (adiaTemp == 2000) { // requires extrapolation since, may be,  data limit reached
            // find the slope for last 200C
            double temp1 = 1800, temp2 = 2000;
            double h1800 = flue.sensHeatFromTemp(temp1);
            double h2000 = flue.sensHeatFromTemp(temp2);
            double slope = (temp2 - temp1) / (h2000 - h1800);
            adiaTemp = temp1 + (totUnitHeat - h1800) * slope;
        }
        return adiaTemp;
    }

    public void setTemperatures(double airTemp) {
        setTemperatures(airTemp, fuelTemp);
    }


 /*   public void setTemperatures (double airTemp, double baseFuelTemp, double addFuelTemp) {
        this.airTemp = airTemp;
        this.fuelTemp = baseFuelTemp;
        airHeatPerUfuel = FlueComposition.hContAir.getYat(airTemp) * actAFRatio;
        fuelSensibleHeat = fuel.sensibleHeat(baseFuelTemp, addFuelTemp);
        effCalValNoFlue = fuel.calVal + airHeatPerUfuel + fuelSensibleHeat;
    }
*/
    public void evalFlue(double excessAir) {
        this.excessAir = excessAir;
        flue = fuel.getFlue(excessAir);
        actAFRatio = fuel.airFuelRatio * (1 + excessAir);
        actFFratio = fuel.flueFuelRatio + fuel.airFuelRatio * excessAir;
    }

    public double flueHeatPerUFuel(double temp) {
        return flue.sensHeatFromTemp(temp) * actFFratio;
    }

    public double flueHeatPerUFuel(double inTemp, double outTemp) {
        return actFFratio * (flue.sensHeatFromTemp(inTemp) - flue.sensHeatFromTemp(outTemp));
    }

    public double heatForAirPerUFuel(double fromTemp, double toTemp) {
        return actAFRatio * (FlueComposition.hContAir.getYat(toTemp) - FlueComposition.hContAir.getYat(fromTemp));
    }

    public FuelHeatDetails effFuelCalVal(double flueExitTemp) {
//        double flueHeat = flueHeatPerUFuel(flueExitTemp);
//        return new FuelHeatDetails(effCalValNoFlue - flueHeat, airHeatPerUfuel, fuelSensibleHeat, flueHeat);
        double netFuelHeat = netUsefulFromFuel(flueExitTemp);
        return new FuelHeatDetails(netFuelHeat, airHeatPerUfuel, fuelSensibleHeat, netFuelHeat - effCalValNoFlue);
    }

    public double netUsefulFromFuel(double flueExitTemp) {
        return effCalValNoFlue - flueHeatPerUFuel(flueExitTemp);
    }

    public double unitFuelHeatWithAPH() {
        return effCalValNoFlue;
    }

    public FuelNameAndFlow baseFuelNameAndFlow(double baseFlow) {
        if (fuel.bMixedFuel)
            return fuel.baseFuelNameAndFlow(baseFlow);
        else
            return new FuelNameAndFlow(fuel, fuelTemp, baseFlow);
    }

    public FuelNameAndFlow addedFuelNameAndFlow(double baseFlow) {
        return fuel.addedFuelNameAndFlow(baseFlow);
    }

    public Vector<Fuel> addUniqueFuels(Vector<Fuel> uniqueFuels) {
        if (!uniqueFuels.contains(fuel)) {
            uniqueFuels.add(fuel);
            if (fuel.bMixedFuel) {
                if (!uniqueFuels.contains(fuel.baseFuel))
                    uniqueFuels.add(fuel.baseFuel);
                if (!uniqueFuels.contains(fuel.addedFuel))
                    uniqueFuels.add(fuel.addedFuel);
            }
        }
        return uniqueFuels;
    }

    public void addFuelUsage(FuelsAndUsage fuelsAndUsage, double flow, boolean bSplitIfMixed) {
        double fuelFlow, airFlow, airSensible, fuelSensible;

//        fuelFlow = flow;
//        airFlow = flow * actAFRatio;
//        airSensible = fuelFlow * airHeatPerUfuel;
//        fuelSensible = fuelFlow * fuelSensibleHeat;
//        fuelsAndUsage.addFuel(fuel, fuelTemp, fuelFlow, airFlow, airSensible, fuelSensible);

        if (bSplitIfMixed && fuel.bMixedFuel) {
            Fuel baseF = fuel.baseFuel;
            fuelFlow = flow;
            airFlow = flow * baseF.airFuelRatio * (1 + excessAir);
            airSensible = airFlow *FlueComposition.hContAir.getYat(airTemp);
            fuelSensible = fuelFlow * baseF.sensHeatFromTemp(fuel.baseFuelTemp);
            fuelsAndUsage.addFuel(baseF, bRegen, fuel.baseFuelTemp, fuelFlow, airFlow, airSensible, fuelSensible);
            Fuel addF = fuel.addedFuel;
            fuelFlow = flow * fuel.fractAddedFuel;
            airFlow = fuelFlow * addF.airFuelRatio * (1 + excessAir);
            airSensible = airFlow *FlueComposition.hContAir.getYat(airTemp);
            fuelSensible = fuelFlow * addF.sensHeatFromTemp(fuel.addFuelTemp);
            fuelsAndUsage.addFuel(addF, bRegen, fuel.addFuelTemp, fuelFlow, airFlow, airSensible, fuelSensible);
//            fuelsAndUsage.addFuel(fuel, 0, 0, 0, 0, 0);
        }
        else {
            fuelFlow = flow;
            airFlow = flow * actAFRatio;
            airSensible = fuelFlow * airHeatPerUfuel;
            fuelSensible = fuelFlow * fuelSensibleHeat;
            fuelsAndUsage.addFuel(fuel, bRegen, fuelTemp, fuelFlow, airFlow, airSensible, fuelSensible);
        }
    }

    boolean takeFromXML(DFHeating dfHeating, String xmlStr) {
        boolean retVal = true;
        ValAndPos vp;
        blk:
        {
            try {
                vp = XMLmv.getTag(xmlStr, "fuel");
                fuel = dfHeating.getSelFuel(vp.val);
                if (fuel != null) {
                    vp = XMLmv.getTag(xmlStr, "excessAir");
                    excessAir = Double.valueOf(vp.val);
                    vp = XMLmv.getTag(xmlStr, "airTemp");
                    airTemp = Double.valueOf(vp.val);
                    vp = XMLmv.getTag(xmlStr, "fuelTemp");
                    fuelTemp = Double.valueOf(vp.val);
                    vp = XMLmv.getTag(xmlStr, "bRegen");
                    bRegen = (vp.val.equals("1"));
                    evalFlue(excessAir);
                    setTemperatures(airTemp, fuelTemp);
                } else {
                    retVal = false;
                    errMsg = "fuel " + vp.val + " not found!";
                    break blk;
                }
            } catch (NumberFormatException e) {

            }
        }
        return retVal;
    }

    public StringBuilder dataInXML() {
        StringBuilder xmlStr = new StringBuilder(XMLmv.putTag("fuel", fuel.toString()));
        xmlStr.append(XMLmv.putTag("excessAir", excessAir));
        xmlStr.append(XMLmv.putTag("airTemp", airTemp));
        xmlStr.append(XMLmv.putTag("fuelTemp", fuelTemp));
        xmlStr.append(XMLmv.putTag("bRegen", (bRegen) ? "1" : "0"));
        return xmlStr;
    }

}
