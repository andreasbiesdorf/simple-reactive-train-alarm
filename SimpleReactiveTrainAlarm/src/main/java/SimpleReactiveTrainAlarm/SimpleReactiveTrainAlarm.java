package SimpleReactiveTrainAlarm;


import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

public class SimpleReactiveTrainAlarm {
	
	private Disposable trainPositionDisposable, workerPositionDisposable; 
	private PositionAdapter positionAdapterWorkerPosition, positionAdapterTrainPosition;
	
	
	public SimpleReactiveTrainAlarm() {
		
		JFrame simpleUI = new JFrame();
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
		
		
		/*Observable<Boolean> startTrainTracking = Observable.create(new ObservableOnSubscribe<Boolean>() {

			@Override
			public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
				
				buttonStartTrain.addActionListener ( new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						//trainPositionDisposable = trainPosition.subscribe(e -> textFieldTrain.setText(e.toString()));
						e.onNext(true);
						e.onComplete();
					}
				});	
			}
		});
		
		startTrainTracking.subscribe( e ->trainPositionDisposable = trainPosition.subscribe(x -> textFieldTrain.setText(x.toString()))); 
		*/
		
		
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
			}
		});
		
		buttonStartWorker.addActionListener ( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {				
				workerPositionDisposable = workerPosition.subscribe(e -> textFieldWorker.setText(e.toString()));
			}
		});	
		
		buttonComputeDistance.addActionListener ( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				Observable.combineLatest(trainPosition, workerPosition, (x,y) -> x.distance(y)).filter(e -> e<3.0).subscribe(e -> System.out.println("Warning (d<3): " + (new DecimalFormat("#0.00")).format(e) + " at " + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now())));
				Observable.combineLatest(trainPosition, workerPosition, (x,y) -> x.distance(y)).filter(e -> e<3.0).subscribe(e -> acknowledgeWarningButton.setEnabled(true));
				Observable.combineLatest(trainPosition, workerPosition, (x,y) -> x.distance(y)).subscribe(e -> textFieldDistance.setText((new DecimalFormat("#0.00")).format(e)));				
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

				if (positionAdapterWorkerPosition != null) {
					rpgWorker.removePositionListener(positionAdapterWorkerPosition);
				}
			}
		});	
		
	
	}
	
	public void acknowledgeAlarm(JButton acknowledgeWarningButton) {
		acknowledgeWarningButton.setEnabled(false);
		System.out.println("Warning tracked");
	}
	
	
	
	public static void main(String[] args) {
		new SimpleReactiveTrainAlarm();
	}

}
