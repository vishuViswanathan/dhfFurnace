package mvUtils.display;

import display.*;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 12/6/12
 * Time: 11:58 AM
 * To change this template use File | Settings | File Templates.
 */
public interface XLcellData {
    public display.ValueForExcel getValueForExcel();
    public String getFmtStr();
    public boolean isEnabled();
}
