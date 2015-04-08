package radiantTubeHeating;

import PropertyViewer.OnePropertyTrace;
import basic.ChMaterial;
import basic.Charge;
import basic.RadiantTube;
import mvUtils.display.FramedPanel;
import mvUtils.display.GraphDisplay;
import mvUtils.display.GraphInfo;
import mvUtils.math.XYArray;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;


import javax.swing.*;
import java.awt.*;
import java.awt.Color;
import java.awt.event.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/19/12
 * Time: 1:28 PM
 * To change this template use File | Settings | File Templates.
 */

public class RTHeating extends JApplet {
    boolean onTest = false;
    JSObject win;
    String header;
    boolean itsON = false;
    JFrame mainF;
    JPanel mainFrame;
    String cvs;

    public RTHeating() {
    }

    public void init() {
        String strTest = this.getParameter("OnTest");
        if (strTest != null)
            onTest = strTest.equalsIgnoreCase("YES");
        if (onTest) {
            setTestData();
            calculateRTFce();
            displayIt();
        } else {
            try {
                win = JSObject.getWindow(this);
            } catch (JSException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                win = null;
            }
            Object o;
            o = win.eval("getData()");
        }
    }

    void setTestData() {
        debug("hC " + setChargeHeatCont("0, 0, 100, 11.5, 300, 37, 450, 59.0005, 600, 85, 700, 104, 750, 117.5, 850, 137, 1000, 161, 2000, 327"));
        debug("tk " + setChargeTk("0, 42"));
        debug("emiss " + setChargeEmiss("0, 0.6, 2000, 0.6"));
        debug("charge Basic " + chargeBasic("TestMaterial", "0000", "" + 7.85, "" + 1.05, "" + 0.55));
        debug("rt " + defineRT("" + 0.198, "" + 1.40, "" + 30, "" + 0.85));
        debug("furnace " + defineFurnace("" + 1.55, "" + 0.8, "" + 0.4, "" + 3.333333, "" + 4000));
        debug("production " + defineProduction("" + 20000, "" + 550, "" + 730, "" + 900, "" + 24, "" + 50, "" + 1.0, "RTHEAT"));
    }

    public void displayIt() {
        if (!itsON && furnace != null) {
            itsON = true;
            mainF = new JFrame("RT Furnace Heating");

            setMenuOptions();
            mainFrame = new JPanel(new GridBagLayout());
            mainF.getContentPane().add(mainFrame);
            mainFrame.setBackground(new JPanel().getBackground());
            GridBagConstraints gbcMf = new GridBagConstraints();
            gbcMf.anchor = GridBagConstraints.CENTER;
            gbcMf.gridx = 0;
            gbcMf.gridy = 0;
            gbcMf.insets = new Insets(0, 0, 0, 0);
            gbcMf.gridwidth = 1;
            gbcMf.gridheight = 1;
            mainFrame.add(getTitlePanel(), gbcMf);
            gbcMf.gridx = 0;
            gbcMf.gridy++;
            mainFrame.add(getGraphPanel(), gbcMf);
            gbcMf.gridx = 0;
            gbcMf.gridy++;
            gbcMf.gridwidth = 1;
            gbcMf.gridheight = 2;
            mainFrame.add(getListPanel(), gbcMf);
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

    JMenu propMenu;

    void setMenuOptions() {
        JMenuBar mb = new JMenuBar();
        OnePropertyTrace oneT;
        JMenuItem mI;
        MenuActions mAction = new MenuActions();
        propMenu = new JMenu("File");
        mI = new JMenuItem("Save Results");
        mI.addActionListener(mAction);
        propMenu.add(mI);
        mI = new JMenuItem("Retrieve Results");
        mI.addActionListener(mAction);
        propMenu.add(mI);
        propMenu.addSeparator();
        mI = new JMenuItem("Exit");
        mI = new JMenuItem("Exit");
        mI.addActionListener(mAction);
        propMenu.add(mI);

        mb.add(propMenu);
        mainF.setJMenuBar(mb);
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
            if (command.equals("Save Results")) {
                Object o;
                o = win.eval("saveResults()");
//                if (Integer.valueOf(((String)o)) >0)
                close();
            }
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


    ChMaterial material;
    Charge theCharge;
    XYArray hC, tk, emiss;
    RadiantTube rt;
    RTFurnace furnace;
    double production, stTemp, endTemp, perMLoss;
    double rtLimitTemp, rtLimitHeat, fceLimitLength;
    boolean bHeatLimit, bRtTempLimit, bFceLengthLimit;

    double uLen;
    RTFurnace.CalculMode iCalculMode;

    ScrollPane resultScroll;
    JTable resultTable;

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

    public String defineRT(String od, String effLen, String rating, String emiss) {
        String retVal = "OK";
        try {
            double odVal = Double.valueOf(od);
            double effLenVal = Double.valueOf(effLen);
            double ratingVal = Double.valueOf(rating);
            double emissVal = Double.valueOf(emiss);
            rt = new RadiantTube(odVal, effLenVal, ratingVal, emissVal);
        } catch (NumberFormatException e) {
            retVal = "ERROR in number format in defineRT!";
        }
        return retVal;
    }

    public String defineFurnace(String width, String heightAbove, String rtCenterAbove, String rtPerM, String lossPerM) {
        String retVal = "OK";
        try {
            double w = Double.valueOf(width);
            double h = Double.valueOf(heightAbove);
            double cl = Double.valueOf(rtCenterAbove);
            double rts = Double.valueOf(rtPerM);
            perMLoss = Double.valueOf(lossPerM);
            if (rt != null)
                furnace = new RTFurnace(w, h, cl, rt, rts);
            else
                retVal = "ERROR Radiant tube not defined in defineFurnace!";
        } catch (NumberFormatException e) {
            retVal = "ERROR in number format in defineFurnace!";
        }
        return retVal;
    }

    public String defineProduction(String output, String stTempStr, String endTempStr, String maxRtTemp,
                                   String maxRTHeat, String maxFceLen, String uFceLen, String limitType) {
        String retVal = "OK";
        try {
            production = Double.valueOf(output);
            stTemp = Double.valueOf(stTempStr);
            endTemp = Double.valueOf(endTempStr);
            rtLimitTemp = Double.valueOf(maxRtTemp);
            rtLimitHeat = Double.valueOf(maxRTHeat);
            fceLimitLength = Double.valueOf(maxFceLen);
            bRtTempLimit = false;
            bHeatLimit = false;
            bFceLengthLimit = false;

            if (limitType.equalsIgnoreCase("" + RTFurnace.CalculMode.RTTEMP))
                iCalculMode = RTFurnace.CalculMode.RTTEMP;
            else if (limitType.equalsIgnoreCase("" + RTFurnace.CalculMode.RTHEAT))
                iCalculMode = RTFurnace.CalculMode.RTHEAT;
            uLen = Double.valueOf(uFceLen);
            furnace.setProduction(theCharge, production, fceLimitLength, uLen, perMLoss * uLen, iCalculMode);
        } catch (NumberFormatException e) {
            retVal = "ERROR in number format in defineProduction!";
        }

        return retVal;
    }

    public void calculateRTFce() {
        furtherCalculations();

    }

    void debug(String msg) {
        System.out.println("RTHeating: " + msg);
    }


    void furtherCalculations() {
        furnace.calculate(stTemp + 273, endTemp + 273, rtLimitTemp + 273, rtLimitHeat);
    }

    public String resultsInCVS() {
        return furnace.resultsInCVS();
    }

}
