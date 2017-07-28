package materials;

import PropertySetter.FuelComponent;
import PropertySetter.PropertyControl;
import basic.FlueComposition;
import basic.Fuel;
import display.*;
import mvUtils.display.InputControl;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.NumberTextField;
import mvUtils.math.XYArrayWithFract;
import mvUtils.mvXML.XMLmv;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

/**
 * User: M Viswanathan
 * Date: 14-Jun-17
 * Time: 3:13 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 10/11/12
 * Time: 3:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class FuelParameters {
    boolean bCanEdit = false;
    InputControl control;
    PropertyControl propControl;
    public String name;
    String type;
    public String units;
    Fuel fuel;
    FuelDisplay fuelDisplay;
    public double calVal;
    public double airFuelRatio;
    public double flueFuelRatio;
    public double fractCO2, fractH2O, fractN2, fractO2, fractSO2;
    public double density;
    boolean composAvailable;
    boolean bValid = false;
    public boolean bNew = false;
    XYArrayWithFract netHeatCont;
    //    double H2, CO, CH4, C2H6, C2H2, C3H8, C4H10, N2, H2O, CO2, O2, C, S, Ash;
    LinkedHashMap<String, ElementNvalue> elementHash;

    public FuelParameters(InputControl control, PropertyControl propControl, Fuel fuel, String type,
                       Vector<FuelComponent> fuelComponents, boolean bNew, boolean bCanEdit) {
        this.control = control;
        this.propControl = propControl;
        this.fuel = fuel;
        this.bNew = bNew;
        this.bCanEdit = bCanEdit;
        name = fuel.name;
        this.type = new String(type);
        fuelDisplay = new FuelDisplay(control, fuel);
        elementHash = new LinkedHashMap<>();
        FuelComponent elem;
        compoTot = new NumberTextField(control, 0, 6, false, 0, 100, "##0.000", "TOTAL %", true);
        compoTot.setEditable(false);
        CompoListener compoLi = new CompoListener();
        for (int i = 0; i < fuelComponents.size(); i++) {
            elem = fuelComponents.get(i);
            elementHash.put(elem.elemName, new ElementNvalue(elem, control, compoLi));
        }
        ButtonListener buttL = new ButtonListener();
        if (bCanEdit) {
            saveButt = new JButton("Save Fuel Data");
            saveButt.addActionListener(buttL);
            saveButt.setEnabled(false);
        }
        editButt = new JButton("Edit Fuel Data");
        editButt.addActionListener(buttL);
        editButt.setEnabled(true);
        resetButt = new JButton("Reset Fuel Data");
        resetButt.addActionListener(buttL);
        resetButt.setEnabled(false);
        quitButt = new JButton("Exit");
        quitButt.addActionListener(buttL);
        quitButt.setEnabled(true);
    }

    public boolean setComposition(double H2, double CO, double CH4, double C2H6, double C2H2, double C3H8, double C4H10,
                                  double N2, double H2O, double CO2, double O2) {
        elementHash.get("H2").setFraction(H2);
        elementHash.get("CO").setFraction(CO);
        elementHash.get("CH4").setFraction(CH4);
        elementHash.get("C2H6").setFraction(C2H6);
        elementHash.get("C2H2").setFraction(C2H2);
        elementHash.get("C3H8").setFraction(C3H8);
        elementHash.get("C4H10").setFraction(C4H10);
        elementHash.get("N2").setFraction(N2);
        elementHash.get("H2O").setFraction(H2O);
        elementHash.get("CO2").setFraction(CO2);
        elementHash.get("O2").setFraction(O2);
        double tot = H2 + CO + CH4 + C2H6 + C2H2 + C3H8 + C4H10 + N2 + H2O + CO2 + O2;
        if (Math.abs(tot - 1) < 0.01) {
            evalAll();
            bValid = true;
        } else
            bValid = false;

        return bValid;
    }

    public int getFuelID() {
        return fuel.getId();
    }

    boolean evalAll() {
        boolean bAllOK = true;
        calVal = 0;
        airFuelRatio = 0;
        flueFuelRatio = 0;
        double CO2 = 0, H2O = 0, N2 = 0, O2 = 0, SO2 = 0;
        density = 0;
        double tot = 0;
        ElementNvalue elem;
        Iterator<String> iter = elementHash.keySet().iterator();
        while (iter.hasNext() && bAllOK) {
            elem = elementHash.get(iter.next());
            tot += elem.fraction;
            if (tot > 1) {
                bAllOK = false;
                break;
            }
            calVal += elem.calVal();
            airFuelRatio += elem.airReqd();
            flueFuelRatio += elem.flue();
            CO2 += elem.CO2();
            H2O += elem.H2O();
            N2 += elem.N2();
            SO2 += elem.SO2();
            density += elem.mass();
        }
        if (bAllOK && tot > 0.999) {
            fractCO2 = CO2 / flueFuelRatio;
            fractH2O = H2O / flueFuelRatio;
            fractN2 = N2 / flueFuelRatio;
            fractSO2 = SO2 / flueFuelRatio;
            fractO2 = O2 / flueFuelRatio;
        }
        else
            bAllOK = false;
        return bAllOK;
    }

    JTextField tfFuelName, tfUnits;
    JButton editButt, saveButt, resetButt, quitButt;
    JPanel headPanel() {
        MultiPairColPanel fp = new MultiPairColPanel(60, 300);
        tfFuelName = new JTextField(fuel.name, 40);
        tfFuelName.setEditable(false);
        tfUnits = new JTextField(fuel.units);
        fp.addItemPair("Fuel Name", tfFuelName);
        fp.addItemPair("Units", tfUnits);
        tfUnits.setEditable(false);
        return fp;
    }

    NumberTextField compoTot;
    MultiPairColPanel fuelCompoP;
    JPanel fuelDetP;

    JPanel fuelCompoPanel() {
        MultiPairColPanel jp;
        if (fuelCompoP == null) {
            jp = new MultiPairColPanel("Fuel Composition", 100, 50);
            fuelCompoP = jp;
        }
        else  {
            jp = fuelCompoP;
            jp.removeAll();
        }
        ElementNvalue elem;
        double tot = 0;
        Iterator<String> iter = elementHash.keySet().iterator();
        while (iter.hasNext()) {
            elem = elementHash.get(iter.next());
            jp.addItemPair(elem.getLabel(), elem.getInputField());
            tot += elem.fraction;
        }
        compoTot.setData(tot * 100);
        jp.addItemPair("Total", compoTot);
        return jp;
    }

    public JPanel fuelDetPanel() {
        JPanel jp;
        if (fuelDetP == null) {
            jp = new JPanel(new GridBagLayout());
            fuelDetP = jp;
        }
        else {
            jp = fuelDetP;
            jp.removeAll();
        }
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        jp.add(headPanel(), gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        jp.add(fuelCompoPanel(), gbc);
        gbc.gridx++;
        jp.add(fuelDisplay.fuelDataWithColHeader(false), gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        jp.add(buttonPan(), gbc);
        jp.updateUI();
        return jp;
    }

    JPanel buttonPan() {
        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        Insets ins = new Insets(1, 1, 1, 1);
        gbc.insets = ins;
        jp.add(editButt, gbc);
        gbc.gridx++;
        jp.add(resetButt, gbc);
        if (bCanEdit) {
            gbc.gridx++;
            jp.add(saveButt, gbc);
        }
        gbc.gridx++;
        ins.set(1, 50, 1, 5);
        jp.add(quitButt, gbc);
        jp.updateUI();
        return jp;
    }

    void setFuel() {
        while (evalAll() && true) {
            try {
                fuel.setValues(calVal, airFuelRatio, flueFuelRatio,
                        new FlueComposition("Flue of " + fuel.name, fractCO2, fractH2O,  fractN2, fractO2, fractSO2));
            } catch (Exception e) {
                break;
            }
            fuelDisplay.upDateUIofFuel();
            break;
        }
    }

    boolean compoOK = false;

    void updateUI() {
        tfFuelName.setText(fuel.name);
        tfUnits.setText(fuel.units);
        fuelDetPanel();
        fuelCompoPanel();
    }

    void noteIt() {
        name = tfFuelName.getText();
        Iterator<String> iter = elementHash.keySet().iterator();
        XYArrayWithFract[] allHCont = new XYArrayWithFract[elementHash.size()];
        double tot = 0;
        ElementNvalue elem;
        int e = 0;
        boolean hContAvailable = true;
        while (iter.hasNext()) {
            elem = elementHash.get(iter.next());
            elem.takeValFromUI();
            if (hContAvailable) {
                if (elem.element.heatContent == null || elem.element.heatContent.arrLen < 1)
                    hContAvailable &= false;
                else
                    allHCont[e]  = new XYArrayWithFract(elem.element.heatContent, elem.fraction);
            }
            e++;
            tot += elem.fraction;
        }
        compoTot.setData(tot * 100);
        compoOK = false;
        if (tot > 1)
            compoTot.setBackground(Color.RED);
        else  {
            if (tot < 0.998)
                compoTot.setBackground(Color.gray);
            else {
                compoTot.setBackground(Color.white);
                compoOK = true;
                setFuel();
                if (hContAvailable)
                    netHeatCont = new XYArrayWithFract(allHCont);
                else
                    netHeatCont = null;
            }
        }
        if (bCanEdit) {
            if (compoOK && bInEdit)
                saveButt.setEnabled(true);
            else
                saveButt.setEnabled(false);
        }
    }

    boolean trySavingFuel() {
        return  propControl.saveData(this);
    }

    public String toString() {
        return fuel.name;
    }

    Fuel fuelCopy;
    String typeCopy;
    LinkedHashMap<String, Double> compoCopy;
    boolean bInEdit = false;

    void getReadyToEdit() {
//        showMessage("Enabling Edit");
        fuelCopy = new Fuel(fuel);
        typeCopy = new String(type);
        compoCopy = new LinkedHashMap<String, Double>();
        Iterator<String> iter = elementHash.keySet().iterator();
        String key;
        ElementNvalue elem;
        while (iter.hasNext()) {
            key = iter.next();
            elem = elementHash.get(key);
            elem.takeValFromUI();
            elem.setEditable(true);
            compoCopy.put(key, new Double(elem.fraction));
        }
        if (bNew)
            tfFuelName.setEditable(true);
        resetButt.setEnabled(true);
        editButt.setEnabled(false);
        bInEdit = true;
        propControl.inEdit(bInEdit);
    }

    void resetFuelData() {
//        showMessage("Resetting Data");
        if (fuelCopy != null) {
            fuel = new Fuel(fuelCopy);
            type = new String(typeCopy);
            fuelDisplay = new FuelDisplay(control, fuel);
        }
        if (compoCopy != null) {
            Iterator<String> iter = compoCopy.keySet().iterator();
            String key;
            ElementNvalue elem;
            while (iter.hasNext()) {
                key = iter.next();
                elem = elementHash.get(key);
                elem.setFraction(compoCopy.get(key).doubleValue());
                elem.setEditable(false);
            }
        }
        updateUI();
        resetButt.setEnabled(false);
        editButt.setEnabled(true);
        tfFuelName.setEditable(false);
        bInEdit = false;
        propControl.inEdit(bInEdit);
        if (bCanEdit)
            saveButt.setEnabled(false);
    }

    public String dataInXML() {
        // WARNING do not modify - has to match with modifyFuelProperties.jsp
        StringBuilder retVal = new StringBuilder();
        retVal.append(XMLmv.putTag("ID", ("" + fuel.getId()).trim()));
        retVal.append(XMLmv.putTag("FuelName", name.trim()));  //  FuelName$
        retVal.append(XMLmv.putTag("FuelType", type));  //  FuelType$
        retVal.append(XMLmv.putTag("Units", fuel.units));  //  Units$
        retVal.append(XMLmv.putTag("CalVal", "" + fuel.calVal));  //  CalVal$
        retVal.append(XMLmv.putTag("H2", "" + elementHash.get("H2").fraction));  //  H2$
        retVal.append(XMLmv.putTag("CO", "" + elementHash.get("CO").fraction));  //  CO$
        retVal.append(XMLmv.putTag("CH4", "" + elementHash.get("CH4").fraction));  //  CH4$
        retVal.append(XMLmv.putTag("C2H6", "" + elementHash.get("C2H6").fraction));  //  C2H6$
        retVal.append(XMLmv.putTag("C2H2", "" + elementHash.get("C2H2").fraction));  //  C2H2$
        retVal.append(XMLmv.putTag("C3H8", "" + elementHash.get("C3H8").fraction));  //  C3H8$
        retVal.append(XMLmv.putTag("C4H10", "" + elementHash.get("C4H10").fraction)); //  C4H10$
        retVal.append(XMLmv.putTag("N2", "" + elementHash.get("N2").fraction)); //  N2$
        retVal.append(XMLmv.putTag("H2O",  "" + elementHash.get("H2O").fraction)); //  H2O$
        retVal.append(XMLmv.putTag("CO2", "" + elementHash.get("CO2").fraction)); //  CO2$
        retVal.append(XMLmv.putTag("O2",  "" + elementHash.get("O2").fraction)); //  O2$
        retVal.append(XMLmv.putTag("C", "" + 0)); //  C$
        retVal.append(XMLmv.putTag("S", "" + 0)); //  S$
        retVal.append(XMLmv.putTag("Ash", "" + 0)); //  Ash$
        retVal.append(XMLmv.putTag("AirFuelRation", "" + fuel.airFuelRatio)); //  AirFuelRatio$
        retVal.append(XMLmv.putTag("FlueFuelRation", "" + fuel.flueFuelRatio)); //  FlueFuelRatio$
        retVal.append(XMLmv.putTag("CO2fract", "" + fractCO2)); //  CO2fract$
        retVal.append(XMLmv.putTag("H2Ofract", "" + fractH2O)); //  H2Ofract$
        retVal.append(XMLmv.putTag("SO2fract", "" + fractSO2)); //  SO2fract$
        retVal.append(XMLmv.putTag("N2fract", "" + fractN2)); //  N2fract$
        retVal.append(XMLmv.putTag("O2fract", "" + fractO2)); //  O2fract$
//        retVal.append(XMLmv.putTag("hContSt",  getHContStr()));
        return retVal.toString();
    }

    public LinkedHashMap<String, String> paramsHashForSaving() {
        LinkedHashMap<String, String> retVal = new LinkedHashMap<>();
//        params[0] = (bNew) ? "YES" : "NO";  //newFuel
        retVal.put("ID", ("" + fuel.getId()).trim());
        retVal.put("FuelName", name.trim());  //  FuelName$
        retVal.put("FuelType", type);  //  FuelType$
        retVal.put("Units", fuel.units);  //  Units$
        retVal.put("CalVal", "" + fuel.calVal);  //  CalVal$
        retVal.put("H2", "" + elementHash.get("H2").fraction);  //  H2$
        retVal.put("CO", "" + elementHash.get("CO").fraction);  //  CO$
        retVal.put("CH4", "" + elementHash.get("CH4").fraction);  //  CH4$
        retVal.put("C2H6", "" + elementHash.get("C2H6").fraction);  //  C2H6$
        retVal.put("C2H2", "" + elementHash.get("C2H2").fraction);  //  C2H2$
        retVal.put("C3H8", "" + elementHash.get("C3H8").fraction);  //  C3H8$
        retVal.put("C4H10", "" + elementHash.get("C4H10").fraction); //  C4H10$
        retVal.put("N2", "" + elementHash.get("N2").fraction); //  N2$
        retVal.put("H2O",  "" + elementHash.get("H2O").fraction); //  H2O$
        retVal.put("CO2", "" + elementHash.get("CO2").fraction); //  CO2$
        retVal.put("O2",  "" + elementHash.get("O2").fraction); //  O2$
        retVal.put("C", "" + 0); //  C$
        retVal.put("S", "" + 0); //  S$
        retVal.put("Ash", "" + 0); //  Ash$
        retVal.put("AirFuelRation", "" + fuel.airFuelRatio); //  AirFuelRatio$
        retVal.put("FlueFuelRation", "" + fuel.flueFuelRatio); //  FlueFuelRatio$
        retVal.put("CO2fract", "" + fractCO2); //  CO2fract$
        retVal.put("H2Ofract", "" + fractH2O); //  H2Ofract$
        retVal.put("SO2fract", "" + fractSO2); //  SO2fract$
        retVal.put("N2fract", "" + fractN2); //  N2fract$
        retVal.put("O2fract", "" + fractO2); //  O2fract$
        retVal.put("hContSt",  getHContStr());
        if (retVal.get("hContStr").length() < 3)  {
            showMessage("Specific Heat Data Could not be prepared!");
        }
        return retVal;
    }

    public String[] paramsForSaving() {
        String[] params  = new String[28];
        params[0] = (bNew) ? "YES" : "NO";  //newFuel
        params[1] = ("" + fuel.getId()).trim();
        params[2] = name.trim();  //  FuelName$
        params[3] = type;  //  FuelType$
        params[4] = fuel.units;  //  Units$
        params[5] = "" + fuel.calVal;  //  CalVal$
        params[6] = "" + elementHash.get("H2").fraction;  //  H2$
        params[7] = "" + elementHash.get("CO").fraction;  //  CO$
        params[8] = "" + elementHash.get("CH4").fraction;  //  CH4$
        params[9] = "" + elementHash.get("C2H6").fraction;  //  C2H6$
        params[10] = "" + elementHash.get("C2H2").fraction;  //  C2H2$
        params[11] = "" + elementHash.get("C3H8").fraction;  //  C3H8$
        params[12] = "" + elementHash.get("C4H10").fraction; //  C4H10$
        params[13] = "" + elementHash.get("N2").fraction; //  N2$
        params[14] = "" + elementHash.get("H2O").fraction; //  H2O$
        params[15] = "" + elementHash.get("CO2").fraction; //  CO2$
        params[16] = "" + elementHash.get("O2").fraction; //  O2$
        params[17] = "" + 0; //  C$
        params[18] = "" + 0; //  S$
        params[19] = "" + 0; //  Ash$
        params[20] = "" + fuel.airFuelRatio; //  AirFuelRatio$
        params[21] = "" + fuel.flueFuelRatio; //  FlueFuelRatio$
        params[22] = "" + fractCO2; //  CO2fract$
        params[23] = "" + fractH2O; //  H2Ofract$
        params[24] = "" + fractSO2; //  SO2fract$
        params[25] = "" + fractN2; //  N2fract$
        params[26] = "" + fractO2; //  O2fract$
        params[27] = getHContStr();
        if (params[27].length() < 3)  {
            showMessage("Specific Heat Data Could not be prepared!");
        }
        return params;
    }


    String getHContStr() {
//        debug("netHeatCont :" + netHeatCont);
        if (netHeatCont != null)
            return netHeatCont.getDataPairStr("###0.###", "###0.####");
        else
            return "";
    }

    void showMessage(String msg) {
        Window parent = control.parent();
        JOptionPane.showMessageDialog(parent, msg, "FOR INFORMATION", JOptionPane.INFORMATION_MESSAGE);
        parent.toFront();
    }

    void showError(String msg) {
        Window parent = control.parent();
        JOptionPane.showMessageDialog(parent, msg, "ERROR", JOptionPane.ERROR_MESSAGE);
        parent.toFront();
    }

    void debug(String msg) {
        System.out.println("FuelParameters " + msg);
    }


    class CompoListener implements ActionListener, FocusListener {
        public void actionPerformed(ActionEvent e) {
            noteIt();
        }

        public void focusGained(FocusEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void focusLost(FocusEvent e) {
            noteIt();
        }

    }

    class ButtonListener implements  ActionListener {
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            if (command.equals("Save Fuel Data"))  {
                noteIt();
                if (compoOK) {
                    trySavingFuel();
                }
            }
            else if (command.equals("Edit Fuel Data")) {
                getReadyToEdit();
            }
            else if (command.equals("Reset Fuel Data")) {
                resetFuelData();
            }
            else if (command.equals("Exit") ) {
                propControl.quit();
            }
        }
    }

    class ElementNvalue {
        FuelComponent element;
        JLabel label;
        NumberTextField ntF;
        double fraction;
        InputControl control;

        ElementNvalue(FuelComponent element, InputControl control, CompoListener compoLi) {
            this.element = element;
            this.control = control;
            label = new JLabel(element.elemName + " (%)");
            ntF = new NumberTextField(control, fraction * 100, 6, false, 0, 100, "##0.000", element.elemName + " (%)", true);
            ntF.addActionListener(compoLi);
            ntF.addFocusListener(compoLi);
            setEditable(false);
        }

        void setEditable(boolean bEdit) {
            ntF.setEditable(bEdit);
        }

        void setFraction(double fraction) {
            this.fraction = fraction;
            updateUI();
        }

        Component getLabel() {
            return label;
        }

        Component getInputField() {
            return ntF;
        }

        void updateUI() {
            ntF.setData(fraction * 100);
        }

        void takeValFromUI () {
            fraction = ntF.getData() / 100;
        }
        double calVal() {
            return fraction * element.calValue;
        }

        double airReqd() {
            return fraction * element.airRequired;
        }

        double flue() {
            return fraction * element.flue;
        }

        double CO2() {
            return fraction * element.CO2;
        }

        double H2O() {
            return fraction * element.H2O;
        }

        double N2() {
            return fraction * element.N2;
        }

        double SO2() {
            return fraction * element.SO2;
        }

        double mass() {
            return fraction * element.density;
        }
    }

}

