package basic;

import directFiredHeating.DFHeating;
import directFiredHeating.process.OneStripDFHProcess;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 6/18/12
 * Time: 4:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProductionData {
    public String processName;
    public OneStripDFHProcess stripProcess;  // used only in strip processing
    public Charge charge;
    public double chEmmissCorrectionFactor = 1.0;
    public double bottShadow = 0;
    public double chPitch = 1.0;
    public double production;  // in kg/h
    public double entryTemp = 30;
    public double exitTemp;
    public double deltaTemp = 1;
    public int nChargeRows = 1;
    // the following two applicable for Strip heating
    public double exitZoneFceTemp;
    public double minExitZoneTemp;
    public double piecesPerh = 0;
    public double speed = 0; // in m/h
    public String errMsg = "";
    public boolean inError = false;

    public ProductionData() {
        inError = false;
    }

    public ProductionData(String processName) {
        this();
        this.processName = processName;

    }

    public ProductionData(OneStripDFHProcess stripProcess) {
        this();
        this.stripProcess = stripProcess;
        processName = stripProcess.getFullProcessID();
    }

    public ProductionData(ProductionData fromProductionData) {
        this();
        ProductionData fP = fromProductionData;
        this.stripProcess = fP.stripProcess;
        this.processName = fP.processName;
        this.chEmmissCorrectionFactor = fP.chEmmissCorrectionFactor;
        charge = new Charge(fromProductionData.charge);
        chPitch = fromProductionData.chPitch;
        setProduction(fP.production, fP.nChargeRows, fP.entryTemp, fP.exitTemp, fP.deltaTemp, fP.bottShadow);
        setExitZoneTempData(fP.exitZoneFceTemp, fP.minExitZoneTemp);
    }

//    public ProductionData(DFHeating dfHeating, String xmlStr) {
//        this();
//        inError = !takeDataFromXML(dfHeating, xmlStr);
//    }

    public void setCharge(Charge charge, double chPitch) {
        this.charge = charge;
        this.chPitch = chPitch;
    }

    public void setProduction(double production, int nChargeRows, double entryTemp, double exitTemp, double deltaTemp, double bottShadow) {
        this.production = production;
        this.entryTemp = entryTemp;
        this.exitTemp = exitTemp;
        this.deltaTemp = deltaTemp;
        this.bottShadow = bottShadow;
        this.nChargeRows = nChargeRows;
        chEmmissCorrectionFactor = 1.0;
        if (charge != null)
            piecesPerh = production / charge.unitWt;
    }

    public void setChEmmissCorrectionFactor(double chEmmissCorrectionFactor) {
        this.chEmmissCorrectionFactor = chEmmissCorrectionFactor;
    }

    public void setExitZoneTempData(double exitZoneFceTemp, double minExitZoneTemp) {
        this.exitZoneFceTemp = exitZoneFceTemp;
        this.minExitZoneTemp = minExitZoneTemp;
    }


    public double totalChargeHeat() {
        return production * charge.getDeltaHeat(entryTemp, exitTemp);
    }

    public double getChargeHeat(double fromTemp, double toTemp) {
        return production * charge.getDeltaHeat(fromTemp, toTemp);
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void copyFrom(ProductionData fromProductionData) {
        ProductionData fP = fromProductionData;
        setCharge(fP.charge, fP.chPitch);
        setProduction(fP.production, fP.nChargeRows, fP.entryTemp, fP.exitTemp, fP.deltaTemp, fP.bottShadow);
        setExitZoneTempData(fP.exitZoneFceTemp, fP.minExitZoneTemp);
    }

    String errString = "";

    boolean takeDataFromXML(DFHeating dfHeating, String xmlStr) {
        boolean bRetVal = true;
        ValAndPos vp;
        try {
            vp = XMLmv.getTag(xmlStr, "processName", 0);
            processName =  vp.val;
            vp = XMLmv.getTag(xmlStr, "charge", 0);
            charge = new Charge(dfHeating, vp.val);
            vp = XMLmv.getTag(xmlStr, "chEmmissCorrectionFactor", vp.endPos);
            if (vp.val.length() > 0)
                chEmmissCorrectionFactor = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "bottShadow", 0);
            bottShadow = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "chPitch", 0);
            chPitch = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "production", 0);
            production = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "entryTemp", 0);
            entryTemp = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "exitTemp", 0);
            exitTemp = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "deltaTemp", 0);
            deltaTemp = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "nChargeRows", 0);
            nChargeRows = Integer.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "dischZoneFceTemp", 0);
            exitZoneFceTemp = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "minDischZoneTemp", 0);
            minExitZoneTemp = Double.valueOf(vp.val);
        } catch (Exception e) {
            errMsg = "Same problem in Reading production data :" + e.getLocalizedMessage();
            bRetVal = false;
        }
        return bRetVal;
    }

    public boolean allOK() {
        boolean bRetVal = true;
        if (chPitch < charge.width) {
            errString = "Charge pitch is less than the charge width!";
            bRetVal = false;
        }
        return bRetVal;
    }

    public String toString() {
        return (String.format("Process: %s; output: %5.2f; Charge(%s); exitT: %4.0f",
                processName, production, charge.toString(), exitTemp));
    }

  }
