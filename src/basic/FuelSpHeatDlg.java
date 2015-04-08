package basic;

import mvUtils.display.InputControl;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.NumberTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 9/27/12
 * Time: 12:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class FuelSpHeatDlg extends JDialog implements InputControl {
    JButton okButt = new JButton("Take Sensible Heat");
    JButton cancel = new JButton("IGNORE Sensible Heat");
    Fuel fuel;
    NumberTextField ntSpHt;
    double spHt;
    InputControl controller;
//    Frame parent;

    public FuelSpHeatDlg(InputControl controller, Fuel fuel) {
        super(null, "Sensible Heat for " + fuel.name, ModalityType.APPLICATION_MODAL);
        this.fuel = fuel;
        this.controller = controller;
        jbInit();
    }

    void jbInit() {
        Listener li = new Listener();
        okButt.addActionListener(li);
        cancel.addActionListener(li);
        Container dlgP = getContentPane();
        ntSpHt = new NumberTextField(this, spHt, 6, false, 0, 10, "##0.00", "Specific Heat");
        String units = fuel.units;
        MultiPairColPanel jp = new MultiPairColPanel("Sensible Heat Data not available for " + fuel.name);
        jp.addItemPair("Enter constant Specific Heat (kcal/" + units + ".degC)", ntSpHt);
        jp.addItemPair(cancel, okButt);

        dlgP.add(jp);
        pack();
    }

    public boolean canNotify() {
        return true;
    }

    public void enableNotify(boolean ena) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Window parent() {
        return this;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void toFront() {
        controller.parent().toFront();
        super.toFront();    //To change body of overridden methods use File | Settings | File Templates.
    }

    class Listener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
           if (e.getSource() == okButt) {
               if (!ntSpHt.inError) {
                   fuel.setSensibleHeat("0, 0, 2000, " + ntSpHt.getData() * 2000);
                   setVisible(false);
                   dispose();
                   controller.parent().setVisible(true);
               }
           }
           else {
           setVisible(false);
           dispose();
           controller.parent().setVisible(true);
           }
        }
    }


}
