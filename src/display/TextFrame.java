package display;
import basic.*;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.*;
import java.awt.*;

public class TextFrame extends JInternalFrame implements ChangeListener {
  public static final int XLAYER = 0;
  public static final int YLAYER = 1;
  public static final int ZLAYER = 2;
  TemperatureStats stats;
  ThreeDArray theObject;
  JTable theTable;
  MyTableModel dataModel;
  boolean hideBorderData;
  int dispLayer;
  String name;
  int rowMax, colMax;
    double time = 100000; // set high to get the get current max time as default
    TimeServer tServer;


  public TextFrame(String name, TemperatureStats theStats,
              int layerOrient) {
    this(name, theStats, layerOrient, false, null);
  }

  public TextFrame(String name, TemperatureStats theStats,
                      int layerOrient,
                      boolean hideBorderData, TimeServer tServer) {
    super(name, true, true, true, true);
    this.name = name;
    this.hideBorderData = hideBorderData;
    stats = theStats;
    this.theObject = theStats.charge;
      this.tServer = tServer;
    try  {
      jbInit(layerOrient);
      setSize(400, 300);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

//  public TextFrame(String name, ThreeDArray theObject,
//              int layerOrient) {
//    this(name, theObject, layerOrient, false);
//  }
//
//  public TextFrame(String name, ThreeDArray theObject,
//                      int layerOrient,
//                      boolean hideBorderData) {
//    super(name, true, true, true, true);
//    this.name = name;
//    this.hideBorderData = hideBorderData;
//    this.theObject = theObject;
//    try  {
//      jbInit(layerOrient);
//      setSize(400, 300);
//    }
//    catch (Exception e) {
//      e.printStackTrace();
//    }
//  }


  private void jbInit(int orientation) throws Exception {
    dataModel = new MyTableModel(orientation);
    switch (orientation) {
      case XLAYER:
        rowMax = theObject.getZsize() - 1;
        colMax = theObject.getYsize() - 1;
        break;
      case YLAYER:
        rowMax = theObject.getZsize() - 1;
        colMax = theObject.getXsize() - 1;
        break;
      case ZLAYER:
        rowMax = theObject.getYsize() - 1;
        colMax = theObject.getXsize() - 1;
        break;
    }
    if (hideBorderData) {
        rowMax = rowMax - 2;
        colMax = colMax - 2;
    }
    JTable table = new JTable(dataModel);
    JScrollPane scrollPane = new JScrollPane(table);
    this.getContentPane().add(scrollPane);
  }

  public void setLayer(int layer) {
    dispLayer = layer;
    dataModel.setLayer(layer);
    setTitle(name + " at " + layer);
    repaint();
  }

    public void stateChanged(ChangeEvent e) {
      time = tServer.getTime() * stats.lastTimePoint();
       repaint();

    }


  class MyTableModel extends AbstractTableModel {
    int layerOrient;  // XLAYER, YLAYER ...
    int layer;
    public MyTableModel(int orientation) {
      this.layer = 0;
      layerOrient = orientation;
    }

    void setLayer(int layer) {
      if (hideBorderData) layer = layer + 1;
      this.layer = layer;
    }

    public int getColumnCount() {
      int count = 0;
      switch (layerOrient) {
          case XLAYER:
            count = theObject.getYsize();
            break;
          case YLAYER:
            count = theObject.getXsize();
            break;
          case ZLAYER:
            count = theObject.getXsize();
            break;
      }
      if (hideBorderData) count = count -2;
      return count;
    }

    public String getColumnName(int col) {
      return "Col #" + col;
    }

    public int getRowCount() {
      int count = 0;
      switch (layerOrient) {
          case XLAYER:
            count = theObject.getZsize() ;
            break;
          case YLAYER:
            count = theObject.getZsize();
            break;
          case ZLAYER:
            count = theObject.getYsize();
            break;
      }
      if (hideBorderData) count = count -2;
      return count;
    }


    public Object getValueAt(int row, int col) { // was(int xPos, int yPos) {
      double value = 0;
      switch (layerOrient) {
          case XLAYER:
            value = (float)stats.getTemperatureDataAt(time, layer, colMax - col, rowMax - row);
            break;
          case YLAYER:
              if (hideBorderData) {
                 col = col + 1;
              }
            value = (float)stats.getTemperatureDataAt(time, col, layer, rowMax - row);
            break;
          case ZLAYER:
              if (hideBorderData) {
                  col = col + 1;
              }
            value = (float)stats.getTemperatureDataAt(time, col, rowMax - row, layer);
            break;
        }
      if (value == -1)
        return new String("");
     else
        return new ValueObject(value);
    }
  }

//  public Object getValueAt(int row, int col) { // was(int xPos, int yPos) {
//    double value = 0;
//    switch (layerOrient) {
//        case XLAYER:
//          value = (float)theObject.getDataAt(layer, colMax - col, rowMax - row);
//          break;
//        case YLAYER:
//          value = (float)theObject.getDataAt(col, layer, rowMax - row);
//          break;
//        case ZLAYER:
//          value = (float)theObject.getDataAt(col, rowMax - row, layer);
//          break;
//      }
//    return new ValueObject(value);
//  }
//}

  // a class make int strings of double data
  class ValueObject {
    double value;
    ValueObject(double val) {
      value = val;
    }

    public String toString() {
      return "" + (int)value;
    }
  }

  void errMessage(String msg) {
    System.err.println("TextFrame: ERROR: " + msg);
  }

  void debug(String msg) {
    System.out.println("TextFrame: " + msg);
  }
}

