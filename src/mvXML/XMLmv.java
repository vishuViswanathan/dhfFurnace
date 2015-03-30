package mvXML;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 14 March 2012
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class XMLmv {
    String xmlData;

    public XMLmv(String xmlSrc) {
        xmlData = xmlSrc;
        // format checking to be added here
    }


    public static ValAndPos getTag(String data, String tag, int pos) {
        if (data.length() > (2 * tag.length() + 5)) {
            String lcTag = tag.toLowerCase();
            String lcData = data.toLowerCase();
            String stTag = "<" + lcTag + ">";
            String endTag = "</"  + lcTag + ">";
            int stFound = lcData.indexOf(stTag, pos);
            int stLoc =  stFound + stTag.length();
            int endLoc = lcData.indexOf(endTag, stLoc);
            if ((stFound >= 0) && (endLoc > stLoc))
                return new ValAndPos(data.substring(stLoc, endLoc).trim(), endLoc + endTag.length());
            else
                return new ValAndPos("", pos);
        }
        else
            return new ValAndPos("", pos);
     }

    public static ValAndPos getTag(String data, String tag) {
        return getTag(data, tag, 0);
    }

    public static String putTag(String tag, String val)  {
        return("<" + tag + ">" + val + "</" + tag + ">\n");
    }

    public static String putTag(String tag, StringBuilder val)  {
        return("<" + tag + ">" + val + "</" + tag + ">\n");
    }

    public static String putTag(String tag, Number val)  {
        return putTag(tag, "" + val);
    }

    public static String putTag(String tag, long val)  {
        return putTag(tag, "" + val);
    }

    public static String putTag(String tag, boolean val)  {
        return putTag(tag, ((val) ? "1" : "0"));
    }

// with StringBuilder
    public static StringBuilder putTagNew(String tag, String val)  {
        return new StringBuilder("<" + tag + ">" + val + "</" + tag + ">\n");
    }

    public static StringBuilder putTagNew(String tag, StringBuilder val)  {
        return new StringBuilder("<" + tag + ">" + val + "</" + tag + ">\n");
    }

    public static StringBuilder putTagNew(String tag, Number val)  {
        return putTagNew(tag, "" + val);
    }

    public static StringBuilder putTagNew(String tag, long val)  {
        return putTagNew(tag, "" + val);
    }

    public static StringBuilder putTagNew(String tag, boolean val)  {
        return putTagNew(tag, ((val) ? "1" : "0"));
    }


    public static DoubleWithErrStat getDoubleWithErrStat(String xmlStr, String header, String tag, XMLgroupStat grpStat) {
        ValAndPos vp = XMLmv.getTag(xmlStr, tag, 0);
        return new DoubleWithErrStat(vp.val, header + tag, grpStat);
    }

    public static DoubleWithErrStat getDoubleWithErrStat(String xmlStr, String tag, XMLgroupStat grpStat) {
        return getDoubleWithErrStat(xmlStr, "", tag, grpStat);
    }

}

