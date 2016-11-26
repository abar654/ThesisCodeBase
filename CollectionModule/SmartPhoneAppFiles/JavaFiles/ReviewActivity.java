package com.emotionsense.demo.data;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CallLog;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.emotionsense.demo.data.loggers.StoreOnlyUnencryptedFiles;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ubhave.datahandler.config.DataStorageConstants;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ReviewActivity extends Activity implements OnMapReadyCallback {

    private AbstractDataLogger logger;
    private final static String LOG_TAG = "ReviewActivity";
    protected MapView mMapView;
    private LatLng myLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        try {
            logger = StoreOnlyUnencryptedFiles.getInstance();
        } catch (ESException e) {
            e.printStackTrace();
        } catch (DataHandlerException e) {
            e.printStackTrace();
        }

        //Set up the progress bar
        ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar);
        //Figure out how many hours need to be logged, set as max
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());

        int currentHour = cal.get(Calendar.HOUR_OF_DAY);
        //If this is after midnight and before 8am
        if(currentHour < 9) {
            //Set cal to be the previous day
            cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) - 1);
            //Set current hour to be 23
            currentHour = 23;
        }

        final int finalHour = currentHour;
        final int startHour = getIntent().getIntExtra("START_HOUR", 7);
        progress.setMax(finalHour-startHour);

        //Set the HOURS_DONE as progress
        final int hoursDone = getIntent().getIntExtra("HOURS_DONE", 2);
        progress.setProgress(hoursDone);

        //Set up all the text to be the correct date and time
        TextView dateTitle = (TextView) findViewById(R.id.title_textView);
        DateFormat titleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date titleDate = cal.getTime();
        dateTitle.setText("Daily Review " + titleDateFormat.format(titleDate));

        TextView timeText = (TextView) findViewById(R.id.time_textView);
        int from = startHour + hoursDone;
        int to = from + 1;
        timeText.setText(from + ":00 to " + to + ":00");

        cal.set(Calendar.HOUR_OF_DAY, from);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long startTime = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, to);
        long endTime = cal.getTimeInMillis();

        //Set up the map

        myLocation = new LatLng(-33.916572, 151.228682);

        try {

            FileInputStream fin = openFileInput(MainActivity.GPS_FILE_NAME);
            DataInputStream in = new DataInputStream(fin);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            while ((strLine = br.readLine()) != null) {
                String[] tokens = strLine.split(",");

                DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date startDate = null;
                try {
                    startDate = df.parse(tokens[0]);
                    String newDateString = df.format(startDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if(startDate != null && startDate.getTime() > startTime && startDate.getTime() <= endTime) {

                    myLocation = new LatLng(Double.valueOf(tokens[1]), Double.valueOf(tokens[2]));
                    break;

                }

            }
            in.close();
            fin.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMapView = (MapView) findViewById(R.id.embedded_mapview);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        //Set up message list

        List<String> messageList = new ArrayList<String>();
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                Calendar msgDate = Calendar.getInstance();
                long dateMillis = Long.valueOf(cursor.getString(4));

                if(dateMillis > startTime && dateMillis <= endTime) {

                    msgDate.setTimeInMillis(dateMillis);
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm");
                    Date date = msgDate.getTime();
                    String dateString = dateFormat.format(date);

                    String msgData = dateString + " - From: " + cursor.getString(2) + " - " + cursor.getString(12);
                    if (msgData.length() > 50) {
                        msgData = msgData.substring(0, 50) + "...";
                    }

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

                if(dateMillis > startTime && dateMillis <= endTime) {

                    msgDate.setTimeInMillis(dateMillis);
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm");
                    Date date = msgDate.getTime();
                    String dateString = dateFormat.format(date);

                    String msgData = dateString + " - To: " + cursor.getString(2) + " - " + cursor.getString(12);
                    if (msgData.length() > 50) {
                        msgData = msgData.substring(0, 50) + "...";
                    }

                    messageList.add(msgData);

                }

            } while (cursor.moveToNext());
        } else {
            // empty box, no SMS
        }

        Collections.sort(messageList);

        ArrayAdapter<String> messageAdapter = new ArrayAdapter<String>(
                this,
                R.layout.list_item_messages,
                R.id.list_item_messages_textview,
                messageList
        );

        ListView messageListView = (ListView) findViewById(R.id.listview_messages);
        messageListView.setAdapter(messageAdapter);
        //So that the scroll view doesn't automatically jump down to the listView
        messageListView.setFocusable(false);

        //Set up the call list
        List<String> callList = new ArrayList<String>();
        cursor = getContentResolver().query(Uri.parse("content://call_log/calls"), null, null, null, null);

        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                Calendar msgDate = Calendar.getInstance();
                int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
                long dateMillis = Long.valueOf(cursor.getString(dateIndex));

                if(dateMillis > startTime && dateMillis <= endTime) {

                    msgDate.setTimeInMillis(dateMillis);
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm");
                    Date date = msgDate.getTime();
                    String dateString = dateFormat.format(date);

                    int nameIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                    int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);

                    int duration = Integer.valueOf(cursor.getString(durationIndex));
                    int mins = duration / 60;
                    int secs = duration % 60;

                    String msgData = dateString + " - " + cursor.getString(nameIndex) + " for " + mins + "m" + secs + "s";
                    if (msgData.length() > 50) {
                        msgData = msgData.substring(0, 50) + "...";
                    }

                    callList.add(msgData);

                }

            } while (cursor.moveToNext());
        } else {
            // empty log, no calls
        }

        ArrayAdapter<String> callAdapter = new ArrayAdapter<String>(
                this,
                R.layout.list_item_messages,
                R.id.list_item_messages_textview,
                callList
        );

        ListView callListView = (ListView) findViewById(R.id.listview_calls);
        callListView.setAdapter(callAdapter);
        //So that the scroll view doesn't automatically jump down to the listView
        callListView.setFocusable(false);

        //Set up the app list
        List<String> appList = new ArrayList<String>();

        //Get the list of package names used in this interval
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            //For this to work you must go to "Settings"->"security"->"apps with usage access"
            //and enable Memotion's access
            UsageStatsManager usm = (UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);

            //Get all events in this period
            UsageEvents usgEvents = usm.queryEvents(startTime, endTime);

            UsageEvents.Event nextEvent = new UsageEvents.Event();

            //Loop through the events adding only foregrounded apps
            while(usgEvents.hasNextEvent()) {

                usgEvents.getNextEvent(nextEvent);

                if(nextEvent.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {

                    String pkgName = nextEvent.getPackageName();
                    if(!appList.contains(pkgName)) {
                        appList.add(pkgName);
                    }

                }
            }
        }

        //Put list into adapter to be displayed
        ArrayAdapter<String> appAdapter = new ArrayAdapter<String>(
                this,
                R.layout.list_item_messages,
                R.id.list_item_messages_textview,
                appList
        );

        ListView appListView = (ListView) findViewById(R.id.listview_apps);
        appListView.setAdapter(appAdapter);
        //So that the scroll view doesn't automatically jump down to the listView
        appListView.setFocusable(false);

        //Setup the rate buttons
        final Activity parent = this;
        ImageButton.OnClickListener listener = new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Set up date to log correctly
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(System.currentTimeMillis());

                int currentHour = cal.get(Calendar.HOUR_OF_DAY);
                //If this is after midnight and before 8am
                if(currentHour < 9) {
                    //Set cal to be the previous day
                    cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) - 1);
                }

                cal.set(Calendar.HOUR_OF_DAY, startHour + hoursDone);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);

                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = cal.getTime();
                String timestamp = dateFormat.format(date);

                //Log the date and rating
                switch (v.getId()) {
                    case R.id.reviewbutton1:
                        logger.log(LOG_TAG, "1,"+timestamp);
                        break;
                    case R.id.reviewbutton2:
                        logger.log(LOG_TAG, "2,"+timestamp);
                        break;
                    case R.id.reviewbutton3:
                        logger.log(LOG_TAG, "3,"+timestamp);
                        break;
                    case R.id.reviewbutton4:
                        logger.log(LOG_TAG, "4,"+timestamp);
                        break;
                    case R.id.reviewbutton5:
                        logger.log(LOG_TAG, "5,"+timestamp);
                        break;
                }

                //Update the last_review_time to be timestamp
                try {
                    FileOutputStream reviewStream = openFileOutput(MainActivity.REVIEW_FILE_NAME, Context.MODE_PRIVATE);
                    reviewStream.write(timestamp.getBytes());
                    reviewStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Check if this is the last review
                if(startHour + hoursDone + 1 < finalHour) {

                    //Now send the intent for the next review
                    Intent intent = new Intent(parent, ReviewActivity.class);

                    //Start hour remains the same
                    intent.putExtra("START_HOUR", startHour);

                    //Increment the hours completed
                    intent.putExtra("HOURS_DONE", hoursDone+1);

                    //Open the review screen
                    startActivity(intent);

                } else {

                    //If it is the last review then clear the GPS log file
                    try {
                        FileOutputStream outStream = openFileOutput(MainActivity.GPS_FILE_NAME, Context.MODE_PRIVATE);
                        outStream.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //Also cancel the notification if it exists
                    NotificationManager notificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                    notificationManager.cancel(MainActivity.REVIEW_CODE);


                }

                parent.finish();
            }
        };

        ImageButton button = (ImageButton) findViewById(R.id.reviewbutton1);
        button.setOnClickListener(listener);
        button = (ImageButton) findViewById(R.id.reviewbutton2);
        button.setOnClickListener(listener);
        button = (ImageButton) findViewById(R.id.reviewbutton3);
        button.setOnClickListener(listener);
        button = (ImageButton) findViewById(R.id.reviewbutton4);
        button.setOnClickListener(listener);
        button = (ImageButton) findViewById(R.id.reviewbutton5);
        button.setOnClickListener(listener);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mMapView != null) {
            mMapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mMapView != null) {
            try {
                mMapView.onDestroy();
            } catch (NullPointerException e) {
                Log.e("Maps","Error while attempting MapView.onDestroy(), ignoring exception", e);
            }
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mMapView != null) {
            mMapView.onLowMemory();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapView != null) {
            mMapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(this);
        googleMap.addMarker(new MarkerOptions().position(myLocation).title("Location"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation,17));
    }

}
