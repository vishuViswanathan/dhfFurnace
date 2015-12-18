package level2.common;

import TMopcUa.*;
import com.prosysopc.ua.client.MonitoredDataItem;
import level2.common.L2ParamGroup;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 07-Jan-15
 * Time: 5:03 PM
 * To change this template use File | Settings | File Templates.
 * changed to test git
 */
public class Tag {
    static public enum TagName {
        // booleans
        Auto("auto"),
        Remote("remote"),
        Enabled("enabled"),
        Noted("noted"),
        Ready("ready"),
        Mode("Mode"),   // eg Strip mode for DFH control
//        StripMode("StripMode"),
        Response("Response"),
        //floats
        PV("PV"),
        SP("SP"),
        CV("CV"),
        Thick("thick"),
        Width("width"),
        Length("balanceLength"),
        Temperature("Temperature"),    // for Air and fuel and strip (from Level2)
        SpeedNow("SpeedNow"),
        SpeedMax("SpeedMax"),
        Span("span"),
        Data("Data"),
        X1("x1"),
        X2("x2"),
        X3("x3"),
        X4("x4"),
        X5("x5"),
        X6("x6"),
        X7("x7"),
        X8("x8"),
        X9("x9"),
        X10("x10"),

        Y1("y1"),
        Y2("y2"),
        Y3("y3"),
        Y4("y4"),
        Y5("y5"),
        Y6("y6"),
        Y7("y7"),
        Y8("y8"),
        Y9("y9"),
        Y10("y10"),
        // Strings
        Process("process"),
        Msg("msg");


        private final String name;

        TagName(String name) {
            this.name = name;
        }

        public String getValue() {
            return name();
        }

        @Override
        public String toString() {
            return name;
        }

        public static TagName getEnum(String text) {
            TagName retVal = null;
            if (text != null) {
                for (TagName b : TagName.values()) {
                    if (text.equalsIgnoreCase(b.name)) {
                        retVal = b;
                        break;
                    }
                }
            }
            return retVal;
        }
    }

    public TagName tagName;
    public boolean bSubscribe = false;
    public L2ParamGroup.Parameter element;
    public ProcessData.DataType dataType;
    public ProcessData.Source dataSource;  // Level2 or Process
    public ProcessData.Access access;
    protected ProcessData processData;
    ProcessValue value = new ProcessValue();

    public Tag(L2ParamGroup.Parameter element, TagName tagName, boolean rw, boolean bSubscribe) {
        this.element = element;
        this.tagName = tagName;
        this.dataType = getDataType(tagName);
        access = (rw) ? ProcessData.Access.RW : ProcessData.Access.READONLY;
        dataSource = (rw) ? ProcessData.Source.LEVEL2 : ProcessData.Source.PROCESS;
        this.bSubscribe = bSubscribe;
    }

    public void setProcessData(ProcessData processData) {
        this.processData = processData;
    }

    public boolean isMonitored() {
        return processData.isMonitored();
    }

    public MonitoredDataItem getMonitoredDataItem() {
        return processData.getMonitoredDataItem();
    }

    public ProcessValue getValue() {
        processData.getValue(value);
        return value;
    }

    public ProcessValue processValue(){
        return value;
    }

    public ProcessValue setValue(float newValue) {
        value.floatValue = newValue;
        processData.setValue(value);
        return value;
    }

    public ProcessValue setValue(boolean newValue) {
        value.booleanValue = newValue;
        processData.setValue(value);
        return value;
    }

    public ProcessValue setValue(double newValue) {
         value.doubleValue = newValue;
         processData.setValue(value);
         return value;
     }

    public ProcessValue setValue(String newValue) {
         value.stringValue = newValue;
         processData.setValue(value);
         return value;
     }

    public String toString() {
        return tagName.toString();
    }

    ProcessData.DataType getDataType(TagName forTagName) {
        ProcessData.DataType type = ProcessData.DataType.FLOAT;
        switch(forTagName) {
            case Auto:
            case Remote:
            case Enabled:
            case Noted:
            case Ready:
            case Mode:
            case Response:
                type = ProcessData.DataType.BOOLEAN;
                break;
            case Process:
            case Msg:
                type = ProcessData.DataType.STRING;
                break;
        }
        return type;
    }

    ProcessData.DataType getDataType() {
        return dataType;
    }

    public boolean checkConnection() {
        boolean retVal;
        TMopcUa.ProcessValue pValue = getValue();
        retVal = pValue.valid;
        if ((access == ProcessData.Access.RW) && retVal) {
            processData.setValue(pValue);
            retVal = pValue.valid;
        }
        return retVal;
    }
}
