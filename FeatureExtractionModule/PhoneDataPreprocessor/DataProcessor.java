import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.json.JSONObject;

public class DataProcessor {
	
	private Participant participant;
	public static final boolean debug = false;
	public static final boolean threePoint = true;
	
	public static void main(String[] args) {
		
		String callsFile = "Calls.csv";
		String messagesFile = "SMS.csv";
		String appFile = "AppUsage.csv";
		String gpsFile = "GPS.csv";
		String connStateFile = "ConnState.json";
		String screenFile = "Screen.json";
		String ratingsFile = "Ratings.csv";
		String reviewsFile = "Reviews.csv";
		
		DataProcessor processor = new DataProcessor();
		
		//To load only ratings or only reviews, comment out the relevant call
		processor.loadRatings(ratingsFile);
		processor.loadReviews(reviewsFile);
		
		processor.loadCalls(callsFile);
		processor.loadMessages(messagesFile);
		processor.loadApps(appFile);
		processor.loadCoords(gpsFile);
		processor.loadConnState(connStateFile);
		processor.loadScreen(screenFile);
		
		String outputFile = "emotionFeatures.csv";
		String headerFile = "emotionHeader.csv";
		processor.outputData(outputFile);
		processor.outputHeader(headerFile);
		processor.outputRatingTimes("rateTimes.csv");
		
	}

	private void outputHeader(String headerFile) {

		try {
			
			FileWriter fileOut = new FileWriter(headerFile);
			BufferedWriter writer = new BufferedWriter(fileOut);
			
			participant.outputFeatureNames(writer);
			
			writer.close();
			fileOut.close();
			
		} catch(IOException e) {
			
			System.out.println("Error writing output");
			
		}
		
	}

	private void outputRatingTimes(String file) {

		//For each participant, each Period can be considered a data point
		try {
			
			FileWriter fileOut = new FileWriter(file);
			BufferedWriter writer = new BufferedWriter(fileOut);
						
			participant.outputRateTimes(writer);
			
			writer.close();
			fileOut.close();
			
		} catch(IOException e) {
			
			System.out.println("Error writing rate times");
			
		}
		
	}

	/** 
	 * Calculates features and outputs them to a CSV.
	 * @param outputFile The name of the file to output to
	 */
	private void outputData(String outputFile) {

		//For each participant, each Period can be considered a data point
		try {
			
			FileWriter fileOut = new FileWriter(outputFile);
			BufferedWriter writer = new BufferedWriter(fileOut);
			
			participant.outputFeatures(writer);
			
			writer.close();
			fileOut.close();
			
		} catch(IOException e) {
			
			System.out.println("Error writing output");
			
		}
		
	}

	/**
	 * Loads the screen on/off data from the provided file
	 * @param screenFile
	 */
	private void loadScreen(String screenFile) {
		
		try {
			
			File file = new File(screenFile);
			Scanner input = new Scanner(file);
			
			/* Read each line and process it:
			 * Data in JSON format
			 * Extract "senseStartTimeMillis" and "status"
			 * Create ScreenState objects for each interval 
			 * where the screen was on and add them to the participant
			 */
			
			Date onTime = null;
			
			while(input.hasNextLine()) {
				
				String record = input.nextLine();
				
				JSONObject obj = new JSONObject(record);
				String status = obj.getString("status");
				
				long timeStamp = obj.getLong("senseStartTimeMillis");
				Date stateTime = new Date();
				stateTime.setTime(timeStamp);
				
				if(status.equals("SCREEN_ON")) {
					
					onTime = stateTime;
					
				} else if(status.equals("SCREEN_OFF")) {
					
					if(onTime != null) {

						//Calculate time difference
						long duration = stateTime.getTime() - onTime.getTime();
						ScreenState nextState = new ScreenState(onTime, duration);
						
						participant.addSensorData(nextState);
						onTime = null;
						
					}
					
				} else {
					System.out.println("Error in ScreenState file");
				}
				
			}

			input.close();
			
		} catch(FileNotFoundException e) {
			
			System.out.println("Error reading screen input file");
			
		}
		
	}

	/**
	 * Loads the connection state data (e.g. WIFI or MOBILE) from the provided file.
	 * @param connStateFile
	 */
	private void loadConnState(String connStateFile) {
		
		try {
			
			File file = new File(connStateFile);
			Scanner input = new Scanner(file);
			
			/* Read each line and process it:
			 * Data in JSON format
			 * Simply want to extract "senseStartTimeMillis" and "networkType"
			 */
			
			while(input.hasNextLine()) {
				
				String record = input.nextLine();
				
				JSONObject obj = new JSONObject(record);
				String networkType = obj.getString("networkType");
				
				long timeStamp = obj.getLong("senseStartTimeMillis");
				Date stateTime = new Date();
				stateTime.setTime(timeStamp);
				
				ConnState nextState = new ConnState(stateTime, networkType);
				
				participant.addSensorData(nextState);
				
			}

			input.close();
			
		} catch(FileNotFoundException e) {
			
			System.out.println("Error reading connState input file");
			
		}
		
	}

	/**
	 * Loads the gps coordinates and times from the file provided.
	 * @param gpsFile
	 */
	private void loadCoords(String gpsFile) {

		try {
			
			File file = new File(gpsFile);
			Scanner input = new Scanner(file);
			
			/* Read each line and process it:
			 * Create a new coordinate and add it to the participant
			 * Also keep a list of all lat and lng to compute medians
			 */
			
			ArrayList<Double> lats = new ArrayList<Double>();
			ArrayList<Double> lngs = new ArrayList<Double>();
			
			while(input.hasNextLine()) {
				
				String record = input.nextLine();
				
				//Format of record
				//"yyyy/MM/dd HH:mm:ss",lat,lng
				
				String[] fields = record.split(",");
				
				if(fields[0].equals("")) {
					continue;
				}
				
				String timeStamp = fields[0];
				DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Date coordTime = dateFormatter.parse(timeStamp);
								
				float lat = Float.parseFloat(fields[1]);
				float lng = Float.parseFloat(fields[2]);
				
				lats.add((double) lat);
				lngs.add((double) lng);
				
				Coordinate nextCoord = new Coordinate(coordTime, lat, lng);
				
				participant.addSensorData(nextCoord);
				
			}

			input.close();
			
			double[] latArray = new double[lats.size()];
			double[] lngArray = new double[lngs.size()];
			
			for(int i = 0; i < lats.size(); i++) {
				latArray[i] = lats.get(i);
				lngArray[i] = lngs.get(i);
			}
			
			Statistics latStats = new Statistics(latArray);
			double latMedian = latStats.median();
			double latSD = latStats.getStdDev();
			
			Statistics lngStats = new Statistics(lngArray);
			double lngMedian = lngStats.median();
			double lngSD = lngStats.getStdDev();
			
			Coordinate.setMedianLatLng((float) latMedian, (float) lngMedian);
			Coordinate.setSDLatLng((float) latSD, (float) lngSD);
			
		} catch(FileNotFoundException e) {
			
			System.out.println("Error reading gps input file");
			
		} catch (ParseException e) {
			System.out.println("Date formatting error in gps");
			e.printStackTrace();
		}
		
	}

	/**
	 * Loads the app usage data from the given file and adds it to the datamodel.
	 * PRECONDITION: loadTruths has already been called.
	 * @param messagesFile The file to read app usage data from
	 */
	private void loadApps(String appFile) {
		
		//NOTE: We may need to sort by date before calculating the actual durations
		//because the logs seem to have returned usages in a strange order.
		
		try {
			
			File file = new File(appFile);
			Scanner input = new Scanner(file);
			
			/*
			 * Read each line and process it.
			 * Create an AppObject and add it to the list.
			 * Then sort the list.
			 */
			
			class AppObject implements Comparable<AppObject>{
				public Date date;
				public boolean toForeground;
				public String pkg;
				
				public AppObject(String pkg, Date date, boolean toForeground) {
					this.pkg = pkg;
					this.date = date;
					this.toForeground = toForeground;
				}

				@Override
				public int compareTo(AppObject other) {
					return this.date.compareTo(other.date);
				}
			}
			
			List<AppObject> uses= new ArrayList<AppObject>();
			
			while(input.hasNextLine()) {
				
				String record = input.nextLine();
				
				//Format of record
				//"yyyy/MM/dd HH:mm:ss",pkgName,eventType
				
				String[] fields = record.split(",");
				
				if(fields[0].equals("")) {
					continue;
				}
				
				String timeStamp = fields[0];
				DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Date usageTime = dateFormatter.parse(timeStamp);
				
				boolean toForeground = false;
								
				if(fields[2].equals("foreground")) {
					toForeground = true;		
				} else if(!fields[2].equals("background")) {
					System.out.println("Error in AppUsage input file");
				}
				
				AppObject nextObject = new AppObject(fields[1], usageTime, toForeground);
				
				uses.add(nextObject);
			}

			input.close();
			Collections.sort(uses);
			
			/* 
			 * If the AppObject is an app going to the foreground,
			 * add the package name as a key in the hashmap with the time as the value;
			 * If the event is a move to background then check if there is a corresponding
			 * move to the foreground in the hashmap and create an AppUsage to add to the participant
			 */
			HashMap<String, Date> toForeground = new HashMap<String, Date>();
			
			for(AppObject event: uses) {
								
				if(event.toForeground) {
					
					//Add to hashmap
					toForeground.put(event.pkg, event.date);
					
				} else {
					
					//Check for corresponding foreground
					if(toForeground.containsKey(event.pkg)){
						
						//Calculate time difference
						long foregroundTime = event.date.getTime() - toForeground.get(event.pkg).getTime();
						if(foregroundTime < 0) {
							System.out.println("Error in App foreground time calculations");
						}
						
						//Create new AppUsage and add to participant
						AppUsage nextUsage = new AppUsage(event.pkg, toForeground.get(event.pkg), foregroundTime);
						participant.addSensorData(nextUsage);
						//Remove the package from the toForeground map
						toForeground.remove(event.pkg);
						
					}
					
				}		
			}
			
		} catch(FileNotFoundException e) {
			
			System.out.println("Error reading appUsage input file");
			
		} catch (ParseException e) {
			System.out.println("Date formatting error in appUsage");
			e.printStackTrace();
		}
		
	}

	/**
	 * Loads the message data from the given file and adds it to the datamodel.
	 * PRECONDITION: loadTruths has already been called.
	 * @param messagesFile The file to read message data from
	 */
	private void loadMessages(String messagesFile) {
		
		try {
			
			File file = new File(messagesFile);
			Scanner input = new Scanner(file);
			
			/* Read each line and process it:
			 * Create a Message object.
			 * Add it to the participant
			 */
			
			while(input.hasNextLine()) {
				
				String record = input.nextLine();
				
				//Format of record
				//"yyyy/MM/dd HH:mm:ss",inOrOut,phoneNumber,length
				
				String[] fields = record.split(",");
				
				if(fields[0].equals("")) {
					continue;
				}
				
				String timeStamp = fields[0];
				DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Date msgTime = dateFormatter.parse(timeStamp);
								
				boolean incoming = false;
				if(fields[1].equals("incoming")) {
					incoming = true;
				}
				
				String destPhone = fields[2];
				
				int msgLen = 0;
				if(fields.length > 3) {
					msgLen = Integer.parseInt(fields[3]);
				}
				
				Message currentMsg = new Message(msgTime, incoming, destPhone, msgLen);
				
				participant.addSensorData(currentMsg);	
				
			}

			input.close();
			
		} catch(FileNotFoundException e) {
			
			System.out.println("Error reading message input file");
			
		} catch (ParseException e) {
			System.out.println("Date formatting error in messages");
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Loads the call data from the given file and adds it to the data model.
	 * PRECONDITION: loadTruths has already been called.
	 * @param callsFile The file which call data is loaded from
	 */
	private void loadCalls(String callsFile) {
		
		try {
			
			File file = new File(callsFile);
			Scanner input = new Scanner(file);
			String record;
			
			/* Read each line and process it:
			 * Create a Call object.
			 * Add it to the participant
			 */
			
			while(input.hasNextLine()) {
				
				record = input.nextLine();
				
				//Format of record
				//"yyyy/MM/dd HH:mm:ss",PhoneNumber,duration,inOrOut
				//Note: 1 is incoming 2 is outgoing
				
				String[] fields = record.split(",");
				
				if(fields[0].equals("")) {
					continue;
				}
				
								
				String timeStamp = fields[0];
				DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Date callTime = dateFormatter.parse(timeStamp);

				String phoneNumber = fields[1]; 
				int duration = Integer.parseInt(fields[2]);
				String inOrOut = fields[3];
				
				Call currentCall = new Call(callTime, duration, phoneNumber, inOrOut);
				
				participant.addSensorData(currentCall);	
				
			}

			input.close();
			
		} catch(FileNotFoundException e) {
			
			System.out.println("Error reading call input file");
			
		} catch (ParseException e) {
			System.out.println("Date formatting error in calls");
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Loads reviews file after the ratings, giving preference to ratings
	 * and filling in periods that haven't been rated with a review value.
	 * 
	 * @param ratingsFile
	 * @param reviewsFile
	 * 
	 * @precondition loadRatings has already been called
	 */
	private void loadReviews(String reviewsFile) {
		
		try {
			
			File file = new File(reviewsFile);
			Scanner input = new Scanner(file);
			String record;
			
			/* Read each line and process it:
			 * 
			 * Create the new Period.
			 * Save the time and rating of the period.
			 * 
			 * Try to add the review
			 * 
			 */
			
			while(input.hasNextLine()) {
				
				record = input.nextLine();
				
				//Format of record
				//rating,"yyyy/MM/dd HH:mm:ss"
				
				String[] fields = record.split(",");
				
				int rating = Integer.parseInt(fields[0]);
				
				if(threePoint) {
					if(rating == 1 || rating == 2) {
						rating = 1;
					} else if(rating == 3) {
						rating = 2;
					} else {
						rating = 3;
					}
				}
								
				String timeStamp = fields[1];
				DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Date ratingTime;
				
				try {
					
					ratingTime = dateFormatter.parse(timeStamp);
					Period nextPeriod = new Period(ratingTime, rating);
					
					//Note: if this period is too close to those already existing it
					//won't be placed.
					participant.addReviewPeriod(nextPeriod);
					
				} catch (ParseException e) {
					System.out.println("Date formatting error in ratings");
					e.printStackTrace();
				}	
				
			}

			input.close();
			
		} catch(FileNotFoundException e) {
			
			System.out.println("Error reading reviews input file");
			
		}
		
	}

	/**
	 * Loads the ratings from the ratingsFile provided and creates a new Period
	 * for each rating, adding the Period to the DataProcessor's Participant
	 * 
	 * @param ratingsFile
	 */
	private void loadRatings(String ratingsFile) {
		
		try {
			
			File file = new File(ratingsFile);
			Scanner input = new Scanner(file);
			String record;
			
			/* Read each line and process it:
			 * 
			 * Create the new Period.
			 * Save the time and rating of the period.
			 * 
			 * If a rating is within 15 minutes of another
			 * then the second rating is kept and the first discarded.
			 * 
			 */
			
			while(input.hasNextLine()) {
				
				record = input.nextLine();
				
				//Format of record
				//rating,"yyyy/MM/dd HH:mm:ss"
				
				String[] fields = record.split(",");
				
				int rating = Integer.parseInt(fields[0]);
				
				if(threePoint) {
					if(rating == 1 || rating == 2) {
						rating = 1;
					} else if(rating == 3) {
						rating = 2;
					} else {
						rating = 3;
					}
				}
								
				String timeStamp = fields[1];
				DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Date ratingTime;
				
				try {
					
					ratingTime = dateFormatter.parse(timeStamp);
					Period nextPeriod = new Period(ratingTime, rating);
					
					//Note: if this period is within 15 minutes or another it will be discarded
					participant.addPeriod(nextPeriod);
					
				} catch (ParseException e) {
					System.out.println("Date formatting error in ratings");
					e.printStackTrace();
				}	
				
			}

			input.close();
			
		} catch(FileNotFoundException e) {
			
			System.out.println("Error reading ratings input file");
			
		}
		
	}

	public DataProcessor() {
		
		participant = new Participant(0);
		
	}

}
