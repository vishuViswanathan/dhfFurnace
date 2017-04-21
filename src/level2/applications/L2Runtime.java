package level2.applications;

import directFiredHeating.accessControl.L2AccessControl;
import level2.common.ReadyNotedParam;
import mvUtils.display.DataStat;
import mvUtils.display.DataWithStatus;
import mvUtils.display.ErrorStatAndMsg;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.swing.*;
import java.util.Date;

/**
 * User: M Viswanathan
 * Date: 08-Aug-16
 * Time: 1:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2Runtime extends L2DFHeating {

    public L2Runtime(String equipment, boolean fromLauncher) {
        super(equipment, fromLauncher);
        bAllowManualCalculation = false;
        bAllowUpdateWithFieldData = false;
        accessLevel = L2AccessControl.AccessLevel.RUNTIME;
        if (setItUp()) {
            if (l2SystemReady) {
                l2Furnace.startL2DisplayUpdater();
                l2Furnace.startSpeedUpdater();
                l2Furnace.enableDeleteInPerformanceData(false);
                informLevel2Ready();
            } else {
                showError("Level2 Runtime could not be started. Aborting ...");
                exitFromLevel2();
            }
        } else {
            showMessage("Runtime: Facing problem connecting to Level1. Aborting ...");
            close();
        }
    }

    protected void startLog4j() {
        PropertyConfigurator.configure("log4jR.properties");
        log = Logger.getLogger("level2.RUNTIME");
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
        defineFileMenu();
//        fileMenu = new JMenu("File");
        fileMenu.add(mIExit);
        return fileMenu;
    }

    public ErrorStatAndMsg checkConnection() {
        ErrorStatAndMsg retVal = super.checkConnection();
        l2ProcessList.noteConnectionsCheckStat(retVal);
        return retVal;
    }

    public boolean connectProcessListToLevel1() {
        return l2ProcessList.connectToLevel1();
    }

    public void clearLevel1ProcessList() {
        l2ProcessList.clearLevel1ProcessList();
//        l2Furnace.processListToLevel1Updated(0);
    }

    public boolean sendProcessListToLevel1() {
        l2Furnace.logTrace("sending Process List to Level1");
        int p = l2ProcessList.sendListToLevel1();
        l2Furnace.processListToLevel1Updated(p);
        return p > 0;
    }

    void justQuit() {
        l2Furnace.clearProcessList();
        super.justQuit();
    }

    public boolean canClose() {
        boolean retVal = false;
        DataWithStatus<Boolean> status = isAnyRunning(
                new L2AccessControl.AccessLevel[]{
                        L2AccessControl.AccessLevel.EXPERT,
                        L2AccessControl.AccessLevel.UPDATER});
        if (status.getStatus() == DataStat.Status.OK) {
            if (status.getValue()) {
                showError("Either of " + L2AccessControl.AccessLevel.EXPERT +
                        ", or " + L2AccessControl.AccessLevel.UPDATER + " is Running\n" +
                "Exit them and then try exiting RUNTIME");
            }
            else
                retVal = true;
        }
        return retVal;
    }
}