package display;
import basic.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;

public class ThreeDDisplay extends JFrame {//  implements Runnable {
  boolean noCalculation = false;
  ThreeDCharge tda;
  double endTime;
  Container contPane;
  BorderLayout borderLayout1 = new BorderLayout();
  JMenuBar menuBar1 = new JMenuBar();
  JMenu menuFile = new JMenu();
  JMenuItem menuFileExit = new JMenuItem();
//  JMenuItem menuFileSave = new JMenuItem("Save Results as Text");
  JMenuItem menuDFileSave = new JMenuItem("Save Results");
//  JMenuItem menuFileRead = new JMenuItem("Read Results Data");
  JMenu menuHelp = new JMenu();
  JMenuItem menuHelpAbout = new JMenuItem();
  JMenuItem menuStart = new JMenuItem("Start");
  JMenuItem menuStop = new JMenuItem("Stop");
  JMenuItem menuContinue = new JMenuItem("Continue");
  JMenu menuAction = new JMenu("Action");
  JButton menuStatus = new JButton("Statistics");
  JButton show3D = new JButton("3D View");
  JButton menuStatList = new JButton("Temperature List");
  JButton menuLenProfile = new JButton("Length Profile");
  JLabel statusBar = new JLabel();
  TemperatureColorFrame baseFrame, yzFrame, xzFrame, xyFrame;
  TextFrame yzTextFrame, xzTextFrame, xyTextFrame;
  Vector<StartStopListener> startStopListener = new Vector<StartStopListener>();
  Vector displayOnOffListener = new Vector();
  final int startIt = 1;
  final int stopIt = 2;
  final int continueIt = 3;
  SelectionView selFrame = null;
  ThreeDBillet threeDView = null;
  TemperatureStats statsData;
  LengthTempProfile lProfile;

  public ThreeDDisplay(String title, ThreeDCharge ch, TemperatureStats stats,
                       double endTime) {
    this(title, ch, stats, false);
    this.endTime = endTime;
  }

  public ThreeDDisplay(String title, ThreeDCharge ch, TemperatureStats stats,
                       boolean onlyDisplay) {
    super(title);
    tda = ch;
    statsData = stats;
    try  {
      jbInit(onlyDisplay);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    if (onlyDisplay) {
      menuLenProfile.setEnabled(true);
      menuStatus.setEnabled(false);
    }
  }

  private void jbInit(boolean onlyDisplay) throws Exception {
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        if (!noCalculation)
          menuStop.doClick();
        if (statDisplay != null)
          statDisplay.dispose();
//        if (statList != null)
//          statList.dispose();
      }
    });
    noCalculation = onlyDisplay;
    this.getContentPane().setLayout(borderLayout1);
    this.setSize(new Dimension(800, 600));
    if (!onlyDisplay) {
//      menuFileSave.addActionListener(new ActionListener() {
//        public void actionPerformed(ActionEvent e) {
//          saveToFile();
//        }
//      });
      menuDFileSave.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          saveToDataFile();
        }
      });
//      menuFileRead.addActionListener(new ActionListener() {
//        public void actionPerformed(ActionEvent e) {
//          readFromFile();
//        }
//      });
    }
    menuFile.setText("File");
    menuFileExit.setText("Exit");
    menuFileExit.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        fileExit_ActionPerformed(e);
      }
    });
    menuStatus.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showStatus();
      }
    });
    menuStatList.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showStatList();
      }
    });
    menuLenProfile.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showLenProfile();
      }
    });

    show3D.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        show3Diew();
      }
    });
    menuHelp.setText("Help");
    menuHelpAbout.setText("About");
    menuHelpAbout.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        helpAbout_ActionPerformed(e);
      }
    });
    statusBar.setText("The status bar");
    if (!onlyDisplay) {
//      menuFile.add(menuFileSave);
      menuFile.add(menuDFileSave);
//      menuFile.add(menuFileRead);
    }
    menuFile.add(menuFileExit);
    menuBar1.add(menuFile);
    if (!onlyDisplay) {
      ActionMenuListener l1 = new ActionMenuListener();
      menuStart.addActionListener(l1);
      menuStop.addActionListener(l1);
      menuContinue.addActionListener(l1);
      menuAction.add(menuStart);
      menuAction.add(menuStop);
      menuAction.add(menuContinue);
      menuBar1.add(menuAction);
    }
    menuBar1.add(menuStatus);
    menuBar1.add(show3D);
    menuStatList.setEnabled(false);
    menuBar1.add(menuStatList);
    menuLenProfile.setEnabled(false);
    menuBar1.add(menuLenProfile);
    menuHelp.add(menuHelpAbout);
    menuBar1.add(menuHelp);

    this.setJMenuBar(menuBar1);
    JDesktopPane deskTop = new JDesktopPane();
    this.getContentPane().add(deskTop);
    this.getContentPane().add(statusBar, BorderLayout.SOUTH);

    selFrame = new SelectionView("Selection View", tda);
    selFrame.initValues();
    selFrame.setSize(400, 300);
    selFrame.setVisible(true);
    selFrame.setLocation(10, 10);

    int x = 20;
    int y = 100;
    yzFrame = new TemperatureColorFrame("Color YZ section", statsData,
            TemperatureColorFrame.XLAYER, true, selFrame, selFrame);
    yzFrame.setLocation(x, y);
    selFrame.addColorListener(yzFrame);
    selFrame.addTimeListener(yzFrame);
//    yzFrame.noteServers(selFrame, selFrame);
    x += 20;
    y += 20;
    yzFrame.addInternalFrameListener(
            new IntFrameWindowListener());
    deskTop.add(yzFrame);
    yzFrame.setVisible(true);
    yzFrame.setIcon(true);


    // YLAYER temperature color frame
    xzFrame = new TemperatureColorFrame("Color XZ section", statsData,
            TemperatureColorFrame.YLAYER, true, selFrame, selFrame);
    xzFrame.setLocation(x, y);
    selFrame.addColorListener(xzFrame);
    selFrame.addTimeListener(xzFrame);
//    xzFrame.noteServers(selFrame, selFrame);
    x += 20;
    y += 20;
    xzFrame.addInternalFrameListener(
            new IntFrameWindowListener());
    deskTop.add(xzFrame);
    xzFrame.setVisible(true);
    // ZLAYER temperature color frame
    xyFrame = new TemperatureColorFrame("Color XY section", statsData,
            TemperatureColorFrame.ZLAYER, true, selFrame, selFrame);
    xyFrame.setLocation(x, y);
    selFrame.addColorListener(xyFrame);
    selFrame.addTimeListener(xyFrame);
//    xyFrame.noteServers(selFrame, selFrame);
    xyFrame.addInternalFrameListener(
            new IntFrameWindowListener());
    deskTop.add(xyFrame);
    xyFrame.setVisible(true);
    xyFrame.setIcon(true);

    x += 20;
    y += 20;
    // text Frame
//    yzTextFrame = new TextFrame("Text YZ section", statsData,
//            TextFrame.XLAYER, true, selFrame);
    yzTextFrame = new TextFrame("Text YZ section", statsData,
            TextFrame.XLAYER, false, selFrame);
    yzTextFrame.setLocation(x, y);
    deskTop.add(yzTextFrame);
    yzTextFrame.setVisible(true);
    yzTextFrame.setIcon(true);
    selFrame.addTimeListener(yzTextFrame);

    x += 50;
    y += 50;
    // text Frame
    xzTextFrame = new TextFrame("Text XZ section", statsData,
            TextFrame.YLAYER, true, selFrame);
    xzTextFrame.setLocation(x, y);
    deskTop.add(xzTextFrame);
    xzTextFrame.setVisible(true);
    xzTextFrame.setIcon(true);
    selFrame.addTimeListener(xzTextFrame);

    x += 50;
    y += 50;
    // text Frame
    xyTextFrame = new TextFrame("Text XY section", statsData,
            TextFrame.ZLAYER, true, selFrame);
    xyTextFrame.setLocation(x, y);
    deskTop.add(xyTextFrame);
    xyTextFrame.setVisible(true);
    xyTextFrame.setIcon(true);
    selFrame.addTimeListener(xyTextFrame);

    threeDView = new ThreeDBillet("Charge in 3-D", tda, statsData, selFrame, selFrame);
    selFrame.addColorListener(threeDView);
    selFrame.addTimeListener(threeDView);
    threeDView.addWindowListener(new StatusWindowListener());
    threeDView.setSize(600, 400);

    selFrame.addSelectionListener(
              new LayerSelListener(SelectionView.YZPLANE),
                              SelectionView.YZPLANE);
    selFrame.addSelectionListener(
              new LayerSelListener(SelectionView.XZPLANE),
                              SelectionView.XZPLANE);
    selFrame.addSelectionListener(
              new LayerSelListener(SelectionView.XYPLANE),
                              SelectionView.XYPLANE);

    deskTop.add(selFrame);

//    xyFrame.addColorChangeListener(threeDView);
    setZlayer();
//    xzFrame.addColorChangeListener(threeDView);
    setYlayer();
//    yzFrame.addColorChangeListener(threeDView);
    setXlayer();

//    deskTop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
    deskTop.repaint();
  }

  void fileExit_ActionPerformed(ActionEvent e) {
    processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
  }

  void helpAbout_ActionPerformed(ActionEvent e) {
    JOptionPane.showMessageDialog(this, new MainFrame_AboutBoxPanel1(),
        "About", JOptionPane.PLAIN_MESSAGE);
  }

  void saveToFile() {
    String fName = getFileName(false, "txt", "Data Text Files");
    if (fName == null)
      return;
    tda.writeArrayToFile(fName, false);
  }

  void saveToDataFile() {
    String fName = getFileName(false, "trd", "Temperature Profile Files");
    if (fName == null)
      return;
    statsData.saveHistory(fName);
  }

  static public TemperatureStats readFromFile(Component parent) {
    GetFileName getFile = new GetFileName(true, "trd", "Temperature Profile Files");
    String fName = getFile.getIt(parent);
    if (fName == null)
      return null;
    TemperatureStats newStat = new TemperatureStats(fName);
    return newStat;
  }

File lastDirectory = null;

  String getFileName (boolean toRead, String ext, String desription) {
    GetFileName getFile = new GetFileName(toRead, ext, desription);
    getFile.setDirectory(lastDirectory);
    String fName = getFile.getIt(this);
    lastDirectory = getFile.getDirectory();
    return fName;
  }


  ThreeDDisplay theMainWindow = this;
  StatisticsDisplay statDisplay = null;

  void showStatus() {
    if (statDisplay == null){
      statDisplay = new StatisticsDisplay(statsData);
      statDisplay.setSize(500, 300);
      statDisplay.addWindowListener(new StatusWindowListener());
    }
    statDisplay.update();
    statDisplay.setLocationRelativeTo(this);
    statDisplay.setVisible(true);
//    menuStatus.setEnabled(false);
  }

  TemperatureList statList = null;
  void showStatList() {
    if (statList == null){
      statList = new TemperatureList("Profile List", statsData, endTime);
      statList.setSize(500, 300);
//      statList.addWindowListener(new StatusWindowListener());
    }
    statList.setLocationRelativeTo(this);
    statList.setVisible(true);
//    menuStatList.setEnabled(false);
  }

  void showLenProfile() {
    lProfile = new LengthTempProfile("Temp Profile along charge Length", statsData);
    lProfile.setSize(400, 600);
    lProfile.addWindowListener(new StatusWindowListener());
    theMainWindow.setEnabled(false);
    lProfile.setVisible(true);
  }

  void show3Diew() {
   if (threeDView == null) {
    threeDView = new ThreeDBillet("Charge in 3-D", tda, statsData, selFrame, selFrame);
    threeDView.addWindowListener(new StatusWindowListener());
    threeDView.setSize(600, 400);
//    threeDView.setVisible(true);
   }
   threeDView.setLocationRelativeTo(this);
   threeDView.setVisible(true);
//   show3D.setEnabled(false);
  }

  void setXlayer() {
      int layer = selFrame.getXLayerPos();
    yzFrame.setLayer(layer);
    yzTextFrame.setLayer(layer);
  }

  void setYlayer() {
      int layer = selFrame.getYLayerPos();
    xzFrame.setLayer(layer);
      xzTextFrame.setLayer(layer);  

  }

  void setZlayer() {
      int layer = selFrame.getZLayerPos();
    xyFrame.setLayer(layer);
      xyTextFrame.setLayer(layer);

  }

  class StatusWindowListener extends WindowAdapter {
    public void windowClosing(WindowEvent e) {
      Object src = e.getSource();
      if (src == statDisplay)
        menuStatus.setEnabled(true);
      if (src == threeDView)
        show3D.setEnabled(true);
      if (src == lProfile) {
        theMainWindow.setEnabled(true);
      }
    }
  }

  public void updateNow() {
    yzFrame.repaint();
    xzFrame.repaint();
    xyFrame.repaint();
    xyTextFrame.repaint();
    yzTextFrame.repaint();
    xzTextFrame.repaint();
//    selFrame.update();
    if (threeDView != null)
      threeDView.update();
    if (statDisplay != null)
      statDisplay.update();
//      System.gc();
  }

  public void resultsReady() {
    menuLenProfile.setEnabled(true);
  }

  public void addStartStopListener(StartStopListener l) {
    startStopListener.add(l);
  }

  public void removeStartStopListener(StartStopListener l) {
    startStopListener.remove(l);
  }

  protected void notifyStartStopListeners(int action) {
    StartStopListener l;

    for (int n = 0; n < startStopListener.size(); n++) {
      l = (StartStopListener)startStopListener.elementAt(n);
      switch(action) {
        case startIt:
          menuLenProfile.setEnabled(false);
          l.startIt();
          break;
        case stopIt:
          l.stopIt();
          break;
        case continueIt:
          menuLenProfile.setEnabled(false);
          l.continueIt();
          break;
        default:
          break;
      }
    }
  }

  class ActionMenuListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      Object src = ae.getSource();
      if (src == menuStart) {
        notifyStartStopListeners(startIt);
      }
      else if (src == menuStop) {
        notifyStartStopListeners(stopIt);
      }
      else if (src == menuContinue) {
        notifyStartStopListeners(continueIt);
      }
    }
  }

  class LayerSelListener implements ChangeListener {
    int planeOrient;
    LayerSelListener(int planeOrient) {
      super();
      this.planeOrient = planeOrient;
    }

    public void stateChanged(ChangeEvent ce) {
      SelectionView src =
          (SelectionView)ce.getSource();
      float percent;
      int sel;
      switch(planeOrient) {
        case SelectionView.YZPLANE:
          setXlayer();
//          percent = src.getXsel();
//          sel =(int)(percent / 100 * (tda.getXsize() - 1));
//          yzFrame.setLayer(sel);
//          yzTextFrame.setLayer(sel);
          break;
        case SelectionView.XZPLANE:
          setYlayer();
//          percent = src.getYsel();
//          sel =(int)(percent / 100 * (tda.getYsize() - 1));
//          xzFrame.setLayer(sel);
//          xzTextFrame.setLayer(sel);
          break;
        case SelectionView.XYPLANE:
          setZlayer();
//          percent = src.getZsel();
//          sel =(int)(percent / 100 * (tda.getZsize() - 1));
//          xyFrame.setLayer(sel);
//          xyTextFrame.setLayer(sel);
          break;
      }
//      selFrame.update();
      if (threeDView != null)
        threeDView.update();
//      System.gc();
    }
  }

  class IntFrameWindowListener extends
                      InternalFrameAdapter {
    public void internalFrameClosing(InternalFrameEvent ife) {
      Object src = ife.getSource();
      if (src instanceof JInternalFrame) {
        JInternalFrame frame = (JInternalFrame)src;
        frame.dispose();
        frame.setVisible(false);
        frame.dispose();
      }
    }
  }

  void errMessage(String msg) {
    JOptionPane.showMessageDialog(null, msg, "ThreeDDisplay",
                JOptionPane.ERROR_MESSAGE);
  }

  boolean getConfirmation(String msg) {
    return (JOptionPane.showConfirmDialog(null, msg, "ThreeDDisplay",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
  }

  void debug(String msg) {
    System.out.println("ThreeDDisplay: " + msg);
  }

}



