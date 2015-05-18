package level2;

import basic.ChMaterial;
import mvUtils.display.EditResponse;
import mvUtils.display.ErrorStatAndMsg;
import mvUtils.display.InputControl;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 15-May-15
 * Time: 3:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class StripDFHProcessList {
    L2DFHeating l2DFHeating;
    Vector<ChMaterial> vChMaterial;
    InputControl inpC;
    Vector<OneStripDFHProcess> list;

    public StripDFHProcessList(L2DFHeating l2DFHeating) {
        this.vChMaterial = l2DFHeating.vChMaterial;
        this.l2DFHeating = l2DFHeating;
        this.inpC = l2DFHeating;
        list = new Vector<OneStripDFHProcess>();
    }

    public void clear() {
        list.clear();
    }

    public boolean addStripDFHProcess() {
         boolean retVal = false;
         String processName = "DDQ";
         OneStripDFHProcess oneDFHProc = new OneStripDFHProcess(processName, vChMaterial, inpC);
         if (oneDFHProc.getEditResponse() == EditResponse.Response.MODIFIED) {
             list.add(oneDFHProc);
             retVal = true;
         }
         return retVal;
    }

    public OneStripDFHProcess get(String processName) {
        OneStripDFHProcess oneProcess = null;
        for (OneStripDFHProcess proc: list)
            if (proc.processName.equalsIgnoreCase(processName)) {
                oneProcess = proc;
                break;
            }
        return oneProcess;
    }

    boolean takeStripProcessListFromXML(String xmlStr) {
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
                    OneStripDFHProcess oneProc = new OneStripDFHProcess(l2DFHeating, vp.val);
                    if (oneProc.inError) {
                            l2DFHeating.showError("In reading StripDFHProc: \n" + oneProc.errMeg);
                        retVal = false;
                        break oneBlk;
                    }
                    else
                        list.add(oneProc);
                }
                retVal = true;
            } catch (NumberFormatException e) {
                l2DFHeating.showError("Error in Number of StripDFHProc");
                break oneBlk;
            }
        }
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
}
