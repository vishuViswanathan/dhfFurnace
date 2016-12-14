package level2.applications;

import TMopcUa.TMuaClient;
import basic.*;
import directFiredHeating.*;
import directFiredHeating.applications.StripHeating;
import display.QueryDialog;
import directFiredHeating.accessControl.L2AccessControl;
import level2.fieldResults.FieldResults;
import level2.process.L2ProcessList;
import level2.stripDFH.L2DFHFurnace;
import directFiredHeating.process.OneStripDFHProcess;
import mvUtils.display.*;
import mvUtils.file.AccessControl;
import mvUtils.file.FileChooserWithOptions;
import mvUtils.math.BooleanWithStatus;
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
//    String fceDataLocation = "level2FceData\\";
    String l2BasePath = "";
    String accessDataFile;
    String lockPath;
    File lockFile;

    //    public enum AccessLevel {NONE, RUNTIME, UPDATER, EXPERT, CONFIGURATOR};
    static public L2AccessControl.AccessLevel accessLevel = L2AccessControl.AccessLevel.NONE;
    TMuaClient uaClient;
    static String uaServerURI;
    public L2DFHFurnace l2Furnace;
    public String equipment;
    L2AccessControl l2AccessControl;
    L2ProcessList l2ProcessList;

    public L2DFHeating(String equipment) {
        super();
//        fceDataLocation = "level2FceData\\";
        File folder = new File("");
        l2BasePath = folder.getAbsolutePath();
        fceDataLocation = l2BasePath + "\\level2FceData\\";
        accessDataFile = fceDataLocation + "l2AccessData.txt";
        lockPath = fceDataLocation + "Syncro.lock";
        bAtSite = true;
        bAllowProfileChange = false;
        userActionAllowed = false;
        releaseDate = "20161207";
        onProductionLine = true;
        asApplication = true;
        this.equipment = equipment;
        accessLevel = L2AccessControl.AccessLevel.RUNTIME;
        DataWithStatus accessFileStat = getAccessFilePath();
        if (accessFileStat.getStatus() != DataStat.Status.OK) {
            showError(accessFileStat.errorMessage + "\n\n   ABORTING");
            System.exit(1);
        }
    }

    String getStatusFileName(L2AccessControl.AccessLevel level)  {
        return fceDataLocation + l2AccessControl.getDescription(level) + " is ON";
    }

    DataWithStatus markIAmON()  {
        DataWithStatus<Boolean> retVal = new DataWithStatus<>();
        FileLock lock = getFileLock();
        if (lock != null) {
            File f = new File(getStatusFileName(accessLevel));
            if (!f.exists()) {
                try {
                    f.createNewFile();
                    BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(f));
                    String data = l2AccessControl.getDescription(accessLevel) +
                                " started on " + dateFormat.format(new Date()) + "\n";
                    oStream.write(data.getBytes());
                    oStream.close();
                    retVal.setValue(true);
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

    protected DataWithStatus<Boolean> isAnyRunning(L2AccessControl.AccessLevel[] levels) {
        DataWithStatus<Boolean> retVal = new DataWithStatus<>();
        retVal.setValue(false);
        FileLock lock = getFileLock();
        if (lock != null) {
            for (L2AccessControl.AccessLevel l:levels) {
                File f = new File(getStatusFileName(l));
                if (f.exists()) {
                    retVal.setValue(true);
                    break;
                }
            }
            releaseLock(lock);
        }
        else
            retVal.setErrorMsg("Unable to get the lock to get Status of Applications");
        return retVal;
    }

    protected DataWithStatus<Boolean> markIAmOFF()  {
        DataWithStatus<Boolean> retVal = new DataWithStatus<>();
        retVal.setValue(false);
        FileLock lock = getFileLock();
        if (lock != null) {
            File f = new File(getStatusFileName(accessLevel));
            if (f.exists()) {
                f.delete();
                retVal.setValue(true);
            }
            else
                retVal.setErrorMsg("This Application was not running");
            releaseLock(lock);
        }
        else
            retVal.setErrorMsg("Unable to get the lock to set Status of this Application ");
        return retVal;
    }

    DataWithStatus<String> getAccessFilePath() {
        DataWithStatus<String> pathStatus =
                FileChooserWithOptions.getOneExistingFilepath(fceDataLocation, L2AccessControl.l2AccessfileExtension, true);
        if (pathStatus.getStatus() == DataStat.Status.OK) {
            accessDataFile = pathStatus.getValue();
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
//        if (!asJNLP && (log == null)) {
            startLog4j();
//        }
        try {
            l2AccessControl = new L2AccessControl(AccessControl.PasswordIntensity.LOW, accessDataFile, true);
            mainF.setTitle("DFH Furnace " + l2AccessControl.getDescription(accessLevel) + " - " + releaseDate + testTitle);

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
            l2ProcessList = new L2ProcessList(this);
            dfhProcessList = l2ProcessList;
            createUIs(false); // without the default menuBar
            if (getFuelAndCharge()) {
                setDefaultSelections();
                StatusWithMessage statL2FceFile = getL2FceFromFile();
                if (statL2FceFile.getDataStatus() != DataStat.Status.WithErrorMsg) {
                    if (!onProductionLine || setupUaClient()) {
                        if (!onProductionLine || l2Furnace.basicConnectionToLevel1(uaClient)) {
                            StatusWithMessage status = l2Furnace.checkAndNoteAccessLevel();
                            if (status.getDataStatus() == DataStat.Status.OK) {
                                furnace.setWidth(width);
                                enableDataEdit();
                                l2MenuSet = true;
                                lockFile = new File(lockPath);
                                getFieldPerformanceList();
                                bProfileEdited = false;
                                if (onProductionLine) {
                                    if (l2Furnace.makeAllConnections()) {   // createL2Zones()) {
                                        connectProcessListToLevel1();
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
//                    lockFile = new File(lockPath);
                    displayIt();
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
            DataWithStatus stat =  markIAmON();
            if (stat.getStatus() != DataStat.Status.OK) {
                showError(stat.errorMessage);
            }
            sendProcessListToLevel1();
        }
        return l2SystemReady;
    }

    public void clearLevel1ProcessList() {
    }

    public boolean sendProcessListToLevel1() {
        return true;
    }

    public boolean connectProcessListToLevel1() {
        return true;
    }

//    protected String getProcessName() {
//        processName = dfhProcessList.getSelectedProcessName();
//        return processName;
//    }

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

        String s = l2AccessControl.getDescription(L2AccessControl.AccessLevel.RUNTIME);
        mRuntimeAccess = new JMenu("for " + s);
        mIAddRuntimeAccess = new JMenuItem("Add access");
        mIDeleteRuntimeAccess = new JMenuItem("Delete access");
        mIAddRuntimeAccess.addActionListener(li);
        mIDeleteRuntimeAccess.addActionListener(li);
        mRuntimeAccess.add(mIAddRuntimeAccess);
        mRuntimeAccess.add(mIDeleteRuntimeAccess);

        s = l2AccessControl.getDescription(L2AccessControl.AccessLevel.UPDATER);
        mUpdaterAccess = new JMenu("for " + s);
        mIAddUpdaterAccess = new JMenuItem("Add access");
        mIDeleteUpdaterAccess = new JMenuItem("Delete access");
        mIAddUpdaterAccess.addActionListener(li);
        mIDeleteUpdaterAccess.addActionListener(li);
        mUpdaterAccess.add(mIAddUpdaterAccess);
        mUpdaterAccess.add(mIDeleteUpdaterAccess);

        s = l2AccessControl.getDescription(L2AccessControl.AccessLevel.EXPERT);
        mExpertAccess = new JMenu("for " + s);
        mIAddExpertAccess = new JMenuItem("Add access");
        mIDeleteExpertAccess = new JMenuItem("Delete access");
        mIAddExpertAccess.addActionListener(li);
        mIDeleteExpertAccess.addActionListener(li);
        mExpertAccess.add(mIAddExpertAccess);
        mExpertAccess.add(mIDeleteExpertAccess);
    }


    protected JMenu createPerformanceMenu() {
        definePerformanceMenu();
//        perfMenu = new JMenu("Performance");
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

    protected void showPerfMenu(boolean show) {
        if (bEnablePerfMenu)
            perfMenu.setVisible(show);
    }

    synchronized public void enablePerfMenu(boolean ena) {
        if (bAllowUpdateWithFieldData) {
            super.enablePerfMenu(ena);
        }
    }

    protected MultiPairColPanel chargeDataPanel() {
        MultiPairColPanel jp = new MultiPairColPanel("Charge Details");
        jp.addItemPair(tfChThickness.getName(), tfChThickness);
        jp.addItemPair(labChLength, tfChLength);
//        jp.addItemPair("Charge Material", cbChMaterial);
        return jp;
    }

    protected MultiPairColPanel chargeInFurnacePanel() {
        MultiPairColPanel jp = new MultiPairColPanel("Charge In Furnace");
        jp.addItemPair(tfProduction);
        jp.addItemPair(tfEntryTemp);
        return jp;
    }

    protected MultiPairColPanel calCulDataPanel() {
        MultiPairColPanel jp = new MultiPairColPanel("");
        jp.addItemPair("", pbCalculate);
        mpCalcul = jp;
        return jp;
    }

//    protected JPanel processPanel() {
//        MultiPairColPanel jp = new MultiPairColPanel("Process");
//        jp.addItemPair("Process Name", dfhProcessList.getListUI());
//        return jp;
//    }

    protected FramedPanel titleAndFceCommon() {
        if (titleAndFceCommon == null) {
            MultiPairColPanel mp = new MultiPairColPanel("");
            mp.addItemPair("Reference ", tfReference);
            mp.addItemPair("Title ", tfFceTitle);
            mp.addItemPair("Customer ", tfCustomer);
            mp.setEnabled(false);
            titleAndFceCommon = mp;
        }
        return titleAndFceCommon;
    }

    protected JPanel OperationPage() {
        JPanel jp = new JPanel(new GridBagLayout());
        jp.setBackground(new JPanel().getBackground());
        GridBagConstraints gbcOP = new GridBagConstraints();

        gbcOP.anchor = GridBagConstraints.CENTER;
        gbcOP.gridx = 0;
        gbcOP.gridy = 0;
        gbcOP.insets = new Insets(0, 0, 0, 0);
        gbcOP.gridwidth = 1;
        gbcOP.gridy++;
        gbcOP.anchor = GridBagConstraints.EAST;
        jp.add(processPanel(), gbcOP);
        gbcOP.gridy++;
        jp.add(chargeDataPanel(), gbcOP);
        gbcOP.gridx = 1;
        gbcOP.gridy = 1;
        mpChInFce = chargeInFurnacePanel();
        jp.add(mpChInFce, gbcOP);
        gbcOP.gridy++;
        gbcOP.anchor = GridBagConstraints.SOUTHWEST;
        jp.add(calCulDataPanel(), gbcOP);
        return jp;
    }

    String performanceExtension = "perfData";

    public boolean savePerformanceDataToFile() {
        boolean retVal = false;
        if (isProfileCodeOK()) {
            FileChooserWithOptions fileDlg = new FileChooserWithOptions("Save Performance Data",
                    "Performance Data (*." + performanceExtension + ")", performanceExtension);
            fileDlg.setSelectedFile(new File(profileCode + " performanceData." + performanceExtension));
            fileDlg.setCurrentDirectory(new File(fceDataLocation));
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
                    oStream.write((profileCodeInXML() + stripDFHProcessListInXML() + furnace.performanceInXML()).getBytes());
                    oStream.close();
                    furnace.performanceIsSaved();
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
            l2Furnace.informProcessDataModified();
        } else if (!gotTheLock)
            showError("Facing some problem in getting Lock");
        else
            showError("Performance Data saved");
        return saved;
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
                        if (!takeStripProcessListFromXML(xmlStr)) {
                            showError("No Strip Process Data available");
                        }
                        else if (dfhProcessList.getCount() < 2) {
                            showError("Process List must have at least one entry");
                        }
                        else {
                            logInfo("Loading Performance Data");
                            if (furnace.takePerformanceFromXML(xmlStr)) {
                                if (showDebugMessages)
                                    showMessage("Performance Data loaded");
                                retVal = true;
                            }
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
                        if (!takeStripProcessListFromXML(xmlStr)) {
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

    void addAccess(L2AccessControl.AccessLevel forLevel) {
        if (authenticate()) {
            StatusWithMessage stm = l2AccessControl.addNewUser(forLevel);
            if (stm.getDataStatus() == DataStat.Status.OK)
                showMessage("User added for " + l2AccessControl.getDescription(forLevel));
            else
                showError(stm.getErrorMessage() + ": No user was added :");
        }
        else
            showError("You are not authorised to Add User for " +
                    l2AccessControl.getDescription(forLevel));
    }

    void deleteAccess(L2AccessControl.AccessLevel forLevel) {
        if (authenticate()) {
            StatusWithMessage stm = l2AccessControl.deleteUser(forLevel);
            if (stm.getDataStatus() == DataStat.Status.OK)
                showMessage("One User deleted for " + l2AccessControl.getDescription(forLevel));
            else
                showError(stm.getErrorMessage() + ": No user was deleted ");
        }
        else
            showError("You are not authorised to Delete User for " +
                l2AccessControl.getDescription(forLevel));
    }

    protected boolean authenticate() {
        boolean retVal = false;
        StatusWithMessage stm = l2AccessControl.authenticate(accessLevel, "Re-confirm authority");
        if (stm.getDataStatus() == DataStat.Status.OK)
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
        DataWithStatus<String> oneFile = FileChooserWithOptions.getOneExistingFilepath(fceDataLocation, profileFileExtension, true);
        StatusWithMessage retVal = new StatusWithMessage();
        if (oneFile.getStatus() == DataStat.Status.OK) {
            String filePath = oneFile.getValue();
            disableCompare();
            furnace.resetSections();
            furnace.resetLossAssignment();
            furnace.clearPerfBase();
            hidePerformMenu();
            retVal = getFceFromFceDatFile(filePath);
            if (retVal.getDataStatus() != DataStat.Status.WithErrorMsg) {
                profileFileName = (new File(filePath)).getName();
                switchPage(DFHDisplayPageType.INPUTPAGE);
            }
        }
        else
            retVal.addErrorMessage(oneFile.errorMessage);
        return retVal;
    }

    boolean getFuelAndCharge() {
        return (fuelSpecsFromFile(fceDataLocation + "FuelSpecifications.dfhSpecs") &&
                chMaterialSpecsFromFile(fceDataLocation + "ChMaterialSpecifications.dfhSpecs"));
    }

    public FceEvaluator calculateFce() {
        userActionAllowed = (accessLevel == L2AccessControl.AccessLevel.EXPERT || accessLevel == L2AccessControl.AccessLevel.INSTALLER);
        return super.calculateFce();
    }

    protected boolean getUserResponse(OneStripDFHProcess oneProc, ChMaterial material) {
        return true;
    }

    String profileCodeInXML() {
        return XMLmv.putTag(profileCodeTag, profileCode);
    }

    protected boolean getStripDFHProcessList(String basePath) {
        boolean retVal = false;
        File file = getParticularFile(basePath, profileCode, performanceExtension);
        if (file != null) {
            StatusWithMessage response = loadStripDFHProcessList(file);
            if (response.getDataStatus() == DataStat.Status.WithErrorMsg)
                showError("Updated Process List: " + response.getErrorMessage());
            else {
                l2Info("loaded Process List from file " + file);
                retVal = true;
            }
        } else
            showError("Unable to locate Performance data File for getting Process List");
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

    public ErrorStatAndMsg getFieldPerformanceList() {
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
            retVal = getFieldPerformanceList(fceDataLocation);
            releaseLock(lock);
            break;
        }
        if (!retVal.inError) {
            updateDisplay(DFHDisplayPageType.PERFOMANCELIST);
        } else if (!gotTheLock)
            retVal.addErrorMsg("Facing some problem in getting Lock to take Modified Performance Data");
        return retVal;
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
            boolean gotProcessList = false;
            ErrorStatAndMsg gotPerformanceData = new ErrorStatAndMsg();
            while (--count > 0) {
                if (!l2Furnace.isProcessDataBeingUsed()) {
                    l2Furnace.setProcessBeingUpdated(true);
                    clearLevel1ProcessList();
                    l2Furnace.logTrace("Reading Process Data");
                    gotProcessList = getStripDFHProcessList();
                    if (gotProcessList) {
                        l2Furnace.logTrace("Reading Performance Data");
                        gotPerformanceData = getFieldPerformanceList();
                    }
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (!gotProcessList)
                logInfo("Unable to read modified Process Data");
            if ((gotPerformanceData.inError))
                logError("Reading Performance data: " + gotPerformanceData.msg);
            l2Furnace.setProcessBeingUpdated(false);
            sendProcessListToLevel1();
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
                l2AccessControl.getDescription(accessLevel) + "?", false))
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
        DataWithStatus stat =  markIAmOFF();
        if (stat.getStatus() != DataStat.Status.OK) {
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
