package display;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 6/1/12
 * Time: 3:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class ErrorStatAndMsg {
    static final public String nlSpace = "\n   ";
    public boolean inError = false;
    public String msg;

    public ErrorStatAndMsg(boolean isError, String msg) {
        this.inError = isError;
        this.msg = msg;
    }
}
