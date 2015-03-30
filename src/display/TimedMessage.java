package display;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 2/10/14
 * Time: 12:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class TimedMessage {
    public static final int ERROR = 1, INFO = 2;
    Window parent;
    String header;
    String msg;
    int type;
    int forTime = 0;
    int msgType;
    java.util.Timer timer;

    public TimedMessage(String header, String msg, int type, Window parent, int forTime) {
        this.header = header;
        this.msg = msg;
        this.parent = parent;
        this.forTime = forTime;
        switch(type) {
            case 1:
                msgType = JOptionPane.ERROR_MESSAGE;
                this.header = ((header.length() > 0) ?  header : "ERROR");
                break;
            case 2:
                msgType = JOptionPane.INFORMATION_MESSAGE;
                this.header = ((header.length() > 0) ?  header : "INFORMATION");
                break;
        }
    }

    public TimedMessage(String header, String msg, int type, Window parent) {
        this(header, msg, type, parent, 0);
    }

    public void show() {
        JOptionPane pane = new JOptionPane(msg, msgType);
        JDialog dialog = pane.createDialog(parent, header);
        if (forTime > 0) {
            timer = new java.util.Timer();
            timer.schedule(new CloseDialogTask(dialog), forTime);
        }
        dialog.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (timer != null) {
                    timer.purge();
                    timer.cancel();
                }
            }
        });
        dialog.setVisible(true);
        parent.toFront();
    }

    class CloseDialogTask extends TimerTask {
         JDialog dlg;
         CloseDialogTask(JDialog dlg) {
             this.dlg = dlg;
         }

         public void run() {
             dlg.setVisible(false);
         }
     }
}
