import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ScreenState extends SensorData {
	
	private long duration;
	private static HashMap<Integer, Long> firstUseOfDay = new HashMap<Integer, Long>();
	private static HashMap<Integer, Long> lastUseOfDay = new HashMap<Integer, Long>();

	public ScreenState(Date date, long duration) {
		super(date);
		this.duration = duration;
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int dayInYear = cal.get(Calendar.DAY_OF_YEAR);
		int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
		
		//If hourOfDay is before 5 then we count this as the day before
		if(hourOfDay < 5) {
			dayInYear--;
		}
		
		//Test if it is the earliest use for the day
		if(firstUseOfDay.containsKey(dayInYear)) {
			if(firstUseOfDay.get(dayInYear) > date.getTime()) {
				firstUseOfDay.put(dayInYear, date.getTime());
			}
		} else {
			firstUseOfDay.put(dayInYear, date.getTime());
		}
		
		//Test if it is the latest use for the day, may include the next day up until 5am.
		if(lastUseOfDay.containsKey(dayInYear)) {
			if(lastUseOfDay.get(dayInYear) < date.getTime()) {
				lastUseOfDay.put(dayInYear, date.getTime());
			}
		} else {
			lastUseOfDay.put(dayInYear, date.getTime());
		}

	}
	
	public long getDuration() {
		return duration;
	}

	public static String computeFeatures(List<ScreenState> screenOns, float periodLength) {
		if(screenOns.size()>0) {
			if(DataProcessor.debug) {
				return 	"Time:" + calcTimeStats(screenOns, periodLength) + ",\n" +
						"Number:" + calcNumberAndDurationStats(screenOns, periodLength);
			}
			return 	calcTimeStats(screenOns, periodLength) + "," +
					calcNumberAndDurationStats(screenOns, periodLength);
		} else {
			return "0,8,8,0,0,0,0,0";
		}
	}

	private static String calcNumberAndDurationStats(List<ScreenState> screenOns, float periodLength) {
		//Percentage number of times in each state
		int numOns = screenOns.size();
		
		//Percentage duration of time spent in each state.
		double[] durationOns = new double[numOns];
		double totalDuration = 0;
		
		for(int i=0; i < screenOns.size(); i++) {
			
			durationOns[i] = screenOns.get(i).getDuration();
			totalDuration += durationOns[i];
			
		}
				
		//Sum, Mean, SD, and Median of time ons			
		Statistics stats = new Statistics(durationOns);
		
		double mean = stats.getMean();
		double stdDev = stats.getStdDev();
		double median = stats.median();
		
		return 	numOns/periodLength + "," +
				totalDuration/periodLength + "," +
				mean + "," +
				stdDev + "," +
				median;
	}

	private static String calcTimeStats(List<ScreenState> screenOns, float periodLength) {
		//SD of times of screenOns
		//(subtract the time of whatever the first call is
		//this is just to keep the numbers small enough and
		//because we only care about the relative differences this is fine)
		long initialTime = screenOns.get(0).getDate().getTime();

		double[] timeList = new double[screenOns.size()];
		
		for(int i = 0; i < screenOns.size(); i++) {
			timeList[i] = screenOns.get(i).getDate().getTime() - initialTime;
		}
		
		Statistics timeStats = new Statistics(timeList);
		double timeSD = timeStats.getStdDev();
		
		//Time of first phone usage on this day
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(initialTime);
		int dayInYear = cal.get(Calendar.DAY_OF_YEAR);
		int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
		
		//If hourOfDay is before 5 then we count this as the day before
		if(hourOfDay < 5) {
			dayInYear--;
		}
		
		cal.setTimeInMillis(firstUseOfDay.get(dayInYear));
		int hourOfFirstUsage = cal.get(Calendar.HOUR_OF_DAY);
		
		//Estimated amount of sleep
		double hoursSlept = 8;
		if(lastUseOfDay.containsKey(dayInYear-1)) {
			hoursSlept = (firstUseOfDay.get(dayInYear)-lastUseOfDay.get(dayInYear-1))/(1000*60*60);
		}
		
		return	timeSD + "," +
				hourOfFirstUsage + "," +
				hoursSlept;
	}

	public static String featureNames() {
		return	timeStatsLabels() + "," +
				numberAndDurationStatsLabels();
	}

	private static String numberAndDurationStatsLabels() {
		return 	"numScreenOns" + "," +
				"totalScreenOnDuration" + "," +
				"screenOnDurationMean" + "," +
				"screenOnDurationStdDev" + "," +
				"screenOnDurationMedian";
	}

	private static String timeStatsLabels() {
		return 	"screenTimeSD" + "," +
				"hourOfFirstUsage" + "," +
				"hoursSlept";
	}

}
