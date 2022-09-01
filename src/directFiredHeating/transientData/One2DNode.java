package directFiredHeating.transientData;

import basic.ChMaterial;

import java.io.*;

public class One2DNode implements Serializable {
    static final int ABOVE = 1;
    static final int BELOW = -1;
    static final int FRONT = 4;
    static final int BACK = -4;

    ChMaterial material;

    double h0; // if gaseous with fixed heat transfer coeff
    boolean constantTemp = false;
    Boundary2D aboveBoundary, belowBoundary,
            frontBoundary, backBoundary;
    double xLength, yLength, zLength;
    double halfXlength, halfYlength, halfZlength;
    double areaForX, areaForY, areaForZ;
    double volume;
    double mass;

    double nowTemperature = -1;
    double nextTemperature;

    public One2DNode(ChMaterial material,
                     double ySize, double zSize) {
        xLength = 1.0;
        halfXlength = xLength / 2;
        yLength = ySize;
        halfYlength = yLength / 2;
        zLength = zSize;
        halfZlength = zLength / 2;

        areaForX = yLength * zLength;
        areaForY = xLength * zLength;
        areaForZ = xLength * yLength;
        volume = xLength * yLength * zLength;
        setMaterial(material);

        aboveBoundary = belowBoundary =
                        frontBoundary = backBoundary = null;
    }

    public void setMaterial(ChMaterial material) {
        this.material = material;
        if (material != null)
            setMass();
    }

    private void setMass() {
        mass = volume * material.getDensity();
    }

    public void setTemperature(double temperature) {
        nowTemperature = nextTemperature = temperature;
    }

    public void setHeatTfCoeff(double heatTrCoeff) {
        h0 = heatTrCoeff;
        constantTemp = true;
    }

    void noteBoundary(Boundary2D boundary, int at) {
        switch (at) {
            case BACK:
                backBoundary = boundary;
                break;
            case FRONT:
                frontBoundary = boundary;
                break;
            case BELOW:
                belowBoundary = boundary;
                break;
            case ABOVE:
                aboveBoundary = boundary;
                break;
            default:
                break;
        }
    }

    One2DNode getNode(int onWhichSide) {
        One2DNode retVal = null;
        switch (onWhichSide) {
            case ABOVE:
                retVal = aboveBoundary.getOtherNode(this);
                break;
            case BELOW:
                retVal = belowBoundary.getOtherNode(this);
                break;
            case FRONT:
                retVal = frontBoundary.getOtherNode(this);
                break;
            case BACK:
                retVal = backBoundary.getOtherNode(this);
                break;
            default:
                errMessage("getNode: Unknown side");
                retVal = null;
                break;
        }
        return retVal;
    }

    Boundary2D getBoundary(int direction) {
        Boundary2D b = null;
        switch (direction) {
            case ABOVE:
                b = aboveBoundary;
                break;
            case BELOW:
                b = belowBoundary;
                break;
            case FRONT:
                b = frontBoundary;
                break;
            case BACK:
                b = backBoundary;
                break;
        }
        return b;
    }

    void update(double delTime, double spHt) {
        if (constantTemp)
            return;
        if (material == null)
            return;
//        double heatInYAxis =
//                ((backBoundary != null ?
//                        backBoundary.getHeatTransferRate() : 0.0) -
//                        (frontBoundary != null ?
//                                frontBoundary.getHeatTransferRate() : 0.0)); //* areaForY;
        double heatInYAxis =
                ((frontBoundary != null ?
                        frontBoundary.getHeatTransferRate() : 0.0) -
                        (backBoundary != null ?
                                backBoundary.getHeatTransferRate() : 0.0)); //* areaForY;
        double heatInZAxis =
                ((belowBoundary != null ?
                        belowBoundary.getHeatTransferRate() : 0.0) -
                        (aboveBoundary != null ?
                                aboveBoundary.getHeatTransferRate() : 0.0)); //* areaForZ;
        double totHeatIn = (heatInYAxis +
                heatInZAxis) * delTime;
        double deltaTemp =
                totHeatIn / mass / spHt;
        nextTemperature = nowTemperature + deltaTemp;
    }

    public double getTemperature() {
        return nowTemperature;
    }

    /**
     * the value of distance from center of node to the
     * boundary divided by the tK
     */
    double getHalfLbyK(double tk, Boundary2D toBoundary) {
        if (constantTemp)
            if (h0 == 0.0)
                return Double.NaN;
            else
                return (1 / h0);
        if (tk == 0)
            return Double.NaN;
        double halfL;
        if (toBoundary == frontBoundary ||
                toBoundary == backBoundary)
            halfL = halfYlength;
        else if (toBoundary == aboveBoundary ||
                toBoundary == belowBoundary)
            halfL = halfZlength;
        else {
            errMessage("getHalfLbyK: unknown boundary!");
            return Double.NaN;
        }
        return halfL / tk;
    }

    void resetTemperature() {
        if (constantTemp)
            return;
        nowTemperature = nextTemperature;
//    elType.setTemperature(nowTemperature);
    }


    void errMessage(String msg) {
        System.err.println("One2DNode: ERROR: " + msg);
    }

    void debug(String msg) {
        System.out.println("One2DNode: " + msg);
    }
}

