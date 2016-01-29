package level2;

import TMopcUa.TMuaClient;
import basic.*;
import directFiredHeating.DFHTuningParams;
import directFiredHeating.DFHeating;
import directFiredHeating.FceEvaluator;
import directFiredHeating.ResultsReadyListener;
import display.QueryDialog;
import mvUtils.display.ErrorStatAndMsg;
import mvUtils.display.FramedPanel;
import mvUtils.display.StatusWithMessage;
import mvUtils.file.FileChooserWithOptions;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import org.apache.log4j.Logger;
import protection.MachineCheck;

import javax.swing.*;
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

    static boolean bAllowEditDFHProcess = false;
    static boolean bAllowEditFurnaceSettings = false;
    static boolean bAllowUpdateWithFieldData = false;
    static boolean bAllowL2Changes = false;
    static boolean bl2ShowDebugMessages = false;
    static boolean bShowAllmenu = false;

    String fceDataLocation = "level2FceData/";
    String lockPath = fceDataLocation + "Sychro.lock";
    File lockFile;

    enum AccessLevel {RUNTIME, UPDATER, EXPERT};
    public AccessLevel accessLevel = AccessLevel.RUNTIME;
    TMuaClient uaClient;
    static String uaServerURI;
    L2DFHFurnace l2Furnace;
    public String equipment;
    StripDFHProcessList dfhProcessList;
//    Hashtable<String, OneStripDFHProcess> stripProcessLookup;
    JMenu mL2Configuration;
    JMenuItem mIEditDFHStripProcess;
    JMenuItem mIViewDFHStripProcess;
    JMenuItem mIReadDFHStripProcess;
    JMenuItem mISaveDFHStripProcess;
    JMenuItem mICreateFceSettings;
    JMenuItem mISaveFceSettings;
    JMenuItem mIReadFceSettings;

    JMenuItem mICreateFieldResultsData;
    JMenuItem mISaveFieldResultsToFile;
    JMenuItem mILoadFieldResult;
    JMenuItem mILevel1FieldResults;
    JMenuItem mISaveAsFieldResult;

    JMenuItem mIEvalForFieldProduction;
    JMenuItem mIEvalWithFieldCorrection;

    boolean l2MenuSet = false;

    public L2DFHeating(String equipment) {
        super();
        releaseDate = "20160106";
        onProductionLine = true;
        asApplication = true;
        this.equipment = equipment;
//        stripProcessLookup = new Hashtable<String, OneStripDFHProcess>();
//        init();
    }

    public boolean l2SystemReady = false;

    public boolean isL2SystemReady() {
        return l2SystemReady;
    }

    public boolean setItUp() {
        modifyJTextEdit();
        setAccessLevel();
        fuelList = new Vector<Fuel>();
        vChMaterial = new Vector<ChMaterial>();
        setUIDefaults();
        mainF = new JFrame();
        mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        if (!asJNLP && (log == null)) {
            log = Logger.getLogger(DFHeating.class);
            // Load Log4j configurations from external file
        }
        mainF.setTitle("DFH Furnace Level2 " + releaseDate + testTitle);

        tuningParams = new DFHTuningParams(this, onProductionLine, 1, 5, 30, 1.12, 1, false, false);
//        debug("Creating new Level2furnace");
        l2Furnace = new L2DFHFurnace(this, false, false, lNameListener);
        if (l2Furnace.basicsSet) {
            furnace = l2Furnace;
            StatusWithMessage status = l2Furnace.checkAndNoteAccessLevel();
            if (status.getDataStatus() == StatusWithMessage.DataStat.OK) {
//            furnace.setWidth(width);
//            debug("Created Level2furnace");
                furnace.setTuningParams(tuningParams);
                createUIs();
                getFuelAndCharge();
                setDefaultSelections();
                if (!testMachineID()) {
                    showError("Software key mismatch, Aborting ...");
                    close();
                }
                showMainAppPanel();  // TODO TEMPERRORY -- TO be changed
                switchPage(DFHDisplayPageType.INPUTPAGE);
//            displayIt();
                if (getL2FceFromFile()) {
                    furnace.setWidth(width);
                    enableDataEdit();
                    createMenuBars();
                    l2MenuSet = true;
                    setFcefor(true);
                    getPerformanceList();
                    dfhProcessList = new StripDFHProcessList(this);
                    if (getStripDFHProcessList()) {
                        if (getFurnaceSettings()) {
                            if (createL2Zones()) {
                                ErrorStatAndMsg connStat = l2Furnace.checkConnection();
                                if (connStat.inError)
                                    showError(connStat.msg);
                                else {
                                    l2Furnace.initForLevel2Operation();
                                    l2SystemReady = true;
                                }
                            }
                        } else
                            showError("Problem in loading Furnace Settings");
                    } else
                        showError("Problem loading test StripDFHProcess list data");
                } else
                    showError("Unable to load Furnace Profile");
            } else
                showError(status.getErrorMessage());
        }
        else
            showError("Unable to start Level2 ERROR:001");
        if (l2SystemReady) {
            lockFile = new File(lockPath);
            displayIt();
        }
        return l2SystemReady;
//        if (!showDebugMessages) {
//            tuningParams.showSectionProgress(false);
//            tuningParams.showSlotProgress(false);
//        }
    }

    JMenuBar menuBarLevel2RT;
    JMenuBar menuBarLevel2Edit;
    JMenuItem mISavePerformanceData;
    JMenuItem mIReadPerformanceData;

    void createMenuBars() {
        menuBarLevel2Edit = new JMenuBar();
        menuBarLevel2Edit.add(fileMenu);
        L2MenuListener li = new L2MenuListener();
        if (bAllowUpdateWithFieldData || bShowAllmenu) {
            mISavePerformanceData = new JMenuItem("Save Performance Data");
            mISavePerformanceData.addActionListener(li);
            mIReadPerformanceData = new JMenuItem("Load Performance Data");
            mIReadPerformanceData.addActionListener(li);
            perfMenu.addSeparator();
            perfMenu.add(mISavePerformanceData);
            perfMenu.add(mIReadPerformanceData);
            menuBarLevel2Edit.add(perfMenu);
        }
        if (bAllowL2Changes || bShowAllmenu) {
            if (bAllowManualCalculation)  {
                menuBarLevel2Edit.add(inputMenu);
                menuBarLevel2Edit.add(resultsMenu);
            }
            mL2Configuration = new JMenu("L2 Config");
            mICreateFceSettings = new JMenuItem("View/Edit Furnace Settings");
            mIReadFceSettings = new JMenuItem("Read Furnace Settings from file");
            mISaveFceSettings = new JMenuItem("Save Furnace Settings to file");

            mIEditDFHStripProcess = new JMenuItem("Add/Edit StripDFHProcess List");
            mIViewDFHStripProcess = new JMenuItem("View StripDFHProcess List");
            mIReadDFHStripProcess = new JMenuItem("Read StripDFHProcess List from File");
            mISaveDFHStripProcess = new JMenuItem("Save StripDFHProcess List to File");

            mICreateFieldResultsData = new JMenuItem("Enter Field Results Data");
            mICreateFieldResultsData.setEnabled(false);
            mISaveFieldResultsToFile = new JMenuItem("Save Field Results to file");
            mISaveFieldResultsToFile.setEnabled(false);
            mILevel1FieldResults = new JMenuItem("Take Results From Level1");
            mILevel1FieldResults.setEnabled(false);
            mILoadFieldResult = new JMenuItem("Load Field Results from File");
            mILoadFieldResult.setEnabled(false);
            mISaveAsFieldResult = new JMenuItem("Save As Field Results");
            mISaveAsFieldResult.setEnabled(false);
            mIEvalForFieldProduction = new JMenuItem("Calculate for Field Production");
            mIEvalForFieldProduction.setEnabled(false);
            mIEvalWithFieldCorrection = new JMenuItem("Re-Calculate With Field Corrections");
            mIEvalWithFieldCorrection.setEnabled(false);

            mICreateFceSettings.addActionListener(li);
            mIReadFceSettings.addActionListener(li);
            mISaveFceSettings.addActionListener(li);
            mIViewDFHStripProcess.addActionListener(li);
            mIEditDFHStripProcess.addActionListener(li);
            mISaveDFHStripProcess.addActionListener(li);
            mIReadDFHStripProcess.addActionListener(li);
            mICreateFieldResultsData.addActionListener(li);
            mISaveFieldResultsToFile.addActionListener(li);
            mILevel1FieldResults.addActionListener(li);
            mILoadFieldResult.addActionListener(li);
            mISaveAsFieldResult.addActionListener(li);
            mIEvalForFieldProduction.addActionListener(li);
            mIEvalWithFieldCorrection.addActionListener(li);
            if (bAllowEditFurnaceSettings || bShowAllmenu) {
                mL2Configuration.add(mICreateFceSettings);
                mL2Configuration.add(mIReadFceSettings);
                mL2Configuration.add(mISaveFceSettings);
                mL2Configuration.addSeparator();
            }
            if (bAllowEditDFHProcess || bShowAllmenu) {
                mL2Configuration.add(mIViewDFHStripProcess);
                mL2Configuration.add(mIEditDFHStripProcess);
                mL2Configuration.add(mIReadDFHStripProcess);
                mL2Configuration.add(mISaveDFHStripProcess);
                mL2Configuration.addSeparator();
            }
            if (bAllowManualCalculation) {
                mL2Configuration.add(mICreateFieldResultsData);
                mL2Configuration.add(mISaveFieldResultsToFile);
                mL2Configuration.addSeparator();
                mL2Configuration.add(mILevel1FieldResults);
                mL2Configuration.add(mILoadFieldResult);
                mL2Configuration.add(mIEvalForFieldProduction);
                mL2Configuration.add(mIEvalWithFieldCorrection);
                mL2Configuration.addSeparator();
                mL2Configuration.add(mISaveAsFieldResult);
            }
            mL2Configuration.setEnabled(true);
            menuBarLevel2Edit.add(mL2Configuration);
            if (bAllowManualCalculation) {
                menuBarLevel2Edit.add(pbEdit);
            }
            mainAppPanel.remove(menuBarMainApp);
        }
        mainAppPanel.add(menuBarLevel2Edit, BorderLayout.NORTH);
        menuBarMainApp.updateUI();
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

    protected void createUIs() {
        super.createUIs();
        theOuterPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        panelLT = new FramedPanel();
        Dimension mainAppPanelSize = mainAppPanel.getPreferredSize();
        int wl = mainAppPanelSize.width + 2;
        int ht = mainAppPanelSize.height + 2;
        int wr = SCREEENWIDTH - wl - 2;
        int hb = SCREENHEIGHT - ht - 2;
        panelLT.setPreferredSize(new Dimension(wl, ht));
        panelRT = new FramedPanel();
        panelRT.setPreferredSize(new Dimension(wr, ht));
        panelLB = new FramedPanel();
        panelLB.setPreferredSize(new Dimension(wl, hb));
        panelRB = new FramedPanel();
        panelRB.setPreferredSize(new Dimension(wr, hb));
        gbc.gridx = 0;
        gbc.gridy = 0;
        theOuterPanel.add(panelLT, gbc);
        gbc.gridx = 1;
        theOuterPanel.add(panelRT, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        theOuterPanel.add(panelLB, gbc);
        gbc.gridx = 1;
        theOuterPanel.add(panelRB, gbc);
    }

    void showMainAppPanel() {
        panelLT.removeAll();
        panelLT.add(mainAppPanel);
    }

    public void displayIt() {
        if (!itsON) {
            itsON = true;
            mainF.add(theOuterPanel);
            mainF.setFocusable(true);
            mainF.requestFocus();
            mainF.toFront(); //setAlwaysOnTop(true);
            mainF.pack();
            mainF.setVisible(true);
            mainF.setResizable(false);
        }
    }

    public DFHTuningParams getTuningParams() {
        return tuningParams;
    }

    protected void setFcefor(boolean showSuggestion) {
        super.setFcefor(showSuggestion);
        if (showSuggestion) {
            DFHTuningParams.ForProcess forProc = getFceFor();
            if (bAllowL2Changes) {
                if (forProc == DFHTuningParams.ForProcess.STRIP) {
                    mIReadDFHStripProcess.setVisible(true);
                    mISaveDFHStripProcess.setVisible(true);
                    mIViewDFHStripProcess.setVisible(true);
                    mIEditDFHStripProcess.setVisible(true);
                    mICreateFceSettings.setVisible(true);
                    mIReadFceSettings.setVisible(true);
                    mISaveFceSettings.setVisible(true);
                    mISaveAsFieldResult.setVisible(true);
                    mILoadFieldResult.setVisible(true);
                    mIEvalForFieldProduction.setVisible(true);
                    mIEvalWithFieldCorrection.setVisible(true);
                } else {
                    mIReadDFHStripProcess.setVisible(false);
                    mISaveDFHStripProcess.setVisible(false);
                    mIViewDFHStripProcess.setVisible(false);
                    mIEditDFHStripProcess.setVisible(false);
                    mICreateFceSettings.setVisible(false);
                    mIReadFceSettings.setVisible(false);
                    mISaveFceSettings.setVisible(false);
                    mISaveAsFieldResult.setVisible(false);
                    mILoadFieldResult.setVisible(false);
                    mIEvalForFieldProduction.setVisible(false);
                    mIEvalWithFieldCorrection.setVisible(false);
                }
            }
        }
      }


    void setStripProcessLookup() {
        dfhProcessList.clear();
        dfhProcessList.addOneDFHProcess(new OneStripDFHProcess(this, dfhProcessList, "FH", "CR Lo-C emiss 0.32", "CR Lo-C emiss 0.34", 550, 0.00015));
        dfhProcessList.addOneDFHProcess(new OneStripDFHProcess(this, dfhProcessList, "CQ", "CR Lo-C emiss 0.32", "CR Lo-C emiss 0.34", 620, 0.0002));
    }

    String stripDFHProcessListInXML() {
//        StringBuffer xmlStr = new StringBuffer(XMLmv.putTag("exitTempAllowance", l2Furnace.getExitTempAllowance()));
//        xmlStr.append(dfhProcessList.dataInXMl());
//        xmlStr.append(XMLmv.putTag("pNum", stripProcessLookup.size()));
//        int pNum = 0;
//        for (OneStripDFHProcess oneProc: stripProcessLookup.values())
//            xmlStr.append(XMLmv.putTag("StripP" + ("" + ++pNum).trim(), oneProc.dataInXML().toString()) + "\n");
//        return xmlStr.toString();
        return dfhProcessList.dataInXMl().toString();
    }

    boolean takeStripProcessListFromXML(String xmlStr) {
////        stripProcessLookup.clear();
//        boolean retVal = false;
//        ValAndPos vp;
//        try {
//            vp = XMLmv.getTag(xmlStr, "exitTempAllowance", 0);
//            if (vp.val.length() > 0)
//                l2Furnace.setExitTempAllowance(Double.valueOf(vp.val));
//            else
//                l2Furnace.setExitTempAllowance(5);
//            retVal = true;
//        } catch (NumberFormatException e1) {
//            showError("Error in Number of StripDFHProc");
//        }
//        if (retVal)
//            retVal = dfhProcessList.takeStripProcessListFromXML(xmlStr);
//        return retVal;
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
        if (saved)
            l2Furnace.informProcessDataModified();
        else if (!gotTheLock)
            showError("Facing some problem in getting Lock");
        else
            showError("Process Data NOT saved");
        return saved ;
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
        }
        else {
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
        }
        else {
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
        if (saved)
            l2Furnace.informPerformanceDataModified();
        else if (!gotTheLock)
            showError("Facing some problem in getting Lock");
        else
            showError("Performance Data NOT saved");
        return saved ;
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
        return gotIt ;
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
                    }
                    else
                        showError("mismatch in DFH Process List file");
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
                    }
                    else
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
        if (dfhProcessList.addStripDFHProcess())
            showMessage("Strip DFh Process List updated");
    }

    void viewStripDFHProcess() {
        dfhProcessList.viewStripDFHProcess();
    }

    void createFceSetting() {
        l2Furnace.showEditFceSettings(true);
    }

    FileSystemView currentView;
    String fceSettExtension = "fceSett";

    boolean saveFurnaceSettings() {
        boolean retVal = false;
        if (isProfileCodeOK()) {
            String title = "Save Furnace Settings";
            FileChooserWithOptions fileDlg = new FileChooserWithOptions(title, "Furnace Settings (*." + fceSettExtension + ")", fceSettExtension);
            fileDlg.setSelectedFile(new File(profileCode + " Furnace Settings." + fceSettExtension));
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
                    oStream.write(("# Furnace Settings saved on " + dateFormat.format(new Date()) + " \n\n").getBytes());
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
        }
        else {
            showError("Save Profile before saving Furnace Settings");
        }
        return retVal;
    }

    void markThisFileAsBak(File file) {
        String bakFilefullName = file.getAbsolutePath() + ".bak";
        File existingBakFile = new  File(bakFilefullName);
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
        for (File file: files)
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
                            l2Info("Furnace Settings loaded");
//                            if (showDebugMessages)
//                                showMessage("Furnace Settings loaded");
                            retVal = true;
                        }
                    } else
                        showError("Mismatch in Furnace Settings Data");
                }
                iStream.close();
            } else
                showError("File size " + len + " for " + file);
        } catch (Exception e) {
            showError("Some Problem in getting Furnace Settings File!");
        }
        return retVal;
    }

    boolean loadFurnaceSettings(String filePath) {
        boolean retVal = false;
        if (!filePath.equals("nullnull")) {
//            debug("File for Furnace Settings :" + filePath);
            try {
                File file = new File(filePath);
                retVal = loadFurnaceSettings(file);
            } catch (Exception e) {
                showError("Some Problem in getting file " + filePath);
            }
        }
        return retVal;
    }

    boolean takeFieldResultsFromUser() {
        return l2Furnace.getFieldDataFromUser();
    }

    ErrorStatAndMsg takeResultsFromLevel1()  {
        if (bAllowL2Changes) {
            mIEvalForFieldProduction.setEnabled(false);
            mIEvalWithFieldCorrection.setEnabled(false);
        }
//        boolean retVal = false;
        ErrorStatAndMsg stat = l2Furnace.takeFieldResultsFromLevel1();
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
        if (l2Furnace.setFieldProductionData() ) {
//            showMessage("Recu Specs maintained as original");
//            l2Furnace.newRecu();
            l2Furnace.setCurveSmoothening(false);
            return calculateFce(true, resultsReadyListener);
        }
        else
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
        }
        else
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
        showError("Not ready toSave Field Results to file yet") ;
        return false;
    }

    boolean saveAsFieldResults() {
        boolean retVal = false;
        if (bResultsReady) {
            String ext =  ".fResult";
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

    boolean setupUaClient() {
        boolean retVal = false;
        try {
            uaClient = new TMuaClient(uaServerURI);
            uaClient.connect();
            retVal = uaClient.isConnected();
        } catch (Exception e) {
            showError("Exception :" + e.getMessage());
            e.printStackTrace();
//        } catch (URISyntaxException e) {
//            showError("URISyntaxException :" + e.getMessage());
//        } catch (SecureIdentityException e) {
//            showError("SecureIdentityException :" + e.getMessage());
//        } catch (IOException e) {
//            showError("IOException :" + e.getMessage());
//        } catch (SessionActivationException e) {
//            showError("SessionActivationException :" + e.getMessage());
//        } catch (ServerListException e) {
//            showError("ServerListException :" + e.getMessage());
        }
        return retVal;
    }

    static protected boolean parseCmdLineArgs(String[] args) {
        int i = 0;
        boolean retVal = false;
        if (DFHeating.parseCmdLineArgs(args)) {
            resetRightsForLevel2();
            L2CommandLineArgs cmdArg;
            while ((args.length > i)
                    && ((args[i].startsWith("-")))) {
                cmdArg = L2CommandLineArgs.getEnum(args[i]);
                switch (cmdArg) {
                    case DFHPROCESS:
                        bAllowEditDFHProcess = true;
                        break;
                    case FURNACESETTINGS:
                        bAllowEditFurnaceSettings = true;
                        break;
                    case FROMFIELD:
                        bAllowUpdateWithFieldData = true;
                        break;
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
        if (retVal)
            adjustRights();
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

    void setAccessLevel() {
        if (bAllowL2Changes)
            accessLevel = AccessLevel.EXPERT;
        else if (bAllowUpdateWithFieldData)
            accessLevel = AccessLevel.UPDATER;
        else
            accessLevel = AccessLevel.RUNTIME;
    }

    boolean getL2FceFromFile() {
        disableCompare();
        boolean bRetVal = false;
        furnace.resetSections();
        String filePath = fceDataLocation + "theFurnace.dfhDat";
//                 setResultsReady(false);
//                 setItFromTFM(false);
        furnace.resetLossAssignment();
        hidePerformMenu();
        //                furnace.clearPerfBase();
//        debug("Data file name :" + filePath);
//        fuelSpecsFromFile(fceDataLocation + "FuelSpecifications.dfhSpecs");
//        chMaterialSpecsFromFile(fceDataLocation + "ChMaterialSpecifications.dfhSpecs");
        bRetVal = getFceFromFceDatFile(filePath);
        switchPage(DFHDisplayPageType.INPUTPAGE);
        return bRetVal;
    }

    public boolean getFceFromFceDatFile(String filePath) {
        boolean retVal = super.getFceFromFceDatFile(filePath);
        return retVal;
    }

    boolean createL2Zones() {
        boolean retVal = false;
        if (l2Furnace.createL2Zones()) {
            l2Info("DFHeatingLevel2 initiated");
            retVal = true;
        }
        else
            showError("Facing some problem inL2 Connection!");
        return retVal;
    }

    void getFuelAndCharge() {
        fuelSpecsFromFile(fceDataLocation + "FuelSpecifications.dfhSpecs");
        chMaterialSpecsFromFile(fceDataLocation + "ChMaterialSpecifications.dfhSpecs");
    }

//    protected ChMaterial getSelChMaterial(String matName) {
//        return super.getSelChMaterial(matName);
//    }

    String profileCode = "";
    String profileCodeTag = "profileCode";
    DecimalFormat profileCodeFormat = new DecimalFormat("000000");

    String createProfileCode() {
        profileCode = profileCodeFormat.format(Math.random() * 999999.0);
        return profileCode;
    }

    boolean checkProfileCode(String withThis) {
        return (withThis != null) && (profileCode.equals(withThis));
    }

    public String inputDataXML(boolean withPerformance) {
        return profileCodeInXML() + super.inputDataXML(withPerformance);
    }

    boolean isProfileCodeOK() {
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

    public String takeProfileDataFromXML(String xmlStr) {
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, profileCodeTag);
        profileCode = vp.val;
//        l2Debug("profile code <" + profileCode + ">");
        if (isProfileCodeOK()) {
            return super.takeProfileDataFromXML(xmlStr);
        }
        else
            return "ERROR: Not a Level2 Furnace Profile";
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
            File f = new File(fileName);
            boolean goAhead = true;
            if (goAhead) {
                try {
                    BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(fileName));
                    oStream.write(inputDataXML(withPerformance).getBytes());
                    oStream.close();
                    if (withPerformance)
                        furnace.performaceIsSaved();
                } catch (FileNotFoundException e) {
                    showError("File " + fileName + " NOT found!");
                } catch (IOException e) {
                    showError("Some IO Error in writing to file " + fileName + "!");
                }
            }
        }
        parent().toFront();
    }

    protected boolean getStripDFHProcessList() {
        boolean retVal = false;
        File file = getParticularFile(fceDataLocation, profileCode, "stripProc");
        if (file != null) {
            retVal = loadStripDFHProcessList(file);
            l2Info("loaded file " + file);
        }
        else
            showError("Unable to locate StripDFHProcess List File");
        return retVal;
    }

    protected boolean getFurnaceSettings() {
        boolean retVal = false;
        File file = getParticularFile(fceDataLocation, profileCode, "fceSett");
        if (file != null) {
            retVal = loadFurnaceSettings(file);
            l2Info("loaded file " + file);
        }
        else
            showError("Unable to locate Furnace Settings File");
        return retVal;
    }

    protected boolean getPerformanceList() {
        boolean retVal = false;
        File file = getParticularFile(fceDataLocation, profileCode, "perfData");
        if (file != null) {
            retVal = loadPerformanceData(file);
            l2Info("Updated Performance Data from file " + file);
        }
        return retVal;
    }

    File getParticularFile(String directory, final String startName, final String extension) {
        File selectedFile = null;
        File folder = new File(directory);
        File[] files = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(extension) && name.startsWith(startName);
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
        return dfhProcessList.getDFHProcess(forProc.toUpperCase());
    }

    public void resultsReady(Observations observations) {
        super.resultsReady(observations);
        l2Furnace.setCurveSmoothening(true);
        if (proc == DFHTuningParams.ForProcess.STRIP) {
            mISaveAsFieldResult.setEnabled(true);
        }
    }

    protected void enableResultsMenu(boolean enable) {
        super.enableResultsMenu(enable);
        if (l2MenuSet && bAllowL2Changes) {
            mISaveAsFieldResult.setEnabled(enable);
        }
    }

    boolean testMachineID() {
        boolean keyOK = false;
        boolean newKey = false;
        MachineCheck  mc = new MachineCheck();
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
//                showMessage("key = " + key);
//                showMessage("Machine OK = " + mc.checkKey(key));
                keyOK = mc.checkKey(key);
                if (!newKey && !keyOK) {
                    boolean response = decide("Software key", "There is some problem in the saved key\n"
                            + " Do you want to delete the earlier key data and enter the key manually?");
                    if (response) {
                        key = "";
                        continue;
                    }
                }
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

    public void handleModifiedProcessData() {
        ProcessModificationHandler theProcessModHandler= new ProcessModificationHandler();
        Thread t = new Thread(theProcessModHandler);
        t.start();
    }

    class ProcessModificationHandler implements Runnable {
        public void run() {
            int count = 5;
            boolean gotIt = false;
            while(--count > 0) {
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
                info("Unable to read modified Process Data");
            l2Furnace.setProcessBeingUpdated(false);
        }
    }



    void informLevel2Ready() {
        l2Furnace.informLevel2Ready();
    }

    String getKeyFromUser(String machineID) {
        QueryDialog dlg = new QueryDialog(mainF, "Software keyString");
        JTextField mcID = new JTextField(machineID);
        mcID.setEditable(false);
        JTextField keyF = new JTextField(machineID.length() + 1);
        dlg.addQuery("Installation ID", mcID);
        dlg.addQuery("Enter key for the above installation ID", keyF);
        dlg.setLocation(100, 100);
        dlg.setVisible(true);
        if (dlg.isUpdated())
            return keyF.getText();
        else
            return "";
    }
    
    public static void l2debug(String msg) {
        if (bl2ShowDebugMessages)
            debug("L2DFHeating:" + msg);
    }

    public static void l2Info(String msg) {
        if (log != null)
            log.info("l2DFHeating:" + msg);
    }

    public void close() {
        if (decide("Quitting Level2", "Do you really want to Exit this Level2 Application?", false))
            exitFromLevel2();
//        {l2Furnace.prepareForDisconnection();
//            if (uaClient != null)
//                uaClient.disconnect();
//            super.close();
//        }
    }

    public void exitFromLevel2() {
        l2Furnace.prepareForDisconnection();
        if (uaClient != null)
            uaClient.disconnect();
        super.close();
    }

    class L2MenuListener implements ActionListener {
         public void actionPerformed(ActionEvent e) {
             Object caller = e.getSource();
             if (caller == mIReadDFHStripProcess)
                 loadStripDFHProcessList();
             else if (caller == mISaveDFHStripProcess)
                 updateProcessDataFile();
//                 saveDFHProcessList();
             else if (caller == mIViewDFHStripProcess)
                 viewStripDFHProcess();
             else if (caller == mIEditDFHStripProcess)
                 editStripDFHProcess();
             else if (caller == mICreateFceSettings)
                 createFceSetting();
             else if (caller == mIReadFceSettings)
                 readFurnaceSettings();
             else if (caller == mISaveFceSettings)
                 saveFurnaceSettings();
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
//                 savePerformanceDataToFile();
             else if (caller == mIReadPerformanceData) {
                 loadPerformanceData();
                 updateDisplay(DFHDisplayPageType.PERFOMANCELIST);
             }
         }
    }

    public static void main(String[] args) {
        //        PropertyConfigurator.configureAndWatch(DFHeating.class
        //                .getResource("log.properties").getFile(), 5000);
//        L2DFHeating.log = Logger.getLogger("L2DFHeating.class");
        final L2DFHeating level2Heating = new L2DFHeating("Furnace");
        if (level2Heating.parseCmdLineArgs(args)) {
            if (level2Heating.setupUaClient()) {
                level2Heating.setItUp();
                if (level2Heating.l2SystemReady) {
                    level2Heating.informLevel2Ready();
                }
                else {
                    level2Heating.showError("Level2 could not be started. Aborting ...");
                    level2Heating.exitFromLevel2();
//                    System.exit(1);
                }
            }
            else
                level2Heating.showMessage("Facing problem connecting to Level1. Aborting ...");
        }
    }
}
