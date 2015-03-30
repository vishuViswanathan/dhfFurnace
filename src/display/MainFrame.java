package display;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainFrame extends JFrame {
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel jPanel1 = new JPanel();
  JMenuBar menuBar1 = new JMenuBar();
  JMenu menuFile = new JMenu();
  JMenuItem menuFileExit = new JMenuItem();
  JMenu menuHelp = new JMenu();
  JMenuItem menuHelpAbout = new JMenuItem();
  JLabel statusBar = new JLabel();

  
  public MainFrame() {
    super();
    try  {
      jbInit();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void jbInit() throws Exception {
    this.getContentPane().setLayout(borderLayout1);
    this.setSize(new Dimension(400, 300));
    menuFile.setText("File");
    menuFileExit.setText("Exit");
    menuFileExit.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        fileExit_ActionPerformed(e);
      }
    });
    menuHelp.setText("Help");
    menuHelpAbout.setText("About");
    menuHelpAbout.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        helpAbout_ActionPerformed(e);
      }
    });
    statusBar.setText("");
    menuFile.add(menuFileExit);
    menuBar1.add(menuFile);
    menuHelp.add(menuHelpAbout);
    menuBar1.add(menuHelp);
    this.setJMenuBar(menuBar1);
    this.getContentPane().add(statusBar, BorderLayout.SOUTH);
    this.getContentPane().add(jPanel1, BorderLayout.CENTER);
  }

  void fileExit_ActionPerformed(ActionEvent e) {
    System.exit(0);
  }

  void helpAbout_ActionPerformed(ActionEvent e) {
    JOptionPane.showMessageDialog(this, new MainFrame_AboutBoxPanel1(), "About", JOptionPane.PLAIN_MESSAGE);
  }
}

 