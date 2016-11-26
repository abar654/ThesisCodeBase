import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Call extends SensorData {
	
	private String phoneNumber;
	private int duration;
	private boolean incoming;

	public Call(Date callTime, int duration, String phoneNumber, String inOrOut) {
		
		super(callTime);
		this.phoneNumber = phoneNumber;
		this.duration = duration;
		incoming = true;
		//Treating both incoming AND missed calls as incoming
		//Could extend this to include missed calls but there are so few that
		//it is unlikely to make any difference in classification
		if(inOrOut.equals("2")) {
			incoming = false;
		}
		
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}
	
	public int getDuration() {
		return duration;
	}
	
	public boolean getIncoming() {
		return incoming;
	}

	public static String computeFeatures(HashMap<String, List<Call>> calls, float periodLength) {
		if(calls.size()>0) {
			
			if(DataProcessor.debug) {
				
				//Debug return value			
				return "Number:" + calcNumber(calls, periodLength) + ",\n" +
				"Time:" +calcTimeStats(calls, periodLength) + ",\n" +
				"Duration:" + calcDurationStats(calls, periodLength) + ",\n" +
				"Contact:" + calcContactStats(calls, periodLength);
				
			}
			
			return calcNumber(calls, periodLength) + "," +
				calcTimeStats(calls, periodLength) + "," +
				calcDurationStats(calls, periodLength) + "," +
				calcContactStats(calls, periodLength);
			
		} else {
			return "0,0,0.5,0,0,0,0,0,0,0.5,0,0,0,0,0,0,0,0,0,0,0.5,0,0,0,0.5,0";
		}
	}

	private static String calcTimeStats(HashMap<String, List<Call>> calls, float periodLength) {
		//SD of times for calls
		//(subtract the time of whatever the first call is
		//this is just to keep the numbers small enough and
		//because we only care about the relative differences this is fine)
		long initialTime = 0;
		for(List<Call> contact: calls.values()) {
			initialTime = contact.get(0).getDate().getTime();
			break;
		}
		
		ArrayList<Double> timeList = new ArrayList<Double>();
		double[] individualTimeSDs = new double[calls.size()];
		
		int j=0;
		
		for(List<Call> contact: calls.values()) {
			
			double[] currContactTimes = new double[contact.size()];
			
			int i = 0;
			for(Call call: contact) {
				
				double currTime = call.getDate().getTime() - initialTime;
				
				timeList.add(currTime);
				currContactTimes[i] = currTime;
				i++;
				
			}
			
			//Now compute the time SD for this contact
			Statistics timeStats = new Statistics(currContactTimes);
			double timeSD = timeStats.getStdDev();
			individualTimeSDs[j] = timeSD;
			j++;
			
		}
		
		//Compute the overall timeSD 
		double[] timeArray = new double[timeList.size()];
		
		for(int i=0; i < timeList.size(); i++) {
			timeArray[i] = timeList.get(i).doubleValue();
		}
		
		Statistics overallTimeStats = new Statistics(timeArray);
		double overallTimeSD = overallTimeStats.getStdDev();
		
		//Avg of SD for each contact individually
		Statistics indivTimeStats = new Statistics(individualTimeSDs);
		double meanIndivSDs = indivTimeStats.getMean();
		
		return 	overallTimeSD + "," +
				meanIndivSDs;
	}

	private static String calcContactStats(HashMap<String, List<Call>> calls, float periodLength) {
		//Total number
		int totalNumber = calls.size();
		
		//Number incoming
		int numIncoming = 0;
		//Number outgoing
		int numOutgoing = 0;
		//Number to most contacted
		int maxToOne = 0;
		
		for(List<Call> contact: calls.values()) {
			
			boolean in = false;
			boolean out = false;
			for(Call call: contact) {
				if(call.getIncoming()) {
					in = true;
				} else {
					out = true;
				}
			}
			
			if(in) {
				numIncoming++;
			}
			if(out) {
				numOutgoing++;
			}
			
			if(contact.size() > maxToOne) {
				maxToOne = contact.size();
			}
			
		}
		
		//Percentage incoming
		double percentageIncoming = 0;
		if(totalNumber > 0) {
			percentageIncoming = 1.0*numIncoming/totalNumber;
		}
		
		//Could add further features about the number contacted in the global "most contacted" list
		return 	1.0*totalNumber/periodLength + "," + 
				1.0*numIncoming/periodLength + "," +
				1.0*numOutgoing/periodLength + "," + 
				percentageIncoming + "," +
				1.0*maxToOne/periodLength;
	}

	private static String calcNumber(HashMap<String, List<Call>> calls, float periodLength) {

		//Incoming
		int incomingCalls = 0;
		//Outgoing
		int outgoingCalls = 0;
		
		for(List<Call> contact: calls.values()) {
			for(Call call: contact) {
				if(call.getIncoming()) {
					incomingCalls++;
				} else {
					outgoingCalls++;
				}
			}			
		}
		
		//Total
		int totalCalls = incomingCalls + outgoingCalls;
		
		//Percentage incoming
		double percentageIncoming = 0;
		if(totalCalls > 0) {
			percentageIncoming = 1.0*incomingCalls/totalCalls;
		}
		
		return 	incomingCalls/periodLength + "," + 
				outgoingCalls/periodLength + "," + 
				percentageIncoming + "," + 
				totalCalls/periodLength;
	}

	private static String calcDurationStats(HashMap<String, List<Call>> calls, float periodLength) {
		//Compute for each of All, Incoming and Outgoing
		ArrayList<Double> incomingList = new ArrayList<Double>();
		ArrayList<Double> outgoingList = new ArrayList<Double>();
		
		for(List<Call> contact: calls.values()) {
			for(Call call: contact) {
				if(call.getIncoming()) {
					incomingList.add((double) call.getDuration());
				} else {
					outgoingList.add((double) call.getDuration());
				}
			}			
		}
		
		double[] incomingArray = new double[incomingList.size()];
		double[] outgoingArray = new double[outgoingList.size()];
		double[] totalArray = new double[incomingList.size() + outgoingList.size()];
		
		double incomingSum = 0;
		double outgoingSum = 0;
		
		for(int i=0; i < incomingList.size(); i++) {
			incomingSum += incomingList.get(i).doubleValue();
			incomingArray[i] = incomingList.get(i).doubleValue();
			totalArray[i] = incomingList.get(i).doubleValue();
		}
		
		for(int i=0; i < outgoingList.size(); i++) {
			outgoingSum += outgoingList.get(i).doubleValue();
			outgoingArray[i] = outgoingList.get(i).doubleValue();
			totalArray[incomingList.size() + i] = outgoingList.get(i).doubleValue();
		}
		
		double totalSum = incomingSum + outgoingSum;
		
		//Sum, mean, SD, median
		
		double incomingMean = 0;
		double incomingSD = 0;
		double incomingMedian = 0;
		if(incomingArray.length > 0) {
			
			Statistics incomingStats = new Statistics(incomingArray);
			
			incomingMean = incomingStats.getMean();
			incomingSD = incomingStats.getStdDev();
			incomingMedian = incomingStats.median();
			
		}
		
		double outgoingMean = 0;
		double outgoingSD = 0;
		double outgoingMedian = 0;
		if(outgoingArray.length > 0) {
			
			Statistics outgoingStats = new Statistics(outgoingArray);
			
			outgoingMean = outgoingStats.getMean();
			outgoingSD = outgoingStats.getStdDev();
			outgoingMedian = outgoingStats.median();
			
		}
		
		double totalMean = 0;
		double totalSD = 0;
		double totalMedian = 0;
		if(totalArray.length > 0) {
			
			Statistics totalStats = new Statistics(totalArray);
			
			totalMean = totalStats.getMean();
			totalSD = totalStats.getStdDev();
			totalMedian = totalStats.median();
			
		}
		
		//Duration to most contacted
		double maxDurationOneContact = 0;
		for(List<Call> contact: calls.values()) {
			
			double contactDuration = 0;
			for(Call call: contact) {
				contactDuration += call.getDuration();
			}
			
			if(contactDuration > maxDurationOneContact) {
				maxDurationOneContact = contactDuration;
			}
		}		
		
		double percentIn = 0;
		if(totalSum > 0) {
			percentIn = 1.0*incomingSum/totalSum;
		}
		
		double percentToOne = 0;
		if(totalSum > 0) {
			percentToOne = 1.0*maxDurationOneContact/totalSum;
		}
		
		return 	incomingSum/periodLength + "," + 
				outgoingSum/periodLength + "," +
				totalSum/periodLength + "," +
				percentIn + "," +
				incomingMean + "," + 
				incomingSD + "," + 
				incomingMedian + "," + 
				outgoingMean + "," + 
				outgoingSD + "," + 
				outgoingMedian + "," + 
				totalMean + "," + 
				totalSD + "," + 
				totalMedian + "," +
				maxDurationOneContact/periodLength + "," +
				percentToOne;
	}

	public static String featureNames() {
		
		return numberLabels() + "," +
				timeStatsLabels() + "," +
				durationStatsLabels() + "," +
				contactStatsLabels();
	}

	private static String contactStatsLabels() {
		return 	"totalNumberContactsCalled" + "," + 
				"numIncomingContactsCalled" + "," +
				"numOutgoingContactsCalled" + "," + 
				"percentageIncomingContactsCalled" + "," +
				"maxCallsToOneContact";
	}

	private static String durationStatsLabels() {
		return 	"incomingCallDurationSum" + "," + 
				"outgoingCallDurationSum" + "," +
				"totalCallDurationSum" + "," +
				"percentCallDurationIncoming" + "," +
				"incomingCallDurationMean" + "," + 
				"incomingCallDurationSD" + "," + 
				"incomingCallDurationMedian" + "," + 
				"outgoingCallDurationMean" + "," + 
				"outgoingCallDurationSD" + "," + 
				"outgoingCallDurationMedian" + "," + 
				"totalCallDurationMean" + "," + 
				"totalCallDurationSD" + "," + 
				"totalCallDurationMedian" + "," +
				"maxCallDurationOneContact" + "," +
				"percentCallDurationToOne";
	}

	private static String timeStatsLabels() {
		return 	"callsOverallTimeSD" + "," +
				"callsMeanIndivSDs";
	}

	private static String numberLabels() {
		return "numIncomingCalls" + "," + 
				"numOutgoingCalls" + "," + 
				"percentageIncomingCalls" + "," + 
				"totalNumCalls";
	}

}
