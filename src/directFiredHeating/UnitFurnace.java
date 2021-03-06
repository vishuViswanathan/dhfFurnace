package directFiredHeating;

// R20180824 wallOnly calculation
import basic.ChMaterial;
import basic.FlueCompoAndQty;
import basic.FlueComposition;
import basic.ProductionData;
import mvUtils.math.MultiColDataPoint;
import mvUtils.math.SPECIAL;

import javax.swing.*;
import java.util.Stack;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 5/9/12
 * Time: 2:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class UnitFurnace {
    int MAXNEIGHBORS = 100;
    protected ProductionData production;
    protected boolean bRecuType;
    boolean bAddedTopSoak = false;
    double height, heightEntry, heightExit;
    double width, length, stPos, endPos;
    double chargeArea;
    public double psi, gThick;
    public double delTime, endTime;
    public double eO, eW;
    double losses;
    public double tempWO, tempWcore, tempWmean, tempG, tempO;
    public double tempOMean, tempWOMean;
    protected double chargeHeat = 0;
    double heatToCharge, heatFromWall, heatFromGas;
    public MultiColDataPoint dpTempWO = null, dpTempWcore = null, dpTempWmean, dpTempG, dpTempO, dpEndTime;
    public MultiColDataPoint dpTotAlpha, dpAlphaGas, dpAlphaWall, dpAlphaTOW, dpAlphaAbsorb;
    public MultiColDataPoint dpChargeHeat, dpHeatToCharge, dpHeatFromWall, dpHeatFromGas, dpHeatAbsorbed;
    public MultiColDataPoint dpWallOnlyFactor;

    protected double gRatio;
    boolean bNoShare = true;  // no coresponding bottom zone
    double s152;
    double alphaGasPart, alphaWallPart, alphaTOW, alphaAbsorbWRTtempG, alphaAbsorbWRTtempO;
    double slotRadOut, slotRadIn, slotRadNetOut;
    public double temporaryLossCorrection = 0;
    UnitFurnace pEntryNei, pExitNei;
    RadUnitFurnace unitFceForWallOnlyFactor;
    Vector<RadToNeighbors> radSrc, radSink;
    boolean bWallFacingExit, bWallFacingEntry;
    double vWallTop, vWallBot;
    FceSubSection fceSubSec;
    public FceSection fceSec;
    public DFHTuningParams.FurnaceFor furnaceFor;
//    FlueComposition flue;
    protected DFHFurnace furnace;
    protected ChMaterial ch;
    public DFHTuningParams tuning;
    public double g;
    public boolean bBot = false;
    double lastDeltaT;
    FlueCompoAndQty flueCompoAndQty;

    public UnitFurnace(FceSubSection fceSubSec, boolean bRecuType, double length, double endPos, double width,
                       double heightEntry, double heightExit, DFHTuningParams.FurnaceFor furnaceFor) {
        this(furnaceFor);
        noteSubSec(fceSubSec);
        bBot = fceSec.botSection;
//        g = (bBot) ? furnace.gBot : ((bAddedTopSoak) ? furnace.gTopAS : furnace.gTop);
        this.bRecuType = bRecuType;
        this.length = length;
        this.endPos = endPos;
        stPos = endPos - length;
        this.width = width;
        this.heightEntry = heightEntry;
        this.heightExit = heightExit;
        setHeight();
        setOtherParams();
        radSink = new Vector<RadToNeighbors>();
        radSrc = new Stack<RadToNeighbors>();
    }

    public UnitFurnace(DFHTuningParams.FurnaceFor furnaceFor) {
        this.furnaceFor = furnaceFor;
        dpTempG = new MultiColDataPoint();
        dpTempO = new MultiColDataPoint();
        if (furnaceFor != DFHTuningParams.FurnaceFor.STRIP) {
            dpTempWO = new MultiColDataPoint();
            dpTempWcore = new MultiColDataPoint();
        }
        dpTempWmean = new MultiColDataPoint();
        dpEndTime = new MultiColDataPoint();
        dpTotAlpha = new MultiColDataPoint();
        dpAlphaGas = new MultiColDataPoint();
        dpAlphaWall = new MultiColDataPoint();
        dpAlphaTOW = new MultiColDataPoint();
        dpAlphaAbsorb = new MultiColDataPoint();
        dpChargeHeat = new MultiColDataPoint();
        dpWallOnlyFactor = new MultiColDataPoint();
        unitFceForWallOnlyFactor = new RadUnitFurnace(this);
    }

    public void setTempO(double tempVal) {
        tempO = tempVal;
    }

    public void setChargeTemperature(double temp) {
        setChargeTemperature(temp, temp, temp);
    }

    public void setChargeTemperature(double tempWO, double tempWmean, double tempWcore) {
        this.tempWO = tempWO;
        this.tempWmean  = tempWmean;
        this.tempWcore = tempWcore;
    }

    void noteSubSec(FceSubSection sub) {
        this.fceSubSec = sub;
        fceSec = fceSubSec.section;
        bAddedTopSoak = fceSec.bAddedSoak;
        furnace = fceSec.furnace;
        flueCompoAndQty = fceSec.passFlueCompAndQty;
//        flue = fceSec.flueComposition;
        tuning = furnace.tuningParams;
        furnace = fceSec.furnace;
        production = furnace.productionData;
        ch = furnace.productionData.charge.chMaterial;
    }

    UnitFurnace(UnitFurnace copyFrom) {
        this(copyFrom.furnaceFor);
        noteSubSec(copyFrom.fceSubSec);
        this.endPos = copyFrom.endPos;
        this.stPos = copyFrom.endPos;
        this.endTime = copyFrom.endTime;
        this.gRatio = copyFrom.gRatio;
        this.bNoShare = copyFrom.bNoShare;
        this.g = copyFrom.g;
        this.bBot = copyFrom.bBot;
        radSink = new Vector<RadToNeighbors>();
        radSrc = new Stack<RadToNeighbors>();
    }

    public void copyParamesTo(UnitFurnace ufTo) {
        ufTo.bRecuType = bRecuType;
        ufTo.noteSubSec(fceSubSec);
        ufTo.gThick = gThick;
        ufTo.psi = psi;
        ufTo.eW = eW;
        ufTo.eO = eO;
        ufTo.s152 = s152;
        ufTo.bBot = bBot;
        ufTo.g = g;
    }

    public void copyChTemps(UnitFurnace ufFrom) {
        tempWmean = ufFrom.tempWmean;
        tempWcore = ufFrom.tempWcore;
        tempWO = ufFrom.tempWO;
//        if (!fceSec.bGasTempSpecified)
//            tempG = ufFrom.tempG;
//
        if (fceSec.bGasTempSpecified)
            tempG = fceSec.presetGasTemp;
        else
            tempG = ufFrom.tempG;
    }

    public double setInitialChHeat(double tIn, double rate) {
       double tOut;
       tOut = tIn  + rate * length;
       chargeHeat = ch.getDeltaHeat(tIn, tOut) * gRatio * production.production ;
       return tOut;
   }

    void setOtherParams() {
        g = (bBot) ? furnace.gBot : ((bAddedTopSoak) ? furnace.gTopAS : furnace.gTop);
        double fceUnitArea;
        ch = furnace.productionData.charge.chMaterial;
        if (furnace.bTopBot && !bAddedTopSoak)
            fceUnitArea = (furnace.fceWidth + 2 * height) * production.chPitch;
        else {
            if (furnace.controller.heatingMode == DFHeating.HeatingMode.TOPBOTSTRIP)
                fceUnitArea = 2 * (furnace.fceWidth + height) * production.chPitch;  // take whole area of chamber since actually it is topBot fired
            else
                fceUnitArea = (2 * (furnace.fceWidth + height) - production.charge.length) *
                    production.chPitch;
        }
        psi = ((bBot)? furnace.chUAreaBot : ((bAddedTopSoak) ? furnace.chUAreaAS : furnace.chUAreaTop))/ fceUnitArea;
//        debug("for psi chUAreaTop is taken");

        s152 = (bAddedTopSoak) ? furnace.s152StartAS :furnace.s152Start;
        delTime = length / furnace.speed;
//        tau = 0;
        setgThick(fceSec.sectionLength());
        if (furnace.bTopBot && !bAddedTopSoak && !furnace.isInTopOnlySection(stPos)) {
            gRatio = ((bBot) ? furnace.chUAreaBot : furnace.chUAreaTop) / (furnace.chUAreaTop + furnace.chUAreaBot);
            bNoShare = false;
        }
        else  {
            gRatio = 1;
            bNoShare = true;
        }
        collectLosses();
//        losses = fceSubSec.totLosses / fceSubSec.length * length;
        eO = furnace.tuningParams.epsilonO;
        eW = 0.8; // to start with
    }

    /**
     *
      * @param startTime
     * @return endTime of the slot
     */
    public double setProductionBasedParams(double startTime) {
        setOtherParams();
        endTime = startTime + delTime;
        return endTime;
    }

    void collectLosses() {
        losses = fceSubSec.totLosses / fceSubSec.length * length;
    }

    public double getWMean(double tempWO, double deltaTemp) {
        this.tempWO = tempWO;
        tempWmean = tempWO - deltaTemp / s152;
        return tempWmean;
    }


    public void setEndTme(double t) {
        endTime = t;
    }

    public void setgThick(double len) {
        if (furnace.controller.heatingMode == DFHeating.HeatingMode.TOPBOTSTRIP)
            gThick = 0.55 * Math.pow(len * furnace.fceWidth * height / 2,(1.0/3));
        else
            gThick = 0.55 * Math.pow(len * furnace.fceWidth * height,(1.0/3));
    }

    void setHeight() {
        height = (heightEntry + heightExit) / 2;
    }

    UnitFurnace getEmptySlot() {
        return new UnitFurnace(this);
    }

    public double minHeight() {
        return (heightEntry < heightExit) ? heightEntry : heightExit;
    }

    public double maxHeight() {
        return (heightEntry > heightExit) ? heightEntry : heightExit;
    }

    public double totLosses() {
        return losses + slotRadNetOut + temporaryLossCorrection;
    }

    public double getvWallTop() {
        return vWallTop;
    }

    public double getvWallBot() {
        return vWallBot;
    }

    public boolean isbWallFacingEntry() {
        return bWallFacingEntry;
    }

    public boolean isbWallFacingExit() {
        return bWallFacingExit;
    }

    public UnitFurnace getEntryNei() {
        return pEntryNei;
    }

    void noteEntryNei(UnitFurnace pEntryNei) {
        this.pEntryNei = pEntryNei;
        if (pEntryNei.heightExit < heightEntry) {
            bWallFacingExit = true;
            vWallTop = heightEntry;
            vWallBot = pEntryNei.heightExit;
        } else {
            bWallFacingExit = false;
        }
    }

    public UnitFurnace getExitNei() {
        return pExitNei;
    }

    public void setChTemps(double tempWO, double tempWmean, double tempWcore) {
        this.tempWO = tempWO;
        this.tempWmean = tempWmean;
        this.tempWcore = tempWcore;
        uploadData();
    }

    public void noteExitNei(UnitFurnace pExitNei) {
        this.pExitNei = pExitNei;
        if (pExitNei.heightEntry < heightExit) {
            bWallFacingEntry = true;
            vWallTop = heightExit;
            vWallBot = pExitNei.heightEntry;
        } else {
            bWallFacingEntry = false;
        }
        pExitNei.noteEntryNei(this);
    }

    public double getHeightEntry() {
        return heightEntry;

    }

    public double getHeight() {
        return height;
    }

    public void setHeightEntry(double heightEntry) {
        this.heightEntry = heightEntry;
        setHeight();
    }

    public double getHeightExit() {
        return heightExit;
    }

    public void setHeightExit(double heightExit) {
        this.heightExit = heightExit;
        setHeight();
    }

    public double getStPos() {
        return endPos - length;
    }

    public RadToNeighbors getRadSrc(int s) {
        return radSrc.get(s);
    }

    public RadToNeighbors gerRadSink(int s) {
        return radSink.get(s);
    }

    public void noteEndWall(boolean bCharging) {
        if (bCharging) {
            bWallFacingEntry = false;
            bWallFacingExit = true;
            vWallTop = heightEntry;
            vWallBot = 0;
        } else {
            bWallFacingEntry = true;
            bWallFacingExit = false;
            vWallTop = heightExit;
            vWallBot = 0;
        }
    }

    public void initradNeighbors() {
        int nei;
        UnitFurnace uF;
        RadToNeighbors rN;
        uF = this;
        nei = 0;
        while (nei < MAXNEIGHBORS) {
            uF = uF.getEntryNei();
            if (uF == null)
                break;
            rN = new RadToNeighbors(this, uF, furnace.controller.heatingMode, false); // receiver is towards entry
            radSink.add(rN);
            nei++;
        }
        uF = this;
        nei = 0;
        while (nei < MAXNEIGHBORS) {
            uF = uF.getExitNei();
            if (uF == null)
                break;
            rN = new RadToNeighbors(this, uF, furnace.controller.heatingMode, true); // receiver is towards exit
            radSink.add(rN);
            nei++;
        }
    }

    void evalSlotRadiationOut() {
        slotRadOut = 0;
        for (int nei = 0; nei < radSink.size(); nei++)
            slotRadOut += radSink.get(nei).calculate();
    }

    void evalSlotRadiationSumm() {
        slotRadIn = 0;
        for (int nei = 0; nei < radSrc.size(); nei++)
            slotRadIn += radSrc.get(nei).getpHeatOut();
        slotRadNetOut = slotRadOut - slotRadIn;
    }

    void chCoreTemp() {
        tempWcore = tempWO - s152 * (tempWO - tempWmean);
        if (tempWcore < production.entryTemp)
            tempWcore = production.entryTemp;
    }

    public boolean addRadSrce(RadToNeighbors radS) {
        if (radSrc.size() <= MAXNEIGHBORS) {
            radSrc.add(radS);
            return true;
        } else
            return false;
    }

    public void setgRatio(double gRatio) {
        this.gRatio = gRatio;
    }

    double gasTforChargeT(double tWo, double surfCore) {
        double tMean, tGassume, tGrevised, tk, tau, alpha, diff;
        tMean = tWo - surfCore / s152;
        tk = ch.getTk(tMean);
        tGassume = tWo + 50;
        boolean done = false;
        while (!done  && furnace.canRun()) {
            alpha = fceTempAndAlpha(tGassume, tWo);
            if (furnace.canRun())  {
                tau = evalTau(alpha, tk, ((bAddedTopSoak) ? furnace.effectiveChThickAS : furnace.effectiveChThick) * gRatio);
                tGrevised = tMean + surfCore / s152 / (1 - tau);
                diff = tGrevised - tGassume;
                if (Math.abs(diff) <= (0.5 * tuning.errorAllowed))
                    done = true;
                else
                    tGassume = (tGassume + tGrevised) / 2;  // 20090430 was tGrevised
            }
        }
        return tGassume;
    }



     protected double evalTau(double alpha, double tk, double gX) {
         // The following formula if for a.t/X2 greatrer than 0.2
         // as per fig.83 of Heilingenstaedt for Plates
        double retVal = 1;
        double tauFactor = alpha * gX / tk;
        retVal = Math.exp(tauFactor * (0.031754 * tauFactor - 0.33172 - 0.0016197 * Math.pow(tauFactor, 2)));
        if (retVal >= 1)
            retVal = 1;
        return retVal;
    }

//    double fceTempAndAlpha(double tg) {
//        return fceTempAndAlpha(tg, tempWO);
//    }

    static int errCount = 0;

    double fceTempAndAlpha(double tg, double two) {
        double alpha = 0;
//        if (gThick > 0.001) {
            if ((psi <= 0) || (gThick < 0) || (eO <= 0) || (eW <= 0)) {
                errMsg("DATA psi=" + psi + ", gThick=" + gThick + ", eo=" + eO +
                        ", ew=" + eW + "  - Finding Alpha (FceTempAndAlpha)");
                alpha = -1;
                two = -100;
            } else {
                if ((tg <= 0) || (tg > 2200)) {
                    debug("GAS temp Out of range <" + tg + ">, still continuing ..." + errCount++);
                    if (errCount > 1000) {
                        errMsg("GAS temp Out of range <" + tg + ">. Aborting after many trials <" + errCount + ">");
                        furnace.abortIt("Unable to evaluate alpha and wall Temperature in UnitFurnace");
                    }
                }
            }
            if (furnace.canRun())
                alpha = fcTalpha(tg, two);
//        }
        return alpha;
    }

    private double alphaOWEffective(double tO, double two, double eWnow) {
        double epsilonOW, alphaOW;
        FlueComposition radFlue = fceSec.totFlueCompAndQty.flueCompo;
        epsilonOW = 1 / (1 / eWnow + psi * (1 / eO - 1));
        if (tO == two)
            alphaOW = SPECIAL.stefenBoltz * (Math.pow(tO + 273, 4) - Math.pow(tO - 1 + 273, 4)) / 1;
        else
            alphaOW = SPECIAL.stefenBoltz * (Math.pow(tO + 273, 4) - Math.pow(two + 273, 4)) / (tO - two);

        double alphaEOW = alphaOW * epsilonOW;
        double alphaGOW = radFlue.alphaGas(tO, two, gThick) * tuning.emmFactor;
        double alphaEGOW = alphaGOW * eO;
        alphaAbsorbWRTtempO = alphaEGOW;
        double effValue = alphaEOW  - alphaEGOW;
        return (effValue > 0) ? effValue : 0;
    }

    double fcTalpha(double tg, double two) {
        double alpha = 0;
        double alphaGO, alphaEGO; // between gas and wall
        double diff;
        double aGOplusConv = 0, aOWeffective = 0, alphaEGWplusCov = 0;
        double alphaConv = (bRecuType) ? tuning.alphaConvRecu : tuning.alphaConvFired;
        FlueComposition radFlue = fceSec.totFlueCompAndQty.flueCompo;
        if (tg < -273 || two < -273) {
            alpha = 1;
            tempO = (tg + two) / 2;
        } else {
            double tempOAssume, tempO1 = 0;
            tempOAssume = (tg + two) / 2; //   Starting assumption for Fce temp
            int nowTrialSet = 2;
            double adjustmentFactor = 0.5;
            int maxLoopCount = 2000;
            int loopCount = maxLoopCount;
            boolean done = false;
            while (!done && furnace.canRun()) {
                loopCount--;
                if (loopCount < 0) {
                    if (nowTrialSet < 0) {
                        errMsg("TOO many iterations! in Finding Alpha (fcTalpha)." +
                                "\n gasTemp =" + tg + ", Charge Surface Temp = " + two + "\nAborting after many trials");

                        furnace.abortIt("Unable to evaluate alpha in UnitFurnace");
                        break;
                    }
                    else {
                        nowTrialSet--;
                        adjustmentFactor *= 0.5;
                        loopCount = maxLoopCount;
                    }
                }
                alphaGO = radFlue.alphaGas(tg, tempOAssume, gThick) * tuning.gasWallHTMultipler * tuning.emmFactor;
                alphaEGO = alphaGO * eO;

                // find revised furnace temperature
                aGOplusConv = alphaEGO + alphaConv; // added convection
                aOWeffective = alphaOWEffective(tempOAssume, two, eW);
                tempO1 = (tg * aGOplusConv + psi * two * aOWeffective - tuning.wallLoss) /
                        (aGOplusConv + psi * aOWeffective);
                diff = tempO1 - tempOAssume;
                if (Math.abs(diff) <= 0.1 * tuning.errorAllowed)
                    done = true;
                else
                    tempOAssume += diff * adjustmentFactor; //  (tempOAssume + tempO1) / 2;
            }
            // result reached
            double alphaGW, alphaEGW;
            if (tuning.noGasRadiationToCharge)
                alphaEGWplusCov = 0;
            else {
                alphaGW = radFlue.alphaGas(tg, two, gThick) * tuning.emmFactor;
                alphaEGW = alphaGW * eW;
                alphaEGWplusCov = alphaEGW + alphaConv; // added convection
            }
            tempO = tempO1;
            alpha = alphaEGWplusCov + aOWeffective * (tempO - two) / (tg - two);
            alphaTOW = alpha * (tg - two) / (tempO - two);


            alphaGasPart = alphaEGWplusCov;
            alphaWallPart = alpha - alphaGasPart;
        }
        if (tuning.bOnTest) {
            heatToCharge = chargeArea * alpha * (tg - two) ;
            heatFromWall = chargeArea * aOWeffective * (tempO - two);
            heatFromGas = chargeArea * alphaEGWplusCov * (tg - two);
            alphaAbsorbWRTtempG = alphaAbsorbWRTtempO * (tempO - two) / (tg - two);
        }
        return alpha;
    }

    /**
     * Calculation considering only wall radiation
     * @param twO
     * @return
     */
    double getSimpleAlpha(double twO) {
//        double alphaConv = (bRecuType) ? tuning.alphaConvRecu : tuning.alphaConvFired;
        double emissChO = ch.getEmiss(twO) * production.chEmmissCorrectionFactor;
        double epsilonOW = 1 / (1 / emissChO + psi * (1 / eO - 1));
        double alpha = epsilonOW * 1 * SPECIAL.stefenBoltz * (Math.pow(tempO + 273, 4) - Math.pow(twO + 273, 4)) / (tempO - twO);
        alpha *= (tuning.radiationMultiplier * fceSec.wallOnlyFactor);
//        alpha += alphaConv;
        alphaTOW = alpha;
        return alpha;
    }

    double getDeltaTcharge(double gasT, double two) {
        double tk = ch.getTk(two);
        double alpha = fceTempAndAlpha(gasT, two);
        double tau = evalTau(alpha, tk, ((bAddedTopSoak) ? furnace.effectiveChThickAS : furnace.effectiveChThick) * gRatio);
        return (gasT - two) * s152 * (1 - tau) / tau;
    }

    public void setEW(double chTemp) {
        eW = ch.getEmiss(chTemp) * production.chEmmissCorrectionFactor;
    }

    double gasTFromFceTandChT(double tempO1, double two) {
        double tempOresult;
        FlueComposition radFlue = fceSec.totFlueCompAndQty.flueCompo;
        double tempGAssume;
        tempGAssume = tempO1 + (tempO1 - two) / 2; //   Starting assumption for Gas temp
        double alphaOW, alphaEOW = 0; // between wall and charge
        double alphaGOW = 0, alphaEGOW = 0; //gas absorption between wall and charge
        double alphaGO, alphaEGO; // between gas and wall
        double diff;
        double aGOplusConv = 0, aOWeffective = 0;
        double aOWforTempO; // for tempO based on heat balance at wall
        int loopCount = 5000;
        boolean done = false;
        double alphaConv = (bRecuType) ? tuning.alphaConvRecu : tuning.alphaConvFired;
        aOWeffective = alphaOWEffective(tempO1, two, eW);
        while (!done && furnace.canRun()) {
            loopCount--;
            if (loopCount <= 0) {
//                errMsg("TOO many iterations! in Finding Alpha (gasTFromFceTandChT). Aborting after many trials");
                errMsg("TOO many iterations! in Finding Alpha (gasTFromFceTandChT)." +
                        "\n Fce Temp =" + tempO1 + ", Charge Surface Temp = " + two + "\nAborting after many trials");
                furnace.abortIt("Unable to evaluate Gas Temperature from Wall and Cgarge Temperature in UnitFurnace");
                break;
            }
            // find alphaOW

            alphaGO = radFlue.alphaGas(tempGAssume, tempO1, gThick) * tuning.gasWallHTMultipler * tuning.emmFactor;
            alphaEGO = alphaGO * eO;

            // find revised furnace temperature
            aGOplusConv = alphaEGO + alphaConv; // added convection

            tempOresult = (tempGAssume * aGOplusConv + psi * two * aOWeffective - tuning.wallLoss) /
                    (aGOplusConv + psi * aOWeffective);
            diff = tempOresult - tempO1;
            if (Math.abs(diff) <= 0.1 * tuning.errorAllowed)
                done = true;
            else
                tempGAssume -= diff / 2;
        }
        return tempGAssume;
    }

    double getAlpha() {
        return alphaGasPart + alphaWallPart;
    }

    double startTime() {
        return endTime - delTime;
    }

    double avgGasTemp() {
        if (pEntryNei != null)
            return (tempG + pEntryNei.tempG) / 2;
        else
            return tempG;
    }

    double chargeSurfTemp(double tg, double twm) {
        double twoAssume, twoRevised, diff, alpha;
        setEW(twm); // 20170227 eW = ch.getEmiss(twm) * production.chEmmissCorrectionFactor;      // not used here
        twoAssume = twm + 20;
        boolean done = false;
        while (!done  && furnace.canRun()) {
            alpha = fceTempAndAlpha(tg, twoAssume);
            twoRevised = tg - evalTau(alpha, ch.getTk(twm), ((bAddedTopSoak) ? furnace.effectiveChThickAS : furnace.effectiveChThick) * gRatio) * (tg - twm);
            diff = twoRevised - twoAssume;
            if (Math.abs(diff) <= 0.5 * tuning.errorAllowed)
                done = true;
            else
                twoAssume = twoRevised;
        }
        return twoAssume;
    }

    /**
     * Charge surfcae temperature base on only Wall radiation
     * @param twm
     * @return
     */
    double chargeSurfTempWithOnlyWallRadiation(double twm) {
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

    double chargeSurfTemp() {
        return chargeSurfTemp(tempG, tempWmean);
    }

    protected double chargeEndTemp(double tempSrc, double twmFrom, double gc, double alpha, double tau, boolean inRev) {
        double deltaT, phiFactor;
        phiFactor = alpha * delTime / gc;
        if (inRev) phiFactor = -phiFactor;
        deltaT = (tempSrc - twmFrom) * Math.exp(-phiFactor * tau);
        return tempSrc - deltaT;
    }

    double gasTempAfterHeat(double t1, double flueQty, double heatGained) {
        FlueComposition radFlue = fceSec.totFlueCompAndQty.flueCompo;
//        FlueComposition radFlue = flueCompoAndQty.flueCompo;
        double h2, deltaH;
        deltaH = heatGained / flueQty;
        h2 = radFlue.sensHeatFromTemp(t1) + deltaH;
        return radFlue.tempFromSensHeat(h2);
    }

    double gasTempAfterHeat(double t1, FlueCompoAndQty passFlue, double heatGained) {
        double h2, deltaH;
        deltaH = heatGained / passFlue.flow;
        h2 = passFlue.flueCompo.sensHeatFromTemp(t1) + deltaH;
        return passFlue.flueCompo.tempFromSensHeat(h2);

    }

    public FceEvaluator.EvalStat evalInRev(boolean bLastSlot, UnitFurnace prevSlot, double tRate)  {
        FceEvaluator.EvalStat retVal = FceEvaluator.EvalStat.DONTKNOW;
        double tWMassume = 0, tWMrevised = 0, diff;
        double chHeat = 0, totheat;
        double tempGB = 0;
        double two = 0, lmDiff, twoAvg, tgAvg, twmAvg;
        double tau, alpha;
        boolean done;
        tWMassume = tempWmean - tRate * delTime;
        done = false;
        boolean inReverse = true;
        int trial = 0;
        int maxTrials = 1000;
        int trialsForTempGBLow = maxTrials - 100;
        while (!done  && furnace.canRun()) {
            if (trial++ > maxTrials) {
                retVal = FceEvaluator.EvalStat.TOOMANYTRIALS;
                break;
            }
            chHeat = production.production * gRatio *
                    (ch.getHeatFromTemp(tWMassume) - ch.getHeatFromTemp(tempWmean));
            totheat = chHeat - totLosses(); // losses;
            tempGB = (bRecuType) ? gasTempAfterHeat(tempG, fceSec.passFlueCompAndQty, totheat) : tempG;
            if (tempGB <= tempWmean) {
                if (trial > trialsForTempGBLow ){
                    retVal = FceEvaluator.EvalStat.TOOLOWGAS;
                    break;
                }
                else {
                    tWMassume = (tempWmean + tWMassume) / 2;
                    continue;
                }
            }
            if (bLastSlot) {
                setEW(tempWmean); // 20170227  eW = ch.getEmiss(tempWmean) * production.chEmmissCorrectionFactor;
                chargeSurfTemp(tempG, tempWmean);
            }

            prevSlot.setEW(tWMassume);  // 20170227 prevSlot.eW = ch.getEmiss(tWMassume) * production.chEmmissCorrectionFactor;
            two = prevSlot.chargeSurfTemp(tempGB, tWMassume);
            if (furnace.canRun()) {
                if ((tempGB - two)/(tempG - tempWO) < 0) {
                    retVal = FceEvaluator.EvalStat.DONTKNOW;
                    break;
                }

                lmDiff = SPECIAL.lmtd((tempGB - two), (tempG - tempWO));
                twoAvg = (two + tempWO) / 2;
                tgAvg = twoAvg + lmDiff;
                twmAvg = (tWMassume + tempWmean) / 2;
                setEW(twmAvg); // 20170227  eW = ch.getEmiss(twmAvg) * production.chEmmissCorrectionFactor;
                alpha = fceTempAndAlpha(tgAvg, twoAvg);
                tau = evalTau(alpha, ch.getTk(twmAvg), ((bAddedTopSoak) ? furnace.effectiveChThickAS : furnace.effectiveChThick) * gRatio);
                tWMrevised = chargeEndTemp(tgAvg, tempWmean,
                        g * gRatio * ch.avgSpHt(tWMassume, tempWmean), alpha, tau, inReverse);
                if (tWMrevised < -100) {
                    retVal = FceEvaluator.EvalStat.TOOHIGHGAS;
                    break;
                }
                diff = tWMassume - tWMrevised;

                if (Math.abs(diff) < 0.5 * tuning.errorAllowed) {
                    retVal = FceEvaluator.EvalStat.OK;
                    done = true;
                }
                else
                    tWMassume = tWMassume + (tWMrevised - tWMassume) / 4;
            }
            else
                retVal = FceEvaluator.EvalStat.ABORT;
        }
        if (furnace.canRun() && (retVal == FceEvaluator.EvalStat.OK)) {
            // temperature with in limits
            chargeHeat = -chHeat;

            tempWO = chargeSurfTemp();
            showResult();
            lastDeltaT = (tempWmean - tWMrevised) / delTime;
            prevSlot.tempG = tempGB;
            prevSlot.tempWO = two;
            prevSlot.tempWmean = tWMassume;
            tempOMean = (tempO + prevSlot.tempO) / 2;
            tempWOMean = (tempWO + prevSlot.tempWO) / 2;
            prevSlot.showResult();
        }
        temporaryLossCorrection = 0;
        return retVal;
    }

    public double totalHeat() {
        return chargeHeat + totLosses(); //losses;
    }

    public void mergeSlots(UnitFurnace uf) {
        double avgVal;
        avgVal = (tempG + uf.tempG) / 2;
        tempG = uf.tempG = avgVal;
        avgVal = (tempO + uf.tempO) / 2;
        tempO = uf.tempO = avgVal;
        avgVal = (tempWO + uf.tempWO) / 2;
        tempWO = avgVal;
        uf.tempWO = avgVal;
        avgVal = (tempWcore + uf.tempWcore) / 2;
        tempWcore = uf.tempWcore = avgVal;
        avgVal = (tempWmean + uf.tempWmean) / 2;
        tempWmean = uf.tempWmean = avgVal;
        uploadData();
        uf.uploadData();
    }

    public void showResult() {
        chCoreTemp();
        uploadData();
        if (tuning.bSlotProgress)
            furnace.updateUI();
    }

    protected void uploadData() {
        dpTempG.updateVal(tempG);
        dpTempO.updateVal(tempO);
        if (furnaceFor != DFHTuningParams.FurnaceFor.STRIP) {
            dpTempWO.updateVal(tempWO);
            dpTempWcore.updateVal(tempWcore);
        }
        dpTempWmean.updateVal(tempWmean);
        dpEndTime.updateVal(endTime);
        if (tuning.bOnTest) {
            dpTotAlpha.updateVal(getAlpha());
            dpAlphaGas.updateVal(alphaGasPart);
            dpAlphaWall.updateVal(alphaWallPart);
            dpAlphaTOW.updateVal(alphaTOW);
            dpAlphaAbsorb.updateVal(alphaAbsorbWRTtempG);
            dpWallOnlyFactor.updateVal(fceSec.wallOnlyFactor);
        }
    }

    public FceEvaluator.EvalStat evalInFwd(boolean bFirstSlot, UnitFurnace prevSlot) {
        FceEvaluator.EvalStat retVal = FceEvaluator.EvalStat.DONTKNOW;
        if (furnace.bBaseOnOnlyWallRadiation)
            return evalWithWallRadiationInFwd(bFirstSlot, prevSlot);
        double deltaT;
        double tWMassume, tWMrevised = 0, diff;
        double chHeat = 0, totheat;
        double tempGE = 0;
        double tempGEforCharge; // temperature as seen by charge (ie. after Gas losses heat to losses)
        double two = 0, lmDiff, twoAvg, tgAvg, twmAvg;
        double tau, alpha;
        boolean done;
        deltaT = (bFirstSlot) ? fceSec.lastRate : 0;
        tWMassume = prevSlot.tempWmean + deltaT * delTime;
        done = false;
        while (!done  && furnace.canRun()) {
            chHeat = production.production * gRatio *
                    (ch.getHeatFromTemp(tWMassume) - ch.getHeatFromTemp(prevSlot.tempWmean));
            tempGEforCharge = (bRecuType) ? gasTempAfterHeat(prevSlot.tempG, fceSec.passFlueCompAndQty, chHeat) : prevSlot.tempG;
            totheat = chHeat + totLosses(); //losses;
            tempGE = (bRecuType) ? gasTempAfterHeat(prevSlot.tempG, fceSec.passFlueCompAndQty, totheat) : prevSlot.tempG;

            setEW(tWMassume); // 20170227  eW = ch.getEmiss(tWMassume) * production.chEmmissCorrectionFactor;
            two = chargeSurfTemp(tempGEforCharge, tWMassume);
            lmDiff = SPECIAL.lmtd((tempGEforCharge - two), (prevSlot.tempG - prevSlot.tempWO));
            twoAvg = (two + prevSlot.tempWO) / 2;
            tgAvg = twoAvg + lmDiff;
            twmAvg = (tWMassume + prevSlot.tempWmean) / 2;
            setEW(twmAvg); // 20170227  eW = ch.getEmiss(twmAvg) * production.chEmmissCorrectionFactor;
            alpha = fceTempAndAlpha(tgAvg, twoAvg);
            tau = evalTau(alpha, ch.getTk(twmAvg), ((bAddedTopSoak) ? furnace.effectiveChThickAS : furnace.effectiveChThick)* gRatio);
            tWMrevised = chargeEndTemp(tgAvg, prevSlot.tempWmean,
                    g * gRatio * ch.avgSpHt(tWMassume, prevSlot.tempWmean), alpha, tau, false);
            diff = tWMassume - tWMrevised;
            if (Math.abs(diff) <= 0.5 * tuning.errorAllowed) {
                retVal = FceEvaluator.EvalStat.OK;
                done = true;
            }
            else
                tWMassume = (bRecuType) ? (tWMassume + tWMrevised) / 2 : tWMrevised;
        }
        tempWO = two;
        tempWmean = tWMrevised;
        chargeHeat = chHeat;
        tempG = tempGE;
        tempOMean = (tempO + prevSlot.tempO) / 2;
        tempWOMean = (tempWO + prevSlot.tempWO) / 2;
//        showOneResult(iSlot)
        showResult();
        deltaT = (tempWmean - prevSlot.tempWmean) / delTime;
        return retVal;
    }

    public FceEvaluator.EvalStat evalForWallOnlyFactor(boolean bFirstSlot, UnitFurnace prevSlot) {
        return unitFceForWallOnlyFactor.evalForWallOnlyFactor(bFirstSlot, prevSlot);
    }

        /**
         *
         * @param bFirstSlot
         * @param prevSlot
         * @return
         */
    public FceEvaluator.EvalStat evalWithWallRadiationInFwd(boolean bFirstSlot, UnitFurnace prevSlot) {
        FceEvaluator.EvalStat status = FceEvaluator.EvalStat.OK;
        double tau, deltaT;
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
            two = chargeSurfTempWithOnlyWallRadiation(tWMassume);
            lmDiff = SPECIAL.lmtd((tempO - two), (tempO - prevSlot.tempWO));
            twmAvg = tempO - lmDiff;
            alpha = getSimpleAlpha(twmAvg);
            tau = evalTau(alpha, ch.getTk(twmAvg), furnace.effectiveChThick* gRatio);
            tWMrevised = chargeEndTemp(tempO, prevSlot.tempWmean,
                     g * gRatio * ch.avgSpHt(tWMassume, prevSlot.tempWmean), alpha, tau,false);
            diff = tWMassume - tWMrevised;
            if (Math.abs(diff) <= 0.5 * tuning.errorAllowed)
                done = true;
            else
                tWMassume = (bRecuType) ? (tWMassume + tWMrevised) / 2 : tWMrevised;
            if (tWMassume >= (tempO - 0.0001)) {
                status = FceEvaluator.EvalStat.ABORT;
                break;
            }
        }
        if (status == FceEvaluator.EvalStat.OK) {
            tempWO = two;
            tempWmean = tWMrevised;
            chargeHeat = chHeat;
            showResult();
        }
        return status;
    }


    void errMsg(String msg) {
        JOptionPane.showMessageDialog(null, "ERROR in UnitFUrnace: \n" + msg);
    }

    void debug(String msg) {
        System.out.println("UnitFurnace " + msg);
    }
}

