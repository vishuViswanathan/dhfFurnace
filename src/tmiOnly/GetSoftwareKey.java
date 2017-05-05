package tmiOnly;

import mvUtils.display.InputControl;
import mvUtils.display.OneParameterDialog;
import mvUtils.display.SimpleDialog;
import mvUtils.security.MachineCheck;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 11-May-15
 * Time: 11:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class GetSoftwareKey {
    public String getKey() {
        OneParameterDialog dlg = new OneParameterDialog(new InputControl() {
            public boolean canNotify() {
                return false;
            }

            public void enableNotify(boolean ena) {

            }

            public Window parent() {
                return null;
            }
        }, "Enter request code");
        dlg.setValue("Enter request code:", null, 20);
        dlg.setVisible(true);
        String theKey = "ERROR";
        if (dlg.isOk()) {
            theKey = getKey(dlg.getTextVal());
            showMessage("The software key", theKey);
        }
        return theKey;
    }

    String getKey(String machineID) {
        if (machineID.length() > 12)
            return new MachineCheck().getKey(machineID);
        else
            return " ERROR in machine ID";
    }

    boolean checkKey(String machineID, String key) {
        return new MachineCheck().checkKey(machineID, key);
    }

    public void showMessage(String title, String msg) {
        SimpleDialog.showMessage(title, msg, true);
    }


    public static void main(String[] args) {
        int nArg = args.length;
        if (nArg == 1)
            System.out.print("Software key :" + new GetSoftwareKey().getKey(args[0]));
        else if (nArg == 2) {
            String machineID = args[0];
            String key = args[1];
            boolean ok = (new GetSoftwareKey()).checkKey(machineID, key);
            System.out.print("Software key " + key +
                    ((ok) ? " matches" : " DOES NOT match ") + " machine ID " + machineID);
        }
        else
            System.out.print("Software key :" + new GetSoftwareKey().getKey());
        System.exit(0);
    }
}

