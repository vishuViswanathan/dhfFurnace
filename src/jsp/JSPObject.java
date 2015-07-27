package jsp;

import jsp.JSPConnection;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 24-Jul-15
 * Time: 10:17 AM
 * To change this template use File | Settings | File Templates.
 */
public interface JSPObject {
    public boolean isDataCollected();
    public boolean collectData(JSPConnection jspConnection);
}
