package mvUtils.display;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 12/6/12
 * Time: 12:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ValueForExcel {
    public boolean bBold;
    public String strValue;
    public double numValue;
    public boolean bNumeric;
    public String fmtStr = "";

    public ValueForExcel(boolean bBold, String strValue, double numValue, boolean bNumeric) {
        this(bBold, strValue, numValue, bNumeric, "");
    }

    public ValueForExcel(boolean bBold, String strValue, double numValue, boolean bNumeric, String fmtStr) {
        this.bBold = bBold;
        this.strValue = strValue;
        this.numValue = numValue;
        this.bNumeric = bNumeric;
        this.fmtStr = fmtStr;
    }

    public ValueForExcel(boolean bBold, String strValue) {
        this(bBold, strValue, 0, false);
    }

}
