package level2.simulator;

import TMopcUa.TMSubscription;
import TMopcUa.TMuaClient;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionAliveListener;
import level2.common.GetLevelResponse;
import level2.common.ReadyNotedParam;
import level2.common.*;
import mvUtils.display.FramedPanel;
import mvUtils.display.InputControl;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.SimpleDialog;
import org.opcfoundation.ua.builtintypes.DataValue;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;
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

    static public enum TagAttribNEW {
        NAME("common.ALLTYPES_NAME"),
        DATATYPE("servermain.TAG_DATA_TYPE"),
        RW("servermain.TAG_READ_WRITE_ACCESS>"),

        TAG("element"), // also "element"
        TAGLIST("tags"),

        TAGGROUP("element"),
        TAGGROUPLIST("tag_Groups"),

        CHANNELLIST("channels"),
        CHANNEL("element"),

        DEVICELIST("devices"),
        DEVICE("element"),
        NOTFOUND("None");
        private final String modeName;

        TagAttribNEW(String modeName) {
            this.modeName = modeName;
        }

        public String getValue() {
            return name();
        }

        @Override
        public String toString() {
            return modeName;
        }

        public static TagAttribNEW getEnum(String text) {
            TagAttribNEW retVal = NOTFOUND;
            if (text != null) {
                for (TagAttribNEW b : TagAttribNEW.values()) {
                    if (text.equalsIgnoreCase(b.modeName)) {
                        retVal = b;
                        break;
                    }
                }
            }
            return retVal;
        }
    }
    String urlID;
    JFrame mainF;
    TMuaClient source;
    static String uaServerURI;
    String equipment;

    Vector<OneSimulatorSection> processZones;
    Vector<OneSimulatorSection> level2Zones;
    boolean uiReady = false;
    protected String releaseDate = "20170329";


    public OpcSimulator(String urlID) {
        this.urlID = urlID;
        modifyJTextEdit();
        mainF = new JFrame();
        mainF.setTitle("Level1 Simulatior " + releaseDate);
        mainF.addWindowListener(new WinListener());
        mainF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainF.setSize(new Dimension(800, 600));
        mainF.setVisible(true);
        mainF.toFront();
        monitoredTags = new Hashtable<MonitoredDataItem, Tag>();
//        if (setupUaClient(urlID))
//            if (loadSimulator()) {
//                showThem();
//                uiReady = true;
//                createL2Messages();
//            }
    }

    void proceed() {
        if (setupUaClient(urlID))
            if (loadSimulator()) {
                showThem();
                uiReady = true;
                createL2Messages();
            }
    }

//    void setDisabledColor(Color color) {
//        UIManager.put("JTextField.disabledBackground", color);
//        UIManager.put("JComboBox.disabledBackground", color);
//        UIManager.put("JTextField.disabledBackground", color);
//        UIManager.put("JTextArea.disabledBackground", color);
//    }

    void showThem() {
//        setDisabledColor(Color.DARK_GRAY);
        JPanel mainP= new JPanel(new BorderLayout());
        addLoadTestPanel(mainP);
        mainP.add(getProcessPane(), BorderLayout.WEST);
        mainP.add(getLevel2Pane(), BorderLayout.EAST);
        mainF.add(mainP);
        mainF.pack();
        mainF.setFocusable(true);
        mainF.toFront();
        startDisplayUpdater();

//        mainF.setVisible(true);
    }

    protected void addLoadTestPanel(JPanel mainP) {

    }

    JComponent getProcessPane() {
        int width = 620;
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
        for (OneSimulatorSection sec: processZones) {
//            if (!sec.name.equalsIgnoreCase("Messages")) {
                oneP = sec.getDisplayPanel(width);
                jp.add(oneP, gbc);
                gbc.gridy++;
//            }
        }
        for (OneSimulatorSection sec: processZones)
            sec.updateUI();
        processPane.setViewportView(jp);
        fp.add(processPane, BorderLayout.CENTER);
        return fp;
    }

    JComponent getLevel2Pane() {
        int width = 580;
        FramedPanel fp = new FramedPanel(new BorderLayout());
//        fp.add(new SizedLabel("From Level2", new Dimension(100, 40), true), BorderLayout.NORTH);
        fp.add(new MultiPairColPanel("From Level2 (DO NOT EDIT DATA)"), BorderLayout.NORTH);
        JScrollPane level2Pane = new JScrollPane();
        level2Pane.setPreferredSize(new Dimension(width + 40, 700));
        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        JPanel oneP;
        for (OneSimulatorSection sec: level2Zones) {
//            if (!sec.name.equalsIgnoreCase("Messages")) {
                oneP = sec.getDisplayPanel(width);
                jp.add(oneP, gbc);
                gbc.gridy++;
//            }
        }
        for (OneSimulatorSection sec: level2Zones)
            sec.updateUI();
        level2Pane.setViewportView(jp);
        fp.add(level2Pane, BorderLayout.CENTER);
        return fp;
     }

//    public void setProcessZones(Vector<OneSection> processZones) {
//        this.processZones = processZones;
//    }

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

    boolean loadSimulatorOLD() {
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

    SimulationReadyNoted l2InfoMessages;
    SimulationReadyNoted l2ErrorMessages;
    SimulationReadyNoted l2YesNoQuery;
    SimulationReadyNoted l2DataQuery;
    Vector<ReadyNotedParam> readyNotedParamList = new Vector<ReadyNotedParam>();
    TMSubscription messageSub;
    GetLevelResponse yesNoResponse;
    GetLevelResponse dataResponse;
    Hashtable<MonitoredDataItem, Tag> monitoredTags;
    boolean monitoredTagsReady = false;

    void noteMonitoredTags(Tag[] tags) {
        for (Tag tag : tags)
            if (tag.isMonitored())
                monitoredTags.put(tag.getMonitoredDataItem(), tag);
    }

    boolean createL2Messages() {
        boolean retVal = false;
        logDebug("creating L2 Massages");
        messageSub = source.createTMSubscription("Messages", new SubAliveListener(), new MessageListener());
        Tag[] errMessageTags = {new Tag(L2ParamGroup.Parameter.ErrMsg, Tag.TagName.Msg, false, false),
                new Tag(L2ParamGroup.Parameter.ErrMsg, Tag.TagName.Ready, false, !true),
                new Tag(L2ParamGroup.Parameter.ErrMsg, Tag.TagName.Msg, true, false),
                new Tag(L2ParamGroup.Parameter.ErrMsg, Tag.TagName.Ready, true, !false),
                new Tag(L2ParamGroup.Parameter.ErrMsg, Tag.TagName.Noted, true, !false),  // sending 'Noted' to Process
                new Tag(L2ParamGroup.Parameter.ErrMsg, Tag.TagName.Noted, false, !true)  // reading 'Noted' from Process
        };
        Tag[] infoMessageTags = {new Tag(L2ParamGroup.Parameter.InfoMsg, Tag.TagName.Msg, false, false),
                new Tag(L2ParamGroup.Parameter.InfoMsg, Tag.TagName.Ready, false, !true),
                new Tag(L2ParamGroup.Parameter.InfoMsg, Tag.TagName.Msg, true, false),
                new Tag(L2ParamGroup.Parameter.InfoMsg, Tag.TagName.Ready, true, !false),
                new Tag(L2ParamGroup.Parameter.InfoMsg, Tag.TagName.Noted, true, !false),  // sending 'Noted' to Process
                new Tag(L2ParamGroup.Parameter.InfoMsg, Tag.TagName.Noted, false, !true)   // reading 'Noted' from Process
        };
        Tag[] yesNoQueryTags = {new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Msg, false, false),
                new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Ready, false, !true),
                new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Msg, true, false),
                new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Ready, true, !false),
                new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Noted, true, !false),  // sending 'Noted' to Process
                new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Noted, false, !true),  // reading 'Noted' from Process
                new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Response, true, false),
                new Tag(L2ParamGroup.Parameter.YesNoQuery, Tag.TagName.Response, false, !true)
        };
        Tag[] dataQueryTags = {new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Msg, false, false),
                new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Ready, false, !true),
                new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Msg, true, false),
                new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Ready, true, !false),
                new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Noted, true, !false),  // sending 'Noted' to Process
                new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Noted, false, !true),  // reading 'Noted' from Process
                new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Data, true, false),
                new Tag(L2ParamGroup.Parameter.DataQuery, Tag.TagName.Data, false, !true)
        };
        try {
            l2InfoMessages = new SimulationReadyNoted(source, equipment, "Messages.InfoMsg", infoMessageTags, messageSub);
            readyNotedParamList.add(l2InfoMessages);
            l2ErrorMessages = new SimulationReadyNoted(source, equipment, "Messages.ErrorMsg", errMessageTags, messageSub);
            readyNotedParamList.add(l2ErrorMessages);
            l2YesNoQuery = new SimulationReadyNoted(source, equipment, "Messages.YesNoQuery", yesNoQueryTags, messageSub);
            readyNotedParamList.add(l2YesNoQuery);
            yesNoResponse = new GetLevelResponse(l2YesNoQuery, this);
            l2DataQuery = new SimulationReadyNoted(source, equipment, "Messages.DataQuery", dataQueryTags, messageSub);
            readyNotedParamList.add(l2DataQuery);
            dataResponse = new GetLevelResponse(l2DataQuery, this);
            noteMonitoredTags(errMessageTags);
            noteMonitoredTags(infoMessageTags);
            noteMonitoredTags(yesNoQueryTags);
            noteMonitoredTags(dataQueryTags);
            monitoredTagsReady = true;
            retVal = true;
        } catch (TagCreationException e) {
            showError("Message connection to Level1 :" + e.getMessage());
        }
        return retVal;
    }

    Vector<OneSimulatorSection> collectSections(String path, boolean rw) {
        Vector<OneSimulatorSection> retVal = new Vector<OneSimulatorSection>();
        OpcTagGroup grp = allGroups.getSubGroup(path);
        try {
            for (OpcTagGroup subGrp: grp.subGroups)
                retVal.add(new OneSimulatorSection(this, subGrp, rw));
        } catch (Exception e) {
            showError("Some problem in creating Collection for " + path + "> " + e.getMessage());
        }
        return retVal;
    }

    public boolean canNotify() {
        return false;
    }

    public void enableNotify(boolean b) {

    }

    public Window parent() {
        return mainF;
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

    public void logInfo(String msg) {
        System.out.println("OpcSimulator:" + msg);
    }

    public void logError(String msg) {

    }

    public void logTrace(String msg) {

    }

    public void logDebug(String nsg) {

    }

    public void showError(String msg) {
        SimpleDialog.showError(parent(), "", msg);
        Window w = parent();
        if (w != null)
            w.toFront();
    }

    public void showMessage(String msg) {
        SimpleDialog.showMessage(parent(), "", msg);
        Window w = parent();
        if (w != null)
            w.toFront();
    }

    public boolean decide(String title, String msg) {
        return decide(title, msg, true);
    }

    public boolean decide(String title, String msg, boolean defaultOption) {
        int resp = SimpleDialog.decide(parent(), title, msg, defaultOption);
        if (resp == JOptionPane.YES_OPTION)
            return true;
        else
            return false;
    }

    void close() {
        stopDisplayUpdater();
        try {
            if (processZones != null)
                for (OneSimulatorSection sec : processZones)
                    sec.closeSubscription();
            if (level2Zones != null)
                for (OneSimulatorSection sec : level2Zones)
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

    protected void modifyJTextEdit() {
         KeyboardFocusManager.getCurrentKeyboardFocusManager()
                 .addPropertyChangeListener("permanentFocusOwner", new PropertyChangeListener() {
                     public void propertyChange(final PropertyChangeEvent e) {
                         if (e.getOldValue() instanceof JTextField) {
                             SwingUtilities.invokeLater(new Runnable() {

                                 public void run() {
                                     JTextField oldTextField = (JTextField) e.getOldValue();
                                     oldTextField.setSelectionStart(0);
                                     oldTextField.setSelectionEnd(0);
                                 }
                             });
                         }
                         if (e.getNewValue() instanceof JTextField) {
                             SwingUtilities.invokeLater(new Runnable() {

                                 public void run() {
                                     JTextField textField = (JTextField) e.getNewValue();
                                     textField.selectAll();
                                 }
                             });
                         }
                     }
                 });
    }

    Thread displayThread;
    boolean displayON = false;
    long displayUpdateInterval = 1000; // ms

    void updateUIDisplay() {
        for (OneSimulatorSection sec : processZones)
            sec.updateUI();
        for (OneSimulatorSection sec : level2Zones)
            sec.updateUI();
    }


    public void startDisplayUpdater() {
        displayON = true;
        L2DisplayUpdater displayUpdater = new L2DisplayUpdater();
        displayThread = new Thread(displayUpdater);
        displayThread.start();
        logTrace("Display updater started ...");
    }

    void stopDisplayUpdater() {
        if (displayON) {
            displayON = false;
            try {
                if (displayThread != null) displayThread.join(displayUpdateInterval * 5);
            } catch (InterruptedException e) {
                logError("Problem in stopping Display Updater");
                e.printStackTrace();
            }
        }
    }


    class L2DisplayUpdater implements Runnable {
        public void run() {
            while (displayON) {
                try {
                    Thread.sleep(displayUpdateInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                updateUIDisplay();
            }
        }
    }

    class SubAliveListener implements SubscriptionAliveListener {     // TODO This is common dummy class used everywhere to be made proper
        public void onAlive(Subscription s) {
        }
        public void onTimeout(Subscription s) {
        }
    }


    class MessageListener extends L2SubscriptionListener {
        @Override
        public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {
            if (monitoredTagsReady) {
                String fromElement = monitoredDataItem.toString();
//                logInfo("Messages" + ":fromElement-" + fromElement + ", VALUE: " + dataValue.getValue().toStringWithType());
                Tag theTag = monitoredTags.get(monitoredDataItem);
//                logInfo("Messages" + ":fromElement-" + theTag.element);
                if (theTag.element == L2ParamGroup.Parameter.InfoMsg) {
                    if (l2InfoMessages.isNewData(theTag)) {  // the data will be already read if new data
                        String msg = l2InfoMessages.getValue(Tag.TagName.Msg).stringValue;
                        showMessage(msg);
                        l2InfoMessages.setAsNoted(true);
                    }
//                    else
//                        logInfo("InfoMsg is not newData");
                }
                if (theTag.element == L2ParamGroup.Parameter.ErrMsg) {
                    if (l2ErrorMessages.isNewData(theTag)) {  // the data will be already read if new data
                        String msg = l2ErrorMessages.getValue(Tag.TagName.Msg).stringValue;
                        showError(msg);
                        l2ErrorMessages.setAsNoted(true);
                    }
//                    else
//                        logInfo("ErrMsg is not newData");
                }
                if (theTag.element == L2ParamGroup.Parameter.YesNoQuery) {
                    if (l2YesNoQuery.isNewData(theTag)) {  // the data will be already read if new data
                        String msg = l2YesNoQuery.getValue(Tag.TagName.Msg).stringValue;
                        if (decide("", msg))
                            l2YesNoQuery.setValue(Tag.TagName.Response, true);
                        else
                            l2YesNoQuery.setValue(Tag.TagName.Response, false);
                        l2YesNoQuery.setAsNoted(true);
                    }
//                    else
//                        logInfo("YesNoQuery is not newData");
                }
            }
        }
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
        OpcSimulator opcS = null;
        if (args.length > 0) {
            if (args.length == 1)
                opcS = new OpcSimulator(args[0]);
            else if (args[1].equalsIgnoreCase("-loadTester"))
                opcS = new LoadTester(args[0]);
        }
        if (opcS != null)
            opcS.proceed();
    }
}
