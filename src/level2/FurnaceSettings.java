package level2;

import mvUtils.display.*;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.math.DoubleRange;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 18-Feb-15
 * Time: 12:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class FurnaceSettings   {
    L2DFHeating l2DFHeating;
    double maxSpeed; // m/min
    DoubleRange[] zoneFuelRange;
    DoubleRange totFuelRange;
    String errMsg = "Error reading Furnace Settings :";
    public boolean inError = false;
    double fuelTurnDown = 7;
    int fuelCharSteps = 7;

    public FurnaceSettings(L2DFHeating l2DFHeating) {
        this.l2DFHeating = l2DFHeating;
    }

    public FurnaceSettings(L2DFHeating l2DFHeating, String xmlStr) {
        this(l2DFHeating);
        takeDataFromXML(xmlStr);
    }

    void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    void setFuelRanges(double[] fuelRange) {
        zoneFuelRange = new DoubleRange[fuelRange.length];
//        double totMax = 0;
//        double totMin  = 0;
        double oneMax;
        double oneMin;
        for (int z = 0; z < fuelRange.length; z++) {
            oneMax = fuelRange[z];
            oneMin = oneMax / fuelTurnDown;
            DoubleRange oneRange = new DoubleRange(oneMin, oneMax);
            zoneFuelRange[z] = oneRange;
//            totMax += oneMax;
//            totMin += oneMin;
        }
        setTotalRange();
//        totFuelRange = new DoubleRange(totMin, totMax);
    }

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

    public boolean showEditData(boolean bEdit)  {
        FceSettingsDlg dlg = new FceSettingsDlg(bEdit);
        dlg.setLocation(100, 50);
        dlg.setVisible(true);
        return false;
    }

    boolean takeDataFromXML(String xmlStr) {  // TODO to add MaxExitZoneTemp
        boolean retVal = false;
        ValAndPos vp;
        errMsg = "Furnace Settings - Reading data:";
        aBlock: {
            try {
                vp = XMLmv.getTag(xmlStr, "maxSpeed", 0);
                maxSpeed = Double.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "fuelCharSteps", 0);
                fuelCharSteps = Integer.valueOf(vp.val);
                vp = XMLmv.getTag(xmlStr, "fuelRanges", 0);
                retVal = takeFuelRangesFromXML(vp.val);
                if (retVal)
                    setTotalRange();
            } catch (NumberFormatException e) {
                errMsg += "Some Number format error";
                inError = true;
                retVal = false;
                break aBlock;
            }
        }
        return retVal;
    }

    public StringBuffer dataInXML() {
        StringBuffer xmlStr = new StringBuffer(XMLmv.putTag("maxSpeed", maxSpeed));
        xmlStr.append(XMLmv.putTag("fuelCharSteps", "" + fuelCharSteps));
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

    class FceSettingsDlg extends JDialog implements DataHandler {
        InputControl ipc;
        DataListEditorPanel editorPanel;
        boolean editable = false;
        EditResponse.Response response;
        NumberTextField ntMaxSpeed;
        NumberTextField ntFuelSegments;
        NumberTextField[] ntRangeMax;
        NumberTextField[] ntTurnDown;
        int nZones;

        FceSettingsDlg(boolean editable) {
             this.editable = editable;
             setModal(true);
             init();
        }


        void init() {
            ipc = l2DFHeating;
            addWindowListener(new WindowAdapter() {
                 @Override
                 public void windowClosing(WindowEvent e) {
                     response = EditResponse.Response.EXIT;
                     super.windowClosing(e);
                 }
            });
            editorPanel = new DataListEditorPanel("Furnace Fuel Settings", this, true);
            ntMaxSpeed = new NumberTextField(ipc, maxSpeed, 6, false, 10, 500, "#,###", "Maximum Process Speed (m/mt)");
            ntFuelSegments = new NumberTextField(ipc, fuelCharSteps, 6, false, 3, 10, "##", "Fuel Characteristics-steps)");
            editorPanel.addItemPair(ntMaxSpeed);
            editorPanel.addItemPair(ntFuelSegments);
            editorPanel.addBlank();
            double max, td;
            nZones = l2DFHeating.l2Furnace.nTopActiveSecs;
            ntRangeMax = new NumberTextField[nZones];
            ntTurnDown = new NumberTextField[nZones];

            for (int z = 0; z < nZones; z++) {
                max = zoneFuelRange[z].max;
                td = (zoneFuelRange[z].min > 0) ? max / (zoneFuelRange[z].min) : 1;
                String zHead = "Zone #" + ("" + (z + 1)).trim();
                ntRangeMax[z] = new NumberTextField(ipc, max, 6, false, 0, 5000, "#,###.00", "Fuel Range Max");
                ntTurnDown[z] = new NumberTextField(ipc, td, 6, false, 1, 20, "##.00", "Fuel flow turn-down");
                editorPanel.addGroup();
                editorPanel.addItemPair(zHead, "", true);
                editorPanel.addItemPair(ntRangeMax[z]);
                editorPanel.addItemPair(ntTurnDown[z]);
            }
            editorPanel.setVisible(true);
            add(editorPanel);
            pack();
        }

        EditResponse.Response getResponse() {
             return response;
         }

        public ErrorStatAndMsg checkData() {
            if (ntMaxSpeed.inError || ntFuelSegments.inError)
                return new ErrorStatAndMsg(true, "Data out of Range");
            else {
                ErrorStatAndMsg errorStat = new ErrorStatAndMsg(false, "Error:");
                for (int z = 0; z < nZones; z++)
                    errorStat.add(checkZoneData(z));
                return errorStat;
            }
        }

        ErrorStatAndMsg checkZoneData(int zNum) {
            if (ntRangeMax[zNum].inError || ntTurnDown[zNum].inError)
                return new ErrorStatAndMsg(true, "Data Out of range for Zone #" + ("" + zNum).trim());
            else
                return new ErrorStatAndMsg(false, "");
        }

        public boolean saveData() {
            maxSpeed = ntMaxSpeed.getData();
            fuelCharSteps = (int)ntFuelSegments.getData();
            for (int z = 0; z < nZones; z++) {
                zoneFuelRange[z].max = ntRangeMax[z].getData();
                zoneFuelRange[z].min = zoneFuelRange[z].max / ntTurnDown[z].getData();
            }
            return false;
        }

        public void deleteData() {
        }

        public void resetData() {
            ntMaxSpeed.setData(maxSpeed);
            ntFuelSegments.setData(fuelCharSteps);
            double max, td;
            for (int z = 0; z < nZones; z++) {
                max = zoneFuelRange[z].max;
                ntRangeMax[z].setData(max);
                td = (zoneFuelRange[z].min > 0) ? max / (zoneFuelRange[z].min) : 1;
                ntTurnDown[z].setData(td);
            }
            editorPanel.resetAll();
//            editorPanel.setVisible(false);
//            editorPanel.setVisible(true);
        }

         public void cancel() {
 //            dataDlg.setVisible(false);
             response = EditResponse.Response.EXIT;
             setVisible(false);
         }
    }

}