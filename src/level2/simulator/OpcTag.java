package level2.simulator;

import level2.common.L2ParamGroup;
import level2.common.Tag;
import mvUtils.display.InputControl;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 11-Sep-15
 * Time: 11:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class OpcTag {
    String name;
    String dataType;
    String readWriteAccess;
    boolean rW;

    @Override
    public String toString() {
        return name + " (" + dataType + ")" + readWriteAccess;
    }

    public TagWithDisplay getTagWithDisplay(L2ParamGroup.Parameter element, boolean rwForSimulator, InputControl ipc) {
        return new TagWithDisplay(element, Tag.TagName.getEnum(name), !rwForSimulator, false, "#,##0.000", ipc );
//        return new TagWithDisplay(element, Tag.TagName.getEnum(name), !rwForSimulator, !rwForSimulator, "#,##0.000", ipc );
    }
}
