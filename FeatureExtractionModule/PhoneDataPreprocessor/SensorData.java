import java.util.Date;

/**
 * A parent class for all sensor data objects
 * @author Andrew
 *
 */

public abstract class SensorData {
	
	private Date mDate;

	public SensorData(Date date) {
		mDate = date;
	}
	
	public Date getDate() {
		return mDate;
	}
	
}
