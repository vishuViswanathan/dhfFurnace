package directFiredHeating;

import mvUtils.display.*;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 7/25/12
 * Time: 11:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class DFHTuningParams {
    public enum FurnaceFor {
        BILLETS("Billet/Slab Heating"),
        STRIP("Strip Heating"),
        MANUAL("Manually set");
        private final String proName;

        FurnaceFor(String proName) {
            this.proName = proName;
        }

        public String getValue() {
            return name();
        }

        @Override
        public String toString() {
            return proName;
        }

        public static FurnaceFor getEnum(String text) {
            FurnaceFor retVal = null;
            if (text != null) {
                for (FurnaceFor b : FurnaceFor.values()) {
                    if (text.equalsIgnoreCase(b.proName)) {
                        retVal = b;
                        break;
                    }
                }
            }
            return retVal;
        }
    }

    public enum ChEmmFactorForTFMmatch {
        REFLECTIVITY("Reflectivity"),
        EMISSIVITY("Emissivity");

        private final String actionType;

        ChEmmFactorForTFMmatch(String actionType) {
            this.actionType = actionType;
        }

        public String getValue() {
            return name();
        }

        @Override
        public String toString() {
            return actionType;
        }

        public static ChEmmFactorForTFMmatch getEnum(String text) {
            ChEmmFactorForTFMmatch retVal = null;
            if (text != null) {
                for (ChEmmFactorForTFMmatch b : ChEmmFactorForTFMmatch.values()) {
                    if (text.equalsIgnoreCase(b.actionType)) {
                        retVal = b;
                        break;
                    }
                }
            }
            return retVal;
        }
    }
    JButton jbRestParams;
    public boolean bOnTest = false;
    JCheckBox cBOnTest;
    public double epsilonO = 0.8;
    public double gasWallHTMultipler = 5;
    public double alphaConvFired = 20;
    public double alphaConvRecu = 20;
    public double emmFactor = 1;
    boolean bNoEmissivityCorrFactor = false;
    JCheckBox cBNoEmissivityCorrFactor;
    public double chEmmissCorrectionFactor = 1.0;  // this is dynamically set before furnace calculation
    // no user entry
    boolean noGasRadiationToCharge = false;
    JCheckBox cBNoGasRadiationToCharge;
    JButton jBresetTFMmatch;
    boolean bDoTFMmatch = false;
    JCheckBox cBdoTFMmatch;
    public boolean bFormulaForTau = false;
    JCheckBox cBFormulaForTau;
    public boolean bApplyChEmissMultiplier = false;
    public double tfmChEmissMultiplier = 1.0;
    NumberTextField ntTfmChEmissMultiplier;
    public boolean bApplyChReflectivityMultiplier = false;
    ChEmmFactorForTFMmatch appTFMchEmissCorrectionType = ChEmmFactorForTFMmatch.REFLECTIVITY;

    XLComboBox cbTFMemissAction;

    double s152LimitMin = 1.5;
//    NumberTextField ntS152LimitMin;
    double s152LimitMax = 2.5;
//    NumberTextField ntS152LimitMax;
    double defaultTauLimitMin = 0.2;
    double tauLimitMin = defaultTauLimitMin;
    NumberTextField ntTauLimitMin;
    double defaultTauLimitMax = 0.99;
    double tauLimitMax = defaultTauLimitMax;
    NumberTextField ntTauLimitMax;
    int defaultN2dCheck = 1;
    public int n2dCheck = defaultN2dCheck;

    NumberTextField ntN2dCheck;
    public boolean bDo2Dcheck = false;
    JCheckBox cBdo2DCheck;
    public boolean bDo2dCalculation = false;
    JCheckBox cBdo2DCalculation;
    boolean bGasAbsorptionHeilingen = false;
    JCheckBox cBgasAbsorptionHeilingen;
    double defaultErrorAllowed = 1.0;
    public double errorAllowed = defaultErrorAllowed;
    public boolean bTakeEndWalls = false;
    public boolean bTakeGasAbsorptionForInterRad = false;
    public double wallLoss = 0;
    public boolean bHotCharge = false;
    double defaultStartFlueTempHead = 100;
    public double startFlueTempHead = defaultStartFlueTempHead;  // for hot charge
    public boolean bEvalBotFirst = false;
    public boolean bSectionalFlueExh = false;    // not used yet
    public boolean bSlotRadInCalcul = false;
    public boolean bSectionProgress = true;
    public boolean bSlotProgress = true;
    double defaultSuggested1stCorrection = 10;
    public double suggested1stCorrection = defaultSuggested1stCorrection;
    double defaultCorrectionForTooLowGas =10;
    public double correctionForTooLowGas = defaultCorrectionForTooLowGas;
    public boolean bDynamicGasTempCorrection = false;
    double defaultMinGasTempCorrection = 1;
    public double minGasTempCorrection = defaultMinGasTempCorrection;
    double defaultMaxGasTempCorrection = 10;
    public double maxGasTempCorrection = defaultMaxGasTempCorrection;
    public double dynamicGasTempCorrectionRange = 0.5; //  position as fraction of length upto end of 1st fired section
    public boolean bMindChHeight = true;
    public boolean bAllowSecFuel = false;
    public boolean bAllowRegenAirTemp = true;
    public boolean bShowFlueCompo = true;
    public boolean bAutoTempForLosses = true;
    public boolean bSmoothenCurve = true;
    public boolean bNoGasAbsorptionInWallBalance = false;
    public boolean bBaseOnZonalTemperature = false;
    public double radiationMultiplier = 1.0;  // used for calculation only with wall radiation for fuel furnace
    LinkedHashMap<FurnaceFor, PreSetTunes> preSets;
    final DFHeating controller;
    FurnaceFor selectedProc;
    PreSetTunes selectedPreset;
    UnitFceArray.ProfileBasis tfmBasis = UnitFceArray.ProfileBasis.FCETEMP;
//    private double tfmStep = 1.0;

    public DFHTuningParams(DFHeating controller, boolean onProductionLine, double epsilonO, double gasWallHTMultipler,
                           double alphaConvFired, double emmFactor, double errorAllowed, boolean bTakeEndWalls,
                           boolean bTakeGasAbsorptionForInterRad) {
        this(controller);
        this.epsilonO = epsilonO;
        this.gasWallHTMultipler = gasWallHTMultipler;
        this.alphaConvFired = alphaConvFired;
        this.alphaConvRecu = this.alphaConvFired;
        this.emmFactor = emmFactor;
        this.errorAllowed = errorAllowed;
        this.bTakeEndWalls = bTakeEndWalls;
        this.bTakeGasAbsorptionForInterRad = bTakeGasAbsorptionForInterRad;
        this.bOnProductionLine = onProductionLine;
        updateUI();
    }

    public DFHTuningParams(DFHeating controller) {
        this.controller = controller;
        jbRestParams = new JButton("Reset All Params");
        jbRestParams.addActionListener(e-> {
            setParamsToDefaultValues(false); // except the TFMmatch Params
        });
        preSets = new LinkedHashMap<FurnaceFor, PreSetTunes>();
        preSets.put(FurnaceFor.BILLETS, new PreSetTunes(1, 5, 30, 30, 1.12, 1.0, false));
        preSets.put(FurnaceFor.STRIP, new PreSetTunes(0.8, 1, 15, 15, 1, 1.0, false));
        preSets.put(FurnaceFor.MANUAL, new PreSetTunes(1, 5, 30, 30, 1, 1.0, true));
        tferrorAllowed = new NumberTextField(controller, errorAllowed, 6, false, 0.001, 5, "#,###.00", "", true);
        tfwallLoss = new NumberTextField(controller, wallLoss, 6, false, 0, 1000, "#,###.00", "", true);
        tfsuggested1stCorrection = new NumberTextField(controller, suggested1stCorrection, 6, false, 0, 20, "#,###.00", "", true);
        tfCorrectionforTooLowGas = new NumberTextField(controller, correctionForTooLowGas, 6, false, 0, 20, "#,###.00", "", true);
        chbDynamicGasTempCorrection = new JCheckBox();
        tfMaxGasTempCorrection = new NumberTextField(controller, maxGasTempCorrection, 6, false, 1, 100, "#,###.00", "Max Gas Temp Correction (C)", true);
        tfMinGasTempCorrection = new NumberTextField(controller, minGasTempCorrection, 6, false, 1, 100, "#,###.00", "Min Gas Temp Correction (C)", true);
//        chbDynamicGasTempCorrection.addActionListener(e-> {
//            tfMinGasTempCorrection.setEnabled(chbDynamicGasTempCorrection.isSelected());
//            tfMaxGasTempCorrection.setEnabled(chbDynamicGasTempCorrection.isSelected());
//        });
        cBSlotRadInCalcul = new JCheckBox();
        cBTakeEndWalls = new JCheckBox();
        cBbaseOnZonalTemperature = new JCheckBox();
        cBbaseOnZonalTemperature.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bBaseOnZonalTemperature = cBbaseOnZonalTemperature.isSelected();
                if (bBaseOnZonalTemperature) {
                    cBSlotRadInCalcul.setEnabled(false);
                    bSlotRadInCalcul = false;
                    cBTakeEndWalls.setEnabled(false);
                    bTakeEndWalls = false;
                }
                else {
                    cBSlotRadInCalcul.setEnabled(true);
                    bSlotRadInCalcul = true;
                    cBTakeEndWalls.setEnabled(true);
                    bTakeEndWalls = true;
                }
                updateUI();
            }
        });
        cBTakeGasAbsorptionForInterRad = new JCheckBox();
        cBNoGasAbsorptionInWallBalance = new JCheckBox();
        jBresetTFMmatch = new JButton("Reset to Default");
        jBresetTFMmatch.addActionListener(e-> {
            resetDefaultTFMmatch();
        });
        cBFormulaForTau = new JCheckBox();
        ntTfmChEmissMultiplier = new NumberTextField(controller, tfmChEmissMultiplier, 6, false,
                0.8,  1.2, "0.00", "Ch Emissivity/ Reflectivity factor", false);
        cbTFMemissAction = new XLComboBox(ChEmmFactorForTFMmatch.values());

        cBdo2DCheck = new JCheckBox();
        cBdo2DCheck.addActionListener(e->{
            bDo2Dcheck = cBdo2DCheck.isSelected();
            cBdo2DCalculation.setSelected(bDo2Dcheck);
        });
//        cBdo2DCheck.setEnabled(false);
//        ntS152LimitMin = new NumberTextField(controller, s152LimitMin, 6, false, 0.5,  1.5, "0.00", "Min Limit of DeltaT/(SurfT - CoreT)", false);
//        ntS152LimitMax = new NumberTextField(controller, s152LimitMax, 6, false, 1.5, 5.0, "0.00", "Max Limit of DeltaT/(SurfT - CoreT)", false);
        ntN2dCheck = new NumberTextField(controller, n2dCheck, 6, true, 0, 5, "#0", "Number of times to do 2DCheck)", true);
        ntN2dCheck.setEnabled(false);
        ntTauLimitMin = new NumberTextField(controller, tauLimitMin, 6, false, 0.1,  0.5, "0.00", "Min Limit (tg-two)/(tg-twm)", false);
        ntTauLimitMin.setEnabled(false);
        ntTauLimitMax = new NumberTextField(controller, tauLimitMax, 6, false, 0.5,  0.99, "0.00", "Max Limit (tg-two)/(tg-twm)", false);
        ntTauLimitMax.setEnabled(false);
        cBbEvalBotFirst = new JCheckBox();
        tfStartFlueTempHead = new NumberTextField(controller, startFlueTempHead, 6, false, 1, 1000, "#,###", "", false);
        cBHotCharge = new JCheckBox();
        cBbSectionProgress = new JCheckBox();
        cBbSlotProgress = new JCheckBox();
        cBbMindChHeight = new JCheckBox();
        cBAllowSecFuel = new JCheckBox();
        cBAllowSecFuel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bAllowSecFuel = (cBAllowSecFuel.isSelected());
                setAllowSecFuel(bAllowSecFuel);
            }
        });
        cBAllowRegenAirTemp = new JCheckBox();
        cBAllowRegenAirTemp.setEnabled(true);
        cBAllowRegenAirTemp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bAllowRegenAirTemp = (cBAllowRegenAirTemp.isSelected());
            }
        });
        cBShowFlueCompo = new JCheckBox();
        cBShowFlueCompo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                bShowFlueCompo = (cBShowFlueCompo.isSelected());
            }
        });

//        cBProfileForTFM = new JComboBox<UnitFceArray.ProfileBasis>(UnitFceArray.ProfileBasis.values());
        cBProfileForTFM = new XLComboBox(UnitFceArray.ProfileBasis.values());
        cBProfileForTFM.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tfmBasis = (UnitFceArray.ProfileBasis)cBProfileForTFM.getSelectedItem();
            }
        });
//        tfTFMStep = new NumberTextField(controller, tfmStep * 1000, 6,false, 200, 5000, "#,###", "Length Step for TFM Temperaure Profile");
        cBbAutoTempForLosses = new JCheckBox();
        cBbAutoTempForLosses.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                allowManualTempForLosses(!cBbAutoTempForLosses.isSelected());
            }
        });
        cBbSmoothenCurve = new JCheckBox();
//        cBbConsiderChTempProfile = new JCheckBox();
//        cBbConsiderChTempProfile.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                bConsiderChTempProfile = cBbConsiderChTempProfile.isSelected();
//            }
//        });
        cBbAdjustChTempProfile = new JCheckBox();
        cBbAdjustChTempProfile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bAdjustChTempProfile = cBbAdjustChTempProfile.isSelected();
            }
        });
//        tfOverUP = new NumberTextField(controller, overUP, 6, false, 1.0, 2.0, "0.##", "Over-capacity factor");
//        tfUnderUP = new NumberTextField(controller, underUP, 6, false, 0.05, 0.9, "0.##", "Under-capacity factor");
        cBbOnProductionLine = new JCheckBox();
        cBbOnProductionLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bOnProductionLine = cBbOnProductionLine.isSelected();
            }
        });
        cBNoEmissivityCorrFactor = new JCheckBox();
        cBNoEmissivityCorrFactor.setEnabled(false);
        cBNoGasRadiationToCharge = new JCheckBox();
        cBNoGasRadiationToCharge.setEnabled(false);
        cBgasAbsorptionHeilingen = new JCheckBox();
        cBgasAbsorptionHeilingen.setEnabled(false);
        cBdo2DCalculation = new JCheckBox();
        cBdo2DCalculation.addActionListener(e-> {
            bDo2dCalculation = cBdo2DCalculation.isSelected();
        });
        cBdo2DCalculation.setEnabled(false);
        cBOnTest = new JCheckBox();
        final DFHeating c = controller;
        cBOnTest.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bOnTest = cBOnTest.isSelected();
                c.itIsOnTest(bOnTest);
                enableOnTestParams();
            }
        });
        enableTFMmatchSettings();
        cBdoTFMmatch = new JCheckBox();
        cBdoTFMmatch.addActionListener(e-> {
            bDoTFMmatch = cBdoTFMmatch.isSelected();
            enableTFMmatchSettings();
        });
        setParamsToDefaultValues(true);
        createTuningPanel();
//        updateUI();
    }

    void enableOnTestParams() {
        if (bOnTest) {
            noGasRadiationToCharge = false;
            cBNoGasRadiationToCharge.setSelected(noGasRadiationToCharge);
            cBNoGasRadiationToCharge.setEnabled(true);
            bGasAbsorptionHeilingen = false;
            cBgasAbsorptionHeilingen.setSelected(bGasAbsorptionHeilingen);
            cBgasAbsorptionHeilingen.setEnabled(true);
            bNoEmissivityCorrFactor = false;
            cBNoEmissivityCorrFactor.setSelected(bNoEmissivityCorrFactor);
            cBNoEmissivityCorrFactor.setEnabled(true);
            cBdo2DCalculation.setSelected(bDo2dCalculation);
            cBdo2DCalculation.setEnabled(true);
        }
        else {
            noGasRadiationToCharge = false;
            cBNoGasRadiationToCharge.setSelected(noGasRadiationToCharge);
            cBNoGasRadiationToCharge.setEnabled(false);
            bGasAbsorptionHeilingen = false;
            cBgasAbsorptionHeilingen.setSelected(bGasAbsorptionHeilingen);
            cBgasAbsorptionHeilingen.setEnabled(false);
            bNoEmissivityCorrFactor = false;
            cBNoEmissivityCorrFactor.setSelected(bNoEmissivityCorrFactor);
            cBNoEmissivityCorrFactor.setEnabled(false);
            bDo2dCalculation = bDo2Dcheck;
            cBdo2DCalculation.setSelected(bDo2dCalculation);
            cBdo2DCalculation.setEnabled(false);
        }
    }

    void enableTFMmatchSettings() {
        boolean ena = bDoTFMmatch;
        ntTfmChEmissMultiplier.setEnabled(ena);
        jBresetTFMmatch.setEnabled(ena);
        cbTFMemissAction.setEnabled(ena);
    }

    public void enableDataEntry(boolean ena) {
        cBTakeEndWalls.setEnabled(ena);
        cBSlotRadInCalcul.setEnabled(ena);
    }

    void setAllowSecFuel(boolean allow) {
        controller.setAllowSecFuel(allow);
    }

    void allowManualTempForLosses(boolean bAllow) {
        controller.allowManualTempForLosses(bAllow);
    }

//    double getTFMStep(){
//        setTFMStep();
//        return tfmStep;
//    }

//    private void setTFMStep() {
//        tfmStep = tfTFMStep.getData() / 1000;
//    }

    public void takeValuesFromUI() {
        preSets.get(FurnaceFor.MANUAL).takeFromUI();
        epsilonO = selectedPreset.eO;
        gasWallHTMultipler = selectedPreset.gMulti;
        alphaConvFired = selectedPreset.aConvFired;
        alphaConvRecu = selectedPreset.aConvRecu;
        radiationMultiplier = selectedPreset.radiationMultiplier;
        if (bNoEmissivityCorrFactor)
            emmFactor = 1;
        else
            emmFactor = selectedPreset.eFact;

        errorAllowed = tferrorAllowed.getData();
        wallLoss = tfwallLoss.getData();
        suggested1stCorrection = tfsuggested1stCorrection.getData();
        correctionForTooLowGas = tfCorrectionforTooLowGas.getData();
        bDynamicGasTempCorrection = chbDynamicGasTempCorrection.isSelected();
        if (bDynamicGasTempCorrection) {
            minGasTempCorrection = tfMinGasTempCorrection.getData();
            maxGasTempCorrection = tfMaxGasTempCorrection.getData();
        }
        bTakeEndWalls = (cBTakeEndWalls.isSelected());
        bEvalBotFirst = (cBbEvalBotFirst.isSelected());
        bHotCharge = cBHotCharge.isSelected();
        startFlueTempHead = tfStartFlueTempHead.getData();
        bTakeGasAbsorptionForInterRad = (cBTakeGasAbsorptionForInterRad.isSelected());
        bNoGasAbsorptionInWallBalance = (cBNoGasAbsorptionInWallBalance.isSelected());
        bDoTFMmatch = cBdoTFMmatch.isSelected();
        bFormulaForTau = (cBFormulaForTau.isSelected());
        tfmChEmissMultiplier =  ntTfmChEmissMultiplier.getData();

        appTFMchEmissCorrectionType  = (ChEmmFactorForTFMmatch)(cbTFMemissAction.getSelectedItem());
        bApplyChReflectivityMultiplier = (appTFMchEmissCorrectionType == ChEmmFactorForTFMmatch.REFLECTIVITY);
        bApplyChEmissMultiplier = (appTFMchEmissCorrectionType == ChEmmFactorForTFMmatch.EMISSIVITY);

        bDo2Dcheck = cBdo2DCheck.isSelected();
        bSlotRadInCalcul = cBSlotRadInCalcul.isSelected();
        bSectionProgress = (cBbSectionProgress.isSelected());
        bSlotProgress = (cBbSlotProgress.isSelected());
        bMindChHeight = (cBbMindChHeight.isSelected());
        bAllowSecFuel = (cBAllowSecFuel.isSelected());
        bAllowRegenAirTemp = (cBAllowRegenAirTemp.isSelected());
        bAutoTempForLosses = cBbAutoTempForLosses.isSelected();
        bSmoothenCurve = cBbSmoothenCurve.isSelected();
//        bConsiderChTempProfile = cBbConsiderChTempProfile.isSelected();
        bAdjustChTempProfile= cBbAdjustChTempProfile.isSelected();
//        overUP = tfOverUP.getData();
//        underUP = tfUnderUP.getData();
        bOnProductionLine = cBbOnProductionLine.isSelected();
        bNoEmissivityCorrFactor = cBNoEmissivityCorrFactor.isSelected();
        noGasRadiationToCharge = cBNoGasRadiationToCharge.isSelected();
        bGasAbsorptionHeilingen = cBgasAbsorptionHeilingen.isSelected();
        bDo2dCalculation = cBdo2DCalculation.isSelected();
        bOnTest = cBOnTest.isSelected();
        bBaseOnZonalTemperature = (cBbaseOnZonalTemperature.isSelected());
        n2dCheck = (int)ntN2dCheck.getData();
//        s152LimitMin = ntS152LimitMin.getData();
//        s152LimitMax = ntS152LimitMax.getData();
        tauLimitMin = ntTauLimitMin.getData();
        tauLimitMax = ntTauLimitMax.getData();
//        setTFMStep();
    }

    void updateUI() {
        tferrorAllowed.setData(errorAllowed);
        tfwallLoss.setData(wallLoss);
        tfsuggested1stCorrection.setData(suggested1stCorrection);
        tfCorrectionforTooLowGas.setData(correctionForTooLowGas);
        tfMinGasTempCorrection.setData(minGasTempCorrection);
        tfMaxGasTempCorrection.setData(maxGasTempCorrection);
        cBTakeEndWalls.setSelected(bTakeEndWalls);
        cBTakeGasAbsorptionForInterRad.setSelected(bTakeGasAbsorptionForInterRad);
        cBNoGasAbsorptionInWallBalance.setSelected(bNoGasAbsorptionInWallBalance);
        cBdoTFMmatch.setSelected(bDoTFMmatch);
        cBFormulaForTau.setSelected(bFormulaForTau);
        ntTfmChEmissMultiplier.setData(tfmChEmissMultiplier);
//        ntS152LimitMin.setData(s152LimitMin);
//        ntS152LimitMax.setData(s152LimitMax);
        cbTFMemissAction.setSelectedItem(appTFMchEmissCorrectionType);
        ntTauLimitMin.setData(tauLimitMin);
        ntTauLimitMax.setData(tauLimitMax);
        ntN2dCheck.setData(n2dCheck);
        cBdo2DCheck.setSelected(bDo2Dcheck);
        cBbEvalBotFirst.setSelected(bEvalBotFirst);
        cBHotCharge.setSelected(bHotCharge);
        tfStartFlueTempHead.setData(startFlueTempHead);
        cBSlotRadInCalcul.setSelected(bSlotRadInCalcul);
        cBbSectionProgress.setSelected(bSectionProgress);
        cBbSlotProgress.setSelected(bSlotProgress);
        cBbMindChHeight.setSelected(bMindChHeight);
//        cBAllowSecFuel.setSelected(bAllowSecFuel);
        if (cBAllowSecFuel.isSelected() != bAllowSecFuel)
            cBAllowSecFuel.doClick();   // this fire event, setSelected() does not!
        cBAllowRegenAirTemp.setSelected(bAllowRegenAirTemp);
        cBShowFlueCompo.setSelected(bShowFlueCompo);
        preSets.get(FurnaceFor.MANUAL).putInUI();
        cBProfileForTFM.setSelectedItem(tfmBasis);
//        tfTFMStep.setData(tfmStep * 1000);
        cBbAutoTempForLosses.setSelected(bAutoTempForLosses);
        cBbSmoothenCurve.setSelected(bSmoothenCurve);
//        cBbConsiderChTempProfile.setSelected(bConsiderChTempProfile);
        cBbAdjustChTempProfile.setSelected(bAdjustChTempProfile);
//        tfOverUP.setData(overUP);
//        tfUnderUP.setData(underUP);
        cBbOnProductionLine.setSelected(bOnProductionLine);
        cBNoEmissivityCorrFactor.setSelected(bNoEmissivityCorrFactor);
        cBNoGasRadiationToCharge.setSelected(noGasRadiationToCharge);
        cBgasAbsorptionHeilingen.setSelected(bGasAbsorptionHeilingen);
        cBdo2DCalculation.setSelected(bDo2dCalculation);
        cBOnTest.setSelected(bOnTest);
        cBbaseOnZonalTemperature.setSelected(bBaseOnZonalTemperature);
    }

    public void setSelectedProc(FurnaceFor selectedProc) {
        this.selectedProc = selectedProc;
        selectedPreset = preSets.get(selectedProc);
        takeValuesFromUI();
    }

    public void showSectionProgress(boolean bShow) {
        cBbSectionProgress.setSelected(bShow);
//        bSectionProgress = bShow;
//        bSlotProgress = false;
    }
    public void showSlotProgress(boolean bShow) {
        cBbSlotProgress.setSelected(bShow);
//        bSectionProgress = false;
//        bSlotProgress = bShow;
    }

    public String dataInXML(DFHTuningParams.FurnaceFor fceFor) {
        updateUI();
        String xmlStr = "";
        if (fceFor == FurnaceFor.MANUAL) {
            PreSetTunes pre = preSets.get(FurnaceFor.MANUAL);
            xmlStr += XMLmv.putTag("epsilonO", pre.eO);
            xmlStr += XMLmv.putTag("gasWallHTMultipler", pre.gMulti);
            xmlStr += XMLmv.putTag("alphaConv", pre.aConvFired);
            xmlStr += XMLmv.putTag("alphaConvRecu", pre.aConvRecu);
            xmlStr += XMLmv.putTag("emmFactor", pre.eFact);
            xmlStr += XMLmv.putTag("radiationMultiplier", pre.radiationMultiplier);
        }
        xmlStr += XMLmv.putTag("bTakeEndWalls", (bTakeEndWalls) ? 1 : 0);
        xmlStr += XMLmv.putTag("bTakeGasAbsorption", (bTakeGasAbsorptionForInterRad) ? 1 : 0);
        xmlStr += XMLmv.putTag("bEvalBotFirst", (bEvalBotFirst) ? 1: 0);
        xmlStr += XMLmv.putTag("bHotCharge", (bHotCharge) ? 1: 0);
        xmlStr += XMLmv.putTag("startFlueTempHead", startFlueTempHead);
//        xmlStr += XMLmv.putTag("bSectionalFlueExh", (bSectionalFlueExh) ? 1: 0);
        xmlStr += XMLmv.putTag("bSlotRadInCalcul", (bSlotRadInCalcul) ? 1: 0);
        xmlStr += XMLmv.putTag("bMindChHeight", (bMindChHeight) ? 1: 0);
        xmlStr += XMLmv.putTag("bAllowSecFuel", (bAllowSecFuel) ? 1: 0);
        xmlStr += XMLmv.putTag("bAllowRegenAirTemp", (bAllowRegenAirTemp) ? 1: 0);
        // Performance base
//        xmlStr += XMLmv.putTag("bConsiderChTempProfile", (bConsiderChTempProfile) ? 1: 0);
        xmlStr += XMLmv.putTag("bOnProductionLine", (bOnProductionLine) ? 1: 0);
        // performance table and field performance
        xmlStr += XMLmv.putTag("unitOutputOverRange", unitOutputOverRange);
        xmlStr += XMLmv.putTag("unitOutputUnderRange", unitOutputUnderRange);
        xmlStr += XMLmv.putTag("unitOutputOverRangeForTable", unitOutputOverRangeForTable);
        xmlStr += XMLmv.putTag("bRespectFirstFiredZoneExitStripTemp", bRespectFirstFiredZoneExitStripTemp);
        xmlStr += XMLmv.putTag("exitTempTolerance", exitTempTolerance);
        xmlStr += XMLmv.putTag("outputStep", outputStep);
        xmlStr += XMLmv.putTag("widthOverRange", widthOverRange);
        xmlStr += XMLmv.putTag("widthStep", widthStep);

        xmlStr += XMLmv.putTag("tfmBasis", "" + tfmBasis);
        xmlStr += XMLmv.putTag("bDoTFMmatch", bDoTFMmatch);
        if (bDoTFMmatch) {
            xmlStr += XMLmv.putTag("tfmChEmissMultiplier", tfmChEmissMultiplier);
            xmlStr += XMLmv.putTag("appTFMchEmissCorrectionType", "" + appTFMchEmissCorrectionType);
        }

        return xmlStr;
    }

    public boolean takeDataFromXML(String xmlStr) {
        ValAndPos vp;
        boolean bRetVal = true;
        try {
            setParamsToDefaultValues(true);
            PreSetTunes pre = preSets.get(FurnaceFor.MANUAL);
            vp = XMLmv.getTag(xmlStr, "epsilonO", 0);
            if (vp.val.length() > 0)
                pre.eO = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "gasWallHTMultipler", 0);
            if (vp.val.length() > 0)
                pre.gMulti = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "alphaConv", 0);
            if (vp.val.length() > 0)
                pre.aConvFired = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "alphaConvRecu", 0);
            if (vp.val.length() > 0)
                pre.aConvRecu = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "emmFactor", 0);
            if (vp.val.length() > 0)
                pre.eFact = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "radiationMultiplier", 0);
            if (vp.val.length() > 0)
                pre.radiationMultiplier = Double.valueOf(vp.val);
            pre.putInUI();

            vp = XMLmv.getTag(xmlStr, "bTakeEndWalls", 0);
            bTakeEndWalls = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bTakeGasAbsorption", 0);
            bTakeGasAbsorptionForInterRad = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bEvalBotFirst", 0);
            bEvalBotFirst = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bHotCharge", 0);
            if (vp.val.length() > 0)
                bHotCharge = (vp.val.equals("1"));
            else
                bHotCharge = false;
            vp = XMLmv.getTag(xmlStr, "startFlueTempHead", 0);
            if (vp.val.length() > 0)
                startFlueTempHead =  Double.valueOf(vp.val);
            else
                startFlueTempHead = defaultStartFlueTempHead;
//            vp = XMLmv.getTag(xmlStr, "bSectionalFlueExh", 0);
//            bSectionalFlueExh = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bSlotRadInCalcul", 0);
            bSlotRadInCalcul = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bMindChHeight", 0);
            bMindChHeight = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bAllowSecFuel", 0);
            bAllowSecFuel = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bAllowRegenAirTemp", 0);
            bAllowRegenAirTemp = (vp.val.equals("1"));

            vp = XMLmv.getTag(xmlStr, "unitOutputOverRange", vp.endPos);
            if (vp.val.length() > 0)
                unitOutputOverRange = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "unitOutputUnderRange", vp.endPos);
            if (vp.val.length() > 0)
                unitOutputUnderRange = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "unitOutputOverRangeForTable", vp.endPos);
            if (vp.val.length() > 0)
                unitOutputOverRangeForTable = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "bRespectFirstFiredZoneExitStripTemp", vp.endPos);
            bRespectFirstFiredZoneExitStripTemp = vp.val.equals("1");
            vp = XMLmv.getTag(xmlStr, "exitTempTolerance", vp.endPos);
            if (vp.val.length() > 0)
                exitTempTolerance = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "outputStep", vp.endPos);
            if (vp.val.length() > 0)
                outputStep = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "widthOverRange", vp.endPos);
            if (vp.val.length() > 0)
                widthOverRange = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "widthStep", vp.endPos);
            if (vp.val.length() > 0)
                widthStep = Double.valueOf(vp.val);
/*
        xmlStr += XMLmv.putTag("bDoTFMmatch", bDoTFMmatch);
        if (bDoTFMmatch) {
            xmlStr += XMLmv.putTag("chEmmissCorrectionFactor", chEmmissCorrectionFactor);
            xmlStr += XMLmv.putTag("appTFMchEmissCorrectionType", "" + appTFMchEmissCorrectionType);
        }

 */
            vp = XMLmv.getTag(xmlStr, "tfmBasis", 0);
            if (vp.val.length() > 0)
                tfmBasis = UnitFceArray.ProfileBasis.getEnum(vp.val);
            vp = XMLmv.getTag(xmlStr, "bDoTFMmatch", 0);
            bDoTFMmatch = vp.val.equals("1");
            if (bDoTFMmatch) {
                vp = XMLmv.getTag(xmlStr, "tfmChEmissMultiplier", vp.endPos);
                if (vp.val.length() > 0)
                    tfmChEmissMultiplier = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "appTFMchEmissCorrectionType", vp.endPos);
                if (vp.val.length() > 0)
                    appTFMchEmissCorrectionType = ChEmmFactorForTFMmatch.getEnum(vp.val);
                enableTFMmatchSettings();
            }
        } catch (NumberFormatException e) {
            bRetVal = false;
        }
        bBaseOnZonalTemperature = false;
        updateUI();
        return bRetVal;
    }

    public void setParamsToDefaultValues(boolean resetTMmatch) {
        errorAllowed = defaultErrorAllowed;
        suggested1stCorrection = defaultSuggested1stCorrection;
        correctionForTooLowGas = defaultCorrectionForTooLowGas;
        bSmoothenCurve = true;
        bHotCharge = false;
        bTakeEndWalls = true;   // false;
        bTakeGasAbsorptionForInterRad = false;
        bEvalBotFirst = false;
        startFlueTempHead = defaultStartFlueTempHead;
        bSectionalFlueExh = false;
        bSlotRadInCalcul = true; // false;
        bSectionProgress = true;
        bSlotProgress = true;
        bMindChHeight = true;
        bAllowSecFuel = true; // false;
        setAllowSecFuel(bAllowSecFuel);
        bAllowRegenAirTemp = true;
        bShowFlueCompo = true;
        bAutoTempForLosses = true;
        bTakeGasAbsorptionForInterRad = false;
        bNoGasAbsorptionInWallBalance = false;
        bConsiderChTempProfile = true;
        bOnProductionLine = false;

        unitOutputOverRange = 1.05;
        unitOutputUnderRange = 0.9;
        unitOutputOverRangeForTable = 1.05;
        exitTempTolerance = 5;   // it is (T - 5) > t >= (T + 5)
        outputStep = 0.2;
        widthOverRange = 1.1;
        //    double minWidthFactor = 0.8;
        widthStep = 0.1;

        bDynamicGasTempCorrection = false; // no saving of this data
        minGasTempCorrection = defaultMinGasTempCorrection;
        maxGasTempCorrection = defaultMaxGasTempCorrection;
        bDoTFMmatch = false;
        bDo2dCalculation = false;
        bDo2Dcheck = false;
        n2dCheck = defaultN2dCheck;
        tauLimitMin = defaultTauLimitMin;
        tauLimitMax = defaultTauLimitMax;
        bFormulaForTau = false;
        bOnTest = false;
        enableOnTestParams();
        cBOnTest.setSelected(false);
        if (resetTMmatch)
            resetDefaultTFMmatch();
        enableTFMmatchSettings();
        updateUI();
    }

    GridBagConstraints mainGbc = new GridBagConstraints();
    JPanel mainTuningPanel = new FramedPanel(new GridBagLayout());
    MultiPairColPanel tfmMatchPan, settings1Pan, settings2Pan, performancePan, testPan;
    JPanel procSetPan;
//    FramedPanel mainTuningPanel;

    void createTuningPanel() {
        mainTuningPanel = new FramedPanel(new GridBagLayout());
        mainTuningPanel.setBackground(new JPanel().getBackground());
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        mainGbc.gridwidth = 2;
        mainTuningPanel.add(procSetPan = getProcSetPanel(), mainGbc);
        mainGbc.gridy++;
        mainGbc.gridwidth = 1;
        mainGbc.gridx = 0;
        tfmMatchPan = tfmMatchP(); // displayed in operations view
//        mainTuningPanel.add(tfmMatchPan = tfmMatchP(), mainGbc);
//        mainGbc.gridy++;
        mainTuningPanel.add(settings1Pan = settings1Pan(), mainGbc);
        mainGbc.gridx++;
        mainTuningPanel.add(settings2Pan = setting2Pan(), mainGbc);
        mainGbc.gridx--;
        mainGbc.gridy++;
        mainTuningPanel.add(performancePan = performancePan(), mainGbc);
        mainGbc.gridy++;
        mainTuningPanel.add(jbRestParams, mainGbc);
//        mainGbc.gridwidth = 2;
//        mainTuningPanel.add(procSetPan = getProcSetPanel(), mainGbc);
//        mainGbc.gridy++;
        mainGbc.gridx++;
        mainGbc.gridy--;
        mainGbc.gridheight = 2;
        mainTuningPanel.add(testPan = testPanel(), mainGbc);
    }

    public JPanel getTuningPanel() {
        return mainTuningPanel;
    }

    public void addToTuningPanel(JPanel jp)  {
        if (mainGbc.gridx == 0)
            mainGbc.gridx++;
        else {
            mainGbc.gridy++;
            mainGbc.gridx = 0;
        }
        mainTuningPanel.add(jp, mainGbc);
    }

    JPanel getProcSetPanel() {
        JPanel jp = new FramedPanel(new GridBagLayout());
        jp.setBackground(new JPanel().getBackground());
        GridBagConstraints gbc = new GridBagConstraints();
        Dimension headSize = new Dimension(120, 20);
        JLabel h;
        gbc.gridx = 0;
        gbc.gridy = 0;
        jp.add(new JLabel("<html><b>Basic Calculation Parameters</b></html>"));
        gbc.gridx = 1;
        gbc.gridy++;
        for (FurnaceFor proc : FurnaceFor.values()) {
            h = new JLabel("" + proc);
            h.setPreferredSize(headSize);
            if (selectedProc == proc)
                h.setBackground(Color.DARK_GRAY);
            jp.add(h, gbc);
            gbc.gridx++;
        }
        gbc.gridy++;
        gbc.gridx = 0;
        jp.add(preSetRowHead(), gbc);
        Iterator keys = preSets.keySet().iterator();
        while (keys.hasNext()) {
            gbc.gridx++;
            jp.add(preSets.get(keys.next()).presetsPan(), gbc);
        }
        return jp;
    }

    MultiPairColPanel testPanel() {
        MultiPairColPanel jp = new MultiPairColPanel("Test Settings");
        jp.addItemPair("Enable Test Mode with the following", cBOnTest);
        jp.addItemPair("Do 2D calculation in TESTING mode", cBdo2DCalculation);
        jp.addItemPair("Neglect Gas Emissivity Correction Factor", cBNoEmissivityCorrFactor);
        jp.addItemPair("No Gas Radiation To Charge", cBNoGasRadiationToCharge);
        jp.addItemPair("Gas Absorption as per Heilingenstadt", cBgasAbsorptionHeilingen);
        jp.addItem(new JLabel("(for STRIP furnace this is considered enabled)"));
        return jp;
    }

    NumberTextField tferrorAllowed, tfwallLoss;
    NumberTextField tfsuggested1stCorrection, tfCorrectionforTooLowGas;
            // Todo the above two are not used
    NumberTextField tfMinGasTempCorrection, tfMaxGasTempCorrection;
    JCheckBox chbDynamicGasTempCorrection;
//    NumberTextField tfTFMStep;
    // for evaluation from reference performance
    public boolean bConsiderChTempProfile = true;
    public boolean bRespectFirstFiredZoneExitStripTemp = false;
        // while adjusting strip temperatures for respecting Exit Zone Minimum temperature
    JCheckBox cbRespectFirstFiredZoneExitStripTemp;
    JCheckBox cBbConsiderChTempProfile;
    public boolean bAdjustChTempProfile = true;    // while respecting last zone minimum fce temp
    JCheckBox cBbAdjustChTempProfile;
    public boolean bOnProductionLine = false;
    JCheckBox cBbOnProductionLine;
    // parameters for Field reference Data
    public double unitOutputOverRange = 1.05;
    public double unitOutputUnderRange = 0.9;
    public double unitOutputOverRangeForTable = 1.05;
    public double exitTempTolerance = 5;   // it is (T - 5) > t >= (T + 5)
    NumberTextField ntUnitOutputOverRange;
    NumberTextField ntUnitOutputUnderRange;
    NumberTextField ntUnitOutputOverRangeForTable;
    NumberTextField ntExitTempTolerance;

    // parameters for Performance Table
//    double minOutputFactor = 0.7;
    public double outputStep = 0.2;
    public double widthOverRange = 1.1;
    //    double minWidthFactor = 0.8;
    public double widthStep = 0.1;
    //    NumberTextField ntMinOutputFactor;
    NumberTextField ntOutputStep;
    NumberTextField ntWidthOverRange;
    //    NumberTextField ntMinWidthFactor;
    NumberTextField ntWidthStep;

    MultiPairColPanel settings1Pan() {
        MultiPairColPanel jp = new MultiPairColPanel("Settings 1", 250, 60);
        jp.addItemPair("Error Allowed (degC)", tferrorAllowed);
        jp.addItemPair("Wall loss (for internal use)", tfwallLoss);
        jp.addItemPair("Smoothen Temperature trends", cBbSmoothenCurve);
        jp.addItemPair("Enable Dynamic Gas Temp Correction", chbDynamicGasTempCorrection);
        jp.addItemPair(tfMinGasTempCorrection);
        jp.addItemPair(tfMaxGasTempCorrection);
        jp.addItem("(Ensure Min < Max)");
        jp.addItemPair("Full Formula for Tau", cBFormulaForTau);
        jp.addItemPair("Do Correction with charge 2D Calculation", cBdo2DCheck);
        jp.addItemPair(ntN2dCheck);
        jp.addItemPair(ntTauLimitMin);
        jp.addItemPair(ntTauLimitMax);
        return jp;
    }

    MultiPairColPanel setting2Pan() {
        MultiPairColPanel jp = new MultiPairColPanel("Settings 2", 250, 60);
        jp.addItemPair("Evaluate Bottom Section First", cBbEvalBotFirst);
        jp.addItemPair("Calculate for Hot Charge", cBHotCharge);
        jp.addItemPair("Start Flue Temp Head Hot Charge", tfStartFlueTempHead);
        jp.addItemPair("Show Section Progress", cBbSectionProgress);
        jp.addItemPair("Show Stepwise Progress", cBbSlotProgress);
        jp.addItemPair("Mind Charge Height for Radiation", cBbMindChHeight);
        jp.addItemPair("Allow Section-wise Fuel", cBAllowSecFuel);
        jp.addItemPair("Allow REGEN Air Temperature", cBAllowRegenAirTemp);
        jp.addItemPair("Show Flue Composition", cBShowFlueCompo);
        jp.addItemPair("Auto Section Temp for Losses", cBbAutoTempForLosses);
        jp.addItemPair("Take Gas Absorption in Internal Rad", cBTakeGasAbsorptionForInterRad);
        jp.addItemPair("No Gas Absorption in Wall Balance", cBNoGasAbsorptionInWallBalance);
        return jp;
    }

    void resetDefaultTFMmatch() {
        tfmChEmissMultiplier = 0.93;
        appTFMchEmissCorrectionType = ChEmmFactorForTFMmatch.REFLECTIVITY;
        tfmBasis = UnitFceArray.ProfileBasis.FCETEMP;
        bFormulaForTau = false;
        updateUI();
    }

    MultiPairColPanel tfmMatchP() {
        MultiPairColPanel jp = new MultiPairColPanel("Transfer to TFM"); //,180, 60);
        jp.addItemPair("Temperature Profile for TFM", cBProfileForTFM);
        jp.addBlank();
        jp.addItemPair("Enable TFM matching", cBdoTFMmatch);
        jp.addItem(jBresetTFMmatch);
        jp.addItemPair(ntTfmChEmissMultiplier);
        jp.addItemPair("Apply above factor on charge", cbTFMemissAction);
        return jp;
    }

    MultiPairColPanel getTFMmatchPanel() {
        return tfmMatchPan;
    }

    JCheckBox cBbEvalBotFirst, cBbSectionProgress, cBbSlotProgress;
    JCheckBox cBbMindChHeight;
    JCheckBox cBAllowSecFuel;
    JCheckBox cBAllowRegenAirTemp;
    JCheckBox cBShowFlueCompo;
    JComboBox<UnitFceArray.ProfileBasis> cBProfileForTFM;
    JCheckBox cBSlotRadInCalcul, cBTakeEndWalls, cBTakeGasAbsorptionForInterRad;
    JCheckBox cBNoGasAbsorptionInWallBalance;
    JCheckBox cBbAutoTempForLosses;
    JCheckBox cBbSmoothenCurve;
    JCheckBox cBHotCharge;
    NumberTextField tfStartFlueTempHead;


    MultiPairColPanel performancePan() {
        MultiPairColPanel jp = new MultiPairColPanel("Reference Performance (STRIP Fce)",200, 60);
//        jp.addItemPair("Consider Charge Temp Profile", cBbConsiderChTempProfile);
        jp.addItemPair("Adjust Charge Temp Profile", cBbAdjustChTempProfile);
        jp.addItemPair("On Production Line", cBbOnProductionLine);
        return jp;
    }

    JCheckBox cBbaseOnZonalTemperature;


    public MultiPairColPanel userTunePan() {
        MultiPairColPanel jp = new MultiPairColPanel("Handling Internal Radiation", 300,6);
        jp.addItemPair("Evaluate Internal Radiation", cBSlotRadInCalcul);
        jp.addItemPair("Evaluate EndWall Radiation", cBTakeEndWalls);
        jp.addBlank();
        jp.addItem(new JLabel("<html><font color='red'>Choice below is on TRIAL and only for STRIP heating</html>"));
        jp.addItemPair("Use Zonal Temperature (NO Heat Balance)", cBbaseOnZonalTemperature);
        return jp;
    }

    JPanel preSetRowHead() {
        JPanel jp = new JPanel(new GridBagLayout());
        jp.setBackground(new JPanel().getBackground());
        GridBagConstraints gbc = new GridBagConstraints();
        Dimension headSize = new Dimension(350, 20);
        gbc.gridx = 0;
        gbc.gridy = 0;
        int hNum = 0;
        JLabel h1 = new JLabel(presetRowHeader[hNum]);
        h1.setPreferredSize(headSize);
        jp.add(h1, gbc);
        hNum++;
        gbc.gridy++;
        JLabel h2 = new JLabel(presetRowHeader[hNum]);
        h2.setPreferredSize(headSize);
        jp.add(h2, gbc);
        hNum++;
        gbc.gridy++;
        JLabel h3 = new JLabel(presetRowHeader[hNum]);
        h3.setPreferredSize(headSize);
        jp.add(h3, gbc);
        hNum++;
        gbc.gridy++;
        JLabel h4 = new JLabel(presetRowHeader[hNum]);
        h4.setPreferredSize(headSize);
        jp.add(h4, gbc);
        hNum++;
        gbc.gridy++;
        JLabel h5 = new JLabel(presetRowHeader[hNum]);
        h5.setPreferredSize(headSize);
        jp.add(h5, gbc);
        hNum++;
        gbc.gridy++;
        JLabel h6 = new JLabel(presetRowHeader[hNum]);
        h6.setPreferredSize(headSize);
        jp.add(h6, gbc);
        return jp;
    }

    void getPerfTableSettings(Component c) {
        JDialog dlg = new PerfTableSetting(DFHeating.mainF);
        dlg.setLocationRelativeTo(c);
        dlg.setVisible(true);
        controller.mainF.setVisible(true);
    }

    //    public void setPerfTurndownSettings(double minOutputFactor, double minWidthFactor) {
//        this. minOutputFactor = minOutputFactor;
//        this.minWidthFactor = minWidthFactor;
//    }

    public int xlTuningDetails(Sheet sheet, ExcelStyles styles) {
        Cell cell;
        Row r;
        r = sheet.createRow(0);
        cell = r.createCell(0);
        cell.setCellStyle(styles.csHeader1);
        cell.setCellValue("Tuning Parameters");
        sheet.setColumnWidth(1, 12000);
        sheet.setColumnWidth(3, 4000);
        sheet.setColumnWidth(4, 12000);
         int topRow = 4, col, row, rRow;
        int leftCol = 1;
        PreSetTunes manPre = preSets.get(FurnaceFor.MANUAL);
        row = topRow;

        r = sheet.createRow(row);
        col = leftCol;
        cell = r.createCell(col);
        cell.setCellValue("Calculation Parameters for MANUAL");
        cell.setCellStyle(styles.csHeader2);
//        CellRangeAddress range = new CellRangeAddress(row, row, col, col + 1);
//        sheet.addMergedRegion(range);
        row++;

        r = sheet.createRow(row++);
        col = leftCol;
        cell = r.createCell(col++);
        cell.setCellValue(manPre.tfeO.getName());
        cell = r.createCell(col);
        cell.setCellValue(manPre.tfeO.toString());

        r = sheet.createRow(row++);
        col = leftCol;
        cell = r.createCell(col++);
        cell.setCellValue(manPre.tfgMulti.getName());
        cell = r.createCell(col);
        cell.setCellValue(manPre.tfgMulti.toString());

        r = sheet.createRow(row++);
        col = leftCol;
        cell = r.createCell(col++);
        cell.setCellValue(manPre.tfaConvFired.getName());
        cell = r.createCell(col);
        cell.setCellValue(manPre.tfaConvFired.toString());

        r = sheet.createRow(row++);
        col = leftCol;
        cell = r.createCell(col++);
        cell.setCellValue(manPre.tfaConvRecu.getName());
        cell = r.createCell(col);
        cell.setCellValue(manPre.tfaConvRecu.toString());


        r = sheet.createRow(row++);
        col = leftCol;
        cell = r.createCell(col++);
        cell.setCellValue(manPre.tfgMulti.getName());
        cell = r.createCell(col);
        cell.setCellValue(manPre.tfgMulti.toString());

        r = sheet.createRow(row++);
        col = leftCol;
        cell = r.createCell(col++);
        cell.setCellValue(manPre.tfRadiationMultiplier.getName());
        cell = r.createCell(col);
        cell.setCellValue(manPre.tfRadiationMultiplier.toString());

        col = 4;
        rRow = styles.xlMultiPairColPanel(testPan, sheet, topRow, col) + 1;
        topRow = Math.max(row, rRow);
        col = 1;
        row = styles.xlMultiPairColPanel(settings1Pan, sheet, topRow, col) + 1;
        col = 4;
        rRow = styles.xlMultiPairColPanel(settings2Pan, sheet, topRow, col) + 1;
        row = Math.max(row, rRow);
        col = 1;
        row = styles.xlMultiPairColPanel(performancePan, sheet, row, col) + 1;
        return  Math.max(rRow, row);
    }

    class PerfTableSetting extends JDialog {
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ActionListener li;
        Frame parent;

        PerfTableSetting(Frame parent) {
            super(parent, "", Dialog.ModalityType.DOCUMENT_MODAL);
            this.parent = parent;
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
                        if (!isInError()) {
                            takeSetValues();
                            closeThisWindow();
                        }
                    } else if (src == cancel) {
                        closeThisWindow();
                    }
                }
            };
            ok.addActionListener(li);
            cancel.addActionListener(li);
            Container dlgP = getContentPane();
            JPanel jp = new JPanel(new BorderLayout());

            jp.add(perfTableSettings(), BorderLayout.NORTH);
            jp.add(fieldRefDataSettings(), BorderLayout.CENTER);
            jp.add(buttonPanel(), BorderLayout.SOUTH);
            dlgP.add(jp);
        }

        JPanel fieldRefDataSettings() {
            MultiPairColPanel jp = new MultiPairColPanel("Settings for Field Reference Data", 250, 60);
            String msg = "<html><h3>For Field results to be taken in</h3>" +
                    "<blockQuote>1. The strip Width has to be in the range<p>" +
                    "&emsp;Maximum Width for DFH Process and<p>" +
                    "&emsp;Maximum Width / A</blockquote>" +
                    "<blockquote>2. UnitOutput must be within the the range factors B and C<p>" +
                    "&emsp;applied to the Maximum Unit Output of the DFHProcess</blockquote>" +
                    "<blockquote>3. Strip Exit Temperature for the DFH Process with Margin (D)<p>" +
                    "<&emsp;given below<p>" +
                    "</html>";
            JLabel toNote = new JLabel(msg);
            jp.addItem(toNote);
            ntUnitOutputOverRange = new NumberTextField(controller, unitOutputOverRange, 6, false, 1.0, 1.2, "0.00", "Unit Output OverRange factor (B)");
            ntUnitOutputUnderRange = new NumberTextField(controller, unitOutputUnderRange, 6, false, 0.8, 1.0, "0.00", "Unit Output UnderRange factor (C)");
            ntExitTempTolerance = new NumberTextField(controller, exitTempTolerance, 6, false, 1, 20, "##",
                    "Margin (+-) on Exit Temperature in degC (D)");
            String explanation = "[(T - Margin) > t >= (T + Margin)]";
            jp.addItemPair(ntUnitOutputOverRange);
            jp.addItemPair(ntUnitOutputUnderRange);
            jp.addBlank();
            jp.addItemPair(ntExitTempTolerance);
            jp.addItem(explanation);
            return jp;
        }

        JPanel perfTableSettings() {
            MultiPairColPanel jp = new MultiPairColPanel("Settings for Performance Table", 200, 60);
//            ntMinOutputFactor = new NumberTextField(controller, minOutputFactor, 6, false, 0.1, 0.9, "0.00", "Minimum Unit Output Factor");
            ntOutputStep = new NumberTextField(controller, outputStep, 6, false, 0.05, 0.5, "0.00", "Unit Step Output Factor");
            ntUnitOutputOverRangeForTable = new NumberTextField(controller, unitOutputOverRangeForTable, 6, false, 1.0, 1.5, "0.00", "Unit output upper Margin for Control");
            ntWidthOverRange = new NumberTextField(controller, widthOverRange, 6, false, 1.01, 1.5, "0.00", "Width OverRange Factor (A)");
//            ntMinWidthFactor = new NumberTextField(controller, minWidthFactor, 6, false, 0.1, 0.9, "0.00", "Minimum Width Factor");
            ntWidthStep = new NumberTextField(controller, widthStep, 6, false, 0.05, 0.5, "0.00", "Width Step Factor");
            cbRespectFirstFiredZoneExitStripTemp = new JCheckBox("Respect FirstZone Charge Temperature", bRespectFirstFiredZoneExitStripTemp);
//            jp.addItemPair(ntMinOutputFactor);
            jp.addItemPair(ntOutputStep);
            jp.addItemPair(ntUnitOutputOverRangeForTable);
            jp.addBlank();
            jp.addItemPair(ntWidthOverRange);
//            jp.addItemPair(ntMinWidthFactor);
            jp.addItemPair(ntWidthStep);
            jp.addItem(cbRespectFirstFiredZoneExitStripTemp);
            jp.addItem(new JLabel("(While adjusting Charge Temp profile for minimum Exit Zone Temp)"));
            return jp;
        }

        JPanel buttonPanel() {
            MultiPairColPanel jp = new MultiPairColPanel(80, 80);
            jp.addItemPair(cancel, ok);
            return jp;
        }

        boolean isInError() {
            boolean inError = false;
            boolean stat;
            String msg = "ERROR : ";
            // new Values
//            double oMin = ntMinOutputFactor.getData();
//             double oStep =ntOutputStep.getData();
//             double wMin = ntMinWidthFactor.getData();
//             double wStep = ntWidthStep.getData();

//            if (oMin < underUP) {
//                msg += "\n   Check Minimum Output Factor must be higher than " + underUP;
//                inError = true;
//            }
            if (inError) {
                controller.enableNotify(false);
                JOptionPane.showMessageDialog(this, msg, "Performance Table Settings", JOptionPane.ERROR_MESSAGE);
                controller.enableNotify(true);
                controller.parent().toFront();
            }
            return inError;
        }

        void takeSetValues() {
//            minOutputFactor = ntMinOutputFactor.getData();
            outputStep = ntOutputStep.getData();
            unitOutputOverRangeForTable = ntUnitOutputOverRangeForTable.getData();
//            minWidthFactor = ntMinWidthFactor.getData();
            widthStep = ntWidthStep.getData();
            widthOverRange = ntWidthOverRange.getData();

            unitOutputOverRange = ntUnitOutputOverRange.getData();
            unitOutputUnderRange = ntUnitOutputUnderRange.getData();
            exitTempTolerance = ntExitTempTolerance.getData();
            bRespectFirstFiredZoneExitStripTemp = cbRespectFirstFiredZoneExitStripTemp.isSelected();
        }

        void closeThisWindow() {
            setVisible(false);
            dispose();
        }
    }

    String[] presetRowHeader = {"Emissivity of Walls", "Gas to Wall Heat transfer Multiplying Factors",
            "hConvection - Fired sections (kcal/m2.h.C)",
            "hConvection - Recuperative sections (kcal/m2.h.C)",
            "Gas Emissivity Correction factor",
            "Radiation Multiplier (for Wall-Radiation-only Calculation)"
    };


    class PreSetTunes {
        public double eO = 0.8;
        public double gMulti = 5;
        public double aConvFired = 20;
        public double aConvRecu = 20;
        public double eFact = 1;
        public double radiationMultiplier = 1;
        boolean editable;
        NumberTextField tfeO, tfgMulti, tfaConvFired, tfaConvRecu, tfeFact, tfRadiationMultiplier;

        PreSetTunes (double eO, double gMulti, double aConvFired, double aConvRecu, double eFact, double radiationMultiplier,  boolean editable) {
            this.eO = eO;
            this.gMulti = gMulti;
            this.aConvFired = aConvFired;
            this.aConvRecu = aConvRecu;
            this.eFact = eFact;
            this.radiationMultiplier = radiationMultiplier;
            this.editable = editable;
            if (editable) {
                int hNum = 0;
                tfeO = new NumberTextField(controller, eO, 6, false, 0.01, 1,
                        "#,##0.00", presetRowHeader[hNum++]);
                tfgMulti = new NumberTextField(controller, gMulti, 6, false, 1, 50,
                        "#,###.00", presetRowHeader[hNum++]);
                tfaConvFired = new NumberTextField(controller, aConvFired, 6, false, 0, 50,
                        "#,###.00", presetRowHeader[hNum++], true);
                tfaConvRecu = new NumberTextField(controller, aConvRecu, 6, false, 0, 50,
                        "#,###.00", presetRowHeader[hNum++], true);
                tfeFact = new NumberTextField(controller, eFact, 6, false, 1, 2,
                        "#,###.00", presetRowHeader[hNum++]);
                tfRadiationMultiplier = new NumberTextField(controller, radiationMultiplier, 6, false,
                        0.8, 1.5, "#,###.00", presetRowHeader[hNum++]);
            }
        }

        void setValues(double eO, double gMulti, double aConvFired, double aConvRecu, double eFact, double radiationMultiplier) {
            this.eO = eO;
            this.gMulti = gMulti;
            this.aConvFired = aConvFired;
            this.aConvRecu = aConvRecu;
            this.eFact = eFact;
            this.radiationMultiplier = radiationMultiplier;
            putInUI();
        }

        void putInUI() {
            if (editable) {
                tfeO.setData(eO);
                tfgMulti.setData(gMulti);
                tfaConvFired.setData(aConvFired);
                tfaConvRecu.setData(aConvRecu);
                tfeFact.setData(eFact);
                tfRadiationMultiplier.setData(radiationMultiplier);
            }
        }

        void takeFromUI() {
            if (editable) {
                eO = tfeO.getData();
                gMulti = tfgMulti.getData();
                aConvFired = tfaConvFired.getData();
                aConvRecu = tfaConvRecu.getData();
                eFact = tfeFact.getData();
                radiationMultiplier = tfRadiationMultiplier.getData();
            }
        }

        JPanel presetsPan() {
            JPanel jp = new JPanel(new GridBagLayout());
            jp.setBackground(new JPanel().getBackground());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            if (editable) {
                jp.add(tfeO, gbc);
                gbc.gridy++;
                jp.add(tfgMulti, gbc);
                gbc.gridy++;
                jp.add(tfaConvFired, gbc);
                gbc.gridy++;
                jp.add(tfaConvRecu, gbc);
                gbc.gridy++;
                jp.add(tfeFact, gbc);
                gbc.gridy++;
                jp.add(tfRadiationMultiplier, gbc);
            }
            else {
                jp.add(new NumberLabel(eO, 60, "#,##0.00"), gbc);
                gbc.gridy++;
                jp.add(new NumberLabel(gMulti, 60, "#,###.00"), gbc);
                gbc.gridy++;
                jp.add(new NumberLabel(aConvFired, 60, "#,###.00"), gbc);
                gbc.gridy++;
                jp.add(new NumberLabel(aConvRecu, 60, "#,###.00"), gbc);
                gbc.gridy++;
                jp.add(new NumberLabel(eFact, 60, "#,###.00"), gbc);
                gbc.gridy++;
                jp.add(new NumberLabel(radiationMultiplier, 60, "#,###.00"), gbc);
            }
            return jp;
        }
    }

}
