package mvUtils.math;

/**
 * class DoubleMaxMin
 * keeps track of maximum and minimum value introduced
 * Even if max and min are directly set, they are checked
 * for their relative values and interchanged if required
 */

public class DoubleMaxMin
    extends DoubleRange {

  public DoubleMaxMin() {
    reset();
  }

  public DoubleMaxMin(double min, double max) {
    if (max > min) {
      this.min = min;
      this.max = max;
    }
    else {
      this.min = max;
      this.max = min;
    }
  }

  public DoubleMaxMin(DoubleRange range) {
    this(range.min, range.max);
  }

  public int takeNewValue(double value) {
    int retval = 0;
    if (value > max) {
      max = value;
      retval |= ITISMAX;
    }

    if (value < min) {
      min = value;
      retval |= ITISMIN;
    }
    return retval;
  }

  public void takeMaxValue(double value) {
    boolean retval = false;
    max = value;
    if (value < min) {
      min = value;
    }
  }

  public void takeMinValue(double value) {
    boolean retval = false;
    min = value;
    if (value > max) {
      max = value;
    }
  }

  public void reset() {
    max = Double.NEGATIVE_INFINITY;
    min = Double.POSITIVE_INFINITY;
  }

}