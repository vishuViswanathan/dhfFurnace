package level2.simulator;

import TMopcUa.TMuaClient;
import com.prosysopc.ua.ServiceException;
import display.SizedLabel;
import level2.common.L2Interface;
import level2.common.TagCreationException;
import mvUtils.display.FramedPanel;
import mvUtils.display.InputControl;
import mvUtils.display.MultiPairColPanel;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 21-Aug-15
 * Time: 12:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class OpcSimulator implements InputControl, L2Interface {
    static public enum TagAttrib {
         NAME("servermain:Name"),
         DATATYPE("servermain:DataType"),
         RW("servermain:ReadWriteAccess"),
         TAG("servermain:Tag"),
         TAGLIST("servermain:TagList"),
         TAGGROUP("servermain:TagGroup"),
         TAGGROUPLIST("servermain:TagGroupList"),
         CHANNELLIST("servermain:ChannelList"),
         CHANNEL("servermain:Channel"),
         DEVICELIST("servermain:DeviceList"),
         DEVICE("servermain:Device"),
         NOTFOUND("None");
         private final String modeName;

         TagAttrib(String modeName) {
             this.modeName = modeName;
         }

         public String getValue() {
             return name();
         }

         @Override
         public String toString() {
             return modeName;
         }

         public static TagAttrib getEnum(String text) {
             TagAttrib retVal = NOTFOUND;
             if (text != null) {
               for (TagAttrib b : TagAttrib.values()) {
                 if (text.equalsIgnoreCase(b.modeName)) {
                   retVal = b;
                     break;
                 }
               }
             }
             return retVal;
           }
     }

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
//        fp.add(new SizedLabel("The Process", new Dimension(100, 30), true), BorderLayout.NORTH);
        fp.add(new MultiPairColPanel("The Process"), BorderLayout.NORTH);
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
//        fp.add(new SizedLabel("From Level2", new Dimension(100, 40), true), BorderLayout.NORTH);
        fp.add(new MultiPairColPanel("From Level2"), BorderLayout.NORTH);
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
        fileDlg.setFile("*.xml");
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
                    if (len > 1000 && len < 1e6) {
                            if (takeDataFromXMLKEP(iStream))  {
                                showMessage("Simulator loaded");
                                level2Zones = collectSections("furnace:level2", false);
                                processZones = collectSections("furnace:process", true);
                                retVal = true;
                            }
                    } else
                        showError("File size " + len + " for " + filePath);
                    iStream.close();
                } catch (Exception e) {
                    showError("Some Problem in getting file!: " + e.getMessage());
                }
            }
        }
        return retVal;
    }

    OpcTagGroup allGroups;

    boolean takeDataFromXMLKEP(InputStream iStream) throws Exception {
        boolean retVal = false;
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        //Get the DOM Builder
        DocumentBuilder builder = factory.newDocumentBuilder();
        //Load and Parse the XML document
        //document contains the complete XML as a Tree.
        Document document =
                builder.parse(iStream);
        allGroups = new OpcTagGroup("Outer");
        OpcTagGroup oneChannel = null;
        //Iterating through the nodes and extracting the data.
        NodeList nodeList = document.getDocumentElement().getChildNodes();
        int nodeListLen = nodeList.getLength();
        for (int i = 0; i < nodeListLen; i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                if (TagAttrib.getEnum(node.getNodeName()) == TagAttrib.CHANNELLIST) {
                    NodeList channelNodeList = node.getChildNodes();
                    int channelListLen = channelNodeList.getLength();
                    for (int c = 0; c < channelListLen; c++) {
                        Node channelNode = channelNodeList.item(c);
                        if (channelNode instanceof Element) {
                            if (TagAttrib.getEnum(channelNode.getNodeName()) == TagAttrib.CHANNEL) {
                                allGroups.addGroup(getOneChannel(channelNode.getChildNodes()));
                                retVal = true;
                            }
                        }
                    }
                }
            }
        }
        return retVal;
    }

    OpcTagGroup getOneChannel(NodeList nodes) {
         OpcTagGroup oneGrp = null;
         int tagGroupNodeLen = nodes.getLength();
         for (int j = 0; j < tagGroupNodeLen; j++) {
             Node oneNode = nodes.item(j);
             if (oneNode instanceof Element) {
                 switch (TagAttrib.getEnum(oneNode.getNodeName())) {
                     case NAME:
                         String channelName = oneNode.getLastChild().getTextContent().trim();
                         equipment = channelName;
                         oneGrp = new OpcTagGroup(channelName);
                         break;
                     case DEVICELIST:
                         NodeList nodeList = oneNode.getChildNodes();
                         int nodeListLen = nodeList.getLength();
                         for (int i = 0; i < nodeListLen; i++) {
                             //We have encountered an <employee> tag.
                             Node node = nodeList.item(i);
                             if (node instanceof Element)
                                 if (TagAttrib.getEnum(node.getNodeName()) == TagAttrib.DEVICE)
                                     oneGrp.addGroup(getOneDevise(node.getChildNodes()));
                         }
                         break;
                 }
             }
         }
         return oneGrp;
    }

    OpcTagGroup getOneDevise(NodeList nodes) {
         OpcTagGroup oneGrp = null;
         int tagGroupNodeLen = nodes.getLength();
         for (int j = 0; j < tagGroupNodeLen; j++) {
             Node oneNode = nodes.item(j);
             if (oneNode instanceof Element) {
                 switch (TagAttrib.getEnum(oneNode.getNodeName())) {
                     case NAME:
                         String channelName = oneNode.getLastChild().getTextContent().trim();
                         oneGrp = new OpcTagGroup(channelName);
                         break;
                     case TAGGROUPLIST:
                         NodeList nodeList = oneNode.getChildNodes();
                         int nodeListLen = nodeList.getLength();
                         for (int i = 0; i < nodeListLen; i++) {
                             //We have encountered an <employee> tag.
                             Node node = nodeList.item(i);
                             if (node instanceof Element)
                                 if (TagAttrib.getEnum(node.getNodeName()) == TagAttrib.TAGGROUP)
                                     oneGrp.addGroup(getTagGroup(node.getChildNodes()));
                         }
                         break;
                 }
             }
         }
         return oneGrp;

    }

    OpcTagGroup getTagGroup(NodeList nodes) {
         OpcTagGroup oneGrp = new OpcTagGroup();
         int tagGroupNodeLen = nodes.getLength();
         for (int j = 0; j < tagGroupNodeLen; j++) {
             Node oneTagGroup = nodes.item(j);
             if (oneTagGroup instanceof Element) {
                 switch (TagAttrib.getEnum(oneTagGroup.getNodeName())) {
                     case TAGGROUPLIST:
                         NodeList tagGroupNodes = oneTagGroup.getChildNodes();
                         int tagGroupListLen = tagGroupNodes.getLength();
                         for (int n = 0; n < tagGroupListLen; n++) {
                             Node tNode = tagGroupNodes.item(n);
                             if (tNode instanceof Element) {
                                 String c2 = oneTagGroup.getLastChild().getTextContent().trim();
                                 switch (TagAttrib.getEnum(tNode.getNodeName())) {
                                     case TAGGROUP:
                                         if (oneGrp != null)
                                             oneGrp.addGroup(getTagGroup(tNode.getChildNodes()));
                                         break;
                                 }
                             }
                         }
                         break;
                      case NAME:
                         String grpName = oneTagGroup.getLastChild().getTextContent().trim();
                         oneGrp.setName(grpName);
                         break;
                     case TAGLIST:
                         NodeList tagNodes = oneTagGroup.getChildNodes();
                         int tagListLen = tagNodes.getLength();
                         for (int n = 0; n < tagListLen; n++) {
                             Node tNode = tagNodes.item(n);
                             if (tNode instanceof Element) {
                                 String c2 = oneTagGroup.getLastChild().getTextContent().trim();
                                 switch (TagAttrib.getEnum(tNode.getNodeName())) {
                                     case TAG:
                                         if (oneGrp != null)
                                             oneGrp.addTag(getTag(tNode.getChildNodes()));
                                         break;
                                 }
                             }
                         }
                         break;
                 }
             }
         }
         return oneGrp;
    }

    OpcTag getTag(NodeList nodes) {
         OpcTag tag = new OpcTag();
         int childNodesLen = nodes.getLength();
         for (int n = 0; n < childNodesLen; n++) {
             Node cNode = nodes.item(n);
             //Identifying the child tag of employee encountered.
             if (cNode instanceof Element) {
                 String content = cNode.getLastChild().
                         getTextContent().trim();
                 switch (TagAttrib.getEnum(cNode.getNodeName())) {
                     case NAME:
                         tag.name = content;
                         break;
                     case DATATYPE:
                         tag.dataType = content;
                         break;
                     case RW:
                         tag.readWriteAccess = content;
                         tag.rW = content.equalsIgnoreCase("Read/Write");
                         break;
                     case NOTFOUND:
                         break;
                 }
             }
         }
         return tag;
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

    Vector<OneSection> collectSections(String path, boolean rw) {
        Vector<OneSection> retVal = new Vector<OneSection>();
        OpcTagGroup grp = allGroups.getSubGroup(path);
        try {
            for (OpcTagGroup subGrp: grp.subGroups)
                retVal.add(new OneSection(this, subGrp, rw));
        } catch (Exception e) {
            showError("Some problem in creating Collection for " + path + "> " + e.getMessage());
        }
        return retVal;
    }

    boolean takeProcessFromXMLKEP(String xmlStr) {
        boolean retVal = true;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "servermain:TagGroupList", 0);
        String allGroups = vp.val;
        vp.endPos = 0;
        int s = 0;
        if (allGroups.length() > 10) {
            do {
                vp = XMLmv.getTag(allGroups, "servermain:TagGroup", vp.endPos);
                if (vp.val.length() < 10)
                    break;
                try {
                   processZones.add(s++, new OneSection(this, vp.val, true, true));
                } catch (TagCreationException e) {
                   showError("Problem in setting process part of simulation: XML[" + vp.val + "] -> " + e.getMessage());
                   retVal = false;
                   break;
                }
            } while (true);
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

    boolean takeLevel2FromXMLKEP(String xmlStr) {
         boolean retVal = true;
         ValAndPos vp;
         vp = XMLmv.getTag(xmlStr, "servermain:TagGroupList", 0);
         String allGroups = vp.val;
         vp.endPos = 0;
         int s = 0;
         if (allGroups.length() > 10) {
             do {
                 vp = XMLmv.getTag(allGroups, "servermain:TagGroup", vp.endPos);
                 if (vp.val.length() < 10)
                     break;
                 try {
                     level2Zones.add(s++, new OneSection(this, vp.val, false, true));
                 } catch (TagCreationException e) {
                    showError("Problem in setting process part of simulation: XML[" + vp.val + "] -> " + e.getMessage());
                    retVal = false;
                    break;
                 }
             } while (true);
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

    public TMuaClient source() {
        return source;
    }

    public InputControl controller() {
        return this;
    }

    public String equipment() {
        return equipment;
    }

    public void info(String msg) {
        showMessage(msg);
    }

    public void error(String msg) {
        showError(msg);
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
