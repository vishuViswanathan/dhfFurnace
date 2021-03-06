package level2.fieldResults;

import FceElements.heatExchanger.HeatExchProps;
import TMopcUa.ProcessValue;
import basic.*;
import directFiredHeating.DFHTuningParams;
import level2.common.L2ParamGroup;
import level2.common.Tag;
import level2.stripDFH.L2DFHFurnace;
import directFiredHeating.process.OneStripDFHProcess;
import mvUtils.display.*;
import mvUtils.math.DoubleMV;
import mvUtils.mvXML.XMLmv;

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
    public OneStripDFHProcess stripDFHProc;
    public ProductionData production;
    FuelFiring fuelFiring;
    double flueTempOut;
    public double commonAirTemp;
    public double flueAtRecu;
    public double commonFuelTemp;
    HeatExchProps airHeatExchProps;
    public boolean inError = false;
    public String errMsg = "";
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

    public FieldResults(L2DFHFurnace l2Furnace, boolean withStripData) {
        this(l2Furnace, withStripData, false);
    }

    public FieldResults(L2DFHFurnace l2Furnace, boolean withStripData, boolean allowNewProcess) {
        this(l2Furnace);
        inError = false;
        errMsg = "";
        if (takeZonalData()) {
            if (takeRecuData(l2Furnace.getRecuperatorZ())) {
                if (withStripData) {
                    ErrorStatAndMsg stripResponse = takeStripData(l2Furnace.getStripZone(), allowNewProcess);
                    if (stripResponse.inError) {
                        inError = true;
                        errMsg += stripResponse.msg;
                    }
                }
                if (!inError) {
                    if (!takeCommonDFHData(l2Furnace.getCommonDFHZ())) {
                        inError = true;
                        errMsg += ", Some problem in reading DFH common data from field 01";
                    }
                }
            }
        }
        else {
            inError = true;
            errMsg = "Error in reading Zonal Data. Probably some parameter is out of Range";
        }
    }

    ErrorStatAndMsg takeStripData(L2ParamGroup stripZone, boolean allowNewProcess) {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg();
        ProcessValue pv = stripZone.getValue(L2ParamGroup.Parameter.Temperature, Tag.TagName.PV);
        if (pv.valid) {
            double stripExitT = pv.floatValue;
            double width = DoubleMV.round(stripZone.getValue(L2ParamGroup.Parameter.Now, Tag.TagName.Width).floatValue, 3) / 1000; // m
            double thick = DoubleMV.round(stripZone.getValue(L2ParamGroup.Parameter.Now, Tag.TagName.Thick).floatValue, 3) / 1000; // m
            pv = stripZone.getValue(L2ParamGroup.Parameter.Speed, Tag.TagName.PV);
            if (pv.valid) {
                double speed = pv.floatValue * 60; // m/h
                String forProcess = stripZone.getValue(L2ParamGroup.Parameter.Now, Tag.TagName.BaseProcess).stringValue.trim(); // TODO what happens for Fresh Field Performance
                stripDFHProc = l2Furnace.l2DFHeating.getStripDFHProcess(forProcess, stripExitT, width, thick);
                l2Furnace.logTrace("forProcess " + forProcess + ", " + stripExitT + ", " + width + ", " + thick);
                boolean nowCreated = false;
                if (stripDFHProc == null) {
                    if (allowNewProcess) {
                        if (l2Furnace.l2DFHeating.decide("Data From Field", "Process does not Exist. DO you want to create a new one?")) {
                            DataWithStatus<OneStripDFHProcess> getNew = l2Furnace.createNewNewProcess(stripExitT, thick, width,
                                    speed, forProcess);
                            if (getNew.getStatus() == DataStat.Status.OK) {
                                stripDFHProc = getNew.getValue();
                                nowCreated = true;
                            } else
                                retVal.addErrorMsg(getNew.getErrorMessage());
                        } else
                            retVal.addErrorMsg("Process creation declined");
                    }
                }
                if (!retVal.inError) {
                    if (stripDFHProc != null) {
//                if (l2Furnace.isRefPerformanceAvailable(stripDFHProc, thick) || nowCreated) {
                        if (stripDFHProc.isPerformanceAvailable() || nowCreated) {
                            production = new ProductionData(stripDFHProc.baseProcessName);
                            DataWithStatus<ChMaterial> chMatSat = stripDFHProc.getChMaterial(thick);
                            if (chMatSat.valid) {
                                ChMaterial chMat = chMatSat.getValue();
                                Charge ch = new Charge(chMat, width, 1.0, thick, 0.1, 0, Charge.ChType.SOLID_RECTANGLE);
                                production.charge = ch;
                                production.chPitch = 1.0;
                                production.production = chMat.density * width * speed * thick; //output;
                                production.exitTemp = stripExitT;
                                production.exitZoneFceTemp = topZones[topZones.length - 1].frFceTemp;
                                production.minExitZoneTemp = stripDFHProc.getMinExitZoneTemp();
//                    DFHTuningParams tune = l2Furnace.tuningParams;
//                    tune.setPerfTurndownSettings(stripDFHProc.minOutputFactor(), stripDFHProc.minWidthFactor());
                            } else
                                retVal.addErrorMsg("Could not ascertain Charge Material for " +
                                        forProcess + " with strip Thickness " + thick);
                        } else {
                            retVal.addErrorMsg("Reference performance is NOT available");
                        }
                    } else
                        retVal.addErrorMsg("Could not get matching Process data while taking strip for Field process " + forProcess);
                }
            }
            else
                retVal.addErrorMsg("Field Process - Strip Speed is out of range");
        }
        else
            retVal.addErrorMsg("Field Process - Strip Exit Temperature is out of range");
        return retVal;
    }

    public ErrorStatAndMsg processOkForFieldResults() {
        l2Furnace.logTrace("In processOkForFieldResults");
        return stripDFHProc.productionOkForPerformanceSave(stripDFHProc.baseProcessName, production);
    }

    public void copyTempAtTCtoSection() {
        for (FieldZone z: topZones)
            z.copyTempAtTCtoSection();
        if (l2Furnace.bTopBot) {
            for (FieldZone z: botZones)
                z.copyTempAtTCtoSection();
        }
    }

    boolean takeRecuData(L2ParamGroup recu) {
        commonAirTemp = recu.getValue(L2ParamGroup.Parameter.AirFlow, Tag.TagName.ExitTemp).floatValue;
        commonFuelTemp = 30; // TODO  commonFuelTemp set as 30
        double excessAir = 0.05; // TODO excess air for fuelFiring is taken as 5%
        flueAtRecu = recu.getValue(L2ParamGroup.Parameter.Flue, Tag.TagName.EntryTemp).floatValue;
        fuelFiring = l2Furnace.getFuelFiring(false, excessAir, commonAirTemp, commonFuelTemp);
        return true;
    }

    boolean takeCommonDFHData(L2ParamGroup dfhZ) {
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
//                l2Furnace.l2DFHeating.showError("Problem in reading field data for Zone " + (z + 1) + ": " +
//                        oneFieldZone.errMsg);
                retVal = false;
                break;
            }
        }
        return retVal;
    }

//    public void setCommonData(double flueTempOut, double airTemp, double fuelTemp) {
//        this.flueTempOut = flueTempOut;
//        this.commonAirTemp = airTemp;
//        this.commonFuelTemp = fuelTemp;
//    }

//    public void addZoneResult(boolean bBot, int zNum, double fceTemp, double fuelFlow, double airTemp, double afRatio) {
//        getZones(bBot)[zNum] = new FieldZone(l2Furnace, bBot, zNum, fceTemp, fuelFlow, airTemp, afRatio);
//    }

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

//    public boolean takeFromXML(String xmlStr) {
//        boolean bRetVal = true;
//        ValAndPos vp;
//        blk:
//        {
//            vp = XMLmv.getTag(xmlStr, "productionData", 0);
//            production = new ProductionData(l2Furnace.controller, vp.val);
//            if (production.inError) {
//                bRetVal = false;
//                errMsg += production.errMsg;
//                break blk;
//            }
//            vp = XMLmv.getTag(xmlStr, "fuelFiring", 0);
//            fuelFiring = new FuelFiring(l2Furnace.controller, vp.val);
//            if (fuelFiring.inError) {
//                bRetVal = false;
//                errMsg += fuelFiring.errMsg;
//                break blk;
//            }
//            if (!production.inError) {
//                try {
//                    vp = XMLmv.getTag(xmlStr, "flueTempOut", 0);
//                    flueTempOut = Double.valueOf(vp.val);
//                    vp = XMLmv.getTag(xmlStr, "commonAirTemp", 0);
//                    commonAirTemp = Double.valueOf(vp.val);
//                    vp = XMLmv.getTag(xmlStr, "commonFuelTemp", 0);
//                    commonFuelTemp = Double.valueOf(vp.val);
//                    vp = XMLmv.getTag(xmlStr, "bTopBot", vp.endPos);
//                    boolean bTopBot = vp.val.equals("1");
//                    if (bTopBot != l2Furnace.bTopBot) {
//                        inError = true;
//                        errMsg += "Furnace Heating mode does not match ";
//                        bRetVal = false;
//                        break blk;
//                    }
//                    vp = XMLmv.getTag(xmlStr, "topZones", vp.endPos);
//                    bRetVal = takeZonesFromXML(false, vp.val);
//                    if (bRetVal) {
//                        if (bTopBot) {
//                            vp = XMLmv.getTag(xmlStr, "botZones", vp.endPos);
//                            bRetVal = takeZonesFromXML(false, vp.val);
//                        }
//                        if (bRetVal) {
//                            totFuel();
//                            vp = XMLmv.getTag(xmlStr, "airHeatExchProps", vp.endPos);
//                            if (vp.val.length() > 0) {
//                                airHeatExchProps = new HeatExchProps();
//                                if (!airHeatExchProps.takeDataFromXML(vp.val, fuelFiring, totalFuel)) {
//                                    bRetVal = false;
//                                    errMsg += "Reading air recuperator data";
//                                    break blk;
//                                }
//                            }
//                        }
//                    }
//                } catch (NumberFormatException e) {
//                    errMsg += "in Reading XML";
//                    bRetVal = false;
//                }
//            }
//        }
//        return bRetVal;
//    }

    public double totFuel() {
        totalFuel = totFuel(false);
        if (l2Furnace.bTopBot)
            totalFuel += totFuel(true);
        return totalFuel;
    }

//    public StringBuilder dataInXML() {
//        StringBuilder xmlStr =
//                new StringBuilder(XMLmv.putTag("productionData", l2Furnace.productionData.dataInXML()));
//        xmlStr.append(XMLmv.putTag("fuelFiring", l2Furnace.commFuelFiring.dataInXML()));
//        xmlStr.append(XMLmv.putTag("flueTempOut", fmtTemp.format(flueTempOut)));
//        xmlStr.append(XMLmv.putTag("commonAirTemp", fmtTemp.format(commonAirTemp)));
//        xmlStr.append(XMLmv.putTag("commonFuelTemp", fmtTemp.format(commonFuelTemp)));
//        xmlStr.append(XMLmv.putTag("bTopBot", (l2Furnace.bTopBot) ? "1" : "0"));
//        xmlStr.append(XMLmv.putTag("topZones", zonesInXML(false)));
//        if (l2Furnace.bTopBot)
//            xmlStr.append(XMLmv.putTag("botZones", zonesInXML(true)));
//        if (airHeatExchProps != null)
//            xmlStr.append(XMLmv.putTag("airHeatExhProps", airHeatExchProps.dataInXML()));
//        return xmlStr;
//    }

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

//    boolean takeZonesFromXML(boolean bBot, String xmlStr) throws NumberFormatException {
//        boolean retVal = true;
//        FieldZone[] zones = getZones(bBot);
//        ValAndPos vp;
//        vp = XMLmv.getTag(xmlStr, "nZones", 0);
//        int nZones = Integer.valueOf(vp.val);
//        if (nZones == ((bBot) ? l2Furnace.nBotActiveSecs : l2Furnace.nTopActiveSecs)) {
//            for (int z = 0; z < nZones; z++) {
//                vp = XMLmv.getTag(xmlStr, "frZ#" + ("" + z).trim(), vp.endPos);
//                zones[z] = new FieldZone(l2Furnace, bBot, z, vp.val);
//                if (!zones[z].bValid) {
//                    inError = true;
//                    errMsg += zones[z].errMsg;
//                    retVal = false;
//                    break;
//                }
//            }
//        } else {
//            inError = true;
//            errMsg += "Number of " + ((bBot) ? "Bottom" : "Top") + " zones does not match";
//            retVal = false;
//        }
//        return retVal;
//    }

    public boolean getDataFromUser() {
        FieldResultsDlg dlg = new FieldResultsDlg(true);
        dlg.setLocation(10, 50);
        dlg.setVisible(true);
        return true;
    }

    class FieldResultsDlg extends JDialog {
        DFHTuningParams.FurnaceFor proc;
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
            proc = l2Furnace.l2DFHeating.furnaceFor;
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
            if (ena && (proc != DFHTuningParams.FurnaceFor.STRIP))
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
