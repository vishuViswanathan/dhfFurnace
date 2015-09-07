package level2.simulator;

import TMopcUa.ProcessData;
import TMopcUa.ProcessValue;
import level2.L2ParamGroup;
import level2.Tag;
import mvUtils.display.InputControl;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.NumberTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 21-Aug-15
 * Time: 4:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class TagWithDisplay extends Tag implements ActionListener, DocumentListener{
    String formatStr;
    JComponent displayComponent;
    NumberTextField numberText;
    JTextArea textArea;
    JComboBox<String> comboBox;
    boolean uiReady = false;

    public TagWithDisplay(L2ParamGroup.Parameter element, TagName tagName, boolean rw, boolean subscribe,
                          String formatStr, InputControl controller) {
        super(element, tagName, rw, subscribe);
        this.formatStr = formatStr;
        switch (dataType) {
            case BOOLEAN:
                comboBox = new JComboBox<String>(new String[]{"No", "Yes"});
                if (!subscribe)
                    comboBox.addActionListener(this);
                displayComponent = comboBox;
                break;
            case FLOAT:
                numberText = new NumberTextField(controller, 0, 6, false, -1e6, +1e6, formatStr, "Enter Value");
                if (!subscribe)
                    numberText.addActionListener(this);
                displayComponent = numberText;
                break;
            case STRING:
                textArea = new JTextArea(2, 40);
                if (!subscribe)
                    textArea.getDocument().addDocumentListener(this);
                displayComponent = textArea;
                break;
        }
        displayComponent.setEnabled(!subscribe);
        uiReady = true;
    }

    JComponent getDisplayComponent() {
        return displayComponent;
    }

    void addToMultiPairColPanel(MultiPairColPanel mp) {
        mp.addItemPair(tagName.toString(), displayComponent);;
    }

    public void actionPerformed(ActionEvent e) {
        switch (dataType) {
            case BOOLEAN:
                setValue((comboBox.getSelectedIndex() == 1));
                break;
            case FLOAT:
                setValue((float)numberText.getData());
                break;
            case STRING:
                setValue(textArea.getText());
                break;
        }
    }

    public void insertUpdate(DocumentEvent e) {
        setValue(textArea.getText());
    }

    public void removeUpdate(DocumentEvent e) {

    }

    public void changedUpdate(DocumentEvent e) {
        setValue(textArea.getText());
    }

    public void updateUI() {
        if (uiReady) {
            ProcessValue pv = getValue();
            switch (dataType) {
                case BOOLEAN:
//                    System.out.println(tagName + " - value = " + processData.getBooleanValue());
                    comboBox.setSelectedIndex((pv.booleanValue) ? 1 : 0);
                    break;
                case FLOAT:
                    numberText.setData(pv.floatValue);
                    break;
                case STRING:
                    textArea.setText(pv.stringValue);
                    break;
            }
        }
    }
}
