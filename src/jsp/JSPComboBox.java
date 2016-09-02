package jsp;

import jsp.JSPConnection;
import jsp.JSPObject;
import mvUtils.display.ValueForExcel;
import mvUtils.display.XLcellData;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 24-Jul-15
 * Time: 10:08 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSPComboBox<E> extends JComboBox<E> implements XLcellData{
    JSPConnection jspConnection;
    public JSPComboBox(JSPConnection jspConnection, Vector<E> items) {
        super(items);
        this.jspConnection = jspConnection;
    }

    public Object getSelectedItem() {
        Object o = super.getSelectedItem();
        if (o != null) {
            if (o instanceof JSPObject)
                ((JSPObject) o).collectData(jspConnection);
        }
        return o;
    }

    public void setSelectedItem(Object anObject) {
        super.setSelectedItem(anObject);
        getSelectedItem();
    }

    public Vector<String> collectedItems() {
        Vector<String> collected = new Vector<String>();
        for (int i = 0; i < getItemCount(); i++) {
            Object o = getItemAt(i);
            if (o instanceof JSPObject) {
                if (((JSPObject) o).isDataCollected())
                    collected.add ("" + o);
            }
        }
        return collected;
    }

    /**
     * show the collected list and get confirmation to save
     * @return
     */
    public boolean confirmToSave(Frame parent) {
        boolean retVal = false;
        Vector<String> availableList = collectedItems();
        if (availableList.size() > 0) {
            StringBuilder msg = new StringBuilder("The following are selected to be saved\n") ;
            for (String s: collectedItems())
                msg.append("     " + s + "\n");
            msg.append("\nSelect OK to save");
            retVal = decide(parent, "Confirm saving", msg.toString());
        }
        else
            showError(parent, "Items must have been selected al least once before saving");
        return retVal;
    }

    public void showMessage(Frame parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "FOR INFORMATION", JOptionPane.INFORMATION_MESSAGE);
        parent.toFront();
    }

    public boolean decide(Frame parent, String title, String msg) {
        int resp = JOptionPane.showConfirmDialog(parent, msg, title, JOptionPane.YES_NO_OPTION);
        if (resp == JOptionPane.YES_OPTION)
            return true;
        else
            return false;
    }

    public void showError(Frame parent, String msg) {
         JOptionPane.showMessageDialog(parent, msg, "ERROR", JOptionPane.ERROR_MESSAGE);
         parent.toFront();
    }

    public ValueForExcel getValueForExcel() {
        if (getSelectedIndex() >= 0)
            return new ValueForExcel(false, "" + getSelectedItem());
        else
            return new ValueForExcel(true, "No Selection");
    }

    public String getFmtStr() {
        return null;
    }
}
