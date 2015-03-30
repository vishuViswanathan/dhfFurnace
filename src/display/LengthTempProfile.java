package display;

import basic.*;
import java.util.*;
import java.awt.*;
import java.math.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.image.*;

import java.text.*;
import java.awt.event.*;
import javax.vecmath.*;


/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class LengthTempProfile extends JFrame {
  TemperatureStats tempStat;

  double width, height, unitCell, halfUnitCell;
  int xCells, yCells;
  int unitSide;

  static int MAXCROSSPOINTS = 6;
  Color[] pointColor = new Color[MAXCROSSPOINTS];
  Vector<Point> crossSpoints = new Vector<Point>();
  double timeForCrossPoint, maxTime;
  int timeLocForCrossPoint;
  boolean markArea[][];

  Color markColor = Color.red;

  FaceDisplay face;

  Button areaSelectedButton, okButon, cancellButton, exitButton;
  JTextField tbCursor = new JTextField(8);

  BufferedImage buffer;
  Graphics2D imageG2;
  ChartPanel profilePanel;
  Selector timeSelector;
  public LengthTempProfile(String name, TemperatureStats tempStat) {
    super(name);
    this.tempStat = tempStat;
    xCells = tempStat.charge.getYsize();
    yCells = tempStat.charge.getZsize();
    maxTime = tempStat.lastTimePoint();
    timeForCrossPoint = maxTime;
    try  {
      jbInit();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  boolean mouseOnPanel = false;

  boolean jbInit() throws Exception {
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    final int maxWidth = 150;
    final int maxHeight = 150;
    int unitW;
    int unitH;
    unitW = maxWidth / (xCells - 2);
    unitH = maxHeight / (yCells - 2);
    unitSide = Math.min(unitW, unitH);

    markArea = new boolean[xCells][yCells];
    resetMarkArea();
    for (int x = 0; x < xCells; x++)
      for (int y = 0; y < yCells; y++)
        markArea[x][y] = false;
    face = new FaceDisplay();
    MouseOnGrid mouseOnGrid = new MouseOnGrid();
    face.addMouseListener(mouseOnGrid);
    face.addMouseMotionListener(mouseOnGrid);
    buffer =
          new BufferedImage((xCells - 2) * unitSide,
                    (yCells - 2) * unitSide,
                    BufferedImage.TYPE_INT_RGB);
    imageG2 = buffer.createGraphics();

    JPanel buttonPanel = new JPanel(new GridLayout(3,1));
    okButon = new Button("Confirm");
    okButon.addActionListener(
       new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            saveSelection();
          }
        });
    cancellButton = new Button("Cancel Selection");
    cancellButton.addActionListener(
       new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            cancellSelection();
          }
        });
    exitButton = new Button("Exit");
    exitButton.addActionListener(
       new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            exitView();
          }
        });
    okButon.setEnabled(false);
    JPanel faceAndButtons = new JPanel(new GridLayout(1,3));
    faceAndButtons.add(face);
    timeSelector = new Selector("Time", false, false);
    timeSelector.setValue((int)(100));
    timeSelector.addChangeListener(new TimeChangeListener());
    buttonPanel.add(timeSelector);
    buttonPanel.add(cancellButton);
    buttonPanel.add(exitButton);
    faceAndButtons.add(buttonPanel);
    getContentPane().add(faceAndButtons, BorderLayout.NORTH);
    profilePanel = new ChartPanel(new ChartSource());
    profilePanel.setXRange(0, tempStat.charge.length,
                           tempStat.charge.length / 10, "###.##");
    profilePanel.setYRange(0, 1400, 200, "####");
    profilePanel.setYscaleAuto(true);
    profilePanel.setPrefferedYDivList(new double[]{1, 2, 5, 10, 20, 50, 100, 200});
    getContentPane().add(profilePanel, BorderLayout.CENTER);
    pack();
    setPointColors();
    return true;
  }
  void setPointColors() {
    pointColor[0] = Color.red;
    pointColor[1] = Color.blue;
    pointColor[2] = Color.black;
    pointColor[3] = Color.cyan;
    pointColor[4] = Color.green;
    pointColor[5] = Color.yellow;
  }

  boolean addCrossPoint(Point mousePt) {
   boolean retVal = false;
   Point chargePoint = getValidPoint(mousePt);
   if (chargePoint != null) {
     if (crossSpoints.size() < MAXCROSSPOINTS) {
       crossSpoints.add(chargePoint);
       markArea[chargePoint.x][chargePoint.y] = true;
       repaint();
       retVal = true;
     }
   }
   return retVal;
 }

  void removeCrossPoint(Point mousePt) {
    Point chargePoint = getValidPoint(mousePt);
    if (chargePoint != null) {
      crossSpoints.remove(chargePoint);
      markArea[chargePoint.x][chargePoint.y] = false;
      repaint();
    }
  }

  void setCrossPointsTime(double t) {
    timeForCrossPoint = t;
  }


  void resetMarkArea() {
    for (int x = 0; x < xCells; x++)
      for (int y = 0; y < yCells; y++)
        markArea[x][y] = false;
    crossSpoints.clear();
    repaint();
  }

  Point lastPt;
  DecimalFormat formatPos = new DecimalFormat("#0.###");

  Point getValidPoint(Point mousePoint) {
    Point chargePoint = null;
    if (mouseOnPanel && mousePoint.x >= leftMin && mousePoint.y >= topMin) {
      int xPos = (mousePoint.x - leftMin) / unitSide;
      int yPos = (mousePoint.y - topMin) / unitSide;
      xPos++;
      yPos++;
      if (xPos < xCells && yPos < yCells) {
        chargePoint = new Point(xPos, yPos);
      }
    }
    return chargePoint;
  }

  void cancellSelection() {
    resetMarkArea();
    okButon.setEnabled(false);
  }

  void saveSelection() {
   okButon.setEnabled(false);
  }
  void exitView() {
    processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
//    this.dispose();
  }

   Point2d[][] getLengthProfile() {
     int points = crossSpoints.size();
     Point2d[][] retVal = new Point2d[points][];
     Point thePoint;
     for (int p = 0; p < points; p++) {
       thePoint = (Point)crossSpoints.get(p);
       retVal[p] = tempStat.tempAlongLengthAt(timeForCrossPoint, thePoint.x,
                                     thePoint.y);
     }
     return retVal;
   }

   void noteTimeChange(ChangeEvent ce) {
     if (ce.getSource() == timeSelector) {
       timeForCrossPoint = maxTime * ((double)timeSelector.getValue() / 100);
       repaint();
     }
     else {
     }
   }
//---------------------------
   class ChartSource implements ChartDataSource {
     public Point2d[] getOneChartData(int traceNum) {
       Point thePoint = (Point)crossSpoints.get(traceNum);
       return tempStat.tempAlongLengthAt(timeForCrossPoint,
                                                xCells - 1 - thePoint.x,
                                                yCells - 1 - thePoint.y);

     }
    public int traceCount(){
      return crossSpoints.size();
    }
    public Color traceColor(int traceNum){
      return pointColor[traceNum];
    }

   }
//-------------------------
   class TimeChangeListener implements ChangeListener {
     public void stateChanged(ChangeEvent ce) {
       noteTimeChange(ce);
     }

     public void addFollowers(Selector sel) {

     }
   }

// --------------------------------------
  final int leftMin = 10;
  final int topMin = 10;
  class FaceDisplay extends JPanel {
    FaceDisplay () {
      super();
      setPreferredSize(new Dimension(300,200));
    }

    public void paint(Graphics g) {
      int atX = 0 ;
      int atY = 0;
      for (int row = 1; row < yCells - 1; row++,
                          atY += unitSide) {
        atX = 0;
        for (int col = 1; col < xCells - 1; col++,
                            atX += unitSide) {
            imageG2.setColor(Color.darkGray);
            imageG2.fillRect(atX, atY, unitSide, unitSide);
        }
      }
      int nPoints = crossSpoints.size();
      Color c;
      Point thePoint;
      for (int i = 0; i < nPoints; i++) {
        thePoint = (Point)crossSpoints.get(i);
        atX = (thePoint.x - 1) * unitSide;
        atY = (thePoint.y - 1) * unitSide;
        imageG2.setColor(pointColor[i]);
        imageG2.fillRect(atX, atY, unitSide, unitSide);
      }
      g.drawImage(buffer, leftMin, topMin, null);
    }
  }

  Color getAmbientColor(int xPos, int yPos) {
    if (markArea[xPos][yPos])
      return markColor;
    return new Color(128, 128, 128);
  }

// --------------------------------------
  class MouseOnGrid extends MouseAdapter implements MouseMotionListener {
    boolean mousePressed = false;
    public void mouseMoved(MouseEvent me) {
    }

    public void mouseDragged(MouseEvent me) {
    }

    public void mousePressed(MouseEvent me) {
      int modifier = me.getModifiers();
      if ((modifier & me.BUTTON1_MASK) > 0) {
        addCrossPoint(me.getPoint());
      }
      else if ((modifier & me.BUTTON3_MASK) > 0) {
        removeCrossPoint(me.getPoint());
      }
    }

    public void mouseEntered(MouseEvent me) {
       mouseOnPanel = true;
    }

    public void mouseExited(MouseEvent me) {
       mouseOnPanel = false;
    }
  }

  void errMessage(String msg) {
    JOptionPane.showMessageDialog(null, msg, "LengthTempProfile",
                JOptionPane.ERROR_MESSAGE);
  }

  void debug(String msg) {
    System.out.println("LengthTempProfile: " + msg);
  }
}