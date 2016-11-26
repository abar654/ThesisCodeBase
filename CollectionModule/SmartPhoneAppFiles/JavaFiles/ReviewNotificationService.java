package com.emotionsense.demo.data;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

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

/**
 * Created by Andrew on 30/09/2016.
 */
public class ReviewNotificationService extends Service {

    private final static String LOG_TAG = "ReviewNotifService";

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate()");
        createNotification();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy()");
    }

    private void createNotification() {

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
                stopSelf();
                return;
            }

        }

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle("Memotion")
                        .setContentText("Please submit a daily review");

        notificationBuilder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });
        notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, ReviewActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        resultIntent.putExtra("START_HOUR", startHour);

        //Add the hours after start extra
        resultIntent.putExtra("HOURS_DONE", 0);

        // Create artificial backstack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ReviewActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        //Setting the notification's click behaviour
        notificationBuilder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // REVIEW_CODE allows you to update the notification later on.
        notificationManager.notify(MainActivity.REVIEW_CODE, notificationBuilder.build());

        stopSelf();
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

}