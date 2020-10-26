package basic;

import mvUtils.display.InputControl;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.NumberTextField;
import mvUtils.mvXML.XMLmv;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/19/12
 * Time: 10:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class RadiantTube {
    public enum RTFunction {HEATING, COOLING}

    ;

    public enum RTSource {ELECTIRICAL, FUELFIRED}

    ;

    public enum RTType {SINGLEENDED, UTYPE, WTYPE}

    ;

    RTFunction function = RTFunction.HEATING;
    RTSource heating = RTSource.ELECTIRICAL;
    RTType tubeType = RTType.SINGLEENDED;

    public double dia = 0.198, activeLen = 1.4;
    public double internHtCoeff = 5000, surfEmiss = 0.85;
    public double rating = 15; // in kW??
    public double elementTemp;

    public RadiantTube() {

    }

    public RadiantTube(RTFunction function, RTSource heating, RTType tubeType, double od, double effLen, double emiss, double rating) {
        this.function = function;
        this.heating = heating;
        this.tubeType = tubeType;
        this.dia = od;
        this.activeLen = effLen;
        this.rating = rating;
        this.surfEmiss = emiss;
    }

    public RadiantTube(double od, double effLen, double rating) {
        this(RTFunction.HEATING, RTSource.ELECTIRICAL, RTType.SINGLEENDED, od, effLen, 0.85, rating);

    }

    public RadiantTube(double od, double effLen, double rating, double emiss) {
        this(RTFunction.HEATING, RTSource.ELECTIRICAL, RTType.SINGLEENDED, od, effLen, emiss, rating);

    }

    NumberTextField ntRadiantTubeOD;
    NumberTextField ntRadiantTubeLen;
    NumberTextField ntRadiantTubeRating;
    NumberTextField ntRadiantTubeEmiss;
    boolean radiantTubeFieldsSet = false;
    MultiPairColPanel dataPanel;

    public JPanel radiantTubesP(InputControl ipc) {
        if (!radiantTubeFieldsSet) {
            MultiPairColPanel pan = new MultiPairColPanel("One Radiant tube" );
            ntRadiantTubeOD = new NumberTextField(ipc, dia * 1000, 6, false,
                    10, 1000, "#,###", "Tube OD (mm)");
            ntRadiantTubeLen = new NumberTextField(ipc, activeLen * 1000, 6, false,
                    10, 10000, "#,###", "Effective Length (mm)");
            ntRadiantTubeRating = new NumberTextField(ipc, rating, 6, false,
                    0.01, 1000, "#,###", "Power Rating (kW)");
            ntRadiantTubeEmiss = new NumberTextField(ipc, surfEmiss, 6, false,
                    0.01, 1.0, "0.00", "Surface Emissivity");
            pan.addItemPair(ntRadiantTubeOD);
            pan.addItemPair(ntRadiantTubeLen);
//            pan.addItemPair(ntRadiantTubeRating);
            pan.addItemPair(ntRadiantTubeEmiss);
            dataPanel = pan;
            radiantTubeFieldsSet = true;
        }
        return dataPanel;
    }

    public MultiPairColPanel getDataPanel() {
        return dataPanel;
    }

    public boolean takeFromUI() {
        boolean retVal = false;
        if (!ntRadiantTubeOD.isInError() && !ntRadiantTubeLen.isInError() && !ntRadiantTubeRating.isInError() &&
                !ntRadiantTubeEmiss.isInError()) {
            dia = ntRadiantTubeOD.getData() / 1000;
            activeLen = ntRadiantTubeLen.getData() / 1000;
            rating = ntRadiantTubeRating.getData();
            surfEmiss = ntRadiantTubeEmiss.getData();
            retVal = true;
        }
        return retVal;
    }

    public void enableDataEdit(boolean ena) {
        ntRadiantTubeOD.setEditable(ena);
        ntRadiantTubeLen.setEditable(ena);
        ntRadiantTubeRating.setEditable(ena);
        ntRadiantTubeEmiss.setEditable(ena);

    }


    public double getTotHeatingSurface() {
        return Math.PI * dia * activeLen;
    }

    public String RTDataToSave() {
        String retVal;
        retVal = "<rt>" + XMLmv.putTag("dia", "" + dia * 1000);
        retVal += XMLmv.putTag("activeLen", "" + activeLen * 1000);
        retVal += XMLmv.putTag("rating", "" + rating);
        retVal += XMLmv.putTag("surfEmiss", "" + surfEmiss);
        retVal += "</rt>" + "\n";
        return retVal;

    }
}
