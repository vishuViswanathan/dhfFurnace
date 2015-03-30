package basic;

import java.io.*;

public class OneNode implements Serializable {
    static final int ABOVE = 1;
    static final int BELOW = -1;
    static final int RIGHT = 2;
    static final int LEFT = -2;
    static final int FRONT = 4;
    static final int BACK = -4;

    ElementType elType = null;
    //  boolean anOuterSufaceNode = false;
    double h0; // if gaseous with fixed heat transfer coeff
    boolean constantTemp = false;
    Boundary aboveBoundary, belowBoundary,
            leftBoundary, rightBoundary,
            frontBoundary, backBoundary;
    double xLength, yLength, zLength;
    double halfXlength, halfYlength, halfZlength;
    double areaForX, areaForY, areaForZ;
    double volume;
    double mass;

    double nowTemperature = -1;
    double nextTemperature;

    public OneNode(ElementType element,
                   double xSize, double ySize, double zSize) {
        xLength = xSize;
        halfXlength = xLength / 2;
        yLength = ySize;
        halfYlength = yLength / 2;
        zLength = zSize;
        halfZlength = zLength / 2;

        areaForX = yLength * zLength;
        areaForY = xLength * zLength;
        areaForZ = xLength * yLength;
        volume = xLength * yLength * zLength;
        setElementType(element);

        aboveBoundary = belowBoundary =
                leftBoundary = rightBoundary =
                        frontBoundary = backBoundary = null;
    }

    public void setElementType(ElementType element) {
        elType = element;
        if (elType == null)
            setHeatTfCoeff(0);
        else
            setMass();
    }

    private void setMass() {
        if (elType == null)
            mass = 0;
        else
            mass = volume * elType.getDensity();
    }

    public OneNode(double heatTrCoeff) {
        setHeatTfCoeff(heatTrCoeff);
    }

    public void setHeatTfCoeff(double heatTrCoeff) {
        h0 = heatTrCoeff;
        constantTemp = true;
    }

    public void setTemperature(double temperature) {
        nowTemperature = nextTemperature = temperature;
    }

    void noteBoundary(Boundary boundary, int at) {
        switch (at) {
            case LEFT:
                leftBoundary = boundary;
                break;
            case RIGHT:
                rightBoundary = boundary;
                break;
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

    OneNode getNode(int onWhichSide) {
        OneNode retVal = null;
        switch (onWhichSide) {
            case ABOVE:
                retVal = aboveBoundary.getOtherNode(this);
                break;
            case BELOW:
                retVal = belowBoundary.getOtherNode(this);
                break;
            case RIGHT:
                retVal = rightBoundary.getOtherNode(this);
                break;
            case LEFT:
                retVal = leftBoundary.getOtherNode(this);
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

    Boundary getBoundary(int direction) {
        Boundary b = null;
        switch (direction) {
            case ABOVE:
                b = aboveBoundary;
                break;
            case BELOW:
                b = belowBoundary;
                break;
            case RIGHT:
                b = rightBoundary;
                break;
            case LEFT:
                b = leftBoundary;
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

    void update(double delTime) {
        if (constantTemp)
            return;
        if (elType == null)
            return;
        double heatInXAxis =
                ((leftBoundary != null ?
                        leftBoundary.getHeatTransferRate() : 0.0) -
                        (rightBoundary != null ?
                                rightBoundary.getHeatTransferRate() : 0.0)); //* areaForX;
        double heatInYAxis =
                ((backBoundary != null ?
                        backBoundary.getHeatTransferRate() : 0.0) -
                        (frontBoundary != null ?
                                frontBoundary.getHeatTransferRate() : 0.0)); //* areaForY;
        double heatInZAxis =
                ((belowBoundary != null ?
                        belowBoundary.getHeatTransferRate() : 0.0) -
                        (aboveBoundary != null ?
                                aboveBoundary.getHeatTransferRate() : 0.0)); //* areaForZ;
        double totHeatIn = (heatInXAxis +
                heatInYAxis +
                heatInZAxis) * delTime;
        double deltaTemp =
                totHeatIn / mass / elType.getC(nowTemperature);
        nextTemperature = nowTemperature + deltaTemp;
    }

    public double getTemperature() {
        return nowTemperature;
    }

    /**
     * the value of distance from center of node to the
     * boundary divided by the tK
     */
    double getHalfLbyK(Boundary toBoundary) {
        if (constantTemp)
            if (h0 == 0.0)
                return Double.NaN;
            else
                return (1 / h0);
        double k = elType.getTk(nowTemperature);  // TO CHECK
        if (k == 0)
            return Double.NaN;
        double halfL;
        if (toBoundary == leftBoundary ||
                toBoundary == rightBoundary)
            halfL = halfXlength;
        else if (toBoundary == frontBoundary ||
                toBoundary == backBoundary)
            halfL = halfYlength;
        else if (toBoundary == aboveBoundary ||
                toBoundary == belowBoundary)
            halfL = halfZlength;
        else {
            errMessage("getHalfLbyK: unknown boundary!");
            return Double.NaN;
        }
        return halfL / k;
    }

    void resetTemperature() {
        if (constantTemp)
            return;
        nowTemperature = nextTemperature;
//    elType.setTemperature(nowTemperature);
    }


    void errMessage(String msg) {
        System.err.println("OneNode: ERROR: " + msg);
    }

    void debug(String msg) {
        System.out.println("OneNode: " + msg);
    }
}

