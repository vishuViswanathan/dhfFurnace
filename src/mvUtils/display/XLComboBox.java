package mvUtils.display;

import javax.swing.*;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 12/10/12
 * Time: 12:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class XLComboBox extends JComboBox implements XLcellData{
    boolean bBold = false;
    boolean bOnlySelected = true;
    public XLComboBox(Object items[], boolean bOnlySelected) {
        super(items);
        this.bOnlySelected = bOnlySelected;
    }

    public XLComboBox(Vector items, boolean bOnlySelected) {
        super(items);
        this.bOnlySelected = bOnlySelected;
    }

    public XLComboBox(Object items[]) {
        this(items, true);
    }

    public XLComboBox(Vector items) {
        this(items, true);
    }


    public ValueForExcel getValueForExcel() {
        if (bOnlySelected)
            return new ValueForExcel(bBold, "" + getSelectedItem());
        else {
            String val = "";
            int n = getItemCount();
            for (int i = 0; i < n; i++)
                val += ((i > 0) ? "" : ", ") + getItemAt(i);
            return new ValueForExcel(bBold, val);
        }
    }

    public String getFmtStr() {
        return "";
    }
}
