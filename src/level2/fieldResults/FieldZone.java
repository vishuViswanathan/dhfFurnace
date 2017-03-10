package level2.fieldResults;

import basic.*;
import directFiredHeating.FceSection;
import level2.stripDFH.L2DFHFurnace;
import level2.common.L2ParamGroup;
import level2.stripDFH.L2DFHZone;
import level2.common.Tag;
import mvUtils.display.*;
import mvUtils.mvXML.XMLmv;
import performance.stripFce.OneZone;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 18-Mar-15
 * Time: 2:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class FieldZone {
    L2DFHFurnace l2Furnace;
    FceSection sec;
    Fuel zonalFuel;
    FuelFiring zonalFuelFiring;
    int zNum;
    double frFceTemp;
    double frFuelFlow;
    double frAirTemp;
    double frAfRatio;
    static DecimalFormat fmtTemp = new DecimalFormat("0.0");
    static DecimalFormat fmtFuelFlow = new DecimalFormat("0.00");
    boolean bValid = true;
    String errMsg;
    double lossFactor = 1;  // used for adjusting to the field results

    public FieldZone(L2DFHFurnace l2Furnace, FceSection sec) {
        this.l2Furnace = l2Furnace;
        this.sec = sec;
        this.zNum = sec.secNum;
    }

    public FieldZone(L2DFHFurnace l2Furnace, boolean bBot, int zNum) {
        this(l2Furnace, l2Furnace.getOneSection(bBot, zNum));
    }

    public FieldZone(L2DFHFurnace l2Furnace, boolean bBot, int zNum, L2DFHZone oneZone) {
        this(l2Furnace, bBot, zNum);
        takeFromL2Zone(oneZone);
        testValidity();
    }

    void takeFromL2Zone(L2DFHZone oneZone) {
        double fceTemp = oneZone.getValue(L2ParamGroup.Parameter.Temperature, Tag.TagName.PV).floatValue;
        double fuelFlow = oneZone.getValue(L2ParamGroup.Parameter.FuelFlow, Tag.TagName.PV).floatValue;
        double airTemp = oneZone.getValue(L2ParamGroup.Parameter.AirFlow, Tag.TagName.Temperature).floatValue;
        double afRatio;
        if (frFuelFlow > 0)
            afRatio = oneZone.getValue(L2ParamGroup.Parameter.AirFlow, Tag.TagName.PV).floatValue /
                    frFuelFlow;
        else
            afRatio = 1.0;
        setValues(fceTemp, fuelFlow, airTemp, afRatio);
    }

    public void copyTempAtTCtoSection() {
        sec.setTempAtTCLocation(frFceTemp);
    }

    void testValidity() {
        bValid = (sec.bRecuType) ? (frFuelFlow == 0) : (frFuelFlow > 0);
        if (!bValid)
            errMsg = toString() + ": Zone type and Fuel flow (" + frFuelFlow + ") does not match";
        else
            errMsg = "";
    }

    FlowAndTemperature compareResults(FlowAndTemperature passingFlue) {
        zonalFuelFiring = new FuelFiring(l2Furnace.commFuelFiring, false);
        zonalFuelFiring.setTemperatures(frAirTemp);
        double heatToCharge = sec.heatToCharge();  // TODO in field Results comparison, heat to charge is considered equal
        OneZone oneZone = sec.getZonePerfData();
        double calculatedLosses = oneZone.losses;
        // evaluate frLosses
        double calculatedFlueTempOut = sec.getTempFlueOut();
        double calculatedFceTemp = oneZone.fceTemp;
        // assuming same ratio between the flueOutTemp and FceTemp calculated frFlueTempOut
        double frFlueTempOut = (l2Furnace.considerFieldZoneTempForLossCorrection()) ?
                calculatedFlueTempOut / calculatedFceTemp * frFceTemp :
                calculatedFlueTempOut;
//         l2Furnace.logTrace("FieldZone.95: considerFieldZoneTempForLossCorrection = " + l2Furnace.considerFieldZoneTempForLossCorrection());
        double frNetFuelHeat = frFuelFlow * zonalFuelFiring.netUsefulFromFuel(frFlueTempOut, frAirTemp);
        Fluid flue = zonalFuelFiring.flue;
        double frHeatFromPassingFlue = passingFlue.flow *
                flue.deltaHeat(passingFlue.temperature, frFlueTempOut);
        double frLosses = frNetFuelHeat + frHeatFromPassingFlue - heatToCharge;
//        lossFactor = frLosses / calculatedLosses;
//        l2Furnace.logTrace("FieldZone.102: lossFactor original = " + sec.getLossFactor());    // TODO to be removed on RELEASE
        lossFactor = sec.getLossFactor() * (frLosses / calculatedLosses);
        // limit LossFactor
        double minLossCorrectionFactor = l2Furnace.getMinLossCorrectionFactor();
        double maxLossCorrectionFactor = l2Furnace.getMaxLossCorrectionFactor();
        if (lossFactor < minLossCorrectionFactor) {
            l2Furnace.logError("FieldZone.compareResults: " + sec.sectionName() + " lossFactor = " + lossFactor);
            lossFactor = minLossCorrectionFactor;
        }
        else if (lossFactor > maxLossCorrectionFactor) {
            l2Furnace.logError("FieldZone.compareResults: " + sec.sectionName() + " lossFactor = " + lossFactor);
            lossFactor = maxLossCorrectionFactor;
        }

//        l2Furnace.logTrace("lossFactor modified = " + lossFactor);     // TODO to be removed on RELEASE

        passingFlue.flow += frFuelFlow * zonalFuelFiring.unitFlueFlow();
        passingFlue.temperature = frFlueTempOut;
        return passingFlue;
    }

    double flueQtyFromZone() {
        return frFuelFlow * zonalFuelFiring.unitFlueFlow();
    }


    public void setValues(double fceTemp, double fuelFlow, double airTemp, double afRatio) {
        this.frFceTemp = fceTemp;
        this.frFuelFlow = fuelFlow;
        this.frAirTemp = airTemp;
        this.frAfRatio = afRatio;
    }

    public StringBuilder dataInXML() {
        StringBuilder xmlStr = new StringBuilder(XMLmv.putTag("frFceTemp", fmtTemp.format(frFceTemp)));
        xmlStr.append(XMLmv.putTag("frFuelFlow", fmtFuelFlow.format(frFuelFlow)));
        xmlStr.append(XMLmv.putTag("frAirTemp", fmtTemp.format(frAirTemp)));
        xmlStr.append(XMLmv.putTag("frAfRatio", frAfRatio));
        return xmlStr;
    }

    public String toString() {
        return " Field Zone - " + zNum;
    }

    static Dimension colHeadSize = new Dimension(250, 20);

    static Insets headerIns = new Insets(1, 1, 1, 1);

    static class LabelBorder implements Border {
         public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
             g.setColor(Color.LIGHT_GRAY);
             g.drawRect(x, y, width - 1, height - 1);
             //To change body of implemented methods use File | Settings | File Templates.
         }

         public Insets getBorderInsets(Component c) {
             return headerIns;  //To change body of implemented methods use File | Settings | File Templates.
         }

         public boolean isBorderOpaque() {
             return false;  //To change body of implemented methods use File | Settings | File Templates.
         }
    }

    static JLabel sizedLabel(String name, Dimension d) {
        JLabel lab = new JLabel(name);
        lab.setPreferredSize(d);
        lab.setBorder(new LabelBorder());
        return lab;
    }

    public static JPanel getRowHeader() {
        JPanel rowHead = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbcHeader = new GridBagConstraints();
        gbcHeader.gridx = 0;
        gbcHeader.gridy = 0;
        rowHead.add(sizedLabel("Zone Number", colHeadSize), gbcHeader);
        gbcHeader.gridy++;
        rowHead.add(sizedLabel("Zone Temperature i (C)", colHeadSize), gbcHeader);
        gbcHeader.gridy++;
        rowHead.add(sizedLabel("Fuel flow (#/h)", colHeadSize), gbcHeader);
        gbcHeader.gridy++;
        rowHead.add(sizedLabel("Air Temperature (C)", colHeadSize), gbcHeader);
        gbcHeader.gridy++;
        rowHead.add(sizedLabel("\"Air Fuel Ratio (relative to Stoichiometric)", colHeadSize), gbcHeader);
        gbcHeader.gridy++;
        return rowHead;
     }


    NumberTextField ntFrFceTemp;
    NumberTextField ntFrFuelFlow;
    NumberTextField ntFrAirTemp;
    NumberTextField ntFrAfRatio;
    boolean panelInitiated = false;

    JPanel dataPanel(InputControl ipc, boolean bEditable) {
        ntFrFceTemp = new NumberTextField(ipc, frFceTemp, 6, false, 200, 2000, "#,###", "Furnace Temperature (C)");
        ntFrFuelFlow = new NumberTextField(ipc, frFuelFlow, 6, false, 0, 20000, "#,###.00", "Fuel Flow (#/h");
        ntFrAirTemp = new NumberTextField(ipc, frAirTemp, 6, false, 0, 2000, "#,###", "Air Temperature (C)");
        ntFrAfRatio = new NumberTextField(ipc, frAfRatio, 6, false, 0, 100, "#,###.00", "Air Fuel Ratio (relative to Stoichiometric)");
        JPanel detailsPanel = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        detailsPanel.add(new TextLabel("#" + ("" + zNum).trim(), true), gbc);
        gbc.gridy++;
        detailsPanel.add(ntFrFceTemp, gbc);
        gbc.gridy++;
        detailsPanel.add(ntFrFuelFlow, gbc);
        gbc.gridy++;
        detailsPanel.add(ntFrAirTemp, gbc);
        gbc.gridy++;
        detailsPanel.add(ntFrAfRatio, gbc);
        if (!bEditable)
            for (Component c: detailsPanel.getComponents())
                c.setEnabled(false);
        panelInitiated = true;
        return detailsPanel;
    }

    ErrorStatAndMsg takeDataFromUI() {
        if (panelInitiated) {
            if (ntFrFceTemp.inError || ntFrFuelFlow.inError || ntFrAirTemp.inError || ntFrAfRatio.inError)
                return new ErrorStatAndMsg(true, "Data out of range for Zone " + ("" + zNum).trim());
            else {
                frFceTemp = ntFrFceTemp.getData();
                frFuelFlow = ntFrFuelFlow.getData();
                frAirTemp = ntFrAirTemp.getData();
                frAfRatio = ntFrAfRatio.getData();
                return new ErrorStatAndMsg(false, "");
            }
        } else
            return new ErrorStatAndMsg(true, "Data Panel is not initiated for Zone "  + ("" + zNum).trim());

    }
}