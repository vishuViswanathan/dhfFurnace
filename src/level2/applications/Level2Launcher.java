package level2.applications;

import level2.accessControl.L2AccessControl;
import mvUtils.display.FramedPanel;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.SimpleDialog;
import mvUtils.display.StatusWithMessage;
import mvUtils.file.AccessControl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
        try {
            accessControl = new L2AccessControl(accessDataFile, true); // only if file exists
            init();
        } catch (Exception e) {
            showError(e.getMessage());
            System.exit(1);
        }
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
        new Level2Configurator("Furnace", true);
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
