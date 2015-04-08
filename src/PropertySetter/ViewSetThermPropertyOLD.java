package PropertySetter;

import mvUtils.display.InputControl;
import mvUtils.display.NumberTextField;
import mvUtils.display.FramedPanel;
import mvUtils.math.XYArray;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 10/26/12
 * Time: 10:13 AM
 * To change this template use File | Settings | File Templates.
 */
public class ViewSetThermPropertyOLD extends JApplet implements InputControl {
    boolean bCanEdit = true;
    boolean bEditON = false;
    int MAXROWS = 20;
    JComboBox jcMatNames, jcProperty;
//    IDandName[] idAndName;
    LinkedHashMap<String, MatAndProps> matAndPropsHash;
    JSObject win;
    JFrame mainF;
    boolean onTest;
    JTextField tfNewMaterial;
    JTable propTable;
    NumberTextField[][] colData;
    JButton pbEditData, pbSaveData, pbCLose, pbFileData;
    boolean itsON = false;

    public ViewSetThermPropertyOLD() {
    }

    public void init() {
        UIManager.put("ComboBox.disabledForeground", Color.black);
        String strTest = this.getParameter("OnTest");
        mainF = new JFrame("Set Material Property");
        mainF.addWindowListener(new winListener());
        matAndPropsHash = new LinkedHashMap<String, MatAndProps>();
        createUIs();
        if (strTest != null)
            onTest = strTest.equalsIgnoreCase("YES");
        if (onTest) {
            setTestData();
            displayIt();
        } else {
            try {
                win = JSObject.getWindow(this);
            } catch (JSException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                win = null;
            }
            Object o;
//            setTestData();
//            displayIt();
            o = win.eval("getData()");
//            populateMatList();
//            displayIt();
        }
    }

    NewMatListner matListener;

    void createUIs() {
        jcMatNames = new JComboBox();
        jcMatNames.setPreferredSize(new Dimension(200, 25));
        matListener = new NewMatListner();
        tfNewMaterial = new JTextField("New Material");
        tfNewMaterial.setEnabled(false);
        tfNewMaterial.setPreferredSize(new Dimension(200, 25));
        propTable = new JTable();
        colData = new NumberTextField[MAXROWS][2];
        for (int c = 0; c < MAXROWS; c++) {
            colData[c][0] = new NumberTextField(this, -1, 6, false, -1, 2000, "#,###.##", "Temperature (degC)", true);
            colData[c][1] = new NumberTextField(this, 0, 6, false, -1e6, 1e6, "#,###.###", "Property Value", true);
        }
        enableColDataEdit(false);
        String[] colName = new String[]{"temperature", "Property"};
        propTable = new JTable(colData, colName);
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

    void initColdata() {
        for (int r = 0; r < colData.length; r++) {
            colData[r][0].setData(-1);
            colData[r][1].setData(0);
        }
    }

    void enableColDataEdit(boolean bEna) {
        for (int r = 0; r < MAXROWS; r++) {
            colData[r][0].setEditable(bEna);
            colData[r][1].setEditable(bEna);
        }
    }
    void setTestData() {
        setPropList("HeatContent::Heat Content::kcal/kg::Emissivity::Emissivity::::ThermalConductivity::Thermal Conductivity::kcal/mhC");
        addMaterialList("Gases", "1001::CO2::1004::H2O::1008::N2");
        addMatProperty("1001", "HeatContent", "0, 0, 100, 35, 200, 75, 2000, 900");
//        populateMatList();
    }

    public void displayIt() {
        if (!itsON) {
            populateMatList();
            populateDataTable();
            itsON = true;
            FramedPanel fp = new FramedPanel(new BorderLayout());
            fp.add(jcPanel(), BorderLayout.NORTH);
            mainF.add(fp);
            mainF.setFocusable(true);
            mainF.setVisible(true);
            mainF.requestFocus();
            mainF.setSize(600, 600);
            mainF.toFront();
        }
    }

    JPanel jcPanel() {
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
        jp.add(new JLabel(matType), gbc);
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
        jp.add(dataPan(), gbc);
        gbc.gridy++;
        jp.add(buttonPan(), gbc);
        return jp;
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

    JPanel dataPan() {
        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        Insets ins = new Insets(1, 20, 1, 2);
        gbc.insets = ins;
        gbc.gridx = 1;
        gbc.gridy = 0;
        jp.add(new JLabel("Temperature in degC"), gbc);
        gbc.gridx++;
        jp.add(new JLabel("Property Value"), gbc);
        for (int r = 0; r < colData.length; r++) {
            gbc.gridx = 0;
            gbc.gridy++;
            jp.add(new JLabel("" + (r + 1)), gbc);
            gbc.gridx++;
            jp.add(colData[r][0], gbc);
            gbc.gridx++;
            jp.add(colData[r][1], gbc);
        }
//        jp.setPreferredSize(new Dimension(400, 500));
        return jp;
    }

    public String setCanEdit(String canEditStr) {
        if (canEditStr.trim().equals("CANEDIT"))
            bCanEdit = true;
        else
            bCanEdit = false;
        return "OK";
    }

    public String addMaterialList(String matGroup, String matIdNamePairList) {
        matType = matGroup;
        String[] idNamePairs;
        idNamePairs = matIdNamePairList.split("::");
        int len = idNamePairs.length;
        if (len > 0 && (len == (len / 2) * 2)) {
            for (int m = 0; m < len / 2; m++)
                matAndPropsHash.put(idNamePairs[m * 2], new MatAndProps(idNamePairs[m * 2], idNamePairs[m * 2 + 1]));
            return "OK";
        } else
            return "ERROR";
    }

    public String addMatProperty(String id, String propCol, String dataPairStr) {
        MatAndProps matProps;
        if (matAndPropsHash.containsKey(id)) {
            matProps = matAndPropsHash.get(id);
            matProps.addPropData(propCol, dataPairStr);
            return "OK";
        }
        else
            return "ERROR";
    }

    String[] propUnits, propCol, propText;

    public String setPropList(String propList) {
        String[] props;
        props = propList.split("::");
        if (props.length > 3) {
            int nProps = props.length / 3;
            propCol = new String[nProps];
            propText = new String[nProps];
            propUnits = new String[nProps];
            for (int p = 0; p < nProps; p++) {
                propCol[p] = props[3 * p];
                propUnits[p] = props[3 * p + 2];
                propText[p] = props[3 * p + 1] + "(" + propUnits[p] + ")";
            }
            jcProperty = new JComboBox(propText);
            jcProperty.addActionListener(new NewPropListner());

            return "OK";
        } else
            return "ERROR";
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
        if (!matType.equals("Fuels"))
            jcMatNames.addItem("Add New Material ...");
        jcMatNames.addActionListener(matListener);
    }

    void populateDataTable() {
        initColdata();
        MatAndProps matProps = (MatAndProps)jcMatNames.getSelectedItem();
        if (matProps != null) {
            int selProp = jcProperty.getSelectedIndex();
            if (selProp >= 0) {
                String  pCol=  propCol[selProp];
                XYArray arr = matProps.getPropsArr(pCol);
                if (arr != null) {
                    double x, y;
                    for (int r = 0; r < arr.arrLen; r++) {
                        x = arr.getXat(r);
                        y = arr.getYat(r);
                        colData[r][0].setData(x);
                        colData[r][1].setData(y);
                    }
                }

            }
        }
    }

    boolean checkAndSave() {
        boolean datOK = true;
        nValidRows = 0;
        if (bNewMaterial) {
            newMatName = tfNewMaterial.getText().trim();
            int len = newMatName.length();
            if (newMatName.equals("New Material") || len < 2 || len > 25) {
                showError("New Material Name '" + newMatName + "' is not acceptable! ");
                datOK = false;
            }
        }
        if (datOK) {
            if (colData[0][0].getData() < 0) {
                showError("First Entry missing!");
                datOK = false;
            } else {
                double val;
                for (int r = 0; r < MAXROWS; r++) {
                    val = colData[r][0].getData();
                    if (val < 0)
                        break;
                    for (int x = 0; x < r; x++) {
                        if (colData[x][0].getData() == val) {
                            showError("Duplicate Temperature value " + val);
                            datOK = false;
                            break;
                        }
                    }
                    if (!datOK)
                        break;
                    nValidRows++;
                }
            }
            if (datOK) {
                int lastData = -1;
                double val;
                for (int r = MAXROWS - 1; r > 0; r--) {
                    val =  colData[r][0].getData();
                    if (val >= 0) {
                        lastData = r + 1;
                        break;
                    }
                }
                boolean proceed = true;
                if (lastData > nValidRows)
                    proceed = decide("List Length", "Taking Data up to Row " + nValidRows);
                parent().toFront();
                if (proceed) {
                   setDataToSend();
                    if (!bNewMaterial) {
                        proceed = decide("Replacing Existing Data if present",
                                "Selected Property of the Material will be Replaced ...");
                    }
                    if (proceed) {
                        if (win != null)  {
                            win.eval("takeData()");
                            debug("return From takeData()!");
                        }
                    }
                }
            }
        }
        return datOK;
    }

    String propStr, matCode, matType, property, isNewMat, newMatName ="";
    boolean bNewMaterial = false;

    void setDataToSend() {
        if (bNewMaterial) {
            isNewMat = "YES";
            matCode = "0";
        }
        else {
            isNewMat = "NO";
            matCode = ((MatAndProps)jcMatNames.getSelectedItem()).getID();
        }
        property = propCol[jcProperty.getSelectedIndex()];
        propStr = "" + colData[0][0].getData() + "," +  colData[0][1].getData();
        for (int r = 1; r < nValidRows; r++) {
            propStr += "," + colData[r][0].getData() + "," +  colData[r][1].getData();
        }
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

    @Override
    public void destroy() {
        debug("In Destroy");
//          super.destroy();    //To change body of overridden methods use File | Settings | File Templates.
        if (!onTest)
            win.eval("gettingOut()");
        else
            super.destroy();
    }

    void close() {
        destroy();
//        debug("CLOSING ...");
//        mainF.dispose();
//        itsON = false;
    } // close

    boolean takeFileData(String[] dataStr) {
        int nDat = dataStr.length;
        if (nDat > MAXROWS) {
            showMessage("Taking " + MAXROWS + " of the available " + nDat + " Data");
            nDat = MAXROWS;
        }
        String[] data;
        double t, p;
        int srcRow, destRow = 0;
        boolean allOK = true;
        boolean skip;
        for (int b = 0; b < MAXROWS; b++) {
            colData[b][0].setData(-1);
            colData[b][1].setData(0);
        }
        if (nDat > 0) {
            for (srcRow = 0; srcRow < nDat; srcRow++) {
                skip = false;
                data = dataStr[srcRow].split(",");
                if (data.length == 2) {
                    try {
                        t = Double.valueOf(data[0]);
                        // check if already entered
                        for (int x = 0; x < destRow; x++) {
                            if (colData[x][0].getData() == t) {
                                showMessage("Ignoring repeated Data " + dataStr[srcRow]);
                                skip = true;
                            }
                        }
                        if (!skip) {
                            p = Double.valueOf(data[1]);
                            colData[destRow][0].setData(t);
                            colData[destRow][1].setData(p);
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

    void showError(String msg) {
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

        MatAndProps(String id, String name) {
            this.id = id;
            this.name = name;
            propNameAndData = new LinkedHashMap<String, XYArray>();
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

        @Override
        public String toString() {
            return name;
        }
    }


    class NewMatListner implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (jcMatNames.getSelectedItem().equals("Add New Material ...")) {
                tfNewMaterial.setEnabled(true);
                initColdata();
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
            if (!jcMatNames.getSelectedItem().equals("Add New Material ..."))
                populateDataTable();
        }
    }

    class ButtonListener implements ActionListener {
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
                        enableColDataEdit(false);
                     }
                    else {
                        bEditON = true;
                        pbFileData.setEnabled(true);
                        pbSaveData.setEnabled(true);
                        jcMatNames.setEnabled(false);
                        jcProperty.setEnabled(false);
                        enableColDataEdit(true);
                        }
                    break;
                }
                if (command.equals("Save Data")) {
                    checkAndSave();
                    break;
                }
                if (command.equals("Exit")) {
                    close();
                    break;
                }
                if (command.equals("Data From File")) {
                    getFceFromFile();
                    break;
                }
            }
        }
    }

    class  winListener implements WindowListener {
        public void windowOpened(WindowEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void windowClosing(WindowEvent e) {
            debug("mainF CLOSING");
            destroy();
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void windowClosed(WindowEvent e) {
             //To change body of implemented methods use File | Settings | File Templates.
        }

        public void windowIconified(WindowEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void windowDeiconified(WindowEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void windowActivated(WindowEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void windowDeactivated(WindowEvent e) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

}
