package linkTFM;

import directFiredHeating.DFHFurnace;
import directFiredHeating.FceSection;
import mvXML.DoubleWithErrStat;
import mvXML.ValAndPos;
import mvXML.XMLgroupStat;
import mvXML.XMLmv;
import mvmath.SPECIAL;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 3/25/13
 * Time: 5:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class OneTFMsec {
    Vector<PosHeight> profPoints;
    double length;
    String secType;
    int nSub;
    int secNum;
    boolean bBot;
    class PosHeight {
        double position, height;

        PosHeight(double position, double height) {
            this.position = position;
            this.height = height;
        }
    }
    OneTFMsec(String xmlStr, int secNum, XMLgroupStat grpStat, String secPos, boolean bBot) {
        this.secNum = secNum;
        this.bBot = bBot;
        DoubleWithErrStat dblWithStat;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "Length", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, secPos + "Length", grpStat)).allOK)
            length = dblWithStat.val;
        vp = XMLmv.getTag(xmlStr, "cbSecType", 0);
        secType = vp.val.trim();
        vp = XMLmv.getTag(xmlStr, "subsections", 0);
        String subStr = vp.val;
        if (subStr.length() > 10) {
            vp = XMLmv.getTag(xmlStr, "nSub", 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, secPos + "nSub", grpStat)).allOK)   {
                nSub = (int)dblWithStat.val;
                if (nSub > 0) {
                    String ss;
                    String ssStr;
                    String subSecPos;
                    double pos =0, ht =0;
                    profPoints = new Vector<PosHeight>(nSub);
                    for (int i = 0; i < nSub; i++) {
                        ss = "ss" + ("" + i).trim();
                        subSecPos = secPos + ss + ".";
                        vp = XMLmv.getTag(subStr, ss, 0);
                        ssStr = vp.val;
                        if (ssStr.length() > 10) {
                            vp = XMLmv.getTag(ssStr, "Position", 0);
                            if ((dblWithStat = new DoubleWithErrStat(vp.val, subSecPos + "Position", grpStat)).allOK)
                                pos = dblWithStat.val;
                            vp = XMLmv.getTag(ssStr, "Height", 0);
                            if ((dblWithStat = new DoubleWithErrStat(vp.val, subSecPos + "Height", grpStat)).allOK)
                                ht = dblWithStat.val;
                            profPoints.add(new PosHeight(pos, ht));
                        }
                        else
                            grpStat.addStat(false, "   " + subSecPos + " data not available\n");
                    }
                    trimPosHtList();
                }
                else
                  grpStat.addStat(false, "   " + secPos + "nSub Value is 0\n");
            }
        }
        else
            grpStat.addStat(false, "   " + secPos + "subsections data not available\n");
     }

    void trimPosHtList() {
        if (profPoints != null) {
            PosHeight now, prev, next;
            int len = profPoints.size();
            double calculHt, slope;
            if (len > 2) {
                for (int p = 1; p < (len - 1); p++) {
                    now = profPoints.get(p);
                    prev = profPoints.get(p - 1);
                    next = profPoints.get(p + 1);
                    // evalute the now height based on the neighbours
                    slope = (next.height - prev.height) / (next.position - prev.position);
                    calculHt = prev.height + slope * (now.position - prev.position);
                    if (Math.abs(calculHt - now.height) < 0.001)  {
//                    if ((next.height == prev.height) && (now.height == prev.height)) {
                        profPoints.remove(p);
                        len = profPoints.size();
                        p--;
                    }
                }
            }
            nSub = len;
        }
    }

    boolean setOneSec(DFHFurnace furnace, String profLevel, XMLgroupStat grpStat) {
        boolean bRetVal = true;
        String secName = profLevel + "Zone" + ("" + (secNum + 1)).trim() + ".";
        furnace.setSectionType(bBot, secNum, !secType.equalsIgnoreCase("With Burners"));
        int ssNum = 0;
        double lenSofar = 0;
        if (nSub > 1) {
            double stPos, endPos, len, stHt, endHt;
            PosHeight ph = profPoints.get(0);
            stPos = SPECIAL.roundToNDecimals(ph.position, 6);
            stHt = ph.height;
            for (int p = 1; p < nSub; p++) {
                if (ssNum >= FceSection.MAXSUBSECTIONS)   {
                    grpStat.addStat(false, "   " + secName + "Too Many Profile Sections\n") ;
                    break;
                }
                ph = profPoints.get(p);
                if (p == (nSub - 1))
                    len = length - lenSofar; // to take care of any rounding error of lengths
                else
                    len = SPECIAL.roundToNDecimals(ph.position, 6) - stPos;
                if (len <= 0) {
                    stHt =  ph.height;
                    continue;
                 }
                endHt = ph.height;
                furnace.changeSubSecData(bBot, secNum, ssNum, len, stHt, endHt, 0);
                lenSofar += len;
                ssNum++;
                stHt = endHt;
                stPos = SPECIAL.roundToNDecimals(ph.position, 6);
            }
        }
        else {
            grpStat.addStat(false, "   " + secName + "Less Than Two Points\n");
            bRetVal = false;
        }
        return bRetVal;
    }
}

