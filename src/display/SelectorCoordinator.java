package display;

import basic.*;

import java.util.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2005
 * Company:
 * @author
 * @version 1.0
 */

public class SelectorCoordinator implements ChangeListener {
  Vector <Selector> followers;

  public SelectorCoordinator() {
    followers = new Vector<Selector>();
  }

  public void stateChanged(ChangeEvent ce) {
    Selector caller = (Selector)ce.getSource();
    int value = caller.getValue();
    Selector sel;
    Iterator followerIter = followers.iterator();
    int n = 0;
    while (followerIter.hasNext()) {
      sel = (Selector)followerIter.next();
      if (!(sel == caller)) {
        sel.setValue(value);
      }
    }
  }

  public void addFollower(Selector sel) {
    followers.add(sel);
  }

  void debug(String msg) {
    System.out.println("SelectorCoordinator: " + msg);
  }
}