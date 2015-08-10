
//Title:       Your Product Name
//Version:
//Copyright:   Copyright (c) 2002
//Author:      M Viswanathan
//Company:     Hypertherm Engineers Pvt. Ltd.
//Description:

package application;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import basic.*;
import display.*;

public class TransientHeat extends JFrame {

  ChargeDef chargeDef;
  Hashtable surfaceAmbientSet;
  ThreeDCharge charge;
  ChargeHeatCycle chHeatCycle;
  TemperatureStats stats;
  JMenu mDefCharge = new JMenu("Define Charge");
  JMenuItem defRectCharge = new JMenuItem("Rectangular Charge");
  JMenuItem defBeamBlankH = new JMenuItem("Horizontal Beam Blank");
  JMenuItem defBeamBlankV = new JMenuItem("Vertical Beam Blank");
  JMenuItem mDefAmbients = new JMenuItem("Define Ambients");
  JMenuItem mChargeAndCycle = new JMenuItem("Charge&Cycle");
  String evalNew = "Evaluate Fresh";
  String evalContinue = "Eval Continue";
  boolean bEvalContinue = false;
  JMenuItem mEvaluate = new JMenuItem(evalNew);
//  JMenuItem mContinue = new JMenuItem("Evaluate-Continue");
  JMenuItem mShowSaved = new JMenuItem("Show Saved");
  JTextArea taCharge = new JTextArea(2, 30);
  Vector<AmbientCycle> ambients;
  QueryPanel ambientsSettingPanel = null;
  int maxElements = 100000; // element divisions
  double minSliceThick = 0.005; // minimum slice thickness in m
  double maxTime = 0.5; // default time
  JTextField jtMaxTime = new JTextField(String.valueOf(maxTime), 6);
  double startTemp = 30;
//  JTextField jtStartTemp = new JTextField(String.valueOf(startTemp), 6);
//  PartAmbListener partAmbListener;
  ChargeInFurnace chInFce;

  public TransientHeat() {

    super("Transient Heating of Charge");
    jbInit();
  }

  private void jbInit() {
    GetChargeListener getChListener = new GetChargeListener();
    defBeamBlankV.addActionListener(getChListener);
    defBeamBlankH.addActionListener(getChListener);
    defRectCharge.addActionListener(getChListener);

    mChargeAndCycle.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            getChAndHeatCycle();
          }
        });
    mDefAmbients.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            getAmbients();
          }
        });

    mEvaluate.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            if (bEvalContinue)
              continueEval();
            else
              evaluate();
          }
        });

//    mContinue.addActionListener(
//        new ActionListener() {
//          public void actionPerformed(ActionEvent ae) {
//            continueEval();
//          }
//        });
//    mContinue.setEnabled(false);
    mShowSaved.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            showSavedData();
          }
        });
    mShowSaved.setEnabled(true);
    JMenuBar menuBar = new JMenuBar();
    mDefCharge.add(defRectCharge);
    mDefCharge.add(defBeamBlankH);
    mDefCharge.add(defBeamBlankV);
    menuBar.add(mDefCharge);
    menuBar.add(mDefAmbients);
    menuBar.add(mChargeAndCycle);
    menuBar.add(mEvaluate);
    mEvaluate.setEnabled(false);
//    menuBar.add(mContinue);
    menuBar.add(mShowSaved);
    this.setJMenuBar(menuBar);
    setSize(600, 300);
    taCharge.setEditable(false);
    JPanel topP = new JPanel();
    topP.add(taCharge);
    this.getContentPane().add(topP, "North");
    setChargeText();
    ambients = new Vector<AmbientCycle>();
    ambients.add(new AmbientCycle()); // the default insulated surface
//    partAmbListener = new PartAmbListener();
  }

  private void setChargeText() {
    if (charge != null)
      taCharge.setText("" + chargeDef);
  }

  class GetChargeListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      getChargeDetails((JMenuItem)e.getSource());
    }
  }

  void getChargeDetails(JMenuItem caller) {
    ChargeDefDlg data ;
    if (caller == defBeamBlankH)
      data = new ChargeDefDlg(this, ChargeDef.BEAMBLANK_H);
    else if (caller == defBeamBlankV)
      data = new ChargeDefDlg(this, ChargeDef.BEAMBLANK_V);
    else
      data = new ChargeDefDlg(this, ChargeDef.RECTANGULAR);

    data.setLocationRelativeTo(this);
    data.setVisible(true);
    chargeDef = data.getChargeDef();
    if (chargeDef == null) {
      errMessage("Charge is not defined");
      return;
    }
    SlicingDlg sliceDlg = new SlicingDlg(this, chargeDef);
    sliceDlg.setLocationRelativeTo(this);
    sliceDlg.setVisible(true);

    double sliceThick = sliceDlg.getSliceSize();
    charge = new ThreeDCharge(chargeDef, sliceThick);
//
//    DataPointSelection dps = new DataPointSelection(this, charge);
//    dps.setLocationRelativeTo(this);
//    dps.setVisible(true);
//    stats = dps.getTempStatPoints();
//    stats.setCollectionLength(1000);
    stats = new TemperatureStats(charge);
    setChargeText();
    chHeatCycle = null;
    setEvalStat(true);
//    mContinue.setEnabled(false);
  }

  void setEvalStat(boolean newEval) {
    if (newEval) {
      bEvalContinue = false;
      mEvaluate.setText(evalNew);
    }
    else {
      bEvalContinue = true;
      mEvaluate.setText(evalContinue);
    }
  }

  void getChAndHeatCycle() {
    if (charge == null) {
      errMessage("Charge not defined!");
      return;
    }
//    chHeatCycle = new ChargeHeatCycle(charge);
//    chHeatCycle.setDataCollection(stats);
//    chargeSurfacePanel();
//    pack();
    if (chInFce == null) {
      chInFce = new ChargeInFurnace(charge, ambients, this);
      mEvaluate.setEnabled(true);
    }
    else {
      chInFce.collectData(ambients);
      mEvaluate.setEnabled(true);
    }
//    chHeatCycle = new ChargeHeatCycle(chInFce);
//    chHeatCycle.setDataCollection(stats);
    chargeSurfacePanel();
    pack();
  }

  void chargeSurfacePanel() {
    if (ambientsSettingPanel != null) {
      this.getContentPane().remove(ambientsSettingPanel);
      ambientsSettingPanel.removeAll();
    }
    ambientsSettingPanel = new QueryPanel();
    Hashtable table =
                ThreeDCharge.getSurfaceNameTable();
    surfaceAmbientSet = new Hashtable();
    int size = table.size();
//    int n = 0;
//    Enumeration keys = table.keys();
//    Object theKey;
//    while (keys.hasMoreElements()) {
//      theKey = keys.nextElement();
//      JButton butt = new JButton((String)(table.get(theKey)));
//      butt.setSize(200, 100);
//      butt.setName("" + ((Integer)theKey).intValue());
// //      butt.addActionListener(partAmbListener);
//      JPanel panel = new JPanel(new BorderLayout());
//      panel.add(butt, BorderLayout.EAST);
//      ambientsSettingPanel.addQuery("", panel);
//      n++;
//    }
//    ambientsSettingPanel.addQuery("Start Temperature",jtStartTemp);
    ambientsSettingPanel.addSpace();
    ambientsSettingPanel.addQuery("Cycle Duration in h",jtMaxTime);
    for (int i = 0; i < 6; i++)
      ambientsSettingPanel.addSpace();
    this.getContentPane().add(ambientsSettingPanel, "Center");
  }


  void evaluate() {
    if (charge == null) {
      errMessage("Charge not defined!");
      return;
    }
    if (chInFce == null || !chInFce.done) {
      errMessage("Charge Surface Ambients NOT specified");
      mEvaluate.setEnabled(false);
      return;
    }
    chHeatCycle = new ChargeHeatCycle(chInFce);
    chHeatCycle.setDataCollection(stats);
    if (chHeatCycle == null) {
      errMessage("Charge Heat Cycle not defined!");
      return;
    }
    startTemp = chInFce.startTemp;
    charge.setBodyTemperature(startTemp);
    chHeatCycle.resetTime();
    continueEval();
    setEvalStat(false);
//    mContinue.setEnabled(true);
  }

  void continueEval() {
    maxTime = new Double(jtMaxTime.getText()).doubleValue();
//    startTemp = new Double(jtStartTemp.getText()).doubleValue();
    chHeatCycle.evaluate(maxTime, true);
    Thread calculThread = new Thread(chHeatCycle);
    calculThread.start();
    try {
     Thread.sleep(100);
    }
     catch (Exception e) {
      ;
    }
    chHeatCycle.addDisplayListener(new WindowAdapter() {
      public void windowOpened(WindowEvent e) {
        calculRunning();
      }
      public void windowClosing(WindowEvent e) {
        calculStopped();
      }
    });
  }

  void showSavedData() {
    TemperatureStats savedStat = ThreeDDisplay.readFromFile(this);
    if (savedStat.oK) {
      ThreeDDisplay savedDisplay =
          new ThreeDDisplay(savedStat.filePath, savedStat.charge, savedStat, true);
      savedDisplay.setVisible(true);
    }
  }

  void calculRunning() {
    mEvaluate.setEnabled(false);
//    mContinue.setEnabled(false);
  }

  void calculStopped() {
    mEvaluate.setEnabled(true);
//    mContinue.setEnabled(true);
  }

  JComboBox ambsCombo = null;

  void getAmbients() {
    SurfaceAmbientsDlg dlg =
            new SurfaceAmbientsDlg(this, ambients);
    dlg.setLocationRelativeTo(this);
    dlg.setVisible(true);
    ambients = dlg.getAmbients();
    ambsCombo = new JComboBox(new DefaultComboBoxModel(ambients));
  }

  void closeAll() {
    System.exit(0);
  }

  TransientHeat theMainWndow = this;

//  void getPartAmbient(int surf) {
//    SurfaceConditionView view =
//        new SurfaceConditionView("ABCD", 20, 15);
//    view.setSize(400, 600);
//    theMainWndow.setEnabled(false);
//    view.setVisible(true);
//  }

//  class PartAmbListener implements ActionListener {
//    public void actionPerformed(ActionEvent ae) {
//      JButton src = (JButton)ae.getSource();
//      int surfN = new Integer(src.getName()).intValue();
//      getPartAmbient(surfN);
//    }
//  }

  public static void main(String[] args) {
    final TransientHeat trHeat = new TransientHeat();
    trHeat.setVisible(true);
    trHeat.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        trHeat.dispose();
        System.exit(0);
      }
    });
  }

  void errMessage(String msg) {
    JOptionPane.showMessageDialog(this, msg, "TransientHeat",
                JOptionPane.ERROR_MESSAGE);
  }

  void debug(String msg) {
    System.out.println("TransientHeat: " + msg);
  }
}

