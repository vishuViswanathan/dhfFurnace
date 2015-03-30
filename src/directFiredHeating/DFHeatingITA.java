package directFiredHeating;

import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 3/15/13
 * Time: 9:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class DFHeatingITA extends DFHeating {

    public DFHeatingITA() {
        debug("ITA release 10.01 20130315");
        locale = Locale.getDefault(); // creates Locale class object by getting the default locale.
        locale = Locale.ITALY;
        Locale.setDefault(locale);
        debug("Locale is " + locale);
    }
}

