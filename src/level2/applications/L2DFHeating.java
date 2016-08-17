package level2.applications;

import TMopcUa.TMuaClient;
import basic.*;
import directFiredHeating.*;
import directFiredHeating.applications.StripHeating;
import display.QueryDialog;
import directFiredHeating.accessControl.L2AccessControl;
import level2.fieldResults.FieldResults;
import level2.stripDFH.L2DFHFurnace;
import directFiredHeating.process.OneStripDFHProcess;
import level2.common.ReadyNotedBothL2;
import directFiredHeating.process.StripDFHProcessList;
import level2.common.Tag;
import level2.stripDFH.L2DFHZone;
import mvUtils.display.*;
import mvUtils.file.FileChooserWithOptions;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import org.apache.log4j.Logger;
import performance.stripFce.Performance;
import protection.MachineCheck;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 03-Jan-15
 * Time: 6:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2DFHeating extends StripHeating {
    public boolean bAllowUpdateWithFieldData = false;
    protected boolean bAllowL2Changes = false;

    public enum L2DisplayPageType {NONE, PROCESS, LEVEL2}

    public L2DisplayPageType l2DisplayNow = L2DisplayPageType.NONE;
    String fceDataLocation = "level2FceData\\";
    String l2BasePath;
    String accessDataFile;
    String lockPath;
    File lockFile;

    //    public enum AccessLevel {NONE, RUNTIME, UPDATER, EXPERT, CONFIGURATOR};
    static public L2AccessControl.AccessLevel accessLevel = L2AccessControl.AccessLevel.NONE;
    TMuaClient uaClient;
    static String uaServerURI;
    public L2DFHFurnace l2Furnace;
    public String equipment;
    L2AccessControl accessControl;

    public L2DFHeating(String equipment) {
        super();
        File folder = new File("");
        l2BasePath = folder.getAbsolutePath();
        fceDataLocation = l2BasePath + "\\" + fceDataLocation;
        accessDataFile = fceDataLocation + "l2AccessData.txt";
        lockPath = fceDataLocation + "Syncro.lock";
        bAtSite = true;
        bAllowProfileChange = false;
        userActionAllowed = false;
        releaseDate = "20160420";
        onProductionLine = true;
        asApplication = true;
        this.equipment = equipment;
        accessLevel = L2AccessControl.AccessLevel.RUNTIME;
        DataWithMsg accessFileStat = getAccessFilePath();
        if (accessFileStat.getStatus() != DataWithMsg.DataStat.OK) {
            showError(accessFileStat.errorMessage + "\n\n   ABORTING");
            System.exit(1);
        }
    }

    String getStatusFileName(L2AccessControl.AccessLevel level)  {
        return fceDataLocation + accessControl.getDescription(level) + " is ON";
    }

    DataWithMsg markIAmON()  {
        DataWithMsg retVal = new DataWithMsg();
        FileLock lock = getFileLock();
        if (lock != null) {
            File f = new File(getStatusFileName(accessLevel));
            if (!f.exists()) {
                try {
                    f.createNewFile();
                    BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(f));
                    String data = accessControl.getDescription(accessLevel) +
                                " started on " + dateFormat.format(new Date()) + "\n";
                    oStream.write(data.getBytes());
                    oStream.close();
                    retVal.setData(true);
                } catch (IOException e) {
                    retVal.setErrorMsg("Unable to set Status of this Application");
                }
            }
            else
                retVal.setErrorMsg("This application is already Running");
            releaseLock(lock);
        }
        else
            retVal.setErrorMsg("Unable to get the lock to set Status of this Application ");
        return retVal;
    }

    DataWithMsg markIAmOFF()  {
        DataWithMsg retVal = new DataWithMsg();
        FileLock lock = getFileLock();
        if (lock != null) {
            File f = new File(getStatusFileName(accessLevel));
            if (f.exists()) {
                f.delete();
                retVal.setData(true);
            }
            else
                retVal.setErrorMsg("This Application was not running");
            releaseLock(lock);
        }
        else
            retVal.setErrorMsg("Unable to get the lock to set Status of this Application ");
        return retVal;
    }

    DataWithMsg getAccessFilePath() {
        DataWithMsg pathStatus =
                FileChooserWithOptions.getOneExistingFilepath(fceDataLocation, L2AccessControl.l2AccessfileExtension, true);
        if (pathStatus.getStatus() == DataWithMsg.DataStat.OK) {
            accessDataFile = pathStatus.stringValue;
        }
        return pathStatus;
    }

    public static L2AccessControl.AccessLevel defaultLevel() {
        return L2AccessControl.AccessLevel.RUNTIME;
    }

    public boolean l2SystemReady = false;

    public boolean isL2SystemReady() {
        return l2SystemReady;
    }

    protected void startLog4j() {
        log = Logger.getLogger("level2.L2DFHeating");
    }

    public boolean setItUp() {
        modifyJTextEdit();
        fuelList = new Vector<Fuel>();
        vChMaterial = new Vector<ChMaterial>();
        setUIDefaults();
        mainF = new JFrame();
        mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        if (!asJNLP && (log == null)) {
            startLog4j();
//            log = Logger.getLogger("level2.L2DFHeating"); //DFHeating.class);
            // Load Log4j configurations from external file
        }
        try {
            accessControl = new L2AccessControl(accessDataFile, true);
            mainF.setTitle("DFH Furnace " + accessControl.getDescription(accessLevel) + " - " + releaseDate + testTitle);

            tuningParams = new DFHTuningParams(this, onProductionLine, 1, 5, 30, 1.12, 1, false, false);
//        debug("Creating new Level2furnace");
            l2Furnace = new L2DFHFurnace(this, false, false, lNameListener);
            furnace = l2Furnace;
            if (!testMachineID()) {
                showError("Software key mismatch, Aborting ...");
                justQuit();
            }
            furnace.setTuningParams(tuningParams);
            tuningParams.bConsiderChTempProfile = true;
            dfhProcessList = new StripDFHProcessList(this);
            createUIs(false); // without the default menuBar
            if (getFuelAndCharge()) {
                setDefaultSelections();
                StatusWithMessage statL2FceFile = getL2FceFromFile();
                if (statL2FceFile.getDataStatus() != StatusWithMessage.DataStat.WithErrorMsg) {
                    if (!onProductionLine || setupUaClient()) {
                        if (!onProductionLine || l2Furnace.basicConnectionToLevel1(uaClient)) {
                            StatusWithMessage status = l2Furnace.checkAndNoteAccessLevel();
                            if (status.getDataStatus() == StatusWithMessage.DataStat.OK) {
                                furnace.setWidth(width);
                                enableDataEdit();
                                l2MenuSet = true;
                                getFieldPerformanceList();
                                bProfileEdited = false;
                                if (onProductionLine) {
                                    if (l2Furnace.makeAllConnections()) {   // createL2Zones()) {
                                        ErrorStatAndMsg connStat = l2Furnace.checkConnection();
                                        if (connStat.inError)
                                            showError(connStat.msg);
                                        else {
                                            l2Furnace.initForLevel2Operation();
                                            l2SystemReady = true;
                                        }
                                    }
                                } else
                                    l2SystemReady = true;
                            } else
                                showError(status.getErrorMessage());
                        } else
                            showError("Unable to start Level2 ERROR:001");
                    } else
                        showError("Unable to connect to OPC server : ERROR:002");
                } else
                    showError("Unable to load Furnace Profile : " + statL2FceFile.getErrorMessage());
                if (l2SystemReady) {
                    lockFile = new File(lockPath);
                    displayIt();
                    if (onProductionLine) {
                        l2Furnace.startL2DisplayUpdater();
                        if (accessLevel == L2AccessControl.AccessLevel.RUNTIME)
                            l2Furnace.startSpeedUpdater();
                    }
                    if (accessLevel == L2AccessControl.AccessLevel.RUNTIME || accessLevel == L2AccessControl.AccessLevel.UPDATER)
                        l2Furnace.enableDeleteInPerformanceData(false);
                    if (accessLevel == L2AccessControl.AccessLevel.INSTALLER) {
                        showMessage("It is the responsibility of the user to ensure data integrity among:" +
                                "\n      1) Profile including Fuel type " +
                                "\n      2) Fuel settings under '" + mL2Configuration.getText() + "'" +
                                "\n      3) DHFProcess List data under '" + mL2Configuration.getText() + "'" +
                                "\n      4) Performance Data under '" + perfMenu.getText() + "'" +
                                "\n\nIt is suggested that the profile with Fuel is finalised before updating" +
                                "\nthe other data." +
                                "\n\nBefore exiting, the the above data to be save to respective files." +
                                "\nThe Profile is to be saved first, followed by the others, since, the profile-save" +
                                "\nassigns a profile ID, which is used as link in other data files");
                    }
                }
            }
            else
                showError("Unable to load Fuel and/or Charge Material Specifications");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not find/load Access Data file :" + e.getMessage());
        }
        System.out.println("Java Version :" + System.getProperty("java.version"));
        if (l2SystemReady) {
            DataWithMsg stat =  markIAmON();
            if (stat.getStatus() != DataWithMsg.DataStat.OK) {
                showError(stat.errorMessage);
            }
        }
        return l2SystemReady;
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

    public boolean setSelectedProcess(OneStripDFHProcess selProc) {
        return dfhProcessList.setSelectedProcess(selProc);
    }

    protected void setDefaultSelections() {
        cbFuel.setSelectedIndex(0);
        if (cbChMaterial.getItemCount() == 1)
            cbChMaterial.setSelectedIndex(0);
    }

    //---------------- Create InternalZone ----------------------------
//    L2DFHZone internalZone;
//    Tag tagRuntimeReady;
//    Tag tagUpdaterReady;
//    Tag tagExpertReady;
//    ReadyNotedBothL2 performanceStat;
//    ReadyNotedBothL2 processDataStat;

    // =========================InternalZone=================================
    public void displayIt() {
        mainAppPanel.setPreferredSize(new Dimension(1200, 720));
        super.displayIt();
        if (onProductionLine)
            switchPage(L2DisplayPageType.PROCESS);
        else
            switchPage(DFHDisplayPageType.INPUTPAGE);
    }

    protected void switchPage(DFHDisplayPageType page) {
        l2DisplayNow = L2DisplayPageType.NONE;
        super.switchPage(page);
    }

    public void switchPage(L2DisplayPageType l2Display) {
        switch (l2Display) {
            case PROCESS:
                slate.setViewportView(l2Furnace.getFurnaceProcessPanel());
                l2DisplayNow = l2Display;
                break;
            case LEVEL2:
                slate.setViewportView(l2Furnace.getFurnaceLevel2Panel());
                l2DisplayNow = l2Display;
                break;
            default:
                l2DisplayNow = L2DisplayPageType.NONE;
        }
    }

    boolean l2MenuSet = false;

    JMenuItem mISavePerformanceData;
    JMenuItem mIReadPerformanceData;
    JMenuItem mIShowProcess;
    JMenuItem mIShowL2Data;
    JMenu mShowCalculation;

    JMenu mAccessControl;
    JMenu mRuntimeAccess;
    JMenuItem mIAddRuntimeAccess;
    JMenuItem mIDeleteRuntimeAccess;
    JMenu mUpdaterAccess;
    JMenuItem mIAddUpdaterAccess;
    JMenuItem mIDeleteUpdaterAccess;
    JMenu mExpertAccess;
    JMenuItem mIAddExpertAccess;
    JMenuItem mIDeleteExpertAccess;

    protected void createAllMenuItems() {
        super.createAllMenuItems();
        L2MenuListener li = new L2MenuListener();
        mISavePerformanceData = new JMenuItem("Save Performance Data");
        mISavePerformanceData.addActionListener(li);
        mIReadPerformanceData = new JMenuItem("Load Performance Data");
        mIReadPerformanceData.addActionListener(li);
        mShowCalculation = new JMenu("Show Calculation");
        mShowCalculation.addMenuListener(li);

        mIShowProcess = new JMenuItem("Process Data");
        mIShowProcess.addActionListener(li);
        mIShowL2Data = new JMenuItem("Level2 Data");
        mIShowL2Data.addActionListener(li);

        String s = accessControl.getDescription(L2AccessControl.AccessLevel.RUNTIME);
        mRuntimeAccess = new JMenu("for " + s);
        mIAddRuntimeAccess = new JMenuItem("Add access");
        mIDeleteRuntimeAccess = new JMenuItem("Delete access");
        mIAddRuntimeAccess.addActionListener(li);
        mIDeleteRuntimeAccess.addActionListener(li);
        mRuntimeAccess.add(mIAddRuntimeAccess);
        mRuntimeAccess.add(mIDeleteRuntimeAccess);

        s = accessControl.getDescription(L2AccessControl.AccessLevel.UPDATER);
        mUpdaterAccess = new JMenu("for " + s);
        mIAddUpdaterAccess = new JMenuItem("Add access");
        mIDeleteUpdaterAccess = new JMenuItem("Delete access");
        mIAddUpdaterAccess.addActionListener(li);
        mIDeleteUpdaterAccess.addActionListener(li);
        mUpdaterAccess.add(mIAddUpdaterAccess);
        mUpdaterAccess.add(mIDeleteUpdaterAccess);

        s = accessControl.getDescription(L2AccessControl.AccessLevel.EXPERT);
        mExpertAccess = new JMenu("for " + s);
        mIAddExpertAccess = new JMenuItem("Add access");
        mIDeleteExpertAccess = new JMenuItem("Delete access");
        mIAddExpertAccess.addActionListener(li);
        mIDeleteExpertAccess.addActionListener(li);
        mExpertAccess.add(mIAddExpertAccess);
        mExpertAccess.add(mIDeleteExpertAccess);
    }

    protected JMenu createPerformanceMenu() {
        perfMenu = new JMenu("Performance");
        perfMenu.add(mIAddToPerfBase);
        perfMenu.addSeparator();
        perfMenu.add(mIShowPerfBase);
        perfMenu.addSeparator();
        perfMenu.add(mISavePerformanceData);
        perfMenu.add(mIReadPerformanceData);
        return perfMenu;
    }

    JMenu createAccessMenu() {
        return null;
    }

    JMenu createLiveDisplayMenu() {
        JMenu jm = new JMenu("Live Displays");
        jm.add(mIShowProcess);
        jm.add(mIShowL2Data);
        return jm;
    }

    boolean bEnablePerfMenu;

    public void enableDataEdit() {
        if (bAllowL2Changes) {
            super.enableDataEdit();
        }
    }

    protected void showPerfMenu(boolean show) {
        if (bEnablePerfMenu)
            perfMenu.setVisible(show);
    }

    synchronized public void enablePerfMenu(boolean ena) {
        if (bAllowUpdateWithFieldData) {
            super.enablePerfMenu(ena);
        }
    }

    String stripDFHProcessListInXML() {
        return dfhProcessList.dataInXMl().toString();
    }

    boolean takeStripProcessListFromXML(String xmlStr) {
        return dfhProcessList.takeStripProcessListFromXML(xmlStr);
    }

    File fceDataLocationDirectory = new File(fceDataLocation);

    String performanceExtension = "perfData";

    public boolean savePerformanceDataToFile() {
        boolean retVal = false;
        if (isProfileCodeOK()) {
            FileChooserWithOptions fileDlg = new FileChooserWithOptions("Save Performance Data",
                    "Performance Data (*." + performanceExtension + ")", performanceExtension);
            fileDlg.setSelectedFile(new File(profileCode + " performanceData." + performanceExtension));
            fileDlg.setCurrentDirectory(fceDataLocationDirectory);
            fileDlg.setStartWithString(profileCode);
            if (currentView != null)
                fileDlg.setFileSystemView(currentView);
            if (fileDlg.showSaveDialog(parent()) == JFileChooser.APPROVE_OPTION) {
                File file = fileDlg.getSelectedFile();
                if (fileDlg.isItDuplicate()) {
                    String fName = file.getAbsolutePath();
                    markThisFileAsBak(file);
                    file = new File(fName);
                }
                deleteParticularFiles("" + fceDataLocation, profileCode, performanceExtension);
                try {
                    BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(file));
                    oStream.write(("# Performance Data saved on " + dateFormat.format(new Date()) + " \n\n").getBytes());
                    oStream.write((profileCodeInXML() + furnace.performanceInXML()).getBytes());
                    oStream.close();
                    retVal = true;
                } catch (FileNotFoundException e) {
                    showError("File " + file + " NOT found!");
                } catch (IOException e) {
                    showError("Some IO Error in writing to file " + file + "!");
                }
            }
            currentView = fileDlg.getFileSystemView();
        } else {
            showError("Save Profile before saving DFH Process List");
        }
        return retVal;
    }

    public boolean updatePerformanceDataFile() {
        boolean saved = false;
        FileLock lock;
        int count = 5;
        boolean gotTheLock = false;
        while (--count > 0) {
            lock = getTheLock();
            if (lock == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            gotTheLock = true;
            saved = savePerformanceDataToFile();
            releaseLock(lock);

            break;
        }
        if (onProductionLine && saved) {
            l2Furnace.informPerformanceDataModified();
            furnace.performanceIsSaved();
        } else if (!gotTheLock)
            showError("Facing some problem in getting Lock");
        else
            showError("Performance Data saved");
        return saved;
    }

    public ErrorStatAndMsg handleModifiedPerformanceData() {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg();
        boolean gotIt = false;
        FileLock lock;
        int count = 5;
        boolean gotTheLock = false;
        while (--count > 0) {
            lock = getTheLock();
            if (lock == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            gotTheLock = true;
            retVal = getFieldPerformanceList();
            releaseLock(lock);
            break;
        }
        if (!retVal.inError)
            updateDisplay(DFHDisplayPageType.PERFOMANCELIST);
        else if (!gotTheLock)
            retVal.addErrorMsg("Facing some problem in getting Lock to take Modified Performance Data");
        return retVal;
    }


    boolean loadSpecificPerformanceList() {
        boolean retVal = false;
        String title = "Read Performance Data";
        FileDialog fileDlg =
                new FileDialog(mainF, title,
                        FileDialog.LOAD);
        fileDlg.setFile("*." + performanceExtension);
        fileDlg.setVisible(true);
        String fileName = fileDlg.getFile();
        if (fileName != null) {
            String filePath = fileDlg.getDirectory() + fileName;
            retVal = loadSpecificPerformanceList(filePath);
        }
        parent().toFront();
        return retVal;
    }

    boolean loadSpecificPerformanceList(String filePath) {
        boolean retVal = false;
        if (!filePath.equals("nullnull")) {
//            debug("File for Performance Data:" + filePath);
            try {
                retVal = loadThePerformanceList(new File(filePath));
            } catch (Exception e) {
                showError("Some Problem in getting file!");
            }
        }
        return retVal;
    }

    boolean loadThePerformanceList(File file) {
        boolean retVal = false;
        try {
            BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(file));
            //           FileInputStream iStream = new FileInputStream(fileName);
            long len = file.length();
            if (len > 50 && len < 1e6) {
                int iLen = (int) len;
                byte[] data = new byte[iLen + 10];
                if (iStream.read(data) > 50) {
                    String xmlStr = new String(data);
                    if (checkProfileCodeInXML(xmlStr)) {
                        if (furnace.takePerformanceFromXML(xmlStr, true)) {
                            if (showDebugMessages)
                                showMessage("Performance Data loaded");
                            retVal = true;
                        }
                    } else
                        showError("mismatch in Performance Data file");
                }
                iStream.close();
            } else
                showError("File size " + len + " for " + file);
        } catch (Exception e) {
            showError("Some Problem in getting file!");
        }
        return retVal;
    }

    StatusWithMessage loadStripDFHProcessList(File file) {
        StatusWithMessage retVal = new StatusWithMessage();
        try {
            BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(file));
            //           FileInputStream iStream = new FileInputStream(fileName);
            long len = file.length();
            if (len > 50 && len < maxSizeOfProfileFile) {
                int iLen = (int) len;
                byte[] data = new byte[iLen + 10];
                if (iStream.read(data) > 50) {
                    String xmlStr = new String(data);
                    if (checkProfileCodeInXML(xmlStr)) {
                        ValAndPos vp;
                        vp = XMLmv.getTag(xmlStr, "dfhProcessList");
                        if (vp.val.length() < 10 || !takeStripProcessListFromXML(vp.val)) {
                            retVal.addErrorMessage("No Strip Process Data available");
                        }
                        else if (dfhProcessList.getCount() < 2) {
                            retVal.addErrorMessage("Process List must have at least one entry");
                        }
                    } else
                        retVal.addErrorMessage("mismatch in Furnace data file");
                }
            } else
                retVal.addErrorMessage("File size " + len + " for " + file);
        } catch (Exception e) {
            retVal.addErrorMessage("Some Problem in getting file for Process Data!");
        }
        return retVal;
    }

    FileSystemView currentView;
    String fceSettExtension = "fceSett";

    boolean markThisFileAsBak(File file) {
        String bakFilefullName = file.getAbsolutePath() + ".bak";
        File existingBakFile = new File(bakFilefullName);
        if (existingBakFile.exists())
            existingBakFile.delete();
        return file.renameTo(new File(bakFilefullName));
    }

    int deleteParticularFiles(String directory, final String startsWith, final String extension) {
        File folder = new File(directory);
        File[] files = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(extension) && name.startsWith(startsWith);
            }
        });
        for (File file : files)
            file.delete();
        return files.length;
    }

    boolean loadFurnaceSettings(File file) {
        boolean retVal = false;
        try {
            BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(file));
            //           FileInputStream iStream = new FileInputStream(fileName);
            long len = file.length();
            if (len > 50 && len < 10000) {
                int iLen = (int) len;
                byte[] data = new byte[iLen + 10];
                if (iStream.read(data) > 50) {
                    String xmlStr = new String(data);
                    if (checkProfileCodeInXML(xmlStr)) {
                        if (l2Furnace.takeFceSettingsFromXML(xmlStr)) {
                            l2Info("Zonal Fuel Range loaded");
//                            if (showDebugMessages)
//                                showMessage("Zonal Fuel Range loaded");
                            retVal = true;
                        }
                    } else
                        showError("Mismatch in Zonal Fuel Range Data");
                }
                iStream.close();
            } else
                showError("File size " + len + " for " + file);
        } catch (Exception e) {
            showError("Some Problem in getting Zonal Fuel Range File!");
        }
        return retVal;
    }

    void addAccess(L2AccessControl.AccessLevel forLevel) {
        if (authenticate()) {
            StatusWithMessage stm = accessControl.addNewUser(forLevel);
            if (stm.getDataStatus() == StatusWithMessage.DataStat.OK)
                showMessage("User added for " + accessControl.getDescription(forLevel));
            else
                showError(stm.getErrorMessage() + ": No user was added :");
        }
        else
            showError("You are not authorised to Add User for " +
                    accessControl.getDescription(forLevel));
    }

    void deleteAccess(L2AccessControl.AccessLevel forLevel) {
        if (authenticate()) {
            StatusWithMessage stm = accessControl.deleteUser(forLevel);
            if (stm.getDataStatus() == StatusWithMessage.DataStat.OK)
                showMessage("One User deleted for " + accessControl.getDescription(forLevel));
            else
                showError(stm.getErrorMessage() + ": No user was deleted ");
        }
        else
            showError("You are not authorised to Delete User for " +
                accessControl.getDescription(forLevel));
    }

    protected boolean authenticate() {
        boolean retVal = false;
        StatusWithMessage stm = accessControl.authenticate(accessLevel, "Re-confirm authority");
        if (stm.getDataStatus() == StatusWithMessage.DataStat.OK)
            retVal = true;
        return retVal;
    }

    public void setFieldProductionData(ProductionData pData, double airTemp, double fuelTemp) {
        fillChargeInFurnaceUI(pData);
        fillRecuDataUI(airTemp, fuelTemp);
    }

    public boolean setFieldProductionData(FieldResults oneFieldResult) {
        fillChargeInFurnaceUI(oneFieldResult.production);
        fillRecuDataUI(oneFieldResult.commonAirTemp, oneFieldResult.commonFuelTemp);
        setSelectedProcess(oneFieldResult.stripDFHProc);
        return true;
    }

    void fillChargeDetailsUI(Charge ch) {
        cbChType.setSelectedItem(ch.type);
        tfChDiameter.setData(ch.diameter * 1000);
        tfChWidth.setData(ch.width * 1000);
        tfChThickness.setData(ch.height * 1000);
        tfChLength.setData(ch.length * 1000);
        cbChMaterial.setSelectedItem(ch.chMaterial);
    }

    void fillChargeInFurnaceUI(ProductionData pData) {
        tfProcessName.setText(pData.processName);
        fillChargeDetailsUI(pData.charge);
        tfBottShadow.setData(pData.bottShadow * 100);
        tfChPitch.setData(pData.chPitch * 1000);
        tfChRows.setData(pData.nChargeRows);
        tfProduction.setData(pData.production / 1000);
        tfEntryTemp.setData(pData.entryTemp);
        tfExitTemp.setData(pData.exitTemp);
        tfDeltaTemp.setData(pData.deltaTemp);
        tfExitZoneFceTemp.setData(pData.exitZoneFceTemp);
        tfMinExitZoneFceTemp.setData(pData.minExitZoneTemp);
    }

    void fillRecuDataUI(double airTemp, double fuelTemp) {
        tfAirTemp.setData(airTemp);
        tfFuelTemp.setData(fuelTemp);
    }

    boolean setupUaClient() {
        boolean retVal = false;
        uaServerURI = furnace.furnaceSettings.getOPCip();
        if (uaServerURI != null) {
            try {
                l2debug("Before creating uaClient");
                uaClient = new TMuaClient(uaServerURI);
                l2debug("Before uaClient.connect");
                uaClient.connect();
                l2debug("uaClient is Connected");
                retVal = uaClient.isConnected();
            } catch (Exception e) {
                showError("Exception :" + e.getMessage());
                e.printStackTrace();
            }
        }
        return retVal;
    }

    protected void hidePerformMenu(){
        if (bEnablePerfMenu)
            super.hidePerformMenu();
    }

    StatusWithMessage getL2FceFromFile() {
        DataWithMsg oneFile = FileChooserWithOptions.getOneExistingFilepath(fceDataLocation, profileFileExtension, true);
        StatusWithMessage retVal = new StatusWithMessage();
        if (oneFile.getStatus() == DataWithMsg.DataStat.OK) {
            String filePath = oneFile.stringValue;
            disableCompare();
            furnace.resetSections();
            furnace.resetLossAssignment();
            furnace.clearPerfBase();
            hidePerformMenu();
            retVal = getFceFromFceDatFile(filePath);
            if (retVal.getDataStatus() != StatusWithMessage.DataStat.WithErrorMsg) {
                profileFileName = (new File(filePath)).getName();
                switchPage(DFHDisplayPageType.INPUTPAGE);
            }
        }
        return retVal;
    }

    boolean getFuelAndCharge() {
        return (fuelSpecsFromFile(fceDataLocation + "FuelSpecifications.dfhSpecs") &&
                chMaterialSpecsFromFile(fceDataLocation + "ChMaterialSpecifications.dfhSpecs"));
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

    public FceEvaluator calculateFce() {
        userActionAllowed = (accessLevel == L2AccessControl.AccessLevel.EXPERT || accessLevel == L2AccessControl.AccessLevel.CONFIGURATOR);
        return super.calculateFce();
    }

    boolean checkProfileCode(String withThis) {
        return (withThis != null) && (profileCode.equals(withThis));
    }

    protected boolean getUserResponse(OneStripDFHProcess oneProc, ChMaterial material) {
        return true;
    }

    public String inputDataXML(boolean withPerformance) {
        return profileCodeInXML() + super.inputDataXML(withPerformance) +
                XMLmv.putTag("FuelSettings", furnace.fceSettingsInXML()) +
                XMLmv.putTag("dfhProcessList", stripDFHProcessListInXML());
    }

    protected boolean isProfileCodeOK() {
        return (profileCode.length() == profileCodeFormat.format(0).length());
    }

    public boolean checkProfileCodeInXML(String xmlStr) {
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, profileCodeTag);
        return checkProfileCode(vp.val);
    }

    String profileCodeInXML() {
        return XMLmv.putTag(profileCodeTag, profileCode);
    }

    public StatusWithMessage takeProfileDataFromXML(String xmlStr) {
        StatusWithMessage retVal = new StatusWithMessage();
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, profileCodeTag);
        profileCode = vp.val;
        if (isProfileCodeOK()) {
            retVal = super.takeProfileDataFromXML(xmlStr, true, HeatingMode.TOPBOTSTRIP,
                    DFHTuningParams.FurnaceFor.STRIP);
            if (retVal.getDataStatus() == StatusWithMessage.DataStat.OK) {
                furnace.clearAssociatedData();
                vp = XMLmv.getTag(xmlStr, "FuelSettings");
                if (vp.val.length() < 10 || !furnace.takeFceSettingsFromXML(vp.val)) {
                    retVal.addErrorMessage("No Fuel settings data available");
                }
                vp = XMLmv.getTag(xmlStr, "dfhProcessList");
                if (vp.val.length() < 10 || !takeStripProcessListFromXML(vp.val)) {
                    retVal.addErrorMessage("No Strip Process Data available");
                }
                else if (dfhProcessList.getCount() < 1) {
                    retVal.addErrorMessage("Process List must have at least one entry");
                }
            }
        } else
            retVal.addErrorMessage("ERROR: Not a Level2 Furnace Profile");
        return retVal;
    }

    protected boolean updateFurnace() {
        boolean saved = false;
        FileLock lock;
        int count = 5;
        boolean gotTheLock = false;
        while (--count > 0) {
            lock = getTheLock();
            if (lock == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            gotTheLock = true;
            saved = saveFurnaceWithNowProfileCode();
            releaseLock(lock);
            break;
        }
        if (saved) {
            if (onProductionLine)
                l2Furnace.informProcessDataModified();
        }
        else if (!gotTheLock)
            showError("Facing some problem in getting Lock");
        else
            showError("Furnace Data not saved");
        return saved;
    }

    boolean saveFurnaceWithNowProfileCode() {
        changeProfileCode = false;
        saveFceToFile(true);
        changeProfileCode = true;
        return true;
    }

    protected void saveFceToFile(boolean withPerformance) {
        takeValuesFromUI();
        StatusWithMessage fceSettingsIntegrity = furnace.furnaceSettings.checkIntegrity();
        if (fceSettingsIntegrity.getDataStatus() == StatusWithMessage.DataStat.OK) {
            String title = "Save/Update Level2 Furnace Data";
            FileDialog fileDlg =
                    new FileDialog(mainF, title,
                            FileDialog.SAVE);
            boolean profileCodeChanged = createProfileCode();
            String promptFile = (profileCodeChanged) ?
                    (profileCode + " FurnaceProfile." + profileFileExtension) :
                    profileFileName;
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
        } else
            showError("Problem in Fuel Settings :\n" + fceSettingsIntegrity.getErrorMessage());
    }

    protected boolean getStripDFHProcessList(String basePath) {
        boolean retVal = false;
        File file = getParticularFile(basePath, profileCode, profileFileExtension);
        if (file != null) {
            StatusWithMessage response = loadStripDFHProcessList(file);
            if (response.getDataStatus() == StatusWithMessage.DataStat.WithErrorMsg)
                showError("Updated Process List: " + response.getErrorMessage());
            else {
                l2Info("loaded Process List from file " + file);
                retVal = true;
            }
        } else
            showError("Unable to locate Furnace data File for getting Process List");
        return retVal;
    }

    FileLock getFileLock() {
        FileLock lock = null;
        int count = 5;
        boolean gotTheLock = false;
        while (--count > 0) {
            lock = getTheLock();
            if (lock == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            break;
        }
        return lock;
    }

    protected boolean getStripDFHProcessList() {
        // to add lock here
        boolean gotIt = false;
        FileLock lock;
        int count = 5;
        boolean gotTheLock = false;
        while (--count > 0) {
            lock = getTheLock();
            if (lock == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            gotTheLock = true;
            gotIt = getStripDFHProcessList(fceDataLocation);
            releaseLock(lock);
            break;
        }
        if (gotIt) {
            logInfo("Process data read from file");
        } else {
            if (!gotTheLock)
                showError("unable to get fileLock for reading Process Data");
            else
                showError("Facing some problem in getting Lock to take Modified Process Data");
        }
        return gotIt;
    }

    protected boolean getFurnaceSettings(String basePath) {
        boolean retVal = false;
        File file = getParticularFile(basePath, profileCode, "fceSett");
        if (file != null) {
            retVal = loadFurnaceSettings(file);
            l2Info("loaded file " + file);
        } else
            showError("Unable to locate Zonal Fuel Range File");
        return retVal;
    }

    protected boolean getFurnaceSettings() {
        return getFurnaceSettings(fceDataLocation);
    }


    protected ErrorStatAndMsg getFieldPerformanceList(String basePath) {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg();
        File file = getParticularFile(basePath, profileCode, "perfData");
        if (file != null) {
            if (loadThePerformanceList(file))
                l2Info("Updated Performance Data from file " + file);
            else {
                retVal.addErrorMsg("Some problem in updating Performance Data from file " + file);
                l2Info(retVal.msg);
            }
        }
        return retVal;
    }

    protected ErrorStatAndMsg getFieldPerformanceList() {
        return getFieldPerformanceList(fceDataLocation);
    }

    File getParticularFile(String directory, final String startName, final String extension) {
        File selectedFile = null;
        File folder = new File(directory);
        File[] files = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (startName != null && startName.length() > 0)
                    return name.endsWith(extension) && name.startsWith(startName);
                else
                    return name.endsWith(extension);
            }
        });
        if (files.length > 0) {
            selectedFile = files[0];
            if (files.length > 1)
                l2Info("loading the first matching file " + files[0]);
        }
        return selectedFile;
    }

    public void resultsReady(Observations observations) {
        super.resultsReady(observations);
        l2Furnace.setCurveSmoothening(true);
    }

    public void resultsReady(Observations observations, DFHResult.Type switchDisplayTo) {
        userActionAllowed = false;
        super.resultsReady(observations, switchDisplayTo);
    }

    // machine ID methods
    boolean testMachineID() {
        boolean keyOK = false;
        boolean newKey = false;
        MachineCheck mc = new MachineCheck();
        String machineId = mc.getMachineID();
        String key = getKeyFromFile();
        do {
            if (key.length() < 5) {
                key = getKeyFromUser(machineId);
                newKey = true;
            }
            if (key.length() > 5) {
                StatusWithMessage keyStatus = mc.checkKey(key);
                boolean tryAgain = false;
                switch (keyStatus.getDataStatus()) {
                    case WithErrorMsg:
                        showError(keyStatus.getErrorMessage());
                        break;
                    case WithInfoMsg:
                        boolean response = decide("Software key", "There is some problem in the saved key\n"
                                + " Do you want to delete the earlier key data and enter the key manually?");
                        if (response) {
                            key = "";
                            tryAgain = true;
                        }
                        break;
                    default:
                        keyOK = true;
                        break;
                }
                if (tryAgain)
                    continue;
                if (keyOK && newKey)
                    saveKeyToFile(key);
            }
            break;
        } while (true);
        return keyOK;
    }

    String keyFileHead = "TMIDFHLevel2Key:";

    void saveKeyToFile(String key) {
        boolean done = false;
        String filePath = fceDataLocation + "machineKey.ini";
//        debug("Data file name for saving key:" + filePath);
        try {
            BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(filePath));
            oStream.write(keyFileHead.getBytes());
            oStream.write(key.getBytes());
            oStream.close();
            done = true;
        } catch (FileNotFoundException e) {
            l2Info("Could not create file " + filePath);
        } catch (IOException e) {
            l2Info("Some IO Error in writing to file " + filePath + "!");
        }
        if (done)
            l2Info("key saved to " + filePath);
        else
            showError("Unable to save software key");
    }

    String getKeyFromFile() {
        String key = "";
        String filePath = fceDataLocation + "machineKey.ini";
//        debug("Data file name :" + filePath);
        try {
            BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(filePath));
            //           FileInputStream iStream = new FileInputStream(fileName);
            File f = new File(filePath);
            long len = f.length();
            int headLen = keyFileHead.length();
            if (len > headLen && len < 100) {
                int iLen = (int) len;
                byte[] data = new byte[iLen];
                iStream.read(data);
                String dataStr = new String(data);
                if (dataStr.substring(0, headLen).equals(keyFileHead))
                    key = dataStr.substring(headLen);
            } else
                showError("File size " + len + " for " + filePath);
        } catch (Exception e) {
            showError("Some Problem in Key!");
        }
        return key;
    }

    public FileLock getTheLock() {
        FileLock lock = null;
        try {
            FileChannel channel = new RandomAccessFile(lockFile, "rw").getChannel();
            lock = channel.tryLock();
        } catch (IOException e) {
            lock = null;
        }
        return lock;
    }

    public void releaseLock(FileLock lock) {
        try {
            lock.release();
        } catch (IOException e) {
            showError("Some Error in releasing file Lock");
        }
    }

    public FceEvaluator calculateForPerformanceTable(Performance baseP) {
        return calculateForPerformanceTable(baseP, null);
    }


    public void handleModifiedProcessData() {
        ProcessModificationHandler theProcessModHandler = new ProcessModificationHandler();
        Thread t = new Thread(theProcessModHandler);
        t.start();
    }

    class ProcessModificationHandler implements Runnable {
        public void run() {
            int count = 5;
            boolean gotIt = false;
            while (--count > 0) {
                if (!l2Furnace.isProcessDataBeingUsed()) {
                    l2Furnace.setProcessBeingUpdated(true);
                    gotIt = getStripDFHProcessList();
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (!gotIt)
                logInfo("Unable to read modified Process Data");
            l2Furnace.setProcessBeingUpdated(false);
        }
    }

    public void setBusyInCalculation(boolean busy) {
        mShowCalculation.setEnabled(busy);
        super.setBusyInCalculation(busy);
    }


    void informLevel2Ready() {
        if (onProductionLine)
            l2Furnace.informLevel2Ready();
    }

    String getKeyFromUser(String machineID) {
        QueryDialog dlg = new QueryDialog(mainF, "Software keyString");
        JTextField mcID = new JTextField(machineID);
        mcID.setEditable(false);
        JTextField keyF = new JTextField(machineID.length() + 1);
        dlg.addQuery("Installation ID", mcID);
        dlg.addQuery("Enter key for the above installation ID", keyF);
        dlg.setLocationRelativeTo(parent());
        dlg.setVisible(true);
        if (dlg.isUpdated())
            return keyF.getText();
        else
            return "";
    }

    public static void l2debug(String msg) {
        if (log == null)
            System.out.println("" + (new Date()) + ": DEBUG: " + msg);
        else
            log.debug(accessLevel.toString() + ":" + msg);
    }

    public void l2Trace(String msg) {
        if (log == null)
            System.out.println("" + (new Date()) + ": TRACE: " + msg);
        else
            log.trace(accessLevel.toString() + ":" + msg);
    }

    public void l2Info(String msg) {
        if (log != null)
            log.info(accessLevel.toString() + ":" + msg);
    }

    public void l2Error(String msg) {
        if (log != null)
            log.error(accessLevel.toString() + ":" + msg);
    }

    public void checkAndClose(boolean check) {
        if (!check || decide("Quitting Level2", "Do you really want to Exit this " +
                accessControl.getDescription(accessLevel) + "?", false))
            exitFromLevel2();
    }

    void justQuit() {
        if (onProductionLine) {
            if (l2Furnace != null)
                l2Furnace.prepareForDisconnection();
            if (uaClient != null) {
                logInfo("Disconnecting uaClient");
                uaClient.disconnect();
            }
        }
        DataWithMsg stat =  markIAmOFF();
        if (stat.getStatus() != DataWithMsg.DataStat.OK) {
            showError(stat.errorMessage);
        }

        close();
    }

    public void exitFromLevel2() {
        if (canClose()) {
            justQuit();
        }
    }

    class L2MenuListener implements ActionListener, MenuListener {
        public void actionPerformed(ActionEvent e) {
            Object caller = e.getSource();
            if (caller == mIShowProcess)
                switchPage(L2DisplayPageType.PROCESS);
            else if (caller == mIShowL2Data)
                switchPage(L2DisplayPageType.LEVEL2);
            else if (caller == mISavePerformanceData)
                updatePerformanceDataFile();
            else if (caller == mIReadPerformanceData) {
                loadSpecificPerformanceList();
                updateDisplay(DFHDisplayPageType.PERFOMANCELIST);
            }
            else if (caller == mIAddExpertAccess)
                addAccess(L2AccessControl.AccessLevel.EXPERT);
            else if (caller == mIDeleteExpertAccess)
                deleteAccess(L2AccessControl.AccessLevel.EXPERT);
            else if (caller == mIAddUpdaterAccess)
                addAccess(L2AccessControl.AccessLevel.UPDATER);
            else if (caller == mIDeleteUpdaterAccess)
                deleteAccess(L2AccessControl.AccessLevel.UPDATER);
            else if (caller == mIAddRuntimeAccess)
                addAccess(L2AccessControl.AccessLevel.RUNTIME);
            else if (caller == mIDeleteRuntimeAccess)
                deleteAccess(L2AccessControl.AccessLevel.RUNTIME);
        }

        public void menuSelected(MenuEvent e) {
            Object caller = e.getSource();
            if (caller == mShowCalculation && isItBusyInCalculation()) {
//                info("showing progress page");
                switchPage(DFHDisplayPageType.PROGRESSPAGE);
            }
        }

        public void menuDeselected(MenuEvent e) {
        }

        public void menuCanceled(MenuEvent e) {
        }
    }

    public static void main(String[] args) {
        final L2DFHeating level2Heating = new L2DFHeating("Furnace");
        if (level2Heating.parseCmdLineArgs(args)) {
            if (!onProductionLine || level2Heating.setupUaClient()) {
                level2Heating.setItUp();
                if (level2Heating.l2SystemReady) {
                    level2Heating.informLevel2Ready();
                } else {
                    level2Heating.showError("Level2 could not be started. Aborting ...");
                    level2Heating.exitFromLevel2();
                }
            } else
                level2Heating.showMessage("Facing problem connecting to Level1. Aborting ...");
        }
    }
}
