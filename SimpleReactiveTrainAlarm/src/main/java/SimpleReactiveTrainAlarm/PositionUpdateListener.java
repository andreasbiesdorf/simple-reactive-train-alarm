package SimpleReactiveTrainAlarm;

/**
 * Simple callback-interface to represent changes in positions of objects 
 * 
 * 
 * @author Andreas Biesdorf
 * @version 1.0
 */

public interface PositionUpdateListener {

	/**
	 * Call, when new position has changed 
	 * @param newPosition new position of the object
	 */
	public void updatePosition (Position newPosition);
	
	/**
	 * Called when an error or exception has occurred while changing the position
	 * @param exception Exception that has occurred  
	 */
	public void updatePositionError(Exception exception);
	
	/**
	 * Called, when no more positions will be sent
	 */
	public void lastPosition ();
}
