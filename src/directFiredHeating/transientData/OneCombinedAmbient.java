package directFiredHeating.transientData;

import directFiredHeating.DFHFurnace;
import directFiredHeating.UnitFurnace;

import java.text.DecimalFormat;
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
            botAlpha = botUf.getAlpha() * (1 - furnace.productionData.bottShadow);
            sideAmbTemp = (topAmbTemp + botAmbTemp) / 2;
            sideAlpha = (topAlpha + botAlpha) / 2 * furnace.sideAlphaFactor;
        }
        else {
            botAmbTemp = 0;
            botAlpha = 0;
            sideAmbTemp = topAmbTemp;
            sideAlpha = topAlpha * furnace.sideAlphaFactor;
        }
    }

    void setS152inUnitFurnaces(double lowerLimit, double upperLimit) {
        topUf.sets152(Math.max(Math.min(s152Top, upperLimit), lowerLimit));
        if (furnace.bTopBot) {
            botUf.sets152(Math.max(Math.min(s152Bot, upperLimit), lowerLimit));
        }
    }

    void setTauInUnitFurnaces(double lowerLimit, double upperLimit) {
        topUf.setTau(Math.max(Math.min(tauTop, upperLimit), lowerLimit));
        if (furnace.bTopBot) {
            botUf.setTau(Math.max(Math.min(tauBot, upperLimit), lowerLimit));
        }
    }

    void noteResultsInUnitFurnaces() {
        topUf.tempWmean2d = meanTemp;
        topUf.tempWmin2d = minimumTemp;
        topUf.tempWO2d = topSurfMeanTemp;
        topUf.chTau2d = tauTop;
        topUf.s5122d = s152Top;
        topUf.upload2dData();
        if (furnace.bTopBot) {
            botUf.tempWmean2d = meanTemp;
            botUf.tempWmin2d = minimumTemp;
            botUf.tempWO2d = botSurfMeanTemp;
            botUf.chTau2d = tauBot;
            botUf.s5122d = s152Bot;
            botUf.upload2dData();
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

    String cellTempStr(DecimalFormat fmt){
        StringBuilder data = new StringBuilder("[");
        for (int y = 0; y < ySize; y++) {
            data.append((y == 0) ? "[": ",[");
            for (int z = 0; z < zSize; z++) {
                data.append((z == 0) ? "" : ",").append(fmt.format(allCellsTemps[y][z]));
            }
            data.append("]");
        }
        data.append("]");
        return data.toString();
    }

    String borderTempsStr(DecimalFormat fmt) {
        StringBuilder data = new StringBuilder("[");
        for (int i = 0;i < borderTemps.size();i++) {
            data.append((i == 0) ? "[": ",[");
            double[] oneSet = borderTemps.get(i);
            for (int j = 0; j < oneSet.length; j++) {
                data.append((j == 0) ? "" : ",").append(fmt.format(oneSet[j]));
            }
            data.append("]");
        }
        data.append("]");
        return data.toString();
    }

    static String dataHeaderInCSV() {
        return("pos(m);startTime;endTime;minimumTemp;meanTemp" +
                ";topSurfMeanTemp;tauTop;s152Top" +
                ";botSurfMeanTemp;tauBot;s152Bot" +
                ";allCellsTemps;BotFrontTopBackTemps\n");
    }

    public String resultsInCSV() {
        DecimalFormat fmtTime = new DecimalFormat("0.000000");
        DecimalFormat fmt = new DecimalFormat("0.00");
        StringBuilder str = new StringBuilder();
        str.append(fmt.format(topUf.getStPos()) + ";" + fmtTime.format(startTime) + ";" + fmtTime.format(endTime))
                .append(";" + fmt.format(minimumTemp) + ";" + fmt.format(meanTemp))
                .append(";" + fmt.format(topSurfMeanTemp) + ";" + fmt.format(tauTop) + ";" + fmt.format(s152Top))
                .append(";" + fmt.format(botSurfMeanTemp) + ";" + fmt.format(tauBot) + ";" + fmt.format(s152Bot))
                .append(";" + cellTempStr(fmt) + ";" + borderTempsStr(fmt) + "\n");
        return str.toString();
    }
}
