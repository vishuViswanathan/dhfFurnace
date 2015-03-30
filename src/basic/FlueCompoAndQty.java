package basic;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 12/28/12
 * Time: 11:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class FlueCompoAndQty {
    public FlueComposition flueCompo;
    public double flow;
    public double flueTemp;
    public double flueHeat = 0;

    public FlueCompoAndQty(FlueComposition flueCompo, double flow, double flueTemp) {
        this.flueCompo = flueCompo;
        this.flow = flow;
        this.flueTemp = flueTemp;
        if (flueCompo != null)
            flueHeat = flueCompo.sensHeatFromTemp(flueTemp) * flow;
    }

    public FlueCompoAndQty(String name, FlueCompoAndQty copyFrom) {
        flueCompo = new FlueComposition(name, copyFrom.flueCompo, 0);
        flow = copyFrom.flow;
        flueTemp = copyFrom.flueTemp;
        flueHeat = copyFrom.flueHeat;
    }

    public FlueCompoAndQty(String name, FlueCompoAndQty flue1, FlueCompoAndQty flue2) {
        flow = flue1.flow + flue2.flow;
        if (flue1.flow == 0)
            flueCompo = new FlueComposition(flue2.flueCompo);
        else {
            double flue2Fract = flue2.flow / flue1.flow;
            flueCompo = new FlueComposition(name, flue1.flueCompo, flue2.flueCompo, flue2Fract);
        }
        flueHeat = flue1.flueHeat + flue2.flueHeat;
        flueTemp = flueCompo.tempFromSensHeat(flueHeat / flow);
    }

    public double heatChangeForTemp(double toTemp) {
        double finalHeat = flow * flueCompo.sensHeatFromTemp(toTemp);
        return finalHeat - flueHeat;
    }

    public void coolIt(double deltaTemp) {
        flueTemp -= deltaTemp;
        flueHeat = flow * flueCompo.sensHeatFromTemp(flueTemp);
    }

    public double giveHeatToAir(double airQty, double airTempIn, double airTempOut) {
        double heatToAir = airQty * (flueCompo.airUnitHeat(airTempOut) - flueCompo.airUnitHeat(airTempIn));
        flueHeat -= heatToAir;
        flueTemp = flueCompo.tempFromSensHeat(flueHeat / flow);
        return flueTemp;
    }

    // exchanges heat with 'withFluid'- updates self and 'withFluid'
    public void exchangeHeat(FlueCompoAndQty withFluid, double toFluidTemp) {
        double heatExch = withFluid.heatChangeForTemp(toFluidTemp);
        flueHeat -= heatExch;
        flueTemp = flueCompo.tempFromSensHeat(flueHeat / flow);
        withFluid.setTemperature(toFluidTemp);
    }

    public void exchangeHeat(double heatTaken) {
        double heatExch = heatTaken;
        flueHeat -= heatExch;
        flueTemp = flueCompo.tempFromSensHeat(flueHeat / flow);
    }

    // returns air Qty if toTemp < flueTemp
    public double diluteWithAir(double toTemp, double airTemp) {
        double airReqd = 0;
        if (flueTemp > toTemp && toTemp > airTemp) {
            airReqd = flueCompo.getDilutionAir(flow, flueTemp, toTemp, airTemp);
            flueCompo = new FlueComposition("Diluted " + flueCompo.name, flueCompo, airReqd / flow);
            flow += airReqd;
            flueTemp = toTemp;
        }
        return airReqd;
    }

    public void setTemperature(double flueTemp) {
        this.flueTemp = flueTemp;
        flueHeat = flueCompo.sensHeatFromTemp(flueTemp) * flow;
    }

    public void noteValues(FlueCompoAndQty src) {
        if (src != null) {
            flueCompo = new FlueComposition(src.flueCompo);
            flow = src.flow;
            flueTemp = src.flueTemp;
            flueHeat = src.flueHeat;
        }
    }

    public void noteValues(FlueComposition flueCompo, double flow, double flueTemp, double flueHeat) {
        this.flueCompo = new FlueComposition(flueCompo);
        this.flow = flow;
        this.flueTemp = flueTemp;
        this.flueHeat = flueHeat;
    }
}
