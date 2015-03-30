package mvXML;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 3/21/13
 * Time: 7:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class DoubleWithErrStat {
    public double val;
    public boolean allOK = true;

    public DoubleWithErrStat(String strSrc, String datName, XMLgroupStat grpStat) {
        strSrc = strSrc.trim();
        if (strSrc.length() <= 0) {
            allOK = false;
            val = Double.NaN;
            grpStat.addStat(false, "   Data not available for " + datName + "\n");
        }
        else {
            try {
                val = Double.valueOf(strSrc);
            } catch (NumberFormatException e) {
                val = Double.NaN;
                allOK = false;
                grpStat.addStat(false, "   Number Error in reading " + datName + "\n");
            }
        }
    }
}
