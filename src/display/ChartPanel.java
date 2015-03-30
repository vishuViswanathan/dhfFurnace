package display;

import basic.*;
import java.awt.*;
import javax.vecmath.*;
import java.text.*;
import javax.swing.event.*;
import java.awt.event.*;

import javax.swing.JPanel;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class ChartPanel
    extends JPanel {
  ChartDataSource theSource;
  int nTraces;
  boolean yScaleAuto = false;
  double xMin, xMax, xDiv, yMinManual, yMaxManual, yDivManual;
  double yMinScale, yMaxScale, yDivScale;
  double xTickMin, yTickMin;
  int xTicks, yTicks;
  String xName, yName, title;
  String xScaleFmt, yScaleFmt;
  DecimalFormat formatX, formatY;
  double[] preffYdiv;
//  Rectangle xScaleR, yScaleR; // for catching mouse click
//  Cursor defaultCusror;
//  Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
//  boolean firstTime = true;
  Button scaleButton = new Button("Y-Auto");
  public ChartPanel(ChartDataSource chartSource) {
    setBackground(SystemColor.white);
    theSource = chartSource;
    setXRange(0, 100, 10, "###.#");
    setYRange(0, 100, 10, "###.#");
    scaleButton.addActionListener(new MouseOnScales());
    add(scaleButton);
   }

  public void setXRange(double xScaleMin, double xScaleMax,
                        double xInterval) {
    xMin = xScaleMin;
    xMax = xScaleMax;
    xDiv = xInterval;
    xTickMin = xMin;
    xTicks = (int)((xMax - xMin)/ xDiv) + 1;
  }

  public void setYscaleAuto(boolean yes) {
    yScaleAuto = yes;
  }

  void setPrefferedYDivList(double[] preffDiv) {
    preffYdiv = preffDiv;
  }

  public void setYRange(double yScaleMin, double yScaleMax,
                        double yInterval) {
    yMinManual = yScaleMin;
    yMaxManual = yScaleMax;
    yDivManual = yInterval;
  }

  public void setXRange(double xScaleMin, double xScaleMax,
                        double xInterval, String xScaleFmt) {
    setXRange(xScaleMin, xScaleMax, xInterval);
    this.xScaleFmt = xScaleFmt;
    formatX = new DecimalFormat(xScaleFmt);
  }

  public void setYRange(double yScaleMin, double yScaleMax,
                        double yInterval, String yScaleFmt) {
    setYRange(yScaleMin, yScaleMax, yInterval);
    this.yScaleFmt = yScaleFmt;
    formatY = new DecimalFormat(yScaleFmt);
  }

  public void setXVariableName(String yVarName) {
    yName = yVarName;
  }

  public void setTitle(String chartTitle) {
    title = chartTitle;
  }

  public void setYVariableName(String xVarName) {
    xName = xVarName;
  }

  public void paint(Graphics g) {
    super.paint(g);
    drawTempProfile(g, false);
//    if (firstTime) {
//      defaultCusror = getCursor();
//      firstTime = false;
//    }
  }

  private void setYrangeIfAuto() {
    double low = 1e50;
    double high = -1e50;
    double val;
    if (yScaleAuto) {
      int nTraces = theSource.traceCount();
      for (int n = 0; n < nTraces; n++) {
        Point2d[] data = theSource.getOneChartData(n);
        if (data != null) {
          int dataLen = data.length;
          for (int i = 0; i < dataLen; i++) {
            val = data[i].y;
            if (val < low)
              low = val;
            else if (val > high)
              high = val;
          }
        }
      }
      double range = high - low;
      if (range > 0) {
        yMinScale = low - range * 0.2;
        yMaxScale = high + range * 0.2;
        yDivScale = (yMaxScale - yMinScale) / 10;
        if (preffYdiv != null) {
          for (int y = 0; y < preffYdiv.length ; y++) {
            if (yDivScale < preffYdiv[y]) {
              yDivScale = preffYdiv[y];
              break;
            }
          }
        }
        yTickMin = ((int)(yMinScale / yDivScale)) * yDivScale;
        yTicks = (int)((yMaxScale - yTickMin)/ yDivScale) + 1;
        yMinScale = yTickMin;
        yMaxScale = yMinScale + yTicks * yDivScale;
      }
      else {
        toggleYscelMode();
      }
    }
    else {
      yMinScale = yMinManual;
      yMaxScale = yMaxManual;
      yDivScale = yDivManual;
      yTickMin = yMinScale;
      yTicks = (int)((yMaxScale - yMinScale)/yDivScale);
    }
  }

//  Rectangle graphRect;

  void drawTempProfile(Graphics g, boolean refresh) {
    int margin = 5;
    Dimension dim = getSize();
    g.setColor(Color.black);
    int right = dim.width - 2 * margin;
    int bottom = dim.height - 2 * margin;
    g.drawRect(margin, margin, right, bottom);
    setYrangeIfAuto();
    drawAxes(g, margin, margin, right, bottom);
    drawTrends(g);
  }

  int absOriginX, absOriginY;
  double xScale, yScale;

  void drawAxes(Graphics g, int orgX, int orgY, int maxX, int maxY) {

    int borderX = 5 + orgX;
    int borderY = 5 + orgY;
    absOriginX = orgX + 50;
    absOriginY = maxY - 30;
    int lx = maxX - absOriginX - borderX;
    xScale = (double) (lx) / (xMax - xMin);
    int ly = absOriginY - borderY;
//    xScaleR = new Rectangle(absOriginX, absOriginY, borderX, absOriginY + 20);
//    yScaleR = new Rectangle(borderX, borderY,absOriginX, absOriginY);
    yScale = (double) (ly) / (yMaxScale - yMinScale);
    g.setColor(Color.blue);
    g.setFont(new Font("Arial", Font.BOLD, 10));
    int tickLen = 3;
    int xLoc, yLoc, textXloc;
    double scaleVal;
    double startLoc = 0;
    // x axis
    g.drawLine(absOriginX, absOriginY, absOriginX + lx, absOriginY);
    scaleVal = xTickMin;
    int ticks = xTicks;
    double tickPitch = xDiv * xScale;
    double tickXLoc = absOriginX + (int) ((scaleVal - xMin) * xScale);;
    int tickEnd = absOriginY + tickLen;
    g.drawLine((int)startLoc, absOriginY, (int)startLoc, tickEnd);
    int tectWby2 = (g.getFontMetrics().stringWidth(xScaleFmt) / 2);
    yLoc = tickEnd + 3 + g.getFontMetrics().getHeight();
    for (int i = 0; i < ticks; i++, tickXLoc += tickPitch,
         startLoc += tickPitch, scaleVal += xDiv) {
      xLoc = (int)tickXLoc;
      g.drawLine(xLoc, absOriginY, xLoc, tickEnd);
      g.drawString(formatX.format(scaleVal), xLoc, yLoc);
    }
    // y axis;
    g.drawLine(absOriginX, absOriginY, absOriginX, absOriginY - ly);
    ticks = xTicks ;
    tickPitch = yDivScale * yScale;
    tickEnd = absOriginX - tickLen;
    g.drawLine(absOriginX, (int)startLoc, tickEnd, (int)startLoc);
    scaleVal = yTickMin;
    startLoc = absOriginY - (int) ((scaleVal - yMinScale) * yScale); // absOriginY;
    textXloc = tickEnd - g.getFontMetrics().stringWidth(yScaleFmt) - 3;
    int textHalfHeight = g.getFontMetrics().getHeight() / 2;
    for (int i = 0; i < ticks; i++, startLoc -= tickPitch, scaleVal += yDivScale) {
      yLoc = (int)startLoc;
      g.drawLine(absOriginX, yLoc, tickEnd, (int)startLoc);
      g.drawString(formatY.format(scaleVal), textXloc, yLoc + textHalfHeight);
    }
  }

  void drawTrends(Graphics g) {
    nTraces = theSource.traceCount();
    for (int n = 0; n < nTraces; n++) {
      drawOneTrend(g, n);
    }
  }

  void drawOneTrend(Graphics g, int traceNum) {
    Point2d[] data = theSource.getOneChartData(traceNum);
    if (data != null) {
      int dataLen = data.length;
      int[] xList = new int[dataLen];
      int[] yList = new int[dataLen];
      for (int i = 0; i < dataLen ; i++) {
        xList[i] = (int) ((data[i].x - xMin)* xScale) + absOriginX;
        yList[i] = absOriginY - (int) ((data[i].y - yMinScale) * yScale);
      }
      g.setColor(theSource.traceColor(traceNum));
      g.drawPolyline(xList, yList, xList.length);
    }
  }

  void toggleYscelMode() {
    yScaleAuto = !yScaleAuto;
    if (yScaleAuto)
      scaleButton.setLabel("Y-Man");
    else
      scaleButton.setLabel("Y-Auto");
    scaleButton.repaint();
    repaint();
  }

//  void switchCursor(boolean hand) {
//    if (hand)
//      setCursor(handCursor);
//    else
//      setCursor(defaultCusror);
//  }

  class MouseOnScales implements ActionListener {
    public void actionPerformed(ActionEvent e) {
        toggleYscelMode();
    }
  }

}