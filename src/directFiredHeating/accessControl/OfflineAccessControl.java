package directFiredHeating.accessControl;

import mvUtils.display.StatusWithMessage;
import mvUtils.file.AccessControl;

import java.awt.*;

/**
 * User: M Viswanathan
 * Date: 25-May-16
 * Time: 4:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class OfflineAccessControl extends L2AccessControl {
    boolean asJNLP = false;
    Frame mainF;
    public OfflineAccessControl(AccessControl.PasswordIntensity intensity,  boolean asJNLP, Frame mainF) {
        super(intensity);
        this.mainF = mainF;
        this.asJNLP = asJNLP;
        if (asJNLP)
            accessControl.setAsJNLP();
        accessControl.setSuggestedExtension(L2AccessControl.installerAccessFileExtension);
    }

    public OfflineAccessControl(boolean asJNLP, Frame mainF) {
        this(AccessControl.PasswordIntensity.HIGH, asJNLP, mainF);
    }
    /**
     * This procedure ensured users selection of file both asJNLP and otherwise
     * @return
     */
    public StatusWithMessage loadAccessFile() {
        debug("in loadAccessFile, asJNLP = " + asJNLP);
        StatusWithMessage retVal = new StatusWithMessage();
        boolean done = true;
        if (!asJNLP) {
            done = false;
            String title = "Get Access Controls file";
            FileDialog fileDlg =
                    new FileDialog(mainF, title,
                            FileDialog.LOAD);
            fileDlg.setFile("*." + L2AccessControl.installerAccessFileExtension);
            fileDlg.setVisible(true);

            String bareFile = fileDlg.getFile();
            if (!(bareFile == null)) {
                accessControl.setFilePath(fileDlg.getDirectory() + bareFile);
                done = true;
            }
        }
        if (done) {
            debug("calling accessControl.readPasswordFile()");
            retVal = accessControl.readPasswordFile();
            debug("return from accessControl.readPasswordFile(): " + retVal.getDataStatus());
        }
        else
            retVal.setErrorMessage("Some problem in reading file");
        return retVal;
    }

    public StatusWithMessage saveAccessFile() {
        StatusWithMessage retVal = new StatusWithMessage();
        boolean done = true;
        if (!asJNLP) {
            done = false;
            String title = "Save Access Controls file";
            FileDialog fileDlg =
                    new FileDialog(mainF, title,
                            FileDialog.SAVE);
            fileDlg.setFile("*." + L2AccessControl.installerAccessFileExtension);
            fileDlg.setVisible(true);

            String bareFile = fileDlg.getFile();
            if (!(bareFile == null)) {
                accessControl.setFilePath(fileDlg.getDirectory() + bareFile);
                done = true;
            }
        }
        if (done)
            retVal = accessControl.saveToPasswordFile();
        else
            retVal.setErrorMessage("Some problem in reading file");
        return retVal;
    }

//    public StatusWithMessage addNewUser(AccessLevel forLevel) {
//        AccessNameAndDescription nd = accessMap.get(forLevel);
//        return accessControl.getAndSaveNewAccess(nd.name, nd.description);
//    }

    void debug(String msg) {
        System.out.println(msg);
    }
}
