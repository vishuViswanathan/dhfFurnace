package mvUtils.display;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 12/7/12
 * Time: 1:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class TextLabel extends JLabel implements XLcellData {
    boolean bBold = false;
    public TextLabel(String text, boolean bBold, int horzAlignment) {
        super(text, horzAlignment);
        if (bBold)
            setFont(getFont().deriveFont(Font.BOLD));
        this.bBold = bBold;
    }

    public TextLabel(String text, boolean bBold) {
        this(text, bBold, JLabel.LEFT);
    }

    public TextLabel(String text) {
        this(text, false);
    }

    public TextLabel(String text, int horzAlignment) {
        this(text, false, horzAlignment);
    }

    public ValueForExcel getValueForExcel() {
        return new ValueForExcel(bBold, getText());
    }

    public String getFmtStr() {
        return "";
    }

    public String toString() {
        return getText();
    }
}
