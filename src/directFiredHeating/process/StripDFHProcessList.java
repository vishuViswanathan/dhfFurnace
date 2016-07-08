package directFiredHeating.process;

import basic.ChMaterial;
import directFiredHeating.DFHeating;
import mvUtils.display.*;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

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
    Vector<OneStripDFHProcess> list;
    StripDFHProcessList me;
    JComboBox<OneStripDFHProcess> cbProcess;

    public StripDFHProcessList(DFHeating dfHeating) {
        this.vChMaterial = dfHeating.vChMaterial;
        this.dfHeating = dfHeating;
        this.inpC = dfHeating;
        list = new Vector<OneStripDFHProcess>();
        cbProcess = new JComboBox<OneStripDFHProcess>(list);
        cbProcess.setPreferredSize(new Dimension(300, 20));
    }

    public String getSelectedProcessName() {
        String retVal = "";
        OneStripDFHProcess p = (OneStripDFHProcess)cbProcess.getSelectedItem();
        if (p != null)
            retVal = p.processName;
        return retVal;
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

    public OneStripDFHProcess getDFHProcess(String processName) {
        OneStripDFHProcess oneProcess = null;
        for (OneStripDFHProcess proc: list) {
//            l2DFHeating.logInfo(String.format("Looking for %s and got %s", processName, proc.processName));
            if (proc.processName.equalsIgnoreCase(processName)) {
                oneProcess = proc;
                break;
            }
        }
        return oneProcess;
    }

    public boolean takeStripProcessListFromXML(String xmlStr) {
        list.clear();
        boolean retVal = false;
        ValAndPos vp;
        oneBlk:
        {
//            vp = XMLmv.getTag(xmlStr, "exitTempAllowance", 0);
//            if (vp.val.length() > 0 )
//                l2Furnace.setExitTempAllowance(Double.valueOf(vp.val));
//            else
//                l2Furnace.setExitTempAllowance(5);
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
                    else
                        list.add(oneProc);
                }
                retVal = true;
            } catch (NumberFormatException e) {
                dfHeating.showError("Error in Number of StripDFHProc");
                break oneBlk;
            }
        }
        cbProcess.updateUI();
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
            if (proc.processName.equalsIgnoreCase(oneProcess.processName)) {
                newOne = false;
                break;
            }
        }
        if (newOne)
            list.add(oneProcess);
        return newOne;
    }

    public boolean replaceOneDFHProcess(OneStripDFHProcess oneProcess)  {
        deleteDFHProcess(oneProcess.processName);
        list.add(oneProcess);
        return true;
    }

    public boolean deleteDFHProcess(String processName) {
        boolean retVal = false;
        processName = processName.toUpperCase();
        for (OneStripDFHProcess proc: list)  {
            if (proc.processName.equals(processName)) {
                list.remove(proc);
                retVal = true;
                break;
            }
        }
        return retVal;
    }

    ErrorStatAndMsg checkDuplication(OneStripDFHProcess oneProcess, String processName) {
        ErrorStatAndMsg status = new ErrorStatAndMsg(false, "ERROR: ");
        if (processName.length() < 2 || processName.substring(0, 1).equals(".")) {
            status.inError = true;
            status.msg += "Enter proper process Name";
        }
        else {
            for (OneStripDFHProcess p : list)
                if ((p != oneProcess) && (p.processName.equalsIgnoreCase(processName))) {
                    status.inError = true;
                    status.msg += "This Process " + processName + " already Exists";
                    break;
                }
        }
        return status;
    }

    public JComponent getListUI() {
        return cbProcess;
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
//            for (OneStripDFHProcess p: list)
//                jcbExisting.addItem(p.processName);
//            jcbExisting.addItem(enterNew);
            jcbExisting.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (!bListBeingChanged)
                        getSelectedProcess();
                }
            });
            outerP.add(jcbExisting, BorderLayout.NORTH);
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
                jcbExisting.addItem(p.processName);
            if (editable)
                jcbExisting.addItem(enterNew);
            bListBeingChanged = false;
        }

        void setSelectedP(OneStripDFHProcess selectedP) {
            jcbExisting.setSelectedItem(selectedP.processName);
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
            selectedP.noteDataFromUI();
            for (OneStripDFHProcess p:list)
                if (p == selectedP) {
                    itsNew = false;
                    break;
                }
            if (itsNew) {
                list.add(selectedP);
                populateJcbExisting();
                jcbExisting.setSelectedItem(selectedP.processName);
                response = EditResponse.Response.SAVE;
            }
            edited = true;
            return edited;
        }

        public void deleteData() {
            list.remove(selectedP);
            populateJcbExisting();
            jcbExisting.setSelectedItem(selectedP.processName);
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
