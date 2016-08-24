package directFiredHeating.accessControl;

import mvUtils.display.StatusWithMessage;
import mvUtils.file.AccessControl;
import mvUtils.file.AccessControlInFile;

import java.util.HashMap;

/**
 * User: M Viswanathan
 * Date: 25-Apr-16
 * Time: 3:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2AccessControl {
    public enum AccessLevel {NONE, RUNTIME, UPDATER, EXPERT, INSTALLER, CONFIGURATOR};

    static public String l2AccessfileExtension = "l2Acc";
    static public String installerAccessFileExtension = "instAcc";

    static HashMap<AccessLevel, AccessNameAndDescription> accessMap =  new HashMap<AccessLevel, AccessNameAndDescription>(){ {
        put(AccessLevel.NONE, new AccessNameAndDescription("None", "No Access"));
        put(AccessLevel.RUNTIME, new AccessNameAndDescription("Runtime", "Level2 - Runtime"));
        put(AccessLevel.UPDATER, new AccessNameAndDescription("Updater", "Level2 - Updater"));
        put(AccessLevel.EXPERT, new AccessNameAndDescription("Expert", "Level2 - Expert"));
        put(AccessLevel.INSTALLER, new AccessNameAndDescription("Installer", "Level2 - Installer"));
        put(AccessLevel.CONFIGURATOR, new AccessNameAndDescription("Configurator", "Level2 - Configurator"));
    }};

    AccessControl accessControl;

    public L2AccessControl(AccessControl.PasswordIntensity intensity) {
        accessControl = new AccessControl(intensity);
    }

    public L2AccessControl() {
        this(AccessControl.PasswordIntensity.HIGH);
    }

    public L2AccessControl(AccessControl.PasswordIntensity intensity, String filePath, boolean onlyIfExists) throws Exception {
        accessControl = new AccessControlInFile(intensity, filePath, onlyIfExists);
    }

    public L2AccessControl(String filePath, boolean onlyIfExists) throws Exception {
        this(AccessControl.PasswordIntensity.HIGH, filePath, onlyIfExists);
    }

    public StatusWithMessage authenticate(AccessLevel forLevel) {
        AccessNameAndDescription nd = accessMap.get(forLevel);
        return accessControl.getAndCheckPassword(nd.name, nd.description);
    }

    public StatusWithMessage authenticate(AccessLevel forLevel, String title) {
        AccessNameAndDescription nd = accessMap.get(forLevel);
        return accessControl.getAndCheckPassword(nd.name, title);
    }

    /**
     * creates a new access Control file with one access entry for 'forLevel'
     * @param forLevel
     * @return
     */
    public static StatusWithMessage initiateAccessFile(AccessLevel forLevel) {
        StatusWithMessage retVal = new StatusWithMessage();
        retVal.setErrorMessage("Not Ready for Access Control yet");
        return retVal;
    }

    public StatusWithMessage addNewUser(AccessLevel forLevel) {
        AccessNameAndDescription nd = accessMap.get(forLevel);
        return accessControl.getAndSaveNewAccess(nd.name, nd.description);
    }

    public StatusWithMessage deleteUser(AccessLevel forLevel) {
        AccessNameAndDescription nd = accessMap.get(forLevel);
        return accessControl.getAndDeleteAccess(nd.name, "Delete One " + nd.description);
    }

    public static String getDescription(AccessLevel forLevel) {
        return accessMap.get(forLevel).description;
    }

//    class AccessNameAndDescription {
//        String name;
//        String description;
//
//        AccessNameAndDescription(String name, String description) {
//            this.name = name;
//            this.description = description;
//        }
//    }
//
    static public void main(String[] args) {
        try {
            L2AccessControl access = new L2AccessControl("level2FceData/l2AccessData.txt", false);
            StatusWithMessage stm = access.addNewUser(AccessLevel.EXPERT);
            if (stm.getDataStatus() == StatusWithMessage.DataStat.OK)
                System.out.println("Done");
            else
                System.out.println(stm.getErrorMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
