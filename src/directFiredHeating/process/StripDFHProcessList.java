package directFiredHeating.process;

import basic.ChMaterial;
import directFiredHeating.DFHeating;
import mvUtils.display.*;
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
    DFHeating dfHeating;
    Vector<ChMaterial> vChMaterial;
    InputControl inpC;
    protected Vector<OneStripDFHProcess> list;
    StripDFHProcessList me;
    protected JComboBox<OneStripDFHProcess> cbProcess;
    protected int maxListLen = 5;

    public StripDFHProcessList(DFHeating dfHeating) {
        this.vChMaterial = dfHeating.vChMaterial;
        this.dfHeating = dfHeating;
        this.inpC = dfHeating;
        list = new Vector<>();
        cbProcess = new JComboBox<>(list);
        cbProcess.setPreferredSize(new Dimension(300, 20));
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

//    public boolean setSelectedProcess(String processName) {
//        OneStripDFHProcess process = getDFHProcess(processName);
//        boolean retVal = false;
//        if (process != null) {
//            retVal = setSelectedProcess(process);
//        }
//        return retVal;
//    }

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

    public OneStripDFHProcess getDFHProcessREMOVE(Performance p) {  // TODO to be removed
        OneStripDFHProcess oneProcess = null;
        for (OneStripDFHProcess proc:list) {
            ErrorStatAndMsg reply =  proc.performanceOkForProcess(p);
            if (!reply.inError) {
                BooleanWithStatus tableStat = proc.checkPerformanceTableRange(p);
                if (tableStat.getDataStatus() == DataStat.Status.OK) {
                    oneProcess = proc;
                    break;
                }
                else
                    dfHeating.logInfo(tableStat.getInfoMessage());
            }
            else
                dfHeating.logInfo(reply.msg);
        }
        return oneProcess;
    }

    public boolean takeStripProcessListFromXML(String xmlStr) {
        list.clear();
        boolean retVal = false;
        ValAndPos vp;
        oneBlk:
        {
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
        cbProcess.setSelectedIndex(-1); // unselect
        return retVal;
    }

    public StringBuilder dataInXMl() {
        StringBuilder xmlStr = new StringBuilder(XMLmv.putTag("pNum", list.size()));
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
        OneStripDFHProcess selectedP;
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
            return selectedP;
        }

        void getSelectedProcess() {
            String pName = (String)jcbExisting.getSelectedItem();
            boolean bNew = (pName == enterNew);
            if (bNew)
                selectedP = new OneStripDFHProcess(pListManager, pName, vChMaterial, inpC);
            else
                selectedP = getDFHProcess(pName);
            detailsP.removeAll();
            editorPanel = selectedP.getEditPanel(vChMaterial, inpC, this, editable, bNew);
            detailsP.add(editorPanel);
            detailsP.updateUI();
//            pack();
        }

        public ErrorStatAndMsg checkData() {
            return selectedP.checkData();
        }

        public boolean saveData() {
            boolean itsNew = true;
            ErrorStatAndMsg dataStat = selectedP.noteDataFromUI();
            if (!dataStat.inError) {
                for (OneStripDFHProcess p : list)
                    if (p == selectedP) {
                        itsNew = false;
                        break;
                    }
                if (itsNew) {
                    list.add(selectedP);
                }
                populateJcbExisting();
                jcbExisting.setSelectedItem(selectedP.baseProcessName);
                response = EditResponse.Response.SAVE;
                edited = true;
            }
            else
                showError("Data Entry", dataStat.msg);
            return edited;
        }

        public void deleteData() {
            list.remove(selectedP);
            populateJcbExisting();
            jcbExisting.setSelectedItem(selectedP.baseProcessName);
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
