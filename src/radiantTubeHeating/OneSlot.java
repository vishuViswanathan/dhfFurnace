package radiantTubeHeating;

import basic.Charge;
import basic.RadiantTube;

import javax.swing.*;
import java.text.DecimalFormat;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/23/12
 * Time: 12:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class OneSlot {
    double stefBoltz = 0.0000000496;
    double TEMPERRLIMIT = 0.1;
    double HEATERRLIMIT = 0.001;

    public static String[] colName = {"Time", "LenPos", "TempHE", "tempFce", "tempCh", "HeatCh", "HeatLoss", "HeatHE"};
    static int nColumn = colName.length;
    double lPos;
    Charge charge;
    RadiantTube rT;
    RTFurnace rtF;
    // all temperatures in K
    double tempChSt, tempChEnd;
    double tempHe, tempFce;
    double heatCh, heatLoss, heatHe;
    double unitTime, endTime;
    double shapeFHeCh;
    double uChWt;
    double grayFactHeCh, sigGrayHeCh, grayFactHeFc, grayFactFcCh;
    RTFurnace.CalculMode iMode;
    OneSlot prevSlot;

    public OneSlot(RTFurnace rtF, double lPos, OneSlot prevSlot) {
        this.rtF = rtF;
        this.charge = rtF.charge;
        this.rT = rtF.rt;
        this.lPos = lPos;
        this.prevSlot = prevSlot;
        shapeFHeCh = shapeFactor12(rtF.uAreaHeCh, rtF.uAreaChHe, rtF.gapHeCh, 0) / rtF.uAreaHeTot;
     }

    // for a dummy slot
    public OneSlot(OneSlot nextSlot, double lPos) {
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
                return formatNumber(endTime, "00.000");
            case 1:
                return formatNumber(lPos, "#0.00");
            case 2:
                return formatNumber(tempHe - 273, "#,###");
            case 3:
                return formatNumber(tempFce - 273, "#,###");
            case 4:
                return formatNumber(tempChEnd - 273, "#,###");
            case 5:
                return formatNumber(heatCh);
            case 6:
                return formatNumber(heatLoss);
            case 7:
                return formatNumber(heatHe);
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
        sigGrayHeCh = stefBoltz * grayFactHeCh * rtF.uAreaHeTot;
        grayFactHeFc = 1 /
            ((1 - rT.surfEmiss) / (rtF.uAreaHeTot *  rT.surfEmiss) +
                (1 / (rtF.uAreaHeTot * (1 - shapeFHeCh))) );
        grayFactFcCh = 1 /
            ((1 - emissCh) / (rtF.uAreaChTot * emissCh) +
                (1 / (rtF.uAreaHeTot * (1 - shapeFHeCh * rtF.uAreaHeTot / rtF.uAreaChTot))));
    }

    double shapeFactor12(Double area1, double area2, double gap, double angle) {
        int CALCdivs = 5;
        int MIDdiv = 2;
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
        return af12sum;
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
            heat2 = heat1 + sigGrayHeCh * (Math.pow(tempSrc, 4.0) - Math.pow(tempChMean, 4.0)) / rtF.production;
            tempChE2 = charge.getTempFromHeat(heat2) + 273;
            err = tempChE2 - tempChE1;
        }
        return tempChE2;
    }

    double furnaceTemp(double tempChMean, double tempSrc) {
        return Math.pow(((grayFactHeFc * Math.pow(tempSrc, 4) + grayFactFcCh * Math.pow(tempChMean, 4)) /
                            (grayFactHeFc + grayFactFcCh)), 0.25);
    }

    public double calculate() {
        if (prevSlot != null)
            return calculate(prevSlot.tempChEnd, prevSlot.tempHe, prevSlot.detltaChTemp(), prevSlot.heatHe, prevSlot.iMode);
        else
            return Double.NaN;
    }

    public double calculate(double fromTemp, double srcTemp, double prevDeltaTemp, double srcHeat, RTFurnace.CalculMode iMode) {
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
        if (iMode == RTFurnace.CalculMode.RTTEMP)
                tempHe1 = srcTemp;
        else {//it is in heat limited mode
            if (prevSlot == null)
                tempHe1 = tempChSt + 0.1;
            else
                tempHe1 = prevSlot.tempHe;
        }
        while (true) { // heat limit check loop
            tempChE2 = chargeEndTemp(tempChSt, tempHe1);
            if (iMode == RTFurnace.CalculMode.RTTEMP)  // exit if not heat limit
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
}
