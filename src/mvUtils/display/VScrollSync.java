package mvUtils.display;

import javax.swing.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/23/13
 * Time: 2:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class VScrollSync implements AdjustmentListener {
    Vector<JScrollBar> vScroll;
    JScrollBar base;

    public VScrollSync(JScrollPane baseP) {
        JScrollBar sB = baseP.getVerticalScrollBar();
        this.base = sB;
        sB.addAdjustmentListener(this);
        vScroll = new Vector<JScrollBar>();
    }

    public void add(JScrollPane pane) {
        vScroll.add(pane.getVerticalScrollBar());
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        syncPos();
    }

    public void syncPos() {
        int value = base.getValue();
        for (JScrollBar bar : vScroll)
            bar.setValue(value);
    }
}
