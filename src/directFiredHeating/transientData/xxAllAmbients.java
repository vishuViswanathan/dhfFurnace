package directFiredHeating.transientData;

import basic.AmbientCycle;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.StringTokenizer;
import java.util.Vector;

public class xxAllAmbients {

    static Vector<AmbientCycle> takeAmbientCyclesFromString(String ambCylesStr) {
        Vector<AmbientCycle> ambCycles = null;
        String oneLine;
        AmbientCycle ambCycle;
        StringReader src = new StringReader(ambCylesStr);
        BufferedReader reader = new BufferedReader(src);
        boolean allOk = false;
        do {
            oneLine = getOneValidLine(reader); // first header
            if (!oneLine.startsWith("Ambient Data File")) {
                errMessage("Not a proper Ambient data File!");
                break;
            }
            oneLine = getOneValidLine(reader); // second header
            // can check Revision number here
            if (!oneLine.startsWith("Version")) {
                errMessage("Not a proper Ambient data File (Version ...)!");
                break;
            }
            oneLine =  getOneValidLine(reader); // the Ambinet count line;
            if (!oneLine.startsWith("Number of Ambients")) {
                errMessage("Not a proper Ambient data File (Number of Ambients ...)!");
                debug("Line: " + oneLine);
                break;
            }
            int ambCount = new Float(getValueAfterSymbol(oneLine, '=')).intValue();
            for (int nowAmb = 0; nowAmb < ambCount; nowAmb++) {
                ambCycle = getOneAmbCycleFromFile(reader);
                if (ambCycle == null)
                    break;
                ambCycles.add(ambCycle);
            }
            allOk = true;
            break;
        } while (true);

        try {
            src.close();
        }
        catch (Exception e) {
            errMessage("Unable to close the file!");
            e.printStackTrace();
        }
        return ambCycles;
    }

    // returns a trimmed line
    static String getOneValidLine(BufferedReader reader) {
        String oneLine = null;
        boolean found = false;
        while (!found) {
            try {
                oneLine = reader.readLine();
                if (oneLine == null)
                    break;
                if (oneLine.charAt(0) == '#')
                    continue;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            break;
        } // while not found
        if (oneLine != null)
            oneLine = oneLine.trim();
        return oneLine;
    }

    // reads one ambient cycle
    static AmbientCycle getOneAmbCycleFromFile(BufferedReader reader) {
        String oneLine = getOneValidLine(reader);
        AmbientCycle ambCycle = null;
        boolean done = false;
        do {
            if (oneLine == null || !oneLine.startsWith("Name")) {
                errMessage("Ambient Cycle Name Entry NOT found");
                debug("Line: " + oneLine);
                break;
            }
            String name = getStrAfterSymbol(oneLine, '=');
            if (name == null) {
                errMessage("Ambient Cycle Name NOT found");
                debug("Line: " + oneLine);
                break;
            }
            ambCycle = new AmbientCycle(name);  // got the ambient name
            // get the steps
            oneLine = getOneValidLine(reader);
            if (oneLine == null || !oneLine.startsWith("Steps")) {
                errMessage("Ambient Cycle Steps Entry NOT found");
                debug("Line: " + oneLine);
                break;
            }
            done = true;
            int steps = new Float(getValueAfterSymbol(oneLine, '=')).intValue();
            String[] dataStr = {"", "", ""};
            for (int nowStep = 0; nowStep < steps; nowStep++) {
                oneLine = getOneValidLine(reader);
                if (oneLine == null)
                    break;
                if (breakDelimtedString(oneLine, ',', dataStr) < 3)
                    break;
                ambCycle.noteCycleSegment(Double.parseDouble(dataStr[0]),
                        Double.parseDouble(dataStr[1]),
                        Double.parseDouble(dataStr[2]));
            }
            break;
        } while (true);
        if (done) {
            ambCycle.makeItReady();
            return ambCycle;
        }
        else
            return null;
    }

    static float getValueAfterSymbol(String str, char symbol) {
        float val = Float.NaN;
        int symbLoc = str.indexOf(String.valueOf(symbol));
        if (symbLoc >= 0) {
            String subStr = str.substring(symbLoc + 1);
            if (subStr != null)
                val = Float.parseFloat(subStr);
        }
        return val;
    }

    static String getStrAfterSymbol(String source, char symbol) {
        String str = null;
        int symbLoc = source.indexOf(String.valueOf(symbol));
        if (symbLoc >= 0) {
            str = source.substring(symbLoc + 1);
            if (str != null)
                str = str.trim();
        }
        return str;
    }

    static int breakDelimtedString(String source, char symbol, String[] breakup) {
        int count = 0;
        StringTokenizer strTok = new StringTokenizer(source, String.valueOf(symbol));
        int maxCount = breakup.length;
        while (strTok.hasMoreTokens() && count < maxCount) {
            breakup[count] = strTok.nextToken().trim();
            count++;
        }
        return count;
    }

    static void errMessage(String msg) {
        JOptionPane.showMessageDialog(null, msg, "AllAmbients",
                JOptionPane.ERROR_MESSAGE);
    }

    static void debug(String msg) {
        System.out.println("AllAmbients: " + msg);
    }
}
