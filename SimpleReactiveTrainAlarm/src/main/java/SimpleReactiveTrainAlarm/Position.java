package SimpleReactiveTrainAlarm;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Position {
	
	private double[] pos = new double[3];
	
	public Position() {
		for (int i=0; i<3; i++)
			pos[i] = 0.0;
	}
	
	public Position(List<Double> position) {
		for (int i=0; i<3; i++)
			this.pos[i] = position.get(i);
	}
	
	public void shufflePosition() {
		for (int i=0; i<3; i++)
			this.pos[i] = Math.random() * 10.0;		
	}
	
	public List<Double> getPosition(){	
		return new ArrayList<Double>(Arrays.asList(pos[0],pos[1],pos[2]));
	}

	public void setPosition(List<Double> position){
		for (int i=0; i<3; i++)
			this.pos[i] = position.get(i);		
	}
	
	public String toString() { 
		DecimalFormat f = new DecimalFormat("#0.00");
		return "[" + f.format(pos[0]) + " - " + f.format(pos[1]) + " - " + f.format(pos[2])  + "]";
	}
	
	public double distance (Position position2) {
		double distance = 0.0;
		
		for (int i=0; i<3; i++)
			distance += Math.pow(pos[i]-position2.getPosition().get(i), 2.0);
		
		return Math.sqrt(distance);
		
	}

}
