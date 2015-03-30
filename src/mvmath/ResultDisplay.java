package mvmath;



public interface ResultDisplay {

	public abstract void setupInfo(GraphInfo gInfo,
												BasicCalculData basicData);

	public abstract boolean startDisplay(String title);

	public abstract void addMDFrameCloseListener(
						MDFrameCloseListener mdfcl);

	public abstract void removeMDFrameCloseListener(
						MDFrameCloseListener mdfcl);


}