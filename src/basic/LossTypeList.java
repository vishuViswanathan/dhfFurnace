package basic;


import com.sun.corba.se.spi.ior.IdentifiableBase;
import com.sun.xml.internal.bind.v2.model.core.ID;
import directFiredHeating.DFHFurnace;
import display.*;
import mvXML.ValAndPos;
import mvXML.XMLgroupStat;
import mvXML.XMLmv;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 5/18/12
 * Time: 11:06 AM
 * To change this template use File | Settings | File Templates.
 */
public class LossTypeList  {
    public static int IDBASE = 1000;
    public static Integer INERNALRADLOSS = 100, FIXEDLOSS = 200;
    public static int MAXLOSSNUM = 25;
    LinkedHashMap<Integer, LossType> list;
    ActionListener listener;
    int lastID = IDBASE;
    InputControl controller;
    DFHFurnace furnace;

    public LossTypeList(InputControl controller, DFHFurnace furnace, ActionListener listener) {
        this.controller = controller;
        this.furnace = furnace;
        this.listener = listener;
        list = new LinkedHashMap<Integer, LossType>();
        for (int l = 0; l < MAXLOSSNUM; l++)
            add(new LossType(controller, furnace, "Loss #" + (l + 1), listener));
    }

    public void resetList() {
       int l = 0;
        for (LossType type: list.values()) {
            l++;
            type.changeData("Loss #" + l, 0, LossType.LossBasis.NONE, LossType.TempAction.NONE);
        }
    }

    public void enableDataEntry(boolean ena) {
        for (LossType type:list.values())
            type.enableDataEntry(ena);
    }


    public int add(LossType loss) {
        lastID++;
        list.put(lastID, loss);
        return lastID;
    }

    public String lossName(Integer id) {
        String name = "UNKNOWN";
        if (id.equals(INERNALRADLOSS))
            name = "Internal Radiation Loss";
        else if (id.equals(FIXEDLOSS))
            name =  "Fixed Loss";
        else  {
            if (list.containsKey(id))
                name = list.get(id).lossName;
        }
        return name;
    }

    public boolean changeLossItemVal(int lossNum, String lossName, double factor,
                                     LossType.LossBasis basis, LossType.TempAction tempAct, boolean bQuiet) {
        boolean retVal = false;
        Integer key = lossNum;
        if (list.containsKey(key)) {
            list.get(key).changeData(lossName, factor, basis, tempAct, bQuiet);
            retVal = true;
        }
        return retVal;
    }

    public void informListeners() {
        get(IDBASE + 1).informListener();
    }

    public boolean changeLossItemVal(int lossNum, String lossName, double factor,
                                     LossType.LossBasis basis, LossType.TempAction tempAct) {
        return  changeLossItemVal(lossNum, lossName, factor, basis, tempAct, false);
    }

    //    returns null if not available
    public LossType get(int id) {
        return list.get(new Integer(id));
    }

    public LossType get(Integer id) {
        return list.get(id);
    }

    public int size() {
        return list.size();
    }


    public Iterator<Integer> keysIter() {
        return list.keySet().iterator();
    }

    public void takeValuesFromUI() {
        for (LossType type:list.values())
            type.takeValuesFromUI();
//        Iterator<LossType> losses = list.values().iterator();
//        while (losses.hasNext())
//            losses.next().takeValuesFromUI();
    }
    public String dataInXML() {
        String xmlStr = "";
        Iterator<Integer> ekey = keysIter();
        Object  k;
        LossType lt;
        while (ekey.hasNext()) {
            k = ekey.next();
            lt = list.get(k);
            xmlStr += XMLmv.putTag("lt" + ("" + k).trim(), lt.dataInXML());
        }
        return xmlStr;
    }

    public boolean takeDataFromXML(String xmlStr) {
        boolean retVal = true;
        ValAndPos vp;
        Iterator<Integer> ekey = keysIter();
            Integer  k;
        LossType lt;
        Integer oneKey = null;
        boolean bFirstTime = true;
        while (ekey.hasNext()) {
            k = ekey.next();
            if (bFirstTime)
                oneKey = k;
            bFirstTime = false;
            lt = list.get(k);
            vp = XMLmv.getTag(xmlStr, "lt" + ("" + k).trim(), 0);
            if (vp.val.length() > 0)
                if (!lt.changeData(vp.val)) {
                    retVal = false;
                    break;
                }
         }
//        if (oneKey != null)
//            list.get(oneKey).informListener();
        return retVal;
    }

    static int labelHeight = 20;
    Dimension colHeadSize = new Dimension(220, labelHeight);

    VScrollSync vScrollSync;

    public JPanel activeLossRowHead(Vector<XLcellData> rowHeadXL) {
        SizedLabel lab;
        Vector <Integer> activeList = activeLossIDs();
        JPanel outerP = new JPanel(new GridBagLayout());
        GridBagConstraints outerGbc = new GridBagConstraints();
        outerGbc.gridx = 0;
        outerGbc.gridy = 0;
        outerGbc.insets = new Insets(1, 0, 1, 0);

        JPanel pan = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(1, 0, 1, 0);
        LossType lt;
        boolean shaded = false;
        for (int l = 0; l < activeList.size(); l++) {
            lt = get(activeList.get(l));
            lab = new SizedLabel(lt.lossName, colHeadSize, true, shaded);
            rowHeadXL.add(lab);
            pan.add(lab, gbc);
            gbc.gridy++;
            shaded = !shaded;
        }
        JScrollPane sp = new JScrollPane();
        sp.getVerticalScrollBar().setUnitIncrement(20);
        sp.setPreferredSize(new Dimension(colHeadSize.width + 20, 210));
        sp.setViewportView(pan);
        vScrollSync = new VScrollSync(sp);

        outerP.add(sp, outerGbc);
        return outerP;
    }

    public VScrollSync getVScrollMaster() {
        return vScrollSync;
    }

    public Vector <Integer> activeLossIDs() {
        Vector <Integer> activeList = new Vector<Integer>();
        Iterator<Integer> ekey = keysIter();
        Integer  k;
        LossType lt;
        while (ekey.hasNext()) {
            k = ekey.next();
            lt = list.get(k);
            if (lt.isValid())
                activeList.add(k);
        }
        return activeList;
    }

    void errMsg(String msg) {
        System.out.println("LossTypeList: ERROR - " + msg);
    }

}
