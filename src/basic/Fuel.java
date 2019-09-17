package basic;

import display.*;
import mvUtils.jsp.JSPConnection;
import mvUtils.display.*;
import mvUtils.display.TimedMessage;
import mvUtils.math.SPECIAL;
import mvUtils.mvXML.XMLmv;
import mvUtils.math.XYArray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 5/9/12
 * Time: 1:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class Fuel extends Fluid{
    static JSPConnection jspC;
    int id;
    public String name;
    public String units;
    public double calVal;
    public double airFuelRatio;
    public double flueFuelRatio;
    public FlueComposition flueComp;
//    boolean o2EnrichedAir = false;
//    double o2FractInAir = SPECIAL.o2InAir;
    XYArray airHeatCont;
    XYArray flueHeatCont;
    XYArray sensHeat = null;
    XYArray sensHeatAdded= null;
    static boolean inited = false;
    static TwoDTable emissCO2, emissH2O;
    boolean bFlowSharing; // for mixed fuels
    public double baseHshare;  // heat fraction
    double fractAddedFuel;  // added fuel flow
    Fuel baseFuel, addedFuel;
    double baseFuelTemp, addFuelTemp;
    public boolean bMixedFuel = false;
    static InputControl controller;
    static FuelDisplay mixedFuelD;
    static FuelDisplay baseFuelD, addFuelD;
    static Vector<Fuel> fuelList;
    public double myFlowShare = 1, myHeatShare = 1;

    public Fuel(String name, String units, double calVal, double airFuelRatio,
                double flueFuelRatio, XYArray sensHeat, FlueComposition flueComp) {
        this.name = name;
        this.units = units;
        this.calVal = calVal;
        this.airFuelRatio = airFuelRatio;
        this.flueFuelRatio = flueFuelRatio;
        this.flueComp = flueComp;
        this.sensHeat = sensHeat;
    }

    public Fuel(Fuel refFuel)  {
        this.name = refFuel.name;
        this.units = refFuel.units;
        this.calVal = refFuel.calVal;
        this.airFuelRatio = refFuel.airFuelRatio;
        this.flueFuelRatio = refFuel.flueFuelRatio;
        this.flueComp = new FlueComposition(refFuel.flueComp);
        this.bMixedFuel = refFuel.bMixedFuel;
        this.id = refFuel.id;
        if (refFuel.sensHeat != null)
            this.sensHeat = new XYArray(refFuel.sensHeat);
        else
            this.sensHeat = null;
    }

    public Fuel(String name, String units, double calVal, double airFuelRatio,
                double flueFuelRatio, FlueComposition flueComp) {
        this(name, units, calVal, airFuelRatio, flueFuelRatio, null, flueComp);
    }

    public Fuel(String name, Fuel baseFuel, double baseFuelTemp, Fuel addedFuel, double addFuelTemp,
                double baseShare, boolean bFlowSharing) throws Exception {
        this.name = name;
        boolean inError = false;
        if (bFlowSharing && !(baseFuel.units.equals(addedFuel.units)))   {
            inError = true;
            throw new Exception("Two Fuels with different Units of flow measurements cannot be mixed based on Flow-Sharing!");
        }
        if (baseFuel == addedFuel)   {
            inError = true;
            throw new Exception("Select two different Fuels to mix!");
        }
        if (!inError) {
            this.bFlowSharing = bFlowSharing;
            this.baseFuel = baseFuel;
            this.addedFuel = addedFuel;
            this.baseFuelTemp = baseFuelTemp;
            this.addFuelTemp = addFuelTemp;
            if (bFlowSharing) {
                this.fractAddedFuel = (1 - baseShare) / baseShare;
                baseFuel.myFlowShare = baseShare;
                addedFuel.myFlowShare = 1- baseShare;
                double totHeat = baseFuel.calVal +  fractAddedFuel * addedFuel.calVal;
                baseFuel.myHeatShare = baseFuel.calVal / totHeat;
                addedFuel.myHeatShare = 1 - baseFuel.myHeatShare;
                this.baseHshare =  baseFuel.myHeatShare;
            }
            else {
                this.baseHshare = baseShare;
                // for 1000 kcal
                double hBaseFuel = baseHshare * 1000;
                double hAddedFuel = 1000 - hBaseFuel;
                double qBaseFuel = hBaseFuel / baseFuel.calVal;
                double qAddedFuel = hAddedFuel / addedFuel.calVal;
                double baseFshare = qBaseFuel / (qBaseFuel + qAddedFuel);
                fractAddedFuel = (1 - baseFshare) / baseFshare;
                baseFuel.myHeatShare = baseShare;
                addedFuel.myHeatShare = 1- baseShare;
                baseFuel.myFlowShare = baseFshare;
                addedFuel.myFlowShare = 1 - baseFshare;
            }
            bMixedFuel = true;
            prepareFuelMix();
        }
    }

    public Fuel(String name, Fuel baseFuel, Fuel addedFuel, double baseShare, boolean bFlowSharing) throws Exception {
        this(name, baseFuel, 0, addedFuel, 0, baseShare, bFlowSharing);
    }

    public Fuel(String name) throws Exception {
        this(name, "m3N", 0, 0, 1, null, new FlueComposition("", 0, 0, 1, 0, 0));
    }

    public Fuel(String xmlStr, boolean itIsXML) throws Exception{
        if (!takeDataFromXML(xmlStr))
            throw new Exception("ERROR: In Fuel Specifications from xml :" + xmlStr);
    }

//    public void changeForEnrichedAir(double o2Fract) {
//        o2FractInAir = o2Fract;
//        o2EnrichedAir = o2FractInAir != SPECIAL.o2InAir;
//
//    }

    protected boolean takeDataFromXML(String xmlStr) {
        boolean retVal = false;
        mvUtils.mvXML.ValAndPos vp;
        vp = mvUtils.mvXML.XMLmv.getTag(xmlStr, "Name", 0);
        name = vp.val;
        if (name.length() >  2) {
            vp = mvUtils.mvXML.XMLmv.getTag(xmlStr, "units", 0);
            units = vp.val;
            vp = mvUtils.mvXML.XMLmv.getTag(xmlStr, "calVal", 0);
            try {
                calVal = Double.valueOf(vp.val);
                vp = mvUtils.mvXML.XMLmv.getTag(xmlStr, "airFuelRatio", 0);
                airFuelRatio = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "flueFuelRatio", 0);
                flueFuelRatio = Double.valueOf(vp.val);
                if (calVal > 0 && airFuelRatio > 0 && flueFuelRatio > 0) {
                    vp = XMLmv.getTag(xmlStr, "sensHeat", 0);
                    if (vp.val.length() > 2)
                        sensHeat = new XYArray(vp.val);
                    vp = XMLmv.getTag(xmlStr, "flueCompo", 0);
                    if (vp.val.length() > 30) {
                        try {
                            flueComp = new FlueComposition("", vp.val) ;
                            retVal = true;
                        } catch (Exception e) {
                            retVal = false;
                        }
                    }
                }
            } catch (NumberFormatException e) {
                retVal = false;
            }
        }
        return retVal;
    }

    public void setID(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Fuel getBaseFuel() {
        return baseFuel;
    }

    public Fuel getAddedFuel() {
        return addedFuel;
    }

    public double getFractAddedFuel() {
        return fractAddedFuel;
    }

    public double getBaseFuelTemp() {
        return baseFuelTemp;
    }

    public double getAddFuelTemp() {
        return addFuelTemp;
    }

    public boolean isbMixedFuel() {
        return bMixedFuel;
    }

    public void setValues(double calVal, double airFuelRatio,
                double flueFuelRatio, FlueComposition flueComp) {
        this.calVal = calVal;
        this.airFuelRatio = airFuelRatio;
        this.flueFuelRatio = flueFuelRatio;
        this.flueComp = flueComp;
    }

    public String fuelSpecInXML() {
        String xmlStr = XMLmv.putTag("Name", name) +
                XMLmv.putTag("units", units) +
                XMLmv.putTag("calVal", calVal) +
                XMLmv.putTag("airFuelRatio", airFuelRatio) +
                XMLmv.putTag("flueFuelRatio", flueFuelRatio);
        if (sensHeat != null)
            xmlStr += XMLmv.putTag("sensHeat", sensHeat.valPairStr());
        xmlStr += XMLmv.putTag("flueCompo", "\n" + flueComp.compoInXML());
        return xmlStr;
    }

    public void setSensibleHeat(String sensHeatPair) {
        XYArray xyA = new XYArray(sensHeatPair);
        if (xyA.arrLen > 0)
            sensHeat = xyA;
        else
            sensHeat = null;
    }

    void deleteSensibleHeat() {
        sensHeat = null;
    }

    public boolean isSensHeatSpecified() {
        return (bMixedFuel || sensHeat != null);
    }

    public boolean isSensHeatSpecified(InputControl control, double temp) {
        if (bMixedFuel)
            return true;
        boolean bFound = !(sensHeat == null);
        controller = control;
        if ( (temp > 0) && bFound) {
            // check if abnormal (ie. sphet > 1.0)
            double hCont = sensHeat.getYat(temp);
            double spHt = hCont / temp;
//            if (spHt > 1) {
//                DecimalFormat fmt = new DecimalFormat("#,##0.000");
//                DecimalFormat tFmt = new DecimalFormat("#,##0");
//                bFound = decide(name, "Sensible Heat at " + tFmt.format(temp) + " is " +
//                        fmt.format(hCont) + " kcal/" + units +
//                        "\nAverage Specific Heat is " + fmt.format(spHt) + " kcal/" + units + "C" +
//                        "\nProceed with this data?");
//            }
        }
        return bFound;
     }

    static JPanel fuelsPanelP;

    public static JPanel mixedFuelPanel(InputControl control, JSPConnection jspConnection, Vector<Fuel> mainFuelList) {
//        if (fuelsPanelP == null) {
            jspC = jspConnection;
            controller = control;
            fuelList = mainFuelList;
            JPanel outerP = new JPanel();
            FramedPanel jp = new FramedPanel(new GridBagLayout());
            jp.setBackground(new JPanel().getBackground());
            GridBagConstraints gbc = new GridBagConstraints();
            Insets ins = new Insets(20, 1, 1, 1);
            gbc.insets = ins;
            gbc.gridwidth = 1;
            gbc.gridx = 0;
            gbc.gridy = 1;
            Font fBold;
            JLabel jL = new JLabel("Properties ");
            fBold = jL.getFont().deriveFont(Font.BOLD);
            jL.setFont(fBold);
            jp.add(jL, gbc);
            gbc.gridx++;
            jL = new JLabel("Base Fuel ");
            jL.setFont(fBold);
            jp.add(jL, gbc);
            gbc.gridx++;
            jL = new JLabel("Added Fuel ");
            jL.setFont(fBold);
            jp.add(jL, gbc);
            gbc.gridx++;
            jL = new JLabel("Mixed Fuel ");
            jL.setFont(fBold);
            jp.add(jL, gbc);
            gbc.gridy++;
            ins.top = 1;
            jL = new JLabel("(Data per unit Base Fuel)");
            jL.setFont(fBold);
            jp.add(jL, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            jp.add(FuelDisplay.rowHeader(), gbc);
            gbc.gridx++;
            Vector <Fuel> existFuels = new Vector<Fuel>();
            for (Fuel f: fuelList)
                if (!f.bMixedFuel)
                    existFuels.add(f);
            baseFuelD = new FuelDisplay(controller, jspC, existFuels);
            jp.add(baseFuelD.fuelData(), gbc);
            gbc.gridx++;
            addFuelD = new FuelDisplay(controller, jspC, existFuels);
            jp.add(addFuelD.fuelData(), gbc);
            gbc.gridx++;
            mixedFuelD = new FuelDisplay(controller, baseFuelD, addFuelD);
            baseFuelD.noteChangeListener(mixedFuelD);
            addFuelD.noteChangeListener(mixedFuelD);
            jp.add(mixedFuelD.fuelData(), gbc);
            gbc.gridy++;
            gbc.gridx = 3;
            jp.add(fuelSavePanel(), gbc);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 4;
            jp.add(mixChoicePanel(controller), gbc);
            outerP.add(jp);
            fuelsPanelP = outerP;
//        }
        return fuelsPanelP;
    }

//    static JComboBox cBmixType = new JComboBox(new String[]{"Heat", "Flow"});
//    static NumberTextField ntBaseShare;

    static JPanel mixChoicePanel(InputControl controller) {
        String sharePrmt = "Share of Base Fuel (%)";
//        ntBaseShare = new NumberTextField(controller, 50, 6, false, 1, 100, "###.00", sharePrmt, false);
        JPanel outerP = new JPanel();
        FramedPanel jp = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        Insets ins = new Insets(2, 1, 2, 1);
        gbc.insets = ins;
        gbc.gridx = 0;
        gbc.gridy = 0;
        Dimension headSize = new Dimension(150, 20);
        JLabel jl = new JLabel("Sharing Mode");
        jl.setPreferredSize(headSize);
        jp.add(jl, gbc);
        gbc.gridx++;
        jp.add(FuelDisplay.cBmixType, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        jl = new JLabel(sharePrmt);
        jl.setPreferredSize(headSize);
        jp.add(jl, gbc);
        gbc.gridx++;
        jp.add(FuelDisplay.ntBaseShare, gbc);
        outerP.add(jp);
        return outerP;
    }

    static JComponent fuelSavePanel() {
        JButton savePB = new JButtonNoPrint("Save Above Mixture");
        savePB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mixedFuelD.dataChanged();
                Fuel fuelMix;
                if (mixedFuelD != null && (mixedFuelD.getFuel() != null)) {
                    mixedFuelD.getSpHtIfRequired();
                    if ((fuelMix = mixedFuelD.getFuel()) != null) {
                        addFuelToList(fuelMix);
                    } else
                        showError("Fuel " + fuelMix.name + " NOT defined!");

                }
            }
        });
        return savePB;
    }

    static JPanel fuelSavePanelXXX() {
        FramedPanel jp = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        JButton savePB = new JButtonNoPrint("Save Above Mixture");
        savePB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mixedFuelD.dataChanged();
                Fuel fuelMix;
                if (mixedFuelD != null) {
                    mixedFuelD.getSpHtIfRequired();
                    if ((fuelMix = mixedFuelD.getFuel()) != null) {
                        addFuelToList(fuelMix);
                    } else
                        showError("Fuel " + fuelMix.name + " NOT defined!");

                }
            }
        });
        jp.add(savePB);
        return jp;
    }

    static boolean addFuelToList(Fuel fuel) {
        boolean bRetVal = false;
        if (fuelList != null) {
            // check if name exists
            String fName = fuel.name.trim();
            boolean bExists = false;
            for (int f = 0; f < fuelList.size(); f++) {
                if (fName.equalsIgnoreCase(fuelList.get(f).name.trim())) {
                    bExists = true;
                    showError("Fuel with name '" + fName + "' already EXIST!");
                    break;
                }
            }
            if (!bExists) {
                fuelList.add(fuel);
                showMessage("Fuel '" + fuel + "' added to List");
                bRetVal = true;
            }
        }
//        baseFuelD.cbFuel.updateUI();
//        addFuelD.cbFuel.updateUI();
        return bRetVal;
    }

    void prepareFuelMix() {
        calVal = baseFuel.calVal + fractAddedFuel * addedFuel.calVal;
        units = baseFuel.units;
        airFuelRatio = baseFuel.airFuelRatio + fractAddedFuel * addedFuel.airFuelRatio;
        flueFuelRatio = baseFuel.flueFuelRatio + fractAddedFuel * addedFuel.flueFuelRatio;
        double fractAddedFlue = (fractAddedFuel * addedFuel.flueFuelRatio) / (baseFuel.flueFuelRatio);//flueFuelRatio;
        flueComp = new FlueComposition("Flue of " + name, baseFuel.flueComp, addedFuel.flueComp, fractAddedFlue);
        sensHeat = baseFuel.sensHeat;
        sensHeatAdded = addedFuel.sensHeat;
    }

    public void noteMixElemSensHeat() {
        sensHeat = baseFuel.sensHeat;
        sensHeatAdded = addedFuel.sensHeat;
    }

    public FlueComposition getFlue() {
        return new FlueComposition(flueComp);
    }

//    public FlueComposition getFlue(double excessAirFract) {
//        if (o2EnrichedAir)
//            return new FlueComposition("Flue of " + name + " with " + excessAirFract * 100 + "% Excess Air", this,
//                    o2FractInAir,
//                    excessAirFract * airFuelRatio / flueFuelRatio);
//        else
//            return new FlueComposition("Flue of " + name + " with " + excessAirFract * 100 + "% Excess Air", flueComp,
//                excessAirFract * airFuelRatio / flueFuelRatio);
//    }

    public double sensHeatFromTemp(double temperature) {
        if (bMixedFuel)
            return sensibleHeat();
        else {
            if (sensHeat == null || sensHeat.arrLen <= 0)
                return 0;
            else
                return sensHeat.getYat(temperature);
        }
    }

    @Override
    public double deltaHeat(double fromTemp, double toTemp) {
        if (bMixedFuel)
            return Double.NaN;
        else {
            if (sensHeat == null || sensHeat.arrLen <= 0)
                return 0;
            else
                return sensHeat.getYat(fromTemp) - sensHeat.getYat(toTemp);
        }
    }

    @Override
    public double tempFromSensHeat(double heat) {
        double retVal = Double.NaN;
        if (!bMixedFuel) {
            if (sensHeat != null)
                retVal = sensHeat.getXat(heat);
        }
        return retVal;
    }

    public double sensibleHeat() {
        if (sensHeat == null || sensHeatAdded == null || sensHeat.arrLen <= 0 || sensHeatAdded.arrLen <= 0)
            return 0;
        else
            return sensHeat.getYat(baseFuelTemp) + fractAddedFuel * sensHeatAdded.getYat(addFuelTemp);
    }

    public void getSpHtData(InputControl controller, Component caller) {
/*
        JDialog dlg = new FuelSpHeatDlg(controller, this);
        dlg.setLocationRelativeTo(caller);
        dlg.setVisible(true);
*/
        OneParameterDialog dlg = new OneParameterDialog(controller, "Specific Heat Data not Available for " + name,
                "Take Sensible Heat", "IGNORE Sensible Heat");
        dlg.setValue("Enter constant Specific Heat (kcal/" + units + ".degC)", 0.01, "##0.00", 0, 10);
        dlg.setLocationRelativeTo(caller);
        dlg.setVisible(true);
        if (dlg.isOk())
            setSensibleHeat("0, 0, 2000, " + dlg.getVal() * 2000);
        else
            deleteSensibleHeat();

    }

    public FuelNameAndFlow baseFuelNameAndFlow(double baseFlow) {
         if (bMixedFuel)
             return new FuelNameAndFlow(baseFuel, baseFuelTemp, baseFlow);
         else
             return null;
     }

     public FuelNameAndFlow addedFuelNameAndFlow(double baseFlow) {
         if (bMixedFuel)
             return new FuelNameAndFlow(addedFuel, addFuelTemp, baseFlow * fractAddedFuel);
         else
             return null;
     }

    @Override
    public String toString() {
        return name;
    }

    MultiPairColPanel displayPan;

    public MultiPairColPanel fuelPanel(InputControl control) {
        FuelDisplay disp = new FuelDisplay(control, this);
        displayPan = disp.fuelDataWithColHeader(true);
        return displayPan;
    }

    public MultiPairColPanel fuelMixDetails() {
        MultiPairColPanel mp = new MultiPairColPanel("Fuel Mix Details", 100, 60);
        mp.addItemPair("Base Fuel", baseFuel.name, true);
        mp.addItemPair("Base Fuel flow units", baseFuel.units, false);
        mp.addItemPair("Base Fuel Temperature (degC)", baseFuelTemp, "#,##0");
        mp.addItemPair("Base Fuel Heat Share (%)", baseHshare * 100, "#,##0.00");

        mp.addItemPair("Added Fuel", addedFuel.name, true);
        mp.addItemPair("Added Fuel flow units", addedFuel.units, false);
        mp.addItemPair("Added Fuel Temperature (degC)", addFuelTemp, "#,##0");
        mp.addItemPair("Added/Base Fuel FLow Ratio (" + addedFuel.units + "/" + baseFuel.units + ")" , fractAddedFuel, "#,##0.00");
        mp.addItemPair("Added Fuel Heat Share (%)", (1 - baseHshare) * 100, "#,##0.00");
        return mp;
    }

    static boolean decide(String title, String msg) {
        int resp = JOptionPane.showConfirmDialog(controller.parent(), msg, title, JOptionPane.YES_NO_OPTION);
        controller.parent().toFront();
        if (resp == JOptionPane.YES_OPTION)
            return true;
        else
            return false;
    }

    static void showError(String msg) {
        if (controller != null) {
            JOptionPane.showMessageDialog(controller.parent(), msg, "ERROR", JOptionPane.ERROR_MESSAGE);
            controller.parent().toFront();
        } else
            debug(msg);
    }

    static void showMessage(String msg) {
        (new TimedMessage("In Furnace data", msg, TimedMessage.INFO, controller.parent(), 3000)).show();
    }

    static void debug(String msg) {
        System.out.println("Fuel: " + msg);
    }


}
