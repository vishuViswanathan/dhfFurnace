package directFiredHeating.process;

import basic.ChMaterial;
import directFiredHeating.applications.StripHeating;
import mvUtils.display.*;
import mvUtils.jsp.JSPComboBox;
import mvUtils.math.BooleanWithStatus;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import performance.stripFce.Performance;
import performance.stripFce.StripProcessAndSize;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 15-May-15
 * Time: 3:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class StripDFHProcessList {
    StripHeating dfHeating;
    Vector<ChMaterial> vChMaterial;
    InputControl inpC;
    protected Vector<OneStripDFHProcess> list;
    StripDFHProcessList me;
    protected JComboBox<OneStripDFHProcess> cbProcess;
    protected int maxListLenFP = 10;
    ChMaterial materialForFieldProcess;
    protected double maxStripSpeedFP = 150 * 60;
    protected double maxExitZoneTempFP = 1050;
    protected double maxStripExitTempFP = 800;
    protected double maxStripThicknessFP = 0.0015;
    JSPComboBox<ChMaterial> cbMaterial;
    NumberTextField ntMaxStripSpeed;
    NumberTextField ntMaxStripExitTemp;
    NumberTextField ntMaxExitZoneTemp;
    NumberTextField ntMaxProcess;
    NumberTextField ntMaxStripThickness;


    public StripDFHProcessList(StripHeating dfHeating) {
        this.vChMaterial = dfHeating.vChMaterial;
        this.dfHeating = dfHeating;
        this.inpC = dfHeating;
        list = new Vector<>();
        cbProcess = new JComboBox<>(list);
        cbProcess.setPreferredSize(new Dimension(300, 20));
        cbMaterial = new JSPComboBox<>(dfHeating.jspConnection, dfHeating.vChMaterial);
        ntMaxStripSpeed =
                new NumberTextField(dfHeating, (maxStripSpeedFP / 60), 6, true, 20, 1000, "#,##0", "Maximum Strip Speed (mpm)");
        ntMaxStripExitTemp =
                new NumberTextField(dfHeating, maxStripExitTempFP, 6, true, 500, 1200, "#,##0", "Maximum Exit Strip Temperature (C)");
        ntMaxExitZoneTemp =
                new NumberTextField(dfHeating, maxExitZoneTempFP, 6, true, 500, 1200, "#,##0", "Maximum Zone Temperature (C)");
        ntMaxProcess =
                new NumberTextField(dfHeating, maxListLenFP, 6, true, 5, 100, "#,##0", "Maximum Number of Processes");
        ntMaxStripThickness =
                new NumberTextField(dfHeating, maxStripThicknessFP * 1000, 6, false, 0.01, 5.0, "##0.000", "Maximum Strip Thickness (mm)");
    }

    public void setMaterialForFieldProcess() {
        JSPComboBox<ChMaterial> cbMaterial = new JSPComboBox<>(dfHeating.jspConnection, dfHeating.vChMaterial);
//        if (materialForFieldProcess != null)
            cbMaterial.setSelectedItem(materialForFieldProcess);
        OneComponentDialog dlg = new OneComponentDialog(dfHeating, "Material for Field Process", cbMaterial);
        dlg.setVisible(true);
        if (dlg.isOk()) {
            materialForFieldProcess = (ChMaterial)(cbMaterial.getSelectedItem());
        }
    }

    public JPanel dataPanel() {
        MultiPairColPanel mp = new MultiPairColPanel("Settings for Field Process");
        resetUI();
        mp.addItemPair("Material for Field Process", cbMaterial);
        mp.addItemPair(ntMaxStripSpeed);
        mp.addItemPair(ntMaxStripThickness);
        mp.addItemPair(ntMaxStripExitTemp);
        mp.addItemPair(ntMaxExitZoneTemp);
        mp.addItemPair(ntMaxProcess);
        return mp;
    }

    public void resetUI() {
        cbMaterial.setSelectedItem(materialForFieldProcess);
        ntMaxStripSpeed.setData(maxStripSpeedFP / 60);
        ntMaxStripThickness.setData(maxStripThicknessFP * 1000);
        ntMaxStripExitTemp.setData(maxStripExitTempFP);
        ntMaxExitZoneTemp.setData(maxExitZoneTempFP);
        ntMaxProcess.setData(maxListLenFP);
    }


    public boolean takeFromUI() {
        boolean retVal = cbMaterial.getSelectedItem() != null &&
                !(ntMaxStripSpeed.inError || ntMaxStripThickness.inError|| ntMaxStripExitTemp.inError || ntMaxExitZoneTemp.inError || ntMaxProcess.inError);
        if (retVal) {
            materialForFieldProcess = (ChMaterial)cbMaterial.getSelectedItem();
            maxStripSpeedFP = ntMaxStripSpeed.getData() * 60;
            maxStripThicknessFP = ntMaxStripThickness.getData() / 1000;
            maxStripExitTempFP = ntMaxStripExitTemp.getData();
            maxExitZoneTempFP = ntMaxExitZoneTemp.getData();
            maxListLenFP = (int)ntMaxProcess.getData();
        }
        return retVal;
    }

    public OneStripDFHProcess getSelectedProcess() {
        return (OneStripDFHProcess)cbProcess.getSelectedItem();
    }

    public String getSelectedProcessName() {
        String retVal = "";
        OneStripDFHProcess p = (OneStripDFHProcess)cbProcess.getSelectedItem();
        if (p != null)
            retVal = p.getFullProcessID();
        return retVal;
    }

    public boolean setSelectedProcess(OneStripDFHProcess process) {
        cbProcess.setSelectedItem(process);
        return true;
    }

    public void clear() {
        list.clear();
    }

    public int getCount() {
        return list.size();
    }

    public boolean addStripDFHProcess(Window parent) {
        boolean redo = true;
        OneStripDFHProcess lastSelectedP = null;
        EditResponse.Response lastResponse = EditResponse.Response.EXIT;
        boolean edited = false;
        do {
            AddProcessDlg dlg = new AddProcessDlg(this, true);
            dlg.setLocationRelativeTo(parent);
            if (lastResponse == EditResponse.Response.RESET)
                dlg.setSelectedP(lastSelectedP);
            dlg.setVisible(true);
            lastResponse = dlg.getResponse();
            if (lastResponse == EditResponse.Response.EXIT)
                redo = false;
            lastSelectedP = dlg.getLastSelectedP();
            edited |= dlg.edited;
        } while (redo);
        cbProcess.updateUI();
        cbProcess.setSelectedIndex(-1); // unselect
        return edited;
    }

    public boolean viewStripDFHProcess(Window parent) {
        if (list.size() > 0) {
            AddProcessDlg dlg = new AddProcessDlg(this, false);
            dlg.setLocationRelativeTo(parent);
            dlg.setVisible(true);
            return true;
        }
        else {
            dfHeating.showError("DFH process data list is empty.\n       Try Adding Process");
            return false;
        }
    }

    public OneStripDFHProcess getDFHProcess(String fullProcessID) {
        OneStripDFHProcess oneProcess = null;
        for (OneStripDFHProcess proc: list) {
            if (proc.getFullProcessID().equalsIgnoreCase(fullProcessID)) {
                oneProcess = proc;
                break;
            }
        }
        return oneProcess;
    }

    public OneStripDFHProcess getDFHProcess(StripProcessAndSize theStrip) {
        OneStripDFHProcess oneProcess = null;
        for (OneStripDFHProcess proc: list) {
            if (proc.doesProcessMatch(theStrip)) {
                oneProcess = proc;
                break;
            }
        }
        return oneProcess;
    }

    public OneStripDFHProcess getDFHProcess(String baseProcessNameX, double tempDFHExitX,
                                             double stripWidth, double stripThick) {
        OneStripDFHProcess oneProcess = null;
        for (OneStripDFHProcess proc: list) {
            if (proc.doesProcessMatch(baseProcessNameX, tempDFHExitX, stripWidth, stripThick)) {
                oneProcess = proc;
                break;
            }
        }
        return oneProcess;
    }

    public DataWithStatus<OneStripDFHProcess> getDFHProcess(Performance p) {
        DataWithStatus<OneStripDFHProcess> retVal = new DataWithStatus<>();
        dfHeating.logInfo("getDFHProcess in StripDFHProcessList lis = " + list);
        for (OneStripDFHProcess proc:list) {
            ErrorStatAndMsg reply =  proc.performanceOkForProcess(p);
            if (!reply.inError) {
                BooleanWithStatus tableStat = proc.checkPerformanceTableRange(p);
                if (tableStat.getDataStatus() == DataStat.Status.OK) {
                    retVal.setValue(proc);
                    break;
                }
                else
                    retVal.setValue(proc, tableStat.getInfoMessage());
            }
            else
                retVal.setErrorMessage(reply.msg);
        }
        return retVal;
    }

    public boolean takeStripProcessListFromXML(String xmlStr) {
        list.clear();
        boolean retVal = false;
        ValAndPos vp;
        oneBlk:
        {
            vp = XMLmv.getTag(xmlStr, "materialForFieldProcess", 0);
            if (vp.val.length() > 0)
                materialForFieldProcess = dfHeating.getSelChMaterial(vp.val);
            else
                materialForFieldProcess = null;
            vp = XMLmv.getTag(xmlStr, "maxStripSpeedFP", 0);
            if (vp.val.length() > 0)
                maxStripSpeedFP = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "maxStripThicknessFP", 0);
            if (vp.val.length() > 0)
                maxStripThicknessFP = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "maxStripExitTempFP", 0);
            if (vp.val.length() > 0)
                maxStripExitTempFP = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "maxExitZoneTempFP", 0);
            if (vp.val.length() > 0)
                maxExitZoneTempFP = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "maxListLenFP", 0);
            if (vp.val.length() > 0)
                maxListLenFP = Integer.valueOf(vp.val);


            vp = XMLmv.getTag(xmlStr, "pNum", 0);
            try {
                int pNum = Integer.valueOf(vp.val);
                for (int p = 0; p < pNum; p++) {
                    vp = XMLmv.getTag(xmlStr, "StripP" + ("" + (p + 1)).trim(), vp.endPos);
                    OneStripDFHProcess oneProc = new OneStripDFHProcess(dfHeating, this, vp.val);
                    if (oneProc.inError) {
                            dfHeating.showError("In reading StripDFHProc: \n" + oneProc.errMeg);
                        retVal = false;
                        break oneBlk;
                    }
                    else {
                        ErrorStatAndMsg stat = checkDuplication(oneProc);
                        if (stat.inError)
                            dfHeating.showError("Skipping process '" + oneProc.getFullProcessID() + "' in Reading Process List\n" +
                                    stat.msg);
                        else
                            list.add(oneProc);
                    }
                }
                retVal = true;
            } catch (NumberFormatException e) {
                dfHeating.showError("Error in Number of StripDFHProc");
                break oneBlk;
            }
        }
        cbProcess.updateUI();
        cbProcess.setSelectedIndex(-1); // un-select
        dfHeating.logInfo("Got " + list.size() + " processes");
        return retVal;
    }

    public StringBuilder dataInXMl() {
        StringBuilder xmlStr = new StringBuilder();
        if (materialForFieldProcess != null)
        xmlStr.append(XMLmv.putTag("materialForFieldProcess", materialForFieldProcess.name));
        xmlStr.append(XMLmv.putTag("maxStripSpeedFP", maxStripSpeedFP));
        xmlStr.append(XMLmv.putTag("maxStripThicknessFP", maxStripThicknessFP));
        xmlStr.append(XMLmv.putTag("maxStripExitTempFP", maxStripExitTempFP));
        xmlStr.append(XMLmv.putTag("maxExitZoneTempFP", maxExitZoneTempFP));
        xmlStr.append(XMLmv.putTag("maxListLenFP", maxListLenFP));
        xmlStr.append(XMLmv.putTag("pNum", list.size()));
        int pNum = 0;
        for (OneStripDFHProcess oneProc: list)
            xmlStr.append(XMLmv.putTag("StripP" + ("" + ++pNum).trim(), oneProc.dataInXML().toString()) + "\n");
        return xmlStr;
    }

    public boolean addOneDFHProcess(OneStripDFHProcess oneProcess) {
        boolean newOne = true;
        for (OneStripDFHProcess proc: list) {
            if (proc.baseProcessName.equalsIgnoreCase(oneProcess.baseProcessName)) {
                newOne = false;
                break;
            }
        }
        if (newOne)
            list.add(oneProcess);
        return newOne;
    }

//    public boolean replaceOneDFHProcess(OneStripDFHProcess oneProcess)  {
//        deleteDFHProcess(oneProcess.baseProcessName);
//        list.add(oneProcess);
//        return true;
//    }

//    public boolean deleteDFHProcess(String processName) {
//        boolean retVal = false;
//        processName = processName.toUpperCase();
//        for (OneStripDFHProcess proc: list)  {
//            if (proc.baseProcessName.equals(processName)) {
//                list.remove(proc);
//                retVal = true;
//                break;
//            }
//        }
//        return retVal;
//    }

    ErrorStatAndMsg checkDuplication(OneStripDFHProcess skipThis, String baseProcessNameX, double  exitTempX, double minWidthX, double maxWidthX,
                                     double minThicknessX, double maxThicknessX) {
        ErrorStatAndMsg status = new ErrorStatAndMsg();
        for (OneStripDFHProcess p: list) {
            if (p != skipThis)
                status = p.doesItOverlap(baseProcessNameX, exitTempX, minWidthX, maxWidthX,
                        minThicknessX, maxThicknessX);
            if (status.inError)
                break;
        }
        return status;
    }

    ErrorStatAndMsg checkDuplication(OneStripDFHProcess withThis) {
        ErrorStatAndMsg status = new ErrorStatAndMsg();
        for (OneStripDFHProcess p: list) {
            status = p.doesItOverlap(withThis);
            if (status.inError)
                break;
        }
        return status;
    }

//    ErrorStatAndMsg checkDuplication(OneStripDFHProcess oneProcess, String processName) {
//        ErrorStatAndMsg status = new ErrorStatAndMsg(false, "ERROR: ");
//        if (processName.length() < 2 || processName.substring(0, 1).equals(".")) {
//            status.inError = true;
//            status.msg += "Enter proper process Name";
//        }
//        else {
//            for (OneStripDFHProcess p : list)
//                if ((p != oneProcess) && (p.baseProcessName.equalsIgnoreCase(processName))) {
//                    status.inError = true;
//                    status.msg += "This Process " + processName + " already Exists";
//                    break;
//                }
//        }
//        return status;
//    }

    public JComponent getListUI() {
        return cbProcess;
    }

    void showError(String title, String msg) {
        SimpleDialog.showError(title, msg);
    }

    class AddProcessDlg extends JDialog implements DataHandler{
        boolean bListBeingChanged = false;
        JComboBox jcbExisting;
        String enterNew = "...Enter Name";
        JPanel detailsP;
//        Vector<ChMaterial> vChMaterial;
        InputControl ipc;
        StripDFHProcessList pListManager;
        OneStripDFHProcess selectedProcess;
        DataListEditorPanel editorPanel;
        boolean editable = false;
        EditResponse.Response response;
        boolean edited = false;

        AddProcessDlg(StripDFHProcessList pListManager, boolean editable) {
            this.pListManager = pListManager;
            this.editable = editable;
            setModal(true);
            init();
        }


        void init() {
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    response = EditResponse.Response.EXIT;
                    super.windowClosing(e);
                }
            });
            JPanel outerP = new JPanel(new BorderLayout());
            jcbExisting = new JComboBox();
            populateJcbExisting();
            jcbExisting.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (!bListBeingChanged)
                        getSelectedProcess();
                }
            });
            JPanel p = new JPanel();
            p.add(jcbExisting);
            outerP.add(p, BorderLayout.NORTH);
            detailsP = new JPanel();
//            detailsP.setPreferredSize(new Dimension(400, 300));
            jcbExisting.setSelectedItem(0);
            getSelectedProcess();
            outerP.add(detailsP, BorderLayout.CENTER);
            add(outerP);
            pack();
        }

        EditResponse.Response getResponse() {
            return response;
        }

        void populateJcbExisting() {
            bListBeingChanged = true;
            jcbExisting.removeAllItems();
            for (OneStripDFHProcess p: list)
                jcbExisting.addItem(p.getFullProcessID());
            if (editable)
                jcbExisting.addItem(enterNew);
            bListBeingChanged = false;
        }

        void setSelectedP(OneStripDFHProcess selectedP) {
            jcbExisting.setSelectedItem(selectedP.baseProcessName);
        }

        OneStripDFHProcess getLastSelectedP() {
            return selectedProcess;
        }

        void getSelectedProcess() {
            String pName = (String)jcbExisting.getSelectedItem();
            boolean bNew = (pName == enterNew);
            if (bNew)
                selectedProcess = new OneStripDFHProcess(pListManager, pName, vChMaterial, inpC);
            else
                selectedProcess = getDFHProcess(pName);
            detailsP.removeAll();
            editorPanel = selectedProcess.getEditPanel(vChMaterial, inpC, this, editable, bNew);
            detailsP.add(editorPanel);
            detailsP.updateUI();
//            pack();
        }

        public ErrorStatAndMsg checkData() {
            return selectedProcess.checkData();
        }

        public boolean saveData() {
            boolean canBeSaved = false;
            boolean itsNew = true;
            String errMsg = "";
            OneStripDFHProcess oldProc = selectedProcess.createCopy();
            ErrorStatAndMsg dataStat = selectedProcess.noteDataFromUI();
            Vector<Performance> performancesToDelete = new Vector<>();
            Vector<Performance> performancesToRedoTable = new Vector<>();
            if (!dataStat.inError) {
                canBeSaved = true;
                for (OneStripDFHProcess process : list) {
                    if (process == selectedProcess) {
                        StatusWithMessage statusWithMessage = process.checkPerformanceDataState();
                        DataStat.Status stat = statusWithMessage.getDataStatus();
                        if (stat != DataStat.Status.OK) {
                            canBeSaved = false;
                            errMsg += "Existing Performance data : " + process.performance + "\n  ";
                            if (stat == DataStat.Status.WithErrorMsg) {
                                errMsg += statusWithMessage.getErrorMessage() + "\n";
                                performancesToDelete.add(process.performance);
                            }
                            if (stat == DataStat.Status.WithInfoMsg) {
                                errMsg += statusWithMessage.getInfoMessage() + "\n";
                                performancesToRedoTable.add(process.performance);
                            }
                        }
//                        StatusWithMessage statThick = process.checkPerformanceDataState(false);
//                        stat = statThick.getDataStatus();
//                        if (stat != DataStat.Status.OK) {
//                            canBeSaved = false;
//                            errMsg += "Existing Performance data for Thick Strip: " + process.pThick + "\n  ";
//                            if (stat == DataStat.Status.WithErrorMsg) {
//                                errMsg += statThick.getErrorMessage() + "\n";
//                                performancesToDelete.add(process.pThick);
//                            }
//                            if (stat == DataStat.Status.WithInfoMsg) {
//                                errMsg += statThick.getInfoMessage() + "\n";
//                                performancesToRedoTable.add(process.pThick);
//                            }
//                        }
                        itsNew = false;
                        break;
                    }
                }
                if (!canBeSaved) {
                    canBeSaved = SimpleDialog.decide(this, "Checking with Performance Data", errMsg + "\n" +
                        "     The affected Performance Data has to be Deleted/ Marked for Table update\n" +
                        "     Press 'Yes', if you still want to proceed with the saving." +
                        " Pressing 'No' will revert to original process data") ==  JOptionPane.YES_OPTION;
                }
                if (canBeSaved) {
                    for (Performance p: performancesToDelete)
                        dfHeating.deletePerformance(p);
                    for (Performance p: performancesToRedoTable) {
                        p.setDFHPProcess(selectedProcess);
                        p.markTableToBeRedone();
//                        FceEvaluator eval = dfHeating.furnace.calculateForPerformanceTable(p);
//                        SimpleDialog.showMessage(this, "For " + p, "Re calculating table");
//                        try {
//                            eval.awaitThreadToExit();
//                            if (eval.healthyExit()) {
//                                edited = true;
//                            }
//                        } catch (InterruptedException e) {
//                            SimpleDialog.showError(this, "Updating Performance Table", "Some problem in creating table for " + p);
//                            canBeSaved = false;
//                        }
                    }
                    if (canBeSaved) {
                        if (itsNew) {
                            list.add(selectedProcess);
                        }
                        populateJcbExisting();
//                        jcbExisting.setSelectedItem(selectedProcess.getFullProcessID());
                        response = EditResponse.Response.SAVE;
                        edited = true;
                    }
                }
                else {
                    oldProc.copyTo(selectedProcess);
                    selectedProcess.fillUI();
                }
            }
            else
                showError("Data Entry", dataStat.msg);
            return edited;
        }

        public void deleteData() {
            list.remove(selectedProcess);
            populateJcbExisting();
            jcbExisting.setSelectedItem(selectedProcess.baseProcessName);
            response = EditResponse.Response.DELETE;
            edited = true;
            setVisible(false);
        }

        public void resetData() {
            response = EditResponse.Response.RESET;
            setVisible(false);
        }

        public void cancel() {
//            dataDlg.setVisible(false);
            response = EditResponse.Response.EXIT;
            setVisible(false);
        }
    }
}
