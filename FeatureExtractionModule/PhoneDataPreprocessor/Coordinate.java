import java.util.Date;
import java.util.List;

public class Coordinate extends SensorData {
	
	private float lat;
	private float lng;
	private static float medianLat;
	private static float medianLng;
	private static float sdLat;
	private static float sdLng;

	public Coordinate(Date coordTime, float lat, float lng) {
		
		super(coordTime);
		this.lat = lat;
		this.lng = lng;
		medianLat = 0;
		medianLng = 0;
		
	}
	
	public static void setMedianLatLng(float lat, float lng) {
		medianLat = lat;
		medianLng = lng;
	}
	
	public static void setSDLatLng(float lat, float lng) {
		sdLat = lat;
		sdLng = lng;
	}
	
	public float getLatitude() {
		return lat;
	}
	
	public float getLongitude() {
		return lng;
	}

	public static String computeFeatures(List<Coordinate> coordinates, float periodLength) {
		
		if(coordinates.size() > 0) {
			
			if(DataProcessor.debug) {
				return 	"Distance:" + calcDistance(coordinates, periodLength) + ",\n" +
						"Stats:" + calcLatLngStats(coordinates, periodLength);
			}
			
			//Could also do more features relating to most common places
			return 	calcDistance(coordinates, periodLength) + "," +
					calcLatLngStats(coordinates, periodLength);
		} else {
			
			float sumMean = medianLat + medianLng;
			float sumMedian = sumMean;
			float sumSD = sdLat + sdLng;
			float latRange = sdLat*2;
			float lngRange = sdLng*2;
			
			return 	"0" + "," +
					medianLat + "," +
					sdLat + "," +
					medianLat + "," +
					medianLng + "," +
					sdLng + "," +
					medianLng + "," +
					sumMean + "," +
					sumSD + "," +
					sumMedian + "," +
					latRange + "," +
					lngRange;
		}
	}

	private static String calcDistance(List<Coordinate> coordinates, float periodLength) {
		//Sum the distance between all consecutive points
		double distance = 0;
		for(int i = 0; i < coordinates.size() - 1; i++) {
			double x = coordinates.get(i + 1).getLatitude() - coordinates.get(i).getLatitude();
			double y = coordinates.get(i + 1).getLongitude() - coordinates.get(i).getLongitude();
			distance += Math.sqrt(x*x + y*y);
		}
		
		//Weight the distance by period length
		return "" + distance*1000*60*60/periodLength;
	}

	private static String calcLatLngStats(List<Coordinate> coordinates, float periodLength) {
		//Range, mean, median, SD of Lat, Lng, Sum
		double latMin = coordinates.get(0).getLatitude();
		double latMax = coordinates.get(0).getLatitude();
		double lngMin = coordinates.get(0).getLongitude();
		double lngMax = coordinates.get(0).getLongitude();
		
		//Create arrays of the lats, lngs, and sums
		double[] lats = new double[coordinates.size()];
		double[] lngs = new double[coordinates.size()];
		double[] sums = new double[coordinates.size()];
		
		for(int i = 0; i < coordinates.size(); i++) {
			
			lats[i] = coordinates.get(i).getLatitude();
			lngs[i] = coordinates.get(i).getLongitude();
			sums[i] = lats[i] + lngs[i];
			
			if(lats[i] < latMin) {
				latMin = lats[i];
			} else if(lats[i] > latMax) {
				latMax = lats[i];
			}
			
			if(lngs[i] < lngMin) {
				lngMin = lngs[i];
			} else if(lngs[i] > lngMax) {
				lngMax = lngs[i];
			}
			
		}
		
		Statistics latStats = new Statistics(lats);
		double latMean = latStats.getMean();
		double latSD = latStats.getStdDev();
		double latMedian = latStats.median();
		
		Statistics lngStats = new Statistics(lngs);
		double lngMean = lngStats.getMean();
		double lngSD = lngStats.getStdDev();
		double lngMedian = lngStats.median();
		
		Statistics sumStats = new Statistics(sums);
		double sumMean = sumStats.getMean();
		double sumSD = sumStats.getStdDev();
		double sumMedian = sumStats.median();
		
		double latRange = latMax - latMin;
		double lngRange = lngMax - lngMin;
		
		return 	latMean + "," +
				latSD + "," +
				latMedian + "," +
				lngMean + "," +
				lngSD + "," +
				lngMedian + "," +
				sumMean + "," +
				sumSD + "," +
				sumMedian + "," +
				latRange + "," +
				lngRange;
	}

	public static String featureNames() {
		return 	distanceLabels() + "," +
				latLngStatsLabels();
	}

	private static String latLngStatsLabels() {
		return 	"latMean" + "," +
				"latSD" + "," +
				"latMedian" + "," +
				"lngMean" + "," +
				"lngSD" + "," +
				"lngMedian" + "," +
				"sumMean" + "," +
				"sumSD" + "," +
				"sumMedian" + "," +
				"latRange" + "," +
				"lngRange";
	}

	private static String distanceLabels() {
		return "Distance";
	}

}
