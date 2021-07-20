package display;

import directFiredHeating.DFHeating;
import mvUtils.display.EditResponse;
import mvUtils.display.ErrorStatAndMsg;
import mvUtils.display.MultiPairColPanel;
import mvUtils.jsp.JSPConnection;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.Hashtable;

public class HelpSystem {
    boolean fromDB = false;
    JSPConnection jspConnection = null;
    int appCode;
    String folder;
    MultiPairColPanel helpPan;
//    JPanel helpPan = new JPanel();
    int tabPosition = JTabbedPane.TOP;
    public HelpSystem(String folder, int tabPosition, String title) {
        this.folder = folder;
        this.tabPosition = tabPosition;
        helpPan = new MultiPairColPanel(title);
    }

    public HelpSystem(JSPConnection jspConnection, int appCode, String folder,
                      int tabPosition, String title) {
        this.jspConnection = jspConnection;
        this.tabPosition = tabPosition;
        this.appCode = appCode;
        this.folder = folder;
        helpPan = new MultiPairColPanel(title);
    }

    public HelpSystem(JSPConnection jspConnection, int appCode, String folder, String title) {
        this(jspConnection, appCode,folder, JTabbedPane.TOP, title);
    }

    public boolean loadHelp() {
        boolean retVal = false;
        // try DB if available
        if (jspConnection != null) {
            retVal = getHelpPagesFromDB();
        }
        // try Files if DB failed or not available
        if (!retVal)
            retVal = getHelpPagesFromFile();
        return retVal;
    }

    JComponent getHelpPage(String data) {
        JPanel p = new JPanel();
        JLabel jl = new JLabel(data);
        p.setBackground(java.awt.Color.white);
        p.add(jl);
        return p;
    }

    JTabbedPane jtp;

    public boolean getHelpPagesFromFile() {
        boolean retVal = false;
        jtp = new JTabbedPane(tabPosition);
        String helpFolder = folder + "\\";
        File folder = new File(helpFolder);
        if (folder.exists()) {
            String[] fileNames = folder.list();
            for (String oneFile : fileNames) {
                if (oneFile.endsWith(".html")) {
                    String filePath = helpFolder + oneFile;
                    try {
                        BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(filePath));
                        File f = new File(filePath);
                        long len = f.length();
                        if (len > 5 && len < 50000) {
                            int iLen = (int) len;
                            byte[] data = new byte[iLen + 100];
                            try {
                                if (iStream.read(data) == len) {
                                    String htmlString = new String(data);
                                    int nameLocStart = htmlString.indexOf("helpName");
                                    String helpName = htmlString.substring(nameLocStart, nameLocStart + 50);
                                    helpName = helpName.split("'")[1];
                                    if (helpName.equalsIgnoreCase("spacer"))
                                        jtp.addTab("", null);
                                    else
                                        jtp.addTab(helpName, getHelpPage(htmlString));
                                }
                                iStream.close();
                                retVal = true;
                            } catch (IOException e) {
                                break;
                            }
                        }
                    } catch (FileNotFoundException e) {
                        break;
                    }
                }  // if file ends with html
            }
        }
        if (retVal) {
            helpPan.addItem(jtp);
            DFHeating.logInfo("Help Pages Loaded from Folder " + helpFolder);
        }
        return retVal;
    }

    boolean getHelpPagesFromDB() {
        boolean retVal = false;
        Hashtable<String,String> query = new Hashtable<String, String>(){
            {put("appCode", ("" + appCode));}
        };
        jtp = new JTabbedPane(tabPosition);
        ErrorStatAndMsg jspResponse = jspConnection.getData("getHelpPages.jsp", query);
        if (!jspResponse.inError) {
            String xmlStr = jspResponse.msg;
            ValAndPos vpAll = XMLmv.getTag(xmlStr, "HelpPage");
            String htmlString = vpAll.val;
            boolean takeHtml = htmlString.length() > 10;
            while (takeHtml) {
                ValAndPos vp = XMLmv.getTag(htmlString, "Tab");
                String helpName = vp.val;
                vp = XMLmv.getTag(htmlString, "htmlText", vp.endPos);
                htmlString = vp.val;
                if (helpName.equalsIgnoreCase("spacer"))
                    jtp.addTab("", null);
                else
                    jtp.addTab(helpName, getHelpPage(htmlString));
                retVal = true;
                // check for more pages
                vpAll = XMLmv.getTag(xmlStr, "HelpPage", vpAll.endPos);
                htmlString = vpAll.val;
                takeHtml = htmlString.length() > 10;
            }
        }
        if (retVal) {
            helpPan.addItem(jtp);
            DFHeating.logInfo("Help Pages Loaded from DB");
        }
        return retVal;
    }

    public JPanel getHelpPanel() {
        return helpPan;
    }

    boolean helpFrameOpened = false;
    HelpDlg helpFrame;

    public void getHelpFrame(Component owner, String title) {
        if (!helpFrameOpened) {
            helpFrame = new HelpDlg(title);
            helpFrameOpened = true;
        }
        helpFrame.showHelp(owner);
    }

    class HelpDlg extends JDialog {
        HelpDlg(String title) {
            add(helpPan);
            pack();
            setTitle(title);
            setResizable(false);
        }

        void showHelp(Component owner) {
            setLocationRelativeTo(owner);
            setVisible(true);
        }
    }
}


