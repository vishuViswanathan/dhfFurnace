package level2.common;

import TMopcUa.*;
import com.prosysopc.ua.client.MonitoredDataItem;
import level2.common.L2ParamGroup;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;

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
        Running("Running"),
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
        SpeedMin("SpeedMin"),
        Span("span"),
        Data("Data"),
        TotFuel("totFuel"), // used for building TotFuel1, TotFue2 etc.
        TotFuel1("totFuel1"),
        TotFuel2("totFuel2"),
        TotFuel3("totFuel3"),
        TotFuel4("totFuel4"),
        TotFuel5("totFuel5"),
        TotFuel6("totFuel6"),
        TotFuel7("totFuel7"),
        TotFuel8("totFuel8"),
        TotFuel9("totFuel9"),
        TotFuel10("totFuel10"),

        ZoneFuel("zoneFuel"),   // used for building ZoneFuel1, ZoneFue2 etc.
        ZoneFuel1("zoneFuel1"),
        ZoneFuel2("zoneFuel2"),
        ZoneFuel3("zoneFuel3"),
        ZoneFuel4("zoneFuel4"),
        ZoneFuel5("zoneFuel5"),
        ZoneFuel6("zoneFuel6"),
        ZoneFuel7("zoneFuel7"),
        ZoneFuel8("zoneFuel8"),
        ZoneFuel9("zoneFuel9"),
        ZoneFuel10("zoneFuel10"),

        Speed("speed"),  // used for building Speed1, Speed2 etc.
        Speed1("speed1"),
        Speed2("speed2"),
        Speed3("speed3"),
        Speed4("speed4"),
        Speed5("speed5"),
        Speed6("speed6"),
        Speed7("speed7"),
        Speed8("speed8"),
        Speed9("speed9"),
        Speed10("speed10"),

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
        Count("count"),
        ExitTemp("exitTemp"),
        Wmin("Wmin"),
        Wmax("Wmax"),
        THmin("THmin"),
        THmax("THmax"),
        // Strings
//        Process("process"),
        BaseProcess("baseProcess"),
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

        public TagName buildTag(int index) {
            return getEnum(name + ("" + index).trim());
        }
    }

    int datW = 6;
    BooleanDisplay booleanStat;

    public TagName tagName;
    public boolean bSubscribe = false;
    public L2ParamGroup.Parameter element;
    public ProcessData.DataType dataType;
    public ProcessData.Source dataSource;  // Level2 or Process
    public ProcessData.Access access;
    protected ProcessData processData;
    ProcessValue value = new ProcessValue();
    Component displayComponent;
    DecimalFormat format;

    public Tag(L2ParamGroup.Parameter element, TagName tagName, boolean rw, boolean bSubscribe) {
        this(element, tagName, rw, bSubscribe, "#,###.00");
    }

    public Tag(L2ParamGroup.Parameter element, TagName tagName, boolean rw, boolean bSubscribe, String fmtStr) {
        this.element = element;
        this.tagName = tagName;
        this.dataType = getDataType(tagName);
        access = (rw) ? ProcessData.Access.RW : ProcessData.Access.READONLY;
        dataSource = (rw) ? ProcessData.Source.LEVEL2 : ProcessData.Source.PROCESS;
        this.bSubscribe = bSubscribe;
        noteFormat(fmtStr);
        createDisplayComponent();
        if (displayComponent != null) {
            displayComponent.setEnabled(false);
        }

    }

    public Tag(L2ParamGroup.Parameter element, TagName tagName, boolean rw, boolean bSubscribe, String[] onOffString) {
        this(element, tagName, rw, bSubscribe, "#,##.00");
        noteFormat(onOffString);
    }

    String fmtStr;

    protected void noteFormat(String fmtStr) {
        this.fmtStr = fmtStr;
        switch(tagName) {
            case Auto:
                booleanStat = new BooleanDisplay("Auto", "Manual");
                break;
            case Remote:
                booleanStat = new BooleanDisplay("Remote", "Local");
                break;
            case Enabled:
                booleanStat = new BooleanDisplay("Enabled", "Disabled");
                break;
            case Running:
                booleanStat = new BooleanDisplay("Running", "Stopped");
                break;
            case Noted:
                booleanStat = new BooleanDisplay("Noted", "");
                break;
            case Ready:
                booleanStat = new BooleanDisplay("Ready", "");
                break;
            case Mode:
                booleanStat = new BooleanDisplay("ON", "OFF");
                break;
            case Response:
                booleanStat = new BooleanDisplay("Received", "OFF");
                break;
            default:
                format = new DecimalFormat(fmtStr);
                datW = fmtStr.length();
                break;
        }
    }

    protected void noteFormat(String[] onOffStr) {
        switch(tagName) {
            case Auto:
            case Remote:
            case Enabled:
            case Running:
            case Noted:
            case Ready:
            case Mode:
            case Response:
                booleanStat = new BooleanDisplay(onOffStr[0], onOffStr[1]);
                break;
        }
    }

    protected void createDisplayComponent() {
        switch(tagName) {
            case Auto:
            case Remote:
            case Enabled:
            case Running:
            case Noted:
            case Ready:
            case Mode:
            case Response:
                JTextField tf = new JTextField(datW);
                tf.setDisabledTextColor(Color.BLUE);
                displayComponent = tf;
                break;
            case Msg:
                JTextArea ta = new JTextArea(4, datW);
                ta.setDisabledTextColor(Color.BLUE);
                displayComponent = ta;
                break;
            default:
//                JTextField tf1 = new JTextField(datW);
                JTextField tf1 = new JTextField("F" + fmtStr);
                tf1.setDisabledTextColor(Color.BLUE);
                displayComponent = tf1;
                if (format != null)
                    ((JTextField)displayComponent).setHorizontalAlignment(JTextField.RIGHT);
                break;
        }
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
            case Running:
            case Noted:
            case Ready:
            case Mode:
            case Response:
                type = ProcessData.DataType.BOOLEAN;
                break;
            case BaseProcess:
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

    public Component displayComponent() {
        return displayComponent;
    }

    public void updateUI() {
        switch(tagName) {
            case Auto:
            case Remote:
            case Enabled:
            case Running:
            case Noted:
            case Ready:
            case Mode:
            case Response:
                ((JTextField)displayComponent).setText(booleanStat.statusString());
                break;
            case BaseProcess:
                ((JTextField)displayComponent).setText(getValue().stringValue);
                break;
            case Msg:
                ((JTextArea)displayComponent).setText(getValue().stringValue);
                break;
            default:
                ((JTextField)displayComponent).setText(format.format(getValue().floatValue));
                break;
        }
    }

    class BooleanDisplay {
        String trueName;
        String falseName;

        BooleanDisplay(String trueName, String falseName) {
            this.trueName = trueName;
            this.falseName = falseName;
            datW = textLen();
        }

        String statusString() {
            if (getValue().booleanValue)
                return trueName;
            else
                return falseName;
        }

        int textLen() {
            return Math.max(trueName.length(), falseName.length());
        }
    }
}
