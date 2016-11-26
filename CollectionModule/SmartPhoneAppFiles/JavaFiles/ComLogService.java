package com.emotionsense.demo.data;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.CallLog;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.emotionsense.demo.data.loggers.StoreOnlyUnencryptedFiles;
import com.ubhave.datahandler.except.DataHandlerException;
import com.ubhave.datahandler.loggertypes.AbstractDataLogger;
import com.ubhave.sensormanager.ESException;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Andrew on 19/09/2016.
 */
public class ComLogService extends Service {

    private final static String LOG_TAG = "ComLogService";
    private final static String LAST_COM_LOG_FILE = "last_com_log";

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate()");

        //Find the last time logs were made
        Date lastReviewDate = new Date();
        try {

            File file = new File(getFilesDir(), LAST_COM_LOG_FILE);

            if(!file.exists()) {

                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                date.setTime(1);
                String timestamp = dateFormat.format(date);

                FileOutputStream reviewStream = openFileOutput(LAST_COM_LOG_FILE, Context.MODE_PRIVATE);
                reviewStream.write(timestamp.getBytes());
                reviewStream.close();

            }

            FileInputStream fin = openFileInput(LAST_COM_LOG_FILE);
            DataInputStream in = new DataInputStream(fin);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String dateString = br.readLine();
            Log.d("LastComLog", dateString);

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

        //Get a logger
        AbstractDataLogger logger = null;
        try {
            logger = StoreOnlyUnencryptedFiles.getInstance();
        } catch (ESException e) {
            e.printStackTrace();
        } catch (DataHandlerException e) {
            e.printStackTrace();
        }

        //Do the logging
        logMessages(lastReviewDate.getTime(), logger);
        logCalls(lastReviewDate.getTime(), logger);
        logApps(lastReviewDate.getTime(), logger);

        //Update the time in last logged file
        try {

            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            String timestamp = dateFormat.format(date);

            FileOutputStream reviewStream = openFileOutput(LAST_COM_LOG_FILE, Context.MODE_PRIVATE);
            reviewStream.write(timestamp.getBytes());
            reviewStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        //End the service
        stopSelf();
    }

    private void logApps(long lastLogTime, AbstractDataLogger logger) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            //For this to work you must go to "Settings"->"security"->"apps with usage access"
            //and enable Memotion's access
            UsageStatsManager usm = (UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);

            //Get all events since the last log
            UsageEvents usgEvents = usm.queryEvents(lastLogTime, System.currentTimeMillis());

            UsageEvents.Event nextEvent = new UsageEvents.Event();

            //Loop through the events only recording move to fore or back ground events
            while(usgEvents.hasNextEvent()) {

                usgEvents.getNextEvent(nextEvent);

                String pkgName = nextEvent.getPackageName();

                Long timestamp = nextEvent.getTimeStamp();
                Calendar msgDate = Calendar.getInstance();
                msgDate.setTimeInMillis(timestamp);
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = msgDate.getTime();
                String dateString = dateFormat.format(date);

                String msgData = dateString + "," + pkgName + ",";

                if(nextEvent.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {

                    msgData = msgData + "foreground";
                    logger.log("AppData", msgData);
                    //Log.d("AppData", msgData);

                } else if(nextEvent.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {

                    msgData = msgData + "background";
                    logger.log("AppData", msgData);
                    //Log.d("AppData", msgData);

                }
            }

        }

    }

    private void logCalls(Long time, AbstractDataLogger logger) {

        Cursor cursor = getContentResolver().query(Uri.parse("content://call_log/calls"), null, null, null, null);

        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                Calendar msgDate = Calendar.getInstance();
                int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
                long dateMillis = Long.valueOf(cursor.getString(dateIndex));

                if(dateMillis > time) {

                    msgDate.setTimeInMillis(dateMillis);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = msgDate.getTime();
                    String dateString = dateFormat.format(date);

                    int nameIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                    int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
                    int directionIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);

                    int duration = Integer.valueOf(cursor.getString(durationIndex));

                    String msgData = dateString + "," + cursor.getString(nameIndex) +
                            "," + duration + "," + cursor.getString(directionIndex);

                    logger.log("CallData", msgData);
                    Log.d("CallData", msgData);

                }

            } while (cursor.moveToNext());
        } else {
            // empty log, no calls
        }

    }

    private void logMessages(Long lastReviewDate, AbstractDataLogger logger) {

        List<String> messageList = new ArrayList<String>();
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                Calendar msgDate = Calendar.getInstance();
                long dateMillis = Long.valueOf(cursor.getString(4));

                if(dateMillis > lastReviewDate) {

                    msgDate.setTimeInMillis(dateMillis);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = msgDate.getTime();
                    String dateString = dateFormat.format(date);
                    int msgLen = cursor.getString(12).length();

                    String msgData = dateString + ",incoming," + cursor.getString(2) + "," + msgLen;

                    messageList.add(msgData);

                }

            } while (cursor.moveToNext());
        } else {
            // empty box, no SMS
        }

        cursor = getContentResolver().query(Uri.parse("content://sms/sent"), null, null, null, null);

        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                Calendar msgDate = Calendar.getInstance();
                long dateMillis = Long.valueOf(cursor.getString(4));

                if(dateMillis > lastReviewDate) {

                    msgDate.setTimeInMillis(dateMillis);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = msgDate.getTime();
                    String dateString = dateFormat.format(date);
                    int msgLen = cursor.getString(12).length();

                    String msgData = dateString + ",outgoing," + cursor.getString(2) + "," + msgLen;

                    messageList.add(msgData);

                }

            } while (cursor.moveToNext());
        } else {
            // empty box, no SMS
        }

        Collections.sort(messageList);

        for(String message: messageList) {

            logger.log("SMSData", message);
            Log.d("SMSData", message);

        }

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy()");
    }


    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

}
