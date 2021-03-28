package display;
import basic.*;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.NumberTextField;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.*;

public class TextFrame extends JInternalFrame implements ChangeListener {
  public static final int XLAYER = 0;
  public static final int YLAYER = 1;
  public static final int ZLAYER = 2;
  TemperatureStats stats;
  ThreeDArray theObject;
  int orientation = -1;
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

  double topMean, botMean, lSideMean, totMean, minimumT;
  double s152Top, s152Bot, tauTop, tauBot;
  NumberTextField ntTopMean, ntBotMean, ntLSideMean, ntTotMean, ntMinimumT;
  NumberTextField ntS152Top, ntS152Bot, ntTauTop, ntTauBot;
    MultiPairColPanel statistcsPan;

    private void jbInit(int orientation) throws Exception {
      this.orientation = orientation;
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
      JPanel jp = new JPanel();
      if (orientation == XLAYER) {
          ntTopMean = new NumberTextField(null, topMean, 6, false, 0, 2000, "#,##0.00", "Top Mean Temp (C)");
          ntLSideMean = new NumberTextField(null, lSideMean, 6, false, 0, 2000, "#,##0.00", "Side Mean Temp (C)");
          ntBotMean = new NumberTextField(null, botMean, 6, false, 0, 2000, "#,##0.00", "Bottom Mean Temp (C)");
          ntMinimumT = new NumberTextField(null, minimumT, 6, false, 0, 2000, "#,##0.00", "Minimum Temp (C)");
          ntTotMean = new NumberTextField(null, totMean, 6, false, 0, 2000, "#,##0.00", "Total Mean Temp (C)");
          ntS152Top = new NumberTextField(null, s152Top, 6, false, 0, 2000, "#,##0.00", "s152Top");
          ntS152Bot = new NumberTextField(null, s152Bot, 6, false, 0, 2000, "#,##0.00", "s152Bot");
          ntTauTop = new NumberTextField(null, tauTop, 6, false, 0, 2000, "#,##0.00", "tauTop");
          ntTauBot = new NumberTextField(null, tauBot, 6, false, 0, 2000, "#,##0.00", "tauBot)");
          statistcsPan = new MultiPairColPanel("Statistics");
          statistcsPan.addItemPair(ntTopMean);
          statistcsPan.addItemPair(ntBotMean);
          statistcsPan.addItemPair(ntLSideMean);
          statistcsPan.addItemPair(ntTotMean);
          statistcsPan.addItemPair(ntMinimumT);
          statistcsPan.addItemPair(ntS152Top);
          statistcsPan.addItemPair(ntS152Bot);
          statistcsPan.addItemPair(ntTauTop);
          statistcsPan.addItemPair(ntTauBot);
          jp.add(statistcsPan);
      }
      JTable table = new JTable(dataModel);
      JScrollPane scrollPane = new JScrollPane(table);
      jp.add(scrollPane);
      this.getContentPane().add(jp);
  }

  public void setLayer(int layer) {
    dispLayer = layer;
    dataModel.setLayer(layer);
    setTitle(name + " at " + layer);
    updateStatistics();
    repaint();
  }

    void updateStatistics() {
        String str = "";
        if (orientation == XLAYER)  {
            setTitle(name + " at " + dispLayer + " at time " + time + "h");
            double topSum = 0;
            double botSum = 0;
            double val;
            for (int col = 1; col < colMax; col++) {
                val = (float)stats.getTemperatureDataAt(time, dispLayer, colMax - col, rowMax - 1);
                topSum += val;
                str += (", " + val);
                val = (float)stats.getTemperatureDataAt(time, dispLayer, colMax - col, 1);
                botSum += val;
            }
            topMean = topSum / (colMax - 1);
            ntTopMean.setData(topMean);
            botMean = botSum / (colMax - 1);
            ntBotMean.setData(botMean);

            str = "";
            double sideSum = 0;
            for (int row = 1; row < rowMax; row++) {
                val = (float)stats.getTemperatureDataAt(time, dispLayer, 1, rowMax - row);
                sideSum += val;
                str += (", " + val);
            }
            lSideMean = sideSum / (rowMax -1);
            ntLSideMean.setData(lSideMean);

            double allSum = 0;
            minimumT = 10000;
            for (int row = 1; row < rowMax; row++) {
                for (int col = 1; col < colMax; col++) {
                    val = (float) stats.getTemperatureDataAt(time, dispLayer, colMax - col, rowMax - row);
                    if (val < minimumT)
                        minimumT = val;
                    allSum += val;
                }
            }
            totMean = allSum / ((rowMax -1) * (colMax - 1));
            ntTotMean.setData(totMean);
            ntMinimumT.setData(minimumT);

            s152Top = (topMean - minimumT) / (topMean - totMean);
            s152Bot = (botMean - minimumT) / (botMean - totMean);
            ntS152Top.setData(s152Top);
            ntS152Bot.setData(s152Bot);

            double tgTop = (float) stats.getCellTemperatureDataAt(time, dispLayer, colMax - 1, rowMax);
            double tgBot= (float) stats.getCellTemperatureDataAt(time, dispLayer, colMax - 1, 0);
            tauTop = (tgTop - topMean) / (tgTop - totMean);
            tauBot = (tgBot - botMean) / (tgBot - totMean);
            ntTauTop.setData(tauTop);
            ntTauBot.setData(tauBot);
        }
    }


    public void stateChanged(ChangeEvent e) {
        time = tServer.getTime() * stats.lastTimePoint();
        updateStatistics();
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

