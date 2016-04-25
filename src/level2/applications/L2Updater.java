package level2.applications;

/**
 * User: M Viswanathan
 * Date: 23-Mar-16
 * Time: 4:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2Updater extends L2DFHeating{

    public L2Updater(String equipment) {
        super(equipment);
        bAllowEditDFHProcess = false;
        bAllowEditFurnaceSettings = false;
        bAllowManualCalculation = false;
        bAllowUpdateWithFieldData = true;
        bAllowL2Changes = false;
        accessLevel = AccessLevel.UPDATER;
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
