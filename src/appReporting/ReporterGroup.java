package appReporting;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 12/18/13
 * Time: 3:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReporterGroup {
    Vector<Reporter> group;

    public ReporterGroup() {
        group = new Vector<Reporter>();
    }

    public void addReporter(Reporter report)  {
        group.add(report);
    }

}
