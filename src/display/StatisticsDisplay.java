package display;

import basic.*;

import java.text.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2005
 * Company:
 * @author
 * @version 1.0
 */

public class StatisticsDisplay extends JFrame{
  TemperatureStats stats;
  ThreeDCharge charge;
  String strFmtTemp = "####";
  String strFmtPos = "#.###";
  String strFmtHeat = "###,###,###";
  DecimalFormat forTemp = new DecimalFormat(strFmtTemp);
  DecimalFormat forheat = new DecimalFormat(strFmtHeat);
  JTextField tfAvgTemp, tfhtMaxSurfTemp, tfhtMinSurfTemp,
              tfwtMaxSurfTemp, tfwtMinSurfTemp,
              tflnMaxSurfTemp, tflnMinSurfTemp;
  JTextField tfmaxTempAt, tfminTempAt;
  JTextField tfHeatFromTop, tfHeatFromBottom, tfHeatToSkids;

  public StatisticsDisplay(TemperatureStats statSrc) {
    super("Statitstics Display");
    stats = statSrc;
    this.charge = statSrc.charge;
    try  {
      jbInit();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  boolean jbInit() throws Exception {
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    QueryPanel basePanel = new QueryPanel();
    tfAvgTemp = new JTextField(6);
    MaxMinAvgAt maxmin = charge.getMaxMinAvgTemp();
    tfmaxTempAt = new JTextField(15); //(maxmin.getMaxAt().textValue(" ####", strFmtPos).length()));
    tfminTempAt = new JTextField(15); //(maxmin.getMinAt().textValue(" ####", strFmtPos).length()));
    tfhtMaxSurfTemp = new JTextField(6);
    tfhtMinSurfTemp = new JTextField(6);
    tfwtMaxSurfTemp = new JTextField(6);
    tfwtMinSurfTemp = new JTextField(6);
    tflnMaxSurfTemp = new JTextField(6);
    tflnMinSurfTemp = new JTextField(6);
    tfHeatFromTop = new JTextField(10);
    tfHeatFromBottom = new JTextField(10);
    tfHeatToSkids = new JTextField(10);
    basePanel.addQuery("Mean Temperature ", tfAvgTemp);
    basePanel.addQuery("Maximum Temperature ", tfmaxTempAt);
    basePanel.addQuery("Minimum Temperature ", tfminTempAt);
    basePanel.addQuery("Top Surface Mean Temperature", tfhtMaxSurfTemp);
    basePanel.addQuery("Bottom Surface Mean Temperature", tfhtMinSurfTemp);
    basePanel.addQuery("Front Surface Mean Temperature", tfwtMaxSurfTemp);
    basePanel.addQuery("Back Surface Mean Temperature", tfwtMinSurfTemp);
    basePanel.addQuery("Near End Surface Mean Temperature", tflnMaxSurfTemp);
    basePanel.addQuery("Far End Surface Mean Temperature", tflnMinSurfTemp);

    basePanel.addQuery("Heat to Charge from TOP", tfHeatFromTop);
    basePanel.addQuery("Heat to Charge from Bottom", tfHeatFromBottom);
    basePanel.addQuery("Heat to Skids from Charge", tfHeatToSkids);

    getContentPane().add(basePanel);
    return true;
  }

  public void update() {
    MaxMinAvgAt maxmin = charge.getMaxMinAvgTemp();
    tfAvgTemp.setText(forTemp.format(maxmin.getAvg()));
    tfmaxTempAt.setText(maxmin.getMaxAt().textValue(" ####", strFmtPos));
    tfminTempAt.setText(maxmin.getMinAt().textValue(" ####", strFmtPos));
    tfhtMaxSurfTemp.setText(forTemp.format(charge.getSurfaceTemp(ThreeDCharge.HEIGHTMAXFACE)));
    tfhtMinSurfTemp.setText(forTemp.format(charge.getSurfaceTemp(ThreeDCharge.HEIGHTMINFACE)));
    tfwtMaxSurfTemp.setText(forTemp.format(charge.getSurfaceTemp(ThreeDCharge.WIDTHMAXFACE)));
    tfwtMinSurfTemp.setText(forTemp.format(charge.getSurfaceTemp(ThreeDCharge.WIDTHMINFACE)));
    tflnMaxSurfTemp.setText(forTemp.format(charge.getSurfaceTemp(ThreeDCharge.LENGTHMAXFACE)));
    tflnMinSurfTemp.setText(forTemp.format(charge.getSurfaceTemp(ThreeDCharge.LENGTHMINFACE)));

    tfHeatFromTop.setText(forheat.format(stats.getTotalSurfHeat(true)));
    tfHeatFromBottom.setText(forheat.format(stats.getTotalSurfHeat(false)));
    tfHeatToSkids.setText(forheat.format(stats.getTotalSurfHeat(false, true)));
  }


}