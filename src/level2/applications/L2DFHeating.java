package level2.applications;

import TMopcUa.TMuaClient;
import basic.*;
import directFiredHeating.*;
import display.QueryDialog;
import directFiredHeating.accessControl.L2AccessControl;
import level2.stripDFH.L2DFHFurnace;
import directFiredHeating.process.OneStripDFHProcess;
import level2.common.ReadyNotedBothL2;
import directFiredHeating.process.StripDFHProcessList;
import level2.common.Tag;
import level2.stripDFH.L2DFHZone;
import mvUtils.display.ErrorStatAndMsg;
import mvUtils.display.FramedPanel;
import mvUtils.display.StatusWithMessage;
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
public class L2DFHeating extends DFHeating {
    public enum L2CommandLineArgs {
        CHANGEPROFILE("-changeProfile"),
        MANUALCALCULATIONS("-manual"),
        FURNACESETTINGS("-furnaceSettings"),
        DFHPROCESS("-dfhProcess"),
        FROMFIELD("-fromField"),
        L2DEBUG("-l2ShowDebugMessages"),
        SHOWALLMENU("-showAllMenu"),
        UNKNOWN("-UnKnown");
        private final String argName;

        L2CommandLineArgs(String argName) {
            this.argName = argName;
        }

        public String getValue() {
            return name();
        }

        @Override
        public String toString() {
            return argName;
        }

        public static L2CommandLineArgs getEnum(String text) {
            L2CommandLineArgs retVal = UNKNOWN;
            if (text != null) {
                for (L2CommandLineArgs b : L2CommandLineArgs.values()) {
                    if (text.equalsIgnoreCase(b.argName)) {
                        retVal = b;
                        break;
                    }
                }
            }
            return retVal;
        }
    }

    //    static boolean bAllowEditDFHProcess = false;
//    static boolean bAllowEditFurnaceSettings = false;
    static public boolean bAllowUpdateWithFieldData = false;
    static boolean bAllowL2Changes = false;
    static boolean bl2ShowDebugMessages = false;
    static boolean bShowAllmenu = false;
//    static boolean bL2Configurator = false;

    public enum L2DisplayPageType {NONE, PROCESS, LEVEL2}

    public L2DisplayPageType l2DisplayNow = L2DisplayPageType.NONE;
    String fceDataLocation = "level2FceData/";
    String accessDataFile = fceDataLocation + "l2AccessData.txt";
    String lockPath = fceDataLocation + "Syncro.lock";
    String opcIPFilePath = fceDataLocation + "opc.path";
    File lockFile;

    //    public enum AccessLevel {NONE, RUNTIME, UPDATER, EXPERT, CONFIGURATOR};
    static public L2AccessControl.AccessLevel accessLevel = L2AccessControl.AccessLevel.NONE;
    TMuaClient uaClient;
    static String uaServerURI;
    public L2DFHFurnace l2Furnace;
    public String equipment;
    StripDFHProcessList dfhProcessList;
    L2AccessControl accessControl;

    public L2DFHeating(String equipment) {
        super();
        bAllowProfileChange = false;
        userActionAllowed = false;
        releaseDate = "20160420";
        onProductionLine = true;
        asApplication = true;
        this.equipment = equipment;
        accessLevel = L2AccessControl.AccessLevel.RUNTIME;
        StatusWithMessage accessFileStat = getAccessFilePath();
        if (accessFileStat.getDataStatus() != StatusWithMessage.DataStat.OK) {
            showError(accessFileStat.getErrorMessage() + "\n\n   ABORTING");
            System.exit(1);
        }
    }

    public L2DFHeating(String equipment, boolean fromLauncher) {
        this(equipment);
        if (!onProductionLine || setupUaClient()) {
            setItUp();
            if (l2SystemReady) {
                informLevel2Ready();
            } else {
                showError("Level2 could not be started. Aborting ...");
                exitFromLevel2();
            }
        } else {
            showMessage("Facing problem connecting to Level1. Aborting ...");
            close();
        }
    }

    StatusWithMessage getAccessFilePath() {
        StatusWithMessage retVal = new StatusWithMessage();
        File folder = new File(fceDataLocation);
        File[] files = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith("." + L2AccessControl.fileExtension);
            }
        });
        if (files.length < 1) {
            retVal.setErrorMessage("Unable to load Access Control!");
        } else if (files.length > 1) {
            retVal.setErrorMessage("There are more than one Access Control data in the folder!");
        } else {
            accessDataFile = files[0].getAbsolutePath();
        }
        return retVal;
    }

    public static L2AccessControl.AccessLevel defaultLevel() {
        return L2AccessControl.AccessLevel.RUNTIME;
    }

    public boolean l2SystemReady = false;

    public boolean isL2SystemReady() {
        return l2SystemReady;
    }

    public boolean setItUp() {
        modifyJTextEdit();
        fuelList = new Vector<Fuel>();
        vChMaterial = new Vector<ChMaterial>();
        setUIDefaults();
        mainF = new JFrame();
        mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        if (!asJNLP && (log == null)) {
            log = Logger.getLogger("level2.L2DFHeating"); //DFHeating.class);
            // Load Log4j configurations from external file
        }
        try {
            accessControl = new L2AccessControl(accessDataFile, true);
            mainF.setTitle("DFH Furnace " + accessControl.getDescription(accessLevel) + " - " + releaseDate + testTitle);

            tuningParams = new DFHTuningParams(this, onProductionLine, 1, 5, 30, 1.12, 1, false, false);
//        debug("Creating new Level2furnace");
            l2Furnace = new L2DFHFurnace(this, false, false, lNameListener);
            if (!testMachineID()) {
                showError("Software key mismatch, Aborting ...");
                justQuit();
            }
            if (!onProductionLine || l2Furnace.basicConnectionToLevel1(uaClient)) {
                furnace = l2Furnace;
                StatusWithMessage status = l2Furnace.checkAndNoteAccessLevel();
                if (status.getDataStatus() == StatusWithMessage.DataStat.OK) {
//            debug("Created Level2furnace");
                    furnace.setTuningParams(tuningParams);
                    tuningParams.bConsiderChTempProfile = true;
                    createUIs(false); // without the default menuBar
                    getFuelAndCharge();
                    setDefaultSelections();
                    switchPage(DFHDisplayPageType.INPUTPAGE);
                    StatusWithMessage statL2FceFile = getL2FceFromFile();
                    if (statL2FceFile.getDataStatus() != StatusWithMessage.DataStat.WithErrorMsg) {
                        furnace.setWidth(width);
                        enableDataEdit();
                        addMenuBar(createL2MenuBar());
                        l2MenuSet = true;
                        getPerformanceList();
                        dfhProcessList = new StripDFHProcessList(this);
                        if (getStripDFHProcessList()) {
                            if (getFurnaceSettings()) {
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
                                showError("Problem in loading Furnace Settings");
                        } else
                            showError("Problem loading test StripDFHProcess list data");
                    } else
                        showError("Unable to load Furnace Profile : " + statL2FceFile.getErrorMessage());
                } else
                    showError(status.getErrorMessage());
            } else
                showError("Unable to start Level2 ERROR:001");
            if (l2SystemReady) {
                lockFile = new File(lockPath);
                displayIt();
                if (onProductionLine)
                    l2Furnace.startL2DisplayUpdater();
                if (accessLevel == L2AccessControl.AccessLevel.RUNTIME || accessLevel == L2AccessControl.AccessLevel.UPDATER)
                    l2Furnace.enableDeleteInPerformanceData(false);
                if (accessLevel == L2AccessControl.AccessLevel.CONFIGURATOR) {
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
        } catch (Exception e) {
            showError("Could not find/load Access Data file");
        }
        System.out.println("Java Version :" + System.getProperty("java.version"));
        return l2SystemReady;
    }

    boolean loadAssociatedData(String basePath) {
        boolean retVal = false;
        getPerformanceList(basePath);
        dfhProcessList = new StripDFHProcessList(this);
        if (getStripDFHProcessList(basePath)) {
            if (getFurnaceSettings(basePath)) {
                retVal = true;
            } else
                showError("Problem in loading Furnace Settings");
        } else
            showError("Problem loading test StripDFHProcess list data");
        return retVal;
    }

    //---------------- Create InternalZone ----------------------------
    L2DFHZone internalZone;
    Tag tagRuntimeReady;
    Tag tagUpdaterReady;
    Tag tagExpertReady;
    ReadyNotedBothL2 performanceStat;
    ReadyNotedBothL2 processDataStat;

    // =========================InternalZone=================================
    public void displayIt() {
        mainAppPanel.setPreferredSize(new Dimension(1200, 720));
        super.displayIt();
        if (onProductionLine)
            switchPage(L2DisplayPageType.PROCESS);
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

    JMenu mL2FileMenu;
    JMenuItem mISaveFurnace;
    JMenuItem mILoadFurnace;
    JMenuItem mIUpdateFurnace;

    JMenu mL2Configuration;
    JMenuItem mIOPCServerIP;
    JMenuItem mIEditDFHStripProcess;
    JMenuItem mIViewDFHStripProcess;
    //    JMenuItem mIReadDFHStripProcess;
//    JMenuItem mISaveDFHStripProcess;
    JMenuItem mICreateFceSettings;
//    JMenuItem mISaveFceSettings;
//    JMenuItem mIReadFceSettings;

    JMenuItem mICreateFieldResultsData;
    JMenuItem mISaveFieldResultsToFile;
    JMenuItem mILoadFieldResult;
    JMenuItem mILevel1FieldResults;
    JMenuItem mISaveAsFieldResult;

    JMenuItem mIEvalForFieldProduction;
    JMenuItem mIEvalWithFieldCorrection;

    boolean l2MenuSet = false;

    JMenuBar menuBarLevel2;
    JMenuItem mISavePerformanceData;
    JMenuItem mIReadPerformanceData;
    JMenuItem mIShowProcess;
    JMenuItem mIShowL2Data;
    JMenu mShowCalculation;

    JMenu mAccessControl;
    JMenuItem mIRuntimeAccess;
    JMenuItem mIUpdaterAccess;
    JMenuItem mIExpertAccess;

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
        mL2FileMenu.add(mIExit);
        return mL2FileMenu;
    }

    JMenuBar createL2MenuBar() {
        // Default menus
        L2MenuListener li = new L2MenuListener();
        menuBarLevel2 = new JMenuBar();
        menuBarLevel2.add(createL2FileMenu(li));
//        menuBarLevel2.add(fileMenu);
        boolean bShowRuntimeData = (accessLevel == L2AccessControl.AccessLevel.RUNTIME) ||
                (accessLevel == L2AccessControl.AccessLevel.UPDATER) ||
                (accessLevel == L2AccessControl.AccessLevel.EXPERT);
        boolean bAllowL2Changes = (accessLevel == L2AccessControl.AccessLevel.EXPERT) ||
                (accessLevel == L2AccessControl.AccessLevel.CONFIGURATOR);
        boolean bEnablePerfMenu = bAllowUpdateWithFieldData || (accessLevel == L2AccessControl.AccessLevel.CONFIGURATOR);
        if (bShowRuntimeData) {
            JMenu jm = new JMenu("Live Displays");
            mIShowProcess = new JMenuItem("Process Data");
            mIShowProcess.addActionListener(li);
            jm.add(mIShowProcess);
            mIShowL2Data = new JMenuItem("Level2 Data");
            mIShowL2Data.addActionListener(li);
            jm.add(mIShowL2Data);
            menuBarLevel2.add(jm);
        }
        mISetPerfTablelimits.setVisible(false);
        mIClearPerfBase.setVisible(false);

        if (bEnablePerfMenu) {
            mShowCalculation = new JMenu("Show Calculation");
            menuBarLevel2.add(mShowCalculation);
            mShowCalculation.addMenuListener(li);
            mISavePerformanceData = new JMenuItem("Save Performance Data");
            mISavePerformanceData.addActionListener(li);
            mIReadPerformanceData = new JMenuItem("Load Performance Data");
            mIReadPerformanceData.addActionListener(li);
            perfMenu.addSeparator();
            perfMenu.add(mISavePerformanceData);
            perfMenu.add(mIReadPerformanceData);
            menuBarLevel2.add(perfMenu);
        }
        if (bAllowL2Changes) {
            menuBarLevel2.add(inputMenu);
            menuBarLevel2.add(resultsMenu);
            mL2Configuration = new JMenu("L2 Config");
            mIOPCServerIP = new JMenuItem("Set OPC server IP");
            mICreateFceSettings = new JMenuItem("View/Edit Zonal Fuel Range");
//            mIReadFceSettings = new JMenuItem("Read Zonal Fuel Range from file");
//            mISaveFceSettings = new JMenuItem("Save Zonal Fuel Range to file");

            mIEditDFHStripProcess = new JMenuItem("Add/Edit StripDFHProcess List");
            mIViewDFHStripProcess = new JMenuItem("View StripDFHProcess List");
//            mIReadDFHStripProcess = new JMenuItem("Read StripDFHProcess List from File");
//            mISaveDFHStripProcess = new JMenuItem("Save StripDFHProcess List to File");
            mIOPCServerIP.addActionListener(li);
            mICreateFceSettings.addActionListener(li);
//            mIReadFceSettings.addActionListener(li);
//            mISaveFceSettings.addActionListener(li);
            mIViewDFHStripProcess.addActionListener(li);
            mIEditDFHStripProcess.addActionListener(li);
//            mISaveDFHStripProcess.addActionListener(li);
//            mIReadDFHStripProcess.addActionListener(li);
            if (!onProductionLine)
                mL2Configuration.add(mIOPCServerIP);
            mL2Configuration.add(mICreateFceSettings);
//            mL2Configuration.add(mIReadFceSettings);
//            mL2Configuration.add(mISaveFceSettings);
            mL2Configuration.addSeparator();
            mL2Configuration.add(mIViewDFHStripProcess);
            mL2Configuration.add(mIEditDFHStripProcess);
//            mL2Configuration.add(mIReadDFHStripProcess);
//            mL2Configuration.add(mISaveDFHStripProcess);
            mL2Configuration.addSeparator();
            mL2Configuration.setEnabled(true);
            menuBarLevel2.add(mL2Configuration);
            menuBarLevel2.add(pbEdit);
        }

        mAccessControl = new JMenu("Access Control");
        mIRuntimeAccess = new JMenuItem("for " + accessControl.getDescription(L2AccessControl.AccessLevel.RUNTIME));
        mIUpdaterAccess = new JMenuItem("for " + accessControl.getDescription(L2AccessControl.AccessLevel.UPDATER));
        mIExpertAccess = new JMenuItem("for " + accessControl.getDescription(L2AccessControl.AccessLevel.EXPERT));
        mIRuntimeAccess.addActionListener(li);
        mIUpdaterAccess.addActionListener(li);
        mIExpertAccess.addActionListener(li);
        mAccessControl.add(mIRuntimeAccess);
        mAccessControl.add(mIUpdaterAccess);
        mAccessControl.add(mIExpertAccess);
        if ((accessLevel == L2AccessControl.AccessLevel.UPDATER) ||
                (accessLevel == L2AccessControl.AccessLevel.EXPERT) ||
                (accessLevel == L2AccessControl.AccessLevel.CONFIGURATOR)) {
            menuBarLevel2.add(mAccessControl);
            switch (accessLevel) {
                case EXPERT:
                    mIExpertAccess.setEnabled(false);
                    break;
                case UPDATER:
                    mIExpertAccess.setEnabled(false);
                    mIUpdaterAccess.setEnabled(false);
                    break;
            }
        }


        return menuBarLevel2;
    }

    public void enablePerfMenu(boolean ena) {
        if (bAllowUpdateWithFieldData) {
            perfMenu.setEnabled(ena);
        }
    }

    FramedPanel panelLT;
    FramedPanel panelRT;
    FramedPanel panelLB;
    FramedPanel panelRB;
    JPanel theOuterPanel = new JPanel(new GridBagLayout());
    int SCREEENWIDTH = 1366;
    int SCREENHEIGHT = 768;

//    public DFHTuningParams getTuningParams() {
//        return tuningParams;
//    }

    void setStripProcessLookup() {
        dfhProcessList.clear();
        dfhProcessList.addOneDFHProcess(new OneStripDFHProcess(this, dfhProcessList, "FH", "CR Lo-C emiss 0.32", "CR Lo-C emiss 0.34", 550, 0.00015));
        dfhProcessList.addOneDFHProcess(new OneStripDFHProcess(this, dfhProcessList, "CQ", "CR Lo-C emiss 0.32", "CR Lo-C emiss 0.34", 620, 0.0002));
    }

    String stripDFHProcessListInXML() {
        return dfhProcessList.dataInXMl().toString();
    }

    boolean takeStripProcessListFromXML(String xmlStr) {
        return dfhProcessList.takeStripProcessListFromXML(xmlStr);
    }

    String stripProcExtension = "stripProc";
    File fceDataLocationDirectory = new File(fceDataLocation);

    public boolean updateProcessDataFile() {
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
            saved = saveDFHProcessList();
            releaseLock(lock);

            break;
        }
        if (onProductionLine && saved)
            l2Furnace.informProcessDataModified();
        else if (!gotTheLock)
            showError("Facing some problem in getting Lock");
        else
            showError("Process Data saved");
        return saved;
    }

    boolean saveDFHProcessList() {
        boolean retVal = false;
        if (isProfileCodeOK()) {
            FileChooserWithOptions fileDlg = new FileChooserWithOptions("Save StripDFHProcess list",
                    "StripDFHProcess List (*." + stripProcExtension + ")", stripProcExtension);
            fileDlg.setSelectedFile(new File(profileCode + " stripDFHProcessList." + stripProcExtension));
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
                deleteParticularFiles("" + fceDataLocation, profileCode, stripProcExtension);
                try {
                    BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(file));
                    oStream.write(("# StripDFhProcess List saved on " + dateFormat.format(new Date()) + " \n\n").getBytes());
                    oStream.write((profileCodeInXML() + stripDFHProcessListInXML()).getBytes());
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

    public boolean handleModifiedPerformanceData() {
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
            gotIt = getPerformanceList();
            releaseLock(lock);
            break;
        }
        if (gotIt)
            updateDisplay(DFHDisplayPageType.PERFOMANCELIST);
        else if (!gotTheLock)
            showError("Facing some problem in getting Lock to take Modified Performance Data");
        return gotIt;
    }


    boolean loadPerformanceData() {
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
            retVal = loadPerformanceData(filePath);
        }
        parent().toFront();
        return retVal;
    }

    boolean loadPerformanceData(String filePath) {
        boolean retVal = false;
        if (!filePath.equals("nullnull")) {
//            debug("File for Performance Data:" + filePath);
            try {
                retVal = loadPerformanceData(new File(filePath));
            } catch (Exception e) {
                showError("Some Problem in getting file!");
            }
        }
        return retVal;
    }

    boolean loadPerformanceData(File file) {
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
                        if (furnace.takePerformanceFromXML(xmlStr)) {
                            if (showDebugMessages)
                                showMessage("Performance Data loaded");
                            retVal = true;
                        }
                    } else
                        showError("mismatch in Performance Data file");
                }
            } else
                showError("File size " + len + " for " + file);
        } catch (Exception e) {
            showError("Some Problem in getting file!");
        }
        return retVal;
    }

    boolean loadStripDFHProcessList() {
        boolean retVal = false;
        String title = "Read StripDFHProcess list";
        FileDialog fileDlg =
                new FileDialog(mainF, title,
                        FileDialog.LOAD);
        fileDlg.setFile("*.stripProc");
        fileDlg.setVisible(true);
        String fileName = fileDlg.getFile();
        if (fileName != null) {
            String filePath = fileDlg.getDirectory() + fileName;
            retVal = loadStripDFHProcessList(filePath);
        }
        parent().toFront();
        return retVal;
    }

    boolean loadStripDFHProcessList(String filePath) {
        boolean retVal = false;
        if (!filePath.equals("nullnull")) {
//            debug("File for StripDFHProcess list :" + filePath);
            try {
                retVal = loadStripDFHProcessList(new File(filePath));
            } catch (Exception e) {
                showError("Some Problem in getting file!");
            }
        }
        return retVal;
    }

    boolean loadStripDFHProcessList(File file) {
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
                        if (takeStripProcessListFromXML(xmlStr)) {
                            l2Info("StripDFHProcess list loaded");
//                            if (showDebugMessages)
//                                showMessage("StripDFHProcess list loaded");
                            retVal = true;
                        }
                    } else
                        showError("mismatch in DFH Process List file");
                }
            } else
                showError("File size " + len + " for " + file);
        } catch (Exception e) {
            showError("Some Problem in getting file!");
        }
        return retVal;
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

    void setOPCIP() {

    }

    void createFceSetting() {
        if (l2Furnace.showEditFceSettings(true)) {
            showMessage("Furnace Fuel Data is modified\n" +
                    "To be effective in Level2 RUNTIME:\n" +
                    "    1) Save data to file with " + mL2FileMenu.getText() + "->" + mIUpdateFurnace.getText() + "\n" +
                    "    2) Restart Level2 RUNTIME, if already running");
        }
    }

    FileSystemView currentView;
    String fceSettExtension = "fceSett";

    boolean saveFurnaceSettings() {
        boolean retVal = false;
        if (isProfileCodeOK()) {
            String title = "Save Zonal Fuel Range";
            FileChooserWithOptions fileDlg = new FileChooserWithOptions(title, "Zonal Fuel Range (*." + fceSettExtension + ")", fceSettExtension);
            fileDlg.setSelectedFile(new File(profileCode + " Zonal Fuel Range." + fceSettExtension));
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
                deleteParticularFiles("" + fceDataLocation, profileCode, fceSettExtension);
                try {
                    BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(file));
                    oStream.write(("# Zonal Fuel Range saved on " + dateFormat.format(new Date()) + " \n\n").getBytes());
                    oStream.write((profileCodeInXML() + l2Furnace.fceSettingsInXML()).getBytes());
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
            showError("Save Profile before saving Zonal Fuel Range");
        }
        return retVal;
    }

    void markThisFileAsBak(File file) {
        String bakFilefullName = file.getAbsolutePath() + ".bak";
        File existingBakFile = new File(bakFilefullName);
        if (existingBakFile.exists())
            existingBakFile.delete();
        file.renameTo(new File(bakFilefullName));
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

    boolean readFurnaceSettings() {
        boolean retVal = false;
        String title = "Read StripDFHProcess list";
        FileDialog fileDlg =
                new FileDialog(mainF, title,
                        FileDialog.LOAD);
        fileDlg.setFile("*.fceSett");
        fileDlg.setVisible(true);
        String fileName = fileDlg.getFile();
        if (fileName != null) {
            String filePath = fileDlg.getDirectory() + fileName;
            retVal = loadFurnaceSettings(filePath);
        }
        parent().toFront();
        return retVal;
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

    boolean loadFurnaceSettings(String filePath) {
        boolean retVal = false;
        if (!filePath.equals("nullnull")) {
//            debug("File for Zonal Fuel Range :" + filePath);
            try {
                File file = new File(filePath);
                retVal = loadFurnaceSettings(file);
            } catch (Exception e) {
                showError("Some Problem in getting Fuel Range file " + filePath);
            }
        }
        return retVal;
    }

    boolean takeFieldResultsFromUser() {
        return l2Furnace.getFieldDataFromUser();
    }

    ErrorStatAndMsg takeResultsFromLevel1() {
        if (bAllowL2Changes) {
            mIEvalForFieldProduction.setEnabled(false);
            mIEvalWithFieldCorrection.setEnabled(false);
        }
//        boolean retVal = false;
        ErrorStatAndMsg stat = l2Furnace.takeFieldResultsFromLevel1(true);
        if (stat.inError) {
            if (bAllowL2Changes)
                mIEvalForFieldProduction.setEnabled(true);
            l2Info("ERROR in L2DFeating.takeResultsFromLevel1, in return from takeResultsFromLevel1 " + stat.msg);
        }
        return stat;
    }

    boolean loadFieldResults() {
        mIEvalForFieldProduction.setEnabled(false);
        mIEvalWithFieldCorrection.setEnabled(false);
        boolean retVal = false;
        String title = "Loading Field Results";
        FileDialog fileDlg =
                new FileDialog(mainF, title,
                        FileDialog.LOAD);
        fileDlg.setFile("*.fResult");
        fileDlg.setVisible(true);
        String fileName = fileDlg.getFile();
        if (fileName != null) {
            String filePath = fileDlg.getDirectory() + fileName;
            retVal = loadFieldResults(filePath);
        }
        parent().toFront();
        if (retVal) {
            mIEvalForFieldProduction.setEnabled(true);
        }
        return retVal;
    }

    void manageExpertAccess() {
        StatusWithMessage stm = accessControl.addNewUser(L2AccessControl.AccessLevel.EXPERT);
        if (stm.getDataStatus() == StatusWithMessage.DataStat.OK)
            showMessage("User added for " + accessControl.getDescription(L2AccessControl.AccessLevel.EXPERT));
        else
            showError("No user was added");
    }

    void manageUpdaterAccess() {
        StatusWithMessage stm = accessControl.authenticate(L2AccessControl.AccessLevel.EXPERT, "Re-confirm authority");
        if (stm.getDataStatus() == StatusWithMessage.DataStat.OK) {
            stm = accessControl.addNewUser(L2AccessControl.AccessLevel.UPDATER);
            if (stm.getDataStatus() == StatusWithMessage.DataStat.OK)
                showMessage("User added for " + accessControl.getDescription(L2AccessControl.AccessLevel.UPDATER));
            else
                showError("No user was added");
        } else
            showError("You are not authorised to add User for " +
                    accessControl.getDescription(L2AccessControl.AccessLevel.UPDATER));
    }

    void manageRuntimeAccess() {
        StatusWithMessage stm = accessControl.authenticate(accessLevel, "Re-confirm authority");
        if (stm.getDataStatus() == StatusWithMessage.DataStat.OK) {
            stm = accessControl.addNewUser(L2AccessControl.AccessLevel.RUNTIME);
            if (stm.getDataStatus() == StatusWithMessage.DataStat.OK)
                showMessage("User added for " + accessControl.getDescription(L2AccessControl.AccessLevel.RUNTIME));
            else
                showError("No user was added");
        } else
            showError("You are not authorised for this activity");
    }

    public void setFieldProductionData(ProductionData pData, double airTemp, double fuelTemp) {
        fillChargeInFurnaceUI(pData);
        fillRecuDataUI(airTemp, fuelTemp);
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

    public FceEvaluator evalForFieldProduction(ResultsReadyListener resultsReadyListener) {   // @TODO incomplete
        if (bAllowL2Changes)
            mIEvalWithFieldCorrection.setEnabled(true);
        if (l2Furnace.setFieldProductionData()) {
//            showMessage("Recu Specs maintained as original");
//            l2Furnace.newRecu();
            l2Furnace.setCurveSmoothening(false);
            return calculateFce(true, resultsReadyListener);
        } else
            return null;
    }

    public FceEvaluator evalForFieldProduction() {
        return evalForFieldProduction(null);
    }

    public FceEvaluator recalculateWithFieldCorrections(ResultsReadyListener resultsReadyListener) {   //  TODO not complete
        if (l2Furnace.adjustForFieldResults()) {
            FceEvaluator fceEvaluator = calculateFce(false, resultsReadyListener); // without reset the loss Factors
            if (bAllowL2Changes)
                mIEvalForFieldProduction.setEnabled(false);
            return fceEvaluator;
        } else
            return null;
    }

    boolean loadFieldResults(String filePath) {
        boolean retVal = false;
        try {
            BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(filePath));
            //           FileInputStream iStream = new FileInputStream(fileName);
            File f = new File(filePath);
            long len = f.length();
            if (len > 50 && len < 10000) {
                int iLen = (int) len;
                byte[] data = new byte[iLen + 10];
                if (iStream.read(data) > 50) {
                    if (l2Furnace.takeFieldResultsFromXML(new String(data))) {
                        showMessage("Field results loaded");
                        retVal = true;
                    }
                }
                iStream.close();
            } else
                showError("File size " + len + " for " + filePath);
        } catch (Exception e) {
            showError("Some Problem in getting file!");
        }
        return retVal;
    }

    boolean saveFieldResultsToFile() {
        showError("Not ready toSave Field Results to file yet");
        return false;
    }

    boolean saveAsFieldResults() {
        boolean retVal = false;
        if (bResultsReady) {
            String ext = ".fResult";
            String filePath = getSaveFilePath("Save as Field Results", ext);
            if (filePath.length() > ext.length())
                retVal = saveAsFieldResults(filePath);
        }
        return retVal;
    }

    boolean saveAsFieldResults(String filePath) {
        boolean retVal = false;
        try {
            BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(filePath));
            oStream.write(("# Field Results saved on " + dateFormat.format(new Date()) + " \n\n").getBytes());

            oStream.write(l2Furnace.fieldResultsInXML().toString().getBytes());
            oStream.close();
            retVal = true;
        } catch (FileNotFoundException e) {
            showError("File " + filePath + " NOT found!");
        } catch (IOException e) {
            showError("Some IO Error in writing to file " + filePath + "!");
        }
        return retVal;
    }

    String getOPCIPFromFile() {
        String opcIP = null;
        try {
            FileReader fileReader = new FileReader(new File(opcIPFilePath));
            BufferedReader br = new BufferedReader(fileReader);
            String line = br.readLine();
            if (line != null)
                opcIP = line.trim();
            br.close();
            fileReader.close();
        } catch (IOException e) {
            showError("Unable to read OPC IP data");
        }
        return opcIP;
    }

    boolean setupUaClient() {
        boolean retVal = false;
        uaServerURI = getOPCIPFromFile();
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

    static protected boolean parseCmdLineArgs(String[] args) {
        int i = 0;
        boolean retVal = false;
        if (DFHeating.parseCmdLineArgs(args)) {
//            resetRightsForLevel2();
            L2CommandLineArgs cmdArg;
            while ((args.length > i)
                    && ((args[i].startsWith("-")))) {
                cmdArg = L2CommandLineArgs.getEnum(args[i]);
                switch (cmdArg) {
//                    case DFHPROCESS:
//                        bAllowEditDFHProcess = true;
//                        break;
//                    case FURNACESETTINGS:
//                        bAllowEditFurnaceSettings = true;
//                        break;
//                    case FROMFIELD:
//                        bAllowUpdateWithFieldData = true;
//                        break;
                    case CHANGEPROFILE:
                        bAllowProfileChange = true;
                        break;
                    case MANUALCALCULATIONS:
                        bAllowManualCalculation = true;
                        break;
                    case L2DEBUG:
                        bl2ShowDebugMessages = true;
                        break;
                    case SHOWALLMENU:
                        bShowAllmenu = true;
                        break;
//                     case ALLOWCHANGES:
//                         allowL2Changes = true;
//                         break;
                }
                i++;
            }
            if (i < args.length) {
                uaServerURI = args[i++];
                retVal = true;
            }
        }
//        if (retVal)
//            adjustRights();
        return retVal;
    }

    static void resetRightsForLevel2() {
        bAllowL2Changes = false;
        bAllowManualCalculation = false;
        bAllowProfileChange = false;
        bAllowEditDFHProcess = false;
        bAllowEditFurnaceSettings = false;
        bAllowEditPerformanceList = false;
        bAllowUpdateWithFieldData = false;
    }

    static void adjustRights() {
        if (bAllowProfileChange) {
            bAllowManualCalculation = true;
        }
        if (bAllowManualCalculation) {
            bAllowEditFurnaceSettings = true;
            bAllowEditPerformanceList = true;
        }
        if (bAllowEditFurnaceSettings) {
            bAllowEditDFHProcess = true;
        }
        if (bAllowEditDFHProcess) {
            bAllowUpdateWithFieldData = true;
            bAllowL2Changes = true;
        }
    }

//    void setAccessLevel() {
//        userActionAllowed = false;
//        if (bAllowL2Changes) {
//            accessLevel = AccessLevel.EXPERT;
//        }
//        else if (bAllowUpdateWithFieldData)
//            accessLevel = AccessLevel.UPDATER;
//        else
//            accessLevel = AccessLevel.RUNTIME;
//        l2Trace("Access Level set to " + accessLevel);
//    }

    StatusWithMessage getL2FceFromFile() {
        disableCompare();
        StatusWithMessage retVal = new StatusWithMessage();
        furnace.resetSections();
        File folder = new File(fceDataLocation);
        File[] files = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".dfhDat");
            }
        });
        if (files.length < 1) {
            retVal.setErrorMessage("Unable to locate Level2 Furnace file!");
        } else if (files.length > 1) {
            retVal.setErrorMessage("There are more than one Furnace File in the directory!");
        } else {
            furnace.resetLossAssignment();
            hidePerformMenu();
            retVal = getFceFromFceDatFile(files[0].getAbsolutePath());
            switchPage(DFHDisplayPageType.INPUTPAGE);
        }
        return retVal;
    }

    public StatusWithMessage getFceFromFceDatFile(String filePath) {
        return super.getFceFromFceDatFile(filePath);
    }

    void getFuelAndCharge() {
        fuelSpecsFromFile(fceDataLocation + "FuelSpecifications.dfhSpecs");
        chMaterialSpecsFromFile(fceDataLocation + "ChMaterialSpecifications.dfhSpecs");
    }

    String profileCode = "";
    boolean changeProfileCode = true;
    String profileCodeTag = "profileCode";
    DecimalFormat profileCodeFormat = new DecimalFormat("000000");

    String createProfileCode() {
        profileCode = profileCodeFormat.format(Math.random() * 999999.0);
        bProfileEdited = true;
        return profileCode;
    }

    boolean createProfileCodeNEW() {
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

    public String inputDataXML(boolean withPerformance) {
        return profileCodeInXML() + super.inputDataXML(withPerformance);
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
//        l2Debug("profile code <" + profileCode + ">");
        if (isProfileCodeOK() || bL2Configurator) {
            retVal = super.takeProfileDataFromXML(xmlStr);
        } else
            retVal.setErrorMessage("ERROR: Not a Level2 Furnace Profile");
        return retVal;
    }

    public StatusWithMessage takeProfileDataFromXMLNEW(String xmlStr) {
        StatusWithMessage retVal = new StatusWithMessage();
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, profileCodeTag);
        profileCode = vp.val;
        if (isProfileCodeOK() || bL2Configurator) {
            retVal = super.takeProfileDataFromXML(xmlStr);
            if (retVal.getDataStatus() == StatusWithMessage.DataStat.OK) {
                furnace.clearAssociatedData();
                vp = XMLmv.getTag(xmlStr, "FuelSettings");
                if (vp.val.length() < 10 || !furnace.takeFceSettingsFromXML(xmlStr))
                    retVal.addInfoMessage("No Fuel settings data available");
                vp = XMLmv.getTag(xmlStr, "dfhProcessList");
                if (vp.val.length() < 10 || !takeStripProcessListFromXML(xmlStr))
                    retVal.addInfoMessage("No Strip Process Data available");
            }
        } else
            retVal.addErrorMessage("ERROR: Not a Level2 Furnace Profile");
        return retVal;
    }

    void clearAssociatedData() {
        if (l2Furnace != null)
            l2Furnace.clearAssociatedData();
        if (dfhProcessList != null)
            dfhProcessList.clear();
    }

    boolean updateFurnace() {
        saveFurnaceWithNowProfileCode();
        return true;
    }

    void saveFurnaceWithNowProfileCode() {
        changeProfileCode = false;
        saveFceToFile(true);
        changeProfileCode = true;
    }

    protected void saveFceToFile(boolean withPerformance) {
        takeValuesFromUI();
        String title = "Save Level2 Furnace Data" + ((withPerformance) ? " (with Performance Data)" : "");
        FileDialog fileDlg =
                new FileDialog(mainF, title,
                        FileDialog.SAVE);
        createProfileCode();
        fileDlg.setFile(profileCode + " FurnaceProfile.dfhDat");
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

    protected void saveFceToFileNEW(boolean withPerformance) {
        takeValuesFromUI();
        StatusWithMessage fceSettingsIntegrity = furnace.furnaceSettings.checkIntegrity();
        if (fceSettingsIntegrity.getDataStatus() == StatusWithMessage.DataStat.OK) {
            String title = "Save Level2 Furnace Data";
            FileDialog fileDlg =
                    new FileDialog(mainF, title,
                            FileDialog.SAVE);
            boolean profileCodeChanged = createProfileCodeNEW();
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
        } else
            showError("Problem in Fuel Settings :\n" + fceSettingsIntegrity.getErrorMessage());
    }


    protected boolean getStripDFHProcessList(String basePath) {  // TODO to be modifed 20160520
        boolean retVal = false;
        File file = getParticularFile(basePath, profileCode, "stripProc");
        if (file != null) {
            retVal = loadStripDFHProcessList(file);
            l2Info("loaded file " + file);
        } else
            showError("Unable to locate StripDFHProcess List File");
        return retVal;
    }

    protected boolean getStripDFHProcessList() {
        return getStripDFHProcessList(fceDataLocation);
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


    protected boolean getPerformanceList(String basePath) {
        boolean retVal = false;
        File file = getParticularFile(basePath, profileCode, "perfData");
        if (file != null) {
            retVal = loadPerformanceData(file);
            l2Info("Updated Performance Data from file " + file);
        }
        return retVal;
    }

    protected boolean getPerformanceList() {
        return getPerformanceList(fceDataLocation);
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

    public OneStripDFHProcess getStripDFHProcess(String forProc) {
        return dfhProcessList.getDFHProcess(forProc.trim().toUpperCase());
    }

    public void resultsReady(Observations observations) {
        super.resultsReady(observations);
        l2Furnace.setCurveSmoothening(true);
//        if (proc == DFHTuningParams.ForProcess.STRIP) {
//            mISaveAsFieldResult.setEnabled(true);
//        }
    }

    public void resultsReady(Observations observations, DFHResult.Type switchDisplayTo) {
        userActionAllowed = false;
        super.resultsReady(observations, switchDisplayTo);
    }

    protected void enableResultsMenu(boolean enable) {
        super.enableResultsMenu(enable);
        if (l2MenuSet && bAllowL2Changes) {
            if (mISaveAsFieldResult != null)
                mISaveAsFieldResult.setEnabled(enable);
        }
    }

    // machine ID methods
    boolean testMachineID() {
        boolean keyOK = false;
        boolean newKey = false;
        MachineCheck mc = new MachineCheck();
        String machineId = mc.getMachineID();
//        showMessage("Machine Id = " + machineId);
//        showMessage("Key = " + mc.getKey(machineId));
        String key = getKeyFromFile();
        do {
            if (key.length() < 5) {
                key = getKeyFromUser(machineId);
                newKey = true;
            }
            if (key.length() > 5) {
//                keyOK = mc.checkKey(key);
//                if (!newKey && !keyOK) {
//                    boolean response = decide("Software key", "There is some problem in the saved key\n"
//                            + " Do you want to delete the earlier key data and enter the key manually?");
//                    if (response) {
//                        key = "";
//                        continue;
//                    }
//                }
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

    public StatusWithMessage setPerformanceTableLimits(Performance baseP) {
        OneStripDFHProcess dfhProcess = getStripDFHProcess(baseP.processName);
        return baseP.setLimits(dfhProcess.minWidth, dfhProcess.maxWidth, 0.2, dfhProcess.minUnitOutput, dfhProcess.maxUnitOutput, 0.2);
    }

    public FceEvaluator calculateForPerformanceTable(Performance baseP) {
//        OneStripDFHProcess dfhProcess = getStripDFHProcess(baseP.processName);
//        baseP.setLimits(dfhProcess.minWidth, dfhProcess.maxWidth, 0.2, dfhProcess.minUnitOutput, dfhProcess.maxUnitOutput, 0.2);
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
                if (!l2Furnace.isProcessDataBeingUsed()) { //  newStripIsBeingHandled.get() && !fieldDataIsBeingHandled.get()) {
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
//        if (bl2ShowDebugMessages) {
        if (log == null)
            System.out.println("" + (new Date()) + ": DEBUG: " + msg);
        else
            log.debug(accessLevel.toString() + ":" + msg);
//        }
    }

    public static void l2Trace(String msg) {
//        if (bl2ShowDebugMessages) {
        if (log == null)
            System.out.println("" + (new Date()) + ": TRACE: " + msg);
        else
            log.trace(accessLevel.toString() + ":" + msg);
//        }
    }

    public static void l2Info(String msg) {
        if (log != null)
            log.info(accessLevel.toString() + ":" + msg);
    }

    public static void l2Error(String msg) {
        if (log != null)
            log.error(accessLevel.toString() + ":" + msg);
    }

    public void checkAndClose(boolean check) {
        if (!check || decide("Quitting Level2", "Do you really want to Exit this " +
                accessControl.getDescription(accessLevel) + "?", false))
            exitFromLevel2();
//        {l2Furnace.prepareForDisconnection();
//            if (uaClient != null)
//                uaClient.disconnect();
//            super.close();
//        }
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
        close();
    }

    public void exitFromLevel2() {
        if (canClose()) {
            justQuit();
//            l2Furnace.prepareForDisconnection();
//            if (uaClient != null)
//                uaClient.disconnect();
//            close();
        }
    }

    class L2MenuListener implements ActionListener, MenuListener {
        public void actionPerformed(ActionEvent e) {
            Object caller = e.getSource();
            if (caller == mILoadFurnace)
                mIGetFceProfile.doClick();
            else if (caller == mISaveFurnace)
                mISaveFceProfile.doClick();
            else if (caller == mIUpdateFurnace)
                updateFurnace();
            else if (caller == mIShowProcess)
                switchPage(L2DisplayPageType.PROCESS);
            else if (caller == mIShowL2Data)
                switchPage(L2DisplayPageType.LEVEL2);
//             else if (caller == mIReadDFHStripProcess)
//                 loadStripDFHProcessList();
//             else if (caller == mISaveDFHStripProcess)
//                 updateProcessDataFile();
            else if (caller == mIViewDFHStripProcess)
                viewStripDFHProcess();
            else if (caller == mIEditDFHStripProcess)
                editStripDFHProcess();
            else if (caller == mICreateFceSettings)
                createFceSetting();
//             else if (caller == mIReadFceSettings)
//                 readFurnaceSettings();
//             else if (caller == mISaveFceSettings)
//                 saveFurnaceSettings();
            else if (caller == mIOPCServerIP)
                setOPCIP();
            else if (caller == mICreateFieldResultsData)
                takeFieldResultsFromUser();
            else if (caller == mISaveFieldResultsToFile)
                saveFieldResultsToFile();
            else if (caller == mISaveAsFieldResult)
                saveAsFieldResults();
            else if (caller == mILevel1FieldResults)
                takeResultsFromLevel1();
            else if (caller == mILoadFieldResult)
                loadFieldResults();
            else if (caller == mIEvalForFieldProduction)
                evalForFieldProduction();
            else if (caller == mIEvalWithFieldCorrection)
                recalculateWithFieldCorrections(null);
            else if (caller == mISavePerformanceData)
                updatePerformanceDataFile();
            else if (caller == mIReadPerformanceData) {
                loadPerformanceData();
                updateDisplay(DFHDisplayPageType.PERFOMANCELIST);
            } else if (caller == mIExpertAccess)
                manageExpertAccess();
            else if (caller == mIUpdaterAccess)
                manageUpdaterAccess();
            else if (caller == mIRuntimeAccess)
                manageRuntimeAccess();
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
        //        PropertyConfigurator.configureAndWatch(DFHeating.class
        //                .getResource("log.properties").getFile(), 5000);
//        L2DFHeating.log = Logger.getLogger("L2DFHeating.class");
        final L2DFHeating level2Heating = new L2DFHeating("Furnace");
        if (level2Heating.parseCmdLineArgs(args)) {
            if (!onProductionLine || level2Heating.setupUaClient()) {
                level2Heating.setItUp();
                if (level2Heating.l2SystemReady) {
                    level2Heating.informLevel2Ready();
                } else {
                    level2Heating.showError("Level2 could not be started. Aborting ...");
                    level2Heating.exitFromLevel2();
//                    System.exit(1);
                }
            } else
                level2Heating.showMessage("Facing problem connecting to Level1. Aborting ...");
        }
    }
}
