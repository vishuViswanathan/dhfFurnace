package basic;

import java.text.DecimalFormat;

/**
 * one element of ambient cycle
 * basically it has three double elements
 */
public class OneCycleElement extends Object {
    double time;
    double temperature;
    double heatTrCoeff;

    public OneCycleElement() {
    }

    public OneCycleElement(double[] elements) {
        setElements(elements);
    }

    public OneCycleElement(double time, double temperature,
                           double heatTrCoeff) {
        setElements(time, temperature, heatTrCoeff);
    }

    public void setElements(double time, double temperature,
                            double heatTrCoeff) {
        this.time = time;
        this.temperature = temperature;
        this.heatTrCoeff = heatTrCoeff;
    }

    public void setElements(double[] elements) {
        if (elements.length >= 3) {
            time = elements[0];
            temperature = elements[1];
            heatTrCoeff = elements[2];
        }
    }

    public double[] getElements() {
        return new double[]{time, temperature, heatTrCoeff};
    }

    public double getTime() {
        return time;
    }

    DecimalFormat fmtTime = new DecimalFormat("00.00000");
    DecimalFormat fmtTemp = new DecimalFormat("0000.0");
    DecimalFormat fmtAlpha = new DecimalFormat("0000.0");


    public String toString() {
        return "At " + fmtTime.format(time) + " h, Temperature " +
                fmtTemp.format(temperature) + " C, HtTrCoeff " +
                fmtAlpha.format(heatTrCoeff) + " kcal/m2hC";
    }
}

