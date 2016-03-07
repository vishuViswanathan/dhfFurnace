package level2.common;

import TMopcUa.TMuaClient;
import mvUtils.display.InputControl;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 16-Sep-15
 * Time: 4:14 PM
 * To change this template use File | Settings | File Templates.
 */
public interface L2Interface {
    public TMuaClient source();
    public InputControl controller();
    public String equipment();
    public void logInfo(String msg);
    public void logError(String msg);
    public void logTrace(String msg);
    public void logDebug(String nsg);
}
