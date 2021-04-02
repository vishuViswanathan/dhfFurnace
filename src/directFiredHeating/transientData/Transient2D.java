package directFiredHeating.transientData;

import basic.Charge;
import directFiredHeating.DFHFurnace;

import javax.swing.*;

public class Transient2D implements Runnable{
    DFHFurnace furnace;
    TwoDCharge twoDCharge;

    @Override
    public void run() {
        evaluate();
    }

    public Transient2D(DFHFurnace furnace, Charge charge) {
        this.furnace = furnace;
        twoDCharge = new TwoDCharge(furnace, charge, 7);
    }

    boolean evaluate() {
        return twoDCharge.evaluate(furnace.chTempIN);
    }

    void errMessage(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Transient2D",
                JOptionPane.ERROR_MESSAGE);
    }

    void debug(String msg) {
        System.out.println("Transient2D: " + msg);
    }
}
