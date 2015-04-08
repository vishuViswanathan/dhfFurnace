package basic;

import directFiredHeating.DFHeating;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/19/12
 * Time: 1:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class Charge {
    static public enum ChType {
        SOLID_RECTANGLE("Rectangular solid"),
        SOLID_CIRCLE("Circular solid");

        private final String chTypeName;

        ChType(String chTypeName) {
            this.chTypeName = chTypeName;
        }

        public String getValue() {
            return name();
        }

        @Override
        public String toString() {
            return chTypeName;
        }

        public static ChType getEnum(String text) {
            ChType retVal = null;
            if (text != null) {
              for (ChType b : ChType.values()) {
                if (text.equalsIgnoreCase(b.chTypeName)) {
                  retVal = b;
                    break;
                }
              }
            }
            return retVal;
          }
    }

    public double width, height, length;
    public double diameter;
    public ChMaterial chMaterial;
    public double unitWt;  // in kg
    public ChType type = ChType.SOLID_RECTANGLE;

    public Charge(ChMaterial chMaterial, double length,
                  double width, double height, double diameter, ChType type) {
//        this.chMaterial = chMaterial;
        setData(chMaterial, length, width, height, diameter, type);
//        this.chMaterial = chMaterial;
//        this.width = width;
//        this.height = height;
//        this.length = length;
//        evalChUnitWt();
    }

    public Charge(ChMaterial chMaterial, double length,
                  double width, double height) {
        this(chMaterial, length, width, height, 0.2, ChType.SOLID_RECTANGLE);

    }

    public Charge(DFHeating dfHeating, String xmlStr) throws Exception {
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "material", 0);
        chMaterial = dfHeating.getSelChMaterial(vp.val);
        vp = XMLmv.getTag(xmlStr, "width", 0);
        width = Double.valueOf(vp.val);
        vp = XMLmv.getTag(xmlStr, "height", 0);
        height = Double.valueOf(vp.val);
        vp = XMLmv.getTag(xmlStr, "length", 0);
        length = Double.valueOf(vp.val);
        vp = XMLmv.getTag(xmlStr, "diameter", 0);
        diameter = Double.valueOf(vp.val);
        vp = XMLmv.getTag(xmlStr, "type", 0);
        type = ChType.getEnum(vp.val);
    }


    private void setData(ChMaterial chMaterial, double length,
                      double width, double height, double diameter, ChType type) {
        this.chMaterial = chMaterial;
        this.type = type;
        setSize(length, width, height, diameter);
    }

    public Charge(Charge fromCharge) {
        copyFrom(fromCharge);
    }

    public void copyFrom(Charge fromCharge) {
        Charge fC = fromCharge;
        setData(fC.chMaterial, fC.length, fC.width, fC.height, fC.diameter, fC.type);
    }

    public void setSize(double length,
                          double width, double height)  {
        setSize(length, width, height, 0.2);
//        this.width = width;
//        this.height = height;
//        this.length = length;
//        if (type == ChType.CYLINDRICAL)
//            this.diameter = 0.2;
//        evalChUnitWt();
    }

    public void setSize(double length,
                          double width, double height, double diameter)  {
        this.width = width;
        this.height = height;
        this.length = length;
        this.diameter = diameter;
        evalChUnitWt();
    }

    void evalChUnitWt() {
        unitWt = getWeight(length);
        // was     unitWt = length * width * height * chMaterial.getDensity(); before 20140611
    }

    public double getUnitWt() {
        return unitWt;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getLength() {
        return length;
    }

    public double getDiameter() { return diameter;}

    public ChMaterial getElementType() {
        return chMaterial;
    }

    public double getEmiss(double temp) {
        return chMaterial.getEmiss(temp);
    }

    public double getWeight(double len) {
        double wt;
        switch (type) {
            case SOLID_CIRCLE:
                wt = len * (Math.PI / 4 * diameter * diameter)* chMaterial.getDensity();
                break;
            default:  // RECTANGULAR
                wt = len * width * height * chMaterial.getDensity();
                break;
        }

        return wt; // was before 20140611 chMaterial.getDensity() * width * height * len;
    }

    public double getHeatFromTemp(double temp) {
        return chMaterial.getHeatFromTemp(temp);
    }

    public double getDeltaHeat(double tFrom, double tTo) {
        return chMaterial.getDeltaHeat(tFrom, tTo);
    }

    public double getTempFromHeat(double heat) {
        return chMaterial.getTempFromHeat(heat);
    }

    /*
        public double width, height, length;
    public double diameter;
    public ChMaterial chMaterial;
    public double unitWt;  // in kg
    public ChType type = ChType.SOLID_RECTANGLE;

     */

    public StringBuilder dataInXML() {
        StringBuilder xmlStr = new StringBuilder(XMLmv.putTag("material", chMaterial.toString()));
        xmlStr.append(XMLmv.putTag("width", width));
        xmlStr.append(XMLmv.putTag("height", height));
        xmlStr.append(XMLmv.putTag("length", length));
        xmlStr.append(XMLmv.putTag("diameter", diameter));
        xmlStr.append(XMLmv.putTag("type", type.toString()));
        return xmlStr;
     }

    public String toString() {
        return "Material: " + chMaterial +
                "\nSize: " + width + "m wide, " +
                height + "m high, " +
                length + "m long";
    }

    void debug(String msg) {
        System.out.println("ChargeDef: " + msg);
    }
}
