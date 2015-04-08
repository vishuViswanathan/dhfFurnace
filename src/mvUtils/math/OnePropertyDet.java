package mvUtils.math;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 3/18/12
 * Time: 1:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class OnePropertyDet {
    public String name ;
    public String units ;

    public OnePropertyDet( String name, String units) {
        this.name = name;
        this.units = units;
    }

    public String toString() {
        return name + " (" + units + ")";
    }
}
