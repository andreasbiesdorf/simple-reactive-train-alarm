package SimpleReactiveTrainAlarm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;
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

public class SimpleReactiveTrainAlarm {
	
	private Disposable trainPositionDisposable, workerPositionDisposable;
	private Disposable trainDrawingDisposable, workerDrawingDisposable;
	
	private PositionAdapter positionAdapterWorkerPosition, positionAdapterTrainPosition;
	
	private JTextArea logTextArea; 
	
	private DrawingPanel dp, dpMarbles;
	
	private Position lastPositionWorker, lastPositionTrain;
		
	public SimpleReactiveTrainAlarm() {
		
		JFrame simpleUI = new JFrame("Simple Reactive Train Alarm Application");
		GridLayout layout = new GridLayout(3,4,2,2);
		
		simpleUI.setLayout(layout);
		
		JTextField textFieldTrain = new JTextField();
		JButton buttonStartTrain = new JButton("Track");
		JButton buttonStopTrain = new JButton("Untrack");
		simpleUI.add(new JLabel("Train"));
		simpleUI.add(textFieldTrain);
		simpleUI.add(buttonStartTrain);
		simpleUI.add(buttonStopTrain);
		

		JTextField textFieldWorker = new JTextField();
		JButton buttonStartWorker = new JButton("Track");
		JButton buttonStopWorker = new JButton("Untrack");
		simpleUI.add(new JLabel("Worker"));
		simpleUI.add(textFieldWorker);
		simpleUI.add(buttonStartWorker);
		simpleUI.add(buttonStopWorker);
		
		JTextField textFieldDistance = new JTextField();
		JButton acknowledgeWarningButton = new JButton("Acknowledge Warning");
		JButton buttonComputeDistance = new JButton("Compute");
		acknowledgeWarningButton.setEnabled(false);
		simpleUI.add(new JLabel("Distance"));
		simpleUI.add(textFieldDistance);
		simpleUI.add(buttonComputeDistance);
		simpleUI.add(acknowledgeWarningButton);

		
		simpleUI.setVisible(true);
		simpleUI.setSize(500,100);
		
		RandomPositionGenerator rpgTrain = new RandomPositionGenerator("Train");
		rpgTrain.startMoving();

		RandomPositionGenerator rpgWorker = new RandomPositionGenerator("Worker");
		rpgWorker.startMoving();
		
		
		JFrame logUI = new JFrame("Reactive Train Alarm Application - Controller"); 
		logUI.setLayout(new GridLayout());
		
		logTextArea = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(logTextArea);
		logUI.add(scrollPane);
		logUI.setSize(600, 400);
		logUI.setVisible(true);
		
		
		JFrame visualizationUI = new JFrame("Reactive Train Alarm Application - Visualization");
		visualizationUI.setLayout(new GridLayout(1, 1, 5, 5));
		dp = new DrawingPanel();
		visualizationUI.add(dp);
		visualizationUI.setSize(400, 400);
		visualizationUI.setVisible(true);
		
		
		JFrame marbleVisualizationUI = new JFrame("Reactive Train Alarm Application - Marble Diagram");
		visualizationUI.setLayout(new GridLayout(1, 1, 5, 5));			
		dpMarbles = new DrawingPanel();
		marbleVisualizationUI.add(dpMarbles);
		marbleVisualizationUI.setSize(800, 200);
		marbleVisualizationUI.setVisible(true);
				
		
		Observable<Position> trainPosition = Observable.create(new ObservableOnSubscribe<Position>() {

			@Override
			public void subscribe(ObservableEmitter<Position> e) throws Exception {
				
				positionAdapterTrainPosition = new PositionAdapter() {
					@Override
					public void updatePosition(Position newPosition) {
						e.onNext(newPosition);							
					}
				};
				
				rpgTrain.addPositionListener(positionAdapterTrainPosition); 
			}			
		
		});
		
		// Log in UI		
		trainPosition.subscribe(e -> log("new position from Train: " + e.toString()));
		trainDrawingDisposable = trainPosition.subscribe(e -> paintPoint(e, true));
		
		
		Observable<Position> workerPosition = Observable.create(new ObservableOnSubscribe<Position>() {

			@Override
			public void subscribe(ObservableEmitter<Position> e) throws Exception {
				
				positionAdapterWorkerPosition = new PositionAdapter() {
					@Override
					public void updatePosition(Position newPosition) {
						e.onNext(newPosition);							
					}
				};
				
				rpgWorker.addPositionListener(positionAdapterWorkerPosition);
			}
		
		});
		
		// Log in UI 
		workerPosition.subscribe(e -> log("new position from Worker: " + e.toString()));		
		workerDrawingDisposable = workerPosition.subscribe(e -> paintPoint(e, false));
		
		
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
		
		acknowledgeAlarm.subscribe( e -> acknowledgeAlarm(acknowledgeWarningButton)); 
		
		
		
		buttonStartTrain.addActionListener ( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {				
				trainPositionDisposable = trainPosition.subscribe(e -> textFieldTrain.setText(e.toString()));	
				//trainDrawingDisposable = trainPosition.subscribe(e -> paintPoint(e, true));
			}
		});
		
		buttonStartWorker.addActionListener ( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {				
				workerPositionDisposable = workerPosition.subscribe(e -> textFieldWorker.setText(e.toString()));
				//workerDrawingDisposable = workerPosition.subscribe(e -> paintPoint(e, false));
			}
		});	
		
		buttonComputeDistance.addActionListener ( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
								
				// update Text field whenever a change comes in 
				Observable.combineLatest(trainPosition, workerPosition, (x,y) -> x.distance(y)).subscribe(e -> textFieldDistance.setText((new DecimalFormat("#0.00")).format(e)));
				
				// issue a warning if the distance is <3.0
				Observable.combineLatest(trainPosition, workerPosition, (x,y) -> x.distance(y)).filter(e -> e<3.0).subscribe(e -> setupWarning(acknowledgeWarningButton, e));
				
			}
		});	
		
		acknowledgeWarningButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				acknowledgeWarningButton.setEnabled(false);
			}
		});

		buttonStopTrain.addActionListener ( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (trainPositionDisposable != null) {
					trainPositionDisposable.dispose();
				}
				if (trainDrawingDisposable != null) {
					//trainDrawingDisposable.dispose();
				}
				
				if (positionAdapterTrainPosition != null) {
					rpgTrain.removePositionListener(positionAdapterTrainPosition);
				}
			}
		});
		
		buttonStopWorker.addActionListener ( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (workerPositionDisposable != null) {
					workerPositionDisposable.dispose();
				}
				if (workerDrawingDisposable != null) {
					//workerDrawingDisposable.dispose();
				}

				if (positionAdapterWorkerPosition != null) {
					rpgWorker.removePositionListener(positionAdapterWorkerPosition);
				}
			}
		});	
	}
	
	public void acknowledgeAlarm(JButton acknowledgeWarningButton) {
		acknowledgeWarningButton.setEnabled(false);
		acknowledgeWarningButton.setBackground(Color.lightGray);
		acknowledgeWarningButton.setOpaque(false);
		acknowledgeWarningButton.setBorderPainted(true);;
		
		
		log ("Warning Acknowledged");		
	}
	
	public void setupWarning (JButton acknowledgeWarningButton, Double distance) {
		
		log("Warning (d<3): " + (new DecimalFormat("#0.00")).format(distance) + " at " + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now()));
		acknowledgeWarningButton.setEnabled(true);		
		acknowledgeWarningButton.setBackground(Color.RED);
		acknowledgeWarningButton.setOpaque(true);
		acknowledgeWarningButton.setBorderPainted(false);;
		
		java.awt.Toolkit.getDefaultToolkit().beep();
	}
	
	private void log(String logText) {
		
		logTextArea.setText(logTextArea.getText() + "\n"  + "[" + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now()) + "]: " + logText );
		
	}
	
	private void paintPoint (Position pos, boolean train) {
		
		
		dp.getG2d().clearRect(0, 0, dp.getWidth(), dp.getHeight());
		

		

		
		if (train) {
			
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
	
	private void paintRectangle(Position pos, Position pos2, Color col, boolean isWorker) {

		Dimension d = dp.getSize();

		dp.getG2d().setColor(Color.black);
		
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
		
		/*Image image;
		try {
			image = ImageIO.read(new File("./src/main/java/SimpleReactiveTrainAlarm/train.png"));
			dp.getG2d().drawImage(image, (int)Math.round(x* d.width/10.0)-3, (int)Math.round(y * d.width/10.0)+3, (int) Math.round(image.getHeight(null)/5.0), (int) Math.round(image.getWidth(null)/5.0), null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		 
		
		double radiusX = 3.0/5.0 * d.width;
		double radiusY = 3.0/5.0 * d.height;
		
		
		if (isWorker)
			dp.getG2d().drawOval((int)Math.round(x* d.width/10.0 - radiusX/2.0), (int)Math.round(y * d.height/10.0 - radiusY/2.0), (int) Math.round(radiusX), (int) Math.round(radiusY));
		
		dp.repaint();
	}
	
	public static void main(String[] args) {
		new SimpleReactiveTrainAlarm();
	}

}
