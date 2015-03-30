package display;
import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2005
 * Company:
 * @author
 * @version 1.0
 */

public class GetFileName {
  File lastDirectory = null;
  String ext, description;
  boolean toRead;

  public GetFileName(boolean toRead, String ext, String description) {
    this.toRead = toRead;
    this.ext = ext;
    this.description = description;
  }

  public void setDirectory(File dir) {
    lastDirectory = dir;
  }

  public File getDirectory() {
    return lastDirectory;
  }

  public String getIt(Component parent) {
    String fName = null;
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogType(JFileChooser.SAVE_DIALOG);
    TheFileFilter filter = new TheFileFilter();
    filter.addExtension(ext);
    filter.setDescription(description);
    chooser.setAcceptAllFileFilterUsed(false);
    chooser.setFileFilter(filter);

    if (lastDirectory != null)
      chooser.setCurrentDirectory(lastDirectory);
    int returnVal;
    do {
      if (toRead) {
        returnVal = chooser.showOpenDialog(parent);
      }
      else
        returnVal = chooser.showSaveDialog(parent);

      if(returnVal == JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();
        fName = file.getAbsolutePath();
        if (fName == null)
          errMessage("No File Selected!");
        else {
          lastDirectory =chooser.getCurrentDirectory();
          if (toRead) {
            if (!file.exists()) {
              errMessage("File does not exist!");
              fName = null;
            }
          }
          else {
            if (!filter.accept(file)) {
              fName = fName + "." + ext;
              file = new File(fName);
            }
            if (file.exists()) {
              if (!getConfirmation("Do you want to overwrite the file: " + fName)) {
                fName = null;
                continue;
              }
            }
          }
        }
        break;
      }
      else
        errMessage("No File Selected!");
      break;
    } while (true);
    return fName;
  }



  void errMessage(String msg) {
    JOptionPane.showMessageDialog(null, msg, "Get File",
                JOptionPane.ERROR_MESSAGE);
  }

  boolean getConfirmation(String msg) {
    return (JOptionPane.showConfirmDialog(null, msg, "Get File",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
  }

}