package mvmath;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;

/**
 * Title:        Calculation of structural Beams
 * Description:
 * Copyright:    Copyright (c) M. Viswanathan
 * Company:
 * @author M. Viswanathan
 * @version 1.0
 */

public class MultiColumnScrollList
    extends JScrollPane {
  JPanel mainP;
  JPanel headP;
  Vector items;
  GridBagConstraints headGbc;

  public MultiColumnScrollList() {
    super();
    mainP = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    setViewportView(mainP);
    headP = new JPanel(new GridBagLayout());
    headGbc = new GridBagConstraints();
    headGbc.gridx = 0;
    headGbc.gridy = 0;
    headGbc.ipadx = 4;
    headGbc.anchor = headGbc.WEST;
    headGbc.fill = headGbc.NONE;
    headGbc.weightx = 0;

    setColumnHeaderView(new JScrollPane(headP));
    items = new Vector();
  }

  public void addColumn(JList list, String colHead) {
    JPanel lp = new JPanel();
    lp.setBorder(new LineBorder(Color.darkGray));
    lp.add(list);
    mainP.add(lp);
    JPanel p = new JPanel();
    p.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    p.setBackground(Color.lightGray);
    p.add(new JLabel(colHead));
    headP.add(p, headGbc);
    headGbc.gridx++;
    items.addElement(new ListAndHead(lp, p));
  }

  public void pack() {
    int size = items.size();
    ListAndHead lh;
    int totWidth = 0;
    int colHt = 0;
    Dimension dimList, dimHead, dimPreff;
    for (int n = 0; n < size; n++) {
      lh = (ListAndHead) items.elementAt(n);
      dimList = lh.list.getPreferredSize();
      dimHead = lh.head.getPreferredSize();
      dimPreff = new Dimension(
          Math.max(dimHead.width, dimList.width),
          dimHead.height);
      lh.list.setPreferredSize(new Dimension(dimPreff.width, dimList.height));
      lh.head.setPreferredSize(dimPreff);
      totWidth += dimPreff.width;
      colHt = Math.max(colHt, dimHead.height);
      if (n == (size - 1)) {
        GridBagLayout lay = (GridBagLayout) headP.getLayout();
        headGbc.gridx--;
        headGbc.weightx = 1;
        lay.setConstraints(lh.head, headGbc);
      }
    }
    headP.setPreferredSize(new Dimension(totWidth, colHt));
  }

  class ListAndHead {
    JComponent list;
    JComponent head;

    ListAndHead(JComponent list, JComponent head) {
      this.list = list;
      this.head = head;
    }
  }
}