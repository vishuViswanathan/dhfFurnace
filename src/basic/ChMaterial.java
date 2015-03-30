package basic;

import com.sun.xml.internal.bind.v2.model.core.ID;
import mvXML.ValAndPos;
import mvXML.XMLmv;
import mvmath.XYArray;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/19/12
 * Time: 1:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChMaterial {
    public String name;
    String matID;
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

    public ChMaterial(String xmlStr) throws Exception {
        ValAndPos vp;
        boolean inError = true;
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
                inError = false;
            } catch (Exception e1) {
                inError = true;
            }
        }
        if (inError)
             throw new Exception("ERROR: In Charge Material Specifications from xml :" + xmlStr);
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

    public double avgSpHt(double t1, double t2) {
        if (t1 == t2)
            t2 = t1 + 10;
        return(getHeatContent(t2) - getHeatContent(t1)) / (t2 - t1);
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
        System.err.println("ElementType: ERROR: " + msg);
    }

    void debug(String msg) {
        System.out.println("ElementType: " + msg);
    }

}
