package level2.stripDFH;

import directFiredHeating.process.OneStripDFHProcess;
import level2.common.L2ParamGroup;
import level2.common.Tag;
import level2.common.TagCreationException;

/**
 * User: M Viswanathan
 * Date: 26-Sep-16
 * Time: 11:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class L2DFHProcessZone extends L2ParamGroup {
    OneStripDFHProcess theProcess;

    public L2DFHProcessZone(L2DFHFurnace l2Furnace, String zoneName, String descriptiveName)
            throws TagCreationException{
        super(l2Furnace, zoneName, descriptiveName);
        connectToLevel1();
     }

    public boolean connectToLevel1()
            throws TagCreationException {
        String temperatureFmt = "#,##0";
        String widthFmt = "#,##0";
        String thicknessFmt = "#0.000";
        Tag[] detailsTags = {
                new Tag(L2ParamGroup.Parameter.Details, Tag.TagName.Wmin, true, false, widthFmt),
                new Tag(L2ParamGroup.Parameter.Details, Tag.TagName.Wmax, true, false, widthFmt),
                new Tag(L2ParamGroup.Parameter.Details, Tag.TagName.THmin, true, false, thicknessFmt),
                new Tag(L2ParamGroup.Parameter.Details, Tag.TagName.THmax, true, false, thicknessFmt),
                new Tag(L2ParamGroup.Parameter.Details, Tag.TagName.ExitTemp, true, false, temperatureFmt),
                new Tag(L2ParamGroup.Parameter.Details, Tag.TagName.BaseProcess, true, false)};
        addOneParameter(L2ParamGroup.Parameter.Details, detailsTags);
        return true;
    }

    public boolean sendToLevel1(OneStripDFHProcess theProcess) {
        boolean retVal = false;
        this.theProcess = theProcess;
        setValue(L2ParamGroup.Parameter.Details, Tag.TagName.Wmin, (float)(theProcess.minWidth * 1000));
        setValue(L2ParamGroup.Parameter.Details, Tag.TagName.Wmax, (float)(theProcess.maxWidth * 1000));
        setValue(L2ParamGroup.Parameter.Details, Tag.TagName.THmin, (float)(theProcess.minThickness * 1000));
        setValue(L2ParamGroup.Parameter.Details, Tag.TagName.THmax, (float)(theProcess.maxThickness * 1000));
        setValue(L2ParamGroup.Parameter.Details, Tag.TagName.ExitTemp, (float)theProcess.tempDFHExit);
        setValue(L2ParamGroup.Parameter.Details, Tag.TagName.BaseProcess, theProcess.baseProcessName);
        retVal = true;
        return retVal;
    }

    public void clearInLevel1() {
        setValue(L2ParamGroup.Parameter.Details, Tag.TagName.BaseProcess, "");
    }


}
