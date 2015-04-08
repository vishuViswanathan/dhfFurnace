package mvUtils.display;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 12/5/12
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class ComponentPair {
    Component compL, compR;
    boolean bCompLBold, bCompRBold;
    public ComponentPair(Component compL, boolean bCompLBold, Component compR, boolean bCompRBold) {
        this.compL = compL;
        this.bCompLBold = bCompLBold;
        this.bCompRBold = bCompRBold;
        this.compR = compR;
    }

    public String compString(boolean bLeft) {
        Component comp = (bLeft) ? compL : compR;
        if (comp instanceof JLabel)
            return (((JLabel) comp).getText());
        else if (comp instanceof JTextField)
            return (((JTextField)comp).getText());
        else
            return "";
    }

    public boolean isBold(boolean bLeft) {
        return ((bLeft) ? bCompLBold : bCompRBold);
    }

    public Component getComponent(boolean bLeft) {
        if (bLeft)
            return compL;
        else
            return compR;
    }
}

