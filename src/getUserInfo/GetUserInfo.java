package getUserInfo;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import javax.swing.*;
import java.applet.Applet;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 8/2/13
 * Time: 11:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class GetUserInfo extends Applet {
    String userName = "";
    JSObject win;
    boolean itsON = false;
    boolean onTest = false;
    public GetUserInfo() {
        debug("Starting applet");
    }

    public void init() {
        userName = System.getProperty("user.name");
        String strTest = this.getParameter("OnTest");
        if (strTest != null)
            onTest = strTest.equalsIgnoreCase("YES");
        if (!onTest) {
            try {
                debug("User Name:" + userName);
//                win = JSObject.getWindow(this);
//                String done = (String)win.eval("setUserInfo()");
//                debug("win: " + win + "\ndone = " + done);
            } catch (JSException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                win = null;
            }
        }
//        displayIt();
    }

    public String userName() {
        return userName;
    }

    public void displayIt() {
        if (!itsON) {
            itsON = true;
            JButton butt = new JButton("Proceed");
            butt.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    proceed();
                }
            });
            add(butt);
        }
    }

    void proceed() {
        String resp = (String)win.eval("proceed()");
    }


    void debug(String msg) {
        System.out.println("getUserInfo " + msg);
    }
}
