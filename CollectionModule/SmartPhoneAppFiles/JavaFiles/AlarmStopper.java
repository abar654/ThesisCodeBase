package com.emotionsense.demo.data;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.Calendar;

public class AlarmStopper extends Service {

    private final static String LOG_TAG = "AlarmStopper";
    private AlarmManager alarmManager;
    private Intent callingIntent;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        callingIntent = intent;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "Stopping notifications");
        stopNotifications();
    }

    private void stopNotifications() {

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(getApplicationContext(), RateNotificationService.class);
        PendingIntent toCancel = PendingIntent.getService(getApplicationContext(), MainActivity.rateNotIntentCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(toCancel);

        //Set the alarm for the following morning
        Calendar morning = Calendar.getInstance();
        morning.setTimeInMillis(System.currentTimeMillis());
        morning.set(Calendar.HOUR_OF_DAY, 8);
        morning.set(Calendar.MINUTE, 30);
        morning.add(Calendar.DATE, 1);

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, morning.getTimeInMillis(), MainActivity.rateNotServiceInterval, toCancel);

        stopSelf();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
