package basic;

import javax.swing.*;
import javax.vecmath.*;
import java.util.*;
import java.io.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2005
 * Company:
 *
 * @author
 * @version 1.0
 */

public class TemperatureStats {
    public ThreeDCharge charge;
    Hashtable pointData = new Hashtable(); //PointInCharge with int ID
    TwoDTable table;
    int maxTableLen;
    public int actualLen;
    int cntDataPoints = 0;
    boolean tableActive = false;
    double[] dataSet;
    Vector<DataChangeListener> dataChangeListeners = new Vector<DataChangeListener>();
    static int MAXTIMEPOINTS = 300;
    double[] timePoints = new double[MAXTIMEPOINTS];
    int validTimePoints = 0;
    double[][][] latestTempdata = null;
    double[] latestHeatdata = null;
    Vector<double[][][]> tempHistory = new Vector<double[][][]>(); // double[][][]
    Vector <double[]> heatHistory = new Vector<double[]>(); // double[] of the surface heats in the interval
    Vector<Point3i> tempPoints = new Vector<Point3i>(); // Point3i; // THIS MAY HAVE TO BE REMOVED
    Vector<Vector<double[][]>> borderHistory = new Vector<>();
            // in the order of directions OneNode.BELOW, LEFT, ABOVE, RIGHT, FRONT, BACK
    // if from saved file
    public String filePath = null;
    public boolean oK = false;

    public TemperatureStats(ThreeDCharge ch) {
        charge = ch;
    }

    public TemperatureStats(String filepath) {
        oK = readFromFile(filepath);
        this.filePath = filepath;
    }

    public TemperatureStats(ThreeDCharge ch, double[] timePoints,
                            Vector <double[][][]>tempHistory, Vector <double[]>heatHistory) {
        init(ch, timePoints, tempHistory, heatHistory);
//    charge = ch;
//    this.timePoints = timePoints;
//    validTimePoints = timePoints.length;
//    this.tempHistory = tempHistory;
//    this.heatHistory = heatHistory;
//    latestTempdata = (double[][][])tempHistory.lastElement();
//    latestHeatdata = (double[])heatHistory.lastElement();
    }

    void init(ThreeDCharge ch, double[] timePoints,
              Vector <double[][][]> tempHistory, Vector<double[]> heatHistory) {
        charge = ch;
        this.timePoints = timePoints;
//                  validTimePoints = timePoints.length;
        this.tempHistory = tempHistory;
        this.heatHistory = heatHistory;
        latestTempdata = (double[][][]) tempHistory.lastElement();
        latestHeatdata = (double[]) heatHistory.lastElement();

    }

    public boolean addPoint(Integer id, double l, double w, double h) {
        Point3i theValidPoint = charge.getCell(l, w, h);
        if (theValidPoint != null) {
            tempPoints.add(theValidPoint);
            return true;
        }
        return false;
//
//    if (tableActive) {
//      errMessage("Collection table is Frozen");
//      return false;
//    }
//    PointInCharge point;
//
//    try {
//      point = new PointInCharge(charge, id, l, w, h);
//    }
//    catch (Exception e) {
//      return false;
//    }
//    pointData.put(id, point);
//    cntDataPoints++;
//    return true;
    }

    public boolean addPoint(PointInCharge pt) {
        addPoint(pt.id, pt.desiredLocL, pt.desiredLocW, pt.desiredLocH);
        return true;

//    if (tableActive) {
//      errMessage("Collection table is Frozen");
//      return false;
//    }
//    if (pt.theCharge == charge)  {
//      pointData.put(pt.id, pt);
//      cntDataPoints++;
//      return true;
//    }
//    return false;
    }

    public boolean setCollectionLength(int len) {
        if (cntDataPoints <= 0) {
            errMessage("No data Points");
            return false;
        }

        Enumeration en = pointData.keys();
        double[] colHead = new double[cntDataPoints];
        int n = 0;
        while (en.hasMoreElements()) {
            colHead[n] = ((Integer) en.nextElement()).intValue();
            n++;
        }
        table = new TwoDTable(colHead, len, true, true);
        dataSet = new double[cntDataPoints];
        tableActive = true;
        return true;
    }

    public PointInCharge[] getPointList() {
        if (cntDataPoints <= 0) {
            errMessage("No data Points");
            return null;
        }
        PointInCharge[] list = new PointInCharge[pointData.size()];
        Enumeration en = pointData.elements();
        int n = 0;
        while (en.hasMoreElements()) {
            list[n] = (PointInCharge) en.nextElement();
            n++;
        }
        return list;

    }

    void update(double time) {
        Enumeration en = pointData.elements();
        PointInCharge thePoint = null;
        int n = 0;
        while (en.hasMoreElements()) {
            thePoint = (PointInCharge) en.nextElement();
            // hash table takes out data in rev order of put
            thePoint.updatePoint();
            if (tableActive)
                dataSet[n] = thePoint.temperature;
            n++;
        }
        if (tableActive) {
            table.addRow(time, dataSet);
            int listeners;
            if ((listeners = dataChangeListeners.size()) > 0) {
                for (int cnt = 0; cnt < listeners; cnt++)
                    ((DataChangeListener) dataChangeListeners.get(cnt)).dataAdded(actualLen);
            }
            actualLen++;
        }
        if (validTimePoints < MAXTIMEPOINTS) {
            timePoints[validTimePoints] = time;
            validTimePoints++;
            tempHistory.add(charge.getNodeTemperatures());
            borderHistory.add(charge.getAllSurfaceTemperatures());
            latestTempdata = (double[][][]) tempHistory.lastElement();
            heatHistory.add(charge.getSurfaceHeatList());
            latestHeatdata = (double[]) heatHistory.lastElement();
        } else {
            errMessage("Time points exceeded limit of " + validTimePoints);
        }
    }

    void informDataChange() {
        int listeners;
        if ((listeners = dataChangeListeners.size()) > 0) {
            for (int cnt = 0; cnt < listeners; cnt++)
                try {
                    ((DataChangeListener) dataChangeListeners.get(cnt)).dataChanged();
                } catch (Exception ex) {
                }
        }
    }

    public double lastTimePoint() {
        if (validTimePoints > 0)
            return timePoints[validTimePoints - 1];
        else
            return -1;
    }

    public double getTemperatureDataAt(double time, int x, int y, int z) {
        double retVal = 0;
        double[][][] tempArray;
        if (time > lastTimePoint()) time = lastTimePoint();
        if (time >= 0) {
            try {
                tempArray = (double[][][]) tempHistory.get(getTimeLoc(time));
                retVal = tempArray[x][y][z];
//        tempArray = null;

            } catch (Exception e) {
                debug("Error in getTemperatureAt()" + e + ", time = " + time + ", x = " + x + ", y = " + y + ", z = " + z);
                maxTableLen = maxTableLen;
            }
            return retVal;
        } else
            return -1;
    }

    public double getTemperatureDataAtNEW(double time, int x, int y, int z) {
        double retVal = 0;
        int timeLoc;
        double[][][] tempArray;
        Vector<double[][]> borderArr;
        if (time > lastTimePoint()) time = lastTimePoint();
        if (time >= 0) {
            try {
                timeLoc = getTimeLoc(time);
                borderArr = borderHistory.get(timeLoc);
                tempArray = (double[][][]) tempHistory.get(timeLoc);
                if (x == 0)
                    retVal = borderArr.get(4)[y][z];
                else if (x == getXsize() - 1)
                    retVal = borderArr.get(5)[y][z];
                else if (y == 0)
                    retVal = borderArr.get(3)[z][x];
                else if (y == getYsize() - 1)
                    retVal = borderArr.get(1)[z][x];
                else if (z == 0)
                    retVal = borderArr.get(0)[x][y];
                else if (z == getZsize() - 1)
                    retVal = borderArr.get(2)[x][y];
                else
                    retVal = tempArray[x][y][z];
//        tempArray = null;

            } catch (Exception e) {
                debug("Error in getTemperatureAt()" + e + ", time = " + time + ", x = " + x + ", y = " + y + ", z = " + z);
                maxTableLen = maxTableLen;
            }
            return retVal;
        } else
            return -1;
    }

    public double getTemperatureDataAt(int x, int y, int z) {
//    if (latestTempdata != null)
//      return latestTempdata[x][y][z];
//    else
//      return 0;
        return getTemperatureDataAt(lastTimePoint(), x, y, z);
    }

    public double[] getSurfHeatDataAt(double time) {
        if (time > lastTimePoint()) time = lastTimePoint();
        if (time >= 0) {
            double[] heatArray = (double[]) heatHistory.get(getTimeLoc(time));
            return heatArray;
        } else
            return null;
    }

    public double getTotalSurfHeat(boolean fromTop) {
        return getTotalSurfHeat(0, lastTimePoint(), fromTop, false);
    }

    public double getTotalSurfHeat(boolean fromTop, boolean skidOnly) {
        return getTotalSurfHeat(0, lastTimePoint(), fromTop, skidOnly);
    }

    public double getTotalSurfHeat(double fromTime, double toTime, boolean fromTop) {
        return getTotalSurfHeat(fromTime, toTime, fromTop, false);
    }

    public double getTotalSurfHeat(double fromTime, double toTime, boolean fromTop,
                                   boolean skidOnly) {
        double retVal = 0;
        double[] theHeatList = null;
        for (int t = getTimeLoc(fromTime); t <= getTimeLoc(toTime); t++) {
            theHeatList = (double[]) heatHistory.get(t);
            retVal += charge.apportionHeatIn(fromTop, skidOnly, theHeatList);
        }
        return retVal;
    }

    public int getXsize() {
        return charge.getXsize();
    }

    public int getYsize() {
        return charge.getYsize();
    }

    public int getZsize() {
        return charge.getZsize();
    }

    public Point2d[] tempAlongLengthAt(double time, double wLoc, double hLoc) {
        Point3i point = charge.getCell(0, wLoc, hLoc);
        return tempAlongLengthAt(time, point.y, point.z);
    }

    public Point2d[] tempAlongLengthAt(double time, int y, int z) {
        int xSize = charge.xSize;
        double slice = charge.unitSide;
        double[][][] tempArray = (double[][][]) tempHistory.get(getTimeLoc(time));
        Point2d[] retVal = new Point2d[xSize - 2];
        double sliceby2 = slice / 2;
        for (int x = 1; x < xSize - 1; x++) {
            retVal[x - 1] = new Point2d((slice * x - sliceby2), tempArray[x][y][z]);
        }
        return retVal;
    }

    public Point2d[] tempHistoryAt(double lLoc, double wLoc, double hLoc) {
        Point3i point = charge.getCell(lLoc, wLoc, hLoc);
        return tempHistoryAt(point.x, point.y, point.z);
    }

    public Point2d[] tempHistoryAt(int x, int y, int z) {
        double[][][] dataAtOneTime;
        double time;
        Point2d[] retVal = new Point2d[validTimePoints];
        for (int i = 0; i < validTimePoints; i++) {
            time = timePoints[i];
            dataAtOneTime = (double[][][]) tempHistory.get(getTimeLoc(time));
            retVal[i] = new Point2d(time, dataAtOneTime[x][y][z]);
        }
        return retVal;
    }

    public Point2d[] tempHistoryAt(Point3i point) {
        return tempHistoryAt(point.x, point.y, point.z);
    }

    public Point2d[] tempHistoryAt(int ptNum) {
        Point3i point = (Point3i) tempPoints.get(ptNum);
        return tempHistoryAt(point);
    }

    int getTimeLoc(double time) {
        int retVal = -1;
        for (int i = validTimePoints - 1; i >= 0; i--) {
            if (timePoints[i] <= time) { // if time too large you get the last data
                retVal = i;
                break;
            }
        }
        return retVal;
    }

    double[] getTimePointList() {
        double[] tPoints = new double[validTimePoints];
        for (int i = 0; i < validTimePoints; i++)
            tPoints[i] = timePoints[i];
        return tPoints;
    }

    public int getPointCount() {
//    return cntDataPoints;
        return tempPoints.size();
    }

    double getTemperatureAt(Integer id) {
        PointInCharge thePoint = (PointInCharge) pointData.get(id);
        if (thePoint != null)
            return thePoint.temperature;
        else
            return Double.NaN;
    }

    public double[][] getOneTrend(int id) {
        if (id >= 0 && id < cntDataPoints)
            return table.getOneColWithRowHead(id);
        else
            return null;
    }

    public String getOneSet(int loc) {
        return table.getOneRow(loc, "00.000", "0000.0");
    }

    public double[] getOneSet(int loc, double[] data) {
        return table.getOneRow(loc, data);
    }

    public String getColumnHeader() {
        if (table != null)
            return ("TempAt: " + table.getColumnHeader("0000.0"));
        else
            return "Points Not Set";
    }

    public boolean saveHistory(String filePath) {
        try {
            OutputStream out =
                    new FileOutputStream(filePath);
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeInt(100);
            // DEBUG
            debug("Writing the charge");
            oos.writeObject(charge);

            oos.writeInt(validTimePoints);
            // DEBUG
            debug("Writing Time Points");
            for (int t = 0; t < validTimePoints; t++)
                oos.writeDouble(timePoints[t]);
            oos.writeInt(1000);
            // DEBUG
            debug("Writing TempHistory");
            oos.writeObject(tempHistory);
            oos.writeInt(2000);
            // DEBUG
            debug("Writing HeatHistory");
            oos.writeObject(heatHistory);
            // DEBUG

            debug("Flushing oos");
            oos.flush();
            out.close();
        } catch (IOException ioe) {
            errMessage("saveHistory: " + ioe);
            return false;
        }
        return true;
    }

    boolean readFromFile(String filePath) {
        boolean allOK = true;
        ThreeDCharge newCharge = null;
        double[] newTimePoints = new double[MAXTIMEPOINTS];
        Vector <double[][][]> newTempHistory = null;
        Vector <double[]> newHeatHistory = null;
        int dataPos = 100;
        try {
            InputStream in =
                    new FileInputStream(filePath);
            ObjectInputStream ois = new ObjectInputStream(in);
            do {
                try {
                    if (ois.readInt() == dataPos)
                        newCharge = (ThreeDCharge) ois.readObject();
                    else {
                        allOK = false;
                        break;
                    }
                    validTimePoints = ois.readInt();
                    debug("validTimepoints = " + validTimePoints);
                    for (int t = 0; t < validTimePoints; t++)
                        newTimePoints[t] = ois.readDouble();
                    dataPos = 1000;
                    if (ois.readInt() == dataPos) {
                        newTempHistory = (Vector<double[][][]>) ois.readObject();
                    } else {
                        allOK = false;
                        break;
                    }
                    dataPos = 2000;
                    if (ois.readInt() == dataPos) {
                        newHeatHistory = (Vector<double[]>) ois.readObject();
                    } else
                        allOK = false;
                    break;
                } catch (Exception e) {
                    errMessage("Improper Data in file:" + filePath);
                }
            } while (allOK);
            if (!allOK)
                errMessage("(" + dataPos + ") Data Error in file: " + filePath);
            in.close();
        } catch (IOException ioe) {
            errMessage("saveHistory: " + ioe);
            allOK = false;
//      return null;
        }
        if (allOK) {
            init(newCharge, newTimePoints, newTempHistory,
                    newHeatHistory);
        }
        return allOK;
    }

    /*
      public TemperatureStats readHistory(String filePath) {
        boolean allOK = true;
        ThreeDCharge newCharge = null;
        double[] newTimePoints = new double[MAXTIMEPOINTS];
        Vector newTempHistory = null;
        Vector newHeatHistory = null;
        int dataPos = 100;
        try {
          InputStream in =
            new FileInputStream(filePath);
          ObjectInputStream ois = new ObjectInputStream(in);
          do {
            try {
              if (ois.readInt() == dataPos)
                newCharge = (ThreeDCharge) ois.readObject();
              else {
                allOK = false;
                break;
              }
              int validP = ois.readInt();
              for (int t = 0; t < validP; t++)
                newTimePoints[t] = ois.readDouble();
              dataPos = 1000;
              if (ois.readInt() == dataPos) {
                newTempHistory = (Vector) ois.readObject();
              }
              else {
                allOK = false;
                break;
              }
              dataPos = 2000;
              if (ois.readInt() == dataPos) {
                newHeatHistory = (Vector) ois.readObject();
              }
              else
                allOK = false;
              break;
            }
            catch (Exception e) {
              errMessage("Improper Data in file:" + filePath);
            }
          } while (allOK);
          if (!allOK)
            errMessage("(" + dataPos + ") Data Error in file: " + filePath);
          in.close();
        }
        catch (IOException ioe) {
          errMessage("saveHistory: " + ioe);
          return null;
        }
        if (allOK)
          informDataChange();
        return new TemperatureStats(newCharge, newTimePoints, newTempHistory,
                                    newHeatHistory);
      }
    */
    public void addDataChangeListener(DataChangeListener listener) {
        dataChangeListeners.add(listener);
    }

    void errMessage(String msg) {
        JOptionPane.showMessageDialog(null, msg, "TemperatureStats",
                JOptionPane.ERROR_MESSAGE);
    }

    void debug(String msg) {
        System.out.println("TemperatureStats: " + msg);
    }

}

