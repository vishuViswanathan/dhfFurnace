package display;
import basic.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import javax.vecmath.*;

public class TemperatureList extends JFrame
    implements DataChangeListener{ // extends JInternalFrame {
  TemperatureStats stats;
  String name;
  JTextArea textArea;
  JMenuBar menuBar1 = new JMenuBar();
  JMenu menuFile = new JMenu();
  JMenuItem menuFileExit = new JMenuItem("Exit");
  JMenuItem menuFileSave = new JMenuItem("Save Results to CSV file");
//  GraphPanel profilePanel;
  ChartPanel profilePanel;
  double maxTime = 3.0; // to be changed
  double maxTemp = 1400;
  PointInCharge[] pointsInCharge;
//  Color[] colorTable = {Color.black, Color.blue, Color.cyan, Color.darkGray,
//      Color.gray, Color.green, Color.lightGray, Color.magenta, Color.orange, Color.red};
  Graphics lastG = null;
  int[] lastX;
  int[] lastY;
  int pointCnt;

  public TemperatureList(String name, TemperatureStats srcStats, double endTime) {
    super(name); // , true, true, true, true);
    stats = srcStats;
    maxTime = endTime;
    try  {
      jbInit();
      setSize(400, 300);
      pack();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    srcStats.addDataChangeListener(this);
    pointCnt = stats.getPointCount();
    pointsInCharge = srcStats.getPointList();
    lastX = new int[pointCnt];
    lastY = new int[pointCnt];
  }

  private void jbInit() throws Exception {
    menuFile.setText("File");
    menuFile.add(menuFileSave);
    menuFile.add(menuFileExit);
    menuBar1.add(menuFile);
    setJMenuBar(menuBar1);
    textArea = new JTextArea(10, 100);
    JScrollPane scrollPane = new JScrollPane(textArea);
    Container container = getContentPane();
    container.setLayout(new BorderLayout());
    container.add(scrollPane, BorderLayout.NORTH);
    textArea.setText(stats.getColumnHeader() + '\n');
    int count = stats.actualLen;
    for (int n = 0; n < count; n++)
      textArea.append(stats.getOneSet(n) + '\n');
//    profilePanel = new GraphPanel();
    profilePanel = new ChartPanel(new ChartSource());
    profilePanel.setXRange(0, 3, 0.5, "#.##");
    profilePanel.setYRange(0, 1400, 200, "####");
    container.add(profilePanel, BorderLayout.CENTER);
  }

// for the interface DataChangeListener interface
  public void dataChanged() {

  }

  public void dataAdded(int n) {
    textArea.append(stats.getOneSet(n) + '\n');
    updateTrends(lastG, stats.getOneSet(n, null));
  }

  Rectangle graphRect;

  void drawTempProfile(Graphics g, boolean refresh) {
    int margin = 5;
    Dimension dim = profilePanel.getSize();
    g.setColor(Color.black);
    int right = dim.width - margin;
    int bottom = dim.height - margin;
    g.drawRect(margin, margin, right, bottom);
    drawAxes(g, margin, margin, right, bottom);
    drawTrends(g);
    lastG = g;
  }

  int absOriginX, absOriginY;
  double timeScale, tempScale;;

  void drawAxes(Graphics g, int orgX, int orgY, int maxX, int maxY) {

    int borderX = 5 + orgX;
    int borderY = 5 + orgY;
    absOriginX = orgX + 40;
    absOriginY = maxY - 20;
//    int originX = 40;
//    int originY = 20;
    int lTimeLine = maxX - absOriginX - borderX;
    timeScale = (double)(lTimeLine) / maxTime;
    int lTempLine = absOriginY - borderY;
    tempScale = (double)(lTempLine) / maxTemp;
    g.setColor(Color.blue);
    // time axis
    g.drawLine(absOriginX, absOriginY, absOriginX + lTimeLine, absOriginY);
    // Temperature axis
    g.drawLine(absOriginX, absOriginY, absOriginX, absOriginY - lTempLine);
  }

  void drawTrends(Graphics g) {
    for (int n = 0; n < pointCnt; n++)
      drawOneTrend(g, n);
  }

  void updateTrends(Graphics g, double[] data) {
    if (lastG != null) {
      for (int n = 0; n < pointCnt; n++)
        updateOneTrend(g, n, data);
    }
    repaint();
  }

  void updateOneTrend(Graphics g, int tr, double[] data) {
    int newX = (int)(data[0] * timeScale) + absOriginX;
    int newY = absOriginY - (int)(data[tr] * tempScale);
    g.setColor(pointsInCharge[tr].color);
    g.drawLine(lastX[tr], lastY[tr], newX, newY);
    lastX[tr] = newX;
    lastY[tr] = newY;
  }

  void drawOneTrend(Graphics g, int tr) {
    double[][] data = stats.getOneTrend(tr);
    if (data != null) {
      int dataLen = data[0].length;
      int[] xList = new int[dataLen];
      int[] yList = new int[dataLen];
      for (int n = 0; n < dataLen; n++) {
        xList[n] = (int)(data[0][n] * timeScale) + absOriginX;
        yList[n] = absOriginY - (int)(data[1][n] * tempScale);
      }
      g.setColor(pointsInCharge[tr].color);
      g.drawPolyline(xList, yList, dataLen);
      // note down the last point for update
      if (dataLen > 0) {
        lastX[tr] = xList[dataLen - 1];
        lastY[tr] = yList[dataLen - 1];
      }
    }
  }

  void errMessage(String msg) {
    System.err.println("TemperatureList: ERROR: " + msg);
  }

  void debug(String msg) {
    System.out.println("TemperatureList: " + msg);
  }

  Point2d[] getOneTrend(int n) {
    return stats.tempHistoryAt(n);
//    double[][] data = stats.getOneTrend(n);
//    Point2d[] retVal = new Point2d[data[0].length];
//    for (int i = 0; i < retVal.length; i++)
//      retVal[i] = new Point2d(data[0][i], data[1][i]);
//    return retVal;
  }

  class ChartSource implements ChartDataSource {
    public Point2d[] getOneChartData(int traceNum) {
      return getOneTrend(traceNum);
    }

    public int traceCount() {
      return pointCnt;
    }

    public Color traceColor(int traceNum) {
      return Color.BLACK; //pointsInCharge[traceNum].color;
    }
  }

  class GraphPanel extends JPanel {

    GraphPanel() {
      setBackground(SystemColor.white);
    }

    public void paint(Graphics g) {
      super.paint(g);
      drawTempProfile(g, false);
//      drawLineCursor(g);
    }
  }

}
