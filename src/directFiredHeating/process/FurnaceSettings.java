package directFiredHeating.process;

import directFiredHeating.DFHeating;
import directFiredHeating.FceSection;
import mvUtils.display.*;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.math.DoubleRange;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 18-Feb-15
 * Time: 12:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class FurnaceSettings   {
    // @@@@@@@@@@@@@@ REMEMBER to modify backup() and restore(), for any change in
    // data structure
    DFHeating dfHeating;
    String opcIP = "opc.tcp://127.0.0.1:49320";
    DoubleRange[] zoneFuelRange;
    DoubleRange[] zonalFuelLimits; // for data error checking
    public DoubleRange temperatureRange =  new DoubleRange(0, 1600);
    public DoubleRange stripSpeedRange = new DoubleRange(0, 1000);
    DoubleRange totFuelRange;
//    public int speedCheckInterval = 30; // in s
    public String errMsg = "Error reading Furnace Settings :";
    public boolean inError = false;
    double maxTurndown = 10;
    public int fuelCharSteps = 5;
    // @@@@@@@@@@@@@@ REMEMBER to modify backup() and restore(), for any change in
    // data structure

    public FurnaceSettings(DFHeating dfHeating) {
        this.dfHeating = dfHeating;
    }

    public FurnaceSettings(DFHeating dfHeating, Vector<FceSection> activeSections) {
        this(dfHeating);
        fuelCharSteps = 7;
        zoneFuelRange = new DoubleRange[activeSections.size()];
        int i = 0;
        for (FceSection sec: activeSections) {
            DoubleRange oneRange;
            if (sec.bRecuType)
                oneRange = new DoubleRange(0, 0);
            else
                oneRange = new DoubleRange(100/7, 100);
            zoneFuelRange[i++] = oneRange;
        }
    }

    public FurnaceSettings(DFHeating dfHeating, String xmlStr) {
        this(dfHeating);
        takeDataFromXML(xmlStr);
    }

    public DoubleRange getTemperatureRange() {
        return temperatureRange;
    }

    public DoubleRange getFuelFlowLimits(int sec) {
        return zonalFuelLimits[sec];
    }

    public String getOPCip() {
        return opcIP;
    }

    public StatusWithMessage checkIntegrity() {
        StatusWithMessage retVal = new StatusWithMessage("\n");
        Vector<FceSection> activeSections = dfHeating.furnace.getActiveSections(false);
        if (zoneFuelRange.length == activeSections.size()) {
            int i = 0;
            for (FceSection sec : activeSections) {
                DoubleRange oneRange = zoneFuelRange[i];
                if (sec.bRecuType) {
                    if (oneRange.max != 0 || oneRange.min != 0)
                        retVal.addErrorMessage("Recuperative section " + sec.sectionName() + " is shown with fuel settings");
                } else {
                    if (oneRange.max <= 0 || oneRange.min >= oneRange.max)
                        retVal.addErrorMessage("Fired section " + sec.sectionName() + " has some Fuel range anomaly");
                }
                i++;
            }
        }
        else
            retVal.addErrorMessage("Mismatch in Zone count");
        if (retVal.getDataStatus() == DataStat.Status.OK)
            setZonalFlowLimits();
        return retVal;
    }

    void setZonalFlowLimits() {
        int nSec = dfHeating.furnace.getActiveSections(false).size();
        zonalFuelLimits = new DoubleRange[nSec];
        for (int i = 0; i < nSec ; i++) {
            zonalFuelLimits[i] = new DoubleRange(0, zoneFuelRange[i].max * 1.5);
        }
    }
                                                                 
    void setTotalRange() {
        double totMax = 0;
        double totMin  = 0;
        for (DoubleRange aZoneFuelRange : zoneFuelRange) {
            totMax += aZoneFuelRange.max;
            totMin += aZoneFuelRange.min;
        }
        totFuelRange = new DoubleRange(totMin, totMax);
    }

    public DoubleRange[] getZoneFuelRange() {
        return zoneFuelRange;
    }

    public DoubleRange getTotalFuelRange()   { return totFuelRange; }

    public boolean showEditData(boolean bEdit, Window parent)  {
        FceSettingsDlg dlg = new FceSettingsDlg(bEdit);
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
        return dlg.edited;
    }

    boolean takeDataFromXML(String xmlStr) {
        boolean retVal = false;
        ValAndPos vp;
        errMsg = "Furnace Settings - Reading data:";
        try {
            vp = XMLmv.getTag(xmlStr, "opcIP", 0);
            if (vp.val.length() > 5)
                opcIP = vp.val.trim();
            vp = XMLmv.getTag(xmlStr, "fuelCharSteps", 0);
            fuelCharSteps = Integer.valueOf(vp.val);
//            vp = XMLmv.getTag(xmlStr, "speedCheckInterval", 0);
//            if (vp.val.length() > 0)
//                speedCheckInterval = Integer.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "fuelRanges", 0);
            retVal = takeFuelRangesFromXML(vp.val);
            if (retVal) {
                setTotalRange();
                retVal = (checkIntegrity().getDataStatus() == DataStat.Status.OK);
            }
        } catch (NumberFormatException e) {
            errMsg += "Some Number format error";
            inError = true;
        }
        return retVal;
    }

    public StringBuffer dataInXML() {
        StringBuffer xmlStr = new StringBuffer(XMLmv.putTag("opcIP", opcIP));
        xmlStr.append(XMLmv.putTag("fuelCharSteps", "" + fuelCharSteps));
//        xmlStr.append(XMLmv.putTag("speedCheckInterval", "" + speedCheckInterval));
        xmlStr.append(XMLmv.putTag("fuelRanges", fuelRangesInXML()));
        return xmlStr;
    }

    StringBuilder fuelRangesInXML() {
        StringBuilder xmlStr = new StringBuilder(XMLmv.putTag("nFuelRange", "" + zoneFuelRange.length));
        for (int z = 0; z < zoneFuelRange.length; z++)
            xmlStr.append(XMLmv.putTag("zfr#" + ("" + (z + 1)).trim(), oneFuelRange(z).toString()));
        return xmlStr;
    }

    boolean takeFuelRangesFromXML(String xmlStr) throws NumberFormatException {
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "nFuelRange", 0);
        int nFuelRange = Integer.valueOf(vp.val);
        zoneFuelRange = new DoubleRange[nFuelRange];
        for (int z = 0; z < nFuelRange; z++) {
            String tag = "zfr#" + ("" + (z + 1)).trim();
            vp = XMLmv.getTag(xmlStr, tag, vp.endPos);
            if (!noteOneFuelRange(vp.val, z)) {
                errMsg += "Fuel Range for " + tag + " :" + vp.val;
                inError = true;
                break;
            }
        }
        return !inError;
    }

    StringBuffer oneFuelRange(int zNum) {
        DoubleRange maxMin = zoneFuelRange[zNum];
        double max = maxMin.getMax();
        double min = maxMin.getMin();
        StringBuffer xmlStr = new StringBuffer(XMLmv.putTag("max", max));
        xmlStr.append(XMLmv.putTag("td", ((max > 0) && (min > 0)) ? max / min : 1.0));
        return xmlStr;
    }

    boolean noteOneFuelRange(String xmlStr, int zNum) throws NumberFormatException {
        boolean retVal = false;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "max", 0);
        double max = Double.valueOf(vp.val);
        vp = XMLmv.getTag(xmlStr, "td", 0);
        double td = Double.valueOf(vp.val);
        if (td > 0) {
            zoneFuelRange[zNum] = new DoubleRange(max / td, max);
            retVal = true;
        }
        return retVal;
    }

    FurnaceSettings getBackup() {
        FurnaceSettings backup = new FurnaceSettings(dfHeating);
        backup.zoneFuelRange = new DoubleRange[zoneFuelRange.length];
        for (int i = 0; i < zoneFuelRange.length; i++)
            backup.zoneFuelRange[i] = new DoubleRange(zoneFuelRange[i]);
        backup.fuelCharSteps = fuelCharSteps;
//        backup.speedCheckInterval = speedCheckInterval;
        return backup;
    }

    void  restoreFrom(FurnaceSettings from) {
        for (int i = 0; i < zoneFuelRange.length; i++)
            zoneFuelRange[i] = new DoubleRange(from.zoneFuelRange[i]);
        fuelCharSteps = from.fuelCharSteps;
//        speedCheckInterval = from.speedCheckInterval;
    }

    public StatusWithMessage checkFuelFlowInRange(int secNumber, double fuelFlow) {
        return zoneFuelRange[secNumber].checkAStatus(fuelFlow);
    }

    public boolean getOPCServerIP(InputControl ipc) {
        boolean retVal = false;
        OneParameterDialog dlg = new OneParameterDialog(ipc, "IP address of OPC server", true);
        dlg.setValue("IP address of OPC Server:", opcIP, 20);
        dlg.setLocationRelativeTo(ipc.parent());
        dlg.setVisible(true);
        if (dlg.isOk()) {
            opcIP = dlg.getTextVal();
            retVal = true;
        }
        return retVal;
    }

    class FceSettingsDlg extends JDialog implements DataHandler {
        InputControl ipc;
        DataListEditorPanel editorPanel;
        boolean editable = false;
        EditResponse.Response response;
//        NumberTextField ntSpeedCheckInterval;
        NumberTextField ntFuelSegments;
        NumberTextField[] ntRangeMax;
        NumberTextField[] ntRangeMin;
        int nZones;
        boolean edited = false;
        FurnaceSettings backup;

        FceSettingsDlg(boolean editable) {
            this.editable = editable;
            setModal(true);
            backup = getBackup();
            init();
        }

        void init() {
            ipc = dfHeating;
            addWindowListener(new WindowAdapter() {
                 @Override
                 public void windowClosing(WindowEvent e) {
                     response = EditResponse.Response.EXIT;
                     super.windowClosing(e);
                 }
            });
            editorPanel = new DataListEditorPanel("Furnace Fuel Settings", this, true);
            ntFuelSegments = new NumberTextField(ipc, fuelCharSteps, 6, false, 3, 10, "##", "Fuel Characteristics-steps");
//            ntSpeedCheckInterval = new NumberTextField(ipc, speedCheckInterval, 6, true, 10, 60000, "#,##0", "Speed Check Interval (s)");

            editorPanel.addItemPair(ntFuelSegments);
//            editorPanel.addItemPair(ntSpeedCheckInterval);
            editorPanel.addBlank();
            double max, min;
            nZones = dfHeating.furnace.nTopActiveSecs;
            ntRangeMax = new NumberTextField[nZones];
            ntRangeMin = new NumberTextField[nZones];

            for (int z = 0; z < nZones; z++) {
                max = zoneFuelRange[z].max;
                min = zoneFuelRange[z].min;
                String zHead = "Zone #" + ("" + (z + 1)).trim();
                ntRangeMax[z] = new NumberTextField(ipc, max, 6, false, 0, 5000, "#,###.00", "Fuel Range Max");
                ntRangeMin[z] = new NumberTextField(ipc, min, 6, false, 0, 5000, "#,###.00", "Fuel Range Min");
                editorPanel.addGroup();
                editorPanel.addItemPair(zHead, "", true);
                editorPanel.addItemPair(ntRangeMax[z]);
                editorPanel.addItemPair(ntRangeMin[z]);
            }
            editorPanel.closeGroup();
            editorPanel.setVisible(true);
            add(editorPanel);
            pack();
        }

        EditResponse.Response getResponse() {
             return response;
         }

        public ErrorStatAndMsg checkData() {
           if (ntFuelSegments.inError) //  || ntSpeedCheckInterval.inError)
                return new ErrorStatAndMsg(true, "Data out of Range");
            else {
                ErrorStatAndMsg errorStat = new ErrorStatAndMsg(false, "Error:");
                for (int z = 0; z < nZones; z++)
                    errorStat.add(checkZoneData(z));
                return errorStat;
            }
        }

        ErrorStatAndMsg checkZoneData(int zNum) {
            ErrorStatAndMsg retVal = new ErrorStatAndMsg();
            if (ntRangeMax[zNum].inError || ntRangeMin[zNum].inError)
                retVal.addErrorMsg("Data Out of range for Zone #" + ("" + (zNum + 1)).trim());
            else {
                double max = ntRangeMax[zNum].getData();
                double min = ntRangeMin[zNum].getData();
                if (max > 0 && min > 0) {
                    double td = max / min;
                    if (td < 1 || td > maxTurndown )
                        retVal.addErrorMsg("Fuel flow range error in zone #" + ("" + (zNum + 1)).trim());
                }
            }
            return retVal;
        }

        public boolean saveData() {
            fuelCharSteps = (int)ntFuelSegments.getData();
//            speedCheckInterval = (int)ntSpeedCheckInterval.getData();
            for (int z = 0; z < nZones; z++) {
                zoneFuelRange[z].max = ntRangeMax[z].getData();
                zoneFuelRange[z].min = ntRangeMin[z].getData();
            }
            edited = true;
            StatusWithMessage stat = checkIntegrity();
            if (stat.getDataStatus() == DataStat.Status.WithErrorMsg) {
                dfHeating.showError("Fuel data Error:\n" + stat.getErrorMessage(), this);
                return false;
            }
            else {
                if (dfHeating.dfhProcessList.takeFromUI()) {
                    backup = getBackup();
                    return true;
                }
                else
                    dfHeating.showError("Error in Settings for Field Process", this);
            }
            return false;
        }

        public void deleteData() {
            edited = true;
        }

        public void resetData() {
            if (backup != null)
                restoreFrom(backup);
            edited = false;
            ntFuelSegments.setData(fuelCharSteps);
//            ntSpeedCheckInterval.setData(speedCheckInterval);
            double max, min;
            for (int z = 0; z < nZones; z++) {
                max = zoneFuelRange[z].max;
                min =  zoneFuelRange[z].min;
                ntRangeMax[z].setData(max);
                ntRangeMin[z].setData(min);
            }
            editorPanel.resetAll();
            dfHeating.dfhProcessList.resetUI();
        }

         public void cancel() {
             if (backup != null)
                 restoreFrom(backup);
             response = EditResponse.Response.EXIT;
             setVisible(false);
         }
    }
}