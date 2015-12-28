package level2;

import mvUtils.math.DoubleRange;
import mvUtils.math.XYArray;
import performance.stripFce.PerformanceTable;
import performance.stripFce.ZonalFuelProfile;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 04-Feb-15
 * Time: 4:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2ZonalFuelProfile extends ZonalFuelProfile {
    L2DFHeating l2DFHeating;
    // modified fuel tables with lower and upper end extensions to zonal fuel ranges
    Double[][] l2TopZoneFuels;
    Double[][] l2TopZoneFuelHeat; // total heat comprising combustion + fuel sensible + air sensible
    Double[][] l2BotZoneFuels;
    Double[][] l2BotZoneFuelHeat; // total heat comprising combustion + fuel sensible + air sensible
    protected XYArray l2SpeedTotalFuelTop, l2SpeedTotalFuelBot;
    protected XYArray l2SpeedTotalFHTop, l2SpeedTotalFHBot; // based on FuelHeat (Combustion + air and fuel sensible
    int extendedSteps;

    L2ZonalFuelProfile(PerformanceTable perfTable, int outputSteps, L2DFHeating l2DFHeating) {
        super(perfTable, outputSteps - 2, l2DFHeating);  // later 2 ends are added
        this.l2DFHeating = l2DFHeating;
        extendedSteps = outputSteps;
    }

    public boolean prepareFuelTable(double stripWidth, double stripThickness) {  // TODO only top zones are handled
        boolean bRetVal = false;
        if (super.prepareFuelTable(stripWidth, stripThickness)) {
            extrapolateEnds();
            l2SpeedTotalFuelTop = l2SpeedFuelArray(false);
//            l2SpeedTotalFHTop =  l2SspeedFHArray(false);
            if (nBotZones > 0) {
                l2SpeedTotalFuelBot = l2SpeedFuelArray(true);
//                l2SpeedTotalFHBot = l2SpeedFHArray(false);
            }
            bRetVal = true;
        }

        return bRetVal;
    }

    public void extrapolateEnds() {
        FurnaceSettings fceSettings = l2DFHeating.l2Furnace.furnaceSettings;
        DoubleRange[] zoneFuelRange = fceSettings.getZoneFuelRange();
        DoubleRange totFuelRange = fceSettings.getTotalFuelRange();
        // top fuels
        l2TopZoneFuels = getCalculatedData(false, false);
        l2TopZoneFuels[0][TotFuelCol] = totFuelRange.min;
        for (int z = 0; z < zoneFuelRange.length; z++)
            l2TopZoneFuels[0][FirstZoneCol + z] = zoneFuelRange[z].min;
        double fuelFactorAtMin = (l2TopZoneFuels[0][TotFuelCol] - l2TopZoneFuels[1][TotFuelCol]) /
                (l2TopZoneFuels[1][TotFuelCol] - l2TopZoneFuels[2][TotFuelCol]);
        l2TopZoneFuels[0][USpeedCol] =  l2TopZoneFuels[1][USpeedCol]   +
                fuelFactorAtMin * (l2TopZoneFuels[1][USpeedCol]- l2TopZoneFuels[2][USpeedCol]);
        l2TopZoneFuels[0][UOutputCol] =  l2TopZoneFuels[1][UOutputCol]   +
                fuelFactorAtMin * (l2TopZoneFuels[1][UOutputCol]- l2TopZoneFuels[2][UOutputCol]);

        l2TopZoneFuels[extendedSteps - 1][TotFuelCol] = totFuelRange.max;
        for (int z = 0; z < zoneFuelRange.length; z++) {
            l2TopZoneFuels[extendedSteps - 1][FirstZoneCol + z] = zoneFuelRange[z].max;
        }
        double fuelFactorAtMax = (l2TopZoneFuels[extendedSteps - 1][TotFuelCol] - l2TopZoneFuels[extendedSteps - 2][TotFuelCol]) /
                (l2TopZoneFuels[extendedSteps - 2][TotFuelCol] - l2TopZoneFuels[extendedSteps - 3][TotFuelCol]);
        l2TopZoneFuels[extendedSteps- 1][USpeedCol] =  l2TopZoneFuels[extendedSteps - 2][USpeedCol]   +
                fuelFactorAtMax * (l2TopZoneFuels[extendedSteps - 2][USpeedCol]- l2TopZoneFuels[extendedSteps - 3][USpeedCol]);
        l2TopZoneFuels[extendedSteps- 1][UOutputCol] =  l2TopZoneFuels[extendedSteps - 2][UOutputCol]   +
                fuelFactorAtMax * (l2TopZoneFuels[extendedSteps - 2][UOutputCol]- l2TopZoneFuels[extendedSteps - 3][UOutputCol]);

        // trim to max min
        Double[] oneRow;
        for (int r = 0; r < l2TopZoneFuels.length; r++) {
            oneRow = l2TopZoneFuels[r];
            for (int z = 0; z < zoneFuelRange.length; z++) {
                double nowVal = oneRow[FirstZoneCol + z];
                oneRow[FirstZoneCol + z] = zoneFuelRange[z].limitedValue(nowVal);
            }
        }
    }

    Double[][] getCalculatedData(boolean bBot, boolean fuelHeat) {
        Double[][] baseData;
        if (bBot) {
            if (fuelHeat)
                baseData = botZoneFuelHeat;
            else
                baseData = botZoneFuels;
        } else {   // top Zones
            if (fuelHeat)
                baseData = topZoneFuelHeat;
            else
                baseData = topZoneFuels;
        }
        int columns = baseData[0].length;
        Double[][] extendedData = new Double[extendedSteps][columns];
        for (int r = 1; r < extendedSteps - 1; r++) {
            for (int c = 0; c < columns; c++)
                extendedData[r][c] = new Double(baseData[r - 1][c]);
        }
        return extendedData;
    }

    public double[][] oneZoneFuelArray(int zNum, boolean bBot) {
        double[][] zoneFuel = new double[extendedSteps][(bBot) ? nBotZones : nTopZones];
        if (zNum >= 0 && zNum < ((bBot) ? nBotZones : nTopZones)) {
            Double[][] allZoneFuels = (bBot) ? l2BotZoneFuels : l2TopZoneFuels;
            for (int r = 0; r < extendedSteps; r++) {
                zoneFuel[r][0] = allZoneFuels[r][2];
                zoneFuel[r][1] = allZoneFuels[r][zNum + 3];
            }
        }
        return zoneFuel;
    }

    public double[][] oneZoneFuelHeatArray(int zNum, boolean bBot) {
        double[][] zoneFuelHeat = new double[extendedSteps][(bBot) ? nBotZones : nTopZones];
        if (zNum >= 0 && zNum < ((bBot) ? nBotZones : nTopZones)) {
            Double[][] allZoneFuelHeats = (bBot) ? l2BotZoneFuelHeat : l2TopZoneFuelHeat;
            for (int r = 0; r < extendedSteps; r++) {
                zoneFuelHeat[r][0] = allZoneFuelHeats[r][2];
                zoneFuelHeat[r][1] = allZoneFuelHeats[r][zNum + 3];
            }
        }
        return zoneFuelHeat;
    }


    protected XYArray l2SpeedFuelArray(boolean bBot) {
        XYArray arr;
        Double[][] table;
        if (bBot) {
            arr = l2SpeedTotalFuelBot;    // TODO speedTotalFuelBot is never initiated for reuse
            table = l2BotZoneFuels;
        } else {
            arr = l2SpeedTotalFuelTop;     // TODO speedTotalFuelTop is never initiated for reuse
            table = l2TopZoneFuels;
        }
        if (arr == null)
            arr = new XYArray(table, USpeedCol, TotFuelCol);
        else
            arr.setValues(table, USpeedCol, TotFuelCol);
        return arr;
    }

    /*
     based on total FuelHeat
    */
    XYArray l2SpeedFHArray(boolean bBot) {  // TODO to be done

        return null;
    }

    public double recommendedSpeed(double totFuel, boolean bBot) {
        if (bBot)
            return l2SpeedTotalFuelBot.getXat(totFuel);
        else
            return l2SpeedTotalFuelTop.getXat(totFuel);
    }

    public double recommendedSpeedOnFuelHeat(double totalFH, boolean bBot) {
        if (bBot)
            return l2SpeedTotalFHBot.getXat(totalFH);
        else
            return l2SpeedTotalFHTop.getXat(totalFH);
    }

}
