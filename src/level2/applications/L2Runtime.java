package level2.applications;

import directFiredHeating.accessControl.L2AccessControl;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.util.Date;

/**
 * User: M Viswanathan
 * Date: 08-Aug-16
 * Time: 1:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2Runtime extends L2DFHeating {

    public L2Runtime(String equipment) {
        super(equipment);
        bAllowManualCalculation = false;
        bAllowUpdateWithFieldData = false;
        accessLevel = L2AccessControl.AccessLevel.RUNTIME;
        if (setItUp()) {
            log = Logger.getLogger("level2.RUNTIME");
            if (l2SystemReady) {
                informLevel2Ready();
            } else {
                showError("Level2 Runtime could not be started. Aborting ...");
                exitFromLevel2();
            }
        } else {
            showMessage("Facing problem connecting to Level1. Aborting ...");
            close();
        }
    }

    public static L2AccessControl.AccessLevel defaultLevel() {
        return L2AccessControl.AccessLevel.RUNTIME;
    }

    protected void enableDefineMenu(boolean ena) {
    }

    public void enableDataEdit() {
    }

    protected void showPerfMenu(boolean show) {
    }

    synchronized public void enablePerfMenu(boolean ena) {
    }

    protected JMenuBar assembleMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.add(createFileMenu());
        mb.add(createLiveDisplayMenu());
        return mb;
    }

    protected JMenu createFileMenu() {
        fileMenu = new JMenu("File");
        fileMenu.add(mIExit);
        return fileMenu;
    }

    public boolean canClose() {
        return true;
    }

//    public void l2Trace(String msg) {
//        if (log != null)
//            log.trace(accessLevel.toString() + " thru RT:" + msg);
//    }
//
//    public void l2Info(String msg) {
//        if (log != null)
//            log.info(accessLevel.toString() + ":" + msg);
//    }
//
//    public void l2Error(String msg) {
//        if (log != null)
//            log.error(accessLevel.toString() + ":" + msg);
//    }


}