package mvUtils.math;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 9/20/12
 * Time: 1:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class LineMv {
    double slope, y0;

    public LineMv(double x1, double y1, double x2, double y2) {
        if (x2 != x1) {
            slope = (y2 - y1) /(x2 - x1);
            y0 = y1 - slope * x1;
        }
        else {
            slope = 0;
            y0 = 0;
        }
    }

    public LineMv(double x1, double y1, double slope) {
        this.slope = slope;
        y0 = y1 - slope * x1;
    }

    public double findX(double y) {
        return (y - y0) / slope;
    }

    public double findY (double x) {
        return y0 = slope * x;
    }

    public double slope() {
        return slope;
    }

    public double getLengthWithX(double x1, double x2) {
        return Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((findY(x1) - findY(x2)), 2));
    }

    public double getLengthWithY(double y1, double y2) {
        return Math.sqrt(Math.pow((y1 - y2), 2) + Math.pow((findX(y1) - findX(y2)), 2));
    }

    public DoublePoint getIntersection(LineMv withLine) {
        double x, y;
        DoublePoint point = null;
        if (withLine.y0 != y0) {
            x = -(y0 - withLine.y0) / (slope - withLine.slope);
            y = y0 + slope * x;
            point = new DoublePoint(x, y);
        }
        return point;
    }

    public LineMv makePerpendLine(DoublePoint fromPoint) {
        return makePerpendLine(fromPoint.x, fromPoint.y);
    }

    public LineMv makePerpendLine(double fromX, double fromY) {
        return new LineMv(fromX, fromY, -(1/slope));
    }


}
