package basic;

import java.io.*;

import java.io.Serializable;

public class ElementType extends Object
        implements Cloneable, Serializable {
    public String name;
    TwoDTable tK; // thermal conductivity
    TwoDTable c;  // specific heat
    TwoDTable emiss; // emissivity
    private double density; // kg/m3
    private double nowTk;
    private double nowC;
    private double nowEmiss;
    // double nowTemperature;
    private boolean constantProps = false;

    public ElementType(String name, double density,
                       TwoDTable thermalK,
                       TwoDTable specificH) {
        this.name = name;
        this.density = density;
        this.tK = thermalK;
        this.c = specificH;
        nowEmiss = 0.8;
    }

    public ElementType(String name, double density,
                       double tk, double c) {
        this.name = name;
        this.density = density;
        nowTk = tk;
        nowC = c;
        nowEmiss = 0.8;
        constantProps = true;
    }

    public ElementType(String name, double density,
                       TwoDTable thermalK,
                       TwoDTable specificH, TwoDTable emiss) {
        this.name = name;
        this.density = density;
        this.tK = thermalK;
        this.c = specificH;
        this.emiss = emiss;
    }


    public String getName() {
        return name;
    }

    public double getDensity() {
        return density;
    }

    public double getC(double temperature) {
        double value;
        if (constantProps)
            value = nowC;
        else {
            try {
                value = c.getData(0, temperature, true);
            } catch (Exception e) {
                errMessage("c at " + temperature);
                value = Double.NaN;
            }
        }
        return value;
    }

    public double getTk(double temperature) {
        double value;
        if (constantProps)
            value = nowTk;
        else {
            try {
                value = tK.getData(0, temperature, true);
            } catch (Exception e) {
                errMessage("tk at " + temperature);
                value = Double.NaN;
            }
        }
        return value;
    }

    public double getEmiss(double temperature) {
        double value;
        if (constantProps)
            value = nowEmiss;
        else {
            try {
                value = emiss.getData(0, temperature, true);
            } catch (Exception e) {
                errMessage("emissivity at " + temperature);
                value = Double.NaN;
            }
        }
        return value;
    }

    //  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//    debug("Trying to save ElementType");
//  }

    public String toString() {
        return name + " Density: " + density;
    }

    void errMessage(String msg) {
        System.err.println("ElementType: ERROR: " + msg);
    }

    void debug(String msg) {
        System.out.println("ElementType: " + msg);
    }

}

