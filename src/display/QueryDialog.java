package display;

import javax.swing.*;
import java.awt.HeadlessException;
import java.awt.*;
import java.awt.event.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>P
 * @author not attributable
 * @version 1.0
 */

public class QueryDialog extends JDialog {
    QueryPanel qp = new QueryPanel();
    JButton ok = new JButton("Ok");
    JButton cancel = new JButton("Cancel");
    boolean updated = false;
    public QueryDialog(JFrame owner, String name) {
        super(owner, name);
        setModal(true);
        Container container = getContentPane();
        container.add(qp, "Center");
        JPanel buttonP = new JPanel();
        buttonP.add(cancel);
        buttonP.add(ok);
        container.add(buttonP, "South");
        ButtonListener buttListener = new ButtonListener();
        ok.addActionListener(buttListener);
        cancel.addActionListener(buttListener);
        pack();
   }

    public void addQuery(String label, Component comp) {
        qp.addQuery(label, comp);
        pack();
    }

    public void addQuery(String label, JTextField value, double min, double max) {
        qp.addQuery(label, value, min, max);
    }

    public void addSpace() {
    qp.addSpace();
    pack();
  }

    public void addTextLine(String text) {
    qp.addTextLine(text);
  }

    public boolean isUpdated() {
    return updated;
  }

    class ButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
        Object caller = e.getSource();
        if (caller == ok)
            updated = true;
            dispose();
        }
    }

}