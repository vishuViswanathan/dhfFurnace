package level2.applications;

import directFiredHeating.accessControl.L2AccessControl;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

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
            if (l2SystemReady) {
                l2Furnace.startL2DisplayUpdater();
                l2Furnace.enableDeleteInPerformanceData(false);
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

    protected void startLog4j() {
        PropertyConfigurator.configure("log4jE.properties");
        log = Logger.getLogger("level2.UPDATER");
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
        defineFileMenu();
//        fileMenu = new JMenu("File");
        fileMenu.add(mIExit);
        return fileMenu;
    }

    JMenu createAccessMenu() {
        mAccessControl = new JMenu("Access Control");
        mAccessControl.add(mRuntimeAccess);
        return mAccessControl;
    }

    protected JMenu createPerformanceMenu() {
        definePerformanceMenu();
//        perfMenu = new JMenu("Performance");
        perfMenu.add(mIShowPerfBase);
        perfMenu.addSeparator();
        perfMenu.add(mISavePerformanceData);
        return perfMenu;
    }


    protected void showPerfMenu(boolean show) {
    }

    synchronized public void enablePerfMenu(boolean ena) {
    }

    public boolean canClose() {
        return true;
    }

 }
