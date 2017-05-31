package level2.applications;

import directFiredHeating.accessControl.L2AccessControl;
import display.QueryDialog;
import mvUtils.display.*;
import mvUtils.file.AccessControl;
import mvUtils.file.ActInBackground;
import mvUtils.file.FileChooserWithOptions;
import mvUtils.file.WaitMsg;
import mvUtils.security.MachineCheck;

import javax.swing.*;
import javax.xml.transform.OutputKeys;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * User: M Viswanathan
 * Date: 25-Apr-16
 * Time: 12:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class Level2Launcher {
    int textFieldWidth = 400;
    JButton launchRuntime = new JButton("Runtime Module");
    String runtimeDetails = "<html>Level2 Runtime is the basic Level2" +
            "<p>This is to be started for Level2 control of the process. " +
            "After starting, there is no operator action required</html>";
    JButton launchUpdater = new JButton("Updater Module");
    String updatetDetails =
            "<html>This has to be started, whenever it is required to update the furnace performance data from " +
            "the field results. The data based on the field results will overwrite the existing. For the updated data " +
            "to be used in the Runtime module, it will be necessary to save the Performance Data to the disc. " +
            "If the Runtime module is already up and running, the updated data saved in the disc will be automatically " +
            "reflected Runtime module." +
            "<p>From this module access control of the Runtime module can be set." +
            "<p>At any time one can run either the Updater Module or the Expert Module. Accordingly it may be necessary " +
            "to exit from the Expert Module, if it is already running." +
            "<p>The necessary pre-condition is that Runtime Module must be ON</html>";
    JButton launchExpert = new JButton("Expert Module");
    String expertDetails = "<html>This module handles the field Performance Update similar to the Updater Module. " +
            "Additionally one can add new Processes based on Field Results and create corresponding Performance Data. " +
            "<p>From this module access control of the Updater module can be set." +
            "<p>At any time one can run either the Expert Module or the Updater Module. Accordingly it may be necessary " +
            "to exit from the Updater Module, if it is already running." +
            "<p>The necessary pre-condition is that Runtime Module must be ON</html>";
    JButton launchInstaller = new JButton("Install Level2");
    String installerDetails = "Apart from installing the Level2, the access control of Expert Module can be set";

    JButton exitButton = new JButton("Exit");

    JFrame mainF;

    L2AccessControl l2AccessControl;
    L2AccessControl installerAccessControl;
    String l2BasePath;
    String fceDataLocation = "level2FceData/";
    String lockPath;
    File lockFile;
    boolean someAppLaunched = false;
    int appCode = 104;
    public Level2Launcher() {
        boolean allOk = false;
        File folder = new File("");
        l2BasePath = folder.getAbsolutePath();
//        System.out.println("l2BasePath = " + l2BasePath);
        fceDataLocation = l2BasePath + "\\" + fceDataLocation;
        lockPath = fceDataLocation + "Syncro.lock";
        if (testMachineID()) {
            init();
            lockFile = new File(lockPath);
            allOk = true;
        } else {
            showError("Software key mismatch, Aborting ...");
            allOk = false;
            ;
        }
        if (!allOk)
            System.exit(1);
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
        } else
            retVal.addErrorMessage(pathStatus.getErrorMessage());
        return retVal;
    }

    boolean getAccessToLevel2(L2AccessControl.AccessLevel forLevel) {
        boolean retVal = false;
        StatusWithMessage status = new StatusWithMessage();
        if (l2AccessControl == null) {
            DataWithStatus<String> pathStatus =
                    FileChooserWithOptions.getOneExistingFilepath(fceDataLocation, L2AccessControl.l2AccessfileExtension, true);
            if (pathStatus.getStatus() == DataStat.Status.OK) {
                try {
                    l2AccessControl = new L2AccessControl(pathStatus.getValue(), true); // only if file exists
                } catch (Exception e) {
                    status.addErrorMessage(e.getMessage());
                }
            } else
                status.addErrorMessage(pathStatus.getErrorMessage());
        }
        if (status.getDataStatus() == DataStat.Status.OK) {
            status = l2AccessControl.authenticate(forLevel);
        }
        if (status.getDataStatus() == DataStat.Status.OK)
            retVal = true;
        else
            showError(status.getErrorMessage());
        return retVal;
    }

    // machine ID methods
    boolean testMachineID() {
        boolean keyOK = false;
        boolean newKey = false;
        MachineCheck mc = new MachineCheck();
        String machineId = mc.getMachineID();
        if (machineId.length() > 2) {
            String key = getKeyFromFile();
            do {
                if (key.length() < 5) {
                    key = getKeyFromUser(machineId);
                    newKey = true;
                }
                if (key.length() > 5) {
                    StatusWithMessage keyStatus = mc.checkKey(key);
                    boolean tryAgain = false;
                    switch (keyStatus.getDataStatus()) {
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
        }
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
            iStream.close();
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

    boolean init() {
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
        mainF.addWindowListener(new WinListener());
        mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainF.setSize(new Dimension(800, 600));

        MultiPairColPanel mainP = new MultiPairColPanel("");
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

    String getStatusFileName(L2AccessControl.AccessLevel level) {
        return fceDataLocation + l2AccessControl.getDescription(level) + " is ON";
    }

    DataWithStatus<Boolean> isAnyAppActive(L2AccessControl.AccessLevel[] levels) {
        DataWithStatus<Boolean> retVal = new DataWithStatus<>();
        retVal.setValue(false);
        FileLock lock = getFileLock();
        if (lock != null) {
            File f;
            for (L2AccessControl.AccessLevel level : levels) {
                f = new File(getStatusFileName(level));
                if (f.exists()) {
                    retVal.setValue(true);
                    break;
                }
            }
            releaseLock(lock);
        } else
            retVal.setErrorMsg("Unable to get the lock to get Status of applications ");
        return retVal;
    }

    FileLock getFileLock() {
        FileLock lock = null;
        int count = 5;
        while (--count > 0) {
            lock = getTheLock();
            if (lock == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            break;
        }
        return lock;
    }

    public FileLock getTheLock() {
        FileLock lock = null;
        try {
            FileChannel channel = new RandomAccessFile(lockFile, "rw").getChannel();
            lock = channel.tryLock();
        } catch (IOException e) {
            lock = null;
        }
        return lock;
    }

    public void releaseLock(FileLock lock) {
        try {
            lock.release();
        } catch (IOException e) {
            showError("Some Error in releasing file Lock");
        }
    }

    void launchRuntime() {
        DataWithStatus<Boolean> stat = isAnyAppActive(
                new L2AccessControl.AccessLevel[]{L2AccessControl.AccessLevel.RUNTIME,
                        L2AccessControl.AccessLevel.INSTALLER});
        if (stat.getStatus() == DataStat.Status.OK) {
            if (!stat.getValue()) {
                if (getAccessToLevel2(L2DFHeating.defaultLevel())) {
                    new WaitMsg(mainF, "Starting Level2Runtime. Please wait ...", new ActInBackground() {
                        public void doInBackground() {
                            L2DFHeating l2 = new L2Runtime("Furnace", true);
                            l2.setLoadTesting(loadTesting);
                            System.out.println("RUNTIME: l2.l2SystemReady = " + l2.l2SystemReady);
                            if (l2.l2SystemReady) {
                                someAppLaunched = true;
                                quit();
                            }
                        }
                    });
                }
            } else
                showError("One of level2 Runtime/ Installer is running");
        } else
            showError(stat.getErrorMessage());
    }

    void launchUpdater() {
        DataWithStatus<Boolean> stat = isAnyAppActive(
                new L2AccessControl.AccessLevel[]{L2AccessControl.AccessLevel.RUNTIME});
        if (stat.getStatus() == DataStat.Status.OK) {
            if (stat.getValue()) {
                stat = isAnyAppActive(
                        new L2AccessControl.AccessLevel[]{L2AccessControl.AccessLevel.UPDATER,
                                L2AccessControl.AccessLevel.EXPERT, L2AccessControl.AccessLevel.INSTALLER});
                if (stat.getStatus() == DataStat.Status.OK) {
                    if (!stat.getValue()) {
                        if (getAccessToLevel2(L2Updater.defaultLevel())) {
                            new WaitMsg(mainF, "Starting Level2Updater. Please wait ...", new ActInBackground() {
                                public void doInBackground() {
                                    L2DFHeating l2 = new L2Updater("Furnace", true);
                                    l2.setLoadTesting(loadTesting);
                                    System.out.println("EXPERT: l2.l2SystemReady = " + l2.l2SystemReady);
                                    if (l2.l2SystemReady) {
                                        someAppLaunched = true;
                                        quit();
                                    }
                                }
                            });
                        }
                    } else
                        showError("One of level2 Updater/ Expert/ Installer is running");
                } else
                    showError(stat.getErrorMessage());
            } else
                showError("Level2 RUNTIME is not ON");
        } else
            showError(stat.getErrorMessage());
    }

    void launchExpert() {
        DataWithStatus<Boolean> stat = isAnyAppActive(
                new L2AccessControl.AccessLevel[]{L2AccessControl.AccessLevel.RUNTIME});
        if (stat.getStatus() == DataStat.Status.OK) {
            if (stat.getValue()) {
                stat = isAnyAppActive(
                        new L2AccessControl.AccessLevel[]{L2AccessControl.AccessLevel.UPDATER,
                                L2AccessControl.AccessLevel.EXPERT, L2AccessControl.AccessLevel.INSTALLER});
                if (stat.getStatus() == DataStat.Status.OK) {
                    if (!stat.getValue()) {
                        if (getAccessToLevel2(Level2Expert.defaultLevel())) {
                            new WaitMsg(mainF, "Starting Level2Expert. Please wait ...", new ActInBackground() {
                                public void doInBackground() {
                                    L2DFHeating l2 = new Level2Expert("Furnace", true);
                                    l2.setLoadTesting(loadTesting);
                                    System.out.println("EXPERT: l2.l2SystemReady = " + l2.l2SystemReady);
                                    if (l2.l2SystemReady) {
                                        someAppLaunched = true;
                                        quit();
                                    }
                                }
                            });
                        }
                    } else
                        showError("One of level2 Updater/ Expert/ Installer is running");
                } else
                    showError(stat.getErrorMessage());
            } else
                showError("Level2 RUNTIME is not ON");
        } else
            showError(stat.getErrorMessage());
    }

    void launchInstaller() {
        DataWithStatus<Boolean> stat = isAnyAppActive(
                new L2AccessControl.AccessLevel[]{L2AccessControl.AccessLevel.EXPERT,
                        L2AccessControl.AccessLevel.UPDATER, L2AccessControl.AccessLevel.RUNTIME});
        if (stat.getStatus() == DataStat.Status.OK) {
            if (!stat.getValue()) {
                boolean allOK = (installerAccessControl != null);
                if (!allOK) {
                    StatusWithMessage status = getInstallerAccessFile();
                    if (status.getDataStatus() == DataStat.Status.OK)
                        allOK = true;
                    else
                        showError("Unable to get Installer Access :" + status.getErrorMessage());
                }
                if (allOK) {
                    StatusWithMessage stm = installerAccessControl.authenticate(Level2Installer.defaultLevel());
                    if (stm.getDataStatus() == DataStat.Status.OK) {
                        new WaitMsg(mainF, "Starting Level2Installer. Please wait ...", new ActInBackground() {
                            public void doInBackground() {
                                new Level2Installer("Furnace", true);
                                someAppLaunched = true;
                                quit();
                            }
                        });
                    } else
                        showError(stm.getErrorMessage());
                }
            } else
                showError("One of level2 Runtime/ Updater/ Expert is running");
        }
    }

    public void showMessage(String msg) {
        showMessage("", msg);
    }

    public void showMessage(String title, String msg) {
        SimpleDialog.showMessage(mainF, title, msg);
        if (mainF != null)
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
        if (!someAppLaunched)
            System.exit(1);
    }

    class DetailsField extends FramedPanel {
        DetailsField(JButton button, Dimension size, String text) {
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
            if (src == launchRuntime) {
                launchRuntime();
            } else if (src == launchUpdater) {
                launchUpdater();
            } else if (src == launchExpert) {
                launchExpert();
            } else if (src == launchInstaller) {
                launchInstaller();
            } else if (src == exitButton) {
                quit();
            }
        }
    }

    class WinListener implements WindowListener {
        public void windowOpened(WindowEvent e) {
        }

        public void windowClosing(WindowEvent e) {
            quit();
//            destroy();
//            if (asApplication)
//                System.exit(0);
//            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void windowClosed(WindowEvent e) {
        }

        public void windowIconified(WindowEvent e) {
        }

        public void windowDeiconified(WindowEvent e) {
        }

        public void windowActivated(WindowEvent e) {
        }

        public void windowDeactivated(WindowEvent e) {
        }
    }

    void debug(String msg) {
        System.out.println(msg);
    }

    static boolean loadTesting = false;

    static protected void parseCmdLineArgs(String[] args) {
        for (int a = 0; a < args.length; a++) {
            if (args[a].trim().equalsIgnoreCase("-loadTesting"))
                loadTesting = true;
        }
    }


    public static void main(String[] args) {
        parseCmdLineArgs(args);
        Level2Launcher launcher = new Level2Launcher();
    }
}
