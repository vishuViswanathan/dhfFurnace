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

    static public boolean bAllowUpdateWithFieldData = false;
    static boolean bAllowL2Changes = false;
    static boolean bl2ShowDebugMessages = false;
    static boolean bShowAllmenu = false;

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

    public L2DFHeating(String equipment, boolean fromLauncher) {
        this(equipment);
        if (setItUp()) {
            if (l2SystemReady) {
                informLevel2Ready();
                markIAmON();
            } else {
                showError("Level2 could not be started. Aborting ...");
                exitFromLevel2();
            }
        } else {
            showMessage("Facing problem connecting to Level1. Aborting ...");
            close();
        }
    }

    public L2DFHeating(String equipment, boolean fromLauncher, boolean bOLD) {
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

    String getStatusFileName(L2AccessControl.AccessLevel level)  {
        return fceDataLocation + accessControl.getDescription(level) + " is ON";
    }

//    DataWithMsg isAnybodyActive(L2AccessControl.AccessLevel level) {
//        DataWithMsg retVal = new DataWithMsg();
//        FileLock lock = getFileLock();
//        if (lock != null) {
//            File f = new File(getStatusFileName(level));
//            if (f.exists())
//                retVal.setData(true);
//            else
//                retVal.setData(false);
//            releaseLock(lock);
//        }
//        else
//            retVal.setErrorMsg("Unable to get the lock to get Status of application " + accessControl.getDescription(level));
//        return retVal;
//    }

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
//                switchPage(DFHDisplayPageType.INPUTPAGE);
                StatusWithMessage statL2FceFile = getL2FceFromFile();
                if (statL2FceFile.getDataStatus() != StatusWithMessage.DataStat.WithErrorMsg) {
                    if (!onProductionLine || setupUaClient()) {
                        if (!onProductionLine || l2Furnace.basicConnectionToLevel1(uaClient)) {
                            StatusWithMessage status = l2Furnace.checkAndNoteAccessLevel();
                            if (status.getDataStatus() == StatusWithMessage.DataStat.OK) {
                                furnace.setWidth(width);
                                enableDataEdit();
//                                addMenuBar(createL2MenuBar());
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

//    public boolean setSelectedProcess(String selProc) {
//        return dfhProcessList.setSelectedProcess(selProc);
//    }

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

//    JMenu mL2FileMenu;
//    JMenuItem mISaveFurnace;
//    JMenuItem mILoadFurnace;
//    JMenuItem mIUpdateFurnace;
//
//    JMenu mL2Configuration;
//    JMenuItem mIOPCServerIP;
//    JMenuItem mIEditDFHStripProcess;
//    JMenuItem mIViewDFHStripProcess;
//    JMenuItem mICreateFceSettings;

    boolean l2MenuSet = false;

    JMenuBar menuBarLevel2;
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

    protected JMenu createFileMenu() {
        fileMenu = super.createFileMenu();
        if (accessLevel != L2AccessControl.AccessLevel.INSTALLER) {
            fileMenu.remove(mIGetFceProfile);
            fileMenu.remove(mISavePerformanceData);
            if (accessLevel != L2AccessControl.AccessLevel.EXPERT)
                fileMenu.remove(mIUpdateFurnace);
        }
        return fileMenu;
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
        mAccessControl = new JMenu("Access Control");
        switch (accessLevel) {
            case INSTALLER:
                mAccessControl.add(mExpertAccess);
            case EXPERT:
                mAccessControl.add(mUpdaterAccess);
            case UPDATER:
                mAccessControl.add(mRuntimeAccess);
                break;
        }
        return mAccessControl;
    }

    JMenu createLiveDisplayMenu() {
        JMenu jm = new JMenu("Live Displays");
        jm.add(mIShowProcess);
        jm.add(mIShowL2Data);
        return jm;
    }

//    JMenu createL2FileMenu(ActionListener li) {
//        mL2FileMenu = new JMenu("File");
//        mISaveFurnace = new JMenuItem("Save Furnace");
//        mISaveFurnace.addActionListener(li);
//        mILoadFurnace = new JMenuItem("Load Furnace");
//        mILoadFurnace.addActionListener(li);
//        mIUpdateFurnace = new JMenuItem("Update Furnace");
//        mIUpdateFurnace.addActionListener(li);
//        if (accessLevel == L2AccessControl.AccessLevel.INSTALLER) {
//            mL2FileMenu.add(mILoadFurnace);
//            mL2FileMenu.add(mISaveFurnace);
//            mL2FileMenu.addSeparator();
//        }
//        if (accessLevel == L2AccessControl.AccessLevel.INSTALLER || accessLevel == L2AccessControl.AccessLevel.EXPERT) {
//            mL2FileMenu.add(mIUpdateFurnace);
//            mL2FileMenu.addSeparator();
//        }
//        mL2FileMenu.add(mIExit);
//        return mL2FileMenu;
//    }

    @Override
    protected JMenuBar assembleMenuBar() {
        boolean bShowRuntimeData = (accessLevel == L2AccessControl.AccessLevel.RUNTIME) ||
                (accessLevel == L2AccessControl.AccessLevel.UPDATER) ||
                (accessLevel == L2AccessControl.AccessLevel.EXPERT);
        boolean bAllowL2Changes = (accessLevel == L2AccessControl.AccessLevel.EXPERT) ||
                (accessLevel == L2AccessControl.AccessLevel.INSTALLER);
        boolean bEnablePerfMenu = bAllowUpdateWithFieldData || (accessLevel == L2AccessControl.AccessLevel.INSTALLER);
        JMenuBar mb = new JMenuBar();
        mb.add(createFileMenu());
        if (bShowRuntimeData) {
            mb.add(createLiveDisplayMenu());
        }
        if (bAllowL2Changes) {
            mb.add(createDefineFurnaceMenu());
            mb.add(createShowResultsMenu());
        }
        if (bEnablePerfMenu) {
            if (bShowRuntimeData) {
                mb.add(mShowCalculation);
            }
            mb.add(createPerformanceMenu());
        }
        if (bAllowL2Changes) {
            mb.add(createL2ConfMenu());
            mb.add(pbEdit);
        }
        if (accessLevel != L2AccessControl.AccessLevel.RUNTIME) {
            mb.add(createAccessMenu());
        }
        return mb;
    }

//    JMenuBar createL2MenuBar() {
//        // Default menus
//        L2MenuListener li = new L2MenuListener();
//        menuBarLevel2 = new JMenuBar();
//        menuBarLevel2.add(createL2FileMenu(li));
//        boolean bShowRuntimeData = (accessLevel == L2AccessControl.AccessLevel.RUNTIME) ||
//                (accessLevel == L2AccessControl.AccessLevel.UPDATER) ||
//                (accessLevel == L2AccessControl.AccessLevel.EXPERT);
//        boolean bAllowL2Changes = (accessLevel == L2AccessControl.AccessLevel.EXPERT) ||
//                (accessLevel == L2AccessControl.AccessLevel.INSTALLER);
//        boolean bEnablePerfMenu = bAllowUpdateWithFieldData || (accessLevel == L2AccessControl.AccessLevel.INSTALLER);
//        if (bShowRuntimeData) {
//            JMenu jm = new JMenu("Live Displays");
//            mIShowProcess = new JMenuItem("Process Data");
//            mIShowProcess.addActionListener(li);
//            jm.add(mIShowProcess);
//            mIShowL2Data = new JMenuItem("Level2 Data");
//            mIShowL2Data.addActionListener(li);
//            jm.add(mIShowL2Data);
//            menuBarLevel2.add(jm);
//        }
//        mIClearPerfBase.setVisible(false);
//
//        if (bAllowL2Changes) {
//            menuBarLevel2.add(inputMenu);
//            menuBarLevel2.add(resultsMenu);
//        }
//        if (bEnablePerfMenu) {
//            mShowCalculation = new JMenu("Show Calculation");
//            if (bShowRuntimeData) {
//                menuBarLevel2.add(mShowCalculation);
//                mShowCalculation.addMenuListener(li);
//            }
//            mISavePerformanceData = new JMenuItem("Save Performance Data");
//            mISavePerformanceData.addActionListener(li);
//            mIReadPerformanceData = new JMenuItem("Load Performance Data");
//            mIReadPerformanceData.addActionListener(li);
//            perfMenu.addSeparator();
//            perfMenu.add(mISavePerformanceData);
//            perfMenu.add(mIReadPerformanceData);
//            menuBarLevel2.add(perfMenu);
//        }
//        if (bAllowL2Changes) {
//            mL2Configuration = new JMenu("L2 Config");
//            mIOPCServerIP = new JMenuItem("Set OPC server IP");
//            mICreateFceSettings = new JMenuItem("View/Edit Zonal Fuel Range");
//
//            mIEditDFHStripProcess = new JMenuItem("Add/Edit StripDFHProcess List");
//            mIViewDFHStripProcess = new JMenuItem("View StripDFHProcess List");
//            mIOPCServerIP.addActionListener(li);
//            mICreateFceSettings.addActionListener(li);
//            mIViewDFHStripProcess.addActionListener(li);
//            mIEditDFHStripProcess.addActionListener(li);
//            if (!onProductionLine) {
//                mL2Configuration.add(mIOPCServerIP);
//                mL2Configuration.addSeparator();
//                mL2Configuration.add(mICreateFceSettings);
//                mL2Configuration.addSeparator();
//            }
//            mL2Configuration.add(mIViewDFHStripProcess);
//            mL2Configuration.add(mIEditDFHStripProcess);
//            mL2Configuration.addSeparator();
//            mL2Configuration.setEnabled(true);
//            menuBarLevel2.add(mL2Configuration);
//            menuBarLevel2.add(pbEdit);
//        }
//
//        mAccessControl = new JMenu("Access Control");
//        String s = accessControl.getDescription(L2AccessControl.AccessLevel.RUNTIME);
//        mRuntimeAccess = new JMenu("for " + s);
//        mIAddRuntimeAccess = new JMenuItem("Add access");
//        mIDeleteRuntimeAccess = new JMenuItem("Delete access");
//        mIAddRuntimeAccess.addActionListener(li);
//        mIDeleteRuntimeAccess.addActionListener(li);
//        mRuntimeAccess.add(mIAddRuntimeAccess);
//        mRuntimeAccess.add(mIDeleteRuntimeAccess);
//
//        s = accessControl.getDescription(L2AccessControl.AccessLevel.UPDATER);
//        mUpdaterAccess = new JMenu("for " + s);
//        mIAddUpdaterAccess = new JMenuItem("Add access");
//        mIDeleteUpdaterAccess = new JMenuItem("Delete access");
//        mIAddUpdaterAccess.addActionListener(li);
//        mIDeleteUpdaterAccess.addActionListener(li);
//        mUpdaterAccess.add(mIAddUpdaterAccess);
//        mUpdaterAccess.add(mIDeleteUpdaterAccess);
//
//        s = accessControl.getDescription(L2AccessControl.AccessLevel.EXPERT);
//        mExpertAccess = new JMenu("for " + s);
//        mIAddExpertAccess = new JMenuItem("Add access");
//        mIDeleteExpertAccess = new JMenuItem("Delete access");
//        mIAddExpertAccess.addActionListener(li);
//        mIDeleteExpertAccess.addActionListener(li);
//        mExpertAccess.add(mIAddExpertAccess);
//        mExpertAccess.add(mIDeleteExpertAccess);
//
//        switch (accessLevel) {
//            case INSTALLER:
//                mAccessControl.add(mExpertAccess);
//            case EXPERT:
//                mAccessControl.add(mUpdaterAccess);
//            case UPDATER:
//                mAccessControl.add(mRuntimeAccess);
//                menuBarLevel2.add(mAccessControl);
//                break;
//        }
//        return menuBarLevel2;
//    }

    public void enablePerfMenu(boolean ena) {
        if (bAllowUpdateWithFieldData) {
            perfMenu.setEnabled(ena);
        }
    }

//    FramedPanel panelLT;
//    FramedPanel panelRT;
//    FramedPanel panelLB;
//    FramedPanel panelRB;
//    JPanel theOuterPanel = new JPanel(new GridBagLayout());
//    int SCREEENWIDTH = 1366;
//    int SCREENHEIGHT = 768;
//
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
            gotIt = getFieldPerformanceList();
            releaseLock(lock);
            break;
        }
        if (gotIt)
            updateDisplay(DFHDisplayPageType.PERFOMANCELIST);
        else if (!gotTheLock)
            showError("Facing some problem in getting Lock to take Modified Performance Data");
        return gotIt;
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

//    void editStripDFHProcess() {
//        if (dfhProcessList.addStripDFHProcess(parent()))
//            showMessage("Strip DFh Process List updated\n" +
//                    "To make it effective in Level2 RUNTIME, the list must be saved to file\n" +
//                    "       " + mL2FileMenu.getText() + "->" + mIUpdateFurnace.getText());
//    }
//
//    void viewStripDFHProcess() {
//        dfhProcessList.viewStripDFHProcess(parent());
//    }
//
//    void setOPCIP() {
//        if (furnace.furnaceSettings.getOPCServerIP(this)) {
//            showMessage("Furnace connection Data is modified\n" +
//                    "To be effective in Level2 RUNTIME:\n" +
//                    "Save data to file with " + mL2FileMenu.getText() + "->" + mIUpdateFurnace.getText());
//        }
//    }
//
//    void createFceSetting() {
//        if (l2Furnace.showEditFceSettings(true)) {
//            showMessage("Furnace Fuel Data is modified\n" +
//                    "To be effective in Level2 RUNTIME:\n" +
//                    "    1) Save data to file with " + mL2FileMenu.getText() + "->" + mIUpdateFurnace.getText() + "\n" +
//                    "    2) Restart Level2 RUNTIME, if already running");
//        }
//    }

    FileSystemView currentView;
    String fceSettExtension = "fceSett";

//    boolean saveFurnaceSettings() {
//        boolean retVal = false;
//        if (isProfileCodeOK()) {
//            String title = "Save Zonal Fuel Range";
//            FileChooserWithOptions fileDlg = new FileChooserWithOptions(title, "Zonal Fuel Range (*." + fceSettExtension + ")", fceSettExtension);
//            fileDlg.setSelectedFile(new File(profileCode + " Zonal Fuel Range." + fceSettExtension));
//            fileDlg.setCurrentDirectory(fceDataLocationDirectory);
//            fileDlg.setStartWithString(profileCode);
//            if (currentView != null)
//                fileDlg.setFileSystemView(currentView);
//            if (fileDlg.showSaveDialog(parent()) == JFileChooser.APPROVE_OPTION) {
//                File file = fileDlg.getSelectedFile();
//                if (fileDlg.isItDuplicate()) {
//                    String fName = file.getAbsolutePath();
//                    markThisFileAsBak(file);
//                    file = new File(fName);
//                }
//                deleteParticularFiles("" + fceDataLocation, profileCode, fceSettExtension);
//                try {
//                    BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(file));
//                    oStream.write(("# Zonal Fuel Range saved on " + dateFormat.format(new Date()) + " \n\n").getBytes());
//                    oStream.write((profileCodeInXML() + l2Furnace.fceSettingsInXML()).getBytes());
//                    oStream.close();
//                    retVal = true;
//                } catch (FileNotFoundException e) {
//                    showError("File " + file + " NOT found!");
//                } catch (IOException e) {
//                    showError("Some IO Error in writing to file " + file + "!");
//                }
//            }
//            currentView = fileDlg.getFileSystemView();
//        } else {
//            showError("Save Profile before saving Zonal Fuel Range");
//        }
//        return retVal;
//    }

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

//    boolean loadFurnaceSettings(String filePath) {
//        boolean retVal = false;
//        if (!filePath.equals("nullnull")) {
////            debug("File for Zonal Fuel Range :" + filePath);
//            try {
//                File file = new File(filePath);
//                retVal = loadFurnaceSettings(file);
//            } catch (Exception e) {
//                showError("Some Problem in getting Fuel Range file " + filePath);
//            }
//        }
//        return retVal;
//    }

//    boolean takeFieldResultsFromUser() {
//        return l2Furnace.getFieldDataFromUser();
//    }
//
//    ErrorStatAndMsg takeResultsFromLevel1() {
//        ErrorStatAndMsg stat = l2Furnace.takeFieldResultsFromLevel1(true);
//        if (stat.inError) {
//            l2Info("ERROR in L2DFeating.takeResultsFromLevel1, in return from takeResultsFromLevel1 " + stat.msg);
//        }
//        return stat;
//    }
//
//    boolean loadFieldResults() {
//        boolean retVal = false;
//        String title = "Loading Field Results";
//        FileDialog fileDlg =
//                new FileDialog(mainF, title,
//                        FileDialog.LOAD);
//        fileDlg.setFile("*.fResult");
//        fileDlg.setVisible(true);
//        String fileName = fileDlg.getFile();
//        if (fileName != null) {
//            String filePath = fileDlg.getDirectory() + fileName;
//            retVal = loadFieldResults(filePath);
//        }
//        parent().toFront();
//        return retVal;
//    }

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

//    boolean loadFieldResults(String filePath) {
//        boolean retVal = false;
//        try {
//            BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(filePath));
//            //           FileInputStream iStream = new FileInputStream(fileName);
//            File f = new File(filePath);
//            long len = f.length();
//            if (len > 50 && len < 10000) {
//                int iLen = (int) len;
//                byte[] data = new byte[iLen + 10];
//                if (iStream.read(data) > 50) {
//                    if (l2Furnace.takeFieldResultsFromXML(new String(data))) {
//                        showMessage("Field results loaded");
//                        retVal = true;
//                    }
//                }
//                iStream.close();
//            } else
//                showError("File size " + len + " for " + filePath);
//        } catch (Exception e) {
//            showError("Some Problem in getting file!");
//        }
//        return retVal;
//    }

//    boolean saveFieldResultsToFile() {
//        showError("Not ready toSave Field Results to file yet");
//        return false;
//    }

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

//    static void resetRightsForLevel2() {
//        bAllowL2Changes = false;
//        bAllowManualCalculation = false;
//        bAllowProfileChange = false;
//        bAllowEditDFHProcess = false;
//        bAllowEditFurnaceSettings = false;
//        bAllowEditPerformanceList = false;
//        bAllowUpdateWithFieldData = false;
//    }
//
//    static void adjustRights() {
//        if (bAllowProfileChange) {
//            bAllowManualCalculation = true;
//        }
//        if (bAllowManualCalculation) {
//            bAllowEditFurnaceSettings = true;
//            bAllowEditPerformanceList = true;
//        }
//        if (bAllowEditFurnaceSettings) {
//            bAllowEditDFHProcess = true;
//        }
//        if (bAllowEditDFHProcess) {
//            bAllowUpdateWithFieldData = true;
//            bAllowL2Changes = true;
//        }
//    }

    StatusWithMessage getL2FceFromFile() {
        DataWithMsg oneFile = FileChooserWithOptions.getOneExistingFilepath(fceDataLocation, profileFileExtension, true);
        StatusWithMessage retVal = new StatusWithMessage();
        if (oneFile.getStatus() == DataWithMsg.DataStat.OK) {
            String filePath = oneFile.stringValue;
            disableCompare();
            furnace.resetSections();
//            dfhProcessList = new StripDFHProcessList(this);
            furnace.resetLossAssignment();
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

//    String createProfileCodeOLD() {
//        profileCode = profileCodeFormat.format(Math.random() * 999999.0);
//        bProfileEdited = true;
//        return profileCode;
//    }
//
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

//    OneStripDFHProcess getDFHProcess() {
//        return getStripDFHProcess(getProcessName());
//    }

    protected ErrorStatAndMsg isChargeInFceOK() {    // TODO 20160622 considered that UI is already read
        ErrorStatAndMsg retVal = super.isChargeInFceOK();
        if ((furnaceFor() == DFHTuningParams.FurnaceFor.STRIP)) {
            OneStripDFHProcess oneProc = getDFHProcess();
            if (oneProc != null) {
                ChMaterial material = oneProc.getChMaterial(chThickness);
                setChMaterial(material);
                setExitTemperaure(oneProc.tempDFHExit);
                setExitZoneTemperatures(oneProc.getMaxExitZoneTemp(), oneProc.getMinExitZoneTemp());
            }
            else {
                retVal.addErrorMsg("\n   Process not available in DFH Process List");
            }
        }
        return retVal;
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

//    void clearAssociatedData() {
//        if (l2Furnace != null)
//            l2Furnace.clearAssociatedData();
//        if (dfhProcessList != null)
//            dfhProcessList.clear();
//    }

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


    protected boolean getFieldPerformanceList(String basePath) {
        boolean retVal = false;
        File file = getParticularFile(basePath, profileCode, "perfData");
        if (file != null) {
            if (loadThePerformanceList(file))
                l2Info("Updated Performance Data from file " + file);
            else
                l2Info("Some problem in updating Performance Data from file " + file);
        }
        return retVal;
    }

    protected boolean getFieldPerformanceList() {
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

//    public OneStripDFHProcess getStripDFHProcess(String forProc) {
//        return dfhProcessList.getDFHProcess(forProc.trim().toUpperCase());
//    }

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

    public static void l2Trace(String msg) {
        if (log == null)
            System.out.println("" + (new Date()) + ": TRACE: " + msg);
        else
            log.trace(accessLevel.toString() + ":" + msg);
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
//            if (caller == mILoadFurnace)
//                mIGetFceProfile.doClick();
//            else if (caller == mISaveFurnace)
//                mISaveFceProfile.doClick();
//            else if (caller == mIUpdateFurnace)
//                updateFurnace();
            if (caller == mIShowProcess)
                switchPage(L2DisplayPageType.PROCESS);
            else if (caller == mIShowL2Data)
                switchPage(L2DisplayPageType.LEVEL2);
//            else if (caller == mIViewDFHStripProcess)
//                viewStripDFHProcess();
//            else if (caller == mIEditDFHStripProcess)
//                editStripDFHProcess();
//            else if (caller == mICreateFceSettings)
//                createFceSetting();
//            else if (caller == mIOPCServerIP)
//                setOPCIP();
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
