package directFiredHeating;

import basic.ProductionData;
import display.ThreadController;
import mvUtils.display.FramedPanel;
import performance.stripFce.Performance;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 6/19/12
 * Time: 2:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class FceEvaluator implements Runnable, ThreadController{
    public static enum EvalStat {
        OK("OK"),
        TOOLOWGAS("Too Low Gas Temperature"),
        TOOHIGHGAS("Too High Gas Temperature"),
        DONTKNOW("Dont know");

        private final String errName;

        EvalStat(String errName) {
            this.errName = errName;
        }

        public boolean isOk() {
            return this == OK;
        }

        public String errName() {
            return errName;
        }

        @Override
        public String toString() {
            return errName;    //To change body of overridden methods use File | Settings | File Templates.
        }
    }

    DFHFurnace furnace;
    JPanel jpFceSummary, jpSecWiseSummary, jpTabResults, jpTrends;
    ActionListener localControl;
    boolean run = true;
    boolean paused = false;
    DFHeating control;
    JScrollPane slate;
    FramedPanel mainFp;
    int passCount = 0;
    public boolean stopped = false;
    double calculStep;
    ProductionData production;
    DFHTuningParams tuningParams;
    boolean bShowProgress = false;
    Performance baseP;

    /**
     *
     * @param control
     * @param slate
     * @param furnace
     * @param calculStep
     * @param baseP if baseP is specified, it is for performance table
     */
    public FceEvaluator(DFHeating control, JScrollPane slate, DFHFurnace furnace, double calculStep, Performance baseP) {
        this.furnace = furnace;
        this.control = control;
        this.slate = slate;
        this.calculStep = calculStep;
        this.baseP = baseP;
        bShowProgress = true;
        production = furnace.production;
        tuningParams = furnace.tuningParams;
    }

    public FceEvaluator(DFHeating control, JScrollPane slate, DFHFurnace furnace, double calculStep) {
        this(control, slate, furnace, calculStep, null);
        bShowProgress = true;
    }

    public FceEvaluator(DFHeating control, DFHFurnace furnace, double calculStep) {
        this(control, null, furnace, calculStep);
        bShowProgress = false;
    }

    public void setShowProgress(boolean bShowProgress)  {
        this.bShowProgress = bShowProgress;
    }

    LocalControl locCtrl;
    JLabel mainTitle;
    JLabel calculTitle;

    JPanel progressPanel;
    boolean init() {
        localControl = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String cmd =  e.getActionCommand();
                if (cmd.equals("Abort"))
                    abortCalculation();
            }
        };
        mainFp = new FramedPanel(new BorderLayout());
        locCtrl = new LocalControl();
        JPanel titleP = new JPanel();
        titleP.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainTitle = new JLabel("Main Title") ;
        titleP.add(mainTitle, gbc);
        gbc.gridy++;
        calculTitle = new JLabel("TOP SECTIONS ...");
        titleP.add(calculTitle, gbc);
        mainFp.add(titleP, BorderLayout.NORTH);
        mainFp.add(locCtrl, BorderLayout.SOUTH);
        progressPanel = progressPanel();
        mainFp.add(progressPanel, BorderLayout.CENTER);
        return furnace.getReadyToCalcul(calculStep);
    }

    JButton pbAbort = new JButton("Abort");

    void pauseCalculation() {
        paused = true;
    }

    boolean checkPaused() {
        if (paused) {
            control.pausingCalculation(paused);
        }
        return paused;
    }

    public void abortIt() {
        abortCalculation();
    }

    void abortCalculation() {
        run= false;
        control.abortingCalculation();
    }

    void resumeCalculation() {
        paused = false;
        control.pausingCalculation(paused);
    }

    public boolean isRunOn() {
        return run;
    }

    public boolean isPauseOn() {
        return paused;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isAborted() {
        return !run;
    }

    JLabel showCount;
    JLabel status;
//    GraphPanel proGraph;

    public void setProgressGraph(String title1, String title2, JPanel panel) {
//        GridBagConstraints gbc = new GridBagConstraints();
//        gbc.gridx = 0;
//        gbc.gridy = 0;
        progressPanel.add(panel,  BorderLayout.CENTER);
        setMainTitle(title1);
        setCalculTitle(title2);
    }

    public void setCalculTitle(String title) {
        calculTitle.setText(title);
    }

    public void setMainTitle(String title) {
        mainTitle.setText(title);
    }

    JPanel progressPanel() {
        JPanel fp = new JPanel(new BorderLayout());
//        furnace.getReadyToCalcul(calculStep);
        status = new JLabel("Start");
        fp.add(status, BorderLayout.SOUTH);
        return fp;
    }

    public JPanel getJpFceSummary() {
        return jpFceSummary;
    }

    public JPanel getJpSecWiseSummary() {
        return jpSecWiseSummary;
    }

    public JPanel getJpTabResults() {
        return jpTabResults;
    }

    public JPanel getJpTrends() {
        return jpTrends;
    }

    public void showStatus(String msg) {
        status.setText(msg);
    }
    boolean bFirstRun = true;

    public void updateGraph() {
/*
        if (bFirstRun) {
            bFirstRun = false;
            try {
                Thread.sleep(500);
                proGraph.showGraph();
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        else
            proGraph.showGraph();
*/
     }
    void evaluate() {
        if (baseP != null)
            baseP.createPerfTable(this);
        else
            furnace.evaluate(this);
        stopped = true;
    }

    void restart() {
        run = true;
        // .....
    }

    public boolean done = false;

    public void run() {
        if (init()) {
            if (bShowProgress)
                showProgress();
            evaluate();
        }
        else
            control.abortingCalculation();
        done = true;
    }

    public void showProgress() {
        slate.setViewportView(mainFp);
    }

    public FramedPanel getProgressPanel() {    // TODO to be removed (not used ?)
        return mainFp;
    }

      class LocalControl extends JPanel implements Runnable {
        LocalControl() {
             super();
             pbAbort.addActionListener(localControl);
             add(pbAbort);
           }

        public void run() {
             //To change body of implemented methods use File | Settings | File Templates.
        }
    }

}
