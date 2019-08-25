package SimpleReactiveTrainAlarm;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

/**
 * SimpleReactiveTrainAlarm ist eine einfache Applikation zur Demonstration von Konzepten zum reaktiven Programmieren
 * 
 * @author Andreas Biesdorf
 * @version 1.0
 */
public class SimpleReactiveTrainAlarm {
	
	private Disposable trainPositionDisposable, workerPositionDisposable;
	private JTextArea logTextArea; 
	private JButton acknowledgeWarningButton;
	private DrawingPanel dp;
	private Position lastPositionWorker, lastPositionTrain;
	private JTextField textFieldTrain, textFieldWorker, textFieldDistance;
	private JButton buttonStartTrackingTrain, buttonStopTrackingTrain, buttonStartTrackingWorker, buttonStopTrackingWorker, buttonComputeDistance;
			
	public SimpleReactiveTrainAlarm() {

		/********
		 * Erzeuge einfache UI zur Visualisierung der Positionen
		 ********/		
		JFrame visualizationUI = new JFrame("Reactive Train Alarm Application - Visualization");
		setupVisualizationUI(visualizationUI);
		visualizationUI.setSize(400, 400);
		visualizationUI.setVisible(true);	
		
		/********
		 * Erzeuge einfache UI zur Darstellung des Log-Fensters
		 ********/
		JFrame logUI = new JFrame("Reactive Train Alarm Application - Controller");
		setupLogUI(logUI);
		logUI.setSize(600, 400);
		logUI.setVisible(true);
		
		/********
		 * Erzeuge einfache UI zur Darstellung der Werte 
		 ********/
		JFrame simpleUI = new JFrame("Simple Reactive Train Alarm Application");
		setupSimpleUI(simpleUI);
		simpleUI.setVisible(true);
		simpleUI.setSize(500,100);
		
		/********
		 * Erzeuge Observable und Observer für die Zugposition
		 ********/
		// Erzeuge zufällige GPS-Positionen des Zugs 
		TrainPositionIoTConnector trainPositionIoTConnector = new TrainPositionIoTConnector("Train");
		trainPositionIoTConnector.startMoving();
		
		// Erzeuge ein Observable, um Subscriptions auf diese Positionsänderung zu ermöglichen
		Observable<Position> trainPosition = Observable.create( new ObservableOnSubscribe<Position> () {

			@Override
			public void subscribe(ObservableEmitter<Position> emitter) throws Exception {
				
				PositionAdapter positionAdapterTrainPosition = new PositionAdapter() {
					
					@Override
					public void updatePosition(Position newPosition) {
						emitter.onNext(newPosition);							
					}
					
					@Override
					public void lastPosition() {
						emitter.onComplete();	
					}

					@Override
					public void updatePositionError(Exception exception) {
						emitter.onError(exception);	
					}
				};
				
				trainPositionIoTConnector.addPositionListener(positionAdapterTrainPosition); 
			}			
		
		});
		
		// Log in UI		
		trainPosition.subscribe (
				(Position newTrainPosition) -> log("new position from Train: " + newTrainPosition.toString()), 
				exception -> exception.printStackTrace()
				);

		// Aktualisiere die Visualisierung
		trainPosition.subscribe(
				(Position newTrainPosition) -> paintPoint(newTrainPosition, true),
				exception -> exception.printStackTrace()
				);	

		buttonStartTrackingTrain.addActionListener ( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if(trainPositionDisposable == null || (trainPositionDisposable.isDisposed())) {
					trainPositionDisposable = trainPosition.subscribe(e -> textFieldTrain.setText(e.toString()));
				}
			}
		});
			
		buttonStopTrackingTrain.addActionListener ( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (trainPositionDisposable != null) {
					trainPositionDisposable.dispose();
				}
			}
		});
		
		/********
		 * Erzeuge Observable und Observer für die Arbeiterposition
		 ********/
		// Erzeuge zufällige GPS-positionen des Arbeiters 
		WorkerPositionGenerator workerPositionGenerator = new WorkerPositionGenerator("Worker");
		workerPositionGenerator.startMoving();
		
		// Erzeuge ein Observable, um Subscriptions auf diese Positionsänderung zu ermöglichen
		Observable<Position> workerPosition = Observable.create(new ObservableOnSubscribe<Position>() {

			@Override
			public void subscribe(ObservableEmitter<Position> emitter) throws Exception {
				
				PositionAdapter positionAdapterWorkerPosition = new PositionAdapter() {
					@Override
					public void updatePosition(Position newPosition) {
						emitter.onNext(newPosition);							
					}
					
					@Override
					public void lastPosition() {
						emitter.onComplete();	
					}

					@Override
					public void updatePositionError(Exception exception) {
						emitter.onError(exception);	
					}
				};
				
				workerPositionGenerator.addPositionListener(positionAdapterWorkerPosition);
			}
		
		});
		
		// Log in UI 
		workerPosition.subscribe(
				(Position newWorkerPosition) -> log("new position from Worker: " + newWorkerPosition.toString()), 
				exception -> exception.printStackTrace()
				);
		
		// Aktualisiere die Visualisierung
		workerPosition.subscribe(
				(Position newWorkerPosition) -> paintPoint(newWorkerPosition, false),
				exception -> exception.printStackTrace()
				);
		
		buttonStartTrackingWorker.addActionListener ( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if((workerPositionDisposable == null) || workerPositionDisposable.isDisposed()){
					workerPositionDisposable = workerPosition.subscribe(e -> textFieldWorker.setText(e.toString()));
				}
			}
		});	
	
		buttonStopTrackingWorker.addActionListener ( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (workerPositionDisposable != null) {
					workerPositionDisposable.dispose();
				}
			}
		});	
		
		/********
		 * Kombiniere mehrere Event Streams, bestimme die Abstände und subscribe einen Observer auf das Ergebnis
		 ********/
		
		buttonComputeDistance.addActionListener ( new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent event) {

				// update Text field whenever a change comes in 
				Observable
				.combineLatest(trainPosition, 
						workerPosition, 
						(trainPosition, workerPosition) -> trainPosition.computeEuclideanDistanceFrom(workerPosition))
				.subscribe(distanceTrainWorker -> textFieldDistance.setText((new DecimalFormat("#0.00")).format(distanceTrainWorker)));				
				
				// issue a warning if the distance is <3.0
				Observable
				.combineLatest(trainPosition, 
						workerPosition, (trainPosition,workerPosition) -> trainPosition.computeEuclideanDistanceFrom(workerPosition))
				.filter(distance -> distance < 3.0)
				.subscribe(distance -> issueWarning(distance));
				
			}
		});	
		
		/********
		 * Erzeuge Observable und Observer für die Alarm-Bestätigung
		 ********/
		
		// Erzeuge Observable, um das Bestätigen von Alarmen zu registrieren
		Observable<Boolean> acknowledgeAlarm = Observable.create(new ObservableOnSubscribe<Boolean>() {

			@Override
			public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
				acknowledgeWarningButton.addActionListener ( new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {						
						e.onNext(true);
					}
				});
			}
		});
		
		// interne Methode um das Acknowledgement zu verarbeiten
		acknowledgeAlarm.subscribe( isAcknowledged -> acknowledgeAlarm(isAcknowledged)); 

	}
	
	/**
	 * Verarbeiten aller Bestätigungen des Alarms  
	 * @param isAcknowledged true, wenn bestätigt
	 */
	public void acknowledgeAlarm(boolean isAcknowledged) {
		if (isAcknowledged) {
			acknowledgeWarningButton.setEnabled(false);
			acknowledgeWarningButton.setBackground(Color.lightGray);
			acknowledgeWarningButton.setOpaque(false);
			acknowledgeWarningButton.setBorderPainted(true);;		
			
			log ("Warning Acknowledged");
		}
	}
	
	/**
	 * Anzeigen der Warning
	 * @param distance Abstand zwischen Zug und Arbeiter
	 */
	public void issueWarning (Double distance) {
		
		log("Warning (d<3): " + (new DecimalFormat("#0.00")).format(distance) + " at " + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now()));
		acknowledgeWarningButton.setEnabled(true);		
		acknowledgeWarningButton.setBackground(Color.RED);
		acknowledgeWarningButton.setOpaque(true);
		acknowledgeWarningButton.setBorderPainted(false);;
		
	}
	
	/**
	 * Anhängen von Text am Log-Fenster 
	 * @param logText Anzuhängender Text 
	 */
	private void log(String logText) {
		logTextArea.append("[" + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now()) + "]: " + logText + "\n");
		logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
	}
	
	/**
	 * Visualisierungs-Hilfsfunktion
	 * @param pos Position des Punkts
	 * @param isTrain true, wenn Zug
	 */
	private void paintPoint (Position pos, boolean isTrain) {
		
		
		dp.getG2d().clearRect(0, 0, dp.getWidth(), dp.getHeight());
		

		

		
		if (isTrain) {
			
			paintRectangle(pos, lastPositionWorker, Color.red, false);
			
			
			
			if (lastPositionWorker != null)
				paintRectangle(lastPositionWorker, lastPositionWorker, Color.blue, true);
						
			lastPositionTrain = pos;
			
		} else {
			

			paintRectangle(pos, lastPositionTrain, Color.blue, true);
			
			if (lastPositionTrain != null)
				paintRectangle(lastPositionTrain, lastPositionTrain, Color.red, false);
						
			lastPositionWorker = pos;
						
		}
	}
	
	/**
	 * Visualisierungs-Hilfsfunktion
	 * @param pos Position erstes Objekt
	 * @param pos2 Position zweites Objekt
	 * @param col Farbe des ersten Objekts
	 * @param isWorker true, wenn Arbeiter
	 */
	private void paintRectangle(Position pos, Position pos2, Color col, boolean isWorker) {

		Dimension d = dp.getSize();

		dp.getG2d().setColor(Color.black);
		
		dp.getG2d().drawOval((int)Math.round(1.1* d.width/10.0), (int)Math.round(1.1 * d.height/10.0), (int) Math.round(7.8* d.width/10.0), (int) Math.round(7.8 * d.height/10.0));
		dp.getG2d().drawOval((int)Math.round(0.9* d.width/10.0), (int)Math.round(0.9 * d.height/10.0), (int) Math.round(8.2* d.width/10.0), (int) Math.round(8.2 * d.height/10.0));
		
		double x= pos.getPosition().get(0);
		double y= pos.getPosition().get(1);
		
		double x2=x,y2=y,x3=x,y3=y;
		
		if (pos2 != null  ) {

			x2= pos2.getPosition().get(0);
			y2= pos2.getPosition().get(1);
			
			x3= x + (x2 - x)/2.0;
			y3= y + (y2 - y)/2.0;
			
			double distance = Math.sqrt((x-x2)*(x-x2) + (y-y2)*(y-y2));
			
			//System.out.println(x + " - " + y + " - " + x2 + " - " + y2 + " - " + x3 + " - " + y3);
			
			if (x != x2 && y != y2) {
			dp.getG2d().drawLine((int)Math.round(x* d.width/10.0), (int)Math.round(y * d.height/10.0), (int)Math.round(x2* d.width/10.0), (int)Math.round(y2 * d.height/10.0));
			
			if (!isWorker) 
			dp.getG2d().drawString((new DecimalFormat("#0.00")).format(distance), (int)Math.round(x3* d.width/10.0), (int)Math.round(y3 * d.height/10.0));
			}
		}
		
		dp.getG2d().setColor(col);
		
		dp.getG2d().fillRect((int)Math.round(x* d.width/10.0)-3, (int)Math.round(y * d.height/10.0)-3, 7, 7);
		 
		
		double radiusX = 3.0/5.0 * d.width;
		double radiusY = 3.0/5.0 * d.height;
		
		
		if (isWorker)
			dp.getG2d().drawOval((int)Math.round(x* d.width/10.0 - radiusX/2.0), (int)Math.round(y * d.height/10.0 - radiusY/2.0), (int) Math.round(radiusX), (int) Math.round(radiusY));
				
		
		
		
		dp.repaint();
	}
	
	/**
	 * UI Hilfsfunktion
	 * @param simpleUI Basis-UI zum Befüllen
	 */
	private void setupSimpleUI (JFrame simpleUI) { 
	
		GridLayout layout = new GridLayout(3,4,2,2);
		
		simpleUI.setLayout(layout);
		
		textFieldTrain = new JTextField();
		buttonStartTrackingTrain = new JButton("Track");
		buttonStopTrackingTrain = new JButton("Untrack");
		simpleUI.add(new JLabel("Train"));
		simpleUI.add(textFieldTrain);
		simpleUI.add(buttonStartTrackingTrain);
		simpleUI.add(buttonStopTrackingTrain);
		
	
		textFieldWorker = new JTextField();
		buttonStartTrackingWorker = new JButton("Track");
		buttonStopTrackingWorker = new JButton("Untrack");
		simpleUI.add(new JLabel("Worker"));
		simpleUI.add(textFieldWorker);
		simpleUI.add(buttonStartTrackingWorker);
		simpleUI.add(buttonStopTrackingWorker);
		
		textFieldDistance = new JTextField();
		acknowledgeWarningButton = new JButton("Acknowledge Warning");
		buttonComputeDistance = new JButton("Compute");
		acknowledgeWarningButton.setEnabled(false);
		simpleUI.add(new JLabel("Distance"));
		simpleUI.add(textFieldDistance);
		simpleUI.add(buttonComputeDistance);
		simpleUI.add(acknowledgeWarningButton);
	}
	
	/**
	 * UI Hilfsfunktion 
	 * @param logUI Basis-UI zum Befüllen
	 */
	private void setupLogUI (JFrame logUI) {
		logUI.setLayout(new GridLayout());
		
		logTextArea = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(logTextArea);
		logUI.add(scrollPane);
	}
	
	/** 
	 * UI Hilfsfunktion
	 * @param visualizationUI Basis-UI zum Befüllen
	 */
	private void setupVisualizationUI (JFrame visualizationUI) {
		visualizationUI.setLayout(new GridLayout(1, 1, 5, 5));
		dp = new DrawingPanel();
		visualizationUI.add(dp);
	}
	
	public static void main(String[] args) { 
		new SimpleReactiveTrainAlarm();
	}

}
