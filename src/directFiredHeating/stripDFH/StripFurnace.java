package directFiredHeating.stripDFH;

import basic.Charge;
import basic.Observations;
import directFiredHeating.*;
import mvUtils.display.DataStat;
import mvUtils.display.StatusWithMessage;
import performance.stripFce.Performance;
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

    public boolean takePerformanceFromXML(String xmlStr) {
        return takePerformanceFromXML(xmlStr, true, false);
    }

    public StatusWithMessage addPerformance(Performance p) {
        return addPerformance(p, -1);
    }

    public StatusWithMessage addPerformance(Performance p, int atLoc) {
        StatusWithMessage stat =  p.linkToProcess();
        if (stat.getDataStatus() == DataStat.Status.OK)
            return super.addPerformance(p, atLoc) ;
        else
            return stat;
    }

    protected String getProcessName() {
        return productionData.processName;
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
            if (status.getDataStatus() == DataStat.Status.WithErrorMsg)
                observations.add("Fuel Flow in " +  oneSection.sectionName() + " is out of Range: " + status.getErrorMessage());
        }
        return observations;
    }
}
