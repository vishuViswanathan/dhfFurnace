package level2;

import basic.ChMaterial;
import com.sun.org.apache.bcel.internal.generic.L2D;
import display.InputControl;
import mvXML.ValAndPos;
import mvXML.XMLmv;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 09-Feb-15
 * Time: 5:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class OneStripDFHProcess {
    L2DFHeating l2DFHeating;
    public String processName;
    ChMaterial chMaterialThin;
    ChMaterial chMaterialThick;
    double tempDFHExit;
    double thinUpperLimit;   // in m
    double maxThickness;  // m
    double maxSpeed; // m/min
    double maxWidth;  // m
    double maxOutput;  // kg/h
    String errMeg = "Error reading StripDFHProcess :";
    public boolean inError = false;

    public OneStripDFHProcess(String processName, ChMaterial chMaterialThin, ChMaterial chMaterialThick,
                              double tempDFHExit, double thinUpperLimit) {
        this.processName = processName;
        this.chMaterialThin = chMaterialThin;
        this.chMaterialThick = chMaterialThick;
        this.tempDFHExit = tempDFHExit;
        this.thinUpperLimit = thinUpperLimit;
    }

    public OneStripDFHProcess(L2DFHeating l2DFHeating, String processName, String chMaterialThinName, String chMaterialThickName,
                              double tempDFHExit, double thinUpperLimit) {
        this.processName = processName;
        this.chMaterialThin = l2DFHeating.getSelChMaterial(chMaterialThinName);
        this.chMaterialThick = l2DFHeating.getSelChMaterial(chMaterialThickName);
        this.tempDFHExit = tempDFHExit;
        this.thinUpperLimit = thinUpperLimit;
    }

    public OneStripDFHProcess(L2DFHeating l2DFHeating, String xmlStr) {
        this.l2DFHeating = l2DFHeating;
        if (!takeDataFromXML(xmlStr))
            inError = true;
    }

    boolean takeDataFromXML(String xmlStr) {
        boolean retVal = false;
        ValAndPos vp;
        errMeg = "StripDFHProces reading data:";
        aBlock:
        {
            try {
                vp = XMLmv.getTag(xmlStr, "processName", 0);
                processName = vp.val.trim();
                String thinMaterialName;
                vp = XMLmv.getTag(xmlStr, "chMaterialThin", 0);
                thinMaterialName = vp.val.trim();
                chMaterialThin = l2DFHeating.getSelChMaterial(thinMaterialName);
                if (chMaterialThin == null) {
                    errMeg += "ChMaterialThin not found";
                    break aBlock;
                }
                String thickMaterialName;
                vp = XMLmv.getTag(xmlStr, "chMaterialThick", 0);
                thickMaterialName = vp.val.trim();
                chMaterialThick = l2DFHeating.getSelChMaterial(thickMaterialName);
                if (chMaterialThick == null) {
                    errMeg += "ChMaterialThick not found";
                    break aBlock;
                }
                vp = XMLmv.getTag(xmlStr, "tempDFHExit", 0);
                tempDFHExit = Double.valueOf(vp.val);

                vp = XMLmv.getTag(xmlStr, "thinUpperLimit", 0);
                thinUpperLimit = Double.valueOf(vp.val) / 1000;

                vp = XMLmv.getTag(xmlStr, "maxOutput", 0);
                maxOutput = Double.valueOf(vp.val) * 1000;

                vp = XMLmv.getTag(xmlStr, "maxSpeed", 0);
                maxSpeed = Double.valueOf(vp.val);

                vp = XMLmv.getTag(xmlStr, "maxThickness", 0);
                maxThickness = Double.valueOf(vp.val) / 1000;

                vp = XMLmv.getTag(xmlStr, "maxWidth", 0);
                maxWidth = Double.valueOf(vp.val) / 1000;

                retVal = true;
            } catch (NumberFormatException e) {
                errMeg += "Some Number format error";
                retVal = false;
                break aBlock;
            }
        }
        return retVal;
    }

    public ChMaterial getChMaterial(String proc, double stripThick) {
        ChMaterial theMaterial = null;
        if (proc.equalsIgnoreCase(processName)) {
            if (stripThick <= thinUpperLimit)
                theMaterial = chMaterialThin;
            else
                theMaterial = chMaterialThick;
        }
        return theMaterial;
    }

    public StringBuffer dataInXML() {
        StringBuffer xmlStr = new StringBuffer(XMLmv.putTag("processName", processName));
        xmlStr.append(XMLmv.putTag("chMaterialThin", "" + chMaterialThin));
        xmlStr.append(XMLmv.putTag("chMaterialThick", "" + chMaterialThick));
        xmlStr.append(XMLmv.putTag("tempDFHExit", "" + tempDFHExit));
        xmlStr.append(XMLmv.putTag("thinUpperLimit", "" + (thinUpperLimit * 1000)));
        xmlStr.append(XMLmv.putTag("maxOutput", "" + (maxOutput / 1000)));
        xmlStr.append(XMLmv.putTag("maxSpeed", "" + maxSpeed));
        xmlStr.append(XMLmv.putTag("maxThickness", "" + (maxThickness * 1000)));
        xmlStr.append(XMLmv.putTag("maxWidth", "" + (maxWidth * 1000)));
        return xmlStr;
    }
}
