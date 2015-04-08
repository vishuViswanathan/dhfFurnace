package PropertyViewer;

import mvUtils.display.*;
import mvUtils.math.DoubleRange;
import mvUtils.math.OnePropertyDet;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 3/17/12
 * Time: 7:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class ShowProperties extends JApplet {
    public int nTraces = 0;
    boolean onTest = false;
    JSObject win;
    PropertyTraces traces;
    String header;
    boolean itsON = false;
    JFrame mainF;
    JPanel mainFrame;
    String title = new String("TEST GROUP");
    private OnePropertyTrace oneT;

    public ShowProperties() {
    }

    public void init() {
        String strTest = this.getParameter("OnTest");
        if (strTest != null)
            onTest = strTest.equalsIgnoreCase("YES");
        traces = new PropertyTraces();
         if (onTest)   {
            setTestData();
             displayIt();
        }
        else  {
             try {
                 win = JSObject.getWindow(this);
             } catch (JSException e) {
                 e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                 win = null;
             }
            Object o;
            o = win.eval("getData()");
        }
//        displayIt();

    }


    JMenuItem fMenuClose ;
    JMenuItem fMenuOpen;
    GraphPanel tracePanel;

    public void displayIt() {
        if (!itsON && traces != null) {
            itsON = true;
//            traces.addTrace(oneT);
            mainF = new JFrame("Material Properties");

            setMenuOptions();
            mainFrame = new JPanel(new GridBagLayout());
            mainF.getContentPane().add(mainFrame);
 //           add(mainFrame);
            mainFrame.setBackground(new JPanel().getBackground());
            GridBagConstraints gbcMf = new GridBagConstraints();
            gbcMf.anchor = GridBagConstraints.CENTER;
            gbcMf.gridx = 1;
            gbcMf.gridy = 0;
            gbcMf.insets = new Insets(0, 0, 0, 0);
            gbcMf.gridwidth = 1;
            gbcMf.gridheight = 1;
            mainFrame.add(getTitlePanel(), gbcMf);
            gbcMf.gridx = 1;
            gbcMf.gridy = 1;
            mainFrame.add(getGraphPanel(), gbcMf);
            gbcMf.gridx = 0;
            gbcMf.gridy = 0;
            gbcMf.gridwidth = 1;
            gbcMf.gridheight = 2;
            mainFrame.add(getListPanel(), gbcMf);
            if (!onTest) {
                Dimension d = getSize();
                win.eval("setAppletSize(" + d.width + ", " + d.height + ")");
            }
            mainF.setLocation(100, 100);
            mainF.pack();
            mainF.setFocusable(true);
            mainF.setVisible(true);
            mainF.requestFocus();
            mainF.toFront(); //setAlwaysOnTop(true);
        }
    }

    JMenu propMenu;

    void setMenuOptions() {
        JMenuBar mb = new JMenuBar ();
        OnePropertyTrace oneT;
        JMenuItem mI;
        TraceActionListener taL = new TraceActionListener();
        if (traces.nTraces > 1) {
            propMenu = new JMenu ("Property");
            mI = new JMenuItem ("Show All");
            mI.setName("Trace:-1");
            mI.addActionListener (taL);
            mI.setEnabled(false);
            propMenu.add(mI);
            propMenu.addSeparator();
            for (int i = 0; i < traces.nTraces; i++) {
                oneT = traces.getOneTrace(i);
                mI = new JMenuItem (oneT.getPropertyYname());
                mI.setName("Trace:" + i);
                mI.addActionListener (taL);
                propMenu.add(mI);
            }
            mb.add(propMenu);
        }
         mainF.setJMenuBar (mb);
    }
    JLabel jLtitle;

    FramedPanel getTitlePanel() {
        FramedPanel titlePanel = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbcTp = new GridBagConstraints();
        final int hse = 4; // ends
        final int vse = 4; // ends
        final int hs = 0;
        final int vs = 0;
        Insets lt = new Insets(vs, hse, vs, hs);
        Insets ltbt = new Insets(vs, hse, vse, hs);
        Insets bt = new Insets(vs, hs, vse, hs);
        Insets rtbt = new Insets(vs, hs, vse, hse);
        Insets rt = new Insets(vs, hs, vs, hse);
        Insets rttp = new Insets(vse, hs, vs, hse);
        Insets lttp = new Insets(vse, hse, vs, hs);
        Insets tp = new Insets(vse, hs, vs, hs);
        Insets ltrt = new Insets(vs, hse, vs, hse);
        Insets lttprt = new Insets(vse, hse, vs, hse);
        Insets ltbtrt = new Insets(vs, hse, vse, hse);
        Insets mid = new Insets(vs, hs, vs, hs);
        gbcTp.anchor = GridBagConstraints.CENTER;
        gbcTp.gridx = 0;
        gbcTp.gridy = 0;
        gbcTp.insets = lttp;
        titlePanel.add(new Label(title), gbcTp);
        gbcTp.gridy++;
        gbcTp.insets = lt;
        jLtitle = new JLabel();
 //       if (nTraces > 1)
 //           setTitle(-1);
 //       else
            setTitle(0);
        titlePanel.add(jLtitle, gbcTp);
        gbcTp.gridy++;
        return titlePanel;
    }

    void setTitle(int t) {
        if (t < 0)
            jLtitle.setText("ALL");
        else
            jLtitle.setText(traces.getOneTrace(t).getPropertyY().toString() +
                        " vs " + traces.getOneTrace(t).getPropertyX().toString());
    }


    ScrollPane  resultScroll;
    JTable resultTable;
    Panel getListPanel() {
        resultTable = traces.getResultTable();
        JPanel headPanel = new JPanel(new GridLayout(1, 1));
        headPanel.add(resultTable.getTableHeader());
        Panel listPanel = new Panel(new BorderLayout());
        listPanel.add(headPanel, BorderLayout.NORTH);
        resultScroll = new ScrollPane();
        resultScroll.setSize(new Dimension(60, 300));
        resultScroll.add(resultTable);
        resultScroll.repaint();
        listPanel.add(resultScroll, BorderLayout.CENTER);
        return listPanel;
    }

    FramedPanel gP;

    FramedPanel getGraphPanel() {
        gP = new FramedPanel(new GridLayout(1, 0));
        tracePanel =
                    new GraphPanel(new Dimension(700, 350));
        for (int t = 0; t < traces.nTraces; t++)
           tracePanel.addTrace((GraphInfo) traces, t, GraphDisplay.COLORS[t]);
//        if (traces.nTraces > 1)
            tracePanel.setTraceToShow(0);   // all
        tracePanel.prepareDisplay();
        gP.add(tracePanel);
     //   gP.setSize(300,300);
        return gP;
    }

    void setGraphInfo() {
    }

    void setTestData() {
        setGroupName("TEST DATA FROM Viewer");
        addOneTrace( "Temperature", "degC", "Specific Heat", "kcal/m3n/C",
                            "0,0.5,200,0.42, 300,0.45, 600,0.09, 800, 0.2, 1000, 0.87");
        addOneTrace( "Temperature", "degC", "Property2", "props/h",
                            "0,0,200,0.90, 300,0.85, 600,0.94, 800, 0.705, 1200, 0.1, 2000, 328");
        addOneTrace( "Temperature", "degC", "TK", "kcal/m/h/C",
                            "0, 41.76");
    }

    public void setGroupName(String grpName)  {
        title = grpName;
    }

    public String setProperties(String colNameX, String nameX, String unitsX,
                                    String colNameY, String nameY, String unitsY) {
        OnePropertyDet pX = new OnePropertyDet(nameX, unitsX);
        OnePropertyDet pY = new OnePropertyDet(nameY, unitsY);
        oneT.setProperties(pX, pY);
        return "OK";
    }

    public String setHeader(String traceName) {
        TraceHeader tH = new TraceHeader(traceName, "", "");
        oneT.setHeader(tH);
        return "OK";
    }

    public String setAutoRange() {
        if (oneT != null) {
            oneT.setAutoRanges();
            return "OK";

        }
        else
            return "ERROR: Trace not set!";
    }


    void close () {
      mainF.dispose ();
      mainF = null;
      fMenuOpen.setEnabled (true);
      fMenuClose.setEnabled (false);
      itsON = false;
    } // close

    public String setRanges(String startX, String endX, String startY, String endY){
        double xMin, xMax, yMin, yMax;
        String retVal = "OK";
        try {
            xMin = Double.valueOf(startX);
            xMax = Double.valueOf(endX);
            yMin = Double.valueOf(startY);
            yMax = Double.valueOf(endY);
            DoubleRange rX = new DoubleRange(xMin, xMax);
            DoubleRange rY = new DoubleRange(yMin,yMax);
            oneT.setRanges(rX, rY);
        } catch (NumberFormatException e) {
            retVal = "ERROR";
        }
        return retVal;
    }

    public String setData(String dataPairStr) {
        if (oneT.setData(dataPairStr) > 0)
            return "OK";
        else
            return "ERROR";
    }

    public String addOneTrace( String nameX, String unitsX,
                                    String nameY, String unitsY,
                                    String dataPairStr) {
        if (nTraces < GraphDisplay.MAXTRACES) {
            OnePropertyTrace oneT = new OnePropertyTrace();
            oneT.setHeader(new TraceHeader(nameY, "" , ""));
            OnePropertyDet pX = new OnePropertyDet(nameX, unitsX);
            OnePropertyDet pY = new OnePropertyDet( nameY, unitsY);
            oneT.setProperties(pX, pY);
            if (oneT.setData(dataPairStr) > 0) {
                oneT.setAutoRanges();
                traces.addTrace(oneT);
                nTraces += 1;
                return "OK";
            }
            else
                return "ERROR:assigning trace data pair!";
        }
        else
            return "ERROR: Too many traces (Limit " + GraphDisplay.MAXTRACES + ")!";
    }

    public void showApplet() {
        requestFocusInWindow();
    }

    public void showFrame() {
       mainF.setVisible(true);
    }

    public void hideFrame() {
        mainF.setVisible(false);
    }

    class TraceActionListener implements ActionListener {
        public void actionPerformed (ActionEvent e) {
          String command = e.getActionCommand ();
          if (command.equals ("Close")) {
              close ();
          } else {
              Object ob = e.getSource();
              if(ob instanceof JMenuItem) {
                String name = ((JMenuItem) ob).getName();
                  String subst = name.substring(0, 5);
                  if (subst.equals("Trace")){
                      String numStr =  name.split(":")[1];
                      int t =Integer.valueOf(numStr);
//                  propMenu.setVisible(false);
                      tracePanel.setTraceToShow(t);
                      if (t >= 0)  {
                        traces.setTraceForTable(t);
                          resultTable.updateUI();
                      }
                      setTitle(t);
                  }
              }
          }
        } // actionPerformed
    } // class TraceActionListener

    class GraphPanel
        extends JPanel {
      final Insets borders = new Insets(2, 2, 2, 2);
      GraphDisplay gDisplay;
      Dimension size;
      Point origin; // in % of graph area

      GraphPanel(Dimension size) {
        this.size = size;
        setSize(size);
        origin = new Point(0, 00);
        gDisplay = new GraphDisplay(this, origin, null); //frameEventDespatcher);
 //       gDisplay.setBasicCalculData(traces);
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

      public void setTraceToShow(int t) {
        gDisplay.setTraceToShow(t);
//          mainF.repaint();
      }



    } // class GraphPanel

    class FrameEventDespatcher
        implements EventDespatcher {

      public void addMouseListener(MouseListener ml) {
        gP.addMouseListener(ml);
      }

      public void addFocusListener(FocusListener fl) {
        gP.addFocusListener(fl);
      }

      public void addComponentListener(ComponentListener fl) {
        gP.addComponentListener(fl);
      }

    }  // class FrameEventDespatcher  (NOT USED!!!)

  }
