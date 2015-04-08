package PropertySetter;

import mvUtils.display.InputControl;
import mvUtils.display.NumberTextField;
import mvUtils.display.FramedPanel;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 10/3/12
 * Time: 12:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class SetProperty extends JApplet implements InputControl {
    int MAXROWS = 20;
    JComboBox jcMatTypes, jcMatNames, jcProperty;
    JButton jbCancel, jbReplaceProp, jbSupplement;
    LinkedHashMap<String, IDandName[]> htMaterials;
    JSObject win;
    JFrame mainF;
    boolean onTest;
    JTextField tfNewMaterial;
    JTable propTable;
    NumberTextField[][] colData;
    JButton pbSaveData, pbCLose, pbFileData;
    boolean itsON = false;

    public SetProperty() {
    }

    public void init() {
        UIManager.put("ComboBox.disabledForeground", Color.black);
        String strTest = this.getParameter("OnTest");
        mainF = new JFrame("Set Material Property");
        mainF.addWindowListener(new winListener());
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
        jcMatTypes = new JComboBox();
        jcMatTypes.setPreferredSize(new Dimension(100, 25));
        htMaterials = new LinkedHashMap<String, IDandName[]>();
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
        String[] colName = new String[]{"temperature", "Property"};
        propTable = new JTable(colData, colName);
        ButtonListener li = new ButtonListener();
        Dimension pbSize = new Dimension(100, 25);
        pbSaveData = new JButton("Save Data");
        pbSaveData.setPreferredSize(pbSize);
        pbSaveData.addActionListener(li);
        pbCLose = new JButton("Exit");
        pbCLose.setPreferredSize(pbSize);
        pbCLose.addActionListener(li);
        pbFileData = new JButton("Data From File");
        pbFileData.addActionListener(li);

    }


    void setTestData() {
        setPropList("HeatContent::Heat Content::kcal/kg::Emissivity::Emissivity::::ThermalConductivity::Thermal Conductivity::kcal/mhC");
        addMaterialList("Gases", "1001::CO2::1004::H2O::1008::N2");
        addMaterialList("Steels", "2001::Low Carbon Steel::2002::High Carbon Steel");
//        populateMatList();
    }

    public void displayIt() {
        if (!itsON) {
            populateMatList();
            itsON = true;
            jcMatTypes.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    populateMatList();
                }
            });
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
        jp.add(jcMatTypes, gbc);
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
        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        Insets ins = new Insets(1, 1, 1, 1);
        gbc.insets = ins;
        jp.add(pbFileData, gbc);
        ins.set(1, 50, 1, 5);
        gbc.gridx++;
        jp.add(pbCLose, gbc);
        gbc.gridx++;
        ins.set(1, 5, 1, 1);
        jp.add(pbSaveData, gbc);
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

    public String addMaterialList(String matType, String matIdNamePairList) {
        String[] idNamePairs;
        idNamePairs = matIdNamePairList.split("::");
        int len = idNamePairs.length;
        IDandName[] idAndname = new IDandName[len / 2];
        if (len > 0 && (len == (len / 2) * 2)) {
            jcMatTypes.addItem(matType);
            for (int m = 0; m < len / 2; m++)
                idAndname[m] = new IDandName(idNamePairs[m * 2], idNamePairs[m * 2 + 1], matType);
            htMaterials.put(matType, idAndname);
            return "OK";
        } else
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

            return "OK";
        } else
            return "ERROR";
    }

    void populateMatList() {
        jcMatNames.removeActionListener(matListener);
        jcMatNames.removeAllItems();
        String matType = "";
        if (jcMatTypes.getSelectedIndex() >= 0) {
            matType = "" + jcMatTypes.getSelectedItem();
            IDandName[] idAndname = htMaterials.get(matType);
            for (int m = 0; m < idAndname.length; m++)
                jcMatNames.addItem(idAndname[m]);
        }
        if (!matType.equals("Fuels"))
            jcMatNames.addItem("Add New Material ...");
        jcMatNames.addActionListener(matListener);
    }

    int nValidRows = 0;

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
debug("called SetDataToSend, win = " + win);
                    if (win != null)  {
                        win.eval("takeData()");
                        debug("return From takeData()!");
                    }
                }
            }
        }
        return datOK;
    }

    String propStr, matCode, matType, property, isNewMat, newMatName ="";
    boolean bNewMaterial = false;

    void setDataToSend() {
        matType = (String) jcMatTypes.getSelectedItem();
        if (bNewMaterial) {
            isNewMat = "YES";
            matCode = "0";
        }
        else {
            isNewMat = "NO";
            matCode = ((IDandName)jcMatNames.getSelectedItem()).getID();
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
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void enableNotify(boolean ena) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    void debug(String msg) {
        System.out.println("SetProperty " + msg);
    }

    void showError(String msg) {
        JOptionPane.showMessageDialog(parent(), msg, "ERROR", JOptionPane.ERROR_MESSAGE);
        parent().toFront();
    }

    void showMessage(String msg) {
        JOptionPane.showMessageDialog(parent(), msg, "FOR INFORMATION", JOptionPane.INFORMATION_MESSAGE);
        parent().toFront();
    }

    class IDandName {
        String id;
        String name;
        String matType;

        IDandName(String id, String name, String matType) {
            this.id = id;
            this.name = name;
            this.matType = matType;
        }

        String getID() {
            return id;
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
                bNewMaterial = true;
            } else {
                tfNewMaterial.setEnabled(false);
                bNewMaterial = false;
            }
        }
    }

    class ButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            while (true) {
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
