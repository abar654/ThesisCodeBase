package com.emotionsense.demo.data;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.emotionsense.demo.data.loggers.StoreOnlyUnencryptedFiles;
import com.ubhave.datahandler.except.DataHandlerException;
import com.ubhave.datahandler.loggertypes.AbstractDataLogger;
import com.ubhave.sensormanager.ESException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RateActivity extends Activity {

    private AbstractDataLogger logger;
    private final static String LOG_TAG = "RateActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate);

        try {
            logger = StoreOnlyUnencryptedFiles.getInstance();
        } catch (ESException e) {
            e.printStackTrace();
        } catch (DataHandlerException e) {
            e.printStackTrace();
        }

        final Activity parent = this;
        ImageButton.OnClickListener listener = new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Return to the home screen
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                String timestamp = dateFormat.format(date);
                int rating = 0;

                switch (v.getId()) {
                    case R.id.button1:
                        rating = 1;
                        break;
                    case R.id.button2:
                        rating = 2;
                        break;
                    case R.id.button3:
                        rating = 3;
                        break;
                    case R.id.button4:
                        rating = 4;
                        break;
                    case R.id.button5:
                        rating = 5;
                        break;
                }

                logger.log(LOG_TAG, rating + "," + timestamp);

                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                notificationManager.cancel(MainActivity.RATE_CODE);

                parent.setResult(rating);
                parent.finish();
            }
        };

        ImageButton button = (ImageButton) findViewById(R.id.button1);
        button.setOnClickListener(listener);
        button = (ImageButton) findViewById(R.id.button2);
        button.setOnClickListener(listener);
        button = (ImageButton) findViewById(R.id.button3);
        button.setOnClickListener(listener);
        button = (ImageButton) findViewById(R.id.button4);
        button.setOnClickListener(listener);
        button = (ImageButton) findViewById(R.id.button5);
        button.setOnClickListener(listener);

    }
}
