package level2.applications;

import directFiredHeating.accessControl.L2AccessControl;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.swing.*;

/**
 * User: M Viswanathan
 * Date: 23-Mar-16
 * Time: 4:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class Level2Expert extends L2DFHeating {
    public Level2Expert(String equipment) {
        super(equipment);
        bAllowManualCalculation = true;
        bAllowUpdateWithFieldData = true;
        bAllowL2Changes = true;
        accessLevel = L2AccessControl.AccessLevel.EXPERT;
    }

    public Level2Expert(String equipment, boolean fromLauncher) {
        super(equipment);
        bAllowManualCalculation = true;
        bAllowUpdateWithFieldData = true;
        bAllowL2Changes = true;
        accessLevel = L2AccessControl.AccessLevel.EXPERT;
        if (setItUp()) {
            if (l2SystemReady) {
                l2Furnace.startL2DisplayUpdater();
                informLevel2Ready();
            } else {
                showError("Level2 could not be started. Aborting 002...");
                exitFromLevel2();
            }
        }
        else {
            showMessage("Facing problem connecting to Level1. Aborting ...");
            close();
        }
    }

    protected void startLog4j() {
        PropertyConfigurator.configure("log4jE.properties");
        log = Logger.getLogger("level2.EXPERT");
    }

    public static L2AccessControl.AccessLevel defaultLevel() {
        return L2AccessControl.AccessLevel.EXPERT;
    }

    protected JMenuBar assembleMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.add(createFileMenu());
        mb.add(createLiveDisplayMenu());
        mb.add(createDefineFurnaceMenu());
        mb.add(createShowResultsMenu());
        mb.add(mShowCalculation);
        mb.add(createPerformanceMenu());
        mb.add(createL2ConfMenu());
        mb.add(pbEdit);
        mb.add(createAccessMenu());
        return mb;
    }

    protected JMenu createFileMenu() {
        fileMenu = new JMenu("File");
        fileMenu.add(mIUpdateFurnace);
        fileMenu.addSeparator();
        fileMenu.add(mIExit);
        return fileMenu;
    }

    JMenu createAccessMenu() {
        mAccessControl = new JMenu("Access Control");
        mAccessControl.add(mUpdaterAccess);
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

    protected JMenu createDefineFurnaceMenu() {
        inputMenu = new JMenu("DefineFurnace");
        inputMenu.add(mIInputData);
        inputMenu.add(mIOpData);
        return inputMenu;
    }

    public boolean canClose() {
        return true;
    }
}
