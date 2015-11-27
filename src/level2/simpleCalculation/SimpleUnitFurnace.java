package level2.simpleCalculation;

import directFiredHeating.DFHTuningParams;
import directFiredHeating.FceEvaluator;
import directFiredHeating.FceSubSection;
import directFiredHeating.UnitFurnace;
import mvUtils.math.MultiColDataPoint;
import mvUtils.math.SPECIAL;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 23-Oct-15
 * Time: 4:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleUnitFurnace extends UnitFurnace {
    double fctTemp;  // of eash unitFurnace
    double sFactorFceCh = 1;
    double emissFceCh;
    public MultiColDataPoint dpFieldTempO;  // evaluated from Furnace temperature of Section from the field
    public double fieldTempO;

    public SimpleUnitFurnace(FceSubSection fceSubSec, boolean bRecuType, double length, double endPos, double width,
                        double heightEntry, double heightExit, DFHTuningParams.ForProcess forProcess) {
        super(fceSubSec,  bRecuType, length, endPos, width,
                               heightEntry, heightExit, forProcess);
    }

    public SimpleUnitFurnace(DFHTuningParams.ForProcess forProcess) {
        super(forProcess);
        dpFieldTempO = new MultiColDataPoint();
    }

    public FceEvaluator.EvalStat evalInFwd(boolean bFirstSlot, UnitFurnace prevSlot) {
        double deltaT;
        double tWMassume, tWMrevised = 0, diff;
        double chHeat = 0;
        double two = 0, lmDiff, twmAvg;
        boolean done = false;
        deltaT = (bFirstSlot) ? fceSec.lastRate : 0;
        tWMassume = prevSlot.tempWmean + deltaT * delTime;
        double alpha;
        while (!done  && furnace.canRun()) {
            chHeat = production.production * gRatio *
                     (ch.getHeatFromTemp(tWMassume) - ch.getHeatFromTemp(prevSlot.tempWmean));
            two = chargeSurfTemp(tWMassume);
            lmDiff = SPECIAL.lmtd((tempO - two), (tempO - prevSlot.tempWO));
            twmAvg = tempO - lmDiff;
            alpha = getSimpleAlpha(twmAvg);
            tau = evalTau(alpha, ch.getTk(twmAvg), furnace.effectiveChThick* gRatio);
            tWMrevised = chargeEndTemp(tempO, prevSlot.tempWmean,
                     g * gRatio * ch.avgSpHt(tWMassume, prevSlot.tempWmean), alpha, false);
            diff = tWMassume - tWMrevised;
            if (Math.abs(diff) <= 0.5 * tuning.errorAllowed)
                done = true;
            else
                tWMassume = (bRecuType) ? (tWMassume + tWMrevised) / 2 : tWMrevised;
        }
        tempWO = two;
        tempWmean = tWMrevised;
        chargeHeat = chHeat;
        showResult();
        return FceEvaluator.EvalStat.OK;
    }

    double chargeSurfTemp(double twm) {
        double twoAssume, twoRevised, diff, alpha;
        twoAssume = twm + 20;
        boolean done = false;
          while (!done  && furnace.canRun()) {
              alpha = getSimpleAlpha(twoAssume);
              twoRevised = tempO - evalTau(alpha, ch.getTk(twm), furnace.effectiveChThick * gRatio) * (tempO - twm);
              diff = twoRevised - twoAssume;
              if (Math.abs(diff) <= 0.5 * tuning.errorAllowed)
                  done = true;
              else
                  twoAssume = twoRevised;
          }
          return twoAssume;
    }

    double getSimpleAlpha(double twO) {
        double emissChO = ch.getEmiss(twO);
        double epsilonOW = 1 / (1 / emissChO + psi * (1 / eO - 1));
        double alpha = epsilonOW * 1 * SPECIAL.stefenBoltz * (Math.pow(tempO + 273, 4) - Math.pow(twO + 273, 4)) / (tempO - twO);
        return alpha;
    }

    protected void uploadData() {
        super.uploadData();
        dpFieldTempO.updateVal(fieldTempO);
    }
 }
