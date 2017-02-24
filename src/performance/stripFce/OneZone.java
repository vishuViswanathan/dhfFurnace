package performance.stripFce;

import directFiredHeating.DFHFurnace;
import directFiredHeating.FceSection;
import mvUtils.display.NumberLabel;
import display.SizedLabel;
import mvUtils.display.TextLabel;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.display.FramedPanel;

import javax.swing.*;
import java.awt.*;
/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 1/27/14
 * Time: 10:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class OneZone {
    boolean bRecuType = true;
    boolean bBot = false;
    public double fuelFlow;
    public double fceTemp, gasTemp, stripTempIn, stripTempOut;
    public double heatToCharge, heatFromPassingFlue, losses;
    public double heatToZoneFlue;
    public double fuelCombustionHeat, fuelSensibleHeat, airHeat;
    public double fuelHeat; // combustion + (sensibles fuel, air)
    public double netHeatDueToFuel;  // combustion + (sensibles fuel, air)  less flue loss
    Performance performanceOf;
    double unitFuel; // fuel/unit output
    boolean interpolated = false;
    double lossFactor = 1.0;
    FceSection fceSection;

    public OneZone() {
    }

    public OneZone(boolean bBot, boolean bRecuType) {
        this.bBot = bBot;
        this.bRecuType = bRecuType;
    }

    public OneZone(FceSection section) {
        this.bBot = section.botSection;
        this.bRecuType = section.bRecuType;
        this.lossFactor = section.getLossFactor();
    }

    public OneZone(OneZone oneZone) {
        this.bBot = oneZone.bBot;
        this.bRecuType = oneZone.bRecuType;
        this.lossFactor = oneZone.lossFactor;
    }

    public OneZone(DFHFurnace furnace, FceSection section) {
        this(section);
        double atPos = section.getTcPosition();
        setValues(section.secFuelFlow, furnace.getFceTempAt(atPos, bBot), furnace.getGasTempAt(atPos, bBot),
                furnace.getChTempAt(section.secStartPos, bBot),
                furnace.getChTempAt(section.secStartPos + section.sectionLength(), bBot),
                section.combustionHeat, section.fuelSensibleHeat(), section.airSensible, section.heatToSecFlue,
                section.getFuelHeat());
        this.heatToCharge = section.heatToCharge();
        this.heatFromPassingFlue = section.heatFromPassingFlue;
        this.losses = section.getLosses();
        interpolated = false;
        getNetHeatDueToFuel();
     }

    public void setValues(double fuelFlow, double fceTemp, double gasTemp, double stripTempIn,
                   double stripTempOut) {
        this.fuelFlow = fuelFlow;
        this.fceTemp = fceTemp;
        this.gasTemp = gasTemp;
        this.stripTempIn = stripTempIn;
        this.stripTempOut = stripTempOut;
        interpolated = true; //no heat data
        this.heatToCharge = 0;
        this.heatFromPassingFlue = 0;
        this.losses = 0;
    }

    public void setValues(double fuelFlow, double fceTemp, double gasTemp, double stripTempIn,
                   double stripTempOut, double fuelCombustionHeat, double fuelSensible, double airSensible,
                   double zoneFlueHeat, double fuelHeat) {
        this.fuelFlow = fuelFlow;
        this.fceTemp = fceTemp;
        this.gasTemp = gasTemp;
        this.stripTempIn = stripTempIn;
        this.stripTempOut = stripTempOut;
        this.fuelCombustionHeat = fuelCombustionHeat;
        this.fuelSensibleHeat = fuelSensible;
        this.heatToZoneFlue = zoneFlueHeat;
        this.airHeat = airSensible;
        this.fuelHeat = fuelHeat;
        interpolated = true; //no heat data, later updated after adding heat data by the caller
        this.heatToCharge = 0;
        this.heatFromPassingFlue = 0;
        this.losses = 0;
    }

    void getNetHeatDueToFuel() {
        netHeatDueToFuel = heatToCharge + losses - heatFromPassingFlue;
    }

    void setPerformanceOf(Performance performanceOf)  {
        this.performanceOf = performanceOf;
        unitFuel = fuelFlow / performanceOf.getOutput();
    }

    public String dataInXML() {
        String xmlStr = "";
        if (!interpolated) {
            xmlStr = XMLmv.putTag("bRecuTypeP", ((bRecuType) ? "1" : "0"));
            xmlStr += XMLmv.putTag("bBotP", ((bBot) ? "1": "0"));
            xmlStr += XMLmv.putTag("fuelFlowP", fuelFlow);
            xmlStr += XMLmv.putTag("fceTempP", fceTemp);
            xmlStr += XMLmv.putTag("gasTempP", gasTemp);
            xmlStr += XMLmv.putTag("stripTempInP", stripTempIn);
            xmlStr += XMLmv.putTag("stripTempOutP", stripTempOut);
            xmlStr += XMLmv.putTag("heatToCharge", heatToCharge);
            xmlStr += XMLmv.putTag("heatFromPassingFlue", heatFromPassingFlue);
            xmlStr += XMLmv.putTag("losses", losses);
            xmlStr += XMLmv.putTag("lossFactor", lossFactor);

            xmlStr += XMLmv.putTag("combustHeat", fuelCombustionHeat);
            xmlStr += XMLmv.putTag("fuelSensible", fuelSensibleHeat);
            xmlStr += XMLmv.putTag("airSensible", airHeat);
        }
        return xmlStr;
    }

    public boolean  takeDataFromXML(String xmlStr) {
        boolean bRetVal = true;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "bRecuTypeP", 0);
        bRecuType = (vp.val.equals("1"));
        vp = XMLmv.getTag(xmlStr, "bBotP", 0);
        bBot = (vp.val.equals("1"));
        try {
            vp = XMLmv.getTag(xmlStr, "fuelFlowP", 0);
            fuelFlow = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "fceTempP", 0);
            fceTemp = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "gasTempP", 0);
            gasTemp = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "stripTempInP", 0);
            stripTempIn = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "stripTempOutP", 0);
            stripTempOut = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatToCharge", 0);
            heatToCharge = (vp.val.length() > 1) ? Double.valueOf(vp.val): 0;
            vp = XMLmv.getTag(xmlStr, "heatFromPassingFlue", 0);
            heatFromPassingFlue = (vp.val.length() > 1) ? Double.valueOf(vp.val): 0;
            vp = XMLmv.getTag(xmlStr, "losses", 0);
            losses = (vp.val.length() > 1) ? Double.valueOf(vp.val): 0;
            vp = XMLmv.getTag(xmlStr, "lossFactor", 0);
            lossFactor = (vp.val.length() > 0) ? Double.valueOf(vp.val): 0;
            getNetHeatDueToFuel();
            vp = XMLmv.getTag(xmlStr, "combustHeat", 0);
            fuelCombustionHeat = (vp.val.length() > 1) ? Double.valueOf(vp.val): 0;
            vp = XMLmv.getTag(xmlStr, "fuelSensible", 0);
            fuelSensibleHeat = (vp.val.length() > 1) ? Double.valueOf(vp.val): 0;
            vp = XMLmv.getTag(xmlStr, "airSensible", 0);
            airHeat = (vp.val.length() > 1) ? Double.valueOf(vp.val): 0;
            interpolated = false;
        } catch (NumberFormatException e) {
            bRetVal = false;
        }
        return bRetVal;
    }

    static FramedPanel rowHead;
    static Dimension colHeadSize = new Dimension(180, 20);

    static SizedLabel sizedLabel(String name, Dimension d, boolean bold) {
        return new SizedLabel(name, d, bold);
    }

    static SizedLabel sizedLabel(String name, Dimension d) {
        return sizedLabel(name, d, false);
    }

    public static FramedPanel getRowHeader() {
        rowHead = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbcH = new GridBagConstraints();
        SizedLabel sL;
        Insets ins = new Insets(0, 0, 0, 0);
        gbcH.gridx = 0;
        gbcH.gridy = 0;
        gbcH.insets = ins;
        gbcH.weightx = 0.1;
        FramedPanel grpPan = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridx = 0;
        gbcL.gridy = 0;
        sL = sizedLabel("Zone Number", colHeadSize);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("Zone Type", colHeadSize, true);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("" + Performance.Params.FUELFLOW + " (#/h)", colHeadSize, true);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        rowHead.add(grpPan, gbcH);
        gbcH.gridy++;
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        sL = sizedLabel("" + Performance.Params.CHTEMPIN + " (C)", colHeadSize);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("" + Performance.Params.CHTEMPOUT+ " (C)", colHeadSize);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("" + Performance.Params.FCETEMP + " (C)", colHeadSize, true);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("" + Performance.Params.GASTEMP + " (C)", colHeadSize);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;

//        gbcL.gridy++;
//        sL = sizedLabel("" + Performance.Params.COMBUSTIOHEAT + " (kcal/h)", colHeadSize);
//        grpPan.add(sL, gbcL);
//        gbcL.gridy++;
//        sL = sizedLabel("" + Performance.Params.FUELSENSIBLE + " (kcal/h)", colHeadSize);
//        grpPan.add(sL, gbcL);
//        gbcL.gridy++;
//        sL = sizedLabel("" + Performance.Params.AIRSENSIBLE + " (kcal/h)", colHeadSize);
//        grpPan.add(sL, gbcL);
//        gbcL.gridy++;
//        sL = sizedLabel("" + Performance.Params.ZONEFLUEHEAT + " (kcal/h)", colHeadSize);
        gbcL.gridy++;
        sL = sizedLabel("" + Performance.Params.ZONEFUELHEAT + " (kcal/h)", colHeadSize);
        grpPan.add(sL, gbcL);
        gbcL.gridy++;
        sL = sizedLabel("" + Performance.Params.LOSSFACTOR , colHeadSize);
        grpPan.add(sL, gbcL);

        rowHead.add(grpPan, gbcH);
        return rowHead;
    }

    double getNumberParam(Performance.Params param) {
        double retVal = -1;
        switch(param) {
            case LOSSFACTOR:
                retVal = lossFactor;
                break;
            case FCETEMP:
                retVal = fceTemp;
                break;
            case GASTEMP:
                retVal = gasTemp;
                break;
            case CHTEMPIN:
                retVal = stripTempIn;
                break;
            case CHTEMPOUT:
                retVal = stripTempOut;
                break;
            case FUELFLOW:
                retVal = fuelFlow;
                break;

            case COMBUSTIOHEAT:
                retVal = fuelCombustionHeat;
                break;
            case FUELSENSIBLE:
                retVal = fuelSensibleHeat;
                break;
            case AIRSENSIBLE:
                retVal = airHeat;
                break;
            case ZONEFLUEHEAT:
                retVal = heatToZoneFlue;
                break;
            case ZONEFUELHEAT:
                retVal = fuelCombustionHeat + fuelSensibleHeat + airHeat;
                break;
        }
        return retVal;
    }

    JPanel zoneDataPanel(String zoneName) {
        int datW = 60;
        JPanel jp = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        Insets ins = new Insets(0, 0, 0, 0);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = ins;
        gbc.weightx = 0.1;
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridx = 0;
        gbcL.gridy = 0;
        NumberLabel nL;
        FramedPanel grpPan = new FramedPanel(new GridBagLayout());
        Dimension dim = new Dimension(datW, 20);
        TextLabel bTypeL = new TextLabel(zoneName, true);
        bTypeL.setPreferredSize(dim);
        grpPan.add(bTypeL, gbcL);
        gbcL.gridy++;
        if (bRecuType)  {
            bTypeL = new TextLabel(" ", false);
            bTypeL.setPreferredSize(dim);
            grpPan.add(bTypeL, gbcL);
            gbcL.gridy++;
            bTypeL = new TextLabel(" ", false);
            bTypeL.setPreferredSize(dim);
            grpPan.add(bTypeL, gbcL);
            gbcL.gridy++;
        }
        else  {
            bTypeL = new TextLabel("FIRED", false);
            bTypeL.setPreferredSize(dim);
            grpPan.add(bTypeL, gbcL);
            gbcL.gridy++;
            nL = new NumberLabel(getNumberParam(Performance.Params.FUELFLOW), datW, "#,##0.00", true);
            grpPan.add(nL, gbcL);
            gbcL.gridy++;
        }
        gbc.gridy++;
        jp.add(grpPan, gbc);
        gbcL.gridy = 0;
        grpPan = new FramedPanel(new GridBagLayout());
        gbcL.gridy = 0;
        nL = new NumberLabel(getNumberParam(Performance.Params.CHTEMPIN), datW, "#,###");
        grpPan.add(nL, gbcL);
        gbcL.gridy++;
        nL = new NumberLabel(getNumberParam(Performance.Params.CHTEMPOUT), datW, "#,###");
        grpPan.add(nL, gbcL);
        gbcL.gridy++;
        nL = new NumberLabel(getNumberParam(Performance.Params.FCETEMP), datW, "#,###", true);
        grpPan.add(nL, gbcL);
        gbcL.gridy++;
        nL = new NumberLabel(getNumberParam(Performance.Params.GASTEMP), datW, "#,###");
        grpPan.add(nL, gbcL);

//        gbcL.gridy++;
//        nL = new NumberLabel(getNumberParam(Performance.Params.COMBUSTIOHEAT), datW, "0.00E0");
//        grpPan.add(nL, gbcL);
//        gbcL.gridy++;
//        nL = new NumberLabel(getNumberParam(Performance.Params.FUELSENSIBLE), datW, "0.00E0");
//        grpPan.add(nL, gbcL);
//        gbcL.gridy++;
//        nL = new NumberLabel(getNumberParam(Performance.Params.AIRSENSIBLE), datW, "0.00E0");
//        grpPan.add(nL, gbcL);
//        gbcL.gridy++;
//        nL = new NumberLabel(getNumberParam(Performance.Params.ZONEFLUEHEAT), datW, "0.00E0");
        gbcL.gridy++;
        nL = new NumberLabel(getNumberParam(Performance.Params.ZONEFUELHEAT), datW, "0.00E0");
        grpPan.add(nL, gbcL);
        gbcL.gridy++;
        nL = new NumberLabel(getNumberParam(Performance.Params.LOSSFACTOR), datW, "0.00E0");
        grpPan.add(nL, gbcL);
        gbc.gridy++;

        jp.add(grpPan, gbc);
        return jp;
    }
}

