package display;

import java.text.*;
import javax.swing.*;
import java.awt.image.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class TemperatureColorScale extends JPanel {
  public static float colorSwitch = 0.8f;  // for color value calculation
  public static float colorStart = 0.0f;  // for color value calculation
  public static float colorEnd = 1.0f;  // for color value calculation

  static double tMin = 0;
  static double tMax = 1400;
  static double tMinDiff = 20;

  DecimalFormat formatTemp0 = new DecimalFormat("####0");
  DecimalFormat format3dec = new DecimalFormat("#.###");
  JTextField jTmaxT = new JTextField(formatTemp0.format(getTempForColor(colorEnd)), 5);
  JTextField jTminT = new JTextField(formatTemp0.format(getTempForColor(colorStart)), 5);

  Vector  <ChangeListener> changeListeners = new Vector <ChangeListener> ();

  public TemperatureColorScale() {
    super(new BorderLayout());
    TempScaleChangeListener l = new TempScaleChangeListener();
    jTmaxT.addActionListener(l);
    add(jTmaxT, BorderLayout.NORTH);
    jTminT.addActionListener(l);
    add(jTminT, BorderLayout.SOUTH);
    ColorScale colScale = new ColorScale();
    add(colScale, BorderLayout.CENTER);
  }

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

  double getTempForColor(float calVal) {
    return tMin + calVal * (tMax - tMin);
  }

  public Color getTemperatureColor(double temperature) {
    float colorVal;
    if (temperature < tMin)
      colorVal = 0.0f;
    else if (temperature > tMax)
      colorVal = 1.0f;
    else
      colorVal = (float)((temperature - tMin) / (tMax - tMin));
    return  getScaleColor(colorVal);
  }

  public void addChangeListener(ChangeListener listener) {
    changeListeners.add(listener);
  }

  void noteChanges(ActionEvent e) {
    float minSpan = (float) (tMinDiff / (tMax  - tMin)) ; // approx 4 C in 1400 C
    if (e.getSource() == jTminT) {
      double startVal = Double.valueOf(jTminT.getText()).doubleValue();
      if (startVal < tMin)
        startVal = tMin;
      else if (startVal > (tMax - tMinDiff))
        startVal = tMax - tMinDiff;
      jTminT.setText("" + Math.round(startVal));
      colorStart = (float) ( (float) startVal / tMax);
      if (colorStart > (colorEnd - minSpan)) {
        colorEnd = colorStart + minSpan;
        jTmaxT.setText("" + Math.round(colorEnd * tMax));
      }
    }
    else {
      double endVal = Double.valueOf(jTmaxT.getText()).doubleValue();
      if (endVal < tMin + tMinDiff)
        endVal = tMin + tMinDiff;
      else if (endVal > tMax)
        endVal = tMax;
      jTmaxT.setText("" + Math.round(endVal));
      colorEnd = (float) ( (float) endVal / tMax);
      if (colorEnd < (colorStart + minSpan)) {
        colorStart = colorEnd - minSpan;
        jTminT.setText("" + Math.round(colorStart * tMax));
      }

    }
//    inform listeners
    int size = changeListeners.size();
    for (int i = 0; i < size; i++)
      ( (ChangeListener) (changeListeners.get(i))).stateChanged(new ChangeEvent(this));
  }

  class TempScaleChangeListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      noteChanges(e);
    }
  }

  final static int scaleSteps = 50;
  int scaleUnitX = 50;
  int scaleUnitY = 5;
  BufferedImage scaleBuffer;
  Graphics2D imageScaleG;

  class ColorScale
      extends JPanel {
    public ColorScale() {
      super();
    }

    public void paint(Graphics g) {
      float oneScaleStep = (colorEnd - colorStart) / scaleSteps;
      float pos = colorEnd;
      int atY = 0 ;
      scaleUnitY = (int)(getSize().getHeight()/scaleSteps);
      for (int row = 1; row < scaleSteps; row++,
                          pos -= oneScaleStep, atY += scaleUnitY) {
            g.setColor(Color.BLACK);
            g.fillRect(0, atY, scaleUnitX, scaleUnitY);
            g.setColor(getScaleColor(pos));
            g.fillRect(0, atY, scaleUnitX, scaleUnitY);
      }
    }
  }

  static void debug(String msg) {
    System.out.println("TemperatureColorScale: " + msg);
  }

}