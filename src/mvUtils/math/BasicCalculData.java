package mvUtils.math;
import java.awt.*;
import java.io.Serializable;



/**
 * interface BasicCalculData
 * The interfce for details of data entered for calculation,
 * some base drawing, title etc.
 */
 
public interface BasicCalculData extends Serializable{
	public abstract String title();
	public abstract void drawBasePic(Graphics g, Rectangle area,
																			Point origin);
	
}