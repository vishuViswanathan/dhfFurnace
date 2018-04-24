package materials;

import basic.FlueComposition;
import basic.Fuel;
import mvUtils.display.DataStat;
import mvUtils.display.DataWithStatus;
import mvUtils.display.InputControl;
import mvUtils.display.SimpleDialog;
import mvUtils.http.PostToWebSite;
import mvUtils.math.XYArray;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.security.MiscUtil;
import protection.CheckAppKey;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Stack;
import java.util.Vector;

/**
 * User: M Viswanathan
 * Date: 12-Jun-17
 * Time: 9:56 AM
 * Modified: 06-Apr-18
 * To change this template use File | Settings | File Templates.
 */
public class FuelData implements InputControl, PropertyControl {
    public int appCode = 0;
    static public String jspBase = "localhost:9080/fceCalculations/jsp/";
    boolean bCanEdit = false;
    JFrame mainF;
    Vector<FuelComponent> fuelComponents;
    Vector<FuelParameters> fuelParemeters;
    boolean itsON = false;
    String user;

    public FuelData() {
        this.appCode = 108;
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
    public boolean setItUp() {
        boolean retVal = false;
        if (getJSPbase()) {
            DataWithStatus<Boolean> runCheck = new CheckAppKey(jspBase).canRunThisApp(appCode, true);
            if (runCheck.getStatus() == DataStat.Status.OK) {
                UIManager.put("ComboBox.disabledForeground", Color.black);
                mainF = new JFrame("Fuel Details");
                mainF.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                fuelComponents = new Stack<>();
                fuelParemeters = new Stack<>();
                getAllData();
                displayIt();
                retVal = true;
            }
            else {
                if (runCheck.getStatus() == DataStat.Status.WithErrorMsg)
                    showError("Access Check: ", runCheck.getErrorMessage());
                else
                    showError("Access Check: ","Some problem in getting Application permissions");
            }
        }
        return retVal;
    }

    public void init() {
        UIManager.put("ComboBox.disabledForeground", Color.black);
        mainF = new JFrame("Fuel Details");
        mainF.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fuelComponents = new Stack<>();
        fuelParemeters = new Stack<>();
        getAllData();
        displayIt();
    }

    JComboBox cbFuels;
    JPanel detPan;

    class CBFuelListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            setDetailsPanel();
        }
    }

    String newFuelName = "..... CREATE NEW FUEL";
    FuelParameters newFuelParameters;
    void populateCBFuel() {
        cbFuels = new JComboBox(fuelParemeters);
        cbFuels.setSelectedIndex(0);
        cbFuels.addActionListener(new CBFuelListener());
    }

    private void addNewFuelChoice() {
        if (bCanEdit) {
            try {
                Fuel newF = new Fuel(newFuelName);
                newFuelParameters = new FuelParameters(this, this, newF, "G", fuelComponents, true, bCanEdit);
                newFuelParameters.setComposition(0, 0, 0, 0, 0, 0, 0, 1,0,0,0);
                fuelParemeters.add(newFuelParameters);
            } catch (Exception e) {
            }
        }

    }

    void createUIs() {
        if (!itsON) {
            populateCBFuel();
            JPanel topP = new JPanel();

            topP.add(cbFuels);
            mainF.add(topP, BorderLayout.NORTH);
            setDetailsPanel();
            mainF.setLocation(20, 10);
            mainF.pack();
        }
    }

    void setDetailsPanel() {
        if (detPan != null)
            mainF.remove(detPan);
        FuelParameters selected =  (FuelParameters)cbFuels.getSelectedItem();
        detPan = selected.fuelDetPanel();
        mainF.add(detPan, BorderLayout.CENTER );
        detPan.updateUI();
    }

    boolean getAllData() {
        return getFuelComponents() && getAllFuelAllDetails();
    }

    boolean getFuelComponents() {
        PostToWebSite jspSrc = new PostToWebSite("http://" + jspBase);
        HashMap<String, String> params = new HashMap<>();
        params.put("user", user);
        String xmlComponents = jspSrc.getByPOSTRequest("getAllFuelComponents.jsp", params, 1000000);
        setFuelComponentsFromXMl(xmlComponents);
        return true;
    }

    boolean getAllFuelAllDetails() {
        PostToWebSite jspSrc = new PostToWebSite("http://" + jspBase);
        HashMap<String, String> params = new HashMap<>();
        params.put("user", user);
        String xmlAllFuelParameters = jspSrc.getByPOSTRequest("getAllFuelAllDetails.jsp", params, 1000000);
        getAllFuelsDetailsFromXML(xmlAllFuelParameters, false);
        return true;

    }

    boolean setFuelComponentsFromXMl(String xmlStr) {
        boolean retVal = false;
        ValAndPos vp;
        int nC;
        ValAndPos localVp;
        String[] keys = {"IDinMaterialCode", "ElemName", "Units", "ElementType", "MolWeight", "CalValue",
                "Density", "Hatoms", "Catoms", "Oatoms", "Satoms", "Natoms", "HeatContStr"};
        LinkedHashMap<String, String> pHash = new LinkedHashMap<>();
        String oneElement;
        vp = XMLmv.getTag(xmlStr, "Status", 0);
        if (vp.val.equalsIgnoreCase("OK")) {
            vp = XMLmv.getTag(xmlStr, "nC", vp.endPos);
            if (vp.val.length() > 0) {
                nC = Integer.valueOf(vp.val);
                retVal = true;
                for (int c = 0; c < nC && retVal; c++) {
                    vp = XMLmv.getTag(xmlStr, "Element", vp.endPos);
                    oneElement = vp.val;
                    if (oneElement.length() > 0) {
                        for (String key : keys) {
                            pHash.put(key, XMLmv.getStringVal(oneElement, key));
                        }
                        String[] p = pHash.values().toArray(new String[]{""});
                        addFuelComponents(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], p[11], p[12]);
                        retVal = true;
                    } else
                        retVal = false;
                }
                if (!retVal)
                    showError("Some error in getting Fuel Components");
            }
        } else {
            if (vp.val.equalsIgnoreCase("Error")) {
                vp = XMLmv.getTag(xmlStr, "Msg", vp.endPos);
                showError(vp.val);

            } else
                showError("Unknown response from server for Fuel Components");
        }
        return retVal;
    }

    boolean getAllFuelsDetailsFromXML(String xmlStr, boolean update) {
        fuelParemeters.clear();
        boolean retVal = false;
        ValAndPos vp;
        int nF;
        LinkedHashMap<String, String> pHash = new LinkedHashMap<>();
        String oneElement;
        vp = XMLmv.getTag(xmlStr, "Status", 0);
        if (vp.val.equalsIgnoreCase("OK")) {
            vp = XMLmv.getTag(xmlStr, "nF", vp.endPos);
            if (vp.val.length() > 0) {
                nF = Integer.valueOf(vp.val);
                retVal = true;
                for (int c = 0; c < nF && retVal; c++) {
                    vp = XMLmv.getTag(xmlStr, "FuelData", vp.endPos);
                    oneElement = vp.val;
                    retVal = setOneFuelsDetailsFromXML(oneElement, update);
                }
                if (retVal) {
                    addNewFuelChoice();
                    if (cbFuels != null)
                        cbFuels.updateUI();
                }
                else
                    showError("Some error in getting Fuel details");
            }
        }
        else {
            if (vp.val.equalsIgnoreCase("Error")) {
                vp = XMLmv.getTag(xmlStr, "Msg", vp.endPos);
                showError(vp.val);

            }
            else
                showError("Unknown response from server for Fuel Details");
        }
        return retVal;
    }

    boolean setOneFuelsDetailsFromXML(String xmlStr, boolean update) {
        return addFuelData(xmlStr, update);
    }

    public void displayIt() {
        createUIs();
        if (!itsON) {
            itsON = true;
            mainF.setFocusable(true);
            mainF.setVisible(true);
            mainF.requestFocus();
            mainF.toFront();
            mainF.setSize(700, 500);
        }
    }

    public String addFuelComponents(String idInMaterialCode, String name, String units, String type, String molWtStr,
                                    String calValStr, String densityStr, String hAtomsStr, String cAtomsStr,
                                    String oAtomsStr, String sAtomsStr, String nAtomsStr, String heatContStr) {
        String retVal = "OK";
        double molWt, calVal, density, hAtoms, cAtoms, oAtoms, sAtoms, nAtoms;
        try {
            molWt = Double.valueOf(molWtStr);
            calVal = Double.valueOf(calValStr);
            density = Double.valueOf(densityStr);
            hAtoms = Double.valueOf(hAtomsStr);
            cAtoms = Double.valueOf(cAtomsStr);
            oAtoms = Double.valueOf(oAtomsStr);
            sAtoms = Double.valueOf(sAtomsStr);
            nAtoms = Double.valueOf(nAtomsStr);
        } catch (NumberFormatException e) {
            retVal = "ERROR: Number Format in FuelComponent Data!";
            return retVal;
        }
        fuelComponents.add(new FuelComponent(idInMaterialCode, name, units, type, molWt, calVal, density, hAtoms,
                cAtoms, oAtoms, sAtoms, nAtoms, heatContStr));
        return retVal;
    }

    public boolean addFuelData(String xmlStr, boolean update)  {
        int id;
        double calVal, airFuelRatio, flueFuelRatio;
        double fractCO2, fractH2O, fractN2, fractO2, fractSO2;
        double h2, co, ch4, c2h6, c2h2, c3h8, c4h10, n2, h2o, co2, o2;
        XYArray sensHeat = null;

        String name = XMLmv.getStringVal(xmlStr, "FuelName");
        String sensHeatPair =  XMLmv.getStringVal(xmlStr, "HeatContStr");
        String type =  XMLmv.getStringVal(xmlStr, "FuelType");
        String units =  XMLmv.getStringVal(xmlStr, "Units");
        if (sensHeatPair.trim().length() > 0)
            sensHeat = new XYArray(sensHeatPair);
        try {
            id = Integer.valueOf(XMLmv.getTag(xmlStr, "ID").val);
            calVal = Double.valueOf(XMLmv.getTag(xmlStr, "CalVal").val);
            airFuelRatio = Double.valueOf(XMLmv.getTag(xmlStr, "AirFuelRatio").val);
            flueFuelRatio = Double.valueOf(XMLmv.getTag(xmlStr, "FlueFuelRatio").val);
            fractCO2 = Double.valueOf(XMLmv.getTag(xmlStr, "CO2fract").val);
            fractH2O = Double.valueOf(XMLmv.getTag(xmlStr, "H2Ofract").val);
            fractN2 = Double.valueOf(XMLmv.getTag(xmlStr, "N2fract").val);
            fractO2 = Double.valueOf(XMLmv.getTag(xmlStr, "O2fract").val);
            fractSO2 = Double.valueOf(XMLmv.getTag(xmlStr, "SO2fract").val);
            h2 = Double.valueOf(XMLmv.getTag(xmlStr, "H2").val);
            co = Double.valueOf(XMLmv.getTag(xmlStr, "CO").val);
            ch4 = Double.valueOf(XMLmv.getTag(xmlStr, "CH4").val);
            c2h6 = Double.valueOf(XMLmv.getTag(xmlStr, "C2H6").val);
            c2h2 = Double.valueOf(XMLmv.getTag(xmlStr, "C2H2").val);
            c3h8 = Double.valueOf(XMLmv.getTag(xmlStr, "C3H8").val);
            c4h10 = Double.valueOf(XMLmv.getTag(xmlStr, "C4H10").val);
            n2 = Double.valueOf(XMLmv.getTag(xmlStr, "N2").val);
            h2o = Double.valueOf(XMLmv.getTag(xmlStr, "H2O").val);
            co2 = Double.valueOf(XMLmv.getTag(xmlStr, "CO2").val);
            o2 = Double.valueOf(XMLmv.getTag(xmlStr, "O2").val);
        } catch (NumberFormatException e) {
            showError("Number Format in Fuel Data!");
            return false;
        }
        FlueComposition flueComp;
        try {
            flueComp = new FlueComposition("Flue of " + name, fractCO2, fractH2O, fractN2, fractO2, fractSO2);
        } catch (Exception e) {
            showError ("ERROR:" + e.getMessage());
            return false;
        }
        Fuel fuel = new Fuel(name, units, calVal, airFuelRatio, flueFuelRatio, sensHeat, flueComp);
        fuel.setID(id);
        FuelParameters fDet = new FuelParameters(this, this, fuel, type, fuelComponents, false, bCanEdit);
//debug("fuelType " + type + " for fuel " + name);
        if (type.equals("G")) {
//debug("setting fuel composition for " + name);
            boolean bResp = fDet.setComposition(h2, co, ch4, c2h6, c2h2, c3h8, c4h10, n2, h2o, co2, o2);
            if (bResp)
                if (update)
                    updateOneFuel(fDet);
                else
                    fuelParemeters.add(fDet);
            else {
                showError("ERROR: in fuel Composition!");
                return false;
            }
        }
        return true;
    }

    boolean updateOneFuel(FuelParameters fDet) {
        boolean done = false;
        for (FuelParameters oneDet: fuelParemeters) {
            if (oneDet.getFuelID() == fDet.getFuelID()) {
                int selectedIndex = cbFuels.getSelectedIndex();
                fuelParemeters.remove(oneDet);
                fuelParemeters.add(selectedIndex, fDet);
                setDetailsPanel();
//                cbFuels.updateUI();
                done = true;
                break;
            }
        }
        return done;
    }

    boolean bCanNotify = true;

    public boolean canNotify() {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void enableNotify(boolean ena) {
        bCanNotify = ena;
    }

    public Frame parent() {
        return mainF;
    }

    boolean decide(String title, String msg) {
        int resp = JOptionPane.showConfirmDialog(parent(), msg, title, JOptionPane.YES_NO_OPTION);
        parent().toFront();
        if (resp == JOptionPane.YES_OPTION)
            return true;
        else
            return false;
    }

    void debug(String msg) {
        System.out.println("FuelData " + msg);
    }

    public void showError(String msg) {
        JOptionPane.showMessageDialog(parent(), msg, "ERROR", JOptionPane.ERROR_MESSAGE);
        if (mainF != null)
            mainF.toFront();
    }

    public void showError(String title, String msg) {
        JOptionPane.showMessageDialog(parent(), msg, title, JOptionPane.ERROR_MESSAGE);
        if (mainF != null)
            mainF.toFront();
    }

    void showMessage(String msg) {
        JOptionPane.showMessageDialog(parent(), msg, "FOR INFORMATION", JOptionPane.INFORMATION_MESSAGE);
        if (mainF != null)
            mainF.toFront();
    }

    void close() {
        debug("CLOSING ...");
        mainF.dispose();
        itsON = false;
        System.exit(0);
    }

    public boolean saveData(Object o) {
        boolean retVal = false;
        if (o instanceof FuelParameters) {
            FuelParameters fD = (FuelParameters)o;
            String name = fD.name;
            String trimmedName = name.trim().replace(" ", "").replace("-", "");
            boolean isNew = fD.bNew;
            if (name.substring(0,1).equals("."))
                showError("Fuel name " + name + " is not acceptable");
            else if (name.length() > 30)
                showError("Fuel Name '" + name + "' is too long (> 30)!");
            else {
                boolean bSave = true;
                FuelParameters existing;
                if (isNew) {
                    for (int f = 0; f < fuelParemeters.size(); f++) {
                        existing = fuelParemeters.get(f);
                        if (existing != newFuelParameters)  { // do not check with new entry
                            if (trimmedName.equalsIgnoreCase(existing.name.trim().replace(" ", "").replace("-", ""))) {
                                showError("Fuel Name '" + name + "' already Exists!\nTry different Name ..." );
                                bSave = false;
                                break;
                            }
                        }
                    }
                }
                else {
                    bSave = decide("Data change for Existing Fuel", "Data for Fuel '" + name + "' will be OVERWRITTEN!");
                }

                if (bSave)  {
                    String xmlStr = fD.dataInXML();
                    PostToWebSite jspSrc = new PostToWebSite("http://" + jspBase);
                    HashMap<String, String> params = new HashMap<>();
                    params.put("user", user);
                    params.put("itsNew", (isNew ? "1": "0"));
                    params.put("allData", xmlStr);
                    params.put("heatContStr", fD.getHContStr());
                    String response = jspSrc.getByPOSTRequest("modifyFuelProperties.jsp", params, 1000000);
                    String status = XMLmv.getStringVal(response, "Status");
                    if (status.equalsIgnoreCase("OK")) {
                        retVal = true;
                    }
                    else {
                        if (status.equalsIgnoreCase("ERROR"))
                            showError(XMLmv.getStringVal(response, "Msg"));
                        else
                            showError("Unknown response from server: " + response);
                    }
                }
            }
        }
        if (retVal) {
            showMessage("Data saved");
            getAllData();
            cbFuels.setSelectedIndex(0);
//            getOneFuelAllDetails(((FuelParameters)cbFuels.getSelectedItem()).getFuelID());
        }
        return retVal;
    }

    public boolean deleteData(Object o) {
        boolean retVal = false;
        if (decide("Deleting a Fuel",
                "Do you want to PERMANENTLY DELETE this Fuel") &&
                decide("DELETING FUEL",  "RE-CONFIRM Fuel DELETION")) {
            showMessage("Deleting Fuel");
            if (o instanceof FuelParameters) {
                FuelParameters fD = (FuelParameters)o;
                String xmlStr = fD.basicDataInXML();
                PostToWebSite jspSrc = new PostToWebSite("http://" + jspBase);
                HashMap<String, String> params = new HashMap<>();
                params.put("user", user);
                params.put("allData", xmlStr);
                String response = jspSrc.getByPOSTRequest("deleteFuel.jsp", params, 1000000);
                String status = XMLmv.getStringVal(response, "Status");
                if (status.equalsIgnoreCase("OK")) {
                    retVal = true;
                }
                else {
                    if (status.equalsIgnoreCase("ERROR"))
                        showError(XMLmv.getStringVal(response, "Msg"));
                    else
                        showError("Unknown response from server: " + response);
                }
            }
            if (retVal) {
                showMessage("Fuel Deleted");
                getAllData();
                cbFuels.setSelectedIndex(0);
//            getOneFuelAllDetails(((FuelParameters)cbFuels.getSelectedItem()).getFuelID());
            }
        }
        return retVal;
    }

    public void inEdit(boolean bInEdit) {
        if (bInEdit) {
            cbFuels.setEnabled(false);
        }
        else {
            cbFuels.setEnabled(true);
        }
    }

    public void quit() {
        close();
    }

    class ButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            while (true) {
                if (command.equals("Save Data")) {
                    break;
                }
            }
        }
    }

    public static void main(String[] args)  {
        FuelData fD = new FuelData();
        fD.setItUp();
    }
}
