package level2.simulator;

import level2.common.L2ParamGroup;
import level2.common.Tag;
import level2.common.TagCreationException;
import mvUtils.display.MultiColDataPanel;
import mvUtils.display.MultiPairColPanel;

import javax.swing.*;
import java.awt.*;

/**
 * User: M Viswanathan
 * Date: 29-Mar-17
 * Time: 3:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoadTester extends OpcSimulator {
    JButton startLoadTest = new JButton("Start Load Test");
    JButton stopLoadTest = new JButton("Stop Load Test");
    JLabel testStatus = new JLabel("TESTING is OFF");
    boolean testerOn = false;

    public LoadTester(String urlID) {
        super(urlID);
    }

    protected void addLoadTestPanel(JPanel mainP) {
        MultiPairColPanel lp = new MultiPairColPanel("");
        startLoadTest.addActionListener(r -> {
            updateTestStatus(true);
        });
        stopLoadTest.addActionListener(r -> {
            updateTestStatus(false);
        });
        lp.addItemPair(stopLoadTest, startLoadTest);
        lp.addItem(testStatus);
        mainP.add(lp, BorderLayout.NORTH);
    }

    void startLoadTest() {
        connectToTags();
        Thread t = new Thread(new RunLoadTest());
        t.start();
    }

    void updateTestStatus(boolean stat) {
        testerOn = stat;
        if (testerOn)
            testStatus.setText("TESTING is  ON");
        else
            testStatus.setText("TESTING is OFF");
    }

    Tag nextStripReady;
    Tag fieldDataReady;

    void connectToTags() {
        L2ParamGroup nextStripG = new L2ParamGroup(this, "Strip");
        nextStripReady = new Tag(L2ParamGroup.Parameter.Next, Tag.TagName.Ready, false, false);
        //rw = false  connects to Process else to level2
        try {
            nextStripG.addOneParameter(L2ParamGroup.Parameter.Next, nextStripReady);
        } catch (TagCreationException e) {
            e.printStackTrace();
        }
    }

    long nextStripGap = 10000; // 10s
    long fieldDataGap = 30000; // 30s

    class RunLoadTest implements Runnable {
        public void run() {
            while (true) {
                try {
                    if (testerOn) {
//                        debug("setting Ready ON");
                        nextStripReady.setValue(true);
                        Thread.sleep(2000);
//                        debug("setting Ready OFF");
                        nextStripReady.setValue(false);
                    }
                    Thread.sleep(nextStripGap);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void debug(String msg) {
        System.out.print(msg + "; ");
    }

    public static void main(String[] args) {
        LoadTester lt;
        if (args.length > 0) {
            lt = new LoadTester(args[0]);
        }
        else
            lt = new LoadTester("opc.tcp://127.0.0.1:49320");
        lt.proceed();
        lt.startLoadTest();
    }

}
