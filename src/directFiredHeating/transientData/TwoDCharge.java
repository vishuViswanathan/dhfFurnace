package directFiredHeating.transientData;

import basic.ChMaterial;
import basic.Charge;
import directFiredHeating.DFHFurnace;
import directFiredHeating.UnitFurnace;

import javax.swing.*;
import java.util.Vector;

public class TwoDCharge {
    public enum FaceType {
        WIDTHNEARFACE("Width Front Face"),
        WIDTHFARTHERFACE("Width Back Face"),
        BOTTOMFACE("Bottom FAce"),
        TOPFACE("Top FAce");

        private final String resultName;

        FaceType(String resultName) {
            this.resultName = resultName;
        }

        public String resultName() {
            return resultName;
        }
        public static FaceType getEnum(String text) {
            if (text != null) {
                for (FaceType b : FaceType.values()) {
                    if (text.equalsIgnoreCase(b.resultName)) {
                        return b;
                    }
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return resultName;    //To change body of overridden methods use File | Settings | File Templates.
        }
    }
    DFHFurnace furnace;
    Charge charge;
    double unitSide, halfUnitSide;
    public double width, height;
    double adjustedWidth, adjustedHeight;
    int ySize, zSize;

    double unitTime = Double.NaN;
    double time = 0;

    One2DNode nodes[][];
    Vector<Boundary2D> boundaries = new Vector<>();

    OrderedDictionary<OneCombinedAmbient> history;
    double totInternalCells = 0;
    boolean boundariesDirty = true;

    public TwoDCharge(DFHFurnace furnace, Charge charge, int minSlices) {
        this.furnace = furnace;
        this.charge = charge;
        double cellSize = (Math.min(charge.width, charge.height) / minSlices);
        this.unitSide = cellSize;
        init();
    }

    void init() {
        width = charge.width;
        height  = charge.height;
        // size increased to include border conditions
        ySize = (int) Math.round(width / unitSide);
        adjustedWidth = unitSide * ySize;
        zSize = (int) Math.round(height / unitSide);
        adjustedHeight = unitSide * zSize;
        // size increased to include border conditions
        totInternalCells = ySize * zSize;
        ySize += 2;
        zSize += 2;
        setSize(ySize, zSize);

        // set body nodes
        One2DNode aNode;
        // the body
        for (int y = 1; y < ySize - 1; y++) {
            for (int z = 1; z < zSize - 1; z++) {
                aNode = new One2DNode(charge.chMaterial, unitSide, unitSide);
                this.add(aNode, y, z);
            }
        }
        // for beam blanks mark part cells as insulated

        // by default insulate the boundary
        insulateFace(FaceType.WIDTHNEARFACE);
        insulateFace(FaceType.WIDTHFARTHERFACE);
        insulateFace(FaceType.TOPFACE);
        insulateFace(FaceType.BOTTOMFACE);
        setBoundaries();

        history = new OrderedDictionary<>();
        Vector<UnitFurnace> topSlots = furnace.getSlotList(false);
        Vector<UnitFurnace> botSlots = null;
        if (furnace.bTopBot)
            botSlots = furnace.getSlotList(true);
        for (int s = 0; s < topSlots.size(); s++) {
            UnitFurnace topUf = topSlots.get(s);
            double endTime = topUf.endTime;
            history.addData(endTime, new OneCombinedAmbient(furnace, this,
                    topUf, (furnace.bTopBot)?botSlots.get(s): null));
        }
    }

    public boolean add(One2DNode node, int atY, int atZ) {
        if (nodes[atY][atZ] != null)
            return false;
        nodes[atY][atZ] = node;
        return true;
    }

    protected void setSize(int ySize, int zSize) {
        nodes = new One2DNode[ySize][zSize];
        debug("Nodes = " + ySize + ", " + zSize);

    }

    public void copyS152ToUfs(double upperLimit) {
        for (int i = 0; i < history.getSize(); i++)
            (history.getDataSet(i)).data.setS152inUnitFurnaces(upperLimit);
    }

    public void insulateFace(FaceType theFace) {
        setSurfaceElement(theFace, null);
    }

    public void setSurfaceElement(FaceType theFace, ChMaterial material) {
        One2DNode aNode;
        switch (theFace) {
            case WIDTHNEARFACE: // y = 0 face
                for (int z = 0; z < zSize; z++) {
                    aNode = new One2DNode(material, unitSide, unitSide);
                    this.add(aNode, 0, z);
                }
            break;
            case WIDTHFARTHERFACE: // y = ySize - 1 face
                for (int z = 0; z < zSize; z++) {
                    aNode = new One2DNode(material, unitSide, unitSide);
                    this.add(aNode, ySize - 1, z);
                }
                break;
            case TOPFACE: // z = 0 face
                for (int y = 0; y < ySize; y++) {
                    aNode = new One2DNode(material, unitSide, unitSide);
                    this.add(aNode, y, 0);
                }
                break;
            case BOTTOMFACE: // z = zSize - 1 face
                for (int y = 0; y < ySize; y++) {
                    aNode = new One2DNode(material, unitSide, unitSide);
                    this.add(aNode, y, zSize - 1);
                }
                break;
        }
    }

    public void setBoundaries() {
        Boundary2D boundary;
        One2DNode neighbour, node;
        for (int atY = 0; atY < ySize; atY++) {
            for (int atZ = 0; atZ < zSize; atZ++) {
                if ((node = nodes[atY][atZ]) == null) {
                    errMessage("No node at " + atY + ", " + atZ);
                } else {
                    if (atY > 0) {
                        neighbour = nodes[atY - 1][atZ];
                        if (neighbour != null) {
                            if (neighbour.getBoundary(One2DNode.BACK) == null) {
//                                boundary =
//                                        new Boundary2D(node, neighbour, Boundary2D.ZX);
                                boundary =
                                        new Boundary2D(neighbour, node, Boundary2D.ZX);
                                neighbour.noteBoundary(boundary, One2DNode.BACK);
                                node.noteBoundary(boundary, One2DNode.FRONT);
                                boundaries.addElement(boundary);
                            }
                        }
                    }
                    if (atY < ySize - 1) {
                        neighbour = nodes[atY + 1][atZ];
                        if (neighbour != null) {
                            if (neighbour.getBoundary(One2DNode.FRONT) == null) {
//                                boundary =
//                                        new Boundary2D(neighbour, node, Boundary2D.ZX);
                                boundary =
                                        new Boundary2D(node, neighbour, Boundary2D.ZX);
                                neighbour.noteBoundary(boundary, One2DNode.FRONT);
                                node.noteBoundary(boundary, One2DNode.BACK);
                                boundaries.addElement(boundary);
                            }
                        }
                    }

                    if (atZ > 0) {
                        neighbour = nodes[atY][atZ - 1];
                        if (neighbour != null) {
                            if (neighbour.getBoundary(One2DNode.ABOVE) == null) {
                                boundary =
                                        new Boundary2D(neighbour, node, Boundary2D.XY);
                                neighbour.noteBoundary(boundary, One2DNode.ABOVE);
                                node.noteBoundary(boundary, One2DNode.BELOW);
                                boundaries.addElement(boundary);
                            }
                        }
                    }

                    if (atZ < zSize - 1) {
                        neighbour = nodes[atY][atZ + 1];
                        if (neighbour != null) {
                            if (neighbour.getBoundary(One2DNode.BELOW) == null) {
                                boundary =
                                        new Boundary2D(node, neighbour, Boundary2D.XY);
                                neighbour.noteBoundary(boundary, One2DNode.BELOW);
                                node.noteBoundary(boundary, One2DNode.ABOVE);
                                boundaries.addElement(boundary);
                            }
                        }
                    }
                }
            }
        }
    }

    double[] getSurfceTemperatures(FaceType face) {
        double[] arr = null;
        switch (face) {
            case TOPFACE:
                arr = new double[ySize];
                for (int y = 1; y < ySize - 1; y++) {
                    arr[y] = nodes[y][zSize - 2].getBoundary(One2DNode.ABOVE).getTemperature();
                }
                break;
            case BOTTOMFACE:
                arr = new double[ySize];
                for (int y = 1; y < ySize - 1; y++) {
                    arr[y] = nodes[y][1].getBoundary(One2DNode.BELOW).getTemperature();
                }
                break;
            case WIDTHNEARFACE:
                arr = new double[zSize];
                for (int z = 1; z < zSize - 1; z++) {
                    arr[z] = nodes[1][z].getBoundary(One2DNode.FRONT).getTemperature();
                }
                break;
            case WIDTHFARTHERFACE:
                arr = new double[zSize];
                for (int z = 1; z < zSize - 1; z++) {
                    arr[z] = nodes[ySize - 2][z].getBoundary(One2DNode.BACK).getTemperature();
                }
                break;
        }
        return arr;
    }

    void calculateBoundaries(double deltTime, double tk) {
        for (Boundary2D b:boundaries) {
            b.update(deltTime, tk);
        }
        boundariesDirty = false;
    }

    boolean updateNodes(double deltaTime, double spHt) {
        if (boundariesDirty) {
            errMessage("boundaries NOT updated!");
            return false;
        } else {
            for (int y = 1; y < (ySize - 1); y++) {
                for (int z = 1; z < (zSize - 1); z++) {
                    nodes[y][z].update(deltaTime, spHt);
                }
            }
            boundariesDirty = false; // ????
            // swap new temp to now
            for (int y = 1; y < (ySize - 1); y++) {
                for (int z = 1; z < (zSize - 1); z++) {
                    nodes[y][z].resetTemperature();
                }
            }
        }
        return true;
    }

    void setAllInternalTemps(double temp) {
        for (int y = 1; y < (ySize - 1); y++) {
            for (int z = 1; z < (zSize - 1); z++) {
                nodes[y][z].setTemperature(temp);
            }
        }
    }

    void setOuterTempAndAlpha(OneCombinedAmbient amb) {
        // bottom
        for (int y = 1; y < (ySize - 1); y++) {
            nodes[y][0].setTemperature(amb.botAmbTemp);
            nodes[y][0].setHeatTfCoeff(amb.botAlpha);
        }
        // top
        for (int y = 1; y < (ySize - 1); y++) {
            nodes[y][zSize - 1].setTemperature(amb.topAmbTemp);
            nodes[y][zSize - 1].setHeatTfCoeff(amb.topAlpha);
        }
        // sided FRONT and BACK
        for (int z = 1; z < (zSize - 1); z++) {
            // Near side (FRONT)
            nodes[0][z].setTemperature(amb.sideAmbTemp);
            nodes[0][z].setHeatTfCoeff(amb.sideAlpha);
            // Far side (BACK)
            nodes[ySize - 1][z].setTemperature(amb.sideAmbTemp);
            nodes[ySize - 1][z].setHeatTfCoeff(amb.sideAlpha);
        }
    }

    public boolean update(double deltaTime, double spHt, double tk) {
        calculateBoundaries(deltaTime, tk);
        return updateNodes(deltaTime, spHt);
    }

    boolean evaluateForAmb(OneCombinedAmbient amb, double chTemp) {
        double meanTemp = chTemp;
        double spHt = charge.chMaterial.spHt(meanTemp);
        double tk = charge.chMaterial.getTk(meanTemp);
        setUnitTime(spHt, tk);
        setOuterTempAndAlpha(amb);
        while (time < amb.endTime) {
            update(unitTime, spHt, tk);
            time += unitTime;
        }
        // do one more step
        update(unitTime, spHt, tk);
        time += unitTime;
        return true;
    }

    void saveResultsInHistory(OneCombinedAmbient amb) {
        double[][] allCellsTemps = new double[ySize][zSize];
        for (int y = 0; y < ySize; y++) {
            for (int z = 0; z < zSize; z++) {
                allCellsTemps[y][z] = nodes[y][z].nowTemperature;
            }
        }
        Vector<double[]> surfaceTemps = new Vector<>();
        surfaceTemps.add(getSurfceTemperatures(FaceType.BOTTOMFACE));
        surfaceTemps.add(getSurfceTemperatures(FaceType.WIDTHNEARFACE));
        surfaceTemps.add(getSurfceTemperatures(FaceType.TOPFACE));
        surfaceTemps.add(getSurfceTemperatures(FaceType.WIDTHFARTHERFACE));
        amb.noteResults(time, allCellsTemps, surfaceTemps);
    }

    private void setUnitTime(double c, double lambda) {
        unitTime = 0.5 * (1.0 / 6.0 *
                (c * charge.chMaterial.density * unitSide *unitSide / lambda));
//    unitTime = 0.5*(1.0/4.0*(c*density * dx * dx /lambda));
// DEBUG
//        debug("Unit time " + unitTime + ", c = " + c + ", tk = " + lambda);
    }

    public boolean evaluate(double startTemp) {
        boolean retVal = true;
        setAllInternalTemps(startTemp);
        OneCombinedAmbient amb;
        double chTemp = startTemp;
        time = 0;

        for (int i = 0; i < history.getSize(); i++) {
            amb = (history.getDataSet(i)).data;
            if (evaluateForAmb(amb, chTemp)) {
                saveResultsInHistory(amb);
                chTemp = amb.meanTemp;
            }
            else {
                retVal = false;
                break;
            }
        }
        return retVal;
    }

    void errMessage(String msg) {
        JOptionPane.showMessageDialog(null, msg, "TwoDCharge",
                JOptionPane.ERROR_MESSAGE);
    }

    void debug(String msg) {
        System.out.println("TwoDCharge: " + msg);
    }
}
