package materials;

import directFiredHeating.DFHeating;
import directFiredHeating.FceEvaluator;
import mvUtils.display.*;
import mvUtils.file.ActInBackground;
import mvUtils.file.WaitMsg;
import mvUtils.http.PostToWebSite;
import mvUtils.jsp.JSPConnection;
import mvUtils.math.DoubleRange;
import mvUtils.math.XYArray;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.security.MiscUtil;
import netscape.javascript.JSObject;
import protection.CheckAppKey;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;

/**
 * User: M Viswanathan
 * Date: 02-Jun-17
 * Time: 1:14 PM
 * To change this template use File | Settings | File Templates.
 */

public class ThermalProperties extends JApplet implements InputControl {
    public int appCode = 0;
    static public JSPConnection jspConnection;
    static public String jspBase = "localhost:9080/fceCalculations/jsp/";
    boolean bCanEdit = false;
    boolean bEditON = false;
    int MAXROWS = 30;
    JComboBox jcMaterialGroup, jcMatNames, jcProperty;
    //    IDandName[] idAndName;
    LinkedHashMap<String, MatAndProps> matAndPropsHash;

    JSObject win;
    JFrame mainF;
    ControlledTextField tfNewMaterial;
    TableAndGraph tableAndGraph;
//    TraceBuilder tableAndGraph;
    JButton pbEditData, pbSaveData, pbCLose, pbFileData;
    boolean itsON = false;
    String user;

    public ThermalProperties() {
        this.appCode = 106;
        user = MiscUtil.getUser();
    }

    protected boolean getJSPbase() {
        boolean retVal = false;
        String jspBaseIDPath = "jspBase.txt";
        File jspBaseFile = new File(jspBaseIDPath);
        long len = jspBaseFile.length();
        if (len > 5 && len < 100) {
            int iLen = (int) len;
            byte[] data = new byte[iLen + 1];
            try {
                BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(jspBaseFile));
                if (iStream.read(data) > 5) {
                    jspBase = new String(data).trim() + ":9080/fceCalculations/jsp/";
                    iStream.close();
                    retVal = true;
                }
            } catch (IOException e) {
                ;
            }
        }
        return retVal;
    }

    protected boolean getJSPConnection() {
        boolean retVal = false;
        try {
            jspConnection = new JSPConnection(jspBase);
            retVal = true;
        } catch (Exception e) {
            System.out.println("DFHeating.4599 " + e.getLocalizedMessage());
        }
        return retVal;
    }

    public boolean setItUp() {
        boolean retVal = false;
        if (getJSPbase()) {
            DataWithStatus<Boolean> runCheck = new CheckAppKey(jspBase).canRunThisApp(appCode, true);
            if (runCheck.getStatus() == DataStat.Status.OK) {
                UIManager.put("ComboBox.disabledForeground", Color.black);
                tableAndGraph = new TableAndGraph(this, MAXROWS, new DoubleRange(0, 2000), new DoubleRange(-1e6, 1e6), "#,###", "#,##0.###");
                mainF = new JFrame("Set Material Property");
                mainF.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                matAndPropsHash = new LinkedHashMap<String, MatAndProps>();
                createUIs();
                getMatGroups();
                displayIt();
                retVal = true;
            }
        }
        return retVal;
    }

    public void init() {
        UIManager.put("ComboBox.disabledForeground", Color.black);
//        String strTest = this.getParameter("OnTest");
        tableAndGraph = new TableAndGraph(this, MAXROWS, new DoubleRange(0, 2000), new DoubleRange(-1e6, 1e6), "#,###", "#,##0.###");
//        tableAndGraph = new TraceBuilder(this, MAXROWS, new DoubleRange(0, 2000), new DoubleRange(-1e6, 1e6), "#,###", "#,##0.###");
        mainF = new JFrame("Set Material Property");
        mainF.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        mainF.addWindowListener(new winListener());
        matAndPropsHash = new LinkedHashMap<String, MatAndProps>();
        createUIs();
        getMatGroups();
//        getSelectedGroupData("steels");
        displayIt();
    }
    NewMatListener matListener;

    String enterNewMaterial = "##Enter New Material";

    void createUIs() {
        jcMaterialGroup = new JComboBox();
//        jcMaterialType.setPreferredSize(new Dimension(200, 25));
//        jcMaterialType.addActionListener(e-> {
//            getSelectedGroupData((String)jcMaterialType.getSelectedItem());
//        });

        jcMatNames = new JComboBox();
        jcMatNames.setPreferredSize(new Dimension(200, 25));
        matListener = new NewMatListener();
        tfNewMaterial = new ControlledTextField(this, 1, 25, new Dimension(200, 25));
        tfNewMaterial.setText(enterNewMaterial);
        tfNewMaterial.setEnabled(false);
//        tfNewMaterial.setPreferredSize(new Dimension(200, 25));
        //       colData = traceBuilder.getColData();    // NumberTextField[MAXROWS][2];
        tableAndGraph.enableColDataEdit(false);
        ButtonListener li = new ButtonListener();
        Dimension pbSize = new Dimension(100, 25);
        pbCLose = new JButton("Exit");
        pbCLose.setPreferredSize(pbSize);
        pbCLose.addActionListener(li);
        if (bCanEdit) {
            pbEditData = new JButton("Edit Data");
            pbEditData.addActionListener(li);
            pbSaveData = new JButton("Save Data");
            pbSaveData.setEnabled(false);
            pbSaveData.setPreferredSize(pbSize);
            pbSaveData.addActionListener(li);
            pbFileData = new JButton("Data From File");
            pbFileData.setEnabled(false);
            pbFileData.addActionListener(li);
        }
    }

    void getMatGroups() {
        PostToWebSite jspSrc = new PostToWebSite("http://" + jspBase);
        HashMap<String, String> params = new HashMap<>();
        params.put("user", user);
        String xmlProplist = jspSrc.getByPOSTRequest("getPropertyList.jsp", params, 100000);
        setPropListFromXML(xmlProplist);

        params.clear();
        params.put("user", user);
        String xmlMatGroupList = jspSrc.getByPOSTRequest("getMaterialGroups.jsp", params, 100000);
        addMatGroupListFromXML(xmlMatGroupList);
        jcMaterialGroup.addActionListener(e-> {
            WaitMsg waitMSg = new WaitMsg(mainF, "Please Wait - Reloading Data");
            collectAndUpdateData();
            waitMSg.close();
        });
        jcMaterialGroup.setSelectedIndex(2);
    }

    void collectAndUpdateData() {
        getSelectedGroupData((String) jcMaterialGroup.getSelectedItem());
        jcProperty.setSelectedIndex(0);
        clearDetailsPanel();
    }

    void getSelectedGroupData(String materialGrp) {
        PostToWebSite jspSrc = new PostToWebSite("http://" + jspBase);
        HashMap<String, String> params = new HashMap<>();
        params.clear();
        params.put("user", user);
        params.put("groupName", materialGrp);
        String xmlMatlist = jspSrc.getByPOSTRequest("getMaterialList.jsp", params, 100000);
        addMaterialListFromXML(materialGrp, xmlMatlist);

        for (String matCode:matAndPropsHash.keySet()) {
            params.clear();
            params.put("user", user);
            params.put("materialID", matCode);
            String xmlProps = jspSrc.getByPOSTRequest("getAllTemperatureProperties.jsp", params, 100000);
            addMatPropertyFromXML(matCode, xmlProps);
        }
        populateMatList();
//        populateDataTable();
    }

    boolean saveProperty() {
        boolean retVal = false;
        String matID = (bNewMaterial) ? getNewMaterialCode():((MatAndProps)jcMatNames.getSelectedItem()).getID();
        if (matID.length() > 0) {
            PostToWebSite jspSrc = new PostToWebSite("http://" + jspBase);
            HashMap<String, String> params = new HashMap<>();
            params.clear();
            params.put("user", user);
            params.put("matCode", matID);
            params.put("propName", propCol[jcProperty.getSelectedIndex()]);
            params.put("data", tableAndGraph.dataAsString());
            String xmlStr = jspSrc.getByPOSTRequest("SetTemperatureProperty.jsp", params, 100000);
            ValAndPos vp;
            vp = XMLmv.getTag(xmlStr, "Status", 0);
            String status = vp.val;
            if (vp.val.length() > 0) {
                if (status.equalsIgnoreCase("Ok"))
                    retVal = true;
                else {
                    if (status.equalsIgnoreCase("Error")) {
                        vp = XMLmv.getTag(xmlStr, "Message", vp.endPos);
                        showError("Error: " + vp.val);
                    } else
                        showError("Error: No Specific Message from Server");
                }
            } else
                showError("Error: Unable to process \n" + xmlStr);
        }
        return retVal;
    }

    String getNewMaterialCode() {
        String matCode = "";
        PostToWebSite jspSrc = new PostToWebSite("http://" + jspBase);
        HashMap<String, String> params = new HashMap<>();
        params.clear();
        params.put("user", user);
        params.put("groupName", (String)jcMaterialGroup.getSelectedItem());
        params.put("material", newMatName);
        String xmlStr = jspSrc.getByPOSTRequest("SetMaterialCode.jsp", params, 100000);
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "Status", 0);
        String status = vp.val;
        if (vp.val.length() > 0) {
            if (status.equalsIgnoreCase("Ok")) {
                vp = XMLmv.getTag(xmlStr, "id", vp.endPos);
                matCode = vp.val;
            }
        } else
            showError("Error: Unable to process \n" + xmlStr);
        return matCode;
    }


    public void displayIt() {
        if (!itsON) {
//            populateMatList();
//            populateDataTable();
            itsON = true;
            FramedPanel fp = new FramedPanel(new BorderLayout());
            fp.add(prepareDetailsPanel(), BorderLayout.NORTH);
//            populateDetailsPanel();
            mainF.add(fp);
            mainF.setFocusable(true);
            mainF.setVisible(true);
            mainF.requestFocus();
            mainF.setSize(1000, 600);
            mainF.toFront();
        }
    }

    JPanel detailsPanel;
    GridBagConstraints gbcDetailsLoc;
    JPanel tablePanel;
    JPanel graphPanel;
    JPanel buttonPanel;
    boolean detailsPanelBaseReady = false;

    JPanel prepareDetailsPanel() {
        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        jp.add(new JLabel("Material Type"), gbc);
        gbc.gridx++;
        jp.add(new JLabel("Material Name"), gbc);
        gbc.gridx++;
        jp.add(new JLabel("Property"), gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        jp.add(jcMaterialGroup, gbc);
//        jp.add(new JLabel(matType), gbc);
        gbc.gridx++;
        jp.add(jcMatNames, gbc);
        gbc.gridx++;
        jp.add(jcProperty, gbc);
        gbc.gridx = 1;
        gbc.gridy++;
        jp.add(tfNewMaterial, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbcDetailsLoc = gbc;
        detailsPanel = jp;
        detailsPanelBaseReady = true;
        return detailsPanel;
    }

    void populateDetailsPanel() {
        if (detailsPanelBaseReady) {
            clearDetailsPanel();
            JPanel jp = detailsPanel;
            GridBagConstraints gbc = (GridBagConstraints) gbcDetailsLoc.clone();
            tablePanel = tableAndGraph.dataPanel();
            jp.add(tablePanel, gbc); // dataPanelTable(), gbc);
            gbc.gridy++;
            buttonPanel = buttonPan();
            jp.add(buttonPanel, gbc);
            gbc.gridy = 0;
            gbc.gridheight = 5;
            gbc.gridx = 4;
            gbc.gridwidth = 1;
            graphPanel = tableAndGraph.graphPanel();
            jp.add(graphPanel, gbc);
        }
    }

    boolean clearDetailsPanel() {
        boolean retVal = false;
        if (tablePanel != null) {
            detailsPanel.remove(graphPanel);
            detailsPanel.remove(buttonPanel);
            detailsPanel.remove(tablePanel);
            retVal = true;
        }
        return retVal;
    }

    JPanel buttonPan() {
        JPanel jp;
        if (bCanEdit) {
            jp = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            Insets ins = new Insets(1, 1, 1, 1);
            gbc.insets = ins;
            jp.add(pbEditData, gbc);
            gbc.gridx++;
//            ins.set(1, 5, 1, 5);
            jp.add(pbFileData, gbc);
            gbc.gridx++;
            jp.add(pbSaveData, gbc);
            gbc.gridx++;
            ins.set(1, 50, 1, 5);
            jp.add(pbCLose, gbc);
        }
        else {
            jp = new JPanel(new BorderLayout());
            jp.add(pbCLose, BorderLayout.EAST);
        }
        return jp;
    }

    public String setCanEdit(String canEditStr) {
        if (canEditStr.trim().equals("CANEDIT"))
            bCanEdit = true;
        else
            bCanEdit = false;
        return "OK";
    }

    int addMatGroupListFromXML(String xmlStr) {
        jcMaterialGroup.removeAllItems();
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "n", 0);
        int nG = Integer.valueOf(vp.val);
        for (int n = 0; n < nG; n++) {
            vp = XMLmv.getTag(xmlStr, "GN", vp.endPos);
            jcMaterialGroup.addItem(vp.val.trim());
        }
        return nG;
    }


    int addMaterialListFromXML(String matGroup, String xmlStr) {
        matAndPropsHash.clear();
        matType = matGroup;
        int nMats = 0;
        String name;
        String id;
        ValAndPos vp;
        ValAndPos localVp;
        String oneXML;
        vp = XMLmv.getTag(xmlStr, "Status", 0);
        if (vp.val.equalsIgnoreCase("OK")) {
            vp = XMLmv.getTag(xmlStr, "n", vp.endPos);
            if (vp.val.length() > 0) {
                nMats = Integer.valueOf(vp.val);
                for (int m = 0; m < nMats; m++) {
                    vp = XMLmv.getTag(xmlStr, "material", vp.endPos);
                    oneXML = vp.val;
                    localVp = XMLmv.getTag(oneXML, "name", 0);
                    name = localVp.val;
                    localVp = XMLmv.getTag(oneXML, "ID", localVp.endPos);
                    id = localVp.val;
                    matAndPropsHash.put(id, new MatAndProps(id, name));
                }
            }
        }
        return nMats;
    }

    public int addMatPropertyFromXML(String id, String xmlStr) {
        ValAndPos vp;
        ValAndPos onePropVp;
        ValAndPos oneSetVp;
        String onePropXML;
        String onsSetXML;
        XYArray data;
        double t, val;
        int nProps = 0;
        String propCol;
        int nSet;
        MatAndProps matProps;
        if (matAndPropsHash.containsKey(id)) {
            matProps = matAndPropsHash.get(id);
            vp = XMLmv.getTag(xmlStr, "Status", 0);
            if (vp.val.equalsIgnoreCase("OK")) {
                vp = XMLmv.getTag(xmlStr, "n", vp.endPos);
                if (vp.val.length() > 0) {
                    nProps = Integer.valueOf(vp.val);
                    for (int m = 0; m < nProps; m++) {
                        vp = XMLmv.getTag(xmlStr, "property", vp.endPos);
                        onePropXML = vp.val;
                        onePropVp = XMLmv.getTag(onePropXML, "PropColumn", 0);
                        propCol = onePropVp.val;
                        onePropVp = XMLmv.getTag(onePropXML, "nSet", onePropVp.endPos);
                        nSet = Integer.valueOf(onePropVp.val);
                        data = new XYArray();
                        for (int s = 0; s < nSet; s++) {
                            onePropVp = XMLmv.getTag(onePropXML, "tSet", onePropVp.endPos);
                            onsSetXML = onePropVp.val;
                            oneSetVp = XMLmv.getTag(onsSetXML, "t", 0);
                            t = Double.valueOf(oneSetVp.val);
                            oneSetVp = XMLmv.getTag(onsSetXML, "val", oneSetVp.endPos);
                            val = Double.valueOf(oneSetVp.val);
                            data.add(t, val);
                        }
                        matProps.addPropData(propCol, data);

                    }
                }
            }
        }
        return nProps;
    }

    String[] propUnits, propCol, propName, propText;

    int setPropListFromXML(String xmlStr) {
        int nProps = 0;
        ValAndPos vp;
        ValAndPos localVp;
        String oneProp;
        vp = XMLmv.getTag(xmlStr, "Status", 0);
        if (vp.val.equalsIgnoreCase("OK")) {
            vp = XMLmv.getTag(xmlStr, "n", vp.endPos);
            if (vp.val.length() > 0) {
                nProps = Integer.valueOf(vp.val);
                propCol = new String[nProps + 1];
                propCol[0] = "";
                propName= new String[nProps + 1];
                propName[0] = "";
                propText = new String[nProps + 1];
                propText[0] = "Select Property";
                propUnits = new String[nProps + 1];

                for (int p = 0; p < nProps; p++)  {
                    vp = XMLmv.getTag(xmlStr, "property", vp.endPos);
                    oneProp  = vp.val;
                    localVp = XMLmv.getTag(oneProp, "propertyColName", 0);
                    propCol[p + 1] = localVp.val;
                    localVp = XMLmv.getTag(oneProp, "propertyText", localVp.endPos);
                    propName[p + 1] = localVp.val;
                    localVp = XMLmv.getTag(oneProp, "units", localVp.endPos);
                    propUnits[p + 1] = localVp.val;
                    propText[p + 1] = propName[p + 1] + "(" + propUnits[p + 1] + ")";
                }
            }
        }
        jcProperty = new JComboBox(propText);
        jcProperty.addActionListener(new NewPropListner());
        return nProps;
    }

    int setPropListFromXMLOLD(String xmlStr) {
        int nProps = 0;
        ValAndPos vp;
        ValAndPos localVp;
        String oneProp;
        vp = XMLmv.getTag(xmlStr, "Status", 0);
        if (vp.val.equalsIgnoreCase("OK")) {
            vp = XMLmv.getTag(xmlStr, "n", vp.endPos);
            if (vp.val.length() > 0) {
                nProps = Integer.valueOf(vp.val);
                propCol = new String[nProps];
                propName= new String[nProps];
                propText = new String[nProps];
                propUnits = new String[nProps];
                for (int p = 0; p < nProps; p++)  {
                    vp = XMLmv.getTag(xmlStr, "property", vp.endPos);
                    oneProp  = vp.val;
                    localVp = XMLmv.getTag(oneProp, "propertyColName", 0);
                    propCol[p] = localVp.val;
                    localVp = XMLmv.getTag(oneProp, "propertyText", localVp.endPos);
                    propName[p] = localVp.val;
                    localVp = XMLmv.getTag(oneProp, "units", localVp.endPos);
                    propUnits[p] = localVp.val;
                    propText[p] = propName[p] + "(" + propUnits[p] + ")";
                }
            }
        }
        jcProperty = new JComboBox(propText);
        jcProperty.addActionListener(new NewPropListner());
        return nProps;
    }

    int nValidRows = 0;

    void populateMatList() {
        jcMatNames.removeActionListener(matListener);
        jcMatNames.removeAllItems();
        String matType = "";
        Iterator<String> iter = matAndPropsHash.keySet().iterator();
        String key;
        while (iter.hasNext())  {
            key = iter.next();
            jcMatNames.addItem(matAndPropsHash.get(key));
        }
        if (!matType.equals("Fuels") && bCanEdit) {
            MatAndProps newMat = new MatAndProps("0", "Add New Material ...", true);
            jcMatNames.addItem(newMat); //"Add New Material ...");
        }
        jcMatNames.addActionListener(matListener);
        jcMatNames.updateUI();
    }

    void populateDataTable() {
        MatAndProps matProps = (MatAndProps)jcMatNames.getSelectedItem();
        if (matProps != null) {
            int selProp = jcProperty.getSelectedIndex();
            if (selProp >= 0) {
                String  pCol=  propCol[selProp];
                String units = propUnits[selProp];
                String name = propName[selProp];
                XYArray arr = matProps.getPropsArr(pCol);
                TraceHeader head = new TraceHeader("Temperature", "degC", "#,###", name, "", "");
                tableAndGraph.populate(head, arr, bEditON);
                setDataVisible(arr != null || bEditON);
            }
        }
    }

    void setDataVisible(boolean ena) {
//        if (tablePanel != null) {
//            tablePanel.setVisible(ena);
//            graphPanel.setVisible(ena);
//        }
    }

    String checkDuplicate(String checkName) {
        String existingName = "";
        String trimmedExist, trimmedNew;
        trimmedNew = checkName.replace(" ", "").replace("-","");
        Iterator<String> iter = matAndPropsHash.keySet().iterator();
        boolean bFound = false;
        while (iter.hasNext()) {
            existingName = matAndPropsHash.get(iter.next()).name;
            trimmedExist = existingName.replace(" ", "").replace("-","");
            if (trimmedNew.equalsIgnoreCase(trimmedExist)) {
                bFound = true;
                break;
            }

        }
        if (bFound)
            return existingName;
        else
            return "";
    }

    boolean checkAndSave() {
        boolean datOK = false;
        nValidRows = 0;
        if (bNewMaterial) {
            if (!tfNewMaterial.dataOK()) {
                showError("New Material Name '" + tfNewMaterial.getText() + "' is not acceptable! ");
            }
            newMatName = tfNewMaterial.getText().trim();
            if (newMatName.substring(0, 2).equalsIgnoreCase("##"))
                showError("Enter proper Material Name");
            else {
                String existing = checkDuplicate(newMatName);
                if (existing.length() > 0)
                    showError("Material Name '" + existing + "' Exists!\nTry different name");
                else
                    datOK = true;
            }
        }
        else
            datOK = true;
        if (datOK) {
            boolean proceed = true;
            if (!bNewMaterial) {
                proceed = decide("Replacing Existing Data if present",
                        "Selected Property of the Material will be Replaced ...");
            }
            if (proceed) {
                saveProperty();
            }
        }
        else
            tfNewMaterial.setText(enterNewMaterial);
        return datOK;
    }

    String propStr, matCode, matType, property, isNewMat, newMatName ="";
    boolean bNewMaterial = false;

    boolean setDataToSend() {
        if (bNewMaterial) {
            isNewMat = "YES";
            matCode = "0";
        }
        else {
            isNewMat = "NO";
            matCode = ((MatAndProps)jcMatNames.getSelectedItem()).getID();
        }
        property = propCol[jcProperty.getSelectedIndex()];
        propStr = tableAndGraph.dataAsString();
        if (propStr.length() > 3)
            return true;
        else
            return false;
    }

    public String getPropStr() {
        return propStr;
    }

    public String getMatCode() {
        return matCode;
    }

    public String getMatType() {
        return matType;
    }

    public String getPropertyCol() {
        return property;
    }

    public String getIsNewMat() {
        return isNewMat;
    }

    public String getNewMatName() {
        return newMatName;
    }

    public Frame parent() {
        return mainF;
    }

//    @Override
//    public void destroy() {
//        debug("In Destroy");
//        super.destroy();
//    }

    void close() {
        debug("CLOSING ...");
        mainF.dispose();
        itsON = false;
        System.exit(0);
    } // close

    boolean takeFileData(String[] dataStr) {
        boolean allOK = true;
        int nDat = dataStr.length;
        if (nDat > MAXROWS) {
            showMessage("Taking " + MAXROWS + " of the available " + nDat + " Data");
            nDat = MAXROWS;
        }
        String[] data;
        double t, p;
        int srcRow, destRow = 0;
        boolean skip;
        tableAndGraph.initData(bEditON);
        if (nDat > 0) {
            for (srcRow = 0; srcRow < nDat; srcRow++) {
                skip = false;
                data = dataStr[srcRow].split(",");
                if (data.length == 2) {
                    try {
                        t = Double.valueOf(data[0]);
                        if (!skip) {
                            p = Double.valueOf(data[1]);
                            tableAndGraph.setValue(destRow, t, p);
                            destRow++;
                        }
                    } catch (NumberFormatException e) {
                        showError("Some Error is Data from File '" + dataStr[srcRow] + "'");
                        allOK = false;
                        break;
                    }
                } else {
                    showError("Some Error is Data from File '" + dataStr[srcRow] + "'");
                    allOK = false;
                    break;
                }
            }
        } else {
            showError("No Valid data!");
            allOK = false;
        }
        return allOK;
    }

    boolean getFceFromFile() {
        boolean bRetVal = false;
        Vector<String> readList = new Vector<String>();
        FileDialog fileDlg =
                new FileDialog(mainF, "Read Property Data from CSV file",
                        FileDialog.LOAD);
        fileDlg.setVisible(true);
        String fileName = fileDlg.getDirectory() + fileDlg.getFile();
        parent().toFront();
        if (!fileName.equals("nullnull")) {
            debug("Data file name :" + fileName);
            File f = new File(fileName);
            long len = f.length();
            if (len < 1000) {
                try {
                    FileInputStream fStream = new FileInputStream(fileName);
                    // Get the object of DataInputStream
                    DataInputStream in = new DataInputStream(fStream);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String strLine;
                    //Read File Line By Line
                    while ((strLine = br.readLine()) != null) {
                        readList.add(strLine);
                    }
                    br.close();
                    in.close();
                    fStream.close();
                } catch (Exception e) {
                    showError("Some Problem in getting file!");
                }
            } else {
                showError("Not a Proper data File '" + fileName + "'!");
                return false;
            }
        }
        if (readList.size() > 0) {
            String[] list = new String[1];
            return takeFileData((String[]) readList.toArray(list));
        } else
            return (false);
    }


    boolean decide(String title, String msg) {
        int resp = JOptionPane.showConfirmDialog(parent(), msg, title, JOptionPane.YES_NO_OPTION);
        if (resp == JOptionPane.YES_OPTION)
            return true;
        else
            return false;
    }

    public boolean canNotify() {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void enableNotify(boolean ena) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    void debug(String msg) {
        System.out.println("ViewSetProperty " + msg);
    }

    public void showError(String msg) {
        JOptionPane.showMessageDialog(parent(), msg, "ERROR", JOptionPane.ERROR_MESSAGE);
        parent().toFront();
    }

    void showMessage(String msg) {
        JOptionPane.showMessageDialog(parent(), msg, "FOR INFORMATION", JOptionPane.INFORMATION_MESSAGE);
        parent().toFront();
    }

    class MatAndProps {
        String id;
        String name;
        LinkedHashMap<String, XYArray> propNameAndData;
        boolean bNew;

        MatAndProps(String id, String name) {
            this(id, name, false);
        }

        MatAndProps(String id, String name, boolean bNew) {
            this.id = id;
            this.name = name;
            propNameAndData = new LinkedHashMap<String, XYArray>();
            this.bNew = bNew;
        }

        String getID() {
            return id;
        }

        XYArray getPropsArr(String colName) {
            if (propNameAndData.containsKey(colName))
                return propNameAndData.get(colName);
            else
                return null;
        }

        void addPropData(String propCol, String dataPairStr) {
            propNameAndData.put(propCol, new XYArray(dataPairStr));
        }

        void addPropData(String propCol, XYArray dataPairStr) {
            propNameAndData.put(propCol, dataPairStr);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    class NewMatListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (((MatAndProps)jcMatNames.getSelectedItem()).bNew)  {
                tfNewMaterial.setEnabled(true);
                tableAndGraph.initData(bEditON);
                bNewMaterial = true;
            } else {
                tfNewMaterial.setEnabled(false);
                bNewMaterial = false;
                populateDataTable();
            }
        }
    }

    class NewPropListner implements ActionListener {
        public void actionPerformed(ActionEvent e) {
//            if (!jcMatNames.getSelectedItem().equals("Add New Material ..."))
            if (jcProperty.getSelectedIndex() > 0) {
//                populateDataTable();
                populateDetailsPanel();
                populateDataTable();
            }
        }
    }

    void editButtonAction()  {
        if (bEditON) {
            bEditON = false;
            populateDataTable();  // restore earlier data
            pbFileData.setEnabled(false);
            pbSaveData.setEnabled(false);
            jcMatNames.setEnabled(true);
            jcProperty.setEnabled(true);
            tableAndGraph.enableColDataEdit(false);
            pbEditData.setText("Edit Data");
        }
        else {
            bEditON = true;
            populateDataTable();  // restore earlier data
            pbFileData.setEnabled(true);
            pbSaveData.setEnabled(true);
            jcMatNames.setEnabled(false);
            jcProperty.setEnabled(false);
            tableAndGraph.enableColDataEdit(true);
            pbEditData.setText("Quit Edit");
        }
    }

    boolean waitMagON = false;
    JDialog dlgWait;

    class ButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            while (true) {
                if (src == pbEditData) {
                    editButtonAction();
                    break;
                }
                if (src == pbSaveData) {
                    if (checkAndSave()) {
                        WaitMsg waitMSg = new WaitMsg(mainF, "Please Wait - Reloading Data");
                        collectAndUpdateData();
                        editButtonAction();
                        waitMSg.close();
                    }
                    break;
                }
                if (src == pbCLose) {
                    close();
                    break;
                }
                if (src == pbFileData) {
                    getFceFromFile();
                    break;
                }
            }
        }
    }

    class ButtonListenerOLD implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            while (true) {
                if (command.equals("Edit Data")) {
                    if (bEditON) {
                        bEditON = false;
                        pbFileData.setEnabled(false);
                        pbSaveData.setEnabled(false);
                        jcMatNames.setEnabled(true);
                        jcProperty.setEnabled(true);
                        tableAndGraph.enableColDataEdit(false);
                    }
                    else {
                        bEditON = true;
                        pbFileData.setEnabled(true);
                        pbSaveData.setEnabled(true);
                        jcMatNames.setEnabled(false);
                        jcProperty.setEnabled(false);
                        tableAndGraph.enableColDataEdit(true);
                    }
                    break;
                }
                if (command.equals("Save \n" +
                        "                    break;\n" +
                        "                }\n" +
                        "                if (command.equals(\"Exit\")) {\n" +
                        "                    close();\n" +
                        "                    break;\n" +
                        "                }\n" +
                        "                if (command.equals(\"Data From File\")) {\n" +
                        "                    getFceFromFile();\n" +
                        "                    break;Data")) {
                    checkAndSave();
                }
            }
        }
    }

    class WaitMsg {
        JDialog dlgWait;
        WaitMsg(Frame parent, String msg) {
            Thread waitThrd = new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            dlgWait = new JDialog(parent, msg, true);
                            dlgWait.setSize(new Dimension(msg.length() * 10, 20));
                            dlgWait.setLocationRelativeTo(mainF);
                            dlgWait.setVisible(true);
                        }
                    },
                    "waiter");
            waitThrd.start();
        }

        void close() {
            dlgWait.dispose();
        }
    }

    public static void main(String[] args)  {
        ThermalProperties tP= new ThermalProperties();
        tP.setItUp();
    }

}

