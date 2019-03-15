package application;

import directFiredHeating.DFHeating;
import directFiredHeating.applications.L2Configurator;
import materials.EditFuelData;
import materials.EditThermalProperties;
import materials.FuelData;
import materials.ThermalProperties;
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
    String title = "Thermal Applications 20190308";
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

    JButton jbViewThermalProperties = new JButton("View-Thermal-Properties Module");
    String strViewThermalProperties = "<html>Viewing Thermal Properties of Materials</html>";

    JButton jbEditThermalProperties = new JButton("Edit-Thermal-Properties Module");
    String strEditThermalProperties = "<html>Adding/Editing Thermal Properties of Materials</html>";

    JButton jbViewFuelProperties= new JButton("View-Fuel-Properties Module");
    String strViewFuelProperties = "<html>Viewing Properties of Fuels</html>";

    JButton jbEditFuelProperties = new JButton("Edit-Fuel-Properties Module");
    String strEditFuelProperties = "<html>Adding/Editing Fuel Properties</html>";

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
        jbViewThermalProperties.addActionListener(bh);
        jbEditThermalProperties.addActionListener(bh);
        jbViewFuelProperties.addActionListener(bh);
        jbEditFuelProperties.addActionListener(bh);

        exitButton.addActionListener(bh);

        int buttonWidth = Math.max(jbViewThermalProperties.getPreferredSize().width, launchRTH.getPreferredSize().width);
        buttonWidth = Math.max(buttonWidth, launchL2Configurator.getPreferredSize().width);
        buttonSize = new Dimension(buttonWidth, launchL2Configurator.getPreferredSize().height);

        mainF = new JFrame(title);
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

        mainP.addItem(new DetailsField(jbViewThermalProperties, buttonSize, strViewThermalProperties));
        mainP.addBlank();
        mainP.addItem(new DetailsField(jbEditThermalProperties, buttonSize, strEditThermalProperties));
        mainP.addBlank();
        mainP.addItem(new DetailsField(jbViewFuelProperties, buttonSize, strViewFuelProperties));
        mainP.addBlank();
        mainP.addItem(new DetailsField(jbEditFuelProperties, buttonSize, strEditFuelProperties));
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
                launchDFHFurnace();
            } else if (src == launchRTH) {
                launchRTH();
            } else if (src == launchL2Configurator) {
                launchL2Configurator() ;
            } else if (src == jbViewThermalProperties) {
                launchViewThermalProperties() ;
            } else if (src == jbEditThermalProperties) {
                launchEditThermalProperties() ;
            } else if (src == jbViewFuelProperties) {
                launchFuelData();
            } else if (src == jbEditFuelProperties) {
                launchEditFuelData() ;
            } else if (src == exitButton) {
                quit();
            }
        }
    }

    void launchDFHFurnace() {
        new WaitMsg(null, "Starting DFHFurnace. Please wait ...", new ActInBackground() {
            public void doInBackground() {
                DFHeating trHeat = new DFHeating(true);
                if (trHeat.setItUp()) {
                    someAppLaunched = true;
                    quit();
                } else
                    trHeat.showError("  Unable to launch the Application.\nAborting ...");
            }
        });
    }

    void launchRTH() {
        new WaitMsg(null, "Starting RTH Furnace. Please wait ...", new ActInBackground() {
            public void doInBackground() {
                RTHeating rth = new RTHeating();
                if (rth.setItUp()) {
                    someAppLaunched = true;
                    quit();
                }
                else
                    rth.showError("  Unable to launch the Application.\nAborting ...");
            }
        });
    }

    void launchL2Configurator() {
        new WaitMsg(null, "Starting DFHL2L2Configurator. Please wait ...", new ActInBackground() {
            public void doInBackground() {
                L2Configurator l2C = new L2Configurator();
                if (l2C.setItUp()) {
                    someAppLaunched = true;
                    quit();
                }
                else
                    l2C.showError("  Unable to launch the Application.\nAborting ...");
            }
        });
    }

    void launchViewThermalProperties() {
        new WaitMsg(null, "Starting Thermal Properties. Please wait ...", new ActInBackground() {
            public void doInBackground() {
                ThermalProperties tp = new ThermalProperties();
                if (tp.setItUp()) {
                    someAppLaunched = true;
                    quit();
                }
                else
                    tp.showError("  Unable to launch the Application.\nAborting ...");
            }
        });
    }

    void launchEditThermalProperties() {
        new WaitMsg(null, "Starting Thermal Properties. Please wait ...", new ActInBackground() {
            public void doInBackground() {
                EditThermalProperties tp = new EditThermalProperties();
                if (tp.setItUp()) {
                    someAppLaunched = true;
                    quit();
                }
                else
                    tp.showError("  Unable to launch the Application.\nAborting ...");
            }
        });
    }

    void launchFuelData() {
        new WaitMsg(null, "Starting Fuel Properties. Please wait ...", new ActInBackground() {
            public void doInBackground() {
                FuelData fd = new FuelData();
                if (fd.setItUp()) {
                    someAppLaunched = true;
                    quit();
                }
                else
                    fd.showError("  Unable to launch the Application.\nAborting ...");
            }
        });
    }

    void launchEditFuelData() {
        new WaitMsg(null, "Starting Add/Edit Fuel Properties. Please wait ...", new ActInBackground() {
            public void doInBackground() {
                EditFuelData efd = new EditFuelData();
                if (efd.setItUp()) {
                    someAppLaunched = true;
                    quit();
                }
                else
                    efd.showError("  Unable to launch the Application.\nAborting ...");
            }
        });
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
