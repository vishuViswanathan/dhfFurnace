package display;

import mvUtils.display.ValueForExcel;
import mvUtils.display.XLcellData;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 12/6/12
 * Time: 12:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class SizedLabel extends JLabel implements XLcellData {
    boolean bBold = false;

    public SizedLabel(String name, Dimension d, boolean bBold, boolean shaded) {
        super(name);
        this.bBold = bBold;
        if (bBold)
            setFont(getFont().deriveFont(Font.BOLD));
        setPreferredSize(d);
        if (shaded) {
            setBackground(Color.PINK);
            setOpaque(true);
        }
    }

    public SizedLabel(String name, Dimension d, boolean bBold) {
        this(name, d, bBold, false);
    }
    public SizedLabel(String name, Dimension d) {
        this(name, d, false, false);
    }

    public ValueForExcel getValueForExcel() {
        String str = getText();
        str.replace(",", "");
        boolean bNumeric = str.matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+");
        double numVal = (bNumeric) ? Double.valueOf(str) : Double.NaN;
        return new ValueForExcel(bBold, getText(), numVal, bNumeric);
     }

    public String getFmtStr() {
        return "";
    }
}
