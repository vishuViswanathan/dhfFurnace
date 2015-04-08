package performance.stripFce;

import basic.TwoDTable;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.display.FramedPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/9/14
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class ZonalTable {
    int zNum;
    boolean bBot;
    Hashtable<Performance.Params, TwoDTable> perfTables;
    String colHeadName = "Output";
    String rowHeadName = "Width";

//    public ZonalTableOLD(boolean bBot, int zNum, double[] capFactors, double[] widthFactors) {
//        init(bBot, zNum);
//        TwoDTable fuelFlowT = new TwoDTable(capFactors, widthFactors);
//        fuelFlowT.setColAndRowHeadNames(colHeadName, rowHeadName);
//        perfTables.put(Performance.Params.FUELFLOW, fuelFlowT);
//        TwoDTable chTempInT = new TwoDTable(capFactors, widthFactors);
//        chTempInT.setColAndRowHeadNames(colHeadName, rowHeadName);
//        perfTables.put(Performance.Params.CHTEMPIN, chTempInT);
//        TwoDTable chTempOutT = new TwoDTable(capFactors, widthFactors);
//        chTempOutT.setColAndRowHeadNames(colHeadName, rowHeadName);
//        perfTables.put(Performance.Params.CHTEMPOUT,chTempOutT);
//        TwoDTable fceTempT = new TwoDTable(capFactors, widthFactors);
//        fceTempT.setColAndRowHeadNames(colHeadName, rowHeadName);
//        perfTables.put(Performance.Params.FCETEMP,fceTempT);
//        TwoDTable gasTempT = new TwoDTable(capFactors, widthFactors);
//        gasTempT.setColAndRowHeadNames(colHeadName, rowHeadName);
//        perfTables.put(Performance.Params.GASTEMP, gasTempT);
//
//        setFormats();
//    }

    public ZonalTable(boolean bBot, int zNum, double[] capFactors, double[] widthFactors) {
        init(bBot, zNum);
        addToPerfTable(Performance.Params.FUELFLOW, capFactors, widthFactors);
        addToPerfTable(Performance.Params.CHTEMPIN, capFactors, widthFactors);
        addToPerfTable(Performance.Params.CHTEMPOUT, capFactors, widthFactors);
        addToPerfTable(Performance.Params.FCETEMP, capFactors, widthFactors);
        addToPerfTable(Performance.Params.GASTEMP, capFactors, widthFactors);
        addToPerfTable(Performance.Params.COMBUSTIOHEAT, capFactors, widthFactors);
        addToPerfTable(Performance.Params.FUELSENSIBLE, capFactors, widthFactors);
        addToPerfTable(Performance.Params.AIRSENSIBLE, capFactors, widthFactors);
        addToPerfTable(Performance.Params.ZONEFLUEHEAT, capFactors, widthFactors);
        addToPerfTable(Performance.Params.ZONEFUELHEAT, capFactors, widthFactors);
        setFormats();
    }

    void addToPerfTable(Performance.Params param, double[] capFactors, double[] widthFactors ) {
        TwoDTable t = new TwoDTable(capFactors, widthFactors);
        t.setColAndRowHeadNames(colHeadName, rowHeadName);
        perfTables.put(param, t);
    }

    public ZonalTable(boolean bBot, int zNum, String xmlStr) throws Exception{
        init(bBot, zNum);
        String msg = ((bBot) ? "Bot" : "Top") + zNum;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "fuelFlowPT", 0);
        perfTables.put(Performance.Params.FUELFLOW, splTwoDTable(vp.val, msg + ", fuel Flow"));
        vp = XMLmv.getTag(xmlStr, "chtInPT", 0);
        perfTables.put(Performance.Params.CHTEMPIN, splTwoDTable(vp.val, msg + ", chTempIn"));
        vp = XMLmv.getTag(xmlStr, "chtOutPT", 0);
        perfTables.put(Performance.Params.CHTEMPOUT, splTwoDTable(vp.val, msg + ", chTempOut"));
        vp = XMLmv.getTag(xmlStr, "fceTPT", 0);
        perfTables.put(Performance.Params.FCETEMP, splTwoDTable(vp.val, msg + ", fceTemp"));
        vp = XMLmv.getTag(xmlStr, "gasTPT", 0);
        perfTables.put(Performance.Params.GASTEMP, splTwoDTable(vp.val, msg + ", gasTemp"));

        vp = XMLmv.getTag(xmlStr, "combustionH", 0);
        TwoDTable table;
        if (vp.val.length() > 5) {
            perfTables.put(Performance.Params.COMBUSTIOHEAT, splTwoDTable(vp.val, msg + ", CombstnHeat"));
            vp = XMLmv.getTag(xmlStr, "fuelSensible", 0);
            if (vp.val.length() > 5)
                perfTables.put(Performance.Params.FUELSENSIBLE, splTwoDTable(vp.val, msg + ", fuelSensible"));
            vp = XMLmv.getTag(xmlStr, "airSensible", 0);
            if (vp.val.length() > 5)
                perfTables.put(Performance.Params.AIRSENSIBLE, splTwoDTable(vp.val, msg + ", airSensible"));
            vp = XMLmv.getTag(xmlStr, "zoneFlueHeat", 0);
            if (vp.val.length() > 5)
                perfTables.put(Performance.Params.ZONEFLUEHEAT, splTwoDTable(vp.val, msg + ", zoneFlueHeat"));
            vp = XMLmv.getTag(xmlStr, "zoneFuelHeat", 0);
            if (vp.val.length() > 5)
                perfTables.put(Performance.Params.ZONEFUELHEAT, splTwoDTable(vp.val, msg + ", zoneFuelHeat"));
        }
        else {
            TwoDTable refTable = perfTables.get(Performance.Params.FUELFLOW);
            perfTables.put(Performance.Params.COMBUSTIOHEAT, splTwoDTable(refTable, 0));
            perfTables.put(Performance.Params.FUELSENSIBLE, splTwoDTable(refTable, 0));
            perfTables.put(Performance.Params.AIRSENSIBLE, splTwoDTable(refTable, 0));
            perfTables.put(Performance.Params.ZONEFLUEHEAT, splTwoDTable(refTable, 0));
            perfTables.put(Performance.Params.ZONEFUELHEAT, splTwoDTable(refTable, 0));
        }

        setFormats();
    }

    private void init(boolean bBot, int zNum) {
        this.bBot = bBot;
        this.zNum = zNum;
        perfTables = new Hashtable<Performance.Params, TwoDTable>();
    }

    TwoDTable splTwoDTable(String inStr, String cMsg) throws Exception{
        TwoDTable table = new TwoDTable(inStr, cMsg);
        table.setColAndRowHeadNames(colHeadName, rowHeadName);
        return table;
    }

    TwoDTable splTwoDTable(TwoDTable copyBaseFrom, double defaultValue)  {
        return new TwoDTable(copyBaseFrom, defaultValue);

    }

    private void setFormats() {
        String colHeadFmtStr = "0.00";
        String rowHeadFmtStr = "0.000";
        perfTables.get(Performance.Params.FUELFLOW).setFormats(colHeadFmtStr, rowHeadFmtStr, "0.00");
        perfTables.get(Performance.Params.CHTEMPIN).setFormats(colHeadFmtStr, rowHeadFmtStr, "0.00");
        perfTables.get(Performance.Params.CHTEMPOUT).setFormats(colHeadFmtStr, rowHeadFmtStr, "0.00");
        perfTables.get(Performance.Params.FCETEMP).setFormats(colHeadFmtStr, rowHeadFmtStr, "0.00");
        perfTables.get(Performance.Params.GASTEMP).setFormats(colHeadFmtStr, rowHeadFmtStr, "0.00");

        perfTables.get(Performance.Params.COMBUSTIOHEAT).setFormats(colHeadFmtStr, rowHeadFmtStr, "0.00");
        perfTables.get(Performance.Params.FUELSENSIBLE).setFormats(colHeadFmtStr, rowHeadFmtStr, "0.00");
        perfTables.get(Performance.Params.AIRSENSIBLE).setFormats(colHeadFmtStr, rowHeadFmtStr, "0.00");
        perfTables.get(Performance.Params.ZONEFLUEHEAT).setFormats(colHeadFmtStr, rowHeadFmtStr, "0.00");
        perfTables.get(Performance.Params.ZONEFUELHEAT).setFormats(colHeadFmtStr, rowHeadFmtStr, "0.00");
     }

    public boolean addToTable(double stripWidth, double outputFactor, OneZone zone) {
        boolean isOk = false;
        double rowHead = stripWidth;
        double colHead = outputFactor;
        if (perfTables.get(Performance.Params.FUELFLOW).setOneValue(rowHead, colHead, zone.fuelFlow))
            if (perfTables.get(Performance.Params.CHTEMPIN).setOneValue(rowHead, colHead, zone.stripTempIn))
                if (perfTables.get(Performance.Params.CHTEMPOUT).setOneValue(rowHead, colHead, zone.stripTempOut))
                    if (perfTables.get(Performance.Params.FCETEMP).setOneValue(rowHead, colHead, zone.fceTemp))
                        if (perfTables.get(Performance.Params.GASTEMP).setOneValue(rowHead, colHead, zone.gasTemp))

                            if (perfTables.get(Performance.Params.COMBUSTIOHEAT).setOneValue(rowHead, colHead, zone.fuelCombustionHeat))
                                if (perfTables.get(Performance.Params.FUELSENSIBLE).setOneValue(rowHead, colHead, zone.fuelSensibleHeat))
                                    if (perfTables.get(Performance.Params.AIRSENSIBLE).setOneValue(rowHead, colHead, zone.airHeat))
                                        if (perfTables.get(Performance.Params.ZONEFLUEHEAT).setOneValue(rowHead, colHead, zone.heatToZoneFlue))
                                            if (perfTables.get(Performance.Params.ZONEFUELHEAT).setOneValue(rowHead, colHead, zone.heatToZoneFlue))
                                                isOk = true;
        return isOk;
    }

    boolean fillInterpolatedZoneData(OneZone zone, double widthFactor, double outputFactor) {
        boolean allOk = true;
        try {
            double fuelFlow = perfTables.get(Performance.Params.FUELFLOW).getData(outputFactor, widthFactor);
            double fceTemp = perfTables.get(Performance.Params.FCETEMP).getData(outputFactor, widthFactor);
            double gasTemp = perfTables.get(Performance.Params.GASTEMP).getData(outputFactor, widthFactor);
            double stripTempIn = perfTables.get(Performance.Params.CHTEMPIN).getData(outputFactor, widthFactor);
            double stripTempOut = perfTables.get(Performance.Params.CHTEMPOUT).getData(outputFactor, widthFactor);

            double combHeat = perfTables.get(Performance.Params.COMBUSTIOHEAT).getData(outputFactor, widthFactor);
            double fuelSensible = perfTables.get(Performance.Params.FUELSENSIBLE).getData(outputFactor, widthFactor);
            double airSensible = perfTables.get(Performance.Params.AIRSENSIBLE).getData(outputFactor, widthFactor);
            double zoneFlueHeat = perfTables.get(Performance.Params.ZONEFLUEHEAT).getData(outputFactor, widthFactor);
            double zoneFuelHeat = perfTables.get(Performance.Params.ZONEFUELHEAT).getData(outputFactor, widthFactor);
            zone.setValues(fuelFlow, fceTemp, gasTemp, stripTempIn, stripTempOut, combHeat, fuelSensible,
                    airSensible, zoneFlueHeat, zoneFuelHeat );
        } catch (Exception e) {
            allOk = false;
        }
        return allOk;
    }

    StringBuilder dataInXML() {
        StringBuilder xmlStr = new StringBuilder();
        addToXML(xmlStr, Performance.Params.FUELFLOW, "fuelFlowPT");
        addToXML(xmlStr, Performance.Params.CHTEMPIN, "chtInPT");
        addToXML(xmlStr, Performance.Params.CHTEMPOUT, "chtOutPT");
        addToXML(xmlStr, Performance.Params.FCETEMP, "fceTPT");
        addToXML(xmlStr, Performance.Params.GASTEMP, "gasTPT");

        addToXML(xmlStr, Performance.Params.COMBUSTIOHEAT, "combustionH");
        addToXML(xmlStr, Performance.Params.FUELSENSIBLE, "fuelSensible");
        addToXML(xmlStr, Performance.Params.AIRSENSIBLE, "airSensible");
        addToXML(xmlStr, Performance.Params.ZONEFLUEHEAT, "zoneFlueHeat");
        addToXML(xmlStr, Performance.Params.ZONEFUELHEAT, "zoneFuelHeat");
        return xmlStr;
    }

    void addToXML(StringBuilder xmlStr, Performance.Params forParam, String tag) {
        TwoDTable table = perfTables.get(forParam);
        xmlStr.append(XMLmv.putTagNew(tag, table.dataForTextFile()));
    }

    public JPanel perfTableP() {
         FramedPanel outerP = new FramedPanel();
         JTabbedPane tabbedPane = new JTabbedPane();
         tabbedPane.addTab("Fuel Flow", null, dataPanel(Performance.Params.FUELFLOW),
                         "Fuel flow #/h");
         tabbedPane.addTab("Ch Temp IN", null, dataPanel(Performance.Params.CHTEMPIN),
                         "Charge Entry Temperature");
         tabbedPane.addTab("Ch Temp OUT", null, dataPanel(Performance.Params.CHTEMPOUT),
                        "Charge Exit Temperature");
         tabbedPane.addTab("Furnace Temp", null, dataPanel(Performance.Params.FCETEMP),
                        "Furnace Temperature");
         tabbedPane.addTab("Gas Temp", null, dataPanel(Performance.Params.GASTEMP),
                        "Gas Temperature");

        tabbedPane.addTab("CombstnH", null, dataPanel(Performance.Params.COMBUSTIOHEAT),
                        "Fuel Combustion Heat");
        tabbedPane.addTab("FuelSensible", null, dataPanel(Performance.Params.FUELSENSIBLE),
                        "Sensible Heat of Fuel");
        tabbedPane.addTab("AirSensible", null, dataPanel(Performance.Params.AIRSENSIBLE),
                        "Sensible Heat of Air");
        tabbedPane.addTab("ZoneFlueHeat", null, dataPanel(Performance.Params.ZONEFLUEHEAT),
                        "Heat to Zonal Flue");
        tabbedPane.addTab("ZoneFuelHeat", null, dataPanel(Performance.Params.ZONEFUELHEAT),
                        "Heat to Zonal Flue");

         outerP.add(tabbedPane);
         return outerP;
     }

     public JPanel dataPanel(Performance.Params forParam) {
         JPanel jp = new JPanel();
         JScrollPane sP = new JScrollPane(perfTables.get(forParam).getTable());
         sP.setPreferredSize(new Dimension(400, 120));
         jp.add(sP);
         return jp;
     }


}
