package basic;

import javax.vecmath.*;
import java.awt.*;

public class PointInCharge {
    public Integer id;
    public ThreeDCharge theCharge;
    double actualLocL, actualLocW, actualLocH;
    double desiredLocL, desiredLocW, desiredLocH;
    Point3d actualLoc;
    Point3d desiredLoc;
    Point3i cellLoc;
    public Color color;
    boolean showTrend;
    double temperature = 0;

    public PointInCharge(ThreeDCharge charge, Integer id, double l, double w, double h)
            throws Exception {
        theCharge = charge;
        this.id = id;
        desiredLocL = l;
        desiredLocW = w;
        desiredLocH = h;
        cellLoc = charge.getCell(l, w, h);
        int justForException;
        if (cellLoc == null)
            justForException = 1 / 0;
        actualLocL = charge.getXLocation(cellLoc.x);
        actualLocW = charge.getYLocation(cellLoc.y);
        actualLocH = charge.getZLocation(cellLoc.z);
        actualLoc = new Point3d(actualLocL, actualLocW, actualLocH);
    }

    public Point3d getActualLoc() {
        return actualLoc;
    }

    void updatePoint() {
        temperature = theCharge.getCellTemperature(cellLoc);
    }
}
