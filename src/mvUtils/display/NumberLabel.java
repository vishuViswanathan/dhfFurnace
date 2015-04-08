package mvUtils.display;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 6/11/12
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class NumberLabel extends JLabel implements XLcellData {
    public Color bgColor;
    public Color errColor = Color.red;
    DecimalFormat format;
    String fmtStr;
    boolean notify = true;
    InputControl controller;
    Window parent;
    boolean bBold = false;
    char chDec;
    public NumberLabel(InputControl controller, double val, int width,String fmtStr, boolean bold) {
        super();
        chDec = new DecimalFormatSymbols().getDecimalSeparator();
        if (width > 0)
            setPreferredSize(new Dimension(width, 20));
        this.controller = controller;
        parent = controller.parent();
        setFmtStr(fmtStr);
//        format = new DecimalFormat(fmtStr);
        bgColor = getBackground();
        setHorizontalAlignment(JTextField.RIGHT);
        setData(val);
        this.bBold = bold;
        if (bold)
            setFont(getFont().deriveFont(Font.BOLD));
    }

    public NumberLabel(InputControl controller, double val, int width,String fmtStr) {
        this(controller, val, width, fmtStr, false);
    }

    public NumberLabel(double val, int width, String fmtStr, boolean bold) {
        super();
        chDec = new DecimalFormatSymbols().getDecimalSeparator();
        if (width > 0)
            setPreferredSize(new Dimension(width, 20));
        setFmtStr(fmtStr);
//        format = new DecimalFormat(fmtStr);
        bgColor = getBackground();
        setHorizontalAlignment(JTextField.RIGHT);
        setData(val);
        this.bBold = bold;
        if (bold)
            setFont(getFont().deriveFont(Font.BOLD));
    }

    public NumberLabel(double val, int width, String fmtStr) {
        this(val, width, fmtStr, false);
    }

    void setFmtStr(String fmtStr) {
        this.fmtStr = fmtStr;
        format = new DecimalFormat(fmtStr);
    }

    public void setData(double val) {
        notify = false;
        setText(format.format(val));
        notify  = true;
    }

    public String format(double val) {
        return format.format(val);
    }

    public String getFmtStr() {
        return fmtStr;
    }


    public ValueForExcel getValueForExcel() {
//        String str = getText();
//        str = str.replace(",", "");
//        boolean bNumeric = str.matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+");
//        double numVal = (bNumeric) ? Double.valueOf(str) : Double.NaN;
//        return new ValueForExcel(bBold, getText(), numVal, bNumeric, fmtStr);
        double numVal = getData();
        boolean bNumeric = !(Double.isNaN(numVal));
        return new ValueForExcel(bBold, getText(), numVal, bNumeric, fmtStr);
     }

    public double getData() {
        String textWithError = getText();
        String data = textWithError;
        if(chDec == ',')  {
            data = data.replace(".", "");
            data = data.replace(",", ".");
        }
        else
            data = data.replace(",", "");
        if (data.length() == 0)
            data = "0";
        double val = 0;
        try {
            val = Double.valueOf(data);
        } catch (NumberFormatException e) {
            val = Double.NaN;
            debug("Number Error");
        }
        return val;
    }

    public String toString() {
        return getText();
    }

    public void showError(boolean err) {
        if (err)
            setForeground(errColor);
        else
            setForeground(bgColor);
        updateUI();
    }

    void debug(String msg) {
        System.out.println("NumberLabel [" + getText() + "]" + msg);
    }

}


