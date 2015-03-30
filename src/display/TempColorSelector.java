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


import javax.swing.JPanel;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2005
 * Company:
 * @author
 * @version 1.0
 */

public class TempColorSelector extends JPanel {
  float colorSwitch = 0.8f;  // for color value calculation
  float colorStart = 0.0f;  // for color value calculation
  float colorEnd = 1.0f;  // for color value calculation

  DecimalFormat formatTemp0 = new DecimalFormat("####0");
  JTextField jTmaxT = new JTextField(formatTemp0.format(getTempForColor(colorEnd)), 5);
  JTextField jTminT = new JTextField(formatTemp0.format(getTempForColor(colorStart)), 5);

  final static int scaleSteps = 50;
  int scaleUnitX = 50;
  int scaleUnitY = 5;
  BufferedImage scaleBuffer;
  Graphics2D imageScaleG;
  static float tMin = 0;
  static float tMax = 1400;

  public TempColorSelector() {
      try  {
        jbInit();
//        setSize(xSize, ySize);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
  }

  boolean jbInit() throws Exception {
    JPanel selPanel = new JPanel(new GridLayout(2,1));

    scaleBuffer =
          new BufferedImage(scaleUnitX,
                    scaleSteps * scaleUnitY,
                    BufferedImage.TYPE_INT_RGB);
    imageScaleG = scaleBuffer.createGraphics();

    ColorScale colScale = new ColorScale();
    JPanel scalePan = new JPanel(new BorderLayout());
//    scalePan.add(jTmaxT, BorderLayout.NORTH);
    scalePan.add(colScale, BorderLayout.CENTER);
//    scalePan.add(jTminT, BorderLayout.SOUTH);
    add(scalePan, BorderLayout.EAST);
    return true;
  }

  float getTempForColor(float calVal) {
    return tMin + calVal * (tMax - tMin);
  }

  public Color getScaleColor(float colorVal) {
    if (colorVal <= colorStart)
      return new Color(0f, 0f, 0f, 0f);
    else if (colorVal >= colorEnd)
      return new Color(1f, 1f, 0, 1f);
    else {
      colorSwitch = colorStart + (colorEnd - colorStart) * 0.8f;
      if (colorVal > colorSwitch)
        return new Color(1.0f, (colorVal - colorSwitch) /(colorEnd -colorSwitch),
                   0.0f, (colorVal - colorStart) / (colorEnd - colorStart));
      else
        return new Color((colorVal- colorStart) /(colorSwitch - colorStart), 0.0f,
                   0.0f, (colorVal - colorStart) / (colorEnd - colorStart));
    }
  }

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
            imageScaleG.setColor(getScaleColor(pos));
            imageScaleG.fillRect(0, atY, scaleUnitX, scaleUnitY);
      }
      g.drawImage(scaleBuffer, 0, 0, null);
    }
  }

}