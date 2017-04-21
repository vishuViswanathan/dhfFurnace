package level2.applications;

import directFiredHeating.accessControl.L2AccessControl;
import mvUtils.display.*;
import mvUtils.file.FileChooserWithOptions;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

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
        super(equipment, false);
        onProductionLine = false;
//        bAllowEditDFHProcess = true;
        bAllowProfileChange = true;
        bAllowManualCalculation = true;
        bAllowUpdateWithFieldData = true;
//        userActionAllowed = true;
        accessLevel = L2AccessControl.AccessLevel.INSTALLER;
    }

    public Level2Installer(String equipment, boolean fromLauncher) {
        super(equipment, fromLauncher);
        onProductionLine = false;
//        bAllowEditDFHProcess = true;
        bAllowProfileChange = true;
        bAllowManualCalculation = true;
        bAllowUpdateWithFieldData = true;
//        userActionAllowed = true;
        accessLevel = L2AccessControl.AccessLevel.INSTALLER;
        StatusWithMessage status = getInstallerAccessFile();
        if (status.getDataStatus() == DataStat.Status.OK) {
            setItUp();
            if (l2SystemReady) {
                showMessage("It is the responsibility of the user to ensure data integrity among:" +
                        "\n      1) Profile including Fuel type " +
                        "\n      2) Fuel settings under '" + mL2Configuration.getText() + "'" +
                        "\n      3) DHFProcess List data under '" + mL2Configuration.getText() + "'" +
                        "\n      4) Performance Data under '" + perfMenu.getText() + "'" +
                        "\n\nIt is suggested that the profile with Fuel is finalised before updating" +
                        "\nthe other data." +
                        "\n\nIf any changes are made, then before exiting, make sure to save/update profile.");
            }
            else {
                showError("Level2 could not be started. Aborting ...");
                System.exit(1);
            }
        }
        else
            showError("Unable to get Installer Access :" + status.getErrorMessage());
    }

    protected void startLog4j() {
        PropertyConfigurator.configure("log4jI.properties");
        log = Logger.getLogger("level2.INSTALLER");
    }

    public static L2AccessControl.AccessLevel defaultLevel() {
        return L2AccessControl.AccessLevel.INSTALLER;
    }

    protected ErrorStatAndMsg getFieldPerformanceList(String basePath) {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg();
        File file = getParticularFile(basePath, profileCode, "perfData");
        if (file != null) {
            if (decide("Field Performance Data", "<html>Field-updated Performance Data File is available" +
                    "<br />   Do you want load this, overwriting Data with the Furnace Profile</html>")) {
                if (loadThePerformanceList(file)) {
                    if (markThisFileAsBak(file)) {
                        showMessage("Field Performance Data", "<html>Combined the Field Performance data with Furnace Profile." +
                                "<br />The existing Field Performance file is marked as *.bak." +
                                "<br />Before exiting the application, ensure to Update the furnace file " +
                                "with <b><font color='blue'>" + mIUpdateFurnace.getText()  + "</font></b> " +
                                "from <b><font color='blue'>" + fileMenu.getText() + "</font></b> " +
                                "menu.</html>");
                        markPerfTobeSaved(true);
                    }
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
        DataWithStatus<String> pathStatus =
                FileChooserWithOptions.getOneExistingFilepath(fceDataLocation, L2AccessControl.installerAccessFileExtension, true);
        if (pathStatus.getStatus() == DataStat.Status.OK) {
            try {
                installerAccessControl = new L2AccessControl(pathStatus.getValue(), true); // only if file exists
            } catch (Exception e) {
                retVal.addErrorMessage(e.getMessage());
            }
        }
        else
            retVal.addErrorMessage(pathStatus.getErrorMessage());
        return retVal;
    }

    protected boolean authenticate() {
        boolean retVal = false;
        StatusWithMessage stm = installerAccessControl.authenticate(accessLevel, "Re-confirm authority");
        if (stm.getDataStatus() == DataStat.Status.OK)
            retVal = true;
        return retVal;
    }

    protected JMenu createFileMenu() {
        defineFileMenu();
//        fileMenu = new JMenu("File");
        fileMenu.add(mIGetFceProfile);
        fileMenu.add(mISaveFceProfile);
        fileMenu.addSeparator();
        fileMenu.add(mIUpdateFurnace);
        fileMenu.addSeparator();
        fileMenu.add(mIExit);
        return fileMenu;
    }

    JMenu createAccessMenu() {
        mAccessControl = new JMenu("Access Control");
        mAccessControl.add(mExpertAccess);
        mAccessControl.add(mUpdaterAccess);
        mAccessControl.add(mRuntimeAccess);
        return mAccessControl;
    }

    public boolean canClose() {
        boolean goAhead = true;
        if (furnace != null && furnace.isPerformanceToBeSaved())
            goAhead = decide("Unsaved Performance Data", "<html>Some Performance/ Process data have been collected." +
                    "<br />To save the updated data, choose NO now and then Update the furnace file " +
                    "with <b><font color='blue'>" + mIUpdateFurnace.getText()  + "</font></b> " +
                    "from <b><font color='blue'>" + fileMenu.getText() + "</font></b> menu."  +
                    "<br /><br /><font color = 'red'>Selecting YES now will ABANDON collected Performance data.</font></html>", false);
        return goAhead;
    }


    protected JMenu createPerformanceMenu() {
        definePerformanceMenu();
//       perfMenu = new JMenu("Performance");
        perfMenu.add(mIAddToPerfBase);
        perfMenu.addSeparator();
        perfMenu.add(mIShowPerfBase);
        perfMenu.addSeparator();
        perfMenu.add(mIReadPerformanceData);
        return perfMenu;
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
