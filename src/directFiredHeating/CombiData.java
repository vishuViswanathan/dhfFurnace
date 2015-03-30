package directFiredHeating;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 9/25/12
 * Time: 2:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class CombiData {
    Vector<UnitFurnace> vUfT, vUfB;
    Vector<OneCombiSlot> vCombiSlots;
    double length;

/*
    public CombiData(Vector<UnitFurnace> vUfT, Vector<UnitFurnace > vUfB) {
        this.vUfT = vUfT;
        this.vUfB = vUfB;
        vCombiSlots = new Vector<OneCombiSlot>();
        int maxT = vUfT.size() - 1;
        int maxB = vUfB.size() - 1;
        int uT = 1, uB = 1;
        double lastEndPos = -1;
        UnitFurnace ufT, ufB;
        while (uT < maxT && uB < maxB) {
            vCombiSlots.add(new OneCombiSlot(uT, uB));
            uT++;
            uB++;
            ufT = vUfT.get(uT);
            ufB = vUfB.get(uB);
            if (ufT.endPos < ufB.endPos)
                uT++;
            if (ufT.endPos > ufB.endPos)
                uB++;
            if (ufT.endPos == lastEndPos) {
                uT++;
                uB++;
            }
            lastEndPos = ufT.endPos;
        }
        length = vUfT.get(maxT - 1).endPos;
    }
*/

    public CombiData(Vector<UnitFurnace> vUfT, Vector<UnitFurnace > vUfB) {
        this.vUfT = vUfT;
        this.vUfB = vUfB;
        vCombiSlots = new Vector<OneCombiSlot>();
        int maxT = vUfT.size() - 1;
        int maxB = vUfB.size() - 1;
        int uT = 0, uB = 0;
        double lastEndPos = -1;
        UnitFurnace ufT, ufB;
        while (uT < maxT && uB < maxB) {
            uT++;
            uB++;
            ufT = vUfT.get(uT);
            ufB = vUfB.get(uB);
            if ((ufT.length <= 0) || (ufT.endPos < ufB.endPos)) {
                uT++;
                if (uT >= maxT)
                    break;
                ufT = vUfT.get(uT);
            }
            if ((ufB.length <= 0) || (ufT.endPos > ufB.endPos)) {
                uB++;
                if (uB >= maxB)
                    break;
                ufB = vUfB.get(uB);
            }
            vCombiSlots.add(new OneCombiSlot(uT, uB));
        }
        length = vUfT.get(maxT - 1).endPos;
    }

    double rmsGdiff() {
        double sum = 0;
        for (int s = 0; s < vCombiSlots.size(); s++)
            sum += vCombiSlots.get(s).gRErrSqLen;
        return Math.sqrt(sum / length);
    }

    void noteCorrection() {
        for (int s = 0; s < vCombiSlots.size(); s++)
            vCombiSlots.get(s).noteCorrection();
    }

    class OneCombiSlot {
         int iT, iB;
         UnitFurnace ufT, ufB;
         double gRatioT, chHeatT, chHeatB, actGratioT, suggGRatioT;
         double gRErrSqLen;

         OneCombiSlot(int iT, int iB) {
             this.iT = iT;
             this.iB = iB;
             ufT = vUfT.get(iT);
             ufB = vUfB.get(iB);
             gRatioT = ufT.gRatio;
             actGratioT = ufT.chargeHeat / (ufT.chargeHeat + ufB.chargeHeat);
             double diff = actGratioT - ufT.gRatio;
             gRErrSqLen = Math.pow(diff, 2) * ufT.length;
             suggGRatioT = ufT.gRatio + diff * 0.25;
             if (suggGRatioT > 0.95)
                 suggGRatioT = 0.95;
         }

         void noteCorrection() {
             ufT.gRatio = suggGRatioT;
             ufB.gRatio = 1 - suggGRatioT;
         }
     }

}
