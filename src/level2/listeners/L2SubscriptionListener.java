package level2.listeners;

import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.MonitoredEventItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionNotificationListener;
import org.opcfoundation.ua.builtintypes.*;
import org.opcfoundation.ua.core.NotificationData;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 28-Jan-15
 * Time: 3:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2SubscriptionListener implements SubscriptionNotificationListener {

    public L2SubscriptionListener() {

    }

    public void onBufferOverflow(Subscription subscription, UnsignedInteger unsignedInteger, ExtensionObject[] extensionObjects) {

    }

    public void onDataChange(Subscription subscription, MonitoredDataItem monitoredDataItem, DataValue dataValue) {

    }

    public void onError(Subscription subscription, Object o, Exception e) {

    }

    public void onEvent(Subscription subscription, MonitoredEventItem monitoredEventItem, Variant[] variants) {

    }

    public long onMissingData(UnsignedInteger unsignedInteger, long l, long l2, StatusCode statusCode) {
        return 0;
    }

    public void onNotificationData(Subscription subscription, NotificationData notificationData) {

    }

    public void onStatusChange(Subscription subscription, StatusCode statusCode, StatusCode statusCode2, DiagnosticInfo diagnosticInfo) {

    }
}
