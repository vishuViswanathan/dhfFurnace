package basic;

import java.util.*;
import java.text.*;

import display.*;

import java.awt.event.*;


/**
 * This object has reference of charge(ThreeDCharge charge),
 * a set of ambient cycles (Hashtable ambientType)
 */
public class ChargeHeatCycle implements Runnable {
    ChargeInFurnace chInFce;
    ThreeDCharge charge;
    //  public ChargeSurface[] chSurf;
    OuterSurface[] outerSurface;
    double c, density, lambda, dx;
    double time = 0.0;
    double unitTime = Double.NaN;
    double delTime;
    double endTime;
    static boolean runIt = false;
    static boolean continueIt = false;
    static boolean itsON = false;
    Vector<WindowListener> displayListener = new Vector<WindowListener>();
    ThreeDDisplay display = null;
    TemperatureStats dataCollection = null;

//  int surfacesI[] = {ThreeDCharge.WIDTHMINFACE, ThreeDCharge.WIDTHMAXFACE,
//                        ThreeDCharge.HEIGHTMINFACE,ThreeDCharge.HEIGHTMAXFACE,
//                        ThreeDCharge.LENGTHMINFACE, ThreeDCharge.LENGTHMAXFACE};

//  public ChargeHeatCycle(ThreeDCharge charge) {
//    this.charge = charge;
//    init();
//  }

    public ChargeHeatCycle(ChargeInFurnace chargeInFurnace) {
        chInFce = chargeInFurnace;
        this.charge = chargeInFurnace.ch;
        init();
    }

    private void init() {
//    chSurf = new ChargeSurface[] {new ChargeSurface(charge, surfacesI[0]),
//                                  new ChargeSurface(charge, surfacesI[1]),
//                                  new ChargeSurface(charge, surfacesI[2]),
//                                  new ChargeSurface(charge, surfacesI[3]),
//                                  new ChargeSurface(charge, surfacesI[4]),
//                                  new ChargeSurface(charge, surfacesI[5])};
        c = charge.getC(0.0);
        density = charge.getDensity();
        lambda = charge.getTk(0.0);
        dx = charge.getUnitSide();
        outerSurface = chInFce.getOuterSurfaces();
        charge.noteOuterSurfaces(outerSurface);
    }

    private void setUnitTime() {
        c = charge.getC();
        lambda = charge.getTk();
        setUnitTime(c, lambda);
    }

    private void setUnitTime(double c, double lambda) {
        unitTime = 0.5 * (1.0 / 6.0 * (c * density * dx * dx / lambda));
//    unitTime = 0.5*(1.0/4.0*(c*density * dx * dx /lambda));
// DEBUG
//    debug("Unit time " + unitTime + ", c = " + c + ", tk = " + lambda);
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
        for (int n = 0; n < outerSurface.length; n++) {
            outerSurface[n].update(nowTime);
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
//      // set surface conditions
//      setSurfaceConditions(time);
            // evaluate one unitTime
            charge.update(unitTime);
            collectSurHeatData();
            time += unitTime;
        }
        return false;
    }

//  void updateSurfaceHeats() {
//    for (int s = 0; s < outerSurface.length; s++)
//      outerSurface[s].deltaHeat();
//  }

    void collectSurHeatData() {
        for (int s = 0; s < outerSurface.length; s++)
            outerSurface[s].updateHeatTransfer();
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
        display = new ThreeDDisplay("Heating Display", charge, dataCollection, endTime);
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
                        display.resultsReady();
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
            if (!runIt && !continueIt)
                continue;
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
            } else
                n++;
        }
        display.resultsReady();
        display.setTitle("READY Data at " + format.format(time) + "h of " +
                format.format(endTime) + " h");
        display.updateNow();
        dataCollection.update(time);
    }


    void errMessage(String msg) {
        System.out.println("ERROR in ChargeHeatCycle: " + msg);
    }

    void debug(String msg) {
        System.out.println("ChargeHeatCycle: " + msg);
    }

}

