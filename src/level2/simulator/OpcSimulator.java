package level2.simulator;

import TMopcUa.TMuaClient;
import com.prosysopc.ua.ServiceException;
import com.sun.xml.internal.ws.encoding.xml.XMLMessage;
import directFiredHeating.DFHeating;
import directFiredHeating.FceSection;
import display.SizedLabel;
import level2.TagCreationException;
import mvUtils.display.FramedPanel;
import mvUtils.display.InputControl;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 21-Aug-15
 * Time: 12:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class OpcSimulator implements InputControl {
    JFrame mainF;
    TMuaClient source;
    static String uaServerURI;
    String equipment;

    Vector<OneSection> processZones;
    Vector<OneSection> level2Zones;

    boolean uiReady = false;

    public OpcSimulator(String urlID) {
        mainF = new JFrame();
        mainF.addWindowListener(new WinListener());
        mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainF.setSize(new Dimension(800, 600));
        mainF.setVisible(true);
        mainF.toFront();
        if (setupUaClient(urlID))
            if (loadSimulator()) {
                showThem();
                uiReady = true;
            }
    }

    void showThem() {
        JPanel mainP= new JPanel(new BorderLayout());
        mainP.add(getProcessPane(), BorderLayout.WEST);
        mainP.add(getLevel2Pane(), BorderLayout.EAST);
        mainF.add(mainP);
        mainF.pack();
        mainF.setFocusable(true);
        mainF.toFront();

//        mainF.setVisible(true);
    }

    JComponent getProcessPane() {
        int width = 580;
        FramedPanel fp = new FramedPanel(new BorderLayout());
        fp.add(new SizedLabel("The Process", new Dimension(100, 30), true), BorderLayout.NORTH);
        JScrollPane processPane = new JScrollPane();
        processPane.setPreferredSize(new Dimension(width + 40, 700));
        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        JPanel oneP;
        for (OneSection sec: processZones) {
            oneP = sec.getDisplayPanel(width);
            jp.add(oneP, gbc);
            gbc.gridy++;
        }
        for (OneSection sec: processZones)
            sec.updateUI();
        processPane.setViewportView(jp);
        fp.add(processPane, BorderLayout.CENTER);
        return fp;
    }

    JComponent getLevel2Pane() {
        int width = 580;
        FramedPanel fp = new FramedPanel(new BorderLayout());
        fp.add(new SizedLabel("From Level2", new Dimension(100, 40), true), BorderLayout.NORTH);
        JScrollPane level2Pane = new JScrollPane();
        level2Pane.setPreferredSize(new Dimension(width + 40, 700));
        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        JPanel oneP;
        for (OneSection sec: level2Zones) {
            oneP = sec.getDisplayPanel(width);
            jp.add(oneP, gbc);
            gbc.gridy++;
        }
        for (OneSection sec: level2Zones)
            sec.updateUI();
        level2Pane.setViewportView(jp);
        fp.add(level2Pane, BorderLayout.CENTER);
        return fp;
     }

    public void setProcessZones(Vector<OneSection> processZones) {
        this.processZones = processZones;
    }

    boolean setupUaClient(String urlId) {
         boolean retVal = false;
         try {
             source = new TMuaClient(urlId);
             source.connect();
             retVal = source.isConnected();
         } catch (Exception e) {
             showError("Exception :" + e.getMessage());
             e.printStackTrace();
 //        } catch (URISyntaxException e) {
 //            showError("URISyntaxException :" + e.getMessage());
 //        } catch (SecureIdentityException e) {
 //            showError("SecureIdentityException :" + e.getMessage());
 //        } catch (IOException e) {
 //            showError("IOException :" + e.getMessage());
 //        } catch (SessionActivationException e) {
 //            showError("SessionActivationException :" + e.getMessage());
 //        } catch (ServerListException e) {
 //            showError("ServerListException :" + e.getMessage());
         }
         return retVal;
     }


    boolean loadSimulator() {
        boolean retVal = false;
        FileDialog fileDlg =
                new FileDialog(mainF, "Load Level1 side Simulator",
                        FileDialog.LOAD);
        fileDlg.setFile("*.simul");
        fileDlg.setVisible(true);
        fileDlg.toFront();
        String fileName = fileDlg.getFile();
        if (fileName != null) {
            String filePath = fileDlg.getDirectory() + fileName;
            if (!filePath.equals("nullnull")) {
                try {
                    BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(filePath));
                    //           FileInputStream iStream = new FileInputStream(fileName);
                    File f = new File(filePath);
                    long len = f.length();
                    if (len > 1000 && len < 1e5) {
                        int iLen = (int) len;
                        byte[] data = new byte[iLen];
                        if (iStream.read(data)== len) {
                            if (takeDataFromXMl(new String(data)))  {
                                showMessage("Simulator loaded");
                                retVal = true;
                            }
                        }
                    } else
                        showError("File size " + len + " for " + filePath);
                } catch (Exception e) {
                    showError("Some Problem in getting file!: " + e.getMessage());
                }
            }
        }
        return retVal;
    }

    boolean takeDataFromXMl(String xmlStr) {
        boolean retVal = false;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "Simulation", 0);
        vp = XMLmv.getTag(xmlStr, "nEquipment", 0);
        int nEquipment = Integer.valueOf(vp.val);
        int e = 1; // TODO only for one equipment now
        ValAndPos oneEquipmentVp = XMLmv.getTag(xmlStr, "Equipment" + ("" + e).trim(), vp.endPos);
        oneEquipmentVp = XMLmv.getTag(oneEquipmentVp.val, "equipName", 0);
        equipment = oneEquipmentVp.val;
        ValAndPos processVp = XMLmv.getTag(xmlStr, "Process", 0);
        oneEquipmentVp.endPos = processVp.endPos;  // note down for next search for "level2"
        if (takeProcessFromXML(processVp.val)) {
            ValAndPos level2Vp = XMLmv.getTag(xmlStr, "Level2", 0);
            retVal = takeLevel2FromXML(level2Vp.val);
        }
        return retVal;
    }

    boolean takeProcessFromXML(String xmlStr) {
        boolean retVal = true;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "nSections", 0);
        int nSections = Integer.valueOf(vp.val);
        processZones = new Vector<OneSection>(nSections);
        for (int s = 0; s < nSections; s++) {
            vp = XMLmv.getTag(xmlStr, "Section" + ("" + (s + 1)).trim(), vp.endPos);
            try {
                processZones.add(s, new OneSection(this, vp.val, true));
            } catch (TagCreationException e) {
                showError("Problem in setting process part of simulation: XML[" + vp.val + "] -> " + e.getMessage());
                retVal = false;
                break;
            }
        }
        return retVal;
    }

    boolean takeLevel2FromXML(String xmlStr) {
        boolean retVal = true;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "nSections", 0);
        int nSections = Integer.valueOf(vp.val);
        level2Zones = new Vector<OneSection>(nSections);
        for (int s = 0; s < nSections; s++) {
            vp = XMLmv.getTag(xmlStr, "Section" + ("" + (s + 1)).trim(), vp.endPos);
            try {
                level2Zones.add(s, new OneSection(this, vp.val, false));
            } catch (TagCreationException e) {
                showError("Problem in setting level2 part of simulation: XML\n [Error: " + e.getMessage() + "\n" + vp.val);
                retVal = false;
                break;
            }
        }
        return retVal;
    }


    public void showError(String msg) {
        JOptionPane.showMessageDialog(null, msg, "ERROR", JOptionPane.ERROR_MESSAGE);
        Window w = parent();
        if (w != null)
            w.toFront();
    }

    public void showMessage(String msg) {
        JOptionPane.showMessageDialog(parent(), msg, "FOR INFORMATION", JOptionPane.INFORMATION_MESSAGE);
        Window w = parent();
        if (w != null)
            w.toFront();
    }

    public boolean canNotify() {
        return false;
    }

    public void enableNotify(boolean b) {

    }

    public Window parent() {
        return null;
    }

    void close() {
        try {
            if (processZones != null)
                for (OneSection sec : processZones)
                    sec.closeSubscription();
            if (level2Zones != null)
                for (OneSection sec : level2Zones)
                    sec.closeSubscription();
//            messageSub.removeItems();
//            stripSub.removeItems();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        if (source != null)
            source.disconnect();
        System.exit(0);
    }

    class WinListener implements WindowListener {
        public void windowOpened(WindowEvent e) {
        }

         public void windowClosing(WindowEvent e) {
             close();
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

    public static void main(String[] args) {
        if (args.length > 0) {
            OpcSimulator opcS = new OpcSimulator(args[0]);
        }
    }
}
