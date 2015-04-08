package FceElements.heatExchanger;

import basic.FlueCompoAndQty;
import basic.FlueComposition;
import basic.Fluid;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.NumberLabel;
import mvUtils.display.FramedPanel;
import mvUtils.math.SPECIAL;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 2/14/13
 * Time: 11:48 AM
 * To change this template use File | Settings | File Templates.
 */

// The recuperator is heated fluid in tubes and heating fluid outside
public class Recuperator {
    FlueCompoAndQty flueBefDilu, flueBefRecu, flueAftRecu;
    Fluid heatedFluid;
    double diluAirFlow = 0;
    double diluAirTempIn;
    double maxFlueInTemp;
    double heatedFluidFlow;
    public double heatedTempIn, heatedTempOut;
    double heatedFlHeatIn, heatedFlHeatOut;
    public boolean bInError = true;
    public String errMsg = "";

    boolean bCounter; // counter flow
    HeatExchProps base;
    public boolean bSpecFrozen;  // specification frozen

    /**
     * for heating air
     * @param flueBefDilu Temperature of Heating fluid before dilution
     * @param maxFlueInTemp max temp restriction of flue at Recu
     * @param airFlow flow of air to be heated
     * @param airTempIn entry temperature of air
     * @param airTempOut  exit temperature of air
     */
    public Recuperator(FlueCompoAndQty flueBefDilu, double maxFlueInTemp,
                       double airFlow, double airTempIn, double airTempOut, double coldAirTemp) {
        this(flueBefDilu, maxFlueInTemp, new FlueComposition(true), airFlow, airTempIn, airTempOut, coldAirTemp);
    }

   // create a recuperator balance for an existing recu with calculated performance
    public Recuperator(Recuperator existingRecu, FlueCompoAndQty flueBefDilu, double maxFlueInTemp,
                               double airFlow, double airTempIn, double coldAirTemp)  {
        // dilute the flue if required
        FlueCompoAndQty dilFlue = getDilutedFlue(flueBefDilu, maxFlueInTemp);
        HeatExchProps perf = existingRecu.getPerformance(dilFlue.flow, dilFlue.flueTemp, airFlow, airTempIn);
        this.flueBefDilu = new FlueCompoAndQty("Flue Before Dilution", flueBefDilu);
        this.heatedFluid = existingRecu.heatedFluid;
        this.maxFlueInTemp = maxFlueInTemp;
        this.heatedFluidFlow = airFlow;
        this.heatedTempIn = airTempIn;
        this.heatedTempOut = perf.heatedToutBase;
        this.diluAirTempIn = coldAirTemp;
        prepareBalance();
    }

    public Recuperator(HeatExchProps existingRecu, FlueCompoAndQty flueBefDilu, double maxFlueInTemp, Fluid heatedFluid,
                               double airFlow, double airTempIn, double coldAirTemp)  {
        // dilute the flue if required
        FlueCompoAndQty dilFlue = getDilutedFlue(flueBefDilu, maxFlueInTemp);
        HeatExchProps perf = existingRecu.getPerformance(dilFlue.flow, dilFlue.flueTemp, airFlow, airTempIn);
        this.flueBefDilu = new FlueCompoAndQty("Flue Before Dilution", flueBefDilu);
        this.heatedFluid = heatedFluid;
        this.maxFlueInTemp = maxFlueInTemp;
        this.heatedFluidFlow = airFlow;
        this.heatedTempIn = airTempIn;
        if (perf != null) {
            this.heatedTempOut = perf.heatedToutBase;
            this.diluAirTempIn = coldAirTemp;
            prepareBalance();
        } else {
            errMsg = "Facing some problem in getting recuperator performance.\n" +
                    "Recommend ABORT calculation and try without Existing Recuperator with manual entry of Air preheat temperature";
            bInError = true;
        }
    }

    public Recuperator(FlueCompoAndQty flueBefDilu, double maxFlueInTemp,
                          Fluid heatedFluid, double fluidFlow, double fluidTempIn, double fluidTempOut, double coldAirTemp) {
        this.flueBefDilu = new FlueCompoAndQty("Flue Before Dilution", flueBefDilu);
        this.heatedFluid = heatedFluid;
        this.maxFlueInTemp = maxFlueInTemp;
        this.heatedFluidFlow = fluidFlow;
        this.heatedTempIn = fluidTempIn;
        this.heatedTempOut = fluidTempOut;
        this.diluAirTempIn = coldAirTemp;
        prepareBalance();
    }

    public HeatExchProps getPerformance(double heatingFlow, double heatingTempIn, double heatedFlow, double heatedTin) {
        if (bSpecFrozen)
            return base.getPerformance(flueBefRecu.flueCompo, heatingFlow, heatingTempIn, heatedFluid,heatedFlow, heatedTin);
        else
            return null;
     }

    public HeatExchProps getHeatExchProps(boolean bCounterFlow) {
        return new HeatExchProps(flueBefRecu.flow, heatedFluidFlow, flueBefRecu.flueTemp,  flueAftRecu.flueTemp,
                            heatedTempIn,  heatedTempOut,  (heatedFlHeatOut - heatedFlHeatIn), bCounterFlow);
    }

    void prepareBalance() {
        bInError = false;
        FlueCompoAndQty flue1 = new FlueCompoAndQty("Flue at Recu", flueBefDilu);
        double entryT = Math.min(maxFlueInTemp, flue1.flueTemp);
        if (heatedFluidFlow > 0 && entryT <= heatedTempOut )  {
            bInError = true;
            errMsg = "Flue Entry temp " + SPECIAL.roundToNDecimals(entryT, 0) +
                    " cannot heat  to " + SPECIAL.roundToNDecimals(heatedTempOut, 0);
        }
        if (flue1.flueTemp > maxFlueInTemp)
            diluAirFlow = flue1.diluteWithAir(maxFlueInTemp,  diluAirTempIn);
        flueBefRecu = new FlueCompoAndQty("Flue Before Recu", flue1);
        heatedFlHeatIn = heatedFluidFlow * heatedFluid.sensHeatFromTemp(heatedTempIn);
        heatedFlHeatOut = heatedFluidFlow * heatedFluid.sensHeatFromTemp(heatedTempOut);
        flue1.exchangeHeat(heatedFlHeatOut - heatedFlHeatIn);
        flueAftRecu = new FlueCompoAndQty("Flue After Recu", flue1);
    }

    public static FlueCompoAndQty getDilutedFlue(FlueCompoAndQty befDilution, double maxTemp) {
        FlueCompoAndQty flue1 = new FlueCompoAndQty("Flue at Recu", befDilution);
        if (flue1.flueTemp > maxTemp)
            flue1.diluteWithAir(maxTemp,  30);
        return flue1;
    }

    JPanel recuBalanceP() {
        return recuBalanceP("Flue", "Air");
    }

    public JPanel recuBalanceP(String heatingFluid, String heatedFluid) {
        JPanel innerP = new FramedPanel(new BorderLayout());
        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        jp.add(recuFlueP(heatingFluid), gbc);
        gbc.gridx++;
        jp.add(recuHeatedP(heatedFluid), gbc);
        innerP.add(jp, BorderLayout.CENTER);
        return innerP;
    }

    MultiPairColPanel mpRecuFlue, mpRecuHeated;
    int valLabelW = 80;
    int labelWidth = 200;

    MultiPairColPanel recuFlueP(String fluidName) {
        int labW = labelWidth + 100;
        MultiPairColPanel mp = new MultiPairColPanel(fluidName + " Side Details", labW, valLabelW);
        NumberLabel nlFlueIn, nlTempBeforeDilu, nltempRecuIn, nlTempRecuOut,
                    nlDilAir,nlDiluFlue,
                    nlHeatRecuIn, heatRecuOut;
        nlFlueIn = new NumberLabel(flueBefDilu.flow, valLabelW, "#,###");
        nlTempBeforeDilu = new NumberLabel(flueBefDilu.flueTemp, valLabelW, "#,###");
        nlDilAir = new NumberLabel(diluAirFlow, valLabelW, "#,###");
        nlDiluFlue = new NumberLabel(flueBefRecu.flow, valLabelW, "#,###");
        nltempRecuIn = new NumberLabel(flueBefRecu.flueTemp, valLabelW, "#,###", true);
        nlHeatRecuIn = new NumberLabel(flueBefRecu.flueHeat, valLabelW, "#,###");
        heatRecuOut = new NumberLabel(flueAftRecu.flueHeat, valLabelW, "#,###");
        nlTempRecuOut = new NumberLabel(flueAftRecu.flueTemp, valLabelW, "#,###", true);
        if (diluAirFlow > 0) {
            mp.addItemPair(fluidName + " Flow before Dilution (m3N/h) ", nlFlueIn);
            mp.addItemPair(fluidName + " Temperature before Dilution (C) ", nlTempBeforeDilu);
            mp.addItemPair("Dilution Air Flow (m3N/h)", nlDilAir);
        }
        mp.addItemPair(fluidName + " Flow before Recu (m3N/h)", nlDiluFlue);
        mp.addItemPair(fluidName + " Temperature at Recu (C) ", nltempRecuIn, true);
        mp.addItemPair(fluidName + " Heat before Recu (kcal/h) ", nlHeatRecuIn);
        mp.addItemPair(fluidName + " Heat after Recu (kcal/h) ", heatRecuOut);
        mp.addItemPair(fluidName + " Temperature after Recu (C) ", nlTempRecuOut, true);
        mpRecuFlue = mp;
        return mp;
    }

    MultiPairColPanel recuHeatedP(String fluidName) {
        int labW = labelWidth + 100;
        MultiPairColPanel mp = new MultiPairColPanel(fluidName + " Side Details", labW, valLabelW);
        NumberLabel nlFlow, nltempRecuIn, nlTempRecuOut,
                    nlHeatRecuIn, nlHeatRecuOut;
        nlFlow = new NumberLabel(heatedFluidFlow, valLabelW, "#,###", true);
        nltempRecuIn = new NumberLabel(heatedTempIn, valLabelW, "#,###", true);
        nlTempRecuOut = new NumberLabel(heatedTempOut, valLabelW, "#,###", true);
        nlHeatRecuIn = new NumberLabel(heatedFlHeatIn, valLabelW, "#,###");
        nlHeatRecuOut = new NumberLabel(heatedFlHeatOut, valLabelW, "#,###");
        mp.addItemPair(fluidName + " Flow (m3N/h) ", nlFlow, true);
        mp.addItemPair(fluidName + " Temperatures before Recu (C) ", nltempRecuIn, true);
        mp.addItemPair(fluidName + " Heat before Recu (kcal/h) ", nlHeatRecuIn);
        mp.addItemPair(fluidName + " Heat after Recu (kcal/h) ", nlHeatRecuOut);
        mp.addItemPair(fluidName + " Temperatures after Recu (C) ", nlTempRecuOut, true);
        mpRecuHeated = mp;
        return mp;
    }

    public MultiPairColPanel mpCompoFlueIn(String name) {
        return flueBefDilu.flueCompo.mpFlueCompo(name);
    }

    public MultiPairColPanel mpCompoFlueOut(String name) {
        return flueAftRecu.flueCompo.mpFlueCompo(name);
    }

    public FlueCompoAndQty getFlueAftRecu() {
        return flueAftRecu;
    }

    public MultiPairColPanel getMpRecuFlue() {
        if (mpRecuFlue == null)
            recuBalanceP();
        return mpRecuFlue;
    }

    public MultiPairColPanel getMpRecuHeated() {
        if (mpRecuFlue == null)
            recuBalanceP();
        return mpRecuHeated;
    }
 }
