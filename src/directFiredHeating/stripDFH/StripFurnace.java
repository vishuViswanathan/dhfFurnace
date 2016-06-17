package directFiredHeating.stripDFH;

import directFiredHeating.DFHFurnace;
import directFiredHeating.DFHeating;
import performance.stripFce.PerformanceGroup;

import java.awt.event.ActionListener;

/**
 * User: M Viswanathan
 * Date: 17-Jun-16
 * Time: 12:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class StripFurnace extends DFHFurnace {
    public StripFurnace(DFHeating dfHeating, boolean bTopBot, boolean bAddTopSoak, ActionListener listener) {
        super(dfHeating, bTopBot, bAddTopSoak, listener);
    }

    protected boolean createPerfBase() {
        performBase = new PerformanceGroup(this, tuningParams);
//        performBase.setTableFactors(0.2, 0.2, 0.7, 0.1);
        if (airRecu != null)
            freezeAirRecu();
        return addResultsToPerfBase(true);
    }

    protected boolean addToPerfBase() {
        return addResultsToPerfBase(true);
    }


}
