package directFiredHeating;

import mvUtils.display.MultiPairColPanel;
import mvUtils.display.NumberLabel;
import mvUtils.display.NumberTextField;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.display.FramedPanel;

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
    public boolean bOnTest = false;
    JCheckBox cBOnTest;
    public double epsilonO = 0.8;
    public double gasWallHTMultipler = 5;
    public double alphaConvFired = 20;
    public double alphaConvRecu = 20;
    public double emmFactor = 1;
    boolean bNoEmissivityCorrFactor = false;
    JCheckBox cBNoEmissivityCorrFactor;
    boolean noGasRadiationToCharge = false;
    JCheckBox cBNoGasRadiationToCharge;
    public double errorAllowed = 1;
    public boolean bTakeEndWalls = false;
    public boolean bTakeGasAbsorptionForInterRad = false;
    public double wallLoss = 0;
    public boolean bEvalBotFirst = false;
    public boolean bSectionalFlueExh = false;    // not used yet
    public boolean bSlotRadInCalcul = false;
    public boolean bSectionProgress = true;
    public boolean bSlotProgress = true;
    public double suggested1stCorrection = 0;
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
    private double tfmStep = 1.0;

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
        preSets = new LinkedHashMap<FurnaceFor, PreSetTunes>();
        preSets.put(FurnaceFor.BILLETS, new PreSetTunes(1, 5, 30, 30, 1.12, 1.0, false));
        preSets.put(FurnaceFor.STRIP, new PreSetTunes(0.8, 1, 15, 15, 1, 1.0, false));
        preSets.put(FurnaceFor.MANUAL, new PreSetTunes(1, 5, 30, 30, 1, 1.0, true));
        tferrorAllowed = new NumberTextField(controller, errorAllowed, 6, false, 0.001, 5, "#,###.00", "", true);
        tfwallLoss = new NumberTextField(controller, wallLoss, 6, false, 0, 1000, "#,###.00", "", true);
        tfsuggested1stCorrection = new NumberTextField(controller, suggested1stCorrection, 6, false, 0, 20, "#,###.00", "", true);

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
                    cBTakeEndWalls.setEnabled(true);
                }
                updateUI();
            }
        });
        cBTakeGasAbsorptionForInterRad = new JCheckBox();
        cBNoGasAbsorptionInWallBalance = new JCheckBox();
        cBbEvalBotFirst = new JCheckBox();
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

        cBProfileForTFM = new JComboBox<UnitFceArray.ProfileBasis>(UnitFceArray.ProfileBasis.values());
        cBProfileForTFM.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tfmBasis = (UnitFceArray.ProfileBasis)cBProfileForTFM.getSelectedItem();
            }
        });
        tfTFMStep = new NumberTextField(controller, tfmStep * 1000, 6,false, 200, 5000, "#,###", "Length Step for TFM Temperaure Profile");
        cBbAutoTempForLosses = new JCheckBox();
        cBbAutoTempForLosses.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                allowManualTempForLosses(!cBbAutoTempForLosses.isSelected());
            }
        });
        cBbSmoothenCurve = new JCheckBox();
        cBbConsiderChTempProfile = new JCheckBox();
        cBbConsiderChTempProfile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bConsiderChTempProfile = cBbConsiderChTempProfile.isSelected();
            }
        });
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
        cBNoEmissivityCorrFactor = new JCheckBox("(TESTING ONLY) Neglect Gas Emissivity Correction Factor");
        cBNoEmissivityCorrFactor.setEnabled(false);
        cBNoGasRadiationToCharge = new JCheckBox("(TESTING ONLY) No Gas Radiation To Charge");
        cBNoGasRadiationToCharge.setEnabled(false);
        cBOnTest = new JCheckBox("ON TEST.... ON TEST");
        final DFHeating c = controller;
        cBOnTest.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bOnTest = cBOnTest.isSelected();
                c.itIsOnTest(bOnTest);
                if (bOnTest) {
                    noGasRadiationToCharge = false;
                    cBNoGasRadiationToCharge.setSelected(noGasRadiationToCharge);
                    cBNoGasRadiationToCharge.setEnabled(true);
                    bNoEmissivityCorrFactor = false;
                    cBNoEmissivityCorrFactor.setSelected(bNoEmissivityCorrFactor);
                    cBNoEmissivityCorrFactor.setEnabled(true);
                }
                else {
                    noGasRadiationToCharge = false;
                    cBNoGasRadiationToCharge.setSelected(noGasRadiationToCharge);
                    cBNoGasRadiationToCharge.setEnabled(false);
                    bNoEmissivityCorrFactor = false;
                    cBNoEmissivityCorrFactor.setSelected(bNoEmissivityCorrFactor);
                    cBNoEmissivityCorrFactor.setEnabled(false);
                }
            }
        });
        updateUI();
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

    double getTFMStep(){
        setTFMStep();
        return tfmStep;
    }

    private void setTFMStep() {
        tfmStep = tfTFMStep.getData() / 1000;
    }

    void takeValuesFromUI() {
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

        bTakeEndWalls = (cBTakeEndWalls.isSelected());
        bEvalBotFirst = (cBbEvalBotFirst.isSelected());
        bTakeGasAbsorptionForInterRad = (cBTakeGasAbsorptionForInterRad.isSelected());
        bNoGasAbsorptionInWallBalance = (cBNoGasAbsorptionInWallBalance.isSelected());
        bSlotRadInCalcul = cBSlotRadInCalcul.isSelected();
        bSectionProgress = (cBbSectionProgress.isSelected());
        bSlotProgress = (cBbSlotProgress.isSelected());
        bMindChHeight = (cBbMindChHeight.isSelected());
        bAllowSecFuel = (cBAllowSecFuel.isSelected());
        bAllowRegenAirTemp = (cBAllowRegenAirTemp.isSelected());
        bAutoTempForLosses = cBbAutoTempForLosses.isSelected();
        bSmoothenCurve = cBbSmoothenCurve.isSelected();
        bConsiderChTempProfile = cBbConsiderChTempProfile.isSelected();
        bAdjustChTempProfile= cBbAdjustChTempProfile.isSelected();
//        overUP = tfOverUP.getData();
//        underUP = tfUnderUP.getData();
        bOnProductionLine = cBbOnProductionLine.isSelected();
        bNoEmissivityCorrFactor = cBNoEmissivityCorrFactor.isSelected();
        noGasRadiationToCharge = cBNoGasRadiationToCharge.isSelected();
        bOnTest = cBOnTest.isSelected();
        bBaseOnZonalTemperature = (cBbaseOnZonalTemperature.isSelected());
        setTFMStep();

    }

    void updateUI() {
        tferrorAllowed.setData(errorAllowed);
        tfwallLoss.setData(wallLoss);
        tfsuggested1stCorrection.setData(suggested1stCorrection);
        cBTakeEndWalls.setSelected(bTakeEndWalls);
        cBTakeGasAbsorptionForInterRad.setSelected(bTakeGasAbsorptionForInterRad);
        cBNoGasAbsorptionInWallBalance.setSelected(bNoGasAbsorptionInWallBalance);
        cBbEvalBotFirst.setSelected(bEvalBotFirst);
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
        tfTFMStep.setData(tfmStep * 1000);
        cBbAutoTempForLosses.setSelected(bAutoTempForLosses);
        cBbSmoothenCurve.setSelected(bSmoothenCurve);
        cBbConsiderChTempProfile.setSelected(bConsiderChTempProfile);
        cBbAdjustChTempProfile.setSelected(bAdjustChTempProfile);
//        tfOverUP.setData(overUP);
//        tfUnderUP.setData(underUP);
        cBbOnProductionLine.setSelected(bOnProductionLine);
        cBNoEmissivityCorrFactor.setSelected(bNoEmissivityCorrFactor);
        cBNoGasRadiationToCharge.setSelected(noGasRadiationToCharge);
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

    public String dataInXML() {
        updateUI();
        String xmlStr = "";
        PreSetTunes pre = preSets.get(FurnaceFor.MANUAL);
        xmlStr += XMLmv.putTag("epsilonO", pre.eO);
        xmlStr += XMLmv.putTag("gasWallHTMultipler", pre.gMulti);
        xmlStr += XMLmv.putTag("alphaConv", pre.aConvFired);
        xmlStr += XMLmv.putTag("alphaConvRecu", pre.aConvRecu);
        xmlStr += XMLmv.putTag("emmFactor", pre.eFact);
        xmlStr += XMLmv.putTag("radiationMultiplier", pre.radiationMultiplier);

        xmlStr += XMLmv.putTag("errorAllowed", errorAllowed);
        xmlStr += XMLmv.putTag("suggested1stCorrection", suggested1stCorrection);
        xmlStr += XMLmv.putTag("bTakeEndWalls", (bTakeEndWalls) ? 1 : 0);
        xmlStr += XMLmv.putTag("bTakeGasAbsorption", (bTakeGasAbsorptionForInterRad) ? 1 : 0);
        xmlStr += XMLmv.putTag("bEvalBotFirst", (bEvalBotFirst) ? 1: 0);
        xmlStr += XMLmv.putTag("bSectionalFlueExh", (bSectionalFlueExh) ? 1: 0);
        xmlStr += XMLmv.putTag("bSlotRadInCalcul", (bSlotRadInCalcul) ? 1: 0);
        xmlStr += XMLmv.putTag("bSectionProgress", (bSectionProgress) ? 1: 0);
        xmlStr += XMLmv.putTag("bSlotProgress", (bSlotProgress) ? 1: 0);
        xmlStr += XMLmv.putTag("bMindChHeight", (bMindChHeight) ? 1: 0);
        xmlStr += XMLmv.putTag("bAllowSecFuel", (bAllowSecFuel) ? 1: 0);
        xmlStr += XMLmv.putTag("bAllowRegenAirTemp", (bAllowRegenAirTemp) ? 1: 0);
        // Performance base
        xmlStr += XMLmv.putTag("bConsiderChTempProfile", (bConsiderChTempProfile) ? 1: 0);
//        xmlStr += XMLmv.putTag("overUP", overUP);
//        xmlStr += XMLmv.putTag("underUP", underUP);
        xmlStr += XMLmv.putTag("bOnProductionLine", (bOnProductionLine) ? 1: 0);
//        xmlStr += XMLmv.putTag("takeEmissivityCorrFactor", (takeEmissivityCorrFactor) ? 1: 0);
        return xmlStr;
    }

    public boolean takeDataFromXML(String xmlStr) {
        ValAndPos vp;
        boolean bRetVal = true;
        try {
            PreSetTunes pre = preSets.get(FurnaceFor.MANUAL);
            vp = XMLmv.getTag(xmlStr, "epsilonO", 0);
            pre.eO = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "gasWallHTMultipler", 0);
            pre.gMulti = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "alphaConv", 0);
            pre.aConvFired = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "alphaConvRecu", 0);
            if (vp.val.length() > 0)
                pre.aConvRecu = Double.valueOf(vp.val);
            else
                pre.aConvRecu = pre.aConvFired;
            vp = XMLmv.getTag(xmlStr, "emmFactor", 0);
            pre.eFact = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "radiationMultiplier", 0);
            if (vp.val.length() > 0)
                pre.radiationMultiplier = Double.valueOf(vp.val);
            else
                pre.radiationMultiplier = 1.0;
            pre.putInUI();


            vp = XMLmv.getTag(xmlStr, "errorAllowed", 0);
            errorAllowed = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "suggested1stCorrection", 0);
            suggested1stCorrection = Double.valueOf(vp.val);


            vp = XMLmv.getTag(xmlStr, "bTakeEndWalls", 0);
            bTakeEndWalls = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bTakeGasAbsorption", 0);
            bTakeGasAbsorptionForInterRad = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bEvalBotFirst", 0);
            bEvalBotFirst = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bSectionalFlueExh", 0);
            bSectionalFlueExh = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bSlotRadInCalcul", 0);
            bSlotRadInCalcul = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bSectionProgress", 0);
            bSectionProgress = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bSlotProgress", 0);
            bSlotProgress = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bMindChHeight", 0);
            bMindChHeight = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bAllowSecFuel", 0);
            bAllowSecFuel = (vp.val.equals("1"));
//            vp = XMLmv.getTag(xmlStr, "bAllowRegenAirTemp", 0);
//            bAllowRegenAirTemp = (vp.val.equals("1"));

            // performance base
            vp = XMLmv.getTag(xmlStr, "bConsiderChTempProfile", 0);
            bConsiderChTempProfile = (vp.val.equals("1"));
//            vp = XMLmv.getTag(xmlStr, "overUP", 0);
//            if (vp.val.length() > 0)
//                overUP = Double.valueOf(vp.val);
//            vp = XMLmv.getTag(xmlStr, "underUP", 0);
//            if (vp.val.length() > 0)
//                underUP = Double.valueOf(vp.val);
//            vp = XMLmv.getTag(xmlStr, "bOnProductionLine", 0);
//            bOnProductionLine = (vp.val.equals("1"));
//            vp = XMLmv.getTag(xmlStr, "takeEmissivityCorrFactor", 0);
//            takeEmissivityCorrFactor = (vp.val.equals("1"));
        } catch (NumberFormatException e) {
            bRetVal = false;
        }
        bBaseOnZonalTemperature = false;
        updateUI();
        return bRetVal;
    }

    GridBagConstraints mainGbc = new GridBagConstraints();
    JPanel mainTuningPanel = new FramedPanel(new GridBagLayout());

    JPanel getTuningPanel() {
//        JPanel mainTuningPanel = new FramedPanel(new GridBagLayout());
        mainTuningPanel.setBackground(new JPanel().getBackground());
//        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        mainGbc.gridwidth = 2;
        mainTuningPanel.add(getProcSetPanel(), mainGbc);
        mainGbc.gridy++;
        mainGbc.gridwidth = 1;
        mainTuningPanel.add(settings1Pan(), mainGbc);
        mainGbc.gridx++;
        mainTuningPanel.add(setting2Pan(), mainGbc);
        mainGbc.gridx = 0;
        mainGbc.gridy++;
        mainTuningPanel.add(performancePan(), mainGbc);

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
        jp.add(cBOnTest, gbc);
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
        gbc.gridx = 0;
        gbc.gridy++;
        jp.add(cBNoEmissivityCorrFactor, gbc);
        gbc.gridy++;
        jp.add(cBNoGasRadiationToCharge, gbc);
        return jp;
    }

    NumberTextField tferrorAllowed, tfwallLoss, tfsuggested1stCorrection;
    NumberTextField tfTFMStep;
    // for evaluation from reference performance
    public boolean bConsiderChTempProfile = true;
    JCheckBox cBbConsiderChTempProfile;
//    public double overUP = 1.05, underUP = 0.2; // allowed upper and lower factors with respect to
    // output per unit Strip width (charge length)
    public boolean bAdjustChTempProfile = true;    // while respecting last zone minimum fce temp
    JCheckBox cBbAdjustChTempProfile;
//    NumberTextField tfOverUP, tfUnderUP;
    public boolean bOnProductionLine = false;
    JCheckBox cBbOnProductionLine;
    // parameters for Field reference Data
    public double unitOutputOverRange = 1.05;
    public double unitOutputUnderRange = 0.9;
    public double exitTempTolerance = 5;
    NumberTextField ntUnitOutputOverRange;
    NumberTextField ntUnitOutputUnderRange;
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

    JPanel settings1Pan() {
        MultiPairColPanel jp = new MultiPairColPanel(200, 60);
        jp.addItemPair("Error Allowed (degC)", tferrorAllowed);
        jp.addItemPair("Wall loss (for internal use)", tfwallLoss);
        jp.addItemPair("Suggested First Correction (degC)", tfsuggested1stCorrection);
        jp.addBlank();
        jp.addItemPair("Temperature Profile for TFM", cBProfileForTFM);
        jp.addItemPair("Length step for TFM Temperature Profile (mm)", tfTFMStep);
        return jp;
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

    JPanel setting2Pan() {
        MultiPairColPanel jp = new MultiPairColPanel(200, 60);
        jp.addItemPair("Smoothen Temperature trends", cBbSmoothenCurve);
        jp.addItemPair("Evaluate Bottom Section First", cBbEvalBotFirst);
        jp.addItemPair("Show Section Progress", cBbSectionProgress);
        jp.addItemPair("Show Stepwise Progress", cBbSlotProgress);
        jp.addItemPair("Mind Charge Height for Radiation", cBbMindChHeight);
        jp.addItemPair("Allow Section-wise Fuel", cBAllowSecFuel);
        jp.addItemPair("Allow REGEN Air Temperature", cBAllowRegenAirTemp);
        jp.addItemPair("Show Flue Composition", cBShowFlueCompo);
//        jp.addItemPair("Auto Section Temp for Losses", cBbAutoTempForLosses);
        jp.addBlank();
        jp.addItemPair("Take Gas Absorption in Internal Rad", cBTakeGasAbsorptionForInterRad);
        jp.addItemPair("No Gas Absorption in Wall Balance", cBNoGasAbsorptionInWallBalance);
//        jp.addBlank();
//        jp.addItemPair("Consider Charge Temp Profile", cBbConsideChTempProfile);
//        jp.addItemPair(tfOverUP);
//        jp.addItemPair(tfUnderUP);
        return jp;
    }

    JPanel performancePan() {
        MultiPairColPanel jp = new MultiPairColPanel("Using Reference Performance",200, 60);
        jp.addItemPair("Consider Charge Temp Profile", cBbConsiderChTempProfile);
        jp.addItemPair("Adjust Charge Temp Profile", cBbAdjustChTempProfile);
//        jp.addItemPair(tfOverUP);
//        jp.addItemPair(tfUnderUP);
        jp.addItemPair("On Production Line", cBbOnProductionLine);
        return jp;
    }

    JCheckBox cBbaseOnZonalTemperature;


    public MultiPairColPanel userTunePan() {
        MultiPairColPanel jp = new MultiPairColPanel("Calculation Basis", 300,6);
        jp.addItemPair("Evaluate Internal Radiation", cBSlotRadInCalcul);
        jp.addItemPair("Evaluate EndWall Radiation", cBTakeEndWalls);
        jp.addBlank();
        jp.addItem(new JLabel("CAUTION: Choice below is on TRIAL"));
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
        JLabel h1 = new JLabel("Emissivity of Walls");
        h1.setPreferredSize(headSize);
        jp.add(h1, gbc);
        gbc.gridy++;
        JLabel h2 = new JLabel("Gas to Wall Heat transfer Multiplying Factors");
        h2.setPreferredSize(headSize);
        jp.add(h2, gbc);
        gbc.gridy++;
        JLabel h3 = new JLabel("hConvection - Fired sections (kcal/m2.h.C)");
        h3.setPreferredSize(headSize);
        jp.add(h3, gbc);
        gbc.gridy++;
        JLabel h4 = new JLabel("hConvection - Recuperative sections (kcal/m2.h.C)");
        h4.setPreferredSize(headSize);
        jp.add(h4, gbc);
        gbc.gridy++;
        JLabel h5 = new JLabel("Gas Emissivity Correction factor");
        h5.setPreferredSize(headSize);
        jp.add(h5, gbc);
        gbc.gridy++;
        JLabel h6 = new JLabel("Radiation Multiplier (for Wall-Radiation-only Calculation)");
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
//
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
            ntExitTempTolerance = new NumberTextField(controller, exitTempTolerance, 6, false, 1, 20, "##", "Margin (+-) on Exit Temperature in degC (D)");

            jp.addItemPair(ntUnitOutputOverRange);
            jp.addItemPair(ntUnitOutputUnderRange);
            jp.addBlank();
            jp.addItemPair(ntExitTempTolerance);
            return jp;
        }

        JPanel perfTableSettings() {
            MultiPairColPanel jp = new MultiPairColPanel("Settings for Performance Table", 200, 60);
//            ntMinOutputFactor = new NumberTextField(controller, minOutputFactor, 6, false, 0.1, 0.9, "0.00", "Minimum Unit Output Factor");
            ntOutputStep = new NumberTextField(controller, outputStep, 6, false, 0.05, 0.5, "0.00", "Unit Step Output Factor");

            ntWidthOverRange = new NumberTextField(controller, widthOverRange, 6, false, 1.01, 1.5, "0.00", "Width OverRange Factor (A)");
//            ntMinWidthFactor = new NumberTextField(controller, minWidthFactor, 6, false, 0.1, 0.9, "0.00", "Minimum Width Factor");
            ntWidthStep = new NumberTextField(controller, widthStep, 6, false, 0.05, 0.5, "0.00", "Width Step Factor");

//            jp.addItemPair(ntMinOutputFactor);
            jp.addItemPair(ntOutputStep);
            jp.addBlank();
            jp.addItemPair(ntWidthOverRange);
//            jp.addItemPair(ntMinWidthFactor);
            jp.addItemPair(ntWidthStep);
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
//            minWidthFactor = ntMinWidthFactor.getData();
            widthStep = ntWidthStep.getData();
            widthOverRange = ntWidthOverRange.getData();

            unitOutputOverRange = ntUnitOutputOverRange.getData();
            unitOutputUnderRange = ntUnitOutputUnderRange.getData();
            exitTempTolerance = ntExitTempTolerance.getData();
        }

        void closeThisWindow() {
            setVisible(false);
            dispose();
        }
    }


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
                tfeO = new NumberTextField(controller, eO, 6, false, 0.01, 1, "#,##0.00", "");
                tfgMulti = new NumberTextField(controller, gMulti, 6, false, 1, 50, "#,###.00", "");
                tfaConvFired = new NumberTextField(controller, aConvFired, 6, false, 0, 50, "#,###.00", "", true);
                tfaConvRecu = new NumberTextField(controller, aConvRecu, 6, false, 0, 50, "#,###.00", "", true);
                tfeFact = new NumberTextField(controller, eFact, 6, false, 1, 2, "#,###.00", "");
                tfRadiationMultiplier = new NumberTextField(controller, radiationMultiplier, 6, false, 0.8, 1.5, "#,###.00", "");
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
