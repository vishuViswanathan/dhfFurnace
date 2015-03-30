package directFiredHeating;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 8/23/12
 * Time: 11:10 AM
 * To change this template use File | Settings | File Templates.
 */
public interface DFHResult {
    public enum Type {
        HEATSUMMARY("Heat Balance - Summary"),
        SECTIONWISE("Heat Balance - Section-wise"),
        TOPSECTIONWISE("Heat Balance - Top Section-wise"),
        BOTSECTIONWISE("Heat Balance - Bottom Section-wise"),
        RECUBALANCE("Heat Balance - Recuperator/s"),
        ALLBALANCES("All Heat Balances"),
        TEMPRESULTS("Temperature Results"),
        TOPtempRESULTS("Top Temperature Results"),
        TOPtempTRENDS("Top Temperature Trends"),
        BOTtempRESULTS("Bottom Temperature Results"),
        BOTtempTRENDS("Bottom Temperature Trends"),
        COMBItempRESULTS("Combined Temperature Results"),
        COMBItempTRENDS("Combined Temperature Trends"),
        ALLtempTRENDS("All Temperature trends"),
        FUELMIX("Fuel Mix"),
        FUELSUMMARY("Fuel Summary"),
        FUELS("Section Fuels"),
        TOPFUELS("Top Section Fuels"),
        BOTFUELS("Bottom Section Fuels"),
        LOSSDETAILS("Loss Details"),
        COMPARISON("Results Comparison");


          private final String resultName;

          Type(String resultName) {
              this.resultName = resultName;
          }

          public String resultName() {
              return resultName;
          }
        public static Type getEnum(String text) {
            if (text != null) {
              for (Type b : Type.values()) {
                if (text.equalsIgnoreCase(b.resultName)) {
                  return b;
                }
              }
            }
            return null;
          }


          @Override
          public String toString() {
              return resultName;    //To change body of overridden methods use File | Settings | File Templates.
          }
      }

}
