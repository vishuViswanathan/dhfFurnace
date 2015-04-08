package mvUtils.math;

public class IntRange {
	public static final int ITISOUTOFRANGE = 0;
	public static final int ITISINRANGE = 1;
	public static final int ITISMAX = 2;
	public static final int ITISMIN = 4;
	public static final int ITSMAXANDMIN = (ITISMAX | ITISMIN);
	int min;
	int max;
	
	public IntRange() {
	}

	public IntRange(int min, int max) {
		this.min = min;
		this.max = max;
	}
	
	public IntRange(IntRange range) {
		min = range.min;
		max = range.max;
	}
	
	public void takeMinValue(int value) {
		min = value;
	}
	
	public void takeMaxValue(int value) {
		max = value;
	}
	
	public int getMax() {
		return max;
	}

	public int getMin() {
		return min;
	}

	public boolean isinRange(int val) {
		return (val >= min && val <= max);
	}

	int range() {
		return (max - min);
	}
}