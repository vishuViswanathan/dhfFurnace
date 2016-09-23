package FceElements.heatExchanger;

import basic.Fluid;
import basic.FuelFiring;
import mvUtils.display.*;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLmv;
import mvUtils.math.SPECIAL;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 1/3/14
 * Time: 10:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class HeatExchProps {
    public double heatingFlowBase = 100;
    public double heatedFlowBase = 100;
    public double heatingTinBase = 100, heatingToutBase = 50;
    public double heatedTinBase = 30, heatedToutBase = 90;
    public double heatExchBase = 100;
    double deltaPHeatingBase = 10;
    double deltaPHeatedBase = 200;
    double flowRatioBase;
    double deltaTRatioBase;
    boolean bCounter = true;
    double fFBase; // flow factor
    double hTaBase;
    public boolean canPerform = false;
    public int calCount;


    public HeatExchProps() {
        canPerform = false;
    }

    public HeatExchProps(double heatingFlowBase, double heatedFlowBase, double heatingTinBase, double heatingToutBase,
                         double heatedTinBase, double heatedToutBase, double heatExch, boolean bCounter) {
        this();
        this.heatingFlowBase = heatingFlowBase;
        this.heatedFlowBase = heatedFlowBase;
        this.heatingTinBase = heatingTinBase;
        this.heatingToutBase = heatingToutBase;
        this.heatedTinBase = heatedTinBase;
        this.heatedToutBase = heatedToutBase;
        this.heatExchBase = heatExch;
        this.bCounter = bCounter;
        checkCanPerform();
    }

    HeatExchProps createCopy() {
        return new HeatExchProps(heatingFlowBase, heatedFlowBase, heatingTinBase, heatingToutBase,
            heatedTinBase, heatedToutBase, heatExchBase, bCounter);
    }

    boolean takeFrom(HeatExchProps from)  {
        if (from.checkCanPerform()) {
            this.heatingFlowBase = from.heatingFlowBase;
            this.heatedFlowBase = from.heatedFlowBase;
            this.heatingTinBase = from.heatingTinBase;
            this.heatingToutBase = from.heatingToutBase;
            this.heatedTinBase = from.heatedTinBase;
            this.heatedToutBase = from.heatedToutBase;
            this.heatExchBase = from.heatExchBase;
            this.deltaPHeatingBase = from.deltaPHeatingBase;
            this.deltaPHeatedBase = from.deltaPHeatedBase;
            this.bCounter = from.bCounter;
            this.canPerform = from.canPerform;
            return true;
        }
        else
            return false;
    }

    void setDeltaPs(double deltaPHeating, double deltaPheated) {
        deltaPHeatingBase =  deltaPHeating;
        deltaPHeatedBase = deltaPheated;
    }

    boolean checkCanPerform() {
        boolean dataOK = true;
        canPerform = false;
        if (heatingTinBase > heatingToutBase && heatedToutBase > heatedTinBase && heatingTinBase > heatedToutBase && heatedTinBase < heatingToutBase &&
                heatedFlowBase > 0 && heatingFlowBase > 0 && heatExchBase > 0) {
            if (!bCounter) {
                dataOK = (heatedToutBase < heatingToutBase);
            }
            if (dataOK) {
                try {
                    double lmtd = lmtd(heatingTinBase, heatingToutBase, heatedTinBase, heatedToutBase);
                    hTaBase = heatExchBase / lmtd;
                    fFBase = Math.pow(heatingFlowBase, 0.6) + Math.pow(heatedFlowBase, 0.8);
                    flowRatioBase = heatingFlowBase / heatedFlowBase;
                    deltaTRatioBase = (heatingTinBase - heatingToutBase) / (heatedToutBase - heatedTinBase);
                    canPerform = true;
                } catch (Exception e) {
                    canPerform = false;
                }
            }
        }
        return canPerform;
    }

    // assumes that the heat-exchange/deltaT is constant based on the Base values for both fluids
    public HeatExchProps getPerformance(double heatingFlow, double heatingTempIn,
                                        double heatedFlow, double heatedTempIn)  {
        return getPerformance(null, heatingFlow, heatingTempIn, null, heatedFlow, heatedTempIn);
    }


    public HeatExchProps getPerformance(Fluid heatingFluid, double heatingFlow, double heatingTempIn,
                                        Fluid heatedFluid, double heatedFlow, double heatedTempIn)  {
        if (bCounter)
            return getPerformanceCounterFlow(heatingFluid, heatingFlow, heatingTempIn,
                heatedFluid, heatedFlow, heatedTempIn);
        else
            return getPerformanceParallelFlow(heatingFluid, heatingFlow, heatingTempIn,
                    heatedFluid, heatedFlow, heatedTempIn);

    }
    public HeatExchProps getPerformanceOLD(Fluid heatingFluid, double heatingFlow, double heatingTempIn,
                                        Fluid heatedFluid, double heatedFlow, double heatedTempIn)  {
        double errAllowed = 0.1; //
        double fF = Math.pow(heatingFlow, 0.6) + Math.pow(heatedFlow, 0.8);
        double ratio = fF / fFBase;
        double hTAavailable = hTaBase * ratio;
        double heatedToutAssume = (heatingTempIn + heatedTempIn) / 2;
        double deltaTHeatedAssume = heatedToutAssume - heatedTempIn;
        double deltaTHeatedRevised;
        double flowRatio = heatingFlow / heatedFlow;
        double deltaTRatio = deltaTRatioBase * (flowRatioBase / flowRatio);
        double cpHeated = heatExchBase / (heatedFlowBase * (heatedToutBase - heatedTinBase));
        double expFactor;
        if (bCounter)
            expFactor = Math.exp((deltaTRatio - 1) / (cpHeated * heatedFlow / hTAavailable));
        else
            expFactor = Math.exp((deltaTRatio + 1) / (cpHeated * heatedFlow / hTAavailable));
        double tempDiffIn = heatingTempIn - heatedTempIn;
        calCount = 0;
        int maxCount = 1000;
        HeatExchProps retVal = null;
        if (tempDiffIn > 0) {
            if (canPerform) {     // at the moment the fluid properties not considered ie. heatingFluid == null
                boolean bDone = false;
                double diff;
                double deltaTheating = 0;
                double heatExch;
                do {
                    deltaTheating = deltaTHeatedAssume * deltaTRatio;
                    if (bCounter)
                        deltaTHeatedRevised = tempDiffIn - (tempDiffIn - deltaTheating)* expFactor;
                    else
                        deltaTHeatedRevised = tempDiffIn - deltaTheating - tempDiffIn / expFactor;

                    diff = deltaTHeatedRevised - deltaTHeatedAssume;
                    if (Math.abs(diff) < errAllowed)
                        bDone = true;
                    else
                        deltaTHeatedAssume += diff * 0.2 ; // = (deltaTHeatedAssume + deltaTHeatedRevised) / 2;
                } while(!bDone && ++calCount < maxCount);
                if (bDone) {
                    heatExch = cpHeated * heatedFlow * deltaTHeatedAssume;
                    retVal =  new HeatExchProps(heatingFlow, heatedFlow, heatingTempIn,
                            heatingTempIn - deltaTheating, heatedTempIn, heatedTempIn + deltaTHeatedAssume, heatExch, bCounter);
                }
            }
        }
        return retVal;
    }

    public HeatExchProps getPerformanceCounterFlow(Fluid heatingFluid, double heatingFlow, double heatingTempIn,
                                        Fluid heatedFluid, double heatedFlow, double heatedTempIn)  {
        // TODO fluid properties are not used
        double errAllowed = 0.1; //
        double fF = Math.pow(heatingFlow, 0.6) + Math.pow(heatedFlow, 0.8);
        double ratio = fF / fFBase;
        double hTAvailable = hTaBase * ratio;
        double cpHeated = heatExchBase / (heatedFlowBase * (heatedToutBase - heatedTinBase));
        double cpHeating = heatExchBase / (heatingFlowBase * (heatingTinBase - heatingToutBase));
        calCount = 0;
        int maxCount = 1000;
        HeatExchProps retVal = null;
        double heatedToutAssume = (heatingTempIn + heatedTempIn) / 2;
        double assumedLast = 0;
        double assumedNew = 0;
        boolean bNotFirstTime = false;
        double heatedToutRevised;
        double correctionFactor = 0.5;
        if (canPerform) {
            boolean bDone = false;
            double diff;
            double heatExch;
            double heatingTOut, lmtd;
            do {
                heatExch = cpHeated * heatedFlow * (heatedToutAssume - heatedTempIn);
                heatingTOut = heatingTempIn - heatExch / (cpHeating * heatingFlow);
                lmtd = SPECIAL.lmtd((heatingTempIn - heatedToutAssume), (heatingTOut - heatedTempIn));
                heatedToutRevised = heatedTempIn +
                        hTAvailable * lmtd / (heatedFlow * cpHeated);
                diff = heatedToutRevised - heatedToutAssume;
                if (Math.abs(diff) < errAllowed) {
                    bDone = true;
                }
                else {
                    assumedNew = heatedToutAssume + diff * correctionFactor;
                    assumedNew = Math.min(assumedNew, (heatingTempIn - 5));
                    if (bNotFirstTime) {
                        if (assumedNew == assumedLast) {
                            correctionFactor /= 2;
                            assumedNew = heatedToutAssume + diff * correctionFactor;
                        }
                    }
                    else
                        bNotFirstTime = true;
                    assumedLast = heatedToutAssume;
                    heatedToutAssume = assumedNew;
                }
            } while(!bDone && ++calCount < maxCount);
            if (bDone) {
                retVal =  new HeatExchProps(heatingFlow, heatedFlow, heatingTempIn,
                        heatingTOut, heatedTempIn, heatedToutAssume, heatExch, bCounter);
                double ratioMeanTHeating = (heatingTempIn + heatingTOut + 2 * 273) /
                        (heatingTinBase + heatingToutBase + 2 * 273);
                double ratioMeanTHeated = (heatedTempIn + heatedToutAssume + 2 * 273) /
                        (heatedTinBase + heatedToutBase + 2 * 273);
                double ratioHeatingFlow = heatingFlow / heatingFlowBase;
                double ratioHeatedFlow = heatedFlow/ heatedFlowBase;
                double deltaPHeating = deltaPHeatingBase * Math.pow(ratioHeatingFlow, 2) * ratioMeanTHeating;
                double deltaPHeated = deltaPHeatedBase * Math.pow(ratioHeatedFlow, 2) * ratioMeanTHeated;
                retVal.setDeltaPs(deltaPHeating, deltaPHeated);
            }
        }
        return retVal;
    }

    public HeatExchProps getPerformanceParallelFlow(Fluid heatingFluid, double heatingFlow, double heatingTempIn,
                                                   Fluid heatedFluid, double heatedFlow, double heatedTempIn)  {
        // TODO fluid properties are not used
         return null;
    }

    double lmtd(double tf1In, double tF1Out, double tF2In, double tF2Out) {
        double deltaTa, deltaTb;
        if (bCounter)   {
            deltaTa = tf1In - tF2Out;
            deltaTb = tF1Out - tF2In;
        }
        else {
            deltaTa = tf1In - tF2In;
            deltaTb = tF1Out - tF2Out;
        }
        return SPECIAL.lmtd(deltaTa, deltaTb);
    }

    public boolean takeDataFromXML(String xmlStr, FuelFiring fuelFiring, double totalFuel) {
        boolean retVal = false;
        try {
            ValAndPos vp;
            vp = XMLmv.getTag(xmlStr, "bCounter", 0);
            bCounter = (vp.val.equals("1"));
            vp = XMLmv.getTag(xmlStr, "heatingTinBase", 0);
            heatingTinBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatingToutBase", 0);
            heatingToutBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatedTinBase", 0);
            heatedTinBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatedToutBase", 0);
            heatedToutBase = Double.valueOf(vp.val);

            vp = XMLmv.getTag(xmlStr, "deltaPHeatingBase", 0);
            if (vp.val.length() > 0)
                deltaPHeatingBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "deltaPHeatedBase", 0);
            if (vp.val.length() > 0)
                deltaPHeatedBase = Double.valueOf(vp.val);

            // @TODO to calculate the heat exchange parameters from Fuel and fuel flow (any dilution ?)
            // heatingFlowBase, heatedFlowBase, heatExchBase
            heatingFlowBase = totalFuel * fuelFiring.unitFlueFlow();
            heatedFlowBase = totalFuel * fuelFiring.unitAirFlow();
            heatExchBase = totalFuel * fuelFiring.heatForAirPerUFuel(heatedTinBase, heatedToutBase);
        } catch (NumberFormatException e) {
            retVal = false;
        }

        return retVal;
    }

    public boolean takeDataFromXML(String xmlStr) {
        boolean retVal = false;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "bCounter", 0);
        bCounter = (vp.val.equals("1"));
        try {
            vp = XMLmv.getTag(xmlStr, "heatingFlowBase", 0);
            heatingFlowBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatedFlowBase", 0);
            heatedFlowBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatingTinBase", 0);
            heatingTinBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatingToutBase", 0);
            heatingToutBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatedTinBase", 0);
            heatedTinBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "heatedToutBase", 0);
            heatedToutBase = Double.valueOf(vp.val);

            vp = XMLmv.getTag(xmlStr, "deltaPHeatingBase", 0);
            if (vp.val.length() > 0)
                deltaPHeatingBase = Double.valueOf(vp.val);
            vp = XMLmv.getTag(xmlStr, "deltaPHeatedBase", 0);
            if (vp.val.length() > 0)
                deltaPHeatedBase = Double.valueOf(vp.val);

            vp = XMLmv.getTag(xmlStr, "heatExchBase", 0);
            heatExchBase = Double.valueOf(vp.val);
            retVal = checkCanPerform();
            if (!retVal)
                SimpleDialog.showError("Recuperator Data", "The Recuperator Data is not Coherent!");
        } catch (NumberFormatException e) {
            SimpleDialog.showError("Recuperator Data", "Facing some problem in reading The Recuperator Data!");
        }
        return retVal;
    }

    public String dataInXML() {
        String xmlStr = XMLmv.putTag("bCounter", ((bCounter) ? "1" : "0"));
        xmlStr += XMLmv.putTag("heatingFlowBase", heatingFlowBase);
        xmlStr += XMLmv.putTag("heatedFlowBase", heatedFlowBase);
        xmlStr += XMLmv.putTag("heatingTinBase", heatingTinBase);
        xmlStr += XMLmv.putTag("heatingToutBase", heatingToutBase);
        xmlStr += XMLmv.putTag("heatedTinBase", heatedTinBase);
        xmlStr += XMLmv.putTag("heatedToutBase", heatedToutBase);
        xmlStr += XMLmv.putTag("deltaPHeatingBase", deltaPHeatingBase);
        xmlStr += XMLmv.putTag("deltaPHeatedBase", deltaPHeatedBase);
        xmlStr += XMLmv.putTag("heatExchBase", heatExchBase);
        xmlStr += XMLmv.putTag("fFBase", fFBase);
        xmlStr += XMLmv.putTag("hTaBase", hTaBase);
        return xmlStr;
    }

    public void getPerformance(InputControl inpC, String title, String nameHeating, String nameHeated) {
        JDialog dlg = new PerformanceDlg(inpC, nameHeating, nameHeated);
        dlg.setLocationRelativeTo(null);
        dlg.setTitle(title);
        dlg.setVisible(true);
    }

    class PerformanceDlg extends JDialog {
        InputControl inpC;
        String nameHeating = "Heating Fluid";
        String nameHeated = "Heated Fluid";
        MultiPairColPanel rowHeadP;
        MultiPairColPanel designP;
        MultiPairColPanel operationP;
        double heatingFlow;
        NumberTextField ntHeatingFlow;
        double heatingTin;
        NumberTextField ntHeatingTin;
        double heatingTout;
        NumberTextField ntHeatingTout;
        double deltaPHeating;
        NumberTextField ntDeltaPHeating;

        double heatedFlow;
        NumberTextField ntHeatedFlow;
        double heatedTin;
        NumberTextField ntHeatedTin;
        double heatedTout;
        NumberTextField ntHeatedTout;
        double deltaPHeated;
        NumberTextField ntDeltaPHeated;

        double heatExch;
        NumberTextField ntHeatExch;

        PerformanceDlg(InputControl inpC, String nameHeating, String nameHeated) {
            this.nameHeating = nameHeating;
            this.nameHeated = nameHeated;
            this.inpC = inpC;
            setModal(true);
            init();
        }

        void init() {
            JPanel outerP = new JPanel(new BorderLayout());
            rowHeadP = new MultiPairColPanel("Description", 200, 6);
            designP = new MultiPairColPanel("Design");
            operationP = new MultiPairColPanel("Operation");

            Component operation;
            String fmt = "#,##0";
            String pFmt = "#,###.00";
            DataListener dl = new DataListener();
            // to start with, for operation default values are same as design
            String nameStr = nameHeating + " Flow (Nm3/h)";
            operation = ntHeatingFlow = new NumberTextField(inpC, heatingFlowBase, 6, false, 100, 10000000, fmt, nameStr);
            addOneLine(nameStr, new NumberLabel(heatingFlowBase, 100, fmt), operation);
            ntHeatingFlow.addActionAndFocusListener(dl);

            nameStr = nameHeating + " Temperature IN (C)";
            operation = ntHeatingTin = new NumberTextField(inpC, heatingTinBase, 6, false, 10, 2000, fmt, nameStr);
            addOneLine(nameStr, new NumberLabel(heatingTinBase, 100, fmt), operation);
            ntHeatingTin.addActionAndFocusListener(dl);

            nameStr = nameHeating + " Temperature OUT (C)";
            operation = ntHeatingTout = new NumberTextField(inpC, heatingToutBase, 6, false, 10, 2000, fmt, nameStr);
            operation.setEnabled(false);
            addOneLine(nameStr, new NumberLabel(heatingToutBase, 100, fmt), operation);

            nameStr = nameHeating + " DeltaP (mmWC)";
            operation = ntDeltaPHeating = new NumberTextField(inpC, deltaPHeatingBase, 6, false, 0, 2000, pFmt, nameStr);
            operation.setEnabled(false);
            addOneLine(nameStr, new NumberLabel(deltaPHeatingBase, 100, fmt), operation);

            addBlankLine();

            nameStr = nameHeated + " Flow (Nm3/h)";
            operation = ntHeatedFlow = new NumberTextField(inpC, heatedFlowBase, 6, false, 100, 10000000, fmt, nameStr);
            addOneLine(nameStr, new NumberLabel(heatedFlowBase, 100, fmt), operation);
            ntHeatedFlow.addActionAndFocusListener(dl);

            nameStr = nameHeated + " Temperature IN (C)";
            operation = ntHeatedTin = new NumberTextField(inpC, heatedTinBase, 6, false, 10, 2000, fmt, nameStr);
            addOneLine(nameStr, new NumberLabel(heatedTinBase, 100, fmt), operation);
            ntHeatedTin.addActionAndFocusListener(dl);

            nameStr = nameHeated + " Temperature OUT (C)";
            operation = ntHeatedTout = new NumberTextField(inpC, heatedToutBase, 6, false, 10, 2000, fmt, nameStr);
            operation.setEnabled(false);
            addOneLine(nameStr, new NumberLabel(heatedToutBase, 100, fmt), operation);

            nameStr = nameHeated + " DeltaP (mmWC)";
            operation = ntDeltaPHeated = new NumberTextField(inpC, deltaPHeatedBase, 6, false, 0, 2000, pFmt, nameStr);
            operation.setEnabled(false);
            addOneLine(nameStr, new NumberLabel(deltaPHeatedBase, 100, fmt), operation);

            addBlankLine();

            nameStr = "Net Heat Exchange (kcal/h)";
            operation = ntHeatExch = new NumberTextField(inpC, heatExchBase, 6, false, 100, 100000000, fmt, nameStr);
            operation.setEnabled(false);
            addOneLine(nameStr, new NumberLabel(heatExchBase, 100, fmt), operation);

            outerP.add(rowHeadP, BorderLayout.WEST);
            outerP.add(designP, BorderLayout.CENTER);
            outerP.add(operationP, BorderLayout.EAST);

            JButton button = new JButton("Evaluate");

            button.addActionListener(e-> {evaluate();});
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(button);
            outerP.add(buttonPanel, BorderLayout.SOUTH);
            add(outerP);
            pack();
            showResults(false);
        }

        class DataListener extends ActionAndFocusListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                showResults(false);
            }

            @Override
            public void focusGained(FocusEvent e) {
                showResults(false);
            }

            @Override
            public void focusLost(FocusEvent e) {
                showResults(false);
            }
        }

        void showResults(boolean show) {
            Color bg = (show) ? Color.WHITE : Color.GRAY;
            ntHeatedTout.setBackground(bg);
            ntDeltaPHeating.setBackground(bg);
            ntHeatingTout.setBackground(bg);
            ntDeltaPHeated.setBackground(bg);
            ntHeatExch.setBackground(bg);
        }

        void evaluate() {
            if (!(ntHeatingFlow.inError || ntHeatedTin.inError || ntHeatedFlow.inError || ntHeatedTin.inError)) {
                heatingFlow = ntHeatingFlow.getData();
                heatingTin = ntHeatingTin.getData();
                heatedFlow = ntHeatedFlow.getData();
                heatedTin = ntHeatedTin.getData();
                HeatExchProps perf = getPerformance(heatingFlow, heatingTin, heatedFlow, heatedTin);
                if (perf != null) {
                    showResults(true);
                    ntHeatingTout.setData(perf.heatingToutBase);
                    ntHeatedTout.setData(perf.heatedToutBase);
                    ntHeatExch.setData(perf.heatExchBase);
                    ntDeltaPHeating.setData(perf.deltaPHeatingBase);
                    ntDeltaPHeated.setData(perf.deltaPHeatedBase);
                }
                else
                    SimpleDialog.showError("Recuperator Performance", "Unable to evaluate Performance");
            }
            else
                SimpleDialog.showError("Recuperator Performance", "Some data is/or out of range");
        }

        void addOneLine(String name, Component design, Component operation) {
            rowHeadP.addItem(name);
            designP.addItem(design);
            operationP.addItem(operation);
        }

        void addBlankLine() {
            rowHeadP.addBlank();
            designP.addBlank();
            operationP.addBlank();
        }
    }

    public EditResponse.Response defineHeatExchanger(InputControl inpC, String title, String nameHeating, String nameHeated) {
        DataDialog dlg = new DataDialog(true, inpC, nameHeating, nameHeated);
        dlg.setLocationRelativeTo(null);
        dlg.setTitle(title);
        dlg.setVisible(true);
        return dlg.response;
    }

    class DataDialog extends JDialog implements DataHandler{
        String nameHeating = "Heating Fulid";
        String nameHeated = "Heated Fluid";
        JPanel detailsP;
        InputControl inpC;
        DataListEditorPanel editorPanel;
        boolean editable = false;
        EditResponse.Response response;
        HeatExchProps original;

        NumberTextField ntHeatingFlowBase;
        NumberTextField ntHeatedFlowBase;
        NumberTextField ntHeatingTinBase;
        NumberTextField ntHeatingToutBase;
        NumberTextField ntHeatedTinBase;
        NumberTextField ntHeatedToutBase;
        NumberTextField ntHeatExchBase;
        NumberTextField ntDeltapHeatingBase;
        NumberTextField ntDeltaPHeatedBase;
        JComboBox<String> cbFlowMode;
        Vector<NumberTextField> dataFieldList;

        DataDialog(boolean editable, InputControl inpC, String nameHeating, String nameHeated) {
            this.nameHeating = nameHeating;
            this.nameHeated = nameHeated;
            this.editable = editable;
            this.inpC = inpC;
            setModal(true);
            original = createCopy();
            init();
        }

        void init() {
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    response = EditResponse.Response.EXIT;
                    super.windowClosing(e);
                }
            });
            JPanel outerP = new JPanel(new BorderLayout());
            detailsP = getEditPanel(this, true, !checkCanPerform());
            outerP.add(detailsP, BorderLayout.CENTER);
            add(outerP);
            pack();
        }

        DataListEditorPanel getEditPanel(DataHandler dataHandler,
                                                boolean editable, boolean startEditable) {
            dataFieldList = new Vector<NumberTextField>();
            NumberTextField ntf;
            ntf = ntHeatingFlowBase = new NumberTextField(inpC, heatingFlowBase, 6, false, 100, 1000000,
                    "#,##0", nameHeating + " Flow (Nm3/h)");
            dataFieldList.add(ntf);
            ntf = ntHeatingTinBase = new NumberTextField(inpC, heatingTinBase, 6, false, 10, 2000,
                    "#,##0", nameHeating + " Temperature IN (C)");
            dataFieldList.add(ntf);
            ntf = ntHeatingToutBase = new NumberTextField(inpC, heatingToutBase, 6, false, 10, 2000,
                    "#,##0", nameHeating + " Temperature OUT (C)");
            dataFieldList.add(ntf);
            ntf = ntDeltapHeatingBase = new NumberTextField(inpC, deltaPHeatingBase, 6, false, 0, 2000,
                    "#,##0", nameHeating + " DeltaP (mmWC)");
            dataFieldList.add(ntf);
            ntf = ntHeatedFlowBase = new NumberTextField(inpC, heatedFlowBase, 6, false, 100, 1000000,
                    "#,##0", nameHeated + " Flow (Nm3/h)");
            dataFieldList.add(ntf);
            ntf = ntHeatedTinBase = new NumberTextField(inpC, heatedTinBase, 6, false, 10, 2000,
                    "#,##0", nameHeated + " Temperature IN (C)");
            dataFieldList.add(ntf);
            ntf = ntHeatedToutBase = new NumberTextField(inpC, heatedToutBase, 6, false, 10, 2000,
                    "#,##0", nameHeated + " Temperature OUT (C)");
            dataFieldList.add(ntf);
            ntf = ntDeltaPHeatedBase = new NumberTextField(inpC, deltaPHeatedBase, 6, false, 0, 2000,
                    "#,##0", nameHeated + " DeltaP (mmWC)");
            dataFieldList.add(ntf);
            ntf = ntHeatExchBase = new NumberTextField(inpC, heatExchBase, 6, false, 10, 1e8,
                    "#,##0", "Net Heat Exchange (kcal/h)");
            dataFieldList.add(ntf);
            String[] flowStr =  {"Counter Flow", "Parallel Flow"};
            cbFlowMode = new JComboBox<String>(flowStr);
            cbFlowMode.setName("Heat Exchange Mode");
            cbFlowMode.setSelectedIndex((bCounter) ? 0 : 1);
            cbFlowMode.setEnabled(false);
            DataListEditorPanel editorPanel = new DataListEditorPanel("Heat Exchanger Data", dataHandler, editable, editable);
            // if editable, it is also deletable
            editorPanel.addItemPair(ntHeatingFlowBase);
            editorPanel.addItemPair(ntHeatingTinBase);
            editorPanel.addItemPair(ntHeatingToutBase);
            editorPanel.addItemPair(ntDeltapHeatingBase);
            editorPanel.addBlank();
            editorPanel.addItemPair(ntHeatedFlowBase);
            editorPanel.addItemPair(ntHeatedTinBase);
            editorPanel.addItemPair(ntHeatedToutBase);
            editorPanel.addItemPair(ntDeltaPHeatedBase);
            editorPanel.addBlank();
            editorPanel.addItemPair(ntHeatExchBase);
            editorPanel.addBlank();
            editorPanel.addItemPair(cbFlowMode);
            editorPanel.setVisible(true, startEditable);
            return editorPanel;
        }

        boolean noteFromUI() {
            boolean retVal = false;
            if (allDataFieldsLegal()) {
                heatingFlowBase =  ntHeatingFlowBase.getData();
                heatedFlowBase = ntHeatedFlowBase.getData();
                heatingTinBase = ntHeatingTinBase.getData();
                heatingToutBase = ntHeatingToutBase.getData();
                heatedTinBase =  ntHeatedTinBase.getData();
                heatedToutBase =  ntHeatedToutBase.getData();
                deltaPHeatingBase = ntDeltapHeatingBase.getData();
                deltaPHeatedBase = ntDeltaPHeatedBase.getData();
                heatExchBase =  ntHeatExchBase.getData();
                bCounter = (cbFlowMode.getSelectedIndex() == 0);
                if (checkCanPerform())
                    retVal = true;
                else
                    SimpleDialog.showError("Heat Exchanger Data", "The Data is improper!");
            }
            else
                SimpleDialog.showError("Heat Exchanger Data", "Some data is/are out of range!");
            return retVal;
        }

        EditResponse.Response getResponse() {
            return response;
        }

        public ErrorStatAndMsg checkData() {
            ErrorStatAndMsg response = new ErrorStatAndMsg();
            if (!noteFromUI())
                response.addErrorMsg("Some Error");
            return response;
        }

        public boolean saveData() {
            boolean done = false;
            if (noteFromUI()) {
                done = true;
                response = EditResponse.Response.SAVE;
                setVisible(false);
            }
            return done;
        }

        void populateUI() {
            ntHeatingFlowBase.setData(heatingFlowBase);
            ntHeatedFlowBase.setData(heatedFlowBase);
            ntHeatingTinBase.setData(heatingTinBase);
            ntHeatingToutBase.setData(heatingToutBase);
            ntHeatedTinBase.setData(heatedTinBase);
            ntHeatedToutBase.setData(heatedToutBase);
            ntHeatExchBase.setData(heatExchBase);
            ntDeltapHeatingBase.setData(deltaPHeatingBase);
            ntDeltaPHeatedBase.setData(deltaPHeatedBase);
            cbFlowMode.setSelectedIndex((bCounter) ? 0:1);

        }

        boolean allDataFieldsLegal() {
            boolean retVal = true;
            for (NumberTextField ntf: dataFieldList)
                if (ntf.inError) {
                    retVal = false;
                    break;
                }
            return retVal;
        }

        public void deleteData() {
            if (SimpleDialog.decide(this, "Deleting heat Exchanger", "Confirm DELETE") == JOptionPane.YES_OPTION) {
                response = EditResponse.Response.DELETE;
                setVisible(false);
            }
        }

        public void resetData() {
            takeFrom(original);
            populateUI();
            response = EditResponse.Response.RESET;
        }

        public void cancel() {
            response = EditResponse.Response.EXIT;
            setVisible(false);
        }
    }

    public String toString() {
        return "heatingFlow = " + heatingFlowBase +
                "\nheatedFlow = " + heatedFlowBase +
                "\nheatingTin = " + heatingTinBase +
                "\nheatingTout = " + heatingToutBase +
                "\nheatedTin = " + heatedTinBase +
                "\nheatedTout = " + heatedToutBase +
                "\nheatExch = " + heatExchBase;
    }

    public static void main(String[] args) {
        final HeatExchProps recu = new HeatExchProps(26000, 20000, 531, 366.90, 30, 270, 1536000, true);
        HeatExchProps perf = recu.getPerformance(13000, 300, 10000, 30);
        System.out.println("" + perf);
        System.out.println("calculCount " + recu.calCount);
    }
}
