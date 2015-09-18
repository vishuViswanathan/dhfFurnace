package level2.common;

import net.sf.antcontrib.logic.Throw;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 16-Jan-15
 * Time: 10:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class TagCreationException extends Exception {
    String tagName;
    String element;
    String equipment;
    String tagMsg;
    String elementMsg;

    public TagCreationException() {
        super();
    }

    public TagCreationException(Throwable cause) {
        super(cause);
    }

    public TagCreationException(String tagName, String message) {
        this.tagName = tagName;
        this.tagMsg = message;
    }

    public void setElement(String element, String addMsg) {
        this.element = element;
        tagMsg += ": " + addMsg;
    }

    public void setEquipment(String equipment, String addMsg) {
        this.equipment = equipment;
        tagMsg += ": " + addMsg;
    }

    public void setMessage(String msg) {
        tagMsg = msg;
    }

    public String getMessage() {
        return equipment + "." + element + "." + tagName + "::" + tagMsg;
    }
}
