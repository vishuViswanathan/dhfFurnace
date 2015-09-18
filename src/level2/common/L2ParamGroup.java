package level2.common;

import TMopcUa.ProcessValue;
import com.prosysopc.ua.client.*;
import level2.L2DFHFurnace;
import mvUtils.display.ErrorStatAndMsg;

import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 27-Jan-15
 * Time: 11:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class L2ParamGroup {
    static public enum Parameter {
        Temperature("Temperature"),
        FuelFlow("FuelFlow"),
        AirFlow("AirFlow"),
        AFRatio("AFRatio"),
        Pressure("Pressure"),
        Speed("Speed"),
        Flue("Flue"),
        FuelCharacteristic("FuelCharacteristic"),
        FieldData("FieldData"),  // from which level2 to work out performance data
        Now("Now"),
        Next("Next"),
        Status("Status"),
        ErrMsg("ErrorMsg"),
        InfoMsg("InfoMsg"),
        YesNoQuery("YesNoQuery"),
        DataQuery("dataQuery"),
        L2Stat("Level2Stat");
        private final String elementName;

        Parameter(String elementName) {
            this.elementName = elementName;
        }

        public String getValue() {
            return name();
        }

        @Override
        public String toString() {
            return elementName;
        }

        public static Parameter getEnum(String text) {
            Parameter retVal = null;
            if (text != null) {
                for (Parameter b : Parameter.values()) {
                    if (text.equalsIgnoreCase(b.elementName)) {
                        retVal = b;
                        break;
                    }
                }
            }
            return retVal;
        }
    }

    public String groupName;
    Hashtable<Parameter, L2ZoneParam> paramList;
    public L2Interface l2Interface;   // was public public L2DFHFurnace l2Furnace;
    Subscription subscription = null;

    /**
     * For common sections with external subscription
     *
     * @param l2Interface
     * @param groupName
     * @param subscription
     */
    public L2ParamGroup(L2Interface l2Interface, String groupName, Subscription subscription) {
        this.l2Interface = l2Interface;
        this.groupName = groupName;
        this.subscription = subscription;
        paramList = new Hashtable<Parameter, L2ZoneParam>();
    }

    public L2ParamGroup(L2Interface l2Furnace, String groupName) {
        this(l2Furnace, groupName, null);
    }

    public void setSubscription(Subscription sub) {
        this.subscription = sub;
    }

    public L2ZoneParam getL2Param(Parameter element) {
        return paramList.get(element);
    }

    public ProcessValue setValue(Parameter element, Tag.TagName tagName, float newValue) {
        return getL2Param(element).setValue(tagName, newValue);
    }

    public ProcessValue setValue(Parameter element, Tag.TagName tagName, String strValue) {
        return getL2Param(element).setValue(tagName, strValue);
    }

    public ProcessValue setValue(Parameter element, Tag.TagName tagName, boolean bValue) {
        return getL2Param(element).setValue(tagName, bValue);
    }

    public ProcessValue getValue(Parameter element, Tag.TagName tagName) {
        ProcessValue pV = getL2Param(element).getValue(tagName);
        if (!pV.valid)
            pV.errorMessage = groupName + "." + element + "." + tagName + ":" + pV.errorMessage;
        return pV;
    }

    public boolean addOneParameter(Parameter element, Tag[] tags) throws TagCreationException {
        boolean retVal = false;
        String basePath = "";
        try {
            L2ZoneParam param = new L2ZoneParam(l2Interface.source(), l2Interface.equipment(),
                    (basePath = groupName + "." + element),
                    tags, subscription);
            paramList.put(element, param);
            retVal = true;
        } catch (TagCreationException e) {
            e.setElement(basePath, "");
            throw (e);
        }
        return retVal;
    }

    public boolean addOneParameter(Parameter element, L2ZoneParam param) {
        paramList.put(element, param);
        return true;
    }

    public ErrorStatAndMsg checkConnections() {
        ErrorStatAndMsg retVal = new ErrorStatAndMsg(false, groupName + ".");
        ErrorStatAndMsg oneParamStat;
        boolean additional = false;
        for (L2ZoneParam p: paramList.values()) {
            oneParamStat = p.checkConnections();
            if (oneParamStat.inError) {
                retVal.inError = true;
                retVal.msg += ((additional) ? "\n" : "") + oneParamStat.msg;
                additional = true;
            }
        }
        return retVal;
    }

    void info(String msg) {
        l2Interface.info("L2DataGroup: " + msg);
    }

}
