package directFiredHeating.process;

import basic.ChMaterial;
import basic.ProductionData;
import com.sun.deploy.xml.XMLable;
import directFiredHeating.DFHTuningParams;
import directFiredHeating.DFHeating;
import mvUtils.display.*;
import mvUtils.jsp.JSPComboBox;
import mvUtils.math.BooleanWithStatus;
import mvUtils.math.DoubleWithStatus;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import performance.stripFce.Performance;
import performance.stripFce.StripProcessAndSize;

import java.text.DecimalFormat;
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
    DFHeating dfHeating;
    public String baseProcessName;
    ChMaterial chMaterialThin;
    ChMaterial chMaterialThick;
    public double tempDFHEntry = 30;
    public double tempDFHExit = 620;
    double maxExitZoneTemp = 1050;
    double minExitZoneTemp = 900;
    double thinUpperLimit = 0.0004;   // in m
    public double maxThickness = 0.0015;  // m
    public double minThickness = 0.0001; //m
    public double maxSpeed = 120 * 60; // m/h
    public double maxWidth = 1.25;  // m
    public double minWidth = 0.9;  // m
    public double maxUnitOutput = 25000;  // kg/h for 1m with
    public double minUnitOutput = 8500; // kg/h  for 1m width
    String errMeg = "Error reading StripDFHProcess :";
    public boolean inError = false;
    EditResponse.Response editResponse = EditResponse.Response.EXIT;
    StripDFHProcessList existingList;

    public OneStripDFHProcess(StripDFHProcessList existingList, String baseProcessName, Vector<ChMaterial> vChMaterial, InputControl inpC) {
        this.existingList = existingList;
        this.dfHeating = existingList.dfHeating;
        this.baseProcessName = baseProcessName;
        chMaterialThin = vChMaterial.get(0);
        chMaterialThick = vChMaterial.get(0);
    }

    public EditResponse.Response getEditResponse() {
        return editResponse;
    }

    public OneStripDFHProcess(DFHeating dfHeating, StripDFHProcessList existingList, String xmlStr) {
        this.existingList = existingList;
        this.dfHeating = dfHeating;
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
            return minWidth / maxWidth;
        else
            return 1;
    }

    public BooleanWithStatus checkStripSize(double width, double thickness) {
        BooleanWithStatus stat = new BooleanWithStatus(true);
        checkWidth(width, stat);
        checkThickness(thickness, stat);
        return stat;
    }

//    public BooleanWithStatus checkUnitOutput(double unitOutput, BooleanWithStatus stat) {
//        if (unitOutput > maxUnitOutput)
//            stat.setErrorMessage(String.format("Unit Output is high <%5.2f thp/m>", unitOutput / 1000));
//        else if (unitOutput < minUnitOutput)
//            stat.setErrorMessage(String.format("Unit Output is low <%5.2f thp/m>", unitOutput / 1000));
//        return stat;
//    }

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
                    outputWithStatus.setErrorMessage(String.format("Unit Output is low <%5.2f tph/m>", output));
                else
                    outputWithStatus.setValue(output);
            }
        } else
            outputWithStatus.setErrorMessage(response.getErrorMessage());
        return outputWithStatus;
    }

    public BooleanWithStatus checkWidth(double width, BooleanWithStatus stat) {
        if (stat.getDataStatus() == StatusWithMessage.DataStat.OK) {
            if (width > maxWidth)
                stat.setErrorMessage(String.format("Strip Width is high <%4.0f mm>", width * 1000));
            else if (width <= minWidth)
                stat.setErrorMessage(String.format("Strip Width is low <%4.0f mm>", width * 1000));
        }
        return stat;
    }

    public BooleanWithStatus checkThickness(double thick, BooleanWithStatus stat) {
        if (stat.getDataStatus() == StatusWithMessage.DataStat.OK) {
            if (thick > maxThickness)
                stat.setErrorMessage(String.format("Strip Thickness is high <%4.3f mm>", thick * 1000));
            else if (thick <= minThickness)
                stat.setErrorMessage(String.format("Strip Thickness is low <%4.3f mm>", thick * 1000));
        }
        return stat;
    }


//    public BooleanWithStatus isStripAcceptable(double output, double width, double thickness) {
//        BooleanWithStatus response = checkStripSize(width, thickness);
//        if ((response.getDataStatus() == StatusWithMessage.DataStat.OK) && (response.getValue()))
//            checkUnitOutput((output / width), response);
//        return response;
//    }

    public DoubleWithStatus getRecommendedSpeed(double output, double width, double thickness) {
        DoubleWithStatus response = checkAndLimitOutput(output, width, thickness);
        StatusWithMessage.DataStat status = response.getDataStatus();
        if (status != StatusWithMessage.DataStat.WithErrorMsg) {
            double speed = response.getValue() / (width * thickness * density);
            if (speed > maxSpeed) {
                speed = maxSpeed;
                response.setValue(speed, "Restricted by Maximum Process Speed");
            } else {
                if (status == StatusWithMessage.DataStat.WithInfoMsg)
                    response.setValue(speed, response.getInfoMessage());   // if it had any infoMessage, it ic forwarded
                else
                    response.setValue(speed);
            }
        }
        return response;
    }

    public double getLimitSpeed(double stripThickness) {
        DataWithStatus<ChMaterial> chMat = getChMaterial(stripThickness);
        if (chMat.valid) {
            double limitSpeed = maxUnitOutput / chMat.getValue().density / stripThickness;
            return Math.min(limitSpeed, maxSpeed);
        }
        else
            return 0;
    }

    boolean takeDataFromXML(String xmlStr) {
        boolean retVal = false;
        ValAndPos vp;
        errMeg = "StripDFHProces reading data:";
        if (xmlStr.length() > 100) {
            aBlock:
            {
                try {
                    vp = XMLmv.getTag(xmlStr, "processName", 0);
                    baseProcessName = vp.val.trim();
//                    vp = XMLmv.getTag(xmlStr, "description", 0);
//                    description = vp.val.trim();
                    String thinMaterialName;
                    vp = XMLmv.getTag(xmlStr, "chMaterialThin", 0);
                    thinMaterialName = vp.val.trim();
                    chMaterialThin = dfHeating.getSelChMaterial(thinMaterialName);
                    if (chMaterialThin == null) {
                        errMeg += "ChMaterialThin not found";
                        break aBlock;
                    }
                    String thickMaterialName;
                    vp = XMLmv.getTag(xmlStr, "chMaterialThick", 0);
                    thickMaterialName = vp.val.trim();
                    chMaterialThick = dfHeating.getSelChMaterial(thickMaterialName);
                    if (chMaterialThick == null) {
                        errMeg += "ChMaterialThick not found";
                        break aBlock;
                    }
                    vp = XMLmv.getTag(xmlStr, "tempDFHEntry", 0);
                    if (vp.val.length() > 0)
                        tempDFHEntry = Double.valueOf(vp.val);

                    vp = XMLmv.getTag(xmlStr, "tempDFHExit", 0);
                    tempDFHExit = Double.valueOf(vp.val);

                    vp = XMLmv.getTag(xmlStr, "maxExitZoneTemp", 0);
                    if (vp.val.length() > 0)
                        maxExitZoneTemp = Double.valueOf(vp.val);

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
                    break aBlock;
                }
            }
        }
        else
            errMeg = "Invalid Process data";
        return retVal;
    }

    public DataWithStatus<ChMaterial> getChMaterial(double stripThick) {
        DataWithStatus<ChMaterial> retVal = new DataWithStatus<>();
        if (thicknessInRange(stripThick)) {
            if (stripThick <= thinUpperLimit)
                retVal.setValue(chMaterialThin);
            else
                retVal.setValue(chMaterialThick);
        }
        return retVal;
    }

    public DataWithStatus<ChMaterial> getChMaterial(String proc, double stripThick) {
        if (proc.equalsIgnoreCase(baseProcessName))
            return getChMaterial(stripThick);
        else
            return new DataWithStatus<>();
    }

    public double getMinExitZoneTemp() {
        return minExitZoneTemp;
    }

    public double getMaxExitZoneTemp() {
        return maxExitZoneTemp;
    }

    public StringBuffer dataInXML() {
        StringBuffer xmlStr = new StringBuffer(XMLmv.putTag("processName", baseProcessName));
//        xmlStr.append(XMLmv.putTag("description", description));
        xmlStr.append(XMLmv.putTag("chMaterialThin", "" + chMaterialThin));
        xmlStr.append(XMLmv.putTag("chMaterialThick", "" + chMaterialThick));
        xmlStr.append(XMLmv.putTag("tempDFHEntry", "" + tempDFHEntry));
        xmlStr.append(XMLmv.putTag("tempDFHExit", "" + tempDFHExit));
        xmlStr.append(XMLmv.putTag("maxExitZoneTemp", "" + maxExitZoneTemp));
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

    XLTextField tfBaseProcessName;
    XLTextField tfFullProcessID;
    JSPComboBox cbChMaterialThin;
    NumberTextField ntThinUpperLimit;
    JSPComboBox cbChMaterialThick;
    Vector<NumberTextField> dataFieldList;
    NumberTextField ntTempDFHEntry;
    NumberTextField ntTempDFHExit;
    NumberTextField ntMinExitZoneTemp;
    NumberTextField ntMaxExitZoneTemp;
    NumberTextField ntMaxUnitOutput;
    NumberTextField ntMinUnitOutput;
    NumberTextField ntMaxSpeed;
    NumberTextField ntMaxThickness;
    NumberTextField ntMinThickness;
    NumberTextField ntMaxWidth;
    NumberTextField ntMinWidth;

    boolean widthInRange(double width) {
        return width > minWidth && width <= maxWidth;
    }

    boolean thicknessInRange(double thickness) {
        return thickness > minThickness && thickness <= maxThickness;
    }

    boolean exitTempInRange(double temperature) {
        double margin = dfHeating.getTuningParams().exitTempTolerance;
        return (temperature > (tempDFHExit - margin)) && (temperature <= (tempDFHExit + margin));
    }

    public ErrorStatAndMsg checkExitTemperature(double nowExitTemp) {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg();
        DFHTuningParams tuning = dfHeating.getTuningParams();
        DecimalFormat tempFmt = new DecimalFormat("#,###");
        double maxExitTempAllowed = tempDFHExit + tuning.exitTempTolerance;
        double minExitTempAllowed = tempDFHExit - tuning.exitTempTolerance;
        if (nowExitTemp <= maxExitTempAllowed) {
            if (nowExitTemp <= minExitTempAllowed)
                retVal.addErrorMsg("Exit Temperature Low (must be greater than " + tempFmt.format(minExitTempAllowed) + " C)");
        } else
            retVal.addErrorMsg("Exit Temperature is High (maximum allowed is " + tempFmt.format(maxExitTempAllowed) + " C)");
        return retVal;
    }

    public ErrorStatAndMsg performanceOkForProcess(Performance p) {
        double chWidth = p.chLength; // charge length corresponds to width of strip
        double chThick = p.chThick;
        String chMaterial = p.chMaterial;
        double unitOutput = p.unitOutput;
        double exitTemp = p.exitTemp();
        DFHTuningParams tuning = dfHeating.getTuningParams();
        ErrorStatAndMsg retVal = new ErrorStatAndMsg(false, "Selecting Process for Performance data: ");
        DataWithStatus<ChMaterial> chMat = getChMaterial(chThick);
        if (chMat.valid && (chMaterial.equalsIgnoreCase(chMat.getValue().name))) {
            if (exitTempInRange(exitTemp)) {
                ErrorStatAndMsg stat = performanceOkForProcess(chWidth, unitOutput);
                if (stat.inError)
                    retVal.add(stat);
            }
            else
                retVal.addErrorMsg("Exit Temperature no in range, required " + tempDFHExit + " +- " + tuning.exitTempTolerance);
        }
        else
            retVal.addErrorMsg("Charge Material mismatch: \n" +
                    "performance = " + p + "\n" +
                    "Process = " + getFullProcessID());
        return retVal;
    }

    /**
     *
     * @param chWidth
     * @param unitOutput
     * @return
     */
    public ErrorStatAndMsg performanceOkForProcess(double chWidth, double unitOutput) {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg(true, "Checking width and output");
        DFHTuningParams tuning = dfHeating.getTuningParams();
        DecimalFormat mmFmt = new DecimalFormat("#,###");
        DecimalFormat outputFmt = new DecimalFormat("#,###.000");
        if (chWidth <= maxWidth) {
            double minWallowed = Math.max(minWidth, maxWidth / tuning.widthOverRange);
            if (chWidth >= minWallowed) {
                double maxUnitOutputAllowed = maxUnitOutput * tuning.unitOutputOverRange;
                double minUnitOutputAllowed = Math.max(minUnitOutput, maxUnitOutput * tuning.unitOutputUnderRange);
                trace("" + minUnitOutputAllowed + " < " + unitOutput + " < " + maxUnitOutputAllowed);
                if (unitOutput >= minUnitOutputAllowed) {
                    if (unitOutput <= maxUnitOutputAllowed) {
                        retVal.inError = false;
                    }else
                        retVal.msg += "Output too high (maximum allowed for this width is " + outputFmt.format(maxUnitOutputAllowed * chWidth / 1000) + " t/h)";
                } else
                    retVal.msg += "Output too low (minimum required for this width is " + outputFmt.format(minUnitOutputAllowed * chWidth / 1000) + " t/h)";
            } else
                retVal.msg += "Strip is too narrow (minimum required " + mmFmt.format(minWallowed * 1000) + "mm)";
        } else
            retVal.msg += "Strip is too Wide (max allowed is " + maxWidth * 1000 + " mm)";
        return retVal;
    }

    public ErrorStatAndMsg productionOkForPerformanceSave(ProductionData production) {
        return productionOkForPerformanceSave(baseProcessName, production);
    }

    public ErrorStatAndMsg productionOkForPerformanceSave(String processName, ProductionData production) {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg(true, "For Process " + processName + " fieldData, ");
        DecimalFormat tempFmt = new DecimalFormat("#,###");
        if (processName.equalsIgnoreCase(this.baseProcessName)) {
            double chWidth = production.charge.length;  // remember it is strip
            double unitOutputNow = production.production / chWidth;
            ErrorStatAndMsg stat = performanceOkForProcess(chWidth, unitOutputNow);
            if (!stat.inError) {
                ErrorStatAndMsg tempStat = checkExitTemperature(production.exitTemp);
                if (!tempStat.inError) {
                    production.entryTemp = tempDFHEntry;
                    production.exitTemp = tempDFHExit;
                    if (production.exitZoneFceTemp > minExitZoneTemp) {
                        retVal.inError = false;
                    } else
                        retVal.msg += "Exit Zone Temperature Low (minimum allowed is " + tempFmt.format(minExitZoneTemp) + " C)";
                }
                else
                    retVal.add(tempStat);
            } else
                retVal.add(stat);
        } else
            retVal.msg += "Not acceptable process name (this is " + this.baseProcessName + ")";
        return retVal;
    }

    public ErrorStatAndMsg productionOkForPerformanceSaveOLD(String processName, ProductionData production) { // TODO to be removed
        ErrorStatAndMsg retVal = new ErrorStatAndMsg(true, "For Process " + processName + " fieldData, ");
        DFHTuningParams tuning = dfHeating.getTuningParams();
        DecimalFormat mmFmt = new DecimalFormat("#,###");
        DecimalFormat outputFmt = new DecimalFormat("#,###.000");
        DecimalFormat tempFmt = new DecimalFormat("#,###");
        if (processName.equalsIgnoreCase(this.baseProcessName)) {
            trace("Process Name ok");
            double chWidth = production.charge.length;  // remember it is strip
            if (chWidth <= maxWidth) {
                double minWallowed = Math.max(minWidth, maxWidth / tuning.widthOverRange);
                if (chWidth >= minWallowed) {
                    trace("Width OK");
                    double output = production.production;
                    double maxUnitOutputAllowed = maxUnitOutput * tuning.unitOutputOverRange;
                    double minUnitOutputAllowed = Math.max(minUnitOutput, maxUnitOutput * tuning.unitOutputUnderRange);
                    double unitOutputNow = output / chWidth;
                    trace("" + minUnitOutputAllowed + " < " + unitOutputNow + " < " + maxUnitOutputAllowed);
                    if (unitOutputNow >= minUnitOutputAllowed) {
                        if (unitOutputNow <= maxUnitOutputAllowed) {
                            trace("output OK");
                            ErrorStatAndMsg tempStat = checkExitTemperature(production.exitTemp);
                            if (!tempStat.inError) {
                                production.entryTemp = tempDFHEntry;
                                production.exitTemp = tempDFHExit;
                                if (production.exitZoneFceTemp > minExitZoneTemp) {
                                    retVal.inError = false;
                                } else
                                    retVal.msg += "Exit Zone Temperature Low (minimum allowed is " + tempFmt.format(minExitZoneTemp) + " C)";
                            }
                            else
                                retVal.msg += tempStat.msg;
                        } else
                            retVal.msg += "Output too high (maximum allowed for this width is " + outputFmt.format(maxUnitOutputAllowed * chWidth / 1000) + " t/h)";
                    } else
                        retVal.msg += "Output too low (minimum required for this width is " + outputFmt.format(minUnitOutputAllowed * chWidth / 1000) + " t/h)";
                } else
                    retVal.msg += "Strip is too narrow (minimum required " + mmFmt.format(minWallowed * 1000) + "mm)";
            } else
                retVal.msg += "Strip is too Wide (max allowed is " + maxWidth * 1000 + " mm)";
        } else
            retVal.msg += "Not acceptable process name (this is " + this.baseProcessName + ")";
        return retVal;
    }

    public DataListEditorPanel getEditPanel(Vector<ChMaterial> vChMaterial, InputControl inpC, DataHandler dataHandler,
                                            boolean editable, boolean startEditable) {
        dataFieldList = new Vector<NumberTextField>();
        NumberTextField ntf;
        tfBaseProcessName = new XLTextField(baseProcessName, 10);
        tfBaseProcessName.setName("Base Process Name");
        tfFullProcessID = new XLTextField(this.toString(), 30);
        tfFullProcessID.setName("Full Process ID");
        tfFullProcessID.setEditable(false);
        cbChMaterialThin = new JSPComboBox<ChMaterial>(dfHeating.jspConnection, vChMaterial);
        cbChMaterialThin.setName("Select Material to be taken for Thin strips");
        cbChMaterialThin.setSelectedItem(chMaterialThin);
        ntf = ntThinUpperLimit = new NumberTextField(inpC, thinUpperLimit * 1000, 6, false, 0.05, 0.9,
                "0.00", "Upper thickness Limit for Thin material (mm)");
        dataFieldList.add(ntf);
        cbChMaterialThick = new JSPComboBox<ChMaterial>(dfHeating.jspConnection, vChMaterial);
        cbChMaterialThick.setName("Select Material to be taken for Thick strips");
        cbChMaterialThick.setSelectedItem(chMaterialThick);
        ntf = ntTempDFHEntry = new NumberTextField(inpC, tempDFHEntry, 6, false, 0, 300,
                "#,##0", "Strip Temperature at DFH Entry (deg C)");
        dataFieldList.add(ntf);
        ntf = ntTempDFHExit = new NumberTextField(inpC, tempDFHExit, 6, false, 400, 1000,
                "#,##0", "Strip Temperature at DFH Exit (deg C)");
        dataFieldList.add(ntf);
        ntf = ntMaxExitZoneTemp = new NumberTextField(inpC, maxExitZoneTemp, 6, false, 800, 1200,
                "#,##0", "Maximum DFH Exit Zone Temperature (deg C)");
        dataFieldList.add(ntf);
        ntf = ntMinExitZoneTemp = new NumberTextField(inpC, minExitZoneTemp, 6, false, 800, 1200,
                "#,##0", "Minimum DFH Exit Zone Temperature (deg C)");
        dataFieldList.add(ntf);
        ntf = ntMaxUnitOutput = new NumberTextField(inpC, maxUnitOutput / 1000, 6, false, 0.2, 1000.0,
                "#,##0.00", "Maximum Unit Output (t/h) - ie. for 1m wide strip");
        dataFieldList.add(ntf);
        ntf = ntMinUnitOutput = new NumberTextField(inpC, minUnitOutput / 1000, 6, false, 0.2, 1000.0,
                "#,##0.00", "Minimum Unit Output (t/h) - ie. for 1m wide strip");
        dataFieldList.add(ntf);
        ntf = ntMaxSpeed = new NumberTextField(inpC, maxSpeed / 60, 6, false, 50, 1000.0,
                "##0.00", "Maximum Process speed (m/min)");
        dataFieldList.add(ntf);
        ntf = ntMaxThickness = new NumberTextField(inpC, maxThickness * 1000, 6, false, 0.0, 100.0,
                "##0.00", "Maximum Strip Thickness (mm) - Inclusive");
        dataFieldList.add(ntf);
        ntMinThickness = new NumberTextField(inpC, minThickness * 1000, 6, false, 0.0, 100.0,
                "##0.00", "Minimum Strip Thickness (mm) - Exclusive");
        ntf = ntMaxWidth = new NumberTextField(inpC, maxWidth * 1000, 6, false, 200, 5000,
                "#,##0", "Maximum Strip Width (mm) - Inclusive");
        dataFieldList.add(ntf);
        ntf = ntMinWidth = new NumberTextField(inpC, minWidth * 1000, 6, false, 200, 5000,
                "#,##0", "Minimum Strip Width (mm) - Exclusive");
        dataFieldList.add(ntf);

        DataListEditorPanel editorPanel = new DataListEditorPanel("Strip Process Data", dataHandler, editable, editable);
        // if editable, it is also deletable
        editorPanel.addItemPair(tfBaseProcessName);
        editorPanel.addItemPair(tfFullProcessID);
        editorPanel.addBlank();
        editorPanel.addItemPair(cbChMaterialThin);
        editorPanel.addItemPair(ntThinUpperLimit);
        editorPanel.addItemPair(cbChMaterialThick);
        editorPanel.addBlank();
        editorPanel.addItemPair(ntTempDFHEntry);
        editorPanel.addItemPair(ntTempDFHExit);
        editorPanel.addBlank();
        editorPanel.addItemPair(ntMinExitZoneTemp);
        editorPanel.addItemPair(ntMaxExitZoneTemp);
        editorPanel.addBlank();
        editorPanel.addItemPair(ntMinUnitOutput);
        editorPanel.addItemPair(ntMaxUnitOutput);
        editorPanel.addBlank();
        editorPanel.addItemPair(ntMinThickness);
        editorPanel.addItemPair(ntMaxThickness);
        editorPanel.addBlank();
        editorPanel.addItemPair(ntMinWidth);
        editorPanel.addItemPair(ntMaxWidth);
        editorPanel.addBlank();
        editorPanel.addItemPair(ntMaxSpeed);
        editorPanel.addItem("<html>Ensure speed is sufficient for Minimum Unit Output with thinnest strip</html>");
        editorPanel.setVisible(true, startEditable);
        return editorPanel;
    }

    public boolean doesProcessMatch(StripProcessAndSize theStrip) {
        return doesProcessMatch(theStrip.processBaseName, theStrip.exitTemp, theStrip.width, theStrip.thickness);
    }

    public boolean doesProcessMatch(String baseProcessNameX, double tempDFHExitX, double stripWidth, double stripThick) {
        return baseProcessNameX.equalsIgnoreCase(baseProcessName) && exitTempInRange(tempDFHExitX) &&
                widthInRange(stripWidth) && thicknessInRange(stripThick);
    }


    public String getFullProcessID() {
        return getFullProcessID(baseProcessName, tempDFHExit, minWidth, maxWidth, minThickness, maxThickness);
    }

    public static String getFullProcessID(String baseProcessNameX, double  exitTempX, double minWidthX, double maxWidthX,
                                    double minThicknessX, double maxThicknessX) {
        if (isProcessBaseNameOk(baseProcessNameX))
            return String.format("%s-(%1.0f<W<%1.0f)(%1.2f<Th<%1.2f)(%1.0fC)", baseProcessNameX, minWidthX * 1000, maxWidthX * 1000,
                minThicknessX * 1000, maxThicknessX * 1000, exitTempX);
        else
            return "?";
    }

    public String toString() {
        return getFullProcessID();
    }

    public ErrorStatAndMsg doesItOverlap(OneStripDFHProcess withThis) {
        return doesItOverlap(withThis.baseProcessName, withThis.tempDFHExit, withThis.minWidth, withThis.maxWidth,
                withThis.minThickness, withThis.maxThickness);
    }

    public ErrorStatAndMsg doesItOverlap(String baseProcessNameX, double  exitTempX, double minWidthX, double maxWidthX,
                                 double minThicknessX, double maxThicknessX) {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg();
        StringBuilder msg = new StringBuilder("New Process '" +
                getFullProcessID(baseProcessNameX, exitTempX, minWidthX, maxWidthX, minThicknessX, maxThicknessX) +
                "', parameters overlap with \nthe existing '" + getFullProcessID() + ";");
        double margin = dfHeating.getTuningParams().exitTempTolerance;
        if (baseProcessNameX.equalsIgnoreCase(baseProcessName)) {
            msg.append("\n  Base Process Names are identical,");
            if (!(((exitTempX + margin) <= (tempDFHExit - margin)) || (exitTempX - margin) >= (tempDFHExit + margin)) ) {
                msg.append(String.format("\n  New Exit Temperatures %4.1f(%4.1f+ to %4.1f) \n" +
                        "     overlaps with existing  %4.1f(%4.1f+ to %4.1f)",
                        exitTempX, exitTempX - margin, exitTempX + margin,
                        tempDFHExit, tempDFHExit - margin, tempDFHExit + margin));
                if (!(maxWidthX <= minWidth || minWidthX >= maxWidth)) {
                    msg.append(String.format("\n  New Width range %4.0f+ to %4.0f\n" +
                            "      overlaps existing range %4.0f+ to %4.0f",
                            minWidthX * 1000, maxWidthX * 1000, minWidth * 1000, maxWidth * 1000));
                    if (!(maxThicknessX <= minThickness || minThicknessX >= maxThickness)) {
                        msg.append(String.format("\n  and New Thickness range %4.3f+ to %4.3f\n" +
                                        "      overlaps existing range %4.3f+ to %4.3f",
                                minThicknessX * 1000, maxThicknessX * 1000, minThickness * 1000, maxThickness * 1000));
                        retVal.addErrorMsg(msg.toString());
                    }
                }
            }
        }
        return retVal;
    }

    static boolean isProcessBaseNameOk(String name)  {
        return !(name.length() < 2 || name.substring(0, 1).equals("."));
    }

    public ErrorStatAndMsg checkData() {
        ErrorStatAndMsg status = new ErrorStatAndMsg();
        String baseProcessNameX = tfBaseProcessName.getText().trim();
//        if (baseProcessNameX.length() < 2 || baseProcessNameX.substring(0, 1).equals(".")) {
        if (!isProcessBaseNameOk(baseProcessNameX)) {
            status.inError = true;
            status.msg += "Enter proper process Name";
        }
        else {
            StringBuilder msg = new StringBuilder();
            // check data in range
            if (allDataFieldsLegal()) {
                double tempDFHExitX = ntTempDFHExit.getData();
                double maxExitZoneTempX = ntMaxExitZoneTemp.getData();
                double minExitZoneTempX = ntMinExitZoneTemp.getData();
                double maxUnitOutputX = ntMaxUnitOutput.getData() * 1000;
                double minUnitOutputX = ntMinUnitOutput.getData() * 1000;
                double maxThicknessX = ntMaxThickness.getData() / 1000;
                double minThicknessX = ntMinThickness.getData() / 1000;
                double maxSpeedX = ntMaxSpeed.getData() * 60;
                double maxWidthX = ntMaxWidth.getData() / 1000;
                double minWidthX = ntMinWidth.getData()/ 1000;
                double thinUpperLimitX = ntThinUpperLimit.getData() / 1000;
                ChMaterial chMaterialThinX = (ChMaterial) cbChMaterialThin.getSelectedItem();
                if (maxExitZoneTempX <= minExitZoneTempX)
                    status.addErrorMsg("Max. Exit ZoneTemperature must be > Min. Exit Zone Temperature\n");
                if (tempDFHExitX >= minExitZoneTempX )
                    status.addErrorMsg("Min. Exit Zone Temperature must be > Strip Exit Temperature\n");
                if (maxUnitOutputX < minUnitOutputX)
                    status.addErrorMsg(" Max. Unit Output must be >  Min. Unit Output\n");
                if (maxThicknessX < minThicknessX)
                    status.addErrorMsg("Max. Thickness must be > Min. Thickness\n");
                if (maxWidthX < minWidthX)
                    status.addErrorMsg("Max. Width must be > Min. Width\n");
                if (minThicknessX > thinUpperLimitX || maxThicknessX < thinUpperLimitX)
                    status.addErrorMsg("Mismatch in Strip Thickness data\n");
                if (minUnitOutputX / (minThicknessX * chMaterialThinX.density) > maxSpeedX)
                    status.addErrorMsg("Maximum strip speed is NOT sufficient to handle thinnest strip");
                if (!status.inError) {
                    status = existingList.checkDuplication(this, baseProcessNameX, tempDFHExitX, minWidthX, maxWidthX,
                            minThicknessX, maxThicknessX);
                }
            }
            else {
                status.addErrorMsg("Some Data is/are out of range");
            }
        }
        inError = status.inError;
        return status;
    }

    boolean allDataFieldsLegal() {
        boolean retVal = true;
        for (NumberTextField ntf: dataFieldList)
            if (ntf.inError) {
                retVal = false;
                break;
            }
        return retVal;
    }

    public ErrorStatAndMsg noteDataFromUI() {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg();
        baseProcessName = tfBaseProcessName.getText().trim();
        chMaterialThin = (ChMaterial) cbChMaterialThin.getSelectedItem();
        chMaterialThick = (ChMaterial) cbChMaterialThick.getSelectedItem();
        if (allDataFieldsLegal()) {
            tempDFHEntry = ntTempDFHEntry.getData();
            tempDFHExit = ntTempDFHExit.getData();
            maxExitZoneTemp = ntMaxExitZoneTemp.getData();
            minExitZoneTemp = ntMinExitZoneTemp.getData();
            thinUpperLimit = ntThinUpperLimit.getData() / 1000;
            maxUnitOutput = ntMaxUnitOutput.getData() * 1000;
            minUnitOutput = ntMinUnitOutput.getData() * 1000;
            maxThickness = ntMaxThickness.getData() / 1000;
            minThickness = ntMinThickness.getData() / 1000;
            maxSpeed = ntMaxSpeed.getData() * 60;
            maxWidth = ntMaxWidth.getData() / 1000;
            minWidth = ntMinWidth.getData() / 1000;
            tfFullProcessID.setText(getFullProcessID());
        }
        else
            retVal.addErrorMsg("Some Fields have un-acceptable Data");
        return retVal;
    }

    void trace(String msg) {
        System.out.println(msg);
    }
}
