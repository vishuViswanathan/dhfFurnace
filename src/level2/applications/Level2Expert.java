package level2.applications;

import level2.accessControl.L2AccessControl;

/**
 * User: M Viswanathan
 * Date: 23-Mar-16
 * Time: 4:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class Level2Expert extends L2DFHeating {
    public Level2Expert(String equipment) {
        super(equipment);
        bAllowEditDFHProcess = true;
        bAllowEditFurnaceSettings = true;
        bAllowManualCalculation = true;
        bAllowUpdateWithFieldData = true;
        bAllowL2Changes = true;
        accessLevel = L2AccessControl.AccessLevel.EXPERT;
    }

    public Level2Expert(String equipment, boolean fromLauncher) {
        super(equipment);
        bAllowEditDFHProcess = true;
        bAllowEditFurnaceSettings = true;
        bAllowManualCalculation = true;
        bAllowUpdateWithFieldData = true;
        bAllowL2Changes = true;
        accessLevel = L2AccessControl.AccessLevel.EXPERT;
        if (setupUaClient()) {
            setItUp();
            if (l2SystemReady) {
                informLevel2Ready();
            } else {
                showError("Level2 could not be started. Aborting ...");
                exitFromLevel2();
            }
        }
        else {
            showMessage("Facing problem connecting to Level1. Aborting ...");
            close();
        }

    }

    public static L2AccessControl.AccessLevel defaultLevel() {
        return L2AccessControl.AccessLevel.EXPERT;
    }

    public static void main(String[] args) {
        final Level2Expert l2Expert = new Level2Expert("Furnace");
        if (l2Expert.parseCmdLineArgs(args)) {
            if (l2Expert.setupUaClient()) {
                l2Expert.setItUp();
                if (l2Expert.l2SystemReady) {
                    l2Expert.informLevel2Ready();
                }
                else {
                    l2Expert.showError("Level2 could not be started. Aborting ...");
                    l2Expert.exitFromLevel2();
//                    System.exit(1);
                }
            }
            else
                l2Expert.showMessage("Facing problem connecting to Level1. Aborting ...");
        }
    }
}
