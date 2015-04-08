package mvUtils.math;

import java.io.*;

public class DoublePoint
        implements Serializable {
    public double x;
    public double y;

    public DoublePoint(double dx, double dy) {
        setPoint(dx, dy);
    }

    public DoublePoint(DoublePoint dp) {
        this.x = dp.x;
        this.y = dp.y;
    }

    public void setPoint(double dx, double dy) {
        x = dx;
        y = dy;
    }

    public void setXval(double dx) { x = dx;}

    public void setYval(double dy) {
        y = dy;
    }

    public void addToYval(double dy) {
        y += dy;
    }


    public String toString() {
        return "" + x + ":" + y;
    }
}