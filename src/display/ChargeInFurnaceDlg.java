package display;

import java.util.*;
import javax.vecmath.*;

import javax.swing.*;

import basic.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class ChargeInFurnaceDlg extends JDialog {
  double chargeGap;
  ThreeDCharge ch;
  Vector amb;
  Point2d[] allPoints;
  Point2d[] validPoints;

  public ChargeInFurnaceDlg(ThreeDCharge charge, Vector ambients) {
  }

  double getChageGap() {
    return chargeGap;
  }

}