package SimpleReactiveTrainAlarm;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RandomPositionGenerator {
	
	List<PositionUpdateListener> listeners = new LinkedList<PositionUpdateListener>();
	
	boolean running =false;
	
	String type = "";
	
	
	public RandomPositionGenerator(String type) {
		this.type = type;
	}
	
	public Position getNewPosition() {	
		
		try {
			TimeUnit.SECONDS.sleep(Math.round(Math.random()*5.0));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Position pos = new Position();
		pos.shufflePosition();
		
		return pos;
	}
	
	public void addPositionListener (PositionUpdateListener listener) {
		listeners.add(listener);
	}
	
	public void removePositionListener (PositionUpdateListener listener) {
		listeners.remove(listener);
	}
	
	public void startMoving() {
		
		if(!running) { 
			running = true;
			
			System.out.println(type + " started to move");
		
			new Thread(() -> {
			
				while (true) {
					try {
						TimeUnit.SECONDS.sleep(Math.round(Math.random()*5.0));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					Position pos = new Position();
					pos.shufflePosition();
					
					//System.out.println("new position from " + type + ": " + pos.toString() + " - " + listeners.size() + " listeners will be informed");
					
					for (int i=0; i<listeners.size(); i++){				
						listeners.get(i).updatePosition(pos);
					}
				}
			
			}).start();

		}	
	}
}
