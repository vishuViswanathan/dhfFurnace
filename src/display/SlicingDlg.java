package display;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.Frame;
import java.awt.BorderLayout;
import javax.vecmath.*;
import java.text.*;
import basic.*;

public class SlicingDlg extends JDialog {

  ChargeDef charge = null;
  double width, height, length;
   boolean done = false;
  JButton ok = new JButton(" OK ");
  JButton cancel = new JButton("Cancel");
  int maxSlices = 100000;
  double minSliceSize = 0.005;
  double sliceSize = 0;
  JTextField tfWidth;
  JTextField tfHeight;
  JTextField tfLength;
  JTextField tfMaxSliceElements;
  JTextField tfMinSliceSize;
  JTextField tfSliceSize;
  DecimalFormat format0Dec = new DecimalFormat("####");

  public SlicingDlg(Frame parent, ChargeDef charge) {
    super(parent, "Slicing Details", true);
    this.charge = charge;
    try  {
      jbInit();
      pack();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void jbInit() throws Exception {
    length = charge.getLength();
    width = charge.getWidth();
    height = charge.getHeight();

   tfWidth = new JTextField(format0Dec.format(width * 1000), 5);
   tfWidth.setEditable(false);
   tfHeight = new JTextField(format0Dec.format(height * 1000), 5);
   tfHeight.setEditable(false);
   tfLength = new JTextField(format0Dec.format(length * 1000), 5);
   tfLength.setEditable(false);
   tfMaxSliceElements = new JTextField("" + maxSlices, 5);
   tfMaxSliceElements.setEditable(false);
   tfMinSliceSize = new JTextField(format0Dec.format(minSliceSize * 1000), 5);
   tfMinSliceSize.setEditable(false);
   sliceSize = Math.min(Math.min(width, height), length) / 7;
   if ((width * height * length / sliceSize) > maxSlices)
     sliceSize = Math.pow(width * height * length / (0.5 * maxSlices), 0.333);
   if (sliceSize < minSliceSize) {
     sliceSize = minSliceSize;
   }
   tfSliceSize =
       new JTextField(format0Dec.format(sliceSize * 1000), 5);
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
    jPanel1.add(new JLabel("Charge Width (mm) "), gbc);
    gbc.gridy++;
    jPanel1.add(new JLabel("Charge Height (mm) "), gbc);
    gbc.gridy++;
    jPanel1.add(new JLabel("Charge Length (mm) "), gbc);
    gbc.gridy++;
    jPanel1.add(new JLabel("Max. Elements "), gbc);
    gbc.gridy++;
    jPanel1.add(new JLabel("Minimum Slice size (mm) "), gbc);
    gbc.gridy++;
    jPanel1.add(new JLabel("Slice size (mm) "), gbc);

    // Text fields
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    jPanel1.add(tfWidth, gbc);
    gbc.gridy++;
    jPanel1.add(tfHeight, gbc);
    gbc.gridy++;
    jPanel1.add(tfLength, gbc);
    gbc.gridy++;
    jPanel1.add(tfMaxSliceElements, gbc);
    gbc.gridy++;
    jPanel1.add(tfMinSliceSize, gbc);
    gbc.gridy++;
    jPanel1.add(tfSliceSize, gbc);
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

  double getTFDblvalue(JTextField tf, double min,
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

  int getTFIntvalue(JTextField tf, int min,
              int max, String msg) {
    int value;
    try {
      value = new Double(tf.getText()).intValue();
      if (value < min || value > max)
        value = min;
    }
    catch (Exception e) {
      value = min;
    }
    return value;
  }

  void takeParams() {
    boolean allOK = false;
    do {
      maxSlices = getTFIntvalue(tfMaxSliceElements, 1000, 100000,
                  "Maximum Slices");

      minSliceSize = getTFDblvalue(tfMinSliceSize, 1, 100, "Minimum Slice Size") / 1000;
      if (Double.isNaN(minSliceSize))
        break;
      sliceSize = getTFDblvalue(tfSliceSize, 1, 100, "Slice Size") / 1000;
      if (Double.isNaN(sliceSize))
        break;
      // take the data point info

      allOK = true;
      break;
    } while (true);
    if (allOK)
      closeThisWindow();
  }

  public double getSliceSize() {
    return sliceSize;
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

  void debug(String msg) {
    System.out.println("SlicingDlg: " + msg);
  }

  void errMessage(String msg) {
    JOptionPane.showMessageDialog(null, msg, "Data ERROR",
                JOptionPane.ERROR_MESSAGE);
  }

}

