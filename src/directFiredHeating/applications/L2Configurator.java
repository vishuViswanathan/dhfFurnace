package directFiredHeating.applications;

import basic.ChMaterial;
import basic.Fuel;
import directFiredHeating.DFHTuningParams;
import directFiredHeating.DFHeating;
import directFiredHeating.accessControl.L2AccessControl;
import directFiredHeating.accessControl.OfflineAccessControl;
import directFiredHeating.process.StripDFHProcessList;
import directFiredHeating.stripDFH.StripFurnace;
import level2.applications.Level2Installer;
import mvUtils.display.DataStat;
import mvUtils.display.DataWithStatus;
import mvUtils.display.StatusWithMessage;
import mvUtils.file.ActInBackground;
import mvUtils.file.WaitMsg;
import mvUtils.jsp.JSPObject;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import protection.CheckAppKey;
import tmiOnly.GetSoftwareKey;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
 * User: M Viswanathan
 * Date: 18-Jul-16
 * Time: 4:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2Configurator extends StripHeating {
    JMenu mAccessControl;
    JMenuItem mIDeleteInstallerAccess;
    JMenuItem mIAddInstallerAccess;
    JMenuItem mILoadAccessFile;
    JMenuItem mISaveAccessFile;
    OfflineAccessControl accessControl;
    boolean accessDataModified = false;

    public L2Configurator() {
        super();
        appCode = 102;
        fceDataLocation = "level2FceData/mustBeUserEntry/";
        bL2Configurator = true;
        enableSpecsSave = true;
        onProductionLine = false;
        bAllowProfileChange = true;
        bAllowManualCalculation = true;
        asApplication = true;
        releaseDate = " 20170901";
//        debugLocal("L2Configurator.49");
        createLocalMenuItems();
    }

    Fuel lastSelected = null;

    public boolean setItUp() {
        boolean retVal = false;
//        debugLocal("L2Configurator.56");
        if (getJSPbase() && getJSPConnection()) {
            DataWithStatus<Boolean> runCheck = new CheckAppKey(jspBase).canRunThisApp(appCode, true);
            if (runCheck.getStatus() == DataStat.Status.OK) {
                modifyJTextEdit();
                fuelList = new Vector<Fuel>();
                vChMaterial = new Vector<ChMaterial>();
                dfhProcessList = new StripDFHProcessList(this);
                setUIDefaults();
                mainF = new JFrame();
                mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                try {
//                debugLocal("L2Configurator.67");
                    accessControl = new OfflineAccessControl(asJNLP, mainF);
                    if (log == null)
                        startLog4j();
                    mainF.setTitle("DFH Furnace - L2 Configurator - " + releaseDate + testTitle);

                    tuningParams = new DFHTuningParams(this, false, 1, 5, 30, 1.12, 1, false, false);
                    furnace = new StripFurnace(this, false, false, lNameListener);
                    furnace.setTuningParams(tuningParams);
                    tuningParams.bConsiderChTempProfile = true;
                    tuningParams.bAdjustChTempProfile = true;
                    createUIs(false); // without the default menuBar
                    mISetPerfTablelimits.setVisible(true);
                    disableSomeUIs();
//            addMenuBar(createL2MenuBar(true, true));
                    if (loadFuelAndChMaterialData()) {
                        setDefaultSelections();
                        setTestData();
                        switchPage(DFHDisplayPageType.INPUTPAGE);
                        if (asJNLP || justJSP) {
                            cbFuel.addActionListener(e -> {
                                Fuel nowSelected = (Fuel) cbFuel.getSelectedItem();
//                    showMessage("Fuel " + nowSelected);
                                if (nowSelected != null) {
                                    if (lastSelected != null) {
                                        if (decide("Change of Fuel", "The Fuel has been changed. The earlier selection will be Deleted" +
                                                "\nEnsure that there are no Performance data with earlier fuel")) {
                                            ((JSPObject) lastSelected).unCollectData();
                                            lastSelected = nowSelected;
                                        } else {
                                            cbFuel.setSelectedItem(lastSelected);
                                            ((JSPObject) nowSelected).unCollectData();
                                        }
                                    } else
                                        lastSelected = nowSelected;
                                }
                            });
                        }
                        displayIt();

                        showMessage("The furnace has to be of " + HeatingMode.TOPBOTSTRIP + " for " + DFHTuningParams.FurnaceFor.STRIP +
                                "\n\nIt is the responsibility of the user to ensure data integrity among:" +
                                "\n      1) Profile including Fuel type " +
                                "\n      2) IP address of OPC server  '" + mL2Configuration.getText() + "'" +
                                "\n      3) L2 Basic settings under '" + mL2Configuration.getText() + "'" +
                                "\n      4) DHFProcess List data under '" + mL2Configuration.getText() + "'" +
                                "\n      5) Performance Data under '" + perfMenu.getText() + "'" +
                                "\n\nIt is suggested that the profile and L2 Basic Setting are finalised before updating" +
                                "\nthe other data." +
                                "\n\nBefore exiting, ensure that the Furnace data is saved/updated through 'File' menu.");
                        associatedDataLoaded = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("Java Version :" + System.getProperty("java.version"));
                retVal = associatedDataLoaded;
            }
            else {
                if (runCheck.getStatus() == DataStat.Status.WithErrorMsg)
                    showError("Access Check" , runCheck.getErrorMessage());
                else
                    showError("Access Check", "Some problem in getting Application permissions");
            }
        }
        else
            showError("Access Check", "Unable to connect to Server");
        return retVal;
    }

    protected void createAllMenuItems() {
        super.createAllMenuItems();
        MenuListener li = new MenuListener();
        mILoadAccessFile = new JMenuItem("Load Access File");
        mIAddInstallerAccess = new JMenuItem("Add Installer Access");
        mIDeleteInstallerAccess = new JMenuItem("Delete one Installer");
        mISaveAccessFile = new JMenuItem("Save Access File");
        mILoadAccessFile.addActionListener(li);
        mIAddInstallerAccess.addActionListener(li);
        mIDeleteInstallerAccess.addActionListener(li);
        mISaveAccessFile.addActionListener(li);
    }

    protected JMenu createFileMenu() {
        defineFileMenu();
//        fileMenu = new JMenu("File");
        fileMenu.add(mIGetFceProfile);
        fileMenu.addSeparator();
        fileMenu.add(mILoadRecuSpecs);
        fileMenu.addSeparator();
        fileMenu.add(mISaveFceProfile);
        fileMenu.add(mIUpdateFurnace);
        fileMenu.addSeparator();
        fileMenu.add(mISaveFuelSpecs);
        fileMenu.add(mISaveSteelSpecs);
        fileMenu.addSeparator();
        fileMenu.add(mIExit);
        return fileMenu;
    }

    JMenu createAccessMenu() {
        mAccessControl = new JMenu("Access Control");
        mAccessControl.add(mIGetSoftwareKey);
        mAccessControl.addSeparator();
        mAccessControl.add(mILoadAccessFile);
        mAccessControl.addSeparator();
        mAccessControl.add(mIAddInstallerAccess);
        mAccessControl.add(mIDeleteInstallerAccess);
        mAccessControl.addSeparator();
        mAccessControl.add(mISaveAccessFile);
        return mAccessControl;
    }

    JMenuItem mIGetSoftwareKey;

    void createLocalMenuItems() {
        SoftKeyListener li = new SoftKeyListener();
        mIGetSoftwareKey = new JMenuItem("Get Software Key");
        mIGetSoftwareKey.addActionListener(li);
    }

    protected void startLog4j() {
        PropertyConfigurator.configure("L2Configurator.properties");
        log = Logger.getLogger(L2Configurator.class);
    }

    protected boolean isProfileCodeOK() {
        boolean retVal = false;
        if (profileCode.length() == profileCodeFormat.format(0).length())
            retVal = true;
        else {
            if (decide("Level2 Profile", "The profile is not be prepared for Level2." +
            "\nSelect YES to continue as Level2 or NO to exit")) {
                createProfileCode();
                retVal = true;
            }
        }
        return retVal;
    }

    class SoftKeyListener implements ActionListener  {
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (src == mIGetSoftwareKey) {
                (new GetSoftwareKey()).getKey();
            }
        }
    }

    protected JMenuBar assembleMenuBar() {
        JMenuBar mb= super.assembleMenuBar();
        mb.add(createAccessMenu());
        return mb;
    }

    boolean loadAccessFile() {
        accessDataModified = false;
        return (accessControl.loadAccessFile().getDataStatus() == DataStat.Status.OK);

    }

    boolean saveAccessFile() {
        accessDataModified = false;
        return (accessControl.saveAccessFile().getDataStatus() == DataStat.Status.OK);
    }

    void addInstallerAccess() {
        StatusWithMessage response =
                accessControl.addNewUser(L2AccessControl.AccessLevel.INSTALLER);
        if (response.getDataStatus() == DataStat.Status.OK)
            accessDataModified = true;
        else
            showError(response.getErrorMessage());
    }

    void deleteInstallerAccess() {
        StatusWithMessage response =
                accessControl.deleteUser(L2AccessControl.AccessLevel.INSTALLER);
        if (response.getDataStatus() == DataStat.Status.OK)
            accessDataModified = true;
        else
            showError(response.getErrorMessage());
    }

    static protected boolean  parseCmdLineArgs(String[] args) {
        boolean retVal = false;
        if (StripHeating.parseCmdLineArgs(args)) {
            retVal = true;
        }
        return retVal;
    }

    class MenuListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object caller = e.getSource();
            if (caller == mIAddInstallerAccess)
                addInstallerAccess();
            else if (caller == mIDeleteInstallerAccess)
                deleteInstallerAccess();
            else if (caller == mILoadAccessFile)
                loadAccessFile();
            else if (caller == mISaveAccessFile)
                saveAccessFile();
        }
    }

    public static void main(String[] args) {
        L2Configurator l2Preparer;
        new WaitMsg(null, "Starting Level2 Configurator. Please wait ...", new ActInBackground() {
            public void doInBackground() {
                L2Configurator l2Configurator = new L2Configurator();
                if (parseCmdLineArgs(args)) {
                    l2Configurator.setItUp();
                    if (!l2Configurator.associatedDataLoaded) {
                        l2Configurator.showError(" Unable to Get Application Data.\nAborting ...");
                        System.exit(1);
                    }
                }
            }
        });
    }
}
