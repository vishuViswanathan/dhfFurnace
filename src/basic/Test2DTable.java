package basic;

import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Test2DTable extends Object {
    TwoDTable table;
    JFrame mainFr;
    JTextField colF, rowF, dataF;

    public Test2DTable(String fName) {
        FileInputStream file = null;
        try {
            file = new FileInputStream(fName);
        } catch (IOException ioe) {
            System.err.println("Test2DTable: " + ioe);
            System.exit(1);
        }
        table = new TwoDTable(file, "Test2DTable");
        setupDialog();
    }

    void setupDialog() {
        mainFr = new JFrame("Test2DTable");
        mainFr.addWindowListener(
                new WindowAdapter() {
                    public void windowClosing(WindowEvent we) {
                        mainFr.setVisible(false);
                        mainFr.dispose();
                        System.exit(0);
                    }
                });

        colF = new JTextField(10);
        rowF = new JTextField(10);
        dataF = new JTextField(10);
        JButton getButt = new JButton("Get Data");
        getButt.addActionListener(
                new GetHandler());
        Container mainPane = mainFr.getContentPane();
        mainPane.setLayout(new FlowLayout());
        mainPane.add(new JLabel("Column Val"));
        mainPane.add(colF);
        mainPane.add(new JLabel("Row Val"));
        mainPane.add(rowF);
        mainPane.add(getButt);
        mainPane.add(new JLabel("Data Val"));
        mainPane.add(dataF);
        mainFr.pack();
        mainFr.setVisible(true);
    }

    class GetHandler implements ActionListener {
        public void actionPerformed(ActionEvent ae) {
            try {
                double colVal = new Double(colF.getText()).doubleValue();
                double rowVal = new Double(rowF.getText()).doubleValue();
                double dataVal = table.getData(colVal, rowVal, true);
                dataF.setText("" + dataVal);
            } catch (NumberFormatException nfe) {
                System.out.println("Number error, reEnter ...");
                dataF.setText("");
            } catch (Exception e) {
                System.out.println("Data Exception ...");
                dataF.setText("" + e);
            }
        }
    }

    public static void main(String[] args) {
        Test2DTable test2DTable = new Test2DTable(args[0]);
    }
}

 