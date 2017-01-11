package level2.process;

import directFiredHeating.process.OneStripDFHProcess;
import directFiredHeating.process.StripDFHProcessList;
import level2.applications.L2DFHeating;
import level2.common.L2ParamGroup;
import level2.common.TagCreationException;
import level2.stripDFH.L2DFHFurnace;
import level2.stripDFH.L2DFHProcessZone;
import mvUtils.display.ErrorStatAndMsg;

import java.util.Vector;

/**
 * User: M Viswanathan
 * Date: 26-Sep-16
 * Time: 5:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2ProcessList extends StripDFHProcessList{
    Vector<L2DFHProcessZone> processZones;
    L2DFHFurnace l2Furnace;
    public L2ProcessList(L2DFHeating l2Heating) {
        super(l2Heating);
        this.l2Furnace = l2Heating.l2Furnace;
        processZones = new Vector<>();
    }

    public boolean connectToLevel1() {
        boolean retVal = false;
        try {
            for (int p = 0; p < maxListLenFP; p++) {
                System.out.println(String.format("Process%02d", (p + 1)));
                processZones.add(new L2DFHProcessZone(l2Furnace, String.format("Process%02d", (p + 1)),
                        String.format("DFH Process %02d", (p + 1))));
            }
            retVal = true;
        } catch (TagCreationException e) {
            e.printStackTrace();
        }
        return retVal;
    }

    public boolean noteConnectionsCheckStat(ErrorStatAndMsg stat) {
        ErrorStatAndMsg oneSecStat;
        for (L2ParamGroup oneZ:processZones) {
            oneSecStat = oneZ.checkConnections();
            if (oneSecStat.inError) {
                stat.inError = true;
                stat.msg += "\n" + oneSecStat.msg;
            }
        }
        return stat.inError;
    }

    public void clearLevel1ProcessList() {
        for (L2DFHProcessZone z: processZones)
            z.clearInLevel1();
    }

    public int sendListToLevel1() {
        int p = 0;
        for (OneStripDFHProcess oneP: list) {
            if (processZones.get(p).sendToLevel1(oneP))
                p++;
            else {
                p = 0;
                break;
            }
        }
        return p;
    }
}
