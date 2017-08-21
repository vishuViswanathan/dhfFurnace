package application;

import directFiredHeating.DFHeating;
import directFiredHeating.applications.L2Configurator;
import mvUtils.display.FramedPanel;
import mvUtils.display.MultiPairColPanel;
import mvUtils.file.ActInBackground;
import mvUtils.file.WaitMsg;
import radiantTubeHeating.RTHeating;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * Created by viswanathanm on 11-08-2017.
 */
public class ApplicationLauncher {
    int textFieldWidth = 400;

    JButton launchDFHFunace = new JButton("DFH Furnace Module");
    String dfhDetails = "<html>Direct Fired Heating Furnace" +
            "<p>- Top-only or Top-And-Bottom Heating" +
            "<p>or with inital Top-And-Bottom Heating followed by Top-Only Heating for Billets, Slabs" +
            "<p>- Top-and-Bottom Heating for Strips </html>";

    JButton launchRTH = new JButton("Radiant Tube Furnace Module");
    String rthDetails =
            "<html>Heating Furnace with Radiant Tubes, " +
                    "<b>Top and Bottom Heating for Strips</html>";

    JButton launchL2Configurator = new JButton("Level2 Configurator Module");
    String l2ConfiguratortDetails = "<html>Configurator for Level2 application for DFH for Strips</html>";

    JButton exitButton = new JButton("Exit");

    JFrame mainF;
    Dimension buttonSize = null;
    boolean someAppLaunched = false;

    public ApplicationLauncher() {
        init();
    }

    boolean init() {
        ButtonHandler bh = new ButtonHandler();

        launchDFHFunace.addActionListener(bh);
        launchRTH.addActionListener(bh);
        launchL2Configurator.addActionListener(bh);
        exitButton.addActionListener(bh);

        int buttonWidth = Math.max(launchDFHFunace.getPreferredSize().width, launchRTH.getPreferredSize().width);
        buttonWidth = Math.max(buttonWidth, launchL2Configurator.getPreferredSize().width);
        buttonSize = new Dimension(buttonWidth, launchL2Configurator.getPreferredSize().height);

        mainF = new JFrame();
        mainF.addWindowListener(new WinListener());
        mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainF.setSize(new Dimension(800, 600));

        MultiPairColPanel mainP = new MultiPairColPanel("");
        mainP.addItem(new JLabel("<html><h1>Thermal Applications for Furnaces</h1>"));

        mainP.addItem(new DetailsField(launchDFHFunace, buttonSize, dfhDetails));
        mainP.addBlank();
        mainP.addItem(new DetailsField(launchRTH, buttonSize, rthDetails));
        mainP.addBlank();
        mainP.addItem(new DetailsField(launchL2Configurator, buttonSize, l2ConfiguratortDetails));
        mainP.addBlank();
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

    class DetailsField extends FramedPanel {
        DetailsField(JButton button, Dimension size, String text) {
            super(new BorderLayout());
            JPanel bp = new JPanel();
            button.setPreferredSize(size);
            bp.add(button);
            add(bp, BorderLayout.WEST);
            add(new JLabel("<html><body width=" + textFieldWidth + ">" + text), BorderLayout.CENTER);
        }
    }

    class ButtonHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            someAppLaunched = false;
            Object src = e.getSource();
            if (src == launchDFHFunace) {
                someAppLaunched = launchDFHFurnace();
            } else if (src == launchRTH) {
                someAppLaunched = launchRTH();
            } else if (src == launchL2Configurator) {
                someAppLaunched = launchL2Configurator() ;
            } else if (src == exitButton) {
                quit();
            }
            if (someAppLaunched)
                quit();
        }
    }

    boolean launchDFHFurnace() {
        new WaitMsg(null, "Starting DFHFurnace. Please wait ...", new ActInBackground() {
            public void doInBackground() {
                    DFHeating trHeat = new DFHeating(true);
                    if (!trHeat.setItUp()) {
                        trHeat.showError("  Unable to Get Application Data.\nAborting ...");
                        System.exit(1);
                    }
            }
        });
        return true;
    }

    boolean launchRTH() {
        new WaitMsg(null, "Starting RTH Furnace. Please wait ...", new ActInBackground() {
            public void doInBackground() {
                RTHeating rth = new RTHeating();
                if (!rth.setItUp()) {
                    rth.showError("  Unable to Get Application Data.\nAborting ...");
                    System.exit(1);
                }
            }
        });
        return true;
    }

    boolean launchL2Configurator() {
        new WaitMsg(null, "Starting DFHL2L2Configurator. Please wait ...", new ActInBackground() {
            public void doInBackground() {
                L2Configurator l2C = new L2Configurator();
                if (!l2C.setItUp()) {
                    l2C.showError("  Unable to Get Application Data.\nAborting ...");
                    System.exit(1);
                }
            }
        });
        return true;
    }

    void quit() {
        mainF.setVisible(false);
        mainF.dispose();
        if (!someAppLaunched)
            System.exit(1);
    }


    class WinListener implements WindowListener {
        public void windowOpened(WindowEvent e) {
        }

        public void windowClosing(WindowEvent e) {
            quit();
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

    public static void main(String[] args) {
        ApplicationLauncher launcher = new ApplicationLauncher();
    }


}
