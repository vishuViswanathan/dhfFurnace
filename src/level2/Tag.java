package level2;

import TMopcUa.*;
import com.prosysopc.ua.client.MonitoredDataItem;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 07-Jan-15
 * Time: 5:03 PM
 * To change this template use File | Settings | File Templates.
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
        //floats
        PV("PV"),
        SP("SP"),
        CV("CV"),
        Thick("thick"),
        Width("width"),
        Length("balanceLength"),
        Temperature("Temperature"),    // for Air and fuel
        Span("span"),
        X1("x1"),
        X2("x2"),
        X3("x3"),
        X4("x4"),
        X5("x5"),
        Y1("y1"),
        Y2("y2"),
        Y3("y3"),
        Y4("y4"),
        Y5("y5"),
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

    TagName tagName;
    boolean subscribe = false;
    L2ParamGroup.Parameter element;
    ProcessData.DataType dataType;
    ProcessData.Source dataSource;  // Level2 or Process
    ProcessData.Access access;
    ProcessData processData;
    ProcessValue value = new ProcessValue();

    public Tag(L2ParamGroup.Parameter element, TagName tagName, boolean rw, boolean subscribe) {
        this.element = element;
        this.tagName = tagName;
        this.dataType = getDataType(tagName);
        access = (rw) ? ProcessData.Access.RW : ProcessData.Access.READONLY;
        dataSource = (rw) ? ProcessData.Source.LEVEL2 : ProcessData.Source.PROCESS;
        this.subscribe = subscribe;
    }

    public void setProcessData(ProcessData processData) {
        this.processData = processData;
    }

    boolean isMonitored() {
        return processData.isMonitored();
    }

    MonitoredDataItem getMonitoredDataItem() {
        return processData.getMonitoredDataItem();
    }

    ProcessValue getValue() {
        processData.getValue(value);
        return value;
    }

    public ProcessValue processValue(){
        return value;
    }

    ProcessValue setValue(float newValue) {
        value.floatValue = newValue;
        processData.setValue(value);
        return value;
    }

    ProcessValue setValue(boolean newValue) {
        value.booleanValue = newValue;
        processData.setValue(value);
        return value;
    }

    ProcessValue setValue(double newValue) {
         value.doubleValue = newValue;
         processData.setValue(value);
         return value;
     }

    ProcessValue setValue(String newValue) {
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
}
