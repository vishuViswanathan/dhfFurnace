package level2;

import basic.ChMaterial;
import basic.ProductionData;
import directFiredHeating.DFHTuningParams;
import mvUtils.display.*;
import mvUtils.math.BooleanWithStatus;
import mvUtils.math.DoubleWithStatus;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

import javax.media.j3d.J3DBuffer;
import javax.swing.*;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 09-Feb-15
 * Time: 5:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class OneStripDFHProcess {
    double density = 7850;
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
    double maxSpeed = 120 * 60; // m/h
    public double maxWidth = 1.25;  // m
    double minWidth = 0.9;  // m
    public double maxUnitOutput = 25000;  // kg/h for 1m with
    double minUnitOutput = 8500; // kg/h  for 1m width
    String errMeg = "Error reading StripDFHProcess :";
    public boolean inError = false;
    EditResponse.Response editResponse = EditResponse.Response.EXIT;
    StripDFHProcessList existingList;

    public OneStripDFHProcess(StripDFHProcessList existingList, String processName, Vector<ChMaterial> vChMaterial, InputControl inpC) {
        this.existingList = existingList;
        this.processName = processName;
        chMaterialThin = vChMaterial.get(0);
        chMaterialThick = vChMaterial.get(0);
//        editResponse = getDataFromUser(vChMaterial, inpC);
    }

    public OneStripDFHProcess(String processName, ChMaterial chMaterialThin, ChMaterial chMaterialThick,
                              double tempDFHExit, double thinUpperLimit) {
        this.processName = processName;
        this.chMaterialThin = chMaterialThin;
        this.chMaterialThick = chMaterialThick;
        this.tempDFHExit = tempDFHExit;
        this.thinUpperLimit = thinUpperLimit;
    }

    public OneStripDFHProcess(L2DFHeating l2DFHeating, StripDFHProcessList existingList, String processName, String chMaterialThinName, String chMaterialThickName,
                              double tempDFHExit, double thinUpperLimit) {
        this.existingList = existingList;
        this.processName = processName;
        this.chMaterialThin = l2DFHeating.getSelChMaterial(chMaterialThinName);
        this.chMaterialThick = l2DFHeating.getSelChMaterial(chMaterialThickName);
        this.tempDFHExit = tempDFHExit;
        this.thinUpperLimit = thinUpperLimit;
    }

    public EditResponse.Response getEditResponse() {
        return editResponse;
    }

    public OneStripDFHProcess(L2DFHeating l2DFHeating, StripDFHProcessList existingList, String xmlStr) {
        this.existingList = existingList;
        this.l2DFHeating = l2DFHeating;
        if (!takeDataFromXML(xmlStr))
            inError = true;
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

    public BooleanWithStatus checkStripSize(double width, double thickness) {
        BooleanWithStatus stat = new BooleanWithStatus(true);
        checkWidth(width, stat);
        checkThickness(thickness, stat);
        return stat;
    }

    public BooleanWithStatus checkUnitOutput(double unitOutput, BooleanWithStatus stat) {
        if (unitOutput > maxUnitOutput)
            stat.setErrorMessage("Unit Output is high <" + unitOutput + ">");
        else if (unitOutput < minUnitOutput)
            stat.setErrorMessage("Unit Output is low <" + unitOutput + ">");
        return stat;
    }

    public DoubleWithStatus checkAndLimitOutput(double output, double width, double thickness) {
        BooleanWithStatus response = checkStripSize(width, thickness);
        StatusWithMessage.DataStat stat = response.getDataStatus();
        DoubleWithStatus outputWithStatus = new DoubleWithStatus(0);
        if (stat == StatusWithMessage.DataStat.OK) {
            if (response.getValue()) {
                double unitOutput = output / width;
                if (unitOutput > maxUnitOutput)
                    outputWithStatus.setValue(maxUnitOutput * width, "Limited by Unit Output");
                else if (unitOutput < minUnitOutput)
                    outputWithStatus.setErrorMessage("Output is low <" + output + ">");
                else
                    outputWithStatus.setValue(output);
            }
        }
        else
            outputWithStatus.setErrorMessage(response.getErrorMessage());
        return outputWithStatus;
    }

    public BooleanWithStatus checkWidth(double width, BooleanWithStatus stat) {
        if (stat.getDataStatus() == StatusWithMessage.DataStat.OK) {
            if (width > maxWidth)
                stat.setErrorMessage("Strip Width is high <" + width + ">");
            else if (width < minWidth)
                stat.setErrorMessage("Strip Width is low <" + width + ">");
        }
        return stat;
    }

    public BooleanWithStatus checkThickness(double thick, BooleanWithStatus stat) {
        if (stat.getDataStatus() == StatusWithMessage.DataStat.OK) {
            if (thick > maxThickness)
                stat.setErrorMessage("Strip Thickness is high <" + thick + ">");
            else if (thick < minThickness)
                stat.setErrorMessage("Strip Thickness is low <" + thick + ">");
        }
        return stat;
    }


    public BooleanWithStatus isStripAcceptable(double output, double width, double thickness) {
        BooleanWithStatus response = checkStripSize(width, thickness);
        if ((response.getDataStatus() == StatusWithMessage.DataStat.OK) && (response.getValue()))
            checkUnitOutput((output / width), response);
        return response;
    }

    DoubleWithStatus getRecommendedSpeed(double output, double width, double thickness) {
        DoubleWithStatus response = checkAndLimitOutput(output, width, thickness);
        StatusWithMessage.DataStat status = response.getDataStatus();
        if (status != StatusWithMessage.DataStat.WithErrorMsg) {
            double speed = response.getValue() / (width * thickness * density);
            if (speed > maxSpeed) {
                speed = maxSpeed;
                response.setValue(speed, "Restricted by Maximum Process Speed");
            }
            else {
                if (status == StatusWithMessage.DataStat.WithInfoMsg)
                    response.setValue(speed, response.getInfoMessage());   // if it had any infoMessage, it ic forwarded
            }
        }
        return response;
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
                maxSpeed = Double.valueOf(vp.val) * 60;

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
        xmlStr.append(XMLmv.putTag("maxSpeed", "" + maxSpeed / 60));
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

    public ErrorStatAndMsg fieldDataOkForProcess(String processName, ProductionData production) {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg(true, "ERROR: ");
        DFHTuningParams tuning = l2DFHeating.getTuningParams();
        DecimalFormat mmFmt = new DecimalFormat("#,###");
        DecimalFormat outputFmt = new DecimalFormat("#,###.000");
        DecimalFormat tempFmt = new DecimalFormat("#,###");
        if (processName.equalsIgnoreCase(this.processName)) {
            double chWidth = production.charge.length;  // remember it is strip
            if (chWidth <= maxWidth) {
                double minWallowed = Math.max(minWidth, maxWidth / tuning.widthOverRange);
                if (chWidth >= minWallowed) {
                    double output = production.production;
                    double maxUnitOutputAllowed = maxUnitOutput * tuning.unitOutputOverRange;
                    double minUnitOutputAllowed = Math.max(minUnitOutput, maxUnitOutput * tuning.unitOutputUnderRange);
                    double unitOutputNow = output / chWidth;
                    if (unitOutputNow >= minUnitOutputAllowed) {
                        if (unitOutputNow <= maxUnitOutputAllowed) {
                            double maxExitTempAllowed = tempDFHExit + tuning.exitTempTolerance;
                            double minExitTempAllowed = tempDFHExit - tuning.exitTempTolerance;
                            double nowExitTemp = production.exitTemp;
                            if (nowExitTemp <= maxExitTempAllowed) {
                                if (nowExitTemp >= minExitTempAllowed) {
                                    if (production.exitZoneFceTemp > minExitZoneTemp)
                                        retVal.inError = false;
                                    else
                                        retVal.msg += "Exit Zone Temperature Low (minimum allowed is " + tempFmt.format(minExitZoneTemp) + " C)";
                                }
                                else
                                    retVal.msg += "Exit Temperature Low (minimum allowed is " + tempFmt.format(minExitTempAllowed) + " C)";
                            }
                            else
                                retVal.msg += "Exit Temperature High (maximum allowed is " + tempFmt.format(maxExitTempAllowed) + " C)";
                        }
                        else
                            retVal.msg += "Output too high (maximum allowed for this width is " + outputFmt.format(maxUnitOutputAllowed * chWidth / 1000) + " t/h)";
                    }
                    else
                        retVal.msg += "Output too low (minimum required for this width is " + outputFmt.format(minUnitOutputAllowed * chWidth / 1000) + " t/h)";
                }
                else
                    retVal.msg += "Strip is too narrow (minimum required " + mmFmt.format(minWallowed * 1000) + "mm)";
            }
            else
                retVal.msg += "Strip is too Wide (max allowed is " + maxWidth * 1000 + " mm)";
        }
        else
            retVal.msg += "Not acceptable process name (this is " + this.processName + ")";
        return retVal;
    }

    public DataListEditorPanel getEditPanel(Vector<ChMaterial> vChMaterial, InputControl inpC, DataHandler dataHandler,
                                       boolean editable, boolean startEditable) {
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
        ntMaxSpeed = new NumberTextField(inpC, maxSpeed / 60, 6, false, 50, 1000.0,
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

        DataListEditorPanel editorPanel = new DataListEditorPanel("Strip Process Data", dataHandler, editable, editable);
                                                        // if editable, it is also deletable
        editorPanel.addItemPair(tfProcessName);
        editorPanel.addBlank();
        editorPanel.addItemPair(cbChMaterialThin);
        editorPanel.addItemPair(ntThinUpperLimit);
        editorPanel.addItemPair(cbChMaterialThick);
        editorPanel.addBlank();
        editorPanel.addItemPair(ntTempDFHExit);
        editorPanel.addItemPair(ntMinExitZoneTemp);
        editorPanel.addBlank();
        editorPanel.addItemPair(ntMaxUnitOutput);
        editorPanel.addItemPair(ntMinUnitOutput);
        editorPanel.addBlank();
        editorPanel.addItemPair(ntMaxSpeed);
        editorPanel.addBlank();
        editorPanel.addItemPair(ntMaxThickness);
        editorPanel.addItemPair(ntMinThickness);
        editorPanel.addBlank();
        editorPanel.addItemPair(ntMaxWidth);
        editorPanel.addItemPair(ntMinWidth);
//        dlg.setLocation(100, 50);
        editorPanel.setVisible(true, startEditable);
        return editorPanel;
    }

    public ErrorStatAndMsg checkData() {
        ErrorStatAndMsg status = existingList.checkDuplication(this, tfProcessName.getText().trim());
        if (!status.inError) {
            StringBuilder msg = new StringBuilder();
            // check data in range
            if (ntMaxUnitOutput.isInError() || ntMinUnitOutput.isInError() || ntMaxThickness.isInError() ||
                    ntMaxThickness.isInError() || ntMaxSpeed.isInError() || ntMaxWidth.isInError() ||
                    ntMaxWidth.isInError()) {
                inError = true;
                msg.append("Some Data is/are out of range");
            } else {
                double maxUnitOutputX = ntMaxUnitOutput.getData();
                double minUnitOutputX = ntMinUnitOutput.getData();
                double maxThicknessX = ntMaxThickness.getData();
                double minThicknessX = ntMaxThickness.getData();
//                double maxSpeedX = ntMaxSpeed.getData();
                double maxWidthX = ntMaxWidth.getData();
                double minWidthX = ntMaxWidth.getData();
                if (maxUnitOutputX < minUnitOutputX) {
                    msg.append(ntMaxUnitOutput.getName() + " must be >= " + ntMinUnitOutput.getName() + "\n");
                    status.inError = true;
                }
                if (maxThicknessX < minThicknessX) {
                    msg.append(ntMaxThickness.getName() + " must be >= " + ntMinThickness.getName() + "\n");
                    status.inError = true;
                }
                if (maxWidthX < minWidthX) {
                    msg.append(ntMaxWidth.getName() + " must be >= " + ntMinWidth.getName() + "\n");
                    status.inError = true;
                }
            }
            if (status.inError)
                status.msg = msg.toString();
        }
        return status;
    }

    public boolean saveData() {
        return false;
    }

    public boolean deleteData() {
        return false;
    }

    public boolean resetData() {
        return false;
    }

    public void noteDataFromUI() {
        processName = tfProcessName.getText().trim();
        chMaterialThin = (ChMaterial)cbChMaterialThin.getSelectedItem();
        tempDFHExit = ntTempDFHExit.getData();
        minExitZoneTemp = ntMinExitZoneTemp.getData();
        chMaterialThick = (ChMaterial)cbChMaterialThick.getSelectedItem();
        maxUnitOutput = ntMaxUnitOutput.getData() * 1000;
        minUnitOutput = ntMinUnitOutput.getData() * 1000;
        maxThickness = ntMaxThickness.getData() / 1000;
        minThickness = ntMaxThickness.getData() / 1000;
        maxSpeed = ntMaxSpeed.getData() * 60;
        maxWidth = ntMaxWidth.getData() / 1000;
        minWidth = ntMaxWidth.getData() / 1000;
    }
}
