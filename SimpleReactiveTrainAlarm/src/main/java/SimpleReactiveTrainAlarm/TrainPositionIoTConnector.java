package SimpleReactiveTrainAlarm;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrainPositionIoTConnector {
	List<PositionUpdateListener> listeners = new LinkedList<PositionUpdateListener>();
	
	boolean running =false;
	
	String type = "";
	
	
	public TrainPositionIoTConnector(String type) {
		this.type = type;
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
		
			new Thread(() -> {
			
				Position pos = new Position();
				double angle = 0.0;
				
				while (true) {
					try {
						// wait at least a second, potentially longer
						// TimeUnit.SECONDS.sleep(1 + Math.round(Math.random()*5.0));
						TimeUnit.SECONDS.sleep(1 + Math.round(Math.random()*1.0));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					angle = angle += Math.random()*0.2;
					
					pos.setPosition(5.0 + 4.0 * Math.cos(angle), 5.0 + 4.0 * Math.sin(angle));
					
					//pos.shufflePosition();
					
					//System.out.println("new position from " + type + ": " + pos.toString() + " - " + listeners.size() + " listeners will be informed");
					
					for (int i=0; i<listeners.size(); i++){				
						listeners.get(i).updatePosition(pos);
					}
				}
			
			}).start();

		}	
	}
}
