package basic;

import display.MultiPairColPanel;
import display.NumberLabel;
import display.VScrollSync;
import display.XLcellData;
import mvmath.DoubleMV;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 8/28/12
 * Time: 1:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class LossListWithVal {
    LinkedHashMap<Integer, DoubleMV> list;
    LossTypeList lossTypeList = null;
    public LossListWithVal(LossTypeList lossTypeList) {
        list = new LinkedHashMap<Integer, DoubleMV>();
        this.lossTypeList = lossTypeList;
    }

    public LossListWithVal() {
        this(null);
    }

    public void add(Integer id, double val) {
        if (list.containsKey(id))
            list.get(id).addVal(val);
        else
            list.put(id, new DoubleMV(val));
    }

    public void add(LossTypeAndVal typeAndVal) {
        add(typeAndVal.lossID,  typeAndVal.loss);
    }

    public double val(Integer id) {
        double retVal = 0;
        if (list.containsKey(id))
            retVal = list.get(id).val();
        return retVal;
    }

    public Iterator<Integer> keyIter() {
        return list.keySet().iterator();
    }

    public double getTotal() {
        double retVal = 0;
        Iterator<Integer> iter = list.keySet().iterator();
        while (iter.hasNext()) {
            retVal += list.get(iter.next()).val();
        }
        return retVal;
    }

    public LossListWithVal addToList(LossListWithVal toList) {
        Iterator<Integer> iter = list.keySet().iterator();
        Integer id;
        double value;
         while (iter.hasNext()) {
             id = iter.next();
             value = list.get(id).val();
             if (value != 0)
                toList.add(id, value);
          }
         return toList;
    }

    public MultiPairColPanel addToMulipairPanel(MultiPairColPanel jp) {
        if (lossTypeList != null) {
            String datFormat = "#,###";
            Iterator<Integer> iter = getSortedKeyList(); //list.keySet().iterator();
            Integer id;
            double value;
            String name;
            while (iter.hasNext()) {
                 id = iter.next();
                 name = lossTypeList.lossName(id);
                value = list.get(id).val();
                jp.addItemPair(name, value, datFormat);
             }
        }
        return jp;
    }

    Iterator<Integer> getSortedKeyList() {
         return new KeySort();
    }

    public JPanel lossValuesPanel(VScrollSync master, Vector <XLcellData> lossValsXL) {
//        lossValsXL = new Vector<XLcellData>();
        Vector <Integer> activeList = lossTypeList.activeLossIDs();
        JPanel pan = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(1, 0, 1, 0);
        DoubleMV loss;
        double val;
        NumberLabel lab;
        boolean shaded = false;
        for (int l = 0; l < activeList.size(); l++) {
            loss = list.get(activeList.get(l));
            if (loss != null)
                val = loss.val();
            else
                val = 0;
            lab = new NumberLabel(val,  70, "#,##0");
            if (shaded) {
                lab.setBackground(Color.PINK);
                lab.setOpaque(true);
            }
            lossValsXL.add(lab);
            pan.add(lab, gbc);
            gbc.gridy++;
            shaded = !shaded;
        }
        JScrollPane sp = new JScrollPane();
        sp.setPreferredSize(new Dimension(80, 210));
        sp.setViewportView(pan);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        master.add(sp);
        JPanel outerP = new JPanel(new GridBagLayout());
        GridBagConstraints outerGbc = new GridBagConstraints();
        outerGbc.gridx = 0;
        outerGbc.gridy = 0;
        outerP.add(sp, gbc);
        return outerP;
    }


    class KeySort implements Iterator<Integer> {
        ArrayList<Integer> keyL;
        int now = 0;
        KeySort() {
           Iterator<Integer> keyIter = list.keySet().iterator();
           keyL = new ArrayList<Integer>();
           while (keyIter.hasNext())
               keyL.add(keyIter.next());
           Collections.sort(keyL);
        }

        public boolean hasNext() {
            if (now < keyL.size())
                return(keyL.get(now) != null);
            else
                return false;
        }

         public Integer next() {
             now++;
             return keyL.get(now - 1);
         }

         public void remove() {
             //To change body of implemented methods use File | Settings | File Templates.
         }
    }
}
