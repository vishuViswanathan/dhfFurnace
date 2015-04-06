package display;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 5/16/12
 * Time: 11:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class NumberTextField extends JTextField implements ActionListener, FocusListener, XLcellData {
    boolean onlyInteger = true;
    public double min, max;
    public Color bgColor;
    public Color errColor = Color.red;
    public boolean inError;
    String textWithError = "";
    DecimalFormat format;
    String fmtStr;
    String errMsg;
    public String title;
    boolean allowZero = false;
    boolean notify = true;
    InputControl controller;
    Window parent;
    JLabel label;
    boolean bBold = false;
    char chDec;

    public NumberTextField(InputControl controller, double val, int size, boolean onlyInteger, double min, double max, String fmtStr,
                           String title) {
        super("", size);
        chDec = new DecimalFormatSymbols().getDecimalSeparator();
        this.controller = controller;
        if (controller != null)
            parent = controller.parent();
        this.min = min;
        this.max = max;
        this.onlyInteger = onlyInteger;
        this.title = title;
        setFormat(fmtStr);
//        this.fmtStr = fmtStr;
//        format = new DecimalFormat(fmtStr);
        DecimalFormatSymbols  dfs = format.getDecimalFormatSymbols();
        bgColor = getBackground();
        inError = false;
        addActionListener(this);
        addFocusListener(this);
        //        getDocument().addDocumentListener(this);
        setHorizontalAlignment(JTextField.RIGHT);
        label = new JLabel(title);
        setData(val);
//        errMsg = "Enter value between " + format.format(min) + " and " + format.format(max);
        errMsg = "Enter value between " + min + " and " + max;
        setToolTipText(errMsg);
    }

    public NumberTextField(InputControl controller, double val, int size, boolean onlyInteger, double min, double max, String fmtStr,
                           String title, boolean allowZero) {
        this(controller, val, size, onlyInteger, min, max, fmtStr, title);
        this.allowZero = allowZero;
    }

    public void setNormalBackground(Color color) {
        super.setBackground(color);
        bgColor = color;
    }

    public void setFormat(String fmtStr) {
        this.fmtStr = fmtStr;
        format = new DecimalFormat(fmtStr);
    }

    public String getFmtStr() {
        return fmtStr;
    }

    public void setTitle(String title) {
        this.title = title;
        label.setText(title);
    }

    public void setEditable(boolean bEdit) {
        super.setEditable(bEdit);
        setBackground(Color.white);
        if (bEdit)
            setToolTipText(errMsg);
        else
            setToolTipText(null);
    }

    public JLabel getLabel() {
        return label;
    }

    public void showError() {
        if (inError)
            setBackground(errColor);
        else
            setBackground(bgColor);
    }

//    public void resetError() {
//        inError = false;
//        setBackground(bgColor);
//    }


    public boolean dataOK() {
        double val = getData();
        return (val >= min && val <= max);
    }


    public boolean isInError() {
        inError = false;
        if (isEditable()) {
            double val = 0;
            textWithError = getText();
            String data = textWithError;
            if(chDec == ',')  {
                data = data.replace(".", "");
                data = data.replace(",", ".");
            }
            else
                data = getText().replace(",", "");
            try {
                if (onlyInteger) {
                    val = Integer.valueOf(data);
                } else {
                    val = Double.valueOf(data);
                }
            } catch (NumberFormatException e) {
                setText("0");
                val = 0;
                inError = true;
            }
            if (!inError) {
                if ((allowZero && (val == 0)) || (val >= min && val <= max)) {
                    setData(val);
                } else
                    inError = true;
            }
            showError();
        }
        return inError;
    }

    public void setData(double val) {
        notify = false;
//        if (val == 0 && allowZero)
//            setText("");
//        else
        String txt = format.format(val);
        setText(txt);
        showError();
//        resetError();
        notify = true;
    }

    public String format(double val) {
        return format.format(val);
    }




    public double getData() {
        textWithError = getText();
        String data = textWithError;
        if(chDec == ',')  {
            data = data.replace(".", "");
            data = data.replace(",", ".");
        }
        else
            data = data.replace(",", "");
//        String val = getText();
        if (data.length() == 0)
            data = "0";
        return Double.valueOf(data);
    }

    public String titleAndVal() {
        return title + " <" + getText() + ">";
    }

    double oldVal = Double.NEGATIVE_INFINITY;

    public void focusGained(FocusEvent fe) {
        oldVal = getData();
    }

    public void focusLost(FocusEvent fe) {
        if (controller != null)
            if (controller.canNotify())
                takeNote();
    }

    public void actionPerformed(ActionEvent te) {
        if (controller != null)
            if (controller.canNotify())
                takeNote();
    }

//    @Override
//    public String getText() {
//        String text = super.getText();
//
//        return text.replace(",", "");
//    }

    void takeNote() {
        if (isInError()) {

            if (controller != null)
                controller.enableNotify(false);
            JOptionPane.showMessageDialog(null, errMsg + " [" + textWithError + "]", title, JOptionPane.ERROR_MESSAGE);
            if (controller != null)
                controller.enableNotify(true);
            if (!(parent == null))
                parent.toFront();
//          showError();
        }
    }

    public void resetValue() {
        if (oldVal != Double.NEGATIVE_INFINITY)
            if ((allowZero && (oldVal == 0)) || (oldVal >= min && oldVal <= max))
                setData(oldVal);
    }

    public boolean isChanged() {
         return getData() != oldVal;
    }

    public String getName() {
        return label.getText();
    }

    public ValueForExcel getValueForExcel() {
//        String str = getText();
//        str.replace(",", "");
//        boolean bNumeric = str.matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+");
//        double numVal = (bNumeric) ? Double.valueOf(str) : Double.NaN;
        double numVal = getData();
        boolean bNumeric = true;
        return new ValueForExcel(bBold, getText(), numVal, bNumeric, fmtStr);
    }

    public String toString() {
        return format.format(getData());
    }

    void debug(String msg) {
        System.out.println("NumberTextField [" + getText() + "]" + msg);
    }
}
