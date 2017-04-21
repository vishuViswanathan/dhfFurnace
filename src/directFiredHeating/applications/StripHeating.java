package directFiredHeating.applications;

import basic.ChMaterial;
import basic.ProductionData;
import directFiredHeating.DFHTuningParams;
import directFiredHeating.DFHeating;
import directFiredHeating.FceEvaluator;
import directFiredHeating.ResultsReadyListener;
import directFiredHeating.process.OneStripDFHProcess;
import directFiredHeating.stripDFH.SampleStripFurnace;
import mvUtils.display.*;
//import mvUtils.jnlp.JNLPFileHandler;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import performance.stripFce.Performance;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.DecimalFormat;


/**
 * User: M Viswanathan
 * Date: 18-Jul-16
 * Time: 4:08 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class StripHeating extends DFHeating {
    protected String fceDataLocation = "level2FceData/mustBeUserEntry/";
    protected String profileCode = "";
    protected boolean changeProfileCode = true;
    protected String profileCodeTag = "profileCode";
    protected DecimalFormat profileCodeFormat = new DecimalFormat("000000");
    boolean associatedDataLoaded = false;
    protected String l2BasePath = "";

    protected StripHeating() {
        super();
    }
    protected void setTestData() {
        StatusWithMessage status = takeProfileDataFromXML(SampleStripFurnace.xmlStr);
        if (status.getDataStatus() == DataStat.Status.WithErrorMsg)
            showError(status.getErrorMessage());
    }

    protected JPanel processPanel() {
        MultiPairColPanel jp = new MultiPairColPanel("Process");
        jp.addItemPair("Process Name", dfhProcessList.getListUI());
        return jp;
    }

    protected OneStripDFHProcess getDFHProcess() {
        return dfhProcessList.getSelectedProcess();
    }

    protected String getProcessName() {
        processName = dfhProcessList.getSelectedProcessName();
        return processName;
    }

    protected void disableSomeUIs() {
        tfMinExitZoneFceTemp.setEnabled(false);
        tfExitZoneFceTemp.setEnabled(false);
        tfExitTemp.setEnabled(false);
        // freeze some selections
        cbHeatingMode.setEnabled(false);
        cbFceFor.setEnabled(false);
    }

    boolean checkProfileCode(String withThis) {
        return (withThis != null) && (profileCode.equals(withThis));
    }

    String profileCodeInXML() {
        return XMLmv.putTag(profileCodeTag, profileCode);
    }

    protected String stripDFHProcessListInXML() {
        return XMLmv.putTag("dfhProcessList", dfhProcessList.dataInXMl().toString());
    }

    public String inputDataXML(boolean withPerformance) {
        return profileCodeInXML() + super.inputDataXML(withPerformance) +
                XMLmv.putTag("FuelSettings", furnace.fceSettingsInXML()) +
                stripDFHProcessListInXML();
    }

    protected boolean isProfileCodeOK() {
        return (profileCode.length() == profileCodeFormat.format(0).length());
    }

    protected boolean getUserResponse(OneStripDFHProcess oneProc, ChMaterial material) {
        boolean proceed = true;
        boolean someDecisionRequired = false;
        String toDecide = "<html><h4>Some parameters are set from Strip DFH Process <b>" + oneProc.baseProcessName + "</b>";
        if (material != selChMaterial) {
            toDecide += "<blockQuote> # Charge Material selected is not matching for thickness <b>" + (chThickness * 1000) + "mm</b>.<p>" +
                    "&emsp;Will be changed to <b>" + material + "</b> as per DFH Strip Process</blockQuote>";
            someDecisionRequired = true;
        }
        toDecide += "<blockQuote># Exit Temperature set as <b>" + oneProc.tempDFHExit + "</b></blockQuote>" +
                "<blockQuote># Exit Zone Temperature set as <b>" + oneProc.getMaxExitZoneTemp() + "</b></blockQuote>" +
                "<blockQuote># Minimum Exit Zone Temperature set as <b>" + oneProc.getMinExitZoneTemp() + "</b></blockQuote>";
        toDecide += "</html>";
        String title = "Strip DFH Process";
        if (someDecisionRequired)
            proceed = decide(title, toDecide);
        else
            showMessage(title, toDecide);
        return proceed;
    }

    protected ErrorStatAndMsg isChargeInFceOK() {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg();
        if (tfProduction.isInError()) {
            retVal.addErrorMsg("\n   " + tfProduction.getName());
        }
        if (!retVal.inError) {
            if ((furnaceFor() == DFHTuningParams.FurnaceFor.STRIP)) {
                OneStripDFHProcess oneProc = getDFHProcess();
                if (oneProc != null) {
                    DataWithStatus<ChMaterial> chMatSat = oneProc.getChMaterial(chThickness);
                    if (chMatSat.valid) {
                        ChMaterial material = chMatSat.getValue();
                        boolean proceed = getUserResponse(oneProc, material);
                        if (proceed) {
                            setChMaterial(material);
                            setEntryTemperature(oneProc.tempDFHEntry);
                            setExitTemperature(oneProc.tempDFHExit);
                            setExitZoneTemperatures(oneProc.getMaxExitZoneTemp(), oneProc.getMinExitZoneTemp());
                        } else
                            retVal.addErrorMsg("\n   ABORTING");
                    } else
                        retVal.addErrorMsg("\n   Charge material could not be obtained from Process Data (may be thickness not in range");
                } else {
                    retVal.addErrorMsg("\n   Process not available in DFH Process List");
                }
            } else
                retVal.addErrorMsg("The furnace if not processing strip");
        }
        return retVal;
    }

    public boolean checkProfileCodeInXML(String xmlStr) {
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, profileCodeTag);
        return checkProfileCode(vp.val);
    }

    public StatusWithMessage takeProfileDataFromXML(String xmlStr) {
        StatusWithMessage retVal = new StatusWithMessage();
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, profileCodeTag);
        profileCode = vp.val;
        if (isProfileCodeOK()) {
            vp = XMLmv.getTag(xmlStr, "dfhProcessList");
            if (vp.val.length() < 10 || !takeStripProcessListFromXML(vp.val)) {
                retVal.addErrorMessage("No Strip Process Data available");
            } else if (dfhProcessList.getCount() < 1) {
                retVal.addErrorMessage("Process List must have at least one entry");
            }
            retVal = super.takeProfileDataFromXML(xmlStr, true, HeatingMode.TOPBOTSTRIP,
                    DFHTuningParams.FurnaceFor.STRIP);
            if (retVal.getDataStatus() == DataStat.Status.OK) {
                furnace.clearAssociatedData();
                vp = XMLmv.getTag(xmlStr, "FuelSettings");
                if (vp.val.length() < 10 || !furnace.takeFceSettingsFromXML(vp.val)) {
                    retVal.addErrorMessage("No Fuel settings data available");
                }
            }
        } else
            retVal.addErrorMessage("ERROR: Not a Level2 Furnace Profile");
        return retVal;
    }

    public void deletePerformance(Performance p) {
        furnace.deletePerformance(p);
    }



    protected boolean saveFurnaceWithNowProfileCode() {
        changeProfileCode = false;
        saveFceToFile(true);
        changeProfileCode = true;
        return true;
    }

    protected void saveFceToFile(boolean withPerformance) {
        takeValuesFromUI();
        StatusWithMessage fceSettingsIntegrity = furnace.furnaceSettings.checkIntegrity();
        if (fceSettingsIntegrity.getDataStatus() == DataStat.Status.OK) {
//            if (asJNLP)
//                saveFceToFileJNLP(withPerformance);
//            else {
                String title = "Save Level2 Furnace Data";
                FileDialog fileDlg =
                        new FileDialog(mainF, title,
                                FileDialog.SAVE);
                boolean profileCodeChanged = createProfileCode();
                String promptFile = (profileCodeChanged) ?
                        (profileCode + " FurnaceProfile." + profileFileExtension) :
                        profileFileName;
                logInfo("setting default Folder to " + fceDataLocation);
                fileDlg.setDirectory(fceDataLocation);
                fileDlg.setFile(promptFile);
                fileDlg.setVisible(true);

                String bareFile = fileDlg.getFile();
                if (!(bareFile == null)) {
                    int len = bareFile.length();
                    if ((len < 8) || !(bareFile.substring(len - 7).equalsIgnoreCase("." + profileFileExtension))) {
                        showMessage("Adding '." + profileFileExtension + "' to file name");
                        bareFile = bareFile + "." + profileFileExtension;
                    }
                    String fileName = fileDlg.getDirectory() + bareFile;
                    try {
                        BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(fileName));
                        oStream.write(inputDataXML(withPerformance).getBytes());
                        oStream.close();
                        if (withPerformance)
                            furnace.performanceIsSaved();
                        profileFileName = bareFile;
                    } catch (FileNotFoundException e) {
                        showError("File " + fileName + " NOT found!");
                    } catch (IOException e) {
                        showError("Some IO Error in writing to file " + fileName + "!");
                    }
                }
                parent().toFront();
//            }
        } else
            showError("Problem in Fuel Settings :\n" + fceSettingsIntegrity.getErrorMessage());
    }

//    protected void saveFceToFileJNLP(boolean withPerformance) {
//        takeValuesFromUI();
//        boolean profileCodeChanged = createProfileCode();
//        String promptFile = (profileCodeChanged) ?
//                (profileCode + " FurnaceProfile." + profileFileExtension) :
//                profileFileName;
//        DataWithStatus<String> saveStatus = JNLPFileHandler.saveToFile(inputDataXML(withPerformance), profileFileExtension,
//                promptFile, "Please save file With extension '." + profileFileExtension + ", and suggested Name");
//        if (saveStatus.getStatus() == DataStat.Status.OK)
//            profileFileName = saveStatus.getValue();
//        else
//            showError(saveStatus.getErrorMessage() + " - Could not save to file");
//        parent().toFront();
//    }
//

    protected boolean createProfileCode() {
        debugLocal("changeProfileCode: " + changeProfileCode);
        if (changeProfileCode || (profileCode.length() < 1)) {
            profileCode = profileCodeFormat.format(Math.random() * 999999.0);
            return true;
        }
        return false;
    }

    protected boolean takeStripProcessListFromXML(String xmlStr) {
//        logInfo("#################### in StripHeating.takeStripProcessListFromXML ");
        return dfhProcessList.takeStripProcessListFromXML(xmlStr);
    }

    protected void editStripDFHProcess() {
        if (dfhProcessList.addStripDFHProcess(parent()))
            showMessage("<html>Strip DFH Process List is modified" +
                    "<br />To make it permanent, the furnace data must be updated" +
                    "<br />with <b><font color= 'blue' >" + mIUpdateFurnace.getText() + "</font></b>" +
                    " from <b><font color = 'blue'>" + fileMenu.getText() + "</font></b> menu</html>");
    }

    void viewStripDFHProcess() {
        dfhProcessList.viewStripDFHProcess(parent());
    }

    void setOPCIP() {
        if (furnace.furnaceSettings.getOPCServerIP(this)) {
            showMessage("Furnace connection Data is modified\n" +
                    "To be effective in Level2 RUNTIME:\n" +
                    "Save data to file with " + fileMenu.getText() + "->" + mIUpdateFurnace.getText());
        }
    }

    private void fuelRangeSettings() {
        if (furnace.showEditFceSettings(true)) {
            showMessage("Furnace Fuel Data is modified\n" +
                    "Make sure to Save data to file with " + fileMenu.getText() + "->" + mIUpdateFurnace.getText());
        }
    }

    private void fieldDataSettings() {
        if (dfhProcessList.showFieldDataSettingsEditData(true, this.parent())) {
            showMessage("Settings for Field Data have been modified\n" +
                    "Make sure to Save data to file with " + fileMenu.getText() + "->" + mIUpdateFurnace.getText());
        }
    }

    //    JMenu mL2FileMenu;
    protected JMenuItem mIUpdateFurnace;

    protected JMenu mL2Configuration;
    protected JMenuItem mIOPCServerIP;
    protected JMenuItem mIEditDFHStripProcess;
    protected JMenuItem mIViewDFHStripProcess;
    protected JMenuItem mISettingsForFuelRange;
    protected JMenuItem mISettingsForFieldData;

    protected void disableCompare() {
    }

    protected void createAllMenuItems() {
        super.createAllMenuItems();
        L2MenuListener li = new L2MenuListener();
        mIOPCServerIP = new JMenuItem("Set OPC server IP");
        mISettingsForFuelRange = new JMenuItem("View/ Edit Fuel Range Settings");
        mISettingsForFieldData = new JMenuItem("View/ Edit Basic Process Settings");
        mIEditDFHStripProcess = new JMenuItem("Add/Edit StripDFHProcess List");
        mIViewDFHStripProcess = new JMenuItem("View StripDFHProcess List");
        mIOPCServerIP.addActionListener(li);
        mISettingsForFuelRange.addActionListener(li);
        mISettingsForFieldData.addActionListener(li);
        mIViewDFHStripProcess.addActionListener(li);
        mIEditDFHStripProcess.addActionListener(li);
        mIUpdateFurnace = new JMenuItem("Update Furnace");
        mIUpdateFurnace.addActionListener(li);
    }

    protected JMenu createDefineFurnaceMenu() {
        defineDefineFurnaceMenu();
        defineFurnaceMenu.add(mIInputData);
        defineFurnaceMenu.add(mIOpData);
        return defineFurnaceMenu;
    }

    protected JMenu createPerformanceMenu() {
        definePerformanceMenu();
        perfMenu.add(mICreatePerfBase);
        perfMenu.add(mIAddToPerfBase);
        perfMenu.addSeparator();
        perfMenu.add(mISetPerfTablelimits);
        mISetPerfTablelimits.setVisible(false);
        perfMenu.addSeparator();
        perfMenu.add(mIShowPerfBase);
        perfMenu.addSeparator();
        perfMenu.add(mIClearPerfBase);
        return perfMenu;
    }

    protected JMenu createL2ConfMenu() {
        mL2Configuration = new JMenu("L2 Config");
        if (!onProductionLine) {
            mL2Configuration.add(mIOPCServerIP);
            mL2Configuration.addSeparator();
            mL2Configuration.add(mISettingsForFuelRange);
            mL2Configuration.add(mISettingsForFieldData);
            mL2Configuration.addSeparator();
        }
        mL2Configuration.add(mIViewDFHStripProcess);
        mL2Configuration.add(mIEditDFHStripProcess);
        mL2Configuration.setEnabled(true);
        return mL2Configuration;
    }

    @Override
    protected JMenuBar assembleMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.add(createFileMenu());
        mb.add(createDefineFurnaceMenu());
        mb.add(createShowResultsMenu());
        mb.add(createPerformanceMenu());
        mb.add(pbEdit);
        mb.add(createL2ConfMenu());
        return mb;
    }

    protected boolean updateFurnace() {
        saveFurnaceWithNowProfileCode();
        return true;
    }

    protected ProductionData defineProduction() {
        return new ProductionData(getDFHProcess());
    }

    public FceEvaluator calculateFce(boolean bResetLossFactor, ResultsReadyListener resultsReadyListener, String subAction) {
        FceEvaluator eval = calculateFce(bResetLossFactor, resultsReadyListener);
        if (eval != null)
            eval.setSubActionTitle(subAction);
        return eval;
    }

    class L2MenuListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object caller = e.getSource();
            if (caller == mIUpdateFurnace)
                updateFurnace();
            else if (caller == mIViewDFHStripProcess)
                viewStripDFHProcess();
            else if (caller == mIEditDFHStripProcess)
                editStripDFHProcess();
            else if (caller == mISettingsForFuelRange)
                fuelRangeSettings();
            else if (caller == mISettingsForFieldData)
                fieldDataSettings();
            else if (caller == mIOPCServerIP)
                setOPCIP();
        }
    }
}