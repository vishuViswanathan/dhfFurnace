package level2;

import TMopcUa.TMuaClient;
import basic.*;
import directFiredHeating.DFHTuningParams;
import directFiredHeating.DFHeating;
import display.QueryDialog;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import org.apache.log4j.Logger;
import protection.MachineCheck;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
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
    String fceDataLocation = "level2FceData/";
    TMuaClient uaClient;
    static String uaServerURI;
    L2DFHFurnace l2Furnace;
    public String equipment;
    StripDFHProcessList dfhProcessList;
//    Hashtable<String, OneStripDFHProcess> stripProcessLookup;
    static boolean allowL2Changes = false;
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
        releaseDate = "20150514";
        onProductionLine = true;
        asApplication = true;
        this.equipment = equipment;
//        stripProcessLookup = new Hashtable<String, OneStripDFHProcess>();
//        init();
    }

    public void init() {
        modifyJTextEdit();
        fuelList = new Vector<Fuel>();
        vChMaterial = new Vector<ChMaterial>();
        setUIDefaults();
        mainF = new JFrame();
        mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        if (log == null) {
            log = Logger.getLogger(DFHeating.class);
            // Load Log4j configurations from external file
        }
        mainF.setTitle("DFH Furnace Level2 " + releaseDate + testTitle);

        tuningParams = new DFHTuningParams(this, onProductionLine, 1, 5, 30, 1.12, 1, false, false);
        debug("Creating new Level2furnace");
        l2Furnace = new L2DFHFurnace(this, false, false, lNameListener);
        furnace = l2Furnace;
        debug("Created Level2furnace");
        furnace.setTuningParams(tuningParams);
        createUIs();
        getFuelAndCharge();
        if (!testMachineID()) {
            showError("Software key mismatch, Aborting ...");
            close();
        }
        displayIt();
        getL2FceFromFile();
//        displayIt();
//        if (l2Furnace.createL2Zones())
//            info("DFHeatingLevel2 inited");
//        else
//            showError("Facing some problem inL2 Connection!");
        enableDataEdit();
        if (allowL2Changes) {
            mL2Configuration = new JMenu("L2 Config");
            mICreateFceSettings = new JMenuItem("View/Edit Furnace Settings");
            mIReadFceSettings = new JMenuItem("Read Furnace Settings from file");
            mISaveFceSettings = new JMenuItem("Save Furnace Settings to file");

            mIEditDFHStripProcess = new JMenuItem("Add/Edit StripDFHProcess List");
            mIViewDFHStripProcess = new JMenuItem("View StripDFHProcess List");
            mIReadDFHStripProcess = new JMenuItem("Read StripDFHProcess List from File");
            mISaveDFHStripProcess = new JMenuItem("Save StripDFHProcess List to File");

            mICreateFieldResultsData = new JMenuItem("Enter Field Results Data");
            mISaveFieldResultsToFile = new JMenuItem("Save Field Results to file");
            mILevel1FieldResults = new JMenuItem("Take Results From Level1");
            mILoadFieldResult = new JMenuItem("Load Field Results from File");
            mISaveAsFieldResult = new JMenuItem("Save As Field Results");
            mISaveAsFieldResult.setEnabled(false);
            mIEvalForFieldProduction = new JMenuItem("Calculate for Field Production");
            mIEvalForFieldProduction.setEnabled(false);
            mIEvalWithFieldCorrection = new JMenuItem("Re-Calculate With Field Corrections");
            mIEvalWithFieldCorrection.setEnabled(false);

            StripProcMenuListener li = new StripProcMenuListener();
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

            mL2Configuration.add(mICreateFceSettings);
            mL2Configuration.add(mIReadFceSettings);
            mL2Configuration.add(mISaveFceSettings);
            mL2Configuration.addSeparator();
            mL2Configuration.add(mIViewDFHStripProcess);
            mL2Configuration.add(mIEditDFHStripProcess);
            mL2Configuration.add(mIReadDFHStripProcess);
            mL2Configuration.add(mISaveDFHStripProcess);
            mL2Configuration.addSeparator();
            mL2Configuration.add(mICreateFieldResultsData);
            mL2Configuration.add(mISaveFieldResultsToFile);
            mL2Configuration.addSeparator();
            mL2Configuration.add(mILevel1FieldResults);
            mL2Configuration.add(mILoadFieldResult);
            mL2Configuration.add(mIEvalForFieldProduction);
            mL2Configuration.add(mIEvalWithFieldCorrection);
            mL2Configuration.addSeparator();
            mL2Configuration.add(mISaveAsFieldResult);
            mL2Configuration.setEnabled(true);


            mb.add(mL2Configuration);
            mb.updateUI();
            l2MenuSet = true;
            setFcefor(true);
        }
        dfhProcessList = new StripDFHProcessList(this);
        if (!getStripDFHProcessList()) {
            showError("Problem loading test StripDFHProcess list data");
            setStripProcessLookup();
        }
        if (!getFurnaceSettings()) {
            showError("Problem in loading Furnace Settings");
        }
    }

    class StripProcMenuListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object caller = e.getSource();
            if (caller == mIReadDFHStripProcess)
                readStripProcessLookup();
            else if (caller == mISaveDFHStripProcess)
                saveSripProcessLookup();
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
                recalculateWithFieldCorrections();
        }
    }

    protected void setFcefor(boolean showSuggestion) {
        super.setFcefor(showSuggestion);
        if (showSuggestion) {
            DFHTuningParams.ForProcess forProc = getFceFor();
            if (allowL2Changes) {
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
        StringBuffer xmlStr = new StringBuffer(XMLmv.putTag("exitTempAllowance", l2Furnace.getExitTempAllowance()));
        xmlStr.append(dfhProcessList.dataInXMl());
//        xmlStr.append(XMLmv.putTag("pNum", stripProcessLookup.size()));
//        int pNum = 0;
//        for (OneStripDFHProcess oneProc: stripProcessLookup.values())
//            xmlStr.append(XMLmv.putTag("StripP" + ("" + ++pNum).trim(), oneProc.dataInXML().toString()) + "\n");
        return xmlStr.toString();
    }

    boolean takeStripProcessListFromXML(String xmlStr) {
//        stripProcessLookup.clear();
        boolean retVal = false;
        ValAndPos vp;
        try {
            vp = XMLmv.getTag(xmlStr, "exitTempAllowance", 0);
            if (vp.val.length() > 0)
                l2Furnace.setExitTempAllowance(Double.valueOf(vp.val));
            else
                l2Furnace.setExitTempAllowance(5);
            retVal = true;
        } catch (NumberFormatException e1) {
            showError("Error in Number of StripDFHProc");
        }
        if (retVal)
            retVal = dfhProcessList.takeStripProcessListFromXML(xmlStr);
        return retVal;
    }

    boolean saveSripProcessLookup() {
        boolean retVal = false;
        String title = "Save StripDFHProcess list";
        FileDialog fileDlg =
                new FileDialog(mainF, title,
                        FileDialog.SAVE);
        fileDlg.setFile("*.stripProc");
        fileDlg.setVisible(true);

        String bareFile = fileDlg.getFile();
        if (!(bareFile == null)) {
            int len = bareFile.length();
            if ((len < 11) || !(bareFile.substring(len - 10).equalsIgnoreCase(".stripProc"))) {
                showMessage("Adding '.stripProc' to file name");
                bareFile = bareFile + ".stripProc";
            }
            String fileName = fileDlg.getDirectory() + bareFile;
            debug("SaveStripDFHProcess list file name :" + fileName);
            File f = new File(fileName);
            boolean goAhead = true;
            if (goAhead) {
                try {
                    BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(fileName));
                    oStream.write(("# StripDFhProcess List saved on " + dateFormat.format(new Date()) + " \n\n").getBytes());
                    oStream.write(stripDFHProcessListInXML().getBytes());
                    oStream.close();
                    retVal = true;
                } catch (FileNotFoundException e) {
                    showError("File " + fileName + " NOT found!");
                } catch (IOException e) {
                    showError("Some IO Error in writing to file " + fileName + "!");
                }
            }
        }
        parent().toFront();
        return retVal;
    }

    boolean readStripProcessLookup() {
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
            debug("File for StripDFHProcess list :" + filePath);
            try {
                BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(filePath));
                //           FileInputStream iStream = new FileInputStream(fileName);
                File f = new File(filePath);
                long len = f.length();
                if (len > 50 && len < 10000) {
                    int iLen = (int) len;
                    byte[] data = new byte[iLen + 10];
                    if (iStream.read(data) > 50) {
                        if (takeStripProcessListFromXML(new String(data))) {
                            showMessage("StripDFHProcess list loaded");
                            retVal = true;
                        }
                    }
                } else
                    showError("File size " + len + " for " + filePath);
            } catch (Exception e) {
                showError("Some Problem in getting file!");
            }
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

    boolean saveFurnaceSettings() {
        boolean retVal = false;
        String title = "Save Furnace Settings";
        FileDialog fileDlg =
                new FileDialog(mainF, title,
                        FileDialog.SAVE);
        fileDlg.setFile("*.fceSett");
        fileDlg.setVisible(true);

        String bareFile = fileDlg.getFile();
        if (!(bareFile == null)) {
            int len = bareFile.length();
            if ((len < 9) || !(bareFile.substring(len - 8).equalsIgnoreCase(".fceSett"))) {
                showMessage("Adding '.fceSett' to file name");
                bareFile = bareFile + ".fceSett";
            }
            String fileName = fileDlg.getDirectory() + bareFile;
            debug("Save Furnace Settings file name :" + fileName);
//            File f = new File(fileName);
            boolean goAhead = true;
            if (goAhead) {
                try {
                    BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(fileName));
                    oStream.write(("# Furnace Settings saved on " + dateFormat.format(new Date()) + " \n\n").getBytes());
                    oStream.write(l2Furnace.fceSettingsInXML().getBytes());
                    oStream.close();
                    retVal = true;
                } catch (FileNotFoundException e) {
                    showError("File " + fileName + " NOT found!");
                } catch (IOException e) {
                    showError("Some IO Error in writing to file " + fileName + "!");
                }
            }
        }
        parent().toFront();
        return retVal;
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

    boolean loadFurnaceSettings(String filePath) {
        boolean retVal = false;
        if (!filePath.equals("nullnull")) {
            debug("File for Furnace Settings :" + filePath);
            try {
                BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(filePath));
                //           FileInputStream iStream = new FileInputStream(fileName);
                File f = new File(filePath);
                long len = f.length();
                if (len > 50 && len < 10000) {
                    int iLen = (int) len;
                    byte[] data = new byte[iLen + 10];
                    if (iStream.read(data) > 50) {
                        if (l2Furnace.takeFceSettingsFromXML(new String(data))) {
                            showMessage("Furnace Settings loaded");
                            retVal = true;
                        }
                    }
                    iStream.close();
                } else
                    showError("File size " + len + " for " + filePath);
            } catch (Exception e) {
                showError("Some Problem in getting file!");
            }
        }
        return retVal;
    }

    boolean takeFieldResultsFromUser() {
        return l2Furnace.getFieldDataFromUser();
    }

    boolean takeResultsFromLevel1()  {
        mIEvalForFieldProduction.setEnabled(false);
        mIEvalWithFieldCorrection.setEnabled(false);
        boolean retVal = false;
        retVal = l2Furnace.takeFieldResultsFromLevel1();
        if (retVal) {
            mIEvalForFieldProduction.setEnabled(true);
        }
        return retVal;
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


    boolean evalForFieldProduction() {   // @TODO incomplete
        mIEvalWithFieldCorrection.setEnabled(true);
        if (l2Furnace.setFieldProductionData() ) {
            showMessage("Recu Specs maintained as original");
//            l2Furnace.newRecu();
            l2Furnace.setCurveSmoothening(false);
            calculateFce();
            return true;
        }
        else
            return false;
    }

    boolean recalculateWithFieldCorrections() {   //  TODO not complete
        if (l2Furnace.adjustForFieldResults()) {
            calculateFce(false); // without reset the loss Factors
            mIEvalForFieldProduction.setEnabled(false);
            return true;
        }
        else
            return false;
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
            CommandLineArgs cmdArg;
            while ((args.length > i)
                    && ((args[i].startsWith("-")))) {
                cmdArg = CommandLineArgs.getEnum(args[i]);
                switch (cmdArg) {
                    case ALLOWCHANGES:
                        allowL2Changes = true;
                }
                i++;
            }
            if (i < args.length) {
                uaServerURI = args[i++];
                retVal = true;
            }
        }
        return retVal;
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
        debug("Data file name :" + filePath);
//        fuelSpecsFromFile(fceDataLocation + "FuelSpecifications.dfhSpecs");
//        chMaterialSpecsFromFile(fceDataLocation + "ChMaterialSpecifications.dfhSpecs");
        bRetVal = getFceFromFceDatFile(filePath);
        switchPage(InputType.INPUTPAGE);
        return bRetVal;
    }

    public boolean getFceFromFceDatFile(String filePath) {
        boolean retVal = super.getFceFromFceDatFile(filePath);
        if (retVal) {
            if (l2Furnace.createL2Zones())
                info("DFHeatingLevel2 initiated");
            else
                showError("Facing some problem inL2 Connection!");
        }
        return retVal;
    }

    void getFuelAndCharge() {
        fuelSpecsFromFile(fceDataLocation + "FuelSpecifications.dfhSpecs");
        chMaterialSpecsFromFile(fceDataLocation + "ChMaterialSpecifications.dfhSpecs");
    }

//    protected ChMaterial getSelChMaterial(String matName) {
//        return super.getSelChMaterial(matName);
//    }

    protected boolean getStripDFHProcessList() {
        String filePath = fceDataLocation + "stripDFHProcessList.stripProc";
        return loadStripDFHProcessList(filePath);
    }

    protected boolean getFurnaceSettings() {
        String filePath = fceDataLocation + "FurnaceSettings.fceSett";
        return loadFurnaceSettings(filePath);
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
        if (l2MenuSet) {
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
        debug("Data file name for saving key:" + filePath);
        try {
            BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(filePath));
            oStream.write(keyFileHead.getBytes());
            oStream.write(key.getBytes());
            oStream.close();
            done = true;
        } catch (FileNotFoundException e) {
            debug("Could not create file " + filePath);
        } catch (IOException e) {
            debug("Some IO Error in writing to file " + filePath + "!");
        }
        if (done)
            debug("key saved to " + filePath);
        else
            showError("Unable to save software key");
    }

    String getKeyFromFile() {
        String key = "";
        String filePath = fceDataLocation + "machineKey.ini";
        debug("Data file name :" + filePath);
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

    public void close() {
        l2Furnace.prepareForDisconnection();
        if (uaClient != null)
            uaClient.disconnect();
        super.close();
    }


    public static void main(String[] args) {
        //        PropertyConfigurator.configureAndWatch(DFHeating.class
        //                .getResource("log.properties").getFile(), 5000);
        final L2DFHeating level2Heating = new L2DFHeating("Furnace");
        if (level2Heating.parseCmdLineArgs(args)) {
            if (level2Heating.setupUaClient()) {
                level2Heating.init();
            }
            else
                level2Heating.showMessage("Facing problem connecting to Level1");
        }

        level2Heating.setVisible(true);
/*

         try {
             TMuaClient opc = new TMuaClient("opc.tcp://127.0.0.1:49320");
             opc.connect();
             Level2Zone zone1Temperature = new Level2Zone(opc, "Furnace", "DFHzone1.temperature");
             readInput(true);
             System.out.println("PV " + zone1Temperature.getPV());
             System.out.println("SP " + zone1Temperature.getSP());
             System.out.println("auto " + zone1Temperature.getAuto());
             boolean resp = zone1Temperature.setSP(37.3);
             System.out.println("Changing SP to 37.30, resp = " + resp);
             System.out.println("SP " + zone1Temperature.getSP());
             System.out.println("stripMode " + zone1Temperature.getStripMode());
             zone1Temperature.setStripMode(true);
             System.out.println("Changing strip mode to true");
             System.out.println("stripMode  imm after change " + zone1Temperature.getStripMode());
             System.out.println("Changing strip mode to false");
             zone1Temperature.setStripMode(false);
             System.out.println("stripMode  imm after change " + zone1Temperature.getStripMode());
             readInput(true);
             System.out.println("SP " + zone1Temperature.getSP());
             System.out.println("stripMode " + zone1Temperature.getStripMode());

             opc.disconnect();

 //            TMuaClient.testFromOutside();
         } catch (Exception e) {
             e.printStackTrace();
         }
*/
    }

}
