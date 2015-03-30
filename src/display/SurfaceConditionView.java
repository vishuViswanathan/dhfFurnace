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
import basic.*;


/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2005
 * Company:
 * @author
 * @version 1.0
 */

public class SurfaceConditionView extends JFrame {
  ChargeSurface surf;
  double width, height, unitCell, halfUnitCell;

  int xCells, yCells;
  int unitSide;

  boolean markArea[][];
  Color markColor = Color.red;

  FaceDisplay face = new FaceDisplay();

  JComboBox ambCombo;
  Button areaSelectedButton, okButon, cancellButton, exitButton;
  Button selectAllButton, clearAmbientButton;
  JTextField tbCursor = new JTextField(8);
  JTextField tbAmbAtCursor = new JTextField("Ambient", 15);

  BufferedImage buffer;
  Graphics2D imageG2;

/**
 * width and heights are the size of the surface to be displayed
 * ambs is a combo box with amient choices
 */
  public SurfaceConditionView(String name, int w, int h) {
    super(name);
    setTitle(name);
 //   super(name, true, true, true, true);
    xCells = w;
    yCells = h;

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
    JPanel cursPanel = new JPanel();
    tbCursor.setEnabled(false);
    tbAmbAtCursor.setEnabled(false);
    cursPanel.add(tbCursor);
    cursPanel.add(tbAmbAtCursor);
    getContentPane().add(cursPanel, BorderLayout.NORTH);

    final int maxWidth = 600;
    final int maxHeight = 350;
    int unitW;
    int unitH;
    unitW = maxWidth / (xCells - 1);
    unitH = maxHeight / (yCells - 1);
    unitSide = Math.min(unitW, unitH);

    markArea = new boolean[xCells][yCells];
    resetMarkArea();
    for (int x = 0; x < xCells; x++)
      for (int y = 0; y < yCells; y++)
        markArea[x][y] = false;

    unitCell = width / (xCells - 1);
    halfUnitCell = unitCell / 2;

    face = new FaceDisplay();
    MouseOnGrid mouseOnGrid = new MouseOnGrid();
    face.addMouseListener(mouseOnGrid);
    face.addMouseMotionListener(mouseOnGrid);

    getContentPane().add(face, BorderLayout.CENTER);

    buffer =
          new BufferedImage((xCells - 1) * unitSide,
                    (yCells - 1) * unitSide,
                    BufferedImage.TYPE_INT_RGB);
    imageG2 = buffer.createGraphics();

    JPanel buttonPanel = new JPanel(new GridLayout(4, 2));
    clearAmbientButton = new Button("Clear Surface Conditions");
    clearAmbientButton.addActionListener(
       new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            clearSurfaceConditions();
          }
        });

    selectAllButton = new Button("Select Whole Face");
    selectAllButton.addActionListener(
       new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            selectAll();
          }
        });

    areaSelectedButton = new Button("Area Selected");
    areaSelectedButton.addActionListener(
       new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            confirmAreaSelection();
          }
        });
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
    buttonPanel.add(selectAllButton);
    buttonPanel.add(areaSelectedButton);
    buttonPanel.add(clearAmbientButton);
    areaSelectedButton.setEnabled(false);
//    buttonPanel.add(ambCombo);
    okButon.setEnabled(false);
    buttonPanel.add(cancellButton);
    buttonPanel.add(okButon);
    buttonPanel.add(exitButton);
    getContentPane().add(buttonPanel, BorderLayout.SOUTH);
 //   ambCombo.setEnabled(false);
    pack();
    return true;
  }

  int startX = 0;
  int startY = 0;
  int endX = 0;
  int endY = 0;

  void setMarkStart(int x, int y) {
      startX = x;
      startY = y;
  }

  void setMarkEnd(int x, int y) {
      endX = x;
      endY = y;
  }
  void resetMarkArea() {
    for (int x = 0; x < xCells; x++)
      for (int y = 0; y < yCells; y++)
        markArea[x][y] = false;
//    startX = 0;
//    startY = 0;
  }

  void markAnArea(int x1, int y1, int x2, int y2) {
    resetMarkArea();
    for (int x = x1; x <= x2; x++)
      for (int y = y1; y <= y2; y++)
        markArea[x][y] = true;
    setMarkStart(x1, y1);
    setMarkEnd(x2, y2);
    areaSelectedButton.setEnabled(true);
  }

  void setMarkArea(int x, int y) {
    if (startX > 0 && startY > 0) {
      int xMin = Math.min(x, startX);
      int yMin = Math.min(y, startY);
      int xMax = Math.max(x, startX);
      int yMax = Math.max(y, startY);
      markAnArea(xMin, yMin, xMax, yMax);
    }
  }

  Point lastPt;
  DecimalFormat formatPos = new DecimalFormat("#0.###");
//  DecimalFormat formatTemp = new DecimalFormat("####0.0");

  void showValueUnmark(Point pt){
    showValue(pt, false, true);
  }
  void showValueMark(Point pt) {
    showValue(pt, true, false);
  }

  void showValue(Point pt) {
    showValue(pt, false, false);
  }


  void showValue(Point pt, boolean markIt, boolean unMarkIt) {
    lastPt = pt;
    boolean done = false;
    if (mouseOnPanel && pt.x >= leftMin && pt.y >= topMin) {
      int xPos = (pt.x - leftMin) / unitSide;
      int yPos = (pt.y - topMin) / unitSide;
      xPos++;
      yPos++;
      if (xPos < xCells && yPos < yCells) {
//        tbCursor.setText(formatPos.format(surf.getXLocation(xPos)) + ", "
//                        + formatPos.format(surf.getYLocation(yPos)));
//        AmbientCycle amb = surf.getAmbientAt(xPos, yPos);
//        if (amb != null)
//          tbAmbAtCursor.setText(amb.getName());
//        else
//          tbAmbAtCursor.setText("???");
        done = true;
        if (markIt){
          if (startX < 1 && startY < 1) {
            setMarkStart(xPos, yPos);
          }
          setMarkArea(xPos, yPos);
          repaint();
        }
        if (unMarkIt){
          markArea[xPos][yPos] = false;
          repaint();
        }
      }
    }
    if (!done) {
      tbCursor.setText("");
      tbAmbAtCursor.setText("");
    }
  }

  void selectAll() {
    setMarkStart(1, 1);
    setMarkArea(xCells - 1, yCells - 1);
    repaint();
//    ambCombo.setEnabled(true);
    okButon.setEnabled(true);
    areaSelectedButton.setEnabled(false);
  }

  void confirmAreaSelection() {
    if (startX > 0 && startY > 0) {
      okButon.setEnabled(true);
      areaSelectedButton.setEnabled(false);
    }
//    ambCombo.setEnabled(true);
  }

  void cancellSelection() {
    resetMarkArea();
    setMarkStart(0, 0);
    okButon.setEnabled(false);
    areaSelectedButton.setEnabled(false);
    repaint();
  }

  void exitView() {
    processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
//    this.dispose();
  }

  void clearSurfaceConditions() {
//    surf.clearAbients();
    cancellSelection();
  }

  void saveSelection() {
//    if (startX > 0) {
//      if (!surf.addSurfCondition((AmbientCycle)ambCombo.getSelectedItem(),
//                          startX, startY, endX, endY)) {
//        errMessage(surf.getLastError());
//      }
//      resetMarkArea();
//      repaint();
//    }
    okButon.setEnabled(false);
//    ambCombo.setEditable(false);
  }

  final int leftMin = 10;
  final int topMin = 10;

  class FaceDisplay extends JPanel {
    public void paint(Graphics g) {
      int atX = 0 ;
      int atY = 0;
      for (int row = 1; row < yCells; row++,
                          atY += unitSide) {
        atX = 0;
        for (int col = 1; col < xCells; col++,
                            atX += unitSide) {
            imageG2.setColor(getAmbientColor(col, row));
            imageG2.fillRect(atX, atY, unitSide, unitSide);
        }
      }
      g.drawImage(buffer, leftMin, topMin, null);
    }
  }

  Color getAmbientColor(int xPos, int yPos) {
    if (markArea[xPos][yPos])
      return markColor;
//    AmbientCycle amb = surf.getAmbientAt(xPos, yPos);
//    int iNum  = ambCombo.getItemCount();
//    int itemPos = 0;
//    for (int n= 0; n < iNum; n++) {
//      if (amb == ambCombo.getItemAt(n)) {
//        itemPos = n;
//        break;
//      }
//    }
//    int cN = itemPos * 255 / iNum;
    return new Color(128, 128, 128);
  }

  class MouseOnGrid extends MouseAdapter implements MouseMotionListener {
    boolean mousePressed = false;
    public void mouseMoved(MouseEvent me) {
      showValue(me.getPoint());
    }

    public void mouseDragged(MouseEvent me) {
      int modifier = me.getModifiers();
      if ((modifier & me.BUTTON1_MASK) > 0)
        showValueMark(me.getPoint());
      if ((modifier & me.BUTTON3_MASK) > 0)
        showValueUnmark(me.getPoint());
    }

    public void mousePressed(MouseEvent me) {
      int modifier = me.getModifiers();
      if ((modifier & me.BUTTON1_MASK) > 0) {
        setMarkStart(0, 0); // reset it
        showValueMark(me.getPoint());
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
    JOptionPane.showMessageDialog(null, msg, "SurfaceConditionView",
                JOptionPane.ERROR_MESSAGE);
  }

  void debug(String msg) {
    System.out.println("SurfaceConditionView: " + msg);
  }

}

