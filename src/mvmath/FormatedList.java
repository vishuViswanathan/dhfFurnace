package mvmath;

import javax.swing.*;
import java.awt.*;
import java.text.*;

/**
 * Title:        Calculation of structural Beams
 * Description:
 * Copyright:    Copyright (c) M. Viswanathan
 * Company:
 * @author M. Viswanathan
 * @version 1.0
 */

public class FormatedList
    extends JList {
  static final int DOUBLEARRAY = 0;
  static final int INTEGERARRAY = 1;
  int arrayType;

  public FormatedList(double[] data, int befDecimal, int afterDecimal) {
    Double[] dData = new Double[data.length];
    for (int n = 0; n < data.length; n++) {
      dData[n] = new Double(data[n]);
    }
    this.setListData(dData);
    arrayType = DOUBLEARRAY;
    this.setCellRenderer(new Renderer(arrayType, befDecimal, afterDecimal));
    JPanel dummy = new JPanel();
    this.setBackground(dummy.getBackground());
    dummy = null;
  }

  public FormatedList(Double[] data, int befDecimal, int afterDecimal) {
    this.setListData(data);
    arrayType = DOUBLEARRAY;
    this.setCellRenderer(new Renderer(arrayType, befDecimal, afterDecimal));
  }

  public FormatedList(float[] data, int befDecimal, int afterDecimal) {
    Double[] dData = new Double[data.length];
    for (int n = 0; n < data.length; n++) {
      dData[n] = new Double(data[n]);
    }
    this.setListData(dData);
    arrayType = DOUBLEARRAY;
    this.setCellRenderer(new Renderer(arrayType, befDecimal, afterDecimal));
  }

  public FormatedList(int[] data, int digits) {
    Integer[] iData = new Integer[data.length];
    for (int n = 0; n < data.length; n++) {
      iData[n] = new Integer(data[n]);
    }
    this.setListData(iData);
    arrayType = INTEGERARRAY;
  }
} // end of class FormatedList

class Renderer
    extends JLabel
    implements ListCellRenderer {
  int objectType;
  DecimalFormat format;

  Renderer(int objectType, int befDecimal, int afterDecimal) {
    super();
    this.objectType = objectType;
    setFormat(befDecimal, afterDecimal);
  }

  void setFormat(int befDecimal, int afterDecimal) {
    if (befDecimal <= 0) {
      befDecimal = 1;
    }
    char[] charF = new char[befDecimal +
        ( (afterDecimal > 0) ? (afterDecimal + 1) : 0)];
    int n;
    for (n = 0; n < befDecimal; n++) {
      charF[n] = '0';
    }
    if (afterDecimal > 0) {
      charF[n++] = '.';
      for (; n < charF.length; n++) {
        charF[n] = '0';
      }
    }
    format = new DecimalFormat(new String(charF));
  }

  /**
   * this method of super is overridden to prevent graying ot text since the
   * list is marked as disabled to avoid user action
   */
  public Component getListCellRendererComponent(
      JList list,
      Object value, // value to display
      int index, // cell index
      boolean isSelected, // is the cell selected
      boolean cellHasFocus) { // the list and the cell have the focus
    switch (objectType) {
      case FormatedList.DOUBLEARRAY:
        setText(format.format( ( (Double) value).doubleValue()));
        break;
      case FormatedList.INTEGERARRAY:
        setText(format.format( ( (Integer) value).intValue()));
        break;
    }
    return this;
  }
} // end of class Renderer
