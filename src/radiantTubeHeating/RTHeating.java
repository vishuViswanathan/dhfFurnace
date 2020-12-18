package radiantTubeHeating;

import directFiredHeating.DFHResult;
import display.HelpSystem;
import display.OnePropertyTrace;
import basic.ChMaterial;
import basic.Charge;
import jsp.JSPchMaterial;
import mvUtils.display.*;
import mvUtils.jsp.JSPComboBox;
import mvUtils.jsp.JSPConnection;
import mvUtils.jsp.JSPObject;
//import netscape.javascript.JSObject;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLgroupStat;
import mvUtils.mvXML.XMLmv;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import protection.CheckAppKey;


import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Vector;


/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/19/12 Modified 30 Sep 2020
 * Time: 1:28 PM
 * To change this template use File | Settings | File Templates.
 */

public class RTHeating extends JPanel implements InputControl {
    public enum LimitMode {
        RTTEMP("RT Temperature"),
        RTHEAT("RT Heat Release");

        private final String modeName;

        LimitMode(String modeName) {
            this.modeName = modeName;
        }

        public String getValue() {
            return name();
        }

        @Override
        public String toString() {
            return modeName;
        }

        public static LimitMode getEnum(String text) {
            LimitMode retVal = null;
            if (text != null) {
                for (LimitMode b : LimitMode.values()) {
                    if (text.equalsIgnoreCase(b.modeName)) {
                        retVal = b;
                        break;
                    }
                }
            }
            return retVal;
        }
    }

    public enum ResultsType {
        ZONE1RESULTS("Zone #1 - Results"),
        ZONE2RESULTS("Zone #2 - Results"),
        ZONE3RESULTS("Zone #3 - Results"),
        ZONE4RESULTS("Zone #4 - Results"),
        ZONE5RESULTS("Zone #5 - Results"),
        ZONE6RESULTS("Zone #6 - Results"),
        ALLZONERESULTS("All Zones - Results");
//        HEATSUMMARY("Heat Balance - Summary");

        private final String resultName;

        ResultsType(String resultName) {
            this.resultName = resultName;
        }

        public static ResultsType getZoneEnum(int zNum) {
            switch(zNum) {
                case 1:
                    return ResultsType.ZONE1RESULTS;
                case 2:
                    return ResultsType.ZONE2RESULTS;
                case 3:
                    return ResultsType.ZONE3RESULTS;
                case 4:
                    return ResultsType.ZONE4RESULTS;
                case 5:
                    return ResultsType.ZONE5RESULTS;
                case 6:
                    return ResultsType.ZONE6RESULTS;
                default:
                    return null;
            }
        }

        public String resultName() {
            return resultName;
        }

        public static ResultsType getEnum(String text) {
            if (text != null) {
                for (ResultsType b : ResultsType.values()) {
                    if (text.equalsIgnoreCase(b.resultName)) {
                        return b;
                    }
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return resultName;    //To change body of overridden methods use File | Settings | File Templates.
        }
    }


    protected enum RTHDisplayPage {
        INPUTPAGE, TRENDSPAGE, RESULTSTABLEPAGE
    }

    String title = "Radiant Tube Heated Furnace 202011218"; // was 20201025"; // was 20201017";
    public int appCode = 101;
    boolean canNotify = true;
    JFrame mainF;
    String cvs;
    String jspBase = "HYPWAP02:9080/fceCalculations/jsp/";
    public int maxNzones = 6;
    JPanel inpPage;
    JPanel resultsPage;
    JPanel trendsPage;
    protected JScrollPane slate = new JScrollPane();
    public JButton pbEdit;
    public JMenu helpMenu;
    public JSPConnection jspConnection;

    public RTHeating() {
    }

    public boolean setItUp() {
        boolean retVal = false;
        if (getJSPbase() && getJSPConnection()) {
            DataWithStatus<Boolean> runCheck = new CheckAppKey(jspBase).canRunThisApp(appCode, true);
            if (runCheck.getStatus() == DataStat.Status.OK) {
                setUIDefaults();
                mainF = new JFrame(title);
                mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                mainF.addWindowListener(new WinListener());
                setMenuOptions();
//        setTestData();
                mainF.add(slate);
                vChMaterial = new Vector<ChMaterial>();
                setDefaultFurnace();
                if (loadChMaterialData()) {
                    chargePan(this);
                    inpPage = theFurnace.inputPage(this, chargeP);
                    switchToInputPage();
                    mainF.pack();
                    mainF.setVisible(true);
                    retVal = true;
                }
            } else {
                if (runCheck.getStatus() == DataStat.Status.WithErrorMsg)
                    showError("Access Check", runCheck.getErrorMessage());
                else
                    showError("Access Check", "Some problem in getting Application permissions");
            }
        } else
            showError("Access Check", "Unable to connect to Server");
        return retVal;
    }

    private void setDefaultFurnace() {
        theFurnace = new RTFurnace(this);
    }

    protected void setUIDefaults() {
        UIManager.put("ComboBox.disabledForeground", Color.black);
        UIManager.put("Label.disabledForeground", Color.black);
        Font oldLabelFont = UIManager.getFont("Label.font");
        UIManager.put("Label.font", oldLabelFont.deriveFont(Font.PLAIN));
        oldLabelFont = UIManager.getFont("ComboBox.font");
        UIManager.put("ComboBox.font", oldLabelFont.deriveFont(Font.PLAIN + Font.ITALIC));
        modifyJTextEdit();
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

    public void switchToSelectedPage(JPanel page) {
        slate.setViewportView(page);
    }

    public void switchToInputPage() {
        slate.setViewportView(inpPage);
    }

    protected boolean getJSPbase() {
        boolean retVal = false;
        String jspBaseIDPath = "jspBase.txt";
        File jspBaseFile = new File(jspBaseIDPath);
        long len = jspBaseFile.length();
        if (len > 5 && len < 100) {
            int iLen = (int) len;
            byte[] data = new byte[iLen + 1];
            try {
                BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(jspBaseFile));
                if (iStream.read(data) > 5) {
                    String svr = new String(data).trim();
                    debug("svr: " + svr);
                    jspBase = svr + ":9080/fceCalculations/jsp/";
                    iStream.close();
                    retVal = true;
                }
            } catch (IOException e) {
                ;
            }
        }
        return retVal;
    }

    protected boolean getJSPConnection() {
        boolean retVal = false;
        try {
            jspConnection = new JSPConnection(jspBase);
            retVal = true;
        } catch (Exception e) {
            System.out.println("RTHeating.234" + e.getMessage());
        }
        return retVal;
    }

    JMenu fileMenu;
    JMenu inputMenu;
    JMenu resultsMenu;
    protected JMenuItem mIGetFceProfile;
    JMenuItem mISaveToXL;
    JMenuItem mIExit;
    JMenuItem mItrends;
    JMenuItem mItrandList;
    Hashtable<ResultsType, ResultPanel>  resultsPanels;
    boolean resultsReady = false;

    void setMenuOptions() {
        JMenuBar mb = new JMenuBar();
//        mb.setLayout(new GridLayout(1, 0));
        OnePropertyTrace oneT;
        MenuActions mAction = new MenuActions();
        fileMenu = new JMenu("File");
//        mI = new JMenuItem("Save Results");
//        mI.addActionListener(mAction);
//        fileMenu.add(mI);
//        mI = new JMenuItem("Retrieve Results");
//        mI.addActionListener(mAction);
//        fileMenu.add(mI);
//        fileMenu.addSeparator();
        mIExit = new JMenuItem("Exit");
        mIExit.addActionListener(mAction);
        mIGetFceProfile = new JMenuItem("Load Furnace");
        mIGetFceProfile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));
        mIGetFceProfile.addActionListener(mAction);
        mIGetFceProfile.setEnabled(true);
        mISaveToXL = new JMenuItem("Save Furnace Data and Results");
        mISaveToXL.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
        mISaveToXL.addActionListener(mAction);
        mISaveToXL.setEnabled(false);
        fileMenu.add(mIGetFceProfile);
        fileMenu.addSeparator();
        fileMenu.add(mISaveToXL);
        fileMenu.addSeparator();
        fileMenu.add(mIExit);

        mb.add(fileMenu);
        inputMenu = new JMenu("input Data");
        createResultsMenu();
//        resultsMenu = new JMenu("Results");
//        resultsMenu.setEnabled(false);
        MenuListener menuListener = new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                Object src = e.getSource();
                if (src == inputMenu)
                    switchToInputPage();
                else if (src == helpMenu)
                    switchToSelectedPage(helpSystem.getHelpPanel());
            }

            @Override
            public void menuDeselected(MenuEvent e) {

            }

            @Override
            public void menuCanceled(MenuEvent e) {

            }
        };

        inputMenu.addMenuListener(menuListener);
//        inputMenu.setMnemonic('I');
        resultsMenu.addMenuListener(menuListener);
        mb.add(inputMenu);
        mb.add(resultsMenu);
        JLabel dummy = new JLabel("--------------");
        mb.add(dummy);
        pbEdit = new JButton("AllowDataEdit");
        pbEdit.setMnemonic(KeyEvent.VK_E);
        pbEdit.getModel().setPressed(true);
        pbEdit.setEnabled(false);
        pbEdit.addActionListener(e -> {
            theFurnace.enableDataEdit(true);
            switchToInputPage();
        });
        mb.add(pbEdit);
//        helpSystem = new HelpSystem("helpSystem\\RTHeating");
        helpSystem = new HelpSystem(jspConnection, 101, "helpSystem\\RTHeating");
        if (helpSystem.loadHelp()) {
            helpMenu = new JMenu("Help");
            helpMenu.setMnemonic(KeyEvent.VK_H);
            helpMenu.addMenuListener(menuListener);
            mb.add(Box.createHorizontalGlue());
            mb.add(helpMenu);
        }
        mainF.setJMenuBar(mb);
    }

    HelpSystem helpSystem;

    JMenu createResultsMenu() {
        resultsMenu = new JMenu("ViewResults");
        resultsPanels = new Hashtable<ResultsType, ResultPanel>();
        ActionListener al = e -> {
            String command = e.getActionCommand();
            showResultsPanel(command);
        };
        for (ResultsType rType: ResultsType.values()) {
            ResultPanel rp = new ResultPanel(rType.resultName, al);
            resultsPanels.put(rType, rp);
            resultsMenu.add(rp.getMenuItem());

        }
        resultsMenu.setEnabled(false);
        return resultsMenu;
    }

    public void enableDataEdit(boolean ena) {
        ntStripWidth.setEditable(ena);
        ntStripThickness.setEditable(ena);
        ntChDiameter.setEditable(ena);
        ntNitemsAlongFceWidth.setEditable(ena);
        ntWallThickness.setEditable(ena);
        cbChMaterial.setEnabled(ena);
        cbChType.setEnabled(ena);
        ntOutput.setEditable(ena);
//        theFurnace.enableDataEdit(ena);
        pbEdit.getModel().setPressed(ena);
        pbEdit.setEnabled(!ena);
        resultsReady = !ena;
        resultsMenu.setEnabled(!ena);
        mISaveToXL.setEnabled(!ena);
    }

    public void setResultsReady(boolean bReady) {
        resultsMenu.setEnabled(bReady);
    }


    public void resultsReady(ResultsType switchDisplayTo) {
         setResultsReady(true);
         pbEdit.setEnabled(true);
         showResultsPanel(switchDisplayTo);
    }

    void close() {
        mainF.dispose();
        System.exit(0);
    } // close


    class MenuActions implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            menuBlk:
            {
                if (src == mIExit) {
                    close();
                    break menuBlk;
                }
                if (src == mISaveToXL) {
                    excelResultsFile();
                    break menuBlk;
                }
                if (src == mIGetFceProfile) {
                    getFceFromFile();
                    break menuBlk;
                }
            }
        } // actionPerformed
    } // class TraceActionListener

    ChMaterial chMaterial;
    protected XLComboBox cbChType;
    Charge.ChType chType = Charge.ChType.SOLID_RECTANGLE;
    RTFurnace theFurnace;
    public double production = 5000;

    JSPComboBox<ChMaterial> cbChMaterial;
    Vector<ChMaterial> vChMaterial;
    MultiPairColPanel chargeP;
    double stripWidth = 1.25;
    double stripThick = 0.0006;
    double chDiameter = 0.05;
    double wallThickness = 0.005;
    public int nChargeAlongFceWidth = 1;
    NumberTextField ntStripWidth;
    NumberTextField ntStripThickness;
    NumberTextField ntChDiameter;
    NumberTextField ntWallThickness;
    NumberTextField ntNitemsAlongFceWidth;
    boolean chargeFieldsSet = false;

    protected boolean loadChMaterialData() {
        boolean retVal = false;
        if (jspConnection.allOK) {
            Vector<JSPchMaterial> metalListJSP = JSPchMaterial.getMetalList(jspConnection);
            for (JSPchMaterial mat : metalListJSP)
                vChMaterial.add(mat);
            retVal = (vChMaterial.size() > 0);
        }
        return retVal;
    }


    public JPanel chargePan(InputControl ipc) {
        if (!chargeFieldsSet) {
            MultiPairColPanel pan = new MultiPairColPanel("Charge Data");
//            if (loadChMaterialData()) {
            cbChMaterial = new JSPComboBox<>(jspConnection, vChMaterial);
            pan.addItemPair("Charge Material", cbChMaterial);
            cbChType = new XLComboBox(Charge.ChType.values());
            pan.addItemPair("Charge Cross Section", cbChType);
            ntStripWidth = new NumberTextField(ipc, stripWidth * 1000, 6, false,
                    50, 10000, "#,###", "Width, along Fce width (mm)");
            ntStripThickness = new NumberTextField(ipc, stripThick * 1000, 6, false,
                    0.001, 200, "0.000", "Thickness (mm)");
            pan.addItemPair(ntStripWidth);
            pan.addItemPair(ntStripThickness);
            ntChDiameter = new NumberTextField(ipc, chDiameter * 1000, 6, false,
                    5, 1000, "#,###", "Charge Diameter (mm)");
            ntWallThickness = new NumberTextField(ipc, wallThickness * 1000, 6, false,
                    1, 50, "#,###", "Tube Wall Thickness (mm)");
            pan.addItemPair(ntChDiameter);
            pan.addItemPair(ntWallThickness);
            ntNitemsAlongFceWidth = new NumberTextField(ipc, nChargeAlongFceWidth, 6, true,
                    1, 200, "#,###", "Number of items in Fce Width");
            pan.addItemPair(ntNitemsAlongFceWidth);
            cbChType.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    chType = (Charge.ChType) cbChType.getSelectedItem();
                    switch (chType) {
                        case TUBULAR:
                            ntStripWidth.setEnabled(false);
                            ntStripThickness.setEnabled(false);
                            ntChDiameter.setEnabled(true);
                            ntWallThickness.setEnabled(true);
                            break;
                        case SOLID_RECTANGLE:
                            ntStripWidth.setEnabled(true);
                            ntStripThickness.setEnabled(true);
                            ntChDiameter.setEnabled(false);
                            ntWallThickness.setEnabled(false);
                            ntNitemsAlongFceWidth.setData(1);
                            break;
                        case SOLID_CIRCLE:
                            ntStripWidth.setEnabled(false);
                            ntStripThickness.setEnabled(false);
                            ntChDiameter.setEnabled(true);
                            ntWallThickness.setEnabled(false);
                            break;
                    }
                }
            });
            ntOutput = new NumberTextField(ipc, production / 1000, 6, false,
                    0.200, 200000, "#,###.000", "Output (t/h)");
            pan.addBlank();
            pan.addItemPair(ntOutput);

            cbChType.setSelectedItem(chType);
            chargeP = pan;
            chargeFieldsSet = true;
//            }
        }
        return chargeP;
    }

    public ChMaterial getSelChMaterial(String matName) {
        ChMaterial chMat = null;
        for (ChMaterial oneMat : vChMaterial)
            if (matName.equalsIgnoreCase(oneMat.name)) {
                chMat = oneMat;
                if (chMat instanceof JSPObject)
                    ((JSPObject) chMat).collectData(jspConnection);
                break;
            }
        return chMat;
    }

    public double getProduction() {
        if (!ntOutput.isInError())
            production = ntOutput.getData() * 1000;
        else
            production = 0;
        return production;
    }

    public int getNChargeAlongFceWidth() {
        return nChargeAlongFceWidth;
    }


    boolean takeChargeFromUI() {
        chMaterial = (ChMaterial) cbChMaterial.getSelectedItem();
        double effectiveChWidth = 0;
        boolean retVal = (chMaterial != null) && !ntNitemsAlongFceWidth.isInError();
        if (retVal) {
            nChargeAlongFceWidth = (int) (ntNitemsAlongFceWidth.getData());
            switch (chType) {
                case SOLID_RECTANGLE:
                    if (!ntStripWidth.isInError() && !ntStripThickness.isInError()) {
                        stripWidth = ntStripWidth.getData() / 1000;
                        stripThick = ntStripThickness.getData() / 1000;
                        effectiveChWidth = nChargeAlongFceWidth * stripWidth;
                    }
                    break;
                case TUBULAR:
                    if (!ntChDiameter.isInError() && !ntWallThickness.isInError()) {
                        chDiameter = ntChDiameter.getData() / 1000;
                        wallThickness = ntWallThickness.getData() / 1000;
                        effectiveChWidth = nChargeAlongFceWidth * chDiameter;
                    }
                    break;
                case SOLID_CIRCLE:
                    if (!ntChDiameter.isInError()) {
                        chDiameter = ntChDiameter.getData() / 1000;
                        effectiveChWidth = nChargeAlongFceWidth * chDiameter;
                    }
                    break;
            }
            if (effectiveChWidth > 0.95 * theFurnace.width) {
                showError("Cannot accommodate charge in furnace width");
                retVal = false;
            }
        }
        return retVal;
    }

    NumberTextField ntOutput;
    MultiPairColPanel productionPanel;

    public Charge getChargeDetails(double uLen) {
        Charge theCharge = null;
        if (takeChargeFromUI()) {
            switch (chType) {
                case SOLID_RECTANGLE:
                    theCharge = new Charge(chMaterial, uLen, stripWidth, stripThick);
                    break;
                case TUBULAR:
                    theCharge = new Charge(chMaterial, uLen, stripWidth, stripThick,
                            chDiameter, wallThickness, Charge.ChType.TUBULAR);
                    break;
                case SOLID_CIRCLE:
                    theCharge = new Charge(chMaterial, uLen, stripWidth, stripThick,
                            chDiameter, wallThickness, Charge.ChType.SOLID_CIRCLE);
                    break;
            }
        }
        return theCharge;
    }

    void upDateUIfromData() {
        cbChType.setSelectedItem(chType);
        cbChMaterial.setSelectedItem(chMaterial);
        ntStripWidth.setData(stripWidth * 1000);
        ntStripThickness.setData(stripThick * 1000);
        ntChDiameter.setData(chDiameter * 1000);
        ntWallThickness.setData(wallThickness * 1000);
        ntNitemsAlongFceWidth.setData(nChargeAlongFceWidth);
        ntOutput.setData(production / 1000);
    }
    /*
            chMaterial = (ChMaterial) cbChMaterial.getSelectedItem();
        double effectiveChWidth = 0;
        boolean retVal = (chMaterial != null) && !ntNitemsAlongFceWidth.isInError();
        if (retVal) {
            nChargeAlongFceWidth = (int) (ntNitemsAlongFceWidth.getData());

     */

    public String chargeDataToSave() {
        StringBuilder strBuff = new StringBuilder(XMLmv.putTag("chMaterial", "" + chMaterial));
        strBuff.append(XMLmv.putTag("chType", "" + chType));
        strBuff.append(XMLmv.putTag("stripWidth", "" + stripWidth));
        strBuff.append(XMLmv.putTag("stripThick", "" + stripThick));
        strBuff.append(XMLmv.putTag("chDiameter", "" + chDiameter));
        strBuff.append(XMLmv.putTag("wallThickness", "" + wallThickness));
        strBuff.append(XMLmv.putTag("nChargeAlongFceWidth", "" + nChargeAlongFceWidth));
        strBuff.append(XMLmv.putTag("production", "" + production));
        return XMLmv.putTag("ChargeData", "\n" + strBuff);
    }

    String fceDataFromXL(String filePath) {
        String data = "";
        try {
            FileInputStream xlInput = new FileInputStream(filePath);
            Workbook wB;
            if (filePath.toLowerCase().endsWith("xlsx"))
                wB = new XSSFWorkbook(xlInput);
            else
                wB = new HSSFWorkbook(xlInput);
            Sheet sh = wB.getSheet("Furnace Profile");
            if (sh != null) {
                Row r = sh.getRow(0);
                Cell cell = r.getCell(0);
                if (cell.getCellType() == CellType.STRING) {
                    if (cell.getStringCellValue().equals("mv7414")) {
                        r = sh.getRow(1);
                        cell = r.getCell(0);
                        if (cell.getCellType() == CellType.NUMERIC)  { // data is multi column
                            int nCols = (int)cell.getNumericCellValue();
                            for (int c = 1; c <= nCols; c++) {
                                cell = r.getCell(c);
                                if (cell.getCellType() ==CellType.STRING)
                                    data += cell.getStringCellValue();
                            }
                        }
                        else {         // kept for backward compatibility
                            if (cell.getCellType() ==CellType.STRING)
                                data = cell.getStringCellValue();
                        }
                    }
                }
            }
            xlInput.close();
        } catch (Exception e) {
            showError("Error in RTHeating:fceDataFromXL >" + e.getMessage());
        }
        return data;
    }

    StatusWithMessage takeProfileDataFromXml(String xmlStr) {
        StatusWithMessage retVal = new StatusWithMessage();
        XMLgroupStat chStat;
        enableDataEdit(true);
        ValAndPos vp;
        if (xmlStr.length() > 50) {
            try {
                vp = XMLmv.getTag(xmlStr, "ChargeData", 0);
                String chDataXML = vp.val;
                if (chDataXML.length() > 0) {
                    chStat = takeChargeDataFromXml(chDataXML);
                    if (chStat.allOK) {
                        vp = XMLmv.getTag(xmlStr, "theFurnace");
                        if (vp.val.length() > 0) {
                            setDefaultFurnace();
                            retVal = theFurnace.takeFceDataFromXML(vp.val);
                        }
                    }
                    else
                        retVal.addErrorMessage(chStat.errMsg);
                }
            }
            catch(Exception e) {
                retVal.addErrorMessage("Data from xml - Facing some problem Reading Fce Charge data");
            }
        }
        else {
            retVal.addErrorMessage("Data len too short at #717");
        }
        return retVal;
    }

    XMLgroupStat takeChargeDataFromXml(String xmlStr) {
        XMLgroupStat retVal = new XMLgroupStat();
        enableDataEdit(true);
        ValAndPos vp;
        if (xmlStr.length() > 50) {
            try {
                vp = XMLmv.getTag(xmlStr, "chMaterial", 0);
                chMaterial = getSelChMaterial(vp.val);
                vp = XMLmv.getTag(xmlStr, "chType", 0);
                chType = Charge.ChType.getEnum(vp.val);
                vp = XMLmv.getTag(xmlStr, "stripWidth", 0);
                stripWidth = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "stripThick", 0);
                stripThick = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "chDiameter", 0);
                chDiameter = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "wallThickness", 0);
                wallThickness = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "nChargeAlongFceWidth", 0);
                nChargeAlongFceWidth = Integer.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "production", 0);
                production = Double.valueOf(vp.val);
                upDateUIfromData();
            }
            catch (Exception e) {
                retVal.addStat(false, "Reading Charge data from xml - Some Error in data");
            }
        }
        else {
            retVal.addStat(false, "Data size too short at #245");
        }
        return retVal;
    }


    void debug(String msg) {
        System.out.println("RTHeating: " + msg);
    }

    protected StatusWithMessage getFceFromFile() {
        StatusWithMessage retVal = new StatusWithMessage();
        boolean bRetVal = false;
        FileDialog fileDlg =
                new FileDialog(mainF, "Get Profile from Read RTH results File",
                        FileDialog.LOAD);
        fileDlg.setFile("*.xlsx; *.xls");
        fileDlg.setVisible(true);
        String fileName = fileDlg.getFile();
        if (fileName != null) {
            String fS = fileName.toLowerCase();
            String filePath = fileDlg.getDirectory() + fileName;
            if (!filePath.equals("nullnull")) {
                setResultsReady(false);
                String fceData = fceDataFromXL(filePath);
                retVal = takeProfileDataFromXml(fceData);
            }
        }
        if (retVal.getDataStatus() == DataStat.Status.OK) {
            upDateUIfromData();
            inpPage = theFurnace.inputPage(this, chargeP);
            switchToInputPage();
        }
        else
            showError("Reading Furnace from file", retVal.getErrorMessage());
        return retVal;
    }


    void excelResultsFile() {
        if (theFurnace.areAllResultsReady() ||
                decide("Saving Results", "All Zones are NOT calculated \n" +
                                "Still proceed with Save ?")) {
            Workbook wb = new XSSFWorkbook();
            int nSheet = 0;
            //  create a new sheet
            ExcelStyles styles = new ExcelStyles(wb);
            wb.createSheet("Furnace Data");
            theFurnace.xlFurnaceData(wb.getSheetAt(nSheet), styles);
    //        nSheet++;
    //        wb.createSheet("Heat Summary");
    //        furnace.xlHeatSummary(wb.getSheetAt(nSheet), styles);
    //        nSheet++;
    //        wb.createSheet("Sec Summary");
    //        furnace.xlSecSummary(wb.getSheetAt(nSheet), styles, false);
    //        nSheet++;
    //        wb.createSheet("Loss Details");
    //        furnace.xlLossDetails(wb.getSheetAt(nSheet), styles);
    //
            nSheet++;
            wb.createSheet("Temp Profile");
            theFurnace.xlTempProfile(wb.getSheetAt(nSheet), styles);
            nSheet++;
            wb.createSheet("Furnace Profile");
            wb.setSheetHidden(nSheet, true);
            Sheet sh = wb.getSheetAt(nSheet);
            xlFceProfile(sh, styles);

            nSheet++;
            //  create a new file
            FileOutputStream out = null;
            FileDialog fileDlg =
                    new FileDialog(mainF, "Saving Results to Excel",
                            FileDialog.SAVE);

            fileDlg.setFile("Test workbook from Java.xlsx");
            fileDlg.setVisible(true);
            String bareFile = fileDlg.getFile();
            if (bareFile != null) {
                int len = bareFile.length();
                if ((len < 4) || !(bareFile.substring(len - 5).equalsIgnoreCase(".xlsx"))) {
                    showMessage("Adding '.xlsx' to file name");
                    bareFile = bareFile + ".xlsx";
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

    public String inputDataXML() {
        return XMLmv.putTag("RTHeating", "\n" + dataInXML());
    }

    String dataInXML() {
        StringBuilder xmlStr =
                new StringBuilder(chargeDataToSave());
        xmlStr.append(theFurnace.fceDataInXML());
        return xmlStr.toString();
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
        String xmlStr = inputDataXML();
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


    void showResultsPanel(String command) {
        showResultsPanel(resultsPanels.get(ResultsType.getEnum(command)));
    }

    public void showResultsPanel(ResultsType type) {
        showResultsPanel(resultsPanels.get(type));
    }

    void showResultsPanel(ResultPanel rP) {
        if (SwingUtilities.isEventDispatchThread()) {
            slate.setViewportView(rP.getPanel());
        }
        else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        slate.setViewportView(rP.getPanel());
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        if (SwingUtilities.isEventDispatchThread()) {
            slate.setViewportView(rP.getPanel());
        }
        else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        slate.setViewportView(rP.getPanel());
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public void addResult(ResultsType type, JPanel panel) {
        ResultPanel rp;
        rp = resultsPanels.get(type);
        if (rp != null)
            rp.setPanel(panel);
    }

    public void undoResults(ResultsType type) {
        ResultPanel rp;
        rp = resultsPanels.get(type);
        if (rp != null)
            rp.removePanel();
    }

    protected class ResultPanel {
        DFHResult.Type type;
        String menuText;
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

        ResultPanel(String menuText, ActionListener li) {
            this.menuText = menuText;
            mI = new JMenuItem(menuText);
            mI.addActionListener(li);
            mI.setEnabled(false);
        }

        public void removePanel() {
            panel = null;
            mI.setEnabled(false);
        }

        JMenuItem getMenuItem() {
            return mI;
        }

        JPanel getPanel() {
            return panel;
        }

        public void setPanel(JPanel panel) {
            this.panel = panel;
            mI.setEnabled(true);
        }

        DFHResult.Type getType() {
            return type;
        }
    }

    @Override
    public boolean canNotify() {
        return canNotify;
    }

    @Override
    public void enableNotify(boolean b) {
        canNotify = b;
    }

    @Override
    public Window parent() {
        return mainF;
    }

    public void showError(String title, String msg) {
        showError(title, msg, mainF);
    }

    public void showError(String msg) {
        showError(msg, mainF);
    }

    public static void showError(String title, String msg, Window w){
        SimpleDialog.showError(w, title, msg);
        if (w != null)
            w.toFront();
    }

    public static void showError(String msg, Window w){
        SimpleDialog.showError(w, "", msg);
        if (w != null)
            w.toFront();
    }

    public void showMessage(String msg) {
        showMessage("", msg);
    }

    public void showMessage(String title, String msg) {
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


    class WinListener implements WindowListener {
        public void windowOpened(WindowEvent e) {
        }

        public void windowClosing(WindowEvent e) {
            close();
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



    public static void main (String[] arg) {
        RTHeating rth = new RTHeating();
        if (!rth.setItUp()) {
            rth.showError("Something is not OK. Aborting");
            System.exit(1);
        }
    }

}
