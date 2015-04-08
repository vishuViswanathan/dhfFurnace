package FceElements;

import basic.*;
import mvUtils.display.InputControl;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.NumberLabel;
import mvUtils.display.NumberTextField;
import mvUtils.display.FramedPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 2/25/13
 * Time: 3:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class RegenBurner {
    static public enum Restriction {
        NOTREADY("Not Evaluated"),
        BYHEAT("By Heat Available"),
        BYTEMP("By Flue Temperature"),
        MANUAL("Manually set");
        private final String proName;

        Restriction(String proName) {
            this.proName = proName;
        }

        public String getValue() {
            return name();
        }

        @Override
        public String toString() {
            return proName;
        }

        static Restriction getEnum(String text) {
            Restriction retVal = null;
            if (text != null) {
                for (Restriction b : Restriction.values()) {
                    if (text.equalsIgnoreCase(b.proName)) {
                        retVal = b;
                        break;
                    }
                }
            }
            return retVal;
        }
    }

    Vector<Fuel> fuelList;
    double excessAir = 0.1; // excess air Fraction
    double regenFract = 0.85;
    double regenEff = 0.9;
    double deltaTRegen = 50; // deltat between flue temp and max fluid temp

    double flueInTemp = 1200, flueOutTemp = 200;
    double maxFluidTemp;
    boolean bAirHeated = true, bFuelHeated = false;

    double fluidInTemp = 30;
    double airMaxTemp, fuelMaxTemp;
    Restriction airTRestrict = Restriction.NOTREADY, fuelTRestrict = Restriction.NOTREADY;

    InputControl control;
    Fuel selFuel;
    FuelFiring fuelFiring;
    FlueCompoAndQty unitFlueIn, unitFlueOut;  // flue per unit fuel
    double unitAir; // air per unit fuel including excess air
    boolean fuelOK = false;
    double adiaFlameT;

    public RegenBurner(Vector<Fuel> fuelList, InputControl control) {
        this.fuelList = new Vector<Fuel>(fuelList); // does it create a copy or uses original elements?
        this.control = control;
        init();
    }

    JComboBox<Fuel> coBFuel;
    NumberTextField ntExcessAir, ntRegenFract, ntRegenEff;
    NumberTextField ntDeltaTRegen;
    NumberTextField ntFlueInTemp, ntFlueOutTemp;
    NumberTextField ntFluidInTemp;
    NumberTextField ntAirMaxTemp, ntFuelMaxTemp;
    NumberLabel nlAdiaFlameT;

    JCheckBox chkBAirHeated, chkBFuelHeated;
    JComboBox <Restriction> coBAirRestrict;
    JComboBox <Restriction> coBFuelRestrict;


    void init() {
        FuelAction fuelAction = new FuelAction();
        OtherAction otherAction = new OtherAction();
        ChkBoxAction chkBoxAction = new ChkBoxAction();
        ExitTempAction exitTempAction = new ExitTempAction();

        coBFuel = new JComboBox<Fuel>(fuelList);
        coBFuel.addActionListener(fuelAction);
        ntExcessAir = new NumberTextField(control, excessAir * 100, 6, false, 0, 200, "###.00", "Excess Air (%)");
        ntExcessAir.addActionListener(fuelAction);
        ntRegenFract = new NumberTextField(control, regenFract * 100, 6, false, 0, 100, "###.00", "Regen Flue flow (%)");
        ntRegenFract.addActionListener(otherAction);
        ntRegenEff = new NumberTextField(control, regenEff * 100, 6, false, 50, 100, "###.00", "Regen heat Efficiency (%)");
        ntRegenEff.addActionListener(otherAction);
        ntDeltaTRegen = new NumberTextField(control, deltaTRegen, 6, false, 0, 200, "#,###", "DeltaT Regen (C)");
        ntDeltaTRegen.addActionListener(otherAction);
        ntFlueInTemp = new NumberTextField(control, flueInTemp, 6, false, 100, 2000, "#,###", "Flue Temperature to Regen (C)");
        ntFlueInTemp.addActionListener(otherAction);
        ntFlueOutTemp = new NumberTextField(control, flueOutTemp, 6, false, 0, 2000, "#,###", "Flue Temperature after Regen (C)");
        ntFlueOutTemp.addActionListener(otherAction);
        ntFluidInTemp = new NumberTextField(control, fluidInTemp, 6, false, 0, 1000, "#,###", "Air/Fuel In Temperature to Regen (C)");
        ntFluidInTemp.addActionListener(otherAction);

        ntAirMaxTemp = new NumberTextField(control, airMaxTemp, 6, false, 0, 2000, "#,###", "Air Temperature to Burner (C)");
        ntAirMaxTemp.addActionListener(exitTempAction);
        ntFuelMaxTemp = new NumberTextField(control, fuelMaxTemp, 6, false, 0, 2000, "#,###", "fuel Temperature to Burner (C)");
        ntFuelMaxTemp.addActionListener(exitTempAction);

        chkBAirHeated = new JCheckBox("Air Heated in Regen");
        chkBAirHeated.addActionListener(chkBoxAction);
        chkBFuelHeated = new JCheckBox("Fuel Heated in Regen");
        chkBFuelHeated.addActionListener(chkBoxAction);

        coBAirRestrict = new JComboBox<Restriction>(Restriction.values());
        removeButton(coBAirRestrict);
        coBFuelRestrict = new JComboBox<Restriction>(Restriction.values());
        removeButton(coBFuelRestrict);

        nlAdiaFlameT = new NumberLabel(adiaFlameT, 100, "#,###", true);
        chkBAirHeated.doClick();
        coBFuel.setSelectedItem(null);
        updateUI();
    }

    void removeButton(JComboBox jc) {
        jc.setEnabled(false);
        for (Component child : jc.getComponents()) {
            if (child instanceof JButton) {
                child.setVisible(false);
                break;
            }
        }
    }

    public JPanel regenPanel() {
        FramedPanel outerP = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        JPanel titleP = new JPanel();
        titleP.add(new JLabel("Regen Burner Study"));
        outerP.add(titleP, gbc);
        gbc.gridy++;
        outerP.add(fuelP(), gbc);
        gbc.gridy++;
        outerP.add(resultsP(), gbc);
        return outerP;
    }

    JPanel fuelP() {
        MultiPairColPanel fuelP = new  MultiPairColPanel(250, 60);
        fuelP.addItemPair("Fuel", coBFuel);
        fuelP.addItemPair(ntExcessAir);
        fuelP.addItemPair(ntRegenFract);
        fuelP.addItemPair(ntDeltaTRegen);
        fuelP.addItemPair(ntRegenEff);
        fuelP.addBlank();
        fuelP.addItemPair(ntFlueInTemp);
        fuelP.addItemPair(ntFlueOutTemp);
        fuelP.addBlank();
        fuelP.addItemPair(ntFluidInTemp);
        return fuelP;
    }

    JPanel resultsP() {
        JPanel resultsP = new JPanel(new BorderLayout());
        MultiPairColPanel leftP = new  MultiPairColPanel(200, 60);
        leftP.addItemPair(ntAirMaxTemp);
        leftP.addItemPair(ntFuelMaxTemp);
        resultsP.add(leftP, BorderLayout.WEST);
        MultiPairColPanel rightP = new  MultiPairColPanel(200, 60);
        rightP.addItemPair(coBAirRestrict, chkBAirHeated);
        rightP.addItemPair(coBFuelRestrict, chkBFuelHeated);
        resultsP.add(rightP, BorderLayout.EAST);
        MultiPairColPanel adiaP = new MultiPairColPanel(200, 60);
        adiaP.addItemPair("Adiabatic Flame Temperature", nlAdiaFlameT);
        resultsP.add(adiaP, BorderLayout.SOUTH);
        return resultsP;
    }

    void updateUI() {
//        chkBAirHeated.doClick();
//        chkBFuelHeated.setSelected(bFuelHeated);
        coBAirRestrict.setSelectedItem(airTRestrict);
        coBFuelRestrict.setSelectedItem(fuelTRestrict);
        ntAirMaxTemp.setData(airMaxTemp);
        ntFuelMaxTemp.setData(fuelMaxTemp);
        nlAdiaFlameT.setData(adiaFlameT);
    }

    void takeChkBoxData() {
        bAirHeated = chkBAirHeated.isSelected();
        bFuelHeated = chkBFuelHeated.isSelected();
    }

    void takeFromUI() {
//        selFuel = (Fuel) coBFuel.getSelectedItem();
        excessAir = ntExcessAir.getData() / 100;
        regenFract = ntRegenFract.getData() / 100;
        regenEff = ntRegenEff.getData() / 100;
        deltaTRegen = ntDeltaTRegen.getData();
        flueInTemp = ntFlueInTemp.getData();
        flueOutTemp = ntFlueOutTemp.getData();
        fluidInTemp = ntFluidInTemp.getData();
        excessAir = ntExcessAir.getData();
        excessAir = ntExcessAir.getData();
    }

    void getFlue() {
        selFuel = (Fuel) coBFuel.getSelectedItem();
        excessAir = ntExcessAir.getData() / 100;
        if (selFuel != null) {
            if (!selFuel.isSensHeatSpecified())
                selFuel.getSpHtData(control, coBFuel);
            fuelFiring = new FuelFiring(selFuel, false, excessAir, 0, 0);
            unitAir = fuelFiring.actAFRatio;
            fuelOK = true;
        } else
            fuelOK = false;
    }

    void markNotReady() {
        airTRestrict = Restriction.NOTREADY;
        airMaxTemp = fluidInTemp;
        fuelTRestrict = Restriction.NOTREADY;
        fuelMaxTemp = fluidInTemp;
    }

    void doHeatBalance() {
        takeFromUI();
        if (fuelOK) {
            FlueComposition flueCompo = fuelFiring.flue;
            maxFluidTemp = flueInTemp - deltaTRegen;
            unitFlueIn = new FlueCompoAndQty(flueCompo, fuelFiring.actFFratio, maxFluidTemp);
            double flueHeatIn = unitFlueIn.flueHeat;
            unitFlueOut = new FlueCompoAndQty("Flue Out", unitFlueIn);
            unitFlueOut.setTemperature(flueOutTemp);
            double flueHeatOut = unitFlueOut.flueHeat;
            double heatAvail = regenFract * regenEff * (flueHeatIn - flueHeatOut);
            if (bAirHeated && bFuelHeated) {  // consider both to a common temperature
                if (airTRestrict == Restriction.MANUAL)
                    fuelBalance(heatAvail);
                else if (fuelTRestrict == Restriction.MANUAL)
                    airBalance(heatAvail);
                else
                    commonBalance(heatAvail);
            }
            else if (bAirHeated)
                airBalance(heatAvail);
            else if (bFuelHeated)
                fuelBalance(heatAvail);
            findAdiaFlameT();
        }
        updateUI();
    }

    void findAdiaFlameT() {
        adiaFlameT = fuelFiring.adiabaticFlameT(airMaxTemp, fuelMaxTemp);
    }


    void commonBalance(double heatAvail) {
        boolean onlyAir = true;
        FluidMixture mix = new FluidMixture(fuelFiring.fuel);
        Fluid air = new FlueComposition(onlyAir);
        mix.addFluid(air, unitAir);
        double totUnitFlow = 1 + unitAir; // wtr fuel
        double endTemp = getFinalTemp(mix, totUnitFlow, fluidInTemp, heatAvail);
        if (endTemp > maxFluidTemp) {
            endTemp = maxFluidTemp;
            airTRestrict = Restriction.BYTEMP;
            fuelTRestrict = Restriction.BYTEMP;
        } else {
            airTRestrict = Restriction.BYHEAT;
            fuelTRestrict = Restriction.BYHEAT;
        }
        airMaxTemp = endTemp;
        fuelMaxTemp = endTemp;
    }

    void airBalance(double heatAvail) {
        if (fuelTRestrict == Restriction.MANUAL) {
            Fluid fluid = fuelFiring.fuel;
            double airInHeat = fluid.sensHeatFromTemp(fluidInTemp);
            double airOutHeat = fluid.sensHeatFromTemp(fuelMaxTemp);
            double heatToFluid = airOutHeat - airInHeat;
            if (heatToFluid >= heatAvail) {
                 showMessage("Heat is not available to heat Fuel to " + fuelMaxTemp +
                         "\nLimiting to heat availability!");
                 fuelMaxTemp = fluid.tempFromSensHeat(heatAvail + airInHeat);
                 fuelTRestrict = Restriction.BYHEAT;
                 airMaxTemp = fluidInTemp;
                 airTRestrict = Restriction.BYHEAT;
                 return;
            }
            heatAvail -= heatToFluid;
        }
        boolean onlyAir = true;
        Fluid air = new FlueComposition(onlyAir);
        double endTemp = getFinalTemp(air, unitAir, fluidInTemp, heatAvail);
        if (endTemp > maxFluidTemp) {
            endTemp = maxFluidTemp;
            airTRestrict = Restriction.BYTEMP;
        } else
            airTRestrict = Restriction.BYHEAT;
        airMaxTemp = endTemp;
    }

    void fuelBalance(double heatAvail) {
        if (airTRestrict == Restriction.MANUAL) {
            boolean onlyAir = true;
            Fluid fluid = new FlueComposition(onlyAir);
            double fluidInHeat = fluid.sensHeatFromTemp(fluidInTemp);
            double fluidOutHeat = fluid.sensHeatFromTemp(airMaxTemp);
            double heatToFluid = unitAir * (fluidOutHeat - fluidInHeat);
            if (heatToFluid >= heatAvail) {
                showMessage("Heat is not available to heat air to " + airMaxTemp +
                        "\nLimiting to heat availability!");
                airMaxTemp = fluid.tempFromSensHeat(heatAvail / unitAir + fluidInHeat);
                airTRestrict = Restriction.BYHEAT;
                fuelMaxTemp = fluidInTemp;
                fuelTRestrict = Restriction.BYHEAT;
                return;
            }
            heatAvail -= heatToFluid;
        }
        Fluid fuel = fuelFiring.fuel;
        double endTemp = getFinalTemp(fuel, 1, fluidInTemp, heatAvail);
        if (endTemp > maxFluidTemp) {
            endTemp = maxFluidTemp;
            fuelTRestrict = Restriction.BYTEMP;
        } else
            fuelTRestrict = Restriction.BYHEAT;
        fuelMaxTemp = endTemp;
    }

    double getFinalTemp(Fluid fluid, double flow, double stTemp, double heat) {
        double hSt = fluid.sensHeatFromTemp(stTemp);
        double hEnd = hSt + heat / flow;
        return fluid.tempFromSensHeat(hEnd);
    }

    void showMessage(String msg) {
        JOptionPane.showMessageDialog(control.parent(), msg, "FOR INFORMATION", JOptionPane.INFORMATION_MESSAGE);
        control.parent().toFront();
    }

    class FuelAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            markNotReady();
            getFlue();
            doHeatBalance();
        }
    }

    class OtherAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            markNotReady();
            getFlue();
            doHeatBalance();
        }
    }

    class ChkBoxAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            takeChkBoxData();
            markNotReady();
            ntAirMaxTemp.setEnabled(bAirHeated);
            ntFuelMaxTemp.setEnabled(bFuelHeated);
            doHeatBalance();
        }
    }

    class ExitTempAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            markNotReady();
            Object comp = e.getSource();
            if (comp == ntAirMaxTemp) {
                double val = ((NumberTextField)comp).getData();
                if (val > maxFluidTemp) {
                    showMessage("Air temperature cannot be more than " + maxFluidTemp +
                                "\nTaking limit value!");
                    airMaxTemp = maxFluidTemp;
                    ntAirMaxTemp.setData(airMaxTemp);
                }
                else
                    airMaxTemp = val;
                airTRestrict = Restriction.MANUAL;
                fuelTRestrict = Restriction.NOTREADY;
            }
            if (comp == ntFuelMaxTemp) {
                double val = ((NumberTextField)comp).getData();
                if (val > maxFluidTemp) {
                    showMessage("Fuel temperature cannot be more than " + maxFluidTemp +
                                "\nTaking limit value!");
                    fuelMaxTemp = maxFluidTemp;
                    ntFuelMaxTemp.setData(fuelMaxTemp);
                }
                else
                    fuelMaxTemp = val;
                fuelTRestrict = Restriction.MANUAL;
                airTRestrict = Restriction.NOTREADY;
            }
            updateUI();
            doHeatBalance();
        }
    }

}
