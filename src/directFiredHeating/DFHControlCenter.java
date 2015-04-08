package directFiredHeating;

import basic.ChMaterial;
import basic.Fuel;
import mvUtils.display.InputControl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 10/12/12
 * Time: 12:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class DFHControlCenter implements InputControl {
    public boolean canNotify() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void enableNotify(boolean ena) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Frame parent() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ActionListener lengthChangeListener() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public FocusListener lengthFocusListener() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ActionListener calCulStatListener() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void pausingCalculation(boolean paused) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Fuel fuelFromName(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ChMaterial chMatFromName(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void abortingCalculation() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void resultsReady() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addResult(DFHResult.Type type, JPanel panel) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
