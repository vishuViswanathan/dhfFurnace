package display;

import basic.FlueComposition;
import basic.Fuel;
import mvUtils.jsp.JSPComboBox;
import mvUtils.jsp.JSPConnection;
import mvUtils.display.*;
import mvUtils.display.FramedPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 9/15/12
 * Time: 4:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class FuelDisplay {
    JSPConnection jspConnection;
    Fuel fuel = null;
    String units = "";
    FlueComposition flue;
    boolean bExisting;
    Vector<Fuel> fuelList;
    FuelDisplay baseFuelD, addFuelD;
    Dimension colSize = new Dimension(100, 25);
    FuelDisplay listener;
    public static NumberTextField ntBaseShare;
    public static JComboBox cBmixType;
    InputControl controller;
    double temperature = 0;

    public FuelDisplay(InputControl controller, JSPConnection jspConnection, Vector<Fuel> fuelList)  {
        this.jspConnection = jspConnection;
        this.fuelList = fuelList;
        bExisting = true;
        this.controller = controller;
        init();
//        cbFuel = new JComboBox<Fuel>(fuelList);
        cbFuel = new JSPComboBox<Fuel>(jspConnection, fuelList);
        cbFuel.addActionListener(new FuelChangeListener());
        cbFuel.setSelectedIndex(0);
        units = ((Fuel)(cbFuel.getSelectedItem())).units;
        upDateUI();
    }

    public FuelDisplay(InputControl controller, FuelDisplay baseFuelD, FuelDisplay addFuelD)  {
        this.controller = controller;
        this.baseFuelD = baseFuelD;
        this.addFuelD = addFuelD;
        this.fuelList = baseFuelD.fuelList;
        bExisting = false;
        units = baseFuelD.units;
        cBmixType =  new JComboBoxNoButtonPrint(new String[]{"Heat", "Flow"});
        ntBaseShare =  new NumberTextField(controller, 50, 6, false, 1, 100, "###.00", "", false);
        MixTypeChangeListener li = new MixTypeChangeListener();
        ntBaseShare.addActionListener(li);
        ntBaseShare.addFocusListener(li);
        cBmixType.addActionListener(li);
        init();
        tfName = new JTextField("Fuel Mix 01", 20);
        tfName.setPreferredSize(new Dimension(100, 30));
        upDateUI();
     }

    public FuelDisplay(InputControl controller, Fuel fuel) {
        this.controller = controller;
        this.fuel = fuel;
        units = fuel.units;
        init();
        upDateUIofFuel(fuel);
    }

    void init() {
//---
        (nlFlowShare = new NumberLabel(0, 60, "##0.##")).setPreferredSize(colSize);
        (nlHeatShare = new NumberLabel(0, 60, "##0.##")).setPreferredSize(colSize);
// ---
        (tlUnits = new TextLabel("")).setPreferredSize(colSize);
        (nlCalVal = new NumberLabel(0, 60, "#,###")).setPreferredSize(colSize);
        ntFuelTemp = new NumberTextField(controller, temperature, 9, false, 0, 1200, "#,##0",
                "Fuel Temperature", true );
        (nlAirFuelRatio = new NumberLabel(0, 60, "#,##0.00")).setPreferredSize(colSize);
        (nlFlueFuelRatio = new NumberLabel(0, 60, "#,##0.00")).setPreferredSize(colSize);
        (nlFractCO2 = new NumberLabel(0, 60, "#,##0.00")).setPreferredSize(colSize);
        (nlFractH2O = new NumberLabel(0, 60, "#,##0.00")).setPreferredSize(colSize);
        (nlFractSO2 = new NumberLabel(0, 60, "#,##0.00")).setPreferredSize(colSize);
        (nlFractO2 = new NumberLabel(0, 60, "#,##0.00")).setPreferredSize(colSize);
        (nlFractN2 = new NumberLabel(0, 60, "#,##0.00")).setPreferredSize(colSize);
    }

    public static JPanel rowHeader(boolean bAll) {
        FramedPanel jp = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        Dimension headSize = new Dimension(150, 25);
        gbc.gridx = 0;
        gbc.gridy = 0;
        if (bAll) {
            JLabel h1 = new TextLabel("Fuel Name");
            h1.setPreferredSize(headSize);
            jp.add(h1, gbc);
            gbc.gridy++;
//----
            JLabel h101 = new TextLabel("Flow Share (%)");
            h101.setPreferredSize(headSize);
            jp.add(h101, gbc);
            gbc.gridy++;
            JLabel h102 = new TextLabel("Heat Share (%)");
            h102.setPreferredSize(headSize);
            jp.add(h102, gbc);
            gbc.gridy++;

//----
        }
        JLabel h2 = new TextLabel("Flow Units");
        h2.setPreferredSize(headSize);
        jp.add(h2, gbc);
        gbc.gridy++;
        JLabel h201 = new TextLabel("Calorific Value (kcal/unit)");
        h201.setPreferredSize(headSize);
        jp.add(h201, gbc);
        gbc.gridy++;
        if (bAll) {
            JLabel h3= new TextLabel("Fuel Temperature (degC)");
            h3.setPreferredSize(headSize);
            jp.add(h3, gbc);
            gbc.gridy++;
        }
        JLabel h4 = new TextLabel("Stoic Air Fuel Ratio");
        h4.setPreferredSize(headSize);
        jp.add(h4, gbc);
        gbc.gridy++;
        JLabel h5 = new TextLabel("Stoic Flue Fuel Ratio", JLabel.LEFT);
        h5.setPreferredSize(headSize);
        jp.add(h5, gbc);
        gbc.gridy++;
        JLabel h0 = new TextLabel("");
        h0.setPreferredSize(headSize);
        jp.add(h0, gbc);
        gbc.gridy++;
        JLabel h6 = new TextLabel("Stoic Flue Composition", JLabel.RIGHT);
        h6.setPreferredSize(headSize);
        jp.add(h6, gbc);
        gbc.gridy++;
        JLabel h7 = new TextLabel("CO2 (%)", JLabel.RIGHT);
        h7.setPreferredSize(headSize);
        jp.add(h7, gbc);
        gbc.gridy++;
        JLabel h8 = new TextLabel("H2O (%)", JLabel.RIGHT);
        h8.setPreferredSize(headSize);
        jp.add(h8, gbc);
        gbc.gridy++;
        JLabel h9 = new TextLabel("SO2 (%)", JLabel.RIGHT);
        h9.setPreferredSize(headSize);
        jp.add(h9, gbc);
        gbc.gridy++;
        JLabel h10 = new TextLabel("O2 (%)", JLabel.RIGHT);
        h10.setPreferredSize(headSize);
        jp.add(h10, gbc);
        gbc.gridy++;
        JLabel h11 = new TextLabel("N2 (%)", JLabel.RIGHT);
        h11.setPreferredSize(headSize);
        jp.add(h11, gbc);
        return jp;
    }

    public static JPanel rowHeader() {
        return rowHeader(true);
    }

//    public JComboBox<Fuel> cbFuel;
    public JSPComboBox<Fuel> cbFuel;
    JTextField tfName;
    NumberLabel nlFlowShare, nlHeatShare;
    NumberLabel nlCalVal, nlAirFuelRatio, nlFlueFuelRatio;
    NumberLabel nlFractCO2, nlFractH2O, nlFractSO2, nlFractO2, nlFractN2;
    NumberTextField ntFuelTemp;
    TextLabel tlUnits;

    public JPanel fuelData(boolean bAll) {
        FramedPanel jp = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        if (bAll)  {
            if (bExisting) {
                jp.add(cbFuel, gbc);
                gbc.gridy++;
                jp.add(nlFlowShare, gbc);
                gbc.gridy++;
                jp.add(nlHeatShare, gbc);
                gbc.gridy++;
            }
            else {
                jp.add(tfName, gbc);
                gbc.gridy++;
                JLabel dummy00 = new TextLabel(" ");
                dummy00.setPreferredSize(colSize);
                jp.add(dummy00, gbc);
                gbc.gridy++;
                JLabel dummy01 = new TextLabel(" ");
                dummy01.setPreferredSize(colSize);
                jp.add(dummy01, gbc);
                gbc.gridy++;
            }
        }
        jp.add(tlUnits, gbc);
        gbc.gridy++;
        jp.add(nlCalVal, gbc);
        gbc.gridy++;
        if (bAll) {
            if(bExisting)
                jp.add(ntFuelTemp, gbc);
            else  {
                JLabel dummy0 = new JLabel("");
                 dummy0.setPreferredSize(colSize);
                 jp.add(dummy0, gbc);
            }
            gbc.gridy++;
        }
        jp.add(nlAirFuelRatio, gbc);
        gbc.gridy++;
        jp.add(nlFlueFuelRatio, gbc);
        gbc.gridy++;
        JLabel dummy = new JLabel("");
        dummy.setPreferredSize(colSize);
        jp.add(dummy, gbc);
        gbc.gridy++;
        JLabel dummy2 = new JLabel("");
        dummy2.setPreferredSize(colSize);
        jp.add(dummy2, gbc);
        gbc.gridy++;
        jp.add(nlFractCO2, gbc);
        gbc.gridy++;
        jp.add(nlFractH2O, gbc);
        gbc.gridy++;
        jp.add(nlFractSO2, gbc);
        gbc.gridy++;
        jp.add(nlFractO2, gbc);
        gbc.gridy++;
        jp.add(nlFractN2, gbc);
        return jp;
    }

    public JPanel fuelData() {
        return fuelData(true);
    }

    MultiPairColPanel mpFuelData;

    public MultiPairColPanel fuelDataWithColHeader(boolean bWithName) {
        if (bWithName)
            mpFuelData = new MultiPairColPanel("Fuel: " + fuel.name, 100, 60);
        else
            mpFuelData = new MultiPairColPanel(100, 60);
        Container rowHead = rowHeader(false);
        Container data = fuelData(false);
        Component[] cLAll = rowHead.getComponents();
        Component[] cRAll = data.getComponents();
        int nRows = cLAll.length;
        Component cL, cR;
        for (int c = 0; c < nRows; c++) {
            try {
                cL = cLAll[c];
                cR = cRAll[c];
                mpFuelData.addItemPair(cL, cR);
            } catch (Exception e) {
                break;
            }
        }
        return mpFuelData;
    }

    public JPanel fuelDataWithColHeaderOLD() {
        JPanel fp = new JPanel(new BorderLayout());
        fp.add(rowHeader(false), BorderLayout.WEST);
        fp.add(fuelData(false), BorderLayout.EAST);
        return fp;
    }

    public void upDateUI() {
        if (bExisting && (cbFuel.getSelectedIndex() >= 0))
            fuel = (Fuel)cbFuel.getSelectedItem();
        if (fuel != null) {
            upDateUIofFuel(fuel);
        }
        else {
            nlCalVal.setText("NA");
            nlAirFuelRatio.setText("NA");
            nlFlueFuelRatio.setText("NA");
            nlFractCO2.setText("NA");
            nlFractH2O.setText("NA");
            nlFractSO2.setText("NA");
            nlFractO2.setText("NA");
            nlFractN2.setText("NA");

        }
    }



    void upDateUIofFuel(Fuel fuel) {
        nlFlowShare.setData(fuel.myFlowShare * 100);
        nlHeatShare.setData(fuel.myHeatShare * 100);
        nlCalVal.setData(fuel.calVal);
        tlUnits.setText(fuel.units);
        nlAirFuelRatio.setData(fuel.airFuelRatio);
        nlFlueFuelRatio.setData(fuel.flueFuelRatio);
        flue = fuel.flueComp;
        nlFractCO2.setData(flue.fractCO2 * 100);
        nlFractH2O.setData(flue.fractH2O * 100);
        nlFractSO2.setData(flue.fractSO2 * 100);
        nlFractO2.setData(flue.fractO2 * 100);
        nlFractN2.setData(flue.fractN2 * 100);
    }

    public void upDateUIofFuel() {
        if (fuel != null)
            upDateUIofFuel(fuel);
    }

    public Fuel getFuel() {
        return fuel;
    }


    public void noteChangeListener(FuelDisplay listener) {
        this.listener =  listener;
    }



    public void dataChanged() {
        try {
            double baseShare = ntBaseShare.getData() / 100;
            int sel = cBmixType.getSelectedIndex();
            boolean bFlowSharing = (sel == 1);
            double baseFuelT = baseFuelD.ntFuelTemp.getData();
            double addFuelT =  addFuelD.ntFuelTemp.getData();
            fuel = new Fuel(tfName.getText(), baseFuelD.fuel, baseFuelT,
                    addFuelD.fuel, addFuelT, baseShare, bFlowSharing);
            baseFuelD.upDateUI();
            addFuelD.upDateUI();
        } catch (Exception e) {
            showError("" + e.getMessage());
            fuel = null;
        }
        upDateUI();
    }

    public void getSpHtIfRequired() {
        double baseFuelT = baseFuelD.ntFuelTemp.getData();
        double addFuelT =  addFuelD.ntFuelTemp.getData();
        if ((baseFuelT > 0) && !baseFuelD.fuel.isSensHeatSpecified(controller, baseFuelT))
            baseFuelD.fuel.getSpHtData(controller, baseFuelD.ntFuelTemp);
        if (baseFuelD.fuel.isSensHeatSpecified()) {
            if ((addFuelT > 0) && !addFuelD.fuel.isSensHeatSpecified(controller, addFuelT))
                addFuelD.fuel.getSpHtData(controller, addFuelD.ntFuelTemp);
        }
        if (!baseFuelD.fuel.isSensHeatSpecified() || !addFuelD.fuel.isSensHeatSpecified()) {
            showError("Sensible Heats of fuels will be ignored!");
        }
        else {
            fuel.noteMixElemSensHeat();
        }
    }

    void showError(String msg) {
        if (controller != null)    {
            JOptionPane.showMessageDialog(controller.parent(), msg, "ERROR", JOptionPane.ERROR_MESSAGE);
            controller.parent().toFront();
        }
        else
            debug(msg);
    }

    void debug(String msg) {
        System.out.println("FuelDisplay: " + msg);
    }

    class FuelChangeListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
//            if (src instanceof JComboBox) {
//                Fuel fuel = (Fuel)(((JComboBox)src).getSelectedItem());
//                if (fuel.bMixedFuel)
//                    showError("A Mixed Fuel cannot be element of another Mixed Fuel!");
//                else {
                    upDateUI();
                     if (listener != null)
                         listener.dataChanged();
//                }
//            }
         }
    }

    class MixTypeChangeListener implements ActionListener, FocusListener {
         public void actionPerformed(ActionEvent e) {
             dataChanged();
         }

        public void focusGained(FocusEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void focusLost(FocusEvent e) {
            dataChanged();
        }

    }

 }
