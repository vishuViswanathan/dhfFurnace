package mvUtils.display;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 10/12/12
 * Time: 11:47 AM
 * To change this template use File | Settings | File Templates.
 */
public interface InputControl {
    public boolean canNotify();

    public void enableNotify(boolean ena);

    public Window parent();
}
