package radiantTubeHeating;

import display.OnePropertyTrace;
import basic.ChMaterial;
import basic.Charge;
import basic.RadiantTube;
import jsp.JSPchMaterial;
import mvUtils.display.*;
import mvUtils.jsp.JSPComboBox;
import mvUtils.jsp.JSPConnection;
import mvUtils.math.XYArray;
import netscape.javascript.JSObject;
import protection.CheckAppKey;


import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.Color;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/19/12
 * Time: 1:28 PM
 * To change this template use File | Settings | File Templates.
 */

public class RTHeating extends JApplet implements InputControl{
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

    protected enum RTHDisplayPage {
        INPUTPAGE, RESULTSPAGE
    }

    String title = "Radiant Tube Heated Furnace 20170901";
    public int appCode = 101;
    boolean canNotify = true;
    JSObject win;
    String header;
    boolean itsON = false;
    JFrame mainF;
    JPanel mainPanel;
    JButton jBcalculate = new JButton("Calculate");
    String cvs;
    String jspBase = "HYPWAP02:9080/fceCalculations/jsp/";
    JPanel inpPage;
    JPanel resultsPage;
    protected JScrollPane slate = new JScrollPane();
    public JButton pbEdit;

    public JSPConnection jspConnection;

    public RTHeating() {
    }

//    public void init() {
////        String strTest = this.getParameter("OnTest");
////        if (strTest != null)
////            onTest = strTest.equalsIgnoreCase("YES");
//        onTest = true;
//        if (onTest) {
//            setTestData();
//            calculateRTFce();
//            displayIt();
//        } else {
//            try {
//                win = JSObject.getWindow(this);
//            } catch (JSException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                win = null;
//            }
//            Object o;
//            o = win.eval("getData()");
//        }
//    }

    public boolean setItUp() {
        boolean retVal = false;
        if (getJSPbase() && getJSPConnection()) {
            DataWithStatus<Boolean> runCheck = new CheckAppKey(jspBase).canRunThisApp(appCode, true);
            if (runCheck.getStatus() == DataStat.Status.OK) {
                setUIDefaults();
                mainF = new JFrame(title);
//        mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                setMenuOptions();
//        setTestData();
                mainF.add(slate);
                vChMaterial = new Vector<ChMaterial>();
                furnace = new RTFurnace(this);
                if (loadChMaterialData()) {
                    inpPage = inputPage();
                    switchToSelectedPage(RTHDisplayPage.INPUTPAGE);
                    mainF.pack();
                    mainF.setVisible(true);
                    retVal = true;
                }
            }
        } else
            showError("Unable to connect to Server");
        return retVal;
    }

    public void setItupOLD() { // TODO check and remove
        setUIDefaults();
        mainF = new JFrame("RadiantTube Furnace");
//        mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMenuOptions();
//        setTestData();
        mainF.add(slate);
        vChMaterial = new Vector<ChMaterial>();
        furnace = new RTFurnace(this);
        if (getJSPbase()) {
            if (getJSPConnection()) {
                inpPage = inputPage();
                switchToSelectedPage(RTHDisplayPage.INPUTPAGE);
                mainF.pack();
                mainF.setVisible(true);
            }
        }
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


    JPanel inputPage() {
        JPanel inputFrame = new JPanel(new GridBagLayout());
        GridBagConstraints gbcMf = new GridBagConstraints();

        gbcMf.anchor = GridBagConstraints.CENTER;
        gbcMf.gridx = 0;
        gbcMf.gridy = 0;
        gbcMf.insets = new Insets(0, 0, 0, 0);
        gbcMf.gridwidth = 1;
        gbcMf.gridheight = 1;
        gbcMf.gridy++;
        inputFrame.add(furnace.fceDetailsP(this), gbcMf);
        gbcMf.gridx = 0;
        gbcMf.gridy++;
        gbcMf.gridheight = 2;
        inputFrame.add(furnace.radiantTubesP(this), gbcMf);
        gbcMf.gridheight = 1;
        gbcMf.gridx = 1;
        gbcMf.gridy = 1;
        inputFrame.add(chargePan(this), gbcMf);
        gbcMf.gridy++;
        inputFrame.add(productionP(this), gbcMf);
        gbcMf.gridy++;
        inputFrame.add(calculDataP(this), gbcMf);
        gbcMf.gridx = 0;
        gbcMf.gridwidth = 2;
        gbcMf.gridy++;
        JPanel buttonPanel = new JPanel();
        jBcalculate.addActionListener(e-> {
            calculateRTFce();
        });
        buttonPanel.add(jBcalculate);
        inputFrame.add(buttonPanel, gbcMf);
        return inputFrame;
    }

    JPanel resultsPage() {
        JPanel resultsFrame = new JPanel(new GridBagLayout());
        GridBagConstraints gbcMf = new GridBagConstraints();
        gbcMf.anchor = GridBagConstraints.CENTER;
        gbcMf.gridx = 0;
        gbcMf.gridy = 0;
        gbcMf.insets = new Insets(0, 0, 0, 0);
        gbcMf.gridwidth = 1;
        gbcMf.gridheight = 1;
        resultsFrame.add(getTitlePanel(), gbcMf);
        gbcMf.gridx = 0;
        gbcMf.gridy++;
        resultsFrame.add(getGraphPanel(), gbcMf);
        gbcMf.gridx = 0;
        gbcMf.gridy++;
        gbcMf.gridwidth = 1;
        gbcMf.gridheight = 2;
        resultsFrame.add(getListPanel(), gbcMf);
        trendPanel.setTraceToShow(-1);
        return resultsFrame;
    }

    private void switchToSelectedPage(RTHDisplayPage page) {
        boolean done = true;
        switch (page) {
            case INPUTPAGE:
                slate.setViewportView(inpPage);
                break;
            case RESULTSPAGE:
                slate.setViewportView(resultsPage);
                break;
        }
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

//    void setTestData() {
//        debug("hC " + setChargeHeatCont("0, 0, 100, 11.5, 300, 37, 450, 59.0005, 600, 85, 700, 104, 750, 117.5, 850, 137, 1000, 161, 2000, 327"));
//        debug("tk " + setChargeTk("0, 42"));
//        debug("emiss " + setChargeEmiss("0, 0.6, 2000, 0.6"));
//        debug("charge Basic " + chargeBasic("TestMaterial", "0000", "" + 7.85, "" + 1.05, "" + 0.00055));
//        debug("rt " + defineRT("" + 0.198, "" + 1.40, "" + 30, "" + 0.85));
//        debug("furnace " + defineFurnace("" + 1.55, "" + 0.8, "" + 0.4, "" + 3.333333, "" + 4000));
//        debug("production " + defineProduction("" + 20000, "" + 550, "" + 730, "" + 900, "" + 24, "" + 50, "" + 1.0, "RTHEAT"));
//    }


    public void displayIt() {
        if (!itsON && furnace != null) {
            itsON = true;
            mainF = new JFrame("RT Furnace Heating");

            setMenuOptions();
            mainPanel = new JPanel(new GridBagLayout());
            mainF.getContentPane().add(mainPanel);
            mainPanel.setBackground(new JPanel().getBackground());
            GridBagConstraints gbcMf = new GridBagConstraints();
            gbcMf.anchor = GridBagConstraints.CENTER;
            gbcMf.gridx = 0;
            gbcMf.gridy = 0;
            gbcMf.insets = new Insets(0, 0, 0, 0);
            gbcMf.gridwidth = 1;
            gbcMf.gridheight = 1;
            mainPanel.add(getTitlePanel(), gbcMf);
            gbcMf.gridx = 0;
            gbcMf.gridy++;
            mainPanel.add(getGraphPanel(), gbcMf);
            gbcMf.gridx = 0;
            gbcMf.gridy++;
            gbcMf.gridwidth = 1;
            gbcMf.gridheight = 2;
            mainPanel.add(getListPanel(), gbcMf);
//            if (!onTest) {
//                Dimension d = getSize();
//                win.eval("setAppletSize(" + d.width + ", " + d.height + ")");
//            }
            mainF.setLocation(100, 100);
            mainF.pack();
            mainF.setFocusable(true);
            mainF.setVisible(true);
            mainF.requestFocus();
            mainF.toFront(); //setAlwaysOnTop(true);
            trendPanel.setTraceToShow(-1);
        }
    }

    JMenu fileMenu;
    JMenu inputMenu;
    JMenu resultsMenu;
    boolean resultsReady = false;

    void setMenuOptions() {
        JMenuBar mb = new JMenuBar();
        OnePropertyTrace oneT;
        JMenuItem mI;
        MenuActions mAction = new MenuActions();
        fileMenu = new JMenu("File");
//        mI = new JMenuItem("Save Results");
//        mI.addActionListener(mAction);
//        fileMenu.add(mI);
//        mI = new JMenuItem("Retrieve Results");
//        mI.addActionListener(mAction);
//        fileMenu.add(mI);
//        fileMenu.addSeparator();
        mI = new JMenuItem("Exit");
        mI.addActionListener(mAction);
        fileMenu.add(mI);
        mb.add(fileMenu);
        inputMenu = new JMenu("input Data");
        resultsMenu = new JMenu("Results");
        resultsMenu.setEnabled(false);
        MenuListener menuListener = new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                if (e.getSource() == inputMenu)
                    switchToSelectedPage(RTHDisplayPage.INPUTPAGE);
                else if (e.getSource() == resultsMenu && resultsReady)
                    switchToSelectedPage(RTHDisplayPage.RESULTSPAGE);
            }

            @Override
            public void menuDeselected(MenuEvent e) {

            }

            @Override
            public void menuCanceled(MenuEvent e) {

            }
        };

        inputMenu.addMenuListener(menuListener);
        resultsMenu.addMenuListener(menuListener);
        mb.add(inputMenu);
        mb.add(resultsMenu);

        pbEdit = new JButton("AllowDataEdit");
        pbEdit.setMnemonic(KeyEvent.VK_E);
        pbEdit.getModel().setPressed(true);
        pbEdit.setEnabled(false);
        pbEdit.addActionListener(e -> {
            switchToSelectedPage(RTHDisplayPage.INPUTPAGE);
            enableDataEdit(true);

        });
        mb.add(pbEdit);
        mainF.setJMenuBar(mb);
    }

    public void enableDataEdit(boolean ena) {

        ntStripWidth.setEditable(ena);
        ntStripThickness.setEditable(ena);
        cbChMaterial.setEnabled(ena);

        ntOutput.setEditable(ena);
        ntChEntryTemp.setEditable(ena);
        ntChExitTemp.setEditable(ena);

        cbLimitMode.setEnabled(ena);
        ntMaxRtTemp.setEditable(ena);
        ntMaxRtHeat.setEditable(ena);
        ntUfceLen.setEditable(ena);

        furnace.enableDataEdit(ena);
        jBcalculate.setEnabled(ena);

//        enableDataEntry(true);
//        enableFileMenu(true);
//        enableDefineMenu(true);
        pbEdit.getModel().setPressed(ena);
        pbEdit.setEnabled(!ena);
        resultsReady = !ena;
        resultsMenu.setEnabled(!ena);
    }

    void close() {
        mainF.dispose();
        mainF = null;
        itsON = false;
    } // close


    class MenuActions implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            if (command.equals("Exit"))
                close();
        } // actionPerformed
    } // class TraceActionListener

    FramedPanel getTitlePanel() {
        FramedPanel titlePanel = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbcTp = new GridBagConstraints();
        final int hse = 4; // ends
        final int vse = 4; // ends
        final int hs = 0;
        final int vs = 0;
        Insets lt = new Insets(vs, hse, vs, hs);
        Insets ltbt = new Insets(vs, hse, vse, hs);
        Insets bt = new Insets(vs, hs, vse, hs);
        Insets rtbt = new Insets(vs, hs, vse, hse);
        Insets rt = new Insets(vs, hs, vs, hse);
        Insets rttp = new Insets(vse, hs, vs, hse);
        Insets lttp = new Insets(vse, hse, vs, hs);
        Insets tp = new Insets(vse, hs, vs, hs);
        Insets ltrt = new Insets(vs, hse, vs, hse);
        Insets lttprt = new Insets(vse, hse, vs, hse);
        Insets ltbtrt = new Insets(vs, hse, vse, hse);
        Insets mid = new Insets(vs, hs, vs, hs);
        gbcTp.anchor = GridBagConstraints.CENTER;
        gbcTp.gridx = 0;
        gbcTp.gridy = 0;
        gbcTp.insets = lttp;
        titlePanel.add(new Label("Radiant Tube Furnace"), gbcTp);
        gbcTp.gridy++;
        gbcTp.insets = lt;
        JLabel jLtitle = new JLabel();
        titlePanel.add(jLtitle, gbcTp);
        gbcTp.gridy++;
        return titlePanel;
    }

    Panel getListPanel() {
        resultTable = furnace.getResultTable();
        JPanel headPanel = new JPanel(new GridLayout(1, 1));
        headPanel.add(resultTable.getTableHeader());
        Panel listPanel = new Panel(new BorderLayout());
        listPanel.add(headPanel, BorderLayout.NORTH);
        resultScroll = new ScrollPane();
        resultScroll.setSize(new Dimension(700, 250));
        resultScroll.add(resultTable);
        resultScroll.repaint();
        listPanel.add(resultScroll, BorderLayout.CENTER);
        return listPanel;
    }

    FramedPanel gP;
    GraphPanel trendPanel;

    FramedPanel getGraphPanel() {
        gP = new FramedPanel(new GridLayout(1, 0));
        trendPanel =
                new GraphPanel(new Dimension(700, 350));
        for (int t = 0; t < furnace.nTraces; t++)
            trendPanel.addTrace(furnace, t, GraphDisplay.COLORS[t]);
//        if (traces.nTraces > 1)
        trendPanel.setTraceToShow(0);   // all
        trendPanel.prepareDisplay();
        gP.add(trendPanel);
        //   gP.setSize(300,300);
        return gP;
    }

    class GraphPanel
            extends JPanel {
        final Insets borders = new Insets(2, 2, 2, 2);
        GraphDisplay gDisplay;
        Dimension size;
        Point origin; // in % of graph area

        GraphPanel(Dimension size) {
            this.size = size;
            setSize(size);
            origin = new Point(0, 00);
            gDisplay = new GraphDisplay(this, origin, null); //frameEventDespatcher);
            //       gDisplay.setBasicCalculData(traces);
        }


        int addTrace(GraphInfo gInfo, int trace, Color color) {
            gDisplay.addTrace(gInfo, trace, color);
            return gDisplay.traceCount();
        }

        void prepareDisplay() {
            gDisplay.prepareDisplay();
        }

        public Insets getInsets() {
            return borders;
        }

        public Dimension getMinimumSize() {
            return size;
        }

        public Dimension getPreferredSize() {
            return size;
        }

        public void setTraceToShow(int t) {
            gDisplay.setTraceToShow(t);
//          mainF.repaint();
        }
    } // class GraphPanel


    ChMaterial chMaterial;
    Charge theCharge;
    XYArray hC, tk, emiss;
    RadiantTube rt;
    RTFurnace furnace;
    double production, stTemp = 30, endTemp = 730, perMLoss;
    double rtLimitTemp = 900, rtLimitHeat = 24, fceLimitLength;
    boolean bHeatLimit, bRtTempLimit, bFceLengthLimit;

    double uLen = 1;
    LimitMode iLimitMode;

    ScrollPane resultScroll;
    JTable resultTable;

    JSPComboBox<ChMaterial> cbChMaterial;
    Vector<ChMaterial> vChMaterial;
    JPanel chargeP;
    double stripWidth = 1.25;
    double stripThick = 0.0006;
    NumberTextField ntStripWidth;
    NumberTextField ntStripThickness;
    boolean chargeFieldsSet = false;
    Vector<NumberTextField> chargeFields;

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
                chargeFields = new Vector<>();
                chargeFields.add(ntStripWidth = new NumberTextField(ipc, stripWidth * 1000, 6, false,
                        50, 10000, "#,###", "Strip Width (mm)"));
                chargeFields.add(ntStripThickness = new NumberTextField(ipc, stripThick * 1000, 6, false,
                        0.001, 200, "0.000", "Strip Thickness (mm)"));
                pan.addItemPair("Charge Material", cbChMaterial);
                pan.addItemPair(ntStripWidth);
                pan.addItemPair(ntStripThickness);
                chargeP = pan;
                chargeFieldsSet = true;
//            }
        }
        return chargeP;
    }

    boolean takeChargeFromUI() {
        chMaterial = (ChMaterial)cbChMaterial.getSelectedItem();
        boolean retVal = (chMaterial != null);
        for (NumberTextField f: chargeFields)
            retVal &= !f.isInError();
        if (retVal) {
            stripWidth = ntStripWidth.getData() / 1000;
            stripThick = ntStripThickness.getData() / 1000;
        }
        return retVal;
    }

    NumberTextField ntOutput;
    NumberTextField ntChEntryTemp;
    NumberTextField ntChExitTemp;
    JPanel productionPanel;
    boolean productionFieldsSet = false;
    Vector<NumberTextField> productionFields;

    public JPanel productionP(InputControl ipc) {
        if (!productionFieldsSet) {
            MultiPairColPanel pan = new MultiPairColPanel("Production Data" );
            productionFields = new Vector<>();
            productionFields.add(ntOutput = new NumberTextField(ipc, production/ 1000, 6, false,
                    0.200, 200000, "#,###", "Output (t/h)"));
            productionFields.add(ntChEntryTemp = new NumberTextField(ipc, stTemp, 6, false,
                    -200, 2000, "#,###", "Charge Entry Temperatures (C)"));
            productionFields.add(ntChExitTemp = new NumberTextField(ipc, endTemp, 6, false,
                    -200, 2000, "#,###", "Charge Exit Temperatures (C)"));
            pan.addItemPair(ntOutput);
            pan.addItemPair(ntChEntryTemp);
            pan.addItemPair(ntChExitTemp);
            productionPanel = pan;
            productionFieldsSet = true;
        }
        return productionPanel;
    }

    public boolean takeProductionFromUI() {
        boolean retVal = true;
        for (NumberTextField f: productionFields)
            retVal &= !f.isInError();
        if (retVal) {
            production = ntOutput.getData() * 1000;
            stTemp = ntChEntryTemp.getData();
            endTemp = ntChExitTemp.getData();
            if (endTemp <= stTemp) {
                showError("Charge Exit Temperature has to be higher the Entry");
                retVal = false;
            }
        }
        return retVal;
    }

//    public String chargeBasic(String matName, String matID, String density, String width, String thickness) {
//        double l = 0; // dimension along furnace width, nomenclatures as per Charge class
//        double w = 1; // dimension along furnace length
//        double th = 0;
//        double den = 0;
//        String retVal = "";
//        try {
//            l = Double.valueOf(width);
//            th = Double.valueOf(thickness);
//            den = Double.valueOf(density);
//            if (tk != null && hC != null && emiss != null) {
//                chMaterial = new ChMaterial(matName, matID, den, tk, hC, emiss);
//                theCharge = new Charge(chMaterial, l, w, th);
//                retVal = "OK";
//            } else
//                retVal = "ERROR: Properties not set!";
//        } catch (NumberFormatException e) {
//            retVal = "ERROR in number format in chargeBasic!";
//        }
//        return retVal;
//    }
//
//    public String setChargeHeatCont(String heatCont) {
//        hC = new XYArray(heatCont);
//        return "OK " + hC.arrLen;
//    }
//
//    public String setChargeTk(String thermalC) {
//        tk = new XYArray(thermalC);
//        return "OK " + tk.arrLen;
//    }
//
//    public String setChargeEmiss(String emissivity) {
//        emiss = new XYArray(emissivity);
//        return "OK " + emiss.arrLen;
//    }
//
//    public String defineRT(String od, String effLen, String rating, String emiss) {
//        String retVal = "OK";
//        try {
//            double odVal = Double.valueOf(od);
//            double effLenVal = Double.valueOf(effLen);
//            double ratingVal = Double.valueOf(rating);
//            double emissVal = Double.valueOf(emiss);
//            rt = new RadiantTube(odVal, effLenVal, ratingVal, emissVal);
//        } catch (NumberFormatException e) {
//            retVal = "ERROR in number format in defineRT!";
//        }
//        return retVal;
//    }
//
//    public String defineFurnace(String width, String heightAbove, String rtCenterAbove, String rtPerM, String lossPerM) {
//        String retVal = "OK";
//        try {
//            double w = Double.valueOf(width);
//            double h = Double.valueOf(heightAbove);
//            double cl = Double.valueOf(rtCenterAbove);
//            double rts = Double.valueOf(rtPerM);
//            perMLoss = Double.valueOf(lossPerM);
//            if (rt != null)
//                furnace = new RTFurnace(w, h, cl, rt, rts);
//            else
//                retVal = "ERROR Radiant tube not defined in defineFurnace!";
//        } catch (NumberFormatException e) {
//            retVal = "ERROR in number format in defineFurnace!";
//        }
//        return retVal;
//    }
//
//    public String defineProduction(String output, String stTempStr, String endTempStr, String maxRtTemp,
//                                   String maxRTHeat, String maxFceLen, String uFceLen, String limitType) {
//        String retVal = "OK";
//        try {
//            production = Double.valueOf(output);
//            stTemp = Double.valueOf(stTempStr);
//            endTemp = Double.valueOf(endTempStr);
//            rtLimitTemp = Double.valueOf(maxRtTemp);
//            rtLimitHeat = Double.valueOf(maxRTHeat);
//            fceLimitLength = Double.valueOf(maxFceLen);
//            bRtTempLimit = false;
//            bHeatLimit = false;
//            bFceLengthLimit = false;
//
//            if (limitType.equalsIgnoreCase("" + LimitMode.RTTEMP))
//                iLimitMode = LimitMode.RTTEMP;
//            else if (limitType.equalsIgnoreCase("" + LimitMode.RTHEAT))
//                iLimitMode = LimitMode.RTHEAT;
//            uLen = Double.valueOf(uFceLen);
//            furnace.setProduction(theCharge, production, fceLimitLength, uLen, perMLoss * uLen, iLimitMode);
//        } catch (NumberFormatException e) {
//            retVal = "ERROR in number format in defineProduction!";
//        }
//
//        return retVal;
//    }

    JPanel calculModeP;
    JComboBox<LimitMode> cbLimitMode;
    NumberTextField ntMaxRtTemp;
    NumberTextField ntMaxRtHeat;
    NumberTextField ntUfceLen;
    Vector<NumberTextField> calculFields;
    boolean calculFieldsSet = false;

    JPanel calculDataP(InputControl ipc) {
        if (!calculFieldsSet) {
            MultiPairColPanel pan = new MultiPairColPanel("Calculation Mode");
            calculFields = new Vector<>();
            cbLimitMode = new JComboBox<>(LimitMode.values());
            pan.addItemPair("Calculation Limiting Mode", cbLimitMode);
            calculFields.add(ntMaxRtTemp = new NumberTextField(ipc, rtLimitTemp, 6, false,
                    200, 2000, "#,###", "Radiant Tube Temperature Limit (C)"));
            productionFields.add(ntMaxRtHeat = new NumberTextField(ipc, rtLimitHeat, 6, false,
                    0, 2000, "#,###.00", "Radiant Tube Heat Limit (kW)"));
            productionFields.add(ntUfceLen = new NumberTextField(ipc, uLen * 1000, 6, false,
                    200, 20000, "#,###", "Furnace Calculation Step Length (mm)"));
            pan.addItemPair(ntMaxRtTemp);
            pan.addItemPair(ntMaxRtHeat);
            pan.addItemPair(ntUfceLen);
            calculModeP = pan;
            calculFieldsSet = true;
        }
        return calculModeP;
    }

    boolean takeCalculModeFromUI() {
        boolean retVal = true;
        for (NumberTextField f: calculFields)
            retVal &= !f.isInError();
        if (retVal) {
            rtLimitTemp = ntMaxRtTemp.getData();
            rtLimitHeat = ntMaxRtHeat.getData();
            uLen = ntUfceLen.getData() / 1000;
            iLimitMode = (LimitMode)(cbLimitMode.getSelectedItem());
            switch ((LimitMode)cbLimitMode.getSelectedItem()) {
                case RTHEAT:
                    bHeatLimit = true;
                    bRtTempLimit = false;
                    break;
                case RTTEMP:
                    bHeatLimit = false;
                    bRtTempLimit = true;
                    break;
            }
        }
        return retVal;
    }

    public void calculateRTFce() {
        if (takeChargeFromUI() && takeProductionFromUI() && furnace.takeFceDetailsFromUI() &&
                takeCalculModeFromUI()) {
            theCharge = new Charge(chMaterial, stripWidth, uLen, stripThick);
            furnace.setProduction(theCharge, production, uLen, iLimitMode);
            furtherCalculations();
            resultsPage = resultsPage();
            enableDataEdit(false);
            switchToSelectedPage(RTHDisplayPage.RESULTSPAGE);
        }
        else
            showError("Some Input Data is not acceptable");
    }

    void debug(String msg) {
        System.out.println("RTHeating: " + msg);
    }


    void furtherCalculations() {
        furnace.calculate(stTemp + 273, endTemp + 273, rtLimitTemp + 273, rtLimitHeat);
    }

//    public String resultsInCVS() {
//        return furnace.resultsInCVS();
//    }

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

    public void showError(String msg) {
        showError(msg, mainF);
    }

    public static void showError(String msg, Window w){
        SimpleDialog.showError(w, "", msg);
        if (w != null)
            w.toFront();
    }



    public static void main (String[] arg) {
        RTHeating rth = new RTHeating();
        if (!rth.setItUp()) {
            rth.showError("Something is not OK. Aborting");
            System.exit(1);
        }
    }

}
