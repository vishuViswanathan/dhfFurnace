package protection;

import directFiredHeating.DFHeating;
import mvUtils.display.DataStat;
import mvUtils.display.DataWithStatus;
import mvUtils.display.SimpleDialog;
import mvUtils.http.PostToWebSite;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.security.GCMCipher;
import mvUtils.security.MachineCheck;
import mvUtils.security.MiscUtil;

import javax.crypto.SecretKey;
import java.io.*;
import java.util.HashMap;

/**
 * User: M Viswanathan
 * Date: 08-May-17
 * Time: 10:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class CheckAppKey {
    enum KeyStat  {FOUND, NOTIAPPFOLDER, NOFILE, NOBASEFOLDER, NOENTRY, ERRORREADING};
    String jspBase;
    String machineID;
    MachineCheck mC;
    final String newLine = "\r\n";
    final String separator = " ";
    String tIAppFolder = "TIApplications";
    String fileName = "softwareKey.ini";
    String user;
    byte[] localKey  = {100};
    GCMCipher cipher;

    public CheckAppKey(String jspBase) {
        this.jspBase = "http://" + jspBase;
        mC = new MachineCheck();
        machineID = mC.getMachineID(true);
        user = MiscUtil.getUser();
        cipher = new GCMCipher();
    }

    /**
     *
     * @param appID
     * @param updateIfNot if true then upadte local database if allowed from App server
     * @return
     */

    public DataWithStatus<Boolean> canRunThisApp(int appID, boolean updateIfNot) {
        DataWithStatus<Boolean> retVal = new DataWithStatus<>();
        KeyWithKeyStat softwareKeyStat = getSavedSoftwareKey(encryptInt(appID));
        KeyStat stat = softwareKeyStat.stat;
        switch (stat) {
            case FOUND:
                String storedKey = softwareKeyStat.key;
                MachineCheck mc = new MachineCheck();
                if ((mc.checkKey(storedKey, true, appID).getDataStatus() == DataStat.Status.OK))
                    retVal.setValue(true);
                else
                    retVal.setErrorMessage("Local Software key does not match");
                break;
            case NOBASEFOLDER:
                retVal.setErrorMessage("Could not find the Base folder for Application Settings");
                break;
            case ERRORREADING:
                retVal.setErrorMessage("Error in Reading Application Settings");
                break;
            default:
                if (updateIfNot) {
                    retVal= createAndSaveSoftwareKey(appID, stat, true);
                }
                else
                    retVal.setErrorMessage("No Local entry of software key");
        }
        if (retVal.getStatus() == DataStat.Status.WithErrorMsg) {
            SimpleDialog.showError("Checking Application Access", retVal.getErrorMessage());
        }
        return retVal;
    }

    KeyWithKeyStat getSavedSoftwareKey(String appID)  {
        KeyWithKeyStat ks = new KeyWithKeyStat(KeyStat.NOENTRY);
        String baseFolder = MiscUtil.appDataFolder();
        if (baseFolder.length() > 2) {
            File tIFolder = new File(baseFolder + "\\" + tIAppFolder);
            if (tIFolder.isDirectory()) {
                File f = new File(baseFolder + "\\" + tIAppFolder + "\\" + fileName);
                if (f.exists()) {
                    try {
                        BufferedInputStream iStream = new BufferedInputStream(new FileInputStream(f));
                        long len = f.length();
                        if (len > 50 && len < 100000) {
                            int iLen = (int) len;
                            byte[] data = new byte[iLen + 10];
                            try {
                                if (iStream.read(data) > 50) {
                                    String allData = new String(data);
                                    String[] lines = allData.split(newLine);
                                    boolean found = false;
                                    String key = "";
                                    for (String line : lines) {
                                        String field[] = line.split(separator);
                                        if (field.length == 3 && field[0].equals(appID)) {
                                            found = true;
                                            key = field[2];
                                            break;
                                        }
                                    }
                                    if (found)
                                       ks = new KeyWithKeyStat(key);
                                    else
                                        ks.stat = KeyStat.NOENTRY;
                                }
                            } catch (IOException e) {
                                ks.stat = KeyStat.ERRORREADING;
                            } finally {
                                try {
                                    if (iStream != null)
                                        iStream.close();
                                } catch (IOException e) {
                                    ks.stat = KeyStat.ERRORREADING;
                                }
                            }
                        }
                    } catch (FileNotFoundException e) {
                        ks.stat = KeyStat.NOFILE;
                    }
                }
                else
                    ks.stat = KeyStat.NOFILE;
            }
            else
                ks.stat = KeyStat.NOTIAPPFOLDER;
        }
        else
            ks.stat = KeyStat.NOBASEFOLDER;
        return ks;
    }

    DataWithStatus<Boolean> createAndSaveSoftwareKey(int appID, KeyStat nowStat,  boolean createNewFile) {
        DataWithStatus<Boolean> retVal = new DataWithStatus<>(false);
        String baseFolder = MiscUtil.appDataFolder();
        debug("folder = " + baseFolder);
        File f = new File(baseFolder + "\\" + tIAppFolder + "\\" + fileName);
        boolean fileReady = false;
        boolean folderReady = false;
        if (baseFolder.length() > 2) {
            switch (nowStat) {
                case NOTIAPPFOLDER:
                    if (createNewFile) {
                        File folder = new File(baseFolder + "\\" + tIAppFolder);
                        if (folder.mkdir()) {
                            folderReady = true;
                        }
                        else {
                            retVal.addErrorMessage("Facing problem in creating AppAccess folder");
                            break;
                        }
                    }
                case NOFILE:
                    if (createNewFile) {
                        if (nowStat == KeyStat.NOFILE || folderReady) {
                            try {
                                if (f.createNewFile())
                                    fileReady = true;
                                else {
                                    retVal.addErrorMessage("Facing problem in creating permissions file");
                                    break;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                case NOENTRY:
                    if (nowStat == KeyStat.NOENTRY || fileReady) {
                        DataWithStatus fileSaveResp = makeEntryToFile(f, appID);
                        if (fileSaveResp.getStatus() == DataStat.Status.OK && (Boolean)fileSaveResp.getValue())
                            retVal.setValue(true);
                        else
                            retVal.addErrorMessage("Unable to make entry to file, " + fileSaveResp.getErrorMessage());
                    }
                    break;
            }
        }
        return retVal;
    }

    DataWithStatus<Boolean> makeEntryToFile(File f, int appID) {
        DataWithStatus retVal = new DataWithStatus(false);
        DataWithStatus appKeyResp = getAppKeyFromServer(appID);
        if (appKeyResp.getStatus() == DataStat.Status.OK) {
            String appIDcrypt = encryptInt(appID);
//            String mIDCrpt = cipher.bytesToByteString(cipher.encrypt(machineID, localKey));
            if (f.exists()) {
                try {
                    FileWriter fileWriter = new FileWriter(f,true);
                    BufferedWriter bw = new BufferedWriter(fileWriter);
                    bw.write(appIDcrypt + separator + machineID + separator + appKeyResp.getValue() + newLine);
                    bw.close();
                    retVal.setValue(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        else
            retVal.addErrorMessage(appKeyResp.getErrorMessage());
        return retVal;
    }

    DataWithStatus<String> getAppKeyFromServer(int appID) {
        DataWithStatus<String> retVal = new DataWithStatus<>("ERROR");
        PostToWebSite jspReq =  new PostToWebSite(jspBase);
        HashMap<String, String> params = new HashMap<>();
//        byte[] keyBytes = localKey;
//        String key =  cipher.bytesToByteString(keyBytes);
//        String uidxx = cipher.encryptStringWithKey(user, key);
//        params.put("user", uidxx);
        params.put("user", user);
        params.put("appCode", ("" + appID).trim());
//        String machineIDxx = cipher.encryptStringWithKey(machineID, key);
//        params.put("mID", machineIDxx);
        params.put("mID", machineID);
//        String keyXX =  cipher.bytesToByteString(cipher.encrypt(keyBytes));
//        params.put("key", keyXX);
        long respLen = 2000l;
        String response = jspReq.getByPOSTRequest("getAppKey.jsp", params, respLen);
        ValAndPos vp;
        vp = XMLmv.getTag(response, "Status", 0);
        if (vp.val.length() > 0) {
            if (vp.val.equalsIgnoreCase("OK")) {
                vp = XMLmv.getTag(response, "softwareKey", vp.endPos);
                if (vp.val.length() > 0)
                    retVal.setValue(vp.val.trim());
                else
                    retVal.setErrorMessage("Got empty softkey");
            }
            else {
                vp = XMLmv.getTag(response, "Msg", vp.endPos);
                retVal.addErrorMessage(vp.val);
            }
        }
        else
            retVal.setErrorMessage("No Status data from Server " );
        return retVal;
    }

    String encryptInt(int i) {
        String inStr = ("" + i).trim();
        GCMCipher cipher = new GCMCipher();
        String key =  cipher.bytesToByteString(localKey);
        return cipher.encryptStringWithKey(inStr, key);
    }

    class KeyWithKeyStat {
        String key;
        KeyStat stat;
        KeyWithKeyStat(String key) {
            this(KeyStat.FOUND);
            this.key = key;
        }

        KeyWithKeyStat(KeyStat key) {
            this.stat = key;
        }
    }

    void debug(String msg)  {
        System.out.println("CheckAppKey: " + msg);
    }

}
