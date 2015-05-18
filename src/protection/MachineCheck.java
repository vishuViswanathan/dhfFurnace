package protection;

import directFiredHeating.DFHFurnace;
import directFiredHeating.DFHeating;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 08-May-15
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class MachineCheck {
    public String getMachineID() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            debug("Current IP address : " + ip.getHostAddress());

            NetworkInterface network = NetworkInterface.getByInetAddress(ip);

            byte[] mac = network.getHardwareAddress();
            return produceIDString(mac);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e){
       		e.printStackTrace();
       	}
        return "";
    }

    String produceIDString(byte[] mac) {
        StringBuilder idStr = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            idStr.append(String.format("%02X", (255-mac[i])));
        }
        return idStr.toString();
    }

    byte[] getMacFromMachineIdDStr(String idStr) {
        int len = idStr.length();
        byte[] mac = new byte[len / 2];
        for (int p = 0; p < len; p += 2) {
            mac[p / 2] = (byte)(255 - Byte.valueOf(idStr.substring(p, p + 2)));
        }
        return mac;
    }

    public String getKey(String idStr) {
        int len = idStr.length();
        StringBuilder keyBase = new StringBuilder();
        int[] modId = new int[len] ;
        for (int p = 0; p < len; p+= 2) {
            keyBase.append(String.format("%04d", ((int)1000 - Integer.parseInt(idStr.substring(p, p + 2), 16))));
        }
        // reverse it
        StringBuilder key = new StringBuilder();
        for (int p = keyBase.length() - 1; p >= 0; p -= 2)
            key.append(keyBase.substring(p, p + 1));
        return key.toString();
    }


    public boolean checkKey(String key) {
        return key.equals(getKey(getMachineID()));
    }

    public boolean checkKey(String machineID, String key) {
        return getKey(machineID).equals(key);
    }

    void debug(String msg)  {
        DFHeating.debug("MachineCheck: " + msg);
    }
}
