package level2;

import TMopcUa.TMuaClient;
import basic.*;
import directFiredHeating.DFHTuningParams;
import directFiredHeating.DFHeating;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Date;
import java.util.Hashtable;
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
    Hashtable<String, OneStripDFHProcess> stripProcessLookup;
    static boolean allowL2Changes = false;
    JMenu mL2Configuration;
    JMenuItem mIAddDFHStripProcess;
    JMenuItem mIReadDFHStripProcess;
    JMenuItem mISaveDFHStripProcess;
    JMenuItem mICreateFceSettings;
    JMenuItem mISaveFceSettings;
    JMenuItem mIReadFceSettings;

    JMenuItem mILoadFieldResult;
    JMenuItem mILevel1FieldResults;
    JMenuItem mISaveFieldResult;

    JMenuItem mIEvalForFieldProduction;
    JMenuItem mIEvalWithFieldCorrection;

    boolean l2MenuSet = false;

    public L2DFHeating(String equipment) {
        super();
        releaseDate = "20150220";
        onProductionLine = true;
        asApplication = true;
        this.equipment = equipment;
        stripProcessLookup = new Hashtable<String, OneStripDFHProcess>();
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
            mICreateFceSettings = new JMenuItem("Create Furnace Settings");
            mIReadFceSettings = new JMenuItem("Read Furnace Settings");
            mISaveFceSettings = new JMenuItem("Save Furnace Settings");

            mIAddDFHStripProcess = new JMenuItem("Add to StripDFHProcess List");
            mIReadDFHStripProcess = new JMenuItem("Read StripDFHProcess List from File");
            mISaveDFHStripProcess = new JMenuItem("Save StripDFHProcess List to File");

            mILevel1FieldResults = new JMenuItem("Take Results From Level1");
            mILoadFieldResult = new JMenuItem("Load Field Results from File");
            mISaveFieldResult = new JMenuItem("Save As Field Results");
            mISaveFieldResult.setEnabled(false);
            mIEvalForFieldProduction = new JMenuItem("Calculate for Field Production");
            mIEvalForFieldProduction.setEnabled(false);
            mIEvalWithFieldCorrection = new JMenuItem("Re-Calculate With Field Corrections");
            mIEvalWithFieldCorrection.setEnabled(false);

            StripProcMenuListener li = new StripProcMenuListener();
            mICreateFceSettings.addActionListener(li);
            mIReadFceSettings.addActionListener(li);
            mISaveFceSettings.addActionListener(li);
            mIAddDFHStripProcess.addActionListener(li);
            mISaveDFHStripProcess.addActionListener(li);
            mIReadDFHStripProcess.addActionListener(li);
            mILevel1FieldResults.addActionListener(li);
            mILoadFieldResult.addActionListener(li);
            mISaveFieldResult.addActionListener(li);
            mIEvalForFieldProduction.addActionListener(li);
            mIEvalWithFieldCorrection.addActionListener(li);

            mL2Configuration.add(mICreateFceSettings);
            mL2Configuration.add(mIReadFceSettings);
            mL2Configuration.add(mISaveFceSettings);
            mL2Configuration.addSeparator();
            mL2Configuration.add(mIAddDFHStripProcess);
            mL2Configuration.add(mIReadDFHStripProcess);
            mL2Configuration.add(mISaveDFHStripProcess);
            mL2Configuration.addSeparator();
            mL2Configuration.add(mILevel1FieldResults);
            mL2Configuration.add(mILoadFieldResult);
            mL2Configuration.add(mIEvalForFieldProduction);
            mL2Configuration.add(mIEvalWithFieldCorrection);
            mL2Configuration.addSeparator();
            mL2Configuration.add(mISaveFieldResult);
            mL2Configuration.setEnabled(true);


            mb.add(mL2Configuration);
            mb.updateUI();
            l2MenuSet = true;
            setFcefor(true);
        }
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
            else if (caller == mIAddDFHStripProcess)
                addStripDFHProcess();
            else if (caller == mICreateFceSettings)
                createFceSetting();
            else if (caller == mIReadFceSettings)
                readFurnaceSettings();
            else if (caller == mISaveFceSettings)
                saveFurnaceSettings();
            else if (caller == mISaveFieldResult)
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
                    mIAddDFHStripProcess.setVisible(true);
                    mICreateFceSettings.setVisible(true);
                    mIReadFceSettings.setVisible(true);
                    mISaveFceSettings.setVisible(true);
                    mISaveFieldResult.setVisible(true);
                    mILoadFieldResult.setVisible(true);
                    mIEvalForFieldProduction.setVisible(true);
                    mIEvalWithFieldCorrection.setVisible(true);
                } else {
                    mIReadDFHStripProcess.setVisible(false);
                    mISaveDFHStripProcess.setVisible(false);
                    mIAddDFHStripProcess.setVisible(false);
                    mICreateFceSettings.setVisible(false);
                    mIReadFceSettings.setVisible(false);
                    mISaveFceSettings.setVisible(false);
                    mISaveFieldResult.setVisible(false);
                    mILoadFieldResult.setVisible(false);
                    mIEvalForFieldProduction.setVisible(false);
                    mIEvalWithFieldCorrection.setVisible(false);
                }
            }
        }
      }


    void setStripProcessLookup() {
        stripProcessLookup.clear();
        stripProcessLookup.put("FH", new OneStripDFHProcess(this, "FH", "CR Lo-C emiss 0.32", "CR Lo-C emiss 0.34", 550, 0.00015));
        stripProcessLookup.put("CQ", new OneStripDFHProcess(this, "CQ", "CR Lo-C emiss 0.32", "CR Lo-C emiss 0.34", 620, 0.0002));
    }

    String stripDFHProcessListInXML() {
        StringBuffer xmlStr = new StringBuffer(XMLmv.putTag("exitTempAllowance", l2Furnace.getExitTempAllowance()));
        xmlStr.append(XMLmv.putTag("pNum", stripProcessLookup.size()));
        int pNum = 0;
        for (OneStripDFHProcess oneProc: stripProcessLookup.values())
            xmlStr.append(XMLmv.putTag("StripP" + ("" + ++pNum).trim(), oneProc.dataInXML().toString()) + "\n");
        return xmlStr.toString();
    }

    boolean takeStripProcessListFromXML(String xmlStr) {
        stripProcessLookup.clear();
        boolean retVal = false;
        ValAndPos vp;
        oneBlk:
        {
            vp = XMLmv.getTag(xmlStr, "exitTempAllowance", 0);
            if (vp.val.length() > 0 )
                l2Furnace.setExitTempAllowance(Double.valueOf(vp.val));
            else
                l2Furnace.setExitTempAllowance(5);
            vp = XMLmv.getTag(xmlStr, "pNum", 0);
            try {
                int pNum = Integer.valueOf(vp.val);
                for (int p = 0; p < pNum; p++) {
                    vp = XMLmv.getTag(xmlStr, "StripP" + ("" + (p + 1)).trim(), vp.endPos);
                    OneStripDFHProcess oneProc = new OneStripDFHProcess(this, vp.val);
                    if (oneProc.inError) {
                        showError("In reading StripDFHProc: \n" + oneProc.errMeg);
                        retVal = false;
                        break oneBlk;
                    }
                    else
                        stripProcessLookup.put(oneProc.processName, oneProc);
                }
                retVal = true;
            } catch (NumberFormatException e) {
                showError("Error in Number of StripDFHProc");
                break oneBlk;
            }
        }
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

    void addStripDFHProcess() {
        showMessage("Not ready for Adding to StripDFHProcess!");
    }

    void createFceSetting() {
        showMessage("Not ready for Creating Furnace Settings!");
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
            showMessage("Unfreezing Recu Specs");
            l2Furnace.newRecu();
            l2Furnace.setCurveSmoothening(false);
            calculateFce();
            return true;
        }
        else
            return false;
    }

    boolean recalculateWithFieldCorrections() {   // @TODO incomplete
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
        return stripProcessLookup.get(forProc);
    }

    public void resultsReady(Observations observations) {
        super.resultsReady(observations);
        l2Furnace.setCurveSmoothening(true);
        if (proc == DFHTuningParams.ForProcess.STRIP) {
            mISaveFieldResult.setEnabled(true);
        }
    }

    protected void enableResultsMenu(boolean enable) {
        super.enableResultsMenu(enable);
        if (l2MenuSet) {
            mISaveFieldResult.setEnabled(enable);
        }
    }

    public void destroy() {
        l2Furnace.prepareForDisconnection();
        if (uaClient != null)
            uaClient.disconnect();
        super.destroy();
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
