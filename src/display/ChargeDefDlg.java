package display;
import java.io.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import basic.*;

public class ChargeDefDlg extends JDialog {
  ChargeDef charge = null;
  double thermalK;
  double spHeat;
  boolean done = false;
  JButton ok = new JButton(" OK ");
  JButton cancel = new JButton("Cancel");
  JButton bLoadFile = new JButton("From File");
  JButton bSaveToFile = new JButton("Save Charge");

  JTextField tfWidth = new JTextField("480", 5);
  JTextField tfHeight = new JTextField("420", 5);
  JTextField tfLength = new JTextField("500", 5);
  JTextField tfFlangeT = new JTextField("120", 5);
  JTextField tfWebT= new JTextField("120", 5);
  JTextField tfDensity = new JTextField("7850", 5);
  JTextField tfTk = new JTextField("30", 5);
  JTextField tfC = new JTextField("0.17", 5);
  JTextField tfMatName = new JTextField("Steel", 15);
  JTextField tfMinSlice = new JTextField("Steel", 15);  // not used?

  int chType = 0; // one of ChargeDef.RECTANGULAR  or ...

  public ChargeDefDlg(Frame parent, int chargeType) {
    super(parent,
          "Charge Details (" + ChargeDef.chargeTypeName(chargeType) + ")", true);
    try  {
      jbInit(chargeType);
      pack();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public ChargeDefDlg() {
    this(null, ChargeDef.RECTANGULAR);
  }

  private void jbInit(int chargeType) throws Exception {
    chType = chargeType;
    if (!ChargeDef.checkTypeValid(chType)) {
        throw  (new Exception());
    }

    addWindowListener(new WindowAdapter () {
       public void windowClosing(WindowEvent we) {
          closeThisWindow();
       }
    } );
    JPanel jPanel1 = new JPanel();
    jPanel1.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    // labels
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.EAST;
    jPanel1.add(new JLabel("Material Name "), gbc);
    gbc.gridy++;
    jPanel1.add(new JLabel("Density (kg/m3) "), gbc);
    gbc.gridy++;
//    jPanel1.add(new JLabel("Constant ThCond (kCal/mhC) "), gbc);
//    gbc.gridy++;
//    jPanel1.add(new JLabel("Constant SpHeat (kCal/kgC) "), gbc);
//    gbc.gridy++;
    switch (chType) {
      case ChargeDef.BEAMBLANK_H:
        jPanel1.add(new JLabel("Beam Height - Along Hearth (mm) "), gbc);
        gbc.gridy++;
        jPanel1.add(new JLabel("Flange Width - Perpendicular to Hearth (mm) "),
                    gbc);
        break;
      case ChargeDef.BEAMBLANK_V:
        jPanel1.add(new JLabel("Flange Width - Along Hearth (mm) "), gbc);
        gbc.gridy++;
        jPanel1.add(new JLabel("Beam Height - Perpendicular to Hearth (mm) "),
                    gbc);
        break;
      default:
        jPanel1.add(new JLabel("Charge Width (mm) "), gbc);
        gbc.gridy++;
        jPanel1.add(new JLabel("Charge Height (mm) "), gbc);
        break;
    }
    if (chType == ChargeDef.BEAMBLANK_H || chType == ChargeDef.BEAMBLANK_V) {
      gbc.gridy++;
      jPanel1.add(new JLabel("Flange Thickness (mm) "), gbc);
      gbc.gridy++;
      jPanel1.add(new JLabel("Web Thickness (mm) "), gbc);
    }
    gbc.gridy++;
    jPanel1.add(new JLabel("Charge Length (mm) "), gbc);

    // Text fields
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    jPanel1.add(tfMatName, gbc);
    gbc.gridy++;
    jPanel1.add(tfDensity, gbc);
    gbc.gridy++;
//    jPanel1.add(tfTk, gbc);
//    gbc.gridy++;
//    jPanel1.add(tfC, gbc);
//    gbc.gridy++;
    jPanel1.add(tfWidth, gbc);
    gbc.gridy++;
    jPanel1.add(tfHeight, gbc);
    if (chType == ChargeDef.BEAMBLANK_H || chType == ChargeDef.BEAMBLANK_V) {
      gbc.gridy++;
      jPanel1.add(tfFlangeT, gbc);
      gbc.gridy++;
      jPanel1.add(tfWebT, gbc);
    }
    gbc.gridy++;
    jPanel1.add(tfLength, gbc);

    getContentPane().add(jPanel1);

    JPanel buttPanel = new JPanel();
    ButtonHandler buttonListener =
              new ButtonHandler();
    ok.addActionListener(buttonListener);
    cancel.addActionListener(buttonListener);
    buttPanel.add(cancel);
    buttPanel.add(ok);
    getContentPane().add(buttPanel, BorderLayout.SOUTH);
  }

  double getTFvalue(JTextField tf, double min,
              double max, String msg) {
    double value;
    try {
      value = new Double(tf.getText()).doubleValue();
      if (value < min || value > max)
        value = Double.NaN;
    }
    catch (Exception e) {
      value = Double.NaN;
    }
    if (Double.isNaN(value))
      errMessage(msg);
    return value;
  }

  void takeParams() {
    boolean allOK = false;
    String name;
    double density, tK, c, width, height, length, flangeT = 0, webT = 0;
    do {
      name = tfMatName.getText();
      density = getTFvalue(tfDensity, 10.0, 100000.0,
                  "Density value");
      if (Double.isNaN(density))
        break;

      tK = getTFvalue(tfTk, 0.01, 1000.0, "ThCond0 value");
      if (Double.isNaN(tK))
        break;

//      c = getTFvalue(tfC, 0.001, 100.0, "SpHeat value");
//      if (Double.isNaN(c))
//        break;

      width = getTFvalue(tfWidth, 10, 3000, "Width value") / 1000;
      if (Double.isNaN(width))
        break;

      height = getTFvalue(tfHeight, 10, 1000, "Heigth value") / 1000;
      if (Double.isNaN(height))
        break;
      if (chType == ChargeDef.BEAMBLANK_H || chType == ChargeDef.BEAMBLANK_V) {
        flangeT = getTFvalue(tfFlangeT, 5, 500, "Flange Thickness") / 1000;
        if (Double.isNaN(flangeT))
          break;
        webT = getTFvalue(tfWebT, 5, 500, "Web Thickness") /1000;
        if (Double.isNaN(webT))
          break;
    }

      length = getTFvalue(tfLength, 100, 15000, "Lenght value") / 1000;
      if (Double.isNaN(length))
        break;
//     String directory =
//          "V:/For Discussion with Techint 200607/Calculations for Maithan/Prilim/";
//      String directory = "V:/Java Programs/transientHeating/classes/Data/";
      String directory = "Data/";

      try {
        FileInputStream fis = new FileInputStream(directory + "SteelTk.2d");
        TwoDTable tkTable = new TwoDTable(fis, "Steel TK");
        fis.close();
        fis = new FileInputStream(directory + "SteelSpHt.2d");
//        fis = new FileInputStream(directory + "SteelSpHt-dummy.2d");
        TwoDTable spHtTable = new TwoDTable(fis, "Steel Sp Ht");
        if (chType == ChargeDef.BEAMBLANK_H || chType == ChargeDef.BEAMBLANK_V) {
          charge = new ChargeDef(chType,
              new ElementType(name, density, tkTable, spHtTable),
              length, width, height, flangeT, webT); // R1 was  width, height, length
        }
        else
          charge = new ChargeDef(chType,
              new ElementType(name, density, tkTable, spHtTable),
              length, width, height, flangeT, webT); // R1 was  width, height, length

      }
      catch (Exception e) {
        errMessage("File Reading, " + e);
      }
//        charge = new ChargeDef(
//            new ElementType(name, density, 30, 0.165),
//            length, width, height); // R1 was  width, height, length

    allOK = true;
      break;
    } while (true);
    if (allOK)
      closeThisWindow();
  }

  void closeThisWindow() {
    setVisible(false);
    dispose();
  }

  class ButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      Object src = ae.getSource();
      if (src == ok) {
        takeParams();
      }
      else if (src == cancel) {
        closeThisWindow();
      }
    }
  }

  public ChargeDef getChargeDef() {
    return charge;
  }

  void errMessage(String msg) {
    JOptionPane.showMessageDialog(this, msg, "Data ERROR",
                JOptionPane.ERROR_MESSAGE);
  }
}

