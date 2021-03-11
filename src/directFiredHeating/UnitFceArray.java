package directFiredHeating;

import mvUtils.display.DataWithStatus;
import mvUtils.math.DoubleRange;
import mvUtils.math.MultiColData;
import mvUtils.math.MultiColDataPoint;
import mvUtils.math.SPECIAL;

import java.awt.*;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 8/1/12
 * Time: 11:06 AM
 * To change this template use File | Settings | File Templates.
 */
public class UnitFceArray {
    protected boolean bBot;
    protected Vector<UnitFurnace> vUfs;
    protected MultiColData multiColData;
    public enum ProfileBasis {
        FCETEMP("Furnace Temperature"),
        GASTEMP("Gas Temperature"),
        AVGTEMP("Average Temperature");

        private final String basisName;

        ProfileBasis(String basisName) {
            this.basisName = basisName;
        }

        public String basisName() {
            return basisName;
        }

        @Override
        public String toString() {
            return basisName;
        }

        public ProfileBasis getEnum(String text) {
            if (text != null) {
                for (ProfileBasis b : ProfileBasis.values()) {
                    if (text.equalsIgnoreCase(b.basisName)) {
                        return b;
                    }
                }
            }
            return null;
        }
    }

    public int colFceTemp = 0, colGasTemp = 0, colChTempMean = 0;
    protected DFHTuningParams.FurnaceFor furnaceFor;

    public UnitFceArray(boolean bBot, DFHTuningParams.FurnaceFor furnaceFor) {
        this.bBot = bBot;
        this.furnaceFor = furnaceFor;
    }

    public UnitFceArray(boolean bBot, Vector<UnitFurnace> vUfs, DFHTuningParams.FurnaceFor furnaceFor){
        this(bBot, furnaceFor);
        this.vUfs = vUfs;
//        this.bBot = bBot;
//        this.forProcess = forProcess;
    }

    public boolean setColData() {
        boolean retVal = false;
        int len = vUfs.size() - 1;
        boolean onTest = vUfs.get(0).tuning.bOnTest;
        if (len > 0) {
            try {
                String suffix = (bBot) ? "B" : "T";
                double[] xVal = new double[len];
                for (int u = 0; u < len; u++)
                    xVal[u] = vUfs.get(u).endPos;
                multiColData = new MultiColData("From Ch End", xVal, "###.00", 60);
                ColNumAndData gasT = new ColNumAndData(suffix + "GasTemp", "#,###", Color.orange);
                colGasTemp = gasT.colNum;
                ColNumAndData fceT = new ColNumAndData(suffix + "FceTemp", "#,###", Color.red);
                colFceTemp = fceT.colNum;
                ColNumAndData chSurfT = null;
                ColNumAndData chCoreT = null;
                if (furnaceFor != DFHTuningParams.FurnaceFor.STRIP) {
                    chSurfT = new ColNumAndData(suffix + "ChSurfTemp", "#,###", Color.yellow);
                    chCoreT = new ColNumAndData(suffix + "ChCoreTemp", "#,###", Color.cyan);
                }
                ColNumAndData chMeanT = new ColNumAndData(suffix + "ChMeanTemp", "#,###", Color.blue);
                colChTempMean = chMeanT.colNum;
                ColNumAndData posTime = new ColNumAndData(suffix + "PosTime", "#0.000");
                ColNumAndData totAlpha = null;
                ColNumAndData alphaWall = null;
                ColNumAndData alphaGas = null;
                ColNumAndData alphaAbsorb = null;
                ColNumAndData effGasEmm = null;
                ColNumAndData cndEw = null;
                ColNumAndData cndChTk = null;
                ColNumAndData cndChSpHt = null;
                ColNumAndData cndPsi = null;
                ColNumAndData cndHeight = null;
                ColNumAndData cndGratio = null;
                ColNumAndData cndGthick = null;
                ColNumAndData cndChTau = null;
                ColNumAndData totAlphaTOW = null;
                ColNumAndData wallOnlyFactor = null;
//            ColNumAndData chargeHeat = null;
//            ColNumAndData heatToCharge = null;
//            ColNumAndData heatFromWall = null;
//            ColNumAndData heatFromGas = null;
//            ColNumAndData heatAbsorbed = null;

                if (onTest) {
                    totAlpha = new ColNumAndData(suffix + "TotAlpha", "#,###.00");
                    alphaWall = new ColNumAndData(suffix + "AlphaWall", "#,###.00");
                    alphaGas = new ColNumAndData(suffix + "AlphaGas", "#,###.00");
                    alphaAbsorb = new ColNumAndData(suffix + "AlphaAbsorb", "#,###.00");
                    effGasEmm = new ColNumAndData(suffix + "effGasEmm", "#,##0.00");
                    cndChSpHt = new ColNumAndData(suffix + "chSpHt", "#,##0.00");
                    cndPsi = new ColNumAndData(suffix + "psi", "#,##0.00");
                    cndHeight = new ColNumAndData(suffix + "height", "#,###.00");
                    cndGratio = new ColNumAndData(suffix + "gRatio", "#,###.00");
                    cndGthick = new ColNumAndData(suffix + "gThick", "#,##0.00");
                    cndChTau = new ColNumAndData(suffix + "tau", "#,##0.00");
                    cndEw = new ColNumAndData(suffix + "chEmiss", "#,##0.00");
                    cndChTk = new ColNumAndData(suffix + "chTk", "#,##0.00");
                    totAlphaTOW = new ColNumAndData(suffix + "TotAlphaTOW", " #,###.00");
                    wallOnlyFactor = new ColNumAndData(suffix + "WOnlyFact", "#0.000");
//                chargeHeat = new ColNumAndData(suffix + "chargeHeat", "#,###");
//                heatToCharge = new ColNumAndData(suffix + "heatToCharge", "#,###");
//                heatFromWall = new ColNumAndData(suffix + "heatFromWall", "#,###");
//                heatFromGas = new ColNumAndData(suffix + "heatFromGas", "#,###");
//                heatAbsorbed = new ColNumAndData(suffix + "AlphaAbsorb", "#,###");
                }
                UnitFurnace uf;
                for (int u = 0; u < len; u++) {
                    uf = vUfs.get(u);
                    gasT.addData(u, uf.dpTempG);
                    fceT.addData(u, uf.dpTempO);
                    if (furnaceFor != DFHTuningParams.FurnaceFor.STRIP) {
                        chSurfT.addData(u, uf.dpTempWO);
                        chCoreT.addData(u, uf.dpTempWcore);
                    }
                    chMeanT.addData(u, uf.dpTempWmean);
                    posTime.addData(u, uf.dpEndTime);
                    if (onTest) {
                        totAlpha.addData(u, uf.dpTotAlpha);
                        alphaWall.addData(u, uf.dpAlphaWall);
                        alphaGas.addData(u, uf.dpAlphaGas);
                        totAlphaTOW.addData(u, uf.dpAlphaTOW);
                        alphaAbsorb.addData(u, uf.dpAlphaAbsorb);
                        effGasEmm.addData(u, uf.dpEffGasEmiss);
                        cndChSpHt.addData(u, uf.dpChSpHt);
                        cndPsi.addData(u, uf.dpPsi);
                        cndHeight.addData(u, uf.dpHeight);
                        cndGratio.addData(u, uf.dpGratio);
                        cndGthick.addData(u, uf.dpGthick);
                        cndChTau.addData(u, uf.dpChTau);
                        cndChTk.addData(u, uf.dpChTk);
                        cndEw.addData(u, uf.dpEw);

//                     wallOnlyFactor.addData(u, uf.dpWallOnlyFactor);
//                    chargeHeat.addData(u, uf.dpChargeHeat);
//                    heatToCharge.addData(u, uf.dpHeatToCharge);
//                    heatFromWall.addData(u, uf.dpHeatFromWall);
//                    heatFromGas.addData(u, uf.dpHeatFromGas);
//                    heatAbsorbed.addData(u, uf.dpHeatAbsorbed);
                    }
                }
                gasT.fillMultiColData();
                fceT.fillMultiColData();
                if (furnaceFor != DFHTuningParams.FurnaceFor.STRIP) {
                    chSurfT.fillMultiColData();
                    chCoreT.fillMultiColData();
                }
                chMeanT.fillMultiColData();
                posTime.fillMultiColData();
                if (onTest) {
                    totAlpha.fillMultiColData();
                    alphaWall.fillMultiColData();
                    alphaGas.fillMultiColData();
                    alphaAbsorb.fillMultiColData();
                    effGasEmm.fillMultiColData();
                    cndChSpHt.fillMultiColData();
                    cndPsi.fillMultiColData();
                    cndHeight.fillMultiColData();
                    cndGratio.fillMultiColData();
                    cndGthick.fillMultiColData();
                    cndChTau.fillMultiColData();
                    cndChTk.fillMultiColData();
                    cndEw.fillMultiColData();
                    totAlphaTOW.fillMultiColData();
//                wallOnlyFactor.fillMultiColData();
//                chargeHeat.fillMultiColData();
//                heatToCharge.fillMultiColData();
//                heatFromWall.fillMultiColData();
//                heatFromGas.fillMultiColData();
//                heatAbsorbed.fillMultiColData();
                }
                retVal = true;
            }
            catch(Exception e) {
                ;
            }
        }
        return retVal;
    }

    public MultiColData getMultiColdata() {
        return multiColData;
    }

    protected class ColNumAndData {
        String name;
        int colNum;
        String format = "";
        Vector <MultiColDataPoint> dataPt;
        public ColNumAndData(String name, String format, Color color) throws Exception {
            this.name = name;
            this.format = format;
            this.colNum = multiColData.addColumn(name, format, color) -1;
            if (this.colNum < 0)
                throw new Exception("Too many columns in Data collection!");
            dataPt = new Vector<MultiColDataPoint>();
        }

        protected ColNumAndData(String name, String format) throws Exception{
            this.name = name;
            this.format = format;
            this.colNum = multiColData.addColumn(name, format) -1;
            if (this.colNum < 0)
                throw new Exception("Too many columns in Data collection!");
            dataPt = new Vector<MultiColDataPoint>();
        }

        public void addData(int x, MultiColDataPoint mcdp) {
            dataPt.add(x, mcdp);
        }

        boolean fillMultiColData() {
            boolean bRetVal = true;
            MultiColDataPoint mcdp;
            for (int row = 0; row < dataPt.size(); row++) {
                mcdp = dataPt.get(row);
                bRetVal = (multiColData.setData(colNum, row, mcdp.val) >= 0);
                if (!bRetVal)
                    break;
                mcdp.setMultiColRef(multiColData, colNum, row);
            }
            return bRetVal;
        }
    }

    public double[] tProfileForTFM(ProfileBasis basis, int divisions) {
        DoubleRange xRange = multiColData.getCommonXrange();
        double step = (xRange.max - xRange.min) / divisions;
        double pos = 0;
        double[] data = new double[divisions + 1];
        double fceT, gasT, forData;
        if (step >= 0) {
            for (int n = 0; n < (divisions + 1); n++) {
                if (n == (divisions + 1))
                    pos = xRange.max;
                switch (basis) {
                    case GASTEMP:
                        forData = multiColData.getYat(colGasTemp, pos);
                        break;
                    case FCETEMP:
                        forData = multiColData.getYat(colFceTemp, pos);
                        break;
                    default:
                        gasT = multiColData.getYat(colGasTemp, pos);
                        fceT = multiColData.getYat(colFceTemp, pos);
                        forData = (gasT + fceT) / 2;
                        break;
                }
                data[n] = SPECIAL.roundToNDecimals(forData, 0);
                pos += step;
            }
        }
        else
            data[0] = Double.NaN;
        return data;
    }
}
