package PropertyViewer;

import mvUtils.display.GraphInfoAdapter;
import mvUtils.display.TraceHeader;
import mvUtils.math.DoublePoint;
import mvUtils.math.DoubleRange;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.text.DecimalFormat;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 3/18/12
 * Time: 12:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class PropertyTraces extends GraphInfoAdapter {
    Vector<OnePropertyTrace> traces;
    int nTraces = 0;
    DoubleRange commonX, commonY;
    public PropertyTraces() {
        traces = new Vector<OnePropertyTrace>();
        nTraces = 0;
    }

    OnePropertyTrace getTrace(int tr) {
        if (tr < nTraces)
            return (OnePropertyTrace)traces.get(tr);
        else
            return null;
    }

    public int addTrace(OnePropertyTrace oneT) {
        traces.addElement(oneT);
//        traces.add(oneT);
        nTraces = traces.size();
        return nTraces;
    }

    public TraceHeader getTraceHeader(int trace) {
        if (trace < nTraces)
            return ((OnePropertyTrace)traces.get(trace)).getTraceHeader();
        else
            return null;
    }

    public DoubleRange getXrange(int trace) {
        if (trace < nTraces) {
            return  ((OnePropertyTrace)traces.get(trace)).getXrange();
        } else {
            return null;
        }
    }

    public  DoubleRange getYrange(int trace) {
        if (trace < nTraces) {
            return  ((OnePropertyTrace)traces.get(trace)).getYrange();
        } else {
            return null;
        }
    }

    public double getYat(int trace, double x) {
        if (trace < nTraces) {
            return  ((OnePropertyTrace)traces.get(trace)).getYat(x);
        } else {
            return Double.NaN;
        }
    }

    public DoublePoint[] getGraph(int trace) {
        if (trace < nTraces) {
            return  ((OnePropertyTrace)traces.get(trace)).getGraph();
        } else {
            return null;
        }
    }

    MyTableModel tableModel;

    public JTable getResultTable() {
        tableModel =  new MyTableModel(0);
        JTable table = new JTable(tableModel);
        table.setDefaultRenderer(table.getColumnClass(1), new CellRenderer());
        return table;
    }

    public OnePropertyTrace getOneTrace(int trace) {
        if (trace < nTraces) {
            return  (OnePropertyTrace)traces.get(trace);
        } else {
            return null;
        }
    }

    public void setTraceForTable(int tr) {
        if (tr >=0 && tr <nTraces)
            tableModel.setTrace(tr);
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
            commonX =  OnePropertyTrace.getAutoRange(mn, mx);
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
            commonY =  OnePropertyTrace.getAutoRange(mn, mx);
        }
        return commonY;
    }

    class MyTableModel extends AbstractTableModel { //here
      OnePropertyTrace oneT;
      int theTrace;
      int nColumns = 2;
      public MyTableModel(int tr) {
          setTrace(tr);
      }

      void setTrace(int tr) {
          oneT = getTrace(tr);
         theTrace = tr;
      }

      public int getColumnCount() {
        return nColumns;
      }

      public String getColumnName(int col) {
        if (col == 0)
            return oneT.getPropertyXname();
        else
            return oneT.getPropertyYname();
      }

      public int getRowCount() {
        return oneT.length;
      }

      public Object getValueAt(int row, int col) { // was(int xPos, int yPos) {
        double data = 0;
        switch (col) {
          case 0:
            data = oneT.dataArr.getXat(row);
            break;
          case 1:
            data = oneT.dataArr.getYat(row);
            break;
        }
        return formatNumber(data);
      }

      String formatNumber(double value) {
        String retVal = null;
        double absVal = Math.abs(value);
        if (absVal == 0)
          retVal = "#";
        else if (absVal < 0.001 || absVal > 1e5)
          retVal = "#.###E00";
        else if (absVal > 100)
          retVal = "###,###";
        else
          retVal = "###.###";
        return new DecimalFormat(retVal).format(value);


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
//
          //switch (column) {
          //case 1:
          //if (row == locMaxAbsSF) {
          //comp.setForeground(Color.RED);
          //}
          //break;
          //case 2:
          //if (row == locMaxAbsBM) {
          //comp.setForeground(Color.RED);
          //}
          //break;
          //case 4:
          //if (row == locMaxAbsDEF) {
          //comp.setForeground(Color.RED);
          //}
          //break;
          //}
          return comp;
      }
    }    // class CellRenderer

}
