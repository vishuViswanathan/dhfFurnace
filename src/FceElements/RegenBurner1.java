package FceElements;

import basic.*;
import mvUtils.display.*;
import mvUtils.jsp.JSPComboBox;
import mvUtils.jsp.JSPConnection;

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
 * Date: 2/25/13
 * Time: 3:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class RegenBurner1 {
    public enum Restriction {
        NOTREADY("Not Evaluated"),
        BYHEAT("Restricted By Heat Available"),
        BYTEMP("Restricted By Flue Temperature"),
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

    enum Mode {AIRTEMP, FUELTEMP, FLUEEXITTEMP, FLUEPERCENT};
    Mode calculationMode = Mode.AIRTEMP;
    Vector<Fuel> fuelList;
    double excessAir = 0.1; // excess air Fraction
    double regenFract = 0.85;
    double regenEff = 0.9;
//    double deltaTFlueAir = 50; // deltat between flue temp and max fluid temp

    double flueInTemp = 1200, flueOutTemp = 200;
    double maxFluidTemp;
    boolean bAirHeated = true, bFuelHeated = false;

    double fluidInTemp = 30;
    double airMaxTemp = fluidInTemp, fuelMaxTemp = fluidInTemp;
    Restriction airTRestrict = Restriction.NOTREADY, fuelTRestrict = Restriction.NOTREADY;

    InputControl control;
    JSPConnection jspConnection;
    Fuel selFuel;
    FuelFiring fuelFiring;
    FlueCompoAndQty unitFlueIn, unitFlueOut;  // flue per unit fuel
    double unitAir; // air per unit fuel including excess air
    boolean fuelOK = false;
    double adiaFlameT;

    public RegenBurner1(Vector<Fuel> fuelList, JSPConnection jspConnection, InputControl control) {
        this.fuelList = new Vector<Fuel>(fuelList); // does it create a copy or uses original elements?
        this.jspConnection = jspConnection;
        this.control = control;
        init();
    }

    JSPComboBox<Fuel> coBFuel;
    NumberTextField ntExcessAir, ntRegenFract, ntRegenEff;
//    NumberTextField ntDeltaTFlueAir;
    NumberTextField ntFlueInTemp, ntFlueOutTemp;
    NumberTextField ntFluidInTemp;
    NumberTextField ntAirMaxTemp, ntFuelMaxTemp;
    NumberLabel nlAdiaFlameT;
    String strNotBalanced = "<html><h3><font color='red'>WARNING! Data in NOT Balanced</h3></html>";
    String strBalanced = "<html><h3><font color='green'>Data is Balanced</h3></html>";
    String strNotEval = "<html><font color='red'>Not Evaluated</html>";
    String strFlueTempRestriction  = "<html><font color='red'>Restricted By Flue Temperature</html>";
    String strBlank = "";
    JLabel jlDataBalanced = new JLabel(strNotBalanced);

    JCheckBox chkBAirHeated, chkBFuelHeated;
    JLabel jlAirRestrict;
    JLabel jlFuelRestrict;
    JComboBox <Restriction> coBAirRestrict;
    JComboBox <Restriction> coBFuelRestrict;

    JButton jbFindFlueExhPercent,jbFindAirTemp, jbFindFuelTemp, jbFindFlueExitTemp;


    void init() {
        FuelAction fuelAction = new FuelAction();
        ChkBoxAction chkBoxAction = new ChkBoxAction();

        ActionListener buttonListener = new ButtonAction();
        ParamsChanged paramsChanged = new ParamsChanged();

        jbFindAirTemp = new JButton("Find Air Temperature");
        jbFindAirTemp.addActionListener(buttonListener);
        jbFindFlueExitTemp = new JButton("Find Flue Exit temp");
        jbFindFlueExitTemp.addActionListener(buttonListener);
        jbFindFlueExhPercent = new JButton("Find Flue Exh. %");
        jbFindFlueExhPercent.addActionListener(buttonListener);
        jbFindFuelTemp = new JButton("Find Fuel Temperature");
        jbFindFuelTemp.addActionListener(buttonListener);

        coBFuel = new JSPComboBox<Fuel>(jspConnection, fuelList);
        coBFuel.addActionListener(paramsChanged);
        coBFuel.addActionListener(fuelAction);
        ntExcessAir = new NumberTextField(control, excessAir * 100, 6, false, 0, 200, "###.00", "Excess Air (%)");
        ntExcessAir.addActionListener(paramsChanged);
        ntExcessAir.addFocusListener(paramsChanged);
        ntExcessAir.addActionListener(fuelAction);
        ntRegenFract = new NumberTextField(control, regenFract * 100, 6, false, 0, 100, "###.00", "Regen Flue flow (%)");
        ntRegenFract.addActionListener(paramsChanged);
        ntRegenFract.addFocusListener(paramsChanged);
        ntRegenEff = new NumberTextField(control, regenEff * 100, 6, false, 50, 100, "###.00", "Regen heat Efficiency (%)");
        ntRegenEff.addActionListener(paramsChanged);
        ntRegenEff.addFocusListener(paramsChanged);
//        ntDeltaTFlueAir = new NumberTextField(control, deltaTFlueAir, 6, false, 0, 1000, "#,###", "DeltaT Flue-Air (C)");
//        ntDeltaTFlueAir.addActionListener(paramsChanged);
        ntFlueInTemp = new NumberTextField(control, flueInTemp, 6, false, 100, 2000, "#,###", "Flue Temperature to Regen (C)");
        ntFlueInTemp.addActionListener(paramsChanged);
        ntFlueInTemp.addFocusListener(paramsChanged);
        ntFlueOutTemp = new NumberTextField(control, flueOutTemp, 6, false, 0, 2000, "#,###", "Flue Temperature after Regen (C)");
        ntFlueOutTemp.addActionListener(paramsChanged);
        ntFlueOutTemp.addFocusListener(paramsChanged);
        ntFluidInTemp = new NumberTextField(control, fluidInTemp, 6, false, 0, 1000, "#,###", "Air/Fuel In Temperature to Regen (C)");
        ntFluidInTemp.addActionListener(paramsChanged);
        ntFluidInTemp.addFocusListener(paramsChanged);

        ntAirMaxTemp = new NumberTextField(control, airMaxTemp, 6, false, 0, 2000, "#,###", "Air Temperature to Burner (C)");
        ntAirMaxTemp.addActionListener(paramsChanged);
        ntAirMaxTemp.addFocusListener(paramsChanged);
        ntFuelMaxTemp = new NumberTextField(control, fuelMaxTemp, 6, false, 0, 2000, "#,###", "fuel Temperature to Burner (C)");
        ntFuelMaxTemp.addActionListener(paramsChanged);
        ntFuelMaxTemp.addFocusListener(paramsChanged);

        chkBAirHeated = new JCheckBox("Air Heated in Regen");
        chkBAirHeated.addActionListener(chkBoxAction);
        chkBFuelHeated = new JCheckBox("Fuel Heated in Regen");
        chkBFuelHeated.addActionListener(chkBoxAction);
        jlAirRestrict = new JLabel(strBlank);
        jlFuelRestrict = new JLabel(strBlank);

//        coBAirRestrict = new JComboBox<Restriction>(Restriction.values());
//        removeButton(coBAirRestrict);
//        coBFuelRestrict = new JComboBox<Restriction>(Restriction.values());
//        removeButton(coBFuelRestrict);

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
        fuelP.addItem(jlDataBalanced);
        fuelP.addItemPair("Fuel", coBFuel);
        fuelP.addItemPair(ntExcessAir);
        addPairWithButton(fuelP, ntRegenFract, jbFindFlueExhPercent);
//        fuelP.addItemPair(ntDeltaTFlueAir);
        fuelP.addItemPair(ntRegenEff);
        fuelP.addBlank();
        fuelP.addItemPair(ntFlueInTemp);
        addPairWithButton(fuelP, ntFlueOutTemp, jbFindFlueExitTemp);
        fuelP.addBlank();
        fuelP.addItemPair(ntFluidInTemp);
        fuelP.addBlank();
//        fuelP.addItemPair(chkBAirHeated, coBAirRestrict);
        fuelP.addItemPair(chkBAirHeated, jlAirRestrict);
        addPairWithButton(fuelP, ntAirMaxTemp, jbFindAirTemp);
//        fuelP.addItemPair(chkBFuelHeated, coBFuelRestrict);
        fuelP.addItemPair(chkBFuelHeated, jlFuelRestrict);
        addPairWithButton(fuelP, ntFuelMaxTemp, jbFindFuelTemp);
        return fuelP;
    }

    void addPairWithButton(MultiPairColPanel p,NumberTextField nt, JButton jb ) {
        JPanel subP = new JPanel();
        subP.add(nt);
        subP.add(jb);
        p.addItemPair(nt.getName(), subP);

    }

    JPanel resultsP() {
        JPanel resultsP = new JPanel(new BorderLayout());
        MultiPairColPanel adiaP = new MultiPairColPanel(200, 60);
        adiaP.addItemPair("Adiabatic Flame Temperature", nlAdiaFlameT);
        resultsP.add(adiaP, BorderLayout.SOUTH);
        return resultsP;
    }

    void updateUI() {
//        coBAirRestrict.setSelectedItem(airTRestrict);
//        coBFuelRestrict.setSelectedItem(fuelTRestrict);
        ntAirMaxTemp.setData(airMaxTemp);
        ntFuelMaxTemp.setData(fuelMaxTemp);
        ntFlueOutTemp.setData(flueOutTemp);
        ntRegenFract.setData(regenFract * 100);
        nlAdiaFlameT.setData(adiaFlameT);
    }

    void takeChkBoxData() {
        bAirHeated = chkBAirHeated.isSelected();
        bFuelHeated = chkBFuelHeated.isSelected();
    }

    void takeFromUI() {
        excessAir = ntExcessAir.getData() / 100;
        regenFract = ntRegenFract.getData() / 100;
        regenEff = ntRegenEff.getData() / 100;
//        deltaTFlueAir = ntDeltaTFlueAir.getData();
        flueInTemp = ntFlueInTemp.getData();
        flueOutTemp = ntFlueOutTemp.getData();
        fluidInTemp = ntFluidInTemp.getData();
//        excessAir = ntExcessAir.getData();
//        excessAir = ntExcessAir.getData();
        airMaxTemp = ntAirMaxTemp.getData();
        fuelMaxTemp = ntFuelMaxTemp.getData();
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

    void markReady(boolean ready) {
        jlDataBalanced.setText(ready ? strBalanced: strNotBalanced);
    }

    void doHeatBalance() {
        takeFromUI();
        if (fuelOK) {
            if (checkDataIntegrity()) {
                FlueComposition flueCompo = fuelFiring.flue;
                maxFluidTemp = flueInTemp; // - deltaTFlueAir;
                unitFlueIn = new FlueCompoAndQty(flueCompo, fuelFiring.actFFratio, maxFluidTemp);
                double flueHeatIn = unitFlueIn.flueHeat;
                unitFlueOut = new FlueCompoAndQty("Flue Out", unitFlueIn);
                unitFlueOut.setTemperature(flueOutTemp);
                double flueHeatOut = unitFlueOut.flueHeat;
                double heatAvail = regenFract * regenEff * (flueHeatIn - flueHeatOut);
                boolean done = true;
                switch (calculationMode) {
                    case FLUEEXITTEMP:
                        flueExitBalance();
                        break;
                    case AIRTEMP:
                        if (bFuelHeated)
                            commonBalance(heatAvail);
                        else
                            airBalance(heatAvail);
                        break;
                    case FUELTEMP:
                        if (bAirHeated)
                            commonBalance(heatAvail);
                        else
                            fuelBalance(heatAvail);

                        break;
                    case FLUEPERCENT:
                        flueFractionBalance();
                        break;
                    default:
                        done = false;
                }
                if (done) {
                    findAdiaFlameT();
                    updateUI();
                    markReady(true);
                } else
                    showMessage("Some problem in balancing choice!");
            }
        }
        else
            showError("Data Entry Error", "Please select Fuel");

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
            jlAirRestrict.setText(strFlueTempRestriction);
//            airTRestrict = Restriction.BYTEMP;
        }
//        else
//            airTRestrict = Restriction.BYHEAT;
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
            jlFuelRestrict.setText(strFlueTempRestriction);
//            fuelTRestrict = Restriction.BYTEMP;
        }
//        else
//            fuelTRestrict = Restriction.BYHEAT;
        fuelMaxTemp = endTemp;
    }

    void flueExitBalance() {
        double unitHeatTransferred = getHeatTransferRequired();
        double flueUnitHeatOut = (unitFlueIn.flueHeat * regenFract - unitHeatTransferred / regenEff) /
                (fuelFiring.actFFratio * regenFract); // for unit flue gas
        flueOutTemp = fuelFiring.flue.tempFromSensHeat(flueUnitHeatOut);
    }

    void flueFractionBalance() {
        double unitHeatTransferred = getHeatTransferRequired();
        regenFract = unitHeatTransferred / regenEff / (unitFlueIn.flueHeat - unitFlueOut.flueHeat);
    }

    /**
     *
     * @return heat transfer corresponding to unit Fuel
     */
    double getHeatTransferRequired() {
        Fluid fluid;
        double heatTransferred = 0;
        if (bAirHeated) {
            boolean onlyAir = true;
            fluid = new FlueComposition(onlyAir);
            double airInHeat = fluid.sensHeatFromTemp(fluidInTemp);
            double airOutHeat = fluid.sensHeatFromTemp(airMaxTemp);
            heatTransferred = unitAir * (airOutHeat - airInHeat);
        }
        if (bFuelHeated) {
            fluid = selFuel;
            double fuelInHeat = fluid.sensHeatFromTemp(fluidInTemp);
            double fuelOutHeat = fluid.sensHeatFromTemp(fuelMaxTemp);
            heatTransferred += (fuelOutHeat - fuelInHeat);
        }
        return heatTransferred;
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

    public void showError(String title, String msg){
        SimpleDialog.showError(control.parent(), title, msg);
    }


    class ButtonAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (src == jbFindAirTemp) {
                calculationMode = Mode.AIRTEMP;
            } else if (src == jbFindFlueExhPercent) {
                calculationMode = Mode.FLUEPERCENT;
            } else if (src == jbFindFlueExitTemp) {
                calculationMode = Mode.FLUEEXITTEMP;
            } else if (src == jbFindFuelTemp) {
                calculationMode = Mode.FUELTEMP;
            }
            doHeatBalance();
        }

    }

    class FuelAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            getFlue();
        }
    }

    class ChkBoxAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            takeChkBoxData();
            if (!bAirHeated) {
                ntAirMaxTemp.setData(fluidInTemp);
                jlAirRestrict.setText(strNotEval);
            }
            else
                jlAirRestrict.setText(strBlank);
            ntAirMaxTemp.setEnabled(bAirHeated);
            if (!bFuelHeated) {
                ntFuelMaxTemp.setData(fluidInTemp);
                jlFuelRestrict.setText(strNotEval);
            }
            else
                jlFuelRestrict.setText(strBlank);
            ntFuelMaxTemp.setEnabled(bFuelHeated);
            jbFindAirTemp.setEnabled(bAirHeated);
            jbFindFuelTemp.setEnabled(bFuelHeated);
            markReady(false);
        }
    }

    class ParamsChanged implements ActionListener, FocusListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (chkBAirHeated.isSelected())
                jlAirRestrict.setText(strBlank);
            if (chkBFuelHeated.isSelected())
                jlFuelRestrict.setText(strBlank);

//            if (src == ntAirMaxTemp) {
//                airTRestrict = Restriction.MANUAL;
//                coBAirRestrict.setSelectedItem(airTRestrict);
//            }
//            if (src == ntFuelMaxTemp) {
//                fuelTRestrict = Restriction.MANUAL;
//                coBFuelRestrict.setSelectedItem(fuelTRestrict);
//            }
            markReady(false);
        }

        @Override
        public void focusGained(FocusEvent e) {

        }

        @Override
        public void focusLost(FocusEvent e) {
            Object src = e.getSource();
            if (src == ntExcessAir)
                getFlue();
            markReady(false);
        }
    }

    boolean checkDataIntegrity() {
        boolean stat = (fluidInTemp < flueInTemp);
        switch (calculationMode) {
            case AIRTEMP:
            case FUELTEMP:
                stat &= (flueOutTemp < flueInTemp);
                break;
            case FLUEEXITTEMP:
                stat &= (bAirHeated && (airMaxTemp < flueInTemp)) || (!bAirHeated);
                stat &= (bFuelHeated && (fuelMaxTemp < flueInTemp)) || (!bFuelHeated);
                break;
            case FLUEPERCENT:
                stat &= (bAirHeated && (airMaxTemp < flueInTemp)) || (!bAirHeated);
                stat &= (bFuelHeated && (fuelMaxTemp < flueInTemp)) || (!bFuelHeated);
                stat &= (flueOutTemp < flueInTemp);
                break;
        }
        if (!stat) {
            showError("Error in Data Entry", "Ensure the following: " +
                        "\n   - Temperature after Regen < Flue Temperature TO Regen "  +
                        "\n   - If Air is Heated in Regen " +
                        "\n           Air Temperature to Burner < Flue Temperature TO Regen" +
                        "\n   - If Fuel is Heated in Regen " +
                        "\n           Fuel Temperature to Burner < Flue Temperature TO Regen" +
                        "\n   - Air/Fuel IN Temperature < Flue Temperature TO Regen"
            );
        }
        return stat;
    }


 }
