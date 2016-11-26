import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Message extends SensorData {
	
	private int length;
	private boolean incoming;
	private String phoneNumber;

	public Message(Date msgTime, boolean incoming, String destPhone, int msgLen) {
		
		super(msgTime);
		this.incoming = incoming;
		length = msgLen;
		phoneNumber = destPhone;
		
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}
	
	public int getLength() {
		return length;
	}
	
	public boolean getIncoming() {
		return incoming;
	}

	public static String computeFeatures(HashMap<String, List<Message>> messages, float periodLength) {
		if(messages.size() > 0) {
			
			if(DataProcessor.debug) {
				
				return 	"Number:" + calcNumber(messages, periodLength) + ",\n" +
						"Time: " + calcTimeStats(messages, periodLength) + ",\n" +
						"Length:" + calcLengthStats(messages, periodLength) + ",\n" +
						"Contact:" + calcContactStats(messages, periodLength);
				
			}
			
			return 	calcNumber(messages, periodLength) + "," +
				calcTimeStats(messages, periodLength) + "," +
				calcLengthStats(messages, periodLength) + "," +
				calcContactStats(messages, periodLength);
			
		} else {
			return "0,0,0.5,0,0,0,0,0,0,0.5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0.5,0";
		}
	}

	private static String calcTimeStats(HashMap<String, List<Message>> messages, float periodLength) {
		//SD of times for messages
		//(subtract the time of whatever the first message is
		//this is just to keep the numbers small enough and
		//because we only care about the relative differences this is fine)
		
		long initialTime = 0;
		for(List<Message> contact: messages.values()) {
			initialTime = contact.get(0).getDate().getTime();
			break;
		}
		
		ArrayList<Double> timeList = new ArrayList<Double>();
		double[] individualTimeSDs = new double[messages.size()];
		
		int j=0;
		
		for(List<Message> contact: messages.values()) {
			
			double[] currContactTimes = new double[contact.size()];
			
			int i = 0;
			for(Message msg: contact) {
				
				double currTime = msg.getDate().getTime() - initialTime;
				
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

	private static String calcContactStats(HashMap<String, List<Message>> msgs, float periodLength) {
		//Total number
		int totalNumber = msgs.size();
		
		//Number incoming
		int numIncoming = 0;
		//Number outgoing
		int numOutgoing = 0;
		//Number to most contacted
		int maxToOne = 0;
		
		for(List<Message> contact: msgs.values()) {
			
			boolean in = false;
			boolean out = false;
			for(Message msg: contact) {
				if(msg.getIncoming()) {
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

	private static String calcLengthStats(HashMap<String, List<Message>> msgs, float periodLength) {
		//Compute for each of All, Incoming and Outgoing
		ArrayList<Double> incomingList = new ArrayList<Double>();
		ArrayList<Double> outgoingList = new ArrayList<Double>();
		
		for(List<Message> contact: msgs.values()) {
			for(Message msg: contact) {
				if(msg.getIncoming()) {
					incomingList.add((double) msg.getLength());
				} else {
					outgoingList.add((double) msg.getLength());
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
		for(List<Message> contact: msgs.values()) {
			
			double contactDuration = 0;
			for(Message msg: contact) {
				contactDuration += msg.getLength();
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

	private static String calcNumber(HashMap<String, List<Message>> msgs, float periodLength) {
		
		//Incoming
		int incomingMsgs = 0;
		//Outgoing
		int outgoingMsgs = 0;
		
		for(List<Message> contact: msgs.values()) {
			for(Message msg: contact) {
				if(msg.getIncoming()) {
					incomingMsgs++;
				} else {
					outgoingMsgs++;
				}
			}			
		}
		
		//Total
		int totalMsgs = incomingMsgs + outgoingMsgs;
		
		//Percentage incoming
		double percentageIncoming = 0;
		if(totalMsgs > 0) {
			percentageIncoming = 1.0*incomingMsgs/totalMsgs;
		}
		
		return 	incomingMsgs/periodLength + "," + 
				outgoingMsgs/periodLength + "," + 
				percentageIncoming + "," + 
				totalMsgs/periodLength;
		
	}

	public static String featureNames() {
		return 	numberLabels() + "," +
				timeStatsLabels() + "," +
				lengthStatsLabels() + "," +
				contactStatsLabels();
	}

	private static String contactStatsLabels() {
		return 	"totalNumberContactsMsged" + "," + 
				"numIncomingContactsMsged" + "," +
				"numOutgoingContactsMsged" + "," + 
				"percentageContactsMsgedIncoming" + "," +
				"maxMsgsToOneContact";
	}

	private static String lengthStatsLabels() {
		return 	"incomingMsgLengthSum" + "," + 
				"outgoingMsgLengthSum" + "," +
				"totalMsgLengthSum" + "," +
				"percentMsgLengthIncoming" + "," +
				"incomingMsgLengthMean" + "," + 
				"incomingMsgLengthSD" + "," + 
				"incomingMsgLengthMedian" + "," + 
				"outgoingMsgLengthMean" + "," + 
				"outgoingMsgLengthSD" + "," + 
				"outgoingMsgLengthMedian" + "," + 
				"totalMsgLengthMean" + "," + 
				"totalMsgLengthSD" + "," + 
				"totalMsgLengthMedian" + "," +
				"maxMsgLengthOneContact" + "," +
				"percentMsgLengthToOne";
	}

	private static String timeStatsLabels() {
		return 	"overallMsgTimesSD" + "," +
				"meanIndivContactMsgTimeSDs";
	}

	private static String numberLabels() {
		return 	"numIncomingMsgs" + "," + 
				"numOutgoingMsgs" + "," + 
				"percentageMsgsIncoming" + "," + 
				"totalNumMsgs";
	}

}
