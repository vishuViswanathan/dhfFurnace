package basic;

import directFiredHeating.DFHeating;
import level2.L2DFHFurnace;
import level2.L2DFHeating;
import mvXML.ValAndPos;
import mvXML.XMLmv;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 6/18/12
 * Time: 4:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProductionData {
    public Charge charge;
    public double bottShadow;
    public double chPitch;
    public double production;  // in kg/h
    public double entryTemp;
    public double exitTemp;
    public double deltaTemp;
    public int nChargeRows = 1;
    public double piecesPerh = 0;
    public double speed = 0; // in m/h
    public String errMsg = "";
    public boolean inError = false;

    public ProductionData() {
        inError = false;
    }

    public ProductionData(ProductionData fromProductionData) {
        this();
        ProductionData fP = fromProductionData;
        charge = new Charge(fromProductionData.charge);
        chPitch = fromProductionData.chPitch;
        setProduction(fP.production, fP.nChargeRows, fP.entryTemp, fP.exitTemp, fP.deltaTemp, fP.bottShadow);
    }

    public ProductionData(DFHeating dfHeating, String xmlStr) {
        this();
        inError = !takeDataFromXML(dfHeating, xmlStr);
    }

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
        if (charge != null)
            piecesPerh = production / charge.unitWt;
    }

    public double totalChargeHeat() {
        return production * charge.getDeltaHeat(entryTemp, exitTemp);
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void copyFrom(ProductionData fromProductionData) {
        ProductionData fP = fromProductionData;
        setCharge(fP.charge, fP.chPitch);
        setProduction(fP.production, fP.nChargeRows, fP.entryTemp, fP.exitTemp, fP.deltaTemp, fP.bottShadow);
    }

    String errString = "";

    boolean takeDataFromXML(DFHeating dfHeating, String xmlStr) {
        boolean bRetVal = true;
        ValAndPos vp;
        try {
            vp = XMLmv.getTag(xmlStr, "charge", 0);
            charge = new Charge(dfHeating, vp.val);
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
        } catch (Exception e) {
            errMsg = "Same problem in Reading production data :" + e.getLocalizedMessage();
            bRetVal = false;
        }
        return bRetVal;
    }

    public StringBuilder dataInXML() {
        StringBuilder xmlStr = new StringBuilder(XMLmv.putTag("charge", charge.dataInXML()));
        xmlStr.append(XMLmv.putTag("bottShadow", bottShadow));
        xmlStr.append(XMLmv.putTag("chPitch", chPitch));
        xmlStr.append(XMLmv.putTag("production", production));
        xmlStr.append(XMLmv.putTag("entryTemp", entryTemp));
        xmlStr.append(XMLmv.putTag("exitTemp", exitTemp));
        xmlStr.append(XMLmv.putTag("deltaTemp", deltaTemp));
        xmlStr.append(XMLmv.putTag("nChargeRows", nChargeRows));
        return xmlStr;
    }

    public String getErrString() {
        return errString;
    }

    public boolean allOK() {
        boolean bRetVal = true;
        if (chPitch < charge.width) {
            errString = "Charge pitch is less than the charge width!";
            bRetVal = false;
        }
        return bRetVal;
    }
  }
