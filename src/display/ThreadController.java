package display;

import mvmath.FramedPanel;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 8/3/12
 * Time: 12:02 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ThreadController {
    public boolean isRunOn();
    public boolean isPauseOn();
    public boolean isAborted();
    public void showStatus(String msg);
    public void updateGraph();
    public void setProgressGraph(String title1, String title2, JPanel panel);
    public void setMainTitle(String title);
    public void setCalculTitle(String title);
    public void abortIt();
    public FramedPanel getProgressPanel();
}
