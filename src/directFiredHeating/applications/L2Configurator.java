package directFiredHeating.applications;

import basic.ChMaterial;
import basic.Fuel;
import directFiredHeating.DFHTuningParams;
import directFiredHeating.accessControl.L2AccessControl;
import directFiredHeating.accessControl.OfflineAccessControl;
import directFiredHeating.process.StripDFHProcessList;
import directFiredHeating.stripDFH.StripFurnace;
import mvUtils.display.DataStat;
import mvUtils.display.StatusWithMessage;
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
        fceDataLocation = "level2FceData/mustBeUserEntry/";
        bL2Configurator = true;
        enableSpecsSave = true;
        onProductionLine = false;
        bAllowProfileChange = true;
        bAllowManualCalculation = true;
        asApplication = true;
        releaseDate = "20161118 10:42";
        createLocalMenuItems();
    }

    public boolean setItUp() {
        modifyJTextEdit();
        fuelList = new Vector<Fuel>();
        vChMaterial = new Vector<ChMaterial>();
        dfhProcessList = new StripDFHProcessList(this);
        setUIDefaults();
        mainF = new JFrame();
        try {
            accessControl = new OfflineAccessControl(asJNLP, mainF);
            mainF.setTitle("DFH Furnace - L2 Configurator - " + releaseDate + testTitle);

            tuningParams = new DFHTuningParams(this, false, 1, 5, 30, 1.12, 1, false, false);
            furnace = new StripFurnace(this, false, false, lNameListener);
            furnace.setTuningParams(tuningParams);
            tuningParams.bConsiderChTempProfile = true;
            createUIs(false); // without the default menuBar
            mISetPerfTablelimits.setVisible(true);
            disableSomeUIs();
//            addMenuBar(createL2MenuBar(true, true));
            associatedDataLoaded = true;
            loadFuelAndChMaterialData();
            setDefaultSelections();
            setTestData();
            switchPage(DFHDisplayPageType.INPUTPAGE);
            displayIt();

            showMessage("The furnace has to be of " + HeatingMode.TOPBOTSTRIP + " for " + DFHTuningParams.FurnaceFor.STRIP +
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
        final L2Configurator l2Preparer = new L2Configurator();
        if (parseCmdLineArgs(args)) {
            l2Preparer.setItUp();
            if (!l2Preparer.associatedDataLoaded) {
                l2Preparer.showError(" Aborting ...");
                System.exit(1);
            }
        }
    }
}
