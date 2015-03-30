package level2.fieldResults;

import directFiredHeating.FceSection;
import level2.L2DFHFurnace;
import mvXML.ValAndPos;
import mvXML.XMLmv;

import java.text.DecimalFormat;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 18-Mar-15
 * Time: 2:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class FieldZone {
    L2DFHFurnace l2Furnace;
    FceSection sec;
    int zNum;
    double frFceTemp;
    double frFuelFlow;
    double frAirTemp;
    double frAfRatio;
    static DecimalFormat fmtTemp = new DecimalFormat("0.0");
    static DecimalFormat fmtFuelFlow = new DecimalFormat("0.00");
    boolean bValid = true;
    String errMsg;
    double lossFactor = 1;  // used for adjusting to the field results

    public FieldZone(L2DFHFurnace l2Furnace, FceSection sec) {
        this.l2Furnace = l2Furnace;
        this.sec = sec;
        this.zNum = sec.secNum;
    }

    public FieldZone(L2DFHFurnace l2Furnace, boolean bBot, int zNum) {
        this(l2Furnace, l2Furnace.getOneSection(bBot, zNum));
    }

    public FieldZone(L2DFHFurnace l2Furnace, boolean bBot, int zNum, double fceTemp, double fuelFlow, double airTemp, double afRatio) {
        this(l2Furnace, bBot, zNum);
        this.frFceTemp = fceTemp;
        this.frFuelFlow = fuelFlow;
        this.frAirTemp = airTemp;
        this.frAfRatio = afRatio;
        testValidity();
    }

    public FieldZone(L2DFHFurnace l2Furnace, boolean bBot, int zNum, String xmlStr) throws NumberFormatException {
        this(l2Furnace, bBot, zNum);
        takeFromXML(xmlStr);
        testValidity();
    }

    void testValidity() {
        bValid = (sec.bRecuType) ? (frFuelFlow == 0) : (frFuelFlow > 0);
        if (!bValid)
            errMsg = toString() + " Zone type and Fuel flow (" + frFuelFlow + ") does not match";
        else
            errMsg = "";
    }

    public void setValues(double fceTemp, double fuelFlow, double airTemp, double afRatio) {
        this.frFceTemp = fceTemp;
        this.frFuelFlow = fuelFlow;
        this.frAirTemp = airTemp;
        this.frAfRatio = afRatio;
    }

    public StringBuilder dataInXML() {
        StringBuilder xmlStr = new StringBuilder(XMLmv.putTag("frFceTemp", fmtTemp.format(frFceTemp)));
        xmlStr.append(XMLmv.putTag("frFuelFlow", fmtFuelFlow.format(frFuelFlow)));
        xmlStr.append(XMLmv.putTag("frAirTemp", fmtTemp.format(frAirTemp)));
        xmlStr.append(XMLmv.putTag("frAfRatio", frAfRatio));
        return xmlStr;
    }

    public boolean takeFromXML(String xmlStr) throws NumberFormatException {
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "frFceTemp", 0);
        frFceTemp = Double.valueOf(vp.val);
        vp = XMLmv.getTag(xmlStr, "frFuelFlow", 0);
        frFuelFlow = Double.valueOf(vp.val);
        vp = XMLmv.getTag(xmlStr, "frAirTemp", 0);
        frAirTemp = Double.valueOf(vp.val);
        vp = XMLmv.getTag(xmlStr, "frAfRatio", 0);
        frAfRatio = Double.valueOf(vp.val);
        return true;
    }

    public String toString() {
        return "Field Zone - " + sec.botSection + "Bottom Section - " + zNum;
    }
}
