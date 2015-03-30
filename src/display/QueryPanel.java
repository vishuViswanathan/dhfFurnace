package display;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.Vector;
public class QueryPanel extends JPanel {
  GridBagConstraints gbcLabel =
      new GridBagConstraints();
  GridBagConstraints gbcData =
      new GridBagConstraints();

  public QueryPanel() {
    super(new GridBagLayout());
    try  {
      jbInit();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void jbInit() throws Exception {
    gbcLabel.gridx = 0;
    gbcLabel.gridy = 0;
    gbcLabel.anchor = GridBagConstraints.EAST;

    gbcData.gridx = 1;
    gbcData.gridy = 0;
    gbcData.anchor = GridBagConstraints.WEST;
  }

  public void addQuery(String label, Component comp) {
    add(new JLabel(label + "  "), gbcLabel);
    add(comp, gbcData);
    gbcLabel.gridy++;
    gbcData.gridy++;
  }

  public void addQuery(String label, JTextField value, double min, double max) {
    EvaluateEntry evalE = new EvaluateEntry(min, max);
    value.addActionListener(evalE);
    value.addFocusListener(evalE);
    addQuery(label, value);
  }

  public void addQuery(String label, String value) {
    JTextField text = new JTextField(value);
    text.setEditable(false);
    addQuery(label, text);
  }

  public void addTextLine(String text) {
    add(new JLabel(text), gbcLabel);
    gbcLabel.gridy++;
    gbcData.gridy++;
  }

  public void addSpace() {
    add(new JLabel(" "), gbcLabel);
    add(new JLabel(" "), gbcData);
    gbcLabel.gridy++;
    gbcData.gridy++;
  }

  class EvaluateEntry implements FocusListener, ActionListener {
    JTextField tf;
    boolean itsON = false;
    double min, max;
    public EvaluateEntry(double min, double max) {
     this.min = min;
      this.max = max;
    }
    public void focusGained(FocusEvent e) {

    }
    public void focusLost(FocusEvent e) {
      tf = (JTextField)e.getSource();
      double val = Double.parseDouble(tf.getText());
      if ((val < min || val > max) && !itsON)
        errMessage("Value Out of range", "" + min + " < Value < " + max);
    }

    public void actionPerformed(ActionEvent e) {
      tf = (JTextField)e.getSource();
      double val = Double.parseDouble(tf.getText());
      if (val < min || val > max) {
        itsON = true;
        errMessage("Value Out of range", "" + min + " < Value < " + max);
        itsON = false;
      }
    }
  }
  void debug(String msg) {
    System.out.println("QueryPanel: " + msg);
  }
  void errMessage(String header, String msg) {
    JOptionPane.showMessageDialog(null, msg, header,
                JOptionPane.ERROR_MESSAGE);
  }
}

