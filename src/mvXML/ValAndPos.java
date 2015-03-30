package mvXML;

/**
 * Created by IntelliJ IDEA.
 * User: viswanathanm
 * Date: 3/14/12
 * Time: 11:08 AM
 * To change this template use File | Settings | File Templates.
 */
public class ValAndPos {
    public String val;
    public int endPos;
    public ValAndPos(String val, int  lastPos){
        this.val = val;
        endPos = lastPos;
    }
}
