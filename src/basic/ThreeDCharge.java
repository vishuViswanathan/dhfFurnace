package basic;

import java.io.*;
import java.util.*;

import basic.*;

import javax.vecmath.*;
import java.math.*;

public class ThreeDCharge extends ThreeDArray implements Serializable {

    public final static int WIDTHMINFACE = 1;
    public final static int WIDTHMAXFACE = 2;
    public final static int HEIGHTMINFACE = 4;
    public final static int HEIGHTMAXFACE = 8;
    public final static int LENGTHMINFACE = 16;
    public final static int LENGTHMAXFACE = 32;
    public ChargeDef chargeDef;
    ElementType type;

    double unitSide, halfUnitSide;
    public double width, height, length;
    double adjustedWidth, adjustedHeight, adjustedLength;
    double adjustedFlangeT, adjustedWebT;
    int xSize, ySize, zSize;
    public int flangeTint, webTint;
    public int webMinCell, webMaxCell;

    OuterSurface[] outerSurfaces;
    Hashtable<Integer, Double>topSurfaces, botSurfaces; // table of surfaces which receive heat
    // frop top or bottom


    // the corner co-ordinates defining he flanges and web for Beam blanks
    // the cell is part of the border cell
    int cornerY1, cornerY2;
    int cornerZ1, cornerZ2;

    /*
      7                  11
    ------             ------
    |     | 8        10|     |
    |     |            |     |
    | 13  |     9      | 15  |
    |(16)  ------------ (18) |            Looking from -x towards + x
    |        14 (17)         |            Up is +y and z ??
  6 |      ------------      | 12         13 and 15 are near surfaces and those
    |     |     3      |     |            within () are far surfaces
    |     |            |     |
    |     | 4        2 |     |
     ------             ------
       5                  1

    The 12 log surfaces for the BEAMBLANK_H, addtionally surface 21 and 22 are
    the ends.
    Similar for BEAMBLANK_V, again starting from right botom corner.

    For RECTANGULAR, there will 1 to 4 plus 21 and 22 for the ends

    */
    private ThreeDCharge() {
        super(); // create sizeless array
    }

//  public ThreeDCharge(ElementType element,
//                double length, double width, double height,
//                double unitSide) {
//    this(); // create sizeless array
//    this.length = length;
//    this.width = width;
//    this.height = height;
//      init(element,unitSide);
//  }

    public ThreeDCharge(double unitSide, double[][][] data) {
        this();
        this.unitSide = unitSide;
        xSize = data.length;
        ySize = data[0].length;
        zSize = data[0][0].length;
        setSize(xSize, ySize, zSize);
        OneNode aNode;
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    aNode = new OneNode(null, unitSide, unitSide, unitSide);
                    aNode.setTemperature(data[x][y][z]);
                    this.add(aNode, x, y, z);
                }
            }
        }
    }

    public ThreeDCharge(ChargeDef def, double unitSide) {
        this(); // create sizeless array
        chargeDef = def;
        this.length = def.getLength();
        this.width = def.getWidth();
        this.height = def.getHeight();
        init(def.getElementType(), unitSide);
    }

//  public ThreeDCharge(ChargeDef def, int divisions) {
//      width = def.getWidth();
//      height = def.getHeight();
//      length = def.getLength();
//      double minimumSide = Math.min(Math.min(width, height), length);
//
//      double unitw = width / divisions;
//      double unith = height / divisions;
//      double unitl = length / divisions;
//
//      double minunit = Math.min(Math.min(unitw, unith), unitl);
//      if (minunit < minimumSide) minunit = minimumSide / 5;
//    init(def.getElementType(), minunit);
//  }

    void init(ElementType element, double unitSide) {
        type = element;
        this.unitSide = unitSide;
        halfUnitSide = unitSide / 2;
        // evaluate size adn set it
        xSize = (int) Math.round(length / unitSide);
        adjustedLength = unitSide * xSize;
        // size increased to include border conditions
        ySize = (int) Math.round(width / unitSide);
        adjustedWidth = unitSide * ySize;
        zSize = (int) Math.round(height / unitSide);
        adjustedHeight = unitSide * zSize;
        // size increased to include border conditions
        xSize += 2;
        ySize += 2;
        zSize += 2;
        setSize(xSize, ySize, zSize);

        if (chargeDef.chargeType == ChargeDef.BEAMBLANK_H) {
            flangeTint = (int) Math.round(chargeDef.flangeT / unitSide);
            webTint = (int) Math.round(chargeDef.webT / unitSide);
            adjustedFlangeT = flangeTint * unitSide;
            adjustedWebT = webTint * unitSide;
            cornerY1 = flangeTint + 1;
            cornerY2 = (ySize - 2) - flangeTint;
            cornerZ1 = ((zSize - 2) - webTint) / 2;
            webMinCell = cornerZ1 + 1;
            cornerZ2 = cornerZ1 + webTint + 1;
            webMaxCell = cornerZ2 - 1;
        } else if (chargeDef.chargeType == ChargeDef.BEAMBLANK_V) {
            flangeTint = (int) Math.round(chargeDef.flangeT / unitSide);
            webTint = (int) Math.round(chargeDef.webT / unitSide);
            adjustedFlangeT = flangeTint * unitSide;
            adjustedWebT = webTint * unitSide;
            cornerZ1 = flangeTint + 1;
            cornerZ2 = (zSize - 2) - flangeTint;
            cornerY1 = ((ySize - 2) - webTint) / 2;
            webMinCell = cornerY1 + 1;
            cornerY2 = cornerY1 + webTint + 1;
            webMaxCell = cornerY2 - 1;
        }

        // set body nodes
        OneNode aNode;
        // the body
        for (int x = 1; x < xSize - 1; x++) {
            for (int y = 1; y < ySize - 1; y++) {
                for (int z = 1; z < zSize - 1; z++) {
                    boolean partOfCharge = true;
                    // for beam blanks mark part cells as insulated
                    if (chargeDef.chargeType == ChargeDef.BEAMBLANK_H) {
                        partOfCharge = (y < cornerY1) || (y > cornerY2) ||
                                (z > cornerZ1 && z < cornerZ2);
                    } else if (chargeDef.chargeType == ChargeDef.BEAMBLANK_V) {
                        partOfCharge = (z < cornerZ1) || (z > cornerZ2) ||
                                (y > cornerY1 && y < cornerY2);
                    }
                    if (partOfCharge)
                        aNode = new OneNode(element, unitSide, unitSide, unitSide);
                    else
                        aNode = new OneNode(null, unitSide, unitSide, unitSide);
                    this.add(aNode, x, y, z);
                }
            }
        }

        // for beam blanks mark part cells as insulated

        // by default insulate the boundary
        insulateFace(WIDTHMINFACE);
        insulateFace(WIDTHMAXFACE);
        insulateFace(HEIGHTMINFACE);
        insulateFace(HEIGHTMAXFACE);
        insulateFace(LENGTHMINFACE);
        insulateFace(LENGTHMAXFACE);
        setBoundaries();
//    prepareHeatSrcTable();             Rev on 20091219
    }


    void prepareHeatSrcTable() {
        topSurfaces = new Hashtable<Integer, Double>();
        botSurfaces = new Hashtable<Integer, Double>();
        switch (chargeDef.chargeType) {
            case ChargeDef.RECTANGULAR:
                topSurfaces.put(new Integer(3), new Double(-1));
                topSurfaces.put(new Integer(2), new Double(0.5));
                topSurfaces.put(new Integer(4), new Double(-0.5));
                topSurfaces.put(new Integer(21), new Double(0.5));
                topSurfaces.put(new Integer(22), new Double(-0.5));

                botSurfaces.put(new Integer(1), new Double(1));
                botSurfaces.put(new Integer(2), new Double(0.5));
                botSurfaces.put(new Integer(4), new Double(-0.5));
                botSurfaces.put(new Integer(21), new Double(0.5));
                botSurfaces.put(new Integer(22), new Double(-0.5));
                break;
            case ChargeDef.BEAMBLANK_H:
                errMessage("Not ready for BBH for heatsrc");
                break;
            case ChargeDef.BEAMBLANK_V:
                errMessage("Not ready for BBV for heatsrc");
                break;
        }
    }


    void noteOuterSurfaces(OuterSurface[] surfaceArray) {
        outerSurfaces = surfaceArray;
    }

    OuterSurface getSpecificSurface(int n) {
        return outerSurfaces[n];
    }

    public void setBodyTemperature(double temperature) {
        for (int x = 1; x < xSize - 1; x++) {
            for (int y = 1; y < ySize - 1; y++) {
                for (int z = 1; z < zSize - 1; z++) {
                    boolean partOfCharge = true;
                    if (chargeDef.chargeType == ChargeDef.BEAMBLANK_H) {
                        partOfCharge = (y < cornerY1) || (y > cornerY2) ||
                                (z > cornerZ1 && z < cornerZ2);
                    } else if (chargeDef.chargeType == ChargeDef.BEAMBLANK_V) {
                        partOfCharge = (z < cornerZ1) || (z > cornerZ2) ||
                                (y > cornerY1 && y < cornerY2);
                    }
                    if (partOfCharge)
                        setTemperatureAt(temperature, x, y, z);
                }
            }
        }
    }

    public void insulateFace(int theFace) {
        setSurfaceElement(theFace, null);
    }

    public void setSurfaceElement(int theFace, ElementType element) {
        OneNode aNode;
        switch (theFace) {
            case WIDTHMINFACE: // y = 0 face
                for (int x = 0; x < xSize; x++) {
                    for (int z = 0; z < zSize; z++) {
                        aNode = new OneNode(element, unitSide, unitSide, unitSide);
                        this.add(aNode, x, 0, z);
                    }
                }
                break;
            case WIDTHMAXFACE: // y = ySize - 1 face
                for (int x = 0; x < xSize; x++) {
                    for (int z = 0; z < zSize; z++) {
                        aNode = new OneNode(element, unitSide, unitSide, unitSide);
                        this.add(aNode, x, ySize - 1, z);
                    }
                }
                break;
            case HEIGHTMINFACE: // z = 0 face
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        aNode = new OneNode(element, unitSide, unitSide, unitSide);
                        this.add(aNode, x, y, 0);
                    }
                }
                break;
            case HEIGHTMAXFACE: // z = zSize - 1 face
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        aNode = new OneNode(element, unitSide, unitSide, unitSide);
                        this.add(aNode, x, y, zSize - 1);
                    }
                }
                break;
            case LENGTHMINFACE: // x = 0 face
                for (int y = 0; y < ySize; y++) {
                    for (int z = 0; z < zSize; z++) {
                        aNode = new OneNode(element, unitSide, unitSide, unitSide);
                        this.add(aNode, 0, y, z);
                    }
                }
                break;
            case LENGTHMAXFACE: // x = xSize - 1 face
                for (int y = 0; y < ySize; y++) {
                    for (int z = 0; z < zSize; z++) {
                        aNode = new OneNode(element, unitSide, unitSide, unitSide);
                        this.add(aNode, xSize - 1, y, z);
                    }
                }
                break;
        }
    }

    OneNode[] getSurfaceNodes(int surface) {
        return getSurfaceNodes(surface, 1, xSize - 2);
//    return getSurfaceNodes(surface, 0, xSize -1);  20080604
    }

    int cellXlocInCharge(double pos) {
        int x = Math.round((float) (pos / unitSide)) + 1;
        if (x < 1)
            x = 1;
        else if (x > xSize - 2)
            x = xSize - 2;
        return x;
    }

    OneNode[] getSurfaceNodes(int surface, double start, double end) {
        int startX = Math.round((float) (start / unitSide)) + 1; //(int)(start / unitSide) + 1;
        int lastX = Math.round((float) (end / unitSide)) + 1; //(int)(end / unitSide); // + 1;
        if (startX < 1)
            startX = 1;
        if (lastX > (xSize - 2))
            lastX = xSize - 2;
//    if (start == 0)  20080604
//      startX = 0;

// TEMP
        debug("start = " + start + " > startX = " + startX + ",   end = " + end + " > lastX = " + lastX);
        return (getSurfaceNodes(surface, startX, lastX));
    }

    OneNode[] getSurfaceNodes(int surface, int first, int last) {
        OneNode[] nodesArr = null;
        Vector nodesV = null;
        int chType = chargeDef.chargeType;
        switch (chType) {
            case ChargeDef.RECTANGULAR:
                nodesV = getRECTSurfaceNodes(surface, first, last);
                break;
            case ChargeDef.BEAMBLANK_H:
                nodesV = getBBHSurfaceNodes(surface, first, last);
                break;
            case ChargeDef.BEAMBLANK_V:
                debug("Not ready for BBV in getSurfaceNodes");
                break;
        }
//    if (chType == ChargeDef.RECTANGULAR)
//      nodesV = getRECTSurfaceNodes(surface, first, last);
//      else
        nodesArr = (OneNode[]) nodesV.toArray(new OneNode[0]);
        return nodesArr;
    }

    Vector getRECTSurfaceNodes(int surface, int first, int last) {
        Vector<OneNode> nodesV = new Vector<OneNode>();
        // 20080604 excluded the end nodes in all directions
        switch (surface) {
            case 1: // bottom surface
                for (int x = first; x <= last; x++) {
                    for (int y = 1; y < ySize - 1; y++) {
                        nodesV.add(super.nodes[x][y][0]);
                    }
                }
                break;
            case 2: // vertical left
                for (int x = first; x <= last; x++) {
                    for (int z = 1; z < zSize - 1; z++) {
                        nodesV.add(super.nodes[x][ySize - 1][z]);
                    }
                }
                break;
            case 3: // Top  surface
                for (int x = first; x <= last; x++) {
                    for (int y = 1; y < ySize - 1; y++) {
                        nodesV.add(super.nodes[x][y][zSize - 1]);
                    }
                }
                break;
            case 4: // vertical right
                for (int x = first; x <= last; x++) {
                    for (int z = 1; z < zSize - 1; z++) {
                        nodesV.add(super.nodes[x][0][z]);
                    }
                }
                break;
            case 21: // near-end surface no chaice of first and last
                for (int y = 1; y < ySize - 1; y++) {
                    for (int z = 1; z < zSize - 1; z++) {
                        nodesV.add(super.nodes[0][y][z]);
                    }
                }
                break;
            case 22: // far-end surface no chaice of first and last
                for (int y = 1; y < ySize - 1; y++) {
                    for (int z = 1; z < zSize - 1; z++) {
                        nodesV.add(super.nodes[xSize - 1][y][z]);
                    }
                }
                break;
        }
        return nodesV;

    }

    Vector getBBHSurfaceNodes(int surface, int first, int last) {
        Vector<OneNode> nodesV = new Vector<OneNode>();
        switch (surface) {
            case 1: // flange edge bottom-right
                for (int x = first; x <= last; x++) {
                    for (int y = 1; y < cornerY1; y++) {
                        nodesV.add(super.nodes[x][y][0]);
                    }
                }
                break;
            case 2: // bottom slot vertical right
                for (int x = first; x <= last; x++) {
                    for (int z = 1; z <= cornerZ1; z++) {
                        nodesV.add(super.nodes[x][cornerY1][z]);
                    }
                }
                break;
            case 3: // bottom slot horizontal
                for (int x = first; x <= last; x++) {
                    for (int y = cornerY1 + 1; y < cornerY2; y++) {
                        nodesV.add(super.nodes[x][y][cornerZ1]);
                    }
                }
                break;
            case 4: // bottom slot vertical left
                for (int x = first; x <= last; x++) {
                    for (int z = 1; z <= cornerZ1; z++) {
                        nodesV.add(super.nodes[x][cornerY2][z]);
                    }
                }
                break;
            case 5: // flange edge bottom-left
                for (int x = first; x <= last; x++) {
                    for (int y = cornerY2 + 1; y < ySize - 1; y++) {
                        nodesV.add(super.nodes[x][y][0]);
                    }
                }
                break;
            case 6: // vertical left
                for (int x = first; x <= last; x++) {
                    for (int z = 1; z < zSize - 1; z++) {
                        nodesV.add(super.nodes[x][ySize - 1][z]);
                    }
                }
                break;
            case 7: // flange edge top-left
                for (int x = first; x <= last; x++) {
                    for (int y = cornerY2 + 1; y < ySize - 1; y++) {
                        nodesV.add(super.nodes[x][y][zSize - 1]);
                    }
                }
                break;
            case 8: // top slot vertical left
                for (int x = first; x <= last; x++) {
                    for (int z = cornerZ2; z < zSize - 1; z++) {
                        nodesV.add(super.nodes[x][cornerY2][z]);
                    }
                }
                break;
            case 9: // top slot horizontal
                for (int x = first; x <= last; x++) {
                    for (int y = cornerY1 + 1; y < cornerY2; y++) {
                        nodesV.add(super.nodes[x][y][cornerZ2]);
                    }
                }
                break;
            case 10: // top slot vertical right
                for (int x = first; x <= last; x++) {
                    for (int z = cornerZ2; z < zSize - 1; z++) {
                        nodesV.add(super.nodes[x][cornerY1][z]);
                    }
                }
                break;
            case 11: // flange edge top-right
                for (int x = first; x <= last; x++) {
                    for (int y = 1; y < cornerY1; y++) {
                        nodesV.add(super.nodes[x][y][zSize - 1]);
                    }
                }
                break;
            case 12: // vertical right
                for (int x = first; x <= last; x++) {
                    for (int z = 1; z < zSize - 1; z++) {
                        nodesV.add(super.nodes[x][0][z]);
                    }
                }
                break;
            case 21: // near-end surface no chaice of first and last
                for (int y = 1; y < ySize - 1; y++) {
                    for (int z = 1; z < zSize - 1; z++) {
                        if (isYZvalid(y, z))
                            nodesV.add(super.nodes[0][y][z]);
                    }
                }
                break;
            case 22: // far-end surface no chaice of first and last
                for (int y = 1; y < ySize - 1; y++) {
                    for (int z = 1; z < zSize - 1; z++) {
                        if (isYZvalid(y, z))
                            nodesV.add(super.nodes[xSize - 1][y][z]);
                    }
                }
        }
        return nodesV;
    }

    /**
     * Checks if y and z cell coordinates are inside charge
     *
     * @param y
     * @param z
     * @return
     */
    boolean isYZvalid(int y, int z) {
        if ((y < 1) || (y > (ySize - 2)) || (z < 1) || (z > (zSize - 2)))
            return false;
        boolean retVal = false;
        switch (chargeDef.chargeType) {
            case ChargeDef.RECTANGULAR:
                retVal = true;
                break;
            case ChargeDef.BEAMBLANK_H:
                if (y < cornerY1 || y > cornerY2)
                    retVal = true;
                else if (z > cornerZ1 && z < cornerZ2)
                    retVal = true;
                break;
            case ChargeDef.BEAMBLANK_V:
                if (z < cornerZ1 || z > cornerZ2)
                    retVal = true;
                else if (y > cornerY1 && y < cornerY2)
                    retVal = true;
                break;
        }
        return retVal;
    }

    public boolean isPartOfCharge(int x, int y, int z) {
        return isYZvalid(y, z);
    }

    public void setSurfaceTemperature(int theFace, double temperature) {
        OneNode aNode;
        switch (theFace) {
            case WIDTHMINFACE: // y = 0 face
                for (int x = 0; x < xSize; x++) {
                    for (int z = 0; z < zSize; z++) {
                        setTemperatureAt(temperature, x, 0, z);
                    }
                }
                break;
            case WIDTHMAXFACE: // y = ySize - 1 face
                for (int x = 0; x < xSize; x++) {
                    for (int z = 0; z < zSize; z++) {
                        setTemperatureAt(temperature, x, ySize - 1, z);
                    }
                }
                break;
            case HEIGHTMINFACE: // z = 0 face
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        setTemperatureAt(temperature, x, y, 0);
                    }
                }
                break;
            case HEIGHTMAXFACE: // z = zSize - 1 face
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        setTemperatureAt(temperature, x, y, zSize - 1);
                    }
                }
                break;
            case LENGTHMINFACE: // x = 0 face
                for (int y = 0; y < ySize; y++) {
                    for (int z = 0; z < zSize; z++) {
                        setTemperatureAt(temperature, 0, y, z);
                    }
                }
                break;
            case LENGTHMAXFACE: // x = xSize - 1 face
                for (int y = 0; y < ySize; y++) {
                    for (int z = 0; z < zSize; z++) {
                        setTemperatureAt(temperature, xSize - 1, y, z);
                    }
                }
                break;
        }
    }

    public void setSurfaceTemperature(int theFace, double temperature,
                                      int xStart, int yStart, int xEnd, int yEnd) {
        OneNode aNode;
        switch (theFace) {
            case WIDTHMINFACE: // y = 0 face
                for (int x = xStart; x <= xEnd; x++) {
                    for (int z = yStart; z <= yEnd; z++) {
                        setTemperatureAt(temperature, x, 0, z);
                    }
                }
                break;
            case WIDTHMAXFACE: // y = ySize - 1 face
                for (int x = xStart; x <= xEnd; x++) {
                    for (int z = yStart; z <= yEnd; z++) {
                        setTemperatureAt(temperature, x, ySize - 1, z);
                    }
                }
                break;
            case HEIGHTMINFACE: // z = 0 face
                for (int x = xStart; x <= xEnd; x++) {
                    for (int y = yStart; y <= yEnd; y++) {
                        setTemperatureAt(temperature, x, y, 0);
                    }
                }
                break;
            case HEIGHTMAXFACE: // z = zSize - 1 face
                for (int x = xStart; x <= xEnd; x++) {
                    for (int y = yStart; y <= yEnd; y++) {
                        setTemperatureAt(temperature, x, y, zSize - 1);
                    }
                }
                break;
            case LENGTHMINFACE: // x = 0 face
                for (int y = xStart; y <= xEnd; y++) {
                    for (int z = yStart; z <= yEnd; z++) {
                        setTemperatureAt(temperature, 0, y, z);
                    }
                }
                break;
            case LENGTHMAXFACE: // x = xSize - 1 face
                for (int y = xStart; y <= xEnd; y++) {
                    for (int z = yStart; z <= yEnd; z++) {
                        setTemperatureAt(temperature, xSize - 1, y, z);
                    }
                }
                break;
        }
    }

    public void setSurfaceHtTrCoeff(int theFace, double heatTrCoeff) {
        OneNode aNode;
        switch (theFace) {
            case WIDTHMINFACE: // y = 0 face
                for (int x = 0; x < xSize; x++) {
                    for (int z = 0; z < zSize; z++) {
                        setHeatTrCoeff(heatTrCoeff, x, 0, z);
                    }
                }
                break;
            case WIDTHMAXFACE: // y = ySize - 1 face
                for (int x = 0; x < xSize; x++) {
                    for (int z = 0; z < zSize; z++) {
                        setHeatTrCoeff(heatTrCoeff, x, ySize - 1, z);
                    }
                }
                break;
            case HEIGHTMINFACE: // z = 0 face
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        setHeatTrCoeff(heatTrCoeff, x, y, 0);
                    }
                }
                break;
            case HEIGHTMAXFACE: // z = zSize - 1 face
                for (int x = 0; x < xSize; x++) {
                    for (int y = 0; y < ySize; y++) {
                        setHeatTrCoeff(heatTrCoeff, x, y, zSize - 1);
                    }
                }
                break;
            case LENGTHMINFACE: // x = 0 face
                for (int y = 0; y < ySize; y++) {
                    for (int z = 0; z < zSize; z++) {
                        setHeatTrCoeff(heatTrCoeff, 0, y, z);
                    }
                }
                break;
            case LENGTHMAXFACE: // x = xSize - 1 face
                for (int y = 0; y < ySize; y++) {
                    for (int z = 0; z < zSize; z++) {
                        setHeatTrCoeff(heatTrCoeff, xSize - 1, y, z);
                    }
                }
                break;
        }
    }

    public void setSurfaceHtTrCoeff(int theFace, double heatTrCoeff,
                                    int xStart, int yStart, int xEnd, int yEnd) {
        OneNode aNode;
        switch (theFace) {
            case WIDTHMINFACE: // y = 0 face
                for (int x = xStart; x <= xEnd; x++) {
                    for (int z = yStart; z <= yEnd; z++) {
                        setHeatTrCoeff(heatTrCoeff, x, 0, z);
                    }
                }
                break;
            case WIDTHMAXFACE: // y = ySize - 1 face
                for (int x = xStart; x <= xEnd; x++) {
                    for (int z = yStart; z <= yEnd; z++) {
                        setHeatTrCoeff(heatTrCoeff, x, ySize - 1, z);
                    }
                }
                break;
            case HEIGHTMINFACE: // z = 0 face
                for (int x = xStart; x <= xEnd; x++) {
                    for (int y = yStart; y <= yEnd; y++) {
                        setHeatTrCoeff(heatTrCoeff, x, y, 0);
                    }
                }
                break;
            case HEIGHTMAXFACE: // z = zSize - 1 face
                for (int x = xStart; x <= xEnd; x++) {
                    for (int y = yStart; y <= yEnd; y++) {
                        setHeatTrCoeff(heatTrCoeff, x, y, zSize - 1);
                    }
                }
                break;
            case LENGTHMINFACE: // x = 0 face
                for (int y = xStart; y <= xEnd; y++) {
                    for (int z = yStart; z <= yEnd; z++) {
                        setHeatTrCoeff(heatTrCoeff, 0, y, z);
                    }
                }
                break;
            case LENGTHMAXFACE: // x = xSize - 1 face
                for (int y = xStart; y <= xEnd; y++) {
                    for (int z = yStart; z <= yEnd; z++) {
                        setHeatTrCoeff(heatTrCoeff, xSize - 1, y, z);
                    }
                }
                break;
        }
    }

    public double getXLocation(int x) {
        return (halfUnitSide + unitSide * (x - 1));
    }

    public double getYLocation(int y) {
        return (halfUnitSide + unitSide * (y - 1));
    }

    public double getZLocation(int z) {
        return (halfUnitSide + unitSide * (z - 1));
    }

    double[] getCellXEdgeList() {
        double[] edges = new double[xSize - 2];
        double start = 0;
        edges[0] = start;
        start += unitSide;
        for (int x = 1; x < xSize - 2; x++, start += unitSide)
            edges[x] = start;
        return edges;
    }
//
//  public double getXLocation(int x) {
//    return (unitSide * x);
//  }
//
//  public double getYLocation(int y) {
//    return (unitSide * y);
//  }
//
//  public double getZLocation(int z) {
//    return (unitSide * z);
//  }

    public static boolean isChargeFace(int face) {
        if (face == WIDTHMINFACE ||
                face == WIDTHMAXFACE ||
                face == HEIGHTMINFACE ||
                face == HEIGHTMAXFACE ||
                face == LENGTHMINFACE ||
                face == LENGTHMAXFACE)
            return true;
        else
            return false;
    }

    public static String getSurfaceName(int theFace) {
        String name = null;
        switch (theFace) {
            case WIDTHMINFACE: // y = 0 face
                name = "FRONT Surface";
                break;
            case WIDTHMAXFACE: // y = ySize - 1 face
                name = "BACK Surface";
                break;
            case HEIGHTMINFACE: // z = 0 face
                name = "BOTTOM Surface";
                break;
            case HEIGHTMAXFACE: // z = zSize - 1 face
                name = "TOP Surface";
                break;
            case LENGTHMINFACE: // x = 0 face
                name = "LEFT Surface";
                break;
            case LENGTHMAXFACE: // x = xSize - 1 face
                name = "RIGHT Surface";
                break;
            default:
                name = "Unknown Surface";
                break;
        }
        return name;
    }

    public double getCellTemperature(Point3i point) {
        return getDataAt(point.x, point.y, point.z);
    }

    public double getCellTemperature(int x, int y, int z) {
        return getDataAt(x, y, z);
    }

    public double getAverageTemp() {
        double value = 0;
        for (int x = 1; x < xSize - 1; x++)
            for (int y = 1; y < ySize; y++)
                for (int z = 1; z < zSize; z++) {
//          debug("getAverageTemp" + x + "," + y + "," + z);
                    value += nodes[x][y][z].getTemperature();
                }
        return value / ((xSize - 2) * (ySize - 2) * (zSize - 2));
    }

    public MaxMinAvgAt getMaxMinAvgTemp() {
        double avg = 0;
        double minVal = 1e10;
        double maxVal = -1e10;
        int maxx = 0, maxy = 0, maxz = 0;
        int minx = 0, miny = 0, minz = 0;

        double value;
        for (int x = 1; x < xSize - 1; x++)
            for (int y = 1; y < ySize - 1; y++)
                for (int z = 1; z < zSize - 1; z++) {
                    value = nodes[x][y][z].getTemperature();
                    avg += value;
                    if (value > maxVal) {
                        maxVal = value;
                        maxx = x;
                        maxy = y;
                        maxz = z;
                    }
                    if (value < minVal) {
                        minVal = value;
                        minx = x;
                        miny = y;
                        minz = z;
                    }
                }
        avg = avg / ((xSize - 2) * (ySize - 2) * (zSize - 2));
        MaxMinAvgAt retVal = new MaxMinAvgAt(
                new ValueAt(maxVal,
                        halfUnitSide + (maxx - 1) * unitSide,
                        halfUnitSide + (maxy - 1) * unitSide,
                        halfUnitSide + (maxz - 1) * unitSide),
                new ValueAt(minVal,
                        halfUnitSide + (minx - 1) * unitSide,
                        halfUnitSide + (miny - 1) * unitSide,
                        halfUnitSide + (minz - 1) * unitSide),
                avg);
        return retVal;
    }

    public double getSurfaceTemp(int surface) {
        double value = 0;
        int x, y, z;
        switch (surface) {
            case WIDTHMINFACE: // y = 1 face
                y = 1;
                for (x = 1; x < xSize - 1; x++) {
                    for (z = 1; z < zSize - 1; z++) {
                        value += nodes[x][y][z].getTemperature();
                    }
                }
                value = value / ((xSize - 2) * (zSize - 2));
                break;
            case WIDTHMAXFACE: // y = ySize - 2 face
                y = ySize - 2;
                for (x = 1; x < xSize - 1; x++) {
                    for (z = 1; z < zSize - 1; z++) {
                        value += nodes[x][y][z].getTemperature();
                    }
                }
                value = value / ((xSize - 2) * (zSize - 2));
                break;
            case HEIGHTMINFACE: // z = 1 face
                z = 1;
                for (x = 1; x < xSize - 1; x++) {
                    for (y = 1; y < ySize - 1; y++) {
                        value += nodes[x][y][z].getTemperature();
                    }
                }
                value = value / ((xSize - 2) * (ySize - 2));
                break;
            case HEIGHTMAXFACE: // z = zSize - 2 face
                z = zSize - 2;
                for (x = 1; x < xSize - 1; x++) {
                    for (y = 1; y < ySize - 1; y++) {
                        value += nodes[x][y][z].getTemperature();
                    }
                }
                value = value / ((xSize - 2) * (ySize - 2));
                break;
            case LENGTHMINFACE: // x = 1 face
                x = 1;
                for (y = 1; y < ySize - 1; y++) {
                    for (z = 1; z < zSize - 1; z++) {
                        value += nodes[x][y][z].getTemperature();
                    }
                }
                value = value / ((ySize - 2) * (zSize - 2));
                break;
            case LENGTHMAXFACE: // x = xSize - 2 face
                x = xSize - 2;
                for (y = 1; y < ySize - 1; y++) {
                    for (z = 1; z < zSize - 1; z++) {
                        value += nodes[x][y][z].getTemperature();
                    }
                }
                value = value / ((ySize - 2) * (zSize - 2));
                break;
        }
        return value;

    }

    public ValueAt getMaxTemperatureAt() {
        MaxMinAvgAt maxminavg = getMaxMinAvgTemp();
        return maxminavg.getMaxAt();
    }

    public ValueAt getMinTemperatureAt() {
        MaxMinAvgAt maxminavg = getMaxMinAvgTemp();
        return maxminavg.getMinAt();
    }

    public static Hashtable getSurfaceNameTable() {
        Hashtable<Integer, String> table = new Hashtable<Integer, String> ();
        table.put(new Integer(WIDTHMINFACE),
                getSurfaceName(WIDTHMINFACE));
        table.put(new Integer(WIDTHMAXFACE),
                getSurfaceName(WIDTHMAXFACE));
        table.put(new Integer(HEIGHTMINFACE),
                getSurfaceName(HEIGHTMINFACE));
        table.put(new Integer(HEIGHTMAXFACE),
                getSurfaceName(HEIGHTMAXFACE));
        table.put(new Integer(LENGTHMINFACE),
                getSurfaceName(LENGTHMINFACE));
        table.put(new Integer(LENGTHMAXFACE),
                getSurfaceName(LENGTHMAXFACE));
        return table;
    }

    double[] getSurfaceHeatList() {
        double[] heatList = new double[outerSurfaces.length];
        for (int i = 0; i < heatList.length; i++)
            heatList[i] = outerSurfaces[i].getHeatTransferQ();
        return heatList;
    }

    public ElementType getElementType() {
        return type;
    }

    public double getC(double temperature) {
        return type.getC(temperature);
    }

    public double getC() {
        return getC(getAverageTemp());
    }

    public double getTk(double temperature) {
        return type.getTk(temperature);
    }

    public double getTk() {
        return getTk(getAverageTemp());
    }

    public double getDensity() {
        return type.getDensity();
    }

    public double getUnitSide() {
        return unitSide;
    }

    /**
     * @param l
     * @param w
     * @param h
     * @return returns the cell location correposding to location inside charge after
     *         correcting for adjusted charge size basd on cell width
     */
    public Point3i getCell(double l, double w, double h) {
        Point3i point = null;
        if (validLocation(l, w, h)) {
            // modify to match the adjusted size
            l = l / length * adjustedLength;
            w = w / width * adjustedWidth;
            h = h / height * adjustedHeight;
            int x = (int) ((l - 0.00001) / unitSide) + 1;
            int y = (int) ((w - 0.00001) / unitSide) + 1;
            int z = (int) ((h - 0.00001) / unitSide) + 1;
            point = new Point3i(x, y, z);
        }
        return point;
    }

    boolean validLocation(double l, double w, double h) {
        boolean retVal = true;
        if (l < 0 || l > length ||
                w < 0 || w > width ||
                h < 0 || h > height)
            retVal = false;
        return retVal;
    }


    public double apportionHeatIn(boolean fromTop, boolean onSkidRegion,
                                  double heatData[]) {
        double retVal = 0;
        Double factor;
        Hashtable heatSrc = null;
        if (fromTop) {
            heatSrc = topSurfaces;
            for (int i = 0; i < heatData.length; i++) {
                factor = (Double) heatSrc.get(new Integer(outerSurfaces[i].surfaceID));
                if (factor != null)
                    retVal += factor.doubleValue() * heatData[i];
            }
        } else {
            heatSrc = botSurfaces;
            for (int i = 0; i < heatData.length; i++) {
                factor = (Double) heatSrc.get(new Integer(outerSurfaces[i].surfaceID));
                if (factor != null && (outerSurfaces[i].aSkid == onSkidRegion))
                    retVal += factor.doubleValue() * heatData[i];
            }
        }
        return retVal;
    }


    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(chargeDef);
//   ElementType type;

        out.writeDouble(unitSide);
        out.writeDouble(halfUnitSide);
        out.writeDouble(width);
        out.writeDouble(height);
        out.writeDouble(length);
        out.writeDouble(adjustedWidth);
        out.writeDouble(adjustedHeight);
        out.writeDouble(adjustedLength);
        out.writeDouble(adjustedFlangeT);
        out.writeDouble(adjustedWebT);
        out.writeInt(xSize);
        out.writeInt(ySize);
        out.writeInt(zSize);
        out.writeInt(flangeTint);
        out.writeInt(webTint);
        out.writeInt(webMinCell);
        out.writeInt(webMaxCell);
        debug("outerSurfaces NOT saved!");
//   OuterSurface[] outerSurfaces;
        debug("saving topSurfaces");
        out.writeObject(topSurfaces);
        debug("saving botSurfaces");
        out.writeObject(botSurfaces);
//   Hashtable topSurfaces, botSurfaces; // table of surfaces which receive heat
        // frop top or bottom

        // the corner co-ordinates defining he flanges and web for Beam blanks
        // the cell is part of the border cell
        out.writeInt(cornerY1);
        out.writeInt(cornerY2);
        out.writeInt(cornerZ1);
        out.writeInt(cornerZ2);
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        chargeDef = (ChargeDef) in.readObject();
//   ElementType type;

        unitSide = in.readDouble();
        halfUnitSide = in.readDouble();
        width = in.readDouble();
        height = in.readDouble();
        length = in.readDouble();
        adjustedWidth = in.readDouble();
        adjustedHeight = in.readDouble();
        adjustedLength = in.readDouble();
        adjustedFlangeT = in.readDouble();
        adjustedWebT = in.readDouble();
        xSize = in.readInt();
        ySize = in.readInt();
        zSize = in.readInt();
        flangeTint = in.readInt();
        webTint = in.readInt();
        webMinCell = in.readInt();
        webMaxCell = in.readInt();
        debug("outerSurfaces NOT read!");
//   OuterSurface[] outerSurfaces;
        debug("reading topSurfaces");
        topSurfaces = (Hashtable<Integer, Double>) in.readObject();
        debug("reading botSurfaces");
        botSurfaces = (Hashtable<Integer, Double>) in.readObject();
        cornerY1 = in.readInt();
        cornerY2 = in.readInt();
        cornerZ1 = in.readInt();
        cornerZ2 = in.readInt();
    }

    void errMessage(String msg) {
        System.err.println("ThreeDCharge: ERROR: " + msg);
    }

    void debug(String msg) {
        System.out.println("ThreeDCharge: " + msg);
    }

}

