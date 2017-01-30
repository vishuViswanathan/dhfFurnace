package directFiredHeating;

import FceElements.RegenBurner;
import basic.*;
import directFiredHeating.billetWH.SampleWHFurnace;
import directFiredHeating.process.OneStripDFHProcess;
import directFiredHeating.process.StripDFHProcessList;
import jsp.*;
import mvUtils.jsp.*;
import mvUtils.display.*;
import mvUtils.jnlp.JNLPFileHandler;
import mvUtils.mvXML.DoubleWithErrStat;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLgroupStat;
import mvUtils.mvXML.XMLmv;
import mvUtils.math.XYArray;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import performance.stripFce.Performance;
import performance.stripFce.StripProcessAndSize;

import javax.jnlp.FileContents;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.PageRanges;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 5/9/12
 * Time: 10:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class DFHeating extends JApplet implements InputControl, EditListener {

    public enum CommandLineArgs {
        EXPERTMODE("-expertMode"),
        ALLOWCHANGES("-allowChanges"),
        ONTEST("-onTest"),
        ALLOWSPECSSAVE("-allowSpecsSave"),
        ALLOWSPECSREAD("-allowSpecsRead"),
        JNLP("-asJNLP"),
        DEBUGMSG("-showDebugMessages");
        private final String argName;

        CommandLineArgs(String argName) {
            this.argName = argName;
        }

        public String getValue() {
            return name();
        }

        @Override
        public String toString() {
            return argName;
        }

        public static CommandLineArgs getEnum(String text) {
            CommandLineArgs retVal = null;
            if (text != null) {
              for (CommandLineArgs b : CommandLineArgs.values()) {
                if (text.equalsIgnoreCase(b.argName)) {
                  retVal = b;
                    break;
                }
              }
            }
            return retVal;
          }
    }


    public enum HeatingMode {
        TOPBOTSTRIP("STRIP - TOP and BOTTOM"),
        TOPBOT("TOP AND BOTTOM FIRED"),
        TOPONLY("TOP FIRED");
        private final String modeName;

        HeatingMode(String modeName) {
            this.modeName = modeName;
        }

        public String getValue() {
            return name();
        }

        @Override
        public String toString() {
            return modeName;
        }

        public static HeatingMode getEnum(String text) {
            HeatingMode retVal = null;
            if (text != null) {
              for (HeatingMode b : HeatingMode.values()) {
                if (text.equalsIgnoreCase(b.modeName)) {
                  retVal = b;
                    break;
                }
              }
            }
            return retVal;
          }
    }
    
    static public boolean bAllowProfileChange = true;
    static public boolean bAllowManualCalculation = true;
    static public boolean bAllowEditPerformanceList = true;

    static public JFrame mainF; // = new JFrame("DFH Furnace");
    static public Vector<Fuel> fuelList = new Vector<Fuel>();
    static public Vector<ChMaterial> vChMaterial; // = new Vector<ChMaterial>();
    static protected boolean enableSpecsSave = false;
    public static Logger log;
    static boolean onTest = false;
    static public boolean showDebugMessages = false;
    static public boolean userActionAllowed = true;
    static public JSPConnection jspConnection;
//    static public boolean bAllowEditDFHProcess = false;
    static public boolean bL2Configurator = false;
    static public boolean bAtSite = false;
    protected String profileFileExtension = "dfhDat";
    protected String profileFileName = "FurnaceProfile." + profileFileExtension;
    protected long maxSizeOfProfileFile = (long)1e6;
    protected String fceDataLocation = "";
    protected FramedPanel mainAppPanel;
    protected String testTitle = "";
    boolean fceFor1stSwitch = true;
    public DFHFurnace furnace;
    protected String releaseDate = "JNLP 20170130";
    protected String DFHversion = "DFHeating Version 001";
    public DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    boolean canNotify = true;
    JSObject win;
    protected boolean itsON = false;
    JPanel mainFrame;
    String reference = "Reference", fceTtitle = "Furnace", customer = "Customer";
    protected double fceWidth = 10;
    double excessAir = 0.1;
    public HeatingMode heatingMode;
    boolean bTopBot = true;
    boolean bAddTopSoak = true;
    Fuel commFuel;
    String nlSpace = ErrorStatAndMsg.nlSpace;
    Hashtable<DFHResult.Type, ResultPanel> resultPanels, printPanels;
    public DFHTuningParams.FurnaceFor furnaceFor = DFHTuningParams.FurnaceFor.BILLETS;
    protected DFHTuningParams tuningParams;
    protected JTextField tfReference, tfFceTitle, tfCustomer;
    NumberTextField ntfWidth;
    protected JComboBox cbFceFor;
    protected JComboBox<HeatingMode> cbHeatingMode;
    protected JSPComboBox cbFuel;
    NumberTextField tfExcessAir;
    protected LossNameChangeListener lNameListener = new LossNameChangeListener();
    InputChangeListener inputChangeListener = new InputChangeListener();
    RegenBurner regenBurnerStudy;
    Locale locale;
    public boolean asApplication = false;
    public static boolean asJNLP = false;
    boolean bDataEntryON = true;
    protected JScrollPane slate = new JScrollPane();
    JPanel opPage;
    JPanel inpPage;
    boolean bAllowSecFuel = false;
    Component lastPageShown = null;
    JPanel fuelMixP;
    protected GridBagConstraints gbcChDatLoc;
    protected JMenu fileMenu;
    protected JMenu defineFurnaceMenu;
    protected JMenu resultsMenu;
    JMenu printMenu;
//    JMenu statMenu;
//    JMenuItem progressP;
    public JButton pbEdit;
    JMenuItem mIBeamParamTFM, mILossParamTFM;
    JMenu compareResults;
    JMenuItem mISaveComparisontoXL, mIAppendComparisontoXL;
    JMenuItem mIShowComparison;
    JMenuItem mISaveComparison;
    JMenuItem mIClearComparison;
    JMenuItem mISaveToXL;

    JMenuItem mISaveForTFM; //, saveForFE;

    protected JMenuItem mISaveFuelSpecs;
    protected JMenuItem mISaveSteelSpecs;

    Vector<PanelAndName> heatBalances, allTrends;
    FramedPanel lossPanel;
    GridBagConstraints gbcLoss;
    JScrollPane lossScroll;
    FramedPanel rowHead;
    JScrollPane detScroll;
    NumberLabel lbTopLen;
    NumberLabel lbBotLen;
    protected FramedPanel titleAndFceCommon;
    MultiPairColPanel mpTitlePanel;
    MultiPairColPanel mpFceCommDataPanel;
    protected double chWidth = 1.2, chThickness = 0.2, chLength = 9, chDiameter = 0.2;
    protected JComboBox cbChType;
    protected int nChargeRows = 1;
    protected ChMaterial selChMaterial;
    protected NumberTextField tfChWidth, tfChThickness, tfChLength, tfChDiameter;
    protected JSPComboBox cbChMaterial;
    MultiPairColPanel mpChargeData;
    protected JLabel labChLength;
    JLabel labChWidth;
    protected double bottShadow, chPitch = 1.3, tph = 100;
    protected double entryTemp = 30, exitTemp = 1200, deltaTemp = 25;
    protected double exitZoneFceTemp = 1050; // for strip heating
    protected double minExitZoneFceTemp = 900; // for strip heating
    protected String processName = "Reheating";
    protected JTextField tfProcessName;
    protected NumberTextField tfBottShadow, tfChPitch, tfChRows, tfProduction;
    protected NumberTextField tfEntryTemp, tfExitTemp, tfDeltaTemp;
    protected NumberTextField tfExitZoneFceTemp;
    protected NumberTextField tfMinExitZoneFceTemp;
    LengthChangeListener lengthListener = new LengthChangeListener();
    protected MultiPairColPanel mpChInFce;
    NumberTextField tfTotTime, tfSpTime, tfSpeed;
    public double calculStep = 1.0;
    public double ambTemp = 30, airTemp = 500, fuelTemp = 30;
    protected NumberTextField tfCalculStep, tfAmbTemp, tfAirTemp, tfFuelTemp;
    protected JButton pbCalculate;
    protected MultiPairColPanel mpCalcul;
    double deltaTflue = 50, deltaTAirFromRecu = 50, maxFlueAtRecu = 800;
    double deltaTFuelFromRecu = 30;
    NumberTextField ntDeltaTFlue;  // temp drop in flue between furnace and recu
    NumberTextField ntdeltaTAirFromRecu;   // temp drop in air between recu and burners
    NumberTextField ntDeltaTFuelFromRecu; // temp drop in Fuel between recu and burners
    NumberTextField ntMaxFlueTatRecu;  // for calculating dilution air
    MultiPairColPanel mpRecuData;
    JCheckBox cBAirHeatByRecu, cBFuelHeatByRecu;
    boolean bAirHeatedByRecu = true;
    boolean bFuelHeatedByRecu = false;
    JCheckBox cBAirAfterFuel;
    boolean bAirAfterFuel = false;
    ChMaterial material;
    Charge theCharge;
    XYArray hC, tk, emiss;
//    protected ProductionData production;
    FceEvaluator evaluator;
    static protected boolean onProductionLine = false;
    public boolean bProfileEdited = false;
    public StripDFHProcessList dfhProcessList;
    boolean freshResults = false;  // i.e results not saved to performance base

    public DFHeating() {
        debug("Release " + releaseDate);
        locale = Locale.getDefault(); // creates Locale class object by getting the default locale.
//        debug("Locale is " + locale);
    }

    public DFHeating(boolean asApplication, boolean onProductionLine) {
        this();
        this.asApplication = asApplication;
        DFHeating.onProductionLine = onProductionLine;
        debug("as Application");
        init();
    }

    public DFHeating(boolean asApplication) {
        this();
        this.asApplication = asApplication;
        debug("as Application");
        init();
    }

    protected void setUIDefaults() {
        UIManager.put("ComboBox.disabledForeground", Color.black);
        UIManager.put("Label.disabledForeground", Color.black);
        Font oldLabelFont = UIManager.getFont("Label.font");
        UIManager.put("Label.font", oldLabelFont.deriveFont(Font.PLAIN));
        oldLabelFont = UIManager.getFont("ComboBox.font");
        UIManager.put("ComboBox.font", oldLabelFont.deriveFont(Font.PLAIN + Font.ITALIC));
    }

    protected void startLog4j() {
        log = Logger.getLogger(DFHeating.class);
    }

    public void init() {
        modifyJTextEdit();
        fuelList = new Vector<Fuel>();
        vChMaterial = new Vector<ChMaterial>();
        setUIDefaults();
        String strTest;
        mainF = new JFrame();
        mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        if (!asApplication) {
            strTest = this.getParameter("OnTest");
            if (strTest != null)
                onTest = strTest.equalsIgnoreCase("YES");
            if (onTest)
                mainF.setTitle("DFH Furnace on Test " + releaseDate);
            else
                mainF.setTitle("DFH Furnace " + releaseDate);
        } else {
            if (!asJNLP && log == null) {
                startLog4j();
//                log = Logger.getLogger(DFHeating.class);
                // Load Log4j configurations from external file
            }
            mainF.setTitle("DFH Furnace Application "+ releaseDate + testTitle);
        }

        tuningParams = new DFHTuningParams(this, onProductionLine, 1, 5, 30, 1.12, 1, false, false);
        debug("Creating new DFHFurnace");
        furnace = new DFHFurnace(this, bTopBot, bAddTopSoak, lNameListener);
        debug("Created furnace");
        furnace.setTuningParams(tuningParams);
        debug("tuning params set");
        if (onTest || asApplication) {
            createUIs();
            loadFuelAndChMaterialData();
            setTestData();
            switchPage(DFHDisplayPageType.INPUTPAGE);
            displayIt();
        } else {
            try {
                win = JSObject.getWindow(this);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                win = null;
            }
            Object o;
            debug("got win");
            o = win.eval("enableSpecsSave()");
            enableSpecsSave = (o != null) && o.equals("1");
            debug("before creating UI");
            loadFuelAndChMaterialData();
            createUIs();
            debug("Created UI");
//            testFunctions();
            setTestData();
            debug("did setTestData()");
            o = win.eval("getData()");
            debug("got Data from aspx");
        }
        fuelMixP = Fuel.mixedFuelPanel(this, jspConnection, fuelList);
        regenBurnerStudy = new RegenBurner(fuelList, jspConnection, this);
        logInfo("DFHeating initiated");
        enableDataEdit();
    }

    String testingWarning = "  (IN TESTING MODE)";

    public String releaseDate() {
        return releaseDate;
    }
    public void itIsOnTest(boolean testing) {
        DFHeating.onTest = testing;
        String title = mainF.getTitle();
        title = title.replace(testingWarning, "");
        if (onTest)
            title = title + testingWarning;
        mainF.setTitle(title);
    }

    protected void createUIs() {
        createUIs(true); // with default MenuBar
    }

    protected void createUIs(boolean withDefaultMenuBar) {
         if (!itsON) {
             mainAppPanel = new FramedPanel(new BorderLayout());
             mainAppPanel.setPreferredSize(new Dimension(1000, 700));
             mainF.addWindowListener(new WinListener());
             createAllMenuItems();
             createAllUIItems();
             addMenuBar();
             inpPage = inputPage();
             opPage = OperationPage();
             slate.setViewportView(inpPage);
             mainAppPanel.add(slate, BorderLayout.CENTER);
             switchPage(DFHDisplayPageType.INPUTPAGE);
             cbHeatingMode.setSelectedItem(HeatingMode.TOPONLY);
             cbFceFor.setSelectedItem(furnaceFor);
             cbFuel.setSelectedItem(commFuel);
         }
    }


    protected void disableSomeUIs() {
    }

    protected void enableTitleEdit(boolean ena)  {
        titleAndFceCommon.setEnabled(ena);
    }

    void enableDataEntry(boolean ena) {
        ena &= bAllowManualCalculation;
        if (ena)
            furnace.resetSections();
//        if (titleAndFceCommon != null)  titleAndFceCommon.setEnabled(ena && bAllowProfileChange);
        enableTitleEdit(ena && bAllowProfileChange);

        cbChType.setEnabled((furnaceFor != DFHTuningParams.FurnaceFor.STRIP) && ena && bAllowProfileChange);
        tfChLength.setEditable(ena);
        tfChWidth.setEditable(ena);
        tfChDiameter.setEditable(ena);
        tfChThickness.setEditable(ena);
        tfBottShadow.setEditable(ena && bAllowProfileChange);
        tfChPitch.setEditable(ena && bAllowProfileChange);
        tfChRows.setEditable(ena && bAllowProfileChange);
        tfProduction.setEditable(ena);
        tfEntryTemp.setEditable(ena);
        tfExitTemp.setEditable(ena);
        tfDeltaTemp.setEditable(ena);
        tfMinExitZoneFceTemp.setEditable(ena);
        tfExitZoneFceTemp.setEditable(ena);
        tfAmbTemp.setEditable(ena  && bAllowProfileChange);
        tfAirTemp.setEditable(ena);
        tfCalculStep.setEditable(ena && bAllowProfileChange);
        cbChMaterial.setEnabled(ena);

        tfFuelTemp.setEditable(ena);

        ntdeltaTAirFromRecu.setEditable(ena && bAllowProfileChange);
        ntDeltaTFlue.setEditable(ena && bAllowProfileChange);
        ntMaxFlueTatRecu.setEditable(ena && bAllowProfileChange);
        ntDeltaTFuelFromRecu.setEditable(ena && bAllowProfileChange);
        cBAirHeatByRecu.setEnabled(ena && bAllowProfileChange);
        cBFuelHeatByRecu.setEnabled(ena && !(commFuel != null && commFuel.bMixedFuel) && bAllowProfileChange);

        if (ena && cBAirHeatByRecu.isSelected() && cBFuelHeatByRecu.isSelected())
            cBAirAfterFuel.setEnabled(ena && bAllowProfileChange);
        else
            cBAirAfterFuel.setEnabled(false && bAllowProfileChange);
        if (ena)
            setTimeValues(0, 0, 0);
        furnace.enableDataEntry(ena && bAllowProfileChange);

        pbCalculate.setEnabled(ena);
        bDataEntryON = ena;
        if (ena) {
            mISaveForTFM.setEnabled(false && !onProductionLine);
            mISaveToXL.setEnabled(false);
        }
        tuningParams.enableDataEntry(ena && bAllowProfileChange);
        userTunePanel.setEnabled(ena && !onProductionLine);
        disableSomeUIs();
    }

    protected void setTestData() {
        StatusWithMessage status = takeProfileDataFromXML(SampleWHFurnace.xmlStr);
        if (status.getDataStatus() == DataStat.Status.WithErrorMsg)
            showError(status.getErrorMessage());
        setDefaultSelections();
        adjustForLengthChange();
        furnace.clearAssociatedData();
    }

    protected void loadFuelAndChMaterialData() {
        if (asApplication) {
            if (asJNLP) {
                if (jspConnection.allOK) {
                    Vector<JSPFuel> fuelListJNLP = JSPFuel.getFuelList(jspConnection);
                    for (JSPFuel fuel : fuelListJNLP)
                        fuelList.add(fuel);
                    Vector<JSPchMaterial> metalListJNLP = JSPchMaterial.getMetalList(jspConnection);
                    for (JSPchMaterial mat : metalListJNLP)
                        vChMaterial.add(mat);
                }
            }
            else {
                String fuelSpecsFile = "defData\\FuelSpecifications.dfhSpecs";
                if (!fuelSpecsFromFile(fuelSpecsFile))
                    showError("Fuel Specification file :" + fuelSpecsFile + " is not available");
                String matSpecFile = "defData\\ChMaterialSpecifications.dfhSpecs";
                if (!chMaterialSpecsFromFile(matSpecFile))
                    showError("Charge Material Specification file :" + matSpecFile + " is not available");
            }
        }

        if (onTest || asApplication) {
            if (fuelList.size() == 0) {
                addFuel("Pseudo Nat Gas #8500 [8,502 kcal/m3N]", "Nm3", "" + 8503, "" +
                                9.47, "" + 10.47, "0, 0, 100, 39.47, 200, 84.82, 900, 553.347, 2000, 1583.07",
                        "" + 0.0951, "" + 0.1899, "" + 0,
                        "" + 0.7149, "" + 0);
            }
            if (vChMaterial.size() == 0) {
                XYArray emiss = new XYArray("0, 0.2, 400, 0.28, 500, 0.3, 980, 0.7, 1300, 0.879, 2000, 0.88");
                XYArray tk = new XYArray("0, 12.96, 50, 13.68, 743,22.6,900, 23, 2000, 26");
                XYArray hc = new XYArray("0, 0, 75, 9.15,125,15.25,175, 21.55, 225, 27.9, 325, 40.9, 425, 54.4, 475, 61.35, 525, 68.45, 575, 75.95, " +
                        "625, 83.7, 675, 91.25, 725, 98.7, 775, 106.2, 825, 113.85, 875, 121.6, 1275, 184.55, 2000, 302.725");
                ChMaterial mat = new ChMaterial("Psuedo SS 316", "5006", 7850, tk, hc, emiss);
                vChMaterial.add(mat);
            }
        }
    }

    // for Applet version
    public String addFuelChoice(String name, String units, String calValStr, String airFuelRatioStr, String flueFuelRatioStr,
                                String sensHeatPair,
                                String percCO2str, String percH2Ostr, String percN2str, String percO2str, String percSO2str) {
        double calVal, airFuelRatio, flueFuelRatio;
        double percCO2, percH2O, percN2, percO2, percSO2;
        XYArray sensHeat = null;
        if (sensHeatPair.trim().length() > 0)
            sensHeat = new XYArray(sensHeatPair);
        try {
            calVal = Double.valueOf(calValStr);
            airFuelRatio = Double.valueOf(airFuelRatioStr);
            flueFuelRatio = Double.valueOf(flueFuelRatioStr);
            percCO2 = Double.valueOf(percCO2str);
            percH2O = Double.valueOf(percH2Ostr);
            percN2 = Double.valueOf(percN2str);
            percO2 = Double.valueOf(percO2str);
            percSO2 = Double.valueOf(percSO2str);
        } catch (NumberFormatException e) {
            return ("ERROR: Number format in addFuelChoice - " + e.getMessage());
        }
        FlueComposition flueComp = null;
        try {
            flueComp = new FlueComposition("Flue of " + name, percCO2 / 100, percH2O / 100, percN2 / 100, percO2 / 100, percSO2 / 100);
        } catch (Exception e) {
            return ("ERROR:" + e.getMessage());
        }
        Fuel fuel = new Fuel(name, units, calVal, airFuelRatio, flueFuelRatio, sensHeat, flueComp);
        if (commFuel == null) commFuel = fuel;
        fuelList.add(fuel);
        return "OK";
    }

    public String addLossType(String lossNumStr, String lossName, String factorStr, String basisStr, String tempActStr) {
        String retVal = "OK";
        double factor;
        int lossNum;
        LossType.LossBasis basis;
        LossType.TempAction tempAct;

        try {
            factor = Double.valueOf(factorStr);
            lossNum = Integer.valueOf(lossNumStr);
        } catch (NumberFormatException e) {
            return ("ERROR: in number format for loss Factor!");
        }

        try {
            basis = LossType.LossBasis.valueOf(basisStr);
            tempAct = LossType.TempAction.valueOf(tempActStr);
        } catch (IllegalArgumentException e) {
            return ("ERROR: in Loss Basis or TempAction!");
        }
        furnace.changeLossItemVal(lossNum, lossName, factor, basis, tempAct);

        return retVal;
    }

    public void displayIt() {
        if (!itsON /* && furnace != null */) {
            itsON = true;
            mainF.add(mainAppPanel);
            mainF.setFocusable(true);
            mainF.requestFocus();
            mainF.toFront(); //setAlwaysOnTop(true);
            mainF.pack();
            mainF.setVisible(true);
            mainF.setResizable(false);
        }
    }

    protected void setDefaultSelections() {
        if (cbFuel.getItemCount() == 1)
            cbFuel.setSelectedIndex(0);
        if (cbChMaterial.getItemCount() == 1)
            cbChMaterial.setSelectedIndex(0);
    }

    protected void setChMaterial(ChMaterial material) {
        selChMaterial = material;
        cbChMaterial.setSelectedItem(selChMaterial);
//        selChMaterial = (ChMaterial)cbChMaterial.getSelectedItem(); // to make it collect all data on JNLP if required
    }

    protected void setEntryTemperature(double temperature) {
        entryTemp = temperature;
        tfEntryTemp.setData(entryTemp);
    }

    protected void setExitTemperature(double temperature) {
        exitTemp = temperature;
        tfExitTemp.setData(exitTemp);
    }

    void setAllowSecFuel(boolean allow) {
        bAllowSecFuel = allow;
        if (!(furnace == null))
            furnace.setAllowSecFuel(allow);
    }

    void allowManualTempForLosses(boolean allow) {
        furnace.allowManualTempForLosses(allow);
    }

    DFHDisplayPageType lastDisplayPage = null;

    protected void updateDisplay(DFHDisplayPageType page) {
        if (lastDisplayPage == page)
            switchPage(page);
    }

    protected void switchPage(DFHDisplayPageType page) {
        boolean done = true;
        switch (page) {
            case INPUTPAGE:
                addFceCommAndDataPanel(inpPage);
                slate.setViewportView(inpPage);
                break;
            case OPPAGE:
                addFceCommAndDataPanel(opPage);
                slate.setViewportView(opPage);
                break;
            case PROGRESSPAGE:
                evaluator.showProgress();
                break;
            case TUNINGPAGE:
                slate.setViewportView(tuningParams.getTuningPanel());
                break;
            case BEAMSPAGE:
                if (furnace.fceProfTFM != null)
                    slate.setViewportView(furnace.fceProfTFM.beamsDataPanel());
                break;
            case LOSSPARAMSTFM:
                if (furnace.fceProfTFM != null)
                    slate.setViewportView(furnace.fceProfTFM.lossPramsPanel());
                break;
            case FUELMIX:
//                fuelMixP = Fuel.mixedFuelPanel(this, fuelList);
                slate.setViewportView(fuelMixP);
                break;
            case REGENSTUDY:
                JPanel p = regenBurnerStudy.regenPanel();
                slate.setViewportView(p);
                break;
            case PERFOMANCELIST:
                slate.setViewportView(furnace.getPerfBaseListPanel());
                break;
            case COMPAREPANEL:
                slate.setViewportView(furnace.getComparePanel());
                break;
            default:
                done = false;
                break;
        }
        lastDisplayPage = (done) ? page : null;
    }

    void switchPage(Component c) {
        slate.setViewportView(c);
        lastDisplayPage = null;
    }

    JPanel inputPage() {
        mainFrame = new JPanel(new GridBagLayout());
        GridBagConstraints gbcMf = new GridBagConstraints();

        gbcMf.anchor = GridBagConstraints.CENTER;
        gbcMf.gridx = 0;
        gbcMf.gridy = 0;
        gbcMf.insets = new Insets(0, 0, 0, 0);
        gbcMf.gridwidth = 1;
        gbcMf.gridheight = 1;
        gbcMf.gridy++;
        mainFrame.add(lossTablePanel(), gbcMf);
        gbcMf.gridx = 0;
        gbcMf.gridy++;

        FramedPanel detPan = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbcDP = new GridBagConstraints();
        gbcDP.gridx = 0;
        gbcDP.gridy = 0;
        detPan.add(getRowHeader(), gbcDP);
        gbcDP.gridx++;
        detPan.add(secDetailsPanel(), gbcDP);
        mainFrame.add(detPan, gbcMf);
        gbcMf.gridx = 0;
        gbcMf.gridy++;
        adjustForLossNameChange();
        return mainFrame;
    }

    public void setHeatingMode(String mode) {
        cbHeatingMode.setSelectedItem(HeatingMode.getEnum(mode));
    }

    void enableParamsForTopBot() {
        tfBottShadow.setEnabled(bTopBot);
    }

    class HeatingModeListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            heatingMode = (HeatingMode)cbHeatingMode.getSelectedItem();
            bAddTopSoak = false;
            switch (heatingMode) {
                case TOPONLY:
                    bTopBot = false;
                    changeTopBot(false);
                    break;
                case TOPBOT:
                    bTopBot = true;
                    break;
                case TOPBOTSTRIP:
                    bTopBot = false;
                    changeTopBot(false);
                    if (cbFceFor.getSelectedItem() != DFHTuningParams.FurnaceFor.STRIP) {
                        cbFceFor.setSelectedItem(DFHTuningParams.FurnaceFor.STRIP);
                    }
                    break;
            }
            furnace.changeFiringMode(bTopBot, bAddTopSoak);
            noteFiringModeChange(bTopBot);
            enableParamsForTopBot();
        }
    }

    MultiPairColPanel userTunePanel;

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

        jp.add(processAndCharge(), gbcOP);
        gbcChDatLoc = new GridBagConstraints();
        gbcChDatLoc.gridx = gbcOP.gridx;
        gbcChDatLoc.gridy = gbcOP.gridy;
        gbcChDatLoc.anchor = gbcOP.anchor;

        gbcOP.gridx++;
        gbcOP.anchor = GridBagConstraints.WEST;
//        jp.add(chargeInFurnacePanel(), gbcOP);
        mpChInFce = chargeInFurnacePanel();
        jp.add(mpChInFce, gbcOP);

        gbcOP.gridx++;
//        jp.add(recuDataPanel(), gbcOP);
        mpRecuData = recuDataPanel();
        jp.add(mpRecuData, gbcOP);
        gbcOP.gridy++;
        gbcOP.gridx = 0;
        userTunePanel = tuningParams.userTunePan();
        userTunePanel.setEnabled(!onProductionLine);
        jp.add(userTunePanel, gbcOP);
        gbcOP.gridx++;
        gbcOP.gridx++;
//        jp.add(calCulDataPanel(), gbcOP);
        mpCalcul = calCulDataPanel();
        jp.add(mpCalcul, gbcOP);
        gbcOP.gridx = 0;
        return jp;
    }

    protected void addFceCommAndDataPanel(JPanel jp) {
        GridBagConstraints gbcOP = new GridBagConstraints();
        gbcOP.gridx = 0;
        gbcOP.gridy = 0;
        gbcOP.insets = new Insets(0, 0, 0, 0);
        gbcOP.gridwidth = 3;
        jp.add(titleAndFceCommon(), gbcOP);
    }

    protected JMenuItem mIGetFceProfile;
    protected JMenuItem mILoadRecuSpecs;
    protected JMenuItem mISaveFceProfile;
    protected JMenuItem mIExit;
    protected JMenuItem mIInputData;
    protected JMenuItem mIOpData;
    protected JMenuItem mIDefineRecuperator;
    protected JMenuItem mIRecuPerformace;
    JMenuItem mICreateFuelMix;
    JMenuItem mIRegenBurnerStudy;
    protected JMenuItem mITuningParams;

    protected void createAllMenuItems() {
        MenuActions mAction = new MenuActions();
        // File Menu Items
        mIGetFceProfile = new JMenuItem("Load Furnace");
        mIGetFceProfile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));
        mIGetFceProfile.addActionListener(mAction);

        mILoadRecuSpecs = new JMenuItem("Load Recuperator Specs.");
        mILoadRecuSpecs.addActionListener(mAction);
        mILoadRecuSpecs.setEnabled(true);

        mISaveFceProfile = new JMenuItem("Save Furnace");
        mISaveFceProfile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        mISaveFceProfile.addActionListener(mAction);

        mISaveToXL = new JMenuItem("Save Results and Furnace Data to Excel");
        mISaveToXL.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
        mISaveToXL.addActionListener(mAction);
        mISaveToXL.setEnabled(false);

        mISaveForTFM = new JMenuItem("Save Temperature Profile for TFM");
        mISaveForTFM.addActionListener(mAction);
        mISaveForTFM.setEnabled(false);

        mISaveFuelSpecs = new JMenuItem("Save Fuel Specifications to File");
        mISaveFuelSpecs.addActionListener(mAction);
        mISaveFuelSpecs.setEnabled(true);

        mISaveSteelSpecs = new JMenuItem("Save Steel Specifications to File");
        mISaveSteelSpecs.addActionListener(mAction);
        mISaveSteelSpecs.setEnabled(true);

        mIExit = new JMenuItem("Exit");
        mIExit.addActionListener(mAction);

        // input Menu Items
        mIInputData = new JMenuItem("Profile Data");
        mIInputData.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK));
        mIInputData.addActionListener(mAction);

        mIOpData = new JMenuItem("Operation Data");
        mIOpData.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        mIOpData.addActionListener(mAction);

        mIDefineRecuperator = new JMenuItem("Define Common Air Recuperator");
        mIDefineRecuperator.addActionListener(mAction);

        mIRecuPerformace = new JMenuItem("Evauate Air Recuperator Performance");
        mIRecuPerformace.addActionListener(mAction);

        mICreateFuelMix = new JMenuItem("Create Fuel Mix");
        mICreateFuelMix.addActionListener(mAction);

        mIRegenBurnerStudy = new JMenuItem("Regen Burner Study");
        mIRegenBurnerStudy.addActionListener(mAction);

        mITuningParams = new JMenuItem("Tuning Parameters");
        mITuningParams.addActionListener(mAction);

        mIBeamParamTFM = new JMenuItem("Walking Beam Params from TFM");
        mIBeamParamTFM.addActionListener(mAction);
        mIBeamParamTFM.setEnabled(false);

        mILossParamTFM = new JMenuItem("Loss Params from TFM");
        mILossParamTFM.addActionListener(mAction);
        mILossParamTFM.setEnabled(false);

        // Allow Edit
        pbEdit = new JButton("AllowDataEdit");
        pbEdit.setMnemonic(KeyEvent.VK_E);
        pbEdit.getModel().setPressed(true);
        pbEdit.setEnabled(false);
        pbEdit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableDataEdit();
            }
        });

        // Compare Menu Items
        CompareMenuListener compareMenuListener = new CompareMenuListener();
        mISaveComparison = new JMenuItem("Save results to Comparison Table");
        mISaveComparison.setEnabled(false);
        mISaveComparison.addActionListener(compareMenuListener);
        mIShowComparison = new JMenuItem("Show Comparison Table");
        mIShowComparison.setEnabled(false);
        mIShowComparison.addActionListener(compareMenuListener);
        mISaveComparisontoXL = new JMenuItem("Save Results Comparison Table to A New Excel file");
        mISaveComparisontoXL.addActionListener(compareMenuListener);
        mISaveComparisontoXL.setEnabled(false);
        mIAppendComparisontoXL = new JMenuItem("Append Results to Comparison Table in Excel");
        mIAppendComparisontoXL.addActionListener(compareMenuListener);
        mIAppendComparisontoXL.setEnabled(false);
        mIClearComparison = new JMenuItem("Clear Comparison Table");
        mIClearComparison.addActionListener(compareMenuListener);
        mIClearComparison.setEnabled(false);

        // Perfromance menu Items
        PerformListener performListener = new PerformListener();
        mICreatePerfBase = new JMenuItem("Create Performance Base");
        mICreatePerfBase.addActionListener(performListener);
        mIAddToPerfBase = new JMenuItem("Add to Performance Base");
        mIAddToPerfBase.setEnabled(false);
        mIAddToPerfBase.addActionListener(performListener);
        mIShowPerfBase = new JMenuItem("Show Performance Base List");
        mIShowPerfBase.setEnabled(false);
        mIShowPerfBase.addActionListener(performListener);

        mIClearPerfBase = new JMenuItem("Clear Performance Base");
        mIClearComparison.setEnabled(false);
        mIClearPerfBase.addActionListener(performListener);
        mISetPerfTablelimits = new JMenuItem("Set Limits for Performance Table");
        mISetPerfTablelimits.addActionListener(performListener);
        mISetPerfTablelimits.setVisible(false);

        // Results MenuItems
        ResultsMenuActions resultsMenuActions = new ResultsMenuActions();
        resultPanels = new Hashtable<DFHResult.Type, ResultPanel>();

        resultPanels.put(DFHResult.Type.HEATSUMMARY, new ResultPanel(DFHResult.Type.HEATSUMMARY, resultsMenuActions));
        resultPanels.put(DFHResult.Type.SECTIONWISE, new ResultPanel(DFHResult.Type.SECTIONWISE , resultsMenuActions));
        resultPanels.put(DFHResult.Type.TOPSECTIONWISE, new ResultPanel(DFHResult.Type.TOPSECTIONWISE, resultsMenuActions));
        resultPanels.put(DFHResult.Type.BOTSECTIONWISE, new ResultPanel(DFHResult.Type.BOTSECTIONWISE, resultsMenuActions));
        resultPanels.put(DFHResult.Type.RECUBALANCE, new ResultPanel(DFHResult.Type.RECUBALANCE, resultsMenuActions));
        resultPanels.put(DFHResult.Type.LOSSDETAILS, new ResultPanel(DFHResult.Type.LOSSDETAILS, resultsMenuActions));
        resultPanels.put(DFHResult.Type.FUELSUMMARY, new ResultPanel(DFHResult.Type.FUELSUMMARY, resultsMenuActions));
        resultPanels.put(DFHResult.Type.FUELS, new ResultPanel(DFHResult.Type.FUELS, resultsMenuActions));
        resultPanels.put(DFHResult.Type.TOPFUELS, new ResultPanel(DFHResult.Type.TOPFUELS, resultsMenuActions));
        resultPanels.put(DFHResult.Type.BOTFUELS, new ResultPanel(DFHResult.Type.BOTFUELS, resultsMenuActions));
        resultPanels.put(DFHResult.Type.TEMPRESULTS, new ResultPanel(DFHResult.Type.TEMPRESULTS, resultsMenuActions));
        resultPanels.put(DFHResult.Type.TOPtempRESULTS, new ResultPanel(DFHResult.Type.TOPtempRESULTS, resultsMenuActions));
        resultPanels.put(DFHResult.Type.BOTtempRESULTS, new ResultPanel(DFHResult.Type.BOTtempRESULTS, resultsMenuActions));
        resultPanels.put(DFHResult.Type.COMBItempTRENDS, new ResultPanel(DFHResult.Type.COMBItempTRENDS, resultsMenuActions));
        resultPanels.put(DFHResult.Type.TOPtempTRENDS, new ResultPanel(DFHResult.Type.TOPtempTRENDS, resultsMenuActions));
        resultPanels.put(DFHResult.Type.BOTtempTRENDS, new ResultPanel(DFHResult.Type.BOTtempTRENDS, resultsMenuActions));

        // print Menu Items
        PrintMenuActions printMenuActions = new PrintMenuActions();
        printPanels = new Hashtable<DFHResult.Type, ResultPanel>();

        printPanels.put(DFHResult.Type.HEATSUMMARY, new ResultPanel(DFHResult.Type.HEATSUMMARY, printMenuActions));
        printPanels.put(DFHResult.Type.SECTIONWISE, new ResultPanel(DFHResult.Type.SECTIONWISE, printMenuActions));
        printPanels.put(DFHResult.Type.TOPSECTIONWISE, new ResultPanel(DFHResult.Type.TOPSECTIONWISE, printMenuActions));
        printPanels.put(DFHResult.Type.BOTSECTIONWISE, new ResultPanel(DFHResult.Type.BOTSECTIONWISE, printMenuActions));
        printPanels.put(DFHResult.Type.ALLBALANCES, new ResultPanel(DFHResult.Type.ALLBALANCES, printMenuActions));
        printPanels.put(DFHResult.Type.RECUBALANCE, new ResultPanel(DFHResult.Type.RECUBALANCE, printMenuActions));
        printPanels.put(DFHResult.Type.FUELSUMMARY, new ResultPanel(DFHResult.Type.FUELSUMMARY, printMenuActions));
        printPanels.put(DFHResult.Type.FUELS, new ResultPanel(DFHResult.Type.FUELS, printMenuActions));
        printPanels.put(DFHResult.Type.TOPFUELS, new ResultPanel(DFHResult.Type.TOPFUELS, printMenuActions));
        printPanels.put(DFHResult.Type.BOTFUELS, new ResultPanel(DFHResult.Type.BOTFUELS, printMenuActions));
        printPanels.put(DFHResult.Type.COMBItempTRENDS, new ResultPanel(DFHResult.Type.COMBItempTRENDS, printMenuActions));
        printPanels.put(DFHResult.Type.TOPtempTRENDS, new ResultPanel(DFHResult.Type.TOPtempTRENDS, printMenuActions));
        printPanels.put(DFHResult.Type.BOTtempTRENDS, new ResultPanel(DFHResult.Type.BOTtempTRENDS, printMenuActions));
        printPanels.put(DFHResult.Type.ALLtempTRENDS, new ResultPanel(DFHResult.Type.ALLtempTRENDS, printMenuActions));
        printPanels.put(DFHResult.Type.FUELMIX, new ResultPanel(DFHResult.Type.FUELMIX, printMenuActions));
    }

    void createAllUIItems() {
        tfReference = new XLTextField(reference, 40);
        tfFceTitle = new XLTextField(fceTtitle, 40);
        tfCustomer = new XLTextField(customer, 40);
        tfProcessName = new XLTextField(processName, 10);
        cbFceFor = new XLComboBox(DFHTuningParams.FurnaceFor.values());
        cbFceFor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setFcefor(!fceFor1stSwitch);
                if (!fceFor1stSwitch)
                    showMessage("You have switched 'Furnace For'\nRecheck data for charge width, charge pitch if applicable ");
                fceFor1stSwitch = false;
            }
        });
        cbFceFor.setPreferredSize(new Dimension(200, 20));

        cbHeatingMode = new XLComboBox(HeatingMode.values());
        cbHeatingMode.addActionListener(new HeatingModeListener());
        cbHeatingMode.setPreferredSize(new Dimension(200, 20));
//        cbHeatingMode.setSelectedItem(HeatingMode.TOPONLY);
        ntfWidth = new NumberTextField(this, fceWidth * 1000, 10, false, 500, 40000, "#,###", "Furnace Width (mm) ");
        ntfWidth.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fceWidth = ntfWidth.getData() / 1000;
                furnace.setFceWidth(fceWidth);
            }
        });
        cbFuel = new JSPComboBox<Fuel>(jspConnection, fuelList);
        cbFuel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Fuel f = (Fuel) cbFuel.getSelectedItem();
                if (f != null && f.bMixedFuel) {
                    tfFuelTemp.setEnabled(false);
                    if (cBFuelHeatByRecu.isSelected())
                        cBFuelHeatByRecu.doClick();
                    cBFuelHeatByRecu.setEnabled(false);
                } else {
                    tfFuelTemp.setEnabled(true);
                    cBFuelHeatByRecu.setEnabled(true);
                }
            }
        });
//        cbFuel.setSelectedItem(commFuel);
        cbFuel.setRenderer(new BasicComboBoxRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                    if (-1 < index) {
                        Fuel f = (Fuel) value;
                        list.setToolTipText(value.toString() + " [" + f.calVal + "kcal/" + f.units + "]");
                    }
                } else {
                    setForeground(list.getForeground());
                    setBackground(list.getBackground());
                }

                setFont(list.getFont());
                setText((value == null) ? "" : value.toString());
                setText((value == null) ? "" : value.toString());
                return this;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        cbFuel.setPreferredSize(new Dimension(200, 20));
        tfExcessAir = new NumberTextField(this, excessAir * 100, 5, false, 0, 100, "###", "Excess Air (%) ");

        cbChType = new XLComboBox(Charge.ChType.values());
        cbChType.setSelectedItem(Charge.ChType.SOLID_RECTANGLE);
        addInputToListener(cbChType);
        tfChDiameter = new NumberTextField(this, chDiameter * 1000, 5, false, 10, 2000, "#,###", "Dia of cross section (mm)");
        addInputToListener(tfChDiameter);
        tfChWidth = new NumberTextField(this, chWidth * 1000, 5, false, 50, 25000, "#,###", "Width (Along Furnace) (mm)");
        addInputToListener(tfChWidth);
        labChWidth = new JLabel("Billet/Slab Width (mm)");
        tfChThickness = new NumberTextField(this, chThickness * 1000, 5, false, 0.05, 10000, "#,###.###", "Thickness (mm)");
        addInputToListener(tfChThickness);
        tfChLength = new NumberTextField(this, chLength * 1000, 5, false, 500, 25000, "#,###", "Length (Across Furnace) (mm)");
        addInputToListener(tfChLength);
        labChLength = new JLabel("Billet/Slab Length (mm)");
        cbChMaterial = new JSPComboBox<ChMaterial>(jspConnection, vChMaterial);
        cbChMaterial.setPreferredSize(new Dimension(200, 18));
        cbChMaterial.setSelectedItem(selChMaterial);
        addInputToListener(cbChMaterial);
        setChargeSizeChoice();

        tfBottShadow = new NumberTextField(this, bottShadow * 100, 5, false, 0, 100, "###", "Shadow on Bottom Surface (%)");
        tfChPitch = new NumberTextField(this, chPitch * 1000, 5, false, 0, 10000, "#,###", "Charge Pitch (mm)");
        tfChRows = new NumberTextField(this, nChargeRows, 5, false, 1, 5, "#,###", "Charge Rows");
        tfProduction = new NumberTextField(this, tph, 5, false, 0, 500, "#,###.00", "Production (t/h)");
        tfEntryTemp = new NumberTextField(this, entryTemp, 5, false, 0, 1500, "#,###", "Charge Entry Temperature (C)");
        tfExitTemp = new NumberTextField(this, exitTemp, 5, false, 0, 1500, "#,###", "Charge Exit Temperature (C)");
        tfDeltaTemp = new NumberTextField(this, deltaTemp, 5, false, 0.001, 500, "#,###.000", "Temperature Dis-uniformity (C)");
        tfExitZoneFceTemp = new NumberTextField(this, exitZoneFceTemp, 5, false, 500, 1400, "#,###", "Exit zone Furnace Temperature (C)");
        tfMinExitZoneFceTemp = new NumberTextField(this, minExitZoneFceTemp, 5, false, 500, 1400, "#,###", "Minimum Exit zone Furnace Temperature (C)");
        tfTotTime = new NumberTextField(this, 0, 5, false, 0, 500, "##0.000", "Total Heating time (h)");
        tfTotTime.setEnabled(false);
        tfSpTime = new NumberTextField(this, 0, 5, false, 0, 500, "##0.000", "Specific Heating time (min/mm)");
        tfSpTime.setEnabled(false);
        tfSpeed = new NumberTextField(this, 0, 5, false, 0, 500, "#,##0.000", "Charge Speed (m/min)");
        tfSpeed.setEnabled(false);
        setTimeValues(0, 0, 0);

        tfAmbTemp = new NumberTextField(this, ambTemp, 5, false, 0, 500, "#,###", "Ambient Temperature (C)");
        tfAirTemp = new NumberTextField(this, airTemp, 5, false, 0, 3000, "#,###", "Air Preheat (C)");
        tfFuelTemp = new NumberTextField(this, fuelTemp, 5, false, 0, 3000, "#,###", "Fuel Preheat (C)");
        tfCalculStep = new NumberTextField(this, calculStep * 1000, 5, false, 200, 5000, "#,###", "Calculation Step (mm)");
        pbCalculate = new JButton("Calculate");
        pbCalculate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                calculateFce();
            }
        });

        ntDeltaTFlue = new NumberTextField(this, deltaTflue, 5, false, 0, 100, "#,###", "Flue DeltaT Fce-Recu (C)");
        ntMaxFlueTatRecu = new NumberTextField(this, maxFlueAtRecu, 5, false, 0, 1200, "#,###", "Max. Flue temp at 1st Recu (C)");
        ntdeltaTAirFromRecu = new NumberTextField(this, deltaTAirFromRecu, 5, false, 0, 100, "#,###", "Air DeltaT Recu-burner (C)");
        ntdeltaTAirFromRecu.setEnabled(bAirHeatedByRecu);
        ntDeltaTFuelFromRecu = new NumberTextField(this, deltaTFuelFromRecu, 5, false, 0, 100, "#,###", "Fuel DeltaT Recu-burner (C)");
        ntDeltaTFuelFromRecu.setEnabled(bFuelHeatedByRecu);
        cBAirHeatByRecu = new JCheckBox();
        cBAirHeatByRecu.setSelected(bAirHeatedByRecu);
        cBAirAfterFuel = new JCheckBox();
        cBAirAfterFuel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bAirAfterFuel = cBAirAfterFuel.isSelected();
            }
        });
        cBAirAfterFuel.setEnabled(false);
        cBAirHeatByRecu.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                bAirHeatedByRecu = cBAirHeatByRecu.isSelected();
                ntdeltaTAirFromRecu.setEnabled(bAirHeatedByRecu);
                enableRecuSequence();
            }
        });
        cBFuelHeatByRecu = new JCheckBox();
        cBFuelHeatByRecu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bFuelHeatedByRecu = cBFuelHeatByRecu.isSelected();
                ntDeltaTFuelFromRecu.setEnabled(bFuelHeatedByRecu);
                enableRecuSequence();
            }
        });
    }

    protected void enablePrintResultsMenu(boolean ena) {
    }

    protected JMenu createPrintResultsMenu() {
        printMenu = new JMenu("Print Results");
        printMenu.add(printPanels.get(DFHResult.Type.HEATSUMMARY).getMenuItem());
        printMenu.add(printPanels.get(DFHResult.Type.SECTIONWISE).getMenuItem());
        printMenu.add(printPanels.get(DFHResult.Type.TOPSECTIONWISE).getMenuItem());
        printMenu.add(printPanels.get(DFHResult.Type.BOTSECTIONWISE).getMenuItem());
        printMenu.add(printPanels.get(DFHResult.Type.ALLBALANCES).getMenuItem());
        printMenu.addSeparator();
        printMenu.add(printPanels.get(DFHResult.Type.RECUBALANCE).getMenuItem());
        printMenu.addSeparator();
        printMenu.add(printPanels.get(DFHResult.Type.FUELSUMMARY).getMenuItem());
        printMenu.add(printPanels.get(DFHResult.Type.FUELS).getMenuItem());
        printMenu.add(printPanels.get(DFHResult.Type.TOPFUELS).getMenuItem());
        printMenu.add(printPanels.get(DFHResult.Type.BOTFUELS).getMenuItem());
        printMenu.addSeparator();
        printMenu.add(printPanels.get(DFHResult.Type.COMBItempTRENDS).getMenuItem());
        printMenu.add(printPanels.get(DFHResult.Type.TOPtempTRENDS).getMenuItem());
        printMenu.add(printPanels.get(DFHResult.Type.BOTtempTRENDS).getMenuItem());
        printMenu.add(printPanels.get(DFHResult.Type.ALLtempTRENDS).getMenuItem());
        printMenu.addSeparator();
        printMenu.add(printPanels.get(DFHResult.Type.FUELMIX).getMenuItem());
        printMenu.addSeparator();
        printMenu.setEnabled(false);
        return printMenu;
    }

    protected void enableShowResultsMenu(boolean ena) {
    }

    protected JMenu createShowResultsMenu() {
        resultsMenu = new JMenu("ViewResults");
        resultsMenu.add(resultPanels.get(DFHResult.Type.HEATSUMMARY).getMenuItem());
        resultsMenu.add(resultPanels.get(DFHResult.Type.SECTIONWISE).getMenuItem());
        resultsMenu.add(resultPanels.get(DFHResult.Type.TOPSECTIONWISE).getMenuItem());
        resultsMenu.add(resultPanels.get(DFHResult.Type.BOTSECTIONWISE).getMenuItem());
        resultsMenu.addSeparator();
        resultsMenu.add(resultPanels.get(DFHResult.Type.RECUBALANCE).getMenuItem());
        resultsMenu.addSeparator();
        resultsMenu.add(resultPanels.get(DFHResult.Type.LOSSDETAILS).getMenuItem());
        resultsMenu.addSeparator();
        resultsMenu.add(resultPanels.get(DFHResult.Type.FUELSUMMARY).getMenuItem());
        resultsMenu.add(resultPanels.get(DFHResult.Type.FUELS).getMenuItem());
        resultsMenu.add(resultPanels.get(DFHResult.Type.TOPFUELS).getMenuItem());
        resultsMenu.add(resultPanels.get(DFHResult.Type.BOTFUELS).getMenuItem());
        resultsMenu.addSeparator();
        resultsMenu.add(resultPanels.get(DFHResult.Type.TEMPRESULTS).getMenuItem());
        resultsMenu.add(resultPanels.get(DFHResult.Type.TOPtempRESULTS).getMenuItem());
        resultsMenu.add(resultPanels.get(DFHResult.Type.BOTtempRESULTS).getMenuItem());
        resultsMenu.addSeparator();
        resultsMenu.add(resultPanels.get(DFHResult.Type.COMBItempTRENDS).getMenuItem());
        resultsMenu.add(resultPanels.get(DFHResult.Type.TOPtempTRENDS).getMenuItem());
        resultsMenu.add(resultPanels.get(DFHResult.Type.BOTtempTRENDS).getMenuItem());
        resultsMenu.setEnabled(false);
        return resultsMenu;
    }

    protected void enableCompareMenu(boolean ena) {
        if (compareResults != null)
            compareResults.setEnabled(ena);
    }

    protected JMenu createCompareResultsMenu() {
        compareResults = new JMenu("Compare Results");
        compareResults.setEnabled(false);
        compareResults.add(mISaveComparison);
        compareResults.add(mIShowComparison);
        compareResults.add(mISaveComparisontoXL);
        compareResults.add(mIAppendComparisontoXL);
        compareResults.add(mIClearComparison);
        return compareResults;
    }

    protected JMenu definePerformanceMenu() {
        perfMenu = new JMenu("Performance");
        perfMenu.setMnemonic(KeyEvent.VK_P);
        return perfMenu;
    }

    protected JMenu createPerformanceMenu() {
        definePerformanceMenu();
//        perfMenu = new JMenu("Performance");
        perfMenu.add(mICreatePerfBase);
        perfMenu.add(mIAddToPerfBase);
        perfMenu.addSeparator();
        perfMenu.add(mIShowPerfBase);
        perfMenu.addSeparator();
        perfMenu.add(mIClearPerfBase);
        perfMenu.setEnabled(false);
        perfMenu.setVisible(false);
        return perfMenu;
    }

    protected void enableDefineMenu(boolean ena) {
        defineFurnaceMenu.setEnabled(ena);
    }

    protected JMenu defineDefineFurnaceMenu() {
        defineFurnaceMenu = new JMenu("DefineFurnace");
        defineFurnaceMenu.setMnemonic(KeyEvent.VK_D);
        return defineFurnaceMenu;
    }

    protected JMenu createDefineFurnaceMenu() {
        defineDefineFurnaceMenu();
//        defineFurnaceMenu = new JMenu("DefineFurnace");
        defineFurnaceMenu.add(mIInputData);
        defineFurnaceMenu.add(mIOpData);

        defineFurnaceMenu.addSeparator();
        defineFurnaceMenu.add(mIDefineRecuperator);
        defineFurnaceMenu.add(mIRecuPerformace);

        defineFurnaceMenu.addSeparator();
        defineFurnaceMenu.add(mICreateFuelMix);
        defineFurnaceMenu.add(mIRegenBurnerStudy);

        defineFurnaceMenu.addSeparator();
        defineFurnaceMenu.add(mITuningParams);

        defineFurnaceMenu.addSeparator();
        defineFurnaceMenu.add(mIBeamParamTFM);
        defineFurnaceMenu.add(mILossParamTFM);
        return defineFurnaceMenu;
    }

    protected void enableFileMenu(boolean ena) {
        fileMenu.setEnabled(ena);
    }

    protected JMenu defineFileMenu() {
        fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        return fileMenu;
    }

    protected JMenu createFileMenu() {
        defineFileMenu();
//        fileMenu = new JMenu("File");
//        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(mIGetFceProfile);
        fileMenu.add(mILoadRecuSpecs);
        fileMenu.addSeparator();
        fileMenu.add(mISaveFceProfile);
        fileMenu.add(mISaveToXL);
        fileMenu.addSeparator();
        fileMenu.add(mISaveForTFM);
        if (enableSpecsSave || onTest) {
            fileMenu.addSeparator();
            fileMenu.add(mISaveFuelSpecs);
            fileMenu.add(mISaveSteelSpecs);
        }
        fileMenu.addSeparator();
        fileMenu.add(mIExit);
        return fileMenu;
    }

    /**
     * All MenuItems area already created
     * @return
     */

    protected JMenuBar assembleMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.add(createFileMenu());
        mb.add(createDefineFurnaceMenu());
//        mb.add(createStatusMenu());
        mb.add(createShowResultsMenu());
        mb.add(createPrintResultsMenu());
        mb.add(createCompareResultsMenu());
        mb.add(createPerformanceMenu());
        mb.add(pbEdit);
        return mb;
    }

    protected void addMenuBar(JMenuBar menuBar) {
        mainAppPanel.add(menuBar, BorderLayout.NORTH);
    }

    protected void addMenuBar() {
        mainAppPanel.add(assembleMenuBar(), BorderLayout.NORTH);
    }

    public boolean isOnProductionLine() {
        return onProductionLine;
    }

    public void performanceTableDone() {
        switchPage(DFHDisplayPageType.OPPAGE);
        enableDataEdit();
    }

    public void enableDataEdit() {
        enableDataEntry(true);
        enableFileMenu(true);
        enableDefineMenu(true);
        pbEdit.getModel().setPressed(true);
        pbEdit.setEnabled(false);
    }

    protected void disableCompare() {
        if (compareResults != null) {
            compareResults.setEnabled(false);
            enableSaveForComparison(false);
            enableShowComparison(false);
        }
    }

    protected JMenuItem mICreatePerfBase;
    protected JMenuItem mIAddToPerfBase;
    protected JMenuItem mIClearPerfBase;
    protected JMenuItem mIShowPerfBase;
    protected JMenu perfMenu;
    protected JMenuItem mISetPerfTablelimits;

    void enableCreatePerform(boolean ena)  {
        mIClearPerfBase.setEnabled(!ena);
        mICreatePerfBase.setEnabled(bResultsReady && ena && freshResults);
    }

    void enableAddToPerform(boolean ena) {
        mIAddToPerfBase.setEnabled(ena && freshResults);
        mIShowPerfBase.setEnabled(ena);
    }

    void perfBaseAvailable(boolean available) {
        mIShowPerfBase.setEnabled(available);
        mIClearPerfBase.setEnabled(available);
    }

    protected void showPerfMenu(boolean show) {
        perfMenu.setVisible(show);
    }

    synchronized public void enablePerfMenu(boolean ena)  {
        perfMenu.setEnabled(ena);
    }

    void enableSaveForComparison(boolean ena) {
        mISaveComparison.setEnabled(ena);
    }

    void enableShowComparison(boolean ena) {
        mIShowComparison.setEnabled(ena);
        mISaveComparisontoXL.setEnabled(ena);
        if (asJNLP)
            mIAppendComparisontoXL.setEnabled(false);
        else
            mIAppendComparisontoXL.setEnabled(ena);
    }

    @Override
    public void destroy() {
        itsON = false;
        debug("In destroy()");
        super.destroy();
        mainF.dispose();
    }

    public boolean canClose() {
        boolean goAhead = true;
        if (furnace != null && furnace.isPerformanceToBeSaved())
            goAhead = decide("Unsaved Performance Data", "Some Performance data have been collected\n" +
                    "Do you want to ABANDON them and exit?");
        return goAhead;
    }

    public void close() {
        if (asApplication) {
            System.exit(0);
        }
        else {
            if (win != null)
                win.eval("gettingOut()");
        }
    }

    public void checkAndClose(boolean check) {
        if (!check || canClose()) close();
    }

    void showResultsPanel(String command) {
        DFHResult.Type type = DFHResult.Type.getEnum(command);
        if (type != null) {
            ResultPanel rP;
            rP = resultPanels.get(type);
            slate.setViewportView(rP.getPanel());
        }
    }

    void printResultsPanel(String command) {
        DFHResult.Type type = DFHResult.Type.getEnum(command);
        if (type != null) {
            ResultPanel rP;
            rP = printPanels.get(type);
            printIt(rP);
        }
    }

    Component secDetailsPanel() {
        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        detScroll = new JScrollPane();  // furnace.secDetailsPanel(false));
        detScroll.getHorizontalScrollBar().setUnitIncrement(150);
        changeTopBot(false);
        detScroll.setPreferredSize(new Dimension(700, 370));
        jp.add(detScroll, gbc);
        return jp;
    }

    void changeTopBot(String now) {
        if (now.equals("Bottom Zones"))
            detScroll.setViewportView(furnace.secDetailsPanel(true));
        else
            detScroll.setViewportView(furnace.secDetailsPanel(false));

    }

    public void changeTopBot(boolean toBot) {
        detScroll.setViewportView(furnace.secDetailsPanel(toBot));
    }

    FramedPanel getRowHeader() {
        lbTopLen = new NumberLabel(this, furnace.fceLength(false) * 1000, 0, "#,###");
        lbBotLen = new NumberLabel(this, furnace.fceLength(true) * 1000, 0, "#,###");
        rowHead = FceSubSection.getRowHeader(lbTopLen, lbBotLen);
        return rowHead;
    }

    FramedPanel lossTablePanel() {
        FramedPanel panel = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbcTp = new GridBagConstraints();
        LossType loss;
        gbcTp.gridx = 0;
        gbcTp.gridy = 0;
        panel.add(new JLabel("Loss Types List"), gbcTp);

        lossPanel = new FramedPanel(new GridBagLayout());
        gbcLoss = new GridBagConstraints();
        gbcLoss.gridx = 0;
        gbcLoss.gridx = 0;
        LossTypeList lossList = furnace.lossTypeList;
        Iterator<Integer> iter = lossList.keysIter();
        Integer k;
        while (iter.hasNext()) {
            k = iter.next();
            gbcLoss.gridy++;
            loss = lossList.get(k);
            lossPanel.add(loss.getLossPanel(), gbcLoss);
        }

        lossScroll = new JScrollPane(lossPanel);
        lossScroll.getVerticalScrollBar().setUnitIncrement(20);
        JPanel hp = new JPanel(new GridBagLayout());
        GridBagConstraints hgb = new GridBagConstraints();
        hgb.gridx = 0;
        hgb.gridy = 0;
        hp.add(LossType.getHeadPanel(), hgb);
        lossScroll.setColumnHeaderView(hp);
        lossScroll.setPreferredSize(new Dimension(850, 110));
        gbcTp.gridy++;
        panel.add(lossScroll, gbcTp);
        return panel;
    }

    protected FramedPanel titleAndFceCommon() {
        if (titleAndFceCommon == null) {
            FramedPanel panel = new FramedPanel(new GridBagLayout());
            GridBagConstraints gbcTp = new GridBagConstraints();
            gbcTp.gridx = 0;
            gbcTp.gridy = 0;
            panel.add(getTitlePanel(), gbcTp);
            gbcTp.gridx++;
//            panel.add(fceCommDataPanel(), gbcTp);
            mpFceCommDataPanel =  fceCommDataPanel();
            panel.add(mpFceCommDataPanel, gbcTp);
            titleAndFceCommon = panel;
        }
        cbFuel.updateUI();
        return titleAndFceCommon;
    }

    MultiPairColPanel titlePanel;

    FramedPanel getTitlePanel() {
        titlePanel = new MultiPairColPanel("");
        titlePanel.addItemPair("Reference ", tfReference);
        titlePanel.addItemPair("Title ", tfFceTitle);
        titlePanel.addItemPair("Customer ", tfCustomer);
        titlePanel.addItemPair("Furnace For ", cbFceFor);
        mpTitlePanel = titlePanel;
        return titlePanel;
    }
    MultiPairColPanel commonDataP;

    MultiPairColPanel fceCommDataPanel() {
        commonDataP = new MultiPairColPanel("");
        commonDataP.addItemPair("Heating Mode ", cbHeatingMode);
        commonDataP.addItemPair(ntfWidth);
        commonDataP.addItemPair("Common Fuel ", cbFuel);
        commonDataP.addItemPair(tfExcessAir);
        mpFceCommDataPanel = commonDataP;
        return commonDataP;
    }

    public void noteFiringModeChange(boolean bTopBot) {
        if (bTopBot) {
            lbBotLen.setEnabled(true);
            lbBotLen.setVisible(true);
        }
        else {
            lbBotLen.setEnabled(false);
            lbBotLen.setVisible(false);
        }
        FceSubSection.noteFiringModeChange(bTopBot);
        detScroll.validate();
    }

    JPanel chSizePanel = new JPanel();

    protected JPanel processAndCharge() {
        JPanel jp = new JPanel(new BorderLayout());
//        tfProcessName = new XLTextField(processName, 10);
        jp.add(processPanel(), BorderLayout.NORTH);
//        jp.add(chargeDataPanel(), BorderLayout.SOUTH);
        mpChargeData = chargeDataPanel();
        jp.add(mpChargeData, BorderLayout.SOUTH);
        return jp;
    }

    protected JPanel processPanel() {
        MultiPairColPanel jp = new MultiPairColPanel("Process");
        jp.addItemPair("Process Name", tfProcessName);
        return jp;
    }

    protected MultiPairColPanel chargeDataPanel() {
        MultiPairColPanel jp = new MultiPairColPanel("Charge Details");
        jp.addItemPair("Charge Cross Section", cbChType);
        jp.addItemPair(tfChDiameter);
        jp.addItemPair(labChWidth, tfChWidth);
        jp.addItemPair(tfChThickness.getName(), tfChThickness);
        jp.addItemPair(labChLength, tfChLength);
        jp.addItemPair("Charge Material", cbChMaterial);
        return jp;
    }

    public ChMaterial getSelChMaterial(String matName) {
        ChMaterial chMat = null;
        for (ChMaterial oneMat: vChMaterial)
            if (matName.equalsIgnoreCase(oneMat.name)) {
                chMat = oneMat;
                if (chMat instanceof JSPObject)
                    ((JSPObject)chMat).collectData(jspConnection);
                break;
            }
        return chMat;
    }

    public Fuel getSelFuel(String fuelName) {
        Fuel selFuel = null;
        for (Fuel fuel: fuelList)
            if (fuelName.equalsIgnoreCase(fuel.name)) {
                selFuel = fuel;
                if (fuel instanceof JSPObject)
                    ((JSPObject)fuel).collectData(jspConnection);
                break;
            }
        return selFuel;
    }

    public Fuel getSelFuel() {
        Fuel selFuel = null;
        if (fuelList.size() == 1)
            selFuel = fuelList.get(0);
        return selFuel;
    }

    public DFHTuningParams getTuningParams() {
        return tuningParams;
    }

    public ActionListener lengthChangeListener() {
        return lengthListener;
    }

    public FocusListener lengthFocusListener() {
        return lengthListener;
    }

    protected MultiPairColPanel chargeInFurnacePanel() {
        MultiPairColPanel jp = new MultiPairColPanel("Charge In Furnace");
        jp.addItemPair(tfBottShadow.getLabel(), tfBottShadow);
        jp.addItemPair(tfChPitch);
        jp.addItemPair(tfChRows);
        jp.addItemPair(tfProduction);
        jp.addItemPair(tfEntryTemp);
        jp.addItemPair(tfExitTemp);
        jp.addItemPair(tfDeltaTemp);
        jp.addItemPair(tfExitZoneFceTemp);
        jp.addItemPair(tfMinExitZoneFceTemp);
        jp.addItemPair(tfTotTime);
        jp.addItemPair(tfSpTime);
        jp.addItemPair(tfSpeed);
        return jp;
    }


    void setTimeValues(double totTime, double spTime, double speed) {
        if (totTime > 0) {
            tfTotTime.setData(totTime);
            tfSpTime.setData(spTime);
            tfSpeed.setData(speed);
        } else {
            tfTotTime.setText("N/A");
            tfSpTime.setText("N/A");
            tfSpeed.setText("N/A");
        }
    }

    protected MultiPairColPanel calCulDataPanel() {
        MultiPairColPanel jp = new MultiPairColPanel("Calculate");
        jp.addItemPair(tfAmbTemp);
        jp.addItemPair(tfAirTemp);
        jp.addItemPair(tfFuelTemp);
        jp.addItemPair(tfCalculStep);
        jp.addItemPair("", pbCalculate);
        return jp;
    }

    void enaCommonFuelTemp() {
        if (furnace.anyCommonFuel())
            tfFuelTemp.setEnabled(true);
        else
            tfFuelTemp.setEnabled(false);
    }

    MultiPairColPanel recuDataPanel() {
        MultiPairColPanel jp = new MultiPairColPanel("Recuperator Data");
        jp.addItemPair(ntDeltaTFlue);
        jp.addItemPair(ntMaxFlueTatRecu);
        jp.addItemPair("Common Air Heated in Recu", cBAirHeatByRecu);
        jp.addItemPair(ntdeltaTAirFromRecu);
        jp.addItemPair("Common Fuel Heated in Recu", cBFuelHeatByRecu);
        jp.addItemPair(ntDeltaTFuelFromRecu);
        jp.addItemPair("Air Recu is after Fuel Recu", cBAirAfterFuel);
        jp.addItem("<html><font color='red'>The above details are for Recuperator Heat Balance." +
                "<p>Even if Recu's are not enabled, the air and flue preheat" +
                "<p>temperatures under 'Calculate' block will be respected</html>");

        return jp;
    }

    void enableRecuSequence() {
        boolean bTowRecus = bFuelHeatedByRecu && bAirHeatedByRecu;
        cBAirAfterFuel.setSelected(cBAirAfterFuel.isSelected() && bTowRecus);
        cBAirAfterFuel.setEnabled(bTowRecus);
    }

    boolean xlFceBasicData(Sheet sheet, ExcelStyles styles) {
        Cell cell;
        Row r;

        r = sheet.createRow(0);
        cell = r.createCell(0);
        cell.setCellStyle(styles.csHeader1);
        cell.setCellValue("FURNACE BASIC DATA");

        sheet.setColumnWidth(1, 8000);
//        sheet.setColumnWidth(2, 9000);
        sheet.setColumnWidth(3, 4000);
        sheet.setColumnWidth(4, 8000);
        sheet.setColumnWidth(6, 4000);
        sheet.setColumnWidth(7, 8000);
        r = sheet.createRow(4);
        cell = r.createCell(1);
        cell.setCellValue("Date and Time");
        cell = r.createCell(2);
        Date date = new Date();

        cell.setCellValue(dateFormat.format(date));
        int topRow = 4, row, rRow, rRow1;
        int col = 1;
        row = styles.xlMultiPairColPanel(mpTitlePanel, sheet, topRow, col) + 1;
        col = 4;
        rRow = styles.xlMultiPairColPanel(mpFceCommDataPanel, sheet, topRow, col) + 1;
        topRow = Math.max(rRow, row);
        col = 1;
        row = styles.xlMultiPairColPanel(mpChargeData, sheet, topRow, col) + 1;
        col = 4;
        rRow = styles.xlMultiPairColPanel(mpChInFce, sheet, topRow, col) + 1;
        col = 7;
        rRow1 = styles.xlMultiPairColPanel(mpRecuData, sheet, topRow, col) + 1;
        topRow = Math.max(Math.max(rRow, row), rRow1);
        col = 1;
        row = styles.xlMultiPairColPanel(mpCalcul, sheet, topRow, col) + 1;

        return true;
    }

    boolean xlFceProfile(Sheet sheet, ExcelStyles styles) {
        Cell cell;
        Row r;

        r = sheet.createRow(0);
        cell = r.createCell(0);
        cell.setCellValue("mv7414");
        styles.hideCell(cell);
        cell = r.createCell(1);
        cell.setCellStyle(styles.csHeader1);
        cell.setCellValue("FURNACE PROFILE");
        r = sheet.createRow(1);
        cell = r.createCell(0);
        String xmlStr = inputDataXML(true);
        int len = xmlStr.length();
        int maxLen = 16000;
        int col = 0;
        if (len > maxLen) {
            int nCells = len / maxLen;
            int pos = 0;
            for (int c = 0; c < nCells; c++) {
                col++;
                cell = r.createCell(col);
                cell.setCellValue(xmlStr.substring(pos, pos + maxLen));
                pos += maxLen;
            }
            if (pos < len) { // some more left
                col++;
                cell = r.createCell(col);
                cell.setCellValue(xmlStr.substring(pos));
            }
        }
        else {
             col++;
            cell = r.createCell(col);
            cell.setCellValue(xmlStr);
        }
        cell = r.createCell(0);
        cell.setCellValue(col);
        sheet.protectSheet("mv7414");
        return true;
    }

    public void adjustForLengthChange() {
        furnace.adjustLengthChange();
        lbTopLen.setData(furnace.fceLength(false) * 1000);
        lbBotLen.setData(furnace.fceLength(true) * 1000);
        lbTopLen.showError(false);
        if (bTopBot) {
            if (lbTopLen.getText().equals(lbBotLen.getText())) {
                lbTopLen.showError(false);
                lbBotLen.showError(false);
            } else {
                lbTopLen.showError(true);
                lbBotLen.showError(true);
            }
        } else
            lbTopLen.showError(false);
        lbBotLen.showError(false);
    }

    void adjustForLossNameChange() {
        furnace.takeLossParams();
        furnace.addToLossList();
        rowHead.updateUI();
    }

    public DataWithStatus<OneStripDFHProcess> getDFHProcess(Performance forThisPerformance) {
        logInfo("in getDFHProcess in DFHeating");
        DataWithStatus<OneStripDFHProcess> retVal = dfhProcessList.getDFHProcess(forThisPerformance) ;
        logInfo("retVal status =" + retVal.getDataStatus() + ", values = " + retVal.getValue());
        return retVal;
    }

    protected OneStripDFHProcess getDFHProcess() {
        return getStripDFHProcess(getProcessName());
    }

    public OneStripDFHProcess getStripDFHProcess(String forProc) {
        return dfhProcessList.getDFHProcess(forProc.trim().toUpperCase());
    }

    public OneStripDFHProcess getStripDFHProcess(StripProcessAndSize theStrip) {
        return dfhProcessList.getDFHProcess(theStrip);
    }

    public OneStripDFHProcess getStripDFHProcess(String baseProcessNameX, double tempDFHExitX,
                                            double stripWidth, double stripThick) {
        return dfhProcessList.getDFHProcess(baseProcessNameX, tempDFHExitX, stripWidth, stripThick);
    }

    public boolean canNotify() {
        return canNotify;
    }

    public void enableNotify(boolean ena) {
        canNotify = ena;
    }

    public JFrame parent() {
        return mainF;
    }

    // for Applet version
    public String addChMaterial(String matName, String matID, String density, String tempTkPairStr, String tempHcPairStr,
                                String tempEmPairStr) {
        String retVal = "";
        double den = 0;
        try {
            den = Double.valueOf(density);
            if (matName.length() > 2 && matID.length() > 2 && den != 0 && tempTkPairStr.length() > 0 &&
                    tempHcPairStr.length() > 0 && tempEmPairStr.length() > 0) {
                vChMaterial.add(new ChMaterial(matName, matID, den, tempTkPairStr, tempHcPairStr, tempEmPairStr));
                retVal = "OK";
            } else {
                retVal = "ERROR in data of Material!";
            }
        } catch (NumberFormatException e) {
            retVal = "ERROR in number format in Material Density!";
        }
        return retVal;
    }

    public String addFuel(String name, String units, String calValStr, String afRatioStr, String ffRatioStr,
                          String sensHeatPair,
                          String CO2fractStr, String H2OfractStr, String SO2fractStr, String N2fractStr, String O2fractStr) {
        String retVal = "";
        double calVal, airFuelRatio, flueFuelRatio;
        double CO2fract, H2Ofract, SO2fract, N2fract, O2fract;
        XYArray sensHeat = null;
        if (sensHeatPair.trim().length() > 0)
            sensHeat = new XYArray(sensHeatPair);

        try {
            calVal = Double.valueOf(calValStr);
            airFuelRatio = Double.valueOf(afRatioStr);
            flueFuelRatio = Double.valueOf(ffRatioStr);
            CO2fract = Double.valueOf(CO2fractStr);
            H2Ofract = Double.valueOf(H2OfractStr);
            SO2fract = Double.valueOf(SO2fractStr);
            N2fract = Double.valueOf(N2fractStr);
            O2fract = Double.valueOf(O2fractStr);

            try {
                FlueComposition flue = new FlueComposition("Flue of " + name, CO2fract, H2Ofract, N2fract, O2fract, SO2fract);
                Fuel fuel = new Fuel(name, units, calVal, airFuelRatio, flueFuelRatio, sensHeat, flue);
                fuelList.add(fuel);
                retVal = "OK";
            } catch (Exception e) {
                retVal = "ERROR - some problem in Flue Composition!";
            }
        } catch (NumberFormatException e) {
            retVal = "ERROR - Number Conversion in Fuel Data!";
        }
        return retVal;
    }

    // for Applet version
    public String chargeBasic(String matName, String matID, String density, String width, String thickness) {
        double w = 0;
        double th = 0;
        double den = 0;
        String retVal = "";
        try {
            w = Double.valueOf(width);
            th = Double.valueOf(thickness);
            den = Double.valueOf(density);
            if (tk != null && hC != null && emiss != null) {
                material = new ChMaterial(matName, matID, den, tk, hC, emiss);
                theCharge = new Charge(material, 1.0, w, th);
                retVal = "OK";
            } else
                retVal = "ERROR: Properties not set!";
        } catch (NumberFormatException e) {
            retVal = "ERROR in number format in chargeBasic!";
        }
        return retVal;
    }

    // for applet version
    public String setChargeHeatCont(String heatCont) {
        hC = new XYArray(heatCont);
        return "OK " + hC.arrLen;
    }

    // for applet version
    public String setChargeTk(String thermalC) {
        tk = new XYArray(thermalC);
        return "OK " + tk.arrLen;
    }

    // for applet version
    public String setChargeEmiss(String emissivity) {
        emiss = new XYArray(emissivity);
        return "OK " + emiss.arrLen;
    }

    public static void debug(String msg) {
        if (log != null)
            log.debug("DFHeating:" + msg);
        else
            System.out.println("DFHeating: " + msg);
    }

    public Fuel getSelectedFuel() {
        return (Fuel)cbFuel.getSelectedItem();
    }

    protected void setExitZoneTemperatures(double tNormal, double tMin) {
        tfExitZoneFceTemp.setData(tNormal);
        exitZoneFceTemp = tNormal;
        tfMinExitZoneFceTemp.setData(tMin);
        minExitZoneFceTemp = tMin;
    }

    protected String getProcessName() {
        processName = tfProcessName.getText();
        return processName;
    }

    public void takeValuesFromUI() {
        reference = tfReference.getText();
        fceTtitle = tfFceTitle.getText();
        customer = tfCustomer.getText();

        furnaceFor = (DFHTuningParams.FurnaceFor) cbFceFor.getSelectedItem();
        tuningParams.setSelectedProc(furnaceFor);
//        bTopBot = (cbHeatingType.getSelectedIndex() == 1);
        fceWidth = ntfWidth.getData() / 1000;
        furnace.setFceWidth(fceWidth);
        commFuel = (Fuel) cbFuel.getSelectedItem();
        excessAir = tfExcessAir.getData() / 100;

        chWidth = tfChWidth.getData() / 1000;
        chLength = tfChLength.getData() / 1000;
        chThickness = tfChThickness.getData() / 1000;
        chDiameter = tfChDiameter.getData() / 1000;
        selChMaterial = (ChMaterial) cbChMaterial.getSelectedItem();

        bottShadow = tfBottShadow.getData() / 100;
        getProcessName();
//        processName = tfProcessName.getText();
        if (cbFceFor.getSelectedItem() == DFHTuningParams.FurnaceFor.STRIP)
            chPitch = 1;
        else
            chPitch = tfChPitch.getData() / 1000;
        nChargeRows = (int) tfChRows.getData();
        tph = tfProduction.getData();
        entryTemp = tfEntryTemp.getData();
        exitTemp = tfExitTemp.getData();
        deltaTemp = tfDeltaTemp.getData();
        exitZoneFceTemp = tfExitZoneFceTemp.getData();
        minExitZoneFceTemp = tfMinExitZoneFceTemp.getData();
        deltaTflue = ntDeltaTFlue.getData();
        deltaTAirFromRecu = ntdeltaTAirFromRecu.getData();
        maxFlueAtRecu = ntMaxFlueTatRecu.getData();

        ambTemp = tfAmbTemp.getData();
        airTemp = tfAirTemp.getData();

//        showMessage("Start Air preheat = " + airTemp);    // TODO remove in RELEASE

        fuelTemp = tfFuelTemp.getData();
        calculStep = tfCalculStep.getData() / 1000;
        furnace.takeValuesFromUI();
    }

    protected void hidePerformMenu() {
        if (perfMenu != null) {
            perfMenu.setVisible(false);
            mICreatePerfBase.setEnabled(false);
            mIAddToPerfBase.setEnabled(false);
            mIClearPerfBase.setEnabled(false);
            mIShowPerfBase.setEnabled(false);
        }
     }

    protected void setFcefor(boolean showSuggestion) {
        furnaceFor = (DFHTuningParams.FurnaceFor)cbFceFor.getSelectedItem();
        if (furnaceFor == DFHTuningParams.FurnaceFor.STRIP) {
            tfChDiameter.setEnabled(false);
            cbChType.setSelectedItem(Charge.ChType.SOLID_RECTANGLE);
            cbChType.setEnabled(false);
            tfChPitch.setEnabled(false);
            chPitch = 1;
            tfChRows.setEnabled(false);
            nChargeRows = 1;
            tfChWidth.setEnabled(false);
            chWidth = 1;
            labChLength.setText("Strip Width (mm)");
            tfExitZoneFceTemp.setEnabled(bDataEntryON);
            tfMinExitZoneFceTemp.setEnabled(bDataEntryON);
            tfDeltaTemp.setEnabled(false);
            setValuesToUI();
            showPerfMenu(true);
            enablePerfMenu(true);
            if (showSuggestion && cbHeatingMode.getSelectedItem() != HeatingMode.TOPBOTSTRIP)
                showMessage("Suggest selecting 'Heating Mode' to STRIP - TOP and BOTTOM");
        } else {
            if (cbHeatingMode.getSelectedItem() == HeatingMode.TOPBOTSTRIP)  {
                cbHeatingMode.setSelectedItem(HeatingMode.TOPBOT);
                showMessage("Heating Mode changed to TOP AND BOTTOM");
            }
            cbChType.setEnabled(true);
            tfProcessName.setEnabled(bDataEntryON);
            tfChPitch.setEnabled(bDataEntryON);
            tfChRows.setEnabled(bDataEntryON);
            tfChWidth.setEnabled(bDataEntryON);
            tfDeltaTemp.setEnabled(bDataEntryON);
            tfExitZoneFceTemp.setEnabled(false);
            tfExitZoneFceTemp.setData(0);
            tfMinExitZoneFceTemp.setEnabled(false);
            tfMinExitZoneFceTemp.setData(0);
            labChLength.setText("Billet/ Slab Length (mm)");
            furnace.clearPerfBase();
            hidePerformMenu();
        }
        disableSomeUIs();
        opPage.updateUI();
        if (furnaceFor == DFHTuningParams.FurnaceFor.MANUAL)
            showMessage("You have selected 'Furnace For' as 'Manually Set'.\n\n   YOU ARE ON YOUR OWN NOW !!!");
    }

    void setChargeSizeChoice() {
        if (cbChType.getSelectedItem() == Charge.ChType.SOLID_RECTANGLE) {
            tfChDiameter.setEnabled(false);
            tfChWidth.setEnabled(true && furnaceFor != DFHTuningParams.FurnaceFor.STRIP);
            tfChThickness.setEnabled(true);
        }
        else {
            tfChDiameter.setEnabled(true);
            tfChWidth.setEnabled(false);
            tfChThickness.setEnabled(false);
        }
    }

    void setValuesToUI() {
        tfReference.setText(reference);
        tfFceTitle.setText(fceTtitle);
        tfCustomer.setText(customer);
        ntfWidth.setData(fceWidth * 1000);
        tfExcessAir.setData(excessAir * 100);

        tfChWidth.setData(chWidth * 1000);
        tfChThickness.setData(chThickness * 1000);
        tfChLength.setData(chLength * 1000);
        tfChDiameter.setData(chDiameter * 1000);

        tfBottShadow.setData(bottShadow * 100);
        tfProduction.setData(tph);
        tfProcessName.setText(processName);
        tfChPitch.setData(chPitch * 1000);
        tfChRows.setData(nChargeRows);
        tfProduction.setData(tph);
        tfEntryTemp.setData(entryTemp);
        tfExitTemp.setData(exitTemp);
        tfDeltaTemp.setData(deltaTemp);
        tfExitZoneFceTemp.setData(exitZoneFceTemp);
        tfMinExitZoneFceTemp.setData(minExitZoneFceTemp);
        ntDeltaTFlue.setData(deltaTflue);
        ntdeltaTAirFromRecu.setData(deltaTAirFromRecu);
        ntMaxFlueTatRecu.setData(maxFlueAtRecu);
        ntDeltaTFuelFromRecu.setData(deltaTFuelFromRecu);
        if (cBAirHeatByRecu.isSelected() != bAirHeatedByRecu)
            cBAirHeatByRecu.doClick();
        if (cBFuelHeatByRecu.isSelected() != bFuelHeatedByRecu)
            cBFuelHeatByRecu.doClick();
        if (cBAirAfterFuel.isSelected() != bAirAfterFuel)
            cBAirAfterFuel.doClick();

        tfAmbTemp.setData(ambTemp);
        tfAirTemp.setData(airTemp);
        tfFuelTemp.setData(fuelTemp);
        tfCalculStep.setData(calculStep * 1000);

        ntDeltaTFlue.setData(deltaTflue);
        ntdeltaTAirFromRecu.setData(deltaTAirFromRecu);
        ntMaxFlueTatRecu.setData(maxFlueAtRecu);
        ntDeltaTFuelFromRecu.setData(deltaTFuelFromRecu);
        cBAirHeatByRecu.setSelected(bAirHeatedByRecu);
        cBFuelHeatByRecu.setSelected(bFuelHeatedByRecu);
    }

    ErrorStatAndMsg isRecuDataOK() {
        boolean ok = true;
        String msg = "";
        if (bAirHeatedByRecu || bFuelHeatedByRecu) {
            if (ntDeltaTFlue.isInError()) {
                ok = false;
                msg += "\n   " + ntDeltaTFlue.getName();
            }
            if (ntMaxFlueTatRecu.isInError()) {
                ok = false;
                msg += "\n   " + ntMaxFlueTatRecu.getName();
            }
        }
        if (bAirHeatedByRecu) {
            if (ntdeltaTAirFromRecu.isInError()) {
                ok = false;
                msg += "\n   " + ntdeltaTAirFromRecu.getName();
            }
        }
        if (bFuelHeatedByRecu) {
            if (ntDeltaTFuelFromRecu.isInError()) {
                ok = false;
                msg += "\n   " + ntDeltaTFuelFromRecu.getName();
            }
        }

        return new ErrorStatAndMsg(!ok, msg);
    }

    ErrorStatAndMsg isChargeDetailsOK() {
        boolean ok = true;
        String msg = "";
        if (tfChLength.isInError()) {
            ok = false;
            msg += "\n   " + tfChLength.getName();
        }
        if (tfChThickness.isInError()) {
            ok = false;
            msg += "\n   " + tfChThickness.getName();
        }
        if (tfChWidth.isInError()) {
            ok = false;
            msg += "\n   " + tfChWidth.getName();
        }
//        if (cbChMaterial.getSelectedIndex() < 0) {
//            ok = false;
//            msg += "\n   Material of Charge";
//        }
        return new ErrorStatAndMsg(!ok, msg);
    }

    ErrorStatAndMsg isFuelOK() {
        boolean ok = true;
        String msg = "";
        if (cbFuel.getSelectedIndex() < 0) {
            ok = false;
            msg = "\n   Fuel";
        }
        return new ErrorStatAndMsg(!ok, msg);
    }

    protected ErrorStatAndMsg isChargeInFceOK() {
        boolean ok = true;
        String msg = "";
        if (cbChMaterial.getSelectedIndex() < 0) {
            ok = false;
            msg += "\n   Material of Charge";
        }
        if (tfBottShadow.isInError()) {
            ok = false;
            msg += "\n   " + tfBottShadow.getName();
        }
        if (tfChPitch.isInError()) {
            ok = false;
            msg += "\n   " + tfChPitch.getName();
        }
        if (tfChRows.isInError()) {
            ok = false;
            msg += "\n   " + tfChRows.getName();
        }
        if (tfProduction.isInError()) {
            ok = false;
            msg += "\n   " + tfProduction.getName();
        }
        if (tfEntryTemp.isInError()) {
            ok = false;
            msg += "\n   " + tfEntryTemp.getName();
        }
        if (tfExitTemp.isInError()) {
            ok = false;
            msg += "\n   " + tfExitTemp.getName();
        }
        if (tfDeltaTemp.isInError()) {
            ok = false;
            msg += "\n   " + tfDeltaTemp.getName();
        }
        return new ErrorStatAndMsg(!ok, msg);
    }

    ErrorStatAndMsg isBasicDataOK() {
        boolean ok = true;
        String msg = "";
        if (ntfWidth.isInError()) {
            ok = false;
            msg += "\n   " + ntfWidth.getName();
        }
        if (tfExcessAir.isInError()) {
            ok = false;
            msg += "\n   " + tfExcessAir.getName();
        }
        return new ErrorStatAndMsg(!ok, msg);
    }

    ErrorStatAndMsg isCalculDataOK() {
        boolean ok = true;
        String msg = "";
        if (tfAmbTemp.isInError()) {
            ok = false;
            msg += "\n   " + tfAmbTemp.getName();
        }
        if (tfAirTemp.isInError()) {
            ok = false;
            msg += "\n   " + tfAirTemp.getName();
        }
        if (tfCalculStep.isInError()) {
            ok = false;
            msg += "\n   " + tfCalculStep.getName();
        }
        return new ErrorStatAndMsg(!ok, msg);
    }

    boolean checkData() {
        String msg = "ERROR : ";
        boolean retVal = true;
        double val;
        ErrorStatAndMsg em;
        if ((em = isFuelOK()).inError) {
            msg += nlSpace + em.msg;
            retVal = false;
        }
        if ((em = isChargeDetailsOK()).inError) {
            msg += nlSpace + em.msg;
            retVal = false;
        }
        if ((em = isChargeInFceOK()).inError) {
            msg += nlSpace + em.msg;
            retVal = false;
        }
        if (retVal) {
            double w = (cbChType.getSelectedItem() == Charge.ChType.SOLID_CIRCLE) ? chDiameter : chWidth;
            if (w > chPitch) {
                msg += nlSpace + "Charge pitch is Less than the charge width/diameter";
                retVal &= false;
            }
        }
        if ((em = isBasicDataOK()).inError) {
            msg += nlSpace + em.msg;
            retVal = false;
        }
        if ((em = isCalculDataOK()).inError) {
            msg += nlSpace + em.msg;
            retVal = false;
        }
        if ((em = furnace.isFurnaceOK()).inError) {
            msg += nlSpace + em.msg;
            retVal = false;
        }

        if ((em = isRecuDataOK()).inError) {
            msg += nlSpace + em.msg;
            retVal = false;
        }

        if (retVal) {
            if ((chLength * nChargeRows) > fceWidth) {
                retVal &= false;
                msg += nlSpace + "Check " + tfChLength.titleAndVal() +
                        ", Charge Rows " + nChargeRows + " and " + ntfWidth.titleAndVal();
            }
            val = furnace.minHeight();
            if (chThickness > val) {
                retVal &= false;
                msg += nlSpace + "Check " + tfChThickness.titleAndVal() +
                        " and Minimum Height of Zone";
            }
            if (entryTemp >= exitTemp) {
                retVal &= false;
                msg += nlSpace + "Check " + tfEntryTemp.titleAndVal() +
                        " and " + tfExitTemp.titleAndVal();
            }

            DFHTuningParams.FurnaceFor proc;
            proc = (DFHTuningParams.FurnaceFor) cbFceFor.getSelectedItem();

            if (proc == DFHTuningParams.FurnaceFor.BILLETS) {  // billets
                if (chThickness < 0.01) {
                    retVal &= false;
                    msg += nlSpace + "Check " + tfChThickness.titleAndVal() +
                            " for Furnace for " + DFHTuningParams.FurnaceFor.BILLETS;
                }
            }
            if (proc == DFHTuningParams.FurnaceFor.STRIP) {
                if (chThickness > 0.01) {
                    retVal &= false;
                    msg += nlSpace + "Check " + tfChThickness.titleAndVal() +
                            " for Furnace for " + DFHTuningParams.FurnaceFor.STRIP;
                }
                if (exitZoneFceTemp < exitTemp) {
                    retVal &= false;
                    msg += nlSpace + "Check " + tfExitZoneFceTemp.titleAndVal() +
                            " with " + tfExitTemp.titleAndVal();
                }
                if (exitZoneFceTemp < minExitZoneFceTemp) {
                    retVal &= false;
                    msg += nlSpace + "Check " + tfExitZoneFceTemp.titleAndVal() +
                            " with " + tfMinExitZoneFceTemp.titleAndVal();
                }
            }
            String fromFurnace = furnace.checkData(nlSpace);
            if (fromFurnace.length() > 0) {
                retVal &= false;
                msg += nlSpace + fromFurnace;

            }
        }
        if (!retVal) {
            JOptionPane.showMessageDialog(mainF, msg);
            mainF.toFront();
            debug(msg);
        } else {
//            if (bAirHeatedByRecu && !bFuelHeatedByRecu && !furnace.checkExistingRecu()) {
            if (!bFuelHeatedByRecu && !furnace.checkExistingRecu()) {
                    furnace.newRecu();
            }
        }
        return retVal;
    }

    protected void enableResultsMenu(boolean enable) {
        if (resultsMenu != null)
            resultsMenu.setEnabled(enable);
        if (printMenu != null)
            printMenu.setEnabled(enable);
        mISaveToXL.setEnabled(enable && !bDataEntryON);
        mISaveForTFM.setEnabled(enable && !bDataEntryON);
        if (!enable) {
            ResultPanel rp;
            DFHResult.Type[] allRts = DFHResult.Type.values();
            DFHResult.Type oneRT;
            for (int t = 0; t < allRts.length; t++) {
                oneRT = allRts[t];
                rp = resultPanels.get(oneRT);
                if (rp != null)
                    rp.removePanel();
                rp = printPanels.get(oneRT);
                if (rp != null)
                    rp.removePanel();
            }
        }
    }

    void changeAirTemp(double newTemp) {
        airTemp = newTemp;
        tfAirTemp.setData(airTemp);
    }

    public FceEvaluator calculateForPerformanceTable(Performance baseP) {
//        baseP.setTableFactors(tuningParams.outputStep, tuningParams.widthStep);
        return calculateForPerformanceTable(baseP, null);
    }

    public FceEvaluator calculateForPerformanceTable(Performance baseP, CalculationsDoneListener doneListener) {
        enableResultsMenu(false);
        enableCalculStat();
        Thread evalThread = new Thread(evaluator = new FceEvaluator(this, slate, furnace, calculStep, baseP, doneListener));
        enablePauseCalcul();
        evalThread.start();
        evaluator.noteYourThread(evalThread);
        return evaluator;
    }

    ResultsReadyListener theResultsListener;

    void addResultsListener(ResultsReadyListener resultReadyListener) {
        theResultsListener = resultReadyListener;
    }

    Vector<CalculationsDoneListener> calculationListeners = new Vector<CalculationsDoneListener>();

    /**
     *
     * @param resultsReadyListener
     */
    public FceEvaluator calculateFce(ResultsReadyListener resultsReadyListener, boolean bShowResults, boolean bResetLossFactor, boolean bCheckData) { // TODO 20160622 is bCheckData required
        if (bShowResults) {
            initPrintGroups();
            enableResultsMenu(false);
        }
        if (bResetLossFactor) {
            furnace.resetLossFactor();
//            if (checkData())    on 20151213
                takeValuesFromUI();
        }
        boolean proceed = false;
        Thread evalThread = null;
        if (bCheckData) {
            if (checkData()) {
                if (furnace.showZoneDataMsgIfRequired(pbCalculate)) {
                    if (!commFuel.bMixedFuel && fuelTemp > 0 && !tuningParams.bOnProductionLine
                            && !commFuel.isSensHeatSpecified(this, fuelTemp)) {
                        commFuel.getSpHtData(this, tfFuelTemp);
                    }
                    furnace.setCommonFuel(new FuelFiring(commFuel, false, excessAir, airTemp, fuelTemp));  // as normal burner
                    //                theCharge = new Charge(selChMaterial, chLength, chWidth, chThickness);
                    theCharge = new Charge(selChMaterial, chLength, chWidth, chThickness, chDiameter, (Charge.ChType) cbChType.getSelectedItem());

                    setProductionData(theCharge, tph * 1000);
                    proceed = true;
                }
            }
        } else
            proceed = true;
        if (proceed) {
            if (evaluator != null)
                if (evaluator.stopped)
                    evaluator = null;
            if (evaluator == null) {
                enableCalculStat();
                evaluator = new FceEvaluator(this, slate, furnace, calculStep);
                evaluator.setShowProgress(bShowResults);
                evalThread = new Thread(evaluator);
                evaluator.noteYourThread(evalThread);
                addResultsListener(resultsReadyListener);
                evalThread.start();
            } else
                showError("Earlier Calculation is still ON!");
        }
        return evaluator;
    }

    protected ProductionData defineProduction() {
        return new ProductionData(processName);
    }

    public boolean setProductionData(Charge charge, double output) {
        ProductionData production = defineProduction();
        production.setCharge(charge, chPitch);
        production.setProduction(output, nChargeRows, entryTemp, exitTemp, deltaTemp, bottShadow);
        production.setExitZoneTempData(exitZoneFceTemp, minExitZoneFceTemp);
        furnace.setProductionData(production);
        return true;
    }

//    public boolean setProductionData(ProductionData production) {
//        furnace.setProductionData(new ProductionData(production));
//        return true;
//    }

    public FceEvaluator calculateFce(boolean bResetLossFactor, ResultsReadyListener resultsReadyListener) {
        return calculateFce(resultsReadyListener, true, bResetLossFactor, true);
    }

    public FceEvaluator calculateFce() {
        return calculateFce(true, null);
    }

    void enableCalculStat() {
        enableFileMenu(false);
        enableDefineMenu(false);
        enableDataEntry(false);
    }

    void enablePauseStat() {
        enableDataEntry(false);
        enableDefineMenu(true);
    }

    public void pausingCalculation(boolean paused) {
        if (paused)
            enablePauseStat();
        else
            enableCalculStat();
    }

    protected DFHTuningParams.FurnaceFor furnaceFor() {
        return furnaceFor;
    }

    public void abortingCalculation(String reason) {
        evaluator = null;
        enableDataEntry(true);
        enableDefineMenu(true);
        enableResultsMenu(false);
        enableFileMenu(true);
        enablePerfMenu(true);
        showError("ABORTING CALCULATION!\n" + reason);
        switchPage(DFHDisplayPageType.INPUTPAGE);
        parent().toFront();
    }

    void enablePauseCalcul() {
    }

    void furtherCalculations() {
    }

    public ActionListener calCulStatListener() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String inputDataXML(boolean withPerformance) {
        return XMLmv.putTag("DataTitle", DFHversion) + XMLmv.putTag("DFHeating", dataInXML(withPerformance));
    }

    public String dataInXML(boolean withPerformance) {
        takeValuesFromUI();
        String xmlStr = XMLmv.putTag("reference", reference) + "\n" +
                XMLmv.putTag("fceTitle", fceTtitle) + "\n" +
                XMLmv.putTag("customer", customer) + "\n" +
                XMLmv.putTag("cbFceFor", "" + cbFceFor.getSelectedItem()) +
                XMLmv.putTag("cbHeatingType", "" + cbHeatingMode.getSelectedItem()) + "\n" +
                XMLmv.putTag("width", "" + fceWidth) +
                XMLmv.putTag("cbFuel", "" + cbFuel.getSelectedItem()) +
                XMLmv.putTag("excessAir", "" + excessAir) + "\n" +
                XMLmv.putTag("chargeData", chDataInXML()) + "\n" +
                XMLmv.putTag("recuData", recuDataInXML()) + "\n" +
                XMLmv.putTag("productionData", productionDataInXML()) + "\n" +
                XMLmv.putTag("calculData", calculDataInXML()) + "\n" +
                XMLmv.putTag("tuning", tuningParams.dataInXML() + "\n") +
                XMLmv.putTag("furnace", furnace.dataInXML(withPerformance));
        return xmlStr;
    }

    String chDataInXML() {
        String xmlStr = XMLmv.putTag("chWidth", chWidth) +
                XMLmv.putTag("chThickness", chThickness) +
                XMLmv.putTag("chLength", chLength) +
                XMLmv.putTag("chDiameter", chDiameter) +
                XMLmv.putTag("chType", cbChType.getSelectedItem().toString());
                XMLmv.putTag("chMaterial", "" + cbChMaterial.getSelectedItem());

        return xmlStr;
    }

    String recuDataInXML() {
        String xmlStr = XMLmv.putTag("deltaTflue", deltaTflue) +
                XMLmv.putTag("deltaTAir", deltaTAirFromRecu) +
                XMLmv.putTag("maxFlueAtRecu", maxFlueAtRecu) +

                XMLmv.putTag("bAirHeatedByRecu", bAirHeatedByRecu) +
                XMLmv.putTag("bFuelHeatedByRecu", bFuelHeatedByRecu) +
                XMLmv.putTag("bAirAfterFuel", bAirAfterFuel) +
                XMLmv.putTag("deltaTFuelFromRecu", deltaTFuelFromRecu);
        return xmlStr;
    }

    String productionDataInXML() {
        String xmlStr = XMLmv.putTag("tph", tph) +
                XMLmv.putTag("entryTemp", entryTemp) +
                XMLmv.putTag("exitTemp", exitTemp) +
                XMLmv.putTag("processName", processName) +
                XMLmv.putTag("chPitch", chPitch) +
                XMLmv.putTag("nChargeRows", nChargeRows) +
                XMLmv.putTag("deltaTemp", deltaTemp) +
                XMLmv.putTag("exitZfceTemp", exitZoneFceTemp) +
                XMLmv.putTag("minExitZoneFceTemp", minExitZoneFceTemp) +
                XMLmv.putTag("bottShadow", bottShadow);
        return xmlStr;
    }

    String calculDataInXML() {
        String xmlStr = XMLmv.putTag("ambTemp", ambTemp) +
                XMLmv.putTag("airTemp", airTemp) +
                XMLmv.putTag("fuelTemp", fuelTemp) +
                XMLmv.putTag("calculStep", calculStep);
        return xmlStr;
    }

    protected boolean checkVersion(String xmlStr) {
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "DataTitle", 0);
        return (vp.val.equals(DFHversion));
    }

    public StatusWithMessage takeProfileDataFromXML(String xmlStr) {
//        logInfo("in takeProfileDatFromXML in DFHeating");
        return takeProfileDataFromXML(xmlStr, false, null, null);
    }

    public StatusWithMessage takeProfileDataFromXML(String xmlStr, boolean selective,
                                                    HeatingMode heatingMode, DFHTuningParams.FurnaceFor particularFceFor) {
        StatusWithMessage retVal = new StatusWithMessage();
        enableResultsMenu(false);
        String errMsg = "";
        boolean allOK = true;
        ValAndPos vp;
        XMLgroupStat grpStat;
        boolean bFromTFM = false;
        vp = XMLmv.getTag(xmlStr, "DataFromTFM", 0);
        if (vp.val.length() > 100) {
            bFromTFM = true;
            setItFromTFM(true);
            debug("data From TFM");
        }
        vp = XMLmv.getTag(xmlStr, "DataTitle", 0);
        if (vp.val.equals(DFHversion)) {
            vp = XMLmv.getTag(xmlStr, "DFHeating", 0);
            String acTData = vp.val;
            if (acTData.length() > 1300) {
                aBlock:
                {
                    vp = XMLmv.getTag(acTData, "reference", 0);
                    reference = vp.val;
                    vp = XMLmv.getTag(acTData, "fceTitle", 0);
                    fceTtitle = vp.val;
                    vp = XMLmv.getTag(acTData, "customer", 0);
                    customer = vp.val;

                    try {
                        vp = XMLmv.getTag(acTData, "width", 0);
                        fceWidth = Double.valueOf(vp.val);
                        furnace.setFceWidth(fceWidth);
                        vp = XMLmv.getTag(acTData, "excessAir", 0);
                        excessAir = Double.valueOf(vp.val);
                    } catch (Exception e) {
                        excessAir = 0.1;
                    }
                    fceFor1stSwitch = true; // to disable fceFor switch warning
                    vp = XMLmv.getTag(acTData, "cbFceFor", 0);
//                    debug("before FurnaceFor.getEnum, cbFceFor = " + vp.val);
                    DFHTuningParams.FurnaceFor fceFor = DFHTuningParams.FurnaceFor.getEnum(vp.val);
                    if (selective && (particularFceFor != null) && (fceFor != particularFceFor)) {
                        allOK = false;
                        errMsg = "Furnace is not for the required Process, " + particularFceFor;
                        break aBlock;
                    }
                    if (fceFor != null) {
                        cbFceFor.setSelectedItem(fceFor);
                        furnaceFor = fceFor;
                    }
                    vp = XMLmv.getTag(acTData, "cbHeatingType", 0);
                    if (selective && (heatingMode != null) && !vp.val.equals("" + heatingMode)) {
                        allOK = false;
                        errMsg = "Furnace is not with the required Heating mode, " + heatingMode;
                        break aBlock;
                    }
                    setHeatingMode(vp.val);
                    vp = XMLmv.getTag(acTData, "chargeData", 0);
                    grpStat = chDataFromXML(vp.val);
                    if (!grpStat.allOK) {
                        errMsg += "In Getting Charge Data: \n" + grpStat.errMsg;
                        allOK = false;
                    }
                    setFcefor(false);
                    setChargeSizeChoice();
                    vp = XMLmv.getTag(acTData, "recuData", 0);
                    grpStat = recuDataFromXML(vp.val, bFromTFM);
                    if (!grpStat.allOK) {
                        errMsg += "In Getting recuData: \n" + grpStat.errMsg;
                        allOK = false;
                    }
                    if (bFromTFM) {
                        debug("Not reading TUNING data from TFM");
                    } else {
                        vp = XMLmv.getTag(acTData, "tuning", 0);
                        if (!tuningParams.takeDataFromXML(vp.val)) {
                            allOK = false;
                            errMsg += "   in Tuning data Data\n";
                        }
                    }
                    vp = XMLmv.getTag(acTData, "furnace", 0);
                    if (bFromTFM) {
                        grpStat = furnace.takeTFMData(vp.val);
                        if (!grpStat.allOK) {
                            errMsg += "In Getting Furnace Data: \n" + grpStat.errMsg;
                            allOK = false;
                        }
                    } else {
                        if (!furnace.takeDataFromXML(vp.val)) {
                            allOK = false;
                            errMsg += "   in Furnace Data\n";
                        }
                    }
                    vp = XMLmv.getTag(acTData, "productionData", 0);
                    grpStat = productionFromXML(vp.val, bFromTFM);
                    if (!grpStat.allOK) {
                        errMsg += "In Getting Production Data: \n" + grpStat.errMsg;
                        allOK = false;
                    }
                    vp = XMLmv.getTag(acTData, "calculData", 0);
                    grpStat = calculDataFromXML(vp.val, bFromTFM);
                    if (!grpStat.allOK) {
                        errMsg += "In Getting Calculation Data: \n" + grpStat.errMsg;
                        allOK = false;
                    }
                    setValuesToUI();
                } // aBlock
            } else {
                allOK = false;
                errMsg += "   in XML Data Length\n";
            }
            if (!allOK) {
                debug("ERROR: " + errMsg);
                retVal.addErrorMessage(errMsg);
            }
        } else
            retVal.addErrorMessage("ERROR: " + "Version ! (" + vp.val + ")");
        freshResults = false;
        return retVal;
    }

    XMLgroupStat chDataFromXML(String xmlStr) {
        XMLgroupStat grpStat = new XMLgroupStat();
        DoubleWithErrStat dblWithStat;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "chWidth", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "chWidth", grpStat)).allOK)
            chWidth = dblWithStat.val;
        vp = XMLmv.getTag(xmlStr, "chThickness", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "chThickness", grpStat)).allOK)
            chThickness = dblWithStat.val;
        vp = XMLmv.getTag(xmlStr, "chLength", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "chLength", grpStat)).allOK)
            chLength = dblWithStat.val;
        vp = XMLmv.getTag(xmlStr, "chType", 0);
        if (vp.val.length() > 5) {
            cbChType.setSelectedItem(Charge.ChType.getEnum(vp.val));
            if (cbChType.getSelectedItem() == Charge.ChType.SOLID_CIRCLE)  {
                vp = XMLmv.getTag(xmlStr, "chDiameter", 0);
                if ((dblWithStat = new DoubleWithErrStat(vp.val, "chDiameter", grpStat)).allOK)
                    chDiameter = dblWithStat.val;
            }
        }
        else
            cbChType.setSelectedItem(Charge.ChType.SOLID_RECTANGLE);
        return grpStat;
    }

    XMLgroupStat recuDataFromXML(String xmlStr, boolean bFromTFM) {
        XMLgroupStat grpStat = new XMLgroupStat();
        DoubleWithErrStat dblWithStat;
        ValAndPos vp;
        if (xmlStr.length() > 30) {
            vp = XMLmv.getTag(xmlStr, "deltaTflue", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "deltaTflue", grpStat)).allOK)
                deltaTflue = dblWithStat.val;
            vp = XMLmv.getTag(xmlStr, "deltaTAir", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "deltaTAir", grpStat)).allOK)
                deltaTAirFromRecu = dblWithStat.val;
            vp = XMLmv.getTag(xmlStr, "maxFlueAtRecu", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "maxFlueAtRecu", grpStat)).allOK)
                maxFlueAtRecu = dblWithStat.val;
            vp = XMLmv.getTag(xmlStr, "bAirHeatedByRecu", 0);
            bAirHeatedByRecu = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bFuelHeatedByRecu", 0);
            bFuelHeatedByRecu = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "bAirAfterFuel", 0);
            bAirAfterFuel = (vp.val.equals("1"));
            if (bFuelHeatedByRecu) {
                vp = XMLmv.getTag(xmlStr, "deltaTFuelFromRecu", 0);
                if ((dblWithStat = new DoubleWithErrStat(vp.val, "deltaTFuelFromRecu", grpStat)).allOK)
                    deltaTFuelFromRecu = dblWithStat.val;
            }
        }
        return grpStat;
    }

    XMLgroupStat productionFromXML(String xmlStr, boolean bFromTFM) {
        XMLgroupStat grpStat = new XMLgroupStat();
        DoubleWithErrStat dblWithStat;
        ValAndPos vp;
        if (cbFceFor.getSelectedItem() != DFHTuningParams.FurnaceFor.STRIP) {
            vp = XMLmv.getTag(xmlStr, "nChargeRows", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "Charge Rows, taking as 1", grpStat)).allOK)
                nChargeRows = (int) dblWithStat.val;
            else
                nChargeRows = 1;
        } else
            nChargeRows = 1;

        vp = XMLmv.getTag(xmlStr, "tph", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "tph", grpStat)).allOK)
            tph = dblWithStat.val;
        vp = XMLmv.getTag(xmlStr, "entryTemp", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "entryTemp", grpStat)).allOK)
            entryTemp = dblWithStat.val;
        vp = XMLmv.getTag(xmlStr, "exitTemp", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "exitTemp", grpStat)).allOK)
            exitTemp = dblWithStat.val;
        vp = XMLmv.getTag(xmlStr, "processName", 0);
        processName = vp.val;
        vp = XMLmv.getTag(xmlStr, "chPitch", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "chPitch", grpStat)).allOK)
            chPitch = dblWithStat.val;
        vp = XMLmv.getTag(xmlStr, "deltaTemp", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "deltaTemp", grpStat)).allOK)
            deltaTemp = dblWithStat.val;
        vp = XMLmv.getTag(xmlStr, "exitZfceTemp", 0);
        if (vp.val.trim().length() > 0) {
            try {
                exitZoneFceTemp = Double.valueOf(vp.val);
            } catch (Exception e) {
                exitZoneFceTemp = 1050;
            }
        }
        vp = XMLmv.getTag(xmlStr, "minExitZoneFceTemp", 0);
        if (vp.val.trim().length() > 0) {
            try {
                minExitZoneFceTemp = Double.valueOf(vp.val);
            } catch (Exception e) {
                minExitZoneFceTemp = 900;
            }
        }
        vp = XMLmv.getTag(xmlStr, "bottShadow", 0);
        if (bFromTFM) {
            bTopBot = furnace.bTopBot;
            if (bTopBot)
                evalBottShadow(vp.val, grpStat);
            else
                bottShadow = 0;
        } else {
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "bottShadow", grpStat)).allOK)
                bottShadow = dblWithStat.val;
        }
        return grpStat;
    }

    XMLgroupStat evalBottShadow(String xmlStr, XMLgroupStat grpStat) {
        DoubleWithErrStat dblWithStat;
        ValAndPos vp;
        int nSecs = 0;
        if (xmlStr.length() > 20) {
            vp = XMLmv.getTag(xmlStr, "nSections", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "Beam Sections", grpStat)).allOK)
                nSecs = (int) dblWithStat.val;
            if (nSecs > 0) {
                double[] len = new double[nSecs];
                double[] totW = new double[nSecs];
                String secName;
                String oneSec;
                for (int i = 0; i < nSecs; i++) {
                    secName = "sectionBeams" + ("" + (i + 1)).trim();
                    vp = XMLmv.getTag(xmlStr, secName, 0);
                    oneSec = vp.val;
                    vp = XMLmv.getTag(oneSec, "sectionLength", 0);
                    if ((dblWithStat = new DoubleWithErrStat(vp.val, "Beams " + secName + " length", grpStat)).allOK)
                        len[i] = dblWithStat.val;
                    vp = XMLmv.getTag(oneSec, "beamsTotWidth", 0);
                    if ((dblWithStat = new DoubleWithErrStat(vp.val, "Beams " + secName + " TotWidth", grpStat)).allOK)
                        totW[i] = dblWithStat.val;
                }
                if ((len[0] > 0)) {
                    double totLW = 0;
                    double totL = 0;
                    for (int i = 0; i < nSecs; i++) {
                        totL += len[i];
                        totLW += totW[i] * len[i];
                    }
                    double avgTotW = totLW / totL;
                    bottShadow = avgTotW / chLength;
                }
            }
        } else
            grpStat.addStat(false, "No Beam Data\n");
        return grpStat;
    }

    XMLgroupStat calculDataFromXML(String xmlStr, boolean bFromTFM) {
        XMLgroupStat grpStat = new XMLgroupStat();
        DoubleWithErrStat dblWithStat;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "ambTemp", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "ambTemp", grpStat)).allOK)
            ambTemp = dblWithStat.val;
        if (!bFromTFM) {
            vp = XMLmv.getTag(xmlStr, "airTemp", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "airTemp", grpStat)).allOK)
                airTemp = dblWithStat.val;
            //            airTemp = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "fuelTemp", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "fuelTemp", grpStat)).allOK)
                fuelTemp = dblWithStat.val;
            vp = XMLmv.getTag(xmlStr, "calculStep", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "calculStep", grpStat)).allOK)
                calculStep = dblWithStat.val;
        }
        return grpStat;
    }

    public Fuel fuelFromName(String name) {
        Fuel f = null;
        boolean found = false;
        for (int i = 0; i < fuelList.size(); i++) {
            f = fuelList.get(i);
            if (f.name.equalsIgnoreCase(name.trim())) {
                found = true;
                break;
            }
        }
        return (found) ? f : null;
    }

    protected boolean bResultsReady = false;

    boolean busyInCalculation = false;

    public void setBusyInCalculation (boolean busy) {
        busyInCalculation = busy;
        if (!busy) {
            for (CalculationsDoneListener listener: calculationListeners)
                listener.noteCalculationsDone();
        }
    }

    public boolean isItBusyInCalculation() {
        return busyInCalculation;
    }

    public void setResultsReady(boolean bReady) {
        bResultsReady = bReady;
        freshResults = bReady;
    }

    public void resultsReady(Observations observations) {
        resultsReady(observations, DFHResult.Type.HEATSUMMARY);
    }

    public void resultsReady(Observations observations, DFHResult.Type switchDisplayTo) {
        setResultsReady(true);
        if (furnaceFor == DFHTuningParams.FurnaceFor.STRIP) {
            enableCompareMenu(true);
            enableSaveForComparison(true);
        }
        enableResultsMenu(true);
        showResultsPanel("" + switchDisplayTo);
        enableDataEntry(false);
        enableFileMenu(true);
        enableDefineMenu(true);
        if (fuelMixP != null && furnace.anyMixedFuel())
            addResult(DFHResult.Type.FUELMIX, fuelMixP);
        evaluator = null;
        pbEdit.setEnabled(true);
        pbEdit.getModel().setPressed(false);
        if (observations.isAnyThere())
            showMessage("Some observations on the results: \n" + observations, 5000);
        if (theResultsListener != null) {
            ResultsReadyListener theNowListener = theResultsListener;
            theResultsListener = null;
            theNowListener.noteResultsReady();
        }
    }

    void initPrintGroups() {
        heatBalances = new Vector<PanelAndName>();
        allTrends = new Vector<PanelAndName>();
    }

    public void addResult(DFHResult.Type type, JPanel panel) {
        ResultPanel rp;
        rp = resultPanels.get(type);
        if (rp != null)
            rp.setPanel(panel);
        rp = printPanels.get(type);
        if (rp != null) {
            rp.setPanel(panel);
            switch (type) {
                case HEATSUMMARY:
                case SECTIONWISE:
                case TOPSECTIONWISE:
                case BOTSECTIONWISE:
                    heatBalances.add(new PanelAndName(panel, "" + type));
                    break;
                case TOPtempTRENDS:
                case BOTtempTRENDS:
                case COMBItempTRENDS:
                    allTrends.add(new PanelAndName(panel, "" + type));
                    break;
                default:
                    break;
            }
            if (heatBalances.size() > 1)
                printPanels.get(DFHResult.Type.ALLBALANCES).setPanel(null);
            if (allTrends.size() > 1)
                printPanels.get(DFHResult.Type.ALLtempTRENDS).setPanel(null);
        }
    }

    //region Message functions
    public void showError(String msg) {
        showError(msg, parent());
    }

    public void showError(String msg, Window w){
        logError(msg);
        SimpleDialog.showError(w, "", msg);
        if (w != null)
            w.toFront();
    }

    public void showMessage(String msg) {
        showMessage("", msg);
    }

    public void showMessage(String title, String msg) {
        logInfo(msg);
        SimpleDialog.showMessage(parent(), title, msg);
        Window w = parent();
        if (w != null)
            w.toFront();
    }

    public boolean decide(String title, String msg) {
        return decide(title, msg, true);
    }

    public boolean decide(String title, String msg, boolean defaultOption) {
        int resp = SimpleDialog.decide(parent(), title, msg, defaultOption);
        return resp == JOptionPane.YES_OPTION;
    }

    public boolean decide(String title, String msg, int forTime) {
        return SimpleDialog.decide(this, title, msg, forTime);
    }

    public static void logError(String msg) {
        if (log != null)
            log.error("DFHeating:" + msg);
    }

    public static void logInfo(String msg) {
        if (log != null)
            log.info("DFHeating:" + msg);
    }

    void showMessage(String msg, int forTime) {
        (new TimedMessage("In DFHFurnace: FOR INFORMATION", msg, TimedMessage.INFO, parent(), forTime)).show();
    }

    protected void saveFceToFile(boolean withPerformance) {
        if (asJNLP)
            saveFceToFileJNLP(withPerformance);
        else {
            takeValuesFromUI();
            String title = "Save DFH Furnace Data" + ((withPerformance) ? " (with Performance Data)" : "");
            FileDialog fileDlg =
                    new FileDialog(mainF, title,
                            FileDialog.SAVE);
            fileDlg.setFile(profileFileName);
            fileDlg.setVisible(true);

            String bareFile = fileDlg.getFile();
            if (!(bareFile == null)) {
                int len = bareFile.length();
                if ((len < 8) || !(bareFile.substring(len - 7).equalsIgnoreCase("." + profileFileExtension))) {
                    showMessage("Adding '." + profileFileExtension + "' to file name");
                    bareFile = bareFile + "." + profileFileExtension;
                }
                String fileName = fileDlg.getDirectory() + bareFile;
                debug("Save Data file name :" + fileName);
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
    }

    protected void saveFceToFileJNLP(boolean withPerformance) {
        takeValuesFromUI();
        if (!JNLPFileHandler.saveToFile(inputDataXML(withPerformance), profileFileExtension, "furnaceProfile." + profileFileExtension))
            showError("Could not save to file!");
        parent().toFront();
    }

    protected StatusWithMessage getFceFromFileJNLP() {
        disableCompare();
        StatusWithMessage retVal = new StatusWithMessage();
        furnace.resetSections();
        try {
            FileContents fc = JNLPFileHandler.getReadFile(null, new String[]{profileFileExtension, "xls", "test"}, 0, 0);
            if (fc != null) {
                String fileName = fc.getName();
                boolean bXL = (fileName.length() > 4) && (fileName.substring(fileName.length() - 4).equalsIgnoreCase(".xls"));
                setResultsReady(false);
                setItFromTFM(false);
                furnace.resetLossAssignment();
                furnace.clearPerfBase();
                hidePerformMenu();
                debug("Data file name (JNLP):" + fileName);
                if (bXL) {
                    String fceData = fceDataFromXL(fc);
                    if (checkVersion(fceData)) {
                        retVal = takeProfileDataFromXML(fceData);
                        if ((retVal.getDataStatus() == DataStat.Status.OK)) {
//                            retVal.setInfoMessage(fileName);
                            profileFileName = fileName;
                            parent().toFront();
                            if (!onProductionLine)
                                showMessage("Fuel and Charge Material to be Selected/Checked before Calculation.");
                        } else {
                            retVal.setErrorMessage("in Furnace Data from XL file!\n" + retVal.getErrorMessage());
                            showError(retVal.getErrorMessage());
                        }
                    } else {
                        retVal.setErrorMessage("This file does not contain proper DFHFurnace data!");
                        showError(retVal.getErrorMessage());
                    }
                }
                else {
                    retVal = getFceFromFceDatFile(fc);
                    if (retVal.getDataStatus() != DataStat.Status.WithErrorMsg)
                        profileFileName = fileName;

                }
            }
        } catch (IOException e) {
            showError("facing some problem in reading data : " + e.getMessage());
            e.printStackTrace();
        }
        switchPage(DFHDisplayPageType.INPUTPAGE);
        return retVal;
    }

    protected StatusWithMessage getFceFromFile() {
        if (asJNLP)
            return getFceFromFileJNLP();
        StatusWithMessage retVal = new StatusWithMessage();
        disableCompare();
        boolean bRetVal = false;
        furnace.resetSections();
        FileDialog fileDlg =
                new FileDialog(mainF, "Read DFH Furnace Data",
                        FileDialog.LOAD);
        fileDlg.setFile("*." + profileFileExtension + "; *.xls");
        fileDlg.setVisible(true);
        String fileName = fileDlg.getFile();
        if (fileName != null) {
            boolean bXL = (fileName.length() > 4) && (fileName.substring(fileName.length() - 4).equalsIgnoreCase(".xls"));
            String filePath = fileDlg.getDirectory() + fileName;
            if (!filePath.equals("nullnull")) {
                setResultsReady(false);
                setItFromTFM(false);
                furnace.resetLossAssignment();
                furnace.clearPerfBase();
                hidePerformMenu();
                debug("Data file name :" + filePath);
                if (bXL) {
                    String fceData = fceDataFromXL(filePath);
                    if (checkVersion(fceData)) {
                        retVal = takeProfileDataFromXML(fceData);
                    } else {
                        retVal.setErrorMessage("This file does not contain proper DFHFurnace data!");
                    }
                } else {
                    retVal = getFceFromFceDatFile(filePath);
                    if (retVal.getDataStatus() != DataStat.Status.WithErrorMsg)
                        profileFileName = fileName;
                }
            }
        }
        switchPage(DFHDisplayPageType.INPUTPAGE);
        return retVal;
    }

    public boolean saveRecuToFile(String xmlStrRecu) {
        boolean retVal = false;
        String fileMsg = "# Recuperator Data saved on " + dateFormat.format(new Date()) + "\n" +
                         "# The data can be modified by Knowledgeable User. The total responsibility is his.\n" +
                         "# No heat balance check is done by the program.\n" +
                         "# the parameters 'fFBase' and 'hTaBase' are not read by the program, \n" +
                         "# and leave them as they are.\n\n\n";

        String xmlStr = fileMsg + XMLmv.putTag("Recuperator", xmlStrRecu);
        if (asJNLP) {
            if (JNLPFileHandler.saveToFile(xmlStr, "recuDat", "recuperator.recuDat"))
                retVal = true;
            else
                showError("Could not save to file!");
        }
        else {
            String title = "Save Recuperator  Data";
            FileDialog fileDlg =
                    new FileDialog(mainF, title,
                            FileDialog.SAVE);
            fileDlg.setFile("*.recuDat");
            fileDlg.setVisible(true);

            String bareFile = fileDlg.getFile();
            if (!(bareFile == null)) {
                int len = bareFile.length();
                if ((len < 9) || !(bareFile.substring(len - 8).equalsIgnoreCase(".recuDat"))) {
                    showMessage("Adding '.recuDat' to file name");
                    bareFile = bareFile + ".recuDat";
                }
                String fileName = fileDlg.getDirectory() + bareFile;
                debug("Save Recu Data file name :" + fileName);
                File f = new File(fileName);
                boolean goAhead = true;
                if (goAhead) {
                    try {
                        BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(fileName));
                        oStream.write(xmlStr.getBytes());
                        oStream.close();
                        retVal = true;
                    } catch (FileNotFoundException e) {
                        showError("File " + fileName + " NOT found!");
                    } catch (IOException e) {
                        showError("Some IO Error in writing to file " + fileName + "!");
                    }
                }
            }
        }
        parent().toFront();
        return retVal;
    }

    void loadRecuperator() {
        boolean loaded = false;
        if (asJNLP)
            loaded = loadRecuperatorJNLP();
        else {
            FileDialog fileDlg =
                    new FileDialog(mainF, "Load Recuperator Specifications",
                            FileDialog.LOAD);
            fileDlg.setFile("*.recuDat");
            fileDlg.setVisible(true);
            String fileName = fileDlg.getFile();
            if (fileName != null) {
                String filePath = fileDlg.getDirectory() + fileName;
                if (!filePath.equals("nullnull")) {
                    debug("Recu Data file name :" + filePath);
                    try {
                        BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(filePath));
                        File f = new File(filePath);
                        long len = f.length();
                        if (len > 50 && len < 1000) {
                            int iLen = (int) len;
                            byte[] data = new byte[iLen + 10];
                            if (iStream.read(data) > 50) {
                                if (furnace.setRecuSpecs(new String(data)))
                                    loaded = true;
//                                    showMessage("Recuperator loaded.\n Remember to save Furnace profile for future use");
                            }
                        } else
                            showError("File size " + len + " for " + filePath);
                    } catch (Exception e) {
                        showError("Some Problem in getting file!");
                    }
                }
            }
        }
        if (loaded)
            showMessage("Recuperator loaded.\n Remember to save Furnace profile for future use");
        else
            showError("Unable to load Recuperator Data");
     }

    boolean loadRecuperatorJNLP() {
        boolean bRetVal = false;
        try {
            FileContents fc = JNLPFileHandler.getReadFile(null, new String[]{"recuDat"}, 50, 1000);
            if (fc != null) {
                if (furnace.setRecuSpecs(new String(JNLPFileHandler.readFile(fc))))   {
//                    showMessage("Recuperator loaded");
                    bRetVal = true;
                }
            }
            else
                showError("Some problem in loading Recuperator.\n    May be the file is not the required file");

        } catch (IOException e) {
            showError("facing some problem in reading Recuperator data : " + e.getMessage());
            e.printStackTrace();
        }
        switchPage(DFHDisplayPageType.INPUTPAGE);
        return bRetVal;
    }


    protected StatusWithMessage getFceFromFceDatFile(FileContents fc) {
        StatusWithMessage retVal = new StatusWithMessage();
        try {
            long len = fc.getLength();
            if (len > 1300 && len < 10e6) {
                String header = JNLPFileHandler.readFile(fc, 200);
                if (checkVersion(header)) {
                    String fceData = JNLPFileHandler.readFile(fc);
                    if (fceData != null) {
                        retVal = takeProfileDataFromXML(fceData);
                    }
                } else {
                    retVal.setErrorMessage("This not a proper DFHFurnace data file!");
                    showError(retVal.getErrorMessage());
                }
            } else {
                retVal.setErrorMessage("File size " + len + " for " + fc.getName());
                showError(retVal.getErrorMessage());
            }
        } catch (Exception e) {
            retVal.setErrorMessage("Some Problem in getting file! : " + e.getMessage());
            showError(retVal.getErrorMessage());
        }
        return retVal;
    }

    protected StatusWithMessage getFceFromFceDatFile(String filePath) {
        StatusWithMessage retVal = new StatusWithMessage();
        try {
            BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(filePath));
            //           FileInputStream iStream = new FileInputStream(fileName);
            File f = new File(filePath);
            long len = f.length();
            if (len > 1300 && len < 10e6) {
                int iLen = (int) len;
                byte[] data = new byte[iLen + 100];
                if (iStream.read(data, 0, 200) == 200) {
                    if (checkVersion(new String(data, 0, 200))) {
                        iStream.read(data, 200, iLen - 200);
                        retVal = takeProfileDataFromXML(new String(data));
                    } else {
                        retVal.setErrorMessage("This not a proper DFHFurnace data file!");
                    }
                }
            } else {
                retVal.setErrorMessage("File size " + len + " for " + filePath);
            }
        } catch (Exception e) {
            retVal.setErrorMessage("Some Problem in getting file " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
        return retVal;
    }

    void setItFromTFM(boolean bYes) {
        mIBeamParamTFM.setEnabled(bYes);
        mILossParamTFM.setEnabled(bYes);
    }

    String fceDataFromXL(FileContents fc) {
        String data = "";
        try {
            InputStream xlInput = fc.getInputStream();
            POIFSFileSystem xlFileSystem = new POIFSFileSystem(xlInput);

            /** Create a workbook using the File System**/
            HSSFWorkbook wB = new HSSFWorkbook(xlFileSystem);

            /** Get the first sheet from workbook**/
            HSSFSheet sh = wB.getSheet("Furnace Profile");
            if (sh != null) {
                Row r = sh.getRow(0);
                Cell cell = r.getCell(0);
                if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                    if (cell.getStringCellValue().equals("mv7414")) {
                        r = sh.getRow(1);
                        cell = r.getCell(0);
                        if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC)  { // data is multi column
                            int nCols = (int)cell.getNumericCellValue();
                            for (int c = 1; c <= nCols; c++) {
                                cell = r.getCell(c);
                                if (cell.getCellType() == Cell.CELL_TYPE_STRING)
                                    data += cell.getStringCellValue();
                            }
                        }
                        else {         // kept for backward compatibility
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING)
                                data = cell.getStringCellValue();
                        }
                    }
                }
            }
            xlInput.close();
        } catch (Exception e) {
            showError("Error in DFHEating:fceDataFromXL >" + e.getMessage());
        }
        return data;
    }

    String fceDataFromXL(String filePath) {
        String data = "";
        try {
            FileInputStream xlInput = new FileInputStream(filePath);
            POIFSFileSystem xlFileSystem = new POIFSFileSystem(xlInput);

            /** Create a workbook using the File System**/
            HSSFWorkbook wB = new HSSFWorkbook(xlFileSystem);

            /** Get the first sheet from workbook**/
            HSSFSheet sh = wB.getSheet("Furnace Profile");
            if (sh != null) {
                Row r = sh.getRow(0);
                Cell cell = r.getCell(0);
                if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                    if (cell.getStringCellValue().equals("mv7414")) {
                        r = sh.getRow(1);
                        cell = r.getCell(0);
                        if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC)  { // data is multi column
                            int nCols = (int)cell.getNumericCellValue();
                            for (int c = 1; c <= nCols; c++) {
                                cell = r.getCell(c);
                                if (cell.getCellType() == Cell.CELL_TYPE_STRING)
                                    data += cell.getStringCellValue();
                            }
                        }
                        else {         // kept for backward compatibility
                            if (cell.getCellType() == Cell.CELL_TYPE_STRING)
                                data = cell.getStringCellValue();
                        }
                    }
                }
            }
            xlInput.close();
        } catch (Exception e) {
            showError("Error in DFHEating:fceDataFromXL >" + e.getMessage());
        }
        return data;
    }

    JPanel printTitleP() {
//        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Date date = new Date();
        MultiPairColPanel jp = new MultiPairColPanel("Results of DFH Furnace Calculations");
        jp.addItemPair(new JLabel("Date :"), new JLabel(dateFormat.format(date)));
        jp.addItemPair(new JLabel("Reference :"), new JLabel(reference));
        jp.addItemPair(new JLabel("Furnace Title :"), new JLabel(fceTtitle));
        jp.addItemPair(new JLabel("Customer :"), new JLabel(customer));
        return jp;
    }

    void printIt(ResultPanel rP) {
        JComponent editorPane = rP.getPanel();
        DFHResult.Type type = rP.getType();
        PrinterJob printJob = PrinterJob.getPrinterJob();
        PrintHandler target;
        int pagesN = 1;
        if (type == DFHResult.Type.ALLBALANCES || type == DFHResult.Type.ALLtempTRENDS) {
            Vector<PanelAndName> printPages;
            if (type == DFHResult.Type.ALLBALANCES) {
                printPages = heatBalances;
                pagesN = heatBalances.size();
            } else {
                printPages = allTrends;
                pagesN = allTrends.size();
            }
            target = new PrintHandler();
            PanelAndName pAndName;
            for (int p = 0; p < pagesN; p++) {
                JPanel jp = new JPanel(new BorderLayout());
                jp.setBackground(Color.WHITE);
                jp.add(printTitleP(), BorderLayout.NORTH);
                pAndName = printPages.get(p);
                jp.add(pAndName.jp, BorderLayout.CENTER);
                target.addPage(jp);
                switchPage(jp);
                if (pagesN > 1)
                    showMessage("Adding Page '" + pAndName.name + "'");
            }
        } else {
            JPanel jp = new JPanel(new BorderLayout());
            jp.add(printTitleP(), BorderLayout.NORTH);
            switchPage(jp);
            jp.add(editorPane, BorderLayout.CENTER);
            target = new PrintHandler(jp);
        }
        printJob.setPrintable(target);
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        aset.add(new PageRanges(1, pagesN));
        try {
            if (printJob.printDialog(aset)) {
                parent().toFront();
                printJob.print();
            } else {
                debug("print permission not granted");
                parent().toFront();
            }
        } catch (java.lang.SecurityException e) {
            debug("" + e);
            showError("Unable to print!");
        } catch (Exception e) {
            debug("" + e);
            showError("Unable to print!");
        }
    }

    void invalidateResults() {
        setTimeValues(0, 0, 0);
        enableResultsMenu(false);
    }

    void addInputToListener(JTextField c) {
        c.addFocusListener(inputChangeListener);
        c.addActionListener(inputChangeListener);
    }

    void addInputToListener(JComboBox c) {
        c.addFocusListener(inputChangeListener);
        c.addActionListener(inputChangeListener);
    }

    void tProfileForTFM() {
        String profStr = furnace.tProfileForTFMWithLen(); //(false);
        if (profStr.length() > 100) {
            if (asJNLP) {
                if (!JNLPFileHandler.saveToFile(profStr, "csv", "Temperature Profile for TFM.csv"))
                    showError("Could not save to file!");
            }
            else {
                FileDialog fileDlg =
                        new FileDialog(mainF, "Temperature Profile for TFM",
                                FileDialog.SAVE);
                fileDlg.setFile("Temperature Profile for TFM.csv");
                fileDlg.setVisible(true);
                String bareFile = fileDlg.getFile();
                if (bareFile != null) {
                    int len = bareFile.length();
                    if ((len < 4) || !(bareFile.substring(len - 4).equalsIgnoreCase(".csv"))) {
                        showMessage("Adding '.csv' to file name");
                        bareFile = bareFile + ".csv";
                    }
                    String fileName = fileDlg.getDirectory() + bareFile;
                    try {
                        BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(fileName));
                        oStream.write(profStr.getBytes());
                        oStream.close();
                    } catch (Exception e) {
                        showError("Some problem in file.\n" + e.getMessage());
                        return;
                    }
                }
            }
            parent().toFront();
        }
    }

    void saveComparisonToXL() {
//  create a new workbook
            Workbook wb = new HSSFWorkbook();
            int nSheet = 0;
//  create a new sheet
            ExcelStyles styles = new ExcelStyles(wb);
            Sheet sh = prepareReportWB(wb, styles);
            furnace.xlComparisonReport(sh, styles);
        if (asJNLP) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                wb.write(bos);
                byte[] bytes = bos.toByteArray();
                if (!JNLPFileHandler.saveToFile(bytes, "xls", "comparisonTable.xls"))
                    showError("Some problem in writing to comparison Table Excel file!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            FileOutputStream out = null;
            FileDialog fileDlg =
                    new FileDialog(mainF, "Saving Results Table to Excel",
                            FileDialog.SAVE);
            fileDlg.setFile("Test Results Table.xls");
            fileDlg.setVisible(true);
            String bareFile = fileDlg.getFile();
            if (bareFile != null) {
                int len = bareFile.length();
                if ((len < 4) || !(bareFile.substring(len - 4).equalsIgnoreCase(".xls"))) {
                    showMessage("Adding '.xls' to file name");
                    bareFile = bareFile + ".xls";
                }
                String fileName = fileDlg.getDirectory() + bareFile;
                try {
                    out = new FileOutputStream(fileName);
                } catch (FileNotFoundException e) {
                    showError("Some problem in file.\n" + e.getMessage());
                    return;
                }
                try {
                    wb.write(out);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    showError("Some problem with file.\n" + e.getMessage());
                }
            }
        }
        parent().toFront();
    }

    void clearComparisonTable() {
        furnace.clearComparisonTable();
        mIShowComparison.setEnabled(false);
        mISaveComparisontoXL.setEnabled(false);
        mIAppendComparisontoXL.setEnabled(false);
        mIClearComparison.setEnabled(false);
        switchPage(DFHDisplayPageType.INPUTPAGE);
    }

    void saveForComparison() {
        furnace.saveForComparison();
        mIClearComparison.setEnabled(true);
    }

    void appendToComparisonToXL() {
        if (asJNLP) {
            showError("Not ready to append to comparison table file yet!");
        }
        else {
            FileDialog fileDlg =
                    new FileDialog(mainF, "Appending Results Table to Excel",
                            FileDialog.LOAD);
            fileDlg.setFile("*.xls");
            fileDlg.setVisible(true);
            String bareFile = fileDlg.getFile();
            if (bareFile != null) {
                boolean bXL = (bareFile.length() > 4) && (bareFile.substring(bareFile.length() - 4).equalsIgnoreCase(".xls"));
                if (bXL) {
                    String filePath = fileDlg.getDirectory() + bareFile;
                    if (!filePath.equals("nullnull")) {
                        FileInputStream xlInput = null;
                        HSSFWorkbook wB = null;
                        try {
                            xlInput = new FileInputStream(filePath);
                            /** Create a workbook using the File System**/
                            wB = new HSSFWorkbook(xlInput);
                        } catch (Exception e) {
                            showError("Some problem in Reading/saving Report file\n" + e.getMessage());
                        }

                        /** Get the first sheet from workbook**/
                        HSSFSheet sh = wB.getSheet("Results Table");
                        if (sh != null) {
                            ExcelStyles styles = new ExcelStyles(wB);
                            try {
                                furnace.xlComparisonReport(sh, styles);
                                xlInput.close();
                                FileOutputStream outFile = new FileOutputStream(filePath);
                                wB.write(outFile);
                                outFile.close();
                            } catch (FileNotFoundException e) {
                                showError("Some problem in Reading/saving Report file\n" + e.getMessage());
                            } catch (IOException e) {
                                showError("Some problem in Reading/saving Report file\n" + e.getMessage());
                            }
                        }
                    }
                } else
                    showMessage("Choose *.xls file");
            }
        }
    }

    Sheet prepareReportWB(Workbook wb, ExcelStyles styles) {
        Sheet sheet = wb.createSheet("Results Table");
        Row r = sheet.createRow(0);
        Cell c = r.createCell(0);
        c.setCellStyle(styles.csHeader1);
        c.setCellValue("Results Comparison Report from DFHFurnace");
        r = sheet.createRow(1);
        c = r.createCell(0);
        c.setCellValue("Last Entry Row");
        c = r.createCell(2);
        c.setCellValue(0);
        return sheet;
    }

    void createPerformBase() {
        if (furnace.createPerfBase())  {
            mIClearPerfBase.setEnabled(true);
            mICreatePerfBase.setEnabled(false);
            mIShowPerfBase.setEnabled(true);
        }
    }

    void addToPerformBase() {
        if (furnace.addToPerfBase())
            freshResults = false;
    }

    public void clearPerformBase() {
        if (decide("Performance Base", "Do you want to DELETE ALL Performance Data?")) {
            furnace.clearPerfBase();
            mIAddToPerfBase.setEnabled(false);
            mIShowPerfBase.setEnabled(false);
        }
    }

    void excelResultsFile() {
        //  create a new workbook
        Workbook wb = new HSSFWorkbook();
        int nSheet = 0;
        //  create a new sheet
        ExcelStyles styles = new ExcelStyles(wb);
        wb.createSheet("Basic Data");
        xlFceBasicData(wb.getSheetAt(nSheet), styles);
        nSheet++;
        wb.createSheet("Heat Summary");
        furnace.xlHeatSummary(wb.getSheetAt(nSheet), styles);
        nSheet++;
        wb.createSheet(furnace.topBotName(false) + "Sec Summary");
        furnace.xlSecSummary(wb.getSheetAt(nSheet), styles, false);
        if (furnace.bTopBot) {
            nSheet++;
            wb.createSheet("Bot Sec Summary");
            furnace.xlSecSummary(wb.getSheetAt(nSheet), styles, true);
        }
        nSheet++;
        wb.createSheet("Loss Details");
        furnace.xlLossDetails(wb.getSheetAt(nSheet), styles);

        nSheet++;
        if (bAirHeatedByRecu || bFuelHeatedByRecu) {
            wb.createSheet("Recu Balance");
            furnace.xlRecuSummary(wb.getSheetAt(nSheet), styles);
            nSheet++;
        }
        wb.createSheet("Furnace Fuel Summary");
        furnace.xlFuelSummary(wb.getSheetAt(nSheet), styles);
        nSheet++;
        wb.createSheet(furnace.topBotName(false) + "Sec Fuel Summary");
        furnace.xlSecFuelSummary(wb.getSheetAt(nSheet), styles, false);
        if (bTopBot) {
            nSheet++;
            wb.createSheet("Bottom Sec Fuel Summary");
            furnace.xlSecFuelSummary(wb.getSheetAt(nSheet), styles, true);
        }
        nSheet++;
        wb.createSheet("Fuels Details");
        furnace.xlUsedFuels(wb.getSheetAt(nSheet), styles);
        nSheet++;
        wb.createSheet(furnace.topBotName(false) + "Temp Profile");
        furnace.xlTempProfile(wb.getSheetAt(nSheet), styles, false);
        if (furnace.bTopBot) {
            nSheet++;
            wb.createSheet("Bot Temp Profile");
            furnace.xlTempProfile(wb.getSheetAt(nSheet), styles, true);
        }
        nSheet++;
        wb.createSheet("Furnace Profile");
        wb.setSheetHidden(nSheet, true);
        Sheet sh = wb.getSheetAt(nSheet);
        xlFceProfile(sh, styles);  // performance is saved here

        nSheet++;
        //  create a new file
        if (asJNLP) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                wb.write(bos);
                byte[] bytes = bos.toByteArray();
                if (!JNLPFileHandler.saveToFile(bytes, "xls", "furnaceResults.xls"))
                    showError("Some problem in writing to Excel file!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            FileOutputStream out = null;
            FileDialog fileDlg =
                    new FileDialog(mainF, "Saving Results to Excel",
                            FileDialog.SAVE);
            fileDlg.setFile("Test workbook from Java.xls");
            fileDlg.setVisible(true);
            String bareFile = fileDlg.getFile();
            if (bareFile != null) {
                int len = bareFile.length();
                if ((len < 4) || !(bareFile.substring(len - 4).equalsIgnoreCase(".xls"))) {
                    showMessage("Adding '.xls' to file name");
                    bareFile = bareFile + ".xls";
                }
                String fileName = fileDlg.getDirectory() + bareFile;
                try {
                    out = new FileOutputStream(fileName);
                } catch (FileNotFoundException e) {
                    showError("Some problem in file.\n" + e.getMessage());
                    return;
                }
                try {
                    wb.write(out);
                    out.close();
                    furnace.performanceIsSaved();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    showError("Some problem with file.\n" + e.getMessage());
                }
            }
        }
        parent().toFront();
    }

    protected void modifyJTextEdit() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addPropertyChangeListener("permanentFocusOwner", new PropertyChangeListener() {

                    public void propertyChange(final PropertyChangeEvent e) {

                        if (e.getOldValue() instanceof JTextField) {
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    JTextField oldTextField = (JTextField) e.getOldValue();
                                    oldTextField.setSelectionStart(0);
                                    oldTextField.setSelectionEnd(0);
                                }
                            });

                        }

                        if (e.getNewValue() instanceof JTextField) {
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    JTextField textField = (JTextField) e.getNewValue();
                                    textField.selectAll();
                                }
                            });

                        }
                    }
                });
    }

    void saveFuelSpecs() {
        if (asJNLP)
            saveFuelSpecsJNLP();
        else {
            String fuelSpecsStr = "# Fuel specifications saved on " + dateFormat.format(new Date()) + "\n\n" +
                    fuelSpecsInXML();

            FileOutputStream out = null;
            FileDialog fileDlg =
                    new FileDialog(mainF, "Saving Fuel Specifications to file",
                            FileDialog.SAVE);
            fileDlg.setFile("FuelSpecifications.dfhSpecs");
            fileDlg.setVisible(true);
            String bareFile = fileDlg.getFile();
            if (bareFile != null) {
                int len = bareFile.length();
                if ((len < 9) || !(bareFile.substring(len - 9).equalsIgnoreCase(".dfhSpecs"))) {
                    showMessage("Adding '.' to file name");
                    bareFile = bareFile + ".dfhSpecs";
                }
                String fileName = fileDlg.getDirectory() + bareFile;
                try {
                    BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(fileName));
                    oStream.write(fuelSpecsStr.getBytes());
                    oStream.close();
                } catch (Exception e) {
                    showError("Some problem in saving Fuel specs.\n" + e.getMessage());
                    return;
                }
            }
        }
    }

    void saveFuelSpecsJNLP() {
        if (cbFuel.confirmToSave(parent())) {
            String xmlStr = fuelSpecsInXMLasJNLP();
            if (!JNLPFileHandler.saveToFile(
                    "# Fuel specifications saved on " + dateFormat.format(new Date()) + "\n\n" + xmlStr,
                    "dfhSpecs", "FuelSpecifications.dfhSpecs"))
                showError("Could not save to file!");
            parent().toFront();
        }
    }

    String fuelSpecsInXML() {
        String xmlStr = XMLmv.putTag("nFuels", fuelList.size()) + "\n";
        int fNum = 0;
        for (Fuel f:fuelList) {
            fNum++;
            xmlStr += XMLmv.putTag("F" + ("" + fNum).trim(), "\n" + f.fuelSpecInXML()) + "\n";
        }
        return xmlStr;
    }

    String fuelSpecsInXMLasJNLP() {
         int nF = 0;
         for (Fuel f:fuelList) {
             if (f instanceof JSPObject)
                 if (((JSPObject)f).isDataCollected())
                     nF++;
         }
         String xmlStr = XMLmv.putTag("nFuels", nF) + "\n";
         int fNum = 0;
         for (Fuel f:fuelList) {
             if (f instanceof JSPObject)
                 if (((JSPObject)f).isDataCollected()) {
                     fNum++;
                     xmlStr += XMLmv.putTag("F" + ("" + fNum).trim(), "\n" + f.fuelSpecInXML()) + "\n";
                 }
         }
         return xmlStr;
    }

    void clearFuelData() {
        fuelList.clear();
        cbFuel.removeAll();
    }

    protected boolean fuelSpecsFromFile(String filePath) {
        boolean bRetVal = false;
        clearFuelData();
        try {
            BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(filePath));
            File f = new File(filePath);
            long len = f.length();
            if (len > 20 && len < 50000) {
                int iLen = (int)len;
                byte[] data = new byte[iLen + 100];
                try {
                    if (iStream.read(data) == len) {
                    String stat = takeFuelSpecsFromXML(new String(data));
                    if (stat.equals("OK"))
                        bRetVal = true;
                    else
                        showMessage("Some problem reading Fuel Specification file", 3000);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
        }
        return bRetVal;
    }

    String takeFuelSpecsFromXML(String xmlStr) {
        ValAndPos vp;
        String retVal = "OK";
        vp = XMLmv.getTag(xmlStr, "nFuels", 0);
        int nFuels = Integer.valueOf(vp.val);
        String tempName;
        if (nFuels > 0) {
            retVal = "OK";
            for (int fNum = 1; fNum <= nFuels; fNum++)   {
                tempName = "F" + ("" + fNum).trim();
                vp = XMLmv.getTag(xmlStr, tempName, vp.endPos);
                try {
                    Fuel f = new Fuel(vp.val, true);
                    fuelList.add(f);
                } catch (Exception e) {
                    retVal = "Some problem in getting Fuel Specs " + tempName + "\n" +
                            e.getMessage();
                    break;
                }
            }
        }
        return retVal;
    }

    void clearChMaterialData() {
        vChMaterial.clear();
    }

    void saveSteelSpecs() {
        if (asJNLP)
            saveSteelSpecsJNLP();
        else {
            String xmlStr = chMaterialSpecsInXML();
            if (xmlStr.length() > 25) {
                FileOutputStream out = null;
                FileDialog fileDlg =
                        new FileDialog(mainF, "Saving Charge Material Specifications to file",
                                FileDialog.SAVE);
                fileDlg.setFile("ChMaterialSpecifications.dfhSpecs");
                fileDlg.setVisible(true);
                String bareFile = fileDlg.getFile();
                if (bareFile != null) {
                    int len = bareFile.length();
                    if ((len < 9) || !(bareFile.substring(len - 9).equalsIgnoreCase(".dfhSpecs"))) {
                        showMessage("Adding '.' to file name");
                        bareFile = bareFile + ".dfhSpecs";
                    }
                    String fileName = fileDlg.getDirectory() + bareFile;
                    try {
                        BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(fileName));
                        oStream.write(("# Charge Material specifications saved on " + dateFormat.format(new Date()) + "\n\n").getBytes());
                        oStream.write(xmlStr.getBytes());
                        oStream.close();
                    } catch (Exception e) {
                        showError("Some problem in saving Charge Material specs.\n" + e.getMessage());
                        return;
                    }
                }
            }
        }
    }

    void saveSteelSpecsJNLP() {
        if (cbChMaterial.confirmToSave(parent())) {
            String xmlStr = chMaterialSpecsInXMLasJNLP();
            if (!JNLPFileHandler.saveToFile(
                    "# Charge Material specifications saved on " + dateFormat.format(new Date()) + "\n\n" + xmlStr,
                    "dfhSpecs", "ChMaterialSpecifications.dfhSpecs"))
                showError("Could not save to file!");
            parent().toFront();
        }
    }


    String chMaterialSpecsInXML() {
        String xmlStr = XMLmv.putTag("nCharge", vChMaterial.size()) + "\n";
        int cNum = 0;
        for (ChMaterial chM:vChMaterial) {
            cNum++;
            xmlStr += XMLmv.putTag("Ch" + ("" + cNum).trim(), "\n" + chM.materialSpecInXML()) + "\n";
        }
        return xmlStr;
    }

    String chMaterialSpecsInXMLasJNLP() {
        int nCharge = 0;
        for (ChMaterial chM:vChMaterial) {
            if (chM instanceof JSPObject)
                if (((JSPObject)chM).isDataCollected())
                    nCharge++;
        }
        String xmlStr = XMLmv.putTag("nCharge", nCharge) + "\n";
        int cNum = 0;
        for (ChMaterial chM:vChMaterial) {
            if (chM instanceof JSPObject)
                if (((JSPObject)chM).isDataCollected()) {
                    cNum++;
                    xmlStr += XMLmv.putTag("Ch" + ("" + cNum).trim(), "\n" + chM.materialSpecInXML()) + "\n";
                }
        }
        return xmlStr;
    }

    protected boolean chMaterialSpecsFromFile(String filePath) {
        boolean bRetVal = false;
        clearChMaterialData();
        try {
            BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(filePath));
            File f = new File(filePath);
            long len = f.length();
            if (len > 20 && len < 50000) {
                int iLen = (int)len;
                byte[] data = new byte[iLen + 100];
                try {
                    if (iStream.read(data) == len) {
                    String stat = takeChMaterialSpecsFromXML(new String(data));
                    if (stat.equals("OK"))
                        bRetVal = true;
                    else
                        showMessage("Some problem reading Charge Material Specification file", 3000);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
        }
        return bRetVal;
    }

    String takeChMaterialSpecsFromXML(String xmlStr) {
        ValAndPos vp;
        String retVal = "OK";
        vp = XMLmv.getTag(xmlStr, "nCharge", 0);
        int nCharge = Integer.valueOf(vp.val);
        String tempName;
        if (nCharge > 0) {
            retVal = "OK";
            for (int cNum = 1; cNum <= nCharge; cNum++)   {
                tempName = "Ch" + ("" + cNum).trim();
                vp = XMLmv.getTag(xmlStr, tempName, vp.endPos);
                try {
                    ChMaterial chM = new ChMaterial(vp.val);
                    vChMaterial.add(chM);
                } catch (Exception e) {
                    retVal = "Some problem in getting Charge Material Specs " + tempName + "\n" +
                            e.getMessage();
                    break;
                }
            }
        }
        return retVal;
    }

    public void edited() {   // TODO 20160509 to track profile edit, to be setup
        bProfileEdited = true;
    }

    protected enum DFHDisplayPageType {
        INPUTPAGE, OPPAGE, FUELMIX, REGENSTUDY, TUNINGPAGE, PROGRESSPAGE, BEAMSPAGE, LOSSPARAMSTFM,
        PERFOMANCELIST, COMPAREPANEL
    }

    class PanelAndName {
        JComponent jp;
        String name;

        PanelAndName(JComponent jp, String name) {
            this.jp = jp;
            this.name = name;
        }
    }

    class ResultPanel {
        DFHResult.Type type;
        JMenuItem mI;
        JPanel panel;

        ResultPanel(DFHResult.Type type, JMenu menu, ActionListener li) {
            this.type = type;
            mI = new JMenuItem("" + type);
            mI.addActionListener(li);
            menu.add(mI);
            mI.setEnabled(false);
        }

        ResultPanel(DFHResult.Type type, ActionListener li) {
            this.type = type;
            mI = new JMenuItem("" + type);
            mI.addActionListener(li);
            mI.setEnabled(false);
        }

        void removePanel() {
            mI.setEnabled(false);
        }

        JMenuItem getMenuItem() {
            return mI;
        }

        JPanel getPanel() {
            return panel;
        }

        void setPanel(JPanel panel) {
            this.panel = panel;
            mI.setEnabled(true);
        }

        DFHResult.Type getType() {
            return type;
        }
    }

    class WinListener implements WindowListener {
        public void windowOpened(WindowEvent e) {
        }

        public void windowClosing(WindowEvent e) {
            debug("mainF CLOSING");
            checkAndClose(true);
//            destroy();
//            if (asApplication)
//                System.exit(0);
//            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void windowClosed(WindowEvent e) {
        }

        public void windowIconified(WindowEvent e) {
        }

        public void windowDeiconified(WindowEvent e) {
        }

        public void windowActivated(WindowEvent e) {
        }

        public void windowDeactivated(WindowEvent e) {
        }
    }

    class MenuActions implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            menuBlk:
            {
                if (src == mIExit) {
                    checkAndClose(true);
                    break menuBlk;
                }
                if (src ==  mISaveToXL) {
                    excelResultsFile();
                    break menuBlk;
                }

                if (src == mISaveForTFM) {
                    tProfileForTFM();
                    break menuBlk;
                }
                if (src == mISaveFceProfile) {
                    Component lastShown = slate.getViewport().getView();
                    saveFceToFile(true);
                    parent().toFront();
                    slate.setViewportView(lastShown);
                    break menuBlk;
                }


                if (src == mIGetFceProfile) {
                    boolean goAhead = true;
                    if (furnace.isPerformanceToBeSaved()) {
                        goAhead = decide("Unsaved Performance Data", "Some Performance data have been collected\n" +
                                    "Do you want to ABANDON them and load a new furnace ?");
                    }
                    if (goAhead) {
                        pbEdit.doClick();
                        StatusWithMessage profileStatMsg = getFceFromFile();
                        DataStat.Status dataStat = profileStatMsg.getDataStatus();
                        if (dataStat != DataStat.Status.WithErrorMsg) {
                            parent().toFront();
                            if (dataStat == DataStat.Status.WithInfoMsg)
                                showMessage(profileStatMsg.getInfoMessage());
                            switchPage(DFHDisplayPageType.INPUTPAGE);
                            enableDataEntry(true);
                        }
                        else
                            showError(profileStatMsg.getErrorMessage());
                    }
                    break menuBlk;
                }
                if (src == mILoadRecuSpecs) {
                    loadRecuperator();
                    break menuBlk;
                }
                if (src == mISaveFuelSpecs) {
                    saveFuelSpecs();
                    break menuBlk;
                }

                if (src == mISaveSteelSpecs) {
                    saveSteelSpecs();
                    break menuBlk;
                }

                if (src == mIInputData) {
                    switchPage(DFHDisplayPageType.INPUTPAGE);
                    break menuBlk;
                }
                if (src == mIOpData) {
                    switchPage(DFHDisplayPageType.OPPAGE);
                    break menuBlk;
                }

                if (src == mIDefineRecuperator) {
                    furnace.defineAirRecuperator();
                    break menuBlk;
                }
                if (src == mIRecuPerformace) {
                    furnace.getAirRecuPerformance();
                    break menuBlk;
                }
                if (src == mITuningParams) {
                    switchPage(DFHDisplayPageType.TUNINGPAGE);
                    break menuBlk;
                }
                if (src == mIBeamParamTFM) {
                    switchPage(DFHDisplayPageType.BEAMSPAGE);
                    break menuBlk;
                }
                if (src == mILossParamTFM) {
                    switchPage(DFHDisplayPageType.LOSSPARAMSTFM);
                    break menuBlk;
                }
                if (src == mICreateFuelMix) {
                    switchPage(DFHDisplayPageType.FUELMIX);
                    break menuBlk;
                }
                if (src == mIRegenBurnerStudy)
                    switchPage(DFHDisplayPageType.REGENSTUDY);
                break menuBlk;
            }
        } // actionPerformed
    } // class MenuActions

    class CompareMenuListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (src == mISaveComparison)
                saveForComparison();
            if (src == mIShowComparison) {
                switchPage(DFHDisplayPageType.COMPAREPANEL);
//                switchPage(furnace.getComparePanel());
            }
            if (src == mISaveComparisontoXL)
                saveComparisonToXL();
            if (src == mIAppendComparisontoXL)
                appendToComparisonToXL();
            if (src == mIClearComparison)
                clearComparisonTable();
        }
    }

    class PerformListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (src == mICreatePerfBase) {
                createPerformBase();
            }
            if (src == mIAddToPerfBase) {
                addToPerformBase();
            }
            if (src == mIShowPerfBase) {
                switchPage(DFHDisplayPageType.PERFOMANCELIST);
            }
            if (src == mIClearPerfBase) {
                clearPerformBase();
            }
            if (src == mISetPerfTablelimits) {
                tuningParams.getPerfTableSettings(mISetPerfTablelimits);
            }
        }
    }

    class ResultsMenuActions implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            showResultsPanel(command);
        } // actionPerformed
    } // class ResultsMenuActions

    class PrintMenuActions implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            printResultsPanel(command);
        } // actionPerformed
    } // class ResultsMenuActions

    class LossNameChangeListener implements ActionListener, FocusListener {
        public void focusGained(FocusEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void focusLost(FocusEvent e) {
            if (canNotify())
                adjustForLossNameChange();
        }

        public void actionPerformed(ActionEvent e) {
            if (canNotify())
                adjustForLossNameChange();
        }
    }

    class LengthChangeListener implements ActionListener, FocusListener {
        public void focusGained(FocusEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void focusLost(FocusEvent e) {
            Object src = e.getSource();
            if (src instanceof JTextComponent) {
                JTextComponent jText = (JTextComponent) src;
                if (jText.isEditable())
                    adjustForLengthChange();
            }
        }

        public void actionPerformed(ActionEvent e) {
            adjustForLengthChange();
        }

    }

    public class InputChangeListener implements ActionListener, FocusListener {
        public void focusGained(FocusEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void focusLost(FocusEvent e) {
//            invalidateResults();
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == cbChType) {
                setChargeSizeChoice();
            }
            invalidateResults();
        }
    }

    public class PrintHandler implements Printable {
        Vector<JComponent> pages;

        public PrintHandler() {
            pages = new Vector<JComponent>();
        }

        public PrintHandler(JComponent editorPane) {
            this();
            addPage(editorPane);
        }

        public int addPage(JComponent addComponent) {
            pages.add(addComponent);
            return pages.size();
        }

        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
            if (pageIndex >= pages.size())
                return Printable.NO_SUCH_PAGE;
            JComponent toPrint = pages.get(pageIndex);
            Graphics2D g2 = (Graphics2D) graphics;
            double imableX = pageFormat.getImageableX();
            double imableY = pageFormat.getImageableY();
            g2.translate((int) imableX, (int) imableY);

            Dimension size = toPrint.getSize();
            g2.rotate(-Math.PI / 2);
            double pageWidth = pageFormat.getImageableWidth(); // Page width
            double pageHeight = pageFormat.getImageableHeight(); // Page height
            g2.translate(-(int) (pageHeight), 0);
            if (size.width > pageHeight) {
                double factor = pageHeight / size.width; // How much to scale
                g2.scale(factor, factor); // Adjust coordinate system
                pageHeight /= factor; // Adjust page size up
                pageWidth /= factor;
            }
            if (size.height > pageWidth) { // Do the same thing for height
                double factor = pageWidth / size.height;
                g2.scale(factor, factor);
            }
            RepaintManager currentManager =
                    RepaintManager.currentManager(toPrint);
            currentManager.setDoubleBufferingEnabled(false);

            toPrint.print(graphics);
            currentManager.setDoubleBufferingEnabled(true);

            return Printable.PAGE_EXISTS;
        }
    }

//    protected static String readInput(boolean useCmdSequence) {
//    		// You can provide "commands" already from the command line, in which
//    		// case they will be kept in cmdSequence
//    		BufferedReader stdin = new BufferedReader(new InputStreamReader(
//    				System.in));
//    		String s = null;
//    		do
//    			try {
//    				s = stdin.readLine();
//    			} catch (IOException e) {
//    				e.printStackTrace();
//    			}
//    		while ((s == null) || (s.length() == 0));
//    		return s;
//    	}

    static protected boolean parseCmdLineArgs(String[] args) {
        boolean retVal = true;
        CommandLineArgs cmdArg;
        for (int a = 0; a < args.length; a++) {
            cmdArg = CommandLineArgs.getEnum(args[a]);
            if (cmdArg != null)
                switch(cmdArg) {
                    case ALLOWSPECSSAVE:
                        enableSpecsSave = true;
                        break;
                    case JNLP:
                        asJNLP = true;
                        jspConnection = new JSPConnection();
                        break;
//                    case L2CONFURATOR:
//                        bL2Configurator = true;
//                        break;
                    case DEBUGMSG:
                        showDebugMessages = true;
                        break;
                }
        }
        return retVal;
    }

    public static void main(String[] args) {
//        PropertyConfigurator.configureAndWatch(DFHeating.class
//                .getResource("log.properties").getFile(), 5000);
        if (DFHeating.parseCmdLineArgs(args)) {
            DFHeating trHeat = new DFHeating(true);
        }
    }

}








