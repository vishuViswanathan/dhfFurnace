package level2.applications;

import directFiredHeating.accessControl.L2AccessControl;
import mvUtils.display.DataWithMsg;
import mvUtils.display.ErrorStatAndMsg;
import mvUtils.display.StatusWithMessage;
import mvUtils.file.FileChooserWithOptions;

import javax.swing.*;
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
//        bAllowEditDFHProcess = true;
        bAllowProfileChange = true;
        bAllowManualCalculation = true;
        bAllowUpdateWithFieldData = true;
//        userActionAllowed = true;
        accessLevel = L2AccessControl.AccessLevel.INSTALLER;
    }

    public Level2Installer(String equipment, boolean fromLauncher) {
        super(equipment);
        onProductionLine = false;
//        bAllowEditDFHProcess = true;
        bAllowProfileChange = true;
        bAllowManualCalculation = true;
        bAllowUpdateWithFieldData = true;
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

    protected ErrorStatAndMsg getFieldPerformanceList(String basePath) {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg();
        File file = getParticularFile(basePath, profileCode, "perfData");
        if (file != null) {
            if (decide("Field Performance Data", "<html>Fiel-updated Performance Data File is available" +
                    "<br />   Do you want load this, overwriting Data with the Furnace Profile</html>")) {
                if (loadThePerformanceList(file)) {
                    if (markThisFileAsBak(file))
                        showMessage("Field Performance Data", "<html>Combined the Field Performance with the data with Furnace Profile." +
                                "<br />The existing Field-updated Performance file is marked as *.bak</html>");
                    else
                        showError("Unable to rename the perfData file to *.bak");
                }
                else
                    retVal.addErrorMsg("Some problem in reading Field Performance Data");
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

    protected JMenu createFileMenu() {
        fileMenu = new JMenu("File");
        fileMenu.add(mIGetFceProfile);
        fileMenu.add(mISaveFceProfile);
        fileMenu.addSeparator();
        fileMenu.add(mIUpdateFurnace);
        fileMenu.addSeparator();
        fileMenu.add(mIExit);
        return fileMenu;
    }

    protected JMenuBar assembleMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.add(createFileMenu());
        mb.add(createDefineFurnaceMenu());
        mb.add(createShowResultsMenu());
        mb.add(createPerformanceMenu());
        mb.add(createL2ConfMenu());
        mb.add(pbEdit);
        mb.add(createAccessMenu());
        return mb;
    }
}
