package display;
import java.text.*;
import java.awt.event.*;
import java.util.*;
import basic.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.image.*;

public class TemperatureColorFrame
          extends JInternalFrame implements ChangeListener{
  public static float colorSwitch = 0.8f;  // for color value calculation
  public static float colorStart = 0.0f;  // for color value calculation
  public static float colorEnd = 1.0f;  // for color value calculation

  Selector colorStartSel;
  Selector colorDeltaSel;
  public static final int XLAYER = 0;
  public static final int YLAYER = 1;
  public static final int ZLAYER = 2;
  int layerOrient;  // XLAYER, YLAYER ...
  ThreeDCharge theObject;
  TemperatureStats stats;
  int dispLayer;
  boolean hideBorderData;
  int rows;
  int cols;
  int layers;
  int rowMin = 0;
  int colMin = 0;
  int rowMax;
  int colMax;
  int unitSide;
  JPanel panel;
  JTextField tfLocalVal;
  JTextField tfSliceThick;
  BufferedImage buffer;
  Graphics2D imageG2;
  String name;

  TimeServer tServer;
  TemperatureColorServer colorServer;

  double time = 100000;    // set high so that it shows the current max time
  static float tMin = 0;
  static float tMax = 1400;

  DecimalFormat formatTemp0 = new DecimalFormat("####0");
  DecimalFormat format3dec = new DecimalFormat("#.###");
//  JTextField jTmaxT = new JTextField(formatTemp0.format(getTempForColor(colorEnd)), 5);
//  JTextField jTminT = new JTextField(formatTemp0.format(getTempForColor(colorStart)), 5);

  static SelectorCoordinator startSelCoordinator = new SelectorCoordinator();
  static SelectorCoordinator delSelCoordinator = new SelectorCoordinator();

//  public TemperatureColorFrame(String name, TemperatureStats theStat,
//              int layerOrient) {
//    this(name, theStat, layerOrient, false);
//  }
//
  public TemperatureColorFrame(String name, TemperatureStats theStat,
                      int layerOrient, boolean hideBorderData,
                      TimeServer timeServer, TemperatureColorServer colorServer) {
    super(name, true, false, true, true);
    noteServers(timeServer, colorServer);
    getContentPane().setLayout(new BorderLayout());
    this.name = name;
    this.layerOrient = layerOrient;
    this.hideBorderData = hideBorderData;
    stats = theStat;
    this.theObject = theStat.charge ;
    try  {
      jbInit();
      setSize(300, 300);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

    public TemperatureColorFrame(String name, TemperatureStats theStat,
                        int layerOrient,
                        boolean hideBorderData, int xSize, int ySize) {
      super(name, true, false, true, true);
      getContentPane().setLayout(new BorderLayout());
      this.name = name;
      this.layerOrient = layerOrient;
      this.hideBorderData = hideBorderData;
      stats = theStat;
      this.theObject = theStat.charge ;
      try  {
        jbInit();
        setSize(xSize, ySize);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    void noteServers(TimeServer timeServer, TemperatureColorServer colorServer) {
      tServer = timeServer;
      this.colorServer = colorServer;
    }

  static boolean mouseOnPanel = false;

  boolean jbInit() throws Exception {
    tfLocalVal = new JTextField(15);
    tfLocalVal.setEditable(false);
    tfLocalVal.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        tfLocalVal_actionPerformed(e);
      }
    });
    tfSliceThick = new JTextField(10);
    tfSliceThick.setText("Slice = " +
                         format3dec.format(theObject.getUnitSide()) + "m");
    JPanel topPanel = new JPanel();
    topPanel.add(new JLabel("Temperature at cursor "));
    topPanel.add(tfLocalVal);
    topPanel.add(tfSliceThick);
    JPanel selPanel = new JPanel(new GridLayout(2,1));

//    colorStartSel = new Selector("Color Low", false, false);
//    colorStartSel.setValue((int)(colorStart * 100));
//    colorStartSel.addChangeListener(new SelChangeListener());
//    colorStartSel.addChangeListener(startSelCoordinator);
//    startSelCoordinator.addFollower(colorStartSel);
//
//    colorDeltaSel = new Selector("Color High", false, false);
//    colorDeltaSel.setValue((int)((colorEnd - colorStart) * 100));
//    colorDeltaSel.addChangeListener(new SelChangeListener());
//    colorDeltaSel.addChangeListener(delSelCoordinator);
//    delSelCoordinator.addFollower(colorDeltaSel);

//    selPanel.add(colorStartSel);
//    selPanel.add(colorDeltaSel);
    this.getContentPane().add(selPanel, BorderLayout.NORTH);
    this.getContentPane().add(topPanel, BorderLayout.SOUTH);
    switch (layerOrient) {
      case XLAYER:
        rows = theObject.getZsize();
        cols = theObject.getYsize();
        layers = theObject.getXsize();
        break;
      case YLAYER:
        rows = theObject.getZsize();
        cols = theObject.getXsize();
        layers = theObject.getYsize();
        break;
      case ZLAYER:
        rows = theObject.getYsize();
        cols = theObject.getXsize();
        layers = theObject.getZsize();
        break;
      default:
        throw new Exception("Unknown layer Orientation");
    }
    if (cols < 3 || rows < 3) {
      errMessage("too few cols(" + cols +
                  " or rows(" + rows + ")!");
      return false;
    }
    final int maxWidth = 512;
    final int maxHeight = 256;
    int unitW;
    int unitH;
    if (hideBorderData) {
      unitW = maxWidth / (cols - 2);
      unitH = maxHeight / (rows - 2);
      colMin = 1;
      rowMin = 1;
      colMax = cols - 1;
      rowMax = rows - 1;
    }
    else {
      unitW = maxWidth / cols;
      unitH = maxHeight / rows;
      colMin = 0;
      rowMin = 0;
      colMax = cols;
      rowMax = rows;
    }
    unitSide = Math.min(unitW, unitH);
    panel = new ColorDisplay();
    panel.addMouseMotionListener(new MouseOnGrid());
    panel.addMouseListener(
        new MouseAdapter() {
          public void mouseEntered(MouseEvent me) {
            mouseOnPanel = true;
          }

          public void mouseExited(MouseEvent me) {
            mouseOnPanel = false;
          }
        }
      );
    JScrollPane scrollPane = new JScrollPane(panel);
    scrollPane.setBackground(Color.black);
    this.getContentPane().add(scrollPane, BorderLayout.CENTER);
    buffer =
          new BufferedImage((colMax - colMin) * unitSide,
                    (rowMax - rowMin) * unitSide,
                    BufferedImage.TYPE_INT_RGB);
    imageG2 = buffer.createGraphics();
    imageG2.setComposite(AlphaComposite.Src);
//    imageG2.setPaintMode();
//    scaleBuffer =
//          new BufferedImage(scaleUnitX,
//                    scaleSteps * scaleUnitY,
//                    BufferedImage.TYPE_INT_RGB);
//    imageScaleG = scaleBuffer.createGraphics();
//
//    ColorScale colScale = new ColorScale();
    JPanel scalePan = new JPanel(new BorderLayout());
//    TempScaleChangeListener tempSetListener = new TempScaleChangeListener();
//    jTmaxT.addActionListener(tempSetListener);
//    jTminT.addActionListener(tempSetListener);
//    scalePan.add(jTmaxT, BorderLayout.NORTH);
//    scalePan.add(colScale, BorderLayout.CENTER);
//    scalePan.add(jTminT, BorderLayout.SOUTH);
    getContentPane().add(scalePan, BorderLayout.EAST);
    return true;
  }

  public void setLayer(int layer) {
    dispLayer = layer;
    setTitle(name + " at " + (int)((float)(layer)/(layers - 1) * 100) + "%");
    repaint();
  }

  public BufferedImage getBufferedImage() {
    return buffer;
  }

  final int leftMin = 10;
  final int topMin = 10;

  class ColorDisplay extends JPanel {
    public void paint(Graphics g) {
      int atX = 0 ;
      int atY = 0;
      Color color;
//      imageG2.setColor(Color.BLACK);
//      imageG2.fillRect(atX, atY, unitSide * colMax, unitSide * rowMax);
      for (int row = rowMin; row < rowMax; row++,
                          atY += unitSide) {
        atX = 0;
        for (int col = colMin; col < colMax; col++,
                            atX += unitSide) {
            if ((color = getTemperatureColor(col, row)) != null) {
              imageG2.setColor(color);
              imageG2.fillRect(atX, atY, unitSide, unitSide);
            }
        }
      }
      g.drawImage(buffer, leftMin, topMin, null);
    }
  }

  float getTempForColor(float calVal) {
    return tMin + calVal * (tMax - tMin);
  }

//  public static Color getTemperatureColor(float temperature) {
//    float colorVal;
//    if (temperature < tMin)
//      colorVal = 0.0f;
//    else if (temperature > tMax)
//      colorVal = 1.0f;
//    else
//      colorVal = (temperature - tMin) / (tMax - tMin);
//    return  getScaleColor(colorVal);
//  }

  static Color getScaleColor(float colorVal) {
    if (colorVal <= colorStart)
      return new Color(0f, 0f, 0f, 0f);
    else if (colorVal >= colorEnd)
      return new Color(1f, 1f, 0, 1f);
    else {
      colorSwitch = colorStart + (colorEnd - colorStart) * 0.8f;
      if (colorVal > colorSwitch) {
        return new Color(1.0f,
                         (colorVal - colorSwitch) / (colorEnd - colorSwitch),
                         0.0f, (colorVal - colorStart) / (colorEnd - colorStart));
      }
      else {
//        return new Color( (colorVal - colorStart) / (colorEnd - colorStart),
//                         0.0f,
//                         0.0f, (colorVal - colorStart) / (colorEnd - colorStart));
       return new Color( (colorVal - colorStart) / (colorSwitch - colorStart),
                        0.0f,
                        0.0f, (colorVal - colorStart) / (colorEnd - colorStart));
      }
    }
  }

  Color getTemperatureColor(int atCol, int atRow) {
    float value = 0.0f;
    switch (layerOrient) {
      case XLAYER:
        if (!theObject.isPartOfCharge(dispLayer, colMax - atCol, rowMax - atRow))
          return null;
        value = (float)stats.getTemperatureDataAt(time, dispLayer, colMax - atCol, rowMax - atRow);
        break;
      case YLAYER:
        if (!theObject.isPartOfCharge(atCol, dispLayer, rowMax - atRow))
          return null;
        value = (float)stats.getTemperatureDataAt(time, atCol, dispLayer, rowMax - atRow);
        break;
      case ZLAYER:
        if (!theObject.isPartOfCharge(atCol, rowMax - atRow, dispLayer))
          return null;
        value = (float)stats.getTemperatureDataAt(time, atCol, rowMax - atRow, dispLayer);
        break;
    }
    return colorServer.getTemperatureColor(value);
  }

  Point lastPt;
  DecimalFormat formatPos = new DecimalFormat("#0.000");
  DecimalFormat formatTemp = new DecimalFormat("####0.0");

  void showValue(Point pt) {
    lastPt = pt;
    boolean done = false;
    if (mouseOnPanel && pt.x >= leftMin && pt.y >= topMin) {
      int xPos = (pt.x - leftMin) / unitSide;
      int yPos = (pt.y - topMin) / unitSide;
      if (hideBorderData) {
        xPos++;
        yPos++;
      }
      if (xPos < colMax && yPos < rowMax) {
        float value = 0.0f;
        double x = 0, y = 0;
        switch (layerOrient) {
          case XLAYER:
            value = (float)stats.getTemperatureDataAt(time, dispLayer, colMax - xPos, rowMax - yPos);
            x = theObject.getYLocation(colMax - xPos);
            y = theObject.getZLocation(rowMax - yPos);
            break;
          case YLAYER:
            value = (float)stats.getTemperatureDataAt(time, xPos, dispLayer, rowMax - yPos);
            x = theObject.getXLocation(xPos);
            y = theObject.getZLocation(rowMax - yPos);
            break;
          case ZLAYER:
            value = (float)stats.getTemperatureDataAt(time, xPos, rowMax - yPos, dispLayer);
            x = theObject.getXLocation(xPos);
            y = theObject.getYLocation(rowMax - yPos);
            break;
        }
        tfLocalVal.setText("" + formatTemp.format(value) + " at " + formatPos.format(x) + ", " +
                          formatPos.format(y));
        done = true;
      }
    }
    if (!done)
      tfLocalVal.setText("");
  }

  void showValue() {
    if (lastPt != null);
    showValue(lastPt);
  }
// void setTime(double theTime) {
//   time = theTime;
// }

  class MouseOnGrid extends MouseMotionAdapter {
    public void mouseMoved(MouseEvent me) {
      showValue(me.getPoint());
    }
  }

  Vector colorChangeL = new Vector();

//  void colorSetChanged(ChangeEvent ce) {
//    float minSpan = 0.03f; // approx 4 C in 1400 C
//    if (ce.getSource() == colorStartSel) {
//      int startVal = colorStartSel.getValue();
//      colorStart = (float)((float)startVal / 100);
//      if (colorStart > (colorEnd - minSpan)) {
//        colorEnd = colorStart + minSpan;
//        colorDeltaSel.setValue((int)(colorEnd * 100));
//      }
//    }
//    else {
//      int deltaVal = colorDeltaSel.getValue();
//      colorEnd = (float)((float)deltaVal / 100);
//      if (colorEnd < (colorStart + minSpan)) {
//        colorStart = colorEnd - minSpan;
//        colorStartSel.setValue((int)(colorStart*100));
//      }
//    }

//    jTmaxT.setText(formatTemp0.format(getTempForColor(colorEnd)));
//    jTminT.setText(formatTemp0.format(getTempForColor(colorStart)));

//    Iterator followerIter = colorChangeL.iterator();
//    while (followerIter.hasNext()) {
//      ((ChangeListener)followerIter.next()).stateChanged(null);
//    }
//  }

//  void colorSetChanged(ActionEvent e) {
//    float minSpan = (float)(4/1400); // approx 4 C in 1400 C
//    if (e.getSource() == jTminT) {
//      int startVal = Double.valueOf(jTminT.getText()).intValue();
//      colorStart = (float) ( (float) startVal / 1400);
//      if (colorStart > (colorEnd - minSpan)) {
//        colorEnd = colorStart + minSpan;
//        colorDeltaSel.setValue( (int) (colorEnd * 100));
//      }
//    }
//    else {
//      int deltaVal = Double.valueOf(jTmaxT.getText()).intValue();
//      colorEnd = (float) ( (float) deltaVal / 1400);
//      if (colorEnd < (colorStart + minSpan)) {
//        colorStart = colorEnd - minSpan;
//        colorStartSel.setValue( (int) (colorStart * 100));
//      }
//
//    }
//    colorStartSel.setValue((int)(colorStart * 100));
//    colorDeltaSel.setValue((int)(colorEnd * 100));
//
//    Iterator followerIter = colorChangeL.iterator();
//    while (followerIter.hasNext()) {
//      ((ChangeListener)followerIter.next()).stateChanged(null);
//    }
//  }
//
//  public void addColorChangeListener(ChangeListener l) {
//    colorChangeL.add(l);
//  }

  void noteColorChange() {
    repaint();
  }

  public void stateChanged(ChangeEvent e) {
    time = tServer.getTime() * stats.lastTimePoint();
    noteColorChange();
  }

//  class TempScaleChangeListener implements ActionListener {
//    public void actionPerformed(ActionEvent e) {
//      colorSetChanged(e);
//      repaint();
//    }
//  }
//
//  class SelChangeListener implements ChangeListener {
//    public void stateChanged(ChangeEvent ce) {
//      colorSetChanged(ce);
//      repaint();
//    }
//
//    public void addFollowers(Selector sel) {
//
//    }
//  }

/*
  final static int scaleSteps = 50;
  int scaleUnitX = 50;
  int scaleUnitY = 5;
  BufferedImage scaleBuffer;
  Graphics2D imageScaleG;

  class ColorScale extends JPanel {
    public ColorScale() {
      super();
    }

    public void paint(Graphics g) {
      float oneScaleStep = (colorEnd - colorStart) / scaleSteps;
      float pos = colorEnd;
      int atY = 0 ;
      for (int row = 1; row < scaleSteps; row++,
                          pos -= oneScaleStep, atY += scaleUnitY) {
            imageScaleG.setColor(Color.BLACK);
            imageScaleG.fillRect(0, atY, scaleUnitX, scaleUnitY);
            imageScaleG.setColor(getScaleColor(pos));
            imageScaleG.fillRect(0, atY, scaleUnitX, scaleUnitY);
      }
      g.drawImage(scaleBuffer, 0, 0, null);
    }
  }
*/

  void errMessage(String msg) {
    System.err.println("TemperatureColorFrame: ERROR: " + msg);
  }

  static void debug(String msg) {
    System.out.println("TemperatureColorFrame: " + msg);
  }

  void tfLocalVal_actionPerformed(ActionEvent e) {

  }


}

