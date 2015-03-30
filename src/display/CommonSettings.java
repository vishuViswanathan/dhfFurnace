package display;
import java.util.*;

// ||

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2005
 * Company:
 * @author
 * @version 1.0
 */

public class CommonSettings {
  Hashtable <String, Object> table;
  public CommonSettings() {
    table = new Hashtable<String, Object>();
  }

  boolean addItem(String key, Object obj) {
    if (key == null || obj == null)
      return false;
    if (table.containsKey(key)) {
      debug("Key Exists '" + key + "'");
      return false;
    }
    table.put(key, obj);
    return true;
  }

  boolean changeItem(String key, Object obj) {
    if (key == null || obj == null)
      return false;
    if (!table.containsKey(key)) {
      debug("Key Does Not Exist '" + key + "'");
      return false;
    }
    table.remove(key);
    table.put(key, obj);
    return true;
  }

  Object getItem(String key) {
    if (key == null)
      return null;
    if (!table.containsKey(key)) {
      debug("getItem - Key Does Not Exist '" + key + "'");
      return null;
    }
    return table.get(key);
  }

  void debug(String msg) {
    System.out.println("CommonSettings: " + msg);
  }

}
