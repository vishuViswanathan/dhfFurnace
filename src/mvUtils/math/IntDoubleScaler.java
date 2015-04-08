package mvUtils.math;


public class IntDoubleScaler {
	DoubleRange dRange;
	IntRange iRange;
	double factor;
	PosAndValue[] scalePoints;
    SetNumberFormat format;
	public IntDoubleScaler(IntRange intRange,
										DoubleRange doubleRange) {
		dRange = doubleRange; // new DoubleRange(doubleRange);
		iRange = new IntRange(intRange);

		factor = (double)(iRange.max - iRange.min) /
										(dRange.max - dRange.min);
		if (iRange.range() == 0)
			factor = 0.0;
		else if (Double.isNaN(factor))
			factor = 0.0;
        format = new SetNumberFormat(dRange.max, dRange.min);
        scalePoints = setScalePoints();
	}

    public void setDoubleRange(DoubleRange doubleRange) {
        dRange = doubleRange; // new DoubleRange(doubleRange);
  		factor = (double)(iRange.max - iRange.min) /
      										(dRange.max - dRange.min);
   		if (iRange.range() == 0)
   			factor = 0.0;
   		else if (Double.isNaN(factor))
   			factor = 0.0;
        format = new SetNumberFormat(dRange.max, dRange.min);
        scalePoints = setScalePoints();
    }
	
	public double factor() {
		return factor;
	}
	
	public int intVal(double dVal) {
		return ((int)Math.round((dVal - dRange.min) * factor) + iRange.min); 
	}
	
	public double doubleVal(int iVal) {
		return ((double)(iVal - iRange.min) / factor + dRange.min); 
	}

    int[] scaleMarks() {
        int[] marks = new int[dRange.nMarks];
        double val = dRange.min;
        for (int i = 0; i < dRange.nMarks; i++)  {
            marks[i] = intVal(val);
            val +=  dRange.majDiv;
        }
        return marks;
    }

    public PosAndValue[] setScalePoints() {
        PosAndValue[] dp = new PosAndValue[dRange.nMarks];
        double val = dRange.min;
        for (int i = 0 ; i < dRange.nMarks; i++) {
            dp[i] = new PosAndValue(intVal(val), val);
            dp[i].noteFormatter(format);
            val +=  dRange.majDiv;
        }
        return dp;
    }

    public PosAndValue[] getScalePoints() {
        return scalePoints;
    }

    public DoubleRange getDoubleRange() {
        return dRange;
    }
 }