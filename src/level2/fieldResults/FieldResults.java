package level2.fieldResults;

import FceElements.heatExchanger.HeatExchProps;
import basic.FuelFiring;
import basic.ProductionData;
import directFiredHeating.FceSection;
import level2.L2DFHFurnace;
import mvXML.ValAndPos;
import mvXML.XMLmv;
import performance.stripFce.OneZone;

import java.text.DecimalFormat;
import java.util.Vector;

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
    double chTempIn;
    double chTempOut;
    double flueTempOut;
    public double commonAirTemp;
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
    public boolean compareResults() {
        boolean retVal = false;
        // the field data
        if (!inError) {
            double netHeatFromFuel = totFuel() * fuelFiring.netUsefulFromFuel(flueTempOut);
            double heatToCharge = production.totalChargeHeat();
            double frTotalLosses = netHeatFromFuel - heatToCharge;
            double calculLosses = l2Furnace.totLosses;
            double lossFactor = frTotalLosses / calculLosses;
            FieldZone[] zones = topZones;
            // @TODO considering uniform loss correction factor now
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
