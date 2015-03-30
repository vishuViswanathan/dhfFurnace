package basic;

import java.io.*;

public class ChargeDef extends Object implements Serializable {
    double width, height, length;
    public double flangeT, webT;  // for Beam blanks
    public int chargeType = 0; // one of the following
    public static final int RECTANGULAR = 0, CIRCULAR = 1, BEAMBLANK_H = 2,
            BEAMBLANK_V = 3; // BEAMBLANK_H is with Web Horzontal
    static final String typeName[] = {"Rectangular", "Circular", "Beam Blank - H.Web", "Beam Blank - V.Web"};
    ElementType element;

    /**
     * The default chage type is taken as Rectangular cross-section
     *
     * @param element
     * @param length
     * @param width
     * @param height
     */
    public ChargeDef(ElementType element, double length,
                     double width, double height) {
        // R2 was width, height, length
        chargeType = RECTANGULAR;
        this.element = element;
        this.width = width;
        this.height = height;
        this.length = length;
    }

    public ChargeDef(int type, ElementType element, double length, double width,
                     double height, double flangeThick, double webThick) {
        if (checkTypeValid(type)) {
            chargeType = type;
            this.element = element;
            this.width = width;
            this.height = height;
            this.length = length;
            flangeT = flangeThick;
            webT = webThick;
        }
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

    public ElementType getElementType() {
        return element;
    }

    public ThreeDCharge createThreeDCharge(double unitSize) {
        return new ThreeDCharge(this, unitSize);
//              length, width, height, unitSize);
    }

    public static boolean checkTypeValid(int type) {
        if (type == RECTANGULAR || /*type == CIRCULAR || */ type == BEAMBLANK_H ||
                type == BEAMBLANK_V)
            return true;
        else
            return false;
    }

    public static String chargeTypeName(int type) {
        if (type >= 0 && type < typeName.length)
            return typeName[type];
        else
            return "UNKNOWN";
    }

//  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//    debug("Trying to save Chargedef");
//  }
//

    public String toString() {
        return "Material: " + element +
                "\nSize: " + width + "m wide, " +
                height + "m high, " +
                length + "m long";
    }

    void debug(String msg) {
        System.out.println("ChargeDef: " + msg);
    }

}

