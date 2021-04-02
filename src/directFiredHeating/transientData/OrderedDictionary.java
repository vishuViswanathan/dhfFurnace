package directFiredHeating.transientData;

import java.util.Vector;

/**
 *
 * @param <V>
 *     The entries have to be in increasing oder of key
 */

public class OrderedDictionary<V> {
    Vector<KeyAndData> keyAndData;
    int size = 0;
    public OrderedDictionary() {
        keyAndData = new Vector<>();
    }

    /**
     *
     * @param keyVal - The entries have to be in increasing oder of key
     * @param oneVal
     * @return the size of the updated collection if successful else 0
     */

    public int addData(double keyVal, V oneVal) {
        if (getData(keyVal) == null &&
                (size == 0 || getDataSet(size-1).key < keyVal)) {
            keyAndData.add(new KeyAndData<>(keyVal, oneVal));
            size = keyAndData.size();
            return keyAndData.size();
        }
        else
            return 0;
    }

    public int getSize() {
        return size;
    }

    public V getData(double keyVal) {
        V retVal = null;
        for (KeyAndData d : keyAndData) {
            if (d.key == keyVal) {
                retVal = (V)(d.data);
            }
        }
        return retVal;
    }

    KeyAndData<V> getDataSet(int index) {
        KeyAndData retVal = null;
        if (index >= 0 & index < size)
            retVal = keyAndData.get(index);
        return retVal;
    }

    void debug(String msg) {
        System.out.println("OrderedDictionary: " + msg);
    }

    public static void main(String[] args) {
        OrderedDictionary<String> d = new OrderedDictionary<>();
        d.addData(7.3, "value Ten.Two");
        d.addData(10.2, "value Seven.Three");
        d.debug(d.getData(10.2));
        d.debug(d.getData(7.3));
    }
}
