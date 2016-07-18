package display;

import mvUtils.display.FramedPanel;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 8/3/12
 * Time: 12:02 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ThreadController {
    boolean isRunOn();
    boolean isPauseOn();
    boolean isAborted();
    void showStatus(String msg);
    void updateGraph();
    void setProgressGraph(String title1, String title2, JPanel panel);
    void setMainTitle(String title);
    void setCalculTitle(String title);
    void abortIt();
}
