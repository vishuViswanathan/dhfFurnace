package display;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 11/30/12
 * Time: 11:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class JComboBoxNoButtonPrint extends JComboBox {
    public JComboBoxNoButtonPrint(ComboBoxModel aModel) {
        super(aModel);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public JComboBoxNoButtonPrint(Object[] items) {
        super(items);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public JComboBoxNoButtonPrint(Vector<?> items) {
        super(items);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public JComboBoxNoButtonPrint() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void paint(Graphics g) {
        Component[] comps = getComponents();
        Component co = null;
        if (isPaintingForPrint()){
            for (int c = 0; c < comps.length; c++) {
                co = comps[c];
                if (co instanceof JButton) {
                    remove(co);
                    break;
                }
            }
            super.paint(g);
            add(co);
        }
        else
            super.paint(g);
//        if (isPaintingForPrint())
//            getComponent(1).paint(g);
//        else
//            super.paint(g);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
