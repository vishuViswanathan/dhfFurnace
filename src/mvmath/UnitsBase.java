package mvmath;

/**
 * Title:        Calculation of structural Beams
 * Description:
 * Copyright:    Copyright (c) M. Viswanathan
 * Company:
 * @author M. Viswanathan
 * @version 1.0
 */

public class UnitsBase {
  public static final int UNITSSI = 1000;
  public static final int UNITMKS = 1001;
  public static final int UNITSFPS = 1002;

  public static final int UNITNAME = 0;
  public static final int LENGTH = UNITNAME + 1;
  public static final int MASS = LENGTH + 1;
  public static final int TIME = MASS + 1;
  public static final int FORCE = TIME + 1;
  public static final int MOMENT = FORCE + 1;
  public static final int PRESSURE = MOMENT + 1;

  // modify this line when more units addded
  public static final int LASTENTRY = PRESSURE + 1;
  static final UnitsSymbol[] unitsLabelSI = new UnitsSymbol[LASTENTRY];
  static {
    unitsLabelSI[UNITNAME] = new UnitsSymbol("Internatinal Units", "SI");
    unitsLabelSI[LENGTH] = new UnitsSymbol("Meter", "m");
    unitsLabelSI[MASS] = new UnitsSymbol("kilogram", "kg");
    unitsLabelSI[TIME] = new UnitsSymbol("second", "s");
    unitsLabelSI[FORCE] = new UnitsSymbol("Newton", "N");
    unitsLabelSI[MOMENT] = new UnitsSymbol("Newton.meter", "N.m");
    unitsLabelSI[PRESSURE] = new UnitsSymbol("Pascal", "p");
  }
  static final UnitsSymbol[] unitsLabelMKS = new UnitsSymbol[LASTENTRY];
  static {
    unitsLabelMKS[UNITNAME] = new UnitsSymbol("MKS Units", "MKS");
    unitsLabelMKS[LENGTH] = new UnitsSymbol("Meter", "m");
    unitsLabelMKS[MASS] = new UnitsSymbol("kilogram", "kg");
    unitsLabelMKS[TIME] = new UnitsSymbol("second", "s");
    unitsLabelMKS[FORCE] = new UnitsSymbol("kilogram", "kg");
    unitsLabelMKS[MOMENT] = new UnitsSymbol("kilogram.meter", "kg.m");
    unitsLabelMKS[PRESSURE] = new UnitsSymbol("kilogram/cm2", "kgf/cm2");
  }
  static final UnitsSymbol[] unitsLabeFPS = new UnitsSymbol[LASTENTRY];

  int base;
  public UnitsBase(int unit) {
    base = checkBase(unit);
  }

  int checkBase(int unit) {
    int u = unit;
    if (unit != UNITSSI && unit != UNITMKS || unit != UNITSFPS)
      u = UNITSSI;
    return u;
  }
}

class UnitsSymbol {
  String name;
  String symbol;

  UnitsSymbol(String name, String symbol){
    this.name = name;
    this.symbol = symbol;
  }

  String getSymbol() {
    return symbol;
  }

  String getName() {
    return name;
      Trends
  }
}