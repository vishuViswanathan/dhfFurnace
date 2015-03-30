package mvmath;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import javax.swing.*;

/**
 * The MixedDisplay object can display the results of
 * some object of a class which either ject implements
 * GraphInfo interface or extends GraphInfoAdapter.
 * It instantiates GraphDisplay object for graph/graphs
 *
 * Depending what info is avilable thru the GraphInfo
 * interface this class can display single/multiple graphs
 * in same/separate graph penels and also display tabular
 * data.
 *
 * The class opens its own frame for the display.
 *
 * A MDFrameClose listener can be registered with the
 * object of this class in case the frame closing event
 * to be informed for any further action by the owner of
 * the object
 *
 */

public class MixedDisplay
    implements ResultDisplay {
  protected GraphInfo gInfo;
  private Vector <MDFrameCloseListener>closeListener = new Vector<MDFrameCloseListener>();
  Frame mainFrame = new Frame();
  BasicCalculData basicData;
  MenuItem separateGraphs;
  MenuItem multipleGraphs;
  CheckboxMenuItem textDisplayChoice;
  FramedPanel graphPanel;
//  TextArea taResults = new TextArea();
  Panel resultPanel = null;
//  JScrollPane resultScroll = null;
  TextArea reactions = new TextArea();
  boolean multipleON;
  boolean textDisplayON = true;
  boolean setupComplete = false;
  boolean independantDisplay = false;
  FrameEventDespatcher frameEventDespatcher =
      new FrameEventDespatcher();

  public static void main(String[] args) {
    System.out.println("starting independant MixedDisplay");
// 		MixedDisplay display = new MixedDisplay();
// 		display.independantDisplay = true;
//		display.showMixedDisplay("Test Display");

  }

  /*
   * A combination of Graphic and Text display of calculated
   * data
   *
   * @param gInfo an object with GraphInfo interface from which
   *        MixedDisplay object can get graph details as well
   *        data. Also the Text display Data
   * @param basicData for getting calclation input data as well as
   *        drawing some base diagram
   */
  public MixedDisplay(String title, GraphInfo gInfo,
                      BasicCalculData basicData) {
    setupInfo(gInfo, basicData);
    showMixedDisplay(title);
  }

  public MixedDisplay() {
  }

// overriding ResultDisplay interface
  public void setupInfo(GraphInfo gInfo,
                        BasicCalculData basicData) {
    this.gInfo = gInfo;
    this.basicData = basicData;
    setupComplete = true;
  }

  public boolean startDisplay(String title) {
    if (gInfo == null) {
      return false;
    }
    showMixedDisplay(title);
    return true;
  }

  public void addMDFrameCloseListener(
      MDFrameCloseListener mdfcl) {
    if (!closeListener.contains(mdfcl)) {
      closeListener.addElement(mdfcl);
    }
  }

  public void removeMDFrameCloseListener(
      MDFrameCloseListener mdfcl) {
    if (closeListener.contains(mdfcl)) {
      closeListener.removeElement(mdfcl);
    }
  }

// -------------

  void showMixedDisplay(String title) {
    mainFrame = new Frame(title);
    mainFrame.setBackground(Color.lightGray);
    mainFrame.addWindowListener(new CloseListener());
    // add menu
    MenuBar menuBar = new MenuBar();
    Menu windowMenu = new Menu("Window");
    separateGraphs = new MenuItem("One Graph per panel");
    multipleGraphs = new MenuItem("All Graph in one panel");
    textDisplayChoice =
        new CheckboxMenuItem("Show List Display", false);
    WindowSetListener wsl = new WindowSetListener();
    separateGraphs.addActionListener(wsl);
    multipleGraphs.addActionListener(wsl);
    textDisplayChoice.addItemListener(wsl);
    windowMenu.add(separateGraphs);
    windowMenu.add(multipleGraphs);
    windowMenu.addSeparator();
    windowMenu.add(textDisplayChoice);
    Menu helpMenu = new Menu("Help");
    menuBar.setHelpMenu(helpMenu);
    menuBar.add(windowMenu);
    mainFrame.setMenuBar(menuBar);
    addMultipleGraphDisplay(mainFrame);
//    mainFrame.add(new Label("The Graphic Frame"), "North");
    addReactions(mainFrame);
    mainFrame.setVisible(true);
  }

  void addTextDisplay(Frame frame) {
    if (textDisplayON) {
      if (resultPanel != null)
        frame.remove(resultPanel);
      JTable table = gInfo.getResultTable();
      JPanel headPanel = new JPanel(new GridLayout(1, 1));
      headPanel.add(table.getTableHeader());
      resultPanel = new Panel(new BorderLayout());
      resultPanel.add(headPanel, BorderLayout.NORTH);
      ScrollPane resultScroll = new ScrollPane();
      resultScroll.add(table);
      resultPanel.add(resultScroll, BorderLayout.CENTER);
      frame.add(resultPanel, BorderLayout.SOUTH);
    }
  }

  void addReactions(Frame frame) {
    DoublePoint[] data = gInfo.getReactions();
    int nsupports = data.length;
    FramedPanel supportsPanel =
        new FramedPanel(new GridBagLayout());
    TextField tfSupport[] = new TextField[nsupports];
    Label laSupport[] = new Label[nsupports];
    GridBagConstraints gbcSp = new GridBagConstraints();
//    gbcSp.insets = lttprt;
    Label headSp = new Label("Supports");
    headSp.setAlignment(Label.CENTER);
    gbcSp.fill = GridBagConstraints.HORIZONTAL;
    gbcSp.gridwidth = 2;
    gbcSp.anchor = GridBagConstraints.CENTER;
    supportsPanel.add(headSp, gbcSp);
    gbcSp.fill = GridBagConstraints.NONE;
    gbcSp.gridwidth = 1;
    gbcSp.anchor = GridBagConstraints.CENTER;
    gbcSp.gridx = 0;
    gbcSp.gridy = 1;
    gbcSp.gridx++;
    supportsPanel.add(new Label("Reaction"), gbcSp);

    gbcSp.gridx = 0;
    gbcSp.gridy++;
    supportsPanel.add(new Label("R#"), gbcSp);
    gbcSp.gridx++;
    supportsPanel.add(new Label("(kg)"), gbcSp);
    gbcSp.gridy++;

    gbcSp.anchor = GridBagConstraints.NORTHWEST;
    DecimalFormat format = new DecimalFormat("###,###.##");
    for (int n = 0; n < nsupports; n++) {
      gbcSp.gridx = 0;
      laSupport[n] = new Label("R" + (n + 1));
      supportsPanel.add(laSupport[n], gbcSp);
      gbcSp.gridx++;
      tfSupport[n] = new TextField(6);
      tfSupport[n].setText(format.format(data[n].y));
      supportsPanel.add(tfSupport[n], gbcSp);
      gbcSp.gridy++;
    }
    frame.add(supportsPanel, "East");
  }

  void addMultipleGraphDisplay(Frame frame) {
    if (graphPanel != null) {
      frame.remove(graphPanel);
    }
    graphPanel = new FramedPanel(new GridLayout(1, 0)); // new FramedPanel(new GridLayout(1, 0),true);
// 		graphPanel.setInsets(10, 10, 10, 10);
    GraphPanel gPanel =
        new GraphPanel(new Dimension(600, 350));
    gPanel.addTrace(gInfo, 0, GraphDisplay.COLORS[0]);
    gPanel.addTrace(gInfo, 1, GraphDisplay.COLORS[1]);
    gPanel.addTrace(gInfo, 2, GraphDisplay.COLORS[2]);
    gPanel.addTrace(gInfo, 3, GraphDisplay.COLORS[3]);
    gPanel.prepareDisplay();
    graphPanel.add(gPanel);
    graphPanel.setSize(300,300);
    frame.add(graphPanel, "Center");
    addTextDisplay(frame);
    frame.pack();
    multipleON = true;
  }

  void addSeparateGraphDisplays(Frame frame) {
    if (graphPanel != null) {
      frame.remove(graphPanel);
    }
    graphPanel = new FramedPanel(new GridLayout(0, 2)); // new FramedPanel(new GridLayout(0, 2), true);
// 		graphPanel.setInsets(10, 10, 10, 10);
    for (int n = 0; n < 4; n++) {
      GraphPanel gPanel =
          new GraphPanel(new Dimension(400, 240));
      gPanel.addTrace(gInfo, n, GraphDisplay.COLORS[n]);
      gPanel.prepareDisplay();
      graphPanel.add(gPanel);
    }
    frame.add(graphPanel, BorderLayout.CENTER);
    addTextDisplay(frame);
    frame.pack();
    multipleON = false;
  }

  class GraphPanel
      extends JPanel {
    final Insets borders = new Insets(2, 2, 2, 2);
    GraphDisplay gDisplay;
    Dimension size;
    Point origin; // in % of graph area

    GraphPanel(Dimension size) {
      this.size = size;
      setSize(size);
      origin = new Point(0, 50);
      gDisplay = new GraphDisplay(this, origin,
                                  frameEventDespatcher);
      gDisplay.setBasicCalculData(basicData);
    }

    int addTrace(GraphInfo gInfo, int trace, Color color) {
      gDisplay.addTrace(gInfo, trace, color);
      return gDisplay.traceCount();
    }

    void prepareDisplay() {
      gDisplay.prepareDisplay();
    }

    public Insets getInsets() {
      return borders;
    }

    public Dimension getMinimumSize() {
      return size;
    }

    public Dimension getPreferredSize() {
      return size;
    }

  }

  class WindowSetListener
      implements ActionListener,
      ItemListener {
    MenuItem mi;
    public void actionPerformed(ActionEvent ae) {
      mi = (MenuItem) ae.getSource();
      if (mi == separateGraphs) {
        if (multipleON) {
          addSeparateGraphDisplays(mainFrame);
        }
      }
      else if (mi == multipleGraphs) {
        if (!multipleON) {
          addMultipleGraphDisplay(mainFrame);
        }
      }
    }

    public void itemStateChanged(ItemEvent ie) {
      if (textDisplayChoice.getState() != textDisplayON) {
        textDisplayON = textDisplayChoice.getState();
        if (multipleON) {
          addMultipleGraphDisplay(mainFrame);
        }
        else {
          addSeparateGraphDisplays(mainFrame);
        }
      }
    }
  }

  class CloseListener
      extends WindowAdapter {

    public void windowClosing(WindowEvent we) {
      mainFrame.setVisible(false);
      mainFrame.dispose();
      if (independantDisplay) {
        System.exit(0);
      }
    }

    public void windowClosed(WindowEvent we) {
      informCloseListeners();
    }
  }

  void informCloseListeners() {
    int size = closeListener.size();
    for (int l = 0; l < size; l++) {
      ( (MDFrameCloseListener) closeListener.elementAt(l)).
          MDFrameClosed(
          new EventObject("Display Frame Closed"));
    }
  }

  class FrameEventDespatcher
      implements EventDespatcher {

    public void addMouseListener(MouseListener ml) {
      graphPanel.addMouseListener(ml);
    }

    public void addFocusListener(FocusListener fl) {
      graphPanel.addFocusListener(fl);
    }

    public void addComponentListener(ComponentListener fl) {
      graphPanel.addComponentListener(fl);
    }

  }

  void debug(String method, String msg) {
    debug(method + ":" + msg);
  }

  void debug(String msg) {
    System.out.println("\n" + new Date() + "\nMixedDisplay:" + msg);
  }

}