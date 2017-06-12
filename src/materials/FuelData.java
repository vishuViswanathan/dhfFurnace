package materials;

import PropertySetter.FuelComponent;
import PropertySetter.FuelDetails;
import PropertySetter.PropertyControl;
import basic.ChMaterial;
import basic.FlueComposition;
import basic.Fuel;
import directFiredHeating.DFHResult;
import display.ControlCenter;
import mvUtils.display.InputControl;
import mvUtils.math.XYArray;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Stack;
import java.util.Vector;

/**
 * User: M Viswanathan
 * Date: 12-Jun-17
 * Time: 9:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class FuelData extends JApplet implements InputControl, PropertyControl {
    //    static public String jspBase = "http://HYPWAP02:9080/fceCalculations/jsp/";
    static public String jspBase = "http://localhost:9080/fceCalculations/jsp/";
    boolean bCanEdit = true;
    JSObject win;
    JFrame mainF;
    boolean onTest = false;
    InputControl controller;
    Vector<FuelComponent> fuelComponents;
    Vector<FuelDetails> fuelDetails;
    boolean itsON = false;
    public void init() {
        UIManager.put("ComboBox.disabledForeground", Color.black);
        String strTest = this.getParameter("OnTest");
        mainF = new JFrame("Set Material Property");
        mainF.addWindowListener(new winListener());
        fuelComponents = new Stack<FuelComponent>();
        fuelDetails = new Stack<FuelDetails>();
        if (strTest != null)
            onTest = strTest.equalsIgnoreCase("YES");
        if (onTest) {
            setTestData();
            displayIt();
        } else {
            try {
                win = JSObject.getWindow(this);
            } catch (JSException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                win = null;
            }
            Object o;
            o = win.eval("getData()");
        }
    }

    JComboBox cbFuels;
    JPanel detPan;

    class CBFuelListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            setDetailsPanel();
        }
    }

    String newFuelName = "..... CREATE NEW FUEL";
    FuelDetails newFuelDetails;
    void populateCBFuel() {
        if (bCanEdit) {
            try {
                Fuel newF = new Fuel(newFuelName);
                newFuelDetails = new FuelDetails(this, this, newF, "G", fuelComponents, true, bCanEdit);
                newFuelDetails.setComposition(0, 0, 0, 0, 0, 0, 0, 1,0,0,0);
                fuelDetails.add(newFuelDetails);
            } catch (Exception e) {
            }
        }
        cbFuels = new JComboBox(fuelDetails.toArray());
        cbFuels = new JComboBox(fuelDetails.toArray());
        cbFuels.setSelectedIndex(0);
        cbFuels.addActionListener(new CBFuelListener());
    }

    void createUIs() {
        if (!itsON) {
            populateCBFuel();
            JPanel topP = new JPanel();

            topP.add(cbFuels);
            mainF.add(topP, BorderLayout.NORTH);
            mainF.addWindowListener(new winListener());
//            setMenuOptions();
            setDetailsPanel();
            mainF.setLocation(20, 10);
            mainF.pack();
        }
    }

    void setDetailsPanel() {
        if (detPan != null)
            mainF.remove(detPan);
        FuelDetails selected =  (FuelDetails)cbFuels.getSelectedItem();
        detPan = selected.fuelDetPanel();
        mainF.add(detPan, BorderLayout.CENTER );
        detPan.updateUI();
    }

    boolean getFuelComponents() {
        return false;
    }

    void setTestData() {
        addFuelComponents("1001", "CH4", "m3N", "G", "16", "8550", "0.714", "4", "1" , "0", "0", "0", "0, 0, 2000, 700");
        addFuelComponents("1002", "C4H10", "m3N", "G", "58", "29510", "2.589", "10", "4" , "0", "0", "0", "0, 0, 2000, 400");
        addFuelComponents("1003", "H2", "m3N", "G", "16", "8550", "0.714", "4", "1" , "0", "0", "0", "");
        addFuelComponents("1004", "CO", "m3N", "G", "58", "29510", "2.589", "10", "4" , "0", "0", "0", "0, 0, 200, 700");
        addFuelComponents("1005", "C2H2", "m3N", "G", "16", "8550", "0.714", "4", "1" , "0", "0", "0", "0, 0, 200, 700");
        addFuelComponents("1006", "C2H6", "m3N", "G", "58", "29510", "2.589", "10", "4" , "0", "0", "0", "0, 0, 200, 700");
        addFuelComponents("1007", "C3H8", "m3N", "G", "58", "29510", "2.589", "10", "4" , "0", "0", "0", "0, 0, 200, 700");
        addFuelComponents("1008", "N2", "m3N", "G", "16", "8550", "0.714", "4", "1" , "0", "0", "0", "0, 0, 2000, 700");
        addFuelComponents("1009", "CO2", "m3N", "G", "58", "29510", "2.589", "10", "4" , "0", "0", "0", "0, 0, 200, 700");
        addFuelComponents("1010", "O2", "m3N", "G", "58", "29510", "2.589", "10", "4" , "0", "0", "0", "0, 0, 200, 700");
        addFuelComponents("1011", "H2O", "m3N", "G", "58", "29510", "2.589", "10", "4" , "0", "0", "0", "0, 0, 200, 700");
        addFuelData("3001", "FUEL OIL", "G", "kg", "17030", " 19.5", "22", "", "0.132", "0.152", "0.716", "0", "0", "0", "0",
                "0.3", "0", "0", "0", "0.7", "0", "0","0", "0", "0", "0", "0");
        addFuelData("3002", "Mixed Gas", "G", "m3N", "19030", " 19.5", "22", "", "0.132", "0.152", "0.716", "0", "0", "0", "0",
                "0.5", "0", "0", "0", "0.49", "0.010", "0","0", "0", "0", "0", "0");
    }

    public void displayIt() {
        createUIs();
        if (!itsON) {
            itsON = true;
            mainF.setFocusable(true);
            mainF.setVisible(true);
            mainF.requestFocus();
            mainF.toFront();
            mainF.setSize(700, 500);
        }
    }

    public String setCanEdit(String canEditStr) {
        if (canEditStr.trim().equals("CANEDIT"))
            bCanEdit = true;
        else
            bCanEdit = false;
        return "OK";
    }

    public String addFuelComponents(String idInMaterialCode, String name, String units, String type, String molWtStr,
                                    String calValStr, String densityStr, String hAtomsStr, String cAtomsStr,
                                    String oAtomsStr, String sAtomsStr, String nAtomsStr, String heatContStr) {
        String retVal = "OK";
        double molWt, calVal, density, hAtoms, cAtoms, oAtoms, sAtoms, nAtoms;
        try {
            molWt = Double.valueOf(molWtStr);
            calVal = Double.valueOf(calValStr);
            density = Double.valueOf(densityStr);
            hAtoms = Double.valueOf(hAtomsStr);
            cAtoms = Double.valueOf(cAtomsStr);
            oAtoms = Double.valueOf(oAtomsStr);
            sAtoms = Double.valueOf(sAtomsStr);
            nAtoms = Double.valueOf(nAtomsStr);
        } catch (NumberFormatException e) {
            retVal = "ERROR: Number Format in FuelComponent Data!";
            return retVal;
        }
        fuelComponents.add(new FuelComponent(idInMaterialCode, name, units, type, molWt, calVal, density, hAtoms,
                cAtoms, oAtoms, sAtoms, nAtoms, heatContStr));
        return retVal;
    }

    public String addFuelData(String idStr, String name, String type, String units, String calValStr, String airFuelRatioStr,
                              String flueFuelRatioStr, String sensHeatPair,
                              String fractCO2str, String fractH2Ostr, String fractN2str, String fractO2str, String fractSO2str,
                              String h2Str, String coStr, String ch4Str,
                              String c2h6Str, String c2h2Str, String c3h8Str, String c4h10Str,
                              String n2Str, String h2oStr, String co2Str, String o2Str, String cStr, String sStr, String ashStr)  {
        String retVal = "OK";
        int id;
        double calVal, airFuelRatio, flueFuelRatio;
        double fractCO2, fractH2O, fractN2, fractO2, fractSO2;
        double h2, co, ch4, c2h6, c2h2, c3h8, c4h10, n2, h2o, co2, o2;
        XYArray sensHeat = null;
        if (sensHeatPair.trim().length() > 0)
            sensHeat = new XYArray(sensHeatPair);
        try {
            id = Integer.valueOf(idStr);
            calVal = Double.valueOf(calValStr);
            airFuelRatio = Double.valueOf(airFuelRatioStr);
            flueFuelRatio = Double.valueOf(flueFuelRatioStr);
            fractCO2 = Double.valueOf(fractCO2str);
            fractH2O = Double.valueOf(fractH2Ostr);
            fractN2 = Double.valueOf(fractN2str);
            fractO2 = Double.valueOf(fractO2str);
            fractSO2 = Double.valueOf(fractSO2str);
            h2 = Double.valueOf(h2Str);
            co = Double.valueOf(coStr);
            ch4 = Double.valueOf(ch4Str);
            c2h6 = Double.valueOf(c2h6Str);
            c2h2 = Double.valueOf(c2h2Str);
            c3h8 = Double.valueOf(c3h8Str);
            c4h10 = Double.valueOf(c4h10Str);
            n2 = Double.valueOf(n2Str);
            h2o = Double.valueOf(h2oStr);
            co2 = Double.valueOf(co2Str);
            o2 = Double.valueOf(o2Str);
        } catch (NumberFormatException e) {
            retVal = "ERROR: Number Format in Fuel Data!";
            return retVal;
        }
        FlueComposition flueComp = null;
        try {
            flueComp = new FlueComposition("Flue of " + name, fractCO2, fractH2O, fractN2, fractO2, fractSO2);
        } catch (Exception e) {
            return ("ERROR:" + e.getMessage());
        }
        Fuel fuel = new Fuel(name, units, calVal, airFuelRatio, flueFuelRatio, sensHeat, flueComp);
        fuel.setID(id);
        FuelDetails fDet = new FuelDetails(this, this, fuel, type, fuelComponents, false, bCanEdit);
//debug("fuelType " + type + " for fuel " + name);
        if (type.equals("G"))  {
//debug("setting fuel composition for " + name);
            boolean bResp = fDet.setComposition(h2, co, ch4, c2h6, c2h2, c3h8, c4h10, n2, h2o, co2, o2);
            if (bResp)
                fuelDetails.add(fDet);
            else
                retVal = "ERROR: in fuel Composition!";
        }
        return retVal;
    }

    boolean bCanNotify = true;

    public boolean canNotify() {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void enableNotify(boolean ena) {
        bCanNotify = ena;
    }

    public Frame parent() {
        return mainF;
    }

    boolean decide(String title, String msg) {
        int resp = JOptionPane.showConfirmDialog(parent(), msg, title, JOptionPane.YES_NO_OPTION);
        parent().toFront();
        if (resp == JOptionPane.YES_OPTION)
            return true;
        else
            return false;
    }

    void debug(String msg) {
        System.out.println("FuelData " + msg);
    }

    void showError(String msg) {
        JOptionPane.showMessageDialog(parent(), msg, "ERROR", JOptionPane.ERROR_MESSAGE);
        parent().toFront();
    }

    void showMessage(String msg) {
        JOptionPane.showMessageDialog(parent(), msg, "FOR INFORMATION", JOptionPane.INFORMATION_MESSAGE);
        parent().toFront();
    }


    @Override
    public void destroy() {
        debug("In Destroy");
        super.destroy();
        if (!onTest)
            win.eval("gettingOut()");
    }

    void close() {
        debug("CLOSING ...");
        mainF.dispose();
        itsON = false;
        if (!onTest)
            win.eval("gettingOut()");
    } // close

    public boolean saveData(Object o) {
        if (o instanceof FuelDetails) {
            FuelDetails fD = (FuelDetails)o;
            String name = fD.name;
            String trimmedName = name.trim().replace(" ", "").replace("-", "");
            boolean isNew = fD.bNew;
            if (name.substring(0,1).equals("."))
                showError("Fuel name " + name + " is not acceptable");
            else if (name.length() > 30)
                showError("Fuel Name '" + name + "' is too long (> 30)!");
            else {
                boolean bSave = true;
                FuelDetails existing;
                if (isNew) {
                    for (int f = 0; f < fuelDetails.size();  f++) {
                        existing = fuelDetails.get(f);
                        if (existing != newFuelDetails)  { // do not check with new entry
                            if (trimmedName.equalsIgnoreCase(existing.name.trim().replace(" ", "").replace("-", ""))) {
                                showError("Fuel Name '" + name + "' already Exists!\nTry different Name ..." );
                                bSave = false;
                                break;
                            }
                        }
                    }
                }
                else {
                    bSave = decide("Data change for Existing Fuel", "Data for Fuel '" + name + "' will be OVERWRITTEN!");
                }

                if (bSave)  {
                    String[] params = fD.paramsForSaving();
                    bSave= decide("Saving Fuel Data", "Proceed with saving Fuel '" + name + "'?");
                    if (bSave && (win != null))  {
                        Object r = win.call("saveFuelData", params);
                        close();
                    }
                }
            }
        }
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void inEdit(boolean bInEdit) {
        if (bInEdit) {
            cbFuels.setEnabled(false);
        }
        else {
            cbFuels.setEnabled(true);
        }
    }

    public void quit() {
        close();
    }

    class ButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            while (true) {
                if (command.equals("Save Data")) {
                    break;
                }
            }
        }
    }

    class Control implements ControlCenter {

        public boolean canNotify() {
            return true;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void enableNotify(boolean ena) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Frame parent() {
            return mainF;  //To change body of implemented methods use File | Settings | File Templates.
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

    class winListener implements WindowListener {
        public void windowOpened(WindowEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void windowClosing(WindowEvent e) {
            debug("mainF CLOSING");
            destroy();
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void windowClosed(WindowEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void windowIconified(WindowEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void windowDeiconified(WindowEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void windowActivated(WindowEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void windowDeactivated(WindowEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }
    public static void main(String[] args)  {
        FuelData fD = new FuelData();
        fD.init();
    }
}
