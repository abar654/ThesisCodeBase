package com.emotionsense.demo.data;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.emotionsense.demo.data.loggers.StoreOnlyUnencryptedFiles;
import com.ubhave.datahandler.ESDataManager;
import com.ubhave.datahandler.except.DataHandlerException;
import com.ubhave.datahandler.loggertypes.AbstractDataLogger;
import com.ubhave.datahandler.transfer.DataUploadCallback;
import com.ubhave.sensormanager.ESSensorManager;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.sensors.SensorUtils;

public class MainActivity extends Activity implements DataUploadCallback
{
	private final static String LOG_TAG = "MainActivity";
	public final static int RATE_CODE = 1;
	public final static int REVIEW_CODE = 2;
	public final static String GPS_FILE_NAME = "gps_log";
	public static final String REVIEW_FILE_NAME = "last_review_time";

	private AbstractDataLogger logger;
	private ESSensorManager sensorManager;
	
	private SubscribeThread[] subscribeThreads;

	// TODO: add push sensors you want to sense from here
	private final int[] pushSensors = { SensorUtils.SENSOR_TYPE_SCREEN, SensorUtils.SENSOR_TYPE_CONNECTION_STATE};

	//Fields for the rate notification alarm
	public static final long rateNotServiceInterval = 60 * 60 * 1000L;
	public static final int rateNotIntentCode = 1;
	private AlarmManager alarmManager;
	private PendingIntent rateNotOperation;

	//Fields for the stopNotifications alarm
	private final int stopIntentCode = 2;
	private PendingIntent stopOperation;

	//Fields for the sense alarm
	private final int senseIntentCode = 3;
	private PendingIntent senseOperation;
	public static final long senseInterval = 2 * 60 * 1000L;

	//Fields for the comLog alarm
	private final int comLogIntentCode = 4;
	private PendingIntent comLogOperation;
	public static final long comLogInterval = 4 * 60 * 60 * 1000L;

	//Fields for the review notification alarm
	public static final long revNotServiceInterval = 24* 60 * 60 * 1000L;
	public static final int revNotIntentCode = 5;
	private PendingIntent revNotOperation;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		startSensing();

		final Activity parent = this;
		Button rateButton = (Button) findViewById(R.id.rate_button);
		rateButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Open the rate screen
				Intent intent = new Intent(parent, RateActivity.class);
				startActivityForResult(intent, RATE_CODE);
			}
		});

		Button reviewButton = (Button) findViewById(R.id.review_button);
		reviewButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {

				Date lastReviewDate = new Date();
				try {

					File file = new File(getFilesDir(), MainActivity.REVIEW_FILE_NAME);

					if(!file.exists()) {

						DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
						Date date = new Date();
						date.setTime(1);
						String timestamp = dateFormat.format(date);

						FileOutputStream reviewStream = openFileOutput(MainActivity.REVIEW_FILE_NAME, Context.MODE_PRIVATE);
						reviewStream.write(timestamp.getBytes());
						reviewStream.close();

					}

					FileInputStream fin = openFileInput(MainActivity.REVIEW_FILE_NAME);
					DataInputStream in = new DataInputStream(fin);
					BufferedReader br = new BufferedReader(new InputStreamReader(in));
					String dateString = br.readLine();
					Log.d("LastReview", dateString);

					in.close();
					fin.close();

					DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					lastReviewDate = df.parse(dateString);

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}

				//Figure out what time the person started using their phone
				//Or what time they last logged
				//24h time, assume 8 am for now
				int startHour = 8;
				Calendar defaultStart = Calendar.getInstance();
				defaultStart.setTimeInMillis(System.currentTimeMillis());

				int currentHour = defaultStart.get(Calendar.HOUR_OF_DAY);
				if(currentHour < 8) {
					defaultStart.set(Calendar.DAY_OF_YEAR, defaultStart.get(Calendar.DAY_OF_YEAR) - 1);
					currentHour = 23;
				}

				defaultStart.set(Calendar.HOUR_OF_DAY, startHour);
				defaultStart.set(Calendar.MINUTE, 0);
				defaultStart.set(Calendar.SECOND, 0);

				Calendar lastReview = Calendar.getInstance();
				lastReview.setTime(lastReviewDate);

				if(lastReview.after(defaultStart)) {
					startHour = lastReview.get(Calendar.HOUR_OF_DAY) + 1;

					if(startHour >= currentHour) {
						//Make a toast to tell the user there are no pending reviews
						Toast.makeText(MainActivity.this, "Reviews up to date: You do not have any pending reviews, "
								+ "wait until we have collected more data and try again later.", Toast.LENGTH_LONG).show();
						return;
					}

				}

				//Set up the first review screen
				Intent intent = new Intent(parent, ReviewActivity.class);

				intent.putExtra("START_HOUR", startHour);

				//Add the hours after start extra
				intent.putExtra("HOURS_DONE", 0);

				//Open the review screen
				startActivity(intent);
			}
		});

		//Set up the alarms for notifications and pull sensors
		setAlarms();

	}

	private void startSensing() {
		try
		{
			logger = StoreOnlyUnencryptedFiles.getInstance();
			sensorManager = ESSensorManager.getSensorManager(this);

			//Start the pushSensors sensing
			subscribeThreads = new SubscribeThread[pushSensors.length];
			for (int i = 0; i < pushSensors.length; i++)
			{
				subscribeThreads[i] = new SubscribeThread(this, sensorManager, logger, pushSensors[i]);
				subscribeThreads[i].start();
			}
		}
		catch (Exception e)
		{
			Toast.makeText(this, "" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			Log.d(LOG_TAG, e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	public void setAlarms() {

		//Set alarm to start at 8:30am
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.set(Calendar.HOUR_OF_DAY, 8);
		cal.set(Calendar.MINUTE, 30);

		alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(getApplicationContext(), RateNotificationService.class);
		rateNotOperation = PendingIntent.getService(getApplicationContext(), rateNotIntentCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		alarmManager.cancel(rateNotOperation); // in case it is already running
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + rateNotServiceInterval, rateNotServiceInterval, rateNotOperation);

		//Set alarm to sense from pull sensors
		Intent senseIntent = new Intent(getApplicationContext(), SenseService.class);
		senseOperation = PendingIntent.getService(getApplicationContext(), senseIntentCode, senseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.cancel(senseOperation); // in case it is already running
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), senseInterval, senseOperation);

		//Set alarm to log call, app and message communication logs
		Intent comLogIntent = new Intent(getApplicationContext(), ComLogService.class);
		comLogOperation = PendingIntent.getService(getApplicationContext(), comLogIntentCode, comLogIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.cancel(comLogOperation); // in case it is already running
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), comLogInterval, comLogOperation);

		//Set an alarm to create the review notification
		cal.set(Calendar.HOUR_OF_DAY, 22);

		Intent revNotIntent = new Intent(getApplicationContext(), ReviewNotificationService.class);
		revNotOperation = PendingIntent.getService(getApplicationContext(), revNotIntentCode, revNotIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.cancel(revNotOperation);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), revNotServiceInterval, revNotOperation);

		//Set an alarm to cancel the notifications after 23:30
		cal.set(Calendar.HOUR_OF_DAY, 23);

		Intent notStopIntent = new Intent(getApplicationContext(), AlarmStopper.class);
		stopOperation = PendingIntent.getService(getApplicationContext(), stopIntentCode, notStopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.cancel(stopOperation); // in case it is already running
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, stopOperation);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == RATE_CODE) {
			String feeling = "No rating";

			switch (resultCode) {
				case 1:
					feeling = "Very unhappy";
					break;
				case 2:
					feeling = "Unhappy";
					break;
				case 3:
					feeling = "Neutral";
					break;
				case 4:
					feeling = "Happy";
					break;
				case 5:
					feeling = "Very happy";
					break;
			}

			Toast.makeText(this, "Rating recorded: " + feeling + "\nSubmit again to override", Toast.LENGTH_LONG).show();

		}

		//Update the call and message logs

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Don't forget to stop sensing when the app terminates
		for (SubscribeThread thread : subscribeThreads)
		{
			thread.stopSensing();
		}

	}

	@Override
	public void onResume()
	{
		super.onResume();
	}

	@Override
	public void onPause()
	{
		super.onPause();

	}
/*
	public void onSearchClicked(final View view)
	{
		// Counts the number of sensor events from the last 60 seconds
		try
		{
			long startTime = System.currentTimeMillis() - (1000L * 60);
			ESDataManager dataManager = logger.getDataManager();
			
			for (int pushSensor : pushSensors)
			{
				List<SensorData> recentData = dataManager.getRecentSensorData(pushSensor, startTime);
				Toast.makeText(this, "Recent "+SensorUtils.getSensorName(pushSensor)+": " + recentData.size(), Toast.LENGTH_LONG).show();
			}
			
			for (int pushSensor : pullSensors)
			{
				List<SensorData> recentData = dataManager.getRecentSensorData(pushSensor, startTime);
				Toast.makeText(this, "Recent "+SensorUtils.getSensorName(pushSensor)+": " + recentData.size(), Toast.LENGTH_LONG).show();
			}
		}
		catch (Exception e)
		{
			Toast.makeText(this, "Error retrieving sensor data", Toast.LENGTH_LONG).show();
			Log.d(LOG_TAG, e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	public void onFlushClicked(final View view)
	{
		// Tries to POST all of the stored sensor data to the server
		try
		{
			ESDataManager dataManager = logger.getDataManager();
			dataManager.postAllStoredData(this);
		}
		catch (DataHandlerException e)
		{
			Toast.makeText(this, "Exception: "+e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			Log.d(LOG_TAG, ""+e.getLocalizedMessage());
		}
	}
*/
	@Override
	public void onDataUploaded()
	{
		runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				// Callback method: the data has been successfully posted
				Toast.makeText(MainActivity.this, "Data transferred.", Toast.LENGTH_LONG).show();
			}
		});
	}
	
	@Override
	public void onDataUploadFailed()
	{
		runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				// Callback method: the data has not been successfully posted
				Toast.makeText(MainActivity.this, "Error transferring data", Toast.LENGTH_LONG).show();
			}
		});
	}
}
