package display;

import java.awt.event.*;
import javax.swing.*;
import java.awt.Frame;
import java.awt.BorderLayout;

import basic.*;

public class OneCycleElementDlg extends JDialog {
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel jPanel1 = new JPanel();
  JTextField tfTime = new JTextField(5);
  JTextField tfTemperature = new JTextField(5);
  JTextField tfHtTrCoeff = new JTextField(5);
  JButton okButton =new JButton("OK");
  JButton cancelButton =new JButton("Cancel");
  OneCycleElement element;

  public OneCycleElementDlg(Frame parent) {
    this(parent, null);
  }

  public OneCycleElementDlg(Frame parent,
          OneCycleElement element) {
    super(parent, "One Cycle Element", true);
    this.element = element;
    initTextFields();
    try  {
      jbInit();
      pack();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void jbInit() throws Exception {
    jPanel1.setLayout(borderLayout1);
    getContentPane().add(jPanel1);

    JPanel buttP = new JPanel();
    cancelButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          quit();
        }
      });
    buttP.add(cancelButton);
    okButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          takeData();
        }
      });
    buttP.add(okButton);
    jPanel1.add(buttP, "South");

    QueryPanel mainP = new QueryPanel();
    mainP.addQuery("At time(h)", tfTime);
    mainP.addQuery("Temperature(C)", tfTemperature);
    mainP.addQuery("Heat Transfer Coeff(kcal/m2hC)", tfHtTrCoeff);
    jPanel1.add(mainP, "Center");
  }

  void initTextFields() {
    if (element != null) {
      double[] data = element.getElements();
      tfTime.setText("" + data[0]);
      tfTemperature.setText("" + data[1]);
      tfHtTrCoeff.setText("" + data[2]);
    }
  }


  void takeData() {
    double time = 0, temp =0, htCoeff = 0;
    boolean ok = false;
    try {
      time = new Double(tfTime.getText()).doubleValue();
      temp = new Double(tfTemperature.getText()).doubleValue();
      htCoeff = new Double(tfHtTrCoeff.getText()).doubleValue();
      ok = true;
    }
    catch (Exception e) {
      errMessage("Some invalid Data");
    }
    if (ok) {
      if (element == null)
        element = new OneCycleElement(time, temp, htCoeff);
      else
        element.setElements(time, temp, htCoeff);
      quit();
    }
  }

  void quit() {
    dispose();
    setVisible(false);
  }

  public OneCycleElement getElement() {
      return element;
  }

  void errMessage(String msg) {
    JOptionPane.showMessageDialog(null, msg, "OneCycleElementDlg",
                JOptionPane.ERROR_MESSAGE);
  }

  void debug(String msg) {
    System.out.println("OneCycleElementDlg: " + msg);
  }
}



