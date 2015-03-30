package basic;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;


public class Selector extends JPanel {
    int minVal = 0;
    int maxVal = 100;
    int defVal = 50;
    String name;
    boolean reverse;

    JSlider slider;
    JTextField plane;
    JTextField value;
    boolean vertical;
    Vector <ChangeListener> listeners;

    public Selector(String name, boolean vertical, boolean reverse) {
        super();
        this.vertical = vertical;
        this.reverse = reverse;
        this.name = new String(name);
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        plane = new JTextField(name, name.length());
        plane.setEditable(false);
        value = new JTextField(5);
        if (vertical) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            slider = new JSlider(JSlider.VERTICAL,
                    minVal, maxVal, defVal);
        } else {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            slider = new JSlider(JSlider.HORIZONTAL,
                    minVal, maxVal, defVal);
        }
        value.setEditable(false);
        add(plane);
        add(value);
        add(slider);
        value.setText("" + slider.getValue() + "%");
        slider.addChangeListener(new SliderListener());
        listeners = new Vector<ChangeListener>();
    }

    public int getValue() {
        if (reverse)
            return (100 - slider.getValue());
        else
            return slider.getValue();
    }

    public void setValue(int val) {
        if (val > 100) val = 100;
        if (val < 0) val = 0;
        slider.setValue(val);
    }

    public void addChangeListener(ChangeListener cl) {
        listeners.add(cl);
    }

    public void removeChangeListener(ChangeListener cl) {
        listeners.remove(cl);
    }

    void informListeners() {
        int size = listeners.size();
        if (size > 0) {
            for (int n = 0; n < size; n++) {
                ChangeListener cl =
                        (ChangeListener) listeners.elementAt(n);
                cl.stateChanged(new ChangeEvent(this));
            }
        }
    }

    class SliderListener implements ChangeListener {
        public void stateChanged(ChangeEvent ce) {
            int val = slider.getValue();
            if (reverse)
                val = 100 - val;
            value.setText("" + val + "%");
            informListeners();
        }
    }

}

