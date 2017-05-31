package directFiredHeating;

import FceElements.heatExchanger.HeatExchProps;
import FceElements.heatExchanger.Recuperator;
import appReporting.Reporter;
import basic.*;
import directFiredHeating.process.FurnaceSettings;
import directFiredHeating.process.OneStripDFHProcess;
import mvUtils.display.TimedMessage;
import mvUtils.display.TrendsPanel;
import mvUtils.display.VScrollSync;
import mvUtils.display.XLcellData;
import linkTFM.FceProfTFM;
import mvUtils.display.*;
import mvUtils.math.*;
import mvUtils.mvXML.DoubleWithErrStat;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLgroupStat;
import mvUtils.mvXML.XMLmv;
import mvUtils.display.FramedPanel;
import mvUtils.display.GraphDisplay;
import mvUtils.display.GraphInfoAdapter;
import mvUtils.display.TraceHeader;
import mvUtils.display.VariableDataTrace;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import performance.stripFce.OneZone;
import performance.stripFce.Performance;
import performance.stripFce.PerformanceGroup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

// import sun.nio.cs.ext.MacThai;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 5/7/12
 * Time: 5:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class DFHFurnace {
    boolean bAutoTopOnlySoak = true;
    static public int MAXSECTIONS = 6;
    double fceWidth;
    public boolean bTopBot;
    boolean bAddTopSoak = false;
    protected Vector<FceSection> topSections, botSections;
    FceSection addedTopSoak = null;
    ActionListener listener;
    public LossTypeList lossTypeList;
    FramedPanel topDetailsPanel, botDetailsPanel;
    public DFHeating controller;
    String nlSpace = ErrorStatAndMsg.nlSpace;
//    boolean bZ2TopTempSpecified = false, bZ2BotTempSpecified = false;
    public FuelFiring commFuelFiring;
    public ProductionData productionData;

    UnitFceArray topUfsArray, botUfsArray;
    UnitFceArray ufsArrAS; // for added Soak
    protected Vector<UnitFurnace> vTopUnitFces, vBotUnitFces;
    Vector<UnitFurnace> vASUnitFces; // UnitFurnaces of added TopSoak
    Vector<Double> vTopSlotTempOLast,  vBotSlotTempOLast;
    public DFHTuningParams tuningParams;
    public MultiColData topTResults, botTResults;
    FceEvaluator masterEvaluator;
    boolean bRecalculBot, bRecalculTop;
    double tempGZ1Bot, tempGZ1Top; // old value for recalculation
    LossListWithVal topLossValList, botLossValList, combiLossValList;
    boolean bBaseOnOnlyWallRadiation = false;
    boolean resultsReady = false;
    boolean bDisplayResults = true;
    public FurnaceSettings furnaceSettings;

    public DFHFurnace(DFHeating controller, boolean bTopBot, boolean bAddTopSoak, ActionListener listener) {
        this.controller = controller;
        this.listener = listener;
        initStaticData();
        init();
        changeFiringMode(bTopBot, bAddTopSoak);
    }

    public boolean showEditFceSettings(boolean bEdit) {
        return furnaceSettings.showEditData(bEdit, controller.parent());
    }

    public void clearAssociatedData() {
        evalActiveSecs();
        furnaceSettings = new FurnaceSettings(controller, getActiveSections(false));

    }

    public String fceSettingsInXML() {
        return furnaceSettings.dataInXML().toString();
    }

    public boolean takeFceSettingsFromXML(String xmlStr) {
        boolean retVal = true;
        furnaceSettings = new FurnaceSettings(controller, xmlStr);
        if (furnaceSettings.inError) {
            controller.showError("Reading Furnace Settings: " + furnaceSettings.errMsg);
            retVal = false;
        }
        return retVal;
    }

    boolean enaEdit = true;

    public void setTuningParams(DFHTuningParams tuningParams) {
        this.tuningParams = tuningParams;
    }

    public void enableDataEntry(boolean ena) {
        enaEdit = ena;
        lossTypeList.enableDataEntry(ena);
        for (FceSection sec : topSections)
            sec.enableDataEntry(ena);
        if (bTopBot) {
            for (FceSection sec : botSections)
                sec.enableDataEntry(ena);
            if (bAddTopSoak)
                addedTopSoak.enableDataEntry(ena);
        }
    }

    public double getAmbTemp() {
        return controller.ambTemp;
    }

    public void setCommonFuel(FuelFiring fuelFiring) {
        commFuelFiring = fuelFiring;
        for (FceSection sec : topSections) {
            if (sec.isActive())
                sec.setFuelFiring(commFuelFiring);
        }
        if (bTopBot) {
            for (FceSection sec : botSections) {
                if (sec.isActive())
                    sec.setFuelFiring(commFuelFiring);
            }
            if (bAddTopSoak)
                addedTopSoak.setFuelFiring(commFuelFiring);
        }
    }

    public void changeAirTemp(double newTemp) {
        controller.changeAirTemp(newTemp);
        commFuelFiring.airTemp = newTemp;
        for (FceSection sec : topSections)
            sec.changeAirTemp(newTemp);
        if (bTopBot)
            for (FceSection sec : botSections)
                sec.changeAirTemp(newTemp);
    }

    public void setFceWidth(double fceWidth) {
        this.fceWidth = fceWidth;
    }

    public double getFceWidth() {
        return fceWidth;
    }

    public double holdingWt;  // in kg
    public double totTime;
    public double speed;
    public double calculStep;
    double fceLength = Double.NaN;
    double bottLength = Double.NaN;
    double changeOverPt = 0;

    boolean topOnlyDEnd = false;

    boolean isInTopOnlySection(double pos) {
        return (bTopBot && topOnlyDEnd && (pos > (changeOverPt - 0.001)));
    }

    boolean checkTopBotLength() {
        fceLength = fceLength(false);
        DecimalFormat fmt = new DecimalFormat("#,##0");
        if (bTopBot) {
            bottLength = fceLength(true);
            double diff = fceLength - bottLength;
            if (bAutoTopOnlySoak) {
                if (diff > 0.5) {
                    topOnlyDEnd = decide("Top and bottom Furnace Lengths",
                            "Top Furnace length is greater than the bottom sections\n" +
                                    "Do you want to proceed with the discharge end " +
                                    fmt.format(diff * 1000) + "mm as TOP ONLY FIRING?");
                    if (topOnlyDEnd)
                        changeOverPt = bottLength;
                    return topOnlyDEnd;
                } else
                    return (Math.abs(diff) < 0.0001);
            } else
                return (Math.abs(diff) < 0.0001);
        } else
            return (fceLength > 0);
    }

    public boolean isFuelAvailable(String fuelName)  {
        return controller.isFuelAvailable(fuelName);
    }

    public String checkData(String nlSpace) {
        String retVal = "";
        if (!checkTopBotLength()) {
            if (bTopBot) {
                double lTop = fceLength(false);
                double lBot = fceLength(true);
                retVal += nlSpace + "Check Furnace Lengths top <" + lTop + "> and bottom <" + lBot + ">";
            } else
                retVal += nlSpace + "Check Furnace Length";
//            }
        }
        int s = 0;
        for (FceSection sec : topSections) {
            s++;
            String resp = sec.checkData();
            if (resp.length() > 0)
                retVal += nlSpace + "Top Zone " + s + ": " + resp;
        }
        if (bTopBot) {
            s = 0;
            for (FceSection sec : botSections) {
                s++;
                String resp = sec.checkData();
                if (resp.length() > 0)
                    retVal += nlSpace + "Bottom Zone " + s + ": " + resp;
            }
        }
        return retVal;
    }

    boolean anyCommonFuel() {
        boolean yes = anyCommonFuel(false);
        if (bTopBot)
            yes |= anyCommonFuel(true);
        return yes;
    }

    boolean anyCommonFuel(boolean bBot) {
        boolean yes = false;
        Vector<FceSection> vSec = getVsecs(bBot);
        int nActive = (bBot) ? nBotActiveSecs : nTopActiveSecs;
        for (int s = 0; s < nActive; s++) {
            if (vSec.get(s).isWithCommonFuel()) {
                yes = true;
                break;
            }
        }
        return yes;
    }

    public void setProductionData(ProductionData productionData) {
        this.productionData = productionData;
        for (FceSection sec : topSections)
            sec.setProductionData(productionData);
        if (bTopBot) {
            for (FceSection sec : botSections)
                sec.setProductionData(productionData);
            if (bAddTopSoak)
                addedTopSoak.setProductionData(productionData);
        }
    }

    public void resetSections() {
        fceLength = Double.NaN;
        topOnlyDEnd = false;
//        bZ2TopTempSpecified = false;
//        bZ2BotTempSpecified = false;
        for (FceSection sec : topSections)
            sec.resetSection();
        if (bTopBot)
            for (FceSection sec : botSections)
                sec.resetSection();
    }

    public void resetGasTempPresets() {
        for (FceSection sec : topSections)
            sec.resetGasTemp();
        if (bTopBot)
            for (FceSection sec : botSections)
                sec.resetGasTemp();
    }

    public void resetLossFactor() {
        for (FceSection sec : topSections)
            sec.resetLossFactor();
        if (bTopBot)
            for (FceSection sec : botSections)
                sec.resetLossFactor();
    }

    protected boolean setLossFactor(Performance refP) {
        boolean someFactorIsNot1 = false; // true if any lossFactor is not 0
        for (int s = 0; s < nTopActiveSecs; s++)
            someFactorIsNot1 |= topSections.get(s).setLossFactor(refP.getLossFactor(s, false));
        if (bTopBot)
            for (int s = 0; s < nBotActiveSecs; s++)
                someFactorIsNot1 |= botSections.get(s).setLossFactor(refP.getLossFactor(s, true));
        return someFactorIsNot1;
    }

    public void resetChEmmissCorrectionFactor() {
        productionData.chEmmissCorrectionFactor = 1.0;
        chEmmFactorForPresetTempProfile = 1.0;
    }

    protected void setChEmmissCorrectionFactor(double factor) {
        productionData.setChEmmissCorrectionFactor(factor);
//        tuningParams.chEmmissCorrectionFactor = factor;
    }

    boolean setWallTemperatures() {
        ZoneTemperatureDlg dlg = new ZoneTemperatureDlg(controller.parent(), false);
        dlg.setLocation(400, 100);
        dlg.setVisible(true);
        boolean allOK = dlg.allOK;
        if (allOK && bTopBot) {
            dlg = new ZoneTemperatureDlg(controller.parent(), true);
            dlg.setLocation(400, 100);
            dlg.setVisible(true);
            allOK = dlg.allOK;
        }
        if (allOK)
            setTempOForAllSlots();
        return allOK;
    }

    protected void setTempOForAllSlots() {
        setTempOForAllSlots(false);
        if (bTopBot) {
            setTempOForAllSlots(true);
        }
    }

    protected void setTempOForAllSlots(boolean bBot) {
        Vector<UnitFurnace> vUf = (bBot) ? vBotUnitFces : vTopUnitFces;
        int nActive = (bBot) ? nBotActiveSecs : nTopActiveSecs;
        Vector<FceSection> vSec = (bBot) ? botSections : topSections;

        XYArray zoneTemps = new XYArray();
        for (int s = 0; s < nActive; s++) {
            FceSection sec = vSec.get(s);
            double tcLoc = sec.tcLoc();
            zoneTemps.add(tcLoc, sec.tempAtTCLocation);
        }
        zoneTemps.extrapolate(true, false);
        UnitFurnace uf;
        FceSection sec;
        double endPos;
        for (int u = 0; u < vUf.size() - 1; u++) {
            uf = vUf.get(u);
            sec = uf.fceSec;
            if (sec.bRecuType) {
                endPos = uf.endPos;
                uf.setTempO(zoneTemps.getYat(endPos));
            }
            else
                uf.setTempO(sec.tempAtTCLocation);
        }
        setTempForLosses(zoneTemps, bBot);
    }

    public boolean getReadyToCalcul() {
        return getReadyToCalcul(calculStep, false);
    }

    public boolean getReadyToCalcul(boolean doNotCreateNewSlots) {
        return getReadyToCalcul(controller.calculStep, doNotCreateNewSlots);
    }

    public boolean getReadyToCalcul(double calculStep) {
        return getReadyToCalcul(calculStep, false);
    }

    public boolean getReadyToCalcul(double calculStep, boolean doNotCreateNewSlots){
        boolean bRetVal = true;
        this.calculStep = calculStep;
        evalTotTime();
        evalChUnitArea();
        FceSection sec;
        evalActiveSecs();
        for (int s = 0; s < nTopActiveSecs; s++) {
            sec = topSections.get(s);
            if (sec.enabled)
                sec.getReadyToCalcul();
            else
                break;
        }
        firedCount(false);
        if (bTopBot) {
            for (int s = 0; s < nBotActiveSecs; s++) {
                sec = botSections.get(s);
                if (sec.enabled)
                    sec.getReadyToCalcul();
                else
                    break;
            }
            firedCount(true);
            if (bAddTopSoak)
                addedTopSoak.getReadyToCalcul();
        }
        bRecalculBot = false;
        bRecalculTop = false;
        boolean bNew = false;
        if (doNotCreateNewSlots) {
            if (vTopUnitFces == null)
                bNew = prepareSlots();
        }
        else
            bNew = prepareSlots();
        if (bNew) {
            topTrends = getTrendGraphPanel(false);
            if (bTopBot) {
                botTrends = getTrendGraphPanel(true);
                combiTrends = getCombiGraphsPanel();
            }
        }
        boolean skipUserEntry = false;
        if (tuningParams.bBaseOnZonalTemperature) {
            if (resultsReady) {
                if (decide("Calculation from FURNACE TEMPERATURE",
                        "Do you want to work with wall temperatures of the last calculation?" +
                        "\nChoose 'Yes' only if Furnace physical params have not been changed")) {
                    copyTempoFromLastResults();
                    skipUserEntry = true;
                }
            }
            if (!skipUserEntry)
                bRetVal = setWallTemperatures();
            setOnlyWallRadiation(bRetVal);
        }

        return bRetVal;
    }

    void copyTempoFromLastResults() {
        for (int s = 0; s < vTopUnitFces.size(); s++)
            vTopUnitFces.get(s).tempO = vTopSlotTempOLast.get(s);
        if (bTopBot) {
            for (int s = 0; s < vBotUnitFces.size(); s++)
                vBotUnitFces.get(s).tempO = vBotSlotTempOLast.get(s);

        }
    }

    void resetResults() {
        honorLastZoneMinFceTemp = false;
        considerChTempProfile = false;
        for (FceSection sec : topSections)
            sec.resetResults();
        for (FceSection sec : botSections)
            sec.resetResults();
    }

    public void setOnlyWallRadiation(boolean onlyWallTemp) {
        bBaseOnOnlyWallRadiation = onlyWallTemp;
    }

    public boolean doTheCalculation() {
        // Zone gas temperature presets,  if required to be cleared, is taken care by the caller
        if (bBaseOnOnlyWallRadiation)
            return doTheCalculationWithOnlyWallRadiation();
        else {
            boolean allOk = true;
            boolean reDo = true;
            resetResults();
            boolean bStart = true;
            String addMsg = "";
            bUnableToUsePerfData = false;
            resultsReady = false;
            skipReferenceDataCheck = false;
            boolean bFirstTime = true;
            while (true) {
//                controller.logTrace("DFHFurnace.481: in while(true) with reDo = " + reDo);
                while (allOk && reDo) {
//                    controller.logTrace("DFHFurnace.483: in while(allOk && reDo) with reDo = " + reDo);
                    allOk = false;
                    if (tuningParams.bEvalBotFirst && !bAddTopSoak) {
                        if (bTopBot)
                            allOk = evalTopOrBottom(true, bStart, addMsg);
                        if (allOk && canRun())
                            allOk = evalTopOrBottom(false, bStart, addMsg);
                    } else {
                        allOk = evalTopOrBottom(false, bStart, addMsg);
                        if (allOk && bTopBot && canRun())
                            allOk = evalTopOrBottom(true, bStart, addMsg);
                    }
                    if (bStart && bFirstTime && tuningParams.bHotCharge) {
                        bFirstTime = false;
                        tuningParams.bHotCharge = false;
                        continue;
//                        allOk = true;
//                        break;
                    }
                    bStart = false;
                    if (allOk && canRun() && prepareHeatBalance()) {
                        if (redoWithRecuResults()) {
                            reDo = true;
                            addMsg = " [With revised Air Preheat " + (new DecimalFormat("#,##0 C")).format(commFuelFiring.airTemp) + "]";
                            continue;
                        }
                    }
                    else {
                        allOk = false;
                        break;
                    }

                    if (allOk && bTopBot && canRun()) {
                        double rmsDiff = getGratioStat();
                        boolean resp = false;
                        if (rmsDiff > 0.005) {
                            resp = decide("Top and Bottom Heating",
                                    "Heat Share Error of Top and Bottom is " + (new DecimalFormat("#0.00")).format(rmsDiff * 100) +
                                            "%, Do you want to recalculate with Correction?");
                            controller.parent().toFront();
                        }
                        if (resp) {
                            combiData.noteCorrection();
                        } else
                            reDo = false;
                    } else
                        reDo = false;
                }
//                controller.logTrace("DFHFurnace.531: Exited from while (allOk && reDo)  with reDo = " + reDo);
                if (allOk && canRun() && prepareHeatBalance()) {
                    topTrendsP = getTrendsPanel(false);
                    if (bTopBot) {
                        botTrendsP = getTrendsPanel(true);
                        combiTrendsP = getCombiTrendsPanel();
                    }
                }

                if (allOk && inPerformanceBaseMode) {
                    savePerformanceIfDue();
                    inPerformanceBaseMode = false;
                    if (resetToRequiredProduction()) {
                        resetResults();
                        addMsg = "Final Calculation ";
                        reDo = true;
                        continue;
                    }
                }
//                controller.logTrace("DFHFurnace.550: exiting while(true)");
                break;
            }
            if  (allOk & canRun()) {
                for (int iSec = 0; iSec < nTopActiveSecs; iSec++)
                    topSections.get(iSec).markZonalTemperature(topUfsArray.getMultiColdata(), topUfsArray.colFceTemp);
                vTopSlotTempOLast = new Vector<Double>();
                for (UnitFurnace uf: vTopUnitFces)
                    vTopSlotTempOLast.add(uf.tempO);
                if (bTopBot)  {
                    for (int iSec = 0; iSec < nBotActiveSecs; iSec++)
                        botSections.get(iSec).markZonalTemperature(botUfsArray.getMultiColdata(), botUfsArray.colFceTemp);
                    vBotSlotTempOLast = new Vector<Double>();
                    for (UnitFurnace uf: vBotUnitFces)
                        vBotSlotTempOLast.add(uf.tempO);
                }
                return true;
            }
            else {
                return false;
            }
        }
    }


    void setEntrySlotChargeTemperatures() {
        UnitFurnace uf = vTopUnitFces.get(0);
        double temp = productionData.entryTemp;
        uf.tempWO = uf.tempWmean = uf.tempWcore = temp;
        if (bTopBot) {
            uf = vBotUnitFces.get(0);
            uf.tempWO = uf.tempWmean = uf.tempWcore = temp;
        }
    }

    protected boolean doTheCalculationWithOnlyWallRadiation() {
        boolean allOk = true;
        boolean reDo = true;
        resetResults();
        boolean bStart = true;
        String addMsg = "";
        resultsReady = false;
        setEntrySlotChargeTemperatures();
        while (true) {
            while (allOk && reDo) {
                allOk = false;
                if (tuningParams.bEvalBotFirst && !bAddTopSoak) {
                    if (bTopBot)
                        allOk = evalTopOrBottomWithOnlyWallRadiation(true, bStart, addMsg);
                    if (allOk && canRun())
                        allOk = evalTopOrBottomWithOnlyWallRadiation(false, bStart, addMsg);
                } else {
                    allOk = evalTopOrBottomWithOnlyWallRadiation(false, bStart, addMsg);
                    if (allOk && bTopBot && canRun())
                        allOk = evalTopOrBottomWithOnlyWallRadiation(true, bStart, addMsg);
                }
                bStart = false;
                reDo = false;
            }

            if (allOk && canRun() && bDisplayResults) {
                topTrendsP = getTrendsPanel(false);
                if (bTopBot) {
                    botTrendsP = getTrendsPanel(true);
                    combiTrendsP = getCombiTrendsPanel();
                }
            }
            break;
        }
        bBaseOnOnlyWallRadiation = false;
        return (allOk & canRun());
    }

    protected ChargeStatus chargeAtExit(boolean bBot)  {
        FceSection exitSection = (bBot) ? botSections.get(nBotActiveSecs - 1) :  topSections.get(nTopActiveSecs - 1);
        return new ChargeStatus(productionData.charge, productionData.production,
                exitSection.chExitSurfaceTemp(), exitSection.chEndTemp(), exitSection.chExitCoreTemp());
    }

    protected ChargeStatus chargeAtExit(ChargeStatus chStatus, boolean bBot)  {
        FceSection exitSection = (bBot) ? botSections.get(nBotActiveSecs - 1) :  topSections.get(nTopActiveSecs - 1);
        chStatus.setStatus(productionData.production,
                exitSection.chExitSurfaceTemp(), exitSection.chEndTemp(), exitSection.chExitCoreTemp());
        return chStatus;
    }

    synchronized public boolean evaluate(FceEvaluator master, boolean bShowResults) {
        enablePeformMenu(false);
        setDisplayResults(bShowResults);
        this.masterEvaluator = master;
        inPerfTableMode = false;
        tuningParams.takeValuesFromUI();
        DFHResult.Type switchDisplayto = DFHResult.Type.HEATSUMMARY;
        boolean noHeatBalance = bBaseOnOnlyWallRadiation;
        resetGasTempPresets();
        if (doTheCalculation() && bDisplayResults) {
            if (noHeatBalance) {
                switchDisplayto = DFHResult.Type.TOPtempTRENDS;
                topResultsP = getResultsPanel(false);
                topSecwiseP = getSecwisePanel(false);
                if (controller.heatingMode == DFHeating.HeatingMode.TOPBOTSTRIP) {
                    controller.addResult(DFHResult.Type.TEMPRESULTS, topResultsP);
                    controller.addResult(DFHResult.Type.COMBItempTRENDS, topTrendsP);
                    switchDisplayto = DFHResult.Type.COMBItempTRENDS;
                } else {
                    controller.addResult(DFHResult.Type.TOPtempRESULTS, topResultsP);
                    controller.addResult(DFHResult.Type.TOPtempTRENDS, topTrendsP);

                }
                if (bTopBot) {
                    controller.addResult(DFHResult.Type.BOTtempTRENDS, botTrendsP);
                    botResultsP = getResultsPanel(true);
                    controller.addResult(DFHResult.Type.BOTtempRESULTS, botResultsP);
                    botSecwiseP = getSecwisePanel(true);
                    if (combiTrends != null) {
                        controller.addResult(DFHResult.Type.COMBItempTRENDS, combiTrendsP);
                        switchDisplayto = DFHResult.Type.COMBItempTRENDS;
                    }
                }
            } else {
                controller.addResult(DFHResult.Type.HEATSUMMARY, heatSummary);
                controller.addResult(DFHResult.Type.FUELSUMMARY, fuelSummaryP);
                topResultsP = getResultsPanel(false);
                topSecwiseP = getSecwisePanel(false);
                if (controller.heatingMode == DFHeating.HeatingMode.TOPBOTSTRIP) {
                    controller.addResult(DFHResult.Type.TEMPRESULTS, topResultsP);
                    controller.addResult(DFHResult.Type.SECTIONWISE, topSecwiseP);
                    controller.addResult(DFHResult.Type.COMBItempTRENDS, topTrendsP);
                    controller.addResult(DFHResult.Type.FUELS, topFuelsP);
                } else {
                    controller.addResult(DFHResult.Type.TOPtempRESULTS, topResultsP);
                    controller.addResult(DFHResult.Type.TOPSECTIONWISE, topSecwiseP);
                    controller.addResult(DFHResult.Type.TOPtempTRENDS, topTrendsP);
                    controller.addResult(DFHResult.Type.TOPFUELS, topFuelsP);
                }
                if (bTopBot) {
                    controller.addResult(DFHResult.Type.BOTFUELS, botFuelsP);
                    controller.addResult(DFHResult.Type.BOTtempTRENDS, botTrendsP);
                    botResultsP = getResultsPanel(true);
                    controller.addResult(DFHResult.Type.BOTtempRESULTS, botResultsP);
                    botSecwiseP = getSecwisePanel(true);
                    controller.addResult(DFHResult.Type.BOTSECTIONWISE, botSecwiseP);
                    if (combiTrends != null)
                        controller.addResult(DFHResult.Type.COMBItempTRENDS, combiTrendsP);
                }
                if (recuBalance != null)
                    controller.addResult(DFHResult.Type.RECUBALANCE, recuBalance);
                controller.addResult(DFHResult.Type.LOSSDETAILS, lossValueDetPan());
            }
            controller.resultsReady(getObservations(), switchDisplayto);
            resultsReady = true;
            //            savePerformanceIfDue();
            if (bDisplayResults)
                enablePeformMenu(true);
            setDisplayResults(true);
            return true;
        } else {
            if (canRun())
                controller.abortingCalculation("Some Error in Calculation in DFHFurnace 01");
            setDisplayResults(true);
            return false;
        }
    }

    public void setDisplayResults (boolean bShowResults) {
        this.bDisplayResults = bShowResults;
    }

    public boolean evaluate(FceEvaluator master, double forOutput, double stripWidth) {
        boolean retVal = false;
        enablePeformMenu(false);
        this.masterEvaluator = master;
        tempProductionData = new ProductionData(productionData);
        Charge rC = tempProductionData.charge;
        productionData.charge.setSize(stripWidth, rC.width, rC.height);   // crC.length is strip width
        ProductionData rP = productionData;
        productionData.setProduction(forOutput,
                rP.nChargeRows, rP.entryTemp, rP.exitTemp, rP.deltaTemp, rP.bottShadow);
        getReadyToCalcul();
        inPerfTableMode = true;
        if (doTheCalculation()) {
            retVal = true;
        } else {
            if (canRun())
                controller.abortingCalculation("Some Error in Calculation in DFHFurnace 02");
        }
        if (bDisplayResults)
            enablePeformMenu(true);
        return retVal;
    }

    protected Observations getObservations() {
        Observations observations = new Observations();
        for (FceSection sec : topSections)
            sec.getObservations(observations);
        if (bTopBot)
            for (FceSection sec : botSections)
                sec.getObservations(observations);
        if (flueAfterRecu != null)
            if (flueAfterRecu.flueTemp < 200)
                observations.add("Flue Temperature after Recuperator is Low <" +
                        SPECIAL.roundToNDecimals(flueAfterRecu.flueTemp, 0) + ">");
        return observations;
    }

    void savePerformanceIfDue() {
        if (refFromPerfBase == PerformanceGroup.SAMEWIDTH)
            addResultsToPerfBase(true);
    }

    void enablePeformMenu(boolean ena) {
        if ((controller.furnaceFor == DFHTuningParams.FurnaceFor.STRIP) && !bTopBot
                && (anyIndividualFuel(false) == 0)) {
            controller.enablePerfMenu(ena);
            if (performBase == null)
                controller.enableCreatePerform(true);
            else {
                controller.enableCreatePerform(false);
                controller.enableAddToPerform(true);
            }
        } else
            controller.enablePerfMenu(false);
    }

    protected PerformanceGroup performBase;

    public boolean isPerformanceToBeSaved() {
        return ((performBase != null) && performBase.isItToBeSaved());
    }

    public void performanceIsSaved() {
        if (performBase != null && performBase.isValid())
            performBase.itIsSaved();
    }

    boolean limitLastZoneFceTempIfReqd() {
        boolean bRetVal = false;
        if (controller.furnaceFor() == DFHTuningParams.FurnaceFor.STRIP) {
            FceSection lastZone = topSections.get(nTopActiveSecs - 1);
            double lastZoneMinTemp = controller.minExitZoneFceTemp;
            if (preDefChTemp) {
                double lastZoneFceTemp = lastZone.getLastSlotFceTemp();
                if (lastZoneFceTemp < lastZoneMinTemp) {
                    UnitFurnace theSlot = lastZone.getLastSlot();
                    theSlot.tempG = theSlot.gasTFromFceTandChT(lastZoneMinTemp, productionData.exitTemp);
                    bRetVal = true;
                }
            }
        }
        return bRetVal;
    }

//    public boolean linkPerformanceWithProcess() {
//        return true;
//    }

    public StatusWithMessage addPerformance(Performance p) {
        performBase.addPerformance(p);
        return new StatusWithMessage();
    }

    protected boolean createPerfBase() {
        performBase = new PerformanceGroup(this, tuningParams);
        if (airRecu != null)
            freezeAirRecu();
        return addResultsToPerfBase(false);
    }

    protected boolean addToPerfBase() {
        return addResultsToPerfBase(false);
    }

    boolean perfBaseReady = false;
    boolean chTempProfAvailable = false;

    protected boolean addResultsToPerfBase(boolean createTable) {
        boolean retVal = true;
        Performance perform = getPerformance();
        if (createTable) {
            OneStripDFHProcess stripProc = controller.getStripDFHProcess(perform.processName);
            if (stripProc != null) {
                ErrorStatAndMsg performanceStat = stripProc.productionOkForPerformanceSave(productionData);
                if (performanceStat.inError) {
                    showError("Cannot save Performance, " + performanceStat.msg);
                    retVal = false;
                }
                else
                    retVal = true;
            }
        }
        if (retVal && perform != null && performBase != null) {
            retVal = performBase.noteBasePerformance(perform);
            if (retVal) {
                chTempProfAvailable = performBase.chTempProfAvailable;
                if (!createTable)
                    showMessage("Performance Data:" + perform);
            }
        }
        if (retVal) {
            if (chTempProfAvailable)
                controller.perfBaseAvailable(true);
            if (perform != null && createTable) {
                calculateForPerformanceTable(perform);
//                controller.calculateForPerformanceTable(perform);
//                showMessage("Preparing performance table for Base: " + perform);
            }
        }
        return retVal;
    }

    public FceEvaluator calculateForPerformanceTable(Performance perform) {
        setProductionData(perform.getBaseProductionData());
        FceEvaluator eval = controller.calculateForPerformanceTable(perform);
        showMessage("Preparing performance table for Base: " + perform);
        return eval;
    }

    public Performance getPerformance() {
        Vector<OneZone> allZones = new Vector<OneZone>();
        FceSection sec;
        for (int s = 0; s < nTopActiveSecs; s++) {
            sec = topSections.get(s);
            allZones.add(sec.getZonePerfData());
        }
        // check it any has negative fuel
        boolean dataOK = true;
        for (OneZone z : allZones)
            if (z.fuelFlow < 0) {
                showError("One of the zones has negative Fuel Flow and cannot be saved as Performance Data", 3000);
                dataOK = false;
                break;
            }
        if (dataOK) {
            GregorianCalendar date = new GregorianCalendar();
            return new Performance(productionData, commFuelFiring.fuel, controller.airTemp, allZones, date, this);
        } else
            return null;
    }

    /**
     * note the current results in the table of baseP
     *
     * @param baseP
     */

    void noteInPerformanceTable(Performance baseP) {

    }

    public void clearPerfBase() {
        performBase = null;
        perfBaseReady = false;
        chTempProfAvailable = false;
        refFromPerfBase = PerformanceGroup.NODATA;
        controller.enableCreatePerform(resultsReady);
    }

    boolean redoWithRecuResults() {
        if (existingHeatExch != null) {
            if (controller.bAirHeatedByRecu) {
                double calculAirTempAtBurner = airRecu.heatedTempOut - controller.deltaTAirFromRecu;
                if (Math.abs(calculAirTempAtBurner - controller.airTemp) > 1) {     // recalculate
                    double correctedAirTempAtBurner = calculAirTempAtBurner; //(controller.airTemp + calculAirTempAtBurner) / 2;
                    changeAirTemp(correctedAirTempAtBurner);
                    return true;
                }
            } else {
                existingHeatExch = null;
            }
        }
        return false;
    }

    Reporter report = null;
    double reportSlNo = 0;
    DateFormat dateFormat;

    void clearComparisonTable() {
        if (report != null)
            report.clearReport();
    }

    void saveForComparison() {
        if (report == null) {
            report = new Reporter("Comparison of Results");
            if (controller.furnaceFor == DFHTuningParams.FurnaceFor.STRIP) {
                dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                report.addColumn(Reporter.ColType.TEXT, 150, 6000, "", "Material", "-", "-");
                report.addColumn(Reporter.ColType.NUMBER, 120, 2500, "#,##0.##", "Width", "(mm)");
                report.addColumn(Reporter.ColType.NUMBER, 120, 2500, "#,##0.##", "Thick", "(mm)");
                report.addColumn(Reporter.ColType.NUMBER, 120, 2500, "#,##0.##", "Speed", "(m/min)");
                report.addColumn(Reporter.ColType.NUMBER, 120, 2500, "#,##0.##", "Output", "(t/h)");
                report.addColumn(Reporter.ColType.NUMBER, 120, 2500, "#,##0.##", "Output/m", "(t/h/m)");
                for (FceSection sec : topSections)
                    sec.addReportColumns(report);
                if (bTopBot)
                    for (FceSection sec : botSections)
                        sec.addReportColumns(report);
                report.addColumn(Reporter.ColType.TEXT, 120, 6000, "", "Date", "Time");
            }
        }
        if (controller.furnaceFor == DFHTuningParams.FurnaceFor.STRIP) {
            reportSlNo++;
            String material = productionData.charge.chMaterial.name;
            double w = productionData.charge.length * 1000;   // for strips
            double t = productionData.charge.height * 1000;
            double sp = speed / 60;
            double output = productionData.production / 1000;
            double uOutput = output / productionData.charge.length;
            Date date = new Date();
            Vector<Object> results = new Vector<Object>();
            results.add(material);
            results.add(w);
            results.add(t);
            results.add(sp);
            results.add(output);
            results.add(uOutput);
            for (FceSection sec : topSections)
                sec.addReportData(results);
            if (bTopBot)
                for (FceSection sec : botSections)
                    sec.addReportData(results);

            results.add(dateFormat.format(date));
            report.addResultLine(results);
            controller.enableSaveForComparison(false);
            controller.enableShowComparison(true);
        }
    }

    CombiData combiData;

    double getGratioStat() {
        combiData = new CombiData(vTopUnitFces, vBotUnitFces);
        return combiData.rmsGdiff();
    }

    public void updateUI() {
        if (bDisplayResults)
            progressGraph.updateUI();
    }

    void setAllowSecFuel(boolean allow) {
        for (int s = 0; s < topSections.size(); s++)
            topSections.get(s).setAllowSecFuel(allow);
        for (int s = 0; s < botSections.size(); s++)
            botSections.get(s).setAllowSecFuel(allow);
    }

    TrendsPanel progressGraph;

    public boolean canRun() {
        return (bDisplayResults) ? masterEvaluator.isRunOn() : true;
    }

    void abortIt(String reason) {
        masterEvaluator.abortIt(reason);
    }

    boolean bUnableToUsePerfData = false;
    int refFromPerfBase = PerformanceGroup.NODATA;

    // the following two used for temporary storage of data while calculating base performance
    ProductionData tempProductionData;
    boolean inPerformanceBaseMode = false;  // TODO This is not clear (is always false)
    boolean inPerfTableMode = false;

    boolean resetToRequiredProduction() {
        boolean retVal = false;
        if (tempProductionData != null) {
            productionData.copyFrom(tempProductionData);
            getReadyToCalcul();
            setAverageTemperatureRate();
            retVal = true;
        }
        tempProductionData = null;
        inPerformanceBaseMode = false;
        return retVal;
    }

    boolean preDefChTemp = true;
//    double[] chInTempProfile;
    boolean honorLastZoneMinFceTemp = false;
    boolean considerChTempProfile = false;
    boolean skipReferenceDataCheck = false;
    boolean smoothenCurve = true;
    protected boolean bConsiderPresetChInTempProfile = false;
    double[] presetChInTempProfileTop;
    double[] presetChInTempProfileBot;
    double chEmmFactorForPresetTempProfile = 1.0;

    protected boolean fillChInTempProfile() {
        boolean retVal = true;
        presetChInTempProfileTop = new double[nTopActiveSecs];
        for (int s = 0; s < nTopActiveSecs; s++)
            presetChInTempProfileTop[s] = topSections.get(s).chEntryTemp();

        // TODO the following to be removed in RELEASE
        String prof = "In fillChInTempProfile";
        for (double val: presetChInTempProfileTop)
            prof += String.format(", %3.3f", val);
        controller.logTrace("DFHFurnace.1051: ChTempProfile = " + prof + ", chEmmFactor = " + productionData.chEmmissCorrectionFactor);  // TODO-remove

        if (bTopBot) {
            presetChInTempProfileBot = new double[nBotActiveSecs];
            for (int s = 0; s < nBotActiveSecs; s++)
                presetChInTempProfileBot[s] = botSections.get(s).chEntryTemp();
        }
        chEmmFactorForPresetTempProfile = productionData.chEmmissCorrectionFactor;
        return retVal;
    }

    public void setCurveSmoothening(boolean ena) {
        smoothenCurve = ena;
    }

    protected String getProcessName() {
        return productionData.charge.chMaterial.toString();
    }

    protected String getMainTitle() {
        Charge ch = productionData.charge;
        String title;
        if (controller.furnaceFor == DFHTuningParams.FurnaceFor.STRIP)
            title = String.format("Strip %4.0f x %4.2f at %5.2f t/h",
                    ch.getLength() * 1000, ch.getHeight() * 1000, productionData.production / 1000);
        else
            title = String.format("Size %5.0f x %5.0f x %5.0f at %5.2f t/h",
                    ch.getWidth() * 1000, ch.getHeight() * 1000, ch.getLength() * 1000, productionData.production / 1000);
        return title;
    }

    boolean evalTopOrBottom(boolean bBot, boolean bStart, String addMsg) {
        boolean allOk = true;
        Vector<FceSection> vSec;
        int iFirstFiredSection;
        int iSecWithEntryTempFixed;  // In case STRIP with respecting Exit Zone Temp, while adjusting
                                     // strip temperature,this section's entry temperatures has to be maintained
        int iLastFiredSection;
        double[] chInTempProfile = null;
        int nFiredSecs;
        int[] fired;
        int firstRevCalUpto = 0;
        boolean bFlueTspecified =  false;
        double flueTemp = 0;
        String title;
        String add2ToTilte = "";
        String addToTitle = " " + addMsg;
        if (bBot) {
            vSec = botSections;
            fired = botFiredSections;
            nFiredSecs = nBotFired;
            progressGraph = botTrends;
            title = "Bottom Zones";
        } else {
            vSec = topSections;
            fired = topFiredSections;
            nFiredSecs = nTopFired;
            progressGraph = topTrends;
            if (bTopBot)
                title = "Top Zones";
            else title = "";
        }
        iFirstFiredSection = fired[0];
        iSecWithEntryTempFixed = (tuningParams.bRespectFirstFiredZoneExitStripTemp) ? (iFirstFiredSection + 1) : 0;
        iLastFiredSection = fired[nFiredSecs - 1];
        int iLastSec = fired[nFiredSecs - 1];
        firstRevCalUpto = iFirstFiredSection + 1;
        setAverageTemperatureRate();
        boolean bFirstTime = true;
        FceSection theSection, toSection;
        FceSection firstSec, lastSec;
        UnitFurnace theSlot;
        Charge ch = productionData.charge;
        double tempWOEnd = productionData.exitTemp;
        String mainTitle = getMainTitle();
        double chTempProfileFactor = 1;    // for correction if (honorLastZoneMinFceTemp)
        double chInTempReqd;
        if (bDisplayResults && (tuningParams.bSlotProgress || tuningParams.bSectionProgress))
            masterEvaluator.setProgressGraph(getProcessName(), mainTitle, title + add2ToTilte + addToTitle, progressGraph);
        String statusHead = (bBot) ? "Bottom Zone " : "Top Zone ";
        FceEvaluator.EvalStat response;
        double szTemp = 0;

        while (allOk && canRun()) {
//            controller.logTrace("DFHFurnace.1148 (evalTopOrBottom): in while(allOk && canRun()");
            if (bTopBot && !bBot && bAddTopSoak) {
                theSection = addedTopSoak;
                theSlot = theSection.getLastSlot();
                theSlot.setEW(tempWOEnd);  // 20170227 theSlot.eW = ch.getEmiss(tempWOEnd);
                theSlot.tempG = gasTforChargeT(theSlot, tempWOEnd, productionData.deltaTemp); // theSlot.gasTforChargeT(tempWOEnd, production.deltaTemp);
                if (bStart) {
                    szTemp = theSlot.tempG;
                    setStartTProf(szTemp, bBot);
                }
                theSlot.getWMean(tempWOEnd, productionData.deltaTemp);

                showStatus(statusHead + "Top Soak");
                response = theSection.oneSectionInRev();

                toSection = vSec.get(fired[nFiredSecs - 1]);
                toSection.copyFromNextSection(theSection);
                FlueCompoAndQty netFlue = new FlueCompoAndQty(theSection.flueComposition, 0, 0);
                netFlue = theSection.fuelInFsection(netFlue);
                toSection.passFlueCompAndQty.noteValues(netFlue);
                toSection.fluePassThrough = netFlue.flow;
            }
            double temWOreqd;
            double deltaTempReqd;
            if (bTopBot && bAddTopSoak) { // added soak is already calculated during top sections round
                theSection = vSec.get(fired[nFiredSecs - 1]);  // the last fired section excluding added top soak
                theSlot = theSection.getLastSlot();
                temWOreqd = addedTopSoak.chEntrySurfTemp();
                deltaTempReqd = addedTopSoak.chEntryDeltaT();
                theSlot.setEW(temWOreqd);  // 20170227 theSlot.eW = ch.getEmiss(temWOreqd);
                theSlot.tempG = gasTforChargeT(theSlot, temWOreqd, deltaTempReqd); // theSlot.gasTforChargeT(temWOreqd, deltaTempReqd);
                if (bStart)
                    setStartTProf(theSlot.tempG, bBot);
                theSlot.getWMean(temWOreqd, deltaTempReqd);
            } else if (bTopBot && bBot && topOnlyDEnd) {
                theSection = vSec.get(fired[nFiredSecs - 1]);  // the last fired section excluding added top soak
                theSlot = theSection.getLastSlot();
                MultiColData results = getTopTResults();
                temWOreqd = results.getYat(2, changeOverPt);
                deltaTempReqd = temWOreqd - results.getYat(3, changeOverPt);
                double tempWMean = results.getYat(4, changeOverPt);
                theSlot.setEW(tempWMean); // 20170227 theSlot.eW = ch.getEmiss(tempWMean);
                theSlot.tempG = gasTforChargeT(theSlot, temWOreqd, deltaTempReqd);// theSlot.gasTforChargeT(temWOreqd, deltaTempReqd);
                if (bStart)
                    setStartTProf(theSlot.tempG, bBot);
                theSlot.getWMean(temWOreqd, deltaTempReqd);
            } else {
                if ((controller.furnaceFor == DFHTuningParams.FurnaceFor.STRIP) &&
                        tuningParams.bConsiderChTempProfile && /*bFirstTime &&*/ chTempProfAvailable /*&& !considerChTempProfile*/) {
//                    chInTempProfile = new double[nTopActiveSecs];
                    chTempProfileFactor = 1.0;  // reset to default. This is for respecting exit zone minimum temperature
                    honorLastZoneMinFceTemp = false;
                    if (!skipReferenceDataCheck) {
                        Performance refP = performBase.getRefPerformance(productionData, commFuelFiring.fuel);
                        if (bConsiderPresetChInTempProfile) {
                            chInTempProfile = presetChInTempProfileTop;
                            setChEmmissCorrectionFactor(chEmmFactorForPresetTempProfile);
                        }
                        else {
//                            Performance refP = performBase.getRefPerformance(productionData, commFuelFiring.fuel);
                            if (refP != null) {
                                chInTempProfile = refP.getChInTempProfile(productionData.exitTemp);
                                setChEmmissCorrectionFactor(refP.chEmmCorrectionFactor);
                            }
                        }
                        if (chInTempProfile != null && chInTempProfile.length == nTopActiveSecs) {
                            if (inPerfTableMode)
                                considerChTempProfile = true;
                            else {
                                if (controller.isOnManualCalculation()) {
                                    if (setLossFactor(refP))
                                        debug("Setting LossFactors from Performance Data, some Factors are not 1.0");
                                }
                                if (!userActionAllowed())
                                    considerChTempProfile = true;
                                else
                                    if (!considerChTempProfile)
                                        considerChTempProfile = decide("Charge Temperature Profile",
                                            "Do you want to consider the Charge Temperature Profile from the Reference Performance data?", 3000);
                            }
                        } else {
                            considerChTempProfile = false;
                            if (tuningParams.bOnProductionLine) {
                                showMessage("No Reference Data available. Aborting calculations");
                                allOk = false;
                                break;
                            } else {
                                showMessage("Reference data not available for this process");
                                skipReferenceDataCheck = true;
                            }
                        }
                    }
                }
                else
                    setChEmmissCorrectionFactor(1.0);

//                System.out.println("DFHFurnace.1222: chEmmfactor = "  + productionData.chEmmissCorrectionFactor); // TODO remove on RELEASE

                theSection = vSec.get(iLastSec);  // the last fired section
                theSlot = theSection.getLastSlot();
                theSlot.setEW(tempWOEnd); // 20170227  theSlot.eW = ch.getEmiss(tempWOEnd);
                if (honorLastZoneMinFceTemp)
                    theSlot.tempG = theSlot.gasTFromFceTandChT(controller.minExitZoneFceTemp, tempWOEnd);
                else {
                    theSlot.tempG = gasTforChargeT(theSlot, tempWOEnd, productionData.deltaTemp);
//                    System.out.println("DFHFurnace.1204: lastSlot.TempG = " + theSlot.tempG );
                }
                if (bStart)
                    setStartTProf(theSlot.tempG, bBot);
                theSlot.getWMean(tempWOEnd, productionData.deltaTemp);
            }

            if (allOk) {
                bStart = false;

                for (int iSec = iLastSec; iSec >= firstRevCalUpto; iSec--) {
                    showStatus(statusHead + (iSec + 1));
                    theSection = vSec.get(iSec);
                    if (iSec < iLastSec) {
                        double nextSecGasT = vSec.get(iSec + 1).getFirstSlotGasTemp();
                        allOk = getGasTempIfRequired(iSec, bBot, nextSecGasT);
                        if (!allOk)
                            break;
                    }

                    if (considerChTempProfile && !((iSec == iLastSec) && honorLastZoneMinFceTemp)) {
                        if (iSec > iFirstFiredSection) {
                            double firstOutTemp = chInTempProfile[iSecWithEntryTempFixed];
                            chInTempReqd = firstOutTemp + chTempProfileFactor * (chInTempProfile[iSec] - firstOutTemp);
                        } else
                            chInTempReqd = chInTempProfile[iSec];
                        response = theSection.oneSectionInRev(chInTempReqd);

//                        System.out.println("DFHFurnace.1232: '" + title + add2ToTilte + addToTitle + "' : theSection.chEntryTemp " +  theSection.chEntryTemp()); // TODO remove in RELEASE

                    } else {
                        response = theSection.oneSectionInRev();
                        if (tuningParams.bAdjustChTempProfile && (iSec == iLastSec) && honorLastZoneMinFceTemp)
                            chTempProfileFactor = chTempFactor(chInTempProfile, iSec, theSection, iSecWithEntryTempFixed, iLastFiredSection);
                    }
                    if (!(response == FceEvaluator.EvalStat.OK)) {
                        allOk = false;
                        break;
                    }
                    toSection = vSec.get(iSec - 1);
                    toSection.copyFromNextSection(theSection);
                    if (tuningParams.bSectionalFlueExh)
                        toSection.fluePassThrough = 0;
                    else {
                        FlueCompoAndQty netFlue = flueFromDEnd(bBot, iSec - 1);
                        toSection.passFlueCompAndQty.noteValues(netFlue);
                        toSection.fluePassThrough = netFlue.flow;
                    }
                    if (considerChTempProfile && (iSec == iLastSec) && !honorLastZoneMinFceTemp) {
                        honorLastZoneMinFceTemp = limitLastZoneFceTempIfReqd();
                        if (honorLastZoneMinFceTemp) {
                            iSec++;
                            continue;
                        }
                    }

                    if (!(response == FceEvaluator.EvalStat.OK)) {
                        allOk = false;
                        break;
                    }
                }
            }
            if (allOk) {
                firstSec = vSec.get(0);
                lastSec = vSec.get(firstRevCalUpto);
                if (bFlueTspecified) {    //eval the first zone in Fwd followed by the next zone in Rev
                    setInitialChHeat(0, firstRevCalUpto - 1, productionData.entryTemp, lastSec.chEntryTemp(), bBot);
                    firstSec.fluePassThrough = totalFlue(flueTemp, bBot);
                    firstSec.totFlueCompAndQty.noteValues(commFuelFiring.flue, firstSec.fluePassThrough, 0, 0);
                    firstSec.passFlueCompAndQty.noteValues(firstSec.totFlueCompAndQty);
                    vSec.get(iFirstFiredSection).secFlueFlow = firstSec.secFlueFlow;
                    firstSec.setEntryGasTemp(flueTemp);
                    firstZoneInFwd(iFirstFiredSection, bBot, true);
                    if (bBot) {
                        tempGZ1Bot = vSec.get(fired[0]).getLastSlotGasTemp();
                        bRecalculBot = true;
                    } else {
                        tempGZ1Top = vSec.get(fired[0]).getLastSlotGasTemp();
                        bRecalculTop = true;
                    }
                    double tmToReach = vSec.get(iFirstFiredSection).chEndTemp();
                    zoneInRevToBalance(firstRevCalUpto - 1, iFirstFiredSection + 1, tmToReach, bBot);

                    firstRevCalUpto = iFirstFiredSection + 1;
                    theSection = vSec.get(fired[1]);
                    theSection.setPresetGasTemp(vSec.get(firstRevCalUpto - 1).getEnteringGasTemp());
                    bFlueTspecified = false;
                    continue;
                } else {
                    if (tuningParams.bHotCharge) {
                        double chEntryTemp = productionData.entryTemp;
                        double tempToReach =  lastSec.chEntryTemp();
                        double tempNow;
                        double diff;
                        FceSection theFiredSec = vSec.get(iFirstFiredSection);
                        flueTemp = productionData.entryTemp + tuningParams.startFlueTempHead;
//                        setInitialChHeat(0, firstRevCalUpto - 1, chEntryTemp, lastSec.chEntryTemp(), bBot);
                        boolean thisDone = false;
                        while(!thisDone) {
                            if (canRun()) {
                                setInitialChHeat(0, firstRevCalUpto - 1, chEntryTemp, lastSec.chEntryTemp(), bBot);
                                firstSec.fluePassThrough = totalFlue(flueTemp, bBot);
//                                System.out.println("flueTemp = " + flueTemp + ", Qty= " + firstSec.fluePassThrough );
                                firstSec.totFlueCompAndQty.noteValues(commFuelFiring.flue, firstSec.fluePassThrough, 0, 0);
                                firstSec.passFlueCompAndQty.noteValues(firstSec.totFlueCompAndQty);
//                        vSec.get(iFirstFiredSection).secFlueFlow = firstSec.secFlueFlow;
                                firstSec.setEntryGasTemp(flueTemp);
                                firstZoneInFwd(iFirstFiredSection, bBot, false);   // do not copy forward the last slot
                                if (bBot) {
                                    tempGZ1Bot = vSec.get(fired[0]).getLastSlotGasTemp();
                                    bRecalculBot = true;
                                } else {
                                    tempGZ1Top = vSec.get(fired[0]).getLastSlotGasTemp();
                                    bRecalculTop = true;
                                }
                                tempNow = theFiredSec.chEndTemp();
                                diff = tempToReach - tempNow;
                                if (Math.abs(diff) < 0.5)
                                    thisDone = true;
                                else {
                                    flueTemp += diff / (tempToReach - chEntryTemp) * (flueTemp - chEntryTemp);
                                }
                            }
                            else {
                                allOk = false;
                                break;
                            }
//                        zoneInRevToBalance(firstRevCalUpto - 1, iFirstFiredSection + 1, tmToReach, bBot);
//
//                        firstRevCalUpto = iFirstFiredSection + 1;
//                        theSection = vSec.get(fired[1]);
//                            theSection.setPresetGasTemp(vSec.get(firstRevCalUpto - 1).getEnteringGasTemp());

                        }
                    }
                    else {
//                        controller.logTrace("DFHFurnace.1187 (evalTopOrBottom): before firstZoneInRev");
                        allOk = firstZoneInRev(bBot);
//                        controller.logTrace("DFHFurnace.1189 (evalTopOrBottom): afer firstZoneInRev");
                    }
                }
            }
            if (allOk) {
                if (canRun() && tuningParams.bSlotRadInCalcul && bFirstTime) {
                    if (tuningParams.bAutoTempForLosses)
                        redoLosses(bBot);
                    evalInterSlotRadiation(bBot);
                    bFirstTime = false;
                    addToTitle = " (With Internal Radiation) ..." + addMsg;
                    masterEvaluator.setCalculTitle(title + add2ToTilte + addToTitle);
                } else {
                    if (bFirstTime && tuningParams.bAutoTempForLosses) {
                        redoLosses(bBot);
                        bFirstTime = false;
                        addToTitle = " (with Re-calculated Losses) ..." + addMsg;
                        masterEvaluator.setCalculTitle(title + add2ToTilte + addToTitle);
                    } else
                        break;
                }
            }
        }
//        controller.logTrace("DFHFurnace.1409 (evalTopOrBottom): exited while(allOk && canRun()");
        if (allOk && smoothenCurve && tuningParams.bSmoothenCurve) {
            smoothenProfile(bBot);
        }
        return allOk;
    }

    boolean evalTopOrBottomWithOnlyWallRadiation(boolean bBot, boolean bStart, String addMsg) {
        boolean allOk = true;
        Vector<FceSection> vSec;
        String title;
        String add2ToTilte = "";
        String addToTitle = " ..." + addMsg;
        int nActiveSecs;
        if (bBot) {
            vSec = botSections;
            nActiveSecs = nBotActiveSecs;
            progressGraph = botTrends;
            title = "Bottom Zones";
        } else {
            vSec = topSections;
            nActiveSecs = nTopActiveSecs;
            progressGraph = topTrends;
            title = topBotName(false) + "Zones";
        }
        setAverageTemperatureRate();
        FceSection theSection;
        String mainTitle = getMainTitle();
        if (bDisplayResults && (tuningParams.bSlotProgress || tuningParams.bSectionProgress))
            masterEvaluator.setProgressGraph(getProcessName(), mainTitle, title + add2ToTilte + addToTitle, progressGraph);
        String statusHead = (bBot) ? "Bottom Zone " : "Top Zone ";
        FceEvaluator.EvalStat response;
        while (allOk && canRun()) {
            FceSection nextSection;
            for (int iSec = 0; iSec < nActiveSecs; iSec++) {
                showStatus(statusHead + (iSec + 1));
                theSection = vSec.get(iSec);
                response = theSection.oneSectionInFwdWithWallRadiation();
                if (!(response == FceEvaluator.EvalStat.OK)) {
                    allOk = false;
                    break;
                }
                if (iSec < (nActiveSecs - 1)) {
                    nextSection = vSec.get(iSec + 1);
                    nextSection.copyFromPrevSection(theSection);
                }
            }
            break;
        }
        if (allOk && smoothenCurve && tuningParams.bSmoothenCurve)
            smoothenProfile(bBot);
        return allOk;
     }

    double chTempFactor(double[] nowProfile, int iSecNum, FceSection theSection, int iSecWithFixedEntryTemp, int iLastFired) {
        double factor = 1;
        if (iSecNum == iLastFired && iSecNum > (iSecWithFixedEntryTemp)) {  // if iLastFired == (iFirstFired + 2) then there is no scope for adjustment
            double nowInTemp = theSection.chEntryTemp();
            double firstOutTemp = nowProfile[iSecWithFixedEntryTemp];  // the IN Temperature of [iFirstFired + 1]
            return (nowInTemp - firstOutTemp) / (nowProfile[iSecNum] - firstOutTemp);
        }
        return factor;
    }

    double gasTforChargeT(UnitFurnace theSlot, double tempWOReqd, double deltaTempReqd) {
        if (controller.furnaceFor == DFHTuningParams.FurnaceFor.STRIP) {
            double gasT = theSlot.gasTFromFceTandChT(controller.exitZoneFceTemp, tempWOReqd);
            productionData.deltaTemp = theSlot.getDeltaTcharge(gasT, tempWOReqd);
            return gasT;
        } else
            return theSlot.gasTforChargeT(tempWOReqd, deltaTempReqd);
    }

    UnitFurnace getUFceAtTopOnlyStart() {
        UnitFurnace uf = null;
        FceSection sec;
        for (int s = nTopActiveSecs - 1; s >= 0; s--) {
            sec = topSections.get(s);
            if (sec.secStartPos <= changeOverPt) {
                uf = sec.getUnitFceAtStPos(changeOverPt);
                break;
            }
        }
        return uf;
    }

    protected boolean userActionAllowed() {
        return controller.userActionAllowed; // (!controller.isOnProductionLine() || controller.showDebugMessages);
    }

    boolean getGasTempIfRequired(int iSec, boolean bBot, double nextSecGasT) {
        boolean allOK = true;
        FceSection sec = (bBot) ? botSections.get(iSec) : topSections.get(iSec);
        if (!sec.bRecuType && !sec.bGasTempSpecified) {
            double suggTemp = sec.avgZoneTempForLosses() + 20;
            if (controller.furnaceFor() == DFHTuningParams.FurnaceFor.STRIP)
                suggTemp = (suggTemp > (nextSecGasT - 20)) ? (nextSecGasT - 20) : suggTemp;
            suggTemp = 10 * SPECIAL.roundToNDecimals(suggTemp / 10, -1) - 5;
//            debug("Checking if userActionAllowed");
            if (userActionAllowed()) {
                String title = topBotName(bBot) + "Zone #" + (iSec + 1);
                OneParameterDialog tempDlg = new OneParameterDialog(controller, title, 3000);
                tempDlg.setValue("Zone Gas Temperature (C)", suggTemp, "#,##0", 100, 1700);
                tempDlg.setLocationRelativeTo(controller.parent());
                tempDlg.setVisible(true);
                if (tempDlg.isOk())
                    sec.setPresetGasTemp(tempDlg.getVal());
                else
                    allOK = false;
            } else
                sec.setPresetGasTemp(suggTemp);

//            System.out.println("DFHFurnace.1472: iSec " + iSec + ", suggTemp = " + suggTemp); // TODO to be removed in RELEASE

        }
        return allOK;
    }

    public boolean assignLoss(boolean bBot, int lossID, double fraction) {
        Vector<FceSection> vSec = getVsecs(bBot);
        int nActive = (bBot) ? nBotActiveSecs : nTopActiveSecs;
        for (int s = 0; s < nActive; s++)
            vSec.get(s).assignLoss(lossID, fraction);
        return true;
    }

    public boolean assignLoss(int s, int subSec, boolean bBot, int lossID) {
        Vector<FceSection> vSec = getVsecs(bBot);
        FceSection sec = vSec.get(s);
        if (sec != null)
            return sec.assignLoss(subSec, lossID);
        else
            return false;
    }

    public boolean assignLoss(boolean bBot, double stPos, double endPos, int lossID, double fraction) {
        Vector<FceSection> vSec = getVsecs(bBot);
        int nActive = activeSections(bBot);
        boolean bRetVal = false;
        for (int s = 0; s < nActive; s++) {
            bRetVal = vSec.get(s).assignLoss(stPos, endPos, lossID, fraction);
        }
        return bRetVal;
    }

    public boolean assignLoss(double atPos, boolean bBot, int lossID, double fraction) {
        Vector<FceSection> vSec = getVsecs(bBot);
        int nActive = activeSections(bBot);
        boolean bRetVal = false;
        for (int s = 0; s < nActive; s++) {
            bRetVal = vSec.get(s).assignLoss(atPos, lossID, fraction);
            if (bRetVal)
                break;
        }
        return bRetVal;
    }

    public boolean assignLoss(boolean atChEnd, boolean atDischEnd, boolean bBot, int lossID, double fraction) {
        boolean retVal = true;
        Vector<FceSection> vSec = getVsecs(bBot);
        int activeN = activeSections(bBot);
        FceSection sec;
        if (activeN > 0) {
            if (atChEnd) {
                sec = vSec.get(0);
                retVal &= sec.assignLoss(true, false, lossID, fraction);
            }
            if (atDischEnd) {
                sec = vSec.get(activeN - 1);
                retVal &= sec.assignLoss(false, true, lossID, fraction);
            }
        }
        return retVal;
    }

    public void resetLossAssignment() {
        for (FceSection sec : topSections)
            sec.resetLossAssignment();
        for (FceSection sec : botSections)
            sec.resetLossAssignment();

    }

    void setStartTProf(double szTemp, boolean bBot) {
        if (tuningParams.bAutoTempForLosses) {
            XYArray tProf = new XYArray();
            //charging end temp is taken as
            double entryT = productionData.entryTemp + (szTemp - productionData.entryTemp) * 0.65;
            tProf.add(new DoublePoint(0, entryT));
            // exit 30% length taken as szTemp
            // the middle 40% is considered with 50% of average slope
            double len = getFceLength();
            double avgSlope = (szTemp - entryT) / (0.7 * len);
            double midSlope = 0.5 * avgSlope;
            double midEntryT = szTemp - midSlope * 0.4 * len;
            tProf.add(new DoublePoint(0.3 * len, midEntryT));
            tProf.add(new DoublePoint(0.7 * len, szTemp));
            tProf.add(new DoublePoint(len, szTemp));
            setTempForLosses(tProf, bBot);
//            Vector<FceSection> vSec = getVsecs(bBot);
//            for (FceSection sec : vSec) {
//                sec.setTempForLosses(tProf);
//                sec.redoLosses();
//            }
        }
    }

    protected void setTempForLosses(XYArray tProf, boolean bBot) {
        for (FceSection sec : getVsecs(bBot)) {
            sec.setTempForLosses(tProf);
            sec.redoLosses();
        }
    }

    void redoLosses(boolean bBot) {
        Vector<FceSection> vSec = getVsecs(bBot);
        MultiColData result = (bBot) ? getBotTResults() : getTopTResults();
        int trace = topUfsArray.colFceTemp;
        for (FceSection sec : vSec) {
            sec.setTempForLosses(result, trace);
            sec.redoLosses();
        }
    }

    public double getFceTempAt(double xPos, boolean bBot) {
        MultiColData result = (bBot) ? getBotTResults() : getTopTResults();
        int trace = (bBot) ? botUfsArray.colFceTemp : topUfsArray.colFceTemp;
        return result.getYat(trace, xPos);
    }

    public double getChTempAt(double xPos, boolean bBot) {
        MultiColData result = (bBot) ? getBotTResults() : getTopTResults();
        int trace = (bBot) ? botUfsArray.colChTempMean : topUfsArray.colChTempMean;
        return result.getYat(trace, xPos);
    }

    public double getGasTempAt(double xPos, boolean bBot) {
        MultiColData result = (bBot) ? getBotTResults() : getTopTResults();
        int trace = (bBot) ? botUfsArray.colGasTemp : topUfsArray.colGasTemp;
        return result.getYat(trace, xPos);
    }

    void allowManualTempForLosses(boolean bAllow) {
        for (FceSection sec : topSections)
            sec.allowManuelTempForLosses(bAllow);
        if (bTopBot) {
            for (FceSection sec : botSections)
                sec.allowManuelTempForLosses(bAllow);
        }
    }

    FceEvaluator.EvalStat zoneInRevToBalance(int iFromSec, int iToSec, double tempChMreqd, boolean bBot) {
        FceEvaluator.EvalStat response, lastResponse;
        double tempGZAssume;
        Vector<FceSection> vSec;
        vSec = getVsecs(bBot);
        String statusHead = (bBot) ? "Bottom ZOne " : "Top Zone ";
        double diff, lastDiff;
        double tmNow, tmLast = 0, tmDiff;
        double tgNow, tgLast = 0, tgDiff;
        double tgMax = -1000, tgMin = 100000;
        boolean trial1;
//        double trialDelT = 10;
//        boolean redoit = false;
        FceSection theSection, toSection;
        FceSection fireSec = vSec.get(iFromSec);
//        double suggested1stCorrection = tuningParams.suggested1stCorrection;
//        if (suggested1stCorrection > 0)
        double trialDelT = tuningParams.suggested1stCorrection;
        tempGZAssume = fireSec.getEnteringGasTemp();
        trial1 = true;
        response = FceEvaluator.EvalStat.OK;
        lastResponse = FceEvaluator.EvalStat.DONTKNOW;
        while (canRun()) {
//            redoit = false;
            fireSec.setLastSlotGasTemp(tempGZAssume);
            for (int iSec = iFromSec; iSec >= iToSec; iSec--) {
                showStatus(statusHead + (iSec + 1));
                theSection = vSec.get(iSec);
                response = theSection.oneSectionInRev();
                if (!(response == FceEvaluator.EvalStat.OK)) {
                    if (response != lastResponse) {
                        lastResponse = response;
//                        if (suggested1stCorrection > 0)
                            trialDelT = tuningParams.suggested1stCorrection;
//                        else
//                            trialDelT = 10;
                    } else
                        trialDelT *= 2;
                    if (response == FceEvaluator.EvalStat.TOOHIGHGAS) {
                        tempGZAssume -= trialDelT;
//                        redoit = true;
                        break;
                    }
                    if (response == FceEvaluator.EvalStat.TOOLOWGAS) {
                        tempGZAssume += trialDelT;
//                        redoit = true;
                        break;
                    }
                }
                if (iSec > 0) {
                    toSection = vSec.get(iSec - 1);
                    toSection.copyFromNextSection(theSection);
                    if (tuningParams.bSectionalFlueExh)
                        toSection.fluePassThrough = 0;
                    else {
                        FlueCompoAndQty netFlue = flueFromDEnd(bBot, iSec - 1);
                        toSection.passFlueCompAndQty.noteValues(netFlue);
                        toSection.fluePassThrough = netFlue.flow;
                    }
                } else {
                    if (tuningParams.bSectionalFlueExh)
                        flueFromDEnd(bBot, iSec - 1);
                }
            }
            tmNow = vSec.get(iToSec).chEntryTemp();
            diff = tmNow - tempChMreqd;
            if (Math.abs(diff) < 1 * tuningParams.errorAllowed)
                break;
            if (trial1) {
                trial1 = false;
                tgLast = tempGZAssume;
                tmLast = tmNow;
                if (diff < 0)
                    tempGZAssume -= trialDelT;
                else
                    tempGZAssume += trialDelT;
            } else {
                tgNow = tempGZAssume;
                tempGZAssume = tgNow + (tgNow - tgLast) / (tmNow - tmLast) * (tempChMreqd - tmNow);
                tgLast = tgNow;
                tmLast = tmNow;
            }
            lastDiff = diff;
        }
        return response;
    }

    void smoothenProfile(boolean bBot) {
        if (bBot) {
            for (int s = 1; s < nBotActiveSecs; s++)
                botSections.get(s).smoothenProfile();
        } else {
            for (int s = 1; s < nTopActiveSecs; s++)
                topSections.get(s).smoothenProfile();
        }
    }

    void evalInterSlotRadiation(boolean bBot) {
        Vector<FceSection> vSec = getVsecs(bBot);
        for (int s = 0; s < activeSections(bBot); s++)
            vSec.get(s).evalSlotRadiationOut();
        if (bTopBot && !bBot && bAddTopSoak)
            addedTopSoak.evalSlotRadiationOut();
        for (int s = 0; s < activeSections(bBot); s++)
            vSec.get(s).evalSlotRadiationSumm();
        if (bTopBot && !bBot && bAddTopSoak)
            addedTopSoak.evalSlotRadiationSumm();
    }

    boolean firstZoneInFwd(int uptoSec, boolean bBot, boolean copyForward) {
        boolean bRetVal = true;
        FceEvaluator.EvalStat response;
        String statusHead;
        Vector<FceSection> vSec;
        int[] fired;
        if (bBot) {
            fired = botFiredSections;
            vSec = botSections;
            statusHead = "Bottom Zone ";
        } else {
            fired = topFiredSections;
            vSec = topSections;
            statusHead = "Top Zone ";
        }
        int iFirstFiredSec = fired[0];
        FceSection sec, nextSec;
        for (int s = 0; s <= iFirstFiredSec; s++) {
            sec = vSec.get(s);
            showStatus(statusHead + (s + 1));
            response = sec.oneSectionInFwd();
            if (!response.isOk()) {
                showError(statusHead + response + "-Some Problem in First Section");
            }
            if(s != iFirstFiredSec || copyForward) {
                nextSec = vSec.get(s + 1);
                nextSec.copyFromPrevSection(sec);
                setInitialChHeat(s + 1, uptoSec, sec.chEndTemp(), vSec.get(uptoSec + 1).chEntryTemp(), bBot);
                nextSec.fluePassThrough = flueFromDEndApprox(sec.getEnteringGasTemp(), s, bBot);
            }
        }
        return bRetVal;
    }

    boolean firstZoneInRev(boolean bBot) {
        boolean bRetVal = true;
        FceEvaluator.EvalStat lastResponse, response;
        String statusHead;
        Vector<FceSection> vSec;
        int[] fired;
        if (bBot) {
            fired = botFiredSections;
            vSec = botSections;
            statusHead = "Bottom Zone ";
        } else {
            fired = topFiredSections;
            vSec = topSections;
            statusHead = "Top Zone ";
        }
        int iFirstFiredSec = fired[0];
        FceSection sec, firstFsec, prevSec, entrySec;
        entrySec = vSec.get(0);
        double tempGZ1Assume;

        double trialDeltaT = (tuningParams.suggested1stCorrection > 0) ?
                tuningParams.suggested1stCorrection : 10;
        firstFsec = vSec.get(iFirstFiredSec);
        tempGZ1Assume = firstFsec.getLastSlotGasTemp();
        if (tempGZ1Assume <= 0)
            tempGZ1Assume = firstFsec.getEnteringGasTemp();
        if (bBot && bRecalculBot)
            tempGZ1Assume = tempGZ1Bot;
        if (!bBot && bRecalculTop)
            tempGZ1Assume = tempGZ1Top;
        boolean bTrial1 = true;
        boolean redoIt = false;
        lastResponse = FceEvaluator.EvalStat.OK;
        response = FceEvaluator.EvalStat.OK;
        boolean done = false;
        boolean loopBack = false;
        double tmNow, tmLast = 0;
        double diff, lastDiff = 0;
        double tgNow, tgLast = 0;
        double reqdChTempM = productionData.entryTemp;
        while (!done && canRun()) {
            redoIt = false;
            firstFsec.setLastSlotGasTemp(tempGZ1Assume);
            for (int s = iFirstFiredSec; s >= 0; s--) {
                showStatus(statusHead + (s + 1));
                sec = vSec.get(s);
                response = sec.oneSectionInRev();
                if (response != FceEvaluator.EvalStat.OK) {
                    if (response != lastResponse) {
                        lastResponse = response;
                        trialDeltaT = (tuningParams.suggested1stCorrection > 0) ?
                                tuningParams.suggested1stCorrection : 10;
                    }
                    if (response == FceEvaluator.EvalStat.TOOHIGHGAS) {
                        trialDeltaT *= 2;
                        tempGZ1Assume -= trialDeltaT;
                        loopBack = true;
                        break;
                    } else if (response == FceEvaluator.EvalStat.TOOLOWGAS) {
                        break;
                    }
                    else {
//                        showError("firstZoneInRev: Unknown response from oneSectionInRev() for " + statusHead + s);
                        redoIt = false;
                        bRetVal = false;
                        break;
                    }
                }
                if (s > 0) {
                    prevSec = vSec.get(s - 1);
                    prevSec.copyFromNextSection(sec);
                    if (tuningParams.bSectionalFlueExh) {
                        prevSec.fluePassThrough = 0;
                    } else {
                        FlueCompoAndQty netFlue = flueFromDEnd(bBot, s - 1);
                        prevSec.passFlueCompAndQty.noteValues(netFlue);
                        prevSec.fluePassThrough = netFlue.flow;
                    }
                } else {
                    if (!tuningParams.bSectionalFlueExh) {
                        flueFromDEnd(bBot, s - 1);
                    }
                }
            }

            if (bRetVal) {
                if (loopBack || redoIt) {
                    loopBack = false;
                    continue;
                }
                if (response != lastResponse) {
                    lastResponse = response;
                    trialDeltaT = (tuningParams.suggested1stCorrection > 0) ?
                            tuningParams.suggested1stCorrection : 10;
                }
                if (response == FceEvaluator.EvalStat.TOOLOWGAS) {
                    tempGZ1Assume += 10;
                    continue;
                }
                tmNow = entrySec.chEntryTemp();
                diff = tmNow - reqdChTempM;

//showError("WAITING TO CONTINUE tmNow = " + tmNow);
                if (Math.abs(diff) <= 1 * tuningParams.errorAllowed)
                    break;
                if (bTrial1) {
                    tgLast = tempGZ1Assume;
                    tmLast = tmNow;
                    tempGZ1Assume += (diff < 0) ? (-trialDeltaT) : trialDeltaT;
                    bTrial1 = false;
                } else {
                    tgNow = tempGZ1Assume;
                    tempGZ1Assume = tgNow + (tgNow - tgLast) / (tmNow - tmLast) * (reqdChTempM - tmNow);
                    if (Double.isNaN(tempGZ1Assume)) {
                        showError("Some problem in getting to charge entry temperature\n" +
                                "     Try with reduced losses in the first section ...");
                        bRetVal = false;
                        break;
                    }
                    tgLast = tgNow;
                    tmLast = tmNow;
                }
                lastDiff = diff;
            }
            else
                break;
        }
        if (bRetVal) {
            entrySec.setEntryChTemps(reqdChTempM, reqdChTempM, reqdChTempM);
            entrySec.showEntryResults();
        }
        return bRetVal;
    }


    boolean firstZoneInRevBEFORWE20170127(boolean bBot) {    // TODO to be removed (have replaced this with 20161130
        boolean bRetVal = true;
        double correctionForToooLowGas = tuningParams.correctionForTooLowGas;
        if (correctionForToooLowGas <= 0)
            correctionForToooLowGas = 10;
        FceEvaluator.EvalStat lastResponse, response;
        String statusHead;
        Vector<FceSection> vSec;
        int[] fired;
        if (bBot) {
            fired = botFiredSections;
            vSec = botSections;
            statusHead = "Bottom Zone ";
        } else {
            fired = topFiredSections;
            vSec = topSections;
            statusHead = "Top Zone ";
        }
        int iFirstFiredSec = fired[0];
        FceSection sec, firstFsec, prevSec, entrySec;
        entrySec = vSec.get(0);
        double tempGZ1Assume;

//        double trialDeltaT = (tuningParams.suggested1stCorrection > 0) ?
//                tuningParams.suggested1stCorrection : 10;
        double trialDeltaT = tuningParams.suggested1stCorrection;
        firstFsec = vSec.get(iFirstFiredSec);
        tempGZ1Assume = firstFsec.getLastSlotGasTemp();
        if (tempGZ1Assume <= 0)
            tempGZ1Assume = firstFsec.getEnteringGasTemp();
        if (bBot && bRecalculBot)
            tempGZ1Assume = tempGZ1Bot;
        if (!bBot && bRecalculTop)
            tempGZ1Assume = tempGZ1Top;
        boolean bTrial1 = true;
        boolean redoIt = false;
        lastResponse = FceEvaluator.EvalStat.OK;
        response = FceEvaluator.EvalStat.OK;
        boolean done = false;
        boolean loopBack = false;
        double tmNow, tmLast = 0;
        double diff, lastDiff = 0;
        double tgNow, tgLast = 0;
        double reqdChTempM = productionData.entryTemp;
        while (!done && canRun()) {
            redoIt = false;
            firstFsec.setLastSlotGasTemp(tempGZ1Assume);
            for (int s = iFirstFiredSec; s >= 0; s--) {
                showStatus(statusHead + (s + 1));
                sec = vSec.get(s);
                response = sec.oneSectionInRev();
                if (response != FceEvaluator.EvalStat.OK) {
                    if (response == FceEvaluator.EvalStat.TOOHIGHGAS) {
                        if (response == lastResponse)
                            trialDeltaT *= 2;
                        tempGZ1Assume -= trialDeltaT;
                        loopBack = true;
                        break;
                    } else if (response == FceEvaluator.EvalStat.TOOLOWGAS) {
                        if (response == lastResponse)
                            trialDeltaT *= 2;
                        tempGZ1Assume += trialDeltaT; 
                        loopBack = true;
                        break;
                    } else {
                        showError("firstZoneInRev: Unknown response from oneSectionInRev() for " + statusHead + s);
                        redoIt = true;
                        break;
                    }
                }
                if (response != lastResponse) {
                    lastResponse = response;
                    trialDeltaT = tuningParams.suggested1stCorrection;
                }
                if (s > 0) {
                    prevSec = vSec.get(s - 1);
                    prevSec.copyFromNextSection(sec);
                    if (tuningParams.bSectionalFlueExh) {
                        prevSec.fluePassThrough = 0;
                    } else {
                        FlueCompoAndQty netFlue = flueFromDEnd(bBot, s - 1);
                        prevSec.passFlueCompAndQty.noteValues(netFlue);
                        prevSec.fluePassThrough = netFlue.flow;
                    }
                } else {
                    if (!tuningParams.bSectionalFlueExh) {
                        flueFromDEnd(bBot, s - 1);
                    }
                }
            }

            if (loopBack || redoIt) {
                loopBack = false;
//                bTrial1 = true;
                continue;
            }
            tmNow = entrySec.chEntryTemp();
            diff = tmNow - reqdChTempM;

            if (Math.abs(diff) <= 1 * tuningParams.errorAllowed)
                break;
            if (bTrial1) {
                tgLast = tempGZ1Assume;
                tmLast = tmNow;
                tempGZ1Assume += (diff < 0) ? (-trialDeltaT) : trialDeltaT;
                bTrial1 = false;
            } else {
                tgNow = tempGZ1Assume;
                tempGZ1Assume = tgNow + (tgNow - tgLast) / (tmNow - tmLast) * (reqdChTempM - tmNow);
                tgLast = tgNow;
                tmLast = tmNow;
            }
            lastDiff = diff;
        }
        entrySec.setEntryChTemps(reqdChTempM, reqdChTempM, reqdChTempM);

        entrySec.showEntryResults();
        return bRetVal;
    }


    void showStatus(String msg) {
        if (bDisplayResults)
            masterEvaluator.showStatus(msg);
    }

    double getCommonFuelUsed(boolean bBot) {
        Vector<FceSection> vSec = getVsecs(bBot);
        double fUsed = 0;
        for (FceSection sec : vSec)
            fUsed += sec.getCommonFuelUsed();
        return fUsed;
    }

    double getCommonFuelUsed() {
        double fUsed = getCommonFuelUsed(false);
        if (bTopBot)
            fUsed += getCommonFuelUsed(true);
        return fUsed;
    }

    JPanel heatSummary, topSecwiseP, botSecwiseP, topResultsP, botResultsP, combiResultsP;
    JPanel topTrendsP, botTrendsP, combiTrendsP;
    JPanel recuBalance;
    TrendsPanel topTrends, botTrends, combiTrends;
    JPanel fuelSummaryP, topFuelsP, botFuelsP;
    JPanel heatSummaryInt, topSecwiseInt, botSecwiseInt, topResultsInt, botResultsInt, combiResultsInt;


    public double chTempIN;
    double chHeatIN, fuelFlow;
    double heatFromComb, heatFromFuelSens;
    public double commonAirTemp;
    double commonAirFlow, commonAirHeat;
    double totAirFlow, totAirHeat;
    double airToRegenBur, regenAirHeat;
    double totHeatIn, totHeatOut;
    public double chTempOUT;
    double chHeatOUT;
    public double totLosses;
    public double flueTempOUT;
    double flueFlow, heatToFlue;
    double spHeatCons;
    double effChHeatVsFuel, effChHeatAndLossVsFuel;
    double regenFlueQty, regenFlueHeat;

    FuelNameAndFlow baseFuelDet, addedFuelDet;

    FuelsAndUsage fuelUsage;
    Vector<Fuel> uniqueFuels;
    FlueCompoAndQty flueToRecu;
    FlueCompoAndQty flueToRegen;

    boolean prepareHeatBalance() {
        // Heat IN
        for (int s = 0; s < nTopActiveSecs; s++)
            topSections.get(s).prepareHeatbalance();
        if (bTopBot) {
            for (int s = 0; s < nBotActiveSecs; s++)
                botSections.get(s).prepareHeatbalance();
            if (bAddTopSoak)
                addedTopSoak.prepareHeatbalance();
        }
        chTempIN = productionData.entryTemp;
        chHeatIN = productionData.production * productionData.charge.getHeatFromTemp(chTempIN);
        commonAirTemp = commFuelFiring.airTemp;

        fuelFlow = totalFuel();
        baseFuelDet = commFuelFiring.baseFuelNameAndFlow(fuelFlow);
        addedFuelDet = commFuelFiring.addedFuelNameAndFlow(fuelFlow);

        fuelUsage = getFuelsAndUsage();

        getFuelPanels();
        setSecFuelsPanel(false);
        if (bTopBot)
            setSecFuelsPanel(true);
        heatFromComb = fuelUsage.totComBustHeat();
        heatFromFuelSens = fuelUsage.totFuelSensHeat();

        commonAirFlow = getCommonAirFlow();

        commonAirHeat = getCommonAirHeat(); //fuelUsage.totAirSensHeat(false);
        airToRegenBur = fuelUsage.totAirFlow(true);
        regenAirHeat = getRegenAirHeat(); // fuelUsage.totAirSensHeat(true);
        totAirFlow = commonAirFlow + airToRegenBur;
        totAirHeat = commonAirHeat + regenAirHeat;
        totHeatIn = chHeatIN + heatFromComb + heatFromFuelSens + totAirHeat;

        // Heat OUT
        chTempOUT = chTempOut();
        chHeatOUT = productionData.production * productionData.charge.getHeatFromTemp(chTempOUT);

        totLosses = losses();

        flueToRecu = getFceExitFlue();
        flueFlow = flueToRecu.flow;
        flueTempOUT = flueToRecu.flueTemp;
        heatToFlue = flueToRecu.flueHeat;

        FlueFlowTempHeat regenFlue;
        if (anyRegen() > 0) {
            regenFlue = getRegenFlueFTH();
            regenFlueQty = regenFlue.flow;
            regenFlueHeat = regenFlue.heat;
        } else {
            regenFlueQty = 0;
            regenFlueHeat = 0;
        }
        totHeatOut = chHeatOUT + totLosses + heatToFlue + regenFlueHeat;
        spHeatCons = heatFromComb / productionData.production;
        effChHeatVsFuel = (chHeatOUT - chHeatIN) / heatFromComb;
        effChHeatAndLossVsFuel = ((chHeatOUT - chHeatIN) + totLosses) / heatFromComb;
        prepareRecuBalance();
        heatSummary = heatSummaryPanel();
        return true;
//        if (prepareRecuBalance()) {
//            heatSummary = heatSummaryPanel();
//            return true;
//        }
//        else
//            return false;
    }

    FlueCompoAndQty flueAfterRecu;
    protected HeatExchProps existingHeatExch;
    protected Recuperator airRecu = null, fuelRecu = null;

    boolean checkExistingRecu() {
        boolean retVal = false;
        if (existingHeatExch != null) {
            if (controller.bAirHeatedByRecu) {
                retVal = perfBaseReady || chTempProfAvailable ||
                        decide("Existing Recuperator", " Do you want to use the existing Air Recuperator ?");

            }
            else {
                showMessage("DELETING Existing Recuperator Data, \n" +
                                "since the Common Air Recuperator has been de-selected");
                existingHeatExch = null;
            }
        }
        return retVal;
//        return (controller.bAirHeatedByRecu && existingHeatExch != null &&
//                (perfBaseReady || chTempProfAvailable ||
//                        decide("Existing Recuperator", " Do you want to use the existing Air Recuperator ?")));
    }

    public void newRecu() {
        existingHeatExch = null;
        bRecuFrozen = false;
    }

    boolean prepareRecuBalance() {
        boolean retVal = false;
        recuBalance = null;
        flueAfterRecu = null;
        if (!controller.bAirHeatedByRecu && !controller.bFuelHeatedByRecu)
            return retVal;
        retVal = true;
        FlueCompoAndQty flue1 = new FlueCompoAndQty("Flue at Recu", flueToRecu);
        // cool it by deltFtemp between fce and recu
        flue1.coolIt(controller.deltaTflue);
        boolean bEntryDone = false; // to check if max flue temp at recu is taken care
        if (controller.bFuelHeatedByRecu && controller.bAirHeatedByRecu) {  // if both
            if (controller.bAirAfterFuel) {
                fuelRecu = createFuelRecu(flue1, bEntryDone);
                bEntryDone = true;
                airRecu = createAirRecu(fuelRecu.getFlueAftRecu(), bEntryDone);
                if (airRecu.bInError) {
                    showError("Could not create Air Recu for Recu balance 01 (DFhFurnace)");
                    retVal = false;
                }
                else {
                    flueAfterRecu = airRecu.getFlueAftRecu();
                }
            } else {
                airRecu = createAirRecu(flue1, bEntryDone);
                if (airRecu.bInError) {
                    showError("Could not create Air Recu for Recu balance 02 (DFhFurnace)");
                    retVal = false;
                }
                else {
                    bEntryDone = true;
                    fuelRecu = createFuelRecu(airRecu.getFlueAftRecu(), bEntryDone);
                    flueAfterRecu = fuelRecu.getFlueAftRecu();
                }
            }
        } else {
            if (controller.bAirHeatedByRecu) {
                airRecu = createAirRecu(flue1, bEntryDone);
                if (airRecu.bInError) {
                    showError("Could not create Air Recu for Recup balance 03 (DFhFurnace)");
                    retVal = false;
                }
                else {
                    flueAfterRecu = airRecu.getFlueAftRecu();
                }
            }
            if (controller.bFuelHeatedByRecu && !commFuelFiring.fuel.isbMixedFuel()) {
                fuelRecu = createFuelRecu(flue1, bEntryDone);
                flueAfterRecu = fuelRecu.getFlueAftRecu();
            }
        }
        if (retVal)
            recuBalance = recuBalanceP();
        return retVal;
    }

    public boolean anyMixedFuel() {
        boolean retVal = false;
        for (FceSection sec : topSections)
            if (sec.isMixedFuel()) {
                retVal = true;
                break;
            }
        if (bTopBot && !retVal) {
            for (FceSection sec : botSections)
                if (sec.isMixedFuel()) {
                    retVal = true;
                    break;
                }
        }
        return retVal;
    }

    Recuperator createAirRecu(FlueCompoAndQty flue, boolean bEntryDone) {
        double airTempIn = controller.ambTemp;
        double airTempBurner = controller.airTemp;
        double airTempLoss = controller.deltaTAirFromRecu;
        double airTempAfterRecu;
        Recuperator recuperator;
        if (existingHeatExch != null)
            recuperator = new Recuperator(existingHeatExch, flue, ((bEntryDone) ? flue.flueTemp : controller.maxFlueAtRecu),
                    new FlueComposition(true), commonAirFlow, airTempIn, getAmbTemp());
        else {
            airTempAfterRecu = airTempBurner + airTempLoss;
            recuperator =
                    new Recuperator(flue, ((bEntryDone) ? flue.flueTemp : controller.maxFlueAtRecu), commonAirFlow,
                            airTempIn, airTempAfterRecu, getAmbTemp());
        }
        if (recuperator.bInError)
            showError("AIR RECUPERATOR: " + recuperator.errMsg);
        return recuperator;
    }

    Recuperator createFuelRecu(FlueCompoAndQty flue, boolean bEntryDone) {
        Recuperator recuperator = null;
        double fUsed = getCommonFuelUsed();
        if (fUsed > 0) {
            double fuelTempIn = controller.ambTemp;
            double fuelTempBurner = controller.fuelTemp;
            double fuelTempLoss = controller.deltaTFuelFromRecu;
            double fuelTempAfterRecu = fuelTempBurner + fuelTempLoss;
            recuperator = new Recuperator(flue, ((bEntryDone) ? flue.flueTemp : controller.maxFlueAtRecu), commFuelFiring.fuel,
                    fUsed, fuelTempIn, fuelTempAfterRecu, getAmbTemp());
            if (recuperator.bInError)
                showError("FUEL RECUPERATOR: " + recuperator.errMsg);
        }
        return recuperator;
    }

    double getCommonAirFlow() {
        double air = 0;
        for (int s = 0; s < nTopActiveSecs; s++)
            air += topSections.get(s).getAirToCommonRecu();
        if (bTopBot) {
            for (int s = 0; s < nBotActiveSecs; s++)
                air += botSections.get(s).getAirToCommonRecu();
        }
        return air;
    }

    double getRegenAirFlow() {
        double air = 0;
        for (int s = 0; s < nTopActiveSecs; s++)
            air += topSections.get(s).getAirToRegen();
        if (bTopBot) {
            for (int s = 0; s < nBotActiveSecs; s++)
                air += botSections.get(s).getAirToRegen();
        }
        return air;
    }

    double getCommonAirHeat() {
        double heat = 0;
        for (int s = 0; s < nTopActiveSecs; s++)
            heat += topSections.get(s).getAirHeatCommonRecu();
        if (bTopBot) {
            for (int s = 0; s < nBotActiveSecs; s++)
                heat += botSections.get(s).getAirHeatCommonRecu();
        }
        return heat;
    }

    double getRegenAirHeat() {
        double heat = 0;
        for (int s = 0; s < nTopActiveSecs; s++)
            heat += topSections.get(s).getAirHeatRegen();
        if (bTopBot) {
            for (int s = 0; s < nBotActiveSecs; s++)
                heat += botSections.get(s).getAirHeatRegen();
        }
        return heat;
    }

    FuelsAndUsage getFuelsAndUsage() {
        FuelsAndUsage fuelsAndUsage = new FuelsAndUsage();
        uniqueFuels = new Vector<Fuel>();
        FceSection sec;
        for (int s = 0; s < nTopActiveSecs; s++) {
            sec = topSections.get(s);
            sec.addFuelUsage(fuelsAndUsage);
            sec.addUniqueFuels(uniqueFuels);
        }
        if (bTopBot) {
            for (int s = 0; s < nBotActiveSecs; s++) {
                sec = botSections.get(s);
                sec.addFuelUsage(fuelsAndUsage);
                sec.addUniqueFuels(uniqueFuels);
            }
            if (bAddTopSoak) {
                addedTopSoak.addFuelUsage(fuelsAndUsage);
                addedTopSoak.addUniqueFuels(uniqueFuels);
            }
        }
        return fuelsAndUsage;
    }

    int anyRegen(boolean bBot) {
        Vector<FceSection> vSec = getVsecs(bBot);
        int nActive = (bBot) ? nBotActiveSecs : nTopActiveSecs;
        boolean bRegen = false;
        int count = 0;
        for (int s = 0; s < nActive; s++) {
            bRegen = vSec.get(s).isWithRegen();
            if (bRegen)
                count++;
        }
        return count;
    }

    int anyIndividualFuel(boolean bBot) {
        Vector<FceSection> vSec = getVsecs(bBot);
        int nActive = (bBot) ? nBotActiveSecs : nTopActiveSecs;
        boolean bIndivFuel = false;
        int count = 0;
        for (int s = 0; s < nActive; s++) {
            bIndivFuel = vSec.get(s).bIndividualFuel;
            if (bIndivFuel) count++;
        }
        return count;
    }

    int anyRegen() {
        return anyRegen(false) + ((bTopBot) ? anyRegen(true) : 0);
    }

    int anyIndividualFuel() {
        return anyIndividualFuel(false) + ((bTopBot) ? anyIndividualFuel(true) : 0);
    }

    FlueFlowTempHeat getRegenFlueFTH(boolean bBot) {
        Vector<FceSection> vSec = getVsecs(bBot);
        FlueFlowTempHeat fth = new FlueFlowTempHeat();
        FceSection sec;
        for (int s = 0; s < vSec.size(); s++) {
            sec = vSec.get(s);
            if (sec.isWithRegen())
                fth.addFlue(sec.regenFlue, sec.regenFlueHeat);
        }
        return fth;
    }

    /*
    WARNING: This does not evaluate proper combine flues temperature
     */
    FlueFlowTempHeat getRegenFlueFTH() {
        FlueFlowTempHeat fth = getRegenFlueFTH(false);
        if (bTopBot)
            fth.addFlue(getRegenFlueFTH(true));
        return fth;
    }


    FlueCompoAndQty getFceExitFlue(boolean bBot) {
        FceSection sec = (bBot) ? botSections.get(0) : topSections.get(0);  // get the first section
        return sec.totFlueCompAndQty;
    }

    FlueCompoAndQty getFceExitFlue() {
        if (bTopBot)
            return new FlueCompoAndQty("Combined Flue (Top And Bot)", getFceExitFlue(false), getFceExitFlue(true));
        else
            return getFceExitFlue(false);
    }


    int valLabelW = 80;
    int labelWidth = 200;

    JPanel heatSummaryPanel() {
        JPanel outerP = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        JPanel jp = new JPanel(layout);
        Color background = new JPanel().getBackground();
        jp.setBackground(background);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        jp.add(spHeatConPan(), gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        jp.add(pChargeHeatIn(), gbc);
        gbc.gridy++;
        jp.add(pFuelHeatIn(), gbc);
        gbc.gridy++;
        jp.add(pAirHeatIn(), gbc);
        gbc.gridy++;
        jp.add(pTotalHeatIn(), gbc);

        gbc.gridx++;
        gbc.gridy = 1;

        jp.add(pChargeHeatOut(), gbc);
        gbc.gridy++;
        jp.add(pLossedOut(), gbc);
        gbc.gridy++;
        jp.add(pFlueHeatOut(), gbc);
        gbc.gridy++;
        jp.add(pTotalHeatOut(), gbc);

        gbc.gridx = 0;

        heatSummaryInt = jp;
        outerP.add(jp);
        return outerP;
    }

    protected JRadioButton recuCounterFlow;
    JButton jbFreezeAirRecu;
    boolean bRecuFrozen = false;
    JButton saveRecuperator;

    protected boolean freezeAirRecu() {
        boolean retVal = false;
        if (airRecu != null && existingHeatExch == null) {
            existingHeatExch = airRecu.getHeatExchProps(recuCounterFlow.isSelected());
            if (existingHeatExch.canPerform) {
                if (jbFreezeAirRecu != null) {
                    jbFreezeAirRecu.setEnabled(false);
                    recuCounterFlow.setEnabled(false);
                }
                bRecuFrozen = true;
                showMessage("Air Recuperator characteristics frozen");
                retVal = true;
            } else {
                existingHeatExch = null;
                showError("Unable to freeze Recuperator!");
            }
        }
        return retVal;
    }

    boolean freezeExistingHeatExch() {
        bRecuFrozen = false;
        if (existingHeatExch.canPerform) {
            if (jbFreezeAirRecu != null) {
                jbFreezeAirRecu.setEnabled(false);
                recuCounterFlow.setEnabled(false);
            }
            bRecuFrozen = true;
            showMessage("Air Recuperator characteristics frozen");
         }
        return bRecuFrozen;
    }

    boolean saveRecuToFile() {
        boolean retVal = false;
        HeatExchProps heProps;

        if (airRecu != null) {
            if (existingHeatExch == null)
                heProps = airRecu.getHeatExchProps(recuCounterFlow.isSelected());
            else
                heProps = existingHeatExch;
            retVal = controller.saveRecuToFile(heProps.dataInXML());
        }
        return retVal;
    }

    public boolean setRecuSpecs(String xmlStr) {
        boolean retVal = false;
        boolean proceed = true;
        if (bRecuFrozen) {
            if (!controller.decide("Recuperator", "Recuperator specifications already exists." +
                    "\n   Do you want to OVERWRITE it?"))
                proceed = false;
        }
        if (proceed) {
            if (performBase != null && performBase.isValid()) {
                if (!controller.decide("Performance Data", "The Performance data are with the  existing Recuperator" +
                        "\nLoading a new Recuperator might affect the performance data" +
                        "\n     Do you still want to proceed loading the new Recuperator?"))
                    proceed = false;
            }
            if (proceed) {
                HeatExchProps hExch = new HeatExchProps();
                if (hExch.takeDataFromXML(xmlStr)) {
                    existingHeatExch = hExch;
                    bRecuFrozen = true;
                    retVal = true;
                }
            }
        }
        return retVal;
    }

    class RecuBalanceListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object caller = e.getSource();
            if (caller == jbFreezeAirRecu)
                freezeAirRecu();
            else if (caller == saveRecuperator) {
                if (saveRecuToFile())
                    saveRecuperator.setEnabled(false);
            }

        }
    }

    RecuBalanceListener recuBalL = new RecuBalanceListener();

    JPanel recuBalanceP() {
        recuCounterFlow = new JRadioButton("Counter Flow Type", true);
        jbFreezeAirRecu = new JButton("Freeze Air Recuperator Specification");
        jbFreezeAirRecu.setEnabled(!bRecuFrozen);
        saveRecuperator = new JButton("Save Recuperator");
        JPanel outerP = new FramedPanel();
        JPanel innerP = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel titleP = new JPanel();
        boolean both = controller.bAirHeatedByRecu && controller.bFuelHeatedByRecu && !commFuelFiring.fuel.isbMixedFuel();
        titleP.add(getTopBotTileP("HEAT BALANCE OF RECUPERATOR" + ((both) ? "S" : "")));
        gbc.gridx = 0;
        gbc.gridy = 0;
        innerP.add(titleP, gbc);
        gbc.gridy++;
        if (controller.bAirHeatedByRecu && !controller.bAirAfterFuel) {
            innerP.add(oneRecuPanel(airRecu, false), gbc);
            gbc.gridy++;
        }
        if (controller.bFuelHeatedByRecu && !commFuelFiring.fuel.isbMixedFuel()) {
            innerP.add(oneRecuPanel(fuelRecu, true), gbc);
            gbc.gridy++;
        }
        if (controller.bAirHeatedByRecu && controller.bAirAfterFuel) {
            innerP.add(oneRecuPanel(airRecu, false), gbc);
            gbc.gridy++;
        }
        if (!controller.bFuelHeatedByRecu) {
            jbFreezeAirRecu.addActionListener(recuBalL);
            saveRecuperator.addActionListener(recuBalL);
            if (existingHeatExch != null) {
                bRecuFrozen = true;
                jbFreezeAirRecu.setEnabled(false);
                recuCounterFlow.setEnabled(false);
            }
            JPanel jp = new JPanel();
            jp.add(jbFreezeAirRecu);
            jp.add(saveRecuperator);
            jp.add(recuCounterFlow);
            innerP.add(jp, gbc);
        }
        outerP.add(innerP);
        return outerP;
    }


    JPanel oneRecuPanel(Recuperator recu, boolean bFuel) {
        FramedPanel pan = new FramedPanel(new BorderLayout());
        pan.add(new JLabel(((bFuel) ? "Fuel" : "Air") + " Recuperator"), BorderLayout.NORTH);
        RecuMultiColPanels mp;
        if (bFuel)
            mp = (mpFuelRecu = new RecuMultiColPanels(commFuelFiring.fuel.name, recu));
        else
            mp = (mpAirRecu = new RecuMultiColPanels("Air", recu));
        pan.add(mp.panel, BorderLayout.CENTER);
        return pan;
    }

    JPanel getRecuDataP(String fluidName, Recuperator recu, RecuMultiColPanels rmp) {
        JPanel p = recu.recuBalanceP("Flue", fluidName);
        rmp.flue = recu.getMpRecuFlue();
        rmp.fluid = recu.getMpRecuHeated();
        rmp.compoBefore = recu.mpCompoFlueIn("Flue Before Dilution");
        rmp.compoAfter = recu.mpCompoFlueOut("Flue After Recuperator");
        return p;
    }

    class RecuMultiColPanels {
        MultiPairColPanel flue, fluid;
        MultiPairColPanel compoBefore, compoAfter;
        JPanel panel;

        RecuMultiColPanels(String fluidName, Recuperator recu) {
            panel = recu.recuBalanceP("Flue", fluidName);
            flue = recu.getMpRecuFlue();
            fluid = recu.getMpRecuHeated();
            compoBefore = recu.mpCompoFlueIn("Flue Before Dilution");
            compoAfter = recu.mpCompoFlueOut("Flue After Recuperator");
        }
    }

    RecuMultiColPanels mpAirRecu, mpFuelRecu;
    MultiPairColPanel mpAirRecuFlue, mpAirRecuAir;
    MultiPairColPanel mpCompoBefDilu, mpCompoAftRecu;

    MultiPairColPanel mpFuelRecuFlue, mpFuelRecuFuel;

    NumberLabel nlSpHeatCon, nlEff1, nlEff2;
    MultiPairColPanel mPspheatConPan;

    JPanel spHeatConPan() {
        int labW = labelWidth + 130;
        MultiPairColPanel jp = new MultiPairColPanel("Summary", labW, valLabelW);
        nlSpHeatCon = new NumberLabel(controller, spHeatCons, valLabelW, "#,###", true);
        nlEff1 = new NumberLabel(controller, effChHeatVsFuel * 100, valLabelW, "##.00");
        nlEff2 = new NumberLabel(controller, effChHeatAndLossVsFuel * 100, valLabelW, "##.00");
        if (anyIndividualFuel() > 0)
            jp.addItemPair("Fuel Name", "See Fuel Summary", false);
        else
            jp.addItemPair("Fuel Name", new JLabel(commFuelFiring.fuel.name));
        jp.addItemPair("Specific Heat Consumption (kcal/kg)", nlSpHeatCon, true);
        jp.addItemPair("Efficiency(ChHeat to Fuel Combustion Heat) (%)", nlEff1);
        jp.addItemPair("Efficiency((ChHeat + Losses) to Fuel Combustion Heat) (%)", nlEff2);
        mPspheatConPan = jp;
        return jp;
    }

    MultiPairColPanel mPfuelMixPan;
    MultiPairColPanel mPchargeHeatIn;

    JPanel pChargeHeatIn() {
        MultiPairColPanel jp = new MultiPairColPanel("Charge Heat-In", labelWidth, valLabelW);
        NumberLabel nlProduction, nlTempIn, nlHeatIn;
        nlProduction = new NumberLabel(controller, productionData.production, valLabelW, "#,###");
        nlTempIn = new NumberLabel(controller, chTempIN, valLabelW, "#,###");
        nlHeatIn = new NumberLabel(controller, chHeatIN, valLabelW, "#,###", true);

        jp.addItemPair("Production (kg/h)", nlProduction);
        jp.addItemPair("Temperature IN (degC)", nlTempIn);
        jp.addItemPair("Heat from charge (kcal/h)", nlHeatIn, true);
        mPchargeHeatIn = jp;
        return jp;
    }

    MultiPairColPanel mPfuelHeatIn;

    JPanel pFuelHeatIn() {
        MultiPairColPanel jp = new MultiPairColPanel("Fuel Heat-In", labelWidth + 20, valLabelW);
        NumberLabel nlFuelFlow, nlTempIn, nlCombHeat, nlSensHeat;
        nlFuelFlow = new NumberLabel(controller, baseFuelDet.fuelFlow, valLabelW, "#,###.##");
        nlTempIn = new NumberLabel(controller, baseFuelDet.temperature, valLabelW, "#,###");
        if (anyIndividualFuel() > 0) {
            jp.addItemPair("Fuel Flow ", "see Fuel Summary", false);
            jp.addItemPair("Fuel Temperature", "see Fuel Summary", false);
        } else {
            if (commFuelFiring.fuel.bMixedFuel) {
                jp.addItemPair("Base Fuel Flow(" + baseFuelDet.units + "/h)", nlFuelFlow);
                jp.addItemPair("Fuel Temperature", "see Fuel Summary", false);
            } else {
                jp.addItemPair("Fuel Flow(" + baseFuelDet.units + "/h)", nlFuelFlow);
                jp.addItemPair("Fuel Temperature(degC)", nlTempIn);
            }
        }
        nlSensHeat = new NumberLabel(controller, heatFromFuelSens, valLabelW, "#,###", true);
        nlCombHeat = new NumberLabel(controller, heatFromComb, valLabelW, "#,###", true);
        jp.addItemPair("Fuel Sensible heat (kcal/h)", nlSensHeat, true);
        jp.addItemPair("Fuel Combustion heat (kcal/h)", nlCombHeat, true);
        mPfuelHeatIn = jp;
        return jp;
    }

    MultiPairColPanel mPairHeatIn;

    JPanel pAirHeatIn() {
        MultiPairColPanel jp = new MultiPairColPanel("Air Heat-In", labelWidth, valLabelW);
        NumberLabel nlCommAirFlow, nlTempIn, nlCommSensHeat;
        NumberLabel nltotAirFlow, nlTotAirHeat;
        NumberLabel nlRegenAirFlow, nlRegenSensHeat;

        nlCommAirFlow = new NumberLabel(controller, commonAirFlow, valLabelW, "#,###.##");
        nlTempIn = new NumberLabel(controller, commonAirTemp, valLabelW, "#,###");
        nlCommSensHeat = new NumberLabel(controller, commonAirHeat, valLabelW, "#,###", true);
        jp.addItemPair("Common Air Flow (m3N/h)", nlCommAirFlow);
        jp.addItemPair("Common Air Temperature (C)", nlTempIn);
        jp.addItemPair("Sens. Heat of Common Air (kcal/h)", nlCommSensHeat, true);
        if ((anyRegen() > 0) && tuningParams.bAllowRegenAirTemp) {
            nlRegenAirFlow = new NumberLabel(controller, airToRegenBur, valLabelW, "#,###.##");
            nlRegenSensHeat = new NumberLabel(controller, regenAirHeat, valLabelW, "#,###", true);
            jp.addItemPair("Regen Air Flow (m3N/h)", nlRegenAirFlow);
            jp.addItemPair("Regen Air Temperature", "see Fuel Summary", false);
            jp.addItemPair("Sen. Heat of Regen Air (kcal/h)", nlRegenSensHeat, true);
        }
        mPairHeatIn = jp;
        return jp;
    }

    MultiPairColPanel mPchargeHeatOut;

    JPanel pChargeHeatOut() {
        MultiPairColPanel jp = new MultiPairColPanel("Charge Heat - Out", labelWidth, valLabelW);
        NumberLabel nlProduction, nlTempOut, nlHeatOut;
        nlProduction = new NumberLabel(controller, productionData.production, valLabelW, "#,###");
        nlTempOut = new NumberLabel(controller, chTempOUT, valLabelW, "#,###");
        nlHeatOut = new NumberLabel(controller, chHeatOUT, valLabelW, "#,###", true);

        jp.addItemPair("Production (kg/h)", nlProduction);
        jp.addItemPair("Temperature OUT (degC)", nlTempOut);
        jp.addItemPair("Heat to charge (kcal/h)", nlHeatOut, true);
        mPchargeHeatOut = jp;
        return jp;
    }

    MultiPairColPanel mPlossesOut;

    JPanel pLossedOut() {
        MultiPairColPanel jp = new MultiPairColPanel("Losses", labelWidth, valLabelW);
        combiLossValList.addToMulipairPanel(jp);
        NumberLabel nlLosses;
        nlLosses = new NumberLabel(controller, totLosses, valLabelW, "#,###", true);
        jp.addItemPair("Total Losses (kcal/h)", nlLosses, true);
        mPlossesOut = jp;
        return jp;
    }

    MultiPairColPanel mPflueHeatOut;

    JPanel pFlueHeatOut() {
        MultiPairColPanel jp = new MultiPairColPanel("Flue Heat - Out", labelWidth, valLabelW);
        NumberLabel nlFuelFlow, nlFlueFlow, nlTempOut, nlHeatOut;
        nlFlueFlow = new NumberLabel(controller, flueFlow, valLabelW, "#,###");
        nlTempOut = new NumberLabel(controller, flueTempOUT, valLabelW, "#,###");
        nlHeatOut = new NumberLabel(controller, heatToFlue, valLabelW, "#,###", true);

        jp.addItemPair("Furnace Flue Flow  (m3N/h)", nlFlueFlow);
        jp.addItemPair("Temperature of Flue (degC)", nlTempOut);
        jp.addItemPair("Heat to Furnace Flue (kcal/h)", nlHeatOut, true);
        if (anyRegen() > 0) {
            jp.addBlank();
            NumberLabel nlRegenFlueQty = new NumberLabel(controller, regenFlueQty, valLabelW, "#,###");
            NumberLabel nlRegenFlueHeat = new NumberLabel(controller, regenFlueHeat, valLabelW, "#,###", true);
            jp.addItemPair("Regen Flue Flow (m3N/h)", nlRegenFlueQty);
            jp.addItemPair("Heat to Regen Flue (kcal/h)", nlRegenFlueHeat, true);
        }
        mPflueHeatOut = jp;
        return jp;
    }

    MultiPairColPanel mPtotHeatIn;

    JPanel pTotalHeatIn() {
        MultiPairColPanel jp = new MultiPairColPanel(labelWidth, valLabelW);
        NumberLabel nlTotHeatIN;
        nlTotHeatIN = new NumberLabel(controller, totHeatIn, valLabelW, "#,###", true);
        jp.addItemPair("Total Heat IN (kcal/h)", nlTotHeatIN, true);
        mPtotHeatIn = jp;
        return jp;
    }

    MultiPairColPanel mPtotHeatOut;

    JPanel pTotalHeatOut() {
        MultiPairColPanel jp = new MultiPairColPanel(labelWidth, valLabelW);
        NumberLabel nlTotHeatOUT;
        nlTotHeatOUT = new NumberLabel(controller, totHeatOut, valLabelW, "#,###", true);
        jp.addItemPair("Total Heat OUT (kcal/h)", nlTotHeatOUT, true);
        mPtotHeatOut = jp;
        return jp;
    }

    Vector<XLcellData> fuelSummTopTexts;

    void getFuelPanels() {
        fuelSummTopTexts = new Vector<XLcellData>();
        fuelSummaryP = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        int nRegen = anyRegen();
        int nIndivFuel = anyIndividualFuel();
        TextLabel l1, l2;
        if (nRegen > 0) {
            if (nRegen > 1)
                l1 = new TextLabel("There are " + nRegen + " sections with REGEN burners.", false);
            else
                l1 = new TextLabel("There is " + nRegen + " section with REGEN burners.", false);
        } else
            l1 = new TextLabel("All Sections are with NORMAL burners.", false);

        if (nIndivFuel > 0) {
            if (nIndivFuel > 1)
                l2 = new TextLabel("There are " + nIndivFuel + " sections with Individual Fuels.", false);
            else
                l2 = new TextLabel("There is " + nIndivFuel + " section with Individual Fuel.", false);
        } else
            l2 = new TextLabel("All fired sections are with a COMMON Fuel.", false);
        fuelSummaryP.add(l1, gbc);
        fuelSummTopTexts.add(l1);
        gbc.gridy++;
        fuelSummaryP.add(l2, gbc);
        fuelSummTopTexts.add(l2);
        gbc.gridy++;
        fuelSummaryP.add(fuelUsage.getSummaryPanel("FUEL DETAILS", (nRegen == 0), (nIndivFuel == 0), commonAirTemp, true), gbc);
    }

    void setSecFuelsPanel(boolean bBot) {
        FramedPanel outerP = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        Vector<FceSection> vSec = getVsecs(bBot);
        int nActive = (bBot) ? nBotActiveSecs : nTopActiveSecs;
        String title = "Fuel Details for " + topBotName(bBot) + "Zone ";
        FceSection sec;
        for (int s = 0; s < nActive; s++) {
            sec = vSec.get(s);
            if (!sec.bRecuType) {
                outerP.add(sec.fuelSummaryPanel(title + (s + 1)), gbc);
                gbc.gridx++;
            }
        }
        if (bTopBot && !bBot && bAddTopSoak) {
            outerP.add(addedTopSoak.fuelSummaryPanel(title + "Added Soak"), gbc);
            gbc.gridx++;
        }
        if (bBot)
            botFuelsP = outerP;
        else
            topFuelsP = outerP;
    }

    double totalFuel() {
        double tot = totalFuel(false);
        if (bTopBot)
            tot += totalFuel(true);
        return tot;
    }

    double totalFuel(boolean bBot) {
        Vector<FceSection> vSec;
        int[] fired;
        int nFired;
        if (bBot) {
            vSec = botSections;
            fired = botFiredSections;
            nFired = nBotFired;
        } else {
            vSec = topSections;
            fired = topFiredSections;
            nFired = nTopFired;
        }
        double tot = 0;
        for (int f = 0; f < nFired; f++)
            tot += vSec.get(fired[f]).secFuelFlow;
        if (bTopBot && !bBot && bAddTopSoak)
            tot += addedTopSoak.secFuelFlow;
        return tot;
    }

    double airFlow() {
        double tot = airFlow(false);
        if (bTopBot)
            tot += airFlow(true);
        return tot;
    }

    double airFlow(boolean bBot) {
        Vector<FceSection> vSec;
        int[] fired;
        int nFired;
        if (bBot) {
            vSec = botSections;
            fired = botFiredSections;
            nFired = nBotFired;
        } else {
            vSec = topSections;
            fired = topFiredSections;
            nFired = nTopFired;
        }
        double tot = 0;
        for (int f = 0; f < nFired; f++)
            tot += vSec.get(fired[f]).airFlow();
        return tot;
    }

    double chTempOut() {
        double chT = chTempOut(false);
        if (bTopBot) {
            chT = (bAddTopSoak || topOnlyDEnd) ? chT : (chT + chTempOut(true)) / 2;
        }
        return chT;
    }

    double chTempOut(boolean bBot) {
        double chT;
        if (bBot)
            chT = botSections.get(nBotActiveSecs - 1).chEndTemp();
        else {
            if (bTopBot && bAddTopSoak)
                chT = addedTopSoak.chEndTemp();
            else
                chT = topSections.get(nTopActiveSecs - 1).chEndTemp();
        }
        return chT;
    }

    double losses() {
        double tot = losses(false);
        combiLossValList = new LossListWithVal(lossTypeList);
        topLossValList.addToList(combiLossValList);
        if (bTopBot) {
            tot += losses(true);
            botLossValList.addToList(combiLossValList);
        }

        return tot;
    }

    double losses(boolean bBot) {
        Vector<FceSection> vSec;
        int nActive;
        LossListWithVal listWithVal;
        if (bBot) {
            vSec = botSections;
            nActive = activeSections(true);
            botLossValList = new LossListWithVal(lossTypeList);
            listWithVal = botLossValList;
        } else {
            vSec = topSections;
            nActive = activeSections(false);
            topLossValList = new LossListWithVal(lossTypeList);
            listWithVal = topLossValList;
        }
        double tot = 0;
        LossListWithVal secLossList;
        for (int s = 0; s < nActive; s++) {
            secLossList = vSec.get(s).getLossListWithVal();
            secLossList.addToList(listWithVal);
        }
        if (bTopBot && !bBot && bAddTopSoak) {
            secLossList = addedTopSoak.getLossListWithVal();
            secLossList.addToList(listWithVal);
        }

        return tot = listWithVal.getTotal();
    }

    Vector<XLcellData> lossSummTopXL, lossSummBotXL, lossSummTotXL;

    JPanel lossValueDetPan() {
        JPanel coverP = new FramedPanel(new BorderLayout());
        JPanel outerP = new FramedPanel(new GridBagLayout());
        GridBagConstraints outerGbc = new GridBagConstraints();
        outerGbc.gridx = 0;
        outerGbc.gridy = 0;
        FramedPanel outerPRowHead = new FramedPanel(new GridBagLayout());
        GridBagConstraints rowHeadGbc = new GridBagConstraints();
        rowHeadGbc.gridx = 0;
        rowHeadGbc.gridy = 0;
        JPanel tp = new JPanel();
        tp.add(new JLabel("ZONE LOSS DETAILS"));
        outerPRowHead.add(tp, rowHeadGbc);
        rowHeadGbc.gridy++;
        JPanel rowHead = FceSubSection.lossDetailTopRowHead();
        outerPRowHead.add(rowHead, rowHeadGbc);
        outerP.add(outerPRowHead, outerGbc);
        outerGbc.gridx++;
        JPanel topBotP = new JPanel(new GridBagLayout());
        GridBagConstraints topBotGbc = new GridBagConstraints();
        VScrollSync scrollMaster = lossTypeList.getVScrollMaster();
        topBotGbc.gridx = 0;
        topBotGbc.gridy = 0;
        topBotP.add(topBotLossValueP(scrollMaster), topBotGbc);
        topBotGbc.gridx++;
        topBotP.add(topBotLossValueP(scrollMaster, false), topBotGbc);
        if (bTopBot) {
            topBotGbc.gridx++;
            topBotP.add(topBotLossValueP(scrollMaster, true), topBotGbc);
        }
        JScrollPane sp = new JScrollPane();
        sp.setPreferredSize(new Dimension(700, outerPRowHead.getPreferredSize().height + 15));
        sp.getHorizontalScrollBar().setUnitIncrement(150);
        sp.setViewportView(topBotP);
        outerP.add(sp, outerGbc);
        JPanel titleP = new JPanel();
        titleP.add(getTopBotTileP("FURNACE HEAT LOSS DETAILS"));
        coverP.add(titleP, BorderLayout.NORTH);
        coverP.add(outerP, BorderLayout.CENTER);
        return coverP;
    }

    JPanel topBotLossValueP(VScrollSync scrollMaster, boolean bBot) {
        JPanel jp = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        Vector<FceSection> vSec = getVsecs(bBot);
        int nActive = (bBot) ? nBotActiveSecs : nTopActiveSecs;
        gbc.gridx = 0;
        gbc.gridy = 1;

        Vector<XLcellData> lossSummXL = new Vector<XLcellData>();
        LossListWithVal lossVal = (bBot) ? botLossValList : topLossValList;
        jp.add(FceSubSection.lossValueDetPan(lossVal, lossSummXL, scrollMaster), gbc);
        if (bBot)
            lossSummBotXL = lossSummXL;
        else
            lossSummTopXL = lossSummXL;
        gbc.gridx++;
        for (int s = 0; s < nActive; s++) {
            jp.add(vSec.get(s).lossValueDetPan(scrollMaster), gbc);
            gbc.gridx++;
        }
        gbc.gridwidth = gbc.gridx; // as wide as required
        gbc.gridx = 0;
        gbc.gridy = 0;
        JPanel tp = new JPanel();
        String subTitle = topBotName(bBot, true) + "ZONES";
        tp.add(new JLabel(subTitle));
        jp.add(tp, gbc);
        return jp;
    }

    JPanel topBotLossValueP(VScrollSync scrollMaster) {  // for furnace total
        JPanel jp = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;

        lossSummTotXL = new Vector<XLcellData>();
        LossListWithVal lossVal = combiLossValList;
        jp.add(FceSubSection.lossValueDetPan(lossVal, lossSummTotXL, scrollMaster), gbc);
        gbc.gridx = 0;
        gbc.gridy = 0;
        JPanel tp = new JPanel();
        tp.add(new JLabel("TOTAL"));
        jp.add(tp, gbc);
        return jp;
    }

    public int xlLossDetails(Sheet sheet, ExcelStyles styles) {
        Cell cell;
        Row r;
        r = sheet.createRow(0);
        cell = r.createCell(0);
        cell.setCellStyle(styles.csHeader1);
        cell.setCellValue("FURNACE LOSS DETAILS");
//        Row headRow = sheet.createRow(3);
        int topRow = 4;
        int col = 1;
        FceSubSection.xlLossRowHead(sheet, styles, topRow, col);
        col++;

        FceSubSection.xlSecLossDetails(sheet, styles, topRow, col, lossSummTotXL);
        styles.mergeCells(sheet, 3, 3, col, col, styles.csBorderedHeader2, "Total");
        int bottRow = topRow + lossSummTotXL.size() - 1;
        styles.drawBorder(sheet, topRow, bottRow, col, col, styles.borderLine);
        col++;
        sheet.setColumnWidth(col, 500);
        col++;
        col = xlLossDetails(sheet, styles, topRow, col, false);
        sheet.setColumnWidth(col, 500);
        col++;
        if (bTopBot)
            col = xlLossDetails(sheet, styles, topRow, col, true);
        return col;
    }

    public String topBotName(boolean bBot, boolean caps) {
        String name = "";
        if (controller.heatingMode == DFHeating.HeatingMode.TOPBOTSTRIP)
            name = "";
        else
            name = (bBot) ? "Bottom " : "Top ";
        if (caps)
            name = name.toUpperCase();
        return name;
    }

    public String topBotName(boolean bBot) {
        return topBotName(bBot, false);
    }

    // returns next column
    public int xlLossDetails(Sheet sheet, ExcelStyles styles, int topRow, int col, boolean bBot) {
        int headRow = topRow - 1;
        Vector<XLcellData> xlDat = (bBot) ? lossSummBotXL : lossSummTopXL;
        int stCol = col;
        FceSubSection.xlSecLossDetails(sheet, styles, topRow, col, xlDat);
        col++;
        Vector<FceSection> vSec = getVsecs(bBot);
        int nActive = (bBot) ? nBotActiveSecs : nTopActiveSecs;
        for (int s = 0; s < nActive; s++)
            col = vSec.get(s).xlSecLossDetails(sheet, styles, topRow, col);
        String subTitle = topBotName(bBot) + "Zones";
        styles.mergeCells(sheet, headRow, headRow, stCol, (col - 1), styles.csBorderedHeader2,
                subTitle);
        int bottRow = topRow + xlDat.size() - 1;
        styles.drawBorder(sheet, topRow, bottRow, stCol, col - 1, styles.borderLine);
        return col;
    }

    JPanel getComparePanel() {
        JTable table;
        table = report.getJTable();
        JPanel outerP = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        outerP.add(new JLabel("Comparison Table"), gbc);
        gbc.gridy++;
        JScrollPane sP = new JScrollPane(table);
        sP.setBackground(SystemColor.lightGray);
        sP.setPreferredSize(new Dimension(table.getPreferredSize().width, 500));
        JPanel innerP = new JPanel();
        innerP.add(sP);
        outerP.add(innerP, gbc);
        return outerP;
    }

    JPanel getPerfBaseListPanel() {
        if (performBase != null && performBase.isValid())
            return performBase.getListPanel();
        else
            return null;
    }

    JPanel getResultsPanel(boolean bBot) {
        JTable table;
        table = (bBot) ? botTResults.getJTable() : topTResults.getJTable();
        JPanel resultsPan = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        resultsPan.add(getTopBotTileP("TEMPERATURE PROFILE OF ", bBot), gbc);
        gbc.gridy++;
        JScrollPane resultScroll = new JScrollPane(table);
        resultScroll.setBackground(SystemColor.lightGray);
        resultScroll.setPreferredSize(new Dimension(table.getPreferredSize().width, 500));
        JPanel innerP = new JPanel();
        innerP.add(resultScroll);
        resultsPan.add(innerP, gbc);
        return resultsPan;
    }

    JPanel getTopBotTileP(String startStr, boolean bBot) {
        String title = startStr + topBotName(bBot, true) + "SECTIONS";
        return getTopBotTileP(title);
    }

    JPanel getTopBotTileP(String title) {
        JPanel titleP = new JPanel(new GridLayout(1, 1));
        titleP.setPreferredSize(new Dimension(300, 50));
        titleP.add(new JLabel(title));
        return titleP;

    }

    JPanel getSecwisePanel(boolean bBot) {
        Vector<FceSection> vSec = getVsecs(bBot);
        int nActive = (bBot) ? nBotActiveSecs : nTopActiveSecs;
        JPanel outerP = new JPanel();
        FramedPanel innerP = new FramedPanel(new BorderLayout());
        FramedPanel jp = new FramedPanel();
        jp.add(FceSection.getRowHeader(commFuelFiring.fuel.bMixedFuel));
        for (int s = 0; s < nActive; s++) {
            jp.add(vSec.get(s).secResultsPanel(commFuelFiring.fuel.bMixedFuel));
        }
        if (bTopBot && !bBot && bAddTopSoak)
            jp.add(addedTopSoak.secResultsPanel(commFuelFiring.fuel.bMixedFuel));
        JPanel titleP = new JPanel();
        titleP.add(getTopBotTileP("HEAT BALANCE SUMMARY OF ", bBot));
        innerP.add(titleP, BorderLayout.NORTH);
        innerP.add(jp, BorderLayout.CENTER);
        if (bBot)
            botSecwiseInt = innerP;
        else
            topSecwiseInt = innerP;
        outerP.add(innerP);
        return outerP;
    }

// ===Progress Panel below======================================

    JPanel getTrendsPanel(boolean bBot) {
        TrendsPanel gp; //  = setTrendPanel(bBot);
        String title;
        if (bBot) {
            gp = botTrends;
        } else {
            gp = topTrends;
        }
        FramedPanel outerPan = new FramedPanel(new BorderLayout());
        JPanel titleP = new JPanel();
        titleP.add(getTopBotTileP("TEMPERATURE  TRENDS  OF ", bBot));
        outerPan.add(titleP, BorderLayout.NORTH);

        FramedPanel innerP = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        innerP.add(gp, gbc);
        outerPan.add(gp, BorderLayout.CENTER);  // TO BE MODIFIED
        return outerPan;
    }

    FurnaceProfile passLine, profTop, profBot;
    MultiColData topTrendData, botTrendData;
    CombiMultiColData combiTrendData;
    double dischEndExtension = 0.5;

    TrendsPanel getTrendGraphPanel(boolean bBot) {
        dischEndExtension = (controller.furnaceFor == DFHTuningParams.FurnaceFor.STRIP) ? 0 : 0.5;
        MultiColData results;
        results = (bBot) ? getBotTResults() : getTopTResults();
        results.setXLimits(0, (fceLength + ((bTopBot && bAddTopSoak) ? addedTopSoak.sectionLength() : 0)) + dischEndExtension);
        results.setYLimits(0, 1400);
        TrendsPanel proGraph = new TrendsPanel(new Dimension(700, 500), GraphDisplay.CURSVALPANALATBOTTOM);
        passLine = new FurnaceProfile(results);
        FurnaceProfile prof = new FurnaceProfile(bBot, results);
        if (bBot)
            profBot = prof;
        else
            profTop = prof;
        int nextCount = results.addTraces(proGraph, 0);
        proGraph.addTrace(passLine, nextCount, Color.gray);
        proGraph.setbShowVal(nextCount, false);
        nextCount++;
        proGraph.addTrace(prof, nextCount, Color.white);
        if (controller.heatingMode == DFHeating.HeatingMode.TOPBOTSTRIP) {
            nextCount++;
            proGraph.addTrace(new FurnaceProfile(false, results, -1), nextCount, Color.white);
        }
        proGraph.setTraceToShow(-1);   // first trace
        proGraph.prepareDisplay();
        if (bBot)
            botTrendData = results;
        else
            topTrendData = results;
        return proGraph;
    }


    JPanel getCombiTrendsPanel() {
        TrendsPanel gp = combiTrends;
        FramedPanel outerPan = new FramedPanel(new BorderLayout());
        JPanel titleP = new JPanel();
        titleP.add(getTopBotTileP("TEMPERATURE  TRENDS  OF  TOTAL  FURNACE"));
        outerPan.add(titleP, BorderLayout.NORTH);

        FramedPanel innerP = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        innerP.add(gp, gbc);
        outerPan.add(gp, BorderLayout.CENTER);  // TO BE MODIFIED
        return outerPan;
    }

    int tTopGas = 0, tTopFce = 1, tBotGas = 5, tBotFce = 6;  // trace numbers

    TrendsPanel getCombiGraphsPanel() {     // a combined trend
        CombiMultiColData results;
        TrendsPanel proGraph = null;
        results = new CombiMultiColData(topTResults, botTResults);
        if (results != null) {
            proGraph = new TrendsPanel(new Dimension(700, 500), GraphDisplay.CURSVALPANALATBOTTOM);
            int nextCount = results.addTraces(proGraph, 0);
            proGraph.addTrace(profTop, nextCount++, Color.white);
            proGraph.addTrace(passLine, nextCount, Color.gray);
            proGraph.setbShowVal(nextCount, false);
            nextCount++;
            proGraph.addTrace(profBot, nextCount, Color.white);
            proGraph.setTraceToShow(-1);   // first trace
            proGraph.prepareDisplay();
        }
        combiTrendData = results;
        return proGraph;
    }
// ===Progress Panel above======================================


    double flueFlow() {
        double tot = flueFlow(false);
        if (bTopBot)
            tot += flueFlow(true);
        return tot;
    }

    double flueFlow(boolean bBot) {
        FceSection sec;
        sec = (bBot) ? botSections.get(0) : topSections.get(0);
        return sec.flueInSection + sec.fluePassThrough;
    }

    double flueTempOut() {
        double fTt = flueTempOut(false);
        double fT;
        if (bTopBot) {
            double fTb = flueTempOut(true);
            double fFt = flueFlow(false);
            double fFb = flueFlow(true);
            fT = (fTt * fFt + fTb * fFb) / (fFt + fFb);
        } else
            fT = fTt;
        return fT;
    }

    double flueTempOut(boolean bBot) {
        double ft;
        ft = (bBot) ? botSections.get(0).getTempFlueOut() : topSections.get(0).getTempFlueOut();
        return ft;
    }

    public Vector<UnitFurnace> getUnitFce(boolean bBot) {
        return ((bBot) ? vBotUnitFces : vTopUnitFces);
    }

    protected void evalTotTime() {
        holdingWt = getFceLength() / productionData.chPitch * (productionData.charge.unitWt * productionData.nChargeRows);
        totTime = holdingWt / productionData.production;
        speed = getFceLength() / totTime;
        productionData.setSpeed(speed);
        controller.setTimeValues(totTime, totTime * 60 / (productionData.charge.height * 1000), speed / 60);
    }

    // AS - for Added Top Only Soak
    double chUAreaTop, chUAreaBot, chUAreaAS;
    double sideAlphaFactor, endAlphaFactor;
    double sideAlphaFactorAS, endAlphaFactorAS;
    double s152Start, s152StartAS;
    public double effectiveChThick, effectiveChThickAS;
    public double gTop, gBot, gTopAS;

    protected void evalChUnitArea() {
        double gap, effSideH = 0;
        Charge ch = productionData.charge;
        if (ch.type == Charge.ChType.SOLID_CIRCLE)
            evalChUnitAreaForCylinder();
        else {
            gap = productionData.chPitch - ch.width;
            if (controller.heatingMode == DFHeating.HeatingMode.TOPBOTSTRIP) {
                chUAreaTop = (ch.length * productionData.nChargeRows) * ch.width * 2;
                // total surface is taken since the strip is top and bottom heated
//                showMessage("STRIP is Top and Bottom heated ");
            } else {
                if (gap == 0) {
                    chUAreaTop = (ch.length * productionData.nChargeRows) * ch.width;
                    if (bTopBot)
                        chUAreaBot = chUAreaTop * (1 - productionData.bottShadow);
                    sideAlphaFactor = 0;
                } else {
                    double e1, e2, et, gapByH;
                    gapByH = gap / ch.height;
                    e1 = (1 + gapByH - Math.sqrt(1 + Math.pow(gapByH, 2))) / 2;
                    if (bTopBot)
                        et = e1;
                    else {
                        e2 = (1 + gapByH - Math.sqrt(1 + Math.pow(gapByH, 2))) /
                                (2 * (1 + (1 + gapByH - Math.sqrt(1 + Math.pow(gapByH, 2))) /
                                        (Math.sqrt(1 + Math.pow(gapByH, 2)) - 1)));
                        et = e1 + e2;
                    }
                    effSideH = et * ch.height;
                    chUAreaTop = (2 * effSideH + ch.width) * (ch.length * productionData.nChargeRows);
                    if (bTopBot) {
                        chUAreaBot = chUAreaTop * (1 - productionData.bottShadow);
                        sideAlphaFactor = (2 * effSideH) / ch.height;
                    } else
                        sideAlphaFactor = effSideH / ch.height;
                }
            }
            endAlphaFactor = sideAlphaFactor;
            double fact = effSideH / ch.height;
            if (fact > 0.5) // this should not happen
                s152Start = 2;
            else
                s152Start = 1.52 + (2 - 1.52) * fact;

            effectiveChThick = (ch.unitWt * productionData.nChargeRows) / chUAreaTop / ch.chMaterial.density;

            double xMin; // heat penetration depth
            xMin = ch.width / 2;
            if (bTopBot) {
                if (ch.height < ch.width)
                    xMin = ch.height / 2;
                xMin *= 2;   // gRatio of top or bottom section takes care of share(gRatio nominal value is 0.5)
            } else {
                if (ch.height < ch.width / 2)
                    xMin = ch.height;
            }
            if (effectiveChThick < xMin)
                effectiveChThick = xMin;
            gTop = (ch.unitWt * productionData.nChargeRows) / chUAreaTop;
            if (bTopBot)
                gBot = (ch.unitWt * productionData.nChargeRows) / chUAreaBot;
            if (bTopBot && bAddTopSoak)
                forAddedSoak();
        }
    }

    void evalChUnitAreaForCylinder() {
        double gap, effSideH = 0;
        Charge ch = productionData.charge;
        gap = productionData.chPitch - ch.diameter;
        double theta, projectedLen;
        double radius = ch.diameter / 2;
        if (gap == 0) {
            chUAreaTop = (ch.length * productionData.nChargeRows) * ch.diameter;
            if (bTopBot)
                chUAreaBot = chUAreaTop * (1 - productionData.bottShadow);
            sideAlphaFactor = 0;
        } else {
            theta = Math.acos(radius * 2 / productionData.chPitch);
            projectedLen = 2 * radius * (1 + theta);
            chUAreaTop = projectedLen * (ch.length * productionData.nChargeRows);
            if (bTopBot)
                chUAreaBot = chUAreaTop * (1 - productionData.bottShadow);
        }
        s152Start = 2;
        effectiveChThick = (ch.unitWt * productionData.nChargeRows) / chUAreaTop / ch.chMaterial.density;

        double xMin; // heat penetration depth
        xMin = ch.diameter / 2;
        if (effectiveChThick < xMin)
            effectiveChThick = xMin;
        gTop = (ch.unitWt * productionData.nChargeRows) / chUAreaTop;
        if (bTopBot)
            gBot = (ch.unitWt * productionData.nChargeRows) / chUAreaBot;
        if (bTopBot && bAddTopSoak)
            forAddedSoakForCylinder();
    }

    void forAddedSoak() {
        double gap, effSideH = 0;
        Charge ch = productionData.charge;
        gap = productionData.chPitch - ch.width;
        if (gap == 0) {
            chUAreaAS = (ch.length * productionData.nChargeRows) * ch.width;
        } else {
            double e1, e2, et, gapByH;
            gapByH = gap / ch.height;
            e1 = (1 + gapByH - Math.sqrt(1 + Math.pow(gapByH, 2))) / 2;
            e2 = (1 + gapByH - Math.sqrt(1 + Math.pow(gapByH, 2))) /
                    (2 * (1 + (1 + gapByH - Math.sqrt(1 + Math.pow(gapByH, 2))) /
                            (Math.sqrt(1 + Math.pow(gapByH, 2)) - 1)));
            et = e1 + e2;
            effSideH = et * ch.height;
            chUAreaAS = (2 * effSideH + ch.width) * (ch.length * productionData.nChargeRows);
            sideAlphaFactorAS = effSideH / ch.height;
        }
        endAlphaFactorAS = sideAlphaFactorAS;
        double fact = effSideH / ch.height;
        if (fact > 0.5) // this should not happen
            s152StartAS = 2;
        else
            s152StartAS = 1.52 + (2 - 1.52) * fact;

        effectiveChThickAS = (ch.unitWt * productionData.nChargeRows) / chUAreaAS / ch.chMaterial.density;

        double xMin; // heat penetration depth
        xMin = ch.width / 2;
        if (ch.height < ch.width / 2)
            xMin = ch.height;
        if (effectiveChThickAS < xMin)
            effectiveChThickAS = xMin;
        gTopAS = (ch.unitWt * productionData.nChargeRows) / chUAreaAS;
    }

    void forAddedSoakForCylinder() {
        double gap, effSideH = 0;
        Charge ch = productionData.charge;
        gap = productionData.chPitch - ch.diameter;
        double theta, projectedLen;
        double radius = ch.diameter / 2;
        if (gap == 0) {
            chUAreaAS = (ch.length * productionData.nChargeRows) * ch.diameter;
        } else {
            theta = Math.acos(radius / (2 * productionData.chPitch));
            projectedLen = 2 * radius * (1 + theta);
            chUAreaAS = projectedLen * (ch.length * productionData.nChargeRows);
        }
        s152StartAS = 2;
        effectiveChThickAS = (ch.unitWt * productionData.nChargeRows) / chUAreaAS / ch.chMaterial.density;

        double xMin; // heat penetration depth
        xMin = ch.diameter / 2;
        if (effectiveChThickAS < xMin)
            effectiveChThickAS = xMin;
        gTopAS = (ch.unitWt * productionData.nChargeRows) / chUAreaAS;
    }

    Vector<Double> lArray;  // array of subsection end pos combining top and bottom
    Vector<Double> lArrAddSoak;

    protected boolean prepareSlots() {
        boolean bRetVal = true;
        Vector<Double> combLens = fillLarray();
        if (combLens != null) {
            createUnitfces(combLens, false);
            if (bTopBot) {
                createUnitfces(combLens, true);
            }
        } else
            bRetVal = false;
        return bRetVal;
    }

    protected void setProductionBasedSlotParams() {
        double startTime = 0;
        for (FceSection sec: topSections) {
            if (sec.isActive())
                startTime = sec.setProductionBasedParams(startTime);
        }
        if (bTopBot) {
            for (FceSection sec: botSections) {
                if (sec.isActive())
                    startTime = sec.setProductionBasedParams(startTime);
            }
        }
    }

    final static int MAXUFS = 200;

    void createUnitfces(Vector<Double> combLens, boolean bBot) {
        int len = combLens.size();
        if (bBot) {
            vBotUnitFces = new Vector<>();
            botUfsArray = new UnitFceArray(bBot, vBotUnitFces, controller.furnaceFor);
        }
        //        botUnitFces = new UnitFurnace[MAXUFS];
        else {
            vTopUnitFces = new Vector<>();
            topUfsArray = new UnitFceArray(bBot, vTopUnitFces, controller.furnaceFor);
        }
        if (bTopBot && bAddTopSoak) {
            vASUnitFces = new Vector<>();
            ufsArrAS = new UnitFceArray(false, vASUnitFces, controller.furnaceFor);
        }
        // space added for empty slots at section boundaries
        Vector<FceSection> vSec = getVsecs(bBot);
        Vector<UnitFurnace> ufs = getUnitFce(bBot);
        UnitFceArray ufsArr = (bBot) ? botUfsArray : topUfsArray;
        double totTime, totLength, hBig, hEnd, endTime;
        int iLloc;
        double balLcombined;
        Charge ch = productionData.charge;
        UnitFurnace theSlot;
        FceSection sec;
        FceSubSection subSec;
        int iSlot;
        iSlot = 0;
        totLength = 0;
        endTime = 0;
        theSlot = new UnitFurnace(controller.furnaceFor);
        if (bTopBot)
            theSlot.setgRatio(chUAreaTop / (chUAreaTop + chUAreaBot));
        else
            theSlot.setgRatio(1);
        ufs.add(theSlot);
        iLloc = 1;
        balLcombined = lArray.get(iLloc).doubleValue();
        CreateUFceReturn cRetVal = new CreateUFceReturn(iLloc, iSlot, balLcombined, totLength, endTime);
        for (int s = 0; s < vSec.size(); s++) {
            sec = vSec.get(s);
            if (sec.isActive()) {
                sec.setvUnitFurnaces(ufs);
                cRetVal = sec.createUnitfces(combLens, ufs, cRetVal);
            } else
                break;
        }
        double cumLossDischEnd = 0;
        if (!bBot && bTopBot && bAddTopSoak) {
            addedTopSoak.setvUnitFurnaces(ufs);
            addedTopSoak.createUnitfces(lArray, ufs, cRetVal);
        }

        for (int s = vSec.size() - 1; s >= 0; s--) {
            sec = vSec.get(s);
            if (sec.enabled) {
                cumLossDischEnd += sec.getLosses();
                sec.setCumLossDischEnd(cumLossDischEnd);
            } else
                break;
        }
        UnitFurnace uf = ufs.get(0);
        uf.setChargeTemperature(productionData.entryTemp);
        linkSlots(bBot);
        ufsArr.setColData();
        if (bBot)
            botTResults = ufsArr.getMultiColdata();
        else {
            topTResults = ufsArr.getMultiColdata();
        }
    }

    public MultiColData getTopTResults() {
        return topTResults;
    }

    public MultiColData getBotTResults() {
        return botTResults;
    }

    void linkSlots(boolean bBot) {
        Vector<UnitFurnace> vUf;
        UnitFurnace ufNext, uf;
        vUf = getUnitFce(bBot);

        for (int u = 1; u < (vUf.size() - 2); u++) {  // leave the first and the last two
            uf = vUf.get(u);
            ufNext = vUf.get(u + 1);
            if (ufNext.length > 0) //  the this on
                uf.noteExitNei(ufNext);
            else {                // else take the one after the blank one
                uf.noteExitNei(vUf.get(u + 2));
                u++;
            }
        }
        if (tuningParams.bTakeEndWalls) {
            vUf.get(1).noteEndWall(true);
            vUf.get(vUf.size() - 2).noteEndWall(false);
        }
        // set inter slot radiation sources
        for (int u = 1; u < (vUf.size() - 1); u++) {
            uf = vUf.get(u);
            if (uf.length > 0)
                uf.initradNeighbors();
        }
    }

    public int nTopActiveSecs, nBotActiveSecs;

    void evalActiveSecs() {
        evalActiveSecs(false);
        if (bTopBot)
            evalActiveSecs(true);
    }

    public Vector<FceSection> getActiveSections(boolean bBot) {
        Vector<FceSection> active = new Vector<FceSection>();
        Vector<FceSection> vSec = getVsecs(bBot);
        for (FceSection sec : vSec) {
            if (sec.isActive()) {
                active.add(sec);
            } else
                break;
        }
        return active;
    }

    public void evalActiveSecs(boolean bBot) {
        int nActive = 0;
        Vector<FceSection> vSec = getVsecs(bBot);
        for (FceSection sec : vSec) {
            if (sec.isActive()) {
                sec.getNactive();
                nActive++;
            } else
                break;
        }
        if (bBot)
            nBotActiveSecs = nActive;
        else
            nTopActiveSecs = nActive;
    }

    public void evalEndLen() {
        double topEndLen = 0;
        FceSection sec;
        for (int s = 0; s < topSections.size(); s++) {
            sec = topSections.get(s);
            if (sec.enabled) {
                topEndLen = sec.evalEndLen(topEndLen);
            } else
                break;
        }
        double botEndLen = 0;
        if (bTopBot) {
            for (int s = 0; s < botSections.size(); s++) {
                sec = botSections.get(s);
                if (sec.enabled) {
                    botEndLen = sec.evalEndLen(botEndLen);
                } else
                    break;
            }

        }
    }

    Vector<Double> fillLarray() {
        lArray = new Vector<>();
        Vector<Double> topLengths = new Vector<Double>();
        topLengths.add(new Double(0));
        double topEndLen = 0;
        FceSection sec;
        for (int s = 0; s < topSections.size(); s++) {
            sec = topSections.get(s);
            if (sec.enabled) {
                topEndLen = sec.addEndPosList(topLengths, topEndLen);
            } else
                break;
        }
        if (bTopBot && bAddTopSoak)
            addedTopSoak.addEndPosList(topLengths, topEndLen);
        topLengths.add(new Double(0));  // a dummy at the end  CHECK THIS

        Vector<Double> botLengths;
        if (bTopBot) {
            botLengths = new Vector<Double>();
            botLengths.add(new Double(0));
            double botEndLen = 0;
            for (int s = 0; s < botSections.size(); s++) {
                sec = botSections.get(s);
                if (sec.enabled) {
                    botEndLen = sec.addEndPosList(botLengths, botEndLen);
                } else
                    break;
            }
            botLengths.add(new Double(0));  // a dummy at the end
            // merge lengths
            int iT = 0, iB = 0;
            Double lT, lB;
            int comp;
            while ((iT < topLengths.size() && (iB < botLengths.size()))) {
                lT = topLengths.get(iT);
                lB = botLengths.get(iB);
                comp = lT.compareTo(lB);
                if (comp <= 0) {
                    lArray.add(lT);
                    if (comp == 0)
                        iB++;
                    iT++;
                } else {
                    lArray.add(lB);
                    iB++;
                }
            }

            if (topOnlyDEnd) { // top only disrcharge end exists
                // remove the last entry
                lArray.remove(lArray.size() - 1);
                while (iT < topLengths.size()) {
                    lArray.add(topLengths.get(iT));
                    iT++;
                }
            }
            if (bAddTopSoak) { // add the top lengths of top Soak
                // remove the last entry
                lArray.remove(lArray.size() - 1);
                while (iT < topLengths.size()) {
                    lArray.add(topLengths.get(iT));
                    iT++;
                }
            }
//            }
        } else {
            lArray = topLengths;
        }
        return lArray;
    }

 /*
    Vector<Double> fillLarrayAddSoak() {
        lArrAddSoak = new Vector<Double>();
        lArrAddSoak.add(new Double(0));
        double topEndLen = 0;
        addedTopSoak.addEndPosList(lArrAddSoak, topEndLen);
        lArrAddSoak.add(new Double(0)); // dummy at the end
        return lArrAddSoak;
    }
*/

    public void changeFiringMode(boolean bTopBot, boolean bAddTopSoak) {
        this.bTopBot = bTopBot;
        this.bAddTopSoak = (bAddTopSoak && bTopBot);
        updateSectionSummary(false);
        if (!bTopBot)
            disableBottSections();
        if (bTopBot) {
            botSections.get(0).enableSection(true);
        }
        showTopSoak(this.bAddTopSoak);
    }

    void updateSectionSummary(boolean bBot) {
        Vector<FceSection> vSec = getVsecs(bBot);
        for (FceSection sec : vSec)
            sec.setSummaryText();
    }

    void disableBottSections() {
        for (int s = 0; s < botSections.size(); s++)
            botSections.get(s).enableSection(false);
    }

    void initStaticData() {
        FceSection.furnace = this;
        FceSubSection.furnace = this;
    }

    public void addToLossList() {
        FceSubSection.addLossHeader();
        for (int s = 0; s < topSections.size(); s++)
            topSections.get(s).noteLossListChange();
        if (bTopBot) {
            for (int s = 0; s < botSections.size(); s++)
                botSections.get(s).noteLossListChange();
        }
    }

    public void takeLossParams() {
        lossTypeList.takeValuesFromUI();
    }

    void init() {
        lossTypeList = new LossTypeList(controller, this, listener);
        topSections = new Vector<FceSection>();
        FceSection sec;
        for (int s = 0; s < MAXSECTIONS; s++) {
            sec = new FceSection(controller, this, false, (s + 1), true);
            topSections.add(sec);
            if (s == 0)
                sec.enableSection(true);
            else
                sec.enableSection(false);
        }
        botSections = new Vector<FceSection>();
        for (int s = 0; s < MAXSECTIONS; s++) {
            sec = new FceSection(controller, this, true, (s + 1), true);
            botSections.add(sec);
            if (s == 0)
                sec.enableSection(true);
            else
                sec.enableSection(false);
        }
        topDetailsPanel = new FramedPanel(new GridBagLayout());
        setupDetailsPanel(false);
        botDetailsPanel = new FramedPanel(new GridBagLayout());
        setupDetailsPanel(true);
    }

    void takeValuesFromUI() {
        lossTypeList.takeValuesFromUI();
        for (int s = 0; s < topSections.size(); s++)
            topSections.get(s).takeValuesFromUI();
        for (int s = 0; s < botSections.size(); s++)
            botSections.get(s).takeValuesFromUI();
    }

    Component topSoakPan;
    GridBagConstraints gbcTopSoak;

    void setupDetailsPanel(boolean bBot) {
        FramedPanel pan = (bBot) ? botDetailsPanel : topDetailsPanel;
        Vector<FceSection> vSec = getVsecs(bBot);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        for (int s = 0; s < vSec.size(); s++) {
            pan.add(vSec.get(s).sectionsPanel(), gbc);
            gbc.gridx++;
        }
        if (!bBot) {
            addedTopSoak = new FceSection(controller, this, true);
            topSoakPan = addedTopSoak.sectionsPanel();
            addedTopSoak.enableSection(true);
            gbcTopSoak = gbc;
        }
    }

    void showTopSoak(boolean bShow) {
        topDetailsPanel.remove(topSoakPan);
        if (bShow)
            topDetailsPanel.add(topSoakPan, gbcTopSoak);
        topDetailsPanel.validate();
    }

    public void addToLossTable(LossType loss) {
        lossTypeList.add(loss);
    }

    public boolean changeLossItemVal(int lossNum, String lossName, double factor,
                                     LossType.LossBasis basis, LossType.TempAction tempAct) {
        return lossTypeList.changeLossItemVal(lossNum, lossName, factor, basis, tempAct);
    }


    public boolean changeSubSecData(boolean bBot, int secNum, int subNum,
                                    double length, double stHeight, double endHeight, double temperature) {
        boolean retVal = false;
        Vector<FceSection> vSec = getVsecs(bBot);
        if (secNum >= 0 && secNum < vSec.size()) {
            vSec.get(secNum).changeSubSectionData(subNum, length, stHeight, endHeight, temperature);
            enableSectionsIfOK(bBot);
            retVal = true;
        }
        return retVal;
    }

    public void setSectionType(boolean bBot, int secNum, boolean bRecuType) {
        Vector<FceSection> vSec = getVsecs(bBot);
        if (secNum < vSec.size() && secNum >= 0)
            vSec.get(secNum).setSecType(bRecuType);
    }


    public void adjustLengthChange() {
        // enable subsections if ok
        for (int s = 0; s < topSections.size(); s++) {
            topSections.get(s).enableChildrenIfOK();
        }
        enableSectionsIfOK(false);

        if (bTopBot) {
            for (int s = 0; s < botSections.size(); s++) {
                botSections.get(s).enableChildrenIfOK();
            }
            enableSectionsIfOK(true);
        }

        if (bTopBot && bAddTopSoak)
            addedTopSoak.enableChildrenIfOK();
    }

    public double fceLength(boolean bBot) {
        double len = 0;
        Vector<FceSection> vSec;
        if ((bBot && bTopBot) || !bBot) {
            vSec = getVsecs(bBot);
            for (int s = 0; s < vSec.size(); s++)
                len += vSec.get(s).sectionLength();
        }
        return len;
    }

    public double getFceLength() {
        if (Double.isNaN(fceLength))
            checkTopBotLength();
        return fceLength;
    }

    void enableSectionsIfOK(boolean bBot) {
        if ((bBot && bTopBot) || !bBot) {
            Vector<FceSection> vSec = getVsecs(bBot);
            FceSection sec, prevSec;
            prevSec = vSec.get(0);
            prevSec.enableSection(true);
            for (int s = 1; s < vSec.size(); s++) {
                sec = vSec.get(s);
                sec.enableSection(prevSec.isActive());
                prevSec = sec;
            }
        }
        if (bTopBot && bAddTopSoak && !bBot)
            addedTopSoak.enableSection(true);
    }


    public Component getSectionPanelXXX(boolean bBot, int sec) {
        Component retVal = null;
        Vector<FceSection> vSec = getVsecs(bBot);
        if (sec >= 0 && sec < vSec.size())
            retVal = vSec.get(sec).sectionsPanel();
        return retVal;
    }

    public JPanel secDetailsPanel(boolean bBot) {
        return (bBot) ? botDetailsPanel : topDetailsPanel;
    }

    JPanel secTopPanelXXX(boolean bBot, int sec) {
        FceSection section;
        section = (bBot) ? botSections.get(sec) : topSections.get(sec);
        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        jp.add(new JLabel(section.sectionName()), gbc);
        gbc.gridy++;

        return jp;
    }

    int[] topFiredSections, botFiredSections;
    int nTopFired, nBotFired;

    public int firedCount(boolean bBot) {
        int nFired = 0;
        Vector<FceSection> vSec = getVsecs(bBot);
        int[] fired;
        if (bBot) {
            botFiredSections = new int[6];
            fired = botFiredSections;
        } else {
            topFiredSections = new int[6];
            fired = topFiredSections;
        }
        FceSection sec;
        for (int s = 0; s < vSec.size(); s++) {
            sec = vSec.get(s);
            if (sec.isActive() && !sec.bRecuType) {
                fired[nFired] = s;
                nFired++;
            }
        }
        if (bBot)
            nBotFired = nFired;
        else
            nTopFired = nFired;
        return nFired;
    }

    public boolean showZoneDataMsgIfRequired(Component caller) {
        firedCount(false);
        if (bTopBot)
            firedCount(true);
        if (chTempProfAvailable || perfBaseReady || tuningParams.bBaseOnZonalTemperature)
            return true;
        boolean bRetVal = showZoneDataMsgIfRequired(caller, false);
        if (bRetVal && bTopBot)
            bRetVal = showZoneDataMsgIfRequired(caller, true);
        return bRetVal;
    }

    /**
     * It is id required that firedCount(..) has been called before
     * @param caller
     * @param bBot
     * @return
     */

    private boolean showZoneDataMsgIfRequired(Component caller, boolean bBot) {
        boolean retVal = false;
        int fired = (bBot) ? nBotFired : nTopFired; // firedCount(bBot);
        int[] firedSecs = (bBot) ? botFiredSections : topFiredSections;
        String topBotString = topBotName(bBot);
        String tOrP = (topBotString.length() > 3) ? "In " + topBotString + "Sections" : "";
        switch (fired) {
            case 0:
                showError("At least 2 Fired Zones required " + tOrP);
                break;
            case 1:
                showError("Not ready for Single Fired Zone " + tOrP);
                break;
            case 2:
                retVal = true;
                break;
            default:
                retVal = true;
                String manGasTZones = " #" + (firedSecs[1] + 1);
                for (int z = 2; z < (fired - 1); z++) {
                    if (z == (fired - 2))
                        manGasTZones += " and #" + (firedSecs[z] + 1);
                    else
                        manGasTZones += ", #" + (firedSecs[z] + 1);
                }
                if (userActionAllowed())
                    showMessage("There are " + fired + " Burner Zones " + tOrP +
                            "\nYou will be prompted for Gas Temperature" +
                            ((fired > 3) ? "s for Zones " : " for Zone ") + manGasTZones);
                break;
        }
        return retVal;
    }

    Vector<FceSection> getVsecs(boolean bBot) {
        return ((bBot) ? botSections : topSections);
    }

    double minHeight() {
        double minHt = Double.POSITIVE_INFINITY;
        FceSection sec;
        for (int s = 0; s < topSections.size(); s++) {
            sec = topSections.get(s);
            if (sec.isActive())
                minHt = Math.min(minHt, sec.minHeight());
        }
        if (bTopBot && bAddTopSoak)
            minHt = Math.min(minHt, addedTopSoak.minHeight());
        if (minHt == Double.POSITIVE_INFINITY)
            minHt = 0;
        return minHt;
    }

    FlueCompoAndQty flueFromDEnd(boolean bBot, int sNum) {
        Vector<FceSection> vSec = getVsecs(bBot);
        FceSection sec;
        double flTIn = 0;
        FlueCompoAndQty flueCompoAndQty = null;
        if (!bBot && bTopBot && bAddTopSoak)  // get flue qty from addedSoak
            flueCompoAndQty = addedTopSoak.fuelInFsection(flueCompoAndQty);
        for (int s = activeSections(bBot) - 1; s > sNum; s--) {
            sec = vSec.get(s);
            flueCompoAndQty = sec.fuelInFsection(flueCompoAndQty);
        }
        return flueCompoAndQty;
    }


    double totalFlueOLD(double flueExitT, boolean bBot) {
        Vector<FceSection> vSec;
        vSec = getVsecs(bBot);
        double totFlue = 0;
        for (int s = 0; s < activeSections(bBot); s++)
            totFlue += vSec.get(s).flueQtyForExitT(flueExitT);
        return totFlue;
    }

    double totalFlue(double flueExitT, boolean bBot) {
        Vector<FceSection> vSec;
        vSec = getVsecs(bBot);
        double totHeat = 0;
        for (int s = 0; s < activeSections(bBot); s++)
            totHeat += vSec.get(s).totalHeat();
        double realCalVal, airHeat, flueHeat;
        airHeat = commFuelFiring.airHeatPerUfuel;
        flueHeat = commFuelFiring.flueHeatPerUFuel(flueExitT);
        realCalVal = commFuelFiring.fuel.calVal + airHeat - flueHeat;
        double fuelQty = totHeat / realCalVal;
        double flue = fuelQty * commFuelFiring.actFFratio;
        return flue;
    }


    double flueFromDEndApprox(double flueExitT, int prevSec, boolean bBot) {
        Vector<FceSection> vSec;
        vSec = getVsecs(bBot);
        double totFlue = 0;
        for (int s = prevSec; s < activeSections(bBot); s++)
            totFlue += vSec.get(s).flueQtyForExitT(flueExitT);
        return totFlue;
    }


    void setAverageTemperatureRate() {
        double avgRate;
        avgRate = (productionData.exitTemp - productionData.entryTemp) / totTime;
        setAvgRate(false, avgRate);
        if (bTopBot)
            setAvgRate(true, avgRate);
    }

    void setAvgRate(boolean bBot, double avgRate) {
        Vector<FceSection> vSec;
        FceSection sec;
        vSec = getVsecs(bBot);
        for (int s = 0; s < vSec.size(); s++) {
            sec = vSec.get(s);
            if (sec.isActive())
                sec.lastRate = avgRate;
        }
    }

    void setInitialChHeat(int from, int to, double tIn, double tOut, boolean bBot) {
        Vector<FceSection> vSec;
        vSec = getVsecs(bBot);
        double rate;
        FceSection secTo = vSec.get(to);
        FceSection secFrom = vSec.get(from);
        rate = (tOut - tIn) / (secTo.secEndPos - secFrom.secStartPos);
        for (int s = from; s <= to; s++)
            tIn = vSec.get(s).setInitialChHeat(tIn, rate);
    }


    public ErrorStatAndMsg isFurnaceOK() {
        boolean ok = true;
        String msg = "Error in Furnace Top Zones:";
        ErrorStatAndMsg eAndM;
        FceSection sec;
        boolean theLastSection = true;
        for (int s = topSections.size() - 1; s >= 0; s--) {
            sec = topSections.get(s);
            if (sec.isActive()) {
                if (theLastSection && sec.bRecuType) {
                    msg += nlSpace + "Discharge End section Must be with Burners";
                    ok = false;
                    break;
                }
                theLastSection = false;
                eAndM = sec.isSectionOK();
                if (eAndM.inError) {
                    msg += nlSpace + eAndM.msg;
                    ok = false;
                }
            }
        }
        if (bTopBot) {
            theLastSection = true;
            msg += "\nError in Furnace Bottom Zones:";
            for (int s = botSections.size() - 1; s >= 0; s--) {
                sec = botSections.get(s);
                if (sec.isActive()) {
                    if (theLastSection && sec.bRecuType) {
                        msg += nlSpace + "Discharge End section Must be with Burners";
                        ok = false;
                        break;
                    }
                    theLastSection = false;
                    eAndM = sec.isSectionOK();
                    if (eAndM.inError) {
                        msg += nlSpace + eAndM.msg;
                        ok = false;
                    }
                }
            }
            if (bAddTopSoak) {
                sec = addedTopSoak;
                msg += "\nError in Added Top Soak:";
                if (sec.isActive()) {
                    if (sec.bRecuType) {
                        msg += nlSpace + "Added Soak Must be with Burners";
                        ok = false;
                    }
                    eAndM = sec.isSectionOK();
                    if (eAndM.inError) {
                        msg += nlSpace + eAndM.msg;
                        ok = false;
                    }
                } else {
                    msg += nlSpace + "Added Top Zone is NOT defined!";
                    ok = false;
                }
            }
        }
        return new ErrorStatAndMsg(!ok, msg);
    }


    int activeSections(boolean bBot) {
        return (bBot) ? nBotActiveSecs : nTopActiveSecs;
    }

    public String dataInXML(boolean withPerformance) {
        evalActiveSecs();
        String xmlStr = XMLmv.putTag("bTopBot", (bTopBot ? "1" : "0"));
        xmlStr += XMLmv.putTag("lossTypeList", lossTypeList.dataInXML());

        String secStr = XMLmv.putTag("nActiveSec", "" + activeSections(false));
        FceSection sec;
        for (int s = 0; s < topSections.size(); s++) {
            sec = topSections.get(s);
            if (sec.isActive())
                secStr += XMLmv.putTag("s" + ("" + s).trim(), sec.dataInXML());
            else
                break;
        }
        if (bTopBot && bAddTopSoak) {
            secStr += XMLmv.putTag("s AddedSoak", addedTopSoak.dataInXML());
        }
        xmlStr += XMLmv.putTag("topSections", secStr);
        if (bTopBot) {
            secStr = XMLmv.putTag("nActiveSec", "" + activeSections(false));
            for (int s = 0; s < botSections.size(); s++) {
                sec = botSections.get(s);
                if (sec.isActive())
                    secStr += XMLmv.putTag("s" + ("" + s).trim(), sec.dataInXML());
                else break;
            }
            xmlStr += XMLmv.putTag("botSections", secStr);
        }

        if (existingHeatExch != null)
            xmlStr += existingAirRecuInXML();
        if (withPerformance && (perfBaseReady || chTempProfAvailable))
            xmlStr += performanceInXML();
        return xmlStr;
    }

    String existingAirRecuInXML() {
        return XMLmv.putTag("ExistingAirRecu", existingHeatExch.dataInXML());
    }

    public String performanceInXML() {
        String xmlStr = XMLmv.putTag("PerformanceData", performBase.dataInXML().toString());
        return xmlStr;
    }

    FceProfTFM fceProfTFM;

    public XMLgroupStat takeTFMData(String xmlStr) {
        XMLgroupStat grpStat = new XMLgroupStat();
        DoubleWithErrStat dblWithStat;
        ValAndPos vp;
        if (xmlStr.length() > 100) {
            clearSectionData();
            fceProfTFM = new FceProfTFM(controller);
            fceProfTFM.fceProfFromTFM(xmlStr, grpStat, this);
        } else {
            grpStat.addStat(false, "   No data for furnace profile\n");
        }
        return grpStat;
    }

    public boolean takeDataFromXML(String xmlStr) {
        newRecu();
        ValAndPos vp;
        boolean bRetVal = true;
        vp = XMLmv.getTag(xmlStr, "bTopBot", 0);
        bTopBot = (vp.val.equals("1")) ? true : false;
        vp = XMLmv.getTag(xmlStr, "lossTypeList", 0);
        bRetVal &= lossTypeList.takeDataFromXML(vp.val);
        for (int s = 0; s < topSections.size(); s++)
            topSections.get(s).setDefaults();
        vp = XMLmv.getTag(xmlStr, "topSections", vp.endPos);
        bRetVal &= takeSectionsFromXML(vp.val, false);

        for (int s = 0; s < botSections.size(); s++)
            botSections.get(s).setDefaults();
        if (bTopBot) {
            vp = XMLmv.getTag(xmlStr, "botSections", vp.endPos);
            bRetVal &= takeSectionsFromXML(vp.val, true);
        }
        takeAirRecuFromXML(xmlStr);
        evalActiveSecs();
        takePerformanceFromXML(xmlStr);
        return bRetVal;
    }

    public boolean takePerformanceFromXML(String xmlStr) {
        return takePerformanceFromXML(xmlStr, false, false);
    }

    public boolean markPerfTobeSaved(boolean tobeSaved) {
        if (performBase != null) {
            performBase.markToBeSaved(tobeSaved);
            return true;
        }
        else
            return false;
    }

    public boolean takePerformanceFromXML(String xmlStr, boolean readPerfTable, boolean append) {
        boolean retVal = false;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "PerformanceData", 0);
        if (vp.val.length() > 100) { //may be some data
            if ((!append) || performBase == null)
                performBase = new PerformanceGroup(this, tuningParams);
            if (performBase.takeDataFromXML(vp.val, readPerfTable, append)) {
                chTempProfAvailable = performBase.chTempProfAvailable;
                controller.perfBaseAvailable(chTempProfAvailable);
                performBase.markToBeSaved(append);
                retVal = true;
            } else {
                showError("Some problem in reading Performance Data");
//                performBase = null;
            }
//            retVal = linkPerformanceWithProcess();
        }
        return retVal;
    }

    public void enableDeleteInPerformanceData(boolean ena) {
        performBase.enableDeleteAction(ena);
    }

    public void deletePerformance(Performance p) {
        performBase.deletePerformance(p);
    }

    public void takeAirRecuFromXML(String xmlStr) {
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "ExistingAirRecu", 0);
        if (vp.val.length() > 100) { //may be some data
            existingHeatExch = new HeatExchProps();
            if (existingHeatExch.takeDataFromXML(vp.val))
                bRecuFrozen = true;
            else {
                showError("Some problem in Reading Existing Air Recuperator Data!");
                existingHeatExch = null;
                bRecuFrozen = false;
            }
        }
    }

    public boolean getAirRecuPerformance() {
        boolean retVal = false;
        if (existingHeatExch != null && existingHeatExch.canPerform) {
            existingHeatExch.getPerformance(controller, "Evaluate Air Recuperator Performance", "Flue", "Air");
            retVal = true;
        }
        return retVal;
    }

    public boolean defineAirRecuperator() {
        boolean modified = false;
        HeatExchProps newRecu = existingHeatExch;
        if (newRecu == null)
            newRecu = new HeatExchProps();
        EditResponse.Response response = newRecu.defineHeatExchanger(controller, "Define Common Air Recuperator", "Flue", "Air");
        switch(response) {
            case DELETE:
                newRecu();
                break;
            case SAVE:
                existingHeatExch = newRecu;
                freezeExistingHeatExch();
                modified = true;
                break;
        }
        return modified;
    }

    void clearSectionData() {
        for (int s = 0; s < topSections.size(); s++)
            topSections.get(s).setDefaults();
        for (int s = 0; s < botSections.size(); s++)
            botSections.get(s).setDefaults();
    }

    boolean takeSectionsFromXML(String xmlStr, boolean bBot) {
        boolean bRetVal = true;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "nActiveSec", 0);
        int nActive = 0;
        try {
            nActive = Integer.valueOf(vp.val.trim());
        } catch (NumberFormatException e) {
            debug("in takeSectionsFromXML, nActive :" + e);
            nActive = 0;
        }
        vp.endPos = 0;
        Vector<FceSection> vSec = getVsecs(bBot);
        if (nActive > 0) {
            for (int s = 0; s < nActive; s++) {
                vp = XMLmv.getTag(xmlStr, "s" + ("" + s).trim(), vp.endPos);
                vSec.get(s).takeDataFromXML(vp.val);
            }
        }
        if (!bBot) {
            vp = XMLmv.getTag(xmlStr, "s AddedSoak", vp.endPos);
            if (vp.val.length() > 10)
                addedTopSoak.takeDataFromXML(vp.val);

        }
        return bRetVal;
    }

//    public String resultsInCVS() {
//        return "ERROR: NOT READY YET\nNOT READY YET\nNOT READY YET\n";
//    }
//
//    public void getFurnaceData() {
//
//    }
//
//    void setDataDisplay() {
//
//    }

    void debug(String msg) {
        if (!controller.asJNLP)  System.out.println("DFHFurnace: " + msg);
    }

    boolean xlRecuSummary(Sheet sheet, ExcelStyles styles) {
        Cell cell;
        Row r;
        r = sheet.createRow(0);
        cell = r.createCell(0);
        cell.setCellStyle(styles.csHeader1);
        cell.setCellValue("HEAT BALANCE OF RECUPERATOR");
        sheet.setColumnWidth(1, 9000);
        sheet.setColumnWidth(2, 3000);
        sheet.setColumnWidth(3, 500);
        sheet.setColumnWidth(4, 9000);
        sheet.setColumnWidth(5, 3000);
        int topRow = 4, row, rRow;
        int col = 1;
        boolean entryDone = false;
        RecuMultiColPanels lastOne = null;
        if (controller.bAirHeatedByRecu && !controller.bAirAfterFuel) {
            topRow = styles.xlMultiPairColPanel(mpAirRecu.compoBefore, sheet, topRow, col) + 1;
            entryDone = true;
            r = sheet.createRow(topRow);
            (r.createCell(1)).setCellValue("Recuperator for Air");
            topRow++;
            topRow = xlOneRecuSummary(mpAirRecu, styles, sheet, topRow) + 1;
            lastOne = mpAirRecu;
        }
        if (controller.bFuelHeatedByRecu && !commFuelFiring.fuel.isbMixedFuel()) {
            if (!entryDone)
                topRow = styles.xlMultiPairColPanel(mpFuelRecu.compoBefore, sheet, topRow, col) + 1;
            r = sheet.createRow(topRow);
            (r.createCell(1)).setCellValue("Recuperator for Fuel");
            topRow++;
            topRow = xlOneRecuSummary(mpFuelRecu, styles, sheet, topRow) + 1;
            lastOne = mpFuelRecu;
        }
        if (controller.bAirAfterFuel && controller.bAirHeatedByRecu) {
            r = sheet.createRow(topRow);
            (r.createCell(1)).setCellValue("Recuperator for Air");
            topRow++;
            topRow = xlOneRecuSummary(mpAirRecu, styles, sheet, topRow) + 1;
            lastOne = mpAirRecu;
        }
        col = 1;
        if (lastOne != null)
            row = styles.xlMultiPairColPanel(lastOne.compoAfter, sheet, topRow, col);
        return true;
    }

    int xlOneRecuSummary(RecuMultiColPanels mpRecu, ExcelStyles styles, Sheet sheet, int topRow) {
        int row, rRow, col;
        col = 1;
        row = styles.xlMultiPairColPanel(mpRecu.flue, sheet, topRow, col);
        col = 4;
        rRow = styles.xlMultiPairColPanel(mpRecu.fluid, sheet, topRow, col);
        return Math.max(row, rRow);
    }

    boolean xlSecSummary(Sheet sheet, ExcelStyles styles, boolean bBot) {
        Cell cell;
        Row r;
        r = sheet.createRow(0);
        cell = r.createCell(0);
        cell.setCellStyle(styles.csHeader1);
        cell.setCellValue("HEAT BALANCE OF " + ((bBot) ? "BOTTOM" : "TOP") + " SECTIONS");
        cell.setCellValue("HEAT BALANCE OF " +
                ((controller.heatingMode == DFHeating.HeatingMode.TOPBOT) ? ((bBot) ? "BOTTOM" : "TOP") : "") + " SECTIONS");
        int topRow = 4;
        int leftCol = 1;
        FceSection.xlSecSummaryHead(sheet, styles, 4, 1);
        if (bBot) {
            for (int sub = 0; sub < nBotActiveSecs; sub++) {
                leftCol++;
                botSections.get(sub).xlSecResults(sheet, styles, topRow, leftCol);
            }
        } else {
            for (int sub = 0; sub < nTopActiveSecs; sub++) {
                leftCol++;
                topSections.get(sub).xlSecResults(sheet, styles, topRow, leftCol);
            }
            if (bTopBot && bAddTopSoak) {
                leftCol++;
                addedTopSoak.xlSecResults(sheet, styles, topRow, leftCol);
            }
        }
        return true;
    }

    boolean xlHeatSummary(Sheet sheet, ExcelStyles styles) {
        Cell cell;
        Row r;

        r = sheet.createRow(0);
        cell = r.createCell(0);
        cell.setCellStyle(styles.csHeader1);
        cell.setCellValue("TOTAL FURNACE HEAT BALANCE");

        sheet.setColumnWidth(1, 9000);
        sheet.setColumnWidth(2, 3000);
        sheet.setColumnWidth(3, 500);
        sheet.setColumnWidth(4, 9000);
        sheet.setColumnWidth(5, 3000);
        int topRow = 4, row, rRow;
        int col = 1;
        row = styles.xlMultiPairColPanel(mPspheatConPan, sheet, topRow, col);
//        if (commFuelFiring.fuel.bMixedFuel) {
//            rRow = styles.xlMultiPairColPanel(mPfuelMixPan, sheet, topRow, col + 3);
//            row = Math.max(row, rRow);
//        }
        topRow = row + 1;
        row = styles.xlMultiPairColPanel(mPchargeHeatIn, sheet, topRow, col);
        rRow = styles.xlMultiPairColPanel(mPchargeHeatOut, sheet, topRow, col + 3);
        rRow = styles.xlMultiPairColPanel(mPchargeHeatOut, sheet, topRow, col + 3);
        row = Math.max(row, rRow);
        topRow = row + 1;
        row = styles.xlMultiPairColPanel(mPfuelHeatIn, sheet, topRow, col);
        rRow = styles.xlMultiPairColPanel(mPlossesOut, sheet, topRow, col + 3);
        row = Math.max(row, rRow);
        topRow = row + 1;
        row = styles.xlMultiPairColPanel(mPairHeatIn, sheet, topRow, col);
        rRow = styles.xlMultiPairColPanel(mPflueHeatOut, sheet, topRow, col + 3);
        row = Math.max(row, rRow);
        topRow = row + 1;
        row = styles.xlMultiPairColPanel(mPtotHeatIn, sheet, topRow, col);
        rRow = styles.xlMultiPairColPanel(mPtotHeatOut, sheet, topRow, col + 3);
        row = Math.max(row, rRow);

        return true;
    }

    boolean xlTempProfile(Sheet sheet, ExcelStyles styles, boolean bBot) {
        Cell cell;
        Row r;
        r = sheet.createRow(0);
        cell = r.createCell(0);
        cell.setCellStyle(styles.csHeader1);
        cell.setCellValue("TEMPERATURE PROFILE OF " + topBotName(bBot, true) + "SECTIONS");
        int topRow = 4;
        int leftCol = 1;
        JTable table = (bBot) ? botTResults.getResultTable() : topTResults.getResultTable();
        int row = styles.xlAddXLCellData(sheet, topRow, leftCol, table);
        return true;
    }

    boolean xlFuelSummary(Sheet sheet, ExcelStyles styles) {
        sheet.setColumnWidth(1, 9000);
        sheet.setColumnWidth(2, 5000);
        Cell cell;
        Row r;
        r = sheet.createRow(0);
        cell = r.createCell(0);
        cell.setCellStyle(styles.csHeader1);
        cell.setCellValue("FUEL SUMMARY OF FURNACE");
        int topRow = 4;
        int leftCol = 1;
        topRow = styles.xlAddXLCellData(sheet, topRow, leftCol, fuelSummTopTexts);
        topRow++;
        topRow = styles.xlMultiPairColPanel(fuelUsage.mpFuelSummary, sheet, topRow, leftCol);
        return true;
    }

    boolean xlUsedFuels(Sheet sheet, ExcelStyles styles) {
        Cell cell;
        Row r;
        r = sheet.createRow(0);
        cell = r.createCell(0);
        cell.setCellStyle(styles.csHeader1);
        cell.setCellValue("FUELS USED");
        int topRow = 4;
        int leftCol = 1;
        sheet.setColumnWidth(leftCol, 9000);
        sheet.setColumnWidth(leftCol + 1, 4000);
        sheet.setColumnWidth(leftCol + 2, 500);
        sheet.setColumnWidth(leftCol + 3, 9000);
        sheet.setColumnWidth(leftCol + 4, 4000);
        int row = topRow;
        for (Fuel fuel : uniqueFuels) {
            if (fuel.bMixedFuel) {
                row = styles.xlMultiPairColPanel(fuel.fuelPanel(controller), sheet, topRow, leftCol) + 1;
                row = Math.max(row, styles.xlMultiPairColPanel(fuel.fuelMixDetails(), sheet, topRow, leftCol + 3) + 1);
                topRow = row;
            }
        }

        for (Fuel fuel : uniqueFuels) {
            if (!fuel.bMixedFuel) {
                row = styles.xlMultiPairColPanel(fuel.fuelPanel(controller), sheet, topRow, leftCol) + 1;
                topRow = row;
            }
        }

        return true;
    }

    boolean xlSecFuelSummary(Sheet sheet, ExcelStyles styles, boolean bBot) {
        Cell cell;
        Row r;
        r = sheet.createRow(0);
        cell = r.createCell(0);
        cell.setCellStyle(styles.csHeader1);
        cell.setCellValue("FUEL SUMMARY OF " + topBotName(bBot, true) + " SECTIONS");
        int topRow = 4, row;
        int leftCol = 1;
        Vector<FceSection> vSec = getVsecs(bBot);
        FceSection sec;
        int nValid = (bBot) ? nBotActiveSecs : nTopActiveSecs;
        for (int s = 0; s < nValid; s++) {
            sec = vSec.get(s);
            if (!sec.bRecuType) {
                sheet.setColumnWidth(leftCol, 8000);
                sheet.setColumnWidth(leftCol + 1, 4000);
                sheet.setColumnWidth(leftCol + 2, 500);
                row = sec.xlSecFuel(sheet, styles, topRow, leftCol);
                leftCol += 3;
            }
        }
        if (bTopBot && !bBot && bAddTopSoak) {
            sheet.setColumnWidth(leftCol, 8000);
            sheet.setColumnWidth(leftCol + 1, 4000);
            sheet.setColumnWidth(leftCol + 2, 500);
            row = addedTopSoak.xlSecFuel(sheet, styles, topRow, leftCol);
            leftCol += 3;
        }
        return true;
    }

    boolean xlComparisonReport(Sheet sheet, ExcelStyles styles) {
        Cell c, rowNumCell;
        Row r;
        int slNo;
        // row 0 is for ID and used row slNo.
        r = sheet.getRow(1);
        rowNumCell = r.getCell(2);
        int topRow = (new Double(rowNumCell.getNumericCellValue())).intValue();
        int leftCol;
        if (topRow <= 0) {
            leftCol = 1;
            topRow = 4;
            slNo = 1;
            topRow = report.xlReportColHead(sheet, styles, topRow, leftCol);
        } else
            slNo = topRow - 4;
        leftCol = 0;
        topRow = report.xlReportLines(sheet, styles, topRow, leftCol, slNo);
        rowNumCell.setCellValue(topRow);
        return true;
    }

    String tProfileForTFMREMOVE(boolean bBot) {
        String dataStr = "";
        UnitFceArray ufsA = (bBot) ? botUfsArray : topUfsArray;
        double[] data = ufsA.tProfileForTFM(tuningParams.tfmBasis, (int) (getFceLength() / tuningParams.getTFMStep()));
        int len = data.length;
        if (len > 1 && !Double.isNaN(data[0])) {
            for (int i = 0; i < (len - 1); i++)
                dataStr += "" + data[i] + "\n";
            dataStr += "" + data[len - 1];
        }
        return dataStr;
    }

/*
    String tProfileForTFMREMPVE() {
        String dataStr = "";
        double[] dataTop = topUfsArray.tProfileForTFM(tuningParams.tfmBasis, (int)(fceLength() / tuningParams.getTFMStep()));
        int len = dataTop.length;
        double[] dataBot = null;
        if (bTopBot) {
            dataBot = botUfsArray.tProfileForTFM(tuningParams.tfmBasis, (int)(fceLength() / tuningParams.getTFMStep()));
            len = Math.min(len, dataBot.length);
        }
        if (len > 1 && !Double.isNaN(dataTop[0])) {
            for (int i = 0; i < len; i++) {
                dataStr += "" + dataTop[i];
                if (bTopBot)
                    dataStr += ";" + dataBot[i];
                if (i < (len - 1))
                    dataStr += "\n";
            }
        }
        return dataStr;
    }
*/

    String tProfileForTFMWithLen() {
        String dataStr = "";
        if (bAddTopSoak) {
            showMessage("TFM file cannot ber created for Furnace with Top-Only Soak section!");
            return dataStr;
        }
        double[][] dataArr;
        int[] colArray;
        int nRows;
        double totLen = SPECIAL.roundToNDecimals(fceLength, 4);
        DecimalFormat fmtL = new DecimalFormat("0.0000");
        DecimalFormat fmtT = new DecimalFormat("0.0");
        if (bTopBot) {
            colArray = new int[]{tTopGas, tTopFce, tBotGas, tBotFce};
            dataArr = combiTrendData.getDataArray(colArray);
        } else {
            colArray = new int[]{tTopGas, tTopFce};
            dataArr = topTrendData.getDataArray(colArray);

        }
        nRows = dataArr.length;
        double tVal1 = 0, tVal2 = 0, bVal1 = 0, bVal2 = 0;
        double delLen, addedLen = 0, lastLen = -1, nowLen;
        double delLenFract, totalDelLenFract = 0;
        for (int r = 0; r < (nRows - 1); r++) {
            nowLen = dataArr[r][0];
            if (r == (nRows - 2))
                delLen = SPECIAL.roundToNDecimals(totLen - addedLen, 4);
            else
                delLen = dataArr[r + 1][0] - nowLen;
            if (delLen <= 0)
                continue;
            if (r > 0)
                dataStr += "\n";
            delLen = SPECIAL.roundToNDecimals(delLen, 4);
            delLenFract = SPECIAL.roundToNDecimals(delLen / totLen, 4);
            addedLen = SPECIAL.roundToNDecimals(addedLen + delLen, 4);
            if (r == (nRows - 1))
                delLenFract = 1 - totalDelLenFract;
            dataStr += fmtL.format(delLenFract);
            totalDelLenFract = SPECIAL.roundToNDecimals(totalDelLenFract + delLenFract, 4);
            switch (tuningParams.tfmBasis) {
                case GASTEMP:
                    tVal1 = dataArr[r][1];
                    tVal2 = dataArr[r + 1][1];
                    if (bTopBot) {
                        bVal1 = dataArr[r][3];
                        bVal2 = dataArr[r + 1][3];
                    }
                    break;
                case FCETEMP:
                    tVal1 = dataArr[r][2];
                    tVal2 = dataArr[r + 1][2];
                    if (bTopBot) {
                        bVal1 = dataArr[r][4];
                        bVal2 = dataArr[r + 1][4];
                    }
                    break;
                default:
                    tVal1 = (dataArr[r][1] + dataArr[r][2]) / 2;
                    tVal2 = (dataArr[r + 1][1] + dataArr[r + 1][2]) / 2;
                    if (bTopBot) {
                        bVal1 = (dataArr[r][3] + dataArr[r][4]) / 2;
                        bVal2 = (dataArr[r + 1][3] + dataArr[r + 1][4]) / 2;
                    }
                    break;
            }
            dataStr += ";" + fmtT.format(tVal1) + ";" + fmtT.format(tVal2);
            if (bTopBot)
                dataStr += ";" + fmtT.format(bVal1) + ";" + fmtT.format(bVal2);
            else
                dataStr += ";" + fmtT.format(0) + ";" + fmtT.format(0);   // just 0.0; 0.0 as data for botttom as required by TFM
            lastLen = nowLen;
        }
        return dataStr;
    }

    String dataForFE() {
        Vector<FceAmbient> fceTopAmbs = new Vector<FceAmbient>();
        Vector<FceAmbient> fceBotAmbs = null;
        String head = "# fromTime, Temperature, Alpha\n";
        for (FceSection sec : topSections)
            sec.addAmbientData(fceTopAmbs);
        if (bTopBot) {
            fceBotAmbs = new Vector<FceAmbient>();
            for (FceSection sec : botSections)
                sec.addAmbientData(fceBotAmbs);
            if (bAddTopSoak) {
                addedTopSoak.addAmbientData(fceTopAmbs);
                fceBotAmbs.add(new FceAmbient(addedTopSoak.getStartTime(), 1000, 0));   // insulated
            }
        }
        String ambDataStr = "Ambient Data File\n" +
                "Version = 1\n" +
                "# Created by DFHFurnace on " + (new Date()) + ".\n" +
                "#\n";
        int nAmbs = (bTopBot) ? 5 : 4;
        int a = 1;
        ambDataStr += "Number of Ambients = " + nAmbs + "\n";
        ambDataStr += "# Ambient" + a + "\nName = " + "%TOP\n";
        ambDataStr += "Steps =" + fceTopAmbs.size() + "\n";
        ambDataStr += head;
        for (FceAmbient amb : fceTopAmbs)
            ambDataStr += "" + amb.ambString() + "\n";
        ambDataStr += "#...\n";
        a++;
        ambDataStr += "# Ambient" + a + "\nName = " + "%BOTTOM\n";
        if (bTopBot) {
            ambDataStr += "Steps =" + fceBotAmbs.size() + "\n";
            ambDataStr += head;
            for (FceAmbient amb : fceBotAmbs)
                ambDataStr += "" + amb.ambString() + "\n";
        } else {
            ambDataStr += "Steps =" + 1 + "\n";
            ambDataStr += head;
            ambDataStr += "0, 1000, 0\n"; // insulated since no bottom heating
        }
        ambDataStr += "#...\n";
        a++;
        ambDataStr += "# Ambient" + a + "\nName = " + "%SIDES\n";
        ambDataStr += "Steps =" + fceTopAmbs.size() + "\n";
        ambDataStr += head;
        FceAmbient ambT, ambB;
        int slot;
        if (bTopBot) {
            ambDataStr += "# Side Alpha as " + sideAlphaFactor + " of Average Alpha\n";
            for (slot = 0; slot < fceBotAmbs.size(); slot++) {
                ambB = fceBotAmbs.get(slot);
                ambT = fceTopAmbs.get(slot);
                ambDataStr += ambB.avgAmbString(ambT, sideAlphaFactor) + "\n";
            }
            for (; slot < fceTopAmbs.size(); slot++) {   // this will happen if there is added top Soak
                ambT = fceTopAmbs.get(slot);
                ambDataStr += ambT.ambString(sideAlphaFactorAS) + "\n";
            }
        } else {
            ambDataStr += "# Side Alpha as " + sideAlphaFactor + " of Top Alpha\n";
            for (FceAmbient amb : fceTopAmbs)
                ambDataStr += "" + amb.ambString(sideAlphaFactor) + "\n";
        }
        ambDataStr += "#...\n";
        a++;
        ambDataStr += "# Ambient" + a + "\nName = " + "%ENDS\n";
        ambDataStr += "Steps =" + fceTopAmbs.size() + "\n";
        ambDataStr += head;
        if (bTopBot) {
            ambDataStr += "# End Alpha as " + endAlphaFactor + " of Average Alpha\n";
            for (slot = 0; slot < fceBotAmbs.size(); slot++) {
                ambB = fceBotAmbs.get(slot);
                ambT = fceTopAmbs.get(slot);
                ambDataStr += ambB.avgAmbString(ambT, endAlphaFactor) + "\n";
            }
            for (; slot < fceTopAmbs.size(); slot++) {   // this will happen if there is added top Soak
                ambT = fceTopAmbs.get(slot);
                ambDataStr += ambT.ambString(endAlphaFactorAS) + "\n";
            }
            ambDataStr += "#...\n";
            a++;
            // add Skids
            ambDataStr +=
                    "# Ambient" + a + "\n" +
                            "Name = %SKID" + "\n" +
                            "#" + "\n" +
                            "Steps =  2" + "\n" +
                            head +
                            "0.0000,  50,   60" + "\n" +
                            "10, 50,   60" + "\n";
        } else {
            ambDataStr += "# End Alpha as " + endAlphaFactor + " of Top Alpha\n";
            for (FceAmbient amb : fceTopAmbs)
                ambDataStr += "" + amb.ambString(endAlphaFactor) + "\n";
        }
        ambDataStr += "#...\n";

        return ambDataStr;
    }

    boolean decide(String title, String msg) {
        return controller.decide(title, msg);
    }

    boolean decide(String title, String msg, int forTime) {
        return controller.decide(title, msg, forTime);
    }

    protected void showError(String msg) {
        (new TimedMessage("In DFHFurnace", msg, TimedMessage.ERROR, controller.parent())).show();
    }

    protected void showError(String msg, int forTime) {
        (new TimedMessage("In DFHFurnace", msg, TimedMessage.ERROR, controller.parent(), forTime)).show();
    }

    public void showMessage(String msg) {
        controller.logInfo("DFHFurnace.4997: " + msg);
        (new TimedMessage("In DFHFurnace", msg, TimedMessage.INFO, controller.parent(), 3000)).show();
    }

    double gasTempZ2Top, gasTempZ2Bot;

    double flueTempTop, flueTempBot;

    class FlueFlowTempHeat {
        double flow;
        double temp;
        double heat;
        FlueComposition compo = null;

        FlueFlowTempHeat() {

        }

        void addFlue(double addFlue, double addHeat) {
            flow += addFlue;
            heat += addHeat;
        }

        /*
       WARNING: This does not evaluate proper combine flues temperature
        */
        void addFlue(FlueFlowTempHeat addFlue) {
            addFlue(addFlue.flow, addFlue.heat);
        }

        double effectiveTemp() {
            return commFuelFiring.flue.tempFromSensHeat(heat / flow);
        }
    }

    class FurnaceProfile extends GraphInfoAdapter {
        boolean bBot;
        boolean bPassLine = false;
        VariableDataTrace traceProf;
        Vector<UnitFurnace> vUfs;
        Vector<DoublePoint> vProf;
        double baseY = 350;
        double scale = 0;
        MultiColData ref; // for scale etc.
        TraceHeader th;
        DoublePoint[] totProf;
        double htFactor = 1;
        Color color;

        FurnaceProfile(boolean bBot, MultiColData ref) {
            this(bBot, ref, 1);
        }

        FurnaceProfile(boolean bBot, MultiColData ref, double htFactor) {
            this.bBot = bBot;
            this.ref = ref;
            this.htFactor = htFactor;
            vUfs = getUnitFce(bBot);
            scale = (bBot) ? -100 : 100;
            scale *= htFactor;
            createTrace(color);
        }

        FurnaceProfile(MultiColData ref) {
            this.ref = ref;
            bPassLine = true;
            totProf = new DoublePoint[2];
            totProf[0] = new DoublePoint(0, baseY);
            if (bTopBot && bAddTopSoak)
                totProf[1] = new DoublePoint(fceLength + addedTopSoak.sectionLength() + dischEndExtension, baseY);
            else
                totProf[1] = new DoublePoint(fceLength + dischEndExtension, baseY);
        }

        void createTrace(Color color) {
            vProf = new Vector<DoublePoint>();
            Vector<FceSection> vSec = getVsecs(bBot);
            int nActive = (bBot) ? nBotActiveSecs : nTopActiveSecs;
            for (int s = 0; s < nActive; s++)
                vSec.get(s).addToProfileTrace(vProf, scale, baseY);
            if (!bBot && bAddTopSoak)
                addedTopSoak.addToProfileTrace(vProf, scale, baseY);
            th = new TraceHeader("From Ch End", "m", ((bBot) ? "Bottom " : "Top ") + "Height", "mm");
            DoublePoint[] dp = new DoublePoint[1];
            traceProf = new VariableDataTrace(th, vProf.toArray(dp), color);
            if (bBot) {
                double addLen = (bAddTopSoak) ? 0 : dischEndExtension;
                if (topOnlyDEnd) {
                    vProf.add(new DoublePoint(bottLength, baseY));
                    vProf.add(new DoublePoint(fceLength + addLen, baseY));
                } else
                    vProf.add(new DoublePoint(fceLength + addLen, vSec.get(nActive - 1).getEndHeight() * scale + baseY));
                vProf.add(new DoublePoint(fceLength + addLen, baseY));
            } else {
                if (bAddTopSoak) {
                    double totLen = fceLength + addedTopSoak.sectionLength();
                    vProf.add(new DoublePoint(totLen + dischEndExtension, addedTopSoak.getEndHeight() * scale + baseY));
                    vProf.add(new DoublePoint(totLen + dischEndExtension, baseY));
                } else {
                    vProf.add(new DoublePoint(fceLength + dischEndExtension, vSec.get(nActive - 1).getEndHeight() * scale + baseY));
                    vProf.add(new DoublePoint(fceLength + dischEndExtension, baseY));
                }
            }
            totProf = vProf.toArray(dp);
        }

        @Override
        public DoublePoint[] getGraph(int trace) {
            return totProf;
//            return traceProf.getGraph();
        }

        @Override
        public DoubleRange getXrange(int trace) {
            return ref.getCommonXrange();
        }

        @Override
        public DoubleRange getYrange(int trace) {
            return ref.getCommonXrange();
        }

        @Override
        public DoubleRange getCommonXrange() {
            return ref.getCommonXrange();
        }

        @Override
        public DoubleRange getCommonYrange() {
            return ref.getCommonYrange();
        }

        @Override
        public TraceHeader getTraceHeader(int trace) {
            return th;
        }

        @Override
        public double getYat(int trace, double x) {
            return (traceProf.getYat(x) - baseY) / scale * 1000;
        }
    }

    class ZoneTemperatureDlg extends JDialog {
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ActionListener li;
        Frame parent;
        boolean forBottomSections = false;
        Vector<FceSection> vSec;
        int nActive;
        boolean allOK = false;

        ZoneTemperatureDlg(Frame parent, boolean forBottomSections) {
            super(parent, "Set Zonal Temperatures", Dialog.ModalityType.DOCUMENT_MODAL);
            this.parent = parent;
            this.forBottomSections = forBottomSections;
            vSec = (forBottomSections) ? botSections : topSections;
            nActive = (forBottomSections) ? nBotActiveSecs : nTopActiveSecs;
            jbInit();
            pack();
        }

        void jbInit() {
            Dimension d = new Dimension(100, 25);
            ok.setPreferredSize(d);
            cancel.setPreferredSize(d);
            li = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Object src = e.getSource();
                    if (src == ok) {
                        if (noteZonalTemperatures()) {
                            closeThisWindow();
                            allOK = true;
                        } else
                            showError("Some Error in Zonal Temperature Entries");
                    } else if (src == cancel)
                        closeThisWindow();
                }
            };
            ok.addActionListener(li);
            cancel.addActionListener(li);
            Container dlgP = getContentPane();
            MultiPairColPanel jp = new MultiPairColPanel("Set Zonal Temperatures");
            for (int s = 0; s < nActive; s++) {
                jp.addItem(vSec.get(s).jpTCLocationAndTemperature());
            }
            jp.addItemPair(cancel, ok);
            dlgP.add(jp);
        }

        boolean noteZonalTemperatures() {
            boolean retVal = true;
            for (int s = 0; s < nActive; s++)
                retVal &= vSec.get(s).noteZonalTemperature();
            return retVal;
        }

        void closeThisWindow() {
            setVisible(false);
            dispose();
        }
    }


}
