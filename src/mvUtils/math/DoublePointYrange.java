package mvUtils.math;
import java.io.*;

/**
* class DoublePointYrange
* holds the range of x values with the correspoding x
* one can get maxYpointAndX and minYpointAndX
*/

public class DoublePointYrange implements Serializable{
	DoubleMaxMin yRange;
	double minYat = 0;
	double maxYat = 0;
		// these values not meaningless if yRange is not set

	public DoublePointYrange() {
		reset();
	}
	
	public void reset() {
		yRange = new DoubleMaxMin();
	}
	
	public void takeApoint(DoublePoint p) {
		int maxOrMin = yRange.takeNewValue(p.y);
		
		if ((maxOrMin & DoubleRange.ITISMAX) == DoubleRange.ITISMAX) {
			maxYat = p.x;
		}

		if ((maxOrMin & DoubleRange.ITISMIN) == DoubleRange.ITISMIN) {
			minYat = p.x;
		}
	}
	
	public DoubleRange getRange() {
		return new DoubleRange(yRange);
	}
	
	public DoublePoint getMaxPoint() {
		return new DoublePoint(yRange.getMax(), maxYat);
	}
}