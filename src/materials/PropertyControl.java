package materials;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 10/17/12
 * Time: 5:40 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PropertyControl {
    boolean saveData(Object o);
    void inEdit(boolean bInEdit);
    boolean deleteData(Object o);
    void quit();
}
