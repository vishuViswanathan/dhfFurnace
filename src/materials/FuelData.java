package materials;

import basic.FlueComposition;
import basic.Fuel;
import mvUtils.display.DataStat;
import mvUtils.display.DataWithStatus;
import mvUtils.display.InputControl;
import mvUtils.http.PostToWebSite;
import mvUtils.math.XYArray;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.security.MiscUtil;
import netscape.javascript.JSObject;
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
 * To change this template use File | Settings | File Templates.
 */
public class FuelData extends JApplet implements InputControl, PropertyControl {
    public int appCode = 0;
    //    static public String jspBase = "HYPWAP02:9080/fceCalculations/jsp/";
    static public String jspBase = "localhost:9080/fceCalculations/jsp/";
    boolean bCanEdit = false;
    JSObject win;
    JFrame mainF;
    boolean onTest = false;
    InputControl controller;
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
        if (bCanEdit) {
            try {
                Fuel newF = new Fuel(newFuelName);
                newFuelParameters = new FuelParameters(this, this, newF, "G", fuelComponents, true, bCanEdit);
                newFuelParameters.setComposition(0, 0, 0, 0, 0, 0, 0, 1,0,0,0);
                fuelParemeters.add(newFuelParameters);
            } catch (Exception e) {
            }
        }
//        cbFuels = new JComboBox(fuelParemeters.toArray());
        cbFuels = new JComboBox(fuelParemeters);
        cbFuels.setSelectedIndex(0);
        cbFuels.addActionListener(new CBFuelListener());
    }

    void createUIs() {
        if (!itsON) {
            populateCBFuel();
            JPanel topP = new JPanel();

            topP.add(cbFuels);
            mainF.add(topP, BorderLayout.NORTH);
//            mainF.addWindowListener(new winListener());
//            setMenuOptions();
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
//        PostToWebSite jspSrc = new PostToWebSite(jspBase);
//        HashMap<String, String> params = new HashMap<>();
//        params.put("user", user);
//        String xmlComponents = jspSrc.getByPOSTRequest("getAllFuelComponents.jsp", params, 1000000);
//        setFuelComponentsFromXMl(xmlComponents);
//        String xmlAllFuelParameters = jspSrc.getByPOSTRequest("getAllFuelAllDetails.jsp", params, 1000000);
//        setAllFuelsDetailsFromXMl(xmlAllFuelParameters);
//        return true;
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

    boolean getOneFuelAllDetails(int fuelID) {
        PostToWebSite jspSrc = new PostToWebSite("http://" + jspBase);
        HashMap<String, String> params = new HashMap<>();
        params.put("user", user);
        params.put("fuelID", "" + fuelID);
        String xmlAllFuelParameters = jspSrc.getByPOSTRequest("getOneFuelAllDetails.jsp", params, 1000000);
        getOneFuelsDetailsFromXML(xmlAllFuelParameters, true);
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
                    showError("Some error in getting Fel Components");
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
        boolean retVal = false;
        ValAndPos vp;
        int nF;
//        ValAndPos localVp;
//        String[] keys = {"ID", "FuelName", "FuelType", "Units", "CalVal",  "AirFuelRatio",
//                "FlueFuelRatio", "HeatContStr", "CO2fract", "H2Ofract", "SO2fract", "N2fract", "O2fract",
//                "H2", "CO", "CH4", "C2H6", "C2H2", "C3H8", "C4H10", "N2", "H2O", "CO2", "O2", "C", "S", "Ash"};
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
//                    if (oneElement.length() > 0) {
//                        for (String key:keys) {
//                            pHash.put(key, XMLmv.getStringVal(oneElement, key));
//                        }
//                        String[] p = pHash.values().toArray(new String[]{""});
//                        addFuelData(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8],p[9], p[10], p[11], p[12],
//                                p[13], p[14], p[15], p[16], p[17], p[18], p[19], p[20],p[21], p[22], p[23], p[24],
//                                p[25], p[26], update);
//                        retVal = true;
//                    }
////                    else
////                        retVal = false;
                }
                if (!retVal)
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

    boolean getOneFuelsDetailsFromXML(String xmlStr, boolean update) {
        boolean retVal = false;
        ValAndPos vp;
        int nF;
        LinkedHashMap<String, String> pHash = new LinkedHashMap<>();
        String oneElement;
        vp = XMLmv.getTag(xmlStr, "Status", 0);
        if (vp.val.equalsIgnoreCase("OK")) {
            vp = XMLmv.getTag(xmlStr, "FuelData", vp.endPos);
            oneElement = vp.val;
            retVal = setOneFuelsDetailsFromXML(oneElement, update);
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
        boolean retVal = false;
        ValAndPos vp;
        String[] keys = {"ID", "FuelName", "FuelType", "Units", "CalVal",  "AirFuelRatio",
                "FlueFuelRatio", "HeatContStr", "CO2fract", "H2Ofract", "SO2fract", "N2fract", "O2fract",
                "H2", "CO", "CH4", "C2H6", "C2H2", "C3H8", "C4H10", "N2", "H2O", "CO2", "O2", "C", "S", "Ash"};
        LinkedHashMap<String, String> pHash = new LinkedHashMap<>();
        if (xmlStr.length() > 0) {
            for (String key:keys) {
                pHash.put(key, XMLmv.getStringVal(xmlStr, key));
            }
            String[] p = pHash.values().toArray(new String[]{""});
            addFuelData(p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8],p[9], p[10], p[11], p[12],
                    p[13], p[14], p[15], p[16], p[17], p[18], p[19], p[20],p[21], p[22], p[23], p[24],
                    p[25], p[26], update);
            retVal = true;
        }
        return retVal;
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

    public String addFuelData(String idStr, String name, String type, String units, String calValStr, String airFuelRatioStr,
                              String flueFuelRatioStr, String sensHeatPair,
                              String fractCO2str, String fractH2Ostr, String fractN2str, String fractO2str, String fractSO2str,
                              String h2Str, String coStr, String ch4Str,
                              String c2h6Str, String c2h2Str, String c3h8Str, String c4h10Str,
                              String n2Str, String h2oStr, String co2Str, String o2Str, String cStr, String sStr, String ashStr,
                              boolean update)  {
        String retVal = "OK";
        int id;
        double calVal, airFuelRatio, flueFuelRatio;
        double fractCO2, fractH2O, fractN2, fractO2, fractSO2;
        double h2, co, ch4, c2h6, c2h2, c3h8, c4h10, n2, h2o, co2, o2;
        XYArray sensHeat = null;
        if (sensHeatPair.trim().length() > 0)
            sensHeat = new XYArray(sensHeatPair);
        try {
            id = Integer.valueOf(idStr);
            calVal = Double.valueOf(calValStr);
            airFuelRatio = Double.valueOf(airFuelRatioStr);
            flueFuelRatio = Double.valueOf(flueFuelRatioStr);
            fractCO2 = Double.valueOf(fractCO2str);
            fractH2O = Double.valueOf(fractH2Ostr);
            fractN2 = Double.valueOf(fractN2str);
            fractO2 = Double.valueOf(fractO2str);
            fractSO2 = Double.valueOf(fractSO2str);
            h2 = Double.valueOf(h2Str);
            co = Double.valueOf(coStr);
            ch4 = Double.valueOf(ch4Str);
            c2h6 = Double.valueOf(c2h6Str);
            c2h2 = Double.valueOf(c2h2Str);
            c3h8 = Double.valueOf(c3h8Str);
            c4h10 = Double.valueOf(c4h10Str);
            n2 = Double.valueOf(n2Str);
            h2o = Double.valueOf(h2oStr);
            co2 = Double.valueOf(co2Str);
            o2 = Double.valueOf(o2Str);
        } catch (NumberFormatException e) {
            retVal = "ERROR: Number Format in Fuel Data!";
            return retVal;
        }
        FlueComposition flueComp = null;
        try {
            flueComp = new FlueComposition("Flue of " + name, fractCO2, fractH2O, fractN2, fractO2, fractSO2);
        } catch (Exception e) {
            return ("ERROR:" + e.getMessage());
        }
        Fuel fuel = new Fuel(name, units, calVal, airFuelRatio, flueFuelRatio, sensHeat, flueComp);
        fuel.setID(id);
        FuelParameters fDet = new FuelParameters(this, this, fuel, type, fuelComponents, false, bCanEdit);
//debug("fuelType " + type + " for fuel " + name);
        if (type.equals("G"))  {
//debug("setting fuel composition for " + name);
            boolean bResp = fDet.setComposition(h2, co, ch4, c2h6, c2h2, c3h8, c4h10, n2, h2o, co2, o2);
            if (bResp)
                if (update)
                    updateOneFuel(fDet);
                else
                    fuelParemeters.add(fDet);
            else
                retVal = "ERROR: in fuel Composition!";
        }
        return retVal;
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
        parent().toFront();
    }

    void showMessage(String msg) {
        JOptionPane.showMessageDialog(parent(), msg, "FOR INFORMATION", JOptionPane.INFORMATION_MESSAGE);
        parent().toFront();
    }


    @Override
    public void destroy() {
        debug("In Destroy");
        super.destroy();
        if (!onTest)
            win.eval("gettingOut()");
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
            getOneFuelAllDetails(((FuelParameters)cbFuels.getSelectedItem()).getFuelID());
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
