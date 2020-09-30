package performance.stripFce;

import directFiredHeating.process.OneStripDFHProcess;

/**
 * User: M Viswanathan
 * Date: 11-Apr-16
 * Time: 11:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class StripProcessAndSize {
    public String processBaseName;
    public OneStripDFHProcess theProcess;
    public double exitTemp;
    public double width;
    public double thickness;
    public Performance refP;

    public StripProcessAndSize(String process, double exitTemp, double width, double thickness) {
        set(process, exitTemp, width, thickness);
    }

    public StripProcessAndSize set(String process, double exitTemp, double width, double thickness) {
        this.processBaseName = process;
        this.exitTemp = exitTemp;
        this.width = width;
        this.thickness = thickness;
        return this;
    }

    public OneStripDFHProcess getProcess() {
        return theProcess;
    }

    public void setTheProcess(OneStripDFHProcess theProcess) {
        this.theProcess = theProcess;
    }

    public String toString() {
        return "Strip " + width * 1000 + "mm x " + thickness * 1000 + "mm for process " + processBaseName + ", exitTemp " + exitTemp;
    }
}
