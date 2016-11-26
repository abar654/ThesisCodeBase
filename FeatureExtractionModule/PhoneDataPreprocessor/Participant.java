import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Participant {
	
	private List<Period> periods;
	private int id;
	public static final int MAX_DATA_AGE = 3*60*60*1000;

	public Participant(int id) {
		this.id = id;
		periods = new ArrayList<Period>();
	}

	/**
	 * Adds the given Period to the list of Periods
	 * NOTE: If a period already exists within 15 minutes of this one
	 * the earlier of the two will be deleted.
	 * @param currentPeriod
	 */
	public void addPeriod(Period nextPeriod) {

		//Find the first Period in the list that is after nextPeriod
		int i;
		
		for(i = 0; i < periods.size(); i++) {
			
			if(nextPeriod.getDate().before(periods.get(i).getDate())) {
				
				if(nextPeriod.getDate().equals(periods.get(i).getDate())) {
					return;
				}
				
				break;
			}
			
		}
		
		//i is now the index where we want to insert
		if(i < periods.size() && 
				(periods.get(i).getDate().getTime() - nextPeriod.getDate().getTime() < 15*60*1000)) {
			
			//Don't add the nextPeriod because it is too close to the period after
			return;
			
		}
		
		periods.add(i, nextPeriod);
		
		if(i > 0 && 
				(nextPeriod.getDate().getTime() - periods.get(i-1).getDate().getTime() < 15*60*1000)) {
			
			//Remove the Period before this one as it is too close to the nextPeriod
			periods.remove(i-1);
			
		}
		
	}
	
	public void addReviewPeriod(Period nextPeriod) {
		
		//Find the first Period in the list that is after nextPeriod
		int i;
		
		for(i = 0; i < periods.size(); i++) {
			
			if(nextPeriod.getDate().before(periods.get(i).getDate())) {
				
				if(nextPeriod.getDate().equals(periods.get(i).getDate())) {
					return;
				}
				
				break;
			}
			
		}
		
		//i is now the index where we want to insert
		if(i < periods.size() && 
				(periods.get(i).getDate().getTime() - nextPeriod.getDate().getTime() > 45*60*1000)) {
			
			//Next rating is atleast 45 minutes away
			//Now check if prev rating is atleast 45 minutes before
			if(i == 0 || 
					(nextPeriod.getDate().getTime() - periods.get(i-1).getDate().getTime() < 45*60*1000)) {
				
				//We can place at the normal time
				periods.add(i, nextPeriod);
				
			} else {
				
				if(periods.get(i).getDate().getTime() - periods.get(i-1).getDate().getTime() > 90*60*1000) {
					//Set the next period to be the previous period PLUS 45 minutes.
					nextPeriod.setTime(periods.get(i-1).getDate().getTime() + 45*60*1000);
					periods.add(i, nextPeriod);
				}
				
			}
			
		} else if(i == periods.size()) {
			
			if(i==0 || (nextPeriod.getDate().getTime() - periods.get(i-1).getDate().getTime() > 45*60*1000)) {
				periods.add(i, nextPeriod);
			} else {
				//Set the next period to be the previous period PLUS 45 minutes.
				nextPeriod.setTime(periods.get(i-1).getDate().getTime() + 45*60*1000);
				periods.add(i, nextPeriod);
			}
		
		}		
		
	}

	public void addSensorData(SensorData data) {
		
		//Find the correct period i.e. the first period 
		//in the list whose time is after this data's time
		
		//Note: We are currently using the start time and 
		//perhaps it would be more appropriate to use end time
		//for certain data types
		
		for(int i = 0; i < periods.size(); i++) {
			
			if(periods.get(i).getDate().after(data.getDate())) {
				
				//If the time between the data and a period is less than MAX_DATA_AGE, add it
				if(data.getDate().getTime() - periods.get(i).getDate().getTime() < MAX_DATA_AGE) {
					periods.get(i).addSensorData(data);
				}
				
				break;
			}
			
		}
		
	}

	/**
	 * Output the feature vector associated with each period
	 * @param writer
	 */
	public void outputFeatures(BufferedWriter writer) {
		Date prevPeriodDate = null;
		for(Period period: periods) {
			try {
				String featureVector = period.outputFeatures(prevPeriodDate);
				writer.write(featureVector);
				prevPeriodDate = period.getDate();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	public void outputRateTimes(BufferedWriter writer) {
		for(Period period: periods) {
			try {
				writer.write(period.getDate().getTime() + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}

	public void outputFeatureNames(BufferedWriter writer) {
		try {
			String featureNameVector = Period.outputFeatureNames();
			writer.write(featureNameVector);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

}
