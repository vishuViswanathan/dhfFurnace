package basic;

import display.*;

import java.util.Vector;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */

public class BeamBlankInFurnace
        implements Runnable {

    ThreeDCharge charge;
    public ChargeSurface[] chSurf;
    double c, density, lambda, dx;
    double time = 0.0;
    double unitTime = Double.NaN;
    double delTime;
    double endTime;
    static boolean runIt = false;
    static boolean continueIt = false;
    static boolean itsON = false;
    Vector<WindowListener> displayListener = new Vector <WindowListener>();
    ThreeDDisplay display = null;
    TemperatureStats dataCollection = null;
    boolean verticalWeb = false;
    boolean valid = false;

    public BeamBlankInFurnace(ThreeDCharge charge) {
        int type = charge.chargeDef.chargeType;
        if (type == ChargeDef.BEAMBLANK_H || type == ChargeDef.BEAMBLANK_V) {
            this.charge = charge;
            init();
            valid = true;
            verticalWeb = (type == ChargeDef.BEAMBLANK_V);
        }
    }

    int surfacesI[] = {
            ThreeDCharge.WIDTHMINFACE, ThreeDCharge.WIDTHMAXFACE,
            ThreeDCharge.HEIGHTMINFACE, ThreeDCharge.HEIGHTMAXFACE,
            ThreeDCharge.LENGTHMINFACE, ThreeDCharge.LENGTHMAXFACE};

    private void init() {
        chSurf = new ChargeSurface[]{
                new ChargeSurface(charge, surfacesI[0]),
                new ChargeSurface(charge, surfacesI[1]),
                new ChargeSurface(charge, surfacesI[2]),
                new ChargeSurface(charge, surfacesI[3]),
                new ChargeSurface(charge, surfacesI[4]),
                new ChargeSurface(charge, surfacesI[5])};
        c = charge.getC(0.0);
        density = charge.getDensity();
        lambda = charge.getTk(0.0);
        dx = charge.getUnitSide();
    }

    private void setUnitTime() {
        c = charge.getC();
        lambda = charge.getTk();
        setUnitTime(c, lambda);
    }

    private void setUnitTime(double c, double lambda) {
        unitTime = 0.5 * (1.0 / 6.0 * (c * density * dx * dx / lambda));
    }

    ChargeSurface getChargeSurface(int orient) {
        ChargeSurface surf = null;
        for (int n = 0; n < 6; n++) {
            if (surfacesI[n] == orient) {
                surf = chSurf[n];
                break;
            }
        }
        return surf;
    }

    boolean setSurfaceAmbientTo(int face, AmbientCycle ambient) {
        boolean retVal = false;
        boolean found = false;
        int n = 0;
        for (n = 0; n < 6; n++) {
            if (surfacesI[n] == face) {
                found = true;
                break;
            }
        }
        if (found) {
            retVal = chSurf[n].addSurfCondition(ambient);
        }
        return retVal;
    }

    public void setChargeTemperature(double temp) {
        charge.setBodyTemperature(temp);
        resetTime();
    }

    public void resetTime() {
        time = 0.0;
        endTime = 0;
        setUnitTime();
    }

    protected void setSurfaceConditions(double nowTime) {
        for (int n = 0; n < 6; n++) {
            chSurf[n].updateSurface(time);
        }
    }

    boolean evaluate(double forTime) {
        if (unitTime == 0) {
            errMessage("unitTime = 0");
            return false;
        }
        double till = time + forTime;
        // set surface conditions once before only
        setSurfaceConditions(time);
        while (time < till) {
            // evaluate one unitTime
            charge.update(unitTime);
            time += unitTime;
        }
        return false;
    }

    public void setDataCollection(TemperatureStats stats) {
        dataCollection = stats;
    }

    public boolean evaluate(double delTime,
                            boolean showResults) {
        this.delTime = delTime;
        endTime = time + delTime;
        return true;
    }

    public void addDisplayListener(WindowListener l) {
        displayListener.add(l);
    }

    public void run() {
        display = new ThreeDDisplay("Heating Display", charge, dataCollection,
                endTime);
        for (int n = 0; n < displayListener.size(); n++) {
            display.addWindowListener((WindowListener) displayListener.elementAt(n));
        }
        DecimalFormat format = new DecimalFormat("##0.000");
        display.addStartStopListener(
                new StartStopListener() {
                    public void startIt() {
                        runIt = true;
                    }

                    public void stopIt() {
                        runIt = false;
                        continueIt = false;
                    }

                    public void continueIt() {
                        continueIt = true;
                    }
                });
        display.setVisible(true);
        int n = 0;
        double grossUtime = unitTime * 5;
        dataCollection.update(time);
        while (time < endTime) {
            try {
                Thread.sleep(5);
            } catch (Exception e) {
                ;
            }
            if (!runIt && !continueIt) {
                continue;
            }
            if (runIt && !itsON) {
                itsON = true;
            }
            evaluate(grossUtime); // evaluate(unitTime);
            if (n++ > 20) {
                display.setTitle("Live Data at " + format.format(time) + "h of " +
                        format.format(endTime) + " h");
                display.updateNow();
                dataCollection.update(time);
                n = 0;
                setUnitTime();
            } else {
                n++;
            }
        }
        display.setTitle("Live Data at " + format.format(time) + "h of " +
                format.format(endTime) + " h");
        display.updateNow();
        dataCollection.update(time);
    }

    void errMessage(String msg) {
        System.out.println("ERROR in BeamBlankInFurnace: " + msg);
    }

    void debug(String msg) {
        System.out.println("BeamBlankInFurnace: " + msg);
    }

}