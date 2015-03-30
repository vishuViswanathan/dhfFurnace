package basic;

import java.util.*;
import java.io.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */

public class OuterSurface {
    int chargeShape;
    public int surfaceID;
    public boolean aSkid = false;
    OneNode[] nodes; // nodes (OneNode) of external surface elements
    AmbientCycle ambient;
    double heatTransferQ = 0;
    double factor = 1.0;

    public OuterSurface(int chargeShape, int surfaceID, OneNode[] chargeSurfaceNodes,
                        AmbientCycle ambient) {
        this(chargeShape, surfaceID, chargeSurfaceNodes, ambient, 1.0, false);
    }

    public OuterSurface(int chargeShape, int surfaceID, OneNode[] chargeSurfaceNodes,
                        AmbientCycle ambient, double htFactor) {
        this(chargeShape, surfaceID, chargeSurfaceNodes, ambient, htFactor, false);
    }

    public OuterSurface(int chargeShape, int surfaceID,
                        OneNode[] chargeSurfaceNodes, AmbientCycle ambient,
                        double htFactor, boolean aSkid) {
        this.chargeShape = chargeShape;
        this.surfaceID = surfaceID;
        this.aSkid = aSkid;
        nodes = chargeSurfaceNodes;
//    for (int i = 0; i < chargeSurfaceNodes.length-1; i++)
//      chargeSurfaceNodes[i].anOuterSufaceNode = true;
        this.ambient = ambient;
        factor = htFactor;
    }

    public void update(double time) {
        double temp = ambient.getTemperature(time);
        double tk = ambient.getHeatTrCoeff(time);
        for (int n = 0; n < nodes.length; n++) {
            nodes[n].setTemperature(temp);
            nodes[n].setHeatTfCoeff(tk * factor);
        }
    }

    public double getHeatTransferQ() {
        double retVal = heatTransferQ;
        heatTransferQ = 0; // reset
        return retVal;
    }

    public void updateHeatTransfer() {
        heatTransferQ = heatTransferQ + deltaHeat();
    }

    double deltaHeat() {
        double q = 0;
        int orient = 0; // OneNode.ABOVE etc. the direction of the boundary to towards charge
        switch (chargeShape) {
            case ChargeDef.RECTANGULAR:
                switch (surfaceID) {
                    case 1:
                        orient = OneNode.ABOVE;
                        break;
                    case 2:
                        orient = OneNode.FRONT; //.LEFT;
                        break;
                    case 3:
                        orient = OneNode.BELOW;
                        break;
                    case 4:
                        orient = OneNode.BACK; //.RIGHT;
                        break;
                    case 21:
                        orient = OneNode.RIGHT; //.FRONT;
                        break;
                    case 22:
                        orient = OneNode.LEFT; //.BACK;
                        break;
                }
                break;
            case ChargeDef.BEAMBLANK_H:

                break;
        }
        if (orient != 0) {
            for (int n = 0; n < nodes.length; n++) {
                q += nodes[n].getBoundary(orient).heatTransferQ;
            }
        }
        return q;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        debug("Trying to save");
    }

    void debug(String msg) {
        System.out.println("OuterSurface: " + msg);
    }

}