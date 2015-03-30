package basic;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 11/27/12
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class FuelNameAndFlow {
    public double fuelFlow = 0;
    public String units;
    public double temperature;
    Fuel fuel;
    public String name;
    public double fuelSensibleHeat = 0;
    public double combustHeat = 0;
    double airFlow = 0;
    boolean bWithRegen = false;
    double airSensibleHeat = 0;
    double effExcessAir = 0; // effective Excess Air Fraction

    public FuelNameAndFlow(Fuel fuel, double temperature, double flow) {
        this(fuel);
        this.temperature = temperature;
        this.fuelFlow = flow;
        fuelSensibleHeat = fuel.sensHeatFromTemp(temperature) * flow;
        combustHeat = fuel.calVal * flow;
    }

    public FuelNameAndFlow(Fuel fuel, double temperature) {
        this(fuel, temperature, 0);
    }

    public FuelNameAndFlow(Fuel fuel) {
        this.fuel = fuel;
        name = fuel.name;
        units = fuel.units;
    }

    public FuelNameAndFlow(Fuel fuel, double fuelFlow, double airFlow, double airSensible, double fuelSensible) {
        this(fuel);
        addValues(fuelFlow, airFlow, airSensible, fuelSensible);
    }

    public void addValues(double fuelFlow, double airFlow, double airSensible, double fuelSensible) {
        this.fuelFlow += fuelFlow;
        this.airFlow += airFlow;
        this.airSensibleHeat += airSensible;
        this.fuelSensibleHeat += fuelSensible;
        combustHeat = fuel.calVal * this.fuelFlow;
        effExcessAir = this.airFlow / (this.fuelFlow * fuel.airFuelRatio) - 1;
    }
}
