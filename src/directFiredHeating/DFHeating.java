package directFiredHeating;

import FceElements.RegenBurner;
import basic.*;
import display.*;
import mvUtils.display.*;
import mvUtils.mvXML.DoubleWithErrStat;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLgroupStat;
import mvUtils.mvXML.XMLmv;
import mvUtils.display.FramedPanel;
import mvUtils.math.XYArray;
//import netscape.javascript.JSException;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.*;
//import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import performance.stripFce.Performance;

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
public class DFHeating extends JApplet implements InputControl {

    static public enum CommandLineArgs {
        ALLOWCHANGES("-allowChanges"),
        ONTEST("-onTest"),
        ALLOWSPECSSAVE("-allowSpecsSave"),
        ALLOWSPECSREAD("-allowSpecsRead"),
        NOTLEVEL2("-notLevel2");

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


    static public enum HeatingMode {
        TOPONLY("TOP FIRED"),
        TOPBOT("TOP AND BOTTOM FIRED"),
        TOPBOTSTRIP("STRIP - TOP and BOTTOM");
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

    static public JFrame mainF; // = new JFrame("DFH Furnace");
    static public Vector<Fuel> fuelList = new Vector<Fuel>();
    static public Vector<ChMaterial> vChMaterial; // = new Vector<ChMaterial>();
    static boolean enableSpecsSave = false;
    public static Logger log;
    static boolean onTest = false;

    protected String testTitle = "";
    boolean fceFor1stSwitch = true;
    public DFHFurnace furnace;
//    public Level2Furnace furnaceLevel2;
    protected String releaseDate = "20150420AM";
    protected String DFHversion = "DFHeating Version 001";
    public DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    boolean canNotify = true;
    JSObject win;
    String header;
    boolean itsON = false;
    JPanel mainFrame;
    String reference = "Reference", fceTtitle = "Furnace", customer = "Customer";
    double width = 10, excessAir = 0.1;
    public HeatingMode heatingMode;
    boolean bTopBot = true;
    boolean bAddTopSoak = true;
    Fuel commFuel;
    FuelFiring commFuelFiring;
    String nlSpace = ErrorStatAndMsg.nlSpace;
    Hashtable<DFHResult.Type, ResultPanel> resultPanels, printPanels;
    protected DFHTuningParams.ForProcess proc = DFHTuningParams.ForProcess.BILLETS;
    protected DFHTuningParams tuningParams;
    JTextField tfReference, tfFceTitle, tfCustomer;
    NumberTextField ntfWidth;
    JComboBox cbFceFor;
//    JComboBox cbHeatingType;
    JComboBox<HeatingMode> cbHeatingMode;
    JComboBox cbFuel;
    NumberTextField tfExcessAir;
    protected LossNameChangeListener lNameListener = new LossNameChangeListener();
    InputChangeListener inputChangeListener = new InputChangeListener();
    RegenBurner regenBurnerStudy;
    Locale locale;
    public boolean asApplication = false;
    boolean bDataEntryON = true;
    JScrollPane slate = new JScrollPane();
    JPanel opPage;
    JPanel inpPage;
    boolean bAllowSecFuel = false;
    Component lastPageShown = null;
    JPanel fuelMixP;
    GridBagConstraints gbcChDatLoc;
    protected JMenu fileMenu;
    JMenu inputMenu;
    JMenu resultsMenu;
    JMenu printMenu;
    JMenu statMenu;
    JMenuItem progressP;
    JButton pbEdit;
    JMenuItem beamParamTFM, lossParamTFM;
    JMenu compareResults;
    JMenuItem saveComparisontoXL, appendComparisontoXL;
    JMenuItem showComparison;
    JMenuItem saveComparison;
    JMenuItem clearComparison;
    String inputDataforTesting = "";
    JMenuItem saveToXL;

    JMenuItem saveForTFM, saveForFE;

    JMenuItem saveFuelSpecs;
    JMenuItem saveSteelSpecs;

    Vector<PanelAndName> heatBalances, allTrends;
    FramedPanel lossPanel;
    GridBagConstraints gbcLoss;
    JScrollPane lossScroll;
    JButton addButton;
    FramedPanel rowHead;
    JScrollPane detScroll;
    NumberLabel lbTopLen;
    NumberLabel lbBotLen;
    FramedPanel titleAndFceCommon;
    MultiPairColPanel mpTitlePanel;
    MultiPairColPanel mpFceCommDataPanel;
    double chWidth = 1.2, chThickness = 0.2, chLength = 9, chDiameter = 0.2;
    protected JComboBox cbChType;
    protected int nChargeRows = 1;
    ChMaterial selChMaterial;
    protected NumberTextField tfChWidth, tfChThickness, tfChLength, tfChDiameter;
    protected JComboBox cbChMaterial;
    MultiPairColPanel mpChargeData;
    JLabel labChWidth, labChLength;
    protected double bottShadow, chPitch = 1.3, tph = 200;
    protected double entryTemp = 30, exitTemp = 1200, deltaTemp = 25;
    protected double exitZoneFceTemp = 1050; // for strip heating
    protected double minExitZoneFceTemp = 900; // for strip heating
    protected NumberTextField tfBottShadow, tfChPitch, tfChRows, tfProduction;
    protected NumberTextField tfEntryTemp, tfExitTemp, tfDeltaTemp;
    protected NumberTextField tfExitZoneFceTemp;
    protected NumberTextField tfMinExitZoneFceTemp;
    LengthChangeListener lengthListener = new LengthChangeListener();
    MultiPairColPanel mpChInFce;
    NumberTextField tfTotTime, tfSpTime, tfSpeed;
    double calculStep = 1.0;
    public double ambTemp = 30, airTemp = 500, fuelTemp = 30;
    protected NumberTextField tfCalculStep, tfAmbTemp, tfAirTemp, tfFuelTemp;
    JButton pbCalculate;
    MultiPairColPanel mpCalcul;
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
    ProductionData production;
    FceEvaluator evaluator;
    static protected boolean onProductionLine = false;

    public DFHeating() {
        debug("Release " + releaseDate);
        locale = Locale.getDefault(); // creates Locale class object by getting the default locale.
        debug("Locale is " + locale);
        // Italian
//        debug("ITA release 10.01 20130315");
//        locale = Locale.getDefault(); // creates Locale class object by getting the default locale.
//        locale = Locale.ITALY;
//        Locale.setDefault(locale);
//        debug("Locale is " + locale);
    }

    public DFHeating(boolean asApplication, boolean onProductionLine) {
        this();
        this.asApplication = asApplication;
        this.onProductionLine = onProductionLine;
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
        Font oldLabelFont = UIManager.getFont("Label.font");
        UIManager.put("Label.font", oldLabelFont.deriveFont(Font.PLAIN));
        oldLabelFont = UIManager.getFont("ComboBox.font");
        UIManager.put("ComboBox.font", oldLabelFont.deriveFont(Font.PLAIN + Font.ITALIC));
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
            if (log == null) {
                log = Logger.getLogger(DFHeating.class);
                // Load Log4j configurations from external file
            }
            mainF.setTitle("DFH Furnace Application "+ releaseDate + testTitle);
        }

        tuningParams = new DFHTuningParams(this, onProductionLine, 1, 5, 30, 1.12, 1, false, false);
        debug("Creating new DFHFurnace");
        furnace = new DFHFurnace(this, bTopBot, bAddTopSoak, lNameListener);
        debug("Created furnace");
        furnace.setTuningParams(tuningParams);
        if (onTest || asApplication) {
            createUIs();
            setTestData();
            displayIt();
        } else {
            try {
                win = JSObject.getWindow(this);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                win = null;
            }
            Object o;
            o = win.eval("enableSpecsSave()");
            enableSpecsSave = (o != null) && o.equals("1");
            createUIs();
            debug("Created UI");
//            testFunctions();
            setTestData();
            debug("did setTestData()");
            o = win.eval("getData()");
            debug("got Data from aspx");
        }
        fuelMixP = Fuel.mixedFuelPanel(this, fuelList);
        regenBurnerStudy = new RegenBurner(fuelList, this);
        info("DFHeating inited");
        enableDataEdit();
    }

    protected void createUIs() {
        if (!itsON) {
            mainF.addWindowListener(new winListener());
            setMenuOptions();
            inpPage = inputPage();
            opPage = OperationPage();
            slate.setSize(new Dimension(900, 750));
            slate.setViewportView(inpPage);
            mainF.add(slate, BorderLayout.CENTER);
            mainF.setLocation(20, 10);
            mainF.pack();
//            switchPage(2);
            switchPage(InputType.INPUTPAGE);
            debug("switched to INPUTPAGE");
            cbFceFor.setSelectedItem(proc);
        }
    }

    void enableDataEntry(boolean ena) {
        if (ena)
            furnace.resetSections();
        tfReference.setEditable(ena && !onProductionLine);
        tfFceTitle.setEditable(ena && !onProductionLine);
        tfCustomer.setEditable(ena && !onProductionLine);
        ntfWidth.setEditable(ena && !onProductionLine);
        tfExcessAir.setEditable(ena);

        cbChType.setEnabled(ena && !onProductionLine);
        tfChLength.setEditable(ena);
        tfChWidth.setEditable(ena);
        tfChDiameter.setEditable(ena);
        tfChThickness.setEditable(ena);
        tfBottShadow.setEditable(ena);
        tfChPitch.setEditable(ena);
        tfChRows.setEditable(ena);
        tfProduction.setEditable(ena);
        tfEntryTemp.setEditable(ena);
        tfExitTemp.setEditable(ena);
        tfDeltaTemp.setEditable(ena);
        tfAmbTemp.setEditable(ena);
        tfAirTemp.setEditable(ena);
        tfCalculStep.setEditable(ena && !onProductionLine);

        cbFceFor.setEnabled(ena && !onProductionLine);
//        cbHeatingType.setEnabled(ena && !onProductionLine);
        cbHeatingMode.setEnabled(ena && !onProductionLine);
        cbFuel.setEnabled(ena && !onProductionLine);
        cbChMaterial.setEnabled(ena);

//        if (tfFuelTemp.isEnabled())
        tfFuelTemp.setEditable(ena);

        ntdeltaTAirFromRecu.setEditable(ena);
        ntDeltaTFlue.setEditable(ena);
        ntMaxFlueTatRecu.setEditable(ena && !onProductionLine);
        ntDeltaTFuelFromRecu.setEditable(ena);
        cBAirHeatByRecu.setEnabled(ena && !onProductionLine);
        cBFuelHeatByRecu.setEnabled(ena && !(commFuel != null && commFuel.bMixedFuel) && !onProductionLine);

        if (ena && cBAirHeatByRecu.isSelected() && cBFuelHeatByRecu.isSelected())
            cBAirAfterFuel.setEnabled(ena && !onProductionLine);
        else
            cBAirAfterFuel.setEnabled(false && !onProductionLine);
        if (ena)
            setTimeValues(0, 0, 0);
        furnace.enableDataEntry(ena && !onProductionLine);

        pbCalculate.setEnabled(ena);
        bDataEntryON = ena;
        if (ena) {
            if (saveForFE != null)
                saveForFE.setEnabled(false && !onProductionLine);
            saveForTFM.setEnabled(false && !onProductionLine);
            saveToXL.setEnabled(false);
        }
        tuningParams.enableDataEntry(ena && !onProductionLine);
//        enableResultsMenu(false);
    }

    void setTestData() {
        addLossType("1001", "Wall Losses", "" + 1.55, "WALL", "LINEAR");
        addLossType("1002", "Roof Losses", "" + 2, "WALL", "LINEAR");
        addLossType("1003", "Skid Losses", "" + 130, "LENGTH", "LINEAR");
        addLossType("1004", "Charging End Loss", "" + 30000, "FIXED", "NONE");
        addLossType("1005", "Discharge End Loss", "" + 60000, "FIXED", "NONE");

//        cbHeatingType.setSelectedItem("TOP AND BOTTOM FIRED");
        cbHeatingMode.setSelectedItem("TOP AND BOTTOM FIRED");
        furnace.changeSubSecData(false, 0, 0, 2.0, 0.9, 0.9, 0);
        furnace.assignLoss(0, 0, false, 1005);
        furnace.changeSubSecData(false, 0, 1, 5.0, 1.2, 1.2, 0);
        furnace.changeSubSecData(false, 1, 0, 8.0, 1.2, 1.2, 0);
        furnace.setSectionType(false, 1, false);
        furnace.changeSubSecData(false, 2, 0, 6.5, 1.0, 1.0, 0);
        furnace.setSectionType(false, 2, false);
        furnace.changeSubSecData(false, 3, 0, 7, 1.0, 1.0, 0);
        furnace.setSectionType(false, 3, false);

        furnace.changeSubSecData(true, 0, 0, 2.0, 1.2, 1.2, 0);
        furnace.changeSubSecData(true, 0, 1, 5.0, 2.2, 2.2, 0);
        furnace.changeSubSecData(true, 1, 0, 6.5, 2, 2, 0);
        furnace.setSectionType(true, 1, false);
        furnace.changeSubSecData(true, 2, 0, 8, 2, 2, 0);
        furnace.setSectionType(true, 2, false);
        furnace.changeSubSecData(true, 3, 0, 7, 2, 2, 0);
        furnace.setSectionType(true, 3, false);
        if (asApplication) {
            fuelSpecsFromFile("defData\\FuelSpecifications.dfhSpecs");
            chMaterialSpecsFromFile("defData\\ChMaterialSpecifications.dfhSpecs");
        }

        if (onTest || asApplication) {
            if (fuelList.size() == 0) {
                addFuel("Psuedo Nat Gas #8500 [8,502 kcal/m3N]", "Nm3", "" + 8503, "" +
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
        adjustForLengthChange();
    }

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
//  debug
//        debug("addFuelChoice sensHeatPair:<" + sensHeatPair + ">");
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
            setDefaultSelections();
            switchPage(InputType.INPUTPAGE);
            mainF.setFocusable(true);
//            mainF.setVisible(true);
            mainF.requestFocus();
            mainF.toFront(); //setAlwaysOnTop(true);
//            trendPanel.setTraceToShow(-1);
            mainF.setSize(1000, 700);
//            inpPage.updateUI();
            mainF.setVisible(true);
        }
    }

//    public void setVisible(boolean bVisible) {
//        mainF.setVisible(bVisible);
//    }

    void setDefaultSelections() {
        if (cbFuel.getItemCount() == 1)
            cbFuel.setSelectedIndex(0);
        if (cbChMaterial.getItemCount() == 1)
            cbChMaterial.setSelectedIndex(0);
    }

    void setAllowSecFuel(boolean allow) {
        bAllowSecFuel = allow;
        if (!(furnace == null))
            furnace.setAllowSecFuel(allow);
    }

    void allowManualTempForLosses(boolean allow) {
        furnace.allowManualTempForLosses(allow);
    }

    protected DFHTuningParams.ForProcess  getFceFor() {
        return (DFHTuningParams.ForProcess)cbFceFor.getSelectedItem();
    }

    protected void switchPage(InputType page) {
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
        }
    }

    void switchPage(Component c) {
        slate.setViewportView(c);
    }

    JPanel inputPage() {
        mainFrame = new JPanel(new GridBagLayout());
//        mainFrame.setBackground(new JPanel().getBackground());
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
                    String msg1 = "";
                    String msg2 = "Enter Furnace Profile for Combined Top And Bottom.\nie. TOTAL Height from Floor to Roof";
                    if (cbFceFor.getSelectedItem() != DFHTuningParams.ForProcess.STRIP) {
                        msg1 = "Changing 'Furnace For' to Strip Heating\n\n";
                        if (!onProductionLine)
                            showMessage(msg1 + msg2);
                        cbFceFor.setSelectedItem(DFHTuningParams.ForProcess.STRIP);
                    }
                    else {
                        if (!onProductionLine)
                            showMessage(msg2);
                    }
                    break;
            }
            furnace.changeFiringMode(bTopBot, bAddTopSoak);
            noteFiringModeChange(bTopBot);
        }
    }

    JPanel OperationPage() {
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
        jp.add(chargeDataPanel(), gbcOP);
        gbcChDatLoc = new GridBagConstraints();
        gbcChDatLoc.gridx = gbcOP.gridx;
        gbcChDatLoc.gridy = gbcOP.gridy;
        gbcChDatLoc.anchor = gbcOP.anchor;

        gbcOP.gridx++;
        gbcOP.anchor = GridBagConstraints.WEST;
        jp.add(chargeInFurnacePanel(), gbcOP);
        gbcOP.gridx++;
        jp.add(recuDataPanel(), gbcOP);
        gbcOP.gridy++;
        gbcOP.gridx = 0;
        jp.add(tuningParams.userTunePan(), gbcOP);
        gbcOP.gridx++;
        gbcOP.gridx++;
        jp.add(calCulDataPanel(), gbcOP);
        gbcOP.gridx = 0;
        return jp;
    }

    void addFceCommAndDataPanel(JPanel jp) {
        GridBagConstraints gbcOP = new GridBagConstraints();
        gbcOP.gridx = 0;
        gbcOP.gridy = 0;
        gbcOP.insets = new Insets(0, 0, 0, 0);
        gbcOP.gridwidth = 3;
        jp.add(titleAndFceCommon(), gbcOP);
    }

    JMenuItem mIGetFceProfile;
    JMenuItem mILoadRecuSpecs;
    JMenuItem mISaveFceProfile;
    JMenuItem mIExit;
    JMenuItem mIInputData;
    JMenuItem mIOpData;
    JMenuItem mICreateFuelMix;
    JMenuItem mIRegenBurnerStudy;
    JMenuItem mITuningParams;


    void prepareMenuItems() {
        MenuActions mAction = new MenuActions();

        mIGetFceProfile = new JMenuItem("Get Furnace Profile");
        mIGetFceProfile.addActionListener(mAction);

        mILoadRecuSpecs = new JMenuItem("Load Recuperator Specs.");
        mILoadRecuSpecs.addActionListener(mAction);

        mISaveFceProfile = new JMenuItem("Save Furnace Profile");
        mISaveFceProfile.addActionListener(mAction);

        saveToXL = new JMenuItem("Save Results and Furnace Data to Excel");
        saveToXL.addActionListener(mAction);
        saveToXL.setEnabled(false);

        saveForTFM = new JMenuItem("Save Temperature Profile for TFM");
        saveForTFM.addActionListener(mAction);
        saveForTFM.setEnabled(false);

        saveForFE = new JMenuItem("Save Furnace Ambients for FE Analysis");
        saveForFE.addActionListener(mAction);
        saveForFE.setEnabled(false);

        saveFuelSpecs = new JMenuItem("Save Fuel Specifications to File");
        saveFuelSpecs.addActionListener(mAction);
        saveFuelSpecs.setEnabled(true);

        saveSteelSpecs = new JMenuItem("Save Steel Specifications to File");
        saveSteelSpecs.addActionListener(mAction);
        saveSteelSpecs.setEnabled(true);

        mIExit = new JMenuItem("Exit");
        mIExit.addActionListener(mAction);

        mIInputData = new JMenuItem("Input Data");
        mIInputData.addActionListener(mAction);

        mIOpData = new JMenuItem("Operation Data");
        mIOpData.addActionListener(mAction);

        mICreateFuelMix = new JMenuItem("Create Fuel Mix");
        mICreateFuelMix.addActionListener(mAction);

        mIRegenBurnerStudy = new JMenuItem("Regen Burner Study");
        mIRegenBurnerStudy.addActionListener(mAction);

        mITuningParams = new JMenuItem("Tuning Parameters");
        mITuningParams.addActionListener(mAction);

        beamParamTFM = new JMenuItem("Walking Beam Params from TFM");
        beamParamTFM.addActionListener(mAction);
        beamParamTFM.setEnabled(false);

        lossParamTFM = new JMenuItem("Loss Params from TFM");
        lossParamTFM.addActionListener(mAction);
        lossParamTFM.setEnabled(false);

        progressP = new JMenuItem("Show Progress");
        progressP.addActionListener(mAction);

        pbEdit = new JButton("Allow Data Edit");
        pbEdit.getModel().setPressed(true);
        pbEdit.setEnabled(false);
        pbEdit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableDataEdit();
//                enableDataEntry(true);
//                pbEdit.getModel().setPressed(true);
//                pbEdit.setEnabled(false);

            }
        });
    }

    protected JMenuBar mb = new JMenuBar();

    void setMenuOptions() {
//        JMenuBar mb = new JMenuBar();
        prepareMenuItems();

        fileMenu = new JMenu("File");
        if (!onProductionLine) {
            fileMenu.add(mIGetFceProfile);
            fileMenu.add(mILoadRecuSpecs);

            fileMenu.addSeparator();
            fileMenu.add(mISaveFceProfile);
            fileMenu.add(saveToXL);

            fileMenu.addSeparator();
            fileMenu.add(saveForTFM);

            if (enableSpecsSave || onTest) {
                fileMenu.addSeparator();
                fileMenu.add(saveForFE);
                fileMenu.add(saveFuelSpecs);
                fileMenu.add(saveSteelSpecs);
            }
        }
        fileMenu.addSeparator();
        fileMenu.add(mIExit);
        mb.add(fileMenu);

        inputMenu = new JMenu("Define Furnace");
        inputMenu.add(mIInputData);
        inputMenu.add(mIOpData);

        if (!onProductionLine) {
            inputMenu.addSeparator();
            inputMenu.add(mICreateFuelMix);
            inputMenu.add(mIRegenBurnerStudy);

            inputMenu.addSeparator();
            inputMenu.add(mITuningParams);

            inputMenu.addSeparator();
            inputMenu.add(beamParamTFM);
            inputMenu.add(lossParamTFM);
        }
        mb.add(inputMenu);

        statMenu = new JMenu("Calculation Status");
        statMenu.add(progressP);
        statMenu.setEnabled(false);
        mb.add(statMenu);

        mb.add(getResultsMenu());
        mb.add(getPrintMenu());
        mb.add(getCompareMenu());

        mb.add(getPerformMenu());
        mb.add(pbEdit);
        mainF.setJMenuBar(mb);
    }

    public boolean isOnProductionLine() {
        return onProductionLine;
    }

    public void enableDataEdit() {
        Component vNow = slate.getViewport().getView();
        if (vNow != inpPage && vNow != opPage)
            switchPage(InputType.INPUTPAGE);

        enableDataEntry(true);
        fileMenu.setEnabled(true);
        inputMenu.setEnabled(true);
        pbEdit.getModel().setPressed(true);
        pbEdit.setEnabled(false);
    }

    protected void disableCompare() {
        compareResults.setEnabled(false);
        enableSaveForComparison(false);
        enableShowComparison(false);
    }

    JMenuItem createPerfBase;
    JMenuItem addToPerfBase;
    JMenuItem clearPerfBase;
    JMenuItem showPerfBase;
    JMenu perfMenu;
    JMenuItem setPerfTablelimits;

    JMenu getPerformMenu() {
        PerformListener li = new PerformListener();
        createPerfBase = new JMenuItem("Create Performance Base");
        createPerfBase.addActionListener(li);
        addToPerfBase = new JMenuItem("Add to Performance Base");
        addToPerfBase.setEnabled(false);
        addToPerfBase.addActionListener(li);
        showPerfBase = new JMenuItem("Show Performance Base List");
        showPerfBase.setEnabled(false);
        showPerfBase.addActionListener(li);

        clearPerfBase = new JMenuItem("Clear Performance Base");
        clearComparison.setEnabled(false);
        clearPerfBase.addActionListener(li);
        setPerfTablelimits = new JMenuItem("Set Limits for Performance Table");
        setPerfTablelimits.addActionListener(li);
        perfMenu = new JMenu("Performance");
        perfMenu.add(createPerfBase);
        perfMenu.add(addToPerfBase);
        perfMenu.addSeparator();
        perfMenu.add(setPerfTablelimits);
        perfMenu.addSeparator();
        perfMenu.add(showPerfBase);
        perfMenu.addSeparator();
        perfMenu.add(clearPerfBase);
        perfMenu.setEnabled(false);
        perfMenu.setVisible(false);
        return perfMenu;
    }

    void enableCreatePerform(boolean ena)  {
        clearPerfBase.setEnabled(!ena);
        createPerfBase.setEnabled(bResultsReady && ena);
    }

    void enableAddToPerform(boolean ena) {
        addToPerfBase.setEnabled(ena);
        showPerfBase.setEnabled(ena);
    }

    void perfBaseAvailable(boolean available) {
        showPerfBase.setEnabled(available);
        clearPerfBase.setEnabled(available);
    }

    void enablePerfMenu(boolean ena)  {
        perfMenu.setVisible(ena);
        perfMenu.setEnabled(ena);
//        if (!perfMenu.isEnabled()) {
//            perfMenu.setEnabled(ena);
//            enableCreatePerform(ena);
//        }
    }

    JMenu getCompareMenu() {
        compareResults = new JMenu("Compare Results");
        compareResults.setEnabled(false);
        CompareMenuListener l = new CompareMenuListener();
        saveComparison = new JMenuItem("Save results to Comparison Table");
        saveComparison.setEnabled(false);
        saveComparison.addActionListener(l);
        showComparison = new JMenuItem("Show Comparison Table");
        showComparison.setEnabled(false);
        showComparison.addActionListener(l);
        compareResults.add(saveComparison);
        compareResults.add(showComparison);
        saveComparisontoXL = new JMenuItem("Save Results Comparison Table to A New Excel file");
        saveComparisontoXL.addActionListener(l);
        saveComparisontoXL.setEnabled(false);
        compareResults.add(saveComparisontoXL);
        appendComparisontoXL = new JMenuItem("Append Results to Comparison Table in Excel");
        appendComparisontoXL.addActionListener(l);
        appendComparisontoXL.setEnabled(false);
        compareResults.add(appendComparisontoXL);
        clearComparison = new JMenuItem("Clear Comparison Table");
        clearComparison.addActionListener(l);
        clearComparison.setEnabled(false);
        compareResults.add(clearComparison);

        return compareResults;
    }

    void enableCompareMenu(boolean ena) {
        compareResults.setEnabled(ena);
    }

    void enableSaveForComparison(boolean ena) {
        saveComparison.setEnabled(ena);
    }

    void enableShowComparison(boolean ena) {
        showComparison.setEnabled(ena);
        saveComparisontoXL.setEnabled(ena);
        appendComparisontoXL.setEnabled(ena);
    }

    JMenu getResultsMenu() {
        ResultsMenuActions li = new ResultsMenuActions();
        resultsMenu = new JMenu("View Results");
        resultPanels = new Hashtable<DFHResult.Type, ResultPanel>();

        resultPanels.put(DFHResult.Type.HEATSUMMARY, new ResultPanel(DFHResult.Type.HEATSUMMARY, resultsMenu, li));
        resultPanels.put(DFHResult.Type.SECTIONWISE, new ResultPanel(DFHResult.Type.SECTIONWISE , resultsMenu, li));
        resultPanels.put(DFHResult.Type.TOPSECTIONWISE, new ResultPanel(DFHResult.Type.TOPSECTIONWISE, resultsMenu, li));
        resultPanels.put(DFHResult.Type.BOTSECTIONWISE, new ResultPanel(DFHResult.Type.BOTSECTIONWISE, resultsMenu, li));
        resultsMenu.addSeparator();
        resultPanels.put(DFHResult.Type.RECUBALANCE, new ResultPanel(DFHResult.Type.RECUBALANCE, resultsMenu, li));
        resultsMenu.addSeparator();

        resultPanels.put(DFHResult.Type.LOSSDETAILS, new ResultPanel(DFHResult.Type.LOSSDETAILS, resultsMenu, li));
        resultsMenu.addSeparator();

        resultPanels.put(DFHResult.Type.FUELSUMMARY, new ResultPanel(DFHResult.Type.FUELSUMMARY, resultsMenu, li));
        resultPanels.put(DFHResult.Type.FUELS, new ResultPanel(DFHResult.Type.FUELS, resultsMenu, li));
        resultPanels.put(DFHResult.Type.TOPFUELS, new ResultPanel(DFHResult.Type.TOPFUELS, resultsMenu, li));
        resultPanels.put(DFHResult.Type.BOTFUELS, new ResultPanel(DFHResult.Type.BOTFUELS, resultsMenu, li));
        resultsMenu.addSeparator();
        resultPanels.put(DFHResult.Type.TEMPRESULTS, new ResultPanel(DFHResult.Type.TEMPRESULTS, resultsMenu, li));
        resultPanels.put(DFHResult.Type.TOPtempRESULTS, new ResultPanel(DFHResult.Type.TOPtempRESULTS, resultsMenu, li));
        resultPanels.put(DFHResult.Type.BOTtempRESULTS, new ResultPanel(DFHResult.Type.BOTtempRESULTS, resultsMenu, li));
        resultsMenu.addSeparator();

        resultPanels.put(DFHResult.Type.COMBItempTRENDS, new ResultPanel(DFHResult.Type.COMBItempTRENDS, resultsMenu, li));
        resultPanels.put(DFHResult.Type.TOPtempTRENDS, new ResultPanel(DFHResult.Type.TOPtempTRENDS, resultsMenu, li));
        resultPanels.put(DFHResult.Type.BOTtempTRENDS, new ResultPanel(DFHResult.Type.BOTtempTRENDS, resultsMenu, li));

        resultsMenu.setEnabled(false);
        return resultsMenu;
    }

    JMenu getPrintMenu() {
        PrintMenuActions li = new PrintMenuActions();
        printMenu = new JMenu("Print Results");
        printPanels = new Hashtable<DFHResult.Type, ResultPanel>();

        printPanels.put(DFHResult.Type.HEATSUMMARY, new ResultPanel(DFHResult.Type.HEATSUMMARY, printMenu, li));
        printPanels.put(DFHResult.Type.SECTIONWISE, new ResultPanel(DFHResult.Type.SECTIONWISE, printMenu, li));
        printPanels.put(DFHResult.Type.TOPSECTIONWISE, new ResultPanel(DFHResult.Type.TOPSECTIONWISE, printMenu, li));
        printPanels.put(DFHResult.Type.BOTSECTIONWISE, new ResultPanel(DFHResult.Type.BOTSECTIONWISE, printMenu, li));
        printPanels.put(DFHResult.Type.ALLBALANCES, new ResultPanel(DFHResult.Type.ALLBALANCES, printMenu, li));
        printMenu.addSeparator();

        printPanels.put(DFHResult.Type.RECUBALANCE, new ResultPanel(DFHResult.Type.RECUBALANCE, printMenu, li));
        printMenu.addSeparator();

/*
        printPanels.put(DFHResult.Type.LOSSDETAILS, new ResultPanel(DFHResult.Type.LOSSDETAILS, printMenu, li));
        printMenu.addSeparator();
*/

        printPanels.put(DFHResult.Type.FUELSUMMARY, new ResultPanel(DFHResult.Type.FUELSUMMARY, printMenu, li));
        printPanels.put(DFHResult.Type.FUELS, new ResultPanel(DFHResult.Type.FUELS, printMenu, li));
        printPanels.put(DFHResult.Type.TOPFUELS, new ResultPanel(DFHResult.Type.TOPFUELS, printMenu, li));
        printPanels.put(DFHResult.Type.BOTFUELS, new ResultPanel(DFHResult.Type.BOTFUELS, printMenu, li));
        printMenu.addSeparator();

/*
        printPanels.put(DFHResult.Type.TOPRESULTS, new ResultPanel(DFHResult.Type.TOPRESULTS, printMenu, li));
        printPanels.put(DFHResult.Type.BOTRESULTS, new ResultPanel(DFHResult.Type.BOTRESULTS, printMenu, li));
        printPanels.put(DFHResult.Type.COMBIRESULTS, new ResultPanel(DFHResult.Type.COMBIRESULTS, printMenu, li));
        printMenu.addSeparator();
*/

        printPanels.put(DFHResult.Type.COMBItempTRENDS, new ResultPanel(DFHResult.Type.COMBItempTRENDS, printMenu, li));
        printPanels.put(DFHResult.Type.TOPtempTRENDS, new ResultPanel(DFHResult.Type.TOPtempTRENDS, printMenu, li));
        printPanels.put(DFHResult.Type.BOTtempTRENDS, new ResultPanel(DFHResult.Type.BOTtempTRENDS, printMenu, li));
        printPanels.put(DFHResult.Type.ALLtempTRENDS, new ResultPanel(DFHResult.Type.ALLtempTRENDS, printMenu, li));
        printMenu.addSeparator();

        printPanels.put(DFHResult.Type.FUELMIX, new ResultPanel(DFHResult.Type.FUELMIX, printMenu, li));

        printMenu.addSeparator();

        printMenu.setEnabled(false);
        return printMenu;
    }

    @Override
    public void destroy() {
        boolean goAhead = true;
        if (furnace.isPerformanceToBeSaved())
            goAhead = decide("Unsaved Performance Data", "Some Performance data have been collected\n" +
                                                "Do you want to ABANDON them and exit?");
        if (goAhead) {
            itsON = false;
            debug("In Destroy");
            super.destroy();    //To change body of overridden methods use File | Settings | File Templates.
            if (win != null)
                win.eval("gettingOut()");
            mainF.dispose();
        }
    }

    void close() {
        destroy();
//        itsON = false;
//        debug("CLOSING ...");
//        mainF.dispose();
//        itsON = false;
//        if (!onTest)
//            win.eval("gettingOut()");
    } // close

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
//            JComponent comp = rP.getPanel();
//            slate.setViewportView(comp);
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
//        changeTopBot("Top Zones");
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
//        rowHead = FceSubSection.getRowHeader(new RadioListener(), lbTopLen, lbBotLen);
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

    FramedPanel titleAndFceCommon() {
        if (titleAndFceCommon == null) {
            FramedPanel panel = new FramedPanel(new GridBagLayout());
            GridBagConstraints gbcTp = new GridBagConstraints();
            gbcTp.gridx = 0;
            gbcTp.gridy = 0;
            panel.add(getTitlePanel(), gbcTp);
            gbcTp.gridx++;
            panel.add(fceCommDataPanel(), gbcTp);
            titleAndFceCommon = panel;
        }
        cbFuel.updateUI();
        return titleAndFceCommon;
    }

    FramedPanel getTitlePanel() {
        MultiPairColPanel titlePanel = new MultiPairColPanel("");
        tfReference = new XLTextField(reference, 40);
        titlePanel.addItemPair("Reference ", tfReference);
        tfFceTitle = new XLTextField(fceTtitle, 40);
        titlePanel.addItemPair("Title ", tfFceTitle);
        tfCustomer = new XLTextField(customer, 40);
        titlePanel.addItemPair("Customer ", tfCustomer);
        cbFceFor = new XLComboBox(DFHTuningParams.ForProcess.values());
//        cbFceFor.setSelectedItem(proc);
        cbFceFor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setFcefor(!fceFor1stSwitch);
                if (!fceFor1stSwitch)
                    showMessage("You have switched 'Furnace For'\nRecheck data for charge width, charge pitch if applicable ");
                fceFor1stSwitch = false;
            }
        });
//        cbFceFor.setSelectedItem(proc);
        cbFceFor.setPreferredSize(new Dimension(200, 20));
        titlePanel.addItemPair("Furnace For ", cbFceFor);
        mpTitlePanel = titlePanel;
        return titlePanel;
    }

    FramedPanel fceCommDataPanel() {
        MultiPairColPanel panel = new MultiPairColPanel("");
//        Vector<String> vHeatType = new Vector<String>();
//        vHeatType.add("TOP FIRED");
//        vHeatType.add("TOP AND BOTTOM FIRED");
//        vHeatType.add("TOP & BOTTOM + TOP SOAK");
//        cbHeatingType = new XLComboBox(vHeatType);
//        cbHeatingType.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                int selIndex = cbHeatingType.getSelectedIndex();
//                bTopBot = (selIndex > 0);
//                bAddTopSoak = (selIndex == 2);
//                if (bAddTopSoak)
//                    showMessage("Top Fired Soak Zone Added after Top Zone 6");
//                furnace.changeFiringMode(bTopBot, bAddTopSoak);
//                noteFiringModeChange(bTopBot);
//            }
//        });
//        cbHeatingType.setPreferredSize(new Dimension(200, 20));
//        panel.addItemPair("Heating Mode ", cbHeatingType);
        cbHeatingMode = new XLComboBox(HeatingMode.values());
        cbHeatingMode.addActionListener(new HeatingModeListener());
        cbHeatingMode.setPreferredSize(new Dimension(200, 20));
        cbHeatingMode.setSelectedItem(HeatingMode.TOPONLY);
        panel.addItemPair("Heating Mode ", cbHeatingMode);
        ntfWidth = new NumberTextField(this, width * 1000, 10, false, 500, 40000, "#,###", "Furnace Width (mm) ");
        panel.addItemPair(ntfWidth);
        cbFuel = new XLComboBox(fuelList);
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
        cbFuel.setSelectedItem(commFuel);
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
        panel.addItemPair("Common Fuel ", cbFuel);
        tfExcessAir = new NumberTextField(this, excessAir * 100, 5, false, 0, 100, "###", "Excess Air (%) ");
        panel.addItemPair(tfExcessAir);
        mpFceCommDataPanel = panel;
        return panel;
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

    JPanel chargeDataPanel() {
        MultiPairColPanel jp = new MultiPairColPanel("Charge Details");
        cbChType = new XLComboBox(Charge.ChType.values());
        cbChType.setSelectedItem(Charge.ChType.SOLID_RECTANGLE);
        addInputToListener(cbChType);
        jp.addItemPair("Charge Cross Section", cbChType);
        tfChDiameter = new NumberTextField(this, chDiameter * 1000, 5, false, 10, 2000, "#,###", "Dia of cross section (mm)");
        addInputToListener(tfChDiameter);
        jp.addItemPair(tfChDiameter);
        tfChWidth = new NumberTextField(this, chWidth * 1000, 5, false, 50, 25000, "#,###", "Width (Along Furnace) (mm)");
        addInputToListener(tfChWidth);
        labChWidth = new JLabel("Billet/Slab Width (mm)");
        jp.addItemPair(labChWidth, tfChWidth);
        tfChThickness = new NumberTextField(this, chThickness * 1000, 5, false, 0.05, 10000, "#,###.###", "Thickness (mm)");
        addInputToListener(tfChThickness);
        jp.addItemPair(tfChThickness.getName(), tfChThickness);
        tfChLength = new NumberTextField(this, chLength * 1000, 5, false, 500, 25000, "#,###", "Length (Across Furnace) (mm)");
        addInputToListener(tfChLength);
        labChLength = new JLabel("Billet/Slab Length (mm)");
        jp.addItemPair(labChLength, tfChLength);
        cbChMaterial = new XLComboBox(vChMaterial);
        cbChMaterial.setPreferredSize(new Dimension(200, 18));
        cbChMaterial.setSelectedItem(selChMaterial);
        addInputToListener(cbChMaterial);
        jp.addItemPair("Charge Material", cbChMaterial);
        setChargeSizeChoice();
        mpChargeData = jp;
        return jp;
    }

    public ChMaterial getSelChMaterial(String matName) {
        ChMaterial chMat = null;
        for (ChMaterial oneMat: vChMaterial)
            if (matName.equalsIgnoreCase(oneMat.name)) {
                chMat = oneMat;
                break;
            }
        return chMat;
    }

    public Fuel getSelFuel(String fuelName) {
        Fuel selFuel = null;
        for (Fuel fuel: fuelList)
            if (fuelName.equalsIgnoreCase(fuel.name)) {
                selFuel = fuel;
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

    public ActionListener lengthChangeListener() {
        return lengthListener;
    }

    public FocusListener lengthFocusListener() {
        return lengthListener;
    }

    JPanel chargeInFurnacePanel() {
        MultiPairColPanel jp = new MultiPairColPanel("Charge In Furnace");
        tfBottShadow = new NumberTextField(this, bottShadow * 100, 5, false, 0, 100, "###", "Shadow on Bottom Surface (%)");
        jp.addItemPair(tfBottShadow.getLabel(), tfBottShadow);
        tfChPitch = new NumberTextField(this, chPitch * 1000, 5, false, 0, 10000, "#,###", "Charge Pitch (mm)");
        jp.addItemPair(tfChPitch);
        tfChRows = new NumberTextField(this, nChargeRows, 5, false, 1, 5, "#,###", "Charge Rows");
        jp.addItemPair(tfChRows);
        tfProduction = new NumberTextField(this, tph, 5, false, 0, 500, "#,###.00", "Production (t/h)");
        jp.addItemPair(tfProduction);
        tfEntryTemp = new NumberTextField(this, entryTemp, 5, false, 0, 1500, "#,###", "Charge Entry Temperature (C)");
        jp.addItemPair(tfEntryTemp);
        tfExitTemp = new NumberTextField(this, exitTemp, 5, false, 0, 1500, "#,###", "Charge Exit Temperature (C)");
        jp.addItemPair(tfExitTemp);
        tfDeltaTemp = new NumberTextField(this, deltaTemp, 5, false, 0.001, 500, "#,###.000", "Temperature Dis-uniformity (C)");
        jp.addItemPair(tfDeltaTemp);
        tfExitZoneFceTemp = new NumberTextField(this, exitZoneFceTemp, 5, false, 500, 1400, "#,###", "Exit zone Furnace Temperature (C)");
        jp.addItemPair(tfExitZoneFceTemp);
        tfMinExitZoneFceTemp = new NumberTextField(this, minExitZoneFceTemp, 5, false, 500, 1400, "#,###", "Minimum Exit zone Furnace Temperature (C)");
        jp.addItemPair(tfMinExitZoneFceTemp);
        tfTotTime = new NumberTextField(this, 0, 5, false, 0, 500, "##0.000", "Total Heating time (h)");
        tfTotTime.setEnabled(false);
        jp.addItemPair(tfTotTime);
        tfSpTime = new NumberTextField(this, 0, 5, false, 0, 500, "##0.000", "Specific Heating time (min/mm)");
        tfSpTime.setEnabled(false);
        jp.addItemPair(tfSpTime);
        tfSpeed = new NumberTextField(this, 0, 5, false, 0, 500, "#,##0.000", "Charge Speed (m/min)");
        tfSpeed.setEnabled(false);
        jp.addItemPair(tfSpeed);
        setTimeValues(0, 0, 0);
        mpChInFce = jp;
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

    JPanel calCulDataPanel() {
        MultiPairColPanel jp = new MultiPairColPanel("Calculate");
        tfAmbTemp = new NumberTextField(this, ambTemp, 5, false, 0, 500, "#,###", "Ambient Temperature (C)");
        jp.addItemPair(tfAmbTemp);
        tfAirTemp = new NumberTextField(this, airTemp, 5, false, 0, 3000, "#,###", "Air Preheat (C)");
        jp.addItemPair(tfAirTemp);
        tfFuelTemp = new NumberTextField(this, fuelTemp, 5, false, 0, 3000, "#,###", "Fuel Preheat (C)");
        jp.addItemPair(tfFuelTemp);
//        tfFuelTemp.setEditable(true);
        tfCalculStep = new NumberTextField(this, calculStep * 1000, 5, false, 200, 5000, "#,###", "Calculation Step (mm)");
        jp.addItemPair(tfCalculStep);
        pbCalculate = new JButton("Calculate");
        pbCalculate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                calculateFce();
            }
        });
        jp.addItemPair("", pbCalculate);
        mpCalcul = jp;
        return jp;
    }

    void enaCommonFuelTemp() {
        if (furnace.anyCommonFuel())
            tfFuelTemp.setEnabled(true);
        else
            tfFuelTemp.setEnabled(false);
    }

    JPanel recuDataPanel() {
        MultiPairColPanel jp = new MultiPairColPanel("Recuperator Data");
        ntDeltaTFlue = new NumberTextField(this, deltaTflue, 5, false, 0, 100, "#,###", "Flue DeltaT Fce-Recu (C)");
        jp.addItemPair(ntDeltaTFlue);
        ntMaxFlueTatRecu = new NumberTextField(this, maxFlueAtRecu, 5, false, 0, 1200, "#,###", "Max. Flue temp at 1st Recu (C)");
        jp.addItemPair(ntMaxFlueTatRecu);
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
        jp.addItemPair("Common Air Heated in Recu", cBAirHeatByRecu);
        jp.addItemPair(ntdeltaTAirFromRecu);
        jp.addItemPair("Common Fuel Heated in Recu", cBFuelHeatByRecu);
        jp.addItemPair(ntDeltaTFuelFromRecu);
        jp.addItemPair("Air Recu is after Fuel Recu", cBAirAfterFuel);
        mpRecuData = jp;
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
//        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
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

//        cell.setCellValue(inputDataXML(false));
//        cell.setCellValue(inputDataXML());
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

    public boolean canNotify() {
        return canNotify;
    }

    public void enableNotify(boolean ena) {
        canNotify = ena;
    }

    public Frame parent() {
        return mainF;
    }

    public String addChMaterial(String matName, String matID, String density, String tempTkPairStr, String tempHcPairStr,
                                String tempEmPairStr) {
        String retVal = "";
        double den = 0;
//  debug("Steel " + matName + "tkStr " + tempTkPairStr);
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
//  debug
//        debug("addFuel sensHeatPair:<" + sensHeatPair + ">");

        return retVal;
    }

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

    public String setChargeHeatCont(String heatCont) {
        hC = new XYArray(heatCont);
        return "OK " + hC.arrLen;
    }

    public String setChargeTk(String thermalC) {
        tk = new XYArray(thermalC);
        return "OK " + tk.arrLen;
    }

    public String setChargeEmiss(String emissivity) {
        emiss = new XYArray(emissivity);
        return "OK " + emiss.arrLen;
    }

    protected void debug(String msg) {
        System.out.println("DFHeating " + msg);
    }

    public void takeValuesFromUI() {
        reference = tfReference.getText();
        fceTtitle = tfFceTitle.getText();
        customer = tfCustomer.getText();

        proc = (DFHTuningParams.ForProcess) cbFceFor.getSelectedItem();
        tuningParams.setSelectedProc(proc);
//        bTopBot = (cbHeatingType.getSelectedIndex() == 1);
        width = ntfWidth.getData() / 1000;
        furnace.setWidth(width);
        commFuel = (Fuel) cbFuel.getSelectedItem();
        excessAir = tfExcessAir.getData() / 100;

        chWidth = tfChWidth.getData() / 1000;
//        if (cbFceFor.getSelectedItem() == DFHTuningParams.ForProcess.STRIP)
//            chLength = 1;
//        else
        chLength = tfChLength.getData() / 1000;
        chThickness = tfChThickness.getData() / 1000;
        chDiameter = tfChDiameter.getData() / 1000;
        selChMaterial = (ChMaterial) cbChMaterial.getSelectedItem();

        bottShadow = tfBottShadow.getData() / 100;
        if (cbFceFor.getSelectedItem() == DFHTuningParams.ForProcess.STRIP)
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
        fuelTemp = tfFuelTemp.getData();
        calculStep = tfCalculStep.getData() / 1000;

        commFuel = (Fuel) cbFuel.getSelectedItem();

        furnace.takeValuesFromUI();
    }

    protected void hidePerformMenu() {
        furnace.clearPerfBase();
        perfMenu.setVisible(false);
        createPerfBase.setEnabled(false);
        addToPerfBase.setEnabled(false);
        clearPerfBase.setEnabled(false);
        showPerfBase.setEnabled(false);
     }

    protected void setFcefor(boolean showSuggestion) {
        proc = (DFHTuningParams.ForProcess)cbFceFor.getSelectedItem();
        if (proc == DFHTuningParams.ForProcess.STRIP) {
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
            enablePerfMenu(true);
            if (showSuggestion && cbHeatingMode.getSelectedItem() != HeatingMode.TOPBOTSTRIP)
                showMessage("Suggest selecting 'Heating Mode' to STRIP - TOP and BOTTOM");
//            perfMenu.setVisible(true);
        } else {
//            tfChDiameter.setEnabled(false);
//            cbChType.setSelectedItem(Charge.ChType.RECTANGULAR);
            if (cbHeatingMode.getSelectedItem() == HeatingMode.TOPBOTSTRIP)  {
                cbHeatingMode.setSelectedItem(HeatingMode.TOPBOT);
                showMessage("Heating Mode changed to TOP AND BOTTOM");
            }
            cbChType.setEnabled(true);
            tfChPitch.setEnabled(bDataEntryON);
            tfChRows.setEnabled(bDataEntryON);
            tfChWidth.setEnabled(bDataEntryON);
            tfDeltaTemp.setEnabled(bDataEntryON);
            tfExitZoneFceTemp.setEnabled(false);
            tfExitZoneFceTemp.setData(0);
            tfMinExitZoneFceTemp.setEnabled(false);
            tfMinExitZoneFceTemp.setData(0);
            labChLength.setText("Billet/ Slab Length (mm)");
            hidePerformMenu();
        }
        opPage.updateUI();
        if (proc == DFHTuningParams.ForProcess.MANUAL)
            showMessage("You have selected 'Furnace For' as 'Manually Set'.\n\n   YOU ARE ON YOUR OWN NOW !!!");
    }

    void setChargeSizeChoice() {
        if (cbChType.getSelectedItem() == Charge.ChType.SOLID_RECTANGLE) {
            tfChDiameter.setEnabled(false);
            tfChWidth.setEnabled(true && proc != DFHTuningParams.ForProcess.STRIP);
            tfChThickness.setEnabled(true);
            if (saveForFE != null)
                saveForFE.setEnabled(true);
        }
        else {
            tfChDiameter.setEnabled(true);
            tfChWidth.setEnabled(false);
            tfChThickness.setEnabled(false);
            if (saveForFE != null)
                saveForFE.setEnabled(false);
        }
    }

    public void setTableFactors(Performance performance) {
        performance.setTableFactors(tuningParams.minOutputFactor, tuningParams.outputStep,
                            tuningParams.minWidthFactor, tuningParams.widthStep);
    }

//    public String addFuelChoice(String name, String units, String calValStr, String airFuelRatioStr, String flueFuelRatioStr,
//                                String sensHeatPair,
//                                String percCO2str, String percH2Ostr, String percN2str, String percO2str, String percSO2str) {

    void setValuesToUI() {
        tfReference.setText(reference);
        tfFceTitle.setText(fceTtitle);
        tfCustomer.setText(customer);
        ntfWidth.setData(width * 1000);
        tfExcessAir.setData(excessAir * 100);

        tfChWidth.setData(chWidth * 1000);
        tfChThickness.setData(chThickness * 1000);
        tfChLength.setData(chLength * 1000);
        tfChDiameter.setData(chDiameter * 1000);

        tfBottShadow.setData(bottShadow * 100);
        tfProduction.setData(tph);
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
        if (cbChMaterial.getSelectedIndex() < 0) {
            ok = false;
            msg += "\n   Material of Charge";
        }
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

    ErrorStatAndMsg isChargeInFceOK() {
        boolean ok = true;
        String msg = "";
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
            if ((chLength * nChargeRows) > width) {
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

            DFHTuningParams.ForProcess proc;
            proc = (DFHTuningParams.ForProcess) cbFceFor.getSelectedItem();

            if (proc == DFHTuningParams.ForProcess.BILLETS) {  // billets
                if (chThickness < 0.01) {
                    retVal &= false;
                    msg += nlSpace + "Check " + tfChThickness.titleAndVal() +
                            " for Furnace for " + DFHTuningParams.ForProcess.BILLETS;
                }
            }

            if (proc == DFHTuningParams.ForProcess.STRIP) {
                if (chThickness > 0.01) {
                    retVal &= false;
                    msg += nlSpace + "Check " + tfChThickness.titleAndVal() +
                            " for Furnace for " + DFHTuningParams.ForProcess.STRIP;
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
            if (bAirHeatedByRecu && !bFuelHeatedByRecu && !furnace.checkExistingRecu()) {
//                if (!decide("Recuperator", "Do you want to use the existing Recuperator?"))
                    furnace.newRecu();
            }
        }
        return retVal;
    }

    protected void enableResultsMenu(boolean enable) {
        resultsMenu.setEnabled(enable);
        printMenu.setEnabled(enable);
        saveToXL.setEnabled(enable && !bDataEntryON);
        saveForTFM.setEnabled(enable && !bDataEntryON);
        if (saveForFE != null)
            saveForFE.setEnabled(enable && !bDataEntryON);
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

    public void calculateForPerformanceTable(Performance baseP) {
        enableResultsMenu(false);
        enableCalculStat();
        Thread evalThread = new Thread(evaluator = new FceEvaluator(this, slate, furnace, calculStep, baseP));
        enablePauseCalcul();
        evalThread.start();
    }

    public boolean evaluate(ThreadController master, double forOutput, double stripWidth) {   // TODO not used
        return furnace.evaluate(master, forOutput, stripWidth);
    }

    protected void calculateFce(boolean bResetLossFactor) {
        initPrintGroups();
        enableResultsMenu(false);
        if (bResetLossFactor) {
            furnace.resetLossFactor();
            takeValuesFromUI();
        }
        while (checkData()) {
            if (furnace.showZoneDataMsgIfRequired(pbCalculate)) {
                if (!commFuel.bMixedFuel && fuelTemp > 0 && !tuningParams.bOnProductionLine
                        && !commFuel.isSensHeatSpecified(this, fuelTemp)) {
                    commFuel.getSpHtData(this, tfFuelTemp);
                }
                furnace.setCommonFuel(new FuelFiring(commFuel, false, excessAir, airTemp, fuelTemp));  // as normal burner
//                theCharge = new Charge(selChMaterial, chLength, chWidth, chThickness);
                theCharge = new Charge(selChMaterial, chLength, chWidth, chThickness, chDiameter, (Charge.ChType)cbChType.getSelectedItem());
                production = new ProductionData();
                production.setCharge(theCharge, chPitch);
                production.setProduction(tph * 1000, nChargeRows, entryTemp, exitTemp, deltaTemp, bottShadow);
                production.setExitZoneTempData(exitZoneFceTemp, minExitZoneFceTemp);
                furnace.setProduction(production);
                if (evaluator != null)
                    if (evaluator.stopped)
                        evaluator = null;
                if (evaluator == null) {
                    enableCalculStat();

                    Thread evalThread = new Thread(evaluator = new FceEvaluator(this, slate, furnace, calculStep));
                    enablePauseCalcul();
                    evalThread.start();
                } else
                    showError("Earlier Calculation is still ON!");
            }
            break;
        }
    }

    protected void calculateFce() {
        calculateFce(true);
    }

    void enableCalculStat() {
        statMenu.setEnabled(true);
        fileMenu.setEnabled(false);
        inputMenu.setEnabled(false);
        enableDataEntry(false);
        progressP.setEnabled(false);
        statMenu.setEnabled(false);
    }

    void enablePauseStat() {
        enableDataEntry(false);
        inputMenu.setEnabled(true);
        progressP.setEnabled(true);
    }

    public void pausingCalculation(boolean paused) {
        if (paused)
            enablePauseStat();
        else
            enableCalculStat();
    }

    DFHTuningParams.ForProcess forProcess() {
        return proc;
    }

    public void abortingCalculation() {
        evaluator = null;
        enableDataEntry(true);
        inputMenu.setEnabled(true);
        statMenu.setEnabled(false);
        enableResultsMenu(false);
//        resultsMenu.setEnabled(false);
//        printMenu.setEnabled(false);
        fileMenu.setEnabled(true);
//        switchPage(InputType.INPUTPAGE);
        showError("ABORTING CALCULATION!");
        switchPage(InputType.INPUTPAGE);
        parent().toFront();
    }

    void enablePauseCalcul() {
        progressP.setEnabled(false);
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
//                XMLmv.putTag("cbHeatingType", "" + cbHeatingType.getSelectedItem()) + "\n" +
                XMLmv.putTag("cbHeatingType", "" + cbHeatingMode.getSelectedItem()) + "\n" +
                XMLmv.putTag("width", "" + width) +
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

    public String takeDataFromXML(String xmlStr) {
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
//        debug("Rev20120909 10:55 After getting DatTitle");
        if (vp.val.equals(DFHversion)) {
            vp = XMLmv.getTag(xmlStr, "DFHeating", 0);
            String acTData = vp.val;
            debug("acTData.length() = " + acTData.length());
            if (acTData.length() > 1300) {
                aBlock:
                {
                    vp = XMLmv.getTag(acTData, "reference", 0);
                    reference = vp.val;
                    debug("reference = " + vp.val);
                    vp = XMLmv.getTag(acTData, "fceTitle", 0);
                    fceTtitle = vp.val;
                    vp = XMLmv.getTag(acTData, "customer", 0);
                    debug("customer = " + vp.val);

                    customer = vp.val;

                    try {
                        vp = XMLmv.getTag(acTData, "width", 0);
                        width = Double.valueOf(vp.val);
                        vp = XMLmv.getTag(acTData, "excessAir", 0);
                        excessAir = Double.valueOf(vp.val);
                    } catch (Exception e) {
                        excessAir = 0.1;
                    }
                    fceFor1stSwitch = true; // to disable fceFor switch warning
                    vp = XMLmv.getTag(acTData, "cbFceFor", 0);
                    debug("before ForProcess.getEnum, cbFceFor = " + vp.val);
                    DFHTuningParams.ForProcess forProc = DFHTuningParams.ForProcess.getEnum(vp.val);
//                    debug("forProc = " + forProc + ", mainF =" + mainF + ", cbFceFor = " + cbFceFor);
                    if (forProc != null)
                        cbFceFor.setSelectedItem(forProc);
                    debug("Before cbHeatingType");
                    vp = XMLmv.getTag(acTData, "cbHeatingType", 0);
                    debug("Before cbHeatingType.setSelectedItem");
                    setHeatingMode(vp.val);
//                    cbHeatingType.setSelectedItem(vp.val);
                    debug("Before cbFuel");
                    debug("Before chargeData");
                    vp = XMLmv.getTag(acTData, "chargeData", 0);
                    grpStat = chDataFromXML(vp.val);
                    if (!grpStat.allOK) {
                        errMsg += "In Getting Charge Data: \n" + grpStat.errMsg;
                        allOK = false;
                    }
                    setFcefor(false);
                    setChargeSizeChoice();
                    debug("Before recuData");
                    vp = XMLmv.getTag(acTData, "recuData", 0);
                    grpStat = recuDataFromXML(vp.val, bFromTFM);
                    if (!grpStat.allOK) {
                        errMsg += "In Getting recuData: \n" + grpStat.errMsg;
                        allOK = false;
                    }
/*
                    debug("Before Production Data");
                    vp = XMLmv.getTag(acTData, "productionData", 0);
                    grpStat = productionFromXML(vp.val, bFromTFM);
                    if (!grpStat.allOK) {
                        errMsg += "In Getting Production Data: \n" + grpStat.errMsg;
                        allOK = false;
                    }
                    debug("before calculData");
                    vp = XMLmv.getTag(acTData, "calculData", 0);
                    grpStat = calculDataFromXML(vp.val, bFromTFM);
                    if (!grpStat.allOK) {
                        errMsg += "In Getting Calculation Data: \n" + grpStat.errMsg;
                        allOK = false;
                    }
*/
                    debug("Before Tuning");
                    if (bFromTFM) {
                        debug("Not reading TUNING data from TFM");
                    } else {
                        vp = XMLmv.getTag(acTData, "tuning", 0);
                        if (!tuningParams.takeDataFromXML(vp.val)) {
                            allOK = false;
                            errMsg += "   in Tuning data Data\n";
                        }
                    }
/*
                    debug("Before setValuesToUI()");
                    setValuesToUI();
*/
                    debug("Before furnace");
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
                    debug("Before Production Data");
                    vp = XMLmv.getTag(acTData, "productionData", 0);
                    grpStat = productionFromXML(vp.val, bFromTFM);
                    if (!grpStat.allOK) {
                        errMsg += "In Getting Production Data: \n" + grpStat.errMsg;
                        allOK = false;
                    }
                    debug("before calculData");
                    vp = XMLmv.getTag(acTData, "calculData", 0);
                    grpStat = calculDataFromXML(vp.val, bFromTFM);
                    if (!grpStat.allOK) {
                        errMsg += "In Getting Calculation Data: \n" + grpStat.errMsg;
                        allOK = false;
                    }
                    debug("Before setValuesToUI()");
                    setValuesToUI();
                } // aBlock
            } else {
                allOK = false;
                errMsg += "   in XML Data Length\n";
            }
            if (allOK) {
                return "OK";
            } else {
                debug("ERROR: " + errMsg);
                return errMsg;
            }
        } else
            return "ERROR: " + "Version ! (" + vp.val + ")";
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
//            try {
            vp = XMLmv.getTag(xmlStr, "deltaTflue", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "deltaTflue", grpStat)).allOK)
                deltaTflue = dblWithStat.val;
//                deltaTflue = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "deltaTAir", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "deltaTAir", grpStat)).allOK)
                deltaTAirFromRecu = dblWithStat.val;
//                deltaTAirFromRecu = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "maxFlueAtRecu", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "maxFlueAtRecu", grpStat)).allOK)
                maxFlueAtRecu = dblWithStat.val;
//                maxFlueAtRecu = Double.valueOf(vp.val);
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
                //                deltaTFuelFromRecu = Double.valueOf(vp.val);
                //            } catch (Exception e) {
                //                bRetVal = false;
                //            }
            }
        }
        return grpStat;
    }

    XMLgroupStat productionFromXML(String xmlStr, boolean bFromTFM) {
        XMLgroupStat grpStat = new XMLgroupStat();
        DoubleWithErrStat dblWithStat;
        ValAndPos vp;
        if (cbFceFor.getSelectedItem() != DFHTuningParams.ForProcess.STRIP) {
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
        vp = XMLmv.getTag(xmlStr, "chPitch", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "chPitch", grpStat)).allOK)
            chPitch = dblWithStat.val;
//        vp = XMLmv.getTag(xmlStr, "nChargeRows", 0);
//        if ((dblWithStat = new DoubleWithErrStat(vp.val, "nChargeRows", grpStat)).allOK)
//            nChargeRows= (int)dblWithStat.val;
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
            String shadowStr = vp.val;
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
//        try {
        vp = XMLmv.getTag(xmlStr, "ambTemp", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "ambTemp", grpStat)).allOK)
            ambTemp = dblWithStat.val;
//            ambTemp = Double.valueOf(vp.val);
        if (!bFromTFM) {
            vp = XMLmv.getTag(xmlStr, "airTemp", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "airTemp", grpStat)).allOK)
                airTemp = dblWithStat.val;
            //            airTemp = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "fuelTemp", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "fuelTemp", grpStat)).allOK)
                fuelTemp = dblWithStat.val;
            //            if (vp.val.length() > 0)
            //                fuelTemp = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "calculStep", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, "calculStep", grpStat)).allOK)
                calculStep = dblWithStat.val;
            //            calculStep = Double.valueOf(vp.val);
            //        } catch (Exception e) {
            //            bRetVal = false;
            //        }
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

    public ChMaterial chMatFromName(String name) {
        ChMaterial f = null;
        boolean found = false;
        for (int i = 0; i < vChMaterial.size(); i++) {
            f = vChMaterial.get(i);
            if (f.name.equalsIgnoreCase(name.trim())) {
                found = true;
                break;
            }
        }
        return (found) ? f : null;
    }

    protected boolean bResultsReady = false;

    public void setResultsReady(boolean bReady) {
        bResultsReady = bReady;
    }

//    public void resultsReady() {
//        resultsReady(new Observations());
//    }

    public void resultsReady(Observations observations) {
        setResultsReady(true);
        if (proc == DFHTuningParams.ForProcess.STRIP) {
            enableCompareMenu(true);
            enableSaveForComparison(true);
        }
        enableResultsMenu(true);
        statMenu.setEnabled(false);
        showResultsPanel("" + DFHResult.Type.HEATSUMMARY);
        enableDataEntry(false);
        fileMenu.setEnabled(true);
        inputMenu.setEnabled(true);
        if (fuelMixP != null && furnace.anyMixedFuel())
            addResult(DFHResult.Type.FUELMIX, fuelMixP);
        evaluator = null;
        pbEdit.setEnabled(true);
        pbEdit.getModel().setPressed(false);
        if (observations.isAnyThere())
            showMessage("Some observations on the results: \n" + observations, 5000);
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
//                case FUELMIX:
                    heatBalances.add(new PanelAndName(panel, "" + type));
                    break;
//                case TEMPTRENDS:
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

    public String getSaveFilePath(String title, String ext) {
        String filePath = "";
        FileDialog fileDlg = new FileDialog(mainF, title, FileDialog.SAVE);
        fileDlg.setFile("*" + ext);
        fileDlg.setVisible(true);

        String bareFile = fileDlg.getFile();
        if (!(bareFile == null)) {
            int len = bareFile.length();
            int extLen = ext.length();
            if ((len <= extLen) || !(bareFile.substring(len - extLen).equalsIgnoreCase(ext))) {
                showMessage("Adding '" + ext + "' to file name");
                bareFile = bareFile + ext;
            }
            filePath = fileDlg.getDirectory() + bareFile;
        }
        return filePath;
    }

    public String getReadFilePath(String title, String ext) {
        String filePath = "";
        FileDialog fileDlg = new FileDialog(mainF, title, FileDialog.LOAD);
        fileDlg.setFile(ext);
        fileDlg.setVisible(true);
        String fileName = fileDlg.getFile();
        if (fileName != null) {
            filePath = fileDlg.getDirectory() + fileName;
        }
        return filePath;
    }

    public String resultsInCVS() {
        return furnace.resultsInCVS();
    }

    //region Message functions
    public boolean decide(String title, String msg) {
        int resp = JOptionPane.showConfirmDialog(parent(), msg, title, JOptionPane.YES_NO_OPTION);
        if (resp == JOptionPane.YES_OPTION)
            return true;
        else
            return false;
    }

    public void showError(String msg) {
        error(msg);
        JOptionPane.showMessageDialog(parent(), msg, "ERROR", JOptionPane.ERROR_MESSAGE);
        parent().toFront();
    }

    public void showMessage(String msg) {
        info(msg);
        JOptionPane.showMessageDialog(parent(), msg, "FOR INFORMATION", JOptionPane.INFORMATION_MESSAGE);
        parent().toFront();
    }

    public static void error(String msg) {
        if (log != null)
            log.error("DFHeating:" + msg);
    }

    public static void info(String msg) {
        if (log != null)
            log.info("DFHeating:" + msg);
    }

    void showMessage(String msg, int forTime) {
        JOptionPane pane = new JOptionPane(msg, JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = pane.createDialog(parent(), "FOR INFORMATION");
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new CloseDialogTask(dialog), forTime);
        dialog.setVisible(true);
    }

    class CloseDialogTask extends TimerTask {
        JDialog dlg;
        CloseDialogTask(JDialog dlg) {
            this.dlg = dlg;
        }

        public void run() {
            dlg.setVisible(false);
        }
    }


    void testFunctions() {
        debug("In testFunctions");
        String s = "";
        s = s.trim();
        debug("s = [" + s + "]");
    }

    void saveFceToFile(boolean withPerformance) {
        takeValuesFromUI();
//        String xmlData = dataInXML(withPerformance);
        String title = "Save DFH Furnace Data" + ((withPerformance)? " (with Performance Data)" : "");
        FileDialog fileDlg =
                new FileDialog(mainF, title,
                        FileDialog.SAVE);
        fileDlg.setFile("*.dfhDat");
        fileDlg.setVisible(true);

        String bareFile = fileDlg.getFile();
        if (!(bareFile == null)) {
            int len = bareFile.length();
            if ((len < 8) || !(bareFile.substring(len - 7).equalsIgnoreCase(".dfhDat"))) {
                showMessage("Adding '.dfhDat' to file name");
                bareFile = bareFile + ".dfhDat";
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

    boolean getFceFromFile() {
        disableCompare();
        boolean bRetVal = false;
        furnace.resetSections();
        FileDialog fileDlg =
                new FileDialog(mainF, "Read DFH Furnace Data",
                        FileDialog.LOAD);
        fileDlg.setFile("*.dfhDat; *.xls");
        fileDlg.setVisible(true);
        String fileName = fileDlg.getFile();
        if (fileName != null) {
            boolean bXL = (fileName.length() > 4) && (fileName.substring(fileName.length() - 4).equalsIgnoreCase(".xls"));
            String filePath = fileDlg.getDirectory() + fileName;
            if (!filePath.equals("nullnull")) {
                setResultsReady(false);
                setItFromTFM(false);
                furnace.resetLossAssignment();
                hidePerformMenu();
//                furnace.clearPerfBase();
                debug("Data file name :" + filePath);
                if (bXL) {
                    String fceData = fceDataFromXL(filePath);
                    if (checkVersion(fceData)) {
                        String msg = takeDataFromXML(fceData);
                        if ((msg.equals("OK"))) {
                            bRetVal = true;
                            parent().toFront();
                            if (!onProductionLine)
                                showMessage("Fuel and Charge Material to be Selected/Checked before Calculation.");
                        } else
                            showError("in Furnace Data from XL file!\n" + msg);
                    } else
                        showError("This file does not contain proper DFHFurnace data!");
                } else {
                    bRetVal = getFceFromFceDatFile(filePath);
                }
            }
        }
        switchPage(InputType.INPUTPAGE);
        return bRetVal;
    }

    public boolean saveRecuToFile(String xmlStrRecu) {
        boolean retVal = false;
        String fileMsg = "# Recuperator Data saved on " + dateFormat.format(new Date()) + "\n" +
                         "# The data can be modified by Knowledgeable User. The total responsibility is his.\n" +
                         "# No heat balance check is done by the program.\n" +
                         "# the parameters 'fFBase' and 'hTaBase' are not read by the program, \n" +
                         "# and leave them as they are.\n\n\n";

        String xmlStr = fileMsg + XMLmv.putTag("Recuperator", xmlStrRecu);
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
        parent().toFront();
        return retVal;
    }

    void loadRecuperator() {
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
                    //           FileInputStream iStream = new FileInputStream(fileName);
                    File f = new File(filePath);
                    long len = f.length();
                    if (len > 50 && len < 1000) {
                        int iLen = (int) len;
                        byte[] data = new byte[iLen + 10];
                        if (iStream.read(data) > 50) {
                            if (furnace.setRecuSpecs(new String(data)))
                                showMessage("Recuperator loaded");
                        }
                    } else
                        showError("File size " + len + " for " + filePath);
                } catch (Exception e) {
                    showError("Some Problem in getting file!");
                }
            }
        }
     }

    protected boolean getFceFromFceDatFile(String filePath) {
        boolean bRetVal = false;
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
                        String stat = takeDataFromXML(new String(data));
                        if (stat.equals("OK")) {
                            bRetVal = true;
                            parent().toFront();
                            if (!onProductionLine)
                                showMessage("Fuel and Charge Material to be Selected/Checked before Calculation.");
                        } else
                            showError(stat);
                    } else
                        showError("This not a proper DFHFurnace data file!");
                }
            } else
                showError("File size " + len + " for " + filePath);
        } catch (Exception e) {
            showError("Some Problem in getting file!");
        }
        return bRetVal;
    }


    void setItFromTFM(boolean bYes) {
        beamParamTFM.setEnabled(bYes);
        lossParamTFM.setEnabled(bYes);
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
        debug("in invalidateResults");
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
    //endregion

    void tProfileForTFM() {
        String profStr = furnace.tProfileForTFMWithLen(); //(false);
        if (profStr.length() > 100) {
            FileDialog fileDlg =
                    new FileDialog(mainF, "Temperature Profile for TFM",
                            FileDialog.SAVE);
            fileDlg.setFile("Top Temperature Profile for TFM.csv");
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
            parent().toFront();
        }
    }

    void saveAmbForFE() {
        String ambStr = furnace.dataForFE();
        if (ambStr.length() > 100) {
            FileDialog fileDlg =
                    new FileDialog(mainF, "Furnace Ambients for FE Analysis",
                            FileDialog.SAVE);
            fileDlg.setFile("Furnace Ambeint.amb");
            fileDlg.setVisible(true);
            String bareFile = fileDlg.getFile();
            if (bareFile != null) {
                int len = bareFile.length();
                if ((len < 4) || !(bareFile.substring(len - 4).equalsIgnoreCase(".amb"))) {
                    showMessage("Adding '.amb' to file name");
                    bareFile = bareFile + ".amb";
                }
                String fileName = fileDlg.getDirectory() + bareFile;
                try {
                    BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(fileName));
                    oStream.write(ambStr.getBytes());
                    oStream.close();
                } catch (Exception e) {
                    showError("Some problem in file.\n" + e.getMessage());
                    return;
                }
            }
            parent().toFront();
        }
    }

    void saveComparisonToXL() {
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
//  create a new workbook
            Workbook wb = new HSSFWorkbook();
            int nSheet = 0;
//  create a new sheet
            ExcelStyles styles = new ExcelStyles(wb);
            Sheet sh = prepareReportWB(wb, styles);
            furnace.xlComparisonReport(sh, styles);
            try {
                wb.write(out);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                showError("Some problem with file.\n" + e.getMessage());
            }
        }
        parent().toFront();
    }

    void clearComparisonTable() {
        furnace.clearComparisonTable();
        showComparison.setEnabled(false);
        saveComparisontoXL.setEnabled(false);
        appendComparisontoXL.setEnabled(false);
        clearComparison.setEnabled(false);
        switchPage(InputType.INPUTPAGE);
    }

    void saveForComparison() {
        furnace.saveForComparison();
        clearComparison.setEnabled(true);
    }

    void appendToComparisonToXL() {
        FileOutputStream out = null;
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
            clearPerfBase.setEnabled(true);
            createPerfBase.setEnabled(false);
            showPerfBase.setEnabled(true);
        }
    }

    void addToPerformBase() {
        furnace.addToPerfBase();
    }

    void performBaseReady() {
        createPerfBase.setEnabled(false);
        addToPerfBase.setEnabled(false);
        clearPerfBase.setEnabled(true);
        perfMenu.setEnabled(true);
        perfMenu.setVisible(true);
//        saveFceAndPerf.setEnabled(true);
    }

    public void clearPerformBase() {
        if (decide("Performance Base", "Do you want to DELETE ALL Performance Data?")) {
            furnace.clearPerfBase();
//        createPerfBase.setEnabled(true);
            addToPerfBase.setEnabled(false);
            showPerfBase.setEnabled(false);
        }
//        saveFceAndPerf.setEnabled(false);
    }

    void excelResultsFile() {
//  create a new file
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
            try {
                wb.write(out);
                out.close();
                furnace.performaceIsSaved();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                showError("Some problem with file.\n" + e.getMessage());
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
        FileOutputStream out = null;
        FileDialog fileDlg =
                new FileDialog(mainF, "Saving Fuel Specifications to file",
                        FileDialog.SAVE);
        fileDlg.setFile("FuelSpecifications.dfhSpecs");
        fileDlg.setVisible(true);
        String bareFile = fileDlg.getFile();
        int byteCount = 0;
        String theData = "";
        if (bareFile != null) {
            int len = bareFile.length();
            if ((len < 9) || !(bareFile.substring(len - 9).equalsIgnoreCase(".dfhSpecs"))) {
                showMessage("Adding '.' to file name");
                bareFile = bareFile + ".dfhSpecs";
            }
            String fileName = fileDlg.getDirectory() + bareFile;
            try {
                BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(fileName));
                oStream.write(("# Fuel specifications saved on " + dateFormat.format(new Date()) + "\n\n").getBytes());
                oStream.write(fuelSpecsInXML().getBytes());
                oStream.close();
            } catch (Exception e) {
                showError("Some problem in saving Fuel specs.\n" + e.getMessage());
                return;
            }
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
            ;
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
                oStream.write(chMaterialSpecsInXML().getBytes());
                oStream.close();
            } catch (Exception e) {
                showError("Some problem in saving Charge Material specs.\n" + e.getMessage());
                return;
            }
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
            ;
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


    protected enum InputType {
        INPUTPAGE, OPPAGE, FUELMIX, REGENSTUDY, TUNINGPAGE, PROGRESSPAGE, BEAMSPAGE, LOSSPARAMSTFM;
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

        void removePanel() {
            mI.setEnabled(false);
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

    class winListener implements WindowListener {
        public void windowOpened(WindowEvent e) {
        }

        public void windowClosing(WindowEvent e) {
            debug("mainF CLOSING");
            destroy();
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
            String command = e.getActionCommand();
            menuBlk:
            {
                if (command.equals("Exit")) {
                    close();
                    break menuBlk;
                }
                if (command.equals("Save Results and Furnace Data to Excel")) {
                    excelResultsFile();
                    break menuBlk;
                }

                if (command.equals("Save Temperature Profile for TFM")) {
                    tProfileForTFM();
                    break menuBlk;
                }

                if (command.equals("Save Furnace Ambients for FE Analysis")) {
                    saveAmbForFE();
                    break menuBlk;
                }

                if (command.equals("Save Furnace Profile")) {
                    Component lastShown = slate.getViewport().getView();
//                    saveFceToFile(false);
                    saveFceToFile(true);
                    parent().toFront();
                    slate.setViewportView(lastShown);
                    break menuBlk;
                }

//                if (command.equals("Save Furnace And Performance")) {
//                    Component lastShown = slate.getViewport().getView();
//                    saveFceToFile(true);
//                    parent().toFront();
//                    slate.setViewportView(lastShown);
//                    break menuBlk;
//                }

                if (command.equals("Get Furnace Profile")) {
                    boolean goAhead = true;
                    if (furnace.isPerformanceToBeSaved()) {
                        goAhead = decide("Unsaved Performance Data", "Some Performance data have been collected\n" +
                                    "Do you want to ABANDON them and load a new furnace ?");
                    }
                    if (goAhead) {
                        pbEdit.doClick();
                        boolean response = getFceFromFile();
                        parent().toFront();
                        if (response) {
                            switchPage(InputType.INPUTPAGE);
                            enableDataEntry(true);
                        }
                    }
                    break menuBlk;
                }
                if (command.equals("Load Recuperator Specs.")) {
                    loadRecuperator();
                    break menuBlk;
                }
                if (command.equals("Save Fuel Specifications to File")) {
                    saveFuelSpecs();
                    break menuBlk;
                }

                if (command.equals("Save Steel Specifications to File")) {
                    saveSteelSpecs();
                    break menuBlk;
                }

                if (command.equals("Input Data")) {
                    switchPage(InputType.INPUTPAGE);
                    break menuBlk;
                }
                if (command.equals("Operation Data")) {
                    switchPage(InputType.OPPAGE);
                    break menuBlk;
                }
                if (command.equals("Show Progress")) {
                    switchPage(InputType.PROGRESSPAGE);
                    break menuBlk;
                }
                if (command.equals("Tuning Parameters")) {
                    switchPage(InputType.TUNINGPAGE);
                    break menuBlk;
                }
                if (command.equals("Walking Beam Params from TFM")) {
                    switchPage(InputType.BEAMSPAGE);
                    break menuBlk;
                }

                if (command.equals("Loss Params from TFM")) {
                    switchPage(InputType.LOSSPARAMSTFM);
                    break menuBlk;
                }
                if (command.equals("Create Fuel Mix")) {
                    switchPage(InputType.FUELMIX);
                    break menuBlk;
                }
                if (command.equals("Regen Burner Study"))
                    switchPage(InputType.REGENSTUDY);
                break menuBlk;
            }
        } // actionPerformed
    } // class MenuActions

    class CompareMenuListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (src == saveComparison)
                saveForComparison();
            if (src == showComparison)
                switchPage(furnace.getComparePanel());
            if (src == saveComparisontoXL)
                saveComparisonToXL();
            if (src == appendComparisontoXL)
                appendToComparisonToXL();
            if (src == clearComparison)
                clearComparisonTable();
        }
    }

    class PerformListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (src == createPerfBase) {
                createPerformBase();
            }
            if (src == addToPerfBase) {
                addToPerformBase();
            }
            if (src == showPerfBase) {
                switchPage(furnace.getPerfBaseListPanel());
            }
            if (src == clearPerfBase) {
                clearPerformBase();
            }
            if (src == setPerfTablelimits) {
                tuningParams.getPerfTableSettings(setPerfTablelimits);
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

    class RadioListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            changeTopBot(e.getActionCommand());
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
                debug("In length Change Listener, jText.isEditable() = " + jText.isEditable());
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
                pageWidth /= factor;
                pageHeight /= factor;
            }
            RepaintManager currentManager =
                    RepaintManager.currentManager(toPrint);
            currentManager.setDoubleBufferingEnabled(false);

            toPrint.print(graphics);
            currentManager.setDoubleBufferingEnabled(true);

            return Printable.PAGE_EXISTS;
        }
    }

    protected static String readInput(boolean useCmdSequence) {
    		// You can provide "commands" already from the command line, in which
    		// case they will be kept in cmdSequence
    		BufferedReader stdin = new BufferedReader(new InputStreamReader(
    				System.in));
    		String s = null;
    		do
    			try {
    				s = stdin.readLine();
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
    		while ((s == null) || (s.length() == 0));
    		return s;
    	}

    static protected boolean parseCmdLineArgs(String[] args) {
        boolean retVal = true;
        CommandLineArgs cmdArg;
        onProductionLine = true;
        for (int a = 0; a < args.length; a++) {
            cmdArg = CommandLineArgs.getEnum(args[a]);
            if (cmdArg != null)
                switch(cmdArg) {
                    case ONTEST:
                        onTest = true;
                        break;
                    case ALLOWSPECSSAVE:
                        enableSpecsSave = true;
                        break;
                    case NOTLEVEL2:
                        onProductionLine = false;
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
//        boolean onProductionLine = true;
//
//        CommandLineArgs cmdArg;
//        for (int a = 0; a < args.length; a++) {
//            cmdArg = CommandLineArgs.getEnum(args[a]);
//            if (cmdArg != null)
//                switch(cmdArg) {
//                    case ONTEST:
//                        onTest = true;
//                        break;
//                    case ALLOWSPECSSAVE:
//                        enableSpecsSave = true;
//                        break;
//                    case NOTLEVEL2:
//                        onProductionLine = false;
//                        break;
//                }
//        }
//        final DFHeating trHeat = new DFHeating(true, onProductionLine);


//        trHeat.setVisible(true);

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








