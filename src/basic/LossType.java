package basic;

import directFiredHeating.DFHFurnace;
import directFiredHeating.FceSubSection;
import mvUtils.display.InputControl;
import mvUtils.display.NumberTextField;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.display.FramedPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 5/7/12
 * Time: 5:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class LossType {
    //    static String[] lossBasis = {"NONE", "Fixed", "Wall Area", "Roof Area", "Wall and Roof Area", "hearth Area", "Section Length" };
    public enum LossBasis {
        NONE("Disabled"),
        FIXED("Fixed"),
        WALL("Lateral Wall Area"),   // was 'Wall Area' earlier
        CHENDWALL("Charging End Wall"),
        DISCHENDWALL("Discharging End Wall"),
        ROOF("Roof Area"),
        HEARTH("Hearth Area"),
        WALLANDROOF("Wall And Roof Area"),
        ALLAREA("All Areas"),
        LENGTH("Section Length"),
        PIECES("Production - Pieces/h");

        private final String lossName;

        LossBasis(String lossName) {
            this.lossName = lossName;
        }

        public String basisName() {
            return lossName;
        }

        @Override
        public String toString() {
            return lossName;    //To change body of overridden methods use File | Settings | File Templates.
        }

        public static LossBasis getEnum(String text) {
            if (text != null) {
              for (LossBasis b : LossBasis.values()) {
                if (text.equalsIgnoreCase(b.lossName)) {
                  return b;
                }
              }
            }
            return null;
        }
    }

    public enum TempAction {
        NONE("Not Related"),
        LINEAR("Linear to Temperature DegC"),
        POW4("DegK ^ 4");
        private final String actName;

        TempAction(String actName) {
            this.actName = actName;
        }

        public String actName() {
            return actName;
        }

        @Override
        public String toString() {
            return actName;    //To change body of overridden methods use File | Settings | File Templates.
        }

        public static TempAction getEnum(String text) {
            if (text != null) {
              for (TempAction b : TempAction.values()) {
                if (text.equalsIgnoreCase(b.actName)) {
                  return b;
                }
              }
            }
            return null;
          }
    }

    //    static Vector<LossBasis> vLossBasis = new Vector<LossBasis>();
    static boolean inited = false;
    double factor;
    LossBasis basis;
    TempAction tempAct;
    public String lossName;
    JTextField tfLossName;
    NumberTextField tfFactor;
    JComboBox <LossBasis>cbBasis;
    JComboBox <TempAction>cbTempAct;
    FramedPanel lossPanel;
    static FramedPanel header;
    InputControl controller;
    ActionListener listener;
    DFHFurnace furnace;

    public LossType(InputControl controller, DFHFurnace furnace, String lossName, double factor, LossBasis basis, TempAction tempAct, ActionListener listener) {
        initStaticData();
        this.furnace = furnace;
        this.listener = listener;
        this.controller = controller;
        this.lossName = lossName;
        this.factor = factor;
        this.basis = basis;
        this.tempAct = tempAct;
        tfLossName = new JTextField(lossName, 20);
        tfLossName.addActionListener(listener);
        tfLossName.addFocusListener((FocusListener) listener);
        tfFactor = new NumberTextField(controller, factor, 10, false, 0, 1e6, "#.###E00", "Loss Factor", false);
        tfFactor.addActionListener(listener);
        tfFactor.addFocusListener((FocusListener)listener);
        cbBasis = new JComboBox<LossBasis>(LossBasis.values());
        cbBasis.addActionListener(listener);
        cbBasis.setPreferredSize(new Dimension(250, 20));
        cbTempAct = new JComboBox<TempAction>(TempAction.values());
        cbTempAct.setPreferredSize(new Dimension(200, 20));
        setValuesToUI();
        setDisplayPanel();
    }

    public LossType(InputControl controller, DFHFurnace furnace, String lossName, ActionListener listener) {
        this(controller, furnace, lossName, 0, LossBasis.NONE, TempAction.NONE, listener);
    }

    public void reset(String lossName) {
        this.lossName = lossName;
        factor = 0;
        basis = LossBasis.NONE;
        tempAct = TempAction.NONE;
    }

    public void enableDataEntry(boolean ena) {
        tfLossName.setEditable(ena);
        tfFactor.setEditable(ena);
        cbBasis.setEnabled(ena);
        cbTempAct.setEnabled(ena);
    }

    public void changeData(String lossName, double factor, LossBasis basis, TempAction tempAct, boolean bQuiet) {
        this.lossName = lossName;
        this.factor = factor;
        this.basis = basis;
        this.tempAct = tempAct;
        setValuesToUI();
        if (!bQuiet)
            informListener();
      }

    public void changeData(String lossName, double factor, LossBasis basis, TempAction tempAct) {
        changeData(lossName, factor, basis, tempAct, false);
    }

    public boolean changeData(String xmlStr) {
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "lossName", 0);
        String ln = vp.val;
        vp =  XMLmv.getTag(xmlStr, "cbBasis", 0);
        LossBasis b;
        if (vp.val.equalsIgnoreCase("wall Area"))
            b = LossBasis.WALL;
        else
            b = LossBasis.getEnum(vp.val);
        vp =  XMLmv.getTag(xmlStr, "cbTempAct", 0);
        TempAction ta =TempAction.getEnum(vp.val);
        try {
            vp = XMLmv.getTag(xmlStr, "factor", 0);
            double f = Double.valueOf(vp.val);
            changeData(ln, f, b, ta);
        } catch (NumberFormatException e) {
            errMsg("Number format in XML data (" + xmlStr + ")");
            return false;
        }
        return true;
    }

    void setValuesToUI() {
        controller.enableNotify(false);
        tfLossName.setText(lossName);
        tfFactor.setData(factor);
        cbBasis.setSelectedItem(basis);
        cbTempAct.setSelectedItem(tempAct);
        controller.enableNotify(true);
    }

    public void takeValuesFromUI() {
        lossName = tfLossName.getText();
        factor = tfFactor.getData(); //Double.valueOf(tfFactor.getText());
//        basis = lbFromString("" +cbBasis.getSelectedItem());
        basis = (LossBasis)cbBasis.getSelectedItem();
//        tempAct = taFromString("" + cbTempAct.getSelectedItem());
        tempAct = (TempAction)cbTempAct.getSelectedItem();
    }

    public static LossBasis lossTypeFromStr(String lossBasisStr) {
        String trimmed = lossBasisStr.trim();
        return LossBasis.valueOf(trimmed);
    }

    static void initStaticData() {
        if (!inited) {
            header = new FramedPanel(new GridBagLayout());
            GridBagConstraints gbcTp = new GridBagConstraints();
            gbcTp.gridx = 0;
            gbcTp.gridy = 0;
//            gbcTp.ipadx = 100;
            header.add(new JLabel("Loss Name"), gbcTp);
            gbcTp.gridx++;
            header.add(new JLabel("Loss Basis"), gbcTp);
            gbcTp.gridx++;
            header.add(new JLabel("Temperature Basis"), gbcTp);
            gbcTp.gridx++;
            header.add(new JLabel("Factor"), gbcTp);
            gbcTp.gridy++;
            gbcTp.gridx = 0;
            gbcTp.ipadx = 200;
            header.add(new JLabel(""), gbcTp);
            gbcTp.gridx++;
            gbcTp.ipadx = 250;
            header.add(new JLabel(""), gbcTp);
            gbcTp.gridx++;
            gbcTp.ipadx = 180;
            header.add(new JLabel(""), gbcTp);
            gbcTp.gridx++;
            gbcTp.ipadx = 150;
            header.add(new JLabel(""), gbcTp);
        }
    }

    void setDisplayPanel() {
        lossPanel = new FramedPanel(new GridBagLayout());
        GridBagConstraints gbcTp = new GridBagConstraints();
        gbcTp.gridx = 0;
        gbcTp.gridy = 0;
        lossPanel.add(tfLossName, gbcTp);
        gbcTp.gridx++;
        lossPanel.add(cbBasis, gbcTp);
//        cbBasis.setSelectedItem(basis);
        gbcTp.gridx++;
        lossPanel.add(cbTempAct, gbcTp);
//        cbTempAct.setSelectedItem(tempAct);
        gbcTp.gridx++;
        lossPanel.add(tfFactor, gbcTp);

    }

    public Component getLossPanel() {
        return lossPanel;
    }

    public static Component getHeadPanel() {
        initStaticData();
        return header;
    }

    public LossBasisAndVal getLosses(FceSubSection forSub, double temp) {
        double aOrL = 1;  // area or Length value
        double tempFact = 1;
        switch (basis) {
            case FIXED:
                aOrL = 1;
                break;
            case WALL:
                aOrL = forSub.wallArea;
                break;
            case CHENDWALL:
                aOrL = forSub.chEndWallArea;
                break;
            case DISCHENDWALL:
                aOrL = forSub.dischEndWallArea;
                break;
            case ROOF:
                aOrL = forSub.roofArea;
                break;
            case HEARTH:
                aOrL = forSub.hearthArea;
                break;
            case WALLANDROOF:
                aOrL = forSub.wallArea + forSub.roofArea;
                break;
            case ALLAREA:
                aOrL = forSub.wallArea + forSub.roofArea + forSub.hearthArea; //  + forSub.dischEndWallArea + forSub.chEndWallArea;
                break;
            case LENGTH:
                aOrL = forSub.length;
                break;
            case PIECES:
                aOrL = forSub.getFurnace().productionData.piecesPerh;
                break;
            case NONE :
            default:
                aOrL = 0;
                break;
        }

        switch (tempAct) {
            case POW4:
                tempFact = (Math.pow((temp + 273), 4) - Math.pow((furnace.getAmbTemp() + 273), 4));
                break;
            case LINEAR:
                tempFact = temp;
                break;
            case NONE:
                tempFact = 1;
                break;
            default:
                tempFact = 0;
                break;
        }
        return new LossBasisAndVal(aOrL, temp, factor * aOrL * tempFact);
    }

    public boolean isValid() {
        return (lossName.length() > 0 && factor != 0 && cbBasis.getSelectedItem() != LossBasis.NONE);
    }

    @Override
    public boolean equals(Object obj) {
        boolean retVal = false;
        if (obj.getClass() == this.getClass()) {
            LossType ref = (LossType) obj;
            if ((ref.basis == basis)
                    && (ref.tempAct == tempAct)
                    && (ref.factor == factor) &&
                    (ref.lossName.equals(lossName)))
                retVal = true;
        }
        return retVal;
    }

    public String dataInXML() {
        String xmlStr = XMLmv.putTag("lossName", lossName) +
                XMLmv.putTag("cbBasis", "" + cbBasis.getSelectedItem()) +
                XMLmv.putTag("cbTempAct", "" + cbTempAct.getSelectedItem()) +
                XMLmv.putTag("factor", "" + factor);
        return xmlStr;
    }

    public void informListener() {
        tfLossName.postActionEvent();
    }

    void errMsg(String msg) {
        System.out.println("LossType: ERROR - " + msg);
    }
}
