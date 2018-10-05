package directFiredHeating;

import mvUtils.math.SPECIAL;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 24/08/2018
 * Time: 2:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class RadUnitFurnace {
    UnitFurnace baseUf;
    public double tempWO, tempWmean;
    public double tempWOMean;

    public RadUnitFurnace(UnitFurnace uF) {
        baseUf = uF;
    }

    public void copyChTemps(UnitFurnace ufFrom) {
        tempWmean = ufFrom.unitFceForWallOnlyFactor.tempWmean;
        tempWO = ufFrom.unitFceForWallOnlyFactor.tempWO;
    }

    public FceEvaluator.EvalStat evalForWallOnlyFactor(boolean bFirstSlot, UnitFurnace prevSlot) {
        FceEvaluator.EvalStat status = FceEvaluator.EvalStat.OK;
        double tau, deltaT;
        double tWMassume, tWMrevised = 0, diff;
        double chHeat = 0;
        double two = 0, lmDiff, twmAvg;
        boolean done = false;
        deltaT = baseUf.fceSec.lastRate;
        RadUnitFurnace prevRadUnitFceSlot = prevSlot.unitFceForWallOnlyFactor;
        tWMassume = prevRadUnitFceSlot.tempWmean + deltaT * baseUf.delTime;
        double alpha;
        while (!done  && baseUf.furnace.canRun()) {
            chHeat = baseUf.production.production * baseUf.gRatio *
                    (baseUf.ch.getHeatFromTemp(tWMassume) - baseUf.ch.getHeatFromTemp(prevRadUnitFceSlot.tempWmean));
            two = chargeSurfTempWithOnlyWallRadiation(tWMassume);
            lmDiff = SPECIAL.lmtd((baseUf.tempO - two), (baseUf.tempO - prevRadUnitFceSlot.tempWO));
            twmAvg = baseUf.tempO - lmDiff;
            alpha = baseUf.getSimpleAlpha(twmAvg);
            tau = baseUf.evalTau(alpha, baseUf.ch.getTk(twmAvg), baseUf.furnace.effectiveChThick * baseUf.gRatio);
            tWMrevised = baseUf.chargeEndTemp(baseUf.tempO, prevRadUnitFceSlot.tempWmean,
                    baseUf.g * baseUf.gRatio * baseUf.ch.avgSpHt(tWMassume, prevRadUnitFceSlot.tempWmean), alpha, tau,false);
            diff = tWMassume - tWMrevised;
            if (Math.abs(diff) <= 0.5 * baseUf.tuning.errorAllowed)
                done = true;
            else
                tWMassume = (baseUf.bRecuType) ? (tWMassume + tWMrevised) / 2 : tWMrevised;
            if (tWMassume >= (baseUf.tempO - 0.0001)) {
                status = FceEvaluator.EvalStat.ABORT;
                break;
            }
        }
        if (status == FceEvaluator.EvalStat.OK) {
            tempWO = two;
            tempWmean = tWMrevised;
//            chargeHeat = chHeat;
//            showResult();
        }
        return status;
    }

    double chargeSurfTempWithOnlyWallRadiation(double twm) {
        double twoAssume, twoRevised, diff, alpha;
        twoAssume = twm + 20;
        boolean done = false;
        while (!done  && baseUf.furnace.canRun()) {
            alpha = baseUf.getSimpleAlpha(twoAssume);
            twoRevised = baseUf.tempO - baseUf.evalTau(alpha, baseUf.ch.getTk(twm), baseUf.furnace.effectiveChThick
                    * baseUf.gRatio) * (baseUf.tempO - twm);
            diff = twoRevised - twoAssume;
            if (Math.abs(diff) <= 0.5 * baseUf.tuning.errorAllowed)
                done = true;
            else
                twoAssume = twoRevised;
        }
        return twoAssume;
    }

 }


