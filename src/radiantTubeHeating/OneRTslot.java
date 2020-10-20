package radiantTubeHeating;

import basic.Charge;
import basic.RadiantTube;
import mvUtils.display.SimpleDialog;

import javax.vecmath.Vector3d;
import java.awt.*;
import java.text.DecimalFormat;

import static mvUtils.math.SPECIAL.stefenBoltz;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 16/10/2020
 * Time: 17:56
 * To change this template use File | Settings | File Templates.
 */
public class OneRTslot {
    // Reference book: Heat & mass Transfer by Dr.D. S. Kumar  9th Edition
    boolean doTrace = true;
    double stefBoltz = stefenBoltz;
    double HEATERRLIMIT = 0.001;

    public static String[] colName = {"Time", "LenPos", "TempHE", "tempFce", "tempCh", "HeatCh", "HeatLoss", "HeatHE"};
    public static String[] fmtStr = {"0.000", "#.000", "#,###.0", "#,###.0", "#,###.0", "#,###", "#,###", "#,###"};
    static int nColumn = colName.length;
    double lPos;
    Charge charge;
    RadiantTube rT;

    double slotLen = 1.0;
    double nRts = 1; // proportional number of RTs in the slot
    double rollDia = 0;
    double rollPitch = 0;
    double distChToRtCenter = 0.6;

    RTFurnace rtF;
    // all temperatures in K
    double tempChSt, tempChEnd;
    double tempHe, tempFce;
    double heatCh, heatLoss, heatHe;
    double unitTime, endTime;
    double shapeFHeCh;
    double uChWt;
    double grayFactHeCh, sigGrayHeCh;
    //    double grayFactHeFc, grayFactFcCh;
//    double relRHEtoWall; // for calculation wall temperature
    double resistTotHEtoCharge; // for calculation wall temperature
    double resistWallsToCharge;
    double resistHEtoWall;
    double restistNearHE;

    RTHeating.LimitMode iMode;
    OneRTslot prevSlot;

    public OneRTslot(RTFurnace rtF, double lPos, OneRTslot prevSlot, double slotLen,
                     double nRts, double rollDia, double rollPitch) {
        this.rtF = rtF;
        this.charge = rtF.charge;
        this.rT = rtF.rt;
        this.distChToRtCenter = rtF.rtCenterAbove;
        this.lPos = lPos;
        this.slotLen = slotLen;
        this.rollPitch = rollPitch;
        this.nRts = nRts;
        this.rollDia = rollDia;
        this.prevSlot = prevSlot;
        double effectiveChWidth = rtF.nChargeAlongFceWidth * charge.getProjectedTopArea() / slotLen;
        double visibleChLength = 2 * (1.732 * distChToRtCenter  + rT.dia);
            // with RT pitch as 2 x dia, which is the minimum
            // this is conservative
        shapeFHeCh = getHeChShapeFactor(rT.dia, rT.activeLen, visibleChLength,
                effectiveChWidth, distChToRtCenter, rollDia, rollPitch);

    }

    public OneRTslot(RTFurnace rtF, double lPos, OneRTslot prevSlot) {
        this.rtF = rtF;
        this.charge = rtF.charge;
        this.rT = rtF.rt;
        this.lPos = lPos;
        this.prevSlot = prevSlot;
        shapeFHeCh = effectiveArea12(rtF.uAreaHeCh, rtF.uAreaChHe, rtF.gapHeCh, 0) / rtF.uAreaHeTot;
    }

    // for a dummy slot
    public OneRTslot(OneRTslot nextSlot, double lPos) {
        this.lPos = lPos;
        tempHe = nextSlot.tempHe;
        tempFce = nextSlot.tempFce;
        tempChEnd = nextSlot.tempChSt;
        heatLoss = 0;
        heatHe = 0;
        endTime = 0;
    }
    public void setCharge(Charge charge, double uChWt, double unitTime, double endTime)  {
        this.charge = charge;
        this.uChWt = uChWt;
        this.unitTime = unitTime;
        this.endTime = endTime;
        this.heatLoss = rtF.unitLoss;
    }

    public double getColDataVal(int col) {
        switch(col) {
            case 0:
                return endTime;
            case 1:
                return lPos;
            case 2:
                return tempHe - 273;
            case 3:
                return tempFce - 273;
            case 4:
                return tempChEnd - 273;
            case 5:
                return heatCh;
            case 6:
                return heatLoss;
            case 7:
                return heatHe;
            default:
                return Double.NaN;
        }
    }

    public String getColData(int col) {
        switch(col) {
            case 0:
                return formatNumber(endTime, fmtStr[col]);
            case 1:
                return formatNumber(lPos, fmtStr[col]);
            case 2:
                return formatNumber(tempHe - 273, fmtStr[col]);
            case 3:
                return formatNumber(tempFce - 273, fmtStr[col]);
            case 4:
                return formatNumber(tempChEnd - 273, fmtStr[col]);
            case 5:
                return formatNumber(heatCh, fmtStr[col]);
            case 6:
                return formatNumber(heatLoss, fmtStr[col]);
            case 7:
                return formatNumber(heatHe, fmtStr[col]);
            default:
                return "NO DATA";
        }
    }


    String formatNumber(double value, String fmt) {
        return new DecimalFormat(fmt).format(value);
    }

    String formatNumber(double value) {
        String retVal = null;
        double absVal = Math.abs(value);
        if (absVal == 0)
            retVal = "#";
        else if (absVal < 0.001 || absVal > 1e5)
            retVal = "#.###E00";
        else if (absVal > 100)
            retVal = "###,###";
        else
            retVal = "###.###";
        return new DecimalFormat(retVal).format(value);
    }

    public String oneColName(int col) {
        if (col >= 0 && col < nColumn)
            return colName[col];
        else
            return "UNKNOWN";
    }

    void getGrayFactors() {
        double emissCh = charge.getEmiss(tempChSt);
        grayFactHeCh = 1 /
                ((1 / rT.surfEmiss - 1) + rtF.uAreaHeTot / rtF.uAreaChTot * (1 / emissCh - 1) +
                        (rtF.uAreaHeTot + rtF.uAreaChTot - 2 * rtF.uAreaHeTot * shapeFHeCh) /
                                (rtF.uAreaChTot - rtF.uAreaHeTot * shapeFHeCh * shapeFHeCh) );
        // this has been checked and found to be ok on 20170801
        // Ref page 402, 403
        sigGrayHeCh = stefBoltz * grayFactHeCh * rtF.uAreaHeTot;

        restistNearHE = (1- rT.surfEmiss) / (rtF.uAreaHeTot * rT.surfEmiss);
        resistTotHEtoCharge = 1 / (rtF.uAreaHeTot * grayFactHeCh);
        double f12 = shapeFHeCh;
        double f21 = f12 * rtF.uAreaHeTot / rtF.uAreaChTot;
        double f23 = 1 - f21;
        resistWallsToCharge = 1 / (rtF.uAreaChTot * f23);
        double f13 = 1 - f12;
        resistHEtoWall = 1 / (rtF.uAreaHeTot * f13);
        if (doTrace) {
            System.out.println("grayFactHeCh:" + grayFactHeCh);
            System.out.println("sigGrayHeCh:" + sigGrayHeCh);
            System.out.println("restistNearHE:" + restistNearHE);
            System.out.println("resistTotHEtoCharge:" + resistTotHEtoCharge);
            System.out.println("resistWallsToCharge:" + resistWallsToCharge);
            System.out.println("resistHEtoWall:" + resistHEtoWall);
            double rNearCh = (1 - emissCh) / (emissCh * rtF.uAreaChTot);
            System.out.println("rNearCh:" + rNearCh);
            System.out.println("rtF.uAreaHeTot:" + rtF.uAreaHeTot);
            System.out.println("f12:" + f12 + ", f21:" + f21);
            System.out.println("f13:" + f13 + ", f23:" + f23);
        }

//        grayFactHeFc = 1 /
//            ((1 - rT.surfEmiss) / (rtF.uAreaHeTot *  rT.surfEmiss) +
//                (1 / (rtF.uAreaHeTot * (1 - shapeFHeCh))) );
////        grayFactFcCh = 1 /
////            ((1 - emissCh) / (rtF.uAreaChTot * emissCh) +
////                (1 / (rtF.uAreaHeTot * (1 - shapeFHeCh * rtF.uAreaHeTot / rtF.uAreaChTot))));
////      Corrected i=on 20170804
//        grayFactFcCh = 1 /
//                ((1 - emissCh) / (rtF.uAreaChTot * emissCh) +
//                        (1 / (rtF.uAreaChTot * (1 - shapeFHeCh * rtF.uAreaHeTot / rtF.uAreaChTot))));
//
////        relRHEtoWall = (1- rT.surfEmiss) / (rtF.uAreaHeTot * rT.surfEmiss) +
////                ((1 / (rtF.uAreaHeTot * shapeFHeCh) * (1 / rtF.uAreaChHe * (1 - shapeFHeCh) ))) /
////                        ((1 / (rtF.uAreaHeTot * ((1 - shapeFHeCh)))) + (1 / (rtF.uAreaChHe * f23)));

    }

    static double effectiveArea12(Double area1, double area2, double gap, double angle) {
        // considers the surfaces as squares with centers and side aligned
        // Presently 'angle' is not used ... assumed 0 (parallel)
        int CALCdivs = 5; // do not change,
        int MIDdiv = 2; // do not change, the two are related to each other, may be 11, 5 is better
        double da1da2, w1, w2, dw1;
        double dw2, h1, h2, dh1, dh2;
        double s, cosphi, mdis1;
        double ndis1, mdis2, ndis2, af12sum;

        h1 = Math.sqrt(area1);
        w1 = h1;
        dh1 = h1 / CALCdivs;
        h2 = Math.sqrt(area2);
        w2 = h2;
        dw1 = w1 / CALCdivs;
        dw2 = w2 / CALCdivs;
        dh2 = h2 / CALCdivs;
        da1da2 = dw1 * dh1 * dw2 * dh2;
        af12sum = 0;
        for (int m1 = 0; m1 < CALCdivs; m1++) {
            mdis1 = dw1 * (m1 - MIDdiv);
            for (int n1 = 0; n1 < CALCdivs; n1++) {
                ndis1 = dh1 * (n1 - MIDdiv);
                for (int m2 = 0; m2 < CALCdivs; m2++) {
                    mdis2 = dw2 * (m2 - MIDdiv);
                    for (int n2 = 0; n2 < CALCdivs; n2++) {
                        ndis2 = dh2 * (n2 - MIDdiv);
                        s = Math.sqrt((mdis1 - mdis2) * (mdis1 - mdis2) +
                                (ndis1 - ndis2) * (ndis1 - ndis2) + gap * gap);
                        cosphi = gap / s;
                        af12sum = af12sum + da1da2 * (cosphi * cosphi) / (s * s) / 3.142;
                    }
                }
            }
        }
        if (af12sum > area1) {
            showMessage("In OneSlot settings" , "RT to Charge view Factor is out of range, limiting it");
            af12sum = area1;
        }
        return af12sum;
    }

    static double area1to2(double a1, Vector3d pos1, Vector3d normal1,
                    double a2, Vector3d pos2, Vector3d normal2) {
            // normal1 and normal2 are unit vectors
        double ret_val = 0;
        pos2.sub(pos1);
        double d_sq = pos2.lengthSquared();
        double d = Math.sqrt(d_sq);
        double cos_theta1 = normal1.dot(pos2) / d;
        pos2.negate();
        double cos_theta2 = normal2.dot(pos2) / d;
        if (cos_theta1 > 0 && cos_theta2 > 0) {
            ret_val = a1 * a2 * cos_theta1 * cos_theta2 / d_sq;
        }
        return ret_val;
    }

    static double getHeChShapeFactor(double rt_dia, double rt_len, double surf_len, double surf_width,
                              double dist_rtcl, double roll_dia, double roll_pitch) {
        // rt_len along surf_width and parallel
        int rt_len_stepsN = 5;
        double del_rt_len = rt_len / rt_len_stepsN;

        double rt_r = rt_dia / 2;
        int  rt_theta_stepsN = 16;
        double del_rt_theta = 2 * Math.PI / rt_theta_stepsN;

        double unit_rt_area = rt_r * del_rt_theta * del_rt_len;

        int surf_w_stepsN = 5;
        double del_surf_w = surf_width / surf_w_stepsN;

        int surf_len_stepsN = 10;
        double del_surf_len = surf_len / surf_len_stepsN;

        double unit_surf_area = del_surf_w * del_surf_len;
        // correct for roll shadow  at bottom, ie. on 50% area
        double rollsInSurfLen = 0;
        if (roll_pitch > 0)
            rollsInSurfLen = surf_len / roll_pitch;
        double shadowLen = rollsInSurfLen * roll_dia;
        double shadowAreaFactor = shadowLen / (2 * surf_len);
        // The adjusted unit_surf_area
        unit_surf_area *= (1 - shadowAreaFactor);

        Vector3d norm2 = new Vector3d(0, 0, 1);
        double eff_surf = 0;
        for (double theta = del_rt_theta / 2; theta < 2 * Math.PI; theta += del_rt_theta) {
            double cos_theta = Math.cos(theta);
            double sin_theta = Math.sin(theta);
            Vector3d norm1 = new Vector3d(cos_theta, 0, sin_theta);

            for (double y1 = del_rt_len / 2 - rt_len / 2;
                 y1 < rt_len - rt_len / 2; y1 += del_rt_len) {
                Vector3d pos1 = new Vector3d(rt_r * cos_theta, y1, dist_rtcl + rt_r * sin_theta);
                for (double x2 = del_surf_len / 2 - surf_len / 2;
                     x2 < surf_len - surf_len / 2; x2 += del_surf_len) {
                    for (double y2 = del_surf_w / 2 - surf_width / 2;
                         y2 < surf_width - surf_width / 2; y2 += del_surf_w) {
                        Vector3d pos2 = new Vector3d(x2, y2, 0);
                        double a = area1to2(unit_rt_area, pos1, norm1, unit_surf_area, pos2, norm2);
                        eff_surf += a / Math.PI;
                    }
                }
            }
        }
        return eff_surf / (Math.PI * rt_dia * rt_len);
    }

    double chargeEndTempOLD(double tempChIn, double tempSrc) {
        double tempChE1, tempChE2, tempChMean;
        double heat1, heat2, chHeat;
        double err;
        double ERRLIMIT = 0.1;
        heat1 = charge.getHeatFromTemp(tempChIn - 273);
        tempChE1 = tempChIn + 10; // assume
        tempChMean = (tempChE1 + tempChIn) / 2;
        heat2 = heat1 + sigGrayHeCh * (Math.pow(tempSrc, 4.0) - Math.pow(tempChMean, 4.0)) / rtF.production;
        tempChE2 = charge.getTempFromHeat(heat2) + 273;
        err = tempChE2 - tempChE1;
        while (Math.abs(err) > ERRLIMIT) {
            tempChE1 = tempChE1 + err / 2;
            tempChMean = (tempChE1 + tempChIn) / 2;
            heat2 = heat1 + sigGrayHeCh * (Math.pow(tempSrc, 4.0) - Math.pow(tempChMean, 4.0)) /
                    rtF.production;
            tempChE2 = charge.getTempFromHeat(heat2) + 273;
            err = tempChE2 - tempChE1;
        }
        return tempChE2;
    }

    double chargeEndTemp(double tempChIn, double tempSrc) {
        double tempChE1, tempChE2, tempChMean;
        double heat1, heat2, chHeat;
        double err;
        double ERRLIMIT = 0.1;
        heat1 = charge.getHeatFromTemp(tempChIn - 273);
        tempChE1 = tempChIn + 10; // assume
        tempChMean = (tempChE1 + tempChIn) / 2;
        heat2 = heat1 + sigGrayHeCh * (Math.pow(tempSrc, 4.0) - Math.pow(tempChMean, 4.0)) / rtF.production;
        tempChE2 = charge.getTempFromHeat(heat2) + 273;
        err = tempChE2 - tempChE1;
        while (Math.abs(err) > ERRLIMIT) {
            tempChE1 = tempChE1 + err / 2;
            tempChMean = (tempChE1 + tempChIn) / 2;
            heat2 = heat1 + sigGrayHeCh * (Math.pow(tempSrc, 4.0) - Math.pow(tempChMean, 4.0)) /
                    rtF.production;
            tempChE2 = charge.getTempFromHeat(heat2) + 273;
            err = tempChE2 - tempChE1;
        }
        return tempChE2;
    }

//    double furnaceTemp(double tempChMean, double tempSrc) {
//        return Math.pow(((grayFactHeFc * Math.pow(tempSrc, 4) + grayFactFcCh * Math.pow(tempChMean, 4)) /
//                            (grayFactHeFc + grayFactFcCh)), 0.25);
//    }
//  Corrected on 20170804

    double furnaceTemp(double tempChMean, double tempSrc) {
        // rt = 1, charge = 2, Fce = 3
//        double f32 = grayFactFcCh;
//        double f13 = grayFactHeFc;
//        double f31 = f13 * rtF.uAreaHeTot / rtF.uAreaWalls;
//        return Math.pow(((f32 * Math.pow(tempSrc, 4) + f31 * Math.pow(tempChMean, 4)) /
//                (f32 + f31)), 0.25);

        double v1 = Math.pow(tempSrc, 4);
        double v2 = Math.pow(tempChMean, 4);
        double current = (v1 - v2 ) / resistTotHEtoCharge;
        double emissCharge = charge.getEmiss(tempChMean);
        double resistNearCharge = (1 - emissCharge) / (emissCharge * rtF.uAreaChTot);

        double v3 = (resistWallsToCharge * (v1 - current * restistNearHE) +
                resistHEtoWall * (v2 + current * resistNearCharge)) /
                (resistWallsToCharge + resistHEtoWall);
        return Math.pow(v3, 0.25);
    }

//    double furnaceTempApsXLVersion(double tempChMean, double tempSrc) {
//        return Math.pow(((grayFactHeFc * Math.pow(tempHe, 4) + grayFactFcCh * Math.pow(tempChMean, 4)) /
//                (grayFactHeFc + grayFactFcCh)), 0.25);
//    }
//

    public double calculate() {
        if (prevSlot != null)
            return calculate(prevSlot.tempChEnd, prevSlot.tempHe, prevSlot.detltaChTemp(), prevSlot.heatHe, prevSlot.iMode);
        else
            return Double.NaN;
    }

    public double calculate(double fromTemp, double srcTemp, double prevDeltaTemp, double srcHeat, RTHeating.LimitMode iMode) {
        tempChSt = fromTemp;
        getGrayFactors();
        double tempChB, tempChE1, tempChE2;
        double tempChMean;
        double tempHe1, tempHe2;
        double chHeatContB, chHeatContE;
        double heatHe1;
        double errHeat;
        this.iMode = iMode;
        if (prevDeltaTemp == 0)
            tempChE1 = tempChSt + 0.1;
        else
            tempChE1 = tempChSt + prevDeltaTemp;

        chHeatContB = charge.getHeatFromTemp(tempChSt - 273);
        if (iMode == RTHeating.LimitMode.RTTEMP)
            tempHe1 = srcTemp;
        else {//it is in heat limited mode
            if (prevSlot == null)
                tempHe1 = tempChSt + 0.1;
            else
                tempHe1 = prevSlot.tempHe;
        }
        while (true) { // heat limit check loop
            tempChE2 = chargeEndTemp(tempChSt, tempHe1);
            if (iMode == RTHeating.LimitMode.RTTEMP)  // exit if not heat limit
                break;
            tempChMean = (tempChSt + tempChE2) / 2;
            heatHe1 = sigGrayHeCh * (Math.pow(tempHe1, 4) - Math.pow(tempChMean, 4)) + heatLoss;
            errHeat = heatHe1 - srcHeat;
            if (Math.abs(errHeat / heatHe1) < HEATERRLIMIT)
                break; // allok
            tempHe2 = tempChMean + (tempHe1 - tempChMean) / heatHe1 * srcHeat;
            tempHe1 = (tempHe1 + tempHe2) / 2;
        }
        tempChMean = (tempChSt + tempChE2) / 2;
        tempChEnd = tempChE2;
        tempHe = tempHe1;
        tempFce = furnaceTemp(tempChMean, tempHe1);
        heatCh = (charge.getHeatFromTemp(tempChE2 - 273) - chHeatContB) * rtF.production;
        heatHe = sigGrayHeCh * (Math.pow(tempHe1, 4) - Math.pow(tempChMean, 4)) + heatLoss;
        return tempChEnd;
    }

    double detltaChTemp() {
        return tempChEnd - tempChSt;
    }

    static public void showMessage(String title, String msg) {
        SimpleDialog.showMessage(null, title, msg);
    }


    public static void main (String[] arg) {
        double roll_dia = 0.2;
        double roll_pitch = 0.6;
        double rt_dia = 0.198;
        double rt_len = 1.4;
        double surf_width = 1.25;
        double dist_rtcl = 0.9;
        double nRTsPerM = 3;
        double surf_len =  2 * (1.732 * dist_rtcl  + rt_dia);

        double area1 = 1.57 * rt_dia * rt_len * nRTsPerM;
        double area2 = surf_width * 1 * 2;
        double gap = dist_rtcl;
        double one_rt_area = Math.PI * rt_dia * rt_len;
        double tot_rt_area = one_rt_area * nRTsPerM;

//        double effA = OneRTslot.effectiveArea12(area1, area2, gap, 0);
//        System.out.print("effectiveArea12() area1: " + area1 + ", area2: " + area2 + ", gap: " + gap +
//                ", EffArea: " + effA);
//        System.out.print("\nHeChFactor = " + effA / tot_rt_area);
        System.out.print("\ngetHeChShapeFactor() with roll: " +
                OneRTslot.getHeChShapeFactor(rt_dia, rt_len, surf_len, surf_width,
                        dist_rtcl, roll_dia, roll_pitch));
        System.out.print("\ngetHeChShapeFactor() without roll: " +
                OneRTslot.getHeChShapeFactor(rt_dia, rt_len, surf_len, surf_width,
                        dist_rtcl, 0, 0));
    }

}

