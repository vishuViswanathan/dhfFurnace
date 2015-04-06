package FceElements.heatExchanger;

import basic.Fluid;
import basic.FuelFiring;
import mvXML.ValAndPos;
import mvXML.XMLmv;
import mvmath.SPECIAL;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 1/3/14
 * Time: 10:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class HeatExchProps {
    public double heatingFlowBase;
    public double heatedFlowBase;
    public double heatingTinBase, heatingToutBase;
    public double heatedTinBase, heatedToutBase;
    public double heatExchBase;
    double flowRatioBase;
    double deltaTRatioBase;
    boolean bCounter;
    double fFBase; // flow factor
    double hTaBase;
    public boolean canPerform = false;
    public int calCount;

    public HeatExchProps() {
        canPerform = false;
    }

    public HeatExchProps(double heatingFlowBase, double heatedFlowBase, double heatingTinBase, double heatingToutBase,
                         double heatedTinBase, double heatedToutBase, double heatExch, boolean bCounter) {
        this();
        this.heatingFlowBase = heatingFlowBase;
        this.heatedFlowBase = heatedFlowBase;
        this.heatingTinBase = heatingTinBase;
        this.heatingToutBase = heatingToutBase;
        this.heatedTinBase = heatedTinBase;
        this.heatedToutBase = heatedToutBase;
        this.heatExchBase = heatExch;
        this.bCounter = bCounter;
        checkCanPerform();
    }

    boolean checkCanPerform() {
        try {
            double lmtd = lmtd(heatingTinBase, heatingToutBase, heatedTinBase, heatedToutBase);
            hTaBase = heatExchBase / lmtd;
            fFBase = Math.pow(heatingFlowBase, 0.6) + Math.pow(heatedFlowBase, 0.8);
            flowRatioBase = heatingFlowBase / heatedFlowBase;
            deltaTRatioBase = (heatingTinBase - heatingToutBase) / (heatedToutBase - heatedTinBase);
            canPerform = true;
        } catch (Exception e) {
            canPerform = false;
        }
        return canPerform;
    }

    // assumes that the heat-exchange/deltaT is constant based on the Base values for both fluids
    public HeatExchProps getPerformance(double heatingFlow, double heatingTempIn,
                                        double heatedFlow, double heatedTempIn)  {
        return getPerformance(null, heatingFlow, heatingTempIn, null, heatedFlow, heatedTempIn);
    }

    public HeatExchProps getPerformance(Fluid heatingFluid, double heatingFlow, double heatingTempIn,
                                        Fluid heatedFluid, double heatedFlow, double heatedTempIn)  {
        double errAllowed = 0.1; //
        double fF = Math.pow(heatingFlow, 0.6) + Math.pow(heatedFlow, 0.8);
        double ratio = fF / fFBase;
        double hTAavailable = hTaBase * ratio;
        double heatedToutAssume = (heatingTempIn + heatedTempIn) / 2;
        double deltaTHeatedAssume = heatedToutAssume - heatedTempIn;
        double deltaTHeatedRevised;
        double flowRatio = heatingFlow / heatedFlow;
        double deltaTRatio = deltaTRatioBase * (flowRatioBase / flowRatio);
        double cpHeated = heatExchBase / (heatedFlowBase * (heatedToutBase - heatedTinBase));
        double expFactor = Math.exp((deltaTRatio -1) / (cpHeated * heatedFlow / hTAavailable));
        double tempDiffIn = heatingTempIn - heatedTempIn;
        calCount = 0;
        int maxCount = 1000;
        HeatExchProps retVal = null;
        if (tempDiffIn > 0) {
            if (canPerform) {     // at the moment the fluid properties not considered ie. heatingFluid == null
                boolean bDone = false;
                double diff;
                double deltaTheating = 0;
                double heatExch;
                do {
                    deltaTheating = deltaTHeatedAssume * deltaTRatio;
                    deltaTHeatedRevised = tempDiffIn - (tempDiffIn - deltaTheating)* expFactor;
                    diff = Math.abs(deltaTHeatedRevised - deltaTHeatedAssume);
                    if (diff < errAllowed)
                        bDone = true;
                    else
                        deltaTHeatedAssume = (deltaTHeatedAssume + deltaTHeatedRevised) / 2;
                } while(!bDone && ++calCount < maxCount);
                if (bDone) {
                    heatExch = cpHeated * heatedFlow * deltaTHeatedAssume;
                    retVal =  new HeatExchProps(heatingFlow, heatedFlow, heatingTempIn,
                            heatingTempIn - deltaTheating, heatedTempIn, heatedTempIn + deltaTHeatedAssume, heatExch, bCounter);
                }
            }
        }
        return retVal;
    }

    double lmtd(double tf1In, double tF1Out, double tF2In, double tF2Out) {
        double deltaTa, deltaTb;
        if (bCounter)   {
            deltaTa = tf1In - tF2Out;
            deltaTb = tF1Out - tF2In;
        }
        else {
            deltaTa = tf1In - tF2In;
            deltaTb = tF1Out - tF2Out;
        }
        return SPECIAL.lmtd(deltaTa, deltaTb);
    }

    public boolean takeDataFromXML(String xmlStr, FuelFiring fuelFiring, double totalFuel) {
        boolean retVal = false;
        try {
            ValAndPos vp;
            vp = XMLmv.getTag(xmlStr, "bCounter", 0);
            bCounter = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "heatingTinBase", 0);
            heatingTinBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatingToutBase", 0);
            heatingToutBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatedTinBase", 0);
            heatedTinBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatedToutBase", 0);
            heatedToutBase = Double.valueOf(vp.val);
            // @TODO to calculate the heat exchange parameters from Fuel and fuel flow (any dilution ?)
            // heatingFlowBase, heatedFlowBase, heatExchBase
            heatingFlowBase = totalFuel * fuelFiring.unitFlueFlow();
            heatedFlowBase = totalFuel * fuelFiring.unitAirFlow();
            heatExchBase = totalFuel * fuelFiring.heatForAirPerUFuel(heatedTinBase, heatedToutBase);
        } catch (NumberFormatException e) {
            retVal = false;
        }

        return retVal;
    }

    public boolean takeDataFromXML(String xmlStr) {
        boolean bRetVal = true;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "bCounter", 0);
        bCounter = (vp.val.equals("1"));
        try {
            vp = XMLmv.getTag(xmlStr, "heatingFlowBase", 0);
            heatingFlowBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatedFlowBase", 0);
            heatedFlowBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatingTinBase", 0);
            heatingTinBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatingToutBase", 0);
            heatingToutBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatedTinBase", 0);
            heatedTinBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatedToutBase", 0);
            heatedToutBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatExchBase", 0);
            heatExchBase = Double.valueOf(vp.val);
            return checkCanPerform();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String dataInXML() {
        String xmlStr = XMLmv.putTag("bCounter", ((bCounter) ? "1" : "0"));
        xmlStr += XMLmv.putTag("heatingFlowBase", heatingFlowBase);
        xmlStr += XMLmv.putTag("heatedFlowBase", heatedFlowBase);
        xmlStr += XMLmv.putTag("heatingTinBase", heatingTinBase);
        xmlStr += XMLmv.putTag("heatingToutBase", heatingToutBase);
        xmlStr += XMLmv.putTag("heatedTinBase", heatedTinBase);
        xmlStr += XMLmv.putTag("heatedToutBase", heatedToutBase);
        xmlStr += XMLmv.putTag("heatExchBase", heatExchBase);
        xmlStr += XMLmv.putTag("fFBase", fFBase);
        xmlStr += XMLmv.putTag("hTaBase", hTaBase);
        return xmlStr;
    }
    public String toString() {
        return "heatingFlow = " + heatingFlowBase +
                "\nheatedFlow = " + heatedFlowBase +
                "\nheatingTin = " + heatingTinBase +
                "\nheatingTout = " + heatingToutBase +
                "\nheatedTin = " + heatedTinBase +
                "\nheatedTout = " + heatedToutBase +
                "\nheatExch = " + heatExchBase;
    }

    public static void main(String[] args) {
        final HeatExchProps recu = new HeatExchProps(26000, 20000, 531, 366.90, 30, 270, 1536000, true);
        HeatExchProps perf = recu.getPerformance(13000, 300, 10000, 30);
        System.out.println("" + perf);
        System.out.println("calculCount " + recu.calCount);
    }
}
