package protection;

import directFiredHeating.DFHeating;
import mvUtils.display.StatusWithMessage;

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
        String retVal = "";
        try {
            InetAddress ip = InetAddress.getLocalHost();
//            debug("Current IP address : " + ip.getHostAddress());
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);

            byte[] mac = network.getHardwareAddress();
            if (mac == null)
                debug("Could not get mac");
            else
                retVal = produceIDString(mac);
        } catch (UnknownHostException e) {
            debug("Host no known");
        } catch (SocketException e){
            debug("Socket error");
       	}
        return retVal;
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
        StringBuilder key = new StringBuilder();
        try {
            for (int p = 0; p < (len - 1); p+= 2) {
                keyBase.append(String.format("%04d", (1000 - Integer.parseInt(idStr.substring(p, p + 2), 16))));
            }
            // reverse it
            for (int p = keyBase.length() - 1; p >= 0; p -= 2)
                key.append(keyBase.substring(p, p + 1));
        } catch (NumberFormatException e) {
            key.append("ERROR in processing input code");
        }
        return key.toString();
    }


    public StatusWithMessage checkKey(String key) {
        StatusWithMessage retVal = new StatusWithMessage();
        String machineID = getMachineID();
        if (machineID.length() > 5) {
            if (!key.equals(getKey(machineID)))
                retVal.setInfoMessage("Key Mismatch");
        }
        else
            retVal.setErrorMessage("It appears that the Network is not connected");
        return retVal;
    }

    public boolean checkKey(String machineID, String key) {
        return getKey(machineID).equals(key);
    }

    void debug(String msg)  {
        DFHeating.debugLocal("MachineCheck: " + msg);
    }
}
