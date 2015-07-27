package jsp;

import basic.Fuel;
import mvUtils.display.ErrorStatAndMsg;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 10-Jul-15
 * Time: 1:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSPFuel extends Fuel implements JSPObject {
    boolean dataCollected = false;
    public static Vector<JSPFuel> getFuelList(JSPConnection jspConnection) {
        Vector<JSPFuel> fuelList = new Vector<JSPFuel>();
        ErrorStatAndMsg jspResponse = jspConnection.getData("../jsp/fuelList.jsp");
        if (!jspResponse.inError) {
            String xmlStr = jspResponse.msg;
            ValAndPos vp;
            String xmlOneFuel;
            vp = XMLmv.getTag(xmlStr, "nFuels");
            int nFuels = Integer.valueOf(vp.val);
            for (int n = 0; n < nFuels; n++) {
                vp = XMLmv.getTag(xmlStr, "F" + ("" + n).trim(), vp.endPos);
                try {
                    fuelList.add(new JSPFuel(vp.val));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        else
            System.out.println("ERROR:" + jspResponse.msg);
        System.out.println("nFuels " + fuelList.size());
        return fuelList;
    }

    public JSPFuel(String xmlStr) throws  Exception {
        super("");
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "name");
        this.name = vp.val;
        vp = XMLmv.getTag(xmlStr, "ID");
        setID(Integer.valueOf(vp.val));
    }

    public JSPFuel(String name, int id)  throws Exception {
        super(name);
        setID(id);
    }

    public boolean isDataCollected() {
        return dataCollected;
    }

    public boolean collectData(JSPConnection jspConnection) {
        if (!dataCollected) {
            ErrorStatAndMsg jspResponse = jspConnection.getData("../jsp/selectedFuel.jsp", "fuelID", ("" + getId()).trim());
            if (!jspResponse.inError) {
                String xmlStr = jspResponse.msg;
                dataCollected = takeDataFromXML(xmlStr);
            }
        }
        return dataCollected;
    }
}
