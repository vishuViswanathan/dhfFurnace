package level2;

import basic.ChMaterial;
import mvUtils.display.*;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

import javax.media.j3d.J3DBuffer;
import javax.swing.*;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 09-Feb-15
 * Time: 5:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class OneStripDFHProcess implements CheckDataList {
    L2DFHeating l2DFHeating;
    public String processName;
    ChMaterial chMaterialThin;
    ChMaterial chMaterialThick;
    double tempDFHExit = 620;
    double maxExitZoneTemp = 1050;
    double minExitZoneTemp = 900;
    double thinUpperLimit = 0.0004;   // in m
    double maxThickness = 0.0015;  // m
    double minThickness = 0.0001; //m
    double maxSpeed = 120; // m/min
    double maxWidth = 1.25;  // m
    double minWidth = 0.9;  // m
    double maxUnitOutput = 25000;  // kg/h for 1m with
    double minUnitOutput = 8500; // kg/h  for 1m width
    String errMeg = "Error reading StripDFHProcess :";
    public boolean inError = false;
    EditResponse.Response editResponse = EditResponse.Response.CANCELLED;

    public OneStripDFHProcess(String processName, Vector<ChMaterial> vChMaterial, InputControl inpC) {
        this.processName = processName;
        chMaterialThin = vChMaterial.get(0);
        chMaterialThick = vChMaterial.get(0);
        editResponse = getDataFromUser(vChMaterial, inpC);
    }

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

    public EditResponse.Response getEditResponse() {
        return editResponse;
    }

    public OneStripDFHProcess(L2DFHeating l2DFHeating, String xmlStr) {
        this.l2DFHeating = l2DFHeating;
        if (!takeDataFromXML(xmlStr))
            inError = true;
    }

    static boolean addStripDFHProcess(Hashtable<String, OneStripDFHProcess> stripProcessLookup,
                                      Vector<ChMaterial> vChMaterial, InputControl inpC) {
        boolean retVal = false;
        String proc = "DDQ";
        OneStripDFHProcess oneDFHProc = new OneStripDFHProcess(proc.toUpperCase(), vChMaterial, inpC);
        if (!oneDFHProc.inError) {
            stripProcessLookup.put(proc, oneDFHProc);
            retVal = true;
        }
        return retVal;
    }


    public double minOutputFactor() {
        if (maxUnitOutput > 0)
            return minUnitOutput / maxUnitOutput;
        else
            return 1;
    }

    public double minWidthFactor() {
        if (maxWidth > 0)
            return minWidth /maxWidth;
        else
            return 1;
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

                vp = XMLmv.getTag(xmlStr, "minExitZoneTemp", 0);
                minExitZoneTemp = Double.valueOf(vp.val);

                vp = XMLmv.getTag(xmlStr, "thinUpperLimit", 0);
                thinUpperLimit = Double.valueOf(vp.val) / 1000;

                vp = XMLmv.getTag(xmlStr, "maxUnitOutput", 0);
                maxUnitOutput = Double.valueOf(vp.val) * 1000;

                vp = XMLmv.getTag(xmlStr, "minUnitOutput", 0);
                minUnitOutput = Double.valueOf(vp.val) * 1000;

                vp = XMLmv.getTag(xmlStr, "maxSpeed", 0);
                maxSpeed = Double.valueOf(vp.val);

                vp = XMLmv.getTag(xmlStr, "maxThickness", 0);
                maxThickness = Double.valueOf(vp.val) / 1000;

                vp = XMLmv.getTag(xmlStr, "minThickness", 0);
                minThickness = Double.valueOf(vp.val) / 1000;

                vp = XMLmv.getTag(xmlStr, "maxWidth", 0);
                maxWidth = Double.valueOf(vp.val) / 1000;

                vp = XMLmv.getTag(xmlStr, "minWidth", 0);
                minWidth = Double.valueOf(vp.val) / 1000;

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

    public double getMinExitZoneTemp() {
        return minExitZoneTemp;
    }

    public StringBuffer dataInXML() {
        StringBuffer xmlStr = new StringBuffer(XMLmv.putTag("processName", processName));
        xmlStr.append(XMLmv.putTag("chMaterialThin", "" + chMaterialThin));
        xmlStr.append(XMLmv.putTag("chMaterialThick", "" + chMaterialThick));
        xmlStr.append(XMLmv.putTag("tempDFHExit", "" + tempDFHExit));
        xmlStr.append(XMLmv.putTag("minExitZoneTemp", "" + minExitZoneTemp));
        xmlStr.append(XMLmv.putTag("thinUpperLimit", "" + (thinUpperLimit * 1000)));
        xmlStr.append(XMLmv.putTag("maxUnitOutput", "" + (maxUnitOutput / 1000)));
        xmlStr.append(XMLmv.putTag("minUnitOutput", "" + (minUnitOutput / 1000)));
        xmlStr.append(XMLmv.putTag("maxSpeed", "" + maxSpeed));
        xmlStr.append(XMLmv.putTag("maxThickness", "" + (maxThickness * 1000)));
        xmlStr.append(XMLmv.putTag("minThickness", "" + (minThickness * 1000)));
        xmlStr.append(XMLmv.putTag("maxWidth", "" + (maxWidth * 1000)));
        xmlStr.append(XMLmv.putTag("minWidth", "" + (minWidth * 1000)));
        return xmlStr;
    }

    JTextField tfProcessName;
    JComboBox cbChMaterialThin;
    NumberTextField ntThinUpperLimit;
    JComboBox cbChMaterialThick;
    NumberTextField ntTempDFHExit;
    NumberTextField ntMinExitZoneTemp;
    NumberTextField ntMaxUnitOutput;
    NumberTextField ntMinUnitOutput;
    NumberTextField ntMaxSpeed;
    NumberTextField ntMaxThickness;
    NumberTextField ntMinThickness;
    NumberTextField ntMaxWidth;
    NumberTextField ntMinWidth;


    EditResponse.Response getDataFromUser(Vector<ChMaterial> vChMaterial, InputControl inpC) {
        tfProcessName = new JTextField(processName, 10);
//        tfProcessName.setEditable(false);
        tfProcessName.setName("Process Name");
        cbChMaterialThin = new JComboBox(vChMaterial);
        cbChMaterialThin.setName("Select Material to be taken for Thin strips");
        cbChMaterialThin.setSelectedItem(chMaterialThin);
        ntThinUpperLimit = new NumberTextField(inpC, thinUpperLimit * 1000, 6, false, 0.05, 0.9,
                "0.00", "Upper thickness Limit for Thin material (mm)");
        cbChMaterialThick = new JComboBox(vChMaterial);
        cbChMaterialThick.setName("Select Material to be taken for Thick strips");
        cbChMaterialThick.setSelectedItem(chMaterialThick);
        ntTempDFHExit = new NumberTextField(inpC, tempDFHExit, 6, false, 400, 1000,
                "#,##0", "Strip Temperature at DFH Exit (deg C)");
        ntMinExitZoneTemp = new NumberTextField(inpC, minExitZoneTemp, 6, false, 800, 1200,
                "#,##0", "Minimum DFH Exit Zone Temperature (deg C)");
        ntMaxUnitOutput = new NumberTextField(inpC, maxUnitOutput / 1000, 6, false, 0.2, 1000.0,
                "#,##0.00", "Maximum output for 1m wide strip (t/h)");
        ntMinUnitOutput = new NumberTextField(inpC, minUnitOutput / 1000, 6, false, 0.2, 1000.0,
                "#,##0.00", "Maximum output for 1m wide strip (t/h)");
        ntMaxSpeed = new NumberTextField(inpC, maxSpeed, 6, false, 50, 1000.0,
                "##0.00", "Maximum Process speed (m/min)");
        ntMaxSpeed.setToolTipText("<html>Ensure speed is sufficient for <p> " + ntMinUnitOutput.getName() + "</html>");
        ntMaxThickness = new NumberTextField(inpC, maxThickness * 1000, 6, false, 0.0, 100.0,
                "##0.00", "Maximum Strip Thickness (mm)");
        ntMinThickness = new NumberTextField(inpC, minThickness * 1000, 6, false, 0.0, 100.0,
                "##0.00", "Minimum Strip Thickness (mm)");
        ntMaxWidth = new NumberTextField(inpC, maxWidth * 1000, 6, false, 200, 5000,
                "#,##0", "Maximum Strip Width (mm)");
        ntMinWidth = new NumberTextField(inpC, minWidth * 1000, 6, false, 200, 5000,
                "#,##0", "Minimum Strip Width (mm)");

        DataListDialog dlg = new DataListDialog("Strip Process Data", this, true);
        dlg.addItemPair(tfProcessName);
        dlg.addBlank();
        dlg.addItemPair(cbChMaterialThin);
        dlg.addItemPair(ntThinUpperLimit);
        dlg.addItemPair(cbChMaterialThick);
        dlg.addBlank();
        dlg.addItemPair(ntTempDFHExit);
        dlg.addItemPair(ntMinExitZoneTemp);
        dlg.addBlank();
        dlg.addItemPair(ntMaxUnitOutput);
        dlg.addItemPair(ntMinUnitOutput);
        dlg.addBlank();
        dlg.addItemPair(ntMaxSpeed);
        dlg.addBlank();
        dlg.addItemPair(ntMaxThickness);
        dlg.addItemPair(ntMinThickness);
        dlg.addBlank();
        dlg.addItemPair(ntMaxWidth);
        dlg.addItemPair(ntMinWidth);
        dlg.setLocation(100, 50);
        dlg.setVisible(true);
        EditResponse.Response response = dlg.editResponse();
        if (response == EditResponse.Response.MODIFIED)
            noteDataFromUI();
        return response;
    }

    public ErrorStatAndMsg isDataListOK() {
        boolean inError = false;
        StringBuilder msg = new StringBuilder();
        // check data in range
        if (ntMaxUnitOutput.isInError() || ntMinUnitOutput.isInError() || ntMaxThickness.isInError() ||
                ntMaxThickness.isInError() || ntMaxSpeed.isInError() || ntMaxWidth.isInError() ||
                ntMaxWidth.isInError()) {
            inError = true;
            msg.append("Some Data is/are out of range");
        }
        else {
            double maxUnitOutputX = ntMaxUnitOutput.getData();
            double minUnitOutputX = ntMinUnitOutput.getData();
            double maxThicknessX = ntMaxThickness.getData();
            double minThicknessX = ntMaxThickness.getData();
            double maxSpeedX = ntMaxSpeed.getData();
            double maxWidthX = ntMaxWidth.getData();
            double minWidthX = ntMaxWidth.getData();
            if (maxUnitOutputX < minUnitOutputX) {
                msg.append(ntMaxUnitOutput.getName() + " must be >= " + ntMinUnitOutput.getName() + "\n");
                inError = true;
            }
            if (maxThicknessX < minThicknessX) {
                msg.append(ntMaxThickness.getName() + " must be >= " + ntMinThickness.getName() + "\n");
                inError = true;
            }
            if (maxWidthX < minWidthX) {
                msg.append(ntMaxWidth.getName() + " must be >= " + ntMinWidth.getName() + "\n");
                inError = true;
            }
        }
        return new ErrorStatAndMsg(inError, msg.toString());
    }

    void noteDataFromUI() {
        chMaterialThin = (ChMaterial)cbChMaterialThin.getSelectedItem();
        tempDFHExit = ntTempDFHExit.getData();
        minExitZoneTemp = ntMinExitZoneTemp.getData();
        chMaterialThick = (ChMaterial)cbChMaterialThick.getSelectedItem();
        maxUnitOutput = ntMaxUnitOutput.getData() * 1000;
        minUnitOutput = ntMinUnitOutput.getData() * 1000;
        maxThickness = ntMaxThickness.getData() / 1000;
        minThickness = ntMaxThickness.getData() / 1000;
        maxSpeed = ntMaxSpeed.getData();
        maxWidth = ntMaxWidth.getData() / 1000;
        minWidth = ntMaxWidth.getData() / 1000;
    }
}
