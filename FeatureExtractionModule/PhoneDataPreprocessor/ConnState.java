import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConnState extends SensorData {
	
	private String type;

	public ConnState(Date stateTime, String networkType) {
		
		super(stateTime);
		type = networkType;
		
	}
	
	public String getType() {
		return type;
	}

	public static String computeFeatures(List<ConnState> connectionStates, float periodLength) {
		if(connectionStates.size()>0 && periodLength > 0) {
			
			//Make a new list that doesn't contain adjacent states which are the same.
			List<ConnState> reducedList = new ArrayList<ConnState>();
			reducedList.add(connectionStates.get(0));
			for(int i = 1; i < connectionStates.size(); i++) {
				if(!connectionStates.get(i).getType().equals(reducedList.get(reducedList.size()-1))) {
					reducedList.add(connectionStates.get(i));
				}
			}
			
			if(DataProcessor.debug) {
				return 	"Time:" + calcTimeStats(reducedList, periodLength) + ",\n" +
						"Number:" + calcNumberAndDurationStats(reducedList, periodLength);
			}
			
			return 	calcTimeStats(reducedList, periodLength) + "," +
					calcNumberAndDurationStats(reducedList, periodLength);
		} else {
			return "0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0";
		}
	}

	private static String calcNumberAndDurationStats(List<ConnState> connectionStates, float periodLength) {
		//Percentage number of times in each state
		int typeNoneOrOther = 0;
		int typeMobile = 0;
		int typeWifi = 0;
		
		for(ConnState conn: connectionStates) {
			if(conn.getType().equals("MOBILE")) {
				typeMobile++;
			} else if(conn.getType().equals("WIFI")) {
				typeWifi++;
			} else {
				typeNoneOrOther++;
			}
		}
		
		int totalTimes = typeNoneOrOther + typeMobile + typeWifi;
		
		//Percentage duration of time spent in each state.
		double[] durationsNoneOrOther = new double[typeNoneOrOther];
		double[] durationsMobile = new double[typeMobile];
		double[] durationsWifi = new double[typeWifi];
		int otherCount = 0;
		int mobileCount = 0;
		int wifiCount = 0;
		double totalDurationNoneOrOther = 0;
		double totalDurationMobile = 0;
		double totalDurationWifi = 0;
		
		for(int i=0; i < connectionStates.size()-1; i++) {
			
			ConnState conn = connectionStates.get(i);
			
			double duration = connectionStates.get(i+1).getDate().getTime() - 
					connectionStates.get(i).getDate().getTime();
			
			if(conn.getType().equals("MOBILE")) {
				durationsMobile[mobileCount++] = duration;
				totalDurationMobile += duration;
			} else if(conn.getType().equals("WIFI")) {
				durationsWifi[wifiCount++] = duration;
				totalDurationWifi += duration;
			} else {
				durationsNoneOrOther[otherCount++] = duration;
				totalDurationNoneOrOther += duration;
			}
		}
		
		//Deal with final entry
		long finalTime = (long) (connectionStates.get(0).getDate().getTime() + periodLength*1000*60);
		ConnState conn = connectionStates.get(connectionStates.size()-1);
		double lastDuration = finalTime - conn.getDate().getTime();
		
		if(conn.getType().equals("MOBILE")) {
			durationsMobile[mobileCount++] = lastDuration;
			totalDurationMobile += lastDuration;
		} else if(conn.getType().equals("WIFI")) {
			durationsWifi[wifiCount++] = lastDuration;
			totalDurationWifi += lastDuration;
		} else {
			durationsNoneOrOther[otherCount++] = lastDuration;
			totalDurationNoneOrOther += lastDuration;
		}
				
		//Sum, Mean, SD, and Median of individual durations spent in each state
		double mobileMean = 0;
		double mobileSD = 0;
		double mobileMedian = 0;
		if(durationsMobile.length > 0) {
			
			Statistics stats = new Statistics(durationsMobile);
			
			mobileMean = stats.getMean();
			mobileSD = stats.getStdDev();
			mobileMedian = stats.median();
			
		}
		
		double wifiMean = 0;
		double wifiSD = 0;
		double wifiMedian = 0;
		if(durationsWifi.length > 0) {
			
			Statistics stats = new Statistics(durationsWifi);
			
			wifiMean = stats.getMean();
			wifiSD = stats.getStdDev();
			wifiMedian = stats.median();
			
		}
		
		double otherMean = 0;
		double otherSD = 0;
		double otherMedian = 0;
		if(durationsNoneOrOther.length > 0) {
			
			Statistics stats = new Statistics(durationsNoneOrOther);
			
			otherMean = stats.getMean();
			otherSD = stats.getStdDev();
			otherMedian = stats.median();
			
		}
		
		return 	typeMobile/totalTimes + "," +
				typeWifi/totalTimes + "," +
				typeNoneOrOther/totalTimes + "," +
				totalDurationNoneOrOther/periodLength + "," +
				totalDurationMobile/periodLength + "," +
				totalDurationWifi/periodLength + "," +
				mobileMean + "," +
				mobileSD + "," +
				mobileMedian + "," +
				wifiMean + "," +
				wifiSD + "," +
				wifiMedian + "," +
				otherMean + "," +
				otherSD + "," +
				otherMedian;
				
	}

	private static String calcTimeStats(List<ConnState> connectionStates, float periodLength) {
		//SD of times of connectionState changes
		//(subtract the time of whatever the first call is
		//this is just to keep the numbers small enough and
		//because we only care about the relative differences this is fine)
		long initialTime = connectionStates.get(0).getDate().getTime();

		double[] timeList = new double[connectionStates.size()];
		
		for(int i = 0; i < connectionStates.size(); i++) {
			timeList[i] = connectionStates.get(i).getDate().getTime() - initialTime;
		}
		
		Statistics timeStats = new Statistics(timeList);
		double timeSD = timeStats.getStdDev();
		
		return 	"" + timeSD;
		
	}

	public static String featureNames() {
		return timeStatsLabels() + "," +
				numberAndDurationStatsLabels();
	}

	private static String numberAndDurationStatsLabels() {
		return	"numTypeMobile" + "," +
				"numTypeWifi" + "," +
				"numTypeNoneOrOther" + "," +
				"totalDurationNoneOrOther" + "," +
				"totalDurationMobile" + "," +
				"totalDurationWifi" + "," +
				"mobileConnDurationMean" + "," +
				"mobileConnDurationSD" + "," +
				"mobileConnDurationMedian" + "," +
				"wifiConnDurationMean" + "," +
				"wifiConnDurationSD" + "," +
				"wifiConnDurationMedian" + "," +
				"otherConnDurationMean" + "," +
				"otherConnDurationSD" + "," +
				"otherConnDurationMedian";
	}

	private static String timeStatsLabels() {
		return "connTimeSD";
	}

}
