import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Period {
	
	private Date time;
	private int rating;
	private HashMap<String, List<Call>> calls;
	private HashMap<String, List<Message>> messages;
	private HashMap<String, List<AppUsage>> appUsages;
	private List<ConnState> connectionStates;
	private List<Coordinate> coordinates;
	private List<ScreenState> screenOns;

	public Period(Date ratingTime, int rating) {
		time = ratingTime;
		this.rating = rating;
		calls = new HashMap<String, List<Call>>();
		messages = new HashMap<String, List<Message>>();
		appUsages = new HashMap<String, List<AppUsage>>();
		connectionStates = new ArrayList<ConnState>();
		coordinates = new ArrayList<Coordinate>();
		screenOns = new ArrayList<ScreenState>();
	}

	public Date getDate() {
		return time;
	}

	public void addSensorData(SensorData data) {
		
		if(data instanceof AppUsage) {
			
			AppUsage currentUsage = (AppUsage) data;
			
			//Has this app already been used in this period?
			//If yes then add the usage to the list associated
			//with this app. If no then make a new list and add
			//the usage to it then add the list to the hashMap.
			if(appUsages.containsKey(currentUsage.getName())) {
				
				appUsages.get(currentUsage.getName()).add(currentUsage);
				
			} else {
				
				List<AppUsage> usagesForNewApp = new ArrayList<AppUsage>();
				usagesForNewApp.add(currentUsage);
				appUsages.put(currentUsage.getName(), usagesForNewApp);
				
			}
			
		} else if(data instanceof Call) {
			
			Call currentCall = (Call) data;
			
			//Has this number been called already in this period? If yes, add the call
			//to the list associated with that number, if no then make a new
			//list and add the call to it, then add to the hashMap.
			if(calls.containsKey(currentCall.getPhoneNumber())) {
				
				calls.get(currentCall.getPhoneNumber()).add(currentCall);
				
			} else {
				
				List<Call> callsForNewNumber = new ArrayList<Call>();
				callsForNewNumber.add(currentCall);
				calls.put(currentCall.getPhoneNumber(), callsForNewNumber);
				
			}
			
		} else if(data instanceof ConnState) {
			
			ConnState currentConnection = (ConnState) data;
			
			//Add to the list s.t. the list remains in ascending date order.
			int i;
			
			for(i=0; i<connectionStates.size(); i++) {
				
				//As soon as we find a connState which is after the current
				//Then we break and insert at this i
				if(connectionStates.get(i).getDate().after(currentConnection.getDate())) {
					break;
				}
				
			}
			
			connectionStates.add(i, currentConnection);
			
		} else if(data instanceof Coordinate) {
			
			Coordinate currentCoordinate = (Coordinate) data;
			
			//Add to the list s.t. the list remains in ascending date order.
			int i;
			
			for(i=0; i<coordinates.size(); i++) {
				
				//As soon as we find a connState which is after the current
				//Then we break and insert at this i
				if(coordinates.get(i).getDate().after(currentCoordinate.getDate())) {
					break;
				}
				
			}
			
			coordinates.add(i, currentCoordinate);
			
		} else if(data instanceof Message) {
			
			Message currentMsg = (Message) data;
			
			//Has this number been messaged already in this period? If yes, add the message
			//to the list associated with that number, if no then make a new
			//list and add the call to it, then add to the hashMap.
			if(messages.containsKey(currentMsg.getPhoneNumber())) {
				
				messages.get(currentMsg.getPhoneNumber()).add(currentMsg);
				
			} else {
				
				List<Message> msgsForNewNumber = new ArrayList<Message>();
				msgsForNewNumber.add(currentMsg);
				messages.put(currentMsg.getPhoneNumber(), msgsForNewNumber);
				
			}
			
		} else if(data instanceof ScreenState) {
			
			ScreenState currentScreen = (ScreenState) data;
			
			//Add to the list s.t. the list remains in ascending date order.
			int i;
			
			for(i=0; i<screenOns.size(); i++) {
				
				//As soon as we find a connState which is after the current
				//Then we break and insert at this i
				if(screenOns.get(i).getDate().after(currentScreen.getDate())) {
					break;
				}
				
			}
			
			screenOns.add(i, currentScreen);
			
		} else {
			System.out.println("Tried to add an invalid sensor");
		}
		
	}

	public String outputFeatures(Date prevPeriodTime) {
		
		//Period length in minutes
		float periodLength = Participant.MAX_DATA_AGE/(1000*60);
		
		if(prevPeriodTime != null) {
			periodLength = (time.getTime() - prevPeriodTime.getTime())/(1000*60);
			if(periodLength > Participant.MAX_DATA_AGE/(1000*60)) {
				periodLength = Participant.MAX_DATA_AGE/(1000*60);
			}
		}
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(time);

		if(DataProcessor.debug) {
			//Debug output
			return "Call:\n" + Call.computeFeatures(calls, periodLength) + "\n" + 
			"Message:\n" + Message.computeFeatures(messages, periodLength) + "\n" +
			"AppUsage:\n" + AppUsage.computeFeatures(appUsages, periodLength) + "\n" +
			"ConnState:\n" + ConnState.computeFeatures(connectionStates, periodLength) + "\n" +
			"Coordinate:\n" + Coordinate.computeFeatures(coordinates, periodLength) + "\n" +
			"ScreenState:\n" + ScreenState.computeFeatures(screenOns, periodLength) + "\n" +
			"Hour: " + calendar.get(Calendar.HOUR_OF_DAY) + "\n" +
			"Rating: " + rating + "\n" + "---------------------------------------\n";
		}
		
		//Features should be calculated relative to the length of
		//this period.
		return Call.computeFeatures(calls, periodLength) + "," + 
		Message.computeFeatures(messages, periodLength) + "," +
		AppUsage.computeFeatures(appUsages, periodLength) + "," +
		ConnState.computeFeatures(connectionStates, periodLength) + "," +
		Coordinate.computeFeatures(coordinates, periodLength) + "," +
		ScreenState.computeFeatures(screenOns, periodLength) + "," +
		calendar.get(Calendar.HOUR_OF_DAY) + "," +
		rating + "\n";

	}

	public void setTime(long newTime) {
		
		time.setTime(newTime);
		
	}

	public static String outputFeatureNames() {

		return Call.featureNames() + "," + 
				Message.featureNames() + "," +
				AppUsage.featureNames() + "," +
				ConnState.featureNames() + "," +
				Coordinate.featureNames() + "," +
				ScreenState.featureNames() + "," +
				"hour_of_day" + "," +
				"rating" + "\n";
		
	}

}
