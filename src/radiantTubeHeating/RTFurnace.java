package radiantTubeHeating;

import PropertyViewer.OnePropertyTrace;
import basic.Charge;
import basic.RadiantTube;
import mvUtils.display.GraphInfoAdapter;
import mvUtils.display.TraceHeader;
import mvUtils.math.DoublePoint;
import mvUtils.math.DoubleRange;
import mvUtils.mvXML.XMLmv;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/19/12
 * Time: 10:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class RTFurnace extends GraphInfoAdapter {
    public enum CalculMode {RTTEMP, RTHEAT} ;
    CalculMode iCalculMode;
    public double width;
    public double heightAbove, heightBelow;
    public double rtCenterAbove, rtCenterBelow;
    public RadiantTube rt;
    double topRTperM, botRTperM;

    public Charge charge;
    double production, unitTime;
    double maxFceLen, unitLen;
    double unitLoss;
    double uAreaChTot, uAreaHeTot;
    public double uAreaChHe, uAreaHeCh;
    double gapHeCh;
    Vector<OneSlot> allSlots;
    int maxSlots;
    public int nSlots = 0;
    MyTableModel tableModel;
    OneSlot startSlot;

    public RTFurnace(double width, double heightAbove, double rtCenterAbove, RadiantTube radiantTube, double rtPerM) {
        this.width = width;
        this.heightAbove = heightAbove;
        this.heightBelow = this.heightAbove;
        this.rtCenterAbove = rtCenterAbove;
        this.rtCenterBelow = this.rtCenterAbove;
        this.rt = radiantTube;
        this.topRTperM = rtPerM / 2;
        this.botRTperM = this.topRTperM;
    }

    public void setProduction(Charge charge, double production, double maxFceLen, double unitLen, double unitLoss, CalculMode iCalculMode) {
        this.charge = charge;
        this.production = production;
        this.unitLen = unitLen;
        this.maxFceLen = maxFceLen;
        this.unitLoss = unitLoss;
        this.iCalculMode = iCalculMode;
        uAreaChTot = charge.getWidth() * 2 * unitLen;
        uAreaChHe = uAreaChTot;
        uAreaHeTot = rt.getTotHeatingSurface() * (topRTperM + botRTperM) * unitLen;
        uAreaHeCh = uAreaHeTot / 2;
        gapHeCh = rtCenterAbove - rt.dia / 2;
        maxSlots =  (int)(maxFceLen / unitLen);
        allSlots = new Vector<OneSlot>();
        double lPos = unitLen;
        OneSlot slot;
        double endTime = 0;
        double uWt = charge.getWeight(unitLen);
        unitTime = unitLen / (production / 60 / uWt);
        OneSlot prevSlot = null;
        for (int i = 0; i < maxSlots; i++)  {
            endTime += unitTime;
            slot = new OneSlot(this, lPos, prevSlot);
            slot.setCharge(charge, uWt, unitTime, endTime);
            allSlots.add(slot);
            lPos += unitLen;
            prevSlot = slot;
        }
    }
    double chStTemp, chEndTemp, srcTemp, rtHeat;
    // Temperatures in K
    public boolean calculate(double chStTemp, double chEndTemp, double srcTemp, double rtHeat) {
        this.chStTemp = chStTemp;
        this.chEndTemp = chEndTemp;
        this.srcTemp = srcTemp;
        this.rtHeat = rtHeat;
        OneSlot slot = allSlots.get(0);
        double srcHeat = rtHeat * 860 * (topRTperM + botRTperM) * unitLen;
        double exitTemp = slot.calculate(chStTemp, srcTemp, 0, srcHeat, iCalculMode);
        nSlots = 1;
        while ((exitTemp < chEndTemp) && (nSlots < allSlots.size())) {
            slot =  allSlots.get(nSlots);
            exitTemp = slot.calculate();
            nSlots++;
        }
        startSlot = new OneSlot(allSlots.get(0), 0);
        setDataForTraces();
        return true;
    }

    public JTable getResultTable() {
        tableModel = new MyTableModel();
        JTable table = new JTable(tableModel);
        table.setDefaultRenderer(table.getColumnClass(1), new CellRenderer());
        return table;
    }

    class MyTableModel extends AbstractTableModel { //here
        public MyTableModel() {
      }

       public int getColumnCount() {
        return OneSlot.nColumn;
      }

       public int getRowCount() {
        return nSlots + 1;
      }

    public String getColumnName(int col) {
      if (col < OneSlot.nColumn)
          return OneSlot.colName[col];
      else
          return "UNKNOWN";
    }

    public Object getValueAt(int row, int col) { // was(int xPos, int yPos) {
         String data;
         OneSlot slot;

         if (row >= 0 && row <allSlots.size() + 1)  {
             if (row == 0)
                 slot = startSlot;
             else
                 slot = allSlots.get(row -1);
             data =  slot.getColData(col);
         }
         else
           data =  "NO DATA";
        return data;
     }

        public double getdoubleValueAt(int row, int col) { // was(int xPos, int yPos) {
              double data;
              OneSlot slot;

              if (row >= 0 && row <allSlots.size() + 1)  {
                  if (row == 0)
                      slot = startSlot;
                  else
                      slot = allSlots.get(row -1);
                  data =  slot.getColDataVal(col);
              }
              else
                data =  Double.NaN;
             return data;
          }
        String getResultsCVS() {
            String cvs = "";
            int c;
            int columns = OneSlot.nColumn;
            for (c = 0; c < OneSlot.nColumn; c++) {
                cvs += getColumnName(c) + ((c == (columns - 1) ? "\n" : ","));
            }
            int rows = getRowCount();
            for (int r = 0; r < rows; r++) {
                for (c = 0; c < columns; c++) {
                    cvs += ((String)getValueAt(r, c)).replace(",", "") + ((c == (columns - 1) ? "\n" : ","));
                }
            }
            return cvs;
        }

     }  // class MyTableModel


    public class CellRenderer extends DefaultTableCellRenderer {
      public Component getTableCellRendererComponent(JTable table,
          Object value,
          boolean isSelected,
          boolean hasFocus,
          int row,
          int column) {
        JLabel comp = new JLabel(value.toString());
          return comp;
      }
    }    // class CellRenderer

    public int nTraces = 3;
    String[] traceName = {"tempHe", "tempFce", "tempCh"};
    TraceHeader[] traceHeader = {new TraceHeader(traceName[0], "m", "DegC"), new TraceHeader(traceName[1], "m", "DegC"),
                            new TraceHeader(traceName[2], "m", "DegC")};
    DoublePoint[] tempHe;
    DoublePoint[] tempFce;
    DoublePoint[] tempCh;
    Vector<OnePropertyTrace> traces;
    DoubleRange commonX, commonY;

    void setDataForTraces() {
        OneSlot slot;
        tempHe = new DoublePoint[nSlots + 1];
        tempFce = new DoublePoint[nSlots + 1];
        tempCh = new DoublePoint[nSlots + 1];
        slot = startSlot;
        double pos;
        pos = slot.lPos;
        tempHe[0] = new DoublePoint(pos, slot.tempHe -273);
        tempFce[0] = new DoublePoint(pos, slot.tempFce -273);
        tempCh[0] = new DoublePoint(pos, slot.tempChEnd -273);
        for (int s = 1; s <= nSlots; s++) {
            slot = allSlots.get(s - 1);
            pos = slot.lPos;
            tempHe[s] = new DoublePoint(pos, slot.tempHe -273);
            tempFce[s] = new DoublePoint(pos, slot.tempFce -273);
            tempCh[s] = new DoublePoint(pos, slot.tempChEnd -273);
        }
        traces = new Vector<OnePropertyTrace>();
        OnePropertyTrace oneTrace;
        oneTrace =  new OnePropertyTrace(traceHeader[0], tempHe );
        oneTrace.setAutoRanges();
        traces.add(oneTrace);
        oneTrace = new OnePropertyTrace(traceHeader[1], tempFce);
        oneTrace.setAutoRanges();
        traces.add(oneTrace);
        oneTrace = new OnePropertyTrace(traceHeader[2], tempCh );
        oneTrace.setAutoRanges();
        traces.add(oneTrace);
        for (int t = 0; t < nTraces; t++)
            traces.get(t).setRanges(getCommonXrange(), getCommonYrange());
    }

    public TraceHeader[] getTraceHeader() {
      return traceHeader;
    }

    public TraceHeader getTraceHeader(int trace) {
        if (trace < nTraces)
            return traces.get(trace).getTraceHeader();
        else
            return null;
    }

    public DoubleRange getXrange(int trace) {
        if (trace < nTraces)
            return  traces.get(trace).getXrange();
        else
            return null;
    }

    public DoubleRange getYrange(int trace) {
         if (trace < nTraces)
             return  traces.get(trace).getYrange();
         else
             return null;
     }

    OnePropertyTrace getTrace(int tr) {
        if (tr < nTraces)
            return traces.get(tr);
        else
            return null;
    }

    public DoubleRange getCommonXrange() {
        OnePropertyTrace tr;
        if (commonX == null) {
            double  mn = Double.MAX_VALUE;
            double mx = Double.MIN_VALUE;
            double x;
            for (int i = 0; i < nTraces; i++) {
                tr =  getTrace(i);
                if (tr.length > 1) {
                    x = tr.getXmin();
                    mn = (x < mn) ? x : mn;
                    x = tr.getXmax();
                    mx = (x > mx) ? x : mx;
                }
            }
            commonX =  OnePropertyTrace.getAutoRange(0, mx, true, false);  // formces minimum to 0
        }
        return commonX;
    }

    public DoubleRange getCommonYrange() {
        OnePropertyTrace tr;
        if (commonY == null) {
            double  mn = Double.MAX_VALUE;
            double mx = Double.MIN_VALUE;
            double y;
            for (int i = 0; i < nTraces; i++) {
                tr = getTrace(i);
                if (tr.length > 1)  {
                    y = tr.getYmin();
                    mn = (y < mn) ? y : mn;
                    y = tr.getYmax();
                    mx = (y > mx) ? y : mx;
                }
            }
            commonY =  OnePropertyTrace.getAutoRange(0, mx, true, false);  // formces minimum to 0
        }
        return commonY;
    }



    public double getYat(int trace, double x) {
         if (trace < nTraces) {
             return  traces.get(trace).getYat(x);
         } else {
             return Double.NaN;
         }
     }

    public DoublePoint[] getGraph(int trace) {
         if (trace < nTraces) {
             return  traces.get(trace).getGraph();
         } else {
             return null;
         }
     }

    public String resultsInCVS() {
        return "\n\n\n" + tableModel.getResultsCVS();
    }

    public String fceDataToSave() {
        String retVal;
        retVal = "<furnace>" + XMLmv.putTag("width", "" + width * 1000);
        retVal += XMLmv.putTag("heightAbove", "" + heightAbove * 1000);
        retVal += XMLmv.putTag("maxFceLen", "" + maxFceLen * 1000);
        retVal += XMLmv.putTag("perMLoss", "" + unitLoss / unitTime);
        retVal += "</furnace>" + "\n";
        return retVal;
    }
//chStTemp, double chEndTemp, double srcTemp, double rtHeat
    public String prodDataToSave() {
        String retVal;
        retVal = "<production>" + XMLmv.putTag("output", "" + production);
        retVal += XMLmv.putTag("chStTemp", "" + (chStTemp - 273));
        retVal += XMLmv.putTag("chEndTemp", "" + (chEndTemp - 273));
        retVal += XMLmv.putTag("rtLimitTemp", "" + (srcTemp - 273));
        retVal += XMLmv.putTag("rtLimitHeat", "" + rtHeat);
        retVal += "</production>" + "\n";
        return retVal;
    }
}
