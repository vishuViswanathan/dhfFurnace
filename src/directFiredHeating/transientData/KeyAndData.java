package directFiredHeating.transientData;

public class KeyAndData<V> {
    public double key;
    public V data;

    KeyAndData(double key, V data) {
        this.key = key;
        this.data = data;
    }
}
