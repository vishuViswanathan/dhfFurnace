package display;

import java.util.*;
import java.io.*;
import java.lang.Number.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import basic.*;

public class SurfaceAmbientsDlg extends JDialog {
  Frame parent;
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel mainPanel = new JPanel();
  JButton okButton = new JButton("OK");
  JButton cancelButton = new JButton("Cancel");
  JButton addButton = new JButton("Add an Ambient");
  JButton editButton = new JButton("Edit an Ambient");
  JButton fromFileButton = new JButton("From File");

  JPanel buttPanel = new JPanel();
  DefaultListModel listModel =
            new DefaultListModel();
  JList jList1 = new JList(listModel);
  Vector<AmbientCycle> ambients;

  public SurfaceAmbientsDlg(Frame parent) {
    this(parent, null);
  }

  public SurfaceAmbientsDlg(Frame parent,
              Vector<AmbientCycle> ambients) {
    super(parent, "Surface Ambient Definitions", true);
    this.parent = parent;
    this.ambients = ambients;
    try  {
      jbInit();
      pack();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void jbInit() throws Exception {
    mainPanel.setLayout(borderLayout1);
    mainPanel.setPreferredSize(new Dimension(600, 300));
    okButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          takeData();
        }
      });
    cancelButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          quit();
        }
      });
    addButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          addRow();
        }
      });
    editButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          editRow();
        }
      });
    fromFileButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          takeFromFile();
        }
      });
    JPanel dummy1 = new JPanel();
    dummy1.setMinimumSize(new Dimension(30, 30));
    JPanel dummy2 = new JPanel();
    dummy2.setMinimumSize(new Dimension(30, 30));
    getContentPane().add(mainPanel);
    mainPanel.add(new JScrollPane(jList1), BorderLayout.CENTER);
    mainPanel.add(buttPanel, BorderLayout.SOUTH);
    buttPanel.add(addButton, null);
    buttPanel.add(dummy1, null);
    buttPanel.add(editButton, null);
    buttPanel.add(dummy2, null);
    buttPanel.add(fromFileButton, null);
    buttPanel.add(dummy2, null);
    buttPanel.add(cancelButton, null);
    buttPanel.add(okButton, null);
    initList();
  }

  public SurfaceAmbientsDlg() {
    this(null);
  }

  private void initList() {
    if (ambients != null) {
      for (int n = 0; n < ambients.size(); n++) {
        listModel.addElement(ambients.elementAt(n));
      }
    }
  }

  void editRow() {
    Object obj = jList1.getSelectedValue();
    if (obj != null) {
      editRow((AmbientCycle)obj);
      jList1.repaint();
    }
  }

  void editRow(AmbientCycle element) {
    AmbientCycleDlg dlg =
            new AmbientCycleDlg(parent, "Ambient ", element);
    dlg.setLocationRelativeTo(this);
    dlg.setVisible(true);
  }

  void takeFromFile() {
    String fName = getFileName(true);
    if (fName == null)
      return;
    readFile(fName);
  }

  void readFile(String fileName) {
    FileInputStream fis = null;
    String oneLine = null;
    String oneValueStr = null;
    AmbientCycle ambCycle = null;
    try  {
      fis = new FileInputStream(fileName);
    }
    catch (Exception e) {
      errMessage("Some Problem in reading from file!");
      e.printStackTrace();
      return;
    }
    InputStreamReader isr = new InputStreamReader(fis);
    BufferedReader reader = new BufferedReader(isr);
    do {
      oneLine = getOneValidLine(reader); // first header
      if (!oneLine.startsWith("Ambient Data File")) {
        errMessage("Not a proper Ambient data File!");
        break;
      }
      oneLine = getOneValidLine(reader); // second header
      // can check Revision number here
      if (!oneLine.startsWith("Version")) {
        errMessage("Not a proper Ambient data File (Version ...)!");
        break;
      }
      oneLine =  getOneValidLine(reader); // the Ambinet count line;
      if (!oneLine.startsWith("Number of Ambients")) {
        errMessage("Not a proper Ambient data File (Number of Ambients ...)!");
        debug("Line: " + oneLine);
        break;
      }
      int ambCount = new Float(getValueAfterSymbol(oneLine, '=')).intValue();
      for (int nowAmb = 0; nowAmb < ambCount; nowAmb++) {
        ambCycle = getOneAmbCycleFromFile(reader);
        if (ambCycle == null)
          break;
        listModel.addElement(ambCycle);
      }
      break;
    } while (true);

    try {
      fis.close();
    }
    catch (Exception e) {
      errMessage("Unable to close the file!");
      e.printStackTrace();
    }
    return;
  }

// reads one ambient cycle
  AmbientCycle getOneAmbCycleFromFile(BufferedReader reader) {
    String oneLine = getOneValidLine(reader);
    AmbientCycle ambCycle = null;
    boolean done = false;
    do {
      if (oneLine == null || !oneLine.startsWith("Name")) {
        errMessage("Ambient Cycle Name Entry NOT found");
        debug("Line: " + oneLine);
        break;
      }
      String name = getStrAfterSymbol(oneLine, '=');
      if (name == null) {
        errMessage("Ambient Cycle Name NOT found");
        debug("Line: " + oneLine);
        break;
      }
      ambCycle = new AmbientCycle(name);  // got the ambient name
      // get the steps
      oneLine = getOneValidLine(reader);
      if (oneLine == null || !oneLine.startsWith("Steps")) {
        errMessage("Ambient Cycle Steps Entry NOT found");
        debug("Line: " + oneLine);
        break;
      }
      done = true;
      int steps = new Float(getValueAfterSymbol(oneLine, '=')).intValue();
      String[] dataStr = {"", "", ""};
      for (int nowStep = 0; nowStep < steps; nowStep++) {
        oneLine = getOneValidLine(reader);
        if (oneLine == null)
          break;
        if (breakDelimtedString(oneLine, ',', dataStr) < 3)
          break;
        ambCycle.noteCycleSegment(Double.parseDouble(dataStr[0]),
                                  Double.parseDouble(dataStr[1]),
                                  Double.parseDouble(dataStr[2]));
      }
      break;
    } while (true);
    if (done) {
      ambCycle.makeItReady();
      return ambCycle;
    }
    else
      return null;
   }

// returns a trimmed line
  String getOneValidLine(BufferedReader reader) {
    String oneLine = null;
    boolean found = false;
    while (!found) {
      try {
        oneLine = reader.readLine();
        if (oneLine == null)
          break;
        if (oneLine.charAt(0) == '#')
          continue;
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      break;
    } // while not found
    if (oneLine != null)
      oneLine = oneLine.trim();
    return oneLine;
  }

  float getValueAfterSymbol(String str, char symbol) {
    float val = Float.NaN;
    int symbLoc = str.indexOf(String.valueOf(symbol));
    if (symbLoc >= 0) {
      String subStr = str.substring(symbLoc + 1);
      if (subStr != null)
        val = Float.parseFloat(subStr);
    }
    return val;
  }

  String getStrAfterSymbol(String source, char symbol) {
    String str = null;
    int symbLoc = source.indexOf(String.valueOf(symbol));
    if (symbLoc >= 0) {
      str = source.substring(symbLoc + 1);
      if (str != null)
        str = str.trim();
    }
    return str;
  }

  int breakDelimtedString(String source, char symbol, String[] breakup) {
    int count = 0;
    StringTokenizer strTok = new StringTokenizer(source, String.valueOf(symbol));
    int maxCount = breakup.length;
    while (strTok.hasMoreTokens() && count < maxCount) {
      breakup[count] = strTok.nextToken().trim();
      count++;
    }
    return count;
  }

  File lastDirectory = null;

  String getFileName (boolean toRead) {
    GetFileName getFile = new GetFileName(toRead, "amb", "Ambient Data Files");
    getFile.setDirectory(lastDirectory);
    String fName = getFile.getIt(this);
    lastDirectory = getFile.getDirectory();
    return fName;
  }


  void takeData() {
    if (ambients == null) {
      ambients = new Vector<AmbientCycle>();
    }
    else
      ambients.clear();
    int size = listModel.size();
    if (size > 0) {
      for (int n = 0; n < size; n++) {
        ambients.add((AmbientCycle)listModel.elementAt(n));
      }
    }
    quit();
  }

  void quit() {
    dispose();
    setVisible(false);
  }

  void addRow() {
    AmbientCycleDlg dlg =
            new AmbientCycleDlg(parent, "Ambient n");
    dlg.setLocationRelativeTo(this);
    dlg.setVisible(true);
    Object element = dlg.getAmbientCycle();
    if (element != null) {
      listModel.addElement(element);
    }
  }

  public Vector getAmbients() {
    return ambients;
  }

  void errMessage(String msg) {
    JOptionPane.showMessageDialog(null, msg, "SurfaceAmbientsDlg",
                JOptionPane.ERROR_MESSAGE);
  }

  void debug(String msg) {
    System.out.println("SurfaceAmbientsDlg: " + msg);
  }
}




