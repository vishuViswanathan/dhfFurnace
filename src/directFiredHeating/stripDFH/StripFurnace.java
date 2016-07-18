package directFiredHeating.stripDFH;

import basic.Observations;
import directFiredHeating.DFHFurnace;
import directFiredHeating.DFHeating;
import directFiredHeating.FceSection;
import mvUtils.display.StatusWithMessage;
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
        if (airRecu != null)
            freezeAirRecu();
        return addResultsToPerfBase(true);
    }

    protected boolean addToPerfBase() {
        return addResultsToPerfBase(true);
    }

    protected Observations getObservations() {
        Observations observations = super.getObservations();
        for (int sec = 0; sec < nTopActiveSecs; sec++) {
            FceSection oneSection = topSections.get(sec);
            StatusWithMessage status = furnaceSettings.checkFuelFlowInRange(sec, oneSection.secFuelFlow);
            if (status.getDataStatus() == StatusWithMessage.DataStat.WithErrorMsg)
                observations.add("Fuel Flow in " +  oneSection.sectionName() + " is out of Range: " + status.getErrorMessage());
        }
        return observations;
    }
}
