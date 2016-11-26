package com.emotionsense.demo.data;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.ubhave.datahandler.loggertypes.AbstractDataLogger;
import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.ESSensorManager;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.data.pull.LocationData;
import com.ubhave.sensormanager.sensors.SensorUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SenseOnceThread extends Thread
{
	private final int sensorType;
	private final ESSensorManager sensorManager;
	private final AbstractDataLogger logger;
	private final Context parent;
	
	public SenseOnceThread(final ESSensorManager sensorManager, AbstractDataLogger logger, int sensorType, Context parent)
	{
		this.sensorManager = sensorManager;
		this.logger = logger;
		this.sensorType = sensorType;
		this.parent = parent;
	}
	
	@Override
	public void run()
	{
		try
		{
			SensorData data = sensorManager.getDataFromSensor(sensorType);
			if (data != null)
			{
				FileOutputStream outStream = parent.openFileOutput(MainActivity.GPS_FILE_NAME, Context.MODE_APPEND);

				if(sensorType == SensorUtils.SENSOR_TYPE_LOCATION) {

					LocationData loc = (LocationData) data;
					Location coords = loc.getLastLocation();
					if(coords != null) {

						DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
						Date date = new Date();
						String timestamp = dateFormat.format(date);
						String output = timestamp + "," + coords.getLatitude() + "," + coords.getLongitude();

						logger.log("Coordinates", output);

						output = output + "\n";

						outStream.write(output.getBytes());

						Log.d("GPS", coords.toString());
					} else {
						Log.d("GPS", "Dun broked...");
					}

					outStream.close();

				} else {

					logger.logSensorData(data);
					Log.d("Test", "Finished sensing: "+SensorUtils.getSensorName(sensorType));

				}

			}
			else
			{
				Log.d("Test", "Finished sensing: null data");
			}
			
		}
		catch (ESException e)
		{
			Log.d("Test", e.getLocalizedMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
