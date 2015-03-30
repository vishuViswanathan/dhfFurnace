package display;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 11/30/12
 * Time: 10:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class JButtonNoPrint extends JButton {
    public JButtonNoPrint() {
        super();
    }

    public JButtonNoPrint(String name) {
        super(name);
    }

    @Override
    public void paint(Graphics g) {
        if (!isPaintingForPrint())
            super.paint(g);
    }
}
