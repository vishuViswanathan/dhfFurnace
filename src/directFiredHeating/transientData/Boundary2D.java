package directFiredHeating.transientData;


import java.io.Serializable;

public class Boundary2D implements Serializable {
    static final int XY = 1;
    static final int ZX = 3;

    One2DNode node1;
    One2DNode node2;
    int orient = 0;
    double area;
    double heatTransferRate;
    double temperature;
    double heatTransferQ;

    public Boundary2D(One2DNode node1, One2DNode node2, int orientation) {
        this.node1 = node1;
        this.node2 = node2;
        orient = orientation;
        switch (orient) {
            case XY:
                area = node1.areaForZ;
                break;
            case ZX:
                area = node1.areaForY;
                break;
        }
    }

    One2DNode getOtherNode(One2DNode oneNode) {
        if (oneNode == node1)
            return node2;
        else if (oneNode == node2)
            return node1;
        else
            return null;
    }

    public boolean update(double delTime, double tk) {
        update(tk);
        heatTransferQ = heatTransferRate * delTime;
        return true;
    }

    public boolean update(double tk) {
        double lByK1 = 0, lByK2 = 0;
        double t1 = 0, t2 = 0;
        t1 = node1.getTemperature();
        lByK1 = node1.getHalfLbyK(tk,this);
        t2 = node2.getTemperature();
        lByK2 = node2.getHalfLbyK(tk, this);
//        // TO CHECK the following is made more sensible
//        if (Double.isNaN(lByK1) || Double.isNaN(lByK2)) {
//            heatTransferRate = 0;
//        } else {
            heatTransferRate = (t1 - t2) / (lByK1 + lByK2) * area;
            temperature = t1 - heatTransferRate / area * lByK1;
//        }
        return true;
    }

    double getHeatTransferRate() {
        return heatTransferRate;
    }

    double getTemperature() {
        return temperature;
    }

    void errMessage(String msg) {
        System.err.println("Boundary: ERROR: " + msg);
    }

    void debug(String msg) {
        System.out.println("Boundary: " + msg);
    }

}

