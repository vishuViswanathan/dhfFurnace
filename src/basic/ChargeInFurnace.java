package basic;

import java.text.*;
import java.util.*;

import java.awt.event.*;
import javax.swing.*;

import display.*;

/**
 * <p>Title: ChargeInFurnace</p>
 * <p>Description: The class with interface will take care of Charge surface
 * Condition definitions - attaching to the loaded ambients. Will alos modify the
 * ambients suitably to different surface orientations based on charge type.
 * The Constructor will take ThreeDCharge as input</p>
 * <p>Copyright: Copyright (c) 2006</p>
 * <p>Author: M Viswanathan </p>
 * <p>Company: Techit Hypertherm </p>
 *
 * @version 1.0
 */

public class ChargeInFurnace {
    JFrame parent;
    ThreeDCharge ch;
    //  double chGap;
    AmbientCycle topAmb, botAmb, frontSideAmb, backSideAmb, nearEndAmb, farEndAmb;
    SkidData[] skids;
    QueryDialog qp;
    //  JTextField gapText;
    JTextField tfStartTemp;
    JComboBox topAmbCombo;
    JComboBox botAmbCombo;
    JComboBox frontSideAmbCombo;
    JComboBox backSideAmbCombo;
    JComboBox nearEndAmbCombo;
    JComboBox farEndAmbCombo;
    JComboBox[] skidFromCombo, skidToCombo;
    int maxSkids = 4;
    JTextField[] skidLoc, skidWidth;
    JComboBox[] skidAmbients;
    public double startTemp = 30;
    public boolean done = false;
    DecimalFormat format0Dec = new DecimalFormat("####");

    public ChargeInFurnace(ThreeDCharge charge, Vector ambients,
                           JFrame parent) {
        ch = charge;
        this.parent = parent;
        collectData(ambients);
    }

//  void setChargeGap(double gap) {
//    chGap = gap;
//  }

//  void notSkidData(SkidData[] skidsData) {
//    skids = skidsData;
//  }

//  double getChargeGap() {
//    return chGap;
//  }

    ThreeDCharge getChargeData() {
        return ch;
    }

    SkidData[] getSkidData() {
        return skids;
    }

    public boolean collectData(Vector ambients) {
        qp = new QueryDialog(parent, "Charge In Furnace Data");
//    gapText = new JTextField(5);
//    qp.addQuery("Charge gap (mm)", gapText);
        tfStartTemp = new JTextField(5);
        qp.addQuery("Start Temperature", tfStartTemp);
        qp.addSpace();
        topAmbCombo = new JComboBox(ambients);
        qp.addQuery("Ambient above Charge", topAmbCombo);
        botAmbCombo = new JComboBox(ambients);
        qp.addQuery("Ambient below Charge", botAmbCombo);
        frontSideAmbCombo = new JComboBox(ambients);
        qp.addQuery("Ambient in Charge gap-front", frontSideAmbCombo);
        backSideAmbCombo = new JComboBox(ambients);
        qp.addQuery("Ambient in Charge gap-back", backSideAmbCombo);
        nearEndAmbCombo = new JComboBox(ambients);
        qp.addQuery("Ambient for Near End", nearEndAmbCombo);
        farEndAmbCombo = new JComboBox(ambients);
        qp.addQuery("Ambient for Far End", farEndAmbCombo);
//    skidLoc = new JTextField[maxSkids];
//    skidWidth = new JTextField[maxSkids];
        skidAmbients = new JComboBox[maxSkids];
        skidFromCombo = new JComboBox[maxSkids];
        skidToCombo = new JComboBox[maxSkids];
        qp.addSpace();
        qp.addTextLine("SKID Postions along Charge Lengh");
        double[] edges = ch.getCellXEdgeList();
        for (int n = 0; n < maxSkids; n++) {
//      skidLoc[n] = new JTextField(5);
//      skidWidth[n] = new JTextField(5);
            skidAmbients[n] = new JComboBox(ambients);
            skidFromCombo[n] = new JComboBox();
            skidToCombo[n] = new JComboBox();
            for (double edge : edges) {
                skidFromCombo[n].addItem((int) (edge * 1000));
                skidToCombo[n].addItem((int) (edge * 1000));
            }
/*
        for (int i = 0; i < edges.length; i++) {
          skidFromCombo[n].addItem((int) (edges[i] * 1000));
          skidToCombo[n].addItem((int) (edges[i] * 1000));
        }
*/
            qp.addQuery("Skid #" + (n + 1) + " Start Pos (mm)", skidFromCombo[n]);
            qp.addQuery("Skid #" + (n + 1) + " End Pos (mm)", skidToCombo[n]);
//      qp.addQuery("Skid #" + (n+1) + " Location (mm)", skidLoc[n], 0, chargeLen);
//      qp.addQuery("Skid #" + (n+1) + " Width (mm)",
//                  skidWidth[n], 0, Math.min(200, (chargeLen / 2)));
            qp.addQuery("Skid #" + (n + 1) + " Ambient", skidAmbients[n]);
            qp.addSpace();
        }
        fillOldData();
        qp.setLocationRelativeTo(parent);
        qp.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                if (qp.isUpdated())
                    noteData();
            }
        }
        );
        qp.setVisible(true);
        return true;
    }

    boolean firstTimeFill = true;

    void fillOldData() {
//    gapText.setText(format0Dec.format(chGap * 1000));
        tfStartTemp.setText("" + startTemp);
        if (!firstTimeFill) {
            topAmbCombo.setSelectedItem(topAmb);
            botAmbCombo.setSelectedItem(botAmb);
            frontSideAmbCombo.setSelectedItem(frontSideAmb);
            backSideAmbCombo.setSelectedItem(backSideAmb);
            nearEndAmbCombo.setSelectedItem(nearEndAmb);
            farEndAmbCombo.setSelectedItem(farEndAmb);
        }

        int skidN = 0;
        SkidData theSkid;
        if (skids != null) {
            for (; skidN < skids.length; skidN++) {
                theSkid = skids[skidN];
//        skidLoc[skidN].setText(format0Dec.format(skids[skidN].getLocation() * 1000));
//        skidWidth[skidN].setText(format0Dec.format(skids[skidN].getWidth() * 1000));
                skidFromCombo[skidN].setSelectedIndex(theSkid.fromX - 1);
                skidToCombo[skidN].setSelectedIndex(theSkid.toX);
                AmbientCycle a = theSkid.getAmbient();
                skidAmbients[skidN].setSelectedItem(a);
            }
        }
        for (; skidN < maxSkids; skidN++) {
//      skidLoc[skidN].setText("" + 0);
//      skidWidth[skidN].setText("" + 0);
            skidFromCombo[skidN].setSelectedIndex(0);
            skidToCombo[skidN].setSelectedIndex(0);
            skidAmbients[skidN].setSelectedIndex(0);
        }
    }

    public void noteData() {
        firstTimeFill = false;
//    chGap = Double.parseDouble(gapText.getText()) / 1000;
        startTemp = Double.parseDouble(tfStartTemp.getText());
        topAmb = (AmbientCycle) topAmbCombo.getSelectedItem();
        botAmb = (AmbientCycle) botAmbCombo.getSelectedItem();
        frontSideAmb = (AmbientCycle) frontSideAmbCombo.getSelectedItem();
        backSideAmb = (AmbientCycle) backSideAmbCombo.getSelectedItem();
        nearEndAmb = (AmbientCycle) nearEndAmbCombo.getSelectedItem();
        farEndAmb = (AmbientCycle) farEndAmbCombo.getSelectedItem();
        Vector<SkidData> skidV = new Vector<SkidData>();
        int fromXsel, toXsel;
        AmbientCycle amb;
        for (int n = 0; n < maxSkids; n++) {
//      double w = Double.parseDouble(skidWidth[n].getText());
//      if (w > 0) {
//        SkidData skid = new SkidData(ch, Double.parseDouble(skidLoc[n].getText()) / 1000,
//                                     Double.parseDouble(skidWidth[n].getText()) / 1000,
//                                     (AmbientCycle) skidAmbients[n].
//                                     getSelectedItem());
            fromXsel = skidFromCombo[n].getSelectedIndex();
            toXsel = skidToCombo[n].getSelectedIndex();
            if ((fromXsel >= 1) && (toXsel >= 1)) {
                if (fromXsel >= toXsel) {
                    errMsg("ERROR in Skid #" + (n + 1) + " definition!");
                } else {
                    amb = (AmbientCycle) skidAmbients[n].getSelectedItem();
                    if (amb.name.compareToIgnoreCase(AmbientCycle.DEFAULTSURFACE) == 0) {
                        errMsg("Skid #" + (n + 1) + " area is INSULATED!");
                    }
                    SkidData skid = new SkidData(fromXsel + 1, toXsel, amb);
                    skidV.add(skid);
                }
            }
        }
        int sValid = skidV.size();
        if (sValid > 0) {
            skids = new SkidData[sValid];
            for (int n = 0; n < sValid; n++)
                skids[n] = skidV.get(n);
        } else
            skids = null;
        done = true;
    }

    OuterSurface[] getOuterSurfaces() {
        Vector<OuterSurface> outerV = new Vector<OuterSurface>();
//    double sideHTFactor = 0.5;
//    double nearEndHTFactor = 2.0;
//    double farEndHTFactor = 0.5;
        double slotHorizontalFactor = 0.6;
        double slotVerticalFactor = 0.4;
//    if (chGap == 0)
//      sideHTFactor = 0;
        int skidsN = (skids == null) ? 0 : skids.length;
        switch (ch.chargeDef.chargeType) {
            case ChargeDef.RECTANGULAR:
                outerV.add(new OuterSurface(ChargeDef.RECTANGULAR, 2, ch.getSurfaceNodes(2),
                        backSideAmb));
                outerV.add(new OuterSurface(ChargeDef.RECTANGULAR, 3, ch.getSurfaceNodes(3),
                        topAmb));
                outerV.add(new OuterSurface(ChargeDef.RECTANGULAR, 4, ch.getSurfaceNodes(4),
                        frontSideAmb));
                outerV.add(new OuterSurface(ChargeDef.RECTANGULAR, 21, ch.getSurfaceNodes(21),
                        nearEndAmb));
                outerV.add(new OuterSurface(ChargeDef.RECTANGULAR, 22, ch.getSurfaceNodes(22),
                        farEndAmb));
                // Bottom surface with skids if available
                if (skidsN > 0) {
                    // skids are already checked for validity and the array is in ascending
                    // order of position
                    int start = 1;
                    int end = 1;
                    for (int sk = 0; sk < skidsN; sk++) {
                        end = skids[sk].fromX - 1;
                        if (end > 1)
                            outerV.add(new OuterSurface(ChargeDef.RECTANGULAR, 1,
                                    ch.getSurfaceNodes(1, start, end), botAmb));
                        start = end + 1;
                        end = skids[sk].toX;
                        outerV.add(new OuterSurface(ChargeDef.RECTANGULAR, 1,
                                ch.getSurfaceNodes(1, start, end),
                                skids[sk].getAmbient(), 1.0, true));
                        start = end + 1;
                    }
                    if (end < ch.xSize - 2) {
                        outerV.add(new OuterSurface(ChargeDef.RECTANGULAR, 1,
                                ch.getSurfaceNodes(1, start, ch.xSize - 2), botAmb));
                    }
                } else // no skid
                    outerV.add(new OuterSurface(ChargeDef.RECTANGULAR, 1,
                            ch.getSurfaceNodes(1), botAmb));
                break;
            case ChargeDef.BEAMBLANK_H:
                // bottom flange edges
                if (skidsN > 0) {
                    // skids are already checked for validity and the array is in ascending
                    // order of position
                    int start = 1;
                    int end = 1;
                    for (int sk = 0; sk < skidsN; sk++) {
                        end = skids[sk].fromX - 1;
                        if (end > 1) {
                            outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 1,
                                    ch.getSurfaceNodes(1, start, end), botAmb));
                            outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 5,
                                    ch.getSurfaceNodes(5, start, end), botAmb));
                        }
                        start = end + 1;
                        end = skids[sk].toX;
                        outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 1,
                                ch.getSurfaceNodes(1, start, end), skids[sk].getAmbient(), 1.0, true));
                        outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 5,
                                ch.getSurfaceNodes(5, start, end), skids[sk].getAmbient(), 1.0, true));
                        start = end + 1;
                    }
                    if (end < (ch.xSize - 2)) {
                        end = ch.xSize - 2;
                        outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 1,
                                ch.getSurfaceNodes(1, start, end), botAmb));
                        outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 5,
                                ch.getSurfaceNodes(5, start, end), botAmb));
                    }
                } else {// no skid
                    outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 1,
                            ch.getSurfaceNodes(1), botAmb));
                    outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 5,
                            ch.getSurfaceNodes(5), botAmb));
                }
                // top flange edges
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 7,
                        ch.getSurfaceNodes(7), topAmb));
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 11, ch.getSurfaceNodes(11), topAmb));
                // outer verticals
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 6,
                        ch.getSurfaceNodes(6), backSideAmb));
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 12,
                        ch.getSurfaceNodes(12), frontSideAmb));
                // slot horizontals
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 3,
                        ch.getSurfaceNodes(3), botAmb, slotHorizontalFactor));
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 9,
                        ch.getSurfaceNodes(9), topAmb, slotHorizontalFactor));
                // slot verticals
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 2,
                        ch.getSurfaceNodes(2), botAmb, slotVerticalFactor));
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 4,
                        ch.getSurfaceNodes(4), botAmb, slotVerticalFactor));
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 8,
                        ch.getSurfaceNodes(8), topAmb, slotVerticalFactor));
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 10,
                        ch.getSurfaceNodes(10), topAmb, slotVerticalFactor));
                // near end
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 13,
                        ch.getSurfaceNodes(13), nearEndAmb));
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 14,
                        ch.getSurfaceNodes(14), nearEndAmb));
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 15,
                        ch.getSurfaceNodes(15), nearEndAmb));
                // far end
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 16,
                        ch.getSurfaceNodes(16), farEndAmb));
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 17,
                        ch.getSurfaceNodes(17), farEndAmb));
                outerV.add(new OuterSurface(ChargeDef.BEAMBLANK_H, 18,
                        ch.getSurfaceNodes(18), farEndAmb));
                break;
            case ChargeDef.BEAMBLANK_V:
                errMsg("Not Ready for Vertical Beam Blank in 'getOuterSurfaces()'");
                break;

        }
        return outerV.toArray(new OuterSurface[0]);
    }


    void errMsg(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Charge in Furnace",
                JOptionPane.ERROR_MESSAGE);
    }

    void debug(String msg) {
        System.out.println("ChargeInFurnace: " + msg);
    }
}