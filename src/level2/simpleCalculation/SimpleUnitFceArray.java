package level2.simpleCalculation;

import directFiredHeating.DFHTuningParams;
import directFiredHeating.UnitFceArray;
import directFiredHeating.UnitFurnace;
import mvUtils.math.MultiColData;

import java.awt.*;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 02-Nov-15
 * Time: 2:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleUnitFceArray extends UnitFceArray {
    ColNumAndData fieldFceT;
    Vector<SimpleUnitFurnace> svUfs;

    public SimpleUnitFceArray(boolean bBot, Vector<SimpleUnitFurnace> svUfs, DFHTuningParams.ForProcess forProcess){
        super(bBot, forProcess);
        this.svUfs = svUfs;
        this.vUfs = new Vector<UnitFurnace>();

    }

    public void setColData() {
        super.setColData();
        int len = vUfs.size() - 1;
        boolean onTest = vUfs.get(0).tuning.bOnTest;
        if (len > 0) {
            String suffix = (bBot)? "B" :"T";
            if (onTest) {
                fieldFceT = new ColNumAndData(suffix + "FieldFceTemp", "#,###", Color.yellow);
                SimpleUnitFurnace uf;
                for (int u = 0; u < len; u++) {
                    uf = svUfs.get(u);
                    fieldFceT.addData(u, uf.dpFieldTempO);
                }

            }
        }
    }
}
