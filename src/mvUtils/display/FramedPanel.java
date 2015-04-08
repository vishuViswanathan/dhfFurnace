package mvUtils.display;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class FramedPanel
    extends JPanel {
  boolean raisedPanel = true;
//	Insets insets = new Insets(4, 4, 4, 4);

//	public FramedPanel(boolean raisedPanel) {
//		super();
//        setBorder(new EtchedBorder(EtchedBorder.RAISED));
//		this.raisedPanel = raisedPanel;
//	}
//
//	public FramedPanel() {
//		super();
//	}
//
  public FramedPanel(LayoutManager lm) {
    super(lm);
    setBorder(new EtchedBorder(EtchedBorder.RAISED));
  }

  public FramedPanel() {
    this(new FlowLayout());
  }

}