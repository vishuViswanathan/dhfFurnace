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

public class MaxMinAvgAt {
    ValueAt max;
    ValueAt min;
    double avg;

    public MaxMinAvgAt(ValueAt max, ValueAt min, double avg) {
        this.max = max;
        this.min = min;
        this.avg = avg;
    }

    public ValueAt getMaxAt() {
        return max;
    }

    public ValueAt getMinAt() {
        return min;
    }

    public double getAvg() {
        return avg;
    }

}