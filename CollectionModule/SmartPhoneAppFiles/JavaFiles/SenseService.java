package com.emotionsense.demo.data;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.emotionsense.demo.data.loggers.StoreOnlyUnencryptedFiles;
import com.ubhave.datahandler.except.DataHandlerException;
import com.ubhave.datahandler.loggertypes.AbstractDataLogger;
import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.ESSensorManager;
import com.ubhave.sensormanager.sensors.SensorUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Andrew on 14/09/2016.
 */

public class SenseService extends Service
{
    private final static String LOG_TAG = "SenseService";
    private SenseOnceThread[] pullThreads;

    // TODO: add pull sensors you want to sense once from here
    private final int[] pullSensors = {
            //SensorUtils.SENSOR_TYPE_SMS_CONTENT_READER,
            SensorUtils.SENSOR_TYPE_LOCATION
            //SensorUtils.SENSOR_TYPE_CALL_CONTENT_READER
    };

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate()");
        senseOnceAndStop();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy()");
    }

    private void senseOnceAndStop()
    {
        try
        {
            ESSensorManager sensorManager = ESSensorManager.getSensorManager(this);
            AbstractDataLogger logger = StoreOnlyUnencryptedFiles.getInstance();

            pullThreads = new SenseOnceThread[pullSensors.length];
            for(int i = 0; i < pullSensors.length; i++) {
                pullThreads[i] = new SenseOnceThread(sensorManager, logger, pullSensors[i], this);
                pullThreads[i].start();
            }

            stopSelf();
        }
        catch (ESException e)
        {
            e.printStackTrace();
        } catch (DataHandlerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

}
