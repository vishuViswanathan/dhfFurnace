package jsp;

import jsp.JSPConnection;
import jsp.JSPObject;

import javax.swing.*;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 24-Jul-15
 * Time: 10:08 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSPComboBox<E> extends JComboBox<E> {
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
}
