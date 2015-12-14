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
    public void info(String msg);
    public void error(String msg);
}