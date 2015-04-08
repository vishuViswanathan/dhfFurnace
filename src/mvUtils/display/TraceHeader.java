package mvUtils.display;
import java.io.*;

public class TraceHeader implements Serializable{
	String traceName;
    String yName;
    String xName = "";
    String xNameWithUnits, yNameWithUnits;
	String xUnits;
	String yUnits;
    String xFormat = "", yFormat = "";

	public TraceHeader(String name, String xUnits,  String yUnits) {
        this("", xUnits, name, yUnits);
	}
	
    public TraceHeader(String xName, String xUnits,  String yName, String yUnits) {
        yName = yName;
        this.xUnits = xUnits;
        this.yUnits = yUnits;
        traceName = yName + ((yUnits.length() > 0) ? ("(" + yUnits + ")") : "");
        this.xName = xName;
        xNameWithUnits = xName + "(" + xUnits + ")";
        yNameWithUnits = yName + "(" + yUnits + ")";
    }

    public TraceHeader(String xName, String xUnits,  String xFormat, String yName, String yUnits, String yFormat) {
        this(xName, xUnits,  yName, yUnits);
        this.xFormat = xFormat;
        this.yFormat = yFormat;
    }

    public TraceHeader copyTo(TraceHeader copyTo) {
        copyTo.traceName = traceName;
        copyTo.yName = yName;
        copyTo.xName = xName;
        copyTo.xNameWithUnits = xNameWithUnits;
        copyTo.yNameWithUnits = yNameWithUnits;
        copyTo.xUnits = xUnits;
        copyTo.yUnits = yUnits;
        copyTo.xFormat = xFormat;
        copyTo.yFormat = yFormat;
        return copyTo;
    }

	public String toString() {
		return traceName;
	}
	
	public String getTraceName() {
		return traceName;
	}

    public String getxName() {
        return xName;
    }

    public String getyName() {
        return yName;
    }
}

