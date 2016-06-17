package directFiredHeating.process;

import directFiredHeating.DFHeating;
import directFiredHeating.FceSection;
import mvUtils.display.*;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.math.DoubleRange;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
    DoubleRange totFuelRange;
    public String errMsg = "Error reading Furnace Settings :";
    public boolean inError = false;
    double maxTurndown = 10;
//    double fuelTurnDown = 7;
    public int fuelCharSteps = 7;
    public boolean considerFieldZoneTempForLossCorrection = false;
    // @@@@@@@@@@@@@@ REMEMBER to modify backup() and restore(), for any change in
    // data structure

    public FurnaceSettings(DFHeating dfHeating) {
        this.dfHeating = dfHeating;
    }

    public FurnaceSettings(DFHeating dfHeating, Vector<FceSection> activeSections) {
        this(dfHeating);
        fuelCharSteps = 7;
        considerFieldZoneTempForLossCorrection = false;
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
        return retVal;
    }

    /*
    void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    void setFuelRanges(double[] fuelRange) {
        zoneFuelRange = new DoubleRange[fuelRange.length];
        double oneMax;
        double oneMin;
        for (int z = 0; z < fuelRange.length; z++) {
            oneMax = fuelRange[z];
            oneMin = oneMax / fuelTurnDown;
            DoubleRange oneRange = new DoubleRange(oneMin, oneMax);
            zoneFuelRange[z] = oneRange;
        }
        setTotalRange();
    }
*/

    void setTotalRange() {
        double totMax = 0;
        double totMin  = 0;
        for (int z = 0; z < zoneFuelRange.length; z++) {
            totMax += zoneFuelRange[z].max;
            totMin += zoneFuelRange[z].min;
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
        aBlock: {
            try {
//                vp = XMLmv.getTag(xmlStr, "maxSpeed", 0);
//                maxSpeed = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "opcIP", 0);
                if (vp.val.length() > 5)
                    opcIP = vp.val.trim();
                vp = XMLmv.getTag(xmlStr, "fuelCharSteps", 0);
                fuelCharSteps = Integer.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "considerFieldZoneTempForLossCorrection", 0);
                considerFieldZoneTempForLossCorrection = (vp.val.equals("1"));
                vp = XMLmv.getTag(xmlStr, "fuelRanges", 0);
                retVal = takeFuelRangesFromXML(vp.val);
                if (retVal)
                    setTotalRange();
            } catch (NumberFormatException e) {
                errMsg += "Some Number format error";
                inError = true;
                break aBlock;
            }
        }
        return retVal;
    }

    public StringBuffer dataInXML() {
//        StringBuffer xmlStr = new StringBuffer(XMLmv.putTag("maxSpeed", maxSpeed));
        StringBuffer xmlStr = new StringBuffer(XMLmv.putTag("opcIP", opcIP));
        xmlStr.append(XMLmv.putTag("fuelCharSteps", "" + fuelCharSteps));
        xmlStr.append(XMLmv.putTag("considerFieldZoneTempForLossCorrection", considerFieldZoneTempForLossCorrection));
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

    FurnaceSettings getbackup() {
//        to.dfHeating = dfHeating;
        FurnaceSettings backup = new FurnaceSettings(dfHeating);
        backup.zoneFuelRange = new DoubleRange[zoneFuelRange.length];
        for (int i = 0; i < zoneFuelRange.length; i++)
            backup.zoneFuelRange[i] = new DoubleRange(zoneFuelRange[i]);
        backup.fuelCharSteps = fuelCharSteps;
        backup.considerFieldZoneTempForLossCorrection = considerFieldZoneTempForLossCorrection;
        return backup;
    }

    void  restoreFrom(FurnaceSettings from) {
        for (int i = 0; i < zoneFuelRange.length; i++)
            zoneFuelRange[i] = new DoubleRange(from.zoneFuelRange[i]);
        fuelCharSteps = from.fuelCharSteps;
        considerFieldZoneTempForLossCorrection = from.considerFieldZoneTempForLossCorrection;
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
//        NumberTextField ntMaxSpeed;
        NumberTextField ntFuelSegments;
        JRadioButton rbConsiderFieldZoneTempForLossCorrection;
        NumberTextField[] ntRangeMax;
        NumberTextField[] ntRangeMin;
//        NumberTextField[] ntTurnDown;
        int nZones;
        boolean edited = false;
        FurnaceSettings backup;

        FceSettingsDlg(boolean editable) {
            this.editable = editable;
            setModal(true);
            backup = getbackup();
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
//            ntMaxSpeed = new NumberTextField(ipc, maxSpeed, 6, false, 10, 500, "#,###", "Maximum Process Speed (m/mt)");
            ntFuelSegments = new NumberTextField(ipc, fuelCharSteps, 6, false, 3, 10, "##", "Fuel Characteristics-steps)");
            rbConsiderFieldZoneTempForLossCorrection =
                    new JRadioButton("Take Field Zone Temp For Loss Check", considerFieldZoneTempForLossCorrection);
            rbConsiderFieldZoneTempForLossCorrection.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if (rbConsiderFieldZoneTempForLossCorrection.isSelected()) {
                        dfHeating.showError("Not ready for this to be ON", FceSettingsDlg.this);
                        rbConsiderFieldZoneTempForLossCorrection.setSelected(false);
                    }
                }
            });
//            editorPanel.addItemPair(ntMaxSpeed);
            editorPanel.addItemPair(ntFuelSegments);
            editorPanel.addBlank();
            editorPanel.addItem(rbConsiderFieldZoneTempForLossCorrection);
            editorPanel.addBlank();
            double max, min, td;
            nZones = dfHeating.furnace.nTopActiveSecs;
            ntRangeMax = new NumberTextField[nZones];
            ntRangeMin = new NumberTextField[nZones];
//            ntTurnDown = new NumberTextField[nZones];

            for (int z = 0; z < nZones; z++) {
                max = zoneFuelRange[z].max;
                min = zoneFuelRange[z].min;
                td = (zoneFuelRange[z].min > 0) ? max / (zoneFuelRange[z].min) : 1;
                String zHead = "Zone #" + ("" + (z + 1)).trim();
                ntRangeMax[z] = new NumberTextField(ipc, max, 6, false, 0, 5000, "#,###.00", "Fuel Range Max");
                ntRangeMin[z] = new NumberTextField(ipc, min, 6, false, 0, 5000, "#,###.00", "Fuel Range Min");
//                ntTurnDown[z] = new NumberTextField(ipc, td, 6, false, 1, 20, "##.00", "Fuel flow turn-down");
                editorPanel.addGroup();
                editorPanel.addItemPair(zHead, "", true);
                editorPanel.addItemPair(ntRangeMax[z]);
                editorPanel.addItemPair(ntRangeMin[z]);
//                editorPanel.addItemPair(ntTurnDown[z]);
            }
            editorPanel.setVisible(true);
            add(editorPanel);
            pack();
        }

        EditResponse.Response getResponse() {
             return response;
         }

        public ErrorStatAndMsg checkData() {
//            if (ntMaxSpeed.inError || ntFuelSegments.inError)
           if (ntFuelSegments.inError)
                return new ErrorStatAndMsg(true, "Data out of Range");
            else {
                ErrorStatAndMsg errorStat = new ErrorStatAndMsg(false, "Error:");
                for (int z = 0; z < nZones; z++)
                    errorStat.add(checkZoneData(z));
                return errorStat;
            }
        }

        ErrorStatAndMsg checkZoneData(int zNum) {
//            if (ntRangeMax[zNum].inError || ntTurnDown[zNum].inError)
//                return new ErrorStatAndMsg(true, "Data Out of range for Zone #" + ("" + zNum).trim());
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
//            maxSpeed = ntMaxSpeed.getData();
            fuelCharSteps = (int)ntFuelSegments.getData();
            considerFieldZoneTempForLossCorrection = rbConsiderFieldZoneTempForLossCorrection.isSelected();
            for (int z = 0; z < nZones; z++) {
                zoneFuelRange[z].max = ntRangeMax[z].getData();
                zoneFuelRange[z].min = ntRangeMin[z].getData();
            }
            edited = true;
            StatusWithMessage stat = checkIntegrity();
            if (stat.getDataStatus() == StatusWithMessage.DataStat.WithErrorMsg) {
                dfHeating.showError("Fuel data Error:\n" + stat.getErrorMessage(), this);
                return false;
            }
            else
                return true;
        }

        public void deleteData() {
            edited = true;
        }

        public void resetData() {
            if (backup != null)
                restoreFrom(backup);
            edited = false;
//            ntMaxSpeed.setData(maxSpeed);
            ntFuelSegments.setData(fuelCharSteps);
            double max, min;
            for (int z = 0; z < nZones; z++) {
                max = zoneFuelRange[z].max;
                min =  zoneFuelRange[z].min;
                ntRangeMax[z].setData(max);
                ntRangeMin[z].setData(min);

//                td = (zoneFuelRange[z].min > 0) ? max / (zoneFuelRange[z].min) : 1;
//                ntTurnDown[z].setData(td);
            }
            editorPanel.resetAll();
//            editorPanel.setVisible(false);
//            editorPanel.setVisible(true);
        }

         public void cancel() {
             if (backup != null)
                 restoreFrom(backup);
             response = EditResponse.Response.EXIT;
             setVisible(false);
         }
    }

}