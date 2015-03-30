package basic;

import performance.stripFce.OneZone;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 2/5/14
 * Time: 12:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class Observations {
    int count = 0;
    String observations = "";

    public Observations() {

    }

    public int add(String msg) {
        count++;
        observations += "\n    " + count + ". " + msg;
        return count;
    }

    public boolean isAnyThere() {
        return count > 0;
    }

    public String toString() {
        return observations;
    }

}
