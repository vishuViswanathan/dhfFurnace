package level2.applications;

import directFiredHeating.DFHeating;
import directFiredHeating.accessControl.L2AccessControl;
import mvUtils.display.DataWithMsg;
import mvUtils.display.StatusWithMessage;
import mvUtils.file.FileChooserWithOptions;

import java.io.File;

/**
 * User: M Viswanathan
 * Date: 23-Mar-16
 * Time: 11:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class Level2Installer extends L2DFHeating {
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

    L2AccessControl installerAccessControl;

    public Level2Installer(String equipment) {
        super(equipment);
        onProductionLine = false;
        bAllowEditDFHProcess = true;
        bAllowEditFurnaceSettings = true;
        bAllowProfileChange = true;
        bAllowManualCalculation = true;
        bShowAllmenu = true;
//        userActionAllowed = true;
        accessLevel = L2AccessControl.AccessLevel.INSTALLER;
    }

    public Level2Installer(String equipment, boolean fromLauncher) {
        super(equipment);
        onProductionLine = false;
        bAllowEditDFHProcess = true;
        bAllowEditFurnaceSettings = true;
        bAllowProfileChange = true;
        bAllowManualCalculation = true;
        bShowAllmenu = true;
//        userActionAllowed = true;
        accessLevel = L2AccessControl.AccessLevel.INSTALLER;
        StatusWithMessage status = getInstallerAccessFile();
        if (status.getDataStatus() == StatusWithMessage.DataStat.OK) {
            setItUp();
            if (!l2SystemReady) {
                showError("Level2 could not be started. Aborting ...");
                System.exit(1);
            }
        }
        else
            showError("Unable to get Installer Access :" + status.getErrorMessage());
    }

    public static L2AccessControl.AccessLevel defaultLevel() {
        return L2AccessControl.AccessLevel.INSTALLER;
    }

    protected boolean getFieldPerformanceList(String basePath) {
        boolean retVal = false;
        File file = getParticularFile(basePath, profileCode, "perfData");
        if (file != null) {
            if (decide("Field Performance Data", "<html>Fiel-updated Performance Data File is available" +
                    "<br />   Do you want load this, overwriting Data with the Furnace Profile</html>")) {
                loadThePerformanceList(file);
                markThisFileAsBak(file);
                showMessage("Field Performance Data", "<html>Combined the Field Performance with the data with Furnace Profile." +
                        "<br />The existing Field-updated Performance file is marked as *.bak</html>");
            }
        }
        return retVal;
    }



    StatusWithMessage getInstallerAccessFile() {
        StatusWithMessage retVal = new StatusWithMessage();
        DataWithMsg pathStatus =
                FileChooserWithOptions.getOneExistingFilepath(fceDataLocation, L2AccessControl.installerAccessFileExtension, true);
        if (pathStatus.getStatus() == DataWithMsg.DataStat.OK) {
            try {
                installerAccessControl = new L2AccessControl(pathStatus.stringValue, true); // only if file exists
            } catch (Exception e) {
                retVal.addErrorMessage(e.getMessage());
            }
        }
        else
            retVal.addErrorMessage(pathStatus.errorMessage);
        return retVal;
    }

    protected boolean authenticate() {
        boolean retVal = false;
        StatusWithMessage stm = installerAccessControl.authenticate(accessLevel, "Re-confirm authority");
        if (stm.getDataStatus() == StatusWithMessage.DataStat.OK)
            retVal = true;
        return retVal;
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
        final Level2Installer l2Preparer = new Level2Installer("Furnace");
        if (l2Preparer.parseCmdLineArgs(args)) {
            l2Preparer.setItUp();
            if (!l2Preparer.l2SystemReady) {
                l2Preparer.showError("Level2 could not be started. Aborting ...");
                System.exit(1);
            }
        }
    }

}
