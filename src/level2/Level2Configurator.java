package level2;

import directFiredHeating.DFHeating;
import mvUtils.display.StatusWithMessage;

/**
 * User: M Viswanathan
 * Date: 23-Mar-16
 * Time: 11:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class Level2Configurator extends L2DFHeating {
    public enum PreparerCommandLineArgs {
        SHOWDEBUG("-showDebug"),
        UNKNOWN("-UnKnown");
        private final String argName;

        PreparerCommandLineArgs(String argName) {
            this.argName = argName;
        }

        public String getValue() {
            return name();
        }

        @Override
        public String toString() {
            return argName;
        }

        public static PreparerCommandLineArgs getEnum(String text) {
            PreparerCommandLineArgs retVal = UNKNOWN;
            if (text != null) {
                for (PreparerCommandLineArgs b : PreparerCommandLineArgs.values()) {
                    if (text.equalsIgnoreCase(b.argName)) {
                        retVal = b;
                        break;
                    }
                }
            }
            return retVal;
        }
    }

    public Level2Configurator(String equipment) {
        super(equipment);
        bL2Configurator = true;
        onProductionLine = false;
        bAllowEditDFHProcess = true;
        bAllowEditFurnaceSettings = true;
        bAllowProfileChange = true;
        bAllowManualCalculation = true;
        bShowAllmenu = true;
//        userActionAllowed = true;
        accessLevel = AccessLevel.CONFIGURATOR;
    }

    protected StatusWithMessage getFceFromFile() {
        StatusWithMessage retVal = super.getFceFromFile();
        if (retVal.getDataStatus() != StatusWithMessage.DataStat.WithErrorMsg) {
            if (isProfileCodeOK()) {
                String filepath = retVal.getInfoMessage();
                int endOfBasePath = filepath.lastIndexOf("\\");
                if (endOfBasePath > 0) {
                    String basePath = filepath.substring(0, endOfBasePath) + "\\";
                    if (!loadAssociatedData(basePath)) {
                        retVal.setErrorMessage("Some problem in reading Associated Data");
                    }
                }
            }
            else {
                showMessage("No ProfileCode found." +
                        "\n   Preparing default Zonal Fuel Range Data" +
                        "\n   This must be updated and saved");
                clearAssociatedData();
            }
        }
        return retVal;
    }

    void defaultFurnaceSetting() {


    }

    static protected boolean  parseCmdLineArgs(String[] args) {
        int i = 0;
        boolean retVal = false;
        if (DFHeating.parseCmdLineArgs(args)) {
            PreparerCommandLineArgs cmdArg;
            while ((args.length > i)
                    && ((args[i].startsWith("-")))) {
                cmdArg = PreparerCommandLineArgs.getEnum(args[i]);
                switch (cmdArg) {
                    case SHOWDEBUG:
                        bl2ShowDebugMessages = true;
                        break;
                }
                i++;
            }
            retVal = true;
        }
        return retVal;
//        return true;
    }


    public static void main(String[] args) {
        final Level2Configurator l2Preparer = new Level2Configurator("Furnace");
        if (l2Preparer.parseCmdLineArgs(args)) {
            l2Preparer.setItUp();
            if (!l2Preparer.l2SystemReady) {
                l2Preparer.showError("Level2 could not be started. Aborting ...");
                System.exit(1);
            }
        }
    }

}
