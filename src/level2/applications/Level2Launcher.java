package level2.applications;

import directFiredHeating.DFHeating;
import directFiredHeating.accessControl.L2AccessControl;
import display.QueryDialog;
import mvUtils.display.FramedPanel;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.SimpleDialog;
import mvUtils.display.StatusWithMessage;
import protection.MachineCheck;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

/**
 * User: M Viswanathan
 * Date: 25-Apr-16
 * Time: 12:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class Level2Launcher {
    int textFieldWidth = 400;
    JButton launchRuntime = new JButton("Runtime Module");
    String runtimeDetails = "Level2 Runtime is the basic Level2\n" +
            "This is to be started for Level2 control of the process. After starting, there is no operator action required";
    JButton launchUpdater = new JButton("Updater Module");
    String updatetDetails = "This has to be started, whenever it is required to update the furnace performance data from " +
            "the field results. The data based on the field results will overwrite the existing. For the updated data " +
            "to be used in the Runtime module, it will be necessary to save the Performance Data to the disc. " +
            "If the Runtime module is already up and running, the updated data saved in the disc will be automatically " +
            "reflected Runtime module." +
            "\n\nFrom this module access control of the Runtime module can be set." +
            "\n\nAt any time one can run either the Updater Module or the Expert Module. Accordingly it may be necessary " +
            "to exit from the Expert Module, if it is already running";
    JButton launchExpert = new JButton("Expert Module");
    String expertDetails = "This module handles the field Performance Update similar to the Updater Module. " +
            "Additionally one can change the basic Process data and add new Processes. " +
            "\n\nFrom this module access control of the Updater module can be set." +
            "\n\nAt any time one can run either the Expert Module or the Updater Module. Accordingly it may be necessary " +
            "to exit from the Updater Module, if it is already running";
    JButton launchInstaller = new JButton("Install Level2");
    String installerDetails = "Apart from installing the Level2, the access control of Expert Module can be set";

    JButton exitButton = new JButton("Exit");

    JFrame mainF;

//    String opcIP = "opc.tcp://127.0.0.1:49320";
    L2AccessControl accessControl;
    String fceDataLocation = "level2FceData/";
    String accessDataFile = fceDataLocation + "l2AccessData.txt";

    public Level2Launcher() {
        if (!testMachineID()) {
            showError("Software key mismatch, Aborting ...");
            System.exit(1);
        }

        StatusWithMessage pathStatus = getAccessFilePath();
        if (pathStatus.getDataStatus() == StatusWithMessage.DataStat.OK) {
            try {
                accessControl = new L2AccessControl(accessDataFile, true); // only if file exists
                init();
            } catch (Exception e) {
                showError(e.getMessage());
                System.exit(1);
            }
        }
        else {
            showError(pathStatus.getErrorMessage() + "\n\n    ABORTING");
            System.exit(1);
        }
    }

    StatusWithMessage getAccessFilePath() {
        StatusWithMessage retVal = new StatusWithMessage();
        File folder = new File(fceDataLocation);
        File[] files = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith("." + L2AccessControl.fileExtension);
            }
        });
        if (files.length < 1) {
            retVal.setErrorMessage("Unable to load Access Control!");
        }
        else if (files.length > 1) {
            retVal.setErrorMessage("There are more than one Access Control data in the folder!");
        }
        else {
            accessDataFile = files[0].getAbsolutePath();
        }
        return retVal;
    }

    // machine ID methods
    boolean testMachineID() {
        boolean keyOK = false;
        boolean newKey = false;
        MachineCheck mc = new MachineCheck();
        String machineId = mc.getMachineID();
        String key = getKeyFromFile();
        do {
            if (key.length() < 5) {
                key = getKeyFromUser(machineId);
                newKey = true;
            }
            if (key.length() > 5) {
                StatusWithMessage keyStatus =  mc.checkKey(key);
                boolean tryAgain = false;
                switch(keyStatus.getDataStatus()) {
                    case WithErrorMsg:
                        showError(keyStatus.getErrorMessage());
                        break;
                    case WithInfoMsg:
                        boolean response = decide("Software key", "There is some problem in the saved key\n"
                                + " Do you want to delete the earlier key data and enter the key manually?");
                        if (response) {
                            key = "";
                            tryAgain = true;
                        }
                        break;
                    default:
                        keyOK = true;
                        break;
                }
                if (tryAgain)
                    continue;
                if (keyOK && newKey)
                    saveKeyToFile(key);
            }
            break;
        } while (true);
        return keyOK;
    }

    String keyFileHead = "TMIDFHLevel2Key:";

    void saveKeyToFile(String key) {
        boolean done = false;
        String filePath = fceDataLocation + "machineKey.ini";
//        debug("Data file name for saving key:" + filePath);
        try {
            BufferedOutputStream oStream = new BufferedOutputStream(new FileOutputStream(filePath));
            oStream.write(keyFileHead.getBytes());
            oStream.write(key.getBytes());
            oStream.close();
            done = true;
        } catch (FileNotFoundException e) {
            showError("Could not create file " + filePath);
        } catch (IOException e) {
            showError("Some IO Error in writing to file " + filePath + "!");
        }
        if (done)
            showMessage("key saved to " + filePath);
        else
            showError("Unable to save software key");
    }

    String getKeyFromFile() {
        String key = "";
        String filePath = fceDataLocation + "machineKey.ini";
//        debug("Data file name :" + filePath);
        try {
            BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(filePath));
            //           FileInputStream iStream = new FileInputStream(fileName);
            File f = new File(filePath);
            long len = f.length();
            int headLen = keyFileHead.length();
            if (len > headLen && len < 100) {
                int iLen = (int) len;
                byte[] data = new byte[iLen];
                iStream.read(data);
                String dataStr = new String(data);
                if (dataStr.substring(0, headLen).equals(keyFileHead))
                    key = dataStr.substring(headLen);
            } else
                showError("File size " + len + " for " + filePath);
        } catch (Exception e) {
            showError("Some Problem in Software Key!");
        }
        return key;
    }

    String getKeyFromUser(String machineID) {
        QueryDialog dlg = new QueryDialog(mainF, "Software keyString");
        JTextField mcID = new JTextField(machineID);
        mcID.setEditable(false);
        JTextField keyF = new JTextField(machineID.length() + 1);
        dlg.addQuery("Installation ID", mcID);
        dlg.addQuery("Enter key for the above installation ID", keyF);
        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);
        if (dlg.isUpdated())
            return keyF.getText();
        else
            return "";
    }

    Dimension buttonSize = null;

    boolean init()  {
        ButtonHandler bh = new ButtonHandler();
        launchRuntime.addActionListener(bh);
        launchUpdater.addActionListener(bh);
        launchExpert.addActionListener(bh);
        launchInstaller.addActionListener(bh);
        exitButton.addActionListener(bh);
        int buttonWidth = Math.max(launchRuntime.getPreferredSize().width, launchUpdater.getPreferredSize().width);
        buttonWidth = Math.max(buttonWidth, launchExpert.getPreferredSize().width);
        buttonWidth = Math.max(buttonWidth, launchInstaller.getPreferredSize().width);
        buttonSize = new Dimension(buttonWidth, launchInstaller.getPreferredSize().height);

        mainF = new JFrame();
//        mainF.addWindowListener(new WinListener());
//        mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainF.setSize(new Dimension(800, 600));

        MultiPairColPanel mainP= new MultiPairColPanel("");
        mainP.addItem(new JLabel("<html><h1>Level2 for Strip Process Furnace</h1>"));

        mainP.addItem(new DetailsField(launchRuntime, buttonSize, runtimeDetails));
        mainP.addBlank();
        mainP.addItem(new DetailsField(launchUpdater, buttonSize, updatetDetails));
        mainP.addBlank();
        mainP.addItem(new DetailsField(launchExpert, buttonSize, expertDetails));
        mainP.addBlank();
        mainP.addItem(new DetailsField(launchInstaller, buttonSize, installerDetails));
        mainP.addBlank();
        mainP.addItem(exitButton);
        mainF.add(mainP);

        mainF.pack();
        mainF.setResizable(false);
        mainF.setLocationRelativeTo(null);
        mainF.setVisible(true);
        mainF.setFocusable(true);
        mainF.toFront();
        return true;
    }

    void launchRuntime() {
        StatusWithMessage stm = accessControl.authenticate(L2DFHeating.defaultLevel());
        if (stm.getDataStatus() == StatusWithMessage.DataStat.OK) {
            L2DFHeating l2 = new L2DFHeating("Furnace", true);
            if (l2.l2SystemReady)
                quit();
        }
        else
            showError(stm.getErrorMessage());
    }

    void launchUpdater() {
        StatusWithMessage stm = accessControl.authenticate(L2Updater.defaultLevel());
        if (stm.getDataStatus() == StatusWithMessage.DataStat.OK) {
            L2DFHeating l2 = new L2Updater("Furnace", true);
            if (l2.l2SystemReady)
                quit();
        }
        else
            showError(stm.getErrorMessage());
    }

    void launchExpert() {
        StatusWithMessage stm = accessControl.authenticate(Level2Expert.defaultLevel());
        if (stm.getDataStatus() == StatusWithMessage.DataStat.OK) {
            L2DFHeating l2 = new Level2Expert("Furnace", true);
            if (l2.l2SystemReady)
                quit();
        }
        else
            showError(stm.getErrorMessage());
    }

    void launchInstaller() {
        new Level2Installer("Furnace", true);
        quit();
    }

    boolean getAndSaveOPCIP() {
        boolean retVal = false;
        return retVal;
    }

    boolean saveOPCIP() {
        return false;
    }

    public void showMessage(String msg) {
        showMessage("", msg);
    }

    public void showMessage(String title, String msg) {
        SimpleDialog.showMessage(mainF, title, msg);
        mainF.toFront();
    }

    public void showError(String msg) {
        showError("", msg);
    }

    public void showError(String title, String msg) {
        SimpleDialog.showError(mainF, title, msg);
        if (mainF != null)
            mainF.toFront();
    }

    public boolean decide(String title, String msg) {
        return decide(title, msg, true);
    }

    public boolean decide(String title, String msg, boolean defaultOption) {
        int resp = SimpleDialog.decide(null, title, msg, defaultOption);
        if (resp == JOptionPane.YES_OPTION)
            return true;
        else
            return false;
    }

    public boolean decide(String title, String msg, int forTime) {
        return SimpleDialog.decide(null, title, msg, forTime);
    }
    void quit() {
        mainF.setVisible(false);
        mainF.dispose();
    }

    class DetailsField extends FramedPanel {
        DetailsField (JButton button, Dimension size, String text) {
            super(new BorderLayout());
            JPanel bp = new JPanel();
            bp.add(button);
            add(bp, BorderLayout.WEST);
            add(new JLabel("<html><body width=" + textFieldWidth + ">" + text), BorderLayout.CENTER);
        }
    }

    class ButtonHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (src == launchRuntime)  {
                launchRuntime();
            }
            else if (src == launchUpdater)  {
                launchUpdater();
            }
            else if (src == launchExpert)  {
                launchExpert();
            }
            else if (src == launchInstaller) {
                launchInstaller();
            }
            else if (src == exitButton) {
                quit();
            }
        }
    }

    public static void main(String[] args) {
        Level2Launcher launcher = new Level2Launcher();
    }
}
