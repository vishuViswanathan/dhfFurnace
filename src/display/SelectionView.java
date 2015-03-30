package display;

import java.awt.event.*;
import basic.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import javax.swing.event.*;
import javax.swing.border.*;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.behaviors.vp.*;
import com.sun.j3d.utils.universe.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.awt.image.BufferedImage;

import javax.media.j3d.*;

public class SelectionView extends JInternalFrame
    implements TemperatureColorServer, TimeServer { //  implements ChangeListener {
  public static final int YZPLANE = 0;
  public static final int XZPLANE = 1;
  public static final int XYPLANE = 2;
  final double maxSideRatio = 5.0; // for readable display
  final int drgWidth = 240;
  final int drgHeight = 180;
  final double sideAngle = 30.0 / 180.0 * Math.PI;
  double sideAngleCos;
  double sideAngleSin;
  ThreeDArray theObject;
  int sideX, sideY, sideZ;
  int selXloc, selYloc, selZloc;  // selected sections
  Selector xSel, ySel, zSel, timeSel;
  int xPercent, yPercent, zPercent;
  int xLayerPos, yLayerPos, zLayerPos;
  Vector <ChangeListener> yzPlaneListeners;
  Vector <ChangeListener>  xzPlaneListeners;
  Vector <ChangeListener>  xyPlaneListeners;
  Vector <ChangeListener> colorListeners;
  Vector <ChangeListener> timeListeners;

  SimpleUniverse u = null;
  Box billet = null;
  JPanel slicePanel, timePanel;
  TemperatureColorScale colorPanel;

  public SelectionView(String name, ThreeDArray theObject) {
    super(name, true, false, false, false); //new BorderLayout());
    this.theObject = theObject;
    try  {
      jbInit();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  double xSize;
  double ySize;
  double zSize;
  void getOptimumSides() {
    xSize = theObject.getXsize();
    ySize = theObject.getYsize();
    zSize = theObject.getZsize();
    double minSide = Math.min(zSize, Math.min(xSize, ySize));
    double maxSide = Math.max(zSize, Math.max(xSize, ySize));
    double maxminRatio = maxSide / minSide;
    if (maxminRatio > maxSideRatio) {
      double factor = Math.log(maxSideRatio) /
                      Math.log(maxminRatio);
      if (xSize > minSide) {
        xSize = minSide * Math.exp(Math.log(xSize / minSide) * factor);
      }
      if (ySize > minSide) {
        ySize = minSide * Math.exp(Math.log(ySize / minSide) * factor);
      }
      if (zSize > minSide) {
        zSize = minSide * Math.exp(Math.log(zSize / minSide) * factor);
      }
    }
    sideAngleCos = Math.cos(sideAngle);
    sideAngleSin = Math.sin(sideAngle);
    // trial with width
    double drgMinSide1 =
      (double)drgWidth / sideAngleCos /
            (xSize + ySize ) * minSide;
    // trial with height
    double drgMinSide2 =
      (double)drgHeight / sideAngleSin /
            (xSize + ySize + zSize / Math.sin(sideAngle)) * minSide;
    double drgMinSide = Math.min(drgMinSide1, drgMinSide2);
    // final results
    sideX = (int)(xSize / minSide * drgMinSide);
    sideY = (int)(ySize / minSide * drgMinSide);
    sideZ = (int)(zSize / minSide * drgMinSide);
  }


  boolean jbInit() throws Exception {
    slicePanel = new JPanel(new BorderLayout());
    slicePanel.setBorder(new BevelBorder(BevelBorder.RAISED));
    colorPanel = new TemperatureColorScale();
    colorPanel.addChangeListener(new ColorChangeListener());
    colorPanel.setBorder(new BevelBorder(BevelBorder.RAISED));
    timePanel = new JPanel();
    timePanel.setBorder(new BevelBorder(BevelBorder.RAISED));
    timeSel = new Selector("Time", false, false);
    timeSel.setValue(100);
    timeSel.addChangeListener(new TimeChangeListener());
    getOptimumSides();
    ObjectPanel panel = new ObjectPanel();
//    getContentPane().add(panel, BorderLayout.CENTER);
    slicePanel.add(panel, BorderLayout.CENTER);

    SelChangeListener listener = new SelChangeListener();
    xSel = new Selector("YZ Plane", false, false);
    xSel.addChangeListener(listener);
    setSelXloc(50);  // at 50%
//    getContentPane().add(xSel, BorderLayout.NORTH);
    slicePanel.add(xSel, BorderLayout.NORTH);

    ySel = new Selector("XZ Plane", false, true);
    ySel.addChangeListener(listener);
    setSelYloc(50);  // at 50%
//    getContentPane().add(ySel, BorderLayout.SOUTH);
    slicePanel.add(ySel, BorderLayout.SOUTH);

    zSel = new Selector("XY", true, false);
    zSel.addChangeListener(listener);
    setSelZloc(50);  // at 50%
//    getContentPane().add(zSel, BorderLayout.EAST);
    slicePanel.add(zSel, BorderLayout.EAST);

    yzPlaneListeners = new Vector <ChangeListener> ();
    xzPlaneListeners = new Vector <ChangeListener> ();
    xyPlaneListeners = new Vector <ChangeListener> ();
    colorListeners = new Vector <ChangeListener> ();
    timeListeners = new Vector <ChangeListener> ();
    Container mainContainer = getContentPane();
    mainContainer.add(slicePanel, BorderLayout.CENTER);
    mainContainer.add(colorPanel, BorderLayout.EAST);
    timePanel.add(timeSel);
    mainContainer.add(timePanel, BorderLayout.SOUTH);
    return true;
  }

  public void initValues() {
    xSelChanged();
    ySelChanged();
    zSelChanged();
  }


  void xSelChanged() {
    setSelXloc(xSel.getValue());
    // inform listeners
    int size = yzPlaneListeners.size();
    if (size > 0) {
      for (int n = 0; n < size; n++) {
        ((ChangeListener)yzPlaneListeners.elementAt(n)).
                stateChanged(new ChangeEvent(this));
      }
    }
  }

  void setSelXloc(double percent) {
    selXloc = (int)(percent / 100 * sideX);
    xPercent = (int)percent;
    xLayerPos = (int)(percent / 100 * (theObject.getXsize() - 1));
  }

  int getXsel() {
    return xPercent;
  }

  int getXLayerPos() {
    return xLayerPos;
  }

  void ySelChanged() {
    setSelYloc(ySel.getValue());
    // inform listeners
    int size = xzPlaneListeners.size();
    if (size > 0) {
      for (int n = 0; n < size; n++) {
        ((ChangeListener)xzPlaneListeners.elementAt(n)).
                stateChanged(new ChangeEvent(this));
      }
    }
  }

  void setSelYloc(double percent) {
    selYloc = (int)(percent / 100 * sideY);
    yPercent = (int)percent;
    yLayerPos = (int)(percent / 100 * (theObject.getYsize() - 1));
  }

  int getYsel() {
    return yPercent;
  }

  int getYLayerPos() {
    return yLayerPos;
  }

  void zSelChanged() {
    setSelZloc(zSel.getValue());
    // inform listeners
    int size = xyPlaneListeners.size();
    if (size > 0) {
      for (int n = 0; n < size; n++) {
        ((ChangeListener)xyPlaneListeners.elementAt(n)).
                stateChanged(new ChangeEvent(this));
      }
    }
  }

  void setSelZloc(double percent) {
    selZloc = (int)(percent / 100 * sideZ);
    zPercent = (int)percent;
    zLayerPos = (int)(percent / 100 * (theObject.getZsize() - 1));
  }

  int getZsel() {
    return zPercent;
  }

  int getZLayerPos() {
    return zLayerPos;
  }

  public void addSelectionListener(ChangeListener cl,
                  int planeOrient) {
    switch(planeOrient) {
      case YZPLANE:
        yzPlaneListeners.add(cl);
        break;
      case XZPLANE:
        xzPlaneListeners.add(cl);
        break;
      case XYPLANE:
        xyPlaneListeners.add(cl);
        break;
    }
  }

  public void addColorListener(ChangeListener l) {
    colorListeners.add(l);
  }

  public void addTimeListener(ChangeListener l) {
    timeListeners.add(l);
  }

  void informColorListeners() {
    int size = colorListeners.size();
    for (int i = 0; i < size; i++)
      ((ChangeListener)colorListeners.get(i)).stateChanged(null);
  }

  void informTimeListeners() {
    int size = timeListeners.size();
    for (int i = 0; i < size; i++)
      ((ChangeListener)timeListeners.get(i)).stateChanged(null);
  }

  public void removeXselListener(ChangeListener cl,
                  int planeOrient) {
    switch(planeOrient) {
      case YZPLANE:
        yzPlaneListeners.remove(cl);
        break;
      case XZPLANE:
        xzPlaneListeners.remove(cl);
        break;
      case XYPLANE:
        xyPlaneListeners.remove(cl);
        break;
    }
  }

  public Color getTemperatureColor(double temperature) {
    return colorPanel.getTemperatureColor(temperature);
  }

  public double getTimeFraction() {
    return (double)timeSel.getValue() / 100;
  }

  public double getTime() {
    return getTimeFraction();
  }

  final int leftMin = 10;
  final int topMin = 10;

  class ObjectPanel extends JPanel {
    Color sectionLineColor = Color.blue;

    public void paint(Graphics g) {
      drawObject(g);
      drawSectionLines(g);
    }

    void drawObject(Graphics g) {
      Point fromPt =
        new Point(leftMin + (int)(sideY * sideAngleCos),
                  topMin + drgHeight);
      Point toPt = ptAt(fromPt, Math.PI - sideAngle, sideY);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
      fromPt = toPt;

      toPt = ptAt(fromPt, Math.PI / 2, sideZ);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
      fromPt = toPt;

      toPt = ptAt(fromPt, - sideAngle, sideY);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
      fromPt = toPt;

      toPt = ptAt(fromPt, - Math.PI / 2, sideZ);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
      fromPt = toPt;

      toPt = ptAt(fromPt, sideAngle, sideX);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
      fromPt = toPt;

      toPt = ptAt(fromPt, Math.PI / 2, sideZ);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
      fromPt = toPt;

      toPt = ptAt(fromPt, - Math.PI + sideAngle , sideX);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
      fromPt = toPt;

      toPt = ptAt(fromPt, sideAngle , sideX);
      fromPt = toPt;
      toPt = ptAt(fromPt, Math.PI - sideAngle , sideY);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
      fromPt = toPt;

      toPt = ptAt(fromPt, - Math.PI + sideAngle , sideX);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
      fromPt = toPt;
    }

    void drawSectionLines(Graphics g) {
      g.setColor(sectionLineColor);
      Point cornerPt =
        new Point(leftMin + (int)(sideY * sideAngleCos),
                  topMin + drgHeight);
      // x axis
      Point fromPt = cornerPt;
      Point toPt = ptAt(fromPt, sideAngle, selXloc);
      fromPt = toPt;
      toPt = ptAt(fromPt, Math.PI / 2, sideZ);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
      fromPt = toPt;
      toPt = ptAt(fromPt, Math.PI - sideAngle, sideY);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
      // yaxis
      fromPt = cornerPt;
      toPt = ptAt(fromPt, Math.PI - sideAngle, selYloc);
      fromPt = toPt;
      toPt = ptAt(fromPt, Math.PI / 2, sideZ);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
      fromPt = toPt;
      toPt = ptAt(fromPt, sideAngle, sideX);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
      // zaxis
      fromPt = cornerPt;
      toPt = ptAt(fromPt, Math.PI / 2, selZloc);
      fromPt = toPt;
      toPt = ptAt(fromPt, sideAngle, sideX);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
      toPt = ptAt(fromPt, Math.PI - sideAngle, sideY);
      g.drawLine(fromPt.x, fromPt.y, toPt.x, toPt.y);
    }

    void inclinedLine(Graphics g, int x1, int y1, double angle, int length) {
      Point pt = ptAt(new Point(x1, y1), angle, length);
      g.drawLine(x1, y1, pt.x, pt.y);
    }

    Point ptAt(Point fromPt, double angle, double length) {
      Point pt =
        new Point(fromPt.x + (int)(length * Math.cos(angle)),
                  fromPt.y - (int)(length * Math.sin(angle)));
      return pt;
    }
  }

  class ColorChangeListener implements ChangeListener {
    public void stateChanged(ChangeEvent ce) {
      informColorListeners();
    }
  }

  class TimeChangeListener implements ChangeListener {
    public void stateChanged(ChangeEvent ce) {
      informTimeListeners();
    }
  }

  class SelChangeListener implements ChangeListener {
    public void stateChanged(ChangeEvent ce) {
      Selector src = (Selector)ce.getSource();
      if (src == xSel)
        xSelChanged();
      else if (src == ySel)
        ySelChanged();
      else if (src == zSel)
        zSelChanged();
      repaint();
    }
  }

//  public void stateChanged(ChangeEvent e) {
//    update();
//  }

//  public void update() {
//  }

  void errMessage(String msg) {
    System.err.println("SelectionView: ERROR: " + msg);
  }

  void debug(String msg) {
    System.out.println("SelectionView: " + msg);
  }
}

