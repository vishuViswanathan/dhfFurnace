package directFiredHeating.applications;

import basic.ChMaterial;
import basic.Fuel;
import directFiredHeating.DFHTuningParams;
import directFiredHeating.DFHeating;
import directFiredHeating.accessControl.L2AccessControl;
import directFiredHeating.accessControl.OfflineAccessControl;
import directFiredHeating.process.OneStripDFHProcess;
import directFiredHeating.process.StripDFHProcessList;
import directFiredHeating.stripDFH.StripFurnace;
import mvUtils.display.ErrorStatAndMsg;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.StatusWithMessage;
import mvUtils.jnlp.JNLPFileHandler;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.DecimalFormat;
import java.util.Vector;

/**
 * User: M Viswanathan
 * Date: 02-May-16
 * Time: 1:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class Level2Configurator extends DFHeating {

    protected String fceDataLocation = "level2FceData/mustBeUserEntry/";
    boolean associatedDataLoaded = false;
    OfflineAccessControl accessControl;

    public Level2Configurator() {
        super();
        bL2Configurator = true;
        enableSpecsSave = true;
        onProductionLine = false;
        bAllowProfileChange = true;
        bAllowManualCalculation = true;
        asApplication = true;
        releaseDate = "20160712 15:00";
    }

    public boolean setItUp() {
        modifyJTextEdit();
        fuelList = new Vector<Fuel>();
        vChMaterial = new Vector<ChMaterial>();
        dfhProcessList = new StripDFHProcessList(this);
        setUIDefaults();
        mainF = new JFrame();
        mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        try {
            accessControl = new OfflineAccessControl(asJNLP, mainF);
            mainF.setTitle("DFH Furnace - Level2 Configurator - " + releaseDate + testTitle);

            tuningParams = new DFHTuningParams(this, false, 1, 5, 30, 1.12, 1, false, false);
            furnace = new StripFurnace(this, false, false, lNameListener);
            furnace.setTuningParams(tuningParams);
            tuningParams.bConsiderChTempProfile = true;
            createUIs(false); // without the default menuBar
            mISetPerfTablelimits.setVisible(true);
            disableSomeUIs();
            setDefaultSelections();
            addMenuBar(createL2MenuBar());
            associatedDataLoaded = true;
            setTestData();
            switchPage(DFHDisplayPageType.INPUTPAGE);
            displayIt();

            showMessage("Level2 Configurator - The furnace has to be of " + HeatingMode.TOPBOTSTRIP + " for " + DFHTuningParams.FurnaceFor.STRIP +
                    "\n\nIt is the responsibility of the user to ensure data integrity among:" +
                    "\n      1) Profile including Fuel type " +
                    "\n      2) IP address of OPC server  '" + mL2Configuration.getText() + "'" +
                    "\n      3) Fuel settings under '" + mL2Configuration.getText() + "'" +
                    "\n      4) DHFProcess List data under '" + mL2Configuration.getText() + "'" +
                    "\n      5) Performance Data under '" + perfMenu.getText() + "'" +
                    "\n\nIt is suggested that the profile with Fuel is finalised before updating" +
                    "\nthe other data." +
                    "\n\nBefore exiting, ensure that the Furnace data is saved/updated through 'File' menu.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Java Version :" + System.getProperty("java.version"));
        return associatedDataLoaded;
    }

    protected JPanel processPanel() {
        MultiPairColPanel jp = new MultiPairColPanel("Process");
        jp.addItemPair("Process Name", dfhProcessList.getListUI());
        return jp;
    }

    protected String getProcessName() {
        processName = dfhProcessList.getSelectedProcessName();
        return processName;
    }

    protected void disableSomeUIs() {
        tfMinExitZoneFceTemp.setEnabled(false);
        tfExitZoneFceTemp.setEnabled(false);
        tfExitTemp.setEnabled(false);
    }

    boolean checkProfileCode(String withThis) {
        return (withThis != null) && (profileCode.equals(withThis));
    }

    public String inputDataXML(boolean withPerformance) {
        return profileCodeInXML() + super.inputDataXML(withPerformance) +
                XMLmv.putTag("FuelSettings", furnace.fceSettingsInXML()) +
                XMLmv.putTag("dfhProcessList", stripDFHProcessListInXML());
    }

    protected boolean isProfileCodeOK() {
        return (profileCode.length() == profileCodeFormat.format(0).length());
    }

    protected ErrorStatAndMsg isChargeInFceOK() {    // TODO 20160622 considered that UI is already read
        ErrorStatAndMsg retVal = super.isChargeInFceOK();
        if ((furnaceFor() == DFHTuningParams.FurnaceFor.STRIP)) {
            OneStripDFHProcess oneProc = getDFHProcess();
            if (oneProc != null) {
                boolean someDecisionRequired = false;
                String toDecide = "<html><h4>Some parameters are set from Strip DFH Process <b>"+ oneProc.processName + "</b>";
                ChMaterial material = oneProc.getChMaterial(chThickness);
                if (material != selChMaterial) {
                    toDecide += "<blockQuote> # Charge Material selected is not matching for thickness <b>" + (chThickness * 1000) + "mm</b>.<p>" +
                            "&emsp;Will be changed to <b>" + material + "</b> as per DFH Strip Process</blockQuote>";
                    someDecisionRequired = true;
                }
                toDecide += "<blockQuote># Exit Temperature set as <b>" + oneProc.tempDFHExit + "</b></blockQuote>" +
                    "<blockQuote># Exit Zone Temperature set as <b>" + oneProc.getMaxExitZoneTemp() + "</b></blockQuote>" +
                        "<blockQuote># Minimum Exit Zone Temperature set as <b>" + oneProc.getMinExitZoneTemp() + "</b></blockQuote>";
                toDecide += "</html>";
                boolean proceed = true;
                String title = "Strip DFH Process";
                if (someDecisionRequired)
                    proceed = decide(title, toDecide);
                else
                    showMessage(title, toDecide);
                if (proceed) {
                    setChMaterial(material);
                    setExitTemperaure(oneProc.tempDFHExit);
                    setExitZoneTemperatures(oneProc.getMaxExitZoneTemp(), oneProc.getMinExitZoneTemp());
                }
                else
                    retVal.addErrorMsg("\n   ABORTING");
            }
            else {
                    retVal.addErrorMsg("\n   Process not available in DFH Process List");
            }
        }
        return retVal;
    }

    public boolean checkProfileCodeInXML(String xmlStr) {
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, profileCodeTag);
        return checkProfileCode(vp.val);
    }

    String profileCodeInXML() {
        return XMLmv.putTag(profileCodeTag, profileCode);
    }

    protected void setFcefor(boolean showSuggestion) {
        super.setFcefor(showSuggestion);
        disableSomeUIs();
    }

    public StatusWithMessage takeProfileDataFromXML(String xmlStr) {
        StatusWithMessage retVal = new StatusWithMessage();
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, profileCodeTag);
        profileCode = vp.val;
        if (isProfileCodeOK() || bL2Configurator) {
            retVal = super.takeProfileDataFromXML(xmlStr, true, HeatingMode.TOPBOTSTRIP,
                    DFHTuningParams.FurnaceFor.STRIP);
            if (retVal.getDataStatus() == StatusWithMessage.DataStat.OK) {
                furnace.clearAssociatedData();
                vp = XMLmv.getTag(xmlStr, "FuelSettings");
                if (vp.val.length() <  10 || !furnace.takeFceSettingsFromXML(xmlStr)  )
                    retVal.addInfoMessage("No Fuel settings data available");
                vp = XMLmv.getTag(xmlStr, "dfhProcessList");
                if (vp.val.length() <  10 || !takeStripProcessListFromXML(xmlStr)  )
                    retVal.addInfoMessage("No Strip Process Data available");
            }
        }
        else
            retVal.addErrorMessage("ERROR: Not a Level2 Furnace Profile");
        return retVal;
    }

    void clearAssociatedData() {
        if (furnace != null)
            furnace.clearAssociatedData();
        if (dfhProcessList != null)
            dfhProcessList.clear();
    }

    void saveFurnaceWithNowProfileCode() {
        changeProfileCode = false;
        saveFceToFile(true);
        changeProfileCode = true;
    }

    protected void saveFceToFile(boolean withPerformance) {
        takeValuesFromUI();
        StatusWithMessage fceSettingsIntegrity = furnace.furnaceSettings.checkIntegrity();
        if (fceSettingsIntegrity.getDataStatus() == StatusWithMessage.DataStat.OK) {
            if (asJNLP)
                saveFceToFileJNLP(withPerformance);
            else {
                String title = "Save Level2 Furnace Data";
                FileDialog fileDlg =
                        new FileDialog(mainF, title,
                                FileDialog.SAVE);
                boolean profileCodeChanged = createProfileCode();
                String promptFile = (profileCodeChanged) ?
                        (profileCode + " FurnaceProfile.dfhDat") :
                        profileFileName;
                fileDlg.setFile(promptFile);
                fileDlg.setVisible(true);

                String bareFile = fileDlg.getFile();
                if (!(bareFile == null)) {
                    int len = bareFile.length();
                    if ((len < 8) || !(bareFile.substring(len - 7).equalsIgnoreCase(".dfhDat"))) {
                        showMessage("Adding '.dfhDat' to file name");
                        bareFile = bareFile + ".dfhDat";
                    }
                    String fileName = fileDlg.getDirectory() + bareFile;
//            debug("Save Data file name :" + fileName);
                    File f = new File(fileName);
                    boolean goAhead = true;
                    if (goAhead) {
                        try {
                            BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(fileName));
                            oStream.write(inputDataXML(withPerformance).getBytes());
                            oStream.close();
                            if (withPerformance)
                                furnace.performanceIsSaved();
                        } catch (FileNotFoundException e) {
                            showError("File " + fileName + " NOT found!");
                        } catch (IOException e) {
                            showError("Some IO Error in writing to file " + fileName + "!");
                        }
                    }
                }
                parent().toFront();
            }
        }else
            showError("Problem in Fuel Settings :\n" + fceSettingsIntegrity.getErrorMessage());
    }

    protected void saveFceToFileJNLP(boolean withPerformance) {
        boolean profileCodeChanged = createProfileCode();
        String fileName = (profileCodeChanged) ?
                                (profileCode + " FurnaceProfile.dfhDat") :
                                profileFileName;
        if (!JNLPFileHandler.saveToFile(inputDataXML(withPerformance), "dfhDat", fileName))
            showError("Some IO Error in writing to file!");
        else {
            if (withPerformance)
                furnace.performanceIsSaved();
        }
        parent().toFront();
    }

    protected StatusWithMessage getFceFromFile() {
        StatusWithMessage retVal = super.getFceFromFile();
        if (retVal.getDataStatus() != StatusWithMessage.DataStat.WithErrorMsg) {
            if (!isProfileCodeOK()) {
                showMessage("No ProfileCode found." +
                        "\n   Preparing default Zonal Fuel Range Data" +
                        "\n   This must be updated and saved");
                clearAssociatedData();
            }
        }
        return retVal;
    }

    String profileCode = "";
    boolean changeProfileCode = true;
    String profileCodeTag = "profileCode";
    DecimalFormat profileCodeFormat = new DecimalFormat("000000");

    boolean createProfileCode() {
        debug("changeProfileCode: " + changeProfileCode);
        if (changeProfileCode || (profileCode.length() < 1)) {
            profileCode = profileCodeFormat.format(Math.random() * 999999.0);
            return true;
        }
        return false;
    }

    String stripDFHProcessListInXML() {
        return dfhProcessList.dataInXMl().toString();
    }

    boolean takeStripProcessListFromXML(String xmlStr) {
        return dfhProcessList.takeStripProcessListFromXML(xmlStr);
    }

    void editStripDFHProcess() {
        if (dfhProcessList.addStripDFHProcess(parent()))
            showMessage("Strip DFh Process List updated\n" +
                    "To make it effective in Level2 RUNTIME, the list must be saved to file\n" +
                    "       " + mL2FileMenu.getText() + "->" + mIUpdateFurnace.getText());
    }

    void viewStripDFHProcess() {
        dfhProcessList.viewStripDFHProcess(parent());
    }

    void setOPCIP () {
        if (furnace.furnaceSettings.getOPCServerIP(this)) {
            showMessage("Furnace connection Data is modified\n" +
                    "To be effective in Level2 RUNTIME:\n" +
                    "Save data to file with " + mL2FileMenu.getText() + "->" + mIUpdateFurnace.getText());
        }
    }

    void createFceSetting() {
        if (furnace.showEditFceSettings(true)) {
            showMessage("Furnace Fuel Data is modified\n" +
                    "To be effective in Level2 RUNTIME:\n" +
                    "    1) Save data to file with " + mL2FileMenu.getText() + "->" + mIUpdateFurnace.getText() + "\n" +
                    "    2) Restart Level2 RUNTIME, if already running");
        }
    }

//    FileSystemView currentView;

    boolean updateFurnace() {
        saveFurnaceWithNowProfileCode();
        return true;
    }

    JMenu mL2FileMenu;
    JMenuItem mISaveFurnace;
    JMenuItem mILoadFurnace;
    JMenuItem mIUpdateFurnace;
    
    JMenu mL2Configuration;
    JMenuItem mIOPCServerIP;
    JMenuItem mIEditDFHStripProcess;
    JMenuItem mIViewDFHStripProcess;
    JMenuItem mICreateFceSettings;

    JMenuBar menuBarLevel2;

    JMenu mAccessControl;
    JMenuItem mIDeleteInstallerAccess;
    JMenuItem mIAddInstallerAccess;
    JMenuItem mILoadAccessFile;
    JMenuItem mISaveAccessFile;
    boolean accessDataModified = false;

    JMenu createL2FileMenu(ActionListener li) {
        mL2FileMenu = new JMenu("File");
        mISaveFurnace = new JMenuItem("Save Furnace");
        mISaveFurnace.addActionListener(li);
        mILoadFurnace = new JMenuItem("Load Furnace");
        mILoadFurnace.addActionListener(li);
        mIUpdateFurnace = new JMenuItem("Update Furnace");
        mIUpdateFurnace.addActionListener(li);
        mL2FileMenu.add(mILoadFurnace);
        mL2FileMenu.add(mISaveFurnace);
        mL2FileMenu.add(mIUpdateFurnace);
        mL2FileMenu.addSeparator();
        mL2FileMenu.add(mISaveFuelSpecs);

        mL2FileMenu.add(mISaveSteelSpecs);
        mL2FileMenu.add(mIExit);
        return mL2FileMenu;
    }

    JMenuBar createL2MenuBar() {
        // Default menus
        L2MenuListener li = new L2MenuListener();
        menuBarLevel2 = new JMenuBar();
        menuBarLevel2.add(createL2FileMenu(li));
        menuBarLevel2.add(inputMenu);
        menuBarLevel2.add(resultsMenu);
        menuBarLevel2.add(perfMenu);
        mL2Configuration = new JMenu("L2 Config");
        mIOPCServerIP = new JMenuItem("Set OPC server IP");
        mICreateFceSettings = new JMenuItem("View/Edit Zonal Fuel Range");

        mIEditDFHStripProcess = new JMenuItem("Add/Edit StripDFHProcess List");
        mIViewDFHStripProcess = new JMenuItem("View StripDFHProcess List");
        mIOPCServerIP.addActionListener(li);
        mICreateFceSettings.addActionListener(li);
        mIViewDFHStripProcess.addActionListener(li);
        mIEditDFHStripProcess.addActionListener(li);
        if (!onProductionLine)
            mL2Configuration.add(mIOPCServerIP);
        mL2Configuration.add(mICreateFceSettings);
        mL2Configuration.addSeparator();
        mL2Configuration.add(mIViewDFHStripProcess);
        mL2Configuration.add(mIEditDFHStripProcess);
        mL2Configuration.addSeparator();
        mL2Configuration.addSeparator();
        mL2Configuration.setEnabled(true);
        menuBarLevel2.add(mL2Configuration);
        menuBarLevel2.add(mL2Configuration);
        menuBarLevel2.add(pbEdit);

        mAccessControl = new JMenu("Access Control");
        mILoadAccessFile = new JMenuItem("Load Access File");
        mIAddInstallerAccess = new JMenuItem("Add Installer Access");
        mIDeleteInstallerAccess = new JMenuItem("Delete one Installer");
        mISaveAccessFile = new JMenuItem("Save Access File");
        mILoadAccessFile.addActionListener(li);
        mIAddInstallerAccess.addActionListener(li);
        mIDeleteInstallerAccess.addActionListener(li);
        mISaveAccessFile.addActionListener(li);
        mAccessControl.add(mILoadAccessFile);
        mAccessControl.addSeparator();
        mAccessControl.add(mIAddInstallerAccess);
        mAccessControl.add(mIDeleteInstallerAccess);
        mAccessControl.addSeparator();
        mAccessControl.add(mISaveAccessFile);
        menuBarLevel2.add(mAccessControl);
        return menuBarLevel2;
    }

    public static void showProgress(String msg) {
    }

    boolean loadAccessFile() {
        accessDataModified = false;
        return (accessControl.loadAccessFile().getDataStatus() == StatusWithMessage.DataStat.OK);

    }

    boolean saveAccessFile() {
        accessDataModified = false;
        return (accessControl.saveAccessFile().getDataStatus() == StatusWithMessage.DataStat.OK);
    }

    void addInstallerAccess() {
        StatusWithMessage response =
            accessControl.addNewUser(L2AccessControl.AccessLevel.INSTALLER);
        if (response.getDataStatus() == StatusWithMessage.DataStat.OK)
            accessDataModified = true;
        else
            showError(response.getErrorMessage());
    }

    void deleteInstallerAccess() {
        StatusWithMessage response =
                accessControl.deleteUser(L2AccessControl.AccessLevel.INSTALLER);
        if (response.getDataStatus() == StatusWithMessage.DataStat.OK)
            accessDataModified = true;
        else
            showError(response.getErrorMessage());
    }

//    OneStripDFHProcess getDFHProcess() {
//        return getStripDFHProcess(getProcessName());
//    }
//
//    class DFHProcessListener implements ActionListener  {
//        public void actionPerformed(ActionEvent e) {
//            Object o = e.getSource();
//            if (o == tfProcessName) {
//                if (getDFHProcess() == null) {
//                    showError("Process not available in DFH Process List");
//                    tfProcessName.setText("");
//                }
//            }
//        }
//    }

    class L2MenuListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object caller = e.getSource();
            if (caller == mILoadFurnace)
                mIGetFceProfile.doClick();
            else if (caller == mISaveFurnace)
                mISaveFceProfile.doClick();
            else if (caller == mIUpdateFurnace)
                updateFurnace();
            else if (caller == mIViewDFHStripProcess)
                viewStripDFHProcess();
            else if (caller == mIEditDFHStripProcess)
                editStripDFHProcess();
            else if (caller == mICreateFceSettings)
                createFceSetting();
            else if (caller == mIOPCServerIP)
                setOPCIP();
            else if (caller == mIAddInstallerAccess)
                addInstallerAccess();
            else if (caller == mIDeleteInstallerAccess)
                deleteInstallerAccess();
            else if (caller == mILoadAccessFile)
                loadAccessFile();
            else if (caller == mISaveAccessFile)
                saveAccessFile();
        }
    }

    static protected boolean  parseCmdLineArgs(String[] args) {
        int i = 0;
        boolean retVal = false;
        if (DFHeating.parseCmdLineArgs(args)) {
            retVal = true;
        }
        return retVal;
//        return true;
    }

    public static void main(String[] args) {
        final Level2Configurator l2Preparer = new Level2Configurator();
        if (parseCmdLineArgs(args)) {
            l2Preparer.setItUp();
            if (!l2Preparer.associatedDataLoaded) {
                l2Preparer.showError(" Aborting ...");
                System.exit(1);
            }
        }
    }
}
