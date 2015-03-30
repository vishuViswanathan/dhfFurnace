package PropertySetter;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 10/17/12
 * Time: 5:40 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PropertyControl {
    public boolean saveData(Object o);
    public void inEdit(boolean bInEdit);

    public void quit();
}
