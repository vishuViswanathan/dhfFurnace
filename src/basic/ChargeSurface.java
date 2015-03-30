package basic;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2005
 * Company:
 *
 * @author
 * @version 1.0
 */

public class ChargeSurface {
    ThreeDCharge ch;
    public int xSize = 0, ySize = 0;
    public double width, height;  // for display
    double halfUnitSide, unitSide;
    int orient; //ThreeDCharge.HEIGHTMAXFACE etc.
    int conditionsN = 0; // numbet of surface conditions
    static final int MAXCONDITIONS = 10;
    SurfaceConditions[] surfCond = new SurfaceConditions[MAXCONDITIONS];
    static String NOERROR = "No Error";
    static String XSCONDITIONS = "Too many Surface Conditions!";
    String errMsg = NOERROR;

    public ChargeSurface(ThreeDCharge charge, int orientation) {
        ch = charge;
        orient = orientation;
        conditionsN = 0;
        switch (orient) {
            case ThreeDCharge.HEIGHTMAXFACE:
            case ThreeDCharge.HEIGHTMINFACE:
                xSize = ch.xSize;
                ySize = ch.ySize;
                width = ch.adjustedLength;
                height = ch.adjustedWidth;
                break;
            case ThreeDCharge.WIDTHMAXFACE:
            case ThreeDCharge.WIDTHMINFACE:
                xSize = ch.xSize;
                ySize = ch.zSize;
                width = ch.adjustedLength;
                height = ch.adjustedHeight;
                break;
            case ThreeDCharge.LENGTHMAXFACE:
            case ThreeDCharge.LENGTHMINFACE:
                xSize = ch.ySize;
                ySize = ch.zSize;
                width = ch.adjustedWidth;
                height = ch.adjustedWidth;
                break;
            default:
                break;
        }
        xSize--;
        ySize--;
        unitSide = width / xSize;
        halfUnitSide = unitSide / 2;
    }

    /**
     * ambient condition for the selected area of surface
     * any new addition ot overlapping area overrides the previous
     */
    public boolean addSurfCondition(AmbientCycle ambient, int x1, int y1, int x2, int y2) {
        if (conditionsN < MAXCONDITIONS - 1) {
            surfCond[conditionsN++] = new SurfaceConditions(ambient, x1, y1, x2, y2);
            return true;
        } else {
            errMsg = XSCONDITIONS;
            return false;
        }
    }

    public String getLastError() {
        String retVal = errMsg;
        errMsg = NOERROR;
        return retVal;
    }

    /**
     * Gets the last (if overlapping) ambient in the cell x, y of the surface
     */
    public AmbientCycle getAmbientAt(int xPos, int yPos) {
        AmbientCycle amb = null;
        for (int n = conditionsN - 1; n >= 0; n--) {
            if (surfCond[n].isThisConditionAt(xPos, yPos)) {
                amb = surfCond[n].ambient;
                break;
            }
        }
        return amb;
    }

    public void clearAbients() {
        for (int n = 0; n < conditionsN; n++) {
            surfCond[n] = null;
        }
        conditionsN = 0;
    }

    /**
     * ambient condiation for the whole surface
     */
    public boolean addSurfCondition(AmbientCycle ambient) {
        return addSurfCondition(ambient, 1, 1, xSize, ySize);
    }

    void clearSurfaceCondition() {
        if (conditionsN > 0) {
            for (int n = 0; n < conditionsN; n++)
                surfCond[n] = null;
            conditionsN = 0;
        }
        return;
    }

    public String getName() {
        return ch.getSurfaceName(orient);
    }

    void updateSurface(double time) {
        for (int n = 0; n < conditionsN; n++)
            surfCond[n].update(time);
    }

    public double getXLocation(int x) {
        return (halfUnitSide + unitSide * (x - 1));
    }

    public double getYLocation(int y) {
        return (halfUnitSide + unitSide * (y - 1));
    }


    class SurfaceConditions {
        boolean valid = false;
        AmbientCycle ambient = null;
        // valid area in cell locations
        int x1, y1;
        int x2, y2;

        public SurfaceConditions(AmbientCycle ambient, int x1, int y1, int x2, int y2) {
            this.ambient = ambient;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        void update(double time) {
            double temp = ambient.getTemperature(time);
            double tk = ambient.getHeatTrCoeff(time);
            ch.setSurfaceTemperature(orient, temp, x1, y1, x2, y2);
            ch.setSurfaceHtTrCoeff(orient, tk, x1, y1, x2, y2);
        }

        boolean isThisConditionAt(int x, int y) {
            if ((x >= x1 && x <= x2) && (y >= y1 && y <= y2))
                return true;
            else
                return false;
        }
    }
}