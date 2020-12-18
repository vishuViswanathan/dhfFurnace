package basic;

import mvUtils.display.InputControl;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.NumberTextField;
import mvUtils.display.StatusWithMessage;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/19/12
 * Time: 10:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class RadiantTube {
    public enum RTFunction {HEATING, COOLING}

    ;

    public enum RTSource {ELECTIRICAL, FUELFIRED}

    ;

    public enum RTType {SINGLEENDED, UTYPE, WTYPE}

    ;

    RTFunction function = RTFunction.HEATING;
    RTSource heating = RTSource.ELECTIRICAL;
    RTType tubeType = RTType.SINGLEENDED;

    public double rTdia = 0.198, activeLen = 1.4;
    public double internHtCoeff = 5000, surfEmiss = 0.85;
    public double rating = 15; // in kW??
    public double elementTemp;
    InputControl ipc;

    public RadiantTube(InputControl ipc) {
        this(ipc, RTFunction.HEATING, RTSource.ELECTIRICAL, RTType.SINGLEENDED, 0.198, 1.4, 0.85, 15);
    }

    public RadiantTube(InputControl ipc, RTFunction function, RTSource heating, RTType tubeType, double od, double effLen, double emiss, double rating) {
        this.ipc = ipc;
        this.function = function;
        this.heating = heating;
        this.tubeType = tubeType;
        this.rTdia = od;
        this.activeLen = effLen;
        this.rating = rating;
        this.surfEmiss = emiss;
        prepareUIs();
    }

    public RadiantTube(InputControl ipc, double od, double effLen, double rating) {
        this(ipc, RTFunction.HEATING, RTSource.ELECTIRICAL, RTType.SINGLEENDED, od, effLen, 0.85, rating);

    }

    public RadiantTube(InputControl ipc, double od, double effLen, double rating, double emiss) {
        this(ipc, RTFunction.HEATING, RTSource.ELECTIRICAL, RTType.SINGLEENDED, od, effLen, emiss, rating);

    }

    public RadiantTube getACopy() {
        return new RadiantTube(ipc, function, heating, tubeType, rTdia, activeLen, surfEmiss, rating);
    }

    NumberTextField ntRadiantTubeOD;
    NumberTextField ntRadiantTubeLen;
    NumberTextField ntRadiantTubeRating;
    NumberTextField ntRadiantTubeEmiss;
    boolean radiantTubeFieldsSet = false;
    MultiPairColPanel dataPanel;
    JTabbedPane tabbedSectionPane;

    void prepareUIs() {
        ntRadiantTubeOD = new NumberTextField(ipc, rTdia * 1000, 6, false,
                10, 1000, "#,###", "Tube OD (mm)");
        ntRadiantTubeLen = new NumberTextField(ipc, activeLen * 1000, 6, false,
                10, 10000, "#,###", "Effective Length (mm)");
        ntRadiantTubeRating = new NumberTextField(ipc, rating, 6, false,
                0.01, 1000, "#,###", "Power Rating (kW)");
        ntRadiantTubeEmiss = new NumberTextField(ipc, surfEmiss, 6, false,
                0.01, 1.0, "0.00", "Surface Emissivity");
    }

    public JPanel radiantTubesP(InputControl ipc) {
        if (!radiantTubeFieldsSet) {
            MultiPairColPanel pan = new MultiPairColPanel("One Radiant tube" );
//            ntRadiantTubeOD = new NumberTextField(ipc, dia * 1000, 6, false,
//                    10, 1000, "#,###", "Tube OD (mm)");
//            ntRadiantTubeLen = new NumberTextField(ipc, activeLen * 1000, 6, false,
//                    10, 10000, "#,###", "Effective Length (mm)");
//            ntRadiantTubeRating = new NumberTextField(ipc, rating, 6, false,
//                    0.01, 1000, "#,###", "Power Rating (kW)");
//            ntRadiantTubeEmiss = new NumberTextField(ipc, surfEmiss, 6, false,
//                    0.01, 1.0, "0.00", "Surface Emissivity");
            pan.addItemPair(ntRadiantTubeOD);
            pan.addItemPair(ntRadiantTubeLen);
//            pan.addItemPair(ntRadiantTubeRating);
            pan.addItemPair(ntRadiantTubeEmiss);
            dataPanel = pan;
            radiantTubeFieldsSet = true;
        }
        return dataPanel;
    }

    public MultiPairColPanel getDataPanel() {
        return dataPanel;
    }

    public boolean takeFromUI() {
        boolean retVal = false;
        if (!ntRadiantTubeOD.isInError() && !ntRadiantTubeLen.isInError() && !ntRadiantTubeRating.isInError() &&
                !ntRadiantTubeEmiss.isInError()) {
            rTdia = ntRadiantTubeOD.getData() / 1000;
            activeLen = ntRadiantTubeLen.getData() / 1000;
            rating = ntRadiantTubeRating.getData();
            surfEmiss = ntRadiantTubeEmiss.getData();
            retVal = true;
        }
        return retVal;
    }

    void updateUI() {
        ntRadiantTubeOD.setData(rTdia * 1000);
        ntRadiantTubeLen.setData(activeLen * 1000);
        ntRadiantTubeRating.setData(rating);
        ntRadiantTubeEmiss.setData(surfEmiss);
    }

    public void enableDataEdit(boolean ena) {
        ntRadiantTubeOD.setEditable(ena);
        ntRadiantTubeLen.setEditable(ena);
        ntRadiantTubeRating.setEditable(ena);
        ntRadiantTubeEmiss.setEditable(ena);

    }

    public double getTotHeatingSurface() {
        return Math.PI * rTdia * activeLen;
    }

    public StatusWithMessage takeRadiantTubeFroXML(String xmlStr) {
        StatusWithMessage retVal = new StatusWithMessage();
        enableDataEdit(true);
        String errMsg = "";
        boolean allOK = true;
        ValAndPos vp;
        if (xmlStr.length() > 50) {
            try {
                vp = XMLmv.getTag(xmlStr, "rTdia", 0);
                rTdia = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "RTactiveLen", 0);
                activeLen = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "RTrating", 0);
                rating = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "RTsurfEmiss", 0);
                surfEmiss = Double.valueOf(vp.val);
                updateUI();
            }
            catch(Exception e) {
                errMsg = "Taking Radiant Tube Data from xml - some problem in reading data";
                allOK = false;
            }
        }
        if (!allOK)
            retVal.setErrorMessage(errMsg);
        return retVal;
    }

    public String RTDataToSave() {
        StringBuilder theStr =
                new StringBuilder(XMLmv.putTag("rTdia", "" + rTdia));
        theStr.append(XMLmv.putTag("RTactiveLen", "" + activeLen)).
                append(XMLmv.putTag("RTrating", "" + rating)).
                append(XMLmv.putTag("RTsurfEmiss", surfEmiss));
        String retVal = XMLmv.putTag("radiantTube", "\n" + theStr);
        return retVal;
    }
}
