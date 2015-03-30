package basic;


import display.MultiPairColPanel;
import mvmath.FramedPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 12/14/12
 * Time: 2:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class FuelsAndUsage {
    Hashtable<Fuel, FuelNameAndFlow> normalTable;
    Hashtable<Fuel, FuelNameAndFlow> regenTable;
    Hashtable<Fuel, FuelNameAndFlow> combiTable;

    public FuelsAndUsage() {
        normalTable = new Hashtable<Fuel, FuelNameAndFlow>();
        regenTable = new Hashtable<Fuel, FuelNameAndFlow>();
        combiTable = new Hashtable<Fuel, FuelNameAndFlow>();
    }

//    public FuelsAndUsage(Fuel fuel) {
//        this();
//        normalTable.put(fuel, new FuelNameAndFlow(fuel));
//    }

//    public void addFuel(Fuel fuel) {
//        if (!normalTable.containsKey(fuel))
//            normalTable.put(fuel, new FuelNameAndFlow(fuel));
//    }

//    public void addFuel(Fuel fuel, double fuelTemp, double fuelFlow, double airFlow, double airSensible,
//                        double fuelSensible) {
//        addFuel(fuel, false, fuelTemp, fuelFlow, airFlow, airSensible, fuelSensible);
//    }

    public void addFuel(Fuel fuel, boolean bRegen, double fuelTemp, double fuelFlow, double airFlow,
                        double airSensible, double fuelSensible) {
        if (bRegen) {
            if (!regenTable.containsKey(fuel))
                regenTable.put(fuel, new FuelNameAndFlow(fuel, fuelTemp));
            regenTable.get(fuel).addValues(fuelFlow, airFlow, airSensible, fuelSensible);
        }
        else {
            if (!normalTable.containsKey(fuel))
                normalTable.put(fuel, new FuelNameAndFlow(fuel, fuelTemp));
            normalTable.get(fuel).addValues(fuelFlow, airFlow, airSensible, fuelSensible);
        }
        if (!combiTable.containsKey(fuel))
            combiTable.put(fuel, new FuelNameAndFlow(fuel, fuelTemp));
        combiTable.get(fuel).addValues(fuelFlow, airFlow, airSensible, fuelSensible);

    }

//    public FuelNameAndFlow getFuelAndFlow(Fuel fuel) {
//        return normalTable.get(fuel);
//    }

    Hashtable<Fuel, FuelNameAndFlow> getTable(boolean bRegen) {
        return (bRegen) ? regenTable : normalTable;
    }

    public double totComBustHeat() {
        double val = 0;
        for(Fuel fuel: normalTable.keySet())
            val += normalTable.get(fuel).combustHeat;
        for(Fuel fuel: regenTable.keySet())
            val += regenTable.get(fuel).combustHeat;
        return val;
    }

    public double totAirFlow(boolean bRegen) {
        double val = 0;
        Hashtable<Fuel, FuelNameAndFlow> table = getTable(bRegen);
        for (Fuel fuel : table.keySet())
            val += table.get(fuel).airFlow;
         return val;
    }

    public double totAirFlow() {
        return totAirFlow(false) + totAirFlow(true);
    }

    public double totBurnerHeat() {
        return totComBustHeat() + totFuelSensHeat() + totAirSensHeat();
    }

    public double totFuelSensHeat() {
        return totFuelSensHeat(false) + totFuelSensHeat(true);
    }

    public double totFuelSensHeat(boolean bRegen) {
        double val = 0;
        Hashtable<Fuel, FuelNameAndFlow> table = getTable(bRegen);
        for(Fuel fuel: table.keySet())
            val += table.get(fuel).fuelSensibleHeat;
        return val;
    }

    public double totAirSensHeat() {
        return totAirSensHeat(false) + totAirSensHeat(true);
    }

    public double totAirSensHeat(boolean bRegen) {
        double val = 0;
        Hashtable<Fuel, FuelNameAndFlow> table = getTable(bRegen);
        for(Fuel fuel: table.keySet())
            val += table.get(fuel).airSensibleHeat;
        return val;
    }

    public Fuel[] getFuels() {
        Vector<Fuel> mixed = new Vector<Fuel>();
        Vector<Fuel> unMixed = new Vector<Fuel>();
        for (Fuel fuel: normalTable.keySet()) {
            if (fuel.bMixedFuel)
                mixed.add(fuel);
            else
                unMixed.add(fuel);
        }
        Fuel[] fuels = new Fuel[normalTable.size()];
        int f = 0;
        for (Fuel fuel:mixed)
            fuels[f++] = fuel;
        for (Fuel fuel:unMixed)
            fuels[f++] = fuel;
        return fuels;
    }

   public MultiPairColPanel mpFuelSummary;

    public JPanel getSummaryPanel(String title, boolean bNoRegen, boolean bNoIndivFuel, double airTemp, boolean bWithSummary) {
        FramedPanel outerP = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        MultiPairColPanel mp = new MultiPairColPanel(title, 200, 120);
        int nFuels = normalTable.size();
//        if ((nFuels = table.size()) > 1) {
//            mp.addItemPair("Number of Fuels", nFuels, "", true);
//        }
        int fNum = 0;
        FuelNameAndFlow fAndf;

        for (Fuel fuel: combiTable.keySet()) {
            fAndf = combiTable.get(fuel);
            fNum++;
            mp.addGroup();
            mp.addItemPair("Fuel" + ((nFuels > 1) ? ("#" + fNum): "") + " Name", fAndf.name, true);
            mp.addItemPair("Fuel Flow (" + fAndf.units + "/h)", fAndf.fuelFlow, "#,##0");
            if (fuel.bMixedFuel)
                mp.addItemPair("Fuel Temperature (degC)", "see Fuel Details", false);
            else {
                if (bNoIndivFuel)
                    mp.addItemPair("Fuel Temperature (degC)", fAndf.temperature, "#,##0");
                else
                    mp.addItemPair("Fuel Temperature (degC)", "See Section Data", false);
            }
            mp.addItemPair("Effective Excess Air (%)", fAndf.effExcessAir * 100, "0.00");
            mp.addItemPair("Fuel Sensible Heat (kcal/h)", fAndf.fuelSensibleHeat, "#,##0");
            mp.addItemPair("Combustion Heat (kcal/h)", fAndf.combustHeat, "#,##0");
            mp.addBlank();
        }
        if (bWithSummary) {
            mp.addGroup();
            mp.addItemPair("TOTAL", "", true);
            mp.addItemPair("Air Flow (Nm3/h)", totAirFlow(), "#,##0");
            if (bNoRegen)
                mp.addItemPair("Air Temperature (degC)", airTemp, "#,##0");
            else
                mp.addItemPair("Air Temperature (degC)", "See Section Data", false);
            mp.addItemPair("Air Sensible Heat (kcal/h)", totAirSensHeat(), "#,##0");
            if (nFuels > 1) {
                mp.addItemPair("Total Fuel Sensible Heat (kcal/h)", totFuelSensHeat(), "#,##0");
                mp.addItemPair("Total Fuel Combustion Heat (kcal/h)", totComBustHeat(), "#,##0");
            }
            mp.addBlank();
            mp.addItemPair("Total Heat from Burners (kcal/h)", totBurnerHeat(), "#,##0", true);
        }
        mpFuelSummary = mp;
        outerP.add(mp);
        return outerP;
    }
}
