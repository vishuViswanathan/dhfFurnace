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
    public double output;
    public double tempWO, tempWM, tempWCore;
    boolean bValid;

    public ChargeStatus(Charge charge, double output, double tempWO, double tempWM, double tempWCore) {
        this.charge = new Charge(charge);
        setStatus(output, tempWO, tempWM, tempWCore);
    }

    public ChargeStatus(Charge charge, double output, double temp) {
        this(charge, output, temp, temp, temp);
    }

    public void setStatus(double output, double tempWO, double tempWM, double tempWCore) {
        this.output = output;
        setStatus(tempWO, tempWM, tempWCore);
    }

    public void setStatus(double output, double temp) {
        setStatus(output, temp, temp, temp);
    }

    public void setStatus(boolean bValid) {
        this.bValid = bValid;
    }

    public void setStatus (double tempWO, double tempWM, double tempWCore) {
        this.tempWO = tempWO;
        this.tempWM = tempWM;
        this.tempWCore = tempWCore;
    }

    public void setStatus(double temp)  {
        setStatus(temp, temp, temp);
    }

    public ProductionData setProductionData(ProductionData pData) {
        pData.charge = charge;
        pData.production = output;
        return pData;
    }

    public boolean isValid() { return bValid; }
}
