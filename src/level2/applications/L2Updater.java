package level2.applications;

import directFiredHeating.accessControl.L2AccessControl;
import org.apache.log4j.Logger;

import javax.swing.*;

/**
 * User: M Viswanathan
 * Date: 23-Mar-16
 * Time: 4:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2Updater extends L2DFHeating{

    public L2Updater(String equipment) {
        super(equipment);
        bAllowManualCalculation = false;
        bAllowUpdateWithFieldData = true;
        bAllowL2Changes = false;
        accessLevel = L2AccessControl.AccessLevel.UPDATER;
    }

    public L2Updater(String equipment, boolean fromLauncher) {
        super(equipment);
        bAllowManualCalculation = false;
        bAllowUpdateWithFieldData = true;
        bAllowL2Changes = false;
        accessLevel = L2AccessControl.AccessLevel.UPDATER;
        if (setItUp()) {
            log = Logger.getLogger("level2.UPDATER");
            if (l2SystemReady) {
                informLevel2Ready();
            } else {
                showError("Level2 Updater could not be started. Aborting ...");
                exitFromLevel2();
            }
        }
        else  {
            showMessage("Facing problem connecting to Level1. Aborting ...");
            close();
        }
    }

    public static L2AccessControl.AccessLevel defaultLevel() {
        return L2AccessControl.AccessLevel.UPDATER;
    }

    public void performanceTableDone() {
        switchPage(L2DisplayPageType.PROCESS);
    }


    public void enableDataEdit() {
    }

    protected void enableDefineMenu(boolean ena) {
    }

    protected JMenuBar assembleMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.add(createFileMenu());
        mb.add(createLiveDisplayMenu());
        mb.add(mShowCalculation);
        mb.add(createPerformanceMenu());
        mb.add(createAccessMenu());
        return mb;
    }

    protected JMenu createFileMenu() {
        fileMenu = new JMenu("File");
        fileMenu.add(mIExit);
        return fileMenu;
    }

    JMenu createAccessMenu() {
        mAccessControl = new JMenu("Access Control");
        mAccessControl.add(mRuntimeAccess);
        return mAccessControl;
    }

    protected JMenu createPerformanceMenu() {
        perfMenu = new JMenu("Performance");
        perfMenu.add(mIShowPerfBase);
        perfMenu.addSeparator();
        perfMenu.add(mISavePerformanceData);
        perfMenu.add(mIReadPerformanceData);
        return perfMenu;
    }


    protected void showPerfMenu(boolean show) {
    }

    synchronized public void enablePerfMenu(boolean ena) {
    }

    public boolean canClose() {
        return true;
    }

    public static void main(String[] args) {
        final L2Updater l2Updater = new L2Updater("Furnace");
        if (l2Updater.parseCmdLineArgs(args)) {
            if (l2Updater.setupUaClient()) {
                l2Updater.setItUp();
                if (l2Updater.l2SystemReady) {
                    l2Updater.informLevel2Ready();
                }
                else {
                    l2Updater.showError("Level2 Updater could not be started. Aborting ...");
                    l2Updater.exitFromLevel2();
                }
            }
            else
                l2Updater.showMessage("Facing problem connecting to Level1. Aborting ...");
        }
    }
}
