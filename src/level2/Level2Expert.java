package level2;

import com.sun.org.apache.bcel.internal.generic.L2D;

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
        accessLevel = AccessLevel.EXPERT;
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
