package directFiredHeating.transientData;

import directFiredHeating.DFHFurnace;
import directFiredHeating.UnitFurnace;

import java.util.Dictionary;
import java.util.Vector;

public class OneCombinedAmbient {
    DFHFurnace furnace;
    TwoDCharge twoDCharge;
    public double startTime, endTime;
    public double topAmbTemp, topAlpha;
    public double botAmbTemp, botAlpha;
    public double sideAmbTemp, sideAlpha;
    public UnitFurnace topUf, botUf;
    int ySize, zSize;
    double totInternalCells;
    // calculated results
    double calculatedEndTime;
    double[][] allCellsTemps;
    Vector<double[]> borderTemps;
            // in order BOTTOMFACE, WIDTHNEARFACE, TOPFACE, WIDTHFARTHERFACE
    double topSurfMeanTemp, botSurfMeanTemp;
    double nearSideSurfMeanTemp, farSideSurfMeanTemp;
    double meanTemp, minimumTemp, maximumTemp;
    double s152Top, s152Bot;
    double tauTop, tauBot;

    public OneCombinedAmbient(DFHFurnace furnace, TwoDCharge twoDCharge,
                              UnitFurnace topUf, UnitFurnace botUf) {
        this.furnace = furnace;
        this.twoDCharge = twoDCharge;
        ySize = twoDCharge.ySize;
        zSize = twoDCharge.zSize;
        totInternalCells = twoDCharge.totInternalCells;
        this.topUf = topUf;
        this.startTime = topUf.endTime - topUf.delTime;
        endTime = topUf.endTime;
        if (furnace.bTopBot)
            this.botUf = botUf;
        setAmbValues();
    }

    void setAmbValues() {
        topAmbTemp = topUf.avgGasTemp();
        topAlpha = topUf.getAlpha();
        if (furnace.bTopBot) {
            botAmbTemp = botUf.avgGasTemp();
            botAlpha = botUf.getAlpha();
            sideAmbTemp = (topAmbTemp + botAmbTemp) / 2;
            sideAlpha = (topAlpha + botAlpha) * furnace.sideAlphaFactor;
        }
        else {
            botAmbTemp = 0;
            botAlpha = 0;
            sideAmbTemp = topAmbTemp;
            sideAlpha = topAlpha * furnace.sideAlphaFactor;
        }
    }

    void setS152inUnitFurnaces(double upperLimit) {
        double value = s152Top;
        if (value <= 0)
            value = upperLimit;
        topUf.sets152(Math.min(value, upperLimit));
        if (furnace.bTopBot) {
            value = s152Bot;
            if (value <= 0)
                value = upperLimit;
            botUf.sets152(Math.min(value, upperLimit));
        }
    }

    void noteResultsInUnitFurnaces() {
        topUf.tempWmean2d = meanTemp;
        topUf.tempWmin2d = minimumTemp;
        topUf.tempWO2d = topSurfMeanTemp;
        topUf.chTau2d = tauTop;
        topUf.s5122d = s152Top;
        if (furnace.bTopBot) {
            botUf.tempWmean2d = meanTemp;
            botUf.tempWmin2d = minimumTemp;
            botUf.tempWO2d = botSurfMeanTemp;
            botUf.chTau2d = tauBot;
            botUf.s5122d = s152Bot;

        }
    }


    void getStatistics() {
        double tot = 0;
        minimumTemp = 10000;
        maximumTemp = -10000;
        double val;
        for (int y = 1; y < (ySize - 1); y++) {
            for (int z = 1; z < (zSize - 1); z++) {
                val = allCellsTemps[y][z];
                if (val > maximumTemp)
                    maximumTemp = val;
                if (val < minimumTemp)
                    minimumTemp = val;
                tot += val;
            }
        }
        meanTemp = tot / totInternalCells;
        double[] surfTemp;
        if (furnace.bTopBot) {
            // botSurfMeanTemp
            tot = 0;
            surfTemp = borderTemps.get(0);
            for (int i = 1; i < surfTemp.length - 1; i++) {
                val = surfTemp[i];
                if (val > maximumTemp)
                    maximumTemp = val;
                if (val < minimumTemp)
                    minimumTemp = val;
                tot += val;
            }
            botSurfMeanTemp = tot / (surfTemp.length - 2);
        }
        // nearSideSurfMeanTemp
        tot = 0;
        surfTemp = borderTemps.get(1);
        for (int i = 1; i < surfTemp.length - 1; i++) {
            val = surfTemp[i];
            if (val > maximumTemp)
                maximumTemp = val;
            if (val < minimumTemp)
                minimumTemp = val;
            tot += val;
        }
        nearSideSurfMeanTemp = tot / (surfTemp.length - 2);
        // topSurfMeanTemp
        tot = 0;
        surfTemp = borderTemps.get(2);
        for (int i = 1; i < surfTemp.length - 1; i++) {
            val = surfTemp[i];
            if (val > maximumTemp)
                maximumTemp = val;
            if (val < minimumTemp)
                minimumTemp = val;
            tot += val;
        }
        topSurfMeanTemp = tot / (surfTemp.length - 2);
        // farSideSurfMeanTemp
        tot = 0;
        surfTemp = borderTemps.get(3);
        for (int i = 1; i < surfTemp.length - 1; i++) {
            val = surfTemp[i];
            if (val > maximumTemp)
                maximumTemp = val;
            if (val < minimumTemp)
                minimumTemp = val;
            tot += val;
        }
        farSideSurfMeanTemp = tot / (surfTemp.length - 2);
        s152Top = (topSurfMeanTemp - minimumTemp) / (topSurfMeanTemp - meanTemp);
        tauTop = (topAmbTemp - topSurfMeanTemp) / (topAmbTemp - meanTemp);
        if (furnace.bTopBot) {
            s152Bot = (botSurfMeanTemp - minimumTemp) / (botSurfMeanTemp - meanTemp);
            tauBot = (botAmbTemp - botSurfMeanTemp) / (botAmbTemp - meanTemp);
        }
    }

    void noteResults(double time, double[][] allCellsTemps, Vector<double[]> borderTemps) {
        calculatedEndTime= time;
        this.allCellsTemps = allCellsTemps;
        this.borderTemps = borderTemps;
        getStatistics();
        noteResultsInUnitFurnaces();
    }
}
