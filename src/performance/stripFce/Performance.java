package performance.stripFce;

import basic.*;
import directFiredHeating.DFHFurnace;
import directFiredHeating.DFHeating;
import display.*;
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
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimerTask;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 1/27/14
 * Time: 10:31 AM
 * To change this template use File | Settings | File Templates.
 */

public class Performance {
    public static final int STRIPWIDTH = 2;
    public static final int STRIPTHICK = 4;
    public static final int MATERIAL = 8;
    public static final int FUEL = 16;
    public static final int UNITOUTPUT = 32;
    public static final int EXITTEMP = 64;
    public double output;
    public double unitOutput; // output per stripWidth
    public Vector<OneZone> topZones, botZones;
    GregorianCalendar dateOfResult;
    String fuelName;
    double airTemp;
    double chLength, chWidth, chThick;
    double chWt;
    double chPitch;
    double speed;
    double piecesPerH;
    public String chMaterial;
    DFHeating controller;
    DFHFurnace furnace;
    PerformanceTable perfTable;
    boolean interpolated = false;

    public enum Params {
        DATE("Date of Calculation"),
        CHMATERIAL("Charge Material"),
        STRIPWIDTH("Strip Width"),
        STRIPTHICK("Strip Thickness"),
        STRIPSPEED("Strip Speed"),
        OUTPUT("Output"),
        FUEL("Fuel"),
        AIRTEMP("Air Temperature"),
        // Zonal Data
        FUELFLOW("Fuel Flow"),
        GASTEMP("Gas Temperature"),
        FCETEMP("Zone Temperature"),
        CHTEMPIN("Charge Temperature IN"),
        CHTEMPOUT("Charge Temperature OUT"),
        COMBUSTIOHEAT("Fuel Combustion Heat"),
        FUELSENSIBLE("Fuel Sensible"),
        AIRSENSIBLE("Air Sensible"),
        ZONEFUELHEAT("Zone Fuel Heat"),
        ZONEFLUEHEAT("Zone FlueHeat");

        private final String name;


        Params(String s) {
              this.name = s;
        }

        public String toString() {
              return name;
        }

    }

    public Performance() {

    }

    public Performance(DFHFurnace furnace) {
        this.furnace = furnace;
        this.controller = furnace.controller;
    }

    public Performance(ProductionData production, Fuel fuel, double airTemp, Vector<OneZone> topZones,
                        Vector<OneZone> botZones, GregorianCalendar dateOfResult,
                        DFHFurnace furnace) {
         output = production.production;
         Charge charge = production.charge;
         chMaterial = charge.chMaterial.name;
         chLength = charge.getLength();
         chWidth = charge.getWidth();
         chThick = charge.getHeight();
         chWt = charge.getUnitWt();
         chPitch = production.chPitch;
         speed = production.speed;
         piecesPerH = production.piecesPerh;
         fuelName = fuel.name;
         this.airTemp = airTemp;
         getUnitOutput();
         this.topZones = topZones;
         this.botZones = botZones;
         for (OneZone z: topZones)
             z.setPerformanceOf(this);
         if (botZones != null)  {
             for (OneZone z: botZones)
                 z.setPerformanceOf(this);
         }
         this.dateOfResult = dateOfResult;
         this.furnace = furnace;
         this.controller = furnace.controller;
     }

    public Performance(ProductionData production, Fuel fuel, double airTemp, Vector<OneZone> topZones, GregorianCalendar dateOfResult,
                       DFHFurnace furnace) {
        this(production, fuel, airTemp, topZones, null, dateOfResult, furnace);
    }

    public void addToZones(boolean bBot, OneZone zone) {
        if (bBot) {
            if (botZones == null)
                botZones = new Vector<OneZone>();
            botZones.add(zone);
        }
        else {
            if (topZones == null)
                topZones = new Vector<OneZone>();
            topZones.add(zone);
        }

    }

    double[] outputFactors;
    double[] widthFactors;

    public void setTableFactors(double minOutputFactor, double outputStep, double minWidthFactor, double widthStep) {
//        int nOutput = (int)((1.0 - minOutputFactor) / outputStep) + 1;
        Vector<Double> vOF = new Vector<Double>();
        double of = 1.0; //  + outputStep;
        while (of > minOutputFactor) {
            vOF.add(of);
            of -= outputStep;
            if (Math.abs(of - minOutputFactor) < (outputStep / 4) )
                break;
        }
        vOF.add(minOutputFactor);
        outputFactors = new double[vOF.size()];
        int n = 0;
        for (double o:vOF)
            outputFactors[n++] = o;

        Vector<Double> vWF = new Vector<Double>();
        double wf = 1.0 + widthStep;
        while (wf > minWidthFactor) {
            vWF.add(wf);
            wf -= widthStep;
            if (Math.abs(wf - minWidthFactor) < (widthStep / 4) )
                break;
        }
        vWF.add(minWidthFactor);
        widthFactors = new double[vWF.size()];
        n = 0;
        for (double w:vWF)
            widthFactors[n++] = w * chLength;
   }

    public PerformanceTable getPerformanceTable() {
        return perfTable;
    }

    public void createPerfTable(ThreadController master) {
        controller.setTableFactors(this);
        perfTable = new PerformanceTable(this, outputFactors, widthFactors);
        double forOutput;
        double forWidth;
        Performance onePerf;
        for (double capF:outputFactors) {
            for (double widthF:widthFactors) {
                if (widthF <= chLength || capF <= 1)  {
                    forWidth = widthF; // chLength * widthF;
                    forOutput = unitOutput * capF * forWidth;
                    if (furnace.evaluate(master, forOutput, forWidth)) {
                        onePerf = furnace.getPerformance();
                        if (onePerf == null)
                            showError("The data for " + forWidth + " not saved to the table");
                        else
                            perfTable.addToTable(widthF, capF, onePerf);
                    }
                }
            }
        }
        controller.enableDataEdit();
    }

    void getUnitOutput() {
        unitOutput = output / chLength;
    }

    OneZone getOneZone(int nZone, boolean bBot) {
        Vector<OneZone> zones = (bBot) ? botZones : topZones;
        OneZone zone = null;
        if ((nZone >= 0) && (zones != null) && (nZone < zones.size()))
            zone = zones.get(nZone);
        return zone;
    }

    double getNumberParam(Params param) {
        double retVal = -1;
        switch(param) {
            case OUTPUT:
                retVal = output;
                break;
            case STRIPSPEED:
                retVal = output / chWt * chPitch;
                break;
            case STRIPTHICK:
                retVal = chThick;
                break;
            case STRIPWIDTH:
                retVal = chLength;
                break;
            case AIRTEMP:
                retVal = airTemp;
                break;
            case CHTEMPOUT:
                retVal = exitTemp();
                break;
        }
        return retVal;
    }

    String getStringParam(Params param) {
        String retVal = "N/A";
        switch(param) {
            case FUEL:
                retVal = fuelName;
                break;
            case CHMATERIAL:
                retVal = chMaterial;
                break;
            case DATE:
                retVal = dateStr();
                break;
        }
        return retVal;
    }

    public double getOutput() {
        return output;
    }

    boolean isProductionComparable(Performance performance, double exitTAllowance) {
        boolean bComparable = (chLength == performance.chLength) &&
                chMaterial.equals(performance.chMaterial) &&
                    fuelName.equals(performance.fuelName) &&
                        (Math.abs((output - performance.output) / output) < 0.01) &&
                            (Math.abs(exitTemp() - performance.exitTemp()) < exitTAllowance);
        return bComparable;
    }

    /**
     *
     * @param nowChMaterial
     * @param nowExitTemp
     * @param allowance     delta allowance in exit temperature
     * @return
     */

    boolean isProductionComparable(ChMaterial nowChMaterial, double nowExitTemp, double allowance) {
        boolean bComparable = chMaterial.equals(nowChMaterial.name);
        bComparable &= (Math.abs(exitTemp() - nowExitTemp) < allowance);
        return bComparable;
    }

    // @TODO - to be REMOVED
    boolean isProductionComparable(ChMaterial nowChMaterial, double nowExitTemp) {
        return isProductionComparable(nowChMaterial,nowExitTemp, 1.0);
    }


    boolean isProductionComparable(ProductionData withProduction, Fuel withFuel, int compTypeFlags, double exitTAllowance) {
        Charge nowCharge = withProduction.charge;
        boolean bComparable = true;
        if ((compTypeFlags & STRIPWIDTH) == STRIPWIDTH)
            bComparable &= (chLength == nowCharge.getLength());
        if ((compTypeFlags & STRIPTHICK) == STRIPTHICK)
            bComparable &= (chThick == nowCharge.getHeight());
        if ((compTypeFlags & MATERIAL) == MATERIAL)
            bComparable &= chMaterial.equals(nowCharge.chMaterial.name);
        if ((compTypeFlags & FUEL) == FUEL)
            bComparable &= fuelName.equals(withFuel.name);
        if ((compTypeFlags & UNITOUTPUT) == UNITOUTPUT)   // allow 1% difference
            bComparable &= (Math.abs((unitOutput - (withProduction.production / nowCharge.length)) / unitOutput) < 0.01);
        if ((compTypeFlags & EXITTEMP) == EXITTEMP)   // allow 1 deg difference
            bComparable &= (Math.abs(exitTemp() - withProduction.exitTemp) < exitTAllowance);
        return bComparable;
    }

    public double exitTemp() {
        return topZones.get(topZones.size() - 1).stripTempOut;
    }

    public int nZones(boolean bBot) {
        int nZones = 0;
        Vector zones = (bBot) ? botZones : topZones;
        if (zones != null)
            nZones = zones.size();
        return nZones;
    }

    boolean getSuggestedFuels(double forOutput, double[] zoneFuelSuggestion)  {
        double fuelNow, fuelRef;
        double cumFuelNow = 0, cumFuelRef = 0;
        double totHeat, heatPassFlue, balanceHeat;
        OneZone zoneRef;
        for (int z = topZones.size() - 1; z >= 0; z--) {
            zoneRef = topZones.get(z);
            heatPassFlue = (cumFuelRef > 0)? (cumFuelNow / cumFuelRef * zoneRef.heatFromPassingFlue) : 0;
            totHeat = zoneRef.losses + zoneRef.heatToCharge / output * forOutput;
            balanceHeat = totHeat - heatPassFlue;
            fuelRef = zoneRef.fuelFlow;
            if (fuelRef > 0) {
                fuelNow = balanceHeat / zoneRef.netHeatDueToFuel * fuelRef;
                if (fuelNow <= 0) {
                    showError("Fuel for Zone " + (z + 1) + " works out be 0 or negative, taking as 0.1");
                    fuelNow = 0.1;
                }
                cumFuelNow += fuelNow;
                cumFuelRef += fuelRef;
                zoneFuelSuggestion[z] = fuelNow;
            }
            else
                zoneFuelSuggestion[z] = 0;
        }
        return true;
    }

    int getChInTempProfile(double[] chTempInProfile) {
        int retVal = topZones.size();
        int zNum = 0;
        if (retVal <= chTempInProfile.length) {
            for (OneZone z: topZones)
                chTempInProfile[zNum++] = z.stripTempIn;
        }
        else
            retVal = 0;
        return retVal;
    }

    int getChInTempProfile(double[] chTempInProfile, double forExitTemp) {
        double tempIn = topZones.get(0).stripTempIn;
        double baseTempOut = topZones.get(topZones.size() - 1).stripTempOut;
        double adjustFactor = (forExitTemp - tempIn) / (baseTempOut - tempIn);
        int retVal = topZones.size();
        int zNum = 0;
        if (retVal <= chTempInProfile.length) {
            for (OneZone z: topZones)
                chTempInProfile[zNum++] = tempIn + (z.stripTempIn - tempIn) * adjustFactor;
        }
        else
            retVal = 0;
        return retVal;
    }

    /**
     * The adjusted temperature profile with the exit temp of the first fired section kept unchanged
     * @param chTempInProfile
     * @param forExitTemp
     * @param firstFiredSec
     * @return
     */
    int getChInTempProfile(double[] chTempInProfile, double forExitTemp, int firstFiredSec) {
         double baseTempIn = topZones.get(firstFiredSec + 1).stripTempIn;
         double baseTempOut = topZones.get(topZones.size() - 1).stripTempOut;
         double adjustFactor = (forExitTemp - baseTempIn) / (baseTempOut - baseTempIn);
         int retVal = topZones.size();
         int zNum = 0;
         if (retVal <= chTempInProfile.length) {
             for (OneZone z: topZones) {
                 if (zNum > (firstFiredSec + 1))
                    chTempInProfile[zNum] = baseTempIn + (z.stripTempIn - baseTempIn) * adjustFactor;
                 else
                     chTempInProfile[zNum] = z.stripTempIn;
                 zNum++;
             }
         }
         else
             retVal = 0;
         return retVal;
     }


    String stripSize() {
        return "" + chLength * 1000 + " x " + chThick * 1000;
    }

    public String toString() {
        DecimalFormat tempFmt = new DecimalFormat("#,##0");
        if (chMaterial != null)
            return chMaterial +
                ", size " + (chLength* 1000) + " x " + (chThick * 1000) +  " to " + tempFmt.format(exitTemp()) +
                " at " + (output / 1000) + " t/h with " + fuelName + " as fuel";
        else
            return super.toString();
    }

    String dateStr() {
        if (interpolated)
            return "Interpolated";
        else
            return formatter.format(dateOfResult.getTime());
    }

    public StringBuilder dataInXML() {
        StringBuilder xmlStr = new StringBuilder(XMLmv.putTag("ChMaterialP", chMaterial));
        if (interpolated)  {
            xmlStr.append((XMLmv.putTagNew("Interpolated", 1)));
            xmlStr.append(XMLmv.putTagNew("dateOfResult", "Interpolated"));
        }
        else
            xmlStr.append(XMLmv.putTagNew("dateOfResult", dateOfResult.getTime().getTime()));
        xmlStr.append(XMLmv.putTagNew("chWidthP", chWidth));
        xmlStr.append(XMLmv.putTagNew("chLengthP", chLength));
        xmlStr.append(XMLmv.putTagNew("chThickP", chThick));
        xmlStr.append(XMLmv.putTagNew("chPitchP", chPitch));
        xmlStr.append(XMLmv.putTagNew("chWtP", chWt));
        xmlStr.append(XMLmv.putTagNew("outputP", output));
        xmlStr.append(XMLmv.putTagNew("airTempP", airTemp));
        xmlStr.append(XMLmv.putTagNew("speed", speed));
        xmlStr.append(XMLmv.putTagNew("unitOutputP", unitOutput));
        xmlStr.append(XMLmv.putTagNew("fuelNameP", fuelName));
        xmlStr.append(XMLmv.putTagNew("nZones", topZones.size()));
        int z = 0;
        for (OneZone zone: topZones)
            xmlStr.append(XMLmv.putTagNew("Zone" + ("" + z++).trim(), zone.dataInXML()));
        if (botZones != null && botZones.size() > 0) {
            StringBuilder bZone = new StringBuilder(XMLmv.putTagNew("nBotZones", botZones.size()));
            z = 0;
            for (OneZone zone: botZones)
                bZone.append(XMLmv.putTagNew("bZone" + ("" + z++).trim(), zone.dataInXML()));
            xmlStr.append(XMLmv.putTagNew("botZones", bZone));
            }
        if (perfTable != null)
            xmlStr.append(XMLmv.putTagNew("PerfTable", perfTable.dataInXML()));
        return xmlStr;
    }

    public boolean  takeDataFromXML(String xmlStr) {
        boolean bRetVal = true;
        ValAndPos vp;
        try {
            vp = XMLmv.getTag(xmlStr, "Interpolated", 0);
            interpolated = (vp.val.equals("1"));
            if (!interpolated) {
                vp = XMLmv.getTag(xmlStr, "dateOfResult", 0);
                if (vp.val.length() > 0) {
                    dateOfResult = new GregorianCalendar();
                    dateOfResult.setTime(new Date(Long.valueOf(vp.val)));
                }
                else {
                    dateOfResult = new GregorianCalendar(2000, 0, 1);
                }
            }
            vp = XMLmv.getTag(xmlStr, "ChMaterialP", 0);
            chMaterial = vp.val;
            vp = XMLmv.getTag(xmlStr, "chWidthP", 0);
            chWidth = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "chLengthP", 0);
            chLength = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "chThickP", 0);
            chThick = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "chPitchP", 0);
            chPitch = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "chWtP", 0);
            chWt = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "outputP", 0);
            output = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "airTempP", 0);
            if (vp.val.length() > 0)
                airTemp = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "speed", 0);
            if (vp.val.length() > 0)
                speed = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "fuelNameP", 0);
            fuelName = vp.val;
            int nZones = 0;
            vp = XMLmv.getTag(xmlStr, "nZones", 0);
            if (vp.val.length() > 0)
                nZones = Integer.valueOf(vp.val);
            if (nZones > 2) {
//                topZones = new Vector<OneZone>();
                for (int z = 0; z < nZones; z++) {
                    OneZone zone = new OneZone();
                    vp = XMLmv.getTag(xmlStr, "zone" + ("" + z).trim(), 0);
                    if (!zone.takeDataFromXML(vp.val)) {
                        showError("takeDataFromXML: Some Problem in reading Performance Data for Top Zones. Data considered as invalid", 10000);
                        bRetVal = false;
                        break;
                    }
                    zone.setPerformanceOf(this);
                    addToZones(false, zone);
//                    topZones.add(zone);
                }
                vp = XMLmv.getTag(xmlStr, "botZones", vp.endPos);
                if (vp.val.length() > 0) {
                    vp = XMLmv.getTag(xmlStr, "nBotZones", 0);
                    if (vp.val.length() > 0)
                        nZones = Integer.valueOf(vp.val);
                    if (nZones > 2) {
//                        botZones = new Vector<OneZone>();
                        for (int z = 0; z < nZones; z++) {
                            OneZone zone = new OneZone();
                            vp = XMLmv.getTag(xmlStr, "bZone" + ("" + z).trim(), 0);
                            if (!zone.takeDataFromXML(vp.val)) {
                                showError("takeDataFromXML: Some Problem in reading Performance Data for Bottom Zones. Data considered as invalid", 3000);
                                bRetVal = false;
                                break;
                            }
                            zone.setPerformanceOf(this);
                            addToZones(true, zone);
//                            botZones.add(zone);
                        }
                    }
                }
                getUnitOutput();
                vp = XMLmv.getTag(xmlStr, "PerfTable", vp.endPos);
                if (vp.val.length() > 20) {
                    try {
                        perfTable = new PerformanceTable(this, vp.val);
                    } catch (Exception e) {
                        showError("takeDataFromXML: Facing some problem is loading Performance table \n" + e.getMessage());
                        perfTable = null;
                    }
                }
            }
            else {
                showError("takeDataFromXML: Number of zones in Performance data is less than 3, Data is considered as invalid", 3000);
                bRetVal = false;
            }
        }
        catch (Exception e) {
            bRetVal = false;
        }
        return bRetVal;
    }


    public JPanel performanceP() {
        JPanel outerP = new FramedPanel(new BorderLayout());
        fillPerformanceP(this);
        JPanel performancePanel = new FramedPanel(new BorderLayout());
//        outerP.add(perfSummaryPanel, BorderLayout.EAST);
        JPanel selectedPerfP = new FramedPanel(new BorderLayout());
         selectedPerfP.add(perfSummaryPanel, BorderLayout.SOUTH);
        selectedPerfP.setPreferredSize(selectedPerfP.getPreferredSize());
        if (perfTable != null) {
            selectedPerfP.add(perfTable.tableSelPanel(), BorderLayout.NORTH);
            performancePanel.add(perfTable.perfTableP(), BorderLayout.NORTH);
            performancePanel.add(fuelFlowProfilePanel, BorderLayout.SOUTH);
            outerP.add(performancePanel, BorderLayout.WEST);
            speedQueryPanel = speedQueryP();
        }
        outerP.add(selectedPerfP, BorderLayout.EAST);
        return outerP;
    }

    JPanel speedQueryPanel;
    ZonalFuelProfile fuelP;

    JPanel speedQueryP() {
        JButton jbFromTotal = new JButton("<-Fuel Flow       Speed in mpm->");
        final NumberTextField ntTotFlow = new NumberTextField(controller, 0, 6, false, 0, 1000, "#,###", "Total Fuel Flow");
        final NumberLabel nlSpeed = new NumberLabel(0, 100, "#,###.##");
        jbFromTotal.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                double flow = ntTotFlow.getData();
                if (flow > 0) {
                    nlSpeed.setData(fuelP.recommendedSpeed(flow, false));
                }
            }
        });
        JButton jbFromTotalFH = new JButton("<-Fuel Heat       Speed in mpm->");
        final NumberTextField ntTotFH = new NumberTextField(controller, 0, 6, false, 0, 1e8, "#,###", "Total Fuel Heat");
        final NumberLabel nlSpeedFH = new NumberLabel(0, 100, "#,###.##");
        jbFromTotalFH.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                double fh = ntTotFH.getData();
                if (fh > 0) {
                    nlSpeedFH.setData(fuelP.recommendedSpeedOnFuelHeat(fh, false));
                }
            }
        });
        JPanel totP = new JPanel(new BorderLayout());
        totP.add(ntTotFlow, BorderLayout.WEST);
        totP.add(jbFromTotal, BorderLayout.CENTER);
        totP.add(nlSpeed, BorderLayout.EAST);
        JPanel totPFH = new JPanel(new BorderLayout());
        totPFH.add(ntTotFH, BorderLayout.WEST);
        totPFH.add(jbFromTotalFH, BorderLayout.CENTER);
        totPFH.add(nlSpeedFH, BorderLayout.EAST);
//        JPanel zoneP = new JPanel();
//        JButton jbFromZonal = new JButton("get");
//        final Vector<NumberTextField> vNtf = new Vector<NumberTextField>();
//        NumberTextField ntf;
//        for (int z = 0; z < topZones.size(); z++) {
//            ntf = new NumberTextField(controller, 0, 6, false, 0, 1000, "###.##", ("f" + z));
//            vNtf.add(ntf);
//            zoneP.add(ntf);
//        }
//        zoneP.add(jbFromZonal);
//        final NumberLabel minL = new NumberLabel(0, 60, "###.##");
//        final NumberLabel maxL = new NumberLabel(0, 60, "###.##");
//        zoneP.add(minL);
//        zoneP.add(maxL);
//
//        final double[] fuels = new double[vNtf.size()];
//        jbFromZonal.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                for (int z = 0; z < vNtf.size(); z++)
//                    fuels[z] = vNtf.get(z).getData();
//                DoubleRange speedRange = fuelP.recommendedSpeedRange(fuels, false);
//                minL.setData(speedRange.min);
//                maxL.setData(speedRange.max);
//            }
//        });
//
        JPanel speedQP = new FramedPanel(new BorderLayout());
        speedQP.add(totP, BorderLayout.NORTH);
        speedQP.add(totPFH, BorderLayout.SOUTH);
//        speedQP.add(zoneP, BorderLayout.SOUTH);
        return speedQP;
    }

    JPanel getSpeedQueryPanel(ZonalFuelProfile fProfile) {
        fuelP = fProfile;
        return speedQueryPanel;
    }

    JPanel fuelFlowProfilePanel = new FramedPanel(new BorderLayout());

    JPanel perfSummaryPanel = new FramedPanel(new GridBagLayout());

    void fillFuelProfile(ZonalFuelProfile fuelProfile) {
        fuelFlowProfilePanel.removeAll();
        fuelFlowProfilePanel.add(getSpeedQueryPanel(fuelProfile), BorderLayout.NORTH);
        fuelFlowProfilePanel.add(fuelProfile.fuelFlowCharacteristic(false), BorderLayout.CENTER);
        fuelFlowProfilePanel.updateUI();
    }

    void greyPerformanceP() {
        perfSummaryPanel.removeAll();
        perfSummaryPanel.updateUI();
    }

    void fillPerformanceP(Performance selP) {
        perfSummaryPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        perfSummaryPanel.add(selP.commonPerfDataP(), gbc);
        gbc.gridy++;
        perfSummaryPanel.add(selP.zonesP(), gbc);
        perfSummaryPanel.setVisible(true);
        perfSummaryPanel.updateUI();
     }

    JPanel zonesP() {
        JPanel outerP = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        outerP.add(OneZone.getRowHeader(), gbc);
        gbc.gridx++;
        int zNum = 0;
        for (OneZone z:topZones) {
            zNum++;
            outerP.add(z.zoneDataPanel(furnace.topBotName(false) + "Z#" + zNum), gbc);
            gbc.gridx++;
        }
        if (botZones != null && botZones.size() > 0) {
            gbc.gridy++;
            gbc.gridx = 0;
            outerP.add(OneZone.getRowHeader(), gbc);
            zNum = 0;
            for (OneZone z:botZones) {
                zNum++;
                outerP.add(z.zoneDataPanel("BotZ#" + zNum), gbc);
                gbc.gridx++;
            }
        }
        return outerP;
    }


    DateFormat formatter = new SimpleDateFormat("HH:MM, dd-MMM-yyyy");

    JPanel commonPerfDataP() {
        MultiPairColPanel pan = new MultiPairColPanel("");
        addItemPair(pan, Params.DATE);
        addItemPair(pan, Params.CHMATERIAL);
        addItemPair(pan, Params.STRIPWIDTH, 1000, "#,##0 mm");
        addItemPair(pan, Params.STRIPTHICK, 1000, "#,##0.00 mm");
        addItemPair(pan, Params.STRIPSPEED, (1.0 / 60), "#,##0.000 m/min");
        addItemPair(pan, Params.CHTEMPOUT, 1, "#,##0 C");
        addItemPair(pan, Params.OUTPUT, (1.0 / 1000), "#,##0.00 t/h");
        addItemPair(pan, Params.FUEL);
        addItemPair(pan, Params.AIRTEMP, 1, "#,##0 C");
        return pan;
    }

    void addItemPair(MultiPairColPanel pan, Params param) {
        pan.addItemPair("" + param + ": ", getStringParam(param), false);
    }

    void addItemPair(MultiPairColPanel pan, Params param, double factor, String format) {
        pan.addItemPair("" + param + ": ", getNumberParam(param) * factor, format);

    }

    boolean hasPerfTable() {
        return (perfTable != null);
    }

    static FramedPanel rowHead;
    static Dimension colHeadSize = new JTextField("Flow of flue passing (m3N/h)", 20).getPreferredSize();

    static SizedLabel sizedLabel(String name, Dimension d, boolean bold) {
        return new SizedLabel(name, d, bold);
    }

    static SizedLabel sizedLabel(String name, Dimension d) {
        return sizedLabel(name, d, false);
    }

    void showError(String msg) {
         JOptionPane.showMessageDialog(controller.parent(), msg, "ERROR", JOptionPane.ERROR_MESSAGE);
         controller.parent().toFront();
     }

    void showError(String msg, int forTime) {
        JOptionPane pane = new JOptionPane("Performance:" + msg, JOptionPane.ERROR_MESSAGE);
        JDialog dialog = pane.createDialog(controller.parent(), "ERROR");
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new CloseDialogTask(dialog), forTime);
        dialog.setVisible(true);
    }

    class CloseDialogTask extends TimerTask {
        JDialog dlg;
        CloseDialogTask(JDialog dlg) {
            this.dlg = dlg;
        }

        public void run() {
            dlg.setVisible(false);
        }
    }

    void showMessage(String msg) {
        JOptionPane.showMessageDialog(controller.parent(), msg, "FOR INFORMATION", JOptionPane.INFORMATION_MESSAGE);
        controller.parent().toFront();
    }

}
