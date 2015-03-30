package mvXML;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 3/21/13
 * Time: 8:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMLgroupStat {
    public boolean allOK = true;
    public String errMsg;
    public XMLgroupStat(String errMsg) {
        this.errMsg = errMsg;
        if (errMsg.length() > 0 ) allOK = false;
    }

    public XMLgroupStat() {
        this("");
    }

    public void addStat(boolean bStat, String msg) {
        allOK &= bStat;
        errMsg += msg;
    }

}
