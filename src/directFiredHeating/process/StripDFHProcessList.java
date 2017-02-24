package directFiredHeating.process;

import basic.ChMaterial;
import directFiredHeating.applications.StripHeating;
import mvUtils.display.*;
import mvUtils.jsp.JSPComboBox;
import mvUtils.math.BooleanWithStatus;
import mvUtils.math.DoubleRange;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import performance.stripFce.Performance;
import performance.stripFce.StripProcessAndSize;

import javax.sound.sampled.DataLine;
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
    public Vector<ChMaterial> vChMaterial;
    InputControl inpC;
    protected Vector<OneStripDFHProcess> list;
    StripDFHProcessList me;
    protected JComboBox<OneStripDFHProcess> cbProcess;
    protected int maxListLenFP = 10;
    ChMaterial materialForFieldProcess;
    protected double maxStripSpeedFP = 150 * 60;
    protected double stripEntryTempFP = 30;
    protected double minExitZoneTempFP = 900;
    protected double maxExitZoneTempFP = 1050;
    protected double maxStripExitTempFP = 800;
    protected double maxStripThicknessFP = 0.0015;
    protected double minStripThicknessFP = 0.00012;
    protected double wallToStripMin = 0.2;
    protected double maxStripWidthFP = 1.8;
    protected double absMaxStripWidth = 0;
    protected double absMinStripWidth = 0.3;
    JSPComboBox<ChMaterial> cbMaterial;
    NumberTextField ntMaxStripSpeed;
    NumberTextField ntStripEntryTemp;
    NumberTextField ntMaxStripExitTemp;
    NumberTextField ntMinExitZoneTemp;
    NumberTextField ntMaxExitZoneTemp;
    NumberTextField ntMaxProcess;
    NumberTextField ntMaxStripWidth;
    NumberTextField ntMaxStripThickness;
    NumberTextField ntMinStripThickness;


    public StripDFHProcessList(StripHeating dfHeating) {
        this.vChMaterial = dfHeating.vChMaterial;
        this.dfHeating = dfHeating;
        this.inpC = dfHeating;
        list = new Vector<>();
        cbProcess = new JComboBox<>(list);

    }

    void prepareUI() {
        absMaxStripWidth = dfHeating.furnace.getFceWidth() - wallToStripMin * 2;
        maxStripWidthFP = (maxStripWidthFP == 0) ? absMaxStripWidth - 50 :
                new DoubleRange(absMinStripWidth, absMaxStripWidth).limitedValue(maxStripWidthFP);
//        cbProcess = new JComboBox<>(list);
        cbProcess.setPreferredSize(new Dimension(300, 20));
        cbMaterial = new JSPComboBox<>(dfHeating.jspConnection, dfHeating.vChMaterial);
        ntMaxStripSpeed =
                new NumberTextField(dfHeating, (maxStripSpeedFP / 60), 6, true, 20, 1000, "#,##0", "Maximum Strip Speed (mpm)");
        ntStripEntryTemp =
                new NumberTextField(dfHeating, stripEntryTempFP, 6, true, 0, 400, "#,##0", "Strip Entry Temperature (C)");
        ntMaxStripExitTemp =
                new NumberTextField(dfHeating, maxStripExitTempFP, 6, true, 500, 1200, "#,##0", "Maximum Exit Strip Temperature (C)");
        ntMinExitZoneTemp =
                new NumberTextField(dfHeating, minExitZoneTempFP, 6, true, 500, 1200, "#,##0", "Minimum DFHExit-Zone Temperature (C)");
        ntMaxExitZoneTemp =
                new NumberTextField(dfHeating, maxExitZoneTempFP, 6, true, 500, 1200, "#,##0", "Maximum DFHExit-Zone Temperature (C)");
        ntMaxProcess =
                new NumberTextField(dfHeating, maxListLenFP, 6, true, 5, 100, "#,##0", "Maximum Number of Processes");
        ntMaxStripWidth =
                new NumberTextField(dfHeating, maxStripWidthFP * 1000, 6, false, absMinStripWidth * 1000, absMaxStripWidth * 1000,
                        "#,#00", "Maximum Strip Width (mm)");
        ntMinStripThickness =
                new NumberTextField(dfHeating, minStripThicknessFP * 1000, 6, false, 0.01, 5.0, "##0.000", "Minimum Strip Thickness (mm)");
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
        prepareUI();
        MultiPairColPanel mp = new MultiPairColPanel("Settings for Field Process");
        resetUI();
        mp.addItemPair("Material for Field Process", cbMaterial);
        mp.addItemPair(ntMaxStripSpeed);
        mp.addItemPair(ntMaxStripWidth);
        mp.addItemPair(ntMinStripThickness);
        mp.addItemPair(ntMaxStripThickness);
        mp.addItemPair(ntStripEntryTemp);
        mp.addItemPair(ntMaxStripExitTemp);
        mp.addItemPair(ntMinExitZoneTemp);
        mp.addItemPair(ntMaxExitZoneTemp);
        mp.addItemPair(ntMaxProcess);
        return mp;
    }

    public void resetUI() {
        cbMaterial.setSelectedItem(materialForFieldProcess);
        ntMaxStripSpeed.setData(maxStripSpeedFP / 60);
        ntMaxStripWidth.setData(maxStripWidthFP * 1000);
        ntStripEntryTemp.setData(stripEntryTempFP);
        ntMaxStripExitTemp.setData(maxStripExitTempFP);
        ntMaxExitZoneTemp.setData(maxExitZoneTempFP);
        ntMinExitZoneTemp.setData(minExitZoneTempFP);
        ntMaxProcess.setData(maxListLenFP);
    }


    public boolean takeFromUI() {
        boolean retVal = cbMaterial.getSelectedItem() != null &&
                !(ntMaxStripSpeed.inError ||ntMaxStripWidth.inError || ntMinStripThickness.inError ||
                        ntMaxStripThickness.inError|| ntMaxStripExitTemp.inError ||
                        ntMaxExitZoneTemp.inError || ntMaxProcess.inError || ntStripEntryTemp.inError || ntMinExitZoneTemp.inError);
        if (retVal) {
            materialForFieldProcess = (ChMaterial)cbMaterial.getSelectedItem();
            maxStripSpeedFP = ntMaxStripSpeed.getData() * 60;
            maxStripWidthFP = ntMaxStripWidth.getData() / 1000;
            minStripThicknessFP = ntMinStripThickness.getData() / 1000;
            maxStripThicknessFP = ntMaxStripThickness.getData() / 1000;
            stripEntryTempFP = ntStripEntryTemp.getData();
            maxStripExitTempFP = ntMaxStripExitTemp.getData();
            maxExitZoneTempFP = ntMaxExitZoneTemp.getData();
            minExitZoneTempFP = ntMinExitZoneTemp.getData();
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

    public DataWithStatus<OneStripDFHProcess> addFieldProcess(String baseName, double stripExitT, double thick, double width, double speed) {
        DataWithStatus<OneStripDFHProcess> retVal = new DataWithStatus<>();
        String title = "Creating New Field Process";
        if (list.size() < maxListLenFP) {
            if (stripExitT <= maxStripExitTempFP) {
                if (thick <= maxStripThicknessFP && thick >= minStripThicknessFP) {
                    if (width <= maxStripWidthFP) {
                        if (speed <= maxStripSpeedFP) {
                            OneStripDFHProcess theProcess = new OneStripDFHProcess(this, baseName, stripExitT,
                                    thick, width, speed);
                            OneComponentDialog dlg = new OneComponentDialog(dfHeating, title,
                                    theProcess.dataPanel("", dfHeating), new NewProcessDataHandler(theProcess));
                            dlg.setVisible(true);
                            if (dlg.isOk()) {
                                ErrorStatAndMsg stat = theProcess.noteDataFromUI();
                                if (stat.inError)
                                    showError(title, "Error in Process data :" + stat.msg);
                                else {
                                    addOneDFHProcess(theProcess);
                                    retVal.setValue(theProcess);
                                }
                            }
                            else
                                retVal.setErrorMessage("Process Data not Entered");
                        } else
                            retVal.addErrorMessage(String.format("Strip Speed is more than the limit %4.3f mpm", maxStripSpeedFP / 60));
                    } else
                        retVal.addErrorMessage(String.format("Strip Width is more than the limit %4.0f mm", maxStripWidthFP * 1000));
                } else
                    retVal.addErrorMessage(String.format("Strip Thickness is outside the range %4.3f to %4.3f mm",
                            minStripThicknessFP * 1000, maxStripThicknessFP * 1000));
            } else
                retVal.addErrorMessage(String.format("Strip DFH-Exit Temperature is more than the limit %4.0f C", maxStripExitTempFP));
        }
        else {
            retVal.setErrorMessage("The process List is already full, cannot add any more");
//            showError(title, retVal.getErrorMessage() );
        }
        return retVal;
    }

    class NewProcessDataHandler implements DataHandler {
        OneStripDFHProcess newProc;
        NewProcessDataHandler(OneStripDFHProcess newProc) {                     
            this.newProc = newProc;
        }
        @Override
        public ErrorStatAndMsg checkData() {
            ErrorStatAndMsg stat = newProc.checkData();
            if (stat.inError) {
                showError("New Process Data", stat.msg);
            }
            return stat;
        }

        @Override
        public boolean saveData() {
            return false;
        }

        @Override
        public void deleteData() {

        }

        @Override
        public void resetData() {

        }

        @Override
        public void cancel() {

        }
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
            vp = XMLmv.getTag(xmlStr, "maxStripWidthFP", 0);
            if (vp.val.length() > 0)
                maxStripWidthFP = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "minStripThicknessFP", 0);
            if (vp.val.length() > 0)
                minStripThicknessFP = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "maxStripThicknessFP", 0);
            if (vp.val.length() > 0)
                maxStripThicknessFP = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "stripEntryTempFP", 0);
            if (vp.val.length() > 0)
                stripEntryTempFP = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "maxStripExitTempFP", 0);
            if (vp.val.length() > 0)
                maxStripExitTempFP = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "maxExitZoneTempFP", 0);
            if (vp.val.length() > 0)
                maxExitZoneTempFP = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "minExitZoneTempFP", 0);
            if (vp.val.length() > 0)
                minExitZoneTempFP = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "maxListLenFP", 0);
            if (vp.val.length() > 0)
                maxListLenFP = Integer.valueOf(vp.val);


            vp = XMLmv.getTag(xmlStr, "pNum", 0);
            try {
                int pNum = Integer.valueOf(vp.val);
                if (pNum <= maxListLenFP) {
                    for (int p = 0; p < pNum; p++) {
                        vp = XMLmv.getTag(xmlStr, "StripP" + ("" + (p + 1)).trim(), vp.endPos);
                        OneStripDFHProcess oneProc = new OneStripDFHProcess(dfHeating, this, vp.val);
                        if (oneProc.inError) {
                            dfHeating.showError("In reading StripDFHProc: \n" + oneProc.errMeg);
                            retVal = false;
                            break oneBlk;
                        } else {
                            ErrorStatAndMsg stat = checkDuplication(oneProc);
                            if (stat.inError)
                                dfHeating.showError("Skipping process '" + oneProc.getFullProcessID() + "' in Reading Process List\n" +
                                        stat.msg);
                            else
                                addAProcess(oneProc); // was list.add(oneProc);
                        }
                    }
                    retVal = true;
                }
                else
                    showError("Collecting Processes", "Too many processes (must max " + maxListLenFP + ")");
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
        xmlStr.append(XMLmv.putTag("maxStripWidthFP", maxStripWidthFP));
        xmlStr.append(XMLmv.putTag("minStripThicknessFP", minStripThicknessFP));
        xmlStr.append(XMLmv.putTag("maxStripThicknessFP", maxStripThicknessFP));
        xmlStr.append(XMLmv.putTag("stripEntryTempFP", stripEntryTempFP));
        xmlStr.append(XMLmv.putTag("maxStripExitTempFP", maxStripExitTempFP));
        xmlStr.append(XMLmv.putTag("maxExitZoneTempFP", maxExitZoneTempFP));
        xmlStr.append(XMLmv.putTag("minExitZoneTempFP", minExitZoneTempFP));
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
            newOne = addAProcess(oneProcess);    // was list.add(oneProcess);
        return newOne;
    }

    boolean addAProcess(OneStripDFHProcess process) {
        boolean retVal = false;
        if (list.size() < maxListLenFP) {
            list.add(process);
            retVal = true;
        }
        else
            showError("Adding to Process List", "No space in the ProcessList");
        return retVal;
    }

    public boolean removeTheProcess(OneStripDFHProcess oneProcess) {
        boolean retVal = false;
        if (list.contains(oneProcess)) {
            list.remove(oneProcess);
            retVal = true;
        }
        return retVal;
    }

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
                jcbExisting.addItem(p.toString());
//                jcbExisting.addItem(p.getFullProcessID());
            if (editable && list.size() < maxListLenFP)
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
                selectedProcess = new OneStripDFHProcess(pListManager, pName, vChMaterial);
            else
                selectedProcess = getDFHProcess(pName);
            detailsP.removeAll();
            editorPanel = selectedProcess.getEditPanel(inpC, this, editable, bNew);
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
                    }
                    if (canBeSaved) {
                        if (itsNew) {
                            addAProcess(selectedProcess); // was  list.add(selectedProcess);
                        }
                        populateJcbExisting();
//                        jcbExisting.setSelectedItem(selectedProcess.getFullProcessID());
                        response = EditResponse.Response.SAVE;
                        edited = true;
                    }
                    dfHeating.markPerfTobeSaved(true);
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
