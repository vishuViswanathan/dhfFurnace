package level2.simulator;

import level2.common.L2ParamGroup;
import mvUtils.display.InputControl;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 11-Sep-15
 * Time: 11:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class OpcTagGroup {
    String name;
    Vector<OpcTag> tags;
    Vector<OpcTagGroup> subGroups;

    OpcTagGroup() {
        this("NoName");
    }

    OpcTagGroup(String name)  {
        subGroups = new Vector<OpcTagGroup>();
        tags = new Vector<OpcTag>();
        setName(name);
    }

    void setName(String name) {
        this.name = name;
    }

    void addGroup(OpcTagGroup grp) {
        subGroups.add(grp);
    }

    void addTag(OpcTag tag) {
        tags.add(tag);
    }

    /**
     *
     * @param path a ':' separated path
     * @return
     */
    OpcTagGroup getSubGroup(String path) {
        String[] pathArr = path.split(":", 2);
        OpcTagGroup retGrp = null;
        String oneName = pathArr[0];
        for (OpcTagGroup oneGrp : subGroups) {
            if (oneGrp.name.equalsIgnoreCase(oneName)) {
                retGrp = oneGrp;
                break;
            }
        }
        if ((pathArr.length > 1) && (retGrp != null))
            return retGrp.getSubGroup(pathArr[1]);
        else
            return retGrp;
    }

    Vector<TagWithDisplay> getVTags(boolean rwForSimulator, InputControl ipc) {
        Vector<TagWithDisplay> vTags = new Vector<TagWithDisplay>();
        for (OpcTag oneTag: tags)
            vTags.add(oneTag.getTagWithDisplay(L2ParamGroup.Parameter.getEnum(name), rwForSimulator, ipc));
        return vTags;
    }
}
