package jsp;

import basic.Fuel;
import directFiredHeating.DFHeating;
import mvUtils.jsp.*;
import mvUtils.display.ErrorStatAndMsg;
import mvUtils.jsp.JSPConnection;
import mvUtils.jsp.JSPObject;
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
//        ErrorStatAndMsg jspResponse = jspConnection.getData("../jsp/fuelList.jsp");
        ErrorStatAndMsg jspResponse = jspConnection.getData("fuelList.jsp");
        if (!jspResponse.inError) {
            String xmlStr = jspResponse.msg;
            ValAndPos vp;
            String xmlOneFuel;
            vp = XMLmv.getTag(xmlStr, "nFuels");
            if (vp.val.length() > 0) {
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
        }
        else
            System.out.println("ERROR:" + jspResponse.msg);
        debug("nFuels " + fuelList.size());
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

    @Override
    public void unCollectData() {
        dataCollected = false;
    }

    public boolean isDataCollected() {
        return dataCollected;
    }

    public boolean collectData(mvUtils.jsp.JSPConnection jspConnection) {
        if (!dataCollected) {
//            ErrorStatAndMsg jspResponse = jspConnection.getData("../jsp/selectedFuel.jsp", "fuelID", ("" + getId()).trim());
            ErrorStatAndMsg jspResponse = jspConnection.getData("selectedFuel.jsp", "fuelID", ("" + getId()).trim());
            if (!jspResponse.inError) {
                String xmlStr = jspResponse.msg;
                dataCollected = takeDataFromXML(xmlStr);
            }
        }
        return dataCollected;
    }
    static void debug(String msg) {
        DFHeating.debugLocal(msg);
    }
}
