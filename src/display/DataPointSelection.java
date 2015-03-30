package display;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.Frame;
import java.awt.BorderLayout;
import javax.vecmath.*;
import java.text.*;
import basic.*;

// NOT USED
// NOT USED

public class DataPointSelection extends JDialog {
  ThreeDCharge charge = null;
  double width, height, length;
  boolean done = false;
  JButton ok = new JButton(" OK ");
  JButton cancel = new JButton("Cancel");
  JTextField tfWidth;
  JTextField tfHeight;
  JTextField tfLength;
  JTextField[] tfPoint = new JTextField[10];
  JTextField[] tfActutalPoint = new JTextField[10];
  Point3d[] points = new Point3d[10];
  Point3d[] actualPoints = new Point3d[10];
  Point3d defaultPt = new Point3d(-1, 0, 0);
  PointInCharge[] pointInCharge = new PointInCharge[10];
  DecimalFormat format3Dec = new DecimalFormat("#.###");
  JButton[] colorB;

  public DataPointSelection(Frame parent, ThreeDCharge charge) {
    super(parent, "Data Points", true);
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
    length = charge.length;
    width = charge.width;
    height = charge.height;

   tfWidth = new JTextField("" + width, 5);
   tfWidth.setEditable(false);
   tfHeight = new JTextField("" + height, 5);
   tfHeight.setEditable(false);
   tfLength = new JTextField("" + length, 5);
   tfLength.setEditable(false);
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
    jPanel1.add(new JLabel("Charge Width (m) "), gbc);
    gbc.gridy++;
    jPanel1.add(new JLabel("Charge Height (m) "), gbc);
    gbc.gridy++;
    jPanel1.add(new JLabel("Charge Length (m) "), gbc);
    for (int n = 0; n < tfPoint.length; n++) {
      gbc.gridy++;
      JButton jb = new JButton("Data Location " + (n + 1));
      jb.setName("" + n);
      jPanel1.add(jb, gbc);
      jb.addActionListener(new PointAction());
    }

    // Text fields of desired point
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    jPanel1.add(tfWidth, gbc);
    gbc.gridy++;
    jPanel1.add(tfHeight, gbc);
    gbc.gridy++;
    jPanel1.add(tfLength, gbc);
    for (int n = 0; n < tfPoint.length; n++) {
      points[n] = new Point3d(defaultPt);
      tfPoint[n] = new JTextField(10);
      tfPoint[n].setEditable(false);
      setPointField(n);
      gbc.gridy++;
      jPanel1.add(tfPoint[n], gbc);
      tfPoint[n].addActionListener(new PointAction());
    }

    // Text fields of actual point
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridy++;
    gbc.gridy++;
    ColorButtonListener colorBListener = new ColorButtonListener();
    colorB = new JButton[tfPoint.length];
    for (int n = 0; n < tfPoint.length; n++) {
      actualPoints[n] = new Point3d(defaultPt);
      tfActutalPoint[n] = new JTextField(12);
      tfActutalPoint[n].setEditable(false);
      gbc.gridy++;
      colorB[n] = new JButton("  ");
      colorB[n].setName("" + n);
      colorB[n].addActionListener(colorBListener);
      jPanel1.add(tfActutalPoint[n], gbc);
      gbc.gridx = 3;
      jPanel1.add(colorB[n], gbc);
      gbc.gridx = 2;
    }
    setDefaultPoints();

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

  void setDefaultPoints() {
    double l = charge.length;
    double w = charge.width;
    double h = charge.height;
    // take 5 default points - 5mm and 20 mm inside the top and bottom and
    // one mid-height, all taken at mid length and mid width
    points[4] = new Point3d(l/2, w/2, 0.005);
    points[3] = new Point3d(l/2, w/2, 0.020);
    points[2] = new Point3d(l/2, w/2, h/2);
    points[1] = new Point3d(l/2, w/2, h - 0.02);
    points[0] = new Point3d(l/2, w/2, h - 0.005);
    for (int n = 0; n < 5; n++) {
      setPointField(n);
      setActualPointField(n);
    }
    for (int n = 5; n < points.length; n++) {
      points[n] = new Point3d(defaultPt);
      setPointField(n);
      actualPoints[n] = new Point3d( -1, -1, -1);
    }
  }

  void setPointField(int n) {
    tfPoint[n].setText(format3Dec.format(points[n].x) + ", " +
                       format3Dec.format(points[n].y) + ", " +
                       format3Dec.format(points[n].z));
  }

  void setActualPointField(int n) {
    try {
      pointInCharge[n] =
          new PointInCharge(charge, new Integer(n + 1), points[n].x,
                            points[n].y, points[n].z);
      actualPoints[n] = pointInCharge[n].getActualLoc();
      tfActutalPoint[n].setText(format3Dec.format(actualPoints[n].x) + ", " +
                                format3Dec.format(actualPoints[n].y) + ", " +
                                format3Dec.format(actualPoints[n].z));
      pointInCharge[n].color = colorB[n].getBackground();
    }
    catch (Exception e) {
      errMessage("Invalid point");
      pointInCharge[n] = null;
      actualPoints[n].set(defaultPt);
      tfActutalPoint[n].setText("");
    }
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

  void selectColor(JButton src) {
    int n = new Integer(src.getName()).intValue();
    Color oldColor = src.getBackground();
    Color newColor = JColorChooser.showDialog(this, "Choose Trend Color", oldColor);
    if (newColor != null) {
      src.setBackground(newColor);
      if (pointInCharge[n] != null)
        pointInCharge[n].color = newColor;
    }
  }

  void takeParams() {
    boolean allOK = false;
    do {
      // take the data point info
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

  public TemperatureStats getTempStatPoints() {
    TemperatureStats ts = new TemperatureStats(charge);
    for (int n = 0; n < points.length; n++) {
      if (pointInCharge[n] != null)
        ts.addPoint(pointInCharge[n]);
    }
    return ts;
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

  class PointAction implements ActionListener {
    JButton src = null;
    public void actionPerformed(ActionEvent ae) {
      src = (JButton)ae.getSource();
      String name = src.getName();
      int n = new Integer(name).intValue();
      getPoint(n, src);
    }
  }

  class ColorButtonListener implements ActionListener {
    JButton src = null;
    public void actionPerformed(ActionEvent ae) {
      src = (JButton)ae.getSource();
      String name = src.getName();
      selectColor(src);
    }
  }

  void getPoint(int n, Component c) {
    Integer id = new Integer(n + 1);
    OnePointDlg dlg = new OnePointDlg(null, id, points[n]);
    dlg.setLocationRelativeTo(c);
    dlg.setVisible(true);
    setPointField(n);
    setActualPointField(n);
  }

  void debug(String msg) {
    System.out.println("DataPointSelection: " + msg);
  }

  void errMessage(String msg) {
    JOptionPane.showMessageDialog(null, msg, "Data ERROR",
                JOptionPane.ERROR_MESSAGE);
  }

  public class OnePointDlg extends JDialog {
    BorderLayout borderLayout1 = new BorderLayout();
    JPanel jPane1 = new JPanel();
    JTextField tfID = new JTextField(5);
    JTextField tfL = new JTextField(5);
    JTextField tfW = new JTextField(5);
    JTextField tfH = new JTextField(5);
    JButton okButton = new JButton("OK");
    JButton cancelButton = new JButton("Cancel");
    Point3d point;
    Integer id;

    public OnePointDlg(Frame parent, Integer id,Point3d point){
      super(parent, "Data Point", true);
      this.id = id;
      this.point = point;
      try {
        jbInit();
        pack();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    private void jbInit() throws Exception {
      jPane1.setLayout(borderLayout1);
      getContentPane().add(jPane1);

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
      jPane1.add(buttP, "South");

      QueryPanel mainP = new QueryPanel();
      tfID.setText("" + id);
      tfID.setEditable(false);
      mainP.addQuery("Point numerical ID)", tfID);
      tfL.setText("" + point.x);
      mainP.addQuery("Length Position in m", tfL);
      tfW.setText("" + point.y);
      mainP.addQuery("Width Position in m", tfW);
      tfH.setText("" + point.z);
      mainP.addQuery("Height Position in m", tfH);
      jPane1.add(mainP, "Center");
    }

    void takeData() {
      double l = 0, w = 0, h = 0;
      boolean ok = false;
      try {
        l = new Double(tfL.getText()).doubleValue();
        w = new Double(tfW.getText()).doubleValue();
        h = new Double(tfH.getText()).doubleValue();
        ok = true;
      }
      catch (Exception e) {
        errMessage("Some invalid Data");
      }
      if (ok) {
        point.set(l, w, h);
        quit();
      }
    }

    void quit() {
      dispose();
      setVisible(false);
    }

  }
}
