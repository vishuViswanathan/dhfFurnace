package mvmath;
import java.io.*;

public class DoubleRange implements Serializable{
	public static final int ITISOUTOFRANGE = 0;
	public static final int ITISINRANGE = 1;
	public static final int ITISMAX = 2;
	public static final int ITISMIN = 4;
	public static final int ITSMAXANDMIN = (ITISMAX | ITISMIN);
	
	public double min;
	public double max;
    double mid;
	public double majDiv;
    public int nMarks = 0;

	public DoubleRange() {
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        mid = 0;
	}
	
	public DoubleRange(double min, double max) {
		this.min = min;
		this.max = max;
        findMid();
	}
	
	public DoubleRange(DoubleRange range) {
        this(range.min, range.max);
//		min = range.min;
//		max = range.max;
//        findMid();
	}

    public void findMid() {
        mid = (max + min) / 2;
    }

    public DoubleRange copyTo(DoubleRange copyTo) {
        copyTo.min = min;
        copyTo.max = max;
        copyTo.findMid();
        copyTo.majDiv = majDiv;
        copyTo.nMarks = nMarks;
        return copyTo;
    }

    public void setMinMax(double min, double max) {
        this.min = min;
        this.max = max;
        findMid();
    }

    public void setDoubleCenter(double x) {
        double shift = x - mid;
        setMinMax(min + shift, max + shift);
    }

    public void shift(double shiftBy) {
        setMinMax(min + shiftBy, max+ shiftBy);
    }

    public void scaleDouble(double factor) {
        double newMin = mid - (mid - min)* factor;
        double newMax = mid + (max - mid) * factor;
        setMinMax(newMin, newMax);
    }

    public void reset() {
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
    }

    public void setOnlyEnds() {
        nMarks = 2;
        majDiv = (max - min);

    }
	public void takeMaxValue(double value) {
		max = value;
        findMid();
	}
	
	public void takeMinValue(double value) {
		min = value;
        findMid();
	}
	
	public double getMax() {
		return max;
	}

	public double getMin() {
		return min;
	}

	public boolean isinRange(double val) {
		return (val >= min && val <= max);
	}
	
	double range() {
		return (max - min);
	}

    public void setMajDiv(double val) {
        majDiv = val;
        nMarks = (int)((max - min) / majDiv);
    }

    public int getNmarks() {
        return nMarks;
    }

    public void takeVal(double val) {
        min = Math.min(min, val);
        max = Math.max(max, val);
        findMid();
    }

    public double limitedValue(double val) {
        if (val < min)
            val = min;
        if (val > max)
            val = max;
        return val;
    }
}