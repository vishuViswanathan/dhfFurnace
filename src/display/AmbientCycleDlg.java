package display;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import basic.*;
public class AmbientCycleDlg extends JDialog {
  Frame parent;
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel mainPanel = new JPanel();
  QueryPanel topPanel = new QueryPanel();
  JTextField tfName = new JTextField(30);
  JButton okButton = new JButton("OK");
  JButton cancelButton = new JButton("Cancel");
  JButton addButton = new JButton("Add a Row");
  JButton editButton = new JButton("Edit the Row");
  JPanel buttPanel = new JPanel();
  DefaultListModel listModel =
            new DefaultListModel();
  JList jList1 = new JList(listModel);
  AmbientCycle ambient;

  public AmbientCycleDlg(Frame parent, String title) {
    this(parent, title, null);
  }

  public AmbientCycleDlg(Frame parent, String title,
              AmbientCycle ambient) {
    super(parent, title, true);
    this.parent = parent;
    this.ambient = ambient;
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
    mainPanel.setPreferredSize(new Dimension(400, 300));
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
          AddRow();
        }
      });
    editButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          editRow();
        }
      });
    JPanel dummy1 = new JPanel();
    dummy1.setMinimumSize(new Dimension(30, 30));
    JPanel dummy2 = new JPanel();
    dummy2.setMinimumSize(new Dimension(30, 30));
    getContentPane().add(mainPanel);
    topPanel.addQuery("Name", tfName);
    mainPanel.add(topPanel, BorderLayout.NORTH);
    mainPanel.add(new JScrollPane(jList1), BorderLayout.CENTER);
    mainPanel.add(buttPanel, BorderLayout.SOUTH);
    buttPanel.add(addButton, null);
    buttPanel.add(dummy1, null);
    buttPanel.add(editButton, null);
    buttPanel.add(dummy2, null);
    buttPanel.add(cancelButton, null);
    buttPanel.add(okButton, null);
    initList();
  }

  public AmbientCycleDlg() {
    this(null, "");
  }

  private void initList() {
    if (ambient != null) {
      tfName.setText(ambient.getName());
      double[] data = ambient.getFirstSegment();
      while (data != null) {
        listModel.addElement(new OneCycleElement(data));
        data = ambient.getNextSegment(data);
      }
    }
  }

  void editRow() {
    Object obj = jList1.getSelectedValue();
    if (obj != null) {
      editRow((OneCycleElement)obj);
      jList1.repaint();
    }
  }

  void editRow(OneCycleElement element) {
    OneCycleElementDlg dlg =
            new OneCycleElementDlg(parent, element);
    dlg.setLocationRelativeTo(this);
    dlg.setVisible(true);
  }

  void takeData() {
    if (ambient == null) {
      ambient = new AmbientCycle(tfName.getText());
    }
    else
      ambient.clear();
    int size = listModel.size();
    if (size > 0) {
      for (int n = 0; n < size; n++) {
        ambient.noteCycleSegment((OneCycleElement)listModel.elementAt(n));
      }
    }
    quit();
  }

  void quit() {
    dispose();
    setVisible(false);
  }

  void AddRow() {
    OneCycleElementDlg dlg =
            new OneCycleElementDlg(parent);
    dlg.setLocationRelativeTo(this);
    dlg.setVisible(true);
    OneCycleElement element = dlg.getElement();
    if (element != null) {
      listModel.addElement(element);
    }
  }

  public AmbientCycle getAmbientCycle() {
    if (ambient != null)
      ambient.makeItReady();
    return ambient;
  }

  void errMessage(String msg) {
    JOptionPane.showMessageDialog(null, msg, "AmbientCycleDlg",
                JOptionPane.ERROR_MESSAGE);
  }

  void debug(String msg) {
    System.out.println("AmbientCycleDlg: " + msg);
  }
}




