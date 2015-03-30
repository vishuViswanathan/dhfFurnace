package mvmath;
import java.awt.event.*;

/**
 * an inteface for despatching events of the container to
 * components
 */

public interface EventDespatcher {
	public void addFocusListener(FocusListener fl);
	public void addMouseListener(MouseListener ml);
	public void addComponentListener(ComponentListener cl);
}