package directFiredHeating.transientData;

import basic.Charge;
import directFiredHeating.DFHFurnace;

import javax.swing.*;

public class Transient2D implements Runnable{
    DFHFurnace furnace;
    TwoDCharge twoDCharge;

    @Override
    public void run() { // NOT USED
        evaluate();
    }

    public Transient2D(DFHFurnace furnace, Charge charge) {
        this.furnace = furnace;
        twoDCharge = new TwoDCharge(furnace, charge, 7);
    }

    public boolean evaluate() {
        return twoDCharge.evaluate(furnace.chTempIN);
    }

    public boolean copyS152ToUfs(double lowerLimit, double upperLimit) {
        twoDCharge.copyS152ToUfs(lowerLimit, upperLimit);
        return true;
    }

    public boolean copyTauToUfs(double lowerLimit, double upperLimit) {
        twoDCharge.copyTauToUfs(lowerLimit, upperLimit);
        return true;
    }

    void errMessage(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Transient2D",
                JOptionPane.ERROR_MESSAGE);
    }

    void debug(String msg) {
        System.out.println("Transient2D: " + msg);
    }
}
