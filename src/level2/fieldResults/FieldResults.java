package level2.fieldResults;

import FceElements.heatExchanger.HeatExchProps;
import basic.*;
import directFiredHeating.DFHTuningParams;
import directFiredHeating.FceSection;
import level2.*;
import level2.common.L2ParamGroup;
import level2.common.Tag;
import mvUtils.display.*;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import performance.stripFce.OneZone;

import javax.swing.*;
import java.awt.*;
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
    OneStripDFHProcess stripDFHProc;
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
        errMsg = "";
        if (takeZonalData()) {
            if (takeRecuData(l2Furnace.getRecuperatorZ())) {
                ErrorStatAndMsg stripResponse = takeStripData(l2Furnace.getStripZone());
                if (stripResponse.inError) {
                    errMsg += stripResponse.msg;
                }
                else {
                    if (takeCommonDFHData(l2Furnace.getCommonDFHZ())) {
                        inError = false;
                    }
                    else
                        errMsg += ", Some problem in reading DFH common data";
                }
            }
        }
    }

    ErrorStatAndMsg takeStripData(L2Zone stripZone) {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg();
        double stripExitT = stripZone.getValue(L2ParamGroup.Parameter.Temperature, Tag.TagName.PV).floatValue;
        double width = stripZone.getValue(L2ParamGroup.Parameter.Now, Tag.TagName.Width).floatValue / 1000; // m
        double thick = stripZone.getValue(L2ParamGroup.Parameter.Now, Tag.TagName.Thick).floatValue / 1000; // m
        double speed = stripZone.getValue(L2ParamGroup.Parameter.Speed, Tag.TagName.PV).floatValue * 60; // m/h
        double output = 7.85 * width * speed * thick * 1000; // kg/h
        String forProcess = stripZone.getValue(L2ParamGroup.Parameter.Now, Tag.TagName.Process).stringValue;
        stripDFHProc = l2Furnace.l2DFHeating.getStripDFHProcess(forProcess);
        if (stripDFHProc != null) {
            if (l2Furnace.isRefPerformanceAvailable(stripDFHProc, thick)) {
                production = new ProductionData(stripDFHProc.processName);
                ChMaterial chMat = stripDFHProc.getChMaterial(thick);
                if (chMat != null) {
                    Charge ch = new Charge(chMat, width, 1.0, thick, 0.1, Charge.ChType.SOLID_RECTANGLE);
                    production.charge = ch;
                    production.chPitch = 1.0;
                    production.production = output;
                    production.exitTemp = stripExitT;
                    production.exitZoneFceTemp = topZones[topZones.length - 1].frFceTemp;
                    production.minExitZoneTemp = stripDFHProc.getMinExitZoneTemp();
                    DFHTuningParams tune = l2Furnace.tuningParams;
                    tune.setPerfTurndownSettings(stripDFHProc.minOutputFactor(), stripDFHProc.minWidthFactor());
                } else
                    retVal.addErrorMsg("Could not ascertain Charge Material for " +
                            forProcess + " with strip Thickness " + thick);
            }
            else
                retVal.addErrorMsg("Reference performance is NOT available");
        } else
            retVal.addErrorMsg("Could not ascertain Process data " + forProcess);
        return retVal;
    }

    public ErrorStatAndMsg processOkForFieldResults() {
        return stripDFHProc.fieldDataOkForProcess(stripDFHProc.processName, production);
    }

    public void copyTempAtTCtoSection() {
        for (FieldZone z: topZones)
            z.copyTempAtTCtoSection();
        if (l2Furnace.bTopBot) {
            for (FieldZone z: botZones)
                z.copyTempAtTCtoSection();
        }
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

    public void setCommonData(double flueTempOut, double airTemp, double fuelTemp) {
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
            for (FieldZone z : zones)
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
     *
     * @return
     */
    public boolean adjustForFieldResults() {
        if (compareResults()) {
            FieldZone[] zones = topZones;
            for (FieldZone z : zones)
                z.sec.setLossFactor(z.lossFactor);
            if (l2Furnace.bTopBot) {
                zones = botZones;
                for (FieldZone z : zones)
                    z.sec.setLossFactor(z.lossFactor);
            }
            return true;
        } else {
            l2Furnace.logInfo("compareResults returned false");
            return false;
        }
    }

    public boolean takeFromXML(String xmlStr) {
        boolean bRetVal = true;
        ValAndPos vp;
        blk:
        {
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
                } catch (NumberFormatException e) {
                    errMsg += "in Reading XML";
                    bRetVal = false;
                }
            }
        }
        return bRetVal;
    }

    public double totFuel() {
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

    double totFuel(boolean bBot) {
        double totFuel = 0;
        FieldZone[] zones = getZones(bBot);
        for (FieldZone oneZone : zones)
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
        } else {
            inError = true;
            errMsg += "Number of " + ((bBot) ? "Bottom" : "Top") + " zones does not match";
            retVal = false;
        }
        return retVal;
    }

    public boolean getDataFromUser() {
        FieldResultsDlg dlg = new FieldResultsDlg(true);
        dlg.setLocation(10, 50);
        dlg.setVisible(true);
        return true;
    }

    class FieldResultsDlg extends JDialog {
        DFHTuningParams.ForProcess proc;
        InputControl ipc;
        DataListEditorPanel editorPanel;
        boolean editable = false;
        EditResponse.Response response;
        JComboBox<ChMaterial> cbChMaterial;
        NumberTextField ntChWidth;
        NumberTextField ntChLength;
        NumberTextField ntChThickness;
        NumberTextField ntChDia;
        JComboBox<Charge.ChType> cbChType;
        NumberTextField ntBottomShadow;
        NumberTextField ntChPitch;

        NumberTextField ntProduction;
        NumberTextField ntEntryTemp;
        NumberTextField ntExitTemp;
        NumberTextField ntDeltaTemp;
        NumberTextField ntChRows;

        NumberTextField ntDischZoneFceTemp;
        NumberTextField ntMinDischZoneTemp;

        JComboBox<Fuel> cbFuel;
        NumberTextField ntExcessAir;
        NumberTextField ntAirTemp;
        NumberTextField ntFuelTemp;
        JCheckBox chkRegenBurner;

        NumberTextField ntFlueTempOut;
        NumberTextField ntCommonAirTemp;
        NumberTextField ntCommonFuelTemp;

        JCheckBox chkTopBot;

        JCheckBox chkCounterFlow;
        NumberTextField ntHeatingFlowBase;
        NumberTextField ntHeatedFlowBase;
        NumberTextField ntHeatingTinBase;
        NumberTextField ntHeatingToutBase;
        NumberTextField ntHeatedTinBase;
        NumberTextField ntHeatedToutBase;
        NumberTextField ntFFBase;
        NumberTextField ntHTaBase;

        int nZones;

        FieldResultsDlg(boolean editable) {
            this.editable = editable;
            setModal(true);
            proc = l2Furnace.l2DFHeating.proc;
            init();
        }


        void init() {
            JPanel outerP = new FramedPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            outerP.add(chargePanel(), gbc);
            gbc.gridx++;
            outerP.add(chInFcePanel(), gbc);

            gbc.gridy++;
            gbc.gridx = 0;
            gbc.gridwidth = 3;
            JPanel zonalP = new FramedPanel();
            zonalP.add(FieldZone.getRowHeader());
            for (FieldZone z : topZones)
                zonalP.add(z.dataPanel(ipc, editable));
            outerP.add(zonalP, gbc);
            add(outerP);
            pack();
        }

        public void setVisible(boolean ena) {
            if (ena && (proc != DFHTuningParams.ForProcess.STRIP))
                l2Furnace.l2DFHeating.showError("This is available only for STRIP Furnace at the moment");
            else
                super.setVisible(ena);
        }

        JPanel chargePanel() {
            JPanel outerP = new FramedPanel();
            MultiPairColPanel mp = new MultiPairColPanel("Charge Details");
            cbChMaterial = new JComboBox<ChMaterial>(l2Furnace.l2DFHeating.vChMaterial);
            Charge ch = production.charge;
            ntChLength = new NumberTextField(ipc,ch.getLength() * 1000, 6, false, 300, 6000, "#,###", "Strip Width (mm)");
//            NumberTextField ntChLength;
            ntChThickness =new NumberTextField(ipc,ch.getHeight() * 1000, 6, false, 0.05, 50, "#,###.00", "Strip Thickness (mm)");
//            NumberTextField ntChDia;
//            JComboBox<Charge.ChType> cbChType;
            mp.addItemPair(cbChMaterial);
            mp.addItemPair(ntChLength);
            mp.addItemPair(ntChThickness);
            outerP.add(mp);
            return outerP;
        }

        JPanel chInFcePanel() {
            JPanel outerP = new FramedPanel();
            MultiPairColPanel mp = new MultiPairColPanel("Charge In Furnace");
//            NumberTextField ntBottomShadow;
//            NumberTextField ntChPitch;

            ntProduction = new NumberTextField(ipc, production.production / 1000, 6, false, 0.5, 500, "#,###.00", "Production (t/h)");
            ntEntryTemp = new NumberTextField(ipc, production.entryTemp, 6, false, 0, 1000, "#,###", "Strip Entry Temperature (C)");
            ntExitTemp = new NumberTextField(ipc, production.exitTemp, 6, false, 200, 1500, "#,###", "Strip Exit temperature (C)");
//            NumberTextField ntDeltaTemp;
//            NumberTextField ntChRows;

            ntDischZoneFceTemp = new NumberTextField(ipc, production.exitZoneFceTemp, 6, false, 300, 2000, "#,###", "Exit Zone Furnace Temperature (C)");
            ntMinDischZoneTemp = new NumberTextField(ipc, production.minExitZoneTemp, 6, false, 300, 2000, "#,###", "Minimum Exit Zone Temperature (C)");
            mp.addItemPair(ntProduction);
            mp.addBlank();
            mp.addItemPair(ntEntryTemp);
            mp.addItemPair(ntExitTemp);
            mp.addBlank();
            mp.addItemPair(ntDischZoneFceTemp);
            mp.addItemPair(ntMinDischZoneTemp);
            outerP.add(mp);
            return outerP;
        }

        EditResponse.Response getResponse() {
            return response;
        }

        public ErrorStatAndMsg checkData() {
            return new ErrorStatAndMsg(true, "Not Ready ro check");
        }

        ErrorStatAndMsg checkZoneData(int zNum) {
            return new ErrorStatAndMsg(true, "Not Ready to check Zonal");
        }

        public boolean saveData() {
            return false;
        }

        public void deleteData() {
        }

        public void cancel() {
        }
    }

}
