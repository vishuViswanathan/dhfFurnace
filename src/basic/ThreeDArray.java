package basic;

import java.util.*;
import java.io.*;
import java.text.*;
import javax.swing.*;

public class ThreeDArray extends Object implements Serializable {
    int tempCntBoundary, tempCntNode;
    int xSize, ySize, zSize;
    OneNode nodes[][][];
    Vector<Boundary> boundaries = new Vector<Boundary>();
    Boundary[] boundariesArray = null;
    boolean boundariesDirty = true;
    public ThreeDArray(int xSize, int ySize, int zSize) {
        setSize(xSize, ySize, zSize);
    }

    protected ThreeDArray() {
    }

    protected void setSize(int xSize, int ySize, int zSize) {
        if (xSize < 1) xSize = 1;
        if (ySize < 1) ySize = 1;
        if (zSize < 1) zSize = 1;
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
        nodes = new OneNode[xSize][ySize][zSize];
// DEBUG
        debug("Nodes = " + xSize + ", " + ySize + ", " + zSize);

    }

    public boolean add(OneNode node, int atX, int atY, int atZ) {
        if (nodes[atX][atY][atZ] != null)
            return false;
        replace(node, atX, atY, atZ);
        return true;
    }

    OneNode getNodeAt(int atX, int atY, int atZ) {
        return nodes[atX][atY][atZ];
    }

    /**
     * uncondtional add to location
     */
    public void replace(OneNode node, int atX, int atY, int atZ) {
        nodes[atX][atY][atZ] = node;
/*
    OneNode neighbour;
    if (!checkIndexes(atX, atY, atZ)) {
        return;
    }
tempCntNode++;
int tempCntB1 = 0;
    Boundary boundary = null;
    if (atX > 0) {
      neighbour = nodes[atX - 1][atY][atZ];
      if (neighbour != null) {
        if (neighbour.getBoundary(OneNode.RIGHT) == null) {
          boundary =
              new Boundary(neighbour, node, Boundary.YZ);
          neighbour.noteBoundary(boundary, OneNode.RIGHT);
          node.noteBoundary(boundary, OneNode.LEFT);
          boundaries.addElement(boundary);
tempCntB1++;
tempCntBoundary++;
        }
      }
    }
    if (atX < xSize - 1) {
      neighbour = nodes[atX + 1][atY][atZ];
      if (neighbour != null) {
        if (neighbour.getBoundary(OneNode.LEFT) == null) {
          boundary =
              new Boundary(node, neighbour, Boundary.YZ);
          neighbour.noteBoundary(boundary, OneNode.LEFT);
          node.noteBoundary(boundary, OneNode.RIGHT);
          boundaries.addElement(boundary);
          tempCntB1++;
          tempCntBoundary++;
        }
      }
    }

   if (atY > 0) {
      neighbour = nodes[atX][atY - 1][atZ];
      if (neighbour != null) {
        if (neighbour.getBoundary(OneNode.FRONT) == null) {
          boundary =
              new Boundary(neighbour, node, Boundary.ZX);
          neighbour.noteBoundary(boundary, OneNode.FRONT);
          node.noteBoundary(boundary, OneNode.BACK);
          boundaries.addElement(boundary);
          tempCntB1++;
          tempCntBoundary++;
        }
      }
    }
    if (atY < ySize - 1) {
      neighbour = nodes[atX][atY + 1][atZ];
      if (neighbour != null) {
        if (neighbour.getBoundary(OneNode.BACK) == null) {
          boundary =
              new Boundary(node, neighbour, Boundary.ZX);
          neighbour.noteBoundary(boundary, OneNode.BACK);
          node.noteBoundary(boundary, OneNode.FRONT);
          boundaries.addElement(boundary);
          tempCntB1++;
          tempCntBoundary++;
        }
      }
    }

   if (atZ > 0) {
      neighbour = nodes[atX][atY][atZ - 1];
      if (neighbour != null) {
        if (neighbour.getBoundary(OneNode.ABOVE) == null) {
          boundary =
              new Boundary(neighbour, node, Boundary.XY);
          neighbour.noteBoundary(boundary, OneNode.ABOVE);
          node.noteBoundary(boundary, OneNode.BELOW);
          boundaries.addElement(boundary);
          tempCntB1++;
          tempCntBoundary++;
        }
      }
    }

    if (atZ < zSize - 1) {
      neighbour = nodes[atX][atY][atZ + 1];
      if (neighbour != null) {
        if (neighbour.getBoundary(OneNode.BELOW) == null) {
          boundary =
              new Boundary(node, neighbour, Boundary.XY);
          neighbour.noteBoundary(boundary, OneNode.BELOW);
          node.noteBoundary(boundary, OneNode.ABOVE);
          boundaries.addElement(boundary);
          tempCntB1++;
          tempCntBoundary++;
        }
      }
    }
debug("node " + tempCntNode + " at " + atX + "," + atY + "," + atZ + ", Bound1 = " + tempCntB1 + ", Total B = " + tempCntBoundary);
*/
    }

    public void setBoundaries() {
        Boundary boundary = null;
        OneNode neighbour, node;
        for (int atX = 0; atX < xSize; atX++) {
            for (int atY = 0; atY < ySize; atY++) {
                for (int atZ = 0; atZ < zSize; atZ++) {
                    if ((node = nodes[atX][atY][atZ]) == null) {
                        errMessage("No node at " + atX + ", " + atY + ", " + atZ);
                    } else {
                        tempCntNode++;
                        int tempCntB1 = 0;
                        if (atX > 0) {
                            neighbour = nodes[atX - 1][atY][atZ];
                            if (neighbour != null) {
                                if (neighbour.getBoundary(OneNode.RIGHT) == null) {
                                    boundary =
                                            new Boundary(neighbour, node, Boundary.YZ);
                                    neighbour.noteBoundary(boundary, OneNode.RIGHT);
                                    node.noteBoundary(boundary, OneNode.LEFT);
                                    boundaries.addElement(boundary);
                                    tempCntB1++;
                                    tempCntBoundary++;
                                }
                            }
                        }
                        if (atX < xSize - 1) {
                            neighbour = nodes[atX + 1][atY][atZ];
                            if (neighbour != null) {
                                if (neighbour.getBoundary(OneNode.LEFT) == null) {
                                    boundary =
                                            new Boundary(node, neighbour, Boundary.YZ);
                                    neighbour.noteBoundary(boundary, OneNode.LEFT);
                                    node.noteBoundary(boundary, OneNode.RIGHT);
                                    boundaries.addElement(boundary);
                                    tempCntB1++;
                                    tempCntBoundary++;
                                }
                            }
                        }

//             if (atY > 0) {
//               neighbour = nodes[atX][atY - 1][atZ];
//               if (neighbour != null) {
//                 if (neighbour.getBoundary(OneNode.FRONT) == null) {
//                   boundary =
//                       new Boundary(neighbour, node, Boundary.ZX);
//                   neighbour.noteBoundary(boundary, OneNode.FRONT);
//                   node.noteBoundary(boundary, OneNode.BACK);
//                   boundaries.addElement(boundary);
//                   tempCntB1++;
//                   tempCntBoundary++;
//                 }
//               }
//             }
//             if (atY < ySize - 1) {
//               neighbour = nodes[atX][atY + 1][atZ];
//               if (neighbour != null) {
//                 if (neighbour.getBoundary(OneNode.BACK) == null) {
//                   boundary =
//                       new Boundary(node, neighbour, Boundary.ZX);
//                   neighbour.noteBoundary(boundary, OneNode.BACK);
//                   node.noteBoundary(boundary, OneNode.FRONT);
//                   boundaries.addElement(boundary);
//                   tempCntB1++;
//                   tempCntBoundary++;
//                 }
//               }
//             }

                        if (atY > 0) {
                            neighbour = nodes[atX][atY - 1][atZ];
                            if (neighbour != null) {
                                if (neighbour.getBoundary(OneNode.BACK) == null) {
                                    boundary =
                                            new Boundary(node, neighbour, Boundary.ZX);
                                    neighbour.noteBoundary(boundary, OneNode.BACK);
                                    node.noteBoundary(boundary, OneNode.FRONT);
                                    boundaries.addElement(boundary);
                                    tempCntB1++;
                                    tempCntBoundary++;
                                }
                            }
                        }
                        if (atY < ySize - 1) {
                            neighbour = nodes[atX][atY + 1][atZ];
                            if (neighbour != null) {
                                if (neighbour.getBoundary(OneNode.FRONT) == null) {
                                    boundary =
                                            new Boundary(neighbour, node, Boundary.ZX);
                                    neighbour.noteBoundary(boundary, OneNode.FRONT);
                                    node.noteBoundary(boundary, OneNode.BACK);
                                    boundaries.addElement(boundary);
                                    tempCntB1++;
                                    tempCntBoundary++;
                                }
                            }
                        }

                        if (atZ > 0) {
                            neighbour = nodes[atX][atY][atZ - 1];
                            if (neighbour != null) {
                                if (neighbour.getBoundary(OneNode.ABOVE) == null) {
                                    boundary =
                                            new Boundary(neighbour, node, Boundary.XY);
                                    neighbour.noteBoundary(boundary, OneNode.ABOVE);
                                    node.noteBoundary(boundary, OneNode.BELOW);
                                    boundaries.addElement(boundary);
                                    tempCntB1++;
                                    tempCntBoundary++;
                                }
                            }
                        }

                        if (atZ < zSize - 1) {
                            neighbour = nodes[atX][atY][atZ + 1];
                            if (neighbour != null) {
                                if (neighbour.getBoundary(OneNode.BELOW) == null) {
                                    boundary =
                                            new Boundary(node, neighbour, Boundary.XY);
                                    neighbour.noteBoundary(boundary, OneNode.BELOW);
                                    node.noteBoundary(boundary, OneNode.ABOVE);
                                    boundaries.addElement(boundary);
                                    tempCntB1++;
                                    tempCntBoundary++;
                                }
                            }
                        }
                    }
                }
            }
        }
    }


/* Tried and failed on 20080604
   public void setBoundaries() {
      Boundary boundary = null;
      OneNode neighbour, node;
      for (int atX = 0; atX < xSize; atX++){
        for (int atY = 0; atY < ySize; atY++) {
          for (int atZ = 0; atZ < zSize; atZ++) {
            if ((node = nodes[atX][atY][atZ]) == null) {
              errMessage("No node at " + atX + ", " + atY + ", " + atZ);
            }
            else {
              tempCntNode++;
              int tempCntB1 = 0;
              if (atX > 0) {
                neighbour = nodes[atX - 1][atY][atZ];
                if (neighbour != null) {
                  if (neighbour.getBoundary(OneNode.FRONT) == null) {
                    boundary =
                        new Boundary(neighbour, node, Boundary.YZ);
                    neighbour.noteBoundary(boundary, OneNode.BACK);
                    node.noteBoundary(boundary, OneNode.FRONT);
                    boundaries.addElement(boundary);
                    tempCntB1++;
                    tempCntBoundary++;
                  }
                }
              }
              if (atX < xSize - 1) {
                neighbour = nodes[atX + 1][atY][atZ];
                if (neighbour != null) {
                  if (neighbour.getBoundary(OneNode.FRONT) == null) {
                    boundary =
                        new Boundary(node, neighbour, Boundary.YZ);
                    neighbour.noteBoundary(boundary, OneNode.FRONT);
                    node.noteBoundary(boundary, OneNode.BACK);
                    boundaries.addElement(boundary);
                    tempCntB1++;
                    tempCntBoundary++;
                  }
                }
              }

              if (atY > 0) {
                neighbour = nodes[atX][atY - 1][atZ];
                if (neighbour != null) {
                  if (neighbour.getBoundary(OneNode.LEFT) == null) {
                    boundary =
                        new Boundary(neighbour, node, Boundary.ZX);
                    neighbour.noteBoundary(boundary, OneNode.LEFT);
                    node.noteBoundary(boundary, OneNode.RIGHT);
                    boundaries.addElement(boundary);
                    tempCntB1++;
                    tempCntBoundary++;
                  }
                }
              }
              if (atY < ySize - 1) {
                neighbour = nodes[atX][atY + 1][atZ];
                if (neighbour != null) {
                  if (neighbour.getBoundary(OneNode.RIGHT) == null) {
                    boundary =
                        new Boundary(node, neighbour, Boundary.ZX);
                    neighbour.noteBoundary(boundary, OneNode.RIGHT);
                    node.noteBoundary(boundary, OneNode.LEFT);
                    boundaries.addElement(boundary);
                    tempCntB1++;
                    tempCntBoundary++;
                  }
                }
              }

              if (atZ > 0) {
                neighbour = nodes[atX][atY][atZ - 1];
                if (neighbour != null) {
                  if (neighbour.getBoundary(OneNode.ABOVE) == null) {
                    boundary =
                        new Boundary(neighbour, node, Boundary.XY);
                    neighbour.noteBoundary(boundary, OneNode.ABOVE);
                    node.noteBoundary(boundary, OneNode.BELOW);
                    boundaries.addElement(boundary);
                    tempCntB1++;
                    tempCntBoundary++;
                  }
                }
              }

              if (atZ < zSize - 1) {
                neighbour = nodes[atX][atY][atZ + 1];
                if (neighbour != null) {
                  if (neighbour.getBoundary(OneNode.BELOW) == null) {
                    boundary =
                        new Boundary(node, neighbour, Boundary.XY);
                    neighbour.noteBoundary(boundary, OneNode.BELOW);
                    node.noteBoundary(boundary, OneNode.ABOVE);
                    boundaries.addElement(boundary);
                    tempCntB1++;
                    tempCntBoundary++;
                  }
                }
              }
            }
          }
        }
      }
    }
*/

    boolean checkIndexes(int x, int y, int z) {
        if (x < 0 || x > (xSize - 1) ||
                y < 0 || y > (ySize - 1) ||
                z < 0 || z > (zSize - 1)) {
            errMessage("Out of range (x = " + x +
                    " (y = " + y +
                    " (z = " + z);
            return false;
        }
        return true;
    }


    public void setTemperatureAt(double temperature,
                                 int x, int y, int z) {
        if (checkIndexes(x, y, z)) {
            nodes[x][y][z].setTemperature(temperature);
        }
    }

    public void setHeatTrCoeff(double heatTrCoeff,
                               int x, int y, int z) {
        if (checkIndexes(x, y, z)) {
            nodes[x][y][z].setHeatTfCoeff(heatTrCoeff);
        }
    }

//  void resetBoundaries() {
//    if (boundariesArray == null)
//      boundariesArray = (Boundary[])boundaries.toArray(new Boundary[0]);
//    int size = boundariesArray.length;
//    for (int n = 0; n < size; n++) {
//      boundariesArray[n].reset();
//    }
//  }

    void calculateBoundaries(double deltTime) {
        if (boundariesArray == null)
            boundariesArray = (Boundary[]) boundaries.toArray(new Boundary[0]);

        int size = boundariesArray.length;
// DEBUG
//    debug("boundarArray Length = " + size);
        for (int n = 0; n < size; n++) {
            boundariesArray[n].update(deltTime);
        }
        boundariesDirty = false;
    }

    boolean updateNodes(double deltaTime) {
        if (boundariesDirty) {
            errMessage("boundaries NOT updated!");
            return false;
        } else {
            for (int x = 1; x < (xSize - 1); x++) {
                for (int y = 1; y < (ySize - 1); y++) {
                    for (int z = 1; z < (zSize - 1); z++) {
                        nodes[x][y][z].update(deltaTime);
                    }
                }
            }
            boundariesDirty = false; // ????
            // swap new temp to now
            for (int x = 1; x < (xSize - 1); x++) {
                for (int y = 1; y < (ySize - 1); y++) {
                    for (int z = 1; z < (zSize - 1); z++) {
                        nodes[x][y][z].resetTemperature();
                    }
                }
            }
        }
        return true;
    }

    public boolean update(double deltaTime) {
//    resetBoundaries();
        calculateBoundaries(deltaTime);
        return updateNodes(deltaTime);
    }

    void displayXYplane(int atZ) {
        if (atZ >= 0 && atZ < zSize) {
            debug("XY plane at z = " + atZ);
            String oneRow = "";
            for (int x = 0; x < xSize; x++)
                oneRow += "\t" + x;
            debug(oneRow);
            for (int y = 0; y < ySize; y++) {
                oneRow = "" + y;
                for (int x = 0; x < xSize; x++) {
                    oneRow +=
                            "\t" + new Double(nodes[x][y][atZ].getTemperature()).intValue();
                }
                debug(oneRow);
            }
        }
    }

    public int getXsize() {
        return xSize;
    }

    public int getYsize() {
        return ySize;
    }

    public int getZsize() {
        return zSize;
    }

    public double getDataAt(int x, int y, int z) {
        double retVal;
        try {
            retVal = nodes[x][y][z].getTemperature();
        } catch (Exception aoe) {
            errMessage("getDataAt: " + aoe);
            errMessage("Call from ThreeDArray, x = " + x + ", y = " + y + ",z = " + z);
            retVal = -1.0;
        }
        return retVal;
    }


    public boolean writeToDataFile(String fileName) {
        int x = -1, y = -1, z = -1;
        try {
            OutputStream out =
                    new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeInt(xSize);
            oos.writeInt(ySize);
            oos.writeInt(zSize);
            for (x = 0; x < xSize; x++)
                for (y = 0; y < ySize; y++)
                    for (z = 0; z < zSize; z++)
                        oos.writeDouble(nodes[x][y][z].getTemperature());
            oos.flush();
            out.close();
        } catch (IOException ioe) {
            errMessage("writeToDataFile: " + ioe);
            return false;
        }
        return true;

    }

    public double[][][] getDataArray() {
        double[][][] dataArray = new double[xSize][ySize][zSize];
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    dataArray[x][y][z] =
                            nodes[x][y][z].getTemperature();
                }
            }
        }
        return dataArray;
    }


    public double[][][] getDataArray(String fileName) {
        double[][][] tempData = null;
        int x = -1, y = -1, z = -1;
        try {
            InputStream in =
                    new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(in);
            int xMax = ois.readInt();
            int yMax = ois.readInt();
            int zMax = ois.readInt();
            tempData = new double[xMax][yMax][zMax];
            for (x = 0; x < xMax; x++)
                for (y = 0; y < yMax; y++)
                    for (z = 0; z < zMax; z++)
                        tempData[x][y][z] = ois.readDouble();
            in.close();
        } catch (IOException ioe) {
            errMessage("getDataArray: " + ioe + " at " + x + ", " + y + ", " + z);
            ioe.printStackTrace();
            return null;
        }
        return tempData;
    }

    public boolean writeArrayToFile(String fileName, boolean append) {
        try {
            OutputStream out =
                    new FileOutputStream(fileName, append);
            for (int z = 0; z < zSize; z++) {
                writeOneXYplaneToFile(out, z);
            }
            for (int y = 0; y < ySize; y++) {
                writeOneXZplaneToFile(out, y);
            }
            for (int x = 0; x < xSize; x++) {
                writeOneYZplaneToFile(out, x);
            }
            out.close();
            return true;
        } catch (IOException ioe) {
            errMessage("writeTempProfileFile: " + ioe);
            return false;
        }
    }


    void writeOneXYplaneToFile(String fileName, int atZ, boolean append) {
        try {
            OutputStream out =
                    new FileOutputStream(fileName, append);
            writeOneXYplaneToFile(out, atZ);
            out.close();
        } catch (IOException ioe) {
            errMessage("writeOneXYplaneToFile: " + ioe);
        }
    }

    void writeOneXZplaneToFile(String fileName, int atY, boolean append) {
        try {
            OutputStream out =
                    new FileOutputStream(fileName, append);
            writeOneXZplaneToFile(out, atY);
            out.close();
        } catch (IOException ioe) {
            errMessage("writeOneXZplaneToFile: " + ioe);
        }
    }

    void writeOneYZplaneToFile(String fileName, int atX, boolean append) {
        try {
            OutputStream out =
                    new FileOutputStream(fileName, append);
            writeOneYZplaneToFile(out, atX);
            out.close();
        } catch (IOException ioe) {
            errMessage("writeOneYZplaneToFile: " + ioe);
        }
    }

    DecimalFormat formatTemp = new DecimalFormat(" 0000");
    DecimalFormat formatPos = new DecimalFormat("  000");

    void writeOneXYplaneToFile(OutputStream out, int atZ) {
        PrintWriter writer = new PrintWriter(out);
        if (atZ >= 0 && atZ < zSize) {
            writer.println("XY plane at z = " + atZ);
            String oneRow = "     ";
            for (int x = 0; x < xSize; x++)
                oneRow += formatPos.format(x);
            writer.println(oneRow);
            for (int y = ySize - 1; y >= 0; y--) {
                oneRow = formatPos.format(y);
                for (int x = 0; x < xSize; x++) {
                    oneRow +=
                            formatTemp.format(new Double(nodes[x][y][atZ].getTemperature()).intValue());
                }
                writer.println(oneRow);
                writer.flush();
            }
        }
    }

    void writeOneXZplaneToFile(OutputStream out, int atY) {
        PrintWriter writer = new PrintWriter(out);
        if (atY >= 0 && atY < ySize) {
            writer.println("XZ plane at y = " + atY);
            String oneRow = "     ";
            for (int x = 0; x < xSize; x++)
                oneRow += formatPos.format(x);
            writer.println(oneRow);
            for (int z = zSize - 1; z >= 0; z--) {
                oneRow = formatPos.format(z);
                for (int x = 0; x < xSize; x++) {
                    oneRow +=
                            formatTemp.format(new Double(nodes[x][atY][z].getTemperature()).intValue());
                }
                writer.println(oneRow);
                writer.flush();
            }
        }
    }

    void writeOneYZplaneToFile(OutputStream out, int atX) {
        PrintWriter writer = new PrintWriter(out);
        if (atX >= 0 && atX < xSize) {
            writer.println("YZ plane at x = " + atX);
            String oneRow = "     ";
            for (int y = ySize - 1; y >= 0; y--)
                oneRow += formatPos.format(y);
            writer.println(oneRow);
            for (int z = zSize - 1; z >= 0; z--) {
                oneRow = formatPos.format(z);
                for (int y = ySize - 1; y >= 0; y--) {
                    oneRow +=
                            formatTemp.format(new Double(nodes[atX][y][z].getTemperature()).intValue());
                }
                writer.println(oneRow);
                writer.flush();
            }
        }
    }

    double[][][] getNodeTemperatures() {
        double[][][] theCopy = new double[xSize][ySize][zSize];
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                for (int z = 0; z < zSize; z++) {
                    theCopy[x][y][z] = nodes[x][y][z].getTemperature();
                }
            }
        }
        return theCopy;
    }

    Vector<double[][]> getAllSurfaceTemperatures() {
        Vector<double[][]> allTemps = new Vector<>();
        allTemps.add(getSurfaceTemperatures(OneNode.BELOW));
        allTemps.add(getSurfaceTemperatures(OneNode.LEFT));
        allTemps.add(getSurfaceTemperatures(OneNode.ABOVE));
        allTemps.add(getSurfaceTemperatures(OneNode.RIGHT));
        allTemps.add(getSurfaceTemperatures(OneNode.FRONT));
        allTemps.add(getSurfaceTemperatures(OneNode.BACK));
        return allTemps;
    }

    // surface is one of OneNode.ABOVE, BELOW etc.
    double[][] getSurfaceTemperaturesOLD(int direction) {
        double[][] arr = null;
        switch (direction) {
            case OneNode.BELOW: // bottom surface
                arr = new double[xSize][ySize];
                for (int x = 1; x < xSize - 1; x++) {
                    for (int y = 1; y < ySize - 1; y++) {
                        arr[x][y] = nodes[x][y][1].getBoundary(direction).getTemperature();
                    }
                }
                break;
            case OneNode.LEFT: // vertical left
                arr = new double[zSize][xSize];
                for (int z = 1; z < zSize - 1; z++) {
                    for (int x = 1; x < xSize - 1; x++) {
                        arr[z][x] = nodes[x][ySize - 2][z].getBoundary(direction).getTemperature();
                    }
                }
                break;
            case OneNode.ABOVE: // Top  surface
                arr = new double[xSize][ySize];
                for (int x = 1; x < xSize - 1; x++) {
                    for (int y = 1; y < ySize - 1; y++) {
                        arr[x][y] = nodes[x][y][zSize - 2].getBoundary(direction).getTemperature();
                    }
                }
                break;
            case OneNode.RIGHT: // vertical right
                arr = new double[zSize][xSize];
                for (int z = 1; z < zSize - 1; z++) {
                    for (int x = 1; x < xSize - 1; x++) {
                        arr[z][x] = nodes[x][1][z].getBoundary(direction).getTemperature();
                    }
                }
                break;
            case OneNode.FRONT: // near end
                arr = new double[ySize][zSize];
                for (int y = 1; y < ySize - 1; y++) {
                    for (int z = 1; z < zSize - 1; z++) {
                        arr[y][z] = nodes[1][y][z].getBoundary(direction).getTemperature();
                    }
                }
                break;
            case OneNode.BACK: // near end
                arr = new double[ySize][zSize];
                for (int y = 1; y < ySize - 1; y++) {
                    for (int z = 1; z < zSize - 1; z++) {
                        arr[y][z] = nodes[xSize - 2][y][z].getBoundary(direction).getTemperature();
                    }
                }
                break;
        }
        return arr;
    }

    double[][] getSurfaceTemperatures(int direction) {
        // all directions looking into furnace from charging end
        double[][] arr = null;
        switch (direction) {
            case OneNode.BELOW: // bottom surface
                arr = new double[xSize][ySize];
                for (int x = 1; x < xSize - 1; x++) {
                    for (int y = 1; y < ySize - 1; y++) {
                        arr[x][y] = nodes[x][y][1].getBoundary(direction).getTemperature();
                    }
                }
                break;
            case OneNode.BACK: // Vertical Back surface
                arr = new double[zSize][xSize];
                for (int z = 1; z < zSize - 1; z++) {
                    for (int x = 1; x < xSize - 1; x++) {
                        arr[z][x] = nodes[x][ySize - 2][z].getBoundary(direction).getTemperature();
                    }
                }
                break;

            case OneNode.ABOVE: // Top  surface
                arr = new double[xSize][ySize];
                for (int x = 1; x < xSize - 1; x++) {
                    for (int y = 1; y < ySize - 1; y++) {
                        arr[x][y] = nodes[x][y][zSize - 2].getBoundary(direction).getTemperature();
                    }
                }
                break;
            case OneNode.FRONT: // vertical Front surface
                arr = new double[zSize][xSize];
                for (int z = 1; z < zSize - 1; z++) {
                    for (int x = 1; x < xSize - 1; x++) {
                        arr[z][x] = nodes[x][1][z].getBoundary(direction).getTemperature();
                    }
                }
                break;
            case OneNode.LEFT: // Left end surface
                arr = new double[ySize][zSize];
                for (int y = 1; y < ySize - 1; y++) {
                    for (int z = 1; z < zSize - 1; z++) {
                        arr[y][z] = nodes[1][y][z].getBoundary(direction).getTemperature();
                    }
                }
                break;
            case OneNode.RIGHT: // right end surface
                arr = new double[ySize][zSize];
                for (int y = 1; y < ySize - 1; y++) {
                    for (int z = 1; z < zSize - 1; z++) {
                        arr[y][z] = nodes[xSize - 2][y][z].getBoundary(direction).getTemperature();
                    }
                }
                break;
        }
        return arr;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(tempCntBoundary);
        out.writeInt(tempCntNode);
        out.writeInt(xSize);
        out.writeInt(ySize);
        out.writeInt(zSize);
        debug("nodes NOT saved");
//    OneNode nodes[][][];
        debug("boundaries NOT saved");
//    Vector boundaries = new Vector();
        debug("boundaryArray NOT saved");
//    Boundary[] boundariesArray = null;
//    debug("Trying to save ThreeDArray");
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        tempCntBoundary = in.readInt();
        tempCntNode = in.readInt();
        xSize = in.readInt();
        ySize = in.readInt();
        zSize = in.readInt();
        debug("nodes NOT read");
//    OneNode nodes[][][];
        debug("boundaries NOT read");
//    Vector boundaries = new Vector();
        debug("boundaryArray NOT read");
//    Boundary[] boundariesArray = null;
    }

    void errMessage(String msg) {
        JOptionPane.showMessageDialog(null, msg, "ThreeDArray",
                JOptionPane.ERROR_MESSAGE);
    }

    void debug(String msg) {
        System.out.println("ThreeDArray: " + msg);
    }

}

