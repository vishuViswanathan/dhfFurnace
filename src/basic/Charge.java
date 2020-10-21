package basic;

import directFiredHeating.DFHeating;
import mvUtils.display.SimpleDialog;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/19/12
 * Time: 1:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class Charge {
    public enum ChType {
        SOLID_RECTANGLE("Rectangular solid"),
        SOLID_CIRCLE("Circular solid"),
        TUBULAR("Tubular");

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
    public double wallThickness;
    public ChMaterial chMaterial;
    public double unitWt;  // in kg
    public double projectedTopArea;
    public double effectiveThickness;
    public ChType type = ChType.SOLID_RECTANGLE;

    public Charge(ChMaterial chMaterial, double length,
                  double width, double height, double diameter,
                  double wallThickness, ChType type) {
        setData(chMaterial, length, width, height, diameter, wallThickness, type);
    }

    public Charge(ChMaterial chMaterial, double length,
                  double width, double height) {
        this(chMaterial, length, width, height, 0.2, 0, ChType.SOLID_RECTANGLE);

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
                      double width, double height, double diameter,
                         double wallThickness, ChType type) {
        this.chMaterial = chMaterial;
        this.type = type;
        setSize(length, width, height, diameter, wallThickness);
    }

    public Charge(Charge fromCharge) {
        copyFrom(fromCharge);
    }

    public void copyFrom(Charge fromCharge) {
        Charge fC = fromCharge;
        setData(fC.chMaterial, fC.length, fC.width, fC.height, fC.diameter, fC.wallThickness, fC.type);
    }

    public void setSize(double length,
                          double width, double height)  {
        setSize(length, width, height, 0.2, 0);
    }

    public void setSize(double length, double width, double height,
                        double diameter, double wallThickness)  {
        this.width = width;
        this.height = height;
        this.length = length;
        this.diameter = diameter;
        this.wallThickness = wallThickness;
        evalProjAreaUnitWtAndEffThick();
    }

//    void evalChUnitWt() {
//        unitWt = getWeight(length);
//    }

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

    void getProjectedTopArea() {
        double pArea = 0;
        switch(type) {
            case SOLID_RECTANGLE:
                pArea = width * length;
                break;
            case SOLID_CIRCLE:
                pArea = diameter * length;
                break;
            case TUBULAR:
                pArea = diameter * width;
                break;
        }
        projectedTopArea = pArea;
    }

    public double getDiameter() { return diameter;}

    public ChMaterial getElementType() {
        return chMaterial;
    }

    public double getEmiss(double temp) {
        return chMaterial.getEmiss(temp);
    }

    void evalProjAreaUnitWtAndEffThick() {
        double wt;
        double id;
        double len = length;
        double crossArea;
        switch (type) {
            case SOLID_CIRCLE:
                crossArea = Math.PI / 4 * diameter * diameter;
                break;
            case TUBULAR:
                id = diameter - 2 * wallThickness;
                crossArea = Math.PI / 4 * (diameter * diameter - id * id);
                break;
            case SOLID_RECTANGLE:
                crossArea = width * height;
                break;
            default:
               showError("In Charge","Unknown type in Charge");
                crossArea = 0;
                break;

        }
        unitWt = len * crossArea* chMaterial.getDensity();
        getProjectedTopArea();
        effectiveThickness = crossArea * len / projectedTopArea;
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

    public void showError(String title, String msg){
        SimpleDialog.showError(title, msg);
    }
}
