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
    String tagMsg;

    public TagCreationException() {
        super();
    }

    public TagCreationException(Throwable cause) {
        super(cause);
    }

    public TagCreationException(String equipment, String processElement, Tag tag, String message) {
        tagName = equipment + "." + tag.dataSource + "." + processElement + "." + tag.tagName;
        this.tagMsg = message;
    }

    public String getMessage() {
        return tagName + ":\n" + tagMsg;
    }
}
