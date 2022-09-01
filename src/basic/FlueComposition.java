package basic;

import mvUtils.display.MultiPairColPanel;
import mvUtils.display.NumberLabel;
import mvUtils.display.TimedMessage;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.math.DoublePoint;
import mvUtils.math.SPECIAL;
import mvUtils.math.XYArray;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 5/10/12
 * Time: 2:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlueComposition extends Fluid {
    static boolean inited = false;
    static TwoDTable emissCO2, emissH2O;
    static XYArray hContCO2, hContH2O, hContN2, hContO2, hContSO2;
    static public XYArray hContAir;
    public double fractCO2 = 0, fractH2O = 0, fractN2 = 0, fractO2 = 0, fractSO2 = 0;
    XYArray flueHcont;
    public double alphaFactor;
    double lastgThick = -1;
    XYArray emmArrCO2, emmArrH2O;
    public String name;
    FlueComposition stoicFlue = null;
    boolean bOnlyAir = false;
    // take care, this is available only in case of o2Enriched air
    double stoicFFratio = 0;
    double effectiveFFratio = 0;

    // property of actual "air' could be o2 enriched

    public FlueComposition(String name, double fractCO2, double fractH2O, double fractN2, double fractO2, double fractSO2)
            throws Exception {
        initStaticData();
        this.name = name;
        this.fractCO2 = fractCO2;
        this.fractH2O = fractH2O;
        this.fractN2 = fractN2;
        this.fractO2 = fractO2;
        this.fractSO2 = fractSO2;
        double tot = fractCO2 + fractH2O + fractN2 + fractSO2 + fractO2;
        if (Math.abs(tot - 1) > 0.01)
            throw (new Exception("ERROR: in Flue Composition sum (" + tot + "%)!"));
        else
            setFlueHcontArr();
        calculateAlphaFactor();
    }

    public FlueComposition(String name, String xmlStr) throws Exception {
        boolean inError = false;
        ValAndPos vp;
        try {
            vp = XMLmv.getTag(xmlStr, "fractCO2", 0);
            fractCO2 = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "fractH2O", 0);
            fractH2O = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "fractSO2", 0);
            fractSO2 = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "fractN2", 0);
            fractN2 = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "fractO2", 0);
            fractO2 = Double.valueOf(vp.val);
            double tot = fractCO2 + fractH2O + fractN2 + fractSO2 + fractO2;
            if (Math.abs(tot - 1) > 0.01)
                inError = true;
        } catch (NumberFormatException e) {
            inError = true;
        }
        if (inError)
            throw (new Exception("ERROR: in Flue Composition numbers"));
    }

    // creates composition with only O2 and N2 as air
    public FlueComposition(boolean bOnlyAir) {
        this.bOnlyAir = bOnlyAir;
        initStaticData();
        name = "Air";
        fractO2 = SPECIAL.o2InAir;
        fractN2 = 1 - fractO2;
//        flueHcont = new XYArray(hContAir);
        setFlueHcontArr();
        calculateAlphaFactor();
    }

    public FlueComposition(boolean bOnlyAir, double o2InAir) {
        this.bOnlyAir = bOnlyAir;
        initStaticData();
        name = "Air";
        fractO2 = o2InAir;
        fractN2 = 1 - fractO2;
//        flueHcont = new XYArray(hContAir);
        setFlueHcontArr();
        calculateAlphaFactor();
    }

    public FlueComposition(FlueComposition refComp) {
        initStaticData();
        this.name = refComp.name;
        this.fractCO2 = refComp.fractCO2;
        this.fractH2O = refComp.fractH2O;
        this.fractN2 = refComp.fractN2;
        this.fractO2 = refComp.fractO2;
        this.fractSO2 = refComp.fractSO2;
        setFlueHcontArr();
        calculateAlphaFactor();
    }
    // addAirFract is air as fraction of flue before addtion of tghis air
    public FlueComposition(String name, FlueComposition flueComp, double addAirFract) {
        initStaticData();
        takeValues(name, flueComp, addAirFract);
    }

    // addAirFract is air as fraction of flue before addtion of tghis air
    public FlueComposition(String name, Fuel fuel, double o2FractInAir, double excessAir) {
        initStaticData();
        takeValues(name, fuel, o2FractInAir, excessAir);
    }

    public FlueComposition(Fuel fuel, double excessAirFract) {
        initStaticData();
        stoicFlue = fuel.flueComp;
        double addAirFract = fuel.airFuelRatio * excessAirFract / fuel.flueFuelRatio;
        takeValues("Flue of " + fuel.name, stoicFlue, addAirFract);
    }

    public FlueComposition(String name, FlueComposition baseFlue, FlueComposition addedFlue, double fractAddedFlue) {
        initStaticData();
        this.name = name;
        double tot = 1 + fractAddedFlue;
        fractCO2 = (baseFlue.fractCO2 + addedFlue.fractCO2 * fractAddedFlue) / tot;
        fractH2O = (baseFlue.fractH2O + addedFlue.fractH2O * fractAddedFlue) / tot;
        fractN2 = (baseFlue.fractN2 + addedFlue.fractN2 * fractAddedFlue) / tot;
        fractO2 = (baseFlue.fractO2 + addedFlue.fractO2 * fractAddedFlue) / tot;
        fractSO2 = (baseFlue.fractSO2 + addedFlue.fractSO2 * fractAddedFlue) / tot;
        setFlueHcontArr();
        calculateAlphaFactor();
    }

    boolean setComposition(double fractCO2, double fractH2O, double fractN2, double fractO2, double fractSO2) {
        boolean retVal = true;
        this.fractCO2 = fractCO2;
        this.fractH2O = fractH2O;
        this.fractN2 = fractN2;
        this.fractO2 = fractO2;
        this.fractSO2 = fractSO2;
        double tot = fractCO2 + fractH2O + fractN2 + fractSO2 + fractO2;
        if (Math.abs(tot - 1) > 0.01)
            retVal = false;
        else    {
            setFlueHcontArr();
            calculateAlphaFactor();
        }
        return retVal;
    }

    public String compoInXML() {
        String xmlStr = XMLmv.putTag("fractCO2", fractCO2) +
                XMLmv.putTag("fractH2O", fractH2O) +
                XMLmv.putTag("fractSO2", fractSO2) +
                XMLmv.putTag("fractN2", fractN2) +
                XMLmv.putTag("fractO2", fractO2) ;
        return xmlStr;
    }

    // addAirFract is air as fraction of flue before addtion of tghis air
    void takeValues(String name, FlueComposition flueComp, double addAirFract){
        this.name = name;
        double factor = 1 + addAirFract;
        this.fractCO2 = flueComp.fractCO2 / factor;
        this.fractH2O = flueComp.fractH2O / factor;
        this.fractN2 = (flueComp.fractN2 + addAirFract * (1 - SPECIAL.o2InAir)) / factor;
        this.fractO2 = (flueComp.fractO2 + addAirFract * SPECIAL.o2InAir) / factor;
        this.fractSO2 = flueComp.fractSO2 / factor;
        this.stoicFlue = flueComp;
        setFlueHcontArr();
        calculateAlphaFactor();
    }

    void takeValues(String name, Fuel fuel, double o2FactInAir, double excessAir){
        this.name = name;
        FlueComposition flueCompWithStdAir = fuel.flueComp;
        double ff1 = fuel.flueFuelRatio;
        double af1 = fuel.airFuelRatio;
        double af2 = af1 * SPECIAL.o2InAir / o2FactInAir;
        // for unit fuel
        double co2 = flueCompWithStdAir.fractCO2 * ff1;
        double h2o = flueCompWithStdAir.fractH2O * ff1;
        double so2 = flueCompWithStdAir.fractSO2 * ff1;
        double o2 = flueCompWithStdAir.fractO2 * ff1;
        double n2 = flueCompWithStdAir.fractN2 * ff1;
        double n2FromFuel = n2 - af1 * (1- SPECIAL.o2InAir);
        double nowN2 = n2FromFuel + af2 * (1 - o2FactInAir);
        double ff2 = ff1 - n2 + nowN2;
        this.fractCO2 = co2 / ff2;
        this.fractH2O = h2o / ff2;
        this.fractSO2 = so2 / ff2;
        this.fractO2 = o2 / ff2;
        this.fractN2 = nowN2 / ff2;
        try {
            this.stoicFlue = new FlueComposition("Stoic " + name, fractCO2, fractH2O, fractN2, fractO2, fractSO2);
        }
        catch (Exception e) {
            showMessage("Some Error in creating stoicFlue in #204", 3000);
        }
        stoicFFratio = ff2;
        effectiveFFratio = ff2 + af2 * excessAir;
        double factor = (ff2 + excessAir * af2) / ff2;
        fractCO2 = co2 / effectiveFFratio;
        fractH2O = h2o / effectiveFFratio;
        fractSO2 = so2 / effectiveFFratio;
        fractN2 = (nowN2 + excessAir * af2 * (1 - o2FactInAir)) / effectiveFFratio;
        fractO2 = (o2 + excessAir * af2 * o2FactInAir) / effectiveFFratio;
        setFlueHcontArr();
        calculateAlphaFactor();
    }

    public double getDilutionAir(double flueQty, double flueTemp, double tempReqd, double airTemp) {
        double addAir = 0;
        if (tempReqd < flueTemp && tempReqd > airTemp) {
            double heatToAbsorb =  flueQty * (sensHeatFromTemp(flueTemp) - sensHeatFromTemp(tempReqd));
            double airUnitCap = airUnitHeat(tempReqd) - airUnitHeat(airTemp);
            addAir = heatToAbsorb / airUnitCap;
        }
        return addAir;
    }

    void calculateAlphaFactor()  {
        double factorB = fractCO2 + fractH2O;
        double factorA = 1 - fractCO2/ factorB;
        alphaFactor = 1 + 0.25 * factorB / (0.11 + factorB) * factorA * Math.log(factorA);
    }

    static void setAirHcontArr() {
        setAirHcontArr(SPECIAL.o2InAir);
    }

    static void setAirHcontArr(double o2Fraction) {
        hContAir = new XYArray();
        double temp;
        double hO2;
        double hN2;
        double hC;
        // take N2 as base for temperature points
        for (int t = 0; t < hContN2.arrLen; t++) {
            temp = hContN2.getXat(t);
            hO2 = hContO2.getYat(temp);
            hN2 = hContN2.getYat(temp);
            hC = o2Fraction * hO2 + (1 - o2Fraction) * hN2;
            hContAir.add(new DoublePoint(temp, hC));
        }
    }

    void setFlueHcontArr() {
        flueHcont = new XYArray();
        double temp;
        // take N2 as base for temperature points
        for (int t = 0; t < hContN2.arrLen; t++) {
            temp = hContN2.getXat(t);
            flueHcont.add(new DoublePoint(temp, heatContent(temp)));
        }
    }

    public double sensHeatFromTemp(double temperature) {
        return flueHcont.getYat(temperature);
    }

    @Override
    public double deltaHeat(double fromTemp, double toTemp) {
        return flueHcont.getYat(fromTemp) - flueHcont.getYat(toTemp);
    }

    public double tempFromSensHeat(double heat) {
        return flueHcont.getXat(heat);
    }

    public double airSensHeatFromTemp(double temperature)  {
        return hContAir.getYat(temperature);
    }

    public double airTempFromSensHeat(double heat) {
        return hContAir.getXat(heat);
    }

    public MultiPairColPanel mpFlueCompo(String name) {
        int valLabelW = 80;
        int labelWidth = 200;
        int labW = labelWidth + 100;
        MultiPairColPanel mp = new MultiPairColPanel("Composition of " + name , labW, valLabelW);
        NumberLabel nlCO2, nlH2O, nlSO2, nlO2, nlN2;
        nlCO2 = new NumberLabel(fractCO2 * 100, valLabelW, "#,##0.00");
        nlH2O = new NumberLabel(fractH2O * 100, valLabelW, "#,##0.00");
        nlSO2 = new NumberLabel(fractSO2 * 100, valLabelW, "#,##0.00");
        nlO2 = new NumberLabel(fractO2 * 100, valLabelW, "#,##0.00");
        nlN2 = new NumberLabel(fractN2 * 100, valLabelW, "#,##0.00");

        mp.addItemPair("CO2 (%)", nlCO2, false);
        mp.addItemPair("H2O (%)", nlH2O, false);
        mp.addItemPair("SO2 (%)", nlSO2, false);
        mp.addItemPair("O2 (%)", nlO2, false);
        mp.addItemPair("N2 (%)", nlN2, false);

        return mp;
    }

    private double heatContent(double temp) {
        double hCO2 = hContCO2.getYat(temp);
        double hH2O = hContH2O.getYat(temp);
        double hSO2 = hContSO2.getYat(temp);
        double hO2 = hContO2.getYat(temp);
        double hN2 = hContN2.getYat(temp);
        return (hCO2 * fractCO2 + hH2O * fractH2O + hSO2 * fractSO2 + hO2 * fractO2 + hN2 * fractN2);
    }

    public double stoicHcontent(double temp) {
        if (stoicFlue == null)
            return sensHeatFromTemp(temp);
        else {
            double hCO2 = hContCO2.getYat(temp);
            double hH2O = hContH2O.getYat(temp);
            double hSO2 = hContSO2.getYat(temp);
            double hO2 = hContO2.getYat(temp);
            double hN2 = hContN2.getYat(temp);
            return (hCO2 * stoicFlue.fractCO2 + hH2O * stoicFlue.fractH2O +
                    hSO2 * stoicFlue.fractSO2 + hO2 * stoicFlue.fractO2 + hN2 * stoicFlue.fractN2);
        }
    }

    void initStaticData() {
        if (!inited) {
            initCO2Emiss();
            initH2OEmiss();
            initElemHcont();
            inited = true;
        }
    }

    void initElemHcont() {
        initHcontCO2();
        initHcontH2O();
        initHcontSO2();
        initHcontO2();
        initHcontN2();
        setAirHcontArr();
//        initHcontAir();
    }

    void initCO2Emiss() {
        double[] colHead = new double[23];
        double val = 0;
        for (int c = 0; c < 23; c++) {
            colHead[c] = val;
            val += 100;
        }
        emissCO2 = new TwoDTable(colHead, 17, true, true, false, true);
        emissCO2.addRow(0.01, new double[]{4.83, 4.83, 4.83, 4.79, 4.97, 5.24, 5.33, 5.26, 5.10, 4.79, 4.41, 4.04, 3.69, 3.34, 3.04, 2.76, 2.50, 2.25, 2.03, 1.83, 1.64, 1.47, 1.31});
        emissCO2.addRow(0.012, new double[]{5.17, 5.17, 5.17, 5.12, 5.40, 5.67, 5.77, 5.72, 5.51, 5.18, 4.78, 4.39, 3.99, 3.63, 3.31, 3.00, 2.71, 2.44, 2.19, 2.00, 1.77, 1.61, 1.44});
        emissCO2.addRow(0.015, new double[]{5.66, 5.66, 5.66, 5.57, 5.82, 6.13, 6.30, 6.25, 6.00, 5.63, 5.22, 4.81, 4.40, 4.01, 3.66, 3.33, 3.03, 2.75, 2.50, 2.26, 2.03, 1.83, 1.65});
        emissCO2.addRow(0.020, new double[]{6.17, 6.17, 6.17, 6.15, 6.32, 6.86, 6.96, 6.82, 6.69, 6.30, 5.88, 5.42, 5.03, 4.63, 4.24, 3.86, 3.49, 3.15, 2.85, 2.58, 2.34, 2.12, 1.91});
        emissCO2.addRow(0.030, new double[]{6.99, 6.99, 6.99, 6.99, 7.40, 7.76, 7.90, 7.88, 7.68, 7.26, 6.78, 6.30, 5.83, 5.35, 4.89, 4.47, 4.09, 3.72, 3.40, 3.10, 2.82, 2.57, 2.33});
        emissCO2.addRow(0.040, new double[]{7.61, 7.61, 7.61, 7.62, 8.03, 8.36, 8.50, 8.50, 8.35, 7.90, 7.40, 6.90, 6.39, 5.84, 5.38, 4.95, 4.54, 4.15, 3.80, 3.48, 3.19, 2.92, 2.65});
        emissCO2.addRow(0.060, new double[]{8.64, 8.64, 8.64, 8.65, 9.06, 9.47, 9.64, 9.65, 9.53, 9.17, 8.64, 8.04, 7.47, 6.92, 6.42, 5.88, 5.41, 4.97, 4.56, 4.19, 3.85, 3.53, 3.23});
        emissCO2.addRow(0.080, new double[]{9.32, 9.32, 9.32, 9.32, 9.72, 10.15, 10.39, 10.46, 10.36, 9.96, 9.42, 8.96, 8.25, 7.64, 7.10, 6.63, 6.13, 5.62, 5.16, 4.75, 4.37, 4.01, 3.68});
        emissCO2.addRow(0.100, new double[]{9.83, 9.83, 9.83, 9.93, 10.32, 10.70, 11.00, 11.15, 11.14, 10.88, 10.28, 9.62, 8.97, 8.33, 7.74, 7.19, 6.66, 6.14, 5.64, 5.21, 4.80, 4.43, 4.07});
        emissCO2.addRow(0.120, new double[]{10.55, 10.55, 10.55, 10.38, 10.60, 11.10, 11.67, 11.90, 11.90, 11.56, 10.98, 10.40, 9.73, 9.00, 8.32, 7.72, 7.15, 6.60, 6.08, 5.58, 5.14, 4.75, 4.36});
        emissCO2.addRow(0.150, new double[]{11.20, 11.20, 11.20, 11.02, 11.29, 11.82, 12.25, 12.48, 12.53, 12.33, 11.79, 11.02, 10.34, 9.65, 8.99, 8.32, 7.70, 7.12, 6.57, 6.07, 5.62, 5.18, 4.76});
        emissCO2.addRow(0.200, new double[]{12.08, 12.08, 12.08, 11.80, 12.08, 12.68, 13.13, 13.44, 13.61, 13.42, 12.91, 12.12, 11.38, 10.68, 9.98, 9.33, 8.68, 7.99, 7.39, 6.80, 6.28, 5.79, 5.33});
        emissCO2.addRow(0.300, new double[]{13.07, 13.07, 13.07, 12.91, 13.30, 13.96, 14.48, 14.77, 14.90, 14.80, 14.47, 13.91, 13.15, 12.31, 11.55, 10.77, 10.03, 9.30, 8.61, 7.97, 7.35, 6.80, 6.25});
        emissCO2.addRow(0.400, new double[]{13.90, 13.90, 13.90, 13.70, 14.17, 15.00, 15.60, 16.00, 16.20, 16.20, 15.80, 15.10, 14.24, 13.42, 12.57, 11.78, 10.97, 10.18, 9.40, 8.73, 8.07, 7.46, 6.90});
        emissCO2.addRow(0.600, new double[]{15.30, 15.30, 15.30, 15.00, 15.60, 16.50, 17.40, 17.90, 18.10, 17.90, 17.50, 16.80, 15.90, 15.10, 14.15, 13.30, 12.42, 11.56, 10.80, 10.07, 9.38, 8.68, 8.03});
        emissCO2.addRow(0.800, new double[]{16.00, 16.00, 16.00, 15.80, 16.50, 17.40, 18.40, 19.00, 19.40, 19.30, 18.90, 18.20, 17.40, 16.40, 15.50, 14.50, 13.53, 12.62, 11.77, 10.95, 10.20, 9.45, 8.75});
        emissCO2.addRow(1.000, new double[]{16.60, 16.60, 16.60, 16.50, 17.30, 18.10, 19.10, 19.80, 20.40, 20.40, 20.00, 19.30, 18.50, 17.50, 16.50, 15.50, 14.50, 13.53, 12.60, 11.72, 10.90, 10.11, 9.40});

    }

    void initH2OEmiss() {
        double[] colHead = new double[23];
        double val = 0;
        for (int c = 0; c < 23; c++) {
            colHead[c] = val;
            val += 100;
        }
        emissH2O = new TwoDTable(colHead, 17, true, true, false, true);
        emissH2O.addRow(0.01, new double[]{5.03, 5.03, 5.03, 4.48, 3.95, 3.50, 3.06, 2.66, 2.32, 2.02, 1.76, 1.53, 1.33, 1.16, 1.01, 0.88, 0.77, 0.67, 0.58, 0.51, 0.44, 0.38, 0.33});
        emissH2O.addRow(0.012, new double[]{5.78, 5.78, 5.78, 5.14, 4.58, 4.06, 3.61, 3.16, 2.76, 2.42, 2.11, 1.85, 1.62, 1.42, 1.24, 1.09, 0.95, 0.83, 0.73, 0.64, 0.56, 0.49, 0.43});
        emissH2O.addRow(0.015, new double[]{6.65, 6.65, 6.65, 5.91, 5.29, 4.75, 4.28, 3.77, 3.31, 2.91, 2.56, 2.25, 1.98, 1.74, 1.53, 1.35, 1.19, 1.04, 0.92, 0.81, 0.71, 0.63, 0.55});
        emissH2O.addRow(0.020, new double[]{7.93, 7.93, 7.93, 7.10, 6.42, 5.81, 5.30, 4.70, 4.16, 3.68, 3.26, 2.89, 2.56, 2.27, 2.01, 1.78, 1.58, 1.40, 1.24, 1.10, 0.97, 0.86, 0.76});
        emissH2O.addRow(0.030, new double[]{9.98, 9.98, 9.98, 9.08, 8.30, 7.57, 6.89, 6.16, 5.51, 4.92, 4.40, 3.94, 3.52, 3.14, 2.81, 2.51, 2.25, 2.01, 1.80, 1.61, 1.43, 1.28, 1.15});
        emissH2O.addRow(0.040, new double[]{11.60, 11.60, 11.60, 10.64, 9.82, 9.02, 8.25, 7.53, 6.68, 6.01, 5.40, 4.86, 4.37, 3.93, 3.54, 3.18, 2.86, 2.57, 2.32, 2.08, 1.87, 1.69, 1.52});
        emissH2O.addRow(0.060, new double[]{14.50, 14.50, 14.50, 13.50, 12.05, 11.56, 10.61, 9.65, 8.77, 7.97, 7.24, 6.60, 5.98, 5.44, 4.94, 4.49, 4.08, 3.71, 3.37, 3.07, 2.79, 2.53, 2.30});
        emissH2O.addRow(0.080, new double[]{16.70, 16.70, 16.70, 15.60, 14.68, 13.70, 12.65, 11.57, 10.57, 9.67, 8.84, 8.08, 7.38, 6.75, 6.17, 5.64, 5.15, 4.71, 4.31, 3.94, 3.60, 3.29, 3.01});
        emissH2O.addRow(0.100, new double[]{18.40, 18.40, 18.40, 17.30, 16.40, 15.50, 14.36, 13.18, 12.10, 11.10, 10.20, 9.36, 8.60, 7.89, 7.25, 6.66, 6.11, 5.61, 5.15, 4.73, 4.34, 3.99, 3.66});
        emissH2O.addRow(0.120, new double[]{20.20, 20.20, 20.20, 19.00, 18.00, 17.00, 15.80, 14.55, 13.42, 12.37, 11.41, 10.51, 9.69, 8.94, 8.24, 7.60, 7.00, 6.46, 5.95, 5.49, 5.06, 4.66, 4.30});
        emissH2O.addRow(0.150, new double[]{22.10, 22.10, 22.10, 21.00, 20.30, 19.00, 17.80, 16.40, 15.20, 14.08, 13.03, 12.06, 11.16, 10.32, 9.55, 8.84, 8.18, 7.57, 7.00, 6.48, 6.00, 5.55, 5.13});
        emissH2O.addRow(0.200, new double[]{24.90, 24.90, 24.90, 23.80, 22.80, 21.60, 20.30, 18.80, 17.50, 16.30, 15.10, 14.07, 13.09, 12.17, 11.31, 10.52, 9.78, 9.10, 8.46, 7.86, 7.31, 6.80, 6.32});
        emissH2O.addRow(0.300, new double[]{28.80, 28.80, 28.80, 27.70, 26.70, 25.60, 24.10, 22.50, 21.10, 19.70, 18.40, 17.20, 16.00, 14.96, 13.97, 13.04, 12.18, 11.37, 10.62, 9.92, 9.26, 8.65, 8.08});
        emissH2O.addRow(0.400, new double[]{32.00, 32.00, 32.00, 30.90, 29.90, 28.70, 27.10, 25.40, 23.80, 22.30, 20.90, 19.60, 18.40, 17.20, 16.20, 15.20, 14.20, 13.31, 12.48, 11.70, 10.94, 10.28, 9.63});
        emissH2O.addRow(0.600, new double[]{36.40, 36.40, 36.40, 35.60, 34.70, 33.60, 32.10, 30.50, 28.60, 26.90, 25.30, 23.80, 22.50, 21.10, 19.90, 18.80, 17.70, 16.60, 15.70, 14.74, 13.88, 13.07, 12.31});
        emissH2O.addRow(0.800, new double[]{39.50, 39.50, 39.50, 38.70, 37.90, 36.90, 35.50, 33.60, 31.70, 29.80, 28.10, 26.50, 24.90, 23.50, 22.10, 20.80, 19.60, 18.50, 17.40, 16.40, 15.50, 14.58, 13.73});
        emissH2O.addRow(1.000, new double[]{42.50, 42.50, 42.50, 41.50, 40.60, 39.00, 37.70, 35.20, 34.60, 32.60, 30.80, 29.00, 27.30, 25.80, 24.30, 22.90, 21.60, 20.40, 19.20, 18.10, 17.10, 16.10, 15.20});
    }

    void initHcontCO2() {
        hContCO2 = new XYArray(new DoublePoint[]{new DoublePoint(0, 0), new DoublePoint(200, 86.4),
                new DoublePoint(400, 186.8), new DoublePoint(600, 296.4), new DoublePoint(800, 412.8),
                new DoublePoint(1000, 533), new DoublePoint(1200, 656.4), new DoublePoint(1400, 781.2),
                new DoublePoint(1600, 908.8), new DoublePoint(1800, 1036.8), new DoublePoint(2000, 1166.0)});

    }

    void initHcontH2O() {
        hContH2O = new XYArray(new DoublePoint[]{new DoublePoint(0, 0), new DoublePoint(200, 72.6),
                new DoublePoint(400, 149.2), new DoublePoint(600, 231), new DoublePoint(800, 317.6),
                new DoublePoint(1000, 410), new DoublePoint(1200, 506.4), new DoublePoint(1400, 607.6),
                new DoublePoint(1600, 712), new DoublePoint(1800, 819), new DoublePoint(2000, 930)});
    }

    void initHcontSO2() {
        hContSO2 = new XYArray(new DoublePoint[]{new DoublePoint(0, 0), new DoublePoint(200, 86.4),
                new DoublePoint(400, 186.8), new DoublePoint(600, 296.4), new DoublePoint(800, 412.8),
                new DoublePoint(1000, 533), new DoublePoint(1200, 656.4), new DoublePoint(1400, 781.2),
                new DoublePoint(1600, 908.8), new DoublePoint(1800, 1036.8), new DoublePoint(2000, 1166.0)});
    }

    void initHcontO2() {
        hContO2 = new XYArray(new DoublePoint[]{new DoublePoint(0, 0), new DoublePoint(200, 63.8),
                new DoublePoint(400, 131.6), new DoublePoint(600, 203.4), new DoublePoint(800, 277.6),
                new DoublePoint(1000, 354), new DoublePoint(1200, 430.8), new DoublePoint(1400, 509.6),
                new DoublePoint(1600, 588.8), new DoublePoint(1800, 669.6), new DoublePoint(2000, 752)});
    }

    void initHcontN2() {
        hContN2 = new XYArray(new DoublePoint[]{new DoublePoint(0, 0), new DoublePoint(200, 62.4),
                new DoublePoint(400, 126), new DoublePoint(600, 192.6), new DoublePoint(800, 262.4),
                new DoublePoint(1000, 334), new DoublePoint(1200, 408), new DoublePoint(1400, 481.6),
                new DoublePoint(1600, 558.4), new DoublePoint(1800, 635.4), new DoublePoint(2000, 712)});
    }

    void initHcontAir() {
        hContAir = new XYArray(new DoublePoint[]{new DoublePoint(0, 0), new DoublePoint(100, 31.2),
                new DoublePoint(200, 62.6), new DoublePoint(300, 94.5), new DoublePoint(400, 127.2),
                new DoublePoint(500, 160.5), new DoublePoint(600, 194.4), new DoublePoint(700, 234.6),
                new DoublePoint(800, 264.8), new DoublePoint(900, 300.9), new DoublePoint(1000, 337),
                new DoublePoint(2000, 720.4), new DoublePoint(3000, 1085)});
    }

    public double airUnitHeat(double temp) {
        return hContAir.getYat(temp);
    }

    public static XYArray getCO2EmissArray(double gThick, double fractionCO2) throws Exception {
        return emissCO2.getRowArray(gThick * fractionCO2);
    }

    public static XYArray getH2OEmissArray(double gThick, double fractionH2O) throws Exception {
        return emissH2O.getRowArray(gThick * fractionH2O);
    }

    public XYArray  getH2OEmissArray (double gThick) throws Exception {
        return emissH2O.getRowArray(gThick * fractH2O);
    }

    public XYArray  getCO2EmissArray (double gThick) throws Exception {
        return emissCO2.getRowArray(gThick * fractCO2);
    }

    public double effectiveEmissivity(double temp1, double temp2, double gThick) {
        return effectiveEmissivity(temp1, temp2, gThick, true);
    }

    public double alphaGas(double temp1, double temp2, double gThick) {
        return alphaGas(temp1, temp2, gThick, true);
    }

    public double effectiveEmissivity(double temp1, double temp2, double gThick, boolean withCorrection) {
        double effectiveEmiss = alphaGas(temp1, temp2, gThick, true) *
                (temp1 - temp2)/ (SPECIAL.stefenBoltz * (Math.pow(temp1 + 273, 4) - Math.pow(temp2 + 273, 4)));
        return effectiveEmiss;

    }

    public double alphaGas(double temp1, double temp2, double gThick, boolean withCorrection) {
        double retVal = 0;
        try {
            if (gThick != lastgThick) {
                emmArrCO2 = getCO2EmissArray(gThick);
                emmArrH2O = getH2OEmissArray(gThick);
            }
            retVal = alphaGasBasic(temp1, temp2, emmArrCO2, emmArrH2O, withCorrection) * alphaFactor;
        } catch (Exception e) {
            retVal = 0;
        }
        return retVal;
    }

    // alphaFactor has to be applied by the caller
    public static double alphaGasBasic(double temp1, double temp2, XYArray emmXYCO2, XYArray emmXYH2O, boolean withCorrection) {
        double retVal = 1;
        boolean done = false;
        double T1_4, T2_4;
        T1_4 = Math.pow(temp1 + 273, 4);
        T2_4 = Math.pow(temp2 + 273, 4);
        while (!done) {
            if ((temp1 < -273) || (temp2 < -273)) {
                retVal = 1;
                break;
            }
            if (Math.abs(temp1 - temp2) < 0.001) {
                if (temp2 > temp1)
                    temp2 = temp1 + 0.001;
                else
                    temp1 = temp2 + 0.001;
            }
            double eCO2_1,  eH2O_1;
            double eCO2_2, eH2O_2;
            eCO2_1 = emmXYCO2.getYat(temp1) * 0.01;
            eH2O_1 = emmXYH2O.getYat(temp1) * 0.01;
            eCO2_2 = emmXYCO2.getYat(temp2) * 0.01;
            eH2O_2 = emmXYH2O.getYat(temp2) * 0.01;
            double qCO2_1, qH2O_1, qCO2_2, qH2O_2, factorH2O, factorCO2;
//            T1_4 = Math.pow(temp1 + 273, 4);
//            T2_4 = Math.pow(temp2 + 273, 4);
            qCO2_1 = eCO2_1 * SPECIAL.stefenBoltz * T1_4;
            qCO2_2 = eCO2_2 * SPECIAL.stefenBoltz * T2_4;
            qH2O_1 = eH2O_1 * SPECIAL.stefenBoltz * T1_4;
            qH2O_2 = eH2O_2 * SPECIAL.stefenBoltz * T2_4;
            double frctn;
            frctn = (temp1 + 273) / (temp2 + 273);
            if (frctn >= 1) {
                factorH2O = Math.pow(frctn, 0.45);
                factorCO2 = Math.pow(frctn, 0.65);
            }
            else {
                factorH2O = 1;
                factorCO2 = 1;
            }
            double qCO2, qH2O;
            if (withCorrection){
                qCO2 = qCO2_1 - qCO2_2 * factorCO2;
                qH2O = qH2O_1 - qH2O_2 * factorH2O;
            }
            else {
                qCO2 = qCO2_1 - qCO2_2;
                qH2O = qH2O_1 - qH2O_2;
            }

            retVal = (qCO2 + qH2O) / (temp1 - temp2);
            done = true;
        }
//        double effectiveEmiss = retVal * (temp1 - temp2)/ (SPECIAL.stefenBoltz * (T1_4 - T2_4));
        return retVal;
    }

    // alphaFactor has to be applied by the caller
    public static double alphaGasBasic(double temp1, double temp2, XYArray arrCo2, XYArray arrH2O) {
        return(alphaGasBasic(temp1, temp2, arrCo2, arrH2O, true));
    }

    void showMessage(String msg, int forTime) {
        (new TimedMessage("In FlueComposition", msg, TimedMessage.INFO, null, forTime)).show();
    }


}
