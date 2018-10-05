package level2.simulator;

import TMopcUa.ProcessValue;
import level2.common.L2ParamGroup;
import level2.common.Tag;
import mvUtils.display.InputControl;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.NumberTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 21-Aug-15
 * Time: 4:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class TagWithDisplay extends Tag implements ActionListener, FocusListener, DocumentListener{
    String formatStr;
    JComponent displayComponent;
    NumberTextField numberText;
    JTextArea textArea;
    JTextField booleanStatus;
    JComboBox<String> comboBox;
    boolean uiReady = false;
    boolean isBeingEdited = false;
    int displayErrorCount = 0;
    int displayErrorLimit = 5;

    public TagWithDisplay(L2ParamGroup.Parameter element, TagName tagName, boolean rw, boolean subscribe,
                          String formatStr, InputControl controller) {
        super(element, tagName, rw, subscribe);
        this.formatStr = formatStr;
        switch (dataType) {
            case BOOLEAN:
                if (bSubscribe) {
                    booleanStatus = new JTextField(4);
                    booleanStatus.setDisabledTextColor(Color.BLUE);
                    booleanStatus.setEnabled(false);
                    displayComponent = booleanStatus;
                }
                else {
                    comboBox = new JComboBox<String>(new String[]{"No", "Yes"});
                    comboBox.addActionListener(this);
                    displayComponent = comboBox;
                }
//                comboBox = new JComboBox<String>(new String[]{"No", "Yes"});
//                if (!subscribe)
//                    comboBox.addActionListener(this);
//                else {
//                    BasicComboBoxEditor editor = (BasicComboBoxEditor)comboBox.getEditor();
//                    editor.getEditorComponent().setForeground(Color.BLUE);
//                    editor.getEditorComponent().setBackground(Color.yellow);
//                    comboBox.setEnabled(false);
//                }
//                displayComponent = comboBox;
                break;
            case FLOAT:
                numberText = new NumberTextField(controller, 0, 5, false, -1e6, +1e6, formatStr, "Enter Value");
                if (!subscribe) {
                    numberText.addActionListener(this);
                    numberText.addFocusListener(this);
                }
                else
                if (subscribe) {
                    numberText.setDisabledTextColor(Color.BLUE);
                    numberText.setEnabled(false);
                }

                displayComponent = numberText;
                break;
            case STRING:
                textArea = new JTextArea(2, 10);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                if (!subscribe)
                    textArea.getDocument().addDocumentListener(this);
                else {
                    textArea.setDisabledTextColor(Color.BLUE);
                    textArea.setEnabled(false);
                }
                displayComponent = textArea;
                break;
        }
        displayComponent.addFocusListener(new UIEditListener());
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

    public void focusGained(FocusEvent e) {

    }

    public void focusLost(FocusEvent e) {
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
        if (uiReady && !isBeingEdited) {
            ProcessValue pv = getValue();
            try {
                switch (dataType) {
                    case BOOLEAN:
                        if (bSubscribe)
                            booleanStatus.setText(pv.booleanValue ? "Yes" : "No");
                        else
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
            catch (Exception e) {
                if (displayErrorCount++ > displayErrorLimit) {
                    displayErrorCount = 0;
                    throw(e);
                }
            }
        }
    }

    class UIEditListener implements FocusListener {
        @Override
        public void focusGained(FocusEvent e) {
            isBeingEdited = true;
        }

        @Override
        public void focusLost(FocusEvent e) {
            isBeingEdited = false;
        }
    }
}
