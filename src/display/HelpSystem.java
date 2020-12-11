package display;

import mvUtils.display.ErrorStatAndMsg;
import mvUtils.jsp.JSPConnection;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

import javax.swing.*;
import java.io.*;
import java.util.Hashtable;

public class HelpSystem {
    boolean fromDB = false;
    JSPConnection jspConnection = null;
    int appCode;
    String folder;
    JPanel helpPan = new JPanel();

    public HelpSystem(String folder) {
        this.folder = folder;
    }

    public HelpSystem(JSPConnection jspConnection, int appCode, String folder) {
        this.jspConnection = jspConnection;
        this.appCode = appCode;
        this.folder = folder;
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

    public boolean getHelpPagesFromFile() {
        boolean retVal = true;
        JTabbedPane jtp = new JTabbedPane();
        String helpFolder = folder + "\\";
        File folder = new File(helpFolder);
        String[] fileNames = folder.list();
        for (String oneFile : fileNames) {
            if (oneFile.endsWith(".html")) {
                String filePath = helpFolder + oneFile;
                try {
                    BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(filePath));
                    File f = new File(filePath);
                    long len = f.length();
                    if (len > 20 && len < 50000) {
                        int iLen = (int) len;
                        byte[] data = new byte[iLen + 100];
                        try {
                            if (iStream.read(data) == len) {
                                String htmlString = new String(data);
                                int nameLocStart = htmlString.indexOf("helpName");
                                String helpName = htmlString.substring(nameLocStart, nameLocStart + 50);
                                helpName = helpName.split("'")[1];
                                jtp.addTab(helpName, new JLabel(htmlString));
                            }
                            iStream.close();
                        } catch (IOException e) {
                            retVal = false;
                        }
                    }
                } catch (FileNotFoundException e) {
                    retVal = false;
                }
            }
        }
        if (retVal)
            helpPan.add(jtp);
        return retVal;
    }

    boolean getHelpPagesFromDB() {
        boolean retVal = false;
        Hashtable<String,String> query = new Hashtable<String, String>(){
            {put("appCode", ("" + appCode));}
        };
        JTabbedPane jtp = new JTabbedPane();
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
//                int nameLocStart = htmlString.indexOf("helpName");
//                String helpName = htmlString.substring(nameLocStart, nameLocStart + 50);
//                helpName = helpName.split("'")[1];
                jtp.addTab(helpName, new JLabel(htmlString));
                retVal = true;
                // check for more pages
                vpAll = XMLmv.getTag(xmlStr, "HelpPage", vpAll.endPos);
                htmlString = vpAll.val;
                takeHtml = htmlString.length() > 10;
            }
        }
        if (retVal)
            helpPan.add(jtp);
        return retVal;
    }

    public JPanel getHelpPanel() {
        return helpPan;
    }
}


