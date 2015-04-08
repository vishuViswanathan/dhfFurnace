package basic;

import mvUtils.mvXML.XMLmv;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 4/19/12
 * Time: 10:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class RadiantTube {
    public enum RTFunction {HEATING, COOLING}

    ;

    public enum RTSource {ELECTIRICAL, FUELFIRED}

    ;

    public enum RTType {SINGLEENDED, UTYPE, WTYPE}

    ;

    RTFunction function;
    RTSource heating;
    RTType tubeType;

    public double dia, activeLen;
    public double internHtCoeff, surfEmiss;
    public double rating;
    public double elementTemp;

    public RadiantTube(RTFunction function, RTSource heating, RTType tubeType, double od, double effLen, double emiss, double rating) {
        this.function = function;
        this.heating = heating;
        this.tubeType = tubeType;
        this.dia = od;
        this.activeLen = effLen;
        this.rating = rating;
        this.surfEmiss = emiss;
    }

    public RadiantTube(double od, double effLen, double rating) {
        this(RTFunction.HEATING, RTSource.ELECTIRICAL, RTType.SINGLEENDED, od, effLen, 0.85, rating);

    }

    public RadiantTube(double od, double effLen, double rating, double emiss) {
        this(RTFunction.HEATING, RTSource.ELECTIRICAL, RTType.SINGLEENDED, od, effLen, emiss, rating);

    }

    public double getTotHeatingSurface() {
        return Math.PI * dia * activeLen;
    }

    public String RTDataToSave() {
        String retVal;
        retVal = "<rt>" + XMLmv.putTag("dia", "" + dia * 1000);
        retVal += XMLmv.putTag("activeLen", "" + activeLen * 1000);
        retVal += XMLmv.putTag("rating", "" + rating);
        retVal += XMLmv.putTag("surfEmiss", "" + surfEmiss);
        retVal += "</rt>" + "\n";
        return retVal;

    }
}
