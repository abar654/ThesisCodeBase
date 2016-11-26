import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class AppUsage extends SensorData {
	
	private String packageName;
	private long millisForeground;

	public AppUsage(String string, Date date, long foregroundTime) {
		
		super(date);
		packageName = string;
		millisForeground = foregroundTime;
		
	}
	
	public String getName() {
		return packageName;
	}
	
	public long getMillisForeground() {
		return millisForeground;
	}

	public static String computeFeatures(HashMap<String, List<AppUsage>> appUsages, float periodLength) {
		if(appUsages.size()>0 && periodLength > 0) {

			if(DataProcessor.debug) {
				
				return 	"Number:" + calcNumber(appUsages, periodLength) + ",\n" +
						"Time:" + calcTimeStats(appUsages, periodLength) + ",\n" +
						"Duration:" + calcDurationStats(appUsages, periodLength) + ",\n" +
						"App:" + calcAppStats(appUsages, periodLength) + ",\n" +
						"Social:" + calcSocialMediaStats(appUsages, periodLength);
				
			}
			
			return 	calcNumber(appUsages, periodLength) + "," +
					calcTimeStats(appUsages, periodLength) + "," +
					calcDurationStats(appUsages, periodLength) + "," +
					calcAppStats(appUsages, periodLength) + "," +
					calcSocialMediaStats(appUsages, periodLength);
			
		} else {
			return "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
		}
	}

	private static String calcTimeStats(HashMap<String, List<AppUsage>> appUsages, float periodLength) {
		//SD of times when apps were used
		//(subtract the time of whatever the first usage is
		//this is just to keep the numbers small enough and
		//because we only care about the relative differences this is fine)
		long initialTime = 0;
		for(List<AppUsage> contact: appUsages.values()) {
			initialTime = contact.get(0).getDate().getTime();
			break;
		}
		
		ArrayList<Double> timeList = new ArrayList<Double>();
		double[] individualAppTimeSDs = new double[appUsages.size()];
		
		int j=0;
		
		for(List<AppUsage> app: appUsages.values()) {
			
			double[] currAppTimes = new double[app.size()];
			
			int i = 0;
			for(AppUsage usage: app) {
				
				double currTime = usage.getDate().getTime() - initialTime;
				
				timeList.add(currTime);
				currAppTimes[i] = currTime;
				i++;
				
			}
			
			//Now compute the time SD for this app
			Statistics timeStats = new Statistics(currAppTimes);
			double timeSD = timeStats.getStdDev();
			individualAppTimeSDs[j] = timeSD;
			j++;
			
		}
		
		//Compute the overall timeSD
		double[] timeArray = new double[timeList.size()];
		
		for(int i=0; i < timeList.size(); i++) {
			timeArray[i] = timeList.get(i).doubleValue();
		}
		
		Statistics overallTimeStats = new Statistics(timeArray);
		double overallTimeSD = overallTimeStats.getStdDev();
		
		//Avg of SD for each app individually
		Statistics indivTimeStats = new Statistics(individualAppTimeSDs);
		double meanIndivSDs = indivTimeStats.getMean();
		
		return 	overallTimeSD + "," +
				meanIndivSDs;
	}

	private static String calcSocialMediaStats(HashMap<String, List<AppUsage>> appUsages, float periodLength) {
		
		//Registered social media package names
		String[] smNames = {"com.facebook.orca",
							"com.facebook.katana"
		};
		
		long totalUseDuration = 0;
		long smDurationSum = 0;
		int totalNumUses = 0;
		
		ArrayList<Double> smDurationList = new ArrayList<Double>();
		
		for(List<AppUsage> app: appUsages.values()) {
			
			totalNumUses += app.size();
			
			//Check if app is a social media app
			boolean isSM = false;
			for(String name: smNames) {
				if(app.get(0).getName().equals(name)) {
					isSM = true;
					break;
				}
			}
				
			for(AppUsage usage: app) {
				
				if(isSM) {
					smDurationList.add((double) usage.getMillisForeground());
					smDurationSum += usage.getMillisForeground();
				}
				totalUseDuration += usage.getMillisForeground();
				totalNumUses++;
				
			}
			
		}
		
		double[] smDurationArray = new double[smDurationList.size()];
		
		for(int i=0; i < smDurationList.size(); i++) {
			smDurationArray[i] = smDurationList.get(i).doubleValue();
		}
		
		//Sum, mean, SD, median of duration of social media use
		double durationMean = 0;
		double durationSD = 0;
		double durationMedian = 0;
		if(smDurationArray.length > 0) {
			
			Statistics durationStats = new Statistics(smDurationArray);
			
			durationMean = durationStats.getMean();
			durationSD = durationStats.getStdDev();
			durationMedian = durationStats.median();
			
		}
		
		//Number of times social media used
		int smUses = smDurationList.size();
		
		//Percentages of total num uses, and duration
		
		double percentNumUses = 0;
		if(totalNumUses>0) {
			percentNumUses = 1.0*smUses/totalNumUses;
		}
		double percentDuration = 0;
		if(totalUseDuration > 0) {
			percentDuration = 1.0*smDurationSum/totalUseDuration;
		}
		
		return 	smDurationSum/periodLength + "," +
				smUses/periodLength + "," +
				durationMean + "," +
				durationSD + "," +
				durationMedian + "," +
				percentNumUses + "," +
				percentDuration;
	}

	private static String calcAppStats(HashMap<String, List<AppUsage>> appUsages, float periodLength) {
		//Total number of apps used
		int totalNumber = appUsages.size();
		
		//Number of uses of most used
		int maxToOne = 0;
		
		for(List<AppUsage> app: appUsages.values()) {
			
			if(app.size() > maxToOne) {
				maxToOne = app.size();
			}
			
		}
		
		//Could add further features about the number used in a global "most used" list
		return 	1.0*totalNumber/periodLength + "," + 
				1.0*maxToOne/periodLength;
	}

	private static String calcDurationStats(HashMap<String, List<AppUsage>> appUsages, float periodLength) {
		//Compute for each of All, Incoming and Outgoing
		ArrayList<Double> durationList = new ArrayList<Double>();
		
		//Duration of use of most used app
		double maxDurationOneApp = 0;
		
		for(List<AppUsage> app: appUsages.values()) {
			
			double appDuration = 0;
			
			for(AppUsage usage: app) {
				
				durationList.add((double) usage.getMillisForeground());
				appDuration += usage.getMillisForeground();
				
			}
			
			if(appDuration > maxDurationOneApp) {
				maxDurationOneApp = appDuration;
			}
			
		}
		
		double[] durationArray = new double[durationList.size()];
		
		long durationSum = 0;
		
		for(int i=0; i < durationList.size(); i++) {
			durationSum += durationList.get(i).doubleValue();
			durationArray[i] = durationList.get(i).doubleValue();
		}
		
		//Sum, mean, SD, median
		double durationMean = 0;
		double durationSD = 0;
		double durationMedian = 0;
		if(durationArray.length > 0) {
			
			Statistics durationStats = new Statistics(durationArray);
			
			durationMean = durationStats.getMean();
			durationSD = durationStats.getStdDev();
			durationMedian = durationStats.median();
			
		}
		
		double percentToOne = 0;
		if(durationSum > 0) {
			percentToOne = 1.0*maxDurationOneApp/durationSum;
		}
		
		return 	durationSum/periodLength + "," + 
				durationMean + "," + 
				durationSD + "," + 
				durationMedian + "," + 
				maxDurationOneApp/periodLength + "," +
				percentToOne;
	}

	private static String calcNumber(HashMap<String, List<AppUsage>> appUsages, float periodLength) {

		int totalUsages = 0;

		for(List<AppUsage> app: appUsages.values()) {
			totalUsages += app.size();
		}
		
		return String.valueOf(1.0*totalUsages/periodLength);
	}

	public static String featureNames() {
		return 	numberLabels() + "," +
				timeStatsLabels() + "," +
				durationStatsLabels() + "," +
				appStatsLabels() + "," +
				socialMediaStatsLabels();
	}

	private static String socialMediaStatsLabels() {
		return 	"smDurationSum" + "," +
				"smNumUses" + "," +
				"smDurationMean" + "," +
				"smDurationSD" + "," +
				"smDurationMedian" + "," +
				"percentNumUsesSM" + "," +
				"percentDurationSM";
	}

	private static String appStatsLabels() {
		return 	"totalNumberAppsUsed" + "," + 
				"maxNumUsesOneApp";
	}

	private static String durationStatsLabels() {
		return 	"appDurationSum" + "," + 
				"appDurationMean" + "," + 
				"appDurationSD" + "," + 
				"appDurationMedian" + "," + 
				"maxDurationOneApp" + "," +
				"percentTimeMaxApp";
	}

	private static String timeStatsLabels() {
		return 	"overallAppTimeSD" + "," +
				"meanIndivAppTimeSDs";
	}

	private static String numberLabels() {
		return "totalAppUsages";
	}

}
