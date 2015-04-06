package display;

import basic.ComponentPair;
import mvmath.FramedPanel;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 5/25/12
 * Time: 11:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class MultiPairColPanel extends FramedPanel {
    int colPairs;
    int lastRow = 0;
    String title = "";
    boolean bHasTitle = false;
    Insets insetL = new Insets(1, 1, 1, 2);
    Insets insetR = new Insets(1, 2, 1, 1);
    Insets insetLR = new Insets(1, 2, 1, 2);
    Insets insetLBlank = new Insets(5, 1, 5, 2);
    GridBagConstraints gbcL =
            new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, 0, insetL, 0, 0);
    GridBagConstraints gbcR =
            new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, 0, insetR, 0, 0);
    GridBagConstraints gbcLR =
            new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, 0, insetLR, 0, 0);
    int labelWidth = 0, dataWidth = 0;
    Font boldFont;
    int rowCount = 0;
    FramedPanel box = null;
    GridBagConstraints gbcBoxL =
            new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, 0, insetL, 0, 0);
    GridBagConstraints gbcBoxR =
            new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, 0, insetR, 0, 0);
    GridBagConstraints gbcBoxLR =
            new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, 0, insetLR, 0, 0);

    Vector<ComponentPair> compPairs;
//    Font titleFont;
//    Font itemFont, dataFont;
    public MultiPairColPanel(String title, int colPairs, int labelWidth, int dataWidth ) {
        super(new GridBagLayout());
//        itemFont =  new Font("sansserif", Font.PLAIN, 12);
//        dataFont =  new Font("sansserif", Font.BOLD, 12);
        this.colPairs = colPairs;
        this.labelWidth = labelWidth;
        this.dataWidth = dataWidth;
        this.title = title;
        if (title.length() > 0)  {
            setTitle(title);
            bHasTitle = true;
        }
        boldFont = (new JLabel()).getFont().deriveFont(Font.BOLD);
        compPairs = new Vector<ComponentPair>();
    }

    public MultiPairColPanel(String title, int labelWidth, int dataWidth ) {
        this(title, 1, labelWidth, dataWidth);
    }

    public MultiPairColPanel(String title) {
        this(title, 1, 0, 0);
    }

    public MultiPairColPanel(int labelWidth, int dataWidth ) {
        this("", 1, labelWidth, dataWidth);
    }

    public void addGroup() {
        box = new FramedPanel(new GridBagLayout());
        lastRow++;
        gbcL.gridx = 0;
        gbcL.gridwidth = 2;
        gbcL.gridy = lastRow;
        gbcL.anchor = GridBagConstraints.CENTER;
        add(box, gbcL);
        gbcBoxL.gridy = -1;
        gbcBoxR.gridy = -1;
    }

    public void removeAll() {
        super.removeAll();
        if (title.length()  > 0)
            setTitle(title);
    }

    public void setTitle(String title) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 1, 5, 1);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = colPairs * 2;
        JLabel tL  = new JLabel(title);
        Font f = tL.getFont();
        tL.setFont(f.deriveFont(Font.BOLD));
        add(tL, gbc);
    }

    JComponent getItemName(String name, boolean bold) {
        JLabel lName = new JLabel(name);
        if (bold)
            lName.setFont(boldFont);
        if (labelWidth > 0)
            lName.setPreferredSize(new Dimension(labelWidth, 20));
        return lName;
    }

    public void addItemPair(String name, Component comp, boolean bold) {
        Component compLeft =  getItemName(name, bold);
        addItemPair(compLeft, bold, comp, bold);
    }

    public void addItemPair(String name, String val, boolean bold) {
        addItemPair(name, new TextLabel(val, bold), bold);
    }

    public void addItemPair(String name, Component comp) {
        addItemPair(name, comp, false);
    }

    public void addItemPair(String name, double val, String format, boolean bold) {
        Component compLeft =  getItemName(name, bold);
        Component compRight = new NumberLabel(val, dataWidth, format, bold);
        addItemPair(compLeft, bold, compRight, bold);
    }

    public void addItemPair(String name, double val, String format) {
        addItemPair(name, val, format, false);
    }

    public void addItem(Component comp) {
        if (box == null) {
            lastRow++;
            gbcLR.gridx = 0;
            gbcLR.gridy = lastRow;
            gbcLR.gridwidth = 2;
            add(comp, gbcLR);

            gbcL.gridy = lastRow;
            gbcR.gridy = lastRow;
            rowCount++;
        }
        else {
            gbcBoxL.gridx = 0;
            gbcBoxL.gridy++;
            gbcBoxR.gridy++;
            gbcBoxLR.gridx = 0;
            gbcBoxLR.gridwidth = 2;
            gbcBoxLR.gridy = gbcBoxL.gridy;
            box.add(comp, gbcBoxLR);
            rowCount++;
        }

    }

    public int getRowCount() {
        return rowCount;
    }

    public void addItemPair(Component compLeft, Component compRight) {
        addItemPair(compLeft, false, compRight, false);
/*
        lastRow++;
        gbcL.gridx = 0;
        gbcL.gridy = lastRow;
        add(compLeft, gbcL);
        gbcR.gridx = 1;
        gbcR.gridy = lastRow;
        add(compRight, gbcR);
        compPairs.add(new ComponentPair(compLeft, compRight));
        rowCount++;
*/
    }

    public void addItemPair(Component compLeft, boolean bBoldLeft, Component compRight, boolean bBoldRight) {
        if (box == null) {
            lastRow++;
            gbcL.gridx = 0;
            gbcL.gridy = lastRow;
            add(compLeft, gbcL);
            gbcR.gridx = 1;
            gbcR.gridy = lastRow;
            add(compRight, gbcR);
            compPairs.add(new ComponentPair(compLeft, bBoldLeft, compRight, bBoldRight));
            rowCount++;
        }
        else {
            gbcBoxL.gridx = 0;
            gbcBoxL.gridy++;
            box.add(compLeft, gbcBoxL);
            gbcBoxR.gridx = 1;
            gbcBoxR.gridy++;
            box.add(compRight, gbcBoxR);
            compPairs.add(new ComponentPair(compLeft, bBoldLeft, compRight, bBoldRight));
            rowCount++;
        }
    }


    String componentString(int row, boolean bLeft) {
        if (row < rowCount) {
            return (compPairs.get(row).compString(bLeft));
        }
        else
            return "NO DATA!";
    }

    ComponentPair getComponentPair(int row) {
        if (row < rowCount) {
            return (compPairs.get(row));
        }
        else
            return null;
    }

    public void addItemPair(NumberTextField ntf, boolean bold) {
        Component compLeft =  getItemName(ntf.getName(), bold);
        Component compRight = ntf;
        addItemPair(compLeft, compRight);
    }

    public void addItemPair(NumberTextField ntf) {
        addItemPair(ntf, false);
    }

    public void addItemPair(NumberTextField ntf, boolean bAllowEdit, boolean bold) {
        Component compLeft =  getItemName(ntf.getName(), bold);
        Component compRight = ntf;
        if (compRight instanceof JTextComponent)
            ((JTextComponent) compRight).setEditable(bAllowEdit);
        addItemPair(compLeft, compRight);
    }

    public void addBlank() {
        gbcL.insets = insetLBlank;
        JPanel p1 = new JPanel();
        addItemPair(p1, p1);
        gbcL.insets = insetL;
    }
}
