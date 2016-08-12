package directFiredHeating;

import appReporting.Reporter;
import basic.*;
import display.*;
import jsp.JSPComboBox;
import mvUtils.display.*;
import mvUtils.math.DoublePoint;
import mvUtils.math.MultiColData;
import mvUtils.math.SPECIAL;
import mvUtils.math.XYArray;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.display.FramedPanel;
import org.apache.poi.ss.usermodel.Sheet;
import performance.stripFce.OneZone;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 5/9/12
 * Time: 12:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class FceSection {
    static public int MAXSUBSECTIONS = 5;
    static FramedPanel rowHead;
    static Dimension colHeadSize = new JTextField("Flow of flue passing (m3N/h)", 20).getPreferredSize();
    static Insets headerIns = new Insets(1, 1, 1, 1);
    static Dimension dataColSize = new JTextField("0,000,000.00", 6).getPreferredSize();

    static Vector<XLcellData> cHGeneral;
    static Vector<XLcellData> cHFuel, cHAir, cHPassFlueIN, cHTotIn;
    static Vector<XLcellData> cHCharge, cHLosses, cHPassFlueOut, cHRegenFlue, cHFceFlue, cHTotOut;

    public static FramedPanel getRowHeader(boolean bMixedFuel) {
        rowHead = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbcH = new GridBagConstraints();
        SizedLabel sL;
        Insets ins = new Insets(0, 0, 0, 0);
        gbcH.gridx = 0;
        gbcH.gridy = 0;
        gbcH.insets = ins;
        gbcH.weightx = 0.1;
        FramedPanel grpPan = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridx = 0;
        cHGeneral = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
//        sL = sizedLabel("Burner Type", sized);
//        cHFuel.add(sL);
//        grpPan.add(sL, gbcL);
//        gbcL.gridy++;
        sL = sizedLabel("TC Location from Entry (mm)", colHeadSize);
        cHGeneral.add(sL);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("Temperature at TC (degC)", colHeadSize);
        cHGeneral.add(sL);
        grpPan.add(sL, gbcL);
        rowHead.add(grpPan, gbcH);
        gbcH.gridy++;

        cHFuel = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = sizedLabel("Burner Type", colHeadSize);
        cHFuel.add(sL);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("Fuel Flow (#/h)", colHeadSize, true);
        cHFuel.add(sL);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("Fuel Temperature (degC)", colHeadSize);
        cHFuel.add(sL);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("Total Fuel Sensible Heat (kcal/h)", colHeadSize, true);
        cHFuel.add(sL);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("Total Heat of Combustion (kCal/h)", colHeadSize, true);
        cHFuel.add(sL);
        grpPan.add(sL, gbcL);
        gbcH.gridy++;

        rowHead.add(grpPan, gbcH);
        cHAir = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = sizedLabel("Combustion Air Flow (m3N/h)", colHeadSize);
        cHAir.add(sL);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("Air Temperature (degC)", colHeadSize);
        cHAir.add(sL);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("Heat from Air (kcal/h)", colHeadSize, true);
        cHAir.add(sL);
        grpPan.add(sL, gbcL);
        gbcH.gridy++;
        rowHead.add(grpPan, gbcH);

        cHPassFlueIN = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = sizedLabel("Flow of flue passing (m3N/h)", colHeadSize);
        cHPassFlueIN.add(sL);
        grpPan.add(sL, gbcH);
        gbcH.gridy++;
        sL = sizedLabel("Flue IN Temperature (degC)", colHeadSize);
        cHPassFlueIN.add(sL);
        grpPan.add(sL, gbcH);
        gbcH.gridy++;
        sL = sizedLabel("Heat from Flue (kcal/h)", colHeadSize, true);
        cHPassFlueIN.add(sL);
        grpPan.add(sL, gbcH);
        gbcH.gridy++;
        rowHead.add(grpPan, gbcH);

        cHTotIn = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = sizedLabel("Total Heat IN (kcal/h)", colHeadSize, true);
        cHTotIn.add(sL);
        grpPan.add(sL, gbcL);
        gbcH.gridy++;
        rowHead.add(grpPan, gbcH);

        gbcH.gridy++;
        rowHead.add(sizedLabel("", colHeadSize), gbcH);

        cHCharge = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = sizedLabel("Charge Production (kg/h)", colHeadSize);
        cHCharge.add(sL);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        if (furnace.bTopBot)  {
            sL = sizedLabel("Production Fraction (%)", colHeadSize);
            cHCharge.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
        }
        sL = sizedLabel("Charge Temperature IN (degC)", colHeadSize);
        cHCharge.add(sL);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("Charge Temperature OUT (degC)", colHeadSize);
        cHCharge.add(sL);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("Heat to Charge (kCal/h)", colHeadSize, true);
        cHCharge.add(sL);
        grpPan.add(sL, gbcL);
        gbcH.gridy++;
        rowHead.add(grpPan, gbcH);

        cHLosses = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        if (tuning.bSlotRadInCalcul) {
            sL = sizedLabel("Losses (kcal/h)", colHeadSize);
            cHLosses.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = sizedLabel("Inter radiation OUT (kcal/h)", colHeadSize);
            cHLosses.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
        }
        sL = sizedLabel("Total Losses (kCal/h)", colHeadSize, true);
        cHLosses.add(sL);
        grpPan.add(sL, gbcL);
        gbcH.gridy++;
        rowHead.add(grpPan, gbcH);

        cHPassFlueOut = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = sizedLabel("Flue Temperature OUT (degC)", colHeadSize);
        cHPassFlueOut.add(sL);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("Heat to Passing Flue (kCal/h)", colHeadSize, true);
        cHPassFlueOut.add(sL);
        grpPan.add(sL, gbcL);
        gbcH.gridy++;
        rowHead.add(grpPan, gbcH);

        cHRegenFlue = new Vector<XLcellData>();
        cHFceFlue = new Vector<XLcellData>();
        if (furnace.anyRegen() > 0) {
            grpPan = new FramedPanel(new GridBagLayout());
            gbcL.gridy = 0;
            sL = sizedLabel("Flue to Regen (m3N/h)", colHeadSize);
            cHRegenFlue.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = sizedLabel("Heat to Regen flue (kCal/h)", colHeadSize, true);
            cHRegenFlue.add(sL);
            grpPan.add(sL, gbcL);
            gbcH.gridy++;
            rowHead.add(grpPan, gbcH);
            grpPan = new FramedPanel(new GridBagLayout());
            gbcL.gridy = 0;
            sL = sizedLabel("Flue to Fce (m3N/h)", colHeadSize);
            cHFceFlue.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = sizedLabel("Heat to Fce flue (kCal/h)", colHeadSize, true);
            cHFceFlue.add(sL);
            grpPan.add(sL, gbcL);
            gbcH.gridy++;
            rowHead.add(grpPan, gbcH);
        }
        else {
            grpPan = new FramedPanel(new GridBagLayout());
            gbcL.gridy = 0;
            sL = sizedLabel("Flue of Combustion (m3N/h)", colHeadSize);
            cHFceFlue.add(sL);
            grpPan.add(sL, gbcL);
            gbcL.gridy++;
            sL = sizedLabel("Heat to Combustion flue (kCal/h)", colHeadSize, true);
            cHFceFlue.add(sL);
            grpPan.add(sL, gbcL);
            gbcH.gridy++;
            rowHead.add(grpPan, gbcH);
        }
        cHTotOut = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = sizedLabel("Total Heat OUT (kcal/h)", colHeadSize, true);
        cHTotOut.add(sL);
        grpPan.add(sL, gbcL);
        gbcH.gridy++;
        rowHead.add(grpPan, gbcH);
        if (tuning.bShowFlueCompo) {
            for (int n=0; n <2; n++) {    // just to go around twice
                grpPan = new FramedPanel(new GridBagLayout());
                gbcL.gridy = 0;
                sL = sizedLabel(((n == 0) ?"Local Flue Composition": "Mixed Flue Composition"), colHeadSize, true);
                cHTotOut.add(sL);
                grpPan.add(sL, gbcL);
                gbcL.gridy++;
                sL = sizedLabel("CO2 (%)", colHeadSize, true);
                cHTotOut.add(sL);
                grpPan.add(sL, gbcL);
                gbcL.gridy++;
                sL = sizedLabel("H2O (%)", colHeadSize, true);
                cHTotOut.add(sL);
                grpPan.add(sL, gbcL);
                gbcL.gridy++;
                sL = sizedLabel("SO2 (%)", colHeadSize, true);
                cHTotOut.add(sL);
                grpPan.add(sL, gbcL);
                gbcL.gridy++;
                sL = sizedLabel("O2 (%)", colHeadSize, true);
                cHTotOut.add(sL);
                grpPan.add(sL, gbcL);
                gbcL.gridy++;
                sL = sizedLabel("N2 (%)", colHeadSize, true);
                cHTotOut.add(sL);
                grpPan.add(sL, gbcL);
                gbcH.gridy++;
                rowHead.add(grpPan, gbcH);
            }
        }
        return rowHead;
    }

    public static int xlSecSummaryHead(Sheet sheet, ExcelStyles style, int topRow, int leftCol) {
        sheet.setColumnWidth(leftCol, 9000);
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, cHGeneral) + 1;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, cHFuel) + 1;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, cHAir) + 1;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, cHPassFlueIN) + 1;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, cHTotIn) + 2;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, cHCharge) + 1;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, cHLosses) + 1;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, cHPassFlueOut) + 1;
        if (cHRegenFlue.size() > 0)
            topRow = style.xlAddXLCellData(sheet, topRow, leftCol, cHRegenFlue) + 1;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, cHFceFlue) + 1;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, cHTotOut);
        return topRow;
    }

    static SizedLabel sizedLabel(String name, Dimension d, boolean bold) {
        return new SizedLabel(name, d, bold);
    }

    static SizedLabel sizedLabel(String name, Dimension d) {
        return sizedLabel(name, d, false);
    }

    Vector<FceSubSection> subSections;
    public boolean bRecuType;
    public boolean botSection;
    boolean bGasTempSpecified;
    double presetGasTemp;
    public double seclength;
    public double secStartPos, secEndPos;
//    double flueTemp;
    public double secFuelFlow = 0, secFlueFlow, fluePassThrough;
    FlueCompoAndQty passFlueCompAndQty, totFlueCompAndQty;
    double secAir;
    public double chargeHeat, airSensible, combustionHeat, heatToSecFlue, totalFlueExitHeat;
    double chargeHeatFraction;
    double passFlueTin, tempFlueOut;
    public double heatPassingFlueIn, heatPassingFlueOut, heatFromPassingFlue;
    public double lastRate;
    double secTime;
    double pLosses, pInterRadLoss;
    double regenPHTemp;
    double flueInSection;
    LossListWithVal lossValList;
    double burnerFlueExhFract = 0;
    double burnerFlueHeat;
    ProductionData production;
    NumberTextField tfFlueExhPercent, tfRegenPHtemp, tfExcessAir, tfFuelTemp;
    NumberTextField tfTCLocation;
    NumberLabel tlTCLocationFromEntry;
    NumberTextField tfZoneTemperature;

//    JComboBox<Fuel> cbFuels = new JComboBox<Fuel>(DFHeating.fuelList);
    JSPComboBox<Fuel> cbFuels = new JSPComboBox<Fuel>(DFHeating.jspConnection, DFHeating.fuelList);
    JComboBox<String> cbSecType = new JComboBox<String>(new String[]{"Recuperative", "With Burners"});
    JComboBox<String> cbFuelChoice = new JComboBox<String>(new String[]{"Common Fuel", "Individual Fuel"});
    JComboBox<String> cbBurnerType = new JComboBox<String>(new String[]{"Normal", "Regenerative"});
    JCheckBox chkAPHCommonRecu = new JCheckBox();
    String nlSpace = ErrorStatAndMsg.nlSpace;
    int firstSlot, lastSlot;
    Fuel fuelInSection;
    FuelFiring fuelFiring;
    FlueComposition flueComposition;
    double excessAir;
    boolean bRegenBurner;
    boolean bAPHCommonRecu = true;
    boolean bIndividualFuel = false;
    public static DFHFurnace furnace;
    static DFHTuningParams tuning;
    public int secNum;
    double secLoss;

    FramedPanel sectionsPanel;
    GridBagConstraints gbcSecs;
    boolean bPanelReady = false;
    DFHeating controller;
    boolean enabled = false;
    UnitFurnace[] unitFurnaces;
    Vector<UnitFurnace> vUnitFurnaces;
    double cumLossDischEnd;
    boolean bAllowSecFuel = false;
    FuelsAndUsage secFuelUsage;
    FuelsAndUsage secFuelUsageBreakup;
    boolean bAddedSoak = false;
    double tcLocationFromZoneStart; // location of thermocouple fro the section start in m
    double tempAtTCLocation = 0;

    public FceSection(DFHeating controller, DFHFurnace furnace,  boolean botSection, boolean bRecuType) {
        this.controller = controller;
        FceSection.furnace = furnace;
        this.botSection = botSection;
        this.bRecuType = bRecuType;
        seclength = 0;
        subSections = new Vector<FceSubSection>();
        FceSubSection sub;
        tfFlueExhPercent = new NumberTextField(controller, burnerFlueExhFract * 100, 5, false, 0, 100, "###.##", "% flue Exh through Burner");
        tfExcessAir = new NumberTextField(controller, excessAir * 100, 5, false, 0, 100, "###.##", "Excess Air (%) ");
        tfRegenPHtemp = new NumberTextField(controller, regenPHTemp, 5, false, 0, 1500, "#,###", "Air Preheat (C) ");
        tfFuelTemp = new NumberTextField(controller, fuelTemp, 5, false, 0, 1500, "#,###", "Fuel Temperature (C) ");
        tfTCLocation = new NumberTextField(controller, tcLocationFromZoneStart * 1000, 5, false, 0, 50000, "#,###", "T/c Location from Zone entry (mm) ");
        tlTCLocationFromEntry = new NumberLabel(controller, tcLocationFromZoneStart * 1000, 60, "#,###");
        tfTCLocation.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tlTCLocationFromEntry.setData(tfTCLocation.getData() + secStartPos * 1000);
            }
        });
        tfZoneTemperature = new NumberTextField(controller, tempAtTCLocation, 5, false, 0, 1500, "#,###", "Zone Temperature at TC (C) ");
        tfZoneTemperature.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

            }
        });
        ActionListener li = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                warnRegenPhTemp(e);
                enableComponents();
            }
        };
        cbFuelChoice.addActionListener(li);
        cbBurnerType.addActionListener(li);
        chkAPHCommonRecu.addActionListener(li);
        cbSecType.addActionListener(li);
        cbFuels.addActionListener(li);
        setValuesToUI();
        for (int s = 0; s < MAXSUBSECTIONS; s++) {
            sub = new FceSubSection(controller, this, (s + 1));
//            if (s == 0)
//                sub.enableSubsection(true);                              `
//            else
                sub.enableSubsection(false);
            subSections.add(sub);
        }
        preparePanel();
        passFlueCompAndQty = new FlueCompoAndQty(null, 0, 0);
        totFlueCompAndQty = new FlueCompoAndQty(null, 0, 0);
    }

    public JPanel jpTCLocationAndTemperature() {
        MultiPairColPanel jp = new MultiPairColPanel(sectionName() + " (" + ((bRecuType) ? "Recuperative" : "With Burners") + ")");
        setTCLocationLimits();
        jp.addItemPair(tfTCLocation);
        tlTCLocationFromEntry.setData(tfTCLocation.getData() + secStartPos * 1000);
        jp.addItemPair("TC location from Furnace Entry", tlTCLocationFromEntry);
        jp.addBlank();
        jp.addItemPair(tfZoneTemperature);
        return jp;
    }

    public boolean noteZonalTemperature() {
        if (tfTCLocation.isInError() || tfZoneTemperature.isInError())
            return false;
        else {
            tempAtTCLocation = tfZoneTemperature.getData();
            tcLocationFromZoneStart = tfTCLocation.getData() / 1000;
            return true;
        }
    }

    void markZonalTemperature(MultiColData results, int trace) {
        tempAtTCLocation = results.getYat(trace, tcLoc());
        tfZoneTemperature.setData(tempAtTCLocation);
    }

    void setPresetGasTemp (double gTemp) {
        presetGasTemp = gTemp;
        bGasTempSpecified = true;
        setLastSlotGasTemp(gTemp);

    }

    void resetSection() {
        nActiveSubs = -1;
        bGasTempSpecified = false;
    }

    void warnRegenPhTemp(ActionEvent e) {
        if ((e.getSource()  == cbBurnerType) && (cbBurnerType.getSelectedIndex() == 1)) {
            if (!bRegenBurner && furnace.tuningParams.bAllowRegenAirTemp)
                showMessage("Air Preheat Temperature has to be Entered here" +
                            ((bAPHCommonRecu) ? "\nand un-select 'Air Preheat with Common Recu' " : ""));
//            chkAPHCommonRecu.setSelected(false);
        }
    }

    public FceSection(DFHeating controller, DFHFurnace furnace,  boolean botSection, int secNum, boolean bRecuType) {
        this(controller, furnace, botSection, bRecuType);
        setSectionID(secNum);
        takeValuesFromUI();
     }

    public FceSection(DFHeating controller, DFHFurnace furnace, boolean bAddedSoak) {
        this(controller, furnace, false, false);
        this.bAddedSoak = bAddedSoak;
        setSecType(false);
//        cbSecType.setSelectedIndex(1);
//        setSectionID(-1);
        takeValuesFromUI();
     }

    public void setDefaults() {
        bRecuType = true;
        bIndividualFuel = false;
        bRegenBurner = false;
        enableComponents();
        for (int s = 0; s < subSections.size(); s++)
            subSections.get(s).setDefaults();
    }

    public void setProduction(ProductionData production) {
        this.production = production;
        FceSubSection sub;
        for (int s = 0; s < subSections.size(); s++) {
            sub = subSections.get(s);
            sub.setProduction(production);
        }
    }

    public double getFuelHeat() {
        return combustionHeat + fuelSensible + airSensible;
    }

    public void resetResults() {
        secFlueFlow = 0;
        secFuelFlow = 0;
        combustionHeat = 0;
        airSensible = 0;
        heatToSecFlue = 0;
        fceFlueHeat = 0;
        flueInSection = 0;
//        heatToSecFlue = 0;
        regenFlueHeat = 0;
    }
    boolean enaEdit = true;

    public void enableDataEntry(boolean ena) {
        enaEdit = ena;
        cbSecType.setEnabled(ena);
        for (int s = 0; s < subSections.size(); s++)
            subSections.get(s).enableDataEntry(ena);
    }

    public boolean isWithRegen() {
        return (isActive() && !bRecuType && bRegenBurner); // was burnerFlueExhFract > 0);
    }
    public void setSecType(boolean bRecuType) {
        this.bRecuType = bRecuType;
        setComboBoxes();
        setSummaryText();
    }

    public void enableSection(boolean ena) {
        enabled = ena;
        subSections.get(0).enableSubsection(enabled);
        if (!enabled) {
            for (int s = 1; s < subSections.size(); s++)
                    subSections.get(s).enableSubsection(enabled);
        }
        enableSecIfOK();
    }

    public boolean isEnabled() {
        return enabled;
    }
    boolean isWithCommonFuel() {
        return ((!bRecuType) && cbFuelChoice.getSelectedIndex() == 0);
    }

    void setAllowSecFuel(boolean allow) {
        bAllowSecFuel = allow;
        if (!bAllowSecFuel) {
            bIndividualFuel = false;
            cbFuelChoice.setSelectedItem(0);
            cbFuels.setSelectedIndex(-1);
        }
        setSummaryText();
    }

    double getCommonFuelUsed() {
        double retVal = 0;
        if (isActive() && !bRecuType && !bIndividualFuel)
            retVal = secFuelFlow;
        return retVal;
    }

    public void setvUnitFurnaces(Vector<UnitFurnace> vUnitFurnaces) {
        this.vUnitFurnaces = vUnitFurnaces;
        tuning = furnace.tuningParams;
    }

    public void setCumLossDischEnd(double cumLossDischEnd) {
        this.cumLossDischEnd = cumLossDischEnd;
    }

    public double sectionLength() {
        double len = 0;
        FceSubSection sub;
        if (isActive()) {
            for (int s = 0; s < subSections.size(); s++) {
                sub = subSections.get(s);
                if (sub.isActive())
                    len += sub.length;
            }
        }
        return len;
    }

    public double addEndPosList(Vector<Double> posList, double stLen) {
        double endLen = stLen;
        secStartPos = stLen;
        if (enabled) {
            for (FceSubSection su : subSections) {
                if (su.bActive) {
                    endLen = su.getEndLen(endLen);
                    posList.add(endLen);
                }
            }
        }
        secEndPos = endLen;
        return endLen;
    }

    public double evalEndLen(double stLen) {
        double endLen = stLen;
        secStartPos = stLen;
        if (enabled) {
            for (FceSubSection su : subSections) {
                if (su.bActive) {
                    endLen = su.getEndLen(endLen);
                }
            }
        }
        secEndPos = endLen;
        return endLen;
    }

    public boolean assignLoss(int lossID, double fraction) {
        FceSubSection sub;
        for (int s = 0; s < getNactive(); s++)
            subSections.get(s).assignLoss(lossID, fraction);
        return true;
    }

    public boolean assignLoss(int subSec, int lossID, double fraction) {
        FceSubSection sSub;
        if (subSec < getNactive())
            return subSections.get(subSec).assignLoss(lossID, fraction);
        else
            return false;
    }

    public boolean assignLoss(double atPos, int lossID, double fraction) {
        boolean bRetVal = false;
        for (FceSubSection ss: subSections) {
            if (ss.isActive()) {
                bRetVal = ss.assignLoss(atPos, lossID, fraction);
                if (bRetVal)
                    break;
            }
        }
        return bRetVal;
    }

    public boolean assignLoss(double stPos, double endPos, int lossID, double fraction) {
        boolean bRetVal = false;
        for (FceSubSection ss: subSections) {
            if (ss.isActive()) {
                bRetVal = ss.assignLoss(stPos, endPos, lossID, fraction);
            }
        }
        // bRetVal is meaningless here
        return bRetVal;
    }

    public boolean assignLoss(boolean atChEnd, boolean atDischEnd, int lossID, double fraction) {
        boolean retVal = false;
        int nLast = getNactive();
        if (nLast > 0) {
            FceSubSection sub = (atChEnd) ? subSections.get(0) : ((atDischEnd) ? subSections.get(nLast - 1): null);
            if (sub != null)  {
                retVal =  sub.assignLoss(lossID, fraction);
            }
        }
        return retVal;
    }

    double avgZoneTempForLosses()  {
        double lenTemp = 0;    // length * temperature;
        double retVal = 0;
        FceSubSection sSub;
        if (enabled) {
            for (int s = 0; s < subSections.size(); s++) {
                sSub = subSections.get(s);
                if (sSub.bActive) {
                    lenTemp += sSub.getLenTemp();
                }
                else
                    break;
            }
            retVal = lenTemp / sectionLength();
        }
        return retVal;
    }

    public void setLossFactor(double lossFactor) {
        for (FceSubSection sub: subSections)
            if (sub.isActive())
                sub.setLossFactor(lossFactor);
    }

    public void resetLossFactor() {
        setLossFactor(1.0);
    }

    public void resetLossAssignment() {
        for (FceSubSection sub: subSections)
            sub.resetLossAssignment();
    }

    public void setTempAtTCLocation(double tempAtTCLocation) {
        this.tempAtTCLocation = tempAtTCLocation;
    }

    public boolean isTempAtTCLocationSet() {
        return tempAtTCLocation > 0;
    }

    public void getReadyToCalcul() {
        FceSubSection sSub;
        if (enabled) {
            for (int s = 0; s < subSections.size(); s++) {
                sSub = subSections.get(s);
                if (sSub.bActive) {
                    sSub.setSection(this);   // TODO check if this is required
                    sSub.getReadyToCalcul();
                }
                else
                    break;
            }
        }
    }

    void redoLosses() {
        if (enabled && isActive()) {
            for (FceSubSection sSub:subSections) {
                if (sSub.bActive) {
                    sSub.redoLosses();
                 }
            }
            for (int u = firstSlot; u <= lastSlot; u++)
                vUnitFurnaces.get(u).collectLosses();
        }
    }

    public UnitFurnace getLastSlot() {
        return vUnitFurnaces.get(lastSlot);
    }



    void copyFromNextSection(FceSection nextSec) {
        UnitFurnace slotFrom, slotTo;
        slotTo = vUnitFurnaces.get(lastSlot);
        slotFrom = vUnitFurnaces.get(lastSlot + 1);
        slotTo.copyChTemps(slotFrom);
    }

    void copyFromPrevSection(FceSection prevSec) {
        UnitFurnace slotFrom, slotTo;
        slotTo = vUnitFurnaces.get(firstSlot - 1);
        slotFrom = vUnitFurnaces.get(firstSlot - 2);
        slotTo.copyChTemps(slotFrom);
    }

    public void setFuelFiring(FuelFiring fuelFiringData) {
        double airPHTemp = (furnace.tuningParams.bAllowRegenAirTemp && bRegenBurner && !bAPHCommonRecu) ? regenPHTemp : fuelFiringData.airTemp;

        if (bIndividualFuel) {
            fuelFiring = new FuelFiring(fuelInSection, bRegenBurner, excessAir, airPHTemp, fuelTemp);
            flueComposition = this.fuelFiring.flue;
        }
        else {
            fuelFiring = new FuelFiring(fuelFiringData, bRegenBurner);
            fuelFiring.setTemperatures(airPHTemp);
            fuelInSection = fuelFiringData.fuel;
            flueComposition = fuelFiringData.flue;
        }
        passFlueCompAndQty.noteValues(this.fuelFiring.flue, 0, 0, 0);
        totFlueCompAndQty.noteValues(passFlueCompAndQty);
    }

    public void changeAirTemp(double newAirTemp) {
        if (!bRecuType && bAPHCommonRecu)
            fuelFiring.setTemperatures(newAirTemp);
    }

    double getStHeight() {
        return subSections.get(0).stHeight;
    }

    double getEndHeight() {
        int nActive = getNactive();
        if (nActive > 0)
            return subSections.get(nActive - 1).endHeight;
        else
            return 0;
    }

    public double setInitialChHeat(double tIn, double rate) {
        for (int u = firstSlot; u <= lastSlot; u++ )
            tIn = vUnitFurnaces.get(u).setInitialChHeat(tIn, rate);
        return tIn;
    }

    public double chEntryTemp() {
         return vUnitFurnaces.get(firstSlot - 1).tempWmean;
    }

    public double chEntrySurfTemp() {
        return vUnitFurnaces.get(firstSlot - 1).tempWO;
    }

    public double chEntryDeltaT() {
        UnitFurnace uf = vUnitFurnaces.get(firstSlot - 1);
        return uf.tempWO - uf.tempWcore;
    }

    public double chEntryMeanT() {
        return vUnitFurnaces.get(firstSlot - 1).tempWmean;
    }

    public double chEndTemp() {
        return vUnitFurnaces.get(lastSlot).tempWmean;
    }

    public double chExitCoreTemp() { return vUnitFurnaces.get(lastSlot).tempWcore;}

    public double chExitSurfaceTemp() { return vUnitFurnaces.get(lastSlot).tempWO;}

    public double getStartTime() {
        UnitFurnace uf = vUnitFurnaces.get(firstSlot);
        return uf.endTime - uf.delTime;
    }

    public double getEntryChMeanTemp() {
        return vUnitFurnaces.get(firstSlot - 1).tempWmean;
    }

    public double getExitChMeanTemp() {
        return vUnitFurnaces.get(lastSlot).tempWmean;
    }

    public void setEntryGasTemp(double temp) {
        vUnitFurnaces.get(firstSlot - 1).tempG = temp;

    }

    public double getEnteringGasTemp() {
        return vUnitFurnaces.get(lastSlot + 1).tempG;
    }

    public double getExitingGasTemp() {
        return vUnitFurnaces.get(firstSlot).tempG;
    }


    public double getFirstSlotGasTemp() {
        return vUnitFurnaces.get(firstSlot).tempG;
    }

    public double getLastSlotGasTemp() {
        return vUnitFurnaces.get(lastSlot).tempG;
    }

    public double getLastSlotFceTemp() {
        return vUnitFurnaces.get(lastSlot).tempO;
    }

    public void setLastSlotGasTemp(double temp) {
        vUnitFurnaces.get(lastSlot).tempG = temp;
    }

    public void setEntryChTemps(double tempWO, double tempWmean, double tempWcore) {
        vUnitFurnaces.get(firstSlot - 1).setChTemps(tempWO, tempWmean, tempWcore);
    }

    public void showEntryResults() {
        vUnitFurnaces.get(firstSlot - 1).showResult();
    }

    public void evalSlotRadiationOut() {
        for (int s = firstSlot; s <= lastSlot; s++)
            vUnitFurnaces.get(s).evalSlotRadiationOut();
    }

    public void evalSlotRadiationSumm() {
        for (int s = firstSlot; s <= lastSlot; s++)
            vUnitFurnaces.get(s).evalSlotRadiationSumm();
    }

    String checkData() {
        String retVal = "";
        if (isActive()) {
            if (bIndividualFuel && (fuelInSection == null))
                retVal = "No Individual Fuel Selected!";
            for (int s = 0; s < nActiveSubs; s++)  {
                String subCheck = subSections.get(s).checkData();
                if (subCheck.length() > 0)
                    retVal += "\n" + subCheck;
            }
        }
        return retVal;
    }

    double roundedBalCom;

    public double setProductionBasedParams(double startTime) {
        UnitFurnace slot;
        for (int iSlot = firstSlot; iSlot <= lastSlot; iSlot++)
            startTime = vUnitFurnaces.get(iSlot).setProductionBasedParams(startTime);
        return startTime;
    }

    public CreateUFceReturn createUnitfces(Vector<Double> lenArr, Vector<UnitFurnace> ufs, CreateUFceReturn cReturn) {
        // iLloc and iSlot endTime are the last ones used
        UnitFurnace theSlot = null;
        FceSubSection sSec;
        boolean done;
        int iSlot = cReturn.iSlot;
        int iLloc = cReturn.iLloc;
        double totLen = cReturn.totLength;
        double balLCombined = cReturn.balLCombined;
        double endTime = cReturn.endTime;
        double calculStep = furnace.calculStep;
        secLoss = 0;
        if (isActive()) {
            firstSlot = iSlot + 1;
            double balLength, hBig, hEnd, slope, slotLen;
            double fceUnitArea;
            for (int s = 0; s < subSections.size(); s++) {
                sSec = subSections.get(s);
                if (sSec.bActive) {
                    secLoss += sSec.totLosses;
                    balLength = sSec.length;
                    hBig = sSec.stHeight;
                    slope = sSec.getRoofSlope(); // sSec.slope;
                    done = false;
                    slotLen = 0;
                    while (!done) {
                        if (balLCombined > calculStep) {
                            if (balLCombined < 1.5 * calculStep)
                                slotLen = SPECIAL.roundToNDecimals(balLCombined / 2, 4); //(double)(Math.round((balLCombined / 2) *  10000)) / 10000;
                            else
                                slotLen = calculStep;
                            balLength = SPECIAL.roundToNDecimals(balLength - slotLen, 3); //-= slotLen;
                            balLCombined = SPECIAL.roundToNDecimals(balLCombined - slotLen, 4);
                        }
                        else {
                            if (balLCombined > 0) {
                                slotLen = balLCombined;
                                iLloc++;
//                                balLCombined = (double)(Math.round((lenArr.get(iLloc).doubleValue() - totLen - slotLen) *1000)) / 1000;
                                balLCombined = SPECIAL.roundToNDecimals(lenArr.get(iLloc).doubleValue() - totLen - slotLen, 4);
                                balLength = SPECIAL.roundToNDecimals(balLength - slotLen, 4); //-= slotLen;
                                if (balLength <= 0)
                                    done = true;
                            }
                        }
                        if (slotLen > 0)  {
                            hEnd = hBig + slope * slotLen;
                            totLen += slotLen;
                            theSlot = new UnitFurnace(sSec, bRecuType, slotLen, totLen,
                                    furnace.width, hBig, hEnd, controller.furnaceFor);
                            endTime += theSlot.delTime;
                            theSlot.setEndTme(endTime);
                            iSlot++;
                            ufs.add(theSlot);    //ufs[iSlot] = theSlot;
                            hBig = hEnd;
                         }
//                        roundedBalCom =  (double)(Math.round(balLCombined * 1000)) / 1000;
                    }
                }
                else
                    break;
            }
            lastSlot = iSlot;
            reCheckLosses();
            iSlot++;
            // an empty slot between sections
            ufs.add(theSlot.getEmptySlot());  // ufs[iSlot] = theSlot.getEmptySlot();
            // copy some params to the one before the firstSlot
            ufs.get(firstSlot).copyParamesTo(ufs.get(firstSlot - 1));
//            ufs[firstSlot].copyParamesTo(ufs[firstSlot - 1]);
        }
        return new CreateUFceReturn(iLloc, iSlot, balLCombined, totLen, endTime);
    }

    UnitFurnace getUnitFceAtStPos(double stPos) {
        UnitFurnace ufReqd = null;
        UnitFurnace uf;
        double pos;
        for (int u = firstSlot ; u <= lastSlot; u++ ) {
            uf = vUnitFurnaces.get(u);
            pos = uf.stPos;
            if (pos > (stPos + 0.001))
                break;
            if (Math.abs(uf.stPos - stPos) < 0.001) {
                ufReqd = uf;
                break;
            }
        }
        return ufReqd;
     }

    public LossListWithVal getLossListWithVal() {
        lossValList = new LossListWithVal(furnace.lossTypeList);
        FceSubSection sub;
        LossListWithVal subList;
        Iterator<Integer> iter;
        double val;
        Integer lossID;
        for (int s = 0; s < subSections.size(); s++ ) {
            sub = subSections.get(s);
            if (sub.isActive()) {
                subList = sub.getLossListWithVal();
                iter = subList.keyIter();
                while (iter.hasNext()) {
                    lossID = iter.next();
                    val = subList.val(lossID);
                    if (val != 0)
                       lossValList.add(lossID, val);                }
            }
        }
        return lossValList;
    }

    JPanel lossValueDetPan(VScrollSync master) {
        FramedPanel jp = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 1, 0, 1);
        for (int s = 0; s < nActiveSubs; s++) {
            jp.add(subSections.get(s).lossValueDetPan(master), gbc);
            gbc.gridx++;
        }
        return jp;
    }

    // returns next column pos
    public int xlSecLossDetails(Sheet sheet, ExcelStyles style, int topRow, int leftCol) {
        for (int s = 0; s < nActiveSubs; s++) {
            leftCol = subSections.get(s).xlSecLossDetails(sheet, style, topRow, leftCol);
        }
        return leftCol;
    }


    void setValuesToUI() {
        tfFlueExhPercent.setData(burnerFlueExhFract * 100);
        tfExcessAir.setData(excessAir * 100);
        tfRegenPHtemp.setData(regenPHTemp);
        tfTCLocation.setData(tcLocationFromZoneStart * 1000);
        tfFuelTemp.setData(fuelTemp);
        chkAPHCommonRecu.setSelected(bAPHCommonRecu);
    }

    void setComboBoxes() {
        cbSecType.setSelectedItem((bRecuType) ? "Recuperative" : "With Burners");
        cbFuelChoice.setSelectedIndex((bIndividualFuel) ? 1 : 0);
        cbFuelChoice.setEnabled(bAllowSecFuel);
        cbFuels.setSelectedItem(fuelInSection);
//        cbFuels.setEnabled(false);
        cbBurnerType.setSelectedIndex((bRegenBurner) ? 1 : 0);
    }

    public boolean changeSubSectionData(int subNum, double length, double stHeight, double endHeight, double temperature) {
        boolean retVal = false;
        FceSubSection sub;
        if (enabled) {
            if (subNum >= 0 && subNum < subSections.size() && (subNum > 0 ? subSections.get(subNum -1).isActive() : true)) {
                sub = subSections.get(subNum);
                sub.changeData(length, stHeight, endHeight);
                sub.setTemperature(temperature);
                enableSubSecIfOK();
                enableSecIfOK();
                retVal = true;
            }
        }
        return retVal;
    }

    public void setSectionID(int secNum) {
        this.secNum = secNum;
    }

    public static DFHFurnace getFurnace() {
        return furnace;
    }

    void preparePanel() {
        if (!bPanelReady) {
            sectionsPanel = new FramedPanel(new GridBagLayout());
            gbcSecs = new GridBagConstraints();
            gbcSecs.gridx = 0;
            gbcSecs.gridy = 0;
            gbcSecs.gridwidth = subSections.size();
            sectionsPanel.add(getSummPanel(), gbcSecs);

            gbcSecs.gridwidth = 1;
            gbcSecs.gridy++;
            for (int s = 0; s < subSections.size(); s++) {
                sectionsPanel.add(subSections.get(s).getFceSubDetFrame(), gbcSecs);
                gbcSecs.gridx++;
            }
            bPanelReady = true;
        }
    }


    JButton summButton;

    Component getSummPanel() {

        summButton = new JButton();
        summButton.setPreferredSize(new Dimension(300, 50));
        summButton.addActionListener(new SummButtActionListener());
        setSummaryText();
//        jp.add(butt);
        summButton.setEnabled(false);
        return summButton;
    }

    void setSummaryText() {
        String dat1 = sectionName() + " (" + ((bRecuType) ? "Recuperative" : "With Burners") + ")";
        String dat2;
        if (bRecuType)
            dat2 = "";
        else
            dat2 = ((bRegenBurner) ? "REGEN Burners" : "Normal Burners") + " with " +
                    ((bIndividualFuel) ? "Individual Fuel" : "Common Fuel");
        summButton.setText("<html>" + dat1 + "<p>" + dat2 + "</html>");
    }

    public String sectionName() {
        if (bAddedSoak)
            return "TOP ONLY SOAK";
        else {
            return (botSection ? "Bottom Zones - Zone #" :
                    ((controller.heatingMode != DFHeating.HeatingMode.TOPBOT) ? "Zone #" : "Top Zones - Zone #")) + secNum;
        }
    }

    double fuelSensible, airTemp;
    double totHeatIN, totHeatOUT;
    double regenFlue, regenFlueHeat;
    public double fceFlue, fceFlueHeat;
    double fuelTemp;
    void prepareHeatbalance() {
        totHeatIN = 0;
        totHeatOUT = 0;
        fuelSensible = secFuelFlow * fuelFiring.fuelSensibleHeat;

        secAir = secFuelFlow * fuelFiring.actAFRatio;
        airTemp = fuelFiring.airTemp;
        fceFlue = secFlueFlow;
//        fceFlueHeat =  regenFlueHeat * fceFlue / regenFlue;
        totHeatIN = combustionHeat + fuelSensible + airSensible + heatPassingFlueIn;
        totHeatOUT = chargeHeat + heatPassingFlueOut + pLosses + regenFlueHeat + fceFlueHeat;        // heatToSecFlue;
        evalChHeatFraction();
    }

    double getFlueBurnerExitQty() {
        return regenFlue;
    }

    double getFlueBurnerExitTemp() {
        return burnerFlueHeat;
    }

    void addFuelUsage(FuelsAndUsage fuelsAndUsage) {
        if (!bRecuType) {
            secFuelUsage = new FuelsAndUsage();
//            if (secFuelFlow < 0)
//                showError("Fuel flow in " + sectionName() + " is negative at " +
//                        (new DecimalFormat("#,##0.00")).format(secFuelFlow) + "!", 5000);
            fuelFiring.addFuelUsage(secFuelUsage, secFuelFlow, false);
            if (fuelFiring.fuel.bMixedFuel) {
                secFuelUsageBreakup = new FuelsAndUsage();
                fuelFiring.addFuelUsage(secFuelUsageBreakup, secFuelFlow, true);
            }
            else
                secFuelUsageBreakup = null;
            fuelFiring.addFuelUsage(fuelsAndUsage, secFuelFlow, true);
        }
    }

    public boolean isMixedFuel() {
        return (isActive() && !bRecuType && fuelInSection.isbMixedFuel());
    }


    public Vector<Fuel> addUniqueFuels(Vector<Fuel> uniqueFuels)  {
        if (!bRecuType)
            fuelFiring.addUniqueFuels(uniqueFuels);
        return uniqueFuels;
    }

    JPanel fuelSummaryPanel(String title) {
        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        if (!bRecuType) {
            if (bRegenBurner)
                jp.add(secFuelUsage.getSummaryPanel(title, true, true, regenPHTemp, true), gbc);
            else
                jp.add(secFuelUsage.getSummaryPanel(title, true, true, airTemp, true), gbc);
            if (secFuelUsageBreakup != null) {
                gbc.gridy++;
                jp.add(secFuelUsageBreakup.getSummaryPanel("Flows of Fuel Mix Elements", true, true, airTemp, false), gbc);
            }
        }
        return jp;
    }

    int addReportColumns(Reporter report) {
        int nCol = 0;
        if (isActive()) {
            String zoneID = ((botSection) ? "BZ" :"TZ") + "#" + ("" + secNum).trim() + ".";
            nCol++;
            report.addColumn(Reporter.ColType.NUMBER, 120, 2500, "#,##0", zoneID, "StripEndT", "(C)");
            report.addColumn(Reporter.ColType.NUMBER, 120, 2500, "#,##0", zoneID, "FceT", "(C)");
            report.addColumn(Reporter.ColType.NUMBER, 120, 2500, "#,##0", zoneID, "GasT", "(C)");
            if (!bRecuType)
                report.addColumn(Reporter.ColType.NUMBER, 120, 2500, "#,##0.##", zoneID, "Fuel", "(" + fuelFiring.fuel.units + "/h)" );
        }
        return nCol;
    }

    void addReportData(Vector<Object> results) {
        if (isActive()) {
            double atPos = secStartPos + tcLocationFromZoneStart;
            results.add(furnace.getChTempAt(secStartPos + sectionLength(), botSection));
            results.add(furnace.getFceTempAt(atPos, botSection));
            results.add(furnace.getGasTempAt(atPos, botSection));
            if (!bRecuType)
                results.add(secFuelFlow);
        }
    }

    public double tcLoc() {
        return secStartPos + tcLocationFromZoneStart;
    }

    JPanel secResults;
    Vector<XLcellData> vXLGeneral;
    Vector<XLcellData> vXLFuel, vXLAir, vXLPassFlueIN, vXLTotIn;
    Vector<XLcellData> vXLCharge, vXLLosses, vXLPassFlueOut, vXLRegenFlue, vXLFceFlue, vXLTotOut;

    JPanel secResultsPanel(boolean bMixedFuel) {
        int datW = 80;
        JPanel jp = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        Insets ins = new Insets(0, 0, 0, 0);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = ins;
        gbc.weightx = 0.1;
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridx = 0;
        gbcL.gridy = 0;
        NumberLabel nL;
        FramedPanel grpPan = new FramedPanel(new GridBagLayout());
        Dimension dim = new Dimension(datW, 20);
        vXLGeneral = new Vector<XLcellData>();
        gbcL.gridy = 0;
        double tcPos = getTcPosition();
        nL = new NumberLabel(tcPos * 1000, datW, "#,###");
        vXLGeneral.add(nL);
        grpPan.add(nL, gbcL);
        gbcL.gridy++;
        nL = new NumberLabel(furnace.getFceTempAt(tcPos, botSection), datW, "#,###");
        vXLGeneral.add(nL);
        grpPan.add(nL, gbcL);
        gbc.gridy++;
        jp.add(grpPan, gbc);


        vXLFuel = new Vector<XLcellData>();
        vXLAir = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        if (bRecuType)
            addBlanks(grpPan, gbcL, dim, 5, vXLFuel);
        else  {
            TextLabel bTypeL = new TextLabel((isWithRegen()? "REGEN" :"NORMAL"), false);
            vXLFuel.add(bTypeL);
            bTypeL.setPreferredSize(dim);
            grpPan.add(bTypeL, gbcL);
            gbcL.gridy++;
            nL = new NumberLabel(secFuelFlow, datW, "#,##0.00", true);
            vXLFuel.add(nL);
            grpPan.add(nL, gbcL);
            gbcL.gridy++;
            nL = new NumberLabel(fuelFiring.fuelTemp,datW, "#,###");
            if (fuelFiring.fuel.bMixedFuel)
                nL.setText(" -- ");
            vXLFuel.add(nL);
            grpPan.add(nL, gbcL);
            gbcL.gridy++;

            nL = new NumberLabel(fuelSensible, datW, "#,###", true);
            vXLFuel.add(nL);
            grpPan.add(nL, gbcL);
            gbcL.gridy++;
            nL = new NumberLabel(combustionHeat, datW, "#,###", true);
            vXLFuel.add(nL);
            grpPan.add(nL, gbcL);
            gbcL.gridy++;
        }
        gbc.gridy++;
        jp.add(grpPan, gbc);
        gbcL.gridy = 0;
        grpPan = new FramedPanel(new GridBagLayout());
        if (bRecuType)
            addBlanks(grpPan, gbcL, dim, 3, vXLAir);
        else {
            gbcL.gridy = 0;
            nL = new NumberLabel(secAir, datW, "#,###");
            vXLAir.add(nL);
            grpPan.add(nL, gbcL);
            gbcL.gridy++;
            nL = new NumberLabel(airTemp, datW, "#,###");
            vXLAir.add(nL);
            grpPan.add(nL, gbcL);
            gbcL.gridy++;
            nL = new NumberLabel(airSensible, datW, "#,###", true);
            vXLAir.add(nL);
            grpPan.add(nL, gbcL);
        }
        gbc.gridy++;
        jp.add(grpPan, gbc);

        vXLPassFlueIN = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        nL = new NumberLabel(fluePassThrough, datW, "#,###");
        vXLPassFlueIN.add(nL);
        grpPan.add(nL, gbcL);
        gbcL.gridy++;
        nL = new NumberLabel(passFlueTin, datW, "#,###");
        vXLPassFlueIN.add(nL);
        grpPan.add(nL, gbcL);
        gbcL.gridy++;
        nL = new NumberLabel(heatPassingFlueIn, datW, "#,###", true);
        vXLPassFlueIN.add(nL);
        grpPan.add(nL, gbcL);
        gbc.gridy++;
        jp.add(grpPan, gbc);

        vXLTotIn = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        nL = new NumberLabel(totHeatIN, datW, "#,###", true);
        vXLTotIn.add(nL);
        grpPan.add(nL, gbcL);
        gbc.gridy++;
        jp.add(grpPan, gbc);

        gbc.gridy++;
        jp.add(sizedLabel("", dataColSize), gbc);

        vXLCharge = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        nL = new NumberLabel(production.production, datW, "#,###");
        vXLCharge.add(nL);
        grpPan.add(nL, gbcL);
        gbcL.gridy++;
        if (furnace.bTopBot) {
            nL = new NumberLabel(chargeHeatFraction * 100, datW, "#,###");
            vXLCharge.add(nL);
            grpPan.add(nL, gbcL);
            gbcL.gridy++;
        }
        nL = new NumberLabel(chEntryTemp(), datW, "#,###");
        vXLCharge.add(nL);
        grpPan.add(nL, gbcL);
        gbcL.gridy++;
        nL = new NumberLabel(chEndTemp(), datW, "#,###");
        vXLCharge.add(nL);
        grpPan.add(nL, gbcL);
        gbcL.gridy++;
        nL = new NumberLabel(chargeHeat, datW, "#,###", true);
        vXLCharge.add(nL);
        grpPan.add(nL, gbcL);
        gbc.gridy++;
        jp.add(grpPan, gbc);

        vXLLosses = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        if (tuning.bSlotRadInCalcul)  {
            nL = new NumberLabel(pLosses - pInterRadLoss, datW, "#,###");
            vXLLosses.add(nL);
            grpPan.add(nL, gbcL);
            gbcL.gridy++;
            nL = new NumberLabel(pInterRadLoss, datW, "#,###");
            vXLLosses.add(nL);
            grpPan.add(nL, gbcL);
            gbcL.gridy++;
        }
        nL = new NumberLabel(pLosses, datW, "#,###", true);
        vXLLosses.add(nL);
        grpPan.add(nL, gbcL);
        gbc.gridy++;
        jp.add(grpPan, gbc);

        vXLPassFlueOut = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        nL = new NumberLabel(tempFlueOut, datW, "#,###");
        vXLPassFlueOut.add(nL);
        grpPan.add(nL, gbcL);
        gbcL.gridy++;
        nL = new NumberLabel(heatPassingFlueOut, datW, "#,###", true);
        vXLPassFlueOut.add(nL);
        grpPan.add(nL, gbcL);
        gbc.gridy++;
        jp.add(grpPan, gbc);

        vXLRegenFlue = new Vector<XLcellData>();
        vXLFceFlue = new Vector<XLcellData>();

        if (furnace.anyRegen() > 0) {
            grpPan = new FramedPanel(new GridBagLayout());
            gbcL.gridy = 0;
            nL = new NumberLabel(regenFlue, datW, "#,###");
            vXLRegenFlue.add(nL);
            grpPan.add(nL, gbcL);
            gbcL.gridy++;
            nL = new NumberLabel(regenFlueHeat, datW, "#,###", true);
            vXLRegenFlue.add(nL);
            grpPan.add(nL, gbcL);
            gbc.gridy++;
            jp.add(grpPan, gbc);

            grpPan = new FramedPanel(new GridBagLayout());
            gbcL.gridy = 0;
            nL = new NumberLabel(fceFlue, datW, "#,###");
            vXLFceFlue.add(nL);
            grpPan.add(nL, gbcL);
            gbcL.gridy++;
            nL = new NumberLabel(fceFlueHeat, datW, "#,###", true);
            vXLFceFlue.add(nL);
            grpPan.add(nL, gbcL);
            gbc.gridy++;
            jp.add(grpPan, gbc);

        }
        else {
            grpPan = new FramedPanel(new GridBagLayout());
            gbcL.gridy = 0;
            nL = new NumberLabel(flueInSection, datW, "#,###");
            vXLFceFlue.add(nL);
            grpPan.add(nL, gbcL);
            gbcL.gridy++;
            nL = new NumberLabel(heatToSecFlue, datW, "#,###", true);
            vXLFceFlue.add(nL);
            grpPan.add(nL, gbcL);
            gbc.gridy++;
            jp.add(grpPan, gbc);
        }

        vXLTotOut = new Vector<XLcellData>();
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        nL = new NumberLabel(totHeatOUT, datW, "#,###", true);
        vXLTotOut.add(nL);
        grpPan.add(nL, gbcL);
        gbc.gridy++;
        jp.add(grpPan, gbc);
        if (tuning.bShowFlueCompo) {
            FlueComposition comp;
            for (int n = 0; n < 2; n++) {
                comp = (n == 0) ? flueComposition : totFlueCompAndQty.flueCompo;
                grpPan = new FramedPanel(new GridBagLayout());
                gbcL.gridy = 0;
                addBlanks(grpPan, gbcL, dim, 1, vXLTotOut);
                if (n == 0 && bRecuType) {
                    addBlanks(grpPan, gbcL, dim, 5, vXLTotOut);
                }
                else {
                    nL = new NumberLabel(comp.fractCO2 * 100, datW, "#,##0.00");
                    vXLTotOut.add(nL);
                    grpPan.add(nL, gbcL);
                    gbcL.gridy++;
                    nL = new NumberLabel(comp.fractH2O * 100, datW, "#,##0.00");
                    vXLTotOut.add(nL);
                    grpPan.add(nL, gbcL);
                    gbcL.gridy++;
                    nL = new NumberLabel(comp.fractSO2 * 100, datW, "#,##0.00");
                    vXLTotOut.add(nL);
                    grpPan.add(nL, gbcL);
                    gbcL.gridy++;
                    nL = new NumberLabel(comp.fractO2 * 100, datW, "#,##0.00");
                    vXLTotOut.add(nL);
                    grpPan.add(nL, gbcL);
                    gbcL.gridy++;
                    nL = new NumberLabel(comp.fractN2 * 100, datW, "#,##0.00");
                    vXLTotOut.add(nL);
                    grpPan.add(nL, gbcL);
                }
                gbc.gridy++;
                jp.add(grpPan, gbc);
            }
        }

        secResults = jp;
        return jp;
    }

    void addBlanks(JPanel jp, GridBagConstraints gbc, Dimension dim, int lines, Vector<XLcellData> xl) {
        TextLabel l;
        for (int n = 0; n < lines; n++) {
            l = new TextLabel(" ");
            l.setPreferredSize(dim);
            jp.add(l, gbc);
            xl.add(l);
            gbc.gridy++;
        }
    }

    public int xlSecResults(Sheet sheet, ExcelStyles style, int topRow, int leftCol) {
        sheet.setColumnWidth(leftCol, 3000);
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, vXLGeneral) + 1;
        if (!bRecuType) {
            topRow = style.xlAddXLCellData(sheet, topRow, leftCol, vXLFuel) + 1;
            topRow = style.xlAddXLCellData(sheet, topRow, leftCol, vXLAir) + 1;
        }
        else
            topRow += (cHFuel.size() + 1 + cHAir.size() + 1);
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, vXLPassFlueIN) + 1;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, vXLTotIn) + 1;
        topRow++;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, vXLCharge) + 1;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, vXLLosses) + 1;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, vXLPassFlueOut) + 1;
        if (furnace.anyRegen() > 0)
            topRow = style.xlAddXLCellData(sheet, topRow, leftCol, vXLRegenFlue) + 1;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, vXLFceFlue) + 1;
        topRow = style.xlAddXLCellData(sheet, topRow, leftCol, vXLTotOut);
        return topRow;
    }

    public int xlSecFuel(Sheet sheet, ExcelStyles styles, int topRow, int leftCol) {
        topRow = styles.xlMultiPairColPanel(secFuelUsage.mpFuelSummary, sheet, topRow, leftCol);
        if (secFuelUsageBreakup != null) {
            topRow = styles.xlMultiPairColPanel(secFuelUsageBreakup.mpFuelSummary, sheet, topRow + 1, leftCol);
        }
        return topRow;
    }


    public Component sectionsPanel() {
        return sectionsPanel;
    }

    public void noteLossListChange() {
        for (int s = 0; s < subSections.size(); s++)
            subSections.get(s).noteLossListChange();
    }

    public boolean addToProfileTrace(Vector<DoublePoint> vProf, double scale, double baseY) {
        FceSubSection sub;
        boolean bRetVal = true;
        for (int ss = 0; ss < subSections.size(); ss++) {
            sub = subSections.get(ss);
            if (sub.isActive())
                bRetVal &= sub.addToProfileTrace(vProf, scale, baseY);
        }
        return bRetVal;
    }

    double getAirToCommonRecu() {
        if (!bRecuType && bAPHCommonRecu)
            return secFuelUsage.totAirFlow();
        else
            return 0;
    }

    double getAirToRegen() {
        if (!bRecuType && !bAPHCommonRecu)
            return secFuelUsage.totAirFlow();
        else
            return 0;
    }


    double getAirHeatCommonRecu() {
        if (!bRecuType && bAPHCommonRecu)
            return secFuelUsage.totAirSensHeat();
        else
            return 0;
    }

    double getAirHeatRegen() {
         if (!bRecuType && !bAPHCommonRecu)
             return secFuelUsage.totAirSensHeat();
         else
             return 0;
    }

    public void takeValuesFromUI() {
        bRecuType = cbSecType.getSelectedItem().toString().equals("Recuperative");
        if (!bRecuType) {
            try {
                burnerFlueExhFract =tfFlueExhPercent.getData() / 100;
                regenPHTemp = tfRegenPHtemp.getData();
                fuelTemp = tfFuelTemp.getData();
            } catch (NumberFormatException e) {

                burnerFlueExhFract = 0;
            }
            bIndividualFuel = (cbFuelChoice.getSelectedIndex() == 1);
            if (bIndividualFuel) {
                fuelInSection = (Fuel) cbFuels.getSelectedItem();
                excessAir = tfExcessAir.getData() / 100;
            }
            bRegenBurner = (cbBurnerType.getSelectedIndex() == 1);
            bAPHCommonRecu = chkAPHCommonRecu.isSelected();
        }
        else
            bRegenBurner = false;
        setSummaryText();
        for (int sub = 0; sub < subSections.size(); sub++)
            subSections.get(sub).takeValuesfromUI();
        setTCLocation();
        tcLocationFromZoneStart = tfTCLocation.getData() / 1000;
    }

    public OneZone getZonePerfData() {
        return new OneZone(furnace, this);
    }

    public double getTcPosition() {
        return secStartPos + tcLocationFromZoneStart;
    }

    public String dataInXML() {
        String xmlStr = XMLmv.putTag("cbSecType", "" + cbSecType.getSelectedItem()) +
                XMLmv.putTag("cbFuelChoice", "" + cbFuelChoice.getSelectedItem()) +
                XMLmv.putTag("cbFuels", "" + cbFuels.getSelectedItem()) +
                XMLmv.putTag("excessAir", "" + "" + excessAir) +
                XMLmv.putTag("fuelTemp", "" + "" + fuelTemp) +
                XMLmv.putTag("cbBurnerType", "" + cbBurnerType.getSelectedItem()) +
                XMLmv.putTag("bAPHCommonRecu", ((bAPHCommonRecu) ? "1" : "0")) +
                XMLmv.putTag("flueExhFract", "" + "" + burnerFlueExhFract) +
                XMLmv.putTag("regenPHTemp", "" + "" + regenPHTemp) +
                XMLmv.putTag("tcLocation", "" + "" + tcLocationFromZoneStart);
        if (!bRecuType && bGasTempSpecified)  // TODO to be removed
            xmlStr += XMLmv.putTag("bGasTempSpecified", ((bGasTempSpecified)?"1":"0")) +
                    XMLmv.putTag("presetGasTemp", presetGasTemp);
        xmlStr += "\n";
         String subSecStr = "";
        int nActive = 0;
        FceSubSection ss;
         for (int i = 0; i < subSections.size(); i++) {
            ss = subSections.get(i);
             if (ss.bActive) {
                subSecStr += XMLmv.putTag("ss" + ("" + i).trim(), ss.dataInXML());
                 nActive++;
             }
         }
         xmlStr += XMLmv.putTag("subsections", XMLmv.putTag("nActive", "" + nActive) + subSecStr);
         return xmlStr;
    }

    int nActiveSubs = -1;

    int getNactive() {
        if (nActiveSubs > 0)
            return nActiveSubs;
        else {
            int nActive = 0;
            FceSubSection ss;
            for (int i = 0; i < subSections.size(); i++) {
                ss = subSections.get(i);
                if (ss.bActive)  {
                    if (i == 0)
                        ss.setLocInSec(true, false);
                    nActive++;
                }
                else
                    break;
            }
            if (nActive > 0) {
                if (nActive == 1)
                    subSections.get(0).setLocInSec(true, true);
                else
                    subSections.get(nActive - 1).setLocInSec(false, true);
            }
            nActiveSubs = nActive;
            return nActive;
        }
    }

    public boolean  takeDataFromXML(String xmlStr) {
        boolean bRetVal = true;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "cbSecType", 0);
        cbSecType.setSelectedItem(vp.val);
        vp = XMLmv.getTag(xmlStr, "cbFuelChoice", 0);
        cbFuelChoice.setSelectedItem(vp.val);
        vp = XMLmv.getTag(xmlStr, "cbFuels", 0);
        Fuel f = controller.fuelFromName(vp.val);
        if (f == null) {
            cbFuels.setSelectedIndex(-1);
            bRetVal &= false;
        }
        else
            cbFuels.setSelectedItem(f);
        vp = XMLmv.getTag(xmlStr, "cbBurnerType", 0);
        ActionListener[] listeners = cbBurnerType.getActionListeners();
        for (ActionListener l:listeners)
            cbBurnerType.removeActionListener(l);
        cbBurnerType.setSelectedItem(vp.val);
        for (ActionListener l:listeners)
            cbBurnerType.addActionListener(l);
        cbBurnerType.setEnabled(true);
        if (cbBurnerType.getSelectedIndex() == 1)
            bAPHCommonRecu = false;
        vp = XMLmv.getTag(xmlStr, "bAPHCommonRecu", 0);
        if (vp.val.length() > 0)
            bAPHCommonRecu = (vp.val.equals("1"));
        try {
            vp = XMLmv.getTag(xmlStr, "flueExhFract", 0);
            burnerFlueExhFract = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "regenPHTemp", 0);
            regenPHTemp = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "excessAir", 0);
            excessAir = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "fuelTemp", 0);
            if (vp.val.length() > 0)
                fuelTemp = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "tcLocation", 0);
            if (vp.val.length() > 0)
                tcLocationFromZoneStart = Double.valueOf(vp.val);
        } catch (Exception e) {
            bRetVal &= false;
        }
        setValuesToUI();

        vp = XMLmv.getTag(xmlStr, "subsections", vp.endPos);
        bRetVal &= takeSubSecsFromXML(vp.val);
        takeValuesFromUI();
        return bRetVal;
     }

    void setTempForLosses(XYArray tprof) {
        for (int ss = 0; ss < getNactive(); ss++)
            subSections.get(ss).setTempForLosses(tprof);
    }

    void setTempForLosses(MultiColData tResults, int trace)  {
        for (int ss = 0; ss < getNactive(); ss++)
            subSections.get(ss).setTempForLosses(tResults, trace);
    }

    void allowManuelTempForLosses(boolean bAllow) {
        for (FceSubSection sub:subSections)
            sub.allowManuelTempForLosses(bAllow);
    }

    boolean takeSubSecsFromXML(String xmlStr) {
        boolean bRetVal = true;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "nActive", 0);

        int nActive = 0;
        try {
            nActive = Integer.valueOf(vp.val.trim());
        } catch (NumberFormatException e) {
            return false;
        }

        for (int s =0; s < nActive; s++) {
           vp = XMLmv.getTag(xmlStr, "ss" + ("" + s).trim(), vp.endPos);
           subSections.get(s).takeDataFromXML(vp.val);
        }
        return bRetVal;
    }

    void debug(String msg) {
        System.out.println("FceSection: " + msg);
    }

    void enableSubSecIfOK() {
        FceSubSection sub, prevSub;
        prevSub = subSections.get(0);
        for (int s = 1; s < subSections.size(); s++) {
            sub = subSections.get(s);
            sub.enableSubsection(enabled && prevSub.isActive());
//            sub.enableIfOF();
            prevSub = sub;
        }
        furnace.enableSectionsIfOK(false);
        furnace.enableSectionsIfOK(true);
    }

    public void enableChildrenIfOK() {
        if (enabled) {
            FceSubSection sub, prevSub;
            prevSub = subSections.get(0);
            for (int s = 1; s < subSections.size(); s++) {
                sub = subSections.get(s);
                sub.enableSubsection(enabled && prevSub.isActive());
                prevSub = sub;
            }
        }
    }

    void enableSecIfOK() {
        summButton.setEnabled(isActive() && enabled);
    }

    public boolean isActive() {
        if (subSections.size() > 0) {
            return (subSections.get(0).isActive() && enabled);
        } else
            return false;
    }

   public ErrorStatAndMsg isSectionOK() {
       boolean ok = true;
       String msg = "";
       FceSubSection sub;
       if (isActive()) {
           sub = subSections.get(0);
           ok &= !sub.isSubSectionOK().inError;
           if (ok) {
               for (int s = 1; s < MAXSUBSECTIONS; s++) {
                sub = subSections.get(s);
                  if (sub.isActive()) {
                    ok &= !sub.isSubSectionOK().inError;
                    if (!ok) {
                        msg = "Sub-section " + s + " is in Error";
                        break;
                    }
                  }
                   else
                      break;
               }
           }
       }
       else  {
           msg = "Section is not Active";
        ok = false;
       }
       return new ErrorStatAndMsg(!ok, msg);
    }

   void getSummaryData(Component c) {
       setTCLocation();
       JDialog dlg = new SectionSummaryDlg(DFHeating.mainF);
       dlg.setLocationRelativeTo(c);
       dlg.setVisible(true);
       DFHeating.mainF.setVisible(true);
    }

    void enableComponents() {
        cbFuelChoice.setEnabled(false);
        tfExcessAir.setEnabled(false);
        cbFuels.setEnabled(false);
        cbBurnerType.setEnabled(false);
        tfFlueExhPercent.setEnabled(false);
        tfRegenPHtemp.setEnabled(false);
        tfFuelTemp.setEnabled(false);
        chkAPHCommonRecu.setEnabled(false);

        tfExcessAir.setEditable(enaEdit);
//        tfFlueExhPercent.setEditable(enaEdit);
        tfRegenPHtemp.setEditable(enaEdit);

        if (!bAllowSecFuel)
           cbFuelChoice.setSelectedIndex(0);
        if (cbSecType.getSelectedIndex() == 1) {
            cbFuelChoice.setEnabled(bAllowSecFuel  && enaEdit );
            if (cbFuelChoice.getSelectedIndex() == 1) {
                tfExcessAir.setEnabled(true && enaEdit);
                cbFuels.setEnabled(true && enaEdit );
                Fuel selF = (Fuel)cbFuels.getSelectedItem();
                if (selF != null && !selF.bMixedFuel)
                    tfFuelTemp.setEnabled(true && enaEdit);
            }
            cbBurnerType.setEnabled(true && enaEdit);
            if (cbBurnerType.getSelectedIndex() == 1) {
                 chkAPHCommonRecu.setEnabled(true && enaEdit );
                tfFlueExhPercent.setEnabled(true && enaEdit);
                if (furnace.tuningParams.bAllowRegenAirTemp && !chkAPHCommonRecu.isSelected()) {// || (cbFuelChoice.getSelectedIndex() == 1))
                    tfRegenPHtemp.setEnabled(true && enaEdit);
                }
                else
                    tfRegenPHtemp.setData(0);
            }
            else {
               tfFlueExhPercent.setData(0);
               tfRegenPHtemp.setData(0);
                chkAPHCommonRecu.setSelected(true);
            }
        }
        else {
            cbBurnerType.setSelectedIndex(0);
            tfFlueExhPercent.setData(0);
        }
    }

    public double minHeight() {
        double minHt = Double.MAX_VALUE;
        FceSubSection sub;
        for (int ss = 0; ss < subSections.size(); ss++) {
            sub = subSections.get(ss);
            if (sub.isActive()) {
                minHt = Math.min(Math.min(minHt, sub.stHeight), sub.endHeight);
            } else
                break;
        }
        return minHt;
    }

    double reCheckLosses() {
        pLosses = 0;
        pInterRadLoss = 0;
        UnitFurnace uf;
        for (int sl = firstSlot; sl <= lastSlot; sl++) {
            uf = vUnitFurnaces.get(sl);
            pLosses += uf.totLosses();
            pInterRadLoss = pInterRadLoss + uf.slotRadNetOut;
        }
        return pLosses;
    }

    public double getLosses() {
        return pLosses;
    }

   public double getTempFlueOut() {
       return vUnitFurnaces.get(firstSlot - 1).tempG;
   }

    public FlueCompoAndQty fuelInFsection(FlueCompoAndQty flueIN) {
        tempFlueOut = vUnitFurnaces.get(firstSlot - 1).tempG;
        passFlueCompAndQty.noteValues(flueIN);
        if (flueIN == null) {
            fluePassThrough = 0;
            passFlueTin = 0;
            heatPassingFlueIn = 0;
            heatPassingFlueOut = 0;
        } else {
            fluePassThrough = flueIN.flow;
            passFlueTin = flueIN.flueTemp;
            heatPassingFlueIn = flueIN.flueHeat; //fluePassThrough * flueComposition.getHeatFromTemp(passFlueTin);
            flueIN.setTemperature(tempFlueOut);
            heatPassingFlueOut = flueIN.flueHeat; //fluePassThrough * flueComposition.getHeatFromTemp(tempFlueOut);
        }
        heatFromPassingFlue = heatPassingFlueIn - heatPassingFlueOut;
        FlueCompoAndQty flueFromSec;
        FlueCompoAndQty retVal;
        if (bRecuType) {
            totalFlueExitHeat = heatPassingFlueOut;
            retVal = flueIN;
        } else {
            heatToCharge();
            double totheat, fltOut;
            totheat = chargeHeat + reCheckLosses() - heatFromPassingFlue;
            flueInSection = fuelEtcFromHeat(totheat, tempFlueOut);
            flueFromSec = new FlueCompoAndQty(flueComposition, flueInSection, tempFlueOut);
            if (flueIN == null)
                retVal = flueFromSec;
            else {
                retVal = new FlueCompoAndQty("", flueIN, flueFromSec);// (flueComposition, flueIN.flow + flueInSection, tempFlueOut);
                retVal.setTemperature(tempFlueOut);
            }
        }
        totFlueCompAndQty.noteValues(retVal);
        return retVal;
    }

    public FlueCompoAndQty fuelInFsectionTESTED(FlueCompoAndQty flueIN) {
        double passFlueExitHeat;
        fluePassThrough = flueIN.flow;
        passFlueTin = flueIN.flueTemp;
        tempFlueOut = vUnitFurnaces.get(firstSlot - 1).tempG;
        heatPassingFlueOut = fluePassThrough * flueComposition.sensHeatFromTemp(tempFlueOut);
        heatPassingFlueIn = fluePassThrough * flueComposition.sensHeatFromTemp(passFlueTin);
        heatFromPassingFlue = heatPassingFlueIn - heatPassingFlueOut;
        FlueCompoAndQty flueFromSec;
        FlueCompoAndQty retVal;
        if (bRecuType) {
            totalFlueExitHeat = heatPassingFlueOut;
            retVal = new FlueCompoAndQty(flueIN.flueCompo, 0, tempFlueOut);
        }
        else {
            heatToCharge();
            double totheat, fltOut;
            totheat = chargeHeat + reCheckLosses() - heatFromPassingFlue;
            flueInSection = fuelEtcFromHeat(totheat, tempFlueOut);
            flueFromSec = new FlueCompoAndQty(flueComposition, flueInSection, tempFlueOut);
            if (flueIN.flueCompo == null)
                retVal = flueFromSec;
            else {
                retVal = new FlueCompoAndQty("", flueIN, flueFromSec);// (flueComposition, flueIN.flow + flueInSection, tempFlueOut);
                retVal.setTemperature(tempFlueOut);
            }
         }
        return retVal;
    }

   public double flueQtyForExitT(double flueExitT) {
       double totHeat = 0;
       for (int u = firstSlot; u <= lastSlot; u++)
            totHeat += vUnitFurnaces.get(u).totalHeat();
       return flueForHeatIfFiring(totHeat, flueExitT);
   }

   public double heatToCharge() {
       chargeHeat = 0;
       for (int s = firstSlot; s <= lastSlot; s++)
           chargeHeat += vUnitFurnaces.get(s).chargeHeat;
       return  chargeHeat;
   }

   void evalChHeatFraction() {
       double stTemp, endTemp;
       stTemp = vUnitFurnaces.get(firstSlot - 1).tempWmean;
       endTemp = vUnitFurnaces.get(lastSlot).tempWmean;
       chargeHeatFraction = chargeHeat /
                   (production.production * production.charge.getDeltaHeat(stTemp, endTemp));
   }

   double flueForHeatIfFiring(double heat, double flueExitTemp) {
       if (!bRecuType) {
           double realCalVal, airHeat, flueHeat;
           airHeat = fuelFiring.airHeatPerUfuel;
           flueHeat = fuelFiring.flueHeatPerUFuel(flueExitTemp);
           realCalVal = fuelInSection.calVal + airHeat - flueHeat; //fuelFiring.effFuelCalVal(flueExitTemp);
           double fuelQty = heat / realCalVal;
           double flue = fuelQty * fuelFiring.actFFratio * (1 - burnerFlueExhFract);
           return flue;
       }
       else return 0;
   }

   public double fuelSensibleHeat() {
       return fuelFiring.fuelSensibleHeat * secFuelFlow;
   }

   public double fuelEtcFromHeat(double heat, double flueExitTemp) {
       FuelHeatDetails fuelHDet = fuelFiring.effFuelCalVal(flueExitTemp);
       secFuelFlow = heat / fuelHDet.effctiveCalVal;   //realCalVal;
       double combustionFlue =  secFuelFlow * fuelFiring.actFFratio;
       secFlueFlow = combustionFlue * (1 - burnerFlueExhFract);
       tempFlueOut = flueExitTemp;
       airSensible = secFuelFlow * fuelHDet.airHeat;
       heatToSecFlue = secFuelFlow * fuelHDet.lossToFlue; // flueHeat;
       combustionHeat = secFuelFlow * fuelInSection.calVal;
       totalFlueExitHeat = heatPassingFlueOut + heatToSecFlue;
       regenFlue =  combustionFlue * burnerFlueExhFract;
       regenFlueHeat = heatToSecFlue * regenFlue / combustionFlue;
       fceFlue = secFlueFlow;
       fceFlueHeat = heatToSecFlue - regenFlueHeat;
       return secFlueFlow;
   }

   public double airFlow() {
       return secFuelFlow * fuelFiring.actAFRatio;
   }

   void updateUI() {
       if (tuning.bSectionProgress)
            furnace.updateUI();
   }


    public FceEvaluator.EvalStat oneSectionInRev() {
        FceEvaluator.EvalStat response = FceEvaluator.EvalStat.OK;
        UnitFurnace theSlot, prevSlot, nextSlot;
        boolean bLastSlot;
        theSlot = vUnitFurnaces.get(lastSlot);
        for (int slot = lastSlot; slot >= firstSlot; slot--) {
            bLastSlot = (slot == lastSlot);
            prevSlot = vUnitFurnaces.get(slot - 1);
            nextSlot = vUnitFurnaces.get(slot + 1);
            response = theSlot.evalInRev(bLastSlot, prevSlot, (bLastSlot) ? lastRate :nextSlot.lastDeltaT );
            if (response != FceEvaluator.EvalStat.OK)
                break;
            theSlot = prevSlot;
        }
        if (response == FceEvaluator.EvalStat.OK) {
            if(bRecuType)
                reCheckLosses();
            heatToCharge();
        }
        updateUI();
        return response;
     }


    // evaluate the section to reach the charge entry temperature as specified
    // this is valid only for burner sections
    public FceEvaluator.EvalStat oneSectionInRev(double entryTemp) {
        FceEvaluator.EvalStat response = FceEvaluator.EvalStat.OK;
        UnitFurnace theSlot, prevSlot, nextSlot;
        boolean done = false;
        double nowTempG, newTempG;
        double nowEntryTemp;
        boolean bLastSlot;
        UnitFurnace lastUnit = vUnitFurnaces.get(lastSlot);
        double exitTemp = lastUnit.tempWmean;
        double deltaTreqd = (exitTemp - entryTemp);
        double chSecMeanReqd = entryTemp + deltaTreqd / 2;
        double deltaTnow;
        double trialDeltaT = (controller.tuningParams.suggested1stCorrection > 0) ?
                controller.tuningParams.suggested1stCorrection : 10;
 //    showOneResult (lastSlot) ' starting conditions
        while (!done) {

            bLastSlot = true;
            theSlot = vUnitFurnaces.get(lastSlot);
            for (int slot = lastSlot; slot >= firstSlot; slot--) {
                prevSlot = vUnitFurnaces.get(slot - 1);
                nextSlot = vUnitFurnaces.get(slot + 1);
                response = theSlot.evalInRev(bLastSlot, prevSlot, (bLastSlot) ? lastRate :nextSlot.lastDeltaT );
                bLastSlot = false;
                if (response != FceEvaluator.EvalStat.OK)
                    break;
                theSlot = prevSlot;
            }
            if (response == FceEvaluator.EvalStat.TOOHIGHGAS) {
                lastUnit.tempG -= trialDeltaT;
                trialDeltaT *= 2;
                continue;
            }
            if (response != FceEvaluator.EvalStat.OK)
                break;
            nowTempG = lastUnit.tempG;
            nowEntryTemp = vUnitFurnaces.get(firstSlot - 1).tempWmean;
            if (Math.abs(nowEntryTemp - entryTemp)  < 0.2)
                done = true;
            else {
               // required change in tempG
                deltaTnow = exitTemp - nowEntryTemp;
                // assume Alpha is unchanged
//                newTempG = (chSecMeanReqd + deltaTreqd / deltaTnow * (nowTempG - (exitTemp - deltaTnow / 2)) + nowTempG) / 2;
                double gasTChange =  (chSecMeanReqd + deltaTreqd / deltaTnow * (nowTempG - (exitTemp - deltaTnow / 2))) - nowTempG;
                newTempG = gasTChange / 3 + nowTempG;
//                newTempG = (chSecMeanReqd + deltaTreqd / deltaTnow * (nowTempG - (exitTemp - deltaTnow / 2)) + nowTempG) / 2;
                // take Radiation relation

                lastUnit.tempG = newTempG;

                presetGasTemp = newTempG;
            }
        }
        if (response == FceEvaluator.EvalStat.OK) {
            if(bRecuType)
                reCheckLosses();
            heatToCharge();
        }
        updateUI();
        return response;
    }

    public FceEvaluator.EvalStat oneSectionInFwd() {
        FceEvaluator.EvalStat response = FceEvaluator.EvalStat.OK;
        double two;
        UnitFurnace theSlot, prevSlot;
        prevSlot = vUnitFurnaces.get(firstSlot - 1);
        two = prevSlot.chargeSurfTemp();
        if (firstSlot > 1) prevSlot.tempWO = two;
        prevSlot.showResult();
//        showOneResult (firstSlot - 1) ' starting conditions
        for (int iSlot = firstSlot; iSlot <= lastSlot; iSlot++){
            theSlot = vUnitFurnaces.get(iSlot);
            response = theSlot.evalInFwd((iSlot == firstSlot), prevSlot);
            if (response != FceEvaluator.EvalStat.OK)
                break;
            prevSlot = theSlot;
        }
        if (response == FceEvaluator.EvalStat.OK)
            heatToCharge();
        updateUI();
        return response;
    }

    public FceEvaluator.EvalStat oneSectionInFwdWithWallRadiation() {
        FceEvaluator.EvalStat response = FceEvaluator.EvalStat.OK;
        UnitFurnace theSlot, prevSlot;
        prevSlot = vUnitFurnaces.get(firstSlot - 1);
        prevSlot.showResult();
        for (int iSlot = firstSlot; iSlot <= lastSlot; iSlot++){
            theSlot = vUnitFurnaces.get(iSlot);
            response = theSlot.evalWithWallRadiationInFwd((iSlot == firstSlot), prevSlot);
            if (response != FceEvaluator.EvalStat.OK)
                break;
            prevSlot = theSlot;
        }
        if (response == FceEvaluator.EvalStat.OK)
            heatToCharge();
        updateUI();
        return response;
    }

    public void smoothenProfile() {
        UnitFurnace ufFirst =vUnitFurnaces.get(firstSlot - 1);
        UnitFurnace ufPrev = vUnitFurnaces.get(firstSlot - 2);
        ufFirst.mergeSlots(ufPrev);
    }

    Vector <FceAmbient> addAmbientData(Vector<FceAmbient> ambientData) {
        UnitFurnace uf;
        double stTime, gasTemp, alpha;
        if (isActive()) {
            for (int u = firstSlot; u <= lastSlot; u++) {
                uf = vUnitFurnaces.get(u);
                ambientData.add(new FceAmbient(uf.startTime(), uf.avgGasTemp(),uf.getAlpha()));
            }
        }
        return ambientData;
    }

    void getObservations(Observations observations) {
        if (isActive() && !bRecuType) {
            if (secFuelFlow < 0)
                observations.add("Fuel flow in " + sectionName() + " is negative at " +
                                        (new DecimalFormat("#,##0.00")).format(secFuelFlow));
        }
    }

    void showMessage(String msg) {
        JOptionPane.showMessageDialog(controller.parent(), msg, "FOR INFORMATION", JOptionPane.INFORMATION_MESSAGE);
        controller.parent().toFront();
    }

    void showError(String msg) {
        JOptionPane.showMessageDialog(controller.parent(), msg, "ERROR", JOptionPane.ERROR_MESSAGE);
        controller.parent().toFront();
    }

    void showError(String msg, int forTime) {
        JOptionPane pane = new JOptionPane(msg, JOptionPane.ERROR_MESSAGE);
        JDialog dialog = pane.createDialog(controller.parent(), "ERROR");
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new CloseDialogTask(dialog), forTime);
        dialog.setVisible(true);
    }

    //implement your CloseDialogTask:

    class CloseDialogTask extends TimerTask {
        JDialog dlg;
        CloseDialogTask(JDialog dlg) {
            this.dlg = dlg;
        }

        public void run() {
            dlg.setVisible(false);
        }
    }

    boolean decide(String title, String msg) {
        int resp = JOptionPane.showConfirmDialog(controller.parent(), msg, title, JOptionPane.YES_NO_OPTION);
        controller.parent().toFront();
        return resp == JOptionPane.YES_OPTION;
    }

    void setTCLocation() {
        if (tcLocationFromZoneStart <= 0 || tcLocationFromZoneStart > sectionLength()) {
            tcLocationFromZoneStart = sectionLength() * 0.6;
            tfTCLocation.setData(tcLocationFromZoneStart * 1000);
        }
    }

    void setTCLocationLimits()  {
        tfTCLocation.setLimits(0, sectionLength() * 1000);
    }

    class SummButtActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            getSummaryData((Component) e.getSource());
        }
    }

    class SectionSummaryDlg extends JDialog {
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ActionListener li;
        Frame parent;

        SectionSummaryDlg(Frame parent) {
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
                            takeValuesFromUI();
                            closeThisWindow();
                        }
                    } else if (src == cancel) {
                        setValuesToUI();
                        setComboBoxes();
                        closeThisWindow();
                    }
                }
            };
            ok.addActionListener(li);
            cancel.addActionListener(li);
            Container dlgP = getContentPane();
            JPanel jp = new JPanel(new GridBagLayout());
            GridBagConstraints gbcL = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.EAST, 0,
                    new Insets(1, 0, 1, 2), 0, 0);
            GridBagConstraints gbcR = new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.WEST, 0,
                    new Insets(1, 2, 1, 0), 0, 0);
            GridBagConstraints gbc = new GridBagConstraints(0, 0, 2, 1, 1, 1, GridBagConstraints.CENTER, 0,
                    new Insets(2, 0, 10, 0), 0, 0);
            jp.add(new JLabel(sectionName()), gbc);

            gbcL.gridy++;
            gbcR.gridy++;
            jp.add(new JLabel("Section Type "), gbcL);
            jp.add(cbSecType, gbcR);

            gbcL.gridy++;
            gbcR.gridy++;
            jp.add(new JLabel("Fuel Choice "), gbcL);
            jp.add(cbFuelChoice, gbcR);

            gbcL.gridy++;
            gbcR.gridy++;
            jp.add(new JLabel("Section Fuel "), gbcL);
            jp.add(cbFuels, gbcR);
            cbFuels.updateUI();
            gbcL.gridy++;
            gbcR.gridy++;
            jp.add(tfExcessAir.getLabel(), gbcL);
            jp.add(tfExcessAir, gbcR);

            gbcL.gridy++;
            gbcR.gridy++;
            jp.add(tfFuelTemp.getLabel(), gbcL);
            jp.add(tfFuelTemp, gbcR);

            gbcL.gridy++;
            gbcR.gridy++;
            jp.add(new JLabel("Burner Type "), gbcL);
            jp.add(cbBurnerType, gbcR);

            gbcL.gridy++;
            gbcR.gridy++;
            jp.add(tfFlueExhPercent.getLabel(), gbcL);
            jp.add(tfFlueExhPercent, gbcR);

            gbcL.gridy++;
            gbcR.gridy++;
            jp.add(new JLabel("Air Heat with Common Recu"), gbcL);
            jp.add(chkAPHCommonRecu, gbcR);

            gbcL.gridy++;
            gbcR.gridy++;
            jp.add(tfRegenPHtemp.getLabel(), gbcL);
            jp.add(tfRegenPHtemp, gbcR);

            gbcL.gridy++;
            gbcR.gridy++;
            jp.add(new JLabel(""), gbcL);

            gbcL.gridy++;
            gbcR.gridy++;
            setTCLocationLimits();
            jp.add(tfTCLocation.getLabel(), gbcL);
            jp.add(tfTCLocation, gbcR);

            gbcL.gridy++;
            gbcR.gridy++;
            jp.add(new JLabel(""), gbcL);

            gbcL.gridy++;
            gbcR.gridy++;
            jp.add(cancel, gbcL);
            jp.add(ok, gbcR);

            dlgP.add(jp);
            enableComponents();
        }

        boolean isInError() {
            boolean retVal = false;
            boolean stat;
            String msg = "ERROR : ";
            if (cbSecType.getSelectedIndex() == 1) {
                if (stat = tfExcessAir.isInError()) {
                    msg += "\n   Check Excess Air %";
                    retVal |= stat;
                }
                if (cbBurnerType.getSelectedIndex() == 1) {
                    if (stat = (tfFlueExhPercent.isInError() || tfFlueExhPercent.getData() < 0)) {
                        msg += "\n   Check % flue Exh through Burner";
                        retVal |= stat;
                    }
                    else if (tfFlueExhPercent.getData() == 0) {
                        retVal |= !decide("Regen Burner", "Flue through Burner is 0% ?");
                    }
                    if (stat = tfRegenPHtemp.isInError()) {
                        msg += "\n   Check Air Preheat";
                        retVal |= stat;
                    }
                }
                if (cbFuelChoice.getSelectedIndex() == 1) {
                    if (cbFuels.getSelectedIndex() < 0) {
                        msg += "\n Section Fuel is NOT selected!";
                        retVal = true;
                    }
                    else {
                        double temp = tfFuelTemp.getData();
                        if (temp != 0) {
                            Fuel f = (Fuel)cbFuels.getSelectedItem();
                            if (!f.isSensHeatSpecified(controller, temp)) {
                                f.getSpHtData(controller, tfFuelTemp);
                            }
                        }
                    }
                }
            }
            if (retVal) {
                controller.enableNotify(false);
                JOptionPane.showMessageDialog(this, msg, "Section Details", JOptionPane.ERROR_MESSAGE);
                controller.enableNotify(true);
                controller.parent().toFront();
            }
            return retVal;
        }

        void closeThisWindow() {
            setVisible(false);
            dispose();
        }
    }
 }
