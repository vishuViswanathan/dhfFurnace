package mvUtils.display;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 10/12/12
 * Time: 11:06 AM
 * To change this template use File | Settings | File Templates.
 */
public class ControlledTextField extends JTextField{
    int maxLen, minLen;
    InputControl controller;
    Window parent;
    ActivityListener actL;

    public ControlledTextField(InputControl controller, int minLen, int maxLen, Dimension size) {
        this(controller, minLen, maxLen);
        setPreferredSize(size);
        actL = new ActivityListener();
        addFocusListener(actL);
    }

    public ControlledTextField(InputControl controller, int minLen, int maxLen) {
        super("");
        this.controller = controller;
        parent = controller.parent();
        this.minLen = minLen;
        this.maxLen = maxLen;
    }

    public boolean dataOK() {
        String text = getText();
        int len = text.length();
        return (len >= minLen && len <= maxLen);
    }

    void showError(String msg) {
        JOptionPane.showMessageDialog(parent, msg, "ERROR", JOptionPane.ERROR_MESSAGE);
        parent.toFront();
    }

    class ActivityListener implements FocusListener {
        public void focusGained(FocusEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void focusLost(FocusEvent e) {
            String text = getText();
            int len = text.length();
            if (len < minLen || len > maxLen) {
                if (controller.canNotify())
                    showError("Enter text of length " + minLen + " to " + maxLen);
            }

        }
    }

}
