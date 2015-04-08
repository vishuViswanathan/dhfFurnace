package mvmath;

import mvUtils.math.MultiColData;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 8/1/12
 * Time: 12:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class MultiColDataPoint {
    MultiColData multiColData;
    int col, row;
    public double val;

    public MultiColDataPoint(MultiColData multiColData, int col, int row) {
        this();
        setMultiColRef(multiColData, col, row);
    }

    public MultiColDataPoint() {
    }

    public void setMultiColRef(MultiColData multiColData, int col, int row) {
        this.multiColData = multiColData;
        this.col = col;
        this.row = row;
    }

    public boolean updateValue(){
        return multiColData.updateValue(col, row, val);
    }

    public boolean updateVal(double newVal){
        val = newVal;
        return updateValue();
    }

}

