package basic;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */

public class SkidData {
    ThreeDCharge charge;
    double location;
    double width;
    int fromX, toX; // both inclusive
    AmbientCycle ambient;

    public SkidData(ThreeDCharge charge, double location, double width, AmbientCycle ambient) {
        this.charge = charge;
        fromX = charge.cellXlocInCharge(location - width / 2);
        toX = charge.cellXlocInCharge((fromX - 1) * charge.unitSide + width);
        this.width = (toX - fromX + 1) * charge.unitSide;
        this.location = (toX - 1) * charge.unitSide + width / 2;
        this.ambient = ambient;
    }

    public SkidData(double fromPos, double toPos, ThreeDCharge charge, AmbientCycle ambient) {
        this.charge = charge;
        fromX = (int) Math.round(fromPos / charge.unitSide);
        toX = charge.cellXlocInCharge((fromX - 1) * charge.unitSide + width);
        this.width = (toX - fromX + 1) * charge.unitSide;
        this.location = (toX - 1) * charge.unitSide + width / 2;
        this.ambient = ambient;
    }

    public SkidData(int fromXCell, int toXCell, AmbientCycle ambient) {
        fromX = fromXCell;
        toX = toXCell;
        this.ambient = ambient;
    }

    double getLocation() {
        return location;
    }

    double getWidth() {
        return width;
    }

    AmbientCycle getAmbient() {
        return ambient;
    }
}