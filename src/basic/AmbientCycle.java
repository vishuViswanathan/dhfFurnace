package basic;

import java.io.*;

import basic.*;

public class AmbientCycle implements Cloneable {
    static final int TEMPERATURE = 0;
    static final int HEATTRCOEFF = 1;
    static String DEFAULTSURFACE = "Insulated Surface";
    String name;
    InterpolTable cycle;

    public AmbientCycle(String name) {
        setName(name);
        init();
    }

    public AmbientCycle() {
        this(DEFAULTSURFACE);
        setInsulatedCycle();
    }

    private void setInsulatedCycle() {
        noteCycleSegment(0.0, 0.0, 0.0);
        noteCycleSegment(10000.0, 0.0, 0.0);
        makeItReady();
    }

    public boolean noteCycleSegment(double time,
                                    double temperature, double heatTrCoeff) {
        double[] data = new double[]{temperature, heatTrCoeff};
        return cycle.add(time, data);
    }

    private void init() {
        cycle = new InterpolTable(2,
                new String[]{"Time", "Temperature", "HeatTrCoeff"});
    }

    public boolean noteCycleSegment(OneCycleElement element) {
        double[] data = element.getElements();
        return cycle.add(data);
    }

    public boolean noteCycleSegment(double time,
                                    double temperature, double heatTrCoeff,
                                    boolean forceIt) {
        boolean retVal = true; // always
        double[] data = new double[]{temperature, heatTrCoeff};
        if (!cycle.add(time, data)) {
            removeCycleSegment(temperature); // remove first
            cycle.add(time, data);
        }
        return retVal;
    }

    public boolean removeCycleSegment(double time) {
        return cycle.remove(time);
    }

    public boolean removeCycleSegment(OneCycleElement element) {
        return cycle.remove(element.getTime());
    }

    public void clear() {
        init();
    }

    public double[] getFirstSegment() {
        return cycle.getFirstData();
    }

    public double[] getNextSegment(double[] ref) {
        return cycle.getNextData(ref);
    }

    public boolean makeItReady() {
        return cycle.thatsIt();
    }

    public double getTemperature(double time) {
        return cycle.getDataAtRef(time, TEMPERATURE);
    }

    public double getHeatTrCoeff(double time) {
        return cycle.getDataAtRef(time, HEATTRCOEFF);
    }

    public Object clone() {
        AmbientCycle ambient = new AmbientCycle("Copy of " + name);
        ambient.cycle = (InterpolTable) cycle.clone();
        return ambient;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void printData() {
        PrintStream ps = System.out;
        ps.println();
        ps.println(this);
        cycle.printData();
    }

    public String toString() {
        return "Ambient type: " + name;
    }

    void debug(String msg) {
        System.out.println("AmbientCyle: " + msg);
    }
}

