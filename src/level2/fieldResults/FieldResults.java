package level2.fieldResults;

import FceElements.heatExchanger.HeatExchProps;
import basic.*;
import directFiredHeating.DFHTuningParams;
import directFiredHeating.FceSection;
import level2.*;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import performance.stripFce.OneZone;

import java.text.DecimalFormat;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 18-Mar-15
 * Time: 2:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class FieldResults {
    L2DFHFurnace l2Furnace;
    public ProductionData production;
    FuelFiring fuelFiring;
    double flueTempOut;
    public double commonAirTemp;
    public double flueAtRecu;
    public double commonFuelTemp;
    HeatExchProps airHeatExchProps;
    public boolean inError = false;
    public String errMsg = "FieldResults:";
    FieldZone[] topZones;
    FieldZone[] botZones;
    double totalFuel = 0;
    static DecimalFormat fmtTemp = new DecimalFormat("0.0");
    static DecimalFormat fmtFuelFlow = new DecimalFormat("0.00");

//    Vector<FieldZone> zones;

    public FieldResults(L2DFHFurnace l2Furnace) {
        this.l2Furnace = l2Furnace;
        topZones = new FieldZone[l2Furnace.nTopActiveSecs];
        if (l2Furnace.bTopBot)
            topZones = new FieldZone[l2Furnace.nBotActiveSecs];
    }

    public void takeFromCalculations() {
        production = new ProductionData(l2Furnace.production);
//        setCommonData(l2Furnace.chTempIN, l2Furnace.chTempOUT, l2Furnace.flueTempOUT, l2Furnace.commonAirTemp);
        setCommonData(l2Furnace.flueTempOUT, l2Furnace.commonAirTemp, l2Furnace.commFuelFiring.fuelTemp);
        FceSection oneSec;
        OneZone oneZone;
        int nSec = l2Furnace.nTopActiveSecs;
        for (int z = 0; z < nSec; z++) {
            oneSec = l2Furnace.getOneSection(false, z);
            oneZone = oneSec.getZonePerfData();
            addZoneResult(false, z, oneZone.fceTemp, oneZone.fuelFlow, l2Furnace.commonAirTemp, 1.0);
            // @TODO the air fuel ratio is taken as 1.0 here in createOneFieldResults()
        }
        if (l2Furnace.bTopBot) {
            nSec = l2Furnace.nBotActiveSecs;
            for (int z = 0; z < nSec; z++) {
                oneSec = l2Furnace.getOneSection(true, z);
                oneZone = oneSec.getZonePerfData();
                addZoneResult(false, z, oneZone.fceTemp, oneZone.fuelFlow, l2Furnace.commonAirTemp, 1.0);
                // @TODO the air fuel ratio is taken as 1.0 here in createOneFieldResults()
            }
        }
        airHeatExchProps = l2Furnace.getAirHeatExchProps();
    }

    public FieldResults(L2DFHFurnace l2Furnace, String xmlStr) {
        this(l2Furnace);
        if (!takeFromXML(xmlStr)) {
            inError = true;
        }
    }

    public FieldResults(L2DFHFurnace l2Furnace, boolean fromLevel1) {  // TODO to be used
        this(l2Furnace);
        inError = true;
        if (takeZonalData()) {
            if (takeRecuData(l2Furnace.getRecuperatorZ())) {
                if (takeStripData(l2Furnace.getStripZone())) {
                    if (takeCommonDFHData(l2Furnace.getCommonDFHZ())) {
                        inError = false;
                    }
                }
            }
        }
    }

    boolean takeStripData(L2Zone stripZone) {
        boolean retVal = false;
        double stripExitT = stripZone.getValue(L2ParamGroup.Parameter.Temperature, Tag.TagName.PV).floatValue;
        double width = stripZone.getValue(L2ParamGroup.Parameter.Data, Tag.TagName.Width).floatValue / 1000; // m
        double thick = stripZone.getValue(L2ParamGroup.Parameter.Data, Tag.TagName.Thick).floatValue / 1000; // m
        double speed = stripZone.getValue(L2ParamGroup.Parameter.Speed, Tag.TagName.PV).floatValue * 60; // m/h
        double output = 7.85 * width * speed * thick * 1000; // kg/h
        String forProcess = stripZone.getValue(L2ParamGroup.Parameter.Data, Tag.TagName.Process).stringValue;
        OneStripDFHProcess stripDFHProc = l2Furnace.l2DFHeating.getStripDFHProcess(forProcess);
        if (stripDFHProc != null) {
            production = new ProductionData();
            ChMaterial chMat = stripDFHProc.getChMaterial(forProcess, thick);
            if (chMat != null) {
                Charge ch = new Charge(stripDFHProc.getChMaterial(forProcess, thick), width, 1.0, thick, 0.1, Charge.ChType.SOLID_RECTANGLE);
                production.charge = ch;
                production.chPitch = 1.0;
                production.production = output;
                production.exitTemp = stripExitT;
                production.exitZoneFceTemp = topZones[topZones.length - 1].frFceTemp;
                production.minExitZoneTemp = stripDFHProc.getMinExitZoneTemp();
                DFHTuningParams tune = l2Furnace.tuningParams;
                tune.setPerfTurndownSettings(stripDFHProc.minOutputFactor(), stripDFHProc.minWidthFactor());
                retVal = true;
            }
            else
                l2Furnace.l2DFHeating.showError("Could not ascertain Charge Material for " +
                        forProcess + " with strip Thickness " + thick);
        }
        else
            l2Furnace.l2DFHeating.showError("Could not ascertain Process data " + forProcess);
        return retVal;
    }

    boolean takeRecuData(L2Zone recu) {
        commonAirTemp = recu.getValue(L2ParamGroup.Parameter.AirFlow, Tag.TagName.Temperature).floatValue;
        commonFuelTemp = 30; // TODO  commonFuelTemp set as 30
        double excessAir = 0.05; // TODO excess air for fuelFiring is taken as 5%
        flueAtRecu = recu.getValue(L2ParamGroup.Parameter.Flue, Tag.TagName.Temperature).floatValue;
        fuelFiring = l2Furnace.getFuelFiring(false, excessAir, commonAirTemp, commonFuelTemp);
        return true;
    }

    boolean takeCommonDFHData(L2Zone dfhZ) {
        flueTempOut = dfhZ.getValue(L2ParamGroup.Parameter.Flue, Tag.TagName.Temperature).floatValue;
        return true;
    }

    boolean takeZonalData() {
        boolean retVal = true;
        FieldZone oneFieldZone;

        for (int z = 0; z < l2Furnace.nTopActiveSecs; z++) {
            oneFieldZone = new FieldZone(l2Furnace, false, z, l2Furnace.getOneL2Zone(z, false));
            if (oneFieldZone.bValid)
                topZones[z] = oneFieldZone;
            else {
                l2Furnace.l2DFHeating.showError("Problem in reading field performance data for Zone " + (z + 1) + ": " +
                        oneFieldZone.errMsg);
                retVal = false;
                break;
            }
        }
        return retVal;
    }

    public void setCommonData(double flueTempOut, double airTemp,  double fuelTemp) {
        this.flueTempOut = flueTempOut;
        this.commonAirTemp = airTemp;
        this.commonFuelTemp = fuelTemp;
    }

    public void addZoneResult(boolean bBot, int zNum, double fceTemp, double fuelFlow, double airTemp, double afRatio) {
        getZones(bBot)[zNum] = new FieldZone(l2Furnace, bBot, zNum, fceTemp, fuelFlow, airTemp, afRatio);
    }

    /**
     * Compares the calculation results with the Field data and works our the lossFactor to be applied to
     * the calculated data
     */

    boolean compareResults() {   // TODO Losses in each Zone is adjusted for
        boolean retVal = false;
        // the field data
        if (!inError) {
            FlowAndTemperature passingFlue = new FlowAndTemperature();
            FieldZone zone;
            for (int z = topZones.length - 1; z >= 0; z--)
                topZones[z].compareResults(passingFlue);
            if (l2Furnace.bTopBot) {
                passingFlue.reset();
                for (int z = botZones.length - 1; z >= 0; z--)
                    botZones[z].compareResults(passingFlue);
            }
            retVal = true;
        }
        return retVal;
    }

    boolean compareResultsOLD() {  // @TODO OLD option with uniform correction
        boolean retVal = false;
        // the field data
        if (!inError) {
            double netHeatFromFuel = totFuel() * fuelFiring.netUsefulFromFuel(flueTempOut);
            double heatToCharge = production.totalChargeHeat();
            double frTotalLosses = netHeatFromFuel - heatToCharge;
            double calculLosses = l2Furnace.totLosses;
            double lossFactor = frTotalLosses / calculLosses;
            FieldZone[] zones = topZones;
            for (FieldZone z: zones)
                z.lossFactor = lossFactor;
            if (l2Furnace.bTopBot) {
                zones = botZones;
                for (FieldZone z : zones)
                    z.lossFactor = lossFactor;
            }
            return true;
        }
        return retVal;
    }



    /**
     * Make necessary corrections in calculation based on field results
     * @return
     */
    public boolean adjustForFieldResults() {
        if (compareResults()) {
            FieldZone[] zones = topZones;
            for (FieldZone z: zones)
                z.sec.setLossFactor(z.lossFactor);
            if (l2Furnace.bTopBot) {
                zones = botZones;
                for (FieldZone z : zones)
                    z.sec.setLossFactor(z.lossFactor);
            }
            return true;
        }
        else
            return false;
    }

    public boolean takeFromXML(String xmlStr) {
        boolean bRetVal = true;
        ValAndPos vp;
        blk:   {
            vp = XMLmv.getTag(xmlStr, "productionData", 0);
            production = new ProductionData(l2Furnace.controller, vp.val);
            if (production.inError) {
                bRetVal = false;
                errMsg += production.errMsg;
                break blk;
            }
            vp = XMLmv.getTag(xmlStr, "fuelFiring", 0);
            fuelFiring = new FuelFiring(l2Furnace.controller, vp.val);
            if (fuelFiring.inError) {
                bRetVal = false;
                errMsg += fuelFiring.errMsg;
                break blk;
            }
            if (!production.inError) {
                try {
                    vp = XMLmv.getTag(xmlStr, "flueTempOut", 0);
                    flueTempOut = Double.valueOf(vp.val);
                    vp = XMLmv.getTag(xmlStr, "commonAirTemp", 0);
                    commonAirTemp = Double.valueOf(vp.val);
                    vp = XMLmv.getTag(xmlStr, "commonFuelTemp", 0);
                    commonFuelTemp = Double.valueOf(vp.val);
                    vp = XMLmv.getTag(xmlStr, "bTopBot", vp.endPos);
                    boolean bTopBot = vp.val.equals("1");
                    if (bTopBot != l2Furnace.bTopBot) {
                        inError = true;
                        errMsg += "Furnace Heating mode does not match ";
                        bRetVal = false;
                        break blk;
                    }
                    vp = XMLmv.getTag(xmlStr, "topZones", vp.endPos);
                    bRetVal = takeZonesFromXML(false, vp.val);
                    if (bRetVal) {
                        if (bTopBot) {
                            vp = XMLmv.getTag(xmlStr, "botZones", vp.endPos);
                            bRetVal = takeZonesFromXML(false, vp.val);
                        }
                        if (bRetVal) {
                            totFuel();
                            vp = XMLmv.getTag(xmlStr, "airHeatExchProps", vp.endPos);
                            if (vp.val.length() > 0) {
                                airHeatExchProps = new HeatExchProps();
                                if (!airHeatExchProps.takeDataFromXML(vp.val, fuelFiring, totalFuel)) {
                                    bRetVal = false;
                                    errMsg += "Reading air recuperator data";
                                    break blk;
                                }
                            }
                        }
                    }
                } catch(NumberFormatException e){
                    errMsg += "in Reading XML";
                    bRetVal = false;
                }
            }
        }
        return bRetVal;
    }

    double totFuel() {
        totalFuel = totFuel(false);
        if (l2Furnace.bTopBot)
            totalFuel += totFuel(true);
        return totalFuel;
    }

    public StringBuilder dataInXML() {
        StringBuilder xmlStr =
                new StringBuilder(XMLmv.putTag("productionData", l2Furnace.production.dataInXML()));
        xmlStr.append(XMLmv.putTag("fuelFiring", l2Furnace.commFuelFiring.dataInXML()));
        xmlStr.append(XMLmv.putTag("flueTempOut", fmtTemp.format(flueTempOut)));
        xmlStr.append(XMLmv.putTag("commonAirTemp", fmtTemp.format(commonAirTemp)));
        xmlStr.append(XMLmv.putTag("commonFuelTemp", fmtTemp.format(commonFuelTemp)));
        xmlStr.append(XMLmv.putTag("bTopBot", (l2Furnace.bTopBot) ? "1" : "0"));
        xmlStr.append(XMLmv.putTag("topZones", zonesInXML(false)));
        if (l2Furnace.bTopBot)
            xmlStr.append(XMLmv.putTag("botZones", zonesInXML(true)));
        if (airHeatExchProps != null)
            xmlStr.append(XMLmv.putTag("airHeatExhProps", airHeatExchProps.dataInXML()));
        return xmlStr;
    }

    FieldZone[] getZones(boolean bBot) {
        return (bBot) ? botZones : topZones;
    }

    StringBuilder zonesInXML(boolean bBot) {
        FieldZone[] zones = getZones(bBot);
        StringBuilder xmlStr = new StringBuilder(XMLmv.putTag("nZones", zones.length));
        for (int z = 0; z < zones.length; z++)
            xmlStr.append(XMLmv.putTag("frZ#" + ("" + z).trim(), zones[z].dataInXML()));
        return xmlStr;
    }

    double totFuel(boolean bBot)  {
        double totFuel = 0;
        FieldZone[] zones = getZones(bBot);
        for (FieldZone oneZone: zones)
            totFuel += oneZone.frFuelFlow;
        return totFuel;
    }

    boolean takeZonesFromXML(boolean bBot, String xmlStr) throws NumberFormatException {
        boolean retVal = true;
        FieldZone[] zones = getZones(bBot);
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "nZones", 0);
        int nZones = Integer.valueOf(vp.val);
        if (nZones == ((bBot) ? l2Furnace.nBotActiveSecs : l2Furnace.nTopActiveSecs)) {
            for (int z = 0; z < nZones; z++) {
                vp = XMLmv.getTag(xmlStr, "frZ#" + ("" + z).trim(), vp.endPos);
                zones[z] = new FieldZone(l2Furnace, bBot, z, vp.val);
                if (!zones[z].bValid) {
                    inError = true;
                    errMsg += zones[z].errMsg;
                    retVal = false;
                    break;
                }
            }
        }
        else {
            inError = true;
            errMsg += "Number of " + ((bBot) ? "Bottom" : "Top") + " zones does not match";
            retVal = false;
        }
        return retVal;
    }
}
