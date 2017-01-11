package performance.stripFce;

import basic.*;
import directFiredHeating.DFHFurnace;
import directFiredHeating.DFHTuningParams;
import directFiredHeating.DFHeating;
import directFiredHeating.process.OneStripDFHProcess;
import display.*;
import mvUtils.display.*;
import mvUtils.math.DoubleRange;
import mvUtils.math.MoreOrLess;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 1/27/14
 * Time: 10:31 AM
 * To change this template use File | Settings | File Templates.
 */

public class Performance {
    public enum Params {
        DATE("Date of Calculation"),
        PROCESSNAME("Process name"),
        CHMATERIAL("Charge Material"),
        CHEMMFACTOR("Charge Emmissvity Factor"),
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

    enum DataBasis {
        CALCULATED("Calculated"),
        ACQUIRED("Acquired"),
        INERPOLATED("Interpolated") ;
        private final String name;


        DataBasis(String s) {
            this.name = s;
        }

        public String toString() {
            return name;
        }

        public static DataBasis getEnum(String text) {
            DataBasis retVal = null;
            if (text != null) {
                for (DataBasis b : DataBasis.values()) {
                    if (text.equalsIgnoreCase(b.name)) {
                        retVal = b;
                        break;
                    }
                }
            }
            return retVal;
        }
    }
    public static final int STRIPWIDTH = 2;
    public static final int STRIPTHICK = 4;
    public static final int MATERIAL = 8;
    public static final int FUEL = 16;
    public static final int UNITOUTPUT = 32;
    public static final int EXITTEMP = 64;
    public String processName;
    OneStripDFHProcess dfhProcess;
    public double output;
    public double unitOutput; // output per stripWidth
    public Vector<OneZone> topZones, botZones;
    GregorianCalendar dateOfResult;
    String fuelName;
    double airTemp;
    public double chLength, chWidth, chThick;
    double chWt;
    double chPitch;
    double speed;
    double piecesPerH;
    public String chMaterial;
    public double chEmmCorrectionFactor = 1.0;
    DFHeating controller;
    DFHFurnace furnace;
    PerformanceTable perfTable;
    boolean tableToBeRedone = false;
    boolean interpolated = false;
    DataBasis dataBasis = DataBasis.CALCULATED;
    double maxWidth, minWidth, widthStep;
    double maxUnitOutput, minUnitOutput, outputStep;
    boolean bLimitsReady = false;
    JButton updateTableButton;
    public Performance() {

    }

    public Performance(DFHFurnace furnace) {
        this.furnace = furnace;
        this.controller = furnace.controller;
        updateTableButton = new JButton("<html>Update Performance<p>Table</html>");
        updateTableButton.addActionListener(e -> {
            showMessage("<html>Not ready for recalculation of table.<p>" +
                "For the moment, the only option is to delete this Performance Data and ...</html>");
//            furnace.calculateForPerformanceTable(this);
        });
    }

    public Performance(ProductionData production, Fuel fuel, double airTemp, Vector<OneZone> topZones,
                        Vector<OneZone> botZones, GregorianCalendar dateOfResult,
                        DFHFurnace furnace) {
        this(furnace);
        output = production.production;
        Charge charge = production.charge;
        chEmmCorrectionFactor = production.chEmmissCorrectionFactor;
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
//        this.furnace = furnace;
//        this.controller = furnace.controller;
        this.processName = production.processName;
        setDFHPProcess(production.stripProcess);
    }

    public ProductionData getBaseProductionData() {
        Charge ch = new Charge(controller.getSelChMaterial(chMaterial), chLength, chWidth, chThick);
        ProductionData production = new ProductionData(dfhProcess);
        production.setCharge(ch, chPitch);
        production.setProduction(output, 1, dfhProcess.tempDFHEntry, dfhProcess.tempDFHExit, 0.1, 0);
        production.setExitZoneTempData(dfhProcess.getMaxExitZoneTemp(), dfhProcess.getMinExitZoneTemp());
        production.setChEmmissCorrectionFactor(chEmmCorrectionFactor);
        return production;
    }

    public Performance(ProductionData production, Fuel fuel, double airTemp, Vector<OneZone> topZones, GregorianCalendar dateOfResult,
                       DFHFurnace furnace) {
        this(production, fuel, airTemp, topZones, null, dateOfResult, furnace);
    }

    public void setDFHPProcess(OneStripDFHProcess stripProcess) {
        dfhProcess = stripProcess;
    }

    public StatusWithMessage linkToProcess() {
        if (dfhProcess != null)
            return dfhProcess.notePerformance(this);
        else {
            StatusWithMessage retVal = new StatusWithMessage();
            retVal.addErrorMessage("DFH Process details not available in the Performance Data");
            return retVal;
        }
    }

    public StatusWithMessage deleteProcessLink() {
        StatusWithMessage retVal = new StatusWithMessage();
        if (dfhProcess != null)
            retVal = dfhProcess.deletePerformance(this);
        dfhProcess = null;
        return retVal;
    }

    public void markTableToBeRedone() {
        if (perfTable != null)
            tableToBeRedone = true;
    }

    public MoreOrLess.CompareResult isDateOfResultSame(Performance p) {
        return MoreOrLess.compare(dateOfResult, p.dateOfResult);
    }

    public StatusWithMessage setLimits() {
        StatusWithMessage retVal = new StatusWithMessage();
//        OneStripDFHProcess dfhProcess = controller.getStripDFHProcess(processName);
        if (dfhProcess == null)
            retVal.setErrorMessage("Strip Process " + processName + " is not available");
        else {
            minWidth = dfhProcess.minWidth;
            maxWidth = dfhProcess.maxWidth;
            minUnitOutput = dfhProcess.minUnitOutput;
            maxUnitOutput = dfhProcess.maxUnitOutput;
            DFHTuningParams tuning = controller.getTuningParams();
            widthStep = tuning.widthStep;
            outputStep = tuning.outputStep;
            if (minWidth < maxWidth && minUnitOutput < maxUnitOutput) {
                bLimitsReady = true;
                retVal = setTableFactors();
            }
            else
                retVal.setErrorMessage("Error in limits for preparing Performance Table");
        }
        return retVal;
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
    double[] widthList; // in fact, it is width steps

    public boolean setTableFactorsREMOVE(double minOutputFactor, double outputStep, double minWidthFactor, double widthStep) { // TOD to be removed
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
        widthList = new double[vWF.size()];
        n = 0;
        for (double w:vWF)
            widthList[n++] = w * chLength;
        return true;
    }

    public boolean setTableFactorsREMOVE(double outputStep, double widthStep) {  // TODO tobe removed
        OneStripDFHProcess dfhProc = controller.getStripDFHProcess(processName);
        if (dfhProc != null) {
            double minOutputFactor = dfhProc.minOutputFactor();
            double minWidthFactor = dfhProc.minWidthFactor();
//        int nOutput = (int)((1.0 - minOutputFactor) / outputStep) + 1;
            Vector<Double> vOF = new Vector<Double>();
            double of = 1.0; //  + outputStep;
            while (of > minOutputFactor) {
                vOF.add(of);
                of -= outputStep;
                if (Math.abs(of - minOutputFactor) < (outputStep / 4))
                    break;
            }
            vOF.add(minOutputFactor);
            outputFactors = new double[vOF.size()];
            int n = 0;
            for (double o : vOF)
                outputFactors[n++] = o;

            Vector<Double> vWF = new Vector<Double>();
            double wf = 1.0 + widthStep;
            while (wf > minWidthFactor) {
                vWF.add(wf);
                wf -= widthStep;
                if (Math.abs(wf - minWidthFactor) < (widthStep / 4))
                    break;
            }
            vWF.add(minWidthFactor);
            widthList = new double[vWF.size()];
            n = 0;
            for (double w : vWF)
                widthList[n++] = w * chLength;
            return true;
        }
        else
            return false;
    }

    public StatusWithMessage setTableFactors() {
        StatusWithMessage retVal = new StatusWithMessage();
        if (bLimitsReady) {
            double refWidth = chLength;
            double refUnitOutput = output / chLength;
            if (refWidth >= minWidth) {
                if (refWidth <= maxWidth) {
                    double maxUnitOutputAllowed = maxUnitOutput * controller.getTuningParams().unitOutputOverRange;
                    if (refUnitOutput >= minUnitOutput) {
                        if (refUnitOutput <= maxUnitOutputAllowed) {
                            Vector<Double> vOF = new Vector<Double>();
                            double nowOutputFactor = maxUnitOutputAllowed / refUnitOutput;
                            double minUnitOutputFactor = minUnitOutput / refUnitOutput;
                            while (nowOutputFactor > minUnitOutputFactor) {
                                vOF.add(nowOutputFactor);
                                nowOutputFactor -= outputStep;
                                if (Math.abs(nowOutputFactor - minUnitOutputFactor) < (outputStep / 4))
                                    break;
                            }
                            vOF.add(minUnitOutputFactor);
                            outputFactors = new double[vOF.size()];
                            int n = 0;
                            for (double o : vOF)
                                outputFactors[n++] = o;

                            Vector<Double> vWF = new Vector<Double>();
                            double nowWidthFactor = maxWidth / refWidth;
                            double minWidthFactor = minWidth / refWidth;
                            while (nowWidthFactor > minWidthFactor) {
                                vWF.add(nowWidthFactor);
                                nowWidthFactor -= widthStep;
                                if (Math.abs(nowWidthFactor - minWidthFactor) < (widthStep / 4))
                                    break;
                            }
                            vWF.add(minWidthFactor);
                            widthList = new double[vWF.size()];
                            n = 0;
                            for (double w : vWF)
                                widthList[n++] = w * refWidth;
                        }
                        else
                            retVal.setErrorMessage(String.format("Production is more than limit of %5.3f t/h/meter width",
                                    maxUnitOutputAllowed / 1000));
                    }
                    else
                        retVal.setErrorMessage(String.format("Production is less than limit of %5.3f t/h/meter width",
                            minUnitOutput / 1000));
                } else
                    retVal.setErrorMessage(String.format("Strip Width is more than limit of %8.3f mm",
                        maxWidth * 1000));
            } else
                retVal.setErrorMessage(String.format("Strip Width is less than limit of %8.3f mm",
                    minWidth * 1000));
        }
        else
            retVal.setErrorMessage("SLimits are set for this Performance base");
        return retVal;
    }

    public PerformanceTable getPerformanceTable() {
        return perfTable;
    }

    public boolean createPerfTable(ThreadController master) {
//        controller.setTableFactors(this);
        boolean allOk = false;
        StatusWithMessage settingStat = setLimits();
        if (settingStat.getDataStatus() == DataStat.Status.OK) {
            perfTable = new PerformanceTable(this, outputFactors, widthList);
            double forOutput;
            double forWidth;
            Performance onePerf;
            allOk = true;
            for (double capF : outputFactors) {
                for (double widthF : widthList) {
//                if (widthF <= chLength || capF <= 1)  {
                    forWidth = widthF; // chLength * widthF;
                    forOutput = unitOutput * capF * forWidth;
                    if (furnace.evaluate(master, forOutput, forWidth)) {
                        onePerf = furnace.getPerformance();
                        if (onePerf == null)
                            showError("The data for " + forWidth + " not saved to the table");
                        else
                            perfTable.addToTable(widthF, capF, onePerf);
                    } else
                        allOk = false;
                    if (!allOk)
                        break;
//                }
                }
                if (!allOk)
                    break;
            }
//            controller.enableDataEdit();
            if (allOk)
                dfhProcess = controller.getStripDFHProcess(processName);
            controller.performanceTableDone();
        }
        return allOk;
    }

    double getUnitOutput() {
        unitOutput = output / chLength;
        return unitOutput;
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
            case CHEMMFACTOR:
                retVal = chEmmCorrectionFactor;
                break;
        }
        return retVal;
    }

    String getStringParam(Params param) {
        String retVal = "N/A";
        switch(param) {
            case PROCESSNAME:
                retVal = processName;
                break;
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
        boolean bComparable = isProcessNameComparable(performance.processName) &&
                (chLength == performance.chLength) &&
                    chMaterial.equals(performance.chMaterial) &&
                        fuelName.equals(performance.fuelName) &&
                            (Math.abs((output - performance.output) / output) < 0.01) &&
                                isExitTempComparable(performance.exitTemp(), exitTAllowance);
//                            (Math.abs(exitTemp() - performance.exitTemp()) < exitTAllowance);
        return bComparable;
    }


    boolean isExitTempComparable(double nowTemp,  double allowance)  {
        double refTemp = exitTemp();
        return ((nowTemp > (refTemp - allowance)) && (nowTemp <= (refTemp + allowance)));
    }

    boolean isProcessNameComparable(String nowProcessName) {
        return nowProcessName.equalsIgnoreCase(processName);
    }

    boolean isProductionComparable(String nowProcessName, String nowChMaterial, double nowExitTemp, double allowance) {
        boolean bComparable = isProcessNameComparable(nowProcessName);
        bComparable &= chMaterial.equals(nowChMaterial);
        bComparable &= isExitTempComparable(nowExitTemp, allowance);
        //(Math.abs(exitTemp() - nowExitTemp) < allowance);
        return bComparable;
    }

    boolean isProductionComparable(String nowProcessName, String nowChMaterial, String withFuel, double nowExitTemp, double allowance) {
        boolean retVal = isProductionComparable(nowProcessName, nowChMaterial, nowExitTemp, allowance);
        retVal &= fuelName.equals(withFuel);
        return retVal;
    }

    boolean isProductionComparable(ProductionData withProduction, Fuel withFuel, int compTypeFlags, double exitTAllowance) {
        boolean bComparable = isProcessNameComparable(withProduction.processName);
        if (bComparable) {
            Charge nowCharge = withProduction.charge;
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
                bComparable &= isExitTempComparable(withProduction.exitTemp, exitTAllowance);
//          bComparable &= (Math.abs(exitTemp() - withProduction.exitTemp) < exitTAllowance);
        }
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

    public DoubleRange getWidthRange() {
        return perfTable.getWidthRange();
    }

    public DoubleRange getUnitOutputRange() {
        return perfTable.getOutputFactorRange().multiply(getUnitOutput());
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

//    int getChInTempProfile(double[] chTempInProfile, double forExitTemp) {
//        double tempIn = topZones.get(0).stripTempIn;
//        double baseTempOut = topZones.get(topZones.size() - 1).stripTempOut;
//        double adjustFactor = (forExitTemp - tempIn) / (baseTempOut - tempIn);
//        int retVal = topZones.size();
//        int zNum = 0;
//        if (retVal <= chTempInProfile.length) {
//            for (OneZone z: topZones)
//                chTempInProfile[zNum++] = tempIn + (z.stripTempIn - tempIn) * adjustFactor;
//        }
//        else
//            retVal = 0;
//        return retVal;
//    }

    public double[] getChInTempProfile(double forExitTemp) {
        double tempIn = topZones.get(0).stripTempIn;
        double baseTempOut = topZones.get(topZones.size() - 1).stripTempOut;
        double adjustFactor = (forExitTemp - tempIn) / (baseTempOut - tempIn);
        double[] chTempInProfile = new double[topZones.size()];
        int zNum = 0;
            for (OneZone z: topZones)
                chTempInProfile[zNum++] = tempIn + (z.stripTempIn - tempIn) * adjustFactor;
        return chTempInProfile;
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
        StringBuilder xmlStr = new StringBuilder(XMLmv.putTag("ProcessNameP", processName));
        xmlStr.append(XMLmv.putTag("ChMaterialP", chMaterial));
        if (interpolated)  {
            xmlStr.append((XMLmv.putTagNew("Interpolated", 1)));
            xmlStr.append(XMLmv.putTagNew("dateOfResult", "Interpolated"));
        }
        else
            xmlStr.append(XMLmv.putTagNew("dateOfResult", dateOfResult.getTime().getTime()));
        xmlStr.append(XMLmv.putTagNew("chEmmCorrectionFactor", chEmmCorrectionFactor));
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

    /**
     * gets the dfhProcess in case readPerfTable is true
     * @param xmlStr
     * @param readPerfTable
     * @return
     */

    public boolean  takeDataFromXML(String xmlStr, boolean readPerfTable) {
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
            vp = XMLmv.getTag(xmlStr, "ProcessNameP", 0);
            processName = vp.val;
            vp = XMLmv.getTag(xmlStr, "ChMaterialP", 0);
            chMaterial = vp.val;
            vp = XMLmv.getTag(xmlStr, "chEmmCorrectionFactor", 0);
            if (vp.val.length() > 0)
                chEmmCorrectionFactor = Double.valueOf(vp.val);
            else
                chEmmCorrectionFactor = 1.0;
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
                if (readPerfTable) {
                    vp = XMLmv.getTag(xmlStr, "PerfTable", vp.endPos);
                    if (vp.val.length() > 20) {
                        try {
                            perfTable = new PerformanceTable(this, vp.val);
                        } catch (Exception e) {
                            showError("takeDataFromXML: Facing some problem is loading Performance table \n" + e.getMessage());
                            perfTable = null;
                        }
                    }
                    if (perfTable != null) {
                        DataWithStatus<OneStripDFHProcess> processStat = controller.getDFHProcess(this);
                        if (processStat.getDataStatus() == DataStat.Status.OK) {
                            dfhProcess = processStat.getValue();
                            processName = dfhProcess.getFullProcessID();
                        }
                        else
                            bRetVal = false;
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
            JPanel basicPanel = new JPanel(new BorderLayout());
            basicPanel.add(baseDataP(), BorderLayout.NORTH);
            basicPanel.add(perfTable.tableSelPanel(), BorderLayout.SOUTH);

            selectedPerfP.add(basicPanel, BorderLayout.NORTH);
//            selectedPerfP.add(perfTable.tableSelPanel(), BorderLayout.NORTH);
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
                    nlSpeed.setData(fuelP.recommendedSpeed(flow, false).getValue());
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
//        fuelFlowProfilePanel.add(getSpeedQueryPanel(fuelProfile), BorderLayout.NORTH);
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


    DateFormat formatter = new SimpleDateFormat("HH:mm, dd-MMM-yyyy");

    JPanel commonPerfDataP() {
        MultiPairColPanel pan = new MultiPairColPanel("");
        pan.addItemPair("Data Type: ", "Interpolated", false);
        addItemPair(pan, Params.STRIPWIDTH, 1000, "#,##0 mm");
        addItemPair(pan, Params.STRIPTHICK, 1000, "#,##0.00 mm");
        addItemPair(pan, Params.STRIPSPEED, (1.0 / 60), "#,##0.000 m/min");
        addItemPair(pan, Params.OUTPUT, (1.0 / 1000), "#,##0.00 t/h");
        addItemPair(pan, Params.AIRTEMP, 1, "#,##0 C");
        return pan;
    }

    public JPanel baseDataP() {
        JPanel outerP = new JPanel(new BorderLayout());
        MultiPairColPanel pan = new MultiPairColPanel("");
        addItemPair(pan, Params.DATE);
        addItemPair(pan, Params.PROCESSNAME);
        addItemPair(pan, Params.CHMATERIAL);
        addItemPair(pan, Params.CHEMMFACTOR, 1, "0.000");
        addItemPair(pan, Params.CHTEMPOUT, 1, "#,##0 C");
        addItemPair(pan, Params.FUEL);
        outerP.add(pan, BorderLayout.WEST);
        outerP.add(actionPanel(), BorderLayout.EAST);
        return outerP;
    }

    JPanel actionPanel() {
        updateTableButton.setEnabled(tableToBeRedone);
        JPanel p = new JPanel();
        p.add(updateTableButton);
        return p;
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
