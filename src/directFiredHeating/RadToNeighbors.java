package directFiredHeating;

import basic.FlueComposition;
import mvmath.DoublePoint;
import mvmath.LineMv;
import mvmath.SPECIAL;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 5/9/12
 * Time: 2:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class RadToNeighbors {
    UnitFurnace slotFrom, slotTo;
    boolean bSlotToOnExit;
    double interChF, shapeF, shadowFact, totalFactor;
    double srcArea, destArea;
    double vSrcArea;
    double lx1, lx2, l1, l2, roofLen;

    double vShapeF, vShadowFact, vTotalfactor;
    double vLx1, vLx2, vL1, vL2, vLen;
    double vHeatOut, hHeatOut;
    double vGasAbsorption, hGasAbsorption;
    double heatOut;
    double pDestHeatIntensity;
    boolean bBot;
    double chHeight;
    boolean bMindChHeight;
    DFHTuningParams tuning;
    FlueComposition flue;
    double distRoofCharge, distWallCharge;
    DFHeating.HeatingMode heatingMode = DFHeating.HeatingMode.TOPONLY;

    public RadToNeighbors(UnitFurnace slotFrom, UnitFurnace slotTo, DFHeating.HeatingMode heatingMode, boolean bSlotToOnExit) {
        this.slotFrom = slotFrom;
        this.slotTo = slotTo;
        this.bSlotToOnExit = bSlotToOnExit;
        this.bBot = slotTo.fceSec.botSection;
        this.flue = slotTo.fceSec.flueComposition;
        this.heatingMode = heatingMode;
        this.chHeight = slotTo.production.charge.height;
        tuning = slotTo.tuning;
        bMindChHeight = tuning.bMindChHeight;
        slotTo.addRadSrce(this);
        evalShapeFactor();
        evalWallShapeFactor();
    }

    public RadToNeighbors(UnitFurnace slotFrom, UnitFurnace slotTo, boolean bSlotToOnExit) {
        this(slotFrom, slotTo, DFHeating.HeatingMode.TOPONLY, bSlotToOnExit);
    }
    double evalObstrFactor(boolean bVwall) {
    if (bSlotToOnExit)
        return evalObstrExitside(bVwall);
    else
        return evalObstrEntryside(bVwall);
    }


    double evalObstrEntryside(boolean bVwall) {
        boolean reDo;
        double obstr, obstrMin;
        obstrMin = 1;
        double frHt; //the last height which was lower
        double lastHt;
        frHt = slotFrom.maxHeight();
        lastHt = frHt;
        double nowHt;
        UnitFurnace  slC;
        slC = slotFrom.getEntryNei();

        reDo = true;
        while (reDo)  {
            if (slC ==  slotTo) {
                reDo = false; // do for the last time
            }
            nowHt = slC.minHeight();
            if (nowHt < lastHt) { // possible obstruction
                lastHt = nowHt; //note it down so that have to check only thoses lower than this
                obstr = getObstrFact(slC, bVwall); // FOR THE TIME BEING
                if (obstr < obstrMin)
                    obstrMin = obstr;
                if (obstrMin <= 0)
                    reDo = false;
            }
             slC = slC.getEntryNei();
        }
        return obstrMin;
    }

    double evalObstrExitside(boolean bVwall) {
        boolean reDo;
        double obstr, obstrMin;
        obstrMin = 1; // we have to get the minimum
        double frHt; // from-slot height
        double lastHt; //the last height which was lower
        frHt = slotFrom.maxHeight();
        lastHt = frHt;
        double nowHt;
        UnitFurnace slC;
        slC = slotFrom.getExitNei();

        reDo = true;
        while(reDo) {
            if (slC == slotTo) {
                reDo = false; // do for the last time
            }
            nowHt = slC.minHeight();
            if (nowHt < lastHt) { // possible obstruction
                lastHt = nowHt; //'note it down so that have to check only thoses lower than this
                obstr = getObstrFact(slC, bVwall); // FOR THE TIME BEING
                if (obstr < obstrMin)
                    obstrMin = obstr;
                if (obstrMin <= 0)
                    reDo = false;
            }
            slC = slC.getExitNei();
         }
        return obstrMin;
    }


    double getObstrFact(UnitFurnace bySlot, boolean bVwall) {
        double obstrN, obstrF; // obstruction near and far
        LineMv lnA, lnB;
        LineMv lnM;  // line connecting mid point of two surfaces
        LineMv lnW; // window line perpendicular to lnM
        DoublePoint pA, pB, fromMidP;
        obstrN = 1;
        obstrF = 1;
        double slotTstPos = slotTo.getStPos();
        double slotFstPos = slotFrom.getStPos();
        double bySlotStPos = bySlot.getStPos();
        double adjustHt = ((bMindChHeight && !bBot) ? chHeight : 0);
        while (true) {
            if (bSlotToOnExit) {
                if (bVwall) {
                    if (slotFrom.bWallFacingExit) {
                        lnA = new LineMv(slotTo.endPos, adjustHt, slotFstPos, slotFrom.vWallTop);
                        lnB = new LineMv(slotTstPos, adjustHt, slotFstPos, slotFrom.vWallBot);
                        fromMidP = new DoublePoint(slotFstPos, (slotFrom.vWallTop + slotFrom.vWallBot) / 2);
                    }
                    else {
                        obstrN = 0;
                        break;
                    }
                }
                else {
                    lnA = new LineMv(slotTo.endPos, adjustHt, slotFrom.endPos, slotFrom.heightExit);
                    lnB = new LineMv(slotTstPos, adjustHt, slotFstPos, slotFrom.heightEntry);
                    fromMidP = new DoublePoint((slotFrom.endPos + slotFstPos) / 2, (slotFrom.heightEntry + slotFrom.heightExit) / 2);
                }
                lnM = new LineMv(fromMidP.x, fromMidP.y,
                                (slotTstPos + slotTo.endPos) / 2, adjustHt); // the slotT is always at hearth level
                // check near edge
                 lnW = lnM.makePerpendLine(bySlotStPos, bySlot.heightEntry);
                 pA = lnW.getIntersection(lnA);
                if (pA.x > bySlotStPos){
                    pB = lnW.getIntersection(lnB);
                    if (pB.x > bySlotStPos) // total interference
                        obstrN = 0;
                    else
                        obstrN = lnW.getLengthWithX(pB.x, bySlotStPos) / lnW.getLengthWithX(pB.x, pA.x);
                }

                //check far edge
                 lnW = lnM.makePerpendLine(bySlot.endPos, bySlot.heightExit);
                 pA = lnW.getIntersection(lnA);
                if (pA.x > bySlot.endPos) {
                     pB = lnW.getIntersection(lnB);
                    if (pB.x > bySlot.endPos) // total interference
                        obstrF = 0;
                    else
                        obstrF = lnW.getLengthWithX(pB.x, bySlot.endPos) / lnW.getLengthWithX(pB.x, pA.x);
                }
            }
            else {
                 if (bVwall) {
                    if (slotFrom.bWallFacingEntry) {
                        lnA = new LineMv(slotTstPos, adjustHt, slotFrom.endPos, slotFrom.vWallTop);
                        lnB = new LineMv(slotTo.endPos, adjustHt, slotFrom.endPos, slotFrom.vWallBot);
                        fromMidP = new DoublePoint(slotFrom.endPos, (slotFrom.vWallTop + slotFrom.vWallBot) / 2);
                    }
                     else {
                        obstrN = 0;
                        break;
                     }
                 }
                else {
                    lnA = new LineMv(slotTstPos, adjustHt, slotFstPos, slotFrom.heightEntry);
                    lnB = new LineMv(slotTo.endPos, adjustHt, slotFrom.endPos, slotFrom.heightExit);
                    fromMidP = new DoublePoint((slotFrom.endPos + slotFstPos) / 2, (slotFrom.heightEntry + slotFrom.heightExit) / 2);
                 }
                lnM = new LineMv(fromMidP.x, fromMidP.y,
                                (slotTstPos + slotTo.endPos) / 2, adjustHt); // the slotT is always at hearth level
                // check near edge
                 lnW = lnM.makePerpendLine(bySlot.endPos, bySlot.heightExit);
                 pA = lnW.getIntersection(lnA);
                if (bySlot.endPos > pA.x) {
                     pB = lnW.getIntersection(lnB);
                    if (bySlot.endPos > pB.x) // total interference
                        obstrF = 0;
                    else
                        obstrF = lnW.getLengthWithX(pB.x, bySlot.endPos) / lnW.getLengthWithX(pB.x, pA.x);
                }

                // check far edge
                 lnW = lnM.makePerpendLine(bySlotStPos, bySlot.heightEntry);
                 pA = lnW.getIntersection(lnA);
                if (bySlotStPos > pA.x) {
                     pB = lnW.getIntersection(lnB);
                    if (bySlotStPos > pB.x) // total interference
                        obstrN = 0;
                    else
                        obstrN = lnW.getLengthWithX(pB.x, bySlotStPos) / lnW.getLengthWithX(pB.x, pA.x);
                }
            }
            break;
        }
        return Math.min(obstrN, obstrF);
    }

    void evalShapeFactor() {
        double localFactor = 1;
        double left1, right1, left2, right2, hL, hR;
        double fceWidth = slotTo.width;
        double adjustHt = ((bMindChHeight && !bBot) ? chHeight : 0);
        hL = slotFrom.heightEntry - adjustHt;
        hR = slotFrom.heightExit - adjustHt;
        if (heatingMode == DFHeating.HeatingMode.TOPBOTSTRIP) {
            // calculate as two half and multiply the result by 2
            hL *= 0.5;
            hR *= 0.5;
            localFactor = 2;
        }
        roofLen = Math.sqrt(Math.pow((hR - hL), 2) + Math.pow(slotFrom.length, 2));
        left1 = slotFrom.endPos - slotFrom.length;
        right1 = slotFrom.endPos;
        left2 = slotTo.endPos - slotTo.length;
        right2 = slotTo.endPos;
        l1 = Math.sqrt(Math.pow((left2 - left1), 2) + Math.pow(hL, 2));
        l2 = Math.sqrt(Math.pow((right2 - right1), 2)+ Math.pow(hR, 2));
        lx1 = Math.sqrt(Math.pow((left2 - right1), 2) + Math.pow(hR, 2));
        lx2 = Math.sqrt(Math.pow((right2 - left1), 2) + Math.pow(hL, 2));
        shapeF = ((lx1 + lx2 - (l1 + l2)) / (2 * roofLen));
        if (shapeF < 0) {
            shapeF = 0;
            shadowFact = 0;
            totalFactor = 0;
        }
        else {
            shadowFact = evalObstrFactor(false);
//            shadowFact = 1;
            totalFactor = shapeF * shadowFact * localFactor;   // with multiplier if TOPBOTSTRIP
        }
        srcArea = fceWidth * roofLen;
        destArea = fceWidth * slotTo.length;
        double dx = (left2 + right2) / 2 - (left1 + right1) / 2;
        double dy = (hL + hR) / 2;
        distRoofCharge = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));  // used only for gas absorption
    }

    void evalWallShapeFactor() {
        double wT, wB;
        double slotTstPos = slotTo.getStPos();
        double slotFstPos = slotFrom.getStPos();
        double adjustHt = ((bMindChHeight && !bBot) ? chHeight : 0);
        double dx = 0, dy = 0;
        if (slotFrom.bWallFacingEntry || slotFrom.bWallFacingExit){
            if (bSlotToOnExit && slotFrom.bWallFacingExit) {
                wT = slotFrom.vWallTop - adjustHt;
                wB = slotFrom.vWallBot - adjustHt;
                if (heatingMode == DFHeating.HeatingMode.TOPBOTSTRIP) {
                    wT *= 0.5;
                    wB *= 0.5;
                }
                if (wB < 0) wB = 0;
                vL1 = Math.sqrt(Math.pow((slotTo.endPos - slotFstPos), 2) + Math.pow(wT, 2));
                vL2 = Math.sqrt(Math.pow((slotTstPos - slotFstPos), 2) + Math.pow(wB, 2));
                vLx1 = Math.sqrt(Math.pow((slotTstPos - slotFstPos), 2) + Math.pow(wT, 2));
                vLx2 = Math.sqrt(Math.pow((slotTo.endPos - slotFstPos), 2) + Math.pow(wB, 2));
                vLen = wT - wB;
                vShapeF = (vLx1 + vLx2 - (vL1 + vL2)) / (2 * vLen);
                dx = (slotTo.endPos + slotTstPos) / 2 - slotFstPos;
                dy = (wT + wB) / 2;

            }
            if (!bSlotToOnExit && slotFrom.bWallFacingEntry) {
                wT = slotFrom.vWallTop - adjustHt;
                wB = slotFrom.vWallBot - adjustHt;
                if (heatingMode == DFHeating.HeatingMode.TOPBOTSTRIP) {
                    wT *= 0.5;
                    wB *= 0.5;
                }
                if (wB < 0) wB = 0;
                vL1 = Math.sqrt( Math.pow((slotFrom.endPos - slotTstPos), 2) +  Math.pow(wT, 2));
                vL2 = Math.sqrt( Math.pow((slotFrom.endPos - slotTo.endPos), 2) +  Math.pow(wB, 2));
                vLx1 = Math.sqrt( Math.pow((slotFrom.endPos - slotTo.endPos), 2) +  Math.pow(wT, 2));
                vLx2 = Math.sqrt( Math.pow((slotFrom.endPos - slotTstPos), 2) +  Math.pow(wB, 2));
                vLen = wT - wB;
                vShapeF = (vLx1 + vLx2 - (vL1 + vL2)) / (2 * vLen);
                dx = (slotTo.endPos + slotTstPos) / 2 - slotFrom.endPos;
                dy = (wT + wB) / 2;
            }
            if (vShapeF < 0) vShapeF = 0;
            vShadowFact = evalObstrFactor(true);
//            vShadowFact = 1;
            vTotalfactor = vShapeF * vShadowFact;
            vSrcArea = slotFrom.width * (slotFrom.vWallTop - slotFrom.vWallBot);
            distWallCharge = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2)); // used only for gas absorption
        }
    }

    public double calculate() {
        interChF = evalInterChF();
        hHeatOut = interChF * totalFactor * SPECIAL.stefenBoltz * srcArea *
                (Math.pow((slotFrom.tempO + 273), 4) - Math.pow((slotTo.tempWO + 273), 4));
        // gas absorption
        double alphaGas =  flue.alphaGas(slotFrom.tempO, slotTo.tempWO, distRoofCharge) * tuning.emmFactor;
        if (tuning.bTakeGasAbsorptionForInterRad)
            hGasAbsorption = alphaGas * totalFactor * srcArea * (slotFrom.tempO - slotTo.tempWO);
        else
            hGasAbsorption = 0;
        if (Math.abs(hGasAbsorption) > Math.abs(hHeatOut))
            debug("hGasAbsorption = " + hGasAbsorption + ", hHeatOut = " + hHeatOut);
        vHeatOut = interChF * vTotalfactor * SPECIAL.stefenBoltz * vSrcArea *
                (Math.pow((slotFrom.tempO + 273), 4) - Math.pow((slotTo.tempWO + 273), 4));
        if (distWallCharge > 0)  {
            alphaGas =  flue.alphaGas(slotFrom.tempO, slotTo.tempWO, distWallCharge) * tuning.emmFactor;
            if (tuning.bTakeGasAbsorptionForInterRad)
                vGasAbsorption = alphaGas * vTotalfactor * vSrcArea * (slotFrom.tempO - slotTo.tempWO);
            else
                vGasAbsorption = 0;
            if (Math.abs(vGasAbsorption) > Math.abs(vHeatOut))
                debug("vGasAbsorption = " + vGasAbsorption + ", vHeatOut = " + vHeatOut);
        }
        heatOut = (hHeatOut - hGasAbsorption) + (vHeatOut - vGasAbsorption);
//        heatOut = (hHeatOut - hGasAbsorption);
        pDestHeatIntensity = heatOut / destArea;
        return heatOut;
    }

    public double getpHeatOut() {
        return heatOut;
    }

    double evalInterChF() {
        return slotTo.eW;
    }

    void debug(String msg) {
        System.out.println("RadToNeighbors " + msg);
    }

}
