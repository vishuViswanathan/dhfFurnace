package testing;

import mvUtils.display.DataStat;
import mvUtils.display.DataWithStatus;
import protection.CheckAppKey;

/**
 * User: M Viswanathan
 * Date: 10-May-17
 * Time: 12:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestPad {
    static public void main(String[] args) {
        CheckAppKey keyCheck = new CheckAppKey("localhost");
        DataWithStatus resp = keyCheck.canRunThisApp(100, false);
        DataStat.Status stat = resp.getStatus();
        print(stat.toString());
        if (stat == DataStat.Status.WithErrorMsg)
            print(resp.getErrorMessage());
    }

    static void print(String msg) {
        System.out.println(msg);
    }
}
