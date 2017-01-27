package jsp;

import basic.ChMaterial;
import mvUtils.display.ErrorStatAndMsg;
import mvUtils.jsp.JSPConnection;
import mvUtils.jsp.JSPObject;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 24-Jul-15
 * Time: 5:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSPchMaterial extends ChMaterial implements JSPObject {
    boolean dataCollected = false;

    public JSPchMaterial(String xmlStr, boolean justName) {
        super("matX", "IDX");
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "name");
        this.name = vp.val;
        vp = XMLmv.getTag(xmlStr, "ID");
        this.matID = vp.val;
    }

    public static Vector<JSPchMaterial> getMetalList(mvUtils.jsp.JSPConnection jspConnection) {
         Vector<JSPchMaterial> metalList = new Vector<JSPchMaterial>();
         ErrorStatAndMsg jspResponse = jspConnection.getData("../jsp/metalList.jsp");
         if (!jspResponse.inError) {
             String xmlStr = jspResponse.msg;
             ValAndPos vp;
             String xmlOneFuel;
             vp = XMLmv.getTag(xmlStr, "nMetals");
             int nMetals = Integer.valueOf(vp.val);
             for (int n = 0; n < nMetals; n++) {
                 vp = XMLmv.getTag(xmlStr, "M" + ("" + n).trim(), vp.endPos);
                 try {
                     metalList.add(new JSPchMaterial(vp.val, true));
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             }
         }
         else
             System.out.println("ERROR:" + jspResponse.msg);
         System.out.println("nMetals " + metalList.size());
         return metalList;
     }

    public boolean isDataCollected() {
        return dataCollected;
    }

    @Override
    public void unCollectData() {
        dataCollected = false;
    }

    public boolean collectData(JSPConnection jspConnection) {
        if (!dataCollected) {
//            debug("in collectData()");
            Hashtable<String,String> query = new Hashtable<String, String>(){
                {put("matID", matID.trim()); put("matName", name);}
            };
            ErrorStatAndMsg jspResponse = jspConnection.getData("../jsp/selectedMetal.jsp", query);
            if (!jspResponse.inError) {
                String xmlStr = jspResponse.msg;
//                debug("xmlStr = " + xmlStr);
                dataCollected = takeDataFromXML(xmlStr);
                if (!dataCollected)
                    debug("ERROR: " + xmlStr);
            }
            else
                debug("jspResponse:" + jspResponse.msg);
        }
        return dataCollected;
    }

    void debug(String msg) {
        System.out.println("JSPchMaterial: " + msg);
    }
}
