package display;

import basic.ChMaterial;
import basic.Fuel;
import directFiredHeating.DFHResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 5/29/12
 * Time: 4:32 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ControlCenter {
      public boolean canNotify();
    public void enableNotify(boolean ena);
    public Frame parent();
    public ActionListener lengthChangeListener();
    public FocusListener lengthFocusListener();
    public ActionListener calCulStatListener();
    public void pausingCalculation(boolean paused);
    public Fuel fuelFromName(String name);
    public ChMaterial chMatFromName(String name);

    public void abortingCalculation();

    public void resultsReady();
    public void addResult(DFHResult.Type type, JPanel panel);

}
