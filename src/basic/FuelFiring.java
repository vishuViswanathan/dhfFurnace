package basic;

import directFiredHeating.DFHeating;
import mvUtils.math.SPECIAL;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

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
    public boolean o2EnrichedAirUsed = false;
    public double o2inAirFraction = SPECIAL.o2InAir;
    FlueComposition oxydiser;
    public double airTemp;
    public double airHeatPerUfuel;
    public double fuelTemp, fuelTempAdded;
    public FlueComposition flue;
    public double actAFRatio, actFFratio;
    public double stoicAFratio, stoicFFratio;
    public double fuelSensibleHeat = 0;
    double effCalValNoFlue;
    boolean bRegen = false;
    public String errMsg = "";
    public boolean inError = false;

    public FuelFiring(Fuel fuel, boolean bRegenBurner, double excessAir, double airTemp, double fuelTemp) {
        this(fuel, bRegenBurner, excessAir, false, SPECIAL.o2InAir, airTemp, fuelTemp);
     }

    public FuelFiring(Fuel fuel, boolean bRegenBurner, double excessAir, boolean o2EnrichedAirUsed, double o2inAirFraction,
                      double airTemp, double fuelTemp) {
        this.fuel =  fuel ; // new Fuel(fuel);
//        this.fuel.changeForEnrichedAir(o2inAirFraction);
        this.bRegen = bRegenBurner;
        this.o2EnrichedAirUsed = o2EnrichedAirUsed;
        this.o2inAirFraction = o2inAirFraction;
        evalFlue(excessAir);
        oxydiser = new FlueComposition(true, o2inAirFraction );
        setTemperatures(airTemp, fuelTemp);
    }

    public FuelFiring(DFHeating dfHeating, String xmlStr) {
        if (!takeFromXML(dfHeating, xmlStr)) {
            inError = true;
            errMsg = "FuelFiring :" + errMsg;
        }
        oxydiser = new FlueComposition(true, o2inAirFraction );
    }

    public FuelFiring(FuelFiring copyFrom, boolean bRegenBurner) {
        this(copyFrom.fuel, bRegenBurner, copyFrom.excessAir, copyFrom.o2EnrichedAirUsed, copyFrom.o2inAirFraction,
                copyFrom.airTemp, copyFrom.fuelTemp);
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
//        airHeatPerUfuel = FlueComposition.hContAir.getYat(airTemp) * actAFRatio;
        airHeatPerUfuel = oxydiser.sensHeatFromTemp(airTemp) * actAFRatio;
        // fuelTemp is neglcted if the fuel is a mix
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
        FlueComposition stdFlue = fuel.getFlue();
        if (o2EnrichedAirUsed) {
            flue = new FlueComposition(
                    "Flue Of " + fuel.name + " with Excess Air " + (excessAir * 100) + " and o2 in Air " + (o2inAirFraction * 100),
                    fuel, o2inAirFraction, excessAir);
            stoicAFratio = fuel.airFuelRatio * (SPECIAL.o2InAir / o2inAirFraction);
            actAFRatio = stoicAFratio * (1 + excessAir);
            stoicFFratio = flue.stoicFFratio;
            actFFratio = flue.effectiveFFratio;
         }
        else {
            flue = new FlueComposition("Flue Of " + fuel.name + " with Excess Air " + (excessAir * 100),
                    stdFlue, excessAir * fuel.airFuelRatio / fuel.flueFuelRatio);
            stoicAFratio = fuel.airFuelRatio;
            actAFRatio = fuel.airFuelRatio * (1 + excessAir);
            stoicFFratio = fuel.flueFuelRatio;
            actFFratio = fuel.flueFuelRatio + fuel.airFuelRatio * excessAir;

        }
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
// 20150424         return new FuelHeatDetails(netFuelHeat, airHeatPerUfuel, fuelSensibleHeat, netFuelHeat - effCalValNoFlue);
        return new FuelHeatDetails(netFuelHeat, airHeatPerUfuel, fuelSensibleHeat,  effCalValNoFlue - netFuelHeat);
    }

    public double netUsefulFromFuel(double flueExitTemp) {
        return effCalValNoFlue - flueHeatPerUFuel(flueExitTemp);
    }

    public double netUsefulFromFuel(double flueExitTemp, double airPhTemp)  {
        double airHeat = FlueComposition.hContAir.getYat(airPhTemp) * actAFRatio;
        double fuelHeat = fuel.sensHeatFromTemp(fuelTemp);
        return fuel.calVal + airHeat + fuelHeat - flueHeatPerUFuel(flueExitTemp);
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
            Fuel addF = fuel.addedFuel;
            double flowBaseF = flow;
            double flowAddF = flow * fuel.fractAddedFuel;
            double baseFuelStdStoicAirFlow = flowBaseF * baseF.airFuelRatio;
            double addFuelStdStoicAirFlow = flowAddF * addF.airFuelRatio;
            double totalAirFlow = flow * actAFRatio;
            double actualToStdStoiRatio = totalAirFlow / (baseFuelStdStoicAirFlow + addFuelStdStoicAirFlow);
            double totalAirSensible = flow * airHeatPerUfuel;
            double airFlowForBaseF = baseFuelStdStoicAirFlow * actualToStdStoiRatio;
            airSensible = totalAirSensible * airFlowForBaseF / totalAirFlow;
            fuelSensible = flowBaseF * baseF.sensHeatFromTemp(fuel.baseFuelTemp);
            fuelsAndUsage.addFuel(baseF, bRegen, fuel.baseFuelTemp, flowBaseF, airFlowForBaseF, o2inAirFraction,
                    airSensible, fuelSensible);
            double airFlowForAddF =addFuelStdStoicAirFlow * actualToStdStoiRatio;
            airSensible = totalAirSensible * airFlowForAddF / totalAirFlow;
            fuelSensible = flowAddF * addF.sensHeatFromTemp(fuel.addFuelTemp);
            fuelsAndUsage.addFuel(addF, bRegen, fuel.addFuelTemp, flowAddF, airFlowForAddF, o2inAirFraction,
                    airSensible, fuelSensible);

        }
        else {
            fuelFlow = flow;
            airFlow = flow * actAFRatio;
            airSensible = fuelFlow * airHeatPerUfuel;
            fuelSensible = fuelFlow * fuelSensibleHeat;
            fuelsAndUsage.addFuel(fuel, bRegen, fuelTemp, fuelFlow, airFlow, o2inAirFraction, airSensible, fuelSensible);
        }
    }

    boolean takeFromXML(DFHeating dfHeating, String xmlStr) {
        boolean retVal = true;
        ValAndPos vp;
        blk:
        {
            try {
                vp = XMLmv.getTag(xmlStr, "fuel");
                fuel = new Fuel(dfHeating.getSelFuel(vp.val));
                if (fuel != null) {
                    vp = XMLmv.getTag(xmlStr, "excessAir");
                    excessAir = Double.valueOf(vp.val);
                    vp = XMLmv.getTag(xmlStr, "FFo2EnrichedAirUsed");
                    o2EnrichedAirUsed = (vp.val.equals("1"));
                    if (o2EnrichedAirUsed) {
                        vp = XMLmv.getTag(xmlStr, "FFo2inAirFraction");
                        o2inAirFraction = Double.valueOf(vp.val);
                    }
                    else
                        o2inAirFraction = SPECIAL.o2InAir;
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
        xmlStr.append(XMLmv.putTag("FFo2EnrichedAirUsed",  (o2EnrichedAirUsed) ? "1" : "0"));
        xmlStr.append(XMLmv.putTag("FFo2inAirFraction", o2inAirFraction));
        xmlStr.append(XMLmv.putTag("airTemp", airTemp));
        xmlStr.append(XMLmv.putTag("fuelTemp", fuelTemp));
        xmlStr.append(XMLmv.putTag("bRegen", (bRegen) ? "1" : "0"));
        return xmlStr;
    }

}
