package basic;

import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.math.XYArray;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/19/12
 * Time: 1:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChMaterial {
    public String name;
    public String matID;
    XYArray tK; // thermal conductivity
    double tK0 = -1;
    XYArray heatC;  // specific heat
    XYArray e; // emissivity
    public double density; // kg/m3

    public ChMaterial(String name, String matID, double density,
                      XYArray thermalK,
                      XYArray heatContent,
                      XYArray emissivity) {
        this.matID = matID;
        this.name = name;
        this.density = density;
        this.tK = thermalK;
        if (tK.nElements() < 2) {
            if (tK.getXat(0) == 0)
                tK0 = tK.getYat(0);
        }
        this.heatC = heatContent;
        this.e = emissivity;
    }

    public ChMaterial(String name, String matID, double density, String tkPairStr, String hcPairStr, String emPairStr) {
        this(name, matID, density, new XYArray(tkPairStr), new XYArray(hcPairStr), new XYArray(emPairStr));
    }

    protected ChMaterial(String name, String matID) {
        this.name = name;
        this.matID = matID;
    }

    public ChMaterial(String xmlStr) throws Exception {
        if (!takeDataFromXML(xmlStr))
             throw new Exception("ERROR: In Charge Material Specifications from xml :" + xmlStr);
    }

    public boolean takeDataFromXML(String xmlStr) {
        ValAndPos vp;
        boolean retVal = false;
        vp = XMLmv.getTag(xmlStr, "Name", 0);
        name = vp.val;
        if (name.length() > 2) {
            vp = XMLmv.getTag(xmlStr, "matID", 0);
            matID = vp.val;
            vp = XMLmv.getTag(xmlStr, "density", 0);

            density = Double.valueOf(vp.val);
            try {
                vp = XMLmv.getTag(xmlStr, "tK", 0);
                tK = new XYArray(vp.val);
                if (tK.nElements() < 2) {
                    if (tK.getXat(0) == 0)
                        tK0 = tK.getYat(0);
                }
                vp = XMLmv.getTag(xmlStr, "heatC", 0);
                heatC = new XYArray(vp.val);
                vp = XMLmv.getTag(xmlStr, "e", 0);
                e = new XYArray(vp.val);
                retVal = true;
            } catch (Exception e1) {
                retVal = false;
            }
        }
        return retVal;
    }

    public String getName() {
        return name;
    }

    public double getDensity() {
        return density;
    }

    public double getHeatContent(double temperature) {
        return heatC.getYat(temperature);
    }

    public double avgSpHtOLD(double t1, double t2) {
        boolean tTooLow = false;
        double retVal = 0;
        if (t1 <= heatC.getXmin() || t2 <= heatC.getXmin())  {
            tTooLow = true;
        }
        if (t1 == t2)
            retVal =  spHtOLD(t1);
        else {
            if (t1 <= heatC.getXmin() && t2 <= heatC.getXmin())
                return spHt(heatC.getXmin() + 5);
            else if (t1 >= heatC.getXmax() && t2 >= heatC.getXmax())
                return spHt(heatC.getXmax() - 5);
            else
                retVal = (getHeatContent(t2) - getHeatContent(t1)) / (t2 - t1);
        }
        if (tTooLow)
            debug("tTooLow t1 =" + t1 + " t2 = " + t2 + " spHt = " + retVal);
        return retVal;
    }

    public double spHtOLD(double t) {
        boolean limited = false;
        double retVal = 0;
        double originalT = t;
        if (t >= heatC.getXmax()) {
            t = heatC.getXmax() - 5;
            limited = true;
        }
        else if (t <= heatC.getXmin()) {
            t = heatC.getXmin() + 5;
            limited = true;
        }
        retVal = (getHeatContent(t + 5) - getHeatContent(t - 5)) / 10;
        if (limited)
            debug("t limited in spHt to " + t + " from " + originalT + " and spHt =" + retVal);
        return retVal;
    }

    public double avgSpHt(double t1, double t2) {
        double retVal;
        double tMin = heatC.getXmin();
        double tMax = heatC.getXmax();
        t1 = (t1 < tMin) ? tMin :((t1 > tMax) ? tMax : t1);
        t2 = (t2 < tMin) ? tMin :((t2 > tMax) ? tMax : t2);
        if (t1 == t2)
            retVal =  spHt(t1);
        else {
            retVal = (getHeatContent(t2) - getHeatContent(t1)) / (t2 - t1);
        }
         return retVal;
    }

    public double spHt(double t) {
        double retVal;
        if (t >= heatC.getXmax()) {
            t = heatC.getXmax() - 5;
        }
        else if (t <= heatC.getXmin()) {
            t = heatC.getXmin() + 5;
        }
        retVal = (getHeatContent(t + 5) - getHeatContent(t - 5)) / 10;
        return retVal;
    }

    public double getHeatFromTemp(double temperature) {
        if (heatC.isXinRange(temperature))
            return heatC.getYat(temperature);
        else {
            if (temperature < 0 && temperature > -100) {
                return (heatC.getYat(100.0) / 100 * temperature);
            }
            else
                return Double.NaN;
        }
    }

    public double getDeltaHeat(double tFrom, double tTo) {
        return getHeatFromTemp(tTo) - getHeatFromTemp(tFrom);
    }

    public double getTempFromHeat(double heat) {
        if (heatC.isYinRange(heat))
            return heatC.getXat(heat);
        else
            return Double.NaN;
    }

    public double getTk(double temperature) {
        double tKVal;
        if (tK0 > 0) {
            if (temperature <= 900)
                tKVal = 23 + (tK0 - 23) * (900 - temperature) / 900;
            else
                tKVal = 23 + (temperature - 900) / (1400 - 900) * (28 - 23);

        }
        else
            tKVal = tK.getYat(temperature);
        return tKVal;
    }

    public double getEmiss(double temperature) {
        return e.getYat(temperature);
    }

    public String materialSpecInXML() {
        String xmlStr = XMLmv.putTag("Name", name) +
                XMLmv.putTag("matID", matID) +
                XMLmv.putTag("density", density);
        if (tK != null)
            xmlStr += XMLmv.putTag("tk", tK.valPairStr());
        if (e != null)
            xmlStr += XMLmv.putTag("e", e.valPairStr());
        if (heatC != null)
            xmlStr += XMLmv.putTag("heatC", heatC.valPairStr());
        return xmlStr;
    }


    public String chBasicToSave() {
        String retVal;
        retVal = "<material>" + XMLmv.putTag("name", name);
        retVal += XMLmv.putTag("matID", matID);
        retVal += "</material>" + "\n";
        return retVal;
    }

    public String toString() {
        return name;
    }

    void errMessage(String msg) {
        System.err.println("ChMaterial: ERROR: " + msg);
    }

    void debug(String msg) {
        System.out.println("ChMaterial: " + msg);
    }

}
