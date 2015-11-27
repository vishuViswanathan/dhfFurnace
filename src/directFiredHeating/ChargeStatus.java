package directFiredHeating;

import basic.Charge;
import basic.ProductionData;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 10-Nov-15
 * Time: 3:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChargeStatus {
    Charge charge;
    double production;
    double tempWO, tempWM, tempWCore;

    public ChargeStatus(Charge charge, double production, double tempWO, double tempWM, double tempWCore) {
        this.charge = new Charge(charge);
        setStatus(production, tempWO, tempWM, tempWCore);
    }

    public ChargeStatus(Charge charge, double production, double temp) {
        this(charge, production, temp, temp, temp);
    }

    public void setStatus(double production, double tempWO, double tempWM, double tempWCore) {
        this.production = production;
        setStatus (tempWO, tempWM, tempWCore);
    }

    public void setStatus(double production, double temp) {
        setStatus(production, temp, temp, temp);
    }

    public void setStatus (double tempWO, double tempWM, double tempWCore) {
        this.tempWO = tempWO;
        this.tempWM = tempWM;
        this.tempWCore = tempWCore;
    }

    public void setStatus(double temp)  {
        setStatus(temp, temp, temp);
    }
}
