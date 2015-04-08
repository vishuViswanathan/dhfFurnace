package basic;

import directFiredHeating.FceSubSection;
import mvUtils.display.InputControl;
import mvUtils.display.NumberTextField;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 8/24/12
 * Time: 2:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class LossTypeAndVal {
    public Integer lossID;
    public LossType lossType;
    double loss;
    double basisDim; // could be area or length or 1 as the case may be
    double temperature; // is valid only for subsections
    boolean noteTemperature = false;
    double fraction = 1;
    InputControl control;
    NumberTextField ntFraction;

    public LossTypeAndVal(Integer lossID, LossType lossType, boolean noteTemperature, InputControl control) {
        this(control);
       setParams(lossID, lossType, noteTemperature);
    }

    public LossTypeAndVal(InputControl control) {
        this.control = control;
        ntFraction = new NumberTextField(control, fraction, 3, false, 0.0, 1.0, "0.000", "Fraction of loss to be applied");
        enableFraction(false);
        ntFraction.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                fraction = ((NumberTextField)e.getSource()).getData();
            }
        });
        reset();
    }

    public void setParams(Integer lossID, LossType lossType, boolean noteTemperature)  {
        this.lossID = lossID;
        this.lossType = lossType;
        this.noteTemperature = noteTemperature;
    }

    public boolean setFraction(double fraction) {
        boolean bRetVal = false;
        if (fraction >= 0 && fraction <= 1) {
            ntFraction.setData(fraction);
            takeFromUI();
            bRetVal = true;
        }
        return bRetVal;
    }

    public void enableFraction(boolean ena) {
        ntFraction.setEnabled(ena);
    }

    public double getFraction() {
        return fraction;
    }

    public void takeFromUI() {
        fraction = ntFraction.getData();
    }

    public NumberTextField getFractionUI() {
        return ntFraction;
    }

    public void reset() {
        basisDim = 0;
        temperature = 0;
        loss = 0;
    }

 /*
    public double addLossREMOVE(FceSubSection subSec, double temperature) {
        LossBasisAndVal basisAndVal = lossType.getLosses(subSec, temperature);
        double val = basisAndVal.loss;
        loss += val;
        if (noteTemperature)
            this.temperature = temperature;
        basisDim += basisAndVal.basisDim;
        return val;
    }
*/
    public double calculateLoss(FceSubSection subSec, double temperature)  {
        takeFromUI();
        LossBasisAndVal basisAndVal = lossType.getLosses(subSec, temperature);
        loss = basisAndVal.loss * fraction;
        if (noteTemperature)
            this.temperature = temperature;
        basisDim = basisAndVal.basisDim;
        return loss;
    }

    public double getLoss() {
        return loss;
    }

}
