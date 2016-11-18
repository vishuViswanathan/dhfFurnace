package level2.applications;

import directFiredHeating.accessControl.L2AccessControl;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.swing.*;
import java.awt.*;
import java.nio.channels.FileLock;

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
                showError("Level2 Expert could not be started. Aborting 002...");
                exitFromLevel2();
            }
        }
        else {
            showMessage("Expert: Facing problem connecting to Level1. Aborting ...");
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

    protected void editStripDFHProcess() {
        if (dfhProcessList.addStripDFHProcess(parent()))
            showMessage("Strip DFH Process List updated\n" +
                    "To make it effective in Level2 RUNTIME, the Perfromance Data must be updated to file\n" +
                    "       " + perfMenu.getText() + "->" + mISavePerformanceData.getText());
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
        defineFileMenu();
//        fileMenu = new JMenu("File");
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
        definePerformanceMenu();
        perfMenu.add(mIShowPerfBase);
        perfMenu.add(new JSeparator());
        JLabel blank = new JLabel("") ;
        blank.setPreferredSize(new Dimension(100, 20));
        perfMenu.add(blank);
        perfMenu.add(new JSeparator());
        perfMenu.add(mISavePerformanceData);
        return perfMenu;
    }

    protected JMenu createDefineFurnaceMenu() {
        defineDefineFurnaceMenu();
//        defineFurnaceMenu = new JMenu("Operation Data");
        defineFurnaceMenu.add(mIOpData);
        return defineFurnaceMenu;
    }

    public boolean canClose() {
        return true;
    }
}
