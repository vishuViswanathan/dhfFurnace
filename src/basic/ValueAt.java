package basic;

import java.text.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2005
 * Company:
 *
 * @author
 * @version 1.0
 */

public class ValueAt {
    double value;
    double x, y, z;

    public ValueAt() {
    }

    public ValueAt(double value, double x, double y, double z) {
        setPoint(x, y, z);
        setValue(value);
    }

    public void setPoint(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String textValue(String valFmt, String posFmt) {
        DecimalFormat forVal = new DecimalFormat(valFmt);
        DecimalFormat forPos = new DecimalFormat(posFmt);
        return (forVal.format(value) + " at " + forPos.format(x) + ", "
                + forPos.format(y) + ", "
                + forPos.format(z));

    }
}